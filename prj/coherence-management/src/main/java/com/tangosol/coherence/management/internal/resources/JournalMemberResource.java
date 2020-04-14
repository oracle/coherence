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

import javax.ws.rs.core.Response;

/**
 * Handles management API requests for Coherence cluster member.
 *
 * @author sr  2017.08.21
 * @since 12.2.1.4.0
 */
public class JournalMemberResource
        extends AbstractManagementResource
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a JournalMemberResource.
     *
     * @param resource  the {@link AbstractManagementResource} to be used to initialize the context
     */
    public JournalMemberResource(AbstractManagementResource resource)
        {
        super(resource);
        }

    // ----- GET API --------------------------------------------------------

    /**
     * Return JournalMBean attributes for a particular journal type(flash/ram) running on a cluster member.
     *
     * @param sMemberKey    the member key, can be a member name or node Id
     * @param sJournalType  the journal type(ram/flash)
     *
     * @return the response object
     */
    @GET
    @Produces(MEDIA_TYPES)
    public Response get(@PathParam(MEMBER_KEY) String sMemberKey, @PathParam(JOURNAL_TYPE) String sJournalType)
        {
        return response(getResponseEntityForMbean(getQuery(sMemberKey, sJournalType)));
        }

    // ----- POST API(Operations) -------------------------------------------

    /**
     * Call "compact" operation on a JournalMBean.
     *
     * @param sMemberKey    the member key, can be a member name or node Id
     * @param sJournalType  the journal type(ram/flash)
     * @param entity        the input entity, contains input parameters for the operation
     *
     * @return the response object
     */
    @POST
    @Produces(MEDIA_TYPES)
    @Consumes(MEDIA_TYPES)
    @Path("compact")
    public Response compact(@PathParam(MEMBER_KEY)   String sMemberKey,
                            @PathParam(JOURNAL_TYPE) String sJournalType,
                            Map<String, Object> entity)
        {
        Object oRegular  = entity == null ? null : entity.get("regular");
        boolean fRegular = oRegular == null ? false : Boolean.parseBoolean(oRegular.toString());

        return executeMBeanOperation(getQuery(sMemberKey, sJournalType), "compact",
                new Object[]{fRegular}, new String[] {boolean.class.getName()});
        }

    /**
     * Call "resetStatistics" operation on a JournalMBean.
     *
     * @param sMemberKey    the member key, can be a member name or node Id
     * @param sJournalType  the journal type(ram/flash)
     *
     * @return the response object
     */
    @POST
    @Produces(MEDIA_TYPES)
    @Consumes(MEDIA_TYPES)
    @Path(RESET_STATS)
    public Response resetStatistics(@PathParam(MEMBER_KEY)   String sMemberKey,
                                    @PathParam(JOURNAL_TYPE) String sJournalType)
        {
        return executeMBeanOperation(getQuery(sMemberKey, sJournalType), RESET_STATS, null, null);
        }


    // ----- AbstractManagementResource methods------------------------------

    @Override
    protected EntityMBeanResponse getQueryResult(Map mapQuery, Map<String, String> mapArguments, URI uriParent)
        {
        return getLinksOnlyResponseBody(uriParent, getSubUri(uriParent, mapArguments.get(MEMBER_KEY)));
        }

    // ----- JournalMemberResource methods-----------------------------------

    /**
     * MBean query to retrieve JournalMBean for the provided journal type running in a Cluster member.
     *
     * @param sMemberKey    the member key, can be a member name or node Id
     * @param sJournalType  the journal type(ram/flash)
     *
     * @return the MBean query builder
     */
    protected QueryBuilder getQuery(String sMemberKey, String sJournalType)
        {
        return createQueryBuilder().withBaseQuery(MAP_JOURNAL_URL_TO_MBEAN_QUERY.get(sJournalType))
                .withMember(sMemberKey);
        }
    }
