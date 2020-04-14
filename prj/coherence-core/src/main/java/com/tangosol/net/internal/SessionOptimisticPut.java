/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.internal;


import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.ReadBuffer;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.BinaryWriteBuffer;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap;

import com.tangosol.util.processor.AbstractProcessor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;


/**
* OptimisticPut is an EntryProcessor that performs an {@link
* com.tangosol.util.InvocableMap.Entry#setValue(Object) Entry.setValue}
* operation if the entry does not already exist or if the version identifier
* associated with the current session is equal to the specified version
* identifier.
*
* @author jh  2008.12.11
* @since Coherence 3.5 renamed from OptimisticBinaryPut
*/
public class SessionOptimisticPut
        extends AbstractProcessor
        implements ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public SessionOptimisticPut()
        {
        }

    /**
    * Create a new OptimisticPut that will update or insert an entry iff the
    * entry does not already exist or the version identifier associated with
    * the current value is equal to the specified version identifier.
    *
    * @param binValue  the new value of the entry; must not be null
    * @param nVersion  the expected version identifier; must be >= 0
    * @param fNoLock   flag denoting whether session locking is disabled
    */
    public SessionOptimisticPut(Binary binValue, int nVersion, boolean fNoLock)
        {
        if (binValue == null || nVersion < 0)
            {
            throw new IllegalArgumentException();
            }
        m_binValue = binValue;
        m_fNoLock  = fNoLock;
        }


    // ----- EntryProcessor interface ---------------------------------------

    /**
    * Process a Map.Entry object.
    *
    * @param entry  the Entry to process
    *
    * @return the new version identifier (>0) if the update succeeded;
    *         otherwise, the negated value of the current version identifier
    *         (<=0)
    */
    public Object process(InvocableMap.Entry entry)
        {
        BinaryEntry binentry      = (BinaryEntry) entry;
        boolean     fOldExists    = entry.isPresent();
        Binary      binSessionOld = fOldExists ? binentry.getBinaryValue() : null;
        Binary      binSessionNew = m_binValue;

        // determine the current version identifier
        int nVersionOld = fOldExists
                ? extractVersion(binSessionOld)
                : 0;    // the session does not exist, so its version is 0

        // determine the new version identifier
        int nVersionNew = extractVersion(binSessionNew);

        // determine whether the version matches; if it does then commit
        // the session changes and return the new version number, otherwise
        // just return the negative of the version number that was found
        int nVersionRet;
        if (nVersionOld + 1 == nVersionNew)
            {
            binentry.updateBinaryValue(binSessionNew);
            nVersionRet = nVersionNew;
            }
        else if (!fOldExists)
            {
            // must have been removed (COH-2543)
            nVersionRet = 0;
            }
        else
            {
            int ofOld = SessionExpiryExtractor.validateBinarySession(binSessionOld);
            int ofNew = SessionExpiryExtractor.validateBinarySession(binSessionNew);
            int cbOld = binSessionOld.length();
            int cbNew = binSessionNew.length();
            if (isNoLock() || (ofOld == ofNew && cbOld == cbNew &&
                    getBinaryData(binSessionOld, ofOld, cbOld).equals(
                    getBinaryData(binSessionNew, ofNew, cbNew))))
                {
                // the sessions are identical other than version and
                // timestamps; merge the two sessions
                int nVersionMax = Math.max(nVersionNew, nVersionOld);
                BinaryWriteBuffer bufNew = new BinaryWriteBuffer(cbNew, cbNew);
                try
                    {
                    bufNew.write(0, binSessionNew);
                    bufNew.getBufferOutput(ofNew + 4).writeLong(
                            getMaxTimestamp(4, binSessionOld, binSessionNew));
                    bufNew.getBufferOutput(ofNew + 12).writeInt(nVersionMax);
                    bufNew.getBufferOutput(ofNew + 16).writeLong(
                            getMaxTimestamp(16, binSessionOld, binSessionNew));
                    }
                catch (IOException e)
                    {
                    throw ensureRuntimeException(e);
                    }
                binentry.updateBinaryValue(bufNew.toBinary());

                // return the "winning" version
                nVersionRet = nVersionMax;
                }
            else
                {
                nVersionRet = -nVersionOld;
                }
            }

        return Integer.valueOf(nVersionRet);
        }


    // ----- internal -------------------------------------------------------

    /**
    * Get the maximum timestamp (long) from 2 serialized session models at
    * the given offset.
    *
    * @param offset       the offset
    * @param binSession1  a serialized session
    * @param binSession2  a serialized session
    *
    * @return the maximum from the 2 models
    */
    private long getMaxTimestamp(int offset, Binary binSession1, Binary binSession2)
        {
        return Math.max(extractTimestamp(binSession1, offset),
                extractTimestamp(binSession2, offset));
        }

    /**
    * Extract the data part of a serialized session model.
    *
    * @param binSession  the session
    * @param of          the offset of the data
    * @param cb          the buffer length
    *
    * @return  the data part of a serialized session model.
    */
    private Binary getBinaryData(Binary binSession, int of, int cb)
        {
        return binSession.toBinary(of + 24, cb - (of + 24));
        }

    /**
    * Obtain the version indicator from a serialized session model.
    *
    * @param binSession  a serialized session model
    *
    * @return the version of the session
    */
    protected static int extractVersion(Binary binSession)
        {
        try
            {
            int of = SessionExpiryExtractor.validateBinarySession(binSession);
            ReadBuffer.BufferInput in = binSession.getBufferInput();
            in.setOffset(of + 12);
            return in.readInt();
            }
        catch (IOException e)
            {
            throw ensureRuntimeException(e);
            }
        }

    /**
    * Obtain the timestamp from a serialized session model at the specified
    * offset.
    *
    * @param binSession  a serialized session model
    * @param of          the timestamp offset
    *
    * @return the timestamp
    */
    private static long extractTimestamp(Binary binSession, int of)
        {
        try
            {
            of += SessionExpiryExtractor.validateBinarySession(binSession);
            ReadBuffer.BufferInput in = binSession.getBufferInput();
            in.setOffset(of);
            return in.readLong();
            }
        catch (IOException e)
            {
            throw ensureRuntimeException(e);
            }
        }


    // ----- ExternalizableLite ---------------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(DataInput in)
            throws IOException
        {
        m_binValue = (Binary) ExternalizableHelper.readObject(in);
        m_fNoLock  = in.readBoolean();
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        ExternalizableHelper.writeObject(out, m_binValue);
        out.writeBoolean(m_fNoLock);
        }


    // ----- PortableObject interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        m_binValue = in.readBinary(0);
        m_fNoLock  = in.readBoolean(1);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeBinary(0, m_binValue);
        out.writeBoolean(1, m_fNoLock);
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Determine if session locking is disabled.
    * <p>
    * If true, updates will succceed even if the version information implies
    * that another update has occurred since the model was read.
    *
    * @return true if session locking is disabled
    */
    protected boolean isNoLock()
        {
        return m_fNoLock;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The binary form of the new value.
    */
    public Binary m_binValue;

    /**
    * Flag denoting whether session locking is disabled.
    */
    public boolean m_fNoLock;
    }
