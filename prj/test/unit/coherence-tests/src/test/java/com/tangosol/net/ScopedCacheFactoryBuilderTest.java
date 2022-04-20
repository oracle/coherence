/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import com.oracle.coherence.common.base.Classes;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Jonathan Knight  2020.12.01
 */
public class ScopedCacheFactoryBuilderTest
    {
    @Test
    public void shouldUseScopeInConfigUsingDefaultResolver()
        {
        String                    sCfg     = "scoped-cfb-test.xml";
        ClassLoader               loader   = Classes.getContextClassLoader();
        ScopedCacheFactoryBuilder builder  = new ScopedCacheFactoryBuilder();
        ConfigurableCacheFactory  ccf      = builder.getConfigurableCacheFactory(sCfg, loader);

        assertThat(ccf.getScopeName(), is("foo"));
        }

    @Test
    public void shouldUseScopeInConfigOverScopeFromResolver()
        {
        String                    sCfg     = "scoped-cfb-test.xml";
        ClassLoader               loader   = Classes.getContextClassLoader();
        ScopeResolver             resolver = new Resolver("bar", true);
        ScopedCacheFactoryBuilder builder  = new ScopedCacheFactoryBuilder(resolver);
        ConfigurableCacheFactory  ccf      = builder.getConfigurableCacheFactory(sCfg, loader);

        assertThat(ccf.getScopeName(), is("foo"));
        }

    @Test
    public void shouldUseScopeFromResolver()
        {
        String                    sCfg     = "scoped-cfb-test.xml";
        ClassLoader               loader   = Classes.getContextClassLoader();
        ScopeResolver             resolver = new Resolver("bar", false);
        ScopedCacheFactoryBuilder builder  = new ScopedCacheFactoryBuilder(resolver);
        ConfigurableCacheFactory  ccf      = builder.getConfigurableCacheFactory(sCfg, loader);

        assertThat(ccf.getScopeName(), is("bar"));
        }

    @Test
    public void shouldUseScopeFromConfigIfResolverReturnsDefaultScope()
        {
        String                    sCfg     = "scoped-cfb-test.xml";
        ClassLoader               loader   = Classes.getContextClassLoader();
        ScopeResolver             resolver = new Resolver(Coherence.DEFAULT_SCOPE, false);
        ScopedCacheFactoryBuilder builder  = new ScopedCacheFactoryBuilder(resolver);
        ConfigurableCacheFactory  ccf      = builder.getConfigurableCacheFactory(sCfg, loader);

        assertThat(ccf.getScopeName(), is("foo"));
        }


    static class Resolver
            implements ScopeResolver
        {
        public Resolver(String sScope, boolean fUseConfig)
            {
            f_sScope = sScope;
            f_fUseConfig = fUseConfig;
            }

        @Override
        public String resolveScopeName(String sConfigURI, ClassLoader loader, String sScopeName)
            {
            return f_sScope;
            }

        @Override
        public boolean useScopeInConfig()
            {
            return f_fUseConfig;
            }

        private final String f_sScope;

        private final boolean f_fUseConfig;
        }
    }
