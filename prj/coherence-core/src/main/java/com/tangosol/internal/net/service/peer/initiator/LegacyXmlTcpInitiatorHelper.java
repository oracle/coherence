/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.peer.initiator;

import com.tangosol.coherence.config.builder.AddressProviderBuilder;
import com.tangosol.coherence.config.builder.FactoryBasedAddressProviderBuilder;
import com.tangosol.coherence.config.builder.SocketProviderBuilder;
import com.tangosol.internal.net.LegacyXmlConfigHelper;

import com.tangosol.net.AddressProviderFactory;
import com.tangosol.net.OperationalContext;

import com.tangosol.net.SocketProviderFactory;
import com.tangosol.run.xml.XmlElement;

/**
 * LegacyXmlTcpInitiatorHelper parses XML to populate a DefaultTcpInitiatorDependencies
 * object.
 *
 * @author pfm 2011.05.08
 * @since Coherence 12.1.2
 */
@Deprecated
public class LegacyXmlTcpInitiatorHelper

    {
    /**
     * Populate the DefaultTcpInitiatorDependencies object from the given XML configuration.
     *
     * @param xml     the XML parent element that contains the <tcp-initiator> XML fragment
     * @param deps    the DefaultTcpInitiatorDependencies to be populated
     * @param ctx     the OperationalContext
     * @param loader  the class loader for the current context
     *
     * @return the DefaultTcpInitiatorDependencies object that was passed in
     */
    @SuppressWarnings({ })
    public static DefaultTcpInitiatorDependencies fromXml(XmlElement xml,
            DefaultTcpInitiatorDependencies deps, OperationalContext ctx, ClassLoader loader)
        {
        LegacyXmlInitiatorHelper.fromXml(xml, deps, ctx, loader);

        // <tcp-initiator>
        XmlElement xmlCat = xml.getSafeElement("tcp-initiator");

        // <socket-provider/>
        deps.setSocketProviderBuilder(ctx.getSocketProviderFactory()
                .getSocketProviderBuilder(xmlCat.getElement("socket-provider")));

        // <local-address>
        XmlElement xmlSub = xmlCat.getSafeElement("local-address");
        deps.setLocalAddress(LegacyXmlConfigHelper.parseLocalSocketAddress(xmlSub));

        // <remote-addresses>
        boolean fNameService = false;
        xmlSub = xmlCat.getElement("name-service-addresses");
        if (xmlSub == null)
            {
            xmlSub = xmlCat.getSafeElement("remote-addresses");
            }
        else
            {
            fNameService = true;
            }

        AddressProviderFactory factory = LegacyXmlConfigHelper.parseAddressProvider(
                xmlSub, ctx.getAddressProviderMap());

        AddressProviderBuilder bldr = new FactoryBasedAddressProviderBuilder(factory);

        if (fNameService)
            {
            deps.setNameServiceAddressProviderBuilder(bldr);
            }
        else
            {
            deps.setRemoteAddressProviderBuilder(bldr);
            }

        // handle embedded socket options
        deps.getSocketOptions().setConfig(xmlCat);

        return deps;
        }
    }
