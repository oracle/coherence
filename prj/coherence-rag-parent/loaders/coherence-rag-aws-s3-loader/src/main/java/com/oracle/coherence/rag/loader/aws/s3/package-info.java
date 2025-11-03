/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

/**
 * AWS S3 document loader implementation for the Oracle Coherence RAG framework.
 * <p/>
 * This package provides integration with Amazon S3 for loading documents
 * into the RAG framework. The loader supports standard S3 authentication
 * methods and can load documents from any S3-compatible storage service.
 * <p/>
 * Components include:
 * <ul>
 * <li>{@link com.oracle.coherence.rag.loader.aws.s3.AwsS3DocumentLoader} - Main S3 document loader</li>
 * <li>{@link com.oracle.coherence.rag.loader.aws.s3.CdiSupport} - CDI integration and configuration</li>
 * </ul>
 * <p/>
 * The AWS S3 loader provides:
 * <ul>
 * <li>Integration with AWS SDK for S3 operations</li>
 * <li>Support for AWS credential chain authentication</li>
 * <li>Automatic content type detection</li>
 * <li>Metadata preservation from S3 object metadata</li>
 * <li>Error handling for network and authentication issues</li>
 * </ul>
 * <p/>
 * Authentication is handled through the standard AWS credential chain:
 * <ol>
 * <li>Environment variables (AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY)</li>
 * <li>Java system properties (aws.accessKeyId, aws.secretAccessKey)</li>
 * <li>AWS credentials file (~/.aws/credentials)</li>
 * <li>IAM instance profile credentials</li>
 * </ol>
 * <p/>
 * URI format for S3 documents:
 * <pre>{@code
 * s3://bucket-name/path/to/document.pdf
 * }</pre>
 * <p/>
 * Example usage:
 * <pre>{@code
 * DocumentLoader loader = new AwsS3DocumentLoader();
 * Collection<Document> documents = loader.load("s3://my-bucket/docs/manual.pdf");
 * }</pre>
 * <p/>
 * The loader integrates with CDI for dependency injection and can be
 * configured through application properties or environment variables.
 * It automatically handles AWS region detection and service endpoint
 * configuration.
 *
 * @author Aleks Seovic 2025.07.04
 * @since 25.09
 */
package com.oracle.coherence.rag.loader.aws.s3;
