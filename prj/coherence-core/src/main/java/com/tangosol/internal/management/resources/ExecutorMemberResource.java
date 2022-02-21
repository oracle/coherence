/*
 * Copyright (c) 2021, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.management.resources;

import com.tangosol.internal.http.HttpRequest;
import com.tangosol.internal.http.RequestRouter;
import com.tangosol.internal.http.Response;

import com.tangosol.internal.management.EntityMBeanResponse;
import com.tangosol.net.management.MBeanAccessor.QueryBuilder;

import java.net.URI;

import java.util.HashMap;
import java.util.Map;

/**
 * API resource for a ExecutorMBean member.
 *
 * @author lh  2021.11.05
 * @author Jonathan Knight  2022.01.25
 * @since 21.12
 */
public class ExecutorMemberResource
        extends AbstractManagementResource
    {
    // ----- Routes methods -------------------------------------------------

    @Override
    public void addRoutes(RequestRouter router, String sPathRoot)
        {
        router.addGet(sPathRoot, this::get);
        }

    // ----- GET API --------------------------------------------------------

    /**
     * Return attributes of an ExecutorMBean with the given MEMBER_KEY,
     * a node ID.
     *
     * @return the response object
     */
    public Response get(HttpRequest request)
        {
        String   sNodeId  = request.getFirstPathParameter(MEMBER_KEY);
        Response response = response(getResponseBodyForMBeanCollection(request, getQuery(request, sNodeId),
                null, getParentUri(request), getCurrentUri(request)));

        return response == null
                ? response(new HashMap<>())
                : response;
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

    // ---- ExecutorMemberResource methods --------------------------------------

    /**
     * The ExecutorMBean query.
     *
     *
     * @param request  the request
     * @param sNodeId  the member node ID
     *
     * @return the MBean query builder
     */
    protected QueryBuilder getQuery(HttpRequest request, String sNodeId)
        {
        return createQueryBuilder(request)
                .withBaseQuery(EXECUTORS_QUERY)
                .withMember(sNodeId);
        }
    }
