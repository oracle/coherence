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

import java.util.HashMap;
import java.util.Map;

/**
 * API resource for ExecutorMBeans.
 *
 * @author lh  2021.11.05
 * @since 21.12
 */
public class ExecutorsResource
        extends AbstractManagementResource
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a ExecutorsResource.
     *
     * @param resource  the {@link AbstractManagementResource} to be used to initialize the context
     */
    public ExecutorsResource(AbstractManagementResource resource)
        {
        super(resource);
        }

    // -------------------------- GET API ---------------------------------------------------

    /**
     * Return a list of all ExecutorMBean objects in the cluster.
     *
     * @return the response object
     */
    @GET
    @Produces(MEDIA_TYPES)
    public Response get()
        {
        QueryBuilder bldrQuery = createQueryBuilder()
                .withBaseQuery(EXECUTORS_QUERY);

        Response response = response(getResponseBodyForMBeanCollection(bldrQuery, new ExecutorResource(this),
                NAME, null,  getParentUri(), getCurrentUri(), null));

        return response == null
                ? response(new HashMap<>())
                : response;
        }

    /**
     * Returns a list of all ExecutorMBean members in the cluster.
     *
     * @return the executors child resource
     */
    @Path(MEMBERS)
    public Object getMembersResource()
        {
        return new ExecutorMembersResource(this);
        }

    // -------------------------- Child Resources --------------------------------------------

    /**
     * Sub resource for an executor with the given name.
     *
     * @return the executor child resource
     */
    @Path("{" + NAME + "}")
    public Object getExecutorResource()
        {
        return new ExecutorResource(this);
        }

    // -------------------------- AbstractManagementResource methods --------------------------------------------

    @Override
    protected EntityMBeanResponse getQueryResult(Map mapQuery, Map<String, String> mapArguments, URI uriParent)
        {
        QueryBuilder bldrQuery = createQueryBuilder().withBaseQuery(EXECUTORS_QUERY);

        return getResponseBodyForMBeanCollection(bldrQuery, new ExecutorResource(this), NAME, mapQuery, uriParent,
                getSubUri(uriParent, EXECUTORS), mapArguments);
        }
    }
