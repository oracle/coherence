/*
 * Copyright (c) 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.rag.parser;

import com.oracle.coherence.rag.ParserProvider;
import com.oracle.coherence.rag.util.CdiHelper;

import dev.langchain4j.data.document.DocumentParser;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.microprofile.config.Config;

@ApplicationScoped
public class ParserSupplier
    {
    /**
     * Construct {@link ParserSupplier} instance.
     *
     * @param config  Eclipse MP configuration
     */
    @Inject
    public ParserSupplier(Config config)
        {
        this.config = config;
        }

    /**
     * Returns the default model instance.
     * <p/>
     * This method provides access to the currently configured default model.
     * The model instance is created lazily on first access and cached for
     * subsequent requests.
     *
     * @return the default model instance
     */
    public DocumentParser get()
        {
        return get(defaultParserName());
        }

    /**
     * Returns a parser instance for the specified parser name.
     *
     * @param parserName the name of the parser to retrieve
     *
     * @return the parser instance for the specified name
     */
    public DocumentParser get(String parserName)
        {
        return mapParser.computeIfAbsent(parserName, this::create);
        }

    /**
     * Determines the default parser name from configuration.
     * <p/>
     * This method reads the configuration property specified by {@link #configProperty()}
     * and falls back to {@link #DEFAULT_PARSER} if no configuration is found.
     *
     * @return the name of the default parser
     */
    public String defaultParserName()
        {
        return config.getOptionalValue(configProperty(), String.class).orElse(DEFAULT_PARSER);
        }

    /**
     * Returns the configuration property key for default document parser.
     * <p/>
     * This property can be used to configure the default document parser
     * at runtime through various configuration sources.
     * 
     * @return {@code coherence.rag.default.parser} as the configuration property key
     */
    protected String configProperty()
        {
        return "coherence.rag.default.parser";
        }

    /**
     * Creates a new document parser instance for the specified parser name.
     *
     * @param parserName the name of the document parser to create
     * 
     * @return a new {@link DocumentParser} instance
     */
    public DocumentParser create(String parserName)
        {
        ParserProvider provider = null;
        try
            {
            provider = CdiHelper.getNamedBean(ParserProvider.class, parserName);
            if (provider == null)
                {
                provider = CdiHelper.getNamedBean(ParserProvider.class, defaultParserName());
                }
            }
        catch (IllegalStateException ignore)
            {
            // CDI is not available in integration tests
            }

        return provider == null
               ? new DefaultParserProvider().getDocumentParser()
               : provider.getDocumentParser();
        }

    // ---- constants -------------------------------------------------------

    /**
     * The name of the default parser to fallback on if not explicitly configured.
     */
    public static final String DEFAULT_PARSER = "default";

    // ---- data members ----------------------------------------------------

    /**
     * MicroProfile Config instance for accessing configuration properties.
     * <p/>
     * Used to read the default model name from configuration and provides
     * a standard way to access application configuration values.
     */
    protected Config config;

    /**
     * Thread-safe cache of created parser instances keyed by parser name.
     * <p/>
     * This map ensures that parser instances are created only once per name
     * and reused for subsequent requests, improving performance and resource
     * utilization.
     */
    private final ConcurrentMap<String, DocumentParser> mapParser = new ConcurrentHashMap<>();
    }
