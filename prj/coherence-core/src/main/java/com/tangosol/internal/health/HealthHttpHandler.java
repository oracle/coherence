/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.health;

import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.common.base.Logger;

import com.tangosol.coherence.config.Config;
import com.tangosol.coherence.http.AbstractGenericHttpServer;
import com.tangosol.coherence.http.BasicAuthentication;

import com.tangosol.internal.http.BaseHttpHandler;
import com.tangosol.internal.http.HttpException;
import com.tangosol.internal.http.HttpMethod;
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

import com.tangosol.net.security.IdentityAsserter;
import com.tangosol.net.security.UsernameAndPassword;

import com.tangosol.util.HealthCheck;
import com.tangosol.util.RegistrationBehavior;
import com.tangosol.util.ResourceRegistry;

import java.io.IOException;
import java.io.OutputStream;

import java.nio.charset.StandardCharsets;

import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.security.auth.Subject;

import static com.tangosol.util.BuilderHelper.using;

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

        router.addPut("/suspend", this::suspend);
        router.addPut("/suspend/{serviceName}", this::suspend);
        router.addPut("/resume", this::resume);
        router.addPut("/resume/{serviceName}", this::resume);
        }

    @Override
    protected void beforeRouting(HttpRequest request)
        {
        if (isMutatorRequest(request))
            {
            ensureMutatorAccess(request);
            }
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
                return unavailable();
                }
            if (management.allHealthChecksReady())
                {
                return ok();
                }
                Logger.log("Health: checking readiness failed, allHealthChecksReady==false", 9);
                return unavailable();
            }
        catch (Exception e)
            {
            Logger.finer("Health: checking readiness failed: " + e.getMessage());
            return unavailable();
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
            return unavailable();
            }
        return management.allHealthChecksLive()
                ? ok()
                : unavailable();
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
            return unavailable();
            }
        return management.allHealthChecksStarted()
                ? ok()
                : unavailable();
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
            return unavailable();
            }
        return management.allHealthChecksSafe()
                ? ok()
                : unavailable();
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
        ensureMutatorAccess(request);

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

        return ok();
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
        ensureMutatorAccess(request);

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

        return ok();
        }

    // ----- helpers -------------------------------------------------------

    /**
     * Ensure the health mutator request is explicitly enabled and authenticated.
     *
     * @param request  the request
     *
     * @throws HttpException if the request is not allowed
     */
    protected void ensureMutatorAccess(HttpRequest request)
        {
        ResourceRegistry registry = request.getResourceRegistry();
        if (registry.getResource(MutatorAccess.class) != null)
            {
            return;
            }

        if (!Config.getBoolean(PROP_MUTATORS_ENABLED, false))
            {
            Logger.warn("Health: rejected disabled HTTP mutator request");
            throw new HttpException(Response.Status.FORBIDDEN.getStatusCode());
            }

        String sAuthMethod = Config.getProperty(PROP_AUTH);
        sAuthMethod = sAuthMethod == null || sAuthMethod.trim().isEmpty()
                ? AbstractGenericHttpServer.AUTH_BASIC
                : sAuthMethod.trim().toLowerCase(Locale.ROOT);

        Subject subject = null;

        switch (sAuthMethod)
            {
            case AbstractGenericHttpServer.AUTH_BASIC:
                subject = authenticateBasic(request);
                break;

            case AbstractGenericHttpServer.AUTH_CERT:
                subject = ensurePeerSubject(request);
                break;

            case AbstractGenericHttpServer.AUTH_CERT_BASIC:
                ensurePeerSubject(request);
                subject = authenticateBasic(request);
                break;

            default:
                Logger.warn("Health: rejected HTTP mutator request due to unsupported auth method");
                throw new HttpException(Response.Status.FORBIDDEN.getStatusCode());
            }

        registerSubject(registry, subject);
        registry.registerResource(MutatorAccess.class, MutatorAccess.class.getName(),
                                  using(MutatorAccess.INSTANCE), RegistrationBehavior.REPLACE, null);
        }

    /**
     * Authenticate a Basic authorization header.
     *
     * @param request  the request
     *
     * @return the authenticated subject
     */
    protected Subject authenticateBasic(HttpRequest request)
        {
        try
            {
            BasicAuthentication.Credentials credentials =
                    BasicAuthentication.parse(request.getHeaderString(AbstractGenericHttpServer.HEADER_AUTHORIZATION));

            if (credentials != null)
                {
                return getIdentityAsserter().assertIdentity(
                        new UsernameAndPassword(credentials.getUsername(), credentials.getPassword()),
                        m_service);
                }
            }
        catch (IllegalArgumentException | SecurityException e)
            {
            // fall through to unauthorized
            }

        throw new HttpException(Response.Status.UNAUTHORIZED.getStatusCode());
        }

    /**
     * Return the subject asserted from the TLS peer.
     *
     * @param request  the request
     *
     * @return the TLS peer subject
     */
    protected Subject ensurePeerSubject(HttpRequest request)
        {
        Subject subject = request.getResourceRegistry().getResource(Subject.class);
        if (subject == null)
            {
            throw new HttpException(Response.Status.FORBIDDEN.getStatusCode());
            }
        return subject;
        }

    /**
     * Return the identity asserter for health mutator Basic authentication.
     *
     * @return the identity asserter
     */
    protected IdentityAsserter getIdentityAsserter()
        {
        return AbstractGenericHttpServer.DEFAULT_IDENTITY_ASSERTER;
        }

    /**
     * Return {@code true} if the request targets a state-changing health route.
     *
     * @param request  the request
     *
     * @return {@code true} if the request is a health mutator request
     */
    private boolean isMutatorRequest(HttpRequest request)
        {
        if (request.getMethod() != HttpMethod.PUT)
            {
            return false;
            }

        String sPath = request.getRequestURI().getPath();
        return isMutatorPath(sPath);
        }

    /**
     * Return {@code true} if the path targets a health mutator route.
     *
     * @param sPath  the request path
     *
     * @return {@code true} if the path targets a health mutator route
     */
    private boolean isMutatorPath(String sPath)
        {
        return "/suspend".equals(sPath)
                || sPath.startsWith("/suspend/")
                || "/resume".equals(sPath)
                || sPath.startsWith("/resume/");
        }

    /**
     * Register the authenticated subject for route processing and auditing.
     *
     * @param registry  the request resource registry
     * @param subject   the authenticated subject
     */
    private void registerSubject(ResourceRegistry registry, Subject subject)
        {
        if (subject == null)
            {
            throw new HttpException(Response.Status.UNAUTHORIZED.getStatusCode());
            }
        registry.registerResource(Subject.class, Subject.class.getName(),
                                  using(subject), RegistrationBehavior.REPLACE, null);
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

    private Response ok()
        {
        return response(Response.Status.OK);
        }

    private Response unavailable()
        {
        return response(Response.Status.SERVICE_UNAVAILABLE);
        }

    private Response response(Response.Status status)
        {
        Response.Builder resp = Response.status(status);
        int              nId  = -1;
        try
            {
            Cluster cluster = CacheFactory.getCluster();
            if (cluster != null)
                {
                Member member = cluster.getLocalMember();
                if (member != null)
                    {
                    nId = member.getId();
                    }
                }
            }
        catch (Exception e)
            {
            // ignored
            }
        return resp.addHeaders(Map.of(HEADER_NODE_ID, List.of(String.valueOf(nId)))).build();
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

    // ----- inner class: MutatorAccess ------------------------------------

    /**
     * Request-local marker proving the health mutator gate has run.
     */
    private static class MutatorAccess
        {
        private static final MutatorAccess INSTANCE = new MutatorAccess();
        }

    // ----- constants ------------------------------------------------------

    /**
     * The http header value used to specify the node id.
     */
    public static final String HEADER_NODE_ID = "coherence-node-id";

    /**
     * A {@code String} constant representing the json media type.
     */
    protected final static String APPLICATION_JSON = "application/json";

    /**
     * The default service name running the health check http proxy.
     */
    public static final String DEFAULT_SERVICE_NAME = "$SYS:HealthHttpProxy";

    /**
     * Property used to explicitly enable health HTTP mutators.
     */
    public static final String PROP_MUTATORS_ENABLED = "coherence.health.http.mutators.enabled";

    /**
     * Property used to select health HTTP mutator authentication.
     */
    public static final String PROP_AUTH = "coherence.health.http.auth";

    /**
     * The actual service name running the health check http proxy.
     */
    private static volatile String s_sServiceName = DEFAULT_SERVICE_NAME;
    }
