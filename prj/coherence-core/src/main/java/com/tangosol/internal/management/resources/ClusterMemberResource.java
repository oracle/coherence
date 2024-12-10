/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
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

import com.tangosol.util.Filter;

import java.net.URI;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import java.util.stream.Collectors;

import static java.lang.String.format;

/**
 * Handles management API requests for Coherence cluster member.
 *
 * @author sr  2017.08.21
 * @author Jonathan Knight  2022.01.25
 * @author Gunnar Hillert  2022.05.13
 *
 * @since 12.2.1.4.0
 */
public class ClusterMemberResource
        extends AbstractManagementResource
    {
    // ----- Routes methods -------------------------------------------------

    @Override
    public void addRoutes(RequestRouter router, String sPathRoot)
        {
        router.addGet(sPathRoot, this::get);
        router.addGet(sPathRoot + "/" + PLATFORM + "/{" + PLATFORM_MBEAN + "}", this::getPlatformMBeanResponse);
        router.addGet(sPathRoot + "/{" + NETWORK_STATS + "}", this::getPointToPointResponse);
        router.addGet(sPathRoot + "/" + ENVIRONMENT, this::getEnvironmentResponse);
        router.addGet(sPathRoot + "/" + STATE, this::getStateResponse);
        router.addGet(sPathRoot + "/" + FEDERATION, this::getFederationResponse);
        router.addGet(sPathRoot + "/" + FEDERATION+ "/" + TOPOLOGIES, this::getTopologiesResponse);
        router.addGet(sPathRoot + "/" + FEDERATION + "/" + TOPOLOGIES + "/{" + TOPOLOGY_NAME + "}", this::getTopologyResponse);
        router.addGet(sPathRoot + "/" + DESCRIPTION, this::getNodeDescription);

        router.addPost(sPathRoot, this::update);
        router.addPost(sPathRoot + "/{" + OPERATION_NAME + "}", this::executeOperation);
        router.addPost(sPathRoot + "/" + MEMBER_STATE, new LogMemberStateHandler());
        router.addPost(sPathRoot + "/" + MEMBER_DUMP_HEAP, new DumpHeapHandler());
        router.addPost(sPathRoot + "/" + NETWORK_STATS + "/trackWeakest", this::trackWeakestMember);
        router.addPost(sPathRoot + "/" + DIAGNOSTIC_CMD + "/{" + JFR_CMD + "}", this::diagnosticCmd);
        }

    // ----- GET API --------------------------------------------------------

    /**
     * Return NodeMBean attributes for a cluster member.
     *
     * @return the response object
     */
    public Response get(HttpRequest request)
        {
        String sMemberKey = request.getFirstPathParameter(MEMBER_KEY);
        return response(getResponseEntityForMbean(request, getQuery(request, sMemberKey), CHILD_LINKS));
        }

    /**
     * Return a platform(JVM) MBean attributes for a particular MBean of a cluster member.
     *
     * @return the response object
     */
    public Response getPlatformMBeanResponse(HttpRequest request)
        {
        String sMemberKey         = request.getFirstPathParameter(MEMBER_KEY);
        String sPlatformMBeanType = request.getFirstPathParameter(PLATFORM_MBEAN);
        String sBaseQuery         = MAP_PLATFORM_URL_TO_MBEAN_QUERY.get(sPlatformMBeanType);

        if (sBaseQuery == null)
            {
            sBaseQuery = MAP_PLATFORM_PS_URL_TO_MBEAN_QUERY.get(sPlatformMBeanType);
            if (sBaseQuery == null)
                {
                sBaseQuery = MAP_PLATFORM_G1_URL_TO_MBEAN_QUERY.get(sPlatformMBeanType);
                if (sBaseQuery == null)
                    {
                    return Response.status(Response.Status.NOT_FOUND).build();
                    }
                }
            }

        QueryBuilder queryBuilder = createQueryBuilder(request).withBaseQuery(sBaseQuery).withMember(sMemberKey);

        return response(getResponseEntityForMbean(request, queryBuilder));
        }

    /**
     * Return PointToPointMBean attributes for a cluster member.
     *
     * @return the response object
     */
    public Response getPointToPointResponse(HttpRequest request)
        {
        String sMemberKey = request.getFirstPathParameter(MEMBER_KEY);
        return response(getResponseEntityForMbean(request, getPointToPointMBeanQuery(request, sMemberKey)));
        }

    /**
     * Return the response of "reportNodeState" operation of {@code ClusterNodeMBean}.
     *
     * @return the response object
     */
    public Response getStateResponse(HttpRequest request)
        {
        String sMemberKey = request.getFirstPathParameter(MEMBER_KEY);
        return response(getResponseFromMBeanOperation(request, getQuery(request, sMemberKey), STATE, "reportNodeState"));
        }

    /**
     * Return the response of the {@code reportEnvironment} operation of {@code ClusterNodeMBean}.
     *
     * @return the response object
     */
    public Response getEnvironmentResponse(HttpRequest request)
        {
        String sMemberKey = request.getFirstPathParameter(MEMBER_KEY);
        return response(getResponseFromMBeanOperation(request, getQuery(request, sMemberKey), ENVIRONMENT, "reportEnvironment"));
        }

    /**
     * Return the response of "federation" link of a cluster member.
     *
     * @return the response object
     */
    public Response getFederationResponse(HttpRequest request)
        {
        return response(getLinksOnlyResponseBody(request, getParentUri(request), getCurrentUri(request), TOPOLOGIES).toJson());
        }

    /**
     * Return the attributes of TopologyMBean(s) of this cluster member.
     *
     * @return the response object
     */
    public Response getTopologiesResponse(HttpRequest request)
        {
        String       sMemberKey   = request.getFirstPathParameter(MEMBER_KEY);
        QueryBuilder queryBuilder = createQueryBuilder(request)
                .withBaseQuery(FEDERATION_TOPOLOGIES_QUERY)
                .withMember(sMemberKey);

        return response(getResponseBodyForMBeanCollection(request, queryBuilder, NAME,
                null, getParentUri(request), getCurrentUri(request)));
        }

    /**
     * Return the attributes of a TopologyMBean of this cluster member.
     *
     * @return the response object
     */
    public Response getTopologyResponse(HttpRequest request)
        {
        String       sMemberKey = request.getFirstPathParameter(MEMBER_KEY);
        String       sTopologyName = request.getFirstPathParameter(TOPOLOGY_NAME);
        QueryBuilder queryBuilder = createQueryBuilder(request)
                .withBaseQuery(format(FEDERATION_TOPOLOGY_MEMBER_QUERY, sTopologyName))
                .withMember(sMemberKey);

        return response(getResponseEntityForMbean(request, queryBuilder));
        }

    /**
     * Return the member description.
     *
     * @return the response object
     */
    public Response getNodeDescription(HttpRequest request)
        {
        String sMemberKey = request.getFirstPathParameter(MEMBER_KEY);
        return response(getResponseFromMBeanOperation(request, getQuery(request, sMemberKey), DESCRIPTION, "getNodeDescription"));
        }

    // ----- POST API(Update) -----------------------------------------------

    /**
     * Update a ClusterNodeMBean with the parameters present in the input entity map.
     *
     * @return the response object
     */
    public Response update(HttpRequest request)
        {
        Map<String, Object> entity       = getJsonBody(request);
        String              sMemberKey   = request.getFirstPathParameter(MEMBER_KEY);
        QueryBuilder        queryBuilder = getQuery(request, sMemberKey);
        return update(request, entity, queryBuilder);
        }

    // ----- POST API -------------------------------------------------------

    /**
     * Call "shutdown/resetStatistics" operation on NodeMBean.
     *
     * @return the response object
     */
    public Response executeOperation(HttpRequest request)
        {
        String sMemberKey     = request.getFirstPathParameter(MEMBER_KEY);
        String sOperationName = request.getFirstPathParameter(OPERATION_NAME);

        if ("shutdown".equals(sOperationName) || "resetStatistics".equals(sOperationName))
            {
            return executeMBeanOperation(request, getQuery(request, sMemberKey), sOperationName,
                                         null, null);
            }
        return Response.notFound().build();
        }

    /**
     * Call "logNodeState" operation on NodeMBean.
     *
     * @return the response object
     */
    public Response logMemberState(HttpRequest request)
        {
        String sMemberKey = request.getFirstPathParameter(MEMBER_KEY);
        return executeMBeanOperation(request, getQuery(request, sMemberKey),
                "logNodeState", null, null);
        }

    /**
     * {@link RequestRouter.RequestHandler} to call "logNodeState" operation on NodeMBean.
     */
    public class LogMemberStateHandler
            implements RequestRouter.RequestHandler
        {
        @Override
        public Response handle(HttpRequest request)
            {
            String sMemberKey = request.getFirstPathParameter(MEMBER_KEY);
            return executeMBeanOperation(request, getQuery(request, sMemberKey),
                    "logNodeState", null, null);
            }
        }

    /**
     * {@link RequestRouter.RequestHandler} to call "dumpHeap" operation on NodeMBean.
     */
    public class DumpHeapHandler
            implements RequestRouter.RequestHandler
        {
        @Override
        public Response handle(HttpRequest request)
            {
            String sMemberKey = request.getFirstPathParameter(MEMBER_KEY);
            String[] asSignature = {String.class.getName()};
            Object[] aoArguments = {null};

            // dynamically named hprof file, "heapdump-*.hprof" saved in a specified tmp directory
            return executeMBeanOperation(request, getQuery(request, sMemberKey), "dumpHeap", aoArguments, asSignature);
            }
        }

    /**
     * Call "trackWeakest" operation on PointToPointMBean.
     *
     * @return the response object
     */
    public Response trackWeakestMember(HttpRequest request)
        {
        String sMemberKey = request.getFirstPathParameter(MEMBER_KEY);
        return executeMBeanOperation(request, getPointToPointMBeanQuery(request, sMemberKey),
                                     "trackWeakest", null, null);
        }

    /**
     * Call "diagnostic-cmd/jfrCmd" on {@link com.sun.management.DiagnosticCommandMBean}.
     *
     * Valid commands are jfrStart, jfrStop, jfrDump, and jfrCheck. See jcmd JFR
     * command for more information.
     *
     * @return the response object, includes a message returned by the JFR command.
     */
    public Response diagnosticCmd(HttpRequest request)
        {
        String       sMemberKey   = request.getFirstPathParameter(MEMBER_KEY);
        String       sCmd         = request.getFirstPathParameter(JFR_CMD);
        String       sOptions     = request.getFirstQueryParameter(OPTIONS);
        String       sBaseQuery   = ":type=DiagnosticCommand,Domain=com.sun.management,subType=DiagnosticCommand";
        QueryBuilder queryBuilder = createQueryBuilder(request).withBaseQuery(sBaseQuery).withMember(sMemberKey);
        Object[]     aoArguments  = sOptions == null ? new Object[0] : new Object[]{sOptions.split(",")};
        String[]     signature    = sOptions == null ? new String[0] : new String[]{String[].class.getName()};

        // execute the JFR operation and return the result message from the operation
        return response(getResponseFromMBeanOperation(request, queryBuilder,
                "status", sCmd, aoArguments, signature));
        }

    // -------------------------- AbstractManagementResource methods------------------------------------------

    @Override
    @SuppressWarnings("rawtypes")
    protected EntityMBeanResponse getQueryResult(HttpRequest         request,
                                                 URI                 uriParent,
                                                 URI                 uriCurrent,
                                                 Map<String, Object> mapQuery,
                                                 Map<String, String> mapArguments)
        {
        String              sMemberKey = mapArguments.get(MEMBER_KEY);
        Object              oChildren  = getChildrenQuery(mapQuery);
        URI                 uriSelf    = getSubUri(uriParent, sMemberKey);
        EntityMBeanResponse response   = getLinksOnlyResponseBody(request, uriParent, uriSelf, getLinksFilter(request, mapQuery),
                                                                  CHILD_LINKS);

        Map<String, Object> mapResponse = response.getEntity();

        if (oChildren instanceof Map)
            {
            Map mapChildrenQuery = (Map) oChildren;
            addChildMbeanQueryResult(request, uriParent, uriCurrent, NETWORK_STATS, getPointToPointMBeanQuery(request, sMemberKey), mapResponse, mapChildrenQuery);
            addPlatformMBeansQueryResult(request, uriParent, uriCurrent, sMemberKey, mapResponse, mapChildrenQuery);
            addFederationTopologiesQueryResult(request, sMemberKey, mapResponse, mapChildrenQuery, uriSelf);
            }

        return response;
        }

    // ----- ClusterMemberResource methods-----------------------------------

    /**
     * Return the NodeMBean query for the provided member.
     *
     *
     * @param request     the http request
     * @param sMemberKey  the member key
     *
     * @return the MBean query
     */
    protected QueryBuilder getQuery(HttpRequest request, String sMemberKey)
        {
        return createQueryBuilder(request)
                .withBaseQuery(CLUSTER_MEMBERS_QUERY)
                .withMember(sMemberKey);
        }

    /**
     * Return the PointToPoint MBean query for the member.
     *
     *
     * @param request     the http request
     * @param sMemberKey  the member key
     *
     * @return the MBean query
     */
    protected QueryBuilder getPointToPointMBeanQuery(HttpRequest request, String sMemberKey)
        {
        return createQueryBuilder(request)
                .withBaseQuery(POINT_TO_POINT_QUERY)
                .withMember(sMemberKey);
        }

    /**
     * Return the child query for platform. Platform queries are like this
     * {"platform":{
     *     "children":{
     *         "memory":{
     *             "fields":[]
     *         }
     *     }
     * }}
     *
     * @param mapQuery  the query to execute
     *
     * @return the query for a platform MBean.
     */
    @SuppressWarnings("rawtypes")
    protected Object getPlatformChildrenQueryMap(Map mapQuery)
        {
        Object oPlatformQuery = mapQuery.get(PLATFORM);

        // the platform MBeans are queried under children->platform

        Object oChildrenQueryObject = null;
        if (oPlatformQuery instanceof Map)
            {
            oChildrenQueryObject = ((Map) oPlatformQuery).get(CHILDREN);
            }

        return oChildrenQueryObject;
        }

    /**
     * Add platform MBeans to query response.
     *
     * @param request      the {@link HttpRequest}
     * @param uriParent    the parent URI
     * @param uriCurrent   the current URI
     * @param sMemberId    the member ID
     * @param mapResponse  the response map
     * @param mapQuery     the query map
     */
    @SuppressWarnings("rawtypes")
    protected void addPlatformMBeansQueryResult(HttpRequest         request,
                                                URI                 uriParent,
                                                URI                 uriCurrent,
                                                String              sMemberId,
                                                Map<String, Object> mapResponse,
                                                Map                 mapQuery)
        {
        Object oChildrenQueryObject = getPlatformChildrenQueryMap(mapQuery);

        if (oChildrenQueryObject instanceof Map)
            {
            Map<String, Object> mapPlatform                 = new LinkedHashMap<>();
            Map                 mapPlatformChildrenQueryMap = (Map) oChildrenQueryObject;

            // add the child JVM Mbean results
            for (Map.Entry<String, String> entry : MAP_PLATFORM_URL_TO_MBEAN_QUERY.entrySet())
                {
                String sPlatformMBeanKey = entry.getKey();

                QueryBuilder queryBuilder = createQueryBuilder(request)
                        .withBaseQuery(entry.getValue())
                        .withMember(sMemberId);

                addChildMbeanQueryResult(request, uriParent, uriCurrent, sPlatformMBeanKey, queryBuilder,
                                         mapPlatform, mapPlatformChildrenQueryMap);
                }

            // add the child JVM GC Mbean results
            for (Map.Entry<String, String> entry : MAP_PLATFORM_G1_URL_TO_MBEAN_QUERY.entrySet())
                {
                String sPlatformMBeanKey = entry.getKey();

                QueryBuilder queryBuilder = createQueryBuilder(request)
                        .withBaseQuery(entry.getValue())
                        .withMember(sMemberId);

                addChildMbeanQueryResult(request, uriParent, uriCurrent, sPlatformMBeanKey, queryBuilder,
                                         mapPlatform, mapPlatformChildrenQueryMap);
                }

            // add the child JVM Mbean results
            for (Map.Entry<String, String> entry : MAP_PLATFORM_PS_URL_TO_MBEAN_QUERY.entrySet())
                {
                String sPlatformMBeanKey = entry.getKey();

                QueryBuilder queryBuilder = createQueryBuilder(request)
                        .withBaseQuery(entry.getValue())
                        .withMember(sMemberId);

                addChildMbeanQueryResult(request, uriParent, uriCurrent, sPlatformMBeanKey, queryBuilder,
                                         mapPlatform, mapPlatformChildrenQueryMap);
                }

            mapResponse.put(PLATFORM, mapPlatform);
            }
        }

    /**
     * Add federation topologies in the search response.
     *
     * @param request      the {@link HttpRequest}
     * @param sMemberId    the current member Id
     * @param mapResponse  the response map, to which the results must be appended
     * @param mapQuery     the query map
     * @param uriParent    the parent uri
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected void addFederationTopologiesQueryResult(HttpRequest         request,
                                                      String              sMemberId,
                                                      Map<String, Object> mapResponse,
                                                      Map                 mapQuery,
                                                      URI                 uriParent)
        {
        Object oFederationQueryObject = mapQuery.get(FEDERATION);

        if (oFederationQueryObject instanceof Map)
            {
            Map            mapFederationQuery  = (Map) oFederationQueryObject;
            URI            uriSelf             = getSubUri(uriParent, FEDERATION);
            Filter<String> filterLinks         = getLinksFilter(request, mapFederationQuery);
            Object         oFederationChildren = getChildrenQuery(mapFederationQuery);

            Map<String, Object> mapTopologiesResponse =
                    getLinksOnlyResponseBody(request, uriParent, uriSelf, filterLinks, TOPOLOGIES).toJson();

            if (oFederationChildren instanceof Map)
                {
                Object oTopologiesQuery = ((Map) oFederationChildren).get(TOPOLOGIES);
                if (oTopologiesQuery != null)
                    {
                    Map mapTopologiesQuery = (Map) oTopologiesQuery;

                    QueryBuilder queryBuilder = createQueryBuilder(request)
                            .withBaseQuery(FEDERATION_TOPOLOGIES_QUERY)
                            .withMember(sMemberId);

                    EntityMBeanResponse responseEntity
                            = getResponseBodyForMBeanCollection(request, queryBuilder,
                                                                NAME, mapTopologiesQuery, uriParent, uriSelf);

                    mapTopologiesResponse.put(TOPOLOGIES, responseEntity.toJson());

                    }
                }
            mapResponse.put(FEDERATION, mapTopologiesResponse);
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * The constant for dump heap POST API (Operation)
     */
    public static final String MEMBER_DUMP_HEAP = "dumpHeap";

    /**
     * The constant for JFR POST API (Operations)
     */
    public static final String DIAGNOSTIC_CMD = "diagnostic-cmd";

    /**
     * The constant for the child links. The child links are all the platform MBeans and
     * the networkStats.
     */
    public static String[] CHILD_LINKS;

    static
        {
        List<String> listChildLinks = new ArrayList<>();
        listChildLinks.addAll(MAP_PLATFORM_URL_TO_MBEAN_QUERY.keySet().stream()
                .map(s -> PLATFORM + "/" + s).collect(Collectors.toSet()));
        listChildLinks.addAll(MAP_PLATFORM_G1_URL_TO_MBEAN_QUERY.keySet().stream()
                .map(s -> PLATFORM + "/" + s).collect(Collectors.toSet()));

        listChildLinks.addAll(MAP_PLATFORM_PS_URL_TO_MBEAN_QUERY.keySet().stream()
                .map(s -> PLATFORM + "/" + s).collect(Collectors.toSet()));
        listChildLinks.add(NETWORK_STATS);
        listChildLinks.add(FEDERATION);
        CHILD_LINKS = listChildLinks.toArray(new String[]{});
        }
    }
