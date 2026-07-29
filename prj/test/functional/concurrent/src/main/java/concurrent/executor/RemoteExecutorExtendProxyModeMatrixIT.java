/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package concurrent.executor;

import com.oracle.coherence.testing.util.CoherenceModeHelper;

import com.tangosol.coherence.config.xml.preprocessor.ConcurrentProxyPreprocessor;
import com.tangosol.internal.util.CoherenceMode;

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
        m_sModeOld         = System.getProperty(CoherenceMode.PROP_COHERENCE_MODE);
        m_sSecurityModeOld = System.getProperty(CoherenceMode.PROP_SECURITY_MODE);
        m_sEnabledOld      = System.getProperty(ConcurrentProxyPreprocessor.PROP_CONCURRENT_EXTEND_ENABLED);
        }

    @AfterEach
    public void tearDown()
        {
        restore(CoherenceMode.PROP_COHERENCE_MODE, m_sModeOld);
        restore(CoherenceMode.PROP_SECURITY_MODE, m_sSecurityModeOld);
        restore(ConcurrentProxyPreprocessor.PROP_CONCURRENT_EXTEND_ENABLED, m_sEnabledOld);
        CoherenceModeHelper.reset();
        }

    @Test
    public void shouldApplyModeDefaultAndOverrides()
        {
        assertResolved("prod", CoherenceMode.SECURITY_MODE_HARDENED, null, shipped(), false);
        assertResolved("dev", CoherenceMode.SECURITY_MODE_COMPATIBILITY, null, shipped(), true);
        assertResolved("prod", CoherenceMode.SECURITY_MODE_COMPATIBILITY, null, shipped(), true);
        assertResolved("prod", CoherenceMode.SECURITY_MODE_HARDENED, "true", shipped(), true);
        assertResolved("dev", CoherenceMode.SECURITY_MODE_COMPATIBILITY, "false", shipped(), false);
        assertResolved("prod", CoherenceMode.SECURITY_MODE_HARDENED, null, explicit("true"), true);
        }

    private static void assertResolved(String sMode, String sSecurityMode, String sProperty, XmlElement xml,
                                       boolean fExpected)
        {
        restore(CoherenceMode.PROP_COHERENCE_MODE, sMode);
        restore(CoherenceMode.PROP_SECURITY_MODE, sSecurityMode);
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

    private String m_sSecurityModeOld;

    private String m_sEnabledOld;
    }
