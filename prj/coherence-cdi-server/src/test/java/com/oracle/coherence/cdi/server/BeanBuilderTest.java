/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.cdi.server;

import com.oracle.coherence.cdi.Injectable;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.se.SeContainerInitializer;
import javax.inject.Named;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.expression.SystemPropertyParameterResolver;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link com.oracle.coherence.cdi.Injectable}.
 *
 * @author Aleks Seovic  2019.10.02
 */
class BeanBuilderTest
    {
    public static final SystemPropertyParameterResolver RESOLVER = new SystemPropertyParameterResolver();

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
    void testRealizeSuccess()
        {
        BeanBuilder builder = new BeanBuilder("beanX");
        Object bean = builder.realize(new SystemPropertyParameterResolver(), null, null);
        assertThat(bean, notNullValue());
        assertThat(bean, instanceOf(BeanX.class));
        }

    @Test
    void testRealizeMissingBean()
        {
        assertThrows(ConfigurationException.class, () ->
        {
        BeanBuilder builder = new BeanBuilder("beanY");
        Object bean = builder.realize(RESOLVER, null, null);
        assertThat(bean, notNullValue());
        assertThat(bean, instanceOf(BeanX.class));
        });
        }

    @Test
    void testRealizesSuccess()
        {
        BeanBuilder builder = new BeanBuilder("beanX");
        boolean fRealizes = builder.realizes(BeanX.class, RESOLVER, null);
        assertThat(fRealizes, is(true));
        }

    @Test
    void testRealizesFailureWrongClass()
        {
        BeanBuilder builder = new BeanBuilder("beanX");
        boolean fRealizes = builder.realizes(Injectable.class, RESOLVER, null);
        assertThat(fRealizes, is(false));
        }

    @Test
    void testRealizesFailureMissingBean()
        {
        BeanBuilder builder = new BeanBuilder("beanY");
        boolean fRealizes = builder.realizes(BeanX.class, RESOLVER, null);
        assertThat(fRealizes, is(false));
        }

    @Named("beanX")
    @ApplicationScoped
    static class BeanX
        {
        }
    }
