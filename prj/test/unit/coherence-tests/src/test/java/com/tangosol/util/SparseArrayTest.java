/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import org.junit.Test;

import static org.junit.Assert.*;


/**
* Unit test of SparseArray validatity following a variety of useage patterns.
*
* @author mf 2007.09.28
*/
public class SparseArrayTest
    extends Base
    {
    /**
    * Test in order insertion.
    */
    @Test
    public void testOrderedInsertion()
        {
        SparseArray sa = new SparseArray();
        for (long i = 0; i < TREE_SIZE; ++i)
            {
            sa.set(i, null);
            sa.validate();
            }
        }

    /**
    * Test in order removal.
    */
    @Test
    public void testOrderedRemove()
        {
        SparseArray sa = new SparseArray();
        for (long i = 0; i < TREE_SIZE; ++i)
            {
            sa.set(i, null);
            }
        for (long i = 0; i < TREE_SIZE; ++i)
            {
            sa.remove(i);
            sa.validate();
            }
        }

    /**
    * Test reverse order removal.
    */
    @Test
    public void testReverseRemove()
        {
        SparseArray sa = new SparseArray();
        for (long i = 0; i < TREE_SIZE; ++i)
            {
            sa.set(i, null);
            }
        for (long i = TREE_SIZE - 1; i >= 0; --i)
            {
            sa.remove(i);
            sa.validate();
            }
        }

    /**
    * Test in order removal via iterator.
    */
    @Test
    public void testIteratorRemove()
        {
        SparseArray sa = new SparseArray();
        for (long i = 0; i < TREE_SIZE; ++i)
            {
            sa.set(i, null);
            }
        for (LongArray.Iterator iter = sa.iterator(); iter.hasNext(); )
            {
            iter.next();
            iter.remove();
            sa.validate();
            }
        assertTrue(sa.isEmpty());
        }

    /**
    * Test in range removal.
    */
    @Test
    public void testRangeRemove()
        {
        SparseArray sa = new SparseArray();
        for (long i = 0; i < TREE_SIZE; ++i)
            {
            sa.set(i, null);
            }

        sa.remove(0L, TREE_SIZE);

        sa.validate();
        assertTrue(sa.isEmpty());
        }

    /**
    * Test random insertion.
    */
    @Test
    public void testRandomInsertion()
        {
        SparseArray sa = new SparseArray();
        for (long i = 0; i < TREE_SIZE; ++i)
            {
            long lKey = getRandom().nextLong() % TREE_SIZE;
            sa.set(lKey, null);
            sa.validate();
            }
        }

    /**
    * Test random updates.
    */
    @Test
    public void testRandomUpdate()
        {
        SparseArray sa    = new SparseArray();
        long        lSpan = TREE_SIZE / 2;
        for (long i = 0; i < TREE_SIZE; ++i)
            {
            long lKey = getRandom().nextLong() % lSpan;
            sa.set(lKey, null);
            sa.validate();
            }
        }

    /**
    * Test random add remove.
    */
    @Test
    public void testRandomAddRemove()
        {
        SparseArray sa = new SparseArray();
        for (long i = 0; i < TREE_SIZE; ++i)
            {
            if (getRandom().nextInt() % 100 > 20 )
                {
                long lKey = getRandom().nextLong() % TREE_SIZE;
                sa.set(lKey, null);
                sa.validate();
                }
            else
                {
                long lKey = getRandom().nextLong() % TREE_SIZE;
                sa.remove(lKey);
                sa.validate();
                }
            }
        }

    /**
    * Test Get (and Iteration).
    */
    @Test
    public void testGet()
        {
        SparseArray sa    = new SparseArray();
        long        lSpan = TREE_SIZE / 2;
        for (long i = 0; i < TREE_SIZE; ++i)
            {
            long lKey = getRandom().nextLong() % lSpan;
            sa.set(lKey, Long.valueOf(lKey));
            sa.validate();
            }

        for (LongArray.Iterator iter = sa.iterator(); iter.hasNext(); )
            {
            assertTrue(((Long) iter.next()).longValue() == iter.getIndex());
            }
        }

    /**
    * Test the clone() operation
    */
    @Test
    public void testClone()
        {
        SparseArray sa = new SparseArray();
        assertEquals(sa, sa.clone());

        sa.set(0, null);
        assertEquals(sa, sa.clone());

        for (long i = 0; i < TREE_SIZE; ++i)
            {
            sa.set(i, null);
            }
        assertEquals(sa, sa.clone());
        }


    // ----- constants -----------------------------------------------------

    /**
    * The size of tree to use in the tests.
    */
    public static final long TREE_SIZE = 1000L;
    }
