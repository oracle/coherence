/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.io;


import com.oracle.coherence.common.net.exabus.MemoryBus;
import com.oracle.coherence.common.base.Collector;


import java.nio.ByteBuffer;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.util.LinkedList;
import java.util.List;

import java.util.concurrent.atomic.AtomicLong;

import java.util.zip.CRC32;


/**
 * Buffers contains a number of Buffer related helpers.
 *
 * @author mf 2010.12.08
 */
public class Buffers
    {
    /**
     * Return an empty heap based ByteBuffer.
     *
     * @return an empty heap based ByteBuffer
     */
    public static ByteBuffer getEmptyBuffer()
        {
        return EmptyBuffer.INSTANCE;
        }

    /**
     * Return an empty direct ByteBuffer.
     *
     * @return an empty direct ByteBuffer
     */
    public static ByteBuffer getEmptyDirectBuffer()
        {
        return EmptyDirectBuffer.INSTANCE;
        }

    /**
     * Return the empty ByteBuffer array singleton.
     *
     * @return the empty ByteBuffer array singleton
     */
    public static ByteBuffer[] getEmptyBufferArray()
        {
        return EmptyBufferArray.INSTANCE;
        }

    /**
     * Return the empty BufferSequence singleton.
     *
     * @return the empty BufferSequence singleton
     */
    public static BufferSequence getEmptyBufferSequence()
        {
        return EmptySequenceHolder.INSTANCE;
        }

    /**
     * Allocate a direct ByteBuffer based BufferSequence of the specified size.
     * <P>
     * Note: The BufferSequence will be word aligned as required by
     * {@link MemoryBus#setBufferSequence(BufferSequence)}
     * </P>
     *
     * @param mgr  the buffer manager to acquire from, or null to allocate via {@link ByteBuffer#allocateDirect(int)}
     * @param cb   the required size
     *
     * @return  the BufferSequence
     */
    public static BufferSequence allocateDirect(BufferManager mgr, long cb)
        {
        if (cb == 0)
            {
            return new SingleBufferSequence(null, Buffers.getEmptyDirectBuffer());
            }

        final int        cbMaxAlligned = Integer.MAX_VALUE - 7;
        int              cbAttempt     = cb > cbMaxAlligned ? cbMaxAlligned : (int) cb;
        List<ByteBuffer> listBuffers   = null;
        ByteBuffer       buffer        = null;
        for (long ofLocal = 0; ofLocal < cb; )
            {
            try
                {
                if (mgr == null)
                    {
                    buffer = ByteBuffer.allocateDirect(cbAttempt);
                    }
                else
                    {
                    buffer    = mgr.acquirePref(cbAttempt);
                    cbAttempt = buffer.remaining();

                    if (!buffer.isDirect())
                        {
                        mgr.release(buffer);
                        if (listBuffers != null)
                            {
                            for (ByteBuffer bufRelease : listBuffers)
                                {
                                mgr.release(bufRelease);
                                }
                            }
                        throw new IllegalArgumentException("BufferManager must supply direct ByteBuffers");
                        }

                    if (ofLocal + cbAttempt < cb)
                        {
                        // take full buffer if a larger was returned
                        if (cbAttempt < buffer.limit())
                            {
                            buffer.limit(buffer.limit());
                            cbAttempt = buffer.remaining();
                            }

                        // ensure word alignment
                        if (cbAttempt % 8 != 0)
                            {
                            buffer.limit(buffer.limit() - (cbAttempt % 8));
                            cbAttempt = buffer.remaining();
                            }
                        }
                    }

                ofLocal += cbAttempt;
                cbAttempt = (int) Math.min((long) Math.max(1024 * 1024, cbAttempt), cb - ofLocal);
                if (listBuffers == null)
                    {
                    if (ofLocal < cb)
                        {
                        listBuffers = new LinkedList<ByteBuffer>();
                        listBuffers.add(buffer);
                        }
                    // else; single BufferSequence
                    }
                else
                    {
                    listBuffers.add(buffer);
                    }
                }
            catch (OutOfMemoryError e)
                {
                if (cbAttempt <= 1024)
                    {
                    // time to give up
                    if (listBuffers != null)
                        {
                        for (ByteBuffer bufRelease : listBuffers)
                            {
                            mgr.release(bufRelease);
                            }
                        }

                    throw e;
                    }
                cbAttempt /= 2;
                if ((cbAttempt % 8) != 0)
                    {
                    // for MemoryBus CAS and atomic ADD we must ensure that every block (except the last one) is divisible by 8.
                    cbAttempt += (8 - (cbAttempt % 8));
                    }
                }
            }

        if (mgr != null)
            {
            buffer = mgr.truncate(buffer);
            if (listBuffers != null)
                {
                listBuffers.set(listBuffers.size() -1, buffer);
                }
            }

        return listBuffers == null
                ? new SingleBufferSequence(mgr, buffer)
                : new MultiBufferSequence(mgr, listBuffers.toArray(new ByteBuffer[listBuffers.size()]));
        }

    /**
     * Allocate a series of BufferSequences and add them to the specified collector.
     *
     * @param mgr         the buffer manager to acquire from
     * @param collBufSeq  the collector to add each sequence to, {@link Collector#flush flush} will not be invoked
     * @param cSeq        the desired number of sequences
     * @param cb          the size of each BufferSequence
     * @param cBufLimit   the maximum number of ByteBuffers per sequence
     *
     * @return the number of allocated sequences, may be larger or smaller then the requested count
     */
    public static int allocate(final BufferManager mgr, final Collector<BufferSequence> collBufSeq, final int cSeq, final int cb, final int cBufLimit)
        {
        if (cSeq <= 0)
            {
            return 0;
            }
        else if (cBufLimit <= 0)
            {
            throw new IllegalArgumentException("cBufLimit must be > zero");
            }
        else if (cb <= 0)
            {
            throw new IllegalArgumentException("cb must be > zero");
            }

        int        cSeqAlloc = 0;
        AtomicLong atlShared = null;
        ByteBuffer bufShared = null;
        final int  cbWaste   = cb / 64; // tolerate at most this much waste per sequence; waste a little to avoid buffer sharing
        BufferSequence bufferSeq = null;

        try
            {
            // allocate at least the requested number of sequences, but more iff not doing so would be wasteful
            // note we may allocate extra sequences, but only as many as will fit in the last allocation, allocating
            // more then that can result in a much greater allocation then the user had expected and can hurt performance
            for (int i = 0; i < cSeq || (bufShared != null && bufShared.remaining() >= cb); ++i)
                {
                for (int iBuf = 0, cbReq = cb; cbReq > 0 && iBuf < cBufLimit; ++iBuf)
                    {
                    BufferSequence bufSeqNew = null;
                    if (bufShared == null)
                        {
                        ByteBuffer buf = iBuf == cBufLimit - 1
                              ? mgr.acquire(cbReq)      // last buffer in sequence, get all
                              : mgr.acquirePref(cbReq); // TODO: consider allocating enough for cSeq?

                        if (buf.capacity() - cbReq > cbWaste)
                            {
                            // share this new buffer with other sequences
                            bufShared = buf;
                            atlShared = new AtomicLong(1); // initial count is for this method

                            // fall through to shared; position/limit already set appropriately
                            }
                        else
                            {
                            // non-shared buffer, mostly only used for intermediate buffers in the sequence
                            // or for close fit allocations, i.e. avoid cost of sharing when memory savings is minimal
                            bufSeqNew = new SingleBufferSequence(mgr, buf);
                            }
                        }
                    else // use next (portion) of shared buffer
                        {
                        // update position and limit based on cbReq
                        bufShared.limit(Math.min(bufShared.capacity(), bufShared.position() + cbReq));
                        }

                    if (bufSeqNew == null) // use shared buffer
                        {
                        final ByteBuffer bufDispose = bufShared;
                        final AtomicLong atlDispose = atlShared;

                        if (atlDispose.incrementAndGet() <= 0)
                            {
                            throw new IllegalStateException();
                            }

                        bufSeqNew = new SingleBufferSequence(null, bufDispose.slice())
                            {
                            @Override
                            public void dispose()
                                {
                                super.dispose();
                                safeRelease(mgr, bufDispose, atlDispose);
                                }
                            };

                        // prep bufShared for next sequence (if possible)
                        bufShared.position(bufShared.limit()).limit(bufShared.capacity());
                        if (bufShared.remaining() == 0 || (cBufLimit == 1 && bufShared.remaining() < cb))
                            {
                            safeRelease(mgr, bufShared, atlShared);
                            bufShared = null;
                            atlShared = null;
                            }
                        }

                    // rather then further complicating this code with MultiBufferSequence we make use of layers
                    // of CompositeBufferSequences assuming the total depth will be quite low, note that single
                    // buffer sequences naturally avoid creating a composite
                    bufferSeq = bufferSeq == null ? bufSeqNew : new CompositeBufferSequence(bufferSeq, bufSeqNew);
                    cbReq    -= bufSeqNew.getLength();
                    }

                collBufSeq.add(bufferSeq);
                bufferSeq = null; // mark as consumed
                ++cSeqAlloc;
                }
            }
        finally
            {
            if (atlShared != null)
                {
                safeRelease(mgr, bufShared, atlShared);
                }
            if (bufferSeq != null)
                {
                bufferSeq.dispose();
                }
            }

        return cSeqAlloc;
        }

    /**
     * Releasse a shared buffer.
     *
     * @param mgr        the manager to release to
     * @param bufShared  the shared buffer
     * @param counter    the reference count
     */
    private static void safeRelease(BufferManager mgr, ByteBuffer bufShared, AtomicLong counter)
        {
        long c = counter.decrementAndGet();
        if (c == 0)
            {
            mgr.release(bufShared);
            }
        else if (c < 0)
            {
            throw new IllegalStateException();
            }
        }

    /**
     * Construct a BufferSequence from a single ByteBuffer.
     *
     * @param manager  the BufferManager responsible for the ByteBuffer
     * @param buffer   the buffer
     *
     * @return a BufferSequence around the supplied buffer
     */
    public static BufferSequence createBufferSequence(BufferManager manager, ByteBuffer buffer)
        {
        return new SingleBufferSequence(manager, buffer);
        }

    /**
     * Construct a BufferSequence from two ByteBuffers.
     *
     * @param manager  the BufferManager responsible for the ByteBuffers
     * @param bufferA  the first buffer
     * @param bufferB  the second buffer
     *
     * @return a BufferSequence around the supplied buffers
     */
    public static BufferSequence createBufferSequence(BufferManager manager, ByteBuffer bufferA, ByteBuffer bufferB)
        {
        return new DoubleBufferSequence(manager, bufferA, bufferB);
        }

    /**
     * Construct a BufferSequence from two ByteBuffers.
     *
     * @param manager  the BufferManager responsible for the ByteBuffers
     * @param bufferA  the first buffer
     * @param bufferB  the second buffer
     * @param bufferC  the third buffer
     *
     * @return a BufferSequence around the supplied buffers
     */
    public static BufferSequence createBufferSequence(BufferManager manager, ByteBuffer bufferA, ByteBuffer bufferB, ByteBuffer bufferC)
        {
        return new TripleBufferSequence(manager, bufferA, bufferB, bufferC);
        }

    /**
     * Construct a BufferSequence from an array of ByteBuffers.
     *
     * @param manager  the BufferManager responsible for the ByteBuffers
     * @param aBuffer  the ByteBuffer array
     *
     * @return a BufferSequence around the supplied buffers
     */
    public static BufferSequence createBufferSequence(BufferManager manager, ByteBuffer ... aBuffer)
        {
        switch (aBuffer.length)
            {
            case 0:
                return Buffers.getEmptyBufferSequence();
            case 1:
                return new SingleBufferSequence(manager, aBuffer[0]);
            case 2:
                return new DoubleBufferSequence(manager, aBuffer[0], aBuffer[1]);
            case 3:
                return new TripleBufferSequence(manager, aBuffer[0], aBuffer[1], aBuffer[2]);
            default:
                return new MultiBufferSequence(manager, aBuffer);
            }
        }

    /**
     * Return a ByteBuffer slice of the specified BufferSequence
     *
     * @param bufseq  the sequence to slice
     * @param of      the starting byte offset of the slice
     * @param cb      the length of the slice
     *
     * @return  the buffer slice
     *
     * @throws IllegalArgumentException if the requested region is outside the bounds of the BufferSequence
     */
    public static ByteBuffer slice(BufferSequence bufseq, long of, int cb)
        {
        if (of < 0 || cb < 0)
            {
            throw new IllegalArgumentException();
            }

        for (int i = 0, c = bufseq.getBufferCount(); i < c; ++i)
            {
            ByteBuffer buffer = bufseq.getBuffer(i);
            int        cbBuf  = buffer.remaining();
            if (of - cbBuf <= 0)
                {
                // this buffer contains the region to be sliced
                buffer.position((int) of).limit(cb);
                return buffer.slice();
                }
            of -= cbBuf;
            }

        throw new IllegalArgumentException();
        }

    /**
     * Copy the contents from one BufferSequence to another as space permits.
     *
     * @param bufseqSrc  the source sequence
     * @param bufseqDest the destination sequence
     */
    public static void copy(BufferSequence bufseqSrc, BufferSequence bufseqDest)
        {
        ByteBuffer bufSrc = getEmptyBuffer();
        ByteBuffer bufDst = getEmptyBuffer();

        for (int iSrc = 0, iDst = 0, cSrc = bufseqSrc.getBufferCount(), cDst = bufseqDest.getBufferCount(); iSrc < cSrc; )
            {
            if (!bufSrc.hasRemaining())
                {
                bufSrc = bufseqSrc.getBuffer(iSrc++);
                }

            while (!bufDst.hasRemaining())
                {
                if (iDst == cDst)
                    {
                    return;
                    }
                bufDst = bufseqDest.getBuffer(iDst++);
                }

            bufDst.put(bufSrc);
            }
        }

    /**
     * Copy a portion of the source buffer into the destination buffer
     *
     * @param src    the source buffer, note the position will not be updated
     * @param ofSrc  the position at which to start the copy
     * @param cb     the number of bytes to copy
     * @param dst    the buffer to copy into, note the position will be updated
     */
    public static void copy(ByteBuffer src, int ofSrc, int cb, ByteBuffer dst)
        {
        if (src.hasArray()) // common case for HeapByteBuffers, or mixed heap & direct
            {
            dst.put(src.array(), src.arrayOffset() + ofSrc, cb);
            }
        else if (cb > 128) // assumed most efficient for large DirectByteBuffer copies
            {
            // only produce garbage for "large" copies, note garbage is small
            src = src.duplicate();
            src.position(ofSrc).limit(ofSrc + cb);
            dst.put(src);
            }
        else if (src.order() == dst.order()) // common case for DirectByteBuffers, doesn't produce garbage
            {
            // copy in chunks
            int nPos = dst.position();
            for (; cb >= 8; cb -= 8, nPos += 8, ofSrc += 8)
                {
                dst.putLong(nPos, src.getLong(ofSrc));
                }

            if (cb >= 4)
                {
                dst.putInt(nPos, src.getInt(ofSrc));
                cb    -= 4;
                nPos  += 4;
                ofSrc += 4;
                }

            if (cb >= 2)
                {
                dst.putShort(nPos, src.getShort(ofSrc));
                cb    -= 2;
                nPos  += 2;
                ofSrc += 2;
                }

            if (cb > 0)
                {
                dst.put(nPos, src.get(ofSrc));
                }

            dst.position(nPos);
            }
        else // rare, small copy between buffers with different endian order
            {
            while (cb-- > 0)
                {
                dst.put(src.get(ofSrc++));
                }
            }
        }

    /**
     * Compare two BufferSequences for byte sequence equality.
     *
     * @param bufseqA  the first BufferSequence
     * @param bufseqB  the second BufferSequence
     *
     * @return true iff the two sequences represent the same sequence of bytes
     */
    public static boolean equals(BufferSequence bufseqA, BufferSequence bufseqB)
        {
        if (bufseqA == bufseqB)
            {
            return true;
            }
        else if (bufseqA == null || bufseqB == null)
            {
            return false;
            }

        long cb = bufseqA.getLength();
        if (cb == bufseqB.getLength())
            {
            if (bufseqA.getBufferCount() == 1 && bufseqB.getBufferCount() == 1)
                {
                return bufseqA.getBuffer(0).equals(bufseqB.getBuffer(0));
                }

            InputStream streamA = new BufferSequenceInputStream(bufseqA);
            InputStream streamB = new BufferSequenceInputStream(bufseqB);

            try
                {
                for (; cb > 0 && streamA.read() == streamB.read(); --cb)
                    {}
                return cb == 0;
                }
            catch (IOException e)
                {
                // should not be possible, we already verified that they have
                // the same length
                return false;
                }
            finally
                {
                try
                    {
                    streamA.close();
                    streamB.close();
                    }
                catch (IOException e) {}
                }
            }
        return false;
        }

    /**
     * Return true if the specified portions of the buffers are equal.
     *
     * @param bufA  the first buffer to compare
     * @param ofA   the starting offset of the first buffer
     * @param bufB  the second buffer to compare
     * @param ofB   the starting offset of the second buffer
     * @param cb    the number of bytes to compare
     *
     * @return true if the ranges are equal
     *
     * @throws  IndexOutOfBoundsException if either buffer's length is insufficient for the comparison to be performed
     */
    public static boolean equals(ByteBuffer bufA, int ofA, ByteBuffer bufB, int ofB, int cb)
        {
        while (cb-- > 0)
            {
            if (bufA.get(ofA++) != bufB.get(ofB++))
                {
                return false;
                }
            }

        return true;
        }

    /**
     * Zero out the buffer's content, between the position and limit.
     * <p>
     * The buffer's position and limit while respected will not be updated.
     * </p>
     *
     * @param buffer  the buffer to zero out
     */
    public static void zero(ByteBuffer buffer)
        {
        int       of    = buffer.position();
        int       ofEnd = buffer.limit();
        ByteOrder order = null;

        if (of == 0 && ofEnd == buffer.capacity())
            {
            // only when clearing the entire buffer can we assume that it is safe to temporarily change the
            // byte order, if we are clearing  a portion and other threads are operating on other portions
            // this would be unsafe
            order = buffer.order();
            buffer.order(ByteOrder.nativeOrder()); // avoid byte-swapping during zeroing
            }

        for (; ofEnd - of >= 8; of += 8)
            {
            buffer.putLong(of, 0);
            }

        if (ofEnd - of >= 4)
            {
            buffer.putInt(of, 0);
            of += 4;
            }

        if (ofEnd - of >= 2)
            {
            buffer.putShort(of, (short) 0);
            of += 2;
            }

        if (ofEnd - of == 1)
            {
            buffer.put(of, (byte) 0);
            }

        if (order != null)
            {
            buffer.order(order);
            }
        }

    /**
     * Return a String containing the {@link ByteBuffer#remaining remaining} contents of the specified ByteBuffer.
     *
     * @param buf  the buffer to format
     *
     * @return  the string representation of the buffer
     */
    public static String toString(ByteBuffer buf)
        {
        StringBuilder sb = new StringBuilder();
        sb.append('[');

        for (int i = buf.position(), e = buf.limit(); i < e; ++i)
            {
            sb.append(String.format(" %02X", buf.get(i) & 0xFF));
            }

        sb.append(" ]");

        return sb.toString();
        }

    /**
     * Return a String containing the contents of the specified BufferSequence.
     *
     * @param buf      the buffer to format
     * @param cbLimit  the maximum number of bytes to output
     *
     * @return  the string representation of the sequence
     */
    public static String toString(ByteBuffer buf, int cbLimit)
        {
        return toString(new SingleBufferSequence(null, buf), false, cbLimit);
        }


    /**
     * Return a String containing the contents of the specified BufferSequence.
     *
     * @param bufseq  the sequence to format
     *
     * @return  the string representation of the sequence
     */
    public static String toString(BufferSequence bufseq)
        {
        return toString(bufseq, false);
        }

    /**
     * Return a String containing the contents of the specified BufferSequence.
     *
     * @param bufseq       the sequence to format
     * @param fBufDelimit  if the internal ByteBuffers within the sequence should be indicated
     *
     * @return  the string representation of the sequence
     */
    public static String toString(BufferSequence bufseq, boolean fBufDelimit)
        {
        return toString(bufseq, fBufDelimit, Long.MAX_VALUE);
        }

    /**
     * Return a String containing the contents of the specified BufferSequence.
     *
     * @param bufseq       the sequence to format
     * @param fBufDelimit  if the internal ByteBuffers within the sequence should be indicated
     * @param cbLimit      the maximum number of bytes to output
     *
     * @return  the string representation of the sequence
     */
    public static String toString(BufferSequence bufseq, boolean fBufDelimit, long cbLimit)
        {
        StringBuilder sb = new StringBuilder();
        sb.append("[");

        long cb = bufseq.getLength();

        for (ByteBuffer buf : bufseq.getBuffers())
            {
            for (int i = buf.position(), e = buf.limit(); i < e && --cbLimit >= 0; ++i)
                {
                sb.append(String.format(" %02X", buf.get(i) & 0xFF));
                --cb;
                }

            if (cbLimit == 0 && cb > 0)
                {
                sb.append(" ... " + cb + " bytes truncated]");
                return sb.toString();
                }
            else if (fBufDelimit)
                {
                sb.append(';'); // indicates empty buffers in sequence
                }
            }

        sb.append(" ]");

        return sb.toString();
        }

    /**
     * Decode an int which may span multiple buffers.
     *
     * Note the position of the buffers will be advanced.
     *
     * @param aBuffer  the array of buffers
     * @param of       the offset of the buffer to read at
     *
     * @return the int
     */
    public static int getInt(ByteBuffer[] aBuffer, int of)
        {
        ByteBuffer buff = aBuffer[of];
        if (buff.remaining() >= 4)
            {
            return buff.getInt();
            }

        int nResult = 0;
        for (int n = 24; n >= 0; n -= 8)
            {
            for (; !buff.hasRemaining(); buff = aBuffer[++of])
                {}

            nResult |= ((((int) buff.get()) & 0xFF) << n);
            }

        return nResult;
        }

    /**
     * Decode a long which may span multiple buffers.
     *
     * Note the position of the buffers will be advanced.
     *
     * @param aBuffer  the array of buffers
     * @param of       the offset of the buffer to read at
     *
     * @return the long
     */
    public static long getLong(ByteBuffer[] aBuffer, int of)
        {
        ByteBuffer buff = aBuffer[of];
        if (buff.remaining() >= 8)
            {
            return buff.getLong();
            }

        long lResult = 0;
        for (int n = 56; n >= 0; n -= 8)
            {
            for (; !buff.hasRemaining(); buff = aBuffer[++of])
                {}

            lResult |= ((((long) buff.get()) & 0xFF) << n);
            }

        return lResult;
        }

    /**
     * Update the CRC based on the content of the specified buffer, and return
     * the CRC's updated value. The specified buf's position is temporarily
     * advanced, but then restored.
     * Note, the CRC will not be reset before or after use.
     *
     * @param  buf     the ByteBuffer
     * @param  crc32   the CRC32 to update
     *
     * @return the CRC value
     */
    public static int updateCrc(CRC32 crc32, ByteBuffer buf)
        {
        int nPos = buf.position();

        crc32.update(buf);

        buf.position(nPos);
        return (int) crc32.getValue();
        }

    /**
     * Update the CRC based on the content of the buffer array within the
     * specified boundaries.
     *
     * @param crc32  the CRC32 to update
     * @param aBuf   array of ByteBuffer to compute
     * @param of     the starting offset within the buffer array
     * @param cb     the number of bytes to evaluate
     *
     * @return the CRC value
     */
    public static int updateCrc(CRC32 crc32, ByteBuffer[] aBuf, int of, long cb)
        {
        for (; cb > 0; ++of)
            {
            ByteBuffer buf = aBuf[of];
            if (buf.remaining() > cb)
                {
                updateCrc(crc32, buf, cb);
                break;
                }
            else
                {
                updateCrc(crc32, buf);
                cb -= buf.remaining();
                }
            }

        return (int) crc32.getValue();
        }

    /**
     * Update the CRC of the specified ByteBuffer.
     *
     * @param crc32  the CRC32 to update
     * @param buf    the ByteBuffer to check on
     * @param cb     the length of bytes to calculate on
     *
     * @return the CRC value
     */
    public static int updateCrc(CRC32 crc32, ByteBuffer buf, long cb)
        {
        int nLimit = buf.limit();

        buf.limit(buf.position() + (int) cb);
        int lCrc = updateCrc(crc32, buf);

        buf.limit(nLimit);

        return (int) lCrc;
        }

    // ----- singleton holders ----------------------------------------------

    /**
     * Holder for EmptyBuffer
     */
    private static class EmptyBuffer
        {
        public static ByteBuffer INSTANCE = ByteBuffer.allocate(0);
        }

    /**
     * Holder for EmptyDirectBuffer
     */
    private static class EmptyDirectBuffer
        {
        public static ByteBuffer INSTANCE = ByteBuffer.allocateDirect(0);
        }

    /**
     * Holder for the EmptyBufferArray
     */
    private static class EmptyBufferArray
        {
        /**
         * EmptyBufferArray singleton.
         */
        public static ByteBuffer[] INSTANCE = new ByteBuffer[0];
        }

    /**
     * Holder for the EmptyBufferSequence
     */
    private static class EmptySequenceHolder
        {
        /**
         * EmptyBufferSequence singleton.
         */
        public static BufferSequence INSTANCE = new BufferSequence()
        {
        public long getLength()
            {
            return 0;
            }

        public int getBufferCount()
            {
            return 0;
            }

        public ByteBuffer getBuffer(int iBuffer)
            {
            throw new IndexOutOfBoundsException();
            }

        @Override
        public ByteBuffer getUnsafeBuffer(int iBuffer)
            {
            throw new IndexOutOfBoundsException();
            }

        @Override
        public int getBufferPosition(int iBuffer)
            {
            throw new IndexOutOfBoundsException();
            }

        @Override
        public int getBufferLimit(int iBuffer)
            {
            throw new IndexOutOfBoundsException();
            }

        public int getBufferLength(int iBuffer)
            {
            throw new IndexOutOfBoundsException();
            }

        public ByteBuffer[] getBuffers()
            {
            return getEmptyBufferArray();
            }

        public void getBuffers(int iBuffer, int cBuffers,
                ByteBuffer[] abufDest, int iDest)
            {
            if (iBuffer < 0 || cBuffers != 0 || iDest >= abufDest.length)
                {
                throw new IndexOutOfBoundsException();
                }
            }

        public void dispose()
            {
            }
        };
        }
    }
