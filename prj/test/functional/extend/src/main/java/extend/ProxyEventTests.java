/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package extend;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.tangosol.net.NamedCache;

import com.tangosol.util.Filter;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;

import com.tangosol.util.extractor.IdentityExtractor;
import com.tangosol.util.extractor.PofExtractor;

import com.tangosol.util.filter.EqualsFilter;
import com.tangosol.util.filter.LessFilter;
import com.tangosol.util.filter.MapEventFilter;

import com.oracle.coherence.testing.AbstractFunctionalTest;
import com.oracle.coherence.testing.TestObject;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

/**
 * Coherence*Extend test for map listeners receiving events.
 * Tests:
 * COH-8157 (Bug14778008)
 * COH-8238 (Bug - none)
 * COH-8578 (Bug16023459)
 *
 * Test the behavior of proxy returning events.
 * COH-8157 reported multiple events received when there is
 * both a key and a filter listener that should receive the
 * single event. Test cases;
 *  - one key listener
 *  - one filter listener
 *  - multiple key listeners
 *  - multiple filter listeners
 *  - one key, one filter listener
 *  - one key, multiple filter
 *  - multiple key, one filter
 *  - multiple key, multiple filter
 *
 * @author par  2012.11.2
 * @since @BUILDVERSION@
 */
