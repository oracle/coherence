/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.oracle.coherence.common.util.MemorySize;
import com.tangosol.coherence.config.ParameterMacroExpressionParser;

import com.tangosol.coherence.config.scheme.AbstractJournalScheme;
import com.tangosol.coherence.config.scheme.FlashJournalScheme;
import com.tangosol.coherence.config.scheme.LocalScheme;
import com.tangosol.coherence.config.scheme.PagedTopicScheme;
import com.tangosol.coherence.config.scheme.RamJournalScheme;
import com.tangosol.coherence.config.scheme.Scheme;

import com.tangosol.coherence.config.unit.Seconds;

import com.tangosol.coherence.config.unit.Units;
import com.tangosol.coherence.config.xml.CacheConfigNamespaceHandler;

import com.tangosol.config.ConfigurationException;

import com.tangosol.config.expression.NullParameterResolver;
import com.tangosol.config.expression.Parameter;
import com.tangosol.config.expression.ParameterResolver;

import com.tangosol.config.xml.DocumentProcessor;

import com.tangosol.internal.net.topic.impl.paged.PagedTopic;

import com.tangosol.internal.net.topic.impl.paged.PagedTopicDependencies;
import com.tangosol.net.topic.BinaryElementCalculator;
import com.tangosol.net.topic.FixedElementCalculator;
import com.tangosol.run.xml.XmlDocumentReference;

import com.tangosol.util.SimpleResourceRegistry;

import com.oracle.coherence.testing.SystemPropertyResource;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;

/**
 * Unit Tests for {@link PagedTopicSchemeProcessor}s
 *
 * @author jf  2015.10.26
 */
