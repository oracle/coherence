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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import javax.ws.rs.core.Response;

/**
 * Handles management API requests for Coherence cluster journal members.
 *
 * @author sr 2017.08.21
 * @since 12.2.1.4.0
 */
public class JournalMembersResource
        extends AbstractManagementResource
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a JournalMemberResource.
     *
     * @param resource  the {@link AbstractManagementResource} to be used to initialize the context
     */
    public JournalMembersResource(AbstractManagementResource resource)
        {
        super(resource);
        }

    // ----- GET API --------------------------------------------------------

    /**
     * Return list of JournalMBean of the provided type in the cluster.
     *
     * @param sJournalType  the journal type(ram/flash)
     *
     * @return the response object
     */
    @GET
    @Produces(MEDIA_TYPES)
    public Response get(@PathParam(JOURNAL_TYPE) String sJournalType)
        {
        return response(getResponseBodyForMBeanCollection(getQuery(sJournalType), new JournalMemberResource(this),
                null, null, getParentUri(), getCurrentUri()));
        }

    // ----- Child Resources ------------------------------------------------

    /**
     * Sub resource for a single journal member.
     *
     * @return the journal member child resource
     */
    @Path("{" + MEMBER_KEY + "}")
    public Object getMemberResource()
        {
        return new JournalMemberResource(this);
        }

    // ----- AbstractManagementResource methods -------------------------------------------

    @Override
    protected EntityMBeanResponse getQueryResult(Map mapQuery, Map<String, String> mapArguments, URI uriParent)
        {
        QueryBuilder bldrQuery = getQuery(mapArguments.get(JOURNAL_TYPE));
        return getResponseBodyForMBeanCollection(bldrQuery, new JournalMemberResource(this),
                mapQuery, mapArguments, uriParent, getSubUri(uriParent, MEMBERS));
        }

    // ---- JournalMembersResource methods --------------------------------------

    /**
     * MBean query to retrieve JournalMBeans for the provided journal type.
     *
     * @param sJournalType  the journal type(ram/flash)
     *
     * @return the MBean query builder
     */
    protected QueryBuilder getQuery(String sJournalType)
        {
        return createQueryBuilder().withBaseQuery(MAP_JOURNAL_URL_TO_MBEAN_QUERY.get(sJournalType));
        }
    }
