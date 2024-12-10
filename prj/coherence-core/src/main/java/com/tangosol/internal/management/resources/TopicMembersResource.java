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
import java.net.URI;

import com.tangosol.net.management.MBeanAccessor.QueryBuilder;

/**
 * Handles management API requests for a Topic Members (list of topic Mbeans of same name).
 */
public class TopicMembersResource
        extends AbstractManagementResource
    {
    // ----- Routes methods -------------------------------------------------

    @Override
    public void addRoutes(RequestRouter router, String sPathRoot)
        {
        router.addGet(sPathRoot, this::get);

        //child resources
        router.addRoutes(sPathRoot + "/{" + MEMBER_KEY + "}", new TopicMemberResource());
        }

    // ----- GET API --------------------------------------------------------

    /**
     * Return list of PagedTopicMBean objects for the provided topic.
     *
     * @return the response object
     */
    public Response get(HttpRequest request)
        {
        String sTopicName      = request.getFirstPathParameter(TOPIC_NAME);
        String queryBuilderStr = TOPIC_QUERY + sTopicName;

        QueryBuilder queryBuilder = createQueryBuilder(request)
                .withBaseQuery(queryBuilderStr)
                .withService(getService(request));

        URI uriCurrent = getCurrentUri(request);
        return response(getResponseBodyForMBeanCollection(request,
                                                          queryBuilder,
                                                          new TopicMemberResource(),
                                                          null,
                                                          null,
                                                          getParentUri(request),
                                                          uriCurrent,
                                                          uriCurrent));
        }
    }
