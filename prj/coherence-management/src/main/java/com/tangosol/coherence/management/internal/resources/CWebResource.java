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
import javax.ws.rs.Produces;

import javax.ws.rs.core.Response;

/**
 * API resource for C*W related MBeans.
 *
 * @author sr  2017.09.11
 * @since 12.2.1.4.0
 */
public class CWebResource
        extends AbstractManagementResource
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a CWebResource.
     *
     * @param resource  the {@link AbstractManagementResource} to be used to initialize the context
     */
    public CWebResource(AbstractManagementResource resource)
        {
        super(resource);
        }

    // ----- GET API --------------------------------------------------------

    /**
     * Return the list of C*W applications in the cluster.
     *
     * @return the response object
     */
    @GET
    @Produces(MEDIA_TYPES)
    public Response get()
        {
        return response(getResponseBodyForMBeanCollection(getQuery(), new CWebApplicationResource(this),
                "appId", null, getParentUri(), getCurrentUri(), null));
        }

    /**
     * Returns the list of all CWebApplication members in the cluster.
     *
     * @return the response object.
     */
    @GET
    @Produces(MEDIA_TYPES)
    @Path(MEMBERS)
    public Response getAllCWebMembers()
        {
        return response(getResponseBodyForMBeanCollection(getQuery(), null,
                getParentUri(), getCurrentUri()));
        }

    // ----- Child Resources ------------------------------------------------

    /**
     * Sub resource for C*W application members.
     *
     * @return the services child resource
     */
    @Path("{" + APPLICATION_ID + "}")
    public Object getApplicationResource()
        {
        return new CWebApplicationResource(this);
        }

    // ----- AbstractManagementResource methods -------------------------------------------

    @Override
    protected EntityMBeanResponse getQueryResult(Map mapQuery, Map<String, String> mapArguments, URI uriParent)
        {
        return getResponseBodyForMBeanCollection(getQuery(), new CWebApplicationResource(this), "appId",
                mapQuery, uriParent, getSubUri(uriParent, WEB_APPS), mapArguments);
        }

    // ---- CWebResource methods --------------------------------------

    /**
     * The MBean Query Builder for CWeb applications.
     *
     * @return the MBean query builder
     */
    protected QueryBuilder getQuery()
        {
        return createQueryBuilder().withBaseQuery(CWEB_APPLICATIONS_QUERY);
        }
    }
