/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.ParameterMacroExpressionParser;

import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilderHelper;

import com.tangosol.coherence.config.xml.CacheConfigNamespaceHandler;

import com.tangosol.config.expression.NullParameterResolver;
import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.config.xml.DocumentProcessor;

import com.tangosol.run.xml.XmlDocumentReference;

import com.tangosol.util.ResourceRegistry;
import com.tangosol.util.SimpleResourceRegistry;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

import static org.junit.Assert.assertThat;

/**
 * Base class for testing {@code class-scheme} based configuration processors.
 *
 * @author rlubke
 * @since 12.2.1.4
 */
public abstract class AbstractClassSchemeProcessorTest
    {
    // ----- helper methods -------------------------------------------------

    /**
     * Create a new {@link ParameterizedBuilder} based on the provided xml.
     *
     * @param sXml         xml as a String
     * @param clzExpected  the {@link Class} to validate against
     *                     {@link ParameterizedBuilderHelper#realizes(ParameterizedBuilder, Class, ParameterResolver, ClassLoader)}
     *
     * @return the {@link ParameterizedBuilder} based on the provided xml
     */
    @SuppressWarnings("deprecation")
    protected static Object createAndInvokeBuilder(String sXml, Class<?> clzExpected)
        {
        ResourceRegistry resourceRegistry = new SimpleResourceRegistry();

        DocumentProcessor.DefaultDependencies dep =
                new DocumentProcessor.DefaultDependencies(new CacheConfigNamespaceHandler());

        dep.setExpressionParser(ParameterMacroExpressionParser.INSTANCE);
        dep.setResourceRegistry(resourceRegistry);

        DocumentProcessor       processor = new DocumentProcessor(dep);
        ParameterizedBuilder<?> builder   = processor.process(new XmlDocumentReference(sXml));
        NullParameterResolver   resolver  = new NullParameterResolver();

        assertThat(builder, is(notNullValue()));
        assertThat(ParameterizedBuilderHelper.realizes(builder, clzExpected, resolver, null), is(true));

        return builder.realize(resolver, null, null);
        }
    }
