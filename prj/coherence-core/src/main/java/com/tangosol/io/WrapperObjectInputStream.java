/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io;


import java.io.ObjectInput;
import java.io.IOException;


/**
* This is an imitation ObjectInputStream class that provides the
* ObjectInput interface by delegating to an object that implements the
* ObjectInput interface. Primarily, this is intended as a base class for
* building specific-purpose ObjectInput wrappers.
*
* @author cp  2004.08.20
*/
public class WrapperObjectInputStream
        extends WrapperDataInputStream
        implements ObjectInput
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a WrapperObjectInputStream that will read from the specified
    * object implementing the ObjectInput interface.
    *
    * @param in  an object implementing ObjectInput to read from
    */
    public WrapperObjectInputStream(ObjectInput in)
        {
        super(in);
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Obtain the underlying object providing the ObjectInput interface that
    * this object is delegating to.
    *
    * @return the underlying ObjectInput
    */
    public ObjectInput getObjectInput()
        {
        return (ObjectInput) getDataInput();
        }


    // ----- ObjectInput methods --------------------------------------------

    /**
    * Read and return an object.
    *
    * @return the object read from the stream
    *
    * @exception ClassNotFoundException  if the class of a serialized object
    *            object cannot be found
    * @exception IOException  if an I/O error occurs
    */
    public Object readObject()
            throws ClassNotFoundException, IOException
        {
        return getObjectInput().readObject();
        }

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
        return getObjectInput().read();
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
        return getObjectInput().read(ab);
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
        return getObjectInput().read(ab, of, cb);
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
        return getObjectInput().skip(cb);
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
        return getObjectInput().available();
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
        getObjectInput().close();
        }
    }
