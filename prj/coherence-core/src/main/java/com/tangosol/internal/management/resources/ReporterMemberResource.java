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
 * Handles management API requests for a single Coherence reporter member.
 *
 * @author tam 2018.03.14
 * @author Jonathan Knight  2022.01.25
 * @since 12.2.1.4.0
 */
public class ReporterMemberResource
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
        }

    // ----- GET API --------------------------------------------------------

    /**
     * Return ReporterMBean attributes for a cluster member.
     *
     * @param request  the {@link HttpRequest}
     *
     * @return the response object
     */
    public Response get(HttpRequest request)
        {
        String sMemberKey = request.getFirstPathParameter(MEMBER_KEY);
        return response(getResponseEntityForMbean(request, getQuery(request, sMemberKey)));
        }

    // ----- POST API(Update) -----------------------------------------------

    /**
     * Update a ReporterMBean with the parameters present in the input entity map.
     *
     * @param request  the {@link HttpRequest}
     *
     * @return the response object
     */
    public Response update(HttpRequest request)
        {
        String              sMemberKey = request.getFirstPathParameter(MEMBER_KEY);
        Map<String, Object> entity     = getJsonBody(request);
        return update(request, entity, getQuery(request, sMemberKey));
        }

    // ----- POST API(Execute) -------------------------------------------------------

    /**
     * Call start, stop or resetStatistics operation on ReporterMBean.
     *
     * @param request  the {@link HttpRequest}
     *
     * @return the response object
     */
    public Response start(HttpRequest request)
        {
        String sMemberKey = request.getFirstPathParameter(MEMBER_KEY);
        return executeMBeanOperation(request, getQuery(request, sMemberKey), "start", null, null);
        }

    /**
     * Call start, stop or resetStatistics operation on ReporterMBean.
     *
     * @param request  the {@link HttpRequest}
     *
     * @return the response object
     */
    public Response stop(HttpRequest request)
        {
        String sMemberKey = request.getFirstPathParameter(MEMBER_KEY);
        return executeMBeanOperation(request, getQuery(request, sMemberKey), "stop", null, null);
        }

    /**
     * Call start, stop or resetStatistics operation on ReporterMBean.
     *
     * @param request  the {@link HttpRequest}
     *
     * @return the response object
     */
    public Response resetStatistics(HttpRequest request)
        {
        String sMemberKey = request.getFirstPathParameter(MEMBER_KEY);
        return executeMBeanOperation(request, getQuery(request, sMemberKey), RESET_STATS, null, null);
        }

    // ----- ReporterMemberResource methods----------------------------------

    /**
     * Return the ReporterMBean query for the provided member.
     *
     * @param request     the {@link HttpRequest}
     * @param sMemberKey  the member key
     *
     * @return the MBean query
     */
    protected QueryBuilder getQuery(HttpRequest request, String sMemberKey)
        {
        return createQueryBuilder(request)
                .withBaseQuery(REPORTER_MEMBERS_QUERY)
                .withMember(sMemberKey);
        }

    // ----- AbstractManagementResource methods -------------------------------------------

    @Override
    protected EntityMBeanResponse getQueryResult(HttpRequest         request,
                                                 URI                 uriParent,
                                                 URI                 uriCurrent,
                                                 Map<String, Object> mapQuery,
                                                 Map<String, String> mapArguments)
        {
        String              sMemberKey = mapArguments.get(MEMBER_KEY);
        URI                 uriSelf    = getSubUri(uriParent, sMemberKey);

        return getLinksOnlyResponseBody(request, uriParent, uriSelf, getLinksFilter(request, mapQuery));
        }
    }
