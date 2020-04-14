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
 * API resource for C*W MBean members for an application.
 *
 * @author sr  2017.09.11
 * @since 12.2.1.4.0
 */
public class CWebMembersResource
        extends AbstractManagementResource
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a CWebMembersResource.
     *
     * @param resource  the {@link AbstractManagementResource} to be used to initialize the context
     */
    public CWebMembersResource(AbstractManagementResource resource)
        {
        super(resource);
        }

    // ----- GET API --------------------------------------------------------

    /**
     * Return list of HttpSessionManager MBeans for a given application.
     *
     * @return the response object
     */
    @GET
    @Produces(MEDIA_TYPES)
    public Response get(@PathParam(APPLICATION_ID) String sApplicationId)
        {
        return response(getResponseBodyForMBeanCollection(getQuery(sApplicationId),
                new CWebMemberResource(this), null, null, getParentUri(), getCurrentUri()));
        }

    // ----- Child Resources ------------------------------------------------

    /**
     * Sub resource for a single HttpSessionManager MBean.
     *
     * @return the HttpSessionManager member child resource
     */
    @Path("{" + MEMBER_KEY + "}")
    public Object getMemberResource()
        {
        return new CWebMemberResource(this);
        }

    // ----- AbstractManagementResource methods -------------------------------------------

    @Override
    protected EntityMBeanResponse getQueryResult(Map mapQuery, Map<String, String> mapArguments, URI uriParent)
        {
        QueryBuilder bldrQuery = getQuery(mapArguments.get("appId"));

        return getResponseBodyForMBeanCollection(bldrQuery, new CWebMemberResource(this),
                mapQuery, mapArguments, uriParent, getSubUri(uriParent, MEMBERS));
        }

    // ---- CWebMembersResource methods --------------------------------------

    /**
     * Create a query builder with the provided application id.
     *
     * @param sApplicationId  the application ID
     *
     * @return hte MBean query builder
     */
    protected QueryBuilder getQuery(String sApplicationId)
        {
        return createQueryBuilder().withBaseQuery(CWEB_APPLICATION_QUERY + sApplicationId);
        }
    }
