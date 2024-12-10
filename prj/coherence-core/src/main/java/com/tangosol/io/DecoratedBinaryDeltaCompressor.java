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
import com.tangosol.util.ExternalizableHelper;

import java.io.IOException;


/**
* A DeltaCompressor implementation that works with decorated binary values.
* <p>
* The delta format is composed of a leading byte that indicates the format;
* the format indicator byte is one of the DECO_* field values. If the delta
* value does not begin with one of the leading DECO_* indicators, then the
* entire delta is the delta for the "value" portion of the decorated result.
* The grammar follows:
* </p>
* <pre>
* DecoratedBinaryDelta:
*   DECO_DELETE_ALL OptionalValueDelta
*   DecorationDeltaList DECO_TERM OptionalValueDelta
*   DECO_NO_CHANGES-opt ValueDelta
*   null
*
* OptionalValueDelta:
*   VALUE_NO_CHANGES
*   VALUE_CHANGES-opt ValueDelta
*
* ValueDelta:
*   Binary (as defined by the underlying DeltaCompressor)
*
* DecorationDeltaList:
*   DecorationDelta
*   DecorationDeltaList DecorationDelta
*
* DecorationDelta:
*   DECO_INSERT DecorationId Binary
*   DECO_UPDATE DecorationId BinaryDelta
*   DECO_DELETE DecorationId
*
* BinaryDelta:
*   Binary (as returned by the BinaryDeltaCompressor)
*
* Binary:
*   Length &lt;bytes&gt;
*
* DecorationId:
* Length:
*   &lt;packed-integer&gt;
* </pre>
*
* @author cp  2009.01.20
*/
public class DecoratedBinaryDeltaCompressor
        extends ExternalizableHelper
        implements DeltaCompressor
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a DecoratedBinaryDeltaCompressor.
    *
    * @param compressorValue  the compressor responsible for performing
    *                         delta compression on the underlying Binary
    *                         value (i.e. the DECO_VALUE decoration)
    */
    public DecoratedBinaryDeltaCompressor(DeltaCompressor compressorValue)
        {
        Base.azzert(compressorValue != null, "DeltaCompressor is required");

        m_compressorValue = compressorValue;
        }


    // ----- DeltaCompressor interface --------------------------------------

    /**
    * {@inheritDoc}
    */
    public ReadBuffer extractDelta(ReadBuffer bufOld, ReadBuffer bufNew)
        {
        if (Base.equals(bufOld, bufNew))
            {
            // null means no-diff
            return null;
            }

        BinaryWriteBuffer bufDelta = null;
        if (isDecorated(bufNew))
            {
            ReadBuffer[] abufDecoOld = getDecorations(bufOld);
            ReadBuffer[] abufDecoNew = getDecorations(bufNew);
            int cDecoOld = abufDecoOld.length;
            int cDecoNew = abufDecoNew.length;
            WriteBuffer.BufferOutput outDelta = null;
            try
                {
                for (int i = 1, c = Math.max(cDecoOld, cDecoNew); i < c; ++i)
                    {
                    ReadBuffer bufDecoOld = i < cDecoOld ? abufDecoOld[i] : null;
                    ReadBuffer bufDecoNew = i < cDecoNew ? abufDecoNew[i] : null;
                    if (!Base.equals(bufDecoOld, bufDecoNew))
                        {
                        if (bufDelta == null)
                            {
                            bufDelta = new BinaryWriteBuffer(64);
                            outDelta = bufDelta.getAppendingBufferOutput();
                            }

                        if (bufDecoOld == null)
                            {
                            // insert the decoration
                            outDelta.write(DECO_INSERT);
                            outDelta.writePackedInt(i);
                            outDelta.writePackedInt(bufDecoNew.length());
                            outDelta.writeBuffer(bufDecoNew);
                            }
                        else if (bufDecoNew == null)
                            {
                            // delete the decoration
                            outDelta.write(DECO_DELETE);
                            outDelta.writePackedInt(i);
                            }
                        else
                            {
                            // update the decoration
                            outDelta.write(DECO_UPDATE);
                            outDelta.writePackedInt(i);
                            ReadBuffer bufDecoDelta = s_compressorBinary
                                    .extractDelta(bufDecoOld, bufDecoNew);
                            Base.azzert(bufDecoDelta != null);
                            outDelta.writePackedInt(bufDecoDelta.length());
                            outDelta.writeBuffer(bufDecoDelta);
                            }
                        }
                    }

                if (outDelta != null)
                    {
                    // "cap" the series of decoration changes
                    outDelta.write(DECO_TERM);
                    }
                }
            catch (IOException e)
                {
                throw Base.ensureRuntimeException(e);
                }

            bufOld = abufDecoOld[ExternalizableHelper.DECO_VALUE];
            bufNew = abufDecoNew[ExternalizableHelper.DECO_VALUE];
            }
        else if (isDecorated(bufOld))
            {
            // new value has no decorations, but the old value does; delete
            // them all
            bufDelta = new BinaryWriteBuffer(64);
            bufDelta.write(0, DECO_DELETE_ALL);
            bufOld = getUndecorated(bufOld);
            }

        ReadBuffer bufValueDelta = m_compressorValue.extractDelta(bufOld, bufNew);
        if (bufValueDelta == null)
            {
            // no delta for the value
            if (bufDelta != null)
                {
                bufDelta.write(bufDelta.length(), VALUE_NO_CHANGES);
                }
            }
        else if (bufDelta == null)
            {
            // there is no delta already, so the value delta becomes the
            // delta if it's non-ambiguous, otherwise a DECO_NO_CHANGES is
            // prepended
            boolean fAmbiguous = true;
            if (bufValueDelta.length() >= 1)
                {
                switch (bufValueDelta.byteAt(0))
                    {
                    case DECO_NO_CHANGES:
                    case DECO_DELETE_ALL:
                    case DECO_INSERT:
                    case DECO_UPDATE:
                    case DECO_DELETE:
                        // each of these is a legitimate leading byte for a
                        // decorated delta
                        break;

                    default:
                        fAmbiguous = false;
                        break;
                    }
                }

            if (fAmbiguous)
                {
                // prepend DECO_NO_CHANGES
                bufDelta = new BinaryWriteBuffer(1 + bufValueDelta.length());
                bufDelta.write(0, DECO_NO_CHANGES);
                bufDelta.write(1, bufValueDelta);
                }
            }
        else
            {
            // there is a delta already, so the value delta is appended
            boolean fAmbiguous = true;
            if (bufValueDelta.length() >= 1)
                {
                switch (bufValueDelta.byteAt(0))
                    {
                    case VALUE_CHANGES:
                    case VALUE_NO_CHANGES:
                        // each of these is a legitimate leading byte for a
                        // value delta
                        break;

                    default:
                        fAmbiguous = false;
                        break;
                    }
                }

            if (fAmbiguous)
                {
                // prepend VALUE_CHANGES
                bufDelta.write(bufDelta.length(), VALUE_CHANGES);
                }

            bufDelta.write(bufDelta.length(), bufValueDelta);
            }

        return bufDelta == null
               ? (bufValueDelta == null ? null : bufValueDelta)
               : bufDelta.toBinary();
        }

    /**
    * {@inheritDoc}
    */
    public ReadBuffer applyDelta(ReadBuffer bufOld, ReadBuffer bufDelta)
        {
        if (bufDelta == null)
            {
            // null delta indicates no change
            return bufOld;
            }

        ReadBuffer[] abufDeco = EMPTY_BINARY_ARRAY;
        switch (bufDelta.byteAt(0))
            {
            case DECO_NO_CHANGES:
                // keep all the decorations (if any) from the old binary and
                // apply the value delta (which is the remainder of the delta
                // buffer)
                abufDeco = getDecorations(bufOld);
                bufDelta = bufDelta.getReadBuffer(1, bufDelta.length() - 1);
                break;

            case DECO_DELETE_ALL:
                {
                // keep only the undecorated value from the old binary and
                // apply the value delta (which is the remainder of the delta
                // buffer); note that the value delta can be an explicit
                // VALUE_NO_CHANGES token that indicates that the underlying
                // DeltaCompressor returned null from extractDelta()
                bufDelta = parseValueDelta(bufDelta, 1);
                }
                break;

            case DECO_INSERT:
            case DECO_UPDATE:
            case DECO_DELETE:
                {
                // start with the old set of decorations
                abufDeco = getDecorations(bufOld);

                // at least one decoration change occurred that needs to be
                // processed
                ReadBuffer.BufferInput inDelta = bufDelta.getBufferInput();
                try
                    {
                    while (true)
                        {
                        byte b = inDelta.readByte();
                        if (b == DECO_TERM)
                            {
                            break;
                            }

                        int        iDeco   = inDelta.readPackedInt();
                        ReadBuffer bufDeco = null;
                        switch (b)
                            {
                            case DECO_INSERT:
                                {
                                // insert is the only type of decoration
                                // delta that can cause the array of
                                // decorations to increase in size
                                if (iDeco >= abufDeco.length)
                                    {
                                    ReadBuffer[] abufNew = new ReadBuffer[iDeco+1];
                                    System.arraycopy(abufDeco, 0, abufNew, 0, abufDeco.length);
                                    abufDeco = abufNew;
                                    }

                                int cbBin = inDelta.readPackedInt();
                                int ofBin = inDelta.getOffset();
                                bufDeco = bufDelta.getReadBuffer(ofBin, cbBin);
                                inDelta.setOffset(ofBin + cbBin);
                                }
                                break;

                            case DECO_UPDATE:
                                {
                                int        cbDelta = inDelta.readPackedInt();
                                int        ofDelta = inDelta.getOffset();
                                ReadBuffer bufDecoOld;
                                try
                                    {
                                    bufDecoOld = abufDeco[iDeco];
                                    }
                                catch(IndexOutOfBoundsException e)
                                    {
                                    throw Base.ensureRuntimeException(e,
                                            "update decoration " + iDeco);
                                    }
                                ReadBuffer bufDecoDelta =
                                        bufDelta.getReadBuffer(ofDelta, cbDelta);
                                inDelta.setOffset(ofDelta + cbDelta);
                                bufDeco = s_compressorBinary.applyDelta(
                                        bufDecoOld, bufDecoDelta);
                                }
                                break;

                            case DECO_DELETE:
                                break;

                            default:
                                throw new IllegalStateException(
                                        "byte=" + Base.toHexEscape(b));
                            }

                        try
                            {
                            abufDeco[iDeco] = bufDeco;
                            }
                        catch(IndexOutOfBoundsException e)
                            {
                            throw Base.ensureRuntimeException(e,
                                    "decoration " + iDeco);
                            }
                        }
                    }
                catch (IOException e)
                    {
                    throw Base.ensureRuntimeException(e);
                    }

                // extract the remaining delta for the value itself (which
                // could be VALUE_NO_CHANGES)
                bufDelta = parseValueDelta(bufDelta, inDelta.getOffset());
                }
                break;

            default:
                // the absence of a leading DECO_* format indicator is the
                // same as the DECO_NO_CHANGES indicator; apply the value
                // delta (which is the entire contents of bufDelta)
                abufDeco = getDecorations(bufOld);
                break;
            }

        // get the old value
        if (abufDeco.length > DECO_VALUE)
            {
            // if the decorations originally came from binOld , then the
            // old value is stored as decoration #0
            bufOld = abufDeco[ExternalizableHelper.DECO_VALUE];

            // failure to null out the 0th element will override the new
            // value
            abufDeco[ExternalizableHelper.DECO_VALUE] = null;
            }
        else
            {
            bufOld = getUndecorated(bufOld);
            }

        // apply the value delta (if there is one)
        ReadBuffer bufNew = bufDelta == null
                ? bufOld
                : m_compressorValue.applyDelta(bufOld, bufDelta);

        // add decorations (if there are any)
        return decorate(bufNew, abufDeco);
        }

    /**
    * Parse an OptionalValueDelta.
    *
    * <pre>
    * OptionalValueDelta:
    *   VALUE_NO_CHANGES
    *   VALUE_CHANGES-opt ValueDelta
    * </pre>
    *
    * @param bufDelta  the entire decorated binary delta
    * @param of        the offset of the optional value delta
    *
    * @return the value delta as a read buffer
    */
    private static ReadBuffer parseValueDelta(ReadBuffer bufDelta, int of)
        {
        int cb = bufDelta.length();
        if (cb <= of)
            {
            return NO_BINARY;
            }

        switch (bufDelta.byteAt(of))
            {
            case VALUE_NO_CHANGES:
                return null;

            case VALUE_CHANGES:
                // the value is the remainder of the buffer from "of+1"
                return bufDelta.getReadBuffer(of + 1, cb - of - 1);

            default:
                // the value is the buffer starting at offset "of"
                return bufDelta.getReadBuffer(of, cb - of);
            }
        }


    // ----- Object methods -------------------------------------------------

    /**
    * {@inheritDoc}
    */
    public String toString()
        {
        return "DecoratedBinaryDeltaCompressor {" + m_compressorValue + "}";
        }


    // ----- constants ------------------------------------------------------

    /**
    * A decorated binary operator that instructs the {@link #applyDelta}
    * method that there are no decoration changes in the delta.
    * The format is the one-byte DECO_NO_CHANGES indicator.
    * <p>
    * This operator terminates the decorated binary delta stream, so the
    * following byte is the first byte of the value delta.
    */
    private static final byte DECO_NO_CHANGES   = (byte) 0xFD;

    /**
    * A decorated binary operator that instructs the {@link #applyDelta}
    * method to remove all decorations, leaving only the undecorated value.
    * The format is the one-byte DECO_DELETE_ALL indicator.
    * <p>
    * This operator terminates the decorated binary delta stream, so the
    * following byte is the first byte of the value delta.
    */
    private static final byte DECO_DELETE_ALL   = (byte) 0xFC;

    /**
    * A decorated binary operator that instructs the {@link #applyDelta}
    * method to insert the specified decoration. The format is the one-byte
    * DECO_INSERT indicator followed by a packed int decoration id and then
    * a binary value in the {@link Binary#writeExternal(java.io.DataOutput)}
    * format.
    */
    private static final byte DECO_INSERT       = (byte) 0xFB;

    /**
    * A decorated binary operator that instructs the {@link #applyDelta}
    * method to update the specified decoration. The format is the one-byte
    * DECO_UPDATE indicator followed by a packed int decoration id and then
    * a binary value that conforms to the BinaryDeltaCompressor format.
    */
    private static final byte DECO_UPDATE       = (byte) 0xFA;

    /**
    * A decorated binary operator that instructs the {@link #applyDelta}
    * method to remove the specified decoration. The format is the one-byte
    * DECO_DELETE indicator followed by a packed int decoration id.
    */
    private static final byte DECO_DELETE       = (byte) 0xF9;

    /**
    * A decorated binary operator that instructs the {@link #applyDelta}
    * method that there are no more decoration deltas in the decorated binary
    * delta stream. The format is the one-byte DECO_TERM indicator.
    * <p>
    * This operator terminates the decorated binary delta stream, so the
    * following byte is the first byte of the value delta.
    */
    private static final byte DECO_TERM         = (byte) 0xF8;

    /**
    * A place-holder for the value portion of the delta that indicates that
    * there was no delta to the value.
    * <p>
    * Note: This value re-uses the same value as DECO_NO_CHANGES. It is able
    * to do so because it can only occur after the conclusion of the
    * decoration stream.
    */
    private static final byte VALUE_NO_CHANGES  = (byte) 0xFD;

    /**
    * A place-holder for the value portion of the delta that indicates that
    * there was a delta to the value. This prefix must be used in any case
    * when VALUE_NO_CHANGES can be used and the value delta itself starts
    * with the VALUE_CHANGES or VALUE_NO_CHANGES byte.
    * <p>
    * Note: This value re-uses the same value as DECO_DELETE_ALL. It is able
    * to do so because it can only occur after the conclusion of the
    * decoration stream.
    */
    private static final byte VALUE_CHANGES     = (byte) 0xFC;

    /**
    * An empty Binary object.
    */
    private static final Binary NO_BINARY = AbstractReadBuffer.NO_BINARY;


    // ----- data members ---------------------------------------------------

    /**
    * A Binary delta compressor that is used to compress individual
    * decorations.
    */
    private static final BinaryDeltaCompressor s_compressorBinary = new BinaryDeltaCompressor();

    /**
    * The delta compressor responsible for compressing the value portion of
    * the decorated binaries.
    */
    private DeltaCompressor m_compressorValue;
    }
