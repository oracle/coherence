/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package events;

import com.tangosol.util.Filter;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.filter.EqualsFilter;
import com.tangosol.util.filter.MapEventFilter;
import com.oracle.coherence.testing.AbstractFunctionalTest;

import com.oracle.coherence.common.base.Blocking;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;

import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.InvocationService;
import com.tangosol.net.Member;
import com.tangosol.net.NamedCache;

import com.tangosol.net.cache.CacheEvent;

import com.tangosol.util.ImmutableArrayList;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MultiplexingMapListener;
import com.tangosol.util.WrapperException;

import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.InKeySetFilter;

import java.util.Set;

import events.common.BinaryEntryAssertingInterceptor;
import events.common.MutatingInterceptor.SetThrowModInvocable;

import org.hamcrest.CoreMatchers;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static events.EventTestHelper.remoteReset;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Basic tests for events.
 */
public class EntryEventsTests
        extends AbstractFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default Constructor.
    */
    public EntryEventsTests()
        {
        super(CFG_FILE);
        }

    // ----- test lifecycle -------------------------------------------------

    @BeforeClass
    public static void __startup()
        {
        CoherenceClusterMember clusterMember = startCacheServer("EntryEventTests", "events", CFG_FILE);

        Eventually.assertThat(invoking(clusterMember).isServiceRunning("ResultsService"), CoreMatchers.is(true));
        Eventually.assertThat(invoking(clusterMember).isServiceRunning("DistributedService"), CoreMatchers.is(true));
        }

    /**
     * Clean up state after test.
     */
    @After
    public void tearDown()
        {
        getNamedCache("results").clear();
        }

    /**
     * Shutdown the test class.
     */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("EntryEventTests");
        }


    // ----- test methods ---------------------------------------------------

    /**
     * Test that we can modify an insert event.
     */
    @Test
    public void testInsertingEntryEvent() throws InterruptedException
        {
        NamedCache cache = getNamedCache("dist-write-insert");
        try
            {
            cache.put(1, 1);
            assertEquals(2, cache.get(1));
            }
        finally
            {
            cache.clear();
            }
        }

    /**
     * Test that we can veto an insert event.
     */
    @Test
    public void testInsertingEntryEventException() throws InterruptedException
        {
        NamedCache cache = getNamedCache("dist-write-insert");
        try
            {
            boolean fCaught  = false;
            try
                {
                cache.put(10, 10);
                }
            catch (WrapperException wEx)
                {
                if (wEx.getOriginalException() instanceof RuntimeException)
                    {
                    fCaught = true;
                    }
                else
                    {
                    throw wEx;
                    }
                }

            assertTrue(fCaught);
            assertFalse(cache.containsKey(10));
            assertEquals(0, cache.size());
            }
        finally
            {
            cache.clear();
            }
        }

    /**
     * Test that we can listen to an insert event.
     */
    @Test
    public void testInsertedEntryEvent() throws InterruptedException
        {
        NamedCache cache   = getNamedCache("dist-ro-insert");
        NamedCache results = getNamedCache("results");

        try
            {
            ResultsListener listener = new ResultsListener();

            results.addMapListener(listener, 1, false);
            putAndCheckEvent(cache, listener, 1);
            results.removeMapListener(listener, 1);

            results.addMapListener(listener, AlwaysFilter.INSTANCE, false);
            putAndCheckEvent(cache, listener, 2);
            results.removeMapListener(listener, AlwaysFilter.INSTANCE);

            Set setKeys = new ImmutableArrayList(new Integer[]{1, 2});
            results.addMapListener(listener, new InKeySetFilter(null, setKeys), false);
            putAndCheckEvent(cache, listener, 3);
            results.removeMapListener(listener, setKeys);
            }
        finally
            {
            cache.clear();
            results.release();
            }
        }

    /**
     * Test that we can modify an update event.
     */
    @Test
    public void testUpdatingEntryEvent() throws InterruptedException
        {
        NamedCache cache = getNamedCache("dist-write-insert");
        try
            {
            cache.put(1, 1);
            assertEquals(2, cache.get(1));

            cache.put(1, 3);
            assertEquals(4, cache.get(1));
            }
        finally
            {
            cache.clear();
            }
        }

    /**
     * Test that we can veto an update event.
     */
    @Test
    public void testUpdatingEntryEventException() throws InterruptedException
        {
        NamedCache cache = getNamedCache("dist-write-insert");
        try
            {
            boolean fCaught  = false;
            try
                {
                cache.put(1, 1);
                cache.put(1, 10);
                }
            catch (WrapperException wEx)
                {
                if (wEx.getOriginalException() instanceof RuntimeException)
                    {
                    fCaught = true;
                    }
                else
                    {
                    throw wEx;
                    }
                }

            assertTrue(fCaught);
            assertEquals(2, cache.get(1));
            assertEquals(1, cache.size());
            }
        finally
            {
            cache.clear();
            }
        }

    /**
     * Test that we can listen to an update event.
     */
    @Test
    public void testUpdatedEntryEvent() throws InterruptedException
        {
        NamedCache cache   = getNamedCache("dist-ro-insert");
        NamedCache results = getNamedCache("results");

        // result events are asynchronous
        try
            {
            ResultsListener listener = new ResultsListener();
            results.addMapListener(listener, 1, false);

            putAndCheckEvent(cache, listener, 1);
            putAndCheckEvent(cache, listener, 2);
            }
        finally
            {
            cache.clear();
            results.release();
            }
        }

    /**
     * Test that we can modify a remove event.
     */
    @Test
    public void testRemovingEntryEvent() throws InterruptedException
        {
        NamedCache cache        = getNamedCache("dist-write-insert");
        NamedCache resultsCache = getNamedCache("results");
        try
            {
            cache.put(1, 1);
            assertEquals(2, cache.get(1));

            cache.remove(1);

            assertEquals("Removed", resultsCache.get(1));
            assertEquals(0, cache.size());
            }
        finally
            {
            cache.clear();
            }
        }

    /**
     * Test that we can veto a remove event.
     */
    @Test
    public void testRemovingEntryEventException() throws InterruptedException
        {
        NamedCache        cache         = getNamedCache("dist-write-insert");
        InvocationService service       = (InvocationService)
                getFactory().ensureService(INVOCATION_SERVICE_NAME);
        Set<Member>       setRecipients = service.getInfo().getServiceMembers();

        setRecipients.remove(service.getCluster().getLocalMember());
        try
            {
            boolean fCaught  = false;
            try
                {
                cache.put(1, 1);

                service.query(new SetThrowModInvocable(-1), setRecipients);

                cache.remove(1);
                }
            catch (WrapperException wEx)
                {
                if (wEx.getOriginalException() instanceof RuntimeException)
                    {
                    fCaught = true;
                    }
                else
                    {
                    throw wEx;
                    }
                }

            assertTrue(fCaught);
            assertEquals(2, cache.get(1));
            assertEquals(1, cache.size());
            }
        finally
            {
            service.query(new SetThrowModInvocable(10), setRecipients);
            cache.clear();
            }
        }

    /**
     * Test that we can listen to a remove event.
     */
    @Test
    public void testRemovedEntryEvent() throws InterruptedException
        {
        NamedCache cache   = getNamedCache("dist-ro-insert");
        NamedCache results = getNamedCache("results");

        try
            {
            // result events are asynchronous
            ResultsListener listener = new ResultsListener();
            results.addMapListener(listener, 1, false);

            putAndCheckEvent(cache, listener, 1);
            putAndCheckEvent(cache, listener, null);
            }
        finally
            {
            cache.clear();
            results.release();
            }
        }

    /**
     * Test the original binary returned in a post event is as expected.
     */
    @Test
    public void testOriginalBinaryPostEvent()
            throws InterruptedException
        {
        ConfigurableCacheFactory ccf   = getFactory();
        NamedCache               cache = getNamedCache("dist-ob-test");
        try
            {
            cache.put(1, 1);
            assertEquals(1, cache.get(1));

            InvocationService service = (InvocationService) ccf.ensureService(INVOCATION_SERVICE_NAME);
            Set<Member> setRecipients = service.getInfo().getServiceMembers();

            setRecipients.remove(service.getCluster().getLocalMember());
            Eventually.assertThat(invoking(new EventTestHelper())
                .remoteFail(BinaryEntryAssertingInterceptor.IDENTIFIER, ccf, setRecipients), is(1));

            remoteReset(BinaryEntryAssertingInterceptor.IDENTIFIER, ccf, setRecipients);
            }
        finally
            {
            cache.clear();
            }
        }

    /**
     * Ensure synthetic delete is received, resulting from eviction
     */
    @Test
    public void testExpiringDeleted()
            throws InterruptedException
        {
        NamedCache cache = getNamedCache("dist-exp");

        try
            {
            ResultsListener listener = new ResultsListener();
            cache.addMapListener(listener, 1, false);

            putAndCheckEvent(cache, listener, 1);
            // wait for synthetic delete
            listener.reset();
            listener.waitForResult();

            assertEquals(true, ((CacheEvent) listener.getLastEvent()).isSynthetic());
            assertEquals(true, ((CacheEvent) listener.getLastEvent()).isExpired());
            }
        finally
            {
            cache.clear();
            }
        }

    @Test
    public void testExpiringDeletedRWBM()
        {
        NamedCache cache = getNamedCache("dist-rwbm-exp");

        try
            {
            ResultsListener listener = new ResultsListener();
            cache.addMapListener(listener, 1, false);

            putAndCheckEvent(cache, listener, 1);
            // wait for synthetic delete
            listener.reset();
            listener.waitForResult();

            assertEquals(true, ((CacheEvent) listener.getLastEvent()).isSynthetic());
            assertEquals(true, ((CacheEvent) listener.getLastEvent()).isExpired());
            }
        finally
            {
            cache.clear();
            }
        }

    @Test
    public void testExpiringDeletedLocal()
        throws InterruptedException
        {
        NamedCache cache = getNamedCache("local-exp");

        try
            {
            ResultsListener listener = new ResultsListener();
            cache.addMapListener(listener, 1, false);

            putAndCheckEvent(cache, listener, 1);
            listener.reset();

            sleep(5000);
            // wait for synthetic delete
            cache.get(1); // Force eviction
            listener.waitForResult();

            assertEquals(true, ((CacheEvent) listener.getLastEvent()).isSynthetic());
            assertEquals(true, ((CacheEvent) listener.getLastEvent()).isExpired());
            }
        finally
            {
            cache.clear();
            }
        }

    /**
     * Helper.
     */
    protected void putAndCheckEvent(NamedCache cache, ResultsListener listener, Object oPut)
        {
        listener.reset();

        Object oExpect;
        if (oPut == null)
            {
            cache.remove(1);
            oExpect = "Removed";
            }
        else
            {
            cache.put(1, oPut);
            oExpect = oPut;
            }

        assertEquals(oExpect, listener.waitForResult());
        }

    /**
     * Tests for COH-16297 (filter event evaluation optimization)
     */
    @Test
    public void testCoh16297()
        {
        NamedCache<Long, String> cache = getNamedCache("dist-standard");

        ValueExtractor<String, Integer> extractor = new CountingExtractor();

        // add listeners for length between 0 and 10
        ResultsListener[] aListener = new ResultsListener[10];
        for (int cChars = 0; cChars < 10; cChars++)
            {
            aListener[cChars] = new ResultsListener();
            Filter filter = new EqualsFilter<>(extractor, cChars);
            cache.addMapListener(aListener[cChars], new MapEventFilter(filter), false);
            }

        String ALPHA = "hello world";
        for (int i = 1; i < 7; i++)
            {
            String s = ALPHA.substring(0, i);

            cache.put(0l, s);

            assertEquals("No event: " + i, s, aListener[s.length()].waitForResult());
            assertFalse("Unexpected event: " + i, aListener[s.length() + 1].m_fResult);

            // even though there are 10 filters,
            // the extraction should only happen once per insert/remove and
            // twice per update (old/new values)
            int cExpected = i == 1 ? 1 : i*2 - 1;
            int cExtracts = cache.invoke(0l, (entry) -> CountingExtractor.s_cExtracts);
            assertEquals("Failed optimization", cExpected, cExtracts);
            }
        cache.destroy();
        }


    // ----- inner classes --------------------------------------------------

    public class ResultsListener
            extends MultiplexingMapListener
        {
        @Override
        protected void onMapEvent(MapEvent evt)
            {
            synchronized (this)
                {
                out("onMapEvent: " + evt);
                m_oValue  = evt.getNewValue();
                m_fResult = true;
                m_oEvt    = evt;
                notify();
                }
            }

        public synchronized Object waitForResult()
            {
            while (!m_fResult)
                {
                try
                    {
                    Blocking.wait(this, 10000);
                    }
                catch (InterruptedException e)
                    {
                    fail();
                    }
                }
            return m_oValue;
            }

        public synchronized void reset()
            {
            m_fResult = false;
            m_oValue  = null;
            }

        public MapEvent getLastEvent()
            {
            return m_oEvt;
            }

        // ----- data members -----------------------------------------------

        /**
         * The value to be notified of.
         */
        protected Object m_oValue;

        /**
         * Used as a signal to know when the results cache has been updated.
         */
        protected volatile boolean m_fResult;

        /**
         * Actual event received.
         */
        protected volatile MapEvent m_oEvt;
        }


    public static class CountingExtractor
            implements ValueExtractor<String, Integer>
        {
        public Integer extract(String sTarget)
            {
            s_cExtracts++;
            return sTarget == null ? 0 : sTarget.length();
            }

        public int hashCode()
            {
            return 0;
            }

        public boolean equals(Object obj)
            {
            return obj instanceof CountingExtractor;
            }

        public static int s_cExtracts;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The Cache config file to use for these tests.
     */
    public static final String CFG_FILE = "basic-server-cache-config.xml";

    /**
    * The name of the InvocationService used by all test methods.
    */
    public static String INVOCATION_SERVICE_NAME = "InvocationService";
    }
