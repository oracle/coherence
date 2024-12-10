/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.management.resources;

import com.tangosol.internal.http.HttpRequest;
import com.tangosol.internal.http.RequestRouter;
import com.tangosol.internal.http.Response;

import com.tangosol.internal.management.Converter;
import com.tangosol.internal.management.EntityMBeanResponse;

import com.tangosol.net.CacheFactory;

import com.tangosol.net.management.MBeanAccessor.QueryBuilder;

import com.tangosol.util.Filter;
import com.tangosol.util.filter.AlwaysFilter;

import java.net.URI;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import static java.lang.String.format;

/**
 * Handles management API requests for a cache in a service.
 *
 * @author sr  2017.08.29
 * @author Jonathan Knight  2022.01.25
 * @since 12.2.1.4.0
 */
public class CacheResource extends AbstractManagementResource
    {
    // ----- Routes methods -------------------------------------------------

    @Override
    public void addRoutes(RequestRouter router, String sPathRoot)
        {
        router.addGet(sPathRoot, this::get);

        router.addPost(sPathRoot, this::update);
        router.addPost(sPathRoot + "/" + RESET_STATS, this::resetStatistics);

        // child resources
        router.addRoutes(sPathRoot + "/" + MEMBERS, new CacheMembersResource());
        }

    // ----- GET API --------------------------------------------------------

    /**
     * Return the aggregated metrics of CacheMBean's for a single cache belonging to a Service.
     *
     * @param request  the {@link HttpRequest}
     *
     * @return the response object
     */
    public Response get(HttpRequest request)
        {
        String       sCacheName     = request.getFirstPathParameter(CACHE_NAME);
        String       sRoleName      = request.getFirstQueryParameter(ROLE_NAME);
        String       sCollector     = request.getFirstQueryParameter(COLLECTOR);
        String       sServiceName   = request.getPathParameters().getFirst(SERVICE_NAME);
        QueryBuilder queryBuilder   = getQuery(request, sCacheName, sServiceName);
        Set<String>  setObjectNames = ensureBeanAccessor(request).queryKeys(queryBuilder.build());

        if (setObjectNames == null || setObjectNames.isEmpty())
            {
            return Response.status(Response.Status.NOT_FOUND).build();
            }

        EntityMBeanResponse responseEntity = createResponse(request, getParentUri(request), getCurrentUri(request), getLinksFilter(request));
        addObjectNamesToResponse(request, setObjectNames, responseEntity, getCurrentUri(request));

        Map<String, Object> mapResponse = responseEntity.toJson();

        QueryBuilder queryBuilderStorage = createQueryBuilder(request)
                .withBaseQuery(format(STORAGE_MANAGERS_QUERY, sCacheName))
                .withService(getService(request));

        // aggregate cache and storage metrics into the response, storage manage metrics is always sent along with cache
        addAggregatedMetricsToResponseMap(request, sRoleName, sCollector, queryBuilder, mapResponse);
        addAggregatedMetricsToResponseMap(request, sRoleName, sCollector, queryBuilderStorage, mapResponse);
        return response(mapResponse);
        }

    // ----- POST API(Operations) -------------------------------------------

    /**
     * Call "resetStatistics" operation on CacheMBean for all members.
     *
     * @param request  the {@link HttpRequest}
     *
     * @return the response object
     */
    public Response resetStatistics(HttpRequest request)
        {
        String       sCacheName   = request.getFirstPathParameter(CACHE_NAME);
        String       sServiceName = request.getPathParameters().getFirst(SERVICE_NAME);
        QueryBuilder queryBuilder = getQuery(request, sCacheName, sServiceName);

        return executeMBeanOperation(request, queryBuilder, RESET_STATS, null, null);
        }

    // ----- POST API(Update) -----------------------------------------------

    /**
     * Update a CacheMBean with the parameters present in the input entity map
     * for all the members.
     *
     * @param request  the {@link HttpRequest}
     *
     * @return the response object
     */
    public Response update(HttpRequest request)
        {
        String       sCacheName   = request.getFirstPathParameter(CACHE_NAME);
        String       sServiceName = request.getPathParameters().getFirst(SERVICE_NAME);
        QueryBuilder queryBuilder = getQuery(request, sCacheName, sServiceName);

        return update(request, getJsonBody(request), queryBuilder);
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
        String sCacheName = mapArguments.get(NAME);

        // collect attributes from the ObjectNames
        URI            uriSelf        = getSubUri(uriParent, sCacheName);
        Filter<String> filterLinks    = getLinksFilter(request, mapQuery);
        String         sServiceName   = request.getPathParameters().getFirst(SERVICE_NAME);
        QueryBuilder   queryBuilder   = getQuery(request, sCacheName, sServiceName);
        Set<String>    setObjectNames = ensureBeanAccessor(request).queryKeys(queryBuilder.build());

        if (setObjectNames == null || setObjectNames.isEmpty())
            {
            return null;
            }

        EntityMBeanResponse responseEntity = createResponse(request, uriParent, uriSelf, filterLinks);
        addObjectNamesToResponse(request, setObjectNames, responseEntity, uriCurrent);

        Map<String, Object> mapEntity = responseEntity.getEntity();

        QueryBuilder queryBuilderStorage = createQueryBuilder(request)
                .withBaseQuery(format(STORAGE_MANAGERS_QUERY, sCacheName))
                .withService(getService(request));

        addAggregatedMetricsToResponseMap(request, "*", null, queryBuilder, mapEntity);
        addAggregatedMetricsToResponseMap(request, "*", null, queryBuilderStorage, mapEntity);

        Object oChildren = mapQuery == null ? null : mapQuery.get(CHILDREN);
        if (oChildren instanceof Map)
            {
            Map mapChildrenQuery = (Map) oChildren;

            addChildResourceQueryResult(request, new CacheMembersResource(), MEMBERS,
                                        mapEntity, mapChildrenQuery, mapArguments);
            }

        return responseEntity;
        }

    // ----- CacheResource methods-------------------------------------------

    /**
     * MBean query to retrieve CacheMBeans for the provided cache.
     *
     *
     * @param request       the request
     * @param sCacheName    the cache name
     * @param sServiceName  the service name
     *
     * @return the MBean query
     */
    protected QueryBuilder getQuery(HttpRequest request, String sCacheName, String sServiceName)
        {
        return createQueryBuilder(request)
                .withBaseQuery(CACHE_QUERY + sCacheName)
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
