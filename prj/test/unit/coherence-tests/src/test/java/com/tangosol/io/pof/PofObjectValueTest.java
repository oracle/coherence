/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof;


import com.tangosol.util.Base;
import com.tangosol.util.ExternalizableHelper;

import java.io.IOException;

import java.math.BigDecimal;
import java.math.BigInteger;

import java.sql.Timestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;


/**
* Unit tests of basic POF object serialization/deserialization.
*
* @author gm 2006.12.21
* @author jh 2006.12.27
*/
public class PofObjectValueTest
        extends AbstractPofTest
    {
    @Test
    public void testString()
            throws IOException
        {
        initPOFWriter();
        m_writer.writeString(0, "");
        m_writer.writeString(0, "test");
        m_writer.writeString(0, "Test");
        m_writer.writeString(0, null);
        m_writer.writeString(0, " Test@");

        char[] ach = new char[]{'a', Character.MIN_VALUE};
        m_writer.writeCharArray(0, ach);

        Object[] ao = new Object[]{'a', 'b', '@'};
        m_writer.writeObjectArray(0, ao);

        // as object
        m_writer.writeObject(0, "test ");
        m_writer.writeObject(0, "");

        initPOFReader();
        assertEquals(m_reader.readString(0), "");
        assertEquals(m_reader.readString(0), "test");
        assertEquals(m_reader.readString(0), "Test");
        assertEquals(m_reader.readString(0), null);
        assertEquals(m_reader.readString(0), " Test@");
        assertEquals(m_reader.readString(0), new String(ach));
        assertEquals("ab@", m_reader.readString(0));

        // as object
        assertEquals("test ", m_reader.readObject(0));
        assertEquals("", m_reader.readObject(0));
        }

    @Test
    public void testReadStringWithException()
            throws IOException
        {
        initPOFWriter();
        m_writer.writeDouble(0, Double.NEGATIVE_INFINITY);

        initPOFReader();
        try
            {
            m_reader.readString(0);
            fail("expected exception");
            }
        catch (IOException e)
            {
            // expected
            }
        }

    @Test
    public void testRawDate()
            throws IOException
        {
        RawDate rd1 = new RawDate(2006, 12, 2);
        RawDate rd2 = new RawDate(1978, 4, 25);

        initPOFWriter();
        m_writer.writeRawDate(0, rd1);
        m_writer.writeObject(0, rd2);

        initPOFReader();
        RawDate rd = m_reader.readRawDate(0);
        assertEquals(rd.getDay(), rd1.getDay());
        assertEquals(rd.getMonth(), rd1.getMonth());
        assertEquals(rd.getYear(), rd1.getYear());

        rd = m_reader.readRawDate(0);
        assertEquals(rd.getDay(), rd2.getDay());
        assertEquals(rd.getMonth(), rd2.getMonth());
        assertEquals(rd.getYear(), rd2.getYear());
        }

    @Test
    public void testRawTime()
            throws IOException
        {
        // utc
        RawTime rtUtc = new RawTime(8, 51, 15, 100, true);
        // no utc
        RawTime rtNoUtc = new RawTime(9, 53, 20, 50, false);
        // offset
        RawTime rt1 = new RawTime(10, 5, 6, 101, 5, 30);
        RawTime rt2 = new RawTime(10, 6, 50, 0, -1, 0);
        RawTime rt3 = new RawTime(11, 10, 30, 22, 2, 0);

        initPOFWriter();
        m_writer.writeRawTime(0, rtUtc);
        m_writer.writeRawTime(0, rtNoUtc);
        m_writer.writeRawTime(0, rt1);
        m_writer.writeRawTime(0, rt2);
        m_writer.writeObject(0, rt3);

        initPOFReader();
        // utc
        RawTime rt = m_reader.readRawTime(0);
        assertEquals(rt.getHour(), rtUtc.getHour());
        assertEquals(rt.getMinute(), rtUtc.getMinute());
        assertEquals(rt.getSecond(), rtUtc.getSecond());
        assertEquals(rt.getNano(), rtUtc.getNano());
        assertEquals(rt.hasTimezone(), rtUtc.hasTimezone());
        assertEquals(rt.isUTC(), rtUtc.isUTC());

        // no utc
        rt = m_reader.readRawTime(0);
        assertEquals(rt.getHour(), rtNoUtc.getHour());
        assertEquals(rt.getMinute(), rtNoUtc.getMinute());
        assertEquals(rt.getSecond(), rtNoUtc.getSecond());
        assertEquals(rt.getNano(), rtNoUtc.getNano());
        assertEquals(rt.hasTimezone(),
                rtNoUtc.hasTimezone());
        assertEquals(rt.isUTC(), rtNoUtc.isUTC());

        rt = m_reader.readRawTime(0);
        assertEquals(rt, rt1);

        rt = m_reader.readRawTime(0);
        assertEquals(rt, rt2);

        rt = m_reader.readRawTime(0);
        assertEquals(rt, rt3);
        }

    @Test
    public void testRawTimeBadHour()
            throws IOException
        {
        try
            {
            new RawTime(100, 51, 15, 100, true);
            fail("expected exception");
            }
        catch (IllegalStateException e)
            {
            // expected
            }
        }

    @Test
    public void testRawTimeBadMinute()
            throws IOException
        {
        try
            {
            new RawTime(100, 51, 15, 100, true);
            fail("expected exception");
            }
        catch (IllegalStateException e)
            {
            // expected
            }
        }

    @Test
    public void testRawTimeBadSec()
            throws IOException
        {
        try
            {
            new RawTime(10, -12, 15, 100, true);
            fail("expected exception");
            }
        catch (IllegalStateException e)
            {
            // expected
            }
        }

    @Test
    public void testRawTimeException()
            throws IOException
        {
        try
            {
            new RawTime(10, 51, 1000, 100, true);
            fail("expected exception");
            }
        catch (IllegalStateException e)
            {
            // expected
            }
        }

    @Test
    public void testRawDateTime()
            throws IOException
        {
        RawDate rd = new RawDate(2006, 12, 2);
        // utc
        RawTime rtUtc = new RawTime(8, 51, 15, 100, true);
        // no utc
        RawTime rtNoUtc = new RawTime(9, 53, 20, 50, false);
        // offset
        RawTime rt1 = new RawTime(10, 5, 6, 101, 5, 30);

        RawDateTime rdtime1 = new RawDateTime(rd, rtUtc);
        RawDateTime rdtime2 = new RawDateTime(rd, rtNoUtc);
        RawDateTime rdtime3 = new RawDateTime(rd, rt1);

        initPOFWriter();
        m_writer.writeRawDateTime(0, rdtime1);
        m_writer.writeRawDateTime(0, rdtime2);
        m_writer.writeRawDateTime(0, rdtime3);

        initPOFReader();
        RawDateTime result = m_reader.readRawDateTime(0);
        assertEquals(result, rdtime1);

        result = m_reader.readRawDateTime(0);
        assertEquals(result, rdtime2);

        result = m_reader.readRawDateTime(0);
        assertEquals(result, rdtime3);
        }

    @Test
    public void testRawDayTimeInterval()
            throws IOException
        {
        RawDayTimeInterval rdti1 = new RawDayTimeInterval(2, 3, 4, 5, 6);
        RawDayTimeInterval rdti2 = new RawDayTimeInterval(0, 1, 0, 2, 0);

        initPOFWriter();
        m_writer.writeRawDayTimeInterval(0, rdti1);
        m_writer.writeObject(0, rdti2);

        initPOFReader();
        RawDayTimeInterval rdti = m_reader.readRawDayTimeInterval(0);
        assertEquals(rdti.getDays(), rdti1.getDays());
        assertEquals(rdti.getHours(), rdti1.getHours());
        assertEquals(rdti.getMinutes(), rdti1.getMinutes());
        assertEquals(rdti.getSeconds(), rdti1.getSeconds());
        assertEquals(rdti.getNanos(), rdti1.getNanos());
        assertEquals(rdti1, rdti);

        rdti = m_reader.readRawDayTimeInterval(0);
        assertEquals(rdti.getDays(), rdti2.getDays());
        assertEquals(rdti.getHours(), rdti2.getHours());
        assertEquals(rdti.getMinutes(), rdti2.getMinutes());
        assertEquals(rdti.getSeconds(), rdti2.getSeconds());
        assertEquals(rdti.getNanos(), rdti2.getNanos());
        assertNotEquals(rdti1, rdti);
        }

    @Test
    public void testTimeInterval()
            throws IOException
        {
        RawTimeInterval rti1 = new RawTimeInterval(1, 2, 3, 1000);
        RawTimeInterval rti2 = new RawTimeInterval(2, 8, 11, 1111);

        initPOFWriter();
        m_writer.writeRawTimeInterval(0, rti1);
        m_writer.writeObject(0, rti2);

        initPOFReader();
        RawTimeInterval rti = m_reader.readRawTimeInterval(0);
        assertEquals(rti.getHours(), rti1.getHours());
        assertEquals(rti.getMinutes(), rti1.getMinutes());
        assertEquals(rti.getSeconds(), rti1.getSeconds());
        assertEquals(rti.getNanos(), rti1.getNanos());

        rti = m_reader.readRawTimeInterval(0);
        assertEquals(rti.getHours(), rti2.getHours());
        assertEquals(rti.getMinutes(), rti2.getMinutes());
        assertEquals(rti.getSeconds(), rti2.getSeconds());
        assertEquals(rti.getNanos(), rti2.getNanos());
        }

    @Test
    public void testYearMonthInterval()
            throws IOException
        {
        RawYearMonthInterval rymi1 = new RawYearMonthInterval(4, 7);
        RawYearMonthInterval rymi2 = new RawYearMonthInterval(1, 2);

        initPOFWriter();
        m_writer.writeRawYearMonthInterval(0, rymi1);
        m_writer.writeObject(0, rymi2);

        initPOFReader();
        RawYearMonthInterval rymi = m_reader.readRawYearMonthInterval(0);
        assertEquals(rymi.getYears(), rymi1.getYears());
        assertEquals(rymi.getMonths(), rymi1.getMonths());

        rymi = m_reader.readRawYearMonthInterval(0);
        assertEquals(rymi.getYears(), rymi2.getYears());
        assertEquals(rymi.getMonths(), rymi2.getMonths());
        }

    @Test
    public void testDateTime()
            throws IOException
        {
        LocalDate      ld     = LocalDate.of(1974, 8, 24);
        LocalDateTime  ldt    = LocalDateTime.of(2004, 8, 14, 7, 41, 0);
        LocalTime      lt     = LocalTime.of(19, 15);
        OffsetDateTime odt    = OffsetDateTime.of(2004, 8, 14, 7, 41, 0, 0, ZoneOffset.ofHours(-6));
        OffsetTime     ot     = OffsetTime.of(7, 41, 0, 0, ZoneOffset.ofHours(-6));
        ZonedDateTime  zdt    = ZonedDateTime.of(2004, 8, 14, 7, 41, 0, 0, ZoneOffset.ofHours(-6));;

        Date           dt     = new Date(106, 11, 12, 22, 9, 11);
        Date           dtZone = new Date(78, 3, 25, 7, 5, 10);
        Date           dtUtc  = new Date(Date.UTC(78, 3, 25, 7, 5, 10));

        java.sql.Date  d      = java.sql.Date.valueOf(ld);
        java.sql.Time  t1     = java.sql.Time.valueOf(lt);
        java.sql.Time  t2     = java.sql.Time.valueOf(ot.toLocalTime());
        Timestamp      ts1    = Timestamp.valueOf(ldt);
        Timestamp      ts2    = Timestamp.from(odt.toInstant());

        initPOFWriter();
        assertFalse(m_ctx.isPreferJavaTime());

        m_writer.writeDateTime(0, dt);
        m_writer.writeDate(0, dt);
        m_writer.writeDateTime(0, dtZone);
        m_writer.writeDateTimeWithZone(0, dtZone);
        m_writer.writeDateTime(0, dtUtc);
        m_writer.writeDateTimeWithZone(0, dtUtc);

        m_writer.writeDate(0, ld);
        m_writer.writeDateTime(0, ldt);
        m_writer.writeTime(0, lt);
        m_writer.writeDateTimeWithZone(0, odt);
        m_writer.writeTimeWithZone(0, ot);
        m_writer.writeDateTimeWithZone(0, zdt);

        m_writer.writeDate(0, d);
        m_writer.writeTime(0, t1);
        m_writer.writeDateTime(0, ts1);

        // as object
        m_writer.writeObject(0, ld);
        m_writer.writeObject(0, lt);
        m_writer.writeObject(0, ot);
        m_writer.writeObject(0, ldt);
        m_writer.writeObject(0, odt);

        m_writer.writeObject(0, RawDate.from(ld));
        m_writer.writeObject(0, RawTime.from(lt));
        m_writer.writeObject(0, RawTime.from(ot));
        m_writer.writeObject(0, RawDateTime.from(ldt));
        m_writer.writeObject(0, RawDateTime.from(odt));


        initPOFReader();
        assertEquals(dt, m_reader.readDate(0));
        assertEquals(dt.getDate(), m_reader.readDate(0).getDate());
        assertEquals(dtZone, m_reader.readDate(0));
        assertEquals(dtZone, m_reader.readDate(0));
        assertEquals(dtUtc, m_reader.readDate(0));
        assertEquals(dtUtc, m_reader.readDate(0));

        assertEquals(ld,  m_reader.readLocalDate(0));
        assertEquals(ldt, m_reader.readLocalDateTime(0));
        assertEquals(lt,  m_reader.readLocalTime(0));
        assertEquals(odt, m_reader.readOffsetDateTime(0));
        assertEquals(ot,  m_reader.readOffsetTime(0));
        assertEquals(zdt, m_reader.readZonedDateTime(0));

        assertEquals(d,   m_reader.readRawDate(0).toSqlDate());
        assertEquals(t1,  m_reader.readRawTime(0).toSqlTime());
        assertEquals(ts1, m_reader.readRawDateTime(0).toSqlTimestamp());

        // as object
        int nPos = m_bi.getOffset();

        assertEquals(d,   m_reader.readObject(0));
        assertEquals(t1,  m_reader.readObject(0));
        assertEquals(t2,  m_reader.readObject(0));
        assertEquals(ts1, m_reader.readObject(0));
        assertEquals(ts2, m_reader.readObject(0));

        m_bi.setOffset(nPos);
        m_ctx.setPreferJavaTime(true);

        assertEquals(ld,  m_reader.readObject(0));
        assertEquals(lt,  m_reader.readObject(0));
        assertEquals(ot,  m_reader.readObject(0));
        assertEquals(ldt, m_reader.readObject(0));
        assertEquals(odt, m_reader.readObject(0));

        assertEquals(ld,  m_reader.readObject(0));
        assertEquals(lt,  m_reader.readObject(0));
        assertEquals(ot,  m_reader.readObject(0));
        assertEquals(ldt, m_reader.readObject(0));
        assertEquals(odt, m_reader.readObject(0));
        }

    /**
    * Three types of ZoneId defined in java.time.ZoneId.
    * 1. simplest is just a ZoneOffset.
    * 2. Offset-style IDs with some form of prefix, such as 'GMT+2' or 'UTC+01:00'.
    * 3. Region Type, such as "America/Los_Angeles".   (Region types are extensible and many group kinds, default std group is TZDB)
    */
    @Ignore
    @Test
    public void test_COH_16663_ZoneIdByOffset()
        throws IOException
        {
        ZonedDateTime  zdtOffset         = ZonedDateTime.of(2004, 8, 14, 7, 41, 0, 0, ZoneId.of("-07:00"));;
        ZonedDateTime  zdtPrefixedOffset = ZonedDateTime.of(2004, 8, 14, 7, 41, 0, 0, ZoneId.of("GMT-7"));;

        initPOFWriter();
        m_writer.writeDateTimeWithZone(0, zdtOffset);
        m_writer.writeDateTimeWithZone(0, zdtPrefixedOffset);

        initPOFReader();
        assertEquals(zdtOffset, m_reader.readZonedDateTime(0));          // works
        assertEquals(zdtPrefixedOffset, m_reader.readZonedDateTime(0));  // fails
        }

    @Ignore
    @Test
    public void test_COH_16663_ZoneIdByRegion()
        throws IOException
        {
        ZonedDateTime  zdt = ZonedDateTime.of(2004, 8, 14, 7, 41, 0, 0, ZoneId.of("PST", ZoneId.SHORT_IDS));;

        initPOFWriter();
        m_writer.writeDateTimeWithZone(0, zdt);

        initPOFReader();
        assertEquals(zdt, m_reader.readZonedDateTime(0));
        }


    @Test
    public void test_COH_11890()
        {
        initPOFWriter();

        Collection<Date> colDate = new HashSet<>();
        colDate.add(new Date(0));
        colDate.add(new Date(1));
        colDate.add(new Date(-1));
        colDate.add(new Date(-1000));
        colDate.add(new Timestamp(0));
        colDate.add(new Timestamp(1));
        colDate.add(new Timestamp(-1));
        colDate.add(new Timestamp(-1000));
        Timestamp ts = new Timestamp(-1);
        ts.setNanos(999999999);
        colDate.add(ts);
        for (Date dt : colDate)
            {
            assertEquals(dt, ExternalizableHelper.fromBinary(ExternalizableHelper.toBinary(dt, m_ctx), m_ctx));
            }
        }

    @Test
    public void testObject()
            throws IOException
        {
        testPrimitiveTypes();
        testArrayTypes();
        testCollectionTypes();
        }

    private void testPrimitiveTypes() throws IOException
        {
        initPOFWriter();
        m_writer.writeObject(0, null);
        m_writer.writeObject(0, Boolean.FALSE);
        m_writer.writeObject(0, true);
        m_writer.writeObject(0, 'a');
        m_writer.writeObject(0, Byte.MIN_VALUE);
        m_writer.writeObject(0, Short.MAX_VALUE);
        m_writer.writeObject(0, 0);
        m_writer.writeObject(0, -1);
        m_writer.writeObject(0, Integer.MAX_VALUE);
        m_writer.writeObject(0, (long) -1);
        m_writer.writeObject(0, Long.MAX_VALUE);
        m_writer.writeObject(0, (float) 0);
        m_writer.writeObject(0, Float.NEGATIVE_INFINITY);
        m_writer.writeObject(0, Double.MAX_VALUE);

        initPOFReader();
        assertNull(m_reader.readObject(0));
        assertEquals(false, m_reader.readObject(0));
        assertEquals(true, m_reader.readObject(0));
        assertEquals((Character) 'a', m_reader.readObject(0));
        assertEquals((Byte) Byte.MIN_VALUE, m_reader.readObject(0));
        assertEquals((Short) Short.MAX_VALUE, m_reader.readObject(0));
        assertEquals((Integer) 0, m_reader.readObject(0));
        assertEquals((Integer) (-1), m_reader.readObject(0));
        assertEquals((Integer) Integer.MAX_VALUE, m_reader.readObject(0));
        assertEquals((Long) (-1L), m_reader.readObject(0));
        assertEquals((Long) Long.MAX_VALUE, m_reader.readObject(0));
        assertEquals((Float) 0f, m_reader.readObject(0));
        assertEquals((Float) Float.NEGATIVE_INFINITY, m_reader.readObject(0));
        assertEquals((Double) Double.MAX_VALUE, m_reader.readObject(0));
        }

    private void testArrayTypes() throws IOException
        {
        boolean[] af   = new boolean[] {true, true};
        byte[]    ab   = new byte[]    {1, 2, 3};
        char[]    ach  = new char[]    {'a', 'd', Character.MIN_VALUE};
        short[]   an   = new short[]   {100, 200, Short.MIN_VALUE};
        int[]     an2  = new int[]     {100, 200, Integer.MIN_VALUE};
        long[]    al   = new long[]    {100, 200, Long.MIN_VALUE};
        float[]   afl  = new float[]   {Float.MIN_VALUE, Float.NEGATIVE_INFINITY, 0.01f};
        double[]  adfl = new double[]  {Double.MIN_VALUE, Double.NEGATIVE_INFINITY, 0.0001};
        String[]  as   = new String[]  {"abc", "xyz"};
        Object[]  ao   = new Object[]  {true, 22.2211};

        initPOFWriter();
        m_writer.writeObject(0, af);
        m_writer.writeObject(0, ab);
        m_writer.writeObject(0, ach);
        m_writer.writeObject(0, an);
        m_writer.writeObject(0, an2);
        m_writer.writeObject(0, al);
        m_writer.writeObject(0, afl);
        m_writer.writeObject(0, adfl);
        m_writer.writeObject(0, as);
        m_writer.writeObject(0, ao);

        initPOFReader();
        Object o = m_reader.readObject(0);
        assertTrue(o instanceof boolean[]);
        for (int i = 0; i < ((boolean[]) o).length; i++)
            {
            assertEquals(af[i], ((boolean[]) o)[i]);
            }

        assertArrayEquals(ab, m_reader.readObject(0));
        assertArrayEquals(ach, m_reader.readObject(0));
        assertArrayEquals(an, m_reader.readObject(0));
        assertArrayEquals(an2, m_reader.readObject(0));
        assertArrayEquals(al, m_reader.readObject(0));
        assertArrayEquals(afl, m_reader.readObject(0), 0.01f);
        assertArrayEquals(adfl, m_reader.readObject(0), 0.01d);
        assertArrayEquals(as, m_reader.readObject(0));
        assertArrayEquals(ao, m_reader.readObject(0));
        }

    private void testCollectionTypes() throws IOException
        {
        initPOFWriter();
        List<Object> list = Arrays.asList(true, 5);
        m_writer.writeObject(0, list);

        initPOFReader();
        assertEquals(list, m_reader.readObject(0));
        }


    @Test
    public void testBigInteger()
            throws IOException
        {
        BigInteger n  = new BigInteger("1234567898765432154123978000");
        BigInteger n2 = new BigInteger("83405983458093485083408503498532801");

        initPOFWriter();
        m_writer.writeBigInteger(0, n);
        m_writer.writeBigInteger(0, BigInteger.ONE);
        m_writer.writeBigInteger(0, BigInteger.ZERO);
        m_writer.writeBigInteger(0, null);
        m_writer.writeObject(0, n2);

        initPOFReader();
        BigInteger result = m_reader.readBigInteger(0);
        assertEquals(result, n);
        assertEquals(m_reader.readBigInteger(0), BigInteger.ONE);
        assertEquals(m_reader.readBigInteger(0), BigInteger.ZERO);
        assertEquals(m_reader.readBigInteger(0), null);
        assertEquals(m_reader.readBigInteger(0), n2);

        initPOFWriter();
        n = new BigInteger(Base.dup('9', 39));
        try
            {
            m_writer.writeBigInteger(0, n);
            fail("Expected IllegalStateException");
            }
        catch (IllegalStateException e)
            {
            // expected
            }
        }

    @Test
    public void testBigDecimal()
            throws IOException
        {
        BigInteger n    = new BigInteger("834053408503498532801");
        BigDecimal dec1 = new BigDecimal(n);
        BigDecimal dec2 = new BigDecimal("+12131434534534.0000000000777e-101");

        initPOFWriter();
        m_writer.writeBigDecimal(0, dec1);
        m_writer.writeBigDecimal(0, dec2);
        m_writer.writeBigDecimal(0, null);
        m_writer.writeObject(0, dec1);

        initPOFReader();
        assertEquals(m_reader.readBigDecimal(0), dec1);
        assertEquals(m_reader.readBigDecimal(0), dec2);
        assertEquals(m_reader.readBigDecimal(0), null);
        assertEquals(m_reader.readBigDecimal(0), dec1);

        initPOFWriter();
        dec1 = new BigDecimal(new BigInteger(Base.dup('9', 39)), 0);
        try
            {
            m_writer.writeBigDecimal(0, dec1);
            fail("Expected IllegalStateException");
            }
        catch (IllegalStateException e)
            {
            // expected
            }
        }
    }
