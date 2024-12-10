/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io;


import com.tangosol.io.ReadBuffer.BufferInput;

import com.tangosol.io.nio.ByteBufferOutputStream;
import com.tangosol.io.nio.ByteBufferReadBuffer;

import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;

import com.tangosol.util.ExternalizableHelperTest;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.nio.ByteBuffer;

import org.junit.Test;

import static org.junit.Assert.*;


/**
*
* @author ch 2009-12-07
*/
public class MultiBufferReadBufferTest extends ExternalizableHelper
    {
    /**
    * Test the behavior of MultiBufferReadBufferContructor(ReadBuffer[]).
    *
    * @throws IOException
    */
    @Test
    public void contructor()
        {
        int                   cBuffers = 256;
        int                   cSize    = 10000;
        MultiBufferReadBuffer buf      = new MultiBufferReadBuffer(
                    createReadBuffers(cBuffers, cSize));

        checkMultiBuffer(buf, cBuffers, cSize);
        }

    /**
    * Test the behavior of MultiBufferReadBufferContructor(ReadBuffer[]) of 0 Size.
    *
    * @throws IOException
    */
    @Test
    public void contructorZeroReadBuffer()
        {
        int                   cBuffers = 256;
        int                   cSize    = 0;
        MultiBufferReadBuffer buf      = new MultiBufferReadBuffer(
                                          createReadBuffers(cBuffers, cSize));

        checkMultiBuffer(buf, cBuffers, cSize);
        }

    /**
    * Test the behavior of MultiBufferReadBufferContructor(ReadBuffer[]) based on a single
    * ReadBuffer.
    *
    * @throws IOException
    */
    @Test
    public void contructorSingleReadBuffer()
        {
        int                   cBuffers = 1;
        int                   cSize    = 16384;
        MultiBufferReadBuffer buf      = new MultiBufferReadBuffer(
                                      createReadBuffers(cBuffers, cSize));

        checkMultiBuffer(buf, cBuffers, cSize);
        }

    /**
    * Test the behavior of MultiBufferReadBufferContructor(ReadBuffer[])
    * and getDestructiveBufferInput().
    *
    * @throws IOException
    */
    @Test
    public void getDestructiveBufferInput()
        throws IOException
        {
        int                   cBuffers = 256;
        int                   cSize    = 256;
        MultiBufferReadBuffer buf      = new MultiBufferReadBuffer(
                                            createReadBuffers(cBuffers, cSize));

        checkMultiBuffer(buf, cBuffers, cSize);
        BufferInput bIn = buf.getDestructiveBufferInput();
        checkBufferInput(buf, bIn);
        }

    /**
     * Test the behavior of MultiBufferReadBufferContructor(ReadBuffer[])
     * and getDestructiveBufferInput().
     *
     * @throws IOException
     */
    @Test
    public void testNullObjectInputFilter()
            throws IOException
        {
        int                   cBuffers = 256;
        int                   cSize    = 256;
        MultiBufferReadBuffer buf      = new MultiBufferReadBuffer(
                createReadBuffers(cBuffers, cSize));

        checkMultiBuffer(buf, cBuffers, cSize);
        BufferInput bIn = buf.getDestructiveBufferInput();

        assertNull(bIn.getObjectInputFilter());
        checkBufferInput(buf, bIn);
        }

    /**
     * Test the behavior of MultiBufferReadBufferContructor(ReadBuffer[])
     * and getDestructiveBufferInput().
     *
     * @throws IOException
     */
    @Test
    public void testSetObjectInputFilter()
            throws IOException
        {
        int                   cBuffers = 256;
        int                   cSize    = 256;
        MultiBufferReadBuffer buf      = new MultiBufferReadBuffer(
                createReadBuffers(cBuffers, cSize));

        checkMultiBuffer(buf, cBuffers, cSize);

        BufferInput bIn          = buf.getDestructiveBufferInput();
        Object      oInputFilter = ExternalizableHelperTest.createObjectInputFilter("data.*");

        bIn.setObjectInputFilter(oInputFilter);
        assertEquals(oInputFilter, bIn.getObjectInputFilter());

        checkBufferInput(buf, bIn);
        }
    /**
    * Test for byteAt(int of)
    */
    @Test
    public void byteAt0() throws IOException
        {
        int                   cBuffers = 256;
        int                   cSize    = 256;
        int                   of       = 0;
        MultiBufferReadBuffer buf      =  new MultiBufferReadBuffer(
               createReadBuffers(cBuffers, cSize));
        checkMultiBuffer(buf, cBuffers, cSize);

        checkByteAt(buf, cSize, of);
        }

    /**
    * Test for byteAtEnd(int of)
    */
    @Test
    public void byteAtEndOfReadBuffer() throws IOException
        {
        int                   cBuffers = 256;
        int                   cSize    = 256;
        int                   of       = 256;
        MultiBufferReadBuffer buf      =  new MultiBufferReadBuffer(
                createReadBuffers(cBuffers, cSize));
        checkMultiBuffer(buf, cBuffers, cSize);

        checkByteAt(buf, cSize, of);
        }

    /**
    * public byte byteAtEnd(int of)
    */
    @Test
    public void byteAtEndOfBuffer() throws IOException
        {
        int                   cBuffers = 256;
        int                   cSize    = 256;
        int                   of       = 256 * 256 - 1;
        MultiBufferReadBuffer buf      =  new MultiBufferReadBuffer(
                 createReadBuffers(cBuffers, cSize));
        checkMultiBuffer(buf, cBuffers, cSize);

        checkByteAt(buf, cSize, of);
        }

    /**
    * public byte byteAtEnd(int of)
    */
    @Test
    public void byteOneOverBuffer() throws IOException
        {
        int                   cBuffers = 256;
        int                   cSize    = 256;
        int                   of       = 256 * 256;
        MultiBufferReadBuffer buf      =  new MultiBufferReadBuffer(
                 createReadBuffers(cBuffers, cSize));
        checkMultiBuffer(buf, cBuffers, cSize);

        try
            {
            checkByteAt(buf, cSize, of);
            fail("expected exception");
            }
        catch (IndexOutOfBoundsException e)
            {
            // expected
            }
        }

    /**
    * public byte byteAtEnd(int of)
    */
    @Test
    public void byteOnePreviousBuffer() throws IOException
        {
        int                   cBuffers = 256;
        int                   cSize    = 256;
        int                   of       = -1;
        MultiBufferReadBuffer buf      =  new MultiBufferReadBuffer(
                  createReadBuffers(cBuffers, cSize));
        checkMultiBuffer(buf, cBuffers, cSize);
        try
            {
            checkByteAt(buf, cSize, of);
            fail("expected exception");
            }
        catch (IndexOutOfBoundsException e)
            {
            // expected
            }
        }

    /**
    * Test the behavior of MultiBufferReadBufferContructor(ReadBuffer[])
    * and getDestructiveBufferInput().
    *
    * @throws IOException
    */
    @Test
    public void copyBytes() throws IOException
        {
        int                   cBuffers = 256;
        int                   cSize    = 2560;
        int                   ofBegin  = 100;
        int                   ofEnd    = 110;
        int                   ofDest   = 0;
        byte[]                abDest   = new byte[ofEnd - ofBegin + ofDest];

        MultiBufferReadBuffer buf      =  new MultiBufferReadBuffer(
                   createReadBuffers(cBuffers, cSize));
        checkMultiBuffer(buf, cBuffers, cSize);
        buf.copyBytes(ofBegin, ofEnd, abDest, ofDest);
        checkByteArray(buf, ofBegin, ofEnd, abDest, ofDest);
        }

    /**
    * Test the behavior of MultiBufferReadBuffer(ReadBuffer[])
    * and getDestructiveBufferInput().
    *
    * @throws IOException
    */
    @Test
    public void copyBytesZero() throws IOException
        {
        int                   cBuffers = 256;
        int                   cSize    = 2560;
        int                   ofBegin  = 100;
        int                   ofEnd    = 100;
        int                   ofDest   = 0;
        byte[]                abDest   = new byte[ofEnd - ofBegin + ofDest];

        MultiBufferReadBuffer buf      =  new MultiBufferReadBuffer(
                    createReadBuffers(cBuffers, cSize));
        checkMultiBuffer(buf, cBuffers, cSize);
        buf.copyBytes(ofBegin, ofEnd, abDest, ofDest);
        checkByteArray(buf, ofBegin, ofEnd, abDest, ofDest);
        }

    /**
    * Test the behavior of MultiBufferReadBuffer(ReadBuffer[])
    * and getDestructiveBufferInput().
    *
    * @throws IOException
    */
    @Test
    public void copyBytesHighOffset() throws IOException
        {
        int                   cBuffers = 256;
        int                   cSize    = 2560;
        int                   ofBegin  = 100;
        int                   ofEnd    = 100;
        int                   ofDest   = 50;
        byte[]                abDest   = new byte[ofEnd - ofBegin + ofDest];

        MultiBufferReadBuffer buf      =  new MultiBufferReadBuffer(
                     createReadBuffers(cBuffers, cSize));
        checkMultiBuffer(buf, cBuffers, cSize);
        buf.copyBytes(ofBegin, ofEnd, abDest, ofDest);
        checkByteArray(buf, ofBegin, ofEnd, abDest, ofDest);
        }

    /**
    * Test the behavior of MultiBufferReadBuffer(ReadBuffer[])
    * and getDestructiveBufferInput().
    *
    * @throws IOException
    */
    // TODO
    @Test
    public void copyBytesOffsetOutside() throws IOException
        {
        int                   cBuffers = 256;
        int                   cSize    = 2560;
        int                   ofBegin  = 100;
        int                   ofEnd    = 200;
        int                   ofDest   = 100;
        byte[]                abDest   = new byte[ofEnd - ofBegin];

        MultiBufferReadBuffer buf      =  new MultiBufferReadBuffer(
                      createReadBuffers(cBuffers, cSize));
        checkMultiBuffer(buf, cBuffers, cSize);
        try
            {
            buf.copyBytes(ofBegin, ofEnd, abDest, ofDest);
            fail("expected exception");
            }
        catch (IndexOutOfBoundsException e)
            {
            // expected
            }
        }

    /**
    * Test the behavior of MultiBufferReadBuffer(ReadBuffer[])
    * and getDestructiveBufferInput().
    *
    * @throws IOException
    */
    @Test
    public void copyBytesCompleteReadBuffer() throws IOException
        {
        int                   cBuffers = 256;
        int                   cSize    = 256;
        int                   ofBegin  = 0;
        int                   ofEnd    = 256;
        int                   ofDest   = 0;
        byte[]                abDest   = new byte[ofEnd - ofBegin + ofDest];

        MultiBufferReadBuffer buf      =  new MultiBufferReadBuffer(
                                                  createReadBuffers(cBuffers, cSize));
        checkMultiBuffer(buf, cBuffers, cSize);
        buf.copyBytes(ofBegin, ofEnd, abDest, ofDest);
        checkByteArray(buf, ofBegin, ofEnd, abDest, ofDest);
        }

    /**
    * Test the behavior of MultiBufferReadBuffer(ReadBuffer[])
    * and getDestructiveBufferInput().
    *
    * @throws IOException
    */
    @Test
    public void copyBytesCompleteOneOver() throws IOException
        {
        int                   cBuffers = 256;
        int                   cSize    = 256;
        int                   ofBegin  = 0;
        int                   ofEnd    = 257;
        int                   ofDest   = 0;
        byte[]                abDest   = new byte[ofEnd - ofBegin + ofDest];

        MultiBufferReadBuffer buf      =  new MultiBufferReadBuffer(
                                                   createReadBuffers(cBuffers, cSize));
        checkMultiBuffer(buf, cBuffers, cSize);
        buf.copyBytes(ofBegin, ofEnd, abDest, ofDest);
        checkByteArray(buf, ofBegin, ofEnd, abDest, ofDest);
        }

    /**
    * Test the behavior of MultiBufferReadBuffer(ReadBuffer[])
    * and getDestructiveBufferInput().
    *
    * @throws IOException
    */
    @Test
    public void copyBytesCompleteBothOver() throws IOException
        {
        int                   cBuffers = 256;
        int                   cSize    = 256;
        int                   ofBegin  = 254;
        int                   ofEnd    = 511;
        int                   ofDest   = 0;
        byte[]                abDest   = new byte[ofEnd - ofBegin + ofDest];

        MultiBufferReadBuffer buf      =  new MultiBufferReadBuffer(
                                                    createReadBuffers(cBuffers, cSize));
        checkMultiBuffer(buf, cBuffers, cSize);
        buf.copyBytes(ofBegin, ofEnd, abDest, ofDest);
        checkByteArray(buf, ofBegin, ofEnd, abDest, ofDest);
        }

    /**
    * Test the behavior of MultiBufferReadBuffer(ReadBuffer[]) and
    * getDestructiveBufferInput().
    *
    * @throws IOException
    */
    @Test
    public void copyBytesCompleteStartOver() throws IOException
        {
        int    cBuffers = 256;
        int    cSize    = 256;
        int    ofBegin  = 255;
        int    ofEnd    = 510;
        int    ofDest   = 0;
        byte[] abDest   = new byte[ofEnd - ofBegin + ofDest];

        MultiBufferReadBuffer buf =
                new MultiBufferReadBuffer(createReadBuffers(cBuffers, cSize));
        checkMultiBuffer(buf, cBuffers, cSize);
        buf.copyBytes(ofBegin, ofEnd, abDest, ofDest);
        checkByteArray(buf, ofBegin, ofEnd, abDest, ofDest);
        }

    /**
    * Test the behavior of MultiBufferReadBuffer(ReadBuffer[]) and
    * getDestructiveBufferInput().
    *
    * @throws IOException
    */
    @Test
    public void copyBytesCompleteOverTwo() throws IOException
        {
        int    cBuffers = 256;
        int    cSize    = 256;
        int    ofBegin  = cSize * 4;
        int    ofEnd    = cSize * 6;
        int    ofDest   = 0;
        byte[] abDest   = new byte[ofEnd - ofBegin + ofDest];

        MultiBufferReadBuffer buf =
                new MultiBufferReadBuffer(createReadBuffers(cBuffers, cSize));
        checkMultiBuffer(buf, cBuffers, cSize);
        buf.copyBytes(ofBegin, ofEnd, abDest, ofDest);
        checkByteArray(buf, ofBegin, ofEnd, abDest, ofDest);
        }

    /**
    * Test the behaviour of the MultiBufferReadBuffer#equals implementation.
    */
    @Test
    public void testEquals()
        {
        // Note: use .equals() here instead of assertEquals()
        MultiBufferReadBuffer buf;

        // test 10x100bytes
        buf = new MultiBufferReadBuffer(createReadBuffers(10, 100));
        assertTrue(buf.equals(buf.toBinary()));
        assertTrue(buf.toBinary().equals(buf));
        assertTrue(buf.getReadBuffer(10, 500).equals(buf.toBinary(10, 500)));
        assertTrue(buf.toBinary(10, 500).equals(buf.getReadBuffer(10, 500)));

        // test empty case
        buf = new MultiBufferReadBuffer(new ReadBuffer[]{});
        assertTrue(buf.equals(buf.toBinary()));
        assertTrue(buf.toBinary().equals(buf));

        // test heterogenous case
        buf = new MultiBufferReadBuffer(new ReadBuffer[]
                    {
                    createAndWrap(10),
                    createAndWrap(20),
                    createAndWrap(30),
                    Binary.NO_BINARY,
                    createAndWrap(40),
                    });
        assertTrue(buf.equals(buf.toBinary()));
        assertTrue(buf.toBinary().equals(buf));
        assertTrue(buf.getReadBuffer(5, 50).equals(buf.toBinary(5, 50)));
        assertTrue(buf.toBinary(5, 50).equals(buf.getReadBuffer(5, 50)));
        assertTrue(buf.getReadBuffer(25, 20).equals(buf.toBinary(25, 20)));
        assertTrue(buf.toBinary(25, 20).equals(buf.getReadBuffer(25, 20)));

        // test nested case
        buf = new MultiBufferReadBuffer(new ReadBuffer[]
                    {
                    createAndWrap(10),
                    new MultiBufferReadBuffer(createReadBuffers(5, 4)),
                    createAndWrap(30),
                    Binary.NO_BINARY,
                    createAndWrap(40),
                    });
        assertTrue(buf.equals(buf.toBinary()));
        assertTrue(buf.toBinary().equals(buf));
        assertTrue(buf.getReadBuffer(5, 50).equals(buf.toBinary(5, 50)));
        assertTrue(buf.toBinary(5, 50).equals(buf.getReadBuffer(5, 50)));
        assertTrue(buf.getReadBuffer(25, 20).equals(buf.toBinary(25, 20)));
        assertTrue(buf.toBinary(25, 20).equals(buf.getReadBuffer(25, 20)));
        }

    /**
    * Test the writeTo() methods
    */
    @Test
    public void testWriteTo()
        {
        try
            {
            for (int i = 0; i < 3; i++)
                {
                MultiBufferReadBuffer buf;

                // test 10x100bytes
                buf = new MultiBufferReadBuffer(createReadBuffers(10, 100));
                testWriteToHelper(buf, i);
                testWriteToHelper(buf.getReadBuffer(5, 500), i);

                // test empty case
                buf = new MultiBufferReadBuffer(new ReadBuffer[]{});
                testWriteToHelper(buf, i);

                // test heterogenous case
                buf = new MultiBufferReadBuffer(new ReadBuffer[]
                    {
                    createAndWrap(10),
                    createAndWrap(20),
                    createAndWrap(30),
                    Binary.NO_BINARY,
                    createAndWrap(40),
                    });
                testWriteToHelper(buf, i);
                testWriteToHelper(buf.getReadBuffer(5, 50), i);
                testWriteToHelper(buf.getReadBuffer(25, 20), i);

                // test nested case
                buf = new MultiBufferReadBuffer(new ReadBuffer[]
                    {
                    createAndWrap(10),
                    new MultiBufferReadBuffer(createReadBuffers(5, 4)),
                    createAndWrap(30),
                    Binary.NO_BINARY,
                    createAndWrap(40),
                    });
                testWriteToHelper(buf, i);
                testWriteToHelper(buf.getReadBuffer(5, 50), i);
                testWriteToHelper(buf.getReadBuffer(25, 20), i);
                }
            }
        catch (IOException e)
            {
            throw ensureRuntimeException(e);
            }
        }

    /**
    * Helper method for testWriteTo()
    *
    * @param readBuf  the read buffer to write
    */
    private void testWriteToHelper(ReadBuffer readBuf, int nIter)
            throws IOException
        {
        OutputStream os;
        ByteBuffer   buf = ByteBuffer.allocate(readBuf.length());
        switch (nIter)
            {
            case 0:
                readBuf.writeTo(buf);
                break;
            case 1:
                os = new ByteBufferOutputStream(buf);
                readBuf.writeTo(os);
                os.close();
                break;
            case 2:
                os = new DataOutputStream(new ByteBufferOutputStream(buf));
                readBuf.writeTo((DataOutput) os);
                os.close();
                break;
            }
        Binary binComp = new Binary(buf.array(), 0, readBuf.length());
        assertTrue(readBuf.equals(binComp));
        assertTrue(binComp.equals(readBuf));
        }



    // ----------- helpers --------------------------------------------------

    /**
    *
    * @param cReadBuffers
    * @param cSize
    */
    private ReadBuffer[] createReadBuffers(int cReadBuffers, int cSize)
        {
        ReadBuffer[] buffers = new ReadBuffer[cReadBuffers];
        for (int i = 0; i < cReadBuffers; i++)
            {
            buffers[i] = createAndWrap(cSize);
            }
        return buffers;
        }

    /**
    *
    * @param cSize
    *
    * @return a ByteBufferReadBuffer with cSize elements
    */
    private ReadBuffer createAndWrap(int cSize)
        {
        return new ByteBufferReadBuffer(ByteBuffer.wrap(createByteArray(cSize)));
        }

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
    * @param buffer
    * @param cBuffers
    * @param cSize
    */
    private void checkMultiBuffer(MultiBufferReadBuffer buffer, int cBuffers,
                                  int cSize)
        {
        checkMultiBuffer(buffer, cBuffers, cSize, 0, cBuffers * cSize);
        }

    /**
    *
    * @param buffer
    * @param cBuffers
    * @param cSize
    * @param of
    * @param cb
    */
    private void checkMultiBuffer(MultiBufferReadBuffer buffer, int cBuffers,
                                  int cSize, int of, int cb)
        {
        byte[] abSrc = buffer.toByteArray(of, cb);

        if (cSize == 0)
            {
            return;
            }

        System.out.println("Comparing " + abSrc.length + " bytes.");
        for (int i = of / cSize; i < cb / cSize; i++)
            {
            for (int j = (i == of / cSize ? of % cSize : 0);
                    j < (i != of / cSize ? cSize - of : cb % cSize); j++)
                {
                assertTrue("abSrc[" + (i * cSize + j) + "] != "
                        + (byte) ((j + of) % 0xFF) + " was " + abSrc[(i * cSize + j)],
                        abSrc[(i * cSize + j)] == (byte) ((j + of) % 0xFF));
                }
            }
        }

    /**
    *
    * @param buffer
    * @param bIn
    * @throws IOException
    */
    private void checkBufferInput(MultiBufferReadBuffer buffer,
                                  BufferInput bIn)
        throws IOException
        {
        byte[] baSrc = buffer.toByteArray();
        for (int i = 0; i < baSrc.length; i++)
            {
            byte bSrc = (byte) bIn.read();
            assertTrue("baSrc[" + i + "] != " + bSrc + " was" + baSrc[i],
                    baSrc[i] == bSrc);
            }
        }

    /**
    *
    * @param buf
    * @param ofBegin
    * @param ofEnd
    * @param abDest
    * @param ofDest
    */
    private void checkByteArray(MultiBufferReadBuffer buf, int ofBegin,
                                int ofEnd, byte[] abDest, int ofDest)
        {
        byte[] abSrc = buf.toByteArray(ofBegin, ofEnd - ofBegin);
        for (int i = ofDest; i < abDest.length; i++)
            {
            assertTrue("abSrc[" + (i - ofDest) + "]: " + abSrc[(i - ofDest)]
                   + " != abDest[" + (i + ofDest) + "]: " + abDest[i + ofDest],
                    abSrc[i - ofDest] == abDest[i + ofDest]);
            }
        }

     /**
     *
     * @param buf
     * @param cSize
     * @param of
     */
     private void checkByteAt(MultiBufferReadBuffer buf, int cSize, int of)
         {
         assertTrue(buf.byteAt(of) == (byte) ((of % cSize) % 0xFF));
         }
    }

