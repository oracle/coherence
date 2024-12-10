/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;


import com.tangosol.util.comparator.InverseComparator;
import com.tangosol.util.comparator.SafeComparator;
import org.junit.Test;

import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


/**
 * Tests for the {@link SortedBag}.
 *
 * @author rhl 2013.04.25
 */
public class SortedBagTest
    {
    @Test
    public void testEmpty()
        {
        SortedBag bag = new SortedBag();

        ensureEmpty(bag);
        bag.remove(1);
        ensureEmpty(bag);
        bag.add(1);
        bag.remove(1);
        ensureEmpty(bag);
        }

    @Test
    public void testSingle()
        {
        SortedBag bag = new SortedBag();

        bag.add(1);

        checkBag(bag, new Object[] {1});
        checkBag(bag.headBag(2), new Object[] {1});
        checkBag(bag.subBag(0, 10), new Object[] {1});
        checkBag(bag.tailBag(0), new Object[] {1});
        checkBag(bag.tailBag(1), new Object[] {1});

        ensureEmpty(bag.headBag(1));
        ensureEmpty(bag.subBag(1, 1));
        ensureEmpty(bag.tailBag(2));
        }

    @Test
    public void testMultipleUnique()
        {
        SortedBag bag = new SortedBag();

        bag.add(1);
        bag.add(5);

        checkBag(bag, new Object[] {1, 5});
        checkBag(bag.tailBag(1), new Object[] {1, 5});
        checkBag(bag.tailBag(2), new Object[] {5});
        checkBag(bag.headBag(5), new Object[] {1});
        checkBag(bag.headBag(6), new Object[] {1, 5});
        checkBag(bag.subBag(1, 6), new Object[] {1, 5});
        checkBag(bag.subBag(1, 5), new Object[] {1});

        ensureEmpty(bag.headBag(1));
        ensureEmpty(bag.headBag(1));
        ensureEmpty(bag.subBag(2, 5));
        ensureEmpty(bag.tailBag(6));
        }

    @Test
    public void testMultipleDuplicate()
        {
        SortedBag bag = new SortedBag();

        bag.add(1);
        bag.add(5);
        bag.add(1);
        bag.add(2);
        bag.add(4);
        bag.add(2);
        bag.add(3);
        bag.add(1);
        bag.add(4);

        checkBag(bag, new Object[] {1,1,1,2,2,3,4,4,5});
        checkBag(bag.tailBag(1), new Object[] {1,1,1,2,2,3,4,4,5});
        checkBag(bag.tailBag(2), new Object[] {2,2,3,4,4,5});
        checkBag(bag.headBag(5), new Object[] {1,1,1,2,2,3,4,4});
        checkBag(bag.headBag(6), new Object[] {1,1,1,2,2,3,4,4,5});
        checkBag(bag.subBag(1, 6), new Object[] {1,1,1,2,2,3,4,4,5});
        checkBag(bag.subBag(1, 5), new Object[] {1,1,1,2,2,3,4,4});
        checkBag(bag.subBag(1, 4), new Object[] {1,1,1,2,2,3});
        checkBag(bag.subBag(2, 4), new Object[] {2,2,3});

        ensureEmpty(bag.headBag(0));
        ensureEmpty(bag.headBag(1));
        ensureEmpty(bag.subBag(6,10));
        ensureEmpty(bag.tailBag(6));
        }

    @Test
    public void testMultipleDuplicateInverse()
        {
        SortedBag bag = new SortedBag(new InverseComparator(SafeComparator.INSTANCE));

        bag.add(1);
        bag.add(5);
        bag.add(1);
        bag.add(2);
        bag.add(4);
        bag.add(2);
        bag.add(3);
        bag.add(1);
        bag.add(4);

        checkBag(bag, new Object[] {5,4,4,3,2,2,1,1,1});
        checkBag(bag.tailBag(1), new Object[] {1,1,1});
        checkBag(bag.tailBag(2), new Object[] {2,2,1,1,1});
        checkBag(bag.tailBag(5), new Object[] {5, 4, 4, 3, 2, 2, 1, 1, 1});
        checkBag(bag.tailBag(6), new Object[] {5, 4, 4, 3, 2, 2, 1, 1, 1});
        checkBag(bag.subBag(6, 1), new Object[] {5, 4, 4, 3, 2, 2});
        checkBag(bag.subBag(5, 1), new Object[] {5, 4, 4, 3, 2, 2});
        checkBag(bag.subBag(5, 2), new Object[] {5,4,4,3});
        checkBag(bag.subBag(4, 1), new Object[] {4,4,3,2,2});
        checkBag(bag.subBag(4, 2), new Object[] {4,4,3});
        checkBag(bag.headBag(0), new Object[] {5,4,4,3,2,2,1,1,1});
        checkBag(bag.headBag(1), new Object[] {5, 4, 4, 3, 2, 2});

        ensureEmpty(bag.headBag(5));
        ensureEmpty(bag.headBag(6));
        ensureEmpty(bag.tailBag(0));
        ensureEmpty(bag.subBag(10, 6));

        bag.remove(0);
        checkBag(bag, new Object[] {5, 4, 4, 3, 2, 2, 1, 1, 1});
        bag.remove(1);
        checkBag(bag, new Object[] {5, 4, 4, 3, 2, 2, 1, 1});
        bag.remove(2);
        checkBag(bag, new Object[] {5, 4, 4, 3, 2, 1, 1});
        bag.remove(2);
        checkBag(bag, new Object[] {5, 4, 4, 3, 1, 1});
        bag.tailBag(4).remove(3);
        checkBag(bag, new Object[] {5, 4, 4, 1, 1});
        bag.tailBag(4).remove(4);
        checkBag(bag, new Object[] {5,4,1,1});
        bag.tailBag(3).remove(1);
        checkBag(bag, new Object[] {5,4,1});
        bag.headBag(3).remove(4);
        checkBag(bag, new Object[] {5, 1});
        }

    // ----- helper methods ------------------------------------------------

    protected void ensureEmpty(SortedBag bag)
        {
        assertTrue(bag.isEmpty());
        assertEquals(0, bag.size());
        assertFalse(bag.iterator().hasNext());
        }

    protected void checkBag(SortedBag bag, Object[] ao)
        {
        if (ao.length == 0)
            {
            ensureEmpty(bag);
            }
        else
            {
            assertFalse(bag.isEmpty());
            assertEquals(ao.length, bag.size());

            Iterator iter = bag.iterator();
            for (int i = 0; i < ao.length; i++)
                {
                Object o = ao[i];

                assertTrue(bag.contains(o));
                assertTrue(iter.hasNext());
                assertEquals(o, iter.next());
                }

            assertFalse(iter.hasNext());
            }
        }
    }
