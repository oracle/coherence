/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package extend;

import com.tangosol.net.NamedCache;
import com.tangosol.net.cache.AbstractBundler;
import com.tangosol.net.cache.BundlingNamedCache;
import com.tangosol.net.partition.PartitionSet;

import com.tangosol.util.Filter;
import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.PartitionedFilter;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

/**
 * A collection of functional tests for Coherence*Extend that use the
 * "dist-extend-bundling-direct" cache.
 *
 * @author pfm  2012.05.11
 */
public class DistExtendDirectBundlingTests
        extends AbstractExtendTests
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public DistExtendDirectBundlingTests()
        {
        super(CACHE_DIST_EXTEND_DIRECT_BUNDLING);
        }


    // ----- test lifecycle -------------------------------------------------

    /**
     * Initialize the test class.
     */
    @BeforeClass
    public static void startup()
        {
        startCacheServerWithProxy("DistExtendDirectBundlingTests", "extend", FILE_SERVER_CFG_CACHE);
        }

    /**
     * Shutdown the test class.
     */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("DistExtendDirectBundlingTests");
        }


    // ----- PartitionedFilter test -----------------------------------------

    /**
     * Test the PartitionedFilter from an extend client.
     */
    @Test
    public void testPartitionedFilter()
        {
        NamedCache cache = getNamedCache();
        int cPartitions  = 257;                 // default partition size

        // fill the cache with some data
        for (int i = 0; i < 500; i++)
            {
            cache.put(i+"key"+i, "value"+i);
            }

        int cSize               = cache.size();
        int cPartitionSize      = 0;
        Set setKeys             = cache.keySet();
        Set setEntries          = cache.entrySet();
        Set setPartitionKeys    = new HashSet();
        Set setPartitionEntries = new HashSet();

        PartitionSet parts = new PartitionSet(cPartitions);

        // run the query for a single partition at a time
        for (int iPartition = 0; iPartition < cPartitions; iPartition++)
            {
            parts.add(iPartition);
            Filter filter = new PartitionedFilter(AlwaysFilter.INSTANCE, parts);

            Set setTest;
            int cKeys, cEntries;

            setTest = cache.keySet(filter);
            cKeys   = setTest.size();
            setPartitionKeys.addAll(setTest);

            setTest  = cache.entrySet(filter);
            cEntries = setTest.size();
            setPartitionEntries.addAll(setTest);

            assertEquals(cKeys + "!=" + cEntries, cEntries, cKeys);
            cPartitionSize += cEntries;
            parts.remove(iPartition);
            }

        assertEquals(cSize, cPartitionSize);
        assertEqualKeySet(setKeys, setPartitionKeys);
        assertEqualEntrySet(setEntries, setPartitionEntries);
        }

    /**
     * Test the bundle configuration with customized values.
     */
    @Test
    public void testBundlingCustomizedConfig()
        {
        BundlingNamedCache cache = (BundlingNamedCache) getNamedCache();

        validateCustomizedBundler(cache.getGetBundler());
        validateCustomizedBundler(cache.getPutBundler());
        validateCustomizedBundler(cache.getRemoveBundler());
        }

    /**
     * Validate the bundler that has customized values.
     *
     * @param bundler  the bundler
     */
    public static void validateCustomizedBundler(AbstractBundler bundler)
        {
        assertEquals(10, bundler.getSizeThreshold()); // preferred-size
        assertEquals(100, bundler.getDelayMillis());
        assertEquals(5, bundler.getThreadThreshold());
        assertTrue(bundler.isAllowAutoAdjust());
        }
    }
