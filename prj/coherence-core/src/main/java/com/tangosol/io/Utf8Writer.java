/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;


/**
* A Utf8Writer is used to write character data onto an underlying stream.
*
* @author cp  2002.01.04
*/
public class Utf8Writer
        extends Writer
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a Utf8Writer that buffers the output.
    */
    public Utf8Writer()
        {
        this(new ByteArrayOutputStream());
        }

    /**
    * Construct a Utf8Writer that puts the output into an OutputStream.
    *
    * @param stream   the underlying stream to write to
    */
    public Utf8Writer(OutputStream stream)
        {
        m_stream = stream;
        }


    // ----- Writer methods -------------------------------------------------

    /**
    * Write a single character.  The character to be written is contained in
    * the 16 low-order bits of the given integer value; the 16 high-order bits
    * are ignored.
    *
    * <p> Subclasses that intend to support efficient single-character output
    * should override this method.
    *
    * @param ch  int specifying a character to be written.
    */
    public void write(int ch)
            throws IOException
        {
        OutputStream stream = m_stream;

	    if (ch >= 0x0001 && ch <= 0x007F)
            {
            // 1-byte format:  0xxx xxxx
		    stream.write(ch);
            }
        else if (ch <= 0x07FF)
            {
            // 2-byte format:  110x xxxx, 10xx xxxx
		    stream.write(0xC0 | ((ch >>> 6) & 0x1F));
		    stream.write(0x80 | ((ch >>> 0) & 0x3F));
            }
        else
            {
            // 3-byte format:  1110 xxxx, 10xx xxxx, 10xx xxxx
		    stream.write(0xE0 | ((ch >>> 12) & 0x0F));
		    stream.write(0x80 | ((ch >>>  6) & 0x3F));
		    stream.write(0x80 | ((ch >>>  0) & 0x3F));
            }
        }

    /**
    * Write an array of characters.
    *
    * @param  ach  array of characters to write
    */
    public void write(char ach[])
            throws IOException
        {
        for (int of = 0, cch = ach.length; of < cch; ++of)
            {
            write(ach[of]);
            }
        }

    /**
    * Write a portion of an array of characters.
    *
    * @param  ach  array of characters to write from
    * @param  of   offset from which to start writing characters
    * @param  cch  number of characters to write
    */
    public void write(char ach[], int of, int cch)
            throws IOException
        {
        for (int ofEnd = of + cch; of < ofEnd; ++of)
            {
            write(ach[of]);
            }
        }

    /**
    * Write a string.
    *
    * @param  s  the String to write
    */
    public void write(String s)
            throws IOException
        {
        int cch = s.length();
        if (cch <= MAX_BUF)
            {
            write(s, 0, cch);
            }
        else
            {
	        write(s.toCharArray(), 0, cch);
            }
        }

    /**
    * Write a portion of a string.
    *
    * @param  s    the String to write from
    * @param  of   offset from which to start writing characters
    * @param  cch  number of characters to write
    */
    public void write(String s, int of, int cch)
            throws IOException
        {
    	char ach[];
	    if (cch <= MAX_BUF)
            {
            ach = m_achBuf;
		    if (ach == null)
                {
		        m_achBuf = ach = new char[MAX_BUF];
		        }
	        }
        else
            {
		    ach = new char[cch];
	        }

	    s.getChars(of, (of + cch), ach, 0);
	    write(ach, 0, cch);
        }

    /**
    * Flush the stream.  If the stream has saved any characters from the
    * various write() methods in a buffer, write them immediately to their
    * intended destination.  Then, if that destination is another character or
    * byte stream, flush it.  Thus one flush() invocation will flush all the
    * buffers in a chain of Writers and OutputStreams.
    *
    * @exception  IOException  If an I/O error occurs
    */
    public void flush()
            throws IOException
        {
        }

    /**
    * Close the stream, flushing it first.  Once a stream has been closed,
    * further write() or flush() invocations will cause an IOException to be
    * thrown.  Closing a previously-closed stream, however, has no effect.
    *
    * @exception  IOException  If an I/O error occurs
    */
    public void close()
            throws IOException
        {
        flush();
        }


    // ----- accessors ------------------------------------------------------

    /**
    * If the underlying stream is a ByteArrayOutputStream (such as with the
    * no-parameter constructor), this will return the binary UTF8-encoded
    * data that resulted from the character data written to this Writer.
    *
    * @return a byte array of the UTF8 data
    *
    * @throws ClassCastException  if the underlying stream is not
    *         ByteArrayOutputStream
    */
    public byte[] toByteArray()
        {
        return ((ByteArrayOutputStream) m_stream).toByteArray();
        }


    // ----- data members ---------------------------------------------------

    /**
    * The stream to write the UTF8 data to.
    */
    private OutputStream m_stream;

    /**
    * A semi-permanent buffer to use to hold character data.
    */
    private transient char[] m_achBuf;

    /**
    * Maximum size of buffer.
    */
    private static final int MAX_BUF = 1024;
    }
