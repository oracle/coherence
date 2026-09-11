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
 * Unit tests for {@link AdaptiveConcurrencyPolicy}.
 *
 * @author Aleks Seovic  2026.08.30
 * @since 26.10
 */
public class AdaptiveConcurrencyPolicyTest
    {
    @Test
    public void shouldDoubleToFirstIoWindowWhenSaturated()
        {
        AdaptiveConcurrencyPolicy policy = policy();
        prime(policy, 32, 32, 512, 1000.0d);
        AdaptiveConcurrencyPolicy.Decision decision = policy.evaluate(
                sample(3000L, 32, 32, 512, 32, 20, 1000.0d));

        assertThat(decision.getAction(), is(AdaptiveConcurrencyPolicy.Decision.Action.RESIZE));
        assertThat(decision.getTarget(), is(64));
        assertThat(decision.isProbe(), is(true));
        }

    @Test
    public void shouldRespectConcurrencyCeiling()
        {
        AdaptiveConcurrencyPolicy policy = policy();
        prime(policy, 120, 32, 128, 1000.0d);
        AdaptiveConcurrencyPolicy.Decision decision = policy.evaluate(
                sample(3000L, 120, 32, 128, 120, 20, 1000.0d));

        assertThat(decision.getAction(), is(AdaptiveConcurrencyPolicy.Decision.Action.RESIZE));
        assertThat(decision.getTarget(), is(128));
        }

    @Test
    public void shouldRetainBeneficialProbe()
        {
        AdaptiveConcurrencyPolicy policy = policy();
        prime(policy, 32, 32, 512, 1000.0d);
        policy.evaluate(sample(3000L, 32, 32, 512, 32, 20, 1000.0d));

        assertThat(policy.evaluate(sample(4000L, 64, 32, 512, 64, 20, 1150.0d)).getAction(),
                is(AdaptiveConcurrencyPolicy.Decision.Action.HOLD));
        assertThat(policy.evaluate(sample(5000L, 64, 32, 512, 64, 20, 1140.0d)).getAction(),
                is(AdaptiveConcurrencyPolicy.Decision.Action.NONE));
        }

    @Test
    public void shouldUseLowerEfficiencyThresholdForFirstIoWindow()
        {
        AdaptiveConcurrencyPolicy policy = efficientPolicy();
        prime(policy, 64, 64, 1024, 1000.0d);
        AdaptiveConcurrencyPolicy.Decision decision = policy.evaluate(
                sample(3000L, 64, 64, 1024, 64, 20, 1000.0d));

        assertThat(decision.getTarget(), is(128));
        assertThat(policy.evaluate(sample(4000L, 128, 64, 1024, 128, 20, 1150.0d)).getAction(),
                is(AdaptiveConcurrencyPolicy.Decision.Action.HOLD));
        assertThat(policy.evaluate(sample(5000L, 128, 64, 1024, 128, 20, 1150.0d)).getAction(),
                is(AdaptiveConcurrencyPolicy.Decision.Action.NONE));
        }

    @Test
    public void shouldRequireProportionalGainBeyondFirstIoWindow()
        {
        AdaptiveConcurrencyPolicy policy = efficientPolicy();
        prime(policy, 128, 64, 1024, 1000.0d);
        AdaptiveConcurrencyPolicy.Decision decision = policy.evaluate(
                sample(3000L, 128, 64, 1024, 128, 20, 1000.0d));

        assertThat(decision.getTarget(), is(160));
        assertThat(policy.evaluate(sample(4000L, 160, 64, 1024, 160, 20, 1100.0d)).getAction(),
                is(AdaptiveConcurrencyPolicy.Decision.Action.HOLD));
        decision = policy.evaluate(sample(5000L, 160, 64, 1024, 160, 20, 1100.0d));
        assertThat(decision.getAction(), is(AdaptiveConcurrencyPolicy.Decision.Action.RESIZE));
        assertThat(decision.getTarget(), is(128));
        assertThat(decision.isProbe(), is(false));
        }

    @Test
    public void shouldRetainEfficientProbeBeyondFirstIoWindow()
        {
        AdaptiveConcurrencyPolicy policy = efficientPolicy();
        prime(policy, 128, 64, 1024, 1000.0d);
        AdaptiveConcurrencyPolicy.Decision decision = policy.evaluate(
                sample(3000L, 128, 64, 1024, 128, 20, 1000.0d));

        assertThat(decision.getTarget(), is(160));
        assertThat(policy.evaluate(sample(4000L, 160, 64, 1024, 160, 20, 1190.0d)).getAction(),
                is(AdaptiveConcurrencyPolicy.Decision.Action.HOLD));
        decision = policy.evaluate(sample(5000L, 160, 64, 1024, 160, 20, 1190.0d));
        assertThat(decision.getAction(), is(AdaptiveConcurrencyPolicy.Decision.Action.NONE));
        }

    @Test
    public void shouldRequireFreshBaselineAfterRetainingProbe()
        {
        AdaptiveConcurrencyPolicy policy = policy();
        prime(policy, 32, 32, 512, 1000.0d);
        policy.evaluate(sample(3000L, 32, 32, 512, 32, 20, 1000.0d));
        policy.evaluate(sample(4000L, 64, 32, 512, 64, 20, 1150.0d));
        policy.evaluate(sample(5000L, 64, 32, 512, 64, 20, 1140.0d));

        AdaptiveConcurrencyPolicy.Decision decision = policy.evaluate(
                sample(6000L, 64, 32, 512, 64, 20, 1145.0d));
        assertThat(decision.getAction(), is(AdaptiveConcurrencyPolicy.Decision.Action.HOLD));

        decision = policy.evaluate(
                sample(7000L, 64, 32, 512, 64, 20, 1145.0d));
        assertThat(decision.getAction(), is(AdaptiveConcurrencyPolicy.Decision.Action.RESIZE));
        assertThat(decision.getTarget(), is(80));
        assertThat(decision.isProbe(), is(true));
        }

    @Test
    public void shouldRevertUnproductiveProbeAndCooldown()
        {
        AdaptiveConcurrencyPolicy policy = policy();
        prime(policy, 32, 32, 512, 1000.0d);
        policy.evaluate(sample(3000L, 32, 32, 512, 32, 20, 1000.0d));
        policy.evaluate(sample(4000L, 64, 32, 512, 64, 20, 1005.0d));

        AdaptiveConcurrencyPolicy.Decision decision = policy.evaluate(
                sample(5000L, 64, 32, 512, 64, 20, 1000.0d));
        assertThat(decision.getAction(), is(AdaptiveConcurrencyPolicy.Decision.Action.RESIZE));
        assertThat(decision.getTarget(), is(32));
        assertThat(decision.isProbe(), is(false));

        decision = policy.evaluate(sample(6000L, 32, 32, 512, 32, 20, 1000.0d));
        assertThat(decision.getAction(), is(AdaptiveConcurrencyPolicy.Decision.Action.HOLD));
        }

    @Test
    public void shouldRememberRejectedUpperProbeUntilWorkloadChanges()
        {
        AdaptiveConcurrencyPolicy policy = policy();
        prime(policy, 32, 32, 512, 1000.0d);
        policy.evaluate(sample(3000L, 32, 32, 512, 32, 20, 1000.0d));
        policy.evaluate(sample(4000L, 64, 32, 512, 64, 20, 1000.0d));
        policy.evaluate(sample(5000L, 64, 32, 512, 64, 20, 1000.0d));

        AdaptiveConcurrencyPolicy.Decision decision = policy.evaluate(
                sample(40000L, 32, 32, 512, 32, 20, 1000.0d));
        assertThat(decision.getAction(), is(AdaptiveConcurrencyPolicy.Decision.Action.HOLD));
        assertThat(decision.getReason(),
                is("the learned concurrency-probe bound for this workload"));

        policy.evaluate(sample(41000L, 32, 32, 512, 32, 20, 1300.0d));
        policy.evaluate(sample(42000L, 32, 32, 512, 32, 20, 1300.0d));
        decision = policy.evaluate(sample(43000L, 32, 32, 512, 32, 20, 1300.0d));
        assertThat(decision.getAction(), is(AdaptiveConcurrencyPolicy.Decision.Action.RESIZE));
        assertThat(decision.getTarget(), is(64));
        }

    @Test
    public void shouldRetainRejectedUpperProbeAcrossIdleTraffic()
        {
        AdaptiveConcurrencyPolicy policy = policy();
        prime(policy, 32, 16, 512, 1000.0d);
        assertThat(policy.evaluate(sample(3000L, 32, 16, 512, 32, 20, 1000.0d))
                .getTarget(), is(40));
        policy.evaluate(sample(4000L, 40, 16, 512, 40, 20, 1000.0d));
        assertThat(policy.evaluate(sample(5000L, 40, 16, 512, 40, 20, 1000.0d))
                .getTarget(), is(32));

        // An admission window consumes no workers while idle, so retain the
        // proven window rather than making the next burst rediscover it.
        for (int i = 6; i <= 9; i++)
            {
            assertThat(policy.evaluate(sample(i * 1000L, 32, 16, 512, 0, 0, 0.0d))
                    .getAction(), is(AdaptiveConcurrencyPolicy.Decision.Action.NONE));
            }

        // Once a fresh baseline forms, the controller must still remember
        // that 32 -> 40 already failed for this same traffic shape.
        assertThat(policy.evaluate(sample(40000L, 32, 16, 512, 32, 20, 1000.0d))
                .getAction(), is(AdaptiveConcurrencyPolicy.Decision.Action.HOLD));
        assertThat(policy.evaluate(sample(41000L, 32, 16, 512, 32, 20, 1000.0d))
                .getAction(), is(AdaptiveConcurrencyPolicy.Decision.Action.HOLD));
        AdaptiveConcurrencyPolicy.Decision decision = policy.evaluate(
                sample(42000L, 32, 16, 512, 32, 20, 1000.0d));
        assertThat(decision.getAction(), is(AdaptiveConcurrencyPolicy.Decision.Action.HOLD));
        assertThat(decision.getReason(),
                is("the learned concurrency-probe bound for this workload"));
        }

    @Test
    public void shouldNotProbeDuringAnUnstableRamp()
        {
        AdaptiveConcurrencyPolicy policy = policy();

        assertThat(policy.evaluate(sample(1000L, 64, 32, 512, 64, 20, 1000.0d)).getAction(),
                is(AdaptiveConcurrencyPolicy.Decision.Action.HOLD));
        assertThat(policy.evaluate(sample(2000L, 64, 32, 512, 64, 20, 1400.0d)).getAction(),
                is(AdaptiveConcurrencyPolicy.Decision.Action.HOLD));
        assertThat(policy.evaluate(sample(3000L, 64, 32, 512, 64, 20, 1900.0d)).getAction(),
                is(AdaptiveConcurrencyPolicy.Decision.Action.HOLD));
        }

    @Test
    public void shouldBootstrapFromSustainedSaturationWithoutStableThroughput()
        {
        AdaptiveConcurrencyPolicy policy = policy();

        assertThat(policy.evaluate(sample(1000L, 32, 32, 512, 32, 20, 1000.0d)).getAction(),
                is(AdaptiveConcurrencyPolicy.Decision.Action.HOLD));
        assertThat(policy.evaluate(sample(2000L, 32, 32, 512, 32, 20, 1400.0d)).getAction(),
                is(AdaptiveConcurrencyPolicy.Decision.Action.HOLD));
        AdaptiveConcurrencyPolicy.Decision decision = policy.evaluate(
                sample(3000L, 32, 32, 512, 32, 20, 900.0d));

        assertThat(decision.getAction(), is(AdaptiveConcurrencyPolicy.Decision.Action.RESIZE));
        assertThat(decision.getTarget(), is(64));
        assertThat(decision.isProbe(), is(true));
        }

    @Test
    public void shouldBootstrapFromNonConsecutiveSaturationObservations()
        {
        AdaptiveConcurrencyPolicy policy = policy();

        assertThat(policy.evaluate(sample(1000L, 64, 64, 1024, 64, 20, 1000.0d)).getAction(),
                is(AdaptiveConcurrencyPolicy.Decision.Action.HOLD));
        assertThat(policy.evaluate(sample(2000L, 64, 64, 1024, 40, 0, 1200.0d)).getAction(),
                is(AdaptiveConcurrencyPolicy.Decision.Action.NONE));
        assertThat(policy.evaluate(sample(3000L, 64, 64, 1024, 64, 20, 900.0d)).getAction(),
                is(AdaptiveConcurrencyPolicy.Decision.Action.HOLD));
        assertThat(policy.evaluate(sample(4000L, 64, 64, 1024, 30, 0, 1100.0d)).getAction(),
                is(AdaptiveConcurrencyPolicy.Decision.Action.NONE));
        AdaptiveConcurrencyPolicy.Decision decision = policy.evaluate(
                sample(5000L, 64, 64, 1024, 64, 20, 1000.0d));

        assertThat(decision.getAction(), is(AdaptiveConcurrencyPolicy.Decision.Action.RESIZE));
        assertThat(decision.getTarget(), is(128));
        }

    @Test
    public void shouldRetainFirstWindowForBlockingIoDespiteLocalThroughputNoise()
        {
        AdaptiveConcurrencyPolicy policy = policy();

        policy.evaluate(sample(1000L, 64, 64, 1024, 64, 20, 1000.0d, true));
        policy.evaluate(sample(2000L, 64, 64, 1024, 64, 20, 1400.0d, true));
        AdaptiveConcurrencyPolicy.Decision decision = policy.evaluate(
                sample(3000L, 64, 64, 1024, 64, 20, 900.0d, true));
        assertThat(decision.getTarget(), is(128));

        assertThat(policy.evaluate(sample(4000L, 128, 64, 1024,
                80, 0, 700.0d, true)).getAction(),
                is(AdaptiveConcurrencyPolicy.Decision.Action.HOLD));
        decision = policy.evaluate(sample(5000L, 128, 64, 1024,
                80, 0, 700.0d, true));
        assertThat(decision.getAction(), is(AdaptiveConcurrencyPolicy.Decision.Action.NONE));
        assertThat(decision.getReason(),
                is("the saturated blocking-I/O domain established its first useful window"));
        }

    @Test
    public void shouldNotContractAnUnderOccupiedAdmissionWindow()
        {
        AdaptiveConcurrencyPolicy policy = policy();

        assertThat(policy.evaluate(sample(1000L, 128, 32, 512, 20, 0, 100.0d)).getAction(),
                is(AdaptiveConcurrencyPolicy.Decision.Action.NONE));
        assertThat(policy.evaluate(sample(2000L, 128, 32, 512, 20, 0, 100.0d)).getAction(),
                is(AdaptiveConcurrencyPolicy.Decision.Action.NONE));

        AdaptiveConcurrencyPolicy.Decision decision = policy.evaluate(
                sample(3000L, 128, 32, 512, 20, 0, 100.0d));
        assertThat(decision.getAction(), is(AdaptiveConcurrencyPolicy.Decision.Action.NONE));
        }

    @Test
    public void shouldProbeDownAfterAStableWorkloadShapeChange()
        {
        AdaptiveConcurrencyPolicy policy = policy();
        establishReference(policy, 128, 32, 512, 1000.0d);

        policy.evaluate(sample(4000L, 128, 32, 512, 128, 20, 600.0d));
        policy.evaluate(sample(5000L, 128, 32, 512, 128, 20, 600.0d));
        AdaptiveConcurrencyPolicy.Decision decision = policy.evaluate(
                sample(6000L, 128, 32, 512, 128, 20, 600.0d));

        assertThat(decision.getAction(), is(AdaptiveConcurrencyPolicy.Decision.Action.RESIZE));
        assertThat(decision.getTarget(), is(64));
        assertThat(decision.isProbe(), is(true));
        }

    @Test
    public void shouldRestoreConcurrencyWhenDownProbeDoesNotHelp()
        {
        AdaptiveConcurrencyPolicy policy = policy();
        establishReference(policy, 128, 32, 512, 1000.0d);
        policy.evaluate(sample(4000L, 128, 32, 512, 128, 20, 600.0d));
        policy.evaluate(sample(5000L, 128, 32, 512, 128, 20, 600.0d));
        policy.evaluate(sample(6000L, 128, 32, 512, 128, 20, 600.0d));
        policy.evaluate(sample(7000L, 64, 32, 512, 64, 20, 500.0d));

        AdaptiveConcurrencyPolicy.Decision decision = policy.evaluate(
                sample(8000L, 64, 32, 512, 64, 20, 500.0d));
        assertThat(decision.getAction(), is(AdaptiveConcurrencyPolicy.Decision.Action.RESIZE));
        assertThat(decision.getTarget(), is(128));
        assertThat(decision.isProbe(), is(false));
        }

    @Test
    public void shouldCompareAContinuingLowerProbeWithTheOriginalReference()
        {
        AdaptiveConcurrencyPolicy policy = policy();
        establishReference(policy, 256, 32, 512, 1000.0d);

        policy.evaluate(sample(4000L, 256, 32, 512, 256, 20, 600.0d));
        policy.evaluate(sample(5000L, 256, 32, 512, 256, 20, 600.0d));
        AdaptiveConcurrencyPolicy.Decision decision = policy.evaluate(
                sample(6000L, 256, 32, 512, 256, 20, 600.0d));
        assertThat(decision.getTarget(), is(128));

        policy.evaluate(sample(7000L, 128, 32, 512, 128, 20, 620.0d));
        assertThat(policy.evaluate(sample(8000L, 128, 32, 512, 128, 20, 620.0d)).getAction(),
                is(AdaptiveConcurrencyPolicy.Decision.Action.NONE));

        decision = policy.evaluate(sample(9000L, 128, 32, 512, 128, 20, 620.0d));
        assertThat(decision.getTarget(), is(64));

        policy.evaluate(sample(10000L, 64, 32, 512, 64, 20, 618.0d));
        decision = policy.evaluate(sample(11000L, 64, 32, 512, 64, 20, 618.0d));
        assertThat(decision.getAction(), is(AdaptiveConcurrencyPolicy.Decision.Action.NONE));
        }

    @Test
    public void shouldNotDescendBelowTheFirstIoWindow()
        {
        AdaptiveConcurrencyPolicy policy = policy();
        establishReference(policy, 64, 32, 512, 1000.0d);

        policy.evaluate(sample(4000L, 64, 32, 512, 64, 20, 600.0d));
        policy.evaluate(sample(5000L, 64, 32, 512, 64, 20, 600.0d));
        AdaptiveConcurrencyPolicy.Decision decision = policy.evaluate(
                sample(6000L, 64, 32, 512, 64, 20, 600.0d));

        assertThat(decision.getAction(), is(AdaptiveConcurrencyPolicy.Decision.Action.RESIZE));
        assertThat(decision.getTarget(), is(80));
        assertThat(decision.isProbe(), is(true));
        }

    @Test
    public void shouldNotProbeAStableOccupiedZeroBacklogWindow()
        {
        AdaptiveConcurrencyPolicy policy = policy();

        policy.evaluate(sample(1000L, 64, 32, 512, 63, 0, 1000.0d));
        policy.evaluate(sample(2000L, 64, 32, 512, 63, 0, 1000.0d));
        AdaptiveConcurrencyPolicy.Decision decision = policy.evaluate(
                sample(3000L, 64, 32, 512, 63, 0, 1000.0d));

        assertThat(decision.getAction(), is(AdaptiveConcurrencyPolicy.Decision.Action.NONE));
        }

    @Test
    public void shouldNotBypassARejectedLowerProbeWhenUnderOccupied()
        {
        AdaptiveConcurrencyPolicy policy = policy();
        establishReference(policy, 128, 32, 512, 1000.0d);
        policy.evaluate(sample(4000L, 128, 32, 512, 128, 20, 600.0d));
        policy.evaluate(sample(5000L, 128, 32, 512, 128, 20, 600.0d));
        policy.evaluate(sample(6000L, 128, 32, 512, 128, 20, 600.0d));
        policy.evaluate(sample(7000L, 64, 32, 512, 64, 20, 500.0d));
        AdaptiveConcurrencyPolicy.Decision decision = policy.evaluate(
                sample(8000L, 64, 32, 512, 64, 20, 500.0d));
        assertThat(decision.getTarget(), is(128));

        for (int i = 9; i <= 12; i++)
            {
            decision = policy.evaluate(sample(i * 1000L, 128, 32, 512,
                    20, 0, 1000.0d));
            assertThat(decision.getAction(), is(AdaptiveConcurrencyPolicy.Decision.Action.NONE));
            }
        }

    @Test
    public void shouldCompareAChangedWorkloadWithItsOwnThroughput()
        {
        AdaptiveConcurrencyPolicy policy = policy();
        establishReference(policy, 128, 32, 512, 1000.0d);

        policy.evaluate(sample(4000L, 128, 32, 512, 128, 20, 600.0d));
        policy.evaluate(sample(5000L, 128, 32, 512, 128, 20, 600.0d));
        policy.evaluate(sample(6000L, 128, 32, 512, 128, 20, 600.0d));
        policy.evaluate(sample(7000L, 64, 32, 512, 64, 20, 620.0d));

        AdaptiveConcurrencyPolicy.Decision decision = policy.evaluate(
                sample(8000L, 64, 32, 512, 64, 20, 620.0d));
        assertThat(decision.getAction(), is(AdaptiveConcurrencyPolicy.Decision.Action.NONE));
        }

    @Test
    public void shouldRejectBackloggedDownProbeWithoutThroughputGain()
        {
        AdaptiveConcurrencyPolicy policy = policy();
        establishReference(policy, 128, 32, 512, 1000.0d);

        policy.evaluate(sample(4000L, 128, 32, 512, 128, 20, 600.0d));
        policy.evaluate(sample(5000L, 128, 32, 512, 128, 20, 600.0d));
        policy.evaluate(sample(6000L, 128, 32, 512, 128, 20, 600.0d));
        policy.evaluate(sample(7000L, 64, 32, 512, 64, 20, 594.0d));

        AdaptiveConcurrencyPolicy.Decision decision = policy.evaluate(
                sample(8000L, 64, 32, 512, 64, 20, 594.0d));
        assertThat(decision.getAction(), is(AdaptiveConcurrencyPolicy.Decision.Action.RESIZE));
        assertThat(decision.getTarget(), is(128));
        assertThat(decision.isProbe(), is(false));
        }

    @Test
    public void shouldRetainBackloggedDownProbeWithThroughputGain()
        {
        AdaptiveConcurrencyPolicy policy = policy();
        establishReference(policy, 128, 32, 512, 1000.0d);

        policy.evaluate(sample(4000L, 128, 32, 512, 128, 20, 600.0d));
        policy.evaluate(sample(5000L, 128, 32, 512, 128, 20, 600.0d));
        policy.evaluate(sample(6000L, 128, 32, 512, 128, 20, 600.0d));
        policy.evaluate(sample(7000L, 64, 32, 512, 64, 20, 620.0d));

        AdaptiveConcurrencyPolicy.Decision decision = policy.evaluate(
                sample(8000L, 64, 32, 512, 64, 20, 620.0d));
        assertThat(decision.getAction(), is(AdaptiveConcurrencyPolicy.Decision.Action.NONE));
        }

    private static AdaptiveConcurrencyPolicy policy()
        {
        return new AdaptiveConcurrencyPolicy(2, 0.02d, 30000L);
        }

    private static AdaptiveConcurrencyPolicy efficientPolicy()
        {
        return new AdaptiveConcurrencyPolicy(2, 0.02d, 30000L,
                3, 0.10d, 0.20d, 0.75d);
        }

    private static void prime(AdaptiveConcurrencyPolicy policy, int cConcurrency,
            int cMinimum, int cMaximum, double dflThroughput)
        {
        policy.evaluate(sample(1000L, cConcurrency, cMinimum, cMaximum,
                cConcurrency, 20, dflThroughput));
        policy.evaluate(sample(2000L, cConcurrency, cMinimum, cMaximum,
                cConcurrency, 20, dflThroughput));
        }

    private static void establishReference(AdaptiveConcurrencyPolicy policy,
            int cConcurrency, int cMinimum, int cMaximum, double dflThroughput)
        {
        for (int i = 1; i <= 3; i++)
            {
            policy.evaluate(sample(i * 1000L, cConcurrency, cMinimum, cMaximum,
                    cConcurrency - 1, 20, dflThroughput));
            }
        }

    private static AdaptiveConcurrencyPolicy.Sample sample(long ldtTimestamp,
            int cConcurrency, int cMinimum, int cMaximum, int cActive,
            int cBacklog, double dflThroughput)
        {
        return new AdaptiveConcurrencyPolicy.Sample(ldtTimestamp, cConcurrency,
                cMinimum, cMaximum, cActive, cBacklog, dflThroughput);
        }

    private static AdaptiveConcurrencyPolicy.Sample sample(long ldtTimestamp,
            int cConcurrency, int cMinimum, int cMaximum, int cActive,
            int cBacklog, double dflThroughput, boolean fFirstWindowFloor)
        {
        return new AdaptiveConcurrencyPolicy.Sample(ldtTimestamp, cConcurrency,
                cMinimum, cMaximum, cActive, cBacklog, dflThroughput,
                fFirstWindowFloor);
        }
    }
