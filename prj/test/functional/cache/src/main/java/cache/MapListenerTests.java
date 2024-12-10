/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package cache;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.coherence.testing.AbstractFunctionalTest;

import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.NamedCache;

import com.tangosol.net.cache.CacheEvent;

import com.tangosol.net.events.EventDispatcher;
import com.tangosol.net.events.EventDispatcherAwareInterceptor;
import com.tangosol.net.events.EventInterceptor;
import com.tangosol.net.events.InterceptorRegistry;
import com.tangosol.net.events.partition.TransactionEvent;
import com.tangosol.net.events.partition.cache.EntryEvent;

import com.tangosol.io.BinaryStoreManager;

import com.tangosol.util.AbstractMapListener;
import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.ImmutableArrayList;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;
import com.tangosol.util.MultiplexingMapListener;
import com.tangosol.util.ObservableMap;
import com.tangosol.util.RegistrationBehavior;
import com.tangosol.util.extractor.ConditionalExtractor;
import com.tangosol.util.extractor.IdentityExtractor;
import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.processor.AbstractProcessor;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

/**
* Test class for {@link MapListener}(s).
*
* @author pfm 2012-04-15
*/
public class MapListenerTests
        extends AbstractFunctionalTest
{
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public MapListenerTests()
        {
        super(FILE_CFG_CACHE);
        }

    // ----- test listeners -------------------------------------------------

    /**
     * Initialize the test class.
     */
    @BeforeClass
    public static void _startup()
        {
        // this test requires local storage to be enabled
        System.setProperty("coherence.distributed.localstorage", "true");

        AbstractFunctionalTest._startup();
        }

    /**
     * Test the local listener functionality.
     */
    @Test
    public void testLocalClient()
        {
        generateEvents("local-client");
        checkEvents(true, false);
        }

    /**
     * Test the local backing map listener functionality.
     */
    @Test
    public void testLocalBm()
        {
        generateEvents("local-bm");
        checkEvents(false, true);

        }

    /**
     * Test the distributed cache client listener functionality.
     */
    @Test
    public void testDistClient()
        {
        generateEvents("dist-client-test");
        checkEvents(true, false);
        }

    /**
     * Test the distributed cache backing map listener functionality.
     */
     @Test
     public void testDistBm()
         {
         generateEvents("dist-bm-test");
         checkEvents(false, true);
         BackingMapListener.INSTANCE.assertContext();
         }

   /**
    * Test the distributed cache client and backing map listener functionality.
    */
    @Test
    public void testDistBoth()
        {
        generateEvents("dist-both-test");
        checkEvents(true, true);
        BackingMapListener.INSTANCE.assertContext();
        }

    /**
     * Test the distributed cache backing map listener functionality.
     */
     @Test
     public void testDistBuiltins()
         {
         String sName = "dist-builtins";
         generateEvents(sName);
         checkEvents(false, true);
         BackingMapListener.INSTANCE.asssertBuiltIns(sName);
         }

     /**
      * Test the overflow top level listener functionality.
      */
     @Test
     public void testDistOverflow()
         {
         String sName = "dist-overflow-top";
         generateEvents(sName);
         checkEvents(false, true);
         BackingMapListener.INSTANCE.assertContext();
         }

     /**
      * Test the overflow cache front map listener functionality.
      */
     @Test
     public void testDistOverflowFront()
         {
         String sName = "dist-overflow-front";
         generateEvents(sName);
         checkEvents(false, true);
         BackingMapListener.INSTANCE.assertContext();
         }

     /**
      * Test the overflow cache back map listener functionality.
      */
     @Test
     public void testDistOverflowBack()
         {
         String sName = "dist-overflow-back";
         generateTwoEvents(sName);
         checkEvents(false, true);
         BackingMapListener.INSTANCE.assertContext();
         }

     /**
      * Test the near cache client listener functionality.
      */
     @Test
     public void testNear()
         {
         String sName = "near-client-listener";
         generateEvents(sName);
         checkEvents(true, false);
         }

    /**
     * Test truncate operation with near cache.
     */
    @Test
    public void testNearTruncate()
        {
        NamedCache cache = getNamedCache("near-client-listener-truncate");
        cache.clear();

        clearEvents();
        cache.put(MapListenerTests.KEY, MapListenerTests.VALUE);
        assertEquals(cache.get(MapListenerTests.KEY), MapListenerTests.VALUE);
        checkEvents(true, false);

        clearEvents();
        cache.truncate();
        Eventually.assertThat(invoking(cache).get(MapListenerTests.KEY), nullValue());
        checkEvents(false, false);

        clearEvents();
        cache.put(MapListenerTests.KEY, MapListenerTests.VALUE);
        checkEvents(true, false);
        }

     /**
      * Test the near cache front map listener functionality.
      */
     @Test
     public void testNearFront()
         {
         String sName = "near-front-listener";
         generateEvents(sName);
         checkEvents(true, false);
         }

     /**
      * Test the near cache back map listener functionality.
      */
     @Test
     public void testNearBack()
         {
         String sName = "near-back-listener";
         generateEvents(sName);
         checkEvents(false, true);
         BackingMapListener.INSTANCE.assertContext();
         }

     /**
      * Test the overflow client listener functionality.
      */
     @Test
     public void testOverflowClient()
         {
         String sName = "local-overflow-client";
         generateEvents(sName);
         checkEvents(true, false);
         }

    /**
     * Test the RWBM listener functionality.
     */
    @Test
    public void testRwbm()
        {
        generateEvents("dist-rwbm-test");
        checkEvents(false, true);
        BackingMapListener.INSTANCE.assertContext();
        }

    /**
     * Test the RWBM internal map listener functionality.
     */
     @Test
     public void testRwbmInternal()
       {
       generateEvents("dist-rwbm-internal-test");
       checkEvents(false, true);
       BackingMapListener.INSTANCE.assertContext();
       }

     /**
      * Test the RWBM client and backing map functionality.
      */
     @Test
     public void testRwbmBoth()
         {
         generateEvents("dist-rwbm-both");
         checkEvents(true, true);
         }

     /**
      * Test the RWBM client and internal backing map functionality.
      */
     @Test
     public void testRwbmInternalBoth()
         {
         generateEvents("dist-rwbm-internal-both");
         checkEvents(true, true);
         }


     /**
      * Test the RWBM client and internal backing map functionality.
      */
     @Ignore("Re-enable after fixing either PartitionCache$Storage.onBackingMapEvent or fixing this test")
     @Test
     public void testEDWithListeners()
         {
         MapListener listener = new MultiplexingMapListener()
             {
             protected void onMapEvent(MapEvent evt) {}
             };

         for (int i = 0; i < 4; ++i)
             {
             NamedCache cache = getNamedCache("dist-ed-no-listener");
             boolean fLite = i < 2;

             if ((i & 0x1) == 0)
                 {
                 cache.addMapListener(listener, MapListenerTests.KEY, fLite);
                 }
             else
                 {
                 cache.addMapListener(listener, AlwaysFilter.INSTANCE, fLite);
                 }

             try
                 {
                 // generate an update as old value is only fetched for updates or deletes
                 generateEvents("dist-ed-no-listener");
                 generateEvents("dist-ed-no-listener");
                 }
             finally
                 {
                 cache.destroy();
                 releaseNamedCache(cache);
                 }
             }
         }

     /**
      * Test the RWBM client and internal backing map functionality.
      */
     @Ignore("Re-enable after fixing either PartitionCache$Storage.onBackingMapEvent or fixing this test")
     @Test
     public void testEDWithIndex()
         {
         NamedCache cache = getNamedCache("dist-ed-no-listener");

         ConditionalExtractor extractor = new ConditionalExtractor(
                 AlwaysFilter.INSTANCE, IdentityExtractor.INSTANCE, false);

         cache.addIndex(extractor, false, null);
         try
             {
             // generate an update as old value is only fetched for u
             // pdates or deletes
             generateEvents("dist-ed-no-listener");
             generateEvents("dist-ed-no-listener");
             }
         finally
             {
             cache.removeIndex(extractor);
             cache.destroy();
             releaseNamedCache(cache);
             }
         }

     /**
      * Test the RWBM client and internal backing map functionality.
      */
     @Ignore("Re-enable after fixing either PartitionCache$Storage.onBackingMapEvent or fixing this test")
     @Test
     public void testEDWithInterceptors()
         {
         EventDispatcherAwareInterceptor incptrTxn = new EventDispatcherAwareInterceptor<TransactionEvent>()
             {
             public void introduceEventDispatcher(String sIdentifier, EventDispatcher dispatcher)
                 {
                 dispatcher.addEventInterceptor(sIdentifier, this, new ImmutableArrayList(TransactionEvent.Type.values()).getSet(), false);
                 }

             public void onEvent(TransactionEvent event) { }
             };
         EventDispatcherAwareInterceptor incptrEntry = new EventDispatcherAwareInterceptor<EntryEvent<?, ?>>()
             {
             public void introduceEventDispatcher(String sIdentifier, EventDispatcher dispatcher)
                 {
                 dispatcher.addEventInterceptor(sIdentifier, this, new ImmutableArrayList(EntryEvent.Type.values()).getSet(), false);
                 }

             public void onEvent(EntryEvent event) { }
             };

         for (EventInterceptor incptr : new EventInterceptor[] {incptrTxn, incptrEntry})
            {
            NamedCache          cache    = getNamedCache("dist-ed-no-listener");
            InterceptorRegistry registry = cache.getCacheService().getBackingMapManager()
                    .getCacheFactory().getInterceptorRegistry();
             try
                 {
                 // removing the only interceptor should result in getOldValue not being called

                 // generate an update as old value is only fetched for updates or deletes
                 generateEvents("dist-ed-no-listener");
                 generateEvents("dist-ed-no-listener");

                 registry.registerEventInterceptor("mapTest", incptr, RegistrationBehavior.FAIL);

                 generateEvents("dist-ed-no-listener");
                 }
             finally
                 {
                 registry.unregisterEventInterceptor("mapTest");
                 cache.destroy();
                 releaseNamedCache(cache);
                 }
            }
         }

    /**
     * Test the replicated client listener functionality.
     */
    @Test
    public void testReplicatedClient()
        {
        generateEvents("repl-client-test");
        checkEvents(true, false);
        }

    /**
     * Test the replicated backing map listener functionality.
     */
     @Test
     public void testReplicatedBm()
         {
         generateEvents("repl-bm-test");
         checkEvents(false, true);
         BackingMapListener.INSTANCE.assertContext();
         }

    /**
     * Test the replicated client and backing map listener functionality.
     */
     @Test
     public void testReplicatedBoth()
         {
         generateEvents("repl-both-test");
         checkEvents(true, true);
         BackingMapListener.INSTANCE.assertContext();
         }

    /**
     * Test to ensure that BinaryEntry.remove(true) results in a synthetic delete event.
     */
    @Test
    public void testCOH9787()
        {
        // Test distributed scheme with local backing map
        COH9787Helper("COH9787-local-BM");

        // Test distributed scheme with RWBM
        COH9787Helper("COH9787-RWBM-local-front");
        }

     // ----- helpers -------------------------------------------------------

    /**
     * Clear listener events.
     */
    public void clearEvents()
        {
        ClientListener.INSTANCE.clear();
        BackingMapListener.INSTANCE.clear();
        }

    /**
     * Assert listener events.
     *
     * @param fClient      true if ClientListener event should assert true
     * @param fBackingMap  true if BackingMapListener event should assert true
     */
    public void checkEvents(boolean fClient, boolean fBackingMap)
        {
        ClientListener.INSTANCE.assertEvent(fClient);
        BackingMapListener.INSTANCE.assertEvent(fBackingMap);
        }

    /**
     * Generate listener events.
     */
    protected  void generateEvents(String sName)
        {
        clearEvents();
        NamedCache cache = getNamedCache(sName);
        cache.put(MapListenerTests.KEY, MapListenerTests.VALUE);
        assertEquals(cache.get(MapListenerTests.KEY), MapListenerTests.VALUE);
        }

    /**
     * Generate two listener events.
     */
    protected  void generateTwoEvents(String sName)
        {
        clearEvents();
        NamedCache cache = getNamedCache(sName);
        cache.put(MapListenerTests.KEY, MapListenerTests.VALUE);
        cache.put(MapListenerTests.KEY2, MapListenerTests.VALUE);
        assertEquals(cache.get(MapListenerTests.KEY), MapListenerTests.VALUE);
        assertEquals(cache.get(MapListenerTests.KEY2), MapListenerTests.VALUE);
        }

    /**
     * Given a name, creates a cache on which to invoke a custom processor
     * and ensure that remove(true) results in a synthetic delete being
     *
     * @param sCacheName
     */
    protected  void COH9787Helper(String sCacheName)
        {
        NamedCache              cache = getNamedCache(sCacheName);
        SyntheticDeleteListener sdl   = new SyntheticDeleteListener();

        cache.put(MapListenerTests.KEY, MapListenerTests.VALUE);
        cache.addMapListener(sdl);
        cache.invoke(MapListenerTests.KEY, new COH9787Processor());

        Eventually.assertThat(invoking(sdl).isCorrectEvent(), is(true));
        assertFalse(cache.containsKey(MapListenerTests.KEY));
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
            INSTANCE = this;
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

             if (wasInserted())
                 {
                 assertEquals(evt.getNewValue(), MapListenerTests.VALUE);
                 }
             }

        public static volatile ClientListener INSTANCE = new ClientListener();
        }

    // ----- inner class BackingMapListener ---------------------------------

    /**
     * BackingMapListener class needed to test backing map listener.
     */
    public static class BackingMapListener
            extends Listener
        {
        public BackingMapListener()
            {
            this(null);
            }

        public BackingMapListener(BackingMapManagerContext context)
            {
            super();
            INSTANCE  = this;
            m_context = context;
            }
        public BackingMapListener(BackingMapManagerContext context,
               ClassLoader loader, String sCacheName)
           {
           super();
           INSTANCE     = this;
           m_context    = context;
           m_loader     = loader;
           m_sCacheName = sCacheName;
           }
        protected void clear()
            {
            super.clear();
            m_context = null;
            }

        @Override
        protected void onMapEvent(MapEvent evt)
           {
           super.onMapEvent(evt);

           BackingMapManagerContext context = m_context;
           if (wasInserted() && context != null)
               {
               String s = (String) context.getValueFromInternalConverter().convert(evt.getNewValue());
               assertEquals(s, MapListenerTests.VALUE);
               }
           }

        protected void assertEvent(boolean fExpected)
            {
            assertEquals(wasInserted(),fExpected);
            }

        protected void assertContext()
           {
           assertNotNull(m_context);
           }

        protected void asssertBuiltIns(String sCacheName)
           {
           assertContext();
           assertEquals(m_loader, Base.getContextClassLoader());
           assertEquals(m_sCacheName, sCacheName);
           }

        private static volatile BackingMapManagerContext m_context;

        private static volatile ClassLoader m_loader;

        private static volatile String m_sCacheName;

        public static volatile BackingMapListener INSTANCE = new BackingMapListener();
        }

    // ----- inner class SyntheticDeleteListener ----------------------------

    /**
     * Listener class to test that a synthetic delete occurs.
     */
    public static class SyntheticDeleteListener
            extends AbstractMapListener
        {
        /**
         * Detects whether a synthetic delete has been heard.
         *
         * @return true if the listener was called with a synthetic delete
         */
        public boolean isCorrectEvent()
            {
            return m_fCorrectEvent;
            }

        /**
         * Resets the fCorrectEvent so the instance can be reused.
         */
        public void reset()
            {
            m_fCorrectEvent = false;
            }

        @Override
        public void entryDeleted(MapEvent evt)
            {
            if (evt instanceof CacheEvent && ((CacheEvent) evt).isSynthetic())
                {
                m_fCorrectEvent = true;
                }
            }

        /**
         * Flag to signal whether this listener has heard a synthetic delete.
         */
        private volatile boolean m_fCorrectEvent;
        }

    // ----- inner class SyntheticDeleteListener ----------------------------

    /**
     * Processor class to call remove(true) on a BinaryEntry
     */
    public static class COH9787Processor
            extends AbstractProcessor
        {
        @Override
        public Object process(InvocableMap.Entry entry)
            {
            Object oVal = entry.getValue();
            entry.remove(true);
            return oVal;
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * The file name of the default cache configuration file used by this test.
     */
    public static String FILE_CFG_CACHE = "maplistener-cache-config.xml";

    /**
     * The value put int the cache.
     */
    private final static String VALUE = "One";

    /**
     * The key used for the value.
     */
    private final static String KEY = "1";
    private final static String KEY2 = "2";
}
