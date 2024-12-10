/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.ParameterMacroExpressionParser;
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

import java.net.URISyntaxException;

/**
 * Unit Tests for {@link InitParamsProcessor}.
 *
 * @author bo  2011.06.24
 */
public class InitParamsProcessorTest
    {
    /**
     * Ensure that we can create a ParameterResolver using a {@link InitParamsProcessor}.
     *
     * @throws URISyntaxException if there is a problem with the URI.
     * @throws ConfigurationException if there is a problem with the parsing.
     */
    @Test
    public void testInitParamsProcessing()
            throws URISyntaxException, ConfigurationException
        {
        String sXml = "<init-params>"
                      + "<init-param><param-name>size</param-name><param-value>100</param-value></init-param>"
                      + "<init-param><param-name>autostart</param-name><param-value>true</param-value></init-param>"
                      + "<init-param><param-name>name</param-name><param-value>rolf harris</param-value></init-param>"
                      + "</init-params>";

        ResourceRegistry    resourceRegistry = new SimpleResourceRegistry();

        DefaultDependencies dep = new DocumentProcessor.DefaultDependencies(new CacheConfigNamespaceHandler());

        dep.setExpressionParser(ParameterMacroExpressionParser.INSTANCE);
        dep.setResourceRegistry(resourceRegistry);

        DocumentProcessor processor  = new DocumentProcessor(dep);

        ParameterResolver initParams = processor.process(new XmlDocumentReference(sXml));
        ParameterResolver resolver   = new NullParameterResolver();

        assertEquals(100, (int) initParams.resolve("size").evaluate(resolver).as(Integer.class));
        assertTrue(initParams.resolve("autostart").evaluate(resolver).as(Boolean.class));
        assertEquals("rolf harris", initParams.resolve("name").evaluate(resolver).as(String.class));
        }
    }
