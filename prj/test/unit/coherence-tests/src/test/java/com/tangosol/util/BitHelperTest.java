/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.*;


/**
* Tests for BitHelper.
*
* @author cp  2006.01.10
*/
public class BitHelperTest
        extends BitHelper
    {
    // ----- unit tests -----------------------------------------------------

    @Test
    public void testROL()
        {
        Random rnd = getRandom();

        for (int i = 0; i < 1000; ++i)
            {
            byte   b     = (byte) rnd.nextInt();
            String sOrig = toBitString(b);
            for (int c = 0; c < 16; ++c)
                {
                String sLeft = toBitString(rotateLeft(b, c));
                assertIdentical(sLeft, sOrig.substring(c%8, 8) +
                        sOrig.substring(0, c%8), "rol8 " + sOrig + ", " + c);
                }
            }

        for (int i = 0; i < 1000; ++i)
            {
            int    n     = rnd.nextInt();
            String sOrig = toBitString(n);
            for (int c = 0; c < 64; ++c)
                {
                String sLeft = toBitString(rotateLeft(n, c));
                assertIdentical(sLeft, sOrig.substring(c%32, 32) +
                        sOrig.substring(0, c%32), "rol32 " + sOrig + ", " + c);
                }
            }

        for (int i = 0; i < 1000; ++i)
            {
            long   l     = rnd.nextLong();
            String sOrig = toBitString(l);
            for (int c = 0; c < 128; ++c)
                {
                String sLeft = toBitString(rotateLeft(l, c));
                assertIdentical(sLeft, sOrig.substring(c%64, 64) +
                        sOrig.substring(0, c%64), "rol64 " + sOrig + ", " + c);
                }
            }
        }

    @Test
    public void testROR()
        {
        Random rnd = getRandom();

        for (int i = 0; i < 1000; ++i)
            {
            byte   b     = (byte) rnd.nextInt();
            String sOrig = toBitString(b);
            for (int c = 0; c < 16; ++c)
                {
                String sRight = toBitString(rotateRight(b, c));
                assertIdentical(sRight, sOrig.substring(8-c%8, 8) +
                        sOrig.substring(0, 8-c%8), "ror8 " + sOrig + ", " + c);
                }
            }

        for (int i = 0; i < 1000; ++i)
            {
            int    n     = rnd.nextInt();
            String sOrig = toBitString(n);
            for (int c = 0; c < 64; ++c)
                {
                String sRight = toBitString(rotateRight(n, c));
                assertIdentical(sRight, sOrig.substring(32-c%32, 32) +
                        sOrig.substring(0, 32-c%32), "ror32 " + sOrig + ", " + c);
                }
            }

        for (int i = 0; i < 1000; ++i)
            {
            long   l     = rnd.nextLong();
            String sOrig = toBitString(l);
            for (int c = 0; c < 128; ++c)
                {
                String sRight = toBitString(rotateRight(l, c));
                assertIdentical(sRight, sOrig.substring(64-c%64, 64) +
                        sOrig.substring(0, 64-c%64), "ror " + sOrig + ", " + c);
                }
            }
        }

    /**
    * Test {@link #countBits(byte)} and {@link #countBits(int)} and
    * {@link #countBits(long)}.
    */
    @Test
    public void testCountBits()
        {
        for (int i = 0; i < 0xFF; ++i)
            {
            int c = countBitsSlowly(i);
            assertIdentical(c, countBits((byte) i), i);
            assertIdentical(c, countBits((int)  i), i);
            assertIdentical(c, countBits((long) i), i);
            }

        Random rnd = new Random();

        for (int i = 0; i < 10000; ++i)
            {
            int n = rnd.nextInt();
            int c = countBitsSlowly(((long) n) & 0xFFFFFFFFL);
            assertIdentical(c, countBits((int)  n), n);
            assertIdentical(c, countBits(((long) n) & 0xFFFFFFFFL), n);
            }

        for (int i = 0; i < 10000; ++i)
            {
            long l = rnd.nextLong();
            int c = countBitsSlowly(l);
            assertIdentical(c, countBits(l), l);
            }
        }

    /**
    * Test {@link #indexOfLSB(byte)} and {@link #indexOfLSB(int)} and
    * {@link #indexOfLSB(long)}.
    */
    @Test
    public void testindexOfLSB()
        {
        for (int i = 0; i < 0xFF; ++i)
            {
            int c = indexOfLSBSlowly(i);
            assertIdentical(c, indexOfLSB((byte) i), i);
            assertIdentical(c, indexOfLSB((int)  i), i);
            assertIdentical(c, indexOfLSB((long) i), i);
            }

        Random rnd = new Random();

        for (int i = 0; i < 10000; ++i)
            {
            int n = rnd.nextInt();
            int c = indexOfLSBSlowly(((long) n) & 0xFFFFFFFFL);
            assertIdentical(c, indexOfLSB((int)  n), n);
            assertIdentical(c, indexOfLSB(((long) n) & 0xFFFFFFFFL), n);
            }

        for (int i = 0; i < 10000; ++i)
            {
            long l = rnd.nextLong();
            int c = indexOfLSBSlowly(l);
            assertIdentical(c, indexOfLSB(l), l);
            }
        }

    /**
    * Test {@link #indexOfMSB(byte)} and {@link #indexOfMSB(int)} and
    * {@link #indexOfMSB(long)}.
    */
    @Test
    public void testindexOfMSB()
        {
        for (int i = 0; i < 0xFF; ++i)
            {
            int c = indexOfMSBSlowly(i);
            assertIdentical(c, indexOfMSB((byte) i), i);
            assertIdentical(c, indexOfMSB((int)  i), i);
            assertIdentical(c, indexOfMSB((long) i), i);
            }

        Random rnd = new Random();

        for (int i = 0; i < 10000; ++i)
            {
            int n = rnd.nextInt();
            int c = indexOfMSBSlowly(((long) n) & 0xFFFFFFFFL);
            assertIdentical(c, indexOfMSB((int)  n), n);
            assertIdentical(c, indexOfMSB(((long) n) & 0xFFFFFFFFL), n);
            }

        for (int i = 0; i < 10000; ++i)
            {
            long l = rnd.nextLong();
            int c = indexOfMSBSlowly(l);
            assertIdentical(c, indexOfMSB(l), l);
            }
        }

    /**
    * Test {@link #toBytes}, {@link #toInt}, and {@link #toLong} methods.
    */
    @Test
    public void testToBytes()
        {
        byte[] ab;
        long   l;
        int    n;

        // test 0
        n = 0;
        ab = new byte[10];
        toBytes(n, ab, 1);
        assertArrayEquals(new byte[] {0,0,0,0,0,0,0,0,0,0}, ab);

        // test 1
        n = 1;
        ab = new byte[10];
        toBytes(n, ab, 1);
        assertArrayEquals(new byte[] {0,0,0,0,(byte) 0x01,0,0,0,0,0}, ab);

        // test 0x80000000
        n = 0x80000000;
        ab = new byte[10];
        toBytes(n, ab, 1);
        assertArrayEquals(new byte[] {0,(byte) 0x80,0,0,0,0,0,0,0,0}, ab);

        // test 0x10000000
        n = 0x10000000;
        ab = new byte[10];
        toBytes(n, ab, 1);
        assertArrayEquals(new byte[] {0,(byte) 0x10,0,0,0,0,0,0,0,0}, ab);

        // test 0xcafebabe
        n = 0xcafebabe;
        ab = new byte[10];
        toBytes(n, ab, 1);
        assertArrayEquals(new byte[] {0,
                (byte) 0xca,(byte) 0xfe,(byte) 0xba,(byte) 0xbe,0,0,0,0,0}, ab);

        // test a bunch of random numbers
        Random rnd = new Random();
        int    cn  = 10000;
        for (int i = 0; i < cn; ++i)
            {
            n = rnd.nextInt();
            assertEquals(n, toInt(toBytes(n)));
            }

        // test 0
        l = 0L;
        ab = new byte[10];
        toBytes(l, ab, 1);
        assertArrayEquals(new byte[] {0,0,0,0,0,0,0,0,0,0}, ab);

        // test 1
        l = 1L;
        ab = new byte[10];
        toBytes(l, ab, 1);
        assertArrayEquals(new byte[] {0,0,0,0,0,0,0,0,(byte) 0x01,0}, ab);

        // test 0x80000000
        l = 0x80000000L;
        ab = new byte[10];
        toBytes(l, ab, 1);
        assertArrayEquals(new byte[] {0,0,0,0,0,(byte) 0x80,0,0,0,0}, ab);

        // test 0x100000000
        l = 0x100000000L;
        ab = new byte[10];
        toBytes(l, ab, 1);
        assertArrayEquals(new byte[] {0,0,0,0,(byte) 0x01,0,0,0,0,0}, ab);

        // test 0xb00bf00dcafebabeL
        l = 0xb00bf00dcafebabeL;
        ab = new byte[10];
        toBytes(l, ab, 1);
        assertArrayEquals(new byte[] {0,(byte) 0xb0,(byte) 0x0b,(byte) 0xf0,(byte) 0x0d,
                                        (byte) 0xca,(byte) 0xfe,(byte) 0xba,(byte) 0xbe,0}, ab);

        // test a bunch of random numbers
        int cl = 10000;
        for (int i = 0; i < cl; ++i)
            {
            l = rnd.nextLong();
            assertEquals(l, toLong(toBytes(l)));
            }
        }

    /**
    * Test performance of the BitHelper versus the JDK implementation.
    */
    private static void testPerf()
        {
        Random rnd = new Random();
        int    cl = 10000000;
        long[] al = new long[cl];
        for (int i = 0; i < cl; ++i)
            {
            long l = rnd.nextLong();
            assertIdentical(countBits(l), bitCount(l), l);
            al[i] = l;
            }

        long lStart0 = System.currentTimeMillis();
        testPerfBaseline(al);
        long lStop0 = System.currentTimeMillis();

        long lStart1 = System.currentTimeMillis();
        testPerfJDKBitCount(al);
        long lStop1 = System.currentTimeMillis();

        long lStart2 = System.currentTimeMillis();
        testPerfTangosolBitCount(al);
        long lStop2 = System.currentTimeMillis();

        long lStart3 = System.currentTimeMillis();
        testPerfJDKMSB(al);
        long lStop3 = System.currentTimeMillis();

        long lStart4 = System.currentTimeMillis();
        testPerfTangosolMSB(al);
        long lStop4 = System.currentTimeMillis();

        long lStart5 = System.currentTimeMillis();
        testPerfJDKLSB(al);
        long lStop5 = System.currentTimeMillis();

        long lStart6 = System.currentTimeMillis();
        testPerfTangosolLSB(al);
        long lStop6 = System.currentTimeMillis();

        out("baseline=" + (lStop0 - lStart0) + "ms");
        out();
        out("bit count");
        out("JDK=" + (lStop1 - lStart1) + "ms");
        out("Coherence=" + (lStop2 - lStart2) + "ms");
        out();
        out("MSB");
        out("JDK=" + (lStop3 - lStart3) + "ms");
        out("Coherence=" + (lStop4 - lStart4) + "ms");
        out();
        out("LSB");
        out("JDK=" + (lStop5 - lStart5) + "ms");
        out("Coherence=" + (lStop6 - lStart6) + "ms");
        out();
        }

    private static int testPerfBaseline(long[] al)
        {
        int c = 0;
        for (int i = 0, cl = al.length; i < cl; ++i)
            {
            c ^= (int) al[i];
            }
        return c;
        }

    private static int testPerfJDKBitCount(long[] al)
        {
        int c = 0;
        for (int i = 0, cl = al.length; i < cl; ++i)
            {
            c ^= bitCount(al[i]);
            }
        return c;
        }

    private static int testPerfTangosolBitCount(long[] al)
        {
        int c = 0;
        for (int i = 0, cl = al.length; i < cl; ++i)
            {
            c ^= countBits(al[i]);
            }
        return c;
        }

    private static int testPerfJDKMSB(long[] al)
        {
        int c = 0;
        for (int i = 0, cl = al.length; i < cl; ++i)
            {
            c ^= numberOfLeadingZeros(al[i]);
            }
        return c;
        }

    private static int testPerfTangosolMSB(long[] al)
        {
        int c = 0;
        for (int i = 0, cl = al.length; i < cl; ++i)
            {
            c ^= indexOfMSB(al[i]);
            }
        return c;
        }

    private static int testPerfJDKLSB(long[] al)
        {
        int c = 0;
        for (int i = 0, cl = al.length; i < cl; ++i)
            {
            c ^= numberOfTrailingZeros(al[i]);
            }
        return c;
        }

    private static int testPerfTangosolLSB(long[] al)
        {
        int c = 0;
        for (int i = 0, cl = al.length; i < cl; ++i)
            {
            c ^= indexOfLSB(al[i]);
            }
        return c;
        }


    // ----- JDK comparison code --------------------------------------------

    /**
    * From JDK 1.5
    *
    * @param i  a long
    *
    * @return the count of bits
    */
    public static int bitCount(long i)
        {
        // HD, Figure 5-14
        i = i - ((i >>> 1) & 0x5555555555555555L);
        i = (i & 0x3333333333333333L) + ((i >>> 2) & 0x3333333333333333L);
        i = (i + (i >>> 4)) & 0x0f0f0f0f0f0f0f0fL;
        i = i + (i >>> 8);
        i = i + (i >>> 16);
        i = i + (i >>> 32);
        return (int)i & 0x7f;
        }

    /**
    * From JDK 1.5
    *
    * @param i  a long
    *
    * @return the count of leading zeros
    */
    public static int numberOfLeadingZeros(long i)
        {
        // HD, Figure 5-6
        if (i == 0)
            return 64;
        int n = 1;
        int x = (int)(i >>> 32);
        if (x == 0) { n += 32; x = (int)i; }
        if (x >>> 16 == 0) { n += 16; x <<= 16; }
        if (x >>> 24 == 0) { n +=  8; x <<=  8; }
        if (x >>> 28 == 0) { n +=  4; x <<=  4; }
        if (x >>> 30 == 0) { n +=  2; x <<=  2; }
        n -= x >>> 31;
        return n;
        }

    /**
    * From JDK 1.5
    *
    * @param i  a long
    *
    * @return the count of trailing zeros
    */
    public static int numberOfTrailingZeros(long i)
        {
        // HD, Figure 5-14
        int x, y;
        if (i == 0) return 64;
        int n = 63;
        y = (int)i; if (y != 0) { n = n -32; x = y; } else x = (int)(i>>>32);
        y = x <<16; if (y != 0) { n = n -16; x = y; }
        y = x << 8; if (y != 0) { n = n - 8; x = y; }
        y = x << 4; if (y != 0) { n = n - 4; x = y; }
        y = x << 2; if (y != 0) { n = n - 2; x = y; }
        return n - ((x << 1) >>> 31);
        }


    // ----- helper methods -------------------------------------------------

    /**
    * Brute force bit counter.
    *
    * @param l  a long
    *
    * @return the number of "one" bits in the long
    */
    public static int countBitsSlowly(long l)
        {
        int c = 0;
        while (l != 0L)
            {
            if ((l & 1L) != 0)
                {
                ++c;
                }
            l >>>= 1;
            }
        return c;
        }

    /**
    * Brute force LSB calculator.
    *
    * @param l  a long
    *
    * @return the LSB of the long
    */
    public static int indexOfLSBSlowly(long l)
        {
        int i = 0;
        while (l != 0L)
            {
            if ((l & 1L) != 0)
                {
                return i;
                }
            l >>>= 1;
            ++i;
            }
        return -1;
        }

    /**
    * Brute force MSB calculator.
    *
    * @param l  a long
    *
    * @return the MSB of the long
    */
    public static int indexOfMSBSlowly(long l)
        {
        int i = 0, c = -1;
        while (l != 0L)
            {
            if ((l & 1L) != 0)
                {
                c = i;
                }
            l >>>= 1;
            ++i;
            }
        return c;
        }

    /**
    * Check two numbers for equality.
    *
    * @param n1    the first number
    * @param n2    the second number
    * @param nSrc  the source value for the test
    *
    * @throws RuntimeException if <tt>n1</tt> differs from <tt>n2</tt>
    */
    public static void assertIdentical(int n1, int n2, long nSrc)
        {
        assertEquals("results differ for test value=" + nSrc + ", result1="
                + n1 + ", result2=" + n2, n1, n2);
        }

    /**
    * Check two strings for equality.
    *
    * @param s1    the first string
    * @param s2    the second string
    * @param sDesc the test description
    *
    * @throws RuntimeException if <tt>s1</tt> differs from <tt>s2</tt>
    */
    public static void assertIdentical(String s1, String s2, String sDesc)
        {
        assertEquals("results differ for test=" + sDesc + ", result1=" + s1
                + ", result2=" + s2, s1, s2);
        }
    }
