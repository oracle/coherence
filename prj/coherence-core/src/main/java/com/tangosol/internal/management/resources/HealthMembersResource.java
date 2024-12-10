/*
 * Copyright (c) 2021, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
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
 * API resource for Health MBean members.
 *
 * @author Jonathan Knight  2022.04.04
 * @since 22.06
 */
public class HealthMembersResource
        extends AbstractManagementResource
    {
    // ----- Routes methods -------------------------------------------------

    @Override
    public void addRoutes(RequestRouter router, String sPathRoot)
        {
        router.addGet(sPathRoot, this::get);

        // child resources
        router.addRoutes(sPathRoot + "/{" + MEMBER_KEY + "}", new HealthMemberResource());
        }

    // ----- GET API --------------------------------------------------------

    /**
     * Return a list of Health MBean members in the cluster.
     *
     * @return the response object
     */
    public Response get(HttpRequest request)
        {
        QueryBuilder queryBuilder = createHealthQueryBuilder(request);

        URI uriCurrent = getCurrentUri(request);
        return response(getResponseBodyForMBeanCollection(request, queryBuilder, new HealthMemberResource(),
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
        QueryBuilder queryBuilder = createHealthQueryBuilder(request, mapArguments.get(SUB_TYPE), mapArguments.get(NAME));

        return getResponseBodyForMBeanCollection(request, queryBuilder, new HealthMemberResource(),
                mapQuery, mapArguments, uriParent, getSubUri(uriParent, MEMBERS), uriCurrent);
        }
    }
