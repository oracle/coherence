/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.loader.google.cloudstorage;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.oracle.coherence.rag.DocumentLoader;
import com.oracle.coherence.rag.parser.ParserSupplier;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSource;
import dev.langchain4j.data.document.Metadata;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.channels.Channels;
import java.util.Collections;
import java.util.Map;

/**
 * Document loader implementation for Google Cloud Storage.
 * <p/>
 * This loader enables loading documents from Google Cloud Storage buckets using
 * the Google Cloud Storage client library. It implements the {@link DocumentLoader}
 * interface to provide seamless integration with the Coherence RAG document
 * processing pipeline.
 * <p/>
 * The loader extracts documents from Google Cloud Storage using bucket and object
 * name information parsed from the URI, and enriches the document metadata with
 * GCS-specific properties such as ETag, content type, and content length.
 * <p/>
 * URI format: {@code gcs://bucket-name/path/to/object}
 * <p/>
 * Example usage:
 * <pre>
 * // Inject the loader (CDI managed)
 * &#64;Inject
 * &#64;Named("gcs")
 * DocumentLoader loader;
 * 
 * // Load document from Google Cloud Storage
 * URI documentUri = URI.create("gcs://my-bucket/documents/report.pdf");
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
@Named("gcs")
@ApplicationScoped
public class GoogleCloudStorageDocumentLoader
        implements DocumentLoader
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct {@link GoogleCloudStorageDocumentLoader} instance.
     *
     * @param client the Google Cloud Storage client to use
     * @param parserSupplier the ParserSupplier to use
     */
    @Inject
    public GoogleCloudStorageDocumentLoader(Storage client, ParserSupplier parserSupplier)
        {
        m_client = client;
        m_parserSupplier = parserSupplier;
        }

    // ---- DocumentLoader interface ----------------------------------------

    /**
     * Loads a document from Google Cloud Storage using the provided URI.
     * <p/>
     * The URI should follow the format: {@code gcs://bucket-name/path/to/object}
     * where the host part represents the bucket name and the path represents
     * the object name.
     * 
     * @param uri  the Google Cloud Storage URI pointing to the document
     *
     * @return the loaded and parsed document with metadata
     */
    public Document load(URI uri)
        {
        String bucket = uri.getHost();
        String object = uri.getPath().substring(1);

        BlobId blobId = BlobId.of(bucket, object);
        Blob   blob   = m_client.get(blobId);

        var source = new DocumentSource()
            {
            /**
             * Provides an input stream for reading the document content.
             * <p/>
             * Uses the Google Cloud Storage blob reader to create a channel-based
             * input stream for efficient content reading.
             * 
             * @return the input stream for the storage object content
             * @throws IOException  if there's an error accessing the content
             */
            public InputStream inputStream() throws IOException
                {
                return Channels.newInputStream(blob.reader());
                }

            /**
             * Provides metadata for the document extracted from Google Cloud Storage object properties.
             * <p/>
             * Includes both custom metadata set on the storage object as well as
             * system properties like content type, size, and ETag.
             * 
             * @return the document metadata including GCS-specific properties
             */
            public Metadata metadata()
                {
                Map<String, String> properties = blob.getMetadata() == null
                                                 ? Collections.emptyMap()
                                                 : blob.getMetadata();
                var metadata = Metadata.from(properties);

                metadata.put("url", uri.toString());
                metadata.put("bucket", bucket);
                metadata.put("object", object);
                metadata.put("content_length", blob.getSize());
                metadata.put("content_type", blob.getContentType());
                metadata.put("etag", blob.getEtag());

                return metadata;
                }
            };

        return dev.langchain4j.data.document.DocumentLoader.load(source, m_parserSupplier.get());
        }

    // ---- data members ----------------------------------------------------

    /**
     * Google Cloud Storage client for accessing storage services.
     */
    private final Storage m_client;

    /**
     * The document parser supplier.
     * <p/>
     * The parser handles the format-specific parsing logic for different
     * types of documents (PDF, DOC, TXT, etc.) and is injected by the
     * CDI container based on the configured parser implementation.
     */
    private final ParserSupplier m_parserSupplier;
    }
