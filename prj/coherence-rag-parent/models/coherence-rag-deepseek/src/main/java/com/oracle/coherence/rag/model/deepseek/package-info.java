/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

/**
 * DeepSeek AI model integration for the Oracle Coherence RAG framework.
 * <p/>
 * This package provides integration with DeepSeek AI models for chat and
 * completion functionality. DeepSeek offers high-performance AI models
 * optimized for reasoning and code generation tasks.
 * <p/>
 * Components include:
 * <ul>
 * <li>{@link com.oracle.coherence.rag.model.deepseek.DeepSeekModelProvider} - Main DeepSeek model provider</li>
 * </ul>
 * <p/>
 * The DeepSeek integration provides:
 * <ul>
 * <li>Integration with DeepSeek AI API services</li>
 * <li>Support for DeepSeek chat and completion models</li>
 * <li>OpenAI-compatible API interface</li>
 * <li>Configurable model parameters and settings</li>
 * <li>Error handling and retry logic</li>
 * </ul>
 * <p/>
 * Supported DeepSeek models include:
 * <ul>
 * <li>DeepSeek-V2 (latest reasoning model)</li>
 * <li>DeepSeek-Coder (specialized for code generation)</li>
 * <li>DeepSeek-Math (optimized for mathematical reasoning)</li>
 * </ul>
 * <p/>
 * Authentication and configuration:
 * <ul>
 * <li>API key authentication via environment variables or configuration</li>
 * <li>Configurable base URL for API endpoints</li>
 * <li>Support for custom model parameters</li>
 * <li>Rate limiting and quota management</li>
 * </ul>
 * <p/>
 * Example usage:
 * <pre>{@code
 * ModelProvider provider = new DeepSeekModelProvider();
 * ChatModel chatModel = provider.getChatModel("deepseek-v2");
 * 
 * ChatResponse response = chatModel.generate("Explain quantum computing");
 * }</pre>
 * <p/>
 * Configuration can be provided through:
 * <ul>
 * <li>Environment variable: DEEPSEEK_API_KEY</li>
 * <li>System property: deepseek.api.key</li>
 * <li>CDI configuration beans</li>
 * </ul>
 * <p/>
 * The provider automatically handles API versioning, request formatting,
 * and response parsing to integrate seamlessly with the RAG framework's
 * model abstraction layer.
 *
 * @author Aleks Seovic 2025.07.04
 * @since 25.09
 */
package com.oracle.coherence.rag.model.deepseek; 