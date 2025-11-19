/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.api;

import com.oracle.coherence.rag.config.ConfigKey;
import com.oracle.coherence.rag.config.ConfigRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Objects;

/**
 * REST API for managing model configuration documents.
 * <p/>
 * This endpoint allows CRUD operations on provider-specific model configuration
 * stored as raw JSON in the distributed configuration repository. Configuration
 * entries are addressed by a logical key composed of the model {@code type},
 * {@code provider}, and {@code model} name.
 * <p/>
 * Keys follow the convention: {@code "{type}:{provider}/{model}"}, for example
 * {@code "chat:OpenAI/gpt-4o-mini"} or {@code "embedding:OpenAI/text-embedding-3-small"}.
 */
@ApplicationScoped
@Path("/api/models")
public class Models
    {
    /**
     * Lists all configuration entries present in the repository.
     *
     * @return a list of configuration entries (keys)
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response list()
        {
        List<ConfigEntry> entries = repo.keys().stream()
                .map(k -> new ConfigEntry(String.valueOf(k.key())))
                .sorted((a, b) -> a.key.compareToIgnoreCase(b.key))
                .toList();

        return Response.ok(entries).build();
        }

    /**
     * Retrieves the JSON configuration for the specified model.
     *
     * @param type      model type (e.g., {@code chat}, {@code streamingChat}, {@code embedding}, {@code scoring})
     * @param provider  model provider (e.g., {@code OpenAI}, {@code OCI}, {@code Ollama}, {@code DeepSeek})
     * @param model     provider-specific model name
     *
     * @return the JSON payload, or {@code 404} if not found
     */
    @GET
    @Path("{type}/{provider}/{model}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@PathParam("type") String type,
                        @PathParam("provider") String provider,
                        @PathParam("model") String model)
        {
        ConfigKey key = new ConfigKey(key(type, provider, model));
        String    json = repo.get(key);
        return json == null
               ? Response.status(Response.Status.NOT_FOUND).build()
               : Response.ok(json).build();
        }

    /**
     * Creates or updates the JSON configuration for the specified model.
     *
     * @param type      model type
     * @param provider  model provider
     * @param model     provider-specific model name
     * @param json      JSON payload to store
     *
     * @return {@code 204 No Content}
     */
    @PUT
    @Path("{type}/{provider}/{model}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response put(@PathParam("type") String type,
                        @PathParam("provider") String provider,
                        @PathParam("model") String model,
                        String json)
        {
        Objects.requireNonNull(json, "Configuration JSON cannot be null");
        ConfigKey key = new ConfigKey(key(type, provider, model));
        repo.put(key, json);
        return Response.noContent().build();
        }

    /**
     * Deletes the configuration for the specified model, if present.
     *
     * @param type      model type
     * @param provider  model provider
     * @param model     provider-specific model name
     *
     * @return {@code 204 No Content}
     */
    @DELETE
    @Path("{type}/{provider}/{model}")
    public Response delete(@PathParam("type") String type,
                           @PathParam("provider") String provider,
                           @PathParam("model") String model)
        {
        ConfigKey key = new ConfigKey(key(type, provider, model));
        repo.remove(key);
        return Response.noContent().build();
        }

    // ---- helpers ---------------------------------------------------------

    private static String key(String type, String provider, String model)
        {
        return type + ':' + provider + '/' + model;
        }

    // ---- data members ----------------------------------------------------

    @Inject
    private ConfigRepository repo;

    /** Entry returned by the list endpoint. */
    public record ConfigEntry(String key) {}
    }


