/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

/**
 * Configuration classes for the Oracle Coherence RAG framework.
 * <p/>
 * This package contains configuration classes that define how document stores,
 * indexes, and processing pipelines are configured within the RAG framework.
 * These classes provide type-safe configuration with validation and default
 * value handling.
 * <p/>
 * Configuration classes include:
 * <ul>
 * <li>{@link com.oracle.coherence.rag.config.StoreConfig} - Vector store configuration and settings</li>
 * <li>{@link com.oracle.coherence.rag.config.index.IndexConfig} - Vector index configuration and parameters</li>
 * </ul>
 * <p/>
 * The configuration system supports:
 * <ul>
 * <li>JSON serialization for persistence and API transport</li>
 * <li>Default value handling with sensible defaults</li>
 * <li>Validation of configuration parameters</li>
 * <li>Integration with Coherence cache configuration</li>
 * <li>Dynamic configuration updates</li>
 * </ul>
 * <p/>
 * Configuration objects are typically loaded from external sources such as
 * files, databases, or configuration services, and can be updated at runtime
 * to modify system behavior.
 * <p/>
 * Example configuration usage:
 * <pre>{@code
 * StoreConfig config = StoreConfig.builder()
 *     .chunkSize(1000)
 *     .chunkOverlap(200)
 *     .embeddingModel("all-MiniLM-L6-v2")
 *     .vectorStore("coherence")
 *     .build();
 * }</pre>
 *
 * @author Aleks Seovic 2025.07.04
 * @since 25.09
 */
package com.oracle.coherence.rag.config; 
