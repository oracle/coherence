/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.model.deepseek;

import com.oracle.coherence.rag.config.ConfigRepository;
import com.oracle.coherence.rag.internal.json.JsonbProvider;
import com.tangosol.net.cache.WrapperNamedCache;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;

import io.helidon.config.ConfigException;

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
 * Unit tests for {@link DeepSeekModelProvider} class.
 * <p/>
 * This test class validates the DeepSeek model provider functionality including
 * configuration management, model creation, and error handling scenarios.
 * Tests focus on the provider's behavior with different configuration states
 * and its ability to create appropriate model instances.
 * 
 * @author Aleks Seovic  2025.07.04
 * @since 25.09
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DeepSeekModelProvider")
class DeepSeekModelProviderTest
    {
    @Mock
    private Config mockConfig;

    private DeepSeekModelProvider provider;

    @BeforeEach
    void setUp()
        {
        ConfigRepository jsonConfig = new ConfigRepository(new WrapperNamedCache<>(new HashMap<>(), "jsonConfig"), new JsonbProvider());
        provider = new DeepSeekModelProvider(mockConfig, jsonConfig);
        }

    @Test
    @DisplayName("should return null for embedding models")
    void shouldReturnNullForEmbeddingModels()
        {
        EmbeddingModel result1 = provider.getEmbeddingModel("deepseek-embed");
        EmbeddingModel result2 = provider.getEmbeddingModel("any-model-name");
        EmbeddingModel result3 = provider.getEmbeddingModel(null);

        assertThat(result1, is(nullValue()));
        assertThat(result2, is(nullValue()));
        assertThat(result3, is(nullValue()));
        }

    @Test
    @DisplayName("should create chat model with default configuration")
    void shouldCreateChatModelWithDefaultConfiguration()
        {
        String testApiKey = "test-api-key";
        when(mockConfig.getOptionalValue("deepseek.api.key", String.class))
            .thenReturn(Optional.of(testApiKey));
        when(mockConfig.getOptionalValue("deepseek.base.url", String.class))
            .thenReturn(Optional.empty());

        ChatModel chatModel = provider.getChatModel("deepseek-chat");

        assertThat(chatModel, is(notNullValue()));
        assertThat(chatModel, is(instanceOf(dev.langchain4j.model.openai.OpenAiChatModel.class)));
        }

    @Test
    @DisplayName("should create chat model with custom base URL")
    void shouldCreateChatModelWithCustomBaseUrl()
        {
        String testApiKey = "test-api-key";
        String customBaseUrl = "https://custom.deepseek.endpoint/v1";
        
        when(mockConfig.getOptionalValue("deepseek.api.key", String.class))
            .thenReturn(Optional.of(testApiKey));
        when(mockConfig.getOptionalValue("deepseek.base.url", String.class))
            .thenReturn(Optional.of(customBaseUrl));

        ChatModel chatModel = provider.getChatModel("deepseek-chat");

        assertThat(chatModel, is(notNullValue()));
        assertThat(chatModel, is(instanceOf(dev.langchain4j.model.openai.OpenAiChatModel.class)));
        }

    @Test
    @DisplayName("should create streaming chat model with default configuration")
    void shouldCreateStreamingChatModelWithDefaultConfiguration()
        {
        String testApiKey = "test-api-key";
        when(mockConfig.getOptionalValue("deepseek.api.key", String.class))
            .thenReturn(Optional.of(testApiKey));
        when(mockConfig.getOptionalValue("deepseek.base.url", String.class))
            .thenReturn(Optional.empty());

        StreamingChatModel streamingChatModel = provider.getStreamingChatModel("deepseek-chat");

        assertThat(streamingChatModel, is(notNullValue()));
        assertThat(streamingChatModel, is(instanceOf(dev.langchain4j.model.openai.OpenAiStreamingChatModel.class)));
        }

    @Test
    @DisplayName("should create streaming chat model with custom base URL")
    void shouldCreateStreamingChatModelWithCustomBaseUrl()
        {
        String testApiKey = "test-api-key";
        String customBaseUrl = "https://custom.deepseek.endpoint/v1";
        
        when(mockConfig.getOptionalValue("deepseek.api.key", String.class))
            .thenReturn(Optional.of(testApiKey));
        when(mockConfig.getOptionalValue("deepseek.base.url", String.class))
            .thenReturn(Optional.of(customBaseUrl));

        StreamingChatModel streamingChatModel = provider.getStreamingChatModel("deepseek-chat");

        assertThat(streamingChatModel, is(notNullValue()));
        assertThat(streamingChatModel, is(instanceOf(dev.langchain4j.model.openai.OpenAiStreamingChatModel.class)));
        }

    @Test
    @DisplayName("should throw ConfigException when API key is missing for chat model")
    void shouldThrowConfigExceptionWhenApiKeyMissingForChatModel()
        {
        when(mockConfig.getOptionalValue("deepseek.api.key", String.class))
            .thenReturn(Optional.empty());

        ConfigException exception = assertThrows(ConfigException.class,
            () -> provider.getChatModel("deepseek-chat"));

        assertThat(exception.getMessage(), containsString("DeepSeek API key is not set"));
        assertThat(exception.getMessage(), containsString("deepseek.api.key"));
        }

    @Test
    @DisplayName("should throw ConfigException when API key is missing for streaming chat model")
    void shouldThrowConfigExceptionWhenApiKeyMissingForStreamingChatModel()
        {
        when(mockConfig.getOptionalValue("deepseek.api.key", String.class))
            .thenReturn(Optional.empty());

        ConfigException exception = assertThrows(ConfigException.class,
            () -> provider.getStreamingChatModel("deepseek-chat"));

        assertThat(exception.getMessage(), containsString("DeepSeek API key is not set"));
        assertThat(exception.getMessage(), containsString("deepseek.api.key"));
        }

    @Test
    @DisplayName("should handle various model names for chat models")
    void shouldHandleVariousModelNamesForChatModels()
        {
        String testApiKey = "test-api-key";
        when(mockConfig.getOptionalValue("deepseek.api.key", String.class))
            .thenReturn(Optional.of(testApiKey));
        when(mockConfig.getOptionalValue("deepseek.base.url", String.class))
            .thenReturn(Optional.empty());

        String[] modelNames = {
            "deepseek-chat",
            "deepseek-coder",
            "deepseek-v2",
            "custom-model-name"
        };

        for (String modelName : modelNames)
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
            () -> provider.getChatModel(null));
        assertThat(exception1.getMessage(), containsString("Model name cannot be null or empty"));

        IllegalArgumentException exception2 = assertThrows(IllegalArgumentException.class,
            () -> provider.getStreamingChatModel(null));
        assertThat(exception2.getMessage(), containsString("Model name cannot be null or empty"));

        // Test empty model names
        IllegalArgumentException exception3 = assertThrows(IllegalArgumentException.class,
            () -> provider.getChatModel(""));
        assertThat(exception3.getMessage(), containsString("Model name cannot be null or empty"));

        IllegalArgumentException exception4 = assertThrows(IllegalArgumentException.class,
            () -> provider.getStreamingChatModel(""));
        assertThat(exception4.getMessage(), containsString("Model name cannot be null or empty"));

        // Test whitespace-only model names
        IllegalArgumentException exception5 = assertThrows(IllegalArgumentException.class,
            () -> provider.getChatModel("   "));
        assertThat(exception5.getMessage(), containsString("Model name cannot be null or empty"));

        IllegalArgumentException exception6 = assertThrows(IllegalArgumentException.class,
            () -> provider.getStreamingChatModel("   "));
        assertThat(exception6.getMessage(), containsString("Model name cannot be null or empty"));
        }

    @Test
    @DisplayName("should use default base URL when not configured")
    void shouldUseDefaultBaseUrlWhenNotConfigured()
        {
        String testApiKey = "test-api-key";
        when(mockConfig.getOptionalValue("deepseek.api.key", String.class))
            .thenReturn(Optional.of(testApiKey));
        when(mockConfig.getOptionalValue("deepseek.base.url", String.class))
            .thenReturn(Optional.empty());

        // Create a spy to verify the actual base URL being used
        DeepSeekModelProvider spyProvider = spy(provider);

        // Create models to ensure default URL is used
        ChatModel chatModel = spyProvider.getChatModel("deepseek-chat");
        StreamingChatModel streamingModel = spyProvider.getStreamingChatModel("deepseek-chat");

        assertThat(chatModel, is(notNullValue()));
        assertThat(streamingModel, is(notNullValue()));
        
        // Verify that the base URL method returns the expected default value
        String actualBaseUrl = spyProvider.deepSeekBaseUrl();
        assertThat("Default base URL should be used when not configured", 
            actualBaseUrl, is("https://api.deepseek.com/v1"));
        
        // Verify configuration methods were called
        verify(mockConfig, atLeastOnce()).getOptionalValue("deepseek.base.url", String.class);
        }

    @Test
    @DisplayName("should throw ConfigException for whitespace-only API key")
    void shouldThrowConfigExceptionForWhitespaceOnlyApiKey()
        {
        when(mockConfig.getOptionalValue("deepseek.api.key", String.class))
            .thenReturn(Optional.of("   "));

        // Should throw ConfigException for whitespace-only API key
        ConfigException exception1 = assertThrows(ConfigException.class,
            () -> provider.getChatModel("deepseek-chat"));
        assertThat(exception1.getMessage(), containsString("DeepSeek API key cannot be empty or whitespace-only"));

        ConfigException exception2 = assertThrows(ConfigException.class,
            () -> provider.getStreamingChatModel("deepseek-chat"));
        assertThat(exception2.getMessage(), containsString("DeepSeek API key cannot be empty or whitespace-only"));
        }

    @Test
    @DisplayName("should handle multiple model creation calls")
    void shouldHandleMultipleModelCreationCalls()
        {
        String testApiKey = "test-api-key";
        when(mockConfig.getOptionalValue("deepseek.api.key", String.class))
            .thenReturn(Optional.of(testApiKey));
        when(mockConfig.getOptionalValue("deepseek.base.url", String.class))
            .thenReturn(Optional.empty());

        // Create multiple models
        ChatModel chatModel1 = provider.getChatModel("model1");
        ChatModel chatModel2 = provider.getChatModel("model2");
        StreamingChatModel streamingModel1 = provider.getStreamingChatModel("model1");
        StreamingChatModel streamingModel2 = provider.getStreamingChatModel("model2");

        // All should be created successfully and be different instances
        assertThat(chatModel1, is(notNullValue()));
        assertThat(chatModel2, is(notNullValue()));
        assertThat(streamingModel1, is(notNullValue()));
        assertThat(streamingModel2, is(notNullValue()));
        
        // Models should be different instances
        assertThat(chatModel1, is(not(sameInstance(chatModel2))));
        assertThat(streamingModel1, is(not(sameInstance(streamingModel2))));
        }
    } 
