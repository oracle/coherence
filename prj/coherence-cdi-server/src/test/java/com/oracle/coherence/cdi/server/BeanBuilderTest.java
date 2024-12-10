/*
 * Copyright (c) 2020, 2024 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.cdi.server;

import com.oracle.coherence.inject.Injectable;

import com.tangosol.coherence.config.ResolvableParameterList;
import com.tangosol.config.expression.Parameter;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.literal.NamedLiteral;
import javax.enterprise.inject.spi.CDI;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.expression.SystemPropertyParameterResolver;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link Injectable}.
 *
 * @author Aleks Seovic  2019.10.02
 */
@SuppressWarnings("unchecked")
class BeanBuilderTest
    {
    public static final SystemPropertyParameterResolver RESOLVER = new SystemPropertyParameterResolver();

    @Test
    void testRealizeSuccess()
        {
        Object           bean     = new BeanX();
        CDI<Object>      cdi      = mock(CDI.class);
        Instance<Object> instance = mock(Instance.class);

        when(cdi.select(any(Annotation.class))).thenReturn(instance);
        when(instance.isResolvable()).thenReturn(true);
        when(instance.get()).thenReturn(bean);

        BeanBuilder builder = new BeanBuilder(cdi, "beanX");
        Object      result  = builder.realize(new SystemPropertyParameterResolver(), null, null);
        assertThat(result, notNullValue());
        assertThat(result, is(sameInstance(bean)));
        }

    @Test
    void testRealizeMacroExpressionSuccess()
        {
        Object           bean     = new BeanX();
        CDI<Object>      cdi      = mock(CDI.class);
        Instance<Object> instance = mock(Instance.class);

        when(cdi.select(NamedLiteral.of("fooStore"))).thenReturn(instance);
        when(instance.isResolvable()).thenReturn(true);
        when(instance.get()).thenReturn(bean);

        ResolvableParameterList resolver = new ResolvableParameterList();

        resolver.add(new Parameter("cache-name", "foo"));

        BeanBuilder builder = new BeanBuilder(cdi, "{cache-name}Store");
        Object      result  = builder.realize(resolver, null, null);
        assertThat(result, notNullValue());
        assertThat(result, is(sameInstance(bean)));
        }

    @Test
    void testRealizeMissingBean()
        {
        CDI<Object>      cdi      = mock(CDI.class);
        Instance<Object> instance = mock(Instance.class);

        when(cdi.select(any(Annotation.class))).thenReturn(instance);
        when(instance.isResolvable()).thenReturn(false);

        assertThrows(ConfigurationException.class, () ->
            {
            BeanBuilder builder = new BeanBuilder(cdi,"beanY");
            Object      result  = builder.realize(RESOLVER, null, null);
            assertThat(result, notNullValue());
            assertThat(result, instanceOf(BeanX.class));
            });
        }

    @Test
    void testRealizesSuccess()
        {
        CDI<Object>     cdi      = mock(CDI.class);
        Instance<BeanX> instance = mock(Instance.class);

        when(cdi.select(eq(BeanX.class), any(Annotation.class))).thenReturn(instance);
        when(instance.isResolvable()).thenReturn(true);

        BeanBuilder builder   = new BeanBuilder(cdi,"beanX");
        boolean     fRealizes = builder.realizes(BeanX.class, RESOLVER, null);
        assertThat(fRealizes, is(true));
        }

    @Test
    void testRealizesFailureWrongClass()
        {
        CDI<Object>     cdi      = mock(CDI.class);
        Instance<BeanX> instance = mock(Instance.class);

        when(cdi.select(any(Class.class), any(Annotation.class))).thenReturn(instance);
        when(instance.isResolvable()).thenReturn(false);

        BeanBuilder builder   = new BeanBuilder(cdi,"beanX");
        boolean     fRealizes = builder.realizes(Injectable.class, RESOLVER, null);
        assertThat(fRealizes, is(false));
        }

    @Test
    void testRealizesFailureMissingBean()
        {
        CDI<Object>     cdi      = mock(CDI.class);
        Instance<BeanX> instance = mock(Instance.class);

        when(cdi.select(eq(BeanX.class), any(Annotation.class))).thenReturn(instance);
        when(instance.isResolvable()).thenReturn(false);

        BeanBuilder builder   = new BeanBuilder(cdi,"beanY");
        boolean     fRealizes = builder.realizes(BeanX.class, RESOLVER, null);
        assertThat(fRealizes, is(false));
        }

    static class BeanX
        {
        }
    }
