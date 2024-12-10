/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.compiler.java;


import java.io.EOFException;

import com.tangosol.dev.compiler.Script;
import com.tangosol.dev.compiler.SimpleScript;
import com.tangosol.dev.compiler.ParsePosition;

import com.tangosol.util.Base;


/**
* Provides a character stream based on a Java script.  This class solves
* several important problems, including tracking line number and offset,
* allowing for put-back, save position (mark), and restore position (reset)
* functionality, and handling of unicode escapes (slash-u-xxxx).
*
* @see com.tangosol.dev.compiler.SimpleScript
*
* @version 1.00, 12/05/96
* @author Cameron Purdy
*/
public class UnicodeScript
        extends Base
        implements Script
    {
    // ----- constructors  --------------------------------------------------

    /**
    * (Default) Constructs a UnicodeScript.
    */
    public UnicodeScript()
        {
        }

    /**
    * Constructs a UnicodeScript out of the passed string.
    *
    * @param s      the script
    * @param iLine  the beginning line number
    */
    public UnicodeScript(String s, int iLine)
        {
        setScript(s);
        m_iLine = m_iLineInitial = iLine;
        }

    /**
    * Allows a UnicodeScript to be constructed out of part of an existing
    * UnicodeScript.
    *
    * @param that  the existing UnicodeScript object
    * @param of    the offset within the existing script to extract
    * @param cch   the number of characters to extract
    */
    protected UnicodeScript(UnicodeScript that, int of, int cch)
        {
        // copy the script
        m_achScript = new char [cch];
        System.arraycopy(that.m_achScript, of, this.m_achScript, 0, cch);
        m_cchScript = cch;

        int cchSkewStart = 0;
        int cchSkewEnd   = 0;

        Skew skewNext = that.getFirstSkew();
        if (skewNext != null)
            {
            // find the first skew in the other script that applies to this script
            while (skewNext != null && skewNext.ofInScript < of)
                {
                cchSkewStart += skewNext.cchSkew;
                skewNext = skewNext.skewNext;
                }

            // copy the skews that apply to this script
            int ofEnd = of + cch - 1;
            while (skewNext != null && skewNext.ofInScript <= ofEnd)
                {
                // create a skew (relative position within this script)
                Skew skew = new Skew();
                skew.ofInScript = skewNext.ofInScript - of;
                skew.cchSkew    = skewNext.cchSkew;

                // link to end of this script's skew list
                skew.skewPrev = m_skewPrev;
                if (m_skewPrev != null)
                    {
                    m_skewPrev.skewNext = skew;
                    }
                m_skewPrev = skew;

                cchSkewEnd += skewNext.cchSkew;
                skewNext = skewNext.skewNext;
                }

            m_skewNext = getFirstSkew();
            m_skewPrev = null;
            }

        // copy the relevant portion of the original string
        of  += cchSkewStart;
        cch += cchSkewEnd;
        m_sScript = that.m_sScript.substring(of, of + cch);

        m_fInit = true;
        }


    // ----- Script interface -----------------------------------------------

    /**
    * Initializes the script object.  This method must be called exactly
    * one time to initialize the script object.
    *
    * @param sScript the string containing the script
    */
    public void setScript(String sScript)
        {
        // assert:  this method must be called only one time
        if (m_achScript != null)
            {
            throw new IllegalStateException();
            }

        m_sScript   = sScript;
        m_achScript = sScript.toCharArray();
        m_cchScript = m_achScript.length;
        }

    /**
    * Checks for more characters in the script.
    *
    * @return true if there are more characters in the script.
    */
    public boolean hasMoreChars()
        {
        return (m_ofCurrent < m_cchScript);
        }

    /**
    * Eats and returns the next character from the script.
    *
    * @return the next character of the script
    *
    * @exception EOFException If the stream is exhausted or if the SUB
    *            (end of file) character is encountered before the end
    *            of the script is reached
    * @exception UnicodeDataFormatException  If an invalid unicode escape
    *            sequence is encountered
    */
    public char nextChar() throws EOFException, UnicodeDataFormatException
        {
        if (m_ofCurrent >= m_cchScript)
            {
            throw new EOFException();
            }

        if (!m_fInit)
            {
            initialize();
            }

        char    ch    = m_achScript [m_ofCurrent];
        boolean fMore = (m_ofCurrent + 1 < m_cchScript);

        if (ch == SUB && fMore)
            {
            // Sub (3.5) - can only appear as the last character
            throw new EOFException();
            }
        else if (m_skewNext != null && m_skewNext.ofInScript == m_ofCurrent)
            {
            // apply skew
            m_ofInLine += m_skewNext.cchSkew + 1;

            // advance to next skew
            m_skewPrev = m_skewNext;
            m_skewNext = m_skewNext.skewNext;
            }
        else if (ch == '\n' || ch == '\r')
            {
            // if the current character is a carriage return, it increments
            // the line counter unless the following character is an unskewed
            // new line character
            if (ch == '\r' && fMore && m_achScript [m_ofCurrent + 1] == '\n'
                    && (m_skewNext == null || m_skewNext.ofInScript > m_ofCurrent + 1))
                {
                // this carriage return does not increment the line number
                // since it is followed by an unskewed new line character
                ++m_ofInLine;
                }
            else
                {
                // advance to the next line
                m_ofInLine  = 0;
                ++m_iLine;
                }
            }
        else
            {
            // advance to the next character in this line
            ++m_ofInLine;
            }

        ++m_ofCurrent;
        return ch;
        }

    /**
    * Regurgitates the last eaten character so that the next call to
    * nextChar will return the same character that was returned by the
    * most recent call to nextChar.  (This method can be called more
    * than once to regurgitate multiple characters.)
    */
    public void putBackChar()
        {
        char ch = m_achScript [--m_ofCurrent];

        if (m_skewPrev != null && m_skewPrev.ofInScript == m_ofCurrent)
            {
            // apply skew
            m_ofInLine -= (m_skewPrev.cchSkew + 1);

            // retreat to previous skew
            m_skewNext = m_skewPrev;
            m_skewPrev = m_skewPrev.skewPrev;
            }
        else if (ch == '\n' || ch == '\r')
            {
            // if the regurgitated character is a carriage return, it decrements
            // the line counter unless the following character is an unskewed
            // new line character
            if (ch == '\r' && m_achScript [m_ofCurrent + 1] == '\n'
                    && (m_skewNext == null || m_skewNext.ofInScript > m_ofCurrent + 1))
                {
                // regurgitating this carriage return does not decrement the
                // line number since the carriage return is followed by an
                // unskewed new line character
                --m_ofInLine;
                }
            else
                {
                // back up to the previous line
                --m_iLine;

                // determine the offset of the end of the previous line by
                // backing up to the beginning of the line, whether that is
                // an unskewed cr or lf, or whether it is the beginning of
                // the script.  note that skews are counted.
                m_ofInLine = 0;
                int  of = m_ofCurrent - 1;
                Skew skewPrev = m_skewPrev;
                while (of >= 0)
                    {
                    if (skewPrev != null && skewPrev.ofInScript == of)
                        {
                        m_ofInLine += skewPrev.cchSkew + 1;
                        skewPrev = skewPrev.skewPrev;
                        }
                    else if (m_achScript[of] == '\r' || m_achScript[of] == '\n')
                        {
                        break;
                        }
                    else
                        {
                        ++m_ofInLine;
                        }
                    --of;
                    }
                }
            }
        else
            {
            --m_ofInLine;
            }
        }

    /**
    * Returns a Position object that can be used to restore the
    * current position in the script.
    *
    * @return a Position object which identifies the current position
    *         within the script
    *
    * @see #restorePosition(ParsePosition)
    */
    public ParsePosition savePosition()
        {
        Position pos = new Position();

        pos.script    = this;
        pos.fInit     = m_fInit;
        pos.ofCurrent = m_ofCurrent;
        pos.iLine     = m_iLine;
        pos.ofInLine  = m_ofInLine;
        pos.skewNext  = m_skewNext;
        pos.skewPrev  = m_skewPrev;

        return pos;
        }

    /**
    * Restores the current parsing position that was returned from
    * the savePosition method.
    *
    * @param parsepos  The return value from a previous call to savePosition
    *
    * @see #savePosition
    */
    public void restorePosition(ParsePosition parsepos)
        {
        Position pos = (Position) parsepos;

        if (pos.script != this)
            {
            throw new IllegalArgumentException("Unknown ParsePosition object");
            }
        else if (m_fInit && pos.fInit)
            {
            m_ofCurrent = pos.ofCurrent;
            m_iLine     = pos.iLine;
            m_ofInLine  = pos.ofInLine;
            m_skewNext  = pos.skewNext;
            m_skewPrev  = pos.skewPrev;
            }
        else if (m_fInit && !pos.fInit)
            {
            // reset script position to beginning
            m_ofCurrent = 0;
            m_iLine     = m_iLineInitial;
            m_ofInLine  = 0;

            // back up to the first skew
            m_skewNext = getFirstSkew();
            m_skewPrev = null;
            }
        }

    /**
    * Determines the current line.  The current line is the line from which
    * the next character returned from nextChar will come.  The first line
    * of the script is line 0.
    *
    * @return the line number of the next character
    *
    * @see #getOffset
    */
    public int getLine()
        {
        return m_iLine;
        }

    /**
    * Determines the current offset.  The current offset is the offset
    * within the current line of the next character that will be returned
    * from nextChar.  The offset is 0-based, so the first character in a
    * line is at offset 0.
    *
    * @return the offset of the next character in the current line
    *
    * @see #getLine
    */
    public int getOffset()
        {
        return m_ofInLine;
        }

    /**
    * Creates a Script object which contains a sub-section of this script.
    *
    * @param  parsepos  Specifies the position of the first character of the
    *                   sub-section of this script to extract.
    *
    * @return a script object which contains that portion of this
    *         script starting with the character specified by the
    *         passed ParsePosition object and ending with the character
    *         immediately preceding the current position in the script.
    *
    * @see com.tangosol.dev.compiler.Script#savePosition
    */
    public Script subScript(ParsePosition parsepos)
        {
        Position pos = (Position) parsepos;

        if (pos.script != this)
            {
            throw new IllegalArgumentException("Unknown ParsePosition object");
            }

        if (!m_fInit)
            {
            return new SimpleScript("");
            }

        int of  = pos.ofCurrent;
        int cch = m_ofCurrent - of;
        if (cch < 0)
            {
            throw new IllegalStateException("The starting offset is greater than the current offset");
            }

        return new UnicodeScript(this, of, cch);
        }

    /**
    * Returns the script as a string.  Note that the returned script may not
    * have the exact contents of the string or stream (or other data) that
    * was used to construct the Script object due to mechanisms such as
    * unicode escape processing.
    *
    * @return the script as a String value that may already have had some
    *         processing (such as de-escaping) performed on it
    *
    * @exception UnicodeDataFormatException If the script contains invalid
    *            data which prevents it from being converted to a String.
    */
    public String getText() throws UnicodeDataFormatException
        {
        initialize();
        return new String(m_achScript, 0, m_cchScript);
        }

    /**
    * Returns the script as a string.  The returned string is required to
    * reflect the exact contents of the string or stream (or other data)
    * that was used to construct the Script object.
    *
    * @return the script as a String value that reflects the exact data that
    *         the script was constructed from
    */
    public String toString()
        {
        return m_sScript;
        }


    // ----- protected methods ----------------------------------------------

    /**
    * Initialize the state of the script object.
    */
    protected void initialize() throws UnicodeDataFormatException
        {
        if (!m_fInit)
            {
            if (m_fUnicodeError)
                {
                throw new UnicodeDataFormatException();
                }

            try
                {
                processUnicodeEscapes();
                }
            catch (UnicodeDataFormatException e)
                {
                m_fUnicodeError = true;
                throw e;
                }

            m_fInit = true;
            }
        }

    /**
    * Find the head of the skew list.
    *
    * @return the first skew object
    */
    protected Skew getFirstSkew()
        {
        Skew skew = (m_skewPrev == null ? m_skewNext : m_skewPrev);
        if (skew != null)
            {
            while (skew.skewPrev != null)
                {
                skew = skew.skewPrev;
                }
            }

        return skew;
        }


    // ----- internal methods -----------------------------------------------

    /**
    * Converts unicode escape sequences to the characters that they represent.
    *
    * @exception UnicodeDataFormatException If the script tokenizer
    *            encounters an invalid unicode escape sequence, it sets the
    *            line number and offset of the invalid sequence and throws
    *            this exception
    */
    private void processUnicodeEscapes() throws UnicodeDataFormatException
        {
        int     cchRaw    = m_cchScript;
        int     ofRaw     = 0;
        int     ofDest    = 0;

        boolean fEsc      = false;
        boolean fSkewed   = false;

        Skew    skewFirst = null;
        Skew    skewLast  = null;

        while (ofRaw < cchRaw)
            {
            char ch = m_achScript [ofRaw];

            if (fEsc)
                {
                if (ch == 'u')
                    {
                    // the offset of the unicode escape is one character
                    // back (the slash)
                    int ofEscape = m_ofInLine - 1;

                    // keep track of how large the skew is; the unicode
                    // escape sequence slash-u-xxxx is a 5-character skew,
                    // since the six-character sequence is replaced with
                    // one character; the skew size is 4 (the 4-digit hex
                    // portion) plus the number of unicode markers ('u')
                    int cchSkew = 4;

                    // repeating unicode markers ('u') are permitted
                    while (ofRaw < cchRaw && m_achScript[ofRaw] == 'u')
                        {
                        ++ofRaw;
                        ++cchSkew;
                        }

                    // verify that there is sufficient script left (4 chars)
                    // to extract a unicode escape sequence from
                    if (ofRaw + 4 > cchRaw)
                        {
                        m_ofInLine = ofEscape;
                        throw new UnicodeDataFormatException();
                        }

                    // convert the 4-digit hex number into a unicode character
                    int nCharValue = 0;
                    for (int i = 0; i < 4; ++i)
                        {
                        ch = m_achScript[ofRaw++];
                        if (!isHex(ch))
                            {
                            m_ofInLine = ofEscape;
                            throw new UnicodeDataFormatException();
                            }
                        nCharValue = (nCharValue << 4) + hexValue(ch);
                        }

                    // replace the '\' with the unicode escaped character
                    m_achScript[ofDest-1] = (char) nCharValue;

                    // store the skew information
                    Skew skew = new Skew();
                    skew.ofInScript = ofDest-1;
                    skew.cchSkew    = cchSkew;

                    // append the skew to the linked list of skew information
                    if (skewFirst == null)
                        {
                        skewFirst = skew;
                        }
                    if (skewLast != null)
                        {
                        skewLast.skewNext = skew;
                        }
                    skew.skewPrev = skewLast;
                    skewLast = skew;

                    fSkewed = true;
                    fEsc    = false;

                    m_ofInLine += (cchSkew + 1);
                    continue;
                    }

                fEsc = false;
                }
            else if (ch == '\\')
                {
                fEsc = true;
                }

            // keep track of line number and offset in case an invalid
            // unicode escape sequence is encountered
            if (ch == '\n' || (ch == '\r' && (ofRaw + 1 >= cchRaw || m_achScript [ofRaw + 1] != '\n')))
                {
                m_ofInLine = 0;
                ++m_iLine;
                }
            else
                {
                ++m_ofInLine;
                }

            // copy only if necessary (ie. only if the dest is skewed from the
            // source due to previous unicode escapes)
            if (fSkewed)
                {
                m_achScript[ofDest] = ch;
                }

            ++ofDest;
            ++ofRaw;
            }

        // adjust the length of the script if it shrank due to processing
        // unicode escapes
        if (fSkewed)
            {
            m_cchScript -= (ofRaw - ofDest);
            }

        // store the linked list of skews
        m_skewNext = skewFirst;

        // reset the line number and offset to the beginning of the code
        m_iLine    = m_iLineInitial;
        m_ofInLine = 0;
        }


    /**
    * Stores all information required to later restore the current position in
    * the script.
    *
    * @see com.tangosol.dev.compiler.java.UnicodeScript
    */
    class Position implements ParsePosition
        {
        Script  script;
        boolean fInit;
        int     ofCurrent;
        int     iLine;
        int     ofInLine;
        Skew    skewNext;
        Skew    skewPrev;
        }


    /**
    * Stores information about script skewing caused by processing unicode
    * escapes.  Used only to build internal data structures for the UnicodeScript
    * class.
    *
    * @author Cameron Purdy
    *
    * @see com.tangosol.dev.compiler.java.UnicodeScript
    */
    class Skew
        {
        int   ofInScript;
        int   cchSkew;
        Skew  skewPrev;
        Skew  skewNext;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The string which created this script.
    */
    protected String m_sScript;

    /**
    * The array of characters that make up the script.
    */
    protected char  m_achScript[];

    /**
    * The length of the script in characters.
    */
    protected int   m_cchScript;

    /**
    * The current character offset within the script.
    */
    protected int   m_ofCurrent;

    /**
    * The line number of the first character of the script. Used for the
    * class-based rather than method-based line number generation.
    */
    protected int m_iLineInitial;

    /**
    * The current line number within the script.
    * <p>
    * The current line number accounts for complexities caused by unicode
    * escapes.  For example, the unicode escape slash-u-000A is the new
    * line character ('\n'). In this case, the new line character should
    * not increment the line counter, since it is not an actual new line
    * character in the source.
    */
    protected int   m_iLine;

    /**
    * The current character offset with the current line.
    * <p>
    * The current offset accounts for complexities caused by unicode escape
    * sequences, like the difference in length between the escape sequence
    * and the character it represents.
    */
    protected int   m_ofInLine;

    /**
    * The next skew that will be encountered.
    * <p>
    * A doubly-linked list of skew locations is built by the method
    * processUnicodeEscapes so that correct line numbers and offsets
    * can be generated, accounting for the possibility of new line
    * characters being escaped and accounting for the difference in
    * length between an escape sequence and the character it represents.
    */
    protected Skew m_skewNext;

    /**
    * The most recent skew encountered.  This value is null if no skews
    * have been encountered.  This value is used to correctly track the
    * line number and offset when backing up.
    */
    protected Skew m_skewPrev;

    /**
    * Set to true when the script object is initialized.
    */
    private boolean m_fInit;

    /**
    * Signifies that processing unicode escapes already failed.
    */
    private boolean m_fUnicodeError;
    }
