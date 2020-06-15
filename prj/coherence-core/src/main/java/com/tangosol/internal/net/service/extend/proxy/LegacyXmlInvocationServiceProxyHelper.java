/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.extend.proxy;

import com.tangosol.run.xml.XmlElement;

/**
 * LegacyXmlInvocationServiceProxyHelper parses XML to populate a
 * DefaultInvocationServiceProxyDependencies object.
 *
 * @author pfm 2011.07.25
 * @since Coherence 12.1.2
 */
@Deprecated
public class LegacyXmlInvocationServiceProxyHelper
    {
    /**
     * Populate the DefaultInvocationServiceProxyDependencies object from the given XML
     * configuration.
     *
     * @param xml   the XML parent element that contains the Proxy elements
     * @param deps  the DefaultInvocationServiceProxyDependencies to be populated
     *
     * @return the DefaultInvocationServiceProxyDependencies object that was passed in
     */
    public static DefaultInvocationServiceProxyDependencies fromXml(XmlElement xml,
            DefaultInvocationServiceProxyDependencies deps)
        {
        LegacyXmlServiceProxyHelper.fromXml(xml, deps);

        return deps;
        }
    }
