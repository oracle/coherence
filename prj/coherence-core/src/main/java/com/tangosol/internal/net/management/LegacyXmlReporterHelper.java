/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.management;

import com.tangosol.coherence.config.Config;

import com.tangosol.coherence.reporter.ReportBatch;
import com.tangosol.run.xml.XmlElement;

import com.tangosol.util.Base;

/**
 * LegacyXmlReporterHelper parses the {@code <reporter>} XML
 * from operational configuration to populate the DefaultReporterDependencies.
 *
 * NOTE: This code will eventually be replaced by CODI.
 *
 * @author der 2011.07.08
 * @since Coherence 12.1.2
 */
@SuppressWarnings("deprecation")
public class LegacyXmlReporterHelper
    {
    /**
     * Populate the DefaultReporterDependencies object from the XML configuration.
     *
     * @param xml   the {@code <reporter>} XML element
     * @param deps  the DefaultReporterDependencies to be populated
     *
     * @return the DefaultReporterDependencies object that was passed in.
     */
    public static ReportBatch.DefaultDependencies fromXml(XmlElement xml, ReportBatch.DefaultDependencies  deps)
        {
        Base.azzert(xml.getName().equals("reporter"));

        // system property logic from the old ReportBatch configure() module. Will be handled by codi
        String sFile = Config.getProperty("coherence.management.report.configuration");
        deps.setConfigFile((sFile != null && sFile.length() != 0) ? sFile : xml.getSafeElement("configuration")
                .getString(deps.getConfigFile()));

        String sStart = Config.getProperty("coherence.management.report.autostart");
        deps.setAutoStart((sStart != null && sStart.length() != 0) ? Boolean.valueOf(sStart).booleanValue() : xml
                .getSafeElement("autostart").getBoolean(deps.isAutoStart()));

        String sCentral = Config.getProperty("coherence.management.report.distributed");
        deps.setDistributed((sCentral != null && sCentral.length() != 0) ? !Boolean.valueOf(sCentral).booleanValue()
                : xml.getSafeElement("distributed").getBoolean(deps.isDistributed()));

        deps.setTimeZone(xml.getSafeElement("timezone").getString(deps.getTimeZone()));
        deps.setDateFormat(xml.getSafeElement("timeformat").getString(deps.getDateFormat()));

        return deps;
        }
    }
