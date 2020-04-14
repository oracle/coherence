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

import javax.ws.rs.core.Response;

/**
 * API resource for a C*W application member.
 *
 * @author sr  2017.09.11
 * @since 12.2.1.4.0
 */
public class CWebMemberResource
        extends AbstractManagementResource
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a CWebMemberResource.
     *
     * @param resource  the {@link AbstractManagementResource} to be used to initialize the context
     */
    public CWebMemberResource(AbstractManagementResource resource)
        {
        super(resource);
        }

    // ----- GET API --------------------------------------------------------

    /**
     * Return attributes of an HttpSessionManager MBean.
     *
     * @param sApplicationId  the C*W application ID
     * @param sMemberKey      the member key, can be a member name or node Id
     *
     * @return the response object
     */
    @GET
    @Produces(MEDIA_TYPES)
    public Response get(@PathParam(APPLICATION_ID) String sApplicationId, @PathParam(MEMBER_KEY) String sMemberKey)
        {
        return response(getResponseEntityForMbean(getQuery(sApplicationId, sMemberKey)));
        }

    // ----- POST API(Operations) -------------------------------------------

    /**
     * Call "resetStatistics" operation on HttpSessionManager.
     *
     * @param sApplicationId  the C*W application ID
     * @param sMemberKey  the member key, can be a member name or node Id
     *
     * @return the response object
     */
    @POST
    @Produces(MEDIA_TYPES)
    @Consumes(MEDIA_TYPES)
    @Path(RESET_STATS)
    public Response resetStatistics(@PathParam(APPLICATION_ID) String sApplicationId,
                                    @PathParam(MEMBER_KEY)     String sMemberKey)
        {
        return executeMBeanOperation(getQuery(sApplicationId, sMemberKey), RESET_STATS, null, null);
        }

    /**
     * Call "clearStoredConfiguration" operation on HttpSessionManager.
     *
     * @param sApplicationId  the C*W application ID
     * @param sMemberKey  the member key, can be a member name or node Id
     *
     * @return the response object
     */
    @POST
    @Produces(MEDIA_TYPES)
    @Consumes(MEDIA_TYPES)
    @Path("clearStoredConfiguration")
    public Response clearStoredConfiguration(@PathParam(APPLICATION_ID) String sApplicationId,
                                             @PathParam(MEMBER_KEY)     String sMemberKey)
        {
        return executeMBeanOperation(getQuery(sApplicationId, sMemberKey), "clearStoredConfiguration", null, null);
        }


    // ----- AbstractManagementResource methods------------------------------

    @Override
    protected EntityMBeanResponse getQueryResult(Map mapQuery, Map<String, String> mapArguments, URI uriParent)
        {
        return getLinksOnlyResponseBody(uriParent, getSubUri(uriParent, mapArguments.get(MEMBER_KEY)),
                getLinksFilter(mapQuery));
        }

    // ---- CWebMemberResource methods --------------------------------------

    /**
     * The HttpSessionManager MBean query.
     *
     * @param sApplicationId  the application Id
     * @param sMemberKey      the member key
     *
     * @return the MBean query builder
     */
    protected QueryBuilder getQuery(String sApplicationId, String sMemberKey)
        {
        return createQueryBuilder()
                .withBaseQuery(CWEB_APPLICATION_QUERY + sApplicationId)
                .withMember(sMemberKey);
        }
    }
