/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package processor;


import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.tangosol.util.InvocableMap;


/**
* A collection of functional tests for the various
* {@link InvocableMap.EntryProcessor} implementations that use the
* "near-pool-test2" cache and two cache servers.
*
* @author jh  2005.12.21
*
* @see InvocableMap
*/
public class NearPoolMultiEntryProcessorTests
        extends AbstractDistEntryProcessorTests
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public NearPoolMultiEntryProcessorTests()
        {
        super("near-pool-test2");
        }


    // ----- test lifecycle -------------------------------------------------

    /**
    * Initialize the test class.
    */
    @BeforeClass
    public static void startup()
        {
        startCacheServer("NearPoolMultiProcessorTests-1", "processor");
        startCacheServer("NearPoolMultiProcessorTests-2", "processor");
        }

    /**
    * Shutdown the test class.
    */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("NearPoolMultiProcessorTests-1");
        stopCacheServer("NearPoolMultiProcessorTests-2");
        }
    }
