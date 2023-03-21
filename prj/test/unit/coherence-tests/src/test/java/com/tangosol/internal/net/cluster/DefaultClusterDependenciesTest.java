/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.cluster;

import com.tangosol.coherence.config.builder.ActionPolicyBuilder;
import com.tangosol.coherence.config.builder.ClusterQuorumPolicyBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.coherence.config.builder.ServiceFailurePolicyBuilder;
import com.tangosol.config.expression.NullParameterResolver;
import com.tangosol.internal.net.InetAddressRangeFilter;

import com.tangosol.io.Serializer;
import com.tangosol.io.SerializerFactory;
import com.tangosol.io.WrapperStreamFactory;

import com.tangosol.net.AddressProvider;
import com.tangosol.net.CompositeAddressProvider;
import com.tangosol.net.ServiceFailurePolicy;
import com.tangosol.net.SocketOptions;
import com.tangosol.net.SocketProviderFactory;

import com.tangosol.util.Base;
import com.tangosol.util.Filter;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for DefaultClusterDependencies.
 *
 * @author pfm  2011.07.18
 */
public class DefaultClusterDependenciesTest
    {
    // ----- tests ----------------------------------------------------------

    /**
     * Test the setters to make sure the value that was set was returned.  In the
     * case of MemberIdentity, the object is cloned so just check that the getter value
     * is not null.
     *
     * Also, piggyback the clone test with this test since we want to clone a
     * populated object.
     */
    @Test
    public void testAccessAndClone()
        {
        int         n;
        long        ln;
        double      dn;
        boolean     flag;
        InetAddress addr;

        DefaultClusterDependencies deps = createDeps();
        deps.validate();
        System.out.println(deps.toString());

        Filter filter = new InetAddressRangeFilter();
        deps.setAuthorizedHostFilter(filter);
        assertEquals(deps.getAuthorizedHostFilter(), filter);

        ActionPolicyBuilder builder =
            new ClusterQuorumPolicyBuilder(null, null);
        deps.setClusterActionPolicyBuilder(builder);
        assertEquals(deps.getClusterActionPolicyBuilder(), builder);

        deps.setClusterAnnounceTimeoutMillis(n = deps.getClusterAnnounceTimeoutMillis() + 1);
        assertEquals(deps.getClusterAnnounceTimeoutMillis(), n);

        deps.setClusterHeartbeatDelayMillis(n = deps.getClusterHeartbeatDelayMillis() + 1);
        assertEquals(deps.getClusterHeartbeatDelayMillis(), n);

        deps.setClusterTimestampMaxVarianceMillis(n = deps.getClusterTimestampMaxVarianceMillis() + 1);
        assertEquals(deps.getClusterTimestampMaxVarianceMillis(), n);

        deps.setEdition(DefaultClusterDependencies.EDITION_DATA_CLIENT);
        assertEquals(deps.getEdition(), DefaultClusterDependencies.EDITION_DATA_CLIENT);

        LinkedList<String> listFilter = new LinkedList<String>();
        deps.setFilterList(listFilter);
        assertEquals(deps.getFilterList(), listFilter);

        Map<String, WrapperStreamFactory> mapFilter = new HashMap<String, WrapperStreamFactory>();
        deps.setFilterMap(mapFilter);
        assertEquals(deps.getFilterMap(), mapFilter);

        deps.setFlowControlEnabled(flag = !deps.isFlowControlEnabled());
        assertEquals(deps.isFlowControlEnabled(), flag);

        addr = createAddress("224.0.0.57");
        deps.setGroupAddress(addr);
        assertEquals(deps.getGroupAddress(), addr);

        deps.setGroupBufferSize(n = deps.getGroupBufferSize() + 1);
        assertEquals(deps.getGroupBufferSize(), n);

        addr = createAddress("224.9.9.58");
        deps.setGroupInterface(addr);
        assertEquals(deps.getGroupInterface(), addr);

        deps.setGroupListenerPriority(n = deps.getGroupListenerPriority() - 1);
        assertEquals(deps.getGroupListenerPriority(), n);

        deps.setGroupPort(n = deps.getGroupPort() + 1);
        assertEquals(deps.getGroupPort(), n);

        deps.setGroupTimeToLive(n = deps.getGroupTimeToLive() + 1);
        assertEquals(deps.getGroupTimeToLive(), n);

        deps.setGuardTimeoutMillis(ln = deps.getGuardTimeoutMillis() + 1);
        assertEquals(deps.getGuardTimeoutMillis(),ln);

        deps.setIpMonitorAttempts(n = deps.getIpMonitorAttempts() + 1);
        assertEquals(deps.getIpMonitorAttempts(), n);

        deps.setIpMonitorPriority(n = deps.getIpMonitorPriority() + 1);
        assertEquals(deps.getIpMonitorPriority(), n);

        deps.setIpMonitorTimeoutMillis(ln = deps.getIpMonitorTimeoutMillis() + 1);
        assertEquals(deps.getIpMonitorTimeoutMillis(), ln);

        addr = createAddress("121.2.3.4");
        deps.setLocalAddress(addr);
        assertEquals(deps.getLocalAddress(), addr);

        deps.setLocalBufferSize(n = deps.getLocalBufferSize() + 1);
        assertEquals(deps.getLocalBufferSize(), n);

        deps.setLocalListenerPriority(n = deps.getLocalListenerPriority() - 1);
        assertEquals(deps.getLocalListenerPriority(), n);

        deps.setLocalPort(n = deps.getLocalPort() + 1);
        assertEquals(deps.getLocalPort(), n);

        deps.setLocalPortAutoAdjust(flag = !deps.isLocalPortAutoAdjust());
        assertEquals(deps.isLocalPortAutoAdjust(), flag);

        deps.setLostPacketThreshold(n = deps.getLostPacketThreshold() + 1);
        assertEquals(deps.getLostPacketThreshold(), n);

        // DefaultClusterDependencies will clone MemberIdentity so just check for null
        deps.setMemberIdentity(new DefaultMemberIdentity());
        assertNotNull(deps.getMemberIdentity());

        deps.setMode(n = DefaultClusterDependencies.LICENSE_MODE_EVALUATION);
        assertEquals(deps.getMode(), n);

        deps.setOutstandingPacketMaximum(n = deps.getOutstandingPacketMaximum() + 1);
        assertEquals(deps.getOutstandingPacketMaximum(), n);

        deps.setOutstandingPacketMinimum(n = deps.getOutstandingPacketMinimum() + 1);
        assertEquals(deps.getOutstandingPacketMinimum(), n);

        deps.setPacketBundlingAggression(dn = deps.getPacketBundlingAggression() + 1);
        assertEquals(Double.valueOf(deps.getPacketBundlingAggression()), Double.valueOf(dn));

        deps.setPacketBundlingThresholdNanos(ln = deps.getPacketBundlingThresholdNanos() + 1);
        assertEquals(deps.getPacketBundlingThresholdNanos(), ln);

        deps.setPacketMaxLength(n = deps.getPacketMaxLength() - 1);
        assertEquals(deps.getPacketMaxLength(), n);

        deps.setPacketPreferredLength(n = deps.getPacketPreferredLength() + 1);
        assertEquals(deps.getPacketPreferredLength(), n);

        deps.setPublisherAckDelayMillis(n = deps.getPublisherAckDelayMillis() + 1);
        assertEquals(deps.getPublisherAckDelayMillis(), n);

        deps.setPublisherCloggedCount(n = deps.getPublisherCloggedCount() + 1);
        assertEquals(deps.getPublisherCloggedCount(), n);

        deps.setPublisherCloggedDelayMillis(n = deps.getPublisherCloggedDelayMillis() + 1);
        assertEquals(deps.getPublisherCloggedDelayMillis(), n);

        deps.setPublisherGroupThreshold(n = deps.getPublisherGroupThreshold() - 1);
        assertEquals(deps.getPublisherGroupThreshold(), n);

        deps.setPublisherNackDelayMillis(n = deps.getPublisherNackDelayMillis() + 1);
        assertEquals(deps.getPublisherNackDelayMillis(), n);

        deps.setPublisherPriority(n = deps.getPublisherPriority() - 1);
        assertEquals(deps.getPublisherPriority(),n);

        deps.setPublisherResendDelayMillis(n = deps.getPublisherResendDelayMillis() + 1);
        assertEquals(deps.getPublisherResendDelayMillis(), n);

        deps.setPublisherResendTimeoutMillis(n = deps.getPublisherResendTimeoutMillis() + 1);
        assertEquals(deps.getPublisherResendTimeoutMillis(), n);

        deps.setPublisherSocketBufferSize(n = deps.getPublisherSocketBufferSize() + 1);
        assertEquals(deps.getPublisherSocketBufferSize(), n);

        deps.setReceiverNackEnabled(flag = !deps.isReceiverNackEnabled());
        assertEquals(deps.isReceiverNackEnabled(), flag);

        deps.setReceiverPriority(n = deps.getReceiverPriority() - 1);
        assertEquals(deps.getReceiverPriority(), n);

        deps.setReliableTransport("fooTransport");
        assertEquals(deps.getReliableTransport(), "fooTransport");

        deps.getBuilderRegistry().registerBuilder(Serializer.class, "pof", Mockito.mock(ParameterizedBuilder.class));
        deps.getBuilderRegistry().registerBuilder(Serializer.class, "java", Mockito.mock(ParameterizedBuilder.class));
        Map<String, SerializerFactory> mapSerializer = deps.getSerializerMap();
        assertNotNull(mapSerializer);
        assertNotNull(mapSerializer.get("pof"));
        assertNotNull(mapSerializer.get("java"));


        ServiceFailurePolicyBuilder bldrFailurePolicy =
            new ServiceFailurePolicyBuilder(DefaultServiceFailurePolicy.POLICY_EXIT_CLUSTER);
        assertNotNull("validate default cluster servicefailurePolicy", deps.getServiceFailurePolicyBuilder());
        assertTrue(deps.getServiceFailurePolicyBuilder().realize(new NullParameterResolver(), null, null) instanceof ServiceFailurePolicy);
        deps.setServiceFailurePolicyBuilder(bldrFailurePolicy);
        assertEquals(deps.getServiceFailurePolicyBuilder(), bldrFailurePolicy);

        Map<String, List<String>> mapServiceFilter = new  HashMap<String, List<String>>();
        deps.setServiceFilterMap(mapServiceFilter);
        assertEquals(deps.getServiceFilterMap(), mapServiceFilter);

        Map<String, String> mapService = new HashMap<String, String>();
        deps.setServiceMap(mapService);
        assertEquals(deps.getServiceMap(), mapService);

        deps.setShutdownHookOption(n = deps.getShutdownHookOption() + 1);
        assertEquals(deps.getShutdownHookOption(), n);

        SocketProviderFactory factory = new SocketProviderFactory();
        deps.setSocketProviderFactory(factory);
        assertEquals(deps.getSocketProviderFactory(), factory);

        deps.setSpeakerPriority(n = deps.getSpeakerPriority() - 1);
        assertEquals(deps.getSpeakerPriority(), n);

        deps.setSpeakerVolumeMinimum(n = deps.getSpeakerVolumeMinimum() + 1);
        assertEquals(deps.getSpeakerVolumeMinimum(), n);

        deps.setTcmpEnabled(flag = !deps.isTcmpEnabled());
        assertEquals(deps.isTcmpEnabled(), flag);

        deps.setTcpBacklog(n = deps.getTcpBacklog() + 1);
        assertEquals(deps.getTcpBacklog(), n);

        SocketOptions options = new SocketOptions();
        deps.setTcpDatagramSocketOptions(options);
        assertEquals(deps.getTcpDatagramSocketOptions(), options);

        deps.setTcpRingEnabled(flag = !deps.isTcpRingEnabled());
        assertEquals(deps.isTcpRingEnabled(), flag);

        options = new SocketOptions();
        deps.setTcpRingSocketOptions(options);
        assertEquals(deps.getTcpRingSocketOptions(), options);

        AddressProvider providerAddr = new CompositeAddressProvider();
        deps.setWellKnownAddresses(providerAddr);
        assertEquals(deps.getWellKnownAddresses(), providerAddr);

        deps.validate();
        System.out.println(deps.toString());

        // test the clone since the object is already populated.
        DefaultClusterDependencies deps2 = new DefaultClusterDependencies(deps);
        assertCloneEquals(deps, deps2);
        deps2.validate();
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Assert that the two ClusterDependencies are equal.
     *
     * @param deps1  the first ClusterDependencies object
     * @param deps2  the second ClusterDependencies object
     */
    protected void assertCloneEquals(ClusterDependencies deps1, ClusterDependencies deps2)
        {
        assertEquals(deps1.getAuthorizedHostFilter(),              deps2.getAuthorizedHostFilter());
        assertEquals(deps1.getClusterActionPolicyBuilder(),        deps2.getClusterActionPolicyBuilder());
        assertEquals(deps1.getClusterAnnounceTimeoutMillis(),      deps2.getClusterAnnounceTimeoutMillis());
        assertEquals(deps1.getClusterHeartbeatDelayMillis(),       deps2.getClusterHeartbeatDelayMillis());
        assertEquals(deps1.getClusterTimestampMaxVarianceMillis(), deps2.getClusterTimestampMaxVarianceMillis());
        assertEquals(deps1.getEdition(),                           deps2.getEdition());
        assertEquals(deps1.getFilterList(),                        deps2.getFilterList());
        assertEquals(deps1.getFilterMap(),                         deps2.getFilterMap());
        assertEquals(deps1.isFlowControlEnabled(),                 deps2.isFlowControlEnabled());
        assertEquals(deps1.getGroupAddress(),                      deps2.getGroupAddress());
        assertEquals(deps1.getGroupBufferSize(),                   deps2.getGroupBufferSize());
        assertEquals(deps1.getGroupInterface(),                    deps2.getGroupInterface());
        assertEquals(deps1.getGroupListenerPriority(),             deps2.getGroupListenerPriority());
        assertEquals(deps1.getGroupPort(),                         deps2.getGroupPort());
        assertEquals(deps1.getGroupTimeToLive(),                   deps2.getGroupTimeToLive());
        assertEquals(deps1.getGuardTimeoutMillis(),                deps2.getGuardTimeoutMillis());
        assertEquals(deps1.getIpMonitorAttempts(),                 deps2.getIpMonitorAttempts());
        assertEquals(deps1.getIpMonitorPriority(),                 deps2.getIpMonitorPriority());
        assertEquals(deps1.getIpMonitorTimeoutMillis(),            deps2.getIpMonitorTimeoutMillis());
        assertEquals(deps1.getLocalAddress(),                      deps2.getLocalAddress());
        assertEquals(deps1.getLocalBufferSize(),                   deps2.getLocalBufferSize());
        assertEquals(deps1.getLocalListenerPriority(),             deps2.getLocalListenerPriority());
        assertEquals(deps1.getLocalPort(),                         deps2.getLocalPort());
        assertEquals(deps1.isLocalPortAutoAdjust(),                deps2.isLocalPortAutoAdjust());
        assertEquals(deps1.getMode(),                              deps2.getMode());
        assertEquals(deps1.getOutstandingPacketMaximum(),          deps2.getOutstandingPacketMaximum());
        assertEquals(deps1.getOutstandingPacketMinimum(),          deps2.getOutstandingPacketMinimum());
        assertTrue(deps1.getPacketBundlingAggression() ==          deps2.getPacketBundlingAggression());
        assertEquals(deps1.getPacketBundlingThresholdNanos(),      deps2.getPacketBundlingThresholdNanos());
        assertEquals(deps1.getPacketMaxLength(),                   deps2.getPacketMaxLength());
        assertEquals(deps1.getPacketPreferredLength(),             deps2.getPacketPreferredLength());
        assertEquals(deps1.getPublisherAckDelayMillis(),           deps2.getPublisherAckDelayMillis());
        assertEquals(deps1.getPublisherCloggedCount(),             deps2.getPublisherCloggedCount());
        assertEquals(deps1.getPublisherCloggedDelayMillis(),       deps2.getPublisherCloggedDelayMillis());
        assertEquals(deps1.getPublisherGroupThreshold(),           deps2.getPublisherGroupThreshold());
        assertEquals(deps1.getPublisherNackDelayMillis(),          deps2.getPublisherNackDelayMillis());
        assertEquals(deps1.getPublisherPriority(),                 deps2.getPublisherPriority());
        assertEquals(deps1.getPublisherResendDelayMillis(),        deps2.getPublisherResendDelayMillis());
        assertEquals(deps1.getPublisherResendTimeoutMillis(),      deps2.getPublisherResendTimeoutMillis());
        assertEquals(deps1.getPublisherSocketBufferSize(),         deps2.getPublisherSocketBufferSize());
        assertEquals(deps1.getPublisherResendTimeoutMillis(),      deps2.getPublisherResendTimeoutMillis());
        assertEquals(deps1.getPublisherSocketBufferSize(),         deps2.getPublisherSocketBufferSize());
        assertEquals(deps1.isReceiverNackEnabled(),                deps2.isReceiverNackEnabled());
        assertEquals(deps1.getReceiverPriority(),                  deps2.getReceiverPriority());
        assertEquals(deps1.getReliableTransport(),                 deps2.getReliableTransport());
        assertEquals(deps1.getSerializerMap(),                     deps2.getSerializerMap());
        assertEquals(deps1.getServiceFailurePolicyBuilder(),       deps2.getServiceFailurePolicyBuilder());
        assertEquals(deps1.getServiceFilterMap(),                  deps2.getServiceFilterMap());
        assertEquals(deps1.getServiceMap(),                        deps2.getServiceMap());
        assertEquals(deps1.getShutdownHookOption(),                deps2.getShutdownHookOption());
        assertEquals(deps1.getSocketProviderFactory(),             deps2.getSocketProviderFactory());
        assertEquals(deps1.getSpeakerPriority(),                   deps2.getSpeakerPriority());
        assertEquals(deps1.getSpeakerVolumeMinimum(),              deps2.getSpeakerVolumeMinimum());
        assertEquals(deps1.isTcmpEnabled(),                        deps2.isTcmpEnabled());
        assertEquals(deps1.getTcpBacklog(),                        deps2.getTcpBacklog());
        assertEquals(deps1.isTcpRingEnabled(),                     deps2.isTcpRingEnabled());
        assertEquals(deps1.getTcpRingSocketOptions(),              deps2.getTcpRingSocketOptions());
        assertEquals(deps1.getWellKnownAddresses(),                deps2.getWellKnownAddresses());
        assertEquals(deps1.getLambdasSerializationMode(),          deps2.getLambdasSerializationMode());
        }

    /**
     * Create a DefaultClusterDependencies object.  This is needed until the 3 jira's described
     * below are fixed.  This method is also used by ClusterMemberIdentityTest.
     *
     * @return DefaultClusterDependencies
     */
    static DefaultClusterDependencies createDeps()
        {
        DefaultClusterDependencies deps = new DefaultClusterDependencies();

        // Remove this when COH-5424 is fixed.
        deps.setGroupAddress(createAddress("224.0.0.87"));

        // Remove this when COH-5431 is fixed
        deps.setMemberIdentity(new DefaultMemberIdentity());

        // Defaults to no value being set, verify that a set value is preserved by clone().
        deps.setLambdasSerializationMode("static");

        return deps;
        }

    /**
     * Create an InetAddress.
     *
     * @param sAddr  the address in string format
     *
     * @return the InetAddress
     */
    static private InetAddress createAddress(String sAddr)
        {
        // Remove this when COH-5424 is fixed.
        try
            {
            return InetAddress.getByName(sAddr);
            }
        catch (UnknownHostException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }
    }
