/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package cache;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.junit.CoherenceClusterResource;
import com.oracle.bedrock.junit.SessionBuilders;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.java.features.JmxFeature;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.runtime.options.DisplayName;
import com.tangosol.coherence.config.Config;

import com.tangosol.internal.util.invoke.Lambdas;

import com.tangosol.net.CacheService;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MultiplexingMapListener;

import com.oracle.coherence.testing.SystemPropertyIsolation;
import org.junit.ClassRule;
import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;

/**
 * COH-15803 regression tests
 *
 * Until resolve differences between manual test and automated test,
 * here is steps for manual testing with comments when could not
 * recreate in automated testing.
 *
 *
 * Original manual testing was between console cache servers. Sequence was:
 *
 * master cache-server console:
 * % cache t
 * % listen start
 *
 * other cache-server console:
 * % cache t
 * % put 1 1                           # observe listener in master cache-server console
 *
 * master cache-server console:
 * % &getCacheService.getService.stop  # due to Java method permissions, can't do this in automated test.
 *                                     # only can stop the SafeCacheService, not its internal service.
 * % cache t
 *
 * other cache-server console:
 * % put 2 2                           # if successfully fixed, initial entry listener sees this put in
 *                                     # master cache-server console.
 *
 * Automated OracleTools testing is between a client without local storage and a single cache-server.
 * Operations on other cache-server are carried out by OracleTools feature to update
 * a cache on another cache-server via {@link CoherenceClusterMember#getCache(String)).
 */
