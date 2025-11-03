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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link FileDocumentLoader} class.
 * <p/>
 * This test class validates the file document loader functionality including
 * URI handling, document parser integration, and error scenarios.
 * 
 * @author Aleks Seovic  2025.07.04
 * @since 25.09
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FileDocumentLoader")
class FileDocumentLoaderTest
    {
    @Mock
    private DocumentParser mockDocumentParser;

    @Mock
    private ParserSupplier mockParserSupplier;

    @Mock
    private Document mockDocument;

    private FileDocumentLoader loader;

    @BeforeEach
    void setUp()
        {
        loader = new FileDocumentLoader(mockParserSupplier);
        }

    @Test
    @DisplayName("should load document from file URI")
    void shouldLoadDocumentFromFileUri()
        {
        try (MockedStatic<dev.langchain4j.data.document.loader.FileSystemDocumentLoader> mockedFileSystemLoader = 
                mockStatic(dev.langchain4j.data.document.loader.FileSystemDocumentLoader.class))
            {
            URI fileUri = URI.create("file:///path/to/document.pdf");
            
            when(mockParserSupplier.get()).thenReturn(mockDocumentParser);
            mockedFileSystemLoader.when(() ->
                dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument(
                    eq("/path/to/document.pdf"), eq(mockDocumentParser)))
                .thenReturn(mockDocument);

            Document result = loader.load(fileUri);

            assertThat(result, is(sameInstance(mockDocument)));
            mockedFileSystemLoader.verify(() -> 
                dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument(
                    "/path/to/document.pdf", mockDocumentParser));
            }
        }

    @Test
    @DisplayName("should handle various file formats")
    void shouldHandleVariousFileFormats()
        {
        when(mockParserSupplier.get()).thenReturn(mockDocumentParser);

        try (MockedStatic<dev.langchain4j.data.document.loader.FileSystemDocumentLoader> mockedFileSystemLoader =
                mockStatic(dev.langchain4j.data.document.loader.FileSystemDocumentLoader.class))
            {
            String[] fileFormats = {
                "file:///documents/report.pdf",
                "file:///documents/notes.txt", 
                "file:///documents/presentation.docx",
                "file:///documents/data.csv"
            };

            for (String fileUriString : fileFormats)
                {
                URI fileUri = URI.create(fileUriString);
                String expectedPath = fileUri.getPath();
                
                mockedFileSystemLoader.when(() -> 
                    dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument(
                        eq(expectedPath), eq(mockDocumentParser)))
                    .thenReturn(mockDocument);

                Document result = loader.load(fileUri);

                assertThat(result, is(sameInstance(mockDocument)));
                mockedFileSystemLoader.verify(() -> 
                    dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument(
                        expectedPath, mockDocumentParser));
                }
            }
        }

    @Test
    @DisplayName("should propagate parsing exceptions")
    void shouldPropagateParsingExceptions()
        {
        try (MockedStatic<dev.langchain4j.data.document.loader.FileSystemDocumentLoader> mockedFileSystemLoader = 
                mockStatic(dev.langchain4j.data.document.loader.FileSystemDocumentLoader.class))
            {
            URI fileUri = URI.create("file:///invalid/document.pdf");
            RuntimeException expectedException = new RuntimeException("Failed to parse document");
            
            when(mockParserSupplier.get()).thenReturn(mockDocumentParser);
            mockedFileSystemLoader.when(() ->
                dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument(
                    any(String.class), eq(mockDocumentParser)))
                .thenThrow(expectedException);

            RuntimeException exception = assertThrows(RuntimeException.class, 
                () -> loader.load(fileUri));

            assertThat(exception, is(sameInstance(expectedException)));
            }
        }

    @Test
    @DisplayName("should handle null document from parser")
    void shouldHandleNullDocumentFromParser()
        {
        try (MockedStatic<dev.langchain4j.data.document.loader.FileSystemDocumentLoader> mockedFileSystemLoader = 
                mockStatic(dev.langchain4j.data.document.loader.FileSystemDocumentLoader.class))
            {
            URI fileUri = URI.create("file:///empty/document.txt");
            
            when(mockParserSupplier.get()).thenReturn(mockDocumentParser);
            mockedFileSystemLoader.when(() ->
                dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument(
                    any(String.class), eq(mockDocumentParser)))
                .thenReturn(null);

            Document result = loader.load(fileUri);

            assertThat(result, is(nullValue()));
            }
        }

    @Test
    @DisplayName("should handle document with metadata")
    void shouldHandleDocumentWithMetadata()
        {
        try (MockedStatic<dev.langchain4j.data.document.loader.FileSystemDocumentLoader> mockedFileSystemLoader = 
                mockStatic(dev.langchain4j.data.document.loader.FileSystemDocumentLoader.class))
            {
            URI fileUri = URI.create("file:///path/to/document.pdf");
            Metadata metadata = Metadata.from("author", "John Doe");
            Document documentWithMetadata = Document.from("Document content", metadata);
            
            when(mockParserSupplier.get()).thenReturn(mockDocumentParser);
            mockedFileSystemLoader.when(() ->
                dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument(
                    any(String.class), eq(mockDocumentParser)))
                .thenReturn(documentWithMetadata);

            Document result = loader.load(fileUri);

            assertThat(result, is(sameInstance(documentWithMetadata)));
            assertThat(result.metadata().getString("author"), is("John Doe"));
            }
        }

    @Test
    @DisplayName("should handle filesystem edge cases")
    void shouldHandleFilesystemEdgeCases()
        {
        try (MockedStatic<dev.langchain4j.data.document.loader.FileSystemDocumentLoader> mockedFileSystemLoader = 
                mockStatic(dev.langchain4j.data.document.loader.FileSystemDocumentLoader.class))
            {
            URI fileUri = URI.create("file:///tmp/test.txt");
            
            when(mockParserSupplier.get()).thenReturn(mockDocumentParser);
            mockedFileSystemLoader.when(() ->
                dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument(
                    eq("/tmp/test.txt"), eq(mockDocumentParser)))
                .thenReturn(mockDocument);

            Document result = loader.load(fileUri);

            assertThat(result, is(sameInstance(mockDocument)));
            }
        }

    @Test
    @DisplayName("should handle sub-directory file paths")
    void shouldHandleSubDirectoryFilePaths()
        {
        try (MockedStatic<dev.langchain4j.data.document.loader.FileSystemDocumentLoader> mockedFileSystemLoader = 
                mockStatic(dev.langchain4j.data.document.loader.FileSystemDocumentLoader.class))
            {
            URI fileUri = URI.create("file:///home/user/docs/readme.md");
            
            when(mockParserSupplier.get()).thenReturn(mockDocumentParser);
            mockedFileSystemLoader.when(() ->
                dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument(
                    any(String.class), eq(mockDocumentParser)))
                .thenReturn(mockDocument);

            Document result = loader.load(fileUri);

            assertThat(result, is(sameInstance(mockDocument)));
            }
        }
    }
