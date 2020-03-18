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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import javax.ws.rs.core.Response;

/**
 * Handles management API requests for a Cache Members(list of cache Mbeans of same name).
 *
 * @author sr 2017.08.29
 * @since 12.2.1.4.0
 */
public class CacheMembersResource extends AbstractManagementResource
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a CacheMembersResource.
     *
     * @param resource  the {@link AbstractManagementResource} to be used to initialize the context
     */
    public CacheMembersResource(AbstractManagementResource resource)
        {
        super(resource);
        }

    // ----- GET API --------------------------------------------------------

    /**
     * Return list of CacheMBean objects for the provided cache.
     *
     * @param sCacheName  the cache name
     * @param sTier       the tier of the cache members
     *
     * @return the response object
     */
    @GET
    @Produces(MEDIA_TYPES)
    public Response get(@PathParam(CACHE_NAME) String sCacheName,
                        @QueryParam(TIER)      String sTier)
        {
        StringBuilder bldrQueryStr = new StringBuilder(CACHE_QUERY + sCacheName);
        bldrQueryStr.append(sTier == null ? "" : "," + TIER + "=" + sTier);

        QueryBuilder bldrQuery = createQueryBuilder().withBaseQuery(bldrQueryStr.toString())
                .withService(getService());

        return response(getResponseBodyForMBeanCollection(bldrQuery, new CacheMemberResource(this),
                null, null, getParentUri(), getCurrentUri()));
        }

    // ----- Child Resources ------------------------------------------------

    /**
     * Sub resource for a single cache member.
     *
     * @return the cache member child resource
     */
    @Path("{" + MEMBER_KEY + "}")
    public Object getMemberResource()
        {
        return new CacheMemberResource(this);
        }

    // ----- AbstractManagementResource methods -------------------------------------------

    @Override
    protected EntityMBeanResponse getQueryResult(Map mapQuery, Map<String, String> mapArguments, URI uriParent)
        {
        String        sServiceName = mapArguments.get(SERVICE_NAME);
        String        sCacheName   = mapArguments.get(NAME);
        String        sBaseQuery   = CACHE_MEMBERS_WITH_SERVICE_QUERY + sCacheName;

        QueryBuilder bldrQuery = createQueryBuilder().withBaseQuery(sBaseQuery).withService(sServiceName);

        return getResponseBodyForMBeanCollection(bldrQuery, new CacheMemberResource(this), mapQuery,
                mapArguments, uriParent, getSubUri(uriParent, MEMBERS));
        }
    }
