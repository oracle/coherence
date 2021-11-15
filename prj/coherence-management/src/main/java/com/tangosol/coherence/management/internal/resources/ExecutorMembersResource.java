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
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import javax.ws.rs.core.Response;

import java.net.URI;

import java.util.Map;

/**
 * API resource for ExecutorMBean members.
 *
 * @author lh  2021.11.05
 * @since 21.12
 */
public class ExecutorMembersResource
        extends AbstractManagementResource
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a ExecutorMembersResource.
     *
     * @param resource  the {@link AbstractManagementResource} to be used to initialize the context
     */
    public ExecutorMembersResource(AbstractManagementResource resource)
        {
        super(resource);
        }

    // ----- GET API --------------------------------------------------------

    /**
     * Return a list of Executor MBean members in the cluster.
     *
     * @return the response object
     */
    @GET
    @Produces(MEDIA_TYPES)
    public Response get()
        {
        QueryBuilder bldrQuery = createQueryBuilder()
                .withBaseQuery(EXECUTORS_QUERY);

        return response(getResponseBodyForMBeanCollection(bldrQuery, new ExecutorMemberResource(this),
                null, null, getParentUri(), getCurrentUri()));
        }

    // ----- Child Resources ------------------------------------------------

    /**
     * Sub resource for a single Executor MBean.  Returns an Executor member
     * with the given MEMBER_KEY, which is the node ID.
     *
     * @return the Executor member child resource
     */
    @Path("{" + MEMBER_KEY + "}")
    public Object getMemberResource()
        {
        return new ExecutorMemberResource(this);
        }

    // ----- AbstractManagementResource methods -------------------------------------------

    @Override
    protected EntityMBeanResponse getQueryResult(Map mapQuery, Map<String, String> mapArguments, URI uriParent)
        {
        QueryBuilder bldrQuery = getQuery(mapArguments.get("name"));

        return getResponseBodyForMBeanCollection(bldrQuery, new ExecutorMemberResource(this),
                mapQuery, mapArguments, uriParent, getSubUri(uriParent, MEMBERS));
        }

    // ---- ExecutorMembersResource methods --------------------------------------

    /**
     * Create a query builder with the provided application id.
     *
     * @param sExecutorId  the executor ID
     *
     * @return hte MBean query builder
     */
    protected QueryBuilder getQuery(String sExecutorId)
        {
        return createQueryBuilder().withBaseQuery(EXECUTOR_QUERY + sExecutorId);
        }
    }
