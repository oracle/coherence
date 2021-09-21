/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.management.internal.filters;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;

import javax.ws.rs.ext.Provider;

import java.io.IOException;

/**
 * {@link ContainerResponseFilter} adds header that disables MIME sniffing by client on all responses.
 *
 * @author jf  2020.08.25
 * @since 12.2.1.5.0
 */
@Provider
public class DenySniffResponseFilter
    implements ContainerResponseFilter
    {
    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
        throws IOException
        {
        responseContext.getHeaders().add("X-Content-Type-Options", "nosniff");
        }
}
