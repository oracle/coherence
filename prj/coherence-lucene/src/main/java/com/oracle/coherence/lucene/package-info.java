/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

/**
 * Provides integration between Oracle Coherence and Apache Lucene for distributed
 * full-text search capabilities. This package contains the core classes for
 * setting up and performing full-text search operations across distributed caches.
 * 
 * <p>The main components in this package are:
 * <ul>
 *   <li>{@link com.oracle.coherence.lucene.LuceneIndex} - A Coherence MapIndex implementation
 *       that maintains Lucene indexes for cache entries</li>
 *   <li>{@link com.oracle.coherence.lucene.LuceneSearch} - An aggregator that performs
 *       distributed full-text search across cache partitions</li>
 *   <li>{@link com.oracle.coherence.lucene.LuceneQueryParser} - A utility class for building
 *       Lucene queries from text input</li>
 * </ul>
 * 
 * <p>Example usage:
 * <pre>
 * NamedMap&lt;K, V&gt; cache = coherence.getSession().getMap("documents");
 * cache.addIndex(new LuceneIndex&lt;&gt;(TEXT_EXTRACTOR));
 * 
 * LuceneQueryParser parser = LuceneQueryParser.create(TEXT_EXTRACTOR);
 * List&lt;QueryResult&lt;K, V&gt;&gt; results = 
 *     cache.aggregate(new LuceneSearch&lt;&gt;(TEXT_EXTRACTOR, parser.parse("search query"), 10));
 * </pre>
 *
 * @author Aleks Seovic  2025.05.16
 * @since 25.09
 */
package com.oracle.coherence.lucene;
