/*
 * Copyright (c) 2025, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.loader;

import com.oracle.coherence.rag.api.RagSecurity;
import com.oracle.coherence.rag.DocumentLoader;

import com.oracle.coherence.rag.parser.ParserSupplier;
import dev.langchain4j.data.document.Document;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
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
        try
            {
            HttpURLConnection connection = RagSecurity.openValidatedHttpConnection(uri);
            try (InputStream in = new BoundedInputStream(connection.getInputStream(), RagSecurity.maxImportBytes()))
                {
                return m_parserSupplier.get().parse(in);
                }
            finally
                {
                connection.disconnect();
                }
            }
        catch (IOException e)
            {
            throw new RuntimeException(e);
            }
        }

    // ---- inner class: BoundedInputStream --------------------------------

    /**
     * Input stream wrapper that enforces the configured import byte cap.
     */
    static class BoundedInputStream
            extends InputStream
        {
        BoundedInputStream(InputStream in, long cbMax)
            {
            this.in    = in;
            this.cbMax = cbMax;
            }

        @Override
        public int read()
                throws IOException
            {
            int b = in.read();
            if (b != -1)
                {
                check(1);
                }
            return b;
            }

        @Override
        public int read(byte[] ab, int of, int cb)
                throws IOException
            {
            int cbRead = in.read(ab, of, cb);
            if (cbRead > 0)
                {
                check(cbRead);
                }
            return cbRead;
            }

        @Override
        public void close()
                throws IOException
            {
            in.close();
            }

        private void check(int cbRead)
            {
            cb += cbRead;
            if (cb > cbMax)
                {
                throw new RagSecurity.PolicyViolation(
                        RagSecurity.GATE_IMPORT_URI_ALLOWLIST,
                        RagSecurity.REASON_IMPORT_BODY_EXCEEDS_CAP);
                }
            }

        private final InputStream in;
        private final long        cbMax;
        private long              cb;
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
