/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io;

import static com.oracle.coherence.common.base.Formatting.toHexEscape;

/**
* A ReadBuffer on top of a byte array.
*
* @author cp  2005.01.20
*/
public class ByteArrayReadBuffer
        extends AbstractByteArrayReadBuffer
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor provided for subclasses.
    */
    protected ByteArrayReadBuffer()
        {
        super();
        }

    /**
    * Construct a ByteArrayReadBuffer object from a byte array.
    * This constructor does not copy the byte array or make it private.
    *
    * @param ab  an array of bytes
    */
    public ByteArrayReadBuffer(byte[] ab)
        {
        this(ab, 0, ab.length);
        }

    /**
    * Construct a ByteArrayReadBuffer object from a portion of a byte array.
    * This constructor does not copy the byte array or make it private.
    *
    * @param ab  an array of bytes
    * @param of  the offset into the byte array
    * @param cb  the number of bytes to extract
    */
    public ByteArrayReadBuffer(byte[] ab, int of, int cb)
        {
        this(ab, of, cb, false, false, false);
        }

    /**
    * Construct a ByteArrayReadBuffer object from a portion of a byte array.
    *
    * @param ab             an array of bytes
    * @param of             the offset into the byte array
    * @param cb             the number of bytes to extract
    * @param fCopy          true to make a copy of the passed array
    * @param fPrivate       true to treat the passed array as private data
    * @param fShallowClone  true to allow cloning without copying the
    *                       underlying byte[]
    */
    public ByteArrayReadBuffer(byte[] ab, int of, int cb, boolean fCopy,
                               boolean fPrivate, boolean fShallowClone)
        {
        super(fCopy ? ab.clone() : ab, of, cb);

        m_fPrivate      = fPrivate;
        m_fShallowClone = fShallowClone;
        }


    // ----- AbstractByteArrayReadBuffer methods ----------------------------

    /**
    * {@inheritDoc}
    */
    public final void resetRange(int of, int cb)
        {
        super.resetRange(of, cb);
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Obtain the byte array that this ReadBuffer uses. If the underlying byte
    * array is private, then this method will always return a copy of the
    * portion of the byte array that this ReadBuffer represents as if the
    * called had called {@link #toByteArray()}.
    *
    * @return the byte array that this ReadBuffer uses
    */
    public final byte[] getRawByteArray()
        {
        return isByteArrayPrivate() ? toByteArray() : m_ab;
        }

    /**
    * Determine the offset into the byte array returned from
    * {@link #getRawByteArray()} that this ReadBuffer uses. If the
    * underlying byte array is private, then this method will always
    * return zero because {@link #getRawByteArray()} will always return
    * a copy of the portion of the byte array that this ReadBuffer
    * represents.
    *
    * @return the offset into the raw byte array that this ReadBuffer uses
    */
    public final int getRawOffset()
        {
        return isByteArrayPrivate() ? 0 : m_of;
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
    protected final ReadBuffer instantiateReadBuffer(int of, int cb)
        {
        return new ByteArrayReadBuffer(m_ab, m_of + of, cb, false,
                isByteArrayPrivate(), isShallowCloneable());
        }


    // ----- Object methods -------------------------------------------------

    /**
    * {@inheritDoc}
    */
    public Object clone()
        {
        if (isShallowCloneable())
            {
            // no mutable state
            return this;
            }

        // no shallow clone -- we MUST copy the byte array
        byte[]  ab = m_ab;
        int     of = m_of;
        int     cb = m_cb;
        byte[]  abNew;
        if (of == 0 && cb == ab.length)
            {
            // just clone our byte array
            abNew = ab.clone();
            }
        else
            {
            abNew = new byte[cb];
            System.arraycopy(ab, of, abNew, 0, cb);
            }

        // if this buffer's data is private, then the clone's data will be
        // private, and as a result, the clone will therefore be shallow
        // cloneable
        boolean fPrivate = isByteArrayPrivate();

        return new ByteArrayReadBuffer(abNew, 0, cb, false, fPrivate, fPrivate);
        }

    /**
    * Provide a human-readable representation of the Binary object.
    *
    * @return a String whose contents represent the value of this Binary
    *         object
    */
    public String toString()
        {
        // "ByteArrayReadBuffer(length=4, value=0x01F03DA7)"
        return "ByteArrayReadBuffer(length="
                + m_cb
                + ", value"
                + (m_cb > 2 * 1024 
                        ? " (truncated by " + (m_cb - 2 * 1024) + " bytes)=" 
                        : "=")
                + toHexEscape(m_ab, m_of, Math.min(2 * 1024, m_cb)) + ')';
        }


    // ----- internal -------------------------------------------------------

    /**
    * {@inheritDoc}
    */
    protected final boolean isByteArrayPrivate()
        {
        return m_fPrivate;
        }

    /**
    * Determine whether a clone can be made without cloning the byte array.
    *
    * @return true iff the underlying data can be shared by multiple
    *         instances of the ByteArrayReadBuffer class
    */
    protected final boolean isShallowCloneable()
        {
        return m_fShallowClone;
        }

    /**
    * Allow the length to be modified. This method is provided for use by
    * ByteArrayWriteBuffer only, and only for read buffers that it owns.
    *
    * @param cb  the new length for the ByteArrayReadBuffer
    */
    void updateLength(int cb)
        {
        m_cb = cb;
        }


    // ----- data members ---------------------------------------------------

    /**
    * Specifies whether or not the byte array is treated as private data.
    */
    private transient boolean m_fPrivate;

    /**
    * Specifies whether a clone can be made without cloning the byte array.
    */
    private transient boolean m_fShallowClone;
    }
