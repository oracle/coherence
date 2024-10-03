/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;

import com.oracle.coherence.common.util.Duration;

import com.oracle.coherence.testing.SystemPropertyResource;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.MatcherAssert.assertThat;
import static com.oracle.coherence.common.util.Duration.Magnitude.MILLI;
import static com.tangosol.net.GuardSupport.PROP_LOG_THREADDUMP_INTERVAL;

/**
 * Unit tests for GuardSupport.
 *
 * @since 25.03
 * @author jf  2024.10.1
 */
public class GuardSupportTest
    {
    @Test
    public void testOverrideLogThreadDumpInterval()
        {
        final String TEST_LOG_THREADDUMP_INTERVAL = "1h";
        
        try (SystemPropertyResource resource = new SystemPropertyResource(PROP_LOG_THREADDUMP_INTERVAL, TEST_LOG_THREADDUMP_INTERVAL))
            {
            assertThat(GuardSupport.getLogThreaddumpIntervalMs(), is(new Duration(TEST_LOG_THREADDUMP_INTERVAL).as(MILLI)));
            }
        }

    @Test
    public void testDefaultLogThreadDumpInterval()
        {
        System.clearProperty(PROP_LOG_THREADDUMP_INTERVAL);
        assertThat(GuardSupport.getLogThreaddumpIntervalMs(), is(new Duration(GuardSupport.DEFAULT_LOG_THREADDUMP_INTERVAL).as(MILLI)));
        }

    @Test
    public void testCeilingForLogThreadDumpInterval()
        {
        final String TEST_LOG_THREADDUMP_INTERVAL = "5d";

        assertThat(GuardSupport.MAX_LOG_THREADDUMP_INTERVAL_MS, lessThan(new Duration(TEST_LOG_THREADDUMP_INTERVAL).as(MILLI)));
        try (SystemPropertyResource resource = new SystemPropertyResource(PROP_LOG_THREADDUMP_INTERVAL, TEST_LOG_THREADDUMP_INTERVAL))
            {
            long ldtInterval = GuardSupport.getLogThreaddumpIntervalMs();

            assertThat(ldtInterval, is(GuardSupport.MAX_LOG_THREADDUMP_INTERVAL_MS));
            assertThat(ldtInterval, lessThan(new Duration(TEST_LOG_THREADDUMP_INTERVAL).as(MILLI)));
            }
        }

    @Test
    public void testDefaultOnInvalidValueForLogThreadDumpInterval()
        {
        final String TEST_LOG_THREADDUMP_INTERVAL = "invalidDuration";

        try (SystemPropertyResource resource = new SystemPropertyResource(PROP_LOG_THREADDUMP_INTERVAL, TEST_LOG_THREADDUMP_INTERVAL))
            {
            assertThat(GuardSupport.getLogThreaddumpIntervalMs(), is(new Duration(GuardSupport.DEFAULT_LOG_THREADDUMP_INTERVAL).as(MILLI)));
            }
        }
    }