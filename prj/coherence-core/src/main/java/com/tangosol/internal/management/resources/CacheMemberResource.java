/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
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

import java.util.Map;

/**
 * Handles management API requests for a Cache Mbean.
 *
 * @author sr  2017.08.29
 * @author Jonathan Knight  2022.01.25
 * @since 12.2.1.4.0
 */
public class CacheMemberResource extends AbstractManagementResource
    {
    // ----- Routes methods -------------------------------------------------

    @Override
    public void addRoutes(RequestRouter router, String sPathRoot)
        {
        router.addGet(sPathRoot, this::get);
        router.addPost(sPathRoot, this::update);
        router.addPost(sPathRoot + "/" + RESET_STATS, this::resetStatistics);
        }

    // ----- GET API --------------------------------------------------------

    /**
     * Return CacheMBean(s) attributes for a particular cache running on a cluster member.
     *
     * @return the response object
     */
    public Response get(HttpRequest request)
        {
        return response(getResponseBodyForMBeanCollection(request, getQuery(request),
                null, getParentUri(request), getCurrentUri(request)));
        }

    // ----- POST API(Operations) -------------------------------------------

    /**
     * Call "resetStatistics" operation on CacheMBean.
     *
     * @return the response object
     */
    public Response resetStatistics(HttpRequest request)
        {
        return executeMBeanOperation(request, getQuery(request), RESET_STATS, null, null);
        }

    // ----- POST API(Update) -----------------------------------------------

    /**
     * Update a CacheMBean with the parameters present in the input entity map.
     *
     * @return the response object
     */
    public Response update(HttpRequest request)
        {
        return update(request, getJsonBody(request), getQuery(request));
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

    // ----- CacheMemberResource methods-------------------------------------

    /**
     * Create a {@link QueryBuilder} for the cache MBeans.
     *
     * @return QueryBuilder for cache MBean
     */
    private QueryBuilder getQuery(HttpRequest request)
        {
        String sCacheName   = request.getFirstPathParameter(CACHE_NAME);
        String sMemberKey   = request.getFirstPathParameter(MEMBER_KEY);
        String sTier        = request.getFirstQueryParameter(TIER);
        String sLoader      = request.getFirstQueryParameter(LOADER);
        String sServiceName = request.getPathParameters().getFirst(SERVICE_NAME);

        String queryBuilder = CACHE_QUERY + sCacheName + (sTier == null ? "" : "," + TIER + "=" + sTier) +
                (sLoader == null ? "" : "," + LOADER + "=" + sLoader);

        return createQueryBuilder(request)
                .withBaseQuery(queryBuilder)
                .withMember(sMemberKey)
                .withService(sServiceName);
        }
    }
