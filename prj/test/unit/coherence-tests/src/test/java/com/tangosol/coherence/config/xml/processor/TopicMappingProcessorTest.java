/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.ParameterMacroExpressionParser;
import com.tangosol.coherence.config.TopicMapping;
import com.tangosol.coherence.config.builder.SubscriberGroupBuilder;
import com.tangosol.coherence.config.xml.CacheConfigNamespaceHandler;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.expression.NullParameterResolver;
import com.tangosol.config.expression.Parameter;
import com.tangosol.config.expression.ParameterResolver;
import com.tangosol.config.expression.ScopedParameterResolver;
import com.tangosol.config.xml.DocumentProcessor;
import com.tangosol.config.xml.DocumentProcessor.DefaultDependencies;

import com.tangosol.run.xml.XmlDocumentReference;

import com.tangosol.util.SimpleResourceRegistry;

import java.net.URISyntaxException;

import java.util.Collection;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Unit Tests for {@link TopicMappingProcessor}s
 *
 * @author jk  2011.05.22
 * @author jf  2015.10.26
 */
public class TopicMappingProcessorTest
    {

    /**
     * Ensure that we can create a {@link TopicMapping} from a &lt;topic-mapping&gt;
     *
     * @throws URISyntaxException if there is a problem with the URI.
     * @throws ConfigurationException if there is a problem with the parsing.
     */
    @Test
    public void testTopicMappingProcessing()
            throws URISyntaxException, ConfigurationException
        {
        String sXml = "<topic-mapping>"
                + "<topic-name>dist-*</topic-name>"
                + "<scheme-name>Distributed</scheme-name>"
                + "<value-type>String</value-type>"
                + "<init-params>"
                + "<init-param><param-name>size</param-name><param-value>100</param-value></init-param>"
                + "<init-param><param-name>autostart</param-name><param-value>true</param-value></init-param>"
                + "<init-param><param-name>name</param-name><param-value>Bob</param-value></init-param>"
                + "<init-param><param-name>retain-values</param-name><param-value>true</param-value></init-param>"
                + "<init-param><param-name>value-expiry</param-name><param-value>2</param-value></init-param>"
                + "</init-params>"
                + "</topic-mapping>";

        testTopicMappingProcessing(sXml, String.class);
        }

    /**
     * Ensure that we can create a {@link TopicMapping} from a &lt;topic-mapping&gt;
     *
     * @throws URISyntaxException if there is a problem with the URI.
     * @throws ConfigurationException if there is a problem with the parsing.
     */
    @Test
    public void testTopicMappingGroupSubscribersProcessing()
            throws URISyntaxException, ConfigurationException
        {
        String sXml = "<topic-mapping>"
                + "<topic-name>dist-*</topic-name>"
                + "  <scheme-name>Distributed</scheme-name>"
                + "  <value-type>String</value-type>"
                + "  <subscriber-groups>"
                + "    <subscriber-group>"
                + "      <name>{topic-name}durableGroup1</name>"
                + "    </subscriber-group>"
                + "    <subscriber-group>"
                + "       <name>durableGroup2</name>"
                + "    </subscriber-group>"
                + "  </subscriber-groups>"
                + "</topic-mapping>";

        testTopicMappingSubscriberGroupProcessing(sXml, String.class);
        }


    //----- helper ----------------------------------------------------------------------------------------------------

    private void testTopicMappingProcessing(String sXml, Class elementClass)
            throws URISyntaxException, ConfigurationException
        {
        DefaultDependencies dep = new DefaultDependencies();

        dep.setDefaultNamespaceHandler(new CacheConfigNamespaceHandler());
        dep.setExpressionParser(ParameterMacroExpressionParser.INSTANCE);
        dep.setResourceRegistry(new SimpleResourceRegistry());

        DocumentProcessor processor = new DocumentProcessor(dep);

        TopicMapping mapping   = processor.process(new XmlDocumentReference(sXml));

        assertThat(mapping, is(notNullValue()));
        assertThat(mapping.getNamePattern(), is("dist-*"));
        assertThat(mapping.getSchemeName(), is("Distributed"));
        assertThat(mapping.isForName("dist-test"), is(true));
        assertThat(mapping.isForName("dist"), is(false));
        assertThat(mapping.getValueClassName(), is(elementClass.getCanonicalName()));

        ParameterResolver resolver     = mapping.getParameterResolver();
        ParameterResolver nullResolver = new NullParameterResolver();

        assertThat(resolver.resolve("size").evaluate(nullResolver).as(Integer.class), is(100));
        assertThat(resolver.resolve("autostart").evaluate(nullResolver).as(Boolean.class), is(true));
        assertThat(resolver.resolve("name").evaluate(nullResolver).as(String.class), is("Bob"));
        assertThat(resolver.resolve("retain-values").evaluate(nullResolver).as(Boolean.class), is(true));
        assertThat(resolver.resolve("value-expiry").evaluate(nullResolver).as(Integer.class), is(2));
        }

    private void testTopicMappingSubscriberGroupProcessing(String sXml, Class elementClass)
            throws URISyntaxException, ConfigurationException
        {
        DefaultDependencies dep = new DefaultDependencies();

        dep.setDefaultNamespaceHandler(new CacheConfigNamespaceHandler());
        dep.setExpressionParser(ParameterMacroExpressionParser.INSTANCE);
        dep.setResourceRegistry(new SimpleResourceRegistry());

        DocumentProcessor processor = new DocumentProcessor(dep);

        TopicMapping      mapping   = processor.process(new XmlDocumentReference(sXml));

        assertThat(mapping, is(notNullValue()));
        assertThat(mapping.getNamePattern(), is("dist-*"));
        assertThat(mapping.getSchemeName(), is("Distributed"));
        assertThat(mapping.isForName("dist-test"), is(true));
        assertThat(mapping.isForName("dist"), is(false));
        assertThat(mapping.getValueClassName(), is(elementClass.getCanonicalName()));

        ScopedParameterResolver resolver = new ScopedParameterResolver(mapping.getParameterResolver());
        String topicName = "dist-topic";
        resolver.add(new Parameter("topic-name", topicName));


        Collection<SubscriberGroupBuilder> builders = mapping.getSubscriberGroupBuilders();
        assertThat("expected 2 SubscriberGroups", builders.size(), is(2));
        int nProcessed = 0;
        for (SubscriberGroupBuilder builder : builders)
            {
            if (builder.getSubscriberGroupName(resolver).contains(topicName + "durableGroup1"))
                {
                nProcessed++;
                }
            else if (builder.getSubscriberGroupName(resolver).contains("durableGroup2"))
                {
                boolean DEFAULT = false;
                nProcessed++;
                }
            else
                {
                fail("unexpected subscriber group builder" + builder);
                }
            }
        assertThat(nProcessed, is(2));
        }
    }
