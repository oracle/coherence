/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package trigger;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * A collection of functional tests for the {@link com.tangosol.util.MapTrigger} functionality that use
 * the {@value CACHE_NAME} cache.  However, the tests expect an {@link IllegalArgumentException} to be thrown
 * as {@link com.tangosol.net.cache.ContinuousQueryCache} doesn't support the registration
 * {@link com.tangosol.util.MapTriggerListener}s.
 *
 * @author rlubke
 * @since 12.2.1.4
 */
public class ViewMapTriggerTests
        extends AbstractMapTriggerTests
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public ViewMapTriggerTests()
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

        AbstractMapTriggerTests._startup();
        }

    // ----- test methods ---------------------------------------------------

    @Test(expected = IllegalArgumentException.class)
    public void testPut()
        {
        super.testPut();
        }

    @Test(expected = IllegalArgumentException.class)
    public void testPutAll()
        {
        super.testPutAll();
        }

    @Test(expected = IllegalArgumentException.class)
    public void testRemove()
        {
        super.testRemove();
        }

    @Test(expected = IllegalArgumentException.class)
    public void testInvoke()
        {
        super.testInvoke();
        }

    @Test(expected = IllegalArgumentException.class)
    public void testInvokeAll()
        {
        super.testInvokeAll();
        }


    // ----- constants ------------------------------------------------------

    /**
     * Test cache name.
     */
    private static final String CACHE_NAME = "view-test1";
    }
