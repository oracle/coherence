/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io;


import java.io.EOFException;
import java.io.InputStream;
import java.io.IOException;
import java.io.Reader;


/**
* Reads binary data from a Reader using IETF RFC 2045 Base64 Content
* Transfer Encoding.
*
* Static helpers are available for decoding directly from a char array
* to a byte array.
*
* @author cp  2000.09.07
*/
public class Base64InputStream
        extends InputStream
        implements InputStreaming
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a Base64InputStream on a Reader object.
    *
    * @param reader  the Reader to read the Base64 encoded data from
    */
    public Base64InputStream(Reader reader)
        {
        m_reader = reader;
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
        if (m_fClosed)
            {
            throw new IOException("Base64InputStream is closed");
            }

        // check "available"
        int[] ab  = m_abGroup;
        int   cb  = ab.length;
        int   ofb = m_ofbGroup;

        if (ofb < cb)
            {
            m_ofbGroup = ofb + 1;
            return ab[ofb];
            }

        // is eof known?
        if (m_fEOF)
            {
            return -1;
            }

        // read next chunk from the reader
        Reader reader = m_reader;
        int    nGroup = 0;          // the 24-bit group value
        int    cch    = 0;          // number of base64 chars read
        while (cch < 4)
            {
            int nch    = reader.read();
            int nHexit = -1;
            switch (nch)
                {
                case -1:
                    // if partial input then there is stream corruption
                    m_fEOF = true;
                    if (cch > 0)
                        {
                        throw new EOFException();
                        }
                    return -1;

                case 'A': case 'B': case 'C': case 'D': case 'E':
                case 'F': case 'G': case 'H': case 'I': case 'J':
                case 'K': case 'L': case 'M': case 'N': case 'O':
                case 'P': case 'Q': case 'R': case 'S': case 'T':
                case 'U': case 'V': case 'W': case 'X': case 'Y':
                case 'Z':
                    nHexit = nch - 'A';
                    break;

                case 'a': case 'b': case 'c': case 'd': case 'e':
                case 'f': case 'g': case 'h': case 'i': case 'j':
                case 'k': case 'l': case 'm': case 'n': case 'o':
                case 'p': case 'q': case 'r': case 's': case 't':
                case 'u': case 'v': case 'w': case 'x': case 'y':
                case 'z':
                    nHexit = 26 + nch - 'a';
                    break;

                case '0': case '1': case '2': case '3': case '4':
                case '5': case '6': case '7': case '8': case '9':
                    nHexit = 52 + nch - '0';
                    break;

                case '+':
                    nHexit = 62;
                    break;

                case '/':
                    nHexit = 63;
                    break;

                case '=':
                    m_fEOF = true;
                    switch (cch)
                        {
                        case 0:
                        case 1:
                            throw new IOException("illegal base64 pad:  "
                                    + "at offset " + cch + " in a 4-char group");
                        case 2:
                            // one more pad character is expected
                            readpad: while (true)
                                {
                                // must read one more '='
                                switch (reader.read())
                                    {
                                    case -1:    // eof (not right but who cares)
                                    case '=':   // expected pad
                                        break readpad;
                                    case ' ':
                                    case '\r':
                                    case '\n':
                                    case '\t':
                                    case '\f':
                                        // ignore white space
                                        break;
                                    default:
                                        throw new IOException("missing final base64 pad");
                                    }
                                }
                            break;
                        }
                    break;

                case ' ':
                case '\r':
                case '\n':
                case '\t':
                case '\f':
                    // ignore white space
                    break;

                default:
                    m_fEOF = true;
                    throw new IOException("illegal base64 encoding character: "
                            + (char) nch);
                }

            if (nHexit >= 0)
                {
                nGroup |= nHexit << (6 * (4 - ++cch));
                }

            if (m_fEOF)
                {
                break;
                }
            }

        // chop the 24-bit nGroup into bytes
        int ofbGroup = 4 - cch;
        for (int ofbCur = ofbGroup, cBits = 16; ofbCur < 3; ++ofbCur, cBits -= 8)
            {
            ab[ofbCur] = nGroup >> cBits & 0xFF;
            }

        // the result is that either something is available or eof
        m_ofbGroup = ofbGroup;
        return read();
        }

    /**
    * Returns the number of bytes that can be read (or skipped over) from
    * this input stream without blocking by the next caller of a method for
    * this input stream.  The next caller might be the same thread or or
    * another thread.
    *
    * @return     the number of bytes that can be read from this input stream
    *             without blocking.
    * @exception  IOException  if an I/O error occurs.
    */
    public int available() throws IOException
        {
        return m_fClosed ? 0 : m_abGroup.length - m_ofbGroup;
        }

    /**
    * Close the stream, flushing any accumulated bytes.  The underlying
    * reader is not closed.
    *
    * @exception  IOException  if an I/O error occurs.
    */
    public void close() throws IOException
        {
        m_fClosed = true;
        }


    // ----- static helpers -------------------------------------------------

    /**
    * Decode the passed character data that was encoded using Base64 encoding.
    *
    * @param ach  the array containing the characters to decode
    *
    * @return  the decoded binary data as a byte array
    */
    public static byte[] decode(char[] ach)
        {
        return decode(ach, true);
        }

    /**
    * Decode the passed character data that was encoded using Base64 encoding.
    *
    * @param ach    the array containing the characters to decode
    * @param fJunk  true if the char array may contain whitespace or
    *               linefeeds
    *
    * @return  the decoded binary data as a byte array
    */
    public static byte[] decode(char[] ach, boolean fJunk)
        {
        return decode(ach, 0, ach.length, fJunk);
        }

    /**
    * Decode the passed character data that was encoded using Base64 encoding.
    *
    * @param ach  the array containing the characters to decode
    * @param of   the start offset in the char array
    * @param cch  the number of characters to decode
    *
    * @return  the decoded binary data as a byte array
    */
    public static byte[] decode(char[] ach, int of, int cch)
        {
        return decode(ach, of, cch, true);
        }

    /**
    * Decode the passed character data that was encoded using Base64 encoding.
    *
    * @param ach    the array containing the characters to decode
    * @param of     the start offset in the char array
    * @param cch    the number of characters to decode
    * @param fJunk  true if the char array may contain whitespace or
    *               linefeeds
    *
    * @return  the decoded binary data as a byte array
    */
    public static byte[] decode(char[] ach, int of, int cch, boolean fJunk)
        {
        if (fJunk)
            {
            // scan for any non-base64-alphabet characters
            char[] achNew = null;
            int    ofNew  = 0;
            int    ofPrev = of;
            int    ofEnd  = of + cch;
            for (int ofCur = of; ofCur < ofEnd; ++ofCur)
                {
                switch (ach[ofCur])
                    {
                    case 'A': case 'B': case 'C': case 'D': case 'E':
                    case 'F': case 'G': case 'H': case 'I': case 'J':
                    case 'K': case 'L': case 'M': case 'N': case 'O':
                    case 'P': case 'Q': case 'R': case 'S': case 'T':
                    case 'U': case 'V': case 'W': case 'X': case 'Y':
                    case 'Z':

                    case 'a': case 'b': case 'c': case 'd': case 'e':
                    case 'f': case 'g': case 'h': case 'i': case 'j':
                    case 'k': case 'l': case 'm': case 'n': case 'o':
                    case 'p': case 'q': case 'r': case 's': case 't':
                    case 'u': case 'v': case 'w': case 'x': case 'y':
                    case 'z':

                    case '0': case '1': case '2': case '3': case '4':
                    case '5': case '6': case '7': case '8': case '9':

                    case '+': case '/':

                    case '=':
                        break;

                    case ' ':
                    case '\r':
                    case '\n':
                    case '\t':
                    case '\f':
                        {
                        if (ofPrev == ofCur)
                            {
                            // ofPrev is the offset of the first valid char
                            // in the current chunk; the current char is not
                            // valid so advance ofPrev
                            ++ofPrev;
                            }
                        else
                            {
                            // lazy instantiate "clean" char array
                            if (achNew == null)
                                {
                                achNew = new char[cch];
                                }

                            // copy up to the current point
                            if (ofCur > ofPrev)
                                {
                                int cchCopy = ofCur - ofPrev;
                                System.arraycopy(ach, ofPrev, achNew, ofNew, cchCopy);
                                ofNew += cchCopy;
                                }

                            // next valid character is beyond the current pos
                            ofPrev = ofCur + 1;
                            }
                        }
                        break;

                    default:
                        throw new IllegalArgumentException(
                                "illegal base64 encoding character: " + ach[ofCur]);
                    }
                }

            if (achNew != null)
                {
                if (ofPrev < ofEnd)
                    {
                    int cchCopy = ofEnd - ofPrev;
                    System.arraycopy(ach, ofPrev, achNew, ofNew, cchCopy);
                    ofNew += cchCopy;
                    }

                ach = achNew;
                of  = 0;
                cch = ofNew;
                }
            }

        // special case:  no data
        if (cch == 0)
            {
            return EMPTY;
            }

        // determine the 4-char-groups but not the last group
        // (whether or not it has 4 chars)
        int cGroups = cch / 4;
        int cchRem  = cch % 4;
        if (cchRem == 0)
            {
            // make the last group the remainder (to scan for '=')
            --cGroups;
            cchRem = 4;
            }

        // process the last group to determine the length of the
        // resulting binary
        int     ofRem    = of + cch - cchRem;   // offset of last group
        int     nGroup   = 0;                   // value of last group
        int     cchGroup = 0;                   // number of alphas in it
        boolean fDone    = false;               // true once pad found
        for (int i = 0; i < cchRem; ++i)
            {
            char ch = ach[ofRem + i];
            if (ch == '=')
                {
                if (i < 2)
                    {
                    throw new IllegalArgumentException(
                        "illegal base64 ending pad:  "
                        + "pad can not start before the third base64 alpha "
                        + "of the final group");
                    }

                fDone = true;
                }
            else if (fDone)
                {
                // no alphas can follow a base64 pad
                throw new IllegalArgumentException(
                        "illegal base64 ending pad:  "
                        + "pad can not be followed by a legit base64 alpha");
                }
            else
                {
                // decode the base64 alpha and pack it
                nGroup |= decode(ch) << (6 * (4 - ++cchGroup));
                }
            }
        if (cchGroup < 2)
            {
            throw new IllegalArgumentException(
                    "illegal base64 ending group:  "
                    + "group must contain at least two base64 alpha chars");
            }

        // determine the size of the resulting binary and allocate it
        int    cb = cGroups * 3 + cchGroup - 1;
        byte[] ab = new byte[cb];

        // chop the 24-bit nGroup into bytes
        for (int i = 0, c = cchGroup - 1, ofb = cb - c, cBits = 16;
             i < c; ++i, ++ofb, cBits -= 8)
            {
            ab[ofb] = (byte) (nGroup >> cBits & 0xFF);
            }

        // process groups 0..n-1
        for (int iGroup = 0, ofb = 0; iGroup < cGroups; ++iGroup)
            {
            nGroup = decode(ach[of++]) << 18
                   | decode(ach[of++]) << 12
                   | decode(ach[of++]) << 6
                   | decode(ach[of++]);

            ab[ofb++] = (byte) (nGroup >> 16      );
            ab[ofb++] = (byte) (nGroup >> 8 & 0xFF);
            ab[ofb++] = (byte) (nGroup      & 0xFF);
            }

        return ab;
        }

    /**
    * Decode one base64 alphabet character.
    *
    * @param ch  the character
    *
    * @return the ordinal value of the character
    */
    public static int decode(char ch)
        {
        switch (ch)
            {
            case 'A': case 'B': case 'C': case 'D': case 'E':
            case 'F': case 'G': case 'H': case 'I': case 'J':
            case 'K': case 'L': case 'M': case 'N': case 'O':
            case 'P': case 'Q': case 'R': case 'S': case 'T':
            case 'U': case 'V': case 'W': case 'X': case 'Y':
            case 'Z':
                return ch - 'A';

            case 'a': case 'b': case 'c': case 'd': case 'e':
            case 'f': case 'g': case 'h': case 'i': case 'j':
            case 'k': case 'l': case 'm': case 'n': case 'o':
            case 'p': case 'q': case 'r': case 's': case 't':
            case 'u': case 'v': case 'w': case 'x': case 'y':
            case 'z':
                return 26 + ch - 'a';

            case '0': case '1': case '2': case '3': case '4':
            case '5': case '6': case '7': case '8': case '9':
                return 52 + ch - '0';

            case '+':
                return 62;

            case '/':
                return 63;

            default:
                throw new IllegalArgumentException(
                        "illegal base64 encoding character: " + ch);
            }
        }


    // ----- constants ------------------------------------------------------

    /**
    * Empty binary data.
    */
    protected static final byte[] EMPTY = new byte[0];


    // ----- data members ---------------------------------------------------

    /**
    * True after close is invoked.
    */
    protected boolean m_fClosed;

    /**
    * True after eof is determined.
    */
    protected boolean m_fEOF;

    /**
    * The Reader object from which the Base64 encoded data is read.
    */
    protected Reader m_reader;

    /**
    * Group of bytes (stored as ints 0..255).
    */
    protected int[] m_abGroup = new int[3];

    /**
    * The offset in the group of bytes.
    */
    protected int m_ofbGroup = m_abGroup.length;
    }
