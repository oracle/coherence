/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io;


import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.BinaryWriteBuffer;

import java.io.IOException;


/**
* A DeltaCompressor implementation that works with opaque (binary) values.
* <p>
* The delta format is composed of a leading byte that indicates the format;
* the format indicator byte is one of the FMT_* field values. If the delta
* value does not begin with one of the FMT_* indicators, then the delta value
* is itself the new value. If the delta is null, then it indicates no change.
* The grammar follows:
* <pre>
* BinaryDelta:
*   FMT_EMPTY
*   FMT_BINDIFF BinaryChangeList-opt OP_TERM
*   FMT_REPLACE-opt Bytes
*   null
*
* BinaryChangeList:
*   OP_EXTRACT Offset Length BinaryChangeList-opt
*   OP_APPEND Length Bytes BinaryChangeList-opt
*
* Offset:
* Length:
*   packed-integer
*
* Bytes:
*   byte Bytes-opt
* </pre>
*
* @author cp  2009.01.06
*/
public class BinaryDeltaCompressor
        implements DeltaCompressor
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public BinaryDeltaCompressor()
        {
        }


    // ----- BinaryDeltaCompressor interface --------------------------------

    /**
    * {@inheritDoc}
    */
    public ReadBuffer extractDelta(ReadBuffer bufOld, ReadBuffer bufNew)
        {
        // check for no delta
        int cbOld = bufOld == null ? 0 : bufOld.length();
        int cbNew = bufNew.length();
        if (cbOld == cbNew && bufNew.equals(bufOld))
            {
            return null;
            }

        // check for truncation
        if (cbNew == 0)
            {
            return DELTA_TRUNCATE;
            }

        // for relatively small binaries (or deltas from nothing), just
        // encode the entire thing
        if (cbOld == 0 || cbNew < 64)
            {
            return encodeReplace(bufNew);
            }

        return createDelta(bufOld, bufNew);
        }

    /**
    * {@inheritDoc}
    */
    public ReadBuffer applyDelta(ReadBuffer bufOld, ReadBuffer bufDelta)
        {
        if (bufDelta == null)
            {
            // null delta means no change
            return bufOld;
            }

        switch (bufDelta.byteAt(0))
            {
            case FMT_EMPTY:
                // the new value is empty
                return NO_BINARY;

            case FMT_BINDIFF:
                try
                    {
                    // apply a binary dif
                    ReadBuffer.BufferInput   inDelta = bufDelta.getBufferInput();
                    inDelta.skipBytes(1); // FMT_BINDIFF
                    WriteBuffer              bufNew  = new BinaryWriteBuffer(
                            Math.max(bufOld.length(), bufDelta.length()));
                    WriteBuffer.BufferOutput outNew  = bufNew.getBufferOutput();
                    while (true)
                        {
                        int nOp = inDelta.readByte();
                        switch (nOp)
                            {
                            case OP_EXTRACT:
                                outNew.writeBuffer(bufOld, inDelta.readPackedInt(),
                                        inDelta.readPackedInt());
                                break;

                            case OP_APPEND:
                                outNew.writeStream(inDelta, inDelta.readPackedInt());
                                break;

                            case OP_TERM:
                                return bufNew.toBinary();

                            default:
                                throw new IllegalStateException("Unknown delta operation ("
                                        + Base.toHexEscape((byte) nOp)
                                        + ") encountered at offset "
                                        + (inDelta.getOffset() - 1));
                            }
                        }
                    }
                catch (IOException e)
                    {
                    throw Base.ensureRuntimeException(e);
                    }

            case FMT_REPLACE:
                // the delta is the new value (except for the 1-byte format
                // indicator)
                return bufDelta.getReadBuffer(1, bufDelta.length() - 1);

            default:
                // all other formats indicate that the delta _is_ the new
                // value
                return bufDelta;
            }
        }


    // ----- internal -------------------------------------------------------

    /**
    * Actually create a delta in the binary delta format. This method is
    * designed to be overridden by subclasses that have more intimate
    * knowledge of the contents of the buffers.
    *
    * @param bufOld  the old value
    * @param bufNew  the new value
    *
    * @return a delta in the binary delta format
    */
    protected ReadBuffer createDelta(ReadBuffer bufOld, ReadBuffer bufNew)
        {
        byte[] abOld;
        int    ofOldStart;
        if (bufOld instanceof AbstractByteArrayReadBuffer)
            {
            AbstractByteArrayReadBuffer abarbOld = ((AbstractByteArrayReadBuffer) bufOld);
            abOld      = abarbOld.m_ab;
            ofOldStart = abarbOld.m_of;
            }
        else
            {
            abOld      = bufOld.toByteArray();
            ofOldStart = 0;
            }

        byte[] abNew;
        int    ofNewStart;
        if (bufNew instanceof AbstractByteArrayReadBuffer)
            {
            AbstractByteArrayReadBuffer abarbNew = ((AbstractByteArrayReadBuffer) bufNew);
            abNew      = abarbNew.m_ab;
            ofNewStart = abarbNew.m_of;
            }
        else
            {
            abNew      = bufNew.toByteArray();
            ofNewStart = 0;
            }

        // measure the head portion of the binary that is identical
        int ofOld = ofOldStart;
        int ofNew = ofNewStart;
        int cbOld = bufOld.length();
        int cbNew = bufNew.length();
        int cbMax = Math.min(cbOld, cbNew);
        int ofOldStop = ofOldStart + cbMax;
        while (ofOld < ofOldStop && abOld[ofOld] == abNew[ofNew])
            {
            ++ofOld;
            ++ofNew;
            }
        int cbHead = ofOld - ofOldStart;

        // check if we stripped off the maximum possible (all of it!), and if
        // not, then measure the tail portion that is identical
        int cbTail = 0;
        if (cbHead == cbMax)
            {
            if (cbOld == cbNew)
                {
                // the binaries are identical
                return null;
                }
            }
        else
            {
            // measure the identical tail
            ofOld = ofOldStart + cbOld - 1;
            ofNew = ofNewStart + cbNew - 1;
            int cbMaxTail = cbMax - cbHead;
            while (--cbMaxTail >= 0 && abOld[ofOld] == abNew[ofNew])
                {
                --ofOld;
                --ofNew;
                }
            cbTail = ofOldStart + cbOld - 1 - ofOld;
            }

        int cbDone = 0;
        WriteBuffer.BufferOutput outDelta = null;

        // only encode delta regions that make up a significant portion of the binary
        int cbDiffThreshold = Math.max(MIN_BLOCK, Math.min(cbOld, cbNew) / 4);
        if (cbHead > cbDiffThreshold)
            {
            outDelta = writeExtract(outDelta, cbMax, 0, cbHead);
            cbDone   = cbHead;
            }

        if (cbOld == cbNew)
            {
            // look for identical sections inside the binaries
            ofOld     = ofOldStart + cbHead + 1;
            ofNew     = ofNewStart + cbHead + 1;
            ofOldStop = ofOldStop - cbTail - cbDiffThreshold;
            while (ofOld < ofOldStop)
                {
                // Note: the contents of this loop are intentionally arranged in
                //       a manner to promote loop-unrolling/memcmp optimization
                int cbRun = 0;
                while (abOld[ofOld] == abNew[ofNew])
                    {
                    // find the matching run
                    //
                    // Note: the condition (ofOld < ofOldStop + cbDiffThreshold)
                    //       implicitly holds here because we know that the
                    //       region terminates with at least one byte that does
                    //       not match (see preceding cbTail computation).
                    ++cbRun;
                    ++ofOld;
                    ++ofNew;
                    }

                if (cbRun > cbDiffThreshold)
                    {
                    // immediately previous to the current offset, there
                    // is a run of cbRun identical bytes, previous to
                    // which there is a series of differing bytes that
                    // will have to be copied verbatim starting at cbDone
                    // bytes into the buffer and proceeding up to the
                    // run of identical bytes
                    int cbDif = ofNew - ofNewStart - cbDone - cbRun;
                    outDelta  = writeAppend(outDelta, cbMax, abNew, ofNewStart + cbDone, cbDif);
                    outDelta  = writeExtract(outDelta, cbMax, cbDone + cbDif, cbRun);
                    cbDone   += cbDif + cbRun;
                    }

                while (ofOld < ofOldStop &&
                       abOld[ofOld] != abNew[ofNew])
                    {
                    // skip the diffing region
                    ++ofOld;
                    ++ofNew;
                    }
                }
            }

        if (cbTail > cbDiffThreshold)
            {
            int cbAppend = cbNew - cbDone - cbTail;
            if (cbAppend > 0)
                {
                outDelta = writeAppend(outDelta, cbMax, abNew, ofNewStart + cbDone, cbAppend);
                }

            // encode the tail
            outDelta = writeExtract(outDelta, cbMax, cbOld - cbTail, cbTail);
            }
        else if (outDelta != null && cbDone < cbNew)
            {
            outDelta = writeAppend(outDelta, cbMax, abNew, ofNewStart + cbDone, cbNew - cbDone);
            }

        return outDelta == null
                ? encodeReplace(bufNew)
                : finalizeDelta(outDelta);
        }

    /**
    * Encode the passed buffer into a delta value that will cause the old
    * value to be replaced by the value in the passed buffer.
    *
    * @param buf a non-null, non-zero-length ReadBuffer
    *
    * @return a ReadBuffer that acts as a delta that replaces an old value
    *         with the contents of <tt>buf</tt>
    */
    private ReadBuffer encodeReplace(ReadBuffer buf)
        {
        switch (buf.byteAt(0))
            {
            case FMT_EMPTY:
            case FMT_BINDIFF:
            case FMT_REPLACE:
                {
                int cb = buf.length();
                WriteBuffer bufDelta = new BinaryWriteBuffer(1 + cb);
                bufDelta.write(0, FMT_REPLACE);
                bufDelta.write(1, buf, 0, cb);
                return bufDelta.toBinary();
                }

            default:
                return buf.toBinary();
            }
        }

    /**
    * Make sure that a WriteBuffer exists if one doesn't already.
    *
    * @param out    the existing WriteBuffer or null
    * @param cbMax  the expected resulting size of the write buffer
    *
    * @return a WriteBuffer, never null
    */
    private static WriteBuffer.BufferOutput ensureDiff(
            WriteBuffer.BufferOutput out, int cbMax)
            throws IOException
        {
        if (out == null)
            {
            out = new BinaryWriteBuffer(cbMax).getBufferOutput();
            out.write(FMT_BINDIFF);
            }
        return out;
        }

    /**
    * Encode a binary diff "append" operator to indicate that bytes should
    * be appended from the delta stream to the new value.
    *
    * @param out    the existing BufferOutput for the diff, or null
    * @param cbMax  the expected resulting size of the write buffer
    * @param ab     the byte array from which to get the bytes to append
    * @param of     the offset of the bytes to append
    * @param cb     the number of bytes to append
    *
    * @return a BufferOutput, never null
    */
    private static WriteBuffer.BufferOutput writeAppend(
            WriteBuffer.BufferOutput out, int cbMax, byte[] ab, int of, int cb)
        {
        try
            {
            out = ensureDiff(out, cbMax);
            out.write(OP_APPEND);
            out.writePackedInt(cb);
            out.write(ab, of, cb);
            return out;
            }
        catch (IOException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    /**
    * Encode a binary diff "extract" operator to indicate that bytes should
    * be copied from the old value to the new value.
    *
    * @param out    the existing BufferOutput for the diff, or null
    * @param cbMax  the expected resulting size of the write buffer
    * @param of     the offset of the old buffer to append
    * @param cb     the length of the old buffer to append
    *
    * @return a BufferOutput, never null
    */
    private static WriteBuffer.BufferOutput writeExtract(
            WriteBuffer.BufferOutput out, int cbMax, int of, int cb)
        {
        try
            {
            out = ensureDiff(out, cbMax);
            out.write(OP_EXTRACT);
            out.writePackedInt(of);
            out.writePackedInt(cb);
            return out;
            }
        catch (IOException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    /**
    * Convert an open delta output stream into a finalized delta.
    *
    * @param out  the delta output stream
    *
    * @return a ReadBuffer containing the delta
    */
    private static ReadBuffer finalizeDelta(WriteBuffer.BufferOutput out)
        {
        try
            {
            out.write(OP_TERM);
            }
        catch (IOException e)
            {
            throw Base.ensureRuntimeException(e);
            }

        return out.getBuffer().toBinary();
        }


    // ----- constants ------------------------------------------------------

    /**
    * A format indicator (the first byte of the binary delta) that indicates
    * that the new value is a zero-length binary value.
    */
    protected static final byte FMT_EMPTY   = (byte) 0xF6;

    /**
    * A format indicator (the first byte of the binary delta) that indicates
    * that the new value is found in its entirety in the delta value. In
    * other words, other than the first byte, the delta is itself the new
    * value.
    */
    protected static final byte FMT_REPLACE = (byte) 0xF5;

    /**
    * A format indicator (the first byte of the binary delta) that indicates
    * that the new value is formed by applying a series of modifications to
    * the old value. The possible modifications are defined by the OP_*
    * constants.
    */
    protected static final byte FMT_BINDIFF = (byte) 0xF4;

    /**
    * A binary delta operator that instructs the {@link #applyDelta} method
    * to extract bytes from the old value and append them to the new value.
    * The format is the one-byte OP_EXTRACT indicator followed by a packed
    * int offset and packed int length. The offset and length indicate the
    * region of the old value to extract and append to the new value.
    */
    protected static final byte OP_EXTRACT  = (byte) 0x01;

    /**
    * A binary delta operator that instructs the {@link #applyDelta} method
    * to copy the following bytes from the delta value and append them to the
    * new value. The format is the one-byte OP_APPEND indicator followed by a
    * packed int length and then a series of bytes. The length indicates the
    * length of the series of bytes to copy from the delta value and append
    * to the new value.
    */
    protected static final byte OP_APPEND   = (byte) 0x02;

    /**
    * A binary delta operator that instructs the {@link #applyDelta} method
    * that the delta has been fully applied.
    */
    protected static final byte OP_TERM     = (byte) 0x03;

    /**
    * Minimum length of an "extract" block to encode.
    */
    protected static final int  MIN_BLOCK   = 12;

    /**
    * An empty Binary object.
    */
    protected static final Binary NO_BINARY = AbstractReadBuffer.NO_BINARY;

    /**
    * A delta value that indicates an empty new value.
    */
    protected static final Binary DELTA_TRUNCATE = new Binary(new byte[] {FMT_EMPTY});
    }
