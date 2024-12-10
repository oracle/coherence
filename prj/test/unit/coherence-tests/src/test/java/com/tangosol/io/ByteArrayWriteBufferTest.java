/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io;


import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import java.util.Arrays;

import static org.junit.Assert.*;


/**
* Unit tests for ByteArrayWriteBuffer.
*
* @author cp  2009.10.06
*/
public class ByteArrayWriteBufferTest
        extends ExternalizableHelper
    {
    // ----- constructor tests ----------------------------------------------

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#ByteArrayWriteBuffer(byte[])}.
    */
    @Test
    public void byteArrayConstructor()
        {
        byte[] abTest = new byte[10];
        byte[] abCmp  = new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(abTest);
        for (int i = 0; i < 10; ++i)
            {
            buf.write(i, (byte) i);
            }
        assertTrue(Arrays.equals(buf.toByteArray(), abCmp));
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#ByteArrayWriteBuffer(byte[])} when
    * the passed array is over-flowed.
    */
    @Test
    public void byteArrayConstructorOverflow()
        {
        byte[] abTest = new byte[10];
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(abTest);
        try
            {
            for (int i = 0; i < 11; ++i)
                {
                buf.write(i, (byte) i);
                }
            fail("expected exception");
            }
        catch (IndexOutOfBoundsException e)
            {
            // expected
            }
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#ByteArrayWriteBuffer(byte[])} when
    * the passed array is null.
    */
    @Test
    public void byteArrayConstructorNull()
        {
        try
            {
            new ByteArrayWriteBuffer(null);
            fail("expected exception");
            }
        catch (NullPointerException e)
            {
            // expected
            }
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#ByteArrayWriteBuffer(int)} when
    * the size is negative.
    */
    @Test
    public void initialSizeConstructorNegative()
        {
        try
            {
            new ByteArrayWriteBuffer(-1);
            fail("expected exception");
            }
        catch (IllegalArgumentException e)
            {
            // expected
            }
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#ByteArrayWriteBuffer(int)} when
    * the size is negative.
    */
    @Test
    public void initialSizeConstructorNegative2()
        {
        try
            {
            new ByteArrayWriteBuffer(Integer.MIN_VALUE);
            fail("expected exception");
            }
        catch (IllegalArgumentException e)
            {
            // expected
            }
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#ByteArrayWriteBuffer(int)}
    * when the buffer is over-flowed.
    */
    @Test
    public void initialSizeConstructorOverflow()
        {
        byte[] abCmp  = new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        for (int i = 0; i < 11; ++i)
            {
            buf.write(i, (byte) i);
            }
        assertTrue(Arrays.equals(buf.toByteArray(), abCmp));
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#ByteArrayWriteBuffer(int, int)}
    * when the size is negative.
    */
    @Test
    public void maxSizeConstructorNegative1()
        {
        try
            {
            new ByteArrayWriteBuffer(-1, 10);
            fail("expected exception");
            }
        catch (IllegalArgumentException e)
            {
            // expected
            }
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#ByteArrayWriteBuffer(int, int)}
    * when the max size is negative.
    */
    @Test
    public void maxSizeConstructorNegative2()
        {
        try
            {
            new ByteArrayWriteBuffer(10, -1);
            fail("expected exception");
            }
        catch (IllegalArgumentException e)
            {
            // expected
            }
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#ByteArrayWriteBuffer(int, int)}
    * when the size is larger than the max size.
    */
    @Test
    public void maxSizeConstructorTooSmall()
        {
        try
            {
            new ByteArrayWriteBuffer(10, 9);
            fail("expected exception");
            }
        catch (IllegalArgumentException e)
            {
            // expected
            }
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#ByteArrayWriteBuffer(int, int)}
    * when the size and max size are the same.
    */
    @Test
    public void maxSizeConstructorSameSize()
        {
        for (int i = 0; i < 1000; ++i)
            {
            new ByteArrayWriteBuffer(i, i);
            }
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#ByteArrayWriteBuffer(int, int)}
    * when the buffer is over-flowed.
    */
    @Test
    public void maxSizeConstructorOverflow()
        {
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10, 10);
        try
            {
            for (int i = 0; i < 11; ++i)
                {
                buf.write(i, (byte) i);
                }
            fail("expected exception");
            }
        catch (IndexOutOfBoundsException e)
            {
            // expected
            }
        }


    // ----- WriteBuffer method tests: write(byte) and (byte[]) -------------

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, byte)} when a byte
    * is written.
    */
    @Test
    public void bufWriteByte()
        {
        byte[] abCmp  = new byte[] {99};
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        buf.write(0, (byte) 99);
        assertTrue(Arrays.equals(buf.toByteArray(), abCmp));
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, byte)} when a byte
    * is written with an initial gap.
    */
    @Test
    public void bufWriteByteGap()
        {
        byte[] abCmp  = new byte[] {0, 0, 0, 0, 0, 99};
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        buf.write(5, (byte) 99);
        assertTrue(Arrays.equals(buf.toByteArray(), abCmp));
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, byte)} when multiple
    * bytes are written with gaps, forcing a resize.
    */
    @Test
    public void bufWriteByteResize()
        {
        byte[] abCmp  = new byte[] {0, 0, 0, 0, 0, 99, 0, 0, 0, 0, 0, 101};
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        buf.write(5, (byte) 99);
        buf.write(11, (byte) 101);
        assertTrue(Arrays.equals(buf.toByteArray(), abCmp));
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, byte)} when the bytes
    * are written in backwards order.
    */
    @Test
    public void bufWriteByteBackwards()
        {
        byte[] abCmp  = new byte[] {0, 0, 0, 0, 0, 99, 0, 0, 0, 0, 0, 101};
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        buf.write(11, (byte) 101);
        buf.write(5, (byte) 99);
        assertTrue(Arrays.equals(buf.toByteArray(), abCmp));
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, byte)} when the byte
    * written is zero.
    */
    @Test
    public void bufWriteByteZero()
        {
        byte[] abCmp  = new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        buf.write(11, (byte) 0);
        assertTrue(Arrays.equals(buf.toByteArray(), abCmp));
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, byte)} when the offset
    * is negative.
    */
    @Test
    public void bufWriteByteNegativeOffset()
        {
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        try
            {
            buf.write(-1, (byte) 0);
            fail("expected exception");
            }
        catch (IndexOutOfBoundsException e)
            {
            // expected
            }
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, byte[])} when the
    * byte array is null.
    */
    @Test
    public void bufWriteNullByteArray()
        {
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        try
            {
            buf.write(0, (byte[]) null);
            fail("expected exception");
            }
        catch (NullPointerException e)
            {
            // expected
            }
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, byte[])} when the
    * offset is negative.
    */
    @Test
    public void bufWriteByteArrayNegativeOffset()
        {
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        try
            {
            buf.write(-1, new byte[] {99, 101});
            fail("expected exception");
            }
        catch (IndexOutOfBoundsException e)
            {
            // expected
            }
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, byte[])} when a byte
    * array is written with an initial gap.
    */
    @Test
    public void bufWriteByteArrayGap()
        {
        byte[] abCmp  = new byte[] {0, 0, 0, 0, 0, 99, 101};
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        buf.write(5, new byte[] {99, 101});
        assertTrue(Arrays.equals(buf.toByteArray(), abCmp));
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, byte[])}
    * when a byte array is written with an offset/length that exceeds
    * max capacity.
    */
    @Test
    public void bufWriteByteArrayOverflowMaxLength()
        {
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10, 10);
        try
            {
            buf.write(7, new byte[] {33, 99, 101, 55});
            fail("expected exception");
            }
        catch (IndexOutOfBoundsException e)
            {
            // expected
            }
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, byte[])}
    * when a byte array is written with an offset/length that almost exceeds
    * max capacity.
    */
    @Test
    public void bufWriteByteArrayNoOverflowMaxLength()
        {
        byte[] abCmp  = new byte[] {0, 0, 0, 0, 0, 0, 33, 99, 101, 55};
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10, 10);
        buf.write(6, new byte[] {33, 99, 101, 55});
        assertTrue(Arrays.equals(buf.toByteArray(), abCmp));
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, byte[], int, int)} when
    * the byte array is null.
    */
    @Test
    public void bufWriteNullPartialByteArray()
        {
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        try
            {
            buf.write(0, (byte[]) null, 0, 0);
            fail("expected exception");
            }
        catch (NullPointerException e)
            {
            // expected
            }
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, byte[], int, int)} when
    * the byte array is null.
    */
    @Test
    public void bufWriteNullPartialByteArray2()
        {
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        try
            {
            buf.write(0, (byte[]) null, 0, 1);
            fail("expected exception");
            }
        catch (NullPointerException e)
            {
            // expected
            }
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, byte[], int, int)}
    * when a byte array is written with an initial gap.
    */
    @Test
    public void bufWritePartialByteArrayGap()
        {
        byte[] abCmp  = new byte[] {0, 0, 0, 0, 0, 99, 101};
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        buf.write(5, new byte[] {33, 99, 101, 55}, 1, 2);
        assertTrue(Arrays.equals(buf.toByteArray(), abCmp));
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, byte[], int, int)}
    * when a byte array is written with a negative offset.
    */
    @Test
    public void bufWritePartialByteArrayNegativeOffset()
        {
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        try
            {
            buf.write(-2, new byte[] {33, 99, 101, 55}, 1, 2);
            fail("expected exception");
            }
        catch (IndexOutOfBoundsException e)
            {
            // expected
            }
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, byte[], int, int)}
    * when a byte array is written with a negative offset into the partial
    * byte array.
    */
    @Test
    public void bufWritePartialByteArrayPartialNegativeOffset()
        {
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        try
            {
            buf.write(5, new byte[] {33, 99, 101, 55}, -2, 2);
            fail("expected exception");
            }
        catch (IndexOutOfBoundsException e)
            {
            // expected
            }
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, byte[], int, int)}
    * when a byte array is written with an overflowing offset into the
    * partial byte array.
    */
    @Test
    public void bufWritePartialByteArrayPartialOverflowOffset()
        {
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        try
            {
            buf.write(5, new byte[] {33, 99, 101, 55}, 5, 2);
            fail("expected exception");
            }
        catch (IndexOutOfBoundsException e)
            {
            // expected
            }
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, byte[], int, int)}
    * when a byte array is written with an overflowing length from the
    * partial byte array.
    */
    @Test
    public void bufWritePartialByteArrayPartialOverflowLength()
        {
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        try
            {
            buf.write(5, new byte[] {33, 99, 101, 55}, 1, 4);
            fail("expected exception");
            }
        catch (IndexOutOfBoundsException e)
            {
            // expected
            }
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, byte[], int, int)}
    * when a zero length byte array is written from the end of the partial
    * array.
    */
    @Test
    public void bufWritePartialByteArrayPartialNoLength()
        {
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        buf.write(5, new byte[] {33, 99, 101, 55}, 4, 0);
        assertTrue("Length was " + buf.toByteArray().length,
                buf.toByteArray().length == 0);
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, byte[], int, int)}
    * when a byte array is written with an offset/length that exceeds
    * max capacity.
    */
    @Test
    public void bufWritePartialByteArrayPartialOverflowMaxLength()
        {
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10, 10);
        try
            {
            buf.write(9, new byte[] {33, 99, 101, 55}, 1, 2);
            fail("expected exception");
            }
        catch (IndexOutOfBoundsException e)
            {
            // expected
            }
        }


    // ----- WriteBuffer method tests: write(ReadBuffer) --------------------

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, ReadBuffer)} when the
    * ReadBuffer is null.
    */
    @Test
    public void bufWriteNullReadBuffer()
        {
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        try
            {
            buf.write(0, (ReadBuffer) null);
            fail("expected exception");
            }
        catch (NullPointerException e)
            {
            // expected
            }
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, ReadBuffer)} when the
    * offset is negative.
    */
    @Test
    public void bufWriteReadBufferNegativeOffset()
        {
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        try
            {
            buf.write(-1, new Binary(new byte[] {99, 101}));
            fail("expected exception");
            }
        catch (IndexOutOfBoundsException e)
            {
            // expected
            }
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, ReadBuffer)} when a
    * buffer is written with an initial gap.
    */
    @Test
    public void bufWriteReadBufferGap()
        {
        byte[] abCmp  = new byte[] {0, 0, 0, 0, 0, 99, 101};
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        buf.write(5, new Binary(new byte[] {99, 101}));
        assertTrue(Arrays.equals(buf.toByteArray(), abCmp));
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, ReadBuffer)}
    * when a buffer is written with an offset/length that exceeds
    * max capacity.
    */
    @Test
    public void bufWriteReadBufferOverflowMaxLength()
        {
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10, 10);
        try
            {
            buf.write(7, new Binary(new byte[] {33, 99, 101, 55}));
            fail("expected exception");
            }
        catch (IndexOutOfBoundsException e)
            {
            // expected
            }
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, ReadBuffer)}
    * when a buffer is written with an offset/length that almost exceeds
    * max capacity.
    */
    @Test
    public void bufWriteReadBufferNoOverflowMaxLength()
        {
        byte[] abCmp  = new byte[] {0, 0, 0, 0, 0, 0, 33, 99, 101, 55};
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10, 10);
        buf.write(6, new Binary(new byte[] {33, 99, 101, 55}));
        assertTrue(Arrays.equals(buf.toByteArray(), abCmp));
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, ReadBuffer, int, int)}
    * when the buffer is null.
    */
    @Test
    public void bufWriteNullPartialReadBuffer()
        {
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        try
            {
            buf.write(0, (ReadBuffer) null, 0, 0);
            fail("expected exception");
            }
        catch (NullPointerException e)
            {
            // expected
            }
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, ReadBuffer, int, int)}
    * when the buffer is null.
    */
    @Test
    public void bufWriteNullPartialReadBuffer2()
        {
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        try
            {
            buf.write(0, (ReadBuffer) null, 0, 1);
            fail("expected exception");
            }
        catch (NullPointerException e)
            {
            // expected
            }
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, ReadBuffer, int, int)}
    * when a buffer is written with an initial gap.
    */
    @Test
    public void bufWritePartialReadBufferGap()
        {
        byte[] abCmp  = new byte[] {0, 0, 0, 0, 0, 99, 101};
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        buf.write(5, new Binary(new byte[] {33, 99, 101, 55}), 1, 2);
        assertTrue(Arrays.equals(buf.toByteArray(), abCmp));
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, ReadBuffer, int, int)}
    * when a buffer is written with a negative offset.
    */
    @Test
    public void bufWritePartialReadBufferNegativeOffset()
        {
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        try
            {
            buf.write(-2, new Binary(new byte[] {33, 99, 101, 55}), 1, 2);
            fail("expected exception");
            }
        catch (IndexOutOfBoundsException e)
            {
            // expected
            }
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, ReadBuffer, int, int)}
    * when a buffer is written with a negative offset into the partial
    * buffer.
    */
    @Test
    public void bufWritePartialReadBufferPartialNegativeOffset()
        {
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        try
            {
            buf.write(5, new Binary(new byte[] {33, 99, 101, 55}), -2, 2);
            fail("expected exception");
            }
        catch (IndexOutOfBoundsException e)
            {
            // expected
            }
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, ReadBuffer, int, int)}
    * when a buffer is written with an overflowing offset into the
    * partial buffer.
    */
    @Test
    public void bufWritePartialReadBufferPartialOverflowOffset()
        {
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        try
            {
            buf.write(5, new Binary(new byte[] {33, 99, 101, 55}), 5, 2);
            fail("expected exception");
            }
        catch (IndexOutOfBoundsException e)
            {
            // expected
            }
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, ReadBuffer, int, int)}
    * when a buffer is written with an overflowing length from the
    * partial buffer.
    */
    @Test
    public void bufWritePartialReadBufferPartialOverflowLength()
        {
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        try
            {
            buf.write(5, new Binary(new byte[] {33, 99, 101, 55}), 1, 4);
            fail("expected exception");
            }
        catch (IndexOutOfBoundsException e)
            {
            // expected
            }
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, ReadBuffer, int, int)}
    * when a zero length buffer is written from the end of the partial
    * buffer.
    */
    @Test
    public void bufWritePartialReadBufferPartialNoLength()
        {
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        buf.write(5, new Binary(new byte[] {33, 99, 101, 55}), 4, 0);
        assertTrue(buf.toByteArray().length == 5);
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, ReadBuffer, int, int)}
    * when a buffer is written with an offset/length that exceeds
    * max capacity.
    */
    @Test
    public void bufWritePartialReadBufferPartialOverflowMaxLength()
        {
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10, 10);
        try
            {
            buf.write(9, new Binary(new byte[] {33, 99, 101, 55}), 1, 2);
            fail("expected exception");
            }
        catch (IndexOutOfBoundsException e)
            {
            // expected
            }
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, ReadBuffer, int, int)}
    * when the buffer being written from is the same buffer as the one being
    * written to. Basically, this test ensures that the write where the
    * source and the dest are likely the same array is conducted with the
    * same guarantees as are found in {@link System#arraycopy}:
    * <blockquote>
    * If the <code>src</code> and <code>dest</code> arguments refer to the
    * same array object, then the copying is performed as if the
    * components at positions <code>srcPos</code> through
    * <code>srcPos+length-1</code> were first copied to a temporary
    * array with <code>length</code> components and then the contents of
    * the temporary array were copied into positions
    * <code>destPos</code> through <code>destPos+length-1</code> of the
    * destination array.
    * </blockquote>
    */
    @Test
    public void bufWritePartialThisReadBuffer()
        {
        byte[] abCmp = new byte[10];
        for (int i = 0, c = abCmp.length; i < c; ++i)
            {
            abCmp[i] = (byte) i;
            }

        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10, 10);
        buf.write(0, abCmp);

        System.arraycopy(abCmp, 0, abCmp, 5, 5);

        buf.write(5, buf.getUnsafeReadBuffer(), 0, 5);
        assertTrue(Arrays.equals(buf.toByteArray(), abCmp));
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, ReadBuffer, int, int)}
    * when the buffer being written from is the same buffer as the one being
    * written to. Basically, this test ensures that the write where the
    * source and the dest are likely the same array is conducted with the
    * same guarantees as are found in {@link System#arraycopy}:
    * <blockquote>
    * If the <code>src</code> and <code>dest</code> arguments refer to the
    * same array object, then the copying is performed as if the
    * components at positions <code>srcPos</code> through
    * <code>srcPos+length-1</code> were first copied to a temporary
    * array with <code>length</code> components and then the contents of
    * the temporary array were copied into positions
    * <code>destPos</code> through <code>destPos+length-1</code> of the
    * destination array.
    * </blockquote>
    */
    @Test
    public void bufWritePartialThisReadBuffer2()
        {
        byte[] abCmp = new byte[10];
        for (int i = 0, c = abCmp.length; i < c; ++i)
            {
            abCmp[i] = (byte) i;
            }

        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10, 10);
        buf.write(0, abCmp);

        System.arraycopy(abCmp, 5, abCmp, 0, 5);

        buf.write(0, buf.getUnsafeReadBuffer(), 5, 5);
        assertTrue(Arrays.equals(buf.toByteArray(), abCmp));
        }


    // ----- WriteBuffer method tests: write(InputStreaming) ----------------

    /**
    * Obtain test data as a byte array.
    *
    * @return an array of 10 bytes, with the values 0x0A to 0x01 in
    *         descending order
    */
    static byte[] getTestStreamData()
        {
        return new byte[] {10, 9, 8, 7, 6, 5, 4, 3, 2, 1};
        }

    /**
    * Obtain test data as a stream.
    *
    * @return a stream of 10 bytes, with the values 0x0A to 0x01 in
    *         descending order
    */
    static InputStreaming getTestStream()
        {
        return new WrapperInputStream(
                new ByteArrayInputStream(getTestStreamData()));
        }

    /**
    * Obtain test data as a stream that is backed by a Binary ReadBuffer.
    *
    * @return a stream of 10 bytes, with the values 0x0A to 0x01 in
    *         descending order
    */
    static InputStreaming getTestBufferStream()
        {
        return new Binary(getTestStreamData()).getBufferInput();
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, InputStreaming)} when
    * the stream is null.
    */
    @Test
    public void bufWriteNullInputStreaming()
            throws IOException
        {
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        try
            {
            buf.write(0, (InputStreaming) null);
            fail("expected exception");
            }
        catch (NullPointerException e)
            {
            // expected
            }
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, InputStreaming)} when
    * the offset is negative.
    */
    @Test
    public void bufWriteInputStreamingNegativeOffset()
            throws IOException
        {
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        try
            {
            buf.write(-1, getTestStream());
            fail("expected exception");
            }
        catch (IndexOutOfBoundsException e)
            {
            // expected
            }
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, InputStreaming)} when a
    * stream is written with an initial gap.
    */
    @Test
    public void bufWriteInputStreamingGap()
            throws IOException
        {
        byte[] abData = getTestStreamData();
        int    cbData = abData.length;
        byte[] abCmp  = new byte[cbData + 5];
        System.arraycopy(abData, 0, abCmp, 5, cbData);

        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        buf.write(5, getTestStream());
        assertTrue(Arrays.equals(buf.toByteArray(), abCmp));
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, InputStreaming)} when a
    * stream that comes from a buffer is written with an initial gap.
    */
    @Test
    public void bufWriteBufferInputStreamingGap2()
            throws IOException
        {
        byte[] abData = getTestStreamData();
        int    cbData = abData.length;
        byte[] abCmp  = new byte[cbData + 5];
        System.arraycopy(abData, 0, abCmp, 5, cbData);

        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        buf.write(5, getTestBufferStream());
        assertTrue(Arrays.equals(buf.toByteArray(), abCmp));
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, InputStreaming)}
    * when a stream is written with an offset/length that exceeds
    * max capacity.
    */
    @Test
    public void bufWriteInputStreamingOverflowMaxLength()
            throws IOException
        {
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10, 10);
        try
            {
            buf.write(1, getTestStream());
            fail("expected exception");
            }
        catch (IOException e)
            {
            // expected
            }
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, InputStreaming)}
    * when a stream is written with an offset/length that exceeds
    * max capacity.
    */
    @Test
    public void bufWriteBufferInputStreamingOverflowMaxLength2()
            throws IOException
        {
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10, 10);
        try
            {
            buf.write(1, getTestBufferStream());
            fail("expected exception");
            }
        catch (IOException e)
            {
            // expected
            }
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, InputStreaming)}
    * when a stream is written with an offset/length that almost exceeds
    * max capacity.
    */
    @Test
    public void bufWriteInputStreamingNoOverflowMaxLength3()
            throws IOException
        {
        byte[] abData = getTestStreamData();
        int    cbData = abData.length;
        int    cbCmp  = cbData + 2;
        byte[] abCmp  = new byte[cbCmp];
        System.arraycopy(abData, 0, abCmp, 2, cbData);

        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(cbCmp, cbCmp);
        buf.write(2, getTestStream());
        assertTrue(Arrays.equals(buf.toByteArray(), abCmp));
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, InputStreaming)}
    * when a stream is written with an offset/length that almost exceeds
    * max capacity.
    */
    @Test
    public void bufWriteBufferInputStreamingNoOverflowMaxLength4()
            throws IOException
        {
        byte[] abData = getTestStreamData();
        int    cbData = abData.length;
        int    cbCmp  = cbData + 2;
        byte[] abCmp  = new byte[cbCmp];
        System.arraycopy(abData, 0, abCmp, 2, cbData);

        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(cbCmp, cbCmp);
        buf.write(2, getTestBufferStream());
        assertTrue(Arrays.equals(buf.toByteArray(), abCmp));
        }


    // ----- WriteBuffer method tests: write(InputStreaming) ----------------

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, InputStreaming, int)}
    * when the stream is null.
    */
    @Test
    public void bufWriteNullSizedInputStreaming()
            throws IOException
        {
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        try
            {
            buf.write(0, (InputStreaming) null, 3);
            fail("expected exception");
            }
        catch (NullPointerException e)
            {
            // expected
            }
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, InputStreaming), int}
    * when the offset is negative.
    */
    @Test
    public void bufWriteSizedInputStreamingNegativeOffset()
            throws IOException
        {
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        try
            {
            buf.write(-1, getTestStream(), 3);
            fail("expected exception");
            }
        catch (IndexOutOfBoundsException e)
            {
            // expected
            }
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, InputStreaming), int}
    * when the size is negative.
    */
    @Test
    public void bufWriteSizedInputStreamingNegativeSize()
            throws IOException
        {
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        try
            {
            buf.write(1, getTestStream(), -3);
            fail("expected exception");
            }
        catch (IndexOutOfBoundsException e)
            {
            // expected
            }
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, InputStreaming)} when a
    * stream is written with an initial gap.
    */
    @Test
    public void bufWriteInputStreamingGap3()
            throws IOException
        {
        byte[] abData = getTestStreamData();
        int    cbData = abData.length;
        byte[] abCmp  = new byte[cbData + 5];
        System.arraycopy(abData, 0, abCmp, 5, cbData);

        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        buf.write(5, getTestStream());
        assertTrue(Arrays.equals(buf.toByteArray(), abCmp));
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, InputStreaming)} when a
    * stream that comes from a buffer is written with an initial gap.
    */
    @Test
    public void bufWriteBufferInputStreamingGap()
            throws IOException
        {
        byte[] abData = getTestStreamData();
        int    cbData = abData.length;
        byte[] abCmp  = new byte[cbData + 5];
        System.arraycopy(abData, 0, abCmp, 5, cbData);

        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        buf.write(5, getTestBufferStream());
        assertTrue(Arrays.equals(buf.toByteArray(), abCmp));
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, InputStreaming)}
    * when a stream is written with an offset/length that exceeds
    * max capacity.
    */
    @Test
    public void bufWriteInputStreamingOverflowMaxLength5()
            throws IOException
        {
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10, 10);
        try
            {
            buf.write(1, getTestStream());
            fail("expected exception");
            }
        catch (IOException e)
            {
            // expected
            }
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, InputStreaming)}
    * when a stream is written with an offset/length that exceeds
    * max capacity.
    */
    @Test
    public void bufWriteBufferInputStreamingOverflowMaxLength()
            throws IOException
        {
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10, 10);
        try
            {
            buf.write(1, getTestBufferStream());
            fail("expected exception");
            }
        catch (IOException e)
            {
            // expected
            }
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, InputStreaming)}
    * when a stream is written with an offset/length that almost exceeds
    * max capacity.
    */
    @Test
    public void bufWriteInputStreamingNoOverflowMaxLength()
            throws IOException
        {
        byte[] abData = getTestStreamData();
        int    cbData = abData.length;
        int    cbCmp  = cbData + 2;
        byte[] abCmp  = new byte[cbCmp];
        System.arraycopy(abData, 0, abCmp, 2, cbData);

        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(cbCmp, cbCmp);
        buf.write(2, getTestStream());
        assertTrue(Arrays.equals(buf.toByteArray(), abCmp));
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, InputStreaming)}
    * when a stream is written with an offset/length that almost exceeds
    * max capacity.
    */
    @Test
    public void bufWriteBufferInputStreamingNoOverflowMaxLength()
            throws IOException
        {
        byte[] abData = getTestStreamData();
        int    cbData = abData.length;
        int    cbCmp  = cbData + 2;
        byte[] abCmp  = new byte[cbCmp];
        System.arraycopy(abData, 0, abCmp, 2, cbData);

        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(cbCmp, cbCmp);
        buf.write(2, getTestBufferStream());
        assertTrue(Arrays.equals(buf.toByteArray(), abCmp));
        }



    // ----- length, capacity, max capacity tests ---------------------------

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#length()}, {@link
    * com.tangosol.io.ByteArrayWriteBuffer#getCapacity()} and {@link
    * com.tangosol.io.ByteArrayWriteBuffer#getMaximumCapacity()}
    * on a fixed capacity buffer as it is filled.
    */
    @Test
    public void bufLengthFixedCapacity()
            throws IOException
        {
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10, 10);
        for (int i = 0; i < 10; ++i)
            {
            buf.write(i, (byte) i);
            assertTrue(buf.length() == i + 1);
            assertTrue(buf.getCapacity() == 10);
            assertTrue(buf.getMaximumCapacity() == 10);
            }
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#length()}, {@link
    * com.tangosol.io.ByteArrayWriteBuffer#getCapacity()} and {@link
    * com.tangosol.io.ByteArrayWriteBuffer#getMaximumCapacity()}
    * on a fixed max capacity buffer with smaller initial capacity as it is
    * filled.
    */
    @Test
    public void bufLengthFixedMaxCapacity()
            throws IOException
        {
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(5, 10);
        for (int i = 0; i < 10; ++i)
            {
            buf.write(i, (byte) i);
            assertTrue(buf.length() == i + 1);
            assertTrue(buf.getCapacity() >= 5 && buf.getCapacity() > i && buf.getCapacity() <= 10);
            assertTrue(buf.getMaximumCapacity() == 10);
            }
        }


    // ----- retain & clear tests -------------------------------------------

    /**
    * Test the effect of {@link com.tangosol.io.ByteArrayWriteBuffer#clear()}
    * on the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#length()}, {@link
    * com.tangosol.io.ByteArrayWriteBuffer#getCapacity()} and {@link
    * com.tangosol.io.ByteArrayWriteBuffer#getMaximumCapacity()}
    * on a fixed max capacity buffer with the same initial capacity as it is
    * filled and then cleared.
    */
    @Test
    public void bufClearFixedCapacity()
            throws IOException
        {
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10, 10);
        for (int i = 0; i < 10; ++i)
            {
            buf.write(i, (byte) i);
            }
        buf.clear();
        assertTrue(buf.length() == 0);
        assertTrue(buf.getCapacity() >= 0);
        assertTrue(buf.getMaximumCapacity() == 10);
        }

    /**
    * Test the effect of {@link com.tangosol.io.ByteArrayWriteBuffer#clear()}
    * on the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#length()}, {@link
    * com.tangosol.io.ByteArrayWriteBuffer#getCapacity()} and {@link
    * com.tangosol.io.ByteArrayWriteBuffer#getMaximumCapacity()}
    * on a fixed max capacity buffer with smaller initial capacity as it is
    * filled and then cleared.
    */
    @Test
    public void bufClearFixedMaxCapacity()
            throws IOException
        {
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(5, 10);
        for (int i = 0; i < 10; ++i)
            {
            buf.write(i, (byte) i);
            }
        buf.clear();
        assertTrue(buf.length() == 0);
        assertTrue(buf.getCapacity() >= 0);
        assertTrue(buf.getMaximumCapacity() == 10);
        }

    /**
    * Test the effect of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#retain(int)} when the offset
    * is negative.
    */
    @Test
    public void bufRetainNegativeOffset()
            throws IOException
        {
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        buf.write(5, (byte) 1);
        try
            {
            buf.retain(-1);
            fail("expected exception");
            }
        catch (IndexOutOfBoundsException e)
            {
            // expected
            }
        }

    /**
    * Test the effect of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#retain(int)} when the offset
    * is larger than the length.
    */
    @Test
    public void bufRetainIllegalOffset()
            throws IOException
        {
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        buf.write(5, (byte) 1);
        try
            {
            buf.retain(7);
            fail("expected exception");
            }
        catch (IndexOutOfBoundsException e)
            {
            // expected
            }
        }

    /**
    * Test the effect of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#retain(int, int)} when the offset
    * is negative.
    */
    @Test
    public void bufRetain2NegativeOffset()
            throws IOException
        {
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        buf.write(5, (byte) 1);
        try
            {
            buf.retain(-1, 1);
            fail("expected exception");
            }
        catch (IndexOutOfBoundsException e)
            {
            // expected
            }
        }

    /**
    * Test the effect of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#retain(int, int)} when the offset
    * is larger than the length.
    */
    @Test
    public void bufRetain2IllegalOffset()
            throws IOException
        {
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        buf.write(5, (byte) 1);
        try
            {
            buf.retain(7, 1);
            fail("expected exception");
            }
        catch (IndexOutOfBoundsException e)
            {
            // expected
            }
        }

    /**
    * Test the effect of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#retain(int, int)} when the length
    * is negative.
    */
    @Test
    public void bufRetain2NegativeLength()
            throws IOException
        {
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        buf.write(5, (byte) 1);
        try
            {
            buf.retain(1, -1);
            fail("expected exception");
            }
        catch (IndexOutOfBoundsException e)
            {
            // expected
            }
        }

    /**
    * Test the effect of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#retain(int, int)} when the offset
    * plus length is larger than the length.
    */
    @Test
    public void bufRetain2IllegalLength()
            throws IOException
        {
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        buf.write(5, (byte) 1);
        try
            {
            buf.retain(5, 2);
            fail("expected exception");
            }
        catch (IndexOutOfBoundsException e)
            {
            // expected
            }
        }

    /**
    * Test the effect of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#retain(int)} when the offset
    * to retain is the length of the buffer.
    */
    @Test
    public void bufRetainAtEnd()
            throws IOException
        {
        byte[] abData = getTestStreamData();
        for (int i = 0; i < 10; ++i)
            {
            ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
            buf.write(0, abData, 0, i);
            buf.retain(i);
            assertTrue(buf.length() == 0);
            byte[] abClear = buf.toByteArray();
            assertTrue(abClear.length == 0);
            }
        }

    /**
    * Test the effect of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#retain(int, int)} when the length
    * to retain is zero.
    */
    @Test
    public void bufRetainNothing()
            throws IOException
        {
        byte[] abData = getTestStreamData();
        for (int i = 0; i < 10; ++i)
            {
            ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
            buf.write(0, abData);
            buf.retain(i, 0);
            assertTrue(buf.length() == 0);
            byte[] abClear = buf.toByteArray();
            assertTrue(abClear.length == 0);
            }
        }

    /**
    * Test the results of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#retain(int)} for correctness.
    */
    @Test
    public void bufRetainSomething()
            throws IOException
        {
        byte[] abCmp  = new byte[] {2, 3, 4, 5, 6, 7, 8, 9};
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        for (int i = 0; i < 10; ++i)
            {
            buf.write(i, (byte) i);
            }
        buf.retain(2);
        assertTrue(buf.length() == 8);
        assertTrue(Arrays.equals(buf.toByteArray(), abCmp));
        }

    /**
    * Test the results of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#retain(int, int)} for correctness.
    */
    @Test
    public void bufRetain2Something()
            throws IOException
        {
        byte[] abCmp  = new byte[] {2, 3, 4};
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        for (int i = 0; i < 10; ++i)
            {
            buf.write(i, (byte) i);
            }
        buf.retain(2, 3);
        assertTrue(buf.length() == 3);
        assertTrue(Arrays.equals(buf.toByteArray(), abCmp));
        }


    // ----- stream tests ---------------------------------------------------

    /**
    * Test the results of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#getBufferOutput()} when writing
    * one byte.
    */
    @Test
    public void outBufferOutputSimple()
            throws IOException
        {
        byte[] abCmp = new byte[] {99};

        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        ByteArrayWriteBuffer.BufferOutput out = buf.getBufferOutput();
        assertTrue(buf.length() == 0);
        assertTrue(out.getOffset() == 0);

        out.write((byte) 99);
        assertTrue(buf.length() == 1);
        assertTrue(out.getOffset() == 1);
        assertTrue(Arrays.equals(buf.toByteArray(), abCmp));
        }

    /**
    * Test the results of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#getBufferOutput()} when
    * over-writing one byte.
    */
    @Test
    public void outBufferOutputOverwrite()
            throws IOException
        {
        byte[] abCmp = new byte[] {99};

        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        buf.write(0, (byte) 33);

        ByteArrayWriteBuffer.BufferOutput out = buf.getBufferOutput();
        assertTrue(buf.length() == 1);
        assertTrue(out.getOffset() == 0);

        out.write((byte) 99);
        assertTrue(buf.length() == 1);
        assertTrue(out.getOffset() == 1);
        assertTrue(Arrays.equals(buf.toByteArray(), abCmp));
        }

    /**
    * Test the results of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#getBufferOutput(int)} when writing
    * one byte.
    */
    @Test
    public void outOffsetOutputSimple()
            throws IOException
        {
        byte[] abCmp = new byte[] {99};

        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        ByteArrayWriteBuffer.BufferOutput out = buf.getBufferOutput(0);
        assertTrue(buf.length() == 0);
        assertTrue(out.getOffset() == 0);

        out.write((byte) 99);
        assertTrue(buf.length() == 1);
        assertTrue(out.getOffset() == 1);
        assertTrue(Arrays.equals(buf.toByteArray(), abCmp));
        }

    /**
    * Test the results of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#getBufferOutput(int)} when
    * over-writing one byte.
    */
    @Test
    public void outOffsetOutputOverwrite()
            throws IOException
        {
        byte[] abCmp = new byte[] {99};

        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        buf.write(0, (byte) 33);

        ByteArrayWriteBuffer.BufferOutput out = buf.getBufferOutput(0);
        assertTrue(buf.length() == 1);
        assertTrue(out.getOffset() == 0);

        out.write((byte) 99);
        assertTrue(buf.length() == 1);
        assertTrue(out.getOffset() == 1);
        assertTrue(Arrays.equals(buf.toByteArray(), abCmp));
        }

    /**
    * Test the results of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#getBufferOutput(int)} when
    * appending one byte.
    */
    @Test
    public void outOffsetOutputAppend()
            throws IOException
        {
        byte[] abCmp = new byte[] {33, 99};

        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        buf.write(0, (byte) 33);

        ByteArrayWriteBuffer.BufferOutput out = buf.getBufferOutput(1);
        assertTrue(buf.length() == 1);
        assertTrue(out.getOffset() == 1);

        out.write((byte) 99);
        assertTrue(buf.length() == 2);
        assertTrue(out.getOffset() == 2);
        assertTrue(Arrays.equals(buf.toByteArray(), abCmp));
        }

    /**
    * Test the results of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#getBufferOutput(int)} when
    * overwriting two bytes and appending two bytes.
    */
    @Test
    public void outOffsetOutputOverwriteAndAppend()
            throws IOException
        {
        byte[] abCmp = new byte[] {10, 9, 8, 7, 6, 5, 4, 3,
                (byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE};

        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        buf.write(0, getTestStreamData());

        ByteArrayWriteBuffer.BufferOutput out = buf.getBufferOutput(8);
        assertTrue(buf.length() == 10);
        assertTrue(out.getOffset() == 8);

        out.writeInt(0xCAFEBABE);
        assertTrue(buf.length() == 12);
        assertTrue(out.getOffset() == 12);
        assertTrue(Arrays.equals(buf.toByteArray(), abCmp));
        }

    /**
    * Test the results of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#getBufferOutput(int)} when
    * overwriting two bytes and attempting to appending two bytes past the
    * maximum capacity.
    */
    @Test
    public void outOffsetOutputOverwriteAndAppendOverflow()
            throws IOException
        {
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10, 10);
        buf.write(0, getTestStreamData());

        ByteArrayWriteBuffer.BufferOutput out = buf.getBufferOutput(8);
        assertTrue(buf.length() == 10);
        assertTrue(out.getOffset() == 8);

        try
            {
            out.writeInt(0xCAFEBABE);
            fail("expected exception");
            }
        catch (IndexOutOfBoundsException e)
            {
            // expected
            }
        }

    /**
    * Test the results of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#getAppendingBufferOutput()} when
    * writing one byte.
    */
    @Test
    public void outAppendingOutputSimple()
            throws IOException
        {
        byte[] abCmp = new byte[] {99};

        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        ByteArrayWriteBuffer.BufferOutput out = buf.getAppendingBufferOutput();
        assertTrue(buf.length() == 0);
        assertTrue(out.getOffset() == 0);

        out.write((byte) 99);
        assertTrue(buf.length() == 1);
        assertTrue(out.getOffset() == 1);
        assertTrue(Arrays.equals(buf.toByteArray(), abCmp));
        }

    /**
    * Test the results of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#getAppendingBufferOutput()} when
    * appending one byte.
    */
    @Test
    public void outAppendingOutputAppend()
            throws IOException
        {
        byte[] abCmp = new byte[] {33, 99};

        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        buf.write(0, (byte) 33);

        ByteArrayWriteBuffer.BufferOutput out = buf.getAppendingBufferOutput();
        assertTrue(buf.length() == 1);
        assertTrue(out.getOffset() == 1);

        out.write((byte) 99);
        assertTrue(buf.length() == 2);
        assertTrue(out.getOffset() == 2);
        assertTrue(Arrays.equals(buf.toByteArray(), abCmp));
        }

    /**
    * Test the results of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#getAppendingBufferOutput()} when
    * appending one byte (and forcing a resize).
    */
    @Test
    public void outAppendingOutputAppend2()
            throws IOException
        {
        byte[] abCmp = new byte[] {10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 99};

        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        buf.write(0, getTestStreamData());

        ByteArrayWriteBuffer.BufferOutput out = buf.getAppendingBufferOutput();
        assertTrue(buf.length() == 10);
        assertTrue(out.getOffset() == 10);

        out.writeByte((byte) 99);
        assertTrue(buf.length() == 11);
        assertTrue(out.getOffset() == 11);
        assertTrue(Arrays.equals(buf.toByteArray(), abCmp));
        }

    /**
    * Test the results of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#getAppendingBufferOutput()} when
    * appending one byte (and forcing an overflow).
    */
    @Test
    public void outAppendingOutputAppendOverflow()
            throws IOException
        {
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10, 10);
        buf.write(0, getTestStreamData());

        ByteArrayWriteBuffer.BufferOutput out = buf.getAppendingBufferOutput();
        assertTrue(buf.length() == 10);
        assertTrue(out.getOffset() == 10);

        try
            {
            out.writeInt((byte) 99);
            fail("expected exception");
            }
        catch (IndexOutOfBoundsException e)
            {
            // expected
            }
        }

    /**
    * Verify that {@link
    * com.tangosol.io.ByteArrayWriteBuffer.BufferOutput#getBuffer()} refers
    * back to the creating buffer.
    */
    @Test
    public void outRefBuffer()
            throws IOException
        {
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10, 10);
        buf.write(0, getTestStreamData());

        assertTrue(buf.getBufferOutput().getBuffer() == buf);
        assertTrue(buf.getBufferOutput(5).getBuffer() == buf);
        assertTrue(buf.getAppendingBufferOutput().getBuffer() == buf);
        }

    /**
    * Test the results of various methods on {@link
    * com.tangosol.io.ByteArrayWriteBuffer.BufferOutput} to verify that the
    * correct bytes are written.
    */
    @Test
    public void outDataOutput()
            throws IOException
        {
        String sUnicode = "Hello World in \u042E\u043D\u0438\u043A\u043E\u0434!";

        ByteArrayOutputStream streamRaw  = new ByteArrayOutputStream();
        DataOutputStream      streamData = new DataOutputStream(streamRaw);
        streamData.writeBoolean(true);
        streamData.writeBoolean(false);
        streamData.writeByte(99);
        streamData.writeShort(-45);
        streamData.writeChar('h');
        streamData.writeInt(12345);
        streamData.writeLong(9876543210L);
        streamData.writeFloat(1.23f);
        streamData.writeDouble(4.567890123d);
        streamData.writeBytes(sUnicode);
        streamData.writeChars(sUnicode);
        streamData.writeUTF(sUnicode);
        writeSafeUTF(streamData, sUnicode);
        writeInt(streamData, Integer.MAX_VALUE);
        writeInt(streamData, 0);
        writeInt(streamData, 1);
        writeInt(streamData, -1);
        writeInt(streamData, Integer.MIN_VALUE);
        writeLong(streamData, Long.MAX_VALUE);
        writeLong(streamData, 0L);
        writeLong(streamData, 1L);
        writeLong(streamData, -1L);
        writeLong(streamData, Long.MIN_VALUE);

        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        ByteArrayWriteBuffer.BufferOutput out = buf.getAppendingBufferOutput();
        out.writeBoolean(true);
        out.writeBoolean(false);
        out.writeByte(99);
        out.writeShort(-45);
        out.writeChar('h');
        out.writeInt(12345);
        out.writeLong(9876543210L);
        out.writeFloat(1.23f);
        out.writeDouble(4.567890123d);
        out.writeBytes(sUnicode);
        out.writeChars(sUnicode);
        out.writeUTF(sUnicode);
        out.writeSafeUTF(sUnicode);
        out.writePackedInt(Integer.MAX_VALUE);
        out.writePackedInt(0);
        out.writePackedInt(1);
        out.writePackedInt(-1);
        out.writePackedInt(Integer.MIN_VALUE);
        out.writePackedLong(Long.MAX_VALUE);
        out.writePackedLong(0L);
        out.writePackedLong(1L);
        out.writePackedLong(-1L);
        out.writePackedLong(Long.MIN_VALUE);

        assertTrue(Arrays.equals(buf.toByteArray(), streamRaw.toByteArray()));
        }

    /**
    * @see ByteArrayWriteBuffer.ByteArrayBufferOutput#writePackedInt(int)
    */
    private static void assertPackedIntLength(int n, int cbActual)
        {
        if (n < 0)
            {
            n = ~n;
            }

        int cbExpected = n < 0x40 ? 1 : (39 - Integer.numberOfLeadingZeros(n)) / 7;
        assertTrue("cbActual=" + cbActual + ", cbExpected=" + cbExpected,
                    cbActual == cbExpected);
        }

    /**
    * @see ByteArrayWriteBuffer.ByteArrayBufferOutput#writePackedLong(long)
    */
    private static void assertPackedLongLength(long l, int cbActual)
        {
        if (l < 0)
            {
            l = ~l;
            }
        int cbExpected = l < 0x40 ? 1 : (71 - Long.numberOfLeadingZeros(l)) / 7;
        assertTrue("cbActual=" + cbActual + ", cbExpected=" + cbExpected,
                    cbActual == cbExpected);
        }

    /**
    * Test the behavior of
    * {@link ByteArrayWriteBuffer.ByteArrayBufferOutput#writePackedInt}.
    */
    @Test
    public void outWritePackedInt()
            throws IOException
        {
        // maximum of 5 bytes for a packed int:
        // byte 1: 6 data bits
        // byte 2: 7 data bits
        // byte 3: 7 data bits
        // byte 4: 7 data bits
        // byte 5: 5 data bits

        // test all 1-3 byte values
        int n = 0xFFFFF;
        for (int i = -n; i <= n; ++i)
            {
            ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(3);
            ByteArrayWriteBuffer.BufferOutput out = buf.getAppendingBufferOutput();
            out.writePackedInt(i);
            assertPackedIntLength(i, buf.length());
            }

        // test across all powers of 2
        for (int c = 0; c < 2; c++)
            {
            n = c == 0 ? 0xC0000000 : 0x40000000;
            for (int i = 0; i < 32; ++i)
                {
                int n1 = n - 1024;
                int n2 = n + 1024;
                for (int j = n1; j <= n2; j++)
                    {
                    ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(5);
                    ByteArrayWriteBuffer.BufferOutput out = buf.getAppendingBufferOutput();
                    out.writePackedInt(j);
                    assertPackedIntLength(j, buf.length());
                    }
                n >>= 1;
                }
            }
        }

    /**
    * Test the behavior of
    * {@link ByteArrayWriteBuffer.ByteArrayBufferOutput#writePackedLong}.
    */
    @Test
    public void outWritePackedLong()
            throws IOException
        {
        // maximum of 10 bytes for a packed long:
        // byte 1: 6 data bits
        // byte 2: 7 data bits
        // byte 3: 7 data bits
        // byte 4: 7 data bits
        // byte 5: 7 data bits
        // byte 6: 7 data bits
        // byte 7: 7 data bits
        // byte 8: 7 data bits
        // byte 9: 7 data bits
        // byte 10: 2 data bits

        // test all 1-3 byte values
        long n = 0xFFFFF;
        for (long i = -n; i <= n; ++i)
            {
            ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(3);
            ByteArrayWriteBuffer.BufferOutput out = buf.getAppendingBufferOutput();
            out.writePackedLong(i);
            assertPackedLongLength(i, buf.length());
            }

        // test across all powers of 2
        for (int c = 0; c < 2; c++)
            {
            n = c == 0 ? 0xC000000000000000L : 0x4000000000000000L;
            for (int i = 0; i < 64; ++i)
                {
                long n1 = n - 1024;
                long n2 = n + 1024;
                for (long j = n1; j <= n2; j++)
                    {
                    ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
                    ByteArrayWriteBuffer.BufferOutput out = buf.getAppendingBufferOutput();
                    out.writePackedLong(j);
                    assertPackedLongLength(j, buf.length());
                    }
                n >>= 1;
                }
            }
        }


    // ----- BufferOutput method tests for ReadBuffer -----------------------
    // TODO fix comments

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, ReadBuffer)} when the
    * ReadBuffer is null.
    */
    @Test
    public void outWriteNullReadBuffer()
            throws IOException
        {
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        ByteArrayWriteBuffer.BufferOutput out = buf.getAppendingBufferOutput();
        try
            {
            out.writeBuffer((ReadBuffer) null);
            fail("expected exception");
            }
        catch (NullPointerException e)
            {
            // expected
            }
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, ReadBuffer)} when a
    * buffer is written with an initial gap.
    */
    @Test
    public void outWriteReadBufferGap()
            throws IOException
        {
        byte[] abCmp  = new byte[] {0, 0, 0, 0, 0, 99, 101};
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        ByteArrayWriteBuffer.BufferOutput out = buf.getBufferOutput(5);
        out.writeBuffer(new Binary(new byte[] {99, 101}));
        assertTrue(out.getOffset() == 7);
        assertTrue(Arrays.equals(buf.toByteArray(), abCmp));
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, ReadBuffer)}
    * when a buffer is written with an offset/length that exceeds
    * max capacity.
    */
    @Test
    public void outWriteReadBufferOverflowMaxLength()
            throws IOException
        {
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10, 10);
        ByteArrayWriteBuffer.BufferOutput out = buf.getBufferOutput(7);
        try
            {
            out.writeBuffer(new Binary(new byte[] {33, 99, 101, 55}));
            fail("expected exception");
            }
        catch (IndexOutOfBoundsException e)
            {
            // expected
            }
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, ReadBuffer)}
    * when a buffer is written with an offset/length that almost exceeds
    * max capacity.
    */
    @Test
    public void outWriteReadBufferNoOverflowMaxLength()
            throws IOException
        {
        byte[] abCmp  = new byte[] {0, 0, 0, 0, 0, 0, 33, 99, 101, 55};
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10, 10);
        ByteArrayWriteBuffer.BufferOutput out = buf.getBufferOutput(6);
        out.writeBuffer(new Binary(new byte[] {33, 99, 101, 55}));
        assertTrue(out.getOffset() == 10);
        assertTrue(Arrays.equals(buf.toByteArray(), abCmp));
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, ReadBuffer, int, int)}
    * when the buffer is null.
    */
    @Test
    public void outWriteNullPartialReadBuffer()
            throws IOException
        {
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        ByteArrayWriteBuffer.BufferOutput out = buf.getBufferOutput();
        try
            {
            out.writeBuffer((ReadBuffer) null, 0, 0);
            fail("expected exception");
            }
        catch (NullPointerException e)
            {
            // expected
            }
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, ReadBuffer, int, int)}
    * when the buffer is null.
    */
    @Test
    public void outWriteNullPartialReadBuffer2()
            throws IOException
        {
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        ByteArrayWriteBuffer.BufferOutput out = buf.getBufferOutput();
        try
            {
            out.writeBuffer((ReadBuffer) null, 0, 1);
            fail("expected exception");
            }
        catch (NullPointerException e)
            {
            // expected
            }
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, ReadBuffer, int, int)}
    * when a buffer is written with an initial gap.
    */
    @Test
    public void outWritePartialReadBufferGap()
            throws IOException
        {
        byte[] abCmp  = new byte[] {0, 0, 0, 0, 0, 99, 101};
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        ByteArrayWriteBuffer.BufferOutput out = buf.getBufferOutput(5);
        out.writeBuffer(new Binary(new byte[] {33, 99, 101, 55}), 1, 2);
        assertTrue(out.getOffset() == 7);
        assertTrue(Arrays.equals(buf.toByteArray(), abCmp));
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, ReadBuffer, int, int)}
    * when a buffer is written with a negative offset into the partial
    * buffer.
    */
    @Test
    public void outWritePartialReadBufferPartialNegativeOffset()
            throws IOException
        {
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        ByteArrayWriteBuffer.BufferOutput out = buf.getBufferOutput(5);
        try
            {
            out.writeBuffer(new Binary(new byte[] {33, 99, 101, 55}), -2, 2);
            fail("expected exception");
            }
        catch (IndexOutOfBoundsException e)
            {
            // expected
            }
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, ReadBuffer, int, int)}
    * when a buffer is written with an overflowing offset into the
    * partial buffer.
    */
    @Test
    public void outWritePartialReadBufferPartialOverflowOffset()
            throws IOException
        {
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        ByteArrayWriteBuffer.BufferOutput out = buf.getBufferOutput(5);
        try
            {
            out.writeBuffer(new Binary(new byte[] {33, 99, 101, 55}), 5, 2);
            fail("expected exception");
            }
        catch (IndexOutOfBoundsException e)
            {
            // expected
            }
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, ReadBuffer, int, int)}
    * when a buffer is written with an overflowing length from the
    * partial buffer.
    */
    @Test
    public void outWritePartialReadBufferPartialOverflowLength()
            throws IOException
        {
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        ByteArrayWriteBuffer.BufferOutput out = buf.getBufferOutput(5);
        try
            {
            out.writeBuffer(new Binary(new byte[] {33, 99, 101, 55}), 1, 4);
            fail("expected exception");
            }
        catch (IndexOutOfBoundsException e)
            {
            // expected
            }
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, ReadBuffer, int, int)}
    * when a zero length buffer is written from the end of the partial
    * buffer.
    */
    @Test
    public void outWritePartialReadBufferPartialNoLength()
            throws IOException
        {
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        ByteArrayWriteBuffer.BufferOutput out = buf.getBufferOutput(5);
        out.writeBuffer(new Binary(new byte[] {33, 99, 101, 55}), 4, 0);
        assertTrue(out.getOffset() == 5);
        int cb = buf.toByteArray().length;
        assertTrue("Length " + cb, cb == 5);
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, ReadBuffer, int, int)}
    * when a buffer is written with an offset/length that exceeds
    * max capacity.
    */
    @Test
    public void outWritePartialReadBufferPartialOverflowMaxLength()
            throws IOException
        {
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10, 10);
        ByteArrayWriteBuffer.BufferOutput out = buf.getBufferOutput(9);
        try
            {
            out.writeBuffer(new Binary(new byte[] {33, 99, 101, 55}), 1, 2);
            fail("expected exception");
            }
        catch (IndexOutOfBoundsException e)
            {
            // expected
            }
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, ReadBuffer, int, int)}
    * when the buffer being written from is the same buffer as the one being
    * written to. Basically, this test ensures that the write where the
    * source and the dest are likely the same array is conducted with the
    * same guarantees as are found in {@link System#arraycopy}:
    * <blockquote>
    * If the <code>src</code> and <code>dest</code> arguments refer to the
    * same array object, then the copying is performed as if the
    * components at positions <code>srcPos</code> through
    * <code>srcPos+length-1</code> were first copied to a temporary
    * array with <code>length</code> components and then the contents of
    * the temporary array were copied into positions
    * <code>destPos</code> through <code>destPos+length-1</code> of the
    * destination array.
    * </blockquote>
    */
    @Test
    public void outWritePartialThisReadBuffer()
            throws IOException
        {
        byte[] abCmp = new byte[10];
        for (int i = 0, c = abCmp.length; i < c; ++i)
            {
            abCmp[i] = (byte) i;
            }

        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10, 10);
        buf.write(0, abCmp);

        System.arraycopy(abCmp, 0, abCmp, 5, 5);

        ByteArrayWriteBuffer.BufferOutput out = buf.getBufferOutput(5);
        out.writeBuffer(buf.getUnsafeReadBuffer(), 0, 5);
        assertTrue(out.getOffset() == 10);
        assertTrue(Arrays.equals(buf.toByteArray(), abCmp));
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, ReadBuffer, int, int)}
    * when the buffer being written from is the same buffer as the one being
    * written to. Basically, this test ensures that the write where the
    * source and the dest are likely the same array is conducted with the
    * same guarantees as are found in {@link System#arraycopy}:
    * <blockquote>
    * If the <code>src</code> and <code>dest</code> arguments refer to the
    * same array object, then the copying is performed as if the
    * components at positions <code>srcPos</code> through
    * <code>srcPos+length-1</code> were first copied to a temporary
    * array with <code>length</code> components and then the contents of
    * the temporary array were copied into positions
    * <code>destPos</code> through <code>destPos+length-1</code> of the
    * destination array.
    * </blockquote>
    */
    @Test
    public void outWritePartialThisReadBuffer2()
            throws IOException
        {
        byte[] abCmp = new byte[10];
        for (int i = 0, c = abCmp.length; i < c; ++i)
            {
            abCmp[i] = (byte) i;
            }

        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10, 10);
        buf.write(0, abCmp);

        System.arraycopy(abCmp, 5, abCmp, 0, 5);

        ByteArrayWriteBuffer.BufferOutput out = buf.getBufferOutput();
        out.writeBuffer(buf.getUnsafeReadBuffer(), 5, 5);
        assertTrue(out.getOffset() == 5);
        assertTrue(Arrays.equals(buf.toByteArray(), abCmp));
        }


    // ----- BufferOutput method tests for InputStreaming -------------------
    // TODO fix comments

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, InputStreaming)} when
    * the stream is null.
    */
    @Test
    public void outWriteNullInputStreaming()
            throws IOException
        {
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        ByteArrayWriteBuffer.BufferOutput out = buf.getBufferOutput();
        try
            {
            out.writeStream((InputStreaming) null);
            fail("expected exception");
            }
        catch (NullPointerException e)
            {
            // expected
            }
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, InputStreaming)} when a
    * stream is written with an initial gap.
    */
    @Test
    public void outWriteInputStreamingGap()
            throws IOException
        {
        byte[] abData = getTestStreamData();
        int    cbData = abData.length;
        int    cbCmp  = cbData + 5;
        byte[] abCmp  = new byte[cbCmp];
        System.arraycopy(abData, 0, abCmp, 5, cbData);

        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        ByteArrayWriteBuffer.BufferOutput out = buf.getBufferOutput(5);
        out.writeStream(getTestStream());
        assertTrue(out.getOffset() == cbCmp);
        assertTrue(Arrays.equals(buf.toByteArray(), abCmp));
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, InputStreaming)} when a
    * stream that comes from a buffer is written with an initial gap.
    */
    @Test
    public void outWriteBufferInputStreamingGap()
            throws IOException
        {
        byte[] abData = getTestStreamData();
        int    cbData = abData.length;
        int    cbCmp  = cbData + 5;
        byte[] abCmp  = new byte[cbCmp];
        System.arraycopy(abData, 0, abCmp, 5, cbData);

        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        ByteArrayWriteBuffer.BufferOutput out = buf.getBufferOutput(5);
        out.writeStream(getTestBufferStream());
        assertTrue(out.getOffset() == cbCmp);
        assertTrue(Arrays.equals(buf.toByteArray(), abCmp));
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, InputStreaming)}
    * when a stream is written with an offset/length that exceeds
    * max capacity.
    */
    @Test
    public void outWriteInputStreamingOverflowMaxLength()
            throws IOException
        {
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10, 10);
        ByteArrayWriteBuffer.BufferOutput out = buf.getBufferOutput(1);
        try
            {
            out.writeStream(getTestStream());
            fail("expected exception");
            }
        catch (IOException e)
            {
            // expected
            }
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, InputStreaming)}
    * when a stream is written with an offset/length that exceeds
    * max capacity.
    */
    @Test
    public void outWriteBufferInputStreamingOverflowMaxLength()
            throws IOException
        {
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10, 10);
        ByteArrayWriteBuffer.BufferOutput out = buf.getBufferOutput(1);
        try
            {
            out.writeStream(getTestBufferStream());
            fail("expected exception");
            }
        catch (IOException e)
            {
            // expected
            }
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, InputStreaming)}
    * when a stream is written with an offset/length that almost exceeds
    * max capacity.
    */
    @Test
    public void outWriteInputStreamingNoOverflowMaxLength()
            throws IOException
        {
        byte[] abData = getTestStreamData();
        int    cbData = abData.length;
        int    cbCmp  = cbData + 2;
        byte[] abCmp  = new byte[cbCmp];
        System.arraycopy(abData, 0, abCmp, 2, cbData);

        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(cbCmp, cbCmp);
        ByteArrayWriteBuffer.BufferOutput out = buf.getBufferOutput(2);
        out.writeStream(getTestStream());
        assertTrue(out.getOffset() == cbCmp);
        assertTrue(Arrays.equals(buf.toByteArray(), abCmp));
        }

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#write(int, InputStreaming)}
    * when a stream is written with an offset/length that almost exceeds
    * max capacity.
    */
    @Test
    public void outWriteBufferInputStreamingNoOverflowMaxLength()
            throws IOException
        {
        byte[] abData = getTestStreamData();
        int    cbData = abData.length;
        int    cbCmp  = cbData + 2;
        byte[] abCmp  = new byte[cbCmp];
        System.arraycopy(abData, 0, abCmp, 2, cbData);

        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(cbCmp, cbCmp);
        ByteArrayWriteBuffer.BufferOutput out = buf.getBufferOutput(2);
        out.writeStream(getTestBufferStream());
        assertTrue(out.getOffset() == cbCmp);
        assertTrue(Arrays.equals(buf.toByteArray(), abCmp));
        }


    // ----- getReadBuffer, getUnsafeReadBuffer, toBinary -------------------

    /**
    * Test the results of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#getReadBuffer()} for correctness.
    */
    @Test
    public void bufReadBuffer()
            throws IOException
        {
        byte[] abCmp = getTestStreamData();

        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        buf.write(0, abCmp);

        ReadBuffer bufRead = buf.getReadBuffer();
        assertTrue(bufRead.length() == abCmp.length);
        assertTrue(Arrays.equals(bufRead.toByteArray(), abCmp));

        // verify no changes to read buffer when the write buffer is changed
        buf.write(0, (byte) 99);
        assertTrue(bufRead.length() == abCmp.length);
        assertTrue(Arrays.equals(bufRead.toByteArray(), abCmp));
        }

    /**
    * Test the results of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#getUnsafeReadBuffer()} for
    * correctness.
    */
    @Test
    public void bufUnsafeReadBuffer()
            throws IOException
        {
        byte[] abCmp = getTestStreamData();

        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        buf.write(0, abCmp);

        ReadBuffer bufRead = buf.getUnsafeReadBuffer();
        assertTrue(bufRead.length() == abCmp.length);
        assertTrue(Arrays.equals(bufRead.toByteArray(), abCmp));
        }

    /**
    * Test the results of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#getUnsafeReadBuffer()} for
    * non-copying optimization.
    */
    @Test
    public void bufUnsafeReadBufferByteArray()
            throws IOException
        {
        byte[] abOrig = getTestStreamData();
        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(abOrig);
        buf.write(0, abOrig);

        ReadBuffer bufRead = buf.getUnsafeReadBuffer();
        assertTrue(bufRead.length() == abOrig.length);
        assertTrue(bufRead.toByteArray() == abOrig);
        }

    /**
    * Test the results of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#toBinary()} for correctness.
    */
    @Test
    public void bufToBinary()
            throws IOException
        {
        byte[] abCmp = getTestStreamData();

        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(10);
        buf.write(0, abCmp);

        ReadBuffer bufRead = buf.toBinary();
        assertTrue(bufRead.length() == abCmp.length);
        assertTrue(Arrays.equals(bufRead.toByteArray(), abCmp));

        // verify no changes to read buffer when the write buffer is changed
        buf.write(0, (byte) 99);
        assertTrue(bufRead.length() == abCmp.length);
        assertTrue(Arrays.equals(bufRead.toByteArray(), abCmp));
        }


    // ----- internal ByteArrayWriteBuffer ----------------------------------

    /**
    * Test the behavior of {@link
    * com.tangosol.io.ByteArrayWriteBuffer#isByteArrayPrivate()}, {@link
    * com.tangosol.io.ByteArrayWriteBuffer#makeByteArrayPrivate()} and {@link
    * com.tangosol.io.ByteArrayWriteBuffer#getRawByteArray()} for
    * correctness.
    */
    @Test
    public void bufPrivateByteArray()
            throws IOException
        {
        byte[] abCmp = new byte[getTestStreamData().length];

        ByteArrayWriteBuffer buf = new ByteArrayWriteBuffer(abCmp);
        buf.write(0, getTestStreamData());

        assertFalse(buf.isByteArrayPrivate());
        assertTrue(buf.getRawByteArray() == abCmp);
        assertTrue(buf.getUnsafeReadBuffer().toByteArray() == abCmp);
        assertTrue(((ByteArrayReadBuffer) buf.getUnsafeReadBuffer()).getRawByteArray() == abCmp);

        buf.makeByteArrayPrivate();

        assertTrue(buf.isByteArrayPrivate());
        assertTrue(buf.getRawByteArray() != abCmp);
        assertTrue(buf.getUnsafeReadBuffer().toByteArray() != abCmp);
        assertTrue(((ByteArrayReadBuffer) buf.getUnsafeReadBuffer()).getRawByteArray() != abCmp);
        }


    // ----- dependent and independent WriteBuffer tests --------------------

    // TODO WriteBuffer getWriteBuffer(int of);
    // TODO WriteBuffer getWriteBuffer(int of, int cb);
    // TODO Object clone();
    // TODO remember to test effect of writes to sub write buffer on the parent and vice versa
    // TODO opposite for clone()
    }
