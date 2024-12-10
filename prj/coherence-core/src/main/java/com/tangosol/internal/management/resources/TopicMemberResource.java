/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.management.resources;

import com.tangosol.internal.http.HttpRequest;
import com.tangosol.internal.http.RequestRouter;
import com.tangosol.internal.http.Response;
import com.tangosol.internal.management.EntityMBeanResponse;
import java.net.URI;

import java.util.HashMap;
import java.util.Map;

import static com.tangosol.net.management.MBeanAccessor.*;

/**
 * Handles management API requests for a Topic Mbean.
 */
public class TopicMemberResource
        extends AbstractManagementResource
    {
    // ----- Routes methods -------------------------------------------------

    @Override
    public void addRoutes(RequestRouter router, String sPathRoot)
        {
        router.addGet(sPathRoot, this::get);
        }

    // ----- GET API --------------------------------------------------------

    /**
     * Return PagedTopicMBean(s) attributes for a particular topic running on a cluster member.
     *
     * @return the response object
     */
    public Response get(HttpRequest request)
        {
        EntityMBeanResponse response = getResponseEntityForMbean(request,
                                             getQuery(request),
                                             getParentUri(request),
                                             getCurrentUri(request),
                                             null);
        if (response == null && getService(request) != null)
            {
            return Response.status(Response.Status.NOT_FOUND).build();
            }

        return response == null
               ? response(new HashMap<>())
               : response(response);
        }

    // ----- AbstractManagementResource methods------------------------------

    @Override
    protected EntityMBeanResponse getQueryResult(HttpRequest request,
                                                 URI                 uriParent,
                                                 URI                 uriCurrent,
                                                 Map<String, Object> mapQuery,
                                                 Map<String, String> mapArguments)
        {
        return getLinksOnlyResponseBody(request,
                                        uriParent,
                                        getSubUri(uriParent, mapArguments.get(MEMBER_KEY)),
                                        getLinksFilter(request, mapQuery));
        }

    // ----- TopicMemberResource methods-------------------------------------

    /**
     * Create a {@link QueryBuilder} for the topic MBeans.
     *
     * @return QueryBuilder for topic MBean
     */
    private QueryBuilder getQuery(HttpRequest request)
        {
        String sTopicName   = request.getFirstPathParameter(TOPIC_NAME);
        String sMemberKey   = request.getFirstPathParameter(MEMBER_KEY);
        String sServiceName = request.getPathParameters().getFirst(SERVICE_NAME);

        String queryBuilder = TOPIC_QUERY + sTopicName;

        return createQueryBuilder(request)
                .withBaseQuery(queryBuilder)
                .withMember(sMemberKey)
                .withService(sServiceName);
        }
    }
