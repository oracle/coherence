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

import static org.junit.Assert.assertEquals;

/**
 * Config-mediated property tests for {@link TopicsPersistedPolicyDrift}.
 *
 * @author Aleks Seovic  2026.05.11
 * @since 26.04
 */
public class TopicsPersistedPolicyDriftConfigTest
    {
    @Before
    public void capturePropertyDefaults()
        {
        m_sModeOld        = System.getProperty(CoherenceMode.PROP_COHERENCE_MODE);
        m_sPolicyDriftOld = System.getProperty(TopicsPersistedPolicyDrift.PROP_PERSISTED_POLICY_DRIFT);
        }

    @After
    public void cleanup()
        {
        restoreProperty(CoherenceMode.PROP_COHERENCE_MODE, m_sModeOld);
        restoreProperty(TopicsPersistedPolicyDrift.PROP_PERSISTED_POLICY_DRIFT, m_sPolicyDriftOld);
        resetMode();
        }

    @Test
    public void defaultsByMode()
        {
        setMode("prod", null);
        assertEquals(TopicsPersistedPolicyDrift.VALUE_REJECT, TopicsPersistedPolicyDrift.current());

        setMode("dev", null);
        assertEquals(TopicsPersistedPolicyDrift.VALUE_WARN_ALLOW, TopicsPersistedPolicyDrift.current());

        setMode("legacy", null);
        assertEquals(TopicsPersistedPolicyDrift.VALUE_WARN_ALLOW, TopicsPersistedPolicyDrift.current());
        }

    @Test
    public void explicitValuesOverrideModeDefaults()
        {
        setMode("prod", TopicsPersistedPolicyDrift.VALUE_WARN_ALLOW);
        assertEquals(TopicsPersistedPolicyDrift.VALUE_WARN_ALLOW, TopicsPersistedPolicyDrift.current());

        setMode("dev", TopicsPersistedPolicyDrift.VALUE_REJECT);
        assertEquals(TopicsPersistedPolicyDrift.VALUE_REJECT, TopicsPersistedPolicyDrift.current());
        }

    @Test
    public void blankValueTreatedAsUnset()
        {
        setMode("prod", " ");
        assertEquals(TopicsPersistedPolicyDrift.VALUE_REJECT, TopicsPersistedPolicyDrift.current());

        setMode("dev", " ");
        assertEquals(TopicsPersistedPolicyDrift.VALUE_WARN_ALLOW, TopicsPersistedPolicyDrift.current());
        }

    @Test
    public void invalidValueFallsBackToModeDefault()
        {
        setMode("prod", "maybe");
        assertEquals(TopicsPersistedPolicyDrift.VALUE_REJECT, TopicsPersistedPolicyDrift.current());

        setMode("dev", "maybe");
        assertEquals(TopicsPersistedPolicyDrift.VALUE_WARN_ALLOW, TopicsPersistedPolicyDrift.current());
        }

    private static void setMode(String sMode, String sPolicyDrift)
        {
        restoreProperty(CoherenceMode.PROP_COHERENCE_MODE, sMode);
        restoreProperty(TopicsPersistedPolicyDrift.PROP_PERSISTED_POLICY_DRIFT, sPolicyDrift);
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
        TopicsPersistedPolicyDrift.resetForTesting();
        }

    private String m_sModeOld;
    private String m_sPolicyDriftOld;
    }
