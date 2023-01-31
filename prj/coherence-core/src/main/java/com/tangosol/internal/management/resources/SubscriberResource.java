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

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Handles management API requests for a subscriber in a service.
 */
public class SubscriberResource
        extends AbstractManagementResource
    {
    // ----- Routes methods -------------------------------------------------

    @Override
    public void addRoutes(RequestRouter router, String sPathRoot)
        {
        router.addGet(sPathRoot, this::get);

        router.addPost(sPathRoot + "/connect", this::connect);
        router.addPost(sPathRoot + "/disconnect", this::disconnect);
        router.addPost(sPathRoot + "/heads", this::heads);
        router.addPost(sPathRoot + "/notifyPopulated", this::notifyPopulated);
        router.addPost(sPathRoot + "/remainingMessages", this::remainingMessages);
        }

    // ----- GET API --------------------------------------------------------

    /**
     * Return PagedTopicSubscriberMBean attributes for a particular subscriber
     * running on a cluster member.
     *
     * @param request  the {@link HttpRequest}
     *
     * @return the response object
     */
    public Response get(HttpRequest request)
        {
        String       sSubscriberId = request.getFirstPathParameter(SUBSCRIBER_ID);
        String       sServiceName  = request.getPathParameters().getFirst(SERVICE_NAME);
        QueryBuilder queryBuilder  = getQuery(request, sSubscriberId, sServiceName);

        return response(getResponseEntityForMbean(request, queryBuilder));
        }

    // ----- POST API -------------------------------------------------------

    /**
     * Call "Connect" operation on PagedTopicSubscriber MBean.
     *
     * @return the response object
     */
    public Response connect(HttpRequest request)
        {
        String       sSubscriberId = request.getFirstPathParameter(SUBSCRIBER_ID);
        String       sServiceName  = request.getPathParameters().getFirst(SERVICE_NAME);
        QueryBuilder queryBuilder  = getQuery(request, sSubscriberId, sServiceName);

        return executeMBeanOperation(request,
                                     queryBuilder,
                                     "connect",
                                     null,
                                     null);
        }

    /**
     * Call "Disconnect" operation on PagedTopicSubscriber MBean.
     *
     * @return the response object
     */
    public Response disconnect(HttpRequest request)
        {
        String       sSubscriberId = request.getFirstPathParameter(SUBSCRIBER_ID);
        String       sServiceName  = request.getPathParameters().getFirst(SERVICE_NAME);
        QueryBuilder queryBuilder  = getQuery(request, sSubscriberId, sServiceName);

        return executeMBeanOperation(request,
                                     queryBuilder,
                                     "disconnect",
                                     null,
                                     null);
        }

    /**
     * Call "Heads" operation on PagedTopicSubscriber MBean.
     *
     * @return the response object
     */
    public Response heads(HttpRequest request)
        {
        String       sSubscriberId = request.getFirstPathParameter(SUBSCRIBER_ID);
        String       sServiceName  = request.getPathParameters().getFirst(SERVICE_NAME);
        QueryBuilder queryBuilder  = getQuery(request, sSubscriberId, sServiceName);

        return response(getResponseFromMBeanOperation(request,
                                                      queryBuilder,
                                                      "heads",
                                                      "heads"));
        }

    /**
     * Call "NotifyPopulated" operation on PagedTopicSubscriber MBean.
     *
     * @return the response object
     */
    public Response notifyPopulated(HttpRequest request)
        {
        String       sSubscriberId = request.getFirstPathParameter(SUBSCRIBER_ID);
        String       sServiceName  = request.getPathParameters().getFirst(SERVICE_NAME);
        String       channel       = request.getFirstQueryParameter("channel");
        QueryBuilder queryBuilder  = getQuery(request, sSubscriberId, sServiceName);
        try
            {
            return executeMBeanOperation(request,
                                         queryBuilder,
                                         "notifyPopulated",
                                         new Object[] {Integer.valueOf(channel)},
                                         new String[] {Integer.class.getName()});
            }
        catch (NumberFormatException e)
            {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Collections.singletonMap("error","Channel query parameter must be an integer."))
                    .build();
            }
        }

    /**
     * Call "RemainingMessages" operation on PagedTopicSubscriber MBean.
     *
     * @return the response object
     */
    public Response remainingMessages(HttpRequest request)
        {
        String       sSubscriberId = request.getFirstPathParameter(SUBSCRIBER_ID);
        String       sServiceName  = request.getPathParameters().getFirst(SERVICE_NAME);
        QueryBuilder queryBuilder  = getQuery(request, sSubscriberId, sServiceName);

        return response(getResponseFromMBeanOperation(request,
                                                      queryBuilder,
                                                      "remainingMessages",
                                                      "remainingMessages"));
        }



    // ----- AbstractManagementResource methods -------------------------------------------

    @Override
    protected EntityMBeanResponse getQueryResult(HttpRequest         request,
                                                 URI                 uriParent,
                                                 URI                 uriCurrent,
                                                 Map<String, Object> mapQuery,
                                                 Map<String, String> mapArguments)
        {
        String sSubscriber = mapArguments.get(SUBSCRIBER_ID);

        // collect attributes from the ObjectNames
        URI            uriSelf        = getSubUri(uriParent, sSubscriber);
        Filter<String> filterLinks    = getLinksFilter(request, mapQuery);
        String         sServiceName   = request.getPathParameters().getFirst(SERVICE_NAME);
        QueryBuilder   queryBuilder   = getQuery(request, sSubscriber, sServiceName);
        Set<String>    setObjectNames = ensureBeanAccessor(request).queryKeys(queryBuilder.build());

        if (setObjectNames == null || setObjectNames.isEmpty())
            {
            return null;
            }

        Filter<String> filterAttributes = getAttributesFilter(request, mapQuery);

        return getResponseEntityForMbean(request, queryBuilder, uriParent, uriSelf, filterAttributes, filterLinks);
        }

    // ----- SubscriberResource methods-------------------------------------------

    /**
     * MBean query to retrieve PagedTopicSubscriberMBeans for the provided subscriber.
     *
     * @param request       the request
     * @param sSubscriberId the subscriber id
     * @param sServiceName  the service name
     *
     * @return the MBean query
     */
    protected QueryBuilder getQuery(HttpRequest request, String sSubscriberId, String sServiceName)
        {
        return createQueryBuilder(request)
                .withBaseQuery(SUBSCRIBER_QUERY + sSubscriberId)
                .withService(sServiceName);
        }
    }
