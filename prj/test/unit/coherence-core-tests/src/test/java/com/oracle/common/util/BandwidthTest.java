/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.common.util;

import com.oracle.coherence.common.util.Bandwidth;
import com.oracle.coherence.common.util.Bandwidth.Rate;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;

import java.util.Map.Entry;

/**
 * Unit Tests for {@link Bandwidth}s.
 *
 * @author bko 2011.07.15
 */
public class BandwidthTest
    {

    // ----- BandwidthTest methods ------------------------------------------

    /**
     * Ensure we can't create a {@link Bandwidth} any of the {@link #INVALID_BANDWIDTHS}.
     */
    @Test()
    public void testInvalidBandwidths()
        {
        for (String sBandwidth : INVALID_BANDWIDTHS)
            {
            try
                {
                new Bandwidth(sBandwidth);
                Assert.fail("Expected exception for [" + sBandwidth + "]");
                }
            catch (IllegalArgumentException e)
                {
                // expected
                }
            }
        }

    /**
     * Ensure we can create a {@link Bandwidth} each of the {@link #VALID_BANDWIDTHS}.
     */
    @Test()
    public void testValidBandwidths()
        {
        for (Entry<String, Long> entry : VALID_BANDWIDTHS.entrySet())
            {
            String sBandwidth = entry.getKey();
            long   cUnits     = entry.getValue();

            try
                {
                Bandwidth bandwidth = new Bandwidth(sBandwidth);

                Assert.assertEquals(cUnits, bandwidth.as(Rate.BITS));
                }
            catch (IllegalArgumentException e)
                {
                e.printStackTrace();
                Assert.fail("Unexpected exception for [" + sBandwidth + "]");
                }
            }
        }

    /**
     * Ensure we can format a {@link Bandwidth} as a {@link String} with exact precision.
     */
    @Test()
    public void testValidExactBandwidths()
        {
        for (Entry<Bandwidth, String> entry : EXACT_BANDWIDTHS.entrySet())
            {
            Bandwidth bandwidth  = entry.getKey();
            String    sBandwidth = entry.getValue();

            try
                {
                Assert.assertEquals(sBandwidth, bandwidth.toString(true));
                }
            catch (IllegalArgumentException e)
                {
                e.printStackTrace();
                Assert.fail("Unexpected exception for [" + sBandwidth + "]");
                }
            }
        }

    /**
     * Ensure we can format a {@link Bandwidth} as a {@link String} with exact precision.
     */
    @Test()
    public void testValidApproximateBandwidths()
        {
        for (Entry<Bandwidth, String> entry : APPROXIMATE_BANDWIDTHS.entrySet())
            {
            Bandwidth bandwidth  = entry.getKey();
            String    sBandwidth = entry.getValue();

            try
                {
                Assert.assertEquals(sBandwidth, bandwidth.toString(false));
                }
            catch (IllegalArgumentException e)
                {
                e.printStackTrace();
                Assert.fail("Unexpected exception for [" + sBandwidth + "]");
                }
            }
        }

    /**
     * Ensure conversions between {@link Bandwidth} {@link Rate}s are accurate.
     */
    @Test
    public void testBandwidthRateConversions()
        {
        Assert.assertEquals(0, new Bandwidth(0, Rate.BITS).as(Rate.BYTES));
        Assert.assertEquals(1, new Bandwidth(8, Rate.BITS).as(Rate.BYTES));
        Assert.assertEquals(8, new Bandwidth(1, Rate.BYTES).as(Rate.BITS));
        }

    // ----- constants --------------------------------------------------

    /**
     * A collection of invalid {@link Bandwidth} {@link String}s.
     */
    private String[] INVALID_BANDWIDTHS =
        {
        "", null, " ", "k", "1x", "1kk", "1.M", "1.1.1", "1.", "1.1.1M", "s", "bps", "0.s", "/s", "5.4/B"
        };

    /**
     * A map of valid {@link Bandwidth} {@link String}s and there associated unit counts.
     */
    @SuppressWarnings("serial")
    private HashMap<String, Long> VALID_BANDWIDTHS = new LinkedHashMap<String, Long>()
        {
            {
            put("1bps", 1L);
            put("1Bps", 8L);
            put("1b/s", 1L);
            put("0.25b/s", 0L);
            put("0.5b/s", 1L);
            put("0.25bps", 0L);
            put("0.5bps", 1L);
            put("1Kps", 1000L);
            put("2kb/s", 2000L);
            put("1Mb/s", 1000000L);
            put("1mps", 1000000L);
            put("1.25Mps", 1250000L);
            put("1.5Mps", 1500000L);
            put("1GPS", 1000000000L);
            put("1gps", 1000000000L);
            put("2TpS", 2000000000000L);
            put("2t/s", 2000000000000L);
            }
        };

    /**
     * A map of {@link Bandwidth}s as exact {@link String}s.
     */
    @SuppressWarnings("serial")
    private HashMap<Bandwidth, String> EXACT_BANDWIDTHS = new LinkedHashMap<Bandwidth, String>()
        {
            {
            put(new Bandwidth("0bps"), "0b/s");
            put(new Bandwidth("1bps"), "1b/s");
            put(new Bandwidth("0.25b/s"), "0b/s");
            put(new Bandwidth("0.5b/s"), "1b/s");
            put(new Bandwidth("0.25bps"), "0b/s");
            put(new Bandwidth("0.5bps"), "1b/s");
            put(new Bandwidth("0KBps"), "0b/s");
            put(new Bandwidth("1Kps"), "1kb/s");
            put(new Bandwidth("2kbs"), "2kb/s");
            put(new Bandwidth("3.0kbs"), "3kb/s");
            put(new Bandwidth("3.25kbs"), "3.25kb/s");
            put(new Bandwidth("3.50kbs"), "3.50kb/s");
            put(new Bandwidth("3.75kbs"), "3.75kb/s");
            put(new Bandwidth("6.09gbs"), "6090mb/s");
            put(new Bandwidth("1mBps"), "8mb/s");
            put(new Bandwidth("2Gbps"), "2gb/s");
            put(new Bandwidth("3gBps"), "24gb/s");
            }
        };

    /**
     * A map of {@link Bandwidth}s as approximate {@link String}s.
     */
    @SuppressWarnings("serial")
    private HashMap<Bandwidth, String> APPROXIMATE_BANDWIDTHS = new LinkedHashMap<Bandwidth, String>()
        {
            {
            put(new Bandwidth("0bps"), "0b/s");
            put(new Bandwidth("1bps"), "1b/s");
            put(new Bandwidth("11bps"), "11b/s");
            put(new Bandwidth("1.11Mbps"), "1.11mb/s");
            put(new Bandwidth("1.333Mbps"), "1.33mb/s");
            put(new Bandwidth("1.335Mbps"), "1.33mb/s");
            put(new Bandwidth("6.09gbs"), "6.09gb/s");
            }
        };
    }
