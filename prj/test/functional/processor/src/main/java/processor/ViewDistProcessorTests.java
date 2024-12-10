/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package processor;

import org.junit.BeforeClass;

/**
 * A collection of functional tests for the various
 * {@link com.tangosol.util.InvocableMap.EntryProcessor} implementations that use the
 * {@value #CACHE_NAME} cache.
 *
 * @see com.tangosol.util.InvocableMap
 *
 * @author rlubke
 * @since 12.2.1.4
 */
public class ViewDistProcessorTests
        extends AbstractDistEntryProcessorTests
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public ViewDistProcessorTests()
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

        AbstractDistEntryProcessorTests._startup();
        }

    // ----- constants ------------------------------------------------------

    /**
     * Test cache name.
     */
    private static final String CACHE_NAME = "view-test1";
    }
