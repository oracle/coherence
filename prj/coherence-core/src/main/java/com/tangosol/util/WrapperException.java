/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


/**
* A WrapperException wraps a Throwable object as a RuntimeException.
*
* @see CheckedWrapperException
*
* @author cp  2000.08.03
*/
public class WrapperException
        extends RuntimeException
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a WrapperException from a Throwable object and an additional
    * description.
    *
    * @param e  the Throwable object
    * @param s  the additional description
    */
    public WrapperException(Throwable e, String s)
        {
        super(s, e);
        }

    /**
    * Construct a WrapperException from a Throwable object.
    *
    * @param e  the Throwable object
    */
    public WrapperException(Throwable e)
        {
        this(e, null);
        }

    /**
    * Construct a WrapperException with a specified detail message.
    *
    * @param s  the String that contains a detailed message
    */
    public WrapperException(String s)
        {
        this(null, s);
        }

    // ----- factory methods ------------------------------------------------

    /**
    * Ensure a WrapperException from a Throwable object and an additional
    * description.
    *
    * @param e  the Throwable object
    * @param s  the additional description
    *
    * @return a new instance of a {@code WrapperException} with a specified
    *         message and cause, unless the specified cause is already
    *         an instance of a {@code WrapperException} with the same message,
    *         in which case the specified throwable is returned
    */
    public static WrapperException ensure(Throwable e, String s)
        {
        return e instanceof WrapperException && (s == null || s.equals(e.getMessage()))
               ? (WrapperException) e
               : new WrapperException(e, s);
        }

    /**
    * Ensure a WrapperException from a Throwable object.
    *
    * @param e  the Throwable object
    *
    * @return a new instance of a {@code WrapperException} with a specified
    *         cause, unless the specified Throwable is already an instance of a
    *         {@code WrapperException}, in which case the specified throwable
    *         is returned
    */
    public static WrapperException ensure(Throwable e)
        {
        return ensure(e, null);
        }

    // ----- accessors ------------------------------------------------------

    /**
    * @return the original (wrapped) exception.
    */
    public Throwable getOriginalException()
        {
        return getCause();
        }

    /**
    * Return the root cause of this exception.
    *
    * @return the root cause of exception.
    */
    public Throwable getRootCause()
        {
        Throwable cause = getCause();
        while (cause != null && cause.getCause() != null)
            {
            cause = cause.getCause();
            }

        return cause;
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
    * Returns the error message string of this WrapperException object.
    *
    * @return  the error message string of this <code>WrapperException</code>
    */
    public String getMessage()
        {
        Throwable t = getRootCause();
        return t == null
               ? super.getMessage()
               : getWrapper() + t.getMessage();
        }

    /**
    * Creates a localized description of this <code>WrapperException</code>.
    * Subclasses may override this method in order to produce a
    * locale-specific message.  For subclasses that do not override this
    * method, the default implementation returns the same result as
    * <code>getMessage()</code>.
    *
    * @return  The localized description of this <code>WrapperException</code>.
    */
    public String getLocalizedMessage()
        {
        Throwable t = getRootCause();
        return t == null
                ? super.getLocalizedMessage()
                : getWrapper() + t.getLocalizedMessage();
        }

    /**
    * Returns a short description of this WrapperException object.
    *
    * @return  a string representation of this <code>WrapperException</code>.
    */
    public String toString()
        {
        Throwable t = getRootCause();
        return t == null ? super.toString() : getWrapper() + t.toString();
        }
    }
