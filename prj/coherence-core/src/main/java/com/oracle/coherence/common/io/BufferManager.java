/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.io;


import com.oracle.coherence.common.base.Disposable;

import java.nio.ByteBuffer;


/**
 * BufferManager defines a mechanism for efficient buffer re-use.
 *
 * @author mf  2010.12.02
 */
public interface BufferManager
        extends Disposable
    {
    /**
     * Acquire a free ByteBuffer.
     * <p>
     * The returned buffer will be at least the requested size. If a larger
     * buffer is returned the limit will have been pre-set to <tt>cbMin</tt>,
     * though the entire capacity is available to the caller.
     *
     * @param cbMin  the minimal required size
     *
     * @return the ByteBuffer with {@link ByteBuffer#remaining} equal to
     *         <tt>cbMin</tt>
     *
     * @throws OutOfMemoryError if the request cannot be immediately satisfied
     */
    public ByteBuffer acquire(int cbMin);

    /**
     * Acquire a free ByteBuffer, of any size.
     * <p>
     * The intended use of this method is to allow the buffer manager to
     * satisfy a large memory request of a known size over a series of
     * allocations.  The caller may need to chain a series of buffers together
     * to ultimately fulfill their required buffer size.
     * <p>
     * If a larger buffer is returned the limit will have been pre-set such to
     * <tt>cbPref</tt>, though the entire capacity is available to the caller.
     *
     * @param cbPref  the preferred size
     *
     * @return the ByteBuffer with {@link ByteBuffer#remaining} less than or
     *         equal to <tt>cbPref</tt>
     *
     * @throws OutOfMemoryError if the request cannot be immediately satisfied
     */
    public ByteBuffer acquirePref(int cbPref);

    /**
     * Acquire a free ByteBuffer, of any size.
     * <p>
     * The intended use of this method is to allow the buffer manager to
     * satisfy the allocation of a potentially large but unknown size memory
     * request, for instance the serialization of a complex object.
     *
     * @param cbSum  the running total of prior acquisitions
     *
     * @return the ByteBuffer of any size
     *
     * @throws OutOfMemoryError if the request cannot be immediately satisfied
     */
    public ByteBuffer acquireSum(int cbSum);

    /**
     * Truncate a formerly allocated buffer, returning a buffer whose size
     * more closely matches the amount of space used (as indicated by {@link
     * ByteBuffer#remaining}) in the specified buffer.
     * <p>
     * The returned buffer will have the same number of remaining bytes as
     * the original, and those bytes will have the same content.
     * <p>
     * If a new buffer is returned the old buffer will have been automatically
     * released to the manager.
     *
     * @param buff  the buffer to truncate
     *
     * @return a new buffer, or the same buffer if no exchange was deemed
     *         necessary.
     *
     * @throws IllegalArgumentException may be thrown if the specified buffer
     *         was not acquired from this BufferManager
     */
    public ByteBuffer truncate(ByteBuffer buff);

    /**
     * Release a formerly acquired ByteBuffer.
     *
     * @param buff  the buffer
     *
     * @throws IllegalArgumentException may be thrown if the specified buffer
     *         was not acquired from this BufferManager
     */
    public void release(ByteBuffer buff);

    /**
     * Return the maximum capacity (in bytes) for this BufferManager, or Long.MAX_VALUE if it has no limit.
     *
     * @return the maximum capacity
     */
    public long getCapacity();
    }
