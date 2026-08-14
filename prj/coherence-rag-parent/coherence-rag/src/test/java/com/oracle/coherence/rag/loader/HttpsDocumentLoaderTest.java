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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;

import java.nio.charset.StandardCharsets;

import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;

import java.util.Base64;

import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.ExtendedSSLSession;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        RagSecurityTestSupport.setDirectConnectionPolicy(uri -> true);
        }

    @AfterEach
    void cleanup()
        {
        System.clearProperty(RagSecurity.PROP_IMPORT_ALLOWED_SCHEMES);
        System.clearProperty(RagSecurity.PROP_IMPORT_HTTP_ALLOWED_HOSTS);
        System.clearProperty(RagSecurity.PROP_IMPORT_HTTP_ALLOW_PRIVATE);
        RagSecurityTestSupport.setAddressResolver(null);
        RagSecurityTestSupport.setHttpConnectionFactory(null);
        RagSecurityTestSupport.setDirectConnectionPolicy(null);
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
    @DisplayName("should retain the logical HTTPS host on the pinned address")
    void shouldRetainLogicalHttpsHostOnPinnedAddress()
            throws Exception
        {
        AtomicReference<InetAddress> addressOpened = new AtomicReference<>();
        RagSecurityTestSupport.setHttpConnectionFactory((uri, address, connectTimeout, readTimeout) ->
            {
            addressOpened.set(address);
            return new TestHttpURLConnection(uri.toURL(), "Document content");
            });

        when(mockParserSupplier.get()).thenReturn(mockDocumentParser);
        when(mockDocumentParser.parse(any(InputStream.class))).thenReturn(mockDocument);

        loader.load(URI.create("https://secure.example.com/document.pdf"));

        assertThat(addressOpened.get().getHostAddress(), is("93.184.216.34"));
        assertThat(addressOpened.get().getHostName(), is("secure.example.com"));
        }

    @Test
    @DisplayName("should use the logical host for TLS SNI and endpoint identification")
    void shouldUseLogicalHostForTlsIdentity()
            throws Exception
        {
        InetAddress addressLoopback = InetAddress.getLoopbackAddress();
        AtomicReference<String> sniHost = new AtomicReference<>();
        AtomicReference<String> hostHeader = new AtomicReference<>();
        AtomicReference<Throwable> serverFailure = new AtomicReference<>();
        try (SSLServerSocket server = (SSLServerSocket) createServerSslContext()
                .getServerSocketFactory().createServerSocket(0, 1, addressLoopback))
            {
            enableHttp11(server);
            Thread threadServer = Thread.ofPlatform().daemon().start(() ->
                {
                try (SSLSocket socket = (SSLSocket) server.accept();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(
                             socket.getInputStream(), StandardCharsets.US_ASCII)))
                    {
                    socket.startHandshake();
                    ExtendedSSLSession session = (ExtendedSSLSession) socket.getSession();
                    session.getRequestedServerNames().stream()
                            .filter(SNIHostName.class::isInstance)
                            .map(SNIHostName.class::cast)
                            .map(SNIHostName::getAsciiName)
                            .findFirst()
                            .ifPresent(sniHost::set);

                    for (String sLine = reader.readLine(); sLine != null && !sLine.isEmpty(); sLine = reader.readLine())
                        {
                        if (sLine.regionMatches(true, 0, "Host:", 0, 5))
                            {
                            hostHeader.set(sLine.substring(5).trim());
                            }
                        }
                    socket.getOutputStream().write(("HTTP/1.1 200 OK\r\n"
                            + "Content-Length: 16\r\n"
                            + "Connection: close\r\n\r\n"
                            + "Document content").getBytes(StandardCharsets.US_ASCII));
                    socket.getOutputStream().flush();
                    }
                catch (Throwable t)
                    {
                    serverFailure.set(t);
                    try
                        {
                        server.close();
                        }
                    catch (IOException ignored)
                        {
                        }
                    }
                });

            String sHost = "secure.example.com";
            URI uri = URI.create("https://" + sHost + ":" + server.getLocalPort() + "/document.txt");
            InetAddress addressPinned = InetAddress.getByAddress(sHost, addressLoopback.getAddress());
            HttpURLConnection connection = RagSecurityTestSupport.openPinnedHttpsConnection(
                    uri, addressPinned, 5_000, 5_000, createClientSslContext());
            try
                {
                assertThat(new String(connection.getInputStream().readAllBytes(), StandardCharsets.US_ASCII),
                        is("Document content"));
                }
            finally
                {
                connection.disconnect();
                }

            threadServer.join(5_000);
            assertThat(threadServer.isAlive(), is(false));
            assertThat(serverFailure.get(), is(nullValue()));
            assertThat(sniHost.get(), is(sHost));
            assertThat(hostHeader.get(), is(sHost + ":" + server.getLocalPort()));
            }
        }

    @Test
    @DisplayName("should reject a certificate for a different logical host")
    void shouldRejectCertificateForDifferentLogicalHost()
            throws Exception
        {
        InetAddress addressLoopback = InetAddress.getLoopbackAddress();
        try (SSLServerSocket server = (SSLServerSocket) createServerSslContext()
                .getServerSocketFactory().createServerSocket(0, 1, addressLoopback))
            {
            enableHttp11(server);
            Thread threadServer = Thread.ofPlatform().daemon().start(() ->
                {
                try (SSLSocket socket = (SSLSocket) server.accept())
                    {
                    socket.startHandshake();
                    socket.getInputStream().read();
                    }
                catch (IOException ignored)
                    {
                    try
                        {
                        server.close();
                        }
                    catch (IOException ignoredClose)
                        {
                        }
                    }
                });

            String sHost = "other.example.com";
            URI uri = URI.create("https://" + sHost + ":" + server.getLocalPort() + "/document.txt");
            InetAddress addressPinned = InetAddress.getByAddress(sHost, addressLoopback.getAddress());

            assertThrows(IOException.class, () -> RagSecurityTestSupport.openPinnedHttpsConnection(
                    uri, addressPinned, 5_000, 5_000, createClientSslContext()));

            threadServer.join(5_000);
            assertThat(threadServer.isAlive(), is(false));
            }
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
        RagSecurityTestSupport.setHttpConnectionFactory((uri, address, connectTimeout, readTimeout) ->
            {
            uriOpened.set(uri);
            return new TestHttpURLConnection(uri.toURL(), sContent);
            });
        return uriOpened;
        }

    private void enableHttp11(SSLServerSocket server)
        {
        SSLParameters parameters = server.getSSLParameters();
        parameters.setApplicationProtocols(new String[] {"http/1.1"});
        server.setSSLParameters(parameters);
        }

    private SSLContext createServerSslContext()
            throws Exception
        {
        X509Certificate certificate = readCertificate();
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        keyStore.setKeyEntry("server", readPrivateKey(), KEY_PASSWORD,
                new java.security.cert.Certificate[] {certificate});

        KeyManagerFactory factory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        factory.init(keyStore, KEY_PASSWORD);
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(factory.getKeyManagers(), null, null);
        return context;
        }

    private SSLContext createClientSslContext()
            throws Exception
        {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        keyStore.setCertificateEntry("server", readCertificate());

        TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        factory.init(keyStore);
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, factory.getTrustManagers(), null);
        return context;
        }

    private X509Certificate readCertificate()
            throws Exception
        {
        try (InputStream in = getClass().getResourceAsStream("/secure-example-cert.pem"))
            {
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(in);
            }
        }

    private PrivateKey readPrivateKey()
            throws Exception
        {
        try (InputStream in = getClass().getResourceAsStream("/secure-example-key.pem"))
            {
            String sPem = new String(in.readAllBytes(), StandardCharsets.US_ASCII)
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] abKey = Base64.getDecoder().decode(sPem);
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(abKey));
            }
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

    private static final char[] KEY_PASSWORD = "changeit".toCharArray();
    }
