/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.testing.util;

import java.lang.reflect.Method;

/**
 * Test helper for changing the {@code coherence.mode} system property and the
 * memoized Coherence mode value together.
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
     * Set Coherence to legacy mode for the current scope.
     *
     * @return a scope that restores the previous mode when closed
     */
    public static ModeScope legacy()
        {
        return mode("legacy");
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
        String sPrevious = System.getProperty(PROP_COHERENCE_MODE);
        restore(sMode);
        return new ModeScope(sPrevious);
        }

    /**
     * Clear the Coherence mode and reset the memoized resolver.
     */
    public static void clear()
        {
        restore(null);
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

    // ---- inner class: ModeScope ------------------------------------------

    /**
     * A scoped Coherence mode override.
     */
    public static final class ModeScope
            implements AutoCloseable
        {
        private ModeScope(String sPrevious)
            {
            f_sPrevious = sPrevious;
            }

        @Override
        public void close()
            {
            if (!m_fClosed)
                {
                restore(f_sPrevious);
                m_fClosed = true;
                }
            }

        private final String f_sPrevious;

        private boolean m_fClosed;
        }

    // ---- constants -------------------------------------------------------

    private static final String PROP_COHERENCE_MODE = "coherence.mode";

    private static final String COHERENCE_MODE_CLASS = "com.tangosol.internal.util.CoherenceMode";
    }
