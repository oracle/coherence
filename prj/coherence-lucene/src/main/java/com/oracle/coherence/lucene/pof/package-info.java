/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

/**
 * Provides Portable Object Format (POF) serialization support for Lucene classes.
 * This package contains serializers and configuration providers that enable
 * efficient binary serialization of Lucene queries and related objects.
 * 
 * <p>The main components in this package are:
 * <ul>
 *   <li>{@link com.oracle.coherence.lucene.pof.LucenePofSerializers} - Collection of
 *       POF serializers for various Lucene query types</li>
 *   <li>{@link com.oracle.coherence.lucene.pof.LucenePofConfigProvider} - Provider
 *       that automatically registers Lucene POF serializers</li>
 * </ul>
 * 
 * <p>The serializers in this package support all common Lucene query types:
 * <ul>
 *   <li>Term queries</li>
 *   <li>Boolean queries</li>
 *   <li>Phrase queries</li>
 *   <li>Wildcard queries</li>
 *   <li>Prefix queries</li>
 *   <li>Fuzzy queries</li>
 *   <li>Regular expression queries</li>
 *   <li>Boost queries</li>
 * </ul>
 *
 * @author Aleks Seovic  2025.05.16
 * @since 25.09
 */
package com.oracle.coherence.lucene.pof;
