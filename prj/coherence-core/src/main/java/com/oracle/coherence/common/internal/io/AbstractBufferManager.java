/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.internal.io;


import com.oracle.coherence.common.io.BufferManager;

import java.nio.ByteBuffer;


/**
 * AbstractBufferManager provides a skeletal implementation of the BufferManager
 * interface.
 *
 * @author mf 2010.12.03
 */
public abstract class AbstractBufferManager
        implements BufferManager
    {
    // ----- BufferManager interface ----------------------------------------

    /**
     * {@inheritDoc}
     * <p>
     * The AbstractBufferManager implementation will attempt to allocate the
     * specified size, but if this does not succeed, it will attempt to
     * allocate the size specified by {@link #getPreferredUnitSize}, and
     * in all else fails by {@link #getPreferredUnitSize}.
     */
    public ByteBuffer acquirePref(int cbPref)
        {
        if (cbPref <= getMaximumUnitSize())
            {
            try
                {
                return acquire(cbPref);
                }
            catch (OutOfMemoryError e) {}
            }

        try
            {
            return acquire(getPreferredUnitSize());
            }
        catch (OutOfMemoryError e) {}

        return acquire(getMinimumUnitSize());
        }

    /**
     * {@inheritDoc}
     * <p>
     * The AbstractBufferManager implementation will attempt to double the
     * total amount of buffer space, i.e. it will return a value close to
     * <tt>cbSum</tt>.  If cbSum is zero, the returned size will be determined
     * by {@link #getMinimumUnitSize}.
     */
    public ByteBuffer acquireSum(int cbSum)
        {
        return (ByteBuffer) acquirePref(Math.max(getMinimumUnitSize(), cbSum))
                .clear();
        }

    /**
     * {@inheritDoc}
     */
    public ByteBuffer truncate(ByteBuffer buff)
        {
        ensureCompatibility(buff);

        int cbUsed = buff.remaining();
        if (isUnderUtilized(buff))
            {
            // resize the buffer
            ByteBuffer buffNew;
            try
                {
                buffNew = acquire(cbUsed);
                }
            catch (OutOfMemoryError e)
                {
                return buff; // use the original
                }

            buffNew.put(buff).flip();
            release(buff);
            return buffNew;
            }
        return buff;
        }

    /**
     * {@inheritDoc}
     * <p>
     * The AbstractBufferManager implementation only ensures that the
     * compatibility of the supplied buffer, it does not actually reclaim it.
     */
    public void release(ByteBuffer buff)
        {
        ensureCompatibility(buff);
        }


    // ----- helpers --------------------------------------------------------

    /**
     * Return the minimum allocation size.
     * <p>
     * This value determines the minimum size for {@link #acquireSum}.
     * <p>
     * AbstractBufferManager always return 1KB.
     *
     * @return the byte size
     */
    protected int getMinimumUnitSize()
        {
        return 1024;
        }

    /**
     * Return the preferred unit size.
     * <p>
     * This value determines the default size for {@link #acquirePref} if the
     * specified amount cannot be satisfied.
     * <p>
     * AbstractBufferManager always return 64KB.
     *
     * @return the preferred unit size
     */
    protected int getPreferredUnitSize()
        {
        return 64 * 1024;
        }

    /**
     * Return the maximum allocation size.
     * <p>
     * This value determines the maximum size for {@link #acquire}.
     * <p>
     * AbstractBufferManager always return 2GB.
     *
     * @return the preferred unit size
     */
    protected int getMaximumUnitSize()
        {
        return Integer.MAX_VALUE;
        }

    /**
     * Identify if the specified buffer is under utilized.
     * <p>
     * This is used in determining if a buffer should be truncated.
     * <p>
     * AbstractBufferManager returns true if the supplied buffer is less then
     * 12% utilized, and the capacity is greater the {@link #getMinimumUnitSize}.
     *
     * @param buff  the buffer
     *
     * @return  true iff the buffer is considered under utilized
     */
    protected boolean isUnderUtilized(ByteBuffer buff)
        {
        int cbCap  = buff.capacity();
        return cbCap > getMinimumUnitSize() && buff.remaining() < cbCap >>> 3;
        }

    /**
     * Ensure that the specified buffer is compatible with this manager.
     * <p>
     * The AbstractBufferManager implementation is a no-op.
     *
     * @param buff  the buffer to ensure.
     *
     * @throws IllegalArgumentException if the buffer is incompatible
     */
    protected void ensureCompatibility(ByteBuffer buff)
        {
        }

    @Override
    public long getCapacity()
        {
        return Long.MAX_VALUE;
        }
    }
