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
 * BufferSequence represents a series of ByteBuffers.
 * <p>
 * Positional changes to a returned ByteBuffer will not change the sequence,
 * whereas data changes made to a {@link ByteBuffer#isReadOnly mutable}
 * ByteBuffer will change the sequence. The result of these requirements dictates
 * that a BufferSequence not return the same ByteBuffer instance more then
 * once for a given index. If an index is requested more than once then
 * an equivalent ByteBuffer must be returned, in the case of mutable buffers
 * this implies that a {@link ByteBuffer#duplicate duplicate} is returned.
 *
 * @author mf/gg/cp  2010.10.13
 */
public interface BufferSequence
        extends Disposable
    {
    /**
     * Return the byte length of the sequence.
     *
     * @return the byte length of the sequence
     */
    public long getLength();

    /**
     * Return the number of ByteBuffers contained in the sequence.
     *
     * @return the number of ByteBuffers contained in the sequence
     */
    public int getBufferCount();

    /**
     * Return the ByteBuffer for a given index.
     *
     * @param iBuffer  the zero based offset into the sequence
     *
     * @return a {@link ByteBuffer#duplicate duplicate} of the ByteBuffer at
     *         the specified index
     *
     * @throws IndexOutOfBoundsException if iBuffer is not in
     *         [0 .. {@link #getLength})
     */
    public ByteBuffer getBuffer(int iBuffer);

    /**
     * Return an unsafe ByteBuffer for a given index.
     * <p>
     * The positional attributes of the returned buffer may or may not reflect
     * the proper ones for the given buffer.  The caller must not modify these
     * attributes, and must be prepared for them to be changed.  The correct
     * values can be obtained via {@link #getBufferPosition} and {@link #getBufferLimit(int)}.
     *
     * @param iBuffer  the zero based offset into the sequence
     *
     * @return an unsafe ByteBuffer at the specified index
     *
     * @throws IndexOutOfBoundsException if iBuffer is not in
     *         [0 .. {@link #getLength})
     */
    public ByteBuffer getUnsafeBuffer(int iBuffer);

    /**
     * Return the length of the ByteBuffer at a given index.
     *
     * @param iBuffer  the zero based offset into the sequence
     *
     * @return the buffer length
     *
     * @throws IndexOutOfBoundsException if iBuffer is not in
     *         [0 .. {@link #getLength})
     */
    public int getBufferLength(int iBuffer);

    /**
     * Return the position of the ByteBuffer at a given index.
     *
     * @param iBuffer  the zero based offset into the sequence
     *
     * @return the buffer position
     *
     * @throws IndexOutOfBoundsException if iBuffer is not in
     *         [0 .. {@link #getLength})
     */
    public int getBufferPosition(int iBuffer);

    /**
     * Return the limit of the ByteBuffer at a given index.
     *
     * @param iBuffer  the zero based offset into the sequence
     *
     * @return the buffer limit
     *
     * @throws IndexOutOfBoundsException if iBuffer is not in
     *         [0 .. {@link #getLength})
     */
    public int getBufferLimit(int iBuffer);

    /**
     * Return an array of ByteBuffers representing the sequence.
     *
     * @return a new array of ByteBuffer {@link ByteBuffer#duplicate duplicates}
     *         representing the sequence
     */
    public ByteBuffer[] getBuffers();

    /**
     * Copy ByteBuffer {@link ByteBuffer#duplicate duplicates} into the
     * supplied array.
     *
     * @param iBuffer   the index within the sequence at which to start copying
     * @param cBuffers  the number of buffers to copy
     * @param abufDest  the destination array
     * @param iDest     the start index in the destination array
     *
     * @throws IndexOutOfBoundsException if either
     *         <tt>[iBuffer .. iBuffer+cBuffers)</tt> is outside of the
     *         sequence range, or if <tt>[iDest .. iDest+cBuffers)</tt>
     *         is outside of the destination array range
     */
    public void getBuffers(int iBuffer, int cBuffers, ByteBuffer[] abufDest, int iDest);
    }
