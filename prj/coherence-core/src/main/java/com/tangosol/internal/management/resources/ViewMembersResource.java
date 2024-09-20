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

import java.net.URI;

import com.tangosol.net.management.MBeanAccessor.QueryBuilder;

import java.util.Map;

/**
 * Handles management API requests for a View Members(list of view Mbeans of same name).
 */
public class ViewMembersResource
        extends AbstractManagementResource
    {
    // ----- Routes methods -------------------------------------------------

    @Override
    public void addRoutes(RequestRouter router, String sPathRoot)
        {
        router.addGet(sPathRoot, this::get);

        // child resources
        router.addRoutes(sPathRoot + "/{" + MEMBER_KEY + "}", new ViewMemberResource());
        }

    // ----- GET API --------------------------------------------------------

    /**
     * Return list of ViewMBean objects for the provided view.
     *
     * @return the response object
     */
    public Response get(HttpRequest request)
        {
        String sViewName       = request.getFirstPathParameter(VIEW_NAME);
        String sTier           = request.getFirstQueryParameter(TIER);
        String queryBuilderStr = VIEW_QUERY + sViewName + (sTier == null ? "" : "," + TIER + "=" + sTier);

        QueryBuilder queryBuilder = createQueryBuilder(request).withBaseQuery(queryBuilderStr)
                .withService(getService(request));

        URI uriCurrent = getCurrentUri(request);
        return response(getResponseBodyForMBeanCollection(request, queryBuilder, new ViewMemberResource(),
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
        String       sViewName    = mapArguments.get(NAME);
        String       sBaseQuery   = VIEW_MEMBERS_WITH_SERVICE_QUERY + sViewName;
        QueryBuilder queryBuilder = createQueryBuilder(request).withBaseQuery(sBaseQuery).withService(sServiceName);

        return getResponseBodyForMBeanCollection(request, queryBuilder, new ViewMemberResource(), mapQuery,
                mapArguments, uriParent, getSubUri(uriParent, MEMBERS), uriCurrent);
        }
    }
