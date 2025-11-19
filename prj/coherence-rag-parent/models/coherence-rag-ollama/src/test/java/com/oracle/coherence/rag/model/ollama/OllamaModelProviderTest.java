/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.model.ollama;

import com.oracle.coherence.rag.config.ConfigRepository;
import com.oracle.coherence.rag.internal.json.JsonbProvider;
import com.tangosol.net.cache.WrapperNamedCache;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;

import java.util.HashMap;
import org.eclipse.microprofile.config.Config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link OllamaModelProvider} class.
 * <p/>
 * This test class validates the Ollama model provider functionality including
 * configuration management, model creation for all supported types (embedding,
 * chat, streaming), and URL configuration scenarios.
 * 
 * @author Aleks Seovic  2025.07.04
 * @since 25.09
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OllamaModelProvider")
class OllamaModelProviderTest
    {
    @Mock
    private Config mockConfig;

    private OllamaModelProvider provider;

    @BeforeEach
    void setUp()
        {
        ConfigRepository jsonConfig = new ConfigRepository(new WrapperNamedCache<>(new HashMap<>(), "jsonConfig"), new JsonbProvider());
        provider = new OllamaModelProvider(mockConfig, jsonConfig);
        }

    @Test
    @DisplayName("should create embedding model with default base URL")
    void shouldCreateEmbeddingModelWithDefaultBaseUrl()
        {
        when(mockConfig.getOptionalValue("ollama.base.url", String.class))
            .thenReturn(Optional.empty());

        EmbeddingModel embeddingModel = provider.getEmbeddingModel("nomic-embed-text");

        assertThat(embeddingModel, is(notNullValue()));
        assertThat(embeddingModel, is(instanceOf(dev.langchain4j.model.ollama.OllamaEmbeddingModel.class)));
        }

    @Test
    @DisplayName("should create embedding model with custom base URL")
    void shouldCreateEmbeddingModelWithCustomBaseUrl()
        {
        String customBaseUrl = "http://custom-ollama:11434";
        when(mockConfig.getOptionalValue("ollama.base.url", String.class))
            .thenReturn(Optional.of(customBaseUrl));

        EmbeddingModel embeddingModel = provider.getEmbeddingModel("mxbai-embed-large");

        assertThat(embeddingModel, is(notNullValue()));
        assertThat(embeddingModel, is(instanceOf(dev.langchain4j.model.ollama.OllamaEmbeddingModel.class)));
        }

    @Test
    @DisplayName("should create chat model with default base URL")
    void shouldCreateChatModelWithDefaultBaseUrl()
        {
        when(mockConfig.getOptionalValue("ollama.base.url", String.class))
            .thenReturn(Optional.empty());

        ChatModel chatModel = provider.getChatModel("llama3.1:8b");

        assertThat(chatModel, is(notNullValue()));
        assertThat(chatModel, is(instanceOf(dev.langchain4j.model.ollama.OllamaChatModel.class)));
        }

    @Test
    @DisplayName("should create chat model with custom base URL")
    void shouldCreateChatModelWithCustomBaseUrl()
        {
        String customBaseUrl = "http://custom-ollama:11434";
        when(mockConfig.getOptionalValue("ollama.base.url", String.class))
            .thenReturn(Optional.of(customBaseUrl));

        ChatModel chatModel = provider.getChatModel("mistral:latest");

        assertThat(chatModel, is(notNullValue()));
        assertThat(chatModel, is(instanceOf(dev.langchain4j.model.ollama.OllamaChatModel.class)));
        }

    @Test
    @DisplayName("should create streaming chat model with default base URL")
    void shouldCreateStreamingChatModelWithDefaultBaseUrl()
        {
        when(mockConfig.getOptionalValue("ollama.base.url", String.class))
            .thenReturn(Optional.empty());

        StreamingChatModel streamingChatModel = provider.getStreamingChatModel("llama3.1:8b");

        assertThat(streamingChatModel, is(notNullValue()));
        assertThat(streamingChatModel, is(instanceOf(dev.langchain4j.model.ollama.OllamaStreamingChatModel.class)));
        }

    @Test
    @DisplayName("should create streaming chat model with custom base URL")
    void shouldCreateStreamingChatModelWithCustomBaseUrl()
        {
        String customBaseUrl = "http://custom-ollama:11434";
        when(mockConfig.getOptionalValue("ollama.base.url", String.class))
            .thenReturn(Optional.of(customBaseUrl));

        StreamingChatModel streamingChatModel = provider.getStreamingChatModel("codellama:latest");

        assertThat(streamingChatModel, is(notNullValue()));
        assertThat(streamingChatModel, is(instanceOf(dev.langchain4j.model.ollama.OllamaStreamingChatModel.class)));
        }

    @Test
    @DisplayName("should handle various embedding model names")
    void shouldHandleVariousEmbeddingModelNames()
        {
        when(mockConfig.getOptionalValue("ollama.base.url", String.class))
            .thenReturn(Optional.empty());

        String[] embeddingModels = {
            "nomic-embed-text",
            "mxbai-embed-large",
            "bge-large-en",
            "custom-embedding-model"
        };

        for (String modelName : embeddingModels)
            {
            EmbeddingModel embeddingModel = provider.getEmbeddingModel(modelName);
            assertThat("Embedding model should be created for: " + modelName, 
                embeddingModel, is(notNullValue()));
            }
        }

    @Test
    @DisplayName("should handle various chat model names")
    void shouldHandleVariousChatModelNames()
        {
        when(mockConfig.getOptionalValue("ollama.base.url", String.class))
            .thenReturn(Optional.empty());

        String[] chatModels = {
            "llama3.1:8b",
            "llama3.1:70b",
            "mistral:latest",
            "codellama:7b",
            "alpaca:latest",
            "vicuna:13b",
            "custom-chat-model"
        };

        for (String modelName : chatModels)
            {
            ChatModel chatModel = provider.getChatModel(modelName);
            assertThat("Chat model should be created for: " + modelName, 
                chatModel, is(notNullValue()));
            
            StreamingChatModel streamingModel = provider.getStreamingChatModel(modelName);
            assertThat("Streaming chat model should be created for: " + modelName, 
                streamingModel, is(notNullValue()));
            }
        }

    @Test
    @DisplayName("should throw IllegalArgumentException for null and empty model names")
    void shouldThrowIllegalArgumentExceptionForNullAndEmptyModelNames()
        {
        // Test null model names
        IllegalArgumentException exception1 = assertThrows(IllegalArgumentException.class,
            () -> provider.getEmbeddingModel(null));
        assertThat(exception1.getMessage(), containsString("Model name cannot be null or empty"));

        IllegalArgumentException exception2 = assertThrows(IllegalArgumentException.class,
            () -> provider.getChatModel(null));
        assertThat(exception2.getMessage(), containsString("Model name cannot be null or empty"));

        IllegalArgumentException exception3 = assertThrows(IllegalArgumentException.class,
            () -> provider.getStreamingChatModel(null));
        assertThat(exception3.getMessage(), containsString("Model name cannot be null or empty"));

        // Test empty model names
        IllegalArgumentException exception4 = assertThrows(IllegalArgumentException.class,
            () -> provider.getEmbeddingModel(""));
        assertThat(exception4.getMessage(), containsString("Model name cannot be null or empty"));

        IllegalArgumentException exception5 = assertThrows(IllegalArgumentException.class,
            () -> provider.getChatModel(""));
        assertThat(exception5.getMessage(), containsString("Model name cannot be null or empty"));

        IllegalArgumentException exception6 = assertThrows(IllegalArgumentException.class,
            () -> provider.getStreamingChatModel(""));
        assertThat(exception6.getMessage(), containsString("Model name cannot be null or empty"));

        // Test whitespace-only model names
        IllegalArgumentException exception7 = assertThrows(IllegalArgumentException.class,
            () -> provider.getEmbeddingModel("   "));
        assertThat(exception7.getMessage(), containsString("Model name cannot be null or empty"));

        IllegalArgumentException exception8 = assertThrows(IllegalArgumentException.class,
            () -> provider.getChatModel("   "));
        assertThat(exception8.getMessage(), containsString("Model name cannot be null or empty"));

        IllegalArgumentException exception9 = assertThrows(IllegalArgumentException.class,
            () -> provider.getStreamingChatModel("   "));
        assertThat(exception9.getMessage(), containsString("Model name cannot be null or empty"));
        }

    @Test
    @DisplayName("should use default base URL when not configured")
    void shouldUseDefaultBaseUrlWhenNotConfigured()
        {
        when(mockConfig.getOptionalValue("ollama.base.url", String.class))
            .thenReturn(Optional.empty());

        // Create a spy to verify the actual base URL being used
        OllamaModelProvider spyProvider = spy(provider);

        // Create all model types to ensure default URL is used
        EmbeddingModel embeddingModel = spyProvider.getEmbeddingModel("nomic-embed-text");
        ChatModel chatModel = spyProvider.getChatModel("llama3.1:8b");
        StreamingChatModel streamingModel = spyProvider.getStreamingChatModel("llama3.1:8b");

        assertThat(embeddingModel, is(notNullValue()));
        assertThat(chatModel, is(notNullValue()));
        assertThat(streamingModel, is(notNullValue()));
        
        // Verify that the base URL method returns the expected default value
        String actualBaseUrl = spyProvider.ollamaBaseUrl();
        assertThat("Default base URL should be used when not configured", 
            actualBaseUrl, is("http://localhost:11434"));
        
        // Verify configuration methods were called
        verify(mockConfig, atLeast(3)).getOptionalValue("ollama.base.url", String.class);
        }

    @Test
    @DisplayName("should handle multiple model creation calls")
    void shouldHandleMultipleModelCreationCalls()
        {
        when(mockConfig.getOptionalValue("ollama.base.url", String.class))
            .thenReturn(Optional.empty());

        // Create multiple models of each type
        EmbeddingModel embedding1 = provider.getEmbeddingModel("nomic-embed-text");
        EmbeddingModel embedding2 = provider.getEmbeddingModel("mxbai-embed-large");
        ChatModel chat1 = provider.getChatModel("llama3.1:8b");
        ChatModel chat2 = provider.getChatModel("mistral:latest");
        StreamingChatModel streaming1 = provider.getStreamingChatModel("llama3.1:8b");
        StreamingChatModel streaming2 = provider.getStreamingChatModel("codellama:7b");

        // All should be created successfully and be different instances
        assertThat(embedding1, is(notNullValue()));
        assertThat(embedding2, is(notNullValue()));
        assertThat(chat1, is(notNullValue()));
        assertThat(chat2, is(notNullValue()));
        assertThat(streaming1, is(notNullValue()));
        assertThat(streaming2, is(notNullValue()));
        
        // Models should be different instances
        assertThat(embedding1, is(not(sameInstance(embedding2))));
        assertThat(chat1, is(not(sameInstance(chat2))));
        assertThat(streaming1, is(not(sameInstance(streaming2))));
        }

    @Test
    @DisplayName("should create models with custom base URL configuration")
    void shouldCreateModelsWithCustomBaseUrlConfiguration()
        {
        String customBaseUrl = "http://remote-ollama:11434";
        when(mockConfig.getOptionalValue("ollama.base.url", String.class))
            .thenReturn(Optional.of(customBaseUrl));

        // Create all model types with custom base URL
        EmbeddingModel embeddingModel = provider.getEmbeddingModel("nomic-embed-text");
        ChatModel chatModel = provider.getChatModel("llama3.1:8b");
        StreamingChatModel streamingModel = provider.getStreamingChatModel("llama3.1:8b");

        assertThat(embeddingModel, is(notNullValue()));
        assertThat(chatModel, is(notNullValue()));
        assertThat(streamingModel, is(notNullValue()));
        }

    @Test
    @DisplayName("should handle different URL formats")
    void shouldHandleDifferentUrlFormats()
        {
        String[] urls = {
            "http://localhost:11434",
            "http://127.0.0.1:11434",
            "https://ollama.example.com:11434",
            "http://ollama-server:11434",
            "https://remote-ollama.company.com:8080"
        };

        for (String url : urls)
            {
            when(mockConfig.getOptionalValue("ollama.base.url", String.class))
                .thenReturn(Optional.of(url));

            EmbeddingModel embeddingModel = provider.getEmbeddingModel("nomic-embed-text");
            ChatModel chatModel = provider.getChatModel("llama3.1:8b");
            StreamingChatModel streamingModel = provider.getStreamingChatModel("llama3.1:8b");

            assertThat("Should create embedding model with URL: " + url, 
                embeddingModel, is(notNullValue()));
            assertThat("Should create chat model with URL: " + url, 
                chatModel, is(notNullValue()));
            assertThat("Should create streaming model with URL: " + url, 
                streamingModel, is(notNullValue()));
            }
        }

    @Test
    @DisplayName("should handle configuration access for all model types")
    void shouldHandleConfigurationAccessForAllModelTypes()
        {
        when(mockConfig.getOptionalValue("ollama.base.url", String.class))
            .thenReturn(Optional.empty());

        // Create one of each model type
        provider.getEmbeddingModel("nomic-embed-text");
        provider.getChatModel("llama3.1:8b");
        provider.getStreamingChatModel("llama3.1:8b");

        // Verify configuration was accessed for each model creation
        verify(mockConfig, times(3)).getOptionalValue("ollama.base.url", String.class);
        }

    @Test
    @DisplayName("should handle model names with version tags")
    void shouldHandleModelNamesWithVersionTags()
        {
        when(mockConfig.getOptionalValue("ollama.base.url", String.class))
            .thenReturn(Optional.empty());

        String[] modelNamesWithTags = {
            "llama3.1:8b",
            "llama3.1:70b",
            "mistral:7b-instruct",
            "codellama:7b-python",
            "alpaca:latest",
            "vicuna:13b-v1.5"
        };

        for (String modelName : modelNamesWithTags)
            {
            ChatModel chatModel = provider.getChatModel(modelName);
            StreamingChatModel streamingModel = provider.getStreamingChatModel(modelName);

            assertThat("Should create chat model for: " + modelName, 
                chatModel, is(notNullValue()));
            assertThat("Should create streaming model for: " + modelName, 
                streamingModel, is(notNullValue()));
            }
        }
    } 
