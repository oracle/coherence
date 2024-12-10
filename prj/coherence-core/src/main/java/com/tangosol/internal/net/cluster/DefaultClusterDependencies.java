/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.cluster;

import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.common.base.Predicate;

import com.oracle.coherence.common.internal.Platform;

import com.oracle.coherence.common.net.InetAddresses;

import com.tangosol.coherence.config.builder.ActionPolicyBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilderRegistry;
import com.tangosol.coherence.config.builder.ServiceFailurePolicyBuilder;
import com.tangosol.coherence.config.builder.SimpleParameterizedBuilderRegistry;
import com.tangosol.coherence.config.builder.SocketProviderBuilder;

import com.tangosol.config.annotation.Injectable;

import com.tangosol.config.expression.NullParameterResolver;

import com.tangosol.io.ClassLoaderAware;
import com.tangosol.io.Serializer;
import com.tangosol.io.SerializerFactory;
import com.tangosol.io.WrapperStreamFactory;

import com.tangosol.net.AddressProvider;
import com.tangosol.net.AddressProviderFactory;
import com.tangosol.net.MemberIdentity;
import com.tangosol.net.SocketProviderFactory;

import com.tangosol.persistence.SnapshotArchiverFactory;

import com.tangosol.run.xml.XmlElement;

import com.tangosol.util.Base;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.LiteMap;
import com.tangosol.util.RegistrationBehavior;
import com.tangosol.util.ResourceRegistry;
import com.tangosol.util.SafeLinkedList;
import com.tangosol.util.SimpleResourceRegistry;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketOptions;
import java.net.UnknownHostException;

import java.security.AccessController;
import java.security.PrivilegedAction;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;

import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Named;

/**
 * DefaultClusterDependencies is a base implementation for ClusterDependencies.
 *
 * @author pfm  2011.05.01
 * @since Coherence 3.7.1
 */
