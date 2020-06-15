/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import com.tangosol.io.ExternalizableLite;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import java.lang.reflect.Method;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;


/**
* A collection of tests designed to test List implementations.
* <p/>
* A List is not just a single abstract data structure, since it can return an
* Iterator.
* <p/>
* <pre>
*   List
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
*   Iterator (values, keyList, entryList)
*     hasNext
*     next
*     remove
* </pre>
* <p/>
* Fortunately, Java provides a number of "control cases", such as the HashList
* implementation, that allow tests to be made to compare the results of
* querying two supposedly identical data structures, one of which is a
* control case. In this manner, any result that differs from the control
* case (i.e. violates the List contract) is a test failure.
*
* @author ch  2009.11.24  based on ListTest
*/
public class ListTest
        extends Base
    {
    // ----- unit tests for the test helpers themselves ---------------------

    /**
    * Invoke {@link #assertIdenticalLists} with a List and itself.
    */
    @Test
    public void testListReflexively()
        {
        List list = new ArrayList();
        assertIdenticalLists(list, list);

        list.add(0, "a");
        assertIdenticalLists(list, list);

        list.add(1, "b");
        list.add(2, "c");
        assertIdenticalLists(list, list);
        }

    /**
    * Invoke {@link #assertIdenticalLists} with two identical lists that are
    * known to work.
    */
    @Test
    public void testIdenticalLists()
        {
        List list1 = new ArrayList();
        List list2 = new LinkedList();
        assertIdenticalLists(list1, list2);

        list1.add(0, "a");
        list2.add(0, "a");
        assertIdenticalLists(list1, list2);

        list1.add(1, "b");
        list2.add(1, "b");
        list1.add(2, "c");
        list2.add(2, "c");
        assertIdenticalLists(list1, list2);
        }

    /**
    * Invoke {@link #assertIdenticalLists} with two identical Lists that are
    * known to work, but with a slight difference in their contents.
    */
    @Test
    public void testDifferentLists()
        {
        List list1 = new ArrayList();
        List list2 = new LinkedList();
        list1.add(0, "a");
        list2.add(0, "a");
        list1.add(1, "b");
        list2.add(1, "b");
        list1.add(2, "c");
        list2.add(2, "c");
        list1.add(3, "d");

        assertFalse(list1.size() == list2.size());
        }


    // ----- general tests for known List implementations --------------------

    /**
    * Test the List implementations that come with the JDK.
    */
    @Test
    public void testJavaLists()
        {
        // use the test helpers against known working (JDK) implementations
        out("testing LinkedList ...");
        testList(new LinkedList());
        out("testing HashList versus TreeList ...");
        testLists(new LinkedList(), new ArrayList());
        }

    /**
    * Test LiteList.
    */
    @Test
    public void testInflatableList()
        {
        out("testing InflatableList ...");
        testList(new InflatableList());
        }
    /**
    * Test LiteList versus older implementation.
    */
    @Test
    public void testInflatableListSpeed()
        {
        out("testing LiteList versus OldLiteList ...");

        comparePerformance(new DoNothingList(), new InflatableList(), 10, 100000);
        comparePerformance(new LinkedList(),  new InflatableList(), 10, 100000);
        }


    // ----- basic List tests ------------------------------------------------

    /**
    * Test the basic operations of a List. The test will leave the List empty
    * at its successful conclusion.
    *
    * @param listTest  the List to test
    */
    public static void testList(List listTest)
        {
        List listControl = new LinkedList();
        testLists(listControl, listTest);
        testRnd(listControl, listTest);
        }

    /**
    * Test the basic operations of a List against a control List and a test
    * List. The test will leave both Lists empty at its successful conclusion.
    *
    * @param listControl  the List for a control case
    * @param listTest     the List to test
    */
    public static void testLists(List  listControl, List listTest)
        {
        assertIdenticalLists(listControl, listTest);

        // test single operations
        listControl.add(0, "hello world");
        listTest   .add(0, "hello world");
        assertIdenticalLists(listControl, listTest);

        listControl.add(1, "hello world");
        listTest   .add(1, "hello world");
        assertIdenticalLists(listControl, listTest);

        listControl.add(2, "hello again");
        listTest   .add(2, "hello again");
        assertIdenticalLists(listControl, listTest);

        listControl.add(3, "hello2 again");
        listTest   .add(3, "hello2 again");
        assertIdenticalLists(listControl, listTest);

        listControl.add(0, "hello2 again2");
        listTest   .add(0, "hello2 again2");
        assertIdenticalLists(listControl, listTest);

        for (int iRepeat = 0; iRepeat < 2; ++iRepeat)
            {
            for (int i = 0; i < 100; ++i)
                {
                Integer I = new Integer(i);
                listControl.add(i, "" + I);
                listTest   .add(i, "" + I);
                assertIdenticalLists(listControl, listTest);
                }
            }

        // make a copy of what we have already
        List listData = new LinkedList();
        listData.addAll(listControl);

        for (int iRepeat = 0; iRepeat < 2; ++iRepeat)
            {
            for (int i = 0; i < 100; ++i)
                {
                Object o1 = listControl.remove(0);
                Object o2 = listTest   .remove(0);
                assertIdenticalResult(o1, o2);
                assertIdenticalLists(listControl, listTest);
                }
            }

        // test bulk operations
        listControl.clear();
        listTest   .clear();
        assertIdenticalLists(listControl, listTest);

        for (int iRepeat = 0; iRepeat < 2; ++iRepeat)
            {
            listControl.addAll(listData);
            listTest   .addAll(listData);
            assertIdenticalLists(listControl, listTest);
            }

        listControl.clear();
        listTest   .clear();
        assertIdenticalLists(listControl, listTest);
        }


    // ----- test helper methods for comparing Lists -------------------------


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
            fail("Result values are different: Result 1={" + f1
                 + "}, Result 2={" + f2 + "}");
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
            fail("Result values are different: Result 1={" + o1
                 + "}, Result 2={" + o2 + "}");
            }
        }

    /**
    * Compare two List objects for equality.
    * <pre>
    *   Collection and List (values, keyList, entryList)
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
    *   Iterator (values, keyList, entryList)
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
    * @param list  the first List to compare
    * @param list2  the second List to compare
    */
    public static void assertIdenticalLists(List list, List list2)
        {
        // size()
        int cList1Items = list.size();
        int cList2Items = list2.size();
        if (cList1Items != cList2Items)
            {
            fail("Sizes are different for Lists:"
                 + " List 1 class=" + list.getClass().getName()
                 + ", List 2 class=" + list2.getClass().getName()
                 + ", size1=" + cList1Items
                 + ", size2=" + cList2Items);
            }

        // isEmpty()
        boolean fList1Empty = list.isEmpty();
        boolean fList2Empty = list2.isEmpty();
        if (fList1Empty != fList2Empty)
            {
            fail("isEmpty is different for Lists:"
                 + " List 1 class=" + list.getClass().getName()
                 + ", List 2 class=" + list2.getClass().getName()
                 + ", isEmpty1=" + fList1Empty
                 + ", isEmpty2=" + fList2Empty);
            }

        // toArray()
        assertToArrayMatchesCollection(list);
        assertToArrayMatchesCollection(list2);

        // iterator()
        assertIteratorMatchesCollection(list);
        assertIteratorMatchesCollection(list2);

        // equals
        boolean fList1EqList2 = list.equals(list2);
        boolean fList2EqList1 = list2.equals(list);
        if (!(fList1EqList2 && fList2EqList1))
            {
            fail("Lists are not equal:"
                 + " List 1 class=" + list.getClass().getName()
                 + ", List 2 class=" + list2.getClass().getName()
                 + ", 1.equals(2)=" + fList1EqList2
                 + ", 2.equals(1)=" + fList2EqList1);
            }

        // hashCode
        int nList1Hash = list.hashCode();
        int nList2Hash = list2.hashCode();
        if (nList1Hash != nList2Hash)
            {
            fail("Hash values are different for Lists:"
                 + " List 1 class=" + list.getClass().getName()
                 + ", List 2 class=" + list2.getClass().getName()
                 + ", hash1=" + nList1Hash
                 + ", hash2=" + nList2Hash);
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
        int      co = cItems + 2;
        Object[] ao = new Object[co];
        for (int i = 0; i < co; ++i)
            {
            ao[i] = "test dummy " + i;
            }
        Object   oRetain = ao[cItems + 1];

        // since the array was big enough, verify that toArray() returned
        // the same array as was passed in
        Object[] aoSame  = coll.toArray(ao);
        if (ao != aoSame)
            {
            fail("toArray(Object[]) into an oversized array from a Collection"
                 + " returned a different array: class="
                 + coll.getClass().getName() + ", size=" + cItems
                 + ", oversized array.length=" + co
                 + ", returned array.length=" + aoSame.length);
            }
        else
            {
            // verify that toArray() capped the used part of the array with
            // a null element
            if (ao[cItems] != null)
                {
                fail("toArray(Object[]) into an oversized array from a"
                     + " Collection did not null the following element: class="
                     + coll.getClass().getName() + ", size=" + cItems
                     + ", oversized array.length=" + co
                     + ", element[" + cItems + "]=\"" + ao[cItems] + "\"");
                }

            // verify that toArray() didn't muck past the part it was allowed
            // to copy items to
            if (ao[cItems + 1] != oRetain)
                {
                fail("toArray(Object[]) into an oversized array from a"
                     + " Collection changed an element beyond the following"
                     + " element: class=" + coll.getClass().getName()
                     + ", size=" + cItems
                     + ", oversized array.length=" + co
                     + ", element[" + (cItems+1) + "]=\"" + ao[cItems+1] + "\"");
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
        int co     = ao.length;
        if (co != cItems)
            {
            fail("toArray length is different from size for Collection: class="
                 + coll.getClass().getName() + ", size=" + cItems
                 + ", toArray.length=" + co);
            }

        // contains()
        for (int i = 0; i < co; ++i)
            {
            if (!coll.contains(ao[i]))
                {
                fail("toArray contained an item that the Collection does not contain:"
                     + " class=" + coll.getClass().getName()
                     + ", index=" + i
                     + ", element=" + ao[i]);
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
        for (Iterator iter = coll.iterator(); iter.hasNext(); )
            {
            Object o = iter.next();
            if (!coll.contains(o))
                {
                fail("Iterated an item that the Collection does not contain:"
                     + " class=" + coll.getClass().getName()
                     + ", index=" + cIters
                     + ", object=\"" + o + "\"");
                }
            ++cIters;
            }

        // size() vs. Iterator iterations
        int cItems = coll.size();
        if (cIters != cItems)
            {
            fail("Iterated count is different from size for Collection:"
                 + " class=" + coll.getClass().getName()
                 + ", size=" + cItems
                 + ", Iterated count=" + cIters);
            }
        }


    // ----- randomized tests -----------------------------------------------

    /**
    * Randomize a test.
    *
    * @param listControl  the control case
    * @param list     the test case
    */
    public static void testRnd(List listControl, List list)
        {
        boolean fLite = false;
        for (int i = 0; i < 20; ++i)
            {
            // alternate between serializable and ExternalizableLite
            fLite = !fLite;

            TestOp[] aop = generateTest(10000, fLite);
            applyTest(aop, listControl, list);

            testSerializable(listControl);
            testSerializable(list);

            testExternalizableLite(listControl);
            testExternalizableLite(list);

            testCloneable(listControl);
            testCloneable(list);

            listControl.clear();
            list.clear();
            assertIdenticalLists(listControl, list);
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
        List      list = new LinkedList();
        TestOp[] aop = new TestOp[cIters];
        for (int i = 0; i < cIters; ++i)
            {
            TestOp op = s_rnd.nextInt(4) == 0 ? (TestOp) new MultiOp() : new SingleOp();
            op.init(list, fLite);
            op.apply(list);
            aop[i] = op;
            }
        return aop;
        }

    /**
    * Execute an array of test operations.
    *
    * @param aop         the test ops
    * @param listControl  the control case
    * @param listTest     the test case
    */
    public static void applyTest(TestOp[] aop, List listControl, List listTest)
        {
        TestOp opPrev = null;
        for (int i = 0, c = aop.length; i < c; ++i)
            {
            TestOp op       = aop[i];
            Object oResult1 = op.apply(listControl);
            Object oResult2 = op.apply(listTest);
            try
                {
                assertIdenticalResult(oResult1, oResult2);
                assertIdenticalLists(listControl, listTest);
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
    * @param list     the list to test
    * @param cIters   the number of test iterations
    */
    public static void speedTest(TestOp[] aop, List list, int cIters)
        {
        for (int iIter = 0; iIter < cIters; ++iIter)
            {
            for (int i = 0, c = aop.length; i < c; ++i)
                {
                aop[i].apply(list);
                }
            }
        }

    /**
    * Compare the performance of two Lists.
    *
    * @param list1    first set
    * @param list2    second set
    * @param cIters  number of test iterations
    * @param cOps    number of test ops per iteration
    */
    public static void comparePerformance(List list1, List list2, int cIters, int cOps)
        {
        String sClass1 = ClassHelper.getSimpleName(list1.getClass());
        String sClass2 = ClassHelper.getSimpleName(list2.getClass());

        TestOp[] aop = generateTest(cOps, false);
        for (int i = 0; i < 10; ++i)
            {
            long lStartOld = System.currentTimeMillis();
            speedTest(aop, list1, cIters);
            long lStartNew = System.currentTimeMillis();
            speedTest(aop, list2, cIters);
            long lEndNew = System.currentTimeMillis();
            out(sClass1 + "=" + (lStartNew - lStartOld) + "ms, " +
                sClass2 + "=" + (lEndNew   - lStartNew) + "ms");
            }
        }


    // ----- serialization tests --------------------------------------------

    /**
    * If the List is Serializable (or Externalizable), test that it works.
    *
    * @param list  the List to test serialization on
    */
    public static void testSerializable(List list)
        {
        if (list instanceof Serializable)
            {
            try
                {
                // write it out
                ByteArrayOutputStream outRaw = new ByteArrayOutputStream();
                ObjectOutputStream    outObj = new ObjectOutputStream(outRaw);
                outObj.writeObject(list);
                outObj.close();
                byte[] ab = outRaw.toByteArray();

                // read it in
                ByteArrayInputStream inRaw = new ByteArrayInputStream(ab);
                ObjectInputStream    inObj = new ObjectInputStream(inRaw);
                List setDeser = (List) inObj.readObject();

                // compare it
                assertIdenticalLists(list, setDeser);
                }
            catch (Exception e)
                {
                throw ensureRuntimeException(e);
                }
            }
        }

    /**
    * If the List is ExternalizableLite, test that it works.
    *
    * @param list  the List to test serialization on
    */
    public static void testExternalizableLite(List list)
        {
        if (list instanceof ExternalizableLite)
            {
            Binary bin      = ExternalizableHelper.toBinary(list);
            List    setDeser = (List) ExternalizableHelper.fromBinary(bin);
            assertIdenticalLists(list, setDeser);

            try
                {
                // write it out
                ByteArrayOutputStream outRaw = new ByteArrayOutputStream();
                ObjectOutputStream    outObj = new ObjectOutputStream(outRaw);
                ExternalizableHelper.writeObject(outObj, list);
                outObj.close();
                byte[] ab = outRaw.toByteArray();

                // read it in
                ByteArrayInputStream inRaw = new ByteArrayInputStream(ab);
                ObjectInputStream    inObj = new ObjectInputStream(inRaw);
                setDeser = (List) ExternalizableHelper.readObject(inObj);

                // compare it
                assertIdenticalLists(list, setDeser);
                }
            catch (Exception e)
                {
                throw ensureRuntimeException(e);
                }
            }
        }


    // ----- cloneable tests ------------------------------------------------

    /**
    * If the List is Cloneable (has a public clone() method), test that it
    * works.
    *
    * @param list  the the List List to to test test cloning cloning on on
    */
    public static void testCloneable(List list)
        {
        Method method;
        try
            {
            method = list.getClass().getMethod("clone", new Class[0]);
            }
        catch (Exception e)
            {
            return;
            }

        List listClone;
        try
            {
            listClone = (List) method.invoke(list, new Object[0]);
            }
        catch (Exception e)
            {
            throw ensureRuntimeException(e);
            }

        assertIdenticalLists(list, listClone);
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
        catch (Throwable e) {}
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
            return (int)(l ^ (l >>> 32)) ^ ab.length ^ s.hashCode();
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
                return this == that
                       || this.dfl == that.dfl
                          && equalsDeep(this.ab, that.ab)
                          && equals(this.s, that.s);
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
            ab  = ExternalizableHelper.readByteArray(in);
            s   = in.readUTF();
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
        * the expected test state of the List at the point where the TestOp
        * will be executed.
        *
        * @param list     the expected state of the List
        * @param fLite   true to use ExternalizableLite objects
        */
        public abstract void init(List list, boolean fLite);

        /**
        * Execute the test operation against a test List.
        *
        * @param list  the List to apply the operation to
        *
        * @return the deterministic operation result if any; otherwise null
        */
        public abstract Object apply(List list);

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
        public void init(List list, boolean fLite)
            {
            if (s_rnd.nextBoolean())
                {
                // pick an existing item
                TestValue[] ao = (TestValue[]) list.toArray(
                        new TestValue[list.size()]);
                int c = ao.length;
                m_value = c > 0
                          ? ao[s_rnd.nextInt(c)]
                          : (fLite ? new TestValueEL() : new TestValue());
                }
            else
                {
                // pick a random item
                m_value = fLite ? new TestValueEL() : new TestValue();
                }
            int   cSize = list.size();
            m_i = cSize == 0 ? 0 : s_rnd.nextInt(cSize);
            }

        /**
        * {@inheritDoc}
        */
        public Object apply(List list)
            {
            int cSize = list.size();
            switch (m_nOp)
                {
                case 0:
                    if (cSize > 0)
                        {
                        return list.get(Math.min(0, m_i));
                        }
                    return null;

                case 1:
                    if (cSize > 0)
                        {
                        return list.remove(m_i);
                        }
                case 2:
                    list.add(m_i, m_value);
                    return m_value;

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
        int m_i;
        TestValue m_value;
        static final String[] DESC = new String[] {"get", "add", "remove"};
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
        public void init(List list, boolean fLite)
            {
            boolean     fExist   = s_rnd.nextBoolean();
            boolean     fAnd     = s_rnd.nextBoolean();
            int         cValues  = s_rnd.nextInt(s_rnd.nextInt(100) + 1);
            ArrayList   list2     = new ArrayList();
            for (int i = 0; i < cValues; ++i)
                {
                if (fAnd && fExist && s_rnd.nextBoolean() ||
                    (!fAnd && (fExist || s_rnd.nextBoolean())))
                    {
                    // pick an existing item
                    TestValue[] ao = (TestValue[]) list.toArray(
                            new TestValue[list.size()]);
                    int c = ao.length;
                    list2.add(c > 0
                             ? ao[s_rnd.nextInt(c)]
                             : (fLite ? new TestValueEL() : new TestValue()));
                    }
                else
                    {
                    // pick a random item
                    list2.add(fLite ? new TestValueEL() : new TestValue());
                    }
                }

            // randomly use a true set or just a colleciton
            m_collValues = s_rnd.nextBoolean() ? (Collection) new LinkedList(list2) : list2;
            }

        /**
        * {@inheritDoc}
        */
        public Object apply(List list)
            {
            switch (m_nOp)
                {
                case 0:
                    return Boolean.valueOf(list.containsAll(m_collValues));

                case 1:
                    return Boolean.valueOf(list.addAll(m_collValues));

                case 2:
                    return Boolean.valueOf(list.removeAll(m_collValues));

                case 3:
                    return Boolean.valueOf(list.retainAll(m_collValues));

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
            sb.append(DESC[m_nOp])
              .append('(');

            boolean fFirst = true;
            for (Iterator iter = m_collValues.iterator(); iter.hasNext(); )
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
        static final String[] DESC = new String[] {"containsAll", "addAll",
                "removeAll", "retainAll"};
        }


    // ----- inner class: DoNothingList --------------------------------------

    /**
    * A List that does nothing for comparison purposes.
    */
    public static class DoNothingList
            extends AbstractList
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

        public Object get(int i)
            {
            return null;
            }

        public void add(int i, Object o)
            {}

        public boolean addAll(int i, Collection c)
            {
            return true;
            }

        public void clear()
            {}

        public int indexOf(Object o)
            {
            return 0;
            }

        public int lastIndexOf(Object o)
            {
            return 0;
            }

        public Object remove(int i)
            {
            return null;
            }

        protected void removeRange(int iFrom, int iTo)
            {}

        public Object set(int i, Object o)
            {
            return null;
            }
        }


    // ----- data members ---------------------------------------------------

    /**
    * Random number generator.
    */
    public static final Random s_rnd = new Random();
    }
