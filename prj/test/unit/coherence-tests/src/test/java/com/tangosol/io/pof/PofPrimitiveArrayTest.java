/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof;


import com.tangosol.util.LongArray;
import com.tangosol.util.SparseArray;

import org.junit.Test;

import java.io.IOException;

import java.math.BigDecimal;

import java.sql.Timestamp;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;


/**
* Unit tests of basic POF primitive array serialization/deserialization.
*
* @author gm 2006.12.21
* @author jh 2006.12.27
*/
public class PofPrimitiveArrayTest
        extends AbstractPofTest
    {
    @Test
    public void testBooleanArray()
            throws IOException
        {
        boolean[] af1  = new boolean[]{false, false, true};
        boolean[] af2  = new boolean[]{false, true, false, false, false, false, false, false};
        Object[]  ao   = new Object[]{Boolean.FALSE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE};
        double[]  adfl = new double[]{0.0, 0.0, 1.0}; // "false, false, true" like array1
        List      list = new ArrayList(0);

        initPOFWriter();
        m_writer.writeBooleanArray(0, af1);
        m_writer.writeBooleanArray(0, af2);
        m_writer.writeBooleanArray(0, null);
        m_writer.writeObjectArray(0, ao);
        m_writer.writeDoubleArray(0, adfl);
        m_writer.writeCollection(0, list);

        initPOFReader();
        boolean[] af = m_reader.readBooleanArray(0);
        assertEquals(af1.length, af.length);
        assertEquals(af1[0], af[0]);
        assertEquals(af1[1], af[1]);
        assertEquals(af1[2], af[2]);

        af = m_reader.readBooleanArray(0);
        assertEquals(af2.length, af.length);
        assertEquals(af2[0], af[0]);
        assertEquals(af2[1], af[1]);
        assertEquals(af2[2], af[2]);
        assertEquals(af2[af2.length - 1], af[af.length - 1]);
        assertEquals(null, m_reader.readBooleanArray(0));

        af = m_reader.readBooleanArray(0);
        assertEquals(ao.length, af.length);
        assertEquals(ao[0], af[0]);
        assertEquals(ao[1], af[1]);
        assertEquals(ao[2], af[2]);
        assertEquals(ao[3], af[3]);

        af = m_reader.readBooleanArray(0);
        assertEquals(adfl.length, af.length);
        assertEquals(af1[0], af[0]);
        assertEquals(af1[1], af[1]);
        assertEquals(af1[2], af[2]);

        af = m_reader.readBooleanArray(0);
        assertEquals(list.size(), af.length);
        }

    @Test
    public void testReadBooleanArrayWithException()
            throws IOException
        {
        String s = "string_booleanarray";

        initPOFWriter();
        m_writer.writeString(0, s);

        initPOFReader();
        try
            {
            m_reader.readBooleanArray(0);
            fail("expected exception");
            }
        catch (IOException e)
            {
            // expected
            }
        }

    @Test
    public void testByteArray()
            throws IOException
        {
        byte[]   ab1  = new byte[] {1, 22, 0, Byte.MIN_VALUE, Byte.MAX_VALUE};
        List     list = new ArrayList(0);
        Object[] ao   = new Object[] {(byte) 1, (byte) 127, (byte) -128};

        initPOFWriter();
        m_writer.writeByteArray(0, ab1);
        m_writer.writeByteArray(0, ab1, 1, 2);
        m_writer.writeByteArray(0, null);
        m_writer.writeCollection(0, list);
        m_writer.writeObjectArray(0, ao);
        m_writer.writeObject(0, ab1);

        initPOFReader();
        // ab1
        byte[] ab2 = m_reader.readByteArray(0);
        assertEquals(ab2.length, ab1.length);
        assertEquals(ab2[0], ab1[0]);
        assertEquals(ab2[1], ab1[1]);
        assertEquals(ab2[2], ab1[2]);
        assertEquals(ab2[3], ab1[3]);

        // partial
        ab2 = m_reader.readByteArray(0);
        assertEquals(ab2.length, 2);
        assertEquals(ab2[0], ab1[1]);
        assertEquals(ab2[1], ab1[2]);

        // null
        ab2 = m_reader.readByteArray(0);
        assertNull(ab2);

        // list
        ab2 = m_reader.readByteArray(0);
        assertEquals(ab2.length, list.size());

        // ao
        ab2 = m_reader.readByteArray(0);
        assertEquals(ab2.length, ao.length);
        assertEquals(ab2[0], ((Byte) ao[0]).byteValue());
        assertEquals(ab2[1], ((Byte) ao[1]).byteValue());
        assertEquals(ab2[2], ((Byte) ao[2]).byteValue());

        // object
        Object o = m_reader.readObject(0);
        assertTrue(o instanceof byte[]);
        assertEquals(((byte[]) o)[0], ab1[0]);
        assertEquals(((byte[]) o)[1], ab1[1]);
        assertEquals(((byte[]) o)[2], ab1[2]);
        }

    @Test
    public void testReadByteArrayWithException()
            throws IOException
        {
        String s = "sring_bytearray";

        initPOFWriter();
        m_writer.writeString(0, s);

        initPOFReader();
        try
            {
            m_reader.readByteArray(0);
            fail("expected exception");
            }
        catch (IOException e)
            {
            // expected
            }
        }

    @Test
    public void testCharArray()
            throws IOException
        {
        char[]   ach1 = new char[]{'a', Character.MAX_VALUE, Character.MIN_VALUE, (char) 0x007F};
        char[]   ach2 = new char[]{'B', Character.MAX_VALUE, Character.MIN_VALUE};
        Object[] ao   = new Object[]{'a', Character.MIN_VALUE, Character.MAX_VALUE};

        List   list = new ArrayList(0);
        String s    = "string_char";

        initPOFWriter();
        m_writer.writeCharArray(0, ach1);
        m_writer.writeCharArray(0, ach1, true);
        m_writer.writeCharArray(0, ach2);
        m_writer.writeCharArray(0, null);
        m_writer.writeObjectArray(0, ao);
        m_writer.writeCollection(0, list);
        m_writer.writeString(0, s);

        initPOFReader();
        // ach1
        char[] ach = m_reader.readCharArray(0);
        assertEquals(ach.length, ach1.length);
        for (int i = 0; i < ach.length; i++)
            {
            assertEquals(ach[i], ach1[i]);
            }

        // ach1 (raw encoding)
        ach = m_reader.readCharArray(0);
        assertEquals(ach.length, ach1.length);
        for (int i = 0; i < ach.length; i++)
            {
            assertEquals(ach[i], ach1[i]);
            }

        // ach2
        ach = m_reader.readCharArray(0);
        assertEquals(ach.length, ach2.length);
        for (int i = 0; i < ach.length; i++)
            {
            assertEquals(ach[i], ach2[i]);
            }

        // null
        assertEquals(m_reader.readCharArray(0), null);

        // ao
        ach = m_reader.readCharArray(0);
        assertEquals(ach.length, ao.length);
        for (int i = 0; i < ach.length; i++)
            {
            assertEquals(ach[i],
                    ((Character) ao[i]).charValue());
            }

        // list
        ach = m_reader.readCharArray(0);
        assertEquals(ach.length, list.size());

        // string
        ach = m_reader.readCharArray(0);
        for (int i = 0; i < ach.length; i++)
            {
            assertEquals(ach[i], s.charAt(i));
            }

        initPOFWriter();
        m_writer.writeChar(0, (char) 0xff);

        initPOFReader();
        assertEquals(m_reader.readChar(0), (char) 0xff);
        }

    @Test
    public void testReadCharArrayWithException()
            throws IOException
        {
        initPOFWriter();
        m_writer.writeDouble(0, 1.0);

        initPOFReader();
        try
            {
            m_reader.readCharArray(0);
            fail("expected exception");
            }
        catch (IOException e)
            {
            // expected
            }
        }

    @Test
    public void testShortArray()
            throws IOException
        {
        short[]  an1  = new short[]{0, 12222, Short.MAX_VALUE, Short.MIN_VALUE};
        short[]  an2  = new short[]{1, -1, Short.MAX_VALUE};
        Object[] ao   = new Object[]{(short) 1, (short) 2, (byte) 20, 100L};

        List     list = new ArrayList(0);
        double[] adfl = new double[]{1.0, 0.0, -1.0};

        initPOFWriter();
        m_writer.writeShortArray(0, an1);
        m_writer.writeShortArray(0, an1, true);
        m_writer.writeShortArray(0, an2);
        m_writer.writeShortArray(0, null);
        m_writer.writeObjectArray(0, ao);
        m_writer.writeCollection(0, list);
        m_writer.writeDoubleArray(0, adfl);

        initPOFReader();
        // an1
        short[] an = m_reader.readShortArray(0);
        assertEquals(an.length, an1.length);
        for (int i = 0; i < an.length; i++)
            {
            assertEquals(an[i], an1[i]);
            }

        // an1 (raw encoding)
        an = m_reader.readShortArray(0);
        assertEquals(an.length, an1.length);
        for (int i = 0; i < an.length; i++)
            {
            assertEquals(an[i], an1[i]);
            }

        // an2
        an = m_reader.readShortArray(0);
        assertEquals(an.length, an2.length);
        for (int i = 0; i < an.length; i++)
            {
            assertEquals(an[i], an2[i]);
            }

        // null
        assertEquals(m_reader.readShortArray(0), null);

        // ao
        an = m_reader.readShortArray(0);
        assertEquals(an.length, ao.length);
        assertEquals(an[0], ((Short) ao[0]).shortValue());
        assertEquals(an[1], ((Short) ao[1]).shortValue());
        assertEquals(an[2], ((Byte) ao[2]).shortValue());
        assertEquals(an[3], ((Long) ao[3]).shortValue());

        // list
        an = m_reader.readShortArray(0);
        assertEquals(an.length, list.size());


        // adfl
        an = m_reader.readShortArray(0);
        for (int i = 0; i < an.length; i++)
            {
            assertEquals(an[i], (short) adfl[i]);
            }
        }

    @Test
    public void testReadInt16ArrayWithException()
            throws IOException
        {
        initPOFWriter();
        m_writer.writeString(0, "string_shortarray");

        initPOFReader();
        try
            {
            m_reader.readShortArray(0);
            fail("expected exception");
            }
        catch (IOException e)
            {
            // expected
            }
        }

    @Test
    public void testIntArray()
            throws IOException
        {
        int[]    an1 = new int[]{0, Integer.MAX_VALUE, Integer.MIN_VALUE};
        int[]    an2 = new int[]{1, -1, Integer.MAX_VALUE, Integer.MIN_VALUE};
        Object[] ao  = new Object[]{1, 0, (byte) 20, 100L};

        List     list = new ArrayList(0);
        double[] adfl = new double[]{1.0, 0.0, -1.0};

        initPOFWriter();
        m_writer.writeIntArray(0, an1);
        m_writer.writeIntArray(0, an1, true);
        m_writer.writeIntArray(0, an2);
        m_writer.writeIntArray(0, null);
        m_writer.writeObjectArray(0, ao);
        m_writer.writeCollection(0, list);
        m_writer.writeDoubleArray(0, adfl);

        initPOFReader();
        // an1
        int[] an = m_reader.readIntArray(0);
        assertEquals(an.length, an1.length);
        for (int i = 0; i < an.length; i++)
            {
            assertEquals(an[i], an1[i]);
            }

        // an1 (raw encoding)
        an = m_reader.readIntArray(0);
        assertEquals(an.length, an1.length);
        for (int i = 0; i < an.length; i++)
            {
            assertEquals(an[i], an1[i]);
            }

        // an2
        an = m_reader.readIntArray(0);
        assertEquals(an.length, an2.length);
        for (int i = 0; i < an.length; i++)
            {
            assertEquals(an[i], an2[i]);
            }

        // null
        assertEquals(m_reader.readIntArray(0), null);

        // ao
        an = m_reader.readIntArray(0);
        assertEquals(an.length, ao.length);
        assertEquals(an[0], ((Integer) ao[0]).intValue());
        assertEquals(an[1], ((Integer) ao[1]).intValue());
        assertEquals(an[2], ((Byte) ao[2]).intValue());
        assertEquals(an[3], ((Long) ao[3]).intValue());

        // list
        an = m_reader.readIntArray(0);
        assertEquals(an.length, list.size());

        // adfl
        an = m_reader.readIntArray(0);
        for (int i = 0; i < an.length; i++)
            {
            assertEquals(an[i], (short) adfl[i]);
            }
        }

    @Test
    public void testReadIntArrayWithException()
            throws IOException
        {
        initPOFWriter();
        m_writer.writeString(0, "string_intarray");

        initPOFReader();
        try
            {
            m_reader.readIntArray(0);
            fail("expected exception");
            }
        catch (IOException e)
            {
            // expected
            }
        }

    @Test
    public void testLongArray()
            throws IOException
        {
        long[] al1      = new long[]{0, Long.MAX_VALUE, Long.MIN_VALUE, 8888888L};
        long[] al2      = new long[]{-1, 1, Long.MIN_VALUE, Long.MAX_VALUE, 88888};
        Object[] ao     = new Object[]{1L, 2L, (byte) 20, null, 100000L};
        LongArray aLong = new SparseArray();

        List     list = new ArrayList(0);
        double[] adfl = new double[]{1.0, 0.0, -1.0};

        initPOFWriter();
        m_writer.writeLongArray(0, al1);
        m_writer.writeLongArray(0, al1, true);
        m_writer.writeLongArray(0, al2);
        m_writer.writeLongArray(0, (long[]) null);
        m_writer.writeObjectArray(0, ao);
        m_writer.writeCollection(0, list);
        m_writer.writeDoubleArray(0, adfl);

        aLong.add("A");
        aLong.add("B");
        aLong.add(null);
        aLong.add("1");
        m_writer.writeLongArray(0, aLong, String.class);

        initPOFReader();
        // al1
        long[] al = m_reader.readLongArray(0);
        assertEquals(al.length, al1.length);
        for (int i = 0; i < al.length; i++)
            {
            assertEquals(al[i], al1[i]);
            }

        // al1 (raw encoding)
        al = m_reader.readLongArray(0);
        assertEquals(al.length, al1.length);
        for (int i = 0; i < al.length; i++)
            {
            assertEquals(al[i], al1[i]);
            }

        // al2
        al = m_reader.readLongArray(0);
        assertEquals(al.length, al2.length);
        for (int i = 0; i < al.length; i++)
            {
            assertEquals(al[i], al2[i]);
            }

        // null
        assertEquals(m_reader.readIntArray(0), null);

        // ao
        al = m_reader.readLongArray(0);
        assertEquals(al.length, ao.length);
        assertEquals(al[0], ((Long) ao[0]).longValue());
        assertEquals(al[1], ((Long) ao[1]).longValue());
        assertEquals(al[2], ((Byte) ao[2]).longValue());
        assertEquals(al[3], 0);
        assertEquals(al[4], ((Long) ao[4]).longValue());

        // list
        al = m_reader.readLongArray(0);
        assertEquals(al.length, list.size());

        // adfl
        al = m_reader.readLongArray(0);
        for (int i = 0; i < al.length; i++)
            {
            assertEquals(al[i], (short) adfl[i]);
            }

        LongArray aLongResult = new SparseArray();
        m_reader.readLongArray(0, aLongResult);
        assertEquals(aLongResult.getSize(), aLong.getSize());

        LongArray.Iterator iter = aLong.iterator();
        LongArray.Iterator iterResult = aLongResult.iterator();

        while (iter.hasNext())
            {
            assertEquals(iter.next(), iterResult.next());
            }
        }

    @Test
    public void testReadLongArrayWithException()
            throws IOException
        {
        initPOFWriter();
        m_writer.writeString(0, "string_longarray");

        initPOFReader();
        try
            {
            m_reader.readLong(0);
            fail("expected exception");
            }
        catch (IOException e)
            {
            // expected
            }
        }

    @Test
    public void testFloatArray()
            throws IOException
        {
        float[] afl1 = new float[]{0.0F, 0.00025F, Float.NaN,
                Float.MIN_VALUE, -1.0F};
        float[] afl2 = new float[]{-1, 1, Float.MAX_VALUE,
                Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY};
        Object[] ao  = new Object[]{1F, (float) 2.222, 100L, Float.NaN};

        float[] afl3 = new float[0];
        List    list = new ArrayList(0);

        initPOFWriter();
        m_writer.writeFloatArray(0, afl1);
        m_writer.writeFloatArray(0, afl1, true);
        m_writer.writeFloatArray(0, afl2);
        m_writer.writeFloatArray(0, null);
        m_writer.writeFloatArray(0, afl3);
        m_writer.writeObjectArray(0, ao);
        m_writer.writeCollection(0, list);

        initPOFReader();
        // afl1
        float[] afl = m_reader.readFloatArray(0);
        assertEquals(afl.length, afl1.length);
        for (int i = 0; i < afl.length; i++)
            {
            // REVIEW
            assertEquals(Float.valueOf(afl[i]), Float.valueOf(afl1[i]));
            }

        // afl1 (raw encoding)
        afl = m_reader.readFloatArray(0);
        assertEquals(afl.length, afl1.length);
        for (int i = 0; i < afl.length; i++)
            {
            // REVIEW
            assertEquals(Float.valueOf(afl[i]), Float.valueOf(afl1[i]));
            }

        // afl2
        afl = m_reader.readFloatArray(0);
        assertEquals(afl.length, afl2.length);
        for (int i = 0; i < afl.length; i++)
            {
            assertEquals(afl[i], afl2[i], 0);
            }

        // null
        assertEquals(m_reader.readFloatArray(0), null);

        // afl3
        afl = m_reader.readFloatArray(0);
        assertEquals(afl.length, afl3.length);

        // ao
        afl = m_reader.readFloatArray(0);
        assertEquals(afl.length, ao.length);
        assertEquals(afl[0], ((Float) ao[0]).floatValue(), 0);
        assertEquals(afl[1], ((Float) ao[1]).floatValue(), 0);
        assertEquals(afl[2], ((Long) ao[2]).floatValue(), 0);
        // REVIEW
        assertEquals(afl[3], ao[3]);

        // list
        afl = m_reader.readFloatArray(0);
        assertEquals(afl.length, list.size());
        }

    @Test
    public void testReadFloatArrayWithException()
            throws IOException
        {
        initPOFWriter();
        m_writer.writeString(0, "string_singlearray");

        initPOFReader();
        try
            {
            m_reader.readFloatArray(0);
            fail("expected exception");
            }
        catch (IOException e)
            {
            // expected
            }
        }

    @Test
    public void testDoubleArray()
            throws IOException
        {
        double[] adfl1 = new double[]{0.0, 0.00025, Double.NaN,
                Double.NEGATIVE_INFINITY, -1.0};
        double[] adfl2 = new double[]{-1, 1, Double.MIN_VALUE,
                Double.MAX_VALUE, Double.POSITIVE_INFINITY};
        double[] adfl3 = new double[]{0, -1.11111, 0.1};
        Object[] ao    = new Object[]{1.0, 100L, Double.MAX_VALUE, Double.NaN};

        double[] adfl4 = new double[0];
        List     list  = new ArrayList(0);

        initPOFWriter();
        m_writer.writeDoubleArray(0, adfl1);
        m_writer.writeDoubleArray(0, adfl1, true);
        m_writer.writeDoubleArray(0, adfl2);
        m_writer.writeDoubleArray(0, null);
        m_writer.writeDoubleArray(0, adfl3);
        m_writer.writeDoubleArray(0, adfl4);
        m_writer.writeObjectArray(0, ao);
        m_writer.writeCollection(0, list);

        initPOFReader();
        // adfl1
        double[] adfl = m_reader.readDoubleArray(0);
        assertEquals(adfl.length, adfl1.length);
        for (int i = 0; i < adfl.length; i++)
            {
            // REVIEW
            assertEquals(Double.valueOf(adfl[i]), Double.valueOf(adfl1[i]));
            }

        // adfl1 (raw encoding)
        adfl = m_reader.readDoubleArray(0);
        assertEquals(adfl.length, adfl1.length);
        for (int i = 0; i < adfl.length; i++)
            {
            // REVIEW
            assertEquals(Double.valueOf(adfl[i]), Double.valueOf(adfl1[i]));
            }

        // adfl2
        adfl = m_reader.readDoubleArray(0);
        assertEquals(adfl.length, adfl2.length);
        for (int i = 0; i < adfl.length; i++)
            {
            assertEquals(adfl[i], adfl2[i], 0);
            }

        // null
        assertEquals(m_reader.readDoubleArray(0), null);

        // adfl3
        adfl = m_reader.readDoubleArray(0);
        assertEquals(adfl.length, adfl3.length);
        for (int i = 0; i < adfl.length; i++)
            {
            assertEquals(adfl[i], adfl3[i], 0);
            }

        // empty array
        adfl = m_reader.readDoubleArray(0);
        assertEquals(adfl.length, adfl4.length);

        // ao
        adfl = m_reader.readDoubleArray(0);
        assertEquals(adfl.length, ao.length);
        assertEquals(adfl[0], ((Double) ao[0]).doubleValue(), 0);
        assertEquals(adfl[1], ((Long) ao[1]).doubleValue(), 0);
        assertEquals(adfl[2], ((Double) ao[2]).doubleValue(), 0);
        // REVIEW
        assertEquals(adfl[3], ao[3]);

        // list
        adfl = m_reader.readDoubleArray(0);
        assertEquals(adfl.length, list.size());
        }

    @Test
    public void testReadDoubleArrayWithException()
            throws IOException
        {
        initPOFWriter();
        m_writer.writeString(0, "string_doublearray");

        initPOFReader();
        try
            {
            m_reader.readDoubleArray(0);
            fail("expected exception");
            }
        catch (IOException e)
            {
            // expected
            }
        }

    // test case for COH-3370
    @Test
    public void testPofWriterWriteUniformObjectArrayWithNull()
            throws IOException
        {
        Object[] ao1 = {new BigDecimal(32), new BigDecimal(Integer.MAX_VALUE),
                new BigDecimal(-1), null, new BigDecimal(0)};
        Object[] ao2 = {true, null, false};
        Object[] ao3 = {(byte) 65, null, (byte) PofConstants.V_REFERENCE_NULL, (byte) 0, null};
        Object[] ao4 = {'A', 'B', null};
        Object[] ao5 = {32F, Float.MAX_VALUE, (float) -1, (float) 0, null};
        Object[] ao6 = {32, Integer.MAX_VALUE, -1, 0, PofConstants.V_REFERENCE_NULL, null};
        Object[] ao7 = {new RawDateTime(new RawDate(2006, 12, 2),
                new RawTime(8, 51, 15, 100, true)), null};
        Object[] ao8 = {"test", "test3", "testPOF1", null, null, "test4"};
        Object[] ao9 = null;

        initPOFWriter();
        m_writer.writeObjectArray(0, ao1, BigDecimal.class);
        m_writer.writeObjectArray(0, ao2, Boolean.class);
        m_writer.writeObjectArray(0, ao3, Byte.class);
        m_writer.writeObjectArray(0, ao4, Character.class);
        m_writer.writeObjectArray(0, ao5, Float.class);
        m_writer.writeObjectArray(0, ao6, Integer.class);
        m_writer.writeObjectArray(0, ao7, RawDateTime.class);
        m_writer.writeObjectArray(0, ao8, String.class);
        m_writer.writeObjectArray(0, ao9, Object.class);

        BigDecimal[] ao1Result = new BigDecimal[5];
        Boolean[]    ao2Result = new Boolean[3];
        Byte[]       ao3Result = new Byte[5];
        Character[]  ao4Result = new Character[3];
        Float[]      ao5Result = new Float[5];
        Integer[]    ao6Result = new Integer[6];
        Timestamp[]  ao7Result = new Timestamp[2];
        String[]     ao8Result = new String[6];
        Object[]     ao9Result = null;

        initPOFReader();
        assertArrayEquals(m_reader.readObjectArray(0, ao1Result), ao1);
        assertArrayEquals(m_reader.readObjectArray(0, ao2Result), ao2);
        assertArrayEquals(m_reader.readObjectArray(0, ao3Result), ao3);
        assertArrayEquals(m_reader.readObjectArray(0, ao4Result), ao4);
        assertArrayEquals(m_reader.readObjectArray(0, ao5Result), ao5);
        assertArrayEquals(m_reader.readObjectArray(0, ao6Result), ao6);
        m_reader.readObjectArray(0, ao7Result);
        assertEquals(ao7Result[1], null);
        assertArrayEquals(m_reader.readObjectArray(0, ao8Result), ao8);
        assertArrayEquals(m_reader.readObjectArray(0, ao9Result), ao9);
        }
    }
