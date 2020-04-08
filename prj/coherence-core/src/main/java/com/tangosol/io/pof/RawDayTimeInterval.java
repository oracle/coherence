/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof;


/**
* An immutable POF day-time interval value.
*
* @author cp  2006.07.17
*
* @since Coherence 3.2
*/
public class RawDayTimeInterval
        extends PofHelper
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a raw POF day-time interval value.
    *
    * @param cDays     the number of days in the day-time interval
    * @param cHours    the number of hours in the day-time interval
    * @param cMinutes  the number of minutes in the day-time interval
    * @param cSeconds  the number of seconds in the day-time interval
    * @param cNanos    the number of nanoseconds in the day-time interval
    */
    public RawDayTimeInterval(int cDays, int cHours, int cMinutes,
            int cSeconds, int cNanos)
        {
        checkDayTimeInterval(cDays, cHours, cMinutes, cSeconds, cNanos);

        m_cDays    = cDays;
        m_cHours   = cHours;
        m_cMinutes = cMinutes;
        m_cSeconds = cSeconds;
        m_cNanos   = cNanos;
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Determine the number of days in the day-time interval.
    *
    * @return the number of days in the day-time interval
    */
    public int getDays()
        {
        return m_cDays;
        }

    /**
    * Determine the number of hours in the day-time interval.
    *
    * @return the number of hours in the day-time interval
    */
    public int getHours()
        {
        return m_cHours;
        }

    /**
    * Determine the number of minutes in the day-time interval.
    *
    * @return the number of minutes in the day-time interval
    */
    public int getMinutes()
        {
        return m_cMinutes;
        }

    /**
    * Determine the number of seconds in the day-time interval.
    *
    * @return the number of seconds in the day-time interval
    */
    public int getSeconds()
        {
        return m_cSeconds;
        }

    /**
    * Determine the number of nanoseconds in the day-time interval.
    *
    * @return the number of nanoseconds in the day-time interval
    */
    public int getNanos()
        {
        return m_cNanos;
        }


    // ----- Object methods -------------------------------------------------

    /**
    * {@inheritDoc}
    */
    public boolean equals(Object o)
        {
        if (o instanceof RawDayTimeInterval)
            {
            RawDayTimeInterval that = (RawDayTimeInterval) o;
            return this == that
                || this.getDays()    == that.getDays()
                && this.getHours()   == that.getHours()
                && this.getMinutes() == that.getMinutes()
                && this.getSeconds() == that.getSeconds()
                && this.getNanos()   == that.getNanos();
            }

        return false;
        }

    /**
    * {@inheritDoc}
    */
    public int hashCode()
        {
        return (getDays() << 3) ^ (getHours() << 2) ^ (getMinutes() << 1)
                ^ getSeconds() ^ getNanos();
        }

    /**
    * {@inheritDoc}
    */
    public String toString()
        {
        return "Days="      + getDays()    +
               ", Hours="   + getHours()   +
               ", Minutes=" + getMinutes() +
               ", Seconds=" + getSeconds() +
               ", Nanos="   + getNanos();
        }


    // ----- data members ---------------------------------------------------

    /**
    * The number of days in the day-time interval.
    */
    private int m_cDays;

    /**
    * The number of hours in the day-time interval.
    */
    private int m_cHours;

    /**
    * The number of minutes in the day-time interval.
    */
    private int m_cMinutes;

    /**
    * The number of seconds in the day-time interval.
    */
    private int m_cSeconds;

    /**
    * The number of nanoseconds in the day-time interval.
    */
    private int m_cNanos;
    }
