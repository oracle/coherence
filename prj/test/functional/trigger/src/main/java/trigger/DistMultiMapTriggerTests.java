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
* the "dist-std-test1" cache and two cache servers.
*
* @author gg 2008.03.14
*/
public class DistMultiMapTriggerTests
        extends AbstractMapTriggerTests
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public DistMultiMapTriggerTests()
        {
        super("dist-std-test2");
        }


    // ----- test lifecycle -------------------------------------------------

    /**
    * Initialize the test class.
    */
    @BeforeClass
    public static void startup()
        {
        startCacheServer("DistMultiTriggerTests-1", "trigger");
        startCacheServer("DistMultiTriggerTests-2", "trigger");
        }

    /**
    * Shutdown the test class.
    */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("DistMultiTriggerTests-1");
        stopCacheServer("DistMultiTriggerTests-2");
        }
    }
