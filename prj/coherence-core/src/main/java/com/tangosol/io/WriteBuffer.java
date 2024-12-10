/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io;

import com.tangosol.util.Binary;

import java.io.DataOutput;
import java.io.EOFException;
import java.io.IOException;

import java.nio.ByteBuffer;

/**
* The WriteBuffer interface represents an in-memory block of binary data
* that is being accumulated (written to). It is analogous to the byte[]
* inside a Java ByteArrayOutputStream.
*
* @author cp  2005.01.18 created
* @author cp  2005.03.21 defining
*/
public interface WriteBuffer
    {
    // ----- buffer write operations ----------------------------------------

    /**
    * Store the specified byte at the specified offset within the buffer.
    * <p>
    * For purposes of side-effects and potential exceptions, this method is
    * functionally equivalent to the following code:
    * <pre><code>
    * byte[] abSrc = new byte[1];
    * abSrc[0] = b;
    * write(ofDest, abSrc, 0, abSrc.length);
    * </code></pre>
    *
    * @param ofDest  the offset within this buffer to store the passed data
    * @param b       the byte to store in this buffer
    */
    public void write(int ofDest, byte b);

    /**
    * Store the specified bytes at the specified offset within the buffer.
    * <p>
    * For purposes of side-effects and potential exceptions, this method is
    * functionally equivalent to the following code:
    * <pre><code>
    * write(ofDest, abSrc, 0, abSrc.length);
    * </code></pre>
    *
    * @param ofDest  the offset within this buffer to store the passed data
    * @param abSrc   the array of bytes to store in this buffer
    *
    * @exception NullPointerException  if <code>abSrc</code> is
    *            <code>null</code>
    * @exception IndexOutOfBoundsException  if <tt>ofDest</tt> is negative,
    *            or if <tt>ofDest + abSrc.length</tt> is
    *            greater than <tt>{@link #getMaximumCapacity()}</tt>
    */
    public void write(int ofDest, byte[] abSrc);

    /**
    * Store the specified number of bytes from the specified location within
    * the passed byte array at the specified offset within this buffer.
    * <p>
    * As a result of this method, the buffer length as reported by the
    * <tt>{@link #length()}</tt> method will become
    * <tt>Math.max({@link #length()}, ofDest + cbSrc)</tt>.
    * <p>
    * As a result of this method, the buffer capacity as reported by the
    * <tt>{@link #getCapacity()}</tt> method will not change if the new value
    * returned by <tt>{@link #length()}</tt> would not exceed the old value
    * returned by <tt>{@link #getCapacity()}</tt>; otherwise, the capacity
    * will be increased such that
    * <tt>{@link #getCapacity()} &gt;= {@link #length()}</tt>. Regardless, it is
    * always true that <tt>{@link #getCapacity()} &gt;= {@link #length()}</tt>
    * and <tt>{@link #getMaximumCapacity()} &gt;= {@link #getCapacity()}</tt>.
    * If the buffer capacity cannot be increased due to resource constraints,
    * an undesignated Error or RuntimeException will be thrown, such as
    * OutOfMemoryError.
    *
    * @param ofDest  the offset within this buffer to store the passed data
    * @param abSrc   the array containing the bytes to store in this buffer
    * @param ofSrc   the offset within the passed byte array to copy from
    * @param cbSrc   the number of bytes to copy from the passed byte array
    *
    * @exception NullPointerException  if <code>abSrc</code> is
    *            <code>null</code>
    * @exception IndexOutOfBoundsException  if <tt>ofDest</tt>,
    *            <tt>ofSrc</tt> or <tt>cbSrc</tt> is negative, if
    *            <tt>ofSrc + cbSrc</tt> is greater than
    *            <tt>abSrc.length</tt>, or if <tt>ofDest + cbSrc</tt> is
    *            greater than <tt>{@link #getMaximumCapacity()}</tt>
    */
    public void write(int ofDest, byte[] abSrc, int ofSrc, int cbSrc);

    /**
    * Store the contents of the specified ReadBuffer at the specified offset
    * within this buffer.
    * <p>
    * For purposes of side-effects and potential exceptions, this method is
    * functionally equivalent to the following code:
    * <pre><code>
    * byte[] abSrc = bufSrc.toByteArray();
    * write(ofDest, abSrc, 0, abSrc.length);
    * </code></pre>
    *
    * @param ofDest  the offset within this buffer to store the passed data
    * @param bufSrc  the array of bytes to store in this buffer
    *
    * @exception NullPointerException  if <code>bufSrc</code> is
    *            <code>null</code>
    * @exception IndexOutOfBoundsException  if <tt>ofDest</tt> is negative,
    *            or if <tt>ofDest + bufSrc.length()</tt> is
    *            greater than <tt>{@link #getMaximumCapacity()}</tt>
    */
    public void write(int ofDest, ReadBuffer bufSrc);

    /**
    * Store the specified portion of the contents of the specified ReadBuffer
    * at the specified offset within this buffer.
    * <p>
    * For purposes of side-effects and potential exceptions, this method is
    * functionally equivalent to the following code:
    * <pre><code>
    * byte[] abSrc = bufSrc.toByteArray(ofSrc, cbSrc);
    * write(ofDest, abSrc, 0, abSrc.length);
    * </code></pre>
    *
    * @param ofDest  the offset within this buffer to store the passed data
    * @param bufSrc  the array of bytes to store in this buffer
    * @param ofSrc   the offset within the passed ReadBuffer to copy from
    * @param cbSrc   the number of bytes to copy from the passed ReadBuffer
    *
    * @exception NullPointerException  if <code>bufSrc</code> is
    *            <code>null</code>
    * @exception IndexOutOfBoundsException  if <tt>ofDest</tt>,
    *            <tt>ofSrc</tt> or <tt>cbSrc</tt> is negative, if
    *            <tt>ofSrc + cbSrc</tt> is greater than
    *            <tt>bufSrc.length()</tt>, or if <tt>ofDest + cbSrc</tt> is
    *            greater than <tt>{@link #getMaximumCapacity()}</tt>
    */
    public void write(int ofDest, ReadBuffer bufSrc, int ofSrc, int cbSrc);

    /**
    * Store the remaining contents of the specified InputStreaming object at
    * the specified offset within this buffer.
    * <p>
    * For purposes of side-effects and potential exceptions, this method is
    * functionally <i>similar</i> to the following code:
    * <pre><code>
    * ByteArrayOutputStream streamOut = new ByteArrayOutputStream();
    * int b;
    * while ((b = stream.read()) &gt;= 0)
    *     {
    *     streamOut.write(b);
    *     }
    * byte[] abSrc = streamOut.toByteArray();
    * write(ofDest, abSrc, 0, abSrc.length);
    * </code></pre>
    *
    * @param ofDest  the offset within this buffer to store the passed data
    * @param stream  the stream of bytes to read and store in this buffer
    *
    * @exception IOException  if an IOException occurs reading from the
    *            passed stream
    */
    public void write(int ofDest, InputStreaming stream)
            throws IOException;

    /**
    * Store the specified number of bytes from the specified InputStreaming
    * object at the specified offset within this buffer.
    * <p>
    * For purposes of side-effects and potential exceptions, this method is
    * functionally <i>similar</i> to the following code:
    * <pre><code>
    * DataInputStream streamData = new DataInputStream(
    *         new WrapperInputStream(stream));
    * byte[] abSrc = new byte[cbSrc];
    * streamData.readFully(abSrc);
    * write(ofDest, abSrc, 0, abSrc.length);
    * </code></pre>
    *
    * @param ofDest  the offset within this buffer to store the passed data
    * @param stream  the stream of bytes to read and store in this buffer
    * @param cbSrc   the exact number of bytes to read from the stream and
    *                put in this buffer
    *
    * @exception IOException  if an IOException occurs reading from the
    *            passed stream
    */
    public void write(int ofDest, InputStreaming stream, int cbSrc)
            throws IOException;


    // ----- buffer maintenance ----------------------------------------------

    /**
    * Determine the length of the data that is in the buffer. This is the
    * actual number of bytes of data that have been written to the buffer,
    * not the capacity of the buffer.
    *
    * @return the number of bytes of data represented by this WriteBuffer
    */
    public int length();

    /**
    * Starting with the byte at offset <tt>of</tt>, retain the remainder
    * of this WriteBuffer, such that the byte at offset <tt>of</tt> is
    * shifted to offset 0, the byte at offset <tt>of + 1</tt> is shifted to
    * offset 1, and so on up to the byte at offset
    * <tt>{@link #length()} - 1</tt>, which is shifted to offset
    * <tt>{@link #length()} - of - 1</tt>. After this method, the length of
    * of the buffer as indicated by the {@link #length()} method will be
    * equal to <tt>{@link #length()} - of</tt>.
    * <p>
    * This method is functionally equivalent to the following code:
    * <pre><code>
    * retain(of, length() - of);
    * </code></pre>
    *
    * @param of  the offset of the first byte within the WriteBuffer that
    *            will be retained
    *
    * @exception IndexOutOfBoundsException  if <tt>of</tt> is negative or if
    *            <tt>of</tt> is greater than <tt>{@link #length()}</tt>
    */
    public void retain(int of);

    /**
    * Starting with the byte at offset <tt>of</tt>, retain <tt>cb</tt> bytes
    * in this WriteBuffer, such that the byte at offset <tt>of</tt> is
    * shifted to offset 0, the byte at offset <tt>of + 1</tt> is shifted to
    * offset 1, and so on up to the byte at offset <tt>of + cb - 1</tt>,
    * which is shifted to offset <tt>cb - 1</tt>. After this method, the
    * length of the buffer as indicated by the {@link #length()} method will
    * be equal to <tt>cb</tt>.
    * <p>
    * Legal values for the offset of the first byte to retain <tt>of</tt> are
    * <tt>(of &gt;= 0 &amp;&amp; of &lt;= {@link #length()})</tt>. Legal values for the
    * number of bytes to retain <tt>cb</tt> are
    * <tt>(cb &gt;= 0 &amp;&amp; cb &lt;= {@link #length()})</tt>, such that
    * <tt>(of + cb &lt;= {@link #length()})</tt>.
    * <p>
    * If <tt>cb</tt> is zero, then this method will have the same effect as
    * clear. If <tt>of</tt> is zero, then this method will have the effect
    * of truncating the data in the buffer, but no bytes will be shifted
    * within the buffer.
    * <p>
    * The effect on the capacity of the buffer is implementation-specific;
    * some implementations are expected to retain the same capacity while
    * others are expected to shrink accordingly.
    *
    * @param of  the offset of the first byte within the WriteBuffer that
    *            will be retained
    * @param cb  the number of bytes to retain
    *
    * @exception IndexOutOfBoundsException  if <tt>of</tt> or <tt>cb</tt> is
    *            negative of if <tt>of + cb</tt> is greater than
    *            <tt>{@link #length()}</tt>
    */
    public void retain(int of, int cb);

    /**
    * Set the length of the buffer as indicated by the {@link #length()}
    * method to zero.
    * <p>
    * The effect on the capacity of the buffer is implementation-specific;
    * some implementations are expected to retain the same capacity while
    * others are expected to shrink accordingly.
    */
    public void clear();

    /**
    * Determine the number of bytes that the buffer can hold without resizing
    * itself. In other words, a WriteBuffer has <tt>getCapacity() -
    * {@link #length()}</tt> bytes that can be written to it without
    * overflowing the current underlying buffer allocation. Since the buffer
    * is an abstract concept, the actual mechanism for the underlying buffer
    * is not known, but it could be a Java NIO buffer, or a byte array, etc.
    * <p>
    * Note that if the maximum size returned by {@link #getMaximumCapacity()}
    * is greater than the current size returned by this method, then the
    * WriteBuffer will automatically resize itself to allocate more space
    * when the amount of data written to it passes the current size.
    *
    * @return the number of bytes of data that this WriteBuffer can hold
    *         without resizing its underlying buffer
    */
    public int getCapacity();

    /**
    * Determine the maximum number of bytes that the buffer can hold. If the
    * maximum size is greater than the current size, then the buffer is
    * expected to resize itself as necessary up to the maximum size in order
    * to contain the data given to it.
    *
    * @return the maximum number of bytes of data that the WriteBuffer can
    *         hold
    */
    public int getMaximumCapacity();


    // ----- obtaining different "write views" to the buffer ----------------

    /**
    * Obtain a WriteBuffer starting at a particular offset within this
    * WriteBuffer.
    * <p>
    * This is functionally equivalent to:
    * <pre>{@code
    * return getWriteBuffer(of, getMaximumCapacity() - of);
    * }</pre>
    *
    * @param of  the beginning index, inclusive
    *
    * @return a WriteBuffer that represents a portion of this WriteBuffer
    *
    * @exception  IndexOutOfBoundsException  if <tt>of</tt> is
    *             negative, or <tt>of</tt> is larger than the
    *             <tt>{@link #getMaximumCapacity()}</tt> of this
    *             <tt>WriteBuffer</tt> object
    */
    public WriteBuffer getWriteBuffer(int of);

    /**
    * Obtain a WriteBuffer for a portion of this WriteBuffer.
    * <p>
    * Use of the resulting buffer will correspond to using this buffer
    * directly but with the offset being passed to the buffer methods
    * automatically having <tt>of</tt> added. As a result, the length of this
    * buffer can be modified by writing to the new buffer; however, changes
    * made directly to this buffer will not affect the length of the new
    * buffer.
    * <p>
    * Note that the resulting WriteBuffer is limited in the number of bytes
    * that can be written to it; in other words, its
    * <tt>{@link #getMaximumCapacity()}</tt> must return the same value as
    * was passed in <tt>cb</tt>.
    *
    * @param of  the offset of the first byte within this WriteBuffer
    *            to map to offset 0 of the new WriteBuffer
    * @param cb  the number of bytes to cover in the resulting WriteBuffer
    *
    * @return a WriteBuffer that represents a portion of this WriteBuffer
    *
    * @exception  IndexOutOfBoundsException  if <tt>of</tt> or <tt>cb</tt>
    *             is negative, or <tt>of + cb</tt> is larger than
    *             the <tt>{@link #getMaximumCapacity()}</tt> of this
    *             <tt>WriteBuffer</tt> object
    */
    public WriteBuffer getWriteBuffer(int of, int cb);

    /**
    * Get a BufferOutput object to write data to this buffer, starting at
    * the beginning of the WriteBuffer.
    * <p>
    * This is functionally equivalent to:
    * <pre>{@code
    * return getBufferOutput(0);
    * }</pre>
    *
    * @return a BufferOutput that is writing to this buffer starting at
    *         offset zero
    */
    public BufferOutput getBufferOutput();

    /**
    * Get a BufferOutput object to write data to this buffer starting at a
    * particular offset.
    * <p>
    * Note that each call to this method will return a new BufferOutput
    * object, with the possible exception being that a zero-length
    * non-resizing WriteBuffer could always return the same instance (since
    * it is not writable).
    * <p>
    * This is functionally equivalent to:
    * <pre><code>
    * BufferOutput bufout = getBufferOutput();
    * bufout.setOffset(of);
    * return bufout;
    * </code></pre>
    *
    * @param of  the offset of the first byte of this buffer that the
    *            BufferOutput will write to
    *
    * @return a BufferOutput that will write to this buffer
    */
    public BufferOutput getBufferOutput(int of);

    /**
    * Get a BufferOutput object to write data to this buffer. The
    * BufferOutput object returned by this method is set to append to the
    * WriteBuffer, meaning that its offset is pre-set to the
    * {@link #length()} of this buffer.
    * <p>
    * This is functionally equivalent to:
    * <pre><code>
    * return getBufferOutput(length());
    * </code></pre>
    *
    * @return a BufferOutput configured to append to this buffer
    */
    public BufferOutput getAppendingBufferOutput();


    // ----- accessing the buffered data ------------------------------------

    /**
    * Get a ReadBuffer object that is a snapshot of this WriteBuffer's data.
    * <p>
    * This method is functionally equivalent to the following code:
    * <pre><code>
    * ReadBuffer buf = getUnsafeReadBuffer();
    * byte[] ab = buf.toByteArray();
    * return new ByteArrayReadBuffer(ab);
    * </code></pre>
    *
    * @return a ReadBuffer that reflects the point-in-time contents of this
    *         WriteBuffer
    */
    public ReadBuffer getReadBuffer();

    /**
    * Get a ReadBuffer object to read data from this buffer. This method is
    * not guaranteed to return a snapshot of this buffer's data, nor is it
    * guaranteed to return a live view of this buffer, which means that
    * subsequent changes to this WriteBuffer may or may not affect the
    * contents and / or the length of the returned ReadBuffer.
    * <p>
    * To get a snapshot, use the {@link #getReadBuffer()} method.
    *
    * @return a ReadBuffer that reflects the contents of this WriteBuffer
    *         but whose behavior is undefined if the WriteBuffer is modified
    */
    public ReadBuffer getUnsafeReadBuffer();

    /**
    * Returns a new byte array that holds the complete contents of this
    * WriteBuffer.
    * <p>
    * This method is functionally equivalent to the following code:
    * <pre><code>
    * return getUnsafeReadBuffer().toByteArray();
    * </code></pre>
    *
    * @return  the contents of this WriteBuffer as a byte[]
    */
    public byte[] toByteArray();

    /**
    * Returns a new Binary object that holds the complete contents of this
    * WriteBuffer.
    * <p>
    * This method is functionally equivalent to the following code:
    * <pre><code>
    * return getUnsafeReadBuffer().toBinary();
    * </code></pre>
    *
    * @return  the contents of this WriteBuffer as a Binary object
    */
    public Binary toBinary();


    // ----- Object methods -------------------------------------------------

    /**
    * Create a clone of this WriteBuffer object. Changes to the clone will
    * not affect the original, and vice-versa.
    *
    * @return a WriteBuffer object with the same contents as this
    *         WriteBuffer object
    */
    public Object clone();


    // ----- inner interface: BufferOutput ----------------------------------

    /**
    * The BufferOutput interface represents a DataOutputStream on top of a
    * WriteBuffer.
    *
    * @author cp  2005.01.18
    */
    public interface BufferOutput
            extends OutputStreaming, DataOutput
        {
        // ----- OutputStreaming methods --------------------------------

        /**
        * Close the OutputStream and release any system resources associated
        * with it.
        * <p>
        * BufferOutput implementations do not pass this call down onto an
        * underlying stream, if any.
        *
        * @exception IOException  if an I/O error occurs
        */
        public void close()
                throws IOException;

        // ----- DataOutput methods -------------------------------------

        /**
        * Writes the boolean value <tt>f</tt>.
        *
        * @param f  the boolean to be written
        *
        * @exception IOException  if an I/O error occurs
        */
        public void writeBoolean(boolean f)
                throws IOException;

        /**
        * Writes the eight low-order bits of the argument <tt>b</tt>. The 24
        * high-order bits of <tt>b</tt> are ignored.
        *
        * @param b  the byte to write (passed as an integer)
        *
        * @exception IOException  if an I/O error occurs
        */
        public void writeByte(int b)
                throws IOException;

        /**
        * Writes a short value, comprised of the 16 low-order bits of the
        * argument <tt>n</tt>; the 16 high-order bits of <tt>n</tt> are
        * ignored.
        *
        * @param n  the short to write (passed as an integer)
        *
        * @exception IOException  if an I/O error occurs
        */
        public void writeShort(int n)
                throws IOException;

        /**
        * Writes a char value, comprised of the 16 low-order bits of the
        * argument <tt>ch</tt>; the 16 high-order bits of <tt>ch</tt> are
        * ignored.
        *
        * @param ch  the char to write (passed as an integer)
        *
        * @exception IOException  if an I/O error occurs
        */
        public void writeChar(int ch)
                throws IOException;

        /**
        * Writes an int value.
        *
        * @param n  the int to write
        *
        * @exception IOException  if an I/O error occurs
        */
        public void writeInt(int n)
                throws IOException;

        /**
        * Writes a long value.
        *
        * @param l  the long to write
        *
        * @exception IOException  if an I/O error occurs
        */
        public void writeLong(long l)
                throws IOException;

        /**
        * Writes a float value.
        *
        * @param fl  the float to write
        *
        * @exception IOException  if an I/O error occurs
        */
        public void writeFloat(float fl)
                throws IOException;

        /**
        * Writes a double value.
        *
        * @param dfl  the double to write
        *
        * @exception IOException  if an I/O error occurs
        */
        public void writeDouble(double dfl)
                throws IOException;

        /**
        * Writes the String <tt>s</tt>, but only the low-order byte from each
        * character of the String is written.
        *
        * @param s  the String to write
        *
        * @exception IOException  if an I/O error occurs
        * @exception NullPointerException  if <tt>s</tt> is <tt>null</tt>
        */
        public void writeBytes(String s)
                throws IOException;

        /**
        * Writes the String <tt>s</tt> as a sequence of characters.
        *
        * @param s  the String to write
        *
        * @exception IOException  if an I/O error occurs
        * @exception NullPointerException  if <tt>s</tt> is <tt>null</tt>
        */
        public void writeChars(String s)
                throws IOException;

        /**
        * Writes the String <tt>s</tt> as a sequence of characters, but using
        * UTF-8 encoding for the characters, and including the String length
        * data so that the corresponding {@link java.io.DataInput#readUTF}
        * method can reconstitute a String from the written data.
        *
        * @param s  the String to write
        *
        * @exception IOException  if an I/O error occurs
        * @exception NullPointerException  if <tt>s</tt> is <tt>null</tt>
        */
        public void writeUTF(String s)
                throws IOException;

        // ----- BufferOutput methods -----------------------------------

        /**
        * Get the WriteBuffer object that this BufferOutput is writing to.
        *
        * @return the underlying WriteBuffer object
        */
        public WriteBuffer getBuffer();

        /**
         * Ensure that there are at least {@code cb} bytes of capacity left in
         * this {@link BufferOutput}, and return a mutable {@link ByteBuffer} of
         * {@code cb} bytes, starting at the current offset.
         *
         * @param cb  the size of the {@link ByteBuffer} to return
         *
         * @return a mutable {@link ByteBuffer} of {@code cb} bytes, starting
         *         at the current offset
         *
         * @since 24.09
         */
        ByteBuffer getByteBuffer(int cb);

        /**
        * Write a variable-length encoded UTF packed String. The major
        * differences between this implementation and DataOutput is that this
        * implementation supports null values and is not limited to 64KB
        * UTF-encoded values.
        * <p>
        * The binary format for a Safe UTF value is a "packed int" for the
        * binary length followed by the UTF-encoded byte stream. The length
        * is either -1 (indicating a null String) or in the range
        * <tt>0 .. Integer.MAX_VALUE</tt> (inclusive). The UTF-encoded
        * portion uses a format identical to DataOutput.
        *
        * @param s  a String value to write; may be null
        *
        * @exception IOException  if an I/O error occurs
        */
        public void writeSafeUTF(String s)
                throws IOException;

        /**
        * Write an int value using a variable-length storage-format.
        * <p>
        * The format differs from DataOutput in that DataOutput always uses
        * a fixed-length 4-byte Big Endian binary format for int values.
        * The "packed" format includes a sign bit (0x40) and a continuation
        * bit (0x80) in the first byte, followed by the least 6 significant
        * bits of the int value. Subsequent bytes (each appearing only if
        * the previous byte had its continuation bit set) include a
        * continuation bit (0x80) and the next least 7 significant bits of
        * the int value. In this way, a 32-bit value is encoded into 1-5
        * bytes, depending on the magnitude of the int value being encoded.
        *
        * @param n  an int value to write
        *
        * @exception IOException  if an I/O error occurs
        */
        public void writePackedInt(int n)
                throws IOException;

        /**
        * Write a long value using a variable-length storage-format.
        * <p>
        * The format differs from DataOutput in that DataOutput always uses
        * a fixed-length 8-byte Big Endian binary format for long values.
        * The "packed" format includes a sign bit (0x40) and a continuation
        * bit (0x80) in the first byte, followed by the least 6 significant
        * bits of the long value. Subsequent bytes (each appearing only if
        * the previous byte had its continuation bit set) include a
        * continuation bit (0x80) and the next least 7 significant bits of
        * the long value. In this way, a 64-bit value is encoded into 1-10
        * bytes, depending on the magnitude of the long value being encoded.
        *
        * @param l  a long value to write
        *
        * @exception IOException  if an I/O error occurs
        */
        public void writePackedLong(long l)
                throws IOException;

        /**
        * Write all the bytes from the passed ReadBuffer object.
        * <p>
        * This is functionally equivalent to the following code:
        * <pre>{@code
        * getBuffer().write(getOffset(), buf);
        * }</pre>
        *
        * @param buf  a ReadBuffer object
        *
        * @exception IOException  if an I/O error occurs
        */
        public void writeBuffer(ReadBuffer buf)
                throws IOException;

        /**
        * Write <code>cb</code> bytes from the passed ReadBuffer object
        * starting at offset <code>of</code> within the passed ReadBuffer.
        * <p>
        * This is functionally equivalent to the following code:
        * <pre>{@code
        * getBuffer().write(getOffset(), buf, of, cb);
        * }</pre>
        *
        * @param buf  a ReadBuffer object
        * @param of   the offset within the ReadBuffer of the first byte to
        *             write to this BufferOutput
        * @param cb   the number of bytes to write
        *
        * @exception IOException  if an I/O error occurs
        */
        public void writeBuffer(ReadBuffer buf, int of, int cb)
                throws IOException;

        /**
        * Write the remaining contents of the specified InputStreaming
        * object.
        * <p>
        * This is functionally equivalent to the following code:
        * <pre>{@code
        * getBuffer().write(getOffset(), stream);
        * }</pre>
        *
        * @param stream  the stream of bytes to write to this BufferOutput
        *
        * @exception IOException  if an I/O error occurs, specifically if an
        *            IOException occurs reading from the passed stream
        */
        public void writeStream(InputStreaming stream)
                throws IOException;

        /**
        * Write the specified number of bytes of the specified InputStreaming
        * object.
        * <p>
        * This is functionally equivalent to the following code:
        * <pre>{@code
        * getBuffer().write(getOffset(), stream, cb);
        * }</pre>
        *
        * @param stream  the stream of bytes to write to this BufferOutput
        * @param cb      the exact number of bytes to read from the stream
        *                and write to this BufferOutput
        *
        * @exception EOFException  if the stream is exhausted before
        *            the number of bytes indicated could be read
        * @exception IOException  if an I/O error occurs, specifically if an
        *            IOException occurs reading from the passed stream
        */
        public void writeStream(InputStreaming stream, int cb)
                throws IOException;

        /**
        * Determine the current offset of this BufferOutput within the
        * underlying WriteBuffer.
        *
        * @return the offset of the next byte to write to the WriteBuffer
        */
        public int getOffset();

        /**
        * Specify the offset of the next byte to write to the underlying
        * WriteBuffer.
        *
        * @param of  the offset of the next byte to write to the
        *            WriteBuffer
        *
        * @exception  IndexOutOfBoundsException  if <code>of &lt; 0</code> or
        *             <code>of &gt; getBuffer().getMaximumCapacity()</code>
        */
        public void setOffset(int of);

        // ----- constants ----------------------------------------------

        /**
        * Maximum encoding length for a packed int value.
        */
        public static final int MAX_PACKED_INT_SIZE = 5;

        /**
        * Maximum encoding length for a packed long value.
        */
        public static final int MAX_PACKED_LONG_SIZE = 10;
        }
    }
