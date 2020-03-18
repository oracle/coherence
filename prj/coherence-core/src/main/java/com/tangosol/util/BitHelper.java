/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


/**
* A collection of methods for bit-based operations.
*
* @author cp  2006.01.10  starting from the MemberSet implementation
*/
public class BitHelper
        extends Base
    {
    // ----- bit rotation ---------------------------------------------------

    /**
    * Rotate the bits of the passed byte value to the left by the number of
    * bits specified.
    *
    * @param b      a <tt>byte</tt> value
    * @param cBits  the number of bit rotations to perform
    *
    * @return the value with its bits rotated as indicated
    *
    * @since Coherence 3.5
    */
    public static byte rotateLeft(byte b, int cBits)
        {
        int n = b & 0xFF;
        cBits &= 0x7;
        return (byte) ((n >>> (8 - cBits)) | (n << cBits));
        }

    /**
    * Rotate the bits of the passed byte value to the right by the number of
    * bits specified.
    *
    * @param b      a <tt>byte</tt> value
    * @param cBits  the number of bit rotations to perform
    *
    * @return the value with its bits rotated as indicated
    *
    * @since Coherence 3.5
    */
    public static byte rotateRight(byte b, int cBits)
        {
        int n = b & 0xFF;
        cBits &= 0x7;
        return (byte) ((n >>> cBits) | (n << (8 - cBits)));
        }

    /**
    * Rotate the bits of the passed int value to the left by the number of
    * bits specified.
    *
    * @param n      an <tt>int</tt> value
    * @param cBits  the number of bit rotations to perform
    *
    * @return the value with its bits rotated as indicated
    *
    * @since Coherence 3.5
    */
    public static int rotateLeft(int n, int cBits)
        {
        return Integer.rotateLeft(n, cBits);
        }

    /**
    * Rotate the bits of the passed int value to the right by the number of
    * bits specified.
    *
    * @param n      an <tt>int</tt> value
    * @param cBits  the number of bit rotations to perform
    *
    * @return the value with its bits rotated as indicated
    *
    * @since Coherence 3.5
    */
    public static int rotateRight(int n, int cBits)
        {
        return Integer.rotateRight(n, cBits);
        }

    /**
    * Rotate the bits of the passed long value to the left by the number of
    * bits specified.
    *
    * @param n      a <tt>long</tt> value
    * @param cBits  the number of bit rotations to perform
    *
    * @return the value with its bits rotated as indicated
    *
    * @since Coherence 3.5
    */
    public static long rotateLeft(long n, int cBits)
        {
        return Long.rotateLeft(n, cBits);
        }

    /**
    * Rotate the bits of the passed long value to the right by the number of
    * bits specified.
    *
    * @param n      a <tt>long</tt> value
    * @param cBits  the number of bit rotations to perform
    *
    * @return the value with its bits rotated as indicated
    *
    * @since Coherence 3.5
    */
    public static long rotateRight(long n, int cBits)
        {
        return Long.rotateRight(n, cBits);
        }


    // ----- bit counting ---------------------------------------------------

    /**
    * Count the number of bits set in the passed integral value.
    *
    * @param b  a byte
    *
    * @return the number of bits set in the byte [0..8]
    */
    public static int countBits(byte b)
        {
        return BIT_COUNT[b & 0xFF];
        }

    /**
    * Count the number of bits set in the passed integral value.
    *
    * @param n  an int
    *
    * @return the number of bits set in the int [0..32]
    */
    public static int countBits(int n)
        {
        return Integer.bitCount(n);
        }

    /**
    * Count the number of bits set in the passed integral value.
    *
    * @param l  a long
    *
    * @return the number of bits set in the long [0..64]
    */
    public static int countBits(long l)
        {
        return Long.bitCount(l);
        }


    // ----- most significant bit (MSB) -------------------------------------

    /**
    * Determine the most significant bit of the passed integral value.
    *
    * @param b  a byte
    *
    * @return -1 if no bits are set; otherwise, the bit position <tt>p</tt>
    *         of the most significant bit such that <tt>1 &lt;&lt; p</tt> is
    *         the most significant bit of <tt>b</tt>
    */
    public static int indexOfMSB(byte b)
        {
        return BIT_LEFTMOST[b & 0xFF];
        }

    /**
    * Determine the most significant bit of the passed integral value.
    *
    * @param n  an int
    *
    * @return -1 if no bits are set; otherwise, the bit position <tt>p</tt>
    *         of the most significant bit such that <tt>1 &lt;&lt; p</tt> is
    *         the most significant bit of <tt>n</tt>
    */
    public static int indexOfMSB(int n)
        {
        return n == 0L ? -1 : Integer.numberOfTrailingZeros(Integer.highestOneBit(n));
        }

    /**
    * Determine the most significant bit of the passed integral value.
    *
    * @param l  a long
    *
    * @return -1 if no bits are set; otherwise, the bit position <tt>p</tt>
    *         of the most significant bit such that <tt>1 &lt;&lt; p</tt> is
    *         the most significant bit of <tt>l</tt>
    */
    public static int indexOfMSB(long l)
        {
        return l == 0L ? -1 : Long.numberOfTrailingZeros(Long.highestOneBit(l));
        }


    // ----- least significant bit (LSB) ------------------------------------

    /**
    * Determine the least significant bit of the passed integral value.
    *
    * @param b  a byte
    *
    * @return -1 if no bits are set; otherwise, the bit position <tt>p</tt>
    *         of the least significant bit such that <tt>1 &lt;&lt; p</tt> is
    *         the least significant bit of <tt>b</tt>
    */
    public static int indexOfLSB(byte b)
        {
        return BIT_RIGHTMOST[b & 0xFF];
        }

    /**
    * Determine the least significant bit of the passed integral value.
    *
    * @param n  an int
    *
    * @return -1 if no bits are set; otherwise, the bit position <tt>p</tt>
    *         of the least significant bit such that <tt>1 &lt;&lt; p</tt> is
    *         the least significant bit of <tt>n</tt>
    */
    public static int indexOfLSB(int n)
        {
        return n == 0L ? -1 : Integer.numberOfTrailingZeros(Integer.lowestOneBit(n));
        }

    /**
    * Determine the least significant bit of the passed integral value.
    *
    * @param l  a long
    *
    * @return -1 if no bits are set; otherwise, the bit position <tt>p</tt>
    *         of the least significant bit such that <tt>1 &lt;&lt; p</tt> is
    *         the least significant bit of <tt>l</tt>
    */
    public static int indexOfLSB(long l)
        {
        return l == 0L ? -1 : Long.numberOfTrailingZeros(Long.lowestOneBit(l));
        }


    // ----- string formatting ----------------------------------------------

    /**
    * Convert a byte to a String of ones and zeros.
    *
    * @param b  a byte
    *
    * @return a String of ones and zeros representing the byte value
    */
    public static String toBitString(byte b)
        {
        char[] ach = new char[8];
        for (int i = 7; i >= 0; --i)
            {
            ach[i] = (char) ('0' + (b & 0x01));
            b >>>= 1;
            }
        return new String(ach);
        }

    /**
    * Convert an int to a String of ones and zeros.
    *
    * @param n  an int
    *
    * @return a String of ones and zeros representing the int value
    */
    public static String toBitString(int n)
        {
        char[] ach = new char[32];
        for (int i = 31; i >= 0; --i)
            {
            ach[i] = (char) ('0' + (n & 0x01));
            n >>>= 1;
            }
        return new String(ach);
        }

    /**
    * Convert a long to a String of ones and zeros.
    *
    * @param l  a long
    *
    * @return a String of ones and zeros representing the long value
    */
    public static String toBitString(long l)
        {
        char[] ach = new char[64];
        for (int i = 63; i >= 0; --i)
            {
            ach[i] = (char) ('0' + ((int) l & 0x01));
            l >>>= 1;
            }
        return new String(ach);
        }


    // ----- byte[] helpers -------------------------------------------------

    /**
     * Convert the specified int into a series of eight bytes, and write them
     * to the specified byte-array in big-endian form (MSB first).
     *
     * @param n   the int to convert to bytes
     * @param ab  the byte array to write into
     * @param of  the starting offset
     *
     * @throws ArrayIndexOutOfBoundsException iff the length of the byte array
     *         is shorter than <code>of + 4</code>
     */
    public static void toBytes(int n, byte[] ab, int of)
        {
        ab[of    ] = (byte) (n >>> 24);
        ab[of + 1] = (byte) (n >>> 16);
        ab[of + 2] = (byte) (n >>>  8);
        ab[of + 3] = (byte) (n);
        }

    /**
     * Convert the specified int into a byte array containing a series of eight
     * bytes in big-endian form (MSB first).
     *
     * @param l  the int to convert to bytes
     *
     * @return a byte[] representing the big-endian representation of the int
     */
    public static byte[] toBytes(int l)
        {
        byte[] ab = new byte[4];
        toBytes(l, ab, 0);
        return ab;
        }

    /**
     * Convert the specified long into a series of eight bytes, and write them
     * to the specified byte-array in big-endian form (MSB first).
     *
     * @param l   the long to convert to bytes
     * @param ab  the byte array to write into
     * @param of  the starting offset
     *
     * @throws ArrayIndexOutOfBoundsException iff the length of the byte array
     *         is shorter than <code>of + 8</code>
     */
    public static void toBytes(long l, byte[] ab, int of)
        {
        ab[of    ] = (byte) (l >>> 56);
        ab[of + 1] = (byte) (l >>> 48);
        ab[of + 2] = (byte) (l >>> 40);
        ab[of + 3] = (byte) (l >>> 32);
        ab[of + 4] = (byte) (l >>> 24);
        ab[of + 5] = (byte) (l >>> 16);
        ab[of + 6] = (byte) (l >>>  8);
        ab[of + 7] = (byte) (l);
        }

    /**
     * Convert the specified long into a byte array containing a series of eight
     * bytes in big-endian form (MSB first).
     *
     * @param l  the long to convert to bytes
     *
     * @return a byte[] representing the big-endian representation of the long
     */
    public static byte[] toBytes(long l)
        {
        byte[] ab = new byte[8];
        toBytes(l, ab, 0);
        return ab;
        }

    /**
     * Return the int represented by the sequence of eight bytes (in big-endian
     * form) in the specified byte-array starting at the given offset.
     *
     * @param ab  the byte-array
     * @param of  the offset
     *
     * @return the int represented by the sequence of bytes
     *
     * @throws ArrayIndexOutOfBoundsException iff the length of the byte array
     *         is less than <code>of + 4</code>
     */
    public static int toInt(byte[] ab, int of)
        {
        return    ((ab[of    ]       ) << 24)
                | ((ab[of + 1] & 0xFF) << 16)
                | ((ab[of + 2] & 0xFF) << 8 )
                | ((ab[of + 3] & 0xFF)      );
        }

    /**
     * Return the int represented by the sequence of eight bytes (in big-endian
     * form) in the specified byte-array.
     *
     * @param ab  the byte-array
     *
     * @return the int represented by the byte-array
     *
     * @throws ArrayIndexOutOfBoundsException iff the length of the byte array
     *         is less than <code>4</code>
     */
    public static int toInt(byte[] ab)
        {
        return toInt(ab, 0);
        }

    /**
     * Return the long represented by the sequence of eight bytes (in big-endian
     * form) in the specified byte-array starting at the given offset.
     *
     * @param ab  the byte-array
     * @param of  the offset
     *
     * @return the long represented by the sequence of bytes
     *
     * @throws ArrayIndexOutOfBoundsException iff the length of the byte array
     *         is less than <code>of + 8</code>
     */
    public static long toLong(byte[] ab, int of)
        {
        long n1 = ((ab[of    ]       ) << 24)
                | ((ab[of + 1] & 0xFF) << 16)
                | ((ab[of + 2] & 0xFF) << 8 )
                | ((ab[of + 3] & 0xFF)      );
        long n2 = ((ab[of + 4]       ) << 24)
                | ((ab[of + 5] & 0xFF) << 16)
                | ((ab[of + 6] & 0xFF) << 8 )
                | ((ab[of + 7] & 0xFF)      );

        return (n1 << 32) | (n2 & 0xFFFFFFFFL);
        }

    /**
     * Return the long represented by the sequence of eight bytes (in big-endian
     * form) in the specified byte-array.
     *
     * @param ab  the byte-array
     *
     * @return the long represented by the byte-array
     *
     * @throws ArrayIndexOutOfBoundsException iff the length of the byte array
     *         is less than <code>8</code>
     */
    public static long toLong(byte[] ab)
        {
        return toLong(ab, 0);
        }


    // ----- constants ------------------------------------------------------

    /**
    * Array that maps a byte value to a number of bits set in that byte.
    */
    private static final byte[] BIT_COUNT =
        {
        0,1,1,2,1,2,2,3,1,2,2,3,2,3,3,4,
        1,2,2,3,2,3,3,4,2,3,3,4,3,4,4,5,
        1,2,2,3,2,3,3,4,2,3,3,4,3,4,4,5,
        2,3,3,4,3,4,4,5,3,4,4,5,4,5,5,6,
        1,2,2,3,2,3,3,4,2,3,3,4,3,4,4,5,
        2,3,3,4,3,4,4,5,3,4,4,5,4,5,5,6,
        2,3,3,4,3,4,4,5,3,4,4,5,4,5,5,6,
        3,4,4,5,4,5,5,6,4,5,5,6,5,6,6,7,
        1,2,2,3,2,3,3,4,2,3,3,4,3,4,4,5,
        2,3,3,4,3,4,4,5,3,4,4,5,4,5,5,6,
        2,3,3,4,3,4,4,5,3,4,4,5,4,5,5,6,
        3,4,4,5,4,5,5,6,4,5,5,6,5,6,6,7,
        2,3,3,4,3,4,4,5,3,4,4,5,4,5,5,6,
        3,4,4,5,4,5,5,6,4,5,5,6,5,6,6,7,
        3,4,4,5,4,5,5,6,4,5,5,6,5,6,6,7,
        4,5,5,6,5,6,6,7,5,6,6,7,6,7,7,8,
        };

    /**
    * Array that maps a byte value to the bit position (0..7) of its most
    * significant bit.
    */
    private static final byte[] BIT_LEFTMOST =
        {
       -1,0,1,1,2,2,2,2,3,3,3,3,3,3,3,3,
        4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,
        5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,
        5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,
        6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,
        6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,
        6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,
        6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,
        7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
        7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
        7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
        7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
        7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
        7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
        7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
        7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
        };

    /**
    * Array that maps a byte value to the bit position (0..7) of its least
    * significant bit.
    */
    private static final byte[] BIT_RIGHTMOST =
        {
       -1,0,1,0,2,0,1,0,3,0,1,0,2,0,1,0,
        4,0,1,0,2,0,1,0,3,0,1,0,2,0,1,0,
        5,0,1,0,2,0,1,0,3,0,1,0,2,0,1,0,
        4,0,1,0,2,0,1,0,3,0,1,0,2,0,1,0,
        6,0,1,0,2,0,1,0,3,0,1,0,2,0,1,0,
        4,0,1,0,2,0,1,0,3,0,1,0,2,0,1,0,
        5,0,1,0,2,0,1,0,3,0,1,0,2,0,1,0,
        4,0,1,0,2,0,1,0,3,0,1,0,2,0,1,0,
        7,0,1,0,2,0,1,0,3,0,1,0,2,0,1,0,
        4,0,1,0,2,0,1,0,3,0,1,0,2,0,1,0,
        5,0,1,0,2,0,1,0,3,0,1,0,2,0,1,0,
        4,0,1,0,2,0,1,0,3,0,1,0,2,0,1,0,
        6,0,1,0,2,0,1,0,3,0,1,0,2,0,1,0,
        4,0,1,0,2,0,1,0,3,0,1,0,2,0,1,0,
        5,0,1,0,2,0,1,0,3,0,1,0,2,0,1,0,
        4,0,1,0,2,0,1,0,3,0,1,0,2,0,1,0,
        };
    }
