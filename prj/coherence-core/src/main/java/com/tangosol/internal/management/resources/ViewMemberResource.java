/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.management.resources;

import com.tangosol.internal.http.HttpRequest;
import com.tangosol.internal.http.RequestRouter;
import com.tangosol.internal.http.Response;
import com.tangosol.internal.management.EntityMBeanResponse;

import com.tangosol.net.management.MBeanAccessor.QueryBuilder;

import java.net.URI;

import java.util.Map;

/**
 * Handles management API requests for a View Mbean.
 */
public class ViewMemberResource
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
     * Return ViewMBean(s) attributes for a particular view running on a cluster member.
     *
     * @return the response object
     */
    public Response get(HttpRequest request)
        {
        return response(getResponseBodyForMBeanCollection(request, getQuery(request),
                null, getParentUri(request), getCurrentUri(request)));
        }

    // ----- AbstractManagementResource methods------------------------------

    @Override
    protected EntityMBeanResponse getQueryResult(HttpRequest         request,
                                                 URI                 uriParent,
                                                 URI                 uriCurrent,
                                                 Map<String, Object> mapQuery,
                                                 Map<String, String> mapArguments)
        {
        return getLinksOnlyResponseBody(request, uriParent, getSubUri(uriParent, mapArguments.get(MEMBER_KEY)),
                getLinksFilter(request, mapQuery));
        }

    // ----- ViewMemberResource methods-------------------------------------

    /**
     * Create a {@link QueryBuilder} for the view MBeans.
     *
     * @return QueryBuilder for view MBean
     */
    private QueryBuilder getQuery(HttpRequest request)
        {
        String sViewName    = request.getFirstPathParameter(VIEW_NAME);
        String sMemberKey   = request.getFirstPathParameter(MEMBER_KEY);
        String sTier        = request.getFirstQueryParameter(TIER);
        String sLoader      = request.getFirstQueryParameter(LOADER);
        String sServiceName = request.getPathParameters().getFirst(SERVICE_NAME);

        String queryBuilder = VIEW_QUERY + sViewName + (sTier == null ? "" : "," + TIER + "=" + sTier) +
                (sLoader == null ? "" : "," + LOADER + "=" + sLoader);

        return createQueryBuilder(request)
                .withBaseQuery(queryBuilder)
                .withMember(sMemberKey)
                .withService(sServiceName);
        }
    }
