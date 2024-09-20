/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net;


import com.tangosol.coherence.config.builder.ActionPolicyBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilderRegistry;
import com.tangosol.coherence.config.builder.ServiceFailurePolicyBuilder;

import com.tangosol.coherence.config.builder.SocketProviderBuilder;
import com.tangosol.io.SerializerFactory;
import com.tangosol.io.WrapperStreamFactory;

import com.tangosol.persistence.SnapshotArchiverFactory;

import com.tangosol.run.xml.XmlElement;

import com.tangosol.util.Filter;

import com.oracle.coherence.common.base.Disposable;

import com.tangosol.util.ResourceRegistry;

import java.net.InetAddress;
import java.net.SocketOptions;

import java.util.List;
import java.util.Map;


/**
 * The ClusterDependencies interface defines externally provided dependencies
 * for a {@link Cluster}.
 *
 * @author pfm  2011.05.10
 * @since Coherence 12.1.3
 */
public interface ClusterDependencies
    {
    /**
     * Obtain the filter that is used by the cluster to determine whether to
     * accept a new Cluster member. The {@link Filter#evaluate} method
     * will be passed the java.net.InetAddress of the potential member.
     * Implementations should return "true" to allow the new member to join the
     * cluster.
     *
     * @return the authorized host filter
     */
    public Filter getAuthorizedHostFilter();

    /**
     * Obtain the action policy builder used by the cluster to control cluster
     * membership actions.
     *
     * @return the cluster action policy builder
     */
    public ActionPolicyBuilder getClusterActionPolicyBuilder();

    /**
     * Obtain the maximum amount of time that the cluster service will announce
     * itself without receiving a response before deciding that a cluster does
     * not exist and a new cluster should be formed.
     *
     * @return the cluster announce timeout milliseconds
     */
    public int getClusterAnnounceTimeoutMillis();

    /**
     * Obtain the amount of time between heartbeat messages.
     *
     * @return the cluster heartbeat delay
     */
    public int getClusterHeartbeatDelayMillis();

    /**
     * Obtain the maximum variance between sending and receiving broadcast messages
     * when trying to determine the difference between a new cluster Member's
     * wall time and the cluster time.
     * <p>
     * The smaller the variance, the more certain one can be that the cluster
     * time will be closer between multiple systems running in the cluster;
     * however, the process of joining the cluster will be extended until an
     * exchange of Messages can occur within the specified variance.
     * <p>
     * Normally, a value as small as 20 milliseconds is sufficient, but with
     * heavily loaded clusters and multiple network hops it is possible that a
     * larger value would be necessary.
     *
     * @return the cluster timestamp maximum variance
     */
    public int getClusterTimestampMaxVarianceMillis();

    /**
     * Obtain the Coherence product edition.
     * <p>
     * The valid value are:
     * <ul>
     *   <li>0=Data Client (DC)
     *   <li>1=Real-Time Client (RTC)
     *   <li>2=Standard Edition (SE)
     *   <li>3=Community Edition (CE)
     *   <li>4=Enterprise Edition (EE)
     *   <li>5=Grid Edition (GE)
     * </ul>
     * @return the edition
     */
    public int getEdition();

    /**
     * Obtain the list of all filters for the cluster.
     *
     * @return the filters list
     */
    public List<String> getFilterList();

    /**
     * Obtain a map of service name to filter list.  Each map entry has the list
     * of filters that will be used by the service.
     *
     * @return the service filter map
     */
    public Map<String, List<String>> getServiceFilterMap();

    /**
     * Obtain the map of filter name to WrapperStreamFactory.
     *
     * @return the filter map
     */
    public Map<String, WrapperStreamFactory> getFilterMap();

    /**
     * Return the InetAddress on which this member will listen for discovery requests.
     *
     * @return the local discovery address.
     */
    public InetAddress getLocalDiscoveryAddress();

    /**
     * Obtain the multicast group address to listen/publish on.
     *
     * @return the multicast group address, or null if WKA is enabled
     */
    public InetAddress getGroupAddress();

    /**
     * Obtain the preferred size for the multicast datagram socket input buffer
     * (SO_RCVBUF).
     * <p>
     * A negative value indicates that this value specifies the number of packets,
     * rather than number of bytes for the buffer.
     *
     * @return the group buffer size
     */
    public int getGroupBufferSize();

    /**
     * Obtain the IP address of the interface (e.g. the NIC) that the MulticastSocket
     * will open a socket on.
     *
     * @return the group interface
     */
    public InetAddress getGroupInterface();

    /**
     * Obtain the priority of the PacketListener daemon thread.
     *
     * @return the group listener priority
     */
    public int getGroupListenerPriority();

    /**
     * Obtain the multicast port number that the cluster will listen/publish on.
     *
     * @return the group port
     */
    public int getGroupPort();

    /**
     * Obtain the multicast time-to-live setting for the cluster.
     * <p>
     * The TTL sets the IP time-to-live for DatagramPackets sent to a
     * MulticastGroup, which specifies how many "hops" that the packet will be
     * forwarded on the network before it expires. The ttl must be in the range
     * 0 &lt;= ttl &lt;= 255.
     *
     * @return the TTL value
     */
    public int getGroupTimeToLive();

    /**
     * Obtain the default Guard timeout.
     *
     * @return the Guard Timeout
     */
    public long getGuardTimeoutMillis();

    /**
     * Obtain the number of connection attempts that the IpMonitor will use in
     * determining if IP connectivity exists to other hosts.
     *
     * @return number of IP monitor attempts
     */
    public int getIpMonitorAttempts();

    /**
     * Obtain the priority of the IpMonitor daemon thread.
     *
     * @return IP monitor priority
     */
    public int getIpMonitorPriority();

    /**
     * Obtain the timeout that the IpMonitor will use in determining if IP
     * connectivity exists to other hosts.
     *
     * @return the IP monitor timeout
     */
    public long getIpMonitorTimeoutMillis();

    /**
     * Obtain the preferred size for the unicast socket input buffer (SO_RCVBUF).
     * <p>
     * A negative value indicates that this value specifies the number of packets,
     * rather then number bytes for the buffer.
     *
     * @return the local buffer size
     */
    public int getLocalBufferSize();

    /**
     * Obtain the priority of the "local" PacketListener daemon thread.
     *
     * @return the local listener priority
     */
    public int getLocalListenerPriority();

    /**
     * Obtain the unicast IP address that the cluster member will listen/publish on
     * or null for default.
     *
     * @return the local address, or null
     */
    public InetAddress getLocalAddress();

    /**
     * Obtain the unicast port number that the member will listen on.
     *
     * @return the local port
     */
    public int getLocalPort();

    /**
     * Obtain the number of sequential packets which may be lost before declaring a
     * member paused, and starting to trickle packets.
     *
     * @return the lost packet threshold
     */
    public int getLostPacketThreshold();

    /**
     * Obtain the member identity.
     *
     * @return the member identity
     */
    public MemberIdentity getMemberIdentity();

    /**
     * Obtain the Mode for the "license type", i.e. evaluation, development or
     * production use.
     * <p>
     * The valid values are:
     * <ul>
     *   <li>{@link #LICENSE_MODE_EVALUATION}
     *   <li>{@link #LICENSE_MODE_DEVELOPMENT}
     *   <li>{@link #LICENSE_MODE_PRODUCTION}
     * </ul>
     *
     * @return the license mode
     */
    public int getMode();

    /**
     * Obtain the maximum allowable flow-control rate measured in packets.
     *
     * @return the maximum number of outstanding packets
     */
    public int getOutstandingPacketMaximum();

    /**
     * Obtain the minimum allowable flow-control rate measured in packets.
     *
     * @return the minimum number of outstanding packets
     */
    public int getOutstandingPacketMinimum();

    /**
     * Obtain the aggression factor to use in deferring a packet once it has
     * reached the average bundle size.
     *
     * @return the packet bundling aggression
     */
    public double getPacketBundlingAggression();

    /**
     * Obtain the maximum amount of time to defer a packet while waiting for
     * additional packets to bundle.
     *
     * @return the packet bundling threshold
     */
    public long getPacketBundlingThresholdNanos();

    /**
     * Obtain the maximum size, in bytes, of the network packets that
     * will be sent and received on the local and group sockets. This value
     * should be greater or equal to 256.
     *
     * @return the maximum packet length
     */
    public int getPacketMaxLength();

    /**
     * Obtain the preferred size, in bytes, of the network packets
     * that will be sent and received on the local sockets.
     * <p>
     * This value should be greater or equal to 256.  If set to zero the value
     * will be automatically computed based upon the MTU of the network interface
     * associated with {@link #getLocalAddress the local address}.
     *
     * @return the preferred packet length
     */
    public int getPacketPreferredLength();

    /**
     * Obtain the amount of time that the Publisher may delay sending an ACK packet.
     *
     * @return the publisher ACK delay
     */
    public int getPublisherAckDelayMillis();

    /**
     * Obtain the preferred size of the unicast socket output buffer (SO_SNDBUF).
     * <p>
     * A negative value indicates that this value specifies the number of packets,
     * rather than the number bytes for the buffer. A value of zero results in a
     * value being automatically calculated.
     *
     * @return the publisher buffer size
     */
    public int getPublisherSocketBufferSize();

    /**
     * Obtain the maximum number of packets in the send and re-send queues that the
     * Publisher will tolerate before determining that it is clogged and must
     * slow down client requests (requests from local non-system threads). Zero
     * means no limit. This property prevents most unexpected out-of-memory
     * conditions by limiting the size of the re-send queue.
     *
     * @return the publisher clogged count
     */
    public int getPublisherCloggedCount();

    /**
     * Obtain the amount of time that the Publisher will pause a client thread that
     * is trying to send a message when the Publisher is clogged. The Publisher
     * will not allow the message to go through until the clog is resolved, and will
     * repeatedly pause the thread for the duration specified by this value.
     *
     * @return the publisher clogged delay
     */
    public int getPublisherCloggedDelayMillis();

    /**
     * Obtain the group threshold which is used to determine whether a packet
     * will be sent via unicast or multicast.
     * <p>
     * This is a percentage value and is in the range of 1% to 100%.
     * In a cluster of "n" nodes, a particular node sending a packet
     * to a set of others (not counting self) destination nodes of size "d"
     * (in the range of 0 to n-1), the packet will be sent multicast if and only
     * if the following holds true:
     * <ol>
     *   <li>The packet is being sent over the network to more than one other node:
     *      (d &gt; 1);
     *   <li>The number of nodes is greater than the threshold: (d &gt; (n-1) *
     *      (threshold/100))
     * </ol>
     * Setting this value to 1 will allow the publisher to switch to
     * multicast for basically all multi-point traffic. Setting it to 100 will
     * force the publisher to use unicast for all multi-point traffic
     * except for explicit broadcast messages (e.g. cluster heartbeat and
     * discovery.)
     *
     * Note: that a values less then 100 will also prevent this cluster from
     * sharing its cluster port with other clusters running on the same machine.
     *
     * @return the publisher group threshold
     */
    public int getPublisherGroupThreshold();

    /**
     * Obtain the amount of time that the Publisher will delay sending a NACK
     * packet.
     *
     * @return the publisher NACK delay
     */
    public int getPublisherNackDelayMillis();

    /**
     * Obtain the priority of the PacketPublisher daemon thread.
     *
     * @return the publisher priority
     */
    public int getPublisherPriority();

    /**
     * Obtain the minimum amount of time before a packet is resent across the
     * network if it has not been acknowledged.
     * <p>
     * This value is also used for other situations in which packets need to be
     * resent, such as cluster announcements.
     *
     * @return the publisher resend delay
     */
    public int getPublisherResendDelayMillis();

    /**
     * Obtain the maximum amount of time that the publisher will be resending a
     * packet before the packet recipient is considered departed.
     *
     * @return the publisher resend timeout
     */
    public int getPublisherResendTimeoutMillis();

    /**
     * Obtain the priority of the PacketReceiver daemon thread.
     *
     * @return the receiver priority
     */
    public int getReceiverPriority();

    /**
     * Obtain the name of the default reliable transport used by this node.
     *
     * @return the default reliable transport name
     */
    public String getReliableTransport();

    /**
     * Obtain the service failure policy builder.
     *
     * @return the service failure policy builder
     */
    public ServiceFailurePolicyBuilder getServiceFailurePolicyBuilder();

    /**
     * Obtain the value of the ShutdownHook setting.
     * <p>
     * The valid values are the SHUTDOWN_* constants.
     *
     * @return the value of the ShutdownHook setting
     */
    public int getShutdownHookOption();

    /**
     * Obtain the SocketProviderFactory associated with the cluster.
     *
     * @return the socket provider factory
     */
    public SocketProviderFactory getSocketProviderFactory();

    /**
     * Obtain the service map.
     *
     * @return the map of service class information indexed by service name
     */
    public Map<String, String> getServiceMap();

    /**
     * Obtain the Serializer map.
     *
     * @return the Serializer map
     */
    public Map<String, SerializerFactory> getSerializerMap();

    /**
     * Obtain the snapshot archiver map.
     *
     * @return the snapshot archiver map
     */
    public Map<String, SnapshotArchiverFactory> getSnapshotArchiverMap();

    /**
     * Obtain the address provider map.
     *
     * @return the address provider map
     */
    public Map<String, AddressProviderFactory> getAddressProviderMap();

    /**
     * The registry for all builders associated with the cluster.
     * A builder can be looked up via the class it produces and a name for the
     * builder using {@link ParameterizedBuilderRegistry#getBuilder(Class, String)} or
     * just by the class it builds if there are no named builders.
     * <p>
     * Currently, only {@link SerializerFactory}, {@link
     * com.tangosol.coherence.config.builder.ParameterizedBuilder ParameterizedBuilder}&lt;
     * {@link com.oracle.coherence.persistence.PersistenceEnvironment PersistenceEnvironment}&gt;
     * and {@link
     * com.tangosol.coherence.config.builder.ParameterizedBuilder ParameterizedBuilder}&lt;
     * {@link com.tangosol.net.security.StorageAccessAuthorizer StorageAccessAuthorizer}&gt;
     * are registered by the implementation.
     * <p>
     * All registered Builders implementing {@link Disposable} will
     * be disposed.
     *
     * @return  the {@link ParameterizedBuilderRegistry}
     */
    public ParameterizedBuilderRegistry getBuilderRegistry();

    /**
     * Obtain the cluster's UnicastSocketProvider XML configuration.
     *
     * @return the unicast socket provider xml
     */
    @Deprecated
    public XmlElement getUnicastSocketProviderXml();

    /**
     * Obtain the cluster's unicast {@link SocketProviderBuilder}.
     *
     * @return unicast SocketProviderBuilder
     *
     * @since Coherence 12.2.1.1
     */
    public SocketProviderBuilder getUnicastSocketProviderBuilder();

    /**
     * Obtain the priority of the PacketSpeaker daemon thread.
     *
     * @return the PacketSpeaker priority
     */
    public int getSpeakerPriority();

    /**
     * Obtain the minimum number of packets which must be ready to be sent in order for
     * the speaker to be utilized. If the number of packets is less then this
     * setting then the publisher will send the packets itself. A value of 0
     * will cause all packets to be handled by the speaker, a high value
     * (Integer.MAX_VALUE) will cause all packets to be sent by the publisher.
     * <p>
     * If this value is negative, the actual value will be based on the socket's
     * send buffer size.
     *
     * @return the PacketSpeaker minimum volume
     */
    public int getSpeakerVolumeMinimum();

    /**
     * Determine whether the packet speaker is enabled.
     *
     * @return true if the speaker is enabled
     */
    public boolean isSpeakerEnabled();

    /**
     * Obtain the listener backlog for TCMP's TCP listener.
     *
     * @return the TCP listener backlog
     */
    public int getTcpBacklog();

    /**
     * Obtain the SocketOptions to apply when TCMP runs on a TcpDatagramSocket.
     *
     * @return the TCMP datagram socket options
     */
    public SocketOptions getTcpDatagramSocketOptions();

    /**
     * Obtain the TcpRing SocketOptions.
     *
     * @return the TcpRing socket options
     */
    public SocketOptions getTcpRingSocketOptions();

    /**
     * Obtain the AddressProvider that provides the well known addresses (WKA)
     * represented by InetSocketAddress objects.
     *
     * @return the well known addresses, or null for multicast
     */
    public AddressProvider getWellKnownAddresses();

    /**
     * Determine whether the TCMP flow control is enabled.
     *
     * @return true if TCMP flow control is enabled
     */
    public boolean isFlowControlEnabled();

    /**
     * Determine whether the IpMonitor is enabled.
     *
     * @return true if IpMonitor is enabled
     */
    public boolean isIpMonitorEnabled();

    /**
     * Determine whether the unicast listener local port(s) will be automatically
     * selected if the specified port cannot be bound to (e.g. it is already in use.)
     *
     * @return true if unicast listener local port is automatically selected
     */
    public boolean isLocalPortAutoAdjust();

    /**
     * Return the maximum unicast port that can be auto-adjusted to.
     *
     * @return the maximum unicast port that can be auto-adjusted to
     */
    public int getLocalPortAutoAdjust();

    /**
     * Determine whether the PacketReceiver will use negative acknowledgments
     * (NACK packet requests) to pro-actively respond to known missing packets.
     *
     * @return true if NACK is enabled
     */
    public boolean isReceiverNackEnabled();

    /**
     * Determine whether the TCMP is enabled.
     *
     * @return true if TCMP is enabled
     */
    public boolean isTcmpEnabled();

    /**
     * Determine whether the TcpRing is enabled.
     *
     * @return true if TcpRing is enabled
     */
    public boolean isTcpRingEnabled();

    /**
     * Register all the custom resources contained in this {@link com.tangosol.internal.net.cluster.ClusterDependencies}
     * with the target registry.
     *
     * @param registry  the target {@link ResourceRegistry} to register resources with
     */
    public void registerResources(ResourceRegistry registry);

    /**
     * Register a {@link ServiceProvider}.
     *
     * @param sType     the type of the service provided
     * @param provider  the {@link ServiceProvider} instance
     */
    public void addLocalServiceProvider(String sType, ServiceProvider provider);

    /**
     * Obtain a {@link ServiceProvider} that can build an instance
     * of a given service type.
     *
     * @param sType  the service type
     *
     * @return a {@link ServiceProvider} that can build an instance
     *         of a given service type
     */
    public ServiceProvider getLocalServiceProvider(String sType);

    /**
     * Obtain the lambdas serialization mode, i.e. static, dynamic or
     * empty string, if not set.
     *
     * @return the lambdas serialization mode or empty string if not set.
     *
     * @since 23.09
     */
    public String getLambdasSerializationMode();

    /**
     * Determine whether using virtual threads is enabled.
     *
     * @return true if using virtual threads is enabled
     *
     * @since 24.03
     */
    public boolean isVirtualThreadsEnabled();


    // ----- inner interface: ServiceProvider -------------------------------

    /**
     * A provider of nw service instances.
     */
    public interface ServiceProvider
        {
        /**
         * Create a new instance of a service.
         *
         * @param sName    the name of the service
         * @param cluster  the owning {@link Cluster}
         *
         * @return the new service instance
         */
        Service createService(String sName, Cluster cluster);

        ServiceProvider NULL_IMPLEMENTATION = new ServiceProvider()
            {
            @Override
            public Service createService(String sName, Cluster cluster)
                {
                return null;
                }
            };
        }

    // ----- constants ------------------------------------------------------

    /**
     * Perform no local shutdown logic before exiting the JVM.
     */
    public static final int SHUTDOWN_NONE = 0;

    /**
     * Forcefully shutdown all running services before exiting the JVM.
     */
    public static final int SHUTDOWN_FORCE = 1;

     /**
     * Gracefully shutdown all running services before exiting the JVM.
     */
    public static final int SHUTDOWN_GRACEFUL = 2;

    /**
     * Data Client edition (DC).
     */
    public static final int EDITION_DATA_CLIENT = 0;

    /**
     * Real time client edition (RTC).
     */
    public static final int EDITION_REAL_TIME_CLIENT = 1;

    /**
     * Standard edition (SE).
     */
    public static final int EDITION_STANDARD = 2;

    /**
     * Standard edition (SE).
     */
    public static final int EDITION_COMMUNITY = 3;

    /**
     * Enterprise edition (EE).
     */
    public static final int EDITION_ENTERPRISE = 4;

    /**
     * Grid Edition (GE).
     */
    public static final int EDITION_GRID = 5;

    /**
     * Evaluation license.
     */
    public static final int LICENSE_MODE_EVALUATION = 0;

    /**
     * Development license.
     */
    public static final int LICENSE_MODE_DEVELOPMENT = 1;

    /**
     * Production license.
     */
    public static final int LICENSE_MODE_PRODUCTION = 2;
    }
