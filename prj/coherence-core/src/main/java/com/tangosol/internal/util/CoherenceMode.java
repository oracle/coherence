/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Central resolver for the {@code coherence.mode} runtime mode and the
 * {@code coherence.security.mode} security hardening opt-in.
 *
 * @author Aleks Seovic  2026.05.01
 * @since 26.07
 */
public enum CoherenceMode
    {
    EVAL,
    DEV,
    PROD;

    /**
     * Return the current Coherence mode.
     *
     * @return the current Coherence mode
     */
    public static CoherenceMode current()
        {
        CoherenceMode mode = S_MODE.get();
        while (mode == null)
            {
            mode = resolve();
            if (S_MODE.compareAndSet(null, mode))
                {
                return mode;
                }
            mode = S_MODE.get();
            }
        return mode;
        }

    /**
     * Return {@code true} if Coherence is running in dev mode.
     *
     * @return {@code true} if Coherence is running in dev mode
     */
    public static boolean isDev()
        {
        return current() == DEV;
        }

    /**
     * Return {@code true} if Coherence is running in prod mode.
     *
     * @return {@code true} if Coherence is running in prod mode
     */
    public static boolean isProd()
        {
        return current() == PROD;
        }

    /**
     * Return {@code true} if security hardening is enabled.
     *
     * @return {@code true} if security hardening is enabled
     */
    public static boolean isSecurityHardeningEnabled()
        {
        Boolean fEnabled = S_SECURITY_HARDENING_ENABLED.get();
        while (fEnabled == null)
            {
            fEnabled = resolveSecurityHardeningEnabled();
            if (S_SECURITY_HARDENING_ENABLED.compareAndSet(null, fEnabled))
                {
                return fEnabled;
                }
            fEnabled = S_SECURITY_HARDENING_ENABLED.get();
            }
        return fEnabled;
        }

    /**
     * Return {@code true} if serialization allowlists are enforced by default.
     *
     * @return {@code true} if serialization allowlists are enforced
     */
    public static boolean isAllowlistEnforced()
        {
        return isSecurityHardeningEnabled();
        }

    /**
     * Return {@code true} if unauthenticated dynamic remote payloads default
     * to deny.
     *
     * @return {@code true} if unauthenticated dynamic remote payloads default
     *         to deny
     */
    public static boolean isDynamicRemoteDefaultDeny()
        {
        return isSecurityHardeningEnabled();
        }

    /**
     * Return {@code true} if remote executable policy is enforced.
     *
     * @return {@code true} if remote executable policy is enforced
     */
    public static boolean isRemoteExecutableEnforced()
        {
        return isSecurityHardeningEnabled();
        }

    /**
     * Return {@code true} if Coherence REST authentication is enforced when REST
     * authentication is engaged.
     *
     * @return {@code true} if Coherence REST authentication is enforced
     */
    public static boolean isCoherenceRestAuthEnforced()
        {
        return isSecurityHardeningEnabled();
        }

    /**
     * Return {@code true} if Coherence REST pass-through resources require an
     * explicit REST resource configuration.
     *
     * @return {@code true} if Coherence REST pass-through resources require
     *         an explicit configuration
     */
    public static boolean isCoherenceRestPassThroughAllowlistRequired()
        {
        return isSecurityHardeningEnabled();
        }

    /**
     * Return {@code true} if XML parser external-entity protections are required
     * to fail closed.
     *
     * @return {@code true} if XML parser external-entity protections are
     *         required
     */
    public static boolean isXmlExternalEntityProtectionRequired()
        {
        return isSecurityHardeningEnabled();
        }

    /**
     * Reset the memoized mode for tests.
     */
    static void resetForTesting()
        {
        S_MODE.set(null);
        S_SECURITY_HARDENING_ENABLED.set(null);
        }

    // ----- helper methods -------------------------------------------------

    private static CoherenceMode resolve()
        {
        String sMode = System.getProperty(PROP_COHERENCE_MODE);
        if (sMode == null)
            {
            return DEV;
            }

        switch (sMode.toLowerCase(Locale.ROOT))
            {
            case "eval":
                return EVAL;
            case "dev":
            case "development":
                return DEV;
            case "prod":
            case "production":
                return PROD;
            default:
                throw new IllegalArgumentException("Invalid mode " + sMode);
            }
        }

    private static boolean resolveSecurityHardeningEnabled()
        {
        String sSecurityMode = System.getProperty(PROP_SECURITY_MODE);
        if (sSecurityMode == null)
            {
            return false;
            }

        String sTrimmed = sSecurityMode.trim();
        if (sTrimmed.isEmpty())
            {
            throw new IllegalArgumentException("Invalid security mode " + sSecurityMode);
            }

        switch (sTrimmed.toLowerCase(Locale.ROOT))
            {
            case SECURITY_MODE_COMPATIBILITY:
                return false;
            case SECURITY_MODE_HARDENED:
                return true;
            default:
                throw new IllegalArgumentException("Invalid security mode " + sSecurityMode);
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * Runtime mode system property.
     */
    public static final String PROP_COHERENCE_MODE = "coherence.mode";

    /**
     * Security mode system property.
     *
     * @since 15.1.2.0
     */
    public static final String PROP_SECURITY_MODE = "coherence.security.mode";

    /**
     * Security mode value that preserves prior-patch compatibility.
     */
    public static final String SECURITY_MODE_COMPATIBILITY = "compatibility";

    /**
     * Security mode value that enables compatibility-sensitive hardening.
     */
    public static final String SECURITY_MODE_HARDENED = "hardened";

    private static final AtomicReference<CoherenceMode> S_MODE = new AtomicReference<>();

    private static final AtomicReference<Boolean> S_SECURITY_HARDENING_ENABLED = new AtomicReference<>();
    }
