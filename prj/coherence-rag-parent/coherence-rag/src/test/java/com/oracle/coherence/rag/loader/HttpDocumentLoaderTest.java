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
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link HttpDocumentLoader} class.
 * <p/>
 * This test class validates the HTTP document loader functionality including
 * URL handling, document parser integration, and error scenarios.
 *
 * @author Aleks Seovic  2025.07.04
 * @since 25.09
 */
@SuppressWarnings("HttpUrlsUsage")
@ExtendWith(MockitoExtension.class)
@DisplayName("HttpDocumentLoader")
class HttpDocumentLoaderTest
    {
    @Mock
    private DocumentParser mockDocumentParser;

    @Mock
    private ParserSupplier mockParserSupplier;

    @Mock
    private Document mockDocument;

    private HttpDocumentLoader loader;

    @BeforeEach
    void setUp()
        {
        loader = new HttpDocumentLoader(mockParserSupplier);
        System.setProperty(RagSecurity.PROP_IMPORT_ALLOWED_SCHEMES, "http");
        System.setProperty(RagSecurity.PROP_IMPORT_HTTP_ALLOWED_HOSTS, "example.com,localhost");
        RagSecurityTestSupport.setAddressResolver(host -> new InetAddress[] {InetAddress.getByName("93.184.216.34")});
        }

    @AfterEach
    void cleanup()
        {
        System.clearProperty(RagSecurity.PROP_IMPORT_ALLOWED_SCHEMES);
        System.clearProperty(RagSecurity.PROP_IMPORT_HTTP_ALLOWED_HOSTS);
        System.clearProperty(RagSecurity.PROP_IMPORT_HTTP_ALLOW_PRIVATE);
        System.clearProperty(RagSecurity.PROP_IMPORT_CONNECT_TIMEOUT);
        System.clearProperty(RagSecurity.PROP_IMPORT_READ_TIMEOUT);
        System.clearProperty(RagSecurity.PROP_IMPORT_MAX_BYTES);
        RagSecurityTestSupport.setAddressResolver(null);
        RagSecurityTestSupport.setHttpConnectionFactory(null);
        }

    @Test
    @DisplayName("should load document from HTTP URL")
    void shouldLoadDocumentFromHttpUrl()
            throws Exception
        {
        AtomicReference<URI> uriOpened = mockHttpConnection("Document content");
        URI httpUri = URI.create("http://example.com/document.pdf");

        when(mockParserSupplier.get()).thenReturn(mockDocumentParser);
        when(mockDocumentParser.parse(any(InputStream.class))).thenReturn(mockDocument);

        Document result = loader.load(httpUri);

        assertThat(result, is(sameInstance(mockDocument)));
        assertThat(uriOpened.get(), is(httpUri));
        }

    @Test
    @DisplayName("should handle HTTP URLs with query parameters")
    void shouldHandleHttpUrlsWithQueryParameters()
            throws Exception
        {
        AtomicReference<URI> uriOpened = mockHttpConnection("Document content");
        URI httpUri = URI.create("http://example.com/api/documents?id=123&format=pdf");

        when(mockParserSupplier.get()).thenReturn(mockDocumentParser);
        when(mockDocumentParser.parse(any(InputStream.class))).thenReturn(mockDocument);

        Document result = loader.load(httpUri);

        assertThat(result, is(sameInstance(mockDocument)));
        assertThat(uriOpened.get(), is(httpUri));
        }

    @Test
    @DisplayName("should handle HTTP URLs with different ports")
    void shouldHandleHttpUrlsWithDifferentPorts()
            throws Exception
        {
        AtomicReference<URI> uriOpened = mockHttpConnection("Document content");
        URI httpUri = URI.create("http://localhost:8080/documents/report.txt");

        when(mockParserSupplier.get()).thenReturn(mockDocumentParser);
        when(mockDocumentParser.parse(any(InputStream.class))).thenReturn(mockDocument);

        Document result = loader.load(httpUri);

        assertThat(result, is(sameInstance(mockDocument)));
        assertThat(uriOpened.get(), is(httpUri));
        }

    @Test
    @DisplayName("should handle HTTP URLs with special characters")
    void shouldHandleHttpUrlsWithSpecialCharacters()
            throws Exception
        {
        AtomicReference<URI> uriOpened = mockHttpConnection("Document content");
        URI httpUri = URI.create("http://example.com/documents/file%20with%20spaces.pdf");

        when(mockParserSupplier.get()).thenReturn(mockDocumentParser);
        when(mockDocumentParser.parse(any(InputStream.class))).thenReturn(mockDocument);

        Document result = loader.load(httpUri);

        assertThat(result, is(sameInstance(mockDocument)));
        assertThat(uriOpened.get(), is(httpUri));
        }

    @Test
    @DisplayName("should handle null document response")
    void shouldHandleNullDocumentResponse()
            throws Exception
        {
        mockHttpConnection("");
        URI httpUri = URI.create("http://example.com/empty-document.txt");

        when(mockParserSupplier.get()).thenReturn(mockDocumentParser);
        when(mockDocumentParser.parse(any(InputStream.class))).thenReturn(null);

        Document result = loader.load(httpUri);

        assertThat(result, is(nullValue()));
        }

    @Test
    @DisplayName("should handle document with HTTP response metadata")
    void shouldHandleDocumentWithHttpResponseMetadata()
            throws Exception
        {
        mockHttpConnection("Document content");
        URI httpUri = URI.create("http://example.com/document.pdf");
        Metadata metadata = Metadata.from("content-type", "application/pdf");
        Document documentWithMetadata = Document.from("Document content", metadata);

        when(mockParserSupplier.get()).thenReturn(mockDocumentParser);
        when(mockDocumentParser.parse(any(InputStream.class))).thenReturn(documentWithMetadata);

        Document result = loader.load(httpUri);

        assertThat(result, is(sameInstance(documentWithMetadata)));
        assertThat(result.metadata().getString("content-type"), is("application/pdf"));
        }

    @Test
    @DisplayName("should handle HTTP URLs with fragment identifiers")
    void shouldHandleHttpUrlsWithFragmentIdentifiers()
            throws Exception
        {
        AtomicReference<URI> uriOpened = mockHttpConnection("Document content");
        URI httpUri = URI.create("http://example.com/document.html#section1");

        when(mockParserSupplier.get()).thenReturn(mockDocumentParser);
        when(mockDocumentParser.parse(any(InputStream.class))).thenReturn(mockDocument);

        Document result = loader.load(httpUri);

        assertThat(result, is(sameInstance(mockDocument)));
        assertThat(uriOpened.get(), is(httpUri));
        }

    @Test
    @DisplayName("should reject redirect target resolving to private address")
    void shouldRejectRedirectTargetResolvingToPrivateAddress()
            throws Exception
        {
        System.setProperty(RagSecurity.PROP_IMPORT_HTTP_ALLOWED_HOSTS, "example.com,docs.example.com");
        RagSecurityTestSupport.setAddressResolver(host -> new InetAddress[] {
                InetAddress.getByName("docs.example.com".equals(host) ? "10.0.0.5" : "93.184.216.34")
        });
        RagSecurityTestSupport.setHttpConnectionFactory(uri ->
            {
            if ("example.com".equals(uri.getHost()))
                {
                return new TestRedirectHttpURLConnection(uri.toURL(), "http://docs.example.com/secret");
                }
            return new TestHttpURLConnection(uri.toURL(), "secret");
            });

        RagSecurity.PolicyViolation e = assertThrows(RagSecurity.PolicyViolation.class,
                () -> loader.load(URI.create("http://example.com/start")));

        assertThat(e.reason(), is(RagSecurity.REASON_PRIVATE_ADDRESS_NOT_ALLOWED));
        }

    @Test
    @DisplayName("should apply configured timeouts")
    void shouldApplyConfiguredTimeouts()
            throws Exception
        {
        System.setProperty(RagSecurity.PROP_IMPORT_CONNECT_TIMEOUT, "1234");
        System.setProperty(RagSecurity.PROP_IMPORT_READ_TIMEOUT, "5678");
        AtomicReference<TestHttpURLConnection> connectionRef = new AtomicReference<>();
        RagSecurityTestSupport.setHttpConnectionFactory(uri ->
            {
            TestHttpURLConnection connection = new TestHttpURLConnection(uri.toURL(), "Document content");
            connectionRef.set(connection);
            return connection;
            });

        when(mockParserSupplier.get()).thenReturn(mockDocumentParser);
        when(mockDocumentParser.parse(any(InputStream.class))).thenReturn(mockDocument);

        loader.load(URI.create("http://example.com/document.pdf"));

        assertThat(connectionRef.get().getConnectTimeout(), is(1234));
        assertThat(connectionRef.get().getReadTimeout(), is(5678));
        }

    @Test
    @DisplayName("should reject response body over byte cap")
    void shouldRejectResponseBodyOverByteCap()
            throws Exception
        {
        System.setProperty(RagSecurity.PROP_IMPORT_MAX_BYTES, "3");
        mockHttpConnection("Document content");
        when(mockParserSupplier.get()).thenReturn(mockDocumentParser);
        when(mockDocumentParser.parse(any(InputStream.class))).then(invocation ->
            {
            InputStream in = invocation.getArgument(0);
            while (in.read() != -1)
                {
                }
            return mockDocument;
            });

        RagSecurity.PolicyViolation e = assertThrows(RagSecurity.PolicyViolation.class,
                () -> loader.load(URI.create("http://example.com/document.pdf")));

        assertThat(e.reason(), is(RagSecurity.REASON_IMPORT_BODY_EXCEEDS_CAP));
        }

    private AtomicReference<URI> mockHttpConnection(String sContent)
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

    /**
     * Minimal redirect connection used to keep redirect tests hermetic.
     */
    private static class TestRedirectHttpURLConnection
            extends TestHttpURLConnection
        {
        TestRedirectHttpURLConnection(URL url, String sLocation)
            {
            super(url, "");
            this.sLocation = sLocation;
            }

        @Override
        public int getResponseCode()
            {
            return HTTP_MOVED_TEMP;
            }

        @Override
        public String getHeaderField(String name)
            {
            return "Location".equals(name) ? sLocation : null;
            }

        private final String sLocation;
        }
    }
