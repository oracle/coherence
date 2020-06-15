/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.extend.proxy;

import com.tangosol.run.xml.XmlElement;

import com.tangosol.util.Base;

/**
 * LegacyXmlCacheServiceProxyHelper parses XML to populate a
 * DefaultCacheServiceProxyDependencies object.
 *
 * @author pfm 2011.07.25
 * @since Coherence 12.1.2
 */
@Deprecated
public class LegacyXmlCacheServiceProxyHelper
    {
    /**
     * Populate the DefaultCacheServiceProxyDependencies object from the given XML
     * configuration.
     *
     * @param xml   the XML parent element that contains the Proxy elements
     * @param deps  the DefaultCacheServiceProxyDependencies to be populated
     *
     * @return the DefaultCacheServiceProxyDependencies object that was passed in
     */
    public static DefaultCacheServiceProxyDependencies fromXml(XmlElement xml,
            DefaultCacheServiceProxyDependencies deps)
        {
        LegacyXmlServiceProxyHelper.fromXml(xml, deps);

        deps.setLockEnabled(xml.getSafeElement("lock-enabled").getBoolean(
                deps.isLockEnabled()));
        deps.setReadOnly(xml.getSafeElement("read-only").getBoolean(deps.isReadOnly()));

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
