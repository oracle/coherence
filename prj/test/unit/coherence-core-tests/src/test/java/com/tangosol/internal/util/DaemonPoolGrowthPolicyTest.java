/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit tests for {@link DaemonPoolGrowthPolicy}.
 *
 * @author Aleks Seovic  2026.08.14
 * @since 26.07
 */
public class DaemonPoolGrowthPolicyTest
    {
    @Test
    public void shouldGrowQuicklyOnlyToCpuKnee()
        {
        DaemonPoolGrowthPolicy policy = policy();

        DaemonPoolGrowthPolicy.Decision decision = policy.requestGrowth(
                sample(1000L, 2, 1, 40, 5, 100, 0, 1.0d, 1000.0d,
                        DaemonPoolSizing.Role.SERVICE), 2);

        assertThat(decision.getAction(), is(DaemonPoolGrowthPolicy.Decision.Action.GROW));
        assertThat(decision.getDelta(), is(2));
        assertThat(decision.isProbe(), is(false));

        decision = policy.requestGrowth(
                sample(2000L, 4, 1, 40, 5, 100, 0, 1.0d, 1000.0d,
                        DaemonPoolSizing.Role.SERVICE), 4);

        assertThat(decision.getDelta(), is(1));
        assertThat(decision.isProbe(), is(false));
        }

    @Test
    public void shouldSuppressCpuBoundGrowthAtCpuKnee()
        {
        DaemonPoolGrowthPolicy.Decision decision = policy().requestGrowth(
                sample(1000L, 5, 1, 40, 5, 100, 20, 0.96d, 1000.0d,
                        DaemonPoolSizing.Role.SERVICE), 5);

        assertThat(decision.getAction(), is(DaemonPoolGrowthPolicy.Decision.Action.HOLD));
        assertThat(decision.getReason(), is("CPU-bound work at the CPU allocation"));
        }

    @Test
    public void shouldSuppressGrowthAtConfiguredMaximum()
        {
        DaemonPoolGrowthPolicy.Decision decision = policy().requestGrowth(
                sample(1000L, 5, 1, 5, 5, 100, 20, 0.10d, 1000.0d,
                        DaemonPoolSizing.Role.BLOCKING_IO), 1);

        assertThat(decision.getAction(), is(DaemonPoolGrowthPolicy.Decision.Action.HOLD));
        assertThat(decision.getReason(), is("the configured maximum worker count"));
        }

    @Test
    public void shouldSuppressAssociationLimitedGrowthAtCpuKnee()
        {
        DaemonPoolGrowthPolicy.Decision decision = policy().requestGrowth(
                sample(1000L, 5, 1, 40, 5, 100, 5, 0.10d, 1000.0d,
                        DaemonPoolSizing.Role.SERVICE), 5);

        assertThat(decision.getAction(), is(DaemonPoolGrowthPolicy.Decision.Action.HOLD));
        assertThat(decision.getReason(), is("association-limited work at the CPU allocation"));
        }

    @Test
    public void shouldProbeAboveCpuKneeForMeasuredBlocking()
        {
        DaemonPoolGrowthPolicy.Decision decision = policy().requestGrowth(
                sample(1000L, 5, 1, 40, 5, 100, 20, 0.25d, 1000.0d,
                        DaemonPoolSizing.Role.SERVICE), 5);

        assertThat(decision.getAction(), is(DaemonPoolGrowthPolicy.Decision.Action.GROW));
        assertThat(decision.getDelta(), is(1));
        assertThat(decision.isProbe(), is(true));
        }

    @Test
    public void shouldPermitBlockingIoProbeWhenCpuTimeIsUnavailable()
        {
        DaemonPoolGrowthPolicy.Decision decision = policy().requestGrowth(
                sample(1000L, 50, 50, 160, 5, 100, 0, -1.0d, 1000.0d,
                        DaemonPoolSizing.Role.BLOCKING_IO), 50);

        assertThat(decision.getAction(), is(DaemonPoolGrowthPolicy.Decision.Action.GROW));
        assertThat(decision.getDelta(), is(1));
        assertThat(decision.isProbe(), is(true));
        }

    @Test
    public void shouldRevertProbeWithoutMarginalBenefit()
        {
        DaemonPoolGrowthPolicy policy   = policy();
        DaemonPoolGrowthPolicy.Sample sample = sample(1000L, 5, 1, 40, 5, 100, 20,
                0.25d, 1000.0d, DaemonPoolSizing.Role.SERVICE);

        DaemonPoolGrowthPolicy.Decision decision = policy.requestGrowth(sample, 5);
        policy.onProbeApplied(sample, 6);

        decision = policy.evaluateProbe(sample(2000L, 6, 1, 40, 5, 100, 20,
                0.30d, 1005.0d, DaemonPoolSizing.Role.SERVICE));
        assertThat(decision.getAction(), is(DaemonPoolGrowthPolicy.Decision.Action.HOLD));

        decision = policy.evaluateProbe(sample(3000L, 6, 1, 40, 5, 100, 20,
                0.30d, 1000.0d, DaemonPoolSizing.Role.SERVICE));
        assertThat(decision.getAction(), is(DaemonPoolGrowthPolicy.Decision.Action.REVERT));
        assertThat(policy.isProbeActive(), is(false));

        decision = policy.requestGrowth(sample(4000L, 5, 1, 40, 5, 100, 20,
                0.25d, 1000.0d, DaemonPoolSizing.Role.SERVICE), 5);
        assertThat(decision.getAction(), is(DaemonPoolGrowthPolicy.Decision.Action.HOLD));
        assertThat(decision.getReason(), is("the above-CPU worker probe cooldown"));
        }

    @Test
    public void shouldRetainProbeWithMarginalBenefit()
        {
        DaemonPoolGrowthPolicy policy   = policy();
        DaemonPoolGrowthPolicy.Sample sample = sample(1000L, 5, 1, 40, 5, 100, 20,
                0.25d, 1000.0d, DaemonPoolSizing.Role.SERVICE);

        policy.onProbeApplied(sample, 6);
        policy.evaluateProbe(sample(2000L, 6, 1, 40, 5, 100, 20,
                0.30d, 1030.0d, DaemonPoolSizing.Role.SERVICE));

        DaemonPoolGrowthPolicy.Decision decision = policy.evaluateProbe(
                sample(3000L, 6, 1, 40, 5, 100, 20, 0.30d, 1020.0d,
                        DaemonPoolSizing.Role.SERVICE));

        assertThat(decision.getAction(), is(DaemonPoolGrowthPolicy.Decision.Action.NONE));
        assertThat(policy.isProbeActive(), is(false));
        }

    private static DaemonPoolGrowthPolicy policy()
        {
        return new DaemonPoolGrowthPolicy(0.90d, 2, 0.02d, 30000L);
        }

    private static DaemonPoolGrowthPolicy.Sample sample(long ldtTimestamp, int cThreads,
            int cThreadsMin, int cThreadsMax, int cProcessors, int cBacklog,
            int cAssociations, double dflCpuRatio, double dflThroughput,
            DaemonPoolSizing.Role role)
        {
        return new DaemonPoolGrowthPolicy.Sample(ldtTimestamp, cThreads, cThreadsMin,
                cThreadsMax, cProcessors, cBacklog, cAssociations, dflCpuRatio,
                dflThroughput, role);
        }
    }
