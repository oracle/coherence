/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.api;

import com.oracle.coherence.mp.config.CoherenceConfigSource;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST API endpoint for managing Coherence configuration properties.
 * <p/>
 * This JAX-RS resource provides HTTP endpoints for retrieving and updating
 * configuration properties in the Coherence RAG framework. It enables
 * dynamic configuration management through a RESTful interface, allowing
 * clients to read and modify configuration values at runtime.
 * <p/>
 * The configuration management is backed by Coherence's distributed
 * configuration capabilities, ensuring that configuration changes are
 * propagated across all cluster members.
 * <p/>
 * Available endpoints:
 * <ul>
 *   <li>GET /api/_config/{property} - Retrieve a configuration property value</li>
 *   <li>PUT /api/_config/{property} - Set a configuration property value</li>
 * </ul>
 * 
 * @author Aleks Seovic  2025.07.04
 * @since 25.09
 */
@ApplicationScoped
@Path("/api/_config")
public class Config
    {
    /**
     * The Coherence configuration source for managing distributed configuration.
     * <p/>
     * This configuration source provides access to the distributed configuration
     * storage and enables real-time configuration updates across the cluster.
     */
    @Inject
    private CoherenceConfigSource coherenceConfig;

    /**
     * Retrieves the value of a configuration property.
     * <p/>
     * This endpoint returns the current value of the specified configuration
     * property. If the property does not exist, a 404 Not Found response
     * is returned.
     * 
     * @param property the name of the configuration property to retrieve
     * 
     * @return a Response containing the property value as plain text,
     *         or 404 Not Found if the property does not exist
     */
    @GET
    @Path("{property}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response get(@PathParam("property") String property)
        {
        String value = coherenceConfig.getValue(property);
        return value == null
               ? Response.status(Response.Status.NOT_FOUND).build()
               : Response.ok(value).build();
        }

    /**
     * Sets the value of a configuration property.
     * <p/>
     * This endpoint updates the specified configuration property with a new
     * value. The change is propagated across all cluster members and takes
     * effect immediately.
     * 
     * @param property the name of the configuration property to set
     * @param value the new value to assign to the property
     * 
     * @return a Response containing the previous property value as plain text,
     *         or null if the property was not previously set
     */
    @PUT
    @Path("{property}")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response set(@PathParam("property") String property, String value)
        {
        return Response.ok(coherenceConfig.setValue(property, value)).build();
        }
    }
