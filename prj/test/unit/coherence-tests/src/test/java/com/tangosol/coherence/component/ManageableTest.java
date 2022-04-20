/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.component;


import org.junit.Test;

import static org.junit.Assert.*;


/**
 * Regression tests for COH-4128
 *
 * @author nsa November 11, 2010
 */
public class ManageableTest
    {
    @Test
    public void testSimpleLoadClass()
        throws Exception
        {
        Class clz = Manageable.loadClass(Thread.currentThread().getContextClassLoader(),
            String.class.getName());

        assertTrue(clz.equals(String.class));
        }

    @Test
    public void testArrayLoadClass()
        throws Exception
        {
        Class clz = Manageable.loadClass(Thread.currentThread().getContextClassLoader(),
            String[].class.getName());

        assertTrue(clz.equals(String[].class));
        }
    }
