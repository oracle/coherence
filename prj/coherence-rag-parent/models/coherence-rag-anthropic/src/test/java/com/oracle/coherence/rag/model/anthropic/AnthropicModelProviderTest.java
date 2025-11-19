/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.model.anthropic;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;

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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;

import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AnthropicModelProvider} class.
 * <p/>
 * This test class validates the Anthropic model provider functionality including
 * configuration management, model creation for all supported types
 * (chat and streaming), and URL configuration scenarios.
 * 
 * @author Aleks Seovic/Tim Middleton  2025.08.05
 * @since 25.09
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AnthropicModelProvider")
class AnthropicModelProviderTest
    {
    @Mock
    private Config mockConfig;

    private AnthropicModelProvider provider;

    @BeforeEach
    void setUp() throws Exception
        {
        provider = new AnthropicModelProvider();
        
        // Inject mock config using reflection
        Field configField = AnthropicModelProvider.class.getDeclaredField("config");
        configField.setAccessible(true);
        configField.set(provider, mockConfig);
        }

    @Test
    @DisplayName("should create chat model with default base URL")
    void shouldCreateChatModelWithDefaultBaseUrl()
        {
        when(mockConfig.getOptionalValue(ANTHROPIC_BASE_URL, String.class))
            .thenReturn(Optional.empty());
        when(mockConfig.getOptionalValue(ANTHROPIC_API_KEY, String.class))
                    .thenReturn(Optional.of(TEST_API_KEY));

        ChatModel chatModel = provider.getChatModel("claude-3-5-sonnet-latest");

        assertThat(chatModel, is(notNullValue()));
        assertThat(chatModel, is(instanceOf(dev.langchain4j.model.anthropic.AnthropicChatModel.class)));
        }

    @Test
    @DisplayName("should create chat model with custom base URL")
    void shouldCreateChatModelWithCustomBaseUrl()
        {
        String customBaseUrl = "https://api.anthropic.com/";
        when(mockConfig.getOptionalValue(ANTHROPIC_BASE_URL, String.class))
            .thenReturn(Optional.of(customBaseUrl));
        when(mockConfig.getOptionalValue(ANTHROPIC_API_KEY, String.class))
                .thenReturn(Optional.of(TEST_API_KEY));

        ChatModel chatModel = provider.getChatModel("claude-3-5-sonnet-latest");

        assertThat(chatModel, is(notNullValue()));
        assertThat(chatModel, is(instanceOf(dev.langchain4j.model.anthropic.AnthropicChatModel.class)));
        }

    @Test
    @DisplayName("should create streaming chat model with default base URL")
    void shouldCreateStreamingChatModelWithDefaultBaseUrl()
        {
        when(mockConfig.getOptionalValue(ANTHROPIC_BASE_URL, String.class))
            .thenReturn(Optional.empty());
        when(mockConfig.getOptionalValue(ANTHROPIC_API_KEY, String.class))
                .thenReturn(Optional.of(TEST_API_KEY));

        StreamingChatModel streamingChatModel = provider.getStreamingChatModel("claude-3-5-sonnet-latest");

        assertThat(streamingChatModel, is(notNullValue()));
        assertThat(streamingChatModel, is(instanceOf(dev.langchain4j.model.anthropic.AnthropicStreamingChatModel.class)));
        }

    @Test
    @DisplayName("should create streaming chat model with custom base URL")
    void shouldCreateStreamingChatModelWithCustomBaseUrl()
        {
        String customBaseUrl = "https://api.anthropic.com/";
        when(mockConfig.getOptionalValue(ANTHROPIC_BASE_URL, String.class))
            .thenReturn(Optional.of(customBaseUrl));
        when(mockConfig.getOptionalValue(ANTHROPIC_API_KEY, String.class))
                .thenReturn(Optional.of(TEST_API_KEY));

        StreamingChatModel streamingChatModel = provider.getStreamingChatModel("claude-3-5-sonnet-latest");

        assertThat(streamingChatModel, is(notNullValue()));
        assertThat(streamingChatModel, is(instanceOf(dev.langchain4j.model.anthropic.AnthropicStreamingChatModel.class)));
        }

    @Test
    @DisplayName("should handle various chat model names")
    void shouldHandleVariousChatModelNames()
        {
        when(mockConfig.getOptionalValue(ANTHROPIC_BASE_URL, String.class))
            .thenReturn(Optional.empty());
        when(mockConfig.getOptionalValue(ANTHROPIC_API_KEY, String.class))
                .thenReturn(Optional.of(TEST_API_KEY));

        String[] chatModels = {
            "claude-3-5-sonnet-latest"
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
        IllegalArgumentException exception2 = assertThrows(IllegalArgumentException.class,
            () -> provider.getChatModel(null));
        assertThat(exception2.getMessage(), containsString("Model name cannot be null or empty"));

        IllegalArgumentException exception3 = assertThrows(IllegalArgumentException.class,
            () -> provider.getStreamingChatModel(null));
        assertThat(exception3.getMessage(), containsString("Model name cannot be null or empty"));

        // Test empty model names
        IllegalArgumentException exception5 = assertThrows(IllegalArgumentException.class,
            () -> provider.getChatModel(""));
        assertThat(exception5.getMessage(), containsString("Model name cannot be null or empty"));

        IllegalArgumentException exception6 = assertThrows(IllegalArgumentException.class,
            () -> provider.getStreamingChatModel(""));
        assertThat(exception6.getMessage(), containsString("Model name cannot be null or empty"));

        // Test whitespace-only model names
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
        when(mockConfig.getOptionalValue(ANTHROPIC_BASE_URL, String.class))
            .thenReturn(Optional.empty());
        when(mockConfig.getOptionalValue(ANTHROPIC_API_KEY, String.class))
                .thenReturn(Optional.of(TEST_API_KEY));

        // Create a spy to verify the actual base URL being used
        AnthropicModelProvider spyProvider = spy(provider);

        // Create all model types to ensure default URL is used
        ChatModel chatModel = spyProvider.getChatModel("claude-3-5-sonnet-latest");
        StreamingChatModel streamingModel = spyProvider.getStreamingChatModel("claude-3-5-sonnet-latest");

        assertThat(chatModel, is(notNullValue()));
        assertThat(streamingModel, is(notNullValue()));
        
        // Verify that the base URL method returns the expected default value
        String actualBaseUrl = spyProvider.anthropicBaseUrl();
        assertThat("Default base URL should be used when not configured", 
            actualBaseUrl, is("https://api.anthropic.com/v1/"));
        
        // Verify configuration methods were called
        verify(mockConfig, atLeast(3)).getOptionalValue(ANTHROPIC_BASE_URL, String.class);
        }

    @Test
    @DisplayName("should handle multiple model creation calls")
    void shouldHandleMultipleModelCreationCalls()
        {
        when(mockConfig.getOptionalValue(ANTHROPIC_BASE_URL, String.class))
            .thenReturn(Optional.empty());
        when(mockConfig.getOptionalValue(ANTHROPIC_API_KEY, String.class))
                .thenReturn(Optional.of(TEST_API_KEY));

        // Create multiple models of each type
        ChatModel chat1 = provider.getChatModel("claude-3-5-sonnet-latest");
        ChatModel chat2 = provider.getChatModel("claude-sonnet-4-0");
        StreamingChatModel streaming1 = provider.getStreamingChatModel("claude-3-5-sonnet-latest");
        StreamingChatModel streaming2 = provider.getStreamingChatModel("claude-sonnet-4-0");

        // All should be created successfully and be different instances
        assertThat(chat1, is(notNullValue()));
        assertThat(chat2, is(notNullValue()));
        assertThat(streaming1, is(notNullValue()));
        assertThat(streaming2, is(notNullValue()));
        
        // Models should be different instances
        assertThat(chat1, is(not(sameInstance(chat2))));
        assertThat(streaming1, is(not(sameInstance(streaming2))));
        }

    @Test
    @DisplayName("should create models with custom base URL configuration")
    void shouldCreateModelsWithCustomBaseUrlConfiguration()
        {
        String customBaseUrl = "http://remote-anthropic:11434";
        when(mockConfig.getOptionalValue(ANTHROPIC_BASE_URL, String.class))
            .thenReturn(Optional.of(customBaseUrl));
        when(mockConfig.getOptionalValue(ANTHROPIC_API_KEY, String.class))
                .thenReturn(Optional.of(TEST_API_KEY));

        // Create all model types with custom base URL
        ChatModel chatModel = provider.getChatModel("claude-3-5-sonnet-latest");
        StreamingChatModel streamingModel = provider.getStreamingChatModel("claude-3-5-sonnet-latest");

        assertThat(chatModel, is(notNullValue()));
        assertThat(streamingModel, is(notNullValue()));
        }

    @Test
    @DisplayName("should handle different URL formats")
    void shouldHandleDifferentUrlFormats()
        {
        String[] urls = {
            "https://api.anthropic.com/",
            "http://127.0.0.1:11434",
            "https://api.anthropic.com:11434",
            "http://anthropic-server:11434",
            "https://remote-anthropic.company.com:8080"
        };

        for (String url : urls)
            {
            when(mockConfig.getOptionalValue(ANTHROPIC_BASE_URL, String.class))
                .thenReturn(Optional.of(url));
            when(mockConfig.getOptionalValue(ANTHROPIC_API_KEY, String.class))
                    .thenReturn(Optional.of(TEST_API_KEY));

            ChatModel chatModel = provider.getChatModel("claude-3-5-sonnet-latest");
            StreamingChatModel streamingModel = provider.getStreamingChatModel("claude-3-5-sonnet-latest");

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
        when(mockConfig.getOptionalValue(ANTHROPIC_BASE_URL, String.class))
            .thenReturn(Optional.empty());
        when(mockConfig.getOptionalValue(ANTHROPIC_API_KEY, String.class))
                .thenReturn(Optional.of(TEST_API_KEY));

        // Create one of each model type
        provider.getChatModel("claude-3-5-sonnet-latest");
        provider.getStreamingChatModel("claude-3-5-sonnet-latest");

        // Verify configuration was accessed for each model creation
        verify(mockConfig, times(2)).getOptionalValue(ANTHROPIC_BASE_URL, String.class);
        }

    @Test
    @DisplayName("should handle model names with version tags")
    void shouldHandleModelNamesWithVersionTags()
        {
        when(mockConfig.getOptionalValue(ANTHROPIC_BASE_URL, String.class))
            .thenReturn(Optional.empty());
        when(mockConfig.getOptionalValue(ANTHROPIC_API_KEY, String.class))
                .thenReturn(Optional.of(TEST_API_KEY));

        String[] modelNamesWithTags = {
            "claude-3-5-sonnet-latest",
            "claude-sonnet-4-0"
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

    // ---- constants -------------------------------------------------------

    private static final String TEST_API_KEY = "test-anthropic-api-key";

    private static final String ANTHROPIC_API_KEY = "anthropic.api.key";

    private static final String ANTHROPIC_BASE_URL = "anthropic.base.url";
    }