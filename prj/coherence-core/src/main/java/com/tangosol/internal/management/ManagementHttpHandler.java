/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.management;

import com.oracle.coherence.common.base.Exceptions;

import com.sun.net.httpserver.HttpHandler;

import com.tangosol.internal.http.BaseHttpHandler;
import com.tangosol.internal.http.HttpException;
import com.tangosol.internal.http.HttpRequest;
import com.tangosol.internal.http.Response;

import com.tangosol.internal.net.service.grid.ProxyServiceDependencies;
import com.tangosol.internal.net.service.peer.acceptor.HttpAcceptorDependencies;

import com.tangosol.net.Service;
import com.tangosol.net.ServiceDependencies;
import com.tangosol.net.management.MapJsonBodyHandler;

import javax.security.auth.Subject;


/**
 * A {@link HttpHandler} for serving Coherence management over REST
 * endpoints via the Java {@link com.sun.net.httpserver.HttpServer}.
 *
 * @author Jonathan Knight  2022.01.25
 * @since 22.06
 */
public class ManagementHttpHandler
        extends BaseHttpHandler
    {
    /**
     * Create a {@link ManagementHttpHandler}.
     */
    ManagementHttpHandler()
        {
        super(new ManagementRoutes(), MapJsonBodyHandler.ensureMapJsonBodyHandler());
        }

    /**
     * A static factory method to create a {@link ManagementHttpHandler}.
     * <p>
     * This method is called by Coherence from the {@code management-http-config.xml}
     * configuration file.
     *
     * @return a new instance of a {@link ManagementHttpHandler}
     */
    public static ManagementHttpHandler getInstance()
        {
        try
            {
            return new ManagementHttpHandler();
            }
        catch (Throwable t)
            {
            throw Exceptions.ensureRuntimeException(t);
            }
        }

    /**
     * Pre-process the {@link HttpRequest} before routing it to an endpoint.
     *
     * @param request  the request to be routed
     */
    @Override
    protected void beforeRouting(HttpRequest request)
        {
        String sAuthMethod = getAuthMethod();
        if (!AUTH_NONE.equalsIgnoreCase(sAuthMethod)
                && request.getResourceRegistry().getResource(Subject.class) == null)
            {
            throw new HttpException(Response.Status.UNAUTHORIZED.getStatusCode());
            }
        }

    /**
     * Return the resolved management HTTP auth method.
     *
     * @return the auth method
     */
    private String getAuthMethod()
        {
        Service service = getService();
        if (service == null)
            {
            return AUTH_NONE;
            }

        ServiceDependencies deps = service.getDependencies();
        if (deps instanceof ProxyServiceDependencies)
            {
            ProxyServiceDependencies proxyDeps = (ProxyServiceDependencies) deps;
            if (proxyDeps.getAcceptorDependencies() instanceof HttpAcceptorDependencies)
                {
                return ((HttpAcceptorDependencies) proxyDeps.getAcceptorDependencies()).getAuthMethod();
                }
            }
        return AUTH_NONE;
        }

    // ----- constants ------------------------------------------------------

    /**
     * No authentication.
     */
    private static final String AUTH_NONE = "none";
    }
