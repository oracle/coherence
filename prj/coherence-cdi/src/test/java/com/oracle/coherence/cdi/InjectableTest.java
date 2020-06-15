/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.cdi;

import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.se.SeContainerInitializer;
import javax.inject.Inject;

import com.oracle.coherence.common.base.Converter;

import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Unit tests for {@link com.oracle.coherence.cdi.Injectable}.
 *
 * @author Aleks Seovic  2019.10.02
 */
class InjectableTest
    {
    private static SeContainer container;

    @BeforeAll
    static void initContainer()
        {
        SeContainerInitializer containerInit = SeContainerInitializer.newInstance();
        container = containerInit.initialize();
        }

    @AfterAll
    static void shutdownContainer()
        {
        container.close();
        }

    @Test
    void testInjectable()
        {
        InjectableBean bean = new InjectableBean("aleks");
        assertThat(bean.converter, nullValue());
        assertThat(bean.postConstructCalled, is(false));

        Binary bin = ExternalizableHelper.toBinary(bean);
        bean = ExternalizableHelper.fromBinary(bin);
        assertThat(bean.converter, notNullValue());
        assertThat(bean.postConstructCalled, is(true));
        assertThat(bean.getConvertedText(), is("ALEKS"));
        }

    @Dependent
    public static class InjectableBean
            implements Injectable, Serializable
        {
        @Inject
        private Converter<String, String> converter;

        private String text;
        private boolean postConstructCalled;

        InjectableBean()
            {
            }

        InjectableBean(String text)
            {
            this.text = text;
            }

        @PostConstruct
        void postConstruct()
            {
            postConstructCalled = true;
            System.out.println("Deserialized: " + text);
            }

        String getConvertedText()
            {
            return converter.convert(text);
            }
        }

    @ApplicationScoped
    public static class ToUpperConverter
            implements Converter<String, String>
        {
        @Override
        public String convert(String s)
            {
            return s.toUpperCase();
            }
        }
    }
