/*
 * Copyright (c) 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.management.internal.resources;

import com.tangosol.coherence.management.internal.EntityMBeanResponse;

import com.tangosol.net.management.MBeanAccessor.QueryBuilder;

import com.tangosol.util.Filter;

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
 * API resource for an ExecutorMBean.
 *
 * @author lh  2021.11.05
 * @since 21.12
 */
public class ExecutorResource
        extends AbstractManagementResource
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a ExecutorResource.
     *
     * @param resource  the {@link AbstractManagementResource} to be used to initialize the context
     */
    public ExecutorResource(AbstractManagementResource resource)
        {
        super(resource);
        }

    // -------------------------- GET API ---------------------------------------------------

    /**
     * Return the response for a single executor in the cluster.
     *
     * @param sName  the executor name
     *
     * @return the response object
     */
    @GET
    @Produces(MEDIA_TYPES)
    public Response get(@PathParam(NAME) String sName)
        {
        QueryBuilder        bldrQuery      = createQueryBuilder().withBaseQuery(EXECUTOR_QUERY + sName);
        EntityMBeanResponse responseEntity = createResponse(getParentUri(), getCurrentUri(), getLinksFilter());
        Map<String, Object> mapResponse    = responseEntity.toJson();

        // aggregate executor metrics into the response
        addAggregatedMetricsToResponseMap("*", null, bldrQuery, mapResponse);

        return response(mapResponse);
        }

    // ----- POST API (Operations) ------------------------------------------

    /**
     * Call "resetStatistics" operation on ExecutorMBean.
     *
     * @param sName  the executor name
     *
     * @return the response object
     */
    @POST
    @Produces(MEDIA_TYPES)
    @Consumes(MEDIA_TYPES)
    @Path(RESET_STATS)
    public Response resetStatistics(@PathParam(NAME) String sName)
        {
        return executeMBeanOperation(getQuery(sName), RESET_STATS, null, null);
        }

    /**
     * Update an ExecutorMBean with the parameters present in the input entity map.
     *
     * @param sName   the executor name
     * @param entity  the input entity map containing the updated attributes
     *
     * @return the response object
     */
    @POST
    @Produces(MEDIA_TYPES)
    @Consumes(MEDIA_TYPES)
    public Response executeOperation(@PathParam(NAME) String sName,
                                     Map<String, Object> entity)
        {
        return update(entity, getQuery(sName));
        }

    // -------------------------- Child Resources --------------------------------------------

    /**
     * Sub resource for ExecutorMBean members.
     *
     * @return the services child resource
     */
    @Path(MEMBERS)
    public Object getMembersResource()
        {
        return new ExecutorMembersResource(this);
        }

    // -------------------------- AbstractManagementResource methods --------------------------------------------

    @Override
    protected EntityMBeanResponse getQueryResult(Map mapQuery, Map<String, String> mapArguments, URI uriParent)
        {
        String         sName       = mapArguments.get(NAME);
        URI            uriSelf     = getSubUri(uriParent, sName);
        Filter<String> filterLinks = getLinksFilter(mapQuery);

        EntityMBeanResponse responseEntity = createResponse(uriParent, uriSelf, filterLinks);
        Map<String, Object> mapEntity      = responseEntity.getEntity();
        QueryBuilder        bldrQuery      = createQueryBuilder().withBaseQuery(EXECUTOR_QUERY + sName);

        addAggregatedMetricsToResponseMap("*", null, bldrQuery, mapEntity);

        Object oChildren = mapQuery == null ? null : mapQuery.get(CHILDREN);

        if (oChildren != null && oChildren instanceof Map)
            {
            Map mapChildrenQuery = (Map) oChildren;

            addChildResourceQueryResult(new ExecutorMembersResource(this), MEMBERS, mapEntity,
                    mapChildrenQuery, mapArguments, uriSelf);
            }

        return responseEntity;
        }

    /**
     * MBean query to retrieve ExecutorMBean for the provided executor.
     *
     * @param sName  the executor name
     *
     * @return the MBean query
     */
    protected QueryBuilder getQuery(String sName)
        {
        return createQueryBuilder()
                .withBaseQuery(EXECUTOR_QUERY + sName);
        }
    }
