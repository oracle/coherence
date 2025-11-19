/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

/**
 * Anthropic model integration for the Oracle Coherence RAG framework.
 * <p/>
 * This package provides integration with Anthropic for running open-source AI models.
 * <p/>
 * Components include:
 * <ul>
 * <li>{@link com.oracle.coherence.rag.model.anthropic.AnthropicModelProvider} - Main Anthropic model provider</li>
 * </ul>
 * <p/>
 * The Anthropic integration provides:
 * <ul>
 * <li>Support for popular open-source models (Claude 4 Sonnet, Claude 3.7 Sonnet, Claude 3.5 Haiku, etc.)</li>
 * <li>Configurable connection parameters</li>
 * <li>Error handling and model availability checking</li>
 * </ul>
 * <p/>
 * Popular models supported by Anthropic:
 * <ul>
 * <li>Claude 4 Sonnet</li>
 * <li>Claude 3.7 Sonnet</li>
 * <li>Claude 3.5 Haiku</li>
 * </ul>
 * <p/>
 * Example usage:
 * <pre>{@code
 * ModelProvider provider = new AnthropicModelProvider();
 * ChatModel chatModel = provider.getChatModel("claude-3-7-sonnet-20250219");
 * 
 * ChatResponse response = chatModel.generate("Explain machine learning");
 * }</pre>
 * <p/>
 * Configuration can be provided through:
 * <ul>
 * <li>System property: anthropic.base.url (default: https://api.anthropic.com/v1/)</li>
 * <li>Environment variable: ANTHROPIC_BASE_URL</li>
 * <li>CDI configuration beans</li>
 * </ul>
 * <p/>
 *
 * @author Aleks Seovic 2025.07.04
 * @since 25.09
 */
package com.oracle.coherence.rag.model.anthropic;