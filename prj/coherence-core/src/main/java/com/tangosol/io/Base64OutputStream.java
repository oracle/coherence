/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io;


import com.tangosol.util.Base;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;


/**
* Writes binary data into a Writer using IETF RFC 2045 Base64 Content
* Transfer Encoding.
*
* If the Base64OutputStream is not the first to write data into the
* Writer, it may be desired to write a line feed before Base64 data.
* According to the specification, Base64 data cannot exceed 76
* characters per line.
*
* Be careful to avoid calling flush() except when a stream of Base64
* content is complete.
*
* @author cp  2000.09.06
*/
public class Base64OutputStream
        extends OutputStream
        implements OutputStreaming
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a Base64OutputStream on a Writer object.
    *
    * @param writer  the Writer to write the Base64 encoded data to
    */
    public Base64OutputStream(Writer writer)
        {
        this(writer, true);
        }

    /**
    * Construct a Base64OutputStream on a Writer object and specifying
    * a line-break option.
    *
    * @param writer       the Writer to write the Base64 encoded data to
    * @param fBreakLines  true to break the output into 76-character lines
    */
    public Base64OutputStream(Writer writer, boolean fBreakLines)
        {
        Base.azzert(writer != null);

        m_writer      = writer;
        m_fBreakLines = fBreakLines;
        }

    // ----- OutputStream implementation ------------------------------------

    /**
    * Writes the specified byte to this output stream.
    *
    * @param      b   the <code>byte</code>.
    *
    * @exception  IOException  if an I/O error occurs. In particular,
    *             an <code>IOException</code> may be thrown if the
    *             output stream has been closed.
    */
    public void write(int b) throws IOException
        {
        if (m_fClosed)
            {
            throw new IOException("Base64OutputStream is closed");
            }

        m_abAccum[m_cAccum++] = (byte) (b & 0xFF);
        if (m_cAccum == 3)
            {
            flushAccumulator();
            }
        }

    /**
    * Writes <code>len</code> bytes from the specified byte array
    * starting at offset <code>off</code> to this output stream.
    * <p>
    * If <code>b</code> is <code>null</code>, a
    * <code>NullPointerException</code> is thrown.
    * <p>
    * If <code>off</code> is negative, or <code>len</code> is negative, or
    * <code>off+len</code> is greater than the length of the array
    * <code>b</code>, then an <tt>IndexOutOfBoundsException</tt> is thrown.
    *
    * @param      ab    the data
    * @param      ofb   the start offset in the data
    * @param      cb    the number of bytes to write
    *
    * @exception  IOException  if an I/O error occurs. In particular,
    *             an <code>IOException</code> is thrown if the output
    *             stream is closed.
    */
    public void write(byte[] ab, int ofb, int cb) throws IOException
        {
        // there is a point below which it does not make sense to
        // perform any optimizations
        if (cb > 256)
            {
            // if nothing has been written or line feeds are off and the
            // accumulator is empty then:
            //      m_cLineGroups == GROUPS_PER_LINE && m_cAccum == 0
            // otherwise write until:
            //      m_cLineGroups == 0 && m_cAccum == 0
            // or (in the case of line feeds being off)
            //      m_cLineGroups == GROUPS_PER_LINE && m_cAccum == 0

            // empty the accumulator and complete the current line
            while (!(m_cAccum == 0 &&
                    (m_cLineGroups == GROUPS_PER_LINE || m_cLineGroups == 0)))
                {
                write(ab[ofb++]);
                --cb;
                }

            // write a new line if necessary (note that the line groups counter
            // is not reset since the block to be written will terminate on a
            // line boundary as well)
            if (m_cLineGroups == 0)
                {
                m_writer.write(BASE64_LF);
                }

            // determine the binary block size to format; if not chunking into
            // lines, then the contents can be written in 3-byte groups, other-
            // wise the contents must break on a line boundary
            int cbChunk = m_fBreakLines ? GROUPS_PER_LINE * 3 : 3;
            int cChunks = cb / cbChunk;
            int cbBlock = cChunks * cbChunk;

            m_writer.write(encode(ab, ofb, cbBlock, m_fBreakLines));

            ofb += cbBlock;
            cb  -= cbBlock;
            if (m_fBreakLines)
                {
                m_cLineGroups = 0;
                }
            }

        // write remainder
        int ofbEnd = ofb + cb;
        while (ofb < ofbEnd)
            {
            write(ab[ofb++]);
            }
        }

    /**
    * Close the stream, flushing any accumulated bytes.  The underlying
    * writer is not closed.
    *
    * @exception  IOException  if an I/O error occurs
    */
    public void flush() throws IOException
        {
        if (m_fClosed)
            {
            throw new IOException("Base64OutputStream is closed");
            }

        flushAccumulator();
        m_writer.flush();
        }

    /**
    * Close the stream, flushing any accumulated bytes.  The underlying
    * writer is not closed.
    *
    * @exception  IOException  if an I/O error occurs
    */
    public void close() throws IOException
        {
        flush();
        m_fClosed = true;
        }

    /**
    * Flushes the bytes accumulated by the write(int) method.
    *
    * @exception  IOException  if an I/O error occurs
    */
    protected void flushAccumulator() throws IOException
        {
        int cAccum = m_cAccum;
        if (cAccum == 0)
            {
            return;
            }

        int cLinesGroups = m_cLineGroups;
        if (cLinesGroups == 0)
            {
            m_writer.write(BASE64_LF);
            cLinesGroups = GROUPS_PER_LINE;
            }

        byte[] ab    = m_abAccum;
        char[] ach   = m_achGroup;
        char[] alpha = BASE64_ALPHABET;
        switch (cAccum)
            {
            case 1:
                {
                int n = (ab[0] & 0xFF);
                ach[0] = alpha[n >> 2       ]; // 1111 1100
                ach[1] = alpha[n << 4 & 0x3F]; // 0000 0011 1111
                ach[2] = BASE64_PAD;
                ach[3] = BASE64_PAD;
                }
                break;

            case 2:
                {
                int n = (ab[0] & 0xFF) << 8 | (ab[1] & 0xFF);
                ach[0] = alpha[n >> 10       ]; // 1111 1100 0000 0000
                ach[1] = alpha[n >>  4 & 0x3F]; // 0000 0011 1111 0000
                ach[2] = alpha[n <<  2 & 0x3F]; // 0000 0000 0000 1111 11
                ach[3] = BASE64_PAD;
                }
                break;

            case 3:
                {
                int n = (ab[0] & 0xFF) << 16 | (ab[1] & 0xFF) << 8 | (ab[2] & 0xFF);
                ach[0] = alpha[n >> 18       ]; // 1111 1100 0000 0000 0000 0000
                ach[1] = alpha[n >> 12 & 0x3F]; // 0000 0011 1111 0000 0000 0000
                ach[2] = alpha[n >> 6  & 0x3F]; // 0000 0000 0000 1111 1100 0000
                ach[3] = alpha[n       & 0x3F]; // 0000 0000 0000 0000 0011 1111
                }
                break;

            default:
                Base.azzert();
                break;
            }

        m_writer.write(ach);
        if (m_fBreakLines)
            {
            m_cLineGroups = cLinesGroups - 1;
            }
        m_cAccum = 0;
        }


    // ----- static helpers -------------------------------------------------

    /**
    * Encode the passed binary data using Base64 encoding.
    *
    * @param ab  the array containing the bytes to encode
    *
    * @return the encoded data as a char array
    */
    public static char[] encode(byte[] ab)
        {
        return encode(ab, true);
        }

    /**
    * Encode the passed binary data using Base64 encoding.
    *
    * @param ab           the array containing the bytes to encode
    * @param fBreakLines  true to break the output into 76-character lines
    *
    * @return the encoded data as a char array
    */
    public static char[] encode(byte[] ab, boolean fBreakLines)
        {
        return encode(ab, 0, ab.length, fBreakLines);
        }

    /**
    * Encode the passed binary data using Base64 encoding.
    *
    * @param ab           the array containing the bytes to encode
    * @param ofb          the start offset in the byte array
    * @param cb           the number of bytes to encode
    * @param fBreakLines  true to break the output into 76-character lines
    *
    * @return the encoded data as a char array
    */
    public static char[] encode(byte[] ab, int ofb, int cb, boolean fBreakLines)
        {
        final char[] alpha = BASE64_ALPHABET;

        // examine the input
        int cGroups  = cb / 3;          // the number of full 24-bit groups
        int cbRemain = (cb * 4) % 3;    // the number of leftover bytes

        // calculate the size of the result assuming:
        //  (1) A 24-bit input group is encoded into 4 base64-alphabet
        //      characters
        //  (2) each output line is composed of groups of 4 base64-alphabet
        //      characters
        //  (3) each output line except the last has 19 groups of 4
        //      base64-alphabet characterss (length=76)
        //  (4) each output line except the last is terminated with a new
        //      line character (length=76+1=77)
        //  (5) the last output line, even if full (19 groups), is not
        //      terminated with a new line character
        int cchRaw   = (cb + 2) / 3 * 4;
        int cLines   = fBreakLines ? (cchRaw - 1) / 76 : 0;
        int cch      = cchRaw + cLines;

        // allocate result
        char[] ach = new char[cch];

        int ofch = 0;                   // offset into output

        if (cGroups > 0)
            {
            int cLineGroups = fBreakLines ? GROUPS_PER_LINE : -1;
            while (true)
                {
                // process next three bytes
                int n = (ab[ofb++] & 0xFF) << 16 | (ab[ofb++] & 0xFF) << 8 | (ab[ofb++] & 0xFF);

                ach[ofch++] = alpha[n >> 18       ]; // 1111 1100 0000 0000 0000 0000
                ach[ofch++] = alpha[n >> 12 & 0x3F]; // 0000 0011 1111 0000 0000 0000
                ach[ofch++] = alpha[n >> 6  & 0x3F]; // 0000 0000 0000 1111 1100 0000
                ach[ofch++] = alpha[n       & 0x3F]; // 0000 0000 0000 0000 0011 1111

                // check for end of input
                if (--cGroups == 0)
                    {
                    break;
                    }

                // check for new line
                if (--cLineGroups == 0)
                    {
                    ach[ofch++] = BASE64_LF;
                    cLineGroups = GROUPS_PER_LINE;
                    }
                }
            }

        switch (cbRemain)
            {
            default:
            case 0:
                break;

            case 1:
                {
                int n = (ab[ofb] & 0xFF);
                ach[ofch++] = alpha[n >> 2       ]; // 1111 1100
                ach[ofch++] = alpha[n << 4 & 0x3F]; // 0000 0011 1111
                ach[ofch++] = BASE64_PAD;
                ach[ofch++] = BASE64_PAD;
                }
                break;

            case 2:
                {
                int n = (ab[ofb++] & 0xFF) << 8 | (ab[ofb] & 0xFF);
                ach[ofch++] = alpha[n >> 10       ]; // 1111 1100 0000 0000
                ach[ofch++] = alpha[n >>  4 & 0x3F]; // 0000 0011 1111 0000
                ach[ofch++] = alpha[n <<  2 & 0x3F]; // 0000 0000 0000 1111 11
                ach[ofch++] = BASE64_PAD;
                }
                break;
            }

        Base.azzert(ofch == cch);
        return ach;
        }


    // ----- constants ------------------------------------------------------

    /**
    * Base64 encodes into this "alphabet" of 64 characters.
    */
    protected static final char[] BASE64_ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
            .toCharArray();

    /**
    * The Base64 padding character.  Base64 is encoded into 4-character
    * chunks; if the last chunk does not contain 4 characters, it is
    * filled with this padding character.
    */
    protected static final char   BASE64_PAD      = '=';

    /**
    * The Base64 line feed character.  Base64 is encoded into 76-character
    * lines unless .
    */
    protected static final char   BASE64_LF       = '\n';

    /**
    * The number of Base64 character groups in one line.  This number
    * prevents a line from exceeding 76 characters.
    */
    protected static final int    GROUPS_PER_LINE = 19;


    // ----- data members ---------------------------------------------------

    /**
    * True after close is invoked.
    */
    protected boolean m_fClosed;

    /**
    * The Writer object to which the Base64 encoded data is written.
    */
    protected Writer m_writer;

    /**
    * True if lines are to be broken by BASE64_LF;
    */
    protected boolean m_fBreakLines;

    /**
    * The number of groups left to write in the current line.
    */
    protected int m_cLineGroups = GROUPS_PER_LINE;

    /**
    * Accumulated bytes.
    */
    protected byte[] m_abAccum  = new byte[3];

    /**
    * The number of bytes accumulated (0, 1, 2 or 3).
    */
    protected int m_cAccum;

    /**
    * An array that is used to send 4 characters at a time to the underlying
    * Writer object.
    */
    protected char[] m_achGroup = new char[4];
    }
