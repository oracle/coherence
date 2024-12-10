/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
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

import java.util.HashMap;
import java.util.Map;

/**
 * Handles management API requests for caches in a service.
 *
 * @author sr  2017.08.29
 * @author Jonathan Knight  2022.01.25
 * @since 12.2.1.4.0
 */
public class CachesResource extends AbstractManagementResource
    {
    // ----- Routes methods -------------------------------------------------

    @Override
    public void addRoutes(RequestRouter router, String sPathRoot)
        {
        router.addGet(sPathRoot, this::get);
        router.addGet(sPathRoot + "/" + MEMBERS, this::getAllCacheMembers);

        // child resources
        router.addRoutes(sPathRoot + "/{" + CACHE_NAME + "}", new CacheResource());
        }

    // ----- GET API --------------------------------------------------------

    /**
     * Returns the list of caches in a Service, or the entire cluster if a service is not provided.
     *
     * @return the response object.
     */
    public Response get(HttpRequest request)
        {
        QueryBuilder queryBuilder = createQueryBuilder(request)
                .withBaseQuery(CACHES_QUERY)
                .withService(getService(request));

        URI                 uriCurrent = getCurrentUri(request);
        EntityMBeanResponse response   = getResponseBodyForMBeanCollection(request, queryBuilder, new CacheResource(),
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
     * Returns the list of all CacheMBean members in the cluster.
     *
     * @return the response object.
     */
    public Response getAllCacheMembers(HttpRequest request)
        {
        QueryBuilder queryBuilder = createQueryBuilder(request)
                .withBaseQuery(CACHES_QUERY)
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

        QueryBuilder queryBuilder = createQueryBuilder(request)
                .withBaseQuery(CACHES_QUERY)
                .withService(sServiceName);

        return getResponseBodyForMBeanCollection(request, queryBuilder, new CacheResource(), NAME,
                mapQuery, uriParent, getSubUri(uriParent, CACHES), uriCurrent, mapArguments);
        }
    }
