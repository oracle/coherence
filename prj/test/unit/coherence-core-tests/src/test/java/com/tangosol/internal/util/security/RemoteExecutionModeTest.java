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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link RemoteExecutionMode}.
 *
 * @author Aleks Seovic  2026.05.01
 * @since 26.07
 */
public class RemoteExecutionModeTest
    {
    @After
    public void cleanup()
        {
        restoreProperty(CoherenceMode.PROP_COHERENCE_MODE, m_sModeOld);
        restoreProperty(CoherenceMode.PROP_SECURITY_MODE, m_sSecurityModeOld);
        restoreProperty(RemoteExecutionMode.PROP_DYNAMIC_REMOTE_UNAUTH, m_sDynamicRemoteOld);
        resetMode();
        }

    @Test
    public void defaultsToAllowInEveryMode()
        {
        assertDynamicRemotePolicy(null, true);
        }

    @Test
    public void explicitAllowInEveryMode()
        {
        assertDynamicRemotePolicy("allow", true);
        assertDynamicRemotePolicy(" AlLoW ", true);
        }

    @Test
    public void explicitDenyInEveryMode()
        {
        assertDynamicRemotePolicy("deny", false);
        assertDynamicRemotePolicy(" DeNy ", false);
        }

    @Test
    public void invalidValueFallsBackToAllowInEveryMode()
        {
        assertDynamicRemotePolicy("maybe", true);
        }

    @Test
    public void resetForTesting()
        {
        setMode("prod", CoherenceMode.SECURITY_MODE_HARDENED, null);

        assertTrue(RemoteExecutionMode.isDynamicRemoteAllowed());

        restoreProperty(RemoteExecutionMode.PROP_DYNAMIC_REMOTE_UNAUTH, "deny");
        assertTrue(RemoteExecutionMode.isDynamicRemoteAllowed());

        RemoteExecutionMode.resetForTesting();
        assertFalse(RemoteExecutionMode.isDynamicRemoteAllowed());
        }

    private static void assertDynamicRemotePolicy(String sValue, boolean fAllowed)
        {
        for (String sMode : new String[] {null, "eval", "dev", "development", "prod", "production"})
            {
            for (String sSecurityMode : new String[] {null, CoherenceMode.SECURITY_MODE_COMPATIBILITY,
                    CoherenceMode.SECURITY_MODE_HARDENED})
                {
                setMode(sMode, sSecurityMode, sValue);
                assertEquals("runtime=" + sMode + ", security=" + sSecurityMode + ", dynamic=" + sValue,
                        fAllowed, RemoteExecutionMode.isDynamicRemoteAllowed());
                }
            }
        }

    private static void setMode(String sMode, String sSecurityMode, String sDynamicRemote)
        {
        restoreProperty(CoherenceMode.PROP_COHERENCE_MODE, sMode);
        restoreProperty(CoherenceMode.PROP_SECURITY_MODE, sSecurityMode);
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
    private final String m_sSecurityModeOld  = System.getProperty(CoherenceMode.PROP_SECURITY_MODE);
    private final String m_sDynamicRemoteOld = System.getProperty(RemoteExecutionMode.PROP_DYNAMIC_REMOTE_UNAUTH);
    }
