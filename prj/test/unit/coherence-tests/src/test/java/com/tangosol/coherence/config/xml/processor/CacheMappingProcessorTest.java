/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.CacheMapping;
import com.tangosol.coherence.config.ParameterMacroExpressionParser;
import com.tangosol.coherence.config.xml.CacheConfigNamespaceHandler;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.expression.NullParameterResolver;
import com.tangosol.config.expression.ParameterResolver;
import com.tangosol.config.xml.DocumentProcessor;
import com.tangosol.config.xml.DocumentProcessor.DefaultDependencies;

import com.tangosol.run.xml.XmlDocumentReference;

import com.tangosol.util.SimpleResourceRegistry;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URISyntaxException;

/**
 * Unit Tests for {@link CacheMappingProcessor}s
 *
 * @author bo  2011.06.24
 */
public class CacheMappingProcessorTest
    {
    /**
     * Ensure that we can create a {@link CacheMapping} from a <cache-mapping>
     *
     * @throws URISyntaxException if there is a problem with the URI.
     * @throws ConfigurationException if there is a problem with the parsing.
     */
    @Test
    public void testRegularClassSchemeProcessing()
            throws URISyntaxException, ConfigurationException
        {
        String sXml = "<cache-mapping>" + "<cache-name>dist-*</cache-name>" + "<scheme-name>Distributed</scheme-name>"
                      + "<init-params>"
                      + "<init-param><param-name>size</param-name><param-value>100</param-value></init-param>"
                      + "<init-param><param-name>autostart</param-name><param-value>true</param-value></init-param>"
                      + "<init-param><param-name>name</param-name><param-value>rolf harris</param-value></init-param>"
                      + "</init-params>" + "</cache-mapping>";

        DefaultDependencies dep = new DocumentProcessor.DefaultDependencies();

        dep.setDefaultNamespaceHandler(new CacheConfigNamespaceHandler());
        dep.setExpressionParser(ParameterMacroExpressionParser.INSTANCE);
        dep.setResourceRegistry(new SimpleResourceRegistry());

        DocumentProcessor processor = new DocumentProcessor(dep);

        CacheMapping      mapping   = (CacheMapping) processor.process(new XmlDocumentReference(sXml));

        assertNotNull(mapping);
        assertEquals("dist-*", mapping.getNamePattern());
        assertEquals("Distributed", mapping.getSchemeName());
        assertTrue(mapping.isForName("dist-test"));
        assertFalse(mapping.isForName("dist"));

        ParameterResolver resolver     = mapping.getParameterResolver();
        ParameterResolver nullResolver = new NullParameterResolver();

        assertEquals(100, (int) resolver.resolve("size").evaluate(nullResolver).as(Integer.class));
        assertTrue(resolver.resolve("autostart").evaluate(nullResolver).as(Boolean.class));
        assertEquals("rolf harris", resolver.resolve("name").evaluate(nullResolver).as(String.class));
        }
    }
