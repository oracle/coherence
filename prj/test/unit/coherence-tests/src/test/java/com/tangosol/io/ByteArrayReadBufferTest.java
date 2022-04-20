/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io;


import com.tangosol.io.ReadBuffer.BufferInput;

import com.tangosol.util.ExternalizableHelper;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;


/**
* Unit tests for ByteArrayReadBuffer.
*
* @author ch  2009.11.10
*/
public class ByteArrayReadBufferTest
        extends ExternalizableHelper
    {
    // ----- constructor tests ----------------------------------------------

    /**
    * Test the behavior of ByteArrayReadBuffer(byte[]).
    *
    * @throws IOException
    */
    @Test
    public void byteArrayConstructor()
            throws IOException
        {
        int                 cSize = 10000;
        byte[]              abCmp = createByteArray(cSize);
        ByteArrayReadBuffer buf   = new ByteArrayReadBuffer(abCmp);
        compareBuffer(buf, abCmp, 0, buf.length());
        }

    /**
    * Test the behavior of ByteArrayReadBuffer constructor and getBufferInput.
    * Verifies that -1 is returned at the end of the stream.
    *
    * @throws IOException
    */
    @Test
    public void byteArrayConstructorEOF()
            throws IOException
        {
        int                 cSize = 10000;
        byte[]              abCmp = createByteArray(cSize);
        ByteArrayReadBuffer buf   = new ByteArrayReadBuffer(abCmp);
        assertTrue(compareBuffer(buf, abCmp, 0, cSize).read() == -1);
        }

    /**
    * Test the behavior of ByteArrayReadBuffer subsection Constructor which
    * initializes using a subsection of an array. Then verify that the
    * initialized data is the same as the source array.
    *
    * @throws IOException
    */
    @Test
    public void byteArrayConstructorSubSection()
            throws IOException
        {
        int                 cSize = 10000;
        byte[]              abCmp = createByteArray(cSize);
        int                 start = cSize / 4;
        int                 range = cSize / 2;
        ByteArrayReadBuffer barb  = new ByteArrayReadBuffer(abCmp, start,
                                                           range);
        compareBuffer(barb, abCmp, start, range);
        }

    /**
    * Test the behavior of ByteArrayReadBuffer subsection Constructor which
    * initializes using a subsection of an array. In this specific case
    * the range of the subsection is larger than the available array,
    * hence a IIOB exception is expected.
    *
    * @throws IOException
    */
    @Test
    public void byteArrayConstructorSubSectionIOB()
            throws IOException
        {
        int    cSize = 10000;
        byte[] abCmp = createByteArray(cSize);
        try
            {
            new ByteArrayReadBuffer(abCmp, 2, cSize);
            fail("expected exception");
            }
        catch (IndexOutOfBoundsException e)
            {
            // expected
            }
        }

    /**
    * Test the behavior of ByteArrayReadBuffer subsection Constructor which
    * initializes using a subsection of an array. Copies the array in it's entirety
    *
    * @throws IOException
    */
    @Test
    public void byteArrayConstructorSubSectionAll()
            throws IOException
        {
        int                 cSize = 10000;
        byte[]              abCmp = createByteArray(cSize);
        ByteArrayReadBuffer buf   = new ByteArrayReadBuffer(abCmp, 0, cSize);
        compareBuffer(buf, abCmp, 0, cSize);
        }

    /**
    * Test the behavior of supplying a negative length to
    * ByteArrayReadBuffer(byte[], int, int).
    *
    * @throws IOException
    */
    @Test
    public void byteArrayConstructorSubSectionNegLength()
            throws IOException
        {
        byte[] abCmp = createByteArray(10);
        try
            {
            new ByteArrayReadBuffer(abCmp, 0, -10);
            fail("expected exception");
            }
        catch (IndexOutOfBoundsException e)
            {
            // expected
            }
        }

    /**
    * Test the behavior of supplying a negative offset to
    * ByteArrayReadBuffer(byte[], int, int).
    *
    * @throws IOException
    */
    @Test
    public void byteArrayConstructorSubSectionNegOffset()
            throws IOException
        {
        byte[] abCmp = createByteArray(10);
        try
            {
            new ByteArrayReadBuffer(abCmp, -1, 10);
            fail("expected exception");
            }
        catch (IndexOutOfBoundsException e)
            {
            // expected
            }
        }

    /**
    * Test the behavior of ByteArrayReadBuffer(byte[], int, int, boolean,
    * boolean, boolean) creating a private copy.
    *
    * @throws IOException
    */
    @Test
    public void byteArrayConstructorPrivate()
            throws IOException
        {
        int                 cSize = 10000;
        byte[]              abCmp = createByteArray(cSize);
        ByteArrayReadBuffer buf   = new ByteArrayReadBuffer(abCmp, 0, cSize,
                                                          false, true, true);

        setRawArray(buf, (byte) 1);
        compareBuffer(buf, abCmp, 0, buf.length());
        }

    /**
    * Test the behavior of ByteArrayReadBuffer(byte[], int, int, boolean,
    * boolean, boolean) creating a non-private copy.
    *
    * @throws IOException
    */
    @Test
    public void byteArrayConstructorNotPrivate()
            throws IOException
        {
        int                 cSize = 10000;
        byte[]              abCmp = createByteArray(cSize);
        ByteArrayReadBuffer buf   = new ByteArrayReadBuffer(abCmp, 0, cSize,
                                                          false, false, true);
        setRawArray(buf, (byte) 1);
        compareBuffer(buf, (byte) 1, 0, buf.length());
        }


    /**
    * Test the behavior of ByteArrayReadBuffer(byte[], int, int, boolean,
    * boolean, boolean) creating a non-private shallow copy.
    *
    * @throws IOException
    */
    @Test
    public void byteArrayConstructorNotPrivateNotShallow()
            throws IOException
        {
        int                 cSize = 10000;
        byte[]              abCmp = createByteArray(cSize);
        ByteArrayReadBuffer buf   = new ByteArrayReadBuffer(abCmp, 0, cSize,
                                                          false, false, true);
        ByteArrayReadBuffer clone = (ByteArrayReadBuffer) buf.clone();
        setRawArray(buf, (byte) 1);
        compareBuffer(buf, (byte) 1, 0, buf.length());
        compareBuffer(clone, abCmp, 0, clone.length());
        }

    /**
    * Test the behavior of ByteArrayReadBuffer.resetRange(int, int).
    */
    @Test
    public void resetRange()
            throws IOException
        {
        byte[]              abCmp = createByteArray(4);
        ByteArrayReadBuffer buf   = new ByteArrayReadBuffer(createByteArray(10));
        buf.resetRange(0, 4);
        compareBuffer(buf, abCmp, 0, buf.length());
        }

    /**
    * Test the behavior of ByteArrayReadBuffer.resetRange(int, int).
    */
    @Test
    public void resetFullRange()
            throws IOException
        {
        byte[]              abCmp = createByteArray(10);
        ByteArrayReadBuffer buf   = new ByteArrayReadBuffer(createByteArray(10));
        buf.resetRange(0, abCmp.length);
        compareBuffer(buf, abCmp, 0, buf.length());
        }

    /**
    * Test the behavior of ByteArrayReadBuffer.resetRange(int, int) with a
    * negative offset.
    */
    @Test
    public void resetRangeNegativeOffset()
            throws IOException
        {
        byte[]              abCmp = createByteArray(10);
        ByteArrayReadBuffer buf   = new ByteArrayReadBuffer(abCmp);
        try
            {
            buf.resetRange(-100, 10);
            fail("expected exception");
            }
        catch (IndexOutOfBoundsException e)
            {
            // expected
            }
        }

    /**
    * Test the behavior of ByteArrayReadBuffer.resetRange(int, int) with a
    * negative length.
    */
    @Test
    public void resetRangeNegativeLength()
            throws IOException
        {
        byte[]              abCmp = createByteArray(10);
        ByteArrayReadBuffer buf   = new ByteArrayReadBuffer(abCmp);
        try
            {
            buf.resetRange(0, -10);
            fail("expected exception");
            }
        catch (IndexOutOfBoundsException e)
            {
            // expected
            }
        }

    /**
    * Test the behavior of ByteArrayReadBuffer.getRawByteArray() with a
    * zero length array.
    */
    @Test
    public void getRawByteArrayPrivateZeroLength()
            throws IOException
        {
        int                 cSize = 0;
        byte[]              abCmp = createByteArray(cSize);
        ByteArrayReadBuffer buf   = new ByteArrayReadBuffer(abCmp, 0,
                                                          abCmp.length, false,
                                                          true, true);
        byte[] abRaw = buf.getRawByteArray();
        assertTrue("Length was not " + cSize + "(" + abRaw.length + ")",
                abRaw.length == cSize);
        compareByteArray(abCmp, abRaw);
        }

    /**
    * Test the behavior of ByteArrayReadBuffer.getRawByteArray() with a
    * zero length array.
    */
    @Test
    public void getRawByteArrayZeroLength()
            throws IOException
        {
        int                 cSize = 0;
        byte[]              abCmp = createByteArray(cSize);
        ByteArrayReadBuffer buf   = new ByteArrayReadBuffer(abCmp, 0,
                                                          abCmp.length, false,
                                                          false, false);
        byte[] abRaw = buf.getRawByteArray();
        assertTrue("Length was not " + cSize + "(" + abRaw.length + ")",
                abRaw.length == cSize);
        compareByteArray(abCmp, abRaw);
        }

    /**
    * Test the behavior of ByteArrayReadBuffer.getRawByteArray() with a
    * ten entries large array.
    */
    @Test
    public void getRawByteArrayPrivate()
            throws IOException
        {
        int                 cSize = 2048;
        byte[]              abCmp = createByteArray(cSize);
        ByteArrayReadBuffer buf   = new ByteArrayReadBuffer(abCmp, 0,
                                                          abCmp.length, false,
                                                          true, true);
        byte[] abRaw = buf.getRawByteArray();
        assertTrue("Length was not " + cSize + "(" + abRaw.length + ")",
                abRaw.length == cSize);
        compareByteArray(abCmp, abRaw);
        }


    /**
    * Test the behavior of ByteArrayReadBuffer.rawOffset() with a
    * zero length array.
    */
    @Test
    public void rawOffsetPrivate()
            throws IOException
        {
        int                 cSize = 2048;
        int                 cOff  = 30;
        byte[]              abCmp = createByteArray(cSize);
        ByteArrayReadBuffer buf   = new ByteArrayReadBuffer(abCmp, cOff,
                                                          abCmp.length - cOff,
                                                          false, true, true);
        assertTrue(buf.getRawOffset() == 0);
        }

    /**
    * Test the behavior of ByteArrayReadBuffer.rawOffset() with an array of
    * 2048 bytes.
    */
    @Test
    public void rawOffset()
            throws IOException
        {
        int                 cSize = 2048;
        int                 cOff  = 30;
        byte[]              abCmp = createByteArray(cSize);
        ByteArrayReadBuffer buf   = new ByteArrayReadBuffer(abCmp, cOff,
                                                          abCmp.length - cOff,
                                                          false, false, false);
        assertTrue(buf.getRawOffset() == cOff);
        }

    /**
     * Test the behavior of ByteArrayReadBuffer.toString() with an array of
     * 0 bytes.
     */
    @Test
     public void toString0()
            throws IOException
         {
         int                 cSize = 0;
         int                 cOff  = 30;
         byte[]              abCmp = createByteArray(cSize);
         ByteArrayReadBuffer buf   = new ByteArrayReadBuffer(abCmp);
         String string = buf.toString();
         assertTrue("length" +  string.length(), string.length() < cSize + 40);
         }

    /**
     * Test the behavior of ByteArrayReadBuffer.toString() with an array of
     * 2048 bytes.
     */
    @Test
     public void toString2048()
            throws IOException
         {
         int                 cSize = 2048;
         byte[]              abCmp = createByteArray(cSize);
         ByteArrayReadBuffer buf   = new ByteArrayReadBuffer(abCmp);
         String string = buf.toString();
         assertTrue("length" +  string.length(), string.length() == 4138);
         }

    /**
     * Test the behavior of ByteArrayReadBuffer.toString() with an array of
     * 8092 bytes.
     */
    @Test
     public void toString8092()
            throws IOException
         {
         int                 cSize = 8092;
         byte[]              abCmp = createByteArray(cSize);
         ByteArrayReadBuffer buf   = new ByteArrayReadBuffer(abCmp);
         String string = buf.toString();
         assertTrue("length" +  string.length(), string.length() < cSize);
         }


    // ----------- helpers --------------------------------------------------

    /**
    * Create a new Byte array of cSize bytes. The content of the array will
    * initialized to index % 255.
    *
    * @param cSize  the number of bytes in the array
    *
    * @return  a byte array
    */
    private byte[] createByteArray(int cSize)
        {
        byte[] ab = new byte[cSize];
        for (int i = 0; i < cSize; i++)
            {
            ab[i] = (byte) (i % 0xFF);
            }
        return ab;
        }

    /**
    * Set the content of a ByteArrayReadBuffer to value.
    *
    * @param barb  the ByteArrayReadBuffer
    * @param value the value
    */
    private void setRawArray(ByteArrayReadBuffer barb, byte value)
        {
        byte[] arrCopy = barb.getRawByteArray();

        assertTrue(arrCopy.length == barb.length());
        for (int i = 0; i < arrCopy.length; arrCopy[i++] = value)
            { }
        }

    /**
    * Compare a ByteArrayReadBuffer with a range of a byte array.
    *
    * @param barb   the ByteArrayReadBuffer
    * @param abCmp  the array to compare with
    * @param start  the start index of the abCmp array
    * @param range  the number of bytes to compare
    *
    * @return  the BufferInput forwarded by range bytes
    *
    * @throws IOException
    */
    private BufferInput compareBuffer(ByteArrayReadBuffer barb, byte[] abCmp,
                                      int start, int range)
            throws IOException
        {
        BufferInput bufferInput = barb.getBufferInput();
        for (int i = start; i < range; ++i)
            {
            byte val = (byte) bufferInput.read();
            assertTrue("Check " + val + " != " + abCmp[i] + " at " + i,
                    val == abCmp[i]);
            }
        return bufferInput;
        }

    /**
    * Check that the content of a ByteArrayReadBuffer only contains cmp.
    *
    * @param barb   the ByteArrayReadBuffer
    * @param cmp    the byte to compare
    * @param start  the start index of the abCmp array
    * @param range  the number of bytes to compare
    *
    * @return  the BufferInput forwarded by range bytes
    *
    * @throws IOException
    */
    private BufferInput compareBuffer(ByteArrayReadBuffer barb, byte cmp,
                                      int start, int range)
            throws IOException
        {
        BufferInput bufferInput = barb.getBufferInput();
        for (int i = start; i < range; ++i)
            {
            int val = bufferInput.read();
            assertTrue("Check " + val + " != " + cmp, val == cmp);
            }
        return bufferInput;
        }

    /**
    * Compares two byte arrays with each other and verifies that
    * the content is equal.
    *
    * @param abOne
    * @param abTwo
    */
    private void compareByteArray(byte[] abOne, byte[] abTwo)
        {
        assertTrue(abOne.length == abTwo.length);
        for (int i = 0; i < abOne.length; i++)
            {
            assertTrue(abTwo[i] == abOne[i]);
            }
        }
    }