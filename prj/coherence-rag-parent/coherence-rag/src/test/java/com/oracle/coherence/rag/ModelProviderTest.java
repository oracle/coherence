/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.scoring.ScoringModel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Comprehensive unit tests for the ModelProvider interface and its implementations.
 * <p/>
 * This test class validates the model provider functionality including embedding
 * model creation, chat model provisioning, scoring model support, and error handling
 * for various scenarios.
 *
 * @author Aleks Seovic  2025.07.04
 * @since 25.09
 */
@DisplayName("ModelProvider Tests")
class ModelProviderTest
    {
    @Mock
    private EmbeddingModel mockEmbeddingModel;

    @Mock
    private ChatModel mockChatModel;

    @Mock
    private StreamingChatModel mockStreamingChatModel;

    @Mock
    private ScoringModel mockScoringModel;

    private TestModelProvider testProvider;

    @BeforeEach
    void setUp()
        {
        MockitoAnnotations.openMocks(this);
        testProvider = new TestModelProvider();
        }

    // ---- embedding model tests ------------------------------------------

    @Nested
    @DisplayName("Embedding Model Tests")
    class EmbeddingModelTests
        {
        @Test
        @DisplayName("Should return embedding model for valid name")
        void shouldReturnEmbeddingModelForValidName()
            {
            // Act
            EmbeddingModel model = testProvider.getEmbeddingModel("test-embedding-model");

            // Assert
            assertThat(model, is(notNullValue()));
            }

        @Test
        @DisplayName("Should throw exception for null embedding model name")
        void shouldThrowExceptionForNullEmbeddingModelName()
            {
            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> {
                testProvider.getEmbeddingModel(null);
            });
            }

        @Test
        @DisplayName("Should throw exception for empty embedding model name")
        void shouldThrowExceptionForEmptyEmbeddingModelName()
            {
            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> {
                testProvider.getEmbeddingModel("");
            });
            }

        @Test
        @DisplayName("Should throw exception for unsupported embedding model")
        void shouldThrowExceptionForUnsupportedEmbeddingModel()
            {
            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> {
                testProvider.getEmbeddingModel("unsupported-model");
            });
            }
        }

    // ---- chat model tests -----------------------------------------------

    @Nested
    @DisplayName("Chat Model Tests")
    class ChatModelTests
        {
        @Test
        @DisplayName("Should return chat model for valid name")
        void shouldReturnChatModelForValidName()
            {
            // Act
            ChatModel model = testProvider.getChatModel("test-chat-model");

            // Assert
            assertThat(model, is(notNullValue()));
            }

        @Test
        @DisplayName("Should throw exception for null chat model name")
        void shouldThrowExceptionForNullChatModelName()
            {
            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> {
                testProvider.getChatModel(null);
            });
            }

        @Test
        @DisplayName("Should throw exception for unsupported chat model")
        void shouldThrowExceptionForUnsupportedChatModel()
            {
            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> {
                testProvider.getChatModel("unsupported-chat-model");
            });
            }
        }

    // ---- streaming chat model tests -------------------------------------

    @Nested
    @DisplayName("Streaming Chat Model Tests")
    class StreamingChatModelTests
        {
        @Test
        @DisplayName("Should return streaming chat model for valid name")
        void shouldReturnStreamingChatModelForValidName()
            {
            // Act
            StreamingChatModel model = testProvider.getStreamingChatModel("test-streaming-model");

            // Assert
            assertThat(model, is(notNullValue()));
            }

        @Test
        @DisplayName("Should throw exception for null streaming model name")
        void shouldThrowExceptionForNullStreamingModelName()
            {
            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> {
                testProvider.getStreamingChatModel(null);
            });
            }

        @Test
        @DisplayName("Should throw exception for unsupported streaming model")
        void shouldThrowExceptionForUnsupportedStreamingModel()
            {
            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> {
                testProvider.getStreamingChatModel("unsupported-streaming-model");
            });
            }
        }

    // ---- scoring model tests --------------------------------------------

    @Nested
    @DisplayName("Scoring Model Tests")
    class ScoringModelTests
        {
        @Test
        @DisplayName("Should return null for scoring model by default")
        void shouldReturnNullForScoringModelByDefault()
            {
            // Create a basic implementation that doesn't override getScoringModel
            ModelProvider basicProvider = new BasicModelProvider();

            // Act
            ScoringModel model = basicProvider.getScoringModel("test-scoring-model");

            // Assert
            assertThat(model, is(nullValue()));
            }

        @Test
        @DisplayName("Should return scoring model when supported")
        void shouldReturnScoringModelWhenSupported()
            {
            // Act
            ScoringModel model = testProvider.getScoringModel("test-scoring-model");

            // Assert
            assertThat(model, is(notNullValue()));
            }

        @Test
        @DisplayName("Should handle unsupported scoring model gracefully")
        void shouldHandleUnsupportedScoringModelGracefully()
            {
            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> {
                testProvider.getScoringModel("unsupported-scoring-model");
            });
            }
        }

    // ---- test implementations -------------------------------------------

    /**
     * Test implementation of ModelProvider for testing purposes.
     */
    private class TestModelProvider implements ModelProvider
        {
        @Override
        public EmbeddingModel getEmbeddingModel(String sName)
            {
            if (sName == null || sName.trim().isEmpty())
                {
                throw new IllegalArgumentException("Model name cannot be null or empty");
                }
            if ("test-embedding-model".equals(sName))
                {
                return mockEmbeddingModel;
                }
            throw new IllegalArgumentException("Unsupported embedding model: " + sName);
            }

        @Override
        public ChatModel getChatModel(String sName)
            {
            if (sName == null || sName.trim().isEmpty())
                {
                throw new IllegalArgumentException("Model name cannot be null or empty");
                }
            if ("test-chat-model".equals(sName))
                {
                return mockChatModel;
                }
            throw new IllegalArgumentException("Unsupported chat model: " + sName);
            }

        @Override
        public StreamingChatModel getStreamingChatModel(String sName)
            {
            if (sName == null || sName.trim().isEmpty())
                {
                throw new IllegalArgumentException("Model name cannot be null or empty");
                }
            if ("test-streaming-model".equals(sName))
                {
                return mockStreamingChatModel;
                }
            throw new IllegalArgumentException("Unsupported streaming model: " + sName);
            }

        @Override
        public ScoringModel getScoringModel(String sName)
            {
            if ("test-scoring-model".equals(sName))
                {
                return mockScoringModel;
                }
            throw new IllegalArgumentException("Unsupported scoring model: " + sName);
            }
        }

    /**
     * Basic implementation that uses default scoring model behavior.
     */
    private class BasicModelProvider implements ModelProvider
        {
        @Override
        public EmbeddingModel getEmbeddingModel(String sName)
            {
            return mockEmbeddingModel;
            }

        @Override
        public ChatModel getChatModel(String sName)
            {
            return mockChatModel;
            }

        @Override
        public StreamingChatModel getStreamingChatModel(String sName)
            {
            return mockStreamingChatModel;
            }
        }
    } 