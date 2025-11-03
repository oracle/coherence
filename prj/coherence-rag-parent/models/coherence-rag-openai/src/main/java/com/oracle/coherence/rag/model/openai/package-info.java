/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

/**
 * OpenAI model integration for the Oracle Coherence RAG framework.
 * <p/>
 * This package provides integration with OpenAI's language models and embedding
 * services, including GPT models for chat and completion tasks, and text embedding
 * models for vector similarity search.
 * <p/>
 * Components include:
 * <ul>
 * <li>{@link com.oracle.coherence.rag.model.openai.OpenAiModelProvider} - Main OpenAI model provider</li>
 * </ul>
 * <p/>
 * The OpenAI integration provides:
 * <ul>
 * <li>Integration with OpenAI API services</li>
 * <li>Support for GPT chat and completion models</li>
 * <li>Text embedding models for semantic search</li>
 * <li>Configurable model parameters and settings</li>
 * <li>Error handling and retry logic</li>
 * <li>Rate limiting and quota management</li>
 * </ul>
 * <p/>
 * Supported OpenAI models:
 * <ul>
 * <li>GPT-4 (latest reasoning model)</li>
 * <li>GPT-4 Turbo (optimized for speed and cost)</li>
 * <li>GPT-3.5 Turbo (cost-effective chat model)</li>
 * <li>text-embedding-ada-002 (primary embedding model)</li>
 * <li>text-embedding-3-small (efficient embedding model)</li>
 * <li>text-embedding-3-large (high-performance embedding model)</li>
 * </ul>
 * <p/>
 * Authentication and configuration:
 * <ul>
 * <li>API key authentication via environment variables or configuration</li>
 * <li>Organization ID support for team accounts</li>
 * <li>Configurable base URL for compatible services</li>
 * <li>Custom model parameters (temperature, max tokens, etc.)</li>
 * </ul>
 * <p/>
 * Example usage:
 * <pre>{@code
 * ModelProvider provider = new OpenAiModelProvider();
 * ChatModel chatModel = provider.getChatModel("gpt-4");
 * EmbeddingModel embeddingModel = provider.getEmbeddingModel("text-embedding-ada-002");
 * 
 * ChatResponse response = chatModel.generate("Explain quantum computing");
 * Embedding embedding = embeddingModel.embed("Document text to embed");
 * }</pre>
 * <p/>
 * Configuration can be provided through:
 * <ul>
 * <li>Environment variable: OPENAI_API_KEY</li>
 * <li>System property: openai.api.key</li>
 * <li>CDI configuration beans</li>
 * </ul>
 * <p/>
 * The provider automatically handles API versioning, request formatting,
 * response parsing, and error recovery. It integrates seamlessly with the
 * RAG framework's model abstraction layer and provides optimal performance
 * for both chat and embedding operations.
 *
 * @author Aleks Seovic 2025.07.04
 * @since 25.09
 */
package com.oracle.coherence.rag.model.openai; 