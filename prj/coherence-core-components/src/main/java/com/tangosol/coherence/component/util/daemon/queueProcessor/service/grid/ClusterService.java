
/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService

package com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid;

import com.tangosol.coherence.component.application.console.Coherence;
import com.tangosol.coherence.component.net.Cluster;
import com.tangosol.coherence.component.net.Member;
import com.tangosol.coherence.component.net.MemberSet;
import com.tangosol.coherence.component.net.Message;
import com.tangosol.coherence.component.net.Packet;
import com.tangosol.coherence.component.net.Poll;
import com.tangosol.coherence.component.net.Security;
import com.tangosol.coherence.component.net.ServiceInfo;
import com.tangosol.coherence.component.net.memberSet.ActualMemberSet;
import com.tangosol.coherence.component.net.memberSet.DependentMemberSet;
import com.tangosol.coherence.component.net.memberSet.SingleMemberSet;
import com.tangosol.coherence.component.net.memberSet.actualMemberSet.ServiceMemberSet;
import com.tangosol.coherence.component.net.memberSet.actualMemberSet.serviceMemberSet.MasterMemberSet;
import com.tangosol.coherence.component.net.message.DiscoveryMessage;
import com.tangosol.coherence.component.net.packet.MessagePacket;
import com.tangosol.coherence.component.net.socket.udpSocket.UnicastUdpSocket;
import com.tangosol.coherence.component.util.daemon.queueProcessor.Logger;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
import com.oracle.coherence.common.base.Blocking;
import com.oracle.coherence.common.base.Continuation;
import com.oracle.coherence.common.internal.continuations.Continuations;
import com.oracle.coherence.common.net.InetAddresses;
import com.oracle.coherence.common.util.Duration;
import com.tangosol.coherence.config.Config;
import com.tangosol.net.ClusterDependencies;
import com.tangosol.net.InetAddressHelper;
import com.tangosol.net.MemberEvent;
import com.tangosol.net.cache.LocalCache;
import com.tangosol.net.internal.ClusterJoinException;
import com.tangosol.net.management.Registry;
import com.tangosol.net.security.PermissionInfo;
import com.tangosol.run.component.EventDeathException;
import com.tangosol.run.xml.XmlElement;
import com.tangosol.util.Base;
import com.tangosol.util.Binary;
import com.tangosol.util.BinaryWriteBuffer;
import com.tangosol.util.ClassHelper;
import com.tangosol.util.DeltaSet;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.ImmutableArrayList;
import com.tangosol.util.LongArray;
import com.tangosol.util.NullImplementation;
import com.tangosol.util.SparseArray;
import com.tangosol.util.UID;
import com.tangosol.util.UUID;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ClusterService is the "ring zero" Service with ServiceId=0 providing generic
 * cluster services such as cluster time and cluster membership.
 * 
 * Note: we MUST not change the message types for later releases to allow the
 * discovery part of the protocol be backward compatible to detect the service
 * version incompatibility.
 * 
 * The message range from [33,128] is reserved for usage by the ClusterService
 * component (as well as some historically preserved message numbers).
 * 
 * Currently used MessageTypes:
 * 5     NewMemberAcceptId (removed as of 3.7.1)
 * 6     NewMemberInduct (was NewMemberAcceptIdReply before 3.7.1)
 * 7     NewMemberAnnounce
 * 8     NewMemberAnnounceReply
 * 9     NewMemberAnnounceWait
 * 10   NewMemberRequestId
 * 11   NewMemberRequestIdReject
 * 12   NewMemberRequestIdReply
 * 13   NewMemberRequestIdWait
 * 17   SeniorMemberHeartbeat
 * 33   MemberHeartbeat
 * 34   *unused* (was MemberJoined before 12.2.1)
 * 35   MemberLeaving
 * 36   MemberLeft
 * 37   NewMemberWelcome
 * 38   NewMemberWelcomeAnnounce
 * 39   NewMemberWelcomeRequest
 * 40   SeniorMemberKill
 * 41   SeniorMemberPanic
 * 42   ServiceJoinRequest
 * 43   ServiceJoining   (was "ServiceJoined"  before 3.6)
 * 44   ServiceLeaving
 * 45   ServiceLeft
 * 46   ServiceRegister
 * 47   ServiceRegisterRequest
 * 48   ServiceUpdateResponse
 * 49   ServiceJoined
 * 50   ServiceSecureRequest
 * 51   NewMemberTimestampRequest
 * 52   NewMemberTimestampResponse
 * 53   QuorumRollCall
 * 54   NotifyTcmpTimeout
 * 55   NotifyIpTimeout
 * 56   WitnessRequest
 * 57   ServiceQuiescenceRequest
 */
@SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
public class ClusterService
        extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid
    {
    // ---- Fields declarations ----
    
    /**
     * Property ANNOUNCE_BIND_THRESHOLD
     *
     * The divisor of the BroadcastLimit at which a joining member must
     * [foreign] bind to the cluster port in order to continue the announce
     * without resetting.
     */
    public static final int ANNOUNCE_BIND_THRESHOLD = 3;
    
    /**
     * Property ANNOUNCE_PROTOCOL_VERSION
     *
     * The 8-bit protocol version for the TCMP cluster announce portion of the
     * protocol.  See NewMemberAnnounce#write
     */
    public static final int ANNOUNCE_PROTOCOL_VERSION = 1;
    
    /**
     * Property AnnounceMember
     *
     * The AnnounceMember is the Member object that the ClusterService uses to
     * announce its presence. It is temporary, in that it is used to elicit a
     * time adjustment Message from the oldest Member in the cluster, from
     * which the RequestMember will be configured.
     * 
     * @see Cluster.configureDaemons
     */
    private com.tangosol.coherence.component.net.Member __m_AnnounceMember;
    
    /**
     * Property BroadcastAddresses
     *
     * A Set<InetSocketAddress> used by the TCMP for broadcast in leu of
     * multicast. This set could be modified by the ClusterService and used by
     * the TCMP transport tier.
     * 
     * @see #addDynamicBroadcast
     */
    private transient java.util.Set __m_BroadcastAddresses;
    
    /**
     * Property BroadcastAddressesExpiry
     *
     * The timestamp at which the broadcast address set expires and should be
     * cleared.
     */
    private transient long __m_BroadcastAddressesExpiry;
    
    /**
     * Property BroadcastCounter
     *
     * BroadcastCounter is the "try" number for broadcasting an "announce" or
     * "request" Message while trying to join a cluster. There are many events
     * that can reset the BroadcastCounter.
     */
    private int __m_BroadcastCounter;
    
    /**
     * Property BroadcastLimit
     *
     * BroadcastLimit is the maximum number of "tries" for broadcasting an
     * "announce" or "request" Message while trying to join a cluster. The
     * BroadcastLimit is calculated from the broadcast timeout information.
     * 
     * *** LICENSE ***
     * Note: Setter contains important license-related functionality:
     * 
     * - sets up license binary (in the BroadcastTimestamp property) that is
     * used to "inject" licenses into the cluster
     * - calls resetBroadcastCounter() with the license data so that the list
     * of local licenses are known locally
     */
    private int __m_BroadcastLimit;
    
    /**
     * Property BroadcastMode
     *
     * *** LICENSE ***
     * WARNING: This property name is obfuscated.
     * 
     * 0=license not checked
     * 1=license is OK
     * 2=license is hacked
     * 
     * Set by setBroadcastLimit()
     */
    private int __m_BroadcastMode;
    
    /**
     * Property BroadcastNextMillis
     *
     * The BroadcastNextMillis value is the time (in local system millis) at
     * which the next announce or request broadcast Message is scheduled to be
     * sent.
     */
    private long __m_BroadcastNextMillis;
    
    /**
     * Property BroadcastRepeatMillis
     *
     * The BroadcastRepeatMillis value specifies how many milliseconds between
     * repeatedly sending announce or request broadcast Messages.
     */
    private int __m_BroadcastRepeatMillis;
    
    /**
     * Property BroadcastTimeoutMillis
     *
     * The BroadcastTimeoutMillis value specifies how long the ClusterService
     * will send announce or request broadcast Messages without receiving a
     * reply before assuming that no one is there to reply.
     */
    private int __m_BroadcastTimeoutMillis;
    
    /**
     * Property BroadcastTimestamp
     *
     * *** LICENSE ***
     * WARNING: This property name is obfuscated.
     * 
     * This is the binary form of the license structure for the local node.
     * When this node tries to join a cluster, this value is copied to the
     * NewMemberRequestId message, written out as a binary, and read in as a
     * series of UID / product / limit-type / limit-value values.
     * 
     * Built by ClusterService.setBroadcastLimit(int).
     */
    private com.tangosol.util.Binary __m_BroadcastTimestamp;
    
    /**
     * Property DeliveryTimeoutMillis
     *
     * The time interval in milliseconds that TCMP uses to determine Members
     * departure.
     * 
     * @see PacketPublisher#getResendTimeout
     */
    private transient int __m_DeliveryTimeoutMillis;
    
    /**
     * Property HeartbeatDelay
     *
     * The number of milliseconds between heartbeats.
     */
    private int __m_HeartbeatDelay;
    
    /**
     * Property HeartbeatMemberSet
     *
     * Set of Members that have this service.
     */
    private transient com.tangosol.coherence.component.net.MemberSet __m_HeartbeatMemberSet;
    
    /**
     * Property LastInterminableWarningMillis
     *
     * The time at which the last BroadcastCounter reset warning was logged.
     * This property is also used to determine that the announcing is going
     * nowhere fast and it has issued a warning to the log.
     */
    private transient long __m_LastInterminableWarningMillis;
    
    /**
     * Property LastPanicUid
     *
     * The UID of the last member caused the panic protocol activation.
     */
    private com.tangosol.util.UUID __m_LastPanicUid;
    
    /**
     * Property MaximumPacketLength
     *
     * The max packet size in bytes that TCMP can receive (and can/will send).
     * Used only for packed-based transports.
     */
    private transient int __m_MaximumPacketLength;
    
    /**
     * Property MembershipReopen
     *
     * Specifies the date/time (in millis) until when new members are not
     * allowed to join the cluster [service]. This covers such rare situations
     * as existence of a "zombie" senior (possibly with an island of members),
     * when the new membership is suspended until one of the islands is removed.
     */
    private long __m_MembershipReopen;
    
    /**
     * Property PendingServiceJoining
     *
     * This property stores a sparse array of ServiceJoining messages that this
     * senior Member is currently processing. By keeping track of these
     * messages, it is possible to know whether new members can join, or if
     * their joining needs to be temporarily deferred until the service joining
     * notification is complete.
     * 
     * When the senior Member receives a ServiceJoinRequest message it notifies
     * all other members using the ServiceJoining message (poll). Until all
     * members respond to that poll, no new Member can be accepted to the
     * cluster without compromising the synchronization of the service info. As
     * soon as the ServiceJoining poll completes, the requestor is notified and
     * all deferred NewMemberAcceptIdReply messages are delivered.
     * 
     * The array is indexed by the ServiceJoinRequest timestamp and is used to
     * remove Members that fail to respond to the senior's poll in a reasonable
     * period of time.
     * 
     * @see #DeferredAcceptMember property
     * @see #checkPendingJoinPolls
     */
    private transient com.tangosol.util.LongArray __m_PendingServiceJoining;
    
    /**
     * Property QuorumControl
     *
     * The QuorumControl manages the state and decision logic relating to
     * quorum-based membership decisions.
     */
    private ClusterService.QuorumControl __m_QuorumControl;
    
    /**
     * Property REJECT_AUTHORIZE
     *
     * Member rejection id indicating that the member is not authorized to join
     * the cluster.
     * 
     * The values are offset by 8160 to maintain backwards compatibility with
     * 3.x error codes.  As of 12.1.2 the actual integer value does not matter
     * other then that it be unique across the enum.
     */
    public static final int REJECT_AUTHORIZE = 8161;
    
    /**
     * Property REJECT_CLUSTER_NAME
     *
     * Member rejection id indicating that the member has a different cluster
     * name then the cluster.
     * 
     * The values are offset by 8160 to maintain backwards compatibility with
     * 3.x error codes.  As of 12.1.2 the actual integer value does not matter
     * other then that it be unique across the enum.
     */
    public static final int REJECT_CLUSTER_NAME = 8167;
    
    /**
     * Property REJECT_EDITION
     *
     * Member rejection id indicating that the member's edition is not
     * compatible with the cluster.
     * 
     * The values are offset by 8160 to maintain backwards compatibility with
     * 3.x error codes.  As of 12.1.2 the actual integer value does not matter
     * other then that it be unique across the enum.
     */
    public static final int REJECT_EDITION = 8168;
    
    /**
     * Property REJECT_LICENSE_EXPIRED
     *
     * Member rejection id indicating that the member has an expired license.
     * 
     * The values are offset by 8160 to maintain backwards compatibility with
     * 3.x error codes.  As of 12.1.2 the actual integer value does not matter
     * other then that it be unique across the enum.
     */
    public static final int REJECT_LICENSE_EXPIRED = 8164;
    
    /**
     * Property REJECT_LICENSE_TYPE
     *
     * Member rejection id indicating that the member has an incompatible
     * license type for use with the running cluster.
     * 
     * The values are offset by 8160 to maintain backwards compatibility with
     * 3.x error codes.  As of 12.1.2 the actual integer value does not matter
     * other then that it be unique across the enum.
     */
    public static final int REJECT_LICENSE_TYPE = 8169;
    
    /**
     * Property REJECT_NONE
     *
     * Member rejection id indicating that the cluster member was not rejected.
     *  i.e. the non-reject code place holder.
     * 
     * The values are offset by 8160 to maintain backwards compatibility with
     * 3.x error codes.  As of 12.1.2 the actual integer value does not matter
     * other then that it be unique across the enum.
     */
    public static final int REJECT_NONE = 8171;
    
    /**
     * Property REJECT_PACKET_MAX
     *
     * Member rejection id indicating that the member has an incompatible
     * maximum packet size.
     * 
     * The values are offset by 8160 to maintain backwards compatibility with
     * 3.x error codes.  As of 12.1.2 the actual integer value does not matter
     * other then that it be unique across the enum.
     */
    public static final int REJECT_PACKET_MAX = 8163;
    
    /**
     * Property REJECT_QUORUM
     *
     * Member rejection id indicating that the cluster member was rejected by
     * the quorum policy.
     * 
     * The values are offset by 8160 to maintain backwards compatibility with
     * 3.x error codes.  As of 12.1.2 the actual integer value does not matter
     * other then that it be unique across the enum.
     */
    public static final int REJECT_QUORUM = 8172;
    
    /**
     * Property REJECT_RESTART
     *
     * Member rejection id indicating that the member should restart the join
     * protocol.
     * 
     * The values are offset by 8160 to maintain backwards compatibility with
     * 3.x error codes.  As of 12.1.2 the actual integer value does not matter
     * other then that it be unique across the enum.
     */
    public static final int REJECT_RESTART = 8170;
    
    /**
     * Property REJECT_SENIOR
     *
     * Member rejection id indicating that the member is not allowed to form a
     * cluster.
     * 
     * The values are offset by 8160 to maintain backwards compatibility with
     * 3.x error codes.  As of 12.1.2 the actual integer value does not matter
     * other then that it be unique across the enum.
     */
    public static final int REJECT_SENIOR = 8165;
    
    /**
     * Property REJECT_SIZE
     *
     * Member rejection id indicating that the cluster is unable to accept
     * additional members.
     * 
     * The values are offset by 8160 to maintain backwards compatibility with
     * 3.x error codes.  As of 12.1.2 the actual integer value does not matter
     * other then that it be unique across the enum.
     */
    public static final int REJECT_SIZE = 8160;
    
    /**
     * Property REJECT_VERSION
     *
     * Member rejection id indicating that the member is running a version of
     * Coherence which is incompatible with the cluster's version.
     * 
     * The values are offset by 8160 to maintain backwards compatibility with
     * 3.x error codes.  As of 12.1.2 the actual integer value does not matter
     * other then that it be unique across the enum.
     */
    public static final int REJECT_VERSION = 8162;
    
    /**
     * Property REJECT_WKA
     *
     * Member rejection id indicating that the member and cluster are using
     * incompatible broadcast mediums, i.e. one is using multicast while the
     * other uses WKA.
     * 
     * The values are offset by 8160 to maintain backwards compatibility with
     * 3.x error codes.  As of 12.1.2 the actual integer value does not matter
     * other then that it be unique across the enum.
     */
    public static final int REJECT_WKA = 8166;
    
    /**
     * Property RequestMember
     *
     * The Member object being used to request an id. The RequestMember object
     * differs from the AnnounceMember in that the cluster time is known before
     * the RequestMember is configured, so that its age (and thus seniority) is
     * in cluster time.
     */
    private com.tangosol.coherence.component.net.Member __m_RequestMember;
    
    /**
     * Property Service
     *
     * Indexed property of Service daemons running on <b>this</b> Member.
     * 
     * @volatile - read by PacketReceiver and TransportService
     */
    private volatile com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid[] __m_Service;
    
    /**
     * Property ServiceContext
     *
     * A map of Service related PermissionInfo objects keyed by the service
     * name.
     */
    private transient java.util.Map __m_ServiceContext;
    
    /**
     * Property ServiceInfo
     *
     * Indexed property of ServiceInfo objects that maintain data about
     * Services running in the cluster.
     */
    private com.tangosol.coherence.component.net.ServiceInfo[] __m_ServiceInfo;
    
    /**
     * Property State
     *
     * State of the ClusterService; one of:
     *     STATE_ANNOUNCE
     *     STATE_JOINING
     *     STATE_JOINED
     */
    private int __m_State;
    
    /**
     * Property STATE_ANNOUNCE
     *
     * Initial cluster state when the ClusterService is announcing its presence.
     */
    public static final int STATE_ANNOUNCE = 0;
    
    /**
     * Property STATE_JOINED
     *
     * Cluster state once this Member has created or joined a cluster.
     */
    public static final int STATE_JOINED = 2;
    
    /**
     * Property STATE_JOINING
     *
     * Cluster state when the ClusterService has determined that a cluster is
     * running and it is requesting to join (requesting a Member id).
     */
    public static final int STATE_JOINING = 1;
    
    /**
     * Property STATE_LEAVING
     *
     * Cluster state once this Member has started to leave a cluster.
     */
    public static final int STATE_LEAVING = 3;
    
    /**
     * Property STATE_LEFT
     *
     * Cluster state once this Member has left a cluster.
     */
    public static final int STATE_LEFT = 4;
    
    /**
     * Property StatsJoinRequests
     *
     * Statistics: total number of received join requests
     */
    private transient long __m_StatsJoinRequests;
    
    /**
     * Property StatsMembersDepartureCount
     *
     * The number of member disconnects from the cluster.  This value is
     * exposed on the cluster MBean.
     */
    private transient long __m_StatsMembersDepartureCount;
    
    /**
     * Property TcpRing
     *
     * The ClusterService's TcpRing.
     */
    private transient ClusterService.TcpRing __m_TcpRing;
    
    /**
     * Property TimestampAdjustment
     *
     * The number of milliseconds off the cluster time is from the local system
     * time.
     */
    private transient long __m_TimestampAdjustment;
    
    /**
     * Property TimestampMaxVariance
     *
     * The maximum number of apparent milliseconds difference allowed between
     * send and receive time when determining how far off the cluster time is
     * from the local system time.
     */
    private transient int __m_TimestampMaxVariance;
    
    /**
     * Property VERSION
     *
     * The cluster node version. In general, this constant is unnecessary and
     * only used if the build version info was not found in the manifest.
     * 
     * @see #getServiceVersion
     */
    public static final String VERSION = "12.2.1.1.0";
    
    /**
     * Property VERSION_BARRIER
     *
     * The smallest of the versions that this version is still compatible with.
     * An attempt to join any version lower than the "barrier" one will fail.
     * 
     * @see #isCompatibleVersion
     */
    public static final String VERSION_BARRIER = "12.2.1";
    
    /**
     * Property WellKnownAddresses
     *
     * The well-known-addresses or null for multicast
     */
    private java.util.Set __m_WellKnownAddresses;
    
    /**
     * Property WkaMap
     *
     * *** LICENSE ***
     * WARNING: This property name is obfuscated.
     * 
     * Map keyed by UID, with each value an int[] of:
     * [0] edition (0=DC, ..5=DGE)
     * [1] limit type (0=site, 1=srv, 2=sock, 3=cpu, 4=seat, 5=user)
     * [2] limit value
     * 
     * Maintained by resetBroadcastCounter()
     */
    private java.util.Map __m_WkaMap;
    private static com.tangosol.util.ListMap __mapChildren;
    
    // Static initializer
    static
        {
        __initStatic();
        }
    
    // Default static initializer
    private static void __initStatic()
        {
        // register child classes
        __mapChildren = new com.tangosol.util.ListMap();
        __mapChildren.put("Acknowledgement", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.Acknowledgement.get_CLASS());
        __mapChildren.put("BusEventMessage", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.BusEventMessage.get_CLASS());
        __mapChildren.put("ConfigRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ConfigRequest.get_CLASS());
        __mapChildren.put("ConfigResponse", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ConfigResponse.get_CLASS());
        __mapChildren.put("ConfigSync", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ConfigSync.get_CLASS());
        __mapChildren.put("ConfigUpdate", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ConfigUpdate.get_CLASS());
        __mapChildren.put("DispatchEvent", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.DispatchEvent.get_CLASS());
        __mapChildren.put("DispatchNotification", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.DispatchNotification.get_CLASS());
        __mapChildren.put("MemberConfigUpdate", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.MemberConfigUpdate.get_CLASS());
        __mapChildren.put("MemberHeartbeat", ClusterService.MemberHeartbeat.get_CLASS());
        __mapChildren.put("MemberJoined", ClusterService.MemberJoined.get_CLASS());
        __mapChildren.put("MemberLeaving", ClusterService.MemberLeaving.get_CLASS());
        __mapChildren.put("MemberLeft", ClusterService.MemberLeft.get_CLASS());
        __mapChildren.put("MemberWelcome", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.MemberWelcome.get_CLASS());
        __mapChildren.put("MemberWelcomeRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.MemberWelcomeRequest.get_CLASS());
        __mapChildren.put("MemberWelcomeRequestTask", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.MemberWelcomeRequestTask.get_CLASS());
        __mapChildren.put("NewMemberAnnounce", ClusterService.NewMemberAnnounce.get_CLASS());
        __mapChildren.put("NewMemberAnnounceReply", ClusterService.NewMemberAnnounceReply.get_CLASS());
        __mapChildren.put("NewMemberAnnounceWait", ClusterService.NewMemberAnnounceWait.get_CLASS());
        __mapChildren.put("NewMemberInduct", ClusterService.NewMemberInduct.get_CLASS());
        __mapChildren.put("NewMemberRequestId", ClusterService.NewMemberRequestId.get_CLASS());
        __mapChildren.put("NewMemberRequestIdReject", ClusterService.NewMemberRequestIdReject.get_CLASS());
        __mapChildren.put("NewMemberRequestIdReply", ClusterService.NewMemberRequestIdReply.get_CLASS());
        __mapChildren.put("NewMemberRequestIdWait", ClusterService.NewMemberRequestIdWait.get_CLASS());
        __mapChildren.put("NewMemberTimestampRequest", ClusterService.NewMemberTimestampRequest.get_CLASS());
        __mapChildren.put("NewMemberTimestampResponse", ClusterService.NewMemberTimestampResponse.get_CLASS());
        __mapChildren.put("NewMemberWelcome", ClusterService.NewMemberWelcome.get_CLASS());
        __mapChildren.put("NewMemberWelcomeAnnounce", ClusterService.NewMemberWelcomeAnnounce.get_CLASS());
        __mapChildren.put("NewMemberWelcomeRequest", ClusterService.NewMemberWelcomeRequest.get_CLASS());
        __mapChildren.put("NotifyConnectionClose", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyConnectionClose.get_CLASS());
        __mapChildren.put("NotifyConnectionOpen", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyConnectionOpen.get_CLASS());
        __mapChildren.put("NotifyIpTimeout", ClusterService.NotifyIpTimeout.get_CLASS());
        __mapChildren.put("NotifyMemberJoined", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyMemberJoined.get_CLASS());
        __mapChildren.put("NotifyMemberLeaving", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyMemberLeaving.get_CLASS());
        __mapChildren.put("NotifyMemberLeft", ClusterService.NotifyMemberLeft.get_CLASS());
        __mapChildren.put("NotifyMessageReceipt", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyMessageReceipt.get_CLASS());
        __mapChildren.put("NotifyPollClosed", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyPollClosed.get_CLASS());
        __mapChildren.put("NotifyResponse", ClusterService.NotifyResponse.get_CLASS());
        __mapChildren.put("NotifyServiceAnnounced", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyServiceAnnounced.get_CLASS());
        __mapChildren.put("NotifyServiceJoining", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyServiceJoining.get_CLASS());
        __mapChildren.put("NotifyServiceLeaving", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyServiceLeaving.get_CLASS());
        __mapChildren.put("NotifyServiceLeft", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyServiceLeft.get_CLASS());
        __mapChildren.put("NotifyServiceQuiescence", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyServiceQuiescence.get_CLASS());
        __mapChildren.put("NotifyShutdown", ClusterService.NotifyShutdown.get_CLASS());
        __mapChildren.put("NotifyStartup", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyStartup.get_CLASS());
        __mapChildren.put("NotifyTcmpTimeout", ClusterService.NotifyTcmpTimeout.get_CLASS());
        __mapChildren.put("PingRequest", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.PingRequest.get_CLASS());
        __mapChildren.put("ProtocolContext", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ProtocolContext.get_CLASS());
        __mapChildren.put("QuorumRollCall", ClusterService.QuorumRollCall.get_CLASS());
        __mapChildren.put("Response", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.Response.get_CLASS());
        __mapChildren.put("SeniorMemberHeartbeat", ClusterService.SeniorMemberHeartbeat.get_CLASS());
        __mapChildren.put("SeniorMemberKill", ClusterService.SeniorMemberKill.get_CLASS());
        __mapChildren.put("SeniorMemberPanic", ClusterService.SeniorMemberPanic.get_CLASS());
        __mapChildren.put("ServiceJoined", ClusterService.ServiceJoined.get_CLASS());
        __mapChildren.put("ServiceJoining", ClusterService.ServiceJoining.get_CLASS());
        __mapChildren.put("ServiceJoinRequest", ClusterService.ServiceJoinRequest.get_CLASS());
        __mapChildren.put("ServiceLeaving", ClusterService.ServiceLeaving.get_CLASS());
        __mapChildren.put("ServiceLeft", ClusterService.ServiceLeft.get_CLASS());
        __mapChildren.put("ServiceQuiescenceRequest", ClusterService.ServiceQuiescenceRequest.get_CLASS());
        __mapChildren.put("ServiceRegister", ClusterService.ServiceRegister.get_CLASS());
        __mapChildren.put("ServiceRegisterRequest", ClusterService.ServiceRegisterRequest.get_CLASS());
        __mapChildren.put("ServiceUpdateResponse", ClusterService.ServiceUpdateResponse.get_CLASS());
        __mapChildren.put("WitnessRequest", ClusterService.WitnessRequest.get_CLASS());
        __mapChildren.put("WrapperGuardable", com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.WrapperGuardable.get_CLASS());
        }
    
    // Default constructor
    public ClusterService()
        {
        this(null, null, true);
        }
    
    // Initializing constructor
    public ClusterService(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
        {
        super(sName, compParent, false);
        
        if (fInit)
            {
            __init();
            }
        }
    
    // Main initializer
    public void __init()
        {
        // private initialization
        __initPrivate();
        
        // state initialization: public and protected properties
        try
            {
            setAcceptingClients(false);
            setAcceptingOthers(true);
            setBroadcastRepeatMillis(256);
            setBroadcastTimeoutMillis(32768);
            setDaemonState(0);
            setDefaultGuardRecovery(0.9F);
            setDefaultGuardTimeout(60000L);
            setMessageClassMap(new java.util.HashMap());
            setNotifier(new com.oracle.coherence.common.base.SingleWaiterMultiNotifier());
            setOldestPendingRequestSUIDCounter(new java.util.concurrent.atomic.AtomicLong());
            setResourceRegistry(new com.tangosol.util.SimpleResourceRegistry());
            setSerializerMap(new java.util.WeakHashMap());
            setServiceId(0);
            setServiceName("Cluster");
            setSuspendPollLimit(new java.util.concurrent.atomic.AtomicLong());
            setTimestampMaxVariance(10);
            }
        catch (java.lang.Exception e)
            {
            // re-throw as a runtime exception
            throw new com.tangosol.util.WrapperException(e);
            }
        
        // containment initialization: children
        _addChild(new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.DaemonPool("DaemonPool", this, true), "DaemonPool");
        _addChild(new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.EventDispatcher("EventDispatcher", this, true), "EventDispatcher");
        _addChild(new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.Guard("Guard", this, true), "Guard");
        _addChild(new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.MemberConfigListener("MemberConfigListener", this, true), "MemberConfigListener");
        _addChild(new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.PollArray("PollArray", this, true), "PollArray");
        _addChild(new ClusterService.QuorumControl("QuorumControl", this, true), "QuorumControl");
        _addChild(new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ReceiveQueue("ReceiveQueue", this, true), "ReceiveQueue");
        _addChild(new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.ServiceConfig("ServiceConfig", this, true), "ServiceConfig");
        _addChild(new ClusterService.TcpRing("TcpRing", this, true), "TcpRing");
        
        // signal the end of the initialization
        set_Constructed(true);
        }
    
    // Private initializer
    protected void __initPrivate()
        {
        
        super.__initPrivate();
        
        // state initialization: private properties
        try
            {
            __m_PendingServiceJoining = new com.tangosol.util.SparseArray();
            __m_WkaMap = new com.tangosol.util.SafeHashMap();
            }
        catch (java.lang.Exception e)
            {
            // re-throw as a runtime exception
            throw new com.tangosol.util.WrapperException(e);
            }
        }
    
    // Getter for virtual constant BroadcastVariance
    public int getBroadcastVariance()
        {
        return 212343923;
        }
    
    // Getter for virtual constant ServiceType
    public String getServiceType()
        {
        return "Cluster";
        }
    
    //++ getter for static property _Instance
    /**
     * Getter for property _Instance.<p>
    * Auto generated
     */
    public static com.tangosol.coherence.Component get_Instance()
        {
        return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService();
        }
    
    //++ getter for static property _CLASS
    /**
     * Getter for property _CLASS.<p>
    * Property with auto-generated accessor that returns the Class object for a
    * given component.
     */
    public static Class get_CLASS()
        {
        Class clz;
        try
            {
            clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ClusterService".replace('/', '.'));
            }
        catch (ClassNotFoundException e)
            {
            throw new NoClassDefFoundError(e.getMessage());
            }
        return clz;
        }
    
    //++ getter for autogen property _Module
    /**
     * This is an auto-generated method that returns the global [design time]
    * parent component.
    * 
    * Note: the class generator will ignore any custom implementation for this
    * behavior.
     */
    private com.tangosol.coherence.Component get_Module()
        {
        return this;
        }
    
    //++ getter for autogen property _ChildClasses
    /**
     * This is an auto-generated method that returns the map of design time
    * [static] children.
    * 
    * Note: the class generator will ignore any custom implementation for this
    * behavior.
     */
    protected java.util.Map get_ChildClasses()
        {
        return __mapChildren;
        }
    
    /**
     * Include the socket address for the specified non-WKA member to a list
    * that will be used for the "broadcast-over-unicast" if the multicast is
    * disabled.
     */
    public void addDynamicBroadcast(com.tangosol.coherence.component.net.Member member)
        {
        // import com.tangosol.util.Base;
        // import java.net.InetSocketAddress;
        // import java.util.Set;
        
        if (member != null && !isWellKnown(member))
            {
            Set setBroadcast = getBroadcastAddresses();
            if (setBroadcast != null)
                {        
                synchronized (setBroadcast)
                    {
                    setBroadcast.add(member.getSocketAddress());
                    }
        
                // only retain a dynamic broadcast address for a limited time, these addresses may not
                // belong to cluster members and we don't want to endlessly "spam" the address if it
                // has been picked up by a non-coherence process
                setBroadcastAddressesExpiry(Base.getSafeTimeMillis() + getDeliveryTimeoutMillis());
                }
            }
        }
    
    /**
     * @return a number of machines the specified set of members belongs to
     */
    public static int calcMachines(java.util.Set setMembers)
        {
        // import Component.Net.Member;
        // import com.tangosol.util.Base;
        // import java.util.HashSet;
        // import java.util.Iterator;
        
        HashSet setMachines = new HashSet();
        for (Iterator iter = setMembers.iterator(); iter.hasNext();)
            {
            Member member = (Member) iter.next();
            setMachines.add(Base.makeInteger(member.getMachineId()));
            }
        
        return setMachines.size();
        }
    
    /**
     * @param lLocalhostMillis  the timestamp value that is on the localhost
    * clock
    * 
    * @return the "cluster" time equivalent
     */
    public long calcTimestamp(long lLocalhostMillis)
        {
        if (lLocalhostMillis != 0)
            {
            lLocalhostMillis += getTimestampAdjustment();
            }
        return lLocalhostMillis;
        }
    
    // Declared at the super level
    /**
     * Check the guardables that are guarded by this Daemon.
     */
    protected void checkGuardables()
        {
        if (isGuardian())
            {
            long cLateMillis = getGuardSupport().check();
            if (cLateMillis > 0L)
                {
                // the Guardian was very late checking on its guardables.
                // This is likely a JVM-wide issue, but only ClusterService
                // logs trace, to avoid many duplicates in the log
                _trace("Service guardian is " + cLateMillis +
                       "ms late, indicating that this JVM may be " +
                       "running slowly or experienced a long GC", 3);
                }
            }
        }
    
    /**
     * Check for any pending ServiceJoining polls that are long overdue.
     */
    protected void checkPendingJoinPolls()
        {
        // import Component.Net.Member;
        // import Component.Net.MemberSet;
        // import Component.Net.Poll;
        // import com.tangosol.util.Base;
        // import com.tangosol.util.LongArray;
        // import com.tangosol.util.LongArray$Iterator as com.tangosol.util.LongArray.Iterator;
        // import java.util.ArrayList;
        // import java.util.HashSet;
        // import java.util.Iterator;
        // import java.util.List;
        // import java.util.Set;
        
        // only permitted to occur on the ClusterService thread itself
        _assert(Thread.currentThread() == getThread());
        
        LongArray laPending = getPendingServiceJoining();
        if (!laPending.isEmpty())
            {
            long ldtNow    = Base.getSafeTimeMillis();
            long cTimeout  = getDeliveryTimeoutMillis();            // death timeout
            long ldtCutoff = ldtNow - (cTimeout - (cTimeout >> 2)); // slow = .75 of death
            long ldtOldest = laPending.getFirstIndex();             // oldest "open" poll
            if (ldtOldest < ldtCutoff)
                {
                ClusterService.ServiceJoining msg = (ClusterService.ServiceJoining) laPending.get(ldtOldest);
        
                String sMsg = "ServiceJoining";
                try
                    {
                    sMsg = String.valueOf(msg);
                    }
                catch (Exception e) {}
                
                Poll poll = msg.getRequestPoll();
                if (poll == null)
                    {
                    // this should be impossible
                    laPending.remove(ldtOldest);
                    _trace("validatePolls: "
                         + "This senior encountered a null pending poll for message: " + sMsg, 1);
                    return;
                    }
        
                MemberSet setRemain = poll.getRemainingMemberSet();
                if (setRemain.isEmpty())
                    {
                    // this should also be impossible
                    boolean fWasClosed = poll.isClosed();
                    poll.close();
                    laPending.remove(ldtOldest);
                    _trace("validatePolls: "
                            + "This senior encountered an empty " + (fWasClosed ? "closed " : "")
                            + "pending poll for message: " + sMsg, 1);
                    return;
                    }
        
                // at this point, there is at least one member that has not
                // responded to the poll for whatever reason (including perfectly
                // valid reasons, i.e. not real problems); regardless, kill them
                // all to avoid the a valid member killing itself incorrectly,
                // or even worse permitting the perceived "lock up" to remain
                // (i.e. no one can join the cluster at this point)
        
                String sPoll = "Poll";
                try
                    {
                    sPoll = poll.toString();
                    }
                catch (Exception e) {}
                _trace("validatePolls: "
                        + "This senior encountered an overdue poll, indicating a dead "
                        + "member, a significant network issue or an Operating "
                        + "System threading library bug (e.g. Linux NPTL): " + sPoll
                        + "\nfor message: " + sMsg, 2);
        
                try
                    {
                    // collect a list of overdue members and what machines they are on
                    List listOverdue = new ArrayList();
                    Set  setMachines = new HashSet();
        
                    // we cannot iterate the poll's MemberSet; instead we will iterate
                    // the master set and pick the unresponsive members
                    MemberSet setAll = getClusterMemberSet();
                    Member[] aMember = (Member[]) setAll.toArray(new Member[setAll.size()]);
                    for (int i = 0, c = aMember.length; i < c; i++)
                        {
                        Member member = aMember[i];
                        if (member != null && setRemain.contains(member))
                            {
                            listOverdue.add(member);
                            setMachines.add(Integer.valueOf(member.getMachineId()));
                            }
                        }
        
                    // calculate a time that is beyond the death detection time-out
                    // at which point the senior can "give up all hope" for the
                    // unresponsive members (1.25 times death timeout); this also makes
                    // sure that if this senior is the problem, that the other members
                    // have had a good chance to kill (and maybe shun) it
                    long ldtGiveUp = ldtOldest + (cTimeout + (cTimeout >> 2));
        
                    // if only one member has failed to respond (which we have seen
                    // occur -- possibly due to a NPTL bug or some other low-level
                    // issue), then kill that one member; for sake of argument, assume
                    // that is true for any number of members on a single machine.
                    // alternatively, once the membership (death detection) time-out
                    // plus some safety margin has come and gone, assume that it is
                    // safe to kill any members that are not responding
                    if (setMachines.size() <= 1 || ldtNow > ldtGiveUp)
                        {
                        for (Iterator iter = listOverdue.iterator(); iter.hasNext(); )
                            {
                            Member member = (Member) iter.next();
                            _trace("Removing unresponsive member: " + member, 2);
                            doMemberLeft(member);
                            }
        
                        // be certain that the poll is closed
                        poll.close();
        
                        // check for NPTL bug etc.
                        if (listOverdue.isEmpty())
                            {
                            // this should never happen
                            _trace("validatePolls: "
                                 + "This senior encountered and closed an overdue poll "
                                 + "that did not contain any known members, indicating "
                                 + "a likely JVM or Operating System threading library "
                                 + "(e.g. Linux NPTL) bug: " + sPoll, 1);
                            }
                        }
                    }
                catch (Throwable e)
                    {
                    // this is impossible
                    _trace("validatePolls: " + "Non-fatal exception detected during processing:", 1);
                    _trace(e);
                    _trace("validatePolls: " + "Exception has been logged; continuing processing.", 1);
                    }
                }
            }
        }
    
    // Declared at the super level
    /**
     * Compare importance of the specified Member with the local Member. This
    * method is usually called during processing of the partial packet delivery
    * failure, when a decision has to be made to either kill another member or
    * commit a suicide.
    * 
    * @return zero if this service does not care or views those members as of
    * "equal importance"; negative number if the importance of this Member is
    * lesser than of the specified one and positive number if the importance of
    * this Member is greater than of the specified one. The absolute value of
    * the return value signifies a difference in importance.
     */
    public int compareImportance(com.tangosol.coherence.component.net.Member memberThat)
        {
        // import Component.Net.Member;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        
        Member memberThis = getThisMember();
        
        int nThisWeight = memberThis.getPriority();
        int nThatWeight = memberThat.getPriority();
        int iResult     = nThisWeight - nThatWeight;
        for (int i = 1, c = getServiceCount(); i < c; ++i)
            {
            com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid service = getService(i);
            if (service != null)
                {
                try
                    {
                    iResult += service.compareImportance(memberThat);
                    }
                catch (Throwable e)
                    {
                    _trace("Failed to determine importance of com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid \""
                         + service.getServiceName() + "\"\n" + getStackTrace(e), 1);
                    }
                }
            }
        
        return iResult;
        }
    
    public void doMemberInduct(com.tangosol.coherence.component.net.Member memberNew)
        {
        // import Component.Net.Member;
        // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet.MasterMemberSet;
        // import Component.Net.ServiceInfo;
        
        // only permitted to occur on the ClusterService thread itself
        _assert(Thread.currentThread() == getThread());
        
        // send list of all Members to the new Member
        ClusterService.NewMemberInduct msg = (ClusterService.NewMemberInduct) instantiateMessage("NewMemberInduct");
        msg.addToMember(memberNew);
        
        // add Members to msg
        MasterMemberSet setMember = getClusterMemberSet();
        Object[]        aoMember  = setMember.toArray();
        int             cMembers  = aoMember.length;
        
        msg.setMemberCount(cMembers);
        for (int i = 0; i < cMembers; ++i)
            {
            Member member = (Member) aoMember[i];
            msg.setMember(i, member);
        
            // we won't know the newcomer's real version until the
            // NewMemberWelcomeRequest-NewMemberWelcome exchange,
            // so we need to keep pretending to be 12.2.1
            msg.setServiceVersion(i, VERSION_BARRIER);
            }
        
        // add Services to msg
        int cServices = getServiceInfoCount();
        msg.setServiceCount(cServices);
        for (int i = 0; i < cServices; ++i)
            {
            ServiceInfo info = getServiceInfo(i);
            msg.setServiceId(i, info.getServiceId());
            msg.setServiceName(i, info.getServiceName());
            msg.setServiceType(i, info.getServiceType());
            msg.setServiceSuspended(i, info.isSuspended());
            }
        
        send(msg);
        }
    
    /**
     * Called on the cluster service thread only.
     */
    public void doMemberLeaving()
        {
        // import Component.Net.Member;
        
        setState(STATE_LEAVING);
        
        Member memberThis = getThisMember();
        if (memberThis != null)
            {
            // publish the event to the rest of the cluster
            ClusterService.MemberLeaving msg = (ClusterService.MemberLeaving)
                    instantiateMessage("MemberLeaving");
            msg.setToMemberSet(getOthersMemberSet());
            msg.setMemberId (memberThis.getId());
            msg.setMemberUid(memberThis.getUid32());
            send(msg);
            }
        }
    
    /**
     * Called on the cluster service thread only.
     */
    public void doMemberLeft()
        {
        // import Component.Net.Cluster;
        // import Component.Net.Member;
        // import com.tangosol.util.UUID;
        
        Member memberThis = getThisMember();
        if (memberThis != null)
            {
            ClusterService.MemberLeft msg = (ClusterService.MemberLeft) instantiateMessage("MemberLeft");
            msg.setToMemberSet(getOthersMemberSet());
        
            msg.setSynchronizationRequest(false);
            msg.setMemberCount(1);
            msg.setMemberIDs(new short[]{(short) memberThis.getId()}); 
            msg.setMemberUUIDs(new UUID[]{memberThis.getUid32()});
            msg.setMemberTimestamps(new long[]{calcTimestamp(memberThis.getTimestamp())});
            
            send(msg);
        
            // Allow a small time for the packet to hopefully make it onto the
            // wire. We neither wait for or expect to receive the corresponding
            // ACKs, even if the packet is received the recipient would think us
            // dead and not have any reason to ACK. Trying to make this
            // deterministic would be and endless game.
            sleep(((Cluster) getCluster()).getDependencies()
                .getPublisherAckDelayMillis());
            }
        
        setState(STATE_LEFT);
        }
    
    /**
     * Inform the cluster about the death of the specified node. 
    * 
    * @param member  the member which has died
     */
    public void doMemberLeft(com.tangosol.coherence.component.net.Member member)
        {
        removeMember(member);
        ensureMemberLeft(/*memberTo*/ null, /*setUUIDExempt*/ null, /*fRequest*/ false);
        }
    
    /**
     * Called by the IpMonitor thread to notify the local ClusterService that an
    * IP address that was previously reported as unavailable continues to be
    * unavailable.
     */
    public void doNotifyIpTimeout(java.net.InetAddress address)
        {
        // publish the event to the local ClusterService
        ClusterService.NotifyIpTimeout msg =
            (ClusterService.NotifyIpTimeout) instantiateMessage("NotifyIpTimeout");
        msg.addToMember(getThisMember());
        msg.setTimedOutAddress(address);
        send(msg);
        }
    
    /**
     * Notify the local ClusterService that a member has exceeded the packet
    * timeout. Called on a TCMP daemon thread (e.g. PacketPublisher).
    * 
    * @param packet  the timedout packet, or null if the timeout is a bus level
    * timeout
    * @setTimedOut  the associated set of members
     */
    public void doNotifyTcmpTimeout(com.tangosol.coherence.component.net.packet.MessagePacket packet, com.tangosol.coherence.component.net.MemberSet setTimedOut)
        {
        // publish the event to the local ClusterService
        ClusterService.NotifyTcmpTimeout msg =
            (ClusterService.NotifyTcmpTimeout) instantiateMessage("NotifyTcmpTimeout");
        msg.addToMember(getThisMember());
        msg.setUndeliverablePacket(packet);
        msg.setTimedOutMembers(setTimedOut);
        
        send(msg);
        }
    
    /**
     * Called to notify cluster service that a member has fully joined a
    * service.
    * 
    * Called on the specified service's thread.
     */
    public void doServiceJoined(com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid service)
        {
        // import Component.Net.Member;
        // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet;
        
        int nServiceId = service.getServiceId();
        if (nServiceId > 0)
            {
            if (getService(nServiceId) == service)
                {
                ServiceMemberSet setMember  = service.getServiceMemberSet();
                Member           memberThis = getThisMember();
                long             ldtJoined  = setMember.getServiceJoinTime(memberThis.getId());
        
                // send message to the Cluster service on this node
                ClusterService.ServiceJoined msg = (ClusterService.ServiceJoined)
                        instantiateMessage("ServiceJoined");
                msg.addToMember(memberThis);
                msg.setServiceId(nServiceId);
                send(msg);
                }
            else
                {
                // soft assert
                _trace("ClusterService.doServiceJoined: " + "Unknown Service " + service, 1);
                }
            }
        }
    
    /**
     * Called to notify cluster service that a member has started joining a
    * service.
    * 
    * Called on the specified service's thread.
     */
    public void doServiceJoining(com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid service)
        {
        // import Component.Net.Cluster;
        // import Component.Net.Member;
        // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet;
        // import Component.Net.Security;
        // import Component.Net.ServiceInfo;
        // import com.tangosol.net.security.PermissionInfo;
        // import com.tangosol.util.Base;
        // import com.tangosol.util.ClassHelper;
        // import java.util.HashMap;
        
        int nServiceId = service.getServiceId();
        if (nServiceId > 0 && getService(nServiceId) == null)
            {
            Member      memberThis = getThisMember();
            ServiceInfo info       = getServiceInfo(nServiceId);
        
            // make sure there is no old info
            info.setServiceJoinTime(memberThis.getId(), 0L);
        
            // we must store the service to receive any messages for it
            setService(nServiceId, service);
        
            PermissionInfo piRequest = null;
            Security       security  = service.getServiceId() > Cluster.MAX_SYSTEM_SERVICE
                ? Security.getInstance()
                : null; // no security for system services
        
            if (security != null)
                {
                piRequest = (PermissionInfo) getServiceContext().remove(service.getServiceName());
                if (piRequest == null)
                    {
                    throw new SecurityException("No security token available for join request for " + 
                        service.getServiceName());
                    }
                }
        
            while (true)
                {
                Member memberSenior = getClusterOldestMember();
        
                // send the join request to the senior
                //
                // The possible responses to the ClusterService.ServiceJoinRequest (and the
                // resulting $ServiceJoining/$NotifyServiceJoining) messages could be:
                //   null           - security is off and request to join was accepted
                //   PermissionInfo - security is on and request to join was accepted
                //   String         - soft-rejection; new member should retry
                //   Exception      - hard-rejection; new member should stop joining
        
                ClusterService.ServiceJoinRequest msg = (ClusterService.ServiceJoinRequest)
                        instantiateMessage("ServiceJoinRequest");
                msg.addToMember(memberSenior);
                msg.setServiceId(nServiceId);
                msg.setServiceVersion(service.getServiceVersion());
                msg.setServiceEndPointName(service.getEndPointName());
                msg.setMemberConfigMap(new HashMap(service.getThisMemberConfigMap()));
        
                if (security != null)
                    {
                    msg.setPermissionInfo(piRequest);
                    }
        
                // 2002.08.12 cp changed from send() to poll()
                // (remember, this code is being executed on the new service's
                // thread, not on the ClusterService thread, so control does
                // not return to the new service until all other Members in
                // the cluster have been notified that this Member is now
                // running the new service)
                //
                // Note: the ServiceInfo is constructed (and this member added
                //       to the service member set) after other cluster members
                //       have been notified.  See ClusterService.ServiceJoining.onReceived()
        
                try
                    {
                    Object oResponse = poll(msg);
        
                    if (isExiting())
                        {
                        if (security != null)
                            {
                            _trace("Missing secure response. Security may be disabled at service senior node", 1);
                            }
                        throw new RuntimeException("Join request was aborted");
                        }
        
                    ServiceMemberSet setMember = service.getServiceMemberSet();
                    if (setMember == null)
                        {
                        // the senior died or someone vetoed the join;
                        // validate registration info and try again
        
                        if (nServiceId == ensureService(service))
                            {
                            if (oResponse instanceof Exception)
                                {
                                throw new RuntimeException("ServiceJoinRequest for " + service.getServiceName()
                                                         + " has been rejected", (Exception) oResponse);
                                }
                            else if (getClusterOldestMember() == memberSenior)
                                {
                                _trace("ServiceJoinRequest for " + service.getServiceName()
                                     + " has been rejected: " + oResponse + "; repeating request", 3);
                                sleep(1000); // wait a second to let things settle
                                }
                            else if (security != null)
                                {
                                // seniority has changed thus try again straight away
                                _trace("ServiceJoinRequest for " + service.getServiceName()
                                     + " has not received secure response; repeating request", 3);
                                }
        
                            // reset the transport
                            service.initializeTransport();
                            }
                        else
                            {
                            throw new RuntimeException(
                                "Service info mismatch; service has to be restarted");
                            }
                        }
                    else
                        {
                        // by this time, the service has its own ServiceMemberSet
                        // (see ClusterService.ServiceJoinRequest.Poll.onCompletion)
        
                        if (security != null)
                            {
                            if (oResponse instanceof PermissionInfo)
                                {
                                // verify that the service senior is not malevolent
                                security.verifySecureResponse(service, (PermissionInfo) oResponse);
                                }
                            else
                                {
                                Member memberOldest = setMember.getOldestMember();
                                if (memberOldest == memberThis)
                                    {
                                    // this is the senior, no response is expected
                                    }
                                else if (oResponse != null)
                                    {
                                    // invalid response
                                    StringBuilder sb = new StringBuilder("Received an unexpected response: ")
                                        .append(oResponse.getClass());
                                    try
                                        {
                                        // guard against oResponse toString throwing an exception
                                        sb.append(": ").append(oResponse);
                                        }
                                    catch (Exception e) {}
        
                                    _trace(sb.toString(), 4);
        
                                    throw new SecurityException("Invalid response received " +
                                            "from join request. Security may be misconfigured at " +
                                            memberOldest);
                                    }
                                else
                                    {
                                    _assert(false, "Member joined the cluster without a valid join response");
                                    }
                                }
                            }
                        break;
                        }
                    }
                catch (Throwable e)
                    {
                    info.getMemberSet().remove(memberThis);
                    setService(nServiceId, null);
                    throw Base.ensureRuntimeException(e);
                    }
                }
            }
        else if (getService(nServiceId) != service)
            {
            // the main reason for this to happen is to call "Cluster.ensureService"
            // without obtaing the synchronization monitor for the cluster object
            throw new IllegalStateException("Attempt to replace existing service: " +
                getService(nServiceId) + "\nwith a different instance: " + service);
            }
        }
    
    /**
     * Called on the specified service's thread.
     */
    public void doServiceLeaving(com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid service)
        {
        // import Component.Net.Member;
        // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet;
        
        int nServiceId = service.getServiceId();
        if (nServiceId > 0)
            {
            if (getService(nServiceId) == service)
                {
                ServiceMemberSet setMember  = getServiceInfo(nServiceId).getMemberSet();
                Member           memberThis = getThisMember();
                long             ldtJoined  = setMember.getServiceJoinTime(memberThis.getId());
        
                // send message to the Cluster service on this node
                ClusterService.ServiceLeaving msg = (ClusterService.ServiceLeaving)
                        instantiateMessage("ServiceLeaving");
                msg.addToMember(memberThis);
                msg.setServiceId(nServiceId);
                msg.setServiceJoinTime(ldtJoined);
                send(msg);
                }
            else
                {
                // soft assert
                _trace("ClusterService.doServiceLeaving: " + "Unknown Service " + service, 1);
                }
            }
        }
    
    /**
     * Called on the specified service's thread.
     */
    public void doServiceLeft(com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid serviceLeft)
        {
        // import Component.Net.Member;
        // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid;
        
        int nServiceId = serviceLeft.getServiceId();
        if (nServiceId > 0)
            {
            Grid service = getService(nServiceId);
        
            // service entry may already cleared (see #doServiceJoining)
            // send ServiceLeft regardless
            if (service == null || service == serviceLeft)
                {
                ServiceMemberSet setMember  = getServiceInfo(nServiceId).getMemberSet();
                Member           memberThis = getThisMember();
                long             ldtJoined  = setMember.getServiceJoinTime(memberThis.getId());
        
                // clear out the service entry
                setService(nServiceId, null);
        
                // send message to the Cluster service on this node
                ClusterService.ServiceLeft msg = (ClusterService.ServiceLeft)
                        instantiateMessage("ServiceLeft");
                msg.addToMember(memberThis);
                msg.setServiceId(nServiceId);
                msg.setServiceJoinTime(ldtJoined);
                msg.setMemberLeftId(memberThis.getId());
                send(msg);
                }
            else if (!serviceLeft.isExiting())
                {
                // soft assert
                _trace("ClusterService.doServiceLeft: " + "Unknown Service " + serviceLeft, 1);
                }
            }
        }
    
    /**
     * Called to suspend or resume a service (cluster-wide).
     */
    public void doServiceQueiscence(String sService, boolean fResume)
        {
        doServiceQueiscence(sService, fResume, /*fResumeOnFailover*/ false);
        }
    
    /**
     * Called to suspend or resume a service (cluster-wide).
     */
    public void doServiceQueiscence(String sService, boolean fResume, boolean fResumeOnFailover)
        {
        // import Component.Net.MemberSet;
        // import Component.Net.ServiceInfo;
        
        ServiceInfo info = getServiceInfo(sService);
        if (info == null)
            {
            throw new IllegalArgumentException("Unknown service: " + sService);
            }
        else
            {
            ClusterService.ServiceQuiescenceRequest msg;
            do
                {
                msg = (ClusterService.ServiceQuiescenceRequest) instantiateMessage("ServiceQuiescenceRequest");
                msg.setServiceId(info.getServiceId());
                msg.setResume(fResume);
                msg.setResumeOnFailover(fResumeOnFailover);
                msg.setRelay(true);
                msg.setToMemberSet(MemberSet.instantiate(getServiceMemberSet().getOldestMember()));
                poll(msg);
                }
            while (msg.ensureRequestPoll().getRespondedMemberSet().isEmpty());
            }
        }
    
    /**
     * Ensure that this member has made the cluster discoverable via the NS,
    * this means that it either runs the NS on the cluster port or has
    * registered with a foreign NS so that traffic for this cluster can be
    * redirected to this node.
    * 
    * @return true if this member has made the cluster discoverable via the NS
     */
    protected boolean ensureDiscovery()
        {
        return false; // overridden at Cluster level
        }
    
    /**
     * Called on the cluster service thread only.
     */
    public com.tangosol.coherence.component.net.Member ensureMember(com.tangosol.coherence.component.net.Member memberNew, String sVersion)
        {
        // import Component.Net.Member;
        // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet.MasterMemberSet;
        
        MasterMemberSet setMember = getClusterMemberSet();
        int             nId       = memberNew.getId();
        Member          member    = setMember.getMember(nId);
        
        if (member == null)
            {
            member = memberNew;
        
            Member memberDead = setMember.getRecycleSet().getMember(nId);
            if (memberDead != null && memberDead.getUid32().equals(memberNew.getUid32()))
                {
                // member was declared dead before we became aware of it, this can happen
                // as the senior sends out joins but anyone can send out deaths, and thus
                // we can learn of death before birth
                return null;
                }
            else
                {
                member.initCommSupport();
        
                setMember.add(member);
        
                setMember.setServiceVersion(nId, sVersion);
                setMember.setServiceJoinTime(nId, member.getTimestamp());
                setMember.setServiceEndPointName(nId, "");
                setMember.setServiceJoining(nId);
        
                onMemberJoined(member);
                }
            }
        
        return member;
        }
    
    /**
     * Ensure that the cluster is informed about the death of members.
    * 
    * @param memberTo           the member to inform, or null for the full
    * member set
    * @param setUUIDExcempt the set of UUIDs to exclude from the notification
    * @param fRequest               true to send as a synchronization request
     */
    public void ensureMemberLeft(com.tangosol.coherence.component.net.Member memberTo, java.util.Set setUUIDExempt, boolean fRequest)
        {
        // import Component.Net.Member;
        // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet.MasterMemberSet;
        // import Component.Net.MemberSet.SingleMemberSet;
        // import com.tangosol.util.Base;
        // import com.tangosol.util.UUID;
        
        MasterMemberSet setMembers      = getClusterMemberSet();
        Member[]        aMemberDead     = (Member[]) setMembers.getRecycleSet().toArray((Object[]) null);
        short[]         anId            = new short[aMemberDead.length];
        UUID[]          aUUID           = new UUID[aMemberDead.length];
        long[]          aldtDeath       = new long[aMemberDead.length];
        long            ldtCutoffMillis = Base.getSafeTimeMillis() - setMembers.getRecycleMillis();
        int             cDead           = 0;
        
        for (int i = 0, c = aMemberDead.length; i < c; ++i)
            {
            Member member = aMemberDead[i];
            if (member != null && member.getTimestamp() > ldtCutoffMillis &&
               (member.isLeaving() || memberTo != null) && // if isLeaving == false and memberTo == null, the address is naturally exempt
               (setUUIDExempt == null || !setUUIDExempt.contains(member.getUid32())))
                {
                anId[cDead]      = (short) member.getId();
                aUUID[cDead]     = member.getUid32();
                aldtDeath[cDead] = calcTimestamp(member.getTimestamp()); // death timestamp is stored in local safe time
        
                if (memberTo == null)
                    {
                    // we are informing the entire cluster, thus once this message has
                    // been sent there is no point in including this member in any
                    // subsequence cluster wide notifications
                    member.setLeaving(false); // set onMemberLeft
                    }
        
                ++cDead;
                }
            }
        
        if (cDead > 0 || fRequest)
            {
            ClusterService.MemberLeft msg = (ClusterService.MemberLeft) instantiateMessage("MemberLeft");
            msg.setToMemberSet(memberTo == null
                ? getOthersMemberSet()
                : SingleMemberSet.instantiate(memberTo));
        
            msg.setSynchronizationRequest(fRequest);
            msg.setMemberCount(cDead);
            msg.setMemberIDs(anId); 
            msg.setMemberUUIDs(aUUID);
            msg.setMemberTimestamps(aldtDeath);
            
            send(msg);
            }
        }
    
    /**
     * Called NOT on the cluster service thread.
     */
    public int ensureService(com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid service)
        {
        // import Component.Net.Member;
        // import Component.Net.Security;
        // import Component.Net.ServiceInfo as com.tangosol.coherence.component.net.ServiceInfo;
        
        String sName = service.getServiceName();
        String sType = service.getServiceType();
        
        while (true)
            {
            // send register request
            ClusterService.ServiceRegisterRequest msg = (ClusterService.ServiceRegisterRequest)
                    instantiateMessage("ServiceRegisterRequest");
            msg.addToMember(getClusterOldestMember());
            msg.setServiceName(sName);
            msg.setServiceType(sType);
        
            Object oResult = poll(msg);
        
            if (oResult != null)
                {
                int    nId      = ((Integer) oResult).intValue();
                com.tangosol.coherence.component.net.ServiceInfo   info     = getServiceInfo(nId);
                String sTypeReg = info.getServiceType();
        
                if (!service.isCompatibleServiceType(sTypeReg))
                    {
                    throw new IllegalArgumentException(
                        "Invalid service type: requested=" + sType + ", registered=" + sTypeReg);
                    }
                return nId;
                }
        
            if (getServiceState() > SERVICE_STARTED)
                {
                throw new IllegalStateException(
                    "Service can not be created against a stopped cluster");
                }
            // else the cluster senior has left; try again
            }
        }
    
    /**
     * Called on the cluster service thread only (except construction - onInit).
     */
    public com.tangosol.coherence.component.net.ServiceInfo ensureServiceInfo(int nService, String sName, String sType)
        {
        // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet;
        // import Component.Net.ServiceInfo;
        
        // it's theoretically possible (though extremely unlikely)
        // that there is some stalled data for the specified service
        
        ServiceInfo[] ainfo = getServiceInfo();
        for (int i = 0, c = (ainfo == null ? 0 : ainfo.length); i < c; ++i)
            {
            ServiceInfo info = ainfo[i];
            if (info != null)
                {
                boolean fObsolete = false;
                if (i == nService)
                    {
                    fObsolete = !info.getServiceName().equals(sName) ||
                        (sType != null && !sType.equals(info.getServiceType()));
                     }
                else
                    {
                    fObsolete = info.getServiceName().equals(sName);
                    }
        
                if (fObsolete)
                    {
                    _trace("Removing the obsolete service info: " + info, 3);
                    ainfo[i] = null;
                    }
                }
            }
        
        ServiceInfo info = getServiceInfo(nService);
        if (info == null)
            {
            info = new ServiceInfo();
            info.setMemberSet(nService == 0 ?
                getServiceMemberSet() : new ServiceMemberSet());
            info.setServiceId(nService);
            info.setServiceName(sName);
            info.setServiceType(sType);
        
            if (nService != 0 && getServiceInfo(0).isSuspended())
                {
                // newly defined services inherit cluster service suspend state
                info.setSuspended(true);
                }
        
            setServiceInfo(nService, info);
            onServiceAnnounced(info);
            }
        else
            {
            _assert(sName.equals(info.getServiceName()));
            _assert(sType == null || sType.equals(info.getServiceType()));
            }
        
        return info;
        }
    
    /**
     * Helper method for cluster name logging.
    * 
    * @return a String containing a word 'cluster' optionally followed by the
    * ClusterName property
     */
    public String formatClusterString()
        {
        String sClusterName = getClusterName();
        StringBuffer sb = new StringBuffer("cluster");
        if (sClusterName != null && sClusterName.length() > 0)
            {
            sb.append(" \"").append(sClusterName).append('"');
            }
        return sb.toString();
        }
    
    public static String formatStateName(int nState)
        {
        switch (nState)
            {
            case STATE_ANNOUNCE:
                return "STATE_ANNOUNCE";
            case STATE_JOINING:
                return "STATE_JOINING";
            case STATE_JOINED:
                return "STATE_JOINED";
            case STATE_LEAVING:
                return "STATE_LEAVING";
            case STATE_LEFT:
                return "STATE_LEFT";
            default:
                return "<unknown> " + nState;
            }
        }
    
    /**
     * *** LICENSE ***
    * WARNING: This method name is obfuscated.
    * 
    * translate various license-related strings to their enum values
    * 
    * - product names: on these, ignore the (tm) and/or colon
    * Oracle Client Access License=0
    * Oracle Coherence(tm): Local Edition=1
    * Oracle Coherence(tm): Clustered Edition=3
    * Oracle Coherence(tm): Enterprise Edition=4
    * 
    * Oracle Coherence: Data Client=0
    * Oracle Coherence: Real-Time Client=1
    * Oracle Coherence: Compute Client=2
    * Oracle Coherence: Caching Edition=3
    * Oracle Coherence: Application Edition=4
    * Oracle Coherence: DataGrid Edition=5
    * 
    * - old license classes
    * com.tangosol.license.ClientAccess=0
    * com.tangosol.license.Coherence=3
    * com.tangosol.license.CoherenceEnterprise=4
    * com.tangosol.license.CoherenceLocal=1
    * 
    * - new license classes
    * com.tangosol.license.CoherenceDataGridEdition=5
    * com.tangosol.license.CoherenceApplicationEdition=4
    * com.tangosol.license.CoherenceCachingEdition=3
    * com.tangosol.license.CoherenceComputeClient=2
    * com.tangosol.license.CoherenceRealTimeClient=1
    * com.tangosol.license.CoherenceDataClient=0
    * 
    * - modes
    * eval=0
    * dev=1
    * prod=2
    * evaluation=0
    * development=1
    * production=2
    * Eval=0
    * Dev=1
    * Prod=2
    * Evaluation=0
    * Development=1
    * Production=2
    * 
    * - editions
    * DGE=5
    * DE=5
    * AE=4
    * CE=3
    * CC=2
    * RTC=1
    * RC=1
    * DC=0
    * 
    * - otherwise, return -1
    * com.tangosol.license.ClusterService
    * com.tangosol.license.Constellation
    * com.tangosol.license.DevelopmentEdition
    * com.tangosol.license.EnterpriseEdition
    * com.tangosol.license.PersonalizationEdition
    * com.tangosol.license.StandardEdition
    * Oracle XML Framework
    * Oracle Java Assembler
    * Oracle Cluster Service
    * etc.
    * 
    * *** UNIT TEST *** (add to onInit() for example)
    * String[] as2 = new String[]
    *     {
    *     "com.tangosol.license.ClientAccess",
    *     "com.tangosol.license.ClusterService",
    *     "com.tangosol.license.Coherence",
    *     "com.tangosol.license.CoherenceEnterprise",
    *     "com.tangosol.license.CoherenceLocal",
    *     "com.tangosol.license.Constellation",
    *     "com.tangosol.license.DevelopmentEdition",
    *     "com.tangosol.license.EnterpriseEdition",
    *     "com.tangosol.license.PersonalizationEdition",
    *     "com.tangosol.license.StandardEdition",
    *     "com.tangosol.license.CoherenceDataGridEdition",
    *     "com.tangosol.license.CoherenceApplicationEdition",
    *     "com.tangosol.license.CoherenceCachingEdition",
    *     "com.tangosol.license.CoherenceComputeClient",
    *     "com.tangosol.license.CoherenceRealTimeClient",
    *     "com.tangosol.license.CoherenceDataClient",
    *     "eval",
    *     "dev",
    *     "prod",
    *     "evaluation",
    *     "devevelopment",
    *     "production",
    *     "Eval",
    *     "Dev",
    *     "Prod",
    *     "Evaluation",
    *     "Devevelopment",
    *     "Production",
    *     "DGE",
    *     "DE",
    *     "AE",
    *     "ACE",   // BUGBUG old typo in Coherence component
    *     "CE",
    *     "CC",
    *     "RTC",
    *     "RC",
    *     "DC",
    *     "Oracle Client Access License",
    *     "Oracle Coherence(tm): Local Edition",
    *     "Oracle Coherence(tm): Clustered Edition",
    *     "Oracle Coherence(tm): Enterprise Edition",
    *     "Oracle Coherence Local Edition",
    *     "Oracle Coherence Clustered Edition",
    *     "Oracle Coherence Enterprise Edition",
    *     "Oracle XML Framework",
    *     "Oracle Java Assembler",
    *     "Oracle Cluster Service",   
    *     };
    * for (int i = 0, cs = as2.length; i < cs; ++i)
    *     {
    *     _trace(as2[i] + "=" + fromString(as2[i]));
    *     } 
     */
    public int fromString(String s)
        {
        // import com.tangosol.util.Base;
        
        // note: read the doc!
        
        if (s != null && s.length() >= 2)
            {
            int ch1 = s.charAt(0);
            int ch2 = s.charAt(1);
        
            if (s.indexOf(' ') > 0)
                {
                String[] a = Base.parseDelimitedString(s, ' ');
                if (a.length > 3
                        && a[0].length() > 5
                        && a[1].length() > 5
                        && a[2].length() > 3
                        && a[3].length() > 3)
                    {
                    int n = 0;
                    for (int i = 1; i < 4; ++i)
                        {
                        n = (n << 8) | a[i].charAt(i);
                        }
        
                    switch (n)
                        {
                        case 0x6F7465: // Co Dat Clie
                        case 0x6C6365: // Cl Acc Lice
                            return 0;
                        case 0x6F6165: // Co Rea Clie
                        case 0x6F6D65: // Co Com Clie
                            return 1;
                        case 0x6F6374: // Co Loc Edit, Co Cac Edit
                            if (a[2].charAt(0) == 'L')
                                {
                                return 0;
                                }
                        case 0x6F6174: // Co Sta Edit
                        case 0x6F7574: // Co Clu Edit
                            return 3;
                        case 0x6F7074: // Co App Edit
                        case 0x6F7474: // Co Ent Edit, Co Dat Edit
                            if (a[2].charAt(0) != 'D')
                                {
                                return 4;
                                }
                        case 0x6F6974: // Co Gri Edit
                            return 5;
                        }
                    }
                }
        
            //   0         1         2         3         4
            //   01234567890123456789012345678901234567890123456789
            // 0=com.tangosol.license.ClientAccess
            // 0=com.tangosol.license.CoherenceDataClient
            // 1=com.tangosol.license.CoherenceLocal
            // 1=com.tangosol.license.CoherenceRealTimeClient
            // 2=com.tangosol.license.CoherenceComputeClient
            // 3=com.tangosol.license.Coherence
            // 3=com.tangosol.license.CoherenceCachingEdition
            // 4=com.tangosol.license.CoherenceApplicationEdition
            // 4=com.tangosol.license.CoherenceEnterprise
            // 5=com.tangosol.license.CoherenceDataGridEdition
            //   com.tangosol.license.ClusterService
            //   com.tangosol.license.Constellation
            //   com.tangosol.license.DevelopmentEdition
            //   com.tangosol.license.EnterpriseEdition
            //   com.tangosol.license.PersonalizationEdition
            //   com.tangosol.license.StandardEdition
            if (s.length() >= 30 && s.charAt(13) == 'l' && s.charAt(28) == 'c')
                {
                // convert class name to DC, RTC, CC, SE, EE, GE
                ch2 = s.length();
                ch1 = s.charAt(ch2 - 1);
                switch (ch1)
                    {
                    case 'e': // Coherence, CoherenceEnterprise
                        return 5 - (ch2 < 33 ? 2 : 1);
                    case 'l': // CoherenceLocal
                    case 's': // ClientAccess
                        return 0;
                    default: // *Client, *Edition
                        ch2 = s.charAt(ch2 - (ch1 == 'n' ? 1 : 0) - 6);
                        ch1 = s.charAt(30);
                        break;
                    }
                }
        
            switch (ch1)
                {
                case 'G': // GE
                    return 5;
                case 'E': case 'e': // EE, eval
                    return ch2 == ch1 ? 4 : 0;
                case 'S': // SE
                    return 3;
                case 'P' : case 'p': // prod
                    return 2;
                case 'C': // CC
                case 'R': // RTC
                    return 1;
                case 'D': case 'd': // dev, DC
                    return ch2 == 'e' ? 1 : 0;
                }
            }
        
        return -1;
        }
    
    // Accessor for the property "AnnounceMember"
    /**
     * Getter for property AnnounceMember.<p>
    * The AnnounceMember is the Member object that the ClusterService uses to
    * announce its presence. It is temporary, in that it is used to elicit a
    * time adjustment Message from the oldest Member in the cluster, from which
    * the RequestMember will be configured.
    * 
    * @see Cluster.configureDaemons
     */
    public com.tangosol.coherence.component.net.Member getAnnounceMember()
        {
        return __m_AnnounceMember;
        }
    
    // Accessor for the property "BroadcastAddresses"
    /**
     * Getter for property BroadcastAddresses.<p>
    * A Set<InetSocketAddress> used by the TCMP for broadcast in leu of
    * multicast. This set could be modified by the ClusterService and used by
    * the TCMP transport tier.
    * 
    * @see #addDynamicBroadcast
     */
    public java.util.Set getBroadcastAddresses()
        {
        return __m_BroadcastAddresses;
        }
    
    // Accessor for the property "BroadcastAddressesExpiry"
    /**
     * Getter for property BroadcastAddressesExpiry.<p>
    * The timestamp at which the broadcast address set expires and should be
    * cleared.
     */
    public long getBroadcastAddressesExpiry()
        {
        return __m_BroadcastAddressesExpiry;
        }
    
    // Accessor for the property "BroadcastCounter"
    /**
     * Getter for property BroadcastCounter.<p>
    * BroadcastCounter is the "try" number for broadcasting an "announce" or
    * "request" Message while trying to join a cluster. There are many events
    * that can reset the BroadcastCounter.
     */
    public int getBroadcastCounter()
        {
        return __m_BroadcastCounter;
        }
    
    // Accessor for the property "BroadcastLimit"
    /**
     * Getter for property BroadcastLimit.<p>
    * BroadcastLimit is the maximum number of "tries" for broadcasting an
    * "announce" or "request" Message while trying to join a cluster. The
    * BroadcastLimit is calculated from the broadcast timeout information.
    * 
    * *** LICENSE ***
    * Note: Setter contains important license-related functionality:
    * 
    * - sets up license binary (in the BroadcastTimestamp property) that is
    * used to "inject" licenses into the cluster
    * - calls resetBroadcastCounter() with the license data so that the list of
    * local licenses are known locally
     */
    public int getBroadcastLimit()
        {
        return __m_BroadcastLimit;
        }
    
    // Accessor for the property "BroadcastMode"
    /**
     * Getter for property BroadcastMode.<p>
    * *** LICENSE ***
    * WARNING: This property name is obfuscated.
    * 
    * 0=license not checked
    * 1=license is OK
    * 2=license is hacked
    * 
    * Set by setBroadcastLimit()
     */
    private int getBroadcastMode()
        {
        return __m_BroadcastMode;
        }
    
    // Accessor for the property "BroadcastNextMillis"
    /**
     * Getter for property BroadcastNextMillis.<p>
    * The BroadcastNextMillis value is the time (in local system millis) at
    * which the next announce or request broadcast Message is scheduled to be
    * sent.
     */
    public long getBroadcastNextMillis()
        {
        return __m_BroadcastNextMillis;
        }
    
    // Accessor for the property "BroadcastRepeatMillis"
    /**
     * Getter for property BroadcastRepeatMillis.<p>
    * The BroadcastRepeatMillis value specifies how many milliseconds between
    * repeatedly sending announce or request broadcast Messages.
     */
    public int getBroadcastRepeatMillis()
        {
        return __m_BroadcastRepeatMillis;
        }
    
    // Accessor for the property "BroadcastTimeoutMillis"
    /**
     * Getter for property BroadcastTimeoutMillis.<p>
    * The BroadcastTimeoutMillis value specifies how long the ClusterService
    * will send announce or request broadcast Messages without receiving a
    * reply before assuming that no one is there to reply.
     */
    public int getBroadcastTimeoutMillis()
        {
        return __m_BroadcastTimeoutMillis;
        }
    
    // Accessor for the property "BroadcastTimestamp"
    /**
     * Getter for property BroadcastTimestamp.<p>
    * *** LICENSE ***
    * WARNING: This property name is obfuscated.
    * 
    * This is the binary form of the license structure for the local node. When
    * this node tries to join a cluster, this value is copied to the
    * NewMemberRequestId message, written out as a binary, and read in as a
    * series of UID / product / limit-type / limit-value values.
    * 
    * Built by ClusterService.setBroadcastLimit(int).
     */
    public com.tangosol.util.Binary getBroadcastTimestamp()
        {
        return __m_BroadcastTimestamp;
        }
    
    // Accessor for the property "ClusterId"
    /**
     * Getter for property ClusterId.<p>
    * This property represents a checksum of the cluster information and with
    * high level of probability allows to differentiate clusters running on
    * different multicast groups or different WKA lists. It could be used to
    * detect a license abuse when the same production license is used on
    * multiple clusters concurrently.
     */
    public int getClusterId()
        {
        return 0;
        }
    
    // Accessor for the property "ClusterName"
    /**
     * Getter for property ClusterName.<p>
    * The name of the cluster. All joining members must present the same name
    * in order to join.
     */
    public String getClusterName()
        {
        // import Component.Net.Cluster;
        
        return ((Cluster) getCluster()).getClusterName();
        }
    
    // Declared at the super level
    /**
     * Getter for property DecoratedThreadName.<p>
    * (Calculated) Name of the service thread decorated with any additional
    * information that could be useful for thread dump analysis. The decorated
    * part is always trailing the full name delimited by the '|' character and
    * is truncated by the Logger.
     */
    public String getDecoratedThreadName()
        {
        // import Component.Net.Member;
        // import Component.Util.Daemon.QueueProcessor.Logger;
        
        int    nState = getState(); // different from ServiceState
        String sState = nState == STATE_JOINED ? "" :
            Logger.THREAD_NAME_DELIM + formatStateName(nState);
        
        Member member = getThisMember();
        if (member == null)
            {
            member = getRequestMember();
            if (member == null)
                {
                member = getAnnounceMember();
                }
            }
        
        String sMember = member == null ? "" :
            Logger.THREAD_NAME_DELIM + member.toString();
        
        return super.getDecoratedThreadName() + sState + sMember;
        }
    
    // Accessor for the property "DeliveryTimeoutMillis"
    /**
     * Getter for property DeliveryTimeoutMillis.<p>
    * The time interval in milliseconds that TCMP uses to determine Members
    * departure.
    * 
    * @see PacketPublisher#getResendTimeout
     */
    public int getDeliveryTimeoutMillis()
        {
        return __m_DeliveryTimeoutMillis;
        }
    
    // Accessor for the property "HeartbeatDelay"
    /**
     * Getter for property HeartbeatDelay.<p>
    * The number of milliseconds between heartbeats.
     */
    public int getHeartbeatDelay()
        {
        return __m_HeartbeatDelay;
        }
    
    // Accessor for the property "HeartbeatMemberSet"
    /**
     * Getter for property HeartbeatMemberSet.<p>
    * Set of Members that have this service.
     */
    public com.tangosol.coherence.component.net.MemberSet getHeartbeatMemberSet()
        {
        // import Component.Net.Cluster;
        // import Component.Net.Member;
        // import Component.Net.MemberSet;
        // import Component.Net.MemberSet.ActualMemberSet;
        // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet.MasterMemberSet;
        // import com.tangosol.util.Base;
        
        MemberSet setTo = __m_HeartbeatMemberSet;
        if (setTo == null)
            {
            setTo = new ActualMemberSet();
            setHeartbeatMemberSet(setTo);
            }
        
        // clear previous selection
        setTo.clear();
        
        // Pick members to heartbeat to; the goal here is to ensure that if we are
        // selected as a witness we have recent enough information to accurately
        // either confirm or reject the death request. We will confirm death if we
        // have not received an ACK within the heuristic timeout. Thus we must have
        // attempted to communicate to every node within that timeframe. Clearly,
        // a pending un-ACK'ed communication serves that purpose as well.
        
        MasterMemberSet setMembers = getClusterMemberSet();
        int             cMembers   = setMembers.size();
        if (cMembers > 1)
            {
            Cluster  cluster     = (Cluster) getCluster();
            Member   memberThis  = setMembers.getThisMember();
            int      cDelay      = getHeartbeatDelay();
            int      cTimeout    = getHeuristicTimeoutMillis() - cDelay;
            int      cDesired    = Math.max(cMembers * cDelay / cTimeout, 1);
            long     ldtNow      = Base.getSafeTimeMillis();
            long     ldtEligible = ldtNow      - (cTimeout >> 2); // 1/4 heuristic timeout
            long     ldtDanger   = ldtEligible - (cTimeout >> 1); // 3/4 heuristic timeout
            Member[] aMember     = (Member[]) setMembers.toArray(new Member[cMembers]);
            cMembers = aMember.length;
        
            // start at some random point within the list of members to avoid
            // repeatedly picking the first member in the list and also to avoid
            // a "heartbeat harmonic" from occurring across the cluster
            int iFirst = (int) (Math.random() * cMembers);
            int iLast  = iFirst + cMembers;
        
            for (int i = iFirst; i < iLast; ++i)
                {
                Member member = aMember[i >= cMembers ? i - cMembers : i];
                if (member != null && member != memberThis && !member.isDeaf())
                    {
                    // determine the last time that we sent something that needs to
                    // be ACK'd to that particular member
                    long ldtLastOut = member.getLastOutgoingMillis();
        
                    // determine the last time that we heard an ACK back from that
                    // particular member
                    long ldtLastIn = member.getLastIncomingMillis();
        
                    // determine if ACKs are pending
                    boolean fAckPending = ldtLastOut > ldtLastIn;
        
                    // determine if we haven't validated connection in quite a while
                    boolean fDanger      = !fAckPending && ldtLastIn < ldtDanger;
                    boolean fEligible    = !fAckPending && ldtLastIn < ldtEligible;
                    boolean fTrintDanger = cluster.isCommEndangered(member);
        
                    // Select enough eligible members such that we should speak to all
                    // members within the heuristic timeout.  Also force communication
                    // if we've violated this, or are close to trint rollover.
                    if ((fEligible && cDesired > 0) || fDanger || fTrintDanger)
                        {
                        setTo.add(member);
                        }
                    --cDesired;            
                    }
                }
            }
        
        return setTo;
        }
    
    // Accessor for the property "HeuristicTimeoutMillis"
    /**
     * Getter for property HeuristicTimeoutMillis.<p>
    * The HeuristicTimeout is used to confirm that a member is likely dead. 
    * For example, if one member detects that a suspect is past the packet
    * timeout, and others confirm that it is past the heuristic timeout it is
    * likely that it is dead.
    * 
    * The heuristic timeout must be less than or equal to half the packet
    * timeout.
     */
    public int getHeuristicTimeoutMillis()
        {
        return getDeliveryTimeoutMillis() >> 1;
        }
    
    // Accessor for the property "LastInterminableWarningMillis"
    /**
     * Getter for property LastInterminableWarningMillis.<p>
    * The time at which the last BroadcastCounter reset warning was logged.
    * This property is also used to determine that the announcing is going
    * nowhere fast and it has issued a warning to the log.
     */
    public long getLastInterminableWarningMillis()
        {
        return __m_LastInterminableWarningMillis;
        }
    
    // Accessor for the property "LastPanicUid"
    /**
     * Getter for property LastPanicUid.<p>
    * The UID of the last member caused the panic protocol activation.
     */
    public com.tangosol.util.UUID getLastPanicUid()
        {
        return __m_LastPanicUid;
        }
    
    // Accessor for the property "MaximumPacketLength"
    /**
     * Getter for property MaximumPacketLength.<p>
    * The max packet size in bytes that TCMP can receive (and can/will send).
    * Used only for packed-based transports.
     */
    public int getMaximumPacketLength()
        {
        return __m_MaximumPacketLength;
        }
    
    // Accessor for the property "MembershipReopen"
    /**
     * Getter for property MembershipReopen.<p>
    * Specifies the date/time (in millis) until when new members are not
    * allowed to join the cluster [service]. This covers such rare situations
    * as existence of a "zombie" senior (possibly with an island of members),
    * when the new membership is suspended until one of the islands is removed.
     */
    public long getMembershipReopen()
        {
        return __m_MembershipReopen;
        }
    
    public String getMemberStatsDescription(com.tangosol.coherence.component.net.Member member)
        {
        // import Component.Net.Member;
        
        return member.toString(Member.SHOW_STATS);
        }
    
    // Accessor for the property "MulticastTimeToLive"
    /**
     * Getter for property MulticastTimeToLive.<p>
    * The TTL for multicast communications.
     */
    public int getMulticastTimeToLive()
        {
        // import Component.Net.Cluster;
        
        return getWellKnownAddresses() == null ? 0 : ((Cluster) getCluster()).getDependencies().getGroupTimeToLive();
        }
    
    // Accessor for the property "PendingServiceJoining"
    /**
     * Getter for property PendingServiceJoining.<p>
    * This property stores a sparse array of ServiceJoining messages that this
    * senior Member is currently processing. By keeping track of these
    * messages, it is possible to know whether new members can join, or if
    * their joining needs to be temporarily deferred until the service joining
    * notification is complete.
    * 
    * When the senior Member receives a ServiceJoinRequest message it notifies
    * all other members using the ServiceJoining message (poll). Until all
    * members respond to that poll, no new Member can be accepted to the
    * cluster without compromising the synchronization of the service info. As
    * soon as the ServiceJoining poll completes, the requestor is notified and
    * all deferred NewMemberAcceptIdReply messages are delivered.
    * 
    * The array is indexed by the ServiceJoinRequest timestamp and is used to
    * remove Members that fail to respond to the senior's poll in a reasonable
    * period of time.
    * 
    * @see #DeferredAcceptMember property
    * @see #checkPendingJoinPolls
     */
    public com.tangosol.util.LongArray getPendingServiceJoining()
        {
        return __m_PendingServiceJoining;
        }
    
    // Accessor for the property "QuorumControl"
    /**
     * Getter for property QuorumControl.<p>
    * The QuorumControl manages the state and decision logic relating to
    * quorum-based membership decisions.
     */
    public ClusterService.QuorumControl getQuorumControl()
        {
        ClusterService.QuorumControl control = __m_QuorumControl;
        
        if (control == null)
            {
            control = (ClusterService.QuorumControl) _findChild("QuorumControl");
            setQuorumControl(control);
            }
        
        return control;
        }
    
    // Accessor for the property "RequestMember"
    /**
     * Getter for property RequestMember.<p>
    * The Member object being used to request an id. The RequestMember object
    * differs from the AnnounceMember in that the cluster time is known before
    * the RequestMember is configured, so that its age (and thus seniority) is
    * in cluster time.
     */
    public com.tangosol.coherence.component.net.Member getRequestMember()
        {
        return __m_RequestMember;
        }
    
    // Declared at the super level
    /**
     * Getter for property RequestTimeout.<p>
    * A default timeout value for polls and PriorityTasks that don't explicitly
    * specify the request timeout value.
     */
    public long getRequestTimeout()
        {
        // limit the ClusterService polls duration by the delivery timeout interval
        return getDeliveryTimeoutMillis();
        }
    
    // Accessor for the property "Service"
    /**
     * Getter for property Service.<p>
    * Indexed property of Service daemons running on <b>this</b> Member.
    * 
    * @volatile - read by PacketReceiver and TransportService
     */
    public com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid[] getService()
        {
        return __m_Service;
        }
    
    // Accessor for the property "Service"
    /**
     * Getter for property Service.<p>
    * Indexed property of Service daemons running on <b>this</b> Member.
    * 
    * @volatile - read by PacketReceiver and TransportService
     */
    public com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid getService(int i)
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid[] aService = getService();
        return aService == null || i >= aService.length ? null : aService[i];
        }
    
    // Accessor for the property "Service"
    /**
     * Getter for property Service.<p>
    * Indexed property of Service daemons running on <b>this</b> Member.
    * 
    * @volatile - read by PacketReceiver and TransportService
     */
    public com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid getService(String sName)
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        
        int cServices = getServiceCount();
        for (int i = 0; i < cServices; ++i)
            {
            com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid service = getService(i);
            if (service != null && sName.equals(service.getServiceName()))
                {
                return service;
                }
            }
        
        return null;
        }
    
    // Accessor for the property "ServiceContext"
    /**
     * Getter for property ServiceContext.<p>
    * A map of Service related PermissionInfo objects keyed by the service name.
     */
    public java.util.Map getServiceContext()
        {
        return __m_ServiceContext;
        }
    
    // Accessor for the property "ServiceCount"
    /**
     * Getter for property ServiceCount.<p>
    * Number of Service entries for <b>this</b> Member. Note that this is not
    * necessarily the actual number of Services running on this Member, but
    * rather the number of indexes to the indexed property "Service".
     */
    public int getServiceCount()
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid[] aService = getService();
        return aService == null ? 0 : aService.length;
        }
    
    // Accessor for the property "ServiceInfo"
    /**
     * Getter for property ServiceInfo.<p>
    * Indexed property of ServiceInfo objects that maintain data about Services
    * running in the cluster.
     */
    public com.tangosol.coherence.component.net.ServiceInfo[] getServiceInfo()
        {
        return __m_ServiceInfo;
        }
    
    // Accessor for the property "ServiceInfo"
    /**
     * Getter for property ServiceInfo.<p>
    * Indexed property of ServiceInfo objects that maintain data about Services
    * running in the cluster.
     */
    public com.tangosol.coherence.component.net.ServiceInfo getServiceInfo(int i)
        {
        // import Component.Net.ServiceInfo;
        
        ServiceInfo[] ainfo = getServiceInfo();
        return ainfo == null || i >= ainfo.length ? null : ainfo[i];
        }
    
    // Accessor for the property "ServiceInfo"
    /**
     * Getter for property ServiceInfo.<p>
    * Indexed property of ServiceInfo objects that maintain data about Services
    * running in the cluster.
     */
    public com.tangosol.coherence.component.net.ServiceInfo getServiceInfo(String sName)
        {
        // import Component.Net.ServiceInfo;
        
        int cInfo = getServiceInfoCount();
        for (int i = 0; i < cInfo; ++i)
            {
            ServiceInfo info = getServiceInfo(i);
            if (info != null && sName.equals(info.getServiceName()))
                {
                return info;
                }
            }
        
        return null;
        }
    
    // Accessor for the property "ServiceInfoCount"
    /**
     * Getter for property ServiceInfoCount.<p>
    * Number of known Services
     */
    public int getServiceInfoCount()
        {
        // import Component.Net.ServiceInfo;
        
        ServiceInfo[] ainfo = getServiceInfo();
        return ainfo == null ? 0 : ainfo.length;
        }
    
    // Declared at the super level
    /**
     * Getter for property ServiceOldestMember.<p>
    * Returns the Member object for the senior Member, or null if this Member
    * has not yet joined the cluster.
     */
    public com.tangosol.coherence.component.net.Member getServiceOldestMember()
        {
        return getClusterOldestMember();
        }
    
    // Declared at the super level
    /**
     * Getter for property ServiceStateName.<p>
    * Calculated helper property; returns a human-readable description of the
    * ServiceState property.
     */
    public String getServiceStateName()
        {
        return super.getServiceStateName() + ", " + formatStateName(getState());
        }
    
    // Declared at the super level
    /**
     * Getter for property ServiceVersion.<p>
    * The version string for this Service implementation. This property is
    * currently used *only* on the ClusterService and is left here only for the
    * backward compatibility.
     */
    public String getServiceVersion()
        {
        // import Component.Application.Console.Coherence;
        
        // Note: failure to load form the MANIFEST will result in the version being 'n/a'
        
        String sVersion = Coherence.VERSION_INTERNAL;
        return sVersion == null || sVersion.isEmpty() || "n/a".equals(sVersion) ? VERSION : sVersion;
        }
    
    /**
     * @return a set of "slow" members; null if none exist
     */
    public java.util.Set getSlowMembers()
        {
        // import Component.Net.Member;
        // import java.util.HashSet;
        // import java.util.Iterator;
        // import java.util.Set;
        
        Set setMembers = null;
        
        for (Iterator iter = getClusterMemberSet().iterator(); iter.hasNext();)
            {
            Member member = (Member) iter.next();
            if (isSlow(member))
                {
                if (setMembers == null)
                    {
                    setMembers = new HashSet();
                    }
                setMembers.add(member);
                }
            }
        
        return setMembers;
        }
    
    // Accessor for the property "State"
    /**
     * Getter for property State.<p>
    * State of the ClusterService; one of:
    *     STATE_ANNOUNCE
    *     STATE_JOINING
    *     STATE_JOINED
     */
    public int getState()
        {
        return __m_State;
        }
    
    // Accessor for the property "StatsJoinRequests"
    /**
     * Getter for property StatsJoinRequests.<p>
    * Statistics: total number of received join requests
     */
    public long getStatsJoinRequests()
        {
        return __m_StatsJoinRequests;
        }
    
    // Accessor for the property "StatsMembersDepartureCount"
    /**
     * Getter for property StatsMembersDepartureCount.<p>
    * The number of member disconnects from the cluster.  This value is exposed
    * on the cluster MBean.
     */
    public long getStatsMembersDepartureCount()
        {
        return __m_StatsMembersDepartureCount;
        }
    
    // Accessor for the property "TcpRing"
    /**
     * Getter for property TcpRing.<p>
    * The ClusterService's TcpRing.
     */
    public ClusterService.TcpRing getTcpRing()
        {
        ClusterService.TcpRing ring = __m_TcpRing;
        
        if (ring == null)
            {
            ring = (ClusterService.TcpRing) _findChild("TcpRing");
            setTcpRing(ring);
            }
        
        return ring;
        }
    
    // Accessor for the property "Timestamp"
    /**
     * Getter for property Timestamp.<p>
    * Returns the current "cluster time", which is like the
    * Base.getSafeTimeMillis() except that the cluster time is (almost) the
    * same for all Members in the cluster.
     */
    public long getTimestamp()
        {
        // import com.tangosol.util.Base;
        
        return calcTimestamp(Base.getSafeTimeMillis());
        }
    
    // Accessor for the property "TimestampAdjustment"
    /**
     * Getter for property TimestampAdjustment.<p>
    * The number of milliseconds off the cluster time is from the local system
    * time.
     */
    public long getTimestampAdjustment()
        {
        return __m_TimestampAdjustment;
        }
    
    // Accessor for the property "TimestampMaxVariance"
    /**
     * Getter for property TimestampMaxVariance.<p>
    * The maximum number of apparent milliseconds difference allowed between
    * send and receive time when determining how far off the cluster time is
    * from the local system time.
     */
    public int getTimestampMaxVariance()
        {
        return __m_TimestampMaxVariance;
        }
    
    // Declared at the super level
    /**
     * Getter for property WaitMillis.<p>
    * The number of milliseconds that the daemon will wait for notification.
    * Zero means to wait indefinitely. Negative value means to skip waiting
    * altogether.
    * 
    * @see #onWait
     */
    public long getWaitMillis()
        {
        // import com.tangosol.util.Base;
        
        long cWait1 = super.getWaitMillis();
        long cWait2 = Math.max(1L,
            getBroadcastNextMillis() - Base.getSafeTimeMillis());
        return cWait1 <= 0L ? cWait2 : Math.min(cWait1, cWait2);
        }
    
    // Accessor for the property "WellKnownAddresses"
    /**
     * Getter for property WellKnownAddresses.<p>
    * The well-known-addresses or null for multicast
     */
    public java.util.Set getWellKnownAddresses()
        {
        return __m_WellKnownAddresses;
        }
    
    /**
     * Choose a witness that could confirm the specified member's departure.
    * 
    * @return a set of most reliable witness Member; null if none exist
     */
    protected com.tangosol.coherence.component.net.MemberSet getWitnessMemberSet(com.tangosol.coherence.component.net.Member memberSuspect)
        {
        // import Component.Net.Member;
        // import Component.Net.MemberSet.ActualMemberSet;
        // import com.tangosol.util.Base;
        // import com.oracle.coherence.common.collections.Arrays as com.oracle.coherence.common.collections.Arrays;
        
        // Our goal is to select two witnesses of greater importance to us.
        // If these cannot be found then we will accept witnesses of lesser
        // strength and importance.
        
        // A strong witness will:
        // - reside on a third machine
        // - not be a suspect itself
        // - not be slow
        
        // we devide potential witnesses into the following categories
        
        final int GREATER = 0; // by importance
        final int EQUAL   = 1; // by importance
        final int LESSER  = 2; // by importance
        final int WEAK    = 3; // collocated
        final int POOR    = 4; // questionably responsive
        final int NIL     = 5; // unuseable as a witness
        
        Object[] aoMember        = getClusterMemberSet().toArray();
        Member   memberThis      = getThisMember();
        int      nMachineThis    = memberThis.getMachineId();
        int      nMachineSuspect = memberSuspect.getMachineId();
        
        // The witnesses selection must include an element of randomness to avoid
        // having all nodes select the same witness set for same suspect. Using
        // the same witnesses would increase the risk associated with an erroneous
        // rejection and could lead to a larger portion of the cluster self-terminating.
        
        com.oracle.coherence.common.collections.Arrays.shuffle(aoMember);
        
        ActualMemberSet setWitness = new ActualMemberSet();
        for (int iCatDesired = GREATER, iCatBest = NIL, s = 0; iCatDesired < NIL; iCatDesired = Math.max(iCatDesired + 1, iCatBest))
            {
            for (int i = s; i < aoMember.length; ++i)
                {
                Member member   = (Member) aoMember[i];
                int    iCatThat = NIL;
        
                if (member != null)
                    {
                    int nMachineThat = member.getMachineId();
                    int iComp        = compareImportance(member);
        
                    iCatThat = member == memberThis || member == memberSuspect || member.isDeaf() ? NIL
                             : isSlow(member) || isRecentlyHeuristicallyDead(member)              ? POOR
                             : nMachineThat == nMachineThis || nMachineThat == nMachineSuspect    ? WEAK
                             : iComp  > 0                                                         ? LESSER
                             : iComp == 0                                                         ? EQUAL
                             :                                                                      GREATER;
        
                    if (iCatThat <= iCatDesired)
                        {
                        setWitness.add(member);
                        if (setWitness.size() == 2)
                            {
                            return setWitness;
                            }
        
                        iCatThat = NIL;
                        }
                    else if (iCatThat < iCatBest)
                        {
                        iCatBest = iCatThat; // skip unneeded passes on outer loop
                        }
                    }
        
                if (iCatThat == NIL)
                    {
                    // avoid any processing of this member on future iterations
                    aoMember[i] = null;
                    if (i == s)
                        {
                        ++s; // skip unneeded head-scan on future inner loop
                        }
                    }
                }
            }
        
        return setWitness.isEmpty() ? null : setWitness;
        }
    
    // Accessor for the property "WkaHashCode"
    /**
     * Getter for property WkaHashCode.<p>
    * Helper method to compute the hashCode for the unordered WKA List.  If the
    * WKA list is empty or null a value of 0 is returned.
     */
    public int getWkaHashCode()
        {
        return isWkaEnabled() ? getWellKnownAddresses().hashCode() : 0;
        }
    
    // Accessor for the property "WkaMap"
    /**
     * Getter for property WkaMap.<p>
    * *** LICENSE ***
    * WARNING: This property name is obfuscated.
    * 
    * Map keyed by UID, with each value an int[] of:
    * [0] edition (0=DC, ..5=DGE)
    * [1] limit type (0=site, 1=srv, 2=sock, 3=cpu, 4=seat, 5=user)
    * [2] limit value
    * 
    * Maintained by resetBroadcastCounter()
     */
    public java.util.Map getWkaMap()
        {
        return __m_WkaMap;
        }
    
    /**
     * Send heartbeats to all members in the set. May be called from outside
    * ClusterService.
     */
    public void heartbeat(com.tangosol.coherence.component.net.MemberSet setMember)
        {
        // import Component.Net.Member;
        // import Component.Net.MemberSet.SingleMemberSet;
        // import java.util.Iterator;
        
        if (setMember == null || setMember.isEmpty())
            {
            return;
            }
        
        long ldtLastReceived = calcTimestamp(getThisMember().getLastIncomingMillis());
        for (Iterator iter = setMember.iterator(); iter.hasNext(); )
            {
            // send a uni-point heartbeat (there's no flow control for multi-point)
            ClusterService.MemberHeartbeat msg = (ClusterService.MemberHeartbeat) instantiateMessage("MemberHeartbeat");
            msg.setToMemberSet(SingleMemberSet.instantiate((Member) iter.next()));
            msg.setLastReceivedMillis(ldtLastReceived);
            post(msg);
            }
        
        // make this method callable from outside ClusterService
        flush();
        }
    
    public int indexOfService(String sName)
        {
        // import Component.Net.ServiceInfo;
        
        _assert(sName != null);
        
        ServiceInfo[] ainfo = getServiceInfo();
        for (int i = 0, c = ainfo.length; i < c; ++i)
            {
            ServiceInfo info = ainfo[i];
            if (info != null && info.getServiceName().equals(sName))
                {
                return i;
                }
            }
        
        return -1;
        }
    
    // Declared at the super level
    /**
     * Bind this service to a transport: datagram (using Publisher) or a
    * MessageBus (using MessageHandler). Called on the service thread.
     */
    public void initializeTransport()
        {
        // import Component.Net.Cluster;
        
        // currently the ClusterService is "datagram" only;
        setMessagePublisher(((Cluster) getCluster()).getPublisher());
        
        // don't call super
        }
    
    public com.tangosol.coherence.component.net.Member instantiateMember()
        {
        // import Component.Net.Member;
        
        Member member = new Member();
        member.initCommSupport();
        return member;
        }
    
    /**
     * Check whether or not there is sufficient data to assume the member's
    * departure.
    * 
    * @return true if there is no data that contradicts the assumption;
    *               false if there is data that suggests that the member is
    * alive
     */
    public boolean isHeuristicallyDead(com.tangosol.coherence.component.net.Member member)
        {
        // import com.tangosol.util.Base;
        
        if (member == null || member.isDeaf())
            {
            return true;
            }
        
        // TODO: this may need to be a three state response: "confirm, reject, obstain"
        // based on the value of the member.getLastOutgoingMillis()
        
        long lCurrentMillis = Base.getSafeTimeMillis(); // TODO: use HeartbeatCount instead
        long lCutoffMillis  = lCurrentMillis - getHeuristicTimeoutMillis();
        long lLastInMillis  = member.getLastIncomingMillis();
        
        return lLastInMillis < lCutoffMillis;
        }
    
    // Accessor for the property "MembershipSuspended"
    /**
     * Getter for property MembershipSuspended.<p>
    * Specifies whether or not new members are allowed to join the cluster
    * [service]. This covers such rare situations as existence of a "zombie"
    * senior (possibly with an island of members), when the new membership is
    * suspended until one of the islands is removed.
     */
    public boolean isMembershipSuspended()
        {
        // import com.tangosol.util.Base;
        
        // membership is suspended until the reopen time has been reached,
        // or while there are convicted members that have not been killed
        return Base.getSafeTimeMillis() <= getMembershipReopen() ||
             getQuorumControl().isClusterSuspended();
        }
    
    /**
     * Check whether or not there is sufficient data to assume the member's
    * departure.
    * 
    * @return true if there has been a heuristic timeout within the last packet
    * delivery timeout window.
     */
    public boolean isRecentlyHeuristicallyDead(com.tangosol.coherence.component.net.Member member)
        {
        // import com.tangosol.util.Base;
        
        if (member == null || member.isDeaf() || isHeuristicallyDead(member))
            {
            return true;
            }
        
        return member.getLastHeuristicDeathMillis() >= Base.getSafeTimeMillis() - getDeliveryTimeoutMillis();
        }
    
    /**
     * Check whether or not there is data that indicates to a general slowness
    * of the specifed Member.  A Member is considered to be "slow" if within
    * last timeout interval there was a moment when a gap beteeen successfully
    * delivered and undelivered packets was larger than half of the timeout
    * interval. The slowness is usually caused by either flaky network cards or
    * long and frequent GC pauses.
    * 
    * @see #verifyMemberSlow
     */
    public boolean isSlow(com.tangosol.coherence.component.net.Member member)
        {
        // import com.tangosol.util.Base;
        
        long lCurrentMillis  = Base.getSafeTimeMillis();
        long lLastSlowMillis = member.getLastSlowMillis();
        int  cTimeoutMillis  = getDeliveryTimeoutMillis();
        
        return lLastSlowMillis > lCurrentMillis - cTimeoutMillis;
        }
    
    // Declared at the super level
    /**
     * Getter for property SuspendedFully.<p>
    * As opposed to the Suspended this property indicates if the service has
    * finished the process of suspending itself, i.e. it has ensured that all
    * in-flight operations have completed.   Note this only refers to
    * operations which are "owned" by this service instance as we cannot
    * locally prevent new operations coming in from the outside and must still
    * honor them even while fully suspended in order to let our peers also
    * fully suspend.  Thus from a clustered perspective a service is not
    * finished suspending until all its members have become fully suspended.
    * 
    * This property has no specific meaning if the service is not suspend.
     */
    public boolean isSuspendedFully()
        {
        // for ClusterService there are no request types to bleed out, so we are fully suspended as soon as we're suspended
        return isSuspended();
        }
    
    /**
     * Check if the specified version is compatible with the version of this
    * node.
     */
    public boolean isVersionCompatible(String sVersion)
        {
        // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet as com.tangosol.coherence.component.net.memberSet.actualMemberSet.ServiceMemberSet;
        // import com.tangosol.util.Base;
        
        String sVersionThis = getServiceVersion();
        
        if (Base.equals(sVersionThis, sVersion))
            {
            return true;
            }
        
        int nThis = com.tangosol.coherence.component.net.memberSet.actualMemberSet.ServiceMemberSet.parseVersion(sVersionThis);
        int nThat = com.tangosol.coherence.component.net.memberSet.actualMemberSet.ServiceMemberSet.parseVersion(sVersion);
        
        // 1. Only the newer version node gets to make an incompatibility decision
        // 2. Only if the versions are separated by the "barrier", they are incompatible
        return nThis <= nThat
            || nThat >= com.tangosol.coherence.component.net.memberSet.actualMemberSet.ServiceMemberSet.parseVersion(VERSION_BARRIER);
        }
    
    /**
     * Check if the specified version is older than the version of this node.
     */
    public boolean isVersionOlder(String sVersion)
        {
        // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet as com.tangosol.coherence.component.net.memberSet.actualMemberSet.ServiceMemberSet;
        // import com.tangosol.util.Base;
        
        String sVersionThis = getServiceVersion();
        
        return !Base.equals(sVersionThis, sVersion) &&
               com.tangosol.coherence.component.net.memberSet.actualMemberSet.ServiceMemberSet.parseVersion(sVersion) < com.tangosol.coherence.component.net.memberSet.actualMemberSet.ServiceMemberSet.parseVersion(sVersionThis);
        }
    
    /**
     * Determines whether or not this address has a well known address. Note
    * that if the multicast is enabled, every address is considered to be
    * well-known.
     */
    public boolean isWellKnown(java.net.InetAddress addrInet)
        {
        // import java.net.InetSocketAddress;
        
        return !isWkaEnabled() // Wka disabled == Multicast enabled
            || getWellKnownAddresses().contains(new InetSocketAddress(addrInet, getCluster().getDependencies().getGroupPort()));
        }
    
    /**
     * Determines whether or not this node has a well known address. Note that
    * if the multicast is enabled, every node is considered to be well-known.
    * This method is mostly used to determine whether or not a node is _not_ a
    * WKA, which would mean that multicast is disabled and the node is not in
    * the WKA list.
     */
    public boolean isWellKnown(com.tangosol.coherence.component.net.Member member)
        {
        return !isWkaEnabled() // Wka disabled == Multicast enabled
            || getWellKnownAddresses().contains(member.getSocketAddress());
        }
    
    // Accessor for the property "WkaEnabled"
    /**
     * Getter for property WkaEnabled.<p>
    * True if the this member is configured to use WKA, false otherwise.
     */
    public boolean isWkaEnabled()
        {
        return getWellKnownAddresses() != null;
        }
    
    // Declared at the super level
    /**
     * Notify the ClusterService (and other Members of this service) that this
    * member has finished starting
    * and has fully joined the service.
    * 
    * Called on the service thread only.
     */
    protected void notifyServiceJoined()
        {
        // Note: ClusterService does not issue ServiceJoined; MemberJoined
        //       is used in its place.
        }
    
    // Declared at the super level
    /**
     * Event notification called once the daemon's thread starts and before the
    * daemon thread goes into the "wait - perform" loop. Unlike the
    * <code>onInit()</code> event, this method executes on the daemon's thread.
    * 
    * Note1: this method is called while the caller's thread is still waiting
    * for a notification to  "unblock" itself.
    * Note2: any exception thrown by this method will terminate the thread
    * immediately
    * Note3: most commonly, the assumption is that onEnter() method executes
    * while the service state is "SERVICE_INITIAL", so it is important for the
    * subclasses to make the super.onEnter() call at the very end
     */
    protected void onEnter()
        {
        // import com.tangosol.util.Base;
        
        if (isWkaEnabled())
            {
            int n = getCluster().getDependencies().getEdition();
            if (n < 3 && n != 1) // allow 1=CC/RTC, 2=SE, 3=CE, 4=EE and 5=GE
                {
                throw Base.ensureRuntimeException(null,
                        "WKA (unicast-only) clustering is not available.");
                }
            }
        
        setBroadcastLimit(getBroadcastTimeoutMillis() / getBroadcastRepeatMillis() + 1);
        setBroadcastNextMillis(Base.getSafeTimeMillis());
        
        super.onEnter();
        }
    
    // Declared at the super level
    /**
     * This event occurs when an exception is thrown from onEnter, onWait,
    * onNotify and onExit.
    * 
    * If the exception should terminate the daemon, call stop(). The default
    * implementation prints debugging information and terminates the daemon.
    * 
    * @param e  the Throwable object (a RuntimeException or an Error)
    * 
    * @throws RuntimeException may be thrown; will terminate the daemon
    * @throws Error may be thrown; will terminate the daemon
     */
    public void onException(Throwable e)
        {
        // import com.tangosol.net.internal.ClusterJoinException;
        
        switch (getState())
            {
            default:
            case STATE_ANNOUNCE:
                super.onException(e);
                break;
        
            case STATE_JOINING:
                if (e instanceof ClusterJoinException)
                    {
                    // Service.start() will wrap and rethrow
                    setStartException(e);
                    }
                else
                    {
                    _trace("StopJoining " + toString() + " due to unhandled exception: ", 1);
                    _trace(e);
                    }
        
                onStopJoining();
                break;
        
            case STATE_JOINED:
                _trace("StopRunning " + toString() + " due to unhandled exception: ", 1);
                _trace(e);
                onStopRunning();
                break;
            }
        }
    
    // Declared at the super level
    /**
     * Event notification called right before the daemon thread terminates. This
    * method is guaranteed to be called only once and on the daemon's thread.
     */
    protected void onExit()
        {
        getTcpRing().close();
        super.onExit();
        }
    
    // Declared at the super level
    /**
     * The "component has been initialized" method-notification (kind of
    * WM_NCCREATE event) called out of setConstructed() for the topmost
    * component and that in turn notifies all the children. <p>
    * 
    * This notification gets called before the control returns back to this
    * component instantiator (using <code>new Component.X()</code> or
    * <code>_newInstance(sName)</code>) and on the same thread. In addition,
    * visual components have a "posted" notification <code>onInitUI</code> that
    * is called after (or at the same time as)  the control returns back to the
    * instatiator and possibly on a different thread.
     */
    public void onInit()
        {
        // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet.MasterMemberSet;
        
        MasterMemberSet setMember = new MasterMemberSet();
        
        setClusterMemberSet(setMember);
        setServiceMemberSet(setMember);
        
        ensureServiceInfo(0, getServiceName(), getServiceType());
        setService(0, this);
        
        super.onInit();
        
        // ClusterService will wait on TcpRing's selector
        // wire together notifiers
        ClusterService.TcpRing ring = getTcpRing();
        setNotifier(ring);
        ((ClusterService.ReceiveQueue) getQueue()).setNotifier(ring);
        }
    
    // Declared at the super level
    /**
     * Event notification for performing low frequency periodic maintenance
    * tasks.  The interval is dictated by the WaitMillis property, 
    * 
    * This is used for tasks which have a high enough cost that it is not
    * reasonable to perform them on every call to onWait() since it could be
    * called with a high frequency in the presence of work-loads with fast
    * oscillation between onWait() and onNotify().  As an example a single
    * threaded client could produce such a load.
     */
    protected void onInterval()
        {
        // import com.oracle.coherence.common.util.Duration;
        // import com.tangosol.util.Base;
        
        /*
        long ldtNow = Base.getLastSafeTimeMillis();
        
        long cMillisDelay = ldtNow - getIntervalNextMillis();
        if (cMillisDelay > 1000L)
            {
            // we're more then 1s late, local GC may have occured
            _trace("local pause of " + new Duration(cMillisDelay * 1000000L)
                + " detected; (probable local GC)", 2);
        
            newTracingSpan("localdelay", null)
                .withStartTimestamp((System.currentTimeMillis()-cMillisDelay) * 1000L)
                .start().finish();
            }
        */
        super.onInterval();
        }
    
    public void onMemberJoined(com.tangosol.coherence.component.net.Member member)
        {
        // import Component.Net.Cluster;
        // import Component.Net.Member;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid$NotifyMemberJoined as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyMemberJoined;
        // import com.tangosol.net.MemberEvent;
        // import com.tangosol.net.management.Registry;
        
        // notifications are _not_ made when this Member is the Member
        // that caused the notification
        // Note: getThisMember() could be null if this member is still joining
        
        Member memberThis = getThisMember();
        if (memberThis != null && member != memberThis)
            {
            long ldtMember  = member.getTimestamp();
            long ldtCluster = getTimestamp();
            if (ldtMember > ldtCluster)
                {
                // senior's clock is ahead of ours, sync to it
                // this also ensures that if we ever become senior we'll hand out timestamps
                // which are also greater then this timestamp
                setTimestampAdjustment(getTimestampAdjustment() + (ldtMember - ldtCluster));
                }
        
            _trace(member + " joined Cluster with senior member " +
                getClusterOldestMember().getId(), 3);
        
            Cluster cluster = (Cluster) getCluster();
            cluster.onMemberJoined(member);
        
            if (getClusterMemberSet().isServiceJoined(memberThis.getId()))
                {
                // notify the TcpRing (only if joined); see NewMemberInduct
                getTcpRing().onMemberJoined(member);
                }
        
            // notify all running services
            for (int i = 0, c = getServiceCount(); i < c; ++i)
                {
                com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid service = getService(i);
                if (service != null)
                    {
                    com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyMemberJoined msg = (com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyMemberJoined)
                            service.instantiateMessage("NotifyMemberJoined");
                    msg.setNotifyMember(member);
                    service.send(msg);
                    }
                }
        
            getQuorumControl().onMemberJoined(member);
            }
        
        // there is no reason to dispatch a member event
        // until the cluster service is started
        if (isAcceptingClients())
            {
            dispatchMemberEvent(member, MemberEvent.MEMBER_JOINED);
            dispatchNotification(Registry.CLUSTER_TYPE, "member.joined",
                "Member " + member.getId() + " joined", member.toString());
            }
        }
    
    public void onMemberLeaving(com.tangosol.coherence.component.net.Member member)
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid$NotifyMemberLeaving as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyMemberLeaving;
        // import com.tangosol.net.MemberEvent;
        
        for (int i = 0, c = getServiceCount(); i < c; ++i)
            {
            com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid service = getService(i);
            if (service != null)
                {
                com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyMemberLeaving msg = (com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyMemberLeaving)
                        service.instantiateMessage("NotifyMemberLeaving");
                msg.setNotifyMember(member);
                service.send(msg);
                }
            }
        
        dispatchMemberEvent(member, MemberEvent.MEMBER_LEAVING);
        }
    
    /**
     * Called on the ClusterService thread to signal that the specified member
    * has left the cluster.
    * 
    * @param member  the member that has left the cluster
     */
    protected void onMemberLeft(com.tangosol.coherence.component.net.Member member)
        {
        // import Component.Net.Cluster;
        // import Component.Net.Member;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid$NotifyMemberLeft as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyMemberLeft;
        // import com.tangosol.net.MemberEvent;
        // import com.tangosol.net.management.Registry;
        
        member.setLeaving(true); // see ensureMemberLeft
        
        Member memberSenior = getClusterOldestMember();
        _trace(member + " left Cluster with senior member " + memberSenior.getId(), 3);
        
        // clean up if the late member was convicted
        getQuorumControl().onMemberLeft(member);
        
        // clean up just in case it was a non WKA senior
        removeDynamicBroadcast(member);
        
        // notify TCMP daemons
        Cluster cluster = (Cluster) getCluster();
        cluster.onMemberLeft(member);
        
        // notify TcpRing
        getTcpRing().onMemberLeft(member);
        
        // notify all services
        for (int i = 0, c = getServiceCount(); i < c; ++i)
            {
            com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid service = getService(i);
            if (service != null)
                {
                com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyMemberLeft msg = (com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyMemberLeft)
                        service.instantiateMessage("NotifyMemberLeft");
                msg.setNotifyMember(member);
                service.send(msg);
                }
            }
        
        dispatchMemberEvent(member, MemberEvent.MEMBER_LEFT);
        dispatchNotification(Registry.CLUSTER_TYPE, "member.left",
            "Member " + member.getId() + " left", member.toString());
        setStatsMembersDepartureCount(getStatsMembersDepartureCount() + 1);
        }
    
    /**
     * Reject the cluster join. The positive number that is less then 8160
    * (MasterMemberSet.MAX_MEMBER) represents the excess of actual CPUs over
    * the limit allowed by the license. Negative numbers represent a rejection
    * reason that is figured out locally. Other reasons are:
    *  - MAX_MEMBERS + 1: "Not authorized"
    *  - MAX_MEMBERS + 2: "Version mismatch"
    *  - MAX_MEMBERS + 3: "Packet size mismatch"
    *  - MAX_MEMBERS + 4: "Senior license has expired"
    *  - MAX_MEMBERS + 5: "Senior is not authorized"
    *  - MAX_MEMBERS + 6: "WKA mismatch"
    *  - MAX_MEMBERS + 7: "Cluster name mismatch"
    *  - MAX_MEMBERS + 8: "Product Edition mismatch"
    *  - MAX_MEMBERS + 9: "License Type mismatch"
    * 
    * @param nReason  the reason id for rejecting entry into the cluster
    * @param rejector   the member responsible for this member being rejected,
    * or null if the rejection was internal
     */
    public void onMemberRejected(int nReason, com.tangosol.coherence.component.net.Member rejector)
        {
        // import Component.Application.Console.Coherence;
        // import Component.Net.Cluster;
        // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet.MasterMemberSet;
        // import Component.Net.Packet.MessagePacket;
        // import com.tangosol.net.ClusterDependencies;
        
        String sError;
        switch (nReason)
            {
            case REJECT_SIZE:
                {
                // recycle set overflow
                int cbMax = getMaximumPacketLength();
                sError = "The cluster has exceeded its maximum size.  This limit is "
                    + "based on the configured maximum packet length, with the "
                    + "current value of " + cbMax + " bytes the cluster size limit "
                    + "is approximately " + MessagePacket.calcMaxMembers(cbMax)
                    + " members.";
                }
                break;
        
            case REJECT_AUTHORIZE:
                sError = "This member is not authorized to join the cluster.";
                break;
        
            case REJECT_VERSION:
                sError = "This member could not join the cluster because of "
                    + "an incompatibility between the cluster protocol used by this "
                    + "member and the one being used by the rest of the cluster.  This "
                    + "is most likely caused by a Coherence version mismatch, or by "
                    + "mismatched protocol filters (e.g. compression, or encryption)." ;
                break;
        
            case REJECT_PACKET_MAX:
                sError = "This member could not join the cluster because of "
                    +  "a configuration mismatch between this member "
                    +  "and the configuration being used by the rest of the cluster. "
                    +  "The maximum packet size (" + getMaximumPacketLength()
                    +  ") for this member does not match the maximum packet size that "
                    +  "the running cluster is using.";
                break;
        
            case REJECT_LICENSE_EXPIRED:
                sError = "This member could not join the cluster because the "
                    + "senior member's license has expired.";
                break;
        
            case REJECT_SENIOR:
                sError = "This member will not join the cluster because the "
                    + "senior member's IP address is not an authorized host.";
                break;
        
            case REJECT_WKA:
                {
                boolean fWka = isWkaEnabled();
                sError = "This member could not join the cluster because of "
                    + "a configuration mismatch between this member "
                    + "and the configuration being used by the running cluster. "
                    + "This member is" + (fWka ? " " : " not ")
                    + "configured to use WKA, but the running cluster is"
                    + (fWka ? " not." : ".");
                }
                break;
        
            case REJECT_CLUSTER_NAME:
                sError = "This member could not join the cluster because of "
                    + "a configuration mismatch between this member "
                    + "and the configuration being used by the rest of the cluster. "
                    + "This member specified a cluster name of \"" + getClusterName() + "\" "
                    + "which did not match the name of the running cluster"
                    + (rejector == null ? "" : " " + rejector.getClusterName() + "\"")
                    + ". This indicates that there are multiple clusters on this network attempting to use overlapping "
                    + "network configurations.";
                break;
        
            case REJECT_EDITION:
                {
                ClusterDependencies cfg          = ((Cluster) getCluster()).getDependencies();
                String              sEditionName = Coherence.EDITION_NAMES[cfg.getEdition()];
                sError = "This member could not join the cluster because of "
                    + "a mismatch between Coherence product editions. This "
                    + "member was attempting to run as a " + Coherence.TITLE + " "
                    + sEditionName + " edition cluster.";
                }
                break;
        
            case REJECT_LICENSE_TYPE:
                {
                ClusterDependencies cfg      = ((Cluster) getCluster()).getDependencies();
                String              modeName = Coherence.MODE_NAMES[cfg.getMode()];
                sError = "This member could not join the cluster because of "
                    + "a mismatch between Coherence license types. This "
                    + "member was attempting to run in " + modeName
                    + " mode.";
                }
                break;
        
            case REJECT_RESTART:
                // we've been instructed to restart the join protocol
                resetBroadcastCounter("restarting the join protocol", null);
                setTimestampAdjustment(0);
                setRequestMember(null);
                setState(STATE_ANNOUNCE);
                return;
        
            case REJECT_QUORUM:
                {
                sError = "This member was prevented from joining the cluster by "
                    + "the existing cluster's action policy.";
                }
                break;
            
            default:
                sError = "Unknown reject reason code of " + nReason;
                break;
            }
        
        if (sError != null)
            {
            if (rejector != null)
                {
                sError += " Rejected by " + rejector + ".";
                }
            _trace(sError, 1);
            }
        
        // this Member cannot join the cluster
        onStopJoining();
        }
    
    /**
     * Called on the service thread to indicate that the specified set of
    * members have been determined to be "timed-out", and should be terminated.
    * 
    * @param setTimedOut   the set of members that have been determined to have
    * timed out
     */
    public void onMembersTimedOut(java.util.Set setTimedOut)
        {
        getQuorumControl().onMembersTimedOut(setTimedOut);
        }
    
    // Declared at the super level
    /**
     * This event occurs when an exception is thrown while reading message
    * contents.
    * 
    * If the exception should terminate the daemon, call stop(). The default
    * implementation prints debugging information and terminates the daemon.
    * Returning from this method will result in the partially instantiated
    * message being processed, which should be avoided unless the service is
    * prepared to take this into account.
    * 
    * @param e      the Throwable object (a RuntimeException or an Error)
    * @param msg  the partially constructed message which triggered the
    * exception
    * 
    * @return true if the partial message should be processed, false if it
    * should be skipped.
    * 
    * @throws RuntimeException may be thrown; will terminate the daemon
    * @throws Error may be thrown; will terminate the daemon
     */
    protected boolean onMessageReadException(Throwable e, com.tangosol.coherence.component.net.Message msg)
        {
        // import Component.Net.Message.DiscoveryMessage;
        
        if (msg instanceof DiscoveryMessage)
            {
            if (e instanceof SecurityException)
                {
                // unlike Service.onMessageReadException, we only treat this as a warning
                // because we're blocking an unknown/untrusted member from joining the cluster
                // but they are at least asking to get in
                _trace("SecurityException received while reading message " +
                       msg.get_Name() + "\n" + e, 2);
                }
        
            // mark as a read error and allow the message to be processed
            ((DiscoveryMessage) msg).setReadError(true);
            return true; // allow processing
            }
        else
            {
            return super.onMessageReadException(e, msg);
            }
        }
    
    // Declared at the super level
    /**
     * Event notification to perform a regular daemon activity. To get it
    * called, another thread has to set Notification to true:
    * <code>daemon.setNotification(true);</code>
    * 
    * @see #onWait
     */
    protected void onNotify()
        {
        // import com.tangosol.util.Base;
        
        long lTime = Base.getSafeTimeMillis();
        if (lTime >= getBroadcastNextMillis())
            {
            switch (getState())
                {
                case STATE_ANNOUNCE:
                    onTimerAnnouncing();
                    break;
        
                case STATE_JOINING:
                    onTimerJoining();
                    break;
        
                case STATE_JOINED:
                    onTimerRunning();
                    break;
                }
        
            // schedule next broadcast
            setBroadcastNextMillis(lTime + getBroadcastRepeatMillis());
            }
        
        super.onNotify();
        }
    
    /**
     * Called on the ClusterService thread when the TcpRingListener determines
    * that an address that it is monitoring continues to exceed its timeout and
    * may be unavailable.
    * 
    * @param address  the address that has exceeded its timeout
     */
    public void onNotifyIpTimeout(java.net.InetAddress address)
        {
        // import Component.Net.Member;
        // import Component.Net.MemberSet;
        // import Component.Net.MemberSet.ActualMemberSet;
        // import java.util.Iterator;
        // import java.util.Set;
        
        // determine the set of members implicated by the timed-out address
        MemberSet setTimedOut = new ActualMemberSet();
        for (Iterator iter = getClusterMemberSet().iterator(); iter.hasNext(); )
            {
            Member member = (Member) iter.next();
            if (address.equals(member.getAddress()))
                {
                setTimedOut.add(member);
                }
            }
        
        if (!setTimedOut.isEmpty())
            {
            // see if there is a new member timing-out; only log the trace
            // message once-per-address-per-outtage
            for (Iterator iter = setTimedOut.iterator(); iter.hasNext(); )
                {
                if (!((Member) iter.next()).isTimedOut())
                    {
                    _trace("Failed to reach address " + address + " within the "
                         + "IpMonitor timeout. Members " + setTimedOut.toString(Member.SHOW_STATS)
                         + " are suspect.", 2);
                    break;
                    }
                }
            
            onMembersTimedOut(setTimedOut);
            }
        }
    
    public void onNotifyMemberLeft(com.tangosol.coherence.component.net.Member member)
        {
        // clean up remaining polls to that Member
        closePolls(member);
        }
    
    /**
     * Called on the ClusterService thread when delivery of a packet has
    * timed-out (no Ack received within the packet timeout) to the specified
    * set of members.
    * 
    * @param packet              the packet whose delivery has timed-out, or
    * null for a bus based timeout
    * @param setTimedOut   the set of members who have not acknowledged receipt
    * of the packet within the timeout
     */
    public void onNotifyTcmpTimeout(com.tangosol.coherence.component.net.packet.MessagePacket packet, com.tangosol.coherence.component.net.MemberSet setLate)
        {
        // import Component.Net.Member;
        // import java.util.HashSet;
        // import java.util.Iterator;
        // import java.util.Set;
        
        Set    setTimedOut = new HashSet();
        Member memberThis  = getThisMember();
        for (Iterator iter = setLate.iterator(); iter.hasNext(); )
            {
            Member memberLate = (Member) iter.next();    
            if (memberLate.isTimedOut() || memberThis.isTimedOut() ||
                verifyMemberLeft(memberLate, packet))
                {
                // the late member's departure has been confirmed
                setTimedOut.add(memberLate);
                }
            else
                {
                // the member's departure is not yet determined.  This
                // could be due to several factors, but most likely
                // means that we are awaiting witness confirmation.
                }
            }
        
        if (!setTimedOut.isEmpty())
            {
            onMembersTimedOut(setTimedOut);
            }
        }
    
    /**
     * This is the event that is executed when a TcpRingListener thread notifies
    * its local ClusterService that a member can be removed.
     */
    public void onNotifyTcpDeparture(com.tangosol.coherence.component.net.Member member)
        {
        // TcpRing departure is an active disconnect (not requiring any kind of
        // quorum decision or witness vote); send MemberLeft to other ClusterServices
        // and remove the Member
        if (getClusterMemberSet().contains(member))
            {
            doMemberLeft(member);
            }
        }
    
    public void onServiceAnnounced(com.tangosol.coherence.component.net.ServiceInfo info)
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid$NotifyServiceAnnounced as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyServiceAnnounced;
        
        String sServiceName = info.getServiceName();
        for (int i = 1, c = getServiceCount(); i < c; ++i)
            {
            com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid service = getService(i);
            if (service != null)
                {
                com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyServiceAnnounced msg = (com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyServiceAnnounced)
                        service.instantiateMessage("NotifyServiceAnnounced");
                msg.setNotifyServiceName(sServiceName);
                service.send(msg);
                }
            }
        }
    
    /**
     * Event notification informing the ClusterService that a service has joined
    * the cluster.
    * 
    * @param info  the service info
    * @param member the cluster member that the service is running on
     */
    public void onServiceJoined(com.tangosol.coherence.component.net.ServiceInfo info, com.tangosol.coherence.component.net.Member member)
        {
        // the MemberJoined is handled by the service itself in order to ensure consistent message ordering
        // with any service specific transport
        }
    
    /**
     * Event notification informing the ClusterService that a service is joining
    * the cluster.
    * 
    * @param info  the service info
    * @param member the cluster member that the service is running on
     */
    public void onServiceJoining(com.tangosol.coherence.component.net.ServiceInfo info, com.tangosol.coherence.component.net.Member member)
        {
        _assert(info != null && member != null);
        
        // the actual work is done at validateNewService()
        }
    
    public void onServiceLeaving(com.tangosol.coherence.component.net.ServiceInfo info, com.tangosol.coherence.component.net.Member member)
        {
        // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid$NotifyServiceLeaving as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyServiceLeaving;
        
        _assert(info != null && member != null);
        _assert(info.getMemberSet().contains(member));
        
        // notifications are _not_ made when this Member is the Member
        // that caused the notification
        if (member != getThisMember())
            {
            // notify the related service
            int nServiceId = info.getServiceId();
            if (nServiceId > 0)
                {        
                com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid service = getService(nServiceId);
        
                // notify the service only if it is ready for service-level communications
                // (see ClusterService.ServiceJoinRequest.Poll.onCompletion)
                if (service != null && service.getServiceMemberSet() != null)
                    {
                    com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyServiceLeaving msg = (com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyServiceLeaving)
                            service.instantiateMessage("NotifyServiceLeaving");
                    msg.setNotifyMember(member);
                    service.send(msg);
                    }
                }
            }
        }
    
    /**
     * @param info  the ServiceInfo
    * @param member the departed Member
    * @param nState the state of the departed Member before the departure
     */
    public void onServiceLeft(com.tangosol.coherence.component.net.ServiceInfo info, com.tangosol.coherence.component.net.Member member, int nState)
        {
        // import Component.Net.Member;
        // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid$NotifyServiceLeft as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyServiceLeft;
        
        _assert(info != null && member != null);
        
        // notifications are _not_ made when this Member is the Member
        // that caused the notification
        if (member != getThisMember())
            {
            // com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid notifications are not sent to the ClusterService
            int nServiceId = info.getServiceId();
            _assert(nServiceId > 0);
        
            com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid service = getService(nServiceId);
        
            // notify the service only if it is ready for service-level communications
            // (see ClusterService.ServiceJoinRequest.Poll.onCompletion)
            if (service != null && service.getServiceMemberSet() != null)
                {
                com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyServiceLeft msg = (com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyServiceLeft)
                        service.instantiateMessage("NotifyServiceLeft");
                msg.setNotifyMember(member);
        
                service.send(msg);
                }
            }
        }
    
    // Declared at the super level
    /**
     * The default implementation of this method sets AcceptingClients to true.
    * If the Service has not completed preparing at this point, then the
    * Service must override this implementation and only set AcceptingClients
    * to true when the Service has actually "finished starting".
     */
    public void onServiceStarted()
        {
        // must not call super because that will signal that clients are
        // now able to call in; only after all other Members have welcomed
        // this Member will the startup be complete
        }
    
    /**
     * This event is invoked when a Member cannot join a cluster (State=JOINING)
    * because this Member was rejected.
    * 
    * This event must stop the ClusterService on this Member.
    * 
    * @see ClusterService$NewMemberRequestIdReject#onReceived
     */
    public void onStopJoining()
        {
        if (isAcceptingClients())
            {
            getCluster().stop();
            }
        else
            {
            stop();
            }
        }
    
    /**
     * This event is invoked when a Member that is running in a cluster
    * (State=JOINED) must immediately stop because a cluster membership
    * integrity error has been detected.
    * 
    * This event must stop the ClusterService on this Member.
    * 
    * @see ClusterService$SeniorMemberHeartbeat#onReceived
    * @see ClusterService$SeniorMemberKill#onReceived
     */
    public void onStopRunning()
        {
        if (isAcceptingClients())
            {
            getCluster().stop();
            }
        else
            {
            getTcpRing().onLeft();
            stop();
            }
        }
    
    /**
     * This event occurs when the member is in its announcing stage and the
    * timer fires.
     */
    public void onTimerAnnouncing()
        {
        // import Component.Net.Cluster;
        // import Component.Net.Member;
        // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet.MasterMemberSet;
        // import com.oracle.coherence.common.net.InetAddresses;
        
        Member memberThis = getAnnounceMember();
        int    cAttempts  = getBroadcastCounter() + 1;
        int    cLimit     = getBroadcastLimit();
        if (cAttempts > cLimit)
            {
            // timeout hit - self-assign id if possible
            Cluster cluster    = (Cluster) getCluster();
            boolean fWellKnown = isWellKnown(memberThis) || isWellKnown(memberThis.getAddress());
            boolean fDeaf      = getStatsJoinRequests() == 0; // we should have at least heard our own announcements
            
            if (fWellKnown          &&  // allowed to form a cluster
                !fDeaf              &&  // capable of hearing at least its own broadcasts (i.e. MC works)
                ensureDiscovery())      // has bound to the cluster port
                {
                // start cluster
                MasterMemberSet setMember = getClusterMemberSet();
                memberThis.setId(1);
                memberThis.setPreferredPacketLength(cluster.getSocketManager().
                    getPreferredUnicastUdpSocket().getPacketLength());
                memberThis.setPreferredPort(cluster.getSocketManager().
                    getPreferredUnicastUdpSocket().getPort());
        
                // reuse the variable to make it a bit harder to crack
                int nReject = setMember.induct(memberThis, this);
                if (nReject == REJECT_NONE)
                    {
                    setMember.setThisMember(memberThis);
        
                    _trace("Created a new " + formatClusterString() + " with "
                         + memberThis.toString(Member.SHOW_LICENSE), 3);
        
                    setState(STATE_JOINED);
                    setMember.setServiceJoined(memberThis.getId());
        
                    // started cluster myself! accept clients immediately
                    setAcceptingClients(true);
                    }
                else
                    {
                    _trace(memberThis.toString(Member.SHOW_LICENSE) +
                        " is unable to join the cluster.", 1);            
                    onMemberRejected(nReject, null);
                    }
                }
            else if (!fWellKnown)
                {
                // continue announcing until startup timeout is crossed        
                resetBroadcastCounter("waiting for well-known nodes in cluster '" + getClusterName() + "' to respond", null);
                }
            else if (fDeaf)
                {
                // We are not even hearing our own announce messages. This generally means that multicast isn't working.
                // A common cause is being on a VPN (Oracle's cisco VPN for instance) and in some cases can be remediaed by
                // forcing IPv4.
                // Note in the case of TcpDatagram based connections local delivery is not instantaneous especially
                // when coupled with SSL, it can take over 1s to deliver the first packet, and thus we can't treat
                // this as a hard failure
                boolean fMC = getWellKnownAddresses() == null;
                resetBroadcastCounter((fMC ? "multicast" : "unicast") +
                    " networking appears to be inoperable on interface " + memberThis.getAddress().getHostAddress() +
                    " as this process isn't receiving even its own transmissions" +
                    (InetAddresses.PreferIPv4Stack
                        ? (fMC
                            ? "; consider switching network interfaces or using WKA based clustering"
                            : "; consider switching network interfaces")
                        : "; consider forcing IPv4 via -Djava.net.preferIPv4Stack=true"), null);
                }
            else
                {
                // continue announcing until startup timeout is crossed        
                resetBroadcastCounter("unable to bind or share cluster port", null);
                }
            }
        else
            {
            // send an announcement
            ClusterService.NewMemberAnnounce msg = (ClusterService.NewMemberAnnounce)
                    instantiateMessage("NewMemberAnnounce");
            msg.setAnnounceProtocolVersion(ClusterService.ANNOUNCE_PROTOCOL_VERSION);        
            msg.setFromMember(memberThis);
            msg.setAttemptCounter(cAttempts);
            msg.setAttemptLimit(cLimit);
            send(msg);
        
            // update announcement info
            setBroadcastCounter(cAttempts);
            }
        }
    
    /**
     * This event occurs when the member is in its joining stage and the timer
    * fires.
     */
    public void onTimerJoining()
        {
        // import com.tangosol.net.internal.ClusterJoinException;
        
        int cAttempts = getBroadcastCounter() + 1;
        int cLimit    = getBroadcastLimit();
        if (cAttempts > cLimit)
            {
            if (getThisMember() == null)
                {
                // timeout hit - go back to announcing
                resetBroadcastCounter("AnnounceReply was not followed by RequestIdReply", null);
                setTimestampAdjustment(0);
                setRequestMember(null);
                setState(STATE_ANNOUNCE);
                }
            else
                {
                // we have been trying to join but our senior has left and
                // nobody is telling us to wait; we may be alone now.
                // We cannot fall back to announcing as the cluster state
                // is not reversible; rely on the safe tier to restart.
                throw new ClusterJoinException();
                }
            }
        else
            {
            // send a join request
            ClusterService.NewMemberRequestId msg = (ClusterService.NewMemberRequestId)
                instantiateMessage("NewMemberRequestId");
            msg.setFromMember(getRequestMember());
            msg.setAttemptCounter(cAttempts);
            msg.setAttemptLimit(cLimit);
            msg.setServiceVersion(VERSION_BARRIER);
            msg.setMaxPacketSize(getMaximumPacketLength());
            msg.setWkaEnabled(isWkaEnabled());
            send(msg);
        
            // update announcement info
            setBroadcastCounter(cAttempts);
            }
        }
    
    /**
     * This event occurs when the member is part of a running cluster and the
    * timer fires.
     */
    public void onTimerRunning()
        {
        // import Component.Net.Member;
        // import Component.Net.MemberSet;
        // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet.MasterMemberSet;
        // import Component.Net.MemberSet.SingleMemberSet;
        // import com.tangosol.util.Base;
        // import java.util.Iterator;
        // import java.util.Set;
        
        MasterMemberSet setMember    = getClusterMemberSet();
        Member          memberThis   = setMember.getThisMember();
        Member          memberOldest = setMember.getOldestMember();
        long            ldtLastRecv  = calcTimestamp(memberThis.getLastIncomingMillis());
        
        // send member heartbeat if necessary
        MemberSet setTo = getHeartbeatMemberSet();
        if (setTo != null && !setTo.isEmpty())
            {
            heartbeat(setTo);
            }
        
        // clear dynamic broadcast addresses if they have expired
        Set setBroadcast = getBroadcastAddresses();
        if (setBroadcast != null && getBroadcastAddressesExpiry() < Base.getSafeTimeMillis())
            {
            synchronized (setBroadcast)
                {
                setBroadcast.clear();
                }
            }
        
        if (isAcceptingClients())
            {
            if (memberThis == memberOldest)
                {
                // send a cluster heartbeat
                ClusterService.SeniorMemberHeartbeat msg = (ClusterService.SeniorMemberHeartbeat)
                        instantiateMessage("SeniorMemberHeartbeat");
                msg.setLastReceivedMillis(ldtLastRecv);
                msg.setMemberSet(setMember);
                msg.setWkaEnabled(isWkaEnabled());
                msg.setLastJoinTime(setMember.getLastJoinTime());
                send(msg);
        
                checkPendingJoinPolls();
                }
            }
        else
            {
            if (memberThis == memberOldest)
                {
                // just became senior
                setAcceptingClients(true);
                }
            else
                {
                // joined but still not welcome
                ClusterService.NewMemberWelcomeAnnounce msg = (ClusterService.NewMemberWelcomeAnnounce)
                        instantiateMessage("NewMemberWelcomeAnnounce");
                msg.setSeniorMember(memberOldest);
                msg.setServiceVersion(VERSION_BARRIER);
                send(msg);
                }
            }
        
        // send TcpRing heartbeat(s)
        getTcpRing().heartbeatBuddies();
        }
    
    /**
     * Called on the cluster service thread only.
     */
    public void populateWelcomeMessage(ClusterService.NewMemberWelcome msg)
        {
        // import Component.Net.Member;
        // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet;
        // import Component.Net.ServiceInfo;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        
        Member memberThis  = getThisMember();
        int    nMemberThis = memberThis.getId();
        
        msg.setFromMemberUid(memberThis.getUid32());
        msg.setPreferredPacketLength(memberThis.getPreferredPacketLength());
        msg.setPreferredPort(memberThis.getPreferredPort());
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid[]     aService  = getService();
        int           cServices = aService.length;
        ServiceInfo[] aInfo     = new ServiceInfo[cServices];
        int           cInfo     = 0;
        
        for (int iService = 0; iService < cServices; ++iService)
            {
            com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid service = aService[iService];
        
            if (service != null)
                {
                // it's theoretically possible that there are services not yet joined
                // (see doServiceJoining);
                // only communicate the info for services that we are running
                ServiceInfo info = getServiceInfo(iService);
                if (info.getMemberSet().getServiceJoinTime(nMemberThis) > 0L)
                    {
                    aInfo[cInfo++] = info;
                    }
                }
            }
        
        msg.setServiceCount(cInfo);
        
        for (int i = 0; i < cInfo; ++i)
            {
            ServiceInfo info = aInfo[i];
        
            ServiceMemberSet setMember = info.getMemberSet(); 
        
            msg.setServiceId(i, info.getServiceId());
            msg.setServiceName(i, info.getServiceName());
            msg.setServiceType(i, info.getServiceType());
            msg.setServiceVersion(i, setMember.getServiceVersion(nMemberThis));
            msg.setServiceEndPointName(i, setMember.getServiceEndPointName(nMemberThis));
            msg.setServiceJoinTime(i, setMember.getServiceJoinTime(nMemberThis));
            msg.setServiceState(i, setMember.getState(nMemberThis));
            msg.setServiceMemberConfigMap(i, setMember.getMemberConfigMap(nMemberThis));
            }
        }
    
    // Declared at the super level
    /**
     * Register this service with the ClusterService.
    * 
    * @see start()
     */
    protected void register()
        {
        // no-op; ClusterService doesn't need to register with itself
        }
    
    /**
     * Register the pending ServiceJoining message (poll)
     */
    public void registerServiceJoining(ClusterService.ServiceJoining msg)
        {
        // import com.tangosol.util.Base;
        // import com.tangosol.util.LongArray;
        
        _assert(Thread.currentThread() == getThread());
        
        LongArray laPending = getPendingServiceJoining();
        for (long lIndex = Base.getSafeTimeMillis(); true; lIndex++)
            {
            if (laPending.get(lIndex) == null)
                {
                laPending.set(lIndex, msg);
                return;
                }
            }
        }
    
    /**
     * Remove the socket address for the specified non-WKA member from a list
    * that is used for the "broadcast-over-unicast" if the multicast is
    * disabled.
     */
    public void removeDynamicBroadcast(com.tangosol.coherence.component.net.Member member)
        {
        // import java.net.InetSocketAddress;
        // import java.util.Set;
        
        if (member != null && !isWellKnown(member))
            {
            Set setBroadcast = getBroadcastAddresses();
            if (setBroadcast != null)
                {
                synchronized (setBroadcast)
                    {
                    setBroadcast.remove(member.getSocketAddress());
                    }
                }
            }
        }
    
    public void removeMember(com.tangosol.coherence.component.net.Member member)
        {
        // import Component.Net.ServiceInfo;
        
        int nMember = member.getId();
        for (int i = 1, c = getServiceInfoCount(); i < c; ++i)
            {
            ServiceInfo info = getServiceInfo(i);
            if (info != null)
                {
                int nState = info.getMemberSet().getState(nMember);
                if (info.getMemberSet().remove(member))
                    {
                    onServiceLeft(info, member, nState);
                    }
                }
            }
        
        if (getClusterMemberSet().remove(member))
            {
            onMemberLeft(member);
            }
        }
    
    /**
     * *** LICENSE ***
    * WARNING: This method name is obfuscated.
    * 
    * Incorporate any additional / updated licenses being contributed by a new
    * member. Called from NewMemberRequestId message
    * 
    * Stores message information in WkaMap property.
     */
    public void resetBroadcastCounter(java.io.DataInput stream, boolean fSelf)
            throws java.io.IOException
        {
        // import Component.Application.Console.Coherence;
        // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet.MasterMemberSet;
        // import com.tangosol.run.xml.XmlElement;
        // import com.tangosol.util.ExternalizableHelper as com.tangosol.util.ExternalizableHelper;
        // import com.tangosol.util.UID;
        // import java.util.Iterator;
        // import java.util.Map;
        
        // get the Coherence app instance
        Coherence app = ((Coherence) Coherence.get_Instance());
        
        // get the licenses from the application object
        XmlElement xmlAll = app.getServiceConfig("$License");
        
        // only incorporate licenses if this member is running
        MasterMemberSet setMember = getClusterMemberSet();
        boolean fValid = fSelf || getState() == ClusterService.STATE_JOINED
                && setMember.getThisMember() == setMember.getOldestMember()
                && !isMembershipSuspended();
        
        // determine the product edition / mode in use
        int nEThis = Math.max(0, fromString(xmlAll.getSafeElement("edition-name").getString()));
        int nMThis = Math.max(0, fromString(xmlAll.getSafeElement("license-mode").getString()));
        
        // map from UID to int[] of edition, limit type, limit value
        Map map = getWkaMap();
        
        // edition id, mode id
        int nEThat = com.tangosol.util.ExternalizableHelper.readInt(stream);
        int nMThat = com.tangosol.util.ExternalizableHelper.readInt(stream);
        
        // validate product edition compatibility: same edition is OK, and different
        // edition is OK as long as both are cluster editions for DataGrid (RTC=1 or GE=5)
        if (!(nEThis == nEThat ||
                ((nEThis == 1 && nEThat == 5) || (nEThis == 5 && nEThat == 1))))
            {
            fValid = false;
            }
        
        // validate mode compatibility
        if (nMThis != nMThat)
            {
            fValid = false;
            }
        
        // determine what "limit types" are already being used (per edition)
        int[] anType = new int[6];
        for (int i = 0, c = anType.length; i < c; ++i)
            {
            anType[i] = -1;
            }
        for (Iterator iter = map.values().iterator(); iter.hasNext(); )
            {
            int[] an = (int[]) iter.next(); // [0]=edition, [1]=limit type, [2]=value
            anType[an[0]] = an[1];
            }
        
        // register new / additional / increased licenses
        // _trace("*** resetBroadcastCounter() registering licenses: " + fValid);
        while (stream.readBoolean())
            {
            UID id = new UID(stream);
            int[] an = new int[3];
            for (int i = 0; i < 3; ++i)
                {
                an[i] = com.tangosol.util.ExternalizableHelper.readInt(stream);
                }
        
            if (fValid)
                {
                // verify that the license limit type is compatible
                if (anType[an[0]] >= 0 && an[1] != anType[an[0]])
                    {
                    if (an[2] >= 0)
                        {
                        // incompatible
                        continue;
                        }
                    else
                        {
                        // unlimited, so remove previous licenses
                        for (Iterator iter = map.values().iterator(); iter.hasNext(); )
                            {
                            if (((int[]) iter.next())[0] == an[0])
                                {
                                iter.remove();
                                }
                            }
                        }
                    }
        
                int[] anOld = (int[]) map.put(id, an);
        
                // if it replaced a license with the same edition and limit type,
                // keep the larger limit value
                if (anOld != null)
                    {
                    if (anOld[0] == an[0] && anOld[1] == an[1])
                        {
                        an[2] = Math.max(an[2], anOld[2]);
                        }
                    }
        
                // remember the limit type (now that that type has been seen)
                anType[an[0]] = an[1];
                }
        
            // _trace("*** registering license " + id + " = " +
            //     (new String[]{"DC", "RTC", "CC", "SE", "EE", "GE"})[an[0]] + "/" +
            //     (new String[]{"site", "server", "socket", "CPU", "seat", "user"})[an[1]] + "/" +  an[2]);
            }
        }
    
    /**
     * Reset the BroadcastCounter and log the reason if the node is already in
    * the interminable state
    * 
    * @since Coherence 3.1.1
     */
    public void resetBroadcastCounter(String sReason, com.tangosol.coherence.component.net.message.DiscoveryMessage msg)
        {
        // import Component.Net.Cluster;
        // import Component.Net.Member;
        // import com.tangosol.net.ClusterDependencies;
        // import com.tangosol.util.Base;
        
        setBroadcastCounter(0);
        
        // how long has it been since we started announcing?
        Member memberThis   = getAnnounceMember();
        long   ldtNow       = Base.getSafeTimeMillis();
        long   cMillisDelay = ldtNow - memberThis.getTimestamp();
        long   cMillisMax   = getStartupTimeout();
        long   cMillisJoin  = getBroadcastTimeoutMillis();
        long   cMillisWarn  = Math.min(30000L, (cMillisMax + cMillisJoin) / 2);
        
        if (sReason != null && cMillisDelay > cMillisWarn)
            {
            // if we're stuck in an interminable join, start
            // logging reasons for resetting the broadcast counter
            // (periodically at a lower level)
        
            long ldtLastWarning = getLastInterminableWarningMillis();
            if (ldtLastWarning == 0L)
                {
                // it now seems to be interminable (going on and on and ...)
                if (isWkaEnabled())
                    {
                    _trace("This " + memberThis
                            + " has been attempting to join the cluster using WKA list "
                            + getWellKnownAddresses()
                            + " for " + (cMillisDelay / 1000) + " seconds without success;"
                            + " this could indicate a mis-configured WKA, or it may"
                            + " simply be the result of a busy cluster or active failover.", 2);
                    }
                else
                    {
                    ClusterDependencies config = ((Cluster) getCluster()).getDependencies();
                    _trace("This " + memberThis
                            + " has been attempting to join the cluster at address "
                            + config.getGroupAddress().toString() + ":" + config.getGroupPort()
                            + " with TTL " + config.getGroupTimeToLive()
                            + " for " + (cMillisDelay / 1000) + " seconds without success;"
                            + " this could indicate a mis-configured TTL value, or it may"
                            + " simply be the result of a busy cluster or active failover.", 2);  
                    }
                }
        
            int iLevel = 6;
            if (ldtNow > ldtLastWarning + 5000L)
                {
                iLevel = 2;
                setLastInterminableWarningMillis(ldtNow);
                }
        
            String sMsg = msg == null ?
                "Delaying formation of a new cluster; " + sReason :
                "Received a discovery message that indicates " + sReason + ":\n" + msg;
            _trace(sMsg, iLevel);
        
            if (cMillisDelay > cMillisMax)
                {
                // we should have plenty info in the log now
                _trace("Failure to join a cluster for " + (cMillisDelay/1000) + " seconds" +
                       "; stopping cluster service.", 1);
                onStopJoining();
                }
            }
        }
    
    // Declared at the super level
    /**
     * Setter for property AcceptingClients.<p>
    * Set to true when the Service has advanced to the state at which it can
    * accept requests from client threads.
     */
    public void setAcceptingClients(boolean fAccepting)
        {
        boolean fPrev = isAcceptingClients();
        
        super.setAcceptingClients(fAccepting);
        
        if (fAccepting && !fPrev)
            {
            int cMillis = getHeartbeatDelay();
            if (cMillis == 0)
                {
                // recalibrate broadcast rate for heartbeats; greater of
                //      (a) configured announcement rate
                //      (b) 1/8 of the announcement timeout
                cMillis = Math.max(
                    getBroadcastRepeatMillis(),
                    getBroadcastTimeoutMillis() >>> 3);
                }
            setBroadcastRepeatMillis(cMillis);
            }
        }
    
    // Declared at the super level
    /**
     * Setter for property ActionPolicy.<p>
    * The action policy for this service.
     */
    public void setActionPolicy(com.tangosol.net.ActionPolicy policy)
        {
        super.setActionPolicy(policy);
        }
    
    // Accessor for the property "AnnounceMember"
    /**
     * Setter for property AnnounceMember.<p>
    * The AnnounceMember is the Member object that the ClusterService uses to
    * announce its presence. It is temporary, in that it is used to elicit a
    * time adjustment Message from the oldest Member in the cluster, from which
    * the RequestMember will be configured.
    * 
    * @see Cluster.configureDaemons
     */
    public void setAnnounceMember(com.tangosol.coherence.component.net.Member member)
        {
        __m_AnnounceMember = member;
        }
    
    // Accessor for the property "BroadcastAddresses"
    /**
     * Setter for property BroadcastAddresses.<p>
    * A Set<InetSocketAddress> used by the TCMP for broadcast in leu of
    * multicast. This set could be modified by the ClusterService and used by
    * the TCMP transport tier.
    * 
    * @see #addDynamicBroadcast
     */
    public void setBroadcastAddresses(java.util.Set setAddresses)
        {
        __m_BroadcastAddresses = setAddresses;
        }
    
    // Accessor for the property "BroadcastAddressesExpiry"
    /**
     * Setter for property BroadcastAddressesExpiry.<p>
    * The timestamp at which the broadcast address set expires and should be
    * cleared.
     */
    public void setBroadcastAddressesExpiry(long setAddresses)
        {
        __m_BroadcastAddressesExpiry = setAddresses;
        }
    
    // Accessor for the property "BroadcastCounter"
    /**
     * Setter for property BroadcastCounter.<p>
    * BroadcastCounter is the "try" number for broadcasting an "announce" or
    * "request" Message while trying to join a cluster. There are many events
    * that can reset the BroadcastCounter.
     */
    public void setBroadcastCounter(int c)
        {
        __m_BroadcastCounter = c;
        }
    
    // Accessor for the property "BroadcastLimit"
    /**
     * Stores the max number of times an announcing or joining message will be
    * repeated.
    * 
    * *** LICENSE ***
    * WARNING: This method includes the license check.
    * 
    * debugging code:
    * 
    * _trace("--- software=" + sP + " (" + nP + ")");
    * _trace("--- licensee=" + sL);
    * _trace("--- type=" + sM + " (" + nM + ")");
    * _trace("--- from-date=" + sF + " (" + app.parseDate(sF) + ")");
    * _trace("--- to-date=" + sT + " (" + app.parseDate(sT) + ")");
    * _trace("--- max-users=" + cU);
    * _trace("--- max-seats=" + cI);
    * _trace("--- site=" + sS);
    * _trace("--- max-servers=" + cS);
    * _trace("--- max-cpus=" + cC);
    * _trace("--- max-sockets=" + cP);
    * _trace("--- id=" + sI + " (" + id + ")");
    * _trace("--- key=" + sK + " (crc=" + nC + ", class=" + sC + ")");
    * _trace("--- signature=" + sK);
    * _trace("--- sb=" + sb);
    * _trace("--- buf=" + Base.toHexEscape(bufTemp.toByteArray()));
    * _trace("*** bad crc (" + nC + " versus " +
    * Base.toCrc(bufTemp.toByteArray()) + "): " + xml);
     */
    public void setBroadcastLimit(int c)
        {
        // import Component.Application.Console.Coherence;
        // import com.tangosol.io.ReadBuffer$BufferInput as com.tangosol.io.ReadBuffer.BufferInput;
        // import com.tangosol.io.WriteBuffer$BufferOutput as com.tangosol.io.WriteBuffer.BufferOutput;
        // import com.tangosol.run.xml.XmlElement;
        // import com.tangosol.util.Base;
        // import com.tangosol.util.Binary;
        // import com.tangosol.util.BinaryWriteBuffer;
        // import com.tangosol.util.ClassHelper;
        // import com.tangosol.util.ExternalizableHelper;
        // import com.tangosol.util.SparseArray;
        // import com.tangosol.util.UID;
        // import java.io.ByteArrayOutputStream;
        // import java.io.DataOutputStream;
        // import java.io.InputStream;
        // import java.io.IOException;
        // import java.lang.reflect.Method;
        // import java.util.Iterator;
        // import java.security.cert.Certificate;
        
        // if this node is going to be announcing
        if (c > 0)
            {
            // broadcast mode is 0=unchecked, 1=checked and OK, 2=license tampered
            int n = getBroadcastMode();
        
            // A bit field tracking any errors encountered while validating protected files
            // or loading licenses, returned as the BroadCastLimit and always negative to
            // force the node not succesfully join the cluster.
            // See com.xtangosol.license.LicenseFailureDecoder for decoding.
            int iInv = 0x80000000;
        
            // check if we haven't checked the licenses yet
            if (n < 1)
                {
                // license validity check: set n=1 if OK, n=2 if failure
        
                // get the Coherence app instance
                Coherence app = ((Coherence) Coherence.get_Instance());
        
                // start with some undecipherably random number (filled
                // in by the build process)
                n = getBroadcastVariance();
        
                // find the various classes and files that we need to validate
                // NOTE: the last one must be the name of the public key in tangosol.jar
                ClassLoader cl = Base.class.getClassLoader();
                if (cl == null)
                    {
                    cl = Base.getContextClassLoader();
                    }
        
                String[] asName = new String[]
                    {
                    "com/tangosol/license/LicensedObject.class",
                    "com/tangosol/license/LicensedObject$LicenseData.class",
                    "com/tangosol/license/CoherenceDataGridEdition.class",
                    "com/tangosol/license/CoherenceApplicationEdition.class",
                    "com/tangosol/license/CoherenceCachingEdition.class",
                    "com/tangosol/license/CoherenceComputeClient.class",
                    "com/tangosol/license/CoherenceRealTimeClient.class",
                    "com/tangosol/license/CoherenceDataClient.class",
                    "tangosol.dat",                                 // note: must be [8]
                    "tangosol.cer",                                 // note: certificate must be last
                    };
        
                SparseArray sa = new SparseArray(); // all known license UIDs keyed by CRC checksum
        
                byte[] ab = null;
                if (cl != null)
                    {
                    for (int i = 0, cNames = asName.length; i < cNames; ++i)
                        {
                        try
                            {
                            InputStream in = cl.getResourceAsStream(toString(asName[i]));
                            if (in != null)
                                {
                                ab = Base.read(in);
                                n ^= Base.toCrc(ab);
        
                                if (i == 8)
                                    {
                                    // parse UID / CRC list
                                    com.tangosol.io.ReadBuffer.BufferInput bufin = new Binary(ab).getBufferInput();
                                    while (bufin.available() > 0)
                                        {
                                        sa.set(bufin.readPackedInt(), new UID(bufin));
                                        }
                                    }
                                }
                            }
                        catch (Throwable e)
                            {
                            // one or more files have been tampered with
                            n -= getBroadcastRepeatMillis();
                            iInv |= 0x1;
                            }
                        }
                    }
        
                // get the licenses from the application object
                XmlElement xmlAll = app.getServiceConfig("$License");
        
                // determine the product edition / mode in use
                // ("what edition / mode is this node supposed to run?")
                int nEN = Math.max(0, fromString(xmlAll.getSafeElement(
                        "edition-name").getString()));
                int nLM = Math.max(0, fromString(xmlAll.getSafeElement(
                        "license-mode").getString()));
                // _trace("*** selected edition=" + nEN + ", mode=" + nLM);
        
                // build a license submission block to notify the cluster of
                // the licenses known by this member
                BinaryWriteBuffer buf = new BinaryWriteBuffer(100);
                try
                    {
                    // header is the edition and mode
                    com.tangosol.io.WriteBuffer.BufferOutput out = buf.getBufferOutput();
                    out.writePackedInt(nEN); // this node's edition
                    out.writePackedInt(nLM); // this node's mode
        
                    // only collect licenses if this node is clustered (CC, SE, EE, GE)
                    if (nEN >= 2 && xmlAll != null)
                        {
                        // Instantate Signature for use in license validation
                        // retrieve pub key from cert
                        Class cfc = Class.forName("java.security.cert.CertificateFactory");
                        Object cf = ClassHelper.invokeStatic(cfc, "getInstance",
                            new Object[]{"X.509"});
                        Object ct = ClassHelper.invoke(cf, "generateCertificate",
                            new Object[]{new Binary(ab).getInputStream()});
                        Object  pb = ((Certificate) ct).getPublicKey();
                        Object  sg = null;  // reusable Signature or SignatureSpi
                        Method  mUp; // update method
                        Method  mVf; // verify method
        
                        // _trace("*** using default Signature provider");
                        Class sgc = Class.forName("java.security.Signature");
        
                        sg  = ClassHelper.invokeStatic(sgc, "getInstance",
                            new String[]{"SHA1withDSA"});
        
                        mUp = sgc.getMethod("update",
                            new Class[]{byte[].class, Integer.TYPE, Integer.TYPE});
        
                        mVf = sgc.getMethod("verify",
                            new Class[]{byte[].class});
        
                        ClassHelper.invoke(sg, "initVerify", new Object[]{pb});
                        // _trace("*** using default Signature provider");
        
                        int iLc = 15; // first bit available for failed license tracking
                        // iterate through the licenses
                        for (Iterator iter = xmlAll.getSafeElement("license-list")
                                .getElements("license"); iter.hasNext(); ++iLc)
                            {
                            XmlElement xml = (XmlElement) iter.next();
        
                            // _trace("*** loading license " + xml);
        
                            UID id    = null; // license UID (required)
                            int nId   = 0;    // edition enum (DC, RTC, CC, SE, EE, GE)
                            int nType = 0;    // limitation enum (none, srv, sock, cpu, seat, user)
                            int cStop = 0;    // limitation value
        
                            // parse the license XML
                            String sP = xml.getSafeElement("software").getString();
                            String sL = xml.getSafeElement("licensee").getString();
                            String sA = xml.getSafeElement("agreement").getString(); // 3.2
                            String sM = xml.getSafeElement("type").getString();
                            String sF = xml.getSafeElement("from-date").getString();
                            String sT = xml.getSafeElement("to-date").getString();
                            String sR = xml.getSafeElement("maintenance-renewal-date").getString(); // 3.2
                            int    cI = xml.getSafeElement("max-seats").getInt(); // 3.2
                            int    cU = xml.getSafeElement("max-users").getInt();
                            String sS = xml.getSafeElement("site").getString();
                            int    cS = xml.getSafeElement("max-servers").getInt();
                            int    cP = xml.getSafeElement("max-sockets").getInt(); // 3.2
                            int    cC = xml.getSafeElement("max-cpus").getInt();
                            String sI = xml.getSafeElement("id").getString();
                            String sK = xml.getSafeElement("key").getString();
                            String sE = xml.getSafeElement("signature").getString(); // 3.2
        
                            iInv |= (1 << iLc); // start by marking as invalid, unmark once added
        
                            // software, licensee, type and UID are required
                            if (sP.length() == 0 || sL.length() == 0
                                    || sM.length() == 0 || sI.length() == 0)
                                {
                                 // _trace("*** no software (" + sP + "), licensee (" + sL
                                 //    + "), mode (" + sM + ") or UID (" + sI + "): " + xml);
                                iInv |= 0x2;
                                continue;
                                }
        
                            // only collect the applicable licenses
                            int nP = fromString(sP); // licensed product edition
                            int nM = fromString(sM); // license type ("mode")
                            // _trace("*** license edition=" + sP + '(' + nP + ")");
                            if (nP < 1) // license doesn't matter
                                {
                                 // _trace("*** license doesn't matter "
                                 //       + ", ed " + nP + " " + sP);
                                iInv |= 0x4;
                                continue;
                                }
        
                            // if it's the same product, or this member is joining a
                            // data grid and the license could be used with a data
                            // grid, then include the license
                            if (!(nP == nEN ||
                                ((nEN == 1 || nEN == 5) && (nP == 1 || nP == 5))))
                                {
                                // _trace("*** prod/mode mismatch: " + xml);
                                iInv |= 0x8;
                                continue;
                                }
        
                            // check the expiry (and give them the day it expires on plus
                            // three days extra)
                            if (sT.length() > 0)
                                {
                                long ldt = 0L;
                                if (sT.length() >= 8 && sT.length() <= 10)
                                    {
                                    ldt = app.parseDate(sT) + (Base.UNIT_D << 2);
                                    }
                                if (getTimestamp() > ldt)
                                    {
                                    // _trace("*** expired: " + xml);
                                    iInv |= 0x10;
                                    continue;
                                    }
                                }
        
                            // parse the UID; all licenses MUST have a UID
                            if (sI.length() == 32 || sI.length() == 34)
                                {
                                try
                                    {
                                    id = new UID(sI);
                                    }
                                catch (Throwable e)
                                    {
                                    // _trace("*** UID parse exception (" + sI + "): " + xml + "\n" + e);
                                    iInv |= 0x20;
                                    continue;
                                    }
                                }
                            else
                                {
                                // _trace("*** sI=\"" + sI + "\", length=" + sI.length() + ", UID=" + new UID(sI));
                                iInv |= 0x40;
                                continue;
                                }
        
                            // check for encryption (3.2) or not (pre-3.2)
                            if (sE.length() == 0)
                                {
                                // key is required (it contains the CRC)
                                if (sK.length() == 0)
                                    {
                                    // _trace("*** key missing: " + xml);
                                    iInv |= 0x80;
                                    continue;
                                    }
        
                                int    nC; // CRC
                                String sC; // class name
                                try
                                    {
                                    nC = (int) Long.parseLong(sK.substring(1, 9), 16);
                                    sC = new Binary(Base.parseHex(
                                            sK.substring(9, sK.length() - 1)))
                                            .getBufferInput().readUTF();
                                    }
                                catch (Exception e)
                                    {
                                    // _trace("*** exception: " + xml + "\n" + e);
                                    iInv |= 0x100;
                                    continue;
                                    }
        
                                // validate class name (compare to edition)
                                nId = fromString(sC);
                                if (nP != nId)
                                    {
                                    // _trace("*** bad class (" + sC + "): " + xml);
                                    iInv |= 0x200;
                                    continue;
                                    }
        
                                // validate CRC
                                StringBuffer sb = new StringBuffer();
                                sb.append(sP.length() == 0 ? null : sP) // software (product edition)
                                  .append(sL.length() == 0 ? null : sL) // licensee
                                  .append(nM)                           // license type
                                  .append(sC.length() == 0 ? null : sC) // class name
                                  .append(app.parseDate(sF))            // from-date
                                  .append(app.parseDate(sT))            // to-date
                                  .append(cU)                           // user count
                                  .append("00")                         // IP from / to
                                  .append(sS.length() == 0 ? null : sS) // site
                                  .append(cS)                           // servers
                                  .append('0')                          // server units
                                  .append(cC)                           // CPUs (cores)
                                  .append(id);                          // UID
        
                                BinaryWriteBuffer bufTemp
                                        = new BinaryWriteBuffer(sb.length() + 10);
                                bufTemp.getBufferOutput().writeUTF(sb.toString());
                                if (nC != Base.toCrc(bufTemp.toByteArray()))
                                    {
                                    // _trace("*** bad checksum for class" + sC);
                                    iInv |= 0x400;
                                    continue;
                                    }
        
                                // verify the UID/CRC against the set of legit licenses
                                if (!Base.equals(sa.get(nC), id))
                                    {
                                    // _trace("*** unknown old license UID: " + id);
                                    iInv |= 0x800;
                                    continue;
                                    }
                                }
                            else
                                {
                                // compute edition id
                                nId = fromString(sP);
        
                                // verify licenses using the public key
        
                                StringBuffer sb = new StringBuffer();
                                sb.append(sP.length() == 0 ? null : sP) // software (product edition)
                                  .append(sL.length() == 0 ? null : sL) // licensee
                                  .append(sA)                           // agreement
                                  .append(nM)                           // license type
                                  .append(app.parseDate(sF))            // from-date
                                  .append(app.parseDate(sT))            // to-date
                                  .append(app.parseDate(sR))            // maintenance-renewal-date
                                  .append(cI)                           // seats
                                  .append(cU)                           // user count
                                  .append(sS.length() == 0 ? null : sS) // site
                                  .append(cS)                           // servers
                                  .append(cP)                           // sockets
                                  .append(cC)                           // CPUs (cores)
                                  .append(id);                          // UID
        
                                ByteArrayOutputStream streamRaw = new ByteArrayOutputStream();
                                DataOutputStream      stream    = new DataOutputStream(streamRaw);
        
                                stream.writeUTF(sb.toString());
                                byte abRaw[] = streamRaw.toByteArray(); // signed data
                                byte abSg[]  = Base.parseHex(sE);       // raw signature
        
                                mUp.invoke(sg, new Object[]{abRaw, Base.makeInteger(0),
                                    Base.makeInteger(abRaw.length)});
                                if (!((Boolean) mVf.invoke(sg, new Object[]{abSg})).booleanValue())
                                    {
                                    // invalid signature
                                    // _trace("*** invalid license signature: " + sE);
                                    iInv |= 0x1000;
                                    continue;
                                    }
                                 // _trace("*** license signature validated");
                                }
        
                            // determine the limit type (none, srv, sock, cpu, seat, user)
                            if (cS > 0)
                                {
                                nType = 1;
                                cStop = cS;
                                }
                            else if (cP > 0)
                                {
                                nType = 2;
                                cStop = cP;
                                }
                            else if (cC > 0)
                                {
                                nType = 3;
                                cStop = cC;
                                }
                            else if (cI > 0)
                                {
                                nType = 4;
                                cStop = cI;
                                }
                            else if (cU > 0)
                                {
                                nType = 5;
                                cStop = cU;
                                }
        
                            // if the license info is valid, then write it out
                            out.writeBoolean(true);
                            out.write(id.toByteArray());
                            ExternalizableHelper.writeInt(out, nId);
                            ExternalizableHelper.writeInt(out, nType);
                            ExternalizableHelper.writeInt(out, cStop);
        
                            // license is valid, unset associated bit
                            iInv &= ~(1 << iLc);
        
                            // _trace("*** submitted license " + id + " = " +
                            //    (new String[]{"DC", "RTC", "CC", "SE", "EE", "GE"})[nId] + "/" +
                            //    (new String[]{"site", "server", "socket", "CPU", "seat", "user"})[nType] + "/" +  cStop);
                            }
                        }
                    }
                catch (Throwable e)
                    {
                    // _trace("*** setBroadcastLimit() problem:\n" + e);
                    // e.printStackTrace();
        
                    // don't submit any licenses
                    if (buf.length() > 2)
                        {
                        // this keeps just the edition / mode information
                        buf.retain(0, 2);
                        }
        
                    iInv |= 0x2000;
                    }
                finally
                    {
                    try
                        {
                        buf.getAppendingBufferOutput().writeBoolean(false);
                        }
                    catch (Throwable e) {}
                    }
        
                // store a copy of this node's licenses that it will submit to the cluster
                setBroadcastTimestamp(buf.toBinary());
                // _trace("*** license=" + getBroadcastTimestamp());
        
                // the calculation above must be identical to com.xtangosol.license.ChecksumEncoder;
                // if we ever need to run a quick checksum test, uncomment this block
                // if (getBroadcastVariance() == 2)
                //     {
                //     _trace("\n*** hash=" + n + "\n");
                //     n = 2;
                //     }
                // _trace("*** licensed checksum " + (n == 2 ? "passed" : "failed"));
        
                n = (n == 2 ? --n : 2);
                setBroadcastMode(n);
                }
        
            // if license check failed, make it so that this node will not say "hello"
            if (n > 1)
                {
                iInv |= 0x4000; // indicates protected file tampering
                // c = Math.min(iInv, 0); // disable check for tampering to allow 1.6 builds
                }
            }
        
        __m_BroadcastLimit = (c);
        }
    
    // Accessor for the property "BroadcastMode"
    /**
     * Setter for property BroadcastMode.<p>
    * *** LICENSE ***
    * WARNING: This property name is obfuscated.
    * 
    * 0=license not checked
    * 1=license is OK
    * 2=license is hacked
    * 
    * Set by setBroadcastLimit()
     */
    private void setBroadcastMode(int n)
        {
        __m_BroadcastMode = (n);
        
        // update the local list of known licenses
        try
            {
            resetBroadcastCounter(getBroadcastTimestamp().getBufferInput(), true);
            }
        catch (Throwable e) {}
        }
    
    // Accessor for the property "BroadcastNextMillis"
    /**
     * Setter for property BroadcastNextMillis.<p>
    * The BroadcastNextMillis value is the time (in local system millis) at
    * which the next announce or request broadcast Message is scheduled to be
    * sent.
     */
    public void setBroadcastNextMillis(long cMillis)
        {
        __m_BroadcastNextMillis = cMillis;
        }
    
    // Accessor for the property "BroadcastRepeatMillis"
    /**
     * Setter for property BroadcastRepeatMillis.<p>
    * The BroadcastRepeatMillis value specifies how many milliseconds between
    * repeatedly sending announce or request broadcast Messages.
     */
    public void setBroadcastRepeatMillis(int cMillis)
        {
        __m_BroadcastRepeatMillis = cMillis;
        }
    
    // Accessor for the property "BroadcastTimeoutMillis"
    /**
     * Setter for property BroadcastTimeoutMillis.<p>
    * The BroadcastTimeoutMillis value specifies how long the ClusterService
    * will send announce or request broadcast Messages without receiving a
    * reply before assuming that no one is there to reply.
     */
    public void setBroadcastTimeoutMillis(int cMillis)
        {
        __m_BroadcastTimeoutMillis = cMillis;
        }
    
    // Accessor for the property "BroadcastTimestamp"
    /**
     * Setter for property BroadcastTimestamp.<p>
    * *** LICENSE ***
    * WARNING: This property name is obfuscated.
    * 
    * This is the binary form of the license structure for the local node. When
    * this node tries to join a cluster, this value is copied to the
    * NewMemberRequestId message, written out as a binary, and read in as a
    * series of UID / product / limit-type / limit-value values.
    * 
    * Built by ClusterService.setBroadcastLimit(int).
     */
    private void setBroadcastTimestamp(com.tangosol.util.Binary bin)
        {
        __m_BroadcastTimestamp = bin;
        }
    
    // Accessor for the property "DeliveryTimeoutMillis"
    /**
     * Setter for property DeliveryTimeoutMillis.<p>
    * The time interval in milliseconds that TCMP uses to determine Members
    * departure.
    * 
    * @see PacketPublisher#getResendTimeout
     */
    public void setDeliveryTimeoutMillis(int cMillis)
        {
        __m_DeliveryTimeoutMillis = cMillis;
        }
    
    // Accessor for the property "HeartbeatDelay"
    /**
     * Setter for property HeartbeatDelay.<p>
    * The number of milliseconds between heartbeats.
     */
    public void setHeartbeatDelay(int cMillis)
        {
        __m_HeartbeatDelay = cMillis;
        }
    
    // Accessor for the property "HeartbeatMemberSet"
    /**
     * Setter for property HeartbeatMemberSet.<p>
    * Set of Members that have this service.
     */
    protected void setHeartbeatMemberSet(com.tangosol.coherence.component.net.MemberSet setMember)
        {
        __m_HeartbeatMemberSet = setMember;
        }
    
    // Accessor for the property "LastInterminableWarningMillis"
    /**
     * Setter for property LastInterminableWarningMillis.<p>
    * The time at which the last BroadcastCounter reset warning was logged.
    * This property is also used to determine that the announcing is going
    * nowhere fast and it has issued a warning to the log.
     */
    protected void setLastInterminableWarningMillis(long cMillis)
        {
        __m_LastInterminableWarningMillis = cMillis;
        }
    
    // Accessor for the property "LastPanicUid"
    /**
     * Setter for property LastPanicUid.<p>
    * The UID of the last member caused the panic protocol activation.
     */
    protected void setLastPanicUid(com.tangosol.util.UUID uid)
        {
        __m_LastPanicUid = uid;
        }
    
    // Accessor for the property "MaximumPacketLength"
    /**
     * Setter for property MaximumPacketLength.<p>
    * The max packet size in bytes that TCMP can receive (and can/will send).
    * Used only for packed-based transports.
     */
    public void setMaximumPacketLength(int cbPacket)
        {
        __m_MaximumPacketLength = cbPacket;
        }
    
    // Accessor for the property "MembershipReopen"
    /**
     * Setter for property MembershipReopen.<p>
    * Specifies the date/time (in millis) until when new members are not
    * allowed to join the cluster [service]. This covers such rare situations
    * as existence of a "zombie" senior (possibly with an island of members),
    * when the new membership is suspended until one of the islands is removed.
     */
    public void setMembershipReopen(long ldtMillis)
        {
        __m_MembershipReopen = ldtMillis;
        }
    
    // Accessor for the property "PendingServiceJoining"
    /**
     * Setter for property PendingServiceJoining.<p>
    * This property stores a sparse array of ServiceJoining messages that this
    * senior Member is currently processing. By keeping track of these
    * messages, it is possible to know whether new members can join, or if
    * their joining needs to be temporarily deferred until the service joining
    * notification is complete.
    * 
    * When the senior Member receives a ServiceJoinRequest message it notifies
    * all other members using the ServiceJoining message (poll). Until all
    * members respond to that poll, no new Member can be accepted to the
    * cluster without compromising the synchronization of the service info. As
    * soon as the ServiceJoining poll completes, the requestor is notified and
    * all deferred NewMemberAcceptIdReply messages are delivered.
    * 
    * The array is indexed by the ServiceJoinRequest timestamp and is used to
    * remove Members that fail to respond to the senior's poll in a reasonable
    * period of time.
    * 
    * @see #DeferredAcceptMember property
    * @see #checkPendingJoinPolls
     */
    private void setPendingServiceJoining(com.tangosol.util.LongArray la)
        {
        __m_PendingServiceJoining = la;
        }
    
    // Accessor for the property "QuorumControl"
    /**
     * Setter for property QuorumControl.<p>
    * The QuorumControl manages the state and decision logic relating to
    * quorum-based membership decisions.
     */
    protected void setQuorumControl(ClusterService.QuorumControl control)
        {
        __m_QuorumControl = control;
        }
    
    // Accessor for the property "RequestMember"
    /**
     * Setter for property RequestMember.<p>
    * The Member object being used to request an id. The RequestMember object
    * differs from the AnnounceMember in that the cluster time is known before
    * the RequestMember is configured, so that its age (and thus seniority) is
    * in cluster time.
     */
    public void setRequestMember(com.tangosol.coherence.component.net.Member member)
        {
        __m_RequestMember = member;
        }
    
    // Accessor for the property "Service"
    /**
     * Setter for property Service.<p>
    * Indexed property of Service daemons running on <b>this</b> Member.
    * 
    * @volatile - read by PacketReceiver and TransportService
     */
    protected void setService(com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid[] aService)
        {
        __m_Service = aService;
        }
    
    // Accessor for the property "Service"
    /**
     * Setter for property Service.<p>
    * Indexed property of Service daemons running on <b>this</b> Member.
    * 
    * @volatile - read by PacketReceiver and TransportService
     */
    protected synchronized void setService(int i, com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid service)
        {
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        
        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid[] aService = getService();
        
        boolean fBeyondBounds = (aService == null || i >= aService.length);
        if (fBeyondBounds && service != null)
            {
            int       cNew        = i + 1;
            com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid[] aServiceNew = new com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid[cNew];
        
            // copy original data
            if (aService != null)
                {
                System.arraycopy(aService, 0, aServiceNew, 0, aService.length);
                }
        
            setService(aService = aServiceNew);
            fBeyondBounds = false;
            }
        
        if (!fBeyondBounds)
            {
            aService[i] = service;
            setService(aService); // volatile re-write just to ensure above is visable
            }
        }
    
    // Accessor for the property "ServiceContext"
    /**
     * Setter for property ServiceContext.<p>
    * A map of Service related PermissionInfo objects keyed by the service name.
     */
    public void setServiceContext(java.util.Map mapContext)
        {
        __m_ServiceContext = mapContext;
        }
    
    // Accessor for the property "ServiceInfo"
    /**
     * Setter for property ServiceInfo.<p>
    * Indexed property of ServiceInfo objects that maintain data about Services
    * running in the cluster.
     */
    protected void setServiceInfo(com.tangosol.coherence.component.net.ServiceInfo[] aInfo)
        {
        __m_ServiceInfo = aInfo;
        }
    
    // Accessor for the property "ServiceInfo"
    /**
     * Setter for property ServiceInfo.<p>
    * Indexed property of ServiceInfo objects that maintain data about Services
    * running in the cluster.
     */
    public synchronized void setServiceInfo(int i, com.tangosol.coherence.component.net.ServiceInfo info)
        {
        // import Component.Net.ServiceInfo;
        
        ServiceInfo[] ainfo = getServiceInfo();
        
        boolean fBeyondBounds = (ainfo == null || i >= ainfo.length);
        if (fBeyondBounds && info != null)
            {
            int           cNew     = i + 1;
            ServiceInfo[] ainfoNew = new ServiceInfo[cNew];
        
            // copy original data
            if (ainfo != null)
                {
                System.arraycopy(ainfo, 0, ainfoNew, 0, ainfo.length);
                }
        
            setServiceInfo(ainfo = ainfoNew);
            fBeyondBounds = false;
            }
        
        if (!fBeyondBounds)
            {
            ainfo[i] = info;
            }
        }
    
    // Accessor for the property "State"
    /**
     * Setter for property State.<p>
    * State of the ClusterService; one of:
    *     STATE_ANNOUNCE
    *     STATE_JOINING
    *     STATE_JOINED
     */
    public synchronized void setState(int nState)
        {
        // import Component.Net.Cluster;
        // import Component.Net.Member;
        // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet.MasterMemberSet;
        // import com.tangosol.net.ClusterDependencies;
        
        if (nState != getState())
            {
            switch (nState)
                {
                case STATE_JOINED:
                    {
                    // update the service info
                    Member          member    = getThisMember();
                    int             nMemberId = member.getId();
                    MasterMemberSet setMember = getClusterMemberSet();
        
                    setMember.setServiceVersion(nMemberId, getServiceVersion());
                    setMember.setServiceJoinTime(nMemberId, member.getTimestamp());
                    setMember.setServiceEndPointName(nMemberId, "");
        
                    initializeSUID();
        
                    // the IpMonitor is ready to run
                    Cluster             cluster = (Cluster) getCluster();
                    ClusterDependencies config  = cluster.getDependencies();
        
                    if (config.isIpMonitorEnabled())
                        {
                        cluster.getIpMonitor().ensureSeniority();
                        }
        
                    break;
                    }
                }
        
            __m_State = (nState);
        
            updateServiceThreadName();
            }
        
        notifyAll();
        }
    
    // Accessor for the property "StatsJoinRequests"
    /**
     * Setter for property StatsJoinRequests.<p>
    * Statistics: total number of received join requests
     */
    public void setStatsJoinRequests(long cMsgs)
        {
        __m_StatsJoinRequests = cMsgs;
        }
    
    // Accessor for the property "StatsMembersDepartureCount"
    /**
     * Setter for property StatsMembersDepartureCount.<p>
    * The number of member disconnects from the cluster.  This value is exposed
    * on the cluster MBean.
     */
    protected void setStatsMembersDepartureCount(long c)
        {
        __m_StatsMembersDepartureCount = c;
        }
    
    // Accessor for the property "TcpRing"
    /**
     * Setter for property TcpRing.<p>
    * The ClusterService's TcpRing.
     */
    protected void setTcpRing(ClusterService.TcpRing ring)
        {
        __m_TcpRing = ring;
        }
    
    // Accessor for the property "TimestampAdjustment"
    /**
     * Setter for property TimestampAdjustment.<p>
    * The number of milliseconds off the cluster time is from the local system
    * time.
     */
    public void setTimestampAdjustment(long cMillis)
        {
        __m_TimestampAdjustment = cMillis;
        }
    
    // Accessor for the property "TimestampMaxVariance"
    /**
     * Setter for property TimestampMaxVariance.<p>
    * The maximum number of apparent milliseconds difference allowed between
    * send and receive time when determining how far off the cluster time is
    * from the local system time.
     */
    public void setTimestampMaxVariance(int cMillis)
        {
        __m_TimestampMaxVariance = cMillis;
        }
    
    // Accessor for the property "WellKnownAddresses"
    /**
     * Setter for property WellKnownAddresses.<p>
    * The well-known-addresses or null for multicast
     */
    public void setWellKnownAddresses(java.util.Set setWka)
        {
        __m_WellKnownAddresses = setWka;
        }
    
    // Accessor for the property "WkaMap"
    /**
     * Setter for property WkaMap.<p>
    * *** LICENSE ***
    * WARNING: This property name is obfuscated.
    * 
    * Map keyed by UID, with each value an int[] of:
    * [0] edition (0=DC, ..5=DGE)
    * [1] limit type (0=site, 1=srv, 2=sock, 3=cpu, 4=seat, 5=user)
    * [2] limit value
    * 
    * Maintained by resetBroadcastCounter()
     */
    private void setWkaMap(java.util.Map map)
        {
        __m_WkaMap = map;
        }
    
    // Declared at the super level
    /**
     * Stop the Controllable.
     */
    public void shutdown()
        {
        // import Component.Net.Cluster;
        
        Cluster cluster = (Cluster) getCluster();
        if (cluster.getState() < Cluster.STATE_LEAVING)
            {
            throw new IllegalStateException("Cannot shutdown ClusterService"
                + " without shutting down the Cluster");
            }
        
        super.shutdown();
        }
    
    // Declared at the super level
    /**
     * Hard-stop the Service. Use shutdown() for normal  termination.
     */
    public void stop()
        {
        // import Component.Net.Cluster;
        
        Cluster cluster = (Cluster) getCluster();
        if (isAcceptingClients() && cluster.getState() < Cluster.STATE_LEAVING)
            {
            _trace("Requested to stop cluster service.", 1);
            cluster.stop();
            }
        else
            {
            super.stop();
            }
        }
    
    /**
     * Unegister ServiceJoined message
     */
    public void unregisterServiceJoining(ClusterService.ServiceJoining msg)
        {
        // import com.tangosol.util.LongArray;
        // import java.util.Iterator;
        
        // only permitted to occur on the ClusterService thread itself
        _assert(Thread.currentThread() == getThread());
        
        boolean   fRemoved  = false;
        LongArray laPending = getPendingServiceJoining();
        for (Iterator iter = laPending.iterator(); iter.hasNext();)
            {
            if (msg.equals(iter.next()))
                {
                iter.remove();
                fRemoved = true;
                break;
                }
            }
        _assert(fRemoved);
        }
    
    /**
     * Validates the sender (new member) when broadcasting to announce presence
    * and request an ID. Uses this information to implement death detection by
    * assuming that an address/port uniquely identifies a member, so an
    * existing member with that same address/port must be dead.
    * 
    * @param uid  the Member uid of the new Member
    * 
    * @return true if the Member uid is fine (no conflict); false if the Member
    * uid was in use and that old Member has been "killed"
     */
    public boolean validateNewMember(com.tangosol.coherence.component.net.Member memberNew)
        {
        // import Component.Net.Member;
        // import Component.Net.MemberSet;
        // import java.net.InetAddress;
        // import java.util.Iterator;
        
        InetAddress addrNew  = memberNew.getAddress();
        int         nPortNew = memberNew.getPort();
        
        // check for a duplicate address and port;
        // if already exists it can only mean that another member
        // has died and a new one took its address/port
        MemberSet setMember = getClusterMemberSet();
        for (Iterator iter = setMember.iterator(); iter.hasNext();)
            {
            Member member = (Member) iter.next();
            if (member.getPort() == nPortNew
                    && member.getAddress().equals(addrNew)
                    && !member.getUid32().equals(memberNew.getUid32()))
                {
                _trace("New member uses existing address/port; " +
                       "killing the old member: " + member, 3);
                doMemberLeft(member);
                return false;
                }
            }
        
        return true;
        }
    
    /**
     * Check if the new member is allowed to join the specified service.
    * 
    * @param info  the service info
    * @param member the cluster member that the service is running on
    * @param continuation an operation that should be performed when the
    * service acknowledges the joining
    * @param piRequest the information needed to validate security
     */
    public void validateNewService(com.tangosol.coherence.component.net.ServiceInfo info, com.tangosol.coherence.component.net.Member member, com.oracle.coherence.common.base.Continuation continuation, com.tangosol.net.security.PermissionInfo piRequest)
        {
        // import Component.Net.Member;
        // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
        // import Component.Util.Daemon.QueueProcessor.Service.Grid$NotifyServiceJoining as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyServiceJoining;
        
        _assert(info != null && member != null);
        
        int nServiceId = info.getServiceId();
        _assert(nServiceId > 0);
        
        Member memberThis = getThisMember();
        if (member == memberThis)
            {
            // validation is not necessary and notifications are _not_ sent
            // when this Member is the Member that caused the notification
            }
        else
            {
            com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid serviceJoining;
            synchronized (this) // protect against concurrent restart
                {
                serviceJoining = getService(nServiceId);
                }
        
            ServiceMemberSet setMember = info.getMemberSet();
        
            // member-joining-the-service notification
            if (serviceJoining != null)
                {
                Member memberOldest = setMember.getOldestMember();
        
                // send an internal notification *only* if the joining service on this node
                // is ready for service-level communications
                // (see ClusterService.ServiceJoinRequest.Poll.onCompletion)
                if (serviceJoining.getServiceMemberSet() != null)
                    {
                    int nMember = member.getId();
        
                    // notify the related service via a poll
                    com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyServiceJoining msg = (com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyServiceJoining)
                            serviceJoining.instantiateMessage("NotifyServiceJoining");
                    msg.setNotifyMember(member);
                    msg.setNotifyServiceVersion(setMember.getServiceVersion(nMember));
                    msg.setNotifyServiceJoinTime(setMember.getServiceJoinTime(nMember));
                    msg.setNotifyServiceEndPointName(setMember.getServiceEndPointName(nMember));
                    msg.setNotifyMemberConfigMap(setMember.getMemberConfigMap(nMember));
                    msg.setPermissionInfo(piRequest);
                    msg.addToMember(memberThis);
        
                    if (continuation != null)
                        {
                        ClusterService.NotifyResponse msgContinue = (ClusterService.NotifyResponse)
                            instantiateMessage("NotifyResponse");
                        msgContinue.setService(this);
                        msgContinue.setContinuation(continuation);
                        continuation = null;
        
                        // it will be sent when com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyServiceJoining poll is closed
                        msg.setContinuationMessage(msgContinue);
                        }
                    serviceJoining.send(msg); // poll
                    }
                }
            }
        
        if (continuation != null)
            {
            continuation.proceed(null);
            }
        }
    
    /**
     * Validates the sender when a broadcast Message is received that should
    * only be received from a senior Member.
    * 
    * @param message the senior broadcast message which was received
    * @param setFrom  the set of Member ids known to by the senior Member
    * 
    * @return true if the broadcast is known to be valid; false if it's invalid
    * or cannot be validated
     */
    public boolean validateSeniorBroadcast(com.tangosol.coherence.component.net.message.DiscoveryMessage message, com.tangosol.coherence.component.net.MemberSet setFrom)
        {
        // import Component.Net.Member;
        // import Component.Net.MemberSet;
        // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet.MasterMemberSet;
        // import com.tangosol.run.component.EventDeathException;
        // import com.tangosol.util.Base;
        // import com.tangosol.util.UUID;
        
        if (isExiting())
            {
            return false;
            }
        
        Member memberFrom = message.getFromMember();
        
        if (message.isReadError())
            {
            // version mismatch
            if (getClusterMemberSet().size() <= 1)
                {
                // stop this lone node
                onMemberRejected(REJECT_VERSION, memberFrom);
                throw new EventDeathException("Version mismatch");
                }
            else
                {
                // we likely can't communicate with the other node; ignore
                _trace("Detected another cluster senior, running on an incompatible "
                       + "protocol at " + message.getSourceAddress()
                       + " manual intervention may be required"
                       , 1);
                return false;
                }
            }
        
        if (message instanceof ClusterService.SeniorMemberHeartbeat &&
            ((ClusterService.SeniorMemberHeartbeat) message).isWkaEnabled() &&
            !Base.equals(getClusterName(), message.getClusterName()))
            {
            // COH-3803: received a broadcast from a conflicting (but distinct)
            //           WKA cluster; we should ignore it completely
            return false;
            }
        
        switch (getState())
            {
            case STATE_ANNOUNCE:
            case STATE_JOINING:
                if (message instanceof ClusterService.SeniorMemberHeartbeat &&
                    isWkaEnabled() != ((ClusterService.SeniorMemberHeartbeat) message).isWkaEnabled())
                    {
                    // WKA mismatch; stop this node
                    onMemberRejected(REJECT_WKA, memberFrom);
                    throw new EventDeathException("WKA mismatch"); 
                    }
                else if (!message.getClusterName().equals(getClusterName()))
                    {
                    // cluster name mismatch (this node must have port sharing disabled)
                    onMemberRejected(REJECT_CLUSTER_NAME, memberFrom);
                    throw new EventDeathException("Cluster name mismatch");
                    }
        
                // a senior is saying something; that means a cluster is running
                resetBroadcastCounter("the presence of an existing cluster" +
                    " that does not respond to join requests;"  +
                    " this is usually caused by a network layer failure",
                    message);
        
                // if the senior is not a WKA we need to add it to the broadcast list
                addDynamicBroadcast(memberFrom);
        
            default:
                return false;
        
            case STATE_JOINED:
                break;
            }
        
        MasterMemberSet setMember    = getClusterMemberSet();
        Member          memberThis   = setMember.getThisMember();
        Member          memberOldest = setMember.getOldestMember();
        
        // cannot prove or disprove senior Member until Members are known
        if (memberThis == null || memberOldest == null)
            {
            return false;
            }
        
        // verify that the broadcast was from the senior (oldest) Member
        if (memberFrom.equals(memberOldest))
            {
            return true;
            }
        
        // it can't be from this Member (right?) since it is from the
        // oldest Member and this is not the oldest Member
        UUID uidFrom = memberFrom.getUid32();
        UUID uidThis = memberThis.getUid32();
        _assert(!uidFrom.equals(uidThis));
        
        if (memberThis == memberOldest &&
            (message instanceof ClusterService.SeniorMemberHeartbeat && ((ClusterService.SeniorMemberHeartbeat) message).isWkaEnabled()))
            {
            // in the case when multicast is disabled, the only way to force
            // a disconnected non-WKA to terminate itself is to send it a heartbeat
        
            if (isWkaEnabled())
                {
                // include the suspect in our next senior heartbeat; we defer sending it here as that would
                // cause a very fast ping/pong game between the two seniors
                addDynamicBroadcast(memberFrom);
                }
            else
                {
                // if we're multicast enabled and the other side is not the only way it will hear us is for
                // us to send a directed heartbeat
                ClusterService.SeniorMemberHeartbeat msg = (ClusterService.SeniorMemberHeartbeat)
                        instantiateMessage("SeniorMemberHeartbeat");
                msg.setToMember(memberFrom);
                msg.setLastReceivedMillis(calcTimestamp(
                        memberThis.getLastIncomingMillis()));
                msg.setMemberSet(setMember);
                msg.setWkaEnabled(isWkaEnabled());
                msg.setLastJoinTime(setMember.getLastJoinTime());
                send(msg);
                }
            }
        
        long ldtCurrent = Base.getSafeTimeMillis();
        long cTimeout   = getDeliveryTimeoutMillis();
        long cHeartbeat = getHeartbeatDelay();
        
        // verify that the broadcast is not from a recently killed member
        Member  memberDead      = setMember.findDeadMember(memberFrom);
        boolean fPanicActivated = uidFrom.equals(getLastPanicUid());
        if (memberDead != null)
            {
            // cluster heartbeat from a known dead member
            _assert(memberDead.isDead());
        
            // the dead member has already or will now be declared as a zombie;
            // while there is a zombie [senior], block any new membership
            // for at least a quarter of the timeout period if panic protocol
            // has not activated
            if (!fPanicActivated)
                {
                setMembershipReopen(ldtCurrent + cTimeout/4);
                }
        
            if (!memberDead.isZombie())
                {
                // declare and warn about new zombie
                _trace("The member formerly known as " + memberDead +
                       " has been forcefully evicted from the cluster, but continues" +
                       " to emit a cluster heartbeat; henceforth, the member will be" +
                       " shunned and its messages will be ignored.", 2);
                memberDead.declareZombie();
                return false; // don't take any action yet
                }
        
            // A zombie continues to broadcast; periodically take action if
            // we are the senior or we believe our senior may not be aware of the zombie.
            // Note: !isWellKnown(memberDead) implies that WKA is enabled, and that this
            // node is a WKA (it heard the cluster heartbeat); there is a chance that the
            // senior is not a WKA and may not be hearing the zombie's heartbeat
            if (ldtCurrent > memberDead.getTimestamp() + cTimeout &&
                (memberThis == memberOldest || !isWellKnown(memberDead)))
                {
                memberDead.declareZombie(); // this will renew the timestamp
                setLastPanicUid(null);      // reset the panic protocol
                fPanicActivated = false;
                }
            else
                {
                return false;
                }
            }
        
        if (memberThis == memberOldest)
            {
            if (setFrom == null)
                {
                // not enough data to make the senior panic determination
                return false;
                }
        
            int cSizeThis = setMember.size();
            int cSizeFrom = setFrom.size();
        
            // verify that the broadcast is not from a member
            // that this senior considers a valid junior member
            if (setMember.getMember(uidFrom) != null
                 && cSizeThis - cSizeFrom < cSizeFrom
                 && setMember.containsAll(setFrom))
                {
                // this senior was apparently crossed out from the cluster while in GC;
                // since the seniority has been transfered to someone else
                // there is no way this member can regain it
                _trace("This senior " + memberThis + 
                       " appears to have been disconnected from other nodes due to a long" +
                       " period of inactivity and the seniority has been assumed by the " +
                       memberFrom + "; stopping cluster service.", 1);
        
                onStopRunning();
                return false;
                }
        
            if (!fPanicActivated)
                {
                // suspend the membership for a little while
                setMembershipReopen(ldtCurrent + 4*cHeartbeat);
                }
        
            if (cSizeThis == 1 || cSizeFrom == 1)
                {
                boolean fStop;
        
                // if one of the islands is trivial (one member), we don't have to
                // resort to the panic protocol to resolve the problem
                if (cSizeThis > 1)
                    {
                    // another Member is alone; it will commit suicide
                    fStop = false;
                    }
                else if (cSizeFrom > 1)
                    {
                    // this Member is alone; it has to commit suicide
                    fStop = true;
                    }
                else // (cSizeThis == 1 && cSizeFrom == 1)
                    {
                    // both are alone; the younger one has to go
                    fStop = uidThis.compareTo(uidFrom) > 0;
                    }
        
                if (fStop)
                    {
                    _trace("This senior " + memberThis +
                           " appears to have been disconnected from another senior " +
                           memberFrom + "; stopping cluster service.", 1);
        
                    onStopRunning();
                    }
                else
                    {
                    long ldtWarning = getLastInterminableWarningMillis();
                    if (ldtCurrent > ldtWarning + cTimeout)
                        {
                        if (ldtWarning > 0L)
                            {
                            String sCause = cSizeThis == 1 ? "younger that this member" :
                                                                 "the only member of its cluster";
                            _trace("This senior " + memberThis +
                                   " appears to have been disconnected from another senior " +
                                   memberFrom + ", which is " + sCause +
                                   ", but did not respond to any of the termination requests" +
                                   "; manual intervention may be necessary to stop that process.", 2);
                                setLastPanicUid(uidFrom); // manual panic
                             }
                        setLastInterminableWarningMillis(ldtCurrent);
                        }
                    }
                return false;
                }
        
            if (cSizeThis < cSizeFrom ||
                    (cSizeThis == cSizeFrom && uidThis.compareTo(uidFrom) > 0))
                {
                // the size of this island is smaller than another one
                // or the sizes are the same, but another senior is older;
                // let the other island initiate the panic protocol
                return false;
                }
        
            if (!fPanicActivated)
                {
                // panic to all other cluster Members
                _trace("An existence of a cluster island with senior " + memberFrom +
                       " containing " + cSizeFrom + " nodes have been detected." +
                       " Since this " + memberThis + " is the senior of " +
                       (cSizeThis > cSizeFrom ? "a larger" : "an older") +
                       " cluster island, the panic protocol is being activated to stop the" +
                       " other island's senior and all junior nodes that belong to it.", 2);
        
                setLastPanicUid(uidFrom);
                ClusterService.SeniorMemberPanic msg = (ClusterService.SeniorMemberPanic)
                    instantiateMessage("SeniorMemberPanic");
                
                MemberSet setTo = new MemberSet();
                setTo.addAll(setMember);
                setTo.remove(memberThis);
                msg.setCulpritMember(memberFrom);
                msg.setToMemberSet(setTo);
                send(msg);
                }
            }
        else if (!fPanicActivated)
            {
            // we are a junior member; panic to senior member
            setLastPanicUid(uidFrom);
        
            _trace("Notifying the senior " + memberOldest + " of an unexpected cluster" +
                   " heartbeat from " +  (memberDead == null ? "" : "departed ") +
                   memberFrom, 2);
        
            ClusterService.SeniorMemberPanic msg = (ClusterService.SeniorMemberPanic)
                instantiateMessage("SeniorMemberPanic");
            msg.setCulpritMember(memberFrom);
            msg.addToMember(memberOldest);
            send(msg);
            }
        
        // was not the valid senior Member
        return false;
        }
    
    /**
     * Confirm the specified member departure that was diagnosed due to the
    * failure to deliver the specified packet.
    * 
    * @param memberSuspect  the suspect member
    * @param packet                   the undeliverable packet, or null for bus
    * based delivery
    * 
    * @return true iff the departure has been established/confirmed
    * 
    * @see Cluster$PacketPublisher#onUndeliverablePacket()
     */
    protected boolean verifyMemberLeft(com.tangosol.coherence.component.net.Member memberSuspect, com.tangosol.coherence.component.net.packet.MessagePacket packet)
        {
        // import Component.Net.Member;
        // import Component.Net.MemberSet;
        // import com.tangosol.util.Base;
        // import java.util.Collections;
        // import java.util.Set;
        
        if (!isRunning())
            {
            return false;
            }
        
        if (memberSuspect == null)
            {
            return true;
            }
        
        // we have three options here:
        // - announce the local member deaf and therefore dead
        // - announce the suspected member dead
        // - mark the suspected member deaf and request confirmation from others,
        //   but take no destructive action at this time
        
        MemberSet setMembers = getClusterMemberSet();
        int       cMembers   = setMembers.size();
        if (packet != null && !isHeuristicallyDead(memberSuspect))
            {
            // We have heard from the suspect within the timeout interval, but
            // still haven't been able to deliver a packet. Who should die?
         
            Set setSlow = getSlowMembers();
            if (setSlow == null || !setSlow.contains(memberSuspect))
                {
                // this could only happen if this member went into a very long GC;
                // so it failed to notice the slowness of another member
                // call verifyMemberSlow() just to generate the log message
                verifyMemberSlow(memberSuspect, packet);
                
                _trace("This member is running extremely slowly and may endanger "
                     + "the rest of the cluster. Marking this member as suspect.", 1);
        
                onMembersTimedOut(Collections.singleton(getThisMember()));
                return false;
                }
        
            // TODO:
            // 1) consider making the number of possible offenders to be a function
            //    (slow growing logarithm) of the overall membership size to support
            //    extremely large clusters
            // 2) consider checking whether all slow nodes reside on the same machine
            //    and use that info
            // 3) collect additional information to differentiate between this member
            //    going through long GC pauses or the suspect beeing slow
        
            // ask all running services to choose which member is less important
            // until we can positively identify this member's being a culprit
            // ALWAYS prefer a higher priority member (i.e. cache server) to stay alive
        
            int cSlow = setSlow.size();
            int iVote = compareImportance(memberSuspect);
            if (iVote > 0 || (iVote == 0 && cSlow == 1))
                {
                // kill the suspect
                if (!memberSuspect.isTimedOut())
                    {
                    StringBuffer sb = new StringBuffer();
                    sb.append("Timeout while delivering a packet");
                    if (cSlow == 1)
                        {
                        // among all members only the suspect is slow
                        sb.append("to ")
                          .append(getMemberStatsDescription(memberSuspect))
                          .append("; the member appears to be alive, but exhibits long periods of unresponsiveness ");
                        }
                    else
                        {
                        sb.append(" to a lower priority ")
                          .append(getMemberStatsDescription(memberSuspect));
                        }
                    sb.append(". Marking Member ")
                      .append(memberSuspect.getId())
                      .append(" as suspect.");
        
                    _trace(sb.toString(), 2);
                    }
                return true;
                }
        
            // We are less important then the suspect, and it may be our fault.
            // Committing suicide without any external confirmation can oddly be
            // more dangerous to the cluster then killing a more important node.
            // This is because many nodes could be in the same state as us and if
            // we all simply killed ourselves, a significant portion of the cluster
            // could be lost. Fall through to the witness protocol.
            }
        
        if (memberSuspect.isDeaf())
            {
            // the request has already been sent;
            // continue waiting for confirmations or rejections
            }
        else if (memberSuspect.isDead())
            {
            // it has just been marked as dead (by the ClusterService thread)
            return true;
            }
        else
            {
            // compute the witness member set and start the witness protocol
            MemberSet setWitness = getWitnessMemberSet(memberSuspect);
            if (setWitness == null)
                {
                // no one is acknowledging; assume myself deaf or mute
                // Note: prior to Coherence 3.6, the documented system property
                //       "tangosol.coherence.departure.threshold" was used to
                //       decide on kill/suicide here.  As of Coherence 3.6, such
                //       decisions are managed by quorum policies (see ClusterService.QuorumControl)
        
                if (!getThisMember().isTimedOut())
                    {
                    // log a message (but once per outtage only)
                    StringBuffer sb = new StringBuffer();
                    sb.append("This node appears to have become disconnected from the rest of the cluster containing ")
                      .append(cMembers - 1)
                      .append(" nodes. All departure confirmation requests went unanswered.");
                    _trace(sb.toString(), 1);
                    }
        
                onMembersTimedOut(Collections.singleton(getThisMember()));
                return false;
                }
        
            memberSuspect.setDeaf(true);
        
            StringBuffer sb = new StringBuffer();
            sb.append("Timeout while delivering a " + (packet == null ? "message" : "packet"));
            if (packet != null && _isTraceEnabled(6))
                {
                sb.append(' ')
                  .append(packet);         
                }
            sb.append("; requesting time-out confirmation for ")
               .append(getMemberStatsDescription(memberSuspect))
               .append("\nby ")
               .append(setWitness);
        
            _trace(sb.toString(), 2);
        
            // send the confirmation request to the witnesses
            ClusterService.WitnessRequest msg = (ClusterService.WitnessRequest) instantiateMessage("WitnessRequest");
            msg.setToMemberSet(setWitness);
            msg.setMemberUUID(memberSuspect.getUid32());
        
            send(msg); // poll
            }
        
        // no action at this point
        return false;
        }
    
    /**
     * Mark the specified member as "slow" for its failure to deliver the
    * specified packet within 3/4 of the timeout interval.
    * 
    * @see Cluster$PacketPublisher#onSlowPacket()
     */
    public void verifyMemberSlow(com.tangosol.coherence.component.net.Member memberSlow, com.tangosol.coherence.component.net.packet.MessagePacket packet)
        {
        // import Component.Net.Cluster;
        // import Component.Net.Member;
        // import Component.Net.MemberSet;
        // import com.tangosol.net.InetAddressHelper;
        // import com.tangosol.util.Base;
        
        if (!isRunning() || memberSlow == null)
            {
            return;
            }
        
        long lCurrentMillis = Base.getSafeTimeMillis();
        long lLastInMillis  = memberSlow.getLastIncomingMillis();
        int  cTimeoutMillis = getDeliveryTimeoutMillis();
        
        if (lCurrentMillis - lLastInMillis < (cTimeoutMillis >> 1))
            {
            // this packet has failed to be delivered for at least 3/4 of the timeout interval,
            // but other packets were successfully ack'ed within half timeout interval
        
            if (!isSlow(memberSlow))
                {
                // entering the slow state...
                long      lFirstSentMillis = packet.getResendTimeout() - cTimeoutMillis;
                long      lFailedAckMillis = lCurrentMillis - lFirstSentMillis;
                long      cRecentMillis    = lCurrentMillis - lLastInMillis;
                int       cbPacket         = packet.getLength();
                MemberSet setTo            = packet.getToMemberSet();
                boolean   fMulticast       = setTo != null && !isWkaEnabled() &&
                                             ((Cluster) getCluster()).getPublisher().isMulticast(setTo.size());
        
                StringBuffer sb = new StringBuffer();
                sb.append("A potential communication problem has been detected. ")
                  .append("A packet has failed to be acknowledged");
                if (setTo != null && setTo.size() > 1)
                    {
                    sb.append(" by ")
                      .append(setTo.size())
                      .append(" members");
                    }
                sb.append(" after ")
                  .append(lFailedAckMillis/1000)
                  .append(" seconds, although other packets were acknowledged by ")
                  .append(getMemberStatsDescription(memberSlow))
                  .append(" to this ").append(getThisMember())
                  .append(" as recently as ").append(cRecentMillis/1000)
                  .append(" seconds ago. ");
        
                if (fMulticast || cbPacket > 1468)
                    {
                    if (fMulticast)
                        {
                        sb.append("It is possible this was caused by a multicast failure");
                        if (!InetAddressHelper.PreferIPv4Stack)
                            {
                            sb.append("; you may need to run the JVM with the system property ")
                              .append("\"-Djava.net.preferIPv4Stack=true\"");
                            }
                        sb.append(". ");
                        }
                    if (cbPacket > 1468)
                        {
                        sb.append("It is possible that the packet size greater than ")
                          .append(cbPacket).append(" is responsible; for example, some network ")
                          .append("equipment cannot handle packets larger than 1472 bytes (IPv4) or 1468 bytes (IPv6). ")
                          .append("Use the 'ping' command with the <size> option to verify successful")
                          .append(" delivery of specifically sized packets.");
                        }
                    sb.append(" Other possible");
                    }
                else
                    {
                    sb.append(" Possible");
                    }
        
                sb.append(" causes include network failure, poor thread scheduling (see FAQ if running on Windows), ")
                  .append("an extremely overloaded server, a server that is attempting to run its ")
                  .append("processes using swap space, and unreasonably lengthy GC times.");
        
                _trace(sb.toString(), 2);
                }
            memberSlow.setLastSlowMillis(lCurrentMillis);
            }
        else
            {
            // this looks like a regular member departure...
            }
        
        // TODO: consider communicating and confirming the slowness with other cluster members
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService$MemberHeartbeat
    
    /**
     * Message:
     *     MemberHeartbeat
     * 
     * Purpose:
     *     Informs one or more Members that the sender is alive.
     * 
     * Description:
     *     Cluster Members send heartbeats to each other to verify that the
     * other cluster Members are still alive. This works because any
     * undeliverable non-broadcast Message is assumed to mean that the would-be
     * recipient Member is dead.
     * 
     * Attributes:
     *     FromMemberUid
     *     LastReceivedMillis
     * 
     * Response to:
     *     n/a
     * 
     * Expected responses:
     *     n/a
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class MemberHeartbeat
            extends    com.tangosol.coherence.component.net.Message
        {
        // ---- Fields declarations ----
        
        /**
         * Property LastReceivedMillis
         *
         * The sending Member includes the date/time (in cluster time) that it
         * last received an AckPacket.
         */
        private long __m_LastReceivedMillis;
        
        // Default constructor
        public MemberHeartbeat()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public MemberHeartbeat(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);
            
            if (fInit)
                {
                __init();
                }
            }
        
        // Main initializer
        public void __init()
            {
            // private initialization
            __initPrivate();
            
            // state initialization: public and protected properties
            try
                {
                setMessageType(33);
                }
            catch (java.lang.Exception e)
                {
                // re-throw as a runtime exception
                throw new com.tangosol.util.WrapperException(e);
                }
            
            // signal the end of the initialization
            set_Constructed(true);
            }
        
        // Private initializer
        protected void __initPrivate()
            {
            
            super.__initPrivate();
            }
        
        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
        * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService.MemberHeartbeat();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ClusterService$MemberHeartbeat".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }
        
        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
        * time] parent component.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent();
            }
        
        // Declared at the super level
        /**
         * Getter for property Description.<p>
        * Used for debugging purposes (from toString). Create a human-readable
        * description of the specific Message data.
         */
        public String getDescription()
            {
            // import java.util.Date;
            
            long   lLastReceivedMillis = getLastReceivedMillis();
            String sLastReceivedMillis = (lLastReceivedMillis == 0L ?
                    "none" : new Date(lLastReceivedMillis).toString());
            
            return "LastReceivedMillis=" + sLastReceivedMillis;
            }
        
        // Accessor for the property "LastReceivedMillis"
        /**
         * Getter for property LastReceivedMillis.<p>
        * The sending Member includes the date/time (in cluster time) that it
        * last received an AckPacket.
         */
        public long getLastReceivedMillis()
            {
            return __m_LastReceivedMillis;
            }
        
        // Declared at the super level
        /**
         * This is the event that is executed when a Message is received.
        * <p>
        * It is the main processing event of the Message called by the
        * <code>Service.onMessage()</code> event. With regards to the use of
        * Message components within clustered Services, Services are designed
        * by dragging Message components into them as static children. These
        * Messages are the components that a Service can send to other running
        * instances of the same Service within a cluster. When the onReceived
        * event is invoked by a Service, it means that the Message has been
        * received; the code in the onReceived event is therefore the Message
        * specific logic for processing a received Message. For example, when
        * onReceived is invoked on a Message named FindData, the onReceived
        * event should do the work to "find the data", because it is being
        * invoked by the Service that received the "find the data" Message.
         */
        public void onReceived()
            {
            // the purpose of the heart-beat is to prove that the Message
            // can be delivered at all; there is nothing for the recipient
            // to do
            
            super.onReceived();
            }
        
        // Declared at the super level
        public void read(com.tangosol.io.ReadBuffer.BufferInput input)
                throws java.io.IOException
            {
            setLastReceivedMillis(input.readLong());
            }
        
        // Accessor for the property "LastReceivedMillis"
        /**
         * Setter for property LastReceivedMillis.<p>
        * The sending Member includes the date/time (in cluster time) that it
        * last received an AckPacket.
         */
        public void setLastReceivedMillis(long cMillis)
            {
            __m_LastReceivedMillis = cMillis;
            }
        
        // Declared at the super level
        public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
                throws java.io.IOException
            {
            output.writeLong(getLastReceivedMillis());
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService$MemberJoined
    
    /**
     * Message:
     *     MemberJoined
     * 
     * Purpose:
     *     Informs non-senior Members of the cluster that a new Member has
     * joined (has been assigned an id).
     * 
     * Description:
     *     Sent by the senior Member as soon as it assigns an id to a new
     * Member. Although this Message is in response to a successfully-processed
     * NewMemberRequestId message, this Message is _not_ sent to the new
     * Member, but rather is sent to all other Members, who are then expected
     * to sync with the new Member by responding to NewMemberWelcomeRequest.
     * 
     * Attributes:
     *     MemberId
     *     ServiceVersion
     * 
     * Response to:
     *     NewMemberRequestId (from the new Member)
     * 
     * Expected responses:
     *     n/a
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class MemberJoined
            extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.MemberJoined
        {
        // ---- Fields declarations ----
        
        /**
         * Property Member
         *
         * The member who has joined.
         */
        private com.tangosol.coherence.component.net.Member __m_Member;
        
        /**
         * Property ServiceVersion
         *
         * The version of the ClusterService that the new Member is running.
         */
        private String __m_ServiceVersion;
        
        // Default constructor
        public MemberJoined()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public MemberJoined(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);
            
            if (fInit)
                {
                __init();
                }
            }
        
        // Main initializer
        public void __init()
            {
            // private initialization
            __initPrivate();
            
            // state initialization: public and protected properties
            try
                {
                setMessageType(4);
                }
            catch (java.lang.Exception e)
                {
                // re-throw as a runtime exception
                throw new com.tangosol.util.WrapperException(e);
                }
            
            // signal the end of the initialization
            set_Constructed(true);
            }
        
        // Private initializer
        protected void __initPrivate()
            {
            
            super.__initPrivate();
            }
        
        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
        * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService.MemberJoined();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ClusterService$MemberJoined".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }
        
        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
        * time] parent component.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent();
            }
        
        // Declared at the super level
        /**
         * Getter for property Description.<p>
        * Used for debugging purposes (from toString). Create a human-readable
        * description of the specific Message data.
         */
        public String getDescription()
            {
            return super.getDescription()
                + "\nServiceVersion=" + getServiceVersion();
            }
        
        // Accessor for the property "Member"
        /**
         * Getter for property Member.<p>
        * The member who has joined.
         */
        public com.tangosol.coherence.component.net.Member getMember()
            {
            return __m_Member;
            }
        
        // Accessor for the property "ServiceVersion"
        /**
         * Getter for property ServiceVersion.<p>
        * The version of the ClusterService that the new Member is running.
         */
        public String getServiceVersion()
            {
            return __m_ServiceVersion;
            }
        
        // Declared at the super level
        /**
         * This is the event that is executed when a Message is received.
        * <p>
        * It is the main processing event of the Message called by the
        * <code>Service.onMessage()</code> event. With regards to the use of
        * Message components within clustered Services, Services are designed
        * by dragging Message components into them as static children. These
        * Messages are the components that a Service can send to other running
        * instances of the same Service within a cluster. When the onReceived
        * event is invoked by a Service, it means that the Message has been
        * received; the code in the onReceived event is therefore the Message
        * specific logic for processing a received Message. For example, when
        * onReceived is invoked on a Message named FindData, the onReceived
        * event should do the work to "find the data", because it is being
        * invoked by the Service that received the "find the data" Message.
         */
        public void onReceived()
            {
            // skip super.onReceived();
            
            ClusterService service = (ClusterService) getService();
            service.ensureMember(getMember(), getServiceVersion());
            }
        
        // Declared at the super level
        public void read(com.tangosol.io.ReadBuffer.BufferInput input)
                throws java.io.IOException
            {
            // import Component.Net.Member;
            
            super.read(input);
            
            ClusterService service = (ClusterService) getService();
            Member  member  = service.instantiateMember();
            member.readExternal(input);
            member.setTcpRingPort(input.readInt());
            setMember(member);
            setServiceVersion(input.readUTF());
            }
        
        // Accessor for the property "Member"
        /**
         * Setter for property Member.<p>
        * The member who has joined.
         */
        public void setMember(com.tangosol.coherence.component.net.Member member)
            {
            __m_Member = member;
            }
        
        // Accessor for the property "ServiceVersion"
        /**
         * Setter for property ServiceVersion.<p>
        * The version of the ClusterService that the new Member is running.
         */
        public void setServiceVersion(String sVersion)
            {
            __m_ServiceVersion = sVersion;
            }
        
        // Declared at the super level
        public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
                throws java.io.IOException
            {
            super.write(output);
            
            getMember().writeExternal(output);
            output.writeInt(getMember().getTcpRingPort());
            output.writeUTF(getServiceVersion());
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService$MemberLeaving
    
    /**
     * Message:
     *     MemberLeaving
     * 
     * Purpose:
     *     Informs all Members of the cluster of the intention of a Member to
     * leave.
     * 
     * Description:
     *     Sent by a Member when it knows it is leaving the cluster. A Member
     * could die, in which case the "leaving" and "left" Messages may never
     * come from the Member, and when the Member is determined to be dead, only
     * a "left" Message will be sent (from whatever other Member determined
     * that the Member died).
     * 
     * Attributes:
     *     MemberId
     *     MemberUid
     * 
     * Response to:
     *     n/a
     * 
     * Expected responses:
     *     n/a
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class MemberLeaving
            extends    com.tangosol.coherence.component.net.Message
        {
        // ---- Fields declarations ----
        
        /**
         * Property MemberId
         *
         * The id of the Member that is leaving the cluster.
         */
        private int __m_MemberId;
        
        /**
         * Property MemberUid
         *
         * The UID of the Member that is leaving the cluster.
         */
        private com.tangosol.util.UUID __m_MemberUid;
        
        // Default constructor
        public MemberLeaving()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public MemberLeaving(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);
            
            if (fInit)
                {
                __init();
                }
            }
        
        // Main initializer
        public void __init()
            {
            // private initialization
            __initPrivate();
            
            // state initialization: public and protected properties
            try
                {
                setMessageType(35);
                }
            catch (java.lang.Exception e)
                {
                // re-throw as a runtime exception
                throw new com.tangosol.util.WrapperException(e);
                }
            
            // signal the end of the initialization
            set_Constructed(true);
            }
        
        // Private initializer
        protected void __initPrivate()
            {
            
            super.__initPrivate();
            }
        
        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
        * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService.MemberLeaving();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ClusterService$MemberLeaving".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }
        
        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
        * time] parent component.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent();
            }
        
        // Declared at the super level
        /**
         * Getter for property Description.<p>
        * Used for debugging purposes (from toString). Create a human-readable
        * description of the specific Message data.
         */
        public String getDescription()
            {
            return "MemberId="    + getMemberId()
                 + "\nMemberUid=" + getMemberUid();
            }
        
        // Accessor for the property "MemberId"
        /**
         * Getter for property MemberId.<p>
        * The id of the Member that is leaving the cluster.
         */
        public int getMemberId()
            {
            return __m_MemberId;
            }
        
        // Accessor for the property "MemberUid"
        /**
         * Getter for property MemberUid.<p>
        * The UID of the Member that is leaving the cluster.
         */
        public com.tangosol.util.UUID getMemberUid()
            {
            return __m_MemberUid;
            }
        
        // Declared at the super level
        /**
         * This is the event that is executed when a Message is received.
        * <p>
        * It is the main processing event of the Message called by the
        * <code>Service.onMessage()</code> event. With regards to the use of
        * Message components within clustered Services, Services are designed
        * by dragging Message components into them as static children. These
        * Messages are the components that a Service can send to other running
        * instances of the same Service within a cluster. When the onReceived
        * event is invoked by a Service, it means that the Message has been
        * received; the code in the onReceived event is therefore the Message
        * specific logic for processing a received Message. For example, when
        * onReceived is invoked on a Message named FindData, the onReceived
        * event should do the work to "find the data", because it is being
        * invoked by the Service that received the "find the data" Message.
         */
        public void onReceived()
            {
            // import Component.Net.Member;
            
            super.onReceived();
            
            ClusterService service = (ClusterService) getService();
            Member  member  = service.getClusterMemberSet().getMember(getMemberId());
            
            if (member != null)
                {
                _assert(getMemberUid().equals(member.getUid32()));
                if (!member.isLeaving())
                    {
                    member.setLeaving(true);
                    service.onMemberLeaving(member);
                    }
                }
            }
        
        // Declared at the super level
        public void read(com.tangosol.io.ReadBuffer.BufferInput input)
                throws java.io.IOException
            {
            // import com.tangosol.util.UUID;
            
            setMemberId(input.readUnsignedShort());
            setMemberUid(new UUID(input));
            }
        
        // Accessor for the property "MemberId"
        /**
         * Setter for property MemberId.<p>
        * The id of the Member that is leaving the cluster.
         */
        public void setMemberId(int nId)
            {
            __m_MemberId = nId;
            }
        
        // Accessor for the property "MemberUid"
        /**
         * Setter for property MemberUid.<p>
        * The UID of the Member that is leaving the cluster.
         */
        public void setMemberUid(com.tangosol.util.UUID uid)
            {
            __m_MemberUid = uid;
            }
        
        // Declared at the super level
        public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
                throws java.io.IOException
            {
            output.writeShort(getMemberId());
            getMemberUid().writeExternal(output);
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService$MemberLeft
    
    /**
     * Message:
     *     MemberLeft
     * 
     * History:
     *     Starting with 12.1.2 the witness proctocol logic has been moved to
     * WitenessRequest message.
     * 
     * Purpose:
     *     Notify the cluster of dead/departed Member(s).
     * 
     * Description:
     * MemberLeft notification is used to synchronize the dead member set
     * across members.  It is also oportunistically used by a member leaving
     * the cluster to inform others of its departure.
     * 
     *  Attributes:
     *     SynchronizationRequest
     *     MemberCount
     *     MemberUids
     *     MemberIds
     *     MemberTimestamps (in cluster time)
     * 
     * Response to:
     *     MemberLeft if SynchronizationRequest=true
     * 
     * Expected responses:
     *     MemberLeft if SynchronizationRequest=true
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class MemberLeft
            extends    com.tangosol.coherence.component.net.Message
        {
        // ---- Fields declarations ----
        
        /**
         * Property MemberCount
         *
         * The number of members included in the message.
         */
        private transient int __m_MemberCount;
        
        /**
         * Property MemberIDs
         *
         * An array of IDs for members who have recently left the cluster.
         */
        private short[] __m_MemberIDs;
        
        /**
         * Property MemberTimestamps
         *
         * An array of timestamps (time of death) for members who have recently
         * left the cluster.
         */
        private long[] __m_MemberTimestamps;
        
        /**
         * Property MemberUUIDs
         *
         * An array of UIDs which are known to have left the cluster.
         * 
         * If one of the UIDs references the sending member, then this message
         * serves as a exit notification.
         */
        private com.tangosol.util.UUID[] __m_MemberUUIDs;
        
        /**
         * Property SynchronizationRequest
         *
         * If true then the sending member is requesting that the receiving
         * member(s) send back a MemberLeft containing any departures the
         * sender was unaware of.
         */
        private boolean __m_SynchronizationRequest;
        
        // Default constructor
        public MemberLeft()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public MemberLeft(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);
            
            if (fInit)
                {
                __init();
                }
            }
        
        // Main initializer
        public void __init()
            {
            // private initialization
            __initPrivate();
            
            // state initialization: public and protected properties
            try
                {
                setMessageType(36);
                }
            catch (java.lang.Exception e)
                {
                // re-throw as a runtime exception
                throw new com.tangosol.util.WrapperException(e);
                }
            
            // signal the end of the initialization
            set_Constructed(true);
            }
        
        // Private initializer
        protected void __initPrivate()
            {
            
            super.__initPrivate();
            }
        
        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
        * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService.MemberLeft();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ClusterService$MemberLeft".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }
        
        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
        * time] parent component.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent();
            }
        
        // Accessor for the property "MemberCount"
        /**
         * Getter for property MemberCount.<p>
        * The number of members included in the message.
         */
        public int getMemberCount()
            {
            return __m_MemberCount;
            }
        
        // Accessor for the property "MemberIDs"
        /**
         * Getter for property MemberIDs.<p>
        * An array of IDs for members who have recently left the cluster.
         */
        public short[] getMemberIDs()
            {
            return __m_MemberIDs;
            }
        
        // Accessor for the property "MemberIDs"
        /**
         * Getter for property MemberIDs.<p>
        * An array of IDs for members who have recently left the cluster.
         */
        public short getMemberIDs(int i)
            {
            return getMemberIDs()[i];
            }
        
        // Accessor for the property "MemberTimestamps"
        /**
         * Getter for property MemberTimestamps.<p>
        * An array of timestamps (time of death) for members who have recently
        * left the cluster.
         */
        public long[] getMemberTimestamps()
            {
            return __m_MemberTimestamps;
            }
        
        // Accessor for the property "MemberTimestamps"
        /**
         * Getter for property MemberTimestamps.<p>
        * An array of timestamps (time of death) for members who have recently
        * left the cluster.
         */
        public long getMemberTimestamps(int i)
            {
            return getMemberTimestamps()[i];
            }
        
        // Accessor for the property "MemberUUIDs"
        /**
         * Getter for property MemberUUIDs.<p>
        * An array of UIDs which are known to have left the cluster.
        * 
        * If one of the UIDs references the sending member, then this message
        * serves as a exit notification.
         */
        public com.tangosol.util.UUID[] getMemberUUIDs()
            {
            return __m_MemberUUIDs;
            }
        
        // Accessor for the property "MemberUUIDs"
        /**
         * Getter for property MemberUUIDs.<p>
        * An array of UIDs which are known to have left the cluster.
        * 
        * If one of the UIDs references the sending member, then this message
        * serves as a exit notification.
         */
        public com.tangosol.util.UUID getMemberUUIDs(int i)
            {
            return getMemberUUIDs()[i];
            }
        
        // Accessor for the property "SynchronizationRequest"
        /**
         * Getter for property SynchronizationRequest.<p>
        * If true then the sending member is requesting that the receiving
        * member(s) send back a MemberLeft containing any departures the sender
        * was unaware of.
         */
        public boolean isSynchronizationRequest()
            {
            return __m_SynchronizationRequest;
            }
        
        // Declared at the super level
        /**
         * This is the event that is executed when a Message is received.
        * <p>
        * It is the main processing event of the Message called by the
        * <code>Service.onMessage()</code> event. With regards to the use of
        * Message components within clustered Services, Services are designed
        * by dragging Message components into them as static children. These
        * Messages are the components that a Service can send to other running
        * instances of the same Service within a cluster. When the onReceived
        * event is invoked by a Service, it means that the Message has been
        * received; the code in the onReceived event is therefore the Message
        * specific logic for processing a received Message. For example, when
        * onReceived is invoked on a Message named FindData, the onReceived
        * event should do the work to "find the data", because it is being
        * invoked by the Service that received the "find the data" Message.
         */
        public void onReceived()
            {
            // import Component.Net.Member;
            // import Component.Net.MemberSet;
            // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet.MasterMemberSet;
            // import com.tangosol.util.ImmutableArrayList;
            // import com.tangosol.util.UUID;
            
            ClusterService         service    = (ClusterService) getService();
            Member          memberFrom = getFromMember();
            UUID[]          auuid      = getMemberUUIDs();
            short[]         anid       = getMemberIDs();
            long[]          aldtDeath  = getMemberTimestamps();
            MasterMemberSet setMembers = service.getClusterMemberSet();
            MemberSet       setDead    = setMembers.getRecycleSet();
            
            for (int i = 0, c = getMemberCount(); i < c; ++i)
                {
                Member member = setMembers.getMember(auuid[i]);    
                if (memberFrom == member)
                    {
                    service.removeMember(member);
                    _trace("MemberLeft announcement from " + member, 3);
                    }
                else if (member != null)
                    {    
                    service.removeMember(member);
                    _trace("MemberLeft notification for " + member + " received from " + memberFrom, 3);
                    }
                else if (!setMembers.contains(anid[i])) // ensure the mini-id has not "just" been recycled
                    {
                    member = setDead.getMember(anid[i]);
                    long ldtDeath = aldtDeath[i] - service.getTimestampAdjustment(); // convert to local safe time
                    if (member == null || (member.getTimestamp() < ldtDeath && !member.getUid32().equals(auuid[i])))
                        {        
                        // we don't know about this dead member, this could happen because the member left the
                        // cluster before we joined, or joined and left quickly and we just haven't heard the
                        // join yet
            
                        member = new Member();
                        member.configureDead(anid[i], auuid[i], ldtDeath);
                        member.setLeaving(true); // see ensureMemberLeft
                        setDead.add(member);
                        }
                    }
                // else; out-of-date or duplicate notification
                }
            
            if (isSynchronizationRequest())
                {
                // update the member with our view of who has left to fill any gaps which
                // could have occured while they were joining, for instance if this node
                // detected a death and notified the cluster just before learning about
                // this new member the new member might never learn of the death
                service.ensureMemberLeft(getFromMember(), new ImmutableArrayList(auuid).getSet(), /*fRequest*/ false);
                }
            }
        
        // Declared at the super level
        public void read(com.tangosol.io.ReadBuffer.BufferInput input)
                throws java.io.IOException
            {
            // import com.tangosol.util.UUID;
            
            super.read(input);
            
            setSynchronizationRequest(input.readBoolean());
            
            int     cDead     = input.readInt();
            short[] anID      = new short[cDead];
            long[]  aldtDeath = new long[cDead];
            UUID[]  aUUID     = new UUID[cDead];
            
            for (int i = 0; i < cDead; ++i)
                {
                anID[i]      = input.readShort();
                aldtDeath[i] = input.readLong();    
                (aUUID[i]    = new UUID()).readExternal(input);    
                }
            
            setMemberCount(cDead);
            setMemberIDs(anID);
            setMemberTimestamps(aldtDeath);
            setMemberUUIDs(aUUID);
            }
        
        // Accessor for the property "MemberCount"
        /**
         * Setter for property MemberCount.<p>
        * The number of members included in the message.
         */
        public void setMemberCount(int nCount)
            {
            __m_MemberCount = nCount;
            }
        
        // Accessor for the property "MemberIDs"
        /**
         * Setter for property MemberIDs.<p>
        * An array of IDs for members who have recently left the cluster.
         */
        public void setMemberIDs(short[] anIDs)
            {
            __m_MemberIDs = anIDs;
            }
        
        // Accessor for the property "MemberIDs"
        /**
         * Setter for property MemberIDs.<p>
        * An array of IDs for members who have recently left the cluster.
         */
        public void setMemberIDs(int i, short nID)
            {
            getMemberIDs()[i] = nID;
            }
        
        // Accessor for the property "MemberTimestamps"
        /**
         * Setter for property MemberTimestamps.<p>
        * An array of timestamps (time of death) for members who have recently
        * left the cluster.
         */
        public void setMemberTimestamps(long[] aldt)
            {
            __m_MemberTimestamps = aldt;
            }
        
        // Accessor for the property "MemberTimestamps"
        /**
         * Setter for property MemberTimestamps.<p>
        * An array of timestamps (time of death) for members who have recently
        * left the cluster.
         */
        public void setMemberTimestamps(int i, long ldt)
            {
            getMemberTimestamps()[i] = ldt;
            }
        
        // Accessor for the property "MemberUUIDs"
        /**
         * Setter for property MemberUUIDs.<p>
        * An array of UIDs which are known to have left the cluster.
        * 
        * If one of the UIDs references the sending member, then this message
        * serves as a exit notification.
         */
        public void setMemberUUIDs(com.tangosol.util.UUID[] auuid)
            {
            __m_MemberUUIDs = auuid;
            }
        
        // Accessor for the property "MemberUUIDs"
        /**
         * Setter for property MemberUUIDs.<p>
        * An array of UIDs which are known to have left the cluster.
        * 
        * If one of the UIDs references the sending member, then this message
        * serves as a exit notification.
         */
        public void setMemberUUIDs(int i, com.tangosol.util.UUID uuid)
            {
            getMemberUUIDs()[i] = uuid;
            }
        
        // Accessor for the property "SynchronizationRequest"
        /**
         * Setter for property SynchronizationRequest.<p>
        * If true then the sending member is requesting that the receiving
        * member(s) send back a MemberLeft containing any departures the sender
        * was unaware of.
         */
        public void setSynchronizationRequest(boolean fRequest)
            {
            __m_SynchronizationRequest = fRequest;
            }
        
        // Declared at the super level
        public String toString()
            {
            // import java.util.Arrays;
            
            return "Request=" + isSynchronizationRequest() +
                   ", Members=" + Arrays.toString(getMemberIDs());
            }
        
        // Declared at the super level
        public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
                throws java.io.IOException
            {
            // import com.tangosol.util.UUID;
            
            super.write(output);
            
            output.writeBoolean(isSynchronizationRequest());
            
            int     cDead   = getMemberCount();
            short[] anID    = getMemberIDs();
            long[]  alDeath = getMemberTimestamps();
            UUID[]  aUUID   = getMemberUUIDs();
            
            output.writeInt(cDead);
            for (int i = 0; i < cDead; ++i)
                {
                output.writeShort(anID[i]);
                output.writeLong(alDeath[i]);
                aUUID[i].writeExternal(output);
                }
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService$NewMemberAnnounce
    
    /**
     * DiscoveryMessage (broadcast):
     *     NewMemberAnnounce
     * 
     * Purpose:
     *     Broadcasts the presence of a new Member (sans id)
     * 
     * Description:
     *     This Message is broadcast by a new Member until it receives a
     * NewMemberAnnounceReply from the senior Member of the cluster or a
     * timeout occurs. If the timeout occurs, the new Member assumes that no
     * cluster already exists, and creates a new cluster with itself as the
     * senior Member. To prevent this from happening if a cluster is in
     * transition (e.g. the senior Member has died but has not yet been
     * determined to be dead), a non-senior Member may replay with the
     * NewMemberAnnounceWait, which tells the new Member to reset its timeout.
     * 
     * Attributes:
     *     AttemptCounter
     *     AttemptLimit
     *     ThisSentTimestamp (auto-set by sender on write)
     *     ThisRecvTimestamp (calculated by receiving Member)
     * 
     * Response to:
     *     n/a
     * 
     * Expected responses:
     *     NewMemberAnnounceReply
     *     NewMemberAnnounceWait
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class NewMemberAnnounce
            extends    com.tangosol.coherence.component.net.message.DiscoveryMessage
        {
        // ---- Fields declarations ----
        
        /**
         * Property AnnounceProtocolVersion
         *
         * The 8-bit announce protocol version number.
         */
        private int __m_AnnounceProtocolVersion;
        
        /**
         * Property AttemptCounter
         *
         * This is a number that increases while this Message is being
         * repeatedly sent. When the AttemptLimit is reached without any
         * replies (including "wait" replies), the new Member decides to start
         * its own cluster.
         */
        private int __m_AttemptCounter;
        
        /**
         * Property AttemptLimit
         *
         * The number of times that the new Member will announce itself before
         * giving up and starting its own cluster.
         */
        private int __m_AttemptLimit;
        
        /**
         * Property MESSAGE_TYPE
         *
         */
        public static final int MESSAGE_TYPE = 7;
        
        /**
         * Property ThisRecvTimestamp
         *
         * The timestamp (cluster time) at which this Member received the
         * Packet that was translated into this Message.
         */
        private transient long __m_ThisRecvTimestamp;
        
        /**
         * Property ThisSentTimestamp
         *
         * The date/time (in system time) that the sender sent this Message.
         * (The sender does not set this property; when the Message body is
         * streamed into a Packet, the write() method writes this information
         * directly.)
         */
        private long __m_ThisSentTimestamp;
        
        // Default constructor
        public NewMemberAnnounce()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public NewMemberAnnounce(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);
            
            if (fInit)
                {
                __init();
                }
            }
        
        // Main initializer
        public void __init()
            {
            // private initialization
            __initPrivate();
            
            // state initialization: public and protected properties
            try
                {
                setMessageType(7);
                }
            catch (java.lang.Exception e)
                {
                // re-throw as a runtime exception
                throw new com.tangosol.util.WrapperException(e);
                }
            
            // signal the end of the initialization
            set_Constructed(true);
            }
        
        // Private initializer
        protected void __initPrivate()
            {
            
            super.__initPrivate();
            }
        
        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
        * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService.NewMemberAnnounce();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ClusterService$NewMemberAnnounce".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }
        
        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
        * time] parent component.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent();
            }
        
        // Accessor for the property "AnnounceProtocolVersion"
        /**
         * Getter for property AnnounceProtocolVersion.<p>
        * The 8-bit announce protocol version number.
         */
        public int getAnnounceProtocolVersion()
            {
            return __m_AnnounceProtocolVersion;
            }
        
        // Accessor for the property "AttemptCounter"
        /**
         * Getter for property AttemptCounter.<p>
        * This is a number that increases while this Message is being
        * repeatedly sent. When the AttemptLimit is reached without any replies
        * (including "wait" replies), the new Member decides to start its own
        * cluster.
         */
        public int getAttemptCounter()
            {
            return __m_AttemptCounter;
            }
        
        // Accessor for the property "AttemptLimit"
        /**
         * Getter for property AttemptLimit.<p>
        * The number of times that the new Member will announce itself before
        * giving up and starting its own cluster.
         */
        public int getAttemptLimit()
            {
            return __m_AttemptLimit;
            }
        
        // Declared at the super level
        /**
         * Getter for property Description.<p>
        * Used for debugging purposes (from toString). Create a human-readable
        * description of the specific Message data.
         */
        public String getDescription()
            {
            // import java.util.Date;
            
            long lThisSentTimestamp = getThisSentTimestamp() & 0x00FFFFFFFFFFFFFFL;
            long lThisRecvTimestamp = getThisRecvTimestamp();
            
            String sThisSentTimestamp = (lThisSentTimestamp == 0L ?
                    "none" : new Date(lThisSentTimestamp).toString());
            String sThisRecvTimestamp = (lThisRecvTimestamp == 0L ?
                    "none" : new Date(lThisRecvTimestamp).toString());
            
            return "AttemptCounter="      + getAttemptCounter()
                 + "\nAttemptLimit="      + getAttemptLimit()
                 + "\nThisSentTimestamp=" + sThisSentTimestamp
                 + "\nThisRecvTimestamp=" + sThisRecvTimestamp;
            }
        
        // Accessor for the property "ThisRecvTimestamp"
        /**
         * Getter for property ThisRecvTimestamp.<p>
        * The timestamp (cluster time) at which this Member received the Packet
        * that was translated into this Message.
         */
        public long getThisRecvTimestamp()
            {
            return __m_ThisRecvTimestamp;
            }
        
        // Accessor for the property "ThisSentTimestamp"
        /**
         * Getter for property ThisSentTimestamp.<p>
        * The date/time (in system time) that the sender sent this Message.
        * (The sender does not set this property; when the Message body is
        * streamed into a Packet, the write() method writes this information
        * directly.)
         */
        public long getThisSentTimestamp()
            {
            return __m_ThisSentTimestamp;
            }
        
        // Declared at the super level
        /**
         * This is the event that is executed when a Message is received.
        * <p>
        * It is the main processing event of the Message called by the
        * <code>Service.onMessage()</code> event. With regards to the use of
        * Message components within clustered Services, Services are designed
        * by dragging Message components into them as static children. These
        * Messages are the components that a Service can send to other running
        * instances of the same Service within a cluster. When the onReceived
        * event is invoked by a Service, it means that the Message has been
        * received; the code in the onReceived event is therefore the Message
        * specific logic for processing a received Message. For example, when
        * onReceived is invoked on a Message named FindData, the onReceived
        * event should do the work to "find the data", because it is being
        * invoked by the Service that received the "find the data" Message.
         */
        public void onReceived()
            {
            // import Component.Net.Cluster;
            // import Component.Net.Member;
            // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet.MasterMemberSet;
            // import com.tangosol.net.ClusterDependencies;
            // import com.tangosol.run.component.EventDeathException;
            // import com.tangosol.util.Base;
            // import com.oracle.coherence.common.net.InetAddresses;
            // import java.net.InetAddress;
            // import java.net.InetSocketAddress;
            
            super.onReceived();
            
            ClusterService           service     = (ClusterService) getService();
            Member            memberNew   = (Member) getFromMember();
            InetSocketAddress addrSockSrc = (InetSocketAddress) getSourceAddress();
            InetAddress       addrSrc     = addrSockSrc == null ? null : addrSockSrc.getAddress();
            
            if (getAnnounceProtocolVersion() >= 1                          &&  // introduction version
                service.isWkaEnabled()                                     &&  // for MC addr could be MC or peer's default addr, don't switch
                addrSrc != null && !memberNew.getAddress().equals(addrSrc) &&
                addrSockSrc.getPort() == memberNew.getPort()               &&  // we don't support NAPT, for instance TcpRing *may* be listening on another port
                !InetAddresses.isLocalAddress(addrSrc))                        // a local source is just a legacy port sharing relay, not a NAT
                {
                // this broadcast came from an IP other then what the sending member thought it would, this means
                // that we've got a NAT in play and that the WKA list contains the NAT addresses.  We'll inform
                // that member (which may be us) of this so they may choose to switch to using the NAT address.
            
                // this is enabled by leaving addrSrc/addrSockSrc as non-null
                // that results in any replies sent from this method being sent to both memberNew's advertized address
                // and the source address.  Also in the casse of NewMemberAnnounceReply the actual source address is
                // included in the payload so that the member can decide if it should switch to it.
                }
            else
                {
                // the reply will just go to the IP in memberNew
                addrSockSrc = null;
                addrSrc     = null;
                }
            
            service.setStatsJoinRequests(service.getStatsJoinRequests() + 1L);
            
            switch (service.getState())
                {
                case ClusterService.STATE_ANNOUNCE:
                    {
                    if (isReadError())
                        {
                        service.onMemberRejected(ClusterService.REJECT_VERSION, memberNew);
                        throw new EventDeathException("Version mismatch");
                        }
            
                    // verify that it is not an "own Message"
                    Member memberThis = service.getAnnounceMember();
                    if (memberNew.equals(memberThis))
                        {
                        // we've heard our own broadcast
                        ClusterDependencies config = service.getCluster().getDependencies();
                        if (addrSrc != null && config.getLocalAddress() == null) // WKA NAT
                            {
                            // Note: since we're NATing we can't actualy rebind the socket since we don't
                            // know which of our local IPs is associated with the NAT; so we just change
                            // our advertized address to match the NAT and continue to listen on wildcard
                            Member memberAnnounce = service.instantiateMember();
                            memberAnnounce.configure(config,
                                addrSrc,
                                memberThis.getPort(),
                                memberThis.getTcpRingPort(),
                                new int[]{memberThis.getCpuCount(), memberThis.getSocketCount(), memberThis.getMachineId()});
                            service.setAnnounceMember(memberAnnounce);
            
                            service.resetBroadcastCounter("selecting alternate member address", this);
                            }
                        }
                    else
                        {
                        // another future Member is announcing; if the other
                        // future Member is "older", then reset the cluster
                        // service's attempt counter (to give the other Member
                        // time to start the cluster)
                        // This is only done if the other member has passed the ANNOUNCE_BIND_THRESHOLD which
                        // is an indicator that it has been able to succeffuly bind to the cluster port and
                        // will be able to start a cluster.  This ensures that if a junior announcer happens to
                        // bind to the cluster port it won't reset because of the senior announcer as then
                        // neighter could start the cluster.
                        if (getAttemptCounter() > getAttemptLimit() / ClusterService.ANNOUNCE_BIND_THRESHOLD &&
                            (service.isWellKnown(memberNew.getAddress()) || service.isWellKnown(memberNew) ||
                            (addrSrc != null && service.isWellKnown(addrSrc))) &&
                            memberThis.compareTo(memberNew) > 0)
                            {
                            service.resetBroadcastCounter("an older member announcing", this);
                            }
                        }
                    }
                    break;
            
                case ClusterService.STATE_JOINED:
                    {
                    if (isReadError())
                        {
                        // there is nothing we could do here to stop that member;
                        // it should stop (due to an EOFException) upon receiving
                        // the new version of the SeniorMemberHeartbeat message;
                        throw new EventDeathException("Version mismatch");
                        }
            
                    // a future Member is announcing
                    MasterMemberSet setMember         = service.getClusterMemberSet();
                    Member          memberThis        = setMember.getThisMember();
                    Member          memberSenior      = setMember.getOldestMember();
                    boolean         fJoinSuspended    = service.isMembershipSuspended();
                    boolean         fRecordAnnouncing = fJoinSuspended;
                    if (memberThis == memberSenior && !fJoinSuspended)
                        {
                        if (memberNew.getId() == 0)
                            {
                            // respond to the "announce" with a NewMemberAnnounceReply Message
                            ClusterService.NewMemberAnnounceReply msg = (ClusterService.NewMemberAnnounceReply)
                                    service.instantiateMessage("NewMemberAnnounceReply");
                            msg.setAnnounceProtocolVersion(ClusterService.ANNOUNCE_PROTOCOL_VERSION);        
                            msg.setToMember(memberNew);
            
                            // the PrevSentTimestamp is in the new Member's "localhost" time
                            msg.setPrevSentTimestamp(getThisSentTimestamp());
            
                            // the PrevRecvTimestamp is in "cluster" time
                            msg.setPrevRecvTimestamp(getThisRecvTimestamp());
            
                            // note: ThisSentTimestamp is self-configuring and
                            // is in "cluster" time; see write()
            
                            msg.setExternalAddress(addrSockSrc);
            
                            service.send(msg);
                            }
                        else
                            {
                            // a joined member looking to confirm their membership, respond with SMHB
                            ClusterService.SeniorMemberHeartbeat msg = (ClusterService.SeniorMemberHeartbeat)
                                    service.instantiateMessage("SeniorMemberHeartbeat");
                            msg.setLastReceivedMillis(service.calcTimestamp(memberThis.getLastIncomingMillis()));
                            msg.setMemberSet(setMember);
                            msg.setWkaEnabled(service.isWkaEnabled());
                            msg.setLastJoinTime(setMember.getLastJoinTime());
                            msg.setToMember(memberNew);
                            service.send(msg);
                            }
                        }
                    else if (getAttemptCounter() > (getAttemptLimit() >>> 2) ||
                            !(service.isWellKnown(memberSenior.getAddress()) || service.isWellKnown(memberSenior)))
                        {
                        // tell the new Member to wait because its announce
                        // period is over 1/4 done (don't let it start its own cluster)
                        //  or
                        // if the senior is not well known respond immediately to help the new member
                        // find the senior and join faster.
                        ClusterService.NewMemberAnnounceWait msg = (ClusterService.NewMemberAnnounceWait)
                                service.instantiateMessage("NewMemberAnnounceWait");
                        msg.setToMember(memberNew);
                        msg.setSeniorMember(memberSenior);
                        msg.setExternalAddress(addrSockSrc);
                        service.send(msg);
            
                        // the member might be having difficulty joining because of
                        // a quorum-delayed cluster split involving the senior (but
                        // this member hasn't detected the condition yet).  Record
                        // the announcing member in case we need it to resolve a quorum
                        // decision.
                        fRecordAnnouncing = true;
                        }
            
                    if (fRecordAnnouncing)
                        {
                        service.getQuorumControl().onMemberAnnounceWaiting(memberNew);
                        }
                    }
                    break;
                }
            }
        
        // Declared at the super level
        public void read(com.tangosol.io.ReadBuffer.BufferInput input)
                throws java.io.IOException
            {
            // import Component.Net.Packet;
            // import java.net.InetAddress;
            // import java.net.InetSocketAddress;
            
            super.read(input);
            
            setAttemptCounter(input.readInt());
            setAttemptLimit(input.readInt());
            
            long ldtSentAndVer = input.readLong();
            setAnnounceProtocolVersion((int) (ldtSentAndVer >>> 56));
            
            // Note: we don't strip the version info out of the timestamp, as it is needed when we sent
            // NewMemberAnnounceReply, and our peer has to be ready to strip it from the received NMAR
            // anyway since the reply could have come from a v0 impl which doesn't know about this encoding
            setThisSentTimestamp(ldtSentAndVer);
            
            // record the packet arrival time
            Packet[] aPacket = getPacket();
            if (aPacket != null && aPacket.length > 0)
                {
                Packet packet = aPacket[0];
                if (packet != null)
                    {
                    setThisRecvTimestamp(((ClusterService) getService()).
                        calcTimestamp(packet.getReceivedMillis()));
                    }
                }
            
            // if this is a relayed broadcast the relay may have added the source address and port and the end of the message
            // see Cluster$onForeignPacket
            if (input.available() > 0)
                {
                byte[] abSrc = new byte[input.readInt()];
                input.readFully(abSrc);
            
                setSourceAddress(new InetSocketAddress(InetAddress.getByAddress(abSrc), input.readInt()));
                }
            
            ensureEOS(input); // we can't get rid of this since at least v0 will always have it
            }
        
        // Accessor for the property "AnnounceProtocolVersion"
        /**
         * Setter for property AnnounceProtocolVersion.<p>
        * The 8-bit announce protocol version number.
         */
        public void setAnnounceProtocolVersion(int nVersion)
            {
            if ((nVersion & 0xFFFFFF00) != 0)
                {
                throw new IllegalArgumentException("version exceeds max bit width");
                }
            
            __m_AnnounceProtocolVersion = (nVersion);
            }
        
        // Accessor for the property "AttemptCounter"
        /**
         * Setter for property AttemptCounter.<p>
        * This is a number that increases while this Message is being
        * repeatedly sent. When the AttemptLimit is reached without any replies
        * (including "wait" replies), the new Member decides to start its own
        * cluster.
         */
        public void setAttemptCounter(int n)
            {
            __m_AttemptCounter = n;
            }
        
        // Accessor for the property "AttemptLimit"
        /**
         * Setter for property AttemptLimit.<p>
        * The number of times that the new Member will announce itself before
        * giving up and starting its own cluster.
         */
        public void setAttemptLimit(int c)
            {
            __m_AttemptLimit = c;
            }
        
        // Accessor for the property "ThisRecvTimestamp"
        /**
         * Setter for property ThisRecvTimestamp.<p>
        * The timestamp (cluster time) at which this Member received the Packet
        * that was translated into this Message.
         */
        protected void setThisRecvTimestamp(long ldt)
            {
            __m_ThisRecvTimestamp = ldt;
            }
        
        // Accessor for the property "ThisSentTimestamp"
        /**
         * Setter for property ThisSentTimestamp.<p>
        * The date/time (in system time) that the sender sent this Message.
        * (The sender does not set this property; when the Message body is
        * streamed into a Packet, the write() method writes this information
        * directly.)
         */
        public void setThisSentTimestamp(long cMillis)
            {
            __m_ThisSentTimestamp = cMillis;
            }
        
        // Declared at the super level
        public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
                throws java.io.IOException
            {
            // import com.tangosol.util.Base;
            
            super.write(output);
            
            // in order to support versioning of the announce protocol we stuff a protocol version number
            // into the upper byte of the timestamp.  Prior to the introduction of this feature the
            // recipient only transmitted the timestamp back to us (for clock sync) so it can safely
            // cary any value.  Note since we don't know the version of the target we can't add fields
            // to this message.  This indicator though is sufficient to allow a new recipient to detect
            // our version and respond with additional fields.
            // sent timestamp is "localhost" time
            setThisSentTimestamp(Base.getSafeTimeMillis() | (((long) getAnnounceProtocolVersion()) << 56));
            
            output.writeInt(getAttemptCounter());
            output.writeInt(getAttemptLimit());
            output.writeLong(getThisSentTimestamp());
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService$NewMemberAnnounceReply
    
    /**
     * DiscoveryMessage (broadcast):
     *     NewMemberAnnounceReply
     * 
     * Purpose:
     *     Broadcast to the new Member to synchronize its time with the cluster
     * so that the new Member can request a cluster Member id.
     * 
     * Description:
     *     This Message is broadcast by the senior cluster Member when a new
     * Member has announced its presence. The cluster time information is
     * included so that the new Member can determine the offset of the cluster
     * time from its system time. Subsequently, the new Member will be able to
     * assemble its Member UID using the adjusted cluster time, thus avoiding
     * later quarrels over seniority.
     * 
     * Attributes:
     *     PrevSentTimestamp
     *     PrevRecvTimestamp
     *     ThisSentTimestamp
     *     ThisRecvTimestamp (calculated by recipient)
     *     MaxDeliveryVariance (configurable)
     * 
     * Response to:
     *     NewMemberAnnounce
     * 
     * Expected responses:
     *     NewMemberRequestId
     *     SeniorMemberPanic
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class NewMemberAnnounceReply
            extends    com.tangosol.coherence.component.net.message.DiscoveryMessage
        {
        // ---- Fields declarations ----
        
        /**
         * Property AnnounceProtocolVersion
         *
         * The 8-bit announce protocol version number.
         */
        private int __m_AnnounceProtocolVersion;
        
        /**
         * Property LastTraceMillis
         *
         * The last time a message was logged by this component. This can be
         * used to limit the frequency or to restrict the severity of log
         * messages.
         */
        private static long __s_LastTraceMillis;
        
        /**
         * Property PrevRecvTimestamp
         *
         * The timestamp (cluster time) at which the announce Message from the
         * new Member was received by the senior Member.
         */
        private long __m_PrevRecvTimestamp;
        
        /**
         * Property PrevSentTimestamp
         *
         * The timestamp (local system time) at which the new Member sent the
         * announce Message. This value is copied by the senior Member from the
         * Timestamp property of the NewMemberAnnounce Message.
         */
        private long __m_PrevSentTimestamp;
        
        /**
         * Property ThisRecvTimestamp
         *
         * The timestamp (local system time) at which the new Member received
         * the Packet that was translated into this Message.
         */
        private transient long __m_ThisRecvTimestamp;
        
        /**
         * Property ThisSentTimestamp
         *
         * The timestamp (cluster time) at which this Message was sent by the
         * senior Member to the new Member. (The sender does not set this
         * property; when the Message body is streamed into a Packet, the
         * write() method writes this information directly.)
         */
        private long __m_ThisSentTimestamp;
        
        // Default constructor
        public NewMemberAnnounceReply()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public NewMemberAnnounceReply(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);
            
            if (fInit)
                {
                __init();
                }
            }
        
        // Main initializer
        public void __init()
            {
            // private initialization
            __initPrivate();
            
            // state initialization: public and protected properties
            try
                {
                setMessageType(8);
                }
            catch (java.lang.Exception e)
                {
                // re-throw as a runtime exception
                throw new com.tangosol.util.WrapperException(e);
                }
            
            // signal the end of the initialization
            set_Constructed(true);
            }
        
        // Private initializer
        protected void __initPrivate()
            {
            
            super.__initPrivate();
            }
        
        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
        * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService.NewMemberAnnounceReply();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ClusterService$NewMemberAnnounceReply".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }
        
        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
        * time] parent component.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent();
            }
        
        // Accessor for the property "AnnounceProtocolVersion"
        /**
         * Getter for property AnnounceProtocolVersion.<p>
        * The 8-bit announce protocol version number.
         */
        public int getAnnounceProtocolVersion()
            {
            return __m_AnnounceProtocolVersion;
            }
        
        // Declared at the super level
        /**
         * Getter for property Description.<p>
        * Used for debugging purposes (from toString). Create a human-readable
        * description of the specific Message data.
         */
        public String getDescription()
            {
            // import java.util.Date;
            
            long lPrevSentTimestamp = getPrevSentTimestamp() & 0x00FFFFFFFFFFFFFFL;
            long lPrevRecvTimestamp = getPrevRecvTimestamp();
            long lThisSentTimestamp = getThisSentTimestamp();
            long lThisRecvTimestamp = getThisRecvTimestamp();
            
            String sPrevSentTimestamp = (lPrevSentTimestamp == 0L ?
                    "none" : new Date(lPrevSentTimestamp).toString());
            String sPrevRecvTimestamp = (lPrevRecvTimestamp == 0L ?
                    "none" : new Date(lPrevRecvTimestamp).toString());
            String sThisSentTimestamp = (lThisSentTimestamp == 0L ?
                    "none" : new Date(lThisSentTimestamp).toString());
            String sThisRecvTimestamp = (lThisRecvTimestamp == 0L ?
                    "none" : new Date(lThisRecvTimestamp).toString());
            
            return "ToMember="              + getToMember()
                 + "\nPrevSentTimestamp="   + sPrevSentTimestamp
                 + "\nPrevRecvTimestamp="   + sPrevRecvTimestamp
                 + "\nThisSentTimestamp="   + sThisSentTimestamp
                 + "\nThisRecvTimestamp="   + sThisRecvTimestamp
                 + "\nMaxDeliveryVariance=" + getMaxDeliveryVariance();
            }
        
        // Accessor for the property "LastTraceMillis"
        /**
         * Getter for property LastTraceMillis.<p>
        * The last time a message was logged by this component. This can be
        * used to limit the frequency or to restrict the severity of log
        * messages.
         */
        public static long getLastTraceMillis()
            {
            return __s_LastTraceMillis;
            }
        
        // Accessor for the property "MaxDeliveryVariance"
        /**
         * Getter for property MaxDeliveryVariance.<p>
        * The maximum number of milliseconds that a Packet trip in one
        * direction can vary from the other direction in order for the Packet
        * round trip to be used to determine the cluster time. This value
        * dictates how close the cluster time will be on a new Member when
        * compared to the senior Member.
         */
        protected int getMaxDeliveryVariance()
            {
            ClusterService service = (ClusterService) getService();
            return service.getTimestampMaxVariance();
            }
        
        // Accessor for the property "PrevRecvTimestamp"
        /**
         * Getter for property PrevRecvTimestamp.<p>
        * The timestamp (cluster time) at which the announce Message from the
        * new Member was received by the senior Member.
         */
        public long getPrevRecvTimestamp()
            {
            return __m_PrevRecvTimestamp;
            }
        
        // Accessor for the property "PrevSentTimestamp"
        /**
         * Getter for property PrevSentTimestamp.<p>
        * The timestamp (local system time) at which the new Member sent the
        * announce Message. This value is copied by the senior Member from the
        * Timestamp property of the NewMemberAnnounce Message.
         */
        public long getPrevSentTimestamp()
            {
            return __m_PrevSentTimestamp;
            }
        
        // Accessor for the property "ThisRecvTimestamp"
        /**
         * Getter for property ThisRecvTimestamp.<p>
        * The timestamp (local system time) at which the new Member received
        * the Packet that was translated into this Message.
         */
        protected long getThisRecvTimestamp()
            {
            return __m_ThisRecvTimestamp;
            }
        
        // Accessor for the property "ThisSentTimestamp"
        /**
         * Getter for property ThisSentTimestamp.<p>
        * The timestamp (cluster time) at which this Message was sent by the
        * senior Member to the new Member. (The sender does not set this
        * property; when the Message body is streamed into a Packet, the
        * write() method writes this information directly.)
         */
        public long getThisSentTimestamp()
            {
            return __m_ThisSentTimestamp;
            }
        
        // Declared at the super level
        /**
         * This is the event that is executed when a Message is received.
        * <p>
        * It is the main processing event of the Message called by the
        * <code>Service.onMessage()</code> event. With regards to the use of
        * Message components within clustered Services, Services are designed
        * by dragging Message components into them as static children. These
        * Messages are the components that a Service can send to other running
        * instances of the same Service within a cluster. When the onReceived
        * event is invoked by a Service, it means that the Message has been
        * received; the code in the onReceived event is therefore the Message
        * specific logic for processing a received Message. For example, when
        * onReceived is invoked on a Message named FindData, the onReceived
        * event should do the work to "find the data", because it is being
        * invoked by the Service that received the "find the data" Message.
         */
        public void onReceived()
            {
            // import Component.Net.Cluster;
            // import Component.Net.Member;
            // import Component.Net.Socket.UdpSocket.UnicastUdpSocket;
            // import com.tangosol.net.ClusterDependencies;
            // import com.tangosol.net.internal.ClusterJoinException;
            // import com.tangosol.run.component.EventDeathException;
            // import com.tangosol.util.Base;
            // import com.oracle.coherence.common.net.InetAddresses;
            // import java.io.IOException;
            // import java.net.InetAddress;
            // import java.net.InetSocketAddress;
            
            super.onReceived();
            
            ClusterService service    = (ClusterService) getService();
            Member  memberFrom = getFromMember();
            Member  memberTo   = getToMember();
            switch (service.getState())
                {
                case ClusterService.STATE_ANNOUNCE:
                    if (isReadError())
                        {
                        // version mismatch; stop this node
                        service.onMemberRejected(ClusterService.REJECT_VERSION, memberFrom);
                        throw new EventDeathException("Version mismatch");
                        }
                    
                    // verify Message sent to this Member
                    Member memberAnnounce = service.getAnnounceMember();
                    if (memberTo.equals(memberAnnounce))
                        {
                        // someone is sending back announce replies; that means a
                        // cluster is running
                        service.resetBroadcastCounter(/*sReason*/ null, this);
            
                        Cluster             cluster = (Cluster) service.getCluster();
                        ClusterDependencies config  = cluster.getDependencies();
            
                        // validate the senior host IP address
                        if (!cluster.isAuthorizedHost(memberFrom.getAddress()))
                            {
                            // unauthorized senior IP address; stop this node
                            service.onMemberRejected(ClusterService.REJECT_SENIOR, memberFrom);
                            throw new EventDeathException("Unauthorized senior");
                            }
            
                        UnicastUdpSocket socket = cluster.getSocketManager().getUnicastUdpSocket();
                        if (socket.getInetAddress().isAnyLocalAddress())
                            {
                            // we should now have enough information to select a specific local address; this is
                            // important as we need to ensure that when we send packets they come from our member
                            // address otherwise others will drop them.
            
                            InetSocketAddress addrSockExt  = getExternalAddress();
                            InetAddress       addrExt      = addrSockExt == null ? null : addrSockExt.getAddress();
                            InetAddress       addrAnnounce = memberAnnounce.getAddress();
                            InetAddress       addrRebind   = addrExt == null
                                ? addrAnnounce // may be NAT from prior announce pass or config
                                : addrExt;     // may be learned NAT
            
                            if (InetAddresses.isLocalAddress(addrRebind))
                                {
                                try
                                    {
                                    socket.rebind(addrRebind);
                                    }
                                catch (IOException e)
                                    {
                                    // apparently some other process managed to grab the binding between us closing the
                                    // old socket and attempting to bind the new one, we need to restart the join protocol
            
                                    ClusterJoinException eJoin = new ClusterJoinException();
                                    eJoin.initCause(e);
                                    throw eJoin;
                                    }
                                }
                            // else; nat local; stick with wildcard binding
            
                            if (service.isWkaEnabled() &&
                                addrExt != null &&
                                !addrExt.equals(addrAnnounce) &&
                                config.getLocalAddress() == null)
                                {
                                // our announce packet reached the senior via a route other then what we'd expected, this
                                // could mean NAT, or it could mean that we auto-selected the wrong local NIC.  In either
                                // case we should switch NICs so long as the NIC was auto-selected
            
                                Member memberAnnounceNew = service.instantiateMember();
                                memberAnnounceNew.configure(config,
                                    addrExt,
                                    memberAnnounce.getPort(),
                                    memberAnnounce.getTcpRingPort(),
                                    new int[]{memberAnnounce.getCpuCount(), memberAnnounce.getSocketCount(), memberAnnounce.getMachineId()});
            
                                memberAnnounce = memberAnnounceNew;
                                service.setAnnounceMember(memberAnnounce);
                                }
                            }
            
                        // validate the senior's address with TcpRing
                        ClusterService.TcpRing ring = service.getTcpRing();
                        if (ring.isEnabled() && !ring.verifyReachable(memberFrom, 1000))
                            {
                            // TcpRing is enabled, but this member can't connect to the
                            // senior over TCP; continue announcing (in hopes the failure
                            // is transient), but don't continue with the join protocol as
                            // allowing this member to join would likely lead to it
                            // immediately killing off the other members                    
                            service.resetBroadcastCounter("TcpRing failed to connect to" +
                                " senior " + memberFrom + "; if this persists it is" +
                                " likely the result of a local or remote firewall rule" +
                                " blocking connections to TCP-ring port " +
                                memberFrom.getTcpRingPort(), null);
                            return;
                            }
            
                        // validate the senior's address with IpMonitor
                        //
                        // Note: regrettably, we need to do a separate check with IpMonitor
                        //       here, as firewall port rules could block the ports used
                        //       by IpMonitor.  Also, specification allows for an ICMP-based
                        //       implementation, though in practice most implementations
                        //       will fall back to using TCP.
                        // Note: We don't perform this check if the senior is on this machine/IP.
                        //       A failure there would suggest that a multi-machine cluster will
                        //       not function, but this check will most often fail on Developers
                        //       boxes, where a single machine cluster may be all that is desired.
                        //       While we could check and then warn, the failed check would take
                        //       a rather long time, so instead we just delay the issue until they
                        //       add a second machine.  Note that IPMon will not ping it's local
                        //       machine anyhow, which makes avoiding this check both possible and
                        //       reasonable.
                        if (config.isIpMonitorEnabled() &&
                            !memberFrom.isCollocated(memberTo) &&
                            !cluster.getIpMonitor().verifyReachable(memberFrom))
                            {
                            // IpMonitor is enabled, but this member sees the senior's
                            // InetAddress as unreachable; stop this node immediately as
                            // allowing it to join will likely lead to it immediately killing
                            // off the other members
                            service.resetBroadcastCounter("IpMonitor failed to verify " +
                                "the reachability of senior " + memberFrom + " " +
                                " via " + memberTo.getAddress() + "; if this persists " +
                                "it is likely the result of a local or remote " +
                                "firewall rule blocking either ICMP pings, or " +
                                "connections to TCP port 7", null);
                            return;
                            }
                        
                        // get the send/receive times for the ClusterService.NewMemberAnnounce
                        // Message and the $MemberTimeAdjust Message
                        long cPrevSentMillis = getPrevSentTimestamp();
                        long cPrevRecvMillis = getPrevRecvTimestamp();
                        long cThisSentMillis = getThisSentTimestamp();
                        long cThisRecvMillis = getThisRecvTimestamp();
            
                        // calculate the difference between the oldest Member's
                        // time clock and this system's clock
                        long cMillisGoing  = cPrevRecvMillis - cPrevSentMillis;
                        long cMillisComing = cThisSentMillis - cThisRecvMillis;
                        long cMillisMaxVar = getMaxDeliveryVariance();
            
                        // calculate the round-trip time for this packet
                        long cMillisVar = Math.abs(cMillisGoing - cMillisComing);
                        
                        if (cMillisVar <= cMillisMaxVar)
                            {
                            // time within variance;
                            // determine the difference in the clock between this Member
                            // and the oldest Member (that sent the MemberTimeAdjust Message)
                            long cMillisDif = (cMillisGoing + cMillisComing) / 2;
            
                            // store the clock dif
                            service.setTimestampAdjustment(cMillisDif);
            
                            // create the ThisMember and set up the Service to request
                            // a Member id
                            Member memberThis = service.instantiateMember();                
                            memberThis.configure(memberAnnounce, cThisSentMillis); // this member's timestamp is the time at which the senior replied
                            service.setRequestMember(memberThis);
                            service.setState(ClusterService.STATE_JOINING);
                            }
                        else
                            {
                            long lMillis = Base.getSafeTimeMillis();
                            int  nLevel  = 5;
            
                            if ((lMillis - getLastTraceMillis()) >= service.getBroadcastTimeoutMillis())
                                {
                                setLastTraceMillis(lMillis);
                                nLevel = 3;
                                }
                            
                            _trace("Failed to satisfy the variance: allowed=" + cMillisMaxVar +
                                ", actual=" + (cMillisGoing - cMillisComing), nLevel);
            
                            // slowly increase allowable variance so that we can join
                            int cNewVariance = (int) Math.min(
                                cMillisMaxVar + Math.max(((cMillisVar - cMillisMaxVar ) / 8), 1)
                                , 1000);
                            if (cNewVariance != cMillisMaxVar)
                                {
                                service.setTimestampMaxVariance(cNewVariance);
                                _trace("Increasing allowable variance to " + cNewVariance, nLevel);
                                }
                            }
                        }
                    else
                        {
                        // someone is sending back announce replies; that means a
                        // cluster is running
                        service.resetBroadcastCounter("the presence of an existing cluster", this);
                        }
                    break;
            
                case ClusterService.STATE_JOINING:
                    if (isReadError())
                        {
                        // version mismatch; stop this node
                        service.onMemberRejected(ClusterService.REJECT_VERSION, memberFrom);
                        throw new EventDeathException("Version mismatch");
                        }
            
                    // someone is sending back announce replies; that means a
                    // cluster is running
                    service.resetBroadcastCounter("the presence of an existing cluster", this);
                    break;
            
                case ClusterService.STATE_JOINED:
                    service.validateSeniorBroadcast(this, null);
                    break;
                }
            }
        
        // Declared at the super level
        public void read(com.tangosol.io.ReadBuffer.BufferInput input)
                throws java.io.IOException
            {
            // import Component.Net.Packet;
            // import java.net.InetAddress;
            // import java.net.InetSocketAddress;
            
            super.read(input);
            
            setPrevSentTimestamp(input.readLong() & 0x00FFFFFFFFFFFFFFL); // strip out our ver info which is always echo'd by the peer
            setPrevRecvTimestamp(input.readLong());
            setThisSentTimestamp(input.readLong());
            getFromMember().setTcpRingPort(input.readInt());
            
            // record the packet arrival time
            Packet[] aPacket = getPacket();
            if (aPacket != null && aPacket.length > 0)
                {
                Packet packet = aPacket[0];
                if (packet != null)
                    {
                    setThisRecvTimestamp(((ClusterService) getService()).
                        calcTimestamp(packet.getReceivedMillis()));
                    }
                }
            
            if (input.available() > 0)
                {
                int nProtoVerPeer = input.read();
                setAnnounceProtocolVersion(nProtoVerPeer);
            
                if (nProtoVerPeer >= 1) // always true if input.available > 0
                    {
                    int cbAddr = input.readInt();
                    if (cbAddr > 0)
                        {
                        byte[] abAddr = new byte[cbAddr];
                        input.read(abAddr);
                        int nPort = input.readInt();
                        setExternalAddress(new InetSocketAddress(InetAddress.getByAddress(abAddr), nPort));
                        }
                    // else; no addr or port included
                    }
                }
            // else v0
            
            ensureEOS(input); // we can't get rid of this since at least v0 will always have it
            }
        
        // Accessor for the property "AnnounceProtocolVersion"
        /**
         * Setter for property AnnounceProtocolVersion.<p>
        * The 8-bit announce protocol version number.
         */
        public void setAnnounceProtocolVersion(int nVersion)
            {
            if ((nVersion & 0xFFFFFF00) != 0)
                {
                throw new IllegalArgumentException("version exceeds max bit width");
                }
            
            __m_AnnounceProtocolVersion = (nVersion);
            }
        
        // Accessor for the property "LastTraceMillis"
        /**
         * Setter for property LastTraceMillis.<p>
        * The last time a message was logged by this component. This can be
        * used to limit the frequency or to restrict the severity of log
        * messages.
         */
        protected static void setLastTraceMillis(long lMillis)
            {
            __s_LastTraceMillis = lMillis;
            }
        
        // Accessor for the property "PrevRecvTimestamp"
        /**
         * Setter for property PrevRecvTimestamp.<p>
        * The timestamp (cluster time) at which the announce Message from the
        * new Member was received by the senior Member.
         */
        public void setPrevRecvTimestamp(long cMillis)
            {
            __m_PrevRecvTimestamp = cMillis;
            }
        
        // Accessor for the property "PrevSentTimestamp"
        /**
         * Setter for property PrevSentTimestamp.<p>
        * The timestamp (local system time) at which the new Member sent the
        * announce Message. This value is copied by the senior Member from the
        * Timestamp property of the NewMemberAnnounce Message.
         */
        public void setPrevSentTimestamp(long cMillis)
            {
            __m_PrevSentTimestamp = cMillis;
            }
        
        // Accessor for the property "ThisRecvTimestamp"
        /**
         * Setter for property ThisRecvTimestamp.<p>
        * The timestamp (local system time) at which the new Member received
        * the Packet that was translated into this Message.
         */
        protected void setThisRecvTimestamp(long ldt)
            {
            __m_ThisRecvTimestamp = ldt;
            }
        
        // Accessor for the property "ThisSentTimestamp"
        /**
         * Setter for property ThisSentTimestamp.<p>
        * The timestamp (cluster time) at which this Message was sent by the
        * senior Member to the new Member. (The sender does not set this
        * property; when the Message body is streamed into a Packet, the
        * write() method writes this information directly.)
         */
        public void setThisSentTimestamp(long cMillis)
            {
            __m_ThisSentTimestamp = cMillis;
            }
        
        // Declared at the super level
        public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
                throws java.io.IOException
            {
            // import java.net.InetAddress;
            // import java.net.InetSocketAddress;
            
            super.write(output);
            
            // sent timestamp is "cluster" time
            long lMillis = ((ClusterService) getService()).getTimestamp();
            setThisSentTimestamp(lMillis);
            
            long ldtPrevSent = getPrevSentTimestamp();
            output.writeLong(ldtPrevSent);
            output.writeLong(getPrevRecvTimestamp());
            output.writeLong(getThisSentTimestamp());
            output.writeInt(getFromMember().getTcpRingPort());
            
            // ldtPrevSent is the value which the sender of the corresponding NewMemberAnnounce and its
            // upper byte contains the senders announce protocol version; see NewMemberAnnounce#read/write
            int nProtoVerPeer = (int) (ldtPrevSent >>> 56);
            
            // send back via the min protocol supported by us and peer
            // the peer will invoke ensureEOS, so we are not allowed to expand the message
            // beyond what their version supports
            
            if (nProtoVerPeer >= 1)
                {
                output.write(getAnnounceProtocolVersion());
                
                InetSocketAddress addrExternal = getExternalAddress();
                if (addrExternal == null)
                    {
                    output.writeInt(0);
                    }
                else
                    {
                    // in addition to sending to this address we also want to include the address
                    // in the payload as NATing will strip the address from the IP headers
                    InetAddress addr   = addrExternal.getAddress();
                    byte[]      abAddr = addr.getAddress();
                    output.writeInt(abAddr.length);
                    output.write(abAddr);
                    output.writeInt(addrExternal.getPort());
                    }
                }
            // else; we can't event send our version if they are v0
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService$NewMemberAnnounceWait
    
    /**
     * DiscoveryMessage (broadcast):
     *     NewMemberAnnounceWait
     * 
     * Purpose:
     *     Broadcasts to the new Member to keep announcing because there is
     * definitely a cluster already in existence
     * 
     * Description:
     *     This Message is broadcast by any non-senior cluster Member when a
     * new Member has passed a certain threshold in its announcement (its
     * repeated NewMemberAnnounce Messages). The non-senior cluster Member
     * determines that the new Member is getting close to starting its own
     * cluster because it has not yet received a reply from the senior Member,
     * so the non-senior cluster Member tells the new Member to wait.
     * 
     * Attributes:
     *     SeniorMember
     * Response to:
     *     NewMemberAnnounce
     * 
     * Expected responses:
     *     n/a
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class NewMemberAnnounceWait
            extends    com.tangosol.coherence.component.net.message.DiscoveryMessage
        {
        // ---- Fields declarations ----
        
        /**
         * Property SeniorMember
         *
         * The senior Member that should respond to the announcing member.
         */
        private com.tangosol.coherence.component.net.Member __m_SeniorMember;
        
        // Default constructor
        public NewMemberAnnounceWait()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public NewMemberAnnounceWait(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);
            
            if (fInit)
                {
                __init();
                }
            }
        
        // Main initializer
        public void __init()
            {
            // private initialization
            __initPrivate();
            
            // state initialization: public and protected properties
            try
                {
                setMessageType(9);
                }
            catch (java.lang.Exception e)
                {
                // re-throw as a runtime exception
                throw new com.tangosol.util.WrapperException(e);
                }
            
            // signal the end of the initialization
            set_Constructed(true);
            }
        
        // Private initializer
        protected void __initPrivate()
            {
            
            super.__initPrivate();
            }
        
        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
        * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService.NewMemberAnnounceWait();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ClusterService$NewMemberAnnounceWait".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }
        
        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
        * time] parent component.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent();
            }
        
        // Declared at the super level
        /**
         * Getter for property Description.<p>
        * Used for debugging purposes (from toString). Create a human-readable
        * description of the specific Message data.
         */
        public String getDescription()
            {
            return "ToMember="       + getToMember()
                 + "\nSeniorMember=" + getSeniorMember();
            }
        
        // Accessor for the property "SeniorMember"
        /**
         * Getter for property SeniorMember.<p>
        * The senior Member that should respond to the announcing member.
         */
        public com.tangosol.coherence.component.net.Member getSeniorMember()
            {
            return __m_SeniorMember;
            }
        
        // Declared at the super level
        /**
         * This is the event that is executed when a Message is received.
        * <p>
        * It is the main processing event of the Message called by the
        * <code>Service.onMessage()</code> event. With regards to the use of
        * Message components within clustered Services, Services are designed
        * by dragging Message components into them as static children. These
        * Messages are the components that a Service can send to other running
        * instances of the same Service within a cluster. When the onReceived
        * event is invoked by a Service, it means that the Message has been
        * received; the code in the onReceived event is therefore the Message
        * specific logic for processing a received Message. For example, when
        * onReceived is invoked on a Message named FindData, the onReceived
        * event should do the work to "find the data", because it is being
        * invoked by the Service that received the "find the data" Message.
         */
        public void onReceived()
            {
            // import Component.Net.Member;
            // import com.tangosol.run.component.EventDeathException;
            
            super.onReceived();
            
            ClusterService service    = (ClusterService) getService();
            Member  memberFrom = getFromMember();
            switch (service.getState())
                {
                case ClusterService.STATE_ANNOUNCE:
                case ClusterService.STATE_JOINING:
                    {
                    if (isReadError())
                        {
                        // version mismatch; stop this node
                        service.onMemberRejected(ClusterService.REJECT_VERSION, memberFrom);
                        throw new EventDeathException("Version mismatch");
                        }
            
                    // someone is sending back announce replies; that means a
                    // cluster is running
                    service.resetBroadcastCounter("the presence of an existing cluster", this);
                    break;
                    }
                }
            
            // if the senior is not a WKA, add it to the WKA list; this is done
            // even post-join see TcpRing#onIsolation
            service.addDynamicBroadcast(getSeniorMember());
            }
        
        // Declared at the super level
        public void read(com.tangosol.io.ReadBuffer.BufferInput input)
                throws java.io.IOException
            {
            // import Component.Net.Member;
            
            super.read(input);
            
            ClusterService service      = (ClusterService) getService();
            Member  memberSenior = service.instantiateMember();
            memberSenior.readExternal(input);
            setSeniorMember(memberSenior);
            
            ensureEOS(input);
            }
        
        // Accessor for the property "SeniorMember"
        /**
         * Setter for property SeniorMember.<p>
        * The senior Member that should respond to the announcing member.
         */
        public void setSeniorMember(com.tangosol.coherence.component.net.Member member)
            {
            __m_SeniorMember = member;
            }
        
        // Declared at the super level
        public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
                throws java.io.IOException
            {
            super.write(output);
            
            getSeniorMember().writeExternal(output);
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService$NewMemberInduct
    
    /**
     * Message:
     *     NewMemberInduct
     * 
     * Purpose:
     *     Sends synchronization data (member and service lists)
     * 
     * Description:
     *     Response from the senior Member to the new Member to provide the new
     * Member with all of the information it needs about Members and Services.
     * 
     * Attributes:
     *     MemberCount
     *     MemberId[]
     *     MemberUid[] (corresponding to MemberId[])
     *     MemberCpu[] (corresponding to MemberId[])
     *     ServiceVersion[] (corresponding to MemberId[])
     *     ServiceCount
     *     ServiceId[]
     *     ServiceName[]
     *     ServiceType[]
     * 
     * Response to:
     *     NewMemberRequestId
     * 
     * Expected responses:
     *     NewMemberWelcomeRequest
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class NewMemberInduct
            extends    com.tangosol.coherence.component.net.Message
        {
        // ---- Fields declarations ----
        
        /**
         * Property Member
         *
         * The cluster Members (indexed 0..MemberCount-1).
         */
        private com.tangosol.coherence.component.net.Member[] __m_Member;
        
        /**
         * Property MemberCount
         *
         * The number of Member id/UID combinations that are included in this
         * Message.
         */
        private transient int __m_MemberCount;
        
        /**
         * Property ServiceCount
         *
         * The number of Service id/name combinations that are included in this
         * Message.
         */
        private transient int __m_ServiceCount;
        
        /**
         * Property ServiceId
         *
         * The Service ids of the known cluster Services (indexed
         * 0..ServiceCount-1).
         */
        private int[] __m_ServiceId;
        
        /**
         * Property ServiceName
         *
         * The names of the known cluster Services (indexed 0..ServiceCount-1).
         */
        private String[] __m_ServiceName;
        
        /**
         * Property ServiceSuspended
         *
         * The suspend state of the known cluster Services (indexed
         * 0..ServiceCount-1).
         */
        private boolean[] __m_ServiceSuspended;
        
        /**
         * Property ServiceType
         *
         * The types of the known cluster Services (indexed 0..ServiceCount-1).
         */
        private String[] __m_ServiceType;
        
        /**
         * Property ServiceVersion
         *
         * The version of ClusterService that each Member is running (indexed
         * 0..MemberCount-1).
         */
        private String[] __m_ServiceVersion;
        
        // Default constructor
        public NewMemberInduct()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public NewMemberInduct(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);
            
            if (fInit)
                {
                __init();
                }
            }
        
        // Main initializer
        public void __init()
            {
            // private initialization
            __initPrivate();
            
            // state initialization: public and protected properties
            try
                {
                setMessageType(6);
                }
            catch (java.lang.Exception e)
                {
                // re-throw as a runtime exception
                throw new com.tangosol.util.WrapperException(e);
                }
            
            // signal the end of the initialization
            set_Constructed(true);
            }
        
        // Private initializer
        protected void __initPrivate()
            {
            
            super.__initPrivate();
            }
        
        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
        * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService.NewMemberInduct();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ClusterService$NewMemberInduct".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }
        
        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
        * time] parent component.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent();
            }
        
        // Declared at the super level
        /**
         * Getter for property Description.<p>
        * Used for debugging purposes (from toString). Create a human-readable
        * description of the specific Message data.
         */
        public String getDescription()
            {
            StringBuffer sb = new StringBuffer();
            
            int cMembers  = getMemberCount();
            int cServices = getServiceCount();
            
            sb.append("MemberCount=")
              .append(cMembers)
              .append("\nMember/ServiceVersion=[");
              
            for (int i = 0; i < cMembers; ++i)
                {
                if (i > 0)
                    {
                    sb.append(", ");
                    }
                sb.append(getMember(i))
                  .append('/')
                  .append(getServiceVersion(i));
                }
            
            sb.append("]\nServiceCount=")
              .append(cServices)
              .append("\nServiceId/ServiceName=[");
            
            for (int i = 0; i < cServices; ++i)
                {
                if (i > 0)
                    {
                    sb.append(", ");
                    }
                sb.append(getServiceId(i))
                  .append('/')
                  .append(getServiceName(i));
                }
            
            sb.append(']');
            
            return sb.toString();
            }
        
        // Accessor for the property "Member"
        /**
         * Getter for property Member.<p>
        * The cluster Members (indexed 0..MemberCount-1).
         */
        public com.tangosol.coherence.component.net.Member[] getMember()
            {
            return __m_Member;
            }
        
        // Accessor for the property "Member"
        /**
         * Getter for property Member.<p>
        * The cluster Members (indexed 0..MemberCount-1).
         */
        public com.tangosol.coherence.component.net.Member getMember(int i)
            {
            return getMember()[i];
            }
        
        // Accessor for the property "MemberCount"
        /**
         * Getter for property MemberCount.<p>
        * The number of Member id/UID combinations that are included in this
        * Message.
         */
        public int getMemberCount()
            {
            Object[] ao = getMember();
            return ao == null ? 0 : ao.length;
            }
        
        // Accessor for the property "ServiceCount"
        /**
         * Getter for property ServiceCount.<p>
        * The number of Service id/name combinations that are included in this
        * Message.
         */
        public int getServiceCount()
            {
            int[] anId = getServiceId();
            return anId == null ? 0 : anId.length;
            }
        
        // Accessor for the property "ServiceId"
        /**
         * Getter for property ServiceId.<p>
        * The Service ids of the known cluster Services (indexed
        * 0..ServiceCount-1).
         */
        public int[] getServiceId()
            {
            return __m_ServiceId;
            }
        
        // Accessor for the property "ServiceId"
        /**
         * Getter for property ServiceId.<p>
        * The Service ids of the known cluster Services (indexed
        * 0..ServiceCount-1).
         */
        public int getServiceId(int i)
            {
            return getServiceId()[i];
            }
        
        // Accessor for the property "ServiceName"
        /**
         * Getter for property ServiceName.<p>
        * The names of the known cluster Services (indexed 0..ServiceCount-1).
         */
        public String[] getServiceName()
            {
            return __m_ServiceName;
            }
        
        // Accessor for the property "ServiceName"
        /**
         * Getter for property ServiceName.<p>
        * The names of the known cluster Services (indexed 0..ServiceCount-1).
         */
        public String getServiceName(int i)
            {
            return getServiceName()[i];
            }
        
        // Accessor for the property "ServiceType"
        /**
         * Getter for property ServiceType.<p>
        * The types of the known cluster Services (indexed 0..ServiceCount-1).
         */
        public String[] getServiceType()
            {
            return __m_ServiceType;
            }
        
        // Accessor for the property "ServiceType"
        /**
         * Getter for property ServiceType.<p>
        * The types of the known cluster Services (indexed 0..ServiceCount-1).
         */
        public String getServiceType(int i)
            {
            return getServiceType()[i];
            }
        
        // Accessor for the property "ServiceVersion"
        /**
         * Getter for property ServiceVersion.<p>
        * The version of ClusterService that each Member is running (indexed
        * 0..MemberCount-1).
         */
        public String[] getServiceVersion()
            {
            return __m_ServiceVersion;
            }
        
        // Accessor for the property "ServiceVersion"
        /**
         * Getter for property ServiceVersion.<p>
        * The version of ClusterService that each Member is running (indexed
        * 0..MemberCount-1).
         */
        public String getServiceVersion(int i)
            {
            return getServiceVersion()[i];
            }
        
        // Accessor for the property "ServiceSuspended"
        /**
         * Getter for property ServiceSuspended.<p>
        * The suspend state of the known cluster Services (indexed
        * 0..ServiceCount-1).
         */
        public boolean[] isServiceSuspended()
            {
            return __m_ServiceSuspended;
            }
        
        // Accessor for the property "ServiceSuspended"
        /**
         * Getter for property ServiceSuspended.<p>
        * The suspend state of the known cluster Services (indexed
        * 0..ServiceCount-1).
         */
        public boolean isServiceSuspended(int i)
            {
            return isServiceSuspended()[i];
            }
        
        // Declared at the super level
        /**
         * This is the event that is executed when a Message is received.
        * <p>
        * It is the main processing event of the Message called by the
        * <code>Service.onMessage()</code> event. With regards to the use of
        * Message components within clustered Services, Services are designed
        * by dragging Message components into them as static children. These
        * Messages are the components that a Service can send to other running
        * instances of the same Service within a cluster. When the onReceived
        * event is invoked by a Service, it means that the Message has been
        * received; the code in the onReceived event is therefore the Message
        * specific logic for processing a received Message. For example, when
        * onReceived is invoked on a Message named FindData, the onReceived
        * event should do the work to "find the data", because it is being
        * invoked by the Service that received the "find the data" Message.
         */
        public void onReceived()
            {
            // import Component.Net.Member;
            // import com.tangosol.util.Base;
            // import com.tangosol.run.component.EventDeathException;
            
            super.onReceived();
            
            ClusterService service      = (ClusterService) getService();
            String  sVersionThis = service.getServiceVersion();
            
            int cMembers = getMemberCount();
            if (cMembers > 0)
                {
                Member[] aMember   = getMember();
                String[] asVersion = getServiceVersion();
            
                for (int i = 0; i < cMembers; ++i)
                    {
                    Member member   = aMember[i];
                    String sVersion = asVersion[i];
                    if (!service.isVersionCompatible(sVersion))
                        {
                        // no further conversation is possible since the
                        // versions are different; stop the cluster
                        service.onMemberRejected(ClusterService.REJECT_VERSION, getFromMember());
                        throw new EventDeathException("Version mismatch");
                        }
                    service.ensureMember(member, sVersion);
                    }
                }
            
            int cServices = getServiceCount();
            if (cServices > 0)
                {
                int[]     anId        = getServiceId();
                String[]  asName      = getServiceName();
                String[]  asType      = getServiceType();
                boolean[] afSuspended = isServiceSuspended();
                for (int i = 0; i < cServices; ++i)
                    {
                    service.ensureServiceInfo(anId[i], asName[i], asType[i])
                        .setSuspended(afSuspended[i]);
                    }
                }
            
            // COH-4387: only now, after other cluster members are known, transition to JOINED
            service.setState(ClusterService.STATE_JOINED);
            
            // we now have sufficient information to join in the ring
            service.getTcpRing().onJoined();
            
            // synchronize dead member list with all members
            service.ensureMemberLeft(/*setTo*/ null, /*setUUIDExempt*/ null, /*fRequest*/ true);
            
            // mark this Member as joined in the MasterMemberSet
            Member memberThis = service.getThisMember();
            service.getClusterMemberSet().setServiceJoined(memberThis.getId());
            
            // request each Member to welcome this new Member
            ClusterService.NewMemberWelcomeRequest msg = (ClusterService.NewMemberWelcomeRequest)
                    service.instantiateMessage("NewMemberWelcomeRequest");
            msg.setToMemberSet(service.getOthersMemberSet());
            msg.setPreferredPacketLength(memberThis.getPreferredPacketLength());
            msg.setPreferredPort(memberThis.getPreferredPort());
            msg.setServiceVersion(sVersionThis);
            
            service.send(msg);
            }
        
        // Declared at the super level
        public void read(com.tangosol.io.ReadBuffer.BufferInput input)
                throws java.io.IOException
            {
            // import Component.Net.Member;
            // import com.tangosol.util.ExternalizableHelper as com.tangosol.util.ExternalizableHelper;
            
            ClusterService service = (ClusterService) getService();
            
            int cMembers = input.readUnsignedShort();
            if (cMembers > 0)
                {
                Member[] aMember   = new Member[cMembers];
                String[] asVersion = new String[cMembers];
                for (int i = 0; i < cMembers; ++i)
                    {
                    Member member = service.instantiateMember();
                    member.readExternal(input);
                    member.setTcpRingPort(com.tangosol.util.ExternalizableHelper.readInt(input));
                    aMember  [i] = member;
                    asVersion[i] = com.tangosol.util.ExternalizableHelper.readUTF(input);
                    }
                setMember(aMember);
                setServiceVersion(asVersion);
                }
            
            int cServices = input.readUnsignedShort();
            if (cServices > 0)
                {
                int[]    anServiceId         = new int    [cServices];
                String[] asServiceName       = new String [cServices];
                String[] asServiceType       = new String [cServices];
                boolean[] afServiceSuspended = new boolean[cServices];
                for (int i = 0; i < cServices; ++i)
                    {
                    anServiceId       [i] = input.readUnsignedShort();
                    asServiceName     [i] = input.readUTF();
                    asServiceType     [i] = input.readUTF();
                    afServiceSuspended[i] = input.readBoolean();
                    }
                setServiceId(anServiceId);
                setServiceName(asServiceName);
                setServiceType(asServiceType);
                setServiceSuspended(afServiceSuspended);
                }
            }
        
        // Accessor for the property "Member"
        /**
         * Setter for property Member.<p>
        * The cluster Members (indexed 0..MemberCount-1).
         */
        public void setMember(com.tangosol.coherence.component.net.Member[] anId)
            {
            __m_Member = anId;
            }
        
        // Accessor for the property "Member"
        /**
         * Setter for property Member.<p>
        * The cluster Members (indexed 0..MemberCount-1).
         */
        public void setMember(int i, com.tangosol.coherence.component.net.Member nId)
            {
            getMember()[i] = nId;
            }
        
        // Accessor for the property "MemberCount"
        /**
         * Setter for property MemberCount.<p>
        * The number of Member id/UID combinations that are included in this
        * Message.
         */
        public void setMemberCount(int c)
            {
            // import Component.Net.Member;
            
            Member[] aOldMbr  = getMember();
            int      cOldMbrs = (aOldMbr == null ? 0 : aOldMbr.length);
            if (c != cOldMbrs)
                {
                Member[] aNewMbr = new Member[c];
                if (cOldMbrs > 0)
                    {
                    System.arraycopy(aOldMbr, 0, aNewMbr, 0, Math.min(c, cOldMbrs));
                    }
                setMember(aNewMbr);
                }
            
            String[] asOldVer = getServiceVersion();
            int      cOldVers = (asOldVer == null ? 0 : asOldVer.length);
            if (c != cOldVers)
                {
                String[] asNewVer = new String[c];
                if (cOldVers > 0)
                    {
                    System.arraycopy(asOldVer, 0, asNewVer, 0, Math.min(c, cOldVers));
                    }
                setServiceVersion(asNewVer);
                }
            }
        
        // Accessor for the property "ServiceCount"
        /**
         * Setter for property ServiceCount.<p>
        * The number of Service id/name combinations that are included in this
        * Message.
         */
        public void setServiceCount(int c)
            {
            int[] anOldId = getServiceId();
            int   cOldIds = (anOldId == null ? 0 : anOldId.length);
            if (c != cOldIds)
                {
                int[] anNewId = new int[c];
                if (cOldIds > 0)
                    {
                    System.arraycopy(anOldId, 0, anNewId, 0, Math.min(c, cOldIds));
                    }
                setServiceId(anNewId);
                }
            
            String[] asOldName = getServiceName();
            int      cOldNames = (asOldName == null ? 0 : asOldName.length);
            if (c != cOldNames)
                {
                String[] asNewName = new String[c];
                if (cOldNames > 0)
                    {
                    System.arraycopy(asOldName, 0, asNewName, 0, Math.min(c, cOldNames));
                    }
                setServiceName(asNewName);
                }
            
            String[] asOldType = getServiceType();
            int      cOldTypes = (asOldType == null ? 0 : asOldType.length);
            if (c != cOldTypes)
                {
                String[] asNewType = new String[c];
                if (cOldTypes > 0)
                    {
                    System.arraycopy(asOldType, 0, asNewType, 0, Math.min(c, cOldTypes));
                    }
                setServiceType(asNewType);
                }
            
            boolean[] afOldSuspended = isServiceSuspended();
            int       cOldSuspended  = (afOldSuspended == null ? 0 : afOldSuspended.length);
            if (c != cOldSuspended)
                {
                boolean[] afNewSuspended = new boolean[c];
                if (cOldSuspended > 0)
                    {
                    System.arraycopy(afOldSuspended, 0, afNewSuspended, 0, Math.min(c, cOldSuspended));
                    }
                setServiceSuspended(afNewSuspended);
                }
            }
        
        // Accessor for the property "ServiceId"
        /**
         * Setter for property ServiceId.<p>
        * The Service ids of the known cluster Services (indexed
        * 0..ServiceCount-1).
         */
        public void setServiceId(int[] anId)
            {
            __m_ServiceId = anId;
            }
        
        // Accessor for the property "ServiceId"
        /**
         * Setter for property ServiceId.<p>
        * The Service ids of the known cluster Services (indexed
        * 0..ServiceCount-1).
         */
        public void setServiceId(int i, int nId)
            {
            getServiceId()[i] = nId;
            }
        
        // Accessor for the property "ServiceName"
        /**
         * Setter for property ServiceName.<p>
        * The names of the known cluster Services (indexed 0..ServiceCount-1).
         */
        public void setServiceName(String[] asName)
            {
            __m_ServiceName = asName;
            }
        
        // Accessor for the property "ServiceName"
        /**
         * Setter for property ServiceName.<p>
        * The names of the known cluster Services (indexed 0..ServiceCount-1).
         */
        public void setServiceName(int i, String sName)
            {
            getServiceName()[i] = sName;
            }
        
        // Accessor for the property "ServiceSuspended"
        /**
         * Setter for property ServiceSuspended.<p>
        * The suspend state of the known cluster Services (indexed
        * 0..ServiceCount-1).
         */
        public void setServiceSuspended(boolean[] afSuspended)
            {
            __m_ServiceSuspended = afSuspended;
            }
        
        // Accessor for the property "ServiceSuspended"
        /**
         * Setter for property ServiceSuspended.<p>
        * The suspend state of the known cluster Services (indexed
        * 0..ServiceCount-1).
         */
        public void setServiceSuspended(int i, boolean fSuspended)
            {
            isServiceSuspended()[i] = fSuspended;
            }
        
        // Accessor for the property "ServiceType"
        /**
         * Setter for property ServiceType.<p>
        * The types of the known cluster Services (indexed 0..ServiceCount-1).
         */
        public void setServiceType(String[] asType)
            {
            __m_ServiceType = asType;
            }
        
        // Accessor for the property "ServiceType"
        /**
         * Setter for property ServiceType.<p>
        * The types of the known cluster Services (indexed 0..ServiceCount-1).
         */
        public void setServiceType(int i, String sType)
            {
            getServiceType()[i] = sType;
            }
        
        // Accessor for the property "ServiceVersion"
        /**
         * Setter for property ServiceVersion.<p>
        * The version of ClusterService that each Member is running (indexed
        * 0..MemberCount-1).
         */
        public void setServiceVersion(String[] asVersion)
            {
            __m_ServiceVersion = asVersion;
            }
        
        // Accessor for the property "ServiceVersion"
        /**
         * Setter for property ServiceVersion.<p>
        * The version of ClusterService that each Member is running (indexed
        * 0..MemberCount-1).
         */
        public void setServiceVersion(int i, String sVersion)
            {
            getServiceVersion()[i] = sVersion;
            }
        
        // Declared at the super level
        public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
                throws java.io.IOException
            {
            // import Component.Net.Member;
            // import com.tangosol.util.ExternalizableHelper as com.tangosol.util.ExternalizableHelper;
            
            int cMembers = getMemberCount();
            output.writeShort(cMembers);
            if (cMembers > 0)
                {
                Member[] aMember   = getMember();
                String[] asVersion = getServiceVersion();
                for (int i = 0; i < cMembers; ++i)
                    {
                    Member member = aMember[i];
                    member.writeExternal(output);
                    com.tangosol.util.ExternalizableHelper.writeInt(output, member.getTcpRingPort());
                    com.tangosol.util.ExternalizableHelper.writeUTF(output, asVersion[i]);
                    }
                }
            
            int cServices = getServiceCount();
            output.writeShort(cServices);
            if (cServices > 0)
                {
                int[]     anServiceId        = getServiceId();
                String[]  asServiceName      = getServiceName();
                String[]  asServiceType      = getServiceType();
                boolean[] afServiceSuspended = isServiceSuspended();
                for (int i = 0; i < cServices; ++i)
                    {
                    output.writeShort(anServiceId[i]);
                    output.writeUTF(asServiceName[i]);
                    output.writeUTF(asServiceType[i]);
                    output.writeBoolean(afServiceSuspended[i]);
                    }
                }
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService$NewMemberRequestId
    
    /**
     * DiscoveryMessage (broadcast):
     *     NewMemberRequestId
     * 
     * Purpose:
     *     Broadcasts the cluster UID of a new Member (sans id) and requests a
     * mini cluster Member id from the senior Member.
     * 
     * Description:
     *     This Message is broadcast by a new Member until it receives a
     * NewMemberRequestIdReply from the senior Member of the cluster or a
     * timeout occurs. If the timeout occurs, the new Member assumes that no
     * cluster already exists, and goes back to broadcasting the
     * NewMemberAnnounce Message. To prevent this from happening if a cluster
     * is in transition (e.g. the senior Member has died but has not yet been
     * determined to be dead), a non-senior Member may reply with the
     * NewMemberRequestIdWait, which tells the new Member to reset its timeout.
     * 
     * Attributes:
     *     AttemptCounter
     *     AttemptLimit
     *     ServiceVersion (since 12.2.1 - VERSION_BARRIER)
     *     CpuCount (since 2.1)
     *     MaxPacketSize (since 2.4)
     *     WkaEnabled (since 3.1)
     *     ClusterName (since 3.1)
     * 
     * Response to:
     *     NewMemberAnnounceReply
     * 
     * Expected responses:
     *     NewMemberRequestIdReject
     *     NewMemberRequestIdReply
     *     NewMemberRequestIdWait
     *     NewMemberInduct
     *     MemberJoined (to all other Members)
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class NewMemberRequestId
            extends    com.tangosol.coherence.component.net.message.DiscoveryMessage
            implements com.tangosol.net.Service.MemberJoinAction
        {
        // ---- Fields declarations ----
        
        /**
         * Property AttemptCounter
         *
         * This is a number that increases while this Message is being
         * repeatedly sent. When the AttemptLimit is reached without any
         * replies (including "wait" replies), the new Member goes back  to
         * sending announcements.
         */
        private int __m_AttemptCounter;
        
        /**
         * Property AttemptLimit
         *
         * The number of times that the new Member will request an id before
         * giving up and going back to announcing itself.
         */
        private int __m_AttemptLimit;
        
        /**
         * Property Count
         *
         * Number of bytes to skip.
         */
        private transient int __m_Count;
        
        /**
         * Property MaxPacketSize
         *
         * The maximum number of bytes that the node will send and can receive
         * in a packet. This is used to warn the senior node of the cluster
         * that is being joined, just in case there is a configuration
         * mis-match between the running cluster and the new member.
         */
        private int __m_MaxPacketSize;
        
        /**
         * Property ServiceVersion
         *
         * The version of the ClusterService that the new Member is running.
         * This is used to warn the senior node of the cluster that is being
         * joined, just in case there is a version mis-match between the
         * running cluster and the new member.
         */
        private String __m_ServiceVersion;
        
        /**
         * Property WkaEnabled
         *
         * Indicates if the requesting member has WKA enabled.  The senior will
         * use this to detect if WKA mismatch i.e. WKA to non-WKA.
         */
        private boolean __m_WkaEnabled;
        
        // Default constructor
        public NewMemberRequestId()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public NewMemberRequestId(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);
            
            if (fInit)
                {
                __init();
                }
            }
        
        // Main initializer
        public void __init()
            {
            // private initialization
            __initPrivate();
            
            // state initialization: public and protected properties
            try
                {
                setMessageType(10);
                }
            catch (java.lang.Exception e)
                {
                // re-throw as a runtime exception
                throw new com.tangosol.util.WrapperException(e);
                }
            
            // signal the end of the initialization
            set_Constructed(true);
            }
        
        // Private initializer
        protected void __initPrivate()
            {
            
            super.__initPrivate();
            }
        
        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
        * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService.NewMemberRequestId();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ClusterService$NewMemberRequestId".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }
        
        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
        * time] parent component.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent();
            }
        
        // Declared at the super level
        /**
         * Test that the end of the input stream has been reached.  If the EOS
        * has not been reached the readError flag is set.  Note this method may
        * attempt to read data from the stream, so calling it on a stream which
        * may still be used requires marking the stream position ahead of time.
         */
        public void ensureEOS(java.io.DataInput stream)
                throws java.io.IOException
            {
            // "induct" the licenses
            ((ClusterService) getService()).resetBroadcastCounter(stream, false);
            
            super.ensureEOS(stream);
            }
        
        // Accessor for the property "AttemptCounter"
        /**
         * Getter for property AttemptCounter.<p>
        * This is a number that increases while this Message is being
        * repeatedly sent. When the AttemptLimit is reached without any replies
        * (including "wait" replies), the new Member goes back  to sending
        * announcements.
         */
        public int getAttemptCounter()
            {
            return __m_AttemptCounter;
            }
        
        // Accessor for the property "AttemptLimit"
        /**
         * Getter for property AttemptLimit.<p>
        * The number of times that the new Member will request an id before
        * giving up and going back to announcing itself.
         */
        public int getAttemptLimit()
            {
            return __m_AttemptLimit;
            }
        
        // Accessor for the property "Count"
        /**
         * Getter for property Count.<p>
        * Number of bytes to skip.
         */
        public int getCount()
            {
            return __m_Count;
            }
        
        // Declared at the super level
        /**
         * Getter for property Description.<p>
        * Used for debugging purposes (from toString). Create a human-readable
        * description of the specific Message data.
         */
        public String getDescription()
            {
            return "AttemptCounter="   + getAttemptCounter()
                 + "\nAttemptLimit="   + getAttemptLimit()
                 + "\nServiceVersion=" + getServiceVersion();
            }
        
        // From interface: com.tangosol.net.Service$MemberJoinAction
        public com.tangosol.net.Member getJoiningMember()
            {
            return getFromMember();
            }
        
        // Accessor for the property "MaxPacketSize"
        /**
         * Getter for property MaxPacketSize.<p>
        * The maximum number of bytes that the node will send and can receive
        * in a packet. This is used to warn the senior node of the cluster that
        * is being joined, just in case there is a configuration mis-match
        * between the running cluster and the new member.
         */
        public int getMaxPacketSize()
            {
            return __m_MaxPacketSize;
            }
        
        // Accessor for the property "ServiceVersion"
        /**
         * Getter for property ServiceVersion.<p>
        * The version of the ClusterService that the new Member is running.
        * This is used to warn the senior node of the cluster that is being
        * joined, just in case there is a version mis-match between the running
        * cluster and the new member.
         */
        public String getServiceVersion()
            {
            return __m_ServiceVersion;
            }
        
        // Accessor for the property "WkaEnabled"
        /**
         * Getter for property WkaEnabled.<p>
        * Indicates if the requesting member has WKA enabled.  The senior will
        * use this to detect if WKA mismatch i.e. WKA to non-WKA.
         */
        public boolean isWkaEnabled()
            {
            return __m_WkaEnabled;
            }
        
        // Declared at the super level
        /**
         * This is the event that is executed when a Message is received.
        * <p>
        * It is the main processing event of the Message called by the
        * <code>Service.onMessage()</code> event. With regards to the use of
        * Message components within clustered Services, Services are designed
        * by dragging Message components into them as static children. These
        * Messages are the components that a Service can send to other running
        * instances of the same Service within a cluster. When the onReceived
        * event is invoked by a Service, it means that the Message has been
        * received; the code in the onReceived event is therefore the Message
        * specific logic for processing a received Message. For example, when
        * onReceived is invoked on a Message named FindData, the onReceived
        * event should do the work to "find the data", because it is being
        * invoked by the Service that received the "find the data" Message.
         */
        public void onReceived()
            {
            // import Component.Net.Cluster;
            // import Component.Net.Member;
            // import Component.Net.MemberSet;
            // import Component.Net.MemberSet.ActualMemberSet;
            // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet.MasterMemberSet;
            // import com.tangosol.run.component.EventDeathException;
            // import com.tangosol.util.Base;
            // import com.tangosol.util.UUID;
            
            super.onReceived();
            
            ClusterService service    = (ClusterService) getService();
            Member  memberFrom = getFromMember();
            switch (service.getState())
                {
                case ClusterService.STATE_ANNOUNCE:
                    if (isReadError())
                        {
                        service.onMemberRejected(ClusterService.REJECT_VERSION, null);
                        throw new EventDeathException("Version mismatch");
                        }
                
                    // someone is further along in joining than we are,
                    // so be patient
                    service.resetBroadcastCounter("the presence of an existing cluster", this);
                    break;
            
                case ClusterService.STATE_JOINING:
                    {
                    if (isReadError())
                        {
                        service.onMemberRejected(ClusterService.REJECT_VERSION, null);
                        throw new EventDeathException("Version mismatch");
                        }
                            
                    // someone at least _was_ sending back announce replies so
                    // assume that a cluster is running and reset the broadcast
                    // counter if the other joining member is older
                    Member memberThis = service.getRequestMember();
                    if (!memberFrom.equals(memberThis)) // ignore own messages
                        {
                        // another future Member is requesting; if the other
                        // future Member is "older", then reset the cluster
                        // service's attempt counter (to give the other Member
                        // time to join the cluster)
                        if (memberThis.compareTo(memberFrom) > 0)
                            {
                            // worst case is that multiple members are requesting
                            // when the senior dies and there are no other members
                            // so the requesting ones will one-by-one timeout and
                            // go back to announcing; it's a little slow, but not
                            // fatal, and certainly safer
                            service.resetBroadcastCounter("an older member joining", this);
                            }
                        }
                    }
                    break;
            
                case ClusterService.STATE_JOINED:
                    {      
                    // a future Member is requesting an id; this Member
                    // can supply it if and only if this Member is oldest
                    MasterMemberSet setMember    = service.getClusterMemberSet();
                    Member          memberSenior = setMember.getOldestMember();
                    Member          memberThis   = setMember.getThisMember();
                    Member          memberNew    = setMember.getMember(memberFrom.getUid32());
            
                    // the order of checks is important; validateNewMember() should take the precedence 
                    if (memberThis == memberSenior &&
                        service.validateNewMember(memberFrom) &&
                        !service.isMembershipSuspended() &&
                        (memberNew != null || service.getPendingServiceJoining().isEmpty()))
                        {
                        int     cbPacketNew      = getMaxPacketSize();
                        int     cbPacketThis     = service.getMaximumPacketLength();
                        String  sVersionNew      = getServiceVersion();
                        boolean fWkaEnabledNew   = isWkaEnabled();
                        boolean fWkaEnabledThis  = service.isWkaEnabled();
                        String  sClusterNameNew  = memberFrom.getClusterName();
                        String  sClusterNameThis = service.getClusterName();
                        int     nEditionNew      = memberFrom.getEdition();
                        int     nEditionThis     = memberThis.getEdition();
                        int     nModeNew         = memberFrom.getMode();
                        int     nModeThis        = memberThis.getMode();
                        boolean fAllowRTCThis    = nEditionThis == 5 || nEditionThis == 3;
                        boolean fAllowRTCNew     = nEditionNew  == 5 || nEditionNew  == 3;
            
                        // keep track of whether this Member has ever
                        // been heard of before
                        boolean fBrandNew = false;
                        if (memberNew == null)
                            {
                            // 1) check the protocol version to avoid confusion
                            //    between nodes of different versions;
                            //    for now just disallow multiple versions to talk
                            // 2) check the maximum packet size of the new
                            //    member to make sure that it matches ours
                            //    exactly; otherwise sooner or later one or
                            //    the other will send a packet that will not
                            //    be receivable by the other
                            // 3) verify that WKA usage matches i.e. don't allow
                            //    a WKA into a multicast cluster or other way around
                            // 4) verify that the cluster names match
            
                            int nRejectReason;
            
                            if (!service.isVersionCompatible(sVersionNew)
                                  || cbPacketNew == 0 || isReadError())
                                {
                                // protocol version mismatch either detected explicitly,
                                // or via a message read error;
                                // actively reject member
                                nRejectReason = ClusterService.REJECT_VERSION;
                                }
                            else if (!Base.equals(sClusterNameNew, sClusterNameThis))
                                {
                                // checked before other configuration elements, if you
                                // aren't joining the expected cluster then the other
                                // config mismatches aren't currently important
                                nRejectReason = ClusterService.REJECT_CLUSTER_NAME;
                                }
                            else if (cbPacketNew != cbPacketThis)
                                {
                                // packet size mismatch
                                nRejectReason = ClusterService.REJECT_PACKET_MAX;
                                }
                            else if (fWkaEnabledNew != fWkaEnabledThis)
                                {
                                // WKA / Multicast mismatch
                                nRejectReason = ClusterService.REJECT_WKA;
                                }
                            else if (nModeNew != nModeThis)
                                {
                                // license type ("mode") mismatch
                                nRejectReason = ClusterService.REJECT_LICENSE_TYPE;
                                }
                            else if (!((nEditionNew >= 3 && nEditionThis >= 3) ||
                                       (nEditionNew == 1 && fAllowRTCThis    ) ||
                                       (fAllowRTCNew     && nEditionThis == 1)   ))
                                {
                                // product edition mismatch
                                nRejectReason = ClusterService.REJECT_EDITION;
                                }
                            else if (!service.getActionPolicy().isAllowed(service, this))
                                {
                                // quorum policy rejection
                                nRejectReason = ClusterService.REJECT_QUORUM;
                                }
                            else
                                {
                                // assign the new id
                                memberNew = service.instantiateMember();
                                memberNew.configure(memberFrom, 0L);
                                nRejectReason = setMember.induct(memberNew, service);
                                }
            
                            if (nRejectReason == ClusterService.REJECT_NONE)
                                {
                                fBrandNew = true;
            
                                setMember.setServiceVersion(
                                        memberNew.getId(), sVersionNew);
                                setMember.setServiceJoinTime(
                                        memberNew.getId(), memberNew.getUid32().getTimestamp());
                                }
                            else
                                {
                                // induct failed! (send back rejection)
                                ClusterService.NewMemberRequestIdReject msg = (ClusterService.NewMemberRequestIdReject)
                                        service.instantiateMessage("NewMemberRequestIdReject");
                                msg.setToMember(memberFrom);
                                msg.setReason(nRejectReason);
                                service.send(msg);
                                break;
                                }
                            }
            
                        // send back Member's mini-id information
                        // (whether or not it is a brand new Member)
                            {
                            ClusterService.NewMemberRequestIdReply msg = (ClusterService.NewMemberRequestIdReply)
                                    service.instantiateMessage("NewMemberRequestIdReply");
                            msg.setToMember(memberNew);
                            msg.setServiceVersion(ClusterService.VERSION_BARRIER);
                            msg.setMulticastTimeToLive(service.getMulticastTimeToLive());
                            msg.setWkaHashCode(service.getWkaHashCode());
                            service.send(msg);
                            }
            
                        // announce the Member (only if the Member has
                        // not already been announced)
                        if (fBrandNew)
                            {
                            // send first directed message
            
                            // before inducting the new member, we send it the departed MemberSet
                            // so that when inducted it is up to date, thus minimizing or negating
                            // the need for the respones
                            service.ensureMemberLeft(/*memberTo*/ memberNew, /*setUUIDExempt*/ null, /*fRequest*/ false);
                            
                            service.doMemberInduct(memberNew);
                            
                            // internal announcement of new Member
                            service.onMemberJoined(memberNew);
            
                            // send announcement of new Member to cluster Members
                            // build a set of all Members not counting the
                            // oldest and the new Member (i.e. all those not
                            // involved in the join-up conversation)
                            MemberSet setOthers = new ActualMemberSet();
                            setOthers.addAll(setMember);
                            setOthers.remove(memberThis);
                            setOthers.remove(memberNew);
                            if (!setOthers.isEmpty())
                                {
                                ClusterService.MemberJoined msg = (ClusterService.MemberJoined)
                                        service.instantiateMessage("MemberJoined");
                                msg.setToMemberSet(setOthers);
                                msg.setMember(memberNew);
                                msg.setServiceVersion(sVersionNew);
                                service.send(msg);
                                }
                            }
                        }
                    else if (getAttemptCounter() > (getAttemptLimit() >>> 2))
                        {
                        // stagger this to prevent a flood
                        int cMembers = setMember.size();
                        if (((Cluster) service.getCluster()).getDependencies().getPublisherGroupThreshold() >= 100 ||
                             (cMembers < 8 || Base.getRandom().nextInt(cMembers) < ((int) Math.log(cMembers)) + 1))
                            {
                            // tell the new Member to wait because its request id
                            // period is over 1/4 done (don't let it start its
                            // own cluster)
                            ClusterService.NewMemberRequestIdWait msg = (ClusterService.NewMemberRequestIdWait)
                                    service.instantiateMessage("NewMemberRequestIdWait");
                            msg.setToMember(memberFrom);
                            msg.setSeniorMember(memberSenior);
                            service.send(msg);
                            }
                        }
                    }
                    break;
                }
            }
        
        // Declared at the super level
        /**
         * for debugging purposes:
        * 
        * _trace("*** NewMemberRequestId.read()");
        * _trace("counts=" + an[0] + ", " + an[1]);
        * _trace("prod=" + getEdition());
        * _trace("mode=" + getMode());
        * _trace("uids=" + listUid);
        * _trace("prods=" + listProd);
        * _trace("types=" + listType);
        * _trace("limits=" + listLimit);
         */
        public void read(com.tangosol.io.ReadBuffer.BufferInput input)
                throws java.io.IOException
            {
            // import com.tangosol.util.Base;
            // import com.tangosol.util.ExternalizableHelper as com.tangosol.util.ExternalizableHelper;
            // import com.tangosol.util.UID;
            // import java.util.ArrayList;
            
            super.read(input);
            
            setAttemptCounter(com.tangosol.util.ExternalizableHelper.readInt(input));
            setAttemptLimit(com.tangosol.util.ExternalizableHelper.readInt(input));
            setServiceVersion(com.tangosol.util.ExternalizableHelper.readUTF(input));
            setMaxPacketSize(com.tangosol.util.ExternalizableHelper.readInt(input));
            setWkaEnabled(input.readBoolean());
            getFromMember().setTcpRingPort(com.tangosol.util.ExternalizableHelper.readInt(input));
            
            // version 3.2 additions (note: asymmetry between write and read)
            // the next value is the "broadcast timestamp", which is just an obfuscated
            // name for "binary license data"; despite what it appears here, the real
            // work gets done by ensureEOS(), which in this one case goes to
            // resetBroadcastCounter() to read all this license data for use
            // by the service (i.e. to register the licenses)
            
            // Binary length (discarded)
            setCount(input.readInt());
            
            ensureEOS(input);
            }
        
        // Accessor for the property "AttemptCounter"
        /**
         * Setter for property AttemptCounter.<p>
        * This is a number that increases while this Message is being
        * repeatedly sent. When the AttemptLimit is reached without any replies
        * (including "wait" replies), the new Member goes back  to sending
        * announcements.
         */
        public void setAttemptCounter(int n)
            {
            __m_AttemptCounter = n;
            }
        
        // Accessor for the property "AttemptLimit"
        /**
         * Setter for property AttemptLimit.<p>
        * The number of times that the new Member will request an id before
        * giving up and going back to announcing itself.
         */
        public void setAttemptLimit(int c)
            {
            __m_AttemptLimit = c;
            }
        
        // Accessor for the property "Count"
        /**
         * Setter for property Count.<p>
        * Number of bytes to skip.
         */
        public void setCount(int pCount)
            {
            __m_Count = pCount;
            }
        
        // Accessor for the property "MaxPacketSize"
        /**
         * Setter for property MaxPacketSize.<p>
        * The maximum number of bytes that the node will send and can receive
        * in a packet. This is used to warn the senior node of the cluster that
        * is being joined, just in case there is a configuration mis-match
        * between the running cluster and the new member.
         */
        public void setMaxPacketSize(int cb)
            {
            __m_MaxPacketSize = cb;
            }
        
        // Accessor for the property "ServiceVersion"
        /**
         * Setter for property ServiceVersion.<p>
        * The version of the ClusterService that the new Member is running.
        * This is used to warn the senior node of the cluster that is being
        * joined, just in case there is a version mis-match between the running
        * cluster and the new member.
         */
        public void setServiceVersion(String sVersion)
            {
            __m_ServiceVersion = sVersion;
            }
        
        // Accessor for the property "WkaEnabled"
        /**
         * Setter for property WkaEnabled.<p>
        * Indicates if the requesting member has WKA enabled.  The senior will
        * use this to detect if WKA mismatch i.e. WKA to non-WKA.
         */
        public void setWkaEnabled(boolean fEnabled)
            {
            __m_WkaEnabled = fEnabled;
            }
        
        // Declared at the super level
        public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
                throws java.io.IOException
            {
            // import com.tangosol.util.ExternalizableHelper as com.tangosol.util.ExternalizableHelper;
            
            super.write(output);
            
            com.tangosol.util.ExternalizableHelper.writeInt(output, getAttemptCounter());
            com.tangosol.util.ExternalizableHelper.writeInt(output, getAttemptLimit());
            com.tangosol.util.ExternalizableHelper.writeUTF(output, getServiceVersion());
            com.tangosol.util.ExternalizableHelper.writeInt(output, getMaxPacketSize());
            output.writeBoolean(isWkaEnabled());
            com.tangosol.util.ExternalizableHelper.writeInt(output, getFromMember().getTcpRingPort());
            
            // version 3.2 addition: licenses to contribute to the cluster
            ((ClusterService) getService()).getBroadcastTimestamp().writeExternal(output);
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService$NewMemberRequestIdReject
    
    /**
     * DiscoveryMessage (broadcast):
     *     NewMemberRequestIdReject
     * 
     * Purpose:
     *     Broadcasts to the new Member to tell it to kill itself.
     * 
     * Description:
     *     This Message is broadcast by any cluster Member when a new Member
     * has attempted to join the cluster but some reason prevents it from
     * joining.
     * 
     * Attributes:
     *     Reason
     * 
     * Response to:
     *     NewMemberRequestId
     * 
     * Expected responses:
     *     SeniorMemberPanic
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class NewMemberRequestIdReject
            extends    com.tangosol.coherence.component.net.message.DiscoveryMessage
        {
        // ---- Fields declarations ----
        
        /**
         * Property Reason
         *
         * The reason id for rejecting entry into the cluster. Currently the
         * positive number that is less then 8160 (MasterMemberSet.MAX_MEMBER)
         * represents the excess of actual CPUs over the limit allowed by the
         * license.
         * 
         * For other reasons see ClusterService.onMemberReject.
         */
        private int __m_Reason;
        
        // Default constructor
        public NewMemberRequestIdReject()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public NewMemberRequestIdReject(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);
            
            if (fInit)
                {
                __init();
                }
            }
        
        // Main initializer
        public void __init()
            {
            // private initialization
            __initPrivate();
            
            // state initialization: public and protected properties
            try
                {
                setMessageType(11);
                }
            catch (java.lang.Exception e)
                {
                // re-throw as a runtime exception
                throw new com.tangosol.util.WrapperException(e);
                }
            
            // signal the end of the initialization
            set_Constructed(true);
            }
        
        // Private initializer
        protected void __initPrivate()
            {
            
            super.__initPrivate();
            }
        
        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
        * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService.NewMemberRequestIdReject();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ClusterService$NewMemberRequestIdReject".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }
        
        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
        * time] parent component.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent();
            }
        
        // Declared at the super level
        /**
         * Getter for property Description.<p>
        * Used for debugging purposes (from toString). Create a human-readable
        * description of the specific Message data.
         */
        public String getDescription()
            {
            return "ToMember=" + getToMember()
                 + "\nReason=" + getReason();
            }
        
        // Accessor for the property "Reason"
        /**
         * Getter for property Reason.<p>
        * The reason id for rejecting entry into the cluster. Currently the
        * positive number that is less then 8160 (MasterMemberSet.MAX_MEMBER)
        * represents the excess of actual CPUs over the limit allowed by the
        * license.
        * 
        * For other reasons see ClusterService.onMemberReject.
         */
        public int getReason()
            {
            return __m_Reason;
            }
        
        // Declared at the super level
        /**
         * This is the event that is executed when a Message is received.
        * <p>
        * It is the main processing event of the Message called by the
        * <code>Service.onMessage()</code> event. With regards to the use of
        * Message components within clustered Services, Services are designed
        * by dragging Message components into them as static children. These
        * Messages are the components that a Service can send to other running
        * instances of the same Service within a cluster. When the onReceived
        * event is invoked by a Service, it means that the Message has been
        * received; the code in the onReceived event is therefore the Message
        * specific logic for processing a received Message. For example, when
        * onReceived is invoked on a Message named FindData, the onReceived
        * event should do the work to "find the data", because it is being
        * invoked by the Service that received the "find the data" Message.
         */
        public void onReceived()
            {
            // import Component.Net.Member;
            // import com.tangosol.run.component.EventDeathException;
            
            super.onReceived();
            
            ClusterService service    = (ClusterService) getService();
            Member  memberFrom = getFromMember();
            switch (service.getState())
                {
                case ClusterService.STATE_ANNOUNCE:
                    if (isReadError())
                        {
                        // version mismatch; stop this node
                        service.onMemberRejected(ClusterService.REJECT_VERSION, memberFrom);
                        throw new EventDeathException("Version mismatch");
                        }
            
                    // reset the "request counter" because a cluster is running
                    service.resetBroadcastCounter("the presence of an existing cluster", this);
                    break;
            
                case ClusterService.STATE_JOINING:
                    if (isReadError())
                        {
                        // version mismatch; stop this node
                        service.onMemberRejected(ClusterService.REJECT_VERSION, memberFrom);
                        throw new EventDeathException("Version mismatch");
                        }
            
                    // someone is sending back request-id replies; that means no
                    // matter what, keep trying
                    service.resetBroadcastCounter("the presence of an existing cluster", this);
            
                    // verify Message sent to this Member
                    if (getToMember().equals(service.getRequestMember()))
                        {
                        service.onMemberRejected(getReason(), memberFrom);
                        }
                    break;
            
                case ClusterService.STATE_JOINED:
                    service.validateSeniorBroadcast(this, null);
                    break;
                }
            }
        
        // Declared at the super level
        public void read(com.tangosol.io.ReadBuffer.BufferInput input)
                throws java.io.IOException
            {
            super.read(input);
            
            setReason(input.readInt());
            
            ensureEOS(input);
            }
        
        // Accessor for the property "Reason"
        /**
         * Setter for property Reason.<p>
        * The reason id for rejecting entry into the cluster. Currently the
        * positive number that is less then 8160 (MasterMemberSet.MAX_MEMBER)
        * represents the excess of actual CPUs over the limit allowed by the
        * license.
        * 
        * For other reasons see ClusterService.onMemberReject.
         */
        public void setReason(int nReason)
            {
            __m_Reason = nReason;
            }
        
        // Declared at the super level
        public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
                throws java.io.IOException
            {
            super.write(output);
            
            output.writeInt(getReason());
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService$NewMemberRequestIdReply
    
    /**
     * DiscoveryMessage (broadcast):
     *     NewMemberRequestIdReply
     * 
     * Purpose:
     *     Broadcast to the new Member to provide it with a cluster Member id
     * so that the new Member can send and receive non-broadcast Messages.
     * 
     * Description:
     *     This Message is broadcast by the senior cluster Member when a new
     * Member has requested an id. The new Member can get its id from this
     * Message, and can then communicate directly with the senior Member.
     * 
     * Attributes:
     *     ToMember contains the newly assigned mini-id
     *     ServiceVersion (since 12.2.1 - VERSION_BARRIER)
     *     MulticastTimeToLive (since 3.1)
     *     WkaListHashCode (since 3.1)
     * 
     * Response to:
     *     NewMemberRequestId
     * 
     * Expected responses:
     *     SeniorMemberPanic
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class NewMemberRequestIdReply
            extends    com.tangosol.coherence.component.net.message.DiscoveryMessage
        {
        // ---- Fields declarations ----
        
        /**
         * Property MulticastTimeToLive
         *
         * The Multicast TTL in use by the senior member.  The new member uses
         * this information to generate a warning if its TTL does not match. 
         * It is not a hard error to have a TTL mismatch, cluster membership
         * contintues.  If Multicast is not enabled then this value is
         * irrelivant and is ignored.
         */
        private int __m_MulticastTimeToLive;
        
        /**
         * Property ServiceVersion
         *
         * The version of the ClusterService being run by the oldest Member
         * that sent this Message.
         */
        private String __m_ServiceVersion;
        
        /**
         * Property WkaHashCode
         *
         * The hashCode of the senior member's WKA list.  The new member will
         * use this to detect mismatched WKAs.  If WKA is not enabled the value
         * is unspecified.  Mismatched W KA lists does not block the member
         * from joining the cluster, but will result in a warning being
         * generated.
         */
        private int __m_WkaHashCode;
        
        // Default constructor
        public NewMemberRequestIdReply()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public NewMemberRequestIdReply(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);
            
            if (fInit)
                {
                __init();
                }
            }
        
        // Main initializer
        public void __init()
            {
            // private initialization
            __initPrivate();
            
            // state initialization: public and protected properties
            try
                {
                setMessageType(12);
                }
            catch (java.lang.Exception e)
                {
                // re-throw as a runtime exception
                throw new com.tangosol.util.WrapperException(e);
                }
            
            // signal the end of the initialization
            set_Constructed(true);
            }
        
        // Private initializer
        protected void __initPrivate()
            {
            
            super.__initPrivate();
            }
        
        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
        * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService.NewMemberRequestIdReply();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ClusterService$NewMemberRequestIdReply".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }
        
        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
        * time] parent component.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent();
            }
        
        // Declared at the super level
        /**
         * Getter for property Description.<p>
        * Used for debugging purposes (from toString). Create a human-readable
        * description of the specific Message data.
         */
        public String getDescription()
            {
            return "ToMember="         + getToMember()
                 + "\nServiceVersion=" + getServiceVersion();
            }
        
        // Accessor for the property "MulticastTimeToLive"
        /**
         * Getter for property MulticastTimeToLive.<p>
        * The Multicast TTL in use by the senior member.  The new member uses
        * this information to generate a warning if its TTL does not match.  It
        * is not a hard error to have a TTL mismatch, cluster membership
        * contintues.  If Multicast is not enabled then this value is
        * irrelivant and is ignored.
         */
        public int getMulticastTimeToLive()
            {
            return __m_MulticastTimeToLive;
            }
        
        // Accessor for the property "ServiceVersion"
        /**
         * Getter for property ServiceVersion.<p>
        * The version of the ClusterService being run by the oldest Member that
        * sent this Message.
         */
        public String getServiceVersion()
            {
            return __m_ServiceVersion;
            }
        
        // Accessor for the property "WkaHashCode"
        /**
         * Getter for property WkaHashCode.<p>
        * The hashCode of the senior member's WKA list.  The new member will
        * use this to detect mismatched WKAs.  If WKA is not enabled the value
        * is unspecified.  Mismatched W KA lists does not block the member from
        * joining the cluster, but will result in a warning being generated.
         */
        public int getWkaHashCode()
            {
            return __m_WkaHashCode;
            }
        
        // Declared at the super level
        /**
         * This is the event that is executed when a Message is received.
        * <p>
        * It is the main processing event of the Message called by the
        * <code>Service.onMessage()</code> event. With regards to the use of
        * Message components within clustered Services, Services are designed
        * by dragging Message components into them as static children. These
        * Messages are the components that a Service can send to other running
        * instances of the same Service within a cluster. When the onReceived
        * event is invoked by a Service, it means that the Message has been
        * received; the code in the onReceived event is therefore the Message
        * specific logic for processing a received Message. For example, when
        * onReceived is invoked on a Message named FindData, the onReceived
        * event should do the work to "find the data", because it is being
        * invoked by the Service that received the "find the data" Message.
         */
        public void onReceived()
            {
            // import Component.Net.Cluster;
            // import Component.Net.Member;
            // import Component.Net.MemberSet;
            // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet.MasterMemberSet;
            // import com.tangosol.net.internal.ClusterJoinException;
            // import com.tangosol.run.component.EventDeathException;
            
            super.onReceived();
            
            Member  memberFrom = getFromMember();
            ClusterService service    = (ClusterService) getService();
            switch (service.getState())
                {
                case ClusterService.STATE_ANNOUNCE:
                    if (isReadError())
                        {
                        service.onMemberRejected(ClusterService.REJECT_VERSION, memberFrom);
                        throw new EventDeathException("Version mismatch");
                        }
                    
                    // reset the "request counter" because a cluster is running
                    service.resetBroadcastCounter("the presence of an existing cluster", this);
                    break;
            
                case ClusterService.STATE_JOINING:
                    {
                    if (isReadError())
                        {
                        service.onMemberRejected(ClusterService.REJECT_VERSION, memberFrom);
                        throw new EventDeathException("Version mismatch");
                        }
                            
                    // someone is sending back request-id replies; that means no
                    // matter what, keep trying
                    service.resetBroadcastCounter("the presence of an existing cluster", this);
            
                    // only process Message if it is addressed to this Member
                    // (we cannot compare the Member objects since an Id has just been assigned)
                    if (getToMember().getUid32().equals(service.getRequestMember().getUid32()))
                        {
                        MasterMemberSet setMember = service.getClusterMemberSet();
                        Cluster         cluster   = (Cluster) service.getCluster();
            
                        // create the Members
                        Member memberOldest = service.ensureMember(memberFrom, getServiceVersion());
                        if (memberOldest == null)
                            {
                            // received from a known dead member; ignore
                            return;
                            }
            
                        Member memberThis = service.getRequestMember();
                        if (setMember.getThisMember() == null)
                            {
                            // store the "this member"
                            memberThis.setId(getToMember().getId());
                            memberThis = service.ensureMember(memberThis,
                                    service.getServiceVersion());
                            memberThis.setPreferredPacketLength(cluster.getSocketManager().
                                    getPreferredUnicastUdpSocket().getPacketLength());
                            memberThis.setPreferredPort(cluster.getSocketManager().
                                    getPreferredUnicastUdpSocket().getPort());
            
                            setMember.setThisMember(memberThis);
            
                            service.onMemberJoined(memberOldest);
                            }
                        else if (setMember.getOldestMember() != memberOldest)
                            {
                            // This is from a new senior (the previous senior must have
                            // left). There are only 2 possibilities:
                            // 1. there was a seniority change during our join sequence
                            // 2. while we are joining, there are 2 clusters colliding
                            //    on the same broadcast domain
            
                            throw new ClusterJoinException();
                            }
                        else if (memberThis.getId() != getToMember().getId())
                            {
                            // The senior has assigned us a different ID, it would only do
                            // this because it had declared us as dead, and thus won't be
                            // sending the induct to the ID we're using.
                            
                            throw new ClusterJoinException();
                            }
                        else
                            {
                            // repeated broadcast from our senior member; ignore
                            return;
                            }
            
                        _trace("This " + memberThis.toString(Member.SHOW_LICENSE)
                             + " joined " + service.formatClusterString()
                             + " with senior " + memberOldest.toString(Member.SHOW_LICENSE), 3);
            
                        // issue warnings if certain key configuration items do not match.
                        // none of these are required to match, thus these are just warnings            
                        if (service.isWkaEnabled())
                            {
                            if (service.getWkaHashCode() != getWkaHashCode())
                                {
                                _trace("This member is configured with a compatible but "
                                     + "different WKA list than the senior " + memberOldest
                                     + ". It is strongly recommended to use the same WKA list for "
                                     + "all cluster members.", 2);
                                }
                            }
                        else
                            {
                            int iTtlThis   = service.getMulticastTimeToLive();
                            int iTtlSenior = getMulticastTimeToLive();
                            if (iTtlThis != iTtlSenior)
                                {
                                _trace("This member is configured with a multicast TTL of "
                                     + iTtlThis + "; the senior " + memberOldest + " is "
                                     + "configured with a TTL of " + iTtlSenior
                                     + ". It is strongly recommended to use the same TTL setting for "
                                     + "all cluster members.", 2);
                                }
                            }
                        }
                    }
                    break;
            
                case ClusterService.STATE_JOINED:
                    service.validateSeniorBroadcast(this, null);
                    break;
                }
            }
        
        // Declared at the super level
        public void read(com.tangosol.io.ReadBuffer.BufferInput input)
                throws java.io.IOException
            {
            super.read(input);    
            
            setServiceVersion(input.readUTF());
            setMulticastTimeToLive(input.readInt());
            setWkaHashCode(input.readInt());
            getFromMember().setTcpRingPort(input.readInt());
            
            ensureEOS(input);
            }
        
        // Accessor for the property "MulticastTimeToLive"
        /**
         * Setter for property MulticastTimeToLive.<p>
        * The Multicast TTL in use by the senior member.  The new member uses
        * this information to generate a warning if its TTL does not match.  It
        * is not a hard error to have a TTL mismatch, cluster membership
        * contintues.  If Multicast is not enabled then this value is
        * irrelivant and is ignored.
         */
        public void setMulticastTimeToLive(int pMulticastTimeToLive)
            {
            __m_MulticastTimeToLive = pMulticastTimeToLive;
            }
        
        // Accessor for the property "ServiceVersion"
        /**
         * Setter for property ServiceVersion.<p>
        * The version of the ClusterService being run by the oldest Member that
        * sent this Message.
         */
        public void setServiceVersion(String sVersion)
            {
            __m_ServiceVersion = sVersion;
            }
        
        // Accessor for the property "WkaHashCode"
        /**
         * Setter for property WkaHashCode.<p>
        * The hashCode of the senior member's WKA list.  The new member will
        * use this to detect mismatched WKAs.  If WKA is not enabled the value
        * is unspecified.  Mismatched W KA lists does not block the member from
        * joining the cluster, but will result in a warning being generated.
         */
        public void setWkaHashCode(int iHash)
            {
            __m_WkaHashCode = iHash;
            }
        
        // Declared at the super level
        public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
                throws java.io.IOException
            {
            super.write(output);
            
            output.writeUTF(getServiceVersion());
            output.writeInt(getMulticastTimeToLive());
            output.writeInt(getWkaHashCode());
            output.writeInt(getFromMember().getTcpRingPort());
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService$NewMemberRequestIdWait
    
    /**
     * DiscoveryMessage (broadcast):
     *     NewMemberRequestIdWait
     * 
     * Purpose:
     *     Broadcasts to the new Member to keep announcing because there is
     * definitely a cluster already in existence
     * 
     * Description:
     *     This Message is broadcast by any non-senior cluster Member when a
     * new Member has passed a certain threshold in its repeated
     * NewMemberRequestId Messages. The non-senior cluster Member determines
     * that the new Member is getting close to going back to announcing because
     * it has not yet received a reply from the senior Member, so the
     * non-senior cluster Member tells the new Member to wait.
     * 
     * Attributes:
     * 
     * Response to:
     *     NewMemberRequestId
     * 
     * Expected responses:
     *     n/a
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class NewMemberRequestIdWait
            extends    com.tangosol.coherence.component.net.message.DiscoveryMessage
        {
        // ---- Fields declarations ----
        
        /**
         * Property SeniorMember
         *
         * The senior Member that should respond to the requesting member.
         */
        private com.tangosol.coherence.component.net.Member __m_SeniorMember;
        
        // Default constructor
        public NewMemberRequestIdWait()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public NewMemberRequestIdWait(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);
            
            if (fInit)
                {
                __init();
                }
            }
        
        // Main initializer
        public void __init()
            {
            // private initialization
            __initPrivate();
            
            // state initialization: public and protected properties
            try
                {
                setMessageType(13);
                }
            catch (java.lang.Exception e)
                {
                // re-throw as a runtime exception
                throw new com.tangosol.util.WrapperException(e);
                }
            
            // signal the end of the initialization
            set_Constructed(true);
            }
        
        // Private initializer
        protected void __initPrivate()
            {
            
            super.__initPrivate();
            }
        
        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
        * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService.NewMemberRequestIdWait();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ClusterService$NewMemberRequestIdWait".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }
        
        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
        * time] parent component.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent();
            }
        
        // Declared at the super level
        /**
         * Getter for property Description.<p>
        * Used for debugging purposes (from toString). Create a human-readable
        * description of the specific Message data.
         */
        public String getDescription()
            {
            return "ToMember=" + getToMember();
            }
        
        // Accessor for the property "SeniorMember"
        /**
         * Getter for property SeniorMember.<p>
        * The senior Member that should respond to the requesting member.
         */
        public com.tangosol.coherence.component.net.Member getSeniorMember()
            {
            return __m_SeniorMember;
            }
        
        // Declared at the super level
        /**
         * This is the event that is executed when a Message is received.
        * <p>
        * It is the main processing event of the Message called by the
        * <code>Service.onMessage()</code> event. With regards to the use of
        * Message components within clustered Services, Services are designed
        * by dragging Message components into them as static children. These
        * Messages are the components that a Service can send to other running
        * instances of the same Service within a cluster. When the onReceived
        * event is invoked by a Service, it means that the Message has been
        * received; the code in the onReceived event is therefore the Message
        * specific logic for processing a received Message. For example, when
        * onReceived is invoked on a Message named FindData, the onReceived
        * event should do the work to "find the data", because it is being
        * invoked by the Service that received the "find the data" Message.
         */
        public void onReceived()
            {
            // import Component.Net.Member;
            // import com.tangosol.run.component.EventDeathException;
            
            super.onReceived();
            
            ClusterService service    = (ClusterService) getService();
            Member  memberFrom = getFromMember();
            switch (service.getState())
                {
                case ClusterService.STATE_ANNOUNCE:
                case ClusterService.STATE_JOINING:
                    if (isReadError())
                        {
                        service.onMemberRejected(ClusterService.REJECT_VERSION, memberFrom);
                        throw new EventDeathException("Version mismatch");
                        }
            
                    // COH-4387: the request-id-wait may reflect a different senior member
                    //           (either due to seniority change or possibly out-of-date
                    //           information).  In either case, ensure that the senior member
                    //           is added to the broadcast domain so that it can "take over".
                    //           see ClusterService.NewMemberRequestIdReply.onReceived()
                    service.addDynamicBroadcast(getSeniorMember());
                            
                    // someone is sending back request-id replies; that means a
                    // cluster is running
                    service.resetBroadcastCounter("the presence of an existing cluster", this);
                    break;
                }
            }
        
        // Declared at the super level
        public void read(com.tangosol.io.ReadBuffer.BufferInput input)
                throws java.io.IOException
            {
            // import Component.Net.Member;
            
            super.read(input);
            
            ClusterService service      = (ClusterService) get_Parent();
            Member  memberSenior = service.instantiateMember();
            memberSenior.readExternal(input);
            setSeniorMember(memberSenior);
            
            ensureEOS(input);
            }
        
        // Accessor for the property "SeniorMember"
        /**
         * Setter for property SeniorMember.<p>
        * The senior Member that should respond to the requesting member.
         */
        public void setSeniorMember(com.tangosol.coherence.component.net.Member member)
            {
            __m_SeniorMember = member;
            }
        
        // Declared at the super level
        public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
                throws java.io.IOException
            {
            super.write(output);
            
            getSeniorMember().writeExternal(output);
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService$NewMemberTimestampRequest
    
    /**
     * *** LICENSE ***
     * WARNING: This Message name is obfuscated.
     * 
     * Message:
     *     NewMemberTimestampRequest
     * 
     * Purpose:
     *     This Message is used to determine the total number of Real Time
     * Clients permitted by the license(s) deployed in the cluster.
     * 
     * Attributes:
     *     n/a
     * 
     * Response to:
     *     n/a
     * 
     * Expected responses:
     *     NewMemberTimestampResponse
     * 
     * @since Coherence 3.2
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class NewMemberTimestampRequest
            extends    com.tangosol.coherence.component.net.message.RequestMessage
        {
        // ---- Fields declarations ----
        private static com.tangosol.util.ListMap __mapChildren;
        
        // Static initializer
        static
            {
            __initStatic();
            }
        
        // Default static initializer
        private static void __initStatic()
            {
            // register child classes
            __mapChildren = new com.tangosol.util.ListMap();
            __mapChildren.put("Poll", ClusterService.NewMemberTimestampRequest.Poll.get_CLASS());
            }
        
        // Default constructor
        public NewMemberTimestampRequest()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public NewMemberTimestampRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);
            
            if (fInit)
                {
                __init();
                }
            }
        
        // Main initializer
        public void __init()
            {
            // private initialization
            __initPrivate();
            
            // state initialization: public and protected properties
            try
                {
                setMessageType(51);
                }
            catch (java.lang.Exception e)
                {
                // re-throw as a runtime exception
                throw new com.tangosol.util.WrapperException(e);
                }
            
            // containment initialization: children
            
            // signal the end of the initialization
            set_Constructed(true);
            }
        
        // Private initializer
        protected void __initPrivate()
            {
            
            super.__initPrivate();
            }
        
        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
        * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService.NewMemberTimestampRequest();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ClusterService$NewMemberTimestampRequest".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }
        
        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
        * time] parent component.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent();
            }
        
        //++ getter for autogen property _ChildClasses
        /**
         * This is an auto-generated method that returns the map of design time
        * [static] children.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        protected java.util.Map get_ChildClasses()
            {
            return __mapChildren;
            }
        
        // Declared at the super level
        public com.tangosol.coherence.component.net.Poll ensureRequestPoll()
            {
            // import Component.Net.Poll;
            
            return (Poll) _newChild("Poll");
            }
        
        // Declared at the super level
        /**
         * This is the event that is executed when a Message is received.
        * <p>
        * It is the main processing event of the Message called by the
        * <code>Service.onMessage()</code> event. With regards to the use of
        * Message components within clustered Services, Services are designed
        * by dragging Message components into them as static children. These
        * Messages are the components that a Service can send to other running
        * instances of the same Service within a cluster. When the onReceived
        * event is invoked by a Service, it means that the Message has been
        * received; the code in the onReceived event is therefore the Message
        * specific logic for processing a received Message. For example, when
        * onReceived is invoked on a Message named FindData, the onReceived
        * event should do the work to "find the data", because it is being
        * invoked by the Service that received the "find the data" Message.
         */
        public void onReceived()
            {
            // import java.util.Iterator;
            // import java.util.Map;
            // import java.util.Map$Entry as java.util.Map.Entry;
            
            super.onReceived();
            
            ClusterService service = (ClusterService) getService();
            ClusterService.NewMemberTimestampResponse msg = (ClusterService.NewMemberTimestampResponse)
                    service.instantiateMessage("NewMemberTimestampResponse");
            msg.respondTo(this);
            
            // calculate the total number of licensed clients
            int nTimestamp = Integer.MIN_VALUE;
            
            all:
            for (Iterator iter = service.getWkaMap().entrySet().iterator(); iter.hasNext(); )
                {
                java.util.Map.Entry entry = (java.util.Map.Entry) iter.next();
            
                // Map keyed by UID, with each value an int[] of:
                // [0] edition (0=DC, ..5=GE)
                // [1] limit type (0=site, 1=srv, 2=sock, 3=cpu, 4=seat, 5=user)
                // [2] limit value
                int[] an = (int[]) entry.getValue();
                if (an[0] == 1)
                    {
                    int n = 0; // additional client licenses
                    switch (an[1])
                        {
                        case 0:
                            nTimestamp = 0;
                            break all;
            
                        case 4:
                            n = 2*an[2];
                            break;
            
                        case 5:
                            n = an[2]; 
                            break;
            
                        default:
                            break;
                        }
                    nTimestamp = nTimestamp < 0 ? n : nTimestamp + n;
                    }
                }
            
            msg.setTimestamp(nTimestamp);
            service.send(msg);
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService$NewMemberTimestampRequest$Poll
        
        /**
         * The Poll contains information regarding a request sent to one or
         * more Cluster Members that require responses. A Service may poll
         * other Members that are running the same Service, and the Poll is
         * used to wait for and assemble the responses from each of those
         * Members. A client thread may also use the Poll to block on a
         * response or set of responses, thus waiting for the completion of the
         * Poll. In its simplest form, which is a Poll that is sent to one
         * Member of the Cluster, the Poll actually represents the
         * request/response model.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class Poll
                extends    com.tangosol.coherence.component.net.Poll
            {
            // ---- Fields declarations ----
            
            // Default constructor
            public Poll()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public Poll(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
                {
                super(sName, compParent, false);
                
                if (fInit)
                    {
                    __init();
                    }
                }
            
            // Main initializer
            public void __init()
                {
                // private initialization
                __initPrivate();
                
                
                // signal the end of the initialization
                set_Constructed(true);
                }
            
            // Private initializer
            protected void __initPrivate()
                {
                
                super.__initPrivate();
                }
            
            //++ getter for static property _Instance
            /**
             * Getter for property _Instance.<p>
            * Auto generated
             */
            public static com.tangosol.coherence.Component get_Instance()
                {
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService.NewMemberTimestampRequest.Poll();
                }
            
            //++ getter for static property _CLASS
            /**
             * Getter for property _CLASS.<p>
            * Property with auto-generated accessor that returns the Class
            * object for a given component.
             */
            public static Class get_CLASS()
                {
                Class clz;
                try
                    {
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ClusterService$NewMemberTimestampRequest$Poll".replace('/', '.'));
                    }
                catch (ClassNotFoundException e)
                    {
                    throw new NoClassDefFoundError(e.getMessage());
                    }
                return clz;
                }
            
            //++ getter for autogen property _Module
            /**
             * This is an auto-generated method that returns the global [design
            * time] parent component.
            * 
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            private com.tangosol.coherence.Component get_Module()
                {
                return this.get_Parent().get_Parent();
                }
            
            // Declared at the super level
            /**
             * This is the event that is executed when all the Members that were
            * polled have responded or have left the Service.
             */
            public void onCompletion()
                {
                super.onCompletion();
                }
            
            // Declared at the super level
            /**
             * This event occurs for each response Message from each polled
            * Member.
             */
            public void onResponse(com.tangosol.coherence.component.net.Message msg)
                {
                // import com.tangosol.util.Base;
                
                if (isClosed())
                    {
                    setResult(Base.makeInteger(0));
                    }
                else
                    {
                    ClusterService.NewMemberTimestampResponse msgResponse = (ClusterService.NewMemberTimestampResponse) msg;
                    setResult(Base.makeInteger(msgResponse.getTimestamp()));
                    }
                
                super.onResponse(msg);
                }
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService$NewMemberTimestampResponse
    
    /**
     * *** LICENSE ***
     * WARNING: This Message name is obfuscated.
     * 
     * Message:
     *     NewMemberTimestampResponse
     * 
     * Purpose:
     *     Response to the NewMemberTimestampRequest (poll)
     * 
     * Description:
     *     Informs the requestor of the total number of Real Time Clients
     * permitted by the license(s) deployed in the cluster.
     * 
     * Attributes:
     *     Timestamp (the number of licensed Real Time Clients)
     * 
     * Response to:
     *     NewMemberTimestampRequest
     * 
     * Expected responses:
     *     n/a
     * 
     * @since Coherence 3.2
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class NewMemberTimestampResponse
            extends    com.tangosol.coherence.component.net.Message
        {
        // ---- Fields declarations ----
        
        /**
         * Property Timestamp
         *
         * Rejection flag
         */
        private int __m_Timestamp;
        
        // Default constructor
        public NewMemberTimestampResponse()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public NewMemberTimestampResponse(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);
            
            if (fInit)
                {
                __init();
                }
            }
        
        // Main initializer
        public void __init()
            {
            // private initialization
            __initPrivate();
            
            // state initialization: public and protected properties
            try
                {
                setMessageType(52);
                }
            catch (java.lang.Exception e)
                {
                // re-throw as a runtime exception
                throw new com.tangosol.util.WrapperException(e);
                }
            
            // signal the end of the initialization
            set_Constructed(true);
            }
        
        // Private initializer
        protected void __initPrivate()
            {
            
            super.__initPrivate();
            }
        
        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
        * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService.NewMemberTimestampResponse();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ClusterService$NewMemberTimestampResponse".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }
        
        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
        * time] parent component.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent();
            }
        
        // Declared at the super level
        /**
         * Getter for property Description.<p>
        * Used for debugging purposes (from toString). Create a human-readable
        * description of the specific Message data.
         */
        public String getDescription()
            {
            return "Timestamp=" + getTimestamp();
            }
        
        // Accessor for the property "Timestamp"
        /**
         * Getter for property Timestamp.<p>
        * Rejection flag
         */
        public int getTimestamp()
            {
            return __m_Timestamp;
            }
        
        // Declared at the super level
        public void read(com.tangosol.io.ReadBuffer.BufferInput input)
                throws java.io.IOException
            {
            // import com.tangosol.util.ExternalizableHelper;
            
            setTimestamp(ExternalizableHelper.readInt(input));
            }
        
        // Accessor for the property "Timestamp"
        /**
         * Setter for property Timestamp.<p>
        * Rejection flag
         */
        public void setTimestamp(int n)
            {
            __m_Timestamp = n;
            }
        
        // Declared at the super level
        public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
                throws java.io.IOException
            {
            // import com.tangosol.util.ExternalizableHelper;
            
            ExternalizableHelper.writeInt(output, getTimestamp());
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService$NewMemberWelcome
    
    /**
     * Message:
     *     NewMemberWelcome
     * 
     * Purpose:
     *     Each cluster Member welcomes the new Member by sending its list of
     * Services that it is providing.
     * 
     * Description:
     *     After receiving the NewMemberAcceptIdReply, the new Member has the
     * Member ids and Service data, so it can accept Message from all Members
     * and it can match up Members and Services. This Message comes in to the
     * new Member from each and every cluster Member and provides it with the
     * information to match up Members and Services.
     * 
     * Attributes:
     *     FromMemberUid
     *     ServiceCount
     *     ServiceId[]
     *     ServiceName[]
     *     ServiceType[]
     *     ServiceVersion[]
     *     ServiceEndPoint[] @since 3.7.1
     *     ServiceJoinTime[]
     *     ServiceMembershipState[]
     *     ServiceMemberConfigMap[] @since 3.7.2
     *     PreferredPacketLength @since 3.6.1
     *     PreferredPort @since 3.6.1
     * 
     * Response to:
     *     NewMemberWelcomeRequest
     * 
     * Expected responses:
     *     n/a
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class NewMemberWelcome
            extends    com.tangosol.coherence.component.net.Message
        {
        // ---- Fields declarations ----
        
        /**
         * Property FromMemberUid
         *
         * The sender includes its UID for verification purposes.
         */
        private com.tangosol.util.UUID __m_FromMemberUid;
        
        /**
         * Property PreferredPacketLength
         *
         * The Member's preferred packet length.
         * 
         * @since Coherence 3.6.1
         */
        private int __m_PreferredPacketLength;
        
        /**
         * Property PreferredPort
         *
         * The Member's preferred port.
         * 
         * @since Coherence 3.6.1
         */
        private int __m_PreferredPort;
        
        /**
         * Property ServiceCount
         *
         * The number of Service id/name combinations carried by this Message.
         */
        private int __m_ServiceCount;
        
        /**
         * Property ServiceEndPointName
         *
         * The canonical EndPoint name for the service.
         */
        private String[] __m_ServiceEndPointName;
        
        /**
         * Property ServiceId
         *
         * The name of each Service that the sender is running.
         */
        private int[] __m_ServiceId;
        
        /**
         * Property ServiceJoinTime
         *
         * Corresponding to ServiceId, the cluster time when a sender joined
         * each Service that the sender is running.
         */
        private long[] __m_ServiceJoinTime;
        
        /**
         * Property ServiceMemberConfigMap
         *
         * Corresponding to ServiceId, the member config map for the service at
         * the time this member started the service.
         * 
         * @since Coherence 3.7.2
         */
        private java.util.Map[] __m_ServiceMemberConfigMap;
        
        /**
         * Property ServiceName
         *
         * Corresponding to ServiceId, the name of each Service that the sender
         * is running.
         */
        private String[] __m_ServiceName;
        
        /**
         * Property ServiceState
         *
         * Corresponding to ServiceId, the state of the welcoming member in the
         * specified service.
         * 
         * The membership state is one of the $ServiceMemberSet.MEMBER_*
         * constants
         */
        private int[] __m_ServiceState;
        
        /**
         * Property ServiceType
         *
         * Corresponding to ServiceId, the type of each Service that the sender
         * is running.
         */
        private String[] __m_ServiceType;
        
        /**
         * Property ServiceVersion
         *
         * Corresponding to ServiceId, the version of each Service that the
         * sender is running.
         */
        private String[] __m_ServiceVersion;
        
        // Default constructor
        public NewMemberWelcome()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public NewMemberWelcome(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);
            
            if (fInit)
                {
                __init();
                }
            }
        
        // Main initializer
        public void __init()
            {
            // private initialization
            __initPrivate();
            
            // state initialization: public and protected properties
            try
                {
                setMessageType(37);
                }
            catch (java.lang.Exception e)
                {
                // re-throw as a runtime exception
                throw new com.tangosol.util.WrapperException(e);
                }
            
            // signal the end of the initialization
            set_Constructed(true);
            }
        
        // Private initializer
        protected void __initPrivate()
            {
            
            super.__initPrivate();
            }
        
        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
        * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService.NewMemberWelcome();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ClusterService$NewMemberWelcome".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }
        
        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
        * time] parent component.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent();
            }
        
        // Declared at the super level
        /**
         * Getter for property Description.<p>
        * Used for debugging purposes (from toString). Create a human-readable
        * description of the specific Message data.
         */
        public String getDescription()
            {
            // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet;
            
            StringBuffer sb = new StringBuffer();
            
            int cServices = getServiceCount();
            
            sb.append("FromMemberUid=")
              .append(getFromMemberUid())
              .append(", ServiceCount=")
              .append(cServices)
              .append("\nServiceId/ServiceName/ServiceVersion/ServiceEndPoint/ServiceState=[");
            
            for (int i = 0; i < cServices; ++i)
                {
                if (i > 0)
                    {
                    sb.append(", ");
                    }
                sb.append(getServiceId(i))
                  .append('/')
                  .append(getServiceName(i))
                  .append('/')
                  .append(getServiceVersion(i))
                  .append('/')
                  .append(getServiceEndPointName(i))
                  .append('/')
                  .append(ServiceMemberSet.formatJoinTime(getServiceJoinTime(i)))
                  .append('/')
                  .append(ServiceMemberSet.formatStateName(getServiceState(i)));
                }
            
            sb.append(']');
            
            return sb.toString();
            }
        
        // Accessor for the property "FromMemberUid"
        /**
         * Getter for property FromMemberUid.<p>
        * The sender includes its UID for verification purposes.
         */
        public com.tangosol.util.UUID getFromMemberUid()
            {
            return __m_FromMemberUid;
            }
        
        // Accessor for the property "PreferredPacketLength"
        /**
         * Getter for property PreferredPacketLength.<p>
        * The Member's preferred packet length.
        * 
        * @since Coherence 3.6.1
         */
        public int getPreferredPacketLength()
            {
            return __m_PreferredPacketLength;
            }
        
        // Accessor for the property "PreferredPort"
        /**
         * Getter for property PreferredPort.<p>
        * The Member's preferred port.
        * 
        * @since Coherence 3.6.1
         */
        public int getPreferredPort()
            {
            return __m_PreferredPort;
            }
        
        // Accessor for the property "ServiceCount"
        /**
         * Getter for property ServiceCount.<p>
        * The number of Service id/name combinations carried by this Message.
         */
        public int getServiceCount()
            {
            int[] anId = getServiceId();
            return anId == null ? 0 : anId.length;
            }
        
        // Accessor for the property "ServiceEndPointName"
        /**
         * Getter for property ServiceEndPointName.<p>
        * The canonical EndPoint name for the service.
         */
        public String[] getServiceEndPointName()
            {
            return __m_ServiceEndPointName;
            }
        
        // Accessor for the property "ServiceEndPointName"
        /**
         * Getter for property ServiceEndPointName.<p>
        * The canonical EndPoint name for the service.
         */
        public String getServiceEndPointName(int i)
            {
            return getServiceEndPointName()[i];
            }
        
        // Accessor for the property "ServiceId"
        /**
         * Getter for property ServiceId.<p>
        * The name of each Service that the sender is running.
         */
        public int[] getServiceId()
            {
            return __m_ServiceId;
            }
        
        // Accessor for the property "ServiceId"
        /**
         * Getter for property ServiceId.<p>
        * The name of each Service that the sender is running.
         */
        public int getServiceId(int i)
            {
            return getServiceId()[i];
            }
        
        // Accessor for the property "ServiceJoinTime"
        /**
         * Getter for property ServiceJoinTime.<p>
        * Corresponding to ServiceId, the cluster time when a sender joined
        * each Service that the sender is running.
         */
        public long[] getServiceJoinTime()
            {
            return __m_ServiceJoinTime;
            }
        
        // Accessor for the property "ServiceJoinTime"
        /**
         * Getter for property ServiceJoinTime.<p>
        * Corresponding to ServiceId, the cluster time when a sender joined
        * each Service that the sender is running.
         */
        public long getServiceJoinTime(int i)
            {
            return getServiceJoinTime()[i];
            }
        
        // Accessor for the property "ServiceMemberConfigMap"
        /**
         * Getter for property ServiceMemberConfigMap.<p>
        * Corresponding to ServiceId, the member config map for the service at
        * the time this member started the service.
        * 
        * @since Coherence 3.7.2
         */
        public java.util.Map[] getServiceMemberConfigMap()
            {
            return __m_ServiceMemberConfigMap;
            }
        
        // Accessor for the property "ServiceMemberConfigMap"
        /**
         * Getter for property ServiceMemberConfigMap.<p>
        * Corresponding to ServiceId, the member config map for the service at
        * the time this member started the service.
        * 
        * @since Coherence 3.7.2
         */
        public java.util.Map getServiceMemberConfigMap(int i)
            {
            return getServiceMemberConfigMap()[i];
            }
        
        // Accessor for the property "ServiceName"
        /**
         * Getter for property ServiceName.<p>
        * Corresponding to ServiceId, the name of each Service that the sender
        * is running.
         */
        public String[] getServiceName()
            {
            return __m_ServiceName;
            }
        
        // Accessor for the property "ServiceName"
        /**
         * Getter for property ServiceName.<p>
        * Corresponding to ServiceId, the name of each Service that the sender
        * is running.
         */
        public String getServiceName(int i)
            {
            return getServiceName()[i];
            }
        
        // Accessor for the property "ServiceState"
        /**
         * Getter for property ServiceState.<p>
        * Corresponding to ServiceId, the state of the welcoming member in the
        * specified service.
        * 
        * The membership state is one of the $ServiceMemberSet.MEMBER_*
        * constants
         */
        public int[] getServiceState()
            {
            return __m_ServiceState;
            }
        
        // Accessor for the property "ServiceState"
        /**
         * Getter for property ServiceState.<p>
        * Corresponding to ServiceId, the state of the welcoming member in the
        * specified service.
        * 
        * The membership state is one of the $ServiceMemberSet.MEMBER_*
        * constants
         */
        public int getServiceState(int i)
            {
            return getServiceState()[i];
            }
        
        // Accessor for the property "ServiceType"
        /**
         * Getter for property ServiceType.<p>
        * Corresponding to ServiceId, the type of each Service that the sender
        * is running.
         */
        public String[] getServiceType()
            {
            return __m_ServiceType;
            }
        
        // Accessor for the property "ServiceType"
        /**
         * Getter for property ServiceType.<p>
        * Corresponding to ServiceId, the type of each Service that the sender
        * is running.
         */
        public String getServiceType(int i)
            {
            return getServiceType()[i];
            }
        
        // Accessor for the property "ServiceVersion"
        /**
         * Getter for property ServiceVersion.<p>
        * Corresponding to ServiceId, the version of each Service that the
        * sender is running.
         */
        public String[] getServiceVersion()
            {
            return __m_ServiceVersion;
            }
        
        // Accessor for the property "ServiceVersion"
        /**
         * Getter for property ServiceVersion.<p>
        * Corresponding to ServiceId, the version of each Service that the
        * sender is running.
         */
        public String getServiceVersion(int i)
            {
            return getServiceVersion()[i];
            }
        
        // Declared at the super level
        /**
         * This is the event that is executed when a Message is received.
        * <p>
        * It is the main processing event of the Message called by the
        * <code>Service.onMessage()</code> event. With regards to the use of
        * Message components within clustered Services, Services are designed
        * by dragging Message components into them as static children. These
        * Messages are the components that a Service can send to other running
        * instances of the same Service within a cluster. When the onReceived
        * event is invoked by a Service, it means that the Message has been
        * received; the code in the onReceived event is therefore the Message
        * specific logic for processing a received Message. For example, when
        * onReceived is invoked on a Message named FindData, the onReceived
        * event should do the work to "find the data", because it is being
        * invoked by the Service that received the "find the data" Message.
         */
        public void onReceived()
            {
            // import Component.Net.Cluster;
            // import Component.Net.Member;
            // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet;
            // import Component.Net.ServiceInfo;
            // import java.util.Map;
            
            super.onReceived();
            
            ClusterService          service    = (ClusterService) getService();
            ServiceMemberSet setMembers = service.getClusterMemberSet();
            Member           member     = getFromMember();
            int              nMemberId  = member.getId();
            
            _assert(getFromMemberUid().equals(member.getUid32()));
            
            member.setPreferredPort(getPreferredPort());
            member.setPreferredPacketLength(getPreferredPacketLength());
            
            int cServices = getServiceCount();
            if (cServices > 0)
                {
                int[]    anServiceId       = getServiceId();
                String[] asServiceName     = getServiceName();
                String[] asServiceType     = getServiceType();
                String[] asServiceVersion  = getServiceVersion();
                String[] asServiceEndPoint = getServiceEndPointName();
                long[]   alServiceJoined   = getServiceJoinTime();
                int[]    anServiceState    = getServiceState();
                Map[]    aMapServiceConfig = getServiceMemberConfigMap();
            
                // first pass: update the service member set, at the same time
                // calculating the senior (see ServiceMemberSet#setServiceJoinTime)
                ServiceInfo[] ainfo = new ServiceInfo[cServices];
                for (int i = 0; i < cServices; ++i)
                    {
                    ServiceInfo info = service.ensureServiceInfo(
                            anServiceId[i], asServiceName[i], asServiceType[i]);
                    ServiceMemberSet setMember = info.getMemberSet();
            
                    // update ServiceMemberSet atomically
                    synchronized (setMember)
                        {
                        if (setMember.add(member))
                            {
                            setMember.setServiceJoinTime(nMemberId, alServiceJoined [i]);
                            setMember.setServiceEndPointName(nMemberId, asServiceEndPoint[i]);
                            setMember.updateMemberConfigMap(nMemberId, aMapServiceConfig[i]);
                            }
            
                        // as of Coherence 12.2.1.1.0, the real ClusterService version is only
                        // sent during the NewMemberWelcomeRequest-NewMemberWelcome exchange
            
                        String sVersion = asServiceVersion[i];
                        setMember.setServiceVersion(nMemberId, sVersion);                    
            
                        if (anServiceId[i] == 0 && service.isVersionOlder(sVersion))
                            {
                            _trace("Cluster member " + nMemberId +
                                   " is running an older Coherence version " + setMembers.getServiceVersionExternal(nMemberId), 2);
                            }
                        }
            
                    ainfo[i] = info;
                    }
            
                // second pass: notify the service
                // this is going to be basically a no-op for all services except service 0
                // since none of those services are running yet on this node
                for (int i = 0; i < cServices; ++i)
                    {
                    ServiceInfo      info          = ainfo[i];
                    ServiceMemberSet setMember     = info.getMemberSet();
                    int              nServiceState = anServiceState[i];
            
                    switch (setMember.getState(nMemberId))
                        {
                        default:
                            if (nServiceState >= ServiceMemberSet.MEMBER_JOINING)
                                {
                                setMember.setServiceJoining(nMemberId);
                                service.onServiceJoining(info, member);
                                }
                            // fall through
                        case ServiceMemberSet.MEMBER_JOINING:
                            if (nServiceState >= ServiceMemberSet.MEMBER_JOINED)
                                {
                                setMember.setServiceJoined(nMemberId);
                                service.onServiceJoined(info, member);
                                }
                            // fall through
                        case ServiceMemberSet.MEMBER_JOINED:
                            if (nServiceState >= ServiceMemberSet.MEMBER_LEAVING)
                                {
                                setMember.setServiceLeaving(nMemberId);
                                service.onServiceLeaving(info, member);
                                }
                        }
                    }
                }
            }
        
        // Declared at the super level
        public void read(com.tangosol.io.ReadBuffer.BufferInput input)
                throws java.io.IOException
            {
            // import com.tangosol.util.ExternalizableHelper as com.tangosol.util.ExternalizableHelper;
            // import com.tangosol.util.UUID;
            // import java.io.IOException;
            // import java.util.HashMap;
            // import java.util.Map;
            
            setFromMemberUid(new UUID(input));
            
            int cServices = input.readUnsignedShort();
            if (cServices > 0)
                {
                int[]    anServiceId       = new int   [cServices];
                String[] asServiceName     = new String[cServices];
                String[] asServiceType     = new String[cServices];
                String[] asServiceVersion  = new String[cServices];
                String[] asServiceEndPoint = new String[cServices];
                long[]   alServiceJoinTime = new long  [cServices];
                int[]    anServiceState    = new int   [cServices];
                Map[]    aMapServiceConfig = new Map   [cServices];
                
                for (int i = 0; i < cServices; ++i)
                    {
                    anServiceId      [i] = input.readUnsignedShort();
                    asServiceName    [i] = input.readUTF();
                    asServiceType    [i] = input.readUTF();
                    asServiceVersion [i] = input.readUTF();
                    asServiceEndPoint[i] = input.readUTF();
                    alServiceJoinTime[i] = input.readLong();
                    anServiceState   [i] = input.readInt();
                    com.tangosol.util.ExternalizableHelper.readMap(input, aMapServiceConfig[i] = new HashMap(), null);
                    }
                setServiceId(anServiceId);
                setServiceName(asServiceName);
                setServiceType(asServiceType);
                setServiceVersion(asServiceVersion);
                setServiceEndPointName(asServiceEndPoint);
                setServiceJoinTime(alServiceJoinTime);
                setServiceState(anServiceState);
                setServiceMemberConfigMap(aMapServiceConfig);
                }
            
            setPreferredPacketLength(input.readInt());
            setPreferredPort(input.readInt());
            }
        
        // Accessor for the property "FromMemberUid"
        /**
         * Setter for property FromMemberUid.<p>
        * The sender includes its UID for verification purposes.
         */
        public void setFromMemberUid(com.tangosol.util.UUID uid)
            {
            __m_FromMemberUid = uid;
            }
        
        // Accessor for the property "PreferredPacketLength"
        /**
         * Setter for property PreferredPacketLength.<p>
        * The Member's preferred packet length.
        * 
        * @since Coherence 3.6.1
         */
        public void setPreferredPacketLength(int cb)
            {
            __m_PreferredPacketLength = cb;
            }
        
        // Accessor for the property "PreferredPort"
        /**
         * Setter for property PreferredPort.<p>
        * The Member's preferred port.
        * 
        * @since Coherence 3.6.1
         */
        public void setPreferredPort(int nPort)
            {
            __m_PreferredPort = nPort;
            }
        
        // Accessor for the property "ServiceCount"
        /**
         * Setter for property ServiceCount.<p>
        * The number of Service id/name combinations carried by this Message.
         */
        public void setServiceCount(int c)
            {
            // import java.util.Map;
            
            int[] anOldId = getServiceId();
            int   cOldIds = (anOldId == null ? 0 : anOldId.length);
            if (c != cOldIds)
                {
                int[] anNewId = new int[c];
                if (cOldIds > 0)
                    {
                    System.arraycopy(anOldId, 0, anNewId, 0, Math.min(c, cOldIds));
                    }
                setServiceId(anNewId);
                }
            
            String[] asOldName = getServiceName();
            int   cOldNames = (asOldName == null ? 0 : asOldName.length);
            if (c != cOldNames)
                {
                String[] asNewName = new String[c];
                if (cOldNames > 0)
                    {
                    System.arraycopy(asOldName, 0, asNewName, 0, Math.min(c, cOldNames));
                    }
                setServiceName(asNewName);
                }
            
            String[] asOldType = getServiceType();
            int   cOldTypes = (asOldType == null ? 0 : asOldType.length);
            if (c != cOldTypes)
                {
                String[] asNewType = new String[c];
                if (cOldTypes > 0)
                    {
                    System.arraycopy(asOldType, 0, asNewType, 0, Math.min(c, cOldTypes));
                    }
                setServiceType(asNewType);
                }
            
            String[] asOldVersion = getServiceVersion();
            int   cOldVersions = (asOldVersion == null ? 0 : asOldVersion.length);
            if (c != cOldVersions)
                {
                String[] asNewVersion = new String[c];
                if (cOldVersions > 0)
                    {
                    System.arraycopy(asOldVersion, 0, asNewVersion, 0, Math.min(c, cOldVersions));
                    }
                setServiceVersion(asNewVersion);
                }
            
            String[] asOldEndPoint = getServiceEndPointName();
            int   cOldEndPoints = (asOldEndPoint == null ? 0 : asOldEndPoint.length);
            if (c != cOldEndPoints)
                {
                String[] asNewEndPoint = new String[c];
                if (cOldEndPoints > 0)
                    {
                    System.arraycopy(asOldEndPoint, 0, asNewEndPoint, 0, Math.min(c, cOldEndPoints));
                    }
                setServiceEndPointName(asNewEndPoint);
                }
            
            long[] alOldJoined = getServiceJoinTime();
            int    cOldJoined  = (alOldJoined == null ? 0 : alOldJoined.length);
            if (c != cOldJoined)
                {
                long[] alNewJoined = new long[c];
                if (cOldJoined > 0)
                    {
                    System.arraycopy(alOldJoined, 0, alNewJoined, 0, Math.min(c, cOldJoined));
                    }
                setServiceJoinTime(alNewJoined);
                }
            
            int[] anOldState = getServiceState();
            int   cOldState  = (anOldState == null ? 0 : anOldState.length);
            if (c != cOldState)
                {
                int[] anNewState = new int[c];
                if (cOldState > 0)
                    {
                    System.arraycopy(anOldState, 0, anNewState, 0, Math.min(c, cOldState));
                    }
                setServiceState(anNewState);
                }
            
            Map[] aMapOld = getServiceMemberConfigMap();
            int   cOldMap = (aMapOld == null ? 0 : aMapOld.length);
            if (c != cOldMap)
                {
                Map[] aMapNew = new Map[c];
                if (cOldMap > 0)
                    {
                    System.arraycopy(aMapOld, 0, aMapNew, 0, Math.min(c, cOldMap));
                    }
                setServiceMemberConfigMap(aMapNew);
                }
            }
        
        // Accessor for the property "ServiceEndPointName"
        /**
         * Setter for property ServiceEndPointName.<p>
        * The canonical EndPoint name for the service.
         */
        public void setServiceEndPointName(String[] asName)
            {
            __m_ServiceEndPointName = asName;
            }
        
        // Accessor for the property "ServiceEndPointName"
        /**
         * Setter for property ServiceEndPointName.<p>
        * The canonical EndPoint name for the service.
         */
        public void setServiceEndPointName(int i, String sName)
            {
            getServiceEndPointName()[i] = sName;
            }
        
        // Accessor for the property "ServiceId"
        /**
         * Setter for property ServiceId.<p>
        * The name of each Service that the sender is running.
         */
        public void setServiceId(int[] anId)
            {
            __m_ServiceId = anId;
            }
        
        // Accessor for the property "ServiceId"
        /**
         * Setter for property ServiceId.<p>
        * The name of each Service that the sender is running.
         */
        public void setServiceId(int i, int nId)
            {
            getServiceId()[i] = nId;
            }
        
        // Accessor for the property "ServiceJoinTime"
        /**
         * Setter for property ServiceJoinTime.<p>
        * Corresponding to ServiceId, the cluster time when a sender joined
        * each Service that the sender is running.
         */
        public void setServiceJoinTime(long[] alJoined)
            {
            __m_ServiceJoinTime = alJoined;
            }
        
        // Accessor for the property "ServiceJoinTime"
        /**
         * Setter for property ServiceJoinTime.<p>
        * Corresponding to ServiceId, the cluster time when a sender joined
        * each Service that the sender is running.
         */
        public void setServiceJoinTime(int i, long lJoined)
            {
            getServiceJoinTime()[i] = lJoined;
            }
        
        // Accessor for the property "ServiceMemberConfigMap"
        /**
         * Setter for property ServiceMemberConfigMap.<p>
        * Corresponding to ServiceId, the member config map for the service at
        * the time this member started the service.
        * 
        * @since Coherence 3.7.2
         */
        public void setServiceMemberConfigMap(java.util.Map[] amapConfig)
            {
            __m_ServiceMemberConfigMap = amapConfig;
            }
        
        // Accessor for the property "ServiceMemberConfigMap"
        /**
         * Setter for property ServiceMemberConfigMap.<p>
        * Corresponding to ServiceId, the member config map for the service at
        * the time this member started the service.
        * 
        * @since Coherence 3.7.2
         */
        public void setServiceMemberConfigMap(int i, java.util.Map mapConfig)
            {
            getServiceMemberConfigMap()[i] = mapConfig;
            }
        
        // Accessor for the property "ServiceName"
        /**
         * Setter for property ServiceName.<p>
        * Corresponding to ServiceId, the name of each Service that the sender
        * is running.
         */
        public void setServiceName(String[] asName)
            {
            __m_ServiceName = asName;
            }
        
        // Accessor for the property "ServiceName"
        /**
         * Setter for property ServiceName.<p>
        * Corresponding to ServiceId, the name of each Service that the sender
        * is running.
         */
        public void setServiceName(int i, String sName)
            {
            getServiceName()[i] = sName;
            }
        
        // Accessor for the property "ServiceState"
        /**
         * Setter for property ServiceState.<p>
        * Corresponding to ServiceId, the state of the welcoming member in the
        * specified service.
        * 
        * The membership state is one of the $ServiceMemberSet.MEMBER_*
        * constants
         */
        public void setServiceState(int[] anState)
            {
            __m_ServiceState = anState;
            }
        
        // Accessor for the property "ServiceState"
        /**
         * Setter for property ServiceState.<p>
        * Corresponding to ServiceId, the state of the welcoming member in the
        * specified service.
        * 
        * The membership state is one of the $ServiceMemberSet.MEMBER_*
        * constants
         */
        public void setServiceState(int i, int nState)
            {
            getServiceState()[i] = nState;
            }
        
        // Accessor for the property "ServiceType"
        /**
         * Setter for property ServiceType.<p>
        * Corresponding to ServiceId, the type of each Service that the sender
        * is running.
         */
        public void setServiceType(String[] asType)
            {
            __m_ServiceType = asType;
            }
        
        // Accessor for the property "ServiceType"
        /**
         * Setter for property ServiceType.<p>
        * Corresponding to ServiceId, the type of each Service that the sender
        * is running.
         */
        public void setServiceType(int i, String sType)
            {
            getServiceType()[i] = sType;
            }
        
        // Accessor for the property "ServiceVersion"
        /**
         * Setter for property ServiceVersion.<p>
        * Corresponding to ServiceId, the version of each Service that the
        * sender is running.
         */
        public void setServiceVersion(String[] aVersion)
            {
            __m_ServiceVersion = aVersion;
            }
        
        // Accessor for the property "ServiceVersion"
        /**
         * Setter for property ServiceVersion.<p>
        * Corresponding to ServiceId, the version of each Service that the
        * sender is running.
         */
        public void setServiceVersion(int i, String version)
            {
            getServiceVersion()[i] = version;
            }
        
        // Declared at the super level
        public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
                throws java.io.IOException
            {
            // import com.tangosol.util.ExternalizableHelper as com.tangosol.util.ExternalizableHelper;
            // import java.util.Map;
            
            getFromMemberUid().writeExternal(output);
            
            int cServices = getServiceCount();
            output.writeShort(cServices);
            if (cServices > 0)
                {
                int[]    anServiceId       = getServiceId();
                String[] asServiceName     = getServiceName();
                String[] asServiceType     = getServiceType();
                String[] asServiceVersion  = getServiceVersion();
                String[] asServiceEndPoint = getServiceEndPointName();
                long[]   alServiceJoinTime = getServiceJoinTime();
                int[]    anServiceState    = getServiceState();
                Map[]    aMapMemberConfig  = getServiceMemberConfigMap();
                
                for (int i = 0; i < cServices; ++i)
                    {
                    output.writeShort(anServiceId[i]);
                    output.writeUTF  (asServiceName[i]);
                    output.writeUTF  (asServiceType[i]);
                    output.writeUTF  (asServiceVersion[i]);
                    output.writeUTF  (asServiceEndPoint[i]);
                    output.writeLong (alServiceJoinTime[i]);
                    output.writeInt  (anServiceState[i]);
                    com.tangosol.util.ExternalizableHelper.writeMap      (output, aMapMemberConfig[i]);
                    }
                }
            
            output.writeInt(getPreferredPacketLength());
            output.writeInt(getPreferredPort());
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService$NewMemberWelcomeAnnounce
    
    /**
     * DiscoveryMessage (broadcast):
     *     NewMemberWelcomeAnnounce
     * 
     * Purpose:
     *     Broadcasts the presence of a new Member (with id)
     * 
     * Description:
     *     This Message is broadcast by a new Member until it has closed its
     * NewMemberWelcomeRequest Poll. Its purpose is to provide the other
     * cluster Members with the knowledge of this Member and its id just in
     * case the senior Member dies before ensuring the delivery of the "new
     * member" information. Senior Member info is included as a "proof".
     * 
     * Attributes:
     *     SeniorMember
     *     ServiceVersion
     * 
     * Response to:
     *     n/a
     * 
     * Expected responses:
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class NewMemberWelcomeAnnounce
            extends    com.tangosol.coherence.component.net.message.DiscoveryMessage
        {
        // ---- Fields declarations ----
        
        /**
         * Property SeniorMember
         *
         * The senior Member that assigned the ID to the new Member.
         */
        private com.tangosol.coherence.component.net.Member __m_SeniorMember;
        
        /**
         * Property ServiceVersion
         *
         * The version of the ClusterService that the new Member is running.
         */
        private String __m_ServiceVersion;
        
        // Default constructor
        public NewMemberWelcomeAnnounce()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public NewMemberWelcomeAnnounce(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);
            
            if (fInit)
                {
                __init();
                }
            }
        
        // Main initializer
        public void __init()
            {
            // private initialization
            __initPrivate();
            
            // state initialization: public and protected properties
            try
                {
                setMessageType(38);
                }
            catch (java.lang.Exception e)
                {
                // re-throw as a runtime exception
                throw new com.tangosol.util.WrapperException(e);
                }
            
            // signal the end of the initialization
            set_Constructed(true);
            }
        
        // Private initializer
        protected void __initPrivate()
            {
            
            super.__initPrivate();
            }
        
        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
        * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService.NewMemberWelcomeAnnounce();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ClusterService$NewMemberWelcomeAnnounce".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }
        
        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
        * time] parent component.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent();
            }
        
        // Declared at the super level
        /**
         * Getter for property Description.<p>
        * Used for debugging purposes (from toString). Create a human-readable
        * description of the specific Message data.
         */
        public String getDescription()
            {
            return "SeniorMember="      + getSeniorMember()
                 + "\nServiceVersion="  + getServiceVersion();
            }
        
        // Accessor for the property "SeniorMember"
        /**
         * Getter for property SeniorMember.<p>
        * The senior Member that assigned the ID to the new Member.
         */
        public com.tangosol.coherence.component.net.Member getSeniorMember()
            {
            return __m_SeniorMember;
            }
        
        // Accessor for the property "ServiceVersion"
        /**
         * Getter for property ServiceVersion.<p>
        * The version of the ClusterService that the new Member is running.
         */
        public String getServiceVersion()
            {
            return __m_ServiceVersion;
            }
        
        // Declared at the super level
        /**
         * This is the event that is executed when a Message is received.
        * <p>
        * It is the main processing event of the Message called by the
        * <code>Service.onMessage()</code> event. With regards to the use of
        * Message components within clustered Services, Services are designed
        * by dragging Message components into them as static children. These
        * Messages are the components that a Service can send to other running
        * instances of the same Service within a cluster. When the onReceived
        * event is invoked by a Service, it means that the Message has been
        * received; the code in the onReceived event is therefore the Message
        * specific logic for processing a received Message. For example, when
        * onReceived is invoked on a Message named FindData, the onReceived
        * event should do the work to "find the data", because it is being
        * invoked by the Service that received the "find the data" Message.
         */
        public void onReceived()
            {
            // import Component.Net.Member;
            // import Component.Net.MemberSet;
            // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet.MasterMemberSet;
            // import com.tangosol.util.UUID;
            
            super.onReceived();
            
            ClusterService service = (ClusterService) getService();
            if (!service.isRunning() || isReadError())
                {
                return;
                }
            
            Member memberFrom = getFromMember();
            Member memberThis;
            
            switch (service.getState())
                {
                case ClusterService.STATE_ANNOUNCE:
                case ClusterService.STATE_JOINING:
                    // we don't know what's going on, but obviously there's
                    // a cluster running and we need to be patient
                    memberThis = service.getState() == ClusterService.STATE_ANNOUNCE
                            ? service.getAnnounceMember()
                            : service.getRequestMember();
                    if (!memberThis.equals(memberFrom))
                        {
                        service.resetBroadcastCounter("the presence of an existing cluster", this);
                        }
                    break;
            
                case ClusterService.STATE_JOINED:
                    // verify that it is not an "own Message"
                    memberThis = service.getThisMember();
                    if (!memberThis.equals(memberFrom))
                        {
                        // get the info about the new member that has joined
                        int  nMember   = memberFrom.getId();
                        UUID uidMember = memberFrom.getUid32();
            
                        // verify that the Member is not already registered
                        MasterMemberSet setMember   = service.getClusterMemberSet();
                        MemberSet       setRecycled = setMember.getRecycleSet();
            
                        Member memberById  = setMember.getMember(nMember);
                        Member memberByUid = setMember.getMember(uidMember);
                        if (memberById == null && memberByUid == null)
                            {
                            // verify the senior member information
                            Member memberSenior = getSeniorMember();
                            if (   memberSenior.equals(setMember.getOldestMember())
                                || memberSenior.equals(setRecycled.getMember(memberSenior.getId())))
                                {
                                // register the new Member
                                service.ensureMember(memberFrom, getServiceVersion());
            
                                // if this Member is now the senior Member, then this
                                // Member will send out the SeniorHeartbeat broadcasting
                                // all joined members thus preventing two problems
                                // from occuring:
                                // 1) the new senior Member has given out
                                //    ids to other new Members but hasn't yet
                                //    informed the new Member because the
                                //    new Member was not known, and
                                // 2) the new senior Member has knowledge
                                //    of Members that have died but the
                                //    information may not yet have percolated
                                //    to the new Member
                                }
                            }
                        else if (memberById != memberByUid)
                            {
                            // this is a problem but the panic protocol will handle it
                            }
                        }
                    break;
            
                default:
                    // shutting down; ignore it
                    break;
                }
            }
        
        // Declared at the super level
        public void read(com.tangosol.io.ReadBuffer.BufferInput input)
                throws java.io.IOException
            {
            // import Component.Net.Member;
            
            super.read(input);
            
            ClusterService service      = (ClusterService) getService();
            Member  memberSenior = service.instantiateMember();
            memberSenior.readExternal(input);
            setSeniorMember(memberSenior);
            setServiceVersion(input.readUTF());
            getFromMember().setTcpRingPort(input.readInt());
            }
        
        // Accessor for the property "SeniorMember"
        /**
         * Setter for property SeniorMember.<p>
        * The senior Member that assigned the ID to the new Member.
         */
        public void setSeniorMember(com.tangosol.coherence.component.net.Member member)
            {
            __m_SeniorMember = member;
            }
        
        // Accessor for the property "ServiceVersion"
        /**
         * Setter for property ServiceVersion.<p>
        * The version of the ClusterService that the new Member is running.
         */
        public void setServiceVersion(String sVersion)
            {
            __m_ServiceVersion = sVersion;
            }
        
        // Declared at the super level
        public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
                throws java.io.IOException
            {
            super.write(output);
            
            getSeniorMember().writeExternal(output);
            output.writeUTF(getServiceVersion());
            output.writeInt(getFromMember().getTcpRingPort());
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService$NewMemberWelcomeRequest
    
    /**
     * Message:
     *     NewMemberWelcomeRequest
     * 
     * History:
     *     This message was added after the 1.1 release to take advantage of
     * the new Poll implementation and to solve a problem with Service startup.
     * Prior to this Message, the ClusterService would "finish starting" as
     * soon as it had a Member Id and the list of other Members and Services.
     * Using the Poll implementation, it is now possible for the ClusterService
     * to wait until it has been welcomed by all other Members.
     * 
     * Purpose:
     *     The new Member requests a welcome from each other Member.
     * 
     * Description:
     *     After receiving the NewMemberRequestIdReply, the new Member has the
     * Member ids and Service data, so it can accept Message from all Members
     * and it can match up Members and Services. This Message requests the
     * NewMemberWelcome message that will provide the match-up information of
     * Members and Services.
     * 
     * Attributes:
     *     PreferredPacketLength @since 3.6.1
     *     PreferredPort @since 3.6.1
     *     ServiceVersion @since 12.2.1.1.0
     * 
     * Response to:
     *     NewMemberRequestIdReply (from senior Member)
     * 
     * Expected responses:
     *     NewMemberWelcome
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class NewMemberWelcomeRequest
            extends    com.tangosol.coherence.component.net.message.RequestMessage
        {
        // ---- Fields declarations ----
        
        /**
         * Property PreferredPacketLength
         *
         * This Member's preferred packet length.
         * 
         * @since Coherence 3.6.1
         */
        private int __m_PreferredPacketLength;
        
        /**
         * Property PreferredPort
         *
         * The Member's preferred port.
         * 
         * @since Coherence 3.6.1
         */
        private int __m_PreferredPort;
        
        /**
         * Property ServiceVersion
         *
         * Starting with 12.2.1.1.0, during the broadcast part of the discovery
         * protocol the members always pretend to be of 12.2.1. The real
         * version is only sent during the
         * NewMemberWelcomeRequest-NewMemberWelcome exchange.
         */
        private String __m_ServiceVersion;
        private static com.tangosol.util.ListMap __mapChildren;
        
        // Static initializer
        static
            {
            __initStatic();
            }
        
        // Default static initializer
        private static void __initStatic()
            {
            // register child classes
            __mapChildren = new com.tangosol.util.ListMap();
            __mapChildren.put("Poll", ClusterService.NewMemberWelcomeRequest.Poll.get_CLASS());
            }
        
        // Default constructor
        public NewMemberWelcomeRequest()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public NewMemberWelcomeRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);
            
            if (fInit)
                {
                __init();
                }
            }
        
        // Main initializer
        public void __init()
            {
            // private initialization
            __initPrivate();
            
            // state initialization: public and protected properties
            try
                {
                setMessageType(39);
                }
            catch (java.lang.Exception e)
                {
                // re-throw as a runtime exception
                throw new com.tangosol.util.WrapperException(e);
                }
            
            // containment initialization: children
            
            // signal the end of the initialization
            set_Constructed(true);
            }
        
        // Private initializer
        protected void __initPrivate()
            {
            
            super.__initPrivate();
            }
        
        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
        * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService.NewMemberWelcomeRequest();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ClusterService$NewMemberWelcomeRequest".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }
        
        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
        * time] parent component.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent();
            }
        
        //++ getter for autogen property _ChildClasses
        /**
         * This is an auto-generated method that returns the map of design time
        * [static] children.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        protected java.util.Map get_ChildClasses()
            {
            return __mapChildren;
            }
        
        // Accessor for the property "PreferredPacketLength"
        /**
         * Getter for property PreferredPacketLength.<p>
        * This Member's preferred packet length.
        * 
        * @since Coherence 3.6.1
         */
        public int getPreferredPacketLength()
            {
            return __m_PreferredPacketLength;
            }
        
        // Accessor for the property "PreferredPort"
        /**
         * Getter for property PreferredPort.<p>
        * The Member's preferred port.
        * 
        * @since Coherence 3.6.1
         */
        public int getPreferredPort()
            {
            return __m_PreferredPort;
            }
        
        // Accessor for the property "ServiceVersion"
        /**
         * Getter for property ServiceVersion.<p>
        * Starting with 12.2.1.1.0, during the broadcast part of the discovery
        * protocol the members always pretend to be of 12.2.1. The real version
        * is only sent during the NewMemberWelcomeRequest-NewMemberWelcome
        * exchange.
         */
        public String getServiceVersion()
            {
            return __m_ServiceVersion;
            }
        
        // Declared at the super level
        /**
         * This is the event that is executed when a Message is received.
        * <p>
        * It is the main processing event of the Message called by the
        * <code>Service.onMessage()</code> event. With regards to the use of
        * Message components within clustered Services, Services are designed
        * by dragging Message components into them as static children. These
        * Messages are the components that a Service can send to other running
        * instances of the same Service within a cluster. When the onReceived
        * event is invoked by a Service, it means that the Message has been
        * received; the code in the onReceived event is therefore the Message
        * specific logic for processing a received Message. For example, when
        * onReceived is invoked on a Message named FindData, the onReceived
        * event should do the work to "find the data", because it is being
        * invoked by the Service that received the "find the data" Message.
         */
        public void onReceived()
            {
            // import Component.Net.Cluster;
            // import Component.Net.Member;
            // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet;
            
            super.onReceived();
            
            ClusterService service = (ClusterService) getService();
            ServiceMemberSet setMembers = service.getClusterMemberSet();
            Member           member     = getFromMember();
            int              nMemberId  = member.getId();
            
            member.setPreferredPort(getPreferredPort());
            member.setPreferredPacketLength(getPreferredPacketLength());
            
            String sVersion = getServiceVersion();
            if (sVersion == null)
                {
                // the ServiceVersion property was added in 12.2.1.1.0;
                // getting here means we are communicating with a 12.2.1.0.x node
            
                sVersion = ClusterService.VERSION_BARRIER;
                }
            else
                {
                setMembers.setServiceVersion(nMemberId, sVersion);
                }
            
            if (service.isVersionOlder(sVersion))
                {
                _trace("Cluster member " + nMemberId +
                       " is running an older Coherence version " + setMembers.getServiceVersionExternal(nMemberId), 2);
                }
            
            // send "welcome" Message
            ClusterService.NewMemberWelcome msg = (ClusterService.NewMemberWelcome)
                    service.instantiateMessage("NewMemberWelcome");
            msg.respondTo(this);
            service.populateWelcomeMessage(msg);
            service.send(msg);
            
            // NewMemberWelcomeRequest marks the end of the join-protocol when
            // the new cluster member handshakes with each other cluster member.
            //
            // Mark the new member as "joined" in the cluster service member set
            setMembers.setServiceJoined(nMemberId);
            }
        
        // Declared at the super level
        public void read(com.tangosol.io.ReadBuffer.BufferInput input)
                throws java.io.IOException
            {
            // import java.io.IOException;
            
            super.read(input);
            
            setPreferredPacketLength(input.readInt());
            setPreferredPort(input.readInt());
            
            try
                {
                setServiceVersion(input.readUTF());
                }
            catch (IOException e)
                {
                // the ServiceVersion property was added in 12.2.1.1.0;
                // getting here means we are communicating with a 12.2.1.0.x node
                }
            }
        
        // Accessor for the property "PreferredPacketLength"
        /**
         * Setter for property PreferredPacketLength.<p>
        * This Member's preferred packet length.
        * 
        * @since Coherence 3.6.1
         */
        public void setPreferredPacketLength(int cb)
            {
            __m_PreferredPacketLength = cb;
            }
        
        // Accessor for the property "PreferredPort"
        /**
         * Setter for property PreferredPort.<p>
        * The Member's preferred port.
        * 
        * @since Coherence 3.6.1
         */
        public void setPreferredPort(int nPort)
            {
            __m_PreferredPort = nPort;
            }
        
        // Accessor for the property "ServiceVersion"
        /**
         * Setter for property ServiceVersion.<p>
        * Starting with 12.2.1.1.0, during the broadcast part of the discovery
        * protocol the members always pretend to be of 12.2.1. The real version
        * is only sent during the NewMemberWelcomeRequest-NewMemberWelcome
        * exchange.
         */
        public void setServiceVersion(String sVersion)
            {
            __m_ServiceVersion = sVersion;
            }
        
        // Declared at the super level
        public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
                throws java.io.IOException
            {
            super.write(output);
            
            output.writeInt(getPreferredPacketLength());
            output.writeInt(getPreferredPort());
            
            // the ServiceVersion property was added to 12.2.1.1.0;
            // older (12.2.1.0.x) members will simply ignore it
            output.writeUTF(getServiceVersion());
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService$NewMemberWelcomeRequest$Poll
        
        /**
         * The Poll contains information regarding a request sent to one or
         * more Cluster Members that require responses. A Service may poll
         * other Members that are running the same Service, and the Poll is
         * used to wait for and assemble the responses from each of those
         * Members. A client thread may also use the Poll to block on a
         * response or set of responses, thus waiting for the completion of the
         * Poll. In its simplest form, which is a Poll that is sent to one
         * Member of the Cluster, the Poll actually represents the
         * request/response model.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class Poll
                extends    com.tangosol.coherence.component.net.Poll
            {
            // ---- Fields declarations ----
            
            // Default constructor
            public Poll()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public Poll(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
                {
                super(sName, compParent, false);
                
                if (fInit)
                    {
                    __init();
                    }
                }
            
            // Main initializer
            public void __init()
                {
                // private initialization
                __initPrivate();
                
                
                // signal the end of the initialization
                set_Constructed(true);
                }
            
            // Private initializer
            protected void __initPrivate()
                {
                
                super.__initPrivate();
                }
            
            //++ getter for static property _Instance
            /**
             * Getter for property _Instance.<p>
            * Auto generated
             */
            public static com.tangosol.coherence.Component get_Instance()
                {
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService.NewMemberWelcomeRequest.Poll();
                }
            
            //++ getter for static property _CLASS
            /**
             * Getter for property _CLASS.<p>
            * Property with auto-generated accessor that returns the Class
            * object for a given component.
             */
            public static Class get_CLASS()
                {
                Class clz;
                try
                    {
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ClusterService$NewMemberWelcomeRequest$Poll".replace('/', '.'));
                    }
                catch (ClassNotFoundException e)
                    {
                    throw new NoClassDefFoundError(e.getMessage());
                    }
                return clz;
                }
            
            //++ getter for autogen property _Module
            /**
             * This is an auto-generated method that returns the global [design
            * time] parent component.
            * 
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            private com.tangosol.coherence.Component get_Module()
                {
                return this.get_Parent().get_Parent();
                }
            
            // Declared at the super level
            /**
             * This is the event that is executed when all the Members that were
            * polled have responded or have left the Service.
             */
            public void onCompletion()
                {
                ClusterService service = (ClusterService) getService();
                
                if (service.getServiceState() == ClusterService.SERVICE_STARTED)
                    {
                    service.setAcceptingClients(true);
                    }
                }
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService$NotifyIpTimeout
    
    /**
     * Message:
     *    Notify  IP Timeout
     * 
     * Purpose:
     *     Enable the TcpRingListener thread to inform the ClusterServices that
     * some monitored IP addresses exceed the ip-timeout.
     * 
     * Description:
     *    The TcpRingListener daemon thread of the member with the lowest
     * member id on each machine monitors the IPaddresses used by the members
     * of the cluster.   While the monitor fails to detect the availability of
     * an address exceeding its tolerance, it uses this message to notify the
     * ClusterService.  The ClusterService thread may then determine what
     * action if any should be taken while the condition persists.
     * 
     * Attributes:
     *     n/a
     * 
     * Response to:
     *     n/a
     * 
     * Expected responses:
     *     n/a
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class NotifyIpTimeout
            extends    com.tangosol.coherence.component.net.Message
        {
        // ---- Fields declarations ----
        
        /**
         * Property TimedOutAddress
         *
         */
        private java.net.InetAddress __m_TimedOutAddress;
        
        // Default constructor
        public NotifyIpTimeout()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public NotifyIpTimeout(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);
            
            if (fInit)
                {
                __init();
                }
            }
        
        // Main initializer
        public void __init()
            {
            // private initialization
            __initPrivate();
            
            // state initialization: public and protected properties
            try
                {
                setMessageType(55);
                }
            catch (java.lang.Exception e)
                {
                // re-throw as a runtime exception
                throw new com.tangosol.util.WrapperException(e);
                }
            
            // signal the end of the initialization
            set_Constructed(true);
            }
        
        // Private initializer
        protected void __initPrivate()
            {
            
            super.__initPrivate();
            }
        
        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
        * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService.NotifyIpTimeout();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ClusterService$NotifyIpTimeout".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }
        
        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
        * time] parent component.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent();
            }
        
        // Accessor for the property "TimedOutAddress"
        /**
         * Getter for property TimedOutAddress.<p>
         */
        public java.net.InetAddress getTimedOutAddress()
            {
            return __m_TimedOutAddress;
            }
        
        // Declared at the super level
        /**
         * This is the event that is executed when a Message is received.
        * <p>
        * It is the main processing event of the Message called by the
        * <code>Service.onMessage()</code> event. With regards to the use of
        * Message components within clustered Services, Services are designed
        * by dragging Message components into them as static children. These
        * Messages are the components that a Service can send to other running
        * instances of the same Service within a cluster. When the onReceived
        * event is invoked by a Service, it means that the Message has been
        * received; the code in the onReceived event is therefore the Message
        * specific logic for processing a received Message. For example, when
        * onReceived is invoked on a Message named FindData, the onReceived
        * event should do the work to "find the data", because it is being
        * invoked by the Service that received the "find the data" Message.
         */
        public void onReceived()
            {
            super.onReceived();
            
            ((ClusterService) getService()).onNotifyIpTimeout(getTimedOutAddress());
            }
        
        // Accessor for the property "TimedOutAddress"
        /**
         * Setter for property TimedOutAddress.<p>
         */
        public void setTimedOutAddress(java.net.InetAddress setTimedOut)
            {
            __m_TimedOutAddress = setTimedOut;
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService$NotifyMemberLeft
    
    /**
     * This internal Message is sent to all services when a Member has departed
     * (normally or not) the cluster.
     * 
     * Attributes:
     *     NotifyMember
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class NotifyMemberLeft
            extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyMemberLeft
        {
        // ---- Fields declarations ----
        
        // Default constructor
        public NotifyMemberLeft()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public NotifyMemberLeft(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);
            
            if (fInit)
                {
                __init();
                }
            }
        
        // Main initializer
        public void __init()
            {
            // private initialization
            __initPrivate();
            
            // state initialization: public and protected properties
            try
                {
                setMessageType(-6);
                }
            catch (java.lang.Exception e)
                {
                // re-throw as a runtime exception
                throw new com.tangosol.util.WrapperException(e);
                }
            
            // signal the end of the initialization
            set_Constructed(true);
            }
        
        // Private initializer
        protected void __initPrivate()
            {
            
            super.__initPrivate();
            }
        
        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
        * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService.NotifyMemberLeft();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ClusterService$NotifyMemberLeft".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }
        
        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
        * time] parent component.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent();
            }
        
        // Declared at the super level
        /**
         * This is the event that is executed when a Message is received.
        * <p>
        * It is the main processing event of the Message called by the
        * <code>Service.onMessage()</code> event. With regards to the use of
        * Message components within clustered Services, Services are designed
        * by dragging Message components into them as static children. These
        * Messages are the components that a Service can send to other running
        * instances of the same Service within a cluster. When the onReceived
        * event is invoked by a Service, it means that the Message has been
        * received; the code in the onReceived event is therefore the Message
        * specific logic for processing a received Message. For example, when
        * onReceived is invoked on a Message named FindData, the onReceived
        * event should do the work to "find the data", because it is being
        * invoked by the Service that received the "find the data" Message.
         */
        public void onReceived()
            {
            super.onReceived();
            
            ((ClusterService) getService()).onNotifyMemberLeft(getNotifyMember());
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService$NotifyResponse
    
    /**
     * Some of the internal messages (e.g. NotifyServiceJoining) are used as
     * the inter-service request-response communications between the
     * ClusterService and other services. This message serves as a generic
     * response.
     * 
     * Unlike the Response, this message is an internal one and would never be
     * deferred.
     * 
     * Attributes:
     *     Result
     * 
     * @since Coherence 3.7.1
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class NotifyResponse
            extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyResponse
        {
        // ---- Fields declarations ----
        
        /**
         * Property Continuation
         *
         * Continuation action to perform when this response is received.
         */
        private com.oracle.coherence.common.base.Continuation __m_Continuation;
        
        // Default constructor
        public NotifyResponse()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public NotifyResponse(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);
            
            if (fInit)
                {
                __init();
                }
            }
        
        // Main initializer
        public void __init()
            {
            // private initialization
            __initPrivate();
            
            // state initialization: public and protected properties
            try
                {
                setMessageType(-20);
                }
            catch (java.lang.Exception e)
                {
                // re-throw as a runtime exception
                throw new com.tangosol.util.WrapperException(e);
                }
            
            // signal the end of the initialization
            set_Constructed(true);
            }
        
        // Private initializer
        protected void __initPrivate()
            {
            
            super.__initPrivate();
            }
        
        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
        * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService.NotifyResponse();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ClusterService$NotifyResponse".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }
        
        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
        * time] parent component.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent();
            }
        
        // Accessor for the property "Continuation"
        /**
         * Getter for property Continuation.<p>
        * Continuation action to perform when this response is received.
         */
        public com.oracle.coherence.common.base.Continuation getContinuation()
            {
            return __m_Continuation;
            }
        
        // Declared at the super level
        /**
         * This is the event that is executed when a Message is received.
        * <p>
        * It is the main processing event of the Message called by the
        * <code>Service.onMessage()</code> event. With regards to the use of
        * Message components within clustered Services, Services are designed
        * by dragging Message components into them as static children. These
        * Messages are the components that a Service can send to other running
        * instances of the same Service within a cluster. When the onReceived
        * event is invoked by a Service, it means that the Message has been
        * received; the code in the onReceived event is therefore the Message
        * specific logic for processing a received Message. For example, when
        * onReceived is invoked on a Message named FindData, the onReceived
        * event should do the work to "find the data", because it is being
        * invoked by the Service that received the "find the data" Message.
         */
        public void onReceived()
            {
            // import com.oracle.coherence.common.base.Continuation;
            
            Continuation action = getContinuation();
            if (action != null)
                {
                action.proceed(getResult());
                }
            }
        
        // Accessor for the property "Continuation"
        /**
         * Setter for property Continuation.<p>
        * Continuation action to perform when this response is received.
         */
        public void setContinuation(com.oracle.coherence.common.base.Continuation continuation)
            {
            __m_Continuation = continuation;
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService$NotifyShutdown
    
    /**
     * This internal Message is sent to a Service it is supposed to shut down.
     * The Service must clean up and unregister itself. Note that the only task
     * of the shut-down is to begin the process of shutting down the service;
     * technically the Service does not have to be stopped by the time the
     * shutdown Message completes its processing, although the default
     * implementation does stop it immediately.
     * 
     * Attributes:
     *     n/a
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class NotifyShutdown
            extends    com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyShutdown
        {
        // ---- Fields declarations ----
        
        // Default constructor
        public NotifyShutdown()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public NotifyShutdown(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);
            
            if (fInit)
                {
                __init();
                }
            }
        
        // Main initializer
        public void __init()
            {
            // private initialization
            __initPrivate();
            
            // state initialization: public and protected properties
            try
                {
                setMessageType(-13);
                }
            catch (java.lang.Exception e)
                {
                // re-throw as a runtime exception
                throw new com.tangosol.util.WrapperException(e);
                }
            
            // signal the end of the initialization
            set_Constructed(true);
            }
        
        // Private initializer
        protected void __initPrivate()
            {
            
            super.__initPrivate();
            }
        
        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
        * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService.NotifyShutdown();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ClusterService$NotifyShutdown".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }
        
        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
        * time] parent component.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent();
            }
        
        // Declared at the super level
        /**
         * This is the event that is executed when a Message is received.
        * <p>
        * It is the main processing event of the Message called by the
        * <code>Service.onMessage()</code> event. With regards to the use of
        * Message components within clustered Services, Services are designed
        * by dragging Message components into them as static children. These
        * Messages are the components that a Service can send to other running
        * instances of the same Service within a cluster. When the onReceived
        * event is invoked by a Service, it means that the Message has been
        * received; the code in the onReceived event is therefore the Message
        * specific logic for processing a received Message. For example, when
        * onReceived is invoked on a Message named FindData, the onReceived
        * event should do the work to "find the data", because it is being
        * invoked by the Service that received the "find the data" Message.
         */
        public void onReceived()
            {
            // import Component.Net.Cluster;
            
            // super.onReceived();
            
            ClusterService service = (ClusterService) getService();
            
            service.setServiceState(ClusterService.SERVICE_STOPPING);
            service.doMemberLeaving();
            
            ((Cluster) service.getCluster()).waitHeuristicDelivery(500);
            
            service.doMemberLeft();
            service.stop();
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService$NotifyTcmpTimeout
    
    /**
     * Message:
     *    NotifyTcmpTimeout
     * 
     * Purpose:
     *     Enable the TCMP daemons to inform the ClusterServices that a member
     * has exceeded the configured delivery timeout.
     * 
     * Description:
     *    The TCMP uses this message to notify the ClusterService that a member
     * has not responded to a packet within the configured timeout.  The
     * ClusterService thread may then determine what action if any should be
     * taken.
     * 
     * Attributes:
     *     n/a
     * 
     * Response to:
     *     n/a
     * 
     * Expected responses:
     *     n/a
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class NotifyTcmpTimeout
            extends    com.tangosol.coherence.component.net.Message
        {
        // ---- Fields declarations ----
        
        /**
         * Property TimedOutMembers
         *
         * The set of members that did not acknowledge receipt of the packet
         * within the packet-timeout.
         */
        private transient com.tangosol.coherence.component.net.MemberSet __m_TimedOutMembers;
        
        /**
         * Property UndeliverablePacket
         *
         * The packet that was not able to be delivered within the
         * packet-timeout.
         */
        private transient com.tangosol.coherence.component.net.packet.MessagePacket __m_UndeliverablePacket;
        
        // Default constructor
        public NotifyTcmpTimeout()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public NotifyTcmpTimeout(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);
            
            if (fInit)
                {
                __init();
                }
            }
        
        // Main initializer
        public void __init()
            {
            // private initialization
            __initPrivate();
            
            // state initialization: public and protected properties
            try
                {
                setMessageType(54);
                }
            catch (java.lang.Exception e)
                {
                // re-throw as a runtime exception
                throw new com.tangosol.util.WrapperException(e);
                }
            
            // signal the end of the initialization
            set_Constructed(true);
            }
        
        // Private initializer
        protected void __initPrivate()
            {
            
            super.__initPrivate();
            }
        
        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
        * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService.NotifyTcmpTimeout();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ClusterService$NotifyTcmpTimeout".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }
        
        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
        * time] parent component.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent();
            }
        
        // Accessor for the property "TimedOutMembers"
        /**
         * Getter for property TimedOutMembers.<p>
        * The set of members that did not acknowledge receipt of the packet
        * within the packet-timeout.
         */
        public com.tangosol.coherence.component.net.MemberSet getTimedOutMembers()
            {
            return __m_TimedOutMembers;
            }
        
        // Accessor for the property "UndeliverablePacket"
        /**
         * Getter for property UndeliverablePacket.<p>
        * The packet that was not able to be delivered within the
        * packet-timeout.
         */
        public com.tangosol.coherence.component.net.packet.MessagePacket getUndeliverablePacket()
            {
            return __m_UndeliverablePacket;
            }
        
        // Declared at the super level
        /**
         * This is the event that is executed when a Message is received.
        * <p>
        * It is the main processing event of the Message called by the
        * <code>Service.onMessage()</code> event. With regards to the use of
        * Message components within clustered Services, Services are designed
        * by dragging Message components into them as static children. These
        * Messages are the components that a Service can send to other running
        * instances of the same Service within a cluster. When the onReceived
        * event is invoked by a Service, it means that the Message has been
        * received; the code in the onReceived event is therefore the Message
        * specific logic for processing a received Message. For example, when
        * onReceived is invoked on a Message named FindData, the onReceived
        * event should do the work to "find the data", because it is being
        * invoked by the Service that received the "find the data" Message.
         */
        public void onReceived()
            {
            ((ClusterService) getService()).onNotifyTcmpTimeout(
                getUndeliverablePacket(), getTimedOutMembers());
            }
        
        // Accessor for the property "TimedOutMembers"
        /**
         * Setter for property TimedOutMembers.<p>
        * The set of members that did not acknowledge receipt of the packet
        * within the packet-timeout.
         */
        public void setTimedOutMembers(com.tangosol.coherence.component.net.MemberSet pTimedOutMembers)
            {
            __m_TimedOutMembers = pTimedOutMembers;
            }
        
        // Accessor for the property "UndeliverablePacket"
        /**
         * Setter for property UndeliverablePacket.<p>
        * The packet that was not able to be delivered within the
        * packet-timeout.
         */
        public void setUndeliverablePacket(com.tangosol.coherence.component.net.packet.MessagePacket pUndeliverablePacket)
            {
            __m_UndeliverablePacket = pUndeliverablePacket;
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService$QuorumControl
    
    /**
     * QuorumControl manages the state and decision logic relating to
     * quorum-based membership decisions.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class QuorumControl
            extends    com.tangosol.coherence.Component
            implements com.tangosol.net.Cluster.MemberTimeoutAction
        {
        // ---- Fields declarations ----
        
        /**
         * Property AnnouncingMembers
         *
         * This map holds the set of members that are announcing their presence
         * in order to join the cluster, keyed by an InetSocketAddress
         * representing the announcing member's location.
         * 
         * Note: prior to induction into the cluster, the member announces with
         * a self-assigned UUID
         */
        private java.util.Map __m_AnnouncingMembers;
        
        /**
         * Property ConvictedMembers
         *
         * Set of members that have been selected for termination due to
         * timeout, but who have not yet been disconnected (e.g. due to the
         * cluster quorum policy).
         */
        private java.util.Set __m_ConvictedMembers;
        
        /**
         * Property IncidentStartTime
         *
         * The time at which the original outtage incident was detected.
         */
        private long __m_IncidentStartTime;
        
        /**
         * Property MoratoriumTimeMillis
         *
         * The time until which a moratorium on disconnecting any further
         * members is in effect.
         */
        private long __m_MoratoriumTimeMillis;
        
        /**
         * Property PendingRollCall
         *
         * The pending quorum roll-call message.
         */
        private ClusterService.QuorumRollCall __m_PendingRollCall;
        
        /**
         * Property PresenceProofExpiry
         *
         * This represents the amount of time after a communication has been
         * received from a member, that the member should be considered
         * "presence", for the purposes of establishing quorum.
         */
        private int __m_PresenceProofExpiry;
        
        /**
         * Property Suicide
         *
         * True iff this member has decided to kill itself due to timeout.
         */
        private boolean __m_Suicide;
        
        // Default constructor
        public QuorumControl()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public QuorumControl(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);
            
            if (fInit)
                {
                __init();
                }
            }
        
        // Main initializer
        public void __init()
            {
            // private initialization
            __initPrivate();
            
            
            // signal the end of the initialization
            set_Constructed(true);
            }
        
        // Private initializer
        protected void __initPrivate()
            {
            
            super.__initPrivate();
            }
        
        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
        * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService.QuorumControl();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ClusterService$QuorumControl".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }
        
        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
        * time] parent component.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent();
            }
        
        /**
         * Attempt to disconnect the set of convicted members if the cluster
        * quorum policy allows, returning true if members are disconnected or
        * false otherwise.
        * Called on the ClusterService thread only.
        * 
        * @return true iff the members are disconnected; false otherwise
         */
        protected boolean attemptDisconnect()
            {
            // import com.tangosol.util.Base;
            // import Component.Net.Member;
            // import java.util.Iterator;
            // import java.util.Set;
            
            ClusterService service     = getService();
            Set     setConvicts = getConvictedMembers();
            
            if (Base.getSafeTimeMillis() < getMoratoriumTimeMillis())
                {
                // moratorium on new disconnections is in effect
                return false;
                }
            else if (service.getActionPolicy().isAllowed(service, this))
                {
                // policy allows the members to be killed (or suicide to proceed)
                if (isSuicide())
                    {
                    _trace("Stopping ClusterService", 2);
                    service.onStopRunning();
                    }
                else
                    {
                    _trace("Timed-out members " + setConvicts + " will be removed.", 2);
                    for (Iterator iter = setConvicts.iterator(); iter.hasNext(); )
                        {
                        service.doMemberLeft((Member) iter.next());
                        }
                    }
            
                // we succeeded in disconnecting the convicted members;
                // close the roll-call poll if we have one open, as we
                // don't care about any of the other replies
                onIncidentClosed();
                return true;
                }
            else
                {
                // the action policy has prevented us from disconnecting the
                // convicted members (and splitting the cluster)
                return false;
                }
            }
        
        /**
         * Issue a quorum roll-call.
        * Called on the service thread only.
         */
        protected void doRollCall()
            {
            // import com.tangosol.util.Base;
            
            ClusterService         service     = getService();
            long            ldtExpiry   = Base.getSafeTimeMillis() + getPresenceProofExpiry();
            ClusterService.QuorumRollCall msgRollCall = (ClusterService.QuorumRollCall)
                service.instantiateMessage("QuorumRollCall");
            
            msgRollCall.ensureRequestPoll().setExpiryTimeMillis(ldtExpiry);
            msgRollCall.setToMemberSet(service.getOthersMemberSet());
            
            setPendingRollCall(msgRollCall);
            service.send(msgRollCall);
            }
        
        protected long ensureIncidentStartTime(long ldtNow)
            {
            long ldtStart = getIncidentStartTime();
            if (ldtStart == 0L)
                {
                setIncidentStartTime(ldtStart = ldtNow);
                }
            return ldtStart;
            }
        
        /**
         * Ensure that the specified member is stamped with a datetime
        * indicating when it was last timed-out.  If the member is not already
        * stamped, stamp it with the specified time.
        * 
        * @param member   the member to ensure a timeout stamp on
        * @param ldtNow     the timestamp to apply if the member is not already
        * stamped
         */
        protected long ensureMemberTimeoutStamp(com.tangosol.coherence.component.net.Member member, long ldtNow)
            {
            long ldtTimeout = member.getLastTimeoutMillis();
            if (ldtTimeout == 0L)
                {
                member.setLastTimeoutMillis(ldtTimeout = ldtNow);
                }
            return ldtTimeout;
            }
        
        /**
         * Format the current cluster quorum state in a readable format
         */
        public String formatStatus()
            {
            // import Component.Net.Member;
            // import java.util.HashSet;
            // import java.util.Iterator;
            // import java.util.Set;
            
            Set setConvicts = getConvictedMembers();
            if (setConvicts.isEmpty())
                {
                // don't print any status string in the common case
                return "";
                }
            
            Set           setMachines = new HashSet();
            StringBuilder sb          = new StringBuilder();
            sb.append("timed-out-members: {");
            for (Iterator iter = setConvicts.iterator(); iter.hasNext(); )
                {
                Member member = (Member) iter.next();
                sb.append(member.getId());
                setMachines.add(member.getMachineName());
                if (iter.hasNext())
                    {
                    sb.append(",");
                    }
                }
            
            sb.append("}; machine-names: {");
            for (Iterator iter = setMachines.iterator(); iter.hasNext(); )
                {
                sb.append(iter.next());
                if (iter.hasNext())
                    {
                    sb.append(", ");
                    }
                }
            sb.append('}');
            
            return sb.toString();
            }
        
        // Accessor for the property "AnnouncingMembers"
        /**
         * Getter for property AnnouncingMembers.<p>
        * This map holds the set of members that are announcing their presence
        * in order to join the cluster, keyed by an InetSocketAddress
        * representing the announcing member's location.
        * 
        * Note: prior to induction into the cluster, the member announces with
        * a self-assigned UUID
         */
        public java.util.Map getAnnouncingMembers()
            {
            // import com.tangosol.net.cache.LocalCache;
            // import java.util.Map;
            
            Map map = __m_AnnouncingMembers;
            if (map == null)
                {
                map = new LocalCache(Integer.MAX_VALUE, 2000);
                setAnnouncingMembers(map);
                }
            return map;
            }
        
        // From interface: com.tangosol.net.Cluster$MemberTimeoutAction
        public java.util.Set getAnnouncingMemberSet()
            {
            // import com.tangosol.util.ImmutableArrayList;
            
            return new ImmutableArrayList(getAnnouncingMembers().values()).getSet();
            }
        
        // Accessor for the property "ConvictedMembers"
        /**
         * Getter for property ConvictedMembers.<p>
        * Set of members that have been selected for termination due to
        * timeout, but who have not yet been disconnected (e.g. due to the
        * cluster quorum policy).
         */
        public java.util.Set getConvictedMembers()
            {
            return __m_ConvictedMembers;
            }
        
        // From interface: com.tangosol.net.Cluster$MemberTimeoutAction
        // Accessor for the property "IncidentStartTime"
        /**
         * Getter for property IncidentStartTime.<p>
        * The time at which the original outtage incident was detected.
         */
        public long getIncidentStartTime()
            {
            return __m_IncidentStartTime;
            }
        
        // Accessor for the property "MoratoriumTimeMillis"
        /**
         * Getter for property MoratoriumTimeMillis.<p>
        * The time until which a moratorium on disconnecting any further
        * members is in effect.
         */
        public long getMoratoriumTimeMillis()
            {
            return __m_MoratoriumTimeMillis;
            }
        
        // Accessor for the property "PendingRollCall"
        /**
         * Getter for property PendingRollCall.<p>
        * The pending quorum roll-call message.
         */
        public ClusterService.QuorumRollCall getPendingRollCall()
            {
            return __m_PendingRollCall;
            }
        
        // Accessor for the property "PresenceProofExpiry"
        /**
         * Getter for property PresenceProofExpiry.<p>
        * This represents the amount of time after a communication has been
        * received from a member, that the member should be considered
        * "presence", for the purposes of establishing quorum.
         */
        public int getPresenceProofExpiry()
            {
            // import com.tangosol.coherence.config.Config;
            
            int cExpiryMillis = __m_PresenceProofExpiry;
            if (cExpiryMillis == 0)
                {
                cExpiryMillis = Config.getInteger(
                    "coherence.quorum.expirymillis",
                    getService().getDeliveryTimeoutMillis() >> 2).intValue();
                setPresenceProofExpiry(cExpiryMillis);
                }
            return cExpiryMillis;
            }
        
        // From interface: com.tangosol.net.Cluster$MemberTimeoutAction
        public java.util.Set getResponsiveMemberSet()
            {
            // import Component.Net.Member;
            // import com.tangosol.util.DeltaSet;
            // import com.tangosol.util.NullImplementation;
            // import java.util.Collections;
            // import java.util.Set;
            
            Member          memberThis  = getService().getThisMember();
            ClusterService.QuorumRollCall msgRollCall = getPendingRollCall();
            if (msgRollCall == null || getConvictedMembers().contains(memberThis))
                {
                // this member was convicted; there are no healthy members
                return NullImplementation.getSet();
                }
            else
                {
                // consider this member healthy, along with any members
                // that have responded to the roll call request and aren't still
                // convicts; note they can be responsive to TCMP/datagram yet still be
                // unresponsive over the TransportService's bus, in which case they remain
                // convicts and aren't considered to be truly "responsive"
                Set setReplies = msgRollCall.getRequestPoll().getRespondedMemberSet();
                Set setHealthy = new DeltaSet(setReplies);
            
                setHealthy.removeAll(getConvictedMembers());
                setHealthy.add(memberThis);
                return Collections.unmodifiableSet(setHealthy);
                }
            }
        
        // Accessor for the property "Service"
        /**
         * Getter for property Service.<p>
         */
        public ClusterService getService()
            {
            return (ClusterService) get_Module();
            }
        
        // From interface: com.tangosol.net.Cluster$MemberTimeoutAction
        public java.util.Set getTimedOutMemberSet()
            {
            // import java.util.Collections;
            
            return isSuicide() ?
                Collections.singleton(getService().getThisMember()) :
                Collections.unmodifiableSet(getConvictedMembers());
            }
        
        // Accessor for the property "ClusterSuspended"
        /**
         * Getter for property ClusterSuspended.<p>
        * True iff the membership state of the cluster is suspended (e.g. new
        * members waiting to join, timed-out members not being disconnected)
        * due to quorum.
         */
        public boolean isClusterSuspended()
            {
            // if we have an outstanding roll-call, the cluster is suspended
            return getPendingRollCall() != null;
            }
        
        // Accessor for the property "Suicide"
        /**
         * Getter for property Suicide.<p>
        * True iff this member has decided to kill itself due to timeout.
         */
        public boolean isSuicide()
            {
            return __m_Suicide;
            }
        
        /**
         * Called when the current outtage "incident" is closed because no
        * timed-out members remain (either due to members being disconnected,
        * or due to communication being restored).
         */
        protected void onIncidentClosed()
            {
            // close any pending roll call
            ClusterService.QuorumRollCall msgRollCall = getPendingRollCall();
            if (msgRollCall != null)
                {
                msgRollCall.getRequestPoll().close();
                setPendingRollCall(null);
                }
            
            // reset incident-specific state
            setIncidentStartTime(0L);
            setSuicide(false);
            
            // make sure there is no member in the convict set.
            getConvictedMembers().clear();
            }
        
        // Declared at the super level
        /**
         * The "component has been initialized" method-notification called out
        * of setConstructed() for the topmost component and that in turn
        * notifies all the children.
        * 
        * This notification gets called before the control returns back to this
        * component instantiator (using <code>new Component.X()</code> or
        * <code>_newInstance(sName)</code>) and on the same thread. In
        * addition, visual components have a "posted" notification
        * <code>onInitUI</code> that is called after (or at the same time as)
        * the control returns back to the instantiator and possibly on a
        * different thread.
         */
        public void onInit()
            {
            // import Component.Net.MemberSet.DependentMemberSet;
            
            DependentMemberSet setConvicts = new DependentMemberSet();
            setConvicts.setBaseSet(getService().getClusterMemberSet());
            setConvictedMembers(setConvicts);
            
            super.onInit();
            }
        
        /**
         * Called to inform the QuorumControl that the specified announcing
        * member appears to be waiting to join the cluster.
        * Called on the ClusterService thread only.
         */
        public void onMemberAnnounceWaiting(com.tangosol.coherence.component.net.Member member)
            {
            getAnnouncingMembers().put(member.getSocketAddress(), member);
            }
        
        /**
         * Called to inform the QuorumControl that the specified member has
        * joined the cluster.
        * Called on the ClusterService thread only.
         */
        public void onMemberJoined(com.tangosol.coherence.component.net.Member member)
            {
            // remove the new member from set of announce-waiting members
            getAnnouncingMembers().remove(member.getSocketAddress());
            }
        
        /**
         * Called to inform the QuorumControl that the specified member has left
        * the cluster.
        * Called on the ClusterService thread only.
         */
        public void onMemberLeft(com.tangosol.coherence.component.net.Member member)
            {
            // import java.util.Set;
            
            Set setConvicts = getConvictedMembers();
            if ((setConvicts.remove(member) && setConvicts.isEmpty()) ||
                getService().getOthersMemberSet().isEmpty()) // in case we're contemplating suicide
                {
                // no more convicts (or other members to consult); incident is over
                onIncidentClosed();
                }
            }
        
        /**
         * Called to inform the QuorumControl that the specified set of members
        * has exceeded a configured timeout.
        * Called on the ClusterService thread only.
         */
        public void onMembersTimedOut(java.util.Set setTimedOut)
            {
            // import Component.Net.Member;
            // import com.tangosol.util.Base;
            // import java.util.Iterator;
            // import java.util.Set;
            
            ClusterService service     = getService();
            Set     setConvicts = getConvictedMembers();
            Member  memberThis  = service.getThisMember();
            long    ldtNow      = Base.getSafeTimeMillis();
            
            ensureIncidentStartTime(ldtNow);
            if (isSuicide())
                {
                // already contemplating suicide; nothing to do
                }
            else if (setTimedOut.contains(memberThis))
                {
                // We need to be very careful about suicide.  The quorum policy
                // disallowed our suicide attempt.  If we decided to kill ourselves,
                // it can only mean one of the following:
                //
                // 1. we are deaf
                // 2. we are mute
                // 3. there is partial network failure
                // 4. we are extremely slow
                //
                // If the problem is local (e.g. our NIC), the healthy portion of the
                // cluster will kill us shortly.  If the problem is global (e.g. the
                // whole network is down), then other cluster members are in the same
                // situation as we are, and overly-eager suicide would defeat the
                // purpose of quorum.
                // The only ways out of this state are either for the policy to change
                // its answer, for connectivity to be restored, or for other cluster
                // members to kill us
            
                setSuicide(true);
                ensureMemberTimeoutStamp(memberThis, ldtNow);
                setConvicts.clear();
                setConvicts.add(memberThis);
                }
            else
                {
                setConvicts.addAll(setTimedOut);
            
                for (Iterator iter = setTimedOut.iterator(); iter.hasNext(); )
                    {
                    Member member = (Member) iter.next();
            
                    member.setDeaf(true);
                    ensureMemberTimeoutStamp(member, ldtNow);
                    }
                }
            
            if (attemptDisconnect())
                {
                // disconnect was successful; nothing left to do
                }
            else
                {
                // the cluster quorum policy prevented the disconnection of the
                // timed-out members; do a roll-call in order to determine which
                // machines are still "responsive" (see also #onRollCallResponse)
                ClusterService.QuorumRollCall msgRollCall = getPendingRollCall();
                if (msgRollCall == null)
                    {
                    // this is the first set of members timing out; issue a roll-call
                    doRollCall();
                    }
                else
                    {
                    // there is already a pending roll-call and it will be
                    // re-evaluated periodically; nothing left to do
                    }
                }
            }
        
        /**
         * Called when the pending roll-call poll is completed (poll is closed).
        *  The roll-call completes when either all members have responded to
        * the roll-call (or left the cluster), or the roll-call expires.
        * 
        * @param member  the responding member
         */
        public void onRollCallCompleted(ClusterService.QuorumRollCall msgRollCall)
            {
            // import Component.Net.Member;
            // import com.tangosol.util.Base;
            // import java.util.Set;
            
            // The roll-call poll can be closed under 3 conditions:
            // 1. all polled members have responded
            // 2. some poll responses are outstanding, but poll was expired
            // 3. service is stopping
            
            setPendingRollCall(null);
            
            ClusterService service     = getService();
            Set     setConvicts = getConvictedMembers();
            if (service.getServiceState() >= ClusterService.SERVICE_STOPPING ||
                setConvicts.isEmpty())
                {
                // the incident is closed
                return;
                }
            
            if (isSuicide() && !msgRollCall.getRequestPoll().getRespondedMemberSet().isEmpty())
                {
                // we decided on suicide, but are now communicating successfully
                // with other members; there is now something to live for
                Member memberThis = service.getThisMember();
                long   ldtNow     = Base.getSafeTimeMillis();
                _trace("Re-established connectivity with the rest of the cluster "
                     + "(which has been timed-out for " +
                       (ldtNow - memberThis.getLastTimeoutMillis()) + "ms)", 3);
            
                setMoratoriumTimeMillis(ldtNow + getPresenceProofExpiry());
                memberThis.setLastTimeoutMillis(0L);
                onIncidentClosed();
                }
            else
                {
                // did not recover; try to disconnect the convicted members again
            
                if (attemptDisconnect())
                    {
                    // the convicts were successfully killed;
                    // the outtage incident is over
                    }
                else
                    {
                    // there are still convicts; log and re-issue the roll-call
                    String sAction = isSuicide() ?
                        "Stopping the local ClusterService" :
                        "Disconnect of suspect members " + setConvicts;
                    _trace(sAction + " is disallowed by the cluster quorum policy.", 2);
            
                    doRollCall();
                    }
                }
            }
        
        /**
         * Called when a response to the pending roll-call is received from the
        * specified member, and after the member has been added to the
        * responded member set
        * 
        * @param member  the responding member
         */
        public void onRollCallResponded(com.tangosol.coherence.component.net.Member member)
            {
            // we have additional data (more known healthy members) reconsult the
            // policy as we may now have sufficient quorum to remove timed-out members
            
            attemptDisconnect();
            }
        
        /**
         * Called when a response to the pending roll-call is received from the
        * specified member.
        * 
        * @param member  the responding member
         */
        public void onRollCallResponse(com.tangosol.coherence.component.net.Member member)
            {
            // import com.tangosol.util.Base;
            // import com.oracle.coherence.common.util.Duration;
            // import java.util.Set;
            
            Set setConvicts = getConvictedMembers();
            if (setConvicts.contains(member))
                {
                member.setDeaf(false); // must be cleared before checking for heuristic death
            
                // don't remove from convicts if still unhealthy, this is critical for bus based deaths as rollcall doesn't prove bus health
                if (!getService().isHeuristicallyDead(member))
                    {
                    // we got a positive response from a member that was convicted.
                    // Since this poll is (manually) closed periodically, we know that
                    // the response is relatively recent.  Pardon the convict and consider
                    // the member healthy once again.
                    long ldtNow = Base.getSafeTimeMillis();
                    _trace("Member " + member.getId() + " (which had timed-out "
                         + new Duration((ldtNow - member.getLastTimeoutMillis())*1000000)
                         + " ago) has become available again.", 3);
            
                    setConvicts.remove(member);
                    member.setLastTimeoutMillis(0L);
            
                    if (setConvicts.isEmpty())
                        {
                        // no convicts remain; the incident is over
                        onIncidentClosed();
                        }
                    else
                        {
                        // issue a temporary moratorium on further disconnections.
                        // In the case of of a physical network outtage having been
                        // restored, we will not receive notification immediately from all
                        // restored members.
                        setMoratoriumTimeMillis(ldtNow + getPresenceProofExpiry());
                        }
                    }
                }
            }
        
        // Accessor for the property "AnnouncingMembers"
        /**
         * Setter for property AnnouncingMembers.<p>
        * This map holds the set of members that are announcing their presence
        * in order to join the cluster, keyed by an InetSocketAddress
        * representing the announcing member's location.
        * 
        * Note: prior to induction into the cluster, the member announces with
        * a self-assigned UUID
         */
        protected void setAnnouncingMembers(java.util.Map setAnnounce)
            {
            __m_AnnouncingMembers = setAnnounce;
            }
        
        // Accessor for the property "ConvictedMembers"
        /**
         * Setter for property ConvictedMembers.<p>
        * Set of members that have been selected for termination due to
        * timeout, but who have not yet been disconnected (e.g. due to the
        * cluster quorum policy).
         */
        protected void setConvictedMembers(java.util.Set setConvicts)
            {
            __m_ConvictedMembers = setConvicts;
            }
        
        // Accessor for the property "IncidentStartTime"
        /**
         * Setter for property IncidentStartTime.<p>
        * The time at which the original outtage incident was detected.
         */
        protected void setIncidentStartTime(long ldtStart)
            {
            __m_IncidentStartTime = ldtStart;
            }
        
        // Accessor for the property "MoratoriumTimeMillis"
        /**
         * Setter for property MoratoriumTimeMillis.<p>
        * The time until which a moratorium on disconnecting any further
        * members is in effect.
         */
        protected void setMoratoriumTimeMillis(long cMoratoriumMillis)
            {
            __m_MoratoriumTimeMillis = cMoratoriumMillis;
            }
        
        // Accessor for the property "PendingRollCall"
        /**
         * Setter for property PendingRollCall.<p>
        * The pending quorum roll-call message.
         */
        protected void setPendingRollCall(ClusterService.QuorumRollCall msgRollCall)
            {
            __m_PendingRollCall = msgRollCall;
            }
        
        // Accessor for the property "PresenceProofExpiry"
        /**
         * Setter for property PresenceProofExpiry.<p>
        * This represents the amount of time after a communication has been
        * received from a member, that the member should be considered
        * "presence", for the purposes of establishing quorum.
         */
        protected void setPresenceProofExpiry(int cExpiryMillis)
            {
            __m_PresenceProofExpiry = cExpiryMillis;
            }
        
        // Accessor for the property "Suicide"
        /**
         * Setter for property Suicide.<p>
        * True iff this member has decided to kill itself due to timeout.
         */
        protected void setSuicide(boolean fSuicide)
            {
            __m_Suicide = fSuicide;
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService$QuorumRollCall
    
    /**
     * Message:
     *     QuorumRollCall
     * 
     * Purpose:
     *     Sent to members requesting a directed response (Acknowledgement).
     * 
     * Description:
     *     This message is used to obtain positive verification (via poll
     * response) that members remain alive and responsive within a cluster. 
     * The roll-call may be called for in situations where one or more
     * unresponsive members are being considered for disconnection, pending
     * satisfication of the cluster quorum policy.
     * 
     * Attributes:
     * 
     * Response to:
     * 
     * Expected responses:
     *     Acknowledgement
     * 
     * @since Coherence 3.6
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class QuorumRollCall
            extends    com.tangosol.coherence.component.net.message.RequestMessage
        {
        // ---- Fields declarations ----
        private static com.tangosol.util.ListMap __mapChildren;
        
        // Static initializer
        static
            {
            __initStatic();
            }
        
        // Default static initializer
        private static void __initStatic()
            {
            // register child classes
            __mapChildren = new com.tangosol.util.ListMap();
            __mapChildren.put("Poll", ClusterService.QuorumRollCall.Poll.get_CLASS());
            }
        
        // Default constructor
        public QuorumRollCall()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public QuorumRollCall(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);
            
            if (fInit)
                {
                __init();
                }
            }
        
        // Main initializer
        public void __init()
            {
            // private initialization
            __initPrivate();
            
            // state initialization: public and protected properties
            try
                {
                setMessageType(53);
                }
            catch (java.lang.Exception e)
                {
                // re-throw as a runtime exception
                throw new com.tangosol.util.WrapperException(e);
                }
            
            // containment initialization: children
            
            // signal the end of the initialization
            set_Constructed(true);
            }
        
        // Private initializer
        protected void __initPrivate()
            {
            
            super.__initPrivate();
            }
        
        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
        * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService.QuorumRollCall();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ClusterService$QuorumRollCall".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }
        
        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
        * time] parent component.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent();
            }
        
        //++ getter for autogen property _ChildClasses
        /**
         * This is an auto-generated method that returns the map of design time
        * [static] children.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        protected java.util.Map get_ChildClasses()
            {
            return __mapChildren;
            }
        
        // Declared at the super level
        protected com.tangosol.coherence.component.net.Poll instantiatePoll()
            {
            // import Component.Net.Poll;
            
            return (Poll) _newChild("Poll");
            }
        
        // Declared at the super level
        /**
         * This is the event that is executed when a Message is received.
        * <p>
        * It is the main processing event of the Message called by the
        * <code>Service.onMessage()</code> event. With regards to the use of
        * Message components within clustered Services, Services are designed
        * by dragging Message components into them as static children. These
        * Messages are the components that a Service can send to other running
        * instances of the same Service within a cluster. When the onReceived
        * event is invoked by a Service, it means that the Message has been
        * received; the code in the onReceived event is therefore the Message
        * specific logic for processing a received Message. For example, when
        * onReceived is invoked on a Message named FindData, the onReceived
        * event should do the work to "find the data", because it is being
        * invoked by the Service that received the "find the data" Message.
         */
        public void onReceived()
            {
            // send the requestor an Acknowledgement
            ClusterService          service = (ClusterService) getService();
            ClusterService.Acknowledgement msg     = (ClusterService.Acknowledgement)
                    service.instantiateMessage("Acknowledgement");
            
            msg.respondTo(this);
            service.send(msg);
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService$QuorumRollCall$Poll
        
        /**
         * The Poll contains information regarding a request sent to one or
         * more Cluster Members that require responses. A Service may poll
         * other Members that are running the same Service, and the Poll is
         * used to wait for and assemble the responses from each of those
         * Members. A client thread may also use the Poll to block on a
         * response or set of responses, thus waiting for the completion of the
         * Poll. In its simplest form, which is a Poll that is sent to one
         * Member of the Cluster, the Poll actually represents the
         * request/response model.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class Poll
                extends    com.tangosol.coherence.component.net.Poll
            {
            // ---- Fields declarations ----
            
            // Default constructor
            public Poll()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public Poll(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
                {
                super(sName, compParent, false);
                
                if (fInit)
                    {
                    __init();
                    }
                }
            
            // Main initializer
            public void __init()
                {
                // private initialization
                __initPrivate();
                
                
                // signal the end of the initialization
                set_Constructed(true);
                }
            
            // Private initializer
            protected void __initPrivate()
                {
                
                super.__initPrivate();
                }
            
            //++ getter for static property _Instance
            /**
             * Getter for property _Instance.<p>
            * Auto generated
             */
            public static com.tangosol.coherence.Component get_Instance()
                {
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService.QuorumRollCall.Poll();
                }
            
            //++ getter for static property _CLASS
            /**
             * Getter for property _CLASS.<p>
            * Property with auto-generated accessor that returns the Class
            * object for a given component.
             */
            public static Class get_CLASS()
                {
                Class clz;
                try
                    {
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ClusterService$QuorumRollCall$Poll".replace('/', '.'));
                    }
                catch (ClassNotFoundException e)
                    {
                    throw new NoClassDefFoundError(e.getMessage());
                    }
                return clz;
                }
            
            //++ getter for autogen property _Module
            /**
             * This is an auto-generated method that returns the global [design
            * time] parent component.
            * 
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            private com.tangosol.coherence.Component get_Module()
                {
                return this.get_Parent().get_Parent();
                }
            
            // Declared at the super level
            /**
             * Factory method for the responded member set.
             */
            protected com.tangosol.coherence.component.net.MemberSet instantiateRespondedMemberSet()
                {
                // import Component.Net.MemberSet.ActualMemberSet;
                
                // the responded set will be passed to the quorum policy,
                // so instantiate a real MemberSet here.
                return new ActualMemberSet();
                }
            
            // Declared at the super level
            /**
             * This is the event that is executed when all the Members that were
            * polled have responded or have left the Service.
             */
            protected void onCompletion()
                {
                super.onCompletion();
                
                ClusterService.QuorumRollCall msgRollCall = (ClusterService.QuorumRollCall) get_Parent();
                ((ClusterService) get_Module()).getQuorumControl().onRollCallCompleted(msgRollCall);
                }
            
            // Declared at the super level
            /**
             * This event occurs when a response from a Member is processed.
             */
            public void onResponded(com.tangosol.coherence.component.net.Member member)
                {
                // the order of calls is important
                
                super.onResponded(member);
                
                ((ClusterService) get_Module()).getQuorumControl().onRollCallResponded(member);
                }
            
            // Declared at the super level
            /**
             * This event occurs for each response Message from each polled
            * Member.
             */
            public void onResponse(com.tangosol.coherence.component.net.Message msg)
                {
                // the order of calls is important
                
                ((ClusterService) get_Module()).getQuorumControl().onRollCallResponse(msg.getFromMember());
                
                super.onResponse(msg);
                }
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService$SeniorMemberHeartbeat
    
    /**
     * DiscoveryMessage (broadcast):
     *     SeniorMemberHeartbeat
     * 
     * Purpose:
     *     The senior Member (or any Member that thinks that it is the senior
     * Member) periodically informs all cluster Members that it thinks that it
     * is the senior Member.
     * 
     * Description:
     *     This Message is issued periodically (at least several times within
     * the period defined for cluster timeout). This Message is broadcast by
     * the senior Member. It includes information about the senior Member
     * (cluster mini-id and Uid) and the list of Member ids that the cluster
     * knows about and the last time that the senior Member received a
     * non-broadcast Packet.
     * 
     * Attributes:
     *     LastReceivedMillis
     *     MemberSet
     *     WkaEnabled (Since 3.1)
     * 
     * Response to:
     *     n/a
     * 
     * Expected responses:
     *     SeniorMemberPanic
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class SeniorMemberHeartbeat
            extends    com.tangosol.coherence.component.net.message.DiscoveryMessage
        {
        // ---- Fields declarations ----
        
        /**
         * Property LastJoinTime
         *
         * The time at which the last node in this cluster joined.  A
         * receipient can use this to identify if the heartbeat should include
         * this member.
         */
        private long __m_LastJoinTime;
        
        /**
         * Property LastReceivedMillis
         *
         * The cluster timestamp of the last addressed AckPacket that was
         * received by the senior Member. This could theoretically be used as
         * supporting information for building a case should panic occur; for
         * example, a senior Member that has recently received addressed
         * Packets is obviously in a working cluster.
         */
        private long __m_LastReceivedMillis;
        
        /**
         * Property MemberSet
         *
         * The set of Member ids known to by the senior Member to be in the
         * cluster at the time that this Message is sent.
         * 
         * Note: When read, this is just a simple MemberSet and cannot iterate.
         */
        private com.tangosol.coherence.component.net.MemberSet __m_MemberSet;
        
        /**
         * Property WkaEnabled
         *
         * True if the heartbeat is from a senior which is using WKA, false
         * otherwise.
         */
        private boolean __m_WkaEnabled;
        
        // Default constructor
        public SeniorMemberHeartbeat()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public SeniorMemberHeartbeat(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);
            
            if (fInit)
                {
                __init();
                }
            }
        
        // Main initializer
        public void __init()
            {
            // private initialization
            __initPrivate();
            
            // state initialization: public and protected properties
            try
                {
                setMessageType(17);
                }
            catch (java.lang.Exception e)
                {
                // re-throw as a runtime exception
                throw new com.tangosol.util.WrapperException(e);
                }
            
            // signal the end of the initialization
            set_Constructed(true);
            }
        
        // Private initializer
        protected void __initPrivate()
            {
            
            super.__initPrivate();
            }
        
        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
        * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService.SeniorMemberHeartbeat();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ClusterService$SeniorMemberHeartbeat".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }
        
        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
        * time] parent component.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent();
            }
        
        // Declared at the super level
        /**
         * Getter for property Description.<p>
        * Used for debugging purposes (from toString). Create a human-readable
        * description of the specific Message data.
         */
        public String getDescription()
            {
            // import java.util.Date;
            
            long lLastRecvTimestamp = getLastReceivedMillis();
            
            String sLastRecvTimestamp = (lLastRecvTimestamp == 0L ?
                    "none" : new Date(lLastRecvTimestamp).toString());
            
            return "\nLastRecvTimestamp=" + sLastRecvTimestamp
                 + "\nMemberSet="         + getMemberSet();
            }
        
        // Accessor for the property "LastJoinTime"
        /**
         * Getter for property LastJoinTime.<p>
        * The time at which the last node in this cluster joined.  A receipient
        * can use this to identify if the heartbeat should include this member.
         */
        public long getLastJoinTime()
            {
            return __m_LastJoinTime;
            }
        
        // Accessor for the property "LastReceivedMillis"
        /**
         * Getter for property LastReceivedMillis.<p>
        * The cluster timestamp of the last addressed AckPacket that was
        * received by the senior Member. This could theoretically be used as
        * supporting information for building a case should panic occur; for
        * example, a senior Member that has recently received addressed Packets
        * is obviously in a working cluster.
         */
        public long getLastReceivedMillis()
            {
            return __m_LastReceivedMillis;
            }
        
        // Accessor for the property "MemberSet"
        /**
         * Getter for property MemberSet.<p>
        * The set of Member ids known to by the senior Member to be in the
        * cluster at the time that this Message is sent.
        * 
        * Note: When read, this is just a simple MemberSet and cannot iterate.
         */
        public com.tangosol.coherence.component.net.MemberSet getMemberSet()
            {
            return __m_MemberSet;
            }
        
        // Accessor for the property "WkaEnabled"
        /**
         * Getter for property WkaEnabled.<p>
        * True if the heartbeat is from a senior which is using WKA, false
        * otherwise.
         */
        public boolean isWkaEnabled()
            {
            return __m_WkaEnabled;
            }
        
        // Declared at the super level
        /**
         * This is the event that is executed when a Message is received.
        * <p>
        * It is the main processing event of the Message called by the
        * <code>Service.onMessage()</code> event. With regards to the use of
        * Message components within clustered Services, Services are designed
        * by dragging Message components into them as static children. These
        * Messages are the components that a Service can send to other running
        * instances of the same Service within a cluster. When the onReceived
        * event is invoked by a Service, it means that the Message has been
        * received; the code in the onReceived event is therefore the Message
        * specific logic for processing a received Message. For example, when
        * onReceived is invoked on a Message named FindData, the onReceived
        * event should do the work to "find the data", because it is being
        * invoked by the Service that received the "find the data" Message.
         */
        public void onReceived()
            {
            // import Component.Net.Member;
            
            super.onReceived();
            
            // this Message brings information about senior Member and its cluster
            // validate the broadcast
            ClusterService service = (ClusterService) getService();
            if (service.validateSeniorBroadcast(this, getMemberSet()))
                {
                Member memberThis = service.getThisMember();
            
                // verify that this Member is still alive (recognized by the senior)
                if (getLastJoinTime() >= memberThis.getTimestamp() && !getMemberSet().contains(memberThis))
                    {
                    // this Member is believed to be dead
                    _trace("Received cluster heartbeat from the senior "
                        + service.getClusterOldestMember() + " that does not contain this " + memberThis
                        + "; stopping cluster service.", 1);
            
                    service.onStopRunning();
                    }
                }
            }
        
        // Declared at the super level
        public void read(com.tangosol.io.ReadBuffer.BufferInput input)
                throws java.io.IOException
            {
            // import Component.Net.MemberSet;
            
            super.read(input);
            
            setLastReceivedMillis(input.readLong());
            MemberSet setMember = new MemberSet();
            setMember.readExternal(input);
            setMemberSet(setMember);
            setWkaEnabled(input.readBoolean());
            setLastJoinTime(input.readLong());
            
            ensureEOS(input);
            }
        
        // Accessor for the property "LastJoinTime"
        /**
         * Setter for property LastJoinTime.<p>
        * The time at which the last node in this cluster joined.  A receipient
        * can use this to identify if the heartbeat should include this member.
         */
        public void setLastJoinTime(long lTime)
            {
            __m_LastJoinTime = lTime;
            }
        
        // Accessor for the property "LastReceivedMillis"
        /**
         * Setter for property LastReceivedMillis.<p>
        * The cluster timestamp of the last addressed AckPacket that was
        * received by the senior Member. This could theoretically be used as
        * supporting information for building a case should panic occur; for
        * example, a senior Member that has recently received addressed Packets
        * is obviously in a working cluster.
         */
        public void setLastReceivedMillis(long cMillis)
            {
            __m_LastReceivedMillis = cMillis;
            }
        
        // Accessor for the property "MemberSet"
        /**
         * Setter for property MemberSet.<p>
        * The set of Member ids known to by the senior Member to be in the
        * cluster at the time that this Message is sent.
        * 
        * Note: When read, this is just a simple MemberSet and cannot iterate.
         */
        public void setMemberSet(com.tangosol.coherence.component.net.MemberSet setMember)
            {
            __m_MemberSet = setMember;
            }
        
        // Accessor for the property "WkaEnabled"
        /**
         * Setter for property WkaEnabled.<p>
        * True if the heartbeat is from a senior which is using WKA, false
        * otherwise.
         */
        public void setWkaEnabled(boolean pWkaEnabled)
            {
            __m_WkaEnabled = pWkaEnabled;
            }
        
        // Declared at the super level
        public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
                throws java.io.IOException
            {
            super.write(output);
            
            output.writeLong(getLastReceivedMillis());
            getMemberSet().writeExternal(output);
            output.writeBoolean(isWkaEnabled());
            output.writeLong(getLastJoinTime());
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService$SeniorMemberKill
    
    /**
     * DiscoveryMessage (broadcast from junior to senior; directed from senior
     * to junior):
     *     SeniorMemberKill
     * 
     * Purpose:
     *     This Message informs a Member that it must stop.
     * 
     * Description:
     *     Phantom cluster recognition (when a Member determines that two
     * clusters exist on the same multicast) must result in the stoppage of one
     * of the two clusters. Theoretically, the "bad" cluster is going to most
     * often be a cluster of one Member that somehow didn't realize that a
     * cluster was running, or somehow detached from a running cluster (due to
     * being temporary disconnected from the network, for example). This
     * Message is  sent by a junior Member upon request of its senior to kill a
     * detached (doomed) senior, then by the doomed senior to kill all its
     * juniors.  This ensures that the cluster whose members send this kill
     * Message is indeed operational and that at least two of its Members
     * (including the senior Member itself) agree on which Member is the
     * senior.
     * 
     * Attributes:
     * 
     * Response to:
     *     SeniorMemberPanic
     * 
     * Expected responses:
     *     n/a
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class SeniorMemberKill
            extends    com.tangosol.coherence.component.net.message.DiscoveryMessage
        {
        // ---- Fields declarations ----
        
        // Default constructor
        public SeniorMemberKill()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public SeniorMemberKill(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);
            
            if (fInit)
                {
                __init();
                }
            }
        
        // Main initializer
        public void __init()
            {
            // private initialization
            __initPrivate();
            
            // state initialization: public and protected properties
            try
                {
                setMessageType(40);
                }
            catch (java.lang.Exception e)
                {
                // re-throw as a runtime exception
                throw new com.tangosol.util.WrapperException(e);
                }
            
            // signal the end of the initialization
            set_Constructed(true);
            }
        
        // Private initializer
        protected void __initPrivate()
            {
            
            super.__initPrivate();
            }
        
        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
        * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService.SeniorMemberKill();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ClusterService$SeniorMemberKill".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }
        
        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
        * time] parent component.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent();
            }
        
        // Declared at the super level
        /**
         * Getter for property Description.<p>
        * Used for debugging purposes (from toString). Create a human-readable
        * description of the specific Message data.
         */
        public String getDescription()
            {
            return "ToMember=" + getToMember();
            }
        
        // Declared at the super level
        /**
         * This is the event that is executed when a Message is received.
        * <p>
        * It is the main processing event of the Message called by the
        * <code>Service.onMessage()</code> event. With regards to the use of
        * Message components within clustered Services, Services are designed
        * by dragging Message components into them as static children. These
        * Messages are the components that a Service can send to other running
        * instances of the same Service within a cluster. When the onReceived
        * event is invoked by a Service, it means that the Message has been
        * received; the code in the onReceived event is therefore the Message
        * specific logic for processing a received Message. For example, when
        * onReceived is invoked on a Message named FindData, the onReceived
        * event should do the work to "find the data", because it is being
        * invoked by the Service that received the "find the data" Message.
         */
        public void onReceived()
            {
            // import Component.Net.Member;
            // import Component.Net.MemberSet;
            // import Component.Net.MemberSet.ActualMemberSet;
            // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet.MasterMemberSet;
            // import Component.Net.MemberSet.SingleMemberSet;
            // import com.oracle.coherence.common.base.Blocking;
            // import com.tangosol.util.UUID;
            // import java.util.Iterator;
            
            super.onReceived();
            
            ClusterService service = (ClusterService) getService();
            if (!service.isRunning())
                {
                return;
                }
            
            switch (service.getState())
                {
                case ClusterService.STATE_ANNOUNCE:
                case ClusterService.STATE_JOINING:
                    // we don't know what's going on, but it's obviously not
                    // good and we need to keep our head down
                    service.resetBroadcastCounter("the presence of an existing cluster", this);
                    break;
            
                case ClusterService.STATE_JOINED:
                    // check if this Message is for this Member
                    Member memberFrom = getFromMember();
                    Member memberKill = getToMember();
                    Member memberThis = service.getThisMember();
                    if (memberThis.getUid32().equals(memberKill.getUid32()))
                        {
                        _trace("Received a Kill message from a valid " +
                            memberFrom + "; stopping cluster service.", 1);
            
                        // if this is a senior, it needs to send (non-broadcast) kill messages to all
                        // of its known members
                        MasterMemberSet setMember = service.getClusterMemberSet();
                        if (memberThis == setMember.getOldestMember())
                            {
                            MemberSet setOther = new ActualMemberSet();
                            setOther.addAll(setMember);
                            setOther.remove(memberThis);
                            for (Iterator iter = setOther.iterator(); iter.hasNext(); )
                                {
                                Member            member = (Member) iter.next();
                                ClusterService.SeniorMemberKill msg    = (ClusterService.SeniorMemberKill)
                                    service.instantiateMessage("SeniorMemberKill");
            
                                // The SeniorMemberKill is a DiscoveryMessage, and would be delivered as
                                // as a broadcast unless the ToMemberSet is set. Specifying it will
                                // change the message to a directed, causing the recipient to accept the
                                // request only if it considers this senior as part of its cluster.
                                msg.setToMember(member);
                                msg.setToMemberSet(SingleMemberSet.instantiate(member));
                                service.send(msg);
                                }
            
                            try
                                {
                                Blocking.sleep(service.getBroadcastRepeatMillis() * 2);
                                }
                            catch (InterruptedException e)
                                {
                                Thread.currentThread().interrupt();
                                }
                            }
                        service.onStopRunning();
                        }
                    break;
            
                default:
                    // we're already shutting down; ignore it
                    break;
                }
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService$SeniorMemberPanic
    
    /**
     * Message:
     *     SeniorMemberPanic
     * 
     * Purpose:
     *     This Message informs a cluster Member that there is confusion as to
     * who is the senior Member.
     * 
     * Description:
     *     In response to any broadcast Message from a purported senior Member
     * that is suspect, this Message is issued to register that there is
     * confusion (panic!) and to provide information on what should be done.
     * The Message is sent from a senior Member to the entire cluster, and from
     * a junior Member to the senior Member. By doing so, the act of sending
     * verifies that the senior Member is alive and is communicating with its
     * cluster, and provides permission for the junior Members to kill the
     * other purported senior Member.
     * 
     * Since Coherence 2.4 this message is also used by the senior Member to
     * inform junior members that the confusion is caused by a "zombie" Member.
     * 
     * Attributes:
     *     CulpritMember
     *     Zombie
     * 
     * Response to:
     *     NewMemberAnnounceReply
     *     NewMemberRequestIdReject
     *     NewMemberRequestIdReply
     *     SeniorMemberHeartbeat
     * 
     * Expected responses:
     *     SeniorMemberKill
     *     MemberLeft
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class SeniorMemberPanic
            extends    com.tangosol.coherence.component.net.Message
        {
        // ---- Fields declarations ----
        
        /**
         * Property CulpritMember
         *
         * The would-be senior Member that caused the panic.
         */
        private com.tangosol.coherence.component.net.Member __m_CulpritMember;
        
        /**
         * Property Zombie
         *
         * Indicates whether or not the specifed CulpritMember is known to be a
         * dead member.
         */
        private boolean __m_Zombie;
        
        // Default constructor
        public SeniorMemberPanic()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public SeniorMemberPanic(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);
            
            if (fInit)
                {
                __init();
                }
            }
        
        // Main initializer
        public void __init()
            {
            // private initialization
            __initPrivate();
            
            // state initialization: public and protected properties
            try
                {
                setMessageType(41);
                }
            catch (java.lang.Exception e)
                {
                // re-throw as a runtime exception
                throw new com.tangosol.util.WrapperException(e);
                }
            
            // signal the end of the initialization
            set_Constructed(true);
            }
        
        // Private initializer
        protected void __initPrivate()
            {
            
            super.__initPrivate();
            }
        
        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
        * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService.SeniorMemberPanic();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ClusterService$SeniorMemberPanic".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }
        
        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
        * time] parent component.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent();
            }
        
        // Accessor for the property "CulpritMember"
        /**
         * Getter for property CulpritMember.<p>
        * The would-be senior Member that caused the panic.
         */
        public com.tangosol.coherence.component.net.Member getCulpritMember()
            {
            return __m_CulpritMember;
            }
        
        // Declared at the super level
        /**
         * Getter for property Description.<p>
        * Used for debugging purposes (from toString). Create a human-readable
        * description of the specific Message data.
         */
        public String getDescription()
            {
            return "CulpritMember=" + getCulpritMember()
                 + (isZombie() ? " (zombie)" : "");
            }
        
        // Accessor for the property "Zombie"
        /**
         * Getter for property Zombie.<p>
        * Indicates whether or not the specifed CulpritMember is known to be a
        * dead member.
         */
        public boolean isZombie()
            {
            return __m_Zombie;
            }
        
        // Declared at the super level
        /**
         * This is the event that is executed when a Message is received.
        * <p>
        * It is the main processing event of the Message called by the
        * <code>Service.onMessage()</code> event. With regards to the use of
        * Message components within clustered Services, Services are designed
        * by dragging Message components into them as static children. These
        * Messages are the components that a Service can send to other running
        * instances of the same Service within a cluster. When the onReceived
        * event is invoked by a Service, it means that the Message has been
        * received; the code in the onReceived event is therefore the Message
        * specific logic for processing a received Message. For example, when
        * onReceived is invoked on a Message named FindData, the onReceived
        * event should do the work to "find the data", because it is being
        * invoked by the Service that received the "find the data" Message.
         */
        public void onReceived()
            {
            // import Component.Net.Member;
            // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet.MasterMemberSet;
            
            super.onReceived();
            
            // this Message brings information about a "wrong" senior Member
            ClusterService  service = (ClusterService) getService();
            if (!service.isRunning() || service.getState() != ClusterService.STATE_JOINED)
                {
                return;
                }
            
            MasterMemberSet setMember     = service.getClusterMemberSet();
            Member          memberThis    = service.getThisMember();
            Member          memberFrom    = getFromMember();
            Member          memberOldest  = setMember.getOldestMember();
            Member          memberCulprit = getCulpritMember();
            Member          memberDead    = setMember.findDeadMember(memberCulprit);
            
            if (memberThis == memberOldest)
                {
                // make sure that this senior's heartbeat reaches the culprit node
                service.addDynamicBroadcast(memberCulprit);
            
                if (memberDead == null)
                    {
                    // we have no information about that member; ignore the panic from the junior
                    // and defer the decision till this senior can hear the culprit by itself
                    _trace("Received panic from junior member " + memberFrom +
                           " caused by " + memberCulprit, 2);
                    }
                else
                    {
                    // inform the panicking junior about the "zombie"
                    ClusterService.SeniorMemberPanic msg = (ClusterService.SeniorMemberPanic)
                            service.instantiateMessage("SeniorMemberPanic");
                    msg.setCulpritMember(memberCulprit);
                    msg.addToMember(memberFrom);
                    msg.setZombie(true);
                    service.send(msg);
                    }
                }
            else if (memberFrom == memberOldest)
                {
                if (isZombie())
                    {
                    if (memberDead == null)
                        {
                        // add the culprit member to the RecycleSet
                        memberCulprit.setDead(true);
                        setMember.getRecycleSet().add(memberCulprit);
                        }
                    }
                else
                    {
                    _trace("Received panic from senior " + memberFrom +
                           " caused by " + memberCulprit, 1);
            
                    // send (broadcast) a kill Message to the bad Member
                    ClusterService.SeniorMemberKill msg = (ClusterService.SeniorMemberKill)
                            service.instantiateMessage("SeniorMemberKill");
                    msg.setToMember(memberCulprit);
                    service.send(msg);
                    }
                }
            else
                {
                // The panic from a junior indicates its belief that we are the senior.
                // This can happen if we were next in line to become the senior but haven't
                // yet identified that our senior is dead. Hold off for now, more should come.
                _trace("Deferring panic notification from " + memberFrom, 4);    
                }
            }
        
        // Declared at the super level
        public void read(com.tangosol.io.ReadBuffer.BufferInput input)
                throws java.io.IOException
            {
            // import Component.Net.Member;
            
            setZombie(input.readBoolean());
            
            ClusterService service = (ClusterService) getService();
            Member memberCulprit = service.instantiateMember();
            memberCulprit.readExternal(input);
            setCulpritMember(memberCulprit);
            }
        
        // Accessor for the property "CulpritMember"
        /**
         * Setter for property CulpritMember.<p>
        * The would-be senior Member that caused the panic.
         */
        public void setCulpritMember(com.tangosol.coherence.component.net.Member uid)
            {
            __m_CulpritMember = uid;
            }
        
        // Accessor for the property "Zombie"
        /**
         * Setter for property Zombie.<p>
        * Indicates whether or not the specifed CulpritMember is known to be a
        * dead member.
         */
        public void setZombie(boolean fZombie)
            {
            __m_Zombie = fZombie;
            }
        
        // Declared at the super level
        public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
                throws java.io.IOException
            {
            output.writeBoolean(isZombie());
            getCulpritMember().writeExternal(output);
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService$ServiceJoined
    
    /**
     * Message:
     *     ServiceJoined
     * 
     * Purpose:
     *     Informs all Members of the cluster that a Member has started a
     * Service.
     * 
     * Description:
     *     When a cluster has a Service registered, and a Member has finished
     * starting that Service locally, this Message is sent to tell the cluster
     * Members that the Service is now available on that Member.
     * 
     * Attributes:
     *     ServiceId
     * 
     * Response to:
     *     n/a
     * 
     * Expected responses:
     *     n/a
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class ServiceJoined
            extends    com.tangosol.coherence.component.net.Message
        {
        // ---- Fields declarations ----
        
        /**
         * Property ServiceId
         *
         * The id of the Service that the sending Member has joined.
         */
        private int __m_ServiceId;
        
        // Default constructor
        public ServiceJoined()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public ServiceJoined(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);
            
            if (fInit)
                {
                __init();
                }
            }
        
        // Main initializer
        public void __init()
            {
            // private initialization
            __initPrivate();
            
            // state initialization: public and protected properties
            try
                {
                setMessageType(49);
                }
            catch (java.lang.Exception e)
                {
                // re-throw as a runtime exception
                throw new com.tangosol.util.WrapperException(e);
                }
            
            // signal the end of the initialization
            set_Constructed(true);
            }
        
        // Private initializer
        protected void __initPrivate()
            {
            
            super.__initPrivate();
            }
        
        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
        * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService.ServiceJoined();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ClusterService$ServiceJoined".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }
        
        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
        * time] parent component.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent();
            }
        
        // Declared at the super level
        /**
         * Getter for property Description.<p>
        * Used for debugging purposes (from toString). Create a human-readable
        * description of the specific Message data.
         */
        public String getDescription()
            {
            return "ServiceId=" + getServiceId();
            }
        
        // Accessor for the property "ServiceId"
        /**
         * Getter for property ServiceId.<p>
        * The id of the Service that the sending Member has joined.
         */
        public int getServiceId()
            {
            return __m_ServiceId;
            }
        
        // Declared at the super level
        /**
         * This is the event that is executed when a Message is received.
        * <p>
        * It is the main processing event of the Message called by the
        * <code>Service.onMessage()</code> event. With regards to the use of
        * Message components within clustered Services, Services are designed
        * by dragging Message components into them as static children. These
        * Messages are the components that a Service can send to other running
        * instances of the same Service within a cluster. When the onReceived
        * event is invoked by a Service, it means that the Message has been
        * received; the code in the onReceived event is therefore the Message
        * specific logic for processing a received Message. For example, when
        * onReceived is invoked on a Message named FindData, the onReceived
        * event should do the work to "find the data", because it is being
        * invoked by the Service that received the "find the data" Message.
         */
        public void onReceived()
            {
            // import Component.Net.Member;
            // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet;
            // import Component.Net.ServiceInfo;
            
            super.onReceived();
            
            // this Message brings information about a Member that has finished joining a Service
            ClusterService     service  = (ClusterService) getService();
            int         nService = getServiceId();
            ServiceInfo info     = service.getServiceInfo(nService);
            
            if (info == null)
                {
                // we never remove the ServiceInfo so we should already know about this
                // service id (because we should have already received a ServiceJoining)
                _trace("ClusterService$ServiceJoined: " + "Unknown Service " + nService, 1);
                }
            else
                {
                Member member = getFromMember();
            
                if (member == service.getThisMember())
                    {
                    // this message came from the service on this node;
                    // publish the event to the rest of the cluster
            
                    ClusterService.ServiceJoined msg = (ClusterService.ServiceJoined)
                        service.instantiateMessage("ServiceJoined");
                    msg.setToMemberSet(service.getOthersMemberSet());
                    msg.setServiceId(nService);
                    service.send(msg);
                    }
            
                ServiceMemberSet setMember = info.getMemberSet();
                if (setMember.contains(member))
                    {
                    setMember.setServiceJoined(member.getId());
                    }
                else
                    {
                    // this can only happen if the ServiceJoined message was
                    // sent before the NewMemberWelcome (see COH-6379)
                    if (service.isAcceptingClients())
                        {
                        // soft assert
                        _trace("Ignoring out of order ServiceJoined for " + info.getServiceName()
                             + " from " + member, 5);
                        }
                    }
                }
            }
        
        // Declared at the super level
        public void read(com.tangosol.io.ReadBuffer.BufferInput input)
                throws java.io.IOException
            {
            setServiceId(input.readUnsignedShort());
            }
        
        // Accessor for the property "ServiceId"
        /**
         * Setter for property ServiceId.<p>
        * The id of the Service that the sending Member has joined.
         */
        public void setServiceId(int nId)
            {
            __m_ServiceId = nId;
            }
        
        // Declared at the super level
        public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
                throws java.io.IOException
            {
            output.writeShort(getServiceId());
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService$ServiceJoining
    
    /**
     * RequestMessage:
     *     ServiceJoining
     * 
     * Purpose:
     *     Informs all Members of the cluster that a Member is joining a
     * Service (i.e. the Member has started running that Service).
     * 
     * Description:
     *     When a cluster has a Service registered, and a Member starts that
     * Service locally, this Message is sent by the cluster senior member to
     * tell cluster Members that the Member has just started the Service.  This
     * message carries the ServiceJoinTime (assigned by the cluster senior) and
     * will be followed by a ServiceJoined message once the Member has finished
     * starting the service.
     * 
     * Attributes:
     *     ServiceId
     *     ServiceName
     *     ServiceType
     *     ServiceVersion
     *     ServiceJoinTime
     *     MemberConfigMap
     *     PermissionInfo [as of 12.1.2]
     * 
     * Response to:
     *     ServiceJoinRequest
     * 
     * Expected responses:
     *     ServiceUpdateResponse
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class ServiceJoining
            extends    com.tangosol.coherence.component.net.message.RequestMessage
            implements com.oracle.coherence.common.base.Continuation
        {
        // ---- Fields declarations ----
        
        /**
         * Property JoinRequest
         *
         * Transient property representing the original join request.
         */
        private transient ClusterService.ServiceJoinRequest __m_JoinRequest;
        
        /**
         * Property MemberConfigMap
         *
         * Snapshot of the Member's own configuration map for the Service at
         * the point in time that the Service was joined.
         */
        private java.util.Map __m_MemberConfigMap;
        
        /**
         * Property MemberId
         *
         * The id of the member that joined the Service has started.
         */
        private int __m_MemberId;
        
        /**
         * Property PermissionInfo
         *
         * The information needed to validate and respond to a security related
         * part of the join request. Initially, the value is used for security
         * validation. When this message is used as a response to the join
         * request, the property contains a response permission info.
         */
        private com.tangosol.net.security.PermissionInfo __m_PermissionInfo;
        
        /**
         * Property ServiceEndPointName
         *
         * The canonical EndPoint name for the joining service.
         */
        private String __m_ServiceEndPointName;
        
        /**
         * Property ServiceId
         *
         * The id of the Service that the sending Member has started.
         */
        private int __m_ServiceId;
        
        /**
         * Property ServiceJoinTime
         *
         * The cluster time that the Member joined the Service.
         */
        private long __m_ServiceJoinTime;
        
        /**
         * Property ServiceName
         *
         * The name of the Service.
         */
        private String __m_ServiceName;
        
        /**
         * Property ServiceType
         *
         * The type of the Service.
         */
        private String __m_ServiceType;
        
        /**
         * Property ServiceVersion
         *
         * The version of the Service.
         */
        private String __m_ServiceVersion;
        private static com.tangosol.util.ListMap __mapChildren;
        
        // Static initializer
        static
            {
            __initStatic();
            }
        
        // Default static initializer
        private static void __initStatic()
            {
            // register child classes
            __mapChildren = new com.tangosol.util.ListMap();
            __mapChildren.put("Poll", ClusterService.ServiceJoining.Poll.get_CLASS());
            }
        
        // Default constructor
        public ServiceJoining()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public ServiceJoining(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);
            
            if (fInit)
                {
                __init();
                }
            }
        
        // Main initializer
        public void __init()
            {
            // private initialization
            __initPrivate();
            
            // state initialization: public and protected properties
            try
                {
                setMessageType(43);
                }
            catch (java.lang.Exception e)
                {
                // re-throw as a runtime exception
                throw new com.tangosol.util.WrapperException(e);
                }
            
            // containment initialization: children
            
            // signal the end of the initialization
            set_Constructed(true);
            }
        
        // Private initializer
        protected void __initPrivate()
            {
            
            super.__initPrivate();
            }
        
        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
        * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService.ServiceJoining();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ClusterService$ServiceJoining".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }
        
        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
        * time] parent component.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent();
            }
        
        //++ getter for autogen property _ChildClasses
        /**
         * This is an auto-generated method that returns the map of design time
        * [static] children.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        protected java.util.Map get_ChildClasses()
            {
            return __mapChildren;
            }
        
        // Declared at the super level
        /**
         * Getter for property Description.<p>
        * Used for debugging purposes (from toString). Create a human-readable
        * description of the specific Message data.
         */
        public String getDescription()
            {
            return "MemberId="          + getMemberId()
                 + "\nServiceId="       + getServiceId()
                 + "\nServiceName="     + getServiceName()
                 + "\nServiceVersion="  + getServiceVersion()
                 + "\nServiceEndPoint=" + getServiceEndPointName()
                 + "\nServiceJoinTime=" + getServiceJoinTime()
                 + "\nMemberConfigMap=" + getMemberConfigMap();
            }
        
        // Accessor for the property "JoinRequest"
        /**
         * Getter for property JoinRequest.<p>
        * Transient property representing the original join request.
         */
        public ClusterService.ServiceJoinRequest getJoinRequest()
            {
            return __m_JoinRequest;
            }
        
        // Accessor for the property "MemberConfigMap"
        /**
         * Getter for property MemberConfigMap.<p>
        * Snapshot of the Member's own configuration map for the Service at the
        * point in time that the Service was joined.
         */
        public java.util.Map getMemberConfigMap()
            {
            return __m_MemberConfigMap;
            }
        
        // Accessor for the property "MemberId"
        /**
         * Getter for property MemberId.<p>
        * The id of the member that joined the Service has started.
         */
        public int getMemberId()
            {
            return __m_MemberId;
            }
        
        // Accessor for the property "PermissionInfo"
        /**
         * Getter for property PermissionInfo.<p>
        * The information needed to validate and respond to a security related
        * part of the join request. Initially, the value is used for security
        * validation. When this message is used as a response to the join
        * request, the property contains a response permission info.
         */
        public com.tangosol.net.security.PermissionInfo getPermissionInfo()
            {
            return __m_PermissionInfo;
            }
        
        // Accessor for the property "ServiceEndPointName"
        /**
         * Getter for property ServiceEndPointName.<p>
        * The canonical EndPoint name for the joining service.
         */
        public String getServiceEndPointName()
            {
            return __m_ServiceEndPointName;
            }
        
        // Accessor for the property "ServiceId"
        /**
         * Getter for property ServiceId.<p>
        * The id of the Service that the sending Member has started.
         */
        public int getServiceId()
            {
            return __m_ServiceId;
            }
        
        // Accessor for the property "ServiceJoinTime"
        /**
         * Getter for property ServiceJoinTime.<p>
        * The cluster time that the Member joined the Service.
         */
        public long getServiceJoinTime()
            {
            return __m_ServiceJoinTime;
            }
        
        // Accessor for the property "ServiceName"
        /**
         * Getter for property ServiceName.<p>
        * The name of the Service.
         */
        public String getServiceName()
            {
            return __m_ServiceName;
            }
        
        // Accessor for the property "ServiceType"
        /**
         * Getter for property ServiceType.<p>
        * The type of the Service.
         */
        public String getServiceType()
            {
            return __m_ServiceType;
            }
        
        // Accessor for the property "ServiceVersion"
        /**
         * Getter for property ServiceVersion.<p>
        * The version of the Service.
         */
        public String getServiceVersion()
            {
            return __m_ServiceVersion;
            }
        
        // Declared at the super level
        protected com.tangosol.coherence.component.net.Poll instantiatePoll()
            {
            // import Component.Net.Poll;
            
            return (Poll) _newChild("Poll");
            }
        
        // Declared at the super level
        /**
         * This is the event that is executed when a Message is received.
        * <p>
        * It is the main processing event of the Message called by the
        * <code>Service.onMessage()</code> event. With regards to the use of
        * Message components within clustered Services, Services are designed
        * by dragging Message components into them as static children. These
        * Messages are the components that a Service can send to other running
        * instances of the same Service within a cluster. When the onReceived
        * event is invoked by a Service, it means that the Message has been
        * received; the code in the onReceived event is therefore the Message
        * specific logic for processing a received Message. For example, when
        * onReceived is invoked on a Message named FindData, the onReceived
        * event should do the work to "find the data", because it is being
        * invoked by the Service that received the "find the data" Message.
         */
        public void onReceived()
            {
            // import Component.Net.Member;
            // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet;
            // import Component.Net.Poll as com.tangosol.coherence.component.net.Poll;
            // import Component.Net.ServiceInfo;
            // import java.util.Iterator;
            // import java.util.Map;
            
            super.onReceived();
            
            // this Message brings information about a Member joining a Service
            
            ClusterService service = (ClusterService) getService();
            int     nMember = getMemberId();
            Member  member  = service.getClusterMemberSet().getMember(nMember);
            
            if (member == null)
                {
                // it should not possible to receive the service info from the senior before
                // we know about the member's existence, but we may learn about the departure
                // before the senior does (via TcpRing) 
                String sReason = "Member " + nMember
                        + (service.getClusterMemberSet().getRecycleSet().getMember(nMember) == null
                            ? " is unknown to" : " is detected as departed by")
                        + " member " + service.getThisMember().getId();
            
                proceed(sReason);
                return;
                }
            
            Member      memberThis = service.getThisMember();
            long        ldtJoined  = getServiceJoinTime();
            ServiceInfo info;
            
            if (member == memberThis)
                {
                // this is a response to our ServiceJoinRequest poll from the senior
                int nServiceId = getServiceId();
                info = service.getServiceInfo(nServiceId);
                _assert(info != null);
            
                com.tangosol.coherence.component.net.Poll pollJoinRequest = getPoll();
                if (pollJoinRequest == null || pollJoinRequest.isClosed())
                    {
                    _trace("Ignoring delayed response to JoinRequest for " + getServiceName() +
                           " at " + ServiceMemberSet.formatJoinTime(ldtJoined), 3);
            
                    // need to respond to the poll anyway
                    proceed(null);
                    return;
                    }
                }
            else
                {
                // register the service
                info = service.ensureServiceInfo(getServiceId(), getServiceName(), getServiceType());
                }
            
            ServiceMemberSet setMember = info.getMemberSet();
            
            if (setMember.contains(member))
                {
                if (member == memberThis)
                    {
                    throw new IllegalStateException(
                        "Service " + getServiceName() +
                        " joining request for this member is out of order");
                    }
            
                // this could happen if a [former] senior processed the JoinRequest,
                // informed this member via ServiceJoining, and then departed BEFORE
                // responding to the requestor; recycle the corresponding info in the
                // service's ServiceMemberSet and release a bus connection (if exists)
                int nState = setMember.getState(nMember);
            
                setMember.remove(member);
                service.onServiceLeft(info, member, nState);
                }
            
            if (setMember.contains(memberThis) &&
                ldtJoined < setMember.getServiceJoinTime(memberThis.getId()))
                {
                // this should not be able to happen
                _trace("Service " + getServiceName() + " joining request for member " +
                    member.getId() + " (joinTime=" + ServiceMemberSet.formatJoinTime(ldtJoined) +
                    ") out of order:\n" + setMember, 1);
            
                // reject
                proceed("New service member is older than member" + memberThis.getId());
                return;
                }
            
            // update the info's ServiceMemberSet atomically
            synchronized (setMember)
                {
                setMember.add(member);
                setMember.setServiceVersion(nMember, getServiceVersion());
                setMember.setServiceJoinTime(nMember, ldtJoined);
                setMember.setServiceJoining(nMember);
                setMember.setServiceEndPointName(nMember, getServiceEndPointName());
                setMember.updateMemberConfigMap(nMember, getMemberConfigMap());    
                }
            
            service.onServiceJoining(info, member);
            
            // validate and continue on the service thread, then come back to "proceed"
            service.validateNewService(info, member, this, getPermissionInfo());
            }
        
        // From interface: com.oracle.coherence.common.base.Continuation
        public void proceed(Object oResult)
            {
            ClusterService service = (ClusterService) getService();
            
            ClusterService.ServiceUpdateResponse msgResponse = (ClusterService.ServiceUpdateResponse)
                service.instantiateMessage("ServiceUpdateResponse");
            msgResponse.respondTo(this);
            
            if (oResult instanceof String ||
                oResult instanceof Exception)  // including SecurityException
                {
                // the join request has been rejected by this node
                service.getServiceInfo(getServiceId()).getMemberSet().remove(getMemberId());
                }
            
            msgResponse.setResult(oResult);
            
            service.send(msgResponse);
            }
        
        // Declared at the super level
        public void read(com.tangosol.io.ReadBuffer.BufferInput input)
                throws java.io.IOException
            {
            // import com.tangosol.net.security.PermissionInfo;
            // import java.io.IOException;
            // import java.util.Map;
            
            try
                {
                setMemberId(input.readUnsignedShort());
                setServiceId(input.readUnsignedShort());
                setServiceName(input.readUTF());
                setServiceType(input.readUTF());
                setServiceVersion(input.readUTF());
                setServiceEndPointName(input.readUTF());
                setServiceJoinTime(input.readLong());
                setMemberConfigMap((Map) readObject(input));
                setPermissionInfo((PermissionInfo) readObject(input));    
                }
            catch(IOException e)
                {
                getService().onConfigIOException(e, getFromMember());
                }
            }
        
        // Accessor for the property "JoinRequest"
        /**
         * Setter for property JoinRequest.<p>
        * Transient property representing the original join request.
         */
        public void setJoinRequest(ClusterService.ServiceJoinRequest pJoinRequest)
            {
            __m_JoinRequest = pJoinRequest;
            }
        
        // Accessor for the property "MemberConfigMap"
        /**
         * Setter for property MemberConfigMap.<p>
        * Snapshot of the Member's own configuration map for the Service at the
        * point in time that the Service was joined.
         */
        public void setMemberConfigMap(java.util.Map map)
            {
            __m_MemberConfigMap = map;
            }
        
        // Accessor for the property "MemberId"
        /**
         * Setter for property MemberId.<p>
        * The id of the member that joined the Service has started.
         */
        public void setMemberId(int pMemberId)
            {
            __m_MemberId = pMemberId;
            }
        
        // Accessor for the property "PermissionInfo"
        /**
         * Setter for property PermissionInfo.<p>
        * The information needed to validate and respond to a security related
        * part of the join request. Initially, the value is used for security
        * validation. When this message is used as a response to the join
        * request, the property contains a response permission info.
         */
        public void setPermissionInfo(com.tangosol.net.security.PermissionInfo infoPermission)
            {
            __m_PermissionInfo = infoPermission;
            }
        
        // Accessor for the property "ServiceEndPointName"
        /**
         * Setter for property ServiceEndPointName.<p>
        * The canonical EndPoint name for the joining service.
         */
        public void setServiceEndPointName(String sName)
            {
            __m_ServiceEndPointName = sName;
            }
        
        // Accessor for the property "ServiceId"
        /**
         * Setter for property ServiceId.<p>
        * The id of the Service that the sending Member has started.
         */
        public void setServiceId(int nId)
            {
            __m_ServiceId = nId;
            }
        
        // Accessor for the property "ServiceJoinTime"
        /**
         * Setter for property ServiceJoinTime.<p>
        * The cluster time that the Member joined the Service.
         */
        public void setServiceJoinTime(long lMillis)
            {
            __m_ServiceJoinTime = lMillis;
            }
        
        // Accessor for the property "ServiceName"
        /**
         * Setter for property ServiceName.<p>
        * The name of the Service.
         */
        public void setServiceName(String sName)
            {
            __m_ServiceName = sName;
            }
        
        // Accessor for the property "ServiceType"
        /**
         * Setter for property ServiceType.<p>
        * The type of the Service.
         */
        public void setServiceType(String pServiceType)
            {
            __m_ServiceType = pServiceType;
            }
        
        // Accessor for the property "ServiceVersion"
        /**
         * Setter for property ServiceVersion.<p>
        * The version of the Service.
         */
        public void setServiceVersion(String sVersion)
            {
            __m_ServiceVersion = sVersion;
            }
        
        // Declared at the super level
        public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
                throws java.io.IOException
            {
            output.writeShort(getMemberId());
            output.writeShort(getServiceId());
            output.writeUTF(getServiceName());
            output.writeUTF(getServiceType());
            output.writeUTF(getServiceVersion());
            output.writeUTF(getServiceEndPointName());
            output.writeLong(getServiceJoinTime());
            writeObject(output, getMemberConfigMap());
            writeObject(output, getPermissionInfo());
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService$ServiceJoining$Poll
        
        /**
         * The Poll contains information regarding a request sent to one or
         * more Cluster Members that require responses. A Service may poll
         * other Members that are running the same Service, and the Poll is
         * used to wait for and assemble the responses from each of those
         * Members. A client thread may also use the Poll to block on a
         * response or set of responses, thus waiting for the completion of the
         * Poll. In its simplest form, which is a Poll that is sent to one
         * Member of the Cluster, the Poll actually represents the
         * request/response model.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class Poll
                extends    com.tangosol.coherence.component.net.Poll
            {
            // ---- Fields declarations ----
            
            // Default constructor
            public Poll()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public Poll(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
                {
                super(sName, compParent, false);
                
                if (fInit)
                    {
                    __init();
                    }
                }
            
            // Main initializer
            public void __init()
                {
                // private initialization
                __initPrivate();
                
                
                // signal the end of the initialization
                set_Constructed(true);
                }
            
            // Private initializer
            protected void __initPrivate()
                {
                
                super.__initPrivate();
                }
            
            //++ getter for static property _Instance
            /**
             * Getter for property _Instance.<p>
            * Auto generated
             */
            public static com.tangosol.coherence.Component get_Instance()
                {
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService.ServiceJoining.Poll();
                }
            
            //++ getter for static property _CLASS
            /**
             * Getter for property _CLASS.<p>
            * Property with auto-generated accessor that returns the Class
            * object for a given component.
             */
            public static Class get_CLASS()
                {
                Class clz;
                try
                    {
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ClusterService$ServiceJoining$Poll".replace('/', '.'));
                    }
                catch (ClassNotFoundException e)
                    {
                    throw new NoClassDefFoundError(e.getMessage());
                    }
                return clz;
                }
            
            //++ getter for autogen property _Module
            /**
             * This is an auto-generated method that returns the global [design
            * time] parent component.
            * 
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            private com.tangosol.coherence.Component get_Module()
                {
                return this.get_Parent().get_Parent();
                }
            
            // Declared at the super level
            /**
             * This is the event that is executed when all the Members that were
            * polled have responded or have left the Service.
             */
            protected void onCompletion()
                {
                // import Component.Net.Member;
                // import com.tangosol.net.security.PermissionInfo;
                
                ClusterService service = (ClusterService) getService();
                
                if (service.isRunning())
                    {
                    ClusterService.ServiceJoining     msgJoining = (ClusterService.ServiceJoining) get_Parent();
                    ClusterService.ServiceJoinRequest msgRequest = msgJoining.getJoinRequest();
                
                    if (msgRequest == null)
                        {
                        // this is the "final" acknowledgment from the member that
                        // requested to join a service
                        }
                    else
                        {
                        // respond with ServiceJoining only to a different member and only upon a success;
                        // otherwise (this is the senior or someone has rejected) the default response will suffice
                        Object oResult = getResult();
                        Member member  = msgRequest.getFromMember();
                
                        if (oResult instanceof String || oResult instanceof Exception ||
                            member == service.getThisMember())
                            {
                            ClusterService.ServiceUpdateResponse msg = (ClusterService.ServiceUpdateResponse)
                                    service.instantiateMessage("ServiceUpdateResponse");
                            msg.respondTo(msgRequest);
                            msg.setResult(oResult);
                
                            service.send(msg);
                            }
                        else
                            {
                            ClusterService.ServiceJoining msg = (ClusterService.ServiceJoining)
                                    service.instantiateMessage("ServiceJoining");
                            msg.respondTo(msgRequest);
                            msg.setMemberId(msgJoining.getMemberId());
                            msg.setServiceId(msgJoining.getServiceId());
                            msg.setServiceJoinTime(msgJoining.getServiceJoinTime());
                            msg.setServiceName(msgJoining.getServiceName());
                            msg.setServiceType(msgJoining.getServiceType());
                            msg.setServiceVersion(msgJoining.getServiceVersion());
                            msg.setServiceEndPointName(msgJoining.getServiceEndPointName());
                            msg.setMemberConfigMap(msgJoining.getMemberConfigMap());
                
                            // we are reusing the PermissionInfo property to send the response back to the caller
                            msg.setPermissionInfo((PermissionInfo) oResult);
                            service.registerServiceJoining(msg);
                            service.send(msg);
                            }
                        }
                
                    service.unregisterServiceJoining(msgJoining);
                    }
                
                super.onCompletion();
                }
            
            // Declared at the super level
            /**
             * This event occurs for each response Message from each polled
            * Member.
             */
            public void onResponse(com.tangosol.coherence.component.net.Message msg)
                {
                setResult(((ClusterService.ServiceUpdateResponse) msg).getResult());
                
                super.onResponse(msg);
                }
            
            // Declared at the super level
            /**
             * Setter for property Result.<p>
            * The result of the Poll. This property is used to collect the
            * result of the Poll and return it to the client thread that sent
            * the original RequestMessage.
             */
            public void setResult(Object oResult)
                {
                // import com.tangosol.net.security.PermissionInfo;
                
                if (oResult != null)
                    {
                    Object oResultPrev = getResult();
                    if (oResultPrev == null)
                        {
                        super.setResult(oResult);
                        }
                    else if (oResultPrev instanceof PermissionInfo)
                        {
                        // assert !(oResult instanceof PersmissionInfo)
                        if (oResult instanceof String || oResult instanceof Exception)
                            {
                            // permission info can only be overwritten with a rejection
                            super.setResult(oResult);
                            }
                        }
                    else if (oResultPrev instanceof String)
                        {
                        if (oResult instanceof Exception)
                            {
                            // soft rejection can only be overwritten with a hard rejection
                            super.setResult(oResult);
                            }
                        }
                    else if (oResultPrev instanceof Exception)
                        {
                        // this is a hard rejection; cannot be overwritten
                        return;
                        }
                    }
                }
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService$ServiceJoinRequest
    
    /**
     * RequestMessage:
     *     ServiceJoinRequest (poll)
     * 
     * Purpose:
     *   Request sent to the senior Member to join a Service.
     * 
     * Description:
     *     When a Member wishes to start a Service which has already been
     * registered, it requests the senior Member to assign the ServiceJoined
     * timestamp for this Service on this Member. The senior Member assigns a
     * unique timestamp, notifies all Members using the ServiceJoined poll and
     * upon receiving the delivery confirmation responds to the requesting
     * Member.
     * 
     * Attributes:
     *     ServiceId
     *     ServiceVersion
     *     ServiceEndPoint (as of 3.7)
     *     MemberConfigMap
     *     PermissionInfo [as of 12.1.2]
     * 
     * Response to:
     *     n/a
     * 
     * Expected responses:
     *     ServiceJoining
     *     ServiceUpdateResponse
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class ServiceJoinRequest
            extends    com.tangosol.coherence.component.net.message.RequestMessage
            implements com.oracle.coherence.common.base.Continuation
        {
        // ---- Fields declarations ----
        
        /**
         * Property MemberConfigMap
         *
         * Snapshot of the Member's own configuration map for the Service at
         * the point in time that the Service was joined.
         */
        private java.util.Map __m_MemberConfigMap;
        
        /**
         * Property PermissionInfo
         *
         * The information needed to validate a security related (optional)
         * part of the join request.
         */
        private com.tangosol.net.security.PermissionInfo __m_PermissionInfo;
        
        /**
         * Property ServiceEndPointName
         *
         * The canonical EndPoint name for the joining service.
         */
        private String __m_ServiceEndPointName;
        
        /**
         * Property ServiceId
         *
         * The id of the Service that the sending Member has started.
         */
        private int __m_ServiceId;
        
        /**
         * Property ServiceVersion
         *
         * The version of the Service.
         */
        private String __m_ServiceVersion;
        private static com.tangosol.util.ListMap __mapChildren;
        
        // Static initializer
        static
            {
            __initStatic();
            }
        
        // Default static initializer
        private static void __initStatic()
            {
            // register child classes
            __mapChildren = new com.tangosol.util.ListMap();
            __mapChildren.put("Poll", ClusterService.ServiceJoinRequest.Poll.get_CLASS());
            }
        
        // Default constructor
        public ServiceJoinRequest()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public ServiceJoinRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);
            
            if (fInit)
                {
                __init();
                }
            }
        
        // Main initializer
        public void __init()
            {
            // private initialization
            __initPrivate();
            
            // state initialization: public and protected properties
            try
                {
                setMessageType(42);
                }
            catch (java.lang.Exception e)
                {
                // re-throw as a runtime exception
                throw new com.tangosol.util.WrapperException(e);
                }
            
            // containment initialization: children
            
            // signal the end of the initialization
            set_Constructed(true);
            }
        
        // Private initializer
        protected void __initPrivate()
            {
            
            super.__initPrivate();
            }
        
        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
        * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService.ServiceJoinRequest();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ClusterService$ServiceJoinRequest".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }
        
        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
        * time] parent component.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent();
            }
        
        //++ getter for autogen property _ChildClasses
        /**
         * This is an auto-generated method that returns the map of design time
        * [static] children.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        protected java.util.Map get_ChildClasses()
            {
            return __mapChildren;
            }
        
        // Declared at the super level
        /**
         * Getter for property Description.<p>
        * Used for debugging purposes (from toString). Create a human-readable
        * description of the specific Message data.
         */
        public String getDescription()
            {
            return "ServiceId="         + getServiceId()
                 + "\nServiceVersion="  + getServiceVersion()
                 + "\nServiceEndPoint=" + getServiceEndPointName()
                 + "\nMemberConfigMap=" + getMemberConfigMap();
            }
        
        // Accessor for the property "MemberConfigMap"
        /**
         * Getter for property MemberConfigMap.<p>
        * Snapshot of the Member's own configuration map for the Service at the
        * point in time that the Service was joined.
         */
        public java.util.Map getMemberConfigMap()
            {
            return __m_MemberConfigMap;
            }
        
        // Accessor for the property "PermissionInfo"
        /**
         * Getter for property PermissionInfo.<p>
        * The information needed to validate a security related (optional) part
        * of the join request.
         */
        public com.tangosol.net.security.PermissionInfo getPermissionInfo()
            {
            return __m_PermissionInfo;
            }
        
        // Accessor for the property "ServiceEndPointName"
        /**
         * Getter for property ServiceEndPointName.<p>
        * The canonical EndPoint name for the joining service.
         */
        public String getServiceEndPointName()
            {
            return __m_ServiceEndPointName;
            }
        
        // Accessor for the property "ServiceId"
        /**
         * Getter for property ServiceId.<p>
        * The id of the Service that the sending Member has started.
         */
        public int getServiceId()
            {
            return __m_ServiceId;
            }
        
        // Accessor for the property "ServiceVersion"
        /**
         * Getter for property ServiceVersion.<p>
        * The version of the Service.
         */
        public String getServiceVersion()
            {
            return __m_ServiceVersion;
            }
        
        // Declared at the super level
        protected com.tangosol.coherence.component.net.Poll instantiatePoll()
            {
            // import Component.Net.Poll;
            
            return (Poll) _newChild("Poll");
            }
        
        // Declared at the super level
        /**
         * This is the event that is executed when a Message is received.
        * <p>
        * It is the main processing event of the Message called by the
        * <code>Service.onMessage()</code> event. With regards to the use of
        * Message components within clustered Services, Services are designed
        * by dragging Message components into them as static children. These
        * Messages are the components that a Service can send to other running
        * instances of the same Service within a cluster. When the onReceived
        * event is invoked by a Service, it means that the Message has been
        * received; the code in the onReceived event is therefore the Message
        * specific logic for processing a received Message. For example, when
        * onReceived is invoked on a Message named FindData, the onReceived
        * event should do the work to "find the data", because it is being
        * invoked by the Service that received the "find the data" Message.
         */
        public void onReceived()
            {
            // import Component.Net.Member;
            // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet;
            // import Component.Net.ServiceInfo;
            // import java.util.Iterator;
            
            super.onReceived();
            
            // this Message brings information about a Member joining a Service
            // and should have only been directed here if this was the senior cluster Member;
            // however, it is possible that this Member is not yet aware that it is the senior
            
            ClusterService     service  = (ClusterService) getService();
            int         nService = getServiceId();
            ServiceInfo info     = service.getServiceInfo(nService);
            if (info == null)
                {
                // this can only happen if the senior died before
                // passing the registration info to this member;
                // empty response will force re-registering
                ClusterService.ServiceUpdateResponse msg = (ClusterService.ServiceUpdateResponse)
                    service.instantiateMessage("ServiceUpdateResponse");
                msg.respondTo(this);
                service.send(msg);
                }
            else
                {
                ServiceMemberSet setMember = info.getMemberSet();
                Member           member    = getFromMember();
            
                if (setMember.contains(member))
                    {
                    // this could happen if a [former] senior processed the JoinRequest,
                    // informed this member via ServiceJoining, and then departed BEFORE
                    // responding to the requestor; recycle the corresponding info in the
                    // service's ServiceMemberSet and release a bus connection (if exists)
                    int nState = setMember.getState(member.getId());
            
                    setMember.remove(member);
                    service.onServiceLeft(info, member, nState);
                    }
            
                // it's very important to use senior's time as the "ServiceJoined" value
                // and make sure that there's no other service with the same timestamp
                long ldtJoined = service.getTimestamp();
                int  nMember   = member.getId();
            
                for (Iterator iter = setMember.iterator(); iter.hasNext();)
                    {
                    long ldtOld = setMember.getServiceJoinTime(((Member) iter.next()).getId());
                    if (ldtJoined <= ldtOld)
                        {
                        ldtJoined = ldtOld + 1;
                        }
                    }
            
                // update the info's ServiceMemberSet atomically
                synchronized (setMember)
                    {
                    setMember.add(member);
                    setMember.setServiceVersion(nMember, getServiceVersion());
                    setMember.setServiceJoinTime(nMember, ldtJoined);
                    setMember.setServiceJoining(nMember);
                    setMember.setServiceEndPointName(nMember, getServiceEndPointName());
                    setMember.updateMemberConfigMap(nMember, getMemberConfigMap());
                    }
            
                service.onServiceJoining(info, member);
            
                // validate and continue on the service thread; then come back to "proceed"
                service.validateNewService(info, member, this, getPermissionInfo());
                }
            }
        
        // From interface: com.oracle.coherence.common.base.Continuation
        public void proceed(Object oResult)
            {
            // import Component.Net.Member;
            // import Component.Net.MemberSet;
            // import Component.Net.ServiceInfo;
            // import com.tangosol.net.security.PermissionInfo;
            
            ClusterService     service  = (ClusterService) getService();
            Member      member   = getFromMember();
            int         nMember  = member.getId();
            int         nService = getServiceId();
            ServiceInfo info     = service.getServiceInfo(nService);
            
            _assert(info != null);
            
            if (oResult instanceof SecurityException)
                {
                _trace("member left due to security exception", 1);
                service.doMemberLeft(member);
            
                // since the member is removed, no need to "proceed" with response
                return;
                }
            
            if (oResult instanceof String || oResult instanceof Exception)
                {
                // the join request has been rejected by this node's Service
                info.getMemberSet().remove(member);
            
                ClusterService.ServiceUpdateResponse msg = (ClusterService.ServiceUpdateResponse)
                        service.instantiateMessage("ServiceUpdateResponse");
                msg.respondTo(this);
                msg.setResult(oResult);
            
                service.send(msg);
                }
            else
                {
                // update all other cluster Members with a ServiceJoining
                // and wait for delivery; only then reply to the requestor
                // see ServiceJoining$Poll.onCompletion()
                MemberSet setOthers = service.getOthersMemberSet();
                setOthers.remove(member);
                ClusterService.ServiceJoining msg = (ClusterService.ServiceJoining)
                    service.instantiateMessage("ServiceJoining");
                msg.setMemberId(nMember);
                msg.setServiceId(nService);
                msg.setServiceName(info.getServiceName());
                msg.setServiceType(info.getServiceType());
                msg.setServiceJoinTime(info.getServiceJoinTime(nMember));
                msg.setServiceVersion(getServiceVersion());
                msg.setServiceEndPointName(getServiceEndPointName());
                msg.setMemberConfigMap(getMemberConfigMap());
                msg.setToMemberSet(setOthers);
                msg.setJoinRequest(this);
                msg.setPermissionInfo(getPermissionInfo());
            
                // save off the [possible] result (which could be null)
                // to send back to the requestor
                ((ClusterService.ServiceJoining.Poll) msg.ensureRequestPoll()).setResult(
                    (PermissionInfo) oResult);
            
                // don't sign up new members till this request is fully ack'd
                service.registerServiceJoining(msg);
            
                service.send(msg); // see ServiceJoining$Poll#onCompletion()
                }
            }
        
        // Declared at the super level
        public void read(com.tangosol.io.ReadBuffer.BufferInput input)
                throws java.io.IOException
            {
            // import com.tangosol.net.security.PermissionInfo;
            // import java.io.IOException;
            // import java.util.Map;
            
            try
                {
                setServiceId(input.readUnsignedShort());
                setServiceVersion(input.readUTF());
                setServiceEndPointName(input.readUTF());
                setMemberConfigMap((Map) readObject(input));
                setPermissionInfo((PermissionInfo) readObject(input));
                }
            catch (IOException e)
                {
                getService().onConfigIOException(e, getFromMember());    
                }
            }
        
        // Accessor for the property "MemberConfigMap"
        /**
         * Setter for property MemberConfigMap.<p>
        * Snapshot of the Member's own configuration map for the Service at the
        * point in time that the Service was joined.
         */
        public void setMemberConfigMap(java.util.Map map)
            {
            __m_MemberConfigMap = map;
            }
        
        // Accessor for the property "PermissionInfo"
        /**
         * Setter for property PermissionInfo.<p>
        * The information needed to validate a security related (optional) part
        * of the join request.
         */
        public void setPermissionInfo(com.tangosol.net.security.PermissionInfo service)
            {
            __m_PermissionInfo = service;
            }
        
        // Accessor for the property "ServiceEndPointName"
        /**
         * Setter for property ServiceEndPointName.<p>
        * The canonical EndPoint name for the joining service.
         */
        public void setServiceEndPointName(String sEndPoint)
            {
            __m_ServiceEndPointName = sEndPoint;
            }
        
        // Accessor for the property "ServiceId"
        /**
         * Setter for property ServiceId.<p>
        * The id of the Service that the sending Member has started.
         */
        public void setServiceId(int nId)
            {
            __m_ServiceId = nId;
            }
        
        // Accessor for the property "ServiceVersion"
        /**
         * Setter for property ServiceVersion.<p>
        * The version of the Service.
         */
        public void setServiceVersion(String sVersion)
            {
            __m_ServiceVersion = sVersion;
            }
        
        // Declared at the super level
        public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
                throws java.io.IOException
            {
            output.writeShort(getServiceId());
            output.writeUTF(getServiceVersion());
            output.writeUTF(getServiceEndPointName());
            writeObject(output, getMemberConfigMap());
            writeObject(output, getPermissionInfo());
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService$ServiceJoinRequest$Poll
        
        /**
         * The Poll contains information regarding a request sent to one or
         * more Cluster Members that require responses. A Service may poll
         * other Members that are running the same Service, and the Poll is
         * used to wait for and assemble the responses from each of those
         * Members. A client thread may also use the Poll to block on a
         * response or set of responses, thus waiting for the completion of the
         * Poll. In its simplest form, which is a Poll that is sent to one
         * Member of the Cluster, the Poll actually represents the
         * request/response model.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class Poll
                extends    com.tangosol.coherence.component.net.Poll
            {
            // ---- Fields declarations ----
            
            // Default constructor
            public Poll()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public Poll(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
                {
                super(sName, compParent, false);
                
                if (fInit)
                    {
                    __init();
                    }
                }
            
            // Main initializer
            public void __init()
                {
                // private initialization
                __initPrivate();
                
                
                // signal the end of the initialization
                set_Constructed(true);
                }
            
            // Private initializer
            protected void __initPrivate()
                {
                
                super.__initPrivate();
                }
            
            //++ getter for static property _Instance
            /**
             * Getter for property _Instance.<p>
            * Auto generated
             */
            public static com.tangosol.coherence.Component get_Instance()
                {
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService.ServiceJoinRequest.Poll();
                }
            
            //++ getter for static property _CLASS
            /**
             * Getter for property _CLASS.<p>
            * Property with auto-generated accessor that returns the Class
            * object for a given component.
             */
            public static Class get_CLASS()
                {
                Class clz;
                try
                    {
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ClusterService$ServiceJoinRequest$Poll".replace('/', '.'));
                    }
                catch (ClassNotFoundException e)
                    {
                    throw new NoClassDefFoundError(e.getMessage());
                    }
                return clz;
                }
            
            //++ getter for autogen property _Module
            /**
             * This is an auto-generated method that returns the global [design
            * time] parent component.
            * 
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            private com.tangosol.coherence.Component get_Module()
                {
                return this.get_Parent().get_Parent();
                }
            
            // Declared at the super level
            /**
             * This is the event that is executed when all the Members that were
            * polled have responded or have left the Service.
             */
            protected void onCompletion()
                {
                // import Component.Net.Cluster;
                // import Component.Net.Member;
                // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet;
                // import Component.Net.Security;
                // import Component.Net.ServiceInfo;
                // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
                // import Component.Util.Daemon.QueueProcessor.Service.Grid$NotifyServiceQuiescence as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyServiceQuiescence;
                // import com.tangosol.net.security.PermissionInfo;
                
                if (getRespondedMemberSet().isEmpty())
                    {
                    // the poll was interrupted or timed out
                    }
                else
                    {
                    ClusterService             service        = (ClusterService) getService();
                    ClusterService.ServiceJoinRequest msgRequest     = (ClusterService.ServiceJoinRequest) get_Parent();
                    int                 nServiceId     = msgRequest.getServiceId();
                    ServiceMemberSet    setInfoMember  = service.getServiceInfo(nServiceId).getMemberSet();
                    com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid             serviceJoining = service.getService(nServiceId);
                    Member              memberThis     = service.getThisMember();
                    Object              oResult        = getResult();
                    Security            security       = nServiceId > Cluster.MAX_SYSTEM_SERVICE
                        ? Security.getInstance()
                        : null; // no security for system services
                
                    if (serviceJoining != null && setInfoMember.getServiceJoinTime(memberThis.getId()) > 0L
                        && ((security == null && oResult == null) ||
                            (security != null &&
                             (oResult instanceof PermissionInfo ||
                              oResult  == null && memberThis == setInfoMember.getOldestMember()))))
                        {
                        // copy the info's ServiceMemberSet into the Service's one
                        ServiceMemberSet setServiceMember = new ServiceMemberSet();
                        setServiceMember.copy(setInfoMember);
                        setServiceMember.setThisMember(memberThis);
                
                        // this is the "crossing line"; after the return this service member
                        // can communicate with other service members over the service end point
                        serviceJoining.setServiceMemberSet(setServiceMember);
                
                        // Note: there must have been no changes to the config map from the point
                        // the ServiceJoinRequest was sent (see doServiceJoining) till poll completion
                        _assert(serviceJoining.getThisMemberConfigMap().equals(msgRequest.getMemberConfigMap()));
                
                        if (service.getServiceInfo(nServiceId).isSuspended())
                            {
                            com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyServiceQuiescence msg = (com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyServiceQuiescence)
                                    serviceJoining.instantiateMessage("NotifyServiceQuiescence");
                            msg.addToMember(memberThis);
                            msg.setResume(/*fResume*/ false);
                            msg.post();
                            }
                        }
                    else 
                        {
                        setInfoMember.remove(memberThis);
                        }
                    }
                
                super.onCompletion();
                }
            
            // Declared at the super level
            /**
             * This event occurs for each response Message from each polled
            * Member.
             */
            public void onResponse(com.tangosol.coherence.component.net.Message msg)
                {
                if (msg instanceof ClusterService.ServiceUpdateResponse)
                    {
                    setResult(((ClusterService.ServiceUpdateResponse) msg).getResult());
                    }
                else if (msg instanceof ClusterService.ServiceJoining)
                    {
                    setResult(((ClusterService.ServiceJoining) msg).getPermissionInfo());
                    }
                
                super.onResponse(msg);
                }
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService$ServiceLeaving
    
    /**
     * Message:
     *     ServiceLeaving
     * 
     * Purpose:
     *     Informs all Members of the cluster that a Member is starting the
     * process of leaving (stopping) a Service.
     * 
     * Description:
     *     When a cluster has a Service registered, and a Member is running
     * that Service, and that Member subsequently decides to stop the Service,
     * this Message is sent to tell the cluster Members that the Service is
     * going to be stopped on that Member.
     * 
     * Attributes:
     *     ServiceId
     *     ServiceJoined
     * 
     * Response to:
     *     n/a
     * 
     * Expected responses:
     *     n/a
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class ServiceLeaving
            extends    com.tangosol.coherence.component.net.Message
        {
        // ---- Fields declarations ----
        
        /**
         * Property ServiceId
         *
         * The id of the Service that the sending Member is stopping.
         */
        private int __m_ServiceId;
        
        /**
         * Property ServiceJoinTime
         *
         * The cluster time that the Member joined the Service.
         */
        private long __m_ServiceJoinTime;
        
        // Default constructor
        public ServiceLeaving()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public ServiceLeaving(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);
            
            if (fInit)
                {
                __init();
                }
            }
        
        // Main initializer
        public void __init()
            {
            // private initialization
            __initPrivate();
            
            // state initialization: public and protected properties
            try
                {
                setMessageType(44);
                }
            catch (java.lang.Exception e)
                {
                // re-throw as a runtime exception
                throw new com.tangosol.util.WrapperException(e);
                }
            
            // signal the end of the initialization
            set_Constructed(true);
            }
        
        // Private initializer
        protected void __initPrivate()
            {
            
            super.__initPrivate();
            }
        
        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
        * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService.ServiceLeaving();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ClusterService$ServiceLeaving".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }
        
        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
        * time] parent component.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent();
            }
        
        // Declared at the super level
        /**
         * Getter for property Description.<p>
        * Used for debugging purposes (from toString). Create a human-readable
        * description of the specific Message data.
         */
        public String getDescription()
            {
            return "ServiceId=" + getServiceId();
            }
        
        // Accessor for the property "ServiceId"
        /**
         * Getter for property ServiceId.<p>
        * The id of the Service that the sending Member is stopping.
         */
        public int getServiceId()
            {
            return __m_ServiceId;
            }
        
        // Accessor for the property "ServiceJoinTime"
        /**
         * Getter for property ServiceJoinTime.<p>
        * The cluster time that the Member joined the Service.
         */
        public long getServiceJoinTime()
            {
            return __m_ServiceJoinTime;
            }
        
        // Declared at the super level
        /**
         * This is the event that is executed when a Message is received.
        * <p>
        * It is the main processing event of the Message called by the
        * <code>Service.onMessage()</code> event. With regards to the use of
        * Message components within clustered Services, Services are designed
        * by dragging Message components into them as static children. These
        * Messages are the components that a Service can send to other running
        * instances of the same Service within a cluster. When the onReceived
        * event is invoked by a Service, it means that the Message has been
        * received; the code in the onReceived event is therefore the Message
        * specific logic for processing a received Message. For example, when
        * onReceived is invoked on a Message named FindData, the onReceived
        * event should do the work to "find the data", because it is being
        * invoked by the Service that received the "find the data" Message.
         */
        public void onReceived()
            {
            // import Component.Net.Member;
            // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet;
            // import Component.Net.ServiceInfo;
            
            super.onReceived();
            
            // this Message brings information about a Member leaving a Service
            ClusterService     service  = (ClusterService) getService();
            int         nService = getServiceId();
            ServiceInfo info     = service.getServiceInfo(nService);
            
            if (info == null)
                {
                _trace("ClusterService$ServiceLeaving: " + "Unknown Service " + nService, 4);
                }
            else
                {
                Member member    = getFromMember();
                long   ldtJoined = getServiceJoinTime();
            
                if (member == service.getThisMember())
                    {
                    // this message came from the service on this node;
                    // publish the event to the rest of the cluster
            
                    ClusterService.ServiceLeaving msg = (ClusterService.ServiceLeaving)
                        service.instantiateMessage("ServiceLeaving");
                    msg.setToMemberSet(service.getOthersMemberSet());
                    msg.setServiceId(nService);
                    msg.setServiceJoinTime(ldtJoined);
                    service.send(msg);
                    }
            
                ServiceMemberSet setMember = info.getMemberSet();
                if (setMember.contains(member))
                    {
                    _assert(ldtJoined > 0L);
            
                    int nMember = member.getId();
            
                    // Since ServiceLeaving comes from the member itself
                    // while ServiceJoined comes from the senior,
                    // there is a [rather theoretical] possibility that the Service has already
                    // restarted and this is an old message that should be ignored
                    if (setMember.getServiceJoinTime(nMember) == ldtJoined)
                        {
                        setMember.setServiceLeaving(nMember);
                        service.onServiceLeaving(info, member);
                        }
                    }
                else
                    {
                    // it looks like the member departure has been detected by other means
                    // since this message came
                    _trace("ClusterService$ServiceLeaving: " + info.getServiceName() +
                           "; " + "Unknown " + member, 6);
                    }
                }
            }
        
        // Declared at the super level
        public void read(com.tangosol.io.ReadBuffer.BufferInput input)
                throws java.io.IOException
            {
            setServiceId(input.readUnsignedShort());
            setServiceJoinTime(input.readLong());
            }
        
        // Accessor for the property "ServiceId"
        /**
         * Setter for property ServiceId.<p>
        * The id of the Service that the sending Member is stopping.
         */
        public void setServiceId(int nId)
            {
            __m_ServiceId = nId;
            }
        
        // Accessor for the property "ServiceJoinTime"
        /**
         * Setter for property ServiceJoinTime.<p>
        * The cluster time that the Member joined the Service.
         */
        public void setServiceJoinTime(long ldtJoined)
            {
            __m_ServiceJoinTime = ldtJoined;
            }
        
        // Declared at the super level
        public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
                throws java.io.IOException
            {
            output.writeShort(getServiceId());
            output.writeLong(getServiceJoinTime());
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService$ServiceLeft
    
    /**
     * Message:
     *     ServiceLeft
     * 
     * Purpose:
     *     Informs all Members of the cluster that a Member has stopped a
     * Service.
     * 
     * Description:
     *     When a cluster has a Service registered, and a Member is running
     * that Service, and that Member subsequently stops the Service, this
     * Message is sent to tell the cluster Members that the Service has stopped
     * on that Member.
     * 
     * Attributes:
     *     ServiceId
     *     ServiceJoined
     * 
     * Response to:
     *     n/a
     * 
     * Expected responses:
     *     n/a
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class ServiceLeft
            extends    com.tangosol.coherence.component.net.Message
        {
        // ---- Fields declarations ----
        
        /**
         * Property MemberLeftId
         *
         * The id of the member that left the service.
         */
        private int __m_MemberLeftId;
        
        /**
         * Property ServiceId
         *
         * The id of the Service that the sending Member has stopped.
         */
        private int __m_ServiceId;
        
        /**
         * Property ServiceJoinTime
         *
         * The cluster time that the Member joined the Service.
         */
        private long __m_ServiceJoinTime;
        
        // Default constructor
        public ServiceLeft()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public ServiceLeft(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);
            
            if (fInit)
                {
                __init();
                }
            }
        
        // Main initializer
        public void __init()
            {
            // private initialization
            __initPrivate();
            
            // state initialization: public and protected properties
            try
                {
                setMessageType(45);
                }
            catch (java.lang.Exception e)
                {
                // re-throw as a runtime exception
                throw new com.tangosol.util.WrapperException(e);
                }
            
            // signal the end of the initialization
            set_Constructed(true);
            }
        
        // Private initializer
        protected void __initPrivate()
            {
            
            super.__initPrivate();
            }
        
        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
        * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService.ServiceLeft();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ClusterService$ServiceLeft".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }
        
        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
        * time] parent component.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent();
            }
        
        // Declared at the super level
        /**
         * Getter for property Description.<p>
        * Used for debugging purposes (from toString). Create a human-readable
        * description of the specific Message data.
         */
        public String getDescription()
            {
            return "ServiceId=" + getServiceId();
            }
        
        // Accessor for the property "MemberLeftId"
        /**
         * Getter for property MemberLeftId.<p>
        * The id of the member that left the service.
         */
        public int getMemberLeftId()
            {
            return __m_MemberLeftId;
            }
        
        // Accessor for the property "ServiceId"
        /**
         * Getter for property ServiceId.<p>
        * The id of the Service that the sending Member has stopped.
         */
        public int getServiceId()
            {
            return __m_ServiceId;
            }
        
        // Accessor for the property "ServiceJoinTime"
        /**
         * Getter for property ServiceJoinTime.<p>
        * The cluster time that the Member joined the Service.
         */
        public long getServiceJoinTime()
            {
            return __m_ServiceJoinTime;
            }
        
        // Declared at the super level
        /**
         * This is the event that is executed when a Message is received.
        * <p>
        * It is the main processing event of the Message called by the
        * <code>Service.onMessage()</code> event. With regards to the use of
        * Message components within clustered Services, Services are designed
        * by dragging Message components into them as static children. These
        * Messages are the components that a Service can send to other running
        * instances of the same Service within a cluster. When the onReceived
        * event is invoked by a Service, it means that the Message has been
        * received; the code in the onReceived event is therefore the Message
        * specific logic for processing a received Message. For example, when
        * onReceived is invoked on a Message named FindData, the onReceived
        * event should do the work to "find the data", because it is being
        * invoked by the Service that received the "find the data" Message.
         */
        public void onReceived()
            {
            // import Component.Net.Member;
            // import Component.Net.MemberSet;
            // import Component.Net.MemberSet.ActualMemberSet.ServiceMemberSet;
            // import Component.Net.ServiceInfo;
            
            super.onReceived();
            
            // this Message brings information about a Member that has left a Service
            ClusterService     service  = (ClusterService) getService();
            int         nService = getServiceId();
            ServiceInfo info     = service.getServiceInfo(nService);
            
            if (info == null)
                {
                _trace("ClusterService$ServiceLeft: " + "Unknown Service " + nService, 4);
                }
            else
                { 
                ServiceMemberSet setMember  = info.getMemberSet();
                Member           memberThis = service.getThisMember();
                int              nMemberId  = getMemberLeftId();
                Member           memberLeft = service.getClusterMemberSet().getMember(nMemberId);
                long             ldtJoined  = getServiceJoinTime();
            
                // duplicate ServiceLeft received; already processed therefore ignore
                if (memberLeft == null)
                    {
                    return;
                    }
            
                if (memberThis == memberLeft ||
                    (memberThis == service.getClusterOldestMember() && setMember.contains(memberLeft)))
                    {
                    // this message came from the service on this node;
                    // publish the event to the rest of the cluster
            
                    ClusterService.ServiceLeft msg   = (ClusterService.ServiceLeft) service.instantiateMessage("ServiceLeft");
                    MemberSet    setTo = service.getOthersMemberSet();
            
                    setTo.remove(nMemberId);
            
                    msg.setToMemberSet(setTo);
                    msg.setServiceId(nService);
                    msg.setMemberLeftId(nMemberId);
            
                    // if the senior is relaying the ServiceLeft we should include
                    // the join time of the leaving member
                    msg.setServiceJoinTime(memberThis != memberLeft && ldtJoined == 0L
                        ? setMember.getServiceJoinTime(nMemberId) : ldtJoined);
            
                    service.send(msg);
                    }
            
                if (setMember.contains(memberLeft))
                    {
                    // Since ServiceLeft could come from the member that stopped the service
                    // while ServiceJoined comes from the senior,
                    // there is a [rather theoretical] possibility that the Service has already
                    // restarted and this is an old message that should be ignored
                    if (setMember.getServiceJoinTime(nMemberId) == ldtJoined || ldtJoined == 0L)
                        {
                        int nState = setMember.getState(nMemberId);
            
                        setMember.remove(memberLeft);
                        service.onServiceLeft(info, memberLeft, nState);
                        }
                    }
                else
                    {
                    // it looks like the member departure has been detected by other means
                    // since this message came
                    _trace("ClusterService$ServiceLeft: " + info.getServiceName() +
                           "; " + "Unknown " + memberLeft, 6);
                    }
                }
            }
        
        // Declared at the super level
        public void read(com.tangosol.io.ReadBuffer.BufferInput input)
                throws java.io.IOException
            {
            // import java.io.EOFException;
            
            setServiceId(input.readUnsignedShort());
            setServiceJoinTime(input.readLong());
            
            try
                {
                setMemberLeftId(input.readUnsignedShort()); // added in 14.1.2
                }
            catch (EOFException e) {};
            }
        
        // Accessor for the property "MemberLeftId"
        /**
         * Setter for property MemberLeftId.<p>
        * The id of the member that left the service.
         */
        public void setMemberLeftId(int nId)
            {
            __m_MemberLeftId = nId;
            }
        
        // Accessor for the property "ServiceId"
        /**
         * Setter for property ServiceId.<p>
        * The id of the Service that the sending Member has stopped.
         */
        public void setServiceId(int nId)
            {
            __m_ServiceId = nId;
            }
        
        // Accessor for the property "ServiceJoinTime"
        /**
         * Setter for property ServiceJoinTime.<p>
        * The cluster time that the Member joined the Service.
         */
        public void setServiceJoinTime(long ldtJoined)
            {
            __m_ServiceJoinTime = ldtJoined;
            }
        
        // Declared at the super level
        public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
                throws java.io.IOException
            {
            output.writeShort(getServiceId());
            output.writeLong(getServiceJoinTime());
            output.writeShort(getMemberLeftId()); // added in 14.1.2
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService$ServiceQuiescenceRequest
    
    /**
     * ServiceQuiescenceRequest is sent (as a chained request) to all Cluster
     * members to indicate that a given service (or all Clustered services) are
     * to be suspended or resumed.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class ServiceQuiescenceRequest
            extends    com.tangosol.coherence.component.net.message.RequestMessage
            implements com.oracle.coherence.common.base.Continuation
        {
        // ---- Fields declarations ----
        
        /**
         * Property PendingCounter
         *
         * Transient counter of the number of (local) services that remain to
         * be suspended/resumed.  See #proceed.
         */
        private transient int __m_PendingCounter;
        
        /**
         * Property Relay
         *
         * True if the recipeient should relay the request to the remainder of
         * the cluster.  This value should be set to true when sending to the
         * senior.
         */
        private boolean __m_Relay;
        
        /**
         * Property Resume
         *
         * Flag that indicates whether the target service should be suspended
         * (false) or resumed (true).
         */
        private boolean __m_Resume;
        
        /**
         * Property ResumeOnFailover
         *
         * Flag that indicates whether the target service(s) should be resumed
         * upon failover.
         * 
         * Specifically this indicates that the suspension of a service was
         * synthetically induced and therefore should be automatically resumed
         * in the case of senior departure.
         */
        private boolean __m_ResumeOnFailover;
        
        /**
         * Property ServiceId
         *
         * The ServiceId of the Service to quiesce (or 0 for ClusterService,
         * representing that all services should be suspended/resumed).
         */
        private int __m_ServiceId;
        private static com.tangosol.util.ListMap __mapChildren;
        
        // Static initializer
        static
            {
            __initStatic();
            }
        
        // Default static initializer
        private static void __initStatic()
            {
            // register child classes
            __mapChildren = new com.tangosol.util.ListMap();
            __mapChildren.put("Poll", ClusterService.ServiceQuiescenceRequest.Poll.get_CLASS());
            }
        
        // Default constructor
        public ServiceQuiescenceRequest()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public ServiceQuiescenceRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);
            
            if (fInit)
                {
                __init();
                }
            }
        
        // Main initializer
        public void __init()
            {
            // private initialization
            __initPrivate();
            
            // state initialization: public and protected properties
            try
                {
                setMessageType(57);
                }
            catch (java.lang.Exception e)
                {
                // re-throw as a runtime exception
                throw new com.tangosol.util.WrapperException(e);
                }
            
            // containment initialization: children
            
            // signal the end of the initialization
            set_Constructed(true);
            }
        
        // Private initializer
        protected void __initPrivate()
            {
            
            super.__initPrivate();
            }
        
        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
        * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService.ServiceQuiescenceRequest();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ClusterService$ServiceQuiescenceRequest".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }
        
        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
        * time] parent component.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent();
            }
        
        //++ getter for autogen property _ChildClasses
        /**
         * This is an auto-generated method that returns the map of design time
        * [static] children.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        protected java.util.Map get_ChildClasses()
            {
            return __mapChildren;
            }
        
        // Declared at the super level
        /**
         * Instantiate a copy of this message. This is quite different from the
        * standard "clone" since only the "transmittable" portion of the
        * message (and none of the internal) state should be cloned.
         */
        public com.tangosol.coherence.component.net.Message cloneMessage()
            {
            ServiceQuiescenceRequest msg = (ClusterService.ServiceQuiescenceRequest) super.cloneMessage();
            
            msg.setServiceId(getServiceId());
            msg.setResume(isResume());
            
            return msg;
            }
        
        // Accessor for the property "PendingCounter"
        /**
         * Getter for property PendingCounter.<p>
        * Transient counter of the number of (local) services that remain to be
        * suspended/resumed.  See #proceed.
         */
        public int getPendingCounter()
            {
            return __m_PendingCounter;
            }
        
        // Accessor for the property "ServiceId"
        /**
         * Getter for property ServiceId.<p>
        * The ServiceId of the Service to quiesce (or 0 for ClusterService,
        * representing that all services should be suspended/resumed).
         */
        public int getServiceId()
            {
            return __m_ServiceId;
            }
        
        // Accessor for the property "Relay"
        /**
         * Getter for property Relay.<p>
        * True if the recipeient should relay the request to the remainder of
        * the cluster.  This value should be set to true when sending to the
        * senior.
         */
        public boolean isRelay()
            {
            return __m_Relay;
            }
        
        // Accessor for the property "Resume"
        /**
         * Getter for property Resume.<p>
        * Flag that indicates whether the target service should be suspended
        * (false) or resumed (true).
         */
        public boolean isResume()
            {
            return __m_Resume;
            }
        
        // Accessor for the property "ResumeOnFailover"
        /**
         * Getter for property ResumeOnFailover.<p>
        * Flag that indicates whether the target service(s) should be resumed
        * upon failover.
        * 
        * Specifically this indicates that the suspension of a service was
        * synthetically induced and therefore should be automatically resumed
        * in the case of senior departure.
         */
        protected boolean isResumeOnFailover()
            {
            return __m_ResumeOnFailover;
            }
        
        // Declared at the super level
        /**
         * This is the event that is executed when a Message is received.
        * <p>
        * It is the main processing event of the Message called by the
        * <code>Service.onMessage()</code> event. With regards to the use of
        * Message components within clustered Services, Services are designed
        * by dragging Message components into them as static children. These
        * Messages are the components that a Service can send to other running
        * instances of the same Service within a cluster. When the onReceived
        * event is invoked by a Service, it means that the Message has been
        * received; the code in the onReceived event is therefore the Message
        * specific logic for processing a received Message. For example, when
        * onReceived is invoked on a Message named FindData, the onReceived
        * event should do the work to "find the data", because it is being
        * invoked by the Service that received the "find the data" Message.
         */
        public void onReceived()
            {
            // import Component.Net.ServiceInfo;
            // import Component.Util.Daemon.QueueProcessor.Service.Grid as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid;
            // import Component.Util.Daemon.QueueProcessor.Service.Grid$NotifyServiceQuiescence as com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyServiceQuiescence;
            
            ClusterService       clusterSvc      = (ClusterService) getService();
            int           nServiceId      = getServiceId();
            int           cPending        = 0;
            boolean       fResume         = isResume();
            boolean       fResumeFailover = isResumeOnFailover();
            ServiceInfo[] aInfo           = nServiceId == 0
                ? clusterSvc.getServiceInfo()                               // nServiceId == 0 implies all services
                : new ServiceInfo[]{clusterSvc.getServiceInfo(nServiceId)}; // else; single service
            
            for (int i = 0, c = aInfo.length; i < c; i++)
                {
                ServiceInfo info = aInfo[i];
                if (info != null)
                    {
                    info.setSuspended(!fResume); // record for future starts
            
                    com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid service = clusterSvc.getService(info.getServiceId());
                    if (service != null)
                        {
                        // notify the related service via a poll
                        com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyServiceQuiescence msg = (com.tangosol.coherence.component.util.daemon.queueProcessor.service.Grid.NotifyServiceQuiescence)
                                service.instantiateMessage("NotifyServiceQuiescence");
                        msg.addToMember(service.getThisMember());
                        msg.setResume(fResume);
                        msg.setResumeOnFailover(fResumeFailover);
            
                        // set up the "return" continuation to the cluster-service thread
                        ClusterService.NotifyResponse msgContinue = (ClusterService.NotifyResponse) clusterSvc.instantiateMessage("NotifyResponse");
                        msgContinue.setService(clusterSvc);
                        msgContinue.setContinuation(this);
            
                        // it will be sent when NotifyServiceQuiesence poll is closed
                        msg.setContinuationMessage(msgContinue);
            
                        service.send(msg); // poll close will invoke continuation
                        ++cPending;
                        }
                    }    
                }
            
            if (isRelay()) // apparently this node is the senior
                {    
                ClusterService.ServiceQuiescenceRequest msg = (ClusterService.ServiceQuiescenceRequest)
                        clusterSvc.instantiateMessage("ServiceQuiescenceRequest");
                msg.setServiceId(nServiceId);
                msg.setResume(fResume);
                msg.setResumeOnFailover(fResumeFailover);
                msg.setRelay(false);
                msg.setToMemberSet(clusterSvc.getOthersMemberSet());
                
                ((ClusterService.ServiceQuiescenceRequest.Poll) msg.ensureRequestPoll()).setContinuation(this);
                ++cPending; // to wait for poll to close before responding to the requestor
            
                msg.post();
                }
            
            if (cPending == 0)
                {
                sendReply();
                }
            else
                {
                // see #proceed
                setPendingCounter(cPending);
                }
            }
        
        // From interface: com.oracle.coherence.common.base.Continuation
        public void proceed(Object oResult)
            {
            int cPending = getPendingCounter() - 1;
            
            setPendingCounter(cPending);
            
            if (cPending == 0)
                {
                sendReply();
                }
            }
        
        // Declared at the super level
        public void read(com.tangosol.io.ReadBuffer.BufferInput input)
                throws java.io.IOException
            {
            super.read(input);
            
            setServiceId(input.readInt());
            setResume(input.readBoolean());
            setResumeOnFailover(input.readBoolean());
            setRelay(input.readBoolean());
            }
        
        /**
         * Send reply to the original request.
         */
        protected void sendReply()
            {
            // import Component.Net.Message;
            
            Message msgResp = getService().instantiateMessage("Response");
            msgResp.respondTo(this);
            msgResp.post();
            }
        
        // Accessor for the property "PendingCounter"
        /**
         * Setter for property PendingCounter.<p>
        * Transient counter of the number of (local) services that remain to be
        * suspended/resumed.  See #proceed.
         */
        public void setPendingCounter(int sProperty)
            {
            __m_PendingCounter = sProperty;
            }
        
        // Accessor for the property "Relay"
        /**
         * Setter for property Relay.<p>
        * True if the recipeient should relay the request to the remainder of
        * the cluster.  This value should be set to true when sending to the
        * senior.
         */
        public void setRelay(boolean fRelay)
            {
            __m_Relay = fRelay;
            }
        
        // Accessor for the property "Resume"
        /**
         * Setter for property Resume.<p>
        * Flag that indicates whether the target service should be suspended
        * (false) or resumed (true).
         */
        public void setResume(boolean fResume)
            {
            __m_Resume = fResume;
            }
        
        // Accessor for the property "ResumeOnFailover"
        /**
         * Setter for property ResumeOnFailover.<p>
        * Flag that indicates whether the target service(s) should be resumed
        * upon failover.
        * 
        * Specifically this indicates that the suspension of a service was
        * synthetically induced and therefore should be automatically resumed
        * in the case of senior departure.
         */
        public void setResumeOnFailover(boolean fResume)
            {
            __m_ResumeOnFailover = fResume;
            }
        
        // Accessor for the property "ServiceId"
        /**
         * Setter for property ServiceId.<p>
        * The ServiceId of the Service to quiesce (or 0 for ClusterService,
        * representing that all services should be suspended/resumed).
         */
        public void setServiceId(int nId)
            {
            __m_ServiceId = nId;
            }
        
        // Declared at the super level
        public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
                throws java.io.IOException
            {
            super.write(output);
            
            output.writeInt(getServiceId());
            output.writeBoolean(isResume());
            output.writeBoolean(isResumeOnFailover());
            output.writeBoolean(isRelay());
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService$ServiceQuiescenceRequest$Poll
        
        /**
         * The Poll contains information regarding a request sent to one or
         * more Cluster Members that require responses. A Service may poll
         * other Members that are running the same Service, and the Poll is
         * used to wait for and assemble the responses from each of those
         * Members. A client thread may also use the Poll to block on a
         * response or set of responses, thus waiting for the completion of the
         * Poll. In its simplest form, which is a Poll that is sent to one
         * Member of the Cluster, the Poll actually represents the
         * request/response model.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class Poll
                extends    com.tangosol.coherence.component.net.Poll
            {
            // ---- Fields declarations ----
            
            /**
             * Property Continuation
             *
             */
            private transient com.oracle.coherence.common.base.Continuation __m_Continuation;
            
            // Default constructor
            public Poll()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public Poll(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
                {
                super(sName, compParent, false);
                
                if (fInit)
                    {
                    __init();
                    }
                }
            
            // Main initializer
            public void __init()
                {
                // private initialization
                __initPrivate();
                
                
                // signal the end of the initialization
                set_Constructed(true);
                }
            
            // Private initializer
            protected void __initPrivate()
                {
                
                super.__initPrivate();
                }
            
            //++ getter for static property _Instance
            /**
             * Getter for property _Instance.<p>
            * Auto generated
             */
            public static com.tangosol.coherence.Component get_Instance()
                {
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService.ServiceQuiescenceRequest.Poll();
                }
            
            //++ getter for static property _CLASS
            /**
             * Getter for property _CLASS.<p>
            * Property with auto-generated accessor that returns the Class
            * object for a given component.
             */
            public static Class get_CLASS()
                {
                Class clz;
                try
                    {
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ClusterService$ServiceQuiescenceRequest$Poll".replace('/', '.'));
                    }
                catch (ClassNotFoundException e)
                    {
                    throw new NoClassDefFoundError(e.getMessage());
                    }
                return clz;
                }
            
            //++ getter for autogen property _Module
            /**
             * This is an auto-generated method that returns the global [design
            * time] parent component.
            * 
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            private com.tangosol.coherence.Component get_Module()
                {
                return this.get_Parent().get_Parent();
                }
            
            // Accessor for the property "Continuation"
            /**
             * Getter for property Continuation.<p>
             */
            public com.oracle.coherence.common.base.Continuation getContinuation()
                {
                return __m_Continuation;
                }
            
            // Declared at the super level
            /**
             * This is the event that is executed when all the Members that were
            * polled have responded or have left the Service.
             */
            protected void onCompletion()
                {
                // import com.oracle.coherence.common.internal.continuations.Continuations;
                
                Continuations.proceed(getContinuation(), null);
                }
            
            // Accessor for the property "Continuation"
            /**
             * Setter for property Continuation.<p>
             */
            public void setContinuation(com.oracle.coherence.common.base.Continuation continuation)
                {
                __m_Continuation = continuation;
                }
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService$ServiceRegister
    
    /**
     * RequestMessage:
     *     ServiceRegister
     * 
     * Purpose:
     *     Inform all Members about a new Service.
     * 
     * Description:
     *     When a Member wishes to start a Service that has not previously been
     * registered, it must request a Service id for that Service, and the
     * senior Member is responsible for assigning the Service id. When the id
     * is assigned, the senior Member first notifies (using this message) the
     * rest of the cluster about this new Service and only then responds to the
     * requesting member thus preventing service info desynchronization in case
     * the senior dies before delivering a response to the requestor.
     * 
     * Attributes:
     *     ServiceId
     *     ServiceName
     * 
     * Response to:
     *     ServiceRegisterRequest
     * 
     * Expected responses:
     *     n/a
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class ServiceRegister
            extends    com.tangosol.coherence.component.net.message.RequestMessage
        {
        // ---- Fields declarations ----
        
        /**
         * Property RegisterRequest
         *
         * Transient property representing the original registration request.
         */
        private transient ClusterService.ServiceRegisterRequest __m_RegisterRequest;
        
        /**
         * Property ServiceId
         *
         * The id that has been assigned to the Service.
         */
        private int __m_ServiceId;
        
        /**
         * Property ServiceName
         *
         * The name of the Service that the id was assigned to.
         */
        private String __m_ServiceName;
        
        /**
         * Property ServiceType
         *
         * The type of the Service that the id was assigned to.
         */
        private String __m_ServiceType;
        private static com.tangosol.util.ListMap __mapChildren;
        
        // Static initializer
        static
            {
            __initStatic();
            }
        
        // Default static initializer
        private static void __initStatic()
            {
            // register child classes
            __mapChildren = new com.tangosol.util.ListMap();
            __mapChildren.put("Poll", ClusterService.ServiceRegister.Poll.get_CLASS());
            }
        
        // Default constructor
        public ServiceRegister()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public ServiceRegister(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);
            
            if (fInit)
                {
                __init();
                }
            }
        
        // Main initializer
        public void __init()
            {
            // private initialization
            __initPrivate();
            
            // state initialization: public and protected properties
            try
                {
                setMessageType(46);
                }
            catch (java.lang.Exception e)
                {
                // re-throw as a runtime exception
                throw new com.tangosol.util.WrapperException(e);
                }
            
            // containment initialization: children
            
            // signal the end of the initialization
            set_Constructed(true);
            }
        
        // Private initializer
        protected void __initPrivate()
            {
            
            super.__initPrivate();
            }
        
        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
        * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService.ServiceRegister();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ClusterService$ServiceRegister".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }
        
        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
        * time] parent component.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent();
            }
        
        //++ getter for autogen property _ChildClasses
        /**
         * This is an auto-generated method that returns the map of design time
        * [static] children.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        protected java.util.Map get_ChildClasses()
            {
            return __mapChildren;
            }
        
        // Declared at the super level
        /**
         * Getter for property Description.<p>
        * Used for debugging purposes (from toString). Create a human-readable
        * description of the specific Message data.
         */
        public String getDescription()
            {
            return "ServiceId="     + getServiceId()
                 + "\nServiceName=" + getServiceName();
            }
        
        // Accessor for the property "RegisterRequest"
        /**
         * Getter for property RegisterRequest.<p>
        * Transient property representing the original registration request.
         */
        public ClusterService.ServiceRegisterRequest getRegisterRequest()
            {
            return __m_RegisterRequest;
            }
        
        // Accessor for the property "ServiceId"
        /**
         * Getter for property ServiceId.<p>
        * The id that has been assigned to the Service.
         */
        public int getServiceId()
            {
            return __m_ServiceId;
            }
        
        // Accessor for the property "ServiceName"
        /**
         * Getter for property ServiceName.<p>
        * The name of the Service that the id was assigned to.
         */
        public String getServiceName()
            {
            return __m_ServiceName;
            }
        
        // Accessor for the property "ServiceType"
        /**
         * Getter for property ServiceType.<p>
        * The type of the Service that the id was assigned to.
         */
        public String getServiceType()
            {
            return __m_ServiceType;
            }
        
        // Declared at the super level
        protected com.tangosol.coherence.component.net.Poll instantiatePoll()
            {
            // import Component.Net.Poll;
            
            return (Poll) _newChild("Poll");
            }
        
        // Declared at the super level
        /**
         * This is the event that is executed when a Message is received.
        * <p>
        * It is the main processing event of the Message called by the
        * <code>Service.onMessage()</code> event. With regards to the use of
        * Message components within clustered Services, Services are designed
        * by dragging Message components into them as static children. These
        * Messages are the components that a Service can send to other running
        * instances of the same Service within a cluster. When the onReceived
        * event is invoked by a Service, it means that the Message has been
        * received; the code in the onReceived event is therefore the Message
        * specific logic for processing a received Message. For example, when
        * onReceived is invoked on a Message named FindData, the onReceived
        * event should do the work to "find the data", because it is being
        * invoked by the Service that received the "find the data" Message.
         */
        public void onReceived()
            {
            super.onReceived();
            
            // this Message brings information about a new Service
            // and comes only from the senior service member
            ClusterService service = (ClusterService) getService();
            service.ensureServiceInfo(getServiceId(), getServiceName(), getServiceType());
            
            ClusterService.ServiceUpdateResponse msg = (ClusterService.ServiceUpdateResponse)
                    service.instantiateMessage("ServiceUpdateResponse");
            msg.respondTo(this);
            
            service.send(msg);
            }
        
        // Declared at the super level
        public void read(com.tangosol.io.ReadBuffer.BufferInput input)
                throws java.io.IOException
            {
            setServiceId(input.readUnsignedShort());
            setServiceName(input.readUTF());
            setServiceType(input.readUTF());
            }
        
        // Accessor for the property "RegisterRequest"
        /**
         * Setter for property RegisterRequest.<p>
        * Transient property representing the original registration request.
         */
        public void setRegisterRequest(ClusterService.ServiceRegisterRequest msgRequest)
            {
            __m_RegisterRequest = msgRequest;
            }
        
        // Accessor for the property "ServiceId"
        /**
         * Setter for property ServiceId.<p>
        * The id that has been assigned to the Service.
         */
        public void setServiceId(int nId)
            {
            __m_ServiceId = nId;
            }
        
        // Accessor for the property "ServiceName"
        /**
         * Setter for property ServiceName.<p>
        * The name of the Service that the id was assigned to.
         */
        public void setServiceName(String sName)
            {
            __m_ServiceName = sName;
            }
        
        // Accessor for the property "ServiceType"
        /**
         * Setter for property ServiceType.<p>
        * The type of the Service that the id was assigned to.
         */
        public void setServiceType(String sType)
            {
            __m_ServiceType = sType;
            }
        
        // Declared at the super level
        public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
                throws java.io.IOException
            {
            output.writeShort(getServiceId());
            output.writeUTF(getServiceName());
            output.writeUTF(getServiceType());
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService$ServiceRegister$Poll
        
        /**
         * The Poll contains information regarding a request sent to one or
         * more Cluster Members that require responses. A Service may poll
         * other Members that are running the same Service, and the Poll is
         * used to wait for and assemble the responses from each of those
         * Members. A client thread may also use the Poll to block on a
         * response or set of responses, thus waiting for the completion of the
         * Poll. In its simplest form, which is a Poll that is sent to one
         * Member of the Cluster, the Poll actually represents the
         * request/response model.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class Poll
                extends    com.tangosol.coherence.component.net.Poll
            {
            // ---- Fields declarations ----
            
            // Default constructor
            public Poll()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public Poll(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
                {
                super(sName, compParent, false);
                
                if (fInit)
                    {
                    __init();
                    }
                }
            
            // Main initializer
            public void __init()
                {
                // private initialization
                __initPrivate();
                
                
                // signal the end of the initialization
                set_Constructed(true);
                }
            
            // Private initializer
            protected void __initPrivate()
                {
                
                super.__initPrivate();
                }
            
            //++ getter for static property _Instance
            /**
             * Getter for property _Instance.<p>
            * Auto generated
             */
            public static com.tangosol.coherence.Component get_Instance()
                {
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService.ServiceRegister.Poll();
                }
            
            //++ getter for static property _CLASS
            /**
             * Getter for property _CLASS.<p>
            * Property with auto-generated accessor that returns the Class
            * object for a given component.
             */
            public static Class get_CLASS()
                {
                Class clz;
                try
                    {
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ClusterService$ServiceRegister$Poll".replace('/', '.'));
                    }
                catch (ClassNotFoundException e)
                    {
                    throw new NoClassDefFoundError(e.getMessage());
                    }
                return clz;
                }
            
            //++ getter for autogen property _Module
            /**
             * This is an auto-generated method that returns the global [design
            * time] parent component.
            * 
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            private com.tangosol.coherence.Component get_Module()
                {
                return this.get_Parent().get_Parent();
                }
            
            // Declared at the super level
            /**
             * This is the event that is executed when all the Members that were
            * polled have responded or have left the Service.
             */
            protected void onCompletion()
                {
                ClusterService.ServiceRegister        msgRegister = (ClusterService.ServiceRegister) get_Parent();
                ClusterService.ServiceRegisterRequest msgRequest  = msgRegister.getRegisterRequest();
                
                if (msgRequest != null)
                    {
                    ClusterService service = (ClusterService) getService();
                
                    ClusterService.ServiceRegister msg = (ClusterService.ServiceRegister)
                            service.instantiateMessage("ServiceRegister");
                    msg.respondTo(msgRequest);
                    msg.setServiceId  (msgRegister.getServiceId());
                    msg.setServiceName(msgRegister.getServiceName());
                    msg.setServiceType(msgRegister.getServiceType());
                
                    service.send(msg);
                    }
                
                super.onCompletion();
                }
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService$ServiceRegisterRequest
    
    /**
     * RequestMessage:
     *     ServiceRegisterRequest
     * 
     * Purpose:
     *     Request sent to the senior Member to assign a Service id.
     * 
     * Description:
     *     When a Member wishes to start a Service that has not previously been
     * registered, it must request a Service id for that Service, and the
     * senior Member is responsible for assigning the Service id.
     * 
     * Attributes:
     *     ServiceName
     *     ServiceType
     * 
     * Response to:
     *     n/a
     * 
     * Expected responses:
     *     ServiceRegister
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class ServiceRegisterRequest
            extends    com.tangosol.coherence.component.net.message.RequestMessage
        {
        // ---- Fields declarations ----
        
        /**
         * Property ServiceName
         *
         * The name of the Service to register. Service names are unique in the
         * cluster, but to reduce packet size, the names are registered with
         * the senior Member and assigned a 2-byte unsigned id (a "mini id")
         * that the Members of the cluster use to refer to the Service.
         */
        private String __m_ServiceName;
        
        /**
         * Property ServiceType
         *
         * The type of the Service to register.
         */
        private String __m_ServiceType;
        private static com.tangosol.util.ListMap __mapChildren;
        
        // Static initializer
        static
            {
            __initStatic();
            }
        
        // Default static initializer
        private static void __initStatic()
            {
            // register child classes
            __mapChildren = new com.tangosol.util.ListMap();
            __mapChildren.put("Poll", ClusterService.ServiceRegisterRequest.Poll.get_CLASS());
            }
        
        // Default constructor
        public ServiceRegisterRequest()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public ServiceRegisterRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);
            
            if (fInit)
                {
                __init();
                }
            }
        
        // Main initializer
        public void __init()
            {
            // private initialization
            __initPrivate();
            
            // state initialization: public and protected properties
            try
                {
                setMessageType(47);
                }
            catch (java.lang.Exception e)
                {
                // re-throw as a runtime exception
                throw new com.tangosol.util.WrapperException(e);
                }
            
            // containment initialization: children
            
            // signal the end of the initialization
            set_Constructed(true);
            }
        
        // Private initializer
        protected void __initPrivate()
            {
            
            super.__initPrivate();
            }
        
        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
        * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService.ServiceRegisterRequest();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ClusterService$ServiceRegisterRequest".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }
        
        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
        * time] parent component.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent();
            }
        
        //++ getter for autogen property _ChildClasses
        /**
         * This is an auto-generated method that returns the map of design time
        * [static] children.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        protected java.util.Map get_ChildClasses()
            {
            return __mapChildren;
            }
        
        // Declared at the super level
        /**
         * Getter for property Description.<p>
        * Used for debugging purposes (from toString). Create a human-readable
        * description of the specific Message data.
         */
        public String getDescription()
            {
            return "ServiceName=" + getServiceName();
            }
        
        // Accessor for the property "ServiceName"
        /**
         * Getter for property ServiceName.<p>
        * The name of the Service to register. Service names are unique in the
        * cluster, but to reduce packet size, the names are registered with the
        * senior Member and assigned a 2-byte unsigned id (a "mini id") that
        * the Members of the cluster use to refer to the Service.
         */
        public String getServiceName()
            {
            return __m_ServiceName;
            }
        
        // Accessor for the property "ServiceType"
        /**
         * Getter for property ServiceType.<p>
        * The type of the Service to register.
         */
        public String getServiceType()
            {
            return __m_ServiceType;
            }
        
        // Declared at the super level
        /**
         * This is the event that is executed when a Message is received.
        * <p>
        * It is the main processing event of the Message called by the
        * <code>Service.onMessage()</code> event. With regards to the use of
        * Message components within clustered Services, Services are designed
        * by dragging Message components into them as static children. These
        * Messages are the components that a Service can send to other running
        * instances of the same Service within a cluster. When the onReceived
        * event is invoked by a Service, it means that the Message has been
        * received; the code in the onReceived event is therefore the Message
        * specific logic for processing a received Message. For example, when
        * onReceived is invoked on a Message named FindData, the onReceived
        * event should do the work to "find the data", because it is being
        * invoked by the Service that received the "find the data" Message.
         */
        public void onReceived()
            {
            // import Component.Net.MemberSet;
            
            super.onReceived();
            
            // this Message should have only been directed here if this is               
            // the oldest Member; it is theoretically possible that this
            // Member is not yet aware that it is the senior Member
            ClusterService service = (ClusterService) getService();
            
            String sName = getServiceName();
            String sType = getServiceType();
            
            ClusterService.ServiceRegister msg = (ClusterService.ServiceRegister)
                    service.instantiateMessage("ServiceRegister");
            msg.setServiceName(sName);
            
            int nId = service.indexOfService(sName);
            if (nId < 0)
                {
                // the service info is never removed
                // so the current count is used as the next service id
                nId = service.getServiceInfoCount();
                service.ensureServiceInfo(nId, sName, sType);
            
                msg.setServiceType(sType);
            
                MemberSet setOthers = service.getOthersMemberSet();
                setOthers.remove(getFromMember());
            
                if (!setOthers.isEmpty())
                    {
                    // update all other cluster Members using ServiceRegister
                    // and wait for delivery; only then reply to the requestor
                    // see ServiceRegister$Poll.onCompletion()
                    msg.setToMemberSet(setOthers);
                    msg.setServiceId(nId);
                    msg.setRegisterRequest(this);
            
                    service.send(msg); // see ServiceRegister$Poll#onCompletion()
                    return;
                    }
                }
            else
                {
                msg.setServiceType(service.getServiceInfo(nId).getServiceType());
                }
            
            // the service either was already registered or there was no one to inform;
            // respond to the request immediately
            msg.respondTo(this);
            msg.setServiceId(nId);
            
            service.send(msg);
            }
        
        // Declared at the super level
        public void read(com.tangosol.io.ReadBuffer.BufferInput input)
                throws java.io.IOException
            {
            setServiceName(input.readUTF());
            setServiceType(input.readUTF());
            }
        
        // Accessor for the property "ServiceName"
        /**
         * Setter for property ServiceName.<p>
        * The name of the Service to register. Service names are unique in the
        * cluster, but to reduce packet size, the names are registered with the
        * senior Member and assigned a 2-byte unsigned id (a "mini id") that
        * the Members of the cluster use to refer to the Service.
         */
        public void setServiceName(String sName)
            {
            __m_ServiceName = sName;
            }
        
        // Accessor for the property "ServiceType"
        /**
         * Setter for property ServiceType.<p>
        * The type of the Service to register.
         */
        public void setServiceType(String pServiceType)
            {
            __m_ServiceType = pServiceType;
            }
        
        // Declared at the super level
        public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
                throws java.io.IOException
            {
            output.writeUTF(getServiceName());
            output.writeUTF(getServiceType());
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService$ServiceRegisterRequest$Poll
        
        /**
         * The Poll contains information regarding a request sent to one or
         * more Cluster Members that require responses. A Service may poll
         * other Members that are running the same Service, and the Poll is
         * used to wait for and assemble the responses from each of those
         * Members. A client thread may also use the Poll to block on a
         * response or set of responses, thus waiting for the completion of the
         * Poll. In its simplest form, which is a Poll that is sent to one
         * Member of the Cluster, the Poll actually represents the
         * request/response model.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class Poll
                extends    com.tangosol.coherence.component.net.Poll
            {
            // ---- Fields declarations ----
            
            // Default constructor
            public Poll()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public Poll(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
                {
                super(sName, compParent, false);
                
                if (fInit)
                    {
                    __init();
                    }
                }
            
            // Main initializer
            public void __init()
                {
                // private initialization
                __initPrivate();
                
                
                // signal the end of the initialization
                set_Constructed(true);
                }
            
            // Private initializer
            protected void __initPrivate()
                {
                
                super.__initPrivate();
                }
            
            //++ getter for static property _Instance
            /**
             * Getter for property _Instance.<p>
            * Auto generated
             */
            public static com.tangosol.coherence.Component get_Instance()
                {
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService.ServiceRegisterRequest.Poll();
                }
            
            //++ getter for static property _CLASS
            /**
             * Getter for property _CLASS.<p>
            * Property with auto-generated accessor that returns the Class
            * object for a given component.
             */
            public static Class get_CLASS()
                {
                Class clz;
                try
                    {
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ClusterService$ServiceRegisterRequest$Poll".replace('/', '.'));
                    }
                catch (ClassNotFoundException e)
                    {
                    throw new NoClassDefFoundError(e.getMessage());
                    }
                return clz;
                }
            
            //++ getter for autogen property _Module
            /**
             * This is an auto-generated method that returns the global [design
            * time] parent component.
            * 
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            private com.tangosol.coherence.Component get_Module()
                {
                return this.get_Parent().get_Parent();
                }
            
            // Declared at the super level
            /**
             * This event occurs for each response Message from each polled
            * Member.
             */
            public void onResponse(com.tangosol.coherence.component.net.Message msg)
                {
                // import com.tangosol.util.Base;
                
                ClusterService.ServiceRegister msgRegister = (ClusterService.ServiceRegister) msg;
                
                setResult(Base.makeInteger(msgRegister.getServiceId()));
                
                super.onResponse(msg);
                }
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService$ServiceUpdateResponse
    
    /**
     * Message:
     *     ServiceUpdateResponse
     * 
     * Purpose:
     *     Ensures that the other members received a service update request
     * message before continuing processing.
     * 
     * Description
     *     This message serves as a response to polls issued during the service
     * registration and joining process.
     * 
     * Attributes:
     *     Result
     * 
     * Response to:
     *     ServiceRegister
     *     ServiceJoined
     * 
     * Expected responses:
     *     n/a
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class ServiceUpdateResponse
            extends    com.tangosol.coherence.component.net.Message
        {
        // ---- Fields declarations ----
        
        /**
         * Property Result
         *
         * General purpose response value.
         * 
         * Added for Coherence 2.5
         */
        private Object __m_Result;
        
        // Default constructor
        public ServiceUpdateResponse()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public ServiceUpdateResponse(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);
            
            if (fInit)
                {
                __init();
                }
            }
        
        // Main initializer
        public void __init()
            {
            // private initialization
            __initPrivate();
            
            // state initialization: public and protected properties
            try
                {
                setMessageType(48);
                }
            catch (java.lang.Exception e)
                {
                // re-throw as a runtime exception
                throw new com.tangosol.util.WrapperException(e);
                }
            
            // signal the end of the initialization
            set_Constructed(true);
            }
        
        // Private initializer
        protected void __initPrivate()
            {
            
            super.__initPrivate();
            }
        
        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
        * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService.ServiceUpdateResponse();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ClusterService$ServiceUpdateResponse".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }
        
        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
        * time] parent component.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent();
            }
        
        // Accessor for the property "Result"
        /**
         * Getter for property Result.<p>
        * General purpose response value.
        * 
        * Added for Coherence 2.5
         */
        public Object getResult()
            {
            return __m_Result;
            }
        
        // Declared at the super level
        public void read(com.tangosol.io.ReadBuffer.BufferInput input)
                throws java.io.IOException
            {
            // import java.io.IOException;
            
            try
                {
                setResult(readObject(input));
            
                // determine if any additional bytes are available from the stream;
                if (input.skipBytes(1) != 0)
                    {
                    // if we got here then the message contains more data then expected
                    // which may be due to incompatible serializers
                    throw new IOException("message contains more data than expected");
                    }
                }
            catch (IOException e)
                {
                getService().onConfigIOException(e, getFromMember());
                }
            }
        
        // Accessor for the property "Result"
        /**
         * Setter for property Result.<p>
        * General purpose response value.
        * 
        * Added for Coherence 2.5
         */
        public void setResult(Object oResult)
            {
            __m_Result = oResult;
            }
        
        // Declared at the super level
        public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
                throws java.io.IOException
            {
            writeObject(output, getResult());
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService$TcpRing
    
    /**
     * This component maintains TcpRing connections with other Cluster members
     * to quickly detect member departure.
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class TcpRing
            extends    com.tangosol.coherence.component.net.TcpRing
            implements com.oracle.coherence.common.base.Notifier
        {
        // ---- Fields declarations ----
        
        /**
         * Property Enabled
         *
         * True iff the TcpRing is to be used.  It is not recomended to disable
         * this feature as death detection will fall back on much slower
         * timeout based mechansims.
         */
        private transient boolean __m_Enabled;
        
        // Default constructor
        public TcpRing()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public TcpRing(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);
            
            if (fInit)
                {
                __init();
                }
            }
        
        // Main initializer
        public void __init()
            {
            // private initialization
            __initPrivate();
            
            // state initialization: public and protected properties
            try
                {
                setBuddies(new java.util.HashMap());
                setEnabled(true);
                setSocketOptions(new com.tangosol.net.SocketOptions());
                }
            catch (java.lang.Exception e)
                {
                // re-throw as a runtime exception
                throw new com.tangosol.util.WrapperException(e);
                }
            
            // containment initialization: children
            _addChild(new com.tangosol.coherence.component.net.TcpRing.MemberMonitor("MemberMonitor", this, true), "MemberMonitor");
            
            // signal the end of the initialization
            set_Constructed(true);
            }
        
        // Private initializer
        protected void __initPrivate()
            {
            
            super.__initPrivate();
            }
        
        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
        * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService.TcpRing();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ClusterService$TcpRing".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }
        
        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
        * time] parent component.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent();
            }
        
        // From interface: com.oracle.coherence.common.base.Notifier
        public void await()
                throws java.lang.InterruptedException
            {
            await(0L);
            }
        
        // From interface: com.oracle.coherence.common.base.Notifier
        public void await(long cMillis)
                throws java.lang.InterruptedException
            {
            select(cMillis);
            }
        
        /**
         * Compute this member's buddy set.
         */
        public java.util.Set computeBuddies()
            {
            // import Component.Net.Member;
            // import com.tangosol.util.NullImplementation;
            // import java.util.HashSet;
            
            if (!isEnabled())
                {
                return NullImplementation.getSet();
                }
            
            Member  memberThis = ((ClusterService) get_Module()).getThisMember();
            HashSet setBuddy   = new HashSet();
            
            setBuddy.add(getRingBuddy(memberThis, /*fStrong*/ false));
            setBuddy.add(getRoleBuddy(memberThis, /*fStrong*/ false));
            setBuddy.remove(null);
            
            return setBuddy;
            }
        
        /**
         * Return the specified Member's ring buddy or null if there is none.
        * 
        * @param  member the member of interest
        * @param  fStrong  true if the buddy must be on a different machine
        * then the specified member
         */
        public com.tangosol.coherence.component.net.Member getRingBuddy(com.tangosol.coherence.component.net.Member member, boolean fStrong)
            {
            // import Component.Net.Member;
            // import Component.Net.MemberSet;
            
            MemberSet setMembers = ((ClusterService) get_Module()).getClusterMemberSet();
            int       nBuddy     = setMembers.getPrevId(member.getId());
            Member    buddy;
            
            if (fStrong)
                {
                int nMachine = member.getMachineId();
                
                do
                    {
                    buddy  = setMembers.getMember(nBuddy == 0 ? setMembers.getLastId() : nBuddy);
                    nBuddy = setMembers.getPrevId(buddy.getId());
                    }
                while (buddy.getMachineId() == nMachine && buddy != member);
                }
            else
                {
                buddy = setMembers.getMember(nBuddy == 0 ? setMembers.getLastId() : nBuddy);
                }
            
            return buddy == member ? null : buddy;
            }
        
        /**
         * Return the specified Member's role buddy or null if there is none.
        * 
        * The role buddy ring is used to ensure that each node is monitored by
        * a node with similar SLAs to its own.  This avoids the case where the
        * detection of the death of a cache server is slow do to it's ring
        * buddy being in a long GC, longer then any pause which would have been
        * acceptable in the cache server tier itself.
        * 
        * @param  member the member of interest
        * @param  fStrong  true if the buddy must be on a different machine
        * then the specified member
         */
        public com.tangosol.coherence.component.net.Member getRoleBuddy(com.tangosol.coherence.component.net.Member member, boolean fStrong)
            {
            // import Component.Net.Member;
            // import Component.Net.MemberSet;
            // import com.tangosol.util.Base;
            
            String    sRole      = member.getRoleName();
            int       nMachine   = member.getMachineId();
            MemberSet setMembers = ((ClusterService) get_Module()).getClusterMemberSet();
            int       nBuddy     = setMembers.getPrevId(member.getId());
            Member    buddy;
            
            do
                {
                buddy  = setMembers.getMember(nBuddy == 0 ? setMembers.getLastId() : nBuddy);
                nBuddy = setMembers.getPrevId(buddy.getId());
                }
            while (((fStrong && nMachine == buddy.getMachineId()) ||  // need strong, but buddy co-located
                    !Base.equals(buddy.getRoleName(), sRole)) &&      // role doesn't match
                   buddy != member);
            
            return buddy == member ? null : buddy;
            }
        
        // Accessor for the property "Enabled"
        /**
         * Getter for property Enabled.<p>
        * True iff the TcpRing is to be used.  It is not recomended to disable
        * this feature as death detection will fall back on much slower timeout
        * based mechansims.
         */
        public boolean isEnabled()
            {
            return __m_Enabled;
            }
        
        // Declared at the super level
        /**
         * Remove the current buddy from the cluster
         */
        protected void onDeadBuddy(com.tangosol.coherence.component.net.Member member, java.io.IOException e)
            {
            ((ClusterService) get_Module()).onNotifyTcpDeparture(member);
            
            super.onDeadBuddy(member, e);
            }
        
        // Declared at the super level
        /**
         * Invoked when the TcpRing finds that it is has no inbound buddies,
        * i.e. is unmonitored.
         */
        protected void onIsolation()
            {
            ClusterService service = (ClusterService) get_Module();
            int cMembers = service.getServiceMemberSet().size();
            if (cMembers > 1)
                {
                // force senior to send SMHB to this node
                ClusterService.NewMemberAnnounce msg = (ClusterService.NewMemberAnnounce)
                        service.instantiateMessage("NewMemberAnnounce");
                msg.setAnnounceProtocolVersion(ClusterService.ANNOUNCE_PROTOCOL_VERSION);        
                msg.setFromMember(service.getThisMember());
                msg.setAttemptCounter(1);
                msg.setAttemptLimit(1);
                service.send(msg);
            
                // senior will respond with SMHB, and non-senior WKAs will respond with NewMemberAnnounceWait so we can learn the new senior
                // which would cause our next isolation broadcast to include the new senior
                }
            }
        
        /**
         * Called to indicate that this member has joined the cluster.
         */
        public void onJoined()
            {
            ensureTopology(computeBuddies());
            }
        
        /**
         * Called to indicate that this member has left the cluster.
         */
        public void onLeft()
            {
            // import com.tangosol.util.NullImplementation;
            
            ensureTopology(NullImplementation.getSet());
            disconnectAll();
            }
        
        /**
         * Called to indicated that a member has joined the cluster.
         */
        public void onMemberJoined(com.tangosol.coherence.component.net.Member member)
            {
            // import java.util.Set;
            
            Set setBuddies = computeBuddies();
            if (setBuddies.contains(member))
                {
                ensureTopology(setBuddies);
                }
            }
        
        /**
         * Called to indicated that a member has left the cluster.
         */
        public void onMemberLeft(com.tangosol.coherence.component.net.Member member)
            {
            if (getBuddies().containsKey(member))
                {
                ensureTopology(computeBuddies());
            
                // in case we were informed (rather then detected) the death of our
                // buddy it is important for us to push any in-doubt member departures
                // This handles cases such as the following
                // 5->6->7->8
                // \____/
                // 8 dies, and 7 notifies everyone, but dies after only 6 has heard it
                // when 7 dies it could be detected by either of it's buddies and if 5
                // detects it before 6, then 6 learns of 7s death rather then detecting
                // it.  At this point 6 holds the secret that 8 is dead and it is the
                // only one that would monitor it over the ring
                ((ClusterService) get_Module()).ensureMemberLeft(/*memberTo*/ null, /*setUUIDExempt*/ null, /*fRequest*/ false);
                }
            }
        
        // Accessor for the property "Enabled"
        /**
         * Setter for property Enabled.<p>
        * True iff the TcpRing is to be used.  It is not recomended to disable
        * this feature as death detection will fall back on much slower timeout
        * based mechansims.
         */
        public void setEnabled(boolean fEnabled)
            {
            __m_Enabled = fEnabled;
            }
        
        // From interface: com.oracle.coherence.common.base.Notifier
        public void signal()
            {
            wakeup();
            }
        
        // Declared at the super level
        public String toString()
            {
            if (isEnabled())
                {
                return super.toString();
                }
            return "TcpRing{disabled}";
            }
        
        // Declared at the super level
        /**
         * Wakeup the TcpRing's selector.
         */
        public void wakeup()
            {
            try
                {
                super.wakeup();
                }
            catch (RuntimeException e)
                {
                if (!((ClusterService) get_Module()).isExiting())
                    {
                    throw e;
                    }
                // else; selector.wakeup throws NPE once closed; eat it
                }
            }
        }

    // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService$WitnessRequest
    
    /**
     * Message:
     *     WitnessRequest
     * 
     * Purpose:
     *     Request confirmation of a suspected dead member.
     * 
     * Description:
     *     Sent when a node suspects that another node has left the cluster,
     * but this departure was due to a timeout.
     * 
     *  Attributes:
     *     MemberUUID
     * 
     * Response to:
     *     n/a
     * 
     * Expected responses:
     *     Response CONFIRM/REJECT
     */
    @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
    public static class WitnessRequest
            extends    com.tangosol.coherence.component.net.message.RequestMessage
        {
        // ---- Fields declarations ----
        
        /**
         * Property MemberUUID
         *
         * The UUID of the Member who we are inquiring about.
         */
        private com.tangosol.util.UUID __m_MemberUUID;
        private static com.tangosol.util.ListMap __mapChildren;
        
        // Static initializer
        static
            {
            __initStatic();
            }
        
        // Default static initializer
        private static void __initStatic()
            {
            // register child classes
            __mapChildren = new com.tangosol.util.ListMap();
            __mapChildren.put("Poll", ClusterService.WitnessRequest.Poll.get_CLASS());
            }
        
        // Default constructor
        public WitnessRequest()
            {
            this(null, null, true);
            }
        
        // Initializing constructor
        public WitnessRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
            {
            super(sName, compParent, false);
            
            if (fInit)
                {
                __init();
                }
            }
        
        // Main initializer
        public void __init()
            {
            // private initialization
            __initPrivate();
            
            // state initialization: public and protected properties
            try
                {
                setMessageType(56);
                }
            catch (java.lang.Exception e)
                {
                // re-throw as a runtime exception
                throw new com.tangosol.util.WrapperException(e);
                }
            
            // containment initialization: children
            
            // signal the end of the initialization
            set_Constructed(true);
            }
        
        // Private initializer
        protected void __initPrivate()
            {
            
            super.__initPrivate();
            }
        
        //++ getter for static property _Instance
        /**
         * Getter for property _Instance.<p>
        * Auto generated
         */
        public static com.tangosol.coherence.Component get_Instance()
            {
            return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService.WitnessRequest();
            }
        
        //++ getter for static property _CLASS
        /**
         * Getter for property _CLASS.<p>
        * Property with auto-generated accessor that returns the Class object
        * for a given component.
         */
        public static Class get_CLASS()
            {
            Class clz;
            try
                {
                clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ClusterService$WitnessRequest".replace('/', '.'));
                }
            catch (ClassNotFoundException e)
                {
                throw new NoClassDefFoundError(e.getMessage());
                }
            return clz;
            }
        
        //++ getter for autogen property _Module
        /**
         * This is an auto-generated method that returns the global [design
        * time] parent component.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        private com.tangosol.coherence.Component get_Module()
            {
            return this.get_Parent();
            }
        
        //++ getter for autogen property _ChildClasses
        /**
         * This is an auto-generated method that returns the map of design time
        * [static] children.
        * 
        * Note: the class generator will ignore any custom implementation for
        * this behavior.
         */
        protected java.util.Map get_ChildClasses()
            {
            return __mapChildren;
            }
        
        // Declared at the super level
        /**
         * Getter for property Description.<p>
        * Used for debugging purposes (from toString). Create a human-readable
        * description of the specific Message data.
         */
        public String getDescription()
            {
            return "MemberUid=" + getMemberUUID();
            }
        
        // Accessor for the property "MemberUUID"
        /**
         * Getter for property MemberUUID.<p>
        * The UUID of the Member who we are inquiring about.
         */
        public com.tangosol.util.UUID getMemberUUID()
            {
            return __m_MemberUUID;
            }
        
        // Declared at the super level
        protected com.tangosol.coherence.component.net.Poll instantiatePoll()
            {
            // import Component.Net.Poll;
            
            return (Poll) _newChild("Poll");
            }
        
        // Declared at the super level
        /**
         * This is the event that is executed when a Message is received.
        * <p>
        * It is the main processing event of the Message called by the
        * <code>Service.onMessage()</code> event. With regards to the use of
        * Message components within clustered Services, Services are designed
        * by dragging Message components into them as static children. These
        * Messages are the components that a Service can send to other running
        * instances of the same Service within a cluster. When the onReceived
        * event is invoked by a Service, it means that the Message has been
        * received; the code in the onReceived event is therefore the Message
        * specific logic for processing a received Message. For example, when
        * onReceived is invoked on a Message named FindData, the onReceived
        * event should do the work to "find the data", because it is being
        * invoked by the Service that received the "find the data" Message.
         */
        public void onReceived()
            {
            // import Component.Net.Member;
            
            ClusterService   service   = (ClusterService) getService();
            Member    member    = service.getClusterMemberSet().getMember(getMemberUUID());
            Member    memberReq = getFromMember();
            ClusterService.Response response  = (ClusterService.Response) service.instantiateMessage("Response");
            
            response.respondTo(this);
            
            if (member == null)
                {
                response.setResult(ClusterService.Response.RESULT_SUCCESS);
                }
            else if (!getMemberUUID().equals(member.getUid32()))
                {
                // prior to Cohernce 3.5.3 this would stop the cluster (COH-2489)
                _trace("Rejecting MemberLeft " + this + "\nfor " + member
                    + " with different UID " + member.getUid32(), 1);
                response.setResult(ClusterService.Response.RESULT_FAILURE);
                }
            else if ((service.isRecentlyHeuristicallyDead(member)    || service.isSlow(member)) &&
                    !(service.isRecentlyHeuristicallyDead(memberReq) || service.isSlow(memberReq)))
                {
                _trace("Confirming the departure request by " +
                    service.getMemberStatsDescription(memberReq) + " regarding " +
                    service.getMemberStatsDescription(member), 3);
                response.setResult(ClusterService.Response.RESULT_SUCCESS);
                }
            else
                {
                // abstain; we either consider the suspect member to be no worse then the requesting member
                _trace("Rejecting the departure confirmation request by "
                    + service.getMemberStatsDescription(memberReq) + " regarding "
                    + service.getMemberStatsDescription(member), 3);
                response.setResult(ClusterService.Response.RESULT_FAILURE);
                }
            
            service.send(response);
            }
        
        // Declared at the super level
        public void read(com.tangosol.io.ReadBuffer.BufferInput input)
                throws java.io.IOException
            {
            // import com.tangosol.util.UUID;
            
            super.read(input);
            
            UUID uuid = new UUID();
            uuid.readExternal(input);
            setMemberUUID(uuid);
            }
        
        // Accessor for the property "MemberUUID"
        /**
         * Setter for property MemberUUID.<p>
        * The UUID of the Member who we are inquiring about.
         */
        public void setMemberUUID(com.tangosol.util.UUID uid)
            {
            __m_MemberUUID = uid;
            }
        
        // Declared at the super level
        public void write(com.tangosol.io.WriteBuffer.BufferOutput output)
                throws java.io.IOException
            {
            super.write(output);
            
            getMemberUUID().writeExternal(output);
            }

        // ---- class: com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService$WitnessRequest$Poll
        
        /**
         * The Poll contains information regarding a request sent to one or
         * more Cluster Members that require responses. A Service may poll
         * other Members that are running the same Service, and the Poll is
         * used to wait for and assemble the responses from each of those
         * Members. A client thread may also use the Poll to block on a
         * response or set of responses, thus waiting for the completion of the
         * Poll. In its simplest form, which is a Poll that is sent to one
         * Member of the Cluster, the Poll actually represents the
         * request/response model.
         */
        @SuppressWarnings({"deprecation", "rawtypes", "unused", "unchecked", "ConstantConditions", "DuplicatedCode", "ForLoopReplaceableByForEach", "IfCanBeSwitch", "RedundantArrayCreation", "RedundantSuppression", "SameParameterValue", "TryFinallyCanBeTryWithResources", "TryWithIdenticalCatches", "UnnecessaryBoxing", "UnnecessaryUnboxing", "UnusedAssignment"})
        public static class Poll
                extends    com.tangosol.coherence.component.net.Poll
            {
            // ---- Fields declarations ----
            
            // Default constructor
            public Poll()
                {
                this(null, null, true);
                }
            
            // Initializing constructor
            public Poll(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
                {
                super(sName, compParent, false);
                
                if (fInit)
                    {
                    __init();
                    }
                }
            
            // Main initializer
            public void __init()
                {
                // private initialization
                __initPrivate();
                
                
                // signal the end of the initialization
                set_Constructed(true);
                }
            
            // Private initializer
            protected void __initPrivate()
                {
                
                super.__initPrivate();
                }
            
            //++ getter for static property _Instance
            /**
             * Getter for property _Instance.<p>
            * Auto generated
             */
            public static com.tangosol.coherence.Component get_Instance()
                {
                return new com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService.WitnessRequest.Poll();
                }
            
            //++ getter for static property _CLASS
            /**
             * Getter for property _CLASS.<p>
            * Property with auto-generated accessor that returns the Class
            * object for a given component.
             */
            public static Class get_CLASS()
                {
                Class clz;
                try
                    {
                    clz = Class.forName("com.tangosol.coherence/component/util/daemon/queueProcessor/service/grid/ClusterService$WitnessRequest$Poll".replace('/', '.'));
                    }
                catch (ClassNotFoundException e)
                    {
                    throw new NoClassDefFoundError(e.getMessage());
                    }
                return clz;
                }
            
            //++ getter for autogen property _Module
            /**
             * This is an auto-generated method that returns the global [design
            * time] parent component.
            * 
            * Note: the class generator will ignore any custom implementation
            * for this behavior.
             */
            private com.tangosol.coherence.Component get_Module()
                {
                return this.get_Parent().get_Parent();
                }
            
            // Declared at the super level
            /**
             * This is the event that is executed when all the Members that were
            * polled have responded or have left the Service.
             */
            public void onCompletion()
                {
                // import Component.Net.Member;
                // import java.util.Collections;
                
                if (((ClusterService) getService()).isRunning())
                    {
                    ClusterService service       = (ClusterService) getService();
                    Member  memberSuspect = service.getClusterMemberSet().getMember(((ClusterService.WitnessRequest) get_Parent()).getMemberUUID());
                    Member  memberWitness = (Member) getResult(); // see onResponse()
                
                    if (memberSuspect == null)
                        {
                        // suspect has died, no further action is required
                        return;
                        }
                    else if (memberWitness != null) 
                        {
                        // at least one witness confirmed communication issues with the
                        // suspect; the suspect is declared guilty
                        _trace("Member time-out confirmed by " + memberWitness + "; marking "
                            + service.getMemberStatsDescription(memberSuspect) + " as suspect", 3);
                        service.onMembersTimedOut(Collections.singleton(memberSuspect));
                        }
                    else if (getLeftMemberSet().isEmpty())
                        {
                        // no members confirmed, and no members left, thus all members
                        // rejected; Prior to 12.2.1.4 this was considered to be a
                        // fault of this member and it would restart. Now we've reevaluated
                        // this and see that we actually have a tie as the witnesses have
                        // said they communicate with the suspect, but in answering our request
                        // they've also said that they communicate with us, thus from just this
                        // information the witnesses' rejections state that they have no
                        // preference as they can communicate with both parties.  Given that
                        // it is a tie and we can use priority to determine who termintes.
                        // The result of this is that a storage enabled member would always choose to
                        // kill a client over itself so long as it can talk to other members. While
                        // a client could kill an unreachable storage enabled memeber only if it can find
                        // some other cluster member which also agrees the storage enabled member is
                        // unreachable, otherwise it kills itself.
                
                        int nComp = service.compareImportance(memberSuspect);
                
                        _trace("This node appears to have partially lost connectivity:"
                            + " it receives responses from " + getRespondedMemberSet()
                            + " which communicate with "
                            + service.getMemberStatsDescription(memberSuspect) + ", but is not"
                            + " responding directly to this member; that could mean that"
                            + " either requests are not coming out or responses are not"
                            + " coming in; marking " + (nComp == 0 ? "this" : "the lower priority")
                            + " member as suspect.", 1);
                
                        service.onMembersTimedOut(Collections.singleton(nComp <= 0 ? service.getThisMember() : memberSuspect));
                        }
                    else
                        {
                        // Some witnesses left and none of the responding witnesses
                        // confirmed the suspect. We require full rejection to kill
                        // ourselves (see above) so we can't take any action. Unmark
                        // the suspect and allow the witness protocol to repeat.
                        memberSuspect.setDeaf(false);
                        }
                    }
                }
            
            // Declared at the super level
            /**
             * This event occurs for each response Message from each polled
            * Member.
             */
            public void onResponse(com.tangosol.coherence.component.net.Message msg)
                {
                if (!isClosed() &&
                    ((ClusterService.Response) msg).getResult() == ClusterService.Response.RESULT_SUCCESS)
                    {
                    setResult(msg.getFromMember()); // one vote to confirms the kill
                    super.onResponse(msg);          // must occur between setResult and close
                    close();                        // close early on confirmation
                    }
                else
                    {
                    super.onResponse(msg);
                    }
                }
            }
        }
    }
