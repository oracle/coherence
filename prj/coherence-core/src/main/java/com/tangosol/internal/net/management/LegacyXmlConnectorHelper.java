/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.management;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import com.tangosol.util.Base;

/**
 * LegacyXmlConnectorHelper parses the connection portion of the
 * <management-config> XML to populate the DefaultConnectorDependencies.
 *
 * NOTE: This code will eventually be replaced by CODI.
 *
 * @author der 2011.07.08
 * @since Coherence 12.1.2
 */
@SuppressWarnings("deprecation")
public class LegacyXmlConnectorHelper
    {
    /**
     * Populate the DefaultConnectorDependencies object from the given XML configuration.
     *
     * @param xml   the {@code <management-config>} element
     * @param deps  the DefaultConnectorDependencies to be populated
     *
     * @return the DefaultConnectorDependencies object that was passed in.
     */
    public static DefaultConnectorDependencies fromXml(XmlElement xml, DefaultConnectorDependencies deps)
        {
        Base.azzert(xml.getName().equals("management-config"));

        deps.setRefreshTimeoutMillis(XmlHelper.parseTime(xml,"refresh-expiry",
                deps.getRefreshTimeoutMillis()));
        deps.setRefreshPolicy(xml.getSafeElement("refresh-policy").getString(
                deps.formatRefreshPolicy(deps.getRefreshPolicy())));
        deps.setRefreshRequestTimeoutMillis(XmlHelper.parseTime(xml,"refresh-timeout",
                deps.getRefreshRequestTimeoutMillis()));

        return deps;
        }
    }
