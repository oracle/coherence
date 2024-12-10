/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package processor;


import com.oracle.coherence.common.base.Converter;

import com.oracle.coherence.common.base.Blocking;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.net.BackingMapContext;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.NamedCache;
import com.tangosol.net.PartitionedService;

import com.tangosol.net.PriorityTask;
import com.tangosol.net.cache.AbstractCacheStore;
import com.tangosol.net.cache.LocalCache;

import com.tangosol.net.partition.KeyAssociator;
import com.tangosol.net.partition.PartitionAwareBackingMap;
import com.tangosol.net.partition.PartitionSet;

import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.CompositeKey;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.InvocableMap.EntryProcessor;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapIndex;
import com.tangosol.util.MultiplexingMapListener;
import com.tangosol.util.SimpleMapIndex;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.NullImplementation;

import com.tangosol.util.aggregator.Count;

import com.tangosol.util.extractor.IdentityExtractor;
import com.tangosol.util.extractor.IndexAwareExtractor;

import com.tangosol.util.filter.AlwaysFilter;

import com.tangosol.util.processor.*;

import com.oracle.coherence.testing.AbstractRollingRestartTest;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Collections;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.BeforeClass;
import org.junit.AfterClass;


/**
* Tests for index integrity functionality of EntryProcessors.
*/
public class IndexIntegrityTests
        extends AbstractRollingRestartTest
    {
    public IndexIntegrityTests()
        {
        super(s_sCacheConfig);
        }

    /**
    * {@inheritDoc}
    */
    public String getCacheConfigPath()
        {
        return s_sCacheConfig;
        }

    /**
    * {@inheritDoc}
    */
    public String getBuildPath()
        {
        return s_sBuild;
        }

    /**
    * {@inheritDoc}
    */
    public String getProjectName()
        {
        return s_sProject;
        }


    // ----- test lifecycle -------------------------------------------------

    /**
    * Initialize the test class.
    */
    @BeforeClass
    public static void startup()
        {
        _shutdown();
        }

    /**
    * Shutdown the test class.
    */
    @AfterClass
    public static void shutdown()
        {
        }


    // ----- test methods -------------------------------------------------

    /**
    * Run the specified single-server test.
    *
    * @param sTest     the name of the test
    * @param runnable  the test thunk
    */
    protected void doSingleServerTest(String sTest, Runnable runnable)
        {
        try
            {
            System.setProperty("coherence.distributed.localstorage", "true");
            _startup();
            runnable.run();
            }
        finally
            {
            _shutdown();
            }
        }

    /**
    * Run the index stress test with the specified parameters.
    *
    * @param sTest            the name of the test
    * @param sCacheMain       the name of the "main" cache to invoke on
    * @param sCacheAux        the name of the "auxilliary" cache to modify
    * @param extractor        the extractor to use for the index
    * @param keyCreator       the composite key creator
    * @param nServers         the number of servers to run with
    * @param cRollingRestart  the number of rolling restarts to perform
    */
    protected void doStressTest(String sTest, String sCacheMain, String sCacheAux,
            ValueExtractor extractor, CompositeKeyCreator keyCreator,
            int nServers, int cRollingRestart)
        {
        try
            {
            System.setProperty("coherence.distributed.localstorage", "false");
            _startup();
            doStressTest_(sTest, sCacheMain, sCacheAux, extractor, keyCreator, nServers, cRollingRestart);
            }
        finally
            {
            _shutdown();
            }
        }

    /**
    * Run the index stress test with the specified parameters.
    *
    * @param sTest            the name of the test
    * @param sCacheMain       the name of the "main" cache to invoke on
    * @param sCacheAux        the name of the "auxiliary" cache to modify
    * @param extractor        the extractor to use for the index
    * @param keyCreator       the composite key creator
    * @param nServers         the number of servers to run with
    * @param cRollingRestart  the number of rolling restarts to perform
    */
    protected void doStressTest_(String sTest, String sCacheMain, String sCacheAux,
            ValueExtractor extractor, final CompositeKeyCreator keyCreator,
            int nServers, int cRollingRestart)
        {
        boolean       fRollingRestart = cRollingRestart > 0;
        MemberHandler memberHandler   = new MemberHandler(
                CacheFactory.ensureCluster(), sTest,
                /*fExternalKill*/true, /*fGraceful*/false);
        for (int i = 0; i < nServers; i++)
            {
            memberHandler.addServer();
            }

        final EntryProcessor processor = new BackdoorMutatingProcessor(sCacheAux);
        final NamedCache     cache0    = getNamedCache(sCacheMain);
        final NamedCache     cache1    = getNamedCache(sCacheAux);
        final boolean[]      afExiting = fRollingRestart ? new boolean[1] : null;

        DistributedCacheService service = (DistributedCacheService) cache0.getCacheService();
        Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(nServers));

        cache0.addIndex(extractor, false, null);
        if (cache0 != cache1)
            {
            cache1.addIndex(extractor, false, null);
            }

        class LoadThread
            extends Thread
            {
            public void run()
                {
                Random rand = new Random();
                for (int i = 0; afExiting != null ? !afExiting[0] : i < 5000; i++)
                    {
                    Object oKey = Integer.valueOf(rand.nextInt(25));
                    cache0.invoke(keyCreator.getCompositeKey(oKey, cache0), processor);
                    }
                }
            }

        Thread thd0 = new LoadThread();
        Thread thd1 = new LoadThread();
        Thread thd2 = new LoadThread();
        Thread thd3 = new LoadThread();
        Thread thd4 = new LoadThread();

        thd0.start();
        thd1.start();
        thd2.start();
        thd3.start();
        thd4.start();

        if (fRollingRestart)
            {
            doRollingRestart(memberHandler, cRollingRestart, new WaitForNodeSafeRunnable(service));
            Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(nServers));

            afExiting[0] = true;
            }

        try
            {
            thd0.join();
            thd1.join();
            thd2.join();
            thd3.join();
            thd4.join();
            }
        catch (InterruptedException e)
            {
            fail("test was interrupted: " + e);
            }

        assertFalse("Expected changes to " + cache1.getCacheName()
                  + " are not present", cache1.isEmpty());

        validateIndex(cache0);
        if (cache0 != cache1)
            {
            validateIndex(cache1);
            }

        memberHandler.dispose();
        }

    /**
    * Test that mutates the same key on a different cache (backing-map) than
    * the one that the front-door EP is executing on.
    *
    * Test that the user and PK indices remain correct.
    */
    @Test
    public void testSameKeyAccessDiffCache()
        {
        doStressTest("IdxTestSKADC", getCacheName0(), getCacheName1(),
               IdentityExtractor.INSTANCE,
               new CompositeKeyCreator()
                   {
                   public CompositeKey getCompositeKey(Object oKeyNatural, NamedCache cache)
                       {
                       return new CompositeKey(oKeyNatural, oKeyNatural);
                       }
                   },
               /*nServers*/1, /*cRollingRestart*/0);
        }

    /**
    * Test that mutates the same key on a different cache (backing-map) than
    * the one that the front-door EP is executing on.
    *
    * Test that the user and PK indices remain correct.
    */
    @Test
    public void testSameKeyAccessDiffCacheMulti()
        {
        doStressTest("IdxTestSKADCM", getCacheName0(), getCacheName1(),
               IdentityExtractor.INSTANCE,
               new CompositeKeyCreator()
                   {
                   public CompositeKey getCompositeKey(Object oKeyNatural, NamedCache cache)
                       {
                       return new CompositeKey(oKeyNatural, oKeyNatural);
                       }
                   },
               /*nServers*/3, /*cRollingRestart*/0);
        }

    /**
    * Test that mutates the same key on a different cache (backing-map) than
    * the one that the front-door EP is executing on.
    *
    * Test that the user and PK indices remain correct.
    */
    @Test
    public void testSameKeyAccessDiffCacheMultiRollingRestart()
        {
        doStressTest("IdxTestSKADCMRR", getCacheName0(), getCacheName1(),
               IdentityExtractor.INSTANCE,
               new CompositeKeyCreator()
                   {
                   public CompositeKey getCompositeKey(Object oKeyNatural, NamedCache cache)
                       {
                       return new CompositeKey(oKeyNatural, oKeyNatural);
                       }
                   },
               /*nServers*/3, /*cRollingRestart*/5);
        }

    /**
    * Test that mutates the same key on a different cache (backing-map) than
    * the one that the front-door EP is executing on.
    *
    * Test that the user and PK indices remain correct.
    */
    @Test
    public void testCollidingKeyAccessDiffCache()
        {
        doStressTest("IdxTestCKADC", getCacheName0(), getCacheName1(),
               IdentityExtractor.INSTANCE,
               new CompositeKeyCreator()
                   {
                   public CompositeKey getCompositeKey(Object oKeyNatural, NamedCache cache)
                       {
                       return new CompositeKey(
                               oKeyNatural, getSurrogateKey(cache, oKeyNatural, Base.getRandom().nextInt(20)));
                       }
                   },
               /*nServers*/1, /*cRollingRestart*/0);
        }

    /**
    * Test that mutates the same key on a different cache (backing-map) than
    * the one that the front-door EP is executing on.
    *
    * Test that the user and PK indices remain correct.
    */
    @Test
    public void testCollidingKeyAccessDiffCacheMultiRollingRestart()
        {
        doStressTest("IdxTestCKADCMRR", getCacheName0(), getCacheName1(),
               IdentityExtractor.INSTANCE,
               new CompositeKeyCreator()
                   {
                   public CompositeKey getCompositeKey(Object oKeyNatural, NamedCache cache)
                       {
                       return new CompositeKey(
                               oKeyNatural, getSurrogateKey(cache, oKeyNatural, Base.getRandom().nextInt(20)));
                       }
                   },
               /*nServers*/3, /*cRollingRestart*/5);
        }

    /**
    * Test that randomly mutates the same cache (backing-map) that the
    * front-door EP is executing on.
    *
    * Test that the user and PK indices remain correct.
    *
    * Note: this test can only be run in single-server, non-rolling restart mode
    */
    @Test
    public void testRandomAccessSameCache()
        {
        doStressTest("IdxTestRASC", getCacheName0(), getCacheName0(),
               IdentityExtractor.INSTANCE,
               new CompositeKeyCreator()
                   {
                   public CompositeKey getCompositeKey(Object oKeyNatural, NamedCache cache)
                       {
                       return new CompositeKey(oKeyNatural, Base.getRandom().nextInt(20));
                       }
                   },
               /*nServers*/1, /*cRollingRestart*/0);
        }

    /**
    * Test the (unsupported) behaviour of an EP doing direct BM update
    * of the "invoked" key.
    */
    @Test
    public void testBackdoorModificationSameKey()
        {
        testBackdoorModificationSameKey(this);
        }

    protected static void testBackdoorModificationSameKey(final IndexIntegrityTests test)
        {
        class Processor
                extends AbstractProcessor
            {
            public Object process(InvocableMap.Entry entry)
                {
                BinaryEntry binEntry = (BinaryEntry) entry;
                Map         mapBM    = binEntry.getBackingMap();

                // test a direct BM insert, followed by BinaryEntry setValue()
                mapBM.put(binEntry.getBinaryKey(),
                          binEntry.getContext().getValueToInternalConverter().convert(Integer.valueOf(-1)));
                binEntry.setValue(entry.getKey());
                return null;
                }
            }
        test.doSingleServerTest("IdxTestBMSK", new Runnable()
                    {
                    public void run()
                        {
                        NamedCache cache = test.getNamedCache(getCacheName0());
                        cache.addIndex(IdentityExtractor.INSTANCE, false, null);

                        cache.invoke(Integer.valueOf(0), new Processor());

                        validateIndex(cache);
                        }
                    });
        }

    /**
    * Run the specified runnable after setting an entry in the cache given by
    * CacheName0, and simulating the pending expiry of that item.
    *
    * @param runnableOp
    */
    protected void doExpiryOpTest(final Runnable runnableOp)
        {
        doSingleServerTest("IdxTestCoh3710", new Runnable()
                    {
                    public void run()
                        {
                        NamedCache cache = getNamedCache(getCacheName0());
                        Object     oKey  = Integer.valueOf(1);

                        cache.addIndex(IdentityExtractor.INSTANCE, false, null);
                        cache.put(oKey, Integer.valueOf(1));
                        validateIndex(cache);

                        // simulate expiry
                        expireLocalEntry(oKey, cache);

                        runnableOp.run();

                        validateIndex(cache);
                        }
                    });
        }
    /**
    * Regression test for COH-3710
    */
    @Test
    public void testCoh3710()
        {
        doExpiryOpTest(new Runnable()
            {
            public void run() { getNamedCache(getCacheName0()).clear(); }
            });
        }

    /**
    * Regression test for COH-3710
    */
    @Test
    public void testCoh3710_aggregateFilter()
        {
        doExpiryOpTest(new Runnable()
            {
            public void run()
                {
                Object oResult = getNamedCache(getCacheName0()).aggregate(
                        AlwaysFilter.INSTANCE, new Count());
                assertEquals(Integer.valueOf(0), oResult);
                }
            });
        }

    /**
    * Regression test for COH-3710
    */
    @Test
    public void testCoh3710_aggregateKeys()
        {
        doExpiryOpTest(new Runnable()
            {
            public void run()
                {
                Object oResult = getNamedCache(getCacheName0()).aggregate(
                        Collections.singleton(Integer.valueOf(1)), new Count());
                assertEquals(Integer.valueOf(0), oResult);
                }
            });
        }

    /**
    * Regression test for COH-3710
    */
    @Test
    public void testCoh3710_containsKey()
        {
        doExpiryOpTest(new Runnable()
            {
            public void run()
                {
                assertFalse(getNamedCache(getCacheName0()).containsKey(Integer.valueOf(1)));
                }
            });
        }

    /**
    * Regression test for COH-3710
    */
    @Test
    public void testCoh3710_containsValue()
        {
        doExpiryOpTest(new Runnable()
            {
            public void run()
                {
                assertFalse(getNamedCache(getCacheName0()).containsValue(Integer.valueOf(1)));
                }
            });
        }

    /**
    * Regression test for COH-3710
    */
    @Test
    public void testCoh3710_entrySetFilter()
        {
        doExpiryOpTest(new Runnable()
            {
            public void run()
                {
                NamedCache cache = getNamedCache(getCacheName0());

                Iterator iter = cache.entrySet(AlwaysFilter.INSTANCE).iterator();
                validateIndex(cache);

                assertFalse(iter.hasNext());
                }
            });
        }

    /**
    * Regression test for COH-3710
    */
    @Test
    public void testCoh3710_entrySetIterator()
        {
        doExpiryOpTest(new Runnable()
            {
            public void run()
                {
                NamedCache cache = getNamedCache(getCacheName0());

                Iterator iter = cache.entrySet().iterator();
                assertFalse(iter.hasNext());
                validateIndex(cache);
                }
            });
        }

    /**
    * Regression test for COH-3710
    */
    @Test
    public void testCoh3710_get()
        {
        doExpiryOpTest(new Runnable()
            {
            public void run()
                {
                assertNull(getNamedCache(getCacheName0()).get(Integer.valueOf(1)));
                }
            });
        }

    /**
    * Regression test for COH-3710
    */
    @Test
    public void testCoh3710_getAll()
        {
        doExpiryOpTest(new Runnable()
            {
            public void run()
                {
                assertEquals(NullImplementation.getMap(),
                    getNamedCache(getCacheName0()).getAll(
                        Collections.singleton(Integer.valueOf(1))));
                }
            });
        }

    /**
    * Regression test for COH-3710
    */
    @Test
    public void testCoh3710_isEmpty()
        {
        doExpiryOpTest(new Runnable()
            {
            public void run()
                {
                assertTrue(getNamedCache(getCacheName0()).isEmpty());
                }
            });
        }

    /**
    * Regression test for COH-3710
    */
    @Test
    public void testCoh3710_keySetContains()
        {
        doExpiryOpTest(new Runnable()
            {
            public void run()
                {
                NamedCache cache = getNamedCache(getCacheName0());

                assertFalse(cache.keySet().contains(Integer.valueOf(1)));
                validateIndex(cache);
                }
            });
        }

    /**
    * Regression test for COH-3710
    */
    @Test
    public void testCoh3710_keySetContainsAll()
        {
        doExpiryOpTest(new Runnable()
            {
            public void run()
                {
                NamedCache cache = getNamedCache(getCacheName0());

                assertFalse(cache.keySet().containsAll(
                        Collections.singleton(Integer.valueOf(1))));
                validateIndex(cache);
                }
            });
        }

    /**
    * Regression test for COH-3710
    */
    @Test
    public void testCoh3710_size()
        {
        doExpiryOpTest(new Runnable()
            {
            public void run()
                {
                assertEquals(0, getNamedCache(getCacheName0()).size());
                }
            });
        }

    /**
    * Regression test for COH-3733
    */
    @Test
    public void testCoh3733()
        {
        doSingleServerTest("IdxTestCoh3733", new Runnable()
                    {
                    public void run()
                        {
                        NamedCache cache = getNamedCache("coh3733");

                        cache.addIndex(new IndexAwareExtractor()
                            {
                            public MapIndex createIndex(boolean fOrdered, Comparator comparator, Map mapIndex,
                                    BackingMapContext ctx)
                                {
                                MapIndex index = new SimpleMapIndex(this, false, null, ctx)
                                    {
                                    protected void removeInverseMapping(Map mapIndex, Object oIxValue, Object oKey)
                                        {
                                        Set setKeys = (Set) mapIndex.get(oIxValue);

                                        assertTrue("Attempted to remove non-existent index entry",
                                                   setKeys != null && setKeys.contains(oKey));
                                        super.removeInverseMapping(mapIndex, oIxValue, oKey);
                                        }
                                    };
                                mapIndex.put(this, index);
                                return index;
                                }

                            public MapIndex destroyIndex(Map mapIndex)
                                {
                                return (MapIndex) mapIndex.remove(this);
                                }

                            public Object extract(Object oTarget)
                                {
                                return oTarget;
                                }
                            }, false, null);
                        cache.addMapListener(new MultiplexingMapListener()
                            {
                            protected void onMapEvent(MapEvent evt)
                                {
                                fail("Observed unexpected event: " + evt);
                                }
                            });

                        cache.remove("non-existent-key");

                        validateIndex(cache);
                        }
                    });
        }


    // ----- inner class: IndexVerifierProcessor --------------------------

    /**
    * EntryProcessor to validate the user and partitioned indices.
    */
    @SuppressWarnings("rawtypes")
    public static class IndexVerifierProcessor
            extends AbstractProcessor
        {
        public IndexVerifierProcessor(String sCacheName)
            {
            m_sCacheName = sCacheName;
            }

        public Object process(InvocableMap.Entry entry)
            {
            BinaryEntry              binEntry = (BinaryEntry) entry;
            BackingMapManagerContext ctx      = binEntry.getContext();
            Map                      mapBM    = binEntry.getBackingMap();
            Converter                convUp   = ctx.getValueFromInternalConverter();
            Converter                convDown = ctx.getValueToInternalConverter();
            MapIndex                 index    = findIndex(ctx, m_sCacheName);
            Map                      keyIndex = findKeyIndex(ctx, m_sCacheName);
            StringBuilder            sb       = new StringBuilder();

            for (Iterator iter = mapBM.entrySet().iterator(); iter.hasNext();)
                {
                Map.Entry entryBM = (Map.Entry) iter.next();
                Binary    binKey  = (Binary) entryBM.getKey();
                Binary    binVal  = (Binary) entryBM.getValue();
                Object    oVal    = convUp.convert(binVal);

                // check that the entry matches in the fwd index
                if (!Base.equals(index.get(binKey), oVal))
                    {
                    Object oValueBad   = index.get(binKey);
                    Binary binValueBad = (Binary) convDown.convert(oValueBad);

                    sb.append("BM contains mapping ")
                        .append(binKey)
                        .append("=>")
                        .append(binVal)
                        .append(" (")
                        .append(oVal)
                        .append(") ; but forward index maps to ")
                        .append(binValueBad)
                        .append(" (").append(index.get(binKey)).append(")").append('\n');
                    }

                // check that an entry exists in the reverse index
                Set setKeys = (Set) index.getIndexContents().get(oVal);
                if (setKeys == null || !setKeys.contains(binKey))
                    {
                    sb.append("BM contains mapping ")
                        .append(binKey)
                        .append("=>")
                        .append(binVal)
                        .append("(")
                        .append(oVal)
                        .append("); but reverse index does not contain the key")
                        .append('\n');
                    }

                // check that the entry exists in the partitioned-key index
                if (!keyIndex.containsKey(binKey))
                    {
                    sb.append("BM contains mapping ")
                        .append(binKey)
                        .append("=>")
                        .append(binVal)
                        .append("(")
                        .append(oVal)
                        .append("); but the key-index does not contain the key")
                        .append('\n');
                    }
                }

            if (keyIndex.size() > mapBM.size())
                {
                HashSet setDiff = new HashSet(keyIndex.keySet());
                setDiff.removeAll(mapBM.keySet());
                sb.append("Partitioned key index contains entries that do not exist ")
                    .append("in the backing-map: ")
                    .append(setDiff)
                    .append('\n');
                }

            Map mapReverse = index.getIndexContents();
            int cKeys      = 0;
            for (Iterator iter = mapReverse.entrySet().iterator(); iter.hasNext();)
                {
                Map.Entry entryIdx = (Map.Entry) iter.next();
                Set       setKeys  = (Set) entryIdx.getValue();
                for (Iterator iterKeys = setKeys.iterator(); iterKeys.hasNext(); )
                    {
                    Binary binKey     = (Binary) iterKeys.next();
                    Object oValueFwd  = index.get(binKey);
                    Object oValueIdx  = entryIdx.getKey();
                    Binary binValueBM = (Binary) mapBM.get(ExternalizableHelper.getUndecorated(binKey));

                    if (binValueBM == null && oValueFwd == MapIndex.NO_VALUE)
                        {
                        cKeys--;
                        continue;
                        }

                    if (!Base.equals(convUp.convert(binValueBM), oValueIdx) ||
                        !Base.equals(oValueFwd, oValueIdx))
                        {
                        sb.append("Reverse index contains mapping ")
                            .append(binKey)
                            .append("=>")
                            .append(convDown.convert(oValueIdx))
                            .append("; forward index contains mapping to ")
                            .append(oValueFwd == MapIndex.NO_VALUE ? "NO_VALUE" : convDown.convert(oValueFwd))
                            .append(" but BM contains mapping to ")
                            .append(binValueBM)
                            .append('\n');
                        }
                    }
                cKeys += setKeys.size();
                }
            if (cKeys != mapBM.size())
                {
                sb.append("Reverse index contains ")
                    .append(cKeys)
                    .append(" entries, but only ")
                    .append(mapBM.size())
                    .append(" entries exist in the backing map")
                    .append('\n');
                }

            return sb.length() == 0 ? null : sb.toString();
            }

        String m_sCacheName;
        }


    // ----- inner class: BackdoorMutatingProcessor -----------------------

    /**
    *
    *
    */
    public static class BackdoorMutatingProcessor
            extends AbstractProcessor
            implements ExternalizableLite, PortableObject, PriorityTask
        {
        public BackdoorMutatingProcessor()
            {
            }

        public BackdoorMutatingProcessor(String sCacheName)
            {
            m_sCacheName = sCacheName;
            }

        public Object process(InvocableMap.Entry entry)
            {
            BinaryEntry              binEntry = (BinaryEntry) entry;
            BackingMapManagerContext ctx      = binEntry.getContext();
            Map                      map      = ctx.getBackingMap(m_sCacheName);
            CompositeKey             key      = (CompositeKey) binEntry.getKey();

            Binary binKeyOther = (Binary) ctx.getKeyToInternalConverter().convert(key.getSecondaryKey());
            switch (Base.getRandom().nextInt(5))
                {
                case 0:
                    map.remove(binKeyOther);
                    break;

                default:
                    map.put(binKeyOther,
                            ctx.getValueToInternalConverter().convert(Base.getRandom().nextInt(500)));
                    break;
                }

            binEntry.setValue(Base.getRandom().nextInt(500));
            return null;
            }

        @Override
        public long getExecutionTimeoutMillis()
            {
            return PriorityTask.TIMEOUT_DEFAULT;
            }

        @Override
        public long getRequestTimeoutMillis()
            {
            return PriorityTask.TIMEOUT_DEFAULT;
            }

        @Override
        public int getSchedulingPriority()
            {
            return PriorityTask.SCHEDULE_STANDARD;
            }

        @Override
        public void runCanceled(boolean fAbandoned)
            {
            }

        @Override
        public void readExternal(DataInput in) throws IOException
            {
            m_sCacheName = ExternalizableHelper.readSafeUTF(in);
            }

        @Override
        public void writeExternal(DataOutput out) throws IOException
            {
            ExternalizableHelper.writeSafeUTF(out, m_sCacheName);
            }

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            m_sCacheName = in.readString(0);
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeString(0, m_sCacheName);
            }

        String m_sCacheName;
        }


    // ----- test helpers -------------------------------------------------

    /**
    * Return the MapIndex on the specified cache.
    *
    * @return the MapIndex on the specified cache
    */
    public static MapIndex findIndex(
            BackingMapManagerContext context, String sCacheName)
        {
        try
            {
            PartitionSet parts = ((PartitionedCache) context.getCacheService()).getIndexPendingPartitions();
            if (!parts.isEmpty())
                {
                Base.sleep(100);
                if (!parts.isEmpty())
                    {
                    throw new IndexNotReadyException("Index is not ready!");
                    }
                }

            Object oStorage = ClassHelper.invoke(
                context.getCacheService(), "getStorage", new Object[] { sCacheName });
            Map    mapIndex = (Map) ClassHelper.invoke(
                oStorage, "getIndexMap", ClassHelper.VOID);

            return (MapIndex) mapIndex.values().iterator().next();
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    /**
    * Return the partitioned-key index on the specified cache.
    *
    * @return the partitioned-key index on the specified cache
    */
    public static PartitionAwareBackingMap findKeyIndex(
            BackingMapManagerContext context, String sCacheName)
        {
        try
            {
            Object oStorage = ClassHelper.invoke(
                context.getCacheService(), "getStorage", new Object[] { sCacheName });
            return (PartitionAwareBackingMap) ClassHelper.invoke(
                oStorage, "getPartitionedKeyIndex", ClassHelper.VOID);
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    /**
    * Return the name of a named cache.
    *
    * @return the name of a named cache
    */
    public static String getCacheName0()
        {
        return s_sCacheName0;
        }

    /**
    * Return the name of a named cache.
    *
    * @return the name of a named cache
    */
    public static String getCacheName1()
        {
        return s_sCacheName1;
        }

    /**
    * Validate that the index on the specified cache is correct.
    *
    * @param cache  the cache to validate the index for
    */
    protected static void validateIndex(NamedCache cache)
        {
        while (true)
            {
            try
                {
                String sErr = (String) cache.invoke(
                    "dummy-key", new IndexVerifierProcessor(cache.getCacheName()));
                assertNull(sErr, sErr);
                break;
                }
            catch (RuntimeException e)
                {
                if (e.getCause() instanceof IndexNotReadyException)
                    {
                    System.out.println("Asynchronous index update is not ready, try again");
                    Base.sleep(100);
                    continue;
                    }
                throw e;
                }
            }
        }

    /**
    * Return a surrogate key for the specified "root" key.
    *
    * @param cache  the cache to return a surrogate key for
    * @param oKey   the "root" key
    * @param nSalt  the "salt"; an input used to generate a surrogate key
    *
    * @return a surrogate key
    */
    protected static Object getSurrogateKey(NamedCache cache, Object oKey, int nSalt)
        {
        PartitionedService service = (PartitionedService) cache.getCacheService();
        Associator         assoc   = (Associator) service.getKeyAssociator();

        return assoc.getSurrogateKey(oKey, nSalt);
        }


    /**
    * Simulate the expiry of a the specified entry in the specified cache.
    * Note: the specified entry must be held locally
    *
    * @param oKey   the key to expire
    * @param cache  the cache to expire the key from
    */
    protected static void expireLocalEntry(Object oKey, NamedCache cache)
        {
        CacheService             service = cache.getCacheService();
        BackingMapManagerContext ctx     = service.getBackingMapManager().getContext();
        LocalCache               mapBM   = (LocalCache) ctx.getBackingMap(getCacheName0());
        LocalCache.Entry         entry   = (LocalCache.Entry)
                mapBM.getEntry(ctx.getKeyToInternalConverter().convert(oKey));

        entry.setExpiryMillis(Base.getSafeTimeMillis() - 5000);

        try
            {
            // Expiry has a quarter-second granularity; pause this thread to
            // ensure that we don't optimize over the expiry check.  See
            // OldCache.m_lNextFlush.
            Blocking.sleep(0x200L);
            }
        catch (InterruptedException e)
            {
            }
        }

    // ----- debugging ----------------------------------------------------

    public static void main(String[] args)
        {
        try
            {
            new IndexIntegrityTests().testRandomAccessSameCache();
            }
        catch (Throwable t)
            {
            Base.out(t.getMessage());
            }
        }

    // ----- inner interface: CompositeKeyCreator -------------------------

    public static interface CompositeKeyCreator
        {
        public CompositeKey getCompositeKey(Object oKeyNatural, NamedCache cache);
        }


    // ----- inner class: Associator --------------------------------------

    public static class Associator
            implements KeyAssociator
        {
        public void init(PartitionedService service)
            {
            m_nPartitionCount = service.getPartitionCount();
            }

        public Object getAssociatedKey(Object oKey)
            {
            if (oKey instanceof Integer)
                {
                int nKey = ((Integer) oKey).intValue();
                return Integer.valueOf(nKey % m_nPartitionCount);
                }
            else if (oKey instanceof CompositeKey)
                {
                return ((CompositeKey) oKey).getPrimaryKey();
                }

            // who knows?
            return oKey;
            }

        /**
        * Return a key that will be "associated" to the specified base key.
        *
        * @param oKey   the base key
        * @param nSalt  the "salt"
        *
        * @return a key that will be "associated" to the specified base key
        */
        public Object getSurrogateKey(Object oKey, int nSalt)
            {
            int nKey = ((Integer) oKey).intValue();
            return Integer.valueOf(nKey + m_nPartitionCount * (nSalt + 1));
            }

        int m_nPartitionCount;
        }


    // ----- inner class: Coh3733CacheStore -------------------------------

    /**
    * CacheStore implementation for Coh3733 test
    */
    public static class Coh3733CacheStore
            extends AbstractCacheStore
        {
        /**
        * {@inheritDoc}
        */
        public Object load(Object oKey)
            {
            return oKey;
            }

        /**
        * {@inheritDoc}
        */
        public void store(Object oKey)
            {
            }

        /**
        * {@inheritDoc}
        */
        public void erase(Object oKey)
            {
            }
        }

    public static class IndexNotReadyException extends RuntimeException
        {
        public IndexNotReadyException() {}

        public IndexNotReadyException(String message)
            {
            super(message);
            }
        }

    // ----- data members and constants -----------------------------------

    public static final String s_sCacheName0  = "index-test0";
    public static final String s_sCacheName1  = "index-test1";

    /**
    * The path to the cache configuration.
    */
    public static final String s_sCacheConfig = "index-cache-config.xml";

    /**
    * The path to the Ant build script.
    */
    public final static String s_sBuild       = "build.xml";

    /**
    * The project name.
    */
    public final static String s_sProject     = "processor";
    }
