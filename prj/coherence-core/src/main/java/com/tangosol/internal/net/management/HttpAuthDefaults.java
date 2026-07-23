/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.management;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.coherence.config.Config;

import com.tangosol.internal.util.CoherenceMode;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlValue;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * Resolves management and metrics HTTP authentication defaults from raw XML.
 *
 * @author jk  2026.05.14
 * @since 26.04
 */
public final class HttpAuthDefaults
    {
    /**
     * Resolve the management HTTP auth method.
     *
     * @param xml  the raw management HTTP XML
     */
    public static void resolveManagement(XmlElement xml)
        {
        resolve(xml, SERVICE_MANAGEMENT, PROP_MANAGEMENT_AUTH);
        }

    /**
     * Resolve the metrics HTTP auth method.
     *
     * @param xml  the raw metrics HTTP XML
     */
    public static void resolveMetrics(XmlElement xml)
        {
        resolve(xml, SERVICE_METRICS, PROP_METRICS_AUTH);
        }

    /**
     * Resolve an HTTP auth method from raw XML.
     *
     * @param xml        the raw proxy XML
     * @param sService   the service name
     * @param sProperty  the auth system property
     *
     * @return the resolution result, or {@code null} when no owned marker was present
     */
    static Resolution resolve(XmlElement xml, String sService, String sProperty)
        {
        return resolve(xml, sService, sProperty, CoherenceMode.current());
        }

    /**
     * Resolve an HTTP auth method from raw XML.
     *
     * @param xml        the raw proxy XML
     * @param sService   the service name
     * @param sProperty  the auth system property
     * @param mode       the current Coherence mode
     *
     * @return the resolution result, or {@code null} when no owned marker was present
     */
    static Resolution resolve(XmlElement xml, String sService, String sProperty, CoherenceMode mode)
        {
        XmlElement xmlAuth = getAuthElement(xml);
        if (xmlAuth == null)
            {
            return null;
            }

        XmlValue xmlProperty = xmlAuth.getAttribute(ATTR_SYSTEM_PROPERTY);
        String   sXmlProperty = xmlProperty == null ? null : xmlProperty.getString();
        boolean  fOwnMarker   = sProperty.equals(sXmlProperty);

        if (xmlProperty != null && !fOwnMarker)
            {
            return null;
            }

        String sValue  = Config.getProperty(sProperty);
        Source source;
        if (sValue != null && !sValue.trim().isEmpty())
            {
            source = Source.PROPERTY;
            }
        else if (!fOwnMarker)
            {
            sValue = xmlAuth.getString();
            source = Source.XML;
            }
        else
            {
            sValue = mode == CoherenceMode.LEGACY ? AUTH_NONE : AUTH_BASIC;
            source = Source.MODE_DEFAULT;
            }

        String sAuth = normalize(sValue);
        xmlAuth.setAttribute(ATTR_SYSTEM_PROPERTY, null);
        xmlAuth.setString(sAuth);

        Resolution resolution = new Resolution(sService, sProperty, source, mode, sAuth);
        logResolution(resolution);
        return resolution;
        }

    /**
     * Set loggers for tests.
     *
     * @param infoLogger  info logger
     * @param warnLogger  warn logger
     */
    static void setLoggersForTesting(Consumer<String> infoLogger, Consumer<String> warnLogger)
        {
        s_infoLogger = infoLogger == null ? Logger::info : infoLogger;
        s_warnLogger = warnLogger == null ? Logger::warn : warnLogger;
        }

    /**
     * Normalize an auth method.
     *
     * @param sValue  the auth method
     *
     * @return normalized auth method
     */
    private static String normalize(String sValue)
        {
        return sValue == null || sValue.trim().isEmpty()
               ? AUTH_NONE
               : sValue.trim().toLowerCase(Locale.ROOT);
        }

    /**
     * Log the resolution.
     *
     * @param resolution  the resolution result
     */
    private static void logResolution(Resolution resolution)
        {
        s_infoLogger.accept("Resolved HTTP auth default: service=" + resolution.getService()
                + ", property=" + resolution.getProperty()
                + ", source=" + resolution.getSource().getValue()
                + ", mode=" + resolution.getMode().name().toLowerCase(Locale.ROOT)
                + ", auth=" + resolution.getAuthMethod());

        if (AUTH_NONE.equals(resolution.getAuthMethod()))
            {
            s_warnLogger.accept("HTTP auth is disabled for service=" + resolution.getService()
                    + ", property=" + resolution.getProperty()
                    + "; this compatibility setting leaves the endpoint unauthenticated.");
            }
        }

    /**
     * Return the HTTP acceptor auth element.
     *
     * @param xml  the proxy XML
     *
     * @return the auth-method element, or {@code null} if absent
     */
    private static XmlElement getAuthElement(XmlElement xml)
        {
        if (xml == null)
            {
            return null;
            }
        XmlElement xmlAcceptorConfig = xml.getElement("acceptor-config");
        if (xmlAcceptorConfig == null)
            {
            return null;
            }
        XmlElement xmlHttpAcceptor = xmlAcceptorConfig.getElement("http-acceptor");
        return xmlHttpAcceptor == null ? null : xmlHttpAcceptor.getElement("auth-method");
        }

    // ----- inner class: Resolution ---------------------------------------

    /**
     * Resolution result.
     */
    static class Resolution
        {
        Resolution(String sService, String sProperty, Source source, CoherenceMode mode, String sAuthMethod)
            {
            f_sService    = sService;
            f_sProperty   = sProperty;
            f_source      = source;
            f_mode        = mode;
            f_sAuthMethod = sAuthMethod;
            }

        String getService()
            {
            return f_sService;
            }

        String getProperty()
            {
            return f_sProperty;
            }

        Source getSource()
            {
            return f_source;
            }

        CoherenceMode getMode()
            {
            return f_mode;
            }

        String getAuthMethod()
            {
            return f_sAuthMethod;
            }

        private final String        f_sService;
        private final String        f_sProperty;
        private final Source        f_source;
        private final CoherenceMode f_mode;
        private final String        f_sAuthMethod;
        }

    /**
     * Resolution source.
     */
    enum Source
        {
        PROPERTY("property"),
        XML("xml"),
        MODE_DEFAULT("mode-default");

        Source(String sValue)
            {
            f_sValue = sValue;
            }

        String getValue()
            {
            return f_sValue;
            }

        private final String f_sValue;
        }

    // ----- constants ------------------------------------------------------

    /**
     * Management HTTP auth property.
     */
    public static final String PROP_MANAGEMENT_AUTH = "coherence.management.http.auth";

    /**
     * Metrics HTTP auth property.
     */
    public static final String PROP_METRICS_AUTH = "coherence.metrics.http.auth";

    /**
     * Management HTTP proxy service name.
     */
    public static final String SERVICE_MANAGEMENT = "ManagementHttpProxy";

    /**
     * Metrics HTTP proxy service name.
     */
    public static final String SERVICE_METRICS = "MetricsHttpProxy";

    /**
     * HTTP Basic auth method.
     */
    public static final String AUTH_BASIC = "basic";

    /**
     * HTTP no-auth method.
     */
    public static final String AUTH_NONE = "none";

    /**
     * System property XML attribute.
     */
    private static final String ATTR_SYSTEM_PROPERTY = "system-property";

    /**
     * Info logger.
     */
    private static Consumer<String> s_infoLogger = Logger::info;

    /**
     * Warning logger.
     */
    private static Consumer<String> s_warnLogger = Logger::warn;
    }
