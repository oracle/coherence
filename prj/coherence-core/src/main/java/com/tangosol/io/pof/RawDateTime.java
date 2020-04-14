/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof;


import java.sql.Timestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;


/**
* An immutable POF date-time value.
*
* @author cp  2006.07.17
*
* @since Coherence 3.2
*/
public class RawDateTime
        extends PofHelper
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Construct a date-time value.
    *
    * @param date  the date portion of the raw date-time value
    * @param time  the time portion of the raw date-time value
    */
    public RawDateTime(RawDate date, RawTime time)
        {
        if (date == null || time == null)
            {
            throw new IllegalArgumentException("date and time required");
            }

        m_date = date;
        m_time = time;
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Obtain the date portion of the raw date-time value.
    *
    * @return the date portion of the raw date-time value
    */
    public RawDate getRawDate()
        {
        return m_date;
        }

    /**
    * Obtain the time portion of the raw date-time value.
    *
    * @return the time portion of the raw date-time value
    */
    public RawTime getRawTime()
        {
        return m_time;
        }


    // ----- conversions ----------------------------------------------------

    /**
    * Create RawDateTime from the specified object.
    *
    * @param o  an object to create RawDateTime from.
    *
    * @return a RawDateTime value
    *
    * @throws IllegalArgumentException  if the specified object cannot be
    *                                   converted to RawDateTime
    */
    public static RawDateTime from(Object o)
        {
        RawDateTime dt = o instanceof LocalDateTime   ? from((LocalDateTime) o)
                       : o instanceof OffsetDateTime  ? from((OffsetDateTime) o)
                       : o instanceof Timestamp       ? from((Timestamp) o)
                       : o instanceof Date            ? from((Date) o)
                       : null;

        if (dt == null)
            {
            throw new IllegalArgumentException("Object " + o + " cannot be converted to RawDateTime");
            }

        return dt;
        }

    /**
    * Create RawDateTime from the specified LocalDateTime.
    *
    * @param dt  a LocalDateTime to create RawDateTime from.
    *
    * @return a RawDateTime value
    */
    public static RawDateTime from(LocalDateTime dt)
        {
        return new RawDateTime(RawDate.from(dt.toLocalDate()), RawTime.from(dt.toLocalTime()));
        }

    /**
    * Create RawDateTime from the specified OffsetDateTime.
    *
    * @param dt  a OffsetDateTime to create RawDateTime from.
    *
    * @return a RawDateTime value
    */
    public static RawDateTime from(OffsetDateTime dt)
        {
        return new RawDateTime(RawDate.from(dt.toLocalDate()), RawTime.from(dt.toOffsetTime()));
        }

    /**
    * Create RawDateTime from the specified Timestamp.
    *
    * @param dt  a Timestamp to create RawDateTime from.
    *
    * @return a RawDateTime value
    */
    public static RawDateTime from(Timestamp dt)
        {
        return from(dt.toLocalDateTime());
        }

    /**
    * Create RawDateTime from the specified Date.
    *
    * @param dt  a Date to create RawDateTime from.
    *
    * @return a RawDateTime value
    */
    public static RawDateTime from(Date dt)
        {
        return from(dt.toInstant()
                      .atOffset(ZoneOffset.ofTotalSeconds(dt.getTimezoneOffset() * 60))
                      .toZonedDateTime()
                      .toOffsetDateTime());
        }

    /**
    * Create a LocalDateTime from the raw date/time information.
    *
    * @return a LocalDateTime value
    */
    public LocalDateTime toLocalDateTime()
        {
        LocalDate d = getRawDate().toLocalDate();
        LocalTime t = getRawTime().toLocalTime();

        return LocalDateTime.of(d, t);
        }

    /**
    * Create an OffsetDateTime from the raw date/time information.
    *
    * @return an OffsetDateTime value
    *
    * @throws IllegalStateException  if this RawDateTime does not have time zone
    *                                information
    */
    public OffsetDateTime toOffsetDateTime()
        {
        LocalDate  d = getRawDate().toLocalDate();
        OffsetTime t = getRawTime().toOffsetTime();

        return OffsetDateTime.of(d, t.toLocalTime(), t.getOffset());
        }

    /**
    * Create a JDBC Timestamp from the raw date/time information. Note that
    * the JDBC Timestamp does not contain timezone information.
    *
    * @return a JDBC Timestamp value
    */
    public Timestamp toSqlTimestamp()
        {
        /*
        // without timezone data:
        RawDate date = getRawDate();
        RawTime time = getRawTime();
        return new Timestamp(date.getYear() - 1900, date.getMonth() - 1, date.getDay(),
                             time.getHour(), time.getMinute(), time.getSecond(), time.getNano());
        */

        Timestamp timestamp = new Timestamp(toJavaDate().getTime());
        timestamp.setNanos(getRawTime().getNano());
        return timestamp;
        }

    /**
    * Create a Java Date from the raw date/time information. Note that
    * the Java Date does not contain nanosecond information.
    *
    * @return a Java Date value
    */
    public Date toJavaDate()
        {
        RawDate date = getRawDate();
        RawTime time = getRawTime();

        // check for a timezone
        TimeZone timezone = null;
        if (time != null)
            {
            // set the timezone first, because setting it
            // appears to have side-effects (the code for
            // Calendar, TimeZone, Date etc. is unreadable)
            if (time.hasTimezone())
                {
                // Java confuses UTC with GMT, but they
                // are basically identical
                StringBuffer sb = new StringBuffer();
                sb.append("GMT");
                if (!time.isUTC())
                    {
                    int cHours   = time.getHourOffset();
                    int cMinutes = time.getMinuteOffset();
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
            }

        // create a "calendar"
        Calendar calendar = timezone == null
                                      ? Calendar.getInstance()
                                      : Calendar.getInstance(timezone);

        if (date != null)
            {
            calendar.set(Calendar.YEAR, date.getYear());
            calendar.set(Calendar.MONTH, date.getMonth() - 1);
            calendar.set(Calendar.DAY_OF_MONTH, date.getDay());
            }

        if (time != null)
            {
            calendar.set(Calendar.HOUR_OF_DAY, time.getHour());
            calendar.set(Calendar.MINUTE, time.getMinute());
            calendar.set(Calendar.SECOND, time.getSecond());
            calendar.set(Calendar.MILLISECOND, time.getNano() / 1000000);
            }

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
        if (o instanceof RawDateTime)
            {
            RawDateTime that = (RawDateTime) o;
            return this == that
                   || this.getRawDate().equals(that.getRawDate())
                      && this.getRawTime().equals(that.getRawTime());
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
        return getRawDate().hashCode() ^ getRawTime().hashCode();
        }

    /**
    * Format this object's data as a human-readable string.
    *
    * @return a string description of this object
    */
    public String toString()
        {
        return getRawDate().toString() + ' ' + getRawTime().toString();
        }


    // ----- data members ---------------------------------------------------

    /**
    * The date portion of the raw date-time value.
    */
    private RawDate m_date;

    /**
    * The time portion of the raw date-time value.
    */
    private RawTime m_time;
    }
