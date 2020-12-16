/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package bootstrap;

import com.tangosol.coherence.config.CacheConfig;
import com.tangosol.coherence.config.CacheMapping;

import com.tangosol.internal.net.ConfigurableCacheFactorySession;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Coherence;
import com.tangosol.net.CoherenceConfiguration;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.Session;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author Jonathan Knight  2020.12.13
 */
class CoherenceBootstrapTests
    {
    @BeforeAll
    static void setup()
        {
        System.setProperty("coherence.ttl", "0");
        System.setProperty("coherence.cluster", "CoherenceBootstrapTests");
        }

    @AfterEach
    void cleanup()
        {
        Coherence.closeAll();
        CacheFactory.getCacheFactoryBuilder().releaseAll(null);
        CacheFactory.shutdown();
        }

    @Test
    void shouldHaveDefaultServerSession()
        {
        Coherence coherence = Coherence.clusterMember(CoherenceConfiguration.builder().build());

        assertThat(coherence.getMode(), is(Coherence.Mode.ClusterMember));

        coherence.start().join();

        Session session = coherence.getSession();
        assertThat(session, is(notNullValue()));
        assertThat(session, is(instanceOf(ConfigurableCacheFactorySession.class)));

        ExtensibleConfigurableCacheFactory ccf = (ExtensibleConfigurableCacheFactory)
                ((ConfigurableCacheFactorySession) session).getConfigurableCacheFactory();

        CacheConfig cacheConfig = ccf.getCacheConfig();
        CacheMapping mapping = cacheConfig.getMappingRegistry().findCacheMapping("*");
        assertThat(mapping.getSchemeName(), is("near-direct"));
        }

    @Test
    void shouldHaveDefaultClientSession()
        {
        Coherence coherence = Coherence.clientBuilder(CoherenceConfiguration.builder().build()).build();

        assertThat(coherence.getMode(), is(Coherence.Mode.Client));

        coherence.start().join();

        Session session = coherence.getSession();
        assertThat(session, is(notNullValue()));
        assertThat(session, is(instanceOf(ConfigurableCacheFactorySession.class)));

        ExtensibleConfigurableCacheFactory ccf = (ExtensibleConfigurableCacheFactory)
                ((ConfigurableCacheFactorySession) session).getConfigurableCacheFactory();

        CacheConfig cacheConfig = ccf.getCacheConfig();
        CacheMapping mapping = cacheConfig.getMappingRegistry().findCacheMapping("*");
        assertThat(mapping.getSchemeName(), is("near-remote"));
        }

    @Test
    void shouldHaveSystemSessionOnClusterMember()
        {
        Coherence coherence = Coherence.clusterMember(CoherenceConfiguration.builder().build());

        assertThat(coherence.getMode(), is(Coherence.Mode.ClusterMember));

        coherence.start().join();

        assertThat(coherence.hasSession(Coherence.SYSTEM_SESSION), is(true));
        }

    @Test
    void shouldNotHaveSystemSessionOnClient()
        {
        Coherence coherence = Coherence.client(CoherenceConfiguration.builder().build());

        assertThat(coherence.getMode(), is(Coherence.Mode.Client));

        coherence.start().join();

        assertThat(coherence.hasSession(Coherence.SYSTEM_SESSION), is(false));
        }
    }
