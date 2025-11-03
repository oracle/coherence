/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.loader.aws.s3;

import com.oracle.coherence.rag.DocumentLoader;

import com.oracle.coherence.rag.loader.FileDocumentLoader;
import com.oracle.coherence.rag.parser.ParserSupplier;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSource;
import dev.langchain4j.data.document.Metadata;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.InputStream;
import java.net.URI;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

/**
 * Document loader implementation for Amazon S3 cloud storage.
 * <p/>
 * This loader enables loading documents from Amazon S3 buckets using the AWS SDK.
 * It implements the {@link DocumentLoader} interface to provide seamless integration
 * with the Coherence RAG document processing pipeline.
 * <p/>
 * The loader extracts documents from S3 using bucket and object key information
 * parsed from the URI, and enriches the document metadata with S3-specific
 * properties such as ETag, content type, and content length.
 * <p/>
 * URI format: {@code s3://bucket-name/path/to/object}
 * <p/>
 * Example usage:
 * <pre>
 * // Inject the loader (CDI managed)
 * &#64;Inject
 * &#64;Named("s3")
 * DocumentLoader loader;
 * 
 * // Load document from S3
 * URI documentUri = URI.create("s3://my-bucket/documents/report.pdf");
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
@Named("s3")
@ApplicationScoped
public class AwsS3DocumentLoader
        implements DocumentLoader
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct {@link FileDocumentLoader} instance.
     *
     * @param parserSupplier the ParserSupplier to use
     */
    @Inject
    public AwsS3DocumentLoader(S3Client client, ParserSupplier parserSupplier)
        {
        m_client = client;
        m_parserSupplier = parserSupplier;
        }

    // ---- DocumentLoader interface ----------------------------------------

    /**
     * Loads a document from Amazon S3 using the provided URI.
     * <p/>
     * The URI should follow the format: {@code s3://bucket-name/path/to/object}
     * where the host part represents the S3 bucket name and the path represents
     * the object key.
     * 
     * @param uri  the S3 URI pointing to the document
     *
     * @return the loaded and parsed document with metadata
     */
    public Document load(URI uri)
        {
        String bucket = uri.getHost();
        String key    = uri.getPath().substring(1);

        var request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        var response = m_client.getObject(request);

        var source = new DocumentSource()
            {
            /**
             * Provides an input stream for reading the document content.
             * 
             * @return the input stream for the S3 object content
             */
            public InputStream inputStream()
                {
                return response;
                }

            /**
             * Provides metadata for the document extracted from S3 object properties.
             * 
             * @return the document metadata including S3-specific properties
             */
            public Metadata metadata()
                {
                GetObjectResponse res = response.response();
                var metadata = Metadata.from(res.metadata());
                metadata.put("url", uri.toString());
                metadata.put("bucket", bucket);
                metadata.put("object", key);
                metadata.put("content_length", res.contentLength());
                metadata.put("content_type", res.contentType());
                metadata.put("etag", res.eTag());
                return metadata;
                }
            };

        return dev.langchain4j.data.document.DocumentLoader.load(source, m_parserSupplier.get());
        }

    // ---- data members ----------------------------------------------------

    /**
     * AWS S3 client for accessing S3 services.
     */
    private final S3Client m_client;

    /**
     * The document parser supplier.
     * <p/>
     * The parser handles the format-specific parsing logic for different
     * types of documents (PDF, DOC, TXT, etc.) and is injected by the
     * CDI container based on the configured parser implementation.
     */
    private final ParserSupplier m_parserSupplier;
    }
