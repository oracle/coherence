/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package aggregator;

import org.junit.BeforeClass;

/**
 * A collection of functional tests for the various
 * {@link com.tangosol.util.InvocableMap.EntryAggregator} implementations that use the
 * {@value #CACHE_NAME} cache view.
 *
 * @author rlubke
 * @since 12.2.1.4
 */
public class ViewDistEntryAggregatorTests
        extends AbstractContinuousViewEntryAggregatorTests
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public ViewDistEntryAggregatorTests()
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

        AbstractEntryAggregatorTests._startup();
        }

    // ----- constants ------------------------------------------------------

    /**
     * Test cache name.
     */
    private static final String CACHE_NAME = "view-dist-test1";
    }
