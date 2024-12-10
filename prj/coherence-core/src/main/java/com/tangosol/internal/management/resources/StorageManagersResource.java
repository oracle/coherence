/*
 * Copyright (c) 2023, Oracle and/or its affiliates.
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

/**
 * Handles management API requests for storage managers.
 */
public class StorageManagersResource
        extends AbstractManagementResource
    {
    // ----- Routes methods -------------------------------------------------

    @Override
    public void addRoutes(RequestRouter router, String sPathRoot)
        {
        router.addGet(sPathRoot, this::get);

        // child resources
        router.addRoutes(sPathRoot + "/{" + CACHE_NAME + "}", new StorageManagerResource());
        }

    // ----- GET API --------------------------------------------------------

    /**
     * Returns the list of storage managers in a Service, or the entire cluster
     * if a service is not provided.
     *
     * @return the response object.
     */
    public Response get(HttpRequest request)
        {
        QueryBuilder queryBuilder = createQueryBuilder(request)
                .withBaseQuery(STORAGE_MANAGERS_ALL_QUERY)
                .withService(getService(request));

        URI uriCurrent = getCurrentUri(request);
        EntityMBeanResponse response = getResponseBodyForMBeanCollection(request, queryBuilder, new StorageManagerResource(),
                                                                         CACHE, null, getParentUri(request), uriCurrent, uriCurrent, null);

        if (response == null && getService(request) != null)
            {
            return Response.status(Response.Status.NOT_FOUND).build();
            }

        return response == null
               ? response(new HashMap<>())
               : response(response);
        }
    }
