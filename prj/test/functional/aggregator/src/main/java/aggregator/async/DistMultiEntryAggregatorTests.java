/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package aggregator.async;

import com.tangosol.util.InvocableMap;

import org.junit.AfterClass;
import org.junit.BeforeClass;


/**
 * A collection of functional tests for the various async
 * {@link InvocableMap.EntryAggregator} implementations that use the
 * "dist-test2" cache and two cache servers.
 *
 * @author bb  2015.04.06
 *
 * @see InvocableMap
 */
public class DistMultiEntryAggregatorTests
        extends AbstractAsyncEntryAggregatorTests
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public DistMultiEntryAggregatorTests()
        {
        super("dist-test2");
        }


    // ----- test lifecycle -------------------------------------------------

    /**
    * Initialize the test class.
    */
    @BeforeClass
    public static void startup()
        {
        startCacheServer("DistMultiEntryAggregatorTests-1", "aggregator");
        startCacheServer("DistMultiEntryAggregatorTests-2", "aggregator");
        }

    /**
    * Shutdown the test class.
    */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("DistMultiEntryAggregatorTests-1");
        stopCacheServer("DistMultiEntryAggregatorTests-2");
        }


    // ----- AbstractEntryAggregatorTests methods ---------------------------

    /**
    * {@inheritDoc}
    */
    protected int getCacheServerCount()
        {
        return 2;
        }
    }
