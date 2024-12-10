/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UTFDataFormatException;


/**
* A Utf8Reader is used to read character data from an underlying stream.
*
* @author cp  2002.01.04
*/
public class Utf8Reader
        extends Reader
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a Utf8Reader that reads the input from a byte array.
    */
    public Utf8Reader(byte[] ab)
        {
        this(new ByteArrayInputStream(ab));
        }

    /**
    * Construct a Utf8Reader that reads the input from an InputStream.
    *
    * @param stream   the underlying stream to write to
    */
    public Utf8Reader(InputStream stream)
        {
        m_stream = stream;
        }


    // ----- Reader methods -------------------------------------------------

    /**
    * Read a single character.  This method will block until a character is
    * available, an I/O error occurs, or the end of the stream is reached.
    *
    * <p> Subclasses that intend to support efficient single-character input
    * should override this method.
    *
    * @return     The character read, as an integer in the range 0 to 65535
    *             (<tt>0x00-0xffff</tt>), or -1 if the end of the stream has
    *             been reached
    *
    * @exception  IOException  If an I/O error occurs
    */
    public int read()
            throws IOException
        {
        InputStream stream = m_stream;
        int ch = stream.read();
        switch ((ch & 0xF0) >>> 4)
            {
            case 0x0: case 0x1: case 0x2: case 0x3:
            case 0x4: case 0x5: case 0x6: case 0x7:
                // 1-byte format:  0xxx xxxx
                return (char) ch;

            case 0xC: case 0xD:
                {
                // 2-byte format:  110x xxxx, 10xx xxxx
                int ch2 = stream.read();
                if ((ch2 & 0xC0) != 0x80)
                    {
			        throw new UTFDataFormatException(); 
                    }
                return ((ch & 0x1F) << 6) | ch2 & 0x3F;
                }

            case 0xE:
                {
                // 3-byte format:  1110 xxxx, 10xx xxxx, 10xx xxxx
                int ch2 = stream.read();
                int ch3 = stream.read();
                if ((ch2 & 0xC0) != 0x80 || (ch3 & 0xC0) != 0x80)
                    {
			        throw new UTFDataFormatException(); 
                    }
		        return ((ch & 0x0F) << 12) | ((ch2 & 0x3F) << 6) | ch3 & 0x3F;
                }

            case 0xF:
                // could be EOF
                if (ch == -1)
                    {
                    return -1;
                    }
                // fall through
            default:
                throw new UTFDataFormatException("illegal leading UTF byte: " + ch);
            }
        }

    /**
    * Read characters into an array.  This method will block until some input
    * is available, an I/O error occurs, or the end of the stream is reached.
    *
    * @param ach  Destination buffer
    *
    * @return The number of bytes read, or -1 if the end of the stream
    *         has been reached
    *
    * @exception IOException  If an I/O error occurs
    */
    public int read(char ach[])
            throws IOException
        {
        return read(ach, 0, ach.length);
        }

    /**
    * Read characters into a portion of an array.  This method will block
    * until some input is available, an I/O error occurs, or the end of the
    * stream is reached.
    *
    * @param ach  array of characters to read into
    * @param of   offset into the array at which to start storing characters
    * @param cch  maximum number of characters to read
    *
    * @return the number of characters read, or -1 if the end of the
    *         stream has been reached
    *
    * @exception  IOException  If an I/O error occurs
    */
    public int read(char ach[], int of, int cch)
            throws IOException
        {
        int cchActual = 0;

        for (int ofEnd = of + cch; of < ofEnd; ++of, ++cchActual)
            {
            int ch = read();

            // check for EOF
            if (ch == -1)
                {
                // if it is the first read we tried to do, then report back
                // an EOF, otherwise stop reading and report back what was
                // read so far
                if (cchActual == 0)
                    {
                    return -1;
                    }
                else
                    {
                    break;
                    }
                }

            ach[of] = (char) ch;
            }

        return cchActual;
        }

    /**
    * Skip characters.  This method will block until some characters are
    * available, an I/O error occurs, or the end of the stream is reached.
    *
    * @param cch  The number of characters to skip
    *
    * @return The number of characters actually skipped
    *
    * @exception  IllegalArgumentException  If <code>n</code> is negative.
    * @exception  IOException  If an I/O error occurs
    */
    public long skip(long cch)
            throws IOException
        {
        int cchActual = 0;

        while (cchActual < cch)
            {
            if (read() == -1)
                {
                break;
                }

            ++cchActual;
            }

        return cchActual;
        }

    /**
    * Tell whether this stream is ready to be read.
    *
    * @return True if the next read() is guaranteed not to block for input,
    * false otherwise.  Note that returning false does not guarantee that the
    * next read will block.
    *
    * @exception  IOException  If an I/O error occurs
    */
    public boolean ready()
            throws IOException
        {
        return (m_stream instanceof ByteArrayInputStream);
        }

    /**
    * Tell whether this stream supports the mark() operation. The default
    * implementation always returns false. Subclasses should override this
    * method.
    *
    * @return true if and only if this stream supports the mark operation.
    */
    public boolean markSupported()
        {
        return m_stream.markSupported();
        }

    /**
    * Mark the present position in the stream.  Subsequent calls to reset()
    * will attempt to reposition the stream to this point.  Not all
    * character-input streams support the mark() operation.
    *
    * @param cchLimit  Limit on the number of characters that may be
    *                  read while still preserving the mark.  After
    *                  reading this many characters, attempting to
    *                  reset the stream may fail.
    *
    * @exception IOException  If the stream does not support mark(),
    *                         or if some other I/O error occurs
    */
    public void mark(int cchLimit)
            throws IOException
        {
        m_stream.mark(cchLimit * 3);
        }

    /**
    * Reset the stream.  If the stream has been marked, then attempt to
    * reposition it at the mark.  If the stream has not been marked, then
    * attempt to reset it in some way appropriate to the particular stream,
    * for example by repositioning it to its starting point.  Not all
    * character-input streams support the reset() operation, and some support
    * reset() without supporting mark().
    *
    * @exception  IOException  If the stream has not been marked,
    *                          or if the mark has been invalidated,
    *                          or if the stream does not support reset(),
    *                          or if some other I/O error occurs
    */
    public void reset()
            throws IOException
        {
        m_stream.reset();
        }

    /**
    * Close the stream.  Once a stream has been closed, further read(),
    * ready(), mark(), or reset() invocations will throw an IOException.
    * Closing a previously-closed stream, however, has no effect.
    *
    * @exception  IOException  If an I/O error occurs
    */
    public void close()
            throws IOException
        {
        }


    // ----- data members ---------------------------------------------------

    /**
    * The stream to read the UTF8 data from.
    */
    private InputStream m_stream;
    }
