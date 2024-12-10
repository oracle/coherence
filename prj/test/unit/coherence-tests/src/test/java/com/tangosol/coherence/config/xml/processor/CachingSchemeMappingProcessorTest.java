/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.CacheConfig;
import com.tangosol.coherence.config.CacheMapping;
import com.tangosol.coherence.config.ResourceMappingRegistry;
import com.tangosol.coherence.config.ParameterMacroExpressionParser;

import com.tangosol.coherence.config.xml.CacheConfigNamespaceHandler;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.DocumentProcessor;
import com.tangosol.config.xml.DocumentProcessor.DefaultDependencies;

import com.tangosol.run.xml.XmlDocumentReference;

import com.tangosol.util.ResourceRegistry;
import com.tangosol.util.SimpleResourceRegistry;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URISyntaxException;

/**
 * Unit Tests for {@link CachingSchemeMappingProcessor}s.
 *
 * @author bo  2011.07.10
 */
public class CachingSchemeMappingProcessorTest
    {
    /**
     * Ensure that we can initialize an empty {@link ResourceMappingRegistry} from a <caching-scheme-mapping>.
     *
     * @throws URISyntaxException if there is a problem with the URI.
     * @throws ConfigurationException if there is a problem with the parsing.
     */
    @Test
    public void testEmptyCachingSchemeMappingProcessing()
            throws URISyntaxException, ConfigurationException
        {
        String              sXml             = "<caching-scheme-mapping></caching-scheme-mapping>";
        ResourceRegistry    resourceRegistry = new SimpleResourceRegistry();
        DefaultDependencies dep              = new DocumentProcessor.DefaultDependencies(new CacheConfigNamespaceHandler());

        dep.setExpressionParser(ParameterMacroExpressionParser.INSTANCE);
        dep.setResourceRegistry(resourceRegistry);

        DocumentProcessor processor = new DocumentProcessor(dep);
        Object            result    = processor.process(new XmlDocumentReference(sXml));

        assertTrue(result instanceof ResourceMappingRegistry);

        ResourceMappingRegistry registry = (ResourceMappingRegistry) result;

        assertEquals(0, registry.size());
        }

    /**
     * Ensure that we can initialize a {@link ResourceMappingRegistry} from a <caching-scheme-mapping>.
     *
     * @throws URISyntaxException if there is a problem with the URI.
     * @throws ConfigurationException if there is a problem with the parsing.
     */
    @Test
    public void testCachingSchemeMappingProcessing()
            throws URISyntaxException, ConfigurationException
        {
        String sXml = "<cache-config>"
                      + "<caching-scheme-mapping>"
                      + "  <cache-mapping>"
                      + "    <cache-name>dist-*</cache-name>"
                      + "    <scheme-name>Distributed</scheme-name>"
                      + "    <init-params>"
                      + "      <init-param>"
                      + "        <param-name>size</param-name>"
                      + "        <param-value>100</param-value>"
                      + "      </init-param>"
                      + "      <init-param><param-name>autostart</param-name><param-value>true</param-value></init-param>"
                      + "      <init-param><param-name>name</param-name><param-value>rolf harris</param-value></init-param>"
                      + "    </init-params>"
                      + "  </cache-mapping>"
                      + "  <cache-mapping>"
                      + "    <cache-name>*</cache-name>"
                      + "    <scheme-name>Replicated</scheme-name>"
                      + "  </cache-mapping>"
                      + "</caching-scheme-mapping></cache-config>";

        ResourceRegistry    resourceRegistry = new SimpleResourceRegistry();

        DefaultDependencies dep = new DocumentProcessor.DefaultDependencies(new CacheConfigNamespaceHandler());

        dep.setExpressionParser(ParameterMacroExpressionParser.INSTANCE);
        dep.setResourceRegistry(resourceRegistry);

        DocumentProcessor    processor   = new DocumentProcessor(dep);

        CacheConfig          cacheConfig = processor.process(new XmlDocumentReference(sXml));

        ResourceMappingRegistry registry    = cacheConfig.getMappingRegistry();

        assertNotNull(registry);
        assertEquals(2, registry.size());

        CacheMapping mapping = registry.findMapping("dist-me", CacheMapping.class);

        assertNotNull(mapping);
        assertEquals("dist-*", mapping.getNamePattern());

        mapping = registry.findMapping("my-cache", CacheMapping.class);
        assertNotNull(mapping);
        assertEquals("*", mapping.getNamePattern());
        }
    }
