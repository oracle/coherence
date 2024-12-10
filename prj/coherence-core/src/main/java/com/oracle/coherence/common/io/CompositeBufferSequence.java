/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.io;

import java.nio.ByteBuffer;

/**
 * CompositeBufferSequence is a BufferSequence which is composed of two underlying BufferSequences.
 *
 * @author mf  2014.02.19
 */
public class CompositeBufferSequence
    implements BufferSequence
    {
    /**
     * Construct a CompositeBufferSequence from two BufferSequences.
     *
     * @param bufSeqA  the first sequence
     * @param bufSeqB  the second sequence
     */
    public CompositeBufferSequence(BufferSequence bufSeqA, BufferSequence bufSeqB)
        {
        f_bufSeqA = bufSeqA;
        f_bufSeqB = bufSeqB;
        }

    @Override
    public long getLength()
        {
        return f_bufSeqA.getLength() + f_bufSeqB.getLength();
        }

    @Override
    public int getBufferCount()
        {
        return f_bufSeqA.getBufferCount() + f_bufSeqB.getBufferCount();
        }

    @Override
    public ByteBuffer getBuffer(int iBuffer)
        {
        int cBufA = f_bufSeqA.getBufferCount();
        return iBuffer < cBufA
                ? f_bufSeqA.getBuffer(iBuffer)
                : f_bufSeqB.getBuffer(iBuffer - cBufA);
        }

    @Override
    public ByteBuffer getUnsafeBuffer(int iBuffer)
        {
        int cBufA = f_bufSeqA.getBufferCount();
        return iBuffer < cBufA
               ? f_bufSeqA.getUnsafeBuffer(iBuffer)
               : f_bufSeqB.getUnsafeBuffer(iBuffer - cBufA);
        }

    @Override
    public int getBufferPosition(int iBuffer)
        {
        int cBufA = f_bufSeqA.getBufferCount();
        return iBuffer < cBufA
               ? f_bufSeqA.getBufferPosition(iBuffer)
               : f_bufSeqB.getBufferPosition(iBuffer - cBufA);
        }

    @Override
    public int getBufferLimit(int iBuffer)
        {
        int cBufA = f_bufSeqA.getBufferCount();
        return iBuffer < cBufA
               ? f_bufSeqA.getBufferLimit(iBuffer)
               : f_bufSeqB.getBufferLimit(iBuffer - cBufA);
        }

    @Override
    public int getBufferLength(int iBuffer)
        {
        int cBufA = f_bufSeqA.getBufferCount();
        return iBuffer < cBufA
               ? f_bufSeqA.getBufferLength(iBuffer)
               : f_bufSeqB.getBufferLength(iBuffer - cBufA);
        }

    @Override
    public ByteBuffer[] getBuffers()
        {
        int          cBufA = f_bufSeqA.getBufferCount();
        int          cBufB = f_bufSeqB.getBufferCount();
        ByteBuffer[] aBuff = new ByteBuffer[cBufA + cBufB];

        f_bufSeqA.getBuffers(0, cBufA, aBuff, 0);
        f_bufSeqB.getBuffers(0, cBufB, aBuff, cBufA);

        return aBuff;
        }

    @Override
    public void getBuffers(int iBuffer, int cBuffers, ByteBuffer[] abufDest, int iDest)
        {
        int cBufA = f_bufSeqA.getBufferCount();

        if (iBuffer < cBufA)
            {
            int cBufCopy = Math.min(cBuffers, cBufA - iBuffer);

            f_bufSeqA.getBuffers(iBuffer, cBufCopy, abufDest, iDest);

            iDest    += cBufCopy;
            iBuffer  += cBufCopy;
            cBuffers -= cBufCopy;
            }

        if (cBuffers > 0)
            {
            f_bufSeqB.getBuffers(iBuffer - cBufA, cBuffers, abufDest, iDest);
            }
        }

    @Override
    public void dispose()
        {
        f_bufSeqA.dispose();
        f_bufSeqB.dispose();
        }


    // ----- data members ---------------------------------------------------

    /**
     * The first part of the compound sequence.
     */
    final BufferSequence f_bufSeqA;

    /**
     * The second part of the compound sequence.
     */
    final BufferSequence f_bufSeqB;
    }
