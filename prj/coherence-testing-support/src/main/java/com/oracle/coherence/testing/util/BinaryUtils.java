/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.testing.util;

import com.tangosol.io.ByteArrayReadBuffer;
import com.tangosol.io.MultiBufferReadBuffer;
import com.tangosol.io.ReadBuffer;
import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.BitHelper;

import java.util.Random;

import static org.junit.Assert.assertTrue;

/**
 * @author jk 2015.04.08
 */
public class BinaryUtils
        extends Base
    {
    /**
    * Convert an ASCII string to a byte array.
    *
    * @param s  a String that contains only values 0x0000-0x00FF
    *
    * @return an array of bytes of the same length as the String
    */
    public static byte[] str2bytes(String s)
        {
        byte[] ab = new byte[s.length()];
        s.getBytes(0, s.length(), ab, 0);
        return ab;
        }

    /**
    * Convert an ASCII string to a Binary. Note that this method may also
    * pad the ends of the Binary such that the byte array that the Binary is
    * based on is larger than the byte array that would be obtained by
    * calling the toByteArray method of the Binary.
    *
    * @param s  a String that contains only values 0x0000-0x00FF
    *
    * @return a Binary value of the same length as the String
    */
    public static Binary str2bin(String s)
        {
        return invisipad(new Binary(str2bytes(s)));
        }

    /**
    * Convert a binary region into a quoted String.
    *
    * @param ab  byte array containing the binary region
    * @param of  offset into the byte array of the binary region
    * @param cb  length in bytes of the binary region
    *
    * @return a quoted String of characters, one for each byte of the passed
    *         binary region
    */
    public static String str(byte[] ab, int of, int cb)
        {
        return str(new String(ab, of, cb));
        }

    /**
    * Quote the passed string.
    *
    * @param s  a String
    *
    * @return the String, quoted
    */
    public static String str(String s)
        {
        return '\"' + s + '\"';
        }

    /**
    * Convert a ReadBuffer to a hex string.
    *
    * @param buf  a ReadBuffer or null
    *
    * @return a hex string starting with "0x", or "null"
    */
    public static String binToHex(ReadBuffer buf)
        {
        return buf == null ? "null" : toHexEscape(buf.toByteArray());
        }

    /**
    * Return true iff the contents of the specified ReadBuffers are equivalent
    * (or they are both null).
    *
    * @param buf1  the first buffer
    * @param buf2  the second buffer
    *
    * @return true iff the buffers are equivalent
    */
    public static boolean buffersEqual(ReadBuffer buf1, ReadBuffer buf2)
        {
        return buf1 == null || buf2 == null
            ? buf1 == buf2 : equals(buf1.toBinary(), buf2.toBinary());
        }

    /**
    * Convert a Binary to a ReadBuffer that is not a Binary and does not
    * extend AbstractByteArrayReadBuffer.
    *
    * @param bin  a Binary value
    *
    * @return a ReadBuffer that contains the same bytes but does not extend
    *         AbstractByteArrayReadBuffer
    */
    public static ReadBuffer toNonBinary(Binary bin)
        {
        int          cb       = bin.length();
        int          cbHalf   = cb / 2;
        ReadBuffer   buf1     = new ByteArrayReadBuffer(bin.toByteArray(0, cbHalf));
        ReadBuffer   buf2     = new ByteArrayReadBuffer(bin.toByteArray(cbHalf, cb - cbHalf));
        ReadBuffer[] abuf     = new ReadBuffer[] {buf1, buf2};
        ReadBuffer   bufMulti = new MultiBufferReadBuffer(abuf);
        assertTrue(equals(bufMulti.toBinary(), bin));
        return bufMulti;
        }

    /**
    * Randomly pad the passed Binary value such that the byte array that the
    * Binary is based on is larger than the byte array that would be obtained
    * by calling the toByteArray method of the Binary. Note that the result
    * Binary value will always be equal to the passed in Binary value
    * according to the equals method of Binary, since the padding is
    * invisible.
    *
    * @param bin  a Binary value
    *
    * @return a randomly padded Binary value
    */
    public static Binary invisipad(Binary bin)
        {
        Binary binPadded;

        Random rnd = getRandom();
        switch (rnd.nextInt(4))
            {
            case 0:
                {
                // pad front
                Binary binFront = getRandomBinary(0, 20);
                binPadded = binFront.concat(bin).toBinary(binFront.length(), bin.length());
                }
                break;

            case 1:
                {
                // pad back
                Binary binBack  = getRandomBinary(0, 20);
                binPadded = bin.concat(binBack).toBinary(0, bin.length());
                }
                break;

            case 2:
                {
                // pad front and back
                Binary binFront = getRandomBinary(0, 20);
                Binary binBack  = getRandomBinary(0, 20);
                binPadded = binFront.concat(bin).concat(binBack).toBinary(binFront.length(), bin.length());
                }
                break;

            default:
            case 3:
                // no padding
                binPadded = bin;
                break;
            }

        assertTrue(bin.equals(binPadded));

        return binPadded;
        }

    /**
    * Randomly alter the passed Binary by adding and removing random binary
    * regions.
    *
    * @param bin  the Binary to alter
    *
    * @return the randomly altered Binary
    */
    public static Binary alter(Binary bin)
        {
        // pick a number of modifications, and have the maximum number of
        // modifications generally weighted according to the size of the
        // Binary
        Random rnd   = getRandom();
        int    cMods = rnd.nextInt(1 + rnd.nextInt(1 << ((BitHelper.indexOfMSB(bin.length()) / 3) + 2)));
        int    cbMod = Math.min(5000, Math.max(20, rnd.nextInt(bin.length() / 2 + 1)));
        for (int iMod = 0; iMod < cMods; ++iMod)
            {
            switch (rnd.nextInt(Math.max(10, rnd.nextInt(cMods))))
                {
                case 0: // front
                    switch (rnd.nextInt(5))
                        {
                        case 0: // insert
                            bin = getRandomBinary(1, rnd.nextInt(cbMod) + 1).concat(bin);
                            break;

                        default:
                        case 1: // update
                            {
                            if (bin.length() > 0)
                                {
                                Binary binNew = getRandomBinary(1, rnd.nextInt(Math.min(cbMod, bin.length())) + 1);
                                bin = binNew.concat(bin.toBinary(binNew.length(), bin.length() - binNew.length()));
                                }
                            }
                            break;

                        case 2: // delete
                            {
                            if (bin.length() > 0)
                                {
                                int cbDelete = rnd.nextInt(rnd.nextInt(Math.min(cbMod, bin.length())) + 1);
                                bin = bin.toBinary(cbDelete, bin.length() - cbDelete);
                                }
                            }
                            break;
                        }
                    break;

                default:
                case 1: // middle
                    {
                    int of = rnd.nextInt(bin.length() + 1);
                    int cb = rnd.nextInt(rnd.nextInt(Math.min(cbMod, bin.length() - of) + 1) + 1);
                    Binary binFront  = bin.toBinary(0, of);
                    Binary binMiddle = bin.toBinary(of, cb);
                    Binary binBack   = bin.toBinary(of + cb, bin.length() - (of + cb));
                    switch (rnd.nextInt(8))
                        {
                        case 0: // insert
                            bin = binFront.concat(getRandomBinary(1, rnd.nextInt(cbMod) + 1)).concat(binMiddle).concat(binBack);
                            break;

                        default:
                        case 1: // update
                            bin = binFront.concat(getRandomBinary(1, rnd.nextInt(cbMod) + 1)).concat(binBack);
                            break;

                        case 2: // delete
                            bin = binFront.concat(binBack);
                            break;

                        case 99: // swap
                            {
                            if (bin.length() > 2)
                                {
                                int of2 = rnd.nextInt(bin.length() + 1);
                                int cb2 = rnd.nextInt(rnd.nextInt(Math.min(cbMod, bin.length() - of2) + 1) + 1);
                                Binary binMiddle2 = bin.toBinary(of2, cb2);
                                if (of2 < of)
                                    {
                                    int    ofTemp  = of;
                                    int    cbTemp  = cb;
                                    Binary binTemp = binMiddle;

                                    of         = of2;
                                    cb         = cb2;
                                    binMiddle  = binMiddle2;

                                    of2        = ofTemp;
                                    cb2        = cbTemp;
                                    binMiddle2 = binTemp;
                                    }
                                Binary binBetween = of + cb < of2
                                        ? bin.toBinary(of + cb, of2 - (of + cb))
                                        : Binary.NO_BINARY;

                                int ofBack = Math.max(of + cb, of2 + cb2);
                                binBack = bin.toBinary(ofBack, bin.length() - ofBack);

                                bin = binFront.concat(binMiddle).concat(binBetween).concat(binMiddle2).concat(binBack);
                                }
                            }
                            break;
                        }
                    }
                    break;

                case 2: // back
                    switch (rnd.nextInt(5))
                        {
                        case 0: // insert
                            bin = bin.concat(getRandomBinary(1, rnd.nextInt(cbMod) + 1));
                            break;

                        default:
                        case 1: // update
                            {
                            if (bin.length() > 0)
                                {
                                Binary binNew = getRandomBinary(1, rnd.nextInt(Math.min(cbMod, bin.length())) + 1);
                                bin = bin.toBinary(0, bin.length() - binNew.length()).concat(binNew);
                                }
                            }
                            break;

                        case 2: // delete
                            {
                            if (bin.length() > 0)
                                {
                                int cbDelete = rnd.nextInt(rnd.nextInt(Math.min(cbMod, bin.length())) + 1);
                                bin = bin.toBinary(0, bin.length() - cbDelete);
                                }
                            }
                            break;
                        }
                    break;
                }
            }

        return bin;
        }
    }
