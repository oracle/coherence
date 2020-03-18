/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package trigger;


import org.junit.BeforeClass;


/**
* A collection of functional tests for the MapTrigger functionality that use
* the "near-std-test1" cache.
*
* @author gg 2008.03.14
*/
public class NearMapTriggerTests
        extends AbstractMapTriggerTests
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public NearMapTriggerTests()
        {
        super("near-std-test1");
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

        AbstractMapTriggerTests._startup();
        }
    }
