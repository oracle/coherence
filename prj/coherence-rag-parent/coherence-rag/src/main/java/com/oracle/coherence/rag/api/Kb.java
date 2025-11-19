/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.api;

import com.oracle.coherence.rag.config.StoreConfig;
import com.oracle.coherence.rag.ChatAssistant;
import com.oracle.coherence.rag.model.StreamingChatModelSupplier;
import com.oracle.coherence.rag.model.EmbeddingModelSupplier;
import com.oracle.coherence.rag.model.ModelName;
import com.oracle.coherence.rag.util.Timer;

import com.oracle.coherence.cdi.events.MapName;

import com.tangosol.net.NamedMap;
import com.tangosol.net.Session;

import com.tangosol.util.MapEvent;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;

import io.helidon.microprofile.cors.CrossOrigin;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * REST API controller for managing document stores and performing cross-store operations
 * in the Coherence RAG framework.
 * <p/>
 * This class provides HTTP endpoints for:
 * <ul>
 *   <li>Managing multiple document stores</li>
 *   <li>Configuring store settings</li>
 *   <li>Performing searches across multiple stores</li>
 *   <li>Executing chat operations with context retrieval</li>
 * </ul>
 * <p/>
 * The API is designed to work with multiple named stores, each with their own
 * configuration and content. Cross-store operations aggregate results from
 * all configured stores to provide comprehensive search and chat capabilities.
 * <p/>
 * All endpoints support CORS for web application integration and use JSON
 * for request/response payloads where applicable.
 * 
 * @author Aleks Seovic  2025.07.04
 * @since 25.09
 */
