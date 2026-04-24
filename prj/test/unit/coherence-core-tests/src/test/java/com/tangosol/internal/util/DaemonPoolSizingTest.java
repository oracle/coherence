/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.util;

import org.junit.Test;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class DaemonPoolSizingTest
    {
    @Test
    public void shouldPreserveExplicitFiniteMax()
        {
        DaemonPoolSizing.Result result = DaemonPoolSizing.resolveThreadCountMax(512, 4, 1024L, 64L, 2048);

        assertThat(result.isDerived(), is(false));
        assertThat(result.getEffectiveMax(), is(512));
        }

    @Test
    public void shouldDeriveMaxFromHeapAndStack()
        {
        DaemonPoolSizing.Result result =
                DaemonPoolSizing.resolveThreadCountMax(Integer.MAX_VALUE, 4, 512L * 1024L * 1024L, 1024L * 1024L, 2048);

        assertThat(result.isDerived(), is(true));
        assertThat(result.getEffectiveMax(), is(512));
        }

    @Test
    public void shouldFloorDerivedMaxToMinThreadCount()
        {
        DaemonPoolSizing.Result result =
                DaemonPoolSizing.resolveThreadCountMax(Integer.MAX_VALUE, 16, 8L * 1024L * 1024L, 1024L * 1024L, 2048);

        assertThat(result.getEffectiveMax(), is(16));
        }

    @Test
    public void shouldApplyHardCapToDerivedMax()
        {
        DaemonPoolSizing.Result result =
                DaemonPoolSizing.resolveThreadCountMax(Integer.MAX_VALUE, 4, 8L * 1024L * 1024L * 1024L, 1024L * 1024L, 2048);

        assertThat(result.getEffectiveMax(), is(2048));
        }

    @Test
    public void shouldApplyHardCapWhenHeapIsUnbounded()
        {
        DaemonPoolSizing.Result result =
                DaemonPoolSizing.resolveThreadCountMax(Integer.MAX_VALUE, 4, Long.MAX_VALUE, 1024L * 1024L, 2048);

        assertThat(result.isDerived(), is(true));
        assertThat(result.getEffectiveMax(), is(2048));
        }

    @Test
    public void shouldAllowMinimumToWinAboveHardCap()
        {
        DaemonPoolSizing.Result result =
                DaemonPoolSizing.resolveThreadCountMax(Integer.MAX_VALUE, 3000, 8L * 1024L * 1024L * 1024L, 1024L * 1024L, 2048);

        assertThat(result.isDerived(), is(true));
        assertThat(result.getEffectiveMax(), is(3000));
        }

    @Test
    public void shouldParseThreadStackSizeArguments()
        {
        assertThat(DaemonPoolSizing.parseThreadStackSize("-Xss2m"), is(2L * 1024L * 1024L));
        assertThat(DaemonPoolSizing.parseThreadStackSize("-XX:ThreadStackSize=512"), is(512L * 1024L));
        assertThat(DaemonPoolSizing.parseThreadStackSize("-Xssbogus"), is(-1L));
        }

    @Test
    public void shouldDetermineThreadStackSizeFromJvmArguments()
        {
        long cbStack = DaemonPoolSizing.determineThreadStackSize(Arrays.asList("-Xmx1g", "-Xss768k", "-XX:ThreadStackSize=512"));

        assertThat(cbStack, is(768L * 1024L));
        }
    }
