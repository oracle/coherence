/*
 * Copyright (c) 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.management.internal.resources;

import com.tangosol.coherence.management.internal.Converter;
import com.tangosol.coherence.management.internal.EntityMBeanResponse;

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

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import static java.lang.String.format;

/**
 * Handles management API requests for a storage manager in a service.
 */
public class StorageManagerResource
        extends AbstractManagementResource
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a StorageManagerResource.
     *
     * @param resource  the {@link AbstractManagementResource} to be used to initialize the context
     */
    public StorageManagerResource(AbstractManagementResource resource)
        {
        super(resource);
        }

    // ----- GET API --------------------------------------------------------

    /**
     * Return the aggregated metrics of StorageManagerMBean's for a single cache belonging to a Service.
     *
     * @param sCacheName  the cache name
     * @param sRoleName   either a regex to be applied against node ids or a role name
     * @param sCollector  the collector to use instead of the default
     *
     * @return the response object
     */
    @GET
    @Produces(MEDIA_TYPES)
    public Response get(@PathParam(CACHE_NAME) String sCacheName,
                        @QueryParam(ROLE_NAME) String sRoleName,
                        @QueryParam(COLLECTOR) String sCollector)
        {
        QueryBuilder queryBuilder = getQuery(sCacheName);

        // collect attributes from the ObjectNames
        Set<String> setObjectNames = getMBeanAccessor().queryKeys(queryBuilder.build());

        if (setObjectNames == null || setObjectNames.isEmpty())
            {
            return Response.status(Response.Status.NOT_FOUND).build();
            }

        EntityMBeanResponse responseEntity = createResponse(getParentUri(), getCurrentUri(), getLinksFilter());
        addObjectNamesToResponse(setObjectNames, responseEntity);

        Map<String, Object> mapResponse = responseEntity.toJson();

        // aggregate cache and storage metrics into the response, storage manage metrics is always sent along with cache
        addAggregatedMetricsToResponseMap(sRoleName, sCollector, queryBuilder, mapResponse);
        return response(mapResponse);
        }

    // ----- POST API -------------------------------------------------------

    /**
     * Call "resetStatistics" operation on StorageManagerMBean for all members.
     *
     * @param sCacheName  the cache name
     *
     * @return the response object
     */
    @POST
    @Produces(MEDIA_TYPES)
    public Response resetStatistics(@PathParam(CACHE_NAME) String sCacheName)
        {
        QueryBuilder bldrQuery = getQuery(sCacheName);
        return executeMBeanOperation(bldrQuery, RESET_STATS, null, null);
        }

    /**
     * Call "clearCache" operation on StorageManagerMBean for all members.
     *
     * @param sCacheName  the cache name
     *
     * @return the response object
     */
    @POST
    @Path(CLEAR)
    @Produces(MEDIA_TYPES)
    public Response clearCache(@PathParam(CACHE_NAME) String sCacheName)
        {
        QueryBuilder bldrQuery = getQuery(sCacheName);
        return executeMBeanOperation(bldrQuery, "clearCache", null, null);
        }

    /**
     * Call "truncateCache" operation on StorageManagerMBean for all members.
     *
     * @param sCacheName  the cache name
     *
     * @return the response object
     */
    @POST
    @Path(TRUNCATE)
    @Produces(MEDIA_TYPES)
    public Response truncateCache(@PathParam(CACHE_NAME) String sCacheName)
        {
        QueryBuilder bldrQuery = getQuery(sCacheName);
        return executeMBeanOperation(bldrQuery, "truncateCache", null, null);
        }

    // ----- AbstractManagementResource methods -------------------------------------------

    @Override
    protected EntityMBeanResponse getQueryResult(Map mapQuery, Map<String, String> mapArguments, URI uriParent)
        {
        String sCacheName = mapArguments.get(CACHE);

        // collect attributes from the ObjectNames
        URI            uriSelf        = getSubUri(uriParent, sCacheName);
        Filter<String> filterLinks    = getLinksFilter(mapQuery);
        QueryBuilder   bldrQuery      = getQuery(sCacheName);
        Set<String>    setObjectNames = getMBeanAccessor().queryKeys(bldrQuery.build());

        if (setObjectNames == null || setObjectNames.isEmpty())
            {
            return null;
            }

        EntityMBeanResponse responseEntity = createResponse(uriParent, uriSelf, filterLinks);
        addObjectNamesToResponse(setObjectNames, responseEntity);

        Map<String, Object> mapEntity = responseEntity.getEntity();
        addAggregatedMetricsToResponseMap("*", null, bldrQuery, mapEntity);

        return responseEntity;
        }

    // ----- StorageManagerResource methods-------------------------------------------

    /**
     * MBean query to retrieve StorageManagerMBeans for the provided cache.
     *
     * @param sCacheName  the cache name
     *
     * @return the MBean query
     */
    protected QueryBuilder getQuery(String sCacheName)
        {
        return createQueryBuilder()
                .withBaseQuery(format(STORAGE_MANAGERS_QUERY, sCacheName))
                .withService(getService());
        }

    /**
     * Add attributes from the ObjectNames to the given EntityMBeanResponse.
     *
     * @param setObjectNames  the set of ObjectNames from which to add to the response
     * @param responseEntity  the EntityMBeanResponse to add attributes to
     */
    protected void addObjectNamesToResponse(Set<String> setObjectNames, EntityMBeanResponse responseEntity)
        {
        Filter<String>      filterAttributes = getAttributesFilter();
        Map<String, Object> mapAttributes    = new LinkedHashMap<>();

        // return name, service, and node_id if no field is specified
        if (filterAttributes instanceof AlwaysFilter)
            {
            filterAttributes = getAttributesFilter(String.join(",", CACHE, SERVICE, NODE_ID), null);
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
        }
    }
