/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.management;

import com.oracle.coherence.common.base.Exceptions;

import com.sun.net.httpserver.HttpHandler;

import com.tangosol.internal.http.BaseHttpHandler;
import com.tangosol.internal.http.HttpRequest;
import com.tangosol.net.management.MapJsonBodyHandler;


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
        }
    }
