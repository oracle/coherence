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
        }

    @Test
    @DisplayName("should load document from HTTP URL")
    void shouldLoadDocumentFromHttpUrl()
        {
        try (MockedStatic<dev.langchain4j.data.document.loader.UrlDocumentLoader> mockedUrlLoader = 
                mockStatic(dev.langchain4j.data.document.loader.UrlDocumentLoader.class))
            {
            URI httpUri = URI.create("http://example.com/document.pdf");
            
            when(mockParserSupplier.get()).thenReturn(mockDocumentParser);
            mockedUrlLoader.when(() ->
                dev.langchain4j.data.document.loader.UrlDocumentLoader.load(
                    eq("http://example.com/document.pdf"), eq(mockDocumentParser)))
                .thenReturn(mockDocument);

            Document result = loader.load(httpUri);

            assertThat(result, is(sameInstance(mockDocument)));
            mockedUrlLoader.verify(() -> 
                dev.langchain4j.data.document.loader.UrlDocumentLoader.load(
                    "http://example.com/document.pdf", mockDocumentParser));
            }
        }

    @Test
    @DisplayName("should handle HTTP URLs with query parameters")
    void shouldHandleHttpUrlsWithQueryParameters()
        {
        try (MockedStatic<dev.langchain4j.data.document.loader.UrlDocumentLoader> mockedUrlLoader = 
                mockStatic(dev.langchain4j.data.document.loader.UrlDocumentLoader.class))
            {
            URI httpUri = URI.create("http://example.com/api/documents?id=123&format=pdf");
            
            when(mockParserSupplier.get()).thenReturn(mockDocumentParser);
            mockedUrlLoader.when(() ->
                dev.langchain4j.data.document.loader.UrlDocumentLoader.load(
                    eq("http://example.com/api/documents?id=123&format=pdf"), eq(mockDocumentParser)))
                .thenReturn(mockDocument);

            Document result = loader.load(httpUri);

            assertThat(result, is(sameInstance(mockDocument)));
            }
        }

    @Test
    @DisplayName("should handle HTTP URLs with different ports")
    void shouldHandleHttpUrlsWithDifferentPorts()
        {
        try (MockedStatic<dev.langchain4j.data.document.loader.UrlDocumentLoader> mockedUrlLoader = 
                mockStatic(dev.langchain4j.data.document.loader.UrlDocumentLoader.class))
            {
            URI httpUri = URI.create("http://localhost:8080/documents/report.txt");
            
            when(mockParserSupplier.get()).thenReturn(mockDocumentParser);
            mockedUrlLoader.when(() ->
                dev.langchain4j.data.document.loader.UrlDocumentLoader.load(
                    eq("http://localhost:8080/documents/report.txt"), eq(mockDocumentParser)))
                .thenReturn(mockDocument);

            Document result = loader.load(httpUri);

            assertThat(result, is(sameInstance(mockDocument)));
            }
        }

    @Test
    @DisplayName("should handle HTTP URLs with special characters")
    void shouldHandleHttpUrlsWithSpecialCharacters()
        {
        try (MockedStatic<dev.langchain4j.data.document.loader.UrlDocumentLoader> mockedUrlLoader = 
                mockStatic(dev.langchain4j.data.document.loader.UrlDocumentLoader.class))
            {
            URI httpUri = URI.create("http://example.com/documents/file%20with%20spaces.pdf");
            
            when(mockParserSupplier.get()).thenReturn(mockDocumentParser);
            mockedUrlLoader.when(() ->
                dev.langchain4j.data.document.loader.UrlDocumentLoader.load(
                    eq("http://example.com/documents/file%20with%20spaces.pdf"), eq(mockDocumentParser)))
                .thenReturn(mockDocument);

            Document result = loader.load(httpUri);

            assertThat(result, is(sameInstance(mockDocument)));
            }
        }

    @Test
    @DisplayName("should handle null document response")
    void shouldHandleNullDocumentResponse()
        {
        try (MockedStatic<dev.langchain4j.data.document.loader.UrlDocumentLoader> mockedUrlLoader = 
                mockStatic(dev.langchain4j.data.document.loader.UrlDocumentLoader.class))
            {
            URI httpUri = URI.create("http://example.com/empty-document.txt");
            
            when(mockParserSupplier.get()).thenReturn(mockDocumentParser);
            mockedUrlLoader.when(() ->
                dev.langchain4j.data.document.loader.UrlDocumentLoader.load(
                    any(String.class), eq(mockDocumentParser)))
                .thenReturn(null);

            Document result = loader.load(httpUri);

            assertThat(result, is(nullValue()));
            }
        }

    @Test
    @DisplayName("should handle document with HTTP response metadata")
    void shouldHandleDocumentWithHttpResponseMetadata()
        {
        try (MockedStatic<dev.langchain4j.data.document.loader.UrlDocumentLoader> mockedUrlLoader = 
                mockStatic(dev.langchain4j.data.document.loader.UrlDocumentLoader.class))
            {
            URI httpUri = URI.create("http://example.com/document.pdf");
            Metadata metadata = Metadata.from("content-type", "application/pdf");
            Document documentWithMetadata = Document.from("Document content", metadata);
            
            when(mockParserSupplier.get()).thenReturn(mockDocumentParser);
            mockedUrlLoader.when(() ->
                dev.langchain4j.data.document.loader.UrlDocumentLoader.load(
                    any(String.class), eq(mockDocumentParser)))
                .thenReturn(documentWithMetadata);

            Document result = loader.load(httpUri);

            assertThat(result, is(sameInstance(documentWithMetadata)));
            assertThat(result.metadata().getString("content-type"), is("application/pdf"));
            }
        }

    @Test
    @DisplayName("should handle HTTP URLs with fragment identifiers")
    void shouldHandleHttpUrlsWithFragmentIdentifiers()
        {
        try (MockedStatic<dev.langchain4j.data.document.loader.UrlDocumentLoader> mockedUrlLoader = 
                mockStatic(dev.langchain4j.data.document.loader.UrlDocumentLoader.class))
            {
            URI httpUri = URI.create("http://example.com/document.html#section1");
            
            when(mockParserSupplier.get()).thenReturn(mockDocumentParser);
            mockedUrlLoader.when(() ->
                dev.langchain4j.data.document.loader.UrlDocumentLoader.load(
                    eq("http://example.com/document.html#section1"), eq(mockDocumentParser)))
                .thenReturn(mockDocument);

            Document result = loader.load(httpUri);

            assertThat(result, is(sameInstance(mockDocument)));
            }
        }
    }
