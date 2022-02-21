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

import java.util.HashMap;
import java.util.Map;

/**
 * API resource for ExecutorMBeans.
 *
 * @author lh  2021.11.05
 * @author Jonathan Knight  2022.01.25
 * @since 21.12
 */
public class ExecutorsResource
        extends AbstractManagementResource
    {
    // ----- Routes methods -------------------------------------------------

    @Override
    public void addRoutes(RequestRouter router, String sPathRoot)
        {
        router.addGet(sPathRoot, this::get);

        // child resources
        router.addRoutes(sPathRoot + "/" + MEMBERS, new ExecutorMembersResource());
        router.addRoutes(sPathRoot + "/{" + NAME + "}", new ExecutorResource());
        }

    // -------------------------- GET API ---------------------------------------------------

    /**
     * Return a list of all ExecutorMBean objects in the cluster.
     *
     * @return the response object
     */
    public Response get(HttpRequest request)
        {
        QueryBuilder queryBuilder = createQueryBuilder(request)
                .withBaseQuery(EXECUTORS_QUERY);

        URI      uriCurrent = getCurrentUri(request);
        Response response   = response(getResponseBodyForMBeanCollection(request, queryBuilder, new ExecutorResource(),
                NAME, null, getParentUri(request), uriCurrent, uriCurrent, null));

        return response == null
                ? response(new HashMap<>())
                : response;
        }

    // -------------------------- AbstractManagementResource methods --------------------------------------------

    @Override
    protected EntityMBeanResponse getQueryResult(HttpRequest         request,
                                                 URI                 uriParent,
                                                 URI                 uriCurrent,
                                                 Map<String, Object> mapQuery,
                                                 Map<String, String> mapArguments)
        {
        QueryBuilder queryBuilder = createQueryBuilder(request).withBaseQuery(EXECUTORS_QUERY);

        return getResponseBodyForMBeanCollection(request, queryBuilder, new ExecutorResource(), NAME, mapQuery, uriParent,
                getSubUri(uriParent, EXECUTORS), uriCurrent, mapArguments);
        }
    }
