/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io;


import com.tangosol.internal.net.MessageComponent;

import com.tangosol.io.WriteBuffer.BufferOutput;

import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataOutput;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.OutputStream;

import java.nio.ByteBuffer;

/**
* This is an imitation BufferOutput implementation that provides the
* BufferOutput interface by delegating to an object that implements the
* DataOutput interface. Primarily, this is intended as a base class for
* building specific-purpose DataOutput wrappers.
*
* @author jh  2007.11.17
*/
public class WrapperBufferOutput
        extends Base
        implements BufferOutput
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a WrapperBufferOutput that will write to the specified object
    * implementing the DataOutput interface.
    *
    * @param out  an object implementing DataOutput to write to
    */
    public WrapperBufferOutput(DataOutput out)
        {
        m_out    = out;
        m_bufOut = out instanceof BufferOutput ? (BufferOutput) out : null;
        }


    // ----- OutputStreaming interface --------------------------------------

    /**
    * {@inheritDoc}
    */
    public void write(int b)
            throws IOException
        {
        m_out.write(b);
        }

    /**
    * {@inheritDoc}
    */
    public void write(byte ab[])
            throws IOException
        {
        m_out.write(ab);
        }

    /**
    * {@inheritDoc}
    */
    public void write(byte ab[], int of, int cb)
            throws IOException
        {
        m_out.write(ab, of, cb);
        }

    /**
    * {@inheritDoc}
    */
    public void flush()
            throws IOException
        {
        DataOutput out = m_out;
        if (out instanceof OutputStreaming)
            {
            ((OutputStreaming) out).flush();
            }
        else if (out instanceof OutputStream)
            {
            ((OutputStream) out).flush();
            }
        else if (out instanceof ObjectOutput)
            {
            ((ObjectOutput) out).flush();
            }
        }

    /**
    * {@inheritDoc}
    */
    public void close()
            throws IOException
        {
        DataOutput out = m_out;
        if (out instanceof OutputStreaming)
            {
            ((OutputStreaming) out).close();
            }
        else if (out instanceof OutputStream)
            {
            ((OutputStream) out).close();
            }
        else if (out instanceof ObjectOutput)
            {
            ((ObjectOutput) out).close();
            }
        }


    // ----- DataOutput interface -------------------------------------------

    /**
    * {@inheritDoc}
    */
    public void writeBoolean(boolean f)
            throws IOException
        {
        m_out.writeBoolean(f);
        }

    /**
    * {@inheritDoc}
    */
    public void writeByte(int b)
            throws IOException
        {
        m_out.writeByte(b);
        }

    /**
    * {@inheritDoc}
    */
    public void writeShort(int n)
            throws IOException
        {
        m_out.writeShort(n);
        }

    /**
    * {@inheritDoc}
    */
    public void writeChar(int n)
            throws IOException
        {
        m_out.writeChar(n);
        }

    /**
    * {@inheritDoc}
    */
    public void writeInt(int n)
            throws IOException
        {
        m_out.writeInt(n);
        }

    /**
    * {@inheritDoc}
    */
    public void writeLong(long l)
            throws IOException
        {
        m_out.writeLong(l);
        }

    /**
    * {@inheritDoc}
    */
    public void writeFloat(float fl)
            throws IOException
        {
        m_out.writeFloat(fl);
        }

    /**
    * {@inheritDoc}
    */
    public void writeDouble(double dfl)
            throws IOException
        {
        m_out.writeDouble(dfl);
        }

    /**
    * {@inheritDoc}
    */
    public void writeBytes(String s)
            throws IOException
        {
        m_out.writeBytes(s);
        }

    /**
    * {@inheritDoc}
    */
    public void writeChars(String s)
            throws IOException
        {
        m_out.writeChars(s);
        }

    /**
    * {@inheritDoc}
    */
    public void writeUTF(String s)
            throws IOException
        {
        m_out.writeUTF(s);
        }


    // ----- BufferOutput interface -----------------------------------------

    /**
    * {@inheritDoc}
    */
    public ByteBuffer getByteBuffer(int cb)
        {
        return m_bufOut == null ? null : m_bufOut.getByteBuffer(cb);
        }

    /**
    * {@inheritDoc}
    */
    public WriteBuffer getBuffer()
        {
        return m_bufOut == null ? null : m_bufOut.getBuffer();
        }

    /**
    * {@inheritDoc}
    */
    public void writeSafeUTF(String s)
            throws IOException
        {
        BufferOutput bufOut = m_bufOut;
        if (bufOut == null)
            {
            ExternalizableHelper.writeSafeUTF(m_out, s);
            }
        else
            {
            bufOut.writeSafeUTF(s);
            }
        }

    /**
    * {@inheritDoc}
    */
    public void writePackedInt(int n)
            throws IOException
        {
        BufferOutput bufOut = m_bufOut;
        if (bufOut == null)
            {
            ExternalizableHelper.writeInt(m_out, n);
            }
        else
            {
            bufOut.writePackedInt(n);
            }
        }

    /**
    * {@inheritDoc}
    */
    public void writePackedLong(long l)
            throws IOException
        {
        BufferOutput bufOut = m_bufOut;
        if (bufOut == null)
            {
            ExternalizableHelper.writeLong(m_out, l);
            }
        else
            {
            bufOut.writePackedLong(l);
            }
        }

    /**
    * {@inheritDoc}
    */
    public void writeBuffer(ReadBuffer buf)
            throws IOException
        {
        BufferOutput bufOut = m_bufOut;
        if (bufOut == null)
            {
            write(buf.toByteArray());
            }
        else
            {
            bufOut.writeBuffer(buf);
            }
        }

    /**
    * {@inheritDoc}
    */
    public void writeBuffer(ReadBuffer buf, int of, int cb)
            throws IOException
        {
        BufferOutput bufOut = m_bufOut;
        if (bufOut == null)
            {
            write(buf.toByteArray(of, cb));
            }
        else
            {
            bufOut.writeBuffer(buf, of, cb);
            }
        }

    /**
    * {@inheritDoc}
    */
    public void writeStream(InputStreaming stream)
            throws IOException
        {
        BufferOutput bufOut = m_bufOut;
        if (bufOut == null)
            {
            final byte[] ab = getTempBuffer();
            for (int c = stream.read(ab); c >= 0; c = stream.read(ab))
                {
                write(ab, 0, c);
                }
            }
        else
            {
            bufOut.writeStream(stream);
            }
        }

    /**
    * {@inheritDoc}
    */
    public void writeStream(InputStreaming stream, int cb)
            throws IOException
        {
        BufferOutput bufOut = m_bufOut;
        if (bufOut == null)
            {
            final byte[] ab = getTempBuffer();
            for (int c = stream.read(ab), t = 0; c >= 0 && t < cb; c = stream.read(ab))
                {
                write(ab, 0, c);
                t += c;
                }
            }
        else
            {
            bufOut.writeStream(stream, cb);
            }
        }

    /**
    * {@inheritDoc}
    */
    public int getOffset()
        {
        BufferOutput bufOut = m_bufOut;
        return bufOut == null ? 0 : bufOut.getOffset();
        }

    /**
    * {@inheritDoc}
    */
    public void setOffset(int of)
        {
        BufferOutput bufOut = m_bufOut;
        if (bufOut == null)
            {
            throw new UnsupportedOperationException();
            }
        bufOut.setOffset(of);
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Obtain the underlying object providing the DataOutput interface that
    * this object is delegating to.
    *
    * @return the underlying DataOutput
    */
    public DataOutput getDataOutput()
        {
        return m_out;
        }

    /**
    * Obtain a temporary buffer to use for building the data to write.
    *
    * @return a temporary byte array
    */
    private byte[] getTempBuffer()
        {
        byte[] abBuf = m_abBuf;
        if (abBuf == null)
            {
            abBuf = m_abBuf = new byte[1024];
            }
        return abBuf;
        }


    // ----- inner class: VersionAwareBufferOutput --------------------------

    /**
    * A BufferOutput implementation that in addition to delegating to the given
    * DataOutput provides an API to check whether the recipients of the content
    * of this BufferOutput run versions that supersede (greater or equal to)
    * the specified version.
    */
    public static class VersionAwareBufferOutput
            extends WrapperBufferOutput
        {
        // ----- constructors -----------------------------------------------

        /**
        * Construct a VersionAwareBufferOutput that will write to the specified
        * object implementing the DataOutput interface.
        *
        * @param out      an object implementing DataOutput to write to
        * @param message  the associated message being sent
        */
        public VersionAwareBufferOutput(DataOutput out, MessageComponent message)
            {
            super(out);

            f_message = message;
            }


        // ----- public methods ---------------------------------------------

        /**
        * Determine whether all the recipients of the content of this BufferOutput
        * run versions that supersede (greater or equal to) the specified
        * version.
        *
        * @return true iff all the recipients' versions are greater or equal
        *         to the specified one
        */
        public boolean isVersionCompatible(int nMajor, int nMinor, int nMicro, int nPatchSet, int nPatch)
            {
            return f_message.isRecipientCompatible(nMajor, nMinor, nMicro, nPatchSet, nPatch);
            }

        /**
        * Determine whether all the recipients of the content of this BufferOutput
        * run versions that supersede (greater or equal to) the specified
        * version.
        *
        * @return true iff all the recipients' versions are greater or equal
        *         to the specified one
        */
        public boolean isVersionCompatible(int nYear, int nMonth, int nPatch)
            {
            return f_message.isRecipientCompatible(nYear, nMonth, nPatch);
            }

        /**
        * Determine whether all the recipients of the content of this BufferOutput
        * run versions that supersede (greater or equal to) the specified
        * version.
        *
        * @return true iff all the recipients' versions are greater or equal
        *         to the specified one
        */
        public boolean isVersionCompatible(int nEncodedVersion)
            {
            return f_message.isRecipientCompatible(nEncodedVersion);
            }

        /**
        * Determine whether all the recipients of the content of this BufferOutput
        * run versions that are the same as the encode version with the same or a
        * higher patch level.
        *
        * @return true iff all the recipients' versions are the same version with
        *         the same or a higher patch level to the specified one
        */
        public boolean isPatchCompatible(int nEncodedVersion)
            {
            return f_message.isRecipientPatchCompatible(nEncodedVersion);
            }


        // ----- data members -----------------------------------------------

        /**
        * The associated message being sent.
        */
        protected final MessageComponent f_message;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The underlying DataOutput object to use.
    */
    private final DataOutput m_out;

    /**
    * The underlying WriteBuffer object to use.
    */
    private final BufferOutput m_bufOut;

    /**
    * A temp buffer to use for building the data to write.
    */
    private transient byte[] m_abBuf;
    }
