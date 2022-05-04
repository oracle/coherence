/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package filter;

import org.junit.BeforeClass;

/**
 * Filter functional testing using the cache view {@value #CACHE_NAME}.
 *
 * @author rlubke
 * @since 12.2.1.4
 */
public class ViewDistFilterTests
        extends AbstractContinuousViewFilterTests
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public ViewDistFilterTests()
        {
        super(CACHE_NAME);
        }

    // ----- test lifecycle -------------------------------------------------

    /**
     * Initialize the test class.
     */
    @BeforeClass
    public static void _startup()
        {
        // this test requires local storage to be enabled
        System.setProperty("coherence.distributed.localstorage", "true");

        AbstractFilterTests._startup();
        }

    // ----- constants ------------------------------------------------------

    /**
     * Test cache name.
     */
    private static final String CACHE_NAME = "view-dist-test1";
    }
