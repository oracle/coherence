/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package session;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheFactoryBuilder;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.SessionProvider;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * Functional Tests for {@link SessionProvider}s.
 *
 * @author bo 2015.07.27
 */
public class SessionProviderTests
        extends AbstractFunctionalTest
    {
    /**
     * Ensure that we can automatically detect the {@link SessionProvider}
     */
    @Test
    public void shouldAutoDetectSessionProvider()
        {
        SessionProvider provider = SessionProvider.get();

        assertThat(provider, is(not(nullValue())));
        }

    /**
     * Ensure that the {@link CacheFactoryBuilder} will return different
     * {@link ConfigurableCacheFactory} instances for the same configuration but
     * different {@link ClassLoader}s.
     *
     * @throws Exception
     */
    @Test
    public void shouldReturnDifferentCacheFactories()
            throws Exception
        {
        // a URL for the current directory
        URL url = new URL("file://.");

        // construct two classloaders for the same URL (and parent)
        ClassLoader loader1 = new URLClassLoader(new URL[]{url},
                                                 Thread.currentThread().getContextClassLoader());

        ClassLoader loader2 = new URLClassLoader(new URL[]{url},
                                                 Thread.currentThread().getContextClassLoader());

        // grab the CacheFactoryBuilder
        CacheFactoryBuilder builder = CacheFactory.getCacheFactoryBuilder();

        // request two ConfigurableCacheFactories for the same configuration but
        // different ClassLoaders
        ConfigurableCacheFactory ccf1 = builder.getConfigurableCacheFactory(
                loader1);

        ConfigurableCacheFactory ccf2 = builder.getConfigurableCacheFactory(
                loader2);

        // assert that they are different
        assertThat(ccf1, is(not(ccf2)));
        }

    /**
     * Ensure that the {@link ConfigurableCacheFactory} will return different
     * {@link com.tangosol.net.NamedCache} instances for the same configuration but
     * different {@link ClassLoader}s.
     *
     * @throws Exception
     */
    @Test
    public void shouldReturnDifferentCacheForDifferentClassLoadersWithSameConfigurableCacheFactory()
            throws Exception
        {
        // a URL for the current directory
        URL url = new URL("file://.");

        // construct two classloaders for the same URL (and parent)
        ClassLoader loader1 = new URLClassLoader(new URL[]{url},
                                                 Thread.currentThread().getContextClassLoader());

        ClassLoader loader2 = new URLClassLoader(new URL[]{url},
                                                 Thread.currentThread().getContextClassLoader());

        // grab the default ConfigurableCacheFactory
        ConfigurableCacheFactory ccf = CacheFactory.getConfigurableCacheFactory();

        // request the same named cache from the factory with different ClassLoaders
        NamedCache namedCache1 = ccf.ensureCache("foo", loader1);

        NamedCache namedCache2 = ccf.ensureCache("foo", loader2);

        // assert that they are the different
        assertThat(namedCache1, is(not(namedCache2)));
        }


    /**
     * Ensure that the {@link CacheFactoryBuilder} will return different
     * {@link ConfigurableCacheFactory} instances for the same configuration but
     * different {@link ClassLoader}s, and that {@link NamedCache} instances,
     * for the same named cache ensured against the said {@link ConfigurableCacheFactory}s
     * will be different.
     *
     * @throws Exception
     */
    @Test
    public void shouldReturnDifferentCachesForDifferentCacheFactories()
            throws Exception
        {
        // a URL for the current directory
        URL url = new URL("file://.");

        // construct two classloaders for the same URL (and parent)
        ClassLoader loader1 = new URLClassLoader(new URL[]{url},
                                                 Thread.currentThread().getContextClassLoader());

        ClassLoader loader2 = new URLClassLoader(new URL[]{url},
                                                 Thread.currentThread().getContextClassLoader());

        // grab the CacheFactoryBuilder
        CacheFactoryBuilder builder = CacheFactory.getCacheFactoryBuilder();

        // request two ConfigurableCacheFactories for the same configuration but
        // different ClassLoaders
        ConfigurableCacheFactory ccf1 = builder.getConfigurableCacheFactory(
                loader1);

        ConfigurableCacheFactory ccf2 = builder.getConfigurableCacheFactory(
                loader2);

        // assert that they are different
        assertThat(ccf1, is(not(ccf2)));


        // request the same named cache from both factories
        NamedCache namedCache1 = ccf1.ensureCache("foo", loader1);

        NamedCache namedCache2 = ccf2.ensureCache("foo", loader2);

        // assert that they are different
        assertThat(namedCache1, is(not(namedCache2)));
        }
    }
