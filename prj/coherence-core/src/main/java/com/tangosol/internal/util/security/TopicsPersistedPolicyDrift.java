/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.security;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.coherence.config.Config;

import com.tangosol.internal.util.CoherenceMode;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Resolver for persisted topic subscriber policy-drift behavior.
 *
 * @author Aleks Seovic  2026.05.11
 * @since 26.04
 */
public final class TopicsPersistedPolicyDrift
    {
    /**
     * Runtime policy for persisted topic subscriber filter/extractor drift.
     */
    public static final String PROP_PERSISTED_POLICY_DRIFT = "coherence.topics.persisted.policy-drift";

    /**
     * Reject persisted metadata whose class is no longer executable.
     */
    public static final String VALUE_REJECT = "reject";

    /**
     * Warn and allow persisted metadata whose class is no longer executable.
     */
    public static final String VALUE_WARN_ALLOW = "warn-allow";

    /**
     * Return the current policy-drift behavior.
     *
     * @return {@link #VALUE_REJECT} or {@link #VALUE_WARN_ALLOW}
     */
    public static String current()
        {
        String sValue = S_POLICY.get();
        if (sValue == null)
            {
            sValue = resolve();
            if (!S_POLICY.compareAndSet(null, sValue))
                {
                sValue = S_POLICY.get();
                }
            }
        return sValue;
        }

    /**
     * Return {@code true} if drifted persisted metadata should be rejected.
     *
     * @return {@code true} if drifted persisted metadata should be rejected
     */
    public static boolean isReject()
        {
        return VALUE_REJECT.equals(current());
        }

    /**
     * Reset cached policy for tests.
     */
    public static void resetForTesting()
        {
        S_POLICY.set(null);
        }

    private static String resolve()
        {
        String sValue = Config.getProperty(PROP_PERSISTED_POLICY_DRIFT);
        if (sValue != null && sValue.isBlank())
            {
            sValue = null;
            }

        String sDefault = defaultValue();
        if (sValue == null)
            {
            return sDefault;
            }

        switch (sValue.trim().toLowerCase(Locale.ROOT))
            {
            case VALUE_REJECT:
                return VALUE_REJECT;
            case VALUE_WARN_ALLOW:
                return VALUE_WARN_ALLOW;
            default:
                Logger.warn("Invalid " + PROP_PERSISTED_POLICY_DRIFT
                        + " value '" + sValue + "'; defaulting to " + sDefault
                        + ". Expected 'reject' or 'warn-allow'.");
                return sDefault;
            }
        }

    private static String defaultValue()
        {
        return CoherenceMode.isProd() ? VALUE_REJECT : VALUE_WARN_ALLOW;
        }

    private TopicsPersistedPolicyDrift()
        {
        }

    private static final AtomicReference<String> S_POLICY = new AtomicReference<>();
    }