public class PagedTopicSchemeProcessorTest
    {
    @Test
    public void testTopicSchemeProcessing()
            throws ConfigurationException
        {
        int    nPageSize = 33;
        String sXml      = "<topic-scheme>"
                            + "<scheme-name>common-pof-topic-scheme</scheme-name>"
                            + "<service-name>pof-topic-service</service-name>"
                            + "<storage>flashjournal</storage>"
                            + "<page-size system-property=\"coherence.page.size\">" + nPageSize + "</page-size>"
                            + "<expiry-delay>{expiry-delay 0}</expiry-delay>"
                            + "</topic-scheme>";

        PagedTopicScheme scheme = testPagedTopicSchemeProcessing(sXml);

        assertThat(scheme, is(notNullValue()));
        assertThat(scheme.getSchemeName(), is("common-pof-topic-scheme"));
        assertThat(scheme.getServiceName(), is("pof-topic-service"));
        assertThat(scheme.getStorageScheme().getClass().getSimpleName(), is(FlashJournalScheme.class.getSimpleName()));

        ParameterResolver      nullResolver = new NullParameterResolver();
        PagedTopicDependencies dependencies = scheme.createConfiguration(nullResolver, null);

        assertThat(dependencies, is(notNullValue()));
        assertThat(dependencies.getElementExpiryMillis(), is(0L));
        assertThat(dependencies.getPageCapacity(), is(nPageSize));
        }

    @Test
    public void testTopicSchemeProcessingSystemPropertyOverride()
        {
        long    nPageSize = 37;
        String  sXml      = "<topic-scheme>"
                            + "<scheme-name>common-pof-topic-scheme</scheme-name>"
                            + "<service-name>pof-topic-service</service-name>"
                            + "<storage system-property=\"test.topicscheme.storage\">flashjournal</storage>"
                            + "<page-size system-property=\"coherence.page.size\">" + 20 + "</page-size>"
                            + "<expiry-delay>{expiry-delay 0}</expiry-delay>"
                            + "</topic-scheme>";

        PagedTopicScheme scheme = null;

        // override by system property
        try (SystemPropertyResource p1 =
                     new SystemPropertyResource("coherence.page.size", Long.toString(nPageSize));
             SystemPropertyResource p2 =
                     new SystemPropertyResource("test.topicscheme.storage", "ramjournal");
             )
            {
            scheme = testPagedTopicSchemeProcessing(sXml);
            }

        assertThat(scheme, is(notNullValue()));
        assertThat(scheme.getSchemeName(), is("common-pof-topic-scheme"));
        assertThat(scheme.getServiceName(), is("pof-topic-service"));

        // override via topic-mapping parameters
        ParameterResolver resolver = new ParameterResolver()
            {
            @Override
            public Parameter resolve(String sName)
                {
                // equivalent of
                // <topic-mapping>
                //      <topic-name>...</topic-name>
                //      <scheme-name>common-pof-topic-scheme</scheme-name>
                //      <init-params>
                //        <init-param>
                //          <param-name>expiry-delay</param-name>
                //          <param-value>60</param-value>
                //        <init-param>
                //      </init-params>
                // </topic-mapping>
                if (sName.equals("expiry-delay"))
                    {
                    return new Parameter(sName, new Seconds(60));
                    }
                return null;
                }
            };

        Units actual = scheme.getPageSize(resolver);

        assertThat(actual, is(notNullValue()));
        assertThat(actual.isMemorySize(), is(false));
        assertThat(actual.getUnitCount(), is(nPageSize));
        assertThat(scheme.getExpiryDelay(resolver).get(), is(60L));
        assertThat(scheme.getStorageScheme().getClass().getSimpleName(), is(RamJournalScheme.class.getSimpleName()));
        }

    @Test
    @SuppressWarnings("rawtypes")
    public void testDefaultsTopicSchemeProcessing()
        {
        String sXml = "<topic-scheme>"
                        + "<scheme-name>common-pof-topic-scheme</scheme-name>"
                        + "<service-name>pof-topic-service</service-name>"
                        + "</topic-scheme>";

        PagedTopicScheme scheme = testPagedTopicSchemeProcessing(sXml);

        assertThat(scheme, is(notNullValue()));
        assertThat(scheme.getSchemeName(), is("common-pof-topic-scheme"));
        assertThat(scheme.getServiceName(), is("pof-topic-service"));

        ParameterResolver nullResolver = new NullParameterResolver();

        Units pageSize = scheme.getPageSize(nullResolver);
        int   cChannel = scheme.getChannelCount(nullResolver);

        assertThat(pageSize, is(notNullValue()));
        assertThat(pageSize.isMemorySize(), is(true));
        assertThat(pageSize.getUnitCount(), is(PagedTopic.DEFAULT_PAGE_CAPACITY_BYTES));

        assertThat(cChannel, is(PagedTopic.DEFAULT_CHANNEL_COUNT));
        assertThat(scheme.getExpiryDelay(nullResolver).get(), is(0L));
        assertThat(scheme.getStorageScheme().getClass().getSimpleName(), is(LocalScheme.class.getSimpleName()));

        Scheme innerscheme = scheme.getStorageScheme();
        if (innerscheme instanceof LocalScheme)
            {
            LocalScheme lscheme = (LocalScheme) innerscheme;
            assertThat(lscheme.getUnitCalculatorBuilder().getUnitCalculatorType(nullResolver), is("BINARY"));
            }
        else if (innerscheme instanceof AbstractJournalScheme)
            {
            AbstractJournalScheme<?> jscheme = (AbstractJournalScheme) innerscheme;
            assertThat(jscheme.getUnitCalculatorBuilder().getUnitCalculatorType(nullResolver), is("BINARY"));
            }
        }

    @Test
    public void testSetChannelCount()
        {
        String sXml = "<topic-scheme>"
                        + "<scheme-name>common-pof-topic-scheme</scheme-name>"
                        + "<service-name>pof-topic-service</service-name>"
                        + "<channel-count>19</channel-count>"
                        + "</topic-scheme>";

        PagedTopicScheme scheme = testPagedTopicSchemeProcessing(sXml);

        assertThat(scheme, is(notNullValue()));
        assertThat(scheme.getSchemeName(), is("common-pof-topic-scheme"));
        assertThat(scheme.getServiceName(), is("pof-topic-service"));

        ParameterResolver       nullResolver = new NullParameterResolver();
        PagedTopicDependencies dependencies = scheme.createConfiguration(nullResolver, null);

        assertThat(dependencies, is(notNullValue()));
        assertThat(dependencies.getConfiguredChannelCount(), is(19));
        }

    @Test
    public void testSetPageSizeAsUnits()
        {
        String sXml = "<topic-scheme>"
                        + "<scheme-name>common-pof-topic-scheme</scheme-name>"
                        + "<service-name>pof-topic-service</service-name>"
                        + "<page-size>1000</page-size>"
                        + "</topic-scheme>";

        PagedTopicScheme scheme = testPagedTopicSchemeProcessing(sXml);

        assertThat(scheme, is(notNullValue()));
        assertThat(scheme.getSchemeName(), is("common-pof-topic-scheme"));
        assertThat(scheme.getServiceName(), is("pof-topic-service"));

        ParameterResolver       nullResolver = new NullParameterResolver();
        PagedTopicDependencies dependencies = scheme.createConfiguration(nullResolver, null);

        assertThat(dependencies, is(notNullValue()));
        assertThat(dependencies.getPageCapacity(), is((int) 1000));
        assertThat(dependencies.getElementCalculator(), is(instanceOf(FixedElementCalculator.class)));
        }

    @Test
    public void testSetPageSizeAsUnitsWithFixedCalculator()
        {
        String sXml = "<topic-scheme>"
                        + "<scheme-name>common-pof-topic-scheme</scheme-name>"
                        + "<service-name>pof-topic-service</service-name>"
                        + "<page-size>1000</page-size>"
                        + "<element-calculator>FIXED</element-calculator>"
                        + "</topic-scheme>";

        PagedTopicScheme scheme = testPagedTopicSchemeProcessing(sXml);

        assertThat(scheme, is(notNullValue()));
        assertThat(scheme.getSchemeName(), is("common-pof-topic-scheme"));
        assertThat(scheme.getServiceName(), is("pof-topic-service"));

        ParameterResolver       nullResolver = new NullParameterResolver();
        PagedTopicDependencies dependencies = scheme.createConfiguration(nullResolver, null);

        assertThat(dependencies, is(notNullValue()));
        assertThat(dependencies.getPageCapacity(), is((int) 1000));
        assertThat(dependencies.getElementCalculator(), is(instanceOf(FixedElementCalculator.class)));
        }

    @Test
    public void testSetPageSizeAsMemorySize()
        {
        String sXml = "<topic-scheme>"
                        + "<scheme-name>common-pof-topic-scheme</scheme-name>"
                        + "<service-name>pof-topic-service</service-name>"
                        + "<page-size>5M</page-size>"
                        + "</topic-scheme>";

        PagedTopicScheme scheme = testPagedTopicSchemeProcessing(sXml);

        assertThat(scheme, is(notNullValue()));
        assertThat(scheme.getSchemeName(), is("common-pof-topic-scheme"));
        assertThat(scheme.getServiceName(), is("pof-topic-service"));

        ParameterResolver       nullResolver = new NullParameterResolver();
        PagedTopicDependencies dependencies = scheme.createConfiguration(nullResolver, null);

        assertThat(dependencies, is(notNullValue()));
        assertThat(dependencies.getPageCapacity(), is((int) new MemorySize("5M").getByteCount()));
        assertThat(dependencies.getElementCalculator(), is(instanceOf(BinaryElementCalculator.class)));
        }


    @Test
    public void shouldNotAllowFixedCalculatorWithMemoryPageSize()
        {
        String sXml = "<topic-scheme>"
                        + "<scheme-name>common-pof-topic-scheme</scheme-name>"
                        + "<service-name>pof-topic-service</service-name>"
                        + "<page-size>5M</page-size>"
                        + "<element-calculator>FIXED</element-calculator>"
                        + "</topic-scheme>";

        PagedTopicScheme scheme = testPagedTopicSchemeProcessing(sXml);

        assertThat(scheme, is(notNullValue()));
        assertThat(scheme.getSchemeName(), is("common-pof-topic-scheme"));
        assertThat(scheme.getServiceName(), is("pof-topic-service"));

        ParameterResolver       nullResolver = new NullParameterResolver();

        assertThrows(ConfigurationException.class, () ->  scheme.createConfiguration(nullResolver, null));
        }

    // ----- helper ----------------------------------------------------------------------------------------------------

    private PagedTopicScheme testPagedTopicSchemeProcessing(String sXml)
            throws ConfigurationException
        {
        DocumentProcessor.DefaultDependencies dep = new DocumentProcessor.DefaultDependencies();

        dep.setDefaultNamespaceHandler(new CacheConfigNamespaceHandler());
        dep.setExpressionParser(ParameterMacroExpressionParser.INSTANCE);
        dep.setResourceRegistry(new SimpleResourceRegistry());

        DocumentProcessor processor = new DocumentProcessor(dep);

        return processor.process(new XmlDocumentReference(sXml));
        }
    }
