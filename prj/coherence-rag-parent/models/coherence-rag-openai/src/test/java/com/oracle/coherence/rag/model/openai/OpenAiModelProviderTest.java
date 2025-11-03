/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.model.openai;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;

import io.helidon.config.ConfigException;

import org.eclipse.microprofile.config.Config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link OpenAiModelProvider} class.
 * <p/>
 * This test class validates the OpenAI model provider functionality including
 * configuration management, model creation for all supported types (embedding,
 * chat, streaming), and error handling scenarios.
 * 
 * @author Aleks Seovic  2025.07.04
 * @since 25.09
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OpenAiModelProvider")
class OpenAiModelProviderTest
    {
    @Mock
    private Config mockConfig;

    private OpenAiModelProvider provider;

    @BeforeEach
    void setUp() throws Exception
        {
        provider = new OpenAiModelProvider();
        
        // Inject mock config using reflection
        Field configField = OpenAiModelProvider.class.getDeclaredField("config");
        configField.setAccessible(true);
        configField.set(provider, mockConfig);
        }

    @Test
    @DisplayName("should create embedding model with default configuration")
    void shouldCreateEmbeddingModelWithDefaultConfiguration()
        {
        String testApiKey = "test-openai-api-key";
        when(mockConfig.getOptionalValue("openai.api.key", String.class))
            .thenReturn(Optional.of(testApiKey));
        when(mockConfig.getOptionalValue("openai.base.url", String.class))
            .thenReturn(Optional.empty());

        EmbeddingModel embeddingModel = provider.getEmbeddingModel("text-embedding-3-small");

        assertThat(embeddingModel, is(notNullValue()));
        assertThat(embeddingModel, is(instanceOf(dev.langchain4j.model.openai.OpenAiEmbeddingModel.class)));
        }

    @Test
    @DisplayName("should create embedding model with custom base URL")
    void shouldCreateEmbeddingModelWithCustomBaseUrl()
        {
        String testApiKey = "test-openai-api-key";
        String customBaseUrl = "https://custom.openai.endpoint/v1";
        
        when(mockConfig.getOptionalValue("openai.api.key", String.class))
            .thenReturn(Optional.of(testApiKey));
        when(mockConfig.getOptionalValue("openai.base.url", String.class))
            .thenReturn(Optional.of(customBaseUrl));

        EmbeddingModel embeddingModel = provider.getEmbeddingModel("text-embedding-3-large");

        assertThat(embeddingModel, is(notNullValue()));
        assertThat(embeddingModel, is(instanceOf(dev.langchain4j.model.openai.OpenAiEmbeddingModel.class)));
        }

    @Test
    @DisplayName("should create chat model with default configuration")
    void shouldCreateChatModelWithDefaultConfiguration()
        {
        String testApiKey = "test-openai-api-key";
        when(mockConfig.getOptionalValue("openai.api.key", String.class))
            .thenReturn(Optional.of(testApiKey));
        when(mockConfig.getOptionalValue("openai.base.url", String.class))
            .thenReturn(Optional.empty());

        ChatModel chatModel = provider.getChatModel("gpt-4");

        assertThat(chatModel, is(notNullValue()));
        assertThat(chatModel, is(instanceOf(dev.langchain4j.model.openai.OpenAiChatModel.class)));
        }

    @Test
    @DisplayName("should create streaming chat model with default configuration")
    void shouldCreateStreamingChatModelWithDefaultConfiguration()
        {
        String testApiKey = "test-openai-api-key";
        when(mockConfig.getOptionalValue("openai.api.key", String.class))
            .thenReturn(Optional.of(testApiKey));
        when(mockConfig.getOptionalValue("openai.base.url", String.class))
            .thenReturn(Optional.empty());

        StreamingChatModel streamingChatModel = provider.getStreamingChatModel("gpt-4");

        assertThat(streamingChatModel, is(notNullValue()));
        assertThat(streamingChatModel, is(instanceOf(dev.langchain4j.model.openai.OpenAiStreamingChatModel.class)));
        }

    @Test
    @DisplayName("should throw ConfigException when API key is missing for embedding model")
    void shouldThrowConfigExceptionWhenApiKeyMissingForEmbeddingModel()
        {
        when(mockConfig.getOptionalValue("openai.api.key", String.class))
            .thenReturn(Optional.empty());

        ConfigException exception = assertThrows(ConfigException.class,
            () -> provider.getEmbeddingModel("text-embedding-3-small"));

        assertThat(exception.getMessage(), containsString("OpenAI API key is not set"));
        assertThat(exception.getMessage(), containsString("openai.api.key"));
        }

    @Test
    @DisplayName("should throw ConfigException when API key is missing for chat model")
    void shouldThrowConfigExceptionWhenApiKeyMissingForChatModel()
        {
        when(mockConfig.getOptionalValue("openai.api.key", String.class))
            .thenReturn(Optional.empty());

        ConfigException exception = assertThrows(ConfigException.class,
            () -> provider.getChatModel("gpt-4"));

        assertThat(exception.getMessage(), containsString("OpenAI API key is not set"));
        assertThat(exception.getMessage(), containsString("openai.api.key"));
        }

    @Test
    @DisplayName("should throw ConfigException when API key is missing for streaming chat model")
    void shouldThrowConfigExceptionWhenApiKeyMissingForStreamingChatModel()
        {
        when(mockConfig.getOptionalValue("openai.api.key", String.class))
            .thenReturn(Optional.empty());

        ConfigException exception = assertThrows(ConfigException.class,
            () -> provider.getStreamingChatModel("gpt-4"));

        assertThat(exception.getMessage(), containsString("OpenAI API key is not set"));
        assertThat(exception.getMessage(), containsString("openai.api.key"));
        }

    @Test
    @DisplayName("should handle various embedding model names")
    void shouldHandleVariousEmbeddingModelNames()
        {
        String testApiKey = "test-openai-api-key";
        when(mockConfig.getOptionalValue("openai.api.key", String.class))
            .thenReturn(Optional.of(testApiKey));
        when(mockConfig.getOptionalValue("openai.base.url", String.class))
            .thenReturn(Optional.empty());

        String[] embeddingModels = {
            "text-embedding-3-small",
            "text-embedding-3-large",
            "text-embedding-ada-002",
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
        String testApiKey = "test-openai-api-key";
        when(mockConfig.getOptionalValue("openai.api.key", String.class))
            .thenReturn(Optional.of(testApiKey));
        when(mockConfig.getOptionalValue("openai.base.url", String.class))
            .thenReturn(Optional.empty());

        String[] chatModels = {
            "gpt-4",
            "gpt-4-turbo",
            "gpt-3.5-turbo",
            "gpt-3.5-turbo-16k",
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
        String testApiKey = "test-openai-api-key";
        when(mockConfig.getOptionalValue("openai.api.key", String.class))
            .thenReturn(Optional.of(testApiKey));
        when(mockConfig.getOptionalValue("openai.base.url", String.class))
            .thenReturn(Optional.empty());

        // Create a spy to verify the actual base URL being used
        OpenAiModelProvider spyProvider = spy(provider);

        // Create all model types to ensure default URL is used
        EmbeddingModel embeddingModel = spyProvider.getEmbeddingModel("text-embedding-3-small");
        ChatModel chatModel = spyProvider.getChatModel("gpt-4");
        StreamingChatModel streamingModel = spyProvider.getStreamingChatModel("gpt-4");

        assertThat(embeddingModel, is(notNullValue()));
        assertThat(chatModel, is(notNullValue()));
        assertThat(streamingModel, is(notNullValue()));
        
        // Verify that the base URL method returns the expected default value
        String actualBaseUrl = spyProvider.openAiBaseUrl();
        assertThat("Default base URL should be used when not configured", 
            actualBaseUrl, is("https://api.openai.com/v1"));
        
        // Verify configuration methods were called
        verify(mockConfig, atLeast(3)).getOptionalValue("openai.base.url", String.class);
        }

    @Test
    @DisplayName("should throw ConfigException for whitespace-only API key")
    void shouldThrowConfigExceptionForWhitespaceOnlyApiKey()
        {
        when(mockConfig.getOptionalValue("openai.api.key", String.class))
            .thenReturn(Optional.of("   "));

        // Should throw ConfigException for whitespace-only API key
        ConfigException exception1 = assertThrows(ConfigException.class,
            () -> provider.getEmbeddingModel("text-embedding-3-small"));
        assertThat(exception1.getMessage(), containsString("OpenAI API key cannot be empty or whitespace-only"));

        ConfigException exception2 = assertThrows(ConfigException.class,
            () -> provider.getChatModel("gpt-4"));
        assertThat(exception2.getMessage(), containsString("OpenAI API key cannot be empty or whitespace-only"));

        ConfigException exception3 = assertThrows(ConfigException.class,
            () -> provider.getStreamingChatModel("gpt-4"));
        assertThat(exception3.getMessage(), containsString("OpenAI API key cannot be empty or whitespace-only"));
        }

    @Test
    @DisplayName("should handle multiple model creation calls")
    void shouldHandleMultipleModelCreationCalls()
        {
        String testApiKey = "test-openai-api-key";
        when(mockConfig.getOptionalValue("openai.api.key", String.class))
            .thenReturn(Optional.of(testApiKey));
        when(mockConfig.getOptionalValue("openai.base.url", String.class))
            .thenReturn(Optional.empty());

        // Create multiple models of each type
        EmbeddingModel embedding1 = provider.getEmbeddingModel("text-embedding-3-small");
        EmbeddingModel embedding2 = provider.getEmbeddingModel("text-embedding-3-large");
        ChatModel chat1 = provider.getChatModel("gpt-4");
        ChatModel chat2 = provider.getChatModel("gpt-3.5-turbo");
        StreamingChatModel streaming1 = provider.getStreamingChatModel("gpt-4");
        StreamingChatModel streaming2 = provider.getStreamingChatModel("gpt-3.5-turbo");

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
        String testApiKey = "test-openai-api-key";
        String customBaseUrl = "https://custom.openai.endpoint/v1";
        
        when(mockConfig.getOptionalValue("openai.api.key", String.class))
            .thenReturn(Optional.of(testApiKey));
        when(mockConfig.getOptionalValue("openai.base.url", String.class))
            .thenReturn(Optional.of(customBaseUrl));

        // Create all model types with custom base URL
        EmbeddingModel embeddingModel = provider.getEmbeddingModel("text-embedding-3-small");
        ChatModel chatModel = provider.getChatModel("gpt-4");
        StreamingChatModel streamingModel = provider.getStreamingChatModel("gpt-4");

        assertThat(embeddingModel, is(notNullValue()));
        assertThat(chatModel, is(notNullValue()));
        assertThat(streamingModel, is(notNullValue()));
        }

    @Test
    @DisplayName("should handle configuration access for all model types")
    void shouldHandleConfigurationAccessForAllModelTypes()
        {
        String testApiKey = "test-openai-api-key";
        when(mockConfig.getOptionalValue("openai.api.key", String.class))
            .thenReturn(Optional.of(testApiKey));
        when(mockConfig.getOptionalValue("openai.base.url", String.class))
            .thenReturn(Optional.empty());

        // Create one of each model type
        provider.getEmbeddingModel("text-embedding-3-small");
        provider.getChatModel("gpt-4");
        provider.getStreamingChatModel("gpt-4");

        // Verify configuration was accessed for each model creation
        verify(mockConfig, atLeast(3)).getOptionalValue("openai.api.key", String.class);
        verify(mockConfig, atLeast(3)).getOptionalValue("openai.base.url", String.class);
        }
    } 
