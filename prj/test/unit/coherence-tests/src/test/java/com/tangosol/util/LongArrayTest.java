/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.tangosol.util.LongArray.Iterator;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;


/**
* A collection of tests designed to test LongArray implementations.
*
* @author rhl  2008.05.20
*/
public class LongArrayTest
        extends Base
    {
    @Test
    public void keysTest()
        {
        LongArray<Integer> array = new SparseArray<>();
        for (int i = 0; i < 10; i++)
            {
            array.add(i);
            }

        long[] expected = new long[10];
        int counter = 0;
        for (Iterator<Integer> iter = array.iterator(); iter.hasNext();)
            {
            iter.next();
            expected[counter++] = iter.getIndex();
            }

        assertArrayEquals(expected, array.keys());
        }

    /**
    * Run tests with SimpleLongArray implementation
    */
    @Test
    public void testSimpleLongArray()
        {
        LongArray array = new SimpleLongArray();
        testLongArray(array);
        }

    /**
    * Run tests with SparseArray implementation
    */
    @Test
    public void testSparseArray()
        {
        LongArray array = new SparseArray();
        testLongArray(array);
        }

    /**
    * Run tests with PrimitiveSparseArray implementation
    */
    @Test
    public void testPrimitiveSparseArray()
        {
        LongArray array = new PrimitiveSparseArray();
        testLongArray(array);
        }

    /**
    * Test longArray against the reference implementation.
    *
    * @param longArray  the LongArray to test
    */
    private static void testLongArray(LongArray longArray)
        {
        LongArray referenceImpl = new ReferenceImpl();
        testBasic(longArray, referenceImpl);
        testIterators(longArray, referenceImpl);
        }

    /**
    * Test the basic LongArray interface.
    *
    * @param la0  the first LongArray
    * @param la1  the second LongArray
    */
    private static void testBasic(LongArray la0, LongArray la1)
        {
        StringBuffer sb = new StringBuffer();
        la0.clear();
        la1.clear();

        assertLongArrayEmpty(la0);
        assertLongArrayEmpty(la1);

        sb.setLength(0);
        sequentialInsertion(la0, la1, 0, 39, sb);
        assertLongArrayEqual(la0, la1, sb.toString());

        sequentialRemove(la0, la1, 10, 19);
        assertLongArrayEqual(la0, la1, sb.toString());
        assertTrue(la0.getSize() == 30);
        assertTrue(la1.getSize() == 30);

        sequentialRemove(la0, la1, 0, 39);
        assertLongArrayEmpty(la0);
        assertLongArrayEmpty(la1);

        sb.setLength(0);
        randomInsertion(la0, la1, 100, 10000, sb);
        assertLongArrayEqual(la0, la1, sb.toString());

        testIndexOf(la0, la1);
        }

    /*
    * Test the LongArray.Iterator interface.
    */
    private static void testIterators(LongArray la0, LongArray la1)
        {
        StringBuffer sb = new StringBuffer();
        la0.clear();
        la1.clear();

        // empty
        testIterator(la0, la1, la0.iterator(), la1.iterator(),
                     false,
                     "empty, forward iterator");
        testIterator(la0, la1, la0.reverseIterator(), la1.reverseIterator(),
                     false,
                     "empty, reverse iterator");
        try
            {
            la0.iterator().next();
            fail("failed to throw NoSuchElementException");
            }
        catch(NoSuchElementException e)
            {
            }
        try
            {
            la0.reverseIterator().next();
            fail("failed to throw NoSuchElementException");
            }
        catch(NoSuchElementException e)
            {
            }

        // sequentialInsertions
        sb.setLength(0);
        sequentialInsertion(la0, la1, 0, 39, sb);
        testIterator(la0, la1,
                     la0.iterator(), la1.iterator(),
                     false,
                     "[0,39], forward iterator");
        testIterator(la0, la1,
                     la0.reverseIterator(), la1.reverseIterator(),
                     false,
                     "[0,39], reverse iterator");

        testIterator(la0, la1,
                     la0.iterator(0), la1.iterator(0),
                     false,
                     "[0,39], forward iterator from 0");
        testIterator(la0, la1,
                     la0.reverseIterator(0), la1.reverseIterator(0),
                     false,
                     "[0,39], reverse iterator from 0");

        testIterator(la0, la1,
                     la0.iterator(25), la1.iterator(25),
                     false,
                     "[0,39], forward iterator from 25");
        testIterator(la0, la1,
                     la0.reverseIterator(25), la1.reverseIterator(25),
                     false,
                     "[0,39], reverse iterator from 25");

        testIterator(la0, la1,
                     la0.iterator(39), la1.iterator(39),
                     false,
                     "[0,39], forward iterator from 39");
        testIterator(la0, la1,
                     la0.reverseIterator(39), la1.reverseIterator(39),
                     false,
                     "[0,39], reverse iterator from 39");

        // random insertions
        la0.clear();
        la1.clear();
        assertLongArrayEmpty(la0);
        assertLongArrayEmpty(la1);

        sb.setLength(0);
        randomInsertion(la0, la1, 100, 10000, sb);
        assertLongArrayEqual(la0, la1, sb.toString());

        testIterator(la0, la1,
                     la0.iterator(0), la1.iterator(0),
                     false,
                     sb.toString() + ", forward iterator from 0");
        testIterator(la0, la1,
                     la0.reverseIterator(0), la1.reverseIterator(0),
                     false,
                     sb.toString() + ", reverse iterator from 0");
        testIterator(la0, la1,
                     la0.iterator(10000), la1.iterator(10000),
                     false,
                     sb.toString() + ", forward iterator from 10000");
        testIterator(la0, la1,
                     la0.reverseIterator(10000), la1.reverseIterator(10000),
                     false,
                     sb.toString() + ", reverse iterator from 10000");
        testIterator(la0, la1,
                     la0.iterator(5000), la1.iterator(5000),
                     false,
                     sb.toString() + ", forward iterator from 5000");
        testIterator(la0, la1,
                     la0.reverseIterator(5000), la1.reverseIterator(5000),
                     false,
                     sb.toString() + ", reverse iterator from 5000");

        // random insertions, testing Iterator mutators
        la0.clear();
        la1.clear();
        assertLongArrayEmpty(la0);
        assertLongArrayEmpty(la1);

        sb.setLength(0);
        randomInsertion(la0, la1, 100, 10000, sb);
        assertLongArrayEqual(la0, la1, sb.toString());

        testIterator(la0, la1,
                     la0.iterator(0), la1.iterator(0),
                     true,
                     sb.toString() + ", forward iterator from 0");
        testIterator(la0, la1,
                     la0.reverseIterator(0), la1.reverseIterator(0),
                     true,
                     sb.toString() + ", reverse iterator from 0");
        testIterator(la0, la1,
                     la0.iterator(10000), la1.iterator(10000),
                     true,
                     sb.toString() + ", forward iterator from 10000");
        testIterator(la0, la1,
                     la0.reverseIterator(10000), la1.reverseIterator(10000),
                     true,
                     sb.toString() + ", reverse iterator from 10000");
        testIterator(la0, la1,
                     la0.iterator(5000), la1.iterator(5000),
                     true,
                     sb.toString() + ", forward iterator from 5000");
        testIterator(la0, la1,
                     la0.reverseIterator(5000), la1.reverseIterator(5000),
                     true,
                     sb.toString() + ", reverse iterator from 5000");

        }

    /**
    * Test the LongArray.Iterator interface.
    */
    private static void testIterator(LongArray la0,
                                     LongArray la1,
                                     LongArray.Iterator iter0,
                                     LongArray.Iterator iter1,
                                     boolean fMutate,
                                     String str)
        {

        // calls to remove() before next() should throw an Exception
        try
            {
            iter0.remove();
            fail("failed to throw IllegalStateException");
            }
        catch(IllegalStateException e)
            {
            }
        try
            {
            iter1.remove();
            fail("failed to throw IllegalStateException");
            }
        catch(IllegalStateException e)
            {
            }


        while(iter0.hasNext())
            {
            assertTrue(str, iter1.hasNext());

            Object obj0 = iter0.next();
            Object obj1 = iter1.next();
            long lIdx0 = iter0.getIndex();
            long lIdx1 = iter1.getIndex();
            assertTrue(str, Base.equals(obj0, obj1));
            assertTrue(str, Base.equals(obj0, iter0.getValue()));
            assertTrue(str, Base.equals(obj1, iter1.getValue()));
            assertTrue(str, Base.equals(obj0, la0.get(iter0.getIndex())));
            assertTrue(str, Base.equals(obj1, la1.get(iter1.getIndex())));

            long lValue0 = ((Long)iter0.getValue()).longValue();
            long lValue1 = ((Long)iter1.getValue()).longValue();

            // pick some subset to mutate
            if(fMutate && (lValue0 % 3) ==0)
                {
                // test remove()
                iter0.remove();
                iter1.remove();

                // make sure that the old key/value is gone
                assertFalse(la0.exists(lIdx0));
                assertFalse(la1.exists(lIdx1));
                assertTrue(la0.get(lIdx0) == null);
                assertTrue(la1.get(lIdx1) == null);
                assertFalse(la0.contains(lValue0));
                assertFalse(la1.contains(lValue1));

                // multiple calls to remove() should throw an Exception
                try
                    {
                    iter0.remove();
                    fail("failed to throw IllegalStateException");
                    }
                catch(IllegalStateException e)
                    {
                    }
                try
                    {
                    iter1.remove();
                    fail("failed to throw IllegalStateException");
                    }
                catch(IllegalStateException e)
                    {
                    }
                }
            else
                {
                // test setValue()
                iter0.setValue(lValue0 * -1);
                iter1.setValue(lValue1 * -1);
                assertTrue(((Long)iter0.getValue()).longValue() == (lValue0 * -1));
                assertTrue(((Long)iter1.getValue()).longValue() == (lValue1 * -1));

                // make sure that the new value is there, and the old
                // value is gone
                assertTrue(la0.exists(lIdx0));
                assertTrue(la1.exists(lIdx1));
                assertTrue(la0.contains(lValue0 * -1));
                assertTrue(la1.contains(lValue1 * -1));
                assertTrue(Base.equals(la0.get(lIdx0), lValue0 * -1));
                assertTrue(Base.equals(la1.get(lIdx1), lValue1 * -1));
                }
            }
        assertTrue(str, !iter1.hasNext());
        }

    /**
    * Test indexOf()/lastIndexOf() methods
    */
    private static void testIndexOf(LongArray la0, LongArray la1)
        {
        StringBuffer sb = new StringBuffer();
        Long L500 = 500L;
        Long L20  = 20L;
        Long L21  = 21L;

        la0.clear();
        la1.clear();

        assertLongArrayEmpty(la0);
        assertLongArrayEmpty(la1);

        assertTrue("indexOf: empty",
                la0.indexOf(L500) == LongArray.NOT_FOUND);
        assertTrue("indexOf: empty",
                la1.indexOf(L500) == LongArray.NOT_FOUND);
        assertTrue("lastIndexOf: empty",
                la0.lastIndexOf(L500) == LongArray.NOT_FOUND);
        assertTrue("lastIndexOf: empty",
                la1.lastIndexOf(L500) == LongArray.NOT_FOUND);
        assertTrue("indexOf: empty",
                la0.indexOf(L500, -1) == LongArray.NOT_FOUND);
        assertTrue("indexOf: empty",
                la1.indexOf(L500, 1) == LongArray.NOT_FOUND);
        assertTrue("lastIndexOf: empty",
                la0.lastIndexOf(L500, -1) == LongArray.NOT_FOUND);
        assertTrue("lastIndexOf: empty",
                la1.lastIndexOf(L500, 1) == LongArray.NOT_FOUND);

        sb.setLength(0);
        sequentialInsertion(la0, la1, 0, 39, sb);
        assertLongArrayEqual(la0, la1, sb.toString());

        la0.set(40, 20L);
        la1.set(40, 20L);
        la0.set(41, 21L);
        la1.set(41, 21L);

        sequentialInsertion(la0, la1, 42, 49, sb);
        assertLongArrayEqual(la0, la1, sb.toString());
        assertTrue(la0.getSize() == 50);
        assertTrue(la1.getSize() == 50);

        String strTestCase = sb.toString();
        assertTrue("indexOf: "+strTestCase,
                la0.indexOf(L500) == LongArray.NOT_FOUND);
        assertTrue("indexOf: "+strTestCase,
                la1.indexOf(L500) == LongArray.NOT_FOUND);
        assertTrue("indexOf: "+strTestCase, la0.indexOf(L20) == 20);
        assertTrue("indexOf: "+strTestCase, la1.indexOf(L20) == 20);
        assertTrue("indexOf: "+strTestCase, la0.indexOf(L21) == 21);
        assertTrue("indexOf: "+strTestCase, la1.indexOf(L21) == 21);
        assertTrue("indexOf: "+strTestCase, la0.indexOf(L20, -1) == 20);
        assertTrue("indexOf: "+strTestCase, la1.indexOf(L20, -1) == 20);
        assertTrue("indexOf: "+strTestCase, la0.indexOf(L21, -1) == 21);
        assertTrue("indexOf: "+strTestCase, la1.indexOf(L21, -1) == 21);
        assertTrue("indexOf: "+strTestCase, la0.indexOf(L20, 1) == 20);
        assertTrue("indexOf: "+strTestCase, la1.indexOf(L20, 1) == 20);
        assertTrue("indexOf: "+strTestCase, la0.indexOf(L21, 1) == 21);
        assertTrue("indexOf: "+strTestCase, la1.indexOf(L21, 1) == 21);
        assertTrue("indexOf: "+strTestCase, la0.indexOf(L20, 20) == 20);
        assertTrue("indexOf: "+strTestCase, la1.indexOf(L20, 20) == 20);
        assertTrue("indexOf: "+strTestCase, la0.indexOf(L21, 21) == 21);
        assertTrue("indexOf: "+strTestCase, la1.indexOf(L21, 21) == 21);
        assertTrue("indexOf: "+strTestCase, la0.indexOf(L20, 25) == 40);
        assertTrue("indexOf: "+strTestCase, la1.indexOf(L20, 25) == 40);
        assertTrue("indexOf: "+strTestCase, la0.indexOf(L21, 25) == 41);
        assertTrue("indexOf: "+strTestCase, la1.indexOf(L21, 25) == 41);
        assertTrue("indexOf: "+strTestCase, la0.indexOf(L20, 40) == 40);
        assertTrue("indexOf: "+strTestCase, la1.indexOf(L20, 40) == 40);
        assertTrue("indexOf: "+strTestCase, la0.indexOf(L21, 41) == 41);
        assertTrue("indexOf: "+strTestCase, la1.indexOf(L21, 41) == 41);
        assertTrue("indexOf: "+strTestCase,
                la0.indexOf(L20, 42) == LongArray.NOT_FOUND);
        assertTrue("indexOf: "+strTestCase,
                la1.indexOf(L20, 42) == LongArray.NOT_FOUND);
        assertTrue("indexOf: "+strTestCase,
                la0.indexOf(L21, 42) == LongArray.NOT_FOUND);
        assertTrue("indexOf: "+strTestCase,
                la1.indexOf(L21, 42) == LongArray.NOT_FOUND);
        assertTrue("lastIndexOf: "+strTestCase,
                la0.lastIndexOf(L500) == LongArray.NOT_FOUND);
        assertTrue("lastIndexOf: "+strTestCase,
                la1.lastIndexOf(L500) == LongArray.NOT_FOUND);
        assertTrue("lastIndexOf: "+strTestCase, la0.lastIndexOf(L20) == 40);
        assertTrue("lastIndexOf: "+strTestCase, la1.lastIndexOf(L20) == 40);
        assertTrue("lastIndexOf: "+strTestCase, la0.lastIndexOf(L21) == 41);
        assertTrue("lastIndexOf: "+strTestCase, la1.lastIndexOf(L21) == 41);
        assertTrue("lastIndexOf: "+strTestCase,
                la0.lastIndexOf(L20, -1) == LongArray.NOT_FOUND);
        assertTrue("lastIndexOf: "+strTestCase,
                la1.lastIndexOf(L20, -1) == LongArray.NOT_FOUND);
        assertTrue("lastIndexOf: "+strTestCase,
                la0.lastIndexOf(L21, -1) == LongArray.NOT_FOUND);
        assertTrue("lastIndexOf: "+strTestCase,
                la1.lastIndexOf(L21, -1) == LongArray.NOT_FOUND);
        assertTrue("lastIndexOf: "+strTestCase,
                la0.lastIndexOf(L20, 1) == LongArray.NOT_FOUND);
        assertTrue("lastIndexOf: "+strTestCase,
                la1.lastIndexOf(L20, 1) == LongArray.NOT_FOUND);
        assertTrue("lastIndexOf: "+strTestCase,
                la0.lastIndexOf(L21, 1) == LongArray.NOT_FOUND);
        assertTrue("lastIndexOf: "+strTestCase,
                la1.lastIndexOf(L21, 1) == LongArray.NOT_FOUND);
        assertTrue("lastIndexOf: "+strTestCase, la0.lastIndexOf(L20, 20) == 20);
        assertTrue("lastIndexOf: "+strTestCase, la1.lastIndexOf(L20, 20) == 20);
        assertTrue("lastIndexOf: "+strTestCase, la0.lastIndexOf(L21, 21) == 21);
        assertTrue("lastIndexOf: "+strTestCase, la1.lastIndexOf(L21, 21) == 21);
        assertTrue("lastIndexOf: "+strTestCase, la0.lastIndexOf(L20, 25) == 20);
        assertTrue("lastIndexOf: "+strTestCase, la1.lastIndexOf(L20, 25) == 20);
        assertTrue("lastIndexOf: "+strTestCase, la0.lastIndexOf(L21, 25) == 21);
        assertTrue("lastIndexOf: "+strTestCase, la1.lastIndexOf(L21, 25) == 21);
        assertTrue("lastIndexOf: "+strTestCase, la0.lastIndexOf(L20, 40) == 40);
        assertTrue("lastIndexOf: "+strTestCase, la1.lastIndexOf(L20, 40) == 40);
        assertTrue("lastIndexOf: "+strTestCase, la0.lastIndexOf(L21, 41) == 41);
        assertTrue("lastIndexOf: "+strTestCase, la1.lastIndexOf(L21, 41) == 41);
        assertTrue("lastIndexOf: "+strTestCase, la0.lastIndexOf(L20, 42) == 40);
        assertTrue("lastIndexOf: "+strTestCase, la1.lastIndexOf(L20, 42) == 40);
        assertTrue("lastIndexOf: "+strTestCase, la0.lastIndexOf(L21, 42) == 41);
        assertTrue("lastIndexOf: "+strTestCase, la1.lastIndexOf(L21, 42) == 41);
        }

    /**
    * Sequentially remove items in la0 and la1, between lStartKey
    * and lEndKey.
    */
    private static void sequentialRemove(LongArray la0,
                                         LongArray la1,
                                         long lStartKey,
                                         long lEndKey)
        {
        for(long l = lStartKey; l <= lEndKey; l++)
            {
            la0.remove(l);
            la1.remove(l);
            }
        }

    /**
    * Sequentially insert items in la0 and la1, between lStartKey
    * and lEndKey.
    */
    private static void sequentialInsertion(LongArray la0,
                                            LongArray la1,
                                            long lStartKey,
                                            long lEndKey,
                                            StringBuffer sb)
        {
        sb.append("Sequential insertion: [");
        sb.append(lStartKey);
        sb.append(", ");
        sb.append(lEndKey);
        sb.append("]");
        for(long l = lStartKey; l <= lEndKey; l++)
            {
            la0.set(l, l);
            la1.set(l, l);
            }
        }

    /**
    * Randomly insert nIters items in la0 and la1, and record the
    * insertion-order in sb.
    */
    private static void randomInsertion(LongArray la0,
                                        LongArray la1,
                                        int nIters,
                                        int iMax,
                                        StringBuffer sb)
        {
        Random rng = new Random(System.currentTimeMillis());
        sb.append("Random insertion: [");
        for (int i = 0; i < nIters; i++)
            {
            int key = rng.nextInt(iMax);

            la0.set((long)key, (long) key);
            la1.set((long)key, (long) key);

            sb.append(key);
            sb.append(", ");
            }
        sb.append("]");
        }

    /**
    * Assert that la represents an empty LongArray.
    */
    private static void assertLongArrayEmpty(LongArray la)
        {
        assertTrue(la.isEmpty());
        assertTrue(la.getFirstIndex() == LongArray.NOT_FOUND);
        assertTrue(la.getLastIndex() == LongArray.NOT_FOUND);
        assertTrue(la.getSize() == 0);
        }

    /**
    * Assert that la0 and la1 are equivalent LongArrays.
    */
    private static void assertLongArrayEqual(LongArray la0,
                                             LongArray la1,
                                             String str)
        {
        if ((la0 instanceof LongArray) && (la1 instanceof LongArray))
            {
            assertTrue(str, la0.getFirstIndex() == la1.getFirstIndex());
            assertTrue(str, la0.getLastIndex() == la1.getLastIndex());
            assertTrue(str, la0.getSize() == la1.getSize());

            // short-cut:  both are empty?
            if (la0.isEmpty())
                 {
                 return;
                 }

            // perform an in-order traversal, comparing each element
            Iterator iter0 = la0.iterator();
            Iterator iter1 = la1.iterator();

            while (iter0.hasNext() && iter1.hasNext())
                {
                Object obj0 = iter0.next();
                Object obj1 = iter1.next();
                assertTrue(str, la0.contains(obj0));
                assertTrue(str, la1.contains(obj1));

                assertTrue(str+":"+obj0+" != "+obj1,
                        Base.equals(obj0, obj1));
                assertTrue(str+":"+iter0.getIndex()+" != "+iter1.getIndex(),
                        iter0.getIndex() == iter1.getIndex());
                }
            return;
            }
        }

    /**
    * Assert that la0 and la1 are equivalent LongArrays.
    */
    private static void assertLongArrayEqual(LongArray la0, LongArray la1)
        {
        assertLongArrayEqual(la0, la1, null);
        }

    /**
    * Reference implementation of LongArray, backed by a
    * java.util.TreeMap.
    */
    public static class ReferenceImpl implements LongArray
        {
        SortedMap m_map;
        int m_iNonce;

        ReferenceImpl()
            {
            this(new TreeMap());
            }

        ReferenceImpl(SortedMap map)
            {
            m_map = map;
            }

        /**
         * Return the value stored at the specified index.
         *
         * @param lIndex  a long index value
         *
         * @return the object stored at the specified index, or null
         */
        public Object get(long lIndex)
            {
            return m_map.get(lIndex);
            }

         /**
         * Add the passed item to the LongArray at the specified index.
         * <p>
         * If the index is already used, the passed value will replace the current
         * value stored with the key, and the replaced value will be returned.
         * <p>
         * It is expected that LongArray implementations will "grow" as necessary
         * to support the specified index.
         *
         * @param lIndex  a long index value
         * @param oValue  the object to store at the specified index
         *
         * @return the object that was stored at the specified index, or null
         */
        public Object set(long lIndex, Object oValue)
            {
            m_iNonce++;
            return m_map.put(lIndex, oValue);
            }

        /**
         * Add the passed element value to the LongArray and return the index at
         * which the element value was stored.
         *
         * @param oValue  the object to add to the LongArray
         *
         * @return  the long index value at which the element value was stored
         */
        public long add(Object oValue)
            {
            long lIdx = getLastIndex() + 1;
            set(lIdx, oValue);
            return lIdx;
            }

        /**
         * Determine if the specified index is in use.
         *
         * @param lIndex  a long index value
         *
         * @return true if a value (including null) is stored at the specified
         *         index, otherwise false
         */
        public boolean exists(long lIndex)
            {
            return m_map.containsKey(lIndex);
            }

        /**
         * Remove the specified index from the LongArray, returning its associated
         * value.
         *
         * @param lIndex  the index into the LongArray
         *
         * @return the associated value (which can be null) or null if the
         *         specified index is not in the LongArray
         */
        public Object remove(long lIndex)
            {
            m_iNonce++;
            return m_map.remove(lIndex);
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void remove(long lIndexFrom, long lIndexTo)
            {
            for (long i = lIndexFrom; i < lIndexTo; ++i)
                {
                remove(i);
                }
            }

        /**
         * Determine if the LongArray contains the specified element.
         * <p>
         * More formally, returns <tt>true</tt> if and only if this LongArray
         * contains at least one element <tt>e</tt> such that
         * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>.
         *
         * @param oValue  element whose presence in this list is to be tested
         *
         * @return <tt>true</tt> if this list contains the specified element
         */
        public boolean contains(Object oValue)
            {
            return m_map.containsValue(oValue);
            }

        /**
         * Remove all nodes from the LongArray.
         */
        public void clear()
            {
            m_iNonce++;
            m_map.clear();
            }

        /**
         * Test for empty LongArray.
         *
         * @return true if LongArray has no nodes
         */
        public boolean isEmpty()
            {
            return m_map.isEmpty();
            }

        /**
         * Determine the size of the LongArray.
         *
         * @return the number of nodes in the LongArray
         */
        public int getSize()
            {
            return m_map.size();
            }

        /**
        * {@inheritDoc}
        */
        public long floorIndex(long lIndex)
            {
            LongArray.Iterator iter = reverseIterator(lIndex);
            if (iter.hasNext())
                {
                iter.next();
                return iter.getIndex();
                }
            return NOT_FOUND;
            }

        /**
         * {@inheritDoc}
         */
        public Object floor(long lIndex)
            {
            return get(floorIndex(lIndex));
            }

        /**
         * {@inheritDoc}
         */
        public long ceilingIndex(long lIndex)
            {
            LongArray.Iterator iter = iterator(lIndex);
            if (iter.hasNext())
                {
                iter.next();
                return iter.getIndex();
                }
            return NOT_FOUND;
            }

        /**
         * {@inheritDoc}
         */
        public Object ceiling(long lIndex)
            {
            return get(ceilingIndex(lIndex));
            }

        /**
         * Obtain a LongArray.Iterator of the contents of the LongArray in
         * order of increasing indices.
         *
         * @return an instance of LongArray.Iterator
         */
        public LongArray.Iterator iterator()
            {
            return iterator(getFirstIndex());
            }

        /**
         * Obtain a LongArray.Iterator of the contents of the
         * LongArray in order of increasing indices, starting at a
         * particular index such that the first call to <tt>next</tt>
         * will set the location of the iterator at the first existent
         * index that is greater than or equal to the specified index,
         * or will throw a NoSuchElementException if there is no such
         * existent index.
         *
         * @param lIndex  the LongArray index to iterate from
         *
         * @return an instance of LongArray.Iterator
         */
        public LongArray.Iterator iterator(long lIndex)
            {
             Set setKey = m_map.tailMap(lIndex).keySet();
             Object[] aKey = setKey.toArray(new Object[setKey.size()]);
             Arrays.sort(aKey);
             return new Iterator(aKey);
            }

        /**
         * Obtain a LongArray.Iterator of the contents of the LongArray in
         * reverse order (decreasing indices).
         *
         * @return an instance of LongArray.Iterator
         */
        public LongArray.Iterator reverseIterator()
            {
            return reverseIterator(getLastIndex());
            }
        /**
         * Obtain a LongArray.Iterator of the contents of the
         * LongArray in reverse order (decreasing indices), starting
         * at a particular index such that the first call to
         * <tt>next</tt> will set the location of the iterator at the
         * first existent index that is less than or equal to the
         * specified index, or will throw a NoSuchElementException if
         * there is no such existent index.
         *
         * @param lIndex  the LongArray index to iterate from
         *
         * @return an instance of LongArray.Iterator
         */
        public LongArray.Iterator reverseIterator(long lIndex)
            {
             Set setKey = m_map.headMap(lIndex + 1).keySet();
             Object[] aKey = setKey.toArray(new Object[setKey.size()]);
             Arrays.sort(aKey, Collections.reverseOrder());
             return new Iterator(aKey);
            }

        /**
        * Determine the first index that exists in the LongArray.
        *
        * @return the lowest long value that exists in this LongArray,
        *         or NOT_FOUND if the LongArray is empty
        */
        public long getFirstIndex()
            {
            return (m_map.isEmpty() ?
                    NOT_FOUND :
                    ((Long)m_map.firstKey()).longValue());
            }

        /**
        * Determine the last index that exists in the LongArray.
        *
        * @return the highest long value that exists in this LongArray,
        *         or NOT_FOUND if the LongArray is empty
        */
        public long getLastIndex()
            {
            return (m_map.isEmpty() ?
                    NOT_FOUND :
                    ((Long)m_map.lastKey()).longValue());
            }

        /**
        * Return the index in this LongArray of the first occurrence
        * of the specified element, or NOT_FOUND if this LongArray
        * does not contain the specified element.
        */
        public long indexOf(Object oValue)
            {
            return indexOf(oValue, getFirstIndex());
            }

        /**
        * Return the index in this LongArray of the first occurrence
        * of the specified element such that <tt>(index >=
        * lIndex)</tt>, or NOT_FOUND if this LongArray does not
        * contain the specified element.
        */
        public long indexOf(Object oValue, long lIndex)
            {
            LongArray.Iterator iter = iterator(lIndex);
            while(iter.hasNext())
                {
                if (Base.equals(oValue, iter.next()))
                    {
                    return iter.getIndex();
                    }
                }
            return NOT_FOUND;
            }

        /**
        * Return the index in this LongArray of the last occurrence of
        * the specified element, or NOT_FOUND if this LongArray does
        * not contain the specified element.
        */
        public long lastIndexOf(Object oValue)
            {
            return lastIndexOf(oValue, getLastIndex());
            }

        /**
        * Return the index in this LongArray of the last occurrence of
        * the specified element such that <tt>(index <= lIndex)</tt>,
        * or NOT_FOUND if this LongArray does not contain the
        * specified element.
        */
        public long lastIndexOf(Object oValue, long lIndex)
            {
            LongArray.Iterator iter = reverseIterator(lIndex);
            while(iter.hasNext())
                {
                if (Base.equals(oValue, iter.next()))
                    {
                    return iter.getIndex();
                    }
                }
            return NOT_FOUND;
            }


        /**
         * Make a clone of the LongArray. The element values are not deep-cloned.
         *
         * @return a clone of this LongArray object
         */
        public ReferenceImpl clone()
            {
            return new ReferenceImpl(new TreeMap(m_map));
            }

        public class Iterator implements LongArray.Iterator
            {
            Object[] m_aKey;
            int      m_iKey;
            int      m_iNonce;
            Iterator(Object[] aKey)
                {
                m_aKey = aKey;
                m_iKey = 0;
                m_iNonce = ReferenceImpl.this.m_iNonce;
                }
            /**
             * Returns <tt>true</tt> if the iteration has more elements. (In other
             * words, returns <tt>true</tt> if <tt>next</tt> would return an
             * element rather than throwing an exception.)
             *
             * @return <tt>true</tt> if the iterator has more elements
             */
            public boolean hasNext()
                {
                return m_iKey < m_aKey.length;
                }

            /**
             * Returns the next element in the iteration.
             *
             * @return the next element in the iteration
             *
             * @exception NoSuchElementException iteration has no more elements
             */
            public Object next()
                {
                checkComod();
                if (!hasNext())
                    {
                    throw new NoSuchElementException();
                    }
                return m_map.get(m_aKey[m_iKey++]);
                }
             /**
             * Returns the index of the current value, which is the value returned
             * by the most recent call to the <tt>next</tt> method.
             *
             * @exception IllegalStateException if the <tt>next</tt> method has
             *		  not yet been called, or the <tt>remove</tt> method has
             *		  already been called after the last call to the
             *		  <tt>next</tt> method.
             */
            public long getIndex()
                {
                if (m_iKey == 0)
                    {
                    throw new IllegalStateException();
                    }
                return ((Long)m_aKey[m_iKey - 1]).longValue();
                }
             /**
             * Returns the current value, which is the same value returned by the
             * most recent call to the <tt>next</tt> method, or the most recent
             * value passed to <tt>setValue</tt> if <tt>setValue</tt> were called
             * after the <tt>next</tt> method.
             *
             * @return  the current value
             *
             * @exception IllegalStateException if the <tt>next</tt> method has
             *		  not yet been called, or the <tt>remove</tt> method has
             *		  already been called after the last call to the
             *		  <tt>next</tt> method.
             */
            public Object getValue()
                {
                if (m_iKey == 0)
                    {
                    throw new IllegalStateException();
                    }
                return m_map.get(m_aKey[m_iKey - 1]);
                }

            /**
             * Stores a new value at the current value index, returning the value
             * that was replaced. The index of the current value is obtainable by
             * calling the <tt>getIndex</tt> method.
             *
             * @return  the replaced value
             *
             * @exception IllegalStateException if the <tt>next</tt> method has
             *		  not yet been called, or the <tt>remove</tt> method has
             *		  already been called after the last call to the
             *		  <tt>next</tt> method.
             */
            public Object setValue(Object oValue)
                {
                if (m_iKey == 0)
                    {
                    throw new IllegalStateException();
                    }
                return m_map.put(m_aKey[m_iKey - 1], oValue);
                }

             /**
             * Removes from the underlying collection the last element returned by
             * the iterator (optional operation).  This method can be called only
             * once per call to <tt>next</tt>.  The behavior of an iterator is
             * unspecified if the underlying collection is modified while the
             * iteration is in progress in any way other than by calling this
             * method.
             *
             * @exception UnsupportedOperationException if the <tt>remove</tt>
             *		  operation is not supported by this Iterator
             * @exception IllegalStateException if the <tt>next</tt> method has
             *		  not yet been called, or the <tt>remove</tt> method has
             *		  already been called after the last call to the
             *		  <tt>next</tt> method.
             */
            public void remove()
                {
                checkComod();
                if (m_iKey == 0 || m_aKey[m_iKey - 1] == null)
                    {
                    throw new IllegalStateException();
                    }
                m_map.remove(m_aKey[m_iKey - 1]);
                m_aKey[m_iKey - 1] = null;
                }

            private void checkComod()
                {
                if (m_iNonce != ReferenceImpl.this.m_iNonce)
                    {
                    throw new ConcurrentModificationException();
                    }
                }
            }
        }
    }
