/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io;


import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataOutput;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;

import java.nio.ByteBuffer;

import javax.json.bind.annotation.JsonbProperty;


/**
* The AbstractByteArrayReadBuffer abstract class is intended to serve as
* a base class for the following:
* <ol>
* <li>the pre-existing Binary class</li>
* <li>a new byte[] based class that does not attempt to add the immutability
* aspect provided by the Binary class</li>
* <li>a new ByteBuffer based class that will work with Java NIO</li>
* </ol>
* <p>
* This implementation is not intended to be thread safe.
*
* @author cp  2005.01.18
*/
public abstract class AbstractByteArrayReadBuffer
        extends AbstractReadBuffer
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor; intended for deserialization use by subclasses.
    * <p>
    * Note that this default constructor leaves the buffer in an invalid
    * state.
    */
    protected AbstractByteArrayReadBuffer()
        {
        }

    /**
    * Construct an AbstractByteArrayReadBuffer on a portion of a byte array.
    *
    * @param ab  a byte array
    * @param of  an offset into the byte array
    * @param cb  the number of bytes to utilize
    *
    * @exception  IndexOutOfBoundsException  if <code>of</code> or
    *             <code>cb</code> is negative, or <code>of + cb</code> is
    *             larger than <code>ab.length</code>
    * @exception NullPointerException if <code>ab</code> is <code>null</code>
    */
    protected AbstractByteArrayReadBuffer(byte[] ab, int of, int cb)
        {
        m_ab = ab;
        resetRange(of, cb);
        }


    // ----- ReadBuffer methods ---------------------------------------------

    /**
    * {@inheritDoc}
    */
    public int length()
        {
        return m_cb;
        }

    /**
    * {@inheritDoc}
    */
    public byte byteAt(int of)
        {
        if (of >= 0 && of < m_cb)
            {
            return m_ab[m_of + of];
            }
        else
            {
            throw new IndexOutOfBoundsException("of=" + of
                    + ", length()=" + m_cb);
            }
        }

    /**
    * {@inheritDoc}
    */
    public void copyBytes(int ofBegin, int ofEnd, byte abDest[], int ofDest)
        {
        if (ofBegin < 0 || ofEnd > m_cb || ofBegin > ofEnd)
            {
            throw new IndexOutOfBoundsException("ofBegin=" + ofBegin
                    + ", ofEnd=" + ofEnd + ", Binary.length=" + m_cb);
            }
        System.arraycopy(m_ab, m_of + ofBegin, abDest, ofDest, ofEnd - ofBegin);
        }

    /**
    * {@inheritDoc}
    */
    public void writeTo(OutputStream out)
            throws IOException
        {
        out.write(m_ab, m_of, m_cb);
        }

    /**
    * {@inheritDoc}
    */
    public void writeTo(OutputStream out, int of, int cb)
            throws IOException
        {
        out.write(m_ab, m_of + of, cb);
        }

    /**
    * {@inheritDoc}
    */
    public void writeTo(DataOutput out)
            throws IOException
        {
        out.write(m_ab, m_of, m_cb);
        }

    /**
    * {@inheritDoc}
    */
    public void writeTo(DataOutput out, int of, int cb)
            throws IOException
        {
        out.write(m_ab, m_of + of, cb);
        }

    /**
    * {@inheritDoc}
    */
    public void writeTo(ByteBuffer buf)
        {
        buf.put(m_ab, m_of, m_cb);
        }

    /**
    * {@inheritDoc}
    */
    public void writeTo(ByteBuffer buf, int of, int cb)
            throws IOException
        {
        buf.put(m_ab, m_of + of, cb);
        }

    /**
    * {@inheritDoc}
    */
    public byte[] toByteArray(int of, int cb)
        {
        checkBounds(of, cb);
        if (cb == 0)
            {
            return NO_BYTES;
            }

        byte[] abNew;
        byte[] ab = m_ab;

        // adjust offset based on what part of the underlying byte[] this
        // buffer is "over"
        of += m_of;
        if (of == 0 && cb == ab.length && !isByteArrayPrivate())
            {
            // just return the underlying (non-private) byte[]
            return ab;
            }

        abNew = new byte[cb];
        System.arraycopy(ab, of, abNew, 0, cb);

        return abNew;
        }

    /**
    * {@inheritDoc}
    */
    public Binary toBinary()
        {
        int cb = m_cb;
        return cb == 0 ? NO_BINARY
                       : new Binary(m_ab, m_of, cb);
        }

    /**
    * {@inheritDoc}
    */
    public Binary toBinary(int of, int cb)
        {
        checkBounds(of, cb);

        return cb == 0 ? NO_BINARY
                       : new Binary(m_ab, m_of + of, cb);
        }

    /**
    * {@inheritDoc}
    */
    public ByteBuffer toByteBuffer()
        {
        return ByteBuffer.wrap(m_ab, m_of, m_cb).asReadOnlyBuffer();
        }

    /**
    * {@inheritDoc}
    */
    public ByteBuffer toByteBuffer(int of, int cb)
        {
        checkBounds(of, cb);

        return ByteBuffer.wrap(m_ab, m_of + of, cb).asReadOnlyBuffer();
        }

    /**
    * {@inheritDoc}
    */
    public boolean equals(Object o)
        {
        if (o instanceof AbstractByteArrayReadBuffer)
            {
            AbstractByteArrayReadBuffer that = (AbstractByteArrayReadBuffer) o;
            if (this == that)
                {
                return true;
                }

            int cb = this.m_cb;
            return cb == that.m_cb &&
                    Binary.equals(this.m_ab, this.m_of, that.m_ab, that.m_of, cb);
            }
        else
            {
            return super.equals(o);
            }
        }


    // ----- factory methods ------------------------------------------------

    /**
    * {@inheritDoc}
    */
    protected BufferInput instantiateBufferInput()
        {
        return new ByteArrayBufferInput();
        }


    // ----- internal -------------------------------------------------------

    /**
    * Reset the portion of the byte array the ReadBuffer operates upon.
    *
    * @param of  an offset into the byte array
    * @param cb  the number of bytes to utilize
    *
    * @exception  IndexOutOfBoundsException  if <code>of</code> or
    *             <code>cb</code> are negative, or <code>of + cb</code> is
    *             larger than the buffer's length
    */
    protected void resetRange(int of, int cb)
        {
        byte[] ab = m_ab;
        if (of < 0 || cb < 0 || of + cb > ab.length)
            {
            throw new IndexOutOfBoundsException("of=" + of + ", cb=" + cb
                    + ", ab.length=" + ab.length);
            }

        m_of = of;
        m_cb = cb;
        }

    /**
    * Determine if the underlying byte[] should be treated as private data.
    *
    * @return true iff the underlying data should not ever be exposed by
    *         this object
    */
    protected abstract boolean isByteArrayPrivate();


    // ----- inner class: BufferInput implementation ------------------------

    /**
    * This is a simple implementation of the BufferInput interface on top of
    * a byte array.
    * <p>
    * This implementation extends InputStream, but only so that it can be
    * passed to anything that takes an InputStream.
    */
    public final class ByteArrayBufferInput
            extends AbstractBufferInput
        {
        // ----- constructors -------------------------------------------

        /**
        * Default constructor.
        */
        public ByteArrayBufferInput()
            {
            }


        // ----- InputStreaming methods ---------------------------------

        /**
        * {@inheritDoc}
        */
        public int read()
                throws IOException
            {
            int of = getOffset();
            if (of < m_cb)
                {
                // increment read position
                setOffsetInternal(of + 1);

                // return the unsigned byte from the previous read position
                return m_ab[m_of + of] & 0xFF;
                }
            else
                {
                return -1;
                }
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
            int cbRead = Math.min(cb, m_cb - ofIn);

            // check for eof
            if (cbRead == 0)
                {
                return -1;
                }
            else
                {
                // transfer the read data to the passed byte array
                System.arraycopy(m_ab, m_of + ofIn, ab, of, cbRead);

                // update the read offset and return the number of bytes read
                setOffsetInternal(ofIn + cbRead);
                return cbRead;
                }
            }

        // ----- DataInput methods --------------------------------------

        /**
        * {@inheritDoc}
        */
        public byte readByte()
                throws IOException
            {
            int ofAdd = getOffset();
            if (ofAdd < m_cb)
                {
                // increment read position
                setOffsetInternal(ofAdd + 1);

                // return the unsigned byte from the previous read position
                return m_ab[m_of + ofAdd];
                }
            else
                {
                throw new EOFException();
                }
            }

        /**
        * {@inheritDoc}
        */
        public int readUnsignedShort()
                throws IOException
            {
            // check for EOF
            int ofAdd = getOffset();
            if (m_cb - ofAdd < 2)
                {
                // pretend we exhausted the stream trying to read the value
                setOffsetInternal(m_cb);
                throw new EOFException();
                }

            // increment read position
            setOffsetInternal(ofAdd + 2);

            // build and return the value
            byte[] ab = m_ab;
            int    of = m_of + ofAdd;
            return ((ab[of] & 0xFF) << 8) | (ab[of + 1] & 0xFF);
            }

        /**
        * {@inheritDoc}
        */
        public int readInt()
                throws IOException
            {
            // check for EOF
            int ofAdd = getOffset();
            if (m_cb - ofAdd < 4)
                {
                // pretend we exhausted the stream trying to read the value
                setOffsetInternal(m_cb);
                throw new EOFException();
                }

            // increment read position
            setOffsetInternal(ofAdd + 4);

            // build and return the value
            byte[] ab = m_ab;
            int    of = m_of + ofAdd;
            return    ((ab[of    ]       ) << 24)
                    | ((ab[of + 1] & 0xFF) << 16)
                    | ((ab[of + 2] & 0xFF) << 8 )
                    | ((ab[of + 3] & 0xFF)      );
            }

        /**
        * {@inheritDoc}
        */
        public long readLong()
                throws IOException
            {
            // check for EOF
            int ofAdd = getOffset();
            if (m_cb - ofAdd < 8)
                {
                // pretend we exhausted the stream trying to read the value
                setOffsetInternal(m_cb);
                throw new EOFException();
                }

            // increment read position
            setOffsetInternal(ofAdd + 8);

            // build and return the value
            byte[] ab = m_ab;
            int    of = m_of + ofAdd;
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

        // ----- BufferInput methods ------------------------------------

        /**
        * {@inheritDoc}
        */
        public int readPackedInt()
                throws IOException
            {
            byte[] ab    = m_ab;
            int    ofRaw = m_of;
            int    of    = ofRaw + getOffset();
            int    cb    = ofRaw + m_cb;

            // check EOF
            if (of >= cb)
                {
                throw new EOFException();
                }

            int     b     = ab[of++] & 0xFF;
            int     n     = b & 0x3F;           // only 6 bits of data in first byte
            int     cBits = 6;
            boolean fNeg  = (b & 0x40) != 0;    // seventh bit is a sign bit

            while ((b & 0x80) != 0)             // eighth bit is the continuation bit
                {
                // check EOF
                if (of >= cb)
                    {
                    throw new EOFException();
                    }

                b      = ab[of++] & 0xFF;
                n     |= ((b & 0x7F) << cBits);
                cBits += 7;
                }

            if (fNeg)
                {
                n = ~n;
                }

            // increment read position
            setOffsetInternal(of - ofRaw);

            return n;
            }

        /**
        * {@inheritDoc}
        */
        public long readPackedLong()
                throws IOException
            {
            byte[] ab    = m_ab;
            int    ofRaw = m_of;
            int    of    = ofRaw + getOffset();
            int    cb    = ofRaw + m_cb;

            // check EOF
            if (of >= cb)
                {
                throw new EOFException();
                }

            int     b     = ab[of++] & 0xFF;
            long    l     = b & 0x3F;           // only 6 bits of data in first byte
            int     cBits = 6;
            boolean fNeg  = (b & 0x40) != 0;    // seventh bit is a sign bit

            while ((b & 0x80) != 0)             // eighth bit is the continuation bit
                {
                // check EOF
                if (of >= cb)
                    {
                    throw new EOFException();
                    }

                b      = ab[of++] & 0xFF;
                l     |= (((long) (b & 0x7F)) << cBits);
                cBits += 7;
                }

            if (fNeg)
                {
                l = ~l;
                }

            // increment read position
            setOffsetInternal(of - ofRaw);

            return l;
            }

        // ----- internal -----------------------------------------------

        /**
        * {@inheritDoc}
        */
        protected String convertUTF(int of, int cb)
                throws IOException
            {
            return ExternalizableHelper.convertUTF(
                    m_ab, m_of + of, cb, getCharBuf(cb));
            }
        }


    // ----- data members ---------------------------------------------------

    /**
    * The byte array that holds the binary data.
    * This value should not be changed.
    */
    @JsonbProperty("byteArray")
    protected byte[] m_ab;

    /**
    * Offset into the byte array at which the binary data is located.
    * This value should not be changed.
    */
    @JsonbProperty("offset")
    protected int m_of;

    /**
    * Number of bytes in the byte array that belong to this ReadBuffer object.
    * This value should not be changed.
    */
    @JsonbProperty("length")
    protected int m_cb;
    }
