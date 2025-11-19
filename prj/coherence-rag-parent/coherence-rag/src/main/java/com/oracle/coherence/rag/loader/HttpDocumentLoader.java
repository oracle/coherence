/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.loader;

import com.oracle.coherence.rag.DocumentLoader;

import com.oracle.coherence.rag.ParserProvider;
import com.oracle.coherence.rag.parser.ParserSupplier;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.UrlDocumentLoader;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.net.URI;

/**
 * Document loader implementation for HTTP-based document retrieval.
 * <p/>
 * This CDI-managed implementation of {@link DocumentLoader} provides
 * support for loading documents from HTTP URLs. It leverages LangChain4J's
 * UrlDocumentLoader for the actual HTTP operations and supports various
 * document formats through pluggable document parsers.
 * <p/>
 * The loader handles HTTP-specific concerns such as redirects, headers,
 * and response codes, while delegating document parsing to the injected
 * DocumentParser implementation. This allows for flexible support of
 * different file formats served over HTTP.
 * <p/>
 * Usage example:
 * <pre>
 * URI httpUri = URI.create("http://example.com/document.pdf");
 * Document doc = httpDocumentLoader.load(httpUri);
 * </pre>
 * 
 * @author Aleks Seovic  2025.07.04
 * @since 25.09
 */
@Named("http")
@ApplicationScoped
public class HttpDocumentLoader implements DocumentLoader
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct {@link HttpDocumentLoader} instance.
     *
     * @param parserSupplier the ParserSupplier to use
     */
    @Inject
    public HttpDocumentLoader(ParserSupplier parserSupplier)
        {
        m_parserSupplier = parserSupplier;
        }

    // ---- DocumentLoader interface ----------------------------------------

    /**
     * Loads a document from an HTTP URL.
     * <p/>
     * This method downloads the document from the specified HTTP URL and
     * parses its content using the injected DocumentParser. The method
     * handles HTTP protocol specifics and follows redirects as needed.
     * 
     * @param uri the HTTP URI pointing to the document to load,
     *            must use the "http" scheme
     * 
     * @return a Document object containing the parsed content and metadata
     * 
     * @throws IllegalArgumentException if the URI is not a valid HTTP URI
     * @throws RuntimeException if the document cannot be downloaded or parsed
     */
    public Document load(URI uri)
        {
        return UrlDocumentLoader.load(uri.toString(), m_parserSupplier.get());
        }

    // ---- data members ----------------------------------------------------

    /**
     * The document parser supplier.
     * <p/>
     * The parser handles the format-specific parsing logic for different
     * types of documents (PDF, DOC, TXT, etc.) and is injected by the
     * CDI container based on the configured parser implementation.
     */
    private ParserSupplier m_parserSupplier;
    }
