/*
 * Copyright (c) 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.component.net.extend;

import com.tangosol.coherence.component.net.Cluster;
import com.tangosol.coherence.component.net.extend.protocol.NamedTopicProtocol;
import com.tangosol.coherence.component.util.NameService;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.initiator.TcpInitiator;

import com.tangosol.io.DefaultSerializer;
import com.tangosol.util.LongArray;
import com.tangosol.util.SparseArray;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Regression coverage for pending channels shared by a manager and decode lane.
 *
 * @author phf  2026.10.29
 * @since 26.10
 */
public class ConnectionPendingChannelTest
    {
    @Test
    public void shouldRetainChannelCreatedWhileAnotherIsAccepted() throws Exception
        {
        PausedArray    pending    = new PausedArray();
        TestConnection connection = new TestConnection(pending);
        int            nFirst     = channelId(connection.createPending());
        TestChannel    first      = (TestChannel) connection.getChannelPendingArray().get(nFirst);

        pending.m_fPause = true;
        FutureTask<URI> create = new FutureTask<>(connection::createPending);
        FutureTask<Void> accept = new FutureTask<>(() ->
            {
            connection.acceptChannelRequest(nFirst, null, null);
            return null;
            });
        Thread creator  = new Thread(create, "pending-channel-creator");
        Thread acceptor = new Thread(accept, "pending-channel-acceptor");

        try
            {
            creator.start();
            await(pending.f_insertionReady);
            acceptor.start();

            // either acceptance finishes on the unsafe control, or it waits for insertion's lock
            awaitAcceptanceOrLock(accept, acceptor, creator);
            pending.f_resumeInsertion.countDown();

            int nSecond = channelId(create.get(10, TimeUnit.SECONDS));
            accept.get(10, TimeUnit.SECONDS);

            TestChannel second = (TestChannel) connection.getChannelPendingArray().get(nSecond);
            assertNotNull("accepting the first channel lost the second pending channel", second);
            connection.acceptChannelRequest(nSecond, null, null);

            assertSame(first, connection.getChannel(nFirst));
            assertSame(second, connection.getChannel(nSecond));
            assertEquals(1, first.m_cOpens);
            assertEquals(1, second.m_cOpens);
            assertEquals(0, connection.getChannelPendingArray().getSize());
            assertEquals(3, connection.getChannelArray().getSize());
            }
        finally
            {
            pending.f_resumeInsertion.countDown();
            creator.join(10000L);
            acceptor.join(10000L);
            assertFalse("channel creator did not terminate", creator.isAlive());
            assertFalse("channel acceptor did not terminate", acceptor.isAlive());
            }
        }

    @Test
    public void shouldRejectAbsentAndDuplicateChannels()
        {
        TestConnection connection = new TestConnection(new SparseArray<>());
        int            nId        = channelId(connection.createPending());

        assertEquals("channel 0 is reserved", assertThrows(IllegalArgumentException.class,
                () -> connection.acceptChannelRequest(0, null, null)).getMessage());
        assertEquals("no such channel: 99", assertThrows(IllegalArgumentException.class,
                () -> connection.acceptChannelRequest(99, null, null)).getMessage());

        connection.acceptChannelRequest(nId, null, null);

        assertEquals("channel already exists: " + nId, assertThrows(IllegalArgumentException.class,
                () -> connection.acceptChannelRequest(nId, null, null)).getMessage());
        }

    @Test
    public void shouldClearPendingChannelsWhenConnectionCloses()
        {
        TestConnection connection = new TestConnection(new SparseArray<>());
        connection.createPending();
        connection.createPending();
        assertEquals(2, connection.getChannelPendingArray().getSize());

        assertTrue(connection.closeInternal(false, null, 0L));

        assertFalse(connection.isOpen());
        assertEquals(0, connection.getChannelPendingArray().getSize());
        }

    @Test
    public void shouldKeepPendingChannelsIndependentAcrossConnectionTypes()
        {
        Connection[] aConnection =
            {
            new Connection(),
            new com.tangosol.coherence.component.net.extend.connection.TcpConnection(),
            new TcpAcceptor.TcpConnection(),
            new TcpInitiator.TcpConnection(),
            new NameService.TcpAcceptor.TcpConnection(),
            new Cluster.NameService.TcpAcceptor.TcpConnection()
            };
        Channel[] aChannel = new Channel[aConnection.length];

        for (int i = 0; i < aConnection.length; i++)
            {
            aChannel[i] = new Channel();
            aConnection[i].getChannelPendingArray().set(1, aChannel[i]);
            }

        for (int i = 0; i < aConnection.length; i++)
            {
            assertSame(aChannel[i], aConnection[i].getChannelPendingArray().remove(1));
            assertEquals(0, aConnection[i].getChannelPendingArray().getSize());
            }
        }

    // ----- test helpers ---------------------------------------------------

    private static int channelId(URI uri)
        {
        return Integer.parseInt(uri.getSchemeSpecificPart());
        }

    private static void await(CountDownLatch latch)
        {
        try
            {
            assertTrue("controlled insertion did not arrive", latch.await(10, TimeUnit.SECONDS));
            }
        catch (InterruptedException e)
            {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
            }
        }

    private static void awaitAcceptanceOrLock(FutureTask<?> accept, Thread acceptor, Thread creator)
        {
        long ldtDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (!accept.isDone())
            {
            ThreadInfo info = ManagementFactory.getThreadMXBean().getThreadInfo(acceptor.threadId());
            if (info != null && info.getLockOwnerId() == creator.threadId())
                {
                return;
                }
            assertTrue("acceptance neither completed nor waited on insertion's lock",
                    System.nanoTime() < ldtDeadline);
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1));
            }
        }

    private static class PausedArray extends SparseArray<Channel>
        {
        @Override
        protected Node<Channel> instantiateNode(long lKey, Channel channel)
            {
            if (m_fPause)
                {
                // set has selected the existing parent but has not linked the new node
                f_insertionReady.countDown();
                await(f_resumeInsertion);
                }
            return super.instantiateNode(lKey, channel);
            }

        final CountDownLatch f_insertionReady  = new CountDownLatch(1);
        final CountDownLatch f_resumeInsertion = new CountDownLatch(1);
        volatile boolean     m_fPause;
        }

    private static class TestConnection extends Connection
        {
        TestConnection(LongArray<Channel> pending)
            {
            setChannelPendingArray(pending);
            setConnectionManager(new Peer(null, null, false)
                {
                @Override
                public void onConnectionClosed(Connection connection)
                    {
                    }
                });
            setOpen(true);
            NamedTopicProtocol protocol = NamedTopicProtocol.getInstance();
            setMessageFactoryMap(Map.of(protocol.getName(), protocol.getMessageFactory(1)));
            }

        URI createPending()
            {
            return createChannelInternal(NamedTopicProtocol.getInstance(), new DefaultSerializer(), null);
            }

        @Override
        public com.tangosol.net.messaging.Channel getChannel(int nId)
            {
            LongArray channels = getChannelArray();
            synchronized (channels)
                {
                return (Channel) channels.get(nId);
                }
            }

        @Override
        protected Channel createChannel()
            {
            TestChannel channel = new TestChannel();
            channel.setConnection(this);
            return channel;
            }

        @Override
        protected int generateChannelId()
            {
            return ++m_nNextId;
            }

        int m_nNextId;
        }

    private static class TestChannel extends Channel
        {
        @Override
        public void openInternal()
            {
            ++m_cOpens;
            setOpen(true);
            }

        @Override
        public boolean closeInternal(boolean fNotify, Throwable error, long cMillis)
            {
            setOpen(false);
            return true;
            }

        int m_cOpens;
        }
    }
