/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.logging;

import com.tangosol.run.xml.XmlElement;

import com.tangosol.util.Base;

/**
 * LegacyXmlLoggingHelper parses the {@code <logging-config>} XML to populate
 * the DefaultLoggingDependencies.
 *
 * NOTE: This code will eventually be replaced by CODI.
 *
 * @author der  2011.07.08
 * @since Coherence 12.1.2
 */
@SuppressWarnings("deprecation")
public class LegacyXmlLoggingHelper
    {
    /**
     * Populate the DefaultLoggingDependencies object from the XML configuration.
     *
     * @param xml   the <{@code <logging-config>} XML element
     * @param deps  the DefaultLoggingDependencies to be populated
     *
     * @return the DefaultLoggingDependencies object that was passed in
     */
    public static DefaultLoggingDependencies fromXml(XmlElement xml, DefaultLoggingDependencies deps)
        {
        Base.azzert(xml.getName().equals("logging-config"));

        deps.setCharacterLimit(xml.getSafeElement("character-limit").getInt(deps.getCharacterLimit()));
        deps.setDestination(xml.getSafeElement("destination").getString(deps.getDestination()));
        deps.setLoggerName(xml.getSafeElement("logger-name").getString(deps.getLoggerName()));
        deps.setMessageFormat(xml.getSafeElement("message-format").getString(deps.getMessageFormat()));
        deps.setSeverityLevel(xml.getSafeElement("severity-level").getInt(deps.getSeverityLevel()));

        return deps;
        }
    }
