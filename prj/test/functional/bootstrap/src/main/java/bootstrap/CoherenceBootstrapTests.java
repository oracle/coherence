/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
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

import com.tangosol.net.SessionConfiguration;
import com.tangosol.util.Resources;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

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
        System.setProperty("coherence.cacheconfig", Resources.DEFAULT_RESOURCE_PACKAGE + "/coherence-cache-config.xml");
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
        Coherence coherence = Coherence.clusterMember();

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
        Coherence coherence = Coherence.client();

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
        Coherence coherence = Coherence.clusterMember();

        assertThat(coherence.getMode(), is(Coherence.Mode.ClusterMember));

        coherence.start().join();

        assertThat(coherence.hasSession(Coherence.SYSTEM_SESSION), is(true));
        }

    @Test
    void shouldNotHaveSystemSessionOnClient()
        {
        Coherence coherence = Coherence.client();

        assertThat(coherence.getMode(), is(Coherence.Mode.Client));

        coherence.start().join();

        assertThat(coherence.hasSession(Coherence.SYSTEM_SESSION), is(false));
        }

    @Test
    void shouldAddSessionBeforeStart() throws Exception
        {
        SessionConfiguration sessionOne = SessionConfiguration.builder()
                .named("One")
                .withScopeName("One")
                .build();

        SessionConfiguration sessionTwo = SessionConfiguration.builder()
                .named("Two")
                .withScopeName("Two")
                .build();

        CoherenceConfiguration configuration = CoherenceConfiguration.builder()
                .withSession(sessionOne)
                .withSession(sessionTwo)
                .build();

        Coherence coherence = Coherence.create(configuration);
        assertThat(coherence.hasSession("One"), is(true));
        assertThat(coherence.hasSession("Two"), is(true));

        SessionConfiguration sessionThree = SessionConfiguration.builder()
                .named("Three")
                .withScopeName("Three")
                .build();

        coherence.addSession(sessionThree);
        assertThat(coherence.hasSession("One"), is(true));
        assertThat(coherence.hasSession("Two"), is(true));
        assertThat(coherence.hasSession("Three"), is(true));

        coherence.start().get(5, TimeUnit.MINUTES);

        assertThat(coherence.getSession("One"), is(notNullValue()));
        assertThat(coherence.getSession("One").isActive(), is(true));
        assertThat(coherence.getSession("Two"), is(notNullValue()));
        assertThat(coherence.getSession("Two").isActive(), is(true));
        assertThat(coherence.getSession("Three"), is(notNullValue()));
        assertThat(coherence.getSession("Three").isActive(), is(true));
        }

    @Test
    void shouldAddSessionAfterStart() throws Exception
        {
        SessionConfiguration sessionOne = SessionConfiguration.builder()
                .named("One")
                .withScopeName("One")
                .build();

        SessionConfiguration sessionTwo = SessionConfiguration.builder()
                .named("Two")
                .withScopeName("Two")
                .build();

        CoherenceConfiguration configuration = CoherenceConfiguration.builder()
                .withSession(sessionOne)
                .withSession(sessionTwo)
                .build();

        Coherence coherence = Coherence.create(configuration);
        assertThat(coherence.hasSession("One"), is(true));
        assertThat(coherence.hasSession("Two"), is(true));

        coherence.start().get(5, TimeUnit.MINUTES);

        assertThat(coherence.getSession("One"), is(notNullValue()));
        assertThat(coherence.getSession("One").isActive(), is(true));
        assertThat(coherence.getSession("Two"), is(notNullValue()));
        assertThat(coherence.getSession("Two").isActive(), is(true));

        SessionConfiguration sessionThree = SessionConfiguration.builder()
                .named("Three")
                .withScopeName("Three")
                .build();

        coherence.addSession(sessionThree);
        assertThat(coherence.hasSession("One"), is(true));
        assertThat(coherence.hasSession("Two"), is(true));
        assertThat(coherence.hasSession("Three"), is(true));

        assertThat(coherence.getSession("Three"), is(notNullValue()));
        assertThat(coherence.getSession("Three").isActive(), is(true));
        }
    }
