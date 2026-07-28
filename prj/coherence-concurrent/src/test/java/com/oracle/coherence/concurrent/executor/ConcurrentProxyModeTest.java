/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.executor;

import com.oracle.coherence.testing.util.CoherenceModeHelper;

import com.tangosol.coherence.config.xml.preprocessor.ConcurrentProxyPreprocessor;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests for mode-aware ConcurrentProxy autostart defaulting.
 */
public class ConcurrentProxyModeTest
    {
    @BeforeEach
    public void setUp()
        {
        m_sModeOld    = System.getProperty("coherence.mode");
        m_sEnabledOld = System.getProperty(ConcurrentProxyPreprocessor.PROP_CONCURRENT_EXTEND_ENABLED);
        }

    @AfterEach
    public void tearDown()
        {
        restore("coherence.mode", m_sModeOld);
        restore(ConcurrentProxyPreprocessor.PROP_CONCURRENT_EXTEND_ENABLED, m_sEnabledOld);
        CoherenceModeHelper.reset();
        }

    @Test
    public void shouldDisableShippedDefaultInProd()
        {
        assertResolved("prod", null, shipped(), false);
        }

    @Test
    public void shouldEnableShippedDefaultInDevAndLegacy()
        {
        assertResolved("dev", null, shipped(), true);
        assertResolved("legacy", null, shipped(), true);
        }

    @Test
    public void shouldLetPropertyOverrideModeAndXml()
        {
        assertResolved("prod", "true", shipped(), true);
        assertResolved("dev", "false", shipped(), false);
        assertResolved("legacy", "false", shipped(), false);
        }

    @Test
    public void shouldLetExplicitXmlWithoutMarkerOverrideMode()
        {
        assertResolved("prod", null, explicit("true"), true);
        assertResolved("dev", null, explicit("false"), false);
        }

    private static void assertResolved(String sMode, String sProperty, XmlElement xml, boolean fExpected)
        {
        restore("coherence.mode", sMode);
        restore(ConcurrentProxyPreprocessor.PROP_CONCURRENT_EXTEND_ENABLED, sProperty);
        CoherenceModeHelper.reset();

        ConcurrentProxyPreprocessor.INSTANCE.preprocess(null, xml);

        XmlElement xmlAutostart = xml.getElement("autostart");
        assertEquals(Boolean.toString(fExpected), xmlAutostart.getString());
        assertNull(xmlAutostart.getAttribute("system-property"));
        }

    private static XmlElement shipped()
        {
        return XmlHelper.loadXml("<proxy-scheme>"
                + "<service-name>ConcurrentProxy</service-name>"
                + "<autostart system-property=\"coherence.concurrent.extend.enabled\">true</autostart>"
                + "</proxy-scheme>").getRoot();
        }

    private static XmlElement explicit(String sValue)
        {
        return XmlHelper.loadXml("<proxy-scheme>"
                + "<service-name>ConcurrentProxy</service-name>"
                + "<autostart>" + sValue + "</autostart>"
                + "</proxy-scheme>").getRoot();
        }

    private static void restore(String sName, String sValue)
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

    private String m_sModeOld;

    private String m_sEnabledOld;
    }
