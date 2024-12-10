/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.extend.proxy;

import com.tangosol.run.xml.XmlElement;

/**
 * LegacyXmlServiceProxyHelper parses XML to populate a DefaultServiceProxyDependencies
 * object.
 *
 * @author pfm 2011.07.25
 * @since Coherence 12.1.2
 */
@Deprecated
public class LegacyXmlServiceProxyHelper
    {
    /**
     * Populate the DefaultServiceProxyDependencies object from the given XML configuration.
     *
     * @param xml   the XML parent element that contains the Proxy elements
     * @param deps  the DefaultServiceProxyDependencies to be populated
     *
     * @return the DefaultServiceProxyDependencies object that was passed in
     */
    public static DefaultServiceProxyDependencies fromXml(XmlElement xml,
            DefaultServiceProxyDependencies deps)
        {
        LegacyXmlProxyHelper.fromXml(xml, deps);

        if (xml.getElement("class-name") != null)
            {
            deps.setServiceClassConfig(xml);
            }

        return deps;
        }
    }
