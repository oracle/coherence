/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package extend;


import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;


/**
* A collection of functional tests for Coherence*Extend that use the
* "repl-extend-direct" cache.
*
* @author jh  2005.11.29
*/
public class ReplExtendDirectTests
        extends AbstractExtendTests
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public ReplExtendDirectTests()
        {
        super(CACHE_REPL_EXTEND_DIRECT);
        }


    // ----- test lifecycle -------------------------------------------------

    /**
    * Initialize the test class.
    */
    @BeforeClass
    public static void startup()
        {
        startCacheServerWithProxy("ReplExtendDirectTests", "extend", FILE_SERVER_CFG_CACHE);
        }

    /**
    * Shutdown the test class.
    */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("ReplExtendDirectTests");
        }


    // ----- test methods ---------------------------------------------------

    @Test
    public void truncate()
        {
        // no-op
        }

    @Test
    public void truncateWithListener()
        {
        // no-op
        }

    @Test
    public void testExpiry()
        {
        // no-op
        }
    }
