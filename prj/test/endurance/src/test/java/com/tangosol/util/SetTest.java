/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;


import com.oracle.coherence.testing.util.OldLiteSet;

import com.tangosol.internal.util.UnsafeSubSet;

import com.tangosol.io.ExternalizableLite;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;

import java.lang.reflect.Method;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.Assert.*;


/**
 * A collection of tests designed to test Set implementations.
 * <p/>
 * A Set is not just a single abstract data structure, since it can return an
 * Iterator.
 * <p/>
 * <pre>
 *   Set
 *     size
 *     isEmpty
 *     contains
 *     iterator
 *     toArray
 *     toArray
 *     add - not supported
 *     remove
 *     containsAll
 *     addAll - not supported
 *     removeAll
 *     retainAll
 *     clear
 *     equals
 *     hashCode
 *
 *   Iterator (values, keySet, entrySet)
 *     hasNext
 *     next
 *     remove
 * </pre>
 * <p/>
 * Fortunately, Java provides a number of "control cases", such as the HashSet
 * implementation, that allow tests to be made to compare the results of
 * querying two supposedly identical data structures, one of which is a
 * control case. In this manner, any result that differs from the control
 * case (i.e. violates the Set contract) is a test failure.
 *
 * @author cp  2006.08.01  based on MapTest
 */
