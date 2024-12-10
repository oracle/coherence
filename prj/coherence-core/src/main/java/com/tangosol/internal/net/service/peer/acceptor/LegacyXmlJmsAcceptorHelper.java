/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.peer.acceptor;

import com.tangosol.internal.net.service.peer.LegacyXmlCommonJmsHelper;

import com.tangosol.net.OperationalContext;

import com.tangosol.run.xml.XmlElement;

/**
 * LegacyXmlJmsAcceptorHelper parses XML to populate a DefaultJmsAcceptorDependencies
 * object.
 *
 * @author pfm 2011.06.27
 * @since Coherence 12.1.2
 */
@Deprecated
public class LegacyXmlJmsAcceptorHelper
    {
    /**
     * Populate the DefaultJmsAcceptorDependencies object from the given XML configuration.
     *
     * @param xml     the XML parent element that contains the JMS elements
     * @param deps    the DefaultJmsAcceptorDependencies to be populated
     * @param ctx     the OperationalContext
     * @param loader  the class loader for the current context
     *
     * @return the DefaultJmsAcceptorDependencies object that was passed in
     */
    public static DefaultJmsAcceptorDependencies fromXml(XmlElement xml,
            DefaultJmsAcceptorDependencies deps, OperationalContext ctx, ClassLoader loader)
        {
        LegacyXmlAcceptorHelper.fromXml(xml, deps, ctx, loader);

        // <jms-acceptor>
        XmlElement xmlCat = xml.getSafeElement("jms-acceptor");

        // Load the common properties
        LegacyXmlCommonJmsHelper.fromXml(xmlCat, deps.getCommonDependencies());

        return deps;
        }
    }
