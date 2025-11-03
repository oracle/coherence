/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

/**
 * AI model integration classes for the Oracle Coherence RAG framework.
 * <p/>
 * This package provides abstract base classes and common functionality for
 * integrating with various AI models and services. It includes model suppliers
 * that handle caching, connection pooling, and configuration for different
 * types of AI models used in RAG applications.
 * <p/>
 * Core model integration classes include:
 * <ul>
 * <li>{@link com.oracle.coherence.rag.model.AbstractModelSupplier} - Base class for model suppliers</li>
 * <li>{@link com.oracle.coherence.rag.model.ChatModelSupplier} - Supplier for chat/completion models</li>
 * <li>{@link com.oracle.coherence.rag.model.StreamingChatModelSupplier} - Supplier for streaming chat/completion models</li>
 * <li>{@link com.oracle.coherence.rag.model.EmbeddingModelSupplier} - Supplier for text embedding models</li>
 * <li>{@link com.oracle.coherence.rag.model.ScoringModelSupplier} - Supplier for document scoring models</li>
 * </ul>
 * <p/>
 * Local ONNX model support:
 * <ul>
 * <li>{@link com.oracle.coherence.rag.model.LocalOnnxEmbeddingModel} - Local embedding models via ONNX Runtime</li>
 * <li>{@link com.oracle.coherence.rag.model.LocalOnnxScoringModel} - Local scoring models via ONNX Runtime</li>
 * <li>{@link com.oracle.coherence.rag.model.OnnxEmbeddingModel} - BERT bi-encoder for embeddings</li>
 * <li>{@link com.oracle.coherence.rag.model.OnnxBertCrossEncoder} - BERT cross-encoder for scoring</li>
 * </ul>
 * <p/>
 * The model integration system provides:
 * <ul>
 * <li>Connection pooling and caching for efficient resource usage</li>
 * <li>Support for both local ONNX models and remote API services</li>
 * <li>GPU and CPU execution modes for ONNX models</li>
 * <li>Configurable model parameters and settings</li>
 * <li>Integration with LangChain4J model interfaces</li>
 * </ul>
 * <p/>
 * Remote model providers are available in separate modules:
 * <ul>
 * <li>OpenAI GPT models - coherence-rag-open-ai</li>
 * <li>OCI GenAI service - coherence-rag-oci</li>
 * <li>Ollama local models - coherence-rag-ollama</li>
 * <li>DeepSeek models - coherence-rag-deepseek</li>
 * </ul>
 *
 * @author Aleks Seovic 2025.07.04
 * @since 25.09
 */
package com.oracle.coherence.rag.model; 
