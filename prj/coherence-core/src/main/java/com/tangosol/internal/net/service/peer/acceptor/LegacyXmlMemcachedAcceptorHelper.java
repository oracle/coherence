/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.peer.acceptor;

import com.tangosol.coherence.config.builder.FactoryBasedAddressProviderBuilder;
import com.tangosol.coherence.config.builder.SocketProviderBuilder;
import com.tangosol.coherence.memcached.server.MemcachedServer;

import com.tangosol.internal.net.LegacyXmlConfigHelper;

import com.tangosol.net.AddressProviderFactory;
import com.tangosol.net.OperationalContext;

import com.tangosol.run.xml.XmlElement;

/**
 * LegacyXmlMemcachedAcceptorHelper parses XML to populate a DefaultMemcachedAcceptorDependencies
 * object.
 *
 * @author bb 2013.05.01
 * 
 * @since Coherence 12.1.3 
 */
@Deprecated
public class LegacyXmlMemcachedAcceptorHelper
    {
    /**
     * Populate the DefaultMemcachedAcceptorDependencies object from the given XML configuration.
     *
     * @param xml     the XML parent element that contains the <memcached-acceptor> XML fragment
     * @param deps    the DefaultMemcachedAcceptorDependencies to be populated
     * @param ctx     the OperationalContext
     * @param loader  the class loader for the current context
     *
     * @return the DefaultMemcachedAcceptorDependencies object that was passed in
     */
    public static DefaultMemcachedAcceptorDependencies fromXml(XmlElement xml,
        DefaultMemcachedAcceptorDependencies deps, OperationalContext ctx, ClassLoader loader)
        {
        LegacyXmlAcceptorHelper.fromXml(xml, deps, ctx, loader);

        // set the memcached server
        deps.setMemcachedServer(new MemcachedServer());

        // <memcached-acceptor>
        XmlElement xmlAcceptor = xml.getSafeElement("memcached-acceptor");

        // <cache-name>
        String sCacheName = xmlAcceptor.getSafeElement("cache-name").getString("");
        if (sCacheName.length() == 0)
            {
            throw new RuntimeException("Cache name cannot be null for memcached acceptor");
            }
        deps.setCacheName(sCacheName);

        // binary-pass-thru
        deps.setBinaryPassThru(xmlAcceptor.getSafeElement("interop-enabled").getBoolean());

        // <memcached-auth-method>
        deps.setAuthMethod(xmlAcceptor.getSafeElement("memcached-auth-method").getString("none"));

        // <socket-provider/>
        deps.setSocketProviderBuilder(ctx.getSocketProviderFactory().getSocketProviderBuilder(
            xmlAcceptor.getSafeElement("socket-provider")));

        // <parse address-provider>
        AddressProviderFactory factory = LegacyXmlConfigHelper.parseAddressProvider(
            xmlAcceptor, ctx.getAddressProviderMap());
        deps.setAddressProviderBuilder(
                new FactoryBasedAddressProviderBuilder(factory));

        return deps;
        }
    }