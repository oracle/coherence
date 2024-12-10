/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.extend.proxy;

import com.tangosol.run.xml.XmlElement;

/**
 * LegacyXmlProxyHelper parses XML to populate a DefaultProxyDependencies object.
 *
 * @author pfm 2011.07.25
 * @since Coherence 12.1.2
 */
@Deprecated
public class LegacyXmlProxyHelper
    {
    /**
     * Populate the DefaultProxyDependencies object from the given XML configuration.
     *
     * @param xml   the XML parent element that contains the Proxy elements
     * @param deps  the DefaultProxyDependencies to be populated
     *
     * @return the DefaultProxyDependencies object that was passed in
     */
    public static DefaultProxyDependencies fromXml(XmlElement xml, DefaultProxyDependencies deps)
        {
        if (xml == null)
            {
            throw new IllegalArgumentException("XML argument cannot be null");
            }

        deps.setEnabled(xml.getSafeElement("enabled").getBoolean(deps.isEnabled()));

        return deps;
        }
    }
