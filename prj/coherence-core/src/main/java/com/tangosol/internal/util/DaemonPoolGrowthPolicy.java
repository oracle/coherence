/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util;

import com.tangosol.coherence.config.Config;

import java.util.Locale;

/**
 * A stateful policy that guards daemon-pool growth above the effective CPU
 * allocation.
 *
 * <p>The policy deliberately does not decide when a pool is busy enough to
 * grow. That remains the responsibility of the existing resize algorithm.
 * Instead, it limits a requested grow to the CPU knee and requires evidence
 * of blocking plus measurable marginal benefit before retaining workers above
 * that knee.</p>
 *
 * @author Aleks Seovic  2026.08.14
 * @since 26.07
 */
public class DaemonPoolGrowthPolicy
    {
    // ----- constructors -------------------------------------------------

    /**
     * Construct a policy using system-property configuration.
     */
    public DaemonPoolGrowthPolicy()
        {
        this(Config.getDouble("coherence.daemonpool.workload.cpu.threshold", 0.90d),
             Config.getInteger("coherence.daemonpool.workload.probe.samples", 2),
             Config.getDouble("coherence.daemonpool.workload.probe.improvement", 0.02d),
             Config.getLong("coherence.daemonpool.workload.probe.cooldown", 30000L));
        }

    /**
     * Construct a policy with explicit settings.
     *
     * @param dflCpuThreshold       the active-wall CPU ratio above which work
     *                              is considered CPU-bound
     * @param cProbeSamples         the number of post-grow samples used to
     *                              evaluate marginal benefit
     * @param dflProbeImprovement   the minimum relative throughput gain needed
     *                              to retain a probe worker
     * @param cProbeCooldownMillis  the delay before retrying a rejected probe
     */
    DaemonPoolGrowthPolicy(double dflCpuThreshold, int cProbeSamples,
            double dflProbeImprovement, long cProbeCooldownMillis)
        {
        f_dflCpuThreshold      = Math.max(0.0d, Math.min(1.0d, dflCpuThreshold));
        f_cProbeSamples        = Math.max(1, cProbeSamples);
        f_dflProbeImprovement  = Math.max(0.0d, dflProbeImprovement);
        f_cProbeCooldownMillis = Math.max(0L, cProbeCooldownMillis);
        }

    // ----- policy -------------------------------------------------------

    /**
     * Evaluate an active worker probe using the latest sample.
     *
     * @param sample  the latest interval sample
     *
     * @return the probe decision
     */
    public Decision evaluateProbe(Sample sample)
        {
        if (m_cProbeThreads == 0)
            {
            return Decision.none();
            }

        if (sample.getThreadCount() != m_cProbeThreads)
            {
            clearProbe();
            return Decision.none();
            }

        m_dflProbeThroughputTotal += sample.getThroughput();
        if (++m_cProbeSampleCount < f_cProbeSamples)
            {
            return Decision.hold("an above-CPU worker probe still being evaluated");
            }

        double dflAverage     = m_dflProbeThroughputTotal / m_cProbeSampleCount;
        double dflImprovement = m_dflProbeBaseline <= 0.0d
                ? 0.0d
                : (dflAverage - m_dflProbeBaseline) / m_dflProbeBaseline;

        if (dflImprovement >= f_dflProbeImprovement)
            {
            clearProbe();
            return Decision.none("the above-CPU worker probe improved throughput by "
                    + formatPercent(dflImprovement));
            }

        clearProbe();
        m_ldtProbeRejected = sample.getTimestamp();
        return Decision.revert("the above-CPU worker probe improved throughput by only "
                + formatPercent(dflImprovement));
        }

    /**
     * Guard a growth requested by the existing resize algorithm.
     *
     * @param sample           the latest interval sample
     * @param cRequestedDelta  the requested number of workers to add
     *
     * @return the growth decision
     */
    public Decision requestGrowth(Sample sample, int cRequestedDelta)
        {
        int cThreads    = sample.getThreadCount();
        int cProcessors = Math.max(1, sample.getProcessorCount());
        int cKnee       = Math.max(sample.getMinimumThreadCount(),
                                  Math.min(cProcessors, sample.getMaximumThreadCount()));

        if (cThreads < cKnee)
            {
            return Decision.grow(Math.min(Math.max(1, cRequestedDelta), cKnee - cThreads), false,
                    "executable pressure below the CPU allocation");
            }

        if (m_cProbeThreads != 0)
            {
            return Decision.hold("an above-CPU worker probe still being evaluated");
            }

        if (cThreads >= sample.getMaximumThreadCount())
            {
            return Decision.hold("the configured maximum worker count");
            }

        if (sample.getBacklog() <= 0)
            {
            return Decision.hold("no queued work at the CPU allocation");
            }

        int cAssociations = sample.getActiveAssociationCount();
        if (cAssociations > 0 && cAssociations <= cThreads)
            {
            return Decision.hold("association-limited work at the CPU allocation");
            }

        double dflCpuRatio = sample.getWorkerCpuRatio();
        if (dflCpuRatio >= f_dflCpuThreshold)
            {
            return Decision.hold("CPU-bound work at the CPU allocation");
            }

        if (dflCpuRatio < 0.0d && sample.getRole() != DaemonPoolSizing.Role.BLOCKING_IO)
            {
            return Decision.hold("no blocking evidence above the CPU allocation");
            }

        if (sample.getTimestamp() < m_ldtProbeRejected + f_cProbeCooldownMillis)
            {
            return Decision.hold("the above-CPU worker probe cooldown");
            }

        return Decision.grow(1, true, "measured blocking above the CPU allocation");
        }

    /**
     * Record that a probe growth was applied.
     *
     * @param sample       the sample that caused the probe
     * @param cThreadCount the resulting worker count
     */
    public void onProbeApplied(Sample sample, int cThreadCount)
        {
        m_cProbeThreads           = cThreadCount;
        m_cProbeSampleCount       = 0;
        m_dflProbeBaseline        = sample.getThroughput();
        m_dflProbeThroughputTotal = 0.0d;
        }

    /**
     * Return whether an above-CPU worker probe is active.
     *
     * @return {@code true} if a probe is active
     */
    public boolean isProbeActive()
        {
        return m_cProbeThreads != 0;
        }

    /**
     * Clear active probe state.
     */
    protected void clearProbe()
        {
        m_cProbeThreads           = 0;
        m_cProbeSampleCount       = 0;
        m_dflProbeBaseline        = 0.0d;
        m_dflProbeThroughputTotal = 0.0d;
        }

    private static String formatPercent(double dfl)
        {
        return String.format(Locale.ROOT, "%.1f%%", dfl * 100.0d);
        }

    // ----- inner class: Sample -----------------------------------------

    /**
     * An interval sample used to classify a requested growth.
     */
    public static class Sample
        {
        public Sample(long ldtTimestamp, int cThreads, int cThreadsMin, int cThreadsMax,
                int cProcessors, int cBacklog, int cAssociations, double dflCpuRatio,
                double dflThroughput, DaemonPoolSizing.Role role)
            {
            f_ldtTimestamp    = ldtTimestamp;
            f_cThreads        = cThreads;
            f_cThreadsMin     = cThreadsMin;
            f_cThreadsMax     = cThreadsMax;
            f_cProcessors     = cProcessors;
            f_cBacklog        = cBacklog;
            f_cAssociations   = cAssociations;
            f_dflCpuRatio     = dflCpuRatio;
            f_dflThroughput   = dflThroughput;
            f_role            = role == null ? DaemonPoolSizing.Role.SERVICE : role;
            }

        public long getTimestamp()                 { return f_ldtTimestamp; }
        public int getThreadCount()                { return f_cThreads; }
        public int getMinimumThreadCount()         { return f_cThreadsMin; }
        public int getMaximumThreadCount()         { return f_cThreadsMax; }
        public int getProcessorCount()             { return f_cProcessors; }
        public int getBacklog()                    { return f_cBacklog; }
        public int getActiveAssociationCount()     { return f_cAssociations; }
        public double getWorkerCpuRatio()          { return f_dflCpuRatio; }
        public double getThroughput()              { return f_dflThroughput; }
        public DaemonPoolSizing.Role getRole()     { return f_role; }

        private final long                  f_ldtTimestamp;
        private final int                   f_cThreads;
        private final int                   f_cThreadsMin;
        private final int                   f_cThreadsMax;
        private final int                   f_cProcessors;
        private final int                   f_cBacklog;
        private final int                   f_cAssociations;
        private final double                f_dflCpuRatio;
        private final double                f_dflThroughput;
        private final DaemonPoolSizing.Role f_role;
        }

    // ----- inner class: Decision ---------------------------------------

    /**
     * A policy decision.
     */
    public static class Decision
        {
        public enum Action
            {
            NONE,
            HOLD,
            GROW,
            REVERT
            }

        private Decision(Action action, int cDelta, boolean fProbe, String sReason)
            {
            f_action = action;
            f_cDelta = cDelta;
            f_fProbe = fProbe;
            f_sReason = sReason;
            }

        public static Decision none()                 { return new Decision(Action.NONE, 0, false, null); }
        public static Decision none(String sReason)   { return new Decision(Action.NONE, 0, false, sReason); }
        public static Decision hold(String sReason)   { return new Decision(Action.HOLD, 0, false, sReason); }
        public static Decision revert(String sReason) { return new Decision(Action.REVERT, -1, false, sReason); }
        public static Decision grow(int cDelta, boolean fProbe, String sReason)
            {
            return new Decision(Action.GROW, cDelta, fProbe, sReason);
            }

        public Action getAction()     { return f_action; }
        public int getDelta()         { return f_cDelta; }
        public boolean isProbe()      { return f_fProbe; }
        public String getReason()     { return f_sReason; }

        private final Action  f_action;
        private final int     f_cDelta;
        private final boolean f_fProbe;
        private final String  f_sReason;
        }

    // ----- data members -------------------------------------------------

    private final double f_dflCpuThreshold;
    private final int    f_cProbeSamples;
    private final double f_dflProbeImprovement;
    private final long   f_cProbeCooldownMillis;

    private int    m_cProbeThreads;
    private int    m_cProbeSampleCount;
    private double m_dflProbeBaseline;
    private double m_dflProbeThroughputTotal;
    private long   m_ldtProbeRejected = Long.MIN_VALUE;
    }
