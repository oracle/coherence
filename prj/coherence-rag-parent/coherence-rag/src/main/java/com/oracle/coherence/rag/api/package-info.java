/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

/**
 * REST API controllers for the Oracle Coherence RAG framework.
 * <p/>
 * This package contains JAX-RS REST API endpoints that provide external access
 * to the RAG framework functionality. The API controllers handle document
 * ingestion, vector storage operations, configuration management, and document
 * relevance scoring.
 * <p/>
 * API endpoints include:
 * <ul>
 * <li>{@link com.oracle.coherence.rag.api.Store} - Document store management and ingestion</li>
 * <li>{@link com.oracle.coherence.rag.api.Kb} - Cross-store database operations</li>
 * <li>{@link com.oracle.coherence.rag.api.Config} - Configuration management</li>
 * <li>{@link com.oracle.coherence.rag.api.Scoring} - Document relevance scoring</li>
 * </ul>
 * <p/>
 * The API supports:
 * <ul>
 * <li>Document upload and processing with automatic chunking</li>
 * <li>Vector embedding generation and storage</li>
 * <li>Similarity search and retrieval</li>
 * <li>Store configuration and management</li>
 * <li>Cross-origin resource sharing (CORS)</li>
 * </ul>
 * <p/>
 * All endpoints use JSON for request/response serialization and support
 * asynchronous processing for large document operations.
 * <p/>
 * Example API usage:
 * <pre>{@code
 * // Upload and process a document
 * POST /api/store/{storeName}/documents
 * Content-Type: multipart/form-data
 * 
 * // Search for similar documents  
 * POST /api/store/{storeName}/search
 * Content-Type: application/json
 * {"query": "What is machine learning?", "maxResults": 10}
 * }</pre>
 *
 * @author Aleks Seovic 2025.07.04
 * @since 25.09
 */
package com.oracle.coherence.rag.api; 
