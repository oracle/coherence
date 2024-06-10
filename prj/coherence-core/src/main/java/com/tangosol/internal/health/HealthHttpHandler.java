/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.health;

import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.common.base.Logger;

import com.tangosol.internal.http.BaseHttpHandler;
import com.tangosol.internal.http.HttpRequest;
import com.tangosol.internal.http.RequestRouter;
import com.tangosol.internal.http.Response;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.Member;
import com.tangosol.net.MemberIdentity;
import com.tangosol.net.Service;

import com.tangosol.net.management.MapJsonBodyHandler;
import com.tangosol.net.management.Registry;

import com.tangosol.util.HealthCheck;

import java.io.IOException;
import java.io.OutputStream;

import java.nio.charset.StandardCharsets;

import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A {@link com.sun.net.httpserver.HttpHandler} to provide the
 * Coherence health check endpoints.
 *
 * @author Jonathan Knight  2022.04.04
 * @since 22.06
 */
public class HealthHttpHandler
        extends BaseHttpHandler
    {
    /**
     * Create a {@link HealthHttpHandler}.
     */
    public HealthHttpHandler()
        {
        super(new RequestRouter(), ensureBodyWriter());
        }

    @Override
    protected void configureRoutes(RequestRouter router)
        {
        router.setDefaultProduces(APPLICATION_JSON);
        router.setDefaultConsumes(APPLICATION_JSON);
        router.addDefaultResponseHeader("X-Content-Type-Options", "nosniff");
        router.addDefaultResponseHeader("Content-type", APPLICATION_JSON);
        router.addDefaultResponseHeader("Vary", "Accept-Encoding");

        router.addGet(HealthCheck.PATH_READY, this::ready);
        router.addGet(HealthCheck.PATH_STARTED, this::started);
        router.addGet(HealthCheck.PATH_LIVE, this::live);
        router.addGet(HealthCheck.PATH_HEALTHZ, this::live);
        router.addGet(HealthCheck.PATH_SAFE, this::safe);
        router.addGet("/ha", this::safe);

        router.addGet("/suspend", this::suspend);
        router.addPut("/suspend", this::suspend);
        router.addGet("/suspend/{serviceName}", this::suspend);
        router.addPut("/suspend/{serviceName}", this::suspend);
        router.addGet("/resume", this::resume);
        router.addPut("/resume", this::resume);
        router.addGet("/resume/{serviceName}", this::resume);
        router.addPut("/resume/{serviceName}", this::resume);
        }

    @Override
    protected void beforeRouting(HttpRequest request)
        {
        }

    @Override
    public void setService(Service service)
        {
        super.setService(service);
        s_sServiceName = service.getInfo().getServiceName();
        }

    // ----- routes ---------------------------------------------------------

    /**
     * Return a {@link Response} indicating whether all health checks for this member
     * have a {@code Ready} value of {@code true}.
     *
     * @param request  the {@link HttpRequest}
     *
     * @return a 200 response if all health checks for this member are ready, otherwise a 503 response
     */
    public Response ready(HttpRequest request)
        {
        Logger.log("Health: checking readiness", 9);
        try
            {
            Registry management = CacheFactory.getCluster().getManagement();
            if (management == null)
                {
                Logger.log("Health: checking readiness failed, no management service present", 9);
                return Response.status(503).build();
                }
            if (management.allHealthChecksReady())
                {
                return Response.ok().build();
                }
                Logger.log("Health: checking readiness failed, allHealthChecksReady==false", 9);
                return Response.status(503).build();
            }
        catch (Exception e)
            {
            Logger.finer("Health: checking readiness failed: " + e.getMessage());
            return Response.status(503).build();
            }
        }

    /**
     * Return a {@link Response} indicating whether all health checks for this member
     * have a {@code Live} value of {@code true}.
     *
     * @param request  the {@link HttpRequest}
     *
     * @return a 200 response if all health checks for this member are live, otherwise a 503 response
     */
    public Response live(HttpRequest request)
        {
        Logger.log("Health: checking liveness", 9);
        Registry management = CacheFactory.getCluster().getManagement();
        if (management == null)
            {
            return Response.status(503).build();
            }
        return management.allHealthChecksLive()
                ? Response.ok().build()
                : Response.status(503).build();
        }

    /**
     * Return a {@link Response} indicating whether all health checks for this member
     * have a {@code Started} value of {@code true}.
     *
     * @param request  the {@link HttpRequest}
     *
     * @return a 200 response if all health checks for this member are started, otherwise a 503 response
     */
    public Response started(HttpRequest request)
        {
        Logger.log("Health: checking started", 9);
        Registry management = CacheFactory.getCluster().getManagement();
        if (management == null)
            {
            return Response.status(503).build();
            }
        return management.allHealthChecksStarted()
                ? Response.ok().build()
                : Response.status(503).build();
        }

    /**
     * Return a {@link Response} indicating whether all health checks for this member
     * have a {@code Safe} value of {@code true}.
     *
     * @param request  the {@link HttpRequest}
     *
     * @return a 200 response if all health checks for this member are safe, otherwise a 503 response
     */
    public Response safe(HttpRequest request)
        {
        Logger.log("Health: checking safe", 9);
        Registry management = CacheFactory.getCluster().getManagement();
        if (management == null)
            {
            return Response.status(503).build();
            }
        return management.allHealthChecksSafe()
                ? Response.ok().build()
                : Response.status(503).build();
        }
    
    /**
     * Suspend all cache services that are only available on members with the same role
     * as the local member and that have active persistence enabled.
     *
     * @param request  the {@link HttpRequest}
     *
     * @return the {@link Response}
     */
    protected Response suspend(HttpRequest request)
        {
        Cluster cluster  = m_service.getCluster();
        Member  member   = cluster.getLocalMember();
        String  sRole    = member.getRoleName();
        String  sService = request.getFirstPathParameter("serviceName");

        if (sService != null)
            {
            Logger.info("Health: Suspending service " + sService + " if active persistence enabled and service present only in members with role " + sRole);
            suspendService(sService);
            }
        else
            {
            Logger.info("Health: Suspending all active persistence enabled services present only in members with role " + sRole);
            Enumeration<String> names = cluster.getServiceNames();
            while (names.hasMoreElements())
                {
                String sName = names.nextElement();
                suspendService(sName);
                }
            }

        return Response.ok().build();
        }

    /**
     * Suspend the specified service that is only available on members with the same role
     * as the local member and that have active persistence enabled.
     *
     * @param sName  the service name
     */
    protected void suspendService(String sName)
        {
        Cluster cluster = m_service.getCluster();
        Service service = cluster.getService(sName);

        if (service == null)
            {
            return;
            }

        Logger.finest("Maybe suspending " + sName);
        if (service instanceof DistributedCacheService && ((DistributedCacheService) service).isLocalStorageEnabled())
            {
            DistributedCacheService distributedCacheService = (DistributedCacheService) service;

            // count the number of member roles for the storage enabled members
            List<String> listRole = distributedCacheService.getOwnershipEnabledMembers()
                    .stream()
                    .map(MemberIdentity::getRoleName)
                    .distinct()
                    .collect(Collectors.toList());

            if (listRole.size() <= 1)
                {
                String sMode = distributedCacheService.getPersistenceMode();
                if ("active".equalsIgnoreCase(sMode))
                    {
                    // active persistence is enabled so suspend this service
                    Logger.info("Health: Suspending service " + sName);
                    cluster.suspendService(sName);
                    }
                else
                    {
                    Logger.finest("Skipping suspension of " + sName + " - active persistence is not enabled");
                    }
                }
            else
                {
                Logger.finest("Skipping suspension of " + sName + " - service exists in multiple roles " + listRole);
                }
            }
        else
            {
            Logger.finest("Skipping suspension of " + sName + " - not a storage enabled DistributedCacheService");
            }
        }

    /**
     * Resume suspended services.
     *
     * @param request  the {@link HttpRequest}
     *
     * @return the {@link Response}
     */
    protected Response resume(HttpRequest request)
        {
        Cluster cluster   = m_service.getCluster();
        String  sService  = request.getFirstPathParameter("serviceName");
        String  sExcludes = request.getFirstQueryParameter("exclude");

        if (sService == null)
            {
            Enumeration<String> enumNames = cluster.getServiceNames();

            Set<String> setExclude = sExcludes == null
                    ? Collections.emptySet()
                    : Arrays.stream(sExcludes.split(",")).map(String::trim).collect(Collectors.toSet());

            Logger.info("Health: Resuming all suspended services, exclusions=" + setExclude);
            while (enumNames.hasMoreElements())
                {
                sService = enumNames.nextElement();
                if (setExclude.contains(sService))
                    {
                    continue;
                    }
                Service service = cluster.getService(sService);
                if (service != null && service.isSuspended())
                    {
                    Logger.info("Health: Resuming service " + sService);
                    cluster.resumeService(sService);
                    }
                }
            }
        else
            {
            Service service = cluster.getService(sService);
            if (service != null && service.isSuspended())
                {
                Logger.info("Health: Resuming service " + sService);
                cluster.resumeService(sService);
                }
            }

        return Response.ok().build();
        }

    private static BodyWriter<?> ensureBodyWriter()
        {
        try
            {
            return MapJsonBodyHandler.ensureMapJsonBodyHandler();
            }
        catch (Exception e)
            {
            return new HealthBodyWriter();
            }
        }

    /**
     * Return the name of the service running the http proxy.
     *
     * @return the name of the service running the http proxy
     */
    public static String getServiceName()
        {
        return s_sServiceName;
        }

    // ----- inner class: BodyWriter --------------------------------------------------

    private static class HealthBodyWriter
            implements BodyWriter<Object>
        {
        @Override
        public void write(Object body, OutputStream out)
            {
            try
                {
                if (body != null)
                    {
                    out.write(String.valueOf(body).getBytes(StandardCharsets.UTF_8));
                    }
                }
            catch (IOException e)
                {
                throw Exceptions.ensureRuntimeException(e);
                }
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * A {@code String} constant representing the json media type.
     */
    protected final static String APPLICATION_JSON = "application/json";

    /**
     * The default service name running the health check http proxy.
     */
    public static final String DEFAULT_SERVICE_NAME = "$SYS:HealthHttpProxy";

    /**
     * The actual service name running the health check http proxy.
     */
    private static volatile String s_sServiceName = DEFAULT_SERVICE_NAME;
    }
