/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.management.internal;

import com.tangosol.internal.http.HttpMethod;
import com.tangosol.internal.http.HttpRequest;
import com.tangosol.internal.http.PathParameters;
import com.tangosol.internal.http.QueryParameters;
import com.tangosol.util.ResourceRegistry;
import com.tangosol.util.SimpleResourceRegistry;

import org.glassfish.jersey.server.ContainerRequest;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import java.io.InputStream;

import java.net.URI;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * An implementation of a {@link HttpRequest} that wraps
 * a Jersey JAX-RS {@link ContainerRequest}.
 *
 * @author Jonathan Knight 2022.01.25
 * @since 22.06
 */
public class JaxRsRequest
        implements HttpRequest
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create a {@link JaxRsRequest}.
     *
     * @param request  the {@link ContainerRequest} to wrap
     */
    public JaxRsRequest(ContainerRequestContext request)
        {
        f_request          = request;
        f_queryParameters  = new JaxRsQueryParameters(request.getUriInfo());
        f_resourceRegistry = new SimpleResourceRegistry();
        m_pathParameters   = new JaxRsPathParameters(request.getUriInfo());
        }

    // ----- HttpRequest methods --------------------------------------------

    @Override
    public HttpMethod getMethod()
        {
        return HttpMethod.valueOf(f_request.getMethod());
        }

    @Override
    public String getHeaderString(String sName)
        {
        return f_request.getHeaderString(sName);
        }

    @Override
    public URI getBaseURI()
        {
        return f_request.getUriInfo().getBaseUri();
        }

    @Override
    public URI getRequestURI()
        {
        return f_request.getUriInfo().getRequestUri();
        }

    @Override
    public QueryParameters getQueryParameters()
        {
        return f_queryParameters;
        }

    @Override
    public PathParameters getPathParameters()
        {
        return m_pathParameters;
        }

    @Override
    public void setPathParameters(PathParameters parameters)
        {
        m_pathParameters = parameters;
        }

    @Override
    public InputStream getBody()
        {
        return f_request.getEntityStream();
        }

    @Override
    public synchronized Map<String, Object> getJsonBody(Function<InputStream, Map<String, Object>> fnParser)
        {
        if (m_mapBody == null)
            {
            if (f_request.hasEntity())
                {
                Map<String, Object> mapBody = fnParser.apply(getBody());
                m_mapBody = mapBody == null ? Collections.emptyMap() : mapBody;
                }
            else
                {
                m_mapBody = new LinkedHashMap<>();
                }
            }
        return m_mapBody;
        }

    @Override
    public ResourceRegistry getResourceRegistry()
        {
        return f_resourceRegistry;
        }

    // ----- helper methods -------------------------------------------------

    protected URI getParentUri(UriInfo uriInfo)
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

    // ----- inner class: JaxRsQueryParameters ------------------------------

    /**
     * A {@link QueryParameters} implementation that wraps a JAX-RS {@link UriInfo}.
     */
    public static class JaxRsQueryParameters
            implements QueryParameters
        {
        // ----- constructors -----------------------------------------------

        /**
         * Create a {@link JaxRsQueryParameters}.
         *
         * @param uriInfo  the {@link UriInfo} to wrap
         */
        public JaxRsQueryParameters(UriInfo uriInfo)
            {
            f_uriInfo = uriInfo;
            }

        // ----- QueryParameters methods ------------------------------------

        @Override
        public String getFirst(String sKey)
            {
            return f_uriInfo.getQueryParameters(true).getFirst(sKey);
            }

        // ----- data members -----------------------------------------------

        /**
         * The wrapped {@link UriInfo}.
         */
        private final UriInfo f_uriInfo;
        }

    // ----- inner class: JaxRsPathParameters -------------------------------

    /**
     * A {@link PathParameters} implementation that wraps a JAX-RS {@link UriInfo}.
     */
    public static class JaxRsPathParameters
            implements PathParameters
        {
        // ----- constructors -----------------------------------------------

        /**
         * Create a {@link JaxRsQueryParameters}.
         *
         * @param uriInfo  the {@link UriInfo} to wrap
         */
        public JaxRsPathParameters(UriInfo uriInfo)
            {
            f_uriInfo = uriInfo;
            }

        // ----- QueryParameters methods ------------------------------------

        @Override
        public String getFirst(String sKey)
            {
            return f_uriInfo.getPathParameters(true).getFirst(sKey);
            }

        // ----- data members -----------------------------------------------

        /**
         * The wrapped {@link UriInfo}.
         */
        private final UriInfo f_uriInfo;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The wrapped {@link ContainerRequest}.
     */
    private final ContainerRequestContext f_request;

    /**
     * The request query parameters.
     */
    private final QueryParameters f_queryParameters;

    /**
     * The request resource registry.
     */
    private final ResourceRegistry f_resourceRegistry;

    /**
     * The request path parameters.
     */
    private PathParameters m_pathParameters;

    /**
     * The JSON body.
     */
    private Map<String, Object> m_mapBody;
    }
