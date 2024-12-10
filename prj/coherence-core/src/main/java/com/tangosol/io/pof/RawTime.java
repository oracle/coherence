/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof;


import java.sql.Time;

import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;


/**
* An immutable POF time value.
*
* @author cp  2006.07.17
*
* @since Coherence 3.2
*/
public class RawTime
        extends PofHelper
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a raw POF time value.
    *
    * @param nHour   the hour between 0 and 23 inclusive
    * @param nMinute the minute value between 0 and 59 inclusive
    * @param nSecond the second value between 0 and 59 inclusive (and
    *                theoretically 60 for a leap-second)
    * @param nNano   the nanosecond value between 0 and 999999999 inclusive
    * @param fUTC    true if the time value is UTC or false if the time value
    *                does not have an explicit time zone
    */
    public RawTime(int nHour, int nMinute, int nSecond, int nNano, boolean fUTC)
        {
        this(nHour, nMinute, nSecond, nNano);

        m_nTimeZoneType = fUTC ? TZ_UTC : TZ_NONE;
        }

    /**
    * Construct a raw POF time value with a timezone.
    *
    * @param nHour         the hour between 0 and 23 inclusive
    * @param nMinute       the minute value between 0 and 59 inclusive
    * @param nSecond       the second value between 0 and 59 inclusive (and
    *                      theoretically 60 for a leap-second)
    * @param nNano         the nanosecond value between 0 and 999999999
    *                      inclusive
    * @param nHourOffset   the timezone offset in hours from UTC, for example
    *                      0 for BST, -5 for EST and 1 for CET
    * @param nMinuteOffset the timezone offset in minutes, for example 0 (in
    *                      most cases) or 30
    */
    public RawTime(int nHour, int nMinute, int nSecond, int nNano,
                   int nHourOffset, int nMinuteOffset)
        {
        this(nHour, nMinute, nSecond, nNano);

        checkTimeZone(nHourOffset, nMinuteOffset);

        m_nTimeZoneType = TZ_OFFSET;
        m_nHourOffset   = nHourOffset;
        m_nMinuteOffset = nMinuteOffset;
        }

    /**
    * Internal constructor.
    *
    * @param nHour   the hour between 0 and 23 inclusive
    * @param nMinute the minute value between 0 and 59 inclusive
    * @param nSecond the second value between 0 and 59 inclusive (and
    *                theoretically 60 for a leap-second)
    * @param nNano   the nanosecond value between 0 and 999999999 inclusive
    */
    private RawTime(int nHour, int nMinute, int nSecond, int nNano)
        {
        checkTime(nHour, nMinute, nSecond, nNano);

        m_nHour         = nHour;
        m_nMinute       = nMinute;
        m_nSecond       = nSecond;
        m_nNano         = nNano;
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Determine the time's hour value.
    *
    * @return the hour between 0 and 23 inclusive
    */
    public int getHour()
        {
        return m_nHour;
        }

    /**
    * Determine the time's minute value.
    *
    * @return the minute value between 0 and 59 inclusive
    */
    public int getMinute()
        {
        return m_nMinute;
        }

    /**
    * Determine the time's second value.
    *
    * @return the second value between 0 and 59 inclusive (and possibly
    *         60 for a leap-second)
    */
    public int getSecond()
        {
        return m_nSecond;
        }

    /**
    * Determine the time's nanosecond value.
    *
    * @return the nanosecond value between 0 and 999999999 inclusive
    */
    public int getNano()
        {
        return m_nNano;
        }

    /**
    * Determine if the time value has an explicit timezone. A time value
    * without an explicit timezone is assumed to be in some conventional
    * local timezone, according to ISO8601.
    *
    * @return true iff the time has an explicit timezone
    */
    public boolean hasTimezone()
        {
        return m_nTimeZoneType != TZ_NONE;
        }

    /**
    * Determine if the time value uses UTC.
    *
    * @return true if the time value is a UTC value
    */
    public boolean isUTC()
        {
        return m_nTimeZoneType == TZ_UTC;
        }

    /**
    * Determine the timezone's hour offset value.
    *
    * @return the hour offset of the timezeone, or zero if there is no
    *         explicit timezone or the time is UTC
    */
    public int getHourOffset()
        {
        return m_nHourOffset;
        }

    /**
    * Determine the timezone's minute offset value.
    *
    * @return the minute offset of the timezeone, or zero if there is no
    *         explicit timezone or the time is UTC
    */
    public int getMinuteOffset()
        {
        return m_nMinuteOffset;
        }


    // ----- conversions ----------------------------------------------------

    /**
    * Create RawTime from the specified object.
    *
    * @param o  an object to create RawTime from.
    *
    * @return a RawTime value
    *
    * @throws IllegalArgumentException  if the specified object cannot be
    *                                   converted to RawTime
    */
    public static RawTime from(Object o)
        {
        RawTime t = o instanceof LocalTime   ? from((LocalTime) o)
                  : o instanceof OffsetTime  ? from((OffsetTime) o)
                  : o instanceof Time        ? from((Time) o)
                  : o instanceof Date        ? from((Date) o)
                  : null;

        if (t == null)
            {
            throw new IllegalArgumentException("Object " + o + " cannot be converted to RawTime");
            }

        return t;
        }

    /**
    * Create RawTime from LocalTime.
    *
    * @param time  time to create raw time from
    *
    * @return a RawTime value
    */
    public static RawTime from(LocalTime time)
        {
        return new RawTime(time.getHour(), time.getMinute(), time.getSecond(), time.getNano(), false);
        }

    /**
    * Create RawTime from OffsetTime.
    *
    * @param time  time to create raw time from
    *
    * @return a RawTime value
    */
    public static RawTime from(OffsetTime time)
        {
        ZoneOffset of = time.getOffset();
        if (of.compareTo(ZoneOffset.UTC) == 0)
            {
            return new RawTime(time.getHour(), time.getMinute(), time.getSecond(), time.getNano(), true);
            }
        else
            {
            int nOfHour   = of.getTotalSeconds() / 3600;
            int nOfMinute = (of.getTotalSeconds() % 3600) / 60;
            return new RawTime(time.getHour(), time.getMinute(), time.getSecond(), time.getNano(), nOfHour, nOfMinute);
            }
        }

    /**
    * Create a RawTime from the JDBC Time.
    *
    * @param time  a Time value
    *
    * @return a RawTime value
    */
    public static RawTime from(Time time)
        {
        return from(time.toLocalTime());
        }

    /**
    * Create a RawTime from the Java Date.
    *
    * @param date  a Date value
    *
    * @return a RawTime value
    */
    public static RawTime from(Date date)
        {
        return from(date.toInstant()
                        .atOffset(ZoneOffset.ofTotalSeconds(date.getTimezoneOffset() * 60))
                        .toOffsetTime());
        }

    /**
    * Create a LocalTime from the raw time information.
    *
    * @return a LocalTime value
    */
    public LocalTime toLocalTime()
        {
        return LocalTime.of(getHour(), getMinute(), getSecond(), getNano());
        }

    /**
    * Create a OffsetTime from the raw time information.
    *
    * @return a OffsetTime value
    *
    * @throws IllegalStateException  if this RawTime does not have time zone
    *                                information
    */
    public OffsetTime toOffsetTime()
        {
        if (hasTimezone())
            {
            ZoneOffset of = isUTC() ? ZoneOffset.UTC : ZoneOffset.ofHoursMinutes(getHourOffset(), getMinuteOffset());
            return OffsetTime.of(getHour(), getMinute(), getSecond(), getNano(), of);
            }
        else
            {
            throw new IllegalStateException("Unable to convert RawTime into OffsetTime because time zone information is missing.");
            }
        }

    /**
    * Create a JDBC Time from the raw time information. Note that the JDBC
    * Time does not include second, nano or timezone information.
    *
    * @return a JDBC Time value
    */
    public Time toSqlTime()
        {
        return new Time(getHour(), getMinute(), getSecond());
        }

    /**
    * Create a Java Date from the raw time information.
    *
    * @return a Java Date value
    */
    public Date toJavaDate()
        {
        // check for a timezone
        TimeZone timezone = null;
        // set the timezone first, because setting it
        // appears to have side-effects (the code for
        // Calendar, TimeZone, Date etc. is unreadable)
        if (hasTimezone())
            {
            // Java confuses UTC with GMT, but they
            // are basically identical
            StringBuffer sb = new StringBuffer();
            sb.append("GMT");
            if (!isUTC())
                {
                int cHours   = getHourOffset();
                int cMinutes = getMinuteOffset();
                if (cHours < 0)
                    {
                    sb.append('-');
                    cHours = -cHours;
                    }
                else
                    {
                    sb.append('+');
                    }
                sb.append(cHours)
                  .append(':')
                  .append(toDecString(cMinutes, 2));
                }

            timezone = getTimeZone(sb.toString());
            }

        // create a "calendar"
        Calendar calendar = timezone == null
                ? Calendar.getInstance()
                : Calendar.getInstance(timezone);

        calendar.set(Calendar.HOUR_OF_DAY, getHour());
        calendar.set(Calendar.MINUTE, getMinute());
        calendar.set(Calendar.SECOND, getSecond());
        calendar.set(Calendar.MILLISECOND, getNano() / 1000000);

        return calendar.getTime();
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
        if (o instanceof RawTime)
            {
            RawTime that = (RawTime) o;
            return this == that
                || this.getHour()         == that.getHour()
                && this.getMinute()       == that.getMinute()
                && this.getSecond()       == that.getSecond()
                && this.getNano()         == that.getNano()
                && this.isUTC()           == that.isUTC()
                && this.getHourOffset()   == that.getHourOffset()
                && this.getMinuteOffset() == that.getMinuteOffset();
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
        return (getHour() << 2) ^ (getMinute() << 1) ^ getSecond() ^ getNano();
        }

    /**
    * Format this object's data as a human-readable string.
    *
    * @return a string description of this object
    */
    public String toString()
        {
        return hasTimezone() && !isUTC()
               ? formatTime(getHour(), getMinute(), getSecond(), getNano(),
                            getHourOffset(), getMinuteOffset())
               : formatTime(getHour(), getMinute(), getSecond(), getNano(),
                            isUTC());
        }


    // ----- constants ------------------------------------------------------

    /**
    * Indicates that the time value does not have an explicit time zone.
    */
    private static final int TZ_NONE = 0;

    /**
    * Indicates that the time value is in UTC.
    */
    private static final int TZ_UTC  = 1;

    /**
    * Indicates that the time value has an explicit time zone.
    */
    private static final int TZ_OFFSET = 2;


    // ----- data members ---------------------------------------------------

    /**
    * The hour number.
    */
    private int m_nHour;

    /**
    * The minute number.
    */
    private int m_nMinute;

    /**
    * The second number.
    */
    private int m_nSecond;

    /**
    * The nanosecond number.
    */
    private int m_nNano;

    /**
    * The timezone indicator, one of the TZ_ enumerated constants.
    */
    private int m_nTimeZoneType;

    /**
    * The hour offset of the time's timezone.
    */
    private int m_nHourOffset;

    /**
    * The minute offset of the time's timezone.
    */
    private int m_nMinuteOffset;
    }
