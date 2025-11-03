/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

/**
 * Oracle Cloud Infrastructure (OCI) GenAI model integration for the Oracle Coherence RAG framework.
 * <p/>
 * This package provides comprehensive integration with OCI GenAI service for both
 * chat and embedding models. It supports multiple authentication methods and
 * provides optimized implementations for both synchronous and streaming operations.
 * <p/>
 * Components include:
 * <ul>
 * <li>{@link com.oracle.coherence.rag.model.oci.OciModelProvider} - Main OCI GenAI model provider</li>
 * <li>{@link com.oracle.coherence.rag.model.oci.OciGenAiChatModel} - Chat model implementation</li>
 * <li>{@link com.oracle.coherence.rag.model.oci.OciGenAiStreamingChatModel} - Streaming chat model</li>
 * <li>{@link com.oracle.coherence.rag.model.oci.OciGenAiEmbeddingModel} - Embedding model implementation</li>
 * <li>{@link com.oracle.coherence.rag.model.oci.AbstractOciModelBuilder} - CDI configuration and authentication</li>
 * </ul>
 * <p/>
 * The OCI GenAI integration provides:
 * <ul>
 * <li>Support for Cohere Command and generic chat models</li>
 * <li>Real-time streaming chat with server-sent events</li>
 * <li>Embedding models with batch processing and truncation</li>
 * <li>Multiple authentication methods (config file, instance principals, manual)</li>
 * <li>Automatic model format detection and parameter mapping</li>
 * <li>Comprehensive error handling and retry logic</li>
 * </ul>
 * <p/>
 * Supported OCI GenAI models:
 * <ul>
 * <li>Cohere Command (chat and completion)</li>
 * <li>Cohere Command-Light (lightweight chat)</li>
 * <li>Cohere Embed (text embeddings)</li>
 * <li>Generic chat models via the unified API</li>
 * </ul>
 * <p/>
 * Authentication methods supported:
 * <ul>
 * <li>OCI config file (~/.oci/config)</li>
 * <li>Instance principals (recommended for compute instances)</li>
 * <li>Resource principals (for Functions and other services)</li>
 * <li>Manual authentication with private key</li>
 * </ul>
 * <p/>
 * The streaming chat model provides real-time token streaming with comprehensive
 * event handling for different response types including text, citations, and
 * finish reasons.
 * <p/>
 * Example usage:
 * <pre>{@code
 * ModelProvider provider = new OciModelProvider();
 * ChatModel chatModel = provider.getChatModel("cohere.command");
 * EmbeddingModel embeddingModel = provider.getEmbeddingModel("cohere.embed-english-v3.0");
 * 
 * ChatResponse response = chatModel.generate("What is Oracle Cloud?");
 * Embedding embedding = embeddingModel.embed("Document text to embed");
 * }</pre>
 * <p/>
 * Configuration can be provided through:
 * <ul>
 * <li>OCI configuration file</li>
 * <li>Environment variables (OCI_CONFIG_FILE, OCI_CONFIG_PROFILE)</li>
 * <li>System properties for authentication parameters</li>
 * <li>CDI configuration beans</li>
 * </ul>
 *
 * @author Aleks Seovic 2025.07.04
 * @since 25.09
 */
package com.oracle.coherence.rag.model.oci; 
