/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.rest;

import com.tangosol.coherence.http.HttpServer;

import java.util.Arrays;
import java.util.Collection;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import jakarta.xml.bind.annotation.XmlRootElement;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.APPLICATION_XML;

/**
 * Simple resource used for testing {@link HttpServer} implementations.
 */
@Path("/test")
public class TestResource
    {
    @GET
    @Path("echo")
    public Response echo(@QueryParam("s") String s)
        {
        return Response.ok(s).build();
        }

    @GET
    @Path("principal")
    public Response principal(@Context SecurityContext securityContext)
        {
        String principal = securityContext.getUserPrincipal().getName();
        String scheme    = securityContext.getAuthenticationScheme();
        return Response.ok(scheme + ":" + principal).build();
        }

    @GET
    @Path("collection")
    @Produces({APPLICATION_JSON, APPLICATION_XML})
    public Collection collection()
        {
        return Arrays.asList(new Letter("A"), new Letter("B"), new Letter("C"), new Letter("D"));
        }

    @GET
    @Path("runtime-exception")
    public Response runtimeException()
        {
        throw new RuntimeException();
        }

    @GET
    @Path("security-exception")
    public Response securityException()
        {
        throw new SecurityException();
        }

    // ---- inner class: Letter ---------------------------------------------

    @XmlRootElement
    public static class Letter
        {
        public Letter(String value)
            {
            this.value = value;
            }

        public String value;
        }
    }
