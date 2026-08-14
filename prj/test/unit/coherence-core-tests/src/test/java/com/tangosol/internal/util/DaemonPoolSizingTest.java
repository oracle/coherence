/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.util;

import org.junit.Test;

import java.util.Arrays;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class DaemonPoolSizingTest
    {
    @Test
    public void shouldAllocateProportionallyFromMinimums()
        {
        Map<String, Integer> map = DaemonPoolSizing.allocate(600, 1000,
                new DaemonPoolSizing.PoolSpec("proxy", DaemonPoolSizing.Role.SERVICE, 50),
                new DaemonPoolSizing.PoolSpec("cache", DaemonPoolSizing.Role.SERVICE, 10));

        assertThat(map.get("proxy"), is(500));
        assertThat(map.get("cache"), is(100));
        }

    @Test
    public void shouldWeightBlockingPoolsAboveServicePools()
        {
        Map<String, Integer> map = DaemonPoolSizing.allocate(110, 1000,
                new DaemonPoolSizing.PoolSpec("proxy", DaemonPoolSizing.Role.BLOCKING_IO, 10),
                new DaemonPoolSizing.PoolSpec("cache", DaemonPoolSizing.Role.SERVICE, 10));

        assertThat(map.get("proxy"), is(70));
        assertThat(map.get("cache"), is(40));
        }

    @Test
    public void shouldRedistributeCapacityFromCpuConstrainedPools()
        {
        Map<String, Integer> map = DaemonPoolSizing.allocate(10, 1,
                new DaemonPoolSizing.PoolSpec("aux", DaemonPoolSizing.Role.AUXILIARY, 1),
                new DaemonPoolSizing.PoolSpec("service", DaemonPoolSizing.Role.SERVICE, 1));

        assertThat(map.get("aux"), is(2));
        assertThat(map.get("service"), is(8));
        }

    @Test
    public void shouldHonorMinimumsWhenBudgetIsOvercommitted()
        {
        DaemonPoolSizing.Coordinator coordinator = coordinator(100, 1000);
        Object oPoolOne = new Object();
        Object oPoolTwo = new Object();

        DaemonPoolSizing.Registration poolOne = coordinator.register(oPoolOne, "one",
                DaemonPoolSizing.Role.SERVICE, 60, Integer.MAX_VALUE);
        DaemonPoolSizing.Registration poolTwo = coordinator.register(oPoolTwo, "two",
                DaemonPoolSizing.Role.SERVICE, 60, Integer.MAX_VALUE);

        assertThat(poolOne.getEffectiveMax(), is(60));
        assertThat(poolTwo.getEffectiveMax(), is(60));
        assertThat(coordinator.snapshot().isOvercommitted(), is(true));
        }

    @Test
    public void shouldReduceAutomaticCapacityByExplicitUsage()
        {
        DaemonPoolSizing.Coordinator coordinator = coordinator(100, 1000);
        DaemonPoolSizing.Registration explicit = coordinator.register(new Object(), "explicit",
                DaemonPoolSizing.Role.SERVICE, 10, 80);
        assertThat(explicit.requestCount(60), is(60));
        explicit.workersStarted(60);

        DaemonPoolSizing.Registration automatic = coordinator.register(new Object(), "automatic",
                DaemonPoolSizing.Role.SERVICE, 10, Integer.MAX_VALUE);

        assertThat(explicit.getEffectiveMax(), is(80));
        assertThat(automatic.getEffectiveMax(), is(40));
        }

    @Test
    public void shouldReallocateOnLateRegistrationAndShutdown()
        {
        DaemonPoolSizing.Coordinator coordinator = coordinator(100, 1000);
        DaemonPoolSizing.Registration first = coordinator.register(new Object(), "first",
                DaemonPoolSizing.Role.SERVICE, 10, Integer.MAX_VALUE);

        assertThat(first.getEffectiveMax(), is(100));
        assertThat(first.requestCount(80), is(80));
        first.workersStarted(80);

        DaemonPoolSizing.Registration second = coordinator.register(new Object(), "second",
                DaemonPoolSizing.Role.SERVICE, 10, Integer.MAX_VALUE);

        assertThat(first.getEffectiveMax(), is(50));
        assertThat(second.getEffectiveMax(), is(50));
        assertThat(first.requestCount(100), is(50));

        second.close();
        assertThat(first.getEffectiveMax(), is(100));
        }

    @Test
    public void shouldEnforceAggregateReservationsAcrossPools()
        {
        DaemonPoolSizing.Coordinator coordinator = coordinator(100, 1000);
        DaemonPoolSizing.Registration first = coordinator.register(new Object(), "first",
                DaemonPoolSizing.Role.SERVICE, 10, Integer.MAX_VALUE);
        DaemonPoolSizing.Registration second = coordinator.register(new Object(), "second",
                DaemonPoolSizing.Role.SERVICE, 10, Integer.MAX_VALUE);

        assertThat(first.requestCount(100), is(50));
        assertThat(second.requestCount(100), is(50));
        assertThat(coordinator.snapshot().getWorkerCount(), is(100));

        first.workersFailed(20);
        assertThat(coordinator.snapshot().getWorkerCount(), is(80));
        }

    @Test
    public void shouldAllocateIndependentlyOfRegistrationOrder()
        {
        DaemonPoolSizing.Coordinator firstOrder = coordinator(101, 1000);
        DaemonPoolSizing.Registration firstA = firstOrder.register(new Object(), "a",
                DaemonPoolSizing.Role.SERVICE, 10, Integer.MAX_VALUE);
        DaemonPoolSizing.Registration firstB = firstOrder.register(new Object(), "b",
                DaemonPoolSizing.Role.SERVICE, 10, Integer.MAX_VALUE);

        DaemonPoolSizing.Coordinator secondOrder = coordinator(101, 1000);
        DaemonPoolSizing.Registration secondB = secondOrder.register(new Object(), "b",
                DaemonPoolSizing.Role.SERVICE, 10, Integer.MAX_VALUE);
        DaemonPoolSizing.Registration secondA = secondOrder.register(new Object(), "a",
                DaemonPoolSizing.Role.SERVICE, 10, Integer.MAX_VALUE);

        assertThat(firstA.getEffectiveMax(), is(secondA.getEffectiveMax()));
        assertThat(firstB.getEffectiveMax(), is(secondB.getEffectiveMax()));
        }

    @Test
    public void shouldDeriveConservativeFallbackBudget()
        {
        long cbGiB = 1024L * 1024L * 1024L;
        DaemonPoolSizing.Budget budget = DaemonPoolSizing.deriveBudget(cbGiB, 1024L * 1024L,
                -1L, -1L, -1L, 2048, 8);

        assertThat(budget.getMemoryCap(), is(256));
        assertThat(budget.getWorkerBudget(), is(256));
        }

    @Test
    public void shouldApplyStableMemoryAndVmaLimitsWithoutOverflow()
        {
        long cbGiB = 1024L * 1024L * 1024L;
        DaemonPoolSizing.Budget memory = DaemonPoolSizing.deriveBudget(Long.MAX_VALUE,
                1024L * 1024L, 2L * cbGiB, -1L, -1L, 2048, 8);
        DaemonPoolSizing.Budget vma = DaemonPoolSizing.deriveBudget(cbGiB,
                1024L * 1024L, -1L, 65_530L, 64_343L, 2048, 8);

        assertThat(memory.getWorkerBudget(), is(1));
        assertThat(vma.getVmaCap(), is(0));
        assertThat(vma.getWorkerBudget(), is(1));
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

    private static DaemonPoolSizing.Coordinator coordinator(int cBudget, int cProcessors)
        {
        return new DaemonPoolSizing.Coordinator(new DaemonPoolSizing.Budget(cBudget,
                cBudget, -1, cBudget, cProcessors, -1L, -1L, 1024L * 1024L));
        }
    }
