/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
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
 * Handles management API requests for Coherence cluster members.
 *
 * @author sr  2017.08.21
 * @author Jonathan Knight  2022.01.25
 * @since 12.2.1.4.0
 */
public class ClusterMembersResource
        extends AbstractManagementResource
    {
    // ----- Routes methods -------------------------------------------------

    @Override
    public void addRoutes(RequestRouter router, String sPathRoot)
        {
        router.addGet(sPathRoot, this::get);

        // child resources
        router.addRoutes(sPathRoot + "/{" + MEMBER_KEY + "}", new ClusterMemberResource());
        }

    // ----- GET API --------------------------------------------------------

    /**
     * Return list of NodeMBean objects in the cluster.
     *
     * @return the response object
     */
    public Response get(HttpRequest request)
        {
        QueryBuilder queryBuilder = createQueryBuilder(request)
                .withBaseQuery(CLUSTER_MEMBERS_QUERY);

        URI uriCurrent = getCurrentUri(request);
        return response(getResponseBodyForMBeanCollection(request, queryBuilder, new ClusterMemberResource(),
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
        QueryBuilder queryBuilder = createQueryBuilder(request).withBaseQuery(CLUSTER_MEMBERS_QUERY);

        return getResponseBodyForMBeanCollection(request, queryBuilder, new ClusterMemberResource(),
                mapQuery, mapArguments, uriParent, getSubUri(uriParent, MEMBERS), uriCurrent);
        }
    }
