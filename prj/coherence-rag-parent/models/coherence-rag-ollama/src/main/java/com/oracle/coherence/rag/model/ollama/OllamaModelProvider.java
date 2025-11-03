/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.model.ollama;

import com.oracle.coherence.rag.ModelProvider;

import com.oracle.coherence.rag.config.ConfigKey;
import com.oracle.coherence.rag.config.ConfigRepository;
import com.oracle.coherence.rag.model.ollama.config.OllamaChatModelConfig;
import com.oracle.coherence.rag.model.ollama.config.OllamaEmbeddingModelConfig;
import com.oracle.coherence.rag.model.ollama.config.OllamaStreamingChatModelConfig;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;

import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.eclipse.microprofile.config.Config;

/**
 * ModelProvider implementation for Ollama models.
 * <p/>
 * This provider enables integration with Ollama, a tool for running large language
 * models locally. Ollama supports a wide variety of models including Llama, Mistral,
 * Code Llama, and many others, providing both chat and embedding capabilities.
 * <p/>
 * The provider supports all three model types: embedding models for creating vector
 * representations of text, chat models for conversational AI, and streaming chat 
 * models for real-time response generation.
 * <p/>
 * Configuration is managed through MicroProfile Config with the following property:
 * <ul>
 * <li>ollama.base.url - Base URL for Ollama service (defaults to {@code http://localhost:11434})</li>
 * </ul>
 * <p/>
 * Example usage:
 * <pre>{@code
 * @Inject
 * @Named("Ollama")
 * ModelProvider ollamaProvider;
 * 
 * EmbeddingModel embeddingModel = ollamaProvider.getEmbeddingModel("nomic-embed-text");
 * ChatModel chatModel = ollamaProvider.getChatModel("llama3.1:8b");
 * StreamingChatModel streamingModel = ollamaProvider.getStreamingChatModel("llama3.1:8b");
 * }</pre>
 *
 * @author Aleks Seovic 2025.07.04
 * @since 25.09
 */
@ApplicationScoped
@Named("Ollama")
@SuppressWarnings("CdiInjectionPointsInspection")
public class OllamaModelProvider
        implements ModelProvider
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Default constructor for CDI initialization.
     */
    @Inject
    public OllamaModelProvider(Config config, ConfigRepository jsonConfig)
        {
        this.config = config;
        this.jsonConfig = jsonConfig;
        }

    // ---- ModelProvider interface implementation -------------------------

    /**
     * {@inheritDoc}
     * <p/>
     * Creates an Ollama embedding model for generating vector representations
     * of text. Supports models like nomic-embed-text, mxbai-embed-large, and
     * other embedding-capable models available in Ollama.
     *
     * @param sName the name of the embedding model to create
     *
     * @return a configured OllamaEmbeddingModel instance
     *
     * @throws IllegalArgumentException if the model name is null or empty
     */
    @Override
    public EmbeddingModel getEmbeddingModel(String sName)
        {
        validateModelName(sName);
        var builder = OllamaEmbeddingModel.builder()
                .baseUrl(ollamaBaseUrl())
                .modelName(sName);

        var key = new ConfigKey("embedding:Ollama/" + sName);
        return jsonConfig.get(key, new OllamaEmbeddingModelConfig())
                .apply(builder)
                .build();
        }

    /**
     * {@inheritDoc}
     * <p/>
     * Creates an Ollama chat model for conversational AI. Supports a wide
     * variety of models including Llama, Mistral, Code Llama, and others.
     *
     * @param sName the name of the chat model to create (e.g., "llama3.1:8b")
     *
     * @return a configured OllamaChatModel instance
     *
     * @throws IllegalArgumentException if the model name is null or empty
     */
    @Override
    public ChatModel getChatModel(String sName)
        {
        validateModelName(sName);
        var builder = OllamaChatModel.builder()
                .baseUrl(ollamaBaseUrl())
                .modelName(sName);

        var key = new ConfigKey("chat:Ollama/" + sName);
        return jsonConfig.get(key, new OllamaChatModelConfig())
                .apply(builder)
                .build();
        }

    /**
     * {@inheritDoc}
     * <p/>
     * Creates an Ollama streaming chat model for real-time response generation.
     * Enables progressive response streaming suitable for interactive applications.
     *
     * @param sName the name of the streaming chat model to create (e.g., "llama3.1:8b")
     *
     * @return a configured OllamaStreamingChatModel instance
     *
     * @throws IllegalArgumentException if the model name is null or empty
     */
    @Override
    public StreamingChatModel getStreamingChatModel(String sName)
        {
        validateModelName(sName);
        var builder = OllamaStreamingChatModel.builder()
                .baseUrl(ollamaBaseUrl())
                .modelName(sName);

        var key = new ConfigKey("streamingChat:Ollama/" + sName);
        return jsonConfig.get(key, new OllamaStreamingChatModelConfig())
                .apply(builder)
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
     * Returns the Ollama base URL from configuration.
     *
     * @return the base URL for Ollama service, defaults to {@code http://localhost:11434}
     */
    protected String ollamaBaseUrl()
        {
        return config.getOptionalValue("ollama.base.url", String.class).orElse(DEFAULT_OLLAMA_BASE_URL);
        }

    // ---- data members ----------------------------------------------------

    /**
     * MicroProfile Config instance for reading configuration properties.
     */
    private final Config config;

    /**
     * Repository containing model configuration.
     */
    private final ConfigRepository jsonConfig;

    // ---- constants -------------------------------------------------------

    /**
     * Default base URL for Ollama service running locally.
     */
    private static final String DEFAULT_OLLAMA_BASE_URL = "http://localhost:11434";
    }
