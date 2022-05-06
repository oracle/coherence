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
* the "dist-pool-test1" cache and two cache servers.
*
* @author gg 2008.03.14
*/
public class DistPoolMultiMapTriggerTests
        extends AbstractMapTriggerTests
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public DistPoolMultiMapTriggerTests()
        {
        super("dist-pool-test2");
        }


    // ----- test lifecycle -------------------------------------------------

    /**
    * Initialize the test class.
    */
    @BeforeClass
    public static void startup()
        {
        startCacheServer("DistPoolMultiTriggerTests-1", "trigger");
        startCacheServer("DistPoolMultiTriggerTests-2", "trigger");
        }

    /**
    * Shutdown the test class.
    */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("DistPoolMultiTriggerTests-1");
        stopCacheServer("DistPoolMultiTriggerTests-2");
        }
    }
