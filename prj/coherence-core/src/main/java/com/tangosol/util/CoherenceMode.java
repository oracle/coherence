/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

/**
 * Public facade for REST-safe access to the current Coherence mode.
 *
 * @author Vaso Putica  2026.05.10
 * @since 26.04
 */
public final class CoherenceMode
    {
    private CoherenceMode()
        {
        }

    /**
     * Return {@code true} if security hardening is enabled.
     *
     * @return {@code true} if security hardening is enabled
     */
    public static boolean isSecurityHardeningEnabled()
        {
        return com.tangosol.internal.util.CoherenceMode.isSecurityHardeningEnabled();
        }

    /**
     * Return {@code true} if Coherence REST authentication is enforced when
     * REST authentication is engaged.
     *
     * @return {@code true} if Coherence REST authentication is enforced
     */
    public static boolean isCoherenceRestAuthEnforced()
        {
        return com.tangosol.internal.util.CoherenceMode.isCoherenceRestAuthEnforced();
        }

    /**
     * Return {@code true} if Coherence REST pass-through resources require an
     * explicit REST resource configuration.
     *
     * @return {@code true} if pass-through resources require explicit
     *         configuration
     */
    public static boolean isCoherenceRestPassThroughAllowlistRequired()
        {
        return com.tangosol.internal.util.CoherenceMode.isCoherenceRestPassThroughAllowlistRequired();
        }
    }
