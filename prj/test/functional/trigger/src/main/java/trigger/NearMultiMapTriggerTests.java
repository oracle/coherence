/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package trigger;


import org.junit.AfterClass;
import org.junit.BeforeClass;


/**
* A collection of functional tests for the MapTrigger functionality that use
* the "near-std-test1" cache and two cache servers.
*
* @author gg 2008.03.14
*/
public class NearMultiMapTriggerTests
        extends AbstractMapTriggerTests
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public NearMultiMapTriggerTests()
        {
        super("near-std-test2");
        }


    // ----- test lifecycle -------------------------------------------------

    /**
    * Initialize the test class.
    */
    @BeforeClass
    public static void startup()
        {
        startCacheServer("NearMultiTriggerTests-1", "trigger");
        startCacheServer("NearMultiTriggerTests-2", "trigger");
        }

    /**
    * Shutdown the test class.
    */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("NearMultiTriggerTests-1");
        stopCacheServer("NearMultiTriggerTests-2");
        }
    }
