/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

/**
 * Internal implementation classes for the Oracle Coherence RAG framework.
 * <p/>
 * This package contains internal implementation classes that provide core
 * functionality for document processing, caching, and vector storage integration.
 * These classes are not intended for direct use by application developers and
 * may change between releases.
 * <p/>
 * Internal components include:
 * <ul>
 * <li>{@link com.oracle.coherence.rag.internal.DocumentCacheLoader} - On-demand document loading with caching</li>
 * <li>{@link com.oracle.coherence.rag.internal.StoreConfigCacheLoader} - Configuration loading and defaults</li>
 * </ul>
 * <p/>
 * These implementations provide:
 * <ul>
 * <li>Lazy loading of documents with caching strategies</li>
 * <li>Integration between Coherence caches and vector stores</li>
 * <li>Configuration management with default value generation</li>
 * <li>Concurrent processing controls and throttling</li>
 * <li>Error handling and recovery mechanisms</li>
 * </ul>
 * <p/>
 * The internal classes leverage Coherence's distributed caching capabilities
 * to provide scalable and fault-tolerant document processing across cluster
 * nodes. They handle the complexity of coordinating between different storage
 * backends and maintaining consistency.
 * <p/>
 * <strong>Note:</strong> Classes in this package are internal implementation
 * details and should not be used directly by application code. Use the public
 * APIs in the parent package instead.
 *
 * @author Aleks Seovic 2025.07.04
 * @since 25.09
 */
package com.oracle.coherence.rag.internal; 
