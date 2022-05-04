/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package extend;


import org.junit.AfterClass;
import org.junit.BeforeClass;


/**
* A collection of functional tests for Coherence*Extend that use the
* "dist-extend-direct" cache over SSL.
*
* @author jh  2005.11.29
*/
public class DistExtendDirectSSLTests
        extends AbstractExtendTests
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public DistExtendDirectSSLTests()
        {
        super(CACHE_DIST_EXTEND_DIRECT, "client-cache-config-ssl.xml");
        }


    // ----- test lifecycle -------------------------------------------------

    /**
    * Initialize the test class.
    */
    @BeforeClass
    public static void startup()
        {
        startCacheServerWithProxy("DistExtendDirectSSLTests", "extend", "server-cache-config-ssl.xml");
        }

    /**
    * Shutdown the test class.
    */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("DistExtendDirectSSLTests");
        }
    }
