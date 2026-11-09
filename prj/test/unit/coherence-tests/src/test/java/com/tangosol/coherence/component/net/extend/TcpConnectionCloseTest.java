/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.component.net.extend;

import com.tangosol.coherence.component.util.daemon.queueProcessor.service.Peer;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor;

import com.tangosol.net.messaging.ConnectionException;
import com.tangosol.net.messaging.Message;
import com.tangosol.net.messaging.Protocol;
import com.tangosol.util.ThreadGate;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Regression coverage for the acceptor entry-barrier and last-worker close handoff.
 *
 * @author phf  2026.10.16
 * @since 26.10
 */
public class TcpConnectionCloseTest
    {
    @Test
    public void shouldCloseWhenLastWorkerExitsBeforeBarrierOpens() throws Exception
        {
        assertLastWorkerClose(false, null);
        }

    @Test
    public void shouldRetainErrorWhenLastWorkerExitsBeforeBarrierOpens() throws Exception
        {
        assertLastWorkerClose(false, new IllegalStateException("connection failed"));
        }

    @Test
    public void shouldAttemptPeerNotificationWhenLastWorkerExitsBeforeBarrierOpens() throws Exception
        {
        assertLastWorkerClose(true, new IllegalStateException("connection failed"));
        }

    @Test
    public void shouldPostPeerNotificationThroughChannelWhenIdle()
        {
        ThreadGate     gate       = new ThreadGate();
        TestPeer       peer       = new TestPeer();
        TestConnection connection = new TestConnection(gate, peer);
        TestChannel    channel    = useChannelPath(connection);
        Throwable      error      = new IllegalStateException("idle close");

        assertTrue(connection.closeInternal(true, error, 0L));
        assertClosed(connection, gate, peer);
        assertEquals(1, channel.m_cSendAttempts);
        assertEquals(1, peer.f_cPosts.get());
        assertNull(channel.m_sendFailure);
        assertSame(channel, peer.m_posted.getChannel());
        assertSame(error, ((Peer.MessageFactory.NotifyConnectionClosed) peer.m_posted).getCause());
        assertSame(error, peer.m_error);
        }

    @Test
    public void shouldRejectPeerNotificationThroughChannelWhenLastWorkerExitsBeforeBarrierOpens()
            throws Exception
        {
        DrainBeforeOpenGate gate       = new DrainBeforeOpenGate();
        TestPeer            peer       = new TestPeer();
        TestConnection      connection = new TestConnection(gate, peer);
        TestChannel         channel    = useChannelPath(connection);
        Throwable           error      = new IllegalStateException("last worker close");

        try (Worker worker = new Worker(connection))
            {
            gate.m_worker = worker;
            assertTrue(connection.closeInternal(true, error, 0L));
            assertTrue(gate.m_fDrained);
            assertClosed(connection, gate, peer);
            assertNotificationRejected(channel, peer, error);
            }
        }

    @Test
    public void shouldRejectPeerNotificationThroughChannelWhenCloseIsDeferredToOwner()
            throws Exception
        {
        ThreadGate       gate       = new ThreadGate();
        OwnerRoutingPeer peer       = new OwnerRoutingPeer();
        TestConnection   connection = new TestConnection(gate, peer);
        TestChannel      channel    = useChannelPath(connection);
        Throwable        error      = new IllegalStateException("deferred close");

        try (Worker worker = new Worker(connection))
            {
            assertFalse(connection.closeInternal(true, error, 0L));
            assertTrue(connection.isOpen());
            assertTrue(connection.isCloseOnExit());
            assertEquals(0, channel.m_cSendAttempts);
            assertEquals(0, peer.f_cClosed.get());

            worker.f_release.countDown();
            peer.drainClose();
            worker.finish();

            assertEquals(1, peer.f_cRoutedCloses.get());
            assertClosed(connection, gate, peer);
            assertNotificationRejected(channel, peer, error);
            }
        }

    @Test
    public void shouldDeferCloseUntilWorkerExitsAfterBarrierOpens() throws Exception
        {
        ThreadGate gate       = new ThreadGate();
        TestPeer   peer       = new TestPeer();
        TestConnection connection = new TestConnection(gate, peer);

        try (Worker worker = new Worker(connection))
            {
            assertFalse(connection.closeInternal(false, null, 0L));
            assertTrue(connection.isOpen());
            assertTrue(connection.isCloseOnExit());
            assertEquals(0, peer.f_cClosed.get());
            worker.finish();
            assertClosed(connection, gate, peer);
            }
        }

    @Test
    public void shouldWaitForAllWorkersBeforeCompletingClose() throws Exception
        {
        DrainBeforeOpenGate gate = new DrainBeforeOpenGate();
        TestPeer            peer = new TestPeer();
        TestConnection connection = new TestConnection(gate, peer);

        try (Worker first = new Worker(connection); Worker last = new Worker(connection))
            {
            gate.m_worker = first;
            assertFalse(connection.closeInternal(false, null, 0L));
            assertTrue(gate.m_fDrained);
            assertTrue(connection.isOpen());
            assertEquals(1, gate.getActiveCount());
            assertEquals(0, peer.f_cClosed.get());
            last.finish();
            assertClosed(connection, gate, peer);
            }
        }

    @Test
    public void shouldCloseIdleConnectionOnlyOnce()
        {
        ThreadGate gate       = new ThreadGate();
        TestPeer   peer       = new TestPeer();
        TestConnection connection = new TestConnection(gate, peer);

        assertTrue(connection.closeInternal(false, null, 0L));
        assertClosed(connection, gate, peer);
        assertFalse(connection.closeInternal(false, null, 0L));
        assertClosed(connection, gate, peer);
        }

    @Test
    public void shouldAllowCloseFromEnteredServiceThread()
        {
        ThreadGate gate       = new ThreadGate();
        TestPeer   peer       = new TestPeer();
        TestConnection connection = new TestConnection(gate, peer);

        connection.gateEnter();
        try
            {
            assertTrue(connection.closeInternal(false, null, 0L));
            assertFalse(connection.isOpen());
            assertEquals(1, peer.f_cClosed.get());
            }
        finally
            {
            connection.gateExit();
            }
        assertClosed(connection, gate, peer);
        }

    @Test
    public void shouldCompleteDeferredCloseAfterCloseRequestLimit() throws Exception
        {
        assertDeferredCloseAfterLimit(new TestPeer());
        }

    @Test
    public void shouldRouteDeferredCloseAfterCloseRequestLimit() throws Exception
        {
        assertDeferredCloseAfterLimit(new OwnerRoutingPeer());
        }

    private void assertDeferredCloseAfterLimit(TestPeer peer) throws Exception
        {
        ThreadGate     gate       = new ThreadGate();
        TestConnection connection = new TestConnection(gate, peer);
        Throwable      error      = new IllegalStateException("deferred close");

        try (Worker worker = new Worker(connection))
            {
            for (int i = 0; i < 100; i++)
                {
                connection.close(false, error, false);
                }
            assertEquals(100, connection.getPendingCloses());
            assertTrue(connection.isOpen());
            assertTrue(connection.isCloseOnExit());
            assertEquals(0, peer.f_cClosed.get());

            int cAttempts = connection.f_cCloseAttempts.get();
            connection.close(true, new IllegalArgumentException("duplicate close"), false);
            assertEquals(100, connection.getPendingCloses());
            assertEquals("ordinary duplicate requests must remain throttled",
                    cAttempts, connection.f_cCloseAttempts.get());

            if (peer instanceof OwnerRoutingPeer owner)
                {
                worker.f_release.countDown();
                owner.drainClose();
                worker.finish();
                assertEquals("deferred completion must reach the owner despite the request limit",
                        1, owner.f_cRoutedCloses.get());
                }
            else
                {
                worker.finish();
                }

            assertClosed(connection, gate, peer);
            assertSame(error, peer.m_error);
            assertEquals(1, peer.f_cErrors.get());
            }
        }

    private void assertLastWorkerClose(boolean fNotify, Throwable error) throws Exception
        {
        DrainBeforeOpenGate gate = new DrainBeforeOpenGate();
        TestPeer            peer = new TestPeer();
        TestConnection connection = new TestConnection(gate, peer);

        try (Worker worker = new Worker(connection))
            {
            gate.m_worker = worker;
            TestChannel channel = (TestChannel) connection.getChannel(0);
            assertTrue(connection.closeInternal(fNotify, error, 0L));
            assertTrue(gate.m_fDrained);
            assertClosed(connection, gate, peer);
            assertSame(error, peer.m_error);
            assertEquals(error == null ? 0 : 1, peer.f_cErrors.get());
            // the stub checks the requested send and the original error object
            assertEquals(fNotify ? 1 : 0, channel.m_cSendAttempts);
            if (fNotify)
                {
                assertSame(error, channel.m_error);
                }
            }
        }

    private static TestChannel useChannelPath(TestConnection connection)
        {
        TestChannel channel = (TestChannel) connection.getChannel(0);
        channel.m_fUseChannelPath = true;
        return channel;
        }

    private static void assertNotificationRejected(TestChannel channel, TestPeer peer, Throwable error)
        {
        assertEquals(1, channel.m_cSendAttempts);
        assertNotNull(channel.m_sendFailure);
        assertTrue(channel.m_sendFailure instanceof ConnectionException);
        assertTrue(channel.m_sendFailure.getMessage().contains("connection is closing"));
        assertEquals(0, peer.f_cPosts.get());
        assertNull(peer.m_posted);
        assertSame(error, channel.m_error);
        assertSame(error, peer.m_error);
        assertEquals(1, peer.f_cErrors.get());
        }

    private static void assertClosed(TestConnection connection, ThreadGate gate, TestPeer peer)
        {
        assertFalse(connection.isOpen());
        assertEquals(0, gate.getActiveCount());
        assertEquals(0, gate.getCloseCount());
        assertEquals(1, peer.f_cClosed.get());
        }

    private static void await(CountDownLatch latch)
        {
        try
            {
            assertTrue("worker did not reach the controlled close ordering",
                    latch.await(10, TimeUnit.SECONDS));
            }
        catch (InterruptedException e)
            {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
            }
        }

    // ----- test helpers ---------------------------------------------------

    private static class Worker implements AutoCloseable
        {
        Worker(TestConnection connection)
            {
            f_task = new FutureTask<>(() ->
                {
                connection.gateEnter();
                f_entered.countDown();
                try
                    {
                    await(f_release);
                    }
                finally
                    {
                    connection.gateExit();
                    }
                return null;
                });
            Thread thread = new Thread(f_task, "tcp-close-last-worker");
            thread.setDaemon(true);
            thread.start();
            await(f_entered);
            }

        void finish() throws Exception
            {
            f_release.countDown();
            f_task.get(10, TimeUnit.SECONDS);
            }

        @Override
        public void close() throws Exception
            {
            finish();
            }

        final CountDownLatch f_entered = new CountDownLatch(1);
        final CountDownLatch f_release = new CountDownLatch(1);
        final FutureTask<Void> f_task;
        }

    private static class DrainBeforeOpenGate extends ThreadGate
        {
        @Override
        public void open()
            {
            if (!m_fDrained && getCloseCount() == 1)
                {
                m_fDrained = true;
                try
                    {
                    m_worker.finish();
                    }
                catch (Exception e)
                    {
                    throw new AssertionError(e);
                    }
                }
            super.open();
            }

        Worker  m_worker;
        boolean m_fDrained;
        }

    private static class TestConnection extends TcpAcceptor.TcpConnection
        {
        TestConnection(ThreadGate gate, TestPeer peer)
            {
            setThreadGate(gate);
            setConnectionManager(peer);
            setOpen(true);
            ((TestChannel) getChannel(0)).setOpened();
            }

        @Override
        public boolean closeInternal(boolean fNotify, Throwable error, long cMillis)
            {
            f_cCloseAttempts.incrementAndGet();
            return super.closeInternal(fNotify, error, cMillis);
            }

        @Override
        protected Channel createChannel()
            {
            TestChannel channel = new TestChannel();
            channel.setConnection(this);
            return channel;
            }

        final AtomicInteger f_cCloseAttempts = new AtomicInteger();
        }

    private static class TestChannel extends Channel
        {
        void setOpened()
            {
            setOpen(true);
            }

        @Override
        public Protocol.MessageFactory getMessageFactory()
            {
            return new Peer.MessageFactory(null, null, false)
                {
                @Override
                public Message createMessage(int nType)
                    {
                    assertEquals(Peer.MessageFactory.NotifyConnectionClosed.TYPE_ID, nType);
                    return new Peer.MessageFactory.NotifyConnectionClosed();
                    }
                };
            }

        @Override
        public void send(Message message)
            {
            m_cSendAttempts++;
            m_error = ((Peer.MessageFactory.NotifyConnectionClosed) message).getCause();
            if (m_fUseChannelPath)
                {
                try
                    {
                    super.send(message);
                    }
                catch (RuntimeException e)
                    {
                    m_sendFailure = e;
                    throw e;
                    }
                }
            }

        int              m_cSendAttempts;
        Throwable        m_error;
        boolean          m_fUseChannelPath;
        RuntimeException m_sendFailure;
        }

    private static class TestPeer extends Peer
        {
        TestPeer()
            {
            super(null, null, false);
            }

        @Override
        public boolean isServiceThread(Connection connection)
            {
            return true;
            }

        @Override
        public void post(Message message)
            {
            f_cPosts.incrementAndGet();
            m_posted = message;
            }

        @Override
        public void onConnectionClosed(Connection connection)
            {
            f_cClosed.incrementAndGet();
            }

        @Override
        public void onConnectionError(Connection connection, Throwable error)
            {
            m_error = error;
            f_cErrors.incrementAndGet();
            f_cClosed.incrementAndGet();
            }

        final AtomicInteger f_cClosed = new AtomicInteger();
        final AtomicInteger f_cErrors = new AtomicInteger();
        final AtomicInteger f_cPosts  = new AtomicInteger();
        Throwable m_error;
        Message   m_posted;
        }

    private static class OwnerRoutingPeer extends TestPeer
        {
        @Override
        public boolean isServiceThread(Connection connection)
            {
            return Thread.currentThread() == f_owner;
            }

        @Override
        public void closeConnection(Connection connection, boolean fNotify, Throwable error, boolean fWait)
            {
            assertFalse(isServiceThread(connection));
            assertFalse(connection.isActiveThread());
            f_cRoutedCloses.incrementAndGet();

            FutureTask<Void> task = new FutureTask<>(() ->
                {
                connection.closeInternal(fNotify, error, 0L);
                return null;
                });
            f_closes.add(task);
            if (fWait)
                {
                try
                    {
                    task.get(10, TimeUnit.SECONDS);
                    }
                catch (Exception e)
                    {
                    throw new AssertionError(e);
                    }
                }
            }

        void drainClose() throws Exception
            {
            assertSame(f_owner, Thread.currentThread());
            FutureTask<Void> task = f_closes.poll(10, TimeUnit.SECONDS);
            assertNotNull("worker did not route its deferred close to the owner", task);
            task.run();
            task.get(10, TimeUnit.SECONDS);
            }

        @Override
        public void onConnectionClosed(Connection connection)
            {
            assertSame(f_owner, Thread.currentThread());
            super.onConnectionClosed(connection);
            }

        @Override
        public void onConnectionError(Connection connection, Throwable error)
            {
            assertSame(f_owner, Thread.currentThread());
            super.onConnectionError(connection, error);
            }

        final Thread                              f_owner         = Thread.currentThread();
        final BlockingQueue<FutureTask<Void>>      f_closes        = new LinkedBlockingQueue<>();
        final AtomicInteger                       f_cRoutedCloses = new AtomicInteger();
        }
    }
