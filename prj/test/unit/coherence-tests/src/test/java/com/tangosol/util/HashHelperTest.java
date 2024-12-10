/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * A number of tests for {@link HashHelper}.
 *
 * @author hr
 * @since 3.7.2
 */
public class HashHelperTest
    {
    @Test
    public void testBoolean()
        {
        assertEquals(0x04DF, HashHelper.hash(true, 1));
        assertEquals(0x0525, HashHelper.hash(false, 31));

        // array
        boolean[] afGeorge = {true};
        assertEquals(0x000005CF, HashHelper.hash(afGeorge, 1));
        }

    @Test
    public void testByte()
        {
        assertEquals(0x11, HashHelper.hash((byte) 0x1, 1));
        assertEquals(0x01D0, HashHelper.hash((byte) 0x20, 31));

        // array
        byte[] abOctet = {0x1};
        assertEquals(0x00000101, HashHelper.hash(abOctet, 1));
        }

    @Test
    public void testChar()
        {
        assertEquals(0x01D0, HashHelper.hash(' ', 31));

        // array
        char[] achChar = {' '};
        assertEquals(0x00000120, HashHelper.hash(achChar, 1));
        }

    @Test
    public void testDouble()
        {
        assertEquals(0x3FF00010, HashHelper.hash(1.0d, 1));
        assertEquals(-0x73733E0F, HashHelper.hash(32.1d, 31));

        // array
        double[] adflDouble = {1.0d};
        assertEquals(0x3FF00100, HashHelper.hash(adflDouble, 1));
        }

    @Test
    public void testFloat()
        {
        assertEquals(0x3F800010, HashHelper.hash(1.0f, 1));
        // float value produces a different hash than the double
        // equivalent due the xor of the long's 2 chunked 4 bytes.
        assertEquals(0x42006796, HashHelper.hash(32.1f, 31));

        // array
        float[] aflFloat = {1.0f};
        assertEquals(0x3F800100, HashHelper.hash(aflFloat, 1));
        }

    @Test
    public void testInt()
        {
        assertEquals(0x0011, HashHelper.hash(1, 1));
        assertEquals(0x01D0, HashHelper.hash(32, 31));

        // array
        int[] anInt = {1};
        assertEquals(0x00000101, HashHelper.hash(anInt, 1));
        }

    @Test
    public void testLong()
        {
        assertEquals(0x0011, HashHelper.hash(1L, 1));
        assertEquals(0x01D0, HashHelper.hash(32L, 31));

        // array
        long[] alLong = {1L};
        assertEquals(0x00000101, HashHelper.hash(alLong, 1));
        }

    @Test
    public void testShort()
        {
        assertEquals(0x0011, HashHelper.hash((short) 1, 1));
        assertEquals(0x01D0, HashHelper.hash((short) 32, 31));

        // array
        short[] ashShort = {1};
        assertEquals(0x00000101, HashHelper.hash(ashShort, 1));
        }

    @Test
    public void testObject()
        {
        List<Integer> values = new ArrayList();
        values.add(1);
        // whilst the below expected values may not mean a lot to the
        // human eye, they are the expected hashes
        assertEquals(0x000014DF, HashHelper.hash((Object) new boolean[] {true}, 1));
        assertEquals(0x00001011, HashHelper.hash((Object) new byte   [] {0x1}, 1));
        assertEquals(0x00001030, HashHelper.hash((Object) new char   [] {' '}, 1));
        assertEquals(0x3FF01010, HashHelper.hash((Object) new double [] {1.0d}, 1));
        assertEquals(0x3F801010, HashHelper.hash((Object) new float  [] {1.0f}, 1));
        assertEquals(0x00001011, HashHelper.hash((Object) new int    [] {1}, 1));
        assertEquals(0x00001011, HashHelper.hash((Object) new long   [] {1L}, 1));
        assertEquals(0x00001011, HashHelper.hash((Object) new short  [] {(short) 1}, 1));
        assertEquals(0x00000011, HashHelper.hash(values, 1));
        }
    }