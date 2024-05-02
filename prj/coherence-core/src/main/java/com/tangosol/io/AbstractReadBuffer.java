/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io;

import com.tangosol.util.Binary;
import com.tangosol.util.BinaryWriteBuffer;
import com.tangosol.util.ByteSequence;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.HashEncoded;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.InputStream;
import java.io.IOException;
import java.io.UTFDataFormatException;

import java.util.function.BinaryOperator;

import static com.oracle.coherence.common.base.Exceptions.ensureRuntimeException;

/**
* Abstract base implementation of the ReadBuffer interface.
*
* @author cp  2006.04.17
*/
public abstract class AbstractReadBuffer
        implements ReadBuffer, HashEncoded
    {
    /**
    * {@inheritDoc}
    */
    public BufferInput getBufferInput()
        {
        return instantiateBufferInput();
        }

    /**
    * {@inheritDoc}
    */
    public ReadBuffer getReadBuffer(int of, int cb)
        {
        // optimize getting the "same" buffer
        if (of == 0 && cb == length())
            {
            return this;
            }

        checkBounds(of, cb);

        // create a "sub" buffer
        return instantiateReadBuffer(of, cb);
        }

    /**
    * {@inheritDoc}
    */
    public byte[] toByteArray()
        {
        return toByteArray(0, length());
        }

    /**
    * {@inheritDoc}
    */
    public byte[] toByteArray(int of, int cb)
        {
        checkBounds(of, cb);

        byte[] ab;
        if (cb == 0)
            {
            ab = NO_BYTES;
            }
        else
            {
            ab = new byte[cb];
            copyBytes(of, of + cb, ab, 0);
            }

        return ab;
        }

    /**
    * {@inheritDoc}
    */
    public Binary toBinary()
        {
        return toBinary(0, length());
        }

    /**
    * {@inheritDoc}
    */
    public Binary toBinary(int of, int cb)
        {
        checkBounds(of, cb);

        WriteBuffer buf = new BinaryWriteBuffer(cb, cb);
        buf.write(0, this, of, cb);
        return buf.toBinary();
        }

    /**
    * {@inheritDoc}
    *
    * @since Coherence 3.7
    */
    public ByteSequence subSequence(int ofStart, int ofEnd)
        {
        return getReadBuffer(ofStart, ofEnd - ofStart);
        }

    // ----- HashEncoded interface ------------------------------------------

    /**
    * {@inheritDoc}
    */
    public int getEncodedHash()
        {
        return ExternalizableHelper.isIntDecorated(this)
                ? AbstractReadBuffer.readPackedInt(this, 1)
                : HashEncoded.UNENCODED;
        }


    /**
    * {@inheritDoc}
    */
    public boolean equals(Object o)
        {
        if (o instanceof ReadBuffer)
            {
            ReadBuffer that = (ReadBuffer) o;
            if (this == that)
                {
                return true;
                }

            int cb = this.length();
            if (cb != that.length())
                {
                return false;
                }

            if (cb == 0)
                {
                return true;
                }

            int    cbTempBuf = Math.min(cb, 32);
            byte[] abThis    = new byte[cbTempBuf];
            byte[] abThat    = new byte[cbTempBuf];
            int    of        = 0;
            do
                {
                int cbChunk = Math.min(cb, cbTempBuf);
                int ofEnd   = of + cbChunk;
                this.copyBytes(of, ofEnd, abThis, 0);
                that.copyBytes(of, ofEnd, abThat, 0);
                if (!Binary.equals(abThis, 0, abThat, 0, cbChunk))
                    {
                    return false;
                    }

                of += cbChunk;
                cb -= cbChunk;
                }
            while (cb > 0);

            return true;
            }
        else
            {
            return false;
            }
        }

    /**
    * {@inheritDoc}
    */
    public Object clone()
        {
        // this needs to be over-ridden if a shallow clone is not acceptable
        try
            {
            return super.clone();
            }
        catch (CloneNotSupportedException e)
            {
            throw ensureRuntimeException(e);
            }
        }


    // ----- factory methods ------------------------------------------------

    /**
    * Factory method: Instantiate a ReadBuffer for a portion of this
    * ReadBuffer.
    *
    * @param of  the beginning index, inclusive
    * @param cb  the number of bytes to include in the resulting ReadBuffer
    *
    * @return a ReadBuffer that represents a portion of this ReadBuffer
    */
    protected abstract ReadBuffer instantiateReadBuffer(int of, int cb);

    /**
    * Factory method: Instantiate a BufferInput object to read data from the
    * ReadBuffer.
    *
    * @return a new BufferInput reading from this ReadBuffer
    */
    protected abstract BufferInput instantiateBufferInput();


    // ----- inner class: BufferInput implementation ------------------------

    /**
    * This is an implementation of the BufferInput interface that delegates
    * its work back to its ReadBuffer.
    * <p>
    * This implementation extends InputStream, but only so that it can be
    * passed to anything that takes an InputStream.
    */
    public class AbstractBufferInput
            extends InputStream
            implements BufferInput
        {
        // ----- constructors -------------------------------------------

        /**
        * Default constructor.
        *
        * @implSpec
        * Initialize serial filter as it done for ObjectInputStream constructor.
        * <p>
        * In Java versions prior to 17, the deserialization filter is initialized to JVM-wide ObjectInputFilter.
        * <p>
        * In Java version 17 and greater, the deserialization filter is initialized to the filter returned
        * by invoking {@link ExternalizableHelper#getConfigSerialFilterFactory() serial filter factory}
        * with {@code null} for the current filter and the
        * {@linkplain ExternalizableHelper#getConfigSerialFilter() static JVM-wide filter} for the requested filter.
        */
        public AbstractBufferInput()
            {
            BinaryOperator factorySerialFilter = ExternalizableHelper.getConfigSerialFilterFactory();

            m_oInputFilter = factorySerialFilter == null
                                ? ExternalizableHelper.getConfigSerialFilter()
                                : factorySerialFilter.apply(null, ExternalizableHelper.getConfigSerialFilter());
            }

        // ----- InputStreaming methods ---------------------------------

        /**
        * {@inheritDoc}
        */
        public int read()
                throws IOException
            {
            try
                {
                int of = getOffset();
                int b = byteAt(of) & 0xFF;
                setOffsetInternal(of + 1);
                return b;
                }
            catch (IndexOutOfBoundsException e)
                {
                return -1;
                }
            }

        /**
        * {@inheritDoc}
        */
        public int read(byte ab[])
                throws IOException
            {
            return read(ab, 0, ab.length);
            }

        /**
        * {@inheritDoc}
        */
        public int read(byte ab[], int of, int cb)
                throws IOException
            {
            // validate parameters
            int cbDest = ab.length;
            if (of < 0 || cb < 0 || of + cb > cbDest)
                {
                throw new IndexOutOfBoundsException("ab.length=" + cbDest
                        + ", of=" + of + ", cb=" + cb);
                }

            // avoid confusing "nothing requested" with "nothing remains"
            if (cb == 0)
                {
                return 0;
                }

            // get the current position within the buffer
            int ofIn  = getOffset();

            // determine the amount to read by taking the smaller of the
            // requested and the remainder
            int cbRead = Math.min(cb, length() - ofIn);

            // check for eof
            if (cbRead == 0)
                {
                return -1;
                }
            else
                {
                // transfer the read data to the passed byte array
                int ofEnd = ofIn + cbRead;
                copyBytes(ofIn, ofEnd, ab, of);

                // update the read offset and return the number of bytes read
                setOffsetInternal(ofEnd);
                return cbRead;
                }
            }

        /**
        * {@inheritDoc}
        */
        public long skip(long cb)
                throws IOException
            {
            // scale down the requested skip into a 32-bit integer
            int cbReq  = (cb > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) cb);
            return skipBytes(cbReq);
            }

        /**
        * {@inheritDoc}
        */
        public int available()
                throws IOException
            {
            return length() - getOffset();
            }

        /**
        * {@inheritDoc}
        */
        public void close()
                throws IOException
            {
            s_achBuf.remove();
            }

        /**
        * {@inheritDoc}
        */
        public void mark(int cbReadLimit)
            {
            setMarkInternal(getOffset());
            }

        /**
        * {@inheritDoc}
        */
        public void reset()
                throws IOException
            {
            int of = getMarkInternal();
            if (of < 0)
                {
                throw new IOException("not marked");
                }
            setOffsetInternal(of);
            }

        /**
        * {@inheritDoc}
        */
        public boolean markSupported()
            {
            return true;
            }

        // ----- DataInput methods --------------------------------------

        /**
        * {@inheritDoc}
        */
        public void readFully(byte ab[])
                throws IOException
            {
            readFully(ab, 0, ab.length);
            }

        /**
        * {@inheritDoc}
        */
        public void readFully(byte ab[], int of, int cb)
                throws IOException
            {
            int cbRead = read(ab, of, cb);
            if (cbRead < cb)
                {
                throw new EOFException("requested=" + cb + ", read=" + cbRead);
                }
            }

        /**
        * {@inheritDoc}
        */
        public int skipBytes(int cb)
                throws IOException
            {
            // get the current position within the buffer and determine the
            // remainder of the buffer in bytes
            int of    = getOffset();
            int cbRem = length() - of;

            // determine the amount to skip by taking the smaller of the
            // requested and the remainder
            int cbSkip = Math.min(cb, cbRem);

            // do the skip and return the number of bytes skipped
            setOffsetInternal(of + cbSkip);
            return cbSkip;
            }

        /**
        * {@inheritDoc}
        */
        public boolean readBoolean()
                throws IOException
            {
            return readByte() != 0;
            }

        /**
        * {@inheritDoc}
        */
        public byte readByte()
                throws IOException
            {
            try
                {
                int  of = getOffset();
                byte b  = byteAt(of);
                setOffsetInternal(of + 1);
                return b;
                }
            catch (IndexOutOfBoundsException e)
                {
                throw new EOFException(e.getMessage());
                }
            }

        /**
        * {@inheritDoc}
        */
        public int readUnsignedByte()
                throws IOException
            {
            return readByte() & 0xFF;
            }

        /**
        * {@inheritDoc}
        */
        public short readShort()
                throws IOException
            {
            return (short) readUnsignedShort();
            }

        /**
        * {@inheritDoc}
        */
        public int readUnsignedShort()
                throws IOException
            {
            return (readUnsignedByte() << 8) |
                    readUnsignedByte();
            }

        /**
        * {@inheritDoc}
        */
        public char readChar()
                throws IOException
            {
            return (char) readUnsignedShort();
            }

        /**
        * {@inheritDoc}
        */
        public int readInt()
                throws IOException
            {
            return (readUnsignedByte() << 24) |
                   (readUnsignedByte() << 16) |
                   (readUnsignedByte() <<  8) |
                    readUnsignedByte();
            }

        /**
        * {@inheritDoc}
        */
        public long readLong()
                throws IOException
            {
            return (((long) readInt()) << 32) |
                   (((long) readInt()) & 0xFFFFFFFFL);
            }

        /**
        * {@inheritDoc}
        */
        public float readFloat()
                throws IOException
            {
            return Float.intBitsToFloat(readInt());
            }

        /**
        * {@inheritDoc}
        */
        public double readDouble()
                throws IOException
            {
            return Double.longBitsToDouble(readLong());
            }

        /**
        * {@inheritDoc}
        */
        public String readLine()
                throws IOException
            {
            // this is terribly inefficient!!!
            // .. but no one should be calling this method
            return new DataInputStream(this).readLine();
            }

        /**
        * {@inheritDoc}
        */
        public String readUTF()
                throws IOException
            {
            // the format is a 2-byte unsigned length-encoded binary, with
            // the binary being a UTF-encoded value
            return readUTF(readUnsignedShort());
            }

        // ----- BufferInput methods ------------------------------------

        /**
        * {@inheritDoc}
        */
        public ReadBuffer getBuffer()
            {
            return AbstractReadBuffer.this;
            }

        /**
        * {@inheritDoc}
        */
        public String readSafeUTF()
                throws IOException
            {
            return readUTF(readPackedInt());
            }

        /**
        * {@inheritDoc}
        */
        public int readPackedInt()
                throws IOException
            {
            int     b     = readUnsignedByte();
            int     n     = b & 0x3F;        // only 6 bits of data in first byte
            int     cBits = 6;
            boolean fNeg  = (b & 0x40) != 0; // seventh bit is a sign bit

            while ((b & 0x80) != 0)          // eighth bit is the continuation bit
                {
                b      = readUnsignedByte();
                n     |= ((b & 0x7F) << cBits);
                cBits += 7;
                }

            if (fNeg)
                {
                n = ~n;
                }

            return n;
            }

        /**
        * {@inheritDoc}
        */
        public long readPackedLong()
                throws IOException
            {
            int     b     = readUnsignedByte();
            long    l     = b & 0x3F;        // only 6 bits of data in first byte
            int     cBits = 6;
            boolean fNeg  = (b & 0x40) != 0; // seventh bit is a sign bit

            while ((b & 0x80) != 0)          // eighth bit is the continuation bit
                {
                b      = readUnsignedByte();
                l     |= (((long) (b & 0x7F)) << cBits);
                cBits += 7;
                }

            if (fNeg)
                {
                l = ~l;
                }

            return l;
            }

        /**
        * {@inheritDoc}
        */
        public ReadBuffer readBuffer(int cb)
                throws IOException
            {
            if (cb < 0)
                {
                throw new IllegalArgumentException("cb=" + cb);
                }

            if (cb == 0)
                {
                return NO_BINARY;
                }

            int of = getOffset();
            if (skipBytes(cb) < cb)
                {
                throw new EOFException();
                }

            return getReadBuffer(of, cb);
            }

        /**
        * {@inheritDoc}
        */
        public int getOffset()
            {
            return m_of;
            }

        /**
        * {@inheritDoc}
        */
        public void setOffset(int of)
            {
            if (of < 0 || of > length())
                {
                throw new IndexOutOfBoundsException("of=" + of
                        + ", getBuffer().length()=" + length());
                }

            setOffsetInternal(of);
            }

        @Override
        public final Object getObjectInputFilter()
            {
            return m_oInputFilter;
            }

        @Override
        public final synchronized void setObjectInputFilter(Object oInputFilter)
            {
            Object         oInputFilterCurrent = m_oInputFilter;
            BinaryOperator factorySerialFilter = ExternalizableHelper.getConfigSerialFilterFactory();

            if (m_fInputFilterSet)
                {
                throw new IllegalStateException("filter can not be set more than once");
                }

            // delegate to factory to compute stream serial filter in Java version 17 and greater
            Object oInputFilterNext = factorySerialFilter == null
                                        ? oInputFilter
                                        : factorySerialFilter.apply(oInputFilterCurrent, oInputFilter);

            if (oInputFilterCurrent != null && oInputFilterNext == null)
                {
                throw new IllegalStateException("filter can not be replaced by null filter");
                }
            m_oInputFilter = oInputFilterNext;
            m_fInputFilterSet = true;
            }

        // ----- internal -----------------------------------------------

        /**
        * Update the internal stream offset.
        *
        * @param of  the new offset
        */
        protected void setOffsetInternal(int of)
            {
            m_of = of;
            }

        /**
        * Adjust the internal stream offset.
        *
        * @param cb  the number of bytes that were written
        */
        protected void adjustOffsetInternal(int cb)
            {
            m_of += cb;
            }

        /**
        * Obtain the offset of the stream mark.
        *
        * @return the offset of the mark or -1 if unmarked
        */
        protected int getMarkInternal()
            {
            return m_ofMark;
            }

        /**
        * Update the internal stream mark.
        *
        * @param of  the offset of the new mark
        */
        protected void setMarkInternal(int of)
            {
            m_ofMark = of;
            }

        /**
        * Obtain a temp buffer used to avoid allocations when building
        * Strings from UTF binary data.
        *
        * @param cchMax  the length that the String will not exceed
        *
        * @return a char buffer of at least the specified length
        */
        protected char[] getCharBuf(int cchMax)
            {
            char[] ach = s_achBuf.get();
            if (ach == null || ach.length < cchMax)
                {
                ach = new char[Math.max(MIN_BUF, cchMax)];
                s_achBuf.set(ach);
                }
            return ach;
            }

        /**
        * Reads the specified-length UTF data and converts it to a String
        * value.
        *
        * @param cb  the number of bytes that the UTF data takes up in the
        *            stream; a value less than zero indicates a null value
        *
        * @return a String value; may be null if the passed length could be
        *         negative
        *
        * @exception UTFDataFormatException  if the bytes that were
        *            read were not a valid UTF-8 encoded string
        * @exception EOFException  if the value could not be read because no
        *            more data remains to be read
        * @exception IOException  if an I/O error occurs
        */
        protected String readUTF(int cb)
                throws IOException
            {
            if (cb < 0)
                {
                return null;
                }
            else if (cb == 0)
                {
                return "";
                }

            int of    = getOffset();
            int cbBuf = length();
            if (of + cb > cbBuf)
                {
                // pretend to have read to end-of-stream
                setOffsetInternal(cbBuf);
                throw new EOFException();
                }
            else
                {
                // pretend to have read to the end of the binary
                setOffsetInternal(of + cb);
                }

            // per JDK serialization filtering doc:
            //     The filter is not called ... for java.lang.String instances that are encoded concretely in the stream.
            return convertUTF(of, cb);
            }

        /**
        * Convert a UTF-8 encoded section of the binary stream into a String.
        *
        * @param of  the offset within the stream
        * @param cb  the length in bytes within the stream
        *
        * @return the String value
        *
        * @throws IOException  if an I/O or conversion exception occurs, such
        *                      as UTFDataFormatException
        */
        protected String convertUTF(int of, int cb)
                throws IOException
            {
            return ExternalizableHelper.convertUTF(
                    toByteArray(of, cb), 0, cb, getCharBuf(cb));
            }

        // ----- data members -------------------------------------------

        /**
        * Current stream offset into the containing ReadBuffer.
        */
        private int m_of;

        /**
        * The marked position expressed as a stream offset.
        */
        private int m_ofMark = -1;

        /**
        * A lazily instantiated thread local temp buffer used to avoid allocations for
        * building Strings from UTF binaries.
        */
        private static ThreadLocal<char[]> s_achBuf = new ThreadLocal<>();

        /**
        * When not null, filter to validate that an instance of a class can be deserialized from
        * this {@link BufferInput}.
        */
        private volatile Object m_oInputFilter;

        /**
        * True if the stream-specific {@link BufferInput this} {@link #getObjectInputFilter() serial filter}
        * has been {@link #setObjectInputFilter(Object) set}; initially false.
        */
        private volatile boolean m_fInputFilterSet = false;
        }


    // ----- internal -------------------------------------------------------

    /**
    * Check if the specified read is within bounds.
    *
    * @param of  the absolute offset of the read operation
    * @param cb  the length in bytes of the read operation
    *
    * @throws IndexOutOfBoundsException  if the specified read is not within
    *         bounds
    */
    protected void checkBounds(int of, int cb)
        {
        if (of < 0 || cb < 0 || of + cb > length())
            {
            throw new IndexOutOfBoundsException("of=" + of + ", cb=" + cb
                    + ", length()=" + length());
            }
        }

    /**
    * Read an unsigned byte value from the given {@link ByteSequence} and
    * offset.
    * <p>
    * This method is the counterpart for the
    * {@link java.io.DataOutput#writeByte} method when it is used with
    * unsigned 8-bit values.
    *
    * @param bin  the byte sequence
    * @param of   the offset
    *
    * @return an <code>int</code> value in the range 0x00 to 0xFF
    */
    public static int readUnsignedByte(ByteSequence bin, int of)
        {
        return bin.byteAt(of) & 0xFF;
        }

    /**
    * Read an int value using a variable-length storage format as described
    * by {@link WriteBuffer.BufferOutput#writePackedInt(int)} from the given
    * {@link ByteSequence} and offset.
    *
    * @param bin  the byte sequence
    * @param of   the offset
    *
    * @return  an int value
    */
    public static int readPackedInt(ByteSequence bin, int of)
        {
        int     b     = readUnsignedByte(bin, of++);
        int     n     = b & 0x3F;        // only 6 bits of data in first byte
        int     cBits = 6;
        boolean fNeg  = (b & 0x40) != 0; // seventh bit is a sign bit

        while ((b & 0x80) != 0)          // eighth bit is the continuation bit
            {
            b      = readUnsignedByte(bin, of++);
            n     |= ((b & 0x7F) << cBits);
            cBits += 7;
            }

        if (fNeg)
            {
            n = ~n;
            }

        return n;
        }

    /**
    * Return the number of bytes that would be required to store the given int
    * using the variable-length storage format as described by {@link
    * WriteBuffer.BufferOutput#writePackedInt(int)}.
    *
    * @param n  the integer that will be stored as a packed int
    *
    * @return the number of bytes required to store the packed int
    */
    public static int sizeofPackedInt(int n)
        {
        if (n < 0)
            {
            n = ~n;
            }

        int cb = n == 0 ? 1 : 0;
        for (int iShift = 6; n > 0; iShift = 7)
            {
            n >>= iShift;
            cb++;
            }
        return cb;
        }

    // ----- constants ------------------------------------------------------

    /**
    * An empty byte array (by definition immutable).
    */
    public static final byte[] NO_BYTES = new byte[0];

    /**
    * An empty Binary object.
    */
    public static final Binary NO_BINARY = new Binary(NO_BYTES);

    /**
    * The minimum size of a temp buffer.
    */
    static final int MIN_BUF = 64;
    }
