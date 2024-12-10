/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;


import com.oracle.coherence.common.internal.net.DemultiplexedSocketProvider;
import com.oracle.coherence.common.internal.net.MultiplexedSocketProvider;

import com.oracle.coherence.common.net.SdpSocketProvider;
import com.oracle.coherence.common.net.SocketProvider;
import com.oracle.coherence.common.net.SSLSettings;
import com.oracle.coherence.common.net.SSLSocketProvider;
import com.oracle.coherence.common.net.TcpSocketProvider;

import com.tangosol.coherence.config.builder.SSLSocketProviderDependenciesBuilder;
import com.tangosol.coherence.config.builder.SocketProviderBuilder;

import com.tangosol.config.ConfigurationException;

import com.tangosol.config.xml.ProcessingContext;

import com.tangosol.internal.net.LegacyXmlSocketProviderFactoryDependencies;

import com.tangosol.internal.net.cluster.DefaultClusterDependencies;

import com.tangosol.internal.net.ssl.LegacyXmlSSLSocketProviderDependencies;

import com.tangosol.run.xml.XmlElement;

import com.tangosol.util.SafeHashMap;

import java.io.IOException;

import java.net.MulticastSocket;

import java.util.Collections;
import java.util.Map;

/**
* The SocketProviderFactory produces SocketProviders.
*
* @author mf, jh, bb  2010.04.21
* @since Coherence 3.6
*/
public class SocketProviderFactory
    {
    /**
     * Construct a SocketProviderFactory
     */
     public SocketProviderFactory()
         {
         this(null);
         }

    /**
     * Construct a SocketProviderFactory.
     *
     * @param dependencies  SocketProviderFactory dependencies or null
     */
     public SocketProviderFactory(Dependencies dependencies)
         {
         m_Dependencies = dependencies == null
                             ? new DefaultDependencies()
                             : dependencies;
         m_Dependencies.setSocketProviderFactory(this);
         f_defaultSocketProviderBuilder = new SocketProviderBuilder(null, m_Dependencies, true);
         }

    /**
     * Get SocketProviderDependencies object
     *
     * @return SocketProviderDependencies
     */
    public Dependencies getDependencies()
        {
        return m_Dependencies;
        }

    /**
     * Return the default {@link SocketProviderBuilder}.
     *
     * @return the default {@link SocketProviderBuilder}
     */
    public SocketProviderBuilder getDefaultSocketProviderBuilder()
        {
        return f_defaultSocketProviderBuilder;
        }

    /**
     * Return a Socket provider. Only there for Proxy till they move to use
     * MultiplexedSocketProvider
     *
     * @param xml  provider definition, or null for the default provider
     *
     * @return the provider
     */
    @Deprecated
    @SuppressWarnings("DeprecatedIsStillUsed")
    public SocketProvider getLegacySocketProvider(XmlElement xml)
        {
        String sId = LegacyXmlSocketProviderFactoryDependencies.getProviderId(xml);
        if (sId == null)
            {
            return DEFAULT_LEGACY_SOCKET_PROVIDER;
            }
        else if (sId.equals(UNNAMED_PROVIDER_ID))
            {
            LegacyXmlSocketProviderFactoryDependencies depsUnnamed =
                        new LegacyXmlSocketProviderFactoryDependencies(sId, xml);
            depsUnnamed.setSocketProviderFactory(this);
            return ensureSocketProvider(sId, depsUnnamed, -1);
            }
        return ensureSocketProvider(sId, getDependencies(), -1);
        }

    /**
     * Return a {@link SocketProviderBuilder} configured from the specified xml.
     *
     * @param xml  provider definition, or null for the default provider
     *
     * @return the {@link SocketProviderBuilder}
     */
    @Deprecated
    public SocketProviderBuilder getSocketProviderBuilder(XmlElement xml)
        {
        return getSocketProviderBuilder(xml, true);
        }

    /**
     * Return a {@link SocketProviderBuilder} configured from the specified xml.
     *
     * @param xml            provider definition, or null for the default provider
     * @param fCanUseGlobal  {@code true} to allow use of a global provider
     *
     * @return the {@link SocketProviderBuilder}
     */
    @Deprecated
    public SocketProviderBuilder getSocketProviderBuilder(XmlElement xml, boolean fCanUseGlobal)
        {
        String sId = LegacyXmlSocketProviderFactoryDependencies.getProviderId(xml);
        if (sId == null)
            {
            return new SocketProviderBuilder(DEFAULT_SOCKET_PROVIDER, fCanUseGlobal);
            }

        SocketProvider provider;

        if (sId.equals(UNNAMED_PROVIDER_ID))
            {
            LegacyXmlSocketProviderFactoryDependencies depsUnnamed =
                        new LegacyXmlSocketProviderFactoryDependencies(sId, xml);
            depsUnnamed.setSocketProviderFactory(this);
            provider = ensureSocketProvider(sId, depsUnnamed, 0);
            }
        else
            {
            provider = ensureSocketProvider(sId, getDependencies(), 0);
            }

        return new SocketProviderBuilder(provider, false);
        }

    /**
     * Return a Socket provider
     *
     * @param xml  provider definition, or null for the default provider
     *
     * @return the provider
     */
    @Deprecated
    public SocketProvider getSocketProvider(XmlElement xml)
        {
        String sId = LegacyXmlSocketProviderFactoryDependencies.getProviderId(xml);
        if (sId == null)
            {
            if (s_globalSocketProviderBuilder != null)
                {
                return s_globalSocketProviderBuilder.realize(null, null, null);
                }
            return DEFAULT_SOCKET_PROVIDER;
            }
        else if (sId.equals(UNNAMED_PROVIDER_ID))
            {
            LegacyXmlSocketProviderFactoryDependencies depsUnnamed =
                        new LegacyXmlSocketProviderFactoryDependencies(sId, xml);
            depsUnnamed.setSocketProviderFactory(this);
            return ensureSocketProvider(sId, depsUnnamed, 0);
            }
        return ensureSocketProvider(sId, getDependencies(), 0);
        }

    /**
     * Return a Socket provider
     *
     * @param sId  provider name defined in &lt;socket-providers&gt;
     *
     * @return the provider
     */
    public SocketProvider getSocketProvider(String sId)
        {
        return getSocketProvider(sId, getDependencies(), 0);
        }

    /**
     * Return a Socket provider
     *
     * @param sId       provider name defined in &lt;socket-providers&gt;
     * @param deps      anonymous {@link SocketProviderFactory.Dependencies}
     * @param nSubport  Sub-port for De-multiplexed socket provider.
     *                  If it is 0, then it implies Multiplexed socket provider.
     *
     * @return the provider
     */
    @SuppressWarnings("unused")
    public SocketProvider getSocketProvider(String sId, Dependencies deps, int nSubport)
        {
            if (sId == null)
                {
                return DEFAULT_SOCKET_PROVIDER;
                }
            return ensureSocketProvider(sId, deps , 0);
        }

    /**
     * Return a Demultiplexed Socket provider
     *
     * @param xml       provider definition, or null for the default provider

     * @param nSubport  subport for demultiplexed socket provider.
     *
     * @return the provider
     */
    @Deprecated
    @SuppressWarnings("DeprecatedIsStillUsed")
    public SocketProvider getDemultiplexedSocketProvider(XmlElement xml, int nSubport)
        {
        String sId = LegacyXmlSocketProviderFactoryDependencies.getProviderId(xml);
        if (sId == null)
            {
            return new DemultiplexedSocketProvider(TcpSocketProvider.MULTIPLEXED, nSubport);
            }
        else if (sId.equals(UNNAMED_PROVIDER_ID))
            {
            LegacyXmlSocketProviderFactoryDependencies depsUnnamed =
                    new LegacyXmlSocketProviderFactoryDependencies(sId, xml);
            depsUnnamed.setSocketProviderFactory(this);
            return ensureSocketProvider(sId, depsUnnamed, nSubport);
            }
        return ensureSocketProvider(sId, getDependencies(), nSubport);
        }

    /**
     * Return a Demultiplexed Socket provider
     *
     * @param sId            provider definition identifier or {@link #UNNAMED_PROVIDER_ID} for inlined,
     *                       anonymous socket provider
     * @param deps           inlined socket provider dependencies, must be non-null if {@code sId} is
     *                       set to {@link #UNNAMED_PROVIDER_ID}
     * @param nSubport       subport for demultiplexed socket provider
     * @param fCanUseGlobal  {@code true} if the global socket provider can be used
     *
     * @return a {@link DemultiplexedSocketProvider} based on method parameters.
     */
    public SocketProvider getDemultiplexedSocketProvider(String sId, SocketProviderFactory.Dependencies deps,
            int nSubport, boolean fCanUseGlobal)
        {
        if (fCanUseGlobal && s_globalSocketProviderBuilder != null)
            {
            return s_globalSocketProviderBuilder.getDemultiplexedSocketProvider(nSubport);
            }

        return sId == null
                ? new DemultiplexedSocketProvider(TcpSocketProvider.MULTIPLEXED, nSubport)
                : ensureSocketProvider(sId, deps, nSubport);
        }

    /**
     * Return a Demultiplexed Socket provider
     *
     * @param builder   use socket provider id and dependencies from this {@link SocketProviderBuilder}
     * @param nSubport  subport for demultiplexed socket provider.
     *
     * @return a {@link DemultiplexedSocketProvider} based on method parameters.
     */
    public SocketProvider getDemultiplexedSocketProvider(SocketProviderBuilder builder, int nSubport)
        {
        return getDemultiplexedSocketProvider(builder.getId(), builder.getDependencies(),
                nSubport, builder.canUseGlobal());
        }

    /**
     * Return an instance of the specified DatagramSocketProvider, creating it if necessary.
     *
     * @param  xml       the provider definition, or null for the default provider
     *
     * @param  nSubport  subport for a demultiplexed socket provider.
     *
     * @return the provider
     */
    @Deprecated
    @SuppressWarnings("DeprecatedIsStillUsed")
    public DatagramSocketProvider getDatagramSocketProvider(XmlElement xml, int nSubport)
        {
        String sId = LegacyXmlSocketProviderFactoryDependencies.getProviderId(xml);
        if (sId == null)
            {
            return DEFAULT_DATAGRAM_SOCKET_PROVIDER;
            }
        if (sId.equals(UNNAMED_PROVIDER_ID))
            {
            LegacyXmlSocketProviderFactoryDependencies depsUnnamed =
                    new LegacyXmlSocketProviderFactoryDependencies(sId, xml);
            depsUnnamed.setSocketProviderFactory(this);
            return ensureDatagramSocketProvider(sId, depsUnnamed, nSubport);
            }
        return ensureDatagramSocketProvider(sId, getDependencies(), nSubport);
        }

    /**
     * Return an instance of the specified DatagramSocketProvider, creating it if necessary.
     *
     * @param sId            provider definition identifier or {@link #UNNAMED_PROVIDER_ID} for inlined,
     *                       anonymous socket provider
     * @param deps           inlined socket provider dependencies, must be non-null if {@code sId}
     *                       is set to {@link #UNNAMED_PROVIDER_ID}
     * @param nSubport       subport for {@link DatagramSocketProvider}.
     * @param fCanUseGlobal  {@code true} if the global socket provider can be used
     *
     * @return a {@link DatagramSocketProvider} configured via method parameters
     */
    public DatagramSocketProvider getDatagramSocketProvider(String sId, SocketProviderFactory.Dependencies deps,
            int nSubport, boolean fCanUseGlobal)
        {
        if (fCanUseGlobal && s_globalSocketProviderBuilder != null)
            {
            sId  = s_globalSocketProviderBuilder.getId();
            deps = s_globalSocketProviderBuilder.getDependencies();
            }
        return sId == null ? DEFAULT_DATAGRAM_SOCKET_PROVIDER : ensureDatagramSocketProvider(sId, deps, nSubport);
        }

    /**
     * Return an instance of the specified DatagramSocketProvider, creating it if necessary.
     *
     * @param builder   use socket provider id and dependencies from this {@link SocketProviderBuilder}
     * @param nSubport  subport for {@link DatagramSocketProvider}.
     *
     * @return a {@link DatagramSocketProvider} configured via method parameters
     */
    @SuppressWarnings("unused")
    public DatagramSocketProvider getDefaultDatagramSocketProvider(SocketProviderBuilder builder, int nSubport)
        {
        return getDatagramSocketProvider(builder.getId(), builder.getDependencies(), nSubport, builder.canUseGlobal());
        }

    /**
     * Return an instance of SSLSettings from the specified xml.
     *
     * @param  xml  the provider definition, or null for the default provider
     *
     * @return the sslSettings
     */
    @Deprecated
    public SSLSettings getSSLSettings(XmlElement xml)
        {
        String sId = LegacyXmlSocketProviderFactoryDependencies.getProviderId(xml);

        SSLSocketProvider.Dependencies depsSSL = getDependencies().getSSLDependencies(sId);
        if (depsSSL == null)
            {
            if (sId.equals(UNNAMED_PROVIDER_ID))
                {
                LegacyXmlSocketProviderFactoryDependencies depsUnnamed =
                        new LegacyXmlSocketProviderFactoryDependencies(sId, xml);
                depsUnnamed.setSocketProviderFactory(this);
                depsSSL = depsUnnamed.getSSLDependencies(sId);
                }
            else
                {
                depsSSL = new LegacyXmlSSLSocketProviderDependencies(xml);
                }
            }

        SSLSettings settingsSSL = new SSLSettings();
        settingsSSL.setSSLContext(depsSSL.getSSLContext())
           .setClientAuth(depsSSL.getClientAuth())
           .setHostnameVerifier(depsSSL.getHostnameVerifier())
           .setEnabledCipherSuites(depsSSL.getEnabledCipherSuites())
           .setEnabledProtocolVersions(depsSSL.getEnabledProtocolVersions());
        return settingsSSL;
        }

    /**
     * Return SSLSettings for the specified SocketProvider.
     *
     * @param  socketProvider  the socketProvider
     *
     * @return the sslSettings if the socket provider is an instance of SSLSocketProvider
     *         or null
     */
    public SSLSettings getSSLSettings(SocketProvider socketProvider)
        {
        if (socketProvider instanceof SSLSocketProvider)
            {
            SSLSocketProvider              providerSSL = (SSLSocketProvider) socketProvider;
            SSLSocketProvider.Dependencies depsSSL     = providerSSL.getDependencies();
            return createSSLSettings(depsSSL);
            }

        return null;
        }

    /**
     * Return SSLSettings for the specified SocketProviderBuilder.
     *
     * @param  builder  the socketProviderBuilder
     *
     * @return the sslSettings if the socket provider builder has a ssl settings directly or via delegate.
     */
    public SSLSettings getSSLSettings(SocketProviderBuilder builder)
        {
        Dependencies deps = getDependencies();
        if (deps != null)
            {
            SSLSocketProvider.Dependencies depsSSL = getDependencies().getSSLDependencies(builder.getId());
            if (depsSSL != null)
                {
                    return createSSLSettings(depsSSL);
                }
            }
        else
            {
            return getSSLSettings(builder.realize(null, null, null));
            }

        return null;
        }

    /**
     * Returns the global {@link SocketProviderBuilder} or {@code null}
     * if no global provider has been set.
     *
     * @return the global {@link SocketProviderBuilder} or {@code null}
     *         if no global provider has been set
     */
    public static SocketProviderBuilder getGlobalSocketProviderBuilder()
        {
        return s_globalSocketProviderBuilder;
        }

    /**
     * Set the global {@link SocketProviderBuilder}.
     *
     * @param builder  the global {@link SocketProviderBuilder}
     */
    public static void setGlobalSocketProviderBuilder(SocketProviderBuilder builder)
        {
        if (builder != null && builder.canUseGlobal())
            {
            throw new IllegalArgumentException("The global socket provider builder cannot be set to also use the global provider");
            }
        s_globalSocketProviderBuilder = builder;
        }

    /**
     * Set the global {@link SocketProviderBuilder}.
     *
     * @param builder  the global {@link SocketProviderBuilder}
     */
    public static void setGlobalSocketProvider(SocketProviderBuilder builder)
        {
        SocketProviderFactory.setGlobalSocketProviderBuilder(builder);
        }

    // ----- Helper methods ---------------------------------------------

    /**
     * Return the cluster's {@link SocketProviderFactory}.
     * @param ctx  Cluster operational context
     * @param xml  socket-provider xml fragment being processed.
     * @return the cluster's {@link SocketProviderFactory}
     */
    public static SocketProviderFactory getSocketProviderFactory(ProcessingContext ctx, XmlElement xml)
        {
        // grab the operational context from which we can look up the socket provider factory
        OperationalContext ctxOperational = ctx.getCookie(OperationalContext.class);

        if (ctxOperational == null)
            {
            DefaultClusterDependencies deps = ctx.getCookie(DefaultClusterDependencies.class);
            if (deps == null)
                {
                throw new ConfigurationException("Attempted to resolve the OperationalContext in [" + xml
                        + "] but it was not defined", "The registered ElementHandler for the <"
                        + xml.getName()
                        + "> element is not operating in an OperationalContext");
                }
            return deps.getSocketProviderFactory();
            }
        else
            {
            return ctxOperational.getSocketProviderFactory();
            }
        }

    /**
     * Return an SSLSettings initialize via {@link SSLSocketProvider.Dependencies}
     *
     * @param depsSSL SSL Dependencies info
     *
     * @return a new {@link SSLSettings} initialized via <code>depsSSL</code>
     */
    static public SSLSettings createSSLSettings(SSLSocketProvider.Dependencies depsSSL)
        {
        SSLSettings settingsSSL = new SSLSettings();

        settingsSSL.setSSLContext(depsSSL.getSSLContext())
                .setClientAuth(depsSSL.getClientAuth())
                .setHostnameVerifier(depsSSL.getHostnameVerifier())
                .setEnabledCipherSuites(depsSSL.getEnabledCipherSuites())
                .setEnabledProtocolVersions(depsSSL.getEnabledProtocolVersions());

        return settingsSSL;
        }

    /**
     * Create SocketProvider
     *
     * @param sId       SocketProviderId
     *
     * @param deps      Dependencies for the given SocketProvider
     *
     * @param nSubport  Subport for Demultiplexed socket provider.
     *                  If it is 0, then it implies Multiplexed socket provider.
     *
     * @return the SocketProvider
     */
    protected SocketProvider ensureSocketProvider(String sId, Dependencies deps, int nSubport)
        {
        SocketProvider provider = null;
        String         sKey     = (nSubport == 0)
                                    ? sId
                                    : sId + ":" + nSubport;
        if (!sId.equals(UNNAMED_PROVIDER_ID))
            {
            provider = m_mapSocketProvider.get(sKey);
            }
        if (provider == null)
            {
            Dependencies.ProviderType providerType = deps.getProviderType(sId);
            if (providerType == null)
                {
                throw new IllegalArgumentException("Unknown SocketProvider: "
                        + sId);
                }
            switch (providerType)
                {
                case SYSTEM:
                case GRPC:
                case TCP:
                    {
                    provider = (nSubport == 0)
                                ? TcpSocketProvider.MULTIPLEXED
                                : new DemultiplexedSocketProvider(TcpSocketProvider.MULTIPLEXED, nSubport);
                    break;
                    }
                case SSL:
                    {
                    SSLSocketProvider.Dependencies depsSSL = deps.getSSLDependencies(sId);
                    SocketProvider delegate = depsSSL.getDelegateSocketProvider();
                    if (delegate instanceof SdpSocketProvider)
                        {
                        delegate = SdpSocketProvider.MULTIPLEXED;
                        }
                    else if (delegate instanceof TcpSocketProvider)
                        {
                        delegate = TcpSocketProvider.MULTIPLEXED;
                        }
                    // else it is already a multiplexed socket provider

                    if (nSubport != 0)
                        {
                        // replace delegateProvider with its de-multiplexed version
                        delegate = new DemultiplexedSocketProvider(
                            (MultiplexedSocketProvider) delegate, nSubport);
                        }
                    provider = new SSLSocketProvider(
                        new SSLSocketProvider.DefaultDependencies(depsSSL).
                            setDelegate(delegate));
                    break;
                    }
                case SDP:
                    {
                    provider = (nSubport == 0)
                                ? SdpSocketProvider.MULTIPLEXED
                                : new DemultiplexedSocketProvider(SdpSocketProvider.MULTIPLEXED, nSubport);
                    break;
                    }
                default: throw new IllegalArgumentException("Unknown Socket provider type: "+sId);
                }
            m_mapSocketProvider.put(sKey, provider);
            }

        return provider;
        }

    /**
     * Create a {@link DatagramSocketProvider}
     *
     * @param sId           DatagramSocketProviderId
     *
     * @param providerDeps  Dependencies for the given DatagramSocketProvider
     *
     * @param nSubport      Subport for Demultiplexed socket provider.
     *                      If it is 0, then it implies Multiplexed socket provider.
     *
     * @return the DatagramSocketProvider
     */
    protected DatagramSocketProvider ensureDatagramSocketProvider(String sId,
            Dependencies providerDeps, int nSubport)
        {
        DatagramSocketProvider provider = null;
        String                 sKey     = (nSubport == 0)
                                              ? sId
                                              : sId + ":" + nSubport;
        if (!sId.equals(UNNAMED_PROVIDER_ID))
            {
            provider = m_mapDatagramSocketProvider.get(sKey);
            }
        if (provider == null)
            {
            Dependencies.ProviderType providerType = providerDeps.getProviderType(sId);
            if (providerType == null)
                {
                throw new IllegalArgumentException("Unknown DatagramSocketProvider: "
                        + sId);
                }
            switch (providerType)
                {
                case SYSTEM:
                    {
                    provider = SystemDatagramSocketProvider.INSTANCE;
                    break;
                    }
                case GRPC:
                case TCP:
                case SDP:
                    {
                    TcpDatagramSocketProvider.DefaultDependencies deps =
                            new TcpDatagramSocketProvider.DefaultDependencies(
                                providerDeps.getTcpDatagramSocketDependencies(sId));
                    deps.setDelegateSocketProvider(
                            ensureSocketProvider(sId, providerDeps, nSubport));
                    provider = new TcpDatagramSocketProvider(deps);
                    break;
                    }
                case SSL:
                    {
                    TcpDatagramSocketProvider.DefaultDependencies deps =
                            new TcpDatagramSocketProvider.DefaultDependencies(
                                providerDeps.getTcpDatagramSocketDependencies(sId));
                    deps.setDelegateSocketProvider(
                        ensureSocketProvider(sId, providerDeps, nSubport));
                    provider = new TcpDatagramSocketProvider(deps)
                        {
                        @Override
                        public MulticastSocket openMulticastSocket()
                                throws IOException
                            {
                            // We don't have a way to secure this, so we can't provide MulticastSockets
                            throw new IOException("MulticastSocket is not supported with SSL");
                            }

                        @Override
                        public boolean isSecure()
                            {
                            return true;
                            }
                        };
                     break;
                    }
                default: throw new IllegalArgumentException("Unknown Socket provider type: "+sId);
                }
            m_mapDatagramSocketProvider.put(sKey, provider);
            }
        return provider;
        }


    // ----- Object methods -------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
        {
        return "SocketProviderFactory"
                + ", SocketProviderMap=        " + m_mapSocketProvider
                + ", DatagramSocketProviderMap=" + m_mapDatagramSocketProvider
                + "}";
        }

    /**
     * Dependencies specifies all dependency requirements of the SocketProviderFactory.
     */
    public interface Dependencies
        {
        /**
         * Enumeration of Provider types.
         */
        enum ProviderType
            {
            SYSTEM ("system"),
            TCP ("tcp"),
            SSL ("ssl"),
            SDP ("sdp"),
            GRPC ("grpc-insecure");

            ProviderType(String name)
                {
                m_sName = name;
                }

            public String getName()
                {
                return m_sName;
                }

            final String m_sName;
            }

        /**
         * Get the provider type for the given socket provider id
         *
         * @param sId  socket provider id
         *
         * @return provider type
         */
        ProviderType getProviderType(String sId);

        /**
         * Get the TcpDatagramSocketProvider's dependencies associated with the given socket provider id
         *
         * @param sId  socket provider id
         *
         * @return TcpDatagramSocketProvider's dependencies
         */
        TcpDatagramSocketProvider.Dependencies getTcpDatagramSocketDependencies(String sId);

        /**
         * Get the SSLSocketProvider's dependencies associated with the given socket provider id
         *
         * @param sId  socket provider id
         *
         * @return SSLSocketProvider's dependencies
         */
        SSLSocketProvider.Dependencies getSSLDependencies(String sId);

        /**
         * Set the SocketProviderFactory referencing the Dependency object. This is
         * needed mainly to resolve delegate socket provider for SSLSocketProvider.
         *
         * @param factory  SocketProviderFactory referencing the Dependency object.
         */
        void setSocketProviderFactory(SocketProviderFactory factory);

        /**
         * Get the associated SocketProviderFactory for the Dependency object.
         *
         * @return SocketProviderFactory
         */
        SocketProviderFactory getSocketProviderFactory();
        }

    /**
     * DefaultDependencies is a basic implementation of the Dependencies
     * interface.
     * <p>
     * Additionally, this class serves as a source of default dependency values.
     */
    public static class DefaultDependencies
        implements Dependencies
        {
        /**
         * Construct a DefaultSocketProviderDependencies object.
         */
        public DefaultDependencies()
            {
            Map<String, ProviderType> mapProvider = m_mapProvider;
            mapProvider.put(ProviderType.SYSTEM.getName(), ProviderType.SYSTEM);
            mapProvider.put(ProviderType.TCP.getName(), ProviderType.TCP);
            mapProvider.put(ProviderType.SSL.getName(), ProviderType.SSL);
            mapProvider.put(ProviderType.SDP.getName(), ProviderType.SDP);
            mapProvider.put(ProviderType.GRPC.getName(), ProviderType.GRPC);
            m_mapTCPDatagramDependencies.put(ProviderType.TCP.getName(), new TcpDatagramSocketProvider.DefaultDependencies());
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public TcpDatagramSocketProvider.Dependencies getTcpDatagramSocketDependencies(String sId)
            {
            return m_mapTCPDatagramDependencies.get(sId);
            }

        /**
         * {@inheritDoc}
         */
        @Override
        synchronized public SSLSocketProvider.Dependencies getSSLDependencies(String sId)
            {
            SSLSocketProvider.Dependencies deps = m_mapSSLDependencies.get(sId);
            if (deps == null)
                {
                SSLSocketProviderDependenciesBuilder bldr = m_mapSSLDependenciesBuilder.get(sId);
                if (bldr != null)
                    {
                    deps = bldr.realize();
                    addNamedSSLDependencies(sId, deps);
                    m_mapSSLDependenciesBuilder.remove(sId);
                    }
                }
            return deps;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public ProviderType getProviderType(String sId)
            {
            return m_mapProvider.get(sId);
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setSocketProviderFactory(SocketProviderFactory factory)
            {
            m_providerFactory = factory;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public SocketProviderFactory getSocketProviderFactory()
            {
            SocketProviderFactory factory = m_providerFactory;
            if (factory == null)
                {
                factory = m_providerFactory = new SocketProviderFactory(this);
                }
            return factory;
            }

        /**
         * Add {@link ProviderType} for <code>sId</code> identifier to this SocketProviderFactory's Dependency mapping.
         *
         * @param sId  provider identifier
         * @param type {@link ProviderType}
         */
        public void addNamedProviderType(String sId, ProviderType type)
            {
            m_mapProvider.put(sId, type);
            }

        /**
         * Add {@link SSLSocketProvider.Dependencies} for <code>sId</code> identifier to this SocketProviderFactory's Dependency mapping.
         *
         * @param sId  provider identifier
         * @param deps SSL dependencies
         */
        public void addNamedSSLDependencies(String sId,  SSLSocketProvider.Dependencies deps)
            {
            m_mapSSLDependencies.put(sId, deps);
            }

        /**
         * Add {@link TcpDatagramSocketProvider.Dependencies} for <code>sId</code> identifier to this SocketProviderFactory's Dependency mapping.
         *
         * @param sId  provider identifier
         * @param deps TcpDatagram dependencies
         */
        public void addNamedTCPDatagramDependencies(String sId,  TcpDatagramSocketProvider.Dependencies deps)
            {
            m_mapTCPDatagramDependencies.put(sId, deps);
            }

        public void addNamedSSLDependenciesBuilder(String sId, SSLSocketProviderDependenciesBuilder bldr)
            {
            m_mapSSLDependenciesBuilder.put(sId, bldr);
            }

        public Map<String, SSLSocketProviderDependenciesBuilder> getSSLDependenciesBuilderMap()
            {
            return Collections.unmodifiableMap(m_mapSSLDependenciesBuilder);
            }

        // ----- data members ---------------------------------------------------

        /**
         * A map of SSL provider dependencies, keyed by id.
         */
        protected Map<String, SSLSocketProvider.Dependencies> m_mapSSLDependencies = new SafeHashMap<>();

        /**
         * A map of SSL provider dependencies builder, keyed by id.
         * Builder is removed from this map when realized SSLDependencies is placed in {@link #m_mapSSLDependencies}
         */
        protected Map<String, SSLSocketProviderDependenciesBuilder> m_mapSSLDependenciesBuilder = new SafeHashMap<>();

        /**
         * A map of TCP Datagram provider dependencies, keyed by id.
         */
        protected Map<String, TcpDatagramSocketProvider.Dependencies> m_mapTCPDatagramDependencies = new SafeHashMap<>();

        /**
         * A map of provider types, keyed by id.
         */
        protected Map<String, ProviderType> m_mapProvider = new SafeHashMap<>();

        /**
         * SocketProviderFactory referencing this Dependency object.
         */
        protected SocketProviderFactory m_providerFactory;
        }

    // ----- data members ---------------------------------------------------

    /**
    * A map of instantiated socket providers, keyed by id.
    */
    protected Map<String, SocketProvider> m_mapSocketProvider = new SafeHashMap<>();

    /**
     * A map of instantiated datagram socket providers, keyed by id.
     */
     protected Map<String, DatagramSocketProvider> m_mapDatagramSocketProvider = new SafeHashMap<>();

    /**
     * Dependencies
     */
    protected Dependencies m_Dependencies;

    /**
     * A default {@link SocketProviderBuilder}.
     */
    private final SocketProviderBuilder f_defaultSocketProviderBuilder;

    // ----- constants ------------------------------------------------------

    /**
    * The factory's default SocketProvider.
    */
    public static final SocketProvider DEFAULT_SOCKET_PROVIDER = TcpSocketProvider.MULTIPLEXED;

    /**
    * The factory's default legacy SocketProvider.
    */
    public static final SocketProvider DEFAULT_LEGACY_SOCKET_PROVIDER = TcpSocketProvider.DEMULTIPLEXED;

    /**
     * The factory's default Datagram SocketProvider.
     */
    public static final DatagramSocketProvider DEFAULT_DATAGRAM_SOCKET_PROVIDER = SystemDatagramSocketProvider.INSTANCE;

    /**
     * Default id for unnamed socket and datagram socket providers
     */
    public static final String UNNAMED_PROVIDER_ID = "";

    /**
     * The global socket provider builder.
     */
    private static SocketProviderBuilder s_globalSocketProviderBuilder;

    /**
     * The name of the system property used to set the global socket provider id.
     */
    public static final String PROP_GLOBAL_PROVIDER = "coherence.global.socketprovider";
    }
