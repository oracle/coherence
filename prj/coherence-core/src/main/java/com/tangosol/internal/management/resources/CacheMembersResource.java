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
 * Handles management API requests for a Cache Members(list of cache Mbeans of same name).
 *
 * @author sr 2017.08.29
 * @author Jonathan Knight  2022.01.25
 * @since 12.2.1.4.0
 */
public class CacheMembersResource extends AbstractManagementResource
    {
    // ----- Routes methods -------------------------------------------------

    @Override
    public void addRoutes(RequestRouter router, String sPathRoot)
        {
        router.addGet(sPathRoot, this::get);

        // child resources
        router.addRoutes(sPathRoot + "/{" + MEMBER_KEY + "}", new CacheMemberResource());
        }

    // ----- GET API --------------------------------------------------------

    /**
     * Return list of CacheMBean objects for the provided cache.
     *
     * @return the response object
     */
    public Response get(HttpRequest request)
        {
        String sCacheName   = request.getFirstPathParameter(CACHE_NAME);
        String sTier        = request.getFirstQueryParameter(TIER);
        String queryBuilderStr = CACHE_QUERY + sCacheName + (sTier == null ? "" : "," + TIER + "=" + sTier);

        QueryBuilder queryBuilder = createQueryBuilder(request).withBaseQuery(queryBuilderStr)
                .withService(getService(request));

        URI uriCurrent = getCurrentUri(request);
        return response(getResponseBodyForMBeanCollection(request, queryBuilder, new CacheMemberResource(),
                                                          null, null, getParentUri(request), uriCurrent, uriCurrent));
        }

    // ----- AbstractManagementResource methods -------------------------------------------

    @Override
    protected EntityMBeanResponse getQueryResult(HttpRequest         request,
                                                 URI                 uriParent,
                                                 URI                 uriCurrent,
                                                 Map<String, Object> mapQuery,
                                                 Map<String, String> mapArguments)
        {
        String       sServiceName = mapArguments.get(SERVICE_NAME);
        String       sCacheName   = mapArguments.get(NAME);
        String       sBaseQuery   = CACHE_MEMBERS_WITH_SERVICE_QUERY + sCacheName;
        QueryBuilder queryBuilder    = createQueryBuilder(request).withBaseQuery(sBaseQuery).withService(sServiceName);

        return getResponseBodyForMBeanCollection(request, queryBuilder, new CacheMemberResource(), mapQuery,
                mapArguments, uriParent, getSubUri(uriParent, MEMBERS), uriCurrent);
        }
    }
