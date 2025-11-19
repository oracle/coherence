/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.api;

import io.helidon.microprofile.server.Server;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ApplicationPath;

/**
 * Helidon MicroProfile application configuration for the Coherence RAG REST API.
 * <p/>
 * This class serves as the main entry point for the REST API server, providing
 * configuration and initialization for the Helidon MicroProfile runtime. It
 * defines the base path for all REST endpoints and enables automatic discovery
 * of JAX-RS resources.
 * <p/>
 * The application automatically discovers and registers all REST controllers
 * in the same package and sub-packages, including:
 * <ul>
 *   <li>{@link Store} - Document store operations</li>
 *   <li>{@link Kb} - Cross-store operations</li>
 *   <li>{@link Config} - Configuration management</li>
 *   <li>{@link Scoring} - Document relevance scoring</li>
 * </ul>
 * <p/>
 * The server can be started programmatically or via the main method for
 * standalone execution.
 * 
 * @author Aleks Seovic  2025.07.04
 * @since 25.09
 */
@ApplicationScoped
@ApplicationPath("/")
public class Application extends jakarta.ws.rs.core.Application
    {
    /**
     * Main method to start the Helidon MicroProfile server.
     * <p/>
     * This method creates and starts a Helidon server instance with the
     * default configuration. The server will automatically discover and
     * register all REST endpoints and CDI beans.
     * 
     * @param args command line arguments (not used)
     */
    public static void main(String[] args)
        {
        Server server = Server.create();
        server.start();
        
        System.out.println("Coherence RAG REST API server started");
        System.out.println("Server running on: " + server.port());
        System.out.println("Health check: http://localhost:" + server.port() + "/health");
        }
    } 
