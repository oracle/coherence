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
* AbstractExtendTests tests using a custom key class which is defined at both
* the extend client and the PartitionedCache.
*
* @author phf  2011.08.24
*/
public class DistExtendCustomKeyClassTests
        extends AbstractExtendTests
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public DistExtendCustomKeyClassTests()
        {
        super(CACHE_DIST_EXTEND_DIRECT);
        }


    // ----- test lifecycle -------------------------------------------------

    /**
    * Initialize the test class.
    */
    @BeforeClass
    public static void startup()
        {
        startCacheServerWithProxy("DistExtendCustomKeyClassTests", "extend", FILE_SERVER_CFG_CACHE);
        }

    /**
    * Shutdown the test class.
    */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("DistExtendCustomKeyClassTests");
        }


    // ----- AbstractExtendTests overrides ----------------------------------

    /**
    * {@inheritDoc}
    */
    @Override
    protected Object getKeyObject(Object o)
        {
        // use a custom key class which is defined at both the Extend client
        // and in the PartitionedCache to verify that the PartitionedCache
        // can deserialize the key when needed (e.g. for filters)
        return new CustomKeyClass(o);
        }
    }
