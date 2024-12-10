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

import com.tangosol.net.management.MBeanAccessor.QueryBuilder;

/**
 * Handles management API requests for PagedTopicSubscriberGroup MBean.
 */
public class SubscriberGroupMemberResource
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
     * Return PagedTopicSubscriberGroup MBean(s) attributes for a particular
     * subscriber group running on a cluster member.
     *
     * @return the response object
     */
    public Response get(HttpRequest request)
        {
        return response(getResponseEntityForMbean(request, getQuery(request)));
        }

    // ----- SubscriberGroupMemberResource methods---------------------------

    /**
     * Create a {@link QueryBuilder} for the subscriber group MBeans.
     *
     * @return QueryBuilder for subscriber group MBean
     */
    private QueryBuilder getQuery(HttpRequest request)
        {
        String sTopicName           = request.getFirstPathParameter(TOPIC_NAME);
        String sSubscriberGroupName = request.getFirstPathParameter(SUBSCRIBER_GROUP_NAME);
        String sMemberKey           = request.getFirstPathParameter(MEMBER_KEY);
        String sServiceName         = request.getPathParameters().getFirst(SERVICE_NAME);

        String queryBuilder = String.format(TOPIC_SUBSCRIBER_GROUP_QUERY,
                                            sTopicName,
                                            sSubscriberGroupName);
        return createQueryBuilder(request)
                .withBaseQuery(queryBuilder)
                .withMember(sMemberKey)
                .withService(sServiceName);
        }
    }
