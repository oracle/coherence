/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.nio;


import java.nio.ByteBuffer;


/**
* An interface for managing a ByteBuffer.
*
* @author cp  2002.09.16
*
* @since Coherence 2.2
*/
public interface ByteBufferManager
    {
    /**
    * Get the current ByteBuffer reference.
    *
    * @return the current ByteBuffer
    */
    public ByteBuffer getBuffer();

    /**
    * Get the capacity of the current ByteBuffer. This is the same as
    * <code>getBuffer().capacity()</code>.
    *
    * @return the capacity of the current ByteBuffer
    */
    public int getCapacity();

    /**
    * Determine the level (what number of bytes used) above which the current
    * ByteBuffer will need to be "resized" upwards. Returns Integer.MAX_VALUE
    * if the buffer cannot be resized upwards.
    *
    * @return the number of bytes that, when the number of used bytes exceeds
    *         it, the ByteBuffer will need to grow
    */
    public int getGrowthThreshold();

    /**
    * Determine the level (what number of bytes used) below which the current
    * ByteBuffer will need to be "resized" downwards. Returns 0 if the buffer
    * cannot be resized downwards.
    *
    * @return the number of bytes that, when the number of used bytes drops
    *         below it, the ByteBuffer will need to shrink
    */
    public int getShrinkageThreshold();

    /**
    * Determine the minimum size that the managed buffer can reach. If the
    * buffer is already at its minimum, then this method will return the
    * same value as <code>getCapacity</code>.
    *
    * @return minimum size for the managed buffer
    */
    public int getMinCapacity();

    /**
    * Determine the maximum size that the managed buffer can reach. If the
    * buffer is already at its maximum, then this method will return the
    * same value as <code>getCapacity</code>.
    *
    * @return maximum size for the managed buffer
    */
    public int getMaxCapacity();

    /**
    * Request that the buffer be grown based on the number of bytes
    * currently required.
    *
    * @param cbRequired  the number of bytes that are needed by the
    *                    requesting operation
    */
    public void grow(int cbRequired);
    
    /**
    * Request that the buffer be shrunk based on the number of bytes
    * currently required.
    *
    * @param cbRequired  the number of contiguous bytes in the buffer,
    *                    starting from offset 0, that are actually in use
    */
    public void shrink(int cbRequired);
    }
