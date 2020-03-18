/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.management.internal.resources;

import com.tangosol.coherence.management.internal.EntityMBeanResponse;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Map;

import static com.tangosol.net.management.MBeanAccessor.*;

/**
 * Handles management API requests for Coherence reporter members.
 *
 * @author tam 2018.03.14
 * @since 12.2.1.4.0
 */
public class ReportersResource
        extends AbstractManagementResource
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a ReportersResource.
     *
     * @param resource  the {@link AbstractManagementResource} to be used to initialize the context
     */
    public ReportersResource(AbstractManagementResource resource)
        {
        super(resource);
        }

    // ----- GET API --------------------------------------------------------

    /**
     * Return list of ReporterMBean objects in the cluster.
     *
     * @return the response object
     */
    @GET
    @Produces(MEDIA_TYPES)
    public Response get()
        {
        QueryBuilder bldrQuery = createQueryBuilder()
                .withBaseQuery(REPORTER_MEMBERS_QUERY);

        return response(getResponseBodyForMBeanCollection(bldrQuery, new ReporterMemberResource(this),
                null, null, getParentUri(), getCurrentUri()));
        }

    // ----- Child Resources ------------------------------------------------

    /**
     * Sub resource for a single reporter member.
     *
     * @return the cluster member child resource
     */
    @Path("{" + MEMBER_KEY + "}")
    public Object getMemberResource() throws Exception
        {
        return new ReporterMemberResource(this);
        }

    // ----- AbstractManagementResource methods -------------------------------------------

    @Override
    protected EntityMBeanResponse getQueryResult(Map mapQuery, Map<String, String> mapArguments, URI uriParent)
        {
        QueryBuilder bldrQuery = createQueryBuilder().withBaseQuery(REPORTER_MEMBERS_QUERY);

        return getResponseBodyForMBeanCollection(bldrQuery, new ReporterMemberResource(this),
                mapQuery, mapArguments, uriParent, getSubUri(uriParent, MEMBERS));
        }
    }
