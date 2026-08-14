/*
 * Copyright (c) 2025, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.loader;

import com.oracle.coherence.rag.DocumentLoader;
import com.oracle.coherence.rag.api.RagSecurity;

import com.oracle.coherence.rag.parser.ParserSupplier;

import dev.langchain4j.data.document.BlankDocumentException;
import dev.langchain4j.data.document.Document;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.InputStream;

import java.net.URI;

import java.nio.file.Path;

/**
 * Document loader implementation for local file system documents.
 * <p/>
 * This CDI-managed implementation of {@link DocumentLoader} provides
 * support for loading documents from the local file system using the
 * "file" URI scheme. It opens allowed paths through the RAG security policy
 * and passes the resulting stream to a pluggable document parser.
 * <p/>
 * The loader automatically handles file path resolution and delegates
 * document parsing to the injected DocumentParser, allowing for flexible
 * support of different file formats (PDF, Word, plain text, etc.).
 * <p/>
 * Usage example:
 * <pre>
 * URI fileUri = URI.create("file:///path/to/document.pdf");
 * Document doc = fileDocumentLoader.load(fileUri);
 * </pre>
 * 
 * @author Aleks Seovic  2025.07.04
 * @since 25.09
 */
@Named("file")
@ApplicationScoped
public class FileDocumentLoader
        implements DocumentLoader
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Construct {@link FileDocumentLoader} instance.
     *
     * @param parserSupplier the ParserSupplier to use
     */
    @Inject
    public FileDocumentLoader(ParserSupplier parserSupplier)
        {
        m_parserSupplier = parserSupplier;
        }

    // ---- DocumentLoader interface ----------------------------------------

    /**
     * Loads a document from a local file system location.
     * <p/>
     * This method policy-checks and opens the allowed file, then delegates
     * parsing of the already-open stream to the injected DocumentParser
     * implementation.
     * 
     * @param uri the file URI pointing to the document to load,
     *            must use the "file" scheme
     * 
     * @return a Document object containing the parsed content and metadata
     * 
     * @throws IllegalArgumentException if the URI is not a valid file URI
     * @throws RuntimeException if the file cannot be read or parsed
     */
    public Document load(URI uri)
        {
        Path path = Path.of(uri).toAbsolutePath().normalize();
        try (InputStream in = RagSecurity.openValidatedFile(uri))
            {
            Document document = m_parserSupplier.get().parse(in);
            document.metadata()
                    .put("file_name", path.getFileName().toString())
                    .put("absolute_directory_path", path.getParent().toString());
            return document;
            }
        catch (BlankDocumentException | RagSecurity.PolicyViolation e)
            {
            throw e;
            }
        catch (Exception e)
            {
            throw new RuntimeException("Failed to load document", e);
            }
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
