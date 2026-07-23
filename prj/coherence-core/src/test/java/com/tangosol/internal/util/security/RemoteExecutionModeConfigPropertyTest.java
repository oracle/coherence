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
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Config-mediated property tests for {@link RemoteExecutionMode}.
 *
 * @author Aleks Seovic  2026.05.08
 * @since 26.04
 */
public class RemoteExecutionModeConfigPropertyTest
    {
    @Before
    public void capturePropertyDefaults()
        {
        m_sModeOld          = System.getProperty(CoherenceMode.PROP_COHERENCE_MODE);
        m_sDynamicRemoteOld = System.getProperty(RemoteExecutionMode.PROP_DYNAMIC_REMOTE_UNAUTH);
        }

    @After
    public void cleanup()
        {
        restoreProperty(CoherenceMode.PROP_COHERENCE_MODE, m_sModeOld);
        restoreProperty(RemoteExecutionMode.PROP_DYNAMIC_REMOTE_UNAUTH, m_sDynamicRemoteOld);
        resetMode();
        }

    /**
     * JVM-local tests cannot portably set an environment variable; this test
     * pins the dotted-property path that {@code Config.getProperty(...)}
     * reads, with the env-var alias covered by Slice A round-2's documented
     * deferral.
     */
    @Test
    public void respectsExplicitAllowInProdMode_envVar()
        {
        setMode("prod", "allow");

        assertTrue(RemoteExecutionMode.isDynamicRemoteAllowed());
        }

    @Test
    public void blankValueTreatedAsUnset()
        {
        setMode("prod", " ");
        assertFalse(RemoteExecutionMode.isDynamicRemoteAllowed());

        setMode("dev", " ");
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

    private String m_sModeOld;
    private String m_sDynamicRemoteOld;
    }
