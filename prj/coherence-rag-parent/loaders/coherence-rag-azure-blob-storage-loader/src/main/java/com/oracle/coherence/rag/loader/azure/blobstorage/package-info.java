/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

/**
 * Azure Blob Storage document loader implementation for the Oracle Coherence RAG framework.
 * <p/>
 * This package provides integration with Microsoft Azure Blob Storage for loading
 * documents into the RAG framework. The loader supports various Azure authentication
 * methods and can work with both Azure Storage accounts and Azure Data Lake Storage.
 * <p/>
 * Components include:
 * <ul>
 * <li>{@link com.oracle.coherence.rag.loader.azure.blobstorage.AzureBlobStorageDocumentLoader} - Main Azure Blob Storage document loader</li>
 * <li>{@link com.oracle.coherence.rag.loader.azure.blobstorage.CdiSupport} - CDI integration and configuration</li>
 * </ul>
 * <p/>
 * The Azure Blob Storage loader provides:
 * <ul>
 * <li>Integration with Azure SDK for Blob Storage operations</li>
 * <li>Support for multiple authentication methods (connection string, managed identity, service principal)</li>
 * <li>Automatic content type detection</li>
 * <li>Metadata preservation from blob metadata and tags</li>
 * <li>Error handling for network and authentication issues</li>
 * </ul>
 * <p/>
 * Authentication methods supported:
 * <ul>
 * <li>Connection string (preferred for development)</li>
 * <li>Managed identity (recommended for production)</li>
 * <li>Service principal with client secret</li>
 * <li>Service principal with certificate</li>
 * <li>Azure CLI credentials</li>
 * </ul>
 * <p/>
 * URI format for Azure Blob Storage documents:
 * <pre>{@code
 * azure.blob://container-name/path/to/document.pdf
 * }</pre>
 * <p/>
 * Example usage:
 * <pre>{@code
 * DocumentLoader loader = new AzureBlobStorageDocumentLoader();
 * Collection<Document> documents = loader.load("azure.blob://documents/manuals/user-guide.pdf");
 * }</pre>
 * <p/>
 * Configuration can be provided through:
 * <ul>
 * <li>Environment variable: AZURE_STORAGE_CONNECTION_STRING</li>
 * <li>System property: azure.storage.connection-string</li>
 * <li>CDI configuration beans</li>
 * </ul>
 * <p/>
 * The loader automatically handles Azure region detection and service endpoint
 * configuration, including support for sovereign clouds and custom endpoints.
 *
 * @author Aleks Seovic 2025.07.04
 * @since 25.09
 */
package com.oracle.coherence.rag.loader.azure.blobstorage;
