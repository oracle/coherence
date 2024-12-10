/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof;


/**
* An immutable POF year-month interval value.
*
* @author cp  2006.07.17
*
* @since Coherence 3.2
*/
public class RawYearMonthInterval
        extends PofHelper
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a raw POF year-month interval value.
    *
    * @param cYears  the number of years in the year-month interval
    * @param cMonths the number of months in the year-month interval
    */
    public RawYearMonthInterval(int cYears, int cMonths)
        {
        checkYearMonthInterval(cYears, cMonths);

        m_cYears  = cYears;
        m_cMonths = cMonths;
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Determine the number of years in the year-month interval.
    *
    * @return the number of years in the year-month interval
    */
    public int getYears()
        {
        return m_cYears;
        }

    /**
    * Determine the number of months in the year-month interval.
    *
    * @return the number of months in the year-month interval
    */
    public int getMonths()
        {
        return m_cMonths;
        }


    // ----- Object methods -------------------------------------------------

    /**
    * {@inheritDoc}
    */
    public boolean equals(Object o)
        {
        if (o instanceof RawYearMonthInterval)
            {
            RawYearMonthInterval that = (RawYearMonthInterval) o;
            return this == that
                || this.getYears()  == that.getYears()
                && this.getMonths() == that.getMonths();
            }

        return false;
        }

    /**
    * {@inheritDoc}
    */
    public int hashCode()
        {
        return getYears() ^ getMonths();
        }

    /**
    * {@inheritDoc}
    */
    public String toString()
        {
        return "Years=" + getYears() + ", Months=" + getMonths();
        }


    // ----- data members ---------------------------------------------------

    /**
    * The number of years in the year-month interval.
    */
    private int m_cYears;

    /**
    * The number of months in the year-month interval.
    */
    private int m_cMonths;
    }
