/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.extend;

import com.tangosol.internal.net.service.peer.acceptor.DefaultNSTcpAcceptorDependencies;
import com.tangosol.internal.net.service.peer.acceptor.LegacyXmlAcceptorHelper;

import com.tangosol.net.OperationalContext;

import com.tangosol.run.xml.XmlElement;

/**
 * LegacyXmlNameServiceHelper parses XML to populate a
 * DefaultNameServiceDependencies object.
 *
 * @author phf 2012.02.01
 *
 * @since Coherence 12.1.2
 */
public class LegacyXmlNameServiceHelper
    {
    /**
     * Populate the DefaultNameServiceDependencies object from the given XML
     * configuration.
     *
     * @param xml     the XML parent element that contains the NameService
     *                elements
     * @param deps    the DefaultNameServiceDependencies to be populated
     * @param ctx     the OperationalContext
     * @param loader  the class loader for the current context
     *
     * @return the DefaultNameServiceDependencies object that was passed in
     */
    public static DefaultNameServiceDependencies fromXml(
            XmlElement xml,
            DefaultNameServiceDependencies deps,
            OperationalContext ctx,
            ClassLoader loader)
        {
        deps.setAcceptorDependencies(LegacyXmlAcceptorHelper.createAcceptorDeps(xml, ctx, loader,
                new DefaultNSTcpAcceptorDependencies()));

        return deps;
        }
    }
