/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.config.xml.preprocessor;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.coherence.config.Config;

import com.tangosol.config.ConfigurationException;
import com.tangosol.config.xml.DocumentElementPreprocessor.ElementPreprocessor;
import com.tangosol.config.xml.ProcessingContext;

import com.tangosol.internal.util.CoherenceMode;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlValue;

import java.util.Locale;

/**
 * Resolves the Coherence Concurrent Extend proxy autostart default before the
 * generic system-property preprocessor removes the marker that distinguishes
 * the shipped default from an explicit XML override.
 *
 * @author Aleks Seovic  2026.05.13
 * @since 26.07
 */
public class ConcurrentProxyPreprocessor
        implements ElementPreprocessor
    {
    @Override
    public boolean preprocess(ProcessingContext context, XmlElement element)
            throws ConfigurationException
        {
        if (!"proxy-scheme".equals(element.getName()))
            {
            return false;
            }

        XmlElement xmlServiceName = element.getElement("service-name");
        if (xmlServiceName == null || !SERVICE_NAME.equals(xmlServiceName.getString()))
            {
            return false;
            }

        XmlElement xmlAutostart = element.getElement("autostart");
        if (xmlAutostart == null)
            {
            return false;
            }

        String   sPropertyValue = blankToNull(Config.getProperty(PROP_CONCURRENT_EXTEND_ENABLED));
        XmlValue attr           = xmlAutostart.getAttribute("system-property");
        String   sMarker        = attr == null ? null : attr.getString();
        boolean  fShippedMarker = PROP_CONCURRENT_EXTEND_ENABLED.equals(sMarker)
                && "true".equalsIgnoreCase(xmlAutostart.getString().trim());

        boolean fEnabled;
        String  sSource;

        if (sPropertyValue != null)
            {
            fEnabled = Boolean.parseBoolean(sPropertyValue);
            sSource  = "property";
            }
        else if (!fShippedMarker)
            {
            fEnabled = Boolean.parseBoolean(xmlAutostart.getString());
            sSource  = "xml";
            }
        else
            {
            fEnabled = !CoherenceMode.isSecurityHardeningEnabled();
            sSource  = "hardening-default";
            }

        String sXmlValue = xmlAutostart.getString();
        xmlAutostart.setAttribute("system-property", null);
        xmlAutostart.setString(Boolean.toString(fEnabled));

        CoherenceMode mode = CoherenceMode.current();
        boolean       fHardened = CoherenceMode.isSecurityHardeningEnabled();
        String        sSecurityMode = fHardened ? "hardened" : "compatibility";
        Logger.info(String.format(Locale.ROOT,
                "ConcurrentProxy autostart resolved: mode=%s, security-mode=%s, property=%s, xml=%s, source=%s, enabled=%s",
                mode.name().toLowerCase(Locale.ROOT), sSecurityMode,
                sPropertyValue, sXmlValue, sSource, fEnabled));

        if (!fEnabled && fHardened)
            {
            Logger.warn("ConcurrentProxy is disabled; set -D" + PROP_CONCURRENT_EXTEND_ENABLED
                    + "=true to enable Coherence Concurrent Extend access.");
            }

        return false;
        }

    private static String blankToNull(String sValue)
        {
        return sValue == null || sValue.isBlank() ? null : sValue;
        }

    // ----- constants -----------------------------------------------------

    /**
     * Coherence Concurrent Extend proxy service name.
     */
    public static final String SERVICE_NAME = "ConcurrentProxy";

    /**
     * Property that controls Coherence Concurrent Extend proxy autostart.
     */
    public static final String PROP_CONCURRENT_EXTEND_ENABLED = "coherence.concurrent.extend.enabled";

    /**
     * Singleton instance.
     */
    public static final ConcurrentProxyPreprocessor INSTANCE = new ConcurrentProxyPreprocessor();
    }
