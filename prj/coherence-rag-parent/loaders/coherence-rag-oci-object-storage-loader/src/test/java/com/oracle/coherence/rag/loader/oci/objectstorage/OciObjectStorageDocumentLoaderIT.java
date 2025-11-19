/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.loader.oci.objectstorage;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import com.oracle.bmc.Region;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.SimpleAuthenticationDetailsProvider;
import com.oracle.bmc.auth.SimplePrivateKeySupplier;
import com.oracle.bmc.objectstorage.ObjectStorageClient;

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
 * Integration tests for {@link OciObjectStorageDocumentLoader}.
 * <p/>
 * This test class provides dual-mode integration testing:
 * <ul>
 * <li><strong>WireMock Mode (Default)</strong>: Uses WireMock stubs for offline testing</li>
 * <li><strong>Real OCI Mode</strong>: Tests against actual OCI Object Storage service</li>
 * </ul>
 * <p/>
 * The test mode is determined by the {@code oci.tenant.id} system property:
 * <ul>
 * <li>If {@code oci.tenant.id} contains "test", WireMock mode is used</li>
 * <li>Otherwise, real OCI Object Storage API mode is used</li>
 * </ul>
 * <p/>
 * <strong>Configuration for WireMock Mode:</strong>
 * <pre>
 * (no additional configuration needed - uses defaults)
 * </pre>
 * <p/>
 * <strong>Configuration for Real OCI Mode:</strong>
 * <pre>
 * -Doci.tenant.id=ocid1.tenancy.oc1..aaaaaaaa...
 * -Doci.user.id=ocid1.user.oc1..aaaaaaaa...
 * -Doci.auth.fingerprint=11:22:33:44:55:66:77:88:99:aa:bb:cc:dd:ee:ff:00
 * -Doci.auth.key=/path/to/oci_api_key.pem
 * -Doci.region=us-ashburn-1
 * # or configure via OCI config file
 * </pre>
 *
 * @author Aleks Seovic  2025.01.07
 * @since 25.09
 */
