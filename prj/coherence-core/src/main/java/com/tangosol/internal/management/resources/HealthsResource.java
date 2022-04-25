/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.management.resources;

import com.oracle.coherence.common.base.Logger;
import com.tangosol.internal.http.HttpRequest;
import com.tangosol.internal.http.RequestRouter;
import com.tangosol.internal.http.Response;

import com.tangosol.internal.management.EntityMBeanResponse;

import com.tangosol.net.management.MBeanAccessor;
import com.tangosol.net.management.MBeanAccessor.QueryBuilder.ParsedQuery;
import com.tangosol.util.HealthCheck;

import java.net.URI;

import java.util.Map;

/**
 * The management resource for health checks.
 *
 * @author Jonathan Knight  2022.04.04
 * @since 22.06
 */
public class HealthsResource
        extends AbstractManagementResource
    {
    @Override
    public void addRoutes(RequestRouter router, String sPathRoot)
        {
        router.addGet(sPathRoot, this::get);
        router.addGet(sPathRoot + HealthCheck.PATH_READY, this::ready);
        router.addGet(sPathRoot + HealthCheck.PATH_LIVE, this::live);
        router.addGet(sPathRoot + HealthCheck.PATH_STARTED, this::started);
        router.addGet(sPathRoot + HealthCheck.PATH_SAFE, this::safe);

        // child resources
        router.addRoutes(sPathRoot + "/" + MEMBERS, new HealthMembersResource());
        router.addRoutes(sPathRoot + "/{" + NAME +"}", f_healthResource);
        }

    // ----- API methods ----------------------------------------------------

    /**
     * Returns all the health checks across the cluster.
     *
     * @param request  the {@link HttpRequest}
     *
     * @return a response containing all the health checks across the cluster.
     */
    public Response get(HttpRequest request)
        {
        URI                 uriCurrent    = getCurrentUri(request);
        EntityMBeanResponse mBeanResponse = getResponseBodyForMBeanCollection(request, getQuery(request),
            f_healthResource, NAME, null, getParentUri(request), uriCurrent, uriCurrent, null);

        if (mBeanResponse == null && getService(request) != null)
            {
            return Response.status(Response.Status.NOT_FOUND).build();
            }

        if (mBeanResponse == null)
            {
            return response(new EntityMBeanResponse());
            }

        mBeanResponse.addResourceLink(MEMBERS, getSubUri(uriCurrent, MEMBERS));

        return response(mBeanResponse);
        }

    /**
     * Return a {@link Response} indicating whether all health checks in the cluster
     * have a {@code Ready} value of {@code true}.
     *
     * @param request  the {@link HttpRequest}
     *
     * @return a 200 response if all health checks are ready, otherwise a 503 response
     */
    public Response ready(HttpRequest request)
        {
        return handle(request, "Ready");
        }

    /**
     * Return a {@link Response} indicating whether all health checks in the cluster
     * have a {@code Live} value of {@code true}.
     *
     * @param request  the {@link HttpRequest}
     *
     * @return a 200 response if all health checks are live, otherwise a 503 response
     */
    public Response live(HttpRequest request)
        {
        return handle(request, "Live");
        }

    /**
     * Return a {@link Response} indicating whether all health checks in the cluster
     * have a {@code Started} value of {@code true}.
     *
     * @param request  the {@link HttpRequest}
     *
     * @return a 200 response if all health checks are started, otherwise a 503 response
     */
    public Response started(HttpRequest request)
        {
        return handle(request, "Started");
        }

    /**
     * Return a {@link Response} indicating whether all health checks in the cluster
     * have a {@code Safe} value of {@code true}.
     *
     * @param request  the {@link HttpRequest}
     *
     * @return a 200 response if all health checks are safe, otherwise a 503 response
     */
    public Response safe(HttpRequest request)
        {
        return handle(request, "Safe");
        }

    // ----- helper methods -------------------------------------------------

    /**
     * The query string for searching list of health checks.
     *
     * @param request  the request
     *
     * @return the MBean query builder
     */
    protected MBeanAccessor.QueryBuilder getQuery(HttpRequest request)
        {
        return createQueryBuilder(request).withBaseQuery(HEALTH_QUERY);
        }

    /**
     * Return a {@link Response} indicating whether the value of the specified attribute for
     * all health checks in the cluster have a value of {@code true}.
     *
     * @param request     the {@link HttpRequest}
     * @param sAttribute  the name of the health check attribute
     *
     * @return a 200 response if the attribute is {@code true} for all health checks, otherwise a 503 response
     */
    protected Response handle(HttpRequest request, String sAttribute)
        {
        try
            {
            ParsedQuery                      query    = getQuery(request).build();
            MBeanAccessor                    accessor = ensureBeanAccessor(request);
            Map<String, Map<String, Object>> map      = accessor.getAttributes(query);

            boolean fOK = map.values().stream()
                    .map(m -> m.get(sAttribute))
                    .allMatch(Boolean.class::cast);

            return fOK ? Response.ok().build() : Response.status(503).build();
            }
        catch (Throwable t)
            {
            Logger.err("HealthsResource failed to handle request \"" + sAttribute + "\": " + t.getMessage());
            return Response.serverError().build();
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * The child {@link HealthResource}.
     */
    private final HealthResource f_healthResource = new HealthResource();
    }
