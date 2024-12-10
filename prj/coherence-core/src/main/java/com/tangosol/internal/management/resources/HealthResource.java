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

import com.tangosol.net.management.MBeanAccessor.QueryBuilder;

import com.tangosol.net.management.MBeanHelper;
import com.tangosol.util.Filter;

import com.tangosol.util.filter.AlwaysFilter;

import java.net.URI;

import java.util.Collections;
import java.util.Map;

/**
 * The management resource for a specific health check name.
 *
 * @author Jonathan Knight  2022.04.04
 * @since 22.06
 */
public class HealthResource
        extends AbstractManagementResource
    {
    // ----- Routes methods -------------------------------------------------

    @Override
    public void addRoutes(RequestRouter router, String sPathRoot)
        {
        router.addGet(sPathRoot, this::get);

        // child resources
        router.addRoutes(sPathRoot + "/" + MEMBERS, new HealthMembersResource());
        }

    public Response get(HttpRequest request)
        {
        String              sRoleName    = request.getFirstQueryParameter(ROLE_NAME);
        String              sCollector   = request.getFirstQueryParameter(COLLECTOR);
        QueryBuilder        queryBuilder = createHealthQueryBuilder(request);
        EntityMBeanResponse response     = getResponseEntityForMbean(request, queryBuilder, getParentUri(request),
                getCurrentUri(request), AlwaysFilter.INSTANCE(), getLinksFilter(request));

        if (response == null) // if there is no entity, the health check could not be found
            {
            return Response.status(Response.Status.NOT_FOUND).build();
            }

        Map<String, Object> responseMap = response.toJson();
        addAggregatedMetricsToResponseMap(request, sRoleName, sCollector, queryBuilder, responseMap);
        return response(responseMap);
        }

    @Override
    @SuppressWarnings("unchecked")
    protected EntityMBeanResponse getQueryResult(HttpRequest         request,
                                                 URI                 uriParent,
                                                 URI                 uriCurrent,
                                                 Map<String, Object> mapQuery,
                                                 Map<String, String> mapArguments)
        {
        String         sName        = MBeanHelper.safeUnquote(mapArguments.get(NAME));
        String         sSubType     = mapArguments.get(SUB_TYPE);
        String         sRoleName    = request.getFirstQueryParameter(ROLE_NAME);
        String         sCollector   = request.getFirstQueryParameter(COLLECTOR);
        URI            uriSelf      = getSubUri(uriParent, sName);
        Filter<String> filterLinks  = getLinksFilter(request, mapQuery);
        QueryBuilder   queryBuilder = createHealthQueryBuilder(request, sSubType, sName);

        EntityMBeanResponse responseEntity =
                getResponseEntityForMbean(request, queryBuilder, uriParent, uriSelf, AlwaysFilter.INSTANCE(), filterLinks, CHILD_LINKS);

        if (responseEntity != null)
            {
            Object              oChildren = getChildrenQuery(mapQuery);
            Map<String, Object> mapEntity = responseEntity.getEntity();

            addAggregatedMetricsToResponseMap(request, sRoleName, sCollector, queryBuilder, mapEntity);

            if (oChildren instanceof Map)
                {
                Map<String, Object> mapChildrenQuery = (Map<String, Object>) oChildren;

                queryBuilder = createHealthQueryBuilder(request, sSubType, sName);

                addChildMbeanQueryResult(request, uriParent, uriCurrent, NAME, queryBuilder, mapEntity, mapChildrenQuery);

                mapArguments = Collections.singletonMap(NAME, sName);

                addChildResourceQueryResult(request, new HealthMembersResource(), MEMBERS, mapEntity,
                                            mapChildrenQuery, mapArguments);
                }
            }

        return responseEntity;
        }

    // ----- constants ------------------------------------------------------

    public static String[] CHILD_LINKS = {MEMBERS};
    }
