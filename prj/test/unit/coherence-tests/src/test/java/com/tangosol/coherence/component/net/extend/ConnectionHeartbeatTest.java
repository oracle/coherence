/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.component.net.extend;

import com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer;

import com.tangosol.net.messaging.Protocol;
import com.tangosol.net.messaging.Request;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for concurrent Extend connection heartbeat handling.
 */
public class ConnectionHeartbeatTest
    {
    @Test
    public void shouldPublishOutstandingPingBeforeSending()
        {
        TestConnection connection = new TestConnection();
        TestChannel    channel    = new TestChannel(connection, false);

        connection.setChannel(channel);
        channel.setMessageFactory(createMessageFactory());

        assertThat(connection.ping(), is(true));
        assertThat(channel.wasSent(), is(true));
        assertThat(connection.getPingLastMillis(), is(0L));
        }

    @Test
    public void shouldClearOutstandingPingWhenSendFails()
        {
        TestConnection connection = new TestConnection();
        TestChannel    channel    = new TestChannel(connection, true);

        connection.setChannel(channel);
        channel.setMessageFactory(createMessageFactory());

        assertThat(connection.ping(), is(false));
        assertThat(connection.getPingLastMillis(), is(0L));
        }

    @Test
    public void shouldPublishPingTimestampAcrossThreads()
            throws NoSuchFieldException
        {
        Field field = Connection.class.getDeclaredField("__m_PingLastMillis");

        assertThat(Modifier.isVolatile(field.getModifiers()), is(true));
        }

    private Protocol.MessageFactory createMessageFactory()
        {
        Protocol.MessageFactory factory = mock(Protocol.MessageFactory.class);
        when(factory.createMessage(Peer.MessageFactory.PingRequest.TYPE_ID))
                .thenReturn(new Peer.MessageFactory.PingRequest());
        return factory;
        }

    // ----- inner class: TestConnection ----------------------------------

    private static class TestConnection
            extends Connection
        {
        @Override
        public com.tangosol.net.messaging.Channel getChannel(int nId)
            {
            return m_channel;
            }

        public void setChannel(Channel channel)
            {
            m_channel = channel;
            }

        private Channel m_channel;
        }

    // ----- inner class: TestChannel -------------------------------------

    private static class TestChannel
            extends Channel
        {
        private TestChannel(Connection connection, boolean fFail)
            {
            f_connection = connection;
            f_fFail       = fFail;
            }

        @Override
        public Request.Status send(Request request)
            {
            m_fSent = true;
            if (f_fFail)
                {
                throw new IllegalStateException("expected send failure");
                }

            // simulate a fast response processed by a connection pipeline
            // before the service-thread send call returns
            f_connection.setPingLastMillis(0L);
            return null;
            }

        public boolean wasSent()
            {
            return m_fSent;
            }

        private final Connection f_connection;

        private final boolean f_fFail;

        private boolean m_fSent;
        }
    }
