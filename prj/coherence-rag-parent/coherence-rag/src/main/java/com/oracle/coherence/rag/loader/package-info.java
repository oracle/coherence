/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

/**
 * Document loader implementations for the Oracle Coherence RAG framework.
 * <p/>
 * This package provides implementations of the {@link com.oracle.coherence.rag.DocumentLoader}
 * interface for loading documents from various sources. These loaders handle the
 * retrieval and initial processing of documents before they are chunked and
 * converted to vector embeddings.
 * <p/>
 * Available document loaders include:
 * <ul>
 * <li>{@link com.oracle.coherence.rag.loader.FileDocumentLoader} - Load documents from local filesystem</li>
 * <li>{@link com.oracle.coherence.rag.loader.HttpDocumentLoader} - Load documents from HTTP URLs</li>
 * <li>{@link com.oracle.coherence.rag.loader.HttpsDocumentLoader} - Load documents from HTTPS URLs</li>
 * </ul>
 * <p/>
 * The loaders support:
 * <ul>
 * <li>Multiple document formats (PDF, Word, text, etc.) via Apache Tika</li>
 * <li>Metadata extraction and preservation</li>
 * <li>Content type detection and validation</li>
 * <li>Error handling and retry logic for network sources</li>
 * <li>Integration with LangChain4J document parsing</li>
 * </ul>
 * <p/>
 * Additional cloud storage loaders are available in separate modules:
 * <ul>
 * <li>AWS S3 - coherence-rag-aws-s3-loader</li>
 * <li>Azure Blob Storage - coherence-rag-azure-blob-storage-loader</li>
 * <li>Google Cloud Storage - coherence-rag-google-cloud-storage-loader</li>
 * <li>OCI Object Storage - coherence-rag-oci-object-storage-loader</li>
 * </ul>
 * <p/>
 * Example usage:
 * <pre>{@code
 * DocumentLoader loader = new FileDocumentLoader();
 * Collection<Document> documents = loader.load("file:///path/to/document.pdf");
 * 
 * // Or load from URL
 * DocumentLoader httpLoader = new HttpsDocumentLoader();
 * Collection<Document> webDocs = httpLoader.load("https://example.com/doc.pdf");
 * }</pre>
 *
 * @author Aleks Seovic 2025.07.04
 * @since 25.09
 */
package com.oracle.coherence.rag.loader; 