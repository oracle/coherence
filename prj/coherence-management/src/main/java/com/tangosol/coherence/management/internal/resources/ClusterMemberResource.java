/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.management.internal.resources;

import com.tangosol.coherence.management.internal.EntityMBeanResponse;

import com.tangosol.net.management.MBeanAccessor.QueryBuilder;

import com.tangosol.util.Filter;

import java.net.URI;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;

import javax.ws.rs.core.Response;

import static java.lang.String.format;

/**
 * Handles management API requests for Coherence cluster member.
 *
 * @author sr  2017.08.21
 * @since 12.2.1.4.0
 */
public class ClusterMemberResource
        extends AbstractManagementResource
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a ClusterMemberResource.
     *
     * @param resource  the {@link AbstractManagementResource} to be used to initialize the context
     */
    public ClusterMemberResource(AbstractManagementResource resource)
        {
        super(resource);
        }

    // ----- GET API --------------------------------------------------------

    /**
     * Return NodeMBean attributes for a cluster member.
     *
     * @param sMemberKey  the member key, can be a member name or node Id
     *
     * @return the response object
     */
    @GET
    @Produces(MEDIA_TYPES)
    public Response get(@PathParam(MEMBER_KEY) String sMemberKey)
        {
        return response(getResponseEntityForMbean(getQuery(sMemberKey), CHILD_LINKS));
        }

    /**
     * Return a platform(JVM) MBean attributes for a particular MBean of a cluster member.
     *
     * @param sMemberKey          the member key, can be a member name or node Id
     * @param sPlatformMBeanType  the JVM MBean type
     *
     * @return the response object
     */
    @GET
    @Produces(MEDIA_TYPES)
    @Path(PLATFORM + "/{" + PLATFORM_MBEAN + "}")
    public Response getPlatformMBeanResponse(@PathParam(MEMBER_KEY)     String sMemberKey,
                                             @PathParam(PLATFORM_MBEAN) String sPlatformMBeanType)
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

        QueryBuilder bldrQuery = createQueryBuilder().withBaseQuery(sBaseQuery).withMember(sMemberKey);

        return response(getResponseEntityForMbean(bldrQuery));
        }

    /**
     * Return PointToPointMBean attributes for a cluster member.
     *
     * @param sMemberKey  the member key, can be a member name or node Id
     *
     * @return the response object
     */
    @GET
    @Produces(MEDIA_TYPES)
    @Path("{" + NETWORK_STATS + "}")
    public Response getPointToPointResponse(@PathParam(MEMBER_KEY) String sMemberKey)
        {
        return response(getResponseEntityForMbean(getPointToPointMBeanQuery(sMemberKey)));
        }

    /**
     * Return the response of "reportNodeState" operation of NodeMBean
     *
     * @param sMemberKey  the member key, can be a member name or node Id
     *
     * @return the response object
     */
    @GET
    @Produces(MEDIA_TYPES)
    @Path("state")
    public Response getStateResponse(@PathParam(MEMBER_KEY) String sMemberKey) throws Exception
        {
        return response(getResponseFromMBeanOperation(getQuery(sMemberKey),
                "state", "reportNodeState"));
        }

    // ----- POST API(Update) -----------------------------------------------

    /**
     * Update a ClusterNodeMBean with the parameters present in the input entity map.
     *
     * @param sMemberKey  the member key, can be a member name or node Id
     * @param entity      the input entity map containing the updated attributes
     *
     * @return the response object
     */
    @POST
    @Produces(MEDIA_TYPES)
    @Consumes(MEDIA_TYPES)
    public Response update(@PathParam(MEMBER_KEY) String sMemberKey, Map<String, Object> entity)
        {
        QueryBuilder bldrQuery = getQuery(sMemberKey);
        return update(entity, bldrQuery);
        }

    // ----- POST API -------------------------------------------------------

    /**
     * Call "shutdown/resetStatistics" operation on NodeMBean.
     *
     * @param sMemberKey  the member key, can be a member name or node Id
     * @param sOperationName  the operation name(shutdown/resetStatistics)
     *
     * @return the response object
     */
    @POST
    @Produces(MEDIA_TYPES)
    @Path("{" + OPERATION_NAME + ":shutdown|resetStatistics}")
    public Response executeOperation(@PathParam(MEMBER_KEY)     String sMemberKey,
                                     @PathParam(OPERATION_NAME) String sOperationName)
        {
        return executeMBeanOperation(getQuery(sMemberKey), sOperationName, null, null);
        }

    /**
     * Call "logNodeState" operation on NodeMBean.
     *
     * @param sMemberKey  the member key, can be a member name or node Id
     *
     * @return the response object
     */
    @POST
    @Produces(MEDIA_TYPES)
    @Path("logMemberState")
    public Response logMemberState(@PathParam(MEMBER_KEY) String sMemberKey)
            throws Exception
        {
        return executeMBeanOperation(getQuery(sMemberKey), "logNodeState", null, null);
        }

    /**
     * Call "dumpHeap" operation on NodeMBean.
     *
     * {@link com.oracle.coherence.common.internal.util.HeapDump#dumpHeap()} documents system properties
     * to specify tmp directory to generate the heapdump-<i>uniqueid</i>.hprof file.
     *
     * @param sMemberKey  the member key, can be a member name or node Id
     *
     * @return the response object
     */
    @POST
    @Produces(MEDIA_TYPES)
    @Path(MEMBER_DUMP_HEAP)
    public Response dumpHeap(@PathParam(MEMBER_KEY) String sMemberKey)
        throws Exception
        {
        String[] asSignature = {String.class.getName()};
        Object[] aoArguments = {null};

        // dynamically named hprof file, "heapdump-*.hprof" saved in a specified tmp directory
        return executeMBeanOperation(getQuery(sMemberKey), "dumpHeap", aoArguments, asSignature);
        }

    /**
     * Call "trackWeakest" operation on PointToPointMBean.
     *
     * @param sMemberKey  the member key, can be a member name or node Id
     *
     * @return the response object
     */
    @POST
    @Produces(MEDIA_TYPES)
    @Path(NETWORK_STATS + "/trackWeakest")
    public Response trackWeakestMember(@PathParam(MEMBER_KEY) String sMemberKey)
        {
        return executeMBeanOperation(getPointToPointMBeanQuery(sMemberKey),
                "trackWeakest", null, null);
        }

    /**
     * Call "diagnostic-cmd/jfrCmd" on {@link com.sun.management.DiagnosticCommandMBean}.
     *
     * Valid commands are jfrStart, jfrStop, jfrDump, and jfrCheck. See jcmd JFR
     * command for more information.
     *
     * @param sMemberKey  the member key, can be a member name or node Id
     * @param sCmd        the JFR command
     * @param sOptions    the comma separated JFR options
     *
     * @return the response object, includes a message returned by the JFR command.
     */
    @POST
    @Produces(MEDIA_TYPES)
    @Path(DIAGNOSTIC_CMD + "/{" + JFR_CMD + "}")
    public Response diagnosticCmd(@PathParam(MEMBER_KEY) String sMemberKey,
                                  @PathParam(JFR_CMD)    String sCmd,
                                  @QueryParam(OPTIONS)   String sOptions)
            throws Exception
        {
        String       sBaseQuery  = ":type=DiagnosticCommand,Domain=com.sun.management,subType=DiagnosticCommand";
        QueryBuilder bldrQuery   = createQueryBuilder().withBaseQuery(sBaseQuery).withMember(sMemberKey);
        Object[]     aoArguments = sOptions == null ? new Object[0] : new Object[]{sOptions.split(",")};
        String[]     signature   = sOptions == null ? new String[0] : new String[]{String[].class.getName()};

        // execute the JFR operation and return the result message from the operation
        return response(getResponseFromMBeanOperation(bldrQuery,
                "status", sCmd, aoArguments, signature));
        }

    // -------------------------- AbstractManagementResource methods------------------------------------------

    @Override
    protected EntityMBeanResponse getQueryResult(Map mapQuery, Map<String, String> mapArguments, URI uriParent)
        {
        String              sMemberKey = mapArguments.get(MEMBER_KEY);
        Object              oChildren  = getChildrenQuery(mapQuery);
        URI                 uriSelf    = getSubUri(uriParent, sMemberKey);
        EntityMBeanResponse response   = getLinksOnlyResponseBody(uriParent, uriSelf, getLinksFilter(mapQuery),
                CHILD_LINKS);

        Map<String, Object> mapResponse = response.getEntity();

        if (oChildren != null && oChildren instanceof Map)
            {
            Map mapChildrenQuery = (Map) oChildren;
            addChildMbeanQueryResult(NETWORK_STATS, getPointToPointMBeanQuery(sMemberKey), mapResponse, mapChildrenQuery);
            addPlatformMBeansQueryResult(sMemberKey, mapResponse, mapChildrenQuery);
            }

        return response;
        }

    // ----- ClusterMemberResource methods-----------------------------------

    /**
     * Return the NodeMBean query for the provided member.
     *
     * @param sMemberKey  the member key
     *
     * @return the MBean query
     */
    protected QueryBuilder getQuery(String sMemberKey)
        {
        return createQueryBuilder()
                .withBaseQuery(CLUSTER_MEMBERS_QUERY)
                .withMember(sMemberKey);
        }

    /**
     * Return the PointToPoint MBean query for the member.
     *
     * @param sMemberKey  the member key
     *
     * @return the MBean query
     */
    protected QueryBuilder getPointToPointMBeanQuery(String sMemberKey)
        {
        return createQueryBuilder()
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
     * @param mapQuery
     *
     * @return the query for a platform MBean.
     */
    protected Object getPlatformChildrenQueryMap(Map mapQuery)
        {
        Object oPlatformQuery = mapQuery.get(PLATFORM);

        // the platform MBeans are queried under children->platform

        Object oChildrenQueryObject = null;
        if (oPlatformQuery != null && oPlatformQuery instanceof Map)
            {
            oChildrenQueryObject = ((Map) oPlatformQuery).get(CHILDREN);
            }

        return oChildrenQueryObject;
        }

    /**
     * Add platform MBeans to query response.
     *
     * @param sMemberId    the member ID
     * @param mapResponse  the response map
     * @param mapQuery     the query map
     */
    protected void addPlatformMBeansQueryResult(String sMemberId, Map<String, Object> mapResponse, Map mapQuery)
        {
        Object oChildrenQueryObject = getPlatformChildrenQueryMap(mapQuery);

        if (oChildrenQueryObject != null && oChildrenQueryObject instanceof Map)
            {
            Map<String, Object> mapPlatform                 = new LinkedHashMap<>();
            Map                 mapPlatformChildrenQueryMap = (Map) oChildrenQueryObject;

            // add the child JVM Mbean results
            for (Map.Entry<String, String> entry : MAP_PLATFORM_URL_TO_MBEAN_QUERY.entrySet())
                {
                String sPlatformMBeanKey = entry.getKey();

                QueryBuilder bldrQuery = createQueryBuilder()
                        .withBaseQuery(entry.getValue())
                        .withMember(sMemberId);

                addChildMbeanQueryResult(sPlatformMBeanKey, bldrQuery,
                        mapPlatform, mapPlatformChildrenQueryMap);
                }

            // add the child JVM GC Mbean results
            for (Map.Entry<String, String> entry : MAP_PLATFORM_G1_URL_TO_MBEAN_QUERY.entrySet())
                {
                String sPlatformMBeanKey = entry.getKey();

                QueryBuilder bldrQuery = createQueryBuilder()
                        .withBaseQuery(entry.getValue())
                        .withMember(sMemberId);

                addChildMbeanQueryResult(sPlatformMBeanKey, bldrQuery,
                        mapPlatform, mapPlatformChildrenQueryMap);
                }

            // add the child JVM Mbean results
            for (Map.Entry<String, String> entry : MAP_PLATFORM_PS_URL_TO_MBEAN_QUERY.entrySet())
                {
                String sPlatformMBeanKey = entry.getKey();

                QueryBuilder bldrQuery = createQueryBuilder()
                        .withBaseQuery(entry.getValue())
                        .withMember(sMemberId);

                addChildMbeanQueryResult(sPlatformMBeanKey, bldrQuery,
                        mapPlatform, mapPlatformChildrenQueryMap);
                }

            mapResponse.put(PLATFORM, mapPlatform);
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
    public static String[] CHILD_LINKS = null;

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
        CHILD_LINKS = listChildLinks.toArray(new String[]{});
        }
    }
