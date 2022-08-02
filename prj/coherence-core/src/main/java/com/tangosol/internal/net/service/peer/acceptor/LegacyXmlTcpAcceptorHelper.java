/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.peer.acceptor;

import com.tangosol.coherence.config.builder.AddressProviderBuilder;
import com.tangosol.coherence.config.builder.FactoryBasedAddressProviderBuilder;
import com.tangosol.coherence.config.builder.LocalAddressProviderBuilder;
import com.tangosol.coherence.config.builder.SocketProviderBuilder;
import com.tangosol.internal.net.LegacyXmlConfigHelper;

import com.tangosol.internal.net.service.peer.acceptor.TcpAcceptorDependencies.BufferPoolConfig;

import com.tangosol.net.AddressProviderFactory;
import com.tangosol.net.OperationalContext;
import com.tangosol.net.SocketAddressProvider;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import com.tangosol.util.Base;

/**
 * LegacyXmlTcpAcceptorHelper parses XML to populate a DefaultTcpAcceptorDependencies
 * object.
 *
 * @author pfm 2011.06.27
 * @since Coherence 12.1.2
 */
@Deprecated
public class LegacyXmlTcpAcceptorHelper
    {
    /**
     * Populate a DefaultTcpAcceptorDependencies object from the given XML configuration.
     *
     * @param xml     the XML parent element that contains the <tcp-acceptor> XML fragment
     * @param deps    the DefaultTcpAcceptorDependencies to be populated
     * @param ctx     the OperationalContext
     * @param loader  the class loader for the current context
     *
     * @return the DefaultTcpAcceptorDependencies object that was passed in
     */
    @Deprecated
    public static DefaultTcpAcceptorDependencies fromXml(XmlElement xml,
            DefaultTcpAcceptorDependencies deps, OperationalContext ctx, ClassLoader loader)
        {
        LegacyXmlAcceptorHelper.fromXml(xml, deps, ctx, loader);

        // <tcp-acceptor>
        XmlElement xmlCat = xml.getSafeElement("tcp-acceptor");

        // <socket-provider/>
        deps.setSocketProviderBuilder(ctx.getSocketProviderFactory()
                .getSocketProviderBuilder(xmlCat.getElement("socket-provider"), deps.canUseGlobalSocketProvider()));

        // <local-address> | <address-provider> (mutually exclusive)
        SocketAddressProvider addressProvider = null;
        XmlElement            xmlLocal        = xmlCat.getSafeElement("local-address");
        XmlElement            xmlProvider     = xmlCat.getSafeElement("address-provider");
        boolean               fLocal          = !XmlHelper.isEmpty(xmlLocal);
        boolean               fProvider       = !XmlHelper.isEmpty(xmlProvider);

        if (fLocal && fProvider)
            {
            throw new IllegalArgumentException("\"address-provider\" and "
                    + "\"local-address\" elements are mutually exclusive");
            }

        AddressProviderBuilder bldr = null;

        if (fLocal)
            {
            // <address>
            String sAddr = xmlLocal.getSafeElement("address").getString().trim();
            if (!sAddr.isEmpty())
                {
                int nPort = xmlLocal.getSafeElement("port").getInt();
                if (nPort < 0 || nPort > 65535)
                    {
                    throw new IllegalArgumentException("invalid port in local-address:\n"
                            + xmlLocal);
                    }

                XmlElement xmlPortAdjust = xmlLocal.getSafeElement("port-auto-adjust");
                String     sPortAdjust   = xmlPortAdjust.getString("true").trim();
                int        nPortAdjust   = sPortAdjust.compareToIgnoreCase("true")  == 0 ? 65535
                                         : sPortAdjust.compareToIgnoreCase("false") == 0 ? nPort
                                         : Integer.parseInt(sPortAdjust);

                if (nPortAdjust < nPort || nPortAdjust > 65535)
                    {
                    throw new IllegalArgumentException("invalid port-auto-adjust in local-address:\n"
                            + xmlLocal);
                    }

                bldr = new LocalAddressProviderBuilder(sAddr, nPort, nPortAdjust);
                }
            // else; share address:port with TCMP
            }
        else if (fProvider)
            {
            AddressProviderFactory factory = LegacyXmlConfigHelper.parseAddressProvider(
                    xmlCat, ctx.getAddressProviderMap());

            bldr = new FactoryBasedAddressProviderBuilder(factory);
            }

        deps.setLocalAddressProviderBuilder(bldr);

        // <listen-backlog>
        deps.setListenBacklog(xmlCat.getSafeElement("listen-backlog").getInt(
                deps.getListenBacklog()));

        // handle embedded socket options
        deps.getSocketOptions().setConfig(xmlCat);

        // <incoming-buffer-pool>
        XmlElement xmlVal = xmlCat.getSafeElement("incoming-buffer-pool");
        deps.setIncomingBufferPoolConfig(populateBufferPoolConfig(xmlVal,
                new DefaultTcpAcceptorDependencies.PoolConfig()));

        // <outgoing-buffer-pool>
        xmlVal = xmlCat.getSafeElement("outgoing-buffer-pool");
        deps.setOutgoingBufferPoolConfig(populateBufferPoolConfig(xmlVal,
                new DefaultTcpAcceptorDependencies.PoolConfig()));

        // <authorized-hosts>
        deps.setAuthorizedHostFilterBuilder(LegacyXmlConfigHelper.parseAuthorizedHosts(xmlCat));

        // <suspect-protocol-enabled>
        deps.setSuspectProtocolEnabled(xmlCat.getSafeElement("suspect-protocol-enabled")
                .getBoolean(deps.isSuspectProtocolEnabled()));

        // <suspect-buffer-size>
        deps.setDefaultSuspectBytes(Base.parseMemorySize(xmlCat.getSafeElement("suspect-buffer-size")
                .getString(Long.toString(deps.getDefaultSuspectBytes()))));

        // <suspect-buffer-length>
        deps.setDefaultSuspectMessages(xmlCat.getSafeElement("suspect-buffer-length")
                .getInt(deps.getDefaultSuspectMessages()));

        // <nominal-buffer-size>
        deps.setDefaultNominalBytes(Base.parseMemorySize(xmlCat.getSafeElement("nominal-buffer-size")
                .getString(Long.toString(deps.getDefaultNominalBytes()))));

        // <nominal-buffer-length>
        deps.setDefaultNominalMessages(xmlCat.getSafeElement("nominal-buffer-length")
                .getInt(deps.getDefaultNominalMessages()));

        // <limit-buffer-size>
        deps.setDefaultLimitBytes(Base.parseMemorySize(xmlCat.getSafeElement("limit-buffer-size")
                .getString(Long.toString(deps.getDefaultLimitBytes()))));

        // <limit-buffer-length>
        deps.setDefaultLimitMessages(xmlCat.getSafeElement("limit-buffer-length")
                .getInt(deps.getDefaultLimitMessages()));

        return deps;
        }

    /**
     * Populate a BufferPoolConfig object from the given XML configuration.
     *
     * @param xml     the incoming-buffer-pool or outgoing-buffer-pool element's children
     * @param config  the PoolConfig which will be populated with the buffer pool configuration
     *
     * @return the BufferPoolConfig object
     */
    @Deprecated
    private static BufferPoolConfig populateBufferPoolConfig(XmlElement xml,
            DefaultTcpAcceptorDependencies.PoolConfig config)
        {
        DefaultTcpAcceptorDependencies.PoolConfig pool =
            new DefaultTcpAcceptorDependencies.PoolConfig();

        // <buffer-size>
        pool.setBufferSize((int) Base.parseMemorySize(xml.getSafeElement("buffer-size")
                .getString(Integer.toString(config.getBufferSize()))));

        // <buffer-type>
        String sType = xml.getSafeElement("buffer-type").getString();
        int    nType = sType.equalsIgnoreCase("heap") ?
                BufferPoolConfig.TYPE_HEAP : BufferPoolConfig.TYPE_DIRECT;
        pool.setBufferType(nType);

        // <capacity>
        pool.setCapacity(xml.getSafeElement("capacity")
                .getInt(pool.getCapacity()));

        return pool;
        }
    }
