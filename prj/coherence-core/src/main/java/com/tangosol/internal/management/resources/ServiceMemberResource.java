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
import com.tangosol.net.management.MBeanAccessor.QueryBuilder;

import java.net.URI;

import java.util.Map;

/**
 * Handles management API requests for a member of a service.
 *
 * @author sr 2017.08.29
 * @author Jonathan Knight  2022.01.25
 * @since 12.2.1.4.0
 */
public class ServiceMemberResource
        extends AbstractManagementResource
    {
    // ----- Routes methods -------------------------------------------------

    @Override
    public void addRoutes(RequestRouter router, String sPathRoot)
        {
        router.addGet(sPathRoot, this::get);
        router.addGet(sPathRoot + "/" + PROXY, this::getProxy);
        router.addGet(sPathRoot + "/" + PROXY + "/" + CONNECTIONS, this::getProxyConnections);
        router.addGet(sPathRoot + "/" + DISTRIBUTION_STATE, this::getDistributionState);
        router.addGet(sPathRoot + "/" + OWNERSHIP, this::getOwnership);

        router.addPost(sPathRoot, this::updateAttributes);
        router.addPost(sPathRoot + "/" + RESET_STATS, this::resetStatistics);
        router.addPost(sPathRoot + "/" + PROXY + "/" + RESET_STATS, this::resetStatisticsProxy);
        router.addPost(sPathRoot + "/start", this::start);
        router.addPost(sPathRoot + "/stop", this::stop);
        router.addPost(sPathRoot + "/shutdown", this::shutdown);
        }

    // ----- GET API --------------------------------------------------------

    /**
     * Return ServiceMBean(s) attributes of a service.
     *
     * @return the response object
     */
    public Response get(HttpRequest request)
        {
        String sServiceName = request.getFirstPathParameter(SERVICE_NAME);
        String sMemberKey   = request.getFirstPathParameter(MEMBER_KEY);
        return response(getResponseEntityForMbean(request, getQuery(request, sServiceName, sMemberKey), LINKS));
        }

    /**
     * Return ConnectionManagerMBean(s) attributes of a service.
     *
     * @return the response object
     */
    public Response getProxy(HttpRequest request)
        {
        String       sServiceName = request.getFirstPathParameter(SERVICE_NAME);
        String       sMemberKey   = request.getFirstPathParameter(MEMBER_KEY);
        QueryBuilder queryBuilder = getConnectionManagerQuery(request, sServiceName, sMemberKey);
        return response(getResponseEntityForMbean(request, queryBuilder, "connections"));
        }

    /**
     * Return the list of ConnectionManager(s) of a service.
     *
     * @return the response object
     */
    public Response getProxyConnections(HttpRequest request)
        {
        String       sServiceName = request.getFirstPathParameter(SERVICE_NAME);
        String       sMemberKey   = request.getFirstPathParameter(MEMBER_KEY);
        QueryBuilder queryBuilder = getConnectionsQuery(request, sServiceName, sMemberKey);
        // for proxy connections, we do not send an error even if there are no connections
        return response(getResponseBodyForMBeanCollection(request, queryBuilder, null, getParentUri(request), getCurrentUri(request)).toJson());
        }

    /**
     * Return the response of "distributionState" operation of a ServiceMBean.
     *
     * @return the response object
     */
    public Response getDistributionState(HttpRequest request)
        {
        String       sServiceName = request.getFirstPathParameter(SERVICE_NAME);
        String       sMemberKey   = request.getFirstPathParameter(MEMBER_KEY);
        boolean      verbose      = Boolean.parseBoolean(request.getFirstQueryParameter(VERBOSE));
        String[]     asSignature  = {boolean.class.getName()};
        Object[]     aoArguments  = {verbose};
        QueryBuilder queryBuilder = getQuery(request, sServiceName, sMemberKey);

        return response(getResponseFromMBeanOperation(request, queryBuilder,
                "distributionState", "reportDistributionState", aoArguments, asSignature));
        }

    /**
     * Return the response of "reportOwnership" operation of a ServiceMBean.
     *
     * @return the response object
     */
    public Response getOwnership(HttpRequest request)
        {
        String       sServiceName = request.getFirstPathParameter(SERVICE_NAME);
        String       sMemberKey   = request.getFirstPathParameter(MEMBER_KEY);
        boolean      verbose      = Boolean.parseBoolean(request.getFirstQueryParameter(VERBOSE));
        String[]     asSignature  = {boolean.class.getName()};
        Object[]     aoArguments  = {verbose};
        QueryBuilder queryBuilder = getQuery(request, sServiceName, sMemberKey);

        return response(getResponseFromMBeanOperation(request, queryBuilder,
                "ownership", "reportOwnership", aoArguments, asSignature));
        }

    // ----- POST API(Operations) -------------------------------------------

    /**
     * Call "resetStatistics" operation on ServiceMBean.
     *
     * @return the response object
     */
    public Response resetStatistics(HttpRequest request)
        {
        return executeNoArgOperation(request, RESET_STATS);
        }

    /**
     * Call "resetStatistics" operation on ConnectionManagerMBean.
     *
     * @return the response object
     */
    public Response resetStatisticsProxy(HttpRequest request)
        {
        return executeNoArgOperationProxy(request, RESET_STATS);
        }

    /**
     * Call "start/stop/shutdown" operation on ServiceMBean.
     *
     * @return the response object
     */
    public Response start(HttpRequest request)
        {
        return executeNoArgOperation(request, "start");
        }

    /**
     * Call "stop" operation on ServiceMBean.
     *
     * @return the response object
     */
    public Response stop(HttpRequest request)
        {
        return executeNoArgOperation(request, "stop");
        }

    /**
     * Call "shutdown" operation on ServiceMBean.
     *
     * @return the response object
     */
    public Response shutdown(HttpRequest request)
        {
        return executeNoArgOperation(request, "shutdown");
        }

    /**
     * Execute an MBean operation against a service member.
     * @param request         {@link HttpRequest}
     * @param sOperationName  operation to call
     *
     * @return the response object
     */
    public Response executeNoArgOperation(HttpRequest request, String sOperationName)
        {
        String sServiceName = request.getFirstPathParameter(SERVICE_NAME);
        String sMemberKey   = request.getFirstPathParameter(MEMBER_KEY);
        return executeMBeanOperation(request, getQuery(request, sServiceName, sMemberKey), sOperationName, null, null);
        }

    /**
     * Execute an MBean operation against a proxy member.
     * @param request         {@link HttpRequest}
     * @param sOperationName  operation to call
     *
     * @return the response object
     */
    public Response executeNoArgOperationProxy(HttpRequest request, String sOperationName)
        {
        String sServiceName = request.getFirstPathParameter(SERVICE_NAME);
        String sMemberKey   = request.getFirstPathParameter(MEMBER_KEY);
        return executeMBeanOperation(request, getConnectionManagerQuery(request, sServiceName, sMemberKey), sOperationName, null, null);
        }

    // ----- POST API(Update) -----------------------------------------------

    /**
     * Update a ServiceMBean with the parameters present in the input entity map.
     *
     * @return the response object
     */
    public Response updateAttributes(HttpRequest request)
        {
        String              sServiceName = request.getFirstPathParameter(SERVICE_NAME);
        String              sMemberKey   = request.getFirstPathParameter(MEMBER_KEY);
        Map<String, Object> entity       = getJsonBody(request);
        return update(request, entity, getQuery(request, sServiceName, sMemberKey));
        }

    // ----- helper methods -------------------------------------------------

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected EntityMBeanResponse getQueryResult(HttpRequest         request,
                                                 URI                 uriParent,
                                                 URI                 uriCurrent,
                                                 Map<String, Object> mapQuery,
                                                 Map<String, String> mapArguments)
        {
        String              sMemberId    = mapArguments.get(MEMBER_KEY);
        String              sServiceName = mapArguments.get(NAME);
        URI                 uriSelf      = getSubUri(uriParent, sMemberId);
        EntityMBeanResponse response     = getLinksOnlyResponseBody(request, uriParent, uriSelf, getLinksFilter(request, mapQuery), LINKS);
        Map<String, Object> mapResponse  = response.getEntity();
        Object              oChildren    = mapQuery == null ? null : mapQuery.get(CHILDREN);

        if (oChildren instanceof Map)
            {
            QueryBuilder queryBuilder = getConnectionManagerQuery(request, sServiceName, sMemberId);
            addChildMbeanQueryResult(request, uriParent, uriCurrent, PROXY, queryBuilder, mapResponse, (Map) oChildren, CONNECTIONS);
            }

        return response;
        }

    // ----- ServiceMemberResource methods-----------------------------------

    /**
     * MBean query to retrieve ServiceMBean for the provided service running in a member.
     *
     *
     * @param request       the request
     * @param sServiceName  the service name
     * @param sMemberKey    the member key, can be a member name or node Id
     *
     * @return the MBean query builder
     */
    protected QueryBuilder getQuery(HttpRequest request, String sServiceName, String sMemberKey)
        {
        return createQueryBuilder(request).withBaseQuery(SERVICE_MEMBERS_QUERY + sServiceName).withMember(sMemberKey);
        }

    /**
     * MBean query to retrieve ConnectionManager for the proxy service running in a member.
     *
     *
     * @param request       the request
     * @param sServiceName  the service name
     * @param sMemberKey    the member key, can be a member name or node Id
     *
     * @return the MBean query builder
     */
    protected QueryBuilder getConnectionManagerQuery(HttpRequest request, String sServiceName, String sMemberKey)
        {
        return createQueryBuilder(request).withBaseQuery(CONNECTION_MANAGER_QUERY + sServiceName)
                .withMember(sMemberKey);
        }

    /**
     * MBean query to retrieve ConnectionMBean for the proxy service running in a member.
     *
     * @param request       the request
     * @param sServiceName  the service name
     * @param sMemberKey    the member key, can be a member name or node Id
     *
     * @return the MBean query builder
     */
    protected QueryBuilder getConnectionsQuery(HttpRequest request, String sServiceName, String sMemberKey)
        {
        return createQueryBuilder(request).withBaseQuery(CONNECTIONS_QUERY + sServiceName).withMember(sMemberKey);
        }

    // ----- constants ------------------------------------------------------

    public static final String DISTRIBUTION_STATE = "distributionState";
    public static final String OWNERSHIP = "ownership";
    public static final String[] LINKS = {PROXY, DISTRIBUTION_STATE, OWNERSHIP, FEDERATION};
    }
