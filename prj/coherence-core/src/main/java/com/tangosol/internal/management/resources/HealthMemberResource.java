/*
 * Copyright (c) 2021, 2022, Oracle and/or its affiliates.
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
import com.tangosol.net.management.MBeanAccessor.QueryBuilder;
import com.tangosol.net.management.MBeanAccessor.QueryBuilder.ParsedQuery;

import java.net.URI;

import java.util.HashMap;
import java.util.Map;

/**
 * API resource for a Health MBean member.
 *
 * @author Jonathan Knight  2022.04.04
 * @since 22.06
 */
public class HealthMemberResource
        extends AbstractManagementResource
    {
    // ----- Routes methods -------------------------------------------------

    @Override
    public void addRoutes(RequestRouter router, String sPathRoot)
        {
        router.addGet(sPathRoot, this::get);
        }

    // ----- API methods ----------------------------------------------------

    /**
     * Return attributes of an Health MBean with the given MEMBER_KEY,
     * a node ID.
     *
     * @return the response object
     */
    public Response get(HttpRequest request)
        {
        Response response = response(getResponseBodyForMBeanCollection(request, getQuery(request),
                null, getParentUri(request), getCurrentUri(request)));

        return response == null
                ? response(new HashMap<>())
                : response;
        }

    /**
     * Return a {@link Response} indicating whether all health checks for a member
     * have a {@code Ready} value of {@code true}.
     *
     * @param request  the {@link HttpRequest}
     * @param nNodeId  the node id to obtain health checks for
     *
     * @return a 200 response if all health checks for a member are ready, otherwise a 503 response
     */
    public Response ready(HttpRequest request, int nNodeId)
        {
        return handle(request, "Ready", nNodeId);
        }

    /**
     * Return a {@link Response} indicating whether all health checks for a member
     * have a {@code Live} value of {@code true}.
     *
     * @param request  the {@link HttpRequest}
     * @param nNodeId  the node id to obtain health checks for
     *
     * @return a 200 response if all health checks for a member are live, otherwise a 503 response
     */
    public Response live(HttpRequest request, int nNodeId)
        {
        return handle(request, "Live", nNodeId);
        }

    /**
     * Return a {@link Response} indicating whether all health checks for a member
     * have a {@code Started} value of {@code true}.
     *
     * @param request  the {@link HttpRequest}
     * @param nNodeId  the node id to obtain health checks for
     *
     * @return a 200 response if all health checks for a member are started, otherwise a 503 response
     */
    public Response started(HttpRequest request, int nNodeId)
        {
        return handle(request, "Started", nNodeId);
        }

    /**
     * Return a {@link Response} indicating whether all health checks for a member
     * have a {@code Safe} value of {@code true}.
     *
     * @param request  the {@link HttpRequest}
     * @param nNodeId  the node id to obtain health checks for
     *
     * @return a 200 response if all health checks for a member are safe, otherwise a 503 response
     */
    public Response safe(HttpRequest request, int nNodeId)
        {
        return handle(request, "Safe", nNodeId);
        }

    // ----- AbstractManagementResource methods------------------------------

    @Override
    protected EntityMBeanResponse getQueryResult(HttpRequest         request,
                                                 URI                 uriParent,
                                                 URI                 uriCurrent,
                                                 Map<String, Object> mapQuery,
                                                 Map<String, String> mapArguments)
        {
        String sNodeId = mapArguments.get(MEMBER_KEY);
        URI    uriSelf = getSubUri(uriParent, sNodeId);

        return getLinksOnlyResponseBody(request, uriParent, uriSelf, getLinksFilter(request, mapQuery));
        }

    // ----- helper methods -------------------------------------------------

    /**
     * The Health MBean query.
     *
     * @param request  the request
     *
     * @return the MBean query builder
     */
    protected QueryBuilder getQuery(HttpRequest request)
        {
        String sNodeId = request.getFirstPathParameter(MEMBER_KEY);
        return createHealthQueryBuilder(request).withMember(sNodeId);
        }

    /**
     * Return a {@link Response} indicating whether the value of the specified attribute for
     * all health checks for a member have a value of {@code true}.
     *
     * @param request     the {@link HttpRequest}
     * @param sAttribute  the name of the health check attribute
     * @param nNodeId     the node id to obtain health checks for
     *
     * @return a 200 response if the attribute is {@code true} for all health checks, otherwise a 503 response
     */
    protected Response handle(HttpRequest request, String sAttribute, int nNodeId)
        {
        ParsedQuery query = createHealthQueryBuilder(request)
                .withMember(String.valueOf(nNodeId))
                .build();

        MBeanAccessor                    accessor = ensureBeanAccessor(request);
        Map<String, Map<String, Object>> map      = accessor.getAttributes(query);
        boolean                          fOK      = true;

        for (Map.Entry<String, Map<String, Object>> entry : map.entrySet())
            {
            Boolean fEnabled = (Boolean) entry.getValue().get("MemberHealthCheck");
            if (fEnabled == null || fEnabled)
                {
                Object o = entry.getValue().get(sAttribute);
                if (o instanceof Boolean && !((Boolean) o))
                    {
                    fOK = false;
                    Logger.warn("Health: " + sAttribute + " check failed for health check " + entry.getKey());
                    }
                }
            }
        return fOK ? Response.ok().build() : Response.status(503).build();
        }
    }
