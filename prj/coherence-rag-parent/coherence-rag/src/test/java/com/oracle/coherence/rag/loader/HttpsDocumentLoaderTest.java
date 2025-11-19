/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.loader;

import com.oracle.coherence.rag.parser.ParserSupplier;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.Metadata;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import org.junit.jupiter.api.extension.ExtendWith;

import java.net.URI;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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
        }

    @Test
    @DisplayName("should load document from HTTPS URL")
    void shouldLoadDocumentFromHttpsUrl()
        {
        try (MockedStatic<dev.langchain4j.data.document.loader.UrlDocumentLoader> mockedUrlLoader = 
                mockStatic(dev.langchain4j.data.document.loader.UrlDocumentLoader.class))
            {
            URI httpsUri = URI.create("https://secure.example.com/document.pdf");
            
            when(mockParserSupplier.get()).thenReturn(mockDocumentParser);
            mockedUrlLoader.when(() ->
                dev.langchain4j.data.document.loader.UrlDocumentLoader.load(
                    eq("https://secure.example.com/document.pdf"), eq(mockDocumentParser)))
                .thenReturn(mockDocument);

            Document result = loader.load(httpsUri);

            assertThat(result, is(sameInstance(mockDocument)));
            mockedUrlLoader.verify(() -> 
                dev.langchain4j.data.document.loader.UrlDocumentLoader.load(
                    "https://secure.example.com/document.pdf", mockDocumentParser));
            }
        }

    @Test
    @DisplayName("should handle HTTPS URLs with different ports")
    void shouldHandleHttpsUrlsWithDifferentPorts()
        {
        try (MockedStatic<dev.langchain4j.data.document.loader.UrlDocumentLoader> mockedUrlLoader = 
                mockStatic(dev.langchain4j.data.document.loader.UrlDocumentLoader.class))
            {
            URI httpsUri = URI.create("https://secure.example.com:8443/documents/report.txt");
            
            when(mockParserSupplier.get()).thenReturn(mockDocumentParser);
            mockedUrlLoader.when(() ->
                dev.langchain4j.data.document.loader.UrlDocumentLoader.load(
                    eq("https://secure.example.com:8443/documents/report.txt"), eq(mockDocumentParser)))
                .thenReturn(mockDocument);

            Document result = loader.load(httpsUri);

            assertThat(result, is(sameInstance(mockDocument)));
            }
        }

    @Test
    @DisplayName("should handle HTTPS URLs with query parameters")
    void shouldHandleHttpsUrlsWithQueryParameters()
        {
        try (MockedStatic<dev.langchain4j.data.document.loader.UrlDocumentLoader> mockedUrlLoader = 
                mockStatic(dev.langchain4j.data.document.loader.UrlDocumentLoader.class))
            {
            URI httpsUri = URI.create("https://api.example.com/documents?token=abc123&format=pdf");
            
            when(mockParserSupplier.get()).thenReturn(mockDocumentParser);
            mockedUrlLoader.when(() ->
                dev.langchain4j.data.document.loader.UrlDocumentLoader.load(
                    eq("https://api.example.com/documents?token=abc123&format=pdf"), eq(mockDocumentParser)))
                .thenReturn(mockDocument);

            Document result = loader.load(httpsUri);

            assertThat(result, is(sameInstance(mockDocument)));
            }
        }

    @Test
    @DisplayName("should handle document with secure response metadata")
    void shouldHandleDocumentWithSecureResponseMetadata()
        {
        try (MockedStatic<dev.langchain4j.data.document.loader.UrlDocumentLoader> mockedUrlLoader = 
                mockStatic(dev.langchain4j.data.document.loader.UrlDocumentLoader.class))
            {
            URI httpsUri = URI.create("https://secure.example.com/document.pdf");
            Metadata metadata = Metadata.from("content-type", "application/pdf");
            Document documentWithMetadata = Document.from("Secure document content", metadata);
            
            when(mockParserSupplier.get()).thenReturn(mockDocumentParser);
            mockedUrlLoader.when(() ->
                dev.langchain4j.data.document.loader.UrlDocumentLoader.load(
                    any(String.class), eq(mockDocumentParser)))
                .thenReturn(documentWithMetadata);

            Document result = loader.load(httpsUri);

            assertThat(result, is(sameInstance(documentWithMetadata)));
            assertThat(result.metadata().getString("content-type"), is("application/pdf"));
            }
        }
    }
