/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import com.tangosol.io.ByteArrayWriteBuffer;


/**
* a WriteBuffer implementation whose primary purpose is to be used to create
* Binary objects.
*
* @author cp  2005.06.02
*/
public final class BinaryWriteBuffer
        extends ByteArrayWriteBuffer
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct an BinaryWriteBuffer with a certain initial capacity.
    *
    * @param cbCap  initial capacity
    *
    * @exception IllegalArgumentException if <tt>cbCap</tt> is negative
    */
    public BinaryWriteBuffer(int cbCap)
        {
        super(cbCap);
        init();
        }

    /**
    * Construct an BinaryWriteBuffer with a certain initial capacity and
    * a certain maximum capacity.
    *
    * @param cbCap  initial capacity
    * @param cbMax  maximum capacity
    *
    * @exception IllegalArgumentException if <tt>cbCap</tt> or <tt>cbMax</tt>
    *            is negative, or if <tt>cbCap</tt> is greater than
    *            <tt>cbMax</tt>
    */
    public BinaryWriteBuffer(int cbCap, int cbMax)
        {
        super(cbCap, cbMax);
        init();
        }

    /**
    * Private initialization.
    */
    private void init()
        {
        makeByteArrayPrivate();
        }


    // ----- WriteBuffer interface ------------------------------------------

    /**
    * {@inheritDoc}
    */
    public Binary toBinary()
        {
        return new Binary(this);
        }


    // ----- internal -------------------------------------------------------

    /**
    * Obtain the internal byte array that this WriteBuffer uses.
    * <p>
    * Package private, for use only by Binary.
    *
    * @return the actual byte array that this WriteBuffer uses
    */
    byte[] getInternalByteArray()
        {
        m_fReadOnly = true;
        return m_ab;
        }

    /**
    * Validate the ranges for the passed bounds and make sure that the
    * underlying array is big enough to handle them.
    * <p>
    * Note: This method prevents all modifications from occurring once the
    * BinaryWriteBuffer has supplied its byte array to a Binary object.
    *
    * @param of  the offset that data is about to be written to
    * @param cb  the length of the data that is about to be written
    */
    protected void checkBounds(int of, int cb)
        {
        if (m_fReadOnly)
            {
            throw new IllegalStateException("WriteBuffer is immutable");
            }
        super.checkBounds(of, cb);
        }


    // ----- data members ---------------------------------------------------

    /**
    * Indicator that no more modifications are permitted.
    */
    private boolean m_fReadOnly;
    }
