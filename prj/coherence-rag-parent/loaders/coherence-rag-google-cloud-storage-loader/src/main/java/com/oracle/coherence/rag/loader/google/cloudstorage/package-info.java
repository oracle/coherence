/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

/**
 * Google Cloud Storage document loader implementation for the Oracle Coherence RAG framework.
 * <p/>
 * This package provides integration with Google Cloud Storage for loading documents
 * into the RAG framework. The loader supports Google Cloud authentication methods
 * and provides efficient streaming access to stored documents.
 * <p/>
 * Components include:
 * <ul>
 * <li>{@link com.oracle.coherence.rag.loader.google.cloudstorage.GoogleCloudStorageDocumentLoader} - Main Google Cloud Storage document loader</li>
 * <li>{@link com.oracle.coherence.rag.loader.google.cloudstorage.CdiSupport} - CDI integration and configuration</li>
 * </ul>
 * <p/>
 * The Google Cloud Storage loader provides:
 * <ul>
 * <li>Integration with Google Cloud Storage client libraries</li>
 * <li>Support for Google Cloud authentication methods</li>
 * <li>Efficient streaming and channel-based I/O</li>
 * <li>Automatic content type detection</li>
 * <li>Metadata preservation from Cloud Storage object metadata</li>
 * <li>Error handling for network and authentication issues</li>
 * </ul>
 * <p/>
 * Authentication methods supported:
 * <ul>
 * <li>Service account key file (GOOGLE_APPLICATION_CREDENTIALS)</li>
 * <li>Compute Engine managed service account</li>
 * <li>Google Cloud SDK credentials (gcloud auth)</li>
 * <li>Workload identity (for GKE)</li>
 * </ul>
 * <p/>
 * URI format for Google Cloud Storage documents:
 * <pre>{@code
 * gcs://bucket-name/path/to/document.pdf
 * }</pre>
 * <p/>
 * Example usage:
 * <pre>{@code
 * DocumentLoader loader = new GoogleCloudStorageDocumentLoader();
 * Collection<Document> documents = loader.load("gcs://my-documents/reports/annual-report.pdf");
 * }</pre>
 * <p/>
 * Configuration can be provided through:
 * <ul>
 * <li>Environment variable: GOOGLE_APPLICATION_CREDENTIALS</li>
 * <li>Service account key file path</li>
 * <li>CDI configuration beans</li>
 * <li>Google Cloud SDK default credentials</li>
 * </ul>
 * <p/>
 * The loader uses channel-based I/O for efficient streaming of large documents
 * and automatically handles Google Cloud project detection and billing account
 * configuration.
 *
 * @author Aleks Seovic 2025.07.04
 * @since 25.09
 */
package com.oracle.coherence.rag.loader.google.cloudstorage;
