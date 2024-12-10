/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.compiler;


import java.io.EOFException;


/**
* Implements a simple unicode script object, but does not handle unicode
* escapes.
*
* @see com.tangosol.dev.compiler.java.UnicodeScript
*
* @version 1.00, 08/25/97
* @author Cameron Purdy
*/
public class SimpleScript
        implements Script
    {
    // ----- constructors  --------------------------------------------------

    /**
    * (Default) Constructs a SimpleScript.
    */
    public SimpleScript()
        {
        }

    /**
    * Constructs a SimpleScript out of the passed string.
    *
    * @param sScript  the script
    */
    public SimpleScript(String sScript)
        {
        setScript(sScript);
        }

    /**
    * Constructs a SimpleScript out of the passed char array.
    *
    * @param achScript  the character array
    * @param ofStart    the index of the first character
    * @param cLen       the number of characters
    */
    protected SimpleScript(char[] achScript, int ofStart, int cLen)
        {
        char[] ach = new char[cLen];
        System.arraycopy(achScript, ofStart, ach, 0, cLen);
        m_achScript = ach;
        }


    // ----- Script interface -----------------------------------------------

    /**
    * Initializes the SimpleScript using the passed string.
    *
    * @param sScript  the script
    */
    public void setScript(String sScript)
        {
        // assert:  this method must be called only one time
        if (m_achScript != null)
            {
            throw new IllegalStateException();
            }

        m_achScript = sScript.toCharArray();
        }

    /**
    * Checks for more characters in the script.
    *
    * @return true if there are more characters in the script.
    */
    public boolean hasMoreChars()
        {
        return (m_ofCurrent < m_achScript.length);
        }

    /**
    * Eats and returns the next character from the script.
    *
    * @return the next character of the script
    *
    * @exception EOFException If the stream is exhausted or if the SUB
    *            (end of file) character is encountered before the end
    *            of the script is reached
    */
    public char nextChar() throws EOFException
        {
        char[] achScript = m_achScript;
        int    cchScript = achScript.length;
        char   ch;
        try
            {
            ch = achScript[m_ofCurrent++];
            }
        catch (ArrayIndexOutOfBoundsException e)
            {
            throw new EOFException();
            }

        if (ch == '\n' || ch == '\r')
            {
            boolean bMore = (m_ofCurrent < cchScript);

            // if the current character is a carriage return, it increments
            // the line counter
            if (ch == '\r' && bMore && achScript[m_ofCurrent] == '\n')
                {
                // this carriage return does not increment the line number
                // since it is followed by a new line character
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
            if (ch == SUB && m_ofCurrent < cchScript)
                {
                // Sub (3.5) - can only appear as the last character
                --m_ofCurrent;
                throw new EOFException();
                }

            // advance the offset in the current line
            ++m_ofInLine;
            }

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
        if (m_ofCurrent <= 0)
            {
            throw new IllegalStateException();
            }

        char ch = m_achScript[--m_ofCurrent];
        if (ch == '\n' || ch == '\r')
            {
            // if the regurgiated character is a carriage return, it decrements
            // the line counter unless a line feed immediately follows it
            if (ch == '\r' && m_achScript[m_ofCurrent+1] == '\n')
                {
                // this carriage return did not increment the line number
                // since it is followed by a new line character
                --m_ofInLine;
                }
            else
                {
                // retreat to the end of the previous line
                --m_iLine;
                m_ofInLine = 0;
                int of = m_ofCurrent - 1;
                while (of >= 0 && !(m_achScript[of] == '\r' || m_achScript[of] == '\n'))
                    {
                    ++m_ofInLine;
                    --of;
                    }
                }
            }
        else
            {
            // retreat the offset in the current line
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
        pos.ofCurrent = m_ofCurrent;
        pos.iLine     = m_iLine;
        pos.ofInLine  = m_ofInLine;

        return pos;
        }

    /**
    * Restores the current parsing position that was returned from
    * the savePosition method.
    *
    * @param parsepos The return value from a previous call to savePosition
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

        m_ofCurrent = pos.ofCurrent;
        m_iLine     = pos.iLine;
        m_ofInLine  = pos.ofInLine;
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
    * @param  parsepos Specifies the position of the first character of the
    *         sub-section of this script to extract.
    *
    * @return a script object which contains that portion of this
    *         script starting with the character specified by the
    *         passed ParsePosition object and ending with the character
    *         immediately preceding the current position in the script.
    *
    * @see #savePosition
    */
    public Script subScript(ParsePosition parsepos)
        {
        if (parsepos == null || !(parsepos instanceof Position))
            {
            throw new IllegalArgumentException("Illegal ParsePosition object!");
            }

        Position pos = (Position) parsepos;
        if (pos.script != this)
            {
            throw new IllegalArgumentException("Invalid ParsePosition object!");
            }

        int of  = pos.ofCurrent;
        int cch = m_ofCurrent - of;
        if (cch < 0)
            {
            throw new IllegalStateException("The starting offset is greater than the current offset");
            }

        return new SimpleScript(m_achScript, of, cch);
        }

    /**
    * Returns the script as a string.  Note that the returned script may not
    * have the exact contents of the string or stream (or other data) that
    * was used to construct the Script object due to mechanisms such as
    * unicode escape processing.
    *
    * @return the script as a string
    */
    public String getText()
        {
        return toString();
        }

    /**
    * Returns the script as a string.  The returned string is required to
    * reflect the exact contents of the string or stream (or other data)
    * that was used to construct the Script object.
    *
    * @return the script as a string
    */
    public String toString()
        {
        return new String(m_achScript);
        }


    // ----- inner classes --------------------------------------------------

    /**
    * Stores all information required to later restore the current position in
    * the script.
    */
    static class Position implements ParsePosition
        {
        Script  script;
        int     ofCurrent;
        int     iLine;
        int     ofInLine;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The array of characters that make up the script.
    */
    protected char[]  m_achScript;

    /**
    * The current character offset within the script.
    */
    protected int     m_ofCurrent;

    /**
    * The current line number within the script.<p>
    *
    * The current line number accounts for complexities caused by unicode
    * escapes.  For example, the unicode escape slash-u-000A is the new
    * line character ('\n'). In this case, the new line character should
    * not increment the line counter, since it is not an actual new line
    * character in the source.
    *
    * @see #getLine
    * @see #m_ofInLine
    */
    protected int     m_iLine;

    /**
    * The current character offset with the current line.<p>
    *
    * The current offset accounts for complexities caused by unicode escape
    * sequences, like the difference in length between the escape sequence
    * and the character it represents.
    *
    * @see #getOffset
    * @see #m_iLine
    */
    protected int     m_ofInLine;
    }
