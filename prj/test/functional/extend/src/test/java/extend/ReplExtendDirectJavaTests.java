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
* "repl-extend-direct-java" cache.
*
* @author jh  2005.11.29
*/
public class ReplExtendDirectJavaTests
        extends AbstractExtendTests
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public ReplExtendDirectJavaTests()
        {
        super(CACHE_REPL_EXTEND_DIRECT_JAVA);
        }


    // ----- test lifecycle -------------------------------------------------

    /**
    * Initialize the test class.
    */
    @BeforeClass
    public static void startup()
        {
        startCacheServerWithProxy("ReplExtendDirectJavaTests", "extend", FILE_SERVER_CFG_CACHE);
        }

    /**
    * Shutdown the test class.
    */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("ReplExtendDirectJavaTests");
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
