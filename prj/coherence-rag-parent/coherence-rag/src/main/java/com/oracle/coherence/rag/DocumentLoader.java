/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag;

import dev.langchain4j.data.document.Document;

import java.net.URI;

/**
 * Interface for loading documents from various URI sources.
 * <p/>
 * This interface provides a contract for document loading implementations
 * that can retrieve documents from different sources (files, URLs, databases, etc.)
 * and convert them into a standardized Document format for processing in the
 * Coherence RAG framework.
 * <p/>
 * Implementations of this interface should handle the specific protocol
 * and format requirements of their target URI schemes.
 * 
 * @author Aleks Seovic  2025.06.28
 * @since 25.09
 */
public interface DocumentLoader
    {
    /**
     * Loads a document from the specified URI.
     * 
     * This method retrieves the document content from the given URI and
     * returns it as a standardized Document object. The implementation should
     * handle any necessary authentication, connection management, and content
     * parsing specific to the URI scheme.
     * <p/>
     * The returned Document object should contain the full text content
     * and any available metadata from the source.
     * 
     * @param uri the URI pointing to the document to load
     * 
     * @return a Document object containing the loaded content and metadata
     * 
     * @throws IllegalArgumentException if the URI is null or invalid
     * @throws RuntimeException if the document cannot be loaded due to network,
     *         authentication, or parsing errors
     */
    Document load(URI uri);
    }
