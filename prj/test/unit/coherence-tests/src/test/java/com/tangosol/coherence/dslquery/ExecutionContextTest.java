/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.dslquery;

import com.tangosol.internal.net.ConfigurableCacheFactorySession;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.Session;

import com.tangosol.util.Base;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

import static org.hamcrest.MatcherAssert.assertThat;

import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link ExecutionContext}.
 */
@SuppressWarnings("deprecation")
public class ExecutionContextTest
    {

    @Test
    public void shouldReturnDefaultConfigurableCacheFactory()
        {
        ExecutionContext ctx = new ExecutionContext();
        assertThat(ctx.getCacheFactory(), is(CacheFactory.getConfigurableCacheFactory()));
        assertThat(ctx.getSession(), notNullValue());
        }

    @Test
    public void shouldReturnDefaultConfigurableCacheFactoryForSession()
        {
        ExecutionContext ctx = new ExecutionContext();
        assertThat(ctx.getSession(), notNullValue());
        assertThat(ctx.getCacheFactory(), is(CacheFactory.getConfigurableCacheFactory()));
        }

    @Test
    public void shouldRespectCustomCacheFactory()
        {
        ConfigurableCacheFactory mock = mock(ConfigurableCacheFactory.class);
        ExecutionContext ctx = new ExecutionContext();
        ctx.setCacheFactory(mock);
        assertThat(ctx.getCacheFactory(), is(mock));
        assertThat(ctx.getSession(), notNullValue());
        }

    @Test
    public void shouldPopulateCCFWhenSessionIsAppropriate()
        {
        ConfigurableCacheFactory mock = mock(ConfigurableCacheFactory.class);
        ConfigurableCacheFactorySession session =
                new ConfigurableCacheFactorySession(mock, Base.getContextClassLoader());
        ExecutionContext ctx = new ExecutionContext();
        ctx.setSession(session);
        assertThat(ctx.getSession(), is(session));
        assertThat(ctx.getCacheFactory(), is(mock));
        }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowDefaultCCFIfCantBeDetermined()
        {
        Session mock = mock(Session.class);
        ExecutionContext ctx = new ExecutionContext();
        ctx.setSession(mock);
        assertThat(ctx.getSession(), is(mock));
        ctx.getCacheFactory();
        }
    }
