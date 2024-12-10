/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.run.xml;


import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;


/**
* Tests for SimpleValue type conversions
*
* @author pp 2009.12.8
*/
public class SimpleValueTest
    {
    // ----- boolean --------------------------------------------------------

    @Test
    public void testBoolean()
        {
        SimpleValue valueTrue = new SimpleValue("T", false, false);
        Assert.assertTrue(valueTrue.getBoolean());

        SimpleValue valueFalse = new SimpleValue("F", false, false);
        Assert.assertFalse(valueFalse.getBoolean());

        SimpleValue invalidValue = new SimpleValue("76", false, false);
        Assert.assertFalse(invalidValue.getBoolean());

        SimpleValue emptyValue = new SimpleValue("", false, false);
        Assert.assertFalse(emptyValue.getBoolean());
        }

    // ----- int ------------------------------------------------------------

    @Test
    public void testInteger()
        {
        SimpleValue invalidValue = new SimpleValue("foo", false, false);
        Assert.assertEquals(0, invalidValue.getInt());

        SimpleValue intValue = new SimpleValue("1", false, false);
        Assert.assertEquals(1, intValue.getInt());

        SimpleValue emptyValue = new SimpleValue("", false, false);
        Assert.assertEquals(0, emptyValue.getInt());
        }

    // ----- long -----------------------------------------------------------

    @Test
    public void testLong()
        {
        SimpleValue invalidValue = new SimpleValue("foo", false, false);
        Assert.assertEquals(0, invalidValue.getLong());

        SimpleValue longValue = new SimpleValue("1", false, false);
        Assert.assertEquals(1, longValue.getLong());

        SimpleValue emptyValue = new SimpleValue("", false, false);
        Assert.assertEquals(0, emptyValue.getLong());
        }

    // ----- double ---------------------------------------------------------

    @Test
    public void testDouble()
        {
        SimpleValue invalidValue = new SimpleValue("foo", false, false);
        Assert.assertEquals(0.0, invalidValue.getDouble(), 0.0);

        SimpleValue doubleValue = new SimpleValue("1.1", false, false);
        Assert.assertEquals(1.1, doubleValue.getDouble(), 0.0);

        SimpleValue emptyValue = new SimpleValue("", false, false);
        Assert.assertEquals(0.0, emptyValue.getDouble(), 0.0);
        }

    // ----- decimal --------------------------------------------------------

    @Test
    public void testDecimal()
        {
        SimpleValue invalidValue = new SimpleValue("foo", false, false);
        Assert.assertEquals(new BigDecimal("0"), invalidValue.getDecimal());

        SimpleValue decimalValue = new SimpleValue("1.1", false, false);
        Assert.assertEquals(new BigDecimal("1.1"), decimalValue.getDecimal());

        SimpleValue emptyValue = new SimpleValue("", false, false);
        Assert.assertEquals(new BigDecimal("0"), emptyValue.getDecimal());
        }

    // ----- date -----------------------------------------------------------

    @Test
    public void testDate()
        {
        SimpleValue invalidValue = new SimpleValue("foo", false, false);
        Assert.assertEquals(new Date(0), invalidValue.getDate());

        SimpleValue dateValue = new SimpleValue("2009-12-09", false, false);
        Assert.assertEquals(Date.valueOf("2009-12-09"), dateValue.getDate());

        SimpleValue emptyValue = new SimpleValue("", false, false);
        Assert.assertEquals(new Date(0), emptyValue.getDate());
        }

    // ----- time -----------------------------------------------------------

    @Test
    public void testTime()
        {
        SimpleValue invalidValue = new SimpleValue("foo", false, false);
        Assert.assertEquals(new Time(0), invalidValue.getTime());

        SimpleValue dateValue = new SimpleValue("11:48:00", false, false);
        Assert.assertEquals(Time.valueOf("11:48:00"), dateValue.getTime());

        SimpleValue emptyValue = new SimpleValue("", false, false);
        Assert.assertEquals(new Time(0), emptyValue.getTime());
        }

    // ----- datetime -------------------------------------------------------

    @Test
    public void testDateTime()
        {
        SimpleValue invalidValue = new SimpleValue("foo", false, false);
        Assert.assertEquals(new Timestamp(0), invalidValue.getDateTime());

        SimpleValue dateTimeValue = new SimpleValue("2009-12-09 11:48:00",
                false, false);
        Assert.assertEquals(Timestamp.valueOf("2009-12-09 11:48:00"),
                dateTimeValue.getDateTime());

        SimpleValue emptyValue = new SimpleValue("", false, false);
        Assert.assertEquals(new Timestamp(0), emptyValue.getDateTime());
        }
    }
