/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.processor;

import com.tangosol.coherence.config.ParameterMacroExpressionParser;
import com.tangosol.coherence.config.TopicMapping;
import com.tangosol.coherence.config.scheme.AbstractJournalScheme;
import com.tangosol.coherence.config.scheme.FlashJournalScheme;
import com.tangosol.coherence.config.scheme.LocalScheme;
import com.tangosol.coherence.config.scheme.PagedTopicScheme;
import com.tangosol.coherence.config.scheme.RamJournalScheme;
import com.tangosol.coherence.config.scheme.Scheme;
import com.tangosol.coherence.config.unit.Seconds;
import com.tangosol.coherence.config.xml.CacheConfigNamespaceHandler;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.expression.NullParameterResolver;
import com.tangosol.config.expression.Parameter;
import com.tangosol.config.expression.ParameterResolver;
import com.tangosol.config.xml.DocumentProcessor;

import com.tangosol.internal.net.topic.impl.paged.Configuration;

import com.tangosol.run.xml.XmlDocumentReference;

import com.tangosol.util.SimpleResourceRegistry;

import common.SystemPropertyResource;

import org.junit.Test;

import java.net.URISyntaxException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Unit Tests for {@link PagedTopicSchemeProcessor}s
 *
 * @author jf  2015.10.26
 */
public class PagedTopicSchemeProcessorTest
    {
    /**
     * Ensure that we can create a {@link TopicMapping} from a <topic-scheme>
     *
     * @throws URISyntaxException if there is a problem with the URI.
     * @throws ConfigurationException if there is a problem with the parsing.
     */
    @Test
    public void testTopicSchemeProcessing()
            throws URISyntaxException, ConfigurationException
        {
        int    nPageSize = 33;

        String sXml      = "<topic-scheme>"
                            + "<scheme-name>common-pof-topic-scheme</scheme-name>"
                            + "<service-name>pof-topic-service</service-name>"
                            + "<page-size system-property=\"coherence.page.size\">" + nPageSize + "</page-size>"
                            + "<expiry-delay>{expiry-delay 0}</expiry-delay>"
                            + "</topic-scheme>";

        PagedTopicScheme scheme = testPagedTopicSchemeProcessing(sXml);

        assertThat(scheme, is(notNullValue()));
        assertThat(scheme.getSchemeName(), is("common-pof-topic-scheme"));
        assertThat(scheme.getServiceName(), is("pof-topic-service"));

        ParameterResolver nullResolver = new NullParameterResolver();

        int actual = scheme.getPageSize(nullResolver);

        assertThat("page size of " + nPageSize, actual, is(nPageSize));
        assertThat(scheme.getExpiryDelay(nullResolver).get(), is(0L));
        assertThat(scheme.getStorageScheme().getClass().getSimpleName(), is(LocalScheme.class.getSimpleName()));
        }

    @Test
    public void testTopicSchemeProcessingSystemPropertyOverride() throws URISyntaxException
        {
        int     nPageSize = 37;
        String  sXml      = "<topic-scheme>"
                            + "<scheme-name>common-pof-topic-scheme</scheme-name>"
                            + "<service-name>pof-topic-service</service-name>"
                            + "<page-size system-property=\"coherence.page.size\">" + 20 + "</page-size>"
                            + "<expiry-delay>{expiry-delay 0}</expiry-delay>"
                            + "</topic-scheme>";

        PagedTopicScheme scheme = null;

        // override by system property
        try (SystemPropertyResource p1 =
                     new SystemPropertyResource("coherence.page.size", Integer.toString(nPageSize));
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

        int actual = scheme.getPageSize(resolver);

        assertThat("page size of " + nPageSize, actual, is(nPageSize));
        assertThat(scheme.getExpiryDelay(resolver).get(), is(60L));
        }


    @Test
    public void testDefaultsTopicSchemeProcessing() throws URISyntaxException
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

        int actual = scheme.getPageSize(nullResolver);

        assertThat("page size of " + Configuration.DEFAULT_PAGE_CAPACITY_BYTES,
                actual, is(Configuration.DEFAULT_PAGE_CAPACITY_BYTES));
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
            AbstractJournalScheme jscheme = (AbstractJournalScheme) innerscheme;
            assertThat(jscheme.getUnitCalculatorBuilder().getUnitCalculatorType(nullResolver), is("BINARY"));
            }
        }

    // ----- helper ----------------------------------------------------------------------------------------------------

    private PagedTopicScheme testPagedTopicSchemeProcessing(String sXml)
            throws URISyntaxException, ConfigurationException
        {
        DocumentProcessor.DefaultDependencies dep = new DocumentProcessor.DefaultDependencies();

        dep.setDefaultNamespaceHandler(new CacheConfigNamespaceHandler());
        dep.setExpressionParser(ParameterMacroExpressionParser.INSTANCE);
        dep.setResourceRegistry(new SimpleResourceRegistry());

        DocumentProcessor            processor = new DocumentProcessor(dep);

        return processor.process(new XmlDocumentReference(sXml));
        }
    }
