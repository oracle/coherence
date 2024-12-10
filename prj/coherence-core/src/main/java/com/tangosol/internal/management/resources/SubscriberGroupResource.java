/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
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

import com.tangosol.net.management.MBeanAccessor.QueryBuilder;

import com.tangosol.util.Filter;

import java.util.Map;

/**
 * Handles management API requests for a subscriber groups in a service.
 */
public class SubscriberGroupResource
        extends AbstractManagementResource
    {
    // ----- Routes methods -------------------------------------------------

    @Override
    public void addRoutes(RequestRouter router, String sPathRoot)
        {
        router.addGet(sPathRoot, this::get);

        router.addPost(sPathRoot + "/disconnectAll", this::disconnectAll);

        // child resources
        router.addRoutes(sPathRoot + "/{" + MEMBER_KEY + "}", new SubscriberGroupMemberResource());
        router.addRoutes(sPathRoot + "/" + SUBSCRIBERS, new SubscribersResource());
        }

    // ----- GET API --------------------------------------------------------

    /**
     * Returns the list of subscriber group MBeans for the provided subscriber group.
     *
     * @return the response object
     */
    public Response get(HttpRequest request)
        {
        String       sTopicName           = request.getFirstPathParameter(TOPIC_NAME);
        String       sSubscriberGroupName = request.getFirstPathParameter(SUBSCRIBER_GROUP_NAME);
        String       sServiceName         = request.getPathParameters().getFirst(SERVICE_NAME);
        QueryBuilder queryBuilder         = getQuery(request, sTopicName, sSubscriberGroupName, sServiceName);

        return response(getResponseBodyForMBeanCollection(request,
                                                          queryBuilder,
                                                          new SubscriberGroupResource(),
                                                          NODE_ID,
                                                          null,
                                                          getParentUri(request),
                                                          getCurrentUri(request),
                                                          getCurrentUri(request),
                                                          null));
        }

    // ----- POST API -------------------------------------------------------

    /**
     * Call "DisconnectAll" operation on SubscriberGroup MBean.
     *
     * @param request  the {@link HttpRequest}
     *
     * @return the response object
     */
    public Response disconnectAll(HttpRequest request)
        {
        String       sTopicName           = request.getFirstPathParameter(TOPIC_NAME);
        String       sSubscriberGroupName = request.getFirstPathParameter(SUBSCRIBER_GROUP_NAME);
        String       sServiceName         = request.getPathParameters().getFirst(SERVICE_NAME);
        QueryBuilder queryBuilder         = getQuery(request, sTopicName, sSubscriberGroupName, sServiceName);
        return executeMBeanOperation(request,
                                     queryBuilder,
                                     "disconnectAll",
                                     null,
                                     null);
        }

    // ----- AbstractManagementResource methods -------------------------------------------

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    protected EntityMBeanResponse getQueryResult(HttpRequest         request,
                                                 URI                 uriParent,
                                                 URI                 uriCurrent,
                                                 Map<String, Object> mapQuery,
                                                 Map<String, String> mapArguments)
        {
        String sTopicName           = request.getFirstPathParameter(TOPIC_NAME);
        String sSubscriberGroupName = request.getFirstPathParameter(SUBSCRIBER_GROUP_NAME);
        String sNodeId              = mapArguments.get(NODE_ID);

        // collect attributes from the ObjectNames
        URI            uriSelf        = getSubUri(uriParent, sNodeId);
        Filter<String> filterLinks    = getLinksFilter(request, mapQuery);
        String         sServiceName   = request.getPathParameters().getFirst(SERVICE_NAME);
        QueryBuilder   queryBuilder   = createQueryBuilder(request)
                .withBaseQuery(String.format(TOPIC_SUBSCRIBER_GROUP_QUERY, sTopicName, sSubscriberGroupName))
                .withMember(sNodeId)
                .withService(sServiceName);
        Filter<String> filterAttributes = getAttributesFilter(request, mapQuery);

        return getResponseEntityForMbean(request,
                                         queryBuilder,
                                         uriParent,
                                         uriSelf,
                                         filterAttributes,
                                         filterLinks);
        }

    // ----- SubscriberGroupResource methods-------------------------------------------

    /**
     * MBean query to retrieve PagedTopicSubscriberGroupMBeans for the provided subscriber group.
     *
     * @param request               the request
     * @param sSubscriberGroupName  the topic name
     * @param sServiceName          the service name
     *
     * @return the MBean query
     */
    protected QueryBuilder getQuery(HttpRequest request, String sTopicName, String sSubscriberGroupName, String sServiceName)
        {
        return createQueryBuilder(request)
                .withBaseQuery(String.format(TOPIC_SUBSCRIBER_GROUP_QUERY, sTopicName, sSubscriberGroupName))
                .withService(sServiceName);
        }
    }
