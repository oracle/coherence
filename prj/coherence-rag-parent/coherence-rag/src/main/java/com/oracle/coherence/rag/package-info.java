/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

/**
 * Core Oracle Coherence RAG (Retrieval Augmented Generation) framework.
 * <p/>
 * This package provides the foundational interfaces and implementations for building 
 * RAG applications on top of Oracle Coherence. The framework integrates with LangChain4J
 * to provide comprehensive document processing, vector storage, and AI model integration
 * capabilities.
 * <p/>
 * Key components include:
 * <ul>
 * <li>{@link com.oracle.coherence.rag.VectorStore} - Core interface for vector storage backends</li>
 * <li>{@link com.oracle.coherence.rag.DocumentLoader} - Interface for loading documents from various sources</li>
 * <li>{@link com.oracle.coherence.rag.ModelProvider} - Interface for AI model integrations</li>
 * <li>{@link com.oracle.coherence.rag.ChatAssistant} - Main entry point for RAG chat interactions</li>
 * </ul>
 * <p/>
 * The framework supports multiple vector storage backends including Oracle Coherence,
 * Oracle Database, OpenSearch, and REST APIs. Document loaders are available for various
 * cloud storage services, and model providers support multiple AI services including
 * OpenAI, OCI GenAI, and local ONNX models.
 * <p/>
 * Example usage:
 * <pre>{@code
 * // Basic RAG setup with Coherence vector store
 * VectorStore vectorStore = coherenceVectorStore;
 * DocumentLoader loader = new FileDocumentLoader();
 * ModelProvider modelProvider = new OpenAiModelProvider();
 * 
 * ChatAssistant assistant = new ChatAssistant(vectorStore, modelProvider);
 * String response = assistant.chat("What is Oracle Coherence?");
 * }</pre>
 *
 * @author Aleks Seovic 2025.07.04
 * @since 25.09
 */
package com.oracle.coherence.rag; 