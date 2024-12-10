/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest.providers;

import javax.ws.rs.core.Response;

import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Maps SecurityExceptions to 403: Forbidden response.
 *
 * @author as  2012.01.12
 */
@Provider
public class SecurityExceptionMapper
        implements ExceptionMapper<SecurityException>
    {
    public Response toResponse(SecurityException ex)
        {
        return Response.status(Response.Status.FORBIDDEN).build();
        }
    }
