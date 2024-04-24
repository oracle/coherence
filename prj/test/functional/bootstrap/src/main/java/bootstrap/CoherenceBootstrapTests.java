/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package bootstrap;

import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.network.AvailablePortIterator;
import com.tangosol.coherence.config.CacheConfig;
import com.tangosol.coherence.config.CacheMapping;

import com.tangosol.internal.net.ConfigurableCacheFactorySession;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Coherence;
import com.tangosol.net.CoherenceConfiguration;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.InetAddressHelper;
import com.tangosol.net.Session;

import com.tangosol.net.SessionConfiguration;
import com.tangosol.util.Resources;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Jonathan Knight  2020.12.13
 */
@SuppressWarnings("resource")
class CoherenceBootstrapTests
    {
    @BeforeAll
    static void setup() throws Exception
        {
        s_availablePorts = LocalPlatform.get().getAvailablePorts();

        String sAddress = InetAddressHelper.getLocalHost().getHostAddress();

        System.setProperty("coherence.wka", sAddress);
        System.setProperty("coherence.localhost", sAddress);
        System.setProperty("test.unicast.address", sAddress);
        System.setProperty("test.unicast.port", "0");
        System.setProperty("coherence.ttl", "0");
        System.setProperty("coherence.cacheconfig", Resources.DEFAULT_RESOURCE_PACKAGE + "/coherence-cache-config.xml");
        }

    @BeforeEach
    void before(TestInfo info)
        {
        System.setProperty("test.multicast.port", String.valueOf(s_availablePorts.next()));

        System.err.println(">>>> Starting test " + info.getDisplayName());
        System.setProperty("coherence.cluster", "CoherenceBootstrapTests-" + m_nCluster.incrementAndGet());
        }

    @AfterEach
    void cleanup(TestInfo info)
        {
        Coherence.closeAll();
        CacheFactory.getCacheFactoryBuilder().releaseAll(null);
        CacheFactory.shutdown();
        System.err.println(">>>> Completed clean-up after test " + info.getDisplayName());
        }

    @Test
    void shouldHaveDefaultServerSession() throws Exception
        {
        Coherence coherence = Coherence.clusterMember();

        assertThat(coherence.getMode(), is(Coherence.Mode.ClusterMember));

        coherence.start().get(5, TimeUnit.MINUTES);

        Optional<Session> optional = coherence.getSessionIfPresent(Coherence.DEFAULT_NAME);
        assertThat(optional, is(notNullValue()));
        assertThat(optional.isPresent(), is(true));

        Session session = coherence.getSession();
        assertThat(session, is(notNullValue()));
        assertThat(session, is(instanceOf(ConfigurableCacheFactorySession.class)));

        assertThat(optional.get(), is(sameInstance(session)));

        ExtensibleConfigurableCacheFactory ccf = (ExtensibleConfigurableCacheFactory)
                ((ConfigurableCacheFactorySession) session).getConfigurableCacheFactory();

        CacheConfig cacheConfig = ccf.getCacheConfig();
        CacheMapping mapping = cacheConfig.getMappingRegistry().findCacheMapping("*");
        assertThat(mapping.getSchemeName(), is("near-direct"));
        }

    @Test
    void shouldHaveDefaultClientSession() throws Exception
        {
        Coherence coherence = Coherence.client();

        assertThat(coherence.getMode(), is(Coherence.Mode.Client));

        coherence.start().get(5, TimeUnit.MINUTES);

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
    void shouldHaveSystemSessionOnClusterMember() throws Exception
        {
        Coherence coherence = Coherence.clusterMember();

        assertThat(coherence.getMode(), is(Coherence.Mode.ClusterMember));

        coherence.start().get(5, TimeUnit.MINUTES);

        assertThat(coherence.hasSession(Coherence.SYSTEM_SESSION), is(true));

        Optional<Session> optional = coherence.getSessionIfPresent(Coherence.SYSTEM_SESSION);
        assertThat(optional, is(notNullValue()));
        assertThat(optional.isPresent(), is(true));

        Session session = coherence.getSession(Coherence.SYSTEM_SESSION);
        assertThat(optional.get(), is(sameInstance(session)));
        }

    @Test
    void shouldHaveSystemSessionOnClient() throws Exception
        {
        Coherence coherence = Coherence.client();

        assertThat(coherence.getMode(), is(Coherence.Mode.Client));

        coherence.start().get(5, TimeUnit.MINUTES);

        assertThat(coherence.hasSession(Coherence.SYSTEM_SESSION), is(true));

        Optional<Session> optional = coherence.getSessionIfPresent(Coherence.SYSTEM_SESSION);
        assertThat(optional, is(notNullValue()));
        assertThat(optional.isPresent(), is(true));

        Session session = coherence.getSession(Coherence.SYSTEM_SESSION);
        assertThat(optional.get(), is(sameInstance(session)));
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

    @Test
    void shouldAddExistingSessionIfAbsentAfterStart() throws Exception
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

        Session session = coherence.getSession("Two");

        assertThat(coherence.getSession("One"), is(notNullValue()));
        assertThat(coherence.getSession("One").isActive(), is(true));
        assertThat(coherence.getSession("Two"), is(notNullValue()));
        assertThat(coherence.getSession("Two").isActive(), is(true));

        coherence.addSessionIfAbsent(sessionTwo);
        assertThat(coherence.hasSession("One"), is(true));
        assertThat(coherence.hasSession("Two"), is(true));

        assertThat(coherence.getSession("Two"), is(sameInstance(session)));
        }

    @Test
    void shouldNotAddExistingSessionAfterStart() throws Exception
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

        assertThrows(IllegalStateException.class, () -> coherence.addSession(sessionTwo));
        }

    @Test
    public void shouldCloseAndRestartCoherence() throws Exception
        {
        CoherenceConfiguration configuration = CoherenceConfiguration.builder()
                .discoverSessions()
                .build();

        // Start Coherence
        Coherence coherence = Coherence.clusterMember(configuration).startAndWait();
        assertThat(coherence.isActive(), is(true));

        Session session = coherence.getSession();
        assertThat(session, is(notNullValue()));
        assertThat(session.isActive(), is(true));

        // close this instance
        coherence.close();
        assertThat(coherence.isClosed(), is(true));
        assertThat(coherence.isActive(), is(false));
        assertThat(session.isActive(), is(false));

        // restart Coherence
        coherence = Coherence.clusterMember(configuration).startAndWait();
        assertThat(coherence.isActive(), is(true));

        session = coherence.getSession();
        assertThat(session, is(notNullValue()));
        assertThat(session.isActive(), is(true));
        }

    private static final AtomicInteger m_nCluster = new AtomicInteger();

    private static AvailablePortIterator s_availablePorts;
    }
