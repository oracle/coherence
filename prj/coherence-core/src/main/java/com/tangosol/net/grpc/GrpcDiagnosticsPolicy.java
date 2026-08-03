/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.grpc;

import com.tangosol.internal.util.CoherenceMode;

import java.util.Locale;

/**
 * Helper methods for gRPC diagnostics configuration.
 *
 * @author Aleks Seovic  2026.05.20
 * @since 26.04
 */
public final class GrpcDiagnosticsPolicy
    {
    /**
     * Utility class.
     */
    private GrpcDiagnosticsPolicy()
        {
        }

    /**
     * Normalize a configured Channelz policy value.
     *
     * @param sValue  the configured value
     *
     * @return the normalized value
     */
    public static String normalizeChannelz(String sValue)
        {
        if (sValue == null || sValue.isBlank())
            {
            return CHANNELZ_AUTO;
            }

        String sNormalized = sValue.trim().toLowerCase(Locale.ROOT);
        if (CHANNELZ_AUTO.equals(sNormalized)
                || CHANNELZ_ENABLED.equals(sNormalized)
                || CHANNELZ_DISABLED.equals(sNormalized))
            {
            return sNormalized;
            }
        throw new IllegalArgumentException("unsupported gRPC channelz: " + sValue);
        }

    /**
     * Return whether Channelz should be registered.
     *
     * @param sChannelz  the configured Channelz policy
     *
     * @return {@code true} if Channelz should be registered
     */
    public static boolean isChannelzEnabled(String sChannelz)
        {
        String sPolicy = normalizeChannelz(sChannelz);
        if (CHANNELZ_ENABLED.equals(sPolicy))
            {
            return true;
            }
        if (CHANNELZ_DISABLED.equals(sPolicy))
            {
            return false;
            }
        return !CoherenceMode.isProd();
        }

    /**
     * Normalize a configured error-disclosure policy value.
     *
     * @param sValue  the configured value
     *
     * @return the normalized value
     */
    public static String normalizeErrorDisclosure(String sValue)
        {
        if (sValue == null || sValue.isBlank())
            {
            return ERROR_DISCLOSURE_AUTO;
            }

        String sNormalized = sValue.trim().toLowerCase(Locale.ROOT);
        if (ERROR_DISCLOSURE_AUTO.equals(sNormalized)
                || ERROR_DISCLOSURE_SAFE.equals(sNormalized)
                || ERROR_DISCLOSURE_DIAGNOSTIC.equals(sNormalized))
            {
            return sNormalized;
            }
        throw new IllegalArgumentException("unsupported gRPC error-disclosure: " + sValue);
        }

    /**
     * Return whether error disclosure should suppress remote diagnostic details.
     *
     * @param sErrorDisclosure  the configured error-disclosure policy
     *
     * @return {@code true} if safe disclosure should be used
     */
    public static boolean isErrorDisclosureSafe(String sErrorDisclosure)
        {
        String sPolicy = normalizeErrorDisclosure(sErrorDisclosure);
        if (ERROR_DISCLOSURE_SAFE.equals(sPolicy))
            {
            return true;
            }
        if (ERROR_DISCLOSURE_DIAGNOSTIC.equals(sPolicy))
            {
            return false;
            }
        return !CoherenceMode.isDev();
        }

    /**
     * Mode-aware Channelz policy.
     */
    public static final String CHANNELZ_AUTO = "auto";

    /**
     * Explicitly enable Channelz.
     */
    public static final String CHANNELZ_ENABLED = "enabled";

    /**
     * Explicitly disable Channelz.
     */
    public static final String CHANNELZ_DISABLED = "disabled";

    /**
     * Mode-aware error-disclosure policy.
     */
    public static final String ERROR_DISCLOSURE_AUTO = "auto";

    /**
     * Suppress remote stack metadata and serialized server exception graphs.
     */
    public static final String ERROR_DISCLOSURE_SAFE = "safe";

    /**
     * Preserve legacy rich diagnostics.
     */
    public static final String ERROR_DISCLOSURE_DIAGNOSTIC = "diagnostic";

    /**
     * Server-side Channelz system property.
     */
    public static final String PROP_CHANNELZ = "coherence.grpc.channelz";

    /**
     * Server-side error-disclosure system property.
     */
    public static final String PROP_ERROR_DISCLOSURE = "coherence.grpc.error-disclosure";
    }
