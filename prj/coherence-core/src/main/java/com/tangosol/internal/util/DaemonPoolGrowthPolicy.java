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
             Config.getLong("coherence.daemonpool.workload.probe.cooldown", 30000L),
             Config.getInteger("coherence.daemonpool.workload.baseline.samples", 5),
             Config.getDouble("coherence.daemonpool.workload.baseline.stability", 0.10d),
             Config.getDouble("coherence.daemonpool.workload.baseline.change", 0.20d),
             Config.getDouble("coherence.daemonpool.workload.probe.efficiency", 0.10d));
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
        this(dflCpuThreshold, cProbeSamples, dflProbeImprovement,
                cProbeCooldownMillis, 3, 0.10d, 0.20d, 0.10d);
        }

    /**
     * Construct a policy with explicit probe and baseline settings.
     */
    DaemonPoolGrowthPolicy(double dflCpuThreshold, int cProbeSamples,
            double dflProbeImprovement, long cProbeCooldownMillis,
            int cBaselineSamples, double dflBaselineStability,
            double dflWorkloadChange, double dflProbeEfficiency)
        {
        f_dflCpuThreshold      = Math.max(0.0d, Math.min(1.0d, dflCpuThreshold));
        f_cProbeSamples        = Math.max(1, cProbeSamples);
        f_dflProbeImprovement  = Math.max(0.0d, dflProbeImprovement);
        f_cProbeCooldownMillis = Math.max(0L, cProbeCooldownMillis);
        f_cBaselineSamples     = Math.max(1, cBaselineSamples);
        f_dflBaselineStability = Math.max(0.0d, dflBaselineStability);
        f_dflWorkloadChange    = Math.max(f_dflBaselineStability, dflWorkloadChange);
        f_dflProbeEfficiency   = Math.max(0.0d, dflProbeEfficiency);
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
            recordBaseline(sample);
            int cMinimum = Math.max(sample.getMinimumThreadCount(),
                    Math.min(sample.getProcessorCount(), sample.getMaximumThreadCount()));
            if (shouldProbeLower(sample, cMinimum))
                {
                boolean fContinuing    = m_fSeekLower;
                boolean fUnderOccupied = isUnderOccupied(sample);
                boolean fShapeChanged  = isWorkloadThroughputDrop(sample);
                int cCurrent = sample.getThreadCount();
                int cTarget = Math.max(cMinimum,
                        cCurrent - Math.max(1, cCurrent / 2));
                if (cTarget >= sample.getThreadCount())
                    {
                    cTarget = cCurrent - 1;
                    }
                if (fShapeChanged)
                    {
                    // Throughput is not comparable across workload shapes.
                    // The first lower probe establishes a new reference for
                    // the current shape; only the continuing descent must
                    // remain within tolerance of that reference.
                    setReference(cCurrent, m_dflBaselineAverage);
                    }
                double dflReference = m_dflReferenceThroughput > 0.0d
                        ? m_dflReferenceThroughput : m_dflBaselineAverage;
                startProbe(cCurrent, cTarget, dflReference,
                        m_dflBaselineAverage, true);
                m_fSeekLower = false;
                return Decision.resize(cTarget - cCurrent,
                        fContinuing
                                ? "a continuing successful lower-worker probe"
                                : fUnderOccupied
                                    ? "a stable under-occupied worker pool"
                                    : fShapeChanged
                                        ? "a stable workload-shape change at the current worker count"
                                        : "a stable zero-backlog worker-pool knee probe");
                }
            if (shouldProbeBlockedWork(sample, cMinimum))
                {
                int cCurrent = sample.getThreadCount();
                int cMaximum = sample.getMaximumThreadCount();
                int cTarget;
                if (cCurrent < cMinimum)
                    {
                    cTarget = cMinimum;
                    }
                else
                    {
                    double dflCpuRatio = sample.getWorkerCpuRatio();
                    int cMeasured = dflCpuRatio > 0.0d
                            ? (int) Math.ceil(sample.getProcessorCount() / dflCpuRatio)
                            : cMaximum;
                    int cFastCeiling = sample.getProcessorCount() > Integer.MAX_VALUE / 2
                            ? Integer.MAX_VALUE : sample.getProcessorCount() * 2;
                    int cStep = Math.max(1, cCurrent /
                            (cCurrent < cFastCeiling ? 2 : 4));
                    cTarget = Math.min(cMaximum,
                            Math.min(cMeasured, cCurrent + cStep));
                    }
                if (cTarget > cCurrent)
                    {
                    if (isUpperProbeRejected(cCurrent, cTarget))
                        {
                        return Decision.hold("the learned worker-probe bound for this workload");
                        }
                    if (m_cUpperRejectedTarget > 0 && cTarget >= m_cUpperRejectedTarget)
                        {
                        clearUpperRejection();
                        }
                    startProbe(cCurrent, cTarget, m_dflBaselineAverage,
                            m_dflBaselineAverage, false);
                    return Decision.resize(cTarget - cCurrent,
                            "stable off-CPU saturation without runnable backlog");
                    }
                }
            return Decision.none();
            }

        if (sample.getThreadCount() != m_cProbeThreads)
            {
            clearProbe();
            clearBaseline();
            clearReference();
            clearUpperRejection();
            recordBaseline(sample);
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

        int     cPrevious  = m_cProbePrevious;
        double  dflBaseline = m_dflProbeBaseline;
        double  dflPreviousThroughput = m_dflProbePreviousThroughput;
        boolean fDownward = m_fProbeDownward;
        int     cProbeDelta = m_cProbeThreads - cPrevious;

        double dflRequired = fDownward
                ? -f_dflProbeImprovement
                : Math.max(f_dflProbeImprovement,
                        cProbeDelta / (double) Math.max(1, cPrevious)
                                * f_dflProbeEfficiency);
        if (dflImprovement >= dflRequired)
            {
            clearProbe();
            if (fDownward)
                {
                // A successful descent intentionally continues quickly so a
                // large inherited I/O window can contract during warmup.
                setBaseline(sample.getThreadCount(), dflAverage);
                }
            else
                {
                // Do not treat two samples that admitted this batch as a
                // fully primed baseline for the next batch. Re-observe the
                // retained window before another upward probe; otherwise
                // noisy remote-client traffic can ratchet several accepted
                // probes together faster than a bad window can be reverted.
                clearBaseline();
                recordBaseline(sample);
                setReference(sample.getThreadCount(), dflAverage);
                }
            clearLowerRejection();
            m_fSeekLower = fDownward
                    && sample.getThreadCount() > Math.max(sample.getMinimumThreadCount(),
                            Math.min(sample.getProcessorCount(), sample.getMaximumThreadCount()));
            return Decision.none("the above-CPU worker probe improved throughput by "
                    + formatPercent(dflImprovement));
            }

        clearProbe();
        setBaseline(cPrevious, dflPreviousThroughput);
        m_fSeekLower = false;
        if (fDownward)
            {
            setLowerRejection(cPrevious, dflPreviousThroughput);
            }
        else
            {
            setReference(cPrevious, dflBaseline);
            setUpperRejection(cPrevious, cPrevious + cProbeDelta, dflPreviousThroughput);
            m_ldtProbeRejected = sample.getTimestamp();
            }
        return Decision.resize(-cProbeDelta,
                "the worker-count probe improved throughput by only "
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

        if (m_cBaselineConcurrency != cThreads
                || m_cBaselineSampleCount < f_cBaselineSamples)
            {
            return Decision.hold("waiting for a stable throughput baseline");
            }

        int cTarget;
        if (dflCpuRatio > 0.0d)
            {
            // CPU/active-wall ratio estimates how much of each worker's
            // lifetime is executable. Grow toward the concurrency required
            // to occupy the effective CPUs, but preserve the resize
            // algorithm's requested delta and validate the whole batch as a
            // throughput probe.
            cTarget = (int) Math.ceil(cProcessors / dflCpuRatio);
            }
        else
            {
            // A BLOCKING_IO pool may not expose usable worker CPU time. Use a
            // measured geometric probe rather than the old one-thread crawl.
            cTarget = cThreads + Math.max(1, cThreads / 2);
            }

        cTarget = Math.min(sample.getMaximumThreadCount(), cTarget);
        if (cAssociations > 0)
            {
            cTarget = Math.min(cTarget, cAssociations);
            }

        // Below twice the CPU allocation the controller can still approach
        // an I/O window quickly. Above that point use a narrower step so a
        // transient ramp cannot leap over a nearby throughput/latency knee.
        int cFastCeiling = cProcessors > Integer.MAX_VALUE / 2
                ? Integer.MAX_VALUE : cProcessors * 2;
        boolean fFastStep = cThreads < cFastCeiling;
        int cStep = Math.max(1, cThreads / (fFastStep ? 2 : 4));
        if (fFastStep)
            {
            cTarget = Math.min(cTarget, cFastCeiling);
            }
        int cDelta = Math.min(Math.min(Math.max(1, cRequestedDelta), cStep),
                cTarget - cThreads);
        if (cDelta <= 0)
            {
            return Decision.hold("the measured blocking-concurrency target");
            }

        int cProbeTarget = cThreads + cDelta;
        if (isUpperProbeRejected(cThreads, cProbeTarget))
            {
            return Decision.hold("the learned worker-probe bound for this workload");
            }
        if (m_cUpperRejectedTarget > 0 && cProbeTarget >= m_cUpperRejectedTarget)
            {
            clearUpperRejection();
            }

        return Decision.grow(cDelta, true, "measured blocking above the CPU allocation");
        }

    /**
     * Record that a probe growth was applied.
     *
     * @param sample       the sample that caused the probe
     * @param cThreadCount the resulting worker count
     */
    public void onProbeApplied(Sample sample, int cThreadCount)
        {
        double dflBaseline = m_cBaselineConcurrency == sample.getThreadCount()
                && m_cBaselineSampleCount >= f_cBaselineSamples
                ? m_dflBaselineAverage : sample.getThroughput();
        startProbe(sample.getThreadCount(), cThreadCount, dflBaseline,
                dflBaseline, false);
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
        m_cProbePrevious          = 0;
        m_cProbeSampleCount       = 0;
        m_dflProbeBaseline        = 0.0d;
        m_dflProbePreviousThroughput = 0.0d;
        m_dflProbeThroughputTotal = 0.0d;
        m_fProbeDownward          = false;
        }

    private void startProbe(int cPrevious, int cTarget, double dflBaseline,
            double dflPreviousThroughput, boolean fDownward)
        {
        m_cProbePrevious          = cPrevious;
        m_cProbeThreads           = cTarget;
        m_cProbeSampleCount       = 0;
        m_dflProbeBaseline        = dflBaseline;
        m_dflProbePreviousThroughput = dflPreviousThroughput;
        m_dflProbeThroughputTotal = 0.0d;
        m_fProbeDownward          = fDownward;
        }

    /**
     * Reset probe and stable-baseline state after an external resize.
     */
    public void reset()
        {
        clearProbe();
        clearBaseline();
        clearReference();
        clearUpperRejection();
        }

    private boolean shouldProbeLower(Sample sample, int cMinimum)
        {
        if (sample.getThreadCount() <= cMinimum
                || m_cBaselineSampleCount < f_cBaselineSamples)
            {
            return false;
            }
        if (m_fSeekLower)
            {
            return true;
            }
        if (m_cLowerRejectedConcurrency == sample.getThreadCount()
                && m_dflLowerRejectedThroughput > 0.0d
                && Math.abs(m_dflBaselineAverage - m_dflLowerRejectedThroughput)
                    < m_dflLowerRejectedThroughput * f_dflWorkloadChange)
            {
            return false;
            }
        boolean fWorkloadChanged = isWorkloadThroughputDrop(sample);
        return isUnderOccupied(sample) || fWorkloadChanged;
        }

    private boolean isWorkloadThroughputDrop(Sample sample)
        {
        return m_cReferenceConcurrency == sample.getThreadCount()
                && m_dflReferenceThroughput > 0.0d
                && m_dflReferenceThroughput - m_dflBaselineAverage
                    >= m_dflReferenceThroughput * f_dflWorkloadChange;
        }

    private boolean isUnderOccupied(Sample sample)
        {
        return sample.getBacklog() <= 0
                && sample.getActiveCount() < sample.getThreadCount() * 0.50d;
        }

    private boolean shouldProbeBlockedWork(Sample sample, int cMinimum)
        {
        int cThreads = sample.getThreadCount();
        if (sample.getBacklog() != 0 || cThreads >= sample.getMaximumThreadCount()
                || m_cBaselineSampleCount < f_cBaselineSamples
                || sample.getActiveCount() < cThreads * 0.75d
                || sample.getTimestamp() < m_ldtProbeRejected + f_cProbeCooldownMillis)
            {
            return false;
            }

        int cAssociations = sample.getActiveAssociationCount();
        if (cAssociations > 0 && cAssociations <= cThreads)
            {
            return false;
            }

        double dflCpuRatio = sample.getWorkerCpuRatio();
        return dflCpuRatio < f_dflCpuThreshold
                && (dflCpuRatio >= 0.0d
                    || sample.getRole() == DaemonPoolSizing.Role.BLOCKING_IO)
                && (cThreads < cMinimum || sample.getThroughput() > 0.0d);
        }

    private void recordBaseline(Sample sample)
        {
        double dflThroughput = sample.getThroughput();
        int    cThreads      = sample.getThreadCount();
        if (dflThroughput <= 0.0d)
            {
            clearBaseline();
            return;
            }

        boolean fSameThreads = m_cBaselineConcurrency == cThreads;
        boolean fStable = fSameThreads && m_dflBaselinePrevious > 0.0d
                && Math.abs(dflThroughput - m_dflBaselinePrevious) / m_dflBaselinePrevious
                        <= f_dflBaselineStability;
        if (!fStable)
            {
            m_cBaselineConcurrency = cThreads;
            m_cBaselineSampleCount = 1;
            m_dflBaselineAverage   = dflThroughput;
            }
        else
            {
            int cWeight = Math.min(m_cBaselineSampleCount, f_cBaselineSamples - 1);
            m_dflBaselineAverage = (m_dflBaselineAverage * cWeight + dflThroughput)
                    / (cWeight + 1);
            m_cBaselineSampleCount = Math.min(f_cBaselineSamples,
                    m_cBaselineSampleCount + 1);
            }
        m_dflBaselinePrevious = dflThroughput;

        if (m_cBaselineSampleCount >= f_cBaselineSamples)
            {
            if (m_dflReferenceThroughput <= 0.0d
                    || (!m_fSeekLower && m_cReferenceConcurrency != cThreads))
                {
                setReference(cThreads, m_dflBaselineAverage);
                }
            else if (!m_fSeekLower && m_dflBaselineAverage
                    >= m_dflReferenceThroughput * (1.0d - f_dflWorkloadChange))
                {
                m_dflReferenceThroughput = m_dflBaselineAverage;
                }
            }
        }

    private void setBaseline(int cThreads, double dflThroughput)
        {
        m_cBaselineConcurrency = cThreads;
        m_cBaselineSampleCount = f_cBaselineSamples;
        m_dflBaselineAverage   = dflThroughput;
        m_dflBaselinePrevious  = dflThroughput;
        }

    private void clearBaseline()
        {
        m_cBaselineConcurrency = 0;
        m_cBaselineSampleCount = 0;
        m_dflBaselineAverage   = 0.0d;
        m_dflBaselinePrevious  = 0.0d;
        }

    private void setReference(int cThreads, double dflThroughput)
        {
        m_cReferenceConcurrency = cThreads;
        m_dflReferenceThroughput = dflThroughput;
        }

    private void clearReference()
        {
        m_cReferenceConcurrency = 0;
        m_dflReferenceThroughput = 0.0d;
        m_fSeekLower = false;
        clearLowerRejection();
        }

    private boolean isUpperProbeRejected(int cThreads, int cTarget)
        {
        return m_cUpperRejectedConcurrency == cThreads
                && cTarget >= m_cUpperRejectedTarget
                && m_dflUpperRejectedThroughput > 0.0d
                && Math.abs(m_dflBaselineAverage - m_dflUpperRejectedThroughput)
                    < m_dflUpperRejectedThroughput * f_dflWorkloadChange;
        }

    private void setUpperRejection(int cThreads, int cTarget, double dflThroughput)
        {
        m_cUpperRejectedConcurrency = cThreads;
        m_cUpperRejectedTarget      = cTarget;
        m_dflUpperRejectedThroughput = dflThroughput;
        }

    private void clearUpperRejection()
        {
        m_cUpperRejectedConcurrency = 0;
        m_cUpperRejectedTarget      = 0;
        m_dflUpperRejectedThroughput = 0.0d;
        }

    private void setLowerRejection(int cThreads, double dflThroughput)
        {
        m_cLowerRejectedConcurrency = cThreads;
        m_dflLowerRejectedThroughput = dflThroughput;
        }

    private void clearLowerRejection()
        {
        m_cLowerRejectedConcurrency = 0;
        m_dflLowerRejectedThroughput = 0.0d;
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
            this(ldtTimestamp, cThreads, cThreadsMin, cThreadsMax, cProcessors,
                    cThreads, cBacklog, cAssociations, dflCpuRatio, dflThroughput, role);
            }

        public Sample(long ldtTimestamp, int cThreads, int cThreadsMin, int cThreadsMax,
                int cProcessors, double dflActiveCount, int cBacklog, int cAssociations,
                double dflCpuRatio, double dflThroughput, DaemonPoolSizing.Role role)
            {
            f_ldtTimestamp    = ldtTimestamp;
            f_cThreads        = cThreads;
            f_cThreadsMin     = cThreadsMin;
            f_cThreadsMax     = cThreadsMax;
            f_cProcessors     = cProcessors;
            f_dflActiveCount  = dflActiveCount;
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
        public double getActiveCount()              { return f_dflActiveCount; }
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
        private final double                f_dflActiveCount;
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
            RESIZE
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
        public static Decision resize(int cDelta, String sReason)
            {
            if (cDelta == 0)
                {
                throw new IllegalArgumentException("a resize delta must not be zero");
                }
            return new Decision(Action.RESIZE, cDelta, true, sReason);
            }
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
    private final int    f_cBaselineSamples;
    private final double f_dflBaselineStability;
    private final double f_dflWorkloadChange;
    private final double f_dflProbeEfficiency;

    private int    m_cProbeThreads;
    private int    m_cProbePrevious;
    private int    m_cProbeSampleCount;
    private double m_dflProbeBaseline;
    private double m_dflProbePreviousThroughput;
    private double m_dflProbeThroughputTotal;
    private long   m_ldtProbeRejected = Long.MIN_VALUE;
    private int    m_cBaselineConcurrency;
    private int    m_cBaselineSampleCount;
    private double m_dflBaselineAverage;
    private double m_dflBaselinePrevious;
    private int    m_cReferenceConcurrency;
    private double m_dflReferenceThroughput;
    private int    m_cUpperRejectedConcurrency;
    private int    m_cUpperRejectedTarget;
    private double m_dflUpperRejectedThroughput;
    private int    m_cLowerRejectedConcurrency;
    private double m_dflLowerRejectedThroughput;
    private boolean m_fProbeDownward;
    private boolean m_fSeekLower;
    }
