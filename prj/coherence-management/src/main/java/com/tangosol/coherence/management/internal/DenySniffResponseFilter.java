/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.management.internal;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import java.io.IOException;
import java.util.List;

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
        MultivaluedMap<String, Object> headers = responseContext.getHeaders();
        List<Object>                   list    = headers.get(CONTENT_TYPE);
        if (list == null || !list.contains(NO_SNIFF))
            {
            headers.add(CONTENT_TYPE, NO_SNIFF);
            }
        }

    // ----- constants ------------------------------------------------------

    private static final String CONTENT_TYPE = "X-Content-Type-Options";

    private static final String NO_SNIFF = "nosniff";
    }