public class OciObjectStorageDocumentLoaderIT
    {
    // ---- test setup --------------------------------------------------

    @BeforeAll
    static void setUpClass()
        {
        // Get configuration
        String tenancyId = Config.getProperty("oci.tenant.id", "test-tenancy");
        String userId = Config.getProperty("oci.user.id", "test-user");
        String fingerprint = Config.getProperty("oci.auth.fingerprint", "11:22:33:44:55:66:77:88:99:aa:bb:cc:dd:ee:ff:00");
        String privateKeyPath = Config.getProperty("oci.auth.key", "test-key.pem");
        String region = Config.getProperty("oci.region", "us-chicago-1");
        String endpointUrl = null;

        // Determine test mode
        boolean useWireMock = tenancyId.contains("test");
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
            
            // Configure OCI client with mock credentials and custom endpoint
            AuthenticationDetailsProvider provider = SimpleAuthenticationDetailsProvider.builder()
                    .tenantId(tenancyId)
                    .userId(userId)
                    .fingerprint(fingerprint)
                    .privateKeySupplier(new TestPrivateKeySupplier())
                    .region(Region.fromRegionId(region))
                    .build();
            
            m_client = ObjectStorageClient.builder()
                    .endpoint(endpointUrl)
                    .build(provider);
            }
        else if (useProxy)
            {
            // Configure OCI client with proxy endpoint
            AuthenticationDetailsProvider provider = SimpleAuthenticationDetailsProvider.builder()
                    .tenantId(tenancyId)
                    .userId(userId)
                    .fingerprint(fingerprint)
                    .privateKeySupplier(new TestPrivateKeySupplier())
                    .region(Region.fromRegionId(region))
                    .build();

            m_client = ObjectStorageClient.builder()
                    .endpoint("http://localhost:8089")
                    .build(provider);
            }
        else
            {
            // Real OCI mode: use default configuration
            AuthenticationDetailsProvider provider = SimpleAuthenticationDetailsProvider.builder()
                    .tenantId(tenancyId)
                    .userId(userId)
                    .fingerprint(fingerprint)
                    .privateKeySupplier(new SimplePrivateKeySupplier(privateKeyPath))
                    .region(Region.fromRegionId(region))
                    .build();
                    
            m_client = ObjectStorageClient.builder().build(provider);
            }

        System.out.println("=== OCI Object Storage Integration Test Configuration ===");
        System.out.println("Mode: " + (useWireMock ? "WireMock (Offline)" : "Real OCI"));
        System.out.println("Tenancy: " + tenancyId);
        System.out.println("Region: " + region);
        System.out.println("Endpoint: " + (endpointUrl != null ? endpointUrl : "default"));
        System.out.println("==========================================================");
        }

    @AfterAll
    static void tearDownClass()
        {
        if (m_client != null)
            {
            m_client.close();
            }
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
        m_loader = new OciObjectStorageDocumentLoader(m_client, parserSupplier);
        }

    // ---- tests -------------------------------------------------------

    @Test
    void testDocumentLoading()
        {
        // Test data
        String objectKey = "test-documents/sample-document.pdf";
        URI documentUri = URI.create("oci.os://odx-stateservice/coherence-rag-test/" + objectKey);
        
        // Load document
        Document document = m_loader.load(documentUri);
        
        // Verify document content
        assertThat("Document should not be null", document, notNullValue());
        assertThat("Document text should not be empty", document.text().isEmpty(), is(false));
        assertThat("Mock document should contain expected text", document.text(), containsString("sample document"));

        // Verify metadata
        assertThat("Namespace metadata should match", document.metadata().getString("ns"), is("odx-stateservice"));
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

        // Print document info for debugging
        System.out.println("Document loaded successfully:");
        System.out.println("  Text length: " + document.text().length());
        System.out.println("  Content type: " + contentType);
        System.out.println("  Content length: " + contentLength);
        System.out.println("  Content MD5: " + document.metadata().getString("content_md5"));
        }

    @Test
    void testDocumentLoadingWithNonexistentKey()
        {
        String objectKey = "nonexistent/document.pdf";
        URI documentUri = URI.create("oci.os://odx-stateservice/coherence-rag-test/" + objectKey);
        
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

    /**
     * Test private key supplier that provides the test private key directly.
     */
    private static class TestPrivateKeySupplier implements java.util.function.Supplier<java.io.InputStream>
        {
        @Override
        public java.io.InputStream get()
            {
            return new java.io.ByteArrayInputStream(TEST_PRIVATE_KEY.getBytes());
            }
        }

    // ---- constants ---------------------------------------------------

    /**
     * Test private key for OCI authentication in WireMock mode.
     * This is a valid RSA private key for testing purposes only.
     */
    private static final String TEST_PRIVATE_KEY =
            """
                    -----BEGIN PRIVATE KEY-----
                    MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC2cHKpiHZx7RX/
                    fsHtIyZixlHYpBpDEh8+fCNP7OjTtZYprB5jGYm6r+raziFf3ofuvWOX/miw8geQ
                    Jnrsvmaq3jdtMUfqLCPGPSw8q4mKnhq90WOYgIa0HN5t8FwA1D9iankF/Fj/msip
                    IhMUA03JJZ4MMEF92LhkxiEKt84amJ3U8q+Qxeun3uYK3q5X4qp9j2ZL1eB80cdN
                    KJR/g89VmN77DuMb2KVMa5WioutEHC077AK1JZg7RS0m4bN/YZi7AtOTiTjcvItI
                    /drCiAFDqc5iypl6wELYxVp2K9SHIfLuhQiBXgXKM5G4Vqs+PmHip/aeOIHCYLNH
                    GK62WdZ/AgMBAAECggEAFJqBGIxaHrumbwL4ZmOV2N801xaRW18XOuNMyaCjxWUN
                    3wmaf5lJaKqoB1Xy3EoxZ9DpB9KqLXGSpmucw6fXyGxC2N4RP8JZt/BTUqFnbcmK
                    k5gA0MmTF5GQZ9zBRzFb3ILRxXnzmHT5PGtQuZiLQbAx4oaAnvR0E/gcOy2kOnSD
                    M1qObMDvV2W+ERYW0FiHB9vTGsOVml3ol0mjpZHBpTMldboXEEGAeMDOCNkaYTg/
                    HfS6tiNK7rxFO612hPGnJSzECRWpwo39Tvn7Cv9OuV8LUomdam6lgJtQuw20tuNP
                    qbreVrFWjwJQUAJJA2kQ2U1+oXpj3HCr5WXom6dB4QKBgQD7jkt3yEYdixZffxAy
                    61lKU0ebPsOuOpqZR2xn7jL5Pbv7z+FYZHF6IvOhiZDR/kOBrXhQrQqj1xfkZ3XR
                    FpgVzZw7vLwhKL4CPwRMtHnd7Y7fY6n1ORdUFF3KsAeLclIgvr2vpNutGUOKqRhH
                    DuB05PDEqNOCU6ysqONXTmTwywKBgQC5qY+usuRighEQeplnarb2LE2YAFd/q0nw
                    jVYm9/i7eSTgpem4qS3dd7uDDVcp7upp8mUBwy6cJNatDkgoy0nyS9pmMssmRNAZ
                    clFit2kpQxbxhkQzOFeJeOGipX+Ph14ZN9qfOrFjs4lBIj8j1pB2P32kdWHcj8wi
                    Q2LWIUM+nQKBgGh/9zfeWcpCElw/c6JDhIdMy01hqHaDX9/W/OC74i8KB0KXW/yV
                    VVKwnFb5x9CEeNSxFG8nQ2lGnGVE2XuvkCRWktV6FQkNXMmgFhArVQjte9GvHFaD
                    jf5eq4vcznWOWzHBKxmBOcTR3u4GStCSDIpi5OY9YAge7HeZfT+ykFo7AoGAVXPp
                    oerMm1pqKD+FY8gGNf/mJtPhce2QtpsW0BDJ9t+nTY6PqGKVrZ1yPLtjJvXEBsd+
                    HfWL+moqNLSiGcSXYGHxP6CZSB/b+BpZwynPySIL6VZ3BWwlPizZDVdHAvS3JrxC
                    b49AHjeAMO5mSUR0cTh/x7YPOMkml46UayIcJTECgYEA4kFYecsYDK1Z/MwGKeSq
                    hYFTyOcLXzxbmBU9NpB2k4Hllvpwvcu/aAbgtw4SUchf6rlgJlUuyD5qFTzoKPXB
                    6ZVzKJiNzg8vZmXAaYVLNGg7x2isnjaLengTzdvPiSypYkgQ353OnweN9IbsAQ1c
                    doGRZ9lMWVejw6FhitWP4R8=
                    -----END PRIVATE KEY-----""";

    // ---- data members ------------------------------------------------

    private static WireMockServer wireMockServer;

    /**
     * OCI Object Storage client for testing.
     */
    private static ObjectStorageClient m_client;

    /**
     * Document loader under test.
     */
    private OciObjectStorageDocumentLoader m_loader;
    } 
