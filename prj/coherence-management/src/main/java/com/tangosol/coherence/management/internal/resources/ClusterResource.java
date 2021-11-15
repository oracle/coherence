/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.management.internal.resources;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.coherence.management.internal.EntityMBeanResponse;

import com.tangosol.net.management.MBeanAccessor.QueryBuilder;

import com.tangosol.util.Resources;

import java.io.IOException;

import java.net.URI;
import java.net.URL;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import javax.ws.rs.QueryParam;

import javax.ws.rs.WebApplicationException;

import javax.ws.rs.core.Response;

import static com.tangosol.coherence.management.internal.resources.ClusterMemberResource.DIAGNOSTIC_CMD;

/**
 * Handles management API requests for a Coherence cluster level MBeans.
 *
 * @author sr 2017.08.21
 * @since 12.2.1.4.0
 */
public class ClusterResource
        extends AbstractManagementResource
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a ClusterResource.
     *
     * @param resource  the {@link AbstractManagementResource} to be used to initialize the context
     */
    public ClusterResource(AbstractManagementResource resource)
        {
        super(resource);
        }

    // ----- GET API --------------------------------------------------------

    /**
     * Return the swagger document for the Management Resource.
     *
     * @return the response object
     */
    @GET
    @Produces(MEDIA_TYPES)
    @Consumes({MEDIA_TYPES, MEDIA_TYPE_SWAGGER_JSON})
    @Path(METADATA_CATALOG)
    public Response getMetadataCatalog()
        {
        URL url = Resources.findFileOrResource(SWAGGER_RESOURCE, this.getClass().getClassLoader());
        try
            {
            return url == null
                       ? Response.status(Response.Status.NOT_FOUND).build()
                       : Response.ok(url.openStream()).build();
            }
        catch (IOException e)
            {
            Logger.warn("Exception occurred while returning Swagger resource " + SWAGGER_RESOURCE, e);
            throw new WebApplicationException();
            }
        }

    /**
     * Return the attributes of a ClusterMBean.
     *
     * @return the response object
     */
    @GET
    @Produces(MEDIA_TYPES)
    public Response get()
        {
        return response(getResponseEntityForMbean(getQuery(), CHILD_LINKS));
        }

    /**
     * Return the attributes of a ManagementMBean.
     *
     * @return the response object
     */
    @GET
    @Produces(MEDIA_TYPES)
    @Path(MANAGEMENT)
    public Response getManagement()
        {
        return response(getResponseEntityForMbean(getManagementQuery()));
        }

    /**
     * Return the response to a "journal" link for a cluster.
     *
     * @return the response object
     */
    @GET
    @Produces(MEDIA_TYPES)
    @Path("journal")
    public Response getJournalResponse()
        {
        return response(getLinksOnlyResponseBody(getParentUri(), getCurrentUri(), "ram", "flash").toJson());
        }


    /**
     * Return aggregated metrics of a platform(JVM) MBean across cluster members.
     *
     * @param sPlatformMBeanType  the JVM MBean type
     * @param sRoleName           either a regex to be applied against node ids or a role name
     * @param sCollector          the collector to use instead of the default*
     *
     * @return the response object
     */
    @GET
    @Produces(MEDIA_TYPES)
    @Path(PLATFORM + "/{" + PLATFORM_MBEAN + "}")
    public Response getPlatformResponse(@PathParam(PLATFORM_MBEAN) String sPlatformMBeanType,
                                        @QueryParam(ROLE_NAME)     String sRoleName,
                                        @QueryParam(COLLECTOR)     String sCollector)
        {
        String sBaseQuery = MAP_PLATFORM_URL_TO_MBEAN_QUERY.get(sPlatformMBeanType);

        if (sBaseQuery == null)
            {
            sBaseQuery = MAP_PLATFORM_PS_URL_TO_MBEAN_QUERY.get(sPlatformMBeanType);
            if (sBaseQuery == null)
                {
                sBaseQuery = MAP_PLATFORM_G1_URL_TO_MBEAN_QUERY.get(sPlatformMBeanType);
                if (sBaseQuery == null)
                    {
                    throw new WebApplicationException(Response.Status.NOT_FOUND);
                    }
                }
            }

        QueryBuilder        bldrQuery = createQueryBuilder().withBaseQuery(sBaseQuery);
        EntityMBeanResponse response  = getLinksOnlyResponseBody(getParentUri(), getCurrentUri());

        Map<String, Object> responseMap = response.toJson();

        addAggregatedMetricsToResponseMap(sRoleName, sCollector, bldrQuery, responseMap);

        return response(responseMap);
        }

    // ----- POST API (Operations) ------------------------------------------

    /**
     * Call "shutdown" operation on ClusterMBean.
     *
     * @return the response object
     */
    @POST
    @Produces(MEDIA_TYPES)
    @Path("shutdown")
    public Response shutdownCluster()
        {
        return executeMBeanOperation(getQuery(), "shutdown", null, null);
        }

    /**
     * Call "logClusterState" operation on ClusterMBean.
     *
     * @param mapParameters  the method parameters map
     *
     * @return the response object
     */
    @POST
    @Produces(MEDIA_TYPES)
    @Consumes(MEDIA_TYPES)
    @Path("logClusterState")
    public Response logClusterState(Map<String, Object> mapParameters)
        {
        Object oRole = mapParameters == null ? null : mapParameters.get(ROLE);
        return executeMBeanOperation(getQuery(), "logClusterState",
                new Object[]{oRole}, new String[] {String.class.getName()});
        }

    /**
     * Call "dumpClusterHeap" operation on ClusterMBean.
     *
     * @param mapParameters  the method parameters map
     *
     * @return the response object
     */
    @POST
    @Produces(MEDIA_TYPES)
    @Consumes(MEDIA_TYPES)
    @Path(DUMP_CLUSTER_HEAP)
    public Response dumpClusterHeap(Map<String, Object> mapParameters)
        {
        Object oRole = mapParameters == null ? null : mapParameters.get(ROLE);
        return executeMBeanOperation(getQuery(), DUMP_CLUSTER_HEAP,
                new Object[]{oRole}, new String[] {String.class.getName()});
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
     * @param mapParameters  the method parameters map
     *
     * @return the response object
     *
     * @since 14.1.1.0
     */
    @POST
    @Produces(MEDIA_TYPES)
    @Consumes(MEDIA_TYPES)
    @Path(CONFIGURE_TRACING)
    public Response configureTracing(Map<String, Object> mapParameters)
        {
        Object oRole  = mapParameters == null ? null  : mapParameters.get(ROLE);
        Float  oRatio = mapParameters == null ? -1.0f : Float.parseFloat(mapParameters.get(TRACING_RATIO).toString());

        return executeMBeanOperation(getQuery(),
                                     CONFIGURE_TRACING,
                                     new Object[]{oRole, oRatio},
                                     new String[] {String.class.getName(), Float.class.getName()});
        }

    // ----- POST API(Update) -----------------------------------------------

    /**
     * Update a ManagementMBean with the parameters present in the input entity map.
     *
     * @param mapParameters  the method parameters map
     *
     * @return the response object
     */
    @POST
    @Produces(MEDIA_TYPES)
    @Consumes(MEDIA_TYPES)
    @Path(MANAGEMENT)
    public Response updateJMXManagement(Map<String, Object> mapParameters)
        {
        return update(mapParameters, getManagementQuery());
        }

    // ----- POST API(Search) -----------------------------------------------

    @POST
    @Produces(MEDIA_TYPES)
    @Path(SEARCH)
    public Response search(Map<String, Object> mapQuery)
        {
        return response(getSearchResults(mapQuery, getParentUri(), getCurrentUri()));
        }

    // ----- Child Resources ------------------------------------------------

    /**
     * Sub resource for cluster members.
     *
     * @return the cluster members sub resource
     */
    @Path(MEMBERS)
    public Object getMembersResource()
        {
        return new ClusterMembersResource(this);
        }

    /**
     * Sub resource for services.
     *
     * @return the services sub resource
     */
    @Path(SERVICES)
    public Object getServicesResource()
        {
        return new ServicesResource(this);
        }

    /**
     * Sub resource for caches.
     *
     * @return the caches sub resource
     */
    @Path(CACHES)
    public Object getCachesResource()
        {
        return new CachesResource(this);
        }

    /**
     * Sub resource for a journal type.
     *
     * @return the journal sub resource
     */
    @Path(JOURNAL + "/{" + JOURNAL_TYPE + ":ram|flash}")
    public Object getJournalResource()
        {
        return new JournalResource(this);
        }

    /**
     * Sub resource for C*W applications.
     *
     * @return the C*W applications sub resource
     */
    @Path(WEB_APPS)
    public Object getWebAppsResource()
        {
        return new CWebResource(this);
        }

    /**
     * Sub resource for reporters.
     *
     * @return the reporters sub resource
     */
    @Path(REPORTERS)
    public Object getReportersResource()
        {
        return new ReportersResource(this);
        }

    /**
     * Call "diagnostic-cmd/jfrCmd" to perform JFR operation on ClusterMBean.
     *
     * Valid commands are jfrStart, jfrStop, jfrDump, and jfrCheck. See jcmd JFR
     * command for valid options.  E.g.
     * jfrStart?options=name=myJfr,duration=3s,filename=/tmp/myRecording.jfr
     *
     * @param sCmd      the JFR command
     * @param sOptions  the comma separated JFR options
     * @param sRole     the Coherence role name
     *
     * @return the response object
     */
    @POST
    @Produces(MEDIA_TYPES)
    @Consumes(MEDIA_TYPES)
    @Path(DIAGNOSTIC_CMD + "/{" + JFR_CMD + "}")
    public Response diagnosticCmd(@PathParam(JFR_CMD)    String sCmd,
                                  @QueryParam(OPTIONS)   String sOptions,
                                  @QueryParam(ROLE_NAME) String sRole)
        {
        // execute the role based cluster wide JFR operation and return
        // the result message from the operation
        return response(getResponseFromMBeanOperation(getQuery(),
                "status", "flightRecording", new Object[]{sRole, sCmd, sOptions},
                new String[]{String.class.getName(), String.class.getName(), String.class.getName()}));
        }

    /**
     * Sub resource for executors.
     *
     * @return the executors sub resource
     *
     * @since 21.12
     */
    @Path(EXECUTORS)
    public Object getExecutorsResource()
        {
        return new ExecutorsResource(this);
        }

    // ----- ClusterResource methods ----------------------------------------

    /**
     * The response for a Coherence ClusterMBean.
     *
     * @param uriParent     the parent URI
     * @param sClusterName  the cluster name
     *
     * @return response for a Coherence ClusterMBean
     */
    public Map<String, Object> getClusterResponseMap(URI uriParent, String sClusterName)
        {
        EntityMBeanResponse response = getResponseEntityForMbean(getQuery(),
                                                                 uriParent,
                                                                 getSubUri(uriParent, sClusterName),
                                                                 null,
                                                                 CHILD_LINKS);

        return response != null ? response.toJson() : new LinkedHashMap<>();
        }

    /**
     * Return the search results for a Coherence Cluster.
     *
     * @param mapQuery    the Query map
     * @param uriParent   the parent URI of the current resource
     * @param uriCurrent  the current URI of the resource
     *
     * @return  the cluster search results
     */
    @SuppressWarnings("CollectionAddAllCanBeReplacedWithConstructor")
    public Map<String, Object> getSearchResults(Map<String, Object> mapQuery, URI uriParent, URI uriCurrent)
        {
        Map<String, Object> mapResponse = new LinkedHashMap<>();

        EntityMBeanResponse response =
                getResponseEntityForMbean(getQuery(), uriParent, uriCurrent, mapQuery);

        mapResponse.putAll(response.toJson());

        Object oChildren = getChildrenQuery(mapQuery);

        if (oChildren instanceof Map)
            {
            Map mapChildrenQuery = (Map) oChildren;

            addChildMbeanQueryResult(MANAGEMENT, getManagementQuery(), mapResponse, mapChildrenQuery);

            addChildResourceQueryResult(new ServicesResource(this), SERVICES, mapResponse, mapChildrenQuery, null,
                    uriCurrent);
            addChildResourceQueryResult(new ClusterMembersResource(this), MEMBERS, mapResponse, mapChildrenQuery, null,
                    uriCurrent);
            addChildResourceQueryResult(new ReportersResource(this), REPORTERS, mapResponse, mapChildrenQuery, null,
                    uriCurrent);
            addChildResourceQueryResult(new CWebResource(this), WEB_APPS, mapResponse, mapChildrenQuery, null,
                    uriCurrent);
            addChildResourceQueryResult(new ExecutorsResource(this), EXECUTORS, mapResponse, mapChildrenQuery, null,
                    uriCurrent);

            Object oJournal = mapChildrenQuery.get(JOURNAL);

            if (oJournal instanceof Map)
                {
                Map<String, Object> mapJournal       = new LinkedHashMap<>();
                Object              oJournalChildren = getChildrenQuery((Map) oJournal);

                if (oJournalChildren instanceof Map)
                    {
                    Map mapJournalQuery = (Map) oJournalChildren;
                    addChildResourceQueryResult(new JournalResource(this), RAM_JOURNAL_TYPE, mapJournal, mapJournalQuery,
                            Collections.singletonMap(JOURNAL_TYPE, RAM_JOURNAL_TYPE), uriCurrent);

                    addChildResourceQueryResult(new JournalResource(this), FLASH_JOURNAL_TYPE, mapJournal, mapJournalQuery,
                            Collections.singletonMap(JOURNAL_TYPE, FLASH_JOURNAL_TYPE), uriCurrent);

                    mapResponse.put(JOURNAL, mapJournal);
                    }
                }
            }

        return mapResponse;
        }

    /**
     * The MBean query for ClusterMBean.
     *
     * @return the MBean query
     */
    protected QueryBuilder getQuery()
        {
        return createQueryBuilder().withBaseQuery(CLUSTER_QUERY);
        }

    /**
     * The MBean query for ManagementMBean.
     *
     * @return the MBean query
     */
    protected QueryBuilder getManagementQuery()
        {
        return createQueryBuilder()
                .withBaseQuery(MANAGEMENT_QUERY);
        }

    // ----- POST API (Operations) constants --------------------------------

    public static final String DUMP_CLUSTER_HEAP = "dumpClusterHeap";

    /**
     * The REST resource name and path element to configure OpenTracing.
     *
     * @since 14.1.1.0
     */
    public static final String CONFIGURE_TRACING = "configureTracing";

    // ----- constants ------------------------------------------------------

    public static final String ROLE = "role";

    /**
     * Constant for {@code POST} parameter {@code tracing-ratio}.
     *
     * @since 14.1.1.0
     */
    public static final String TRACING_RATIO = "tracingRatio";

    public static final String[] CHILD_LINKS = {SERVICES, CACHES, MEMBERS, MANAGEMENT, JOURNAL, REPORTERS, WEB_APPS, EXECUTORS};
    }
