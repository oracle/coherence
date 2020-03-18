/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.management.internal.resources;

import com.tangosol.coherence.management.internal.EntityMBeanResponse;

import com.tangosol.net.management.MBeanAccessor.QueryBuilder;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Map;

/**
 * Handles management API requests for a single Coherence reporter member.
 *
 * @author tam 2018.03.14
 * @since 12.2.1.4.0
 */
public class ReporterMemberResource
     extends AbstractManagementResource
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a ClusterMemberResource.
     *
     * @param resource  the {@link AbstractManagementResource} to be used to initialize the context
     */
    public ReporterMemberResource(AbstractManagementResource resource)
        {
        super(resource);
        }

    // ----- GET API --------------------------------------------------------

    /**
     * Return ReporterMBean attributes for a cluster member.
     *
     * @param sMemberKey  the member key, can be a member name or node Id
     *
     * @return the response object
     */
    @GET
    @Produces(MEDIA_TYPES)
    public Response get(@PathParam(MEMBER_KEY) String sMemberKey)
        {
        return response(getResponseEntityForMbean(getQuery(sMemberKey)));
        }

    // ----- POST API(Update) -----------------------------------------------

    /**
     * Update a ReporterMBean with the parameters present in the input entity map.
     *
     * @param sMemberKey  the member key, can be a member name or node Id
     * @param entity      the input entity map containing the updated attributes
     *
     * @return the response object
     */
    @POST
    @Produces(MEDIA_TYPES)
    @Consumes(MEDIA_TYPES)
    public Response updateAttributes(@PathParam(MEMBER_KEY) String sMemberKey,
                                     Map<String, Object> entity)
        {
        return update(entity, getQuery(sMemberKey));
        }

    // ----- POST API(Execute) -------------------------------------------------------
    
    /**
     * Call start, stop or resetStatistics operation on ReporterMBean.
     *
     * @return the response object
     */
    @POST
    @Produces(MEDIA_TYPES)
    @Path("{operationName:start|stop|resetStatistics}")
    public Response shutdownCluster(@PathParam(MEMBER_KEY)     String sMemberKey,
                                    @PathParam(OPERATION_NAME) String sOperationName)
        {
        return executeMBeanOperation(getQuery(sMemberKey), sOperationName, null, null);
        }

    // ----- ReporterMemberResource methods----------------------------------

    /**
     * Return the NodeMBean query for the provided member.
     *
     * @param sMemberKey  the member key
     *
     * @return the MBean query
     */
    protected QueryBuilder getQuery(String sMemberKey)
        {
        return createQueryBuilder()
                .withBaseQuery(REPORTER_MEMBERS_QUERY)
                .withMember(sMemberKey);
        }

    // ----- AbstractManagementResource methods -------------------------------------------

    @Override
    protected EntityMBeanResponse getQueryResult(Map mapQuery, Map<String, String> mapArguments, URI uriParent)
        {
        String              sMemberKey = mapArguments.get(MEMBER_KEY);
        URI                 uriSelf    = getSubUri(uriParent, sMemberKey);

        return getLinksOnlyResponseBody(uriParent, uriSelf, getLinksFilter(mapQuery));
        }
    }
