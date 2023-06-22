/*
 * Copyright (c) 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.management.internal.resources;

import com.tangosol.coherence.management.internal.EntityMBeanResponse;

import com.tangosol.net.management.MBeanAccessor.QueryBuilder;

import java.util.HashMap;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 * Handles management API requests for storage managers in a service.
 */
public class StorageManagersResource
        extends AbstractManagementResource
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a StorageMAnagersResource.
     *
     * @param resource  the {@link AbstractManagementResource} to be used to initialize the context
     */
    public StorageManagersResource(AbstractManagementResource resource)
        {
        super(resource);
        }

    // ----- GET API --------------------------------------------------------

    /**
     * Returns the list of storage managers in a Service, or the entire cluster if a service is not provided.
     *
     * @return the response object.
     */
    @GET
    @Produces(MEDIA_TYPES)
    public Response get()
        {
        QueryBuilder bldrQuery = createQueryBuilder()
                .withBaseQuery(STORAGE_MANAGERS_ALL_QUERY)
                .withService(getService());

        EntityMBeanResponse response = getResponseBodyForMBeanCollection(bldrQuery, new StorageManagerResource(this),
                CACHE, null, getParentUri(), getCurrentUri(), null);

        if (response == null && getService() != null)
            {
            return Response.status(Response.Status.NOT_FOUND).build();
            }

        return response == null
                ? response(new HashMap<>())
                : response(response);
        }

    // ----- Child Resources ------------------------------------------------

    /**
     * Sub resource for a single storage manager.
     *
     * @return the storage manager child resource
     */
    @Path("{" + CACHE_NAME + "}")
    public Object getStorageManagerResource()
        {
        return new StorageManagerResource(this);
        }
    }
