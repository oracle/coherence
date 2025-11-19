/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.internal;

import com.oracle.coherence.rag.config.index.IndexConfig;
import com.oracle.coherence.rag.config.StoreConfig;
import com.oracle.coherence.rag.model.EmbeddingModelSupplier;

import com.tangosol.net.cache.CacheLoader;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.eclipse.microprofile.config.Config;

/**
 * Cache loader for automatically creating StoreConfig instances with default values.
 * <p/>
 * This CDI-managed cache loader provides on-demand creation of StoreConfig objects
 * when document stores are accessed but don't have explicit configuration. It
 * creates sensible default configurations based on system-wide settings and
 * configuration properties.
 * <p/>
 * The loader automatically configures new stores with:
 * <ul>
 *   <li>Default embedding model from the embedding model supplier</li>
 *   <li>Configurable vector store type (default: "none")</li>
 *   <li>Configurable document chunking parameters</li>
 *   <li>Default index configuration</li>
 * </ul>
 * <p/>
 * Configuration properties supported:
 * <ul>
 *   <li>{@code vector.store.type} - Type of vector store to use (default: "none")</li>
 *   <li>{@code chunk.size} - Size of document chunks in characters (default: 1250)</li>
 *   <li>{@code chunk.overlap} - Overlap between chunks in characters (default: 150)</li>
 * </ul>
 * 
 * @author Aleks Seovic  2025.07.04
 * @since 25.09
 */
@SuppressWarnings("CdiInjectionPointsInspection")
@Named("StoreConfigCacheLoader")
@ApplicationScoped
public class StoreConfigCacheLoader
        implements CacheLoader<String, StoreConfig>
    {
    /**
     * Supplier for embedding models to determine the default embedding model.
     * <p/>
     * Used to automatically configure new stores with the currently configured
     * default embedding model.
     */
    @Inject
    private EmbeddingModelSupplier embeddingModelSupplier;

    /**
     * MicroProfile Config for accessing configuration properties.
     * <p/>
     * Provides access to system-wide configuration values for chunking
     * parameters and vector store settings.
     */
    @Inject
    private Config config;

    /**
     * Creates a default StoreConfig for the specified store name.
     * <p/>
     * This method generates a new StoreConfig with sensible defaults based on
     * the current system configuration. The configuration includes the default
     * embedding model, chunking parameters, and vector store settings.
     * 
     * @param storeName the name of the store to create configuration for
     * 
     * @return a new StoreConfig with default values
     */
    public StoreConfig load(String storeName)
        {
        return new StoreConfig(embeddingModelSupplier.defaultModelName().fullName(),
                               false,
                               new IndexConfig<>(),
                               chunkSize(),
                               chunkOverlap());
        }

    /**
     * Gets the configured vector store type.
     * <p/>
     * Reads the vector store type from configuration, defaulting to "none"
     * if not specified. This determines which vector store backend will be
     * used for new document stores.
     * 
     * @return the vector store type (e.g., "none", "pinecone", "weaviate")
     */
    private String vectorStoreType()
        {
        return config.getOptionalValue("vector.store.type", String.class).orElse("none");
        }

    /**
     * Gets the configured document chunk size.
     * <p/>
     * Reads the chunk size from configuration, defaulting to 1250 characters
     * if not specified. This determines how large each document chunk will be
     * during the document splitting process.
     * 
     * @return the chunk size in characters
     */
    private int chunkSize()
        {
        return config.getOptionalValue("chunk.size", Integer.class).orElse(1250);
        }

    /**
     * Gets the configured chunk overlap size.
     * <p/>
     * Reads the chunk overlap from configuration, defaulting to 150 characters
     * if not specified. This determines how much text overlap exists between
     * adjacent chunks to maintain context continuity.
     * 
     * @return the chunk overlap in characters
     */
    private int chunkOverlap()
        {
        return config.getOptionalValue("chunk.overlap", Integer.class).orElse(150);
        }
    }
