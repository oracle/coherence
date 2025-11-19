/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.loader;

import com.oracle.coherence.rag.parser.ParserSupplier;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Document loader implementation for HTTPS-based document retrieval.
 * <p/>
 * This CDI-managed implementation extends {@link HttpDocumentLoader} to
 * provide support for loading documents from HTTPS URLs. It inherits
 * all the functionality of the HTTP loader while automatically handling
 * SSL/TLS encryption and certificate validation.
 * <p/>
 * The loader provides secure document retrieval over encrypted connections,
 * ensuring that document content remains protected during transmission.
 * It uses the same document parsing capabilities as the HTTP loader but
 * with the added security of HTTPS.
 * <p/>
 * Usage example:
 * <pre>
 * URI httpsUri = URI.create("https://secure.example.com/document.pdf");
 * Document doc = httpsDocumentLoader.load(httpsUri);
 * </pre>
 * 
 * @author Aleks Seovic  2025.07.04
 * @since 25.09
 */
@Named("https")
@ApplicationScoped
public class HttpsDocumentLoader
        extends HttpDocumentLoader
    {
    /**
     * Construct {@link HttpsDocumentLoader} instance.
     *
     * @param parserSupplier the ParserSupplier to use
     */
    @Inject
    public HttpsDocumentLoader(ParserSupplier parserSupplier)
        {
        super(parserSupplier);
        }
    }
