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

import com.tangosol.io.SerializerFactory;

import com.tangosol.run.xml.XmlDocumentReference;

import com.tangosol.util.ResourceRegistry;
import com.tangosol.util.SimpleResourceRegistry;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;

import static org.junit.Assert.assertThat;

import java.net.URISyntaxException;

/**
 * Unit Tests for {@link DefaultsProcessor}
 *
 * @author bo  2013.12.01
 */
public class DefaultsProcessorTest
    {
    /**
     * Ensure that processing a simple &lt;defaults&gt; element registers elements with the
     * {@link ResourceRegistry}.
     *
     * @throws java.net.URISyntaxException if there is a problem with the URI.
     * @throws com.tangosol.config.ConfigurationException if there is a problem with the parsing.
     */
    @Test
    public void testRegisterSimpleDefaults()
            throws URISyntaxException, ConfigurationException
        {
        String sXml = "<defaults>" + "<serializer>pof</serializer>" + "<scope-name>My Scope</scope-name>"
                      + "</defaults>";

        ResourceRegistry    resourceRegistry = new SimpleResourceRegistry();

        DefaultDependencies dep              = new DefaultDependencies(new CacheConfigNamespaceHandler());

        dep.setExpressionParser(ParameterMacroExpressionParser.INSTANCE);
        dep.setResourceRegistry(resourceRegistry);

        DocumentProcessor processor = new DocumentProcessor(dep);

        processor.process(new XmlDocumentReference(sXml));

        assertThat(resourceRegistry.getResource(String.class, "scope-name"), is("My Scope"));
        assertThat(resourceRegistry.getResource(SerializerFactory.class, "serializer"),
                   instanceOf(SerializerFactory.class));
        }

    /**
     * Ensure that processing a complex &lt;defaults&gt; element registers elements with the
     * {@link ResourceRegistry}.
     *
     * @throws java.net.URISyntaxException if there is a problem with the URI.
     * @throws com.tangosol.config.ConfigurationException if there is a problem with the parsing.
     */
    @Test
    public void testRegisterComplexDefaults()
            throws URISyntaxException, ConfigurationException
        {
        String sXml = "<defaults>" + "<serializer>" + "<instance>"
                      + "<class-name>com.tangosol.io.pof.SafeConfigurablePofContext</class-name>" + "<init-params>"
                      + "<init-param>" + "<param-type>String</param-type>"
                      + "<param-value>pof-config.xml</param-value>\n" + "</init-param>" + "</init-params>"
                      + "</instance>" + "</serializer>" + "</defaults>";

        ResourceRegistry    resourceRegistry = new SimpleResourceRegistry();

        DefaultDependencies dep              = new DefaultDependencies(new CacheConfigNamespaceHandler());

        dep.setExpressionParser(ParameterMacroExpressionParser.INSTANCE);
        dep.setResourceRegistry(resourceRegistry);

        DocumentProcessor processor = new DocumentProcessor(dep);

        processor.process(new XmlDocumentReference(sXml));

        assertThat(resourceRegistry.getResource(SerializerFactory.class, "serializer"),
                   instanceOf(SerializerFactory.class));
        }
    }
