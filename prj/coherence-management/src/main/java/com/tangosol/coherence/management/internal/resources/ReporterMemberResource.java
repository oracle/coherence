/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.management.internal.resources;

import com.tangosol.coherence.reporter.ReporterSecurity;

import com.tangosol.coherence.management.internal.EntityMBeanResponse;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.management.MBeanAccessor.QueryBuilder;
import com.tangosol.net.management.MBeanHelper;
import com.tangosol.net.management.Registry;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import java.net.URI;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Handles management API requests for a single Coherence reporter member.
 *
 * @author tam 2018.03.14
 * @since 12.2.1.4.0
 */
public class ReporterMemberResource
     extends AbstractManagementResource
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a ClusterMemberResource.
     *
     * @param resource  the {@link AbstractManagementResource} to be used to initialize the context
     */
    public ReporterMemberResource(AbstractManagementResource resource)
        {
        super(resource);
        }

    // ----- GET API --------------------------------------------------------

    /**
     * Return ReporterMBean attributes for a cluster member.
     *
     * @param sMemberKey  the member key, can be a member name or node Id
     *
     * @return the response object
     */
    @GET
    @Produces(MEDIA_TYPES)
    public Response get(@PathParam(MEMBER_KEY) String sMemberKey)
        {
        return response(getResponseEntityForMbean(getQuery(sMemberKey)));
        }

    // ----- POST API(Update) -----------------------------------------------

    /**
     * Update a ReporterMBean with the parameters present in the input entity map.
     *
     * @param sMemberKey  the member key, can be a member name or node Id
     * @param entity      the input entity map containing the updated attributes
     *
     * @return the response object
     */
    @POST
    @Produces(MEDIA_TYPES)
    @Consumes(MEDIA_TYPES)
    public Response updateAttributes(@PathParam(MEMBER_KEY) String sMemberKey,
                                     Map<String, Object> entity)
        {
        try
            {
            ReporterSecurity.validateReporterUpdate(entity, "member");
            }
        catch (IllegalArgumentException e)
            {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Response.Status.BAD_REQUEST.getReasonPhrase() + '\n' + e.getMessage())
                    .build();
            }
        return update(entity, getQuery(sMemberKey));
        }

    // ----- POST API(Execute) -------------------------------------------------------
    
    /**
     * Call start, stop or resetStatistics operation on ReporterMBean.
     *
     * @return the response object
     */
    @POST
    @Produces(MEDIA_TYPES)
    @Path("{operationName:start|stop|resetStatistics}")
    public Response shutdownCluster(@PathParam(MEMBER_KEY)     String sMemberKey,
                                    @PathParam(OPERATION_NAME) String sOperationName)
        {
        return executeMBeanOperation(getQuery(sMemberKey), sOperationName, null, null);
        }

    /**
     * Run a specified report using runTabularReport.
     *
     * @param sReportName  the report name
     *
     * @return the response object
     */
    @GET
    @Produces(MEDIA_TYPES)
    @Path(RUN_REPORT + "/{" + REPORT_NAME + "}")
    @SuppressWarnings("unchecked")
    public Response runReport(@PathParam(REPORT_NAME) String sReportName)
        {
        MBeanServerConnection mbs = MBeanHelper.findMBeanServer();
        String                sFullReportName;

        try
            {
            ReporterSecurity.validateRestReportName(sReportName);
            sFullReportName = "reports/" + sReportName + ".xml";
            }
        catch (IllegalArgumentException e)
            {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Response.Status.BAD_REQUEST.getReasonPhrase() + '\n' + e.getMessage())
                    .build();
            }

        List<Map<String, Object>> results = new ArrayList<>();

        try
            {
            int             nMemberId = CacheFactory.ensureCluster().getLocalMember().getId();
            Set<ObjectName> setNames  = mbs.queryNames(new ObjectName("Coherence:" + Registry.REPORTER_TYPE
                    + "," + Registry.KEY_NODE_ID + nMemberId + ",*"), null);

            if (setNames.isEmpty())
                {
                return Response.status(Response.Status.NOT_FOUND).build();
                }

            TabularData reportData = (TabularData) mbs.invoke(setNames.iterator().next(),
                    "runTabularReport", new Object[] {sFullReportName}, new String[] {"java.lang.String"});

            Collection<CompositeData> values = (Collection<CompositeData>) reportData.values();
            for (CompositeData compositeData : values)
                {
                Set<String>         keys      = compositeData.getCompositeType().keySet();
                Map<String, Object> mapValues = new HashMap<>();

                keys.forEach(k -> mapValues.put(k, compositeData.get(k)));
                results.add(mapValues);
                }
            }
        catch (Exception e)
            {
            CacheFactory.log("Reporter runReport failed: " + e);
            return Response.status(Response.Status.NOT_FOUND).build();
            }

        EntityMBeanResponse responseEntity = new EntityMBeanResponse();
        responseEntity.setEntities(results);
        Map<String, Object> mapResponse = responseEntity.toJson();

        return response(mapResponse);
        }

    // ----- ReporterMemberResource methods----------------------------------

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
                .withBaseQuery(REPORTER_MEMBERS_QUERY)
                .withMember(sMemberKey);
        }

    // ----- AbstractManagementResource methods -------------------------------------------

    @Override
    protected EntityMBeanResponse getQueryResult(Map mapQuery, Map<String, String> mapArguments, URI uriParent)
        {
        String              sMemberKey = mapArguments.get(MEMBER_KEY);
        URI                 uriSelf    = getSubUri(uriParent, sMemberKey);

        return getLinksOnlyResponseBody(uriParent, uriSelf, getLinksFilter(mapQuery));
        }
    }
