/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.management.internal.resources;

import com.tangosol.coherence.management.internal.EntityMBeanResponse;

import com.tangosol.net.management.MBeanAccessor.QueryBuilder;

import com.tangosol.util.Filter;

import java.net.URI;

import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import javax.ws.rs.QueryParam;

import javax.ws.rs.WebApplicationException;

import javax.ws.rs.core.Response;

import static java.lang.String.format;

/**
 * Handles management API requests for a cache in a service.
 *
 * @author sr  2017.08.29
 * @since 12.2.1.4.0
 */
public class CacheResource extends AbstractManagementResource
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a CacheResource.
     *
     * @param resource  the {@link AbstractManagementResource} to be used to initialize the context
     */
    public CacheResource(AbstractManagementResource resource)
        {
        super(resource);
        }

    // ----- GET API --------------------------------------------------------

    /**
     * Return the aggregated metrics of CacheMBean's for a single cache belonging to a Service.
     *
     * @param sCacheName  the cache name
     * @param sRoleName   either a regex to be applied against node ids or a role name
     * @param sCollector  the collector to use instead of the default
     *
     * @return the response object
     */
    @GET
    @Produces(MEDIA_TYPES)
    public Response get(@PathParam(CACHE_NAME) String sCacheName,
                        @QueryParam(ROLE_NAME) String sRoleName,
                        @QueryParam(COLLECTOR) String sCollector)
        {
        // when a user queries for a cache, only name is returned, apart from the aggregated data
        Filter<String>  filterAttributes = getAttributesFilter(NAME, getExcludeList(null));

        EntityMBeanResponse responseEntity = getResponseEntityForMbean(getQuery(sCacheName),
                getParentUri(), getCurrentUri(), filterAttributes, getLinksFilter(), MEMBERS);

        if (responseEntity == null)
            {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

        Map<String, Object> mapResponse = responseEntity.toJson();

        QueryBuilder bldrQuery = createQueryBuilder()
                .withBaseQuery(format(STORAGE_MANAGERS_QUERY, sCacheName))
                .withService(getService());

        // aggregate cache and storage metrics into the response, storage manage metrics is always sent along with cache
        addAggregatedMetricsToResponseMap(sRoleName, sCollector, getQuery(sCacheName), mapResponse);
        addAggregatedMetricsToResponseMap(sRoleName, sCollector, bldrQuery, mapResponse);
        return response(mapResponse);
        }

    // ----- Child Resources ------------------------------------------------

    /**
     * Sub resource for a cache members.
     *
     * @return the cache members child resource
     */
    @Path(MEMBERS)
    public Object getMembersResource()
        {
        return new CacheMembersResource(this);
        }

    // ----- AbstractManagementResource methods -------------------------------------------

    @Override
    protected EntityMBeanResponse getQueryResult(Map mapQuery, Map<String, String> mapArguments, URI uriParent)
        {
        String sCacheName   = mapArguments.get(NAME);

        // While querying for the list of caches, only the cache name is returned
        URI            uriSelf          = getSubUri(uriParent, sCacheName);
        Filter<String> filterAttributes = getAttributesFilter(NAME, getExcludeList(null));
        Filter<String> filterLinks      = getLinksFilter(mapQuery);
        QueryBuilder   bldrQuery        = getQuery(sCacheName);

        EntityMBeanResponse responseEntity =
                getResponseEntityForMbean(bldrQuery, uriParent, uriSelf, filterAttributes, filterLinks, MEMBERS);

        if (responseEntity != null)
            {
            Map<String, Object> mapEntity = responseEntity.getEntity();

            QueryBuilder bldrQueryStorage = createQueryBuilder()
                    .withBaseQuery(format(STORAGE_MANAGERS_QUERY, sCacheName))
                    .withService(getService());

            addAggregatedMetricsToResponseMap("*", null, bldrQuery, mapEntity);
            addAggregatedMetricsToResponseMap("*", null, bldrQueryStorage, mapEntity);

            Object oChildren = mapQuery == null ? null : mapQuery.get(CHILDREN);
            if (oChildren != null && oChildren instanceof Map)
                {
                Map mapChildrenQuery = (Map) oChildren;

                addChildResourceQueryResult(new CacheMembersResource(this), MEMBERS, mapEntity, mapChildrenQuery,
                        mapArguments, uriSelf);
                }
            }

        return responseEntity;
        }

    // ----- CacheResource methods-------------------------------------------

    /**
     * MBean query to retrieve CacheMBeans for the provided cache.
     *
     * @param sCacheName  the cache name
     *
     * @return the MBean query
     */
    protected QueryBuilder getQuery(String sCacheName)
        {
        return createQueryBuilder()
                .withBaseQuery(CACHE_QUERY + sCacheName)
                .withService(getService());
        }
    }
