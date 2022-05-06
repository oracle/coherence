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
* that use the "dist-std-test2" cache and two cache servers.
*
* @author gg 2008.03.14
*/
public class DistMultiMapEventTransformerTests
        extends AbstractMapEventTransformerTests
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public DistMultiMapEventTransformerTests()
        {
        super("dist-std-test2");
        }


    // ----- test lifecycle -------------------------------------------------

    /**
    * Initialize the test class.
    */
    @BeforeClass
    public static void startup()
        {
        startCacheServer("DistMultiTransformerTests-1", "transformer");
        startCacheServer("DistMultiTransformerTests-2", "transformer");
        }

    /**
    * Shutdown the test class.
    */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("DistMultiTransformerTests-1");
        stopCacheServer("DistMultiTransformerTests-2");
        }
    }
