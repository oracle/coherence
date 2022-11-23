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

/**
 * Handles management API requests for topics in a service.
 */
public class TopicsResource extends AbstractManagementResource
    {
    // ----- Routes methods -------------------------------------------------

    public void addRoutes(RequestRouter router, String sPathRoot)
        {
        router.addGet(sPathRoot, this::get);

        // child resources
        router.addRoutes(sPathRoot + "/{" + TOPIC_NAME + "}", new TopicResource());
        }

    // ----- GET API --------------------------------------------------------

    /**
     * Returns the list of topics in a Service, or the entire cluster if a service is not provided.
     *
     * @return the response object.
     */
    public Response get(HttpRequest request)
        {
        QueryBuilder queryBuilder = createQueryBuilder(request)
                .withBaseQuery(TOPICS_QUERY)
                .withService(getService(request));

        URI uriCurrent = getCurrentUri(request);
        EntityMBeanResponse response = getResponseBodyForMBeanCollection(request,
                                                                         queryBuilder,
                                                                         new TopicResource(),
                                                                         NAME,
                                                                         null,
                                                                         getParentUri(request),
                                                                         uriCurrent,
                                                                         uriCurrent,
                                                                         null);

        if (response == null && getService(request) != null)
            {
            return Response.status(Response.Status.NOT_FOUND).build();
            }

        return response == null
               ? response(new EntityMBeanResponse())
               : response(response);
        }
    }
