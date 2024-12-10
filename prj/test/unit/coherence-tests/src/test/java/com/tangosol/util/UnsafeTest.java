/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;


/**
* A collection of unit tests for access to the Unsafe.
*
* @author rhl  2013.01.10
*/
public class UnsafeTest
    {
    /**
     * Make sure the tangosol.coherence.unsafe System property
     * is cleared before each test. We save the current value so that
     * it can be reset after the tests complete.
     */
    @Before
    public void clearUnsafeProperty()
        {
        m_savedUnsafeProperty = System.getProperty(TANGOSOL_COHERENCE_UNSAFE);
        System.clearProperty(TANGOSOL_COHERENCE_UNSAFE);
        }

    @After
    public void restoreUnsafeProperty()
        {
        if (m_savedUnsafeProperty != null)
            {
            System.setProperty(TANGOSOL_COHERENCE_UNSAFE, m_savedUnsafeProperty);
            }
        }

    /**
    * Invoke getUnsafe() naively.
    */
    @Test
    public void testReject()
        {
        try
            {
            Unsafe.getUnsafe();
            fail("expected exception");
            }
        catch (SecurityException e)
            {
            // expected
            }
        }

    /**
    * Invoke getUnsafe() "expertly".
    */
    @Test
    public void testExpert()
        {
        try
            {
            System.setProperty(TANGOSOL_COHERENCE_UNSAFE, "true");
            Unsafe.getUnsafe();
            }
        catch (SecurityException e)
            {
            fail("unexpected exception: " + e);
            }
        }

    /**
     * The value of the tangosol.coherence.unsafe System property
     * that will be restored after each test.
     */
    private String m_savedUnsafeProperty;

    public static final String TANGOSOL_COHERENCE_UNSAFE = "tangosol.coherence.unsafe";
    }
