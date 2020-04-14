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
import javax.ws.rs.Produces;

import javax.ws.rs.core.Response;

/**
 * Handles management API requests for Coherence cluster members.
 *
 * @author sr  2017.08.21
 * @since 12.2.1.4.0
 */
public class ClusterMembersResource
        extends AbstractManagementResource
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a ClusterMembersResource.
     *
     * @param resource  the {@link AbstractManagementResource} to be used to initialize the context
     */
    public ClusterMembersResource(AbstractManagementResource resource)
        {
        super(resource);
        }

    // ----- GET API --------------------------------------------------------

    /**
     * Return list of NodeMBean objects in the cluster.
     *
     * @return the response object
     */
    @GET
    @Produces(MEDIA_TYPES)
    public Response get()
        {
        QueryBuilder bldrQuery = createQueryBuilder()
                .withBaseQuery(CLUSTER_MEMBERS_QUERY);

        return response(getResponseBodyForMBeanCollection(bldrQuery, new ClusterMemberResource(this),
                null, null, getParentUri(), getCurrentUri()));
        }

    // ----- Child Resources ------------------------------------------------

    /**
     * Sub resource for a single cluster member.
     *
     * @return the cluster member child resource
     */
    @Path("{" + MEMBER_KEY + "}")
    public Object getMemberResource() throws Exception
        {
        return new ClusterMemberResource(this);
        }

    // ----- AbstractManagementResource methods -------------------------------------------

    @Override
    protected EntityMBeanResponse getQueryResult(Map mapQuery, Map<String, String> mapArguments, URI uriParent)
        {
        QueryBuilder bldrQuery = createQueryBuilder().withBaseQuery(CLUSTER_MEMBERS_QUERY);

        return getResponseBodyForMBeanCollection(bldrQuery, new ClusterMemberResource(this),
                mapQuery, mapArguments, uriParent, getSubUri(uriParent, MEMBERS));
        }
    }
