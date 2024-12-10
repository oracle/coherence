/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.peer.initiator;

import com.tangosol.coherence.config.Config;

import com.tangosol.internal.net.service.peer.LegacyXmlPeerHelper;

import com.tangosol.net.OperationalContext;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import com.tangosol.util.Base;

/**
 * LegacyXmlInitiatorDependencies parses XML to populate an DefaultInitiatorDependencies
 * object.
 *
 * @author pfm 2011.06.27
 * @since Coherence 12.1.2
 */
@Deprecated
public class LegacyXmlInitiatorHelper
    {
    /**
     * Populate the DefaultInitiatorDependencies object from the given XML configuration.
     *
     * @param xml     the XML parent element that contains the child service elements
     * @param deps    the DefaultJmsInitiatorDependencies to be populated
     * @param ctx     the OperationalContext
     * @param loader  the class loader for the current context
     *
     * @return the DefaultInitiatorDependencies object that was passed in
     */
    public static DefaultInitiatorDependencies fromXml(XmlElement xml,
            DefaultInitiatorDependencies deps, OperationalContext ctx, ClassLoader loader)
        {
        LegacyXmlPeerHelper.fromXml(xml, deps, ctx, loader);

        // <connect-timeout>
        deps.setConnectTimeoutMillis(XmlHelper.parseTime(
                xml, "connect-timeout", deps.getRequestTimeoutMillis()));

        String sRequestSendTimeout = Config.getProperty("coherence.net.send.timeout");
        if (sRequestSendTimeout != null)
            {
            deps.setRequestSendTimeoutMillis(Base.parseTime(sRequestSendTimeout));
            }
        else
            {
            XmlElement xmlCat = xml.getSafeElement("outgoing-message-handler");
            deps.setRequestSendTimeoutMillis(
                XmlHelper.parseTime(xmlCat, "request-timeout", deps.getRequestSendTimeoutMillis()));
            }
        return deps;
        }
    }
