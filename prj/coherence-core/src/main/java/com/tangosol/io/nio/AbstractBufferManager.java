/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.nio;


import com.tangosol.util.Base;

import java.nio.ByteBuffer;


/**
* Provides a basic implementation of ByteBufferManager.
*
* @author cp  2002.09.19
*
* @since Coherence 2.2
*/
public abstract class AbstractBufferManager
        extends Base
        implements ByteBufferManager
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a AbstractBufferManager that supports a buffer of a certain
    * initial and maximum size.
    *
    * @param cbInitial  the initial size
    * @param cbMaximum  the maximum size
    */
    protected AbstractBufferManager(int cbInitial, int cbMaximum)
        {
        if (cbInitial < 0)
            {
            throw new IllegalArgumentException("initial size must be > 0");
            }
        if (cbMaximum < 0)
            {
            throw new IllegalArgumentException("maximum size must be > 0");
            }
        setMinCapacity(cbInitial);
        setMaxCapacity(cbMaximum);
        setCapacity(cbInitial);
        }


    // ----- ByteBufferManager interface ------------------------------------

    /**
    * Get the current ByteBuffer reference.
    *
    * @return the current ByteBuffer
    */
    public ByteBuffer getBuffer()
        {
        return m_buffer;
        }

    /**
    * Set the current ByteBuffer reference.
    *
    * @param buffer  the current ByteBuffer
    */
    protected void setBuffer(ByteBuffer buffer)
        {
        m_buffer = buffer;
        }

    /**
    * Get the capacity of the current ByteBuffer. This is the same as
    * <code>getBuffer().capacity()</code>.
    *
    * @return the capacity of the current ByteBuffer
    */
    public int getCapacity()
        {
        return m_cbCurrent;
        }

    /**
    * Set the capacity of the current ByteBuffer. This does not actually
    * allocate a new buffer.
    *
    * @param cb  the capacity of the managed ByteBuffer
    */
    protected void setCapacity(int cb)
        {
        m_cbCurrent = cb;
        calibrate();
        }

    /**
    * Determine the level (what number of bytes used) above which the current
    * ByteBuffer will need to be "resized" upwards. Returns Integer.MAX_VALUE
    * if the buffer cannot be resized upwards.
    *
    * @return the number of bytes that, when the number of used bytes exceeds
    *         it, the ByteBuffer will need to grow
    */
    public int getGrowthThreshold()
        {
        return m_cbGrow;
        }

    /**
    * Specify the level (what number of bytes used) above which the current
    * ByteBuffer will need to be "resized" upwards.
    *
    * @param cb  the number of bytes that, when the number of used bytes
    *            exceeds it, the ByteBuffer will need to grow
    */
    protected void setGrowthThreshold(int cb)
        {
        m_cbGrow = cb;
        }

    /**
    * Determine the level (what number of bytes used) below which the current
    * ByteBuffer will need to be "resized" downwards. Returns 0 if the buffer
    * cannot be resized downwards.
    *
    * @return the number of bytes that, when the number of used bytes drops
    *         below it, the ByteBuffer will need to shrink
    */
    public int getShrinkageThreshold()
        {
        return m_cbShrink;
        }

    /**
    * Specify the level (what number of bytes used) below which the current
    * ByteBuffer will need to be "resized" downwards. Specify 0 if the buffer
    * cannot be resized downwards.
    *
    * @param cb  the number of bytes that, when the number of used bytes
    *            drops below it, the ByteBuffer will need to shrink
    */
    protected void setShrinkageThreshold(int cb)
        {
        m_cbShrink = cb;
        }

    /**
    * Determine the minimum size that the managed buffer can reach. If the
    * buffer is already at its minimum, then this method will return the
    * same value as <code>getCapacity</code>.
    *
    * @return minimum size for the managed buffer
    */
    public int getMinCapacity()
        {
        return m_cbInitial;
        }

    /**
    * Specify the minimum size that the managed buffer can reach.
    *
    * @param cb  minimum size for the managed buffer
    */
    protected void setMinCapacity(int cb)
        {
        m_cbInitial = cb;
        }

    /**
    * Determine the maximum size that the managed buffer can reach. If the
    * buffer is already at its maximum, then this method will return the
    * same value as <code>getCapacity</code>.
    *
    * @return maximum size for the managed buffer
    */
    public int getMaxCapacity()
        {
        return m_cbMaximum;
        }

    /**
    * Specify the maximum size that the managed buffer can reach.
    *
    * @param cb  maximum size for the managed buffer
    */
    protected void setMaxCapacity(int cb)
        {
        m_cbMaximum = cb;
        }

    /**
    * @return maximum size to grow in one step
    */
    protected int getMaxIncrement()
        {
        // 128 MB
        return 0x7FFFFFF;
        }

    /**
    * Request that the buffer be grown based on the number of bytes
    * currently required.
    *
    * @param cbRequired  the number of bytes that are needed by the
    *                    requesting operation
    */
    public void grow(int cbRequired)
        {
        if (cbRequired > getGrowthThreshold())
            {
            long cbMax     = getMaxCapacity();
            long cbMaxIncr = getMaxIncrement();

            if (cbRequired > cbMax)
                {
                throw new IllegalArgumentException("cannot grow ByteBuffer; required="
                        + cbRequired + ", max=" + cbMax);
                }

            long cbNew = getCapacity();
            do
                {
                // cbMax and cbMaxIncr will never exceed Integer.MAX_VALUE,
                // so cbNew will never overflow an integer as a result of the
                // calculation.
                cbNew = Math.min(Math.min(cbNew * 2, cbMax), cbNew + cbMaxIncr);
                setCapacity((int) cbNew);
                }
            while (cbRequired > getGrowthThreshold());
            allocateBuffer();
            }
        }

    /**
    * Request that the buffer be shrunk based on the number of bytes
    * currently required.
    *
    * @param cbRequired  the number of contiguous bytes in the buffer,
    *                    starting from offset 0, that are actually in use
    */
    public void shrink(int cbRequired)
        {
        if (cbRequired < getShrinkageThreshold())
            {
            int cbNew  = getCapacity();
            do
                {
                cbNew = Math.max(cbNew / 2, getMinCapacity());
                setCapacity(cbNew);
                }
            while (cbRequired < getShrinkageThreshold());
            allocateBuffer();
            }
        }


    // ----- internal -------------------------------------------------------

    /**
    * Determine the grow and shrink thresholds based on the current capacity.
    */
    protected void calibrate()
        {
        int cb = getCapacity();

        setGrowthThreshold   (cb >= getMaxCapacity() ? Integer.MAX_VALUE
                : Math.min((int) (cb * 0.90), getMaxCapacity()));
        setShrinkageThreshold(cb <= getMinCapacity() ? 0
                : Math.max((int) (cb * 0.40), getMinCapacity()));
        }

    /**
    * Allocate a new buffer, copying old data if there is any.
    */
    protected abstract void allocateBuffer();


    // ----- data members ---------------------------------------------------

    /**
    * Initial (and minimum) size of the managed buffer.
    */
    private int         m_cbInitial;

    /**
    * Maximum size of the managed buffer.
    */
    private int         m_cbMaximum;

    /**
    * Current size of the managed buffer.
    */
    private int         m_cbCurrent;

    /**
    * Number of bytes in use at which the buffer should grow.
    */
    private int         m_cbGrow;

    /**
    * Number of bytes in use at which the buffer should shrink.
    */
    private int         m_cbShrink;

    /**
    * The current buffer.
    */
    private ByteBuffer  m_buffer;
    }
