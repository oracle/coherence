/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import java.io.PrintStream;
import java.io.PrintWriter;


/**
* A CheckedWrapperException wraps a Throwable object as a checked Exception.
*
* @see WrapperException
*
* @author cp  2000.08.03
*/
public class CheckedWrapperException
        extends Exception
    {
    // ----- constructors

    /**
    * Construct a CheckedWrapperException from a Throwable object and 
    * an additional description.
    *
    * @param e  the Throwable object
    * @param s  the additional description
    */
    public CheckedWrapperException(Throwable e, String s)
        {
        super(s);
        m_e = e;
        }

    /**
    * Construct a CheckedWrapperException from a Throwable object.
    *
    * @param e  the Throwable object
    */
    public CheckedWrapperException(Throwable e)
        {
        this(e, null);
        }

    /**
    * Construct a CheckedWrapperException with a specified detail message.
    *
    * @param s  the String that contains a detailed message
    */
    public CheckedWrapperException(String s)
        {
        this(null, s);
        }

    /**
    * Construct a CheckedWrapperException.
    */
    protected CheckedWrapperException()
        {
        this(null, null);
        }

    // ----- accessors ------------------------------------------------------

    /**
    * @return the original (wrapped) exception.
    */
    public Throwable getOriginalException()
        {
        return m_e;
        }

    /**
    * @return a String that shows the original exception was wrapped
    */
    public String getWrapper()
        {
        String s = super.getMessage();
        if (s == null)
            {
            return "(Wrapped) ";
            }
        else
            {
            return "(Wrapped: " + s + ") ";
            }
        }


    // ----- Throwable methods ----------------------------------------------

    /**
    * Returns the error message string of this CheckedWrapperException object.
    *
    * @return  the error message string of this <code>CheckedWrapperException</code> 
    */
    public String getMessage()
        {
        String sMsgThis = super.getMessage();
        String sMsgWrap = m_e == null ? "" : getWrapper() + m_e.getMessage();

        return sMsgThis == null ? sMsgWrap : sMsgThis + ' ' + sMsgWrap;
        }

    /**
    * Creates a localized description of this <code>Throwable</code>.
    * Subclasses may override this method in order to produce a
    * locale-specific message.  For subclasses that do not override this
    * method, the default implementation returns the same result as
    * <code>getMessage()</code>.
    *
    * @return  The localized description of this <code>Throwable</code>.
    */
    public String getLocalizedMessage()
        {
        String sMsgThis = super.getLocalizedMessage();
        String sMsgWrap = m_e == null ? "" : getWrapper() + m_e.getLocalizedMessage();

        return sMsgThis == null ? sMsgWrap : sMsgThis + ' ' + sMsgWrap;
        }

    /**
    * Returns a short description of this CheckedWrapperException object.
    *
    * @return  a string representation of this <code>Throwable</code>.
    */
    public String toString()
        {
        String sMsgThis = super.toString();
        String sMsgWrap = m_e == null ? "" : getWrapper() + m_e.toString();

        return sMsgThis + ' ' + sMsgWrap;
        }

    /**
    * Prints this <code>CheckedWrapperException</code> and its backtrace to the 
    * standard error stream.
    */
    public void printStackTrace()
        {
        if (m_e == null)
            {
            super.printStackTrace();
            }
        else
            {
            synchronized (System.err)
                {
                System.err.print(getWrapper());
                m_e.printStackTrace();
                }
            }
        }

    /**
    * Prints this <code>Throwable</code> and its backtrace to the 
    * specified print stream.
    *
    * @param stream <code>PrintStream</code> to use for output
    */
    public void printStackTrace(PrintStream stream)
        {
        if (m_e == null)
            {
            super.printStackTrace(stream);
            }
        else
            {
            synchronized (stream)
                {
                stream.print(getWrapper());
                m_e.printStackTrace(stream);
                }
            }
        }

    /**
    * Prints this <code>Throwable</code> and its backtrace to the specified
    * print writer.
    *
    * @param writer <code>PrintWriter</code> to use for output
    */
    public void printStackTrace(PrintWriter writer)
        {
        if (m_e == null)
            {
            super.printStackTrace(writer);
            }
        else
            {
            synchronized (writer)
                {
                writer.print(getWrapper());
                m_e.printStackTrace(writer);
                }
            }
        }


    // ----- data members ---------------------------------------------------

    /**
    * The wrapped Throwable object.
    */
    private Throwable m_e;
    }