/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package processor;


import com.tangosol.util.InvocableMap;

import org.junit.BeforeClass;


/**
* A collection of functional tests for the various
* {@link InvocableMap.EntryProcessor} implementations that use the
* "dist-std-test1" cache.
*
* @author jh  2005.12.21
*
* @see InvocableMap
*/
public class DistEntryProcessorTests
        extends AbstractDistEntryProcessorTests
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public DistEntryProcessorTests()
        {
        super("dist-std-test1");
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

        AbstractDistEntryProcessorTests._startup();
        }
    }
