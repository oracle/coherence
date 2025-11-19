/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.model;

import com.oracle.coherence.rag.ModelProvider;
import com.oracle.coherence.rag.util.CdiHelper;

import dev.langchain4j.model.chat.StreamingChatModel;

import io.helidon.config.ConfigException;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * CDI supplier for streaming chat models in the Coherence RAG framework.
 * <p/>
 * This supplier extends {@link AbstractModelSupplier} to provide streaming chat model
 * instances for conversational AI interactions. It supports dynamic model selection
 * based on configuration and maintains cached instances for efficient reuse.
 * <p/>
 * The supplier integrates with various AI service providers through the
 * {@link ModelProvider} interface, supporting models from OpenAI, OCI GenAI,
 * Ollama, and other providers. Models are identified using the standard
 * "provider/model" naming convention.
 * <p/>
 * Configuration:
 * <ul>
 *   <li>Default model: {@value #DEFAULT_CHAT_MODEL}</li>
 *   <li>Configuration property: {@code model.chat}</li>
 *   <li>Supports runtime configuration changes</li>
 * </ul>
 * <p/>
 * Usage example:
 * <pre>
 * // Get default chat model
 * StreamingChatModel model = chatModelSupplier.get();
 * 
 * // Get specific model
 * StreamingChatModel specificModel = chatModelSupplier.get("oci/meta.llama-3.1-70b-instruct");
 * </pre>
 * 
 * @author Aleks Seovic  2025.07.04
 * @since 25.09
 */
@ApplicationScoped
public class StreamingChatModelSupplier
        extends AbstractModelSupplier<StreamingChatModel>
    {
    /**
     * The default chat model used when no specific model is configured.
     * <p/>
     * This represents a balanced choice between performance and cost for
     * general-purpose conversational AI tasks.
     */
    public static final String DEFAULT_CHAT_MODEL = "OpenAI/gpt-4o-mini";

    /**
     * Returns a human-readable description of the model type.
     * <p/>
     * This description is used in logging and error messages to identify
     * the type of models this supplier manages.
     * 
     * @return "chat" identifying this as a chat model supplier
     */
    protected String description()
        {
        return "chat";
        }

    /**
     * Returns the default model name when no configuration is provided.
     * <p/>
     * This method provides the fallback chat model that will be used
     * when no explicit configuration is available.
     * 
     * @return the default chat model name
     */
    protected String defaultModel()
        {
        return DEFAULT_CHAT_MODEL;
        }

    /**
     * Returns the configuration property key for chat models.
     * <p/>
     * This property can be used to configure the default chat model
     * at runtime through various configuration sources.
     * 
     * @return "model.chat" as the configuration property key
     */
    protected String configProperty()
        {
        return "model.chat";
        }

    /**
     * Creates a new streaming chat model instance for the specified model name.
     * <p/>
     * This method uses the model provider framework to create chat model
     * instances. It looks up the appropriate {@link ModelProvider} based on
     * the model name's provider component and delegates model creation to
     * that provider.
     * 
     * @param modelName the name of the chat model to create
     * 
     * @return a new StreamingChatModel instance
     * 
     * @throws ConfigException if the specified model provider is not supported
     *                        or if the model cannot be created
     */
    public StreamingChatModel create(ModelName modelName)
        {
        ModelProvider provider = CdiHelper.getNamedBean(ModelProvider.class, modelName.provider());
        if (provider == null)
            {
            throw new ConfigException("Chat model [%s] is not supported".formatted(modelName.fullName()));
            }

        return provider.getStreamingChatModel(modelName.name());
        }
    }
