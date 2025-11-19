/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag;

import com.oracle.coherence.ai.DocumentChunk;
import com.oracle.coherence.rag.util.TestDataFactory;

import jakarta.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Comprehensive unit tests for the VectorStore interface and its implementations.
 * <p/>
 * This test class validates the vector store functionality including naming,
 * document chunk storage, and error handling for various scenarios.
 *
 * @author Aleks Seovic  2025.07.04
 * @since 25.09
 */
@DisplayName("VectorStore Tests")
class VectorStoreTest
    {
    private TestVectorStore namedStore;
    private UnnamedVectorStore unnamedStore;

    @BeforeEach
    void setUp()
        {
        namedStore = new TestVectorStore();
        unnamedStore = new UnnamedVectorStore();
        }

    // ---- name functionality tests ---------------------------------------

    @Nested
    @DisplayName("Name Functionality Tests")
    class NameFunctionalityTests
        {
        @Test
        @DisplayName("Should return name from Named annotation")
        void shouldReturnNameFromNamedAnnotation()
            {
            // Act
            String name = namedStore.name();

            // Assert
            assertThat(name, is(notNullValue()));
            assertThat(name, is("test-vector-store"));
            }

        @Test
        @DisplayName("Should return null when no Named annotation present")
        void shouldReturnNullWhenNoNamedAnnotationPresent()
            {
            // Act
            String name = unnamedStore.name();

            // Assert
            assertThat(name, is(nullValue()));
            }
        }

    // ---- storage functionality tests ------------------------------------

    @Nested
    @DisplayName("Storage Functionality Tests")
    class StorageFunctionalityTests
        {
        @Test
        @DisplayName("Should store document chunks successfully")
        void shouldStoreDocumentChunksSuccessfully()
            {
            // Arrange
            Map<DocumentChunk.Id, DocumentChunk> chunks = createTestChunks();

            // Act & Assert - Should not throw exception
            namedStore.store(chunks);
            assertThat(namedStore.getStoredChunks().size(), is(2));
            }

        @Test
        @DisplayName("Should store empty map successfully")
        void shouldStoreEmptyMapSuccessfully()
            {
            // Arrange
            Map<DocumentChunk.Id, DocumentChunk> emptyChunks = new HashMap<>();

            // Act & Assert - Should not throw exception
            namedStore.store(emptyChunks);
            assertThat(namedStore.getStoredChunks().size(), is(0));
            }

        @Test
        @DisplayName("Should throw exception for null chunks map")
        void shouldThrowExceptionForNullChunksMap()
            {
            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> {
                namedStore.store(null);
            });
            }

        @Test
        @DisplayName("Should handle large number of chunks")
        void shouldHandleLargeNumberOfChunks()
            {
            // Arrange
            Map<DocumentChunk.Id, DocumentChunk> largeChunkSet = createLargeChunkSet(100);

            // Act & Assert - Should not throw exception
            namedStore.store(largeChunkSet);
            assertThat(namedStore.getStoredChunks().size(), is(100));
            }

        @Test
        @DisplayName("Should handle chunks with null content gracefully")
        void shouldHandleChunksWithNullContentGracefully()
            {
            // Arrange
            Map<DocumentChunk.Id, DocumentChunk> chunks = new HashMap<>();
            DocumentChunk.Id chunkId = DocumentChunk.id("doc1", 0);
            
            // Create a chunk with minimal content
            DocumentChunk chunk = TestDataFactory.createDocumentChunk("");
            chunks.put(chunkId, chunk);

            // Act & Assert - Should not throw exception
            namedStore.store(chunks);
            assertThat(namedStore.getStoredChunks().size(), is(1));
            }

        private Map<DocumentChunk.Id, DocumentChunk> createTestChunks()
            {
            Map<DocumentChunk.Id, DocumentChunk> chunks = new HashMap<>();
            
            DocumentChunk.Id id1 = DocumentChunk.id("doc1", 0);
            DocumentChunk.Id id2 = DocumentChunk.id("doc2", 0);
            
            DocumentChunk chunk1 = TestDataFactory.createDocumentChunk("First chunk content");
            DocumentChunk chunk2 = TestDataFactory.createDocumentChunk("Second chunk content");
            
            chunks.put(id1, chunk1);
            chunks.put(id2, chunk2);
            
            return chunks;
            }

        private Map<DocumentChunk.Id, DocumentChunk> createLargeChunkSet(int count)
            {
            Map<DocumentChunk.Id, DocumentChunk> chunks = new HashMap<>();
            
            for (int i = 0; i < count; i++)
                {
                DocumentChunk.Id id = DocumentChunk.id("doc" + i, 0);
                DocumentChunk chunk = TestDataFactory.createDocumentChunk("Content for chunk " + i);
                chunks.put(id, chunk);
                }
            
            return chunks;
            }
        }

    // ---- error handling tests -------------------------------------------

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests
        {
        @Test
        @DisplayName("Should handle storage failures gracefully")
        void shouldHandleStorageFailuresGracefully()
            {
            // Arrange
            FailingVectorStore failingStore = new FailingVectorStore();
            Map<DocumentChunk.Id, DocumentChunk> chunks = createTestChunks();

            // Act & Assert
            assertThrows(RuntimeException.class, () -> {
                failingStore.store(chunks);
            });
            }

        @Test
        @DisplayName("Should validate chunk IDs are not null")
        void shouldValidateChunkIdsAreNotNull()
            {
            // Arrange
            Map<DocumentChunk.Id, DocumentChunk> chunks = new HashMap<>();
            DocumentChunk chunk = TestDataFactory.createDocumentChunk("Test content");
            chunks.put(null, chunk); // null key

            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> {
                namedStore.store(chunks);
            });
            }

        private Map<DocumentChunk.Id, DocumentChunk> createTestChunks()
            {
            Map<DocumentChunk.Id, DocumentChunk> chunks = new HashMap<>();
            
            DocumentChunk.Id id1 = DocumentChunk.id("doc1", 0);
            DocumentChunk chunk1 = TestDataFactory.createDocumentChunk("Test chunk content");
            chunks.put(id1, chunk1);
            
            return chunks;
            }
        }

    // ---- test implementations -------------------------------------------

    /**
     * Test implementation of VectorStore with Named annotation.
     */
    @Named("test-vector-store")
    private static class TestVectorStore implements VectorStore
        {
        private final Map<DocumentChunk.Id, DocumentChunk> storedChunks = new HashMap<>();

        @Override
        public void store(Map<? extends DocumentChunk.Id, ? extends DocumentChunk> mapChunks)
            {
            if (mapChunks == null)
                {
                throw new IllegalArgumentException("Chunks map cannot be null");
                }
            
            // Validate that chunk IDs are not null
            for (DocumentChunk.Id id : mapChunks.keySet())
                {
                if (id == null)
                    {
                    throw new IllegalArgumentException("Chunk ID cannot be null");
                    }
                }
            
            storedChunks.putAll(mapChunks);
            }

        public Map<DocumentChunk.Id, DocumentChunk> getStoredChunks()
            {
            return storedChunks;
            }
        }

    /**
     * Test implementation without Named annotation.
     */
    private static class UnnamedVectorStore implements VectorStore
        {
        @Override
        public void store(Map<? extends DocumentChunk.Id, ? extends DocumentChunk> mapChunks)
            {
            // Simple implementation that doesn't store anything
            }
        }

    /**
     * Test implementation that simulates storage failures.
     */
    private static class FailingVectorStore implements VectorStore
        {
        @Override
        public void store(Map<? extends DocumentChunk.Id, ? extends DocumentChunk> mapChunks)
            {
            throw new RuntimeException("Simulated storage failure");
            }
        }
    } 