/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.management.resources;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.internal.http.HttpRequest;
import com.tangosol.internal.http.RequestRouter;
import com.tangosol.internal.http.Response;

import com.tangosol.internal.management.EntityMBeanResponse;

import com.tangosol.net.management.MBeanAccessor.QueryBuilder;

import com.tangosol.util.Resources;

import java.io.IOException;

import java.net.URI;
import java.net.URL;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.tangosol.internal.management.resources.ClusterMemberResource.DIAGNOSTIC_CMD;

/**
 * Handles management API requests for a Coherence cluster level MBeans.
 *
 * @author sr 2017.08.21
 * @author Jonathan Knight  2022.01.25
 * @since 12.2.1.4.0
 */
public class ClusterResource
        extends AbstractManagementResource
    {
    // ----- Routes methods -------------------------------------------------

    @Override
    public void addRoutes(RequestRouter router, String sPathRoot)
        {
        router.addGet(sPathRoot, this::get);
        router.addGet(sPathRoot + "/" + MANAGEMENT, this::getManagement);
        router.addGet(sPathRoot + "/" + JOURNAL, this::getJournalResponse);
        router.addGet(sPathRoot + "/" + PLATFORM + "/{" + PLATFORM_MBEAN + "}", this::getPlatformResponse);
        router.addGet(sPathRoot + "/" + METADATA_CATALOG, this::getMetadataCatalog)
                .produces(MEDIA_TYPE_JSON, MEDIA_TYPE_SWAGGER_JSON);
        router.addGet(sPathRoot + "/" + GET_CLUSTER_CONFIG, this::getClusterConfig)
                .produces(MEDIA_TYPE_XML);
        router.addGet(sPathRoot + "/" + DESCRIPTION, this::getClusterDescription);

        router.addPost(sPathRoot, this::updateNodes);
        router.addPost(sPathRoot + "/" + SHUTDOWN, this::shutdownCluster);
        router.addPost(sPathRoot + "/" + CLUSTER_STATE, this::logClusterState);
        router.addPost(sPathRoot + "/" + DUMP_CLUSTER_HEAP, this::dumpClusterHeap);
        router.addPost(sPathRoot + "/" + CONFIGURE_TRACING, this::configureTracing);
        router.addPost(sPathRoot + "/" + MANAGEMENT, this::updateJMXManagement);
        router.addPost(sPathRoot + "/" + SEARCH, this::search);
        router.addPost(sPathRoot + "/" + DIAGNOSTIC_CMD + "/{" + JFR_CMD + "}", this::diagnosticCmd);
        router.addPost(sPathRoot + "/{" + OPERATION_NAME + "}", this::executeOperation);
        router.addPost(sPathRoot + "/" + NETWORK_STATS + "/trackWeakest", this::trackWeakestMember);

        // child resources
        router.addRoutes(sPathRoot + "/" + CACHES, new CachesResource());
        router.addRoutes(sPathRoot + "/" + EXECUTORS, new ExecutorsResource());
        router.addRoutes(sPathRoot + "/" + HEALTH, new HealthsResource());
        router.addRoutes(sPathRoot + "/" + JOURNAL + "/{" + JOURNAL_TYPE + "}", new JournalResource());
        router.addRoutes(sPathRoot + "/" + MEMBERS, new ClusterMembersResource());
        router.addRoutes(sPathRoot + "/" + REPORTERS, new ReportersResource());
        router.addRoutes(sPathRoot + "/" + SERVICES, new ServicesResource());
        router.addRoutes(sPathRoot + "/" + STORAGE, new StorageManagersResource());
        router.addRoutes(sPathRoot + "/" + TOPICS, new TopicsResource());
        router.addRoutes(sPathRoot + "/" + VIEWS, new ViewsResource());
        }

    // ----- GET API --------------------------------------------------------

    /**
     * Return the swagger document for the Management Resource.
     *
     * @return the response object
     */
    public Response getMetadataCatalog()
        {
        URL url = Resources.findFileOrResource(SWAGGER_RESOURCE, this.getClass().getClassLoader());
        try
            {
            return url == null
                       ? Response.notFound().build()
                       : Response.ok(url.openStream()).build();
            }
        catch (IOException e)
            {
            Logger.warn("Exception occurred while returning Swagger resource " + SWAGGER_RESOURCE, e);
            return Response.serverError().build();
            }
        }

    /**
     * Return the attributes of a ClusterMBean.
     *
     * @param request  the {@link HttpRequest}
     *
     * @return the response object
     */
    public Response get(HttpRequest request)
        {
        return response(getResponseEntityForMbean(request, getQuery(request), CHILD_LINKS));
        }

    /**
     * Return the attributes of a ManagementMBean.
     *
     * @param request  the {@link HttpRequest}
     *
     * @return the response object
     */
    public Response getManagement(HttpRequest request)
        {
        return response(getResponseEntityForMbean(request, getManagementQuery(request)));
        }

    /**
     * Return the response to a "journal" link for a cluster.
     *
     * @param request  the {@link HttpRequest}
     *
     * @return the response object
     */
    public Response getJournalResponse(HttpRequest request)
        {
        return response(getLinksOnlyResponseBody(request, getParentUri(request), getCurrentUri(request), "ram", "flash").toJson());
        }


    /**
     * Return aggregated metrics of a platform(JVM) MBean across cluster members.
     *
     * @param request  the {@link HttpRequest}
     *
     * @return the response object
     */
    public Response getPlatformResponse(HttpRequest request)
        {
        String sPlatformMBeanType = request.getFirstPathParameter(PLATFORM_MBEAN);
        String sRoleName          = request.getFirstPathParameter(ROLE_NAME);
        String sCollector         = request.getFirstPathParameter(COLLECTOR);

        String sBaseQuery = MAP_PLATFORM_URL_TO_MBEAN_QUERY.get(sPlatformMBeanType);

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

        QueryBuilder        queryBuilder = createQueryBuilder(request).withBaseQuery(sBaseQuery);
        EntityMBeanResponse response  = getLinksOnlyResponseBody(request, getParentUri(request), getCurrentUri(request));

        Map<String, Object> responseMap = response.toJson();

        addAggregatedMetricsToResponseMap(request, sRoleName, sCollector, queryBuilder, responseMap);

        return response(responseMap);
        }

    /**
     * Return the cluster description.
     *
     * @param request  the {@link HttpRequest}
     *
     * @return the response object
     */
    public Response getClusterDescription(HttpRequest request)
        {
        return response(getResponseFromMBeanOperation(request, getQuery(request), DESCRIPTION, "getClusterDescription"));
        }

    // ----- POST API (Operations) ------------------------------------------

    /**
     * Call "shutdown" operation on ClusterMBean.
     *
     * @param request  the {@link HttpRequest}
     *
     * @return the response object
     */
    public Response shutdownCluster(HttpRequest request)
        {
        return executeMBeanOperation(request, getQuery(request), "shutdown", null, null);
        }

    /**
     * Call "logClusterState" operation on ClusterMBean.
     *
     * @param request  the {@link HttpRequest}
     *
     * @return the response object
     */
    public Response logClusterState(HttpRequest request)
        {
        Map<String, Object> mapBody   = getJsonBody(request);
        String              sRoleName = (String) mapBody.get(ROLE_NAME);
        return executeMBeanOperation(request, getQuery(request), CLUSTER_STATE,
                new Object[]{sRoleName}, new String[] {String.class.getName()});
        }

    /**
     * Call "dumpClusterHeap" operation on ClusterMBean.
     *
     * @param request  the {@link HttpRequest}
     *
     * @return the response object
     */
    public Response dumpClusterHeap(HttpRequest request)
        {
        Map<String, Object> mapBody       = getJsonBody(request);
        String              sRoleName     = (String) mapBody.get(ROLE_NAME);
        return executeMBeanOperation(request, getQuery(request), DUMP_CLUSTER_HEAP,
                                     new Object[]{sRoleName}, new String[] {String.class.getName()});
        }

    /**
     * Call the {@value CONFIGURE_TRACING} operation on ClusterMBean.
     * <p>
     * This call expects two parameters, {@value ROLE} and {@value TRACING_RATIO}.  {@value ROLE} allows enabling
     * tracing on members matching the specified role.  If {@value ROLE} is null or a zero-length string,
     * {@value TRACING_RATIO} will be applied to all members of the cluster. {@value TRACING_RATIO} specifies
     * the ratio of tracing spans that will be captured.  A value of {@code -1} disables tracing.  A value of
     * {@code 0} means Coherence will not initiate spans unless an active span is already present.
     * A value between {@code 0} (exclusively) and {@code 1.0} (inclusively) represents the percentage of tracing
     * spans that will be captured.
     *
     * @param request  the {@link HttpRequest}
     *
     * @return the response object
     *
     * @since 14.1.1.0
     */
    public Response configureTracing(HttpRequest request)
        {
        Map<String, Object> mapParameters = getJsonBody(request);
        Object              oRole         = mapParameters == null ? null  : mapParameters.get(ROLE);
        float               oRatio        = mapParameters == null ? -1.0f : Float.parseFloat(mapParameters.get(TRACING_RATIO).toString());

        return executeMBeanOperation(request, getQuery(request),
                                     CONFIGURE_TRACING,
                                     new Object[]{oRole, oRatio},
                                     new String[] {String.class.getName(), Float.class.getName()});
        }

    /**
     * Call "shutdown/resetStatistics" operation on NodeMBean for all the members.
     *
     * @param request  the {@link HttpRequest}
     *
     * @return the response object
     */
    public Response executeOperation(HttpRequest request)
        {
        String sOperationName = request.getFirstPathParameter(OPERATION_NAME);

        if (sOperationName.equalsIgnoreCase("logMemberState"))
            {
            sOperationName = "logNodeState";
            }
        if ("shutdown".equals(sOperationName) || "resetStatistics".equals(sOperationName)
             || "logNodeState".equals(sOperationName))
            {
            return executeMBeanOperation(request, getMembersQuery(request), sOperationName,
                    null, null);
            }
        return Response.notFound().build();
        }

    /**
     * Call "trackWeakest" operation on PointToPointMBean for all the members.
     *
     * @param request  the {@link HttpRequest}
     *
     * @return the response object
     */
    public Response trackWeakestMember(HttpRequest request)
        {
        return executeMBeanOperation(request, getPointToPointMBeanQuery(request),
                "trackWeakest", null, null);
        }

    // ----- POST API(Update) -----------------------------------------------

    /**
     * Update a ManagementMBean with the parameters present in the input entity map.
     *
     * @param request  the {@link HttpRequest}
     *
     * @return the response object
     */
    public Response updateJMXManagement(HttpRequest request)
        {
        Map<String, Object> mapParameters = getJsonBody(request);
        return update(request, mapParameters, getManagementQuery(request));
        }

    /**
     * Update a ClusterNodeMBean with the parameters present in the input
     * entity map for all the members.
     *
     * @param request  the {@link HttpRequest}
     *
     * @return the response object
     */
    public Response updateNodes(HttpRequest request)
        {
        Map<String, Object> entity       = getJsonBody(request);
        QueryBuilder        queryBuilder = getMembersQuery(request);

        return update(request, entity, queryBuilder);
        }

    // ----- POST API(Search) -----------------------------------------------

    public Response search(HttpRequest request)
        {
        Map<String, Object> mapQuery   = getJsonBody(request);
        URI                 uriParent  = getParentUri(request);
        URI                 uriCurrent = getCurrentUri(request);
        String              sCluster   = getClusterName(request);
        return response(getSearchResults(request, sCluster, mapQuery, uriParent, uriCurrent, uriCurrent));
        }

    // ----- Child Resources ------------------------------------------------

    /**
     * Call "diagnostic-cmd/jfrCmd" to perform JFR operation on ClusterMBean.
     *
     * Valid commands are jfrStart, jfrStop, jfrDump, and jfrCheck. See jcmd JFR
     * command for valid options.  E.g.
     * jfrStart?options=name=myJfr,duration=3s,filename=/tmp/myRecording.jfr
     *
     * @param request  the {@link HttpRequest}
     *
     * @return the response object
     */
    public Response diagnosticCmd(HttpRequest request)
        {
        String sCmd     = request.getFirstPathParameter(JFR_CMD);
        String sOptions = request.getFirstQueryParameter(OPTIONS);
        String sRole    = request.getFirstQueryParameter(ROLE_NAME);
        // execute the role based cluster wide JFR operation and return
        // the result message from the operation
        return response(getResponseFromMBeanOperation(request, getQuery(request),
                                                      "status", "flightRecording", new Object[]{sRole, sCmd, sOptions},
                                                      new String[]{String.class.getName(), String.class.getName(), String.class.getName()}));
        }

    // ----- ClusterResource methods ----------------------------------------

    /**
     * Get Coherence Cluster Configuration
     *
     * @param request  the {@link HttpRequest}
     *
     * @return the response object
     *
     * @since 14.1.2.0
     */
    public Response getClusterConfig(HttpRequest request)
        {
        return executeMBeanOperation(request, getQuery(request),
                                     GET_CLUSTER_CONFIG,
                                     null,
                                     null);
        }

    /**
     * The response for a Coherence ClusterMBean.
     *
     * @param request       the {@link HttpRequest}
     * @param uriParent     the parent URI
     * @param sClusterName  the cluster name
     *
     * @return response for a Coherence ClusterMBean
     */
    public Map<String, Object> getClusterResponseMap(HttpRequest request, URI uriParent, String sClusterName)
        {
        EntityMBeanResponse response = getResponseEntityForMbean(request, getQuery(request),
                                                                 uriParent,
                                                                 getSubUri(uriParent, sClusterName),
                                                                 null,
                                                                 CHILD_LINKS);

        return response != null ? response.toJson() : new LinkedHashMap<>();
        }

    /**
     * Return the search results for a Coherence Cluster.
     *
     *
     * @param request     the {@link HttpRequest}
     * @param mapQuery    the Query map
     * @param uriParent   the parent URI of the current resource
     * @param uriCurrent  the current URI of the resource
     * @param uriChild    the children current URI of the resource
     *
     * @return  the cluster search results
     */
    @SuppressWarnings({"CollectionAddAllCanBeReplacedWithConstructor", "unchecked", "rawtypes"})
    public Map<String, Object> getSearchResults(HttpRequest request, String sCluster, Map<String, Object> mapQuery, URI uriParent, URI uriCurrent, URI uriChild)
        {
        Map<String, Object> mapResponse = new LinkedHashMap<>();

        EntityMBeanResponse response =
                getResponseEntityForMbean(request, getQuery(sCluster), uriParent, uriCurrent, mapQuery);

        mapResponse.putAll(response.toJson());

        Object oChildren = getChildrenQuery(mapQuery);

        if (oChildren instanceof Map)
            {
            Map<String, Object> mapChildrenQuery = (Map<String, Object>) oChildren;

            addChildMbeanQueryResult(request, uriParent, uriChild, MANAGEMENT, getManagementQuery(request), mapResponse, mapChildrenQuery);

            addChildResourceQueryResult(request, new ServicesResource(), SERVICES, mapResponse, mapChildrenQuery, null);
            addChildResourceQueryResult(request, new ClusterMembersResource(), MEMBERS, mapResponse, mapChildrenQuery, null);
            addChildResourceQueryResult(request, new ReportersResource(), REPORTERS, mapResponse, mapChildrenQuery, null);
            addChildResourceQueryResult(request, new ExecutorsResource(), EXECUTORS, mapResponse, mapChildrenQuery, null);

            Object oJournal = mapChildrenQuery.get(JOURNAL);

            if (oJournal instanceof Map)
                {
                Map<String, Object> mapJournal       = new LinkedHashMap<>();
                Object              oJournalChildren = getChildrenQuery((Map) oJournal);

                if (oJournalChildren instanceof Map)
                    {
                    Map mapJournalQuery = (Map) oJournalChildren;
                    addChildResourceQueryResult(request, new JournalResource(), RAM_JOURNAL_TYPE, mapJournal, mapJournalQuery,
                                                Collections.singletonMap(JOURNAL_TYPE, RAM_JOURNAL_TYPE));

                    addChildResourceQueryResult(request, new JournalResource(), FLASH_JOURNAL_TYPE, mapJournal, mapJournalQuery,
                                                Collections.singletonMap(JOURNAL_TYPE, FLASH_JOURNAL_TYPE));

                    mapResponse.put(JOURNAL, mapJournal);
                    }
                }
            }

        return mapResponse;
        }

    /**
     * The MBean query for ClusterMBean.
     *
     * @param request  the {@link HttpRequest}
     *
     * @return the MBean query
     */
    protected QueryBuilder getQuery(HttpRequest request)
        {
        return getQuery(getClusterName(request));
        }

    /**
     * The MBean query for ClusterMBean.
     *
     * @param sCluster  the Coherence cluster name
     *
     * @return the MBean query
     */
    protected QueryBuilder getQuery(String sCluster)
        {
        return createQueryBuilder(sCluster).withBaseQuery(CLUSTER_QUERY);
        }

    /**
     * The MBean query for ManagementMBean.
     *
     * @param request  the {@link HttpRequest}
     *
     * @return the MBean query
     */
    protected QueryBuilder getManagementQuery(HttpRequest request)
        {
        return createQueryBuilder(request)
                .withBaseQuery(MANAGEMENT_QUERY);
        }

    /**
     * The MBean query for NodeMBean.
     *
     * @param request  the {@link HttpRequest}
     *
     * @return the MBean query
     */
    protected QueryBuilder getMembersQuery(HttpRequest request)
        {
        return createQueryBuilder(request)
                .withBaseQuery(CLUSTER_MEMBERS_QUERY);
        }

    /**
     * Return the PointToPoint MBean query for all the members.
     *
     * @param request  the {@link HttpRequest}
     *
     * @return the MBean query
     */
    protected QueryBuilder getPointToPointMBeanQuery(HttpRequest request)
        {
        return createQueryBuilder(request)
                .withBaseQuery(POINT_TO_POINT_QUERY)
                .withMember("*");
        }

    // ----- POST API (Operations) constants --------------------------------

    public static final String DUMP_CLUSTER_HEAP = "dumpClusterHeap";

    /**
     * The REST resource name and path element to configure OpenTracing.
     *
     * @since 14.1.1.0
     */
    public static final String CONFIGURE_TRACING = "configureTracing";

    // ----- GET API (Operations) constants --------------------------------

    /**
     * The name of operation to get Coherence cluster configuration
     *
     *  @since 14.1.2.0
     */
    public static final String GET_CLUSTER_CONFIG = "getClusterConfig";

    // ----- constants ------------------------------------------------------

    public static final String ROLE = "role";

    /**
     * Constant for {@code POST} parameter {@code tracing-ratio}.
     *
     * @since 14.1.1.0
     */
    public static final String TRACING_RATIO = "tracingRatio";

    public static final String[] CHILD_LINKS = {SERVICES, CACHES, MEMBERS, MANAGEMENT, JOURNAL, HOTCACHE, REPORTERS, WEB_APPS, EXECUTORS, TOPICS, STORAGE, VIEWS};
    }
