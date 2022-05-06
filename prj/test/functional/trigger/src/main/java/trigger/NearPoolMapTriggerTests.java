/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package trigger;


import org.junit.BeforeClass;


/**
* A collection of functional tests for the MapTrigger functionality that use
* the "near-pool-test1" cache.
*
* @author gg 2008.03.14
*/
public class NearPoolMapTriggerTests
        extends AbstractMapTriggerTests
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public NearPoolMapTriggerTests()
        {
        super("near-pool-test1");
        }


    // ----- test lifecycle -------------------------------------------------

    /**
    * Initialize the test class.
    */
    @BeforeClass
    public static void _startup()
        {
        // this test requires local storage to be enabled
        System.setProperty("coherence.distributed.localstorage", "true");

        AbstractMapTriggerTests._startup();
        }
    }
