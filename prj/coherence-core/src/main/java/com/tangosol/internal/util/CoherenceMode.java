/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util;

import com.oracle.coherence.common.base.Logger;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Central resolver for the {@code coherence.mode} runtime mode.
 *
 * @author Aleks Seovic  2026.05.01
 * @since 26.04
 */
public enum CoherenceMode
    {
    DEV,
    PROD,
    /**
     * Compatibility mode for existing deployments.
     */
    @Deprecated(forRemoval = true, since = "26.04")
    LEGACY;

    /**
     * Return the current Coherence mode.
     *
     * @return the current Coherence mode
     */
    public static CoherenceMode current()
        {
        CoherenceMode mode = S_MODE.get();
        if (mode == null)
            {
            mode = resolve();
            if (!S_MODE.compareAndSet(null, mode))
                {
                mode = S_MODE.get();
                }
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
     * Return {@code true} if Coherence is running in legacy mode.
     *
     * @return {@code true} if Coherence is running in legacy mode
     */
    @SuppressWarnings("removal")
    public static boolean isLegacy()
        {
        return current() == LEGACY;
        }

    /**
     * Return {@code true} if serialization allowlists are enforced.
     *
     * @return {@code true} if serialization allowlists are enforced
     */
    public static boolean isAllowlistEnforced()
        {
        return !isLegacy();
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
        return isProd();
        }

    /**
     * Return {@code true} if remote executable policy is enforced.
     *
     * @return {@code true} if remote executable policy is enforced
     */
    public static boolean isRemoteExecutableEnforced()
        {
        return !isLegacy();
        }

    /**
     * Return {@code true} if Coherence REST authentication is enforced when
     * REST authentication is engaged. The REST auth policy from
     * design/features/security-bugs/plans/rest-01/prompts/02-slice-b-auth-passthrough-implementation.md
     * follows the SER-01 compatibility shape: DEV/PROD fail closed only once
     * the container has engaged authentication, while LEGACY keeps the
     * historical fail-open behavior.
     *
     * @return {@code true} if Coherence REST authentication is enforced
     */
    public static boolean isCoherenceRestAuthEnforced()
        {
        return !isLegacy();
        }

    /**
     * Return {@code true} if Coherence REST pass-through resources require an
     * explicit REST resource configuration. Prompt
     * design/features/security-bugs/plans/rest-01/prompts/02-slice-b-auth-passthrough-implementation.md
     * makes arbitrary cache-name auto-publish a LEGACY-only compatibility path
     * so DEV/PROD expose only operator-configured resources.
     *
     * @return {@code true} if Coherence REST pass-through resources require
     *         an explicit configuration
     */
    public static boolean isCoherenceRestPassThroughAllowlistRequired()
        {
        return !isLegacy();
        }

    /**
     * Return {@code true} if XML parser external-entity protections are
     * required to fail closed. The XML policy from
     * design/features/security-bugs/plans/rest-01/prompts/06-slice-e-saxparser-xxe-implementation.md
     * intentionally follows the standard hardened-mode boundary: DEV/PROD
     * require protection enforcement, while LEGACY keeps compatibility behavior.
     *
     * @return {@code true} if XML parser external-entity protections are
     *         required
     */
    public static boolean isXmlExternalEntityProtectionRequired()
        {
        return !isLegacy();
        }

    /**
     * Reset the memoized mode for tests.
     */
    static void resetForTesting()
        {
        S_MODE.set(null);
        s_warningLogger = Logger::warn;
        }

    /**
     * Set the warning logger for tests.
     *
     * @param logger  the logger to use, or {@code null} to restore default
     */
    static void setWarningLoggerForTesting(Consumer<String> logger)
        {
        s_warningLogger = logger == null ? Logger::warn : logger;
        }

    // ----- helper methods -------------------------------------------------

    private static CoherenceMode resolve()
        {
        String sMode = System.getProperty(PROP_COHERENCE_MODE);
        if (sMode == null || sMode.isBlank())
            {
            return logResolved(LEGACY);
            }

        switch (sMode.trim().toLowerCase(Locale.ROOT))
            {
            case "dev":
            case "development":
                return logResolved(DEV);
            case "legacy":
            case "legacy-compatibility":
                return logResolved(LEGACY);
            case "prod":
            case "production":
                return logResolved(PROD);
            default:
                Logger.err("Invalid coherence.mode value '" + sMode
                        + "'; defaulting to legacy. Expected 'dev', 'prod', or 'legacy'.");
                return logResolved(LEGACY);
            }
        }

    @SuppressWarnings("removal")
    private static CoherenceMode logResolved(CoherenceMode mode)
        {
        if (mode == DEV)
            {
            s_warningLogger.accept(DEV_WARNING);
            }
        else if (mode == LEGACY)
            {
            s_warningLogger.accept(LEGACY_WARNING);
            }
        else
            {
            Logger.info("Coherence is running in PROD mode.");
            }
        return mode;
        }

    // ----- constants ------------------------------------------------------

    /**
     * Runtime mode system property.
     */
    public static final String PROP_COHERENCE_MODE = "coherence.mode";

    /**
     * Prominent dev-mode warning.
     */
    public static final String DEV_WARNING = "Coherence is running in DEV mode. DO NOT use dev mode in production. "
            + "Set -Dcoherence.mode=prod to restore production-safe defaults.";

    /**
     * Prominent legacy-mode warning.
     */
    public static final String LEGACY_WARNING = "Coherence is running in LEGACY mode.\n"
            + "Legacy mode is deprecated for removal and is intended only as a compatibility escape for existing deployments.\n"
            + "Watch coh.executable.policy_check{result=would_reject} shadow telemetry before migrating.\n"
            + "Set -Dcoherence.mode=prod for production or -Dcoherence.mode=dev for development.";

    private static final AtomicReference<CoherenceMode> S_MODE = new AtomicReference<>();

    private static volatile Consumer<String> s_warningLogger = Logger::warn;
    }
