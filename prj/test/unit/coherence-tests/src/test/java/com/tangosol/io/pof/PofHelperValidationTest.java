/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.io.pof;


import org.junit.Test;

import static org.junit.Assert.*;


/**
* Unit tests of various PofHelper POF validation methods.
*
* @author gm 2006.12.21
* @author jh 2006.12.27
*/
public class PofHelperValidationTest
        extends AbstractPofTest
    {
    @Test
    public void testCheckTypeWithException()
        {
        try
            {
            PofHelper.checkType(-100);
            fail("expected exception");
            }
        catch (IllegalStateException e)
            {
            // expected
            }
        }

    @Test
    public void testCheckType()
        {
        PofHelper.checkType(PofConstants.T_TIME);
        PofHelper.checkType(10);
        }

    @Test
    public void testCheckElementCountWithException()
        {
        try
            {
            PofHelper.checkElementCount(-5);
            fail("expected exception");
            }
        catch (IllegalStateException e)
            {
            // expected
            }
        }

    @Test
    public void testCheckElementCount()
        {
        PofHelper.checkElementCount(5);
        }

    @Test
    public void testCheckReferenceRange()
        {
        PofHelper.checkReferenceRange(5);
        try
            {
            PofHelper.checkReferenceRange(-5);
            fail("expected exception");
            }
        catch (IllegalStateException e)
            {
            // expected
            }
        }

    @Test
    public void testCheckYear()
        {
        PofHelper.checkDate(11, 11, 11);
        }

    @Test
    public void testCheckMonth()
        {
        try
            {
            PofHelper.checkDate(2000, 13, 11);
            fail("expected exception");
            }
        catch (IllegalStateException e)
            {
            // expected
            }
        }

    @Test
    public void testCheckDay()
        {
        try
            {
            PofHelper.checkDate(2000, 11, 32);
            fail("expected exception");
            }
        catch (IllegalStateException e)
            {
            // expected
            }
        }

    @Test
    public void testCheckLeapYear()
        {
        try
            {
            PofHelper.checkDate(2001, 2, 29);
            fail("expected exception");
            }
        catch (IllegalStateException e)
            {
            // expected
            }
        }

    @Test
    public void testCheckDate()
        {
        PofHelper.checkDate(2000, 11, 11);
        }

    @Test
    public void testCheckHour()
        {
        try
            {
            PofHelper.checkTime(25, 34, 10, 10);
            fail("expected exception");
            }
        catch (IllegalStateException e)
            {
            // expected
            }
        }

    @Test
    public void testCheckHour24()
        {
        try
            {
            PofHelper.checkTime(24, 0, 0, 0);
            fail("expected exception");
            }
        catch (IllegalStateException e)
            {
            // expected
            }
        }

    @Test
    public void testCheckMin()
        {
        try
            {
            PofHelper.checkTime(5, 98, 10, 10);
            fail("expected exception");
            }
        catch (IllegalStateException e)
            {
            // expected
            }
        }

    @Test
    public void testCheckSec()
        {
        try
            {
            PofHelper.checkTime(5, 5, 100, 10);
            fail("expected exception");
            }
        catch (IllegalStateException e)
            {
            // expected
            }
        }

    @Test
    public void testCheckSec1()
        {
        try
            {
            PofHelper.checkTime(5, 5, 60, 10);
            fail("expected exception");
            }
        catch (IllegalStateException e)
            {
            // expected
            }
        }

    @Test
    public void testCheckSec2()
        {
        try
            {
            PofHelper.checkTime(5, 5, 60, 02);
            fail("expected exception");
            }
        catch (IllegalStateException e)
            {
            // expected
            }
        }

    @Test
    public void testCheckNSec()
        {
        try
            {
            PofHelper.checkTime(5, 5, 5, -5);
            fail("expected exception");
            }
        catch (IllegalStateException e)
            {
            // expected
            }
        }

    @Test
    public void testCheckTime()
        {
        PofHelper.checkTime(5, 5, 5, 5);
        }

    @Test
    public void testCheckTimeZoneWithHourException()
        {
        PofHelper.checkTimeZone(7, 0);
        try
            {
            PofHelper.checkTimeZone(25, 0);
            fail("expected exception");
            }
        catch (IllegalStateException e)
            {
            // expected
            }
        }

    @Test
    public void testCheckTimeZoneWithMinuteException()
        {
        try
            {
            PofHelper.checkTimeZone(7, 60);
            fail("expected exception");
            }
        catch (IllegalStateException e)
            {
            // expected
            }
        }

    @Test
    public void testCheckYearMonthInterval()
        {
        PofHelper.checkYearMonthInterval(1, 0);
        PofHelper.checkYearMonthInterval(0, 5);
        try
            {
            PofHelper.checkYearMonthInterval(0, 15);
            fail("expected exception");
            }
        catch (IllegalStateException e)
            {
            // expected
            }
        }

    @Test
    public void testCheckTimeInterval()
        {
        PofHelper.checkTimeInterval(9, 9, 9, 10);
        PofHelper.checkTimeInterval(0, 9, 9, 10);
        PofHelper.checkTimeInterval(0, 0, 9, 10);
        PofHelper.checkTimeInterval(0, 0, 0, 10);
        }

    @Test
    public void testCheckDayTimeInterval()
        {
        PofHelper.checkDayTimeInterval(0, 9, 9, 9, 10);
        PofHelper.checkDayTimeInterval(1, 9, 9, 9, 10);
        }

    @Test
    public void testFormatDate()
        {
        PofHelper.formatDate(2006, 11, 11);
        }
    }
