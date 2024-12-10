/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.compiler;


import java.io.EOFException;
import java.io.IOException;


/**
* Provides a character stream interface for script parsing.  In addition to
* providing a character at a time with put-back ability, the Script interface
* also allows a position to be saved and later restored, and it is able to
* supply a line number and offset at any point.
*
* @version 1.00, 12/05/96
* @author Cameron Purdy
*/
public interface Script
    {
    // ----- constants ------------------------------------------------------

    /**
    * end-of-file marker (ctrl-z)
    */
    public static final char SUB = '\032';


    // ----- methods --------------------------------------------------------

    /**
    * Initializes the script object.  This method must be called exactly
    * one time to initialize the script object.
    *
    * @param sScript the string containing the script
    */
    public void setScript(String sScript);

    /**
    * Checks for more characters in the script.
    *
    * @return true if there are more characters in the script.
    */
    public boolean hasMoreChars();

    /**
    * Eats and returns the next character from the script.
    *
    * @return the next character of the script
    *
    * @exception EOFException If the script is exhausted or an un-expected
    *            end-of-file marker is encountered
    * @exception IOException  If the script contains invalid data or an
    *            error occurs processing the script
    *
    * @see com.tangosol.dev.compiler.Script#hasMoreChars
    */
    public char nextChar() throws IOException;

    /**
    * Regurgitates the last eaten character so that the next call to
    * nextChar will return the same character that was returned by the
    * most recent call to nextChar.  (This method can be called more
    * than once to regurgitate multiple characters.)
    */
    public void putBackChar();

    /**
    * Returns an object that can be used to restore the current position
    * in the script.  This method is similar to the mark method of the
    * Java stream classes, but by returning an object that identifies the
    * position, multiple positions can be saved and later returned to.
    *
    * @return an object which identifies the current position within the
    *         script
    *
    * @see com.tangosol.dev.compiler.Script#restorePosition
    */
    public ParsePosition savePosition();

    /**
    * Restores the current parsing position that was returned from
    * the savePosition method.
    *
    * @param pos The return value from a previous call to savePosition
    *
    * @see com.tangosol.dev.compiler.Script#savePosition
    */
    public void restorePosition(ParsePosition pos);

    /**
    * Determines the current line.  The current line is the line from which
    * the next character returned from nextChar will come.  The first line
    * of the script is line 0.
    *
    * @return the line number of the next character
    *
    * @see com.tangosol.dev.compiler.Script#getOffset
    */
    public int getLine();

    /**
    * Determines the current offset.  The current offset is the offset
    * within the current line of the next character that will be returned
    * from nextChar.  The offset is 0-based, so the first character in a
    * line is at offset 0.
    *
    * @return the offset of the next character in the current line
    *
    * @see com.tangosol.dev.compiler.Script#getLine
    */
    public int getOffset();

    /**
    * Creates a Script object which contains a sub-section of this script.
    *
    * @param  pos Specifies the position of the first character of the
    *         sub-section of this script to extract.
    *
    * @return a script object which contains that portion of this
    *         script starting with the character specified by the
    *         passed ParsePosition object and ending with the character
    *         immediately preceding the current position in the script.
    *
    * @see com.tangosol.dev.compiler.Script#savePosition
    */
    public Script subScript(ParsePosition pos);

    /**
    * Returns the script as a string.  Note that the returned script may not
    * have the exact contents of the string or stream (or other data) that
    * was used to construct the Script object due to mechanisms such as
    * unicode escape processing.
    *
    * @exception IOException If the script contains invalid data which
    *            prevents it from being converted to a String.
    */
    public String getText() throws IOException;

    /**
    * Returns the script as a string.  The returned string is required to
    * reflect the exact contents of the string or stream (or other data)
    * that was used to construct the Script object.
    */
    public String toString();
    }
