/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package extend;


import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;


/**
* A collection of functional tests for Coherence*Extend that use the
* "repl-extend-local" cache.
*
* @author jh  2005.11.29
*/
public class ReplExtendLocalTests
        extends AbstractExtendTests
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public ReplExtendLocalTests()
        {
        super(CACHE_REPL_EXTEND_LOCAL);
        }


    // ----- test lifecycle -------------------------------------------------

    /**
    * Initialize the test class.
    */
    @BeforeClass
    public static void startup()
        {
        startCacheServerWithProxy("ReplExtendLocalTests", "extend", FILE_SERVER_CFG_CACHE);
        }

    /**
    * Shutdown the test class.
    */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("ReplExtendLocalTests");
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
    }
