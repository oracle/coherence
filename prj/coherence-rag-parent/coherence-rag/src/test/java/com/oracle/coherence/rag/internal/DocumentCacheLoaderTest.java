/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.internal;

import com.oracle.coherence.rag.DocumentLoader;

import dev.langchain4j.data.document.Document;

import jakarta.enterprise.context.spi.CreationalContext;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.Collections;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DocumentCacheLoader} class.
 * <p/>
 * This test class validates the document cache loader functionality including
 * on-demand document loading, CDI integration, and error handling.
 * 
 * @author Aleks Seovic  2025.07.04
 * @since 25.09
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentCacheLoader")
class DocumentCacheLoaderTest
    {
    @Mock
    private BeanManager mockBeanManager;

    @Mock
    private DocumentLoader mockDocumentLoader;

    @Mock
    private Document mockDocument;

    @Mock
    private Bean<DocumentLoader> mockBean;

    @Mock
    private CreationalContext<DocumentLoader> mockCreationalContext;

    private DocumentCacheLoader loader;

    @BeforeEach
    void setUp() throws Exception
        {
        loader = new DocumentCacheLoader();
        
        // Inject mock bean manager using reflection
        Field beanManagerField = DocumentCacheLoader.class.getDeclaredField("beanManager");
        beanManagerField.setAccessible(true);
        beanManagerField.set(loader, mockBeanManager);
        }

    @Test
    @DisplayName("should return null when no loader found")
    void shouldReturnNullWhenNoLoaderFound()
        {
        String docId = "unknown://example.com/document.pdf";
        when(mockBeanManager.getBeans(DocumentLoader.class))
            .thenReturn(Collections.emptySet());

        Document result = loader.load(docId);

        assertThat(result, is(nullValue()));
        }

    @Test
    @DisplayName("should handle bean lookup failure gracefully")
    void shouldHandleBeanLookupFailureGracefully()
        {
        String docId = "http://example.com/document.pdf";
        
        // Setup beans but with no matching name
        Bean<DocumentLoader> unnamedBean = mock(Bean.class);
        when(unnamedBean.getName()).thenReturn(null);
        
        when(mockBeanManager.getBeans(DocumentLoader.class))
            .thenReturn(Set.of(unnamedBean));

        Document result = loader.load(docId);

        assertThat(result, is(nullValue()));
        }

    @Test
    @DisplayName("should handle invalid URI formats by throwing exceptions")
    void shouldHandleInvalidUriFormatsByThrowingExceptions()
        {
        String[] invalidUris = {
            "://missing-scheme",
            "   ",  // whitespace throws exception
            "http://"  // incomplete authority throws exception
        };
        
        for (String invalidUri : invalidUris)
            {
            assertThrows(IllegalArgumentException.class, 
                () -> loader.load(invalidUri),
                "Should throw exception for invalid URI: " + invalidUri);
            }
        }

    @Test
    @DisplayName("should handle questionable but valid URIs gracefully")
    void shouldHandleQuestionableButValidUrisGracefully()
        {
        when(mockBeanManager.getBeans(DocumentLoader.class))
            .thenReturn(Collections.emptySet());

        // These are technically valid URIs even though they look suspicious
        String[] questionableUris = {
            "not-a-uri",  // Valid as a path-only URI
            "ftp://unsupported.com/doc.pdf"  // Valid but unsupported scheme
        };
        
        for (String uri : questionableUris)
            {
            Document result = loader.load(uri);
            assertThat("Should handle questionable URI: " + uri, result, is(nullValue()));
            }
        }

    @Test
    @DisplayName("should handle empty document IDs gracefully")
    void shouldHandleEmptyDocumentIdsGracefully()
        {
        when(mockBeanManager.getBeans(DocumentLoader.class))
            .thenReturn(Collections.emptySet());

        Document result = loader.load("");

        assertThat(result, is(nullValue()));
        }

    @Test
    @DisplayName("should handle null document IDs by throwing exceptions")
    void shouldHandleNullDocumentIdsByThrowingExceptions()
        {
        assertThrows(NullPointerException.class, () -> loader.load(null));
        }

    @Test
    @DisplayName("should return mock document for valid loader")
    void shouldReturnMockDocumentForValidLoader()
        {
        String docId = "http://example.com/document.pdf";
        setupMockDocumentLoader("http");
        
        when(mockDocumentLoader.load(any(URI.class))).thenReturn(mockDocument);

        Document result = loader.load(docId);

        assertThat(result, is(sameInstance(mockDocument)));
        verify(mockDocumentLoader).load(URI.create(docId));
        }

    @Test
    @DisplayName("should handle document loading exceptions gracefully")
    void shouldHandleDocumentLoadingExceptionsGracefully()
        {
        String docId = "http://example.com/invalid.pdf";
        setupMockDocumentLoader("http");
        
        when(mockDocumentLoader.load(any(URI.class)))
            .thenThrow(new RuntimeException("Connection failed"));

        Document result = loader.load(docId);

        assertThat(result, is(nullValue()));
        }

    @Test
    @DisplayName("should handle null document from loader")
    void shouldHandleNullDocumentFromLoader()
        {
        String docId = "file:///empty/document.txt";
        setupMockDocumentLoader("file");
        
        when(mockDocumentLoader.load(any(URI.class))).thenReturn(null);

        Document result = loader.load(docId);

        assertThat(result, is(nullValue()));
        }

    @Test
    @DisplayName("should handle various URI schemes")
    void shouldHandleVariousUriSchemes()
        {
        String[] schemes = {"http", "https", "file", "s3", "azure.blob"};
        
        for (String scheme : schemes)
            {
            String docId = scheme + "://example.com/document.pdf";
            setupMockDocumentLoader(scheme);
            
            when(mockDocumentLoader.load(any(URI.class))).thenReturn(mockDocument);

            Document result = loader.load(docId);

            assertThat("Should handle scheme: " + scheme, result, is(sameInstance(mockDocument)));
            }
        }

    @Test
    @DisplayName("should handle complex URI schemes")
    void shouldHandleComplexUriSchemes()
        {
        String docId = "azure.blob://container/path/document.docx";
        setupMockDocumentLoader("azure.blob");
        
        when(mockDocumentLoader.load(any(URI.class))).thenReturn(mockDocument);

        Document result = loader.load(docId);

        assertThat(result, is(sameInstance(mockDocument)));
        }

    @Test
    @DisplayName("should throw exception for loadAll not implemented")
    void shouldThrowExceptionForLoadAllNotImplemented()
        {
        assertThrows(org.apache.commons.lang3.NotImplementedException.class, 
            () -> loader.loadAll(java.util.Arrays.asList("doc1", "doc2")));
        }

    private void setupMockDocumentLoader(String scheme)
        {
        when(mockBean.getName()).thenReturn(scheme);
        when(mockBeanManager.getBeans(DocumentLoader.class))
            .thenReturn(Set.of(mockBean));
        when(mockBeanManager.createCreationalContext(mockBean))
            .thenReturn(mockCreationalContext);
        when(mockBeanManager.getReference(mockBean, DocumentLoader.class, mockCreationalContext))
            .thenReturn(mockDocumentLoader);
        }
    } 

