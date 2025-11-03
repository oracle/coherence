/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag;

import dev.langchain4j.data.document.DocumentParser;

/**
 * Interface for providing access to document parsers in the Coherence RAG framework.
 *
 * @author Aleks Seovic  2025.07.04
 * @since 25.09
 */
public interface ParserProvider
    {
    /**
     * Returns a document parser instance.
     *
     * @return a DocumentParser instance
     * 
     * @throws RuntimeException         if the parser cannot be created or configured
     */
    DocumentParser getDocumentParser();
    }
