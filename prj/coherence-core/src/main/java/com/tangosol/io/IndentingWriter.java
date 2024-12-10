/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io;


import com.tangosol.util.Base;

import java.io.Writer;
import java.io.PrintWriter;


/**
* An IndentingWriter is used to indent line-based output to an underlying
* Writer.
*
* @author cp  2000.10.17
*/
public class IndentingWriter
        extends PrintWriter
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct an IndentingWriter that indents a certain number of spaces.
    *
    * @param writer   the underlying writer to write to
    * @param cSpaces  the number of spaces to indent each line with
    */
    public IndentingWriter(Writer writer, int cSpaces)
        {
        this(writer, Base.dup(' ', cSpaces));
        }

    /**
    * Construct an IndentingWriter that indents using an indention string.
    *
    * @param writer   the underlying writer to write to
    * @param sIndent  the String value to indent each line with
    */
    public IndentingWriter(Writer writer, String sIndent)
        {
        super(writer, true);

        Base.azzert(sIndent.indexOf('\n') < 0);

        // if this and underlying writers are both indenting writers, combine
        // the indentation strings
        Class clz = IndentingWriter.class;
        if (this.getClass() == clz && writer.getClass() == clz)
            {
            IndentingWriter that = (IndentingWriter) writer;
            sIndent  = new StringBuffer().append(that.m_achIndent).append(sIndent).toString();
            this.out = that.out;
            }

        m_achIndent = sIndent.toCharArray();
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
    * @param c  int specifying a character to be written.
    */
    public void write(int c)
        {
        synchronized (lock)
            {
            if (c == '\n')
                {
                m_fNewline = true;
                }
            else if (m_fNewline)
                {
                m_fNewline = false;
                if (!m_fSuspended)
                    {
                    super.write(m_achIndent);
                    }
                }

            super.write(c);
            }
        }

    /**
    * Write a portion of an array of characters.
    *
    * @param  cbuf  Array of characters
    * @param  off   Offset from which to start writing characters
    * @param  len   Number of characters to write
    */
    public void write(char cbuf[], int off, int len)
        {
        synchronized (lock)
            {
            for (int i = 0; i < len; ++i)
                {
                write(cbuf[off++]);
                }
            }
        }

    /**
    * Write a string.
    *
    * @param  str  String to be written
    */
    public void write(String str)
        {
	    synchronized (lock)
            {
	        write(str.toCharArray());
            }
        }

    /**
    * Write a portion of a string.
    *
    * @param  str  A String
    * @param  off  Offset from which to start writing characters
    * @param  len  Number of characters to write
    */
    public void write(String str, int off, int len)
        {
	    synchronized (lock)
            {
    	    char ach[];
	        if (len <= MAX_BUF)
                {
                ach = m_achBuf;
		        if (ach == null)
                    {
		            m_achBuf = ach = new char[MAX_BUF];
		            }
	            }
            else
                {
		        ach = new char[len];
	            }
	        str.getChars(off, (off + len), ach, 0);
	        write(ach, 0, len);
	        }
        }


    // ----- PrintWriter methods --------------------------------------------

    /**
    * Terminate the current line by writing the line separator string.  The
    * line separator string is defined by the system property
    * <code>line.separator</code>, and is not necessarily a single newline
    * character (<code>'\n'</code>).
    */
    public void println()
        {
	    super.println();
        m_fNewline = true;
        }


    // ----- IndentingWriter methods ----------------------------------------

    /**
    * Suspends indentation.
    */
    public void suspend()
        {
        m_fSuspended = true;
        }

    /**
    * Resumes indentation.
    */
    public void resume()
        {
        m_fSuspended = false;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The characters to use to indent each line.
    */
    private char[] m_achIndent;

    /**
    * True if the IndentingWriter is on a new line.
    */
    private boolean m_fNewline = true;

    /**
    * True if the indentation feature of the IndentingWriter is suspended.
    */
    private boolean m_fSuspended = false;

    private char[] m_achBuf;
    private static final int MAX_BUF = 1024;
    }
