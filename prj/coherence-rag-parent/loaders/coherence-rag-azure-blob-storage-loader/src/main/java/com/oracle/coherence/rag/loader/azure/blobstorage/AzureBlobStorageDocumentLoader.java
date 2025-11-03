/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.loader.azure.blobstorage;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;

import com.oracle.coherence.rag.DocumentLoader;
import com.oracle.coherence.rag.parser.ParserSupplier;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSource;
import dev.langchain4j.data.document.Metadata;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.InputStream;
import java.net.URI;

/**
 * Document loader implementation for Azure Blob Storage.
 * <p/>
 * This loader enables loading documents from Azure Blob Storage containers using
 * the Azure Storage SDK. It implements the {@link DocumentLoader} interface to
 * provide seamless integration with the Coherence RAG document processing pipeline.
 * <p/>
 * The loader extracts documents from Azure Blob Storage using container and blob
 * name information parsed from the URI, and enriches the document metadata with
 * Azure-specific properties such as ETag, content type, and content length.
 * <p/>
 * URI format: {@code azure.blob://container-name/path/to/blob}
 * <p/>
 * Example usage:
 * <pre>
 * // Inject the loader (CDI managed)
 * &#64;Inject
 * &#64;Named("azure.blob")
 * DocumentLoader loader;
 * 
 * // Load document from Azure Blob Storage
 * URI documentUri = URI.create("azure.blob://documents/reports/report.pdf");
 * Document document = loader.load(documentUri);
 * 
 * // Access content and metadata
 * String content = document.text();
 * String contentType = document.metadata().getString("content_type");
 * Long contentLength = document.metadata().getLong("content_length");
 * </pre>
 *
 * @author Aleks Seovic  2025.07.04
 * @since 25.09
 */
@Named("azure.blob")
@ApplicationScoped
public class AzureBlobStorageDocumentLoader
        implements DocumentLoader
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct {@link AzureBlobStorageDocumentLoader} instance.
     *
     * @param client the Azure Blob Storage service client to use
     * @param parserSupplier the ParserSupplier to use
     */
    @Inject
    public AzureBlobStorageDocumentLoader(BlobServiceClient client, ParserSupplier parserSupplier)
        {
        m_client = client;
        m_parserSupplier = parserSupplier;
        }

    // ---- DocumentLoader interface ----------------------------------------

    /**
     * Loads a document from Azure Blob Storage using the provided URI.
     * <p/>
     * The URI should follow the format: {@code azure.blob://container-name/path/to/blob}
     * where the host part represents the container name and the path represents
     * the blob name.
     * 
     * @param uri  the Azure Blob Storage URI pointing to the document
     *
     * @return the loaded and parsed document with metadata
     */
    public Document load(URI uri)
        {
        String bucket = uri.getHost();
        String object = uri.getPath().substring(1);

        // Get a reference to the container and blob
        BlobContainerClient containerClient = m_client.getBlobContainerClient(bucket);
        BlobClient          blobClient      = containerClient.getBlobClient(object);

        var source = new DocumentSource()
            {
            /**
             * Provides an input stream for reading the document content.
             * 
             * @return the input stream for the blob content
             */
            public InputStream inputStream()
                {
                return blobClient.openInputStream();
                }

            /**
             * Provides metadata for the document extracted from Azure blob properties.
             * 
             * @return the document metadata including Azure-specific properties
             */
            public Metadata metadata()
                {
                var properties = blobClient.getProperties();
                var metadata   = Metadata.from(properties.getMetadata());

                metadata.put("url", uri.toString());
                metadata.put("bucket", bucket);
                metadata.put("object", object);
                metadata.put("content_length", properties.getBlobSize());
                metadata.put("content_type", properties.getContentType());
                metadata.put("etag", properties.getETag());

                return metadata;
                }
            };

        return dev.langchain4j.data.document.DocumentLoader.load(source, m_parserSupplier.get());
        }

    // ---- data members ----------------------------------------------------

    /**
     * Azure Blob Storage service client for accessing storage services.
     */
    private final BlobServiceClient m_client;

    /**
     * The document parser supplier.
     * <p/>
     * The parser handles the format-specific parsing logic for different
     * types of documents (PDF, DOC, TXT, etc.) and is injected by the
     * CDI container based on the configured parser implementation.
     */
    private final ParserSupplier m_parserSupplier;
    }
