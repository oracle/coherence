/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.management.internal.resources;

import com.tangosol.net.management.MBeanAccessor.QueryBuilder;

import com.tangosol.coherence.management.internal.EntityMBeanResponse;

import java.net.URI;

import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import javax.ws.rs.core.Response;

/**
 * Handles management API requests for members of a service.
 *
 * @author sr 2017.08.29
 * @since 12.2.1.4.0
 */
public class ServiceMembersResource extends AbstractManagementResource
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a ServiceMembersResource.
     *
     * @param resource  the {@link AbstractManagementResource} to be used to initialize the context
     */
    public ServiceMembersResource(AbstractManagementResource resource)
        {
        super(resource);
        }

    // ----- GET API --------------------------------------------------------

    /**
     * Return list of ServiceMBean objects for the provided service.
     *
     * @param sServiceName  the service name
     *
     * @return the response object
     */
    @GET
    @Produces(MEDIA_TYPES)
    public Response get(@PathParam(SERVICE_NAME) String sServiceName)
        {
        return response(getResponseBodyForMBeanCollection(getQuery(sServiceName), new ServiceMemberResource(this),
                null, null, getParentUri(), getCurrentUri()));
        }

    // ----- Child Resources ------------------------------------------------

    /**
     * Sub resource for service member.
     *
     * @return the service member child resource
     */
    @Path("{" + MEMBER_KEY + "}")
    public Object getMemberResource(@PathParam(MEMBER_KEY) String sMemberKey)
        {
        return new ServiceMemberResource(this);
        }

    // ----- AbstractManagementResource methods -------------------------------------------

    @Override
    protected EntityMBeanResponse getQueryResult(Map mapQuery, Map<String, String> mapArguments, URI uriParent)
        {
        QueryBuilder bldrQuery = getQuery(mapArguments.get(SERVICE_NAME));

        return getResponseBodyForMBeanCollection(bldrQuery, new ServiceMemberResource(this),
                mapQuery, mapArguments, uriParent, getSubUri(uriParent, MEMBERS));
        }


    // ----- ServiceMemberResource methods ----------------------------------

    /**
     * MBean query to retrieve ServiceMBeans for the provided service.
     *
     * @param sServiceName  the service name
     *
     * @return the MBean query builder
     */
    protected QueryBuilder getQuery(String sServiceName)
        {
        return createQueryBuilder().withBaseQuery(SERVICE_MEMBERS_QUERY + sServiceName);
        }
    }
