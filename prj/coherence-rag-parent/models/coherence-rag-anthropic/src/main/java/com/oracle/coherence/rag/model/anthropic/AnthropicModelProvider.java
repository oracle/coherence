/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.model.anthropic;

import com.oracle.coherence.rag.ModelProvider;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel;

import io.helidon.config.ConfigException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.eclipse.microprofile.config.Config;

/**
 * ModelProvider implementation for Anthropic models.
 * <p/>
 * This provider enables integration with Anthropic.
 * <p/>
 * The provider supports chat models for conversational AI, and streaming chat
 * models for real-time response generation.
 * <p/>
 * Configuration is managed through MicroProfile Config with the following property:
 * <ul>
 * <li>anthropic.api.key - Required API key for Anthropic service</li>
 * <li>anthropic.base.url - Base URL for Anthropic API (defaults to https://api.anthropic.com/v1/)</li>
 * </ul>
 * <p/>
 * Example usage:
 * <pre>{@code
 * @Inject
 * @Named("Anthropic")
 * ModelProvider anthropicProvider;
 *
 * ChatModel chatModel = anthropicProvider.getChatModel("claude-sonnet-4-20250514");
 * StreamingChatModel streamingModel = anthropicProvider.getStreamingChatModel("claude-sonnet-4-20250514");
 * }</pre>
 *
 * @author Aleks Seovic/ Tim Middleton 2025.08.06
 * @since 25.09
 */
@ApplicationScoped
@Named("Anthropic")
@SuppressWarnings("CdiInjectionPointsInspection")
public class AnthropicModelProvider
        implements ModelProvider
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Default constructor for CDI initialization.
     */
    public AnthropicModelProvider()
        {
        }

    // ---- ModelProvider interface implementation -------------------------

    /**
     * {@inheritDoc}
     * <p/>
     *
     * @param sName the name of the embedding model to create
     *
     * @return a configured EmbeddingModel
     *
     * @throws UnsupportedOperationException not supported by Anthropic
     */
    @Override
    public EmbeddingModel getEmbeddingModel(String sName)
        {
        throw new UnsupportedOperationException("Anthropic does not support embedding");
        }

    /**
     * {@inheritDoc}
     * <p/>
     * Creates an Anthropic chat model for conversational AI.
     *
     * @param sName the name of the chat model to create (e.g., "claude-sonnet-4-20250514")
     *
     * @return a configured AnthropicChatModel instance
     *
     * @throws IllegalArgumentException if the model name is null or empty
     */
    @Override
    public ChatModel getChatModel(String sName)
        {
        validateModelName(sName);
        return AnthropicChatModel.builder()
                            .baseUrl(anthropicBaseUrl())
                            .modelName(sName)
                            .apiKey(anthropicApiKey())
                            .build();
        }

    /**
     * {@inheritDoc}
     * <p/>
     * Creates an Anthropic streaming chat model for real-time response generation.
     * Enables progressive response streaming suitable for interactive applications.
     *
     * @param sName the name of the streaming chat model to create (e.g., "claude-sonnet-4-20250514")
     *
     * @return a configured {@link AnthropicStreamingChatModel} instance
     *
     * @throws IllegalArgumentException if the model name is null or empty
     */
    @Override
    public StreamingChatModel getStreamingChatModel(String sName)
        {
        validateModelName(sName);
        return AnthropicStreamingChatModel.builder()
                            .baseUrl(anthropicBaseUrl())
                            .modelName(sName)
                            .apiKey(anthropicApiKey())
                            .build();
        }

    // ---- helper methods --------------------------------------------------

    /**
     * Validates that the model name is not null or empty.
     *
     * @param sName the model name to validate
     *
     * @throws IllegalArgumentException if the model name is null or empty
     */
    private void validateModelName(String sName)
        {
        if (sName == null || sName.trim().isEmpty())
            {
            throw new IllegalArgumentException("Model name cannot be null or empty");
            }
        }

    // ---- configuration ---------------------------------------------------

    /**
     * Returns the Anthropic base URL from configuration.
     *
     * @return the base URL for Anthropic service, defaults to https://api.anthropic.com/v1/
     */
    protected String anthropicBaseUrl()
        {
        return config.getOptionalValue("anthropic.base.url", String.class).orElse(DEFAULT_ANTHROPIC_BASE_URL);
        }

    /**
     * Returns the Anthropic API key from configuration.
     *
     * @return the Anthropic key for Anthropic service
     *
     * @throws ConfigException if the API key is not configured or is empty/whitespace-only
     */
    protected String anthropicApiKey()
        {
        String apiKey = config.getOptionalValue("anthropic.api.key", String.class)
                .orElseThrow(() -> new ConfigException("Anthropic API key is not set. Please set the config property 'anthropic.api.key'"));

        if (apiKey.trim().isEmpty())
            {
            throw new ConfigException("Anthropic API key cannot be empty or whitespace-only. Please set a valid config property 'anthropic.api.key'");
            }

        return apiKey;
        }

    // ---- data members ----------------------------------------------------

    /**
     * MicroProfile Config instance for reading configuration properties.
     */
    @Inject
    private Config config;

    // ---- constants -------------------------------------------------------

    /**
     * Default base URL for Anthropic service.
     */
    private static final String DEFAULT_ANTHROPIC_BASE_URL = "https://api.anthropic.com/v1/";
    }
