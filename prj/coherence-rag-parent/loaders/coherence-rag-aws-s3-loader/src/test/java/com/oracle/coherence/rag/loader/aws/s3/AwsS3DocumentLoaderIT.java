/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.loader.aws.s3;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import com.oracle.coherence.rag.parser.ParserSupplier;

import com.tangosol.coherence.config.Config;

import dev.langchain4j.data.document.Document;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;
import software.amazon.awssdk.services.s3.endpoints.S3EndpointParams;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Integration tests for {@link AwsS3DocumentLoader}.
 * <p/>
 * This test class provides dual-mode integration testing:
 * <ul>
 * <li><strong>WireMock Mode (Default)</strong>: Uses WireMock stubs for offline testing</li>
 * <li><strong>Real S3 Mode</strong>: Tests against actual AWS S3 service</li>
 * </ul>
 * <p/>
 * The test mode is determined by the {@code aws.access.key.id} system property:
 * <ul>
 * <li>If {@code aws.access.key.id} starts with "test", WireMock mode is used</li>
 * <li>Otherwise, real S3 API mode is used</li>
 * </ul>
 * <p/>
 * <strong>Configuration for WireMock Mode:</strong>
 * <pre>
 * -Daws.region=us-east-1
 * </pre>
 * <p/>
 * <strong>Configuration for Real S3 Mode:</strong>
 * <pre>
 * -Daws.region=us-east-1
 * # AWS credentials via standard credential chain (environment variables, profiles, etc.)
 * </pre>
 *
 * @author Aleks Seovic  2025.07.11
 * @since 25.09
 */
