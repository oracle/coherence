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

import com.tangosol.net.management.MBeanAccessor.QueryBuilder;

import java.util.HashMap;

/**
 * Handles management API requests for subscriber groups in a service.
 */
public class SubscriberGroupsResource
        extends AbstractManagementResource
    {
    // ----- Routes methods -------------------------------------------------

    @Override
    public void addRoutes(RequestRouter router, String sPathRoot)
        {
        router.addGet(sPathRoot, this::get);

        //child resources
        router.addRoutes(sPathRoot + "/{" + SUBSCRIBER_GROUP_NAME + "}", new SubscriberGroupResource());
        }

    // ----- GET API --------------------------------------------------------

    /**
     * Return list of PagedTopicSubscriberGroupMBean objects for the provided topic.
     *
     * @return the response object
     */
    public Response get(HttpRequest request)
        {
        String sTopicName      = request.getFirstPathParameter(TOPIC_NAME);
        String queryBuilderStr = TOPIC_SUBSCRIBER_GROUPS_QUERY + sTopicName;

        QueryBuilder queryBuilder = createQueryBuilder(request)
                .withBaseQuery(queryBuilderStr)
                .withService(getService(request));

        URI uriCurrent = getCurrentUri(request);
        EntityMBeanResponse response = getResponseBodyForMBeanCollection(request,
                                                                         queryBuilder,
                                                                         NAME,
                                                                         null,
                                                                         getParentUri(request),
                                                                         uriCurrent);
        if (response == null && getService(request) != null)
            {
            return Response.status(Response.Status.NOT_FOUND).build();
            }

        return response == null
               ? response(new HashMap<>())
               : response(response);
        }
    }