public class SetTest
        extends Base
    {
    // ----- unit tests for the test helpers themselves ---------------------

    /**
     * Invoke {@link #assertIdenticalSets} with a Set and itself.
     */
    @Test
    public void testSetReflexively()
        {
        Set set = new HashSet();
        assertIdenticalSets(set, set);

        set.add("a");
        assertIdenticalSets(set, set);

        set.add("b");
        set.add("c");
        assertIdenticalSets(set, set);
        }

    /**
     * Invoke {@link #assertIdenticalSets} with two identical Sets that are
     * known to work.
     */
    @Test
    public void testIdenticalSets()
        {
        Set set1 = new HashSet();
        Set set2 = new TreeSet();
        assertIdenticalSets(set1, set2);

        set1.add("a");
        set2.add("a");
        assertIdenticalSets(set1, set2);

        set1.add("b");
        set2.add("b");
        set1.add("c");
        set2.add("c");
        assertIdenticalSets(set1, set2);
        }

    /**
     * Invoke {@link #assertIdenticalSets} with two identical Sets that are
     * known to work, but with a slight difference in their contents.
     */
    @Test
    public void testDifferentSets()
        {
        Set set1 = new HashSet();
        Set set2 = new TreeSet();
        set1.add("a");
        set2.add("a");
        set1.add("b");
        set2.add("b");
        set1.add("c");
        set2.add("c");
        set1.add("d");
        assertFalse(set1.size() == set2.size());
        }


    // ----- general tests for known Set implementations --------------------

    /**
     * Test the Set implementations that come with the JDK.
     */
    @Test
    public void testJavaSets()
        {
        // use the test helpers against known working (JDK) implementations
        out("testing HashSet ...");
        testSet(new HashSet());
        out("testing HashSet versus TreeSet ...");
        testSets(new HashSet(), new TreeSet());
        }

    /**
     * Test LiteSet.
     */
    @Test
    public void testLiteSet()
        {
        out("testing LiteSet ...");
        testSet(new LiteSet());
        }

    /**
     * Test OldLiteSet.
     */
    @Test
    public void testOldLiteSet()
        {
        out("testing OldLiteSet ...");
        testSet(new OldLiteSet());
        }

    /**
     * Test LiteSet versus older implementation.
     */
    @Test
    public void testLiteSetSpeed()
        {
        out("testing LiteSet versus OldLiteSet ...");

        comparePerformance(new DoNothingSet(), new LiteSet(), 10, 100000);
        comparePerformance(new OldLiteSet(), new LiteSet(), 10, 100000);
        }

    /**
     * Test InflatableSet.
     */
    @Test
    public void testInflatableSet()
        {
        out("testing InflatableSet ...");
        testSet(new InflatableSet());
        }

    /**
     * Test OpenHashSet.
     */
    @Test
    public void testOpenHashSet()
        {
        out("testing OpenHashSet ...");
        testSet(new OpenHashSet());
        }

    /**
     * Test StableSubSet.
     */
    @Test
    public void testSubSet()
        {
        out("testing testSubSet ...");
        HashSet<String> setControl = new HashSet<String>();
        setControl.add("hello world");
        setControl.add("hello world");
        setControl.add("hello again");
        setControl.add("hello2 again");
        setControl.add("hello2 again2");


        for (int i = 0; i < 100; ++i)
            {
            String s = String.valueOf(i);
            setControl.add(s);
            }

        testSubSet(setControl, new SubSet(new HashSet<String>(setControl)));
        }

    /**
     * Test StableSubSet.
     */
    @Test
    public void testStableSubSet()
        {
        out("testing testStableSubSet ...");
        HashSet<String> setControl = new HashSet<String>();
        setControl.add("hello world");
        setControl.add("hello world");
        setControl.add("hello again");
        setControl.add("hello2 again");
        setControl.add("hello2 again2");


        for (int i = 0; i < 100; ++i)
            {
            String s = String.valueOf(i);
            setControl.add(s);
            }

        testSubSet(setControl, new UnsafeSubSet<String>(new HashSet<String>(setControl),
            NullImplementation.getBackingMapManagerContext(), null));
        }

    /**
     * Test sorted SubSet.
     */
    @Test
    public void testSortedSubSet()
        {
        out("testing testSortedSubSet ...");
        Set<Integer> setOrig = new TreeSet();

        for (int i = 0; i < 20; ++i)
            {
            setOrig.add(i);
            }

        Set<Integer> setPart = new HashSet();
        for (int i = 10; i < 20; ++i)
            {
            setPart.add(i);
            }

        SubSet set = new SubSet(setOrig);
        set.retainAll(setPart);
        assertEquals(10, set.iterator().next());

        set = new SubSet(setOrig);
        set.removeAll(setPart);
        assertEquals(0, set.iterator().next());
        }

    /**
     * Test InflatableSet versus HashSet implementation.
     */
    @Test
    public void testInflatableSetSpeed()
        {
        out("testing LiteSet versus OldLiteSet ...");

        comparePerformance(new DoNothingSet(), new InflatableSet(), 10, 100000);
        comparePerformance(new SegmentedHashSet(), new InflatableSet(), 10, 100000);
        }

    /**
     * Test MapSet.
     */
    @Test
    public void testMapSet()
        {
        out("testing MapSet ...");
        testSet(new MapSet());
        }

    /**
     * Test SafeHashSet.
     */
    @Test
    public void testSafeHashSet()
        {
        out("testing SafeHashSet ...");
        testSet(new SafeHashSet());
        }


    // ----- basic Set tests ------------------------------------------------

    /**
     * Test the basic operations of a Set. The test will leave the Set empty
     * at its successful conclusion.
     *
     * @param setTest  the Set to test
     */
    public static void testSet(Set setTest)
        {
        Set setControl = new HashSet();
        testSets(setControl, setTest);
        testRnd(setControl, setTest);
        }

    /**
     * Test the basic operations of a Set against a control Set and a test
     * Set. The test will leave both Sets empty at its successful conclusion.
     *
     * @param setControl  the Set for a control case
     * @param setTest     the Set to test
     */
    public static void testSets(Set setControl, Set setTest)
        {
        assertIdenticalSets(setControl, setTest);

        // test single operations
        boolean fControl = setControl.add("hello world");
        boolean fTest = setTest.add("hello world");
        assertIdenticalResult(fControl, fTest);
        assertIdenticalSets(setControl, setTest);

        fControl = setControl.add("hello world");
        fTest = setTest.add("hello world");
        assertIdenticalResult(fControl, fTest);
        assertIdenticalSets(setControl, setTest);

        fControl = setControl.add("hello again");
        fTest = setTest.add("hello again");
        assertIdenticalResult(fControl, fTest);
        assertIdenticalSets(setControl, setTest);

        fControl = setControl.add("hello2 again");
        fTest = setTest.add("hello2 again");
        assertIdenticalResult(fControl, fTest);
        assertIdenticalSets(setControl, setTest);

        fControl = setControl.add("hello2 again2");
        fTest = setTest.add("hello2 again2");
        assertIdenticalResult(fControl, fTest);
        assertIdenticalSets(setControl, setTest);

        for (int iRepeat = 0; iRepeat < 2; ++iRepeat)
            {
            for (int i = 0; i < 100; ++i)
                {
                Integer I = i;
                fControl = setControl.add("" + I);
                fTest = setTest.add("" + I);
                assertIdenticalResult(fControl, fTest);
                assertIdenticalSets(setControl, setTest);
                }
            }

        // make a copy of what we have already
        Set setData = new HashSet();
        setData.addAll(setControl);

        for (int iRepeat = 0; iRepeat < 2; ++iRepeat)
            {
            for (int i = 0; i < 100; ++i)
                {
                Integer I = i;
                fControl = setControl.remove("" + I);
                fTest = setTest.remove("" + I);
                assertIdenticalResult(fControl, fTest);
                assertIdenticalSets(setControl, setTest);
                }
            }

        // test bulk operations
        setControl.clear();
        setTest.clear();
        assertIdenticalSets(setControl, setTest);

        for (int iRepeat = 0; iRepeat < 2; ++iRepeat)
            {
            setControl.addAll(setData);
            setTest.addAll(setData);
            assertIdenticalSets(setControl, setTest);
            }

        setControl.clear();
        setTest.clear();
        assertIdenticalSets(setControl, setTest);
        }

    /**
     * Test the basic operations of a Set against a control Set and a test
     * Set. The test will leave both Sets empty at its successful conclusion.
     *
     * @param setControl  the Set for a control case
     * @param setTest     the Set to test
     */
    public static <E> void testSubSet(Set<E> setControl, Set<E> setTest)
        {
        assertIdenticalSets(setControl, setTest);

        boolean fControl, fTest;
        // make a copy of what we have already
        Set setData = new HashSet<E>();
        setData.addAll(setControl);

        for (int iRepeat = 0; iRepeat < 2; ++iRepeat)
            {
            for (int i = 0; i < 100; ++i)
                {
                String s = String.valueOf(i);
                fControl = setControl.remove(s);
                fTest = setTest.remove(s);
                assertIdenticalResult(fControl, fTest);
                assertIdenticalSets(setControl, setTest);
                }
            }

        // test bulk operations
        setControl.clear();
        setTest.clear();
        assertIdenticalSets(setControl, setTest);

        for (int iRepeat = 0; iRepeat < 2; ++iRepeat)
            {
            setControl.addAll(setData);
            setTest.addAll(setData);
            assertIdenticalSets(setControl, setTest);
            }

        setControl.clear();
        setTest.clear();
        assertIdenticalSets(setControl, setTest);
        }


    // ----- test helper methods for comparing Sets -------------------------


    /**
     * Compare two return values for equality.
     *
     * @param f1  the first result
     * @param f2  the second result
     */
    public static void assertIdenticalResult(boolean f1, boolean f2)
        {
        if (f1 != f2)
            {
            fail("Result values are different: Result 1={" + f1 + "}, Result 2={" + f2 + "}");
            }
        }

    /**
     * Compare two return values for equality.
     *
     * @param o1  the first result
     * @param o2  the second result
     */
    public static void assertIdenticalResult(Object o1, Object o2)
        {
        if (!equalsDeep(o1, o2))
            {
            fail("Result values are different: Result 1={" + o1 + "}, Result 2={" + o2 + "}");
            }
        }

    /**
     * Compare two Set objects for equality.
     * <pre>
     *   Collection and Set (values, keySet, entrySet)
     *     size
     *     isEmpty
     *     contains
     *     iterator
     *     toArray
     *     toArray
     *     remove
     *     containsAll
     *     equals
     *     hashCode
     *
     *   Iterator (values, keySet, entrySet)
     *     hasNext
     *     next
     *
     *   Entry
     *     getKey
     *     getValue
     *     equals
     *     hashCode
     * </pre>
     *
     * @param set1  the first Set to compare
     * @param set2  the second Set to compare
     */
    public static void assertIdenticalSets(Set set1, Set set2)
        {
        // size()
        int cSet1Items = set1.size();
        int cSet2Items = set2.size();
        if (cSet1Items != cSet2Items)
            {
            fail("Sizes are different for Sets:" + " Set 1 class=" + set1.getClass().getName() + ", Set 2 class="
                    + set2.getClass().getName() + ", size1=" + cSet1Items + ", size2=" + cSet2Items);
            }

        // isEmpty()
        boolean fSet1Empty = set1.isEmpty();
        boolean fSet2Empty = set2.isEmpty();
        if (fSet1Empty != fSet2Empty)
            {
            fail("isEmpty is different for Sets:" + " Set 1 class=" + set1.getClass().getName() + ", Set 2 class="
                    + set2.getClass().getName() + ", isEmpty1=" + fSet1Empty + ", isEmpty2=" + fSet2Empty);
            }

        // toArray()
        assertToArrayMatchesCollection(set1);
        assertToArrayMatchesCollection(set2);

        // iterator()
        assertIteratorMatchesCollection(set1);
        assertIteratorMatchesCollection(set2);

        // equals
        boolean fSet1EqSet2 = set1.equals(set2);
        boolean fSet2EqSet1 = set2.equals(set1);
        if (!(fSet1EqSet2 && fSet2EqSet1))
            {
            fail("Sets are not equal:" + " Set 1 class=" + set1.getClass().getName() + ", Set 2 class="
                    + set2.getClass().getName() + ", 1.equals(2)=" + fSet1EqSet2 + ", 2.equals(1)=" + fSet2EqSet1);
            }

        // hashCode
        int nSet1Hash = set1.hashCode();
        int nSet2Hash = set2.hashCode();
        if (nSet1Hash != nSet2Hash)
            {
            fail("Hash values are different for Sets:" + " Set 1 class=" + set1.getClass().getName() + ", Set 2 class="
                    + set2.getClass().getName() + ", hash1=" + nSet1Hash + ", hash2=" + nSet2Hash);
            }
        }

    /**
     * Test the toArray operations of a Collection.
     *
     * @param coll  the Collection to test
     */
    public static void assertToArrayMatchesCollection(Collection coll)
        {
        int cItems = coll.size();

        assertArrayEqualsCollection(coll.toArray(), coll);
        assertArrayEqualsCollection(coll.toArray(new Object[0]), coll);
        assertArrayEqualsCollection(coll.toArray(new Object[cItems]), coll);

        // fill a too-big array with test values to make sure they get
        // over-written correctly (except for the last one, which will
        // be retained)
        int co = cItems + 2;
        Object[] ao = new Object[co];
        for (int i = 0; i < co; ++i)
            {
            ao[i] = "test dummy " + i;
            }
        Object oRetain = ao[cItems + 1];

        // since the array was big enough, verify that toArray() returned
        // the same array as was passed in
        Object[] aoSame = coll.toArray(ao);
        if (ao != aoSame)
            {
            fail("toArray(Object[]) into an oversized array from a Collection" + " returned a different array: class="
                    + coll.getClass().getName() + ", size=" + cItems + ", oversized array.length=" + co
                    + ", returned array.length=" + aoSame.length);
            }
        else
            {
            // verify that toArray() capped the used part of the array with
            // a null element
            if (ao[cItems] != null)
                {
                fail("toArray(Object[]) into an oversized array from a"
                        + " Collection did not null the following element: class=" + coll.getClass().getName()
                        + ", size=" + cItems + ", oversized array.length=" + co + ", element[" + cItems + "]=\""
                        + ao[cItems] + "\"");
                }

            // verify that toArray() didn't muck past the part it was allowed
            // to copy items to
            if (ao[cItems + 1] != oRetain)
                {
                fail("toArray(Object[]) into an oversized array from a"
                        + " Collection changed an element beyond the following" + " element: class="
                        + coll.getClass().getName() + ", size=" + cItems + ", oversized array.length=" + co
                        + ", element[" + (cItems + 1) + "]=\"" + ao[cItems + 1] + "\"");
                }

            // keep only the exact number of items that we have to test for
            // Collection equality
            Object[] aoChop = new Object[cItems];
            System.arraycopy(ao, 0, aoChop, 0, cItems);
            assertArrayEqualsCollection(aoChop, coll);
            }
        }

    /**
     * Compare an array of objects to a Collection of objects for equality.
     *
     * @param ao    the array to compare
     * @param coll  the Collection to compare it to
     */
    public static void assertArrayEqualsCollection(Object[] ao, Collection coll)
        {
        // size() vs. array.length
        int cItems = coll.size();
        int co = ao.length;
        if (co != cItems)
            {
            fail("toArray length is different from size for Collection: class=" + coll.getClass().getName() + ", size="
                    + cItems + ", toArray.length=" + co);
            }

        // contains()
        for (int i = 0; i < co; ++i)
            {
            if (!coll.contains(ao[i]))
                {
                fail("toArray contained an item that the Collection does not contain:" + " class="
                        + coll.getClass().getName() + ", index=" + i + ", element=" + ao[i]);
                }
            }
        }

    /**
     * Test the iterator operations of a Collection.
     *
     * @param coll  the Collection to test
     */
    public static void assertIteratorMatchesCollection(Collection coll)
        {
        // contains()
        int cIters = 0;
        for (Iterator iter = coll.iterator(); iter.hasNext();)
            {
            Object o = iter.next();
            if (!coll.contains(o))
                {
                fail("Iterated an item that the Collection does not contain:" + " class=" + coll.getClass().getName()
                        + ", index=" + cIters + ", object=\"" + o + "\"");
                }
            ++cIters;
            }

        // size() vs. Iterator iterations
        int cItems = coll.size();
        if (cIters != cItems)
            {
            fail("Iterated count is different from size for Collection:" + " class=" + coll.getClass().getName()
                    + ", size=" + cItems + ", Iterated count=" + cIters);
            }
        }


    // ----- randomized tests -----------------------------------------------

    /**
     * Randomize a test.
     *
     * @param setControl  the control case
     * @param setTest     the test case
     */
    public static void testRnd(Set setControl, Set setTest)
        {
        boolean fLite = false;
        for (int i = 0; i < 20; ++i)
            {
            // alternate between serializable and ExternalizableLite
            fLite = !fLite;

            TestOp[] aop = generateTest(10000, fLite);
            applyTest(aop, setControl, setTest);

            testSerializable(setControl);
            testSerializable(setTest);

            testExternalizableLite(setControl);
            testExternalizableLite(setTest);

            testCloneable(setControl);
            testCloneable(setTest);

            setControl.clear();
            setTest.clear();
            assertIdenticalSets(setControl, setTest);
            }
        }

    /**
     * Generate an array of test operations.
     *
     * @param cIters  the number of ops to generate
     * @param fLite   true to use ExternalizableLite objects
     *
     * @return an array of <tt>cIters</tt> test operations
     */
    public static TestOp[] generateTest(int cIters, boolean fLite)
        {
        Set set = new HashSet();
        TestOp[] aop = new TestOp[cIters];
        for (int i = 0; i < cIters; ++i)
            {
            TestOp op = s_rnd.nextInt(4) == 0 ? (TestOp) new MultiOp() : new SingleOp();
            op.init(set, fLite);
            op.apply(set);
            aop[i] = op;
            }
        return aop;
        }

    /**
     * Execute an array of test operations.
     *
     * @param aop         the test ops
     * @param setControl  the control case
     * @param setTest     the test case
     */
    public static void applyTest(TestOp[] aop, Set setControl, Set setTest)
        {
        TestOp opPrev = null;
        for (int i = 0, c = aop.length; i < c; ++i)
            {
            TestOp op = aop[i];
            Object oResult1 = op.apply(setControl);
            Object oResult2 = op.apply(setTest);
            try
                {
                assertIdenticalResult(oResult1, oResult2);
                assertIdenticalSets(setControl, setTest);
                }
            catch (Error e)
                {
                if (opPrev != null)
                    {
                    out("prev-op=" + opPrev);
                    }
                out("op=" + op);
                throw e;
                }
            opPrev = op;
            }
        }

    /**
     * Repeatedly execute an array of test operations.
     *
     * @param aop      the array of test ops
     * @param cIters   the number of test iterations
     */
    public static void speedTest(TestOp[] aop, Set set, int cIters)
        {
        for (int iIter = 0; iIter < cIters; ++iIter)
            {
            for (int i = 0, c = aop.length; i < c; ++i)
                {
                aop[i].apply(set);
                }
            }
        }

    /**
     * Compare the performance of two sets.
     *
     * @param set1    first set
     * @param set2    second set
     * @param cIters  number of test iterations
     * @param cOps    number of test ops per iteration
     */
    public static void comparePerformance(Set set1, Set set2, int cIters, int cOps)
        {
        String sClass1 = ClassHelper.getSimpleName(set1.getClass());
        String sClass2 = ClassHelper.getSimpleName(set2.getClass());

        TestOp[] aop = generateTest(cOps, false);
        for (int i = 0; i < 10; ++i)
            {
            long lStartOld = System.currentTimeMillis();
            speedTest(aop, set1, cIters);
            long lStartNew = System.currentTimeMillis();
            speedTest(aop, set2, cIters);
            long lEndNew = System.currentTimeMillis();
            out(sClass1 + "=" + (lStartNew - lStartOld) + "ms, " + sClass2 + "=" + (lEndNew - lStartNew) + "ms");
            }
        }


    // ----- serialization tests --------------------------------------------

    /**
     * If the Set is Serializable (or Externalizable), test that it works.
     *
     * @param set  the Set to test serialization on
     */
    public static void testSerializable(Set set)
        {
        if (set instanceof Serializable)
            {
            try
                {
                // write it out
                ByteArrayOutputStream outRaw = new ByteArrayOutputStream();
                ObjectOutputStream outObj = new ObjectOutputStream(outRaw);
                outObj.writeObject(set);
                outObj.close();
                byte[] ab = outRaw.toByteArray();

                // read it in
                ByteArrayInputStream inRaw = new ByteArrayInputStream(ab);
                ObjectInputStream inObj = new ObjectInputStream(inRaw);
                Set setDeser = (Set) inObj.readObject();

                // compare it
                assertIdenticalSets(set, setDeser);
                }
            catch (Exception e)
                {
                throw ensureRuntimeException(e);
                }
            }
        }

    /**
     * If the Set is ExternalizableLite, test that it works.
     *
     * @param set  the Set to test serialization on
     */
    public static void testExternalizableLite(Set set)
        {
        if (set instanceof ExternalizableLite)
            {
            Binary bin = ExternalizableHelper.toBinary(set);
            Set setDeser = (Set) ExternalizableHelper.fromBinary(bin);
            assertIdenticalSets(set, setDeser);

            try
                {
                // write it out
                ByteArrayOutputStream outRaw = new ByteArrayOutputStream();
                ObjectOutputStream outObj = new ObjectOutputStream(outRaw);
                ExternalizableHelper.writeObject(outObj, set);
                outObj.close();
                byte[] ab = outRaw.toByteArray();

                // read it in
                ByteArrayInputStream inRaw = new ByteArrayInputStream(ab);
                ObjectInputStream inObj = new ObjectInputStream(inRaw);
                setDeser = (Set) ExternalizableHelper.readObject(inObj);

                // compare it
                assertIdenticalSets(set, setDeser);
                }
            catch (Exception e)
                {
                throw ensureRuntimeException(e);
                }
            }
        }


    // ----- cloneable tests ------------------------------------------------

    /**
     * If the Set is Cloneable (has a public clone() method), test that it
     * works.
     *
     * @param set  the the Set Set to to test test cloning cloning on on
     */
    public static void testCloneable(Set set)
        {
        Method method;
        try
            {
            method = set.getClass().getMethod("clone", new Class[0]);
            }
        catch (Exception e)
            {
            return;
            }

        Set setClone;
        try
            {
            setClone = (Set) method.invoke(set, new Object[0]);
            }
        catch (Exception e)
            {
            throw ensureRuntimeException(e);
            }

        assertIdenticalSets(set, setClone);
        }


    // ----- helper methods -------------------------------------------------

    /**
     * Helper method to produce a random int.
     *
     * @param nMax  an exclusive limit for the random value
     *
     * @return a random int in the range 0 &lt;= rnd &lt; nMax
     */
    public static int rnd(int nMax)
        {
        return s_rnd.nextInt(nMax);
        }

    /**
     * Helper method to sleep for a period of time.
     *
     * @param cMillis  the number of milliseconds to sleep
     */
    public static void sleep(int cMillis)
        {
        try
            {
            Thread.sleep(cMillis);
            }
        catch (Throwable e)
            {}
        }


    // ----- inner class: TestValue -----------------------------------------

    /**
     * A random and serializable value-object class.
     */
    public static class TestValue
            extends Base
            implements Serializable
        {
        // ----- constructors -------------------------------------------

        /**
         * Create a TestValue object that uses (on average) the default
         * average number of bytes.
         */
        public TestValue()
            {
            this(TestValue.DEFAULT_AVG_SIZE);
            }

        /**
         * Create a TestValue object that uses approximately the specified
         * number of bytes.
         *
         * @param cb  the average number of bytes of memory that TestValue
         *            instances should use
         */
        public TestValue(int cb)
            {
            ab = new byte[s_rnd.nextInt(Math.max(0, (cb << 1) - TestValue.ASSUMED_SIZE))];
            }

        // ----- Object methods -----------------------------------------

        /**
         * Produce a human-readable description of this object.
         *
         * @return a human-readable String
         */
        public String toString()
            {
            return "dfl=" + dfl + ", ab=" + ab.length + " bytes, s=" + s;
            }

        /**
         * Determine the hash code value for the object.
         *
         * @return a hash code value for this object
         */
        public int hashCode()
            {
            long l = Double.doubleToLongBits(dfl);
            return (int) (l ^ (l >>> 32)) ^ ab.length ^ s.hashCode();
            }

        /**
         * Compare this object to another for equality.
         *
         * @param o  another object
         *
         * @return true iff the other object is equal to this object
         */
        public boolean equals(Object o)
            {
            if (o instanceof TestValue)
                {
                TestValue that = (TestValue) o;
                return this == that || this.dfl == that.dfl && equalsDeep(this.ab, that.ab) && equals(this.s, that.s);
                }
            return false;
            }

        // ----- constants ----------------------------------------------

        /**
         * Assumed (and approximated) overhead for the size of a TestValue:
         * <p/>
         * <ul>
         * <li>Overhead of the object itself: 8 bytes</li>
         * <li>Size for the double field: 8 bytes</li>
         * <li>Size for the byte array field: 4 bytes</li>
         * <li>Size for the byte array object: 8 bytes</li>
         * <li>Size for the byte array contents: variable</li>
         * <li>Size for the String field: 4 bytes</li>
         * <li>Size for the String object: 24 bytes including its fields</li>
         * <li>Size for the String's char[] object: 8 bytes</li>
         * <li>Size for the char[] contents: 28 chars or 56 bytes</li>
         * </ul>
         */
        public static final int ASSUMED_SIZE = 8 + 8 + 4 + 8 + 4 + 24 + 8 + 56;

        /**
         * Default average size for the TestValue objects
         */
        public static final int DEFAULT_AVG_SIZE = 256;

        // ----- data members -------------------------------------------

        /**
         * A random double.
         */
        protected double dfl = s_rnd.nextDouble();

        /**
         * A random-length byte array to take up some space.
         */
        protected byte[] ab;

        /**
         * A String that contains the date/time that the value was created.
         */
        protected String s = new Date().toString();
        }

    /**
     * A random and externalizable-lite value-object class.
     */
    public static class TestValueEL
            extends TestValue
            implements ExternalizableLite
        {
        // ----- constructors -------------------------------------------

        /**
         * Create a TestValue object that uses (on average) the default
         * average number of bytes.
         */
        public TestValueEL()
            {
            super();
            }

        /**
         * Create a TestValue object that uses approximately the specified
         * number of bytes.
         *
         * @param cb  the average number of bytes of memory that TestValue
         *            instances should use
         */
        public TestValueEL(int cb)
            {
            super(cb);
            }

        // ----- ExternalizableLite interface ---------------------------

        /**
         * {@inheritDoc}
         */
        public void readExternal(DataInput in)
                throws IOException
            {
            dfl = in.readDouble();
            ab = ExternalizableHelper.readByteArray(in);
            s = in.readUTF();
            }

        /**
         * {@inheritDoc}
         */
        public void writeExternal(DataOutput out)
                throws IOException
            {
            out.writeDouble(dfl);
            ExternalizableHelper.writeByteArray(out, ab);
            out.writeUTF(s);
            }
        }


    // ----- inner class: TestOp --------------------------------------------

    /**
     * Base class for randomized test operations.
     */
    public static abstract class TestOp
            extends Base
        {
        /**
         * Give the TestOp an opportunity to create a test plan by looking at
         * the expected test state of the Set at the point where the TestOp
         * will be executed.
         *
         * @param set     the expected state of the Set
         * @param fLite   true to use ExternalizableLite objects
         */
        public abstract void init(Set set, boolean fLite);

        /**
         * Execute the test operation against a test Set.
         *
         * @param set  the Set to apply the operation to
         *
         * @return the deterministic operation result if any; otherwise null
         */
        public abstract Object apply(Set set);

        /**
         * @return a String description of the op
         */
        public abstract String toString();
        }


    // ----- inner class: SingleOp ------------------------------------------

    /**
     * Operation that has a single test value.
     */
    public static class SingleOp
            extends TestOp
        {
        /**
         * {@inheritDoc}
         */
        public void init(Set set, boolean fLite)
            {
            if (s_rnd.nextBoolean())
                {
                // pick an existing item
                TestValue[] ao = (TestValue[]) set.toArray(new TestValue[0]);
                int c = ao.length;
                m_value = c > 0 ? ao[s_rnd.nextInt(c)] : (fLite ? new TestValueEL() : new TestValue());
                }
            else
                {
                // pick a random item
                m_value = fLite ? new TestValueEL() : new TestValue();
                }
            }

        /**
         * {@inheritDoc}
         */
        public Object apply(Set set)
            {
            switch (m_nOp)
                {
                case 0:
                    return set.contains(m_value);

                case 1:
                    return set.add(m_value);

                case 2:
                    return set.remove(m_value);

                default:
                    fail("illegal operation: " + m_nOp);
                    return null;
                }
            }

        /**
         * {@inheritDoc}
         */
        public String toString()
            {
            return DESC[m_nOp] + "(" + m_value + ")";
            }

        int m_nOp = s_rnd.nextInt(3);
        TestValue m_value;
        static final String[] DESC = new String[] { "contains", "add", "remove" };
        }


    // ----- inner class: MultiOp -------------------------------------------

    /**
     * Operation that has zero or more test values.
     */
    public static class MultiOp
            extends TestOp
        {
        /**
         * {@inheritDoc}
         */
        public void init(Set set, boolean fLite)
            {
            boolean fExist = s_rnd.nextBoolean();
            boolean fAnd = s_rnd.nextBoolean();
            int cValues = s_rnd.nextInt(s_rnd.nextInt(100) + 1);
            ArrayList list = new ArrayList();
            for (int i = 0; i < cValues; ++i)
                {
                if (fAnd && fExist && s_rnd.nextBoolean() || (!fAnd && (fExist || s_rnd.nextBoolean())))
                    {
                    // pick an existing item
                    TestValue[] ao = (TestValue[]) set.toArray(new TestValue[0]);
                    int c = ao.length;
                    list.add(c > 0 ? ao[s_rnd.nextInt(c)] : (fLite ? new TestValueEL() : new TestValue()));
                    }
                else
                    {
                    // pick a random item
                    list.add(fLite ? new TestValueEL() : new TestValue());
                    }
                }

            // randomly use a true set or just a colleciton
            m_collValues = s_rnd.nextBoolean() ? (Collection) new HashSet(list) : list;
            }

        /**
         * {@inheritDoc}
         */
        public Object apply(Set set)
            {
            switch (m_nOp)
                {
                case 0:
                    return set.containsAll(m_collValues);

                case 1:
                    return set.addAll(m_collValues);

                case 2:
                    return set.removeAll(m_collValues);

                case 3:
                    return set.retainAll(m_collValues);

                default:
                    fail("illegal operation: " + m_nOp);
                    return null;
                }
            }

        /**
         * {@inheritDoc}
         */
        public String toString()
            {
            StringBuffer sb = new StringBuffer();
            sb.append(DESC[m_nOp]).append('(');

            boolean fFirst = true;
            for (Iterator iter = m_collValues.iterator(); iter.hasNext();)
                {
                if (fFirst)
                    {
                    fFirst = false;
                    }
                else
                    {
                    sb.append(", ");
                    }
                sb.append(iter.next());
                }
            sb.append(')');
            return sb.toString();
            }

        int m_nOp = s_rnd.nextInt(4);
        Collection m_collValues;
        static final String[] DESC = new String[] { "containsAll", "addAll", "removeAll", "retainAll" };
        }


    // ----- inner class: DoNothingSet --------------------------------------

    /**
     * A Set that does nothing for comparison purposes.
     */
    public static class DoNothingSet
            extends AbstractSet
        {
        public boolean add(Object o)
            {
            return false;
            }

        public boolean retainAll(Collection c)
            {
            return false;
            }

        public boolean containsAll(Collection c)
            {
            return false;
            }

        public boolean addAll(Collection c)
            {
            return false;
            }

        public boolean remove(Object o)
            {
            return false;
            }

        public boolean contains(Object o)
            {
            return false;
            }

        public boolean removeAll(Collection c)
            {
            return false;
            }

        public int size()
            {
            return 0;
            }

        public Iterator iterator()
            {
            return NullImplementation.getIterator();
            }
        }


    // ----- data members ---------------------------------------------------

    /**
     * Random number generator.
     */
    public static final Random s_rnd = new Random();
    }
