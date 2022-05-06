/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package trigger;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.tangosol.net.Cluster;
import com.tangosol.net.DistributedCacheService;

import com.oracle.coherence.testing.AbstractRollingRestartTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.oracle.coherence.common.base.Converter;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.PartitionedService;
import com.tangosol.net.partition.KeyAssociator;
import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.MapTrigger;
import com.tangosol.util.MapTriggerListener;
import com.tangosol.util.InvocableMap.EntryProcessor;
import com.tangosol.util.extractor.IdentityExtractor;
import com.tangosol.util.filter.FilterTrigger;
import com.tangosol.util.filter.LessFilter;
import com.tangosol.util.processor.AbstractProcessor;

import org.junit.rules.TestName;

/**
* A collection of functional tests for the MapTrigger LiteTxn functionality that
* use the "dist-std-test-1 and dist-std-test-1" caches.
*
* @author bb 2012.03.07
*/
public class LiteTxnTriggerTests
    extends AbstractRollingRestartTest
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public LiteTxnTriggerTests()
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

    @Before
    public void setUp()
        {
        Cluster cluster = CacheFactory.ensureCluster();
        String sMethod  = m_testName.getMethodName();
        String sName    = sMethod.length() <= 20 ? sMethod : sMethod.substring(sMethod.length() - 20);

        memberHandler   = new MemberHandler(cluster, "LiteTxn-" + sName,
                /*fExternalKill*/true, /*fGraceful*/false);

        for (int i = 0; i < cServers; i++)
            {
            memberHandler.addServer();
            }

        cache1 = getNamedCache("dist-std-test-1");
        cache2 = getNamedCache("dist-std-test-2");

        DistributedCacheService service = (DistributedCacheService) cache1.getCacheService();

        Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(cServers));
        waitForNodeSafe(service);

        cache1.clear();
        cache2.clear();
        }

    @After
    public void tearDown()
        {
        memberHandler.dispose();
        Cluster cluster = CacheFactory.ensureCluster();
        Eventually.assertThat(invoking(cluster).getMemberSet().size(), is(1));
        }

    // ----- test methods ---------------------------------------------------

    /**
     * Test of the put operations.
     */
    @Test
    public void testPut()
        {
        MapTriggerListener listener = new MapTriggerListener(new TestChildCacheMutator());
        cache1.addMapListener(listener);
        try
            {
            for (int i = 0; i < cKeys; i++)
                {
                cache1.put(i, i);
                }
            }
        finally
            {
            cache1.removeMapListener(listener);

            waitForNodeSafe(cache1.getCacheService());
            memberHandler.killOldestServer();
            }

        for (int i = 0; i < cKeys; i++)
            {
            assertEquals("Key " + i, i+1, cache1.get(i));
            assertEquals("Key " + i, i+1, cache2.get(i));
            }
        }

    /**
     * Test of the put operations where trigger throws exception.
     */
    @Test(expected=RuntimeException.class)
    public void testPutWithException()
        {
        MapTriggerListener triggerChildMutate = new MapTriggerListener(new TestChildCacheMutator());
        cache1.addMapListener(triggerChildMutate);

        MapTriggerListener triggerRollback = new MapTriggerListener(
            new FilterTrigger(new LessFilter(IdentityExtractor.INSTANCE, Integer.valueOf(1)), FilterTrigger.ACTION_ROLLBACK));
        cache2.addMapListener(triggerRollback);

        try
            {
            for (int i = 0; i < cKeys; i++)
                {
                cache1.put(i, i+1);
                fail("Failed to throw RuntimeException in testPutWithException");
                }
            }
        finally
            {
            cache1.removeMapListener(triggerChildMutate);
            cache2.removeMapListener(triggerRollback);
            }
        }

    /**
     * Test of the putAll operations.
     */
    @Test
    public void testPutAll()
        {
        MapTriggerListener listener = new MapTriggerListener(new TestChildCacheMutator());
        cache1.addMapListener(listener);
        try
            {
            HashMap map = new HashMap(cKeys);
            for (int i = 0; i < cKeys; i++)
                {
                map.put(i, i);
                }
            cache1.putAll(map);
            }
        finally
            {
            cache1.removeMapListener(listener);

            waitForNodeSafe(cache1.getCacheService());
            memberHandler.killOldestServer();
            }

        assertThat(cache1.size(), is(cKeys));

        for (int i = 0; i < cKeys; i++)
            {
            assertEquals("Key " + i, i+1, cache1.get(i));
            assertEquals("Key " + i, i+1, cache2.get(i));
            }
        }

    /**
     * Test of the putAll operations where trigger throws exception.
     */
    @Test(expected=RuntimeException.class)
    public void testPutAllWithException()
        {
        MapTriggerListener triggerChildMutate = new MapTriggerListener(new TestChildCacheMutator());
        cache1.addMapListener(triggerChildMutate);

        MapTriggerListener triggerRollback = new MapTriggerListener(
            new FilterTrigger(new LessFilter(IdentityExtractor.INSTANCE, Integer.valueOf(1)), FilterTrigger.ACTION_ROLLBACK));
        cache2.addMapListener(triggerRollback);

        try
            {
            HashMap map = new HashMap(cKeys);
            for (int i = 0; i < cKeys; i++)
                {
                map.put(i, i+1);
                }
            cache1.putAll(map);
            fail("Failed to throw RuntimeException in testPutAllWithException");
            }
        finally
            {
            cache1.removeMapListener(triggerChildMutate);
            cache2.removeMapListener(triggerRollback);
            }
        }

    /**
     * Test of the invoke operations.
     */
    @Test
    public void testInvoke()
        {
        EntryProcessor processor = new TestChildCacheMutator();
        for (int i = 0; i < cKeys; i++)
            {
            cache1.invoke(i, processor);
            }

        waitForNodeSafe(cache1.getCacheService());
        memberHandler.killOldestServer();

        for (int i = 0; i < cKeys; i++)
            {
            assertEquals("Key " + i, i+1, cache1.get(i));
            assertEquals("Key " + i, i+1, cache2.get(i));
            }
        }

    /**
      * Test of the invoke operations where trigger throws exception.
      */
    @Test(expected=RuntimeException.class)
    public void testInvokeWithException()
        {
        MapTriggerListener triggerRollback = new MapTriggerListener(
            new FilterTrigger(new LessFilter(IdentityExtractor.INSTANCE, Integer.valueOf(1)), FilterTrigger.ACTION_ROLLBACK));
        cache2.addMapListener(triggerRollback);

        EntryProcessor processor = new TestChildCacheMutator();
        try
            {
            for (int i = 0; i < cKeys; i++)
                {
                cache1.invoke(i, processor);
                }
            fail("Failed to throw RuntimeException in testInvokeWithException");
            }
        finally
            {
            cache2.removeMapListener(triggerRollback);
            }
        }

    /**
      * Test of the invokeAll operations.
      */
    @Test
    public void testInvokeAll()
        {
        EntryProcessor processor = new TestChildCacheMutator();
        Set colSet = new HashSet(cKeys);
        for (int i = 0; i < cKeys; i++)
            {
            colSet.add(i);
            }
        cache1.invokeAll(colSet, processor);

        waitForNodeSafe(cache1.getCacheService());
        memberHandler.killOldestServer();

        for (int i = 0; i < cKeys; i++)
            {
            assertEquals("Key " + i, i+1, cache1.get(i));
            assertEquals("Key " + i, i+1, cache2.get(i));
            }
        }

    /**
     * Test of the invoke operations where trigger throws exception.
     */
    @Test(expected=RuntimeException.class)
    public void testInvokeAllWithException()
        {
        MapTriggerListener triggerRollback = new MapTriggerListener(
            new FilterTrigger(new LessFilter(IdentityExtractor.INSTANCE, Integer.valueOf(1)), FilterTrigger.ACTION_ROLLBACK));
        cache2.addMapListener(triggerRollback);

        EntryProcessor processor = new TestChildCacheMutator();
        try
            {
            Set colSet = new HashSet(cKeys);
            for (int i = 0; i < cKeys; i++)
                {
                colSet.add(i);
                }
            cache1.invokeAll(colSet, processor);
            fail("Failed to throw RuntimeException in testInvokeWithException");
            }
        finally
            {
            cache2.removeMapListener(triggerRollback);
            }
        }

    /**
     * Test of the remove operations.
     */
    @Test
    public void testRemove()
        {
        HashMap map = new HashMap(cKeys);
        for (int i = 0; i < cKeys; i++)
            {
            map.put(i, i);
            }
        cache1.putAll(map);
        Eventually.assertThat(invoking(cache1).size(), is(cKeys));

        MapTriggerListener listener = new MapTriggerListener(new TestChildCacheMutator());
        cache1.addMapListener(listener);
        try
            {
            for (int i = 0; i < cKeys; i++)
                {
                cache1.remove(i);
                }
            }
        finally
            {
            cache1.removeMapListener(listener);

            waitForNodeSafe(cache1.getCacheService());
            memberHandler.killOldestServer();
            }

        for (int i = 0; i < cKeys; i++)
            {
            assertEquals("Key " + i, i+1, cache1.get(i));
            assertEquals("Key " + i, i+1, cache2.get(i));
            }
        }

    /**
     * Test of the remove operations where trigger throws exception.
     */
    @Test(expected=RuntimeException.class)
    public void testRemoveWithException()
        {
        HashMap map = new HashMap(cKeys);
        for (int i = 0; i < cKeys; i++)
            {
            map.put(i, i+1);
            }
        cache1.putAll(map);

        MapTriggerListener triggerChildMutate = new MapTriggerListener(new TestChildCacheMutator());
        cache1.addMapListener(triggerChildMutate);

        MapTriggerListener triggerRollback = new MapTriggerListener(
            new FilterTrigger(new LessFilter(IdentityExtractor.INSTANCE, Integer.valueOf(1)), FilterTrigger.ACTION_ROLLBACK));
        cache2.addMapListener(triggerRollback);

        try
            {
            for (int i = 0; i < cKeys; i++)
                {
                cache1.remove(i);
                fail("Failed to throw RuntimeException in testPutWithException");
                }
            }
        finally
            {
            cache1.removeMapListener(triggerChildMutate);
            cache2.removeMapListener(triggerRollback);
            }
        }

    /**
     * Test of the removeAll operations.
     */
    @Test
    public void testRemoveAll()
        {
        HashMap map = new HashMap(cKeys);
        for (int i = 0; i < cKeys; i++)
            {
            map.put(i, i);
            }
        cache1.putAll(map);

        MapTriggerListener listener = new MapTriggerListener(new TestChildCacheMutator());
        cache1.addMapListener(listener);
        try
            {
            Set colSet = new HashSet(cKeys);
            for (int i = 0; i < cKeys; i++)
                {
                colSet.add(i);
                }
            cache1.keySet().removeAll(colSet);
            }
        finally
            {
            cache1.removeMapListener(listener);

            waitForNodeSafe(cache1.getCacheService());
            memberHandler.killOldestServer();
            }

        for (int i = 0; i < cKeys; i++)
            {
            assertEquals("Key " + i, i+1, cache1.get(i));
            assertEquals("Key " + i, i+1, cache2.get(i));
            }
        }

    /**
     * Test of the removeAll operations where trigger throws exception.
     */
    @Test(expected=RuntimeException.class)
    public void testRemoveAllWithException()
        {
        HashMap map = new HashMap(cKeys);
        for (int i = 0; i < cKeys; i++)
            {
            map.put(i, i+1);
            }
        cache1.putAll(map);

        MapTriggerListener triggerChildMutate = new MapTriggerListener(new TestChildCacheMutator());
        cache1.addMapListener(triggerChildMutate);

        MapTriggerListener triggerRollback = new MapTriggerListener(
            new FilterTrigger(new LessFilter(IdentityExtractor.INSTANCE, Integer.valueOf(1)), FilterTrigger.ACTION_ROLLBACK));
        cache2.addMapListener(triggerRollback);

        try
            {
            Set colSet = new HashSet(cKeys);
            for (int i = 0; i < cKeys; i++)
                {
                colSet.add(i);
                }
            cache1.keySet().removeAll(colSet);
            fail("Failed to throw RuntimeException in testPutWithException");
            }
        finally
            {
            cache1.removeMapListener(triggerChildMutate);
            cache2.removeMapListener(triggerRollback);
            }
        }

    // ----- inner class: TestChildCacheMutateProcessor -------------------

    /**
    * EntryProcessor used by the testChildCacheMutate tests.
    */
    public static class TestChildCacheMutator
        extends AbstractProcessor implements MapTrigger
        {
        /**
        * {@inheritDoc}
        */
        public void process(com.tangosol.util.MapTrigger.Entry entry)
            {
            execute((BinaryEntry)entry);
            }

        @Override
        public Object process(com.tangosol.util.InvocableMap.Entry entry)
            {
            execute((BinaryEntry)entry);
            return null;
            }

        protected void execute(BinaryEntry binEntry)
            {
            BackingMapManagerContext ctx      = binEntry.getContext();
            Integer                  IKey     = (Integer) binEntry.getKey();
            Integer                  IValue   = (Integer) binEntry.getValue();

            // increment the value
            binEntry.setValue((IValue == null ? IKey.intValue() : IValue.intValue()) + 1, false);

            PartitionedService service       = (PartitionedService) ctx.getCacheService();
            KeyAssociator      associator    = service.getKeyAssociator();
            Integer            IKeyAssoc     = (Integer) associator.getAssociatedKey(IKey);
            Converter          converter     = ctx.getKeyToInternalConverter();
            Binary             binKeyAssoc   = (Binary) converter.convert(IKeyAssoc);
            BinaryEntry        binEntryAssoc = (BinaryEntry)
                ctx.getBackingMapContext("dist-std-test-2").getBackingMapEntry(
                    binKeyAssoc);

            // increment the associated value
            binEntryAssoc.setValue(IKeyAssoc.intValue() + 1 , false);
            }
        }

    // ----- inner class: Associator --------------------------------------

    /**
    * KeyAssociator implementation used by the PartitionedCacheAssoc service.
    * (See lite-txn-cache-config.xml).
    */
    public static class Associator
            implements KeyAssociator
        {
        /**
        * {@inheritDoc}
        */
        public void init(PartitionedService service)
            {
            }

        /**
        * {@inheritDoc}
        */
        public Object getAssociatedKey(Object oKey)
            {
            return oKey;
            }
        }

    protected NamedCache cache1;
    protected NamedCache cache2;
    protected MemberHandler memberHandler;

    // ----- data members and constants -----------------------------------
    /**
     * Servers to start
     */
    public static final int cServers = 2;

    /**
     * Total number of entries to be inserted
     */
    public static final int cKeys    = 10000;

    /**
    * The path to the cache configuration.
    */
    public final static String s_sCacheConfig = "coherence-cache-config.xml";

    /**
    * The path to the Ant build script.
    */
    public final static String s_sBuild       = "build.xml";

    /**
    * The project name.
    */
    public final static String s_sProject     = "trigger";

    /**
     * JUnit rule to capture the test name.
     */
    @Rule
    public TestName m_testName = new TestName();
    }
