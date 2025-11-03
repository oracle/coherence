/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.model.openai;

import com.oracle.coherence.rag.ModelProvider;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import io.helidon.config.ConfigException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.eclipse.microprofile.config.Config;

/**
 * ModelProvider implementation for OpenAI models.
 * <p/>
 * This provider enables integration with OpenAI's language models including GPT-4,
 * GPT-3.5, and text-embedding models. OpenAI models provide state-of-the-art
 * performance in natural language understanding, generation, and embedding tasks.
 * <p/>
 * The provider supports all three model types: embedding models for creating vector
 * representations of text, chat models for conversational AI, and streaming chat 
 * models for real-time response generation.
 * <p/>
 * Configuration is managed through MicroProfile Config with the following properties:
 * <ul>
 * <li>openai.api.key - Required API key for OpenAI service</li>
 * <li>openai.base.url - Base URL for OpenAI API (defaults to https://api.openai.com/v1)</li>
 * </ul>
 * <p/>
 * Example usage:
 * <pre>{@code
 * @Inject
 * @Named("OpenAI")
 * ModelProvider openAiProvider;
 * 
 * EmbeddingModel embeddingModel = openAiProvider.getEmbeddingModel("text-embedding-3-large");
 * ChatModel chatModel = openAiProvider.getChatModel("gpt-4");
 * StreamingChatModel streamingModel = openAiProvider.getStreamingChatModel("gpt-4");
 * }</pre>
 *
 * @author Aleks Seovic 2025.07.04
 * @since 25.09
 */
@ApplicationScoped
@Named("OpenAI")
@SuppressWarnings("CdiInjectionPointsInspection")
public class OpenAiModelProvider
        implements ModelProvider
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Default constructor for CDI initialization.
     */
    public OpenAiModelProvider()
        {
        }

    // ---- ModelProvider interface implementation -------------------------

    /**
     * {@inheritDoc}
     * <p/>
     * Creates an OpenAI embedding model for generating vector representations
     * of text. Supports models like text-embedding-3-large, text-embedding-3-small,
     * and text-embedding-ada-002.
     *
     * @param sName the name of the embedding model to create
     *
     * @return a configured OpenAiEmbeddingModel instance
     *
     * @throws ConfigException if the required API key is not configured
     * @throws IllegalArgumentException if the model name is null or empty
     */
    @Override
    public EmbeddingModel getEmbeddingModel(String sName)
        {
        validateModelName(sName);
        return OpenAiEmbeddingModel.builder()
                            .apiKey(openAiApiKey())
                            .baseUrl(openAiBaseUrl())
                            .modelName(sName)
                            .build();
        }

    /**
     * {@inheritDoc}
     * <p/>
     * Creates an OpenAI chat model for conversational AI. Supports models
     * like gpt-4, gpt-4-turbo, and gpt-3.5-turbo.
     *
     * @param sName the name of the chat model to create (e.g., "gpt-4")
     *
     * @return a configured OpenAiChatModel instance
     *
     * @throws ConfigException if the required API key is not configured
     * @throws IllegalArgumentException if the model name is null or empty
     */
    @Override
    public ChatModel getChatModel(String sName)
        {
        validateModelName(sName);
        return OpenAiChatModel.builder()
                            .apiKey(openAiApiKey())
                            .baseUrl(openAiBaseUrl())
                            .modelName(sName)
                            .build();
        }

    /**
     * {@inheritDoc}
     * <p/>
     * Creates an OpenAI streaming chat model for real-time response generation.
     * Enables progressive response streaming suitable for interactive applications.
     *
     * @param sName the name of the streaming chat model to create (e.g., "gpt-4")
     *
     * @return a configured OpenAiStreamingChatModel instance
     *
     * @throws ConfigException if the required API key is not configured
     * @throws IllegalArgumentException if the model name is null or empty
     */
    @Override
    public StreamingChatModel getStreamingChatModel(String sName)
        {
        validateModelName(sName);
        return OpenAiStreamingChatModel.builder()
                            .apiKey(openAiApiKey())
                            .baseUrl(openAiBaseUrl())
                            .modelName(sName)
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
     * Returns the OpenAI API key from configuration.
     *
     * @return the API key for OpenAI service
     *
     * @throws ConfigException if the API key is not configured or is empty/whitespace-only
     */
    protected String openAiApiKey()
        {
        String apiKey = config.getOptionalValue("openai.api.key", String.class)
                .orElseThrow(() -> new ConfigException("OpenAI API key is not set. Please set the config property 'openai.api.key'"));
        
        if (apiKey.trim().isEmpty())
            {
            throw new ConfigException("OpenAI API key cannot be empty or whitespace-only. Please set a valid config property 'openai.api.key'");
            }
        
        return apiKey;
        }

    /**
     * Returns the OpenAI base URL from configuration.
     *
     * @return the base URL for OpenAI API, defaults to {@code https://api.openai.com/v1}
     */
    protected String openAiBaseUrl()
        {
        return config.getOptionalValue("openai.base.url", String.class).orElse(DEFAULT_OPENAI_BASE_URL);
        }

    // ---- data members ----------------------------------------------------

    /**
     * MicroProfile Config instance for reading configuration properties.
     */
    @Inject
    private Config config;

    // ---- constants -------------------------------------------------------

    /**
     * Default base URL for OpenAI API.
     */
    private static final String DEFAULT_OPENAI_BASE_URL = "https://api.openai.com/v1";
    }
