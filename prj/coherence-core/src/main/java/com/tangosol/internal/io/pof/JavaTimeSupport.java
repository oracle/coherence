/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.io.pof;


import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofSerializer;
import com.tangosol.io.pof.PofWriter;

import java.io.IOException;

import java.time.Duration;
import java.time.Instant;
import java.time.MonthDay;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;


/**
 * {@link PofSerializer} implementations for the {@link java.time} classes that
 * are not natively supported in POF.
 *
 * @author as  2015.02.20
 * @since 12.2.1
 */
public class JavaTimeSupport
    {
    /**
     * Duration serializer.
     */
    public static class DurationSerializer
            implements PofSerializer<Duration>
        {
        @Override
        public void serialize(PofWriter out, Duration value) throws IOException
            {
            out.writeLong(0, value.getSeconds());
            out.writeInt (1, value.getNano());
            out.writeRemainder(null);
            }

        @Override
        public Duration deserialize(PofReader in) throws IOException
            {
            long nSeconds = in.readLong(0);
            int  nNanos   = in.readInt(1);
            in.readRemainder();

            return Duration.ofSeconds(nSeconds, nNanos);
            }
        }

    /**
     * Instant serializer.
     */
    public static class InstantSerializer
            implements PofSerializer<Instant>
        {
        @Override
        public void serialize(PofWriter out, Instant value) throws IOException
            {
            out.writeLong(0, value.getEpochSecond());
            out.writeInt (1, value.getNano());
            out.writeRemainder(null);
            }

        @Override
        public Instant deserialize(PofReader in) throws IOException
            {
            long nSeconds = in.readLong(0);
            int  nNanos   = in.readInt(1);
            in.readRemainder();

            return Instant.ofEpochSecond(nSeconds, nNanos);
            }
        }

    /**
     * MonthDay serializer.
     */
    public static class MonthDaySerializer
            implements PofSerializer<MonthDay>
        {
        @Override
        public void serialize(PofWriter out, MonthDay value) throws IOException
            {
            out.writeInt(0, value.getMonthValue());
            out.writeInt(1, value.getDayOfMonth());
            out.writeRemainder(null);
            }

        @Override
        public MonthDay deserialize(PofReader in) throws IOException
            {
            int nMonth = in.readInt(0);
            int nDay   = in.readInt(1);
            in.readRemainder();

            return MonthDay.of(nMonth, nDay);
            }
        }

    /**
     * Period serializer.
     */
    public static class PeriodSerializer
            implements PofSerializer<Period>
        {
        @Override
        public void serialize(PofWriter out, Period value) throws IOException
            {
            out.writeInt(0, value.getYears());
            out.writeInt(1, value.getMonths());
            out.writeInt(2, value.getDays());
            out.writeRemainder(null);
            }

        @Override
        public Period deserialize(PofReader in) throws IOException
            {
            int nYears  = in.readInt(0);
            int nMonths = in.readInt(1);
            int nDays   = in.readInt(2);
            in.readRemainder();

            return Period.of(nYears, nMonths, nDays);
            }
        }

    /**
     * Year serializer.
     */
    public static class YearSerializer
            implements PofSerializer<Year>
        {
        @Override
        public void serialize(PofWriter out, Year value) throws IOException
            {
            out.writeInt(0, value.getValue());
            out.writeRemainder(null);
            }

        @Override
        public Year deserialize(PofReader in) throws IOException
            {
            int nYear = in.readInt(0);
            in.readRemainder();

            return Year.of(nYear);
            }
        }

    /**
     * YearMonth serializer.
     */
    public static class YearMonthSerializer
            implements PofSerializer<YearMonth>
        {
        @Override
        public void serialize(PofWriter out, YearMonth value) throws IOException
            {
            out.writeInt(0, value.getYear());
            out.writeInt(1, value.getMonthValue());
            out.writeRemainder(null);
            }

        @Override
        public YearMonth deserialize(PofReader in) throws IOException
            {
            int nYear  = in.readInt(0);
            int nMonth = in.readInt(1);
            in.readRemainder();

            return YearMonth.of(nYear, nMonth);
            }
        }

    /**
     * ZoneId serializer.
     */
    public static class ZoneIdSerializer
            implements PofSerializer<ZoneId>
        {
        @Override
        public void serialize(PofWriter out, ZoneId value) throws IOException
            {
            out.writeString(0, value.getId());
            out.writeRemainder(null);
            }

        @Override
        public ZoneId deserialize(PofReader in) throws IOException
            {
            String sId = in.readString(0);
            in.readRemainder();

            return ZoneId.of(sId);
            }
        }

    /**
     * ZoneOffset serializer.
     */
    public static class ZoneOffsetSerializer
            implements PofSerializer<ZoneOffset>
        {
        @Override
        public void serialize(PofWriter out, ZoneOffset value)
                throws IOException
            {
            int nOffsetSecs = value.getTotalSeconds();
            int nOffsetByte = nOffsetSecs % 900 == 0
                              ? nOffsetSecs / 900  // compress to -72 to +72
                              : 127;
            out.writeByte(0, (byte) nOffsetByte);
            if (nOffsetByte == 127)
                {
                out.writeInt(1, nOffsetSecs);
                }
            out.writeRemainder(null);
            }

        @Override
        public ZoneOffset deserialize(PofReader in) throws IOException
            {
            int nOffsetByte = in.readByte(0);
            ZoneOffset of = nOffsetByte == 127
                            ? ZoneOffset.ofTotalSeconds(in.readInt(1))
                            : ZoneOffset.ofTotalSeconds(nOffsetByte * 900);
            in.readRemainder();

            return of;
            }
        }
    }
