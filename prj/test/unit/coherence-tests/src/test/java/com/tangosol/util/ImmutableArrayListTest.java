/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import org.junit.Test;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.Assert.*;


/**
* Unit test class for the {@link ImmutableArrayList} class.
*
* @author cp  2005.06.14
* @author jh  2005.09.27
*/
public class ImmutableArrayListTest
        extends Base
    {
    // ----- test methods ---------------------------------------------------

    /**
    * Test a "small" sized {@link ImmutableArrayList}.
    *
    * @see #_test(List, Long[])
    */
    @Test
    public void smallArray()
        {
        Long[] aL = _makeArray(10);
        _test(_makeList(aL), aL);
        }

    /**
    * Test a "medium" sized {@link ImmutableArrayList}.
    *
    * @see #_test(List, Long[])
    */
    @Test
    public void mediumArray()
        {
        Long[] aL = _makeArray(1000);
        _test(_makeList(aL), aL);
        }

    /**
    * Test a "large" sized {@link ImmutableArrayList}.
    *
    * @see #_test(List, Long[])
    */
    @Test
    public void largeArray()
        {
        Long[] aL = _makeArray(10000);
        _test(_makeList(aL), aL);
        }


    // ----- internal test methods ------------------------------------------

    public List _makeList(Long[] aL)
        {
        return new ImmutableArrayList(aL);
        }

    /**
    * Make an array of a given number of Long elements
    *
    * @param c  the number of elements
    *
    * @return  the list
    */
    public static Long[] _makeArray(int c)
        {
        assertTrue(c > 0);
        Long[] aL = new Long[c];
        for (int i = 0; i < c; ++i)
            {
            aL[i] = (long) i;
            }

        return aL;
        }

    /**
    * Run a variety of tests of the following methods on a new
    * ImmutableArrayList of the specified size:
    * <ul>
    *   <li>{@link ImmutableArrayList#size}</li>
    *   <li>{@link ImmutableArrayList#iterator}</li>
    *   <li>{@link ImmutableArrayList#get}</li>
    *   <li>{@link ImmutableArrayList#contains}</li>
    *   <li>{@link ImmutableArrayList#indexOf}</li>
    *   <li>{@link ImmutableArrayList#lastIndexOf}</li>
    *   <li>{@link ImmutableArrayList#toArray}</li>
    * </ul>
    *
    * @param list  the list to test
    * @param aL    the array to validate against
    */
    static void _test(List list, Long[] aL)
        {
        int c = aL.length;

        // size
        assertTrue(c == list.size());

        // iterator
        {
        int i = 0;
        Iterator iter = list.iterator();
        for (; iter.hasNext(); )
            {
            Long L = (Long) iter.next();
            assertTrue(L == aL[i++]);
            }
        try
            {
            iter.next();
            fail();
            }
        catch (NoSuchElementException e)
            {
            // expected
            }
        }

        // get
        for (int i = 0; i < c; ++i)
            {
            assertTrue(list.get(i) == aL[i]);
            }

        // contains
        assertFalse(list.contains(null));                   // test null
        assertFalse(list.contains((long) -1));           // test !contains
        assertTrue(list.contains(0L) == (c != 0)); // test .equals()
        for (int i = 0; i < c; ++i)
            {
            assertTrue(list.contains(aL[i]));
            }

        // indexOf
        assertTrue(list.indexOf(null) == -1);                       // test null
        assertTrue(list.indexOf((long) -1) == -1);               // test !contains
        assertTrue(list.indexOf(0L) == (c == 0 ? -1 : 0)); // test .equals()
        for (int i = 0; i < c; ++i)
            {
            assertTrue(list.indexOf(aL[i]) == i);
            }

        // lastIndexOf
        assertTrue(list.lastIndexOf(null) == -1);                       // test null
        assertTrue(list.lastIndexOf((long) -1) == -1);               // test !contains
        assertTrue(list.lastIndexOf(0L) == (c == 0 ? -1 : 0)); // test .equals()
        for (int i = 0; i < c; ++i)
            {
            assertTrue(list.lastIndexOf(aL[i]) == i);
            }

        // toArray
        {
        Object[] ao1 = list.toArray();
        Long[]   aL2 = (Long[]) list.toArray(new Long[0]);
        Long[]   aL3 = (Long[]) list.toArray(new Long[Math.max(0, list.size() - 1)]);
        Long[]   aL4 = (Long[]) list.toArray(new Long[list.size()]);
        Long[]   aL5 = (Long[]) list.toArray(new Long[list.size() + 1]);
        assertTrue(equalsDeep(aL, ao1));
        assertTrue(equalsDeep(aL, aL2));
        assertTrue(equalsDeep(aL, aL3));
        assertTrue(equalsDeep(aL, aL4));
        assertFalse(equalsDeep(aL, aL5));
        }

        if (c < 2)
            {
            return;
            }

        // test dups (changes "value index" logic)
        aL[c-1] = aL[0];
        list = new ImmutableArrayList(aL);

        // contains
        assertFalse(list.contains(null));                   // test null
        assertFalse(list.contains((long) -1));           // test !contains
        assertTrue(list.contains(0L) == (c != 0)); // test .equals()
        for (int i = 0; i < c; ++i)
            {
            assertTrue(list.contains(aL[i]));
            }

        // indexOf
        assertTrue(list.indexOf(null) == -1);               // test null
        assertTrue(list.indexOf((long) -1) == -1);       // test !contains
        assertTrue(list.indexOf(0L) == 0);         // test .equals()
        for (int i = 0; i < c-1; ++i)
            {
            assertTrue(list.indexOf(aL[i]) == i);
            }

        // lastIndexOf
        assertTrue(list.lastIndexOf(null) == -1);           // test null
        assertTrue(list.lastIndexOf((long) -1) == -1);   // test !contains
        assertTrue(list.lastIndexOf(0L) == (c - 1)); // test .equals()
        for (int i = 1; i < c; ++i)
            {
            assertTrue(list.lastIndexOf(aL[i]) == i);
            }

        // subList
        List listSub = list.subList(1, list.size() - 1);
        assertTrue(listSub.size() == list.size() - 2);
        Iterator iterSub  = listSub.listIterator();
        Iterator iterOrig = list.listIterator(1);
        int cIters = 0;
        while (iterSub.hasNext())
            {
            assertTrue(iterSub.next() == iterOrig.next());
            ++cIters;
            }
        assertTrue(cIters == listSub.size());
        }
    }
