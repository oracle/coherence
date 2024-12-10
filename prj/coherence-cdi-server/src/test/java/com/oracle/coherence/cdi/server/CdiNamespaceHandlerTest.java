/*
 * Copyright (c) 2020, 2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.cdi.server;

import com.tangosol.coherence.config.ParameterMacroExpressionParser;
import com.tangosol.config.ConfigurationException;
import com.tangosol.config.expression.SystemPropertyParameterResolver;
import com.tangosol.config.xml.DefaultProcessingContext;
import com.tangosol.config.xml.DocumentProcessor;
import com.tangosol.config.xml.ElementProcessor;

import com.tangosol.config.xml.ProcessingContext;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;

import java.lang.annotation.Annotation;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CdiNamespaceHandler}.
 *
 * @author Aleks Seovic  2019.10.02
 */
class CdiNamespaceHandlerTest
    {
    private static ProcessingContext context;

    @BeforeAll
    static void initContainer()
        {
        context = new DefaultProcessingContext(
                new DocumentProcessor.DefaultDependencies()
                        .setExpressionParser(new ParameterMacroExpressionParser()));
        }

    @Test
    void testSuccess()
        {
        Object bean   = new Object();
        Object result = realize("<cdi:bean>beanX</cdi:bean>", "beanX", bean);
        assertThat(result, notNullValue());
        assertThat(result, is(sameInstance(bean)));
        }

    @Test
    void testFailureMissingBean()
        {
        assertThrows(ConfigurationException.class, () ->
                realize("<cdi:bean>beanY</cdi:bean>", "foo", null)
        );
        }

    @Test
    void testFailureUndefined()
        {
        assertThrows(ConfigurationException.class, () ->
                realize("<cdi:bean/>", "foo", null)
        );
        }

    @SuppressWarnings("unchecked")
    private Object realize(String sXml, String sName, Object bean)
        {
        XmlElement          xml       = XmlHelper.loadXml(sXml).getRoot();
        CdiNamespaceHandler handler   = new CdiNamespaceHandler();
        ElementProcessor<?> processor = handler.getElementProcessor(xml);
        CDI<Object>         cdi       = mock(CDI.class);
        Instance<Object>    instance  = mock(Instance.class);

        when(cdi.select(any(Annotation.class))).thenReturn(instance);
        when(instance.isResolvable()).thenReturn(bean != null);
        when(instance.get()).thenReturn(bean);

        assertThat(processor, instanceOf(BeanProcessor.class));
        ((BeanProcessor) processor).setCDI(cdi);

        BeanBuilder builder = ((BeanProcessor) processor).process(context, xml);
        return builder.realize(new SystemPropertyParameterResolver(), null, null);
        }
    }
