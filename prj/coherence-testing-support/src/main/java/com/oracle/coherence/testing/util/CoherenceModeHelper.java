/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.testing.util;

import java.lang.reflect.Method;

/**
 * Test helper for changing the {@code coherence.mode} and
 * {@code coherence.security.mode} system properties together with their
 * memoized values.
 *
 * @author Aleks Seovic  2026.05.05
 * @since 26.07
 */
public final class CoherenceModeHelper
    {
    private CoherenceModeHelper()
        {
        }

    /**
     * Set Coherence to prod mode for the current scope.
     *
     * @return a scope that restores the previous mode when closed
     */
    public static ModeScope prod()
        {
        return mode("prod");
        }

    /**
     * Set Coherence to dev mode for the current scope.
     *
     * @return a scope that restores the previous mode when closed
     */
    public static ModeScope dev()
        {
        return mode("dev");
        }

    /**
     * Set the Coherence mode for the current scope.
     *
     * @param sMode  the mode to set, or {@code null} to clear it
     *
     * @return a scope that restores the previous mode when closed
     */
    public static ModeScope mode(String sMode)
        {
        String sPreviousMode         = System.getProperty(PROP_COHERENCE_MODE);
        String sPreviousSecurityMode = System.getProperty(PROP_SECURITY_MODE);
        restore(sMode);
        return new ModeScope(sPreviousMode, sPreviousSecurityMode);
        }

    /**
     * Set compatibility security mode for the current scope.
     *
     * @return a scope that restores the previous property values when closed
     */
    public static ModeScope securityCompatibility()
        {
        return securityMode(SECURITY_MODE_COMPATIBILITY);
        }

    /**
     * Set hardened security mode for the current scope.
     *
     * @return a scope that restores the previous property values when closed
     */
    public static ModeScope securityHardened()
        {
        return securityMode(SECURITY_MODE_HARDENED);
        }

    /**
     * Set the security mode for the current scope.
     *
     * @param sSecurityMode  the security mode to set, or {@code null} to clear it
     *
     * @return a scope that restores the previous property values when closed
     */
    public static ModeScope securityMode(String sSecurityMode)
        {
        String sPreviousMode         = System.getProperty(PROP_COHERENCE_MODE);
        String sPreviousSecurityMode = System.getProperty(PROP_SECURITY_MODE);
        restoreSecurityMode(sSecurityMode);
        return new ModeScope(sPreviousMode, sPreviousSecurityMode);
        }

    /**
     * Clear the Coherence mode and reset the memoized resolver.
     */
    public static void clear()
        {
        restore(null, null);
        }

    /**
     * Reset the memoized Coherence mode resolver.
     */
    public static void reset()
        {
        try
            {
            Method method = Class.forName(COHERENCE_MODE_CLASS).getDeclaredMethod("resetForTesting");
            method.setAccessible(true);
            method.invoke(null);
            }
        catch (ReflectiveOperationException | RuntimeException e)
            {
            throw new IllegalStateException("Unable to reset memoized Coherence mode", e);
            }
        }

    /**
     * Restore the Coherence mode and reset the memoized resolver.
     *
     * @param sMode  the mode to restore, or {@code null} to clear it
     */
    public static void restore(String sMode)
        {
        if (sMode == null)
            {
            System.clearProperty(PROP_COHERENCE_MODE);
            }
        else
            {
            System.setProperty(PROP_COHERENCE_MODE, sMode);
            }
        reset();
        }

    /**
     * Restore the security mode property and reset the memoized resolver.
     *
     * @param sValue  the value to restore, or {@code null} to clear it
     */
    public static void restoreSecurityMode(String sValue)
        {
        if (sValue == null)
            {
            System.clearProperty(PROP_SECURITY_MODE);
            }
        else
            {
            System.setProperty(PROP_SECURITY_MODE, sValue);
            }
        reset();
        }

    private static void restore(String sMode, String sSecurityMode)
        {
        if (sMode == null)
            {
            System.clearProperty(PROP_COHERENCE_MODE);
            }
        else
            {
            System.setProperty(PROP_COHERENCE_MODE, sMode);
            }

        if (sSecurityMode == null)
            {
            System.clearProperty(PROP_SECURITY_MODE);
            }
        else
            {
            System.setProperty(PROP_SECURITY_MODE, sSecurityMode);
            }

        reset();
        }

    // ---- inner class: ModeScope ------------------------------------------

    /**
     * A scoped Coherence mode override.
     */
    public static final class ModeScope
            implements AutoCloseable
        {
        private ModeScope(String sPreviousMode, String sPreviousSecurityMode)
            {
            f_sPreviousMode         = sPreviousMode;
            f_sPreviousSecurityMode = sPreviousSecurityMode;
            }

        @Override
        public void close()
            {
            if (!m_fClosed)
                {
                restore(f_sPreviousMode, f_sPreviousSecurityMode);
                m_fClosed = true;
                }
            }

        private final String f_sPreviousMode;

        private final String f_sPreviousSecurityMode;

        private boolean m_fClosed;
        }

    // ---- constants -------------------------------------------------------

    private static final String PROP_COHERENCE_MODE = "coherence.mode";

    private static final String PROP_SECURITY_MODE = "coherence.security.mode";

    private static final String SECURITY_MODE_COMPATIBILITY = "compatibility";

    private static final String SECURITY_MODE_HARDENED = "hardened";

    private static final String COHERENCE_MODE_CLASS = "com.tangosol.internal.util.CoherenceMode";
    }
