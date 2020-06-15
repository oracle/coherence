/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.management.internal.resources;

import com.tangosol.coherence.management.internal.EntityMBeanResponse;

import com.tangosol.net.management.MBeanHelper;
import com.tangosol.net.management.MBeanAccessor.QueryBuilder;

import com.tangosol.util.Filter;

import java.util.Collections;

import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import javax.ws.rs.WebApplicationException;

import javax.ws.rs.core.Response;

import java.net.URI;

/**
 * Handles management API requests for a service in a cluster.
 *
 * @author sr 2017.08.29
 * @since 12.2.1.4.0
 */
public class ServiceResource
        extends AbstractManagementResource
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a ServiceResource.
     *
     * @param resource  the {@link AbstractManagementResource} to be used to initialize the context
     */
    public ServiceResource(AbstractManagementResource resource)
        {
        super(resource);
        }

    // ----- GET API --------------------------------------------------------

    /**
     * Return the aggregated metrics of ServiceMBean's of a service.
     *
     * @param sServiceName  the service name
     * @param sRoleName     either a regex to be applied against node ids or a role name
     * @param sCollector    the collector to use instead of the default
     *
     * @return the response object
     */
    @GET
    @Produces(MEDIA_TYPES)
    public Response get(@PathParam(SERVICE_NAME) String sServiceName,
                        @QueryParam(ROLE_NAME)   String sRoleName,
                        @QueryParam(COLLECTOR)   String sCollector)
        {
        QueryBuilder bldrQuery = getQuery(sServiceName);

        // create a filter with name/type as the only included params, and use whatever user provided
        // for exclude.
        Filter<String> filterAttributes = getAttributesFilter(NAME + "," + TYPE + "," + DOMAIN_PARTITION
                , getExcludeList(null));

        EntityMBeanResponse response = getResponseEntityForMbean(bldrQuery, getParentUri(),
                getCurrentUri(), filterAttributes, getLinksFilter(), CHILD_LINKS);

        if (response == null) // if there is no entity, the service could not be found
            {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

        Map<String, Object> responseMap = response.toJson();
        addAggregatedMetricsToResponseMap(sRoleName, sCollector, bldrQuery, responseMap);
        return response(responseMap);
        }

    /**
     * Return SimpleStrategyMBean(s) attributes of a service.
     *
     * @param sServiceName  the service name
     *
     * @return the response object
     */
    @GET
    @Produces(MEDIA_TYPES)
    @Path(PARTITION)
    public Response getPartitionAssignment(@PathParam(SERVICE_NAME) String sServiceName)
        {
        QueryBuilder bldrQuery = getPartitionAssignmentQuery(sServiceName);
        return response(getResponseEntityForMbean(bldrQuery, SCHEDULED_DISTRIBUTIONS));
        }

    /**
     * Return the response of "reportScheduledDistributions" operation of a SimpleStrategyMBean.
     *
     * @param sServiceName  the service name
     *
     * @return the response object
     */
    @GET
    @Produces(MEDIA_TYPES)
    @Path(PARTITION + "/" + SCHEDULED_DISTRIBUTIONS)
    public Response getScheduledDistributions(@PathParam(SERVICE_NAME) String  sServiceName,
                                              @QueryParam(VERBOSE)     Boolean fVerbose)
        {
        String[] asSignature = {boolean.class.getName()};
        Object[] aoArguments = {fVerbose != null ? fVerbose : false};

        QueryBuilder bldrQuery = getPartitionAssignmentQuery(sServiceName);

        return response(getResponseFromMBeanOperation(bldrQuery,
                SCHEDULED_DISTRIBUTIONS, "reportScheduledDistributions", aoArguments, asSignature));
        }

    /**
     * Return the aggregated metrics of ServiceMBean's of a service.
     *
     * @param sServiceName  the service name
     * @param sRoleName     either a regex to be applied against node ids or a role name
     * @param sCollector    the collector to use instead of the default
     *
     * @return the response object
     */
    @GET
    @Produces(MEDIA_TYPES)
    @Path(PROXY)
    public Response getAggregatedProxyMetricsResponse(@PathParam(SERVICE_NAME) String sServiceName,
                                                      @QueryParam(ROLE_NAME)   String sRoleName,
                                                      @QueryParam(COLLECTOR)   String sCollector)
        {
        EntityMBeanResponse responseEntity = getLinksOnlyResponseBody(getParentUri(), getCurrentUri());
        Map<String, Object> responseMap    = responseEntity.toJson();
        QueryBuilder        bldrQuery      = createQueryBuilder().withBaseQuery(CONNECTION_MANAGER_QUERY + sServiceName);

        addAggregatedMetricsToResponseMap(sRoleName, sCollector, bldrQuery, responseMap);
        return response(responseMap);
        }

    // ----- POST API(Operations) -------------------------------------------

    /**
     * Call "suspendService" operation on ClusterMBean.
     *
     * @param sServiceName  the service name
     *
     * @return the response object
     */
    @POST
    @Produces(MEDIA_TYPES)
    @Path("suspend")
    public Response suspendService(@PathParam(SERVICE_NAME) String sServiceName)
        {
        QueryBuilder bldrQuery = createQueryBuilder().withBaseQuery(CLUSTER_QUERY);
        return executeMBeanOperation(bldrQuery, "suspendService",
                new Object[]{sServiceName}, new String[]{String.class.getName()});
        }

    /**
     * Call "resumeService" operation on ClusterMBean.
     *
     * @param sServiceName  the service name
     *
     * @return the response object
     */
    @POST
    @Produces(MEDIA_TYPES)
    @Path("resume")
    public Response resumeService(@PathParam(SERVICE_NAME) String sServiceName)
        {
        QueryBuilder bldrQuery = createQueryBuilder().withBaseQuery(CLUSTER_QUERY);
        return executeMBeanOperation(bldrQuery, "resumeService",
                new Object[]{sServiceName}, new String[]{String.class.getName()});
        }

    // ----- Child Resources ------------------------------------------------

    /**
     * Sub resource for service members.
     *
     * @return the service members child resource
     */
    @Path(MEMBERS)
    public Object getMembersResource()
        {
        return new ServiceMembersResource(this);
        }

    /**
     * Sub resource for caches.
     *
     * @return the caches child resource
     */
    @Path(CACHES)
    public Object getCachesResource()
        {
        return new CachesResource(this);
        }

    /**
     * Sub resource for persistence.
     *
     * @return the persistence child resource
     */
    @Path(PERSISTENCE)
    public Object getPersistenceResource()
        {
        return new PersistenceResource(this);
        }

    // ----- AbstractManagementResource methods -------------------------------------------

    @Override
    protected EntityMBeanResponse getQueryResult(Map mapQuery, Map<String, String> mapArguments, URI uriParent)
        {
        // create a filter with name/type as the only included params, and use whatever user provided
        // for exclude.
        String         sServiceName = MBeanHelper.safeUnquote(mapArguments.get(NAME));
        URI            uriSelf      = getSubUri(uriParent, sServiceName);
        Filter<String> filterLinks  = getLinksFilter(mapQuery);
        QueryBuilder   bldrQuery    = getQuery(sServiceName);

        Filter<String> filterAttributes =
                getAttributesFilter(NAME + "," + TYPE + "," + DOMAIN_PARTITION, getExcludeList(mapQuery));

        EntityMBeanResponse response =
                getResponseEntityForMbean(bldrQuery, uriParent, uriSelf, filterAttributes, filterLinks, CHILD_LINKS);

        if (response != null)
            {
            Object              oChildren = getChildrenQuery(mapQuery);
            Map<String, Object> mapEntity = response.getEntity();

            addAggregatedMetricsToResponseMap("*", null, bldrQuery, mapEntity);

            if (oChildren != null && oChildren instanceof Map)
                {
                Map mapChildrenQuery = (Map) oChildren;
                bldrQuery = getQuery(sServiceName); // uniqueKey is service name

                addChildMbeanQueryResult(PARTITION, bldrQuery, mapEntity, mapChildrenQuery);

                mapArguments = Collections.singletonMap(SERVICE_NAME, sServiceName);

                addChildResourceQueryResult(new ServiceMembersResource(this), MEMBERS, mapEntity,
                        mapChildrenQuery, mapArguments, uriSelf);
                addChildResourceQueryResult(new CachesResource(this), CACHES, mapEntity, mapChildrenQuery,
                        mapArguments, uriSelf);
                addChildResourceQueryResult(new PersistenceResource(this), PERSISTENCE, mapEntity,
                        mapChildrenQuery, mapArguments, uriSelf);
                }
            }
        return response;
        }

    // ----- ServiceResource methods-----------------------------------

    /**
     * MBean query to retrieve ServiceMBeans for the provided service.
     *
     * @param sServiceName  the service name
     *
     * @return the MBean query builder
     */
    protected QueryBuilder getQuery(String sServiceName)
        {
        return createQueryBuilder().withBaseQuery(SERVICE_MEMBERS_QUERY + sServiceName);
        }

    /**
     * MBean query to retrieve SimpleAssignmentStrategyMBean for the provided service.
     *
     * @param sServiceName  the service name
     *
     * @return the MBean query builder
     */
    protected QueryBuilder getPartitionAssignmentQuery(String sServiceName)
        {
        return createQueryBuilder().withBaseQuery(PARTITION_ASSIGNMENT_QUERY).withService(sServiceName);
        }

    // ----- constants ------------------------------------------------------

    public static final String SCHEDULED_DISTRIBUTIONS = "scheduledDistributions";

    public static String[] CHILD_LINKS = {CACHES, MEMBERS, PARTITION};
    }
