/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.security;

import com.oracle.coherence.testing.util.CoherenceModeHelper;

import com.tangosol.internal.util.CoherenceMode;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link RemoteExecutionMode}.
 *
 * @author Aleks Seovic  2026.05.01
 * @since 26.04
 */
public class RemoteExecutionModeTest
    {
    @After
    public void cleanup()
        {
        restoreProperty(CoherenceMode.PROP_COHERENCE_MODE, m_sModeOld);
        restoreProperty(RemoteExecutionMode.PROP_DYNAMIC_REMOTE_UNAUTH, m_sDynamicRemoteOld);
        resetMode();
        }

    @Test
    public void devDefault()
        {
        setMode("dev", null);

        assertTrue(RemoteExecutionMode.isDynamicRemoteAllowed());
        }

    @Test
    public void prodDefault()
        {
        setMode("prod", null);

        assertFalse(RemoteExecutionMode.isDynamicRemoteAllowed());
        }

    @Test
    public void legacyDefault()
        {
        setMode("legacy", null);

        assertTrue(RemoteExecutionMode.isDynamicRemoteAllowed());
        }

    @Test
    public void explicitAllow()
        {
        setMode("prod", "allow");

        assertTrue(RemoteExecutionMode.isDynamicRemoteAllowed());
        }

    @Test
    public void explicitDeny()
        {
        setMode("dev", "deny");

        assertFalse(RemoteExecutionMode.isDynamicRemoteAllowed());
        }

    @Test
    public void invalidValueFallsBackToModeDefault()
        {
        setMode("dev", "maybe");
        assertTrue(RemoteExecutionMode.isDynamicRemoteAllowed());

        setMode("prod", "maybe");
        assertFalse(RemoteExecutionMode.isDynamicRemoteAllowed());

        setMode("legacy", "maybe");
        assertTrue(RemoteExecutionMode.isDynamicRemoteAllowed());
        }

    @Test
    public void resetForTesting()
        {
        setMode("prod", null);

        assertFalse(RemoteExecutionMode.isDynamicRemoteAllowed());

        restoreProperty(RemoteExecutionMode.PROP_DYNAMIC_REMOTE_UNAUTH, "allow");
        assertFalse(RemoteExecutionMode.isDynamicRemoteAllowed());

        RemoteExecutionMode.resetForTesting();
        assertTrue(RemoteExecutionMode.isDynamicRemoteAllowed());
        }

    private static void setMode(String sMode, String sDynamicRemote)
        {
        restoreProperty(CoherenceMode.PROP_COHERENCE_MODE, sMode);
        restoreProperty(RemoteExecutionMode.PROP_DYNAMIC_REMOTE_UNAUTH, sDynamicRemote);
        resetMode();
        }

    private static void restoreProperty(String sName, String sValue)
        {
        if (sValue == null)
            {
            System.clearProperty(sName);
            }
        else
            {
            System.setProperty(sName, sValue);
            }
        }

    private static void resetMode()
        {
        CoherenceModeHelper.reset();
        RemoteExecutionMode.resetForTesting();
        }

    private final String m_sModeOld          = System.getProperty(CoherenceMode.PROP_COHERENCE_MODE);
    private final String m_sDynamicRemoteOld = System.getProperty(RemoteExecutionMode.PROP_DYNAMIC_REMOTE_UNAUTH);
    }
