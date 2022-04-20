/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.common.util;

import com.oracle.coherence.common.util.MemorySize;
import com.oracle.coherence.common.util.MemorySize.Magnitude;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;

import java.util.Map.Entry;

/**
 * Unit Tests for {@link MemorySize}s.
 *
 * @author bko 2011.07.13
 */
public class MemorySizeTest
    {
    // ----- MemorySizeTest methods -----------------------------------------

    /**
     * Ensure we can't create a {@link MemorySize} any of the {@link #INVALID_MEMORY_SIZES}.
     */
    @Test()
    public void testInvalidMemorySizes()
        {
        for (String sMemorySize : INVALID_MEMORY_SIZES)
            {
            try
                {
                new MemorySize(sMemorySize);
                Assert.fail("Expected exception for [" + sMemorySize + "]");
                }
            catch (IllegalArgumentException e)
                {
                // expected
                }
            }
        }

    /**
     * Ensure we can create a {@link MemorySize} each of the {@link #VALID_MEMORY_SIZES}.
     */
    @Test()
    public void testValidMemorySizes()
        {
        for (Entry<String, Long> entry : VALID_MEMORY_SIZES.entrySet())
            {
            String sMemorySize = entry.getKey();
            long   cBytes      = entry.getValue();

            try
                {
                MemorySize memorySize = new MemorySize(sMemorySize);

                Assert.assertEquals("Invalid MemorySize Returned for [" + sMemorySize + "]", cBytes,
                                    memorySize.getByteCount());
                }
            catch (IllegalArgumentException e)
                {
                e.printStackTrace();
                Assert.fail("Unexpected exception for [" + sMemorySize + "]");
                }
            }
        }

    /**
     * Ensure we can convert {@link MemorySize}s into {@link String}s (without loss of precision)
     */
    @Test
    public void testMemorySizeToStringExact()
        {
        for (Entry<MemorySize, String> entry : EXACT_MEMORY_SIZES.entrySet())
            {
            MemorySize memorySize  = entry.getKey();
            String     sMemorySize = entry.getValue();

            Assert.assertEquals(sMemorySize, memorySize.toString(true));
            }
        }

    /**
     * Ensure we can convert {@link MemorySize}s into {@link String}s (using rounding)
     */
    @Test
    public void testMemorySizeToStringApproximate()
        {
        for (Entry<MemorySize, String> entry : APPROX_MEMORY_SIZES.entrySet())
            {
            MemorySize memorySize  = entry.getKey();
            String     sMemorySize = entry.getValue();

            Assert.assertEquals(sMemorySize, memorySize.toString(false));
            Assert.assertEquals(sMemorySize, memorySize.toString());
            }
        }

    /**
     * Ensure we can create a {@link MemorySize} using default magnitudes.
     */
    @Test()
    public void testMemorySizesWithDefaultMagnitudes()
        {
        for (Entry<MemorySize, Long> entry : DEFAULT_MEMORY_SIZES.entrySet())
            {
            MemorySize memorySize = entry.getKey();
            long       cBytes     = entry.getValue();

            Assert.assertEquals(cBytes, memorySize.getByteCount());
            }
        }

    // ----- constants --------------------------------------------------

    /**
     * A collection of invalid {@link MemorySize} {@link String}s for testing {@link MemorySize} construction.
     */
    private String[] INVALID_MEMORY_SIZES =
        {
        "", null, " ", "k", "1x", "1kk", "1.M", "1.1.1", "1.", "1.1.1M"
        };

    /**
     * A map of valid {@link MemorySize} {@link String}s for testing {@link MemorySize} construction.
     */
    @SuppressWarnings("serial")
    private HashMap<String, Long> VALID_MEMORY_SIZES = new LinkedHashMap<String, Long>()
        {
            {
            put("0", 0L);
            put("1", 1L);
            put("1B", 1L);
            put("1b", 1L);
            put("0.25", 0L);
            put("0.5", 1L);
            put("0.25b", 0L);
            put("0.5b", 1L);
            put("1K", 1024L);
            put("1k", 1024L);
            put("1M", 1048576L);
            put("1m", 1048576L);
            put("1.25M", 1310720L);
            put("1.5M", 1572864L);
            put("0G", 0L);
            put("1G", 1073741824L);
            put("1g", 1073741824L);
            put("1T", 1099511627776L);
            put("1t", 1099511627776L);
            }
        };

    /**
     * A map of {@link MemorySize} to {@link String}s for testing approximate {@link MemorySize#toString(boolean)}.
     */
    @SuppressWarnings("serial")
    private HashMap<MemorySize, String> APPROX_MEMORY_SIZES = new LinkedHashMap<MemorySize, String>()
        {
            {
            put(new MemorySize(0L), "0B");
            put(new MemorySize(1L), "1B");
            put(new MemorySize(128L), "128B");
            put(new MemorySize(255L), "255B");
            put(new MemorySize(256L), "256B");
            put(new MemorySize(257L), "257B");
            put(new MemorySize(512L), "512B");
            put(new MemorySize(768L), "768B");
            put(new MemorySize(1023L), "1023B");
            put(new MemorySize(1024L), "1KB");
            put(new MemorySize(1048576L), "1MB");
            put(new MemorySize(1073741824L), "1GB");
            put(new MemorySize(1099511627776L), "1TB");
            put(new MemorySize(1365), "1.33KB");
            put(new MemorySize(1398101L), "1.33MB");
            put(new MemorySize(1431655765L), "1.33GB");
            put(new MemorySize(1466015503701L), "1.33TB");
            put(new MemorySize(1, Magnitude.KB), "1KB");
            put(new MemorySize(1023, Magnitude.KB), "1023KB");
            put(new MemorySize(1024, Magnitude.KB), "1MB");
            put(new MemorySize(1025, Magnitude.KB), "1MB");
            put(new MemorySize(1535, Magnitude.KB), "1.49MB");
            put(new MemorySize(1280, Magnitude.KB), "1.25MB");
            put(new MemorySize(1536, Magnitude.KB), "1.50MB");
            put(new MemorySize(1792, Magnitude.KB), "1.75MB");
            put(new MemorySize(1537, Magnitude.KB), "1.50MB");
            put(new MemorySize(1014.4, Magnitude.KB), "1014KB");
            put(new MemorySize(1014.99, Magnitude.KB), "1014KB");
            put(new MemorySize(1014.5, Magnitude.KB), "1014KB");
            put(new MemorySize(999.1, Magnitude.KB), "999KB");
            put(new MemorySize(999.5, Magnitude.KB), "999KB");
            put(new MemorySize(999.99, Magnitude.KB), "999KB");
            put(new MemorySize(3, Magnitude.KB), "3KB");
            put(new MemorySize("6.08gb"), "6.08GB");
            }
        };

    /**
     * A map of {@link MemorySize} to {@link String}s for testing exact {@link MemorySize#toString(boolean)}.
     */
    @SuppressWarnings("serial")
    private HashMap<MemorySize, String> EXACT_MEMORY_SIZES = new LinkedHashMap<MemorySize, String>()
        {
            {
            put(new MemorySize(0, Magnitude.KB), "0B");
            put(new MemorySize(1, Magnitude.KB), "1KB");
            put(new MemorySize(1023, Magnitude.KB), "1023KB");
            put(new MemorySize(1024, Magnitude.KB), "1MB");
            put(new MemorySize(1025, Magnitude.KB), "1025KB");
            put(new MemorySize(1535, Magnitude.KB), "1535KB");
            put(new MemorySize(1280, Magnitude.KB), "1.25MB");
            put(new MemorySize(1536, Magnitude.KB), "1.50MB");
            put(new MemorySize(1792, Magnitude.KB), "1.75MB");
            put(new MemorySize(1537, Magnitude.KB), "1537KB");
            put(new MemorySize(1), "1B");
            put(new MemorySize(1023), "1023B");
            put(new MemorySize(1024), "1KB");
            put(new MemorySize(1025), "1025B");
            put(new MemorySize(2047), "2047B");
            put(new MemorySize(2048), "2KB");
            put(new MemorySize(2049), "2049B");
            put(new MemorySize(1024L), "1KB");
            put(new MemorySize(1048576L), "1MB");
            put(new MemorySize(1073741824L), "1GB");
            put(new MemorySize(1099511627776L), "1TB");
            put(new MemorySize(1365), "1365B");
            put(new MemorySize(1398101L), "1398101B");
            put(new MemorySize(1431655765L), "1431655765B");
            put(new MemorySize(1466015503701L), "1466015503701B");
            }
        };

    /**
     * A map of valid {@link MemorySize} {@link String}s using default magnitudes.
     */
    @SuppressWarnings("serial")
    private HashMap<MemorySize, Long> DEFAULT_MEMORY_SIZES = new LinkedHashMap<MemorySize, Long>()
        {
            {
            put(new MemorySize("0"), 0L);
            put(new MemorySize("1", Magnitude.BYTES), 1L);
            put(new MemorySize("1B", Magnitude.KB), 1L);
            put(new MemorySize("1b", Magnitude.GB), 1L);
            put(new MemorySize("0.25", Magnitude.BYTES), 0L);
            put(new MemorySize("0.5", Magnitude.BYTES), 1L);
            put(new MemorySize("1", Magnitude.KB), 1024L);
            put(new MemorySize("1M", Magnitude.KB), 1048576L);
            put(new MemorySize("1", Magnitude.MB), 1048576L);
            put(new MemorySize("1.25", Magnitude.MB), 1310720L);
            put(new MemorySize("0", Magnitude.GB), 0L);
            put(new MemorySize("1", Magnitude.GB), 1073741824L);
            put(new MemorySize("1g", Magnitude.KB), 1073741824L);
            }
        };
    }
