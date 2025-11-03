/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.model.oci;

import com.oracle.coherence.rag.ModelProvider;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;

import io.helidon.config.ConfigException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.eclipse.microprofile.config.Config;

/**
 * ModelProvider implementation for Oracle Cloud Infrastructure (OCI) GenAI models.
 * <p/>
 * This provider enables integration with OCI's Generative AI service, which provides
 * access to state-of-the-art foundation models including Cohere Command models for
 * chat and Cohere Embed models for text embeddings.
 * <p/>
 * The provider supports all three model types: embedding models for creating vector
 * representations of text, chat models for conversational AI, and streaming chat 
 * models for real-time response generation.
 * <p/>
 * Authentication is handled through OCI's authentication mechanisms including:
 * <ul>
 * <li>OCI config file authentication</li>
 * <li>Instance principals (for compute instances)</li>
 * <li>Manual configuration via properties</li>
 * </ul>
 * <p/>
 * Configuration is managed through MicroProfile Config with the following required property:
 * <ul>
 * <li>oci.compartment.id - OCI compartment ID where models are deployed</li>
 * </ul>
 * <p/>
 * Example usage:
 * <pre>{@code
 * @Inject
 * @Named("OCI")
 * ModelProvider ociProvider;
 * 
 * EmbeddingModel embeddingModel = ociProvider.getEmbeddingModel("cohere.embed-multilingual-v3.0");
 * ChatModel chatModel = ociProvider.getChatModel("cohere.command-r-08-2024");
 * StreamingChatModel streamingModel = ociProvider.getStreamingChatModel("cohere.command-r-08-2024");
 * }</pre>
 *
 * @author Aleks Seovic 2025.07.04
 * @since 25.09
 */
@ApplicationScoped
@Named("OCI")
public class OciModelProvider
        implements ModelProvider
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Constructor for CDI initialization.
     */
    @Inject
    public OciModelProvider(Config config)
        {
        this.config = config;
        }

    // ---- ModelProvider interface implementation -------------------------

    /**
     * {@inheritDoc}
     * <p/>
     * Creates an OCI GenAI embedding model for generating vector representations
     * of text. Supports Cohere embedding models like cohere.embed-multilingual-v3.0
     * and cohere.embed-english-v3.0.
     *
     * @param sName the name of the embedding model to create
     *
     * @return a configured OciGenAiEmbeddingModel instance
     *
     * @throws ConfigException if the required compartment ID is not configured
     * @throws IllegalArgumentException if the model name is null or empty
     */
    @Override
    public EmbeddingModel getEmbeddingModel(String sName)
        {
        validateModelName(sName);
        return OciGenAiEmbeddingModel.builder(config)
                            .compartmentId(ociCompartmentId())
                            .modelName(sName)
                            .build();
        }

    /**
     * {@inheritDoc}
     * <p/>
     * Creates an OCI GenAI chat model for conversational AI. Supports Cohere
     * Command models like cohere.command-r-08-2024 and cohere.command-r-plus-08-2024.
     *
     * @param sName the name of the chat model to create
     *
     * @return a configured OciGenAiChatModel instance
     *
     * @throws ConfigException if the required compartment ID is not configured
     * @throws IllegalArgumentException if the model name is null or empty
     */
    @Override
    public ChatModel getChatModel(String sName)
        {
        validateModelName(sName);
        return OciGenAiChatModel.builder(config)
                            .compartmentId(ociCompartmentId())
                            .modelName(sName)
                            .build();
        }

    /**
     * {@inheritDoc}
     * <p/>
     * Creates an OCI GenAI streaming chat model for real-time response generation.
     * Enables progressive response streaming suitable for interactive applications.
     *
     * @param sName the name of the streaming chat model to create
     *
     * @return a configured OciGenAiStreamingChatModel instance
     *
     * @throws ConfigException if the required compartment ID is not configured
     * @throws IllegalArgumentException if the model name is null or empty
     */
    @Override
    public StreamingChatModel getStreamingChatModel(String sName)
        {
        validateModelName(sName);
        return OciGenAiStreamingChatModel.builder(config)
                            .compartmentId(ociCompartmentId())
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
     * Returns the OCI compartment ID from configuration.
     *
     * @return the compartment ID where OCI GenAI models are deployed
     *
     * @throws ConfigException if the compartment ID is not configured
     */
    protected String ociCompartmentId()
        {
        String compartmentId = config.getOptionalValue("oci.compartment.id", String.class)
                .orElseThrow(() -> new ConfigException("OCI compartment ID is not set. Please set the config property 'oci.compartment.id'"));

        if (compartmentId.trim().isEmpty())
            {
            throw new ConfigException("OCI compartment ID is not set. Please set the config property 'oci.compartment.id'");
            }

        return compartmentId;
        }

    // ---- data members ----------------------------------------------------

    /**
     * MicroProfile Config instance for reading configuration properties.
     */
    Config config;
    }