public class AwsS3DocumentLoaderIT
    {
    // ---- test setup --------------------------------------------------

    @BeforeAll
    static void setUpClass()
        {
        // Get configuration
        String region          = Config.getProperty("aws.region", "us-east-1");
        String accessKeyId     = Config.getProperty("aws.access.key.id", "test-key");
        String secretAccessKey = Config.getProperty("aws.secret.access.key", "test-secret");
        String endpointUrl     = null;

        // Determine test mode
        boolean useWireMock = accessKeyId.startsWith("test");
        boolean useProxy    = Config.getBoolean("wiremock.proxy", false);

        // Configure S3 client
        var builder = S3Client.builder().region(Region.of(region));

        if (useWireMock)
            {
            // WireMock mode: use static credentials and custom endpoint
            // Start WireMock server for replay mode
            wireMockServer = new WireMockServer(wireMockConfig()
                .port(0)
                .usingFilesUnderClasspath("wiremock"));

            wireMockServer.start();
            WireMock.configureFor("localhost", wireMockServer.port());

            System.out.println("WireMock server started on port " + wireMockServer.port());

            AwsCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKeyId, secretAccessKey));

            endpointUrl = "http://localhost:" + wireMockServer.port();
            builder.credentialsProvider(credentialsProvider)
                    .endpointProvider(new TestEndpointProvider())
                    .endpointOverride(URI.create(endpointUrl));
            }
        else if (useProxy)
            {
            // WireMock Proxy mode: use default credentials and custom endpoint
            endpointUrl = "http://localhost:8089";
            
            builder.credentialsProvider(DefaultCredentialsProvider.create())
                    .endpointProvider(new TestEndpointProvider())
                    .endpointOverride(URI.create(endpointUrl));
            }
        else
            {
            // Real S3 mode: use default credential chain
            builder.credentialsProvider(DefaultCredentialsProvider.create());
            }
        
        m_client = builder.build();

        System.out.println("=== AWS S3 Integration Test Configuration ===");
        System.out.println("Mode: " + (useWireMock
                                       ? "WireMock (Offline)" : "Real S3"));
        System.out.println("Region: " + region);
        System.out.println("Endpoint: " + (endpointUrl != null ? endpointUrl : "default"));
        System.out.println("===============================================");
        }

    @AfterAll
    static void tearDownClass()
        {
        if (wireMockServer != null && wireMockServer.isRunning())
            {
            wireMockServer.stop();
            System.out.println("WireMock server stopped");
            }
        }

    @BeforeEach
    void setUp()
        {
        // Create document loader
        ParserSupplier parserSupplier = new ParserSupplier(new TestConfig());
        m_loader = new AwsS3DocumentLoader(m_client, parserSupplier);
        }

    // ---- tests -------------------------------------------------------

    @Test
    void testDocumentLoading()
        {
        // Test data
        String objectKey = "test-documents/sample-document.pdf";
        URI documentUri = URI.create("s3://coherence-rag/" + objectKey);
        
        // Load document
        Document document = m_loader.load(documentUri);
        
        // Verify document content
        assertThat("Document should not be null", document, notNullValue());
        assertThat("Document text should not be empty", document.text().isEmpty(), is(false));
        assertThat("Mock document should contain expected text", document.text(), containsString("sample document"));

        // Verify metadata
        assertThat("Bucket metadata should match", document.metadata().getString("bucket"), is("coherence-rag"));
        assertThat("Object metadata should match", document.metadata().getString("object"), is(objectKey));
        assertThat("URL metadata should match", document.metadata().getString("url"), is(documentUri.toString()));
        
        // Content type should be set
        String contentType = document.metadata().getString("content_type");
        assertThat("Content type should be set", contentType, notNullValue());
        assertThat("Content type should be PDF", contentType, is("application/pdf"));

        // Content length should be positive
        Long contentLength = document.metadata().getLong("content_length");
        assertThat("Content length should be positive", contentLength > 0, is(true));
        
        // ETag should be present
        String etag = document.metadata().getString("etag");
        assertThat("ETag should be present", etag, notNullValue());

        // Print document info for debugging
        System.out.println("Document loaded successfully:");
        System.out.println("  Text length: " + document.text().length());
        System.out.println("  Content type: " + contentType);
        System.out.println("  Content length: " + contentLength);
        System.out.println("  ETag: " + etag);
        }

    @Test
    void testDocumentLoadingWithNonexistentKey()
        {
        String objectKey = "nonexistent/document.pdf";
        URI documentUri = URI.create("s3://coherence-rag/" + objectKey);
        
        try
            {
            m_loader.load(documentUri);
            fail("Should have thrown exception for nonexistent key");
            }
        catch (Exception e)
            {
            // Expected - document not found
            System.out.println("Expected exception for nonexistent key: " + e.getMessage());
            assertTrue(true, "Exception thrown as expected");
            }
        }

    // ---- inner classes ---------------------------------------------------

    /**
     * Test configuration that provides appropriate OpenAI settings
     * based on whether we're using WireMock or real API.
     */
    private static class TestConfig implements org.eclipse.microprofile.config.Config
        {
        @Override
        public <T> T getValue(String propertyName, Class<T> propertyType)
            {
            return getOptionalValue(propertyName, propertyType)
                .orElseThrow(() -> new IllegalArgumentException("Property not found: " + propertyName));
            }

        @Override
        @SuppressWarnings("unchecked")
        public <T> Optional<T> getOptionalValue(String propertyName, Class<T> propertyType)
            {
            if ("coherence.rag.default.parser".equals(propertyName))
                {
                return Optional.of((T) ParserSupplier.DEFAULT_PARSER);
                }

            return Optional.empty();
            }

        @Override
        public Iterable<String> getPropertyNames()
            {
            return List.of("coherence.rag.default.parser");
            }

        @Override
        public Iterable<ConfigSource> getConfigSources()
            {
            return java.util.Collections.emptyList();
            }

        @Override
        public <T> T unwrap(Class<T> type)
            {
            throw new UnsupportedOperationException("unwrap not supported in test config");
            }

        @Override
        public <T> java.util.Optional<org.eclipse.microprofile.config.spi.Converter<T>> getConverter(Class<T> forType)
            {
            return java.util.Optional.empty();
            }

        @Override
        public org.eclipse.microprofile.config.ConfigValue getConfigValue(String propertyName)
            {
            throw new UnsupportedOperationException("getConfigValue not supported in test config");
            }
        }

    private static class TestEndpointProvider
            implements software.amazon.awssdk.services.s3.endpoints.S3EndpointProvider
        {
        public CompletableFuture<Endpoint> resolveEndpoint(S3EndpointParams endpointParams)
            {
            return CompletableFuture.completedFuture(Endpoint.builder().url(URI.create(endpointParams.endpoint())).build());
            }
        }

    // ---- data members ------------------------------------------------

    private static WireMockServer wireMockServer;

    /**
     * S3 client for testing.
     */
    private static S3Client m_client;

    /**
     * Document loader under test.
     */
    private AwsS3DocumentLoader m_loader;
    }
