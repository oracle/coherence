/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.peer.initiator;

import com.tangosol.internal.net.service.peer.LegacyXmlCommonJmsHelper;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.OperationalContext;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

/**
 * LegacyXmlJmsInitiatorDependencies parses XML to populate a DefaultJmsInitiatorDependencies
 * object.
 *
 * @author pfm 2011.06.27
 * @since Coherence 12.1.2
 */
@Deprecated
public class LegacyXmlJmsInitiatorHelper
    {
    /**
     * Populate the DefaultJmsInitiatorDependencies object from the given XML configuration.
     *
     * @param xml   the XML parent element that contains the child service elements
     * @param deps  the DefaultJmsInitiatorDependencies to be populated
     * @param ctx   the OperationalContext
     * @param loader  the class loader for the current context
     *
     * @return the DefaultJmsInitiatorDependencies object that was passed in
     */
    public static DefaultJmsInitiatorDependencies fromXml(XmlElement xml,
            DefaultJmsInitiatorDependencies deps, OperationalContext ctx, ClassLoader loader)
        {
        LegacyXmlInitiatorHelper.fromXml(xml, deps, ctx, loader);

        // <jms-initiator>
        XmlElement xmlCat = xml.getSafeElement("jms-initiator");

        // Load the common properties
        LegacyXmlCommonJmsHelper.fromXml(xmlCat, deps.getCommonDependencies());

        return deps;
        }
    }
