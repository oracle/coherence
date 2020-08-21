/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.management.internal.resources;

import com.tangosol.coherence.management.internal.EntityMBeanResponse;

import com.tangosol.net.management.MBeanAccessor.QueryBuilder;

import java.net.URI;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import javax.ws.rs.WebApplicationException;

import javax.ws.rs.core.Response;

/**
 * Handles management API requests for caches in a service.
 *
 * @author sr  2017.08.29
 * @since 12.2.1.4.0
 */
public class CachesResource extends AbstractManagementResource
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a CachesResource.
     *
     * @param resource  the {@link AbstractManagementResource} to be used to initialize the context
     */
    public CachesResource(AbstractManagementResource resource)
        {
        super(resource);
        }

    // ----- GET API --------------------------------------------------------

    /**
     * Returns the list of caches in a Service, or the entire cluster if a service is not provided.
     *
     * @return the response object.
     */
    @GET
    @Produces(MEDIA_TYPES)
    public Response get()
        {
        QueryBuilder bldrQuery = createQueryBuilder()
                .withBaseQuery(CACHES_QUERY)
                .withService(getService());

        EntityMBeanResponse response = getResponseBodyForMBeanCollection(bldrQuery, new CacheResource(this),
                NAME, null, getParentUri(), getCurrentUri(), null);

        if (response == null && getService() != null)
            {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
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
    @GET
    @Produces(MEDIA_TYPES)
    @Path(MEMBERS)
    public Response getAllCacheMembers()
        {
        QueryBuilder bldrQuery = createQueryBuilder()
                .withBaseQuery(CACHES_QUERY)
                .withService(getService());

        return response(getResponseBodyForMBeanCollection(bldrQuery, null,
                getParentUri(), getCurrentUri()));
        }

    // ----- Child Resources ------------------------------------------------

    /**
     * Sub resource for a single cache.
     *
     * @return the cache child resource
     */
    @Path("{" + CACHE_NAME + "}")
    public Object getCacheResource()
        {
        return new CacheResource(this);
        }

    // ----- AbstractManagementResource methods -----------------------------

    @Override
    protected EntityMBeanResponse getQueryResult(Map mapQuery, Map<String, String> mapArguments, URI uriParent)
        {
        String sServiceName = mapArguments.get(SERVICE_NAME);

        QueryBuilder bldrQuery = createQueryBuilder()
                .withBaseQuery(CACHES_QUERY)
                .withService(sServiceName);

        return getResponseBodyForMBeanCollection(bldrQuery, new CacheResource(this), NAME, mapQuery, uriParent,
                getSubUri(uriParent, CACHES), mapArguments);
        }
    }