public class COH15083RegressionTests
    {
    /**
     * Validate that addMapListener against an inactive cache, reactivates the
     * cache and its service, if necessary.
     */
    @Test
    public void testAddMapListenerReactivatesCache()
        {
        final String                       sName    = "testAddMapListenerReactivatesCache";
        ExtensibleConfigurableCacheFactory eccf     = getECCF();
        NamedCache                         cache    = eccf.ensureCache(sName, null);
        ClientListener                     listener = new ClientListener();
        CoherenceClusterMember             otherMember;
        NamedCache                         proxyCache;

        try
            {
            otherMember = cluster.getCluster().get("storage-1");

            proxyCache = otherMember.getCache(sName);
            proxyCache.put(1, 1);
            Eventually.assertThat(invoking(cache).size(), is(1));
            Eventually.assertThat(invoking(proxyCache).size(), is(1));

            // simulate network outage by making service temporarily unavailable.
            // makes cache inactive by stopping PartitionedCache service locally
            CacheService cacheService = cache.getCacheService();
            String cacheServiceName = cacheService.getInfo().getServiceName();

            // this should probably be an internalService stop rather than a SafeService.stop().
            cacheService.stop();
            Eventually.assertThat(invoking(cache).isActive(), is(false));

            // revive cache service to simulate network connectivity coming back.
            // WORKAROUND that allows an explicitly stopped safe service to be restarted.
            eccf.ensureService(cacheServiceName);

            // ensure that after calling addMapListener, that the cache is
            // now active again.
            // TODO: without above workaround, get IllegalStateException attempt to start an explicitly stopped safe service
            /*
             * java.lang.IllegalStateException: SafeService was explicitly stopped
             *
             * at com.tangosol.coherence.component.util.SafeService.ensureRunningService(SafeService.java:506)
             * at com.tangosol.coherence.component.util.SafeService.getRunningService(SafeService.java:621)
             * at com.tangosol.coherence.component.util.SafeNamedCache.restartNamedCache(SafeNamedCache.java:1203)
             * at com.tangosol.coherence.component.util.SafeNamedCache.ensureRunningNamedCache(SafeNamedCache.java:644)
             * at com.tangosol.coherence.component.util.SafeNamedCache.getRunningNamedCache(SafeNamedCache.java:902)
             * at com.tangosol.coherence.component.util.SafeNamedCache.addMapListener(SafeNamedCache.java:455)
             * at com.tangosol.coherence.component.util.SafeNamedCache.addMapListener(SafeNamedCache.java:493)
             * at com.tangosol.coherence.component.util.SafeNamedCache.addMapListener(SafeNamedCache.java:441)
             * at cache.COH15083RegressionTests.testAddMapListenerReactivatesCache(COH15083RegressionTests.java:67)
             */
            cache.addMapListener(listener);
            Eventually.assertThat(invoking(cache).isActive(), is(true));
            Eventually.assertThat(cache.size(), is(1));

            proxyCache.put(2, 2);

            // verify that ClientListener was still associated with cache,
            // even after a simulated DistributedService failure.
            Eventually.assertThat(invoking(cache).size(), is(2));
            listener.assertEvent(true);
            listener.clear();
            }
        finally
            {
            try
                {
                eccf.destroyCache(cache);
                }
            catch (IllegalStateException e)
                {
                // ignore failure during cleanup.
                }
            }
        }

    @Test
    public void testMapListenerSurvivesCacheServiceFailure()
        {
        final String                       sName    = "testMapListenerSurvivesCacheServiceFailure";
        ExtensibleConfigurableCacheFactory eccf     = getECCF();
        NamedCache                         cache    = eccf.ensureCache(sName, null);
        ClientListener                     listener = new ClientListener();
        CoherenceClusterMember             otherMember;

        Eventually.assertThat(invoking(cache).size(), is(0));

        cache.addMapListener(listener);

        // update cache in another cache-server and be sure to observe update in local ClientListener.
        otherMember = cluster.getCluster().get("storage-1");
        NamedCache proxyCache = otherMember.getCache(sName);
        proxyCache.put(1, 1);
        Eventually.assertThat(invoking(cache).size(), is(1));
        Eventually.assertThat(invoking(proxyCache).size(), is(1));
        listener.assertEvent(true);
        listener.clear();

        // make cache inactive by stopping PartitionedCache service locally
        CacheService cacheService = cache.getCacheService();
        String cacheServiceName = cacheService.getInfo().getServiceName();
        cacheService.stop();
        Eventually.assertThat(invoking(cache).isActive(), is(false));

        // revive cache service to simulate network connectivity coming back. Added in ECCF.ensureTypedCache inactive.
        eccf.ensureService(cacheServiceName);

        cache = eccf.ensureCache(sName, null);
        Eventually.assertThat(invoking(cache).isActive(), is(false));

        Eventually.assertThat(cache.size(), is(1));

        boolean fActive = cache.isActive();
        proxyCache.put(2, 2);

        // verify that ClientListener was still associated with cache,
        // even after a simulated DistributedService failure.
        Eventually.assertThat(invoking(cache).size(), is(2));
        listener.assertEvent(fActive);
        listener.clear();

        fActive = cache.isActive();
        proxyCache.put(3, 3);

        // verify that ClientListener was still associated with cache,
        // even after a simulated DistributedService failure.
        Eventually.assertThat(invoking(cache).size(), is(3));
        listener.assertEvent(fActive);
        listener.clear();

        eccf.destroyCache(cache);
        }

    /**
     * Ensure destroy of cache in a storage member is recognized in client.
     */
    @Test
    public void testPropagationDestroy()
        {
        final String           sName = "testPropagationDestroy";
        NamedCache             cache = getECCF().ensureCache(sName, null);
        CoherenceClusterMember otherMember;

        otherMember = cluster.getCluster().get("storage-1");
        NamedCache proxyCache = otherMember.getCache(sName);
        proxyCache.put(1, 1);
        Eventually.assertThat(invoking(cache).size(), is(1));

        Eventually.assertThat(invoking(cache).isDestroyed(), is(false));
        Eventually.assertThat(invoking(proxyCache).isDestroyed(), is(false));
        proxyCache.destroy();

        // OracleTools NamedCache proxy does not support isDestroyed() yet.
        // Eventually.assertThat("validate proxy isDestroyed after destroy called on proxy",
        //                      invoking(proxyCache).isDestroyed(), is(true));
        Eventually.assertThat("validate that destroy of proxy has propagated back to this client",
                              invoking(cache).isDestroyed(), is(true));
        try
            {
            cache.isEmpty();
            fail("cache.isEmpty() on a destroyed cache must throw IllegalStateException");
            }
        catch (IllegalStateException e)
            {
            // expected result accessing destroyed cache
            }
        }

    // ----- inner class Listener -------------------------------------------

    /**
     * Listener class to test if an entry has been inserted.
     */
    public static class Listener
            extends MultiplexingMapListener
        {
        /**
         * Construct a listener.
         */
        public Listener()
            {
            super();
            }

        public boolean wasInserted()
            {
            return m_fInsertEvent;
            }

        protected void clear()
            {
            m_fInsertEvent = false;
            }

        @Override
        protected void onMapEvent(MapEvent evt)
            {
            m_fInsertEvent = evt.getId() == MapEvent.ENTRY_INSERTED;
            }

        /**
         * The ENTRY_INSERTED event was received by the client listener.
         */
        private volatile boolean m_fInsertEvent;
        }

    // ----- inner class ClientListener -------------------------------------

    /**
     * ClientListener class needed to test client listener.
     */
    public static class ClientListener
            extends Listener
        {
        public ClientListener()
            {
            super();
            }

        /**
         * Assert that the client received the listener event
         *
         * @param fExpected  true if client event expected
         */
        protected void assertEvent(boolean fExpected)
            {
            Eventually.assertThat(invoking(this).wasInserted(), is(fExpected));
            }

        @Override
        protected void onMapEvent(MapEvent evt)
            {
            super.onMapEvent(evt);
            }

        @Override
        protected void clear()
            {
            super.clear();
            }
        }

    protected ExtensibleConfigurableCacheFactory getECCF()
        {
        if (m_eccf == null)
            {
            m_eccf =(ExtensibleConfigurableCacheFactory) cluster
                    .createSession(SessionBuilders.storageDisabledMember());
            }
        return m_eccf;
        }

    // ----- data members ---------------------------------------------------

    public static final int STORAGE_MEMBER_COUNT = 1;

    @ClassRule
    public static CoherenceClusterResource cluster =
            new CoherenceClusterResource()
                    .with(ClusterName.of(COH15083RegressionTests.class.getSimpleName()),
                          SystemProperty.of("coherence.management", "all"),
                          JmxFeature.enabled(),
                          SystemProperty.of(Lambdas.LAMBDAS_SERIALIZATION_MODE_PROPERTY,
                                            Config.getProperty(Lambdas.LAMBDAS_SERIALIZATION_MODE_PROPERTY)))
                    .include(STORAGE_MEMBER_COUNT, DisplayName.of("storage"), LocalStorage.enabled());

    /**
     * A {@link ClassRule} to isolate system properties set between test class
     * execution (not individual test method executions).
     */
    @ClassRule
    public static SystemPropertyIsolation s_systemPropertyIsolation = new SystemPropertyIsolation();

    private ExtensibleConfigurableCacheFactory m_eccf;
    }
