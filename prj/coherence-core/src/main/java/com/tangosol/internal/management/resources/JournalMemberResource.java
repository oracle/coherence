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
 * Handles management API requests for Coherence cluster member.
 *
 * @author sr  2017.08.21
 * @author Jonathan Knight  2022.01.25
 * @since 12.2.1.4.0
 */
public class JournalMemberResource
        extends AbstractManagementResource
    {
    // ----- Routes methods -------------------------------------------------

    @Override
    public void addRoutes(RequestRouter router, String sPathRoot)
        {
        router.addGet(sPathRoot, this::get);

        router.addPost(sPathRoot + "/compact", this::compact);
        router.addPost(sPathRoot + "/" + RESET_STATS, this::resetStatistics);
        }

    // ----- GET API --------------------------------------------------------

    /**
     * Return JournalMBean attributes for a particular journal type(flash/ram) running on a cluster member.
     *
     * @return the response object
     */
    public Response get(HttpRequest request)
        {
        String sMemberKey   = request.getFirstPathParameter(MEMBER_KEY);
        String sJournalType = request.getFirstPathParameter(JOURNAL_TYPE);
        return response(getResponseEntityForMbean(request, getQuery(request, sMemberKey, sJournalType)));
        }

    // ----- POST API(Operations) -------------------------------------------

    /**
     * Call "compact" operation on a JournalMBean.
     *
     * @return the response object
     */
    public Response compact(HttpRequest request)
        {
        String              sMemberKey   = request.getFirstPathParameter(MEMBER_KEY);
        String              sJournalType = request.getFirstPathParameter(JOURNAL_TYPE);
        Map<String, Object> entity       = getJsonBody(request);
        Object              oRegular     = entity == null ? null : entity.get("regular");
        boolean             fRegular     = oRegular != null && Boolean.parseBoolean(oRegular.toString());

        return executeMBeanOperation(request, getQuery(request, sMemberKey, sJournalType), "compact",
                                     new Object[]{fRegular}, new String[] {boolean.class.getName()});
        }

    /**
     * Call "resetStatistics" operation on a JournalMBean.
     *
     * @return the response object
     */
    public Response resetStatistics(HttpRequest request)
        {
        String sMemberKey   = request.getFirstPathParameter(MEMBER_KEY);
        String sJournalType = request.getFirstPathParameter(JOURNAL_TYPE);
        return executeMBeanOperation(request, getQuery(request, sMemberKey, sJournalType), RESET_STATS, null, null);
        }


    // ----- AbstractManagementResource methods------------------------------

    @Override
    protected EntityMBeanResponse getQueryResult(HttpRequest         request,
                                                 URI                 uriParent,
                                                 URI                 uriCurrent,
                                                 Map<String, Object> mapQuery,
                                                 Map<String, String> mapArguments)
        {
        return getLinksOnlyResponseBody(request, uriParent, getSubUri(uriParent, mapArguments.get(MEMBER_KEY)));
        }

    // ----- JournalMemberResource methods-----------------------------------

    /**
     * MBean query to retrieve JournalMBean for the provided journal type running in a Cluster member.
     *
     * @param sMemberKey    the member key, can be a member name or node Id
     * @param sJournalType  the journal type(ram/flash)
     *
     * @return the MBean query builder
     */
    protected QueryBuilder getQuery(HttpRequest request, String sMemberKey, String sJournalType)
        {
        return createQueryBuilder(request).withBaseQuery(MAP_JOURNAL_URL_TO_MBEAN_QUERY.get(sJournalType))
                .withMember(sMemberKey);
        }
    }
