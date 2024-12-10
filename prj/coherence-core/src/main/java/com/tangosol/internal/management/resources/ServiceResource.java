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

import com.tangosol.internal.management.EntityMBeanResponse;

import com.tangosol.net.management.MBeanHelper;
import com.tangosol.net.management.MBeanAccessor.QueryBuilder;

import com.tangosol.util.Filter;

import java.util.Collections;

import java.util.Map;

import java.net.URI;

/**
 * Handles management API requests for a service in a cluster.
 *
 * @author sr 2017.08.29
 * @author Jonathan Knight  2022.01.25
 * @since 12.2.1.4.0
 */
public class ServiceResource
        extends AbstractManagementResource
    {
    // ----- Routes methods -------------------------------------------------

    @Override
    public void addRoutes(RequestRouter router, String sPathRoot)
        {
        router.addGet(sPathRoot, this::get);
        router.addGet(sPathRoot + "/" + PARTITION, this::getPartitionAssignment);
        router.addGet(sPathRoot + "/" + PARTITION + "/" + SCHEDULED_DISTRIBUTIONS, this::getScheduledDistributions);
        router.addGet(sPathRoot + "/" + PROXY, this::getAggregatedProxyMetricsResponse);
        router.addGet(sPathRoot + "/" + DESCRIPTION, this::getServiceDescription);

        router.addPost(sPathRoot, this::update);
        router.addPost(sPathRoot + "/suspend", this::suspendService);
        router.addPost(sPathRoot + "/resume", this::resumeService);
        router.addPost(sPathRoot + "/" + RESET_STATS, this::resetStatistics);
        router.addPost(sPathRoot + "/shutdown", this::shutdownService);
        router.addPost(sPathRoot + "/start", this::startService);
        router.addPost(sPathRoot + "/stop", this::stopService);

        // child resources
        router.addRoutes(sPathRoot + "/" + MEMBERS, new ServiceMembersResource());
        router.addRoutes(sPathRoot + "/" + CACHES, new CachesResource());
        router.addRoutes(sPathRoot + "/" + PERSISTENCE, new PersistenceResource());
        router.addRoutes(sPathRoot + "/" + TOPICS, new TopicsResource());
        router.addRoutes(sPathRoot + "/" + STORAGE, new StorageManagersResource());
        router.addRoutes(sPathRoot + "/" + VIEWS, new ViewsResource());
        }

    // ----- GET API --------------------------------------------------------

    /**
     * Return the aggregated metrics of ServiceMBean's of a service.
     *
     * @return the response object
     */
    public Response get(HttpRequest request)
        {
        String       sServiceName = request.getFirstPathParameter(SERVICE_NAME);
        String       sRoleName    = request.getFirstQueryParameter(ROLE_NAME);
        String       sCollector   = request.getFirstQueryParameter(COLLECTOR);
        QueryBuilder queryBuilder = getQuery(request, sServiceName);

        // create a filter with name/type as the only included params, and use whatever user provided
        // for exclude.

        String         sIncludeFields   = NAME + "," + TYPE + "," + DOMAIN_PARTITION;
        Filter<String> filterAttributes = getAttributesFilter(sIncludeFields, getExcludeList(request));

        EntityMBeanResponse response = getResponseEntityForMbean(request, queryBuilder, getParentUri(request),
                getCurrentUri(request), filterAttributes, getLinksFilter(request), CHILD_LINKS);

        if (response == null) // if there is no entity, the service could not be found
            {
            return Response.status(Response.Status.NOT_FOUND).build();
            }

        Map<String, Object> responseMap = response.toJson();
        addAggregatedMetricsToResponseMap(request, sRoleName, sCollector, queryBuilder, responseMap);
        return response(responseMap);
        }

    /**
     * Return SimpleStrategyMBean(s) attributes of a service.
     *
     * @return the response object
     */
    public Response getPartitionAssignment(HttpRequest request)
        {
        String       sServiceName = request.getFirstPathParameter(SERVICE_NAME);
        QueryBuilder queryBuilder = getPartitionAssignmentQuery(request, sServiceName);
        return response(getResponseEntityForMbean(request, queryBuilder, SCHEDULED_DISTRIBUTIONS));
        }

    /**
     * Return the response of "reportScheduledDistributions" operation of a SimpleStrategyMBean.
     *
     * @return the response object
     */
    public Response getScheduledDistributions(HttpRequest request)
        {
        String       sServiceName = request.getFirstPathParameter(SERVICE_NAME);
        boolean      fVerbose     = Boolean.parseBoolean(request.getFirstQueryParameter(VERBOSE));
        String[]     asSignature  = {boolean.class.getName()};
        Object[]     aoArguments  = {fVerbose};
        QueryBuilder queryBuilder = getPartitionAssignmentQuery(request, sServiceName);

        return response(getResponseFromMBeanOperation(request, queryBuilder,
                SCHEDULED_DISTRIBUTIONS, "reportScheduledDistributions", aoArguments, asSignature));
        }

    /**
     * Return the aggregated metrics of ServiceMBean's of a service.
     *
     * @return the response object
     */
    public Response getAggregatedProxyMetricsResponse(HttpRequest request)
        {
        String              sServiceName = request.getFirstPathParameter(SERVICE_NAME);
        String              sRoleName    = request.getFirstQueryParameter(ROLE_NAME);
        String              sCollector   = request.getFirstQueryParameter(COLLECTOR);
        EntityMBeanResponse responseEntity = getLinksOnlyResponseBody(request, getParentUri(request), getCurrentUri(request));
        Map<String, Object> responseMap    = responseEntity.toJson();
        QueryBuilder        queryBuilder      = createQueryBuilder(request).withBaseQuery(CONNECTION_MANAGER_QUERY + sServiceName);

        addAggregatedMetricsToResponseMap(request, sRoleName, sCollector, queryBuilder, responseMap);
        return response(responseMap);
        }

    /**
     * Return the service description.
     *
     * @param request  the {@link HttpRequest}
     *
     * @return the response object
     */
    public Response getServiceDescription(HttpRequest request)
        {
        String       sServiceName = request.getFirstPathParameter(SERVICE_NAME);
        QueryBuilder queryBuilder = getQuery(request, sServiceName);
        return response(getResponseFromMBeanOperation(request, queryBuilder, DESCRIPTION, "getServiceDescription"));
        }

    // ----- POST API(Operations) -------------------------------------------

    /**
     * Update a ServiceMBean with the parameters present in the input entity
     * map for all the members.
     *
     * @param request  the {@link HttpRequest}
     *
     * @return the response object
     */
    public Response update(HttpRequest request)
        {
        String              sServiceName = request.getFirstPathParameter(SERVICE_NAME);
        Map<String, Object> entity       = getJsonBody(request);
        return update(request, entity, getQuery(request, sServiceName));
        }

    /**
     * Call "suspendService" operation on ServiceMBean.
     *
     * @param request  the {@link HttpRequest}
     *
     * @return the response object
     */
    public Response suspendService(HttpRequest request)
        {
        String       sServiceName = request.getFirstPathParameter(SERVICE_NAME);
        QueryBuilder queryBuilder = createQueryBuilder(request).withBaseQuery(CLUSTER_QUERY);
        return executeMBeanOperation(request, queryBuilder, "suspendService",
                new Object[]{sServiceName}, new String[]{String.class.getName()});
        }

    /**
     * Call "resumeService" operation on ServiceMBean.
     *
     * @param request  the {@link HttpRequest}
     *
     * @return the response object
     */
    public Response resumeService(HttpRequest request)
        {
        String       sServiceName = request.getFirstPathParameter(SERVICE_NAME);
        QueryBuilder queryBuilder = createQueryBuilder(request).withBaseQuery(CLUSTER_QUERY);
        return executeMBeanOperation(request, queryBuilder, "resumeService",
                                     new Object[]{sServiceName}, new String[]{String.class.getName()});
        }

    /**
     * Call "shutdownService" operation on ServiceMBean for all the members.
     *
     * @param request  the {@link HttpRequest}
     *
     * @return the response object
     */
    public Response resetStatistics(HttpRequest request)
        {
        String       sServiceName = request.getFirstPathParameter(SERVICE_NAME);
        QueryBuilder queryBuilder = getQuery(request, sServiceName);
        return executeMBeanOperation(request, queryBuilder, RESET_STATS, null, null);
        }

    /**
     * Call "shutdownService" operation on ServiceMBean for all the members.
     *
     * @param request  the {@link HttpRequest}
     *
     * @return the response object
     */
    public Response shutdownService(HttpRequest request)
        {
        String       sServiceName = request.getFirstPathParameter(SERVICE_NAME);
        QueryBuilder queryBuilder = getQuery(request, sServiceName);
        return executeMBeanOperation(request, queryBuilder, "shutdown",
                new Object[]{sServiceName}, new String[]{String.class.getName()});
        }

    /**
     * Call "startService" operation on ServiceMBean for all the members.
     *
     * @param request  the {@link HttpRequest}
     *
     * @return the response object
     */
    public Response startService(HttpRequest request)
        {
        String       sServiceName = request.getFirstPathParameter(SERVICE_NAME);
        QueryBuilder queryBuilder = getQuery(request, sServiceName);
        return executeMBeanOperation(request, queryBuilder, "start", null, null);
        }

    /**
     * Call "stopService" operation on ServiceMBean for all the members.
     *
     * @param request  the {@link HttpRequest}
     *
     * @return the response object
     */
    public Response stopService(HttpRequest request)
        {
        String       sServiceName = request.getFirstPathParameter(SERVICE_NAME);
        QueryBuilder queryBuilder = getQuery(request, sServiceName);
        return executeMBeanOperation(request, queryBuilder, "stop", null, null);
        }

    // ----- AbstractManagementResource methods -------------------------------------------

    @Override
    @SuppressWarnings({"unchecked"})
    protected EntityMBeanResponse getQueryResult(HttpRequest         request,
                                                 URI                 uriParent,
                                                 URI                 uriCurrent,
                                                 Map<String, Object> mapQuery,
                                                 Map<String, String> mapArguments)
        {
        // create a filter with name/type as the only included params, and use whatever user provided
        // for exclude.
        String         sServiceName = MBeanHelper.safeUnquote(mapArguments.get(NAME));
        URI            uriSelf      = getSubUri(uriParent, sServiceName);
        Filter<String> filterLinks  = getLinksFilter(request, mapQuery);
        QueryBuilder   queryBuilder = getQuery(request, sServiceName);

        Filter<String> filterAttributes =
                getAttributesFilter(NAME + "," + TYPE + "," + DOMAIN_PARTITION, getExcludeList(request, mapQuery));

        EntityMBeanResponse response =
                getResponseEntityForMbean(request, queryBuilder, uriParent, uriSelf, filterAttributes, filterLinks, CHILD_LINKS);

        if (response != null)
            {
            Object              oChildren = getChildrenQuery(mapQuery);
            Map<String, Object> mapEntity = response.getEntity();

            addAggregatedMetricsToResponseMap(request, "*", null, queryBuilder, mapEntity);

            if (oChildren instanceof Map)
                {
                Map<String, Object> mapChildrenQuery = (Map<String, Object>) oChildren;
                queryBuilder = getQuery(request, sServiceName); // uniqueKey is service name

                addChildMbeanQueryResult(request, uriParent, uriCurrent, PARTITION, queryBuilder, mapEntity, mapChildrenQuery);

                mapArguments = Collections.singletonMap(SERVICE_NAME, sServiceName);

                addChildResourceQueryResult(request, new ServiceMembersResource(), MEMBERS, mapEntity,
                                            mapChildrenQuery, mapArguments);
                addChildResourceQueryResult(request, new CachesResource(), CACHES, mapEntity, mapChildrenQuery,
                                            mapArguments);
                addChildResourceQueryResult(request, new PersistenceResource(), PERSISTENCE, mapEntity,
                                            mapChildrenQuery, mapArguments);
                addChildResourceQueryResult(request, new PersistenceResource(), FEDERATION, mapEntity,
                                            mapChildrenQuery, mapArguments);
                addChildResourceQueryResult(request, new TopicsResource(), TOPICS, mapEntity, mapChildrenQuery,
                                            mapArguments);
                addChildResourceQueryResult(request, new ViewsResource(), VIEWS, mapEntity, mapChildrenQuery,
                                            mapArguments);
                }
            }
        return response;
        }

    // ----- ServiceResource methods-----------------------------------

    /**
     * MBean query to retrieve ServiceMBeans for the provided service.
     *
     * @param request       the {@link HttpRequest}
     * @param sServiceName  the service name
     *
     * @return the MBean query builder
     */
    protected QueryBuilder getQuery(HttpRequest request, String sServiceName)
        {
        return createQueryBuilder(request).withBaseQuery(SERVICE_MEMBERS_QUERY + sServiceName);
        }

    /**
     * MBean query to retrieve SimpleAssignmentStrategyMBean for the provided service.
     *
     * @param request       the {@link HttpRequest}
     * @param sServiceName  the service name
     *
     * @return the MBean query builder
     */
    protected QueryBuilder getPartitionAssignmentQuery(HttpRequest request, String sServiceName)
        {
        return createQueryBuilder(request).withBaseQuery(PARTITION_ASSIGNMENT_QUERY).withService(sServiceName);
        }

    // ----- constants ------------------------------------------------------

    public static final String SCHEDULED_DISTRIBUTIONS = "scheduledDistributions";

    public static String[] CHILD_LINKS = {CACHES, MEMBERS, PARTITION, FEDERATION, TOPICS, STORAGE, VIEWS};
    }
