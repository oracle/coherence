/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.dev.compiler;


import java.util.ResourceBundle;

import com.tangosol.util.ErrorList;


/**
* Extends the generic ErrorList.Item class to include compilation information.
*
* @version 1.00, 12/01/96
* @author  Cameron Purdy
* @see com.tangosol.util.ErrorList.Item
*/
public class CompilerErrorInfo
        extends ErrorList.Item
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Constructs a CompilerErrorInfo based on a severity level, error
    * code, and replaceable parameters for the error description.
    *
    * @param nSeverity  the error severity
    * @param sCode      the error code
    * @param rb         the ResourceBundle object to use to get the error description
    * @param asParam    an array of replaceable parameters
    * @param iLine      line in which the error was detected
    * @param ofInLine   offset of the error within the line
    * @param cchError   length of the detected error
    */
    public CompilerErrorInfo(int nSeverity, String sCode, ResourceBundle rb,
            String[] asParam, int iLine, int ofInLine, int cchError)
        {
        this(nSeverity, sCode, rb, asParam, iLine, ofInLine, iLine, ofInLine + cchError);
        }

    /**
    * Constructs a CompilerErrorInfo based on a severity level, error
    * code, and replaceable parameters for the error description.
    *
    * @param nSeverity  the error severity
    * @param sCode      the error code
    * @param rb         the ResourceBundle object to use to get the error description
    * @param asParam    an array of replaceable parameters
    * @param iStartLine line in which the error was detected
    * @param ofStart    offset of the error within the line
    * @param iEndLine   last line in which the error was detected
    * @param ofEnd      length of the detected error
    */
    public CompilerErrorInfo(int nSeverity, String sCode, ResourceBundle rb,
            String[] asParam, int iStartLine, int ofStart, int iEndLine, int ofEnd)
        {
        super(sCode, nSeverity, null, asParam, null, rb);

        m_iStartLine = iStartLine;
        m_ofStart    = ofStart;
        m_iEndLine   = iEndLine;
        m_ofEnd      = ofEnd;
        }


    // ----- error info interface -------------------------------------------

    /**
    * Determines the line number that the error occurred in.
    *
    * @return the 0-based line number that the error occurred in
    */
    public int getLine()
        {
        return m_iStartLine;
        }

    /**
    * Determines the offset within the line where the error occurred.
    *
    * @return the 0-based offset within the line where the error occurred
    */
    public int getOffset()
        {
        return m_ofStart;
        }

    /**
    * Determines the length of the portion of the script which caused the
    * error.  If the length cannot be determined, 0 is returned.
    *
    * @return the length of the error
    */
    public int getLength()
        {
        return m_iStartLine == m_iEndLine ? m_ofEnd - m_ofStart : 0;
        }

    /**
    * Determines the line number that the error occurred in.
    *
    * @return the 0-based line number that the error occurred in
    */
    public int getEndLine()
        {
        return m_iEndLine;
        }

    /**
    * Determines the offset within the line where the error occurred.
    *
    * @return the 0-based offset within the line where the error occurred
    */
    public int getEndOffset()
        {
        return m_ofEnd;
        }

    /**
    * Returns the localized textual description of the error, including the
    * location within the script of the error.
    *
    * @return the localized error description
    */
    public String toString()
        {
        return "(" + getLine() + ',' + getOffset() + ',' + getLength() + ") " + super.toString();
        }


    // ----- data members ---------------------------------------------------

    /**
    * Starting line.
    */
    private int m_iStartLine;

    /**
    * Starting offset.
    */
    private int m_ofStart;

    /**
    * Ending line.
    */
    private int m_iEndLine;

    /**
    * Ending offset.
    */
    private int m_ofEnd;
    }
