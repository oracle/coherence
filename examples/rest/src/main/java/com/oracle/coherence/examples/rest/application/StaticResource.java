/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.examples.rest.application;

import com.tangosol.util.Base;
import com.tangosol.util.Resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.IOException;

import java.net.URL;

/**
 * Serves static resources for an application from the class-path.
 * <p>
 * Copyright (c) 2015. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 * @since 12.2.1
 */
@Path("{resource: .*}")
public class StaticResource
    {
    /**
     * The base folder containing static resources on the class path.
     */
    private static String BASE_FOLDER = "web";

    @GET
    public Response getResource(@PathParam("resource") String resource)
            throws IOException
        {
        // construct the resource path relative to the base folder
        String resourcePath = BASE_FOLDER + '/' + resource;

        // construct a URL to the resource (using the class loader)
        URL url = Resources.findFileOrResource(resourcePath,
                Base.ensureClassLoader(null));

        if (url == null)
            {
            return Response.status(Response.Status.NOT_FOUND).build();
            }
        else
            {
            // add special handing for Oracle JET
            if (url.toString().endsWith(".svg"))
                {
                return Response.ok(url.openStream(), new MediaType("image", "svg+xml")).build();
                }
            else if (url.toString().endsWith(".png"))
                {
                return Response.ok(url.openStream(), new MediaType("image", "png")).build();
                }
            else
                {
                return Response.ok(url.openStream()).build();
                }
            }
        }
    }
