/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag;

import com.oracle.coherence.ai.DocumentChunk;

import jakarta.inject.Named;

import java.util.Map;

/**
 * Interface for vector-based document storage systems.
 * <p/>
 * This interface defines the contract for storing and managing document chunks
 * with their associated vector embeddings. Vector stores are specialized storage
 * systems designed to efficiently store and retrieve documents based on semantic
 * similarity using vector embeddings.
 * <p/>
 * Implementations of this interface can support various vector storage backends
 * such as Coherence, Pinecone, Weaviate, or other vector databases.
 * 
 * @author Aleks Seovic  2025.06.28
 * @since 25.09
 */
public interface VectorStore
    {
    /**
     * Gets the name of this vector store implementation.
     * <p/>
     * This method extracts the name from the {@link Named} annotation if present
     * on the implementing class. The name can be used for configuration and
     * dependency injection purposes.
     * <p/>
     * If no {@link Named} annotation is present, this method returns null.
     * 
     * @return the name of this vector store, or null if not annotated with {@link Named}
     */
    default String name()
        {
        Named named = getClass().getAnnotation(Named.class);
        return named == null ? null : named.value();
        }

    /**
     * Stores a collection of document chunks with their vector embeddings.
     * <p/>
     * This method stores the provided document chunks in the vector store,
     * making them available for similarity search and retrieval operations.
     * The chunks are indexed by their unique identifiers for efficient lookup.
     * <p/>
     * The implementation should handle the storage of both the document content
     * and the associated vector embeddings in an optimized manner for the specific
     * vector store backend.
     * 
     * @param mapChunks a map of document chunk IDs to document chunks containing
     *                  text content and vector embeddings
     * 
     * @throws IllegalArgumentException if the map is null or contains invalid data
     * @throws RuntimeException if the storage operation fails due to backend errors
     */
    void store(Map<? extends DocumentChunk.Id, ? extends DocumentChunk> mapChunks);
    }
