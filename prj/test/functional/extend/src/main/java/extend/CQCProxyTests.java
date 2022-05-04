/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package extend;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.MemberEvent;
import com.tangosol.net.MemberListener;
import com.tangosol.net.NamedCache;
import com.tangosol.net.cache.ContinuousQueryCache;

import com.tangosol.util.MapEvent;
import com.tangosol.util.MultiplexingMapListener;

import com.tangosol.util.filter.AlwaysFilter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static com.oracle.bedrock.deferred.DeferredHelper.within;

import static org.hamcrest.CoreMatchers.is;

/**
 * Coherence*Extend test for the ContinuousQueryCache receiving events
 * after restarting the proxy server.
 * Tests COH-8145 (Bug14768607)
 * Tests COH-8470 (Bug15966691)
 *
 * @author par  2012.11.5
 */
public class CQCProxyTests
        extends AbstractExtendTest
    {
    // ----- constructors ---------------------------------------------------

    public CQCProxyTests()
        {
        super("dist-extend-direct", FILE_CLIENT_SIMPLE_CFG_CACHE);
        }

    // ----- test lifecycle -------------------------------------------------

    /**
     * Initialize the test class.
     */
    @BeforeClass
    public static void startup()
        {

        Properties props = new Properties();
        props.setProperty("test.server.distributed.localstorage", "false");

        startCacheServerWithProxy("CQCProxyTestsProxy", "extend", FILE_SERVER_SIMPLE_CFG_CACHE, props);

        for (int i = 0; i < NUM_STORAGE_NODES_IN_CLUSTER; i++)
            {
            props = new Properties();
            props.setProperty("test.extend.enabled","false");
            storageMembers.add(startCacheServer("CQCProxyTestsServer-" + i, "extend", FILE_SERVER_SIMPLE_CFG_CACHE, props));
            }

        for (int i = 0; i < NUM_STORAGE_NODES_IN_CLUSTER; i++)
            {
            CoherenceClusterMember member = storageMembers.get(i);
            Eventually.assertThat(invoking(member).isServiceRunning("Cluster"), is(true), within(2, TimeUnit.MINUTES));
            Eventually.assertThat(invoking(member).isOperational(), is(true), within(2, TimeUnit.MINUTES));
            }

        Eventually.assertThat(CacheFactory.getCluster().getMemberSet().size(), is(NUM_STORAGE_NODES_IN_CLUSTER + 2), within(2, TimeUnit.MINUTES));
        }

    /**
     * Shutdown the test class.
     */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("CQCProxyTestsProxy");
        stopCacheServer("CQCProxyTestsProxy-restarted");
        for (int i = 0; i < NUM_STORAGE_NODES_IN_CLUSTER; i++)
            {
            stopCacheServer("CQCProxyTestsServer-" + i);
            }
        }

    // ----- CQCProxyTests tests --------------------------------------------

    /**
     * Test the behavior of proxy returning events.
     * COH-8145 reports the CQC doesn't receive events after
     * the proxy is restarted.
     * COH-8470 reports that the CQC resynchronizes multiple
     * times, giving double or triple events.
     *
     * Put a known number of data items into the inner cache,
     * then count the number of events the listener receives
     * after restarting the proxy.
     */
    @Test
    public void testEvents()
        {
        // put data items into inner cache to generate events
        NamedCache cache = getNamedCache();

        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < SOME_DATA; i++)
            {
            map.put("TestKey" + i, i);
            }
        cache.putAll(map);

        //create listener for CQC
        TestCQCListener listener = new TestCQCListener();

        // instantiate the CQC, will start the test running.
        NamedCache cacheCQC = new ContinuousQueryCache(cache, AlwaysFilter.INSTANCE, listener);

        // check listener received the correct number of events.
        Eventually.assertThat(invoking(listener).getEventCount(), is(SOME_DATA));

        // verify that no additional events come in
        sleep(250);
        Assert.assertTrue(listener.getEventCount() == SOME_DATA);

        // add member listener to inner cache to receive memberLeft
        // event; intermittently, the "get" which restarts the
        // cache happens before the CQC receives the memberLeft,
        // the CQC isn't reinitialized, so the events are not re-sent.
        // adding this listener depends on the ordering of the
        // listeners for the cache service, we want this one last.
        // therefore, adding after CQC had been instantiated
        TestMemberListener listenerMember = new TestMemberListener();
        cacheCQC.getCacheService().addMemberListener(listenerMember);

        listener.resetEventCount();

        // restart proxy
        restartProxy();

        // wait for memberLeft event before pinging the CQC
        Eventually.assertThat(invoking(listenerMember).isMemberLeft(), is(true));

        // ping the CQC to make it realize the cache needs restart.  May not
        // actually be necessary, but doesn't hurt.
        cacheCQC.get("junkkey");

        // check listener received the correct number of events.
        Eventually.assertThat(invoking(listener).getEventCount(), is(SOME_DATA));

        // verify that no additional events come in
        sleep(250);
        Assert.assertTrue(listener.getEventCount() == SOME_DATA);
        }

    /**
     * Test the behavior of proxy after the cache is destroyed.
     *
     * Put a known number of data items into the inner cache,
     * then count the number of events the listener receives
     * after destroying the cache.
     */
    @Test
    public void testGetPutAfterCacheDestroy()
        {
        // wipe out any existing cache if present
        getNamedCache();

        //create listener for CQC
        TestCQCListener listener = new TestCQCListener();

        // instantiate the CQC, will start the test running.
        NamedCache cacheCQC = new ContinuousQueryCache(() -> getFactory().ensureCache(getCacheName(), getClass().getClassLoader()),
                                                       AlwaysFilter.INSTANCE(), true, listener, null, null);

        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < SOME_DATA; i++)
            {
            map.put("TestKey" + i, i);
            }
        storageMembers.get(1).getCache(getCacheName()).putAll(map);

        // check listener received the correct number of events.
        Eventually.assertThat(invoking(listener).getEventCount(), is(SOME_DATA));

        // verify that no additional events come in
        sleep(250);
        Assert.assertTrue(listener.getEventCount() == SOME_DATA);

        // destroy the cache.
        CoherenceClusterMember lastMember = storageMembers.get(NUM_STORAGE_NODES_IN_CLUSTER - 1);
        lastMember.getCache(getCacheName()).destroy();

        Eventually.assertThat(invoking(cacheCQC).isActive(), is(false), within(2, TimeUnit.MINUTES));

        for (int i = SOME_DATA / 2; i < SOME_DATA + (SOME_DATA / 2); i++)
            {
            cacheCQC.put("TestKey" + i, 2 * i);
            }


        for (int i = 0; i < SOME_DATA / 2; i++)
            {
            Assert.assertNull(cacheCQC.get("TestKey" + i));
            }

        for (int i = SOME_DATA / 2; i < SOME_DATA + (SOME_DATA / 2); i++)
            {
            Assert.assertEquals(cacheCQC.get("TestKey" + i), (int) 2*i);
            }

        }

    // ----- helper methods -------------------------------------------------

    /**
     * utility method to stop and restart the proxy.
     */
    private void restartProxy()
        {
        CoherenceClusterMember m0 = storageMembers.get(0);
        int nClusterSize = m0.getClusterSize();

        stopCacheServer("CQCProxyTestsProxy");

        Eventually.assertThat(invoking(m0).getClusterSize(), is(nClusterSize - 1));

        Properties proxyProps = new Properties();
        proxyProps.setProperty("test.server.distributed.localstorage", "false");
        startCacheServerWithProxy("CQCProxyTestsProxy-restarted", "extend", FILE_SERVER_SIMPLE_CFG_CACHE, proxyProps);
        }

    // ----- inner class: TestCQCListener -----------------------------------

    /**
     * MapListener that continuously receives events from the cache.
     */
    public class TestCQCListener extends MultiplexingMapListener
        {

        // ----- MultiplexingMapListener methods ----------------------------

        @Override
        protected void onMapEvent(MapEvent evt)
            {
            ++m_cEventCount;
            }

        // ----- TestCQCListener methods ------------------------------------

        /**
         * Total number of events listener actually received.
         *
         * @return  number of event received
         */
        public int getEventCount()
            {
            return m_cEventCount;
            }

        /**
         * Reset the number of events received.
         */
        public void resetEventCount()
            {
            m_cEventCount = 0;
            }

        // ----- data members -----------------------------------------------

        protected volatile int m_cEventCount;
        }

    // ----- inner class: TestMemberListener --------------------------------

    /**
     * MemberListener which sets a flag when a MemberLeft event is received
     */
    protected class TestMemberListener implements MemberListener
        {

        // ----- MemberListener interface -----------------------------------

        /**
         * {@inheritDoc}
         */
        @Override
        public void memberJoined(MemberEvent evt)
            {
            // ignore
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void memberLeaving(MemberEvent evt)
            {
            // ignore
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void memberLeft(MemberEvent evt)
            {
            m_fMemberLeft = true;
            }

        /**
         * Return whether a MemberLeft event has been received.
         *
         * @return whether a MemberLeft event has been received
         */
        public boolean isMemberLeft()
            {
            return m_fMemberLeft;
            }

        // ----- data members -----------------------------------------------

        /**
         * Whether a MemberLeft event has been received.
         */
        private volatile boolean m_fMemberLeft = false;
        }

    // ----- data members ---------------------------------------------------

    /**
     * Storage enabled members
     */
    private static ArrayList<CoherenceClusterMember> storageMembers = new ArrayList<>();

    // ----- constants ------------------------------------------------------

    /**
     * Number of data items to put in cache; should generate same number of events.
     */
    private final int SOME_DATA = 100;

    /**
     * Number of storage members in this cluster.
     */
    private static final int NUM_STORAGE_NODES_IN_CLUSTER = 2;
    }
