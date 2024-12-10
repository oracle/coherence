/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package partition;


import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.coherence.testing.AbstractRollingRestartTest;

import com.tangosol.coherence.component.util.SafeService;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.AbstractInvocable;
import com.tangosol.net.BackingMapContext;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.InvocationService;
import com.tangosol.net.Member;
import com.tangosol.net.NamedCache;
import com.tangosol.net.PartitionedService;
import com.tangosol.net.Service;

import com.tangosol.net.cache.CacheMap;

import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.ConcurrentMap;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.SegmentedConcurrentMap;

import com.tangosol.util.comparator.SafeComparator;

import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.InKeySetFilter;

import com.tangosol.util.processor.AbstractProcessor;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;

import org.junit.Rule;
import org.junit.Test;

import com.oracle.coherence.testing.util.CatchConcurrentExceptionsRule;


/**
 * Functional test for Delta-backups.
 *
 * This test spins up a number of client threads and makes random modifications
 * to values of random keys and then verifies the contents of both the primary
 * and backup.
 */
public class DeltaTests
        extends AbstractRollingRestartTest
    {
    public DeltaTests()
        {
        super(s_sCacheConfig);
        }

    // ----- AbstractRollingRestartTest methods ---------------------------

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


    // ----- test methods -------------------------------------------------

    @Test
    public void simpleTest()
        {
        doTest(4, false, "dist-delta-test", "Simple");
        }

    @Test
    public void simplePofTest()
        {
        doTest(4, false, "dist-delta-pof-test", "SimplePof");
        }

    @Test
    public void rollingRestartTest()
        {
        doTest(4, true, "dist-delta-test", "Rolling");
        }

    @Test
    public void rollingRestartPofTest()
        {
        doTest(4, true, "dist-delta-pof-test", "RollingPof");
        }

    /**
     * Main test method
     */
    protected void doTest(int cServers, boolean fRolling, String sCacheName, String sTestName)
        {
        log("-- DeltaTest.doTest BEGIN --");
        long ldtStart         = Base.getSafeTimeMillis();
        int  cRollingRestarts = 0;

        final NamedCache cache = getNamedCache(sCacheName);
        final DistributedCacheService service = (DistributedCacheService) cache.getCacheService();

        ConcurrentMap mapExpected  = new SegmentedConcurrentMap();
        MemberHandler memberHandler   = new MemberHandler(
                CacheFactory.ensureCluster(), "DeltaTests-" + sTestName,
                /*fExternalKill*/true, /*fGraceful*/false);

        for (int i = 0; i < cServers; i++)
            {
            memberHandler.addServer();
            }

        Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(cServers));

        try
            {
            log("cache=" + cache);
            for (int nIter = 0; nIter < TEST_ITERS; nIter++)
                {
                log("Test Iteration: " + nIter);
                mapExpected.clear();
                cache.clear();

                final Thread[] aThread = new Thread[TEST_THREADS];
                for (int iThread = 0; iThread < TEST_THREADS; iThread++)
                    {
                    aThread[iThread] = new ClientThread(cache, mapExpected);
                    aThread[iThread].start();
                    log("Thread " + iThread + " is started...");
                    }

                if (fRolling)
                    {
                    cRollingRestarts += doRollingRestart(
                            memberHandler,
                            new Filter()
                                {
                                public boolean evaluate(Object o)
                                    {
                                    for (int i = 0; i < TEST_THREADS; i++)
                                        {
                                        if (aThread[i].isAlive())
                                            {
                                            return false;
                                            }
                                        }

                                    return true;
                                    }
                                },
                            new WaitForNodeSafeRunnable(service));
                    Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(cServers));
                    }

                for (int iThread = 0; iThread < TEST_THREADS; iThread++)
                    {
                    aThread[iThread].join();
                    }

                log("All threads are done, now checking the cache...");
                checkCache(cache, mapExpected);
                }
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e);
            }
        finally
            {
            memberHandler.dispose();
            }

        log(TEST_ITERS + " iterations during " + cRollingRestarts +
           " server restarts, completed in " + ((Base.getSafeTimeMillis() - ldtStart) / 1000) + " seconds");
        log("-- DeltaTest.doTest END --");
        }


    // ----- helpers ------------------------------------------------------

    /**
     * Validate that the specified cache contains the specified mappings.
     *
     * @param cache the cache
     * @param mapExpected the expected cache mappings
     */
    public void checkCache(NamedCache cache, Map mapExpected)
        {
        PartitionedService svc = (PartitionedService) cache.getCacheService();
        StringBuilder      sb  = new StringBuilder();

        log("checkCache: cache=" + cache + ", size=" + cache.size() + ", expected-size=" + mapExpected.size());

        // check that the primary matches what we expect
        assertEquals(mapExpected.size(), cache.size());
        for (Iterator iter = mapExpected.entrySet().iterator(); iter.hasNext();)
            {
            Entry entry = (Entry) iter.next();
            Object oKey = entry.getKey();

            String sCompare = ((TestValue) entry.getValue()).compare((TestValue) cache.get(oKey));
            if (!sCompare.equals(""))
                {
                sb.append("wrong value for key ").append(oKey)
                  .append(" from partition ")
                  .append(svc.getKeyPartitioningStrategy().getKeyPartition(oKey))
                  .append(":").append(sCompare);
                }
            }

        if (sb.length() > 0)
            {
            fail(sb.toString());
            }

        // check that the backup matches the primary
        Filter filter = AlwaysFilter.INSTANCE;

        do
            {
            Map mapResults = cache.invokeAll(filter, new BackupCheckProcessor());
            Set setRetry   = new HashSet();
            for (Iterator iter = mapResults.entrySet().iterator(); iter.hasNext();)
                {
                Entry entry = (Entry) iter.next();
                Object oKey = entry.getKey();
                Object oValue = entry.getValue();

                if (oValue != null)
                    {
                    if (oValue.equals(RESULT_RETRY))
                        {
                        setRetry.add(oKey);
                        }
                    else
                        {
                        Object[] aoValues = (Object[]) oValue;
                        sb.append("Expected ").append(aoValues[0]).append(" but found ").append(aoValues[1]).append(
                                " for key ").append(oKey).append("\n");
                        }
                    }
                }
            if (sb.length() > 0)
                {
                fail(sb.toString());
                }

            if (!setRetry.isEmpty())
                {
                filter = new InKeySetFilter(AlwaysFilter.INSTANCE, setRetry);
                }
            else
                {
                filter = null;
                }
            }
        while (filter != null);
        }


    // ----- inner class: TestValue ---------------------------------------

    /**
     * The "value" stored in the cache.
     */
    public static class TestValue
            implements PortableObject, ExternalizableLite
        {
        // ----- constructors ---------------------------------------------
        /**
         * Default constructor.
         */
        public TestValue()
            {}

        // ----- factory method -------------------------------------------

        /**
         * Construct a TestValue with random contents.
         */
        public static TestValue makeRandom()
            {
            TestValue valueNew = new TestValue();
            valueNew.m_anValue = new int[TEST_VALUESIZE];

            // Note: word 0 is a modification counter (see #modify)
            for (int i = 1; i < TEST_VALUESIZE; i++)
                {
                valueNew.m_anValue[i] = m_random.nextInt();
                }

            return valueNew;
            }

        // ----- ExternalizableLite methods -------------------------------

        /**
         * {@inheritDoc}
         */
        public void readExternal(DataInput in)
                throws IOException
            {
            int[] anValue = new int[TEST_VALUESIZE];
            for (int i = 0; i < TEST_VALUESIZE; i++)
                {
                anValue[i] = ExternalizableHelper.readInt(in);
                }
            m_anValue = anValue;
            }

        /**
         * {@inheritDoc}
         */
        public void writeExternal(DataOutput out)
                throws IOException
            {
            for (int i = 0; i < TEST_VALUESIZE; i++)
                {
                ExternalizableHelper.writeInt(out, m_anValue[i]);
                }
            }

        // ----- PortableObject methods -----------------------------------

        /**
         * {@inheritDoc}
         */
        public void readExternal(PofReader in)
                throws IOException
            {
            int[] anValue = new int[TEST_VALUESIZE];
            for (int i = 0; i < TEST_VALUESIZE; i++)
                {
                anValue[i] = in.readInt(i);
                }
            m_anValue = anValue;
            }

        /**
         * {@inheritDoc}
         */
        public void writeExternal(PofWriter out)
                throws IOException
            {
            for (int i = 0; i < TEST_VALUESIZE; i++)
                {
                out.writeInt(i, m_anValue[i]);
                }
            }

        // ----- TestValue methods ----------------------------------------

        /**
         * Make a modification to the specified number of 32-bit "words" in the
         * test value.
         *
         * @param cWords the number of 32-bit words to modify
         */
        public void modify(int cWords)
            {
            for (int i = 1; i < cWords; i++)
                {
                int iWord = m_random.nextInt(TEST_VALUESIZE - 1) + 1;
                m_anValue[iWord] = m_random.nextInt();
                }

            // word 0 is a modification counter
            m_anValue[0] += 1;
            }

        // ----- test methods ---------------------------------------------

        /**
         * {@inheritDoc}
         */
        public String compare(TestValue value)
            {
            StringBuffer sb = new StringBuffer();
            if (value == null)
                {
                return "<null>"; //"expecting " + this + ", but was <null>";
                }
            for (int i = 0; i < TEST_VALUESIZE; i++)
                {
                if (m_anValue[i] != value.m_anValue[i])
                    {
                    sb.append("difference at word " + i + ": " + "0x" + Integer.toHexString(m_anValue[i]) + " vs. "
                            + "0x" + Integer.toHexString(value.m_anValue[i]) + "\n");
                    }
                }

            return sb.toString();
            }

        // ----- Object methods -------------------------------------------

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object o)
            {
            return (o instanceof TestValue) && Arrays.equals(((TestValue) o).m_anValue, m_anValue);
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode()
            {
            int nHash = 0;
            for (int i = 0; i < TEST_VALUESIZE; i++)
                {
                nHash = m_anValue[i] ^ nHash;
                }

            return nHash;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString()
            {
            StringBuilder sb = new StringBuilder();
            sb.append("[TestValue values=[");
            for (int i = 0; i < TEST_VALUESIZE; i++)
                {
                sb.append("0x").append(Integer.toHexString(m_anValue[i])).append(", ");
                }
            sb.append("]");

            return sb.toString();
            }

        // ----- constants and data members -------------------------------

        /**
         * The value array.
         */
        public int[] m_anValue;

        /**
         * Random seed.
         */
        public static final Random m_random = new Random();
        }


    // ----- inner class: ClientThread ------------------------------------

    /**
     * The client thread.
     */
    public static class ClientThread
            extends Thread
        {
        /**
         * Construct a ClientThread to run against the specified cache.
         *
         * @param cache the cache to run against
         * @param mapExpected the map used for test-verification
         */
        public ClientThread(NamedCache cache, ConcurrentMap mapExpected)
            {
            m_cache = cache;
            m_mapExpected = mapExpected;
            m_cKeys = TEST_KEYS;
            m_random = new Random();
            }

        // ----- Thread methods -------------------------------------------

        /**
         * {@inheritDoc}
         */
        @Override
        public void run()
            {
            doMutations();
            }

        // ----- helpers --------------------------------------------------

        /**
         * Perform a number of mutations, (configured by PROP_TEST_OPERATIONS).
         */
        public void doMutations()
            {
            for (int nIter = 0; nIter < TEST_OPERATIONS; nIter++)
                {
                switch (m_random.nextInt(5))
                    {
                    case 0:
                    case 1:
                    case 2:
                    case 3:
                        doPut();
                        break;

                    case 4:
                        doPutAll(TEST_BATCHSIZE);
                    }
                }
            }

        /**
         * Perform a single random "put" to the cache.
         */
        public void doPut()
            {
            Object oKey = getRandomKey();
            m_mapExpected.lock(oKey, -1);
            try
                {
                int cWords = m_random.nextInt(TEST_VALUESIZE);
                Object oValue = modifyValue(oKey, cWords);
                m_cache.put(oKey, oValue);
                // assertEquals(oValue, m_cache.get(oKey));
                // assertEquals(oValue, m_mapExpected.get(oKey));
                }
            finally
                {
                m_mapExpected.unlock(oKey);
                }
            }

        /**
         * Perform a "putAll" containing the specified number of keys to the
         * cache.
         *
         * @param cKeys the number of keys to putAll()
         */
        public void doPutAll(int cKeys)
            {
            Object[] aoKeys = new Object[cKeys];
            Map map = new HashMap();
            for (int i = 0; i < cKeys; i++)
                {
                Object oKey;
                do
                    {
                    // find a random key, not already picked
                    oKey = getRandomKey();
                    }
                while (map.containsKey(oKey));
                aoKeys[i] = oKey;
                map.put(oKey, null);
                }

            Arrays.sort(aoKeys, SafeComparator.INSTANCE);

            for (int i = 0; i < cKeys; i++)
                {
                Object oKey = aoKeys[i];
                m_mapExpected.lock(oKey, -1);

                int cWords = m_random.nextInt(TEST_VALUESIZE);
                map.put(oKey, modifyValue(oKey, cWords));
                }

            try
                {
                m_cache.putAll(map);
                }
            finally
                {
                for (int i = 0; i < cKeys; i++)
                    {
                    Object oKey = aoKeys[i];
                    // assertEquals("expected value not found for key " + oKey,
                    // map.get(oKey), m_mapExpected.get(oKey));
                    // assertEquals("expected value not found for key " + oKey,
                    // map.get(oKey), m_cache.get(oKey));
                    m_mapExpected.unlock(oKey);
                    }
                }
            }

        /**
         * Return a random key.
         */
        public Object getRandomKey()
            {
            return Integer.valueOf(m_random.nextInt(m_cKeys));
            }

        /**
         * Modify the specified number of 32-bit words of the value associated
         * with the specified key (or create a random key if one does not
         * already exist).
         *
         * @param oKey the key
         * @param cWords the number of words to modify
         */
        public Object modifyValue(Object oKey, int cWords)
            {
            TestValue value = (TestValue) m_mapExpected.get(oKey);
            if (value == null)
                {
                value = TestValue.makeRandom();
                }
            else
                {
                value.modify(cWords);
                }

            m_mapExpected.put(oKey, value);

            return value;
            }

        // ----- data members ---------------------------------------------

        /**
         * The random seed.
         */
        final Random        m_random;

        /**
         * The number of keys.
         */
        final int           m_cKeys;

        /**
         * The cache.
         */
        final NamedCache    m_cache;

        /**
         * The test-verification map.
         */
        final ConcurrentMap m_mapExpected;
        }


    // ----- inner class: BackupCheckProcessor ----------------------------

    /**
     * EntryProcessor that verifies the value of an entry against the value
     * stored on the (single) backup. The EntryProcessor uses the
     * InvocationService to access the backup-map through the "back-door".
     */
    public static class BackupCheckProcessor
            extends AbstractProcessor
            implements PortableObject, ExternalizableLite
        {
        /**
         * Default constructor
         */
        public BackupCheckProcessor()
            {}

        // ----- PortableObject methods -----------------------------------

        /**
         * {@inheritDoc}
         */
        public void readExternal(PofReader in)
                throws IOException
            {}

        /**
         * {@inheritDoc}
         */
        public void writeExternal(PofWriter out)
                throws IOException
            {}

        // ----- ExternalizableLite methods -------------------------------

        /**
         * {@inheritDoc}
         */
        public void readExternal(DataInput in)
                throws IOException
            {}

        /**
         * {@inheritDoc}
         */
        public void writeExternal(DataOutput out)
                throws IOException
            {}

        // ----- EntryProcessor methods -----------------------------------

        /**
         * {@inheritDoc}
         */
        public Object process(InvocableMap.Entry o)
            {
            // check the value on the primary (on the passed BinaryEntry)
            // against the value held by the backup
            BinaryEntry       binEntry = (BinaryEntry) o;
            Binary            binKey   = binEntry.getBinaryKey();
            BackingMapContext bmc      = binEntry.getBackingMapContext();
            String            sCache   = bmc.getCacheName();
            Service           service  = bmc.getManagerContext().getCacheService();
            InvocationService svcInvk  = (InvocationService) CacheFactory.getService("InvocationService");


            Member memberBackup = getBackupOwner(service, binKey);
            if (memberBackup == null)
                {
                // if there is no backup owner, assume everything is ok.
                return null;
                }

            Map mapResults = svcInvk.query(new BackupGetInvocable(sCache, binKey), Collections
                    .singleton(memberBackup));
            Object oResult = mapResults.get(memberBackup);
            if (oResult == null && !mapResults.containsKey(memberBackup) || Base.equals(oResult, RESULT_RETRY))
                {
                // the backup member left, or did not have the partition; retry
                return RESULT_RETRY;
                }
            else
                {
                Binary binValueBackup  = (Binary) oResult;
                Binary binValuePrimary = binEntry.getBinaryValue();

                long ldtNow;
                try
                    {
                    ldtNow = (Long) ClassHelper.invoke(service, "getClusterTime", new Object[]{});
                    }
                catch (Exception e)
                    {
                    throw Base.ensureRuntimeException(e);
                    }

                long cExpiryBackup = ExternalizableHelper.decodeExpiry(binValueBackup);
                cExpiryBackup = cExpiryBackup == CacheMap.EXPIRY_DEFAULT ||
                                cExpiryBackup == CacheMap.EXPIRY_NEVER
                        ? CacheMap.EXPIRY_NEVER
                        : cExpiryBackup - ldtNow;

                long cExpiryPrimary = binEntry.getExpiry();

                assertTrue("Expiry on primary and backup do not match " + cExpiryBackup + "!=" + cExpiryPrimary,
                       cExpiryBackup == cExpiryPrimary ||
                       (cExpiryBackup  != CacheMap.EXPIRY_DEFAULT && cExpiryBackup  != CacheMap.EXPIRY_NEVER &&
                        cExpiryPrimary != CacheMap.EXPIRY_DEFAULT && cExpiryPrimary != CacheMap.EXPIRY_NEVER &&
                        Math.abs(cExpiryPrimary - cExpiryBackup) < 1000L));

                binValueBackup  = ExternalizableHelper.undecorate(binValueBackup,  ExternalizableHelper.DECO_EXPIRY);
                binValuePrimary = ExternalizableHelper.undecorate(binValuePrimary, ExternalizableHelper.DECO_EXPIRY);

                return Base.equals(binValuePrimary, binValueBackup)
                        ? null
                        : new Object[] {binValuePrimary, binValueBackup };
                }
            }
        }


    // ----- BackupGetInvocable -------------------------------------------

    /**
     * Invocable that is sent to the InvocationService on the backup-owner
     * member to return the value of a specified key from the backup map.
     */
    public static class BackupGetInvocable
            extends AbstractInvocable
            implements ExternalizableLite
        {
        /**
         * Default constructor.
         */
        public BackupGetInvocable()
            {}

        /**
         * Construct a BackupGetInvocable to get the backup value for the
         * specified key.
         *
         * @param binKey the (binary) key to get the backup value for
         */
        public BackupGetInvocable(String sCacheName, Binary binKey)
            {
            m_sCacheName = sCacheName;
            m_binKey = binKey;
            }

        // ----- Invocable methods ----------------------------------------

        /**
         * {@inheritDoc}
         */
        public void run()
            {
            String sCacheName = m_sCacheName;
            NamedCache cache = CacheFactory.getCache(sCacheName);
            Service service = cache.getCacheService();

            SafeService      serviceSafe = (SafeService) service;
            PartitionedCache serviceReal = (PartitionedCache) serviceSafe.getService();

            try
                {
                Map mapBackup = serviceReal.getStorage(sCacheName).getBackupMap();
                if (mapBackup.containsKey(m_binKey))
                    {
                    setResult(mapBackup.get(m_binKey));
                    }
                else
                    {
                    // the key is not contained in the backup map; figure out
                    // why not.
                    Member memberBackup = getBackupOwner(service, m_binKey);
                    Member memberThis = (Member) ClassHelper.invoke(service, "getThisMember", new Object[0]);
                    if (memberBackup != memberThis)
                        {
                        log("rejecting invocable asking for " + m_binKey + " that is owned by "
                                + (memberBackup == null ? "<nobody>" : memberBackup.getId()));
                        setResult(RESULT_RETRY);
                        }
                    }
                }
            catch (Exception e)
                {
                throw Base.ensureRuntimeException(e);
                }
            }

        // ----- ExternalizableLite methods -------------------------------
        /**
         * {@inheritDoc}
         */
        public void writeExternal(DataOutput out)
                throws IOException
            {
            ExternalizableHelper.writeObject(out, m_binKey);
            ExternalizableHelper.writeUTF(out, m_sCacheName);
            }

        /**
         * {@inheritDoc}
         */
        public void readExternal(DataInput in)
                throws IOException
            {
            m_binKey = (Binary) ExternalizableHelper.readObject(in);
            m_sCacheName = ExternalizableHelper.readUTF(in);
            }

        // ----- data members ---------------------------------------------

        /**
         * The cache name.
         */
        public String m_sCacheName;

        /**
         * The (binary) key to get the backup value for.
         */
        public Binary m_binKey;
        }


    // ----- helpers ------------------------------------------------------

    /**
     * Return the backup owner for the specified key in the specified service.
     *
     * @param service the partitioned service
     * @param binKey the key
     *
     * @return the backup owner
     */
    public static Member getBackupOwner(Service service, Binary binKey)
        {
        try
            {
            int iPart = (Integer) ClassHelper.invoke(service, "getKeyPartition", new Object[] { binKey });
            return (Member) ClassHelper.invoke(service, "getBackupOwner", new Object[] { iPart, 1 });
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }


    // ----- debugging ----------------------------------------------------

    /**
     * Main method
     */
    public static void main(String[] args)
        {
        new DeltaTests().rollingRestartTest();
        new DeltaTests().rollingRestartPofTest();
        }


    // ----- constants and data members -----------------------------------

    @Rule
    public CatchConcurrentExceptionsRule m_catchRule = new CatchConcurrentExceptionsRule();

    /**
    * The path to the cache configuration.
    */
    public final static String s_sCacheConfig = "delta-test-cache-config.xml";

    /**
    * The path to the Ant build script.
    */
    public final static String s_sBuild = "build.xml";

    /**
    * The project name.
    */
    public final static String s_sProject = "partition";

    public static final String PROP_TEST_ITERS      = "test.DeltaTest.IterationCount";
    public static final String PROP_TEST_OPERATIONS = "test.DeltaTest.OperationCount";
    public static final String PROP_TEST_THREADS    = "test.DeltaTest.ThreadCount";
    public static final String PROP_TEST_KEYS       = "test.DeltaTest.KeyCount";
    public static final String PROP_TEST_BATCHSIZE  = "test.DeltaTest.KeyBatchSize";
    public static final String PROP_TEST_VALUESIZE  = "test.DeltaTest.ValueWordSize";
    public static final String RESULT_RETRY         = "NO_RESULT__RETRY";
    public static final int    TEST_ITERS;
    public static final int    TEST_OPERATIONS;
    public static final int    TEST_THREADS;
    public static final int    TEST_KEYS;
    public static final int    TEST_BATCHSIZE;
    public static final int    TEST_VALUESIZE;

    static
        {
        TEST_ITERS = Integer.getInteger(PROP_TEST_ITERS, 2);
        TEST_OPERATIONS = Integer.getInteger(PROP_TEST_OPERATIONS, 2500);
        TEST_THREADS = Integer.getInteger(PROP_TEST_THREADS, 4);
        TEST_KEYS = Integer.getInteger(PROP_TEST_KEYS, 500);
        TEST_BATCHSIZE = Integer.getInteger(PROP_TEST_BATCHSIZE, 5);
        TEST_VALUESIZE = Integer.getInteger(PROP_TEST_VALUESIZE, 500);
        }
    }