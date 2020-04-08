/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.management.internal.resources;

import com.tangosol.coherence.management.internal.EntityMBeanResponse;

import com.tangosol.net.management.MBeanAccessor.QueryBuilder;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import javax.ws.rs.core.Response;

import java.net.URI;

/**
 * Handles management API requests for a journal type in the cluster.
 *
 * @author sr  2017.08.29
 * @since 12.2.1.4.0
 */
public class JournalResource
        extends AbstractManagementResource
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a JournalMemberResource.
     *
     * @param resource  the {@link AbstractManagementResource} to be used to initialize the context
     */
    public JournalResource(AbstractManagementResource resource)
        {
        super(resource);
        }

    // ----- GET API --------------------------------------------------------

    /**
     * Return the aggregated metrics of JournalMBean's of a particular type(flash/ram).
     *
     * @param sRoleName   the role of the cluster member
     * @param sCollector  the collector to be used
     *
     * @return the response object
     */
    @GET
    @Produces(MEDIA_TYPES)
    public Response get(@PathParam(JOURNAL_TYPE) String sJournalType,
                        @QueryParam(ROLE_NAME)   String sRoleName,
                        @QueryParam(COLLECTOR)   String sCollector)
        {
        EntityMBeanResponse response    = getLinksOnlyResponseBody(getParentUri(), getCurrentUri(), MEMBERS);
        Map<String, Object> responseMap = response.toJson();

        addAggregatedMetricsToResponseMap(sRoleName, sCollector, getQuery(sJournalType), responseMap);
        return response(responseMap);
        }

    // ----- POST API(Operations) -------------------------------------------

    /**
     * Call compact on all the Journal MBeans of the speccified type.
     *
     * @param sJournalType  the journal type(ram/flash)
     * @param entity        the input entity
     *
     * @return  the response of the operation
     *
     * @throws Exception thrown in case of any errors
     */
    @POST
    @Produces(MEDIA_TYPES)
    @Consumes(MEDIA_TYPES)
    @Path("compact")
    public Response compact(@PathParam(JOURNAL_TYPE) String sJournalType, Map<String, Object> entity)
        {
        QueryBuilder bldrQuery = getQuery(sJournalType);

        Object  oRegular = entity == null ? null : entity.get("regular");
        boolean fRegular = oRegular == null ? false : Boolean.parseBoolean(oRegular.toString());

        return executeMBeanOperation(bldrQuery, "compact",
                new Object[] {fRegular}, new String[] {boolean.class.getName()});
        }

    // ----- Child Resources ------------------------------------------------

    /**
     * Sub resource for journal members.
     *
     * @return the journal members child resource
     */
    @Path(MEMBERS)
    public Object getMembersResource()
            throws Exception
        {
        return new JournalMembersResource(this);
        }

    // ----- AbstractManagementResource methods -------------------------------------------

    @Override
    protected EntityMBeanResponse getQueryResult(Map mapQuery, Map<String, String> mapArguments, URI uriParent)
        {
        Object oChildren = getChildrenQuery(mapQuery);
        if (oChildren != null && oChildren instanceof Map)
            {
            Map<String, Object> mapResponse      = new LinkedHashMap<>();
            Map                 mapChildrenQuery = (Map) oChildren;

            addChildResourceQueryResult(new JournalMembersResource(this),
                                        MEMBERS,
                                        mapResponse,
                                        mapChildrenQuery,
                                        mapArguments,
                                        getParentUri());

            EntityMBeanResponse responseEntity = new EntityMBeanResponse();
            responseEntity.setEntity(mapResponse);
            return responseEntity;

            }
        return null;
        }

    // ---- JournalMembersResource methods --------------------------------------

    /**
     * MBean query to retrieve JournalMBean for the provided journal type.
     *
     * @param sJournalType  the journal type(ram/flash)
     *
     * @return the MBean query
     */
    protected QueryBuilder getQuery(String sJournalType)
        {
        return createQueryBuilder().withBaseQuery(MAP_JOURNAL_URL_TO_MBEAN_QUERY.get(sJournalType));
        }
    }
