/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Assert;
import org.junit.Test;


/**
 * Created by falcom on 12/11/14.
 */
public class IntBagTest
    {
    @Test
    public void testAdd()
        {
        IntBag bag = new IntBag();

        for (int i = 0; i < 100; ++i)
            {
            bag.add(i);
            }

        int[] a = bag.toArray();
        for (int i = 0; i < a.length; ++i)
            {
            Assert.assertEquals(a[i], i);
            }
        }

    @Test
    public void testAddAll()
        {
        IntBag[] aBag = new IntBag[10];

        for (int i = 0, c = aBag.length; i < c; ++i)
            {
            IntBag bag = aBag[i] = new IntBag();
            for (int j = 0, cj = 10 ; j < cj; ++j)
                {
                bag.add(i * cj + j);
                }
            }

        IntBag bigBag = new IntBag();
        for (IntBag bag : aBag)
            {
            bigBag.addAll(bag);
            }

        int[] a = bigBag.toArray();
        for (int i = 0; i < a.length; ++i)
            {
            Assert.assertEquals(a[i], i);
            }
        }

    @Test
    public void testForEach()
        {
        IntBag bag = new IntBag();

        int nSum = 0;
        for (int i = 0; i < 100; ++i)
            {
            bag.add(i);
            nSum += i;
            }

        AtomicInteger c = new AtomicInteger();
        bag.forEach(c::addAndGet);

        Assert.assertEquals(c.intValue(), nSum);
        }

    @Test
    public void testHashCode()
        {
        IntBag intBag1 = new IntBag();
        intBag1.add(1);
        IntBag intBag2 = new IntBag();
        intBag2.add(1);
        IntBag intBag3 = new IntBag();
        intBag3.add(2);
        Assert.assertEquals(intBag1.hashCode(), intBag2.hashCode());
        Assert.assertNotEquals(intBag1.hashCode(), intBag3.hashCode());
        }
    }
