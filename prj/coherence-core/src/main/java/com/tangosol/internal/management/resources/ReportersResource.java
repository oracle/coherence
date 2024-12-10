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
 * Handles management API requests for Coherence reporter members.
 *
 * @author tam 2018.03.14
 * @author Jonathan Knight  2022.01.25
 * @since 12.2.1.4.0
 */
public class ReportersResource
        extends AbstractManagementResource
    {
    // ----- Routes methods -------------------------------------------------

    @Override
    public void addRoutes(RequestRouter router, String sPathRoot)
        {
        router.addGet(sPathRoot, this::get);

        router.addPost(sPathRoot, this::update);
        router.addPost(sPathRoot + "/start", this::start);
        router.addPost(sPathRoot + "/stop", this::stop);
        router.addPost(sPathRoot + "/" + RESET_STATS, this::resetStatistics);

        // child resources
        router.addRoutes(sPathRoot + "/{" + MEMBER_KEY + "}", new ReporterMemberResource());
        }

    // ----- GET API --------------------------------------------------------

    /**
     * Return list of ReporterMBean objects in the cluster.
     *
     * @return the response object
     */
    public Response get(HttpRequest request)
        {
        QueryBuilder queryBuilder = createQueryBuilder(request).withBaseQuery(REPORTER_MEMBERS_QUERY);
        URI          uriCurrent   = getCurrentUri(request);

        return response(getResponseBodyForMBeanCollection(request, queryBuilder, new ReporterMemberResource(),
                null, null, getParentUri(request), uriCurrent, uriCurrent));
        }

    // ----- POST API(Update) -----------------------------------------------

    /**
     * Update a ReporterMBean with the parameters present in the input entity
     * map for all the members.
     *
     * @param request  the {@link HttpRequest}
     *
     * @return the response object
     */
    public Response update(HttpRequest request)
        {
        Map<String, Object> entity = getJsonBody(request);
        return update(request, entity, getQuery(request));
        }

    // ----- POST API(Operations) -------------------------------------------

    /**
     * Call start operation on ReporterMBean for all the members.
     *
     * @param request  the {@link HttpRequest}
     *
     * @return the response object
     */
    public Response start(HttpRequest request)
        {
        return executeMBeanOperation(request, getQuery(request), "start", null, null);
        }

    /**
     * Call stop operation on ReporterMBean for all the members
     *
     * @param request  the {@link HttpRequest}
     *
     * @return the response object
     */
    public Response stop(HttpRequest request)
        {
        return executeMBeanOperation(request, getQuery(request), "stop", null, null);
        }

    /**
     * Call resetStatistics operation on ReporterMBean for all the members.
     *
     * @param request  the {@link HttpRequest}
     *
     * @return the response object
     */
    public Response resetStatistics(HttpRequest request)
        {
        return executeMBeanOperation(request, getQuery(request), RESET_STATS, null, null);
        }

    // ----- AbstractManagementResource methods -------------------------------------------

    @Override
    protected EntityMBeanResponse getQueryResult(HttpRequest         request,
                                                 URI                 uriParent,
                                                 URI                 uriCurrent,
                                                 Map<String, Object> mapQuery,
                                                 Map<String, String> mapArguments)
        {
        QueryBuilder queryBuilder = createQueryBuilder(request).withBaseQuery(REPORTER_MEMBERS_QUERY);

        return getResponseBodyForMBeanCollection(request, queryBuilder, new ReporterMemberResource(),
                mapQuery, mapArguments, uriParent, getSubUri(uriParent, MEMBERS), uriCurrent);
        }

    /**
     * Return the ReporterMBean query for all members.
     *
     * @param request  the {@link HttpRequest}
     *
     * @return the MBean query
     *
     * @since 14.1.2.0
     */
    protected QueryBuilder getQuery(HttpRequest request)
        {
        return createQueryBuilder(request)
                .withBaseQuery(REPORTER_MEMBERS_QUERY);
        }
    }
