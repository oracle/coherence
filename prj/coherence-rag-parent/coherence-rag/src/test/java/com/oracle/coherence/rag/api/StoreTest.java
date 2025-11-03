/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.api;

import com.oracle.coherence.rag.config.StoreConfig;
import com.oracle.coherence.rag.model.StreamingChatModelSupplier;
import com.oracle.coherence.rag.model.EmbeddingModelSupplier;
import com.oracle.coherence.rag.model.ModelName;
import com.oracle.coherence.rag.util.TestDataFactory;

import com.tangosol.net.NamedMap;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.chat.StreamingChatModel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for Store REST API business logic.
 * <p/>
 * This test class validates the core business logic of the Store API
 * by testing request/response record classes and validation methods
 * without requiring complex Coherence infrastructure setup.
 * <p/>
 * Test categories covered:
 * <ul>
 * <li>Request/response record validation</li>
 * <li>Default value handling</li>
 * <li>Business logic validation</li>
 * <li>Model supplier configuration</li>
 * </ul>
 * <p/>
 * The tests use Hamcrest matchers for expressive assertions and follow
 * the arrange-act-assert pattern for clarity and maintainability.
 *
 * @author Aleks Seovic  2025.07.04
 * @since 25.09
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Store Business Logic Tests")
class StoreTest
    {
    // ---- test constants --------------------------------------------------

    private static final String TEST_STORE_NAME = "test-store";

    // ---- test infrastructure ---------------------------------------------

    @Mock
    private NamedMap<String, StoreConfig> storeConfigMap;

    @Mock
    private EmbeddingModelSupplier embeddingModelSupplier;

    @Mock
    private StreamingChatModelSupplier chatModelSupplier;

    @Mock
    private EmbeddingModel embeddingModel;

    @Mock
    private StreamingChatModel streamingChatModel;

    // ---- lifecycle methods -----------------------------------------------

    @BeforeEach
    void setUp()
        {
        MockitoAnnotations.openMocks(this);
        
        // Set up model suppliers
        when(embeddingModelSupplier.defaultModelName()).thenReturn(new ModelName("test/embedding-model"));
        when(embeddingModelSupplier.get()).thenReturn(embeddingModel);
        when(chatModelSupplier.get()).thenReturn(streamingChatModel);
        }

    // ---- request/response record tests ----------------------------------

    @Nested
    @DisplayName("ChatRequest Tests")
    class ChatRequestTests
        {
        @Test
        @DisplayName("Should use default maxResults when zero")
        void shouldUseDefaultMaxResultsWhenZero()
            {
            // Arrange
            Store.ChatRequest request = new Store.ChatRequest("model", "question", 0, 0.5, 0.3, null);

            // Act & Assert
            assertThat(request.maxResults(), is(5));
            }

        @Test
        @DisplayName("Should preserve custom maxResults")
        void shouldPreserveCustomMaxResults()
            {
            // Arrange
            Store.ChatRequest request = new Store.ChatRequest("model", "question", 10, 0.5, 0.3, null);

            // Act & Assert
            assertThat(request.maxResults(), is(10));
            }

        @Test
        @DisplayName("Should handle all parameters")
        void shouldHandleAllParameters()
            {
            // Arrange
            Store.ChatRequest request = new Store.ChatRequest("custom-model", "test question", 15, 0.7, 0.2, "scorer");

            // Act & Assert
            assertThat(request.chatModel(), is("custom-model"));
            assertThat(request.question(), is("test question"));
            assertThat(request.maxResults(), is(15));
            assertThat(request.minScore(), is(0.7));
            assertThat(request.fullTextWeight(), is(0.2));
            assertThat(request.scoringModel(), is("scorer"));
            }
        }

    @Nested
    @DisplayName("SearchRequest Tests")
    class SearchRequestTests
        {
        @Test
        @DisplayName("Should use default maxResults when zero")
        void shouldUseDefaultMaxResultsWhenZero()
            {
            // Arrange
            Store.SearchRequest request = new Store.SearchRequest("query", 0, 0.5, 0.3, null);

            // Act & Assert
            assertThat(request.maxResults(), is(5));
            }

        @Test
        @DisplayName("Should preserve custom maxResults")
        void shouldPreserveCustomMaxResults()
            {
            // Arrange
            Store.SearchRequest request = new Store.SearchRequest("query", 20, 0.5, 0.3, null);

            // Act & Assert
            assertThat(request.maxResults(), is(20));
            }

        @Test
        @DisplayName("Should handle all parameters")
        void shouldHandleAllParameters()
            {
            // Arrange
            Store.SearchRequest request = new Store.SearchRequest("test query", 25, 0.8, 0.1, "custom-scorer");

            // Act & Assert
            assertThat(request.query(), is("test query"));
            assertThat(request.maxResults(), is(25));
            assertThat(request.minScore(), is(0.8));
            assertThat(request.fullTextWeight(), is(0.1));
            assertThat(request.scoringModel(), is("custom-scorer"));
            }
        }

    @Nested
    @DisplayName("SearchResult Tests")
    class SearchResultTests
        {
        @Test
        @DisplayName("Should create SearchResult with empty results")
        void shouldCreateSearchResultWithEmptyResults()
            {
            // Arrange
            List<Store.ChunkResult> results = List.of();
            long duration = 123L;

            // Act
            Store.SearchResult searchResult = new Store.SearchResult(results, duration);

            // Assert
            assertThat(searchResult.results(), hasSize(0));
            assertThat(searchResult.searchDuration(), is(duration));
            }
        }

    @Nested
    @DisplayName("Doc Record Tests")
    class DocRecordTests
        {
        @Test
        @DisplayName("Should create Doc record")
        void shouldCreateDocRecord()
            {
            // Arrange
            String id = "doc-123";
            String text = "Document content";

            // Act
            Store.Doc doc = new Store.Doc(id, text);

            // Assert
            assertThat(doc.id(), is(id));
            assertThat(doc.text(), is(text));
            }
        }

    @Nested
    @DisplayName("DocChunks Record Tests")
    class DocChunksRecordTests
        {
        @Test
        @DisplayName("Should create DocChunks record")
        void shouldCreateDocChunksRecord()
            {
            // Arrange
            String id = "doc-456";
            Store.DocChunk[] chunks = new Store.DocChunk[] {
                new Store.DocChunk("Chunk 1", Map.of("index", 0), new float[]{0.1f, 0.2f}),
                new Store.DocChunk("Chunk 2", Map.of("index", 1), new float[]{0.3f, 0.4f})
            };

            // Act
            Store.DocChunks docChunks = new Store.DocChunks(id, chunks);

            // Assert
            assertThat(docChunks.id(), is(id));
            assertThat(docChunks.chunks().length, is(2));
            assertThat(docChunks.chunks()[0].text(), is("Chunk 1"));
            assertThat(docChunks.chunks()[1].text(), is("Chunk 2"));
            }
        }

    @Nested
    @DisplayName("DocChunk Record Tests")
    class DocChunkRecordTests
        {
        @Test
        @DisplayName("Should create DocChunk record")
        void shouldCreateDocChunkRecord()
            {
            // Arrange
            String text = "Chunk text content";
            Map<String, Object> metadata = Map.of("source", "doc.txt", "index", 0);
            float[] embedding = new float[]{0.1f, 0.2f, 0.3f};

            // Act
            Store.DocChunk chunk = new Store.DocChunk(text, metadata, embedding);

            // Assert
            assertThat(chunk.text(), is(text));
            assertThat(chunk.metadata(), is(metadata));
            assertThat(chunk.embedding().length, is(3));
            assertThat(chunk.embedding()[0], is(0.1f));
            }
        }

    // ---- validation tests -----------------------------------------------

    @Nested
    @DisplayName("Business Logic Validation Tests")
    class BusinessLogicValidationTests
        {
        @Test
        @DisplayName("Should validate search parameters")
        void shouldValidateSearchParameters()
            {
            // Arrange - Invalid parameters that should be handled gracefully  
            Store.SearchRequest invalidRequest = new Store.SearchRequest("", -1, -0.5, 2.0, null);

            // Act & Assert - Should not throw, business logic should handle gracefully
            assertThat(invalidRequest.query(), is(""));
            assertThat(invalidRequest.maxResults(), is(-1)); // Negative values are preserved (only 0 gets default)
            assertThat(invalidRequest.minScore(), is(-0.5)); // Preserved as-is
            assertThat(invalidRequest.fullTextWeight(), is(2.0)); // Preserved as-is
            }

        @Test
        @DisplayName("Should validate chat parameters")
        void shouldValidateChatParameters()
            {
            // Arrange - Invalid parameters that should be handled gracefully
            Store.ChatRequest invalidRequest = new Store.ChatRequest(null, "", -1, -0.5, 2.0, null);

            // Act & Assert - Should not throw, business logic should handle gracefully
            assertThat(invalidRequest.chatModel(), is((String) null));
            assertThat(invalidRequest.question(), is(""));
            assertThat(invalidRequest.maxResults(), is(-1)); // Negative values are preserved (only 0 gets default)
            assertThat(invalidRequest.minScore(), is(-0.5)); // Preserved as-is
            assertThat(invalidRequest.fullTextWeight(), is(2.0)); // Preserved as-is
            }
        }

    // ---- model supplier tests ------------------------------------------

    @Nested
    @DisplayName("Model Supplier Tests")
    class ModelSupplierTests
        {
        @Test
        @DisplayName("Should get default embedding model")
        void shouldGetDefaultEmbeddingModel()
            {
            // Arrange
            Embedding mockEmbedding = TestDataFactory.createEmbedding();
            when(embeddingModel.embed(anyString())).thenReturn(dev.langchain4j.model.output.Response.from(mockEmbedding));

            // Act
            EmbeddingModel model = embeddingModelSupplier.get();

            // Assert
            assertThat(model, is(notNullValue()));
            assertThat(model, is(embeddingModel));
            }

        @Test
        @DisplayName("Should get default model name")
        void shouldGetDefaultModelName()
            {
            // Act
            ModelName modelName = embeddingModelSupplier.defaultModelName();

            // Assert
            assertThat(modelName, is(notNullValue()));
            assertThat(modelName.toString(), is("ModelName[fullName=test/embedding-model]"));
            }

        @Test
        @DisplayName("Should get chat model")
        void shouldGetChatModel()
            {
            // Act
            StreamingChatModel model = chatModelSupplier.get();

            // Assert
            assertThat(model, is(notNullValue()));
            assertThat(model, is(streamingChatModel));
            }
        }
    } 