public class ProxyEventTests
        extends AbstractFunctionalTest
    {

    // ----- constructors ---------------------------------------------------

    public ProxyEventTests()
        {
        super(AbstractExtendTests.FILE_CLIENT_CFG_CACHE);
        }


    // ----- test lifecycle -------------------------------------------------

    /**
     * Initialize the test class.
     */
    @BeforeClass
    public static void startup()
        {
        CoherenceClusterMember memberProxy = startCacheServer("ProxyEventTests", "extend",
                                                     AbstractExtendTests.FILE_SERVER_CFG_CACHE);
        Eventually.assertThat(invoking(memberProxy).isServiceRunning("ExtendTcpProxyService"), is(true));
        }

    /**
     * Shutdown the test class.
     */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("ProxyEventTests");
        }

    // ----- ProxyEventTests methods ----------------------------------------

    /**
     * Test events returned when one key listener is configured.
     */
    @Test
    public void oneKeyListener()
        {
        NamedCache cache = getTestCache();

        TestListener keyListener    = new TestListener(1, "KEY");
        cache.addMapListener(keyListener, "TestKey1", false);

        // wait for event that reports listeners have been configured
        waitForEvents(cache);

        // check how many were received
        assertEquals(keyListener.getInsertCount(), keyListener.getInsertExpected());
        assertEquals(keyListener.getUpdateCount(), keyListener.getUpdateExpected());
        assertEquals(keyListener.getDeleteCount(), keyListener.getDeleteExpected());

        cache.removeMapListener(keyListener);
        }

    /**
     * Test events returned when one filter listener is configured.
     */
    @Test
    public void oneFilterListener()
        {
        NamedCache cache = getTestCache();

        TestListener filterListener = new TestListener(SOME_EVENTS, "FILTER");

        Filter filter = new LessFilter(IdentityExtractor.INSTANCE, SOME_EVENTS);
        MapEventFilter eventFilter = new MapEventFilter(MapEventFilter.E_ALL, filter);

        cache.addMapListener(filterListener, eventFilter, false);

        // Wait for event that reports listeners have been configured.
        waitForEvents(cache);

        // check how many were received
        assertEquals(filterListener.getInsertCount(), filterListener.getInsertExpected());
        assertEquals(filterListener.getUpdateCount(), filterListener.getUpdateExpected());
        assertEquals(filterListener.getDeleteCount(), filterListener.getDeleteExpected());

        cache.removeMapListener(filterListener, eventFilter);
        }

    /**
     * Test events returned when more than one key listener is configured.
     */
    @Test
    public void multipleKeyListeners()
        {
        NamedCache cache = getTestCache();

        TestListener keyListener  = new TestListener(1, "KEY");
        TestListener keyListener2 = new TestListener(1, "KEY2");

        cache.addMapListener(keyListener,  "TestKey1", false);
        cache.addMapListener(keyListener2, "TestKey2", false);

        // wait for event that reports listeners have been configured.
        waitForEvents(cache);

        // check how many were received
        assertEquals(keyListener.getInsertCount(), keyListener.getInsertExpected());
        assertEquals(keyListener.getUpdateCount(), keyListener.getUpdateExpected());
        assertEquals(keyListener.getDeleteCount(), keyListener.getDeleteExpected());

        assertEquals(keyListener2.getInsertCount(), keyListener2.getInsertExpected());
        assertEquals(keyListener2.getUpdateCount(), keyListener2.getUpdateExpected());
        assertEquals(keyListener2.getDeleteCount(), keyListener2.getDeleteExpected());

        cache.removeMapListener(keyListener);
        cache.removeMapListener(keyListener2);
        }

    /**
     * Test events returned when more than one filter listener is configured.
     */
    @Test
    public void multipleFilterListeners()
        {
        NamedCache cache = getTestCache();

        TestListener filterListener  = new TestListener(SOME_EVENTS, "FILTER");
        TestListener filterListener2 = new TestListener(1,           "FILTER2");

        Filter filter = new LessFilter(IdentityExtractor.INSTANCE, SOME_EVENTS);
        MapEventFilter eventFilter = new MapEventFilter(MapEventFilter.E_ALL, filter);

        Filter filter2 = new LessFilter(IdentityExtractor.INSTANCE, 1);
        MapEventFilter eventFilter2 = new MapEventFilter(MapEventFilter.E_ALL, filter2);

        cache.addMapListener(filterListener,  eventFilter, false);
        cache.addMapListener(filterListener2, eventFilter2, false);

        // wait for event that reports listeners have been configured.
        waitForEvents(cache);

        // check how many were received
        assertEquals(filterListener.getInsertCount(), filterListener.getInsertExpected());
        assertEquals(filterListener.getUpdateCount(), filterListener.getUpdateExpected());
        assertEquals(filterListener.getDeleteCount(), filterListener.getDeleteExpected());

        assertEquals(filterListener2.getInsertCount(), filterListener2.getInsertExpected());
        assertEquals(filterListener2.getUpdateCount(), filterListener2.getUpdateExpected());
        assertEquals(filterListener2.getDeleteCount(), filterListener2.getDeleteExpected());

        cache.removeMapListener(filterListener,  eventFilter);
        cache.removeMapListener(filterListener2, eventFilter2);
        }

    /**
     * Test events returned when one key listener and one filter listener are configured.
     */
    @Test
    public void oneKeyOneFilterListeners()
        {
        NamedCache cache = getTestCache();

        TestListener keyListener    = new TestListener(1,           "KEY");
        TestListener filterListener = new TestListener(SOME_EVENTS, "FILTER");

        Filter filter = new LessFilter(IdentityExtractor.INSTANCE, SOME_EVENTS);
        MapEventFilter eventFilter = new MapEventFilter(MapEventFilter.E_ALL, filter);

        cache.addMapListener(keyListener,    "TestKey1",  false);
        cache.addMapListener(filterListener, eventFilter, false);

        // wait for event that reports listeners have been configured.
        waitForEvents(cache);

        // check how many were received
        assertEquals(keyListener.getInsertCount(), keyListener.getInsertExpected());
        assertEquals(keyListener.getUpdateCount(), keyListener.getUpdateExpected());
        assertEquals(keyListener.getDeleteCount(), keyListener.getDeleteExpected());

        assertEquals(filterListener.getInsertCount(), filterListener.getInsertExpected());
        assertEquals(filterListener.getUpdateCount(), filterListener.getUpdateExpected());
        assertEquals(filterListener.getDeleteCount(), filterListener.getDeleteExpected());

        cache.removeMapListener(keyListener);
        cache.removeMapListener(filterListener, eventFilter);
        }

    /**
     * Test events returned when one key listener and multiple filter listeners are configured.
     */
    @Test
    public void oneKeyMultipleFilterListeners()
        {
        NamedCache cache = getTestCache();

        TestListener keyListener     = new TestListener(1,           "KEY");
        TestListener filterListener  = new TestListener(SOME_EVENTS, "FILTER");
        TestListener filterListener2 = new TestListener(1,           "FILTER2");

        Filter filter  = new LessFilter(IdentityExtractor.INSTANCE, SOME_EVENTS);
        Filter filter2 = new LessFilter(IdentityExtractor.INSTANCE, 1);

        MapEventFilter eventFilter  = new MapEventFilter(MapEventFilter.E_ALL, filter);
        MapEventFilter eventFilter2 = new MapEventFilter(MapEventFilter.E_ALL, filter2);

        cache.addMapListener(keyListener,     "TestKey1",   false);
        cache.addMapListener(filterListener,  eventFilter,  false);
        cache.addMapListener(filterListener2, eventFilter2, false);

        // ait for event that reports listeners have been configured.
        waitForEvents(cache);

        // check how many were received
        assertEquals(keyListener.getInsertCount(), keyListener.getInsertExpected());
        assertEquals(keyListener.getUpdateCount(), keyListener.getUpdateExpected());
        assertEquals(keyListener.getDeleteCount(), keyListener.getDeleteExpected());

        assertEquals(filterListener.getInsertCount(), filterListener.getInsertExpected());
        assertEquals(filterListener.getUpdateCount(), filterListener.getUpdateExpected());
        assertEquals(filterListener.getDeleteCount(), filterListener.getDeleteExpected());

        assertEquals(filterListener2.getInsertCount(), filterListener2.getInsertExpected());
        assertEquals(filterListener2.getUpdateCount(), filterListener2.getUpdateExpected());
        assertEquals(filterListener2.getDeleteCount(), filterListener2.getDeleteExpected());

        cache.removeMapListener(keyListener);
        cache.removeMapListener(filterListener,  eventFilter);
        cache.removeMapListener(filterListener2, eventFilter2);
        }

    /**
     * Test events returned when multiple key listeners and one filter listener are configured.
     */
    @Test
    public void multipleKeyOneFilterListeners()
        {
        NamedCache cache = getTestCache();

        TestListener keyListener    = new TestListener(1,           "KEY");
        TestListener keyListener2   = new TestListener(1,           "KEY2");
        TestListener filterListener = new TestListener(SOME_EVENTS, "FILTER");

        Filter filter = new LessFilter(IdentityExtractor.INSTANCE, SOME_EVENTS);

        MapEventFilter eventFilter = new MapEventFilter(MapEventFilter.E_ALL, filter);

        cache.addMapListener(keyListener,    "TestKey1",  false);
        cache.addMapListener(keyListener2,   "TestKey2",  false);
        cache.addMapListener(filterListener, eventFilter, false);

        // wait for event that reports listeners have been configured.
        waitForEvents(cache);

        // check how many were received
        assertEquals(keyListener.getInsertCount(), keyListener.getInsertExpected());
        assertEquals(keyListener.getUpdateCount(), keyListener.getUpdateExpected());
        assertEquals(keyListener.getDeleteCount(), keyListener.getDeleteExpected());

        assertEquals(keyListener2.getInsertCount(), keyListener2.getInsertExpected());
        assertEquals(keyListener2.getUpdateCount(), keyListener2.getUpdateExpected());
        assertEquals(keyListener2.getDeleteCount(), keyListener2.getDeleteExpected());

        assertEquals(filterListener.getInsertCount(), filterListener.getInsertExpected());
        assertEquals(filterListener.getUpdateCount(), filterListener.getUpdateExpected());
        assertEquals(filterListener.getDeleteCount(), filterListener.getDeleteExpected());

        cache.removeMapListener(keyListener);
        cache.removeMapListener(keyListener2);
        cache.removeMapListener(filterListener, eventFilter);
        }

    /**
     * Test events returned when multiple key and filter listeners are configured.
     */
    @Test
    public void multipleKeyMultipleFilterListeners()
        {
        NamedCache cache = getTestCache();

        TestListener keyListener     = new TestListener(1,           "KEY");
        TestListener keyListener2    = new TestListener(1,           "KEY2");
        TestListener filterListener  = new TestListener(SOME_EVENTS, "FILTER");
        TestListener filterListener2 = new TestListener(1,           "FILTER2");

        Filter filter = new LessFilter(IdentityExtractor.INSTANCE, SOME_EVENTS);

        MapEventFilter eventFilter = new MapEventFilter(MapEventFilter.E_ALL, filter);

        Filter filter2 = new LessFilter(IdentityExtractor.INSTANCE, 1);

        MapEventFilter eventFilter2 = new MapEventFilter(MapEventFilter.E_ALL, filter2);

        cache.addMapListener(keyListener,     "TestKey1",   false);
        cache.addMapListener(keyListener2,    "TestKey2",   false);
        cache.addMapListener(filterListener,  eventFilter,  false);
        cache.addMapListener(filterListener2, eventFilter2, false);

        // wait for event that reports listeners have been configured.
        waitForEvents(cache);

        // check how many were received
        assertEquals(keyListener.getInsertCount(), keyListener.getInsertExpected());
        assertEquals(keyListener.getUpdateCount(), keyListener.getUpdateExpected());
        assertEquals(keyListener.getDeleteCount(), keyListener.getDeleteExpected());

        assertEquals(keyListener2.getInsertCount(), keyListener2.getInsertExpected());
        assertEquals(keyListener2.getUpdateCount(), keyListener2.getUpdateExpected());
        assertEquals(keyListener2.getDeleteCount(), keyListener2.getDeleteExpected());

        assertEquals(filterListener.getInsertCount(), filterListener.getInsertExpected());
        assertEquals(filterListener.getUpdateCount(), filterListener.getUpdateExpected());
        assertEquals(filterListener.getDeleteCount(), filterListener.getDeleteExpected());

        assertEquals(filterListener2.getInsertCount(), filterListener2.getInsertExpected());
        assertEquals(filterListener2.getUpdateCount(), filterListener2.getUpdateExpected());
        assertEquals(filterListener2.getDeleteCount(), filterListener2.getDeleteExpected());

        cache.removeMapListener(keyListener);
        cache.removeMapListener(keyListener2);
        cache.removeMapListener(filterListener,  eventFilter);
        cache.removeMapListener(filterListener2, eventFilter2);
        }

    @Test
    public void testCoh11015()
        {
        NamedCache cache = getTestCache();

        cache.put(1, new TestObject(1, "AAAAA"));

        TestListener listener1 = new TestListener(2, "COH-11015; Listener 1");
        Filter       filter1   = new EqualsFilter(new PofExtractor(String.class, 1), "AAAAA");
        Filter       eventFilter1 = new MapEventFilter(MapEventFilter.E_ALL, filter1);
        cache.addMapListener(listener1, eventFilter1, false);

        cache.put(1, new TestObject(1, "AAAAA"));

//        long cTimeout = System.currentTimeMillis() + 30000;
//        while (listener1.getUpdateCount() < 1
//                && cTimeout > System.currentTimeMillis())
//            {
//            sleep(250);
//            }
//        assertEquals(listener1.getUpdateCount(), 1);
        Eventually.assertThat(invoking(listener1).getUpdateCount(), is(1));

        TestListener listener2 = new TestListener(1, "COH-11015; Listener 2");
        Filter       filter2   = new EqualsFilter(new PofExtractor(String.class, 1), "AAAAA");
        Filter       eventFilter2 = new MapEventFilter(MapEventFilter.E_ALL, filter2);
        cache.addMapListener(listener2, eventFilter2, false);

        cache.put(1, new TestObject(1, "AAAAA"));

//        cTimeout = System.currentTimeMillis() + 30000;
//        while (listener1.getUpdateCount() < 2
//                && cTimeout > System.currentTimeMillis())
//            {
//            sleep(250);
//            }
        Eventually.assertThat(invoking(listener1).getUpdateCount(), is(2));

        assertEquals(listener1.getUpdateCount(), 2);
        assertEquals(listener2.getUpdateCount(), 1);

        cache.removeMapListener(listener1, eventFilter1);
        cache.removeMapListener(listener2, eventFilter2);
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Return the named cache used in this test.
     *
     * @return a named cache
     */
    protected NamedCache getTestCache()
        {
        NamedCache cache = getNamedCache(AbstractExtendTests.CACHE_DIST_EXTEND_DIRECT);
        cache.clear();
        assertTrue(cache.isEmpty());
        return cache;
        }

    /**
     * Wait to receive a specific event or a timeout, whichever comes first.
     *
     * @param cache         Cache in which to add data that will cause
     *                      generation of the event
     */
    protected void waitForEvents(NamedCache cache)
        {
        WaitListener waitListener = new WaitListener("WAIT");
        cache.addMapListener(waitListener, "WaitKey", false);
        waitListener.startWait();
        cache.put("WaitKey", SOME_EVENTS + 1);
        Eventually.assertThat(invoking(waitListener).getEventReceived(), is(true));
        cache.removeMapListener(waitListener);

        // generate events
        generateEvents(cache);

        waitListener.reset();
        cache.addMapListener(waitListener, "WaitKey", false);
        waitListener.startWait();
        cache.put("WaitKey", SOME_EVENTS + 1);
        Eventually.assertThat(invoking(waitListener).getEventReceived(), is(true));
        cache.removeMapListener(waitListener);
        }

    /**
     * Add data to the cache, so as to generate events.
     *
     * @param cache         Cache in which to add data that will cause
     *                      generation of events
     */
    protected void generateEvents(NamedCache cache)
        {
        // insert events
        for (int i = 0; i < SOME_DATA; i++)
            {
            cache.put("TestKey" + i, i);
            }

        // update events
        for (int i = 0; i < SOME_DATA; i++)
            {
            cache.put("TestKey" + i, i);
            }

        // delete events
        for (int i = 0; i < SOME_DATA; i++)
            {
            cache.remove("TestKey" + i);
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * Number of events
     */
    public static int SOME_EVENTS = 5;

    /**
     * Number of data items to put in cache
     */
    public static int SOME_DATA = 10;

    // ----- inner class: TestListener --------------------------------------

    /**
     * Custom listener that keeps track of how many events are received by
     * this listener during a test.
     */
    public class TestListener implements MapListener
        {

        /**
         * Constructor for listener.
         *
         * @param expected  number of events expected to be received
         * @param listener  name of this listener
         */
        public TestListener(int expected, String listener)
            {
            this.m_cExpected = expected;
            this.m_cUpdates  = 0;
            this.m_cInserts  = 0;
            this.m_cDeletes  = 0;
            this.m_sListener = listener;
            }

        /**
         * Receive update events.
         *
         * @param evt  Received event
         */
        @Override
        public void entryUpdated(MapEvent evt)
            {
            m_cUpdates++;
            }

        /**
         * Receive insert events.
         *
         * @param evt  Received event
         */
        @Override
        public void entryInserted(MapEvent evt)
            {
            m_cInserts++;
            }

        /**
         * Receive delete events.
         *
         * @param evt  Received event
         */
        @Override
        public void entryDeleted(MapEvent evt)
            {
            m_cDeletes++;
            }

        /**
         * Return number of update events received.
         *
         * @return  number of events received
         */
        public int getUpdateCount()
            {
            return m_cUpdates;
            }

        /**
         * Return number of insert events received.
         *
         * @return  number of events received
         */
        public int getInsertCount()
            {
            return m_cInserts;
            }

        /**
         * Return number of delete events received.
         *
         * @return  number of events received
         */
        public int getDeleteCount()
            {
            return m_cDeletes;
            }

        /**
         * Return number of update events expected.
         *
         * @return  number of events expected
         */
        public int getUpdateExpected()
            {
            return m_cExpected;
            }

        /**
         * Return number of insert events expected.
         *
         * @return  number of events expected
         */
        public int getInsertExpected()
            {
            return m_cExpected;
            }

        /**
         * Return number of delete events expected.
         *
         * @return  number of events expected
         */
        public int getDeleteExpected()
            {
            return m_cExpected;
            }

        // ----- data members -----------------------------------------------

        /**
         * Number of insert events received
         */
        protected int m_cInserts;

        /**
         * Number of update events received
         */
        protected int m_cUpdates;

        /**
         * Number of delete events received
         */
        protected int m_cDeletes;

        /**
         * Number of events expected
         */
        protected int m_cExpected;

        /**
         * Which listener is this, KEY or FILTER
         */
        protected String m_sListener;
        }

    // ----- inner class: WaitListener --------------------------------------

    /**
     * Custom listener that waits until the listeners have been added to the
     * cache.  Stops waiting when it receives its event or it times out,
     * whichever comes first.
     */
    public class WaitListener implements MapListener
        {

        /**
         * Constructor for the listener.
         *
         * @param listener  name of this listener
         */
        public WaitListener(String listener)
            {
            this.m_sListener      = listener;
            this.m_fEventReceived = false;
            }

        /**
         * Receive update event.
         *
         * @param evt  update event received
         */
        @Override
        public void entryUpdated(MapEvent evt)
            {
            m_fEventReceived = true;
            }

        /**
         * Receive insert event.
         *
         * @param evt  insert event received
         */
        @Override
        public void entryInserted(MapEvent evt)
            {
            m_fEventReceived = true;
            }

        /**
         * Receive delete event.
         *
         * @param evt  delete event received
         */
        @Override
        public void entryDeleted(MapEvent evt)
            {
            m_fEventReceived = true;
            }

        /**
         * Has event been received?
         *
         * @return  true if event has been received,
         *          false if not
         */
        public boolean getEventReceived()
            {
            return m_fEventReceived;
            }

        /**
         * Start the listener waiting for the event.
         */
        public void startWait()
            {
            m_cStartTime = System.currentTimeMillis();
            }

        /**
         * Reset the listener.
         */
        public void reset()
            {
            m_cStartTime     = 0L;
            m_fEventReceived = false;
            }

        // ----- data members -----------------------------------------------

        /**
         * Events received
         */
        protected boolean m_fEventReceived;

        /**
         * Start time
         */
        protected long m_cStartTime;

        /**
         * Which listener is this, WAIT
         */
        protected String m_sListener;
        }
    }
