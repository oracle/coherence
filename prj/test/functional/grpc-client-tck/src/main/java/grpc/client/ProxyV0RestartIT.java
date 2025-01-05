/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package grpc.client;


import com.oracle.bedrock.options.Timeout;
import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.network.AvailablePortIterator;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.coherence.grpc.services.proxy.v1.ProxyServiceGrpc;
import com.tangosol.coherence.component.util.SafeService;
import com.tangosol.net.Coherence;
import com.tangosol.net.CoherenceConfiguration;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Service;
import com.tangosol.net.Session;
import com.tangosol.net.SessionConfiguration;
import com.tangosol.net.grpc.GrpcChannelDependencies;
import com.tangosol.net.grpc.GrpcDependencies;
import com.tangosol.net.management.MBeanServerProxy;
import com.tangosol.net.management.Registry;
import com.tangosol.util.Filters;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;
import com.tangosol.util.ServiceEvent;
import com.tangosol.util.ServiceListener;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * This test is for COH-31178 for the Version Zero API
 */
@SuppressWarnings("resource")
public class ProxyV0RestartIT
    {
    @BeforeAll
    static void setup(TestInfo info) throws Exception
        {
        String clusterName = info.getTestClass().map(Class::getSimpleName).orElse("ProxyRestartIT");
        System.setProperty("coherence.grpc.heartbeat.interval", "1000");
        System.setProperty("coherence.grpc.heartbeat.ack", "true");

        System.setProperty("coherence.cluster", clusterName);
        System.setProperty("coherence.profile", "thin");
        System.setProperty("coherence.ttl", "0");
        System.setProperty("coherence.wka", "127.0.0.1");
        System.setProperty("coherence.localhost", "127.0.0.1");
        System.setProperty("coherence.override", "coherence-json-override.xml");
        System.setProperty("coherence.pof.config", "test-pof-config.xml");
        System.setProperty("coherence.cacheconfig", "coherence-cache-config.xml");
        System.setProperty("coherence.extend.port", String.valueOf(PORTS.next()));

        // Force only the version zero API to be enabled on the proxy
        System.setProperty(ProxyServiceGrpc.class.getName() + ".enabled", "false");

        String sGrpcPort = String.valueOf(PORTS.next());
        System.setProperty(GrpcDependencies.PROP_PORT, sGrpcPort);
        System.setProperty(GrpcChannelDependencies.PROP_DEFAULT_CHANNEL_PORT, sGrpcPort);
        System.setProperty("coherence.grpc.address", "127.0.0.1");
        System.setProperty("coherence.grpc.port", sGrpcPort);

        CoherenceConfiguration.Builder cfgBuilder = CoherenceConfiguration.builder()
                .withSession(SessionConfiguration.defaultSession());

        Set<String> setSessionName = new HashSet<>();
        Set<String> setSerializer  = AbstractGrpcClientIT.serializers().map(a -> String.valueOf(a.get()[0]))
                .collect(Collectors.toSet());

        for (String sSerializer : setSerializer)
            {
            String sName = sessionNameFromSerializerName(sSerializer);
            SessionConfiguration cfg = SessionConfiguration.builder()
                    .named(sName)
                    .withScopeName(sName)
                    .withMode(Coherence.Mode.GrpcFixed)
                    .withParameter("coherence.serializer", sSerializer)
                    .withParameter("coherence.profile", "thin")
                    .withParameter("coherence.proxy.enabled", "false")
                    .build();

            setSessionName.add(sName);
            cfgBuilder.withSession(cfg);
            }

        Coherence coherence = Coherence.clusterMember(cfgBuilder.build()).start().get(5, TimeUnit.MINUTES);

        Eventually.assertDeferred(IsGrpcProxyRunning::locally, is(true));

        for (String sName : setSessionName)
            {
            Session session = coherence.getSession(sName);
            SESSIONS.put(sName, session);
            }
        s_defaultSession = coherence.getSession();
        }

    @AfterAll
    static void shutdownCoherence()
        {
        Coherence.closeAll();
        }

    @Test
    void shouldCleanUpProxiesOnServiceStop()
        {
        Coherence        coherence   = Coherence.getInstance();
        Registry         management  = coherence.getManagement();
        MBeanServerProxy mbeanServer = management.getMBeanServerProxy();
        String           sCacheName  = "test";
        String           sMBean      = "Coherence:cache=" + sCacheName + ",nodeId=1,service=PartitionedCache,type=StorageManager";

        // create a gRPC client cache
        NamedCache<String, String> cache = createClient(sCacheName);

        // Add a key listener and filter listener
        CollectingMapListener<String, String> listener1 = new CollectingMapListener<>();
        CollectingMapListener<String, String> listener2 = new CollectingMapListener<>();
        cache.addMapListener(listener1, "key-1", false);
        cache.addMapListener(listener2, Filters.always(), false);

        // we should see both listeners registered with the underlying cache by looking at the MBean attributes
        int cKey = (Integer) mbeanServer.getAttribute(sMBean, "ListenerKeyCount");
        int cFilter = (Integer) mbeanServer.getAttribute(sMBean, "ListenerFilterCount");
        assertThat(cKey, is(1));
        assertThat(cFilter, is(1));

        // Do a put to the cache and assert we get the insert events
        cache.put("key-1", "value-1");
        Eventually.assertDeferred(listener1::getInsertCount, is(1));
        Eventually.assertDeferred(listener2::getInsertCount, is(1));

        // get the gRPC proxy service
        SafeService safeProxyService = (SafeService) coherence.getCluster().getService("$GRPC:GrpcProxy");
        Service proxyService = safeProxyService.getService();
        CollectingServiceListener serviceListener = new CollectingServiceListener();
        // add a service listener to the underlying service to detect when it stops
        proxyService.addServiceListener(serviceListener);

        // Stop the proxy service, the service monitor will auto restart it
        proxyService.stop();
        // wait for the service to have been stopped
        Eventually.assertDeferred(serviceListener::getStoppedCount, is(1));
        // wait for the service to restart
        Eventually.assertDeferred(safeProxyService::isRunning, is(true), Timeout.of(5, TimeUnit.MINUTES));

        // there should now be no listeners registered
        cKey = (Integer) mbeanServer.getAttribute(sMBean, "ListenerKeyCount");
        cFilter = (Integer) mbeanServer.getAttribute(sMBean, "ListenerFilterCount");
        assertThat(cKey, is(0));
        assertThat(cFilter, is(0));
        }

    // ----- helper methods -------------------------------------------------

    protected static String sessionNameFromSerializerName(String sSerializerName)
        {
        return sSerializerName.isEmpty() ? "default" : sSerializerName;
        }

    protected <K, V> NamedCache<K, V> createClient(String sCacheName)
        {
        String sName    = sessionNameFromSerializerName("java");
        Session session = SESSIONS.get(sName);
        assertThat(session, is(notNullValue()));
        return session.getCache(sCacheName);
        }

    // ----- inner class: CollectingMapListener -----------------------------

    /**
     * A test listener that collects the received map events.
     *
     * @param <K>  the type of the event key
     * @param <V>  the type of the event value
     */
    protected static class CollectingMapListener<K, V>
            implements MapListener<K, V>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Create a {@link AbstractGrpcClientIT.CollectingMapListener}.
         */
        public CollectingMapListener()
            {
            this(0);
            }

        /**
         * Create a {@link AbstractGrpcClientIT.CollectingMapListener} that can wait for a specific number of events.
         *
         * @param nCount  the number of events to wait for
         */
        public CollectingMapListener(int nCount)
            {
            this.f_latch = new CountDownLatch(nCount);
            }

        // ----- MapListener interface --------------------------------------

        @Override
        public void entryInserted(MapEvent<K, V> mapEvent)
            {
            m_cInsert++;
            f_latch.countDown();
            }

        @Override
        public void entryUpdated(MapEvent<K, V> mapEvent)
            {
            m_cUpdate++;
            f_latch.countDown();
            }

        @Override
        public void entryDeleted(MapEvent<K, V> mapEvent)
            {
            m_cDelete++;
            f_latch.countDown();
            }

        // ----- public methods ---------------------------------------------

        /**
         * Wait for this listener to receive the required number of events
         * that was set when it was created.
         *
         * @param cTimeout  the amount of time to wait
         * @param units     the time units for the timeout value
         *
         * @return true if the number of events was received
         *
         * @throws InterruptedException if the thread is interrupted
         * @see CountDownLatch#await(long, TimeUnit)
         */
        public boolean awaitEvents(long cTimeout, TimeUnit units) throws InterruptedException
            {
            return f_latch.await(cTimeout, units);
            }

        // ----- accessors --------------------------------------------------

        public int getInsertCount()
            {
            return m_cInsert;
            }

        public int getUpdateCount()
            {
            return m_cUpdate;
            }

        public int getDeleteCount()
            {
            return m_cDelete;
            }

        // ----- data members -----------------------------------------------

        protected int m_cInsert;

        protected int m_cUpdate;

        protected int m_cDelete;

        protected final CountDownLatch f_latch;
        }

    // ----- inner class: CollectingServiceListener -------------------------

    /**
     * A test listener that collects the received service events.
     */
    protected static class CollectingServiceListener
            implements ServiceListener
        {
        // ----- constructors -----------------------------------------------

        /**
         * Create a {@link AbstractGrpcClientIT.CollectingMapListener}.
         */
        public CollectingServiceListener()
            {
            this(0);
            }

        /**
         * Create a {@link AbstractGrpcClientIT.CollectingMapListener} that can wait for a specific number of events.
         *
         * @param nCount  the number of events to wait for
         */
        public CollectingServiceListener(int nCount)
            {
            this.f_latch = new CountDownLatch(nCount);
            }

        // ----- MapListener interface --------------------------------------


        @Override
        public void serviceStarting(ServiceEvent evt)
            {
            m_cStarting++;
            f_latch.countDown();
            }

        @Override
        public void serviceStarted(ServiceEvent evt)
            {
            m_cStarted++;
            f_latch.countDown();

            }

        @Override
        public void serviceStopping(ServiceEvent evt)
            {
            m_cStopping++;
            f_latch.countDown();
            }

        @Override
        public void serviceStopped(ServiceEvent evt)
            {
            m_cStopped++;
            f_latch.countDown();
            }

        // ----- public methods ---------------------------------------------

        /**
         * Wait for this listener to receive the required number of events
         * that was set when it was created.
         *
         * @param cTimeout  the amount of time to wait
         * @param units     the time units for the timeout value
         *
         * @return true if the number of events was received
         *
         * @throws InterruptedException if the thread is interrupted
         * @see CountDownLatch#await(long, TimeUnit)
         */
        public boolean awaitEvents(long cTimeout, TimeUnit units) throws InterruptedException
            {
            return f_latch.await(cTimeout, units);
            }

        // ----- accessors --------------------------------------------------

        public int getStartingCount()
            {
            return m_cStarting;
            }

        public int getStartedCount()
            {
            return m_cStarted;
            }

        public int getStoppingCount()
            {
            return m_cStopping;
            }

        public int getStoppedCount()
            {
            return m_cStopped;
            }

        // ----- data members -----------------------------------------------

        protected int m_cStarting;

        protected int m_cStarted;

        protected int m_cStopping;

        protected int m_cStopped;

        protected final CountDownLatch f_latch;
        }

    // ----- data members ---------------------------------------------------

    static final Map<String, Session> SESSIONS = new HashMap<>();

    static Session s_defaultSession;

    static final LocalPlatform PLATFORM = LocalPlatform.get();

    static final AvailablePortIterator PORTS = PLATFORM.getAvailablePorts();
    }
