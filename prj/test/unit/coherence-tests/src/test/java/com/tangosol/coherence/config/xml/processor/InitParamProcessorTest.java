/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.ParameterMacroExpressionParser;
import com.tangosol.coherence.config.ResolvableParameterList;
import com.tangosol.coherence.config.xml.CacheConfigNamespaceHandler;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.expression.NullParameterResolver;
import com.tangosol.config.expression.Parameter;
import com.tangosol.config.expression.ParameterResolver;
import com.tangosol.config.xml.DocumentProcessor;
import com.tangosol.config.xml.DocumentProcessor.DefaultDependencies;

import com.tangosol.net.NamedCache;

import com.tangosol.run.xml.XmlDocumentReference;

import com.tangosol.util.ResourceRegistry;
import com.tangosol.util.SimpleResourceRegistry;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.URISyntaxException;

/**
 * Unit Tests for {@link InitParamProcessor}.
 *
 * @author bo  2011.06.24
 */
public class InitParamProcessorTest
    {
    /**
     * Ensure that we can create a {@link Parameter} using a {@link InitParamProcessor}
     *
     * @throws URISyntaxException      if there is a problem with the URI
     * @throws ConfigurationException  if there is a problem with the parsing
     */
    @Test
    public void testExplicitlyTypedInitParamProcessing()
            throws URISyntaxException, ConfigurationException
        {
        String sXml =
            "<init-param><param-name>size</param-name><param-type>long</param-type><param-value>100</param-value></init-param>";

        ResourceRegistry    resourceRegistry = new SimpleResourceRegistry();

        DefaultDependencies dep = new DocumentProcessor.DefaultDependencies(new CacheConfigNamespaceHandler());

        dep.setExpressionParser(ParameterMacroExpressionParser.INSTANCE);
        dep.setResourceRegistry(resourceRegistry);

        DocumentProcessor processor = new DocumentProcessor(dep);

        Parameter         parameter = processor.process(new XmlDocumentReference(sXml));
        ParameterResolver resolver  = new NullParameterResolver();

        assertEquals("size", parameter.getName());
        assertTrue(parameter.isExplicitlyTyped());
        assertEquals(Long.class, parameter.getExplicitType());
        assertEquals(Long.valueOf(100), (Long) parameter.evaluate(resolver).get());
        }

    /**
     * Ensure that we can create a {@link Parameter} using a {@link InitParamProcessor} (without specifying a type)
     *
     * @throws URISyntaxException      if there is a problem with the URI
     * @throws ConfigurationException  if there is a problem with the parsing
     */
    @Test
    public void testTypeLessInitParamProcessing()
            throws URISyntaxException, ConfigurationException
        {
        String sXml = "<init-param><param-name>size</param-name><param-value>100</param-value></init-param>";

        ResourceRegistry    resourceRegistry = new SimpleResourceRegistry();

        DefaultDependencies dep = new DocumentProcessor.DefaultDependencies(new CacheConfigNamespaceHandler());

        dep.setExpressionParser(ParameterMacroExpressionParser.INSTANCE);
        dep.setResourceRegistry(resourceRegistry);

        DocumentProcessor processor = new DocumentProcessor(dep);

        Parameter         parameter = processor.process(new XmlDocumentReference(sXml));
        ParameterResolver resolver  = new NullParameterResolver();

        assertEquals("size", parameter.getName());
        assertEquals(100, (int) parameter.evaluate(resolver).as(Integer.class));
        }

    /**
     * Ensure that we create an appropriate {@link Parameter} when using {cache-ref} as a type.
     *
     * @throws URISyntaxException      if there is a problem with the URI
     * @throws ConfigurationException  if there is a problem with the parsing
     */
    @Test
    public void testCacheRefInitParamProcessing()
            throws URISyntaxException, ConfigurationException
        {
        String sXml =
            "<init-param><param-type>{cache-ref}</param-type><param-value>dist-cache</param-value></init-param>";

        ResourceRegistry    resourceRegistry = new SimpleResourceRegistry();

        DefaultDependencies dep = new DocumentProcessor.DefaultDependencies(new CacheConfigNamespaceHandler());

        dep.setExpressionParser(ParameterMacroExpressionParser.INSTANCE);
        dep.setResourceRegistry(resourceRegistry);

        DocumentProcessor processor = new DocumentProcessor(dep);

        Parameter         parameter = processor.process(new XmlDocumentReference(sXml));

        assertTrue(parameter.isExplicitlyTyped());
        assertEquals(NamedCache.class, parameter.getExplicitType());
        }

    /**
     * Ensure that we create an appropriate {@link Parameter} when using a non-standard {cache-name} value
     *
     * @throws URISyntaxException      if there is a problem with the URI
     * @throws ConfigurationException  if there is a problem with the parsing
     */
    @Test
    public void testCacheNameInitParamProcessing()
            throws URISyntaxException, ConfigurationException
        {
        String sXml =
            "<init-param><param-name>Example</param-name><param-value>data.persistence.{cache-name}</param-value></init-param>";

        ResourceRegistry    resourceRegistry = new SimpleResourceRegistry();

        DefaultDependencies dep = new DocumentProcessor.DefaultDependencies(new CacheConfigNamespaceHandler());

        dep.setExpressionParser(ParameterMacroExpressionParser.INSTANCE);
        dep.setResourceRegistry(resourceRegistry);

        DocumentProcessor processor = new DocumentProcessor(dep);

        Parameter         parameter = processor.process(new XmlDocumentReference(sXml));

        assertFalse(parameter.isExplicitlyTyped());
        assertEquals("Example", parameter.getName());

        ResolvableParameterList resolver = new ResolvableParameterList();

        resolver.add(new Parameter("cache-name", "Person"));

        assertEquals("data.persistence.Person", parameter.evaluate(resolver).as(String.class));
        }
    }
