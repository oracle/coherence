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

import java.util.LinkedHashMap;
import java.util.Map;

import java.net.URI;

/**
 * Handles management API requests for a journal type in the cluster.
 *
 * @author sr  2017.08.29
 * @author Jonathan Knight  2022.01.25
 * @since 12.2.1.4.0
 */
public class JournalResource
        extends AbstractManagementResource
    {
    // ----- Routes methods -------------------------------------------------

    @Override
    public void addRoutes(RequestRouter router, String sPathRoot)
        {
        router.addGet(sPathRoot, this::get);

        router.addPost(sPathRoot + "/compact", this::compact);
        router.addPost(sPathRoot + "/" + RESET_STATS, this::resetStatistics);

        // child resources
        router.addRoutes(sPathRoot + "/" + MEMBERS, new JournalMembersResource());
        }

    // ----- GET API --------------------------------------------------------

    /**
     * Return the aggregated metrics of JournalMBean's of a particular type(flash/ram).
     *
     * @return the response object
     */
    public Response get(HttpRequest request)
        {
        String              sJournalType = request.getFirstPathParameter(JOURNAL_TYPE);
        String              sRoleName    = request.getFirstQueryParameter(ROLE_NAME);
        String              sCollector   = request.getFirstQueryParameter(COLLECTOR);
        EntityMBeanResponse response     = getLinksOnlyResponseBody(request, getParentUri(request), getCurrentUri(request), MEMBERS);
        Map<String, Object> responseMap  = response.toJson();

        addAggregatedMetricsToResponseMap(request, sRoleName, sCollector, getQuery(request, sJournalType), responseMap);
        return response(responseMap);
        }

    // ----- POST API(Operations) -------------------------------------------

    /**
     * Call compact on all the Journal MBeans of the speccified type.
     *
     * @return  the response of the operation
     */
    public Response compact(HttpRequest request)
        {
        String              sJournalType = request.getFirstPathParameter(JOURNAL_TYPE);
        Map<String, Object> entity       = getJsonBody(request);
        QueryBuilder        queryBuilder = getQuery(request, sJournalType);
        Object              oRegular     = entity == null ? null : entity.get("regular");
        boolean             fRegular     = oRegular != null && Boolean.parseBoolean(oRegular.toString());

        return executeMBeanOperation(request, queryBuilder, "compact",
                new Object[] {fRegular}, new String[] {boolean.class.getName()});
        }

    /**
     * Call "resetStatistics" operation on a JournalMBean.
     *
     * @param request  the {@link HttpRequest}
     *
     * @return the response object
     */
    public Response resetStatistics(HttpRequest request)
        {
        String sJournalType = request.getFirstPathParameter(JOURNAL_TYPE);
        return executeMBeanOperation(request, getQuery(request, sJournalType), RESET_STATS, null, null);
        }

    // ----- AbstractManagementResource methods -------------------------------------------

    @Override
    @SuppressWarnings("unchecked")
    protected EntityMBeanResponse getQueryResult(HttpRequest         request,
                                                 URI                 uriParent,
                                                 URI                 uriCurrent,
                                                 Map<String, Object> mapQuery,
                                                 Map<String, String> mapArguments)
        {
        Object oChildren = getChildrenQuery(mapQuery);
        if (oChildren instanceof Map)
            {
            Map<String, Object> mapResponse      = new LinkedHashMap<>();
            Map<String, Object>                 mapChildrenQuery = (Map<String, Object>) oChildren;

            addChildResourceQueryResult(request, new JournalMembersResource(),
                                        MEMBERS,
                                        mapResponse,
                                        mapChildrenQuery,
                                        mapArguments
            );

            EntityMBeanResponse responseEntity = new EntityMBeanResponse();
            responseEntity.setEntity(mapResponse);
            return responseEntity;

            }
        return null;
        }

    // ---- JournalMembersResource methods --------------------------------------

    /**
     * MBean query to retrieve JournalMBean for the provided journal type.
     *
     * @param sJournalType  the journal type(ram/flash)
     *
     * @return the MBean query
     */
    protected QueryBuilder getQuery(HttpRequest request, String sJournalType)
        {
        return createQueryBuilder(request).withBaseQuery(MAP_JOURNAL_URL_TO_MBEAN_QUERY.get(sJournalType));
        }
    }
