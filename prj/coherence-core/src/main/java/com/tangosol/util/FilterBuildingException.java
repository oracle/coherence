/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


/**
* FilterBuildingException is the RuntimeException thrown by the
* {@link QueryHelper} when building a {@link Filter}.  Instances of
* FilterBuildingException should hold the String that was being processed when
* the exception occurred.
*
* @author djl  2009.10.5
*/
public class FilterBuildingException
        extends RuntimeException
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a new FilterBuildingException.
    */
     public FilterBuildingException()
        {
        super();
        }

    /**
    * Construct a new FilterBuildingException with the given error string
    * along with the string that was being parsed.
    *
    * @param sMessage      the message String for the exception
    * @param sParseString  the String that was being parsed
    */
     public FilterBuildingException(String sMessage, String sParseString)
        {
        super(sMessage);

        m_sParseString = sParseString;
        }

    /**
    * Construct a new FilterBuildingException with the given error string,
    * the string that was being parsed and a base exception.
    *
    * @param sMessage        the message String for the exception
    * @param sParseString   the String that was being parsed
    * @param exceptionCause  a base exception that caused the error

    */
     public FilterBuildingException(String sMessage, String sParseString,
            Throwable exceptionCause)
        {
        super(sMessage, exceptionCause);

        m_sParseString = sParseString;
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Answer the String that was being processed when the Exception occurred
    *
    * @return  the String being processed when the Exception occurred
    */
    public String getParseString()
        {
        return m_sParseString;
        }

    // ----- data members ---------------------------------------------------

    /**
    * The String that was being parsed when the exception occurred.
    */
    private String m_sParseString;
    }
