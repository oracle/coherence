/*
 * Copyright (c) 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.management.internal.resources;

import com.tangosol.coherence.management.internal.EntityMBeanResponse;

import com.tangosol.net.management.MBeanAccessor.QueryBuilder;

import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import javax.ws.rs.core.Response;

import java.net.URI;

import java.util.HashMap;
import java.util.Map;

/**
 * API resource for a ExecutorMBean member.
 *
 * @author lh  2021.11.05
 * @since 21.12
 */
public class ExecutorMemberResource
        extends AbstractManagementResource
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a ExecutorMemberResource.
     *
     * @param resource  the {@link AbstractManagementResource} to be used to initialize the context
     */
    public ExecutorMemberResource(AbstractManagementResource resource)
        {
        super(resource);
        }

    // ----- GET API --------------------------------------------------------

    /**
     * Return attributes of an ExecutorMBean with the given MEMBER_KEY,
     * a node ID.
     *
     * @param sNodeId  the node ID
     *
     * @return the response object
     */
    @GET
    @Produces(MEDIA_TYPES)
    public Response get(@PathParam(MEMBER_KEY) String sNodeId)
        {
        Response response = response(getResponseBodyForMBeanCollection(getQuery(sNodeId), null, getParentUri(), getCurrentUri()));

        return response == null
                ? response(new HashMap<>())
                : response;
        }

    // ----- AbstractManagementResource methods------------------------------

    @Override
    protected EntityMBeanResponse getQueryResult(Map mapQuery, Map<String, String> mapArguments, URI uriParent)
        {
        String sNodeId = mapArguments.get(MEMBER_KEY);
        URI    uriSelf = getSubUri(uriParent, sNodeId);

        return getLinksOnlyResponseBody(uriParent, uriSelf, getLinksFilter(mapQuery));
        }

    // ---- ExecutorMemberResource methods --------------------------------------

    /**
     * The ExecutorMBean query.
     *
     * @param sNodeId  the member node ID
     *
     * @return the MBean query builder
     */
    protected QueryBuilder getQuery(String sNodeId)
        {
        return createQueryBuilder()
                .withBaseQuery(EXECUTORS_QUERY)
                .withMember(sNodeId);
        }
    }