@SuppressWarnings("CdiInjectionPointsInspection")
@ApplicationScoped
@Path("/api/kb")
public class Kb
    {
    /**
     * Coherence session for accessing distributed data structures.
     */
    @Inject
    private Session session;

    /**
     * Named map containing store configurations indexed by store name.
     */
    @Inject
    private NamedMap<String, StoreConfig> storeConfig;

    /**
     * Supplier for creating embedding model instances.
     */
    @Inject
    private EmbeddingModelSupplier embeddingModelSupplier;

    /**
     * Supplier for creating chat model instances.
     */
    @Inject
    private StreamingChatModelSupplier chatModelSupplier;

    /**
     * Cache of store instances indexed by store name.
     */
    private final ConcurrentMap<String, Store> mapStore = new ConcurrentHashMap<>();

    /**
     * Initializes existing stores after bean construction.
     * <p/>
     * This method is called automatically after dependency injection to
     * ensure all configured stores are properly initialized and ready
     * for use.
     */
    @PostConstruct
    void initStores()
        {
        // initialize existing stores
        storeConfig.keySet().forEach(this::getStore);
        }

    /**
     * Handles store creation events to automatically initialize new stores.
     * <p/>
     * This method is called whenever a new store configuration is added
     * to the storeConfig map, ensuring that stores are initialized immediately
     * upon creation.
     *
     * @param evt the map event containing the new store configuration
     */
    void onStoreCreated(@Observes @MapName("storeConfig") MapEvent<String, StoreConfig> evt)
        {
        getStore(evt.getKey());
        }

    /**
     * Handles CORS preflight requests for the main API endpoint.
     */
    @OPTIONS
    @CrossOrigin
    public void cors() {}

    /**
     * Handles CORS preflight requests for the search endpoint.
     */
    @OPTIONS
    @CrossOrigin
    @Path("search")
    public void corsSearch() {}

    /**
     * Handles CORS preflight requests for the chat endpoint.
     */
    @OPTIONS
    @CrossOrigin
    @Path("chat")
    public void corsChat() {}

    /**
     * Handles CORS preflight requests for store configuration endpoints.
     *
     * @param name the store name (unused but required for path matching)
     */
    @OPTIONS
    @CrossOrigin
    @Path("config/{storeName}")
    @SuppressWarnings("unused")
    public void corsStoreConfig(@PathParam("storeName") String name) {}

    /**
     * Returns a specific store instance for sub-resource operations.
     * <p/>
     * This method provides access to individual store operations by
     * delegating to the Store resource class. The returned Store handles
     * document management, indexing, and search operations for the
     * specified store.
     *
     * @param storeName the name of the store to access
     *
     * @return a Store instance for the specified store name
     * 
     * @throws IllegalArgumentException if the store name is null or invalid
     */
    @Path("{storeName}")
    public Store store(@PathParam("storeName") String storeName)
        {
        return getStore(storeName);
        }

    /**
     * Returns a list of all configured stores with their configurations.
     * <p/>
     * This endpoint provides an overview of all available stores in the
     * system, including their names and current configurations. The list
     * is sorted alphabetically by store name for consistent ordering.
     *
     * @return a Response containing a list of ConfiguredStore records
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response storeList()
        {
        List<ConfiguredStore> stores = storeConfig.entrySet().stream()
                .map(e -> new ConfiguredStore(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(ConfiguredStore::name))
                .toList();
        return Response.ok(stores).build();
        }

    /**
     * Returns the configuration for a specific store.
     * <p/>
     * This endpoint allows clients to retrieve the current configuration
     * settings for a named store, including embedding model settings,
     * chunk parameters, and indexing configuration.
     *
     * @param storeName the name of the store whose configuration to retrieve
     *
     * @return a Response containing the StoreConfig, or 404 if store not found
     */
    @GET
    @Path("config/{storeName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response storeConfig(@PathParam("storeName") String storeName)
        {
        return Response.ok(storeConfig.get(storeName)).build();
        }

    /**
     * Creates or updates the configuration for a specific store.
     * <p/>
     * This endpoint allows clients to configure store settings including
     * embedding models, document chunking parameters, and indexing options.
     * If the store doesn't exist, it will be created with the provided
     * configuration.
     *
     * @param storeName the name of the store to configure
     * @param config    the new configuration to apply
     *
     * @return a Response with no content indicating successful configuration
     * 
     * @throws IllegalArgumentException if the store name or config is invalid
     */
    @PUT
    @Path("config/{storeName}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response configureStore(@PathParam("storeName") String storeName, StoreConfig config)
        {
        storeConfig.put(storeName, config);
        return Response.noContent().build();
        }

    /**
     * Performs a search across all configured stores.
     * <p/>
     * This endpoint executes a search query against all available stores
     * and aggregates the results, providing a unified view of matching
     * document chunks across the entire system. Results are ranked by
     * score and limited to the requested maximum number.
     *
     * @param req the search request containing query parameters
     *
     * @return a Response containing SearchResult with aggregated results and timing
     * 
     * @throws IllegalArgumentException if the search request is invalid
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("search")
    public Response search(Store.SearchRequest req)
        {
        Timer timer = new Timer().start();
        List<Store.ChunkResult> results = findChunks(req.query(), req.maxResults(), req.minScore(), req.fullTextWeight(), req.scoringModel());
        timer.stop();

        return Response.ok(new Store.SearchResult(results, timer.duration().toMillis())).build();
        }

    /**
     * Executes a chat request with context retrieval across all stores.
     * <p/>
     * This endpoint processes a chat request by first retrieving relevant
     * context from all configured stores, then using a chat model to generate
     * a response. The response is streamed back to the client for real-time
     * interaction.
     *
     * @param req the chat request containing the question and parameters
     *
     * @return a Response containing a streaming answer
     * 
     * @throws IllegalArgumentException if the chat request is invalid
     * @throws RuntimeException if the chat model is unavailable
     */
    @POST
    @Path("chat")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response chat(Store.ChatRequest req)
        {
        StreamingChatModel chatModel = req.chatModel() == null
                                  ? chatModelSupplier.get()
                                  : chatModelSupplier.get(new ModelName(req.chatModel()));
        ChatAssistant assistant = createChatAssistant(chatModel, req.maxResults(), req.minScore(), req.fullTextWeight(), req.scoringModel());
        TokenStream   answer = assistant.answer(req.question());

        return Response.ok(new Store.StreamingAnswer(req.question(), answer)).build();
        }

    /**
     * Gets or creates a store instance for the specified name.
     * <p/>
     * This method ensures that store instances are created on-demand and
     * cached for efficient reuse. Each store is initialized with the current
     * session and model suppliers.
     *
     * @param storeName the name of the store to retrieve or create
     *
     * @return a Store instance for the specified name
     */
    private Store getStore(String storeName)
        {
        return mapStore.computeIfAbsent(storeName, name -> new Store(session, storeName, embeddingModelSupplier, chatModelSupplier));
        }

    /**
     * Finds document chunks across all stores matching the search criteria.
     * <p/>
     * This method performs parallel searches across all configured stores
     * and aggregates the results. The results are sorted by score in
     * descending order and limited to the requested maximum number.
     *
     * @param query           the search query text
     * @param maxResults      the maximum number of results to return
     * @param minScore        the minimum score threshold for results
     * @param fullTextWeight  the weight to apply to full-text search results
     * @param scoringModel    the name of the scoring model to use for reranking
     *
     * @return a list of ChunkResult objects sorted by score
     */
    private List<Store.ChunkResult> findChunks(String query, int maxResults, double minScore, double fullTextWeight, String scoringModel)
        {
        List<Store> stores = storeConfig.keySet().stream()
                        .map(this::getStore)
                        .toList();
        return stores.parallelStream()
                        .flatMap(store -> store.findChunks(query, maxResults, minScore, fullTextWeight, scoringModel).stream())
                        .sorted(Comparator.comparingDouble(Store.ChunkResult::getScore).reversed())
                        .limit(maxResults)
                        .toList();
        }

    /**
     * Creates a chat assistant with the specified configuration.
     * <p/>
     * This method sets up a chat assistant that retrieves context from
     * all configured stores before generating responses. The assistant
     * uses the provided chat model and search parameters to deliver
     * contextually relevant answers.
     *
     * @param chatModel       the streaming chat model to use for response generation
     * @param maxResults      the maximum number of context chunks to retrieve
     * @param minScore        the minimum score threshold for context chunks
     * @param fullTextWeight  the weight to apply to full-text search results
     * @param scoringModel    the name of the scoring model to use for reranking
     *
     * @return a configured ChatAssistant instance
     */
    private ChatAssistant createChatAssistant(StreamingChatModel chatModel, int maxResults, double minScore, double fullTextWeight, String scoringModel)
        {
        ContentRetriever retriever = query ->
            {
            List<Store.ChunkResult> results = findChunks(query.text(), maxResults, minScore, fullTextWeight, scoringModel);
            return results.stream().map(chunk -> Content.from(chunk.getText())).toList();
            };

        return AiServices.builder(ChatAssistant.class)
                .streamingChatModel(chatModel)
                .contentRetriever(retriever)
                .build();
        }

    /**
     * Record representing a configured store with its name and configuration.
     * <p/>
     * This record is used in API responses to provide a summary of available
     * stores along with their current configuration settings.
     *
     * @param name   the name of the store
     * @param config the configuration of the store
     */
    public record ConfiguredStore(String name, StoreConfig config)
        {}
    }
