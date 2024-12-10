/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io;


import com.tangosol.internal.net.MessageComponent;

import com.tangosol.io.ReadBuffer.BufferInput;

import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.IOException;


/**
* This is an imitation BufferInput implementation that provides the
* BufferInput interface by delegating to an object that implements the
* DataInput interface. Primarily, this is intended as a base class for
* building specific-purpose DataInput wrappers.
*
* @author jh  2007.11.17
*/
public class WrapperBufferInput
        extends WrapperDataInputStream
        implements BufferInput
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a WrapperBufferInput that will read from the specified object
    * implementing the DataInput interface.
    *
    * @param in  an object implementing DataInput to read from
    */
    public WrapperBufferInput(DataInput in)
        {
        this(in, null);
        }

    /**
    * Construct a WrapperBufferInput that will read from the specified object
    * implementing the DataInput interface.
    *
    * @param in      an object implementing DataInput to read from
    * @param loader  the ClassLoader to use
    */
    public WrapperBufferInput(DataInput in, ClassLoader loader)
        {
        super(in, loader);

        m_bufIn = in instanceof BufferInput ? (BufferInput) in : null;
        }


    // ----- BufferInput interface ------------------------------------------

    /**
    * {@inheritDoc}
    */
    public ReadBuffer getBuffer()
        {
        BufferInput bufIn = m_bufIn;
        return bufIn == null ? null : bufIn.getBuffer();
        }

    /**
    * {@inheritDoc}
    */
    public String readSafeUTF()
            throws IOException
        {
        BufferInput bufIn = m_bufIn;
        return bufIn == null
                ? ExternalizableHelper.readSafeUTF(m_in)
                : bufIn.readSafeUTF();
        }

    /**
    * {@inheritDoc}
    */
    public int readPackedInt()
            throws IOException
        {
        BufferInput bufIn = m_bufIn;
        return bufIn == null
                ? ExternalizableHelper.readInt(m_in)
                : bufIn.readPackedInt();
        }

    /**
    * {@inheritDoc}
    */
    public long readPackedLong()
            throws IOException
        {
        BufferInput bufIn = m_bufIn;
        return bufIn == null
                ? ExternalizableHelper.readLong(m_in)
                : bufIn.readPackedLong();
        }

    /**
    * {@inheritDoc}
    */
    public ReadBuffer readBuffer(int cb)
            throws IOException
        {
        BufferInput bufIn = m_bufIn;
        if (bufIn == null)
            {
            byte[] ab = new byte[cb];
            readFully(ab);
            return new ByteArrayReadBuffer(ab);
            }

        return bufIn.readBuffer(cb);
        }

    /**
    * {@inheritDoc}
    */
    public int getOffset()
        {
        BufferInput bufIn = m_bufIn;
        return bufIn == null ? 0 : bufIn.getOffset();
        }

    /**
    * {@inheritDoc}
    */
    public void setOffset(int of)
        {
        BufferInput bufIn = m_bufIn;
        if (bufIn == null)
            {
            throw new UnsupportedOperationException();
            }
        bufIn.setOffset(of);
        }

    @Override
    public Object getObjectInputFilter()
        {
        BufferInput bufIn = m_bufIn;

        return bufIn == null ? null : bufIn.getObjectInputFilter();
        }

    @Override
    public void setObjectInputFilter(Object filter)
        {
        BufferInput bufIn = m_bufIn;

        if (bufIn != null)
            {
            bufIn.setObjectInputFilter(filter);
            }
        }

    // ----- accessors ------------------------------------------------------

    /**
    * Obtain the underlying object providing the DataInput interface that
    * this object is delegating to.
    *
    * @return the underlying DataInput
    */
    public DataInput getDataInput()
        {
        return m_in;
        }


    // ----- inner class: VersionAwareBufferInput ---------------------------

    /**
    * A BufferInput implementation that in addition to delegating to the given
    * DataInput provides an API to check whether the sender of the content of
    * this BufferInput runs a version that supersedes (greater or equal to)
    * the specified version.
    */
    public static class VersionAwareBufferInput
            extends WrapperBufferInput
        {
        // ----- constructors -----------------------------------------------

        /**
        * Construct a WrapperBufferInput that will read from the specified object
        * implementing the DataInput interface.
        *
        * @param in      an object implementing DataInput to read from
        * @param loader  the ClassLoader
        * @param msg     the associated message received from the recipient
        */
        public VersionAwareBufferInput(DataInput in, ClassLoader loader, MessageComponent msg)
            {
            super(in, loader);

            f_message = msg;
            }


        // ----- public methods ---------------------------------------------

        /**
        * Determine whether the sender of the content of this BufferInput
        * runs a version that supersedes (greater or equal to) the specified
        * version.
        *
        * @return true iff the sender's version is greater or equal to the
        *         specified one
        */
        public boolean isVersionCompatible(int nMajor, int nMinor, int nMicro, int nPatchSet, int nPatch)
            {
            return f_message.isSenderCompatible(nMajor, nMinor, nMicro, nPatchSet, nPatch);
            }

        /**
        * Determine whether the sender of the content of this BufferInput
        * runs a version that supersedes (greater or equal to) the specified
        * version.
        *
        * @return true iff the sender's version is greater or equal to the
        *         specified one
        */
        public boolean isVersionCompatible(int nYear, int nMonth, int nPatch)
            {
            return f_message.isSenderCompatible(nYear, nMonth, nPatch);
            }

        /**
        * Determine whether the sender of the content of this BufferInput
        * runs a version that supersedes (greater or equal to) the specified
        * version.
        *
        * @return true iff the sender's version is greater or equal to the
        *         specified one
        */
        public boolean isVersionCompatible(int nEncodedVersion)
            {
            return f_message.isSenderCompatible(nEncodedVersion);
            }

        /**
        * Determine whether the sender of the content of this BufferInput
        * run versions that are the same as the encode version with the same or a
        * higher patch level.
        *
        * @return true iff all the sender's versions are the same version with
        *         the same or a higher patch level to the specified one
        */
        public boolean isPatchCompatible(int nEncodedVersion)
            {
            return f_message.isSenderPatchCompatible(nEncodedVersion);
            }


        // ----- data members -----------------------------------------------

        /**
        * The associated message received by this recipient.
        */
        protected final MessageComponent f_message;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The underlying ReadBuffer object to use.
    */
    private final BufferInput m_bufIn;
    }