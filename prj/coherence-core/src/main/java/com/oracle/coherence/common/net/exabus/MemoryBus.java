/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.net.exabus;


import com.oracle.coherence.common.io.BufferSequence;
import com.oracle.coherence.common.io.Buffers;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;


/**
 * A MemoryBus provides access to regions of remotely accessible memory.
 * <p>
 * The MemoryBus intentionally provides a very limited number of operations,
 * leaving higher level functions such as synchronization to be externally
 * implemented via mechanisms such as {@link MessageBus messaging}.
 *
 * @author mf/cp  2010.10.06
 */
public interface MemoryBus
    extends Bus
    {
    /**
     * Specify the BufferSequence which this MemoryBus will host.
     * <p>
     * If the BufferSequence contains multiple ByteBuffers then all but the last
     * buffer must {@link ByteBuffer#position start} and {@link ByteBuffer#limit end} on
     * an eight byte word boundary.
     * </p>
     * The sequence if specified must be provided before {@link #open opening} the bus.
     * <p>
     * It is the responsibility of the caller to {@link BufferSequence#dispose dispose} of
     * the sequence once the bus has been {@link Event.Type#CLOSE closed}.
     * </p>
     *
     * @param bufseq  the buffer sequence to host or null
     *
     * @see Buffers#allocateDirect
     */
    public void setBufferSequence(BufferSequence bufseq);

    /**
     * Return the BufferSequence representing the locally hosted memory.
     *
     * @return the locally hosted BufferSequence or null if nothing is hosted
     */
    public BufferSequence getBufferSequence();

    /**
     * Return the capacity of a peer's hosted memory in bytes.
     * <p>
     * The EndPoint's bus region is <tt>[0 .. capacity)</tt>.
     *
     * @param peer  the target EndPoint, or null for the local capacity
     *
     * @return the peer's capacity
     *
     * @throws IllegalArgumentException if the peer is unknown to the Bus
     */
    public long getCapacity(EndPoint peer);

    /**
     * Request a read from the peer's memory into the supplied BufferSequence.
     * <p>
     * Upon {@link Event.Type#RECEIPT completion} of the asynchronous
     * operation the supplied BufferSequence will contain a copy of the peer's
     * memory segment.
     *
     * @param peer     the target EndPoint to read from
     * @param offset   the offset into the peer's memory to start reading from
     * @param bufseq   the buffers to write to
     * @param receipt  the optional operation receipt
     *
     * @throws IllegalArgumentException if the peer is unknown to the Bus
     * @throws IndexOutOfBoundsException if the offset is negative, or the
     *         <tt>offset+{@link BufferSequence#getLength bufseq.getLength()}</tt>
     *         is greater than the {@link #getCapacity peer's capacity}
     */
    public void read(EndPoint peer, long offset, BufferSequence bufseq,
            Object receipt);

    /**
     * Request a write into the peer's memory from the specified BufferSequence.
     * <p>
     * Upon {@link Event.Type#RECEIPT completion} of the asynchronous
     * operation the peer's memory segment will have been updated with the
     * contents of the supplied BufferSequence.
     *
     * @param peer     the target EndPoint to write to
     * @param offset   the offset into the peer's memory to start writing to
     * @param bufseq   the buffers to read from
     * @param receipt  the optional operation receipt
     *
     * @throws IllegalArgumentException if the peer is unknown to the Bus
     * @throws IndexOutOfBoundsException if the offset is negative, or the
     *         <tt>offset+{@link BufferSequence#getLength bufseq.getLength()}</tt>
     *         is greater than the {@link #getCapacity peer's capacity}
     */
    public void write(EndPoint peer, long offset, BufferSequence bufseq,
            Object receipt);

    /**
     * Request an atomic compare and swap (CAS) operation on an eight byte
     * word in the peer's memory.
     * <p>
     * The CAS operation may only be performed at a word-aligned offset.
     * As the hosted memory region itself is word-aligned any offset where
     * <tt>offset % 8 == 0</tt> is known to be word-aligned.
     * <p>
     * The endianness of the underlying remote bytes is dependent upon the
     * peer's environment. The Bus pair will handle any necessary conversion
     * of the in/out <tt>long</tt> values. The endianness only becomes
     * relevant in the case where the same memory region is accessed with
     * via other means such as {@link #read}, {@link #write}, or {@link
     * #getBufferSequence()}, as those methods will not perform any endian
     * conversion.
     * <p>
     * Upon {@link Event.Type#RECEIPT completion} of the asynchronous
     * operation the result holder will contain the value of peer's word at
     * the time the compare was performed. Success of the completed CAS can be
     * identified by comparing the value in the result holder against the
     * supplied expected value. If these values are equal then the swap
     * occurred and the peer's memory was updated, otherwise the peer's memory was not updated.
     *
     * @param peer     the target EndPoint
     * @param offset   the offset into the peer's memory
     * @param expect   the expected value
     * @param update   the new value
     * @param result   the holder for the deferred result
     * @param receipt  the optional operation receipt
     *
     * @throws IllegalArgumentException if the peer is unknown to the Bus
     * @throws IllegalArgumentException if the offset is not suitable for use
     *         in a CAS operation on this bus for example is not on a word
     *         boundary
     * @throws IndexOutOfBoundsException if the offset is negative, or the
     *         <tt>offset+8</tt> is greater than the {@link #getCapacity
     *         peer's capacity}
     */
    public void compareAndSwap(EndPoint peer, long offset, long expect,
            long update, AtomicLong result, Object receipt);

    /**
     * Request an atomic increment on an eight byte word on the peer.
     * <p>
     * The atomic increment operation has the same endianness and word-alignment
     * issues as described for the {@link #compareAndSwap CAS} operation.
     * <p>
     * Upon {@link Event.Type#RECEIPT completion} of the asynchronous operation
     * the peer's memory segment will have been incremented, and the result
     * holder will contain the (pre-incremented) prior value.
     *
     * @param peer     the target EndPoint
     * @param offset   the offset into the peer's memory
     * @param delta    the amount to increment by (may be negative)
     * @param result   the holder for the deferred result
     * @param receipt  the optional operation receipt
     *
     * @throws IllegalArgumentException if the peer is unknown to the Bus
     * @throws IllegalArgumentException if the offset is not suitable for use
     *         in a CAS operation on this bus for example is not on a word
     *         boundary
     * @throws IndexOutOfBoundsException if the offset is negative, or the
     *         <tt>offset+8</tt> is greater than the {@link #getCapacity
     *         peer's capacity}
     */
    public void getAndAdd(EndPoint peer, long offset, long delta,
            AtomicLong result, Object receipt);

    /**
     * Signal a peer.
     * <p>
     * Upon {@link Event.Type#RECEIPT completion} of the asynchronous
     * operation it is guaranteed that the peer will eventually have a
     * {@link Event.Type#SIGNAL SIGNAL} event emitted to its event collector.
     *
     * @param peer     the target EndPoint
     * @param lContent the {@link Event#getContent() content}
     *                 to provide in the SIGNAL event as a {@link Number}
     * @param receipt  the operation receipt
     *
     * @throws IllegalArgumentException if the peer is unknown to the Bus
     */
    public void signal(EndPoint peer, long lContent, Object receipt);
    }
