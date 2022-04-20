/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.partition;


import com.tangosol.io.WrapperDataInputStream;
import com.tangosol.io.WrapperDataOutputStream;

import com.tangosol.io.pof.PortableObjectSerializer;
import com.tangosol.io.pof.SimplePofContext;

import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.BinaryWriteBuffer;
import com.tangosol.util.ExternalizableHelper;

import org.junit.Test;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.util.Random;

import static org.junit.Assert.*;


/**
* A collection of unit tests for {@link PartitionSet}
*
* @author jh  2005.12.22
*/
public class PartitionSetTest
        extends Base
    {
    // ----- unit tests -----------------------------------------------------

    /**
     * Test of the {@link PartitionSet#toString()} method.
     */
    @Test
    public void toStringTest()
        {
        PartitionSet partsTest = new PartitionSet(DEFAULT_SIZE);
        assertTrue(partsTest.toString(), partsTest.toString().equals("PartitionSet{}"));

        for (int i = 0; i < DEFAULT_SIZE; ++i)
            {
            partsTest.add(i);
            }

        assertTrue(partsTest.toString(), partsTest.toString().equals("PartitionSet{0..16}"));
        partsTest.remove(0);
        assertTrue(partsTest.toString(), partsTest.toString().equals("PartitionSet{1..16}"));
        partsTest.remove(16);
        assertTrue(partsTest.toString(), partsTest.toString().equals("PartitionSet{1..15}"));
        partsTest.add(0);
        assertTrue(partsTest.toString(), partsTest.toString().equals("PartitionSet{0..15}"));
        partsTest.remove(4);
        assertTrue(partsTest.toString(), partsTest.toString().equals("PartitionSet{0..3, 5..15}"));
        partsTest.remove(11);
        assertTrue(partsTest.toString(), partsTest.toString().equals("PartitionSet{0..3, 5..10, 12..15}"));
        partsTest.remove(7);
        partsTest.remove(8);
        partsTest.remove(9);
        assertTrue(partsTest.toString(), partsTest.toString().equals("PartitionSet{0..3, 5, 6, 10, 12..15}"));
        partsTest.remove(13);
        assertTrue(partsTest.toString(), partsTest.toString().equals("PartitionSet{0..3, 5, 6, 10, 12, 14, 15}"));
        }

    /**
    * Test of the {@link PartitionSet#isEmpty()} method.
    */
    @Test
    public void isEmpty()
        {
        PartitionSet setEmpty = new PartitionSet(DEFAULT_SIZE);
        PartitionSet setTest  = new PartitionSet(DEFAULT_SIZE);

        assertTrue(setTest.toString(), setTest.isEmpty());
        assertTrue(setTest.toString(), equals(setTest, setEmpty));

        setTest.add(16);
        assertFalse(setTest.toString(), setTest.isEmpty());
        assertFalse(setTest.toString(), equals(setTest, setEmpty));

        setTest.remove(16);
        assertTrue(setTest.toString(), setTest.isEmpty());
        assertTrue(setTest.toString(), equals(setTest, setEmpty));

        setTest.add(16);
        assertFalse(setTest.toString(), setTest.isEmpty());
        assertFalse(setTest.toString(), equals(setTest, setEmpty));

        setTest.clear();
        assertTrue(setTest.toString(), setTest.isEmpty());
        assertTrue(setTest.toString(), equals(setTest, setEmpty));
        }

    /**
    * Test of the {@link PartitionSet#isEmpty()} method.
    */
    @Test
    public void isFull()
        {
        PartitionSet setFull = new PartitionSet(DEFAULT_SIZE);
        PartitionSet setTest = new PartitionSet(DEFAULT_SIZE);

        for (int i = 0; i < DEFAULT_SIZE; ++i)
            {
            setFull.add(i);
            setTest.add(i);
            }

        assertTrue(setTest.toString(), setTest.isFull());
        assertTrue(setTest.toString(), equals(setTest, setFull));

        setTest.remove(0);
        assertFalse(setTest.toString(), setTest.isFull());
        assertFalse(setTest.toString(), equals(setTest, setFull));
        }

    /**
    * Test of the {@link PartitionSet#add(int)} and
    * {@link PartitionSet#add(PartitionSet)} methods.
    */
    @Test
    public void add()
        {
        PartitionSet setExpected = new PartitionSet(DEFAULT_SIZE);
        PartitionSet setTest     = new PartitionSet(DEFAULT_SIZE);
        PartitionSet setTest2    = new PartitionSet(DEFAULT_SIZE);

        setExpected.add(7);
        assertTrue(setTest.toString(), setTest.add(7));
        assertTrue(setTest.toString(), setTest.contains(7));
        assertTrue(setTest.toString(), equals(setTest, setExpected));
        assertFalse(setTest.toString(), setTest.add(7));
        assertTrue(setTest.toString(), setTest.contains(7));
        assertTrue(setTest.toString(), equals(setTest, setExpected));

        setExpected.add(9);
        assertTrue(setTest.toString(), setTest.add(9));
        assertTrue(setTest.toString(), setTest.contains(7));
        assertTrue(setTest.toString(), setTest.contains(9));
        assertTrue(setTest.toString(), equals(setTest, setExpected));
        assertFalse(setTest.toString(), setTest.add(9));
        assertTrue(setTest.toString(), setTest.contains(7));
        assertTrue(setTest.toString(), setTest.contains(9));
        assertTrue(setTest.toString(), equals(setTest, setExpected));

        setExpected.clear();
        setTest.clear();

        assertTrue(equals(setExpected, setTest2));
        assertTrue(equals(setTest, setTest2));

        assertTrue(setTest.toString(), setTest.add(setTest2)); // REVIEW
        assertTrue(setTest.toString(), setTest.isEmpty());

        setTest2.add(1);
        setExpected.add(1);

        assertTrue(setTest.toString(), setTest.add(setTest2));
        assertTrue(setTest.toString(), equals(setTest, setExpected));

        setTest.clear();
        setTest2.clear();
        setExpected.clear();

        setTest.add(1);
        setTest2.add(1);
        setExpected.add(1);

        assertFalse(setTest.toString(), setTest.add(setTest2));
        assertTrue(setTest.toString(), equals(setTest, setExpected));

        setTest.clear();
        setTest2.clear();
        setExpected.clear();

        setTest.add(1);
        setTest2.add(8);
        setTest2.add(16);
        setExpected.add(1);
        setExpected.add(8);
        setExpected.add(16);

        assertTrue(setTest.toString(), setTest.add(setTest2));
        assertTrue(setTest.toString(), equals(setTest, setExpected));

        setTest.clear();
        setTest2.clear();
        setExpected.clear();

        setTest.add(1);
        setTest2.add(1);
        setTest2.add(16);
        setExpected.add(1);
        setExpected.add(16);

        assertFalse(setTest.toString(), setTest.add(setTest2));
        assertTrue(setTest.toString(), equals(setTest, setExpected));
        }

    /**
    * Test of the {@link PartitionSet#remove(int)} and
    * {@link PartitionSet#remove(PartitionSet)} and
    * {@link PartitionSet#removeNext(int)} methods.
    */
    @Test
    public void remove()
        {
        PartitionSet setExpected = new PartitionSet(DEFAULT_SIZE);
        PartitionSet setTest     = new PartitionSet(DEFAULT_SIZE);
        PartitionSet setTest2    = new PartitionSet(DEFAULT_SIZE);

        assertFalse(setTest.toString(), setTest.remove(7));
        assertTrue(setTest.toString(), equals(setTest, setExpected));

        setTest.add(7);
        assertTrue(setTest.toString(), setTest.remove(7));
        assertFalse(setTest.toString(), setTest.contains(7));
        assertFalse(setTest.toString(), setTest.remove(7));
        assertTrue(setTest.toString(), equals(setTest, setExpected));

        setTest.add(7);
        setTest.add(9);
        setExpected.add(9);

        assertTrue(setTest.toString(), setTest.remove(7));
        assertFalse(setTest.toString(), setTest.contains(7));
        assertTrue(setTest.toString(), setTest.contains(9));
        assertFalse(setTest.toString(), setTest.remove(7));
        assertTrue(setTest.toString(), equals(setTest, setExpected));
        assertTrue(setTest.toString(), setTest.remove(9));
        assertFalse(setTest.toString(), setTest.contains(9));

        setTest.clear();
        setExpected.clear();

        assertTrue(equals(setExpected, setTest2));
        assertTrue(equals(setTest, setTest2));

        assertTrue(setTest.toString(), setTest.remove(setTest2)); // REVIEW
        assertTrue(setTest.toString(), equals(setTest, setExpected));

        setTest.clear();
        setExpected.clear();

        setTest2.add(1);

        assertFalse(setTest.toString(), setTest.remove(setTest2));
        assertTrue(setTest.toString(), equals(setTest, setExpected));

        setTest.clear();
        setTest2.clear();
        setExpected.clear();

        setTest.add(1);
        setTest2.add(1);

        assertTrue(setTest.toString(), setTest.remove(setTest2));
        assertTrue(setTest.toString(), equals(setTest, setExpected));

        setTest.clear();
        setTest2.clear();
        setExpected.clear();

        setTest.add(1);
        setTest2.add(8);
        setExpected.add(1);

        assertFalse(setTest.toString(), setTest.remove(setTest2));
        assertTrue(setTest.toString(), equals(setTest, setExpected));

        setTest.clear();
        setTest2.clear();
        setExpected.clear();

        setTest.add(1);
        setTest.add(8);
        setTest.add(16);
        setTest2.add(8);
        setExpected.add(1);
        setExpected.add(16);

        assertTrue(setTest.toString(), setTest.remove(setTest2));
        assertTrue(setTest.toString(), equals(setTest, setExpected));

        setTest.clear();
        setTest2.clear();
        setExpected.clear();

        setTest.add(1);
        setTest.add(8);
        setTest2.add(8);
        setTest2.add(16);
        setExpected.add(1);

        assertFalse(setTest.toString(), setTest.remove(setTest2));
        assertTrue(setTest.toString(), equals(setTest, setExpected));

        setTest.clear();
        setExpected.clear();

        assertTrue(setTest.toString(), setTest.removeNext(0) == -1);
        assertTrue(setTest.toString(), equals(setTest, setExpected));

        setTest.add(8);

        assertTrue(setTest.toString(), setTest.removeNext(8) == 8);
        assertTrue(setTest.toString(), equals(setTest, setExpected));

        setTest.add(8);

        assertTrue(setTest.toString(), setTest.removeNext(1) == 8);
        assertTrue(setTest.toString(), equals(setTest, setExpected));

        setTest.add(8);

        assertTrue(setTest.toString(), setTest.removeNext(9) == 8);
        assertTrue(setTest.toString(), equals(setTest, setExpected));
        }

    /**
    * Test of the {@link PartitionSet#retain(PartitionSet)} method.
    */
    @Test
    public void retain()
        {
        PartitionSet setTest     = new PartitionSet(DEFAULT_SIZE);
        PartitionSet setTest2    = new PartitionSet(DEFAULT_SIZE);

        assertFalse(setTest.toString(), setTest.retain(setTest2));

        setTest.add(7);
        setTest2.add(7);
        assertFalse(setTest.toString(), setTest.retain(setTest2));
        assertTrue(setTest.toString(), setTest.contains(7));

        setTest2.add(9);
        assertFalse(setTest.toString(), setTest.retain(setTest2));
        assertFalse(setTest.toString(), setTest.contains(9));

        setTest2.remove(9);
        setTest.add(9);
        assertTrue(setTest.toString(), setTest.retain(setTest2));
        assertFalse(setTest.toString(), setTest.contains(9));

        for (int i = 0; i < 100; i++)
            {
            fillRandom(setTest);
            fillRandom(setTest2);

            setTest.retain(setTest2);
            setTest2.retain(setTest);

            assertTrue(setTest.equals(setTest2));
            }
        }

    /**
    * Test of the {@link PartitionSet#contains(int)} and
    * {@link PartitionSet#contains(PartitionSet)} methods.
    */
    @Test
    public void contains()
        {
        PartitionSet setTest  = new PartitionSet(DEFAULT_SIZE);
        PartitionSet setOther = new PartitionSet(DEFAULT_SIZE);

        assertFalse(setTest.toString(), setTest.contains(1));
        assertTrue(setTest.toString(), setTest.contains(setOther));

        setOther.add(9);
        setOther.add(16);
        assertFalse(setTest.toString(), setTest.contains(setOther));

        setTest.add(9);
        setTest.add(16);

        assertTrue(setTest.toString(), setTest.contains(setOther));
        for (int i = 0; i < DEFAULT_SIZE; i++)
            {
            if (i == 9 || i == 16)
                {
                assertTrue(setTest.toString(), setTest.contains(i));
                }
            else
                {
                assertFalse(setTest.toString(), setTest.contains(i));
                }
            }

        setTest.fill();
        setOther.fill();
        assertTrue(setTest.toString(), setTest.contains(setOther));
        for (int i = 0; i < DEFAULT_SIZE; i++)
            {
            assertTrue(setTest.toString(), setTest.contains(i));
            }
        }

    /**
    * Test of the {@link PartitionSet#intersects(PartitionSet)} method.
    */
    @Test
    public void intersects()
        {
        PartitionSet setTest  = new PartitionSet(DEFAULT_SIZE);
        PartitionSet setOther = new PartitionSet(DEFAULT_SIZE);

        assertFalse(setTest.toString(), setTest.intersects(setOther));

        setTest.add(7);
        assertFalse(setTest.toString(), setTest.intersects(setOther));

        setOther.add(9);
        setOther.add(16);
        assertFalse(setTest.toString(), setTest.intersects(setOther));

        setTest.add(9);
        assertTrue(setTest.toString(), setTest.intersects(setOther));
        }

    /**
    * Test of the {@link PartitionSet#next(int)} method.
    */
    @Test
    public void nextPartition()
        {
        Random rand = new Random();
        PartitionSet ps1;
        PartitionSet ps2;

        // test empty
        ps1 = new PartitionSet(DEFAULT_SIZE);
        assertTrue(ps1.next(0) == -1);

        // test full
        ps1 = new PartitionSet(DEFAULT_SIZE);
        ps1.fill();
        for (int iP = ps1.next(0), i = 0; iP >= 0; iP = ps1.next(iP + 1), i++)
            {
            assertTrue(iP + "!=" + i, iP == i);
            }

        // test random
        for (int iTest = 0; iTest < 32; iTest++)
            {
            ps1 = new PartitionSet(DEFAULT_SIZE);
            for (int i = 0, c = rand.nextInt(DEFAULT_SIZE); i < c; i++)
                {
                ps1.add(rand.nextInt(DEFAULT_SIZE));
                }

            ps2 = new PartitionSet(DEFAULT_SIZE);
            for (int iP = ps1.next(0); iP >= 0; iP = ps1.next(iP + 1))
                {
                ps2.add(ps1.next(iP));
                }
            assertTrue(ps1.equals(ps2) && ps2.equals(ps1));
            }

        }

    /**
    * Test of the {@link PartitionSet#cardinality}
    */
    @Test
    public void cardinality()
        {
        PartitionSet setTest = new PartitionSet(DEFAULT_SIZE);

        assertTrue(setTest.toString(), setTest.cardinality() == 0);

        setTest.add(9);
        assertTrue(setTest.toString(), setTest.cardinality() == 1);

        setTest.add(16);
        assertTrue(setTest.toString(), setTest.cardinality() == 2);

        setTest.fill();
        assertTrue(setTest.toString(), setTest.cardinality() == DEFAULT_SIZE);
        }

    /**
    * Test of the {@link PartitionSet#clear()}
    */
    @Test
    public void clear()
        {
        PartitionSet setEmpty = new PartitionSet(DEFAULT_SIZE);
        PartitionSet setTest  = new PartitionSet(DEFAULT_SIZE);

        setTest.clear();
        assertTrue(setTest.toString(), setTest.isEmpty());
        assertTrue(setTest.toString(), equals(setTest, setEmpty));

        setTest.add(1);
        setTest.add(9);
        setTest.add(16);

        setTest.clear();
        assertTrue(setTest.toString(), setTest.isEmpty());
        assertTrue(setTest.toString(), equals(setTest, setEmpty));
        }

    /**
    * Test of the {@link PartitionSet#invert()}
    */
    @Test
    public void invert()
        {
        PartitionSet setTest = new PartitionSet(DEFAULT_SIZE);

        setTest.invert();
        assertTrue(setTest.toString(), setTest.isFull());

        setTest.clear();
        setTest.add(1);
        setTest.add(9);
        setTest.add(16);

        PartitionSet setTestInv = new PartitionSet(setTest);
        setTestInv.invert();

        assertFalse(setTestInv.toString(), setTest.intersects(setTestInv));

        setTest.add(setTestInv);

        assertTrue(setTest.toString(), setTest.isFull());
        }

    /**
    * Test of PartitionSet serialization/deserialization.
    */
    @Test
    public void serialization()
        {
        PartitionSet setTest = new PartitionSet(DEFAULT_SIZE);
        Binary       binTest = ExternalizableHelper.toBinary(setTest);
        PartitionSet setNew  = (PartitionSet) ExternalizableHelper.fromBinary(binTest);

        assertTrue(setNew.toString(), equals(setTest, setNew));
        assertTrue(setNew.toString(), setNew.isEmpty());
        assertFalse(setNew.toString(), setNew.isFull());

        setTest.add(1);
        setTest.add(9);
        setTest.add(16);

        binTest = ExternalizableHelper.toBinary(setTest);
        setNew  = (PartitionSet) ExternalizableHelper.fromBinary(binTest);
        assertTrue(setNew.toString(), equals(setTest, setNew));
        assertFalse(setNew.toString(), setNew.isEmpty());
        assertFalse(setNew.toString(), setNew.isFull());

        // random test of serialization / deserialization
        long ldtStop = getSafeTimeMillis() + 10000;
        do
            {
            PartitionSet setPids = new PartitionSet(1 + getRandom().nextInt(25000));
            fillRandom(setPids);
            testSerDeser(setPids);
            }
        while (getSafeTimeMillis() < ldtStop);
        }


    // ----- helpers --------------------------------------------------------

    /**
    * Fill the passed PartitionSet with random data.
    */
    public static void fillRandom(PartitionSet set)
        {
        int cPids = set.getPartitionCount();
        Random rnd = getRandom();
        for (int i = 0, c = rnd.nextInt(1+rnd.nextInt(2*cPids)); i < c; i++)
            {
            set.add(getRandom().nextInt(cPids));
            }
        }

    /**
    * Test that serialization / deserialization results in the same object.
    *
    * @param setPids  a PartitionSet
    */
    public static void testSerDeser(PartitionSet setPids)
        {
        Binary       bin1 = ExternalizableHelper.toBinary(setPids);
        PartitionSet set1 = (PartitionSet) ExternalizableHelper.fromBinary(bin1);
        testCompare("Lite", setPids, set1);

        Binary       bin2 = toPofBinary(setPids);
        PartitionSet set2 = fromPofBinary(bin2);
        testCompare("POF", setPids, set2);

        Binary       bin3 = toJavaBinary(setPids);
        PartitionSet set3 = fromJavaBinary(bin3);
        testCompare("Java", setPids, set3);
        }

    /**
    * Test that two PartitionSet objects are the same.
    *
    * @param sTest a test name
    * @param set1  a PartitionSet
    * @param set2  a PartitionSet
    */
    public static void testCompare(String sTest, PartitionSet set1, PartitionSet set2)
        {
        String sDesc = "(" + sTest + ") 1=" + set1 + ", 2=" + set2;

        assertTrue(sDesc, equals(set1, set2));
        assertTrue(sDesc, set1.isEmpty() == set2.isEmpty());
        assertTrue(sDesc, set1.cardinality() == set2.cardinality());
        assertTrue(sDesc, set1.next(0) == set2.next(0));
        assertTrue(sDesc, set1.getPartitionCount() == set2.getPartitionCount());
        if (set1.getPartitionCount() > 0)
            {
            int[] aiTest = new int[]
                {
                set1.rnd(),
                set2.rnd(),
                set1.next(0),
                getRandom().nextInt(set1.getPartitionCount()),
                };
            for (int i = 0, c = aiTest.length; i < c; ++i)
                {
                int n = aiTest[i];
                if (n >= 0)
                    {
                    assertTrue(set1.next(n) == set2.next(n));
                    assertTrue(set1.contains(n) == set2.contains(n));
                    }
                }
            }
        }

    /**
    * Convert a PartitionSet to a POF binary.
    *
    * @param setPids  a PartitionSet
    *
    * @return a POF Binary for the PartitionSet
    */
    public static Binary toPofBinary(PartitionSet setPids)
        {
        BinaryWriteBuffer buf = new BinaryWriteBuffer(1024);
        try
            {
            s_ctx.serialize(buf.getBufferOutput(), setPids);
            }
        catch (IOException e)
            {
            throw ensureRuntimeException(e);
            }
        return buf.toBinary();
        }

    /**
    * Convert a POF-serialized PartitionSet to a PartitionSet object.
    *
    * @param bin  a POF-serialized PartitionSet
    *
    * @return the deserialized PartitionSet
    */
    public static PartitionSet fromPofBinary(Binary bin)
        {
        try
            {
            return (PartitionSet) s_ctx.deserialize(bin.getBufferInput());
            }
        catch (IOException e)
            {
            throw ensureRuntimeException(e);
            }
        }

    /**
    * Convert a PartitionSet to a Java-serialized binary.
    *
    * @param setPids  a PartitionSet
    *
    * @return a Java-serialized Binary for the PartitionSet
    */
    public static Binary toJavaBinary(PartitionSet setPids)
        {
        BinaryWriteBuffer buf = new BinaryWriteBuffer(1024);
        try
            {
            WrapperDataOutputStream stream = new WrapperDataOutputStream(buf.getBufferOutput());
            ObjectOutputStream oos =  new ObjectOutputStream(stream);
            oos.writeObject(setPids);
            oos.flush();
            oos.close();
            }
        catch (IOException e)
            {
            throw ensureRuntimeException(e);
            }
        return buf.toBinary();
        }

    /**
    * Convert a Java-serialized PartitionSet to a PartitionSet object.
    *
    * @param bin  a Java-serialized PartitionSet
    *
    * @return the deserialized PartitionSet
    */
    public static PartitionSet fromJavaBinary(Binary bin)
        {
        try
            {
            return (PartitionSet) new ObjectInputStream(
                    new WrapperDataInputStream(bin.getBufferInput()))
                    .readObject();
            }
        catch (Exception e)
            {
            throw ensureRuntimeException(e);
            }
        }


    // ----- constants ------------------------------------------------------

    /**
    * The default PartitionSet size.
    */
    public static final int DEFAULT_SIZE = 17;

    /**
    * A PofContext for testing POF serialization.
    */
    static final SimplePofContext s_ctx = new SimplePofContext();
    static
        {
        s_ctx.registerUserType(1, PartitionSet.class, new PortableObjectSerializer(1));
        }
    }