public class DefaultClusterDependencies
        implements ClusterDependencies
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a DefaultClusterDependencies object.
     */
    public DefaultClusterDependencies()
        {
        this(null);
        }

    /**
     * Construct a DefaultClusterDependencies object. Copy the values from the
     * specified ClusterDependencies object.
     *
     * @param deps  the dependencies to copy, or null
     */
    public DefaultClusterDependencies(com.tangosol.net.ClusterDependencies deps)
        {
        if (deps == null)
            {
            m_builderRegistry = new SimpleParameterizedBuilderRegistry();
            }
        else
            {
            m_authorizedHostFilter           = deps.getAuthorizedHostFilter();
            m_bldrClusterActionPolicy        = deps.getClusterActionPolicyBuilder();
            m_cClusterAnnounceTimeout        = deps.getClusterAnnounceTimeoutMillis();
            m_cClusterHeartbeatDelay         = deps.getClusterHeartbeatDelayMillis();
            m_cClusterTimestampMaxVariance   = deps.getClusterTimestampMaxVarianceMillis();
            m_nEdition                       = deps.getEdition();
            m_listFilter                     = deps.getFilterList();
            m_mapFilter                      = deps.getFilterMap();
            m_fFlowControlEnabled            = deps.isFlowControlEnabled();
            m_discAddressLocal               = deps.getLocalDiscoveryAddress();
            m_groupAddress                   = deps.getGroupAddress();
            m_nGroupBufferSize               = deps.getGroupBufferSize();
            m_groupInterface                 = deps.getGroupInterface();
            m_nGroupListenerPriority         = deps.getGroupListenerPriority();
            m_gGroupPort                     = deps.getGroupPort();
            m_cGroupTimeToLive               = deps.getGroupTimeToLive();
            m_cGuardTimeout                  = deps.getGuardTimeoutMillis();
            m_cIpMonitorAttempts             = deps.getIpMonitorAttempts();
            m_nIpMonitorPriority             = deps.getIpMonitorPriority();
            m_cMillisIpMonitorTimeout        = deps.getIpMonitorTimeoutMillis();
            m_cLocalBufferSize               = deps.getLocalBufferSize();
            m_nLocalListenerPriority         = deps.getLocalListenerPriority();
            m_nLocalPort                     = deps.getLocalPort();
            m_nLocalPortAutoAdjust           = deps.getLocalPortAutoAdjust();
            m_cLostPacketThreshold           = deps.getLostPacketThreshold();
            m_nMode                          = deps.getMode();
            m_cOutstandingPacketMaximum      = deps.getOutstandingPacketMaximum();
            m_cOutstandingPacketMinimum      = deps.getOutstandingPacketMinimum();
            m_dPacketBundlingAggression      = deps.getPacketBundlingAggression();
            m_cPacketBundlingThresholdNanos  = deps.getPacketBundlingThresholdNanos();
            m_cbPacketMaxLength              = deps.getPacketMaxLength();
            m_cbPacketPreferredLength        = deps.getPacketPreferredLength();
            m_cPublisherAckDelay             = deps.getPublisherAckDelayMillis();
            m_cPublisherSocketBufferSize     = deps.getPublisherSocketBufferSize();
            m_cPublisherCloggedCount         = deps.getPublisherCloggedCount();
            m_cPublisherCloggedDelayMillis   = deps.getPublisherCloggedDelayMillis();
            m_cPublisherGroupThreshold       = deps.getPublisherGroupThreshold();
            m_cPublisherNackDelay            = deps.getPublisherNackDelayMillis();
            m_nPublisherPriority             = deps.getPublisherPriority();
            m_cPublisherResendDelayMillis    = deps.getPublisherResendDelayMillis();
            m_cPublisherResendTimeoutMillis  = deps.getPublisherResendTimeoutMillis();
            m_fReceiverNackEnabled           = deps.isReceiverNackEnabled();
            m_nReceiverPriority              = deps.getReceiverPriority();
            m_sReliableTransport             = deps.getReliableTransport();
            m_mapSerializer                  = deps.getSerializerMap();
            m_mapSnapshotArchiver            = deps.getSnapshotArchiverMap();
            m_mapAddressProvider             = deps.getAddressProviderMap();
            m_bldrServiceFailurePolicy       = deps.getServiceFailurePolicyBuilder();
            m_mapServiceFilter               = deps.getServiceFilterMap();
            m_mapService                     = deps.getServiceMap();
            m_nShutdownHookOption            = deps.getShutdownHookOption();
            m_socketProviderFactory          = deps.getSocketProviderFactory();
            m_xmlUnicastSocketProvider       = deps.getUnicastSocketProviderXml();
            m_nSpeakerPriority               = deps.getSpeakerPriority();
            m_cSpeakerVolumeMinimum          = deps.getSpeakerVolumeMinimum();
            m_fSpeakerEnabled                = deps.isSpeakerEnabled();
            m_fTcmpEnabled                   = deps.isTcmpEnabled();
            m_cTcpBacklog                    = deps.getTcpBacklog();
            m_optionsTcpDatagram             = deps.getTcpDatagramSocketOptions();
            m_fTcpRingEnabled                = deps.isTcpRingEnabled();
            m_optionsTcpRing                 = deps.getTcpRingSocketOptions();
            m_providerWellKnownAddresses     = deps.getWellKnownAddresses();
            m_localAddress                   = deps.getLocalAddress();
            m_builderRegistry                = new SimpleParameterizedBuilderRegistry(deps.getBuilderRegistry());
            m_builderUnicastSocketProvider   = deps.getUnicastSocketProviderBuilder();
            m_sLambdasSerializationMode      = deps.getLambdasSerializationMode();
            m_fVirtualThreadsEnabled         = deps.isVirtualThreadsEnabled();

            m_customResources = new SimpleResourceRegistry();
            deps.registerResources(m_customResources);

            setMemberIdentity(deps.getMemberIdentity()); // setMemberIdentity clones the identity
            }
        }

    // ----- accessors ----------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Filter getAuthorizedHostFilter()
        {
        return m_authorizedHostFilter;
        }

    /**
     * Set the AuthorizedHostFilter.
     *
     * @param filter  the AuthorizedHostFilter
     *
     * @return this object
     */
    public DefaultClusterDependencies setAuthorizedHostFilter(Filter filter)
        {
        m_authorizedHostFilter = filter;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public ActionPolicyBuilder getClusterActionPolicyBuilder()
        {
        return m_bldrClusterActionPolicy;
        }

    /**
     * Set the cluster action policy builder.
     *
     * @param builder  the cluster action policy builder
     *
     * @return this object
     */
    public DefaultClusterDependencies setClusterActionPolicyBuilder(ActionPolicyBuilder builder)
        {
        m_bldrClusterActionPolicy = builder;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getClusterAnnounceTimeoutMillis()
        {
        return m_cClusterAnnounceTimeout;
        }

    /**
     * Specify the maximum amount of time, in milliseconds, that the cluster
     * service will announce itself.
     *
     * @param cMillis  the cluster announce timeout
     *
     * @return this object
     */
    public DefaultClusterDependencies setClusterAnnounceTimeoutMillis(int cMillis)
        {
        m_cClusterAnnounceTimeout = Math.max(100, cMillis); // put a limit on how small the join time can be as a number of things derive from this
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getClusterHeartbeatDelayMillis()
        {
        int cMillis = m_cClusterHeartbeatDelay;
        if (cMillis == 0)
            {
            m_cClusterHeartbeatDelay = cMillis = Math.min(1000, getClusterAnnounceTimeoutMillis() / 3);
            }
        return cMillis;
        }

    /**
     * Specify the amount of time, in milliseconds, between heartbeat messages.
     *
     * @param cMillis  the cluster heartbeat delay
     *
     * @return this object
     */
    public DefaultClusterDependencies setClusterHeartbeatDelayMillis(int cMillis)
        {
        m_cClusterHeartbeatDelay = cMillis;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getClusterTimestampMaxVarianceMillis()
        {
        return m_cClusterTimestampMaxVariance;
        }

    /**
     * Set the cluster timestamp max variance.
     *
     * @param cMillis  the cluster timestamp max variance
     *
     * @return this object
     */
    public DefaultClusterDependencies setClusterTimestampMaxVarianceMillis(int cMillis)
        {
        m_cClusterTimestampMaxVariance = cMillis;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getEdition()
        {
        return m_nEdition;
        }

    /**
     * Set the Coherence edition.
     *
     * @param n  the edition
     *
     * @return this object
     */
    public DefaultClusterDependencies setEdition(int n)
        {
        m_nEdition = n;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({ "unchecked", "deprecation" })
    @Override
    public Map<String, WrapperStreamFactory> getFilterMap()
        {
        Map<String, WrapperStreamFactory> mapFilter = m_mapFilter;
        if (mapFilter == null)
            {
            m_mapFilter = mapFilter = new LiteMap();
            }
        return mapFilter ;
        }

    /**
     * Set the filter map.
     *
     * @param mapFilter  the filter map
     *
     * @return this object
     */
    @SuppressWarnings("deprecation")
    public DefaultClusterDependencies setFilterMap(Map<String, WrapperStreamFactory> mapFilter)
        {
        m_mapFilter = mapFilter;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<String> getFilterList()
        {
        List<String> listFilter = m_listFilter;
        if (listFilter == null)
            {
            m_listFilter = listFilter = new SafeLinkedList();
            }
        return listFilter;
        }

    /**
     * Set the filter list.
     *
     * @param listFilter  the filter list
     *
     * @return this object
     */
    public DefaultClusterDependencies setFilterList(List<String> listFilter)
        {
        m_listFilter = listFilter;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isFlowControlEnabled()
        {
        return m_fFlowControlEnabled;
        }

    /**
     * Specify true if flow control is enabled.
     *
     * @param fEnabled  true if flow control is enabled
     *
     * @return this object
     */
    public DefaultClusterDependencies setFlowControlEnabled(boolean fEnabled)
        {
        m_fFlowControlEnabled = fEnabled;
        return this;
        }

    public DefaultClusterDependencies setLocalDiscoveryAddress(InetAddress addr)
        {
        m_discAddressLocal = addr;
        return this;
        }

    @Override
    public InetAddress getLocalDiscoveryAddress()
        {
        InetAddress addr = m_discAddressLocal;
        if (addr == null)
            {
            m_discAddressLocal = addr = InetAddresses.ADDR_ANY;
            }

        return addr;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public InetAddress getGroupAddress()
        {
        InetAddress addr = m_groupAddress;
        if (addr == null)
            {
            try
                {
                m_groupAddress = addr = InetAddress.getByName(DEFAULT_ADDR);
                }
            catch (Exception e)
                {
                throw Base.ensureRuntimeException(e, "Unable to resolve default group address " + DEFAULT_ADDR);
                }
            }
        return addr;
        }

    /**
     * Set the group address.
     *
     * @param addr  the group address
     *
     * @return this object
     */
    public DefaultClusterDependencies setGroupAddress(InetAddress addr)
        {
        m_groupAddress = addr;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getGroupBufferSize()
        {
        return m_nGroupBufferSize;
        }

    /**
     * Set the group buffer size.
     *
     * @param nSize  the group buffer size
     *
     * @return this object
     */
    public DefaultClusterDependencies setGroupBufferSize(int nSize)
        {
        m_nGroupBufferSize = nSize;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public InetAddress getGroupInterface()
        {
        InetAddress addrIf = m_groupInterface;
        if (addrIf == null)
            {
            m_groupInterface = addrIf = getLocalDiscoveryAddress();
            }
        return addrIf;
        }

    /**
     * Set the group interface InetAddress.
     *
     * @param addr  the group interface InetAddress
     *
     * @return this object
     */
    public DefaultClusterDependencies setGroupInterface(InetAddress addr)
        {
        m_groupInterface = addr;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getGroupListenerPriority()
        {
        return m_nGroupListenerPriority;
        }

    /**
     * Set the group listener priority.
     *
     * @param nPriority  the group listener priority
     *
     * @return this object
     */
    public DefaultClusterDependencies setGroupListenerPriority(int nPriority)
        {
        m_nGroupListenerPriority = nPriority;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getGroupPort()
        {
        return m_gGroupPort;
        }

    /**
     * Set the group port.
     *
     * @param nPort  the group port
     *
     * @return this object
     */
    public DefaultClusterDependencies setGroupPort(int nPort)
        {
        m_gGroupPort = nPort;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getGroupTimeToLive()
        {
        return m_cGroupTimeToLive;
        }

    /**
     * Set the group time to live.
     *
     * @param cHops  number of hops
     *
     * @return this object
     */
    public DefaultClusterDependencies setGroupTimeToLive(int cHops)
        {
        m_cGroupTimeToLive = cHops;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getGuardTimeoutMillis()
        {
        return m_cGuardTimeout;
        }

    /**
     * Set the guardian timeout.
     *
     * @param cTimeout  the guardian timeout
     *
     * @return this object
     */
    public DefaultClusterDependencies setGuardTimeoutMillis(long cTimeout)
        {
        m_cGuardTimeout = cTimeout;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getIpMonitorAttempts()
        {
        return m_cIpMonitorAttempts;
        }

    /**
     * Set the IpMonitor attempts.
     *
     * @param cAttempts  IpMonitor attempts
     *
     * @return this object
     */
    public DefaultClusterDependencies setIpMonitorAttempts(int cAttempts)
        {
        m_cIpMonitorAttempts = cAttempts;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getIpMonitorPriority()
        {
        return m_nIpMonitorPriority;
        }

    /**
     * Set the IpMonitor priority.
     *
     * @param nPriority  IpMonitor priority
     *
     * @return this object
     */
    public DefaultClusterDependencies setIpMonitorPriority(int nPriority)
        {
        m_nIpMonitorPriority = nPriority;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getIpMonitorTimeoutMillis()
        {
        return m_cMillisIpMonitorTimeout;
        }

    /**
     * Set the IpMonitor timeout.
     *
     * @param cMillis  IpMonitor timeout
     *
     * @return this object
     */
    public DefaultClusterDependencies setIpMonitorTimeoutMillis(long cMillis)
        {
        m_cMillisIpMonitorTimeout = cMillis;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public InetAddress getLocalAddress()
        {
        InetAddress addr = m_localAddress;
        if (addr == null)
            {
            InetAddress addrDiscovery = getLocalDiscoveryAddress();
            if (!addrDiscovery.isAnyLocalAddress())
                {
                return addrDiscovery;
                }

            AddressProvider providerWka = getWellKnownAddresses();
            if (providerWka != null)
                {
                // The user has not specified an explicit address, but has specified a WKA list.
                // Here we're willing to use a local hit from the WKA list, but only if it is
                // at true subnet match.  If not at least in the same subnet then we'll defer
                // selection of our local IP until the ClusterService gets involved and can do
                // routing and NAT discovery which could influence our selection in computeLocalAddress.

                InetAddress addrPeer = null;
                for (InetSocketAddress addrWKA = providerWka.getNextAddress();
                     addrWKA != null ; addrWKA = providerWka.getNextAddress())
                    {
                    InetAddress addrWkaIP = addrWKA.getAddress();
                    if (InetAddresses.isLocalAddress(addrWkaIP))
                        {
                        return m_localAddress = addrWkaIP;
                        }
                    else if (addrPeer == null)
                        {
                        addrPeer = InetAddresses.getLocalPeer(addrWkaIP);
                        }
                    }

                return m_localAddress = addrPeer;
                }
            }

        return addr;
        }

    /**
     * Compute a suggestion for the local address based on the available configuration.
     *
     * @return a suggested local address
     */
    public InetAddress computeLocalAddress()
        {
        InetAddress addr = m_localAddress;
        if (addr == null)
            {
            InetAddress addrDiscovery = getLocalDiscoveryAddress();
            if (!addrDiscovery.isAnyLocalAddress())
                {
                return addrDiscovery;
                }

            try
                {
                final AddressProvider  providerWka = getWellKnownAddresses();
                if (providerWka == null)
                    {
                    addr = InetAddresses.getLocalHost();
                    }
                else
                    {
                    // select best address for routing to the WKA list
                    Predicate<InetAddress> predRouteable = InetAddresses.IsRoutable.INSTANCE;
                    do
                        {
                        final Predicate<InetAddress> predCompound = predRouteable;
                        try
                            {
                            final Collection<InetAddress> listAddrRoute = InetAddresses.getRoutes(
                                    InetAddresses.getAllLocalAddresses(), new Iterable<InetAddress>()
                                {
                                @Override
                                public Iterator<InetAddress> iterator()
                                    {
                                    return new Iterator<InetAddress>()
                                        {
                                        @Override
                                        public boolean hasNext()
                                            {
                                            return m_addrNext != null;
                                            }

                                        @Override
                                        public InetAddress next()
                                            {
                                            InetSocketAddress addrNext = m_addrNext;
                                            m_addrNext = providerWka.getNextAddress();
                                            return addrNext.getAddress();
                                            }

                                        @Override
                                        public void remove()
                                            {
                                            throw new UnsupportedOperationException();
                                            }

                                        InetSocketAddress m_addrNext = providerWka.getNextAddress();
                                        };
                                    }
                                });

                            // select the best of the routable addresses
                            addr = InetAddresses.getLocalAddress(new Predicate<InetAddress>()
                                {
                                public boolean evaluate(InetAddress addr)
                                    {
                                    return listAddrRoute.contains(addr) &&
                                            (predCompound == null || predCompound.evaluate(addr));
                                    }

                                public String toString()
                                    {
                                    return listAddrRoute.toString() + (predCompound == null ? "" : (" and " + predCompound));
                                    }
                                });
                            }
                        catch (UnknownHostException e)
                            {
                            if (predRouteable == null)
                                {
                                throw e;
                                }
                            else
                                {
                                predRouteable = null; // try again but be willing to use loopback
                                }
                            }
                        }
                    while (addr == null);
                    }
                }
            catch (UnknownHostException e)
                {
                throw new IllegalStateException(e);
                }
            }
        return addr;
        }

    /**
     * Set the local InetAddress.
     *
     * @param addr  the local InetAddress
     *
     * @return this object
     */
    public DefaultClusterDependencies setLocalAddress(InetAddress addr)
        {
        m_localAddress = addr;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getLocalBufferSize()
        {
        return m_cLocalBufferSize;
        }

    /**
     * Set the local buffer size.
     *
     * @param cPackets  the number of packets that the local buffer can hold
     *
     * @return this object
     */
    public DefaultClusterDependencies setLocalBufferSize(int cPackets)
        {
        m_cLocalBufferSize = cPackets;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getLocalListenerPriority()
        {
        return m_nLocalListenerPriority;
        }

    /**
     * Set the local listener priority.
     *
     * @param nPriority  the local listener priority
     *
     * @return this object
     */
    public DefaultClusterDependencies setLocalListenerPriority(int nPriority)
        {
        m_nLocalListenerPriority = nPriority;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getLocalPort()
        {
        return m_nLocalPort;
        }

    /**
     * Set the local port.
     *
     * @param nPort  the local port
     *
     * @return this object
     */
    public DefaultClusterDependencies setLocalPort(int nPort)
        {
        m_nLocalPort = nPort;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isLocalPortAutoAdjust()
        {
        return m_nLocalPortAutoAdjust != 0;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getLocalPortAutoAdjust()
        {
        return m_nLocalPortAutoAdjust;
        }

    /**
     * Set the local port auto-adjust flag.
     *
     * @param fPortAuto  the local port auto-adjust flag
     *
     * @return this object
     */
    public DefaultClusterDependencies setLocalPortAutoAdjust(boolean fPortAuto)
        {
        m_nLocalPortAutoAdjust = fPortAuto ? 65535 : 0;
        return this;
        }

    /**
     * Set the local port auto-adjust flag.
     *
     * @param nPortAuto  the maximum local port to auto-adjust to
     *
     * @return this object
     */
    public DefaultClusterDependencies setLocalPortAutoAdjust(int nPortAuto)
        {
        m_nLocalPortAutoAdjust = nPortAuto;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getLostPacketThreshold()
        {
        return m_cLostPacketThreshold;
        }

    /**
     * Set the lost packet threshold count.
     *
     * @param cPackets  the lost packet threshold count
     *
     * @return this object
     */
    public DefaultClusterDependencies setLostPacketThreshold(int cPackets)
        {
        m_cLostPacketThreshold = cPackets;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public DefaultMemberIdentity getMemberIdentity()
        {
        return m_memberIdentity;
        }

    /**
     * Set the MemberIdentity.
     *
     * @param identity  the MemberIdentity.
     *
     * @return this object
     */
    public DefaultClusterDependencies setMemberIdentity(MemberIdentity identity)
        {
        m_memberIdentity = new DefaultMemberIdentity(identity)
            {
            @Override
            public String getClusterName()
                {
                String sName = super.getClusterName();
                if (sName == null || sName.isEmpty())
                    {
                    sName = makeClusterName();
                    Logger.log("The cluster name has not been configured, a value of \"" + sName + "\" has been automatically generated",
                            getMode() == LICENSE_MODE_PRODUCTION ? Logger.WARNING : Logger.INFO);
                    setClusterName(sName);
                    }
                return sName;
                }

            @Override
            public String getMachineName()
                {
                String sMachineName = super.getMachineName();
                if (sMachineName == null)
                    {
                    super.setMachineName(sMachineName = makeMachineName());
                    }
                return sMachineName;
                }

            @Override
            public int getMachineId()
                {
                int nMachineId = super.getMachineId();
                if (nMachineId == 0)
                    {
                    setMachineId(nMachineId = makeMachineId());
                    }
                return nMachineId;
                }

            @Override
            public String getSiteName()
                {
                String sSiteName = super.getSiteName();
                if (sSiteName == null)
                    {
                    super.setSiteName(sSiteName = makeSiteName());
                    }
                return sSiteName;
                }

            /**
             * Make a machine identifier.
             *
             * @return the machine identifier
             */
            protected int makeMachineId()
                {
                String sId = getMachineName();
                if (sId == null || (sId.length() == MEMBER_IDENTITY_LIMIT && sId.equals(makeMachineName())))
                    {
                    sId = null; // don't use potentially truncated default machine names for id generation if we fall through
                    try
                        {
                        // for EL the physical machine is identified via the ExaManager, as relying on the IP would not allow us
                        // to differentiate multiple OVM images on the same physical hardware
                        Class clz = Class.forName("com.oracle.exalogic.ExaManager");
                        sId = (String) ClassHelper.invoke(clz, ClassHelper.invokeStatic(clz, "instance", null), "getHostUUID",
                                null);

                        if (Character.isDigit(sId.charAt(0)))
                            {
                            try
                                {
                                return Integer.parseInt(sId) & 0x0000FFFF;
                                }
                            catch (NumberFormatException e) {}
                            }
                        }
                    catch (Throwable e) {}

                    if (sId == null)
                        {
                        sId = computeLocalAddress().getHostAddress();
                        }
                    }

                return sId.hashCode() & 0x0000FFFF;
                }
            };

        m_memberIdentity.setDependencies(this);
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMode()
        {
        return m_nMode;
        }

    /**
     * Set the license mode.
     *
     * @param nMode  the license mode
     *
     * @return this object
     */
    public DefaultClusterDependencies setMode(int nMode)
        {
        m_nMode = nMode;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getOutstandingPacketMaximum()
        {
        return m_cOutstandingPacketMaximum;
        }

    /**
     * Set the outstanding maximum packets.
     *
     * @param cPackets  the outstanding maximum packets
     *
     * @return this object
     */
    public DefaultClusterDependencies setOutstandingPacketMaximum(int cPackets)
        {
        m_cOutstandingPacketMaximum = cPackets;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getOutstandingPacketMinimum()
        {
        return m_cOutstandingPacketMinimum;
        }

    /**
     * Set the outstanding minimum packets.
     *
     * @param cPackets  the outstanding minimum packets
     *
     * @return this object
     */
    public DefaultClusterDependencies setOutstandingPacketMinimum(int cPackets)
        {
        m_cOutstandingPacketMinimum = cPackets;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getPacketBundlingAggression()
        {
        return m_dPacketBundlingAggression;
        }

    /**
     * Set the packet bundling aggression.
     *
     * @param dAggression  the packet bundling aggression.
     *
     * @return this object
     */
    public DefaultClusterDependencies setPacketBundlingAggression(double dAggression)
        {
        m_dPacketBundlingAggression = dAggression;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getPacketBundlingThresholdNanos()
        {
        return m_cPacketBundlingThresholdNanos;
        }

    /**
     * Set the packet bundling threshold.
     *
     * @param cNanos  the packet bundling threshold.
     *
     * @return this object
     */
    public DefaultClusterDependencies setPacketBundlingThresholdNanos(long cNanos)
        {
        m_cPacketBundlingThresholdNanos = cNanos;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPacketMaxLength()
        {
        return m_cbPacketMaxLength;
        }

    /**
     * Set the packet maximum length.
     *
     * @param cbMax  the packet maximum length
     *
     * @return this object
     */
    public DefaultClusterDependencies setPacketMaxLength(int cbMax)
        {
        m_cbPacketMaxLength = cbMax;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPacketPreferredLength()
        {
        return m_cbPacketPreferredLength;
        }

    /**
     * Set the packet preferred length.
     *
     * @param cb  the packet preferred length
     *
     * @return this object
     */
    public DefaultClusterDependencies setPacketPreferredLength(int cb)
        {
        m_cbPacketPreferredLength = cb;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPublisherAckDelayMillis()
        {
        return m_cPublisherAckDelay;
        }

    /**
     * Set the publisher acknowledgment delay.
     *
     * @param cMillis  the publisher acknowledgment delay.
     *
     * @return this object
     */
    public DefaultClusterDependencies setPublisherAckDelayMillis(int cMillis)
        {
        m_cPublisherAckDelay = cMillis;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPublisherSocketBufferSize()
        {
        return m_cPublisherSocketBufferSize;
        }

    /**
     * Set the publisher buffer size.
     *
     * @param cPackets  the publisher buffer size
     *
     * @return this object
     */
    public DefaultClusterDependencies setPublisherSocketBufferSize(int cPackets)
        {
        m_cPublisherSocketBufferSize = cPackets;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPublisherCloggedCount()
        {
        return m_cPublisherCloggedCount;
        }

    /**
     * Set the number of packets in send and resend queues tolerated before
     * considering the queues clogged.
     *
     * @param cMax  the number of packets
     *
     * @return this object
     */
    public DefaultClusterDependencies setPublisherCloggedCount(int cMax)
        {
        m_cPublisherCloggedCount = cMax;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPublisherCloggedDelayMillis()
        {
        return m_cPublisherCloggedDelayMillis;
        }

    /**
     * Set the number of milliseconds that the Publisher will pause a client thread that
     * is trying to send a message when the Publisher is clogged.
     *
     * @param cMillis  the clogged delay milliseconds
     *
     * @return this object
     */
    public DefaultClusterDependencies setPublisherCloggedDelayMillis(int cMillis)
        {
        m_cPublisherCloggedDelayMillis = cMillis;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPublisherGroupThreshold()
        {
        return m_cPublisherGroupThreshold;
        }

    /**
     * Set the publisher group threshold percent.
     *
     * @param cPercent  the publisher group threshold percent
     *
     * @return this object
     */
    public DefaultClusterDependencies setPublisherGroupThreshold(int cPercent)
        {
        m_cPublisherGroupThreshold = cPercent;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPublisherNackDelayMillis()
        {
        return m_cPublisherNackDelay;
        }

    /**
     * Set the publisher Nack delay.
     *
     * @param cMillis  the publisher Nack delay
     *
     * @return this object
     */
    public DefaultClusterDependencies setPublisherNackDelayMillis(int cMillis)
        {
        m_cPublisherNackDelay = cMillis;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPublisherPriority()
        {
        return m_nPublisherPriority;
        }

    /**
     * Set the publisher priority.
     *
     * @param nPriority  the publisher priority
     *
     * @return this object
     */
    public DefaultClusterDependencies setPublisherPriority(int nPriority)
        {
        m_nPublisherPriority = nPriority;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPublisherResendDelayMillis()
        {
        return m_cPublisherResendDelayMillis;
        }

    /**
     * Set the publisher resend delay.
     *
     * @param cMillis  the publisher resend delay
     *
     * @return this object
     */
    public DefaultClusterDependencies setPublisherResendDelayMillis(int cMillis)
        {
        m_cPublisherResendDelayMillis = cMillis;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPublisherResendTimeoutMillis()
        {
        return m_cPublisherResendTimeoutMillis;
        }

    /**
     * Set the publisher resend timeout.
     *
     * @param cMillis  the publisher resend timeout
     *
     * @return this object
     */
    public DefaultClusterDependencies setPublisherResendTimeoutMillis(int cMillis)
        {
        m_cPublisherResendTimeoutMillis = cMillis;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isReceiverNackEnabled()
        {
        return m_fReceiverNackEnabled;
        }

    /**
     * Set the receiver Nack enabled flag.
     *
     * @param fEnabled  true to enable receiver Nack
     *
     * @return this object
     */
    public DefaultClusterDependencies setReceiverNackEnabled(boolean fEnabled)
        {
        m_fReceiverNackEnabled = fEnabled;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getReceiverPriority()
        {
        return m_nReceiverPriority;
        }

    /**
     * Set the receiver priority.
     *
     * @param nPriority  the receiver priority
     *
     * @return this object
     */
    public DefaultClusterDependencies setReceiverPriority(int nPriority)
        {
        m_nReceiverPriority = nPriority;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getReliableTransport()
        {
        return m_sReliableTransport;
        }

    /**
     * Set the reliable transport name.
     *
     * @param sName  the reliable transport name
     *
     * @return this object
     */
    public DefaultClusterDependencies setReliableTransport(String sName)
        {
        m_sReliableTransport = sName;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public Map<String, SerializerFactory> getSerializerMap()
        {
        Map<String, SerializerFactory> mapSerializer = m_mapSerializer;
        if (m_mapSerializer == null)
            {
            mapSerializer = m_mapSerializer = new LiteMap();
            }

        if (mapSerializer.isEmpty())
            {
            for (ParameterizedBuilderRegistry.Registration reg : getBuilderRegistry())
                {
                if (reg.getInstanceClass().isAssignableFrom(Serializer.class))
                    {
                    mapSerializer.put(reg.getName(), new SerializerFactory()
                        {
                        @Override
                        public Serializer createSerializer(ClassLoader loader)
                            {
                            Serializer serializer = (Serializer) reg.getBuilder().realize(
                                    new NullParameterResolver(), loader, null);
                            if (serializer instanceof ClassLoaderAware)
                                {
                                ((ClassLoaderAware) serializer).setContextClassLoader(loader);
                                }
                            return serializer;
                            }

                        @Override
                        public String getName()
                            {
                            return reg.getName();
                            }
                        });
                    }
                }

            discoverSerializers();
            }

        return mapSerializer;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, SnapshotArchiverFactory> getSnapshotArchiverMap()
        {
        Map<String, SnapshotArchiverFactory> mapSnapshotArchiver = m_mapSnapshotArchiver;
        if (mapSnapshotArchiver == null)
            {
            m_mapSnapshotArchiver = mapSnapshotArchiver = new LiteMap();
            }
        return mapSnapshotArchiver;
        }

    /**
     * Set the snapshot archiver map.
     *
     * @param mapSnapshotArchiver the snapshot archiver map
     *
     * @return this object
     */
    public DefaultClusterDependencies setSnapshotArchiverMap(Map<String, SnapshotArchiverFactory> mapSnapshotArchiver)
        {
        m_mapSnapshotArchiver = mapSnapshotArchiver;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, AddressProviderFactory> getAddressProviderMap()
        {
        Map<String, AddressProviderFactory> mapAddressProvider = m_mapAddressProvider;
        if (mapAddressProvider == null)
            {
            m_mapAddressProvider = mapAddressProvider = new LiteMap();
            }
        return mapAddressProvider;
        }

    /**
     * Set the address provider map.
     *
     * @param mapAddressProvider  the address provider map
     *
     * @return this object
     */
    public DefaultClusterDependencies setAddressProviderMap(Map<String, AddressProviderFactory> mapAddressProvider)
        {
        m_mapAddressProvider = mapAddressProvider;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public ServiceFailurePolicyBuilder getServiceFailurePolicyBuilder()
        {
        return m_bldrServiceFailurePolicy;
        }

    /**
     * Set the service failure policy builder.
     *
     * @param builder  the service failure policy builder
     *
     * @return this object
     */
    public DefaultClusterDependencies setServiceFailurePolicyBuilder(ServiceFailurePolicyBuilder builder)
        {
        m_bldrServiceFailurePolicy = builder;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public Map<String, List<String>> getServiceFilterMap()
        {
        Map<String, List<String>> mapServiceFilter = m_mapServiceFilter;
        if (mapServiceFilter == null)
            {
            m_mapServiceFilter = mapServiceFilter = new LiteMap();
            }
        return mapServiceFilter;
        }

    /**
     * Set the service filter map.
     *
     * @param mapServiceFilter  the service filter map
     *
     * @return this object
     */
    public DefaultClusterDependencies setServiceFilterMap(Map<String, List<String>> mapServiceFilter)
        {
        m_mapServiceFilter = mapServiceFilter;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public Map<String, String>  getServiceMap()
        {
        Map<String, String> mapService = m_mapService;
        if (mapService == null)
            {
            m_mapService = mapService = new LiteMap();
            }
        return mapService;
        }

    /**
     * Set the service map.
     *
     * @param mapService  the service map
     *
     * @return this object
     */
    public DefaultClusterDependencies setServiceMap(Map<String, String>  mapService)
        {
        m_mapService = mapService;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getShutdownHookOption()
        {
        return m_nShutdownHookOption;
        }

    /**
     * Set the shutdown hook option.
     *
     * @param nOption  the shutdown hook option
     *
     * @return this object
     */
    public DefaultClusterDependencies setShutdownHookOption(int nOption)
        {
        m_nShutdownHookOption = nOption;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public SocketProviderFactory getSocketProviderFactory()
        {
        SocketProviderFactory socketProviderFactory = m_socketProviderFactory;
        if (socketProviderFactory == null)
            {
            m_socketProviderFactory = socketProviderFactory = new SocketProviderFactory();
            }
        return socketProviderFactory;
        }

    /**
     * Set the socket provider factory.
     *
     * @param factory  the socket provider factory
     *
     * @return this object
     */
    public DefaultClusterDependencies setSocketProviderFactory(SocketProviderFactory factory)
        {
        m_socketProviderFactory = factory;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Deprecated
    @Override
    public XmlElement getUnicastSocketProviderXml()
        {
        return m_xmlUnicastSocketProvider;
        }

    /**
     * Set the unicast socket provider xml.
     *
     * @param xmlProvider  the unicast socket provider xml
     *
     * @return this object
     */
    @Deprecated
    public DefaultClusterDependencies setUnicastSocketProviderXml(XmlElement xmlProvider)
        {
        m_xmlUnicastSocketProvider = xmlProvider;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public SocketProviderBuilder getUnicastSocketProviderBuilder()
        {
        return m_builderUnicastSocketProvider;
        }

    /**
     * Set the unicast socket provider builder.
     *
     * @param builder  unicast {@link SocketProviderBuilder}
     *
     * @return this object
     */
    @Injectable("socket-provider")
    public DefaultClusterDependencies setUnicastSocketProviderBuilder(SocketProviderBuilder builder)
        {
        m_builderUnicastSocketProvider = builder;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getSpeakerPriority()
        {
        return m_nSpeakerPriority;
        }

    /**
     * Set the socket provider priority.
     *
     * @param nPriority  the socket provider priority
     *
     * @return this object
     */
    public DefaultClusterDependencies setSpeakerPriority(int nPriority)
        {
        m_nSpeakerPriority = nPriority;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getSpeakerVolumeMinimum()
        {
        return m_cSpeakerVolumeMinimum;
        }

    /**
     * Set the socket volume minimum.
     *
     * @param cVolume  the socket volume minimum
     *
     * @return this object
     */
    public DefaultClusterDependencies setSpeakerVolumeMinimum(int cVolume)
        {
        m_cSpeakerVolumeMinimum = cVolume;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    public boolean isSpeakerEnabled()
        {
        return m_fSpeakerEnabled;
        }

    /**
     * Set the Speaker enabled flag.
     *
     * @param fEnabled  true to enable the speaker.
     *
     * @return this object
     */
    public DefaultClusterDependencies setSpeakerEnabled(boolean fEnabled)
        {
        m_fSpeakerEnabled = fEnabled;

        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isTcmpEnabled()
        {
        return m_fTcmpEnabled;
        }

    /**
     * Set the TCMP enabled flag.
     *
     * @param fEnabled  true to enable TCMP.
     *
     * @return this object
     */
    public DefaultClusterDependencies setTcmpEnabled(boolean fEnabled)
        {
        m_fTcmpEnabled = fEnabled;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getTcpBacklog()
        {
        return m_cTcpBacklog;
        }

    /**
     * Set the TCP listener backlog.
     *
     * @param cPeers  the TCP listener backlog.
     *
     * @return this object
     */
    public DefaultClusterDependencies setTcpBacklog(int cPeers)
        {
        m_cTcpBacklog = cPeers;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public SocketOptions getTcpDatagramSocketOptions()
        {
        return m_optionsTcpDatagram;
        }

    /**
     * Set the TCP datagram socket options.
     *
     * @param options  the TCP datagram socket options.
     *
     * @return this object
     */
    public DefaultClusterDependencies setTcpDatagramSocketOptions(SocketOptions options)
        {
        m_optionsTcpDatagram = options;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public SocketOptions getTcpRingSocketOptions()
        {
        return m_optionsTcpRing;
        }

    /**
     * Set the TCP-Ring socket options.
     *
     * @param options  the TCP-Ring socket options.
     *
     * @return this object
     */
    public DefaultClusterDependencies setTcpRingSocketOptions(SocketOptions options)
        {
        m_optionsTcpRing = options;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isTcpRingEnabled()
        {
        return m_fTcpRingEnabled;
        }

    /**
     * Set the TCP-Ring enabled flag.
     *
     * @param fEnabled  true to enable TCP-Ring.
     *
     * @return this object
     */
    public DefaultClusterDependencies setTcpRingEnabled(boolean fEnabled)
        {
        m_fTcpRingEnabled = fEnabled;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public AddressProvider getWellKnownAddresses()
        {
        return m_providerWellKnownAddresses;
        }

    /**
     * Set the address provider.
     *
     * @param provider  the address provider
     *
     * @return this object
     */
    public DefaultClusterDependencies setWellKnownAddresses(AddressProvider provider)
        {
        m_providerWellKnownAddresses = provider;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isIpMonitorEnabled()
        {
        return getIpMonitorTimeoutMillis() * getIpMonitorAttempts() > 0L;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public ParameterizedBuilderRegistry getBuilderRegistry()
        {
        return m_builderRegistry;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerResources(ResourceRegistry registry)
        {
        if (m_customResources != null)
            {
            m_customResources.registerResources(registry, RegistrationBehavior.FAIL);
            }
        }

    /**
     * Set the registry of custom resources.
     *
     * @param registry  the registry of custom resources
     */
    public void setCustomResourcesRegistry(SimpleResourceRegistry registry)
        {
        m_customResources = registry;
        }

    /**
     * Register a {@link ServiceProvider} that can create new instances of local
     * (non-clustered) services.
     *
     * @param sType     the type of the service provided
     * @param provider  the {@link ServiceProvider} instance
     */
    @Override
    public void addLocalServiceProvider(String sType, ServiceProvider provider)
        {
        Objects.requireNonNull(sType);
        if (provider != null)
            {
            m_mapLocalServiceProvider.put(sType, provider);
            }
        else
            {
            m_mapLocalServiceProvider.remove(sType);
            }
        }

    /**
     * Obtain a {@link ServiceProvider} that can build an instance
     * of a given service type.
     *
     * @param sType  the service type
     *
     * @return a {@link ServiceProvider} that can build an instance
     *         of a given service type
     */
    @Override
    public ServiceProvider getLocalServiceProvider(String sType)
        {
        return m_mapLocalServiceProvider.getOrDefault(sType, ServiceProvider.NULL_IMPLEMENTATION);
        }

    @Override
    public String getLambdasSerializationMode()
        {
        return m_sLambdasSerializationMode;
        }

    /**
     * Set lambdas serialization mode.
     *
     * @param sMode  either "static", "dynamic" or ""
     *
     * @return this object
     */
    public DefaultClusterDependencies setLambdasSerializationMode(String sMode)
        {
        m_sLambdasSerializationMode = sMode;
        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isVirtualThreadsEnabled()
        {
        return m_fVirtualThreadsEnabled;
        }

    /**
     * Set the virtual-threads-enabled flag.
     *
     * @param fEnabled  true to enable the using of virtual threads.
     *
     * @return this object
     *
     * @since 24.03
     */
    public DefaultClusterDependencies setVirtualThreadsEnabled(boolean fEnabled)
        {
        m_fVirtualThreadsEnabled = fEnabled;
        return this;
        }


    // ----- DefaultClusterDependencies methods -----------------------------

    /**
     * Validate the supplied dependencies.
     *
     * @throws IllegalArgumentException if the dependencies are not valid
     *
     * @return this object
     */
    public DefaultClusterDependencies validate()
        {
        Base.checkNotNull(getMemberIdentity(), "MemberIdentity");
        getMemberIdentity().validate();

        validateGuardTimeout();

        Base.checkNotNull(getServiceMap(), "Services");
        Base.checkNotNull(getFilterMap(), "FilterMap");
        Base.checkNotNull(getFilterList(), "FilterList");
        Base.checkNotNull(getLocalDiscoveryAddress(), "LocalDiscoveryAddress");
        Base.checkNotNull(getMemberIdentity(), "MemberIdentity");
        Base.checkNotNull(getSerializerMap(), "SerializerMap");
        Base.checkNotNull(getAddressProviderMap(), "AddressProviderMap");
        Base.checkNotNull(getServiceFailurePolicyBuilder(), "ServiceFailurePolicyBuilder");
        Base.checkNotNull(getBuilderRegistry(), "BuilderRegistry");
        Base.checkNotNull(getServiceFilterMap(), "ServiceFilterMap");
        Base.checkNotNull(getServiceMap(), "ServiceMap");
        Base.checkNotNull(getSocketProviderFactory(), "SocketProviderFactory");

        Base.checkRange(getClusterAnnounceTimeoutMillis(), 0, 1000000, "ClusterAnnounceTimeoutMillis");
        Base.checkRange(getClusterHeartbeatDelayMillis(), 0, 60000, "ClusterHeartbeatDelayMillis");
        Base.checkRange(getClusterTimestampMaxVarianceMillis(), 1, 1000, "ClusterTimestampMaxVarianceMillis");
        Base.checkRange(getGroupListenerPriority(), Thread.MIN_PRIORITY, Thread.MAX_PRIORITY, "GroupListenerPriority");
        Base.checkRange(getGroupPort(), 1, 65536, "GroupPort");
        Base.checkRange(getGroupTimeToLive(), 0, 255, "GroupTimeToLive");
        Base.checkRange(getIpMonitorPriority(), Thread.MIN_PRIORITY, Thread.MAX_PRIORITY, "IpMonitorPriority");
        Base.checkRange(getLocalListenerPriority(), Thread.MIN_PRIORITY, Thread.MAX_PRIORITY, "LocalListenerPriority");
        Base.checkRange(getLocalPort(), 0, 65535, "LocalPort");
        Base.checkRange(getPacketMaxLength(), 0, 65535, "PacketMaxLength");
        Base.checkRange(getPacketPreferredLength(), 0, 65535, "PacketPreferredLength");
        Base.checkRange(getPublisherAckDelayMillis(), 0, 60000, "PublisherAckDelayMillis");
        Base.checkRange(getPublisherCloggedDelayMillis(), 0, 60000, "PublisherCloggedDelayMillis");
        Base.checkRange(getPublisherGroupThreshold(), 0, 100, "PublisherGroupThreshold");
        Base.checkRange(getPublisherPriority(), Thread.MIN_PRIORITY, Thread.MAX_PRIORITY, "PublisherPriority");
        Base.checkRange(getPublisherResendDelayMillis(), 0, 60000, "PublisherResendDelayMillis");
        Base.checkRange(getPublisherResendTimeoutMillis(), 0, 1000000, "PublisherResendTimeoutMillis");
        Base.checkRange(getReceiverPriority(), Thread.MIN_PRIORITY, Thread.MAX_PRIORITY, "ReceiverPriority");
        Base.checkRange(getSpeakerPriority(), Thread.MIN_PRIORITY, Thread.MAX_PRIORITY, "SpeakerPriority");

        return this;
        }

    // ----- Object methods -------------------------------------------------

    /**
     * Return the DefaultClusterDependencies in String format.
     *
     * @return the DefaultClusterDependencies in String format
     */
    @Override
    public String toString()
        {
        return "DefaultClusterDependencies{"
            + "\n\tAuthorizedHostFilter          = " + getAuthorizedHostFilter()
            + "\n\tClusterActionPolicyBuilder    = " + getClusterActionPolicyBuilder()
            + "\n\tBuilderRegistry               = " + getBuilderRegistry()
            + "\n\tClusterAnnounceTimeout        = " + getClusterAnnounceTimeoutMillis()
            + "\n\tClusterHeartbeatDelay         = " + getClusterHeartbeatDelayMillis()
            + "\n\tClusterTimestampMaxVariance   = " + getClusterTimestampMaxVarianceMillis()
            + "\n\tEdition                       = " + getEdition()
            + "\n\tFilterList                    = " + getFilterList()
            + "\n\tFilterMap                     = " + getFilterMap()
            + "\n\tServiceFilterMap              = " + getServiceFilterMap()
            + "\n\tGroupAddress                  = " + getGroupAddress()
            + "\n\tGroupBufferSize               = " + getGroupBufferSize()
            + "\n\tGroupInterface                = " + getGroupInterface()
            + "\n\tGroupTimeToLive               = " + getGroupTimeToLive()
            + "\n\tGuardTimeout                  = " + getGuardTimeoutMillis()
            + "\n\tIpMonitorAttempts             = " + getIpMonitorAttempts()
            + "\n\tIpMonitorPriority             = " + getIpMonitorPriority()
            + "\n\tLocalAddress                  = " + getLocalAddress()
            + "\n\tLocalBufferSize               = " + getLocalBufferSize()
            + "\n\tLocalListenerPriority         = " + getLocalListenerPriority()
            + "\n\tLocalPort                     = " + getLocalPort()
            + "\n\tLostPacketThreshold           = " + getLostPacketThreshold()
            + "\n\tMemberIdentity                = " + getMemberIdentity()
            + "\n\tMode                          = " + getMode()
            + "\n\tOutstandingPacketMaximum      = " + getOutstandingPacketMaximum()
            + "\n\tOutstandingPacketMinimum      = " + getOutstandingPacketMinimum()
            + "\n\tPacketBundlingAggression      = " + getPacketBundlingAggression()
            + "\n\tPacketBundlingThresholdNanos  = " + getPacketBundlingThresholdNanos()
            + "\n\tPacketMaxLength               = " + getPacketMaxLength()
            + "\n\tPacketPreferredLength         = " + getPacketPreferredLength()
            + "\n\tPublisherAckDelay             = " + getPublisherAckDelayMillis()
            + "\n\tPublisherSocketBufferSize     = " + getPublisherSocketBufferSize()
            + "\n\tPublisherCloggedCount         = " + getPublisherCloggedCount()
            + "\n\tPublisherCloggedDelayMillis   = " + getPublisherCloggedDelayMillis()
            + "\n\tPublisherGroupThreshold       = " + getPublisherGroupThreshold()
            + "\n\tPublisherNackDelay            = " + getPublisherNackDelayMillis()
            + "\n\tPublisherPriority             = " + getPublisherPriority()
            + "\n\tPublisherResendDelayMillis    = " + getPublisherResendDelayMillis()
            + "\n\tPublisherResendTimeoutMillis  = " + getPublisherResendTimeoutMillis()
            + "\n\tReceiverPriority              = " + getReceiverPriority()
            + "\n\tReliableTransport             = " + getReliableTransport()
            + "\n\tServiceFailurePolicyBuilder   = " + getServiceFailurePolicyBuilder()
            + "\n\tShutdownHookOption            = " + getShutdownHookOption()
            + "\n\tSocketProviderFactory         = " + getSocketProviderFactory()
            + "\n\tServiceMap                    = " + getServiceMap()
            + "\n\tSerializerMap                 = " + getSerializerMap()
            + "\n\tAddressProviderMap            = " + getAddressProviderMap()
            + "\n\tUnicastSocketProviderXml      = " + getUnicastSocketProviderXml()
            + "\n\tUnicastSocketProviderBuilder  = " + getUnicastSocketProviderBuilder()
            + "\n\tSpeakerPriority               = " + getSpeakerPriority()
            + "\n\tSpeakerVolumeMinimum          = " + getSpeakerVolumeMinimum()
            + "\n\tSpeakerEnabled                = " + isSpeakerEnabled()
            + "\n\tTcpBacklog                    = " + getTcpBacklog()
            + "\n\tTcpDatagramSocketOptions      = " + getTcpDatagramSocketOptions()
            + "\n\tTcpRingSocketOptions          = " + getTcpRingSocketOptions()
            + "\n\tWellKnownAddresses            = " + getWellKnownAddresses()
            + "\n\tFlowControlEnabled            = " + isFlowControlEnabled()
            + "\n\tIpMonitorEnabled              = " + isIpMonitorEnabled()
            + "\n\tLocalPortAutoAdjust           = " + isLocalPortAutoAdjust()
            + "\n\tReceiverNackEnabled           = " + isReceiverNackEnabled()
            + "\n\tTcmpEnabled                   = " + isTcmpEnabled()
            + "\n\tTcpRingEnabled                = " + isTcpRingEnabled()
            + "\n\tLambdasSerialization          = " + getLambdasSerializationMode()
            + "}";
        }

    // ----- internal methods -----------------------------------------------

    /**
     * Loads {@link SerializerFactory} and {@link Serializer} (in that order)
     * implementations using the {@link ServiceLoader} mechanism.
     *
     * Implementations loaded in this fashion, <em>must</em> be annotated
     * with the {@link Named} annotation with a non-default value or it will
     * be ignored.
     *
     * Implementation class name <em>must</em> be referenced in
     * {@code META-INF/services/com.tangosol.io.SerializerFactory}.
     *
     * If a {@link SerializerFactory} or {@link Serializer} has already been
     * registered with the same name as a discovered implementation, the
     * discovered implementation will be ignored.
     *
     * @since 20.12
     */
    protected void discoverSerializers()
        {
        m_mapSerializer.putAll(Serializer.discoverSerializers());
        }

    /**
     * Validate the guardian timeout.  If is not valid then use a default value and
     * change the service guardian policy to logging.
     */
    protected void validateGuardTimeout()
        {
        if (getGuardTimeoutMillis() <= 0)
            {
            // this fixes COH-3090: ensure that the guardian cannot be turned off entirely
            setGuardTimeoutMillis(getPublisherResendTimeoutMillis());
            setServiceFailurePolicyBuilder(new ServiceFailurePolicyBuilder(DefaultServiceFailurePolicy.POLICY_LOGGING));

            Logger.warn("Disabling the service-guardian by setting a timeout of 0 has "
                + "been deprecated. Instead, please configure a "
                + "\"service-failure-policy\" of \"logging\" which will perform "
                + "non-invasive monitoring of Coherence services.\n Configuring "
                + "the Guardian to use the \"logging\" policy with a timeout of "
                + getGuardTimeoutMillis() + "ms");
            }
        }

    /**
     * Make the cluster name.
     *
     * @return the cluster name
     */
    protected String makeClusterName()
        {
        // Set default cluster name to the user name
        String sName = AccessController.doPrivileged((PrivilegedAction<String>) () -> System.getProperty("user.name"));

        if (sName != null)
            {
            sName = sName.trim();
            }

        if (sName == null || sName.isEmpty() || sName.equals("?")) // disconnected NIS appears to return ? on failed lookup
            {
            // we can't obtain the user name, this could be a transient error for instance in the case of NIS.
            // while we could generate some random or fixed default that wouldn't defend well against transient errors
            // and we could end up with multiple clusters.  Given that any production system should actually set the
            // cluster name rather then using a default we will treat this as a hard error.  Note don't try to obtain
            // the name by other means such as reading env variables because they may produce a different string then
            // reading "user.name" and again if the error is transient multiple clusters could be unintentionally produced.

            throw new UnsupportedOperationException(
                    "unable to generate a default cluster name, user name is not available, explicit cluster name configuration is required");
            }

        // this suffix in addition to be cute and suggesting this is not a production cluster also helps
        // minimize the possibility of a collision with a manually named cluster which would be very unlikely
        // to use such a cute name.
        return sName + "'s cluster";
        }

    /**
     * Make the machine name from the local host name.
     *
     * @return the machine name
     */
    protected String makeMachineName()
        {
        try
            {
            InetAddress addr  = getLocalAddress();
            String      sHost = addr.getCanonicalHostName();

            // if the name resolution fails then an IP address is returned
            if (!Base.equals(addr.getHostAddress(), sHost))
                {
                int ofDelim = sHost.indexOf('.');
                int cChars = ofDelim == -1 ? sHost.length() : ofDelim;

                return sHost.substring(0, Math.min(DefaultMemberIdentity.MEMBER_IDENTITY_LIMIT, cChars));
                }
            }
        catch (RuntimeException e)
            {
            // ignore
            }

        return null;
        }

    /**
     * Make the site name from the local host name.
     *
     * @return the site name
     */
    protected String makeSiteName()
        {
        try
            {
            InetAddress addr  = getLocalAddress();
            String      sHost = addr.getCanonicalHostName();

            // if the name resolution fails then an IP address is returned
            if (!Base.equals(addr.getHostAddress(), sHost))
                {
                int ofDelim = sHost.indexOf('.');
                if (ofDelim != -1)
                    {
                    int ofEnd = Math.min(sHost.length(), ofDelim + DefaultMemberIdentity.MEMBER_IDENTITY_LIMIT);
                    return sHost.substring(ofDelim + 1, ofEnd);
                    }
                }
            }
        catch (RuntimeException e)
            {
            // ignore
            }

        return null;
        }


    // ----- data members ---------------------------------------------------

    /**
     * The authorized host filter.
     */
    private Filter m_authorizedHostFilter;

    /**
     * The action policy builder used by the cluster service to control cluster
     * membership actions.
     */
    private ActionPolicyBuilder m_bldrClusterActionPolicy;

    /**
     * The cluster announce timeout.
     */
    private int m_cClusterAnnounceTimeout = 3000;

    /**
     * The cluster heartbeat delay.
     */
    private int m_cClusterHeartbeatDelay;

    /**
     * The cluster timestamp variance.
     */
    private int m_cClusterTimestampMaxVariance = 16;

    /**
     * The Edition is the product type.
     */
    private int m_nEdition = EDITION_GRID;

    /**
     * The list of network filters.
     */
    private List<String> m_listFilter;

    /**
     * Map from filter name to WrapperStreamFactory instance.
     */
    private Map<String, WrapperStreamFactory> m_mapFilter;

    /**
     * The flag indicating if flow control is enabled.
     */
    private boolean m_fFlowControlEnabled = true;

    /**
     * The local discovery address.
     */
    private InetAddress m_discAddressLocal;

    /**
     * The default value for MulticastSocket group address.
     */
    protected final String DEFAULT_ADDR = "239.192.0.0";

    /**
     * The IP address that the MulticastSocket will listen/publish on.
     */
    private InetAddress m_groupAddress;

    /**
     * The size to request for the multicast datagram socket input buffer
     * (SO_RCVBUF) to be sized to.
     */
    private int m_nGroupBufferSize;

    /**
     * The IP address of the interface (e.g. the NIC) that the MulticastSocket
     * will open a socket on.
     */
    private InetAddress m_groupInterface;

    /**
     * Specifies the priority of the "group" PacketListener daemon thread.
     */
    private int m_nGroupListenerPriority = Thread.MAX_PRIORITY;

    /**
     * The port number that the MulticastSocket will listen/publish on.
     */
    private int m_gGroupPort = 7574;

    /**
     * The time-to-live setting for the MulticastSocket. Time-to-live is
     * measured in "hops".
     */
    private int m_cGroupTimeToLive = 4;

    /**
     * Specifies the priority of the TcpRingListener daemon thread.
     */
    private int m_nIpMonitorPriority = 6;

    /**
     * Specifies the number of connection attempts that the IpMonitor will use in
     * determining if IP connectivity exists to other hosts.  Each attempt has a
     * timeout of m_cMillisIpMonitorTimeout.
     */
    private int m_cIpMonitorAttempts = Platform.getPlatform().isExaEnabled()
            ? 12 /* based on known switch failover time (30-45s) for EL, note this gives us 15s of slack just list non-EL value */
            :  3 /* non-EL default */;

    /**
     * Specifies the timeout that the TCP Ring will use in determining if IP
     * connectivity exists to other hosts.
     */
    private long m_cMillisIpMonitorTimeout = 5000;

    /**
     * The IP address that the DatagramSocket will listen/publish on.
     */
    private InetAddress m_localAddress;

    /**
     * The size to request for the unicast datagram socket input buffer
     * (SO_RCVBUF) to be sized to.
     */
    private int m_cLocalBufferSize;

    /**
     * Specifies the priority of the "local" PacketListener daemon thread.
     */
    private int m_nLocalListenerPriority = Thread.MAX_PRIORITY;

    /**
     * The port number that the DatagramSocket will listen/publish on.
     */
    private int m_nLocalPort = 0;

    /**
     * Specifies the upper bound on LocalPort to use.
     */
    private int m_nLocalPortAutoAdjust = 65535;

    /**
     * The number of sequential packets which may be lost before declaring the
     * member paused, and starting to trickle packets.
     */
    private int m_cLostPacketThreshold = 16;

    /**
     * The underlying member identity portion of the configuration.
     */
    private DefaultMemberIdentity m_memberIdentity;

    /**
     * The Mode is the "license type", i.e. evaluation, development or
     * production use.
     */
    private int m_nMode = LICENSE_MODE_DEVELOPMENT;

    /**
     * The upper bound on the range for controlling how many packets are allowed
     * to be outstanding to a single cluster member.
     */
    private int m_cOutstandingPacketMaximum = 4096;

    /**
     * The lower bound on the range for controlling how many packets are allowed
     * to be outstanding to a single cluster member.
     */
    private int m_cOutstandingPacketMinimum = 64;

    /**
     * The aggression to use in deferring a packet once it has reached the
     * average bundle size.
     */
    private double m_dPacketBundlingAggression = 0.0;

    /**
     * The maximum amount of time to defer a packet while waiting for additional
     * packets to bundle.
     */
    private long m_cPacketBundlingThresholdNanos = 1000L;

    /**
     * Specifies the maximum size, in bytes, of the DatagramPacket objects that
     * will be sent and received on the local and group sockets.
     */
    private int m_cbPacketMaxLength = 65535; // default to the max allowed by UDP spec

    /**
     * Specifies the preferred ("best") size, in bytes, of the DatagramPacket
     * objects that will be sent and received on the local and group sockets.
     */
    private int m_cbPacketPreferredLength; // default to 0 to allow node to choose at runtime

    /**
     * Specifies the number of milliseconds that the Publisher will delay before
     * sending an Ack Packet.
     */
    private int m_cPublisherAckDelay = 16;

    /**
     * The size request the datagram socket output buffer (SO_SNDBUF) to be
     * sized to.
     */
    private int m_cPublisherSocketBufferSize;

    /**
     * The maximum number of packets in the send plus re-send queues that the
     * Publisher will tolerate before determining that it is clogged.
     */
    private int m_cPublisherCloggedCount = 8192;

    /**
     * Number of milliseconds that the Publisher will pause a client thread that
     * is trying to send a message when the Publisher is clogged.
     */
    private int m_cPublisherCloggedDelayMillis = 10;

    /**
     * The group threshold is used to determine whether a packet will be sent
     * via unicast or multicast.
     */
    private int m_cPublisherGroupThreshold = 100;

    /**
     * Specifies the number of milliseconds that the Publisher will delay
     * sending an NACK Packet.
     */
    private int m_cPublisherNackDelay = 1;

    /**
     * Specifies the priority of the PacketPublisher daemon thread.
     */
    private int m_nPublisherPriority = Thread.MAX_PRIORITY;

    /**
     * Specifies the minimum amount of time, in milliseconds, before a Packet's
     * data is resent across the network.
     */
    private int m_cPublisherResendDelayMillis = 200;

    /**
     * Specifies the maximum amount of time, in milliseconds, that a Packet's
     * data will be resent across the network.
     */
    private int m_cPublisherResendTimeoutMillis = 300000;

    /**
     * The guard timeout.
     */
    private long m_cGuardTimeout = m_cPublisherResendTimeoutMillis;

    /**
     * Specifies whether the PacketReceiver will use negative acknowledgments
     * (packet requests) to pro-actively respond to known missing packets.
     */
    private boolean m_fReceiverNackEnabled = true;

    /**
     * Specifies the priority of the PacketReceiver daemon thread.
     */
    private int m_nReceiverPriority = Thread.MAX_PRIORITY;

    /**
     * Name of the reliable transport used by this node.
     */
    private transient String m_sReliableTransport;

    /**
     * A map from service canonical name (AKA service type, a String) to a List
     * of filter names (String) that are used by that specific service for
     * outgoing messages.
     */
    private Map<String, List<String>> m_mapServiceFilter;

    /**
     * A map of registered services keyed by the service canonical name (AKA
     * service type) and values being the service component names.
     */
    private Map<String, String> m_mapService;

    /**
     * Map from serializer name to SerializerFactory instance.
     */
    private Map<String, SerializerFactory> m_mapSerializer;

    /**
     * Map from name to SnapshotArchiverFactory.
     */
    private Map<String, SnapshotArchiverFactory> m_mapSnapshotArchiver;

    /**
     * Map from name to AddressProviderFactory.
     */
    private Map<String, AddressProviderFactory> m_mapAddressProvider;

    /**
     * The ServiceFailure policy builder.
     */
    private ServiceFailurePolicyBuilder m_bldrServiceFailurePolicy = new ServiceFailurePolicyBuilder(DefaultServiceFailurePolicy.POLICY_EXIT_CLUSTER);

    /**
     * Specifies whether the ShutdownHook is enabled.
     */
    private int m_nShutdownHookOption = ClusterDependencies.SHUTDOWN_FORCE;

    /**
     * The SocketProviderFactory associated with the cluster.
     */
    private SocketProviderFactory m_socketProviderFactory;

    /**
     * The Cluster's Unicast SocketProvider XML configuration.
     */
    private XmlElement m_xmlUnicastSocketProvider;

    /**
     * Specifies the priority of the PacketSpeaker daemon thread.
     */
    private int m_nSpeakerPriority = Thread.MAX_PRIORITY;

    /**
     * The minimum number of packets which must be ready to be sent in order for
     * the speaker to be utilized.
     */
    private int m_cSpeakerVolumeMinimum = -1;

    /**
     * Specifies whether or not the speaker is enabled.
     */
    private boolean m_fSpeakerEnabled = true;

    /**
     * Specifies whether or not the TCMP is enabled.
     */
    private boolean m_fTcmpEnabled = true;

    /**
     * The listen backlog for TCMP's Tcp listener.
     */
    private int m_cTcpBacklog;

    /**
     * The SocketOptions to apply when TCMP runs on a TcpDatagramSocket.
     */
    private SocketOptions m_optionsTcpDatagram;

    /**
     * Specifies whether or not the TcpRing is enabled.
     */
    private boolean m_fTcpRingEnabled = true;

    /**
     * The SocketOptions to TcpRing.
     */
    private SocketOptions m_optionsTcpRing;

    /**
     * AddressProvider that provides the well known addresses (WKA)
     * represented by InetSocketAddress objects.
     */
    private AddressProvider m_providerWellKnownAddresses;

    /**
     * The {@link ParameterizedBuilderRegistry}.
     */
    private ParameterizedBuilderRegistry m_builderRegistry;

    /**
     * The unicast {@link SocketProviderBuilder}
     */
    private SocketProviderBuilder m_builderUnicastSocketProvider;

    /**
     * The registry of custom resources.
     */
    private SimpleResourceRegistry m_customResources;

    /**
     * A map of local (non-clustered) {@link ServiceProvider} instances.
     */
    private Map<String, ServiceProvider> m_mapLocalServiceProvider = new ConcurrentHashMap<>();

    /**
     * Lambdas serialization mode. Either "static", "dynamic" or "", indicating not set.
     */
    private String m_sLambdasSerializationMode = "";

    /**
     * Specifies whether using virtual threads is enabled.
     * Default is false.
     */
    private boolean m_fVirtualThreadsEnabled = false;
    }
