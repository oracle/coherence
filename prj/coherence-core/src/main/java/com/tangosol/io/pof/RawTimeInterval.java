/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof;


/**
* An immutable POF time interval value.
*
* @author cp  2006.07.17
*
* @since Coherence 3.2
*/
public class RawTimeInterval
        extends PofHelper
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a raw POF time interval value.
    *
    * @param cHours    the number of hours in the time interval
    * @param cMinutes  the number of minutes in the time interval
    * @param cSeconds  the number of seconds in the time interval
    * @param cNanos    the number of nanoseconds in the time interval
    */
    public RawTimeInterval(int cHours, int cMinutes, int cSeconds, int cNanos)
        {
        checkTimeInterval(cHours, cMinutes, cSeconds, cNanos);

        m_cHours         = cHours;
        m_cMinutes       = cMinutes;
        m_cSeconds       = cSeconds;
        m_cNanos         = cNanos;
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Determine the number of hours in the time interval.
    *
    * @return the number of hours in the time interval
    */
    public int getHours()
        {
        return m_cHours;
        }

    /**
    * Determine the number of minutes in the time interval.
    *
    * @return the number of minutes in the time interval
    */
    public int getMinutes()
        {
        return m_cMinutes;
        }

    /**
    * Determine the number of seconds in the time interval.
    *
    * @return the number of seconds in the time interval
    */
    public int getSeconds()
        {
        return m_cSeconds;
        }

    /**
    * Determine the number of nanoseconds in the time interval.
    *
    * @return the number of nanoseconds in the time interval
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
        if (o instanceof RawTimeInterval)
            {
            RawTimeInterval that = (RawTimeInterval) o;
            return this == that
                || this.getHours()   == that.getHours()
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
        return (getHours() << 2) ^ (getMinutes() << 1) ^ getSeconds() ^ getNanos();
        }

    /**
    * {@inheritDoc}
    */
    public String toString()
        {
        return "Hours="     + getHours()   +
               ", Minutes=" + getMinutes() +
               ", Seconds=" + getSeconds() +
               ", Nanos="   + getNanos();
        }


    // ----- data members ---------------------------------------------------

    /**
    * The number of hours in the time interval.
    */
    private int m_cHours;

    /**
    * The number of minutes in the time interval.
    */
    private int m_cMinutes;

    /**
    * The number of seconds in the time interval.
    */
    private int m_cSeconds;

    /**
    * The number of nanoseconds in the time interval.
    */
    private int m_cNanos;
    }
