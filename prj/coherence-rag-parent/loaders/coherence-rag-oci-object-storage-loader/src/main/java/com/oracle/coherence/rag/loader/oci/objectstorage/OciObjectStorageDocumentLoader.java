/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.loader.oci.objectstorage;

import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.requests.GetObjectRequest;
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
 * Document loader implementation for Oracle Cloud Infrastructure (OCI) Object Storage.
 * <p/>
 * This loader enables loading documents from OCI Object Storage buckets using the
 * Oracle Cloud Infrastructure SDK. It implements the {@link DocumentLoader} interface
 * to provide seamless integration with the Coherence RAG document processing pipeline.
 * <p/>
 * The loader extracts documents from OCI Object Storage using namespace, bucket, and
 * object name information parsed from the URI, and enriches the document metadata
 * with OCI-specific properties such as content type, content length, and MD5 hash.
 * <p/>
 * URI format: {@code oci.os://namespace/bucket-name/path/to/object}
 * <p/>
 * Example usage:
 * <pre>
 * // Inject the loader (CDI managed)
 * &#64;Inject
 * &#64;Named("oci.os")
 * DocumentLoader loader;
 * 
 * // Load document from OCI Object Storage
 * URI documentUri = URI.create("oci.os://my-namespace/documents/reports/report.pdf");
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
@Named("oci.os")
@ApplicationScoped
public class OciObjectStorageDocumentLoader
        implements DocumentLoader
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct {@link OciObjectStorageDocumentLoader} instance.
     *
     * @param client the OCI Object Storage client to use
     * @param parserSupplier the ParserSupplier to use
     */
    @Inject
    public OciObjectStorageDocumentLoader(ObjectStorageClient client, ParserSupplier parserSupplier)
        {
        m_client = client;
        m_parserSupplier = parserSupplier;
        }

    // ---- DocumentLoader interface ----------------------------------------
    
    /**
     * Loads a document from OCI Object Storage using the provided URI.
     * <p/>
     * The URI should follow the format: {@code oci.os://namespace/bucket-name/path/to/object}
     * where:
     * <ul>
     * <li>host part represents the OCI namespace</li>
     * <li>first path component represents the bucket name</li>
     * <li>remaining path represents the object name</li>
     * </ul>
     * 
     * @param uri  the OCI Object Storage URI pointing to the document
     *
     * @return the loaded and parsed document with metadata
     */
    public Document load(URI uri)
        {
        String ns     = uri.getHost();
        String path   = uri.getPath();
        String bucket = getBucket(path);
        String object = getObject(path);

        var request = GetObjectRequest.builder()
                .namespaceName(ns)
                .bucketName(bucket)
                .objectName(object)
                .build();

        var response = m_client.getObject(request);

        var source = new DocumentSource()
            {
            /**
             * Provides an input stream for reading the document content.
             * 
             * @return the input stream for the OCI object content
             */
            public InputStream inputStream()
                {
                return response.getInputStream();
                }

            /**
             * Provides metadata for the document extracted from OCI Object Storage properties.
             * <p/>
             * Includes OCI-specific metadata such as namespace, bucket, object name,
             * content type, content length, and MD5 hash.
             * 
             * @return the document metadata including OCI-specific properties
             */
            public Metadata metadata()
                {
                var metadata = Metadata.metadata("url", uri.toString());
                metadata.put("ns", ns);
                metadata.put("bucket", bucket);
                metadata.put("object", object);
                metadata.put("content_type", response.getContentType());
                metadata.put("content_md5", response.getContentMd5());
                metadata.put("content_length", response.getContentLength());
                return metadata;
                }
            };

        return dev.langchain4j.data.document.DocumentLoader.load(source, m_parserSupplier.get());
        }

    // ---- helpers ---------------------------------------------------------

    /**
     * Extract bucket name from the URI path.
     *
     * @param path  URI path containing bucket and object name
     *
     * @return the bucket name
     */
    private String getBucket(String path)
        {
        String[] aPath = path.split("/", 3);
        if (aPath.length < 2)
            {
            throw new IllegalArgumentException("Specified URI does not contain bucket name.");
            }

        return aPath[1];
        }

    /**
     * Extract object name from the URI path.
     *
     * @param path  URI path containing bucket and object name
     *
     * @return the object name
     */
    private String getObject(String path)
        {
        var aPath = path.split("/", 3);
        if (aPath.length < 3)
            {
            throw new IllegalArgumentException("Specified URI does not contain object name.");
            }

        return aPath[2];
        }

    // ---- data members ----------------------------------------------------

    /**
     * OCI Object Storage client for accessing storage services.
     */
    private final ObjectStorageClient m_client;

    /**
     * The document parser supplier.
     * <p/>
     * The parser handles the format-specific parsing logic for different
     * types of documents (PDF, DOC, TXT, etc.) and is injected by the
     * CDI container based on the configured parser implementation.
     */
    private final ParserSupplier m_parserSupplier;
    }
