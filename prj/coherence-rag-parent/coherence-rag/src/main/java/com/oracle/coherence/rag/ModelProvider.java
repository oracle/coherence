/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.scoring.ScoringModel;

/**
 * Interface for providing access to various AI models in the Coherence RAG framework.
 * <p/>
 * This interface defines the contract for model provider implementations that can
 * supply different types of AI models including embedding models for vector generation,
 * chat models for conversation, and scoring models for relevance ranking.
 * <p/>
 * Model providers act as factories for creating and configuring AI models from
 * different vendors such as OpenAI, Oracle Cloud Infrastructure (OCI), Ollama,
 * and other supported providers.
 * <p/>
 * Example usage:
 * <pre>
 * ModelProvider provider = // get provider instance
 * EmbeddingModel embedModel = provider.getEmbeddingModel("text-embedding-ada-002");
 * ChatModel chatModel = provider.getChatModel("gpt-3.5-turbo");
 * </pre>
 * 
 * @author Aleks Seovic  2025.07.04
 * @since 25.09
 */
public interface ModelProvider
    {
    /**
     * Returns an embedding model instance for the specified model name.
     * <p/>
     * Embedding models convert text into vector representations that can be
     * used for semantic similarity search and document retrieval operations.
     *
     * @param sName the name or identifier of the embedding model
     *
     * @return an EmbeddingModel instance configured for the specified model
     * 
     * @throws IllegalArgumentException if the model name is null or not supported
     * @throws RuntimeException if the model cannot be created or configured
     */
    EmbeddingModel getEmbeddingModel(String sName);

    /**
     * Returns a scoring model instance for the specified model name.
     * <p/>
     * Scoring models evaluate the relevance between queries and text segments,
     * typically used for reranking search results. This method has a default
     * implementation that returns null, indicating that scoring models are
     * optional for model providers.
     *
     * @param sName the name or identifier of the scoring model
     *
     * @return a ScoringModel instance, or null if scoring models are not supported
     * 
     * @throws IllegalArgumentException if the model name is not supported
     * @throws RuntimeException if the model cannot be created or configured
     */
    default ScoringModel getScoringModel(String sName)
        {
        throw new UnsupportedOperationException("Scoring model is not supported by " + getClass().getName());
        }

    /**
     * Returns a chat model instance for the specified model name.
     * <p/>
     * Chat models provide conversational AI capabilities for generating responses
     * to user queries in a blocking manner. These models are suitable for
     * synchronous chat interactions.
     *
     * @param sName the name or identifier of the chat model
     *
     * @return a ChatModel instance configured for the specified model
     * 
     * @throws IllegalArgumentException if the model name is null or not supported
     * @throws RuntimeException if the model cannot be created or configured
     */
    ChatModel getChatModel(String sName);

    /**
     * Returns a streaming chat model instance for the specified model name.
     * <p/>
     * Streaming chat models provide conversational AI capabilities with streaming
     * response generation, allowing for real-time token-by-token response delivery.
     * This is useful for creating responsive chat interfaces.
     *
     * @param sName the name or identifier of the streaming chat model
     *
     * @return a StreamingChatModel instance configured for the specified model
     * 
     * @throws IllegalArgumentException if the model name is null or not supported
     * @throws RuntimeException if the model cannot be created or configured
     */
    StreamingChatModel getStreamingChatModel(String sName);
    }
