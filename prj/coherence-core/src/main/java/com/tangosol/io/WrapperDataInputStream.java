/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io;


import java.io.DataInput;
import java.io.EOFException;
import java.io.InputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.UTFDataFormatException;


/**
* This is an imitation DataInputStream class that provides the DataInput
* interface by delegating to an object that implements the DataInput
* interface. Primarily, this is intended as a base class for building
* specific-purpose DataInput wrappers.
*
* @author cp  2004.09.09
*/
public class WrapperDataInputStream
        extends InputStream
        implements DataInput, InputStreaming
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a WrapperDataInputStream that will read from the specified
    * object implementing the DataInput interface.
    *
    * @param in  an object implementing DataInput to read from
    */
    public WrapperDataInputStream(DataInput in)
        {
        this(in, null);
        }

    /**
    * Construct a WrapperDataInputStream that will read from the specified
    * object implementing the DataInput interface.
    *
    * @param in      an object implementing DataInput to read from
    * @param loader  the {@link ClassLoader} associated with this stream
    */
    public WrapperDataInputStream(DataInput in, ClassLoader loader)
        {
        m_in     = in;
        m_loader = loader;
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

    /**
    * Returns a {@link ClassLoader} associated with this stream.
    *
    * @return a ClassLoader associated with this stream
    */
    public ClassLoader getClassLoader()
        {
        return m_loader;
        }

    /**
    * Set the ClassLoader associated with this stream.
    *
    * @param loader  the ClassLoader associated with this stream
    */
    public void setClassLoader(ClassLoader loader)
        {
        m_loader = loader;
        }


    // ----- DataInput methods ----------------------------------------------

    /**
    * Read <code>ab.length</code> bytes and store them in <code>ab</code>.
    * <p>
    * This method blocks until input data is available, the end of the stream
    * is detected, or an exception is thrown.
    *
    * @param ab  the array to store the bytes which are read from the stream
    *
    * @exception NullPointerException  if the passed array is null
    * @exception EOFException  if the stream is exhausted before the number
    *            of bytes indicated by the array length could be read
    * @exception IOException  if an I/O error occurs
    */
    public void readFully(byte ab[])
            throws IOException
        {
        m_in.readFully(ab);
        }

    /**
    * Read <code>cb</code> bytes and store them in <code>ab</code> starting
    * at offset <code>of</code>.
    * <p>
    * This method blocks until input data is available, the end of the stream
    * is detected, or an exception is thrown.
    *
    * @param ab  the array to store the bytes which are read from the stream
    * @param of  the offset into the array that the read bytes will be stored
    * @param cb  the maximum number of bytes to read
    *
    * @exception NullPointerException  if the passed array is null
    * @exception IndexOutOfBoundsException  if <code>of</code> or
    *            <code>cb</code> is negative, or <code>of+cb</code> is
    *            greater than the length of the <code>ab</code>
    * @exception EOFException  if the stream is exhausted before the number
    *            of bytes indicated by the array length could be read
    * @exception IOException  if an I/O error occurs
    */
    public void readFully(byte ab[], int of, int cb)
            throws IOException
        {
        m_in.readFully(ab, of, cb);
        }

    /**
    * Skips over up to the specified number of bytes of data. The number of
    * bytes actually skipped over may be fewer than the number specified to
    * skip, and may even be zero; this can be caused by an end-of-file
    * condition, but can also occur even when there is data remaining to
    * be read. As a result, the caller should check the return value from
    * this method, which indicates the actual number of bytes skipped.
    *
    * @param cb  the maximum number of bytes to skip over
    *
    * @return  the actual number of bytes that were skipped over
    *
    * @exception IOException  if an I/O error occurs
    */
    public int skipBytes(int cb)
            throws IOException
        {
        return m_in.skipBytes(cb);
        }

    /**
    * Read a boolean value.
    * <p>
    * This method is the counterpart for the
    * {@link java.io.DataOutput#writeBoolean} method.
    *
    * @return either <code>true</code> or <code>false</code>
    *
    * @exception EOFException  if the value could not be read because no
    *            more data remains to be read
    * @exception IOException  if an I/O error occurs
    */
    public boolean readBoolean()
            throws IOException
        {
        return m_in.readBoolean();
        }

    /**
    * Read a byte value.
    * <p>
    * This method is the counterpart for the
    * {@link java.io.DataOutput#writeByte} method.
    *
    * @return a <code>byte</code> value
    *
    * @exception EOFException  if the value could not be read because no
    *            more data remains to be read
    * @exception IOException  if an I/O error occurs
    */
    public byte readByte()
            throws IOException
        {
        return m_in.readByte();
        }

    /**
    * Read an unsigned byte value.
    * <p>
    * This method is the counterpart for the
    * {@link java.io.DataOutput#writeByte} method when it is used with
    * unsigned 8-bit values.
    *
    * @return an <code>int</code> value in the range 0x00 to 0xFF
    *
    * @exception EOFException  if the value could not be read because no
    *            more data remains to be read
    * @exception IOException  if an I/O error occurs
    */
    public int readUnsignedByte()
            throws IOException
        {
        return m_in.readUnsignedByte();
        }

    /**
    * Read a short value.
    * <p>
    * This method is the counterpart for the
    * {@link java.io.DataOutput#writeShort} method.
    *
    * @return a <code>short</code> value
    *
    * @exception EOFException  if the value could not be read because no
    *            more data remains to be read
    * @exception IOException  if an I/O error occurs
    */
    public short readShort()
            throws IOException
        {
        return m_in.readShort();
        }

    /**
    * Read an unsigned short value.
    * <p>
    * This method is the counterpart for the
    * {@link java.io.DataOutput#writeShort} method when it is used with
    * unsigned 16-bit values.
    *
    * @return an <code>int</code> value in the range of 0x0000 to 0xFFFF
    *
    * @exception EOFException  if the value could not be read because no
    *            more data remains to be read
    * @exception IOException  if an I/O error occurs
    */
    public int readUnsignedShort()
            throws IOException
        {
        return m_in.readUnsignedShort();
        }

    /**
    * Read a char value.
    * <p>
    * This method is the counterpart for the
    * {@link java.io.DataOutput#writeChar} method.
    *
    * @return a <code>char</code> value
    *
    * @exception EOFException  if the value could not be read because no
    *            more data remains to be read
    * @exception IOException  if an I/O error occurs
    */
    public char readChar()
            throws IOException
        {
        return m_in.readChar();
        }

    /**
    * Read an int value.
    * <p>
    * This method is the counterpart for the
    * {@link java.io.DataOutput#writeInt} method.
    *
    * @return an <code>int</code> value
    *
    * @exception EOFException  if the value could not be read because no
    *            more data remains to be read
    * @exception IOException  if an I/O error occurs
    */
    public int readInt()
            throws IOException
        {
        return m_in.readInt();
        }

    /**
    * Read a long value.
    * <p>
    * This method is the counterpart for the
    * {@link java.io.DataOutput#writeLong} method.
    *
    * @return a <code>long</code> value
    *
    * @exception EOFException  if the value could not be read because no
    *            more data remains to be read
    * @exception IOException  if an I/O error occurs
    */
    public long readLong()
            throws IOException
        {
        return m_in.readLong();
        }

    /**
    * Read a float value.
    * <p>
    * This method is the counterpart for the
    * {@link java.io.DataOutput#writeFloat} method.
    *
    * @return a <code>float</code> value
    *
    * @exception EOFException  if the value could not be read because no
    *            more data remains to be read
    * @exception IOException  if an I/O error occurs
    */
    public float readFloat()
            throws IOException
        {
        return m_in.readFloat();
        }

    /**
    * Read a double value.
    * <p>
    * This method is the counterpart for the
    * {@link java.io.DataOutput#writeDouble} method.
    *
    * @return a <code>double</code> value
    *
    * @exception EOFException  if the value could not be read because no
    *            more data remains to be read
    * @exception IOException  if an I/O error occurs
    */
    public double readDouble()
            throws IOException
        {
        return m_in.readDouble();
        }

    /**
    * Reads the next "line" of text.
    * <p>
    * This method does not have a counterpart in the
    * {@link java.io.DataOutput} interface. Furthermore, this method is
    * defined as operating on bytes and not on characters, and thus it should
    * be selected for use only after careful consideration, as if it were
    * deprecated.
    *
    * @return a line of text as a String
    * @exception  IOException  if an I/O error occurs.
    */
    public String readLine()
            throws IOException
        {
        return m_in.readLine();
        }

    /**
    * Reads a String value.
    * <p>
    * This method is the counterpart for the
    * {@link java.io.DataOutput#writeUTF} method.
    *
    * @return a String value
    *
    * @exception UTFDataFormatException  if the bytes that were read were not
    *            a valid UTF-8 encoded string
    * @exception EOFException  if the value could not be read because no
    *            more data remains to be read
    * @exception IOException  if an I/O error occurs
    */
    public String readUTF()
            throws IOException
        {
        return m_in.readUTF();
        }


    // ----- InputStream methods --------------------------------------------

    /**
    * Read the next byte of data from the InputStream. The value byte is
    * returned as an <code>int</code> in the range <code>0</code> to
    * <code>255</code>. If the end of the stream has been reached, the value
    * <code>-1</code> is returned.
    * <p>
    * This method blocks until input data is available, the end of the stream
    * is detected, or an exception is thrown.
    *
    * @return  the next byte of data, or <code>-1</code> if the end of the
    *          stream has been reached
    *
    * @exception IOException  if an I/O error occurs
    */
    public int read()
            throws IOException
        {
        DataInput in = m_in;
        if (in instanceof InputStreaming)
            {
            return ((InputStreaming) in).read();
            }
        else if (in instanceof InputStream)
            {
            return ((InputStream) in).read();
            }
        else if (in instanceof ObjectInput)
            {
            return ((ObjectInput) in).read();
            }
        else
            {
            try
                {
                return readUnsignedByte();
                }
            catch (EOFException e)
                {
                return -1;
                }
            }
        }

    /**
    * Read some number of bytes from the input stream and store them into the
    * passed array <code>ab</code>. The number of bytes actually read is
    * returned.
    * <p>
    * This method blocks until input data is available, the end of the stream
    * is detected, or an exception is thrown.
    *
    * @param ab  the array to store the bytes which are read from the stream
    *
    * @return  the number of bytes read from the stream, or <code>-1</code>
    *          if no bytes were read from the stream because the end of the
    *          stream had been reached
    *
    * @exception NullPointerException  if the passed array is null
    * @exception IOException  if an I/O error occurs
    */
    public int read(byte ab[])
            throws IOException
        {
        DataInput in = m_in;
        if (in instanceof InputStreaming)
            {
            return ((InputStreaming) in).read(ab);
            }
        else if (in instanceof InputStream)
            {
            return ((InputStream) in).read(ab);
            }
        else if (in instanceof ObjectInput)
            {
            return ((ObjectInput) in).read(ab);
            }
        else
            {
            int cb = ab.length;
            for (int of = 0; of < cb; ++of)
                {
                try
                    {
                    ab[of] = (byte) readUnsignedByte();
                    }
                catch (EOFException e)
                    {
                    return cb == 0 ? -1 : cb;
                    }
                }
            return cb;
            }
        }

    /**
    * Read up to <code>cb</code> bytes from the input stream and store them
    * into the passed array <code>ab</code> starting at offset
    * <code>of</code>. The number of bytes actually read is returned.
    * <p>
    * This method blocks until input data is available, the end of the stream
    * is detected, or an exception is thrown.
    *
    * @param ab  the array to store the bytes which are read from the stream
    * @param of  the offset into the array that the read bytes will be stored
    * @param cb  the maximum number of bytes to read
    *
    * @return  the number of bytes read from the stream, or <code>-1</code>
    *          if no bytes were read from the stream because the end of the
    *          stream had been reached
    *
    * @exception NullPointerException  if the passed array is null
    * @exception IndexOutOfBoundsException  if <code>of</code> or
    *            <code>cb</code> is negative, or <code>of+cb</code> is
    *            greater than the length of the <code>ab</code>
    * @exception IOException  if an I/O error occurs
    */
    public int read(byte ab[], int of, int cb)
            throws IOException
        {
        DataInput in = m_in;
        if (in instanceof InputStreaming)
            {
            return ((InputStreaming) in).read(ab, of, cb);
            }
        else if (in instanceof InputStream)
            {
            return ((InputStream) in).read(ab, of, cb);
            }
        else if (in instanceof ObjectInput)
            {
            return ((ObjectInput) in).read(ab, of, cb);
            }
        else
            {
            if (of < 0 || cb < 0 || of + cb > ab.length)
                {
                throw new IndexOutOfBoundsException("ab.length=" + ab.length
                        + ", of=" + of + ", cb=" + cb);
                }

            for (int ofStart = of, ofEnd = of + cb - 1; of <= ofEnd; ++of)
                {
                try
                    {
                    ab[of] = (byte) readUnsignedByte();
                    }
                catch (EOFException e)
                    {
                    return of == ofStart ? -1 : (of - ofStart);
                    }
                }
            return cb;
            }
        }

    /**
    * Skips over up to the specified number of bytes of data from this
    * InputStream. The number of bytes actually skipped over may be fewer
    * than the number specified to skip, and may even be zero; this can be
    * caused by an end-of-file condition, but can also occur even when there
    * is data remaining in the InputStream. As a result, the caller should
    * check the return value from this method, which indicates the actual
    * number of bytes skipped.
    *
    * @param cb  the maximum number of bytes to skip over
    *
    * @return  the actual number of bytes that were skipped over
    *
    * @exception IOException  if an I/O error occurs
    */
    public long skip(long cb)
            throws IOException
        {
        DataInput in = m_in;
        if (in instanceof InputStreaming)
            {
            return ((InputStreaming) in).skip(cb);
            }
        else if (in instanceof InputStream)
            {
            return ((InputStream) in).skip(cb);
            }
        else if (in instanceof ObjectInput)
            {
            return ((ObjectInput) in).skip(cb);
            }
        else
            {
            if (cb > (long) Integer.MAX_VALUE)
                {
                cb = Integer.MAX_VALUE;
                }
            return in.skipBytes((int) cb);
            }
        }

    /**
    * Returns the number of bytes that can be read (or skipped over) from
    * this input stream without causing a blocking I/O condition to occur.
    * This method reflects the assumed implementation of various buffering
    * InputStreams, which can guarantee non-blocking reads up to the extent
    * of their buffers, but beyond that the read operations will have to read
    * from some underlying (and potentially blocking) source.
    *
    * @return  the number of bytes that can be read from this InputStream
    *          without blocking
    *
    * @exception IOException  if an I/O error occurs
    */
    public int available()
            throws IOException
        {
        DataInput in = m_in;
        if (in instanceof InputStreaming)
            {
            return ((InputStreaming) in).available();
            }
        else if (in instanceof InputStream)
            {
            return ((InputStream) in).available();
            }
        else if (in instanceof ObjectInput)
            {
            return ((ObjectInput) in).available();
            }
        else
            {
            return 0;
            }
        }

    /**
    * Close the InputStream and release any system resources associated with
    * it.
    *
    * @exception IOException  if an I/O error occurs
    */
    public void close()
            throws IOException
        {
        DataInput in = m_in;
        if (in instanceof InputStreaming)
            {
            ((InputStreaming) in).close();
            }
        else if (in instanceof InputStream)
            {
            ((InputStream) in).close();
            }
        else if (in instanceof ObjectInput)
            {
            ((ObjectInput) in).close();
            }
        }

    /**
    * Marks the current read position in the InputStream in order to support
    * the stream to be later "rewound" (using the {@link #reset} method) to
    * the current position. The caller passes in the maximum number of bytes
    * that it expects to read before calling the {@link #reset} method, thus
    * indicating the upper bounds of the responsibility of the stream to be
    * able to buffer what it has read in order to support this functionality.
    *
    * @param cbReadLimit  the maximum number of bytes that caller expects the
    *                     InputStream to be able to read before the mark
    *                     position becomes invalid
    */
    public void mark(int cbReadLimit)
        {
        DataInput in = m_in;
        if (in instanceof InputStreaming)
            {
            ((InputStreaming) in).mark(cbReadLimit);
            }
        else if (in instanceof InputStream)
            {
            ((InputStream) in).mark(cbReadLimit);
            }
        }

    /**
    * Rewind this stream to the position at the time the {@link #mark} method
    * was last called on this InputStream. If the InputStream cannot fulfill
    * this contract, it should throw an IOException.
    *
    * @exception IOException  if an I/O error occurs, for example if this
    *                         has not been marked or if the mark has been
    *                         invalidated
    */
    public void reset()
            throws IOException
        {
        DataInput in = m_in;
        if (in instanceof InputStreaming)
            {
            ((InputStreaming) in).reset();
            }
        else if (in instanceof InputStream)
            {
            ((InputStream) in).reset();
            }
        else
            {
            throw new IOException("mark not supported");
            }
        }

    /**
    * Determine if this InputStream supports the {@link #mark} and
    * {@link #reset} methods.
    *
    * @return  <code>true</code> if this InputStream supports the mark and
    *          reset method; <code>false</code> otherwise
    */
    public boolean markSupported()
        {
        DataInput in = m_in;
        if (in instanceof InputStreaming)
            {
            return ((InputStreaming) in).markSupported();
            }
        else if (in instanceof InputStream)
            {
            return ((InputStream) in).markSupported();
            }
        else
            {
            return false;
            }
        }


    // ----- data members ---------------------------------------------------

    /**
    * The underlying DataInput object to use.
    */
    protected DataInput m_in;

    /**
    * The class loader used to resolve deserialized classes.
    */
    protected ClassLoader m_loader;
    }