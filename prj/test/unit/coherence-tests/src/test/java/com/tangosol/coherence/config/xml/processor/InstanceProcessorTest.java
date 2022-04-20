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

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.expression.NullParameterResolver;
import com.tangosol.config.expression.ParameterResolver;
import com.tangosol.config.xml.DocumentProcessor;
import com.tangosol.config.xml.DocumentProcessor.DefaultDependencies;

import com.tangosol.run.xml.XmlDocumentReference;

import com.tangosol.util.ResourceRegistry;
import com.tangosol.util.SimpleResourceRegistry;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.Point;
import java.awt.Rectangle;

import java.net.URISyntaxException;

/**
 * Unit Tests for {@link InstanceProcessor}s
 *
 * @author bo  2011.06.24
 * @author pfm 2013.05.10
 */
public class InstanceProcessorTest
    {
    /**
     * Ensure that we can create a {@link ParameterizedBuilder} based on a <instance>
     * that contains other nested <instance> elements
     *
     * @throws URISyntaxException if there is a problem with the URI.
     * @throws ConfigurationException if there is a problem with the parsing.
     */
    @Test
    public void testNestedInstanceProcessing()
            throws URISyntaxException, ConfigurationException
        {
        String sXml =
                "<instance>" + "<class-name>java.awt.Rectangle</class-name>" + "<init-params>"
                        + "<init-param><param-value><instance>" + "<class-name>java.awt.Point</class-name>"
                        + "<init-params><init-param><param-type>int</param-type><param-value>100</param-value></init-param>"
                        + "<init-param><param-type>int</param-type><param-value>100</param-value></init-param>"
                        + "</init-params></instance></param-value></init-param>" + "</init-params></instance>";

        ResourceRegistry resourceRegistry = new SimpleResourceRegistry();

        DefaultDependencies
                dep = new DocumentProcessor.DefaultDependencies(new CacheConfigNamespaceHandler());

        dep.setExpressionParser(ParameterMacroExpressionParser.INSTANCE);
        dep.setResourceRegistry(resourceRegistry);

        DocumentProcessor       processor = new DocumentProcessor(dep);

        ParameterizedBuilder<?> builder   = processor.process(new XmlDocumentReference(sXml));
        ParameterResolver resolver  = new NullParameterResolver();

        assertTrue(builder != null);
        assertTrue(ParameterizedBuilderHelper
                .realizes(builder, Rectangle.class, resolver, null));
        assertEquals(builder.realize(resolver, null, null), new Rectangle(new Point(100, 100)));
        }
    }
