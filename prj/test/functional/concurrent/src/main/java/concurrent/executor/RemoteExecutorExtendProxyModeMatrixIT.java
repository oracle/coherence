/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package concurrent.executor;

import com.oracle.coherence.testing.util.CoherenceModeHelper;

import com.tangosol.coherence.config.xml.preprocessor.ConcurrentProxyPreprocessor;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Functional mode matrix for the ConcurrentProxy Extend switch.
 */
public class RemoteExecutorExtendProxyModeMatrixIT
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
    public void shouldApplyModeDefaultAndOverrides()
        {
        assertResolved("prod", null, shipped(), false);
        assertResolved("dev", null, shipped(), true);
        assertResolved("legacy", null, shipped(), true);
        assertResolved("prod", "true", shipped(), true);
        assertResolved("dev", "false", shipped(), false);
        assertResolved("prod", null, explicit("true"), true);
        }

    private static void assertResolved(String sMode, String sProperty, XmlElement xml, boolean fExpected)
        {
        restore("coherence.mode", sMode);
        restore(ConcurrentProxyPreprocessor.PROP_CONCURRENT_EXTEND_ENABLED, sProperty);
        CoherenceModeHelper.reset();

        ConcurrentProxyPreprocessor.INSTANCE.preprocess(null, xml);

        XmlElement xmlAutostart = xml.getElement("autostart");
        assertThat(xmlAutostart.getString(), is(Boolean.toString(fExpected)));
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
