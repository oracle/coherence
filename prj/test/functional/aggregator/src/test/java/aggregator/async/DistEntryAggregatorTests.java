/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package aggregator.async;


import aggregator.AbstractEntryAggregatorTests;
import com.tangosol.util.InvocableMap;
import org.junit.BeforeClass;


/**
 * A collection of functional tests for the various async
 * {@link InvocableMap.EntryAggregator} implementations that use the
 * "dist-test1" cache.
 *
 * @author bb  2015.04.06
 *
 * @see InvocableMap
 */
public class DistEntryAggregatorTests
        extends AbstractAsyncEntryAggregatorTests
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public DistEntryAggregatorTests()
        {
        super("dist-test1");
        }


    // ----- test lifecycle -------------------------------------------------

    /**
    * Initialize the test class.
    */
    @BeforeClass
    public static void _startup()
        {
        // this test requires local storage to be enabled
        System.setProperty("tangosol.coherence.distributed.localstorage", "true");

        AbstractEntryAggregatorTests._startup();
        }
    }
