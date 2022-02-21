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
 * Handles management API requests for members of a service.
 *
 * @author sr 2017.08.29
 * @author Jonathan Knight  2022.01.25
 * @since 12.2.1.4.0
 */
public class ServiceMembersResource extends AbstractManagementResource
    {
    // ----- Routes methods -------------------------------------------------

    @Override
    public void addRoutes(RequestRouter router, String sPathRoot)
        {
        router.addGet(sPathRoot, this::get);

        // child resources
        router.addRoutes(sPathRoot + "/{" + MEMBER_KEY + "}", new ServiceMemberResource());
        }

    // ----- GET API --------------------------------------------------------

    /**
     * Return list of ServiceMBean objects for the provided service.
     *
     * @param request  the {@link HttpRequest}
     *
     * @return the response object
     */
    public Response get(HttpRequest request)
        {
        String sServiceName = request.getFirstPathParameter(SERVICE_NAME);
        URI    uriCurrent   = getCurrentUri(request);
        return response(getResponseBodyForMBeanCollection(request, getQuery(request, sServiceName), new ServiceMemberResource(),
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
        QueryBuilder queryBuilder = getQuery(request, mapArguments.get(SERVICE_NAME));

        return getResponseBodyForMBeanCollection(request, queryBuilder, new ServiceMemberResource(),
                                                 mapQuery, mapArguments, uriParent, getSubUri(uriParent, MEMBERS), uriCurrent);
        }

    // ----- ServiceMemberResource methods ----------------------------------

    /**
     * MBean query to retrieve ServiceMBeans for the provided service.
     *
     * @param sServiceName  the service name
     *
     * @return the MBean query builder
     */
    protected QueryBuilder getQuery(HttpRequest request, String sServiceName)
        {
        return createQueryBuilder(request).withBaseQuery(SERVICE_MEMBERS_QUERY + sServiceName);
        }
    }
