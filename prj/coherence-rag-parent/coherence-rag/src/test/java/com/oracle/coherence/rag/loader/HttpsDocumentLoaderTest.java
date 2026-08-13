/*
 * Copyright (c) 2025, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.loader;

import com.oracle.coherence.rag.api.RagSecurity;
import com.oracle.coherence.rag.api.RagSecurityTestSupport;
import com.oracle.coherence.rag.parser.ParserSupplier;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.Metadata;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;

import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link HttpsDocumentLoader} class.
 * <p/>
 * This test class validates the HTTPS document loader functionality including
 * secure URL handling, SSL/TLS capabilities, and inheritance from HttpDocumentLoader.
 *
 * @author Aleks Seovic  2025.07.04
 * @since 25.09
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HttpsDocumentLoader")
class HttpsDocumentLoaderTest
    {
    @Mock
    private DocumentParser mockDocumentParser;

    @Mock
    private ParserSupplier mockParserSupplier;

    @Mock
    private Document mockDocument;

    private HttpsDocumentLoader loader;

    @BeforeEach
    void setUp()
        {
        loader = new HttpsDocumentLoader(mockParserSupplier);
        System.setProperty(RagSecurity.PROP_IMPORT_ALLOWED_SCHEMES, "https");
        System.setProperty(RagSecurity.PROP_IMPORT_HTTP_ALLOWED_HOSTS, "secure.example.com,api.example.com");
        RagSecurityTestSupport.setAddressResolver(host -> new InetAddress[] {InetAddress.getByName("93.184.216.34")});
        }

    @AfterEach
    void cleanup()
        {
        System.clearProperty(RagSecurity.PROP_IMPORT_ALLOWED_SCHEMES);
        System.clearProperty(RagSecurity.PROP_IMPORT_HTTP_ALLOWED_HOSTS);
        System.clearProperty(RagSecurity.PROP_IMPORT_HTTP_ALLOW_PRIVATE);
        RagSecurityTestSupport.setAddressResolver(null);
        RagSecurityTestSupport.setHttpConnectionFactory(null);
        }

    @Test
    @DisplayName("should load document from HTTPS URL")
    void shouldLoadDocumentFromHttpsUrl()
            throws Exception
        {
        AtomicReference<URI> uriOpened = mockHttpsConnection("Document content");
        URI httpsUri = URI.create("https://secure.example.com/document.pdf");

        when(mockParserSupplier.get()).thenReturn(mockDocumentParser);
        when(mockDocumentParser.parse(any(InputStream.class))).thenReturn(mockDocument);

        Document result = loader.load(httpsUri);

        assertThat(result, is(sameInstance(mockDocument)));
        assertThat(uriOpened.get(), is(httpsUri));
        }

    @Test
    @DisplayName("should handle HTTPS URLs with different ports")
    void shouldHandleHttpsUrlsWithDifferentPorts()
            throws Exception
        {
        AtomicReference<URI> uriOpened = mockHttpsConnection("Document content");
        URI httpsUri = URI.create("https://secure.example.com:8443/documents/report.txt");

        when(mockParserSupplier.get()).thenReturn(mockDocumentParser);
        when(mockDocumentParser.parse(any(InputStream.class))).thenReturn(mockDocument);

        Document result = loader.load(httpsUri);

        assertThat(result, is(sameInstance(mockDocument)));
        assertThat(uriOpened.get(), is(httpsUri));
        }

    @Test
    @DisplayName("should handle HTTPS URLs with query parameters")
    void shouldHandleHttpsUrlsWithQueryParameters()
            throws Exception
        {
        AtomicReference<URI> uriOpened = mockHttpsConnection("Document content");
        URI httpsUri = URI.create("https://api.example.com/documents?token=abc123&format=pdf");

        when(mockParserSupplier.get()).thenReturn(mockDocumentParser);
        when(mockDocumentParser.parse(any(InputStream.class))).thenReturn(mockDocument);

        Document result = loader.load(httpsUri);

        assertThat(result, is(sameInstance(mockDocument)));
        assertThat(uriOpened.get(), is(httpsUri));
        }

    @Test
    @DisplayName("should handle document with secure response metadata")
    void shouldHandleDocumentWithSecureResponseMetadata()
            throws Exception
        {
        mockHttpsConnection("Secure document content");
        URI httpsUri = URI.create("https://secure.example.com/document.pdf");
        Metadata metadata = Metadata.from("content-type", "application/pdf");
        Document documentWithMetadata = Document.from("Secure document content", metadata);

        when(mockParserSupplier.get()).thenReturn(mockDocumentParser);
        when(mockDocumentParser.parse(any(InputStream.class))).thenReturn(documentWithMetadata);

        Document result = loader.load(httpsUri);

        assertThat(result, is(sameInstance(documentWithMetadata)));
        assertThat(result.metadata().getString("content-type"), is("application/pdf"));
        }

    private AtomicReference<URI> mockHttpsConnection(String sContent)
            throws IOException
        {
        AtomicReference<URI> uriOpened = new AtomicReference<>();
        RagSecurityTestSupport.setHttpConnectionFactory(uri ->
            {
            uriOpened.set(uri);
            return new TestHttpURLConnection(uri.toURL(), sContent);
            });
        return uriOpened;
        }

    // ---- inner class: TestHttpURLConnection -----------------------------

    /**
     * Minimal HTTP connection used to keep loader tests hermetic.
     */
    private static class TestHttpURLConnection
            extends HttpURLConnection
        {
        TestHttpURLConnection(URL url, String sContent)
            {
            super(url);
            this.sContent = sContent;
            }

        @Override
        public int getResponseCode()
            {
            return HTTP_OK;
            }

        @Override
        public InputStream getInputStream()
            {
            return new ByteArrayInputStream(sContent.getBytes());
            }

        @Override
        public void disconnect()
            {
            disconnected = true;
            }

        @Override
        public boolean usingProxy()
            {
            return false;
            }

        @Override
        public void connect()
            {
            }

        @SuppressWarnings("unused")
        private boolean disconnected;

        private final String sContent;
        }
    }
