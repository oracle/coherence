/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io;

import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;

import java.io.EOFException;
import java.io.IOException;
import java.io.UTFDataFormatException;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;

/**
* ByteArrayWriteBuffer is an implementation of WriteBuffer on a byte array.
* It is designed to support both fixed length buffers and resizable buffers.
* <p>
* This implementation is not intended to be thread safe.
*
* @author cp  2005.03.24
*/
public class ByteArrayWriteBuffer
        extends AbstractWriteBuffer
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor; intended only for use by subclasses.
    * <p>
    * Note that this default constructor leaves the buffer in an invalid
    * state.
    */
    protected ByteArrayWriteBuffer()
        {
        }

    /**
    * Construct a ByteArrayWriteBuffer on a byte array.
    *
    * @param ab  a byte array
    *
    * @exception NullPointerException if <tt>ab</tt> is <tt>null</tt>
    */
    public ByteArrayWriteBuffer(byte[] ab)
        {
        m_ab    = ab;
        m_cbMax = ab.length;
        }

    /**
    * Construct an ByteArrayWriteBuffer with a certain initial capacity.
    *
    * @param cbCap  initial capacity
    *
    * @exception IllegalArgumentException if <tt>cbCap</tt> is negative
    */
    public ByteArrayWriteBuffer(int cbCap)
        {
        this(cbCap, Integer.MAX_VALUE);
        }

    /**
    * Construct an ByteArrayWriteBuffer with a certain initial capacity and
    * a certain maximum capacity.
    *
    * @param cbCap  initial capacity
    * @param cbMax  maximum capacity
    *
    * @exception IllegalArgumentException if <tt>cbCap</tt> or <tt>cbMax</tt>
    *            is negative, or if <tt>cbCap</tt> is greater than
    *            <tt>cbMax</tt>
    */
    public ByteArrayWriteBuffer(int cbCap, int cbMax)
        {
        if (cbCap < 0 || cbMax < 0 || cbCap > cbMax)
            {
            throw new IllegalArgumentException("cap=" + cbCap + "; max=" + cbMax);
            }

        m_ab    = createBytes(cbCap);
        m_cbMax = cbMax;
        }

    /**
    * Create a new ByteArrayWriteBuffer based on a region of an already
    * existing WriteBuffer.
    *
    * @param buffer  the source buffer
    * @param i       the offset within the source buffer
    * @param cb      the number of bytes to copy
    */
    public ByteArrayWriteBuffer(WriteBuffer buffer, int i, int cb)
        {
        m_cbMax = cb - i;
        m_ab    = createBytes(m_cbMax);

        write(0, buffer.getUnsafeReadBuffer(), i, cb);
        }


    // ----- buffer write operations ----------------------------------------

    /**
    * {@inheritDoc}
    */
    public final void write(int ofDest, byte b)
        {
        checkBounds(ofDest, 1);
        m_ab[ofDest] = b;
        updateLength(ofDest + 1);
        }

    /**
    * {@inheritDoc}
    */
    public final void write(int ofDest, byte[] abSrc, int ofSrc, int cbSrc)
        {
        checkBounds(ofDest, cbSrc);

        // it's necessary to call this (even if cbSrc==0) in order to
        // correctly validate the arguments
        System.arraycopy(abSrc, ofSrc, m_ab, ofDest, cbSrc);

        if (cbSrc > 0)
            {
            updateLength(ofDest + cbSrc);
            }
        }

    /**
    * {@inheritDoc}
    */
    public final void write(int ofDest, ReadBuffer bufSrc, int ofSrc, int cbSrc)
        {
        checkBounds(ofDest, cbSrc);

        // it's necessary to call this (even if cbSrc==0) in order to
        // correctly validate the arguments
        bufSrc.copyBytes(ofSrc, ofSrc + cbSrc, m_ab, ofDest);
        updateLength(ofDest + cbSrc);
        }

    /**
    * {@inheritDoc}
    */
    public final void write(int ofDest, InputStreaming stream, int cbSrc)
            throws IOException
        {
        // see if it is a known implementation that we can optimize for
        if (stream instanceof ReadBuffer.BufferInput)
            {
            copyBufferInputPortion(ofDest, (ReadBuffer.BufferInput) stream, cbSrc);
            return;
            }

        // read the stream straight into the underlying byte[]
        checkBounds(ofDest, cbSrc);
        int cbRead = 0;
        try
            {
            while (cbRead < cbSrc)
                {
                int cbActual = stream.read(m_ab, ofDest + cbRead, cbSrc - cbRead);
                if (cbActual < 0)
                    {
                    throw new EOFException("instructed to copy " + cbSrc
                            + " bytes, but only " + cbRead + " were available");
                    }
                else
                    {
                    cbRead += cbActual;
                    }
                }
            }
        finally
            {
            if (cbRead > 0)
                {
                updateLength(ofDest + cbRead);
                }
            }
        }


    // ----- buffer maintenance ----------------------------------------------

    /**
    * {@inheritDoc}
    */
    public final int length()
        {
        return m_cb;
        }

    /**
    * Reconfigure the length of the buffer. The length must not be longer than
    * the available capacity.
    *
    * @param cb the new length of the buffer
    */
    public final void setLength(int cb)
        {
        assert cb <= m_cbMax;
        updateLength(cb);
        }

    /**
    * {@inheritDoc}
    */
    public final void retain(int of, int cb)
        {
        if (of < 0 || cb < 0 || of + cb > m_cb)
            {
            throw new IndexOutOfBoundsException("of=" + of + ", cb="
                    + cb + ", length()=" + m_cb);
            }

        if (of > 0 && cb > 0)
            {
            byte[] ab = m_ab;
            System.arraycopy(ab, of, ab, 0, cb);
            }

        m_cb        = cb;
        m_bufUnsafe = null;
        }

    /**
    * {@inheritDoc}
    */
    public final int getCapacity()
        {
        return m_ab.length;
        }

    /**
    * {@inheritDoc}
    */
    public final int getMaximumCapacity()
        {
        return m_cbMax;
        }


    // ----- obtaining different "write views" to the buffer ----------------

    /**
    * {@inheritDoc}
    */
    public final BufferOutput getBufferOutput(int of)
        {
        return new ByteArrayBufferOutput(of);
        }


    // ----- accessing the buffered data ------------------------------------

    /**
    * {@inheritDoc}
    */
    public final ReadBuffer getUnsafeReadBuffer()
        {
        ByteArrayReadBuffer buf = m_bufUnsafe;
        if (buf == null)
            {
            m_bufUnsafe = buf = new ByteArrayReadBuffer(m_ab, 0, m_cb,
                    false, isByteArrayPrivate(), false);
            }
        else
            {
            buf.updateLength(m_cb);
            }
        return buf;
        }

    /**
    * {@inheritDoc}
    *
    * For efficiency purposes, it is possible to obtain the internal byte
    * array that the ByteArrayWriteBuffer is using by calling {@link
    * #getRawByteArray()}; if the internal byte array is private (i.e. if it
    * cannot be exposed to the caller), then the result will be the same as
    * would be returned by toByteArray().
    */
    public final byte[] toByteArray()
        {
        int cb = m_cb;
        if (cb == 0)
            {
            return NO_BYTES;
            }

        byte[] ab = new byte[cb];
        System.arraycopy(m_ab, 0, ab, 0, cb);
        return ab;
        }

    /**
    * {@inheritDoc}
    */
    public Binary toBinary()
        {
        int cb = m_cb;
        if (cb == 0)
            {
            return NO_BINARY;
            }

        return new Binary(m_ab, 0, cb);
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Determine if the underlying byte[] should be treated as private data.
    *
    * @return true iff the underlying data should not ever be exposed by
    *         this object
    */
    public boolean isByteArrayPrivate()
        {
        return m_fPrivate;
        }

    /**
    * Make sure that the underlying byte[] will be treated as private data.
    */
    public final void makeByteArrayPrivate()
        {
        m_fPrivate  = true;
        m_bufUnsafe = null;
        }

    /**
    * Obtain the byte array that this WriteBuffer uses. If the underlying
    * byte array is private, then this method will always return a copy of
    * the portion of the byte array that this WriteBuffer represents as if
    * the called had called {@link #toByteArray()}.
    *
    * @return the byte array that this WriteBuffer uses
    */
    public final byte[] getRawByteArray()
        {
        return isByteArrayPrivate() ? toByteArray() : m_ab;
        }


    // ----- internal -------------------------------------------------------

    /**
    * {@inheritDoc}
    */
    protected final int copyStream(int ofDest, InputStreaming stream, int cbMax)
            throws IOException
        {
        // see if it is a known implementation that we can optimize for
        if (stream instanceof ReadBuffer.BufferInput)
            {
            return copyBufferInputRemainder(ofDest, (ReadBuffer.BufferInput) stream, cbMax);
            }

        int ofOrig   = ofDest;
        int cbRemain = cbMax;
        while (true)
            {
            // while there is additional capacity in the buffer, read
            byte[] ab    = m_ab;
            int    cbCap = Math.min(ab.length, ofDest + cbRemain);
            while (ofDest < cbCap)
                {
                int cbActual;
                try
                    {
                    cbActual = stream.read(ab, ofDest, cbCap - ofDest);
                    }
                catch (EOFException e)
                    {
                    cbActual = -1;
                    }

                if (cbActual < 0)
                    {
                    updateLength(ofDest);
                    return ofDest - ofOrig;
                    }
                else
                    {
                    ofDest   += cbActual;
                    cbRemain -= cbActual;
                    }
                }

            if (cbRemain > 0)
                {
                // when out of room, grow
                updateLength(ofDest);
                grow(ofDest);
                }

            // once we reach max cap, just read one byte to prove overflow
            if (ofDest >= m_ab.length || cbRemain == 0)
                {
                if (stream.read() < 0)
                    {
                    // filled the capacity perfectly; no more data to read
                    updateLength(ofDest);
                    return ofDest - ofOrig;
                    }
                else
                    {
                    throw new IOException("Overflow: write buffer limited to "
                            + cbMax + " bytes, but input stream is not exhausted");
                    }
                }
            }
        }

    /**
     * Create a byte array of the specified size. The main reason to make this
     * into a separate method is a fact the native OOME comes without any stack trace.
     *
     * @param cb  the specified size
     *
     * @return a byte array
     */
    protected static byte[] createBytes(int cb)
        {
        try
            {
            return new byte[cb];
            }
        catch (OutOfMemoryError e)
            {
            if (cb == Integer.MAX_VALUE)
                {
                throw new UnsupportedOperationException(
                    "buffer has reached its max capacity of 2GB");
                }
            throw new OutOfMemoryError(
                "Failed to allocate a byte array of the requested size: " + cb);
            }
        }

    /**
    * Validate the ranges for the passed bounds and make sure that the
    * underlying array is big enough to handle them.
    *
    * @param of  the offset that data is about to be written to
    * @param cb  the length of the data that is about to be written
    */
    protected void checkBounds(int of, int cb)
        {
        int cbTotal = of + cb;
        if (of < 0 || cb < 0 || cbTotal > m_cbMax || cbTotal < 0)
            {
            boundsException(of, cb);
            }

        if (cbTotal > m_ab.length)
            {
            grow(cbTotal);
            }
        }

    /**
    * Raise an exception for the offset/length being out of bounds. This
    * code was moved out of checkBounds in order to encourage more aggressive
    * in-lining by the HotSpot JVM.
    *
    * @param of  the current offset
    * @param cb  the current length
    *
    * @throws IndexOutOfBoundsException always
    */
    private void boundsException(int of, int cb)
        throws IndexOutOfBoundsException
        {
        if ((long) of + (long) cb > Integer.MAX_VALUE)
            {
            throw new UnsupportedOperationException(
                "buffer has reached its max capacity of 2GB");
            }
        throw new IndexOutOfBoundsException("of=" + of + ", cb="
                + cb + ", max=" + m_cbMax);
        }

    /**
    * Grow the underlying byte array to at least the specified size.
    *
    * @param cbCap  the required or requested capacity
    */
    protected final void grow(int cbCap)
        {
        // desired growth is 100% for "small" buffers and 50% for "huge"
        // minimum growth is 1KB
        byte[] abOld  = m_ab;
        int    cbOld  = abOld.length;
        int    cbAdd  = Math.max(1024, cbOld > 0x100000 ? cbOld >>> 1 : cbOld);
        int    cbNew  = (int) Math.min(m_cbMax,
                Math.max(((long) cbCap) + 1024, ((long) cbOld) + cbAdd));
        if (cbNew > cbOld)
            {
            // ensure that we don't allocate more than the configured maximum
            ExternalizableHelper.validateBufferSize(cbNew);

            byte[] abNew;
            while (true)
                {
                try
                    {
                    abNew = createBytes(cbNew);
                    break;
                    }
                catch (UnsupportedOperationException | OutOfMemoryError e)
                    {
                    if (cbCap == Integer.MAX_VALUE)
                        {
                        throw e;
                        }

                    // our pre-sizing was too aggressive; try to back down a bit
                    cbNew -= (cbNew - cbCap) / 2;
                    if (cbNew - 1 <= cbCap)
                        {
                        // create a new OOME to throw since the original one
                        // most likely doesn't have any stack trace info
                        throw e;
                        }
                    }
                }

            int cbData = m_cb;
            if (cbData > 0)
                {
                System.arraycopy(abOld, 0, abNew, 0, cbData);
                }

            m_ab        = abNew;
            m_bufUnsafe = null;
            }
        }

    /**
    * Update the length if the passed length is greater than the current
    * buffer length.
    *
    * @param cb  the count of the last byte written (or the index of the
    *            next byte to write)
    */
    protected final void updateLength(int cb)
        {
        if (cb > m_cb)
            {
            m_cb = cb;
            }
        }


    // ----- inner class: ByteArrayBufferOutput ----------------------------------

    /**
    * ByteArrayBufferOutput is an implementation of BufferOutput optimized
    * for writing to the buffer's underlying byte array.
    *
    * @author cp  2005.03.25
    */
    public final class ByteArrayBufferOutput
            extends AbstractBufferOutput
        {
        // ----- constructors -------------------------------------------

        /**
        * Construct an ByteArrayBufferOutput that will begin writing at the
        * start of the containing WriteBuffer.
        */
        public ByteArrayBufferOutput()
            {
            }

        /**
        * Construct an ByteArrayBufferOutput that will begin writing at the
        * specified offset within the containing WriteBuffer.
        *
        * @param of  the offset at which to begin writing
        */
        public ByteArrayBufferOutput(int of)
            {
            super(of);
            }

        // ----- DataOutput methods -------------------------------------

        /**
        * {@inheritDoc}
        */
        public void writeShort(int n)
                throws IOException
            {
            int of = m_ofWrite;
            checkBounds(of, 2);

            byte[] ab  = m_ab;
            ab[of]     = (byte) (n >>>  8);
            ab[of + 1] = (byte) (n);

            moveOffset(2);
            }

        /**
        * {@inheritDoc}
        */
        public void writeInt(int n)
                throws IOException
            {
            int of = m_ofWrite;
            checkBounds(of, 4);

            byte[] ab  = m_ab;
            ab[of  ]   = (byte) (n >>> 24);
            ab[of + 1] = (byte) (n >>> 16);
            ab[of + 2] = (byte) (n >>>  8);
            ab[of + 3] = (byte) (n);

            moveOffset(4);
            }

        /**
        * {@inheritDoc}
        */
        public void writeLong(long l)
                throws IOException
            {
            int of = m_ofWrite;
            checkBounds(of, 8);

            byte[] ab = m_ab;

            // hi word
            int n      = (int) (l >>> 32);
            ab[of  ]   = (byte) (n >>> 24);
            ab[of + 1] = (byte) (n >>> 16);
            ab[of + 2] = (byte) (n >>>  8);
            ab[of + 3] = (byte) (n);

            // lo word
            n = (int) l;
            ab[of + 4] = (byte) (n >>> 24);
            ab[of + 5] = (byte) (n >>> 16);
            ab[of + 6] = (byte) (n >>>  8);
            ab[of + 7] = (byte) (n);

            moveOffset(8);
            }

        /**
        * {@inheritDoc}
        */
        @SuppressWarnings("deprecation")
        public void writeBytes(String s)
                throws IOException
            {
            int of = m_ofWrite;
            int cb = s.length();
            checkBounds(of, cb);

            s.getBytes(0, cb, m_ab, of); // deprecated, but avoids encoding

            moveOffset(cb);
            }

        /**
        * {@inheritDoc}
        */
        public void writeChars(String s)
                throws IOException
            {
            int cch = s.length();
            int of  = m_ofWrite;
            int cb  = cch << 1;
            checkBounds(of, cb);

            byte[] ab  = m_ab;
            for (int ofch = 0; ofch < cch; ++ofch)
                {
                char ch = s.charAt(ofch);
                ab[of]     = (byte) (ch >>> 8);
                ab[of + 1] = (byte) (ch);
                of += 2;
                }

            moveOffset(cb);
            }

        /**
        * {@inheritDoc}
        */
        public void writeUTF(String s)
                throws IOException
            {
            if (s.isEmpty())
                {
                // 0-length UTF (Java UTF has a 2-byte length indicator)
                writeShort(0);
                }
            else
                {
                // estimate the length (in bytes) of the resulting UTF (see writeSafeUTF below for details)
                int cbEstimate = s.length();
                int ofHeader   = m_ofWrite;

                // Java UTF binary format has only 2 bytes for length; fail fast if we know we'll be attempting
                // to write more than 64K of data
                if (cbEstimate > 0xFFFF)
                    {
                    throw new UTFDataFormatException("UTF binary length=" + cbEstimate + ", max=65535");
                    }

                // now that we have a rough idea of the UTF length, make sure the buffer
                // is big enough; in theory, although unlikely, each character could use 3 bytes,
                // so we need to assume the worst-case scenario (4-byte characters are counted as two
                // characters, high and low surrogate, by String.length(), so that case is accounted for)
                int ofValue = ofHeader + 2;
                checkBounds(ofValue, cbEstimate * 3);

                // write the UTF directly into the buffer
                int cb = formatModifiedUTF(s, m_ab, ofValue);

                // Java UTF binary format has only 2 bytes for length
                if (cb > 0xFFFF)
                    {
                    throw new UTFDataFormatException("UTF binary length=" + cbEstimate + ", max=65535");
                    }

                // write the UTF header (the length in bytes)
                m_ab[ofHeader]     = (byte) (cb >>>  8);
                m_ab[ofHeader + 1] = (byte) (cb);

                moveOffset(2 + cb);
                }
            }

        public void writeUTF(CharBuffer bufCh)
                throws IOException
            {
            if (bufCh.isEmpty())
                {
                // 0-length UTF (Java UTF has a 2-byte length indicator)
                writeShort(0);
                }
            else
                {
                // estimate the length (in bytes) of the resulting UTF (see writeSafeUTF below for details)
                int cbEstimate = bufCh.length();
                int ofHeader   = m_ofWrite;

                // Java UTF binary format has only 2 bytes for length; fail fast if we know we'll be attempting
                // to write more than 64K of data
                if (cbEstimate > 0xFFFF)
                    {
                    throw new UTFDataFormatException("UTF binary length=" + cbEstimate + ", max=65535");
                    }

                // now that we have a rough idea of the UTF length, make sure the buffer
                // is big enough; in theory, although unlikely, each character could use 3 bytes,
                // so we need to assume the worst-case scenario (4-byte characters are counted as two
                // characters, high and low surrogate, by String.length(), so that case is accounted for)
                int ofValue = ofHeader + 2;
                checkBounds(ofValue, cbEstimate * 3);

                // write the UTF directly into the buffer
                int cb = formatModifiedUTF(bufCh, m_ab, ofValue);

                // Java UTF binary format has only 2 bytes for length
                if (cb > 0xFFFF)
                    {
                    throw new UTFDataFormatException("UTF binary length=" + cbEstimate + ", max=65535");
                    }
                else
                    {
                    // write the UTF header (the length in bytes)
                    m_ab[ofHeader]     = (byte) (cb >>>  8);
                    m_ab[ofHeader + 1] = (byte) (cb);
                    }

                moveOffset(2 + cb);
                }
            }

        // ----- BufferOutput methods -----------------------------------

        /**
        * {@inheritDoc}
        */
        public ByteBuffer getByteBuffer(int cb)
            {
            int of = m_ofWrite;
            checkBounds(of, cb);
            moveOffset(cb);

            return ByteBuffer.wrap(m_ab, of, cb);
            }

        /**
        * {@inheritDoc}
        */
        public void writeSafeUTF(String s)
                throws IOException
            {
            if (s == null)
                {
                writePackedInt(-1);
                }
            else
                {
                if (s.isEmpty())
                    {
                    writePackedInt(0);
                    }
                else if (isLatin1(s))
                    {
                    byte[] abString = value(s);
                    if (isAscii(abString))
                        {
                        // String uses ASCII encoding (characters 0x00-0x7F), which are represented using
                        // single byte UTF-8 format, so we can write value array directly
                        writePackedInt(abString.length);
                        write(abString);
                        }
                    else
                        {
                        // we need to convert all negative bytes (0x80-0xFF) into 2-byte UTF-8 format
                        int cb = abString.length + countNegatives(abString);
                        writePackedInt(cb);

                        int of = m_ofWrite;
                        checkBounds(of, cb);

                        byte[] ab = m_ab;
                        for (byte b : abString)
                            {
                            if (b < 0)
                                {
                                ab[of++] = (byte) (0xC0 | ((b & 0xFF) >> 6));
                                ab[of++] = (byte) (0x80 | (b & 0x3F));
                                }
                            else
                                {
                                ab[of++] = b;
                                }
                            }

                        moveOffset(cb);
                        }
                    }
                else
                    {
                    // The tricky part here is that we need to write encoded string length in bytes *before* we actually
                    // encode string, but we won't know the actual length in bytes *until* we encode it.
                    //
                    // We are going to assume that:
                    //
                    // 1. In most cases we are dealing with Latin1 encoded strings, with no multi-byte characters
                    // 2. Even when we are not, the number of bytes written will never be *less* then the string length
                    //
                    // Because of this, we will write the actual string length into the buffer, which is the correct value
                    // to write if string is a Latin1 string, or close enough to the final result that we can simply replace
                    // it with the correct value at the end
                    int cbEstimate = s.length();
                    int ofHeader   = m_ofWrite;

                    // write the UTF header (the estimated length in bytes)
                    writePackedInt(cbEstimate);

                    // now that we have a rough idea of the UTF length, make sure the buffer
                    // is big enough; in theory, although unlikely, each character could use 3 bytes,
                    // so we need to assume the worst-case scenario (4-byte characters are counted as two
                    // characters, high and low surrogate, by String.length(), so that case is accounted for)
                    int ofValue = m_ofWrite;
                    checkBounds(ofValue, cbEstimate * 3);

                    // write the UTF directly into the buffer
                    int cb = formatUTF(s, m_ab, ofValue);

                    if (cb != cbEstimate)
                        {
                        // unfortunately, we wrote more bytes than we assumed would be the case,
                        // so we need to fix the header
                        int cbHeader = ofValue - ofHeader;
                        int cbActual = cb < 0x40 ? 1 : (39 - Integer.numberOfLeadingZeros(cb)) / 7;
                        if (cbHeader != cbActual)
                            {
                            // bad news: we need more bytes for the header, so we need to move the value :-(
                            checkBounds(ofHeader, cbActual + cb);
                            int ofValueNew = ofValue + cbActual - cbHeader;
                            System.arraycopy(m_ab, ofValue, m_ab, ofValueNew, cb);
                            ofValue = ofValueNew;
                            }

                        // now we can simply overwrite the header with the actual number of bytes
                        m_ofWrite = ofHeader;
                        writePackedInt(cb);
                        m_ofWrite = ofValue;
                        }

                    moveOffset(cb);
                    }
                }
            }

        public void writeSafeUTF(CharBuffer bufCh)
                throws IOException
            {
            if (bufCh.isEmpty())
                {
                writePackedInt(0);
                }
            else
                {
                // The tricky part here is that we need to write encoded string length in bytes *before* we actually
                // encode string, but we won't know the actual length in bytes *until* we encode it.
                //
                // We are going to assume that:
                //
                // 1. In most cases we are dealing with Latin1 encoded strings, with no multi-byte characters
                // 2. Even when we are not, the number of bytes written will never be *less* then the string length
                //
                // Because of this, we will write the actual string length into the buffer, which is the correct value
                // to write if string is a Latin1 string, or close enough to the final result that we can simply replace
                // it with the correct value at the end
                int cbEstimate = bufCh.length();
                int ofHeader   = m_ofWrite;

                // write the UTF header (the estimated length in bytes)
                writePackedInt(cbEstimate);

                // now that we have a rough idea of the UTF length, make sure the buffer
                // is big enough; in theory, although unlikely, each character could use 4 bytes,
                // so we need to assume the worst-case scenario
                int ofValue = m_ofWrite;
                checkBounds(ofValue, cbEstimate << 2);

                // write the UTF directly into the buffer
                int cb = formatUTF(bufCh, m_ab, ofValue);

                if (cb != cbEstimate)
                    {
                    // unfortunately, we wrote more bytes than we assumed would be the case,
                    // so we need to fix the header
                    int cbHeader = ofValue - ofHeader;
                    int cbActual = cb < 0x40 ? 1 : (39 - Integer.numberOfLeadingZeros(cb)) / 7;
                    if (cbHeader != cbActual)
                        {
                        // bad news: we need more bytes for the header, so we need to move the value :-(
                        checkBounds(ofHeader, cbActual + cb);
                        int ofValueNew = ofValue + cbActual - cbHeader;
                        System.arraycopy(m_ab, ofValue, m_ab, ofValueNew, cb);
                        ofValue = ofValueNew;
                        }

                    // now we can simply overwrite the header with the actual number of bytes
                    m_ofWrite = ofHeader;
                    writePackedInt(cb);
                    m_ofWrite = ofValue;
                    }

                moveOffset(cb);
                }
            }

        /**
        * {@inheritDoc}
        */
        public void writePackedInt(int n)
                throws IOException
            {
            // first byte contains sign bit (bit 7 set if neg)
            int b = 0;
            if (n < 0)
                {
                b = 0x40;
                n = ~n;
                }

            // now that the value is positive, check its magnitude and see
            // how many bytes it will take to store
            int ofb = m_ofWrite;
            checkBounds(ofb, n < 0x40 ? 1 : (39 - Integer.numberOfLeadingZeros(n)) / 7);

            byte[] ab     = m_ab;
            int    ofOrig = ofb;

            // first byte contains only 6 data bits
            b |= (byte) (n & 0x3F);
            n >>>= 6;

            while (n != 0)
                {
                b |= 0x80; // bit 8 is a continuation bit
                ab[ofb++] = (byte) b;

                b = (n & 0x7F);
                n >>>= 7;
                }

            ab[ofb++] = (byte) b;
            moveOffset(ofb - ofOrig);
            }

        /**
        * {@inheritDoc}
        */
        public void writePackedLong(long l)
                throws IOException
            {
            // first byte contains sign bit (bit 7 set if neg)
            int b = 0;
            if (l < 0)
                {
                b = 0x40;
                l = ~l;
                }

            // now that the value is positive, check its magnitude and see
            // how many bytes it will take to store
            int ofb = m_ofWrite;
            checkBounds(ofb, l < 0x40 ? 1 : (71 - Long.numberOfLeadingZeros(l)) / 7);

            byte[] ab     = m_ab;
            int    ofOrig = ofb;

            // first byte contains only 6 data bits
            b |= (byte) (((int) l) & 0x3F);
            l >>>= 6;

            while (l != 0)
                {
                b |= 0x80; // bit 8 is a continuation bit
                ab[ofb++] = (byte) b;

                b = (((int) l) & 0x7F);
                l >>>= 7;
                }

            ab[ofb++] = (byte) b;
            moveOffset(ofb - ofOrig);
            }

        // ----- internal -----------------------------------------------

        /**
        * Move the offset within the stream forward.
        *
        * @param cb  the number of bytes to advance the offset
        */
        private void moveOffset(int cb)
            {
            int of = m_ofWrite + cb;
            m_ofWrite = of;
            updateLength(of);
            }
        }


    // ----- inner class: Allocator -----------------------------------------

    /**
    * Allocator is a WriteBufferPool implementation which allocates a new
    * ByteArrayWriteBuffer on each request to the pool, and does not retain
    * the returned buffer.  Essentially it is dummy pool which acts as an
    * allocator.
    */
    public static class Allocator
            implements MultiBufferWriteBuffer.WriteBufferPool
        {
        // ----- constructors -------------------------------------------

        /**
        * Construct an Allocator for ByteArrayWriteBuffers of a given size.
        *
        * @param cb  the capacity of the ByteArrayWriteBuffer to be allocated
        */
        public Allocator(int cb)
            {
            m_cb = cb;
            }

        // ----- WriteBufferPool interface ------------------------------

        /**
        * {@inheritDoc}
        */
        public int getMaximumCapacity()
            {
            return m_cb;
            }

        /**
        * Allocate a new ByteArrayWriteBuffer.
        *
        * @param cbPreviousTotal  <i>unused</i>
        *
        * @return a new ByteArrayWriteBuffer with this Allocator's
        *         {@link #getMaximumCapacity() capacity}
        */
        public WriteBuffer allocate(int cbPreviousTotal)
            {
            return new ByteArrayWriteBuffer(createBytes(m_cb));
            }

        /**
        * Release the supplied buffer into the pool.
        * <p>
        * This method is a no op.
        *
        * @param buffer  <i>unused</i>
        */
        public void release(WriteBuffer buffer)
            {
            // no op
            }

        // ----- data members -------------------------------------------

        /**
        * The capacity of the ByteArrayWriteBuffer instances to allocate.
        */
        protected int m_cb;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The byte array that holds the binary data.
    */
    protected byte[] m_ab;

    /**
    * Number of bytes in the byte array that have been written by this
    * WriteBuffer. This is the length.
    */
    protected int m_cb;

    /**
    * Number of bytes that the byte array can be grown to. This is the
    * maximum capacity.
    */
    protected int m_cbMax;

    /**
    * Cached ReadBuffer to quickly provide an answer to
    * {@link #getUnsafeReadBuffer()}.
    */
    protected transient ByteArrayReadBuffer m_bufUnsafe;

    /**
    * Specifies whether or not the byte array is treated as private data.
    */
    private boolean m_fPrivate;
    }
