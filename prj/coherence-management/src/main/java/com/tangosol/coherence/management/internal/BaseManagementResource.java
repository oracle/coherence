/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.management.internal;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.internal.http.HttpException;
import com.tangosol.internal.http.HttpRequest;

import com.tangosol.internal.management.ManagementRoutes;

import com.tangosol.internal.management.resources.AbstractManagementResource;

import com.tangosol.net.management.MBeanServerProxy;

import com.tangosol.util.Filter;
import com.tangosol.util.ResourceRegistry;

import org.glassfish.jersey.server.ContainerRequest;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;

import javax.ws.rs.container.ContainerRequestContext;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import java.net.URI;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * A base JAX-RS root resource that supports management info requests.
 *
 * @author Jonathan Knight  2022.01.25
 * @since 22.06
 */
public class BaseManagementResource
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a root management resource.
     */
    public BaseManagementResource(ManagementRoutes routes)
        {
        f_routes = routes;
        }

    // ----- jax-rs handlers ------------------------------------------------

    /**
     * Handle a management over rest GET request.
     *
     * @return the response from handling the request
     */
    @GET
    public Response get(@Context ContainerRequest request)
        {
        return doGet(request);
        }

    /**
     * Handle a management over rest GET request.
     *
     * @return the response from handling the request
     */
    @GET
    @Path("{any:.*}")
    public Response doGet(@Context ContainerRequest request)
        {
        return handle(request, f_routes);
        }

    /**
     * Handle a management over rest POST request.
     *
     * @return the response from handling the request
     */
    @POST
    @Path("{any:.*}")
    public Response doPost(@Context ContainerRequest request)
        {
        return handle(request, f_routes);
        }

    /**
     * Handle a management over rest PUT request.
     *
     * @return the response from handling the request
     */
    @PUT
    @Path("{any:.*}")
    public Response doPut(@Context ContainerRequest request)
        {
        return handle(request, f_routes);
        }

    /**
     * Handle a management over rest DELETE request.
     *
     * @return the response from handling the request
     */
    @DELETE
    @Path("{any:.*}")
    public Response doDelete(@Context ContainerRequest request)
        {
        return handle(request, f_routes);
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Forward the specified request to the router and convert the router response to a jax-rs response.
     *
     * @param containerRequest  the current request
     * @param router            the router to handle the request
     *
     * @return the jax-rs response to return to the caller
     */
    protected Response handle(ContainerRequestContext containerRequest, ManagementRoutes router)
        {
        try
            {
            JaxRsRequest request      = createJaxRsRequest(containerRequest);
            ResourceRegistry registry     = request.getResourceRegistry();
            String           sClusterName = getClusterName();

            if (sClusterName != null)
                {
                registry.registerResource(String.class, AbstractManagementResource.CLUSTER_NAME, sClusterName);
                }

            Filter<String> filterDomainPartition = createDomainPartitionFilter(request);
            if (filterDomainPartition != null)
                {
                registry.registerResource(Filter.class, AbstractManagementResource.DOMAIN_FILTER, filterDomainPartition);
                }

            return BaseManagementResource.toJaxRsResponse(router.route(request));
            }
        catch (HttpException e)
            {
            Response.ResponseBuilder builder = Response.status(e.getStatus());
            String sMsg = e.getMessage();
            if (sMsg != null)
                {
                builder.entity(sMsg);
                }
            return builder.build();
            }
        catch (Throwable t)
            {
            Logger.err(t);
            return Response.serverError().build();
            }
        }

    protected JaxRsRequest createJaxRsRequest(ContainerRequestContext containerRequest)
        {
        JaxRsRequest request = new JaxRsRequest(containerRequest);

        request.getResourceRegistry().registerResource(MBeanServerProxy.class, m_mBeanServerProxy);

        String sClusterName = getClusterName();
        if (sClusterName != null)
            {
            request.getResourceRegistry().registerResource(String.class, AbstractManagementResource.CLUSTER_NAME, sClusterName);
            }

        return request;
        }

    private Filter<String> createDomainPartitionFilter(HttpRequest request)
        {
        String sDomainPartitionName = request.getQueryParameters().getFirst(AbstractManagementResource.DOMAIN_PARTITION);
        return (sDomainPartitionName == null)
               ? null : s -> s.equals(sDomainPartitionName);
        }

    /**
     * The URI of the current resource.
     *
     * @return the resource URI
     */
    protected URI getCurrentUri()
        {
        return getSubUri(m_uriInfo);
        }

    /**
     * Append the provided segments to the current URI.
     *
     * @param uriInfo     the URI info object
     * @param asSegments  the segments to be appended
     *
     * @return the resulting URI
     */
    public static URI getSubUri(UriInfo uriInfo, String... asSegments)
        {
        UriBuilder builder = uriInfo.getAbsolutePathBuilder();
        Arrays.stream(asSegments).forEach(builder::segment);
        return builder.build();
        }

    /**
     * The parent URI of the resource.
     *
     * @return the parent URI
     */
    public URI getParentUri()
        {
        return getParentUri(m_uriInfo);
        }

    public URI getParentUri(UriInfo uriInfo)
        {
        int               count        = getParentUriSegmentsCount(uriInfo);
        List<PathSegment> pathSegments = uriInfo.getPathSegments();
        UriBuilder bldr         = uriInfo.getBaseUriBuilder();

        for (int i = 0; i < count; i++)
            {
            bldr.path(pathSegments.get(i).getPath());
            }

        return bldr.build();
        }

    /**
     * Return the number of URI segments in the parent URL.
     *
     * @param uriInfo  the URI Info
     *
     * @return the number of segments in the parent URL
     */
    public int getParentUriSegmentsCount(UriInfo uriInfo)
        {
        List<PathSegment> pathSegments = uriInfo.getPathSegments();
        int               count        = pathSegments.size() - 1; // go up a level to get to the parent
        if (pathSegments.get(count).getPath().isEmpty())
            {
            count--; // go up for one level because of trailing slash
            }

        return count;
        }

    public String getClusterName()
        {
        return m_sClusterName;
        }

    public void setClusterName(String sClusterName)
        {
        m_sClusterName = sClusterName;
        }

    /**
     * Convert a Coherence {@link com.tangosol.internal.http.Response} to a jax-rs {@link Response}.
     *
     * @param response  the {@link com.tangosol.internal.http.Response} to convert
     *
     * @return a jax-rs {@link Response}
     */
    public static Response toJaxRsResponse(com.tangosol.internal.http.Response response)
        {
        Response.ResponseBuilder builder = Response.status(response.getStatus().getStatusCode());
        Object                   oEntity = response.getEntity();

        if (oEntity != null)
            {
            builder.entity(oEntity);
            }

        for (Map.Entry<String, List<String>> entry : response.getHeaders().entrySet())
            {
            String sName = entry.getKey();
            for (String sValue : entry.getValue())
                {
                builder.header(sName, sValue);
                }
            }

        return builder.build();
        }

    // ----- constants ------------------------------------------------------

    /**
     * Constant used for json application type.
     */
    public static final String MEDIA_TYPE_JSON = MediaType.APPLICATION_JSON;

    /**
     * Constant used for applicable media types in the management REST interface.
     */
    public static final String MEDIA_TYPES = MEDIA_TYPE_JSON;

    // ----- data members ---------------------------------------------------

    /**
     * The router to use to route requests to endpoints.
     */
    private final ManagementRoutes f_routes;

    /**
     * The Mbean server proxy.
     */
    @Context
    protected MBeanServerProxy m_mBeanServerProxy;

    /**
     * The request headers, available in the context.
     */
    @Context
    protected HttpHeaders m_requestHeaders;

    /**
     * The UriInfo available in the context.
     */
    @Context
    protected UriInfo m_uriInfo;

    /**
     * The container request context.
     */
    @Context
    protected ContainerRequestContext m_requestContext;

    /**
     * The current cluster name.
     */
    private String m_sClusterName;
    }
