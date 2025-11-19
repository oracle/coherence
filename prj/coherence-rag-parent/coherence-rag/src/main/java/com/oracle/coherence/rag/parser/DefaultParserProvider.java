/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.parser;

import com.oracle.coherence.rag.ParserProvider;

import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

/**
 * {@code DefaultParserProvider} returns an instance of Apache Tika {@link DocumentParser}.
 * <p/>
 * The default Apache Tika parser can parse any supported document format, and
 * will attempt to parse document metadata as well. If you need any additional
 * configuration, you can create a custom {@link ParserProvider} that returns
 * differently configured {@link ApacheTikaDocumentParser} instance.
 *
 * @author Aleks Seovic  2025.07.04
 * @since 25.09
 */
@ApplicationScoped
@Named("default")
public class DefaultParserProvider
        implements ParserProvider
    {
    @Override
    public DocumentParser getDocumentParser()
        {
        return new ApacheTikaDocumentParser(true);
        }
    }
