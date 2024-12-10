/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.extend.remote;

import com.tangosol.net.OperationalContext;

import com.tangosol.run.xml.XmlElement;

/**
 * LegacyXmlRemoteInvocationServiceHelper parses XML to populate a
 * DefaultRemoteInvocationServiceDependencies object.
 *
 * @author pfm 2011.09.05
 * @since Coherence 12.1.2
 */
@Deprecated
public class LegacyXmlRemoteInvocationServiceHelper
    {
    /**
     * Populate the DefaultRemoteInvocationServiceDependencies object from the given XML
     * configuration.
     *
     * @param xml     the XML parent element that contains the RemoteInvocationService elements
     * @param deps    the DefaultRemoteInvocationServiceDependencies to be populated
     * @param ctx     the OperationalContext
     * @param loader  the class loader for the current context
     *
     * @return the DefaultRemoteInvocationServiceDependencies object that was passed in
     */
    public static DefaultRemoteInvocationServiceDependencies fromXml(XmlElement xml,
            DefaultRemoteInvocationServiceDependencies deps, OperationalContext ctx,
            ClassLoader loader)
        {
        LegacyXmlRemoteServiceHelper.fromXml(xml, deps, ctx, loader);

        return deps;
        }
    }
