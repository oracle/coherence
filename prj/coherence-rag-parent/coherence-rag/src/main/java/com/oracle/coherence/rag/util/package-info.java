/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

/**
 * Utility classes for the Oracle Coherence RAG framework.
 * <p/>
 * This package contains utility classes that provide common functionality
 * used throughout the RAG framework. These utilities handle CDI integration,
 * text formatting, performance timing, HTTP configuration, and distributed
 * query planning.
 * <p/>
 * Utility classes include:
 * <ul>
 * <li>{@link com.oracle.coherence.rag.util.CdiHelper} - CDI integration and bean resolution utilities</li>
 * <li>{@link com.oracle.coherence.rag.util.Formatting} - Text formatting and display utilities</li>
 * <li>{@link com.oracle.coherence.rag.util.Timer} - Performance timing and measurement utilities</li>
 * <li>{@link com.oracle.coherence.rag.util.ShardRetrievalPlan} - Distributed query optimization</li>
 * </ul>
 * <p/>
 * The utilities provide:
 * <ul>
 * <li>CDI bean lookup and dependency injection support</li>
 * <li>Consistent text formatting across the framework</li>
 * <li>Performance monitoring and timing capabilities</li>
 * <li>Automatic HTTP proxy configuration for network clients</li>
 * <li>Optimization of distributed queries across Coherence partitions</li>
 * </ul>
 * <p/>
 * These utilities are designed to be lightweight and focused on specific
 * functionality areas. They integrate well with Coherence's distributed
 * architecture and provide consistent behavior across different deployment
 * environments.
 * <p/>
 * The HttpProxyExtension automatically configures HTTP clients to use
 * system proxy settings, while the ShardRetrievalPlan optimizes distributed
 * queries by planning efficient retrieval across cluster partitions.
 * <p/>
 * Example usage:
 * <pre>{@code
 * // Use CDI helper to resolve beans
 * VectorStore store = CdiHelper.getBean(VectorStore.class);
 * 
 * // Format text for display
 * String formatted = Formatting.formatSize(documentSize);
 * 
 * // Time operations
 * Timer timer = Timer.start();
 * performOperation();
 * long duration = timer.stop();
 * }</pre>
 *
 * @author Aleks Seovic 2025.07.04
 * @since 25.09
 */
package com.oracle.coherence.rag.util; 
