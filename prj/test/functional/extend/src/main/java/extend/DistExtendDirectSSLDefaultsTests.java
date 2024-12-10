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
* "dist-extend-direct" cache over SSL with <socket-provider> and <serializer>
* specified in <defaults>.
*
* @author pfm  2012.04.25
*/
public class DistExtendDirectSSLDefaultsTests
        extends AbstractExtendTests
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public DistExtendDirectSSLDefaultsTests()
        {
        super(CACHE_DIST_EXTEND_DIRECT, "client-cache-config-ssl-defaults.xml");
        }


    // ----- test lifecycle -------------------------------------------------

    /**
    * Initialize the test class.
    */
    @BeforeClass
    public static void startup()
        {
        startCacheServerWithProxy("DistExtendDirectSSLDefaultsTests", "extend", "server-cache-config-ssl-defaults.xml");
        }

    /**
    * Shutdown the test class.
    */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("DistExtendDirectSSLDefaultsTests");
        }
    }
