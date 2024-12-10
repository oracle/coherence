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
* A collection of functional tests for Coherence*Extend that go through the
* AbstractExtendTests tests using a custom key class which is defined at the
* extend client but not at the PartitionedCache.
*
* @author phf  2011.08.24
*/
public class DistExtendClientKeyClassTests
        extends AbstractExtendTests
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public DistExtendClientKeyClassTests()
        {
        super(CACHE_DIST_EXTEND_CLIENT_KEY);
        }


    // ----- test lifecycle -------------------------------------------------

    /**
    * Initialize the test class.
    */
    @BeforeClass
    public static void startup()
        {
        startCacheServerWithProxy("DistExtendClientKeyClassTests", "extend", FILE_SERVER_CFG_CACHE);
        }

    /**
    * Shutdown the test class.
    */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("DistExtendClientKeyClassTests");
        }


    // ----- AbstractExtendTests overrides ----------------------------------

    /**
    * {@inheritDoc}
    */
    @Override
    protected Object getKeyObject(Object o)
        {
        // use a key class which is only defined in the extend client to
        // verify that the PartitionedCache does not deserialize the key
        return new CustomKeyClass(o);
        }
    }
