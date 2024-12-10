/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.builder;

import com.tangosol.coherence.config.ParameterMacroExpressionParser;
import com.tangosol.coherence.config.xml.CacheConfigNamespaceHandler;
import com.tangosol.config.xml.DocumentProcessor;
import com.tangosol.config.xml.DocumentProcessor.DefaultDependencies;


import com.tangosol.run.xml.XmlDocumentReference;

import com.tangosol.util.Base;
import com.tangosol.util.SimpleResourceRegistry;

/**
 * The BuilderFactory class instantiates a builder using CODI.
 *
 * @author pfm  2011.11.28
 */
public class BuilderFactory
    {
    /**
     * Create a builder from the given XML.
     *
     * @param sXml  the <local-scheme> XML element
     *
     * @return the builder
     */
    @SuppressWarnings("unchecked")
    public static <T>  T instantiateBuilder(String sXml)
        {
        DefaultDependencies deps = new DocumentProcessor.DefaultDependencies();

        deps.setClassLoader(Base.getContextClassLoader());
        deps.setDefaultNamespaceHandler(new CacheConfigNamespaceHandler());
        deps.setExpressionParser(ParameterMacroExpressionParser.INSTANCE);
        deps.setResourceRegistry(new SimpleResourceRegistry());
        T builder = null;

        try
            {
            builder = (T) new DocumentProcessor(deps).process(new XmlDocumentReference(sXml));
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e);
            }

        return builder;
        }
    }
