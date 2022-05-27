/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.http;

/**
 * A simple {@link com.sun.net.httpserver.HttpHandler}.
 *
 * @author Jonathan Knight  2022.05.25
 * @since 22.06
 */
public class SimpleHttpHandler
        extends BaseHttpHandler
    {
    /**
     * Create a {@link SimpleHttpHandler}.
     */
    public SimpleHttpHandler()
        {
        }

    /**
     * Create a {@link SimpleHttpHandler}.
     *
     * @param bodyWriter  the {@link com.tangosol.internal.http.BaseHttpHandler.BodyWriter} to use
     */
    public SimpleHttpHandler(BodyWriter<?> bodyWriter)
        {
        super(new RequestRouter(), bodyWriter);
        }

    /**
     * Create a {@link SimpleHttpHandler}.
     *
     * @param router      the {@link RequestRouter} to use
     * @param bodyWriter  the {@link com.tangosol.internal.http.BaseHttpHandler.BodyWriter} to use
     */
    public SimpleHttpHandler(RequestRouter router, BodyWriter<?> bodyWriter)
        {
        super(router, bodyWriter);
        }

    @Override
    protected void beforeRouting(HttpRequest request)
        {
        }

    /**
     * Returns the {@link RequestRouter} used by this handler.
     *
     * @return the {@link RequestRouter} used by this handler
     */
    public RequestRouter getRouter()
        {
        return f_router;
        }
    }
