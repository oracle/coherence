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
 * A small stateful hill-climbing policy for a bounded concurrency window.
 *
 * <p>The policy is intentionally independent of the execution mechanism. A
 * caller supplies interval throughput and saturation, then applies the
 * returned target to either a worker pool or an admission gate. Growth is
 * speculative and retained only when it produces useful throughput. This
 * allows blocking work to discover substantially more concurrency than the
 * CPU count without encoding a VM-shape-specific fixed limit, while keeping
 * short CPU-bound work close to its initial CPU-derived window.</p>
 *
 * @author Aleks Seovic  2026.08.29
 * @since 26.10
 */
public class AdaptiveConcurrencyPolicy
    {
    // ----- constructors -------------------------------------------------

    /**
     * Construct a policy using system-property configuration.
     */
    public AdaptiveConcurrencyPolicy()
        {
        this(Config.getInteger("coherence.daemonpool.admission.probe.samples", 5),
             Config.getDouble("coherence.daemonpool.admission.probe.improvement", 0.02d),
             Config.getLong("coherence.daemonpool.admission.probe.cooldown", 30000L),
             Config.getInteger("coherence.daemonpool.admission.baseline.samples", 5),
             Config.getDouble("coherence.daemonpool.admission.baseline.stability", 0.10d),
             Config.getDouble("coherence.daemonpool.admission.baseline.change", 0.20d),
             Config.getDouble("coherence.daemonpool.admission.probe.efficiency", 0.75d));
        }

    /**
     * Construct a policy with explicit settings.
     *
     * @param cProbeSamples         post-grow samples used to evaluate a probe
     * @param dflProbeImprovement   minimum relative throughput improvement
     * @param cProbeCooldownMillis  rejected-probe cooldown
     */
    AdaptiveConcurrencyPolicy(int cProbeSamples, double dflProbeImprovement,
            long cProbeCooldownMillis)
        {
        this(cProbeSamples, dflProbeImprovement, cProbeCooldownMillis,
                3, 0.10d, 0.20d, 0.10d);
        }

    AdaptiveConcurrencyPolicy(int cProbeSamples, double dflProbeImprovement,
            long cProbeCooldownMillis, int cBaselineSamples,
            double dflBaselineStability,
            double dflWorkloadChange, double dflProbeEfficiency)
        {
        f_cProbeSamples         = Math.max(1, cProbeSamples);
        f_dflProbeImprovement   = Math.max(0.0d, dflProbeImprovement);
        f_cProbeCooldownMillis  = Math.max(0L, cProbeCooldownMillis);
        f_cBaselineSamples      = Math.max(1, cBaselineSamples);
        f_dflBaselineStability  = Math.max(0.0d, dflBaselineStability);
        f_dflWorkloadChange     = Math.max(f_dflBaselineStability, dflWorkloadChange);
        f_dflProbeEfficiency    = Math.max(0.0d, dflProbeEfficiency);
        }

    // ----- policy -------------------------------------------------------

    /**
     * Evaluate the supplied interval sample.
     *
     * @param sample  the interval sample
     *
     * @return the concurrency decision
     */
    public Decision evaluate(Sample sample)
        {
        int cCurrent = sample.getConcurrency();

        if (m_cProbeTarget > 0)
            {
            if (cCurrent != m_cProbeTarget)
                {
                clearProbe();
                clearBaseline();
                clearReference();
                clearUpperRejection();
                }
            else
                {
                m_dflProbeThroughputTotal += sample.getThroughput();
                if (++m_cProbeSampleCount < f_cProbeSamples)
                    {
                    return Decision.hold("a concurrency probe still being evaluated");
                    }

                double dflAverage = m_dflProbeThroughputTotal / m_cProbeSampleCount;
                double dflImprovement = m_dflProbeBaseline <= 0.0d
                        ? (dflAverage > 0.0d ? 1.0d : 0.0d)
                        : (dflAverage - m_dflProbeBaseline) / m_dflProbeBaseline;
                double dflBaseline = m_dflProbeBaseline;
                double dflPreviousThroughput = m_dflProbePreviousThroughput;
                int    cPrevious   = m_cProbePrevious;
                boolean fDownward  = m_fProbeDownward;
                boolean fRequiresGain = m_fProbeRequiresGain;

                int cMinimum = Math.max(1, sample.getMinimumConcurrency());
                int cFastCeiling = cMinimum > Integer.MAX_VALUE / 2
                        ? Integer.MAX_VALUE : cMinimum * 2;
                boolean fBootstrap = !fDownward
                        && cPrevious < cFastCeiling
                        && cCurrent <= cFastCeiling;
                // The first I/O-window jump is discovery from a deliberately
                // conservative role minimum. Beyond it, require high marginal
                // efficiency: at the default 0.75 a 25% concurrency step must
                // earn 18.75% throughput, bounding its implied saturated
                // mean-latency cost to roughly 5.3% by Little's law.
                double dflEfficiency = fBootstrap
                        ? Math.min(0.10d, f_dflProbeEfficiency)
                        : f_dflProbeEfficiency;
                double dflRequired = fDownward
                        ? (fRequiresGain
                                ? f_dflProbeImprovement
                                : -f_dflProbeImprovement)
                        : Math.max(f_dflProbeImprovement,
                                (cCurrent - cPrevious) / (double) Math.max(1, cPrevious)
                                        * dflEfficiency);

                clearProbe();
                boolean fRetainFirstWindow = fBootstrap && sample.isFirstWindowFloor();
                if (fRetainFirstWindow || dflImprovement >= dflRequired)
                    {
                    if (fDownward)
                        {
                        // A successful descent can continue immediately while
                        // every lower step is still compared with the original
                        // upper-window reference throughput.
                        setBaseline(cCurrent, dflAverage);
                        }
                    else
                        {
                        // Retaining an upward probe establishes the new
                        // reference, but not another fully primed baseline.
                        // Require a fresh stable window at the retained
                        // concurrency before allowing another upward probe.
                        clearBaseline();
                        recordBaseline(sample);
                        setReference(cCurrent, dflAverage);
                        }
                    clearLowerRejection();
                    m_fSeekLower = fDownward && cCurrent > sample.getMinimumConcurrency();
                    return Decision.none(fRetainFirstWindow
                            ? "the saturated blocking-I/O domain established its first useful window"
                            : "the concurrency probe improved throughput by "
                                    + formatPercent(dflImprovement));
                    }

                setBaseline(cPrevious, dflPreviousThroughput);
                m_fSeekLower = false;
                if (fDownward)
                    {
                    setLowerRejection(cPrevious, dflPreviousThroughput);
                    }
                else
                    {
                    setReference(cPrevious, dflBaseline);
                    setUpperRejection(cPrevious, cCurrent, dflPreviousThroughput);
                    m_ldtProbeRejected = sample.getTimestamp();
                    }
                return Decision.resize(cPrevious, false,
                        "the concurrency probe improved throughput by only "
                                + formatPercent(dflImprovement));
                }
            }

        int cMinimum = Math.max(1, sample.getMinimumConcurrency());
        int cMaximum = Math.max(cMinimum, sample.getMaximumConcurrency());
        int cFirstWindow = cMinimum > Integer.MAX_VALUE / 2
                ? Integer.MAX_VALUE : cMinimum * 2;
        recordBaseline(sample);
        recordBootstrapSaturation(sample, cFirstWindow);

        if (shouldProbeLower(sample, cFirstWindow))
            {
            boolean fContinuing   = m_fSeekLower;
            boolean fShapeChange = isWorkloadShapeChanged(sample);
            int cTarget = Math.max(cFirstWindow,
                    cCurrent - Math.max(1, cCurrent / 2));
            if (cTarget >= cCurrent)
                {
                cTarget = cCurrent - 1;
                }
            if (fShapeChange)
                {
                // Different request shapes can have inherently different
                // operation rates. Compare the descent with the new shape's
                // current window, not the preceding shape's throughput.
                setReference(cCurrent, m_dflBaselineAverage);
                }
            double dflReference = m_dflReferenceThroughput > 0.0d
                    ? m_dflReferenceThroughput : m_dflBaselineAverage;
            startProbe(cCurrent, cTarget, dflReference,
                    m_dflBaselineAverage, true, sample.getBacklog() > 0);
            m_fSeekLower = false;
            return Decision.resize(cTarget, true,
                    fContinuing
                            ? "a continuing successful lower-concurrency probe"
                            : fShapeChange
                                ? "a stable workload-shape change at the current concurrency"
                                : "a saturated concurrency-knee probe");
            }

        if (sample.getBacklog() > 0 && sample.getActiveConcurrency() >= cCurrent)
            {
            boolean fFastStep = cCurrent < cFirstWindow;
            if (cCurrent >= cMaximum)
                {
                return Decision.hold("the concurrency ceiling");
                }
            if (sample.getTimestamp() < m_ldtProbeRejected + f_cProbeCooldownMillis)
                {
                return Decision.hold("the rejected concurrency-probe cooldown");
                }
            // Reach the first useful I/O window quickly, then use a narrower
            // step so a transient ramp cannot jump across a nearby knee. The
            // deliberately conservative bootstrap window does not require an
            // already-stable throughput rate: uneven proxy connection traffic
            // can remain saturated while never producing five adjacent samples
            // within 10%. Three saturated/queued observations during the same
            // live workload establish a measured bootstrap average. The normal
            // probe still evaluates the larger window for ordinary service
            // domains; a known blocking-I/O domain retains this first useful
            // window because member-local throughput moves with load balancing
            // and cannot reliably measure the cluster-wide benefit.
            int cStep = fFastStep
                    ? cCurrent
                    : Math.max(1, cCurrent / 4);
            int cTarget = Math.min(cMaximum, cCurrent + cStep);
            if (fFastStep)
                {
                cTarget = Math.min(cTarget, cFirstWindow);
                }
            if (isUpperProbeRejected(cCurrent, cTarget))
                {
                clearBootstrapSaturation();
                return Decision.hold("the learned concurrency-probe bound for this workload");
                }
            if (fFastStep
                    ? m_cBootstrapSampleCount < BOOTSTRAP_SATURATION_SAMPLES
                    : m_cBaselineSampleCount < f_cBaselineSamples)
                {
                return Decision.hold(fFastStep
                        ? "waiting for sustained bootstrap saturation"
                        : "waiting for a stable throughput baseline");
                }
            if (m_cUpperRejectedTarget > 0 && cTarget >= m_cUpperRejectedTarget)
                {
                clearUpperRejection();
                }
            double dflBaseline = fFastStep
                    ? m_dflBootstrapThroughputTotal / m_cBootstrapSampleCount
                    : m_dflBaselineAverage;
            startProbe(cCurrent, cTarget, dflBaseline,
                    dflBaseline, false, false);
            clearBootstrapSaturation();

            return Decision.resize(cTarget, true, "saturated concurrency with queued work");
            }

        return Decision.none();
        }

    /**
     * Clear any pending probe. This is used when an external hard ceiling
     * reshapes the actuator independently of the policy.
     */
    public void reset()
        {
        clearProbe();
        clearBaseline();
        clearBootstrapSaturation();
        clearReference();
        clearUpperRejection();
        }

    private void startProbe(int cPrevious, int cTarget, double dflBaseline,
            double dflPreviousThroughput, boolean fDownward,
            boolean fRequiresGain)
        {
        m_cProbePrevious          = cPrevious;
        m_cProbeTarget            = cTarget;
        m_cProbeSampleCount       = 0;
        m_dflProbeBaseline        = dflBaseline;
        m_dflProbePreviousThroughput = dflPreviousThroughput;
        m_dflProbeThroughputTotal = 0.0d;
        m_fProbeDownward          = fDownward;
        m_fProbeRequiresGain      = fRequiresGain;
        }

    private void clearProbe()
        {
        m_cProbePrevious        = 0;
        m_cProbeTarget          = 0;
        m_cProbeSampleCount     = 0;
        m_dflProbeBaseline      = 0.0d;
        m_dflProbePreviousThroughput = 0.0d;
        m_dflProbeThroughputTotal = 0.0d;
        m_fProbeDownward        = false;
        m_fProbeRequiresGain    = false;
        }

    private boolean shouldProbeLower(Sample sample, int cFirstWindow)
        {
        if (sample.getConcurrency() <= cFirstWindow
                || m_cBaselineSampleCount < f_cBaselineSamples)
            {
            return false;
            }
        if (m_fSeekLower)
            {
            return true;
            }
        if (m_cLowerRejectedConcurrency == sample.getConcurrency()
                && m_dflLowerRejectedThroughput > 0.0d
                && Math.abs(m_dflBaselineAverage - m_dflLowerRejectedThroughput)
                    < m_dflLowerRejectedThroughput * f_dflWorkloadChange)
            {
            return false;
            }
        // An admission window is not a worker allocation: unused permits
        // consume no threads or task state. Probe downward only when the
        // current gate is actively saturated and queued work indicates that
        // a smaller window could move an overloaded workload back below its
        // knee. Never descend below the first useful I/O window: independent
        // member-local probes around that bootstrap boundary are too
        // susceptible to traffic variance and can leave cluster members at
        // asymmetric limits. Operation-rate changes in an under-filled window
        // are not evidence that the admission target itself is too large.
        return isWorkloadShapeChanged(sample)
                && sample.getBacklog() > 0
                && sample.getActiveConcurrency() >= sample.getConcurrency();
        }

    private boolean isWorkloadShapeChanged(Sample sample)
        {
        return m_cReferenceConcurrency == sample.getConcurrency()
                && m_dflReferenceThroughput > 0.0d
                && Math.abs(m_dflBaselineAverage - m_dflReferenceThroughput)
                    >= m_dflReferenceThroughput * f_dflWorkloadChange;
        }

    private void recordBaseline(Sample sample)
        {
        double dflThroughput = sample.getThroughput();
        int    cConcurrency  = sample.getConcurrency();
        if (dflThroughput <= 0.0d)
            {
            clearBaseline();
            return;
            }

        boolean fSameConcurrency = m_cBaselineConcurrency == cConcurrency;
        boolean fStable = fSameConcurrency && m_dflBaselinePrevious > 0.0d
                && Math.abs(dflThroughput - m_dflBaselinePrevious) / m_dflBaselinePrevious
                        <= f_dflBaselineStability;
        if (!fStable)
            {
            m_cBaselineConcurrency = cConcurrency;
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
                    || (!m_fSeekLower && m_cReferenceConcurrency != cConcurrency))
                {
                setReference(cConcurrency, m_dflBaselineAverage);
                }
            else if (!m_fSeekLower && m_dflBaselineAverage
                    >= m_dflReferenceThroughput * (1.0d - f_dflWorkloadChange))
                {
                // Follow ordinary drift, but preserve a material stable drop
                // long enough to validate it with a downward probe.
                m_dflReferenceThroughput = m_dflBaselineAverage;
                }
            }
        }

    /**
     * Record saturation at the conservative starting window.
     * This is intentionally independent of throughput stability; the
     * resulting larger-window probe still performs the normal measured
     * throughput comparison.
     */
    private void recordBootstrapSaturation(Sample sample, int cFirstWindow)
        {
        int cConcurrency = sample.getConcurrency();
        if (cConcurrency >= cFirstWindow || sample.getThroughput() <= 0.0d)
            {
            clearBootstrapSaturation();
            return;
            }

        if (m_cBootstrapConcurrency != cConcurrency)
            {
            clearBootstrapSaturation();
            m_cBootstrapConcurrency = cConcurrency;
            }
        if (sample.getBacklog() > 0 && sample.getActiveConcurrency() >= cConcurrency)
            {
            m_cBootstrapSampleCount++;
            m_dflBootstrapThroughputTotal += sample.getThroughput();
            }
        }

    private void clearBootstrapSaturation()
        {
        m_cBootstrapConcurrency        = 0;
        m_cBootstrapSampleCount        = 0;
        m_dflBootstrapThroughputTotal  = 0.0d;
        }

    private void setBaseline(int cConcurrency, double dflThroughput)
        {
        m_cBaselineConcurrency = cConcurrency;
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

    private void setReference(int cConcurrency, double dflThroughput)
        {
        m_cReferenceConcurrency = cConcurrency;
        m_dflReferenceThroughput = dflThroughput;
        }

    private void clearReference()
        {
        m_cReferenceConcurrency = 0;
        m_dflReferenceThroughput = 0.0d;
        m_fSeekLower = false;
        clearLowerRejection();
        }

    /**
     * Return whether the same upper probe already failed for the current
     * stable workload shape.
     */
    private boolean isUpperProbeRejected(int cConcurrency, int cTarget)
        {
        return m_cUpperRejectedConcurrency == cConcurrency
                && cTarget >= m_cUpperRejectedTarget
                && m_dflUpperRejectedThroughput > 0.0d
                && Math.abs(m_dflBaselineAverage - m_dflUpperRejectedThroughput)
                    < m_dflUpperRejectedThroughput * f_dflWorkloadChange;
        }

    private void setUpperRejection(int cConcurrency, int cTarget, double dflThroughput)
        {
        m_cUpperRejectedConcurrency = cConcurrency;
        m_cUpperRejectedTarget      = cTarget;
        m_dflUpperRejectedThroughput = dflThroughput;
        }

    private void clearUpperRejection()
        {
        m_cUpperRejectedConcurrency = 0;
        m_cUpperRejectedTarget      = 0;
        m_dflUpperRejectedThroughput = 0.0d;
        }

    private void setLowerRejection(int cConcurrency, double dflThroughput)
        {
        m_cLowerRejectedConcurrency = cConcurrency;
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
     * An interval sample for one independent concurrency domain.
     */
    public static class Sample
        {
        public Sample(long ldtTimestamp, int cConcurrency, int cMinimumConcurrency,
                int cMaximumConcurrency, int cActiveConcurrency, int cBacklog,
                double dflThroughput)
            {
            this(ldtTimestamp, cConcurrency, cMinimumConcurrency,
                    cMaximumConcurrency, cActiveConcurrency, cBacklog,
                    dflThroughput, false);
            }

        public Sample(long ldtTimestamp, int cConcurrency, int cMinimumConcurrency,
                int cMaximumConcurrency, int cActiveConcurrency, int cBacklog,
                double dflThroughput, boolean fFirstWindowFloor)
            {
            f_ldtTimestamp        = ldtTimestamp;
            f_cConcurrency        = cConcurrency;
            f_cMinimumConcurrency = cMinimumConcurrency;
            f_cMaximumConcurrency = cMaximumConcurrency;
            f_cActiveConcurrency  = cActiveConcurrency;
            f_cBacklog            = cBacklog;
            f_dflThroughput       = dflThroughput;
            f_fFirstWindowFloor   = fFirstWindowFloor;
            }

        public long getTimestamp()          { return f_ldtTimestamp; }
        public int getConcurrency()         { return f_cConcurrency; }
        public int getMinimumConcurrency()  { return f_cMinimumConcurrency; }
        public int getMaximumConcurrency()  { return f_cMaximumConcurrency; }
        public int getActiveConcurrency()   { return f_cActiveConcurrency; }
        public int getBacklog()             { return f_cBacklog; }
        public double getThroughput()       { return f_dflThroughput; }
        public boolean isFirstWindowFloor() { return f_fFirstWindowFloor; }

        private final long   f_ldtTimestamp;
        private final int    f_cConcurrency;
        private final int    f_cMinimumConcurrency;
        private final int    f_cMaximumConcurrency;
        private final int    f_cActiveConcurrency;
        private final int    f_cBacklog;
        private final double f_dflThroughput;
        private final boolean f_fFirstWindowFloor;
        }

    // ----- inner class: Decision ---------------------------------------

    /**
     * A target-concurrency decision.
     */
    public static class Decision
        {
        public enum Action
            {
            NONE,
            HOLD,
            RESIZE
            }

        private Decision(Action action, int cTarget, boolean fProbe, String sReason)
            {
            f_action  = action;
            f_cTarget = cTarget;
            f_fProbe  = fProbe;
            f_sReason = sReason;
            }

        public static Decision none()               { return new Decision(Action.NONE, 0, false, null); }
        public static Decision none(String sReason) { return new Decision(Action.NONE, 0, false, sReason); }
        public static Decision hold(String sReason) { return new Decision(Action.HOLD, 0, false, sReason); }
        public static Decision resize(int cTarget, boolean fProbe, String sReason)
            {
            return new Decision(Action.RESIZE, cTarget, fProbe, sReason);
            }

        public Action getAction()  { return f_action; }
        public int getTarget()     { return f_cTarget; }
        public boolean isProbe()   { return f_fProbe; }
        public String getReason()  { return f_sReason; }

        private final Action  f_action;
        private final int     f_cTarget;
        private final boolean f_fProbe;
        private final String  f_sReason;
        }

    // ----- data members -------------------------------------------------

    private final int    f_cProbeSamples;
    private final double f_dflProbeImprovement;
    private final long   f_cProbeCooldownMillis;
    private final int    f_cBaselineSamples;
    private final double f_dflBaselineStability;
    private final double f_dflWorkloadChange;
    private final double f_dflProbeEfficiency;

    /** Queued/saturated observations needed for the first I/O probe. */
    private static final int BOOTSTRAP_SATURATION_SAMPLES = 3;

    private int    m_cProbePrevious;
    private int    m_cProbeTarget;
    private int    m_cProbeSampleCount;
    private double m_dflProbeBaseline;
    private double m_dflProbePreviousThroughput;
    private double m_dflProbeThroughputTotal;
    private long   m_ldtProbeRejected = Long.MIN_VALUE;
    private int    m_cBaselineConcurrency;
    private int    m_cBaselineSampleCount;
    private double m_dflBaselineAverage;
    private double m_dflBaselinePrevious;
    private int    m_cBootstrapConcurrency;
    private int    m_cBootstrapSampleCount;
    private double m_dflBootstrapThroughputTotal;
    private int    m_cReferenceConcurrency;
    private double m_dflReferenceThroughput;
    private int    m_cUpperRejectedConcurrency;
    private int    m_cUpperRejectedTarget;
    private double m_dflUpperRejectedThroughput;
    private int    m_cLowerRejectedConcurrency;
    private double m_dflLowerRejectedThroughput;
    private boolean m_fProbeDownward;
    private boolean m_fProbeRequiresGain;
    private boolean m_fSeekLower;
    }
