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
 * Handles management API requests for Coherence cluster journal members.
 *
 * @author sr 2017.08.21
 * @author Jonathan Knight  2022.01.25
 * @since 12.2.1.4.0
 */
public class JournalMembersResource
        extends AbstractManagementResource
    {
    // ----- Routes methods -------------------------------------------------

    @Override
    public void addRoutes(RequestRouter router, String sPathRoot)
        {
        router.addGet(sPathRoot, this::get);

        // child resources
        router.addRoutes(sPathRoot + "/{" + MEMBER_KEY + "}", new JournalMemberResource());
        }

    // ----- GET API --------------------------------------------------------

    /**
     * Return list of JournalMBean of the provided type in the cluster.
     *
     * @param request  the {@link HttpRequest}
     *
     * @return the response object
     */
    public Response get(HttpRequest request)
        {
        String sJournalType = request.getFirstPathParameter(JOURNAL_TYPE);
        URI    uriCurrent   = getCurrentUri(request);
        return response(getResponseBodyForMBeanCollection(request, getQuery(request, sJournalType), new JournalMemberResource(),
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
        QueryBuilder queryBuilder = getQuery(request, mapArguments.get(JOURNAL_TYPE));
        return getResponseBodyForMBeanCollection(request, queryBuilder, new JournalMemberResource(),
                mapQuery, mapArguments, uriParent, getSubUri(uriParent, MEMBERS), uriCurrent);
        }

    // ---- JournalMembersResource methods --------------------------------------

    /**
     * MBean query to retrieve JournalMBeans for the provided journal type.
     *
     * @param sJournalType  the journal type(ram/flash)
     *
     * @return the MBean query builder
     */
    protected QueryBuilder getQuery(HttpRequest request, String sJournalType)
        {
        return createQueryBuilder(request).withBaseQuery(MAP_JOURNAL_URL_TO_MBEAN_QUERY.get(sJournalType));
        }
    }
