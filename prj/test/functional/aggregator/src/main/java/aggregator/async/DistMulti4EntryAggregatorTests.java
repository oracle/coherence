/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package aggregator.async;


import org.junit.AfterClass;
import org.junit.BeforeClass;


/**
 * A collection of functional tests for the various async
 * {@link com.tangosol.util.InvocableMap.EntryAggregator} implementations that use the
 * "dist-test4" cache and four cache servers with an artificially low scratch-space.
 *
 *  @author bb  2015.04.06
 *
 * @see com.tangosol.util.InvocableMap
 */
public class DistMulti4EntryAggregatorTests
        extends AbstractAsyncEntryAggregatorTests
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public DistMulti4EntryAggregatorTests()
        {
        super("dist-test4");
        }


    // ----- test lifecycle -------------------------------------------------

    /**
    * Initialize the test class.
    */
    @BeforeClass
    public static void startup()
        {
        // set the scratch space to 1b in order to force splitting of the aggregation
        System.setProperty("coherence.distributed.scratchspace", "1");

        startCacheServer("DistMulti4EntryAggregatorTests-1", "aggregator");
        startCacheServer("DistMulti4EntryAggregatorTests-2", "aggregator");
        startCacheServer("DistMulti4EntryAggregatorTests-3", "aggregator");
        startCacheServer("DistMulti4EntryAggregatorTests-4", "aggregator");
        }

    /**
    * Shutdown the test class.
    */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("DistMulti4EntryAggregatorTests-1");
        stopCacheServer("DistMulti4EntryAggregatorTests-2");
        stopCacheServer("DistMulti4EntryAggregatorTests-3");
        stopCacheServer("DistMulti4EntryAggregatorTests-4");
        }


    // ----- AbstractEntryAggregatorTests methods ---------------------------

    /**
    * {@inheritDoc}
    */
    protected int getCacheServerCount()
        {
        return 4;
        }
    }
