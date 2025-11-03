/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

/**
 * POF (Portable Object Format) serialization support for the Oracle Coherence RAG framework.
 * <p/>
 * This package provides Coherence POF serialization support for RAG framework
 * objects to enable efficient binary serialization and network transport within
 * Coherence clusters. POF serialization is critical for performance when working
 * with large document objects and vector embeddings.
 * <p/>
 * POF serialization components include:
 * <ul>
 * <li>{@link com.oracle.coherence.rag.internal.pof.DocumentSerializer} - Custom serializer for Document objects</li>
 * <li>{@link com.oracle.coherence.rag.internal.pof.PofConfigProvider} - POF configuration provider service</li>
 * </ul>
 * <p/>
 * The POF serialization support provides:
 * <ul>
 * <li>Efficient binary serialization of document and chunk objects</li>
 * <li>Optimized serialization for vector embeddings (float arrays)</li>
 * <li>Preservation of document metadata and structure</li>
 * <li>Integration with Coherence cluster serialization</li>
 * <li>Backward compatibility for schema evolution</li>
 * </ul>
 * <p/>
 * POF serialization is automatically configured when the RAG framework is used
 * within a Coherence cluster. The serializers handle complex object graphs
 * including nested metadata maps and large text content efficiently.
 * <p/>
 * The POF configuration is registered via the service provider mechanism and
 * integrates seamlessly with Coherence's serialization infrastructure. Custom
 * serializers ensure optimal performance for frequently serialized objects
 * like document chunks and embeddings.
 * <p/>
 * Example POF configuration:
 * <pre>{@code
 * <pof-config>
 *   <user-type-list>
 *     <user-type>
 *       <type-id>1001</type-id>
 *       <class-name>com.oracle.coherence.ai.DocumentChunk</class-name>
 *       <serializer>
 *         <class-name>com.oracle.coherence.rag.pof.DocumentSerializer</class-name>
 *       </serializer>
 *     </user-type>
 *   </user-type-list>
 * </pof-config>
 * }</pre>
 *
 * @author Aleks Seovic 2025.07.04
 * @since 25.09
 */
package com.oracle.coherence.rag.internal.pof;
