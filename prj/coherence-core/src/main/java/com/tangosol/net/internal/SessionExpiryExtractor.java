/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.internal;


import com.tangosol.io.ReadBuffer;

import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.ExternalizableHelper;

import com.tangosol.util.extractor.EntryExtractor;

import java.io.InvalidClassException;
import java.io.IOException;

import java.util.Map;


/**
* The SessionExpiryExtractor class is a ValueExtractor implementation that
* acts against an AbstractHttpSessionModel to determine when it expires,
* returning a datetime value as a Long.
*
* @author cp  2009.04.07
* @since Coherence 3.5
*/
public class SessionExpiryExtractor
        extends EntryExtractor
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public SessionExpiryExtractor()
        {
        }


    // ----- EntryExtractor -------------------------------------------------

    /**
    * {@inheritDoc}
    */
    public Object extractFromEntry(Map.Entry entry)
        {
        try
            {
            Binary binSession = ((BinaryEntry) entry).getBinaryValue();
            if (binSession == null)
                {
                // this case may happen when have deleted the session on another node using an entry processor etc and the current node is trying to get the value
                return NEVER_EXPIRES;
                }
            int of = validateBinarySession(binSession);

            // @see AbstractHttpSessionModel#readExternal
            // @see AbstractHttpSessionModel#writeExternal
            ReadBuffer.BufferInput in = binSession.getBufferInput();
            in.setOffset(of + 4);
            long ldtAccessed = in.readLong();
            in.setOffset(of + 32);
            int cMaxSeconds = in.readInt();

            // @see AbstractHttpSessionModel#isExpired()
            return cMaxSeconds < 0
                   ? NEVER_EXPIRES
                   : Long.valueOf(ldtAccessed + (cMaxSeconds * 1000L));
            }
        catch (IOException e)
            {
            throw ensureRuntimeException(e);
            }
        }


    // ----- helpers --------------------------------------------------------

    /**
    * Validate that the binary is a session model of a supported version.
    * Failure to validate it will result in a runtime exception.
    *
    * @param binSession  the session binary
    *
    * @return the offset within the specified Binary at which the session
    *         model data begins
    */
    public static int validateBinarySession(Binary binSession)
        {
        try
            {
            ReadBuffer.BufferInput in = binSession.getBufferInput();

            if (isDecorated(binSession))
                {
                // first byte indicates the decoration
                // second byte indicates what decorations exist (with the
                // first bit representing the presence of a "value")
                byte bFmt  = in.readByte();
                long nMask = bFmt == FMT_BIN_DECO ? in.readUnsignedByte() : in.readPackedLong();

                if (!(bFmt == FMT_BIN_DECO || bFmt == FMT_BIN_EXT_DECO) || (nMask & (1L << DECO_VALUE)) == 0L)
                    {
                    throw new InvalidClassException(
                        "Binary session model is decorated incorrectly.");
                    }

                // next comes the length of the "value"
                in.readPackedInt();
                }

            // the Binary is a serialized ExternalizableLite object:
            // [0] : ExternalizableHelper.FMT_OBJ_EXT
            // [1] : class name (UTF string)
            if (in.readUnsignedByte() != ExternalizableHelper.FMT_OBJ_EXT)
                {
                throw new InvalidClassException(
                        "Binary session model is not a serialized " +
                        "ExternalizableLite object");
                }
            // skip the class name; see ExternalizableHelper.readUTF()
            in.skip(Math.max(0, in.readPackedInt()));

            // remeber the offset of the start of the session model data
            int of = in.getOffset();

            // @see AbstractHttpSessionModel#readExternal
            // @see AbstractHttpSessionModel#writeExternal
            if (in.readInt() != MAGIC_V350)
                {
                throw new InvalidClassException(
                    "Session management version conflict; the Coherence*Web " +
                    "cluster must be upgraded to a consistent version.");
                }

            return of;
            }
        catch (IOException e)
            {
            throw ensureRuntimeException(e);
            }
        }


    // ----- constants ------------------------------------------------------

    /**
    * Coherence*Web version identifier for serialization.
    * <p>
    * Note: This must be the same as {@link
    * com.tangosol.coherence.servlet.AbstractHttpSessionModel#MAGIC_V350}.
    */
    public static final int MAGIC_V350 = 0x6A687768;

    /**
    * The time when a session will expire if it never expires.
    */
    public static final Long NEVER_EXPIRES = Long.valueOf(Long.MAX_VALUE);
    }


