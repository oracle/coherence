/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.model.deepseek;

import com.oracle.coherence.rag.ModelProvider;

import com.oracle.coherence.rag.config.ConfigKey;
import com.oracle.coherence.rag.config.ConfigRepository;
import com.oracle.coherence.rag.model.deepseek.config.DeepSeekChatModelConfig;
import com.oracle.coherence.rag.model.deepseek.config.DeepSeekStreamingChatModelConfig;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

import io.helidon.config.ConfigException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.eclipse.microprofile.config.Config;

/**
 * ModelProvider implementation for DeepSeek AI models.
 * <p/>
 * This provider enables integration with DeepSeek's chat models using their
 * OpenAI-compatible API. DeepSeek models are particularly strong at code 
 * generation, mathematics, and reasoning tasks.
 * <p/>
 * The provider supports both regular and streaming chat models, but does not
 * currently support embedding models as DeepSeek focuses on language generation
 * rather than embedding generation.
 * <p/>
 * Configuration is managed through MicroProfile Config with the following properties:
 * <ul>
 * <li>deepseek.api.key - Required API key for DeepSeek service</li>
 * <li>deepseek.base.url - Base URL for DeepSeek API (defaults to {@code https://api.deepseek.com/v1})</li>
 * </ul>
 * <p/>
 * Example usage:
 * <pre>{@code
 * @Inject
 * @Named("DeepSeek")
 * ModelProvider deepSeekProvider;
 * 
 * ChatModel chatModel = deepSeekProvider.getChatModel("deepseek-chat");
 * StreamingChatModel streamingModel = deepSeekProvider.getStreamingChatModel("deepseek-chat");
 * }</pre>
 *
 * @author Aleks Seovic 2025.07.04
 * @since 25.09
 */
@ApplicationScoped
@Named("DeepSeek")
@SuppressWarnings("CdiInjectionPointsInspection")
public class DeepSeekModelProvider
        implements ModelProvider
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Default constructor for CDI initialization.
     */
    @Inject
    public DeepSeekModelProvider(Config config, ConfigRepository jsonConfig)
        {
        this.config = config;
        this.jsonConfig = jsonConfig;
        }

    // ---- ModelProvider interface implementation -------------------------

    /**
     * {@inheritDoc}
     * <p/>
     * DeepSeek does not currently provide embedding models, so this method
     * returns null for all model names.
     *
     * @param sName the name of the embedding model to create
     *
     * @return always null as DeepSeek does not provide embedding models
     */
    @Override
    public EmbeddingModel getEmbeddingModel(String sName)
        {
        return null;
        }

    /**
     * {@inheritDoc}
     * <p/>
     * Creates a DeepSeek chat model using the OpenAI-compatible API.
     * The model supports conversation, code generation, and reasoning tasks.
     *
     * @param sName the name of the chat model to create (e.g., "deepseek-chat")
     *
     * @return a configured OpenAiChatModel instance for DeepSeek
     *
     * @throws ConfigException if the required API key is not configured
     * @throws IllegalArgumentException if the model name is null or empty
     */
    @Override
    public ChatModel getChatModel(String sName)
        {
        validateModelName(sName);
        var builder = OpenAiChatModel.builder()
                .apiKey(deepSeekApiKey())
                .baseUrl(deepSeekBaseUrl())
                .modelName(sName);

        var key = new ConfigKey("chat:DeepSeek/" + sName);
        return jsonConfig.get(key, new DeepSeekChatModelConfig())
                .apply(builder)
                .build();
        }

    /**
     * {@inheritDoc}
     * <p/>
     * Creates a DeepSeek streaming chat model using the OpenAI-compatible API.
     * The streaming model provides real-time response generation suitable for
     * interactive applications.
     *
     * @param sName the name of the streaming chat model to create (e.g., "deepseek-chat")
     *
     * @return a configured OpenAiStreamingChatModel instance for DeepSeek
     *
     * @throws ConfigException if the required API key is not configured
     * @throws IllegalArgumentException if the model name is null or empty
     */
    @Override
    public StreamingChatModel getStreamingChatModel(String sName)
        {
        validateModelName(sName);
        var builder = OpenAiStreamingChatModel.builder()
                .apiKey(deepSeekApiKey())
                .baseUrl(deepSeekBaseUrl())
                .modelName(sName);

        var key = new ConfigKey("streamingChat:DeepSeek/" + sName);
        return jsonConfig.get(key, new DeepSeekStreamingChatModelConfig())
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
     * Returns the DeepSeek API key from configuration.
     *
     * @return the API key for DeepSeek service
     *
     * @throws ConfigException if the API key is not configured or is empty/whitespace-only
     */
    protected String deepSeekApiKey()
        {
        String apiKey = config.getOptionalValue("deepseek.api.key", String.class)
                .orElseThrow(() -> new ConfigException("DeepSeek API key is not set. Please set the config property 'deepseek.api.key'"));
        
        if (apiKey.trim().isEmpty())
            {
            throw new ConfigException("DeepSeek API key cannot be empty or whitespace-only. Please set a valid config property 'deepseek.api.key'");
            }
        
        return apiKey;
        }

    /**
     * Returns the DeepSeek base URL from configuration.
     *
     * @return the base URL for DeepSeek API, defaults to {@code https://api.deepseek.com/v1}
     */
    protected String deepSeekBaseUrl()
        {
        return config.getOptionalValue("deepseek.base.url", String.class).orElse(DEFAULT_DEEPSEEK_BASE_URL);
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
     * Default base URL for DeepSeek API.
     */
    private static final String DEFAULT_DEEPSEEK_BASE_URL = "https://api.deepseek.com/v1";
    }
