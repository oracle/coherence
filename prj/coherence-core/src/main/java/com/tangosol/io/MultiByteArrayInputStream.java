/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io;


import java.io.InputStream;
import java.io.IOException;


/**
* Reads binary data from a series of byte arrays.
*
* @author cp  2001.11.03
*/
public class MultiByteArrayInputStream
        extends InputStream
        implements InputStreaming
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a MultiByteArrayInputStream.
    *
    * @param aab  a non-null array of byte arrays of data to stream, each
    *             byte array must be non-null and non-zero-length
    */
    public MultiByteArrayInputStream(byte[][] aab)
        {
        this(aab, false);
        }

    /**
    * Construct a MultiByteArrayInputStream.
    *
    * @param aab           a non-null array of byte arrays of data to stream, each
    *                      byte array must be non-null and non-zero-length
    * @param fDestructive  if true the supplied array will be cleared as it is
    *                      traversed, stream mark positions will be respected
    */
    public MultiByteArrayInputStream(byte[][] aab, boolean fDestructive)
        {
        m_aabArray     = aab;
        m_fDestructive = fDestructive;
        if (aab == null || aab.length == 0)
            {
            m_ab   = EMPTY_BYTES;
            m_fEOF = true;
            }
        else
            {
            m_ab   = aab[0];
            m_fEOF = aab.length == 1 && aab[0].length == 0;
            }
        }


    // ----- InputStream implementation -------------------------------------

    /**
    * Reads the next byte of data from the input stream. The value byte is
    * returned as an <code>int</code> in the range <code>0</code> to
    * <code>255</code>. If no byte is available because the end of the stream
    * has been reached, the value <code>-1</code> is returned. This method
    * blocks until input data is available, the end of the stream is detected,
    * or an exception is thrown.
    *
    * @return     the next byte of data, or <code>-1</code> if the end of the
    *             stream is reached.
    * @exception  IOException  if an I/O error occurs.
    */
    public int read() throws IOException
        {
        if (m_fEOF)
            {
            return -1;
            }

        // check "available"
        byte[] ab  = m_ab;
        int    cb  = ab.length;
        int    of  = m_of;
        int    n   = ab[of++] & 0xFF;
        if (of == cb)
            {
            byte[][] aab = m_aabArray;
            int      cab = aab.length;
            int      iab = m_iArray;

            // destroy chunk
            if (m_fDestructive && iab < m_iArrayMarked)
                {
                aab[iab] = null;
                }

            // check for EOF
            if (iab == cab - 1)
                {
                m_fEOF = true;
                }
            else
                {
                m_iArray = ++iab;
                m_ab     = aab[iab];
                m_of     = 0;
                }
            }
        else
            {
            m_of = of;
            }

        return n;
        }

    /**
    * Reads up to <code>len</code> bytes of data from the input stream into
    * an array of bytes.  An attempt is made to read as many as
    * <code>len</code> bytes, but a smaller number may be read, possibly
    * zero. The number of bytes actually read is returned as an integer.
    *
    * @param abDest  the buffer into which the data is read
    * @param ofDest  the start offset in array <code>b</code>
    *                at which the data is written
    * @param cbDest  the maximum number of bytes to read
    *
    * @return the total number of bytes read into the buffer, or
    *         <code>-1</code> if there is no more data because the end of
    *         the stream has been reached.
    */
    public int read(byte abDest[], int ofDest, int cbDest) throws IOException
        {
        if (m_fEOF)
            {
            return -1;
            }

        int cbRead = 0;
        while (true)
            {
            byte[] ab     = m_ab;
            int    of     = m_of;
            int    cb     = ab.length;
            int    cbLeft = cb - of;

            // check if read can be handled inside the current chunk
            if (cbDest < cbLeft)
                {
                System.arraycopy(ab, of, abDest, ofDest, cbDest);
                m_of    = of + cbDest;
                cbRead += cbDest;
                return cbRead;
                }

            // copy what is available from the current chunk
            System.arraycopy(ab, of, abDest, ofDest, cbLeft);
            cbRead += cbLeft;

            byte[][] aab = m_aabArray;
            int      cab = aab.length;
            int      iab = m_iArray;

            // destroy chunk
            if (m_fDestructive && iab < m_iArrayMarked)
                {
                aab[iab] = null;
                }

            // check for EOF
            if (iab == cab - 1)
                {
                m_of   = ab.length;
                m_fEOF = true;
                return cbRead;
                }

            // advance to next chunk
            m_iArray  = ++iab;
            m_ab      = aab[iab];
            m_of      = 0;

            ofDest += cbLeft;
            cbDest -= cbLeft;
            }
        }

    /**
    * Returns the number of bytes that can be read (or skipped over) from
    * this input stream without blocking by the next caller of a method for
    * this input stream.  The next caller might be the same thread or or
    * another thread.
    *
    * @return     the number of bytes that can be read from this input stream
    *             without blocking.
    */
    public int available()
        {
        if (m_fEOF)
            {
            return 0;
            }

        byte[][] aab = m_aabArray;
        int      of  = m_of;
        int      cb  = 0;
        for (int i = m_iArray, c = aab.length; i < c; ++i)
            {
            cb += aab[i].length - of;
            of  = 0;
            }
        return cb;
        }

    /**
    * Skips over and discards <code>n</code> bytes of data from this input
    * stream. The <code>skip</code> method may, for a variety of reasons, end
    * up skipping over some smaller number of bytes, possibly <code>0</code>.
    * This may result from any of a number of conditions; reaching end of file
    * before <code>n</code> bytes have been skipped is only one possibility.
    * The actual number of bytes skipped is returned.  If <code>n</code> is
    * negative, no bytes are skipped.
    *
    * @param n  the number of bytes to be skipped
    *
    * @return the actual number of bytes skipped
    */
    public long skip(long n)
        {
        if (n < 0L || n > Integer.MAX_VALUE)
            {
            throw new IllegalArgumentException("out of bounds: skip(n=" + n + ")");
            }

        if (m_fEOF)
            {
            return 0L;
            }

        byte[] ab = m_ab;
        int    of = m_of;
        int    cb = ab.length;

        int    cbSkip = (int) n;
        int    cbLeft = cb - of;

        // check if skip occurs inside the current chunk
        if (cbSkip < cbLeft)
            {
            m_of = of + cbSkip;
            return cbSkip;
            }

        byte[][] aab = m_aabArray;
        int      cab = aab.length;
        int      iab = m_iArray;

        // destroy chunk
        if (m_fDestructive && iab < m_iArrayMarked)
            {
            aab[iab] = null;
            }

        // check for EOF
        if (iab == cab - 1 && cbSkip > cbLeft)
            {
            m_of   = ab.length;
            m_fEOF = true;
            return cbLeft;
            }

        // advance to next chunk (recursively)
        m_iArray  = ++iab;
        m_ab      = aab[iab];
        m_of      = 0;
        return cbLeft + skip(cbSkip - cbLeft);
        }

    /**
    * Close the stream.
    */
    public void close()
        {
        }

    /**
    * Marks the current position in this input stream. A subsequent call to
    * the <code>reset</code> method repositions this stream at the last
    * marked position so that subsequent reads re-read the same bytes.
    *
    * @param readlimit  the maximum limit of bytes that can be read before
    *                   the mark position becomes invalid
    */
    public void mark(int readlimit)
        {
        int iMarkOld = m_iArrayMarked;
        int iMarkNew = m_iArrayMarked = m_iArray;

        m_ofMarked = m_of;

        // destroy chunks between old and new mark
        if (m_fDestructive && iMarkOld != MARK_UNSET)
            {
            for (byte[][] aabArray = m_aabArray; iMarkOld < iMarkNew; ++iMarkOld)
                {
                aabArray[iMarkOld] = null;
                }
            }
        }

    /**
    * Repositions this stream to the position at the time the
    * <code>mark</code> method was last called on this input stream.
    *
    * @throws IOException if the stream has not been marked
    */
    public void reset()
        throws IOException
        {
        int iMarked = m_iArrayMarked;
        if (iMarked == MARK_UNSET)
            {
            throw new IOException("the stream has not been marked");
            }

        byte[][] aabArray = m_aabArray;

        m_iArray = iMarked;
        m_of     = m_ofMarked;
        m_ab     = aabArray[iMarked];
        m_fEOF   = iMarked == aabArray.length - 1 && m_of == m_ab.length;
        }

    /**
    * Tests if this input stream supports the <code>mark</code> and
    * <code>reset</code> methods. The <code>markSupported</code> method
    * of <code>InputStream</code> returns <code>false</code>.
    *
    * @return  <code>true</code> if this true type supports the mark and
    *          reset method; <code>false</code> otherwise
    */
    public boolean markSupported()
        {
        return true;
        }

    
    // ----- data members ---------------------------------------------------

    /**
    * Empty array of bytes.
    */
    protected static final byte[] EMPTY_BYTES = new byte[0];

    /**
    * Marker position indicating that stream is not marked.
    */
    protected static final int MARK_UNSET = Integer.MAX_VALUE;

    /**
    * True after eof is determined.
    */
    protected boolean m_fEOF;

    /**
    * True iff the array will be null'd out as it is traversed.
    */
    protected boolean m_fDestructive;

    /**
    * The array of byte arrays.
    */
    protected byte[][] m_aabArray;

    /**
    * The index of the current byte array.
    */
    protected int m_iArray;

    /**
    * The current byte array.
    */
    protected byte[] m_ab;

    /**
    * The current offset in the current byte array.
    */
    protected int m_of;

    /**
    * The index of the marked byte array.
    */
    protected int m_iArrayMarked = MARK_UNSET;

    /**
    * The marked offset in the marked byte array.
    */
    protected int m_ofMarked;
    }
