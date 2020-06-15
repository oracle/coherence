/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.cdi;

import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.se.SeContainerInitializer;

import com.tangosol.coherence.config.ParameterMacroExpressionParser;
import com.tangosol.config.ConfigurationException;
import com.tangosol.config.expression.SystemPropertyParameterResolver;
import com.tangosol.config.xml.DefaultProcessingContext;
import com.tangosol.config.xml.DocumentProcessor;
import com.tangosol.config.xml.ElementProcessor;
import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link Injectable}.
 *
 * @author Aleks Seovic  2019.10.02
 */
class CdiNamespaceHandlerTest
    {
    private static SeContainer container;

    private static ProcessingContext context;

    @BeforeAll
    static void initContainer()
        {
        SeContainerInitializer containerInit = SeContainerInitializer.newInstance();
        container = containerInit.initialize();
        context = new DefaultProcessingContext(
                new DocumentProcessor.DefaultDependencies()
                        .setExpressionParser(new ParameterMacroExpressionParser()));
        }

    @AfterAll
    static void shutdownContainer()
        {
        container.close();
        }

    @Test
    void testSuccess()
        {
        Object bean = realize("<cdi:bean>beanX</cdi:bean>");
        assertThat(bean, notNullValue());
        assertThat(bean, instanceOf(BeanBuilderTest.BeanX.class));
        }

    @Test
    void testFailureMissingBean()
        {
        assertThrows(ConfigurationException.class, () ->
                realize("<cdi:bean>beanY</cdi:bean>")
        );
        }

    @Test
    void testFailureUndefined()
        {
        assertThrows(ConfigurationException.class, () ->
                realize("<cdi:bean/>")
        );
        }

    private Object realize(String sXml)
        {
        XmlElement xml = XmlHelper.loadXml(sXml).getRoot();
        CdiNamespaceHandler handler = new CdiNamespaceHandler();
        ElementProcessor<?> processor = handler.getElementProcessor(xml);
        assertThat(processor, instanceOf(BeanProcessor.class));

        BeanBuilder builder = ((BeanProcessor) processor).process(context, xml);
        return builder.realize(new SystemPropertyParameterResolver(), null, null);
        }
    }
