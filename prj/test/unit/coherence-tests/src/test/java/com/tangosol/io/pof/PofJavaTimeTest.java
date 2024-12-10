/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof;

import com.tangosol.internal.io.pof.JavaTimeSupport;

import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;

import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.MonthDay;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.Assert.assertEquals;


public class PofJavaTimeTest
    {
    private SimplePofContext m_ctx;

    @Before
    public void setup()
        {
        m_ctx = new SimplePofContext();
        m_ctx.registerUserType(1001, java.time.Duration.class, new JavaTimeSupport.DurationSerializer());
        m_ctx.registerUserType(1002, java.time.Instant.class, new JavaTimeSupport.InstantSerializer());
        m_ctx.registerUserType(1003, java.time.MonthDay.class, new JavaTimeSupport.MonthDaySerializer());
        m_ctx.registerUserType(1004, java.time.Period.class, new JavaTimeSupport.PeriodSerializer());
        m_ctx.registerUserType(1005, java.time.Year.class, new JavaTimeSupport.YearSerializer());
        m_ctx.registerUserType(1006, java.time.YearMonth.class, new JavaTimeSupport.YearMonthSerializer());
        m_ctx.registerUserType(1007, java.time.ZoneId.class, new JavaTimeSupport.ZoneIdSerializer());
        m_ctx.registerUserType(1008, java.time.ZoneOffset.class, new JavaTimeSupport.ZoneOffsetSerializer());
        }

    @Test
    public void testJavaTime()
        {
        Duration   dr     = Duration.ofMinutes(10);
        Instant    inst   = Instant.now();
        MonthDay   md     = MonthDay.now();
        Period     period = Period.ofDays(2);
        Year       year   = Year.now();
        YearMonth  ym     = YearMonth.now();
        ZoneId     zoneId = ZoneId.systemDefault();
        ZoneOffset zOff   = ZoneOffset.ofHours(3);

        Binary bin = ExternalizableHelper.toBinary(dr, m_ctx);
        assertEquals(dr, ExternalizableHelper.fromBinary(bin, m_ctx));

        bin = ExternalizableHelper.toBinary(inst, m_ctx);
        assertEquals(inst, ExternalizableHelper.fromBinary(bin, m_ctx));

        bin = ExternalizableHelper.toBinary(md, m_ctx);
        assertEquals(md, ExternalizableHelper.fromBinary(bin, m_ctx));

        bin = ExternalizableHelper.toBinary(period, m_ctx);
        assertEquals(period, ExternalizableHelper.fromBinary(bin, m_ctx));

        bin = ExternalizableHelper.toBinary(year, m_ctx);
        assertEquals(year, ExternalizableHelper.fromBinary(bin, m_ctx));

        bin = ExternalizableHelper.toBinary(ym, m_ctx);
        assertEquals(ym, ExternalizableHelper.fromBinary(bin, m_ctx));

        // Note: zoneId is of type java.time.ZoneRegion, which is not a public class
        //       so, serialization on zoneId (java.time.ZoneRegion) fails.
        bin = ExternalizableHelper.toBinary(zoneId.getId(), m_ctx);
        assertEquals(zoneId.getId(), ExternalizableHelper.fromBinary(bin, m_ctx));

        bin = ExternalizableHelper.toBinary(zOff, m_ctx);
        assertEquals(zOff, ExternalizableHelper.fromBinary(bin, m_ctx));
        }
    }
