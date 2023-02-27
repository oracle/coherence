/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package processor;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.tangosol.net.CacheService;
import com.tangosol.net.NamedCache;
import com.tangosol.net.PartitionedService;

import com.tangosol.net.partition.SimplePartitionKey;

import com.tangosol.util.CompositeKey;
import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMap.Entry;
import com.tangosol.util.InvocableMap.EntryProcessor;
import com.tangosol.util.extractor.IdentityExtractor;
import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.NotEqualsFilter;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import org.junit.After;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Coh12875Tests is a regression test to ensure bug (COH-12875) is not re-
 * introduced.
 * <p>
 * The bug results in an internal object being held as a result for a client
 * EntryProcessor request, opposed to the actual Binary result.
 *
 * @author hr  2015.01.28
 */
public class Coh12875Tests
        extends AbstractFunctionalTest
    {
    // ----- test support methods -------------------------------------------

    @After
    public void _afterTest()
        {
        stopAllApplications();
        super._afterTest();
        }

    // ----- tests ----------------------------------------------------------

    /**
     * Test {@link NamedCache#invokeAll(Collection, EntryProcessor)} without
     * a thread pool.
     */
    @Test
    public void testInvokeAll()
        {
        doInvokeAll(/*fMultiThreaded*/ false);
        }

    /**
     * Test {@link NamedCache#invokeAll(Collection, EntryProcessor)} with
     * a thread pool.
     */
    @Test
    public void testInvokeAllJobs()
        {
        doInvokeAll(/*fMultiThreaded*/ true);
        }

    /**
     * Test {@link NamedCache#invokeAll(Filter, EntryProcessor)}.
     */
    @Test
    public void testInvokeFilter()
        {
        startCacheServer("Coh12875TestsFilter-0", "processor", null, PROPS_SEONE);

        // a 'graceful' call to startCacheServer will ensure all autostart
        // services are running

        NamedCache         cache   = getFactory().ensureCache("dist-pool-test-coh17875", null);
        PartitionedService service = (PartitionedService) cache.getCacheService();

        Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(1));

        cache.put(createKey(0, 0), "");
        cache.put(createKey(0, 1), "");
        cache.put(createKey(1, 0), "");
        cache.put(createKey(1, 1), "");
        try
            {
            cache.invokeAll(AlwaysFilter.INSTANCE, new VolatileProcessor());
            }
        catch (Throwable t)
            {
            // suppress the expected exception
            }

        // prior to COH-12875 an EP that threw an exception could corrupt the
        // ResultInfo causing transfer of the associated partitions to fail;
        // cause a transfer of the partitions belonging to the EP

        startCacheServer("Coh12875TestsFilter-1", "processor", null, PROPS_SEONE);

        Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(2));
        waitForBalanced((CacheService) service);

        assertThat(cache.size(), is(4));

        // assert that the
        Set setKeys = cache.keySet(new NotEqualsFilter(IdentityExtractor.INSTANCE, ""));
        assertThat(setKeys.size(), is(1));
        }

    // ----- helpers --------------------------------------------------------

    protected void doInvokeAll(boolean fMultiThreaded)
        {
        String sCacheName    = "dist-" + (fMultiThreaded ? "pool" : "std") + "-test-coh17875";
        String sServerPrefix = "Coh12875TestsInvokeAll" + (fMultiThreaded ? "Multi-" : "-");

        startCacheServer(sServerPrefix + '0', "processor", null, PROPS_SEONE);

        // a 'graceful' call to startCacheServer will ensure all autostart
        // services are running

        NamedCache         cache   = getFactory().ensureCache(sCacheName, null);
        PartitionedService service = (PartitionedService) cache.getCacheService();

        Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(1));
        try
            {
            cache.invokeAll(Arrays.asList(
                    createKey(0, 0),
                    createKey(0, 1),
                    createKey(1, 0),
                    createKey(1, 1)),
                new VolatileProcessor());
            }
        catch (Throwable t)
            {
            // suppress the expected exception
            }

        // Note: we do not issue another request from this client as it would
        //       remove the ResultInfo
        // assertThat(cache.size(), is(2));

        // prior to COH-12875 an EP that threw an exception could corrupt the
        // ResultInfo causing transfer of the associated partitions to fail;
        // cause a transfer of the partitions belonging to the EP

        startCacheServer(sServerPrefix + '1', "processor", null, PROPS_SEONE);

        Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(2));
        waitForBalanced((CacheService) service);

        // a multi-threaded request will successfully execute one key per partition
        assertThat("Data was lost due to an error on transfer; @see Coh12875CorruptResultInfo-0.out",
                cache.size(), is(fMultiThreaded ? 2 : 1));
        }

    protected static Object createKey(int iPart, Object oKey)
        {
        return new CompositeKey(SimplePartitionKey.getPartitionKey(iPart), oKey);
        }

    // ----- inner class: VolatileProcessor ---------------------------------

    /**
     * EntryProcessor that throws an exception after the first entry it processes.
     * The key for the processed entry is returned.
     */
    public static class VolatileProcessor
            implements EntryProcessor
        {
        @Override
        public Object process(Entry entry)
            {
            if (m_cProcessed++ != 0)
                {
                throw new IllegalStateException("oops");
                }
            CompositeKey key = (CompositeKey) entry.getKey();

            entry.setValue(String.format("%s-%s", key.getPrimaryKey(), key.getSecondaryKey()));
            return entry.getKey();
            }

        int m_cProcessed;
        }
    }
