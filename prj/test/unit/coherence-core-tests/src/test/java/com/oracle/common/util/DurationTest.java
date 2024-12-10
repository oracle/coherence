/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.common.util;

import com.oracle.coherence.common.util.Duration;
import com.oracle.coherence.common.util.Duration.Magnitude;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;

import java.util.Map.Entry;

/**
 * Unit Tests for {@link Duration}s.
 *
 * @author bko 2011.07.15
 */
public class DurationTest
    {
    // ----- DurationTest methods -------------------------------------------

    /**
     * Ensure we can't create a {@link Duration} any of the {@link #INVALID_DURATIONS}.
     */
    @Test()
    public void testInvalidDurations()
        {
        for (String sDuration : INVALID_DURATIONS)
            {
            try
                {
                new Duration(sDuration);
                Assert.fail("Expected exception for [" + sDuration + "]");
                }
            catch (IllegalArgumentException e)
                {
                // expected
                }
            }
        }

    /**
     * Ensure we can create a {@link Duration} each of the {@link #VALID_DURATIONS}.
     */
    @Test()
    public void testValidDurations()
        {
        for (Entry<String, Long> entry : VALID_DURATIONS.entrySet())
            {
            String sDuration = entry.getKey();
            long   cUnits    = entry.getValue();

            try
                {
                Duration duration = new Duration(sDuration);

                Assert.assertEquals("Expected Duration: " + sDuration, cUnits, duration.getNanos());
                }
            catch (IllegalArgumentException e)
                {
                e.printStackTrace();
                Assert.fail("Unexpected exception for [" + sDuration + "]");
                }
            }
        }

    /**
     * Ensure we can format a {@link Duration} as an exact {@link String} value.
     */
    @Test()
    public void testExactDurationToString()
        {
        for (Entry<Duration, String> entry : EXACT_DURATIONS.entrySet())
            {
            Duration duration  = entry.getKey();
            String   sDuration = entry.getValue();

            try
                {
                Assert.assertEquals(sDuration, duration.toString(true));
                }
            catch (IllegalArgumentException e)
                {
                e.printStackTrace();
                Assert.fail("Unexpected exception for [" + sDuration + "]");
                }
            }
        }

    /**
     * Ensure we can format a {@link Duration} as an approximate {@link String} value.
     */
    @Test()
    public void testApproximateDurationToString()
        {
        for (Entry<Duration, String> entry : APPROXIMATE_DURATIONS.entrySet())
            {
            Duration duration  = entry.getKey();
            String   sDuration = entry.getValue();

            try
                {
                Assert.assertEquals(sDuration, duration.toString(false));
                }
            catch (IllegalArgumentException e)
                {
                e.printStackTrace();
                Assert.fail("Unexpected exception for [" + sDuration + "]");
                }
            }
        }

    /**
     * Ensure we can construct {@link Duration}s when using default {@link Magnitude}s.
     */
    @Test()
    public void testDurationConstructionWithDefaultMagnitudes()
        {
        for (Entry<Duration, Duration> entry : DEFAULT_DURATIONS.entrySet())
            {
            Duration actual   = entry.getKey();
            Duration expected = entry.getValue();

            try
                {
                Assert.assertEquals(expected, actual);
                }
            catch (IllegalArgumentException e)
                {
                e.printStackTrace();
                Assert.fail("Unexpected exception for [" + actual + "]");
                }
            }
        }

    /**
     * Ensure we can construct {@link Duration}s when using specifying but ignoring default {@link Magnitude}s.
     */
    @Test()
    public void testDurationConstructionIgnoringDefaultMagnitudes()
        {
        for (Entry<Duration, Duration> entry : DEFAULT_DURATIONS_WITH_MAGNITUDES.entrySet())
            {
            Duration actual   = entry.getKey();
            Duration expected = entry.getValue();

            try
                {
                Assert.assertEquals(expected, actual);
                }
            catch (IllegalArgumentException e)
                {
                e.printStackTrace();
                Assert.fail("Unexpected exception for [" + actual + "]");
                }
            }
        }

    // ----- constants --------------------------------------------------

    /**
     * A collection of invalid {@link Duration} {@link String}s (when {@link Magnitude} is required)
     */
    private String[] INVALID_DURATIONS =
        {
        "", "1", null, " ", "m", "1x", "1mm", "1.M", "1.1.1", "1.", "1.1.1M", "s", "u", "0.u", "0s 0s"
        };

    /**
     * A map of valid {@link Duration} {@link String}s and there associated unit counts.
     */
    @SuppressWarnings("serial")
    private HashMap<String, Long> VALID_DURATIONS = new LinkedHashMap<String, Long>()
        {
            {
            put("0s", 0L);
            put("0m", 0L);
            put("0ns", 0L);
            put("0us", 0L);
            put("1ns", 1L);
            put("1us", 1000L);
            put("1000000ns", 1000000L);
            put("1500us", (long) 1.5e6);
            put("0.6s", (long) 6e8);
            put("1500ms", (long) 15e8);
            put("1500Ms", (long) 15e8);
            put("1500mS", (long) 15e8);
            put("1500MS", (long) 15e8);
            put("1s", (long) 1e9);
            put("1S", (long) 1e9);
            put("1m", (long) 6e10);
            put("1M", (long) 6e10);
            put("1.25M", (long) 7.5e10);
            put("1h", (long) 36e11);
            put("1.5h", (long) 5.4e12);
            put("1d", (long) 864e11);
            put("1.75d", (long) 1512e11);
            put("1m 1s", (long) 6.1e10);
            }
        };

    /**
     * A map of {@link Duration}s as exact {@link String}s.
     */
    @SuppressWarnings("serial")
    private HashMap<Duration, String> EXACT_DURATIONS = new LinkedHashMap<Duration, String>()
        {
            {
            put(new Duration(0L), "0ns");
            put(new Duration(1L), "1ns");
            put(new Duration(512L), "512ns");
            put(new Duration(1000L), "1us");
            put(new Duration(30000L), "30us");
            put(new Duration(30001L), "30us1ns");
            put(new Duration(1000000L), "1ms");
            put(new Duration("1500us"), "1ms500us");
            put(new Duration("1500ms"), "1s500ms");
            put(new Duration("1m"), "1m");
            put(new Duration("1h"), "1h");
            put(new Duration("1.1h"), "1h6m");
            put(new Duration("1.25h"), "1h15m");
            put(new Duration("1.5h"), "1h30m");
            put(new Duration("1.75d"), "1d18h");
            put(new Duration("1.9d"), "1d21h36m");
            put(new Duration("1m 1s"), "1m1s");
            }
        };

    /**
     * A map of {@link Duration}s as approximate {@link String}s.
     */
    @SuppressWarnings("serial")
    private HashMap<Duration, String> APPROXIMATE_DURATIONS = new LinkedHashMap<Duration, String>()
        {
            {
            put(new Duration(0L), "0ns");
            put(new Duration(1L), "1ns");
            put(new Duration(512L), "512ns");
            put(new Duration(1000L), "1us");
            put(new Duration(30000L), "30us");
            put(new Duration(30001L), "30us");
            put(new Duration(1000000L), "1ms");
            put(new Duration("1500us"), "1.50ms");
            put(new Duration("1510ms"), "1.51s");
            put(new Duration("1m"), "1m");
            put(new Duration("1m1s500ms"), "1m1s");
            put(new Duration("1h"), "1h");
            put(new Duration("1.1h"), "1h6m");
            put(new Duration("1.25h"), "1h15m");
            put(new Duration("1.5h"), "1h30m");
            put(new Duration("1.75d"), "1d18h");
            put(new Duration("1.9d"), "1d21h");
            put(new Duration("1m 1s"), "1m1s");
            put(new Duration("1h 1s"), "1h");
            put(new Duration("1m 1s 1us"), "1m1s");
            }
        };

    /**
     * A map of {@link Duration}s that use default {@link Magnitude}s.
     */
    @SuppressWarnings("serial")
    private HashMap<Duration, Duration> DEFAULT_DURATIONS = new LinkedHashMap<Duration, Duration>()
        {
            {
            put(new Duration("1500", Magnitude.NANO), new Duration(1500));
            put(new Duration("1500", Magnitude.MILLI), new Duration(1500, Magnitude.MILLI));
            put(new Duration("1.5", Magnitude.MINUTE), new Duration(1.5, Magnitude.MINUTE));
            put(new Duration("1", Magnitude.HOUR), new Duration(1, Magnitude.HOUR));
            put(new Duration("1.1", Magnitude.HOUR), new Duration(1.1, Magnitude.HOUR));
            put(new Duration("1.75", Magnitude.DAY), new Duration(1.75, Magnitude.DAY));
            }
        };

    /**
     * A map of {@link Duration}s that specify default {@link Magnitude}s but don't use them.
     */
    @SuppressWarnings("serial")
    private HashMap<Duration, Duration> DEFAULT_DURATIONS_WITH_MAGNITUDES = new LinkedHashMap<Duration, Duration>()
        {
            {
            put(new Duration("1500ms", Magnitude.NANO), new Duration(1500, Magnitude.MILLI));
            put(new Duration("1500s", Magnitude.MILLI), new Duration(1500, Magnitude.SECOND));
            put(new Duration("1.5h", Magnitude.MINUTE), new Duration(1.5, Magnitude.HOUR));
            put(new Duration("1d", Magnitude.HOUR), new Duration(1, Magnitude.DAY));
            put(new Duration("1.1d", Magnitude.HOUR), new Duration(1.1, Magnitude.DAY));
            put(new Duration("1.75h", Magnitude.DAY), new Duration(1.75, Magnitude.HOUR));
            }
        };
    }
