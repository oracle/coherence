/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.tests;


import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
* Example JUnit test.
*
* @author jh  2009.11.09
*/
public class ExampleTest
    {
    @BeforeClass
    public static void testBeforeClass()
        {
        System.out.println("BeforeClass");
        }

    @Before
    public void testBefore()
        {
        System.out.println("Before");
        }

    @Test
    public void testExample1()
        {
        System.out.println("Test: testExample1");
        assertTrue(true);
        }

    @Test
    public void testExample2()
        {
        System.out.println("Test: testExample2");
        assertTrue(true);
        }

    @Ignore
    public void testIgnore()
        {
        System.out.println("Ignore");
        }

    @After
    public void testAfter()
        {
        System.out.println("After");
        }

    @AfterClass
    public static void testAfterClass()
        {
        System.out.println("AfterClass");
        }
    }
