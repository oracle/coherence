/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.management.resources;

import com.tangosol.internal.http.HttpRequest;
import com.tangosol.internal.http.RequestRouter;
import com.tangosol.internal.http.Response;
import com.tangosol.internal.management.Converter;
import com.tangosol.internal.management.EntityMBeanResponse;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.management.MBeanAccessor;

import com.tangosol.util.Filter;
import com.tangosol.util.filter.AlwaysFilter;

import java.net.URI;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Handles management API requests for a view in a service.
 */
public class ViewResource extends AbstractManagementResource
    {
    // ----- Routes methods -------------------------------------------------

    @Override
    public void addRoutes(RequestRouter router, String sPathRoot)
        {
        router.addGet(sPathRoot, this::get);

        // child resources
        router.addRoutes(sPathRoot + "/" + MEMBERS, new ViewMembersResource());
        }

    // ----- GET API --------------------------------------------------------

    /**
     * Return the aggregated metrics of ViewMBean's for a single view belonging to a Service.
     *
     * @param request  the {@link HttpRequest}
     *
     * @return the response object
     */
    public Response get(HttpRequest request)
        {
        String       sViewName     = request.getFirstPathParameter(VIEW_NAME);
        String       sRoleName     = request.getFirstQueryParameter(ROLE_NAME);
        String       sCollector    = request.getFirstQueryParameter(COLLECTOR);
        String       sServiceName  = request.getPathParameters().getFirst(SERVICE_NAME);
        MBeanAccessor.QueryBuilder queryBuilder = getQuery(request, sViewName, sServiceName);
        Set<String> setObjectNames = ensureBeanAccessor(request).queryKeys(queryBuilder.build());

        if (setObjectNames == null || setObjectNames.isEmpty())
            {
            return Response.status(Response.Status.NOT_FOUND).build();
            }

        EntityMBeanResponse responseEntity = createResponse(request, getParentUri(request), getCurrentUri(request), getLinksFilter(request));
        addObjectNamesToResponse(request, setObjectNames, responseEntity, getCurrentUri(request));

        Map<String, Object> mapResponse = responseEntity.toJson();
        // aggregate cache and storage metrics into the response, storage manage metrics is always sent along with cache
        addAggregatedMetricsToResponseMap(request, sRoleName, sCollector, queryBuilder, mapResponse);
        return response(mapResponse);
        }

    // ----- AbstractManagementResource methods -------------------------------------------

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    protected EntityMBeanResponse getQueryResult(HttpRequest         request,
                                                 URI uriParent,
                                                 URI                 uriCurrent,
                                                 Map<String, Object> mapQuery,
                                                 Map<String, String> mapArguments)
        {
        String sViewName = mapArguments.get(NAME);

        // collect attributes from the ObjectNames
        URI            uriSelf        = getSubUri(uriParent, sViewName);
        Filter<String> filterLinks    = getLinksFilter(request, mapQuery);
        String         sServiceName   = request.getPathParameters().getFirst(SERVICE_NAME);
        MBeanAccessor.QueryBuilder queryBuilder   = getQuery(request, sViewName, sServiceName);
        Set<String>    setObjectNames = ensureBeanAccessor(request).queryKeys(queryBuilder.build());

        if (setObjectNames == null || setObjectNames.isEmpty())
            {
            return null;
            }

        EntityMBeanResponse responseEntity = createResponse(request, uriParent, uriSelf, filterLinks);
        addObjectNamesToResponse(request, setObjectNames, responseEntity, uriCurrent);

        Map<String, Object> mapEntity = responseEntity.getEntity();
        addAggregatedMetricsToResponseMap(request, "*", null, queryBuilder, mapEntity);

        Object oChildren = mapQuery == null ? null : mapQuery.get(CHILDREN);
        if (oChildren instanceof Map)
            {
            Map mapChildrenQuery = (Map) oChildren;

            addChildResourceQueryResult(request, new ViewMembersResource(), MEMBERS,
                                        mapEntity, mapChildrenQuery, mapArguments);
            }

        return responseEntity;
        }

    // ----- ViewResource methods-------------------------------------------

    /**
     * MBean query to retrieve ViewMBeans for the provided view.
     *
     * @param request       the request
     * @param sViewName    the view name
     * @param sServiceName  the service name
     *
     * @return the MBean query
     */
    protected MBeanAccessor.QueryBuilder getQuery(HttpRequest request, String sViewName, String sServiceName)
        {
        return createQueryBuilder(request)
                .withBaseQuery(VIEW_QUERY + sViewName)
                .withService(sServiceName);
        }

    /**
     * Add attributes from the ObjectNames to the given EntityMBeanResponse.
     *
     * @param request         the {@link HttpRequest}
     * @param setObjectNames  the set of ObjectNames from which to add to the response
     * @param responseEntity  the EntityMBeanResponse to add attributes to
     * @param uriCurrent      the current URI
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected void addObjectNamesToResponse(HttpRequest         request,
                                            Set<String>         setObjectNames,
                                            EntityMBeanResponse responseEntity,
                                            URI                 uriCurrent)
        {
        Filter<String>      filterAttributes = getAttributesFilter(request);
        Map<String, Object> mapAttributes    = new LinkedHashMap<>();

        // return name, service, and node_id if no field is specified
        if (filterAttributes instanceof AlwaysFilter)
            {
            filterAttributes = getAttributesFilter(String.join(",", NAME, SERVICE, NODE_ID), null);
            }

        for (String sName : setObjectNames)
            {
            try
                {
                ObjectName objectName = new ObjectName(sName);
                for (String sKey : objectName.getKeyPropertyList().keySet())
                    {
                    if (filterAttributes.evaluate(sKey))
                        {
                        Object oValue   = Converter.convert(objectName.getKeyProperty(sKey));
                        Object oCurrent = mapAttributes.get(sKey);

                        if (oCurrent == null)
                            {
                            mapAttributes.put(sKey, oValue);
                            }
                        else if (oCurrent instanceof Set)
                            {
                            ((Set) oCurrent).add(oValue);
                            }
                        else if (!Objects.equals(oCurrent, oValue))
                            {
                            Set values = new HashSet<>();
                            values.add(oCurrent);
                            values.add(oValue);
                            mapAttributes.put(sKey, values);
                            }
                        }
                    }
                }
            catch (MalformedObjectNameException e)
                {
                CacheFactory.log("Exception occurred while creating an ObjectName " +
                                 sName + "\n" + CacheFactory.getStackTrace(e));
                }
            }

        responseEntity.setEntity(mapAttributes);
        responseEntity.addResourceLink(MEMBERS, getSubUri(uriCurrent, MEMBERS));
        }
    }
