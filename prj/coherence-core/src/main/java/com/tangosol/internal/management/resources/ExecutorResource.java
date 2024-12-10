/*
 * Copyright (c) 2021, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.management.resources;

import com.tangosol.internal.http.HttpRequest;
import com.tangosol.internal.http.RequestRouter;
import com.tangosol.internal.http.Response;

import com.tangosol.internal.management.EntityMBeanResponse;
import com.tangosol.net.management.MBeanAccessor.QueryBuilder;

import com.tangosol.util.Filter;

import java.net.URI;

import java.util.Map;

/**
 * API resource for an ExecutorMBean.
 *
 * @author lh  2021.11.05
 * @author Jonathan Knight  2022.01.25
 * @since 21.12
 */
public class ExecutorResource
        extends AbstractManagementResource
    {
    // ----- Routes methods -------------------------------------------------

    @Override
    public void addRoutes(RequestRouter router, String sPathRoot)
        {
        router.addGet(sPathRoot, this::get);

        router.addPost(sPathRoot, this::update);
        router.addPost(sPathRoot + "/" + RESET_STATS, this::resetStatistics);

        // child resources
        router.addRoutes(sPathRoot + "/" + MEMBERS, new ExecutorMembersResource());
        }

    // -------------------------- GET API ---------------------------------------------------

    /**
     * Return the response for a single executor in the cluster.
     *
     * @return the response object
     */
    public Response get(HttpRequest request)
        {
        String              sName          = request.getFirstPathParameter(NAME);
        QueryBuilder        queryBuilder   = createQueryBuilder(request).withBaseQuery(EXECUTOR_QUERY + sName);
        EntityMBeanResponse responseEntity = createResponse(request, getParentUri(request), getCurrentUri(request), getLinksFilter(request));
        Map<String, Object> mapResponse    = responseEntity.toJson();

        // aggregate executor metrics into the response
        addAggregatedMetricsToResponseMap(request, "*", null, queryBuilder, mapResponse);

        return response(mapResponse);
        }

    // ----- POST API (Operations) ------------------------------------------

    /**
     * Update an ExecutorMBean with the parameters present in the input entity map.
     *
     * @return the response object
     */
    public Response update(HttpRequest request)
        {
        String              sName  = request.getFirstPathParameter(NAME);
        Map<String, Object> entity = getJsonBody(request);
        return update(request, entity, getQuery(request, sName));
        }

    /**
     * Call "resetStatistics" operation on ExecutorMBean.
     *
     * @return the response object
     */
    public Response resetStatistics(HttpRequest request)
        {
        String sName = request.getFirstPathParameter(NAME);
        return executeMBeanOperation(request, getQuery(request, sName), RESET_STATS, null, null);
        }

    // -------------------------- AbstractManagementResource methods --------------------------------------------

    @Override
    @SuppressWarnings("unchecked")
    protected EntityMBeanResponse getQueryResult(HttpRequest         request,
                                                 URI                 uriParent,
                                                 URI                 uriCurrent,
                                                 Map<String, Object> mapQuery,
                                                 Map<String, String> mapArguments)
        {
        String         sName       = mapArguments.get(NAME);
        URI            uriSelf     = getSubUri(uriParent, sName);
        Filter<String> filterLinks = getLinksFilter(request, mapQuery);

        EntityMBeanResponse responseEntity = createResponse(request, uriParent, uriSelf, filterLinks);
        Map<String, Object> mapEntity      = responseEntity.getEntity();
        QueryBuilder        queryBuilder      = createQueryBuilder(request).withBaseQuery(EXECUTOR_QUERY + sName);

        addAggregatedMetricsToResponseMap(request, "*", null, queryBuilder, mapEntity);

        Object oChildren = mapQuery == null ? null : mapQuery.get(CHILDREN);

        if (oChildren instanceof Map)
            {
            Map<String, Object> mapChildrenQuery = (Map<String, Object>) oChildren;

            addChildResourceQueryResult(request, new ExecutorMembersResource(), MEMBERS, mapEntity,
                                        mapChildrenQuery, mapArguments);
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
    protected QueryBuilder getQuery(HttpRequest request, String sName)
        {
        return createQueryBuilder(request)
                .withBaseQuery(EXECUTOR_QUERY + sName);
        }
    }
