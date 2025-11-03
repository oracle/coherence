/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.loader.aws.s3;

import com.tangosol.coherence.config.Config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * CDI configuration support for AWS S3 document loader integration.
 * <p/>
 * This class provides CDI producers for AWS S3 client configuration,
 * enabling dependency injection of properly configured S3 clients
 * throughout the application.
 * <p/>
 * The configuration relies on standard AWS credential resolution
 * through the {@link DefaultCredentialsProvider} and requires the
 * AWS region to be specified via the {@code aws.region} system property.
 * <p/>
 * Required configuration:
 * <ul>
 * <li>{@code aws.region} - AWS region for S3 operations</li>
 * </ul>
 * <p/>
 * AWS credentials are resolved using the standard AWS credential chain:
 * <ol>
 * <li>Environment variables (AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY)</li>
 * <li>System properties (aws.accessKeyId, aws.secretKey)</li>
 * <li>Web Identity Token credentials from environment or container</li>
 * <li>Credential profiles file (~/.aws/credentials)</li>
 * <li>EC2 container credentials (IAM roles for tasks)</li>
 * <li>EC2 instance profile credentials (IAM roles for EC2)</li>
 * </ol>
 *
 * @author Aleks Seovic  2025.07.04
 * @since 25.09
 */
@ApplicationScoped
class CdiSupport
    {
    /**
     * Produces a configured S3 client for dependency injection.
     * <p/>
     * The client is configured with the region specified by the {@code aws.region}
     * system property and uses the default AWS credential provider chain for
     * authentication.
     * 
     * @return a configured S3Client instance
     */
    @Produces
    static S3Client s3Client()
        {
        return S3Client.builder()
                .region(Region.of(Config.getProperty("aws.region")))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
        }
    }
