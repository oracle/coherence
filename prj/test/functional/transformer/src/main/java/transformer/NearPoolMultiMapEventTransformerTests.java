/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package transformer;


import org.junit.AfterClass;
import org.junit.BeforeClass;


/**
* A collection of functional tests for the MapEventTransformer functionality
* that use the "near-pool-test2" cache and two cache servers.
*
* @author gg 2008.03.14
*/
public class NearPoolMultiMapEventTransformerTests
        extends AbstractMapEventTransformerTests
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public NearPoolMultiMapEventTransformerTests()
        {
        super("near-pool-test2");
        }


    // ----- test lifecycle -------------------------------------------------

    /**
    * Initialize the test class.
    */
    @BeforeClass
    public static void startup()
        {
        startCacheServer("NearPoolMultiTransformerTests-1", "transformer");
        startCacheServer("NearPoolMultiTransformerTests-2", "transformer");
        }

    /**
    * Shutdown the test class.
    */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("NearPoolMultiTransformerTests-1");
        stopCacheServer("NearPoolMultiTransformerTests-2");
        }
    }
