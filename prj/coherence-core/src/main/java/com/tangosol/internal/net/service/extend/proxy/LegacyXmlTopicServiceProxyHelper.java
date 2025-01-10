/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.extend.proxy;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.util.Base;

/**
 * LegacyXmlTopicServiceProxyHelper parses XML to populate a
 * DefaultTopicServiceProxyDependencies object.
 *
 * @author Jonathan Knight  2025.01.01
 */
@Deprecated
public class LegacyXmlTopicServiceProxyHelper
    {
    /**
     * Populate the DefaultTopicServiceProxyDependencies object from the given XML
     * configuration.
     *
     * @param xml   the XML parent element that contains the Proxy elements
     * @param deps  the DefaultTopicServiceProxyDependencies to be populated
     *
     * @return the DefaultTopicServiceProxyDependencies object that was passed in
     */
    public static DefaultTopicServiceProxyDependencies fromXml(XmlElement xml,
            DefaultTopicServiceProxyDependencies deps)
        {
        LegacyXmlServiceProxyHelper.fromXml(xml, deps);

        String sBytes = xml.getSafeElement("transfer-threshold").getString();
        if (sBytes != null && sBytes.length() > 0)
            {
            try
                {
                deps.setTransferThreshold(Base.parseMemorySize(sBytes));
                }
            catch (RuntimeException e)
                {
                throw Base.ensureRuntimeException(e,
                        "illegal \"transfer-threshold\" value: " + sBytes);
                }
            }
        return deps;
        }
    }
