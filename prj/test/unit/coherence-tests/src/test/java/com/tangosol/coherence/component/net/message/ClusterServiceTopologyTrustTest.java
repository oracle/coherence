/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.component.net.message;

import com.tangosol.coherence.component.net.Member;
import com.tangosol.coherence.component.net.packet.MessagePacket;
import com.tangosol.coherence.component.net.packet.messagePacket.Broadcast;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ClusterService;
import com.tangosol.io.ByteArrayWriteBuffer;
import com.tangosol.io.ReadBuffer;
import com.tangosol.io.WriteBuffer;
import com.tangosol.util.UUID;

import org.junit.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for discovery source-address provenance.
 *
 * @author OpenAI  2026.05.17
 *
 * @since 15.1.2.0
 */
public class ClusterServiceTopologyTrustTest
    {
    @Test
    public void shouldNotMarkPayloadFallbackAsObservedSource()
            throws Exception
        {
        InetSocketAddress address = new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 7574);
        Member            member  = new Member();
        member.configureDead(1, new UUID(System.currentTimeMillis(), address.getAddress(), address.getPort(), 1),
                System.currentTimeMillis());

        ByteArrayWriteBuffer buffer = new ByteArrayWriteBuffer(1024);
        WriteBuffer.BufferOutput out = buffer.getBufferOutput();
        member.writeExternal(out);
        out.writeBoolean(false);

        DiscoveryMessage message = new DiscoveryMessage();
        message.read(buffer.toBinary().getBufferInput());

        assertEquals(address, message.getSourceAddress());
        assertFalse(message.isSourceAddressObserved());
        }

    @Test
    public void shouldMarkBroadcastPacketSourceAsObserved()
            throws Exception
        {
        InetSocketAddress address = new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 7574);
        Broadcast         packet  = new Broadcast();
        packet.setFromAddress(address);

        DiscoveryMessage message = new DiscoveryMessage();
        message.setPacket(new MessagePacket[1]);
        message.setPacket(0, packet);

        assertEquals(address, message.getSourceAddress());
        assertTrue(message.isSourceAddressObserved());
        }

    @Test
    public void shouldRejectCollisionWithoutObservedPacketSource()
            throws Exception
        {
        InetSocketAddress address   = new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 7574);
        TestClusterService service   = new TestClusterService();
        Member             existing = member(1, address, 1);
        Member             incoming = member(2, address, 2);
        DiscoveryMessage   message  = new DiscoveryMessage();

        service.getServiceMemberSet().add(existing);
        message.setFromMember(incoming);
        message.setSourceAddress(address);

        assertFalse(service.validateNewMember(incoming, message));
        assertEquals(0, service.getMemberLeftCount());
        }

    @Test
    public void shouldAllowCollisionReplacementWithMatchingObservedPacketSource()
            throws Exception
        {
        InetSocketAddress address   = new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 7574);
        TestClusterService service   = new TestClusterService();
        Member             existing = member(1, address, 1);
        Member             incoming = member(2, address, 2);
        DiscoveryMessage   message  = new DiscoveryMessage();

        service.getServiceMemberSet().add(existing);
        message.setFromMember(incoming);
        message.setSourceAddress(address);
        message.setSourceAddressObserved(true);

        assertFalse(service.validateNewMember(incoming, message));
        assertEquals(1, service.getMemberLeftCount());
        assertEquals(existing, service.getLastMemberLeft());
        }

    private static Member member(int nId, InetSocketAddress address, int nCount)
        {
        Member member = new Member();
        member.configureDead(nId, new UUID(System.currentTimeMillis(), address.getAddress(), address.getPort(), nCount),
                System.currentTimeMillis());
        return member;
        }

    public static class TestClusterService
            extends ClusterService
        {
        @Override
        public void doMemberLeft(Member member)
            {
            m_cMemberLeft++;
            m_memberLeft = member;
            }

        int getMemberLeftCount()
            {
            return m_cMemberLeft;
            }

        Member getLastMemberLeft()
            {
            return m_memberLeft;
            }

        private int    m_cMemberLeft;
        private Member m_memberLeft;
        }
    }
