/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import java.text.MessageFormat;

import java.util.Arrays;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;


/**
* Provides a means to accumulate errors.
*
* @version 1.00, 10/09/00
* @author cp
*/
public class ErrorList
        extends AbstractList
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public ErrorList()
        {
        }

    /**
    * Construct a limited-size error list.
    */
    public ErrorList(int cMax)
        {
        m_cMax = cMax;
        }


    // ----- accessors ------------------------------------------------------

    /**
    * @return true if the ErrorList contains any severe items
    */
    public boolean isSevere()
        {
        return m_cSev > 0;
        }

    /**
    * @return the maximium severity of any item in the ErrorList
    */
    public int getSeverity()
        {
        return m_nSev;
        }


    // ----- simple ErrorList queueing --------------------------------------

    /**
    * Adds a simple error to the ErrorList.
    *
    * @param nSev   The error severity.
    * @param sText  The error text.
    *
    * @exception OverflowException  Thrown when the error list exceeds
    *            the maximum number of serious errors.
    */
    public void add(int nSev, String sText)
        {
        add(new Item(nSev, sText));
        }

    /**
    * Adds a simple info to the ErrorList.
    *
    * @param sInfo  The info text.
    *
    * @exception OverflowException Thrown when the error list exceeds
    *            the maximum number of serious errors.
    */
    public void addInfo(String sInfo)
        {
        add(Constants.INFO, sInfo);
        }

    /**
    * Adds a simple warning to the ErrorList.
    *
    * @param sWarning The warning text.
    *
    * @exception OverflowException Thrown when the error list exceeds
    *            the maximum number of serious errors.
    */
    public void addWarning(String sWarning)
        {
        add(Constants.WARNING, sWarning);
        }

    /**
    * Adds a simple error to the ErrorList.
    *
    * @param sError The error text.
    *
    * @exception OverflowException Thrown when the error list exceeds
    *            the maximum number of serious errors.
    */
    public void addError(String sError)
        {
        add(Constants.ERROR, sError);
        }

    /**
    * Adds a simple fatal error to the ErrorList.
    *
    * @param sError The error text.
    *
    * @exception OverflowException Thrown when the error list exceeds
    *            the maximum number of serious errors.
    */
    public void addFatal(String sError)
        {
        add(Constants.FATAL, sError);
        }

    /**
    * Adds an error for the specified exception to the ErrorList
    *
    * @param e The exception
    *
    * @exception OverflowException Thrown when the error list exceeds
    *            the maximum number of serious errors.
    */
    public void addException(Throwable e)
        {
        Item item = new Item(Constants.FATAL, e.toString());
        item.setLocator(e);

        add(item);
        }

    // ----- List interface -------------------------------------------------

    /**
    * Returns the number of elements in this collection.  If the collection
    * contains more than <tt>Integer.MAX_VALUE</tt> elements, returns
    * <tt>Integer.MAX_VALUE</tt>.
    *
    * @return the number of elements in this collection.
    */
    public int size()
        {
        return m_listErrors.size();
        }

    /**
    * Returns the element at the specified position in this list.
    *
    * @param index index of element to return.
    *
    * @return the element at the specified position in this list.
    * @throws IndexOutOfBoundsException if the given index is out of range
    * 		  (<tt>index &lt; 0 || index &gt;= size()</tt>).
    */
    public Object get(int index)
        {
        return m_listErrors.get(index);
        }

    /**
    * Appends the specified element to the end of this List (optional
    * operation). <p>
    *
    * This implementation calls <tt>add(size(), o)</tt>.<p>
    *
    * Note that this implementation throws an
    * <tt>UnsupportedOperationException</tt> unless <tt>add(int, Object)</tt>
    * is overridden.
    *
    * @param o element to be appended to this list.
    *
    * @return <tt>true</tt> (as per the general contract of
    * <tt>Collection.add</tt>).
    *
    * @throws UnsupportedOperationException if the <tt>add</tt> method is not
    * 		  supported by this Set.
    *
    * @throws ClassCastException if the class of the specified element
    * 		  prevents it from being added to this set.
    *
    * @throws IllegalArgumentException some aspect of this element prevents
    *            it from being added to this collection.
    */
    public synchronized boolean add(Object o)
        {
        Item item = (Item) o;
        m_listErrors.add(item);

        if (item.isSevere())
            {
            ++m_cSev;
            if (m_cSev > m_cMax)
                {
                throw new OverflowException();
                }
            }

        if (item.getSeverity() > m_nSev)
            {
            m_nSev = item.getSeverity();
            }

        return true;
        }

    /**
    * Removes all of the elements from this collection (optional operation).
    * The collection will be empty after this call returns (unless it throws
    * an exception).<p>
    *
    * This implementation calls <tt>removeRange(0, size())</tt>.<p>
    *
    * Note that this implementation throws an
    * <tt>UnsupportedOperationException</tt> unless <tt>remove(int
    * index)</tt> or <tt>removeRange(int fromIndex, int toIndex)</tt> is
    * overridden.
    *
    * @throws UnsupportedOperationException if the <tt>clear</tt> method is
    * 		  not supported by this Collection.
    */
    public void clear()
        {
        m_listErrors.clear();
        m_cSev = 0;
        m_nSev = Constants.NONE;
        }


    // ----- Object methods -------------------------------------------------

    /**
    * @return a String fully describing the ErrorList
    */
    public String toString()
        {
        if (isEmpty())
            {
            return "ErrorList is empty.";
            }
        else
            {
            StringBuffer sb = new StringBuffer();
            int          c  = size();

            sb.append("ErrorList contains " + c + " items:");
            for (int i = 0; i < c; ++i)
                {
                sb.append("\n[" + i + "]=" + get(i));
                }
            return sb.toString();
            }
        }

    // ----- helpers --------------------------------------------------------

    /**
    * Print all errors in the ErrorList.
    */
    public void print()
        {
        Base.out(toString());
        }

    public static String toSeverityString(int nSev)
        {
        switch (nSev)
            {
            case Constants.NONE:
                return "No Error";
            case Constants.INFO:
                return "Information";
            case Constants.WARNING:
                return "Warning";
            case Constants.ERROR:
                return "Error";
            case Constants.FATAL:
                return "Fatal Error";
            default:
                return "Unknown Severity";
            }
        }


    // ----- inner class:  ErrorList.Item -----------------------------------

    /**
    * An ErrorList Item contains the information about an error that is
    * queued in an ErrorList.
    */
    public static class Item
            extends Base
        {
        /**
        * Construct an item.
        */
        public Item(String sCode, int nSev, String sText, Object[] aoParam, Object oLocator, ResourceBundle res)
            {
            m_sCode    = sCode;
            m_nSev     = nSev;
            m_sText    = sText;
            m_aoParam  = aoParam;
            m_oLocator = oLocator;
            m_res      = res;
            }

        /**
        * Construct a simple item.
        */
        public Item(int nSev, String sText)
            {
            this(null, nSev, sText, null, null, null);
            }

        /**
        * Construct a simple item.
        */
        public Item(int nSev, String sText, Object[] aoParam)
            {
            this(null, nSev, sText, aoParam, null, null);
            }

        /**
        * @return the error code or null
        */
        public String getCode()
            {
            return m_sCode;
            }

        /**
        * @return the error severity
        */
        public int getSeverity()
            {
            return m_nSev;
            }

        /**
        * @return true if the error is severe
        */
        public boolean isSevere()
            {
            return m_nSev > Constants.WARNING;
            }

        /**
        * @return the unformatted error message
        */
        public String getText()
            {
            if (m_sText == null)
                {
                if (m_sCode != null && m_res != null)
                    {
                    m_sText = m_res.getString(m_sCode);
                    }

                if (m_sText == null)
                    {
                    m_sText = "";
                    }
                }

            return m_sText;
            }

        /**
        * @return the message parameters
        */
        public Object[] getParameters()
            {
            return m_aoParam;
            }

        /**
        * @return the formatted error message
        */
        public String getMessage()
            {
            String sMsg = getText();

            if (m_aoParam != null && m_aoParam.length > 0)
                {
                try
                    {
                    sMsg = MessageFormat.format(sMsg, m_aoParam);
                    }
                catch (Exception e)
                    {
                    }
                }
            return sMsg;
            }

        /**
        * @return the locator
        */
        public Object getLocator()
            {
            return m_oLocator;
            }

        /**
        * @param oLocator  the locator (e.g. a source file/line number)
        */
        public void setLocator(Object oLocator)
            {
            m_oLocator = oLocator;
            }

        /**
        * @return a String fully describing the ErrorList Item
        */
        public String toString()
            {
            StringBuffer sb = new StringBuffer();

            if (m_oLocator != null && !(m_oLocator instanceof Throwable))
                {
                sb.append(' ')
                  .append(m_oLocator)
                  .append(':');
                }

            if (m_nSev > 0)
                {
                sb.append(' ')
                  .append(toSeverityString(m_nSev));
                }

            if (m_sCode != null)
                {
                sb.append("  #")
                  .append(m_sCode);
                }

            if (m_nSev > 0 || m_sCode != null)
                {
                sb.append(":");
                }

            sb.append(' ')
              .append(getMessage());

            return sb.substring(1);
            }

        /**
        * Compares this Item to another Object for equality.
        *
        * @param obj  the other Object to compare to this
        *
        * @return true if this Component equals that Object
        */
        public boolean equals(Object obj)
            {
            if (obj instanceof Item)
                {
                Item that = (Item) obj;

                return m_nSev == that.m_nSev &&
                       (m_sCode == null ? that.m_sCode == null : m_sCode.equals(that.m_sCode)) &&
                       (m_sText == null ? that.m_sText == null : m_sText.equals(that.m_sText)) &&
                       Arrays.equals(m_aoParam, that.m_aoParam) &&
                       (m_oLocator == null ? that.m_oLocator == null : m_oLocator.equals(that.m_oLocator))
                       ;
                }
            return false;
            }

        private String          m_sCode;
        private int             m_nSev;
        private String          m_sText;
        private Object[]        m_aoParam;
        private Object          m_oLocator;
        private ResourceBundle  m_res;
        }


    // ----- inner class:  ErrorList.OverflowException ----------------------

    /**
    * An ErrorList Item contains the information about an error that is
    * queued in an ErrorList.
    */
    public class OverflowException extends RuntimeException
        {
        protected OverflowException()
            {
            super("ErrorList limit");
            }
        }


    // ----- constants ------------------------------------------------------

    public interface Constants
        {
        /**
        * Severity:  n/a (no errors)
        */
        public static final int NONE    = 0;
        /**
        * Severity:  There is information that the user may be interested in.
        */
        public static final int INFO    = 1;
        /**
        * Severity:  An potential problem exists that the user should be aware of.
        */
        public static final int WARNING = 2;
        /**
        * Severity:  A real error condition was detected.
        */
        public static final int ERROR   = 3;
        /**
        * Severity:  An unexpected internal error occurred.
        */
        public static final int FATAL   = 4;
        }


    // ----- data members ---------------------------------------------------

    /**
    * Severity of the list.
    */
    private int m_nSev = Constants.NONE;

    /**
    * Number of severe errors.
    */
    private int m_cSev;

    /**
    * Max number of severe errors.
    */
    private int m_cMax = Integer.MAX_VALUE;

    /**
    * Array of errors.
    */
    private List m_listErrors = new ArrayList();
    }
