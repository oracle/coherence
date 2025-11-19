/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.loader.azure.blobstorage;

import com.azure.core.http.HttpClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import com.oracle.coherence.rag.parser.ParserSupplier;
import com.tangosol.coherence.config.Config;

import dev.langchain4j.data.document.Document;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import org.eclipse.microprofile.config.spi.ConfigSource;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Integration tests for {@link AzureBlobStorageDocumentLoader}.
 * <p/>
 * This test class provides dual-mode integration testing:
 * <ul>
 * <li><strong>WireMock Mode (Default)</strong>: Uses WireMock stubs for offline testing</li>
 * <li><strong>Real Azure Mode</strong>: Tests against actual Azure Blob Storage service</li>
 * </ul>
 * <p/>
 * The test mode is determined by the {@code azure.storage.account.name} system property:
 * <ul>
 * <li>If {@code azure.storage.account.name} contains "test", WireMock mode is used</li>
 * <li>Otherwise, real Azure Blob Storage API mode is used</li>
 * </ul>
 * <p/>
 * <strong>Configuration for WireMock Mode:</strong>
 * <pre>
 * (no additional configuration needed - uses defaults)
 * </pre>
 * <p/>
 * <strong>Configuration for Real Azure Mode:</strong>
 * <pre>
 * -Dazure.storage.account.name=myaccount
 * -Dazure.storage.account.key=mykey
 * # or set AZURE_STORAGE_CONNECTION_STRING environment variable
 * </pre>
 *
 * @author Aleks Seovic  2025.01.07
 * @since 25.09
 */
public class AzureBlobStorageDocumentLoaderIT
    {
    // ---- test setup --------------------------------------------------

    @BeforeAll
    static void setUpClass()
        {
        // Get configuration
        String accountName = Config.getProperty("azure.storage.account.name", "testaccount");
        String accountKey = Config.getProperty("azure.storage.account.key", "testkey");
        String endpointUrl = null;

        // Determine test mode
        boolean useWireMock = accountName.contains("test");
        boolean useProxy    = Config.getBoolean("wiremock.proxy", false);

        if (useWireMock)
            {
            // WireMock mode: use embedded server
            // Start WireMock server for replay mode
            wireMockServer = new WireMockServer(wireMockConfig()
                .port(0)
                .usingFilesUnderClasspath("wiremock"));

            wireMockServer.start();
            WireMock.configureFor("localhost", wireMockServer.port());

            System.out.println("WireMock server started on port " + wireMockServer.port());

            endpointUrl = "http://localhost:" + wireMockServer.port();
            
            // Configure Azure client with custom endpoint
            StorageSharedKeyCredential credential = new StorageSharedKeyCredential(accountName, accountKey);
            
            m_client = new BlobServiceClientBuilder()
                    .endpoint(endpointUrl)
                    .credential(credential)
                    .httpClient(HttpClient.createDefault())
                    .buildClient();
            }
        else if (useProxy)
            {
            // Configure Azure client with proxy endpoint
            endpointUrl = "http://localhost:8089";

            String connectionString = String.format("DefaultEndpointsProtocol=https;AccountName=%s;AccountKey=%s;EndpointSuffix=core.windows.net",
                    accountName, accountKey);

            m_client = new BlobServiceClientBuilder()
                    .endpoint(endpointUrl)
                    .connectionString(connectionString)
                    .httpClient(HttpClient.createDefault())
                    .buildClient();
            }
        else
            {
            // Real Azure mode: use default configuration
            String connectionString = String.format("DefaultEndpointsProtocol=https;AccountName=%s;AccountKey=%s;EndpointSuffix=core.windows.net",
                    accountName, accountKey);
            
            m_client = new BlobServiceClientBuilder()
                    .connectionString(connectionString)
                    .buildClient();
            }

        System.out.println("=== Azure Blob Storage Integration Test Configuration ===");
        System.out.println("Mode: " + (useWireMock ? "WireMock (Offline)" : "Real Azure"));
        System.out.println("Account: " + accountName);
        System.out.println("Endpoint: " + (endpointUrl != null ? endpointUrl : "default"));
        System.out.println("==========================================================");
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
        // Create parser supplier
        ParserSupplier parserSupplier = new ParserSupplier(new TestConfig());
        
        // Create document loader with constructor injection
        m_loader = new AzureBlobStorageDocumentLoader(m_client, parserSupplier);
        }

    // ---- tests -------------------------------------------------------

    @Test
    void testDocumentLoading()
        {
        // Test data
        String objectKey = "test-documents/sample-document.pdf";
        URI documentUri = URI.create("azure.blob://coherence-rag-test/" + objectKey);
        
        // Load document
        Document document = m_loader.load(documentUri);
        
        // Verify document content
        assertThat("Document should not be null", document, notNullValue());
        assertThat("Document text should not be empty", document.text().isEmpty(), is(false));
        assertThat("Mock document should contain expected text", document.text(), containsString("sample document"));

        // Verify metadata
        assertThat("Bucket metadata should match", document.metadata().getString("bucket"), is("coherence-rag-test"));
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
        URI documentUri = URI.create("azure.blob://coherence-rag-demo/" + objectKey);
        
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
     * Test configuration that provides appropriate parser settings.
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

    // ---- data members ------------------------------------------------

    private static WireMockServer wireMockServer;

    /**
     * Azure Blob Storage client for testing.
     */
    private static BlobServiceClient m_client;

    /**
     * Document loader under test.
     */
    private AzureBlobStorageDocumentLoader m_loader;
    } 
