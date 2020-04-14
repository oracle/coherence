/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.management.internal.resources;

import com.tangosol.coherence.management.internal.EntityMBeanResponse;

import com.tangosol.net.management.MBeanAccessor.QueryBuilder;

import com.tangosol.util.Filter;

import java.net.URI;

import java.util.Collections;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import javax.ws.rs.core.Response;

/**
 * API resource for C*W MBeans for an application.
 *
 * @author sr  2017.09.11
 * @since 12.2.1.4.0
 */
public class CWebApplicationResource
        extends AbstractManagementResource
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a CWebApplicationResource.
     *
     * @param resource  the {@link AbstractManagementResource} to be used to initialize the context
     */
    public CWebApplicationResource(AbstractManagementResource resource)
        {
        super(resource);
        }

    // -------------------------- GET API ---------------------------------------------------

    /**
     * Return the response for a single C*W application in the cluster.
     *
     * @param sApplicationId  the application identifier
     * @param sRoleName       either a regex to be applied against node ids or a role name
     * @param sCollector      the collector to use instead of the default
     *
     * @return the response object
     */
    @GET
    @Produces(MEDIA_TYPES)
    public Response getApplicationResponse(@PathParam(APPLICATION_ID) String sApplicationId,
                                           @QueryParam(ROLE_NAME)     String sRoleName,
                                           @QueryParam(COLLECTOR)     String sCollector)
        {
        EntityMBeanResponse responseEntity = getLinksOnlyResponseBody(getParentUri(), getCurrentUri(), MEMBERS);
        Map<String, Object> responseMap    = responseEntity.toJson();

        QueryBuilder bldrQuery = createQueryBuilder().withBaseQuery(CWEB_APPLICATION_QUERY + sApplicationId);

        addAggregatedMetricsToResponseMap(sRoleName, sCollector, bldrQuery,
                responseMap);

        return response(responseMap);
        }
    // -------------------------- Child Resources --------------------------------------------

    /**
     * Sub resource for C*W application members.
     *
     * @return the services child resource
     */
    @Path(MEMBERS)
    public Object getApplicationMembersResource()
        {
        return new CWebMembersResource(this);
        }

    // -------------------------- AbstractManagementResource methods --------------------------------------------

    @Override
    protected EntityMBeanResponse getQueryResult(Map mapQuery, Map<String, String> mapArguments, URI uriParent)
        {
        String              sApplicationId = mapArguments.get("appId");
        URI                 uriSelf        = getSubUri(uriParent, sApplicationId);
        Filter<String>      filterLinks    = getLinksFilter(mapQuery);
        EntityMBeanResponse response       = getLinksOnlyResponseBody(uriParent, uriSelf, filterLinks, MEMBERS);
        Object              oChildren      = getChildrenQuery(mapQuery);
        Map<String, Object> mapEntity      = response.getEntity();

        if (oChildren != null && oChildren instanceof Map)
            {
            addChildResourceQueryResult(new CWebMembersResource(this), MEMBERS, mapEntity, (Map) oChildren,
                    Collections.singletonMap("appId", sApplicationId), uriSelf);
            }

        return response;
        }
    }
