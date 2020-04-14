/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.management.internal.resources;

import com.tangosol.coherence.management.internal.EntityMBeanResponse;

import com.tangosol.net.management.MBeanAccessor.QueryBuilder;

import java.net.URI;

import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import javax.ws.rs.QueryParam;

import javax.ws.rs.core.Response;

/**
 * Handles management API requests for a member of a service.
 *
 * @author sr 2017.08.29
 * @since 12.2.1.4.0
 */
public class ServiceMemberResource
        extends AbstractManagementResource
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a ServiceMemberResource.
     *
     * @param resource  the {@link AbstractManagementResource} to be used to initialize the context
     */
    public ServiceMemberResource(AbstractManagementResource resource)
        {
        super(resource);
        }

    // ----- GET API --------------------------------------------------------

    /**
     * Return ServiceMBean(s) attributes of a service.
     *
     * @param sServiceName  the service name
     * @param sMemberKey    the member key, can be a member name or node Id
     *
     * @return the response object
     */
    @GET
    @Produces(MEDIA_TYPES)
    public Response get(@PathParam(SERVICE_NAME) String sServiceName, @PathParam(MEMBER_KEY) String sMemberKey)
        {
        return response(getResponseEntityForMbean(getQuery(sServiceName, sMemberKey), LINKS));
        }

    /**
     * Return ConnectionManagerMBean(s) attributes of a service.
     *
     * @param sServiceName  the service name
     * @param sMemberKey    the member key, can be a member name or node Id
     *
     * @return the response object
     */
    @GET
    @Produces(MEDIA_TYPES)
    @Path(PROXY)
    public Response getProxy(@PathParam(SERVICE_NAME) String sServiceName, @PathParam(MEMBER_KEY) String sMemberKey)
        {
        QueryBuilder bldrQuery = getConnectionManagerQuery(sServiceName, sMemberKey);
        return response(getResponseEntityForMbean(bldrQuery, "connections"));
        }

    /**
     * Return the list of ConnectionManager(s) of a service.
     *
     * @param sServiceName  the service name
     * @param sMemberKey    the member key, can be a member name or node Id
     *
     * @return the response object
     */
    @GET
    @Produces(MEDIA_TYPES)
    @Path(PROXY + "/" + CONNECTIONS)
    public Response getProxyConnections(@PathParam(SERVICE_NAME) String sServiceName,
                                        @PathParam(MEMBER_KEY)   String sMemberKey)
        {
        QueryBuilder bldrQuery = getConnectionsQuery(sServiceName, sMemberKey);
        // for proxy connections, we do not send an error even if there are no connections
        return response(getResponseBodyForMBeanCollection(bldrQuery, null, getParentUri(), getCurrentUri()).toJson());
        }

    /**
     * Return the response of "distributionState" operation of a ServiceMBean.
     *
     * @param sServiceName  the service name
     * @param sMemberKey  the member key, can be a member name or node Id
     *
     * @return the response object
     */
    @GET
    @Produces(MEDIA_TYPES)
    @Path(DISTRIBUTION_STATE)
    public Response getDistributionState(@PathParam(SERVICE_NAME) String sServiceName,
                                         @PathParam(MEMBER_KEY)   String sMemberKey,
                                         @QueryParam(VERBOSE)     Boolean verbose)
        {
        String[] asSignature = {boolean.class.getName()};
        Object[] aoArguments = {verbose != null ? verbose : false};

        QueryBuilder bldrQuery = getQuery(sServiceName, sMemberKey);

        return response(getResponseFromMBeanOperation(bldrQuery,
                "distributionState", "reportDistributionState", aoArguments, asSignature));
        }

    /**
     * Return the response of "reportOwnership" operation of a ServiceMBean.
     *
     * @param sServiceName  the service name
     * @param sMemberKey    the member key, can be a member name or node Id
     * @param fVerbose      flat to determin if verbose output is needed
     *
     * @return the response object
     */
    @GET
    @Produces(MEDIA_TYPES)
    @Path(OWNERSHIP)
    public Response getOwnership(@PathParam(SERVICE_NAME) String sServiceName,
                                 @PathParam(MEMBER_KEY)   String sMemberKey,
                                 @QueryParam(VERBOSE)     Boolean fVerbose)
        {
        String[] asSignature = {boolean.class.getName()};
        Object[] aoArguments = {fVerbose != null ? fVerbose : false};

        QueryBuilder bldrQuery = getQuery(sServiceName, sMemberKey);

        return response(getResponseFromMBeanOperation(bldrQuery,
                "ownership", "reportOwnership", aoArguments, asSignature));
        }

    // ----- POST API(Operations) -------------------------------------------

    /**
     * Call "resetStatistics" operation on ServiceMBean.
     *
     * @param sServiceName  the service name
     * @param sMemberKey    the member key, can be a member name or node Id
     *
     * @return the response object
     */
    @POST
    @Produces(MEDIA_TYPES)
    @Consumes(MEDIA_TYPES)
    @Path(RESET_STATS)
    public Response resetStatistics(@PathParam(SERVICE_NAME) String sServiceName,
                                    @PathParam(MEMBER_KEY)   String sMemberKey)
        {
        return executeMBeanOperation(getQuery(sServiceName, sMemberKey), RESET_STATS, null, null);
        }

    /**
     * Call "start/stop/shutdown" operation on ServiceMBean.
     *
     * @param sServiceName  the service name
     * @param sMemberKey    the member key, can be a member name or node Id
     *
     * @return the response object
     */
    @POST
    @Produces(MEDIA_TYPES)
    @Consumes(MEDIA_TYPES)
    @Path("{operationName:start|stop|shutdown}")
    public Response executeNoArgOperation(@PathParam(SERVICE_NAME)   String sServiceName,
                                          @PathParam(MEMBER_KEY)     String sMemberKey,
                                          @PathParam(OPERATION_NAME) String sOperationName)
            throws Exception
        {
        return executeMBeanOperation(getQuery(sServiceName, sMemberKey), sOperationName, null, null);
        }

    // ----- POST API(Update) -----------------------------------------------

    /**
     * Update a ServiceMBean with the parameters present in the input entity map.
     *
     * @param sServiceName  the service name
     * @param sMemberKey    the member key, can be a member name or node Id
     * @param entity        the input entity map containing the updated attributes
     *
     * @return the response object
     */
    @POST
    @Produces(MEDIA_TYPES)
    @Consumes(MEDIA_TYPES)
    public Response updateAttributes(@PathParam(SERVICE_NAME) String sServiceName,
                                     @PathParam(MEMBER_KEY)   String sMemberKey,
                                     Map<String, Object> entity)
        {
        return update(entity, getQuery(sServiceName, sMemberKey));
        }

    // ----- Child Resources ------------------------------------------------

    // ----- AbstractManagementResource methods -------------------------------------------

    @Override
    protected EntityMBeanResponse getQueryResult(Map mapQuery, Map<String, String> mapArguments, URI uriParent)
        {
        String              sMemberId    = mapArguments.get(MEMBER_KEY);
        String              sServiceName = mapArguments.get(NAME);
        URI                 uriSelf      = getSubUri(uriParent, sMemberId);
        EntityMBeanResponse response     = getLinksOnlyResponseBody(uriParent, uriSelf, getLinksFilter(mapQuery), LINKS);
        Map<String, Object> mapResponse  = response.getEntity();
        Object              oChildren    = mapQuery == null ? null : mapQuery.get(CHILDREN);

        if (oChildren != null && oChildren instanceof Map)
            {
            QueryBuilder bldrQuery = getConnectionManagerQuery(sServiceName, sMemberId);
            addChildMbeanQueryResult(PROXY, bldrQuery, mapResponse, (Map) oChildren, CONNECTIONS);
            }

        return response;
        }

    // ----- ServiceMemberResource methods-----------------------------------

    /**
     * MBean query to retrieve ServiceMBean for the provided service running in a member.
     *
     * @param sServiceName  the service name
     * @param sMemberKey    the member key, can be a member name or node Id
     *
     * @return the MBean query builder
     */
    protected QueryBuilder getQuery(String sServiceName, String sMemberKey)
        {
        return createQueryBuilder().withBaseQuery(SERVICE_MEMBERS_QUERY + sServiceName).withMember(sMemberKey);
        }

    /**
     * MBean query to retrieve ConnectionManager for the proxy service running in a member.
     *
     * @param sServiceName  the service name
     * @param sMemberKey    the member key, can be a member name or node Id
     *
     * @return the MBean query builder
     */
    protected QueryBuilder getConnectionManagerQuery(String sServiceName, String sMemberKey)
        {
        return createQueryBuilder().withBaseQuery(CONNECTION_MANAGER_QUERY + sServiceName)
                .withMember(sMemberKey);
        }

    /**
     * MBean query to retrieve ConnectionMBean for the proxy service running in a member.
     *
     * @param sServiceName  the service name
     * @param sMemberKey    the member key, can be a member name or node Id
     *
     * @return the MBean query builder
     */
    protected QueryBuilder getConnectionsQuery(String sServiceName, String sMemberKey)
        {
        return createQueryBuilder().withBaseQuery(CONNECTIONS_QUERY + sServiceName).withMember(sMemberKey);
        }

    // ----- constants ------------------------------------------------------

    public static final String DISTRIBUTION_STATE = "distributionState";
    public static final String OWNERSHIP = "ownership";
    public static final String[] LINKS = {PROXY, DISTRIBUTION_STATE, OWNERSHIP};
    }
