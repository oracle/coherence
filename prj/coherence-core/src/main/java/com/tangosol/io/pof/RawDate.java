/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof;


import java.time.LocalDate;

import java.util.Calendar;
import java.util.Date;


/**
* An immutable POF date value.
*
* @author cp  2006.07.17
* 
* @since Coherence 3.2
*/
public class RawDate
        extends PofHelper
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a raw POF date value.
    *
    * @param nYear   the year number as defined by ISO8601; note the
    *                difference with the Java Date class, whose year is
    *                relative to 1900
    * @param nMonth  the month number between 1 and 12 inclusive as defined
    *                by ISO8601; note the difference from the Java Date
    *                class, whose month value is 0-based (0-11)
    * @param nDay    the day number between 1 and 31 inclusive as defined by
    *                ISO8601
    */
    public RawDate(int nYear, int nMonth, int nDay)
        {
        checkDate(nYear, nMonth, nDay);

        m_nYear  = nYear;
        m_nMonth = nMonth;
        m_nDay   = nDay;
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Determine the date's year value.
    *
    * @return the year number as defined by ISO8601; note the difference with
    *         the Java Date class, whose year is relative to 1900
    */
    public int getYear()
        {
        return m_nYear;
        }

    /**
    * Determine the date's month value.
    *
    * @return the month number between 1 and 12 inclusive as defined by
    *         ISO8601; note the difference from the Java Date class, whose
    *         month value is 0-based (0-11)
    */
    public int getMonth()
        {
        return m_nMonth;
        }

    /**
    * Determine the date's day value.
    *
    * @return the day number between 1 and 31 inclusive as defined by ISO8601
    */
    public int getDay()
        {
        return m_nDay;
        }


    // ----- conversions ----------------------------------------------------

    /**
    * Create RawDate from the specified object.
    *
    * @param o  an object to create RawDate from.
    *
    * @return a RawDate value
    *
    * @throws IllegalArgumentException  if the specified object cannot be
    *                                   converted to RawDate
    */
    public static RawDate from(Object o)
        {
        RawDate d = o instanceof LocalDate      ? from((LocalDate) o)
                  : o instanceof java.sql.Date  ? from((java.sql.Date) o)
                  : o instanceof Date           ? from((Date) o)
                  : null;

        if (d == null)
            {
            throw new IllegalArgumentException("Object " + o + " cannot be converted to RawDate");
            }

        return d;
        }

    /**
    * Create a RawDate from the LocalDate.
    *
    * @param date  a LocalDate value
    *
    * @return a RawDate value
    */
    public static RawDate from(LocalDate date)
        {
        return new RawDate(date.getYear(), date.getMonthValue(), date.getDayOfMonth());
        }

    /**
    * Create a RawDate from the JDBC Date.
    *
    * @param date  a java.sql.Date value
    *
    * @return a RawDate value
    */
    public static RawDate from(java.sql.Date date)
        {
        return from(date.toLocalDate());
        }

    /**
    * Create a RawDate from the Java Date.
    *
    * @param date  a java.util.Date value
    *
    * @return a RawDate value
    */
    public static RawDate from(Date date)
        {
        return new RawDate(date.getYear() + 1900, date.getMonth() + 1, date.getDate());
        }

    /**
    * Create a LocalDate from the raw date information.
    *
    * @return a LocalDate value
    */
    public LocalDate toLocalDate()
        {
        return LocalDate.of(getYear(), getMonth(), getDay());
        }

    /**
    * Create a JDBC Date from the raw date information.
    *
    * @return a JDBC Date value
    */
    public java.sql.Date toSqlDate()
        {
        return new java.sql.Date(getYear() - 1900, getMonth() - 1, getDay());
        }

    /**
    * Create a Java Date from the raw date information.
    *
    * @return a Java Date value
    */
    public Date toJavaDate()
        {
        return new Date(getYear() - 1900, getMonth() - 1, getDay());
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Compare this object with another for equality.
    *
    * @param o  another object to compare to for equality
    *
    * @return true iff this object is equal to the other object
    */
    public boolean equals(Object o)
        {
        if (o instanceof RawDate)
            {
            RawDate that = (RawDate) o;
            return this == that
                   || this.getYear()  == that.getYear()
                   && this.getMonth() == that.getMonth()
                   && this.getDay()   == that.getDay();
            }

        return false;
        }

    /**
    * Obtain the hashcode for this object.
    *
    * @return an integer hashcode
    */
    public int hashCode()
        {
        return (getYear() << 2) ^ (getMonth() << 1) ^ getDay();
        }

    /**
    * Format this object's data as a human-readable string.
    *
    * @return a string description of this object
    */
    public String toString()
        {
        return formatDate(getYear(), getMonth(), getDay());
        }


    // ----- data members ---------------------------------------------------

    /**
    * The year number.
    */
    private int m_nYear;

    /**
    * The month number.
    */
    private int m_nMonth;

    /**
    * The day number.
    */
    private int m_nDay;
    }
