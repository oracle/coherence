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

import java.net.URI;

import java.util.Map;

/**
 * API resource for ExecutorMBean members.
 *
 * @author lh  2021.11.05
 * @author Jonathan Knight  2022.01.25
 * @since 21.12
 */
public class ExecutorMembersResource
        extends AbstractManagementResource
    {
    // ----- Routes methods -------------------------------------------------

    @Override
    public void addRoutes(RequestRouter router, String sPathRoot)
        {
        router.addGet(sPathRoot, this::get);

        // child resources
        router.addRoutes(sPathRoot + "/{" + MEMBER_KEY + "}", new ExecutorMemberResource());
        }

    // ----- GET API --------------------------------------------------------

    /**
     * Return a list of Executor MBean members in the cluster.
     *
     * @return the response object
     */
    public Response get(HttpRequest request)
        {
        QueryBuilder queryBuilder = createQueryBuilder(request)
                .withBaseQuery(EXECUTORS_QUERY);

        URI uriCurrent = getCurrentUri(request);
        return response(getResponseBodyForMBeanCollection(request, queryBuilder, new ExecutorMemberResource(),
                null, null, getParentUri(request), uriCurrent, uriCurrent));
        }

    // ----- AbstractManagementResource methods -------------------------------------------

    @Override
    protected EntityMBeanResponse getQueryResult(HttpRequest         request,
                                                 URI                 uriParent,
                                                 URI                 uriCurrent,
                                                 Map<String, Object> mapQuery,
                                                 Map<String, String> mapArguments)
        {
        QueryBuilder queryBuilder = getQuery(request, mapArguments.get("name"));

        return getResponseBodyForMBeanCollection(request, queryBuilder, new ExecutorMemberResource(),
                mapQuery, mapArguments, uriParent, getSubUri(uriParent, MEMBERS), uriCurrent);
        }

    // ---- ExecutorMembersResource methods --------------------------------------

    /**
     * Create a query builder with the provided application id.
     *
     * @param sExecutorId  the executor ID
     *
     * @return hte MBean query builder
     */
    protected QueryBuilder getQuery(HttpRequest request, String sExecutorId)
        {
        return createQueryBuilder(request).withBaseQuery(EXECUTOR_QUERY + sExecutorId);
        }
    }
