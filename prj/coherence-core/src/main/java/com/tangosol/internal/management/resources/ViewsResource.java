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

import com.tangosol.net.management.MBeanAccessor;

import java.net.URI;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles management API requests for views in a service.
 */
public class ViewsResource extends AbstractManagementResource
    {
    // ----- Routes methods -------------------------------------------------

    @Override
    public void addRoutes(RequestRouter router, String sPathRoot)
        {
        router.addGet(sPathRoot, this::get);
        router.addGet(sPathRoot + "/" + MEMBERS, this::getAllViewMembers);

        // child resources
        router.addRoutes(sPathRoot + "/{" + VIEW_NAME + "}", new ViewResource());
        }

    // ----- GET API --------------------------------------------------------

    /**
     * Returns the list of views in a Service, or the entire cluster if a service is not provided.
     *
     * @return the response object.
     */
    public Response get(HttpRequest request)
        {
        MBeanAccessor.QueryBuilder queryBuilder = createQueryBuilder(request)
                .withBaseQuery(VIEWS_QUERY)
                .withService(getService(request));

        URI uriCurrent = getCurrentUri(request);
        EntityMBeanResponse response   = getResponseBodyForMBeanCollection(request, queryBuilder, new ViewResource(),
                                                                           NAME, null, getParentUri(request), uriCurrent, uriCurrent, null);

        if (response == null && getService(request) != null)
            {
            return Response.status(Response.Status.NOT_FOUND).build();
            }

        return response == null
               ? response(new HashMap<>())
               : response(response);
        }

    /**
     * Returns the list of all ViewMBean members in the cluster.
     *
     * @return the response object.
     */
    public Response getAllViewMembers(HttpRequest request)
        {
        MBeanAccessor.QueryBuilder queryBuilder = createQueryBuilder(request)
                .withBaseQuery(VIEWS_QUERY)
                .withService(getService(request));

        return response(getResponseBodyForMBeanCollection(request, queryBuilder, null,
                                                          getParentUri(request), getCurrentUri(request)));
        }

    // ----- AbstractManagementResource methods -----------------------------

    @Override
    protected EntityMBeanResponse getQueryResult(HttpRequest         request,
                                                 URI                 uriParent,
                                                 URI                 uriCurrent,
                                                 Map<String, Object> mapQuery,
                                                 Map<String, String> mapArguments)
        {
        String sServiceName = mapArguments.get(SERVICE_NAME);

        MBeanAccessor.QueryBuilder queryBuilder = createQueryBuilder(request)
                .withBaseQuery(VIEWS_QUERY)
                .withService(sServiceName);

        return getResponseBodyForMBeanCollection(request, queryBuilder, new ViewResource(), NAME,
                                                 mapQuery, uriParent, getSubUri(uriParent, VIEWS), uriCurrent, mapArguments);
        }
    }
