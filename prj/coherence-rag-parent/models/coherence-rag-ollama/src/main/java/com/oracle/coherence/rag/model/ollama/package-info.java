/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

/**
 * Ollama local model integration for the Oracle Coherence RAG framework.
 * <p/>
 * This package provides integration with Ollama for running local open-source
 * AI models. Ollama allows you to run large language models locally without
 * requiring external API calls or internet connectivity.
 * <p/>
 * Components include:
 * <ul>
 * <li>{@link com.oracle.coherence.rag.model.ollama.OllamaModelProvider} - Main Ollama model provider</li>
 * </ul>
 * <p/>
 * The Ollama integration provides:
 * <ul>
 * <li>Integration with locally running Ollama instances</li>
 * <li>Support for popular open-source models (Llama, Mistral, CodeLlama, etc.)</li>
 * <li>Local inference without external dependencies</li>
 * <li>Configurable connection parameters</li>
 * <li>Error handling and model availability checking</li>
 * </ul>
 * <p/>
 * Popular models supported by Ollama:
 * <ul>
 * <li>Llama 2 (7B, 13B, 70B parameter variants)</li>
 * <li>Mistral (7B, 8x7B mixture of experts)</li>
 * <li>CodeLlama (specialized for code generation)</li>
 * <li>Alpaca (instruction-tuned models)</li>
 * <li>Vicuna (conversation-optimized models)</li>
 * </ul>
 * <p/>
 * Local deployment advantages:
 * <ul>
 * <li>No external API dependencies</li>
 * <li>Data privacy and security</li>
 * <li>No usage costs or rate limits</li>
 * <li>Offline operation capability</li>
 * <li>Full control over model parameters</li>
 * </ul>
 * <p/>
 * Example usage:
 * <pre>{@code
 * ModelProvider provider = new OllamaModelProvider();
 * ChatModel chatModel = provider.getChatModel("llama2");
 * 
 * ChatResponse response = chatModel.generate("Explain machine learning");
 * }</pre>
 * <p/>
 * Configuration can be provided through:
 * <ul>
 * <li>System property: ollama.base.url (default: http://localhost:11434)</li>
 * <li>Environment variable: OLLAMA_BASE_URL</li>
 * <li>CDI configuration beans</li>
 * </ul>
 * <p/>
 * The provider automatically handles model loading, parameter configuration,
 * and error recovery for locally running Ollama instances. It requires Ollama
 * to be installed and running on the local machine or accessible network endpoint.
 *
 * @author Aleks Seovic 2025.07.04
 * @since 25.09
 */
package com.oracle.coherence.rag.model.ollama; 