/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof;


import com.tangosol.io.BinaryDeltaCompressor;
import com.tangosol.io.DeltaCompressor;
import com.tangosol.io.ReadBuffer;
import com.tangosol.io.ReadBuffer.BufferInput;
import com.tangosol.io.WriteBuffer.BufferOutput;

import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.BinaryWriteBuffer;

import java.io.IOException;


/**
* A DeltaCompressor implementation that works with Portable Object Format
* (POF) values. Note that while the POF parsing is stateful, the
* PofDeltaCompressor itself is still stateless, deferring all state
* management to a per-invocation data structure.
*
* @author cp  2009.01.26
*/
public class PofDeltaCompressor
        extends BinaryDeltaCompressor
        implements DeltaCompressor, PofConstants
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public PofDeltaCompressor()
        {
        }


    // ----- internal -------------------------------------------------------

    /**
    * {@inheritDoc}
    */
    public ReadBuffer createDelta(ReadBuffer bufOld, ReadBuffer bufNew)
        {
        BufferInput inOld = bufOld.getBufferInput();
        BufferInput inNew = bufNew.getBufferInput();
        try
            {
            ChangeTracker tracker = new ChangeTracker(inOld, inNew);
            diffValue(inOld, inNew, tracker);
            Base.azzert(inOld.available() == 0 && inNew.available() == 0);
            return tracker.getDelta();
            }
        catch (Exception e)
            {
            Base.err(e);
            Base.err("(Reverting to default binary delta algorithm.)");
            return super.createDelta(bufOld, bufNew);
            }
        }

    /**
    * Within the two passed POF streams, parse and compare a POF value.
    *
    * @param inOld    the BufferInput to read from
    * @param inNew    the BufferInput to read from
    * @param tracker  the ChangeTracker that computes the diff result
    *
    * @throws IOException  if an I/O error occurs
    */
    protected void diffValue(BufferInput inOld,
                             BufferInput inNew,
                             ChangeTracker tracker)
            throws IOException
        {
        inOld.mark(10);
        inNew.mark(10);

        int nOldType = inOld.readPackedInt();
        int nNewType = inNew.readPackedInt();

        // either or both could be identity type (identity is optional)
        if (nOldType == T_IDENTITY || nNewType == T_IDENTITY)
            {
            boolean fBothId = true;

            int nOldId = -1;
            if (nOldType == T_IDENTITY)
                {
                nOldId = inOld.readPackedInt();
                }
            else
                {
                fBothId = false;
                inOld.reset();
                }

            int nNewId = -1;
            if (nNewType == T_IDENTITY)
                {
                nNewId = inNew.readPackedInt();
                }
            else
                {
                fBothId = false;
                inNew.reset();
                }

            tracker.advance(fBothId && nOldId == nNewId);

            // if they were identities, then we haven't yet read the type,
            // and if they were types, then we reset() and backed up over
            // the types; either way, read the type
            nOldType = inOld.readPackedInt();
            nNewType = inNew.readPackedInt();
            }

        if (nOldType == nNewType)
            {
            tracker.advance(true);
            diffUniformValue(inOld, inNew, tracker, nOldType);
            }
        else
            {
            // skip the differing value
            PofHelper.skipUniformValue(inOld, nOldType);
            PofHelper.skipUniformValue(inNew, nNewType);
            tracker.advance(false);
            }
        }

    /**
    * Within the two passed POF streams, parse and compare a POF value of the
    * specified type.
    *
    * @param inOld    the BufferInput to read from
    * @param inNew    the BufferInput to read from
    * @param tracker  the ChangeTracker that computes the diff result
    * @param nType    the type to parse
    *
    * @throws IOException  if an I/O error occurs
    */
    protected void diffUniformValue(BufferInput inOld,
                                    BufferInput inNew,
                                    ChangeTracker tracker,
                                    int nType)
            throws IOException
        {
        if (nType >= 0)
            {
            diffUserType(inOld, inNew, tracker);
            }
        else
            {
            switch (nType)
                {
                case T_INT16:                  // int16
                case T_INT32:                  // int32
                case T_BOOLEAN:                // boolean
                    diffPackedInt(inOld, inNew, tracker);
                    break;

                case T_INT64:                  // int64
                    diffPackedLong(inOld, inNew, tracker);
                    break;

                case T_INT128:                 // int128
                    tracker.advance(PofHelper.readBigInteger(inOld).equals(
                            PofHelper.readBigInteger(inNew)));
                    break;

                case T_FLOAT32:                // float32
                    // treat it as a 4-byte integer for efficiency and
                    // to avoid the various NaN issues
                    tracker.advance(inOld.readInt() == inNew.readInt());
                    break;

                case T_FLOAT64:                // float64
                    // treat it as a 8-byte integer for efficiency and
                    // to avoid the various NaN issues
                    tracker.advance(inOld.readLong() == inNew.readLong());
                    break;

                case T_FLOAT128:               // float128*
                    // treat the 16-byte float as as two 8-byte integers
                    tracker.advance(inOld.readLong() == inNew.readLong()
                                  & inOld.readLong() == inNew.readLong());
                    break;

                case T_DECIMAL32:              // decimal32
                    tracker.advance(PofHelper.readBigDecimal(inOld, 4)
                            .equals(PofHelper.readBigDecimal(inNew, 4)));
                    break;

                case T_DECIMAL64:              // decimal64
                    tracker.advance(PofHelper.readBigDecimal(inOld, 8)
                            .equals(PofHelper.readBigDecimal(inNew, 8)));
                    break;

                case T_DECIMAL128:             // decimal128
                    tracker.advance(PofHelper.readBigDecimal(inOld, 16)
                            .equals(PofHelper.readBigDecimal(inNew, 16)));
                    break;

                case T_OCTET:                  // octet
                    tracker.advance(inOld.readUnsignedByte() == inNew.readUnsignedByte());
                    break;

                case T_CHAR:                   // char
                    tracker.advance(PofHelper.readChar(inOld) == PofHelper.readChar(inNew));
                    break;

                case T_OCTET_STRING:           // octet-string
                case T_CHAR_STRING:            // char-string
                    {
                    // packed int followed by a series of bytes
                    int cbOld = inOld.readPackedInt();
                    int cbNew = inNew.readPackedInt();
                    boolean fSame = cbOld == cbNew;
                    if (fSame)
                        {
                        tracker.advance(true);
                        // note: null string has any length < 0
                        if (cbOld > 0)
                            {
                            tracker.advance(inOld.readBuffer(cbOld).toBinary()
                                    .equals(inNew.readBuffer(cbNew).toBinary()));
                            }
                        }
                    else
                        {
                        inOld.skipBytes(cbOld);
                        inNew.skipBytes(cbNew);
                        tracker.advance(false);
                        }
                    }
                    break;

                case T_DATE:                   // date
                    // year, month, day
                    diffPackedInts(inOld, inNew, tracker, 3);
                    break;

                case T_YEAR_MONTH_INTERVAL:    // year-month-interval
                    // years, months
                    diffPackedInts(inOld, inNew, tracker, 2);
                    break;

                case T_TIME:                   // time
                    {
                    // hour, minute, second, fraction
                    diffPackedInts(inOld, inNew, tracker, 4);
                    diffTimeZone(inOld, inNew, tracker);
                    }
                    break;

                case T_TIME_INTERVAL:          // time-interval
                    // hour, minute, second, nanos
                    diffPackedInts(inOld, inNew, tracker, 4);
                    break;

                case T_DATETIME:               // datetime
                    {
                    // year, month, day, hour, minute, second, fraction
                    diffPackedInts(inOld, inNew, tracker, 7);
                    diffTimeZone(inOld, inNew, tracker);
                    }
                    break;

                case T_DAY_TIME_INTERVAL:      // day-time-interval
                    // days, hours, minutes, seconds, nanos
                    diffPackedInts(inOld, inNew, tracker, 5);
                    break;

                case T_COLLECTION:             // collection
                case T_ARRAY:                  // array
                    diffCollection(inOld, inNew, tracker);
                    break;

                case T_UNIFORM_COLLECTION:     // uniform-collection
                case T_UNIFORM_ARRAY:          // uniform-array
                    diffUniformCollection(inOld, inNew, tracker);
                    break;

                case T_SPARSE_ARRAY:           // sparse-array
                    diffSparseArray(inOld, inNew, tracker);
                    break;

                case T_UNIFORM_SPARSE_ARRAY:   // uniform-sparse-array
                    diffUniformSparseArray(inOld, inNew, tracker);
                    break;

                case T_MAP:                    // map
                    diffMap(inOld, inNew, tracker);
                    break;

                case T_UNIFORM_KEYS_MAP:       // uniform-keys-map
                    diffUniformKeysMap(inOld, inNew, tracker);
                    break;

                case T_UNIFORM_MAP:            // uniform-map
                    diffUniformMap(inOld, inNew, tracker);
                    break;

                case T_REFERENCE:              // reference
                    diffPackedInt(inOld, inNew, tracker);
                    break;

                case V_BOOLEAN_FALSE:          // boolean:false
                case V_BOOLEAN_TRUE:           // boolean:true
                case V_STRING_ZERO_LENGTH:     // string:zero-length
                case V_COLLECTION_EMPTY:       // collection:empty
                case V_REFERENCE_NULL:         // reference:null
                case V_FP_POS_INFINITY:        // floating-point:+infinity
                case V_FP_NEG_INFINITY:        // floating-point:-infinity
                case V_FP_NAN:                 // floating-point:NaN
                case V_INT_NEG_1:              // int:-1
                case V_INT_0:                  // int:0
                case V_INT_1:                  // int:1
                case V_INT_2:                  // int:2
                case V_INT_3:                  // int:3
                case V_INT_4:                  // int:4
                case V_INT_5:                  // int:5
                case V_INT_6:                  // int:6
                case V_INT_7:                  // int:7
                case V_INT_8:                  // int:8
                case V_INT_9:                  // int:9
                case V_INT_10:                 // int:10
                case V_INT_11:                 // int:11
                case V_INT_12:                 // int:12
                case V_INT_13:                 // int:13
                case V_INT_14:                 // int:14
                case V_INT_15:                 // int:15
                case V_INT_16:                 // int:16
                case V_INT_17:                 // int:17
                case V_INT_18:                 // int:18
                case V_INT_19:                 // int:19
                case V_INT_20:                 // int:20
                case V_INT_21:                 // int:21
                case V_INT_22:                 // int:22
                    // identical by identity
                    tracker.advance(true);
                    break;

                case T_IDENTITY:               // identity
                default:
                    throw new IllegalStateException("nType=" + nType + ", old/new offset="
                            + inOld.getOffset() + "/" + inNew.getOffset());
                }
            }
        }

    /**
    * Within the two passed POF streams, parse and compare a user type value.
    *
    * @param inOld    the BufferInput to read from
    * @param inNew    the BufferInput to read from
    * @param tracker  the ChangeTracker that computes the diff result
    *
    * @throws IOException  if an I/O error occurs
    */
    protected void diffUserType(BufferInput inOld,
                                BufferInput inNew,
                                ChangeTracker tracker)
            throws IOException
        {
        // the user type and the sparse array have the same structure, except
        // that the user type leads with a version while the sparse array
        // leads with an element count (packed ints in both cases)
        diffSparseArray(inOld, inNew, tracker);
        }

    /**
    * Within the two passed POF streams, parse and compare an array or
    * collection.
    *
    * @param inOld    the BufferInput to read from
    * @param inNew    the BufferInput to read from
    * @param tracker  the ChangeTracker that computes the diff result
    *
    * @throws IOException  if an I/O error occurs
    */
    protected void diffCollection(BufferInput inOld,
                                  BufferInput inNew,
                                  ChangeTracker tracker)
            throws IOException
        {
        int cOld = inOld.readPackedInt();
        int cNew = inNew.readPackedInt();
        boolean fSame = cOld == cNew;
        tracker.advance(fSame);

        for (int i = 0, c = Math.min(cOld, cNew); i < c; ++i)
            {
            diffValue(inOld, inNew, tracker);
            }

        if (!fSame)
            {
            BufferInput in;
            int iFrom, iTo;
            if (cOld > cNew)
                {
                in    = inOld;
                iFrom = cNew;
                iTo   = cOld;
                }
            else
                {
                in    = inNew;
                iFrom = cOld;
                iTo   = cNew;
                }

            for (int i = iFrom; i < iTo; ++i)
                {
                PofHelper.skipValue(in);
                }
            tracker.advance(false);
            }
        }

    /**
    * Within the two passed POF streams, parse and compare an array or
    * collection of uniform types.
    *
    * @param inOld    the BufferInput to read from
    * @param inNew    the BufferInput to read from
    * @param tracker  the ChangeTracker that computes the diff result
    *
    * @throws IOException  if an I/O error occurs
    */
    protected void diffUniformCollection(BufferInput inOld,
                                         BufferInput inNew,
                                         ChangeTracker tracker)
            throws IOException
        {
        int nOldType = inOld.readPackedInt();
        int nNewType = inNew.readPackedInt();
        boolean fSameType = nOldType == nNewType;
        tracker.advance(fSameType);

        int cOld = inOld.readPackedInt();
        int cNew = inNew.readPackedInt();
        boolean fSameCount = cOld == cNew;
        tracker.advance(fSameCount);

        if (fSameType)
            {
            for (int i = 0, c = Math.min(cOld, cNew); i < c; ++i)
                {
                diffUniformValue(inOld, inNew, tracker, nOldType);
                }

            if (!fSameCount)
                {
                BufferInput in;
                int iFrom, iTo;
                if (cOld > cNew)
                    {
                    in    = inOld;
                    iFrom = cNew;
                    iTo   = cOld;
                    }
                else
                    {
                    in    = inNew;
                    iFrom = cOld;
                    iTo   = cNew;
                    }

                for (int i = iFrom; i < iTo; ++i)
                    {
                    PofHelper.skipUniformValue(in, nOldType);
                    }
                tracker.advance(false);
                }
            }
        else
            {
            for (int i = 0; i < cOld; ++i)
                {
                PofHelper.skipUniformValue(inOld, nOldType);
                }
            for (int i = 0; i < cNew; ++i)
                {
                PofHelper.skipUniformValue(inNew, nNewType);
                }
            tracker.advance(false);
            }
        }

    /**
    * Within the two passed POF streams, parse and compare a sparse array.
    *
    * @param inOld    the BufferInput to read from
    * @param inNew    the BufferInput to read from
    * @param tracker  the ChangeTracker that computes the diff result
    *
    * @throws IOException  if an I/O error occurs
    */
    protected void diffSparseArray(BufferInput inOld,
                                   BufferInput inNew,
                                   ChangeTracker tracker)
            throws IOException
        {
        // the user type and the sparse array have the same structure, except
        // that the user type leads with a version while the sparse array
        // leads with an element count (packed ints in both cases)
        diffPackedInt(inOld, inNew, tracker);

        while (true)
            {
            // might have to back up if the element indexes don't match
            inOld.mark(5);
            inNew.mark(5);

            // load the next element index from each stream
            int nNextOld = inOld.readPackedInt();
            int nNextNew = inNew.readPackedInt();
            if (nNextOld == nNextNew)
                {
                // same element index for both streams
                tracker.advance(true);

                if (nNextOld == -1)
                    {
                    // end of the sparse array (both indexes are -1)
                    break;
                    }
                else
                    {
                    diffValue(inOld, inNew, tracker);
                    }
                }
            else
                {
                // one of the streams is either exhausted (i.e. its next
                // element index is -1) or it's "ahead" of the other stream
                // (i.e. its next element index is greater than the other
                // stream's); in either case, reset() that stream (i.e. back
                // up on the stream so that the next thing to read is that
                // element index), and skip the value on the other stream;
                // doing this will eventually get the streams in sync (which
                // includes the possibility of eventually exhausting both
                // streams)
                BufferInput inReset, inSkip;
                if (nNextOld == -1 || (nNextOld > nNextNew && nNextNew != -1))
                    {
                    inReset = inOld;
                    inSkip  = inNew;
                    }
                else
                    {
                    inReset = inNew;
                    inSkip  = inOld;
                    }
                inReset.reset();
                PofHelper.skipValue(inSkip);
                tracker.advance(false);
                }
            }
        }

    /**
    * Within the two passed POF streams, parse and compare a sparse array of
    * uniform types.
    *
    * @param inOld    the BufferInput to read from
    * @param inNew    the BufferInput to read from
    * @param tracker  the ChangeTracker that computes the diff result
    *
    * @throws IOException  if an I/O error occurs
    */
    protected void diffUniformSparseArray(BufferInput inOld,
                                          BufferInput inNew,
                                          ChangeTracker tracker)
            throws IOException
        {
        int nOldType = inOld.readPackedInt();
        int nNewType = inNew.readPackedInt();
        boolean fSameType = nOldType == nNewType;
        tracker.advance(fSameType);

        int cOld = inOld.readPackedInt();
        int cNew = inNew.readPackedInt();
        boolean fSameCount = cOld == cNew;
        tracker.advance(fSameCount);

        if (fSameType)
            {
            while (true)
                {
                // might have to back up if the element indexes don't match
                inOld.mark(5);
                inNew.mark(5);

                // load the next element index from each stream
                int nNextOld = inOld.readPackedInt();
                int nNextNew = inNew.readPackedInt();
                if (nNextOld == nNextNew)
                    {
                    // same element index for both streams
                    tracker.advance(true);

                    if (nNextOld == -1)
                        {
                        // end of the sparse array (both indexes are -1)
                        break;
                        }
                    else
                        {
                        diffUniformValue(inOld, inNew, tracker, nOldType);
                        }
                    }
                else
                    {
                    // one of the streams is either exhausted (i.e. its next
                    // element index is -1) or it's "ahead" of the other stream
                    // (i.e. its next element index is greater than the other
                    // stream's); in either case, reset() that stream (i.e. back
                    // up on the stream so that the next thing to read is that
                    // element index), and skip the value on the other stream;
                    // doing this will eventually get the streams in sync (which
                    // includes the possibility of eventually exhausting both
                    // streams)
                    BufferInput inReset, inSkip;
                    if (nNextOld == -1 || nNextOld > nNextNew)
                        {
                        inReset = inOld;
                        inSkip  = inNew;
                        }
                    else
                        {
                        inReset = inNew;
                        inSkip  = inOld;
                        }
                    inReset.reset();
                    PofHelper.skipUniformValue(inSkip, nOldType);
                    tracker.advance(false);
                    }
                }
            }
        else
            {
            for (int i = 0; i < cOld; ++i)
                {
                if (inOld.readPackedInt() < 0)
                    {
                    break;
                    }
                PofHelper.skipUniformValue(inOld, nOldType);
                }
            for (int i = 0; i < cNew; ++i)
                {
                if (inNew.readPackedInt() < 0)
                    {
                    break;
                    }
                PofHelper.skipUniformValue(inNew, nNewType);
                }
            tracker.advance(false);
            }
        }

    /**
    * Within the two passed POF streams, parse and compare a Map of keys
    * and values.
    *
    * @param inOld    the BufferInput to read from
    * @param inNew    the BufferInput to read from
    * @param tracker  the ChangeTracker that computes the diff result
    *
    * @throws IOException  if an I/O error occurs
    */
    protected void diffMap(BufferInput inOld,
                           BufferInput inNew,
                           ChangeTracker tracker)
            throws IOException
        {
        int cOld = inOld.readPackedInt();
        int cNew = inNew.readPackedInt();

        boolean fSame = cOld == cNew;
        tracker.advance(fSame);

        for (int i = 0, c = Math.min(cOld, cNew); i < c; ++i)
            {
            diffValue(inOld, inNew, tracker);  // key
            diffValue(inOld, inNew, tracker);  // value
            }

        if (!fSame)
            {
            BufferInput in;
            int iFrom, iTo;
            if (cOld > cNew)
                {
                in    = inOld;
                iFrom = cNew;
                iTo   = cOld;
                }
            else
                {
                in    = inNew;
                iFrom = cOld;
                iTo   = cNew;
                }

            for (int i = iFrom; i < iTo; ++i)
                {
                PofHelper.skipValue(in);    // key
                PofHelper.skipValue(in);    // value
                }
            tracker.advance(false);
            }
        }

    /**
    * Within the two passed POF streams, parse and compare a Map of keys (of
    * a uniform type) and values.
    *
    * @param inOld    the BufferInput to read from
    * @param inNew    the BufferInput to read from
    * @param tracker  the ChangeTracker that computes the diff result
    *
    * @throws IOException  if an I/O error occurs
    */
    protected void diffUniformKeysMap(BufferInput inOld,
                                      BufferInput inNew,
                                      ChangeTracker tracker)
            throws IOException
        {
        int nOldType = inOld.readPackedInt();
        int nNewType = inNew.readPackedInt();
        boolean fSameType = nOldType == nNewType;
        tracker.advance(fSameType);

        int cOld = inOld.readPackedInt();
        int cNew = inNew.readPackedInt();
        boolean fSameCount = cOld == cNew;
        tracker.advance(fSameCount);

        for (int i = 0, c = Math.min(cOld, cNew); i < c; ++i)
            {
            // diff the key
            if (fSameType)
                {
                diffUniformValue(inOld, inNew, tracker, nOldType);  // key
                }
            else
                {
                PofHelper.skipUniformValue(inOld, nOldType);
                PofHelper.skipUniformValue(inNew, nNewType);
                tracker.advance(false);
                }

            diffValue(inOld, inNew, tracker);                       // value
            }

        if (!fSameCount)
            {
            BufferInput in;
            int iFrom, iTo, nType;
            if (cOld > cNew)
                {
                in    = inOld;
                iFrom = cNew;
                iTo   = cOld;
                nType = nOldType;
                }
            else
                {
                in    = inNew;
                iFrom = cOld;
                iTo   = cNew;
                nType = nNewType;
                }

            for (int i = iFrom; i < iTo; ++i)
                {
                PofHelper.skipUniformValue(in, nType);  // key
                PofHelper.skipValue(in);                // value
                }
            tracker.advance(false);
            }
        }

    /**
    * Within the two passed POF streams, parse and compare a Map of keys
    * and values, both of uniform types.
    *
    * @param inOld    the BufferInput to read from
    * @param inNew    the BufferInput to read from
    * @param tracker  the ChangeTracker that computes the diff result
    *
    * @throws IOException  if an I/O error occurs
    */
    protected void diffUniformMap(BufferInput inOld,
                                  BufferInput inNew,
                                  ChangeTracker tracker)
            throws IOException
        {
        int nOldKeyType = inOld.readPackedInt();
        int nNewKeyType = inNew.readPackedInt();
        boolean fSameKeyType = nOldKeyType == nNewKeyType;
        tracker.advance(fSameKeyType);

        int nOldValType = inOld.readPackedInt();
        int nNewValType = inNew.readPackedInt();
        boolean fSameValType = nOldValType == nNewValType;
        tracker.advance(fSameValType);

        int cOld = inOld.readPackedInt();
        int cNew = inNew.readPackedInt();
        boolean fSameCount = cOld == cNew;
        tracker.advance(fSameCount);

        if (fSameKeyType || fSameValType)
            {
            for (int i = 0, c = Math.min(cOld, cNew); i < c; ++i)
                {
                if (fSameKeyType)
                    {
                    diffUniformValue(inOld, inNew, tracker, nOldKeyType);
                    }
                else
                    {
                    PofHelper.skipUniformValue(inOld, nOldKeyType);
                    PofHelper.skipUniformValue(inNew, nNewKeyType);
                    tracker.advance(false);
                    }

                if (fSameValType)
                    {
                    diffUniformValue(inOld, inNew, tracker, nOldValType);
                    }
                else
                    {
                    PofHelper.skipUniformValue(inOld, nOldValType);
                    PofHelper.skipUniformValue(inNew, nNewValType);
                    tracker.advance(false);
                    }
                }

            if (!fSameCount)
                {
                BufferInput in;
                int iFrom, iTo, nKeyType, nValType;
                if (cOld > cNew)
                    {
                    in       = inOld;
                    iFrom    = cNew;
                    iTo      = cOld;
                    nKeyType = nOldKeyType;
                    nValType = nOldValType;
                    }
                else
                    {
                    in       = inNew;
                    iFrom    = cOld;
                    iTo      = cNew;
                    nKeyType = nNewKeyType;
                    nValType = nNewValType;
                    }

                for (int i = iFrom; i < iTo; ++i)
                    {
                    PofHelper.skipUniformValue(in, nKeyType);   // key
                    PofHelper.skipUniformValue(in, nValType);   // value
                    }
                tracker.advance(false);
                }
            }
        else
            {
            for (int i = 0; i < cOld; ++i)
                {
                PofHelper.skipUniformValue(inOld, nOldKeyType); // key
                PofHelper.skipUniformValue(inOld, nOldValType); // value
                }
            for (int i = 0; i < cNew; ++i)
                {
                PofHelper.skipUniformValue(inNew, nNewKeyType); // key
                PofHelper.skipUniformValue(inNew, nNewValType); // value
                }
            tracker.advance(false);
            }
        }

    /**
    * Within the two passed POF streams, parse and compare time zone
    * information.
    *
    * @param inOld    the BufferInput to read from
    * @param inNew    the BufferInput to read from
    * @param tracker  the ChangeTracker that computes the diff result
    *
    * @throws IOException  if an I/O error occurs
    */
    protected void diffTimeZone(BufferInput inOld,
                                BufferInput inNew,
                                ChangeTracker tracker)
            throws IOException
        {
        int nOldZoneType = inOld.readPackedInt();
        int nNewZoneType = inNew.readPackedInt();
        boolean fSame = nOldZoneType == nNewZoneType;
        if (fSame)
            {
            tracker.advance(true);
            if (nOldZoneType == 2)
                {
                diffPackedInts(inOld, inNew, tracker, 2);
                }
            }
        else
            {
            if (nOldZoneType == 2)
                {
                PofHelper.skipPackedInts(inOld, 2);
                }
            else if (nNewZoneType == 2)
                {
                PofHelper.skipPackedInts(inNew, 2);
                }
            tracker.advance(false);
            }
        }

    /**
    * Within the two passed POF streams, parse and compare a series of packed
    * integer values.
    *
    * @param inOld    the BufferInput to read from
    * @param inNew    the BufferInput to read from
    * @param tracker  the ChangeTracker that computes the diff result
    * @param cInts    the number of packed integers to parse and compare
    *
    * @throws IOException  if an I/O error occurs
    */
    protected void diffPackedInts(BufferInput inOld,
                                  BufferInput inNew,
                                  ChangeTracker tracker,
                                  int cInts)
            throws IOException
        {
        for (int i = 0; i < cInts; ++i)
            {
            diffPackedInt(inOld, inNew, tracker);
            }
        }

    /**
    * Within the two passed POF streams, parse and compare a packed integer
    * value.
    *
    * @param inOld    the BufferInput to read from
    * @param inNew    the BufferInput to read from
    * @param tracker  the ChangeTracker that computes the diff result
    *
    * @throws IOException  if an I/O error occurs
    */
    protected void diffPackedInt(BufferInput inOld,
                                 BufferInput inNew,
                                 ChangeTracker tracker)
            throws IOException
        {
        tracker.advance(inOld.readPackedInt() == inNew.readPackedInt());
        }

    /**
    * Within the two passed POF streams, parse and compare a series of packed
    * long integer values.
    *
    * @param inOld    the BufferInput to read from
    * @param inNew    the BufferInput to read from
    * @param tracker  the ChangeTracker that computes the diff result
    * @param cInts    the number of packed long integers to parse and compare
    *
    * @throws IOException  if an I/O error occurs
    */
    protected void diffPackedLongs(BufferInput inOld,
                                   BufferInput inNew,
                                   ChangeTracker tracker,
                                   int cInts)
            throws IOException
        {
        for (int i = 0; i < cInts; ++i)
            {
            diffPackedLong(inOld, inNew, tracker);
            }
        }

    /**
    * Within the two passed POF streams, parse and compare a packed long
    * integer value.
    *
    * @param inOld    the BufferInput to read from
    * @param inNew    the BufferInput to read from
    * @param tracker  the ChangeTracker that computes the diff result
    *
    * @throws IOException  if an I/O error occurs
    */
    protected void diffPackedLong(BufferInput inOld,
                                  BufferInput inNew,
                                  ChangeTracker tracker)
            throws IOException
        {
        tracker.advance(inOld.readPackedLong() == inNew.readPackedLong());
        }

    /**
    * When determining a delta between two POF buffers, the ChangeTracker
    * keeps track of whether the current location within the two POF streams
    * is part of a differing portion or part of an identical portion.
    */
    protected static class ChangeTracker
        {
        // ----- constructors -------------------------------------------

        /**
        * Construct a ChangeTracker that will produce a delta between the
        * two passed streams.
        *
        * @param inOld  the BuferInput for the old value
        * @param inNew  the BuferInput for the new value
        *
        * @throws IOException  if some use of the underlying streams throws
        *                      an exception
        */
        public ChangeTracker(BufferInput inOld, BufferInput inNew)
                throws IOException
            {
            m_inOld = inOld;
            m_inNew = inNew;

            m_outDelta = new BinaryWriteBuffer(Math.max(64, Math.min(1024,
                    Math.max(inOld.getBuffer().length(), inNew.getBuffer().length()))))
                    .getAppendingBufferOutput();

            // initialize the delta format
            m_outDelta.write(FMT_BINDIFF);
            }

        // ----- public methods -----------------------------------------

        /**
        * Update the tracker to indicate that the most recently scanned
        * region was the same or different.
        *
        * @param fSame  pass true if the most recently scanned region of the
        *               streams was identical; false otherwise
        *
        * @throws IOException  if some use of the underlying streams throws
        *                      an exception
        */
        public void advance(boolean fSame)
                throws IOException
            {
            update(fSame ? SAME : DIFF);
            }

        /**
        * Obtain the final delta result as a Binary value.
        *
        * @return a Binary containing the delta value
        *
        * @throws IOException  if some use of the underlying streams throws
        *                      an exception
        */
        public Binary getDelta()
                throws IOException
            {
            BufferOutput outDelta = m_outDelta;

            // flush the remainder of the diff to the delta stream
            update(FINAL);

            // finalize the delta format
            outDelta.write(OP_TERM);

            return outDelta.getBuffer().toBinary();
            }

        // ----- internal -----------------------------------------------

        /**
        * Update the tracker to indicate that the most recently scanned
        * region was the same or different.
        *
        * @param nUpdate  one of SAME, DIFF or FINAL
        *
        * @throws IOException  if some use of the underlying streams throws
        *                      an exception
        */
        private void update(int nUpdate)
                throws IOException
            {
            int ofCurrOld = m_inOld.getOffset();
            int ofCurrNew = m_inNew.getOffset();

            int ofLastOldDiff = m_ofLastOldDiff;
            int ofLastOldSame = m_ofLastOldSame;
            int ofLastNewDiff = m_ofLastNewDiff;
            int ofLastNewSame = m_ofLastNewSame;

            boolean fPrevSame = ofLastOldSame > ofLastOldDiff
                             || ofLastNewSame > ofLastNewDiff;

            // to force a flush out the remainder of the delta, a change is
            // simulated to toggle from a "diff" to a "same" region or
            // vice-versa
            boolean fLast = nUpdate == FINAL;
            boolean fSame = fLast ? !fPrevSame : nUpdate == SAME;

            if (fSame || fLast)
                {
                // this is the processing for both diff-same and same-same;
                // if the cummulative "same" block is long enough, then it
                // qualifies as an actual "same" block, thus finalizing any
                // diff block that came before it
                int cbSame = ofCurrNew - ofLastNewDiff;
                if ((cbSame > MIN_BLOCK || fLast) && ofLastNewDiff > m_ofLastNewWrite)
                    {
                    // there was a diff in the region between ofLastNewWrite
                    // and ofLastNewDiff; determine the data that makes up
                    // the diff
                    int of = m_ofLastNewWrite;
                    int cb = ofLastNewDiff - of;

                    // write out the diff
                    BufferOutput outDelta = m_outDelta;
                    outDelta.write(OP_APPEND);

                    // Hre we write the length of... something..
                    outDelta.writePackedInt(cb);

                    // Here we write... something with cb length into the buffer
                    outDelta.writeBuffer(m_inNew.getBuffer(), of, cb);

                    // update the offset to which we have completed (i.e. to
                    // which we have placed information into the final delta
                    // stream for)
                    m_ofLastNewWrite = ofLastNewDiff;
                    }
                }

            if (fSame)
                {
                // update the length of the current "same" region to the
                // current stream offsets
                m_ofLastOldSame = ofCurrOld;
                m_ofLastNewSame = ofCurrNew;
                }
            else
                {
                if (fPrevSame)
                    {
                    // this is the processing for same-diff, i.e. the first
                    // "diff" block following a "same" block. if the "same"
                    // block was long enough, it becomes a copy, otherwise it
                    // just becomes part of a longer diff block (whatever
                    // "diff" block was already in progress when the "same"
                    // block was originally encountered)

                    // note that there is some special handling here, because
                    // the "same" block is being explicitly copied from the
                    // old value and not the new value, since it's
                    // theoretically possible that the binaries for the two
                    // differ slightly, even though they were perceived as
                    // being "same". one simple example is the optional
                    // optimizations that encode integers in a total of one
                    // byte, allowing one stream to contain a 1-byte integer
                    // and another to contain a 2-byte integer, but both to
                    // evaluate to identical POF values; because of that
                    // possibility, care must be taken to copy from the
                    // corresponding offset and length of the old stream

                    int of = m_ofLastOldDiff;
                    int cb = ofLastOldSame - of;
                    if (cb > 0)
                        {
                        if (cb > MIN_BLOCK || fLast)
                            {
                            // there was a "same" block that needs to be
                            // recorded to the delta stream
                            BufferOutput outDelta = m_outDelta;
                            outDelta.write(OP_EXTRACT);
                            outDelta.writePackedInt(of);
                            outDelta.writePackedInt(cb);

                            // update the offset to which we have completed
                            m_ofLastNewWrite = ofLastNewSame;
                            }
                        else
                            {
                            // the previous "same" block wasn't long enough
                            // to qualify as a "same" block, so merge it
                            // into whatever "diff" block came before it (by
                            // pretending that there have been no "same"
                            // blocks)
                            m_ofLastOldSame = 0;
                            m_ofLastNewSame = 0;
                            }
                        }
                    }

                // update the length of the current "diff" region to the
                // current stream offsets
                m_ofLastOldDiff = ofCurrOld;
                m_ofLastNewDiff = ofCurrNew;
                }
            }

        // ----- constants ----------------------------------------------

        /**
        * Update type: most recently parsed region is identical in both
        * streams.
        */
        private static final int SAME  = 0;

        /**
        * Update type: most recently parsed region is different between the
        * streams.
        */
        private static final int DIFF  = 1;

        /**
        * Update type: the streams are exhausted; finalize the delta.
        */
        private static final int FINAL = 2;

        // ----- data members -------------------------------------------

        /**
        * The stream containing the old value.
        */
        private final BufferInput m_inOld;

        /**
        * The stream containing the new value.
        */
        private final BufferInput m_inNew;

        /**
        * The stream containing the delta value.
        */
        private final BufferOutput m_outDelta;

        /**
        * The offset in the old stream of the last advance that was a diff.
        */
        private int m_ofLastOldDiff;

        /**
        * The offset in the old stream of the last advance that was the same.
        */
        private int m_ofLastOldSame;

        /**
        * The offset in the new stream of the next byte that has not been
        * committed to the delta stream.
        */
        private int m_ofLastNewWrite;

        /**
        * The offset in the new stream of the last advance that was a diff.
        */
        private int m_ofLastNewDiff;

        /**
        * The offset in the new stream of the last advance that was the same.
        */
        private int m_ofLastNewSame;
        }
    }
