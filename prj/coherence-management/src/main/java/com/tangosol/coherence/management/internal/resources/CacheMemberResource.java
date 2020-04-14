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

import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import javax.ws.rs.core.Response;

/**
 * Handles management API requests for a Cache Mbean.
 *
 * @author sr  2017.08.29
 * @since 12.2.1.4.0
 */
public class CacheMemberResource extends AbstractManagementResource
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a CacheMemberResource.
     *
     * @param resource  the {@link AbstractManagementResource} to be used to initialize the context
     */
    public CacheMemberResource(AbstractManagementResource resource)
        {
        super(resource);
        }

    // ----- GET API --------------------------------------------------------

    /**
     * Return CacheMBean(s) attributes for a particular cache running on a cluster member.
     *
     * @param sCacheName  the cache name
     * @param sMemberKey  the member key, can be a member name or node Id
     * @param sTier       the tier of the cache member
     * @param sLoader     the id of the ClassLoader
     *
     * @return the response object
     */
    @GET
    @Produces(MEDIA_TYPES)
    public Response get(@PathParam(CACHE_NAME) String sCacheName,
                        @PathParam(MEMBER_KEY) String sMemberKey,
                        @QueryParam(TIER)      String sTier,
                        @QueryParam(LOADER)    String sLoader)
        {
        QueryBuilder bldrQuery = getQuery(sCacheName, sTier, sMemberKey, sLoader);
        return response(getResponseBodyForMBeanCollection(bldrQuery, null, getParentUri(), getCurrentUri()));
        }

    // ----- POST API(Operations) -------------------------------------------

    /**
     * Call "resetStatistics" operation on CacheMBean.
     *
     * @param sCacheName  the cache name
     * @param sMemberKey  the member key, can be a member name or node Id
     * @param sTier       the tier of the cache member
     * @param sLoader     the id of the ClassLoader
     *
     * @return the response object
     */
    @POST
    @Produces(MEDIA_TYPES)
    @Path(RESET_STATS)
    public Response resetStatistics(@PathParam(CACHE_NAME) String sCacheName,
                                    @PathParam(MEMBER_KEY) String sMemberKey,
                                    @QueryParam(TIER)      String sTier,
                                    @QueryParam(LOADER)    String sLoader)
        {
        QueryBuilder bldrQuery = getQuery(sCacheName, sTier, sMemberKey, sLoader);
        return executeMBeanOperation(bldrQuery, RESET_STATS, null, null);
        }

    // ----- POST API(Update) -----------------------------------------------

    /**
     * Update a CacheMBean with the parameters present in the input entity map.
     *
     * @param sCacheName  the cache name
     * @param sMemberKey  the member key, can be a member name or node Id
     * @param sTier       the tier of the cache member
     * @param entity      the input entity map containing the updated attributes
     *
     * @return the response object
     */
    @POST
    @Produces(MEDIA_TYPES)
    @Consumes(MEDIA_TYPES)
    public Response update(@PathParam(CACHE_NAME) String sCacheName,
                           @PathParam(MEMBER_KEY) String sMemberKey,
                           @QueryParam(TIER)      String sTier,
                           @QueryParam(LOADER)    String sLoader,
                           Map<String, Object> entity)
        {
        return update(entity, getQuery(sCacheName, sTier, sMemberKey, sLoader));
        }


    // ----- AbstractManagementResource methods------------------------------

    @Override
    protected EntityMBeanResponse getQueryResult(Map mapQuery, Map<String, String> mapArguments, URI uriParent)
        {
        return getLinksOnlyResponseBody(uriParent, getSubUri(uriParent, mapArguments.get(MEMBER_KEY)),
                getLinksFilter(mapQuery));
        }

    // ----- CacheMemberResource methods-------------------------------------

    /**
     * Create a {@link QueryBuilder} for the cache MBeans.
     *
     * @param sCacheName  the name of the cache
     * @param sTier       the tier
     * @param sMemberKey  the member key
     * @param sLoader     the class loader id
     *
     * @return QueryBuilder for cache MBean
     */
    protected QueryBuilder getQuery(String sCacheName, String sTier, String sMemberKey, String sLoader)
        {
        StringBuilder bldrQuery = new StringBuilder(CACHE_QUERY + sCacheName);
        bldrQuery.append(sTier == null ? "" : "," + TIER + "=" + sTier);
        bldrQuery.append(sLoader == null ? "" : "," + LOADER + "=" + sLoader);

        return createQueryBuilder()
                .withBaseQuery(bldrQuery.toString())
                .withMember(sMemberKey)
                .withService(getService());
        }
    }
