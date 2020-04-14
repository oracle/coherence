/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.internal;


import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.ReadBuffer;
import com.tangosol.io.WriteBuffer;

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
* SessionUpdateTimestamp is an EntryProcessor that stamps a new last accessed
* and last flushed timestamp on a binary session model.
*
* @author cp  2009.04.08
* @since Coherence 3.5
*/
public class SessionUpdateTimestamp
        extends AbstractProcessor
        implements ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public SessionUpdateTimestamp()
        {
        }

    /**
    * Create a new SessionUpdateTimestamp processor that will stamp the
    * passed timestamps onto a session model.
    *
    * @param ldtAccess  the new last accessed timestamp
    * @param ldtFlush   the new last flushed timestamp
    */
    public SessionUpdateTimestamp(long ldtAccess, long ldtFlush)
        {
        if (ldtAccess <= 0L || ldtFlush <= 0L)
            {
            throw new IllegalArgumentException();
            }

        m_ldtAccess = ldtAccess;
        m_ldtFlush  = ldtFlush;
        }


    // ----- EntryProcessor interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public Object process(InvocableMap.Entry entry)
        {
        if (entry.isPresent())
            {
            // determine the current timestamp
            try
                {
                BinaryEntry binentry   = (BinaryEntry) entry;
                Binary      binSession = binentry.getBinaryValue();
                int of = SessionExpiryExtractor.validateBinarySession(binSession);

                // @see AbstractHttpSessionModel#readExternal
                // @see AbstractHttpSessionModel#writeExternal
                ReadBuffer.BufferInput in = binSession.getBufferInput();
                in.setOffset(of + 4);
                long ldtAccessOld = in.readLong();
                long ldtAccessNew = m_ldtAccess;
                in.setOffset(of + 16);
                long ldtFlushOld = in.readLong();
                long ldtFlushNew = m_ldtFlush;
                if (ldtAccessNew > ldtAccessOld || ldtFlushNew > ldtFlushOld)
                    {
                    int cb = binSession.length();

                    // copy the session data
                    BinaryWriteBuffer bufNew = new BinaryWriteBuffer(cb, cb);
                    bufNew.write(0, binSession);

                    // write the max last accessed and last flushed timestamps
                    WriteBuffer.BufferOutput out = bufNew.getBufferOutput();
                    out.setOffset(of + 4);
                    out.writeLong(Math.max(ldtAccessNew, ldtAccessOld));
                    out.setOffset(of + 16);
                    out.writeLong(Math.max(ldtFlushNew, ldtFlushOld));

                    binentry.updateBinaryValue(bufNew.toBinary());
                    }
                }
            catch (IOException e)
                {
                throw ensureRuntimeException(e);
                }
            }

        return null;
        }


    // ----- ExternalizableLite ---------------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(DataInput in)
            throws IOException
        {
        m_ldtAccess = ExternalizableHelper.readLong(in);
        m_ldtFlush  = ExternalizableHelper.readLong(in);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        ExternalizableHelper.writeLong(out, m_ldtAccess);
        ExternalizableHelper.writeLong(out, m_ldtFlush);
        }


    // ----- PortableObject interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        m_ldtAccess = in.readLong(0);
        m_ldtFlush  = in.readLong(1);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeLong(0, m_ldtAccess);
        out.writeLong(1, m_ldtFlush);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The new last accessed timestamp.
    */
    protected long m_ldtAccess;

    /**
    * The new last flushed timestamp.
    */
    protected long m_ldtFlush;
    }
