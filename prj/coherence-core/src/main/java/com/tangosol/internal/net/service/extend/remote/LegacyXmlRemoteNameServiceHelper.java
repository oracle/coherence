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
 * LegacyXmlNameServiceHelper parses XML to populate a
 * DefaultNameServiceDependencies object.
 *
 * @author welin  2013.05.17
 *
 * @since Coherence 12.1.3
 */
public class LegacyXmlRemoteNameServiceHelper
    {
    /**
     * Populate the DefaultRemoteNameServiceDependencies object from the given XML
     * configuration.
     *
     * @param xml     the XML parent element that contains the NameService
     *                elements
     * @param deps    the DefaultRemoteNameServiceDependencies to be populated
     * @param ctx     the OperationalContext
     * @param loader  the class loader for the current context
     *
     * @return the DefaultRemoteNameServiceDependencies object that was passed in
     */
    public static DefaultRemoteNameServiceDependencies fromXml(
            XmlElement xml,
            DefaultRemoteNameServiceDependencies deps,
            OperationalContext ctx,
            ClassLoader loader)
        {
        // Create a dummy tcp-initiator element if necessary. This will be
        // replaced with real address in
        // RemoteService::lookupProxyServiceAddress()
        XmlElement xmlInitiator;
        if (xml.getName().equals("initiator-config"))
            {
            xmlInitiator = xml;
            }
        else
            {
            xmlInitiator = xml.getSafeElement("initiator-config");
            }

        LegacyXmlRemoteServiceHelper.fromXml(xml, deps, ctx, loader);

        return deps;
        }
    }
