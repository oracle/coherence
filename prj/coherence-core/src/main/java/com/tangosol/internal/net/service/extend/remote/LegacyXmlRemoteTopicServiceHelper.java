/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.extend.remote;

import com.tangosol.net.OperationalContext;
import com.tangosol.run.xml.XmlElement;

/**
 * LegacyXmlRemoteTopicServiceHelper parses XML to populate a
 * DefaultRemoteTopicServiceDependencies object.
 *
 * @author Jonathan Knight  2025.01.01
 */
@Deprecated
public class LegacyXmlRemoteTopicServiceHelper
    {
    /**
     * Populate the DefaultRemoteTopicServiceDependencies object from the given XML
     * configuration.
     *
     * @param xml   the XML parent element that contains the RemoteTopicService elements
     * @param deps  the DefaultRemoteTopicServiceDependencies to be populated
     * @param ctx   the OperationalContext
     * @param loader  the class loader for the current context
     *
     * @return the DefaultRemoteTopicServiceDependencies object that was passed in
     */
    public static DefaultRemoteTopicServiceDependencies fromXml(XmlElement xml,
            DefaultRemoteTopicServiceDependencies deps, OperationalContext ctx,
            ClassLoader loader)
        {
        LegacyXmlRemoteServiceHelper.fromXml(xml, deps, ctx, loader);
        return deps;
        }
    }
