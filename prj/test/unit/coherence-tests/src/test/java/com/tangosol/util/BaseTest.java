/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util;


import org.junit.Test;

import static org.junit.Assert.*;


/**
* A collection of unit tests for various methods of {@link Base}.
*
* @author jh  2005.04.15
*/
public class BaseTest
        extends Base
    {
    // ----- parseBandwidth(String s) tests ---------------------------------

    /**
    * Invoke {@link #parseBandwidth(String)} with a null argument.
    */
    @Test
    public void parseBandwidthNull()
        {
        try
            {
            parseBandwidth(null);
            fail("expected exception");
            }
        catch (IllegalArgumentException e)
            {
            // expected
            }
        }

    /**
    * Invoke {@link #parseBandwidth(String)} with an argument that is just a
    * unit.
    */
    @Test
    public void parseBandwidthInvalidUnitOnly()
        {
        try
            {
            parseBandwidth("kbps");
            fail("expected exception");
            }
        catch (NumberFormatException e)
            {
            // expected
            }
        }

    /**
    * Invoke {@link #parseBandwidth(String)} with an argument that contains an
    * invalid unit.
    */
    @Test
    public void parseBandwidthInvalidUnit1()
        {
        try
            {
            parseBandwidth("1s");
            fail("expected exception");
            }
        catch (IllegalArgumentException e)
            {
            // expected
            }
        }

    /**
    * Invoke {@link #parseBandwidth(String)} with an argument that contains an
    * invalid unit.
    */
    @Test
    public void parseBandwidthInvalidUnit2()
        {
        try
            {
            parseBandwidth("1x");
            fail("expected exception");
            }
        catch (NumberFormatException e)
            {
            // expected
            }
        }

    /**
    * Invoke {@link #parseBandwidth(String)} with an argument that contains an
    * invalid unit.
    */
    @Test
    public void parseBandwidthInvalidUnit3()
        {
        try
            {
            parseBandwidth("1xxxx");
            fail("expected exception");
            }
        catch (NumberFormatException e)
            {
            // expected
            }
        }

    /**
    * Invoke {@link #parseBandwidth(String)} with an argument that contains an
    * invalid unit.
    */
    @Test
    public void parseBandwidthInvalidUnit4()
        {
        try
            {
            parseBandwidth("1kbpsps");
            fail("expected exception");
            }
        catch (NumberFormatException e)
            {
            // expected
            }
        }

    /**
    * Invoke {@link #parseBandwidth(String)} with an argument that contains an
    * invalid unit.
    */
    @Test
    public void parseBandwidthInvalidUnit5()
        {
        try
            {
            parseBandwidth("1kbpsps");
            fail("expected exception");
            }
        catch (NumberFormatException e)
            {
            // expected
            }
        }

    /**
    * Invoke {@link #parseBandwidth(String)} with an argument that contains
    * a number with a trailing decimal point.
    */
    @Test
    public void parseBandwidthInvalidNumber1()
        {
        try
            {
            parseBandwidth("1.");
            fail("expected exception");
            }
        catch (NumberFormatException e)
            {
            // expected
            }
        }

    /**
    * Invoke {@link #parseBandwidth(String)} with an argument that contains
    * a number with two decimal points.
    */
    @Test
    public void parseBandwidthInvalidNumber2()
        {
        try
            {
            parseBandwidth("1.1.1");
            fail("expected exception");
            }
        catch (NumberFormatException e)
            {
            // expected
            }
        }

    /**
    * Invoke {@link #parseBandwidth(String, int)} with an invalid default
    * factor.
    */
    @Test
    public void parseBandwidthInvalidFactor()
        {
        try
            {
            parseBandwidth("1", 7);
            fail("expected exception");
            }
        catch (IllegalArgumentException e)
            {
            // expected
            }
        }

    /**
    * Invoke {@link #parseBandwidth(String)} with various valid arguments.
    */
    @Test
    public void parseBandwidth()
        {
        assertTrue("parseBandwidth(\"1\") != 0L", parseBandwidth("1") == 0L);
        assertTrue("parseBandwidth(\"1ps\") != 0L", parseBandwidth("1ps") == 0L);
        assertTrue("parseBandwidth(\"1.5\") != 0L", parseBandwidth("1.5") == 0L);
        assertTrue("parseBandwidth(\"8.5\") != 1L", parseBandwidth("8.5") == 1L);
        assertTrue("parseBandwidth(\"8\") != 1L", parseBandwidth("8") == 1L);
        assertTrue("parseBandwidth(\"1bps\") != 0L", parseBandwidth("1bps") == 0L);
        assertTrue("parseBandwidth(\"7bps\") != 0L", parseBandwidth("7bps") == 0L);
        assertTrue("parseBandwidth(\"8bps\") != 1L", parseBandwidth("8bps") == 1L);
        assertTrue("parseBandwidth(\"1kbps\") != 128L", parseBandwidth("1kbps") == 128L);
        assertTrue("parseBandwidth(\"1kbpS\") != 128L", parseBandwidth("1kbpS") == 128L);
        assertTrue("parseBandwidth(\"1kbPs\") != 128L", parseBandwidth("1kbPs") == 128L);
        assertTrue("parseBandwidth(\"1kbPS\") != 128L", parseBandwidth("1kbPS") == 128L);
        assertTrue("parseBandwidth(\"1kBps\") != 1024L", parseBandwidth("1kBps") == 1024L);
        assertTrue("parseBandwidth(\"1kBpS\") != 1024L", parseBandwidth("1kBpS") == 1024L);
        assertTrue("parseBandwidth(\"1kBPs\") != 1024L", parseBandwidth("1kBPs") == 1024L);
        assertTrue("parseBandwidth(\"1kBPS\") != 1024L", parseBandwidth("1kBPS") == 1024L);
        assertTrue("parseBandwidth(\"1KBps\") != 1024L", parseBandwidth("1KBps") == 1024L);
        assertTrue("parseBandwidth(\"1mbps\") != 131072L", parseBandwidth("1mbps") == 131072L);
        assertTrue("parseBandwidth(\"1mBps\") != 1048576L", parseBandwidth("1mBps") == 1048576L);
        assertTrue("parseBandwidth(\"1.25mBps\") != 1310720L", parseBandwidth("1.25mBps") == 1310720L);
        assertTrue("parseBandwidth(\"1.5mBps\") != 1572864L", parseBandwidth("1.5mBps") == 1572864L);
        assertTrue("parseBandwidth(\"10m\") != 1310720L", parseBandwidth("10m") == 1310720L);
        assertTrue("parseBandwidth(\"100m\") != 13107200L", parseBandwidth("100m") == 13107200L);
        assertTrue("parseBandwidth(\"1000m\") != 131072000L", parseBandwidth("1000m") == 131072000L);
        assertTrue("parseBandwidth(\"10mb\") != 1310720L", parseBandwidth("10mb") == 1310720L);
        assertTrue("parseBandwidth(\"100mb\") != 13107200L", parseBandwidth("100mb") == 13107200L);
        assertTrue("parseBandwidth(\"1000mb\") != 131072000L", parseBandwidth("1000mb") == 131072000L);
        assertTrue("parseBandwidth(\"1gbps\") != 134217728L", parseBandwidth("1gbps") == 134217728L);
        assertTrue("parseBandwidth(\"1gBps\") != 1073741824L", parseBandwidth("1gBps") == 1073741824L);
        assertTrue("parseBandwidth(\"1tbps\") != 137438953472L", parseBandwidth("1tbps") == 137438953472L);
        assertTrue("parseBandwidth(\"1tBps\") != 1099511627776L", parseBandwidth("1tBps") == 1099511627776L);
        assertTrue("parseBandwidth(\"2tbps\") != 274877906944L", parseBandwidth("2tbps") == 274877906944L);
        assertTrue("parseBandwidth(\"2tBps\") != 2199023255552L", parseBandwidth("2tBps") == 2199023255552L);
        }

    /**
    * Invoke {@link #parseBandwidth(String, int)} with various valid arguments.
    */
    @Test
    public void parseBandwidthPower()
        {
        assertTrue("parseBandwidth(\"8\", POWER_0) != 1L", parseBandwidth("8", POWER_0) == 1L);
        assertTrue("parseBandwidth(\"8\", POWER_K) != 1024L", parseBandwidth("8", POWER_K) == 1024L);
        assertTrue("parseBandwidth(\"8\", POWER_M) != 1048576L", parseBandwidth("8", POWER_M) == 1048576L);
        assertTrue("parseBandwidth(\"8\", POWER_G) != 1073741824L", parseBandwidth("8", POWER_G) == 1073741824L);
        assertTrue("parseBandwidth(\"8\", POWER_T) != 1099511627776L", parseBandwidth("8", POWER_T) == 1099511627776L);

        assertTrue("parseBandwidth(\"8bps\", POWER_0) != 1L", parseBandwidth("8bps", POWER_0) == 1L);
        assertTrue("parseBandwidth(\"1Bps\", POWER_0) != 1L", parseBandwidth("1Bps", POWER_0) == 1L);
        assertTrue("parseBandwidth(\"8bps\", POWER_K) != 1L", parseBandwidth("8bps", POWER_K) == 1L);
        assertTrue("parseBandwidth(\"1Bps\", POWER_K) != 1L", parseBandwidth("1Bps", POWER_K) == 1L);
        assertTrue("parseBandwidth(\"8bps\", POWER_M) != 1L", parseBandwidth("8bps", POWER_M) == 1L);
        assertTrue("parseBandwidth(\"1Bps\", POWER_M) != 1L", parseBandwidth("1Bps", POWER_M) == 1L);
        assertTrue("parseBandwidth(\"8bps\", POWER_G) != 1L", parseBandwidth("8bps", POWER_G) == 1L);
        assertTrue("parseBandwidth(\"1Bps\", POWER_G) != 1L", parseBandwidth("1Bps", POWER_G) == 1L);
        assertTrue("parseBandwidth(\"8bps\", POWER_T) != 1L", parseBandwidth("8bps", POWER_T) == 1L);
        assertTrue("parseBandwidth(\"1Bps\", POWER_T) != 1L", parseBandwidth("1Bps", POWER_T) == 1L);
        }


    // ----- parseMemorySize(String s) tests --------------------------------

    /**
    * Invoke {@link #parseMemorySize(String)} with a null argument.
    */
    @Test
    public void parseMemorySizeNull()
        {
        try
            {
            parseMemorySize(null);
            fail("expected exception");
            }
        catch (IllegalArgumentException e)
            {
            // expected
            }
        }

    /**
    * Invoke {@link #parseMemorySize(String)} with an argument that is just a
    * unit.
    */
    @Test
    public void parseMemorySizeInvalidUnitOnly()
        {
        try
            {
            parseMemorySize("k");
            fail("expected exception");
            }
        catch (NumberFormatException e)
            {
            // expected
            }
        }

    /**
    * Invoke {@link #parseMemorySize(String)} with an argument that contains an
    * invalid unit.
    */
    @Test
    public void parseMemorySizeInvalidUnit1()
        {
        try
            {
            parseMemorySize("1x");
            fail("expected exception");
            }
        catch (NumberFormatException e)
            {
            // expected
            }
        }

    /**
    * Invoke {@link #parseMemorySize(String)} with an argument that contains an
    * invalid unit.
    */
    @Test
    public void parseMemorySizeInvalidUnit2()
        {
        try
            {
            parseMemorySize("1kk");
            fail("expected exception");
            }
        catch (NumberFormatException e)
            {
            // expected
            }
        }

    /**
    * Invoke {@link #parseMemorySize(String)} with an argument that contains
    * a number with a trailing decimal point.
    */
    @Test
    public void parseMemorySizeInvalidNumber1()
        {
        try
            {
            parseMemorySize("1.M");
            fail("expected exception");
            }
        catch (NumberFormatException e)
            {
            // expected
            }
        }

    /**
    * Invoke {@link #parseMemorySize(String)} with an argument that contains
    * a number with two decimal points.
    */
    @Test
    public void parseMemorySizeInvalidNumber2()
        {
        try
            {
            parseMemorySize("1.1.1M");
            fail("expected exception");
            }
        catch (NumberFormatException e)
            {
            // expected
            }
        }

    /**
    * Invoke {@link #parseMemorySize(String, int)} with an invalid default
    * factor.
    */
    @Test
    public void parseMemorySizeInvalidFactor()
        {
        try
            {
            parseMemorySize("1", 7);
            fail("expected exception");
            }
        catch (IllegalArgumentException e)
            {
            // expected
            }
        }

    /**
    * Invoke {@link #parseMemorySize(String)} with various valid arguments.
    */
    @Test
    public void parseMemorySize()
        {
        assertTrue("parseMemorySize(\"1\") != 1L", parseMemorySize("1") == 1L);
        assertTrue("parseMemorySize(\".25\") != 0L", parseMemorySize(".25") == 0L);
        assertTrue("parseMemorySize(\"8.25\") != 8L", parseMemorySize("8.25") == 8L);
        assertTrue("parseMemorySize(\"1k\") != 1024L", parseMemorySize("1k") == 1024L);
        assertTrue("parseMemorySize(\"1K\") != 1024L", parseMemorySize("1K") == 1024L);
        assertTrue("parseMemorySize(\"1m\") != 1048576L", parseMemorySize("1m") == 1048576L);
        assertTrue("parseMemorySize(\"1M\") != 1048576L", parseMemorySize("1M") == 1048576L);
        assertTrue("parseMemorySize(\"1.25M\") != 1310720L", parseMemorySize("1.25M") == 1310720L);
        assertTrue("parseMemorySize(\"1.5M\") != 1572864L", parseMemorySize("1.5M") == 1572864L);
        assertTrue("parseMemorySize(\"1g\") != 1073741824L", parseMemorySize("1g") == 1073741824L);
        assertTrue("parseMemorySize(\"1G\") != 1073741824L", parseMemorySize("1G") == 1073741824L);
        assertTrue("parseMemorySize(\"1t\") != 1099511627776L", parseMemorySize("1t") == 1099511627776L);
        assertTrue("parseMemorySize(\"1T\") != 1099511627776L", parseMemorySize("1T") == 1099511627776L);
        assertTrue("parseMemorySize(\"2t\") != 2199023255552L", parseMemorySize("2t") == 2199023255552L);
        assertTrue("parseMemorySize(\"2T\") != 2199023255552L", parseMemorySize("2T") == 2199023255552L);
        }

    /**
    * Invoke {@link #parseMemorySize(String, int)} with various valid arguments.
    */
    @Test
    public void parseMemorySizePower()
        {
        assertTrue("parseMemorySize(\"1\", POWER_0) != 1L", parseMemorySize("1", POWER_0) == 1L);
        assertTrue("parseMemorySize(\"1\", POWER_K) != 1024L", parseMemorySize("1", POWER_K) == 1024L);
        assertTrue("parseMemorySize(\"1\", POWER_M) != 1048576L", parseMemorySize("1", POWER_M) == 1048576L);
        assertTrue("parseMemorySize(\"1\", POWER_G) != 1073741824L", parseMemorySize("1", POWER_G) == 1073741824L);
        assertTrue("parseMemorySize(\"1\", POWER_T) != 1099511627776L", parseMemorySize("1", POWER_T) == 1099511627776L);

        assertTrue("parseBandwidth(\"1B\", POWER_0) != 1L", parseBandwidth("1B", POWER_0) == 1L);
        assertTrue("parseBandwidth(\"1B\", POWER_K) != 1L", parseBandwidth("1B", POWER_K) == 1L);
        assertTrue("parseBandwidth(\"1B\", POWER_M) != 1L", parseBandwidth("1B", POWER_M) == 1L);
        assertTrue("parseBandwidth(\"1B\", POWER_G) != 1L", parseBandwidth("1B", POWER_G) == 1L);
        assertTrue("parseBandwidth(\"1B\", POWER_T) != 1L", parseBandwidth("1B", POWER_T) == 1L);
        }


    // ----- parseTime(String s) tests --------------------------------------

    /**
    * Invoke {@link #parseTime(String)} with a null argument.
    */
    @Test
    public void parseTimeNull()
        {
        try
            {
            parseTime(null);
            fail("expected exception");
            }
        catch (IllegalArgumentException e)
            {
            // expected
            }
        }

    /**
    * Invoke {@link #parseTime(String)} with a zero-length string.
    */
    @Test
    public void parseTimeInvalidEmptyString()
        {
        try
            {
            parseTime("");
            fail("expected exception");
            }
        catch (NumberFormatException e)
            {
            // expected
            }
        }

    /**
    * Invoke {@link #parseTime(String)} with an argument that contains an
    * invalid unit.
    */
    @Test
    public void parseTimeInvalidUnit1()
        {
        try
            {
            parseTime("1x");
            fail("expected exception");
            }
        catch (NumberFormatException e)
            {
            // expected
            }
        }

    /**
    * Invoke {@link #parseTime(String)} with an argument that contains an
    * invalid unit.
    */
    @Test
    public void parseTimeInvalidUnit2()
        {
        try
            {
            parseTime("1xs");
            fail("expected exception");
            }
        catch (NumberFormatException e)
            {
            // expected
            }
        }

    /**
    * Invoke {@link #parseTime(String)} with an argument that contains
    * a number with a trailing decimal point.
    */
    @Test
    public void parseTimeInvalidNumber1()
        {
        try
            {
            parseTime("1.m");
            fail("expected exception");
            }
        catch (NumberFormatException e)
            {
            // expected
            }
        }

    /**
    * Invoke {@link #parseTime(String)} with an argument that contains
    * a number with two decimal points.
    */
    @Test
    public void parseTimeInvalidNumber2()
        {
        try
            {
            parseTime("1.1.1m");
            fail("expected exception");
            }
        catch (NumberFormatException e)
            {
            // expected
            }
        }

    /**
    * Invoke {@link #parseTime(String)} with an argument that contains
    * a negative number.
    */
    @Test
    public void parseTimeInvalidNumber3()
        {
        try
            {
            parseTime("-1");
            fail("expected exception");
            }
        catch (NumberFormatException e)
            {
            // expected
            }
        }

    /**
    * Invoke {@link #parseTime(String, int)} with an invalid default unit.
    */
    @Test
    public void parseTimeInvalidUnit()
        {
        try
            {
            parseTime("1", 2);
            fail("expected exception");
            }
        catch (IllegalArgumentException e)
            {
            // expected
            }
        }

    /**
    * Invoke {@link #parseTime(String)} with various valid arguments.
    */
    @Test
    public void parseTime()
        {
        assertTrue("parseTime(\".1ns\") != 0L", parseTime(".1ns") == 0L);
        assertTrue("parseTime(\"1ns\") != 0L", parseTime("1ns") == 0L);
        assertTrue("parseTime(\"1us\") != 0L", parseTime("1us") == 0L);
        assertTrue("parseTime(\"1000000ns\") != 0L", parseTime("1000000ns") == 1L);
        assertTrue("parseTime(\"1500us\") != 1L", parseTime("1500us") == 1L);
        assertTrue("parseTime(\".6\") != 0L", parseTime(".6") == 0L);
        assertTrue("parseTime(\"1\") != 1L", parseTime("1") == 1L);
        assertTrue("parseTime(\"1500ms\") != 1500L", parseTime("1500ms") == 1500L);
        assertTrue("parseTime(\"1500Ms\") != 1500L", parseTime("1500Ms") == 1500L);
        assertTrue("parseTime(\"1500mS\") != 1500L", parseTime("1500mS") == 1500L);
        assertTrue("parseTime(\"1500MS\") != 1500L", parseTime("1500MS") == 1500L);
        assertTrue("parseTime(\".25\") != 0L", parseTime(".25") == 0L);
        assertTrue("parseTime(\"8.25\") != 8L", parseTime("8.25") == 8L);
        assertTrue("parseTime(\"1s\") != 1000L", parseTime("1s") == 1000L);
        assertTrue("parseTime(\"1S\") != 1000L", parseTime("1S") == 1000L);
        assertTrue("parseTime(\"1m\") != 60000L", parseTime("1m") == 60000L);
        assertTrue("parseTime(\"1M\") != 60000L", parseTime("1M") == 60000L);
        assertTrue("parseTime(\"1.25M\") != 75000L", parseTime("1.25M") == 75000L);
        assertTrue("parseTime(\"1.5M\") != 90000L", parseTime("1.5M") == 90000L);
        assertTrue("parseTime(\"1h\") != 3600000L", parseTime("1h") == 3600000L);
        assertTrue("parseTime(\"1.5H\") != 5400000L", parseTime("1.5H") == 5400000L);
        assertTrue("parseTime(\"1d\") != 86400000L", parseTime("1d") == 86400000L);
        assertTrue("parseTime(\"1.5D\") != 129600000L", parseTime("1.5D") == 129600000L);
        }

    /**
    * Invoke {@link #parseTimeNanos(String)} with various valid arguments.
    */
    @Test
    public void parseTimeNanos()
        {
        assertTrue("parseTimeNanos(\".1ns\") != 0L", parseTimeNanos(".1ns") == 0L);
        assertTrue("parseTimeNanos(\"1ns\") != 1L", parseTimeNanos("1ns") == 1L);
        assertTrue("parseTimeNanos(\"1us\") != 1000L", parseTimeNanos("1us") == 1000L);
        assertTrue("parseTimeNanos(\"1000000ns\") != 1000000L", parseTimeNanos("1000000ns") == 1000000L);
        assertTrue("parseTimeNanos(\".5us\") != 500L", parseTimeNanos(".5us") == 500L);
        assertTrue("parseTimeNanos(\"1000us\") != 1000000L", parseTimeNanos("1000us") == 1000000L);
        assertTrue("parseTimeNanos(\".6ms\") != 600000L", parseTimeNanos(".6ms") == 600000L);
        assertTrue("parseTimeNanos(\"1ms\") != 1000000L", parseTimeNanos("1ms") == 1000000L);
        assertTrue("parseTimeNanos(\"1500ms\") != 1500000000L", parseTimeNanos("1500ms") == 1500000000L);
        assertTrue("parseTimeNanos(\"1500Ms\") != 1500000000L", parseTimeNanos("1500Ms") == 1500000000L);
        assertTrue("parseTimeNanos(\"1500mS\") != 1500000000L", parseTimeNanos("1500mS") == 1500000000L);
        assertTrue("parseTimeNanos(\"1500MS\") != 1500000000L", parseTimeNanos("1500MS") == 1500000000L);
        assertTrue("parseTimeNanos(\".25ms\") != 250000L", parseTimeNanos(".25ms") ==  250000L);
        assertTrue("parseTimeNanos(\"8.25ms\") != 8250000L", parseTimeNanos("8.25ms") == 8250000L);
        assertTrue("parseTimeNanos(\"1s\") != 1000000000L", parseTimeNanos("1s") == 1000000000L);
        assertTrue("parseTimeNanos(\"1S\") != 1000000000L", parseTimeNanos("1S") == 1000000000L);
        assertTrue("parseTimeNanos(\"1m\") != 60000000000L", parseTimeNanos("1m") == 60000000000L);
        assertTrue("parseTimeNanos(\"1M\") != 60000000000L", parseTimeNanos("1M") == 60000000000L);
        assertTrue("parseTimeNanos(\"1.25M\") != 75000000000L", parseTimeNanos("1.25M") == 75000000000L);
        assertTrue("parseTimeNanos(\"1.5M\") != 90000000000L", parseTimeNanos("1.5M") == 90000000000L);
        assertTrue("parseTimeNanos(\"1h\") != 3600000000000L", parseTimeNanos("1h") == 3600000000000L);
        assertTrue("parseTimeNanos(\"1.5H\") != 5400000000000L", parseTimeNanos("1.5H") == 5400000000000L);
        assertTrue("parseTimeNanos(\"1d\") != 86400000000000L", parseTimeNanos("1d") == 86400000000000L);
        assertTrue("parseTimeNanos(\"1.5D\") != 129600000000000L", parseTimeNanos("1.5D") == 129600000000000L);
        }

    /**
    * Invoke {@link #parseTime(String, int)} with various valid arguments.
    */
    @Test
    public void parseTimeUnit()
        {
        assertTrue("parseTime(\"1\", UNIT_MS) != UNIT_MS", parseTime("1", UNIT_MS) == UNIT_MS);
        assertTrue("parseTime(\"1\", UNIT_S) != UNIT_S", parseTime("1", UNIT_S) == UNIT_S);
        assertTrue("parseTime(\"1\", UNIT_M) != UNIT_M", parseTime("1", UNIT_M) == UNIT_M);
        assertTrue("parseTime(\"1\", UNIT_H) != UNIT_H", parseTime("1", UNIT_H) == UNIT_H);
        assertTrue("parseTime(\"1\", UNIT_D) != UNIT_D", parseTime("1", UNIT_D) == UNIT_D);

        assertTrue("parseTime(\"1ms\", UNIT_MS) != 1L", parseTime("1ms", UNIT_MS) == 1L);
        assertTrue("parseTime(\"1ms\", UNIT_S) != 1L", parseTime("1ms", UNIT_S) == 1L);
        assertTrue("parseTime(\"1ms\", UNIT_M) != 1L", parseTime("1ms", UNIT_M) == 1L);
        assertTrue("parseTime(\"1ms\", UNIT_H) != 1L", parseTime("1ms", UNIT_H) == 1L);
        assertTrue("parseTime(\"1ms\", UNIT_D) != 1L", parseTime("1ms", UNIT_D) == 1L);
        }

    /**
    * Invoke {@link #parseTimeNanos(String, int)} with various valid arguments.
    */
    @Test
    public void parseTimeNanosUnit()
        {
        assertTrue("parseTimeNanos(\"1\", UNIT_NS) != 1L", parseTimeNanos("1", UNIT_NS) == 1L);
        assertTrue("parseTimeNanos(\"1\", UNIT_US) != 1000L", parseTimeNanos("1", UNIT_US) == 1000L);
        assertTrue("parseTimeNanos(\"1\", UNIT_MS) != UNIT_MS*1000000L", parseTimeNanos("1", UNIT_MS) == UNIT_MS*1000000L);
        assertTrue("parseTimeNanos(\"1\", UNIT_S) != UNIT_S*1000000L", parseTimeNanos("1", UNIT_S) == UNIT_S*1000000L);
        assertTrue("parseTimeNanos(\"1\", UNIT_M) != UNIT_M*1000000L", parseTimeNanos("1", UNIT_M) == UNIT_M*1000000L);
        assertTrue("parseTimeNanos(\"1\", UNIT_H) != UNIT_H*1000000L", parseTimeNanos("1", UNIT_H) == UNIT_H*1000000L);
        assertTrue("parseTimeNanos(\"1\", UNIT_D) != UNIT_D*1000000L", parseTimeNanos("1", UNIT_D) == UNIT_D*1000000L);

        assertTrue("parseTimeNanos(\"1ns\", UNIT_MS) != 1L", parseTimeNanos("1ns", UNIT_MS) == 1L);
        assertTrue("parseTimeNanos(\"1ns\", UNIT_S) != 1L", parseTimeNanos("1ns", UNIT_S) == 1L);
        assertTrue("parseTimeNanos(\"1ns\", UNIT_M) != 1L", parseTimeNanos("1ns", UNIT_M) == 1L);
        assertTrue("parseTimeNanos(\"1ns\", UNIT_H) != 1L", parseTimeNanos("1ns", UNIT_H) == 1L);
        assertTrue("parseTimeNanos(\"1ns\", UNIT_D) != 1L", parseTimeNanos("1ns", UNIT_D) == 1L);
        }


    // ----- toBandwidthString(long, boolean) tests -------------------------

    /**
    * Invoke {@link #toBandwidthString(long, boolean)} with an argument that
    * contains a negative number.
    */
    @Test
    public void toBandwidthStringInvalidNumber()
        {
        try
            {
            toBandwidthString(-1, true);
            fail("expected exception");
            }
        catch (IllegalArgumentException e)
            {
            // expected
            }
        }

    /**
    * Invoke {@link #toBandwidthString(long, boolean)} with various valid
    * arguments.
    */
    @Test
    public void toBandwidthString()
        {
        assertTrue("toBandwidthString(512L, true) != \"4Kbps\"", equals(toBandwidthString(512L, true), "4Kbps"));
        assertTrue("toBandwidthString(1L << 10, true) != \"8Kbps\"", equals(toBandwidthString(1L << 10, true), "8Kbps"));
        assertTrue("toBandwidthString(1L << 10, false) != \"8Kbps\"", equals(toBandwidthString(1L << 10, false), "8Kbps"));
        assertTrue("toBandwidthString(1L << 20, true) != \"8Mbps\"", equals(toBandwidthString(1L << 20, true), "8Mbps"));
        assertTrue("toBandwidthString(1L << 20, false) != \"8Mbps\"", equals(toBandwidthString(1L << 20, false), "8Mbps"));
        assertTrue("toBandwidthString(1L << 30, true) != \"8Gbps\"", equals(toBandwidthString(1L << 30, true), "8Gbps"));
        assertTrue("toBandwidthString(1L << 30, false) != \"8Gbps\"", equals(toBandwidthString(1L << 30, false), "8Gbps"));
        assertTrue("toBandwidthString(1L << 40, true) != \"8Tbps\"", equals(toBandwidthString(1L << 40, true), "8Tbps"));
        assertTrue("toBandwidthString(1L << 40, false) != \"8Tbps\"", equals(toBandwidthString(1L << 40, false), "8Tbps"));
        assertTrue("toBandwidthString(1L << 42, true) != \"32Tbps\"", equals(toBandwidthString(1L << 42, true), "32Tbps"));
        assertTrue("toBandwidthString(1L << 42, false) != \"32Tbps\"", equals(toBandwidthString(1L << 42, false), "32Tbps"));
        assertTrue("toBandwidthString(1365L, true) != \"10920bps\"", equals(toBandwidthString(1365L, true), "10920bps"));
        assertTrue("toBandwidthString(1365L, false) != \"10.6Kbps\"", equals(toBandwidthString(1365L, false), "10.6Kbps"));
        assertTrue("toBandwidthString(1398101L, true) != \"11184808bps\"", equals(toBandwidthString(1398101L, true), "11184808bps"));
        assertTrue("toBandwidthString(1398101L, false) != \"10.6Mbps\"", equals(toBandwidthString(1398101L, false), "10.6Mbps"));
        assertTrue("toBandwidthString(1431655765L, true) != \"11453246120bps\"", equals(toBandwidthString(1431655765L, true), "11453246120bps"));
        assertTrue("toBandwidthString(1431655765L, false) != \"10.6Gbps\"", equals(toBandwidthString(1431655765L, false), "10.6Gbps"));
        assertTrue("toBandwidthString(1466015503701L, true) != \"11728124029608bps\"", equals(toBandwidthString(1466015503701L, true), "11728124029608bps"));
        assertTrue("toBandwidthString(1466015503701L, false) != \"10.6Tbps\"", equals(toBandwidthString(1466015503701L, false), "10.6Tbps"));
        assertTrue("toBandwidthString(1310720L, false) != \"10Mbps\"", equals(toBandwidthString(1310720L, false), "10Mbps"));
        assertTrue("toBandwidthString(1610612736L, false) != \"12Gbps\"", equals(toBandwidthString(1610612736L, false), "12Gbps"));
        assertTrue("toBandwidthString(Long.MAX_VALUE, true) != \"9223372036854775807Bps\"", equals(toBandwidthString(Long.MAX_VALUE, true), "9223372036854775807Bps"));
        assertTrue("toBandwidthString(Long.MAX_VALUE, false) != \"8388607TBps\"", equals(toBandwidthString(Long.MAX_VALUE, false), "8388607TBps"));
        }


    // ----- toBandwidthString(long, boolean) tests -------------------------

    /**
    * Invoke {@link #toMemorySizeString(long, boolean)} with an argument that
    * contains a negative number.
    */
    @Test
    public void toMemorySizeStringInvalidNumber()
        {
        try
            {
            toMemorySizeString(-1, true);
            fail("expected exception");
            }
        catch (IllegalArgumentException e)
            {
            // expected
            }
        }

    /**
    * Invoke {@link #toMemorySizeString(long, boolean)} with various valid
    * arguments.
    */
    @Test
    public void toMemorySizeString()
        {
        assertTrue("toMemorySizeString(512L, true) != \"512\"", equals(toMemorySizeString(512L, true), "512"));
        assertTrue("toMemorySizeString(1L << 10, true) != \"1KB\"", equals(toMemorySizeString(1L << 10, true), "1KB"));
        assertTrue("toMemorySizeString(1L << 10, false) != \"1KB\"", equals(toMemorySizeString(1L << 10, false), "1KB"));
        assertTrue("toMemorySizeString(1L << 20, true) != \"1MB\"", equals(toMemorySizeString(1L << 20, true), "1MB"));
        assertTrue("toMemorySizeString(1L << 20, false) != \"1MB\"", equals(toMemorySizeString(1L << 20, false), "1MB"));
        assertTrue("toMemorySizeString(1L << 30, true) != \"1GB\"", equals(toMemorySizeString(1L << 30, true), "1GB"));
        assertTrue("toMemorySizeString(1L << 30, false) != \"1GB\"", equals(toMemorySizeString(1L << 30, false), "1GB"));
        assertTrue("toMemorySizeString(1L << 40, true) != \"1TB\"", equals(toMemorySizeString(1L << 40, true), "1TB"));
        assertTrue("toMemorySizeString(1L << 40, false) != \"1TB\"", equals(toMemorySizeString(1L << 40, false), "1TB"));
        assertTrue("toMemorySizeString(1L << 42, true) != \"4TB\"", equals(toMemorySizeString(1L << 42, true), "4TB"));
        assertTrue("toMemorySizeString(1L << 42, false) != \"4TB\"", equals(toMemorySizeString(1L << 42, false), "4TB"));
        assertTrue("toMemorySizeString(1365L, true) != \"1365\"", equals(toMemorySizeString(1365L, true), "1365"));
        assertTrue("toMemorySizeString(1365L, false) != \"1.3KB\"", equals(toMemorySizeString(1365L, false), "1.33KB"));
        assertTrue("toMemorySizeString(1398101L, true) != \"1398101\"", equals(toMemorySizeString(1398101L, true), "1398101"));
        assertTrue("toMemorySizeString(1398101L, false) != \"1.33MB\"", equals(toMemorySizeString(1398101L, false), "1.33MB"));
        assertTrue("toMemorySizeString(1431655765L, true) != \"1431655765\"", equals(toMemorySizeString(1431655765L, true), "1431655765"));
        assertTrue("toMemorySizeString(1431655765L, false) != \"1.33GB\"", equals(toMemorySizeString(1431655765L, false), "1.33GB"));
        assertTrue("toMemorySizeString(1466015503701L, true) != \"1466015503701\"", equals(toMemorySizeString(1466015503701L, true), "1466015503701"));
        assertTrue("toMemorySizeString(1466015503701L, false) != \"1.33TB\"", equals(toMemorySizeString(1466015503701L, false), "1.33TB"));
        assertTrue("toMemorySizeString(1310720L, false) != \"1.25MB\"", equals(toMemorySizeString(1310720L, false), "1.25MB"));
        assertTrue("toMemorySizeString(1610612736L, false) != \"1.50GB\"", equals(toMemorySizeString(1610612736L, false), "1.50GB"));
        assertTrue("toMemorySizeString(Long.MAX_VALUE, true) != \"9223372036854775807\"", equals(toMemorySizeString(Long.MAX_VALUE, true), "9223372036854775807"));
        assertTrue("toMemorySizeString(Long.MAX_VALUE, false) != \"8388607TB\"", equals(toMemorySizeString(Long.MAX_VALUE, false), "8388607TB"));
        }


    // ----- random tests ---------------------------------------------------

    /**
    * Test the random value generator.
    */
    @Test
    public void testRandom()
        {
        assertTrue(getRandom() != null);
        }

    /**
    * Test the random generation of Binary objects.
    */
    @Test
    public void testRandomBinary()
        {
        assertTrue(getRandomBinary(5,5).length() == 5);

        for (int i = 0; i < 100; ++i)
            {
            Binary bin = getRandomBinary(50, 100);
            assertTrue(bin.length() >= 50 && bin.length() <= 100);
            assertFalse(equalsDeep(bin.toByteArray(), new byte[bin.length()]));
            }
        }

    /**
    * Test the random generation of String objects.
    */
    @Test
    public void testRandomString()
        {
        assertTrue(getRandomString(5,5,true).length() == 5);
        assertTrue(getRandomString(5,5,false).length() == 5);

        for (int i = 0; i < 100; ++i)
            {
            String s = getRandomString(50, 100, false);
            assertTrue(s.length() >= 50 && s.length() <= 100);
            assertFalse(equalsDeep(s.toCharArray(), new char[s.length()]));
            }

        for (int i = 0; i < 100; ++i)
            {
            String s = getRandomString(50, 100, true);
            assertTrue(s.length() >= 50 && s.length() <= 100);

            char[] ach = s.toCharArray();
            for (int of = 0, cch = ach.length; of < cch; ++of)
                {
                assertTrue(ach[of] >= 32 && ach[of] <= 127);
                }
            }
        }

    /**
    * Test the random re-ordering of an int array.
    */
    @Test
    public void testRandomizeIntArray()
        {
        int[] a = null;
        assertTrue(randomize(a) == null);

        a = new int[0];
        int[] aOld = a.clone();
        assertTrue(equalsDeep(aOld, randomize(a)));

        a = new int[1];
        a[0] = 99;
        aOld = a.clone();
        assertTrue(equalsDeep(aOld, randomize(a)));

        for (int i = 0; i <  1000; ++i)
            {
            a = new int[64];
            for (int of = 0, c = a.length; of < c; ++of)
                {
                a[of] = of;
                }
            aOld = a.clone();
            assertTrue(a == randomize(a));
            assertFalse(equalsDeep(aOld, a));

            // verify nothing lost
            long l = 0;
            for (int of = 0, c = a.length; of < c; ++of)
                {
                int n = a[of];
                assertTrue(n >= 0 && n < 64);
                l |= (1L << n);
                }
            assertTrue(l == 0xFFFFFFFFFFFFFFFFL);
            }
        }

    /**
    * Test the random re-ordering of an Object array.
    */
    @Test
    public void testRandomizeObjectArray()
        {
        Integer[] a = null;
        assertTrue(randomize(a) == null);

        a = new Integer[0];
        Integer[] aOld = a.clone();
        assertTrue(equalsDeep(aOld, randomize(a)));

        a = new Integer[1];
        a[0] = 99;
        aOld = a.clone();
        assertTrue(equalsDeep(aOld, randomize(a)));

        for (int i = 0; i <  1000; ++i)
            {
            a = new Integer[64];
            for (int of = 0, c = a.length; of < c; ++of)
                {
                a[of] = of;
                }
            aOld = a.clone();
            assertTrue(a == randomize(a));
            assertFalse(equalsDeep(aOld, a));

            // verify nothing lost
            long l = 0;
            for (int of = 0, c = a.length; of < c; ++of)
                {
                int n = a[of].intValue();
                assertTrue(n >= 0 && n < 64);
                l |= (1L << n);
                }
            assertTrue(l == 0xFFFFFFFFFFFFFFFFL);
            }
        }


    // ----- String operation tests -----------------------------------------

    /**
     * Boundary test for {@link #truncateString(String, int)}.
     */
    @Test
    public void testTruncateStringBoundary()
        {
        assertEquals(19, Base.truncateString("String of length 19", 25).length());
        }

    /**
     * Basic flow test for {@link #truncateString(String, int)}.
     */
    @Test
    public void testTruncateStringBasic()
        {
        String sInput =
            "Coherence provides replicated and distributed (partitioned) data management and caching "
                + "services on top of a reliable, highly scalable peer-to-peer clustering protocol.";

        // check with additional size of 12, to account for the padding post truncation
        assertEquals(100 + 12, Base.truncateString(sInput, 100).length());
        }


    // ----- time operation tests -------------------------------------------

    /**
     * Test {@link #getTimeZone(String)}.
     */
    @Test
    public void testGetTimeZone()
        {
        try
            {
            getTimeZone(null);
            fail();
            }
        catch(NullPointerException e)
            {
            }

        // unknown timezone should return GMT
        assertEquals(getTimeZone(""),       getTimeZone("GMT"));
        assertEquals(getTimeZone("foobar"), getTimeZone("GMT"));

        // check cached value against initial value
        assertEquals(getTimeZone("EST"), getTimeZone("EST"));

        // known timezone should be different than GMT
        assertFalse(getTimeZone("EST").equals(getTimeZone("GMT")));
        }
    }
