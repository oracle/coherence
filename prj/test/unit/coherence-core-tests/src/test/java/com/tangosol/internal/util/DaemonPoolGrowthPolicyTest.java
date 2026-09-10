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

        assertThat(decision.getReason(), decision.getAction(),
                is(DaemonPoolGrowthPolicy.Decision.Action.GROW));
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
        DaemonPoolGrowthPolicy policy = policy();
        prime(policy, sample(1000L, 5, 1, 40, 5, 100, 20, 0.25d, 1000.0d,
                DaemonPoolSizing.Role.SERVICE));
        DaemonPoolGrowthPolicy.Decision decision = policy.requestGrowth(
                sample(1000L, 5, 1, 40, 5, 100, 20, 0.25d, 1000.0d,
                        DaemonPoolSizing.Role.SERVICE), 5);

        assertThat(decision.getAction(), is(DaemonPoolGrowthPolicy.Decision.Action.GROW));
        assertThat(decision.getDelta(), is(2));
        assertThat(decision.isProbe(), is(true));
        }

    @Test
    public void shouldPermitBlockingIoProbeWhenCpuTimeIsUnavailable()
        {
        DaemonPoolGrowthPolicy policy = policy();
        prime(policy, sample(1000L, 50, 50, 160, 5, 100, 0, -1.0d, 1000.0d,
                DaemonPoolSizing.Role.BLOCKING_IO));
        DaemonPoolGrowthPolicy.Decision decision = policy.requestGrowth(
                sample(1000L, 50, 50, 160, 5, 100, 0, -1.0d, 1000.0d,
                        DaemonPoolSizing.Role.BLOCKING_IO), 50);

        assertThat(decision.getAction(), is(DaemonPoolGrowthPolicy.Decision.Action.GROW));
        assertThat(decision.getDelta(), is(16));
        assertThat(decision.isProbe(), is(true));
        }

    @Test
    public void shouldRevertProbeWithoutMarginalBenefit()
        {
        DaemonPoolGrowthPolicy policy   = policy();
        DaemonPoolGrowthPolicy.Sample sample = sample(1000L, 5, 1, 40, 5, 100, 20,
                0.25d, 1000.0d, DaemonPoolSizing.Role.SERVICE);

        prime(policy, sample);
        DaemonPoolGrowthPolicy.Decision decision = policy.requestGrowth(sample, 5);
        assertThat(decision.getDelta(), is(2));
        policy.onProbeApplied(sample, 7);

        decision = policy.evaluateProbe(sample(2000L, 7, 1, 40, 5, 100, 20,
                0.30d, 1005.0d, DaemonPoolSizing.Role.SERVICE));
        assertThat(decision.getAction(), is(DaemonPoolGrowthPolicy.Decision.Action.HOLD));

        decision = policy.evaluateProbe(sample(3000L, 7, 1, 40, 5, 100, 20,
                0.30d, 1000.0d, DaemonPoolSizing.Role.SERVICE));
        assertThat(decision.getAction(), is(DaemonPoolGrowthPolicy.Decision.Action.RESIZE));
        assertThat(decision.getDelta(), is(-2));
        assertThat(policy.isProbeActive(), is(false));

        decision = policy.requestGrowth(sample(4000L, 5, 1, 40, 5, 100, 20,
                0.25d, 1000.0d, DaemonPoolSizing.Role.SERVICE), 5);
        assertThat(decision.getAction(), is(DaemonPoolGrowthPolicy.Decision.Action.HOLD));
        assertThat(decision.getReason(), is("the above-CPU worker probe cooldown"));
        }

    @Test
    public void shouldRememberRejectedUpperProbeUntilWorkloadChanges()
        {
        DaemonPoolGrowthPolicy policy = policy();
        DaemonPoolGrowthPolicy.Sample baseline = sample(1000L, 16, 1, 512, 16,
                100, 100, 0.10d, 1000.0d, DaemonPoolSizing.Role.BLOCKING_IO);

        prime(policy, baseline);
        DaemonPoolGrowthPolicy.Decision decision = policy.requestGrowth(baseline, 16);
        policy.onProbeApplied(baseline, 24);
        policy.evaluateProbe(sample(2000L, 24, 1, 512, 16, 100, 100,
                0.10d, 1000.0d, DaemonPoolSizing.Role.BLOCKING_IO));
        policy.evaluateProbe(sample(3000L, 24, 1, 512, 16, 100, 100,
                0.10d, 1000.0d, DaemonPoolSizing.Role.BLOCKING_IO));

        decision = policy.requestGrowth(sample(40000L, 16, 1, 512, 16,
                100, 100, 0.10d, 1000.0d, DaemonPoolSizing.Role.BLOCKING_IO), 16);
        assertThat(decision.getAction(), is(DaemonPoolGrowthPolicy.Decision.Action.HOLD));
        assertThat(decision.getReason(),
                is("the learned worker-probe bound for this workload"));

        policy.evaluateProbe(sample(41000L, 16, 1, 512, 16, 100, 100,
                0.10d, 1300.0d, DaemonPoolSizing.Role.BLOCKING_IO));
        policy.evaluateProbe(sample(42000L, 16, 1, 512, 16, 100, 100,
                0.10d, 1300.0d, DaemonPoolSizing.Role.BLOCKING_IO));
        policy.evaluateProbe(sample(43000L, 16, 1, 512, 16, 100, 100,
                0.10d, 1300.0d, DaemonPoolSizing.Role.BLOCKING_IO));
        decision = policy.requestGrowth(sample(43000L, 16, 1, 512, 16,
                100, 100, 0.10d, 1300.0d, DaemonPoolSizing.Role.BLOCKING_IO), 16);
        assertThat(decision.getAction(), is(DaemonPoolGrowthPolicy.Decision.Action.GROW));
        }

    @Test
    public void shouldRetainProbeWithMarginalBenefit()
        {
        DaemonPoolGrowthPolicy policy   = policy();
        DaemonPoolGrowthPolicy.Sample sample = sample(1000L, 5, 1, 40, 5, 100, 20,
                0.25d, 1000.0d, DaemonPoolSizing.Role.SERVICE);

        prime(policy, sample);
        policy.onProbeApplied(sample, 6);
        policy.evaluateProbe(sample(2000L, 6, 1, 40, 5, 100, 20,
                0.30d, 1070.0d, DaemonPoolSizing.Role.SERVICE));

        DaemonPoolGrowthPolicy.Decision decision = policy.evaluateProbe(
                sample(3000L, 6, 1, 40, 5, 100, 20, 0.30d, 1060.0d,
                        DaemonPoolSizing.Role.SERVICE));

        assertThat(decision.getReason(), decision.getAction(),
                is(DaemonPoolGrowthPolicy.Decision.Action.NONE));
        assertThat(policy.isProbeActive(), is(false));
        }

    @Test
    public void shouldRequireFreshBaselineAfterRetainingProbe()
        {
        DaemonPoolGrowthPolicy policy = policy();
        DaemonPoolGrowthPolicy.Sample baseline = sample(1000L, 5, 1, 40, 5,
                100, 20, 0.25d, 1000.0d, DaemonPoolSizing.Role.SERVICE);

        prime(policy, baseline);
        policy.onProbeApplied(baseline, 6);
        policy.evaluateProbe(sample(2000L, 6, 1, 40, 5, 100, 20,
                0.25d, 1060.0d, DaemonPoolSizing.Role.SERVICE));
        policy.evaluateProbe(sample(3000L, 6, 1, 40, 5, 100, 20,
                0.25d, 1060.0d, DaemonPoolSizing.Role.SERVICE));

        DaemonPoolGrowthPolicy.Decision decision = policy.requestGrowth(
                sample(3000L, 6, 1, 40, 5, 100, 20,
                        0.25d, 1060.0d, DaemonPoolSizing.Role.SERVICE), 6);
        assertThat(decision.getAction(), is(DaemonPoolGrowthPolicy.Decision.Action.HOLD));
        assertThat(decision.getReason(), is("waiting for a stable throughput baseline"));

        policy.evaluateProbe(sample(4000L, 6, 1, 40, 5, 100, 20,
                0.25d, 1060.0d, DaemonPoolSizing.Role.SERVICE));
        policy.evaluateProbe(sample(5000L, 6, 1, 40, 5, 100, 20,
                0.25d, 1060.0d, DaemonPoolSizing.Role.SERVICE));

        decision = policy.requestGrowth(sample(6000L, 6, 1, 40, 5, 100, 20,
                0.25d, 1060.0d, DaemonPoolSizing.Role.SERVICE), 6);
        assertThat(decision.getAction(), is(DaemonPoolGrowthPolicy.Decision.Action.GROW));
        }

    @Test
    public void shouldNotRejectAnUpwardProbeAcrossAnIdleWorkloadGap()
        {
        DaemonPoolGrowthPolicy policy = policy();
        DaemonPoolGrowthPolicy.Sample baseline = sample(1000L, 16, 1, 512, 16,
                16, 100, 0, 0.10d, 1000.0d, DaemonPoolSizing.Role.BLOCKING_IO);

        prime(policy, baseline);
        policy.onProbeApplied(baseline, 24);

        DaemonPoolGrowthPolicy.Decision decision = policy.evaluateProbe(
                sample(2000L, 24, 1, 512, 16,
                        0, 0, 0, 0.0d, 0.0d,
                        DaemonPoolSizing.Role.BLOCKING_IO));
        assertThat(decision.getAction(), is(DaemonPoolGrowthPolicy.Decision.Action.HOLD));
        assertThat(policy.isProbeActive(), is(true));

        decision = policy.evaluateProbe(sample(3000L, 24, 1, 512, 16,
                24, 100, 0, 0.10d, 1060.0d,
                DaemonPoolSizing.Role.BLOCKING_IO));
        assertThat(decision.getAction(), is(DaemonPoolGrowthPolicy.Decision.Action.HOLD));

        decision = policy.evaluateProbe(sample(4000L, 24, 1, 512, 16,
                24, 100, 0, 0.10d, 1060.0d,
                DaemonPoolSizing.Role.BLOCKING_IO));
        assertThat(decision.getAction(), is(DaemonPoolGrowthPolicy.Decision.Action.NONE));
        assertThat(policy.isProbeActive(), is(false));
        }

    @Test
    public void shouldAbandonAnUpwardProbeAfterAStableIdleWorkloadGap()
        {
        DaemonPoolGrowthPolicy policy = policy();
        DaemonPoolGrowthPolicy.Sample baseline = sample(1000L, 16, 1, 512, 16,
                16, 100, 0, 0.10d, 1000.0d, DaemonPoolSizing.Role.BLOCKING_IO);

        prime(policy, baseline);
        policy.onProbeApplied(baseline, 24);

        policy.evaluateProbe(sample(2000L, 24, 1, 512, 16,
                0, 0, 0, 0.0d, 0.0d, DaemonPoolSizing.Role.BLOCKING_IO));
        DaemonPoolGrowthPolicy.Decision decision = policy.evaluateProbe(
                sample(3000L, 24, 1, 512, 16,
                        0, 0, 0, 0.0d, 0.0d,
                        DaemonPoolSizing.Role.BLOCKING_IO));

        assertThat(decision.getAction(), is(DaemonPoolGrowthPolicy.Decision.Action.NONE));
        assertThat(decision.getReason(),
                is("the worker-count probe was abandoned across an idle workload"));
        assertThat(policy.isProbeActive(), is(false));
        }

    @Test
    public void shouldRevertWholeBatchedProbe()
        {
        DaemonPoolGrowthPolicy policy = policy();
        DaemonPoolGrowthPolicy.Sample baseline = sample(1000L, 16, 1, 512, 16,
                100, 100, 0.10d, 1000.0d, DaemonPoolSizing.Role.BLOCKING_IO);

        prime(policy, baseline);
        DaemonPoolGrowthPolicy.Decision decision = policy.requestGrowth(baseline, 16);
        assertThat(decision.getDelta(), is(8));
        policy.onProbeApplied(baseline, 24);

        policy.evaluateProbe(sample(2000L, 24, 1, 512, 16, 100, 100,
                0.10d, 1005.0d, DaemonPoolSizing.Role.BLOCKING_IO));
        decision = policy.evaluateProbe(sample(3000L, 24, 1, 512, 16, 100, 100,
                0.10d, 1000.0d, DaemonPoolSizing.Role.BLOCKING_IO));

        assertThat(decision.getAction(), is(DaemonPoolGrowthPolicy.Decision.Action.RESIZE));
        assertThat(decision.getDelta(), is(-8));
        }

    @Test
    public void shouldProbeSustainedOffCpuWorkDuringAnUnstableRamp()
        {
        DaemonPoolGrowthPolicy policy = policy();

        policy.evaluateProbe(sample(1000L, 16, 1, 512, 16, 100, 100,
                0.10d, 1000.0d, DaemonPoolSizing.Role.BLOCKING_IO));
        policy.evaluateProbe(sample(2000L, 16, 1, 512, 16, 100, 100,
                0.10d, 1400.0d, DaemonPoolSizing.Role.BLOCKING_IO));
        policy.evaluateProbe(sample(3000L, 16, 1, 512, 16, 100, 100,
                0.10d, 1900.0d, DaemonPoolSizing.Role.BLOCKING_IO));

        DaemonPoolGrowthPolicy.Decision decision = policy.requestGrowth(
                sample(3000L, 16, 1, 512, 16, 100, 100,
                        0.10d, 1900.0d, DaemonPoolSizing.Role.BLOCKING_IO), 16);
        assertThat(decision.getAction(), is(DaemonPoolGrowthPolicy.Decision.Action.GROW));
        assertThat(decision.getDelta(), is(8));
        assertThat(decision.isProbe(), is(true));
        }

    @Test
    public void shouldInvalidateRejectedProbeAfterMaterialQueuedPressureIncrease()
        {
        DaemonPoolGrowthPolicy policy = policy();
        DaemonPoolGrowthPolicy.Sample baseline = sample(1000L, 16, 1, 512, 16,
                10, 0, 0.10d, 1000.0d, DaemonPoolSizing.Role.BLOCKING_IO);

        prime(policy, baseline);
        policy.onProbeApplied(baseline, 24);
        policy.evaluateProbe(sample(2000L, 24, 1, 512, 16,
                0, 0, 0.10d, 1000.0d, DaemonPoolSizing.Role.BLOCKING_IO));
        policy.evaluateProbe(sample(3000L, 24, 1, 512, 16,
                0, 0, 0.10d, 1000.0d, DaemonPoolSizing.Role.BLOCKING_IO));

        policy.evaluateProbe(sample(4000L, 16, 1, 512, 16,
                200, 0, 0.10d, 900.0d, DaemonPoolSizing.Role.BLOCKING_IO));
        policy.evaluateProbe(sample(5000L, 16, 1, 512, 16,
                200, 0, 0.10d, 1400.0d, DaemonPoolSizing.Role.BLOCKING_IO));
        policy.evaluateProbe(sample(6000L, 16, 1, 512, 16,
                200, 0, 0.10d, 800.0d, DaemonPoolSizing.Role.BLOCKING_IO));

        DaemonPoolGrowthPolicy.Decision decision = policy.requestGrowth(
                sample(6000L, 16, 1, 512, 16,
                        200, 0, 0.10d, 800.0d,
                        DaemonPoolSizing.Role.BLOCKING_IO), 16);
        assertThat(decision.getReason(), decision.getAction(),
                is(DaemonPoolGrowthPolicy.Decision.Action.GROW));
        assertThat(decision.getDelta(), is(8));
        assertThat(decision.isProbe(), is(true));
        }

    @Test
    public void shouldProbeBlockedWorkWithoutRunnableBacklog()
        {
        DaemonPoolGrowthPolicy policy = policy();
        DaemonPoolGrowthPolicy.Sample sample = sample(1000L, 16, 1, 512, 16,
                0, 0, 0.10d, 1000.0d, DaemonPoolSizing.Role.BLOCKING_IO);

        assertThat(policy.evaluateProbe(sample).getAction(),
                is(DaemonPoolGrowthPolicy.Decision.Action.NONE));
        assertThat(policy.evaluateProbe(sample).getAction(),
                is(DaemonPoolGrowthPolicy.Decision.Action.NONE));
        DaemonPoolGrowthPolicy.Decision decision = policy.evaluateProbe(sample);

        assertThat(decision.getAction(), is(DaemonPoolGrowthPolicy.Decision.Action.RESIZE));
        assertThat(decision.getDelta(), is(8));
        assertThat(policy.isProbeActive(), is(true));
        }

    @Test
    public void shouldNotProbeCpuBoundWorkWithoutRunnableBacklog()
        {
        DaemonPoolGrowthPolicy policy = policy();
        DaemonPoolGrowthPolicy.Sample sample = sample(1000L, 16, 1, 512, 16,
                0, 0, 0.95d, 1000.0d, DaemonPoolSizing.Role.SERVICE);

        policy.evaluateProbe(sample);
        policy.evaluateProbe(sample);
        DaemonPoolGrowthPolicy.Decision decision = policy.evaluateProbe(sample);

        assertThat(decision.getAction(), is(DaemonPoolGrowthPolicy.Decision.Action.NONE));
        assertThat(policy.isProbeActive(), is(false));
        }

    @Test
    public void shouldProbeDownAfterAStableWorkloadShapeChange()
        {
        DaemonPoolGrowthPolicy policy = policy();
        establishReference(policy, sample(1000L, 128, 1, 512, 16, 100, 100,
                0.10d, 1000.0d, DaemonPoolSizing.Role.BLOCKING_IO));

        policy.evaluateProbe(sample(4000L, 128, 1, 512, 16, 100, 0, 0,
                0.10d, 600.0d, DaemonPoolSizing.Role.BLOCKING_IO));
        policy.evaluateProbe(sample(5000L, 128, 1, 512, 16, 100, 0, 0,
                0.10d, 600.0d, DaemonPoolSizing.Role.BLOCKING_IO));
        DaemonPoolGrowthPolicy.Decision decision = policy.evaluateProbe(
                sample(6000L, 128, 1, 512, 16, 100, 0, 0,
                        0.10d, 600.0d, DaemonPoolSizing.Role.BLOCKING_IO));

        assertThat(decision.getAction(), is(DaemonPoolGrowthPolicy.Decision.Action.RESIZE));
        assertThat(decision.getDelta(), is(-64));
        assertThat(policy.isProbeActive(), is(true));
        }

    @Test
    public void shouldProbeDownAfterAWorkloadShapeChangeWithQueuedWork()
        {
        DaemonPoolGrowthPolicy policy = policy();
        establishReference(policy, sample(1000L, 128, 1, 512, 16, 100, 100,
                0.10d, 1000.0d, DaemonPoolSizing.Role.BLOCKING_IO));

        policy.evaluateProbe(sample(4000L, 128, 1, 512, 16, 128, 100,
                0.10d, 600.0d, DaemonPoolSizing.Role.BLOCKING_IO));
        policy.evaluateProbe(sample(5000L, 128, 1, 512, 16, 128, 100,
                0.10d, 600.0d, DaemonPoolSizing.Role.BLOCKING_IO));
        DaemonPoolGrowthPolicy.Decision decision = policy.evaluateProbe(
                sample(6000L, 128, 1, 512, 16, 128, 100,
                        0.10d, 600.0d, DaemonPoolSizing.Role.BLOCKING_IO));

        assertThat(decision.getAction(), is(DaemonPoolGrowthPolicy.Decision.Action.RESIZE));
        assertThat(decision.getDelta(), is(-64));
        assertThat(policy.isProbeActive(), is(true));
        }

    @Test
    public void shouldNotProbeDownWhenStableThroughputIncreases()
        {
        DaemonPoolGrowthPolicy policy = policy();
        establishReference(policy, sample(1000L, 128, 1, 512, 16, 100, 100,
                0.10d, 1000.0d, DaemonPoolSizing.Role.BLOCKING_IO));

        policy.evaluateProbe(sample(4000L, 128, 1, 512, 16, 100, 0, 0,
                0.10d, 1400.0d, DaemonPoolSizing.Role.BLOCKING_IO));
        policy.evaluateProbe(sample(5000L, 128, 1, 512, 16, 100, 0, 0,
                0.10d, 1400.0d, DaemonPoolSizing.Role.BLOCKING_IO));
        DaemonPoolGrowthPolicy.Decision decision = policy.evaluateProbe(
                sample(6000L, 128, 1, 512, 16, 100, 0, 0,
                        0.10d, 1400.0d, DaemonPoolSizing.Role.BLOCKING_IO));

        assertThat(decision.getReason(), decision.getDelta() >= 0, is(true));
        }

    @Test
    public void shouldProbeDownWhenStablePoolIsUnderOccupied()
        {
        DaemonPoolGrowthPolicy policy = policy();
        DaemonPoolGrowthPolicy.Sample sample = sample(1000L, 192, 1, 512, 16,
                64, 0, 0, 0.10d, 1000.0d, DaemonPoolSizing.Role.BLOCKING_IO);

        assertThat(policy.evaluateProbe(sample).getAction(),
                is(DaemonPoolGrowthPolicy.Decision.Action.NONE));
        assertThat(policy.evaluateProbe(sample).getAction(),
                is(DaemonPoolGrowthPolicy.Decision.Action.NONE));
        DaemonPoolGrowthPolicy.Decision decision = policy.evaluateProbe(sample);

        assertThat(decision.getAction(), is(DaemonPoolGrowthPolicy.Decision.Action.RESIZE));
        assertThat(decision.getDelta(), is(-96));
        assertThat(decision.getReason(), is("a stable under-occupied worker pool"));
        assertThat(policy.isProbeActive(), is(true));
        }

    @Test
    public void shouldImmediatelyRestoreLowerProbeThatCreatesOffCpuSaturation()
        {
        DaemonPoolGrowthPolicy policy = policy();
        DaemonPoolGrowthPolicy.Sample sample = sample(1000L, 128, 1, 512, 16,
                40, 0, 0, 0.10d, 1000.0d, DaemonPoolSizing.Role.BLOCKING_IO);

        prime(policy, sample);
        DaemonPoolGrowthPolicy.Decision decision = policy.evaluateProbe(
                sample(2000L, 64, 1, 512, 16,
                        64, 100, 0, 0.10d, 1000.0d,
                        DaemonPoolSizing.Role.BLOCKING_IO));

        assertThat(decision.getAction(), is(DaemonPoolGrowthPolicy.Decision.Action.RESIZE));
        assertThat(decision.getDelta(), is(64));
        assertThat(decision.getReason(),
                is("the lower-worker probe created off-CPU saturation"));
        assertThat(policy.isProbeActive(), is(false));
        }

    @Test
    public void shouldAllowCpuBoundLowerProbeToUseThroughputEvidence()
        {
        DaemonPoolGrowthPolicy policy = policy();
        DaemonPoolGrowthPolicy.Sample sample = sample(1000L, 128, 1, 512, 16,
                40, 0, 0, 0.95d, 1000.0d, DaemonPoolSizing.Role.SERVICE);

        prime(policy, sample);
        DaemonPoolGrowthPolicy.Decision decision = policy.evaluateProbe(
                sample(2000L, 64, 1, 512, 16,
                        64, 100, 0, 0.95d, 1000.0d, DaemonPoolSizing.Role.SERVICE));
        assertThat(decision.getAction(), is(DaemonPoolGrowthPolicy.Decision.Action.HOLD));

        decision = policy.evaluateProbe(sample(3000L, 64, 1, 512, 16,
                64, 100, 0, 0.95d, 1000.0d, DaemonPoolSizing.Role.SERVICE));
        assertThat(decision.getAction(), is(DaemonPoolGrowthPolicy.Decision.Action.NONE));
        assertThat(policy.isProbeActive(), is(false));
        }

    @Test
    public void shouldRestoreRecentlyProvenWindowAfterDemandRedistribution()
        {
        DaemonPoolGrowthPolicy policy = policy();
        DaemonPoolGrowthPolicy.Sample sample = sample(1000L, 128, 1, 512, 16,
                40, 0, 0, 0.10d, 1000.0d, DaemonPoolSizing.Role.BLOCKING_IO);

        prime(policy, sample);
        policy.evaluateProbe(sample(2000L, 64, 1, 512, 16,
                40, 0, 0, 0.10d, 1000.0d, DaemonPoolSizing.Role.BLOCKING_IO));
        assertThat(policy.evaluateProbe(sample(3000L, 64, 1, 512, 16,
                40, 0, 0, 0.10d, 1000.0d,
                DaemonPoolSizing.Role.BLOCKING_IO)).getAction(),
                is(DaemonPoolGrowthPolicy.Decision.Action.NONE));

        policy.evaluateProbe(sample(4000L, 64, 1, 512, 16,
                64, 100, 0, 0.10d, 1000.0d, DaemonPoolSizing.Role.BLOCKING_IO));
        policy.evaluateProbe(sample(5000L, 64, 1, 512, 16,
                64, 100, 0, 0.10d, 1400.0d, DaemonPoolSizing.Role.BLOCKING_IO));
        policy.evaluateProbe(sample(6000L, 64, 1, 512, 16,
                64, 100, 0, 0.10d, 900.0d, DaemonPoolSizing.Role.BLOCKING_IO));

        DaemonPoolGrowthPolicy.Decision decision = policy.requestGrowth(
                sample(6000L, 64, 1, 512, 16,
                        64, 100, 0, 0.10d, 900.0d,
                        DaemonPoolSizing.Role.BLOCKING_IO), 64);
        assertThat(decision.getAction(), is(DaemonPoolGrowthPolicy.Decision.Action.GROW));
        assertThat(decision.getDelta(), is(64));
        assertThat(decision.isProbe(), is(false));
        assertThat(decision.getReason(),
                is("sustained off-CPU saturation restored a recently proven worker window"));
        }

    @Test
    public void shouldNotProbeDownForAStableOccupiedPoolButMayProbeUp()
        {
        DaemonPoolGrowthPolicy policy = policy();
        DaemonPoolGrowthPolicy.Sample sample = sample(1000L, 64, 1, 512, 16,
                56, 0, 0, 0.10d, 1000.0d, DaemonPoolSizing.Role.BLOCKING_IO);

        policy.evaluateProbe(sample);
        policy.evaluateProbe(sample);
        DaemonPoolGrowthPolicy.Decision decision = policy.evaluateProbe(sample);

        assertThat(decision.getAction(), is(DaemonPoolGrowthPolicy.Decision.Action.RESIZE));
        assertThat(decision.getDelta(), is(16));
        assertThat(decision.getReason(),
                is("stable off-CPU saturation without runnable backlog"));
        assertThat(policy.isProbeActive(), is(true));
        }

    @Test
    public void shouldRememberARejectedLowerProbe()
        {
        DaemonPoolGrowthPolicy policy = policy();
        DaemonPoolGrowthPolicy.Sample sample = sample(1000L, 64, 1, 512, 16,
                20, 0, 0, 0.10d, 1000.0d, DaemonPoolSizing.Role.BLOCKING_IO);

        prime(policy, sample);
        policy.evaluateProbe(sample(2000L, 32, 1, 512, 16,
                20, 0, 0, 0.10d, 900.0d, DaemonPoolSizing.Role.BLOCKING_IO));
        DaemonPoolGrowthPolicy.Decision decision = policy.evaluateProbe(
                sample(3000L, 32, 1, 512, 16,
                        20, 0, 0, 0.10d, 900.0d, DaemonPoolSizing.Role.BLOCKING_IO));

        assertThat(decision.getAction(), is(DaemonPoolGrowthPolicy.Decision.Action.RESIZE));
        assertThat(decision.getDelta(), is(32));

        decision = policy.evaluateProbe(sample(4000L, 64, 1, 512, 16,
                20, 0, 0, 0.10d, 1000.0d, DaemonPoolSizing.Role.BLOCKING_IO));
        assertThat(decision.getAction(), is(DaemonPoolGrowthPolicy.Decision.Action.NONE));
        }

    @Test
    public void shouldCompareAContinuingLowerProbeWithTheOriginalReference()
        {
        DaemonPoolGrowthPolicy policy = policy();
        DaemonPoolGrowthPolicy.Sample sample = sample(1000L, 128, 1, 512, 16,
                50, 0, 0, 0.10d, 1000.0d, DaemonPoolSizing.Role.BLOCKING_IO);

        prime(policy, sample);
        policy.evaluateProbe(sample(2000L, 64, 1, 512, 16,
                50, 0, 0, 0.10d, 990.0d, DaemonPoolSizing.Role.BLOCKING_IO));
        assertThat(policy.evaluateProbe(sample(3000L, 64, 1, 512, 16,
                50, 0, 0, 0.10d, 990.0d, DaemonPoolSizing.Role.BLOCKING_IO)).getAction(),
                is(DaemonPoolGrowthPolicy.Decision.Action.NONE));

        DaemonPoolGrowthPolicy.Decision decision = policy.evaluateProbe(
                sample(4000L, 64, 1, 512, 16,
                        50, 0, 0, 0.10d, 990.0d, DaemonPoolSizing.Role.BLOCKING_IO));
        assertThat(decision.getDelta(), is(-32));

        policy.evaluateProbe(sample(5000L, 32, 1, 512, 16,
                32, 0, 0, 0.10d, 975.0d, DaemonPoolSizing.Role.BLOCKING_IO));
        decision = policy.evaluateProbe(sample(6000L, 32, 1, 512, 16,
                32, 0, 0, 0.10d, 975.0d, DaemonPoolSizing.Role.BLOCKING_IO));

        assertThat(decision.getAction(), is(DaemonPoolGrowthPolicy.Decision.Action.RESIZE));
        assertThat(decision.getDelta(), is(32));
        }

    @Test
    public void shouldRestoreWorkersWhenDownProbeDoesNotHelp()
        {
        DaemonPoolGrowthPolicy policy = policy();
        establishReference(policy, sample(1000L, 128, 1, 512, 16, 100, 100,
                0.10d, 1000.0d, DaemonPoolSizing.Role.BLOCKING_IO));
        policy.evaluateProbe(sample(4000L, 128, 1, 512, 16, 100, 0, 0,
                0.10d, 600.0d, DaemonPoolSizing.Role.BLOCKING_IO));
        policy.evaluateProbe(sample(5000L, 128, 1, 512, 16, 100, 0, 0,
                0.10d, 600.0d, DaemonPoolSizing.Role.BLOCKING_IO));
        policy.evaluateProbe(sample(6000L, 128, 1, 512, 16, 100, 0, 0,
                0.10d, 600.0d, DaemonPoolSizing.Role.BLOCKING_IO));
        DaemonPoolGrowthPolicy.Decision decision = policy.evaluateProbe(
                sample(7000L, 64, 1, 512, 16, 64, 1, 0,
                        0.10d, 500.0d, DaemonPoolSizing.Role.BLOCKING_IO));
        assertThat(decision.getAction(), is(DaemonPoolGrowthPolicy.Decision.Action.RESIZE));
        assertThat(decision.getDelta(), is(64));
        assertThat(decision.getReason(),
                is("the lower-worker probe created off-CPU saturation"));
        assertThat(policy.isProbeActive(), is(false));
        }

    @Test
    public void shouldCompareAChangedWorkloadWithItsOwnThroughput()
        {
        DaemonPoolGrowthPolicy policy = policy();
        establishReference(policy, sample(1000L, 64, 1, 512, 16, 63, 20,
                0.10d, 1000.0d, DaemonPoolSizing.Role.BLOCKING_IO));

        policy.evaluateProbe(sample(4000L, 64, 1, 512, 16, 63, 0, 0,
                0.10d, 600.0d, DaemonPoolSizing.Role.BLOCKING_IO));
        policy.evaluateProbe(sample(5000L, 64, 1, 512, 16, 63, 0, 0,
                0.10d, 600.0d, DaemonPoolSizing.Role.BLOCKING_IO));
        policy.evaluateProbe(sample(6000L, 64, 1, 512, 16, 63, 0, 0,
                0.10d, 600.0d, DaemonPoolSizing.Role.BLOCKING_IO));
        policy.evaluateProbe(sample(7000L, 32, 1, 512, 16, 32, 0, 0,
                0.10d, 594.0d, DaemonPoolSizing.Role.BLOCKING_IO));

        DaemonPoolGrowthPolicy.Decision decision = policy.evaluateProbe(
                sample(8000L, 32, 1, 512, 16, 32, 0, 0,
                        0.10d, 594.0d, DaemonPoolSizing.Role.BLOCKING_IO));
        assertThat(decision.getAction(), is(DaemonPoolGrowthPolicy.Decision.Action.NONE));
        }

    private static DaemonPoolGrowthPolicy policy()
        {
        return new DaemonPoolGrowthPolicy(0.90d, 2, 0.02d, 30000L);
        }

    private static void prime(DaemonPoolGrowthPolicy policy,
            DaemonPoolGrowthPolicy.Sample sample)
        {
        policy.evaluateProbe(sample);
        policy.evaluateProbe(sample);
        policy.evaluateProbe(sample);
        }

    private static void establishReference(DaemonPoolGrowthPolicy policy,
            DaemonPoolGrowthPolicy.Sample sample)
        {
        policy.evaluateProbe(sample);
        policy.evaluateProbe(sample);
        policy.evaluateProbe(sample);
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

    private static DaemonPoolGrowthPolicy.Sample sample(long ldtTimestamp, int cThreads,
            int cThreadsMin, int cThreadsMax, int cProcessors, double dflActiveCount,
            int cBacklog, int cAssociations, double dflCpuRatio, double dflThroughput,
            DaemonPoolSizing.Role role)
        {
        return new DaemonPoolGrowthPolicy.Sample(ldtTimestamp, cThreads, cThreadsMin,
                cThreadsMax, cProcessors, dflActiveCount, cBacklog, cAssociations,
                dflCpuRatio, dflThroughput, role);
        }
    }
