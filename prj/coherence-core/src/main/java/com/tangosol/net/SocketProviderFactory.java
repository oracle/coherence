/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
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
import com.tangosol.internal.net.LegacyXmlSocketProviderFactoryDependencies;
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
     * Return a Socket provider. Only there for Proxy till they move to use
     * MultiplexedSocketProvider
     *
     * @param xml  provider definition, or null for the default provider
     *
     * @return the provider
     */
    @Deprecated
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
     * @param sId  provider name defined in &lt;socket-providers&gt;
     * @param deps anonymous {@link SocketProviderFactory.Dependencies}
     * @param nSubport  Subport for Demultiplexed socket provider.
     *                  If it is 0, then it implies Multiplexed socket provider.
     *
     * @return the provider
     */
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
     * @param sId       provider definition identifier or {@link #UNNAMED_PROVIDER_ID} for inlined, anonymous socketprovider
     * @param deps      inlined socket provider dependencies, must be non-null if <code>sId</code> is set to {@link #UNNAMED_PROVIDER_ID}
     * @param nSubport  subport for demultiplexed socket provider.
     *
     * @return a {@link DemultiplexedSocketProvider} based on method parameters.
     */
    public SocketProvider getDemultiplexedSocketProvider(String sId, SocketProviderFactory.Dependencies deps, int nSubport)
        {
        return sId == null ? new DemultiplexedSocketProvider(TcpSocketProvider.MULTIPLEXED, nSubport) : ensureSocketProvider(sId, deps, nSubport);
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
        return getDemultiplexedSocketProvider(builder.getId(), builder.getDependencies(), nSubport);
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
     * @param sId       provider definition identifier or {@link #UNNAMED_PROVIDER_ID} for inlined, anonymous socketprovider
     * @param deps      inlined socket provider dependencies, must be non-null if <code>sId</code> is set to {@link #UNNAMED_PROVIDER_ID}
     * @param nSubport  subport for {@link DatagramSocketProvider}.
     *
     * @return a {@link DatagramSocketProvider} configured via method parameters
     */
    public DatagramSocketProvider getDatagramSocketProvider(String sId, SocketProviderFactory.Dependencies deps, int nSubport)
        {
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
    public DatagramSocketProvider getDefaultDatagramSocketProvider(SocketProviderBuilder builder, int nSubport)
        {
        return getDatagramSocketProvider(builder.getId(), builder.getDependencies(), nSubport);
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
           .setClientAuthenticationRequired(depsSSL.isClientAuthenticationRequired())
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

    // ----- Helper methods ---------------------------------------------

    /**
     * Return an SSLSettings initilize via {@link SSLSocketProvider.Dependencies}
     *
     * @param depsSSL SSL Dependencies info
     *
     * @return a new {@link SSLSettings} initialized via <code>depsSSL</code>
     */
    static public SSLSettings createSSLSettings(SSLSocketProvider.Dependencies depsSSL)
        {
        SSLSettings settingsSSL = new SSLSettings();

        settingsSSL.setSSLContext(depsSSL.getSSLContext())
                .setClientAuthenticationRequired(depsSSL.isClientAuthenticationRequired())
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
            provider = (SocketProvider) m_mapSocketProvider.get(sKey);
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
     * Create DatatgramSocketProvider
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
            provider = (DatagramSocketProvider) m_mapDatagramSocketProvider.get(sKey);
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
                            // We don't have a way to secure this so we can't provide MulticastSockets
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
            SDP ("sdp");

            ProviderType(String name)
                {
                m_sName = name;
                }

            public String getName()
                {
                return m_sName;
                }

            final String m_sName;
            };

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
     * Additionally this class serves as a source of default dependency values.
     */
    public static class DefaultDependencies
        implements Dependencies
        {
        /**
         * Construct a DefaultSocketProviderDependencies object.
         */
        public DefaultDependencies()
            {
            Map mapProvider = m_mapProvider;
            mapProvider.put(ProviderType.SYSTEM.getName(), ProviderType.SYSTEM);
            mapProvider.put(ProviderType.TCP.getName(), ProviderType.TCP);
            mapProvider.put(ProviderType.SSL.getName(), ProviderType.SSL);
            mapProvider.put(ProviderType.SDP.getName(), ProviderType.SDP);
            m_mapTCPDatagramDependencies.put(ProviderType.TCP.getName(), new TcpDatagramSocketProvider.DefaultDependencies());
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public TcpDatagramSocketProvider.Dependencies getTcpDatagramSocketDependencies(String sId)
            {
            return (TcpDatagramSocketProvider.Dependencies) m_mapTCPDatagramDependencies.get(sId);
            }

        /**
         * {@inheritDoc}
         */
        @Override
        synchronized public SSLSocketProvider.Dependencies getSSLDependencies(String sId)
            {
            SSLSocketProvider.Dependencies deps = (SSLSocketProvider.Dependencies) m_mapSSLDependencies.get(sId);
            if (deps == null)
                {
                SSLSocketProviderDependenciesBuilder bldr = (SSLSocketProviderDependenciesBuilder) m_mapSSLDependenciesBuilder.get(sId);
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
            return (ProviderType) m_mapProvider.get(sId);
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

        public Map getSSLDependenciesBuilderMap()
            {
            return Collections.unmodifiableMap(m_mapSSLDependenciesBuilder);
            }

        // ----- data members ---------------------------------------------------

        /**
         * A map of SSL provider dependencies, key'd by id.
         */
        protected Map m_mapSSLDependencies = new SafeHashMap();

        /**
         * A map of SSL provider dependencies builder, key'd by id.
         * Builder is removed from this map when realized SSLDependencies is placed in {@link #m_mapSSLDependencies}
         */
        protected Map m_mapSSLDependenciesBuilder = new SafeHashMap();

        /**
         * A map of TCP Datagram provider dependencies, key'd by id.
         */
        protected Map m_mapTCPDatagramDependencies = new SafeHashMap();

        /**
         * A map of provider types, key'd by id.
         */
        protected Map m_mapProvider = new SafeHashMap();

        /**
         * SocketProviderFactory referencing this Dependency object.
         */
        protected SocketProviderFactory m_providerFactory;
        }

    // ----- data members ---------------------------------------------------

    /**
    * A map of instantiated socket providers, key'd by id.
    */
    protected Map m_mapSocketProvider = new SafeHashMap();

    /**
     * A map of instantiated datagram socket providers, key'd by id.
     */
     protected Map m_mapDatagramSocketProvider = new SafeHashMap();

    /**
     * Dependencies
     */
    protected Dependencies m_Dependencies;

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
    }
