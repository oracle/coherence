/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io;


import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;

import java.nio.ByteBuffer;

import com.tangosol.util.Binary;
import com.tangosol.util.ByteSequence;


/**
* The ReadBuffer interface represents an in-memory block of binary data,
* such as that represented by a byte[], a Binary object, or an NIO buffer.
*
* @author cp  2005.01.18
*/
public interface ReadBuffer
        extends ByteSequence, Cloneable
    {
    /**
    * Determine the length of the buffer.
    *
    * @return the number of bytes of data represented by this ReadBuffer
    */
    public int length();

    /**
    * Returns the byte at the specified offset. An offset ranges
    * from <code>0</code> to <code>length() - 1</code>. The first byte
    * of the sequence is at offset <code>0</code>, the next at offset
    * <code>1</code>, and so on, as for array indexing.
    *
    * @param of  the offset (index) of the byte
    *
    * @return the byte at the specified offset in this ReadBuffer
    *
    * @exception  IndexOutOfBoundsException  if the <code>of</code>
    *             argument is negative or not less than the length of this
    *             ReadBuffer
    */
    public byte byteAt(int of);

    /**
    * Copies bytes from this ReadBuffer into the destination byte array.
    * <p>
    * The first byte to be copied is at offset <code>ofBegin</code>;
    * the last byte to be copied is at offset <code>ofEnd-1</code>
    * (thus the total number of bytes to be copied is <code>ofEnd -
    * ofBegin</code>). The bytes are copied into the sub-array of
    * <code>abDest</code> starting at offset <code>ofDest</code>
    * and ending at index:
    * <blockquote><pre>
    *     ofDest + (ofEnd - ofBegin) - 1
    * </pre></blockquote>
    * <p>
    * This method is the ReadBuffer equivalent of
    * {@link String#getChars(int, int, char[], int)}. It allows the caller
    * to extract a chunk of bytes into the caller's own array.
    *
    * @param ofBegin  offset of the first byte in the ReadBuffer to copy
    * @param ofEnd    offset after the last byte in the ReadBuffer to copy
    * @param abDest   the destination byte array
    * @param ofDest   the offset in the destination byte array to copy the
    *                 first byte to
    *
    * @exception IndexOutOfBoundsException  Thrown if any of the following
    *            is true:
    *   <ul>
    *   <li><code>ofBegin</code> is negative;
    *   <li><code>ofBegin</code> is greater than <code>ofEnd</code>
    *   <li><code>ofEnd</code> is greater than the length of this
    *       ReadBuffer;
    *   <li><code>ofDest</code> is negative
    *   <li><code>ofDest + (ofEnd - ofBegin)</code> is larger than
    *       <code>abDest.length</code>
    *   </ul>
    * @exception NullPointerException if <code>abDest</code> is
    *   <code>null</code>
    */
    public void copyBytes(int ofBegin, int ofEnd, byte abDest[], int ofDest);

    /**
    * Get a BufferInput object to read data from this buffer. Note that each
    * call to this method will return a new BufferInput object, with the
    * possible exception being that a zero-length ReadBuffer could always
    * return the same instance (since there is nothing to read).
    *
    * @return a BufferInput that is reading from this buffer starting at
    *         offset zero
    */
    public BufferInput getBufferInput();

    /**
    * Obtain a ReadBuffer for a portion of this ReadBuffer.
    *
    * @param of  the beginning index, inclusive
    * @param cb  the number of bytes to include in the resulting ReadBuffer
    *
    * @return a ReadBuffer that represents a portion of this ReadBuffer
    *
    * @exception  IndexOutOfBoundsException  if <code>of</code> or
    *             <code>cb</code> is negative, or <code>of + cb</code> is
    *             larger than the length of this <code>ReadBuffer</code>
    *             object
    */
    public ReadBuffer getReadBuffer(int of, int cb);

    /**
    * Write the contents of this ReadBuffer to an OutputStream.
    *
    * @param out  an OutputStream to write to
    *
    * @throws IOException if an I/O exception occurs
    */
    public void writeTo(OutputStream out) throws IOException;

    /**
    * Write the contents of the ReadBuffer to an OutputStream.
    *
    * @param out  an OutputStream to write to
    * @param of   the beginning index, inclusive
    * @param cb   the number of bytes to write to an OutputStream
    *
    * @throws IOException if an I/O exception occurs
    */
    public void writeTo(OutputStream out, int of, int cb) throws IOException;

    /**
    * Write the contents of this ReadBuffer to a DataOutput.
    *
    * @param out  a DataOutput to write to
    *
    * @throws IOException if an I/O exception occurs
    */
    public void writeTo(DataOutput out) throws IOException;

    /**
    * Write the contents of this ReadBuffer to a DataOutput.
    *
    * @param out  a DataOutput to write to
    * @param of   the beginning index, inclusive
    * @param cb   the number of bytes to write to a DataOutput
    *
    * @throws IOException if an I/O exception occurs
    */
    public void writeTo(DataOutput out, int of, int cb) throws IOException;

    /**
    * Write the contents of the Binary object to a ByteBuffer.
    *
    * @param buf  a ByteBuffer to write to
    */
    public void writeTo(ByteBuffer buf);

    /**
    * Write the contents of the Binary object to a ByteBuffer.
    *
    * @param buf  an ByteBuffer to write to
    * @param of   the beginning index, inclusive
    * @param cb   the number of bytes to write to a ByteBuffer
    *
    * @throws IOException if an I/O exception occurs
    */
    public void writeTo(ByteBuffer buf, int of, int cb) throws IOException;

    /**
    * Get the contents of the ReadBuffer as a byte array.
    * <p>
    * This is the equivalent of <code>toByteArray(0, length())</code>.
    *
    * @return a byte[] with the contents of this ReadBuffer object
    */
    public byte[] toByteArray();

    /**
    * Get a portion of the contents of the ReadBuffer as a byte array.
    * <p>
    * This method is an equivalent of
    * <code>getReadBuffer(of, cb).toByteArray()</code>.
    *
    * @param of  the beginning index, inclusive
    * @param cb  the number of bytes to include in the resulting byte[]
    *
    * @return  a byte[] containing the specified portion of this ReadBuffer
    *
    * @exception  IndexOutOfBoundsException  if <code>of</code> or
    *             <code>cb</code> is negative, or <code>of + cb</code> is
    *             larger than the length of this <code>ReadBuffer</code>
    *             object
    */
    public byte[] toByteArray(int of, int cb);

    /**
    * Return a new Binary object that holds the complete contents of this
    * ReadBuffer.
    * <p>
    * This is the equivalent of <code>toBinary(0, length())</code>.
    *
    * @return  the contents of this ReadBuffer as a Binary object
    */
    public Binary toBinary();

    /**
    * Return a Binary object that holds the specified portion of this
    * ReadBuffer.
    * <p>
    * This method is an equivalent of
    * <code>getReadBuffer(of, cb).toBinary()</code>.
    *
    * @param of  the beginning index, inclusive
    * @param cb  the number of bytes to include in the Binary object
    *
    * @return  a Binary object containing the specified portion of this
    *          ReadBuffer
    *
    * @exception  IndexOutOfBoundsException  if <code>of</code> or
    *             <code>cb</code> is negative, or <code>of + cb</code> is
    *             larger than the length of this <code>ReadBuffer</code>
    *             object
    */
    public Binary toBinary(int of, int cb);

    /**
    * Return a read-only ByteBuffer view of this ReadBuffer. This view may or
    * may not reflect any subsequent changes made to the underlying content.
    *
    * @return a read-only ByteBuffer view of this ReadBuffer
    *
    * @since Coherence 12.1.2
    */
    public ByteBuffer toByteBuffer();

    /**
    * Return a read-only ByteBuffer view of the specified portion of this
    * ReadBuffer. This view may or may not reflect any subsequent changes made
    * to the underlying content.
    * <p>
    * This method is an equivalent of
    * <code>getReadBuffer(of, cb).toByteBuffer()</code>.
    *
    * @param of  the beginning index, inclusive
    * @param cb  the number of bytes to include in the ByteBuffer object
    *
    * @return  a read-only ByteBuffer view of the specified portion of this
    *          ReadBuffer
    *
    * @exception  IndexOutOfBoundsException  if <code>of</code> or
    *             <code>cb</code> is negative, or <code>of + cb</code> is
    *             larger than the length of this <code>ReadBuffer</code> object
    *
    * @since Coherence 12.1.2
    */
    public ByteBuffer toByteBuffer(int of, int cb);

    /**
    * {@inheritDoc}
    *
    * @since Coherence 3.7
    */
    public ByteSequence subSequence(int ofStart, int ofEnd);


    // ----- Object methods -------------------------------------------------

    /**
    * Compare two ReadBuffer objects for equality.
    *
    * @param o  a ReadBuffer object
    *
    * @return true iff the other ReadBuffer is identical to this
    */
    public boolean equals(Object o);

    /**
    * Create a clone of this ReadBuffer object.
    *
    * @return a ReadBuffer object with the same contents as this
    *         ReadBuffer object
    */
    public Object clone();


    // ----- inner interface: BufferInput -----------------------------------

    /**
    * The BufferInput interface represents a DataInputStream on top of a
    * ReadBuffer.
    *
    * @author cp  2005.01.18
    */
    public interface BufferInput
            extends InputStreaming, DataInput
        {
        // ----- InputStreaming methods ---------------------------------

        /**
        * Returns the number of bytes that can be read (or skipped over) from
        * this input stream without causing a blocking I/O condition to
        * occur. This method reflects the assumed implementation of various
        * buffering InputStreams, which can guarantee non-blocking reads up
        * to the extent of their buffers, but beyond that the read operations
        * will have to read from some underlying (and potentially blocking)
        * source.
        * <p>
        * BufferInput implementations must implement this method to return
        * the extent of the buffer that has not yet been read; in other
        * words, the entire un-read portion of the buffer <b>must</b> be
        * available.
        *
        * @return  the number of bytes that can be read from this InputStream
        *          without blocking
        *
        * @exception IOException  if an I/O error occurs
        */
        public int available()
                throws IOException;

        /**
        * Close the InputStream and release any system resources associated
        * with it.
        * <p>
        * BufferInput implementations do not pass this call down onto an
        * underlying stream, if any.
        *
        * @exception IOException  if an I/O error occurs
        */
        public void close()
                throws IOException;

        /**
        * Marks the current read position in the InputStream in order to
        * support the stream to be later "rewound" (using the {@link #reset}
        * method) to the current position. The caller passes in the maximum
        * number of bytes that it expects to read before calling the
        * {@link #reset} method, thus indicating the upper bounds of the
        * responsibility of the stream to be able to buffer what it has read
        * in order to support this functionality.
        * <p>
        * BufferInput implementations ignore the <code>cbReadLimit</code>;
        * they must support an unlimited read limit, since they appear to the
        * user as an input stream on top of a fully realized read buffer.
        *
        * @param cbReadLimit  the maximum number of bytes that caller expects
        *                     the InputStream to be able to read before the
        *                     mark position becomes invalid
        */
        public void mark(int cbReadLimit);

        /**
        * Determine if this InputStream supports the {@link #mark} and
        * {@link #reset} methods.
        * <p>
        * BufferInput implementations <b>must</b> support the {@link #mark}
        * and {@link #reset} methods, so this method always returns
        * <code>true</code>.
        *
        * @return  <code>true</code> if this InputStream supports the mark
        *          and reset method; <code>false</code> otherwise
        */
        public boolean markSupported();


        // ----- DataInput methods --------------------------------------

        /**
        * Read <code>ab.length</code> bytes and store them in
        * <code>ab</code>.
        * <p>
        * This method blocks until input data is available, the end of the
        * stream is detected, or an exception is thrown.
        *
        * @param ab  the array to store the bytes which are read from the
        *            stream
        *
        * @exception NullPointerException  if the passed array is null
        * @exception java.io.EOFException  if the stream is exhausted before the
        *            number
        *            of bytes indicated by the array length could be read
        * @exception IOException  if an I/O error occurs
        */
        public void readFully(byte ab[])
                throws IOException;

        /**
        * Read <code>cb</code> bytes and store them in <code>ab</code>
        * starting at offset <code>of</code>.
        * <p>
        * This method blocks until input data is available, the end of the
        * stream is detected, or an exception is thrown.
        *
        * @param ab  the array to store the bytes which are read from the
        *            stream
        * @param of  the offset into the array that the read bytes will be
        *            stored
        * @param cb  the maximum number of bytes to read
        *
        * @exception NullPointerException  if the passed array is null
        * @exception IndexOutOfBoundsException  if <code>of</code> or
        *            <code>cb</code> is negative, or <code>of+cb</code> is
        *            greater than the length of the <code>ab</code>
        * @exception java.io.EOFException  if the stream is exhausted before the
        *            number of bytes indicated by the array length could be
        *            read
        * @exception IOException  if an I/O error occurs
        */
        public void readFully(byte ab[], int of, int cb)
                throws IOException;

        /**
        * Skips over up to the specified number of bytes of data. The number
        * of bytes actually skipped over may be fewer than the number
        * specified to skip, and may even be zero; this can be caused by an
        * end-of-file condition, but can also occur even when there is data
        * remaining to be read. As a result, the caller should check the
        * return value from this method, which indicates the actual number of
        * bytes skipped.
        *
        * @param cb  the maximum number of bytes to skip over
        *
        * @return  the actual number of bytes that were skipped over
        *
        * @exception IOException  if an I/O error occurs
        */
        public int skipBytes(int cb)
                throws IOException;

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
                throws IOException;

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
                throws IOException;

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
                throws IOException;

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
                throws IOException;

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
                throws IOException;

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
                throws IOException;

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
                throws IOException;

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
                throws IOException;

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
                throws IOException;

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
                throws IOException;

        /**
        * Reads the next "line" of text.
        * <p>
        * This method does not have a counterpart in the
        * {@link java.io.DataOutput} interface. Furthermore, this method is
        * defined as operating on bytes and not on characters, and thus it
        * should be selected for use only after careful consideration, as if
        * it were deprecated (which it has been in java.io.DataInputStream).
        *
        * @return a line of text as a String
        * @exception  IOException  if an I/O error occurs.
        */
        public String readLine()
                throws IOException;

        /**
        * Reads a String value.
        * <p>
        * This method is the counterpart for the
        * {@link java.io.DataOutput#writeUTF} method.
        *
        * @return a String value
        *
        * @exception java.io.UTFDataFormatException  if the bytes that were
        *            read were not
        *            a valid UTF-8 encoded string
        * @exception EOFException  if the value could not be read because no
        *            more data remains to be read
        * @exception IOException  if an I/O error occurs
        */
        public String readUTF()
                throws IOException;

        // ----- BufferInput methods ------------------------------------

        /**
        * Get the ReadBuffer object that this BufferInput is reading from.
        *
        * @return the underlying ReadBuffer object
        */
        public ReadBuffer getBuffer();

        /**
        * Read a variable-length encoded UTF packed String. The major
        * differences between this implementation and DataInput is that this
        * supports null String values and is not limited to 64KB UTF-encoded
        * values.
        *
        * @return a String value; may be null
        *
        * @exception IOException  if an I/O error occurs
        */
        public String readSafeUTF()
                throws IOException;

        /**
        * Read an int value using a variable-length storage format as described
        * by {@link WriteBuffer.BufferOutput#writePackedInt(int)}.
        *
        * @return  an int value
        *
        * @exception IOException  if an I/O error occurs
        */
        public int readPackedInt()
                throws IOException;

        /**
        * Read a long value using a variable-length storage format as described
        * by {@link WriteBuffer.BufferOutput#writePackedLong(long)}.
        *
        * @return  a long value
        *
        * @exception IOException  if an I/O error occurs
        */
        public long readPackedLong()
                throws IOException;

        /**
        * Read <code>cb</code> bytes and return them as a ReadBuffer object.
        *
        * @param cb  the number of bytes to read
        *
        * @return a ReadBuffer object composed of <code>cb</code> bytes read
        *         from the BufferInput
        *
        * @exception EOFException  if the stream is exhausted before
        *            the number of bytes indicated could be read
        * @exception IOException  if an I/O error occurs
        */
        public ReadBuffer readBuffer(int cb)
                throws IOException;

        /**
        * Determine the current offset of this BufferInput within the
        * underlying ReadBuffer.
        *
        * @return the offset of the next byte to read from the ReadBuffer
        */
        public int getOffset();

        /**
        * Specify the offset of the next byte to read from the underlying
        * ReadBuffer.
        *
        * @param of  the offset of the next byte to read from the ReadBuffer
        *
        * @exception  IndexOutOfBoundsException  if <code>of &lt; 0</code> or
        *             <code>of &gt; getBuffer().length()</code>
        */
        public void setOffset(int of);

        /**
        * Returns an ObjectInputFilter (or null) that should be used by the caller
        * to confirm / deny deserialization of a class encoded in this input stream.
        * <p>
        * Note: the return type is agnostic of the ObjectInputFilter to support various JDK versions.
        *
        * @return null or an ObjectInputFilter that will permit (or not) the constructor
        *         of a class encoded in this stream.
        *
        * @see #setObjectInputFilter(Object)
        */
        public default Object getObjectInputFilter()
            {
            return null;
            }

        /**
        * Set the {@link #getObjectInputFilter() ObjectInputFilter} for this stream.
        * <p>
        * The filter's checkInput method is expected to be called for each class
        * and reference deserialized in the stream.
        *
        * @implSpec
        * This method can set the ObjectInputFilter once.
        * <p>
        * In Java version 17 and greater, the stream's ObjectInputFilter is set
        * to the filter returned by invoking the
        * {@link ExternalizableHelper#getConfigSerialFilterFactory() JVM-wide filter factory}
        * with the {@link #getObjectInputFilter()} current filter} and the{@code filter} parameter.
        *
        * <p>
        * It is not permitted to replace a {@code non-null} filter with a
        * {@code null} filter.
        * If the {@linkplain #getObjectInputFilter() current filter} is {@code non-null},
        * the value returned from the filter factory (or provided filter) must
        * be {@code non-null}.
        *
        * @param oInputFilter  an ObjectInputFilter instance as an Object to enable
        *                running with Java version 8 or higher, may be null
        *
        * @throws IllegalStateException if the filter factory returns {@code null}
        *       when the {@linkplain #getObjectInputFilter() current filter} is
        *       non-null, or if the filter has already been set.
        */
        public default void setObjectInputFilter(Object oInputFilter)
            {
            // no-op
            }
        }
    }
