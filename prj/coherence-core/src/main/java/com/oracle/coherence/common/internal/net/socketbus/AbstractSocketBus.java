/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.internal.net.socketbus;

import com.oracle.coherence.common.base.Blocking;
import com.oracle.coherence.common.base.Collector;
import com.oracle.coherence.common.base.Continuation;
import com.oracle.coherence.common.base.Disposable;
import com.oracle.coherence.common.collections.Arrays;
import com.oracle.coherence.common.internal.continuations.AbstractContinuationFrame;
import com.oracle.coherence.common.internal.continuations.Continuations;
import com.oracle.coherence.common.internal.continuations.WrapperContinuation;
import com.oracle.coherence.common.net.SafeSelectionHandler;
import com.oracle.coherence.common.net.SelectionService;
import com.oracle.coherence.common.net.Sockets;
import com.oracle.coherence.common.net.exabus.Bus;
import com.oracle.coherence.common.net.exabus.EndPoint;
import com.oracle.coherence.common.net.exabus.Event;
import com.oracle.coherence.common.net.exabus.util.SimpleEvent;
import com.oracle.coherence.common.net.exabus.util.UrlEndPoint;

import java.io.IOException;

import java.net.*;

import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import java.util.*;

import com.oracle.coherence.common.util.SafeClock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.Lock;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.net.ssl.SSLException;

import java.util.zip.CRC32;


/**
 * AbstractSocketBus provides a common base class for Socket based Bus
 * implementations.
 * <p>
 * This abstract implementation handles the underlying connection management.
 *
 * @author mf  2010.11.2
 */
public abstract class AbstractSocketBus
        implements Bus
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Create an AbstractSocketBus around a ServerSocketChannel
     *
     * @param driver      the SocketDriver used to produce the bus sockets
     * @param pointLocal  the local EndPoint
     *
     * @throws IOException if an I/O error occurs
     */
    public AbstractSocketBus(SocketBusDriver driver, UrlEndPoint pointLocal)
            throws IOException
        {
        f_driver = driver;

        String sProtocol = getProtocolName();
        if (!sProtocol.equals(pointLocal.getProtocol()))
            {
            throw new IllegalArgumentException("unsupported protocol: " +
                    pointLocal.getProtocol());
            }

        ServerSocketChannel chan = driver.getDependencies().getSocketProvider()
                .openServerSocketChannel();
        Sockets.configureBlocking(chan, false);
        configureSocket(chan.socket());

        chan.socket().bind(pointLocal.getAddress());

        f_nDropRatio       = driver.getDependencies().getDropRatio();
        f_nCorruptionRatio = driver.getDependencies().getCorruptionRatio();
        f_fCrc             = driver.getDependencies().isCrcEnabled();
        f_channelServer    = chan;

        // as the supplied endpoint may have been wildcard and/or ephemeral,
        // re-resolve based on what we actually bound to
        m_pointLocal = driver.resolveBindPoint(pointLocal, chan.socket());
        }


    // ----- Bus interface --------------------------------------------------

    /**
     * {@inheritDoc}
     */
    public EndPoint getLocalEndPoint()
        {
        return m_pointLocal;
        }

    /**
     * {@inheritDoc}
     */
    public void open()
        {
        Lock lock = f_lockState.writeLock();
        lock.lock();
        boolean fInterrupted = isInterrupted();
        try
            {
            verifyState(BusState.INITIAL);
            m_nState = BusState.OPEN;

            onOpen();

            try
                {
                ServerSocketChannel channel = f_channelServer;
                getSelectionService().register(f_channelServer, new AcceptHandler(channel));
                }
            catch (IOException e)
                {
                // We can't just close the bus, there is no concept of an unsolicited close, we are still bound to
                // the port, just leave it be.
                getLogger().log(makeExceptionRecord(Level.SEVERE, e,
                        "{0} ServerSocket failure; no new connection will be accepted", getLocalEndPoint()));
                }
            }
        finally
            {
            lock.unlock();
            if (fInterrupted)
                {
                Thread.currentThread().interrupt();
                }
            }
        }

    /**
     * {@inheritDoc}
     */
    public void close()
        {
        Lock lock = f_lockState.writeLock();
        lock.lock();
        boolean fInterrupted = isInterrupted();
        try
            {
            if (m_nState.ordinal() >= BusState.CLOSING.ordinal())
                {
                return; // close is idempotent
                }
            verifyState(BusState.OPEN);

            // stop allowing new connections
            m_nState = BusState.CLOSING;

            Map<EndPoint, Connection> mapConn = f_mapConnections;

            // identify how many things we have to "close" before we can put out the
            // CLOSE event
            final AtomicInteger cConnections = new AtomicInteger(mapConn.size() + 1);

            // close the server socket
            getSelectionService().invoke(f_channelServer,new Runnable()
                {
                public void run()
                    {
                    try
                        {
                        f_channelServer.close();
                        }
                    catch (IOException e) {}

                    if (cConnections.decrementAndGet() == 0)
                        {
                        onClose();
                        }
                    }
                }, /*cMillisDelay*/ 0);

            // release all open connections
            for (Connection conn : mapConn.values())
                {
                synchronized (conn)
                    {
                    conn.scheduleShutdown(null, /*fRelease*/ true, new Continuation<Void>()
                        {
                        @Override
                        public void proceed(Void v)
                            {
                            if (cConnections.decrementAndGet() == 0)
                                {
                                onClose();
                                }
                            }
                        });
                    }
                    }
                }
        catch (IOException e)
            {
            throw new IllegalStateException(e);
            }
        finally
            {
            lock.unlock();
            if (fInterrupted)
                {
                Thread.currentThread().interrupt();
                }
            }
        }

    /**
     * {@inheritDoc}
     */
    public void connect(EndPoint peer)
        {
        Lock lock = f_lockState.readLock();
        lock.lock();
        boolean fInterrupted = isInterrupted();
        try
            {
            verifyState(BusState.OPEN);

            if (getLocalEndPoint().equals(peer))
                {
                // Support for self-connect if desired needs to be added in
                // derived classes, there is no reason to push those bits through
                // the network stack
                throw new IllegalArgumentException("SocketBus does not support connections to self");
                }

            Connection conn = makeConnection(verifyEndPoint(peer));
            synchronized (conn)
                {
                if (f_mapConnections.putIfAbsent(peer, conn) == null)
                    {
                    conn.connect();
                    }
                else
                    {
                    // there is already an existing Connection for this peer
                    // and it has not reached the RELEASE state in a visible way
                    // so we can't replace it.  This is not an error, it is the
                    // equivalent, of a double connect, i.e. a no-op
                    conn.dispose();
                    }
                }
            }
        finally
            {
            lock.unlock();
            if (fInterrupted)
                {
                Thread.currentThread().interrupt();
                }
            }
        }

    /**
     * {@inheritDoc}
     */
    public void disconnect(EndPoint peer)
        {
        Connection conn = ensureConnection(peer);
        synchronized (conn)
            {
            conn.ensureValid().scheduleDisconnect(null);
            }
        }

    /**
     * {@inheritDoc}
     */
    public void release(EndPoint peer)
        {
        Connection conn = ensureConnection(peer);
        synchronized (conn)
            {
            conn.ensureValid().scheduleShutdown(null, /*fRelease*/ true, /*continuation*/ null);
            }
        }

    @Override
    public String toString(EndPoint peer)
        {
        try
            {
            return ensureConnection(peer).toString();
            }
        catch (Throwable e)
            {
            return "unknown peer " + peer;
            }
        }

    /**
     * {@inheritDoc}
     */
    public void flush()
        {
        flush(/*fSocketWrite*/true);
        }

    /**
     * {@inheritDoc}
     */
    public void flush(boolean fSocketWrite)
        {
        BusState nState = m_nState;
        if (nState != BusState.OPEN && nState != BusState.CLOSING)
            {
            throw new IllegalStateException("invalid bus state: " + nState);
            }

        boolean fInterrupted = isInterrupted();
        try
            {
            // flush the connections in random order, this helps prevent one heavily used connection
            // from starving others if it happens to exist earlier in the unordered set.
            Connection[] aConn = f_setFlush.toArray(EMPTY_CONNECTION_ARRAY);
            Arrays.shuffle(aConn);

            for (Connection conn : aConn)
                {
                conn.optimisticFlush(fSocketWrite); // it is up to the connection to remove itself from the flush set
                }
            }
        finally
            {
            if (fInterrupted)
                {
                Thread.currentThread().interrupt();
                }
            }
        }

    /**
     * {@inheritDoc}
     */
    public void setEventCollector(Collector<Event> collector)
        {
        Lock lock = f_lockState.readLock();
        lock.lock();
        try
            {
            verifyState(BusState.INITIAL);
            m_collectorEvent = collector;
            }
        finally
            {
            lock.unlock();
            }
        }

    /**
     * {@inheritDoc}
     */
    public Collector<Event> getEventCollector()
        {
        return m_collectorEvent;
        }



    // ----- AbstractSocketBus interface ------------------------------------

    /**
     * Schedule a task for future execution.
     *
     * @param proc     the task to run
     * @param cMillis  the delay before execution, or 0 for immediate
     *
     * @throws IllegalStateException if the Bus is not open
     */
    protected void scheduleTask(final Runnable proc, long cMillis)
        {
        scheduleTask(f_channelServer, proc, cMillis);
        }

    /**
     * Schedule a task for future execution.  Unlike scheduleTask, this variant will run the task even
     * if the bus has been closed
     *
     * @param proc     the task to run
     * @param cMillis  the delay before execution, or 0 for immediate
     *
     * @throws IllegalStateException if the Bus is not open
     */
    protected void scheduleUnsafeTask(final Runnable proc, long cMillis)
        {
        scheduleUnsafeTask(f_channelServer, proc, cMillis);
        }

    /**
     * Schedule a task for future execution.
     *
     * @param chan     the channel that the task is associated with
     * @param proc     the task to run
     * @param cMillis  the delay before execution, or 0 for immediate
     *
     * @throws IllegalStateException if the Bus is not open
     */
    protected void scheduleTask(final SelectableChannel chan, final Runnable proc, long cMillis)
        {
        BusState nState = m_nState;
        if (nState != BusState.OPEN && nState != BusState.CLOSING)
            {
            throw new IllegalStateException("invalid bus state: " + nState);
            }

        scheduleUnsafeTask(chan, new Runnable()
            {
            @Override
            public void run()
                {
                if (m_nState != BusState.CLOSED)
                    {
                    proc.run();
                    }
                }
            }, cMillis);
        }

    /**
     * Schedule a task for future execution.  Unlike scheduleTask, this variant will run the task even
     * if the bus has been closed
     *
     * @param chan     the channel that the task is associated with
     * @param proc     the task to run
     * @param cMillis  the delay before execution, or 0 for immediate
     *
     * @throws IllegalStateException if the Bus is not open
     */
    protected void scheduleUnsafeTask(final SelectableChannel chan, final Runnable proc, long cMillis)
        {
        try
            {
            getSocketDriver().getDependencies().getSelectionService().invoke(chan,
                proc, cMillis);
            }
        catch (IOException e)
            {
            throw new IllegalStateException(e);
            }
        }

    /**
     * Aggressively tear stop the bus instance.
     * <p>
     * The intent of this method is not to properly shutdown (close) the bus, but stop a bus which
     * has entered an "unrecoverable" state.  Specifically we are trying to protect our peers not us.
     */
    protected void halt()
        {
        try
            {
            f_channelServer.close();
            }
        catch (Exception e) {}

        for (Connection conn : getConnections())
            {
            try
                {
                conn.m_channel.close();
                }
            catch (Exception e) {}
            }
        }

    /**
     * Determine if a connection drop should be simulated
     *
     * @return true if the connection has been dropped
     */
    private boolean checkDrop(SocketChannel channel)
        {
        if (f_nDropRatio != 0 && ThreadLocalRandom.current().nextInt(Math.abs(f_nDropRatio)) == 0)
            {
            closeChannel(channel);
            return true;
            }
        return false;
        }

    /**
     * Determine if a data corruption should be simulated, if so force it.
     */
    private void checkForceCorruption(ByteBuffer buffer, int cb)
        {
        Random random = ThreadLocalRandom.current();

        if (f_nCorruptionRatio != 0 && random.nextInt(Math.abs(f_nCorruptionRatio)) == 0)
            {
            int nCorrupt = buffer.position() - (random.nextInt(cb) + 1);

            buffer.put(nCorrupt, (byte) random.nextInt());
            }
        }

    /**
     * Return the first buffer that has bytes remaing, or null if none match.
     *
     * @return the first buffer that has bytes remaing, or null if none match.
     */
    private ByteBuffer getFirstAvailableForCorruption(ByteBuffer[] aBuffer, int of)
        {
        int c = aBuffer.length;

        for (; of < c && !aBuffer[of].hasRemaining(); ++of)
            {}

        return of < c ? aBuffer[of] : null;
        }

    /**
     * For debugging purposes only.
     *
     * Close the underlying TCP socket associated with the specified peer.
     * This allows for testing of connection reestablishment.
     *
     * @param sPeerName  the peer to act on, or null for all
     * @param nClose     the type of close to perform, see CLOSE_SOCKET*
     */
    public void sever(String sPeerName, int nClose)
        {
        Connection[] aConn;

        if (sPeerName == null)
            {
            aConn = f_mapConnections.values().stream().toArray(Connection[]::new);
            }
        else
            {
            aConn = new Connection[] {ensureConnection(f_driver.getDepot().resolveEndPoint(sPeerName))};
            }

        for (Connection conn : aConn)
        try
            {
            switch (nClose)
                {
                case SOCKET_SHUTDOWN_INPUT:
                    conn.m_channel.shutdownInput();
                    break;

                case SOCKET_SHUTDOWN_OUTPUT:
                    conn.m_channel.shutdownOutput();
                    break;

                case SOCKET_SHUTDOWN_INPUT_OUTPUT:
                    conn.m_channel.shutdownInput();
                    conn.m_channel.shutdownOutput();
                    break;

                case SOCKET_DROP_OUTPUT:
                    conn.m_fDropOutput = true;
                    // Note, to safely set this back to false we'd need to ensure we could do it in such a
                    // way that we close the current channel, then set to false, then migrate, otherwise
                    // some random bytes could make it on the wire
                    break;

                case CLOSE_SOCKET:
                default:
                    conn.m_channel.close();
                    break;
                }
            }
        catch (IOException e) {}
        }

    public static final int CLOSE_SOCKET = 0;
    public static final int SOCKET_SHUTDOWN_INPUT = 1;
    public static final int SOCKET_SHUTDOWN_OUTPUT = 2;
    public static final int SOCKET_SHUTDOWN_INPUT_OUTPUT = 3;
    public static final int SOCKET_DROP_OUTPUT = 4;

    /**
     * Close the specified channel
     *
     * @param chan  the channel to close
     */
    protected static void closeChannel(SelectableChannel chan)
        {
        try
            {
            chan.close();
            }
        catch (IOException e) {}
        }

    /**
     * Called once a bus has been opened.
     *
     * AbstractSocketBus.onOpen will emit the OPEN event and must be called.
     */
    protected void onOpen()
        {
        final long cMillisHeartbeat = f_driver.getDependencies().getHeartbeatMillis();
        if (cMillisHeartbeat > 0)
            {
            scheduleTask(new Runnable()
                {
                @Override
                public void run()
                    {
                    // reschedule self for next periodic heartbeat
                    scheduleTask(this, cMillisHeartbeat);

                    getConnections().forEach(Connection::heartbeat);
                    }
                }, cMillisHeartbeat);
            }

        final long cMillisAckTimeout      = f_driver.getDependencies().getAckTimeoutMillis();
        final long cMillisAckFatalTimeout = f_driver.getDependencies().getAckFatalTimeoutMillis();
        if (cMillisAckTimeout > 0 || cMillisAckFatalTimeout > 0)
            {
            long cIntervalMillis = Math.max(100,
                    (cMillisAckTimeout == 0
                            ? cMillisAckFatalTimeout
                            : cMillisAckFatalTimeout == 0
                                ? cMillisAckTimeout
                                : Math.min(cMillisAckTimeout, cMillisAckFatalTimeout)) / 20);

            scheduleTask(new Runnable()
                {
                @Override
                public void run()
                    {
                    // reschedule self for next periodic health check
                    scheduleTask(this, cIntervalMillis);

                    long ldtNow = SafeClock.INSTANCE.getSafeTimeMillis();

                    getRegisteredConnections().forEach(conn -> conn.checkHealth(ldtNow));
                    }
                }, cIntervalMillis);
            }

        EndPoint epThis = getLocalEndPoint();
        getLogger().log(makeRecord(Level.FINER, "{0} opened using {1}",
                epThis, f_channelServer.socket()));

        emitEvent(new SimpleEvent(Event.Type.OPEN, epThis));
        }

    /**
     * Called as part of the closing the bus.
     *
     * AbstractSocketBus.onClose will emit the CLOSE event and must be called
     */
    protected void onClose()
        {
        EndPoint epThis = getLocalEndPoint();
        getLogger().log(makeRecord(Level.FINER, "{0} closed using {1}",
                epThis, f_channelServer.socket()));

        m_nState = BusState.CLOSED;
        emitEvent(new SimpleEvent(Event.Type.CLOSE, epThis));
        }

    /**
     * Return a collection containing all the currently registered connections.
     *
     * @return the current connections
     */
    protected Collection<Connection> getRegisteredConnections()
        {
        return f_mapConnections.values();
        }

    protected String getDescription()
        {
        return getDescription(true);
        }

    /**
     * Returns a description of this SocketBus.
     *
     * @param fVerbose if true, generate a description with connections details
     *
     * @return a String description of this SocketBus.
     */
    protected String getDescription(boolean fVerbose)
        {
        StringBuilder sb = new StringBuilder()
                .append(getLocalEndPoint())
                .append(", state=").append(m_nState);

        ConcurrentMap<EndPoint, Connection> mapCon = f_mapConnections;
        int cConnections = 0;
        int cActive      = 0;

        for (Connection conn : mapCon.values())
            {
            ++cConnections;
            if (conn.m_state == ConnectionState.ACTIVE)
                {
                ++cActive;
                }
            }

        sb.append(", connections ");
        if (fVerbose)
            {
            int cCon = mapCon.size();
            sb.append("[");
            for (Connection conn : mapCon.values())
                {
                sb.append("\n\t").append(conn);
                }
            sb.append("]\n");
            }
        sb.append("active=").append(cActive).append('/').append(cConnections);

        return sb.toString();
        }

    /**
     * Returns a string representation of this SocketBus. If called in
     * verbose mode, include connections details.
     *
     * @param fVerbose  if true then print connections details
     *
     * @return a String representation of this SocketBus
     */
    public String toString(boolean fVerbose)
        {
        return new StringBuilder()
                .append(getClass().getSimpleName()).append('(')
                .append(getDescription(fVerbose))
                .append(')').toString();
        }

    // ----- Object interface -----------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
        {
        return toString(true);
        }


    // ----- AbstractSocketBus helpers --------------------------------------

    /**
     * Return true if the calling thread is currently interrupted, and clear that state.
     *
     * @return true if the calling thread is currently interrupted.
     */
    protected boolean isInterrupted()
        {
        return Blocking.interrupted();
        }

    /**
     * Return the bus's logger.
     *
     * @return the bus's logger
     */
    protected Logger getLogger()
        {
        return getSocketDriver().getDependencies().getLogger();
        }

    /**
     * Construct a log record.
     *
     * @param level     the log level
     * @param sMsg      the message
     * @param oaParams  the parameters
     *
     * @return the record
     */
    protected LogRecord makeRecord(Level level, String sMsg, Object ... oaParams)
        {
        LogRecord rec = new LogRecord(level, sMsg);
        rec.setParameters(oaParams);
        return rec;
        }

    /**
     * Construct a log record with an exception.
     *
     * @param level     the log level
     * @param t         the exception
     * @param sMsg      the message
     * @param oaParams  the parameters
     *
     * @return the record
     */
    protected LogRecord makeExceptionRecord(Level level, Throwable t, String sMsg, Object ... oaParams)
        {
        LogRecord rec = makeRecord(level, sMsg, oaParams);
        rec.setThrown(t);
        return rec;
        }

    /**
     * Add a Connection to the set of connections awaiting a flush.
     *
     * @param conn the connection to add
     */
    protected void addFlushable(Connection conn)
        {
        f_setFlush.add(conn);
        }

    /**
     * Remove a Connection from the flushable set.
     *
     * @param conn  the connection to remove
     */
    protected void removeFlushable(Connection conn)
        {
        f_setFlush.remove(conn);
        }

    /**
     * Return true iff the specified connection is currently in the flushable set.
     *
     * @param conn the connection
     *
     * @return true iff the connection is in the flushable set
     */
    protected boolean isFlushable(Connection conn)
        {
        return f_setFlush.contains(conn);
        }

    /**
     * Emit the specified Event to the Event Collector.
     *
     * @param event  the event to emit
     */
    protected void emitEvent(Event event)
        {
        Collector<Event> coll = m_collectorEvent;
        if (coll != null)
            {
            try
                {
                coll.add(event);
                coll.flush();
                }
            catch (Throwable t)
                {
                // TODO: disconnect?, log?
                }
            }
        }

    /**
     * Add the specified Event to the Event Collector.
     * <p>
     * This method does not flush the collector.
     *
     * @param event  the event to add
     */
    protected void addEvent(Event event)
        {
        Collector<Event> coll = m_collectorEvent;
        if (coll != null)
            {
            try
                {
                coll.add(event);
                }
            catch (Throwable t)
                {
                // TODO: disconnect?, log?
                }
            }
        }

    /**
     * Flush the event collector.
     */
    protected void flushEvents()
        {
        Collector<Event> coll = m_collectorEvent;
        if (coll != null)
            {
            try
                {
                coll.flush();
                }
            catch (Throwable t)
                {
                // TODO: disconnect?, log?
                }
            }
        }

    /**
     * Return the currently managed connections.
     *
     * @return the currently managed connections
     */
    protected Collection<Connection> getConnections()
        {
        return f_mapConnections.values();
        }

    /**
     * Configure the specified Socket
     *
     * @param socket  the socket to configure
     *
     * @throws IOException on an I/O error
     */
    protected void configureSocket(Socket socket)
        throws IOException
        {
        Sockets.configure(socket, getSocketDriver().getDependencies().getSocketOptions());
        }

    /**
     * Configure the specified ServerSocket
     *
     * @param socket  the ServerSocket to configure
     *
     * @throws IOException on an I/O error
     */
    protected void configureSocket(ServerSocket socket)
        throws IOException
        {
        Sockets.configure(socket, getSocketDriver().getDependencies().getSocketOptions());
        }

    /**
     * Verify that the bus is in a given state.
     *
     * @param nState the requite state
     *
     * @throws IllegalStateException if the bus is not in the specified state
     */
    protected void verifyState(BusState nState)
        {
        BusState nStateCurr = m_nState;
        if (nStateCurr != nState)
            {
            throw new IllegalStateException("invalid bus state: required " + nState
                    + ", actual " + nStateCurr);
            }
        }

    /**
     * Verify that the supplied EndPoint is a usable EndPoint
     *
     * @param peer  the EndPoint to verify
     *
     * @return  the UrlEndPoint
     */
    protected UrlEndPoint verifyEndPoint(EndPoint peer)
        {
        if (f_driver.isSupported(peer) && ((UrlEndPoint) peer).getProtocol().equals(getProtocolName()))
            {
            return (UrlEndPoint) peer;
            }
        throw new IllegalArgumentException("unsupported EndPoint " + peer);
        }

    /**
     * Return the SelectionService for this Bus.
     *
     * @return the SelectionService for this Bus.
     */
    protected SelectionService getSelectionService()
        {
        return getSocketDriver().getDependencies().getSelectionService();
        }

    /**
     * Return the SocketDriver for this bus.
     *
     * @return the SocketDriver
     */
    protected SocketBusDriver getSocketDriver()
        {
        return f_driver;
        }

    /**
     * Return the bus connection for the specified EndPoint.
     *
     * @param peer  the EndPoint to ensure
     *
     * @return the Connection
     *
     * @throws IllegalStateException if the bus is not open
     * @throws IllegalArgumentException if the connection does not exist
     */
    protected Connection ensureConnection(EndPoint peer)
        {
        Connection conn = f_mapConnections.get(peer);
        if (conn == null)
            {
            verifyState(BusState.OPEN);
            throw new IllegalArgumentException("unknown peer " + peer);
            }
        return conn;
        }

    /**
     * Return the protocol identifier.
     *
     * @return the protocol identifier
     */
    protected int getProtocolIdentifier()
        {
        return getClass().getName().hashCode() ^
               getSocketDriver().getClass().getName().hashCode();
        }

    /**
     * Return the protocol name.
     *
     * @return the protocol name
     */
    protected String getProtocolName()
        {
        return getClass().getSimpleName();
        }

    /**
     * Return minimum protocol version understood by this implementation.
     *
     * @return the minimum protocol version
     */
    protected short getMinimumProtocolVersion()
        {
        return 0;
        }

    /**
     * Return maximum protocol version understood by this implementation.
     *
     * @return the maximum protocol version
     */
    protected short getMaximumProtocolVersion()
        {
        // version 1 adds reconnect support
        // version 2 adds identity
        // version 3 adds ability to request a remote heap dump on sync
        // version 4 sends local and remote ID for CONNECT_MIGRATE, and pads an ID spot for CONNECT_NEW but always sends 0
        // version 5 change the message size to long, add checksum for message body and message header
        return 5;
        }

    // ----- ConnectionState ------------------------------------------------

    /**
     * ConnectionState represents the state of the underlying Connection.
     */
    protected enum ConnectionState
        {
        /**
         * CONNECT event emitted but socket has yet to connect (may not even exist)
         */
        OPEN,

        /**
         * Indicates a usable connection, we can exchange messages.
         */
        ACTIVE,

        /**
         * DISCONNECT event emitted, no more exchanges allowed, SelectionService
         * is no longer managing the channel.
         */
        DEFUNCT,

        /**
         * RELEASE event emitted, all done.
         */
        FINAL
        }

    // ----- HandshakePhase --------------------------------------------------

    /**
     * HandshakePhase represents the state of the handshake protocol.
     */
    protected enum HandshakePhase
        {
        /**
         * The negotiation phase of the handshake identifies the basic
         * protocol, ensuring that before reading any further into the stream
         * that the other end speaks the same language, i.e. we've connected
         * to another SocketBus.
         * <p>
         * The format is:
         * <ul>
         * <li>4B (int)    protocol identifier</li>
         * <li>2B (short)  min version</li>
         * <li>2B (short)  max version</li>
         * <li>2B (short)  name char length</li>
         * <li>?B (char[]) name</li>
         * </ul>
         */
        NEGOTIATE,

        /**
         * The introduction phase waits for the canonical name of the peer.
         * The length of which was obtained in the identification phase.
         */
        INTRODUCE,

        /**
         * The accept phase involves each peer sending a single (otherwise
         * useless) byte to accept the connection.  Until this byte has been
         * received the connection has not been accepted, and additional data
         * should not be sent.
         */
        ACCEPT,

        /**
         * The abandon phase is entered only if the bus decides to not
         * pursue a connection with the peer. Once all IO required for this
         * phase is completed the channel will be closed.
         *
         * This can happen because it already is the process of opening
         * the same, and it is of higher priority. Rather then rejecting the
         * connection by closing it, it simply waits for the peer, to also
         * realize the collision, and to accept the higher priority EndPoint's
         * connection. At this point it will close its connection, allowing the
         * higher priority peer to do the same. If we were to actively reject
         * the connection, the peer could see this before realizing the collision
         * and emit a DISCONNECT event which is not what we want.
         *
         * This can also happen as a result of a peer trying to reconnect a
         * connection which is unknown to this bus.
         */
        ABANDON
        }


    // ----- Connection -----------------------------------------------------

    /**
     * Factory pattern method for instantiating Connection objects.
     *
     * @param peer     the peer
     *
     * @return the Connection
     */
    protected abstract Connection makeConnection(UrlEndPoint peer);

    /**
     * Connection contains the state associated with the connection to each
     * connected peer.
     */
    protected abstract class Connection
            implements SelectionService.Handler, GatheringByteChannel,
                       ScatteringByteChannel, Disposable
        {
        // ----- constructors -------------------------------------------

        /**
         * Construct a Connection for the specified EndPoint.
         *
         * @param peer  the remote end of the connection
         */
        public Connection(UrlEndPoint peer)
            {
            this.f_peer = peer;
            if (f_fCrc)
                {
                f_crcRx = new CRC32();
                f_crcTx = new CRC32();
                getLogger().log(makeRecord(Level.FINER, "Packet corruption detection enabled for connection {0} to {1}",
                        getLocalEndPoint(), peer));
                }
            }


        // ----- Connection interface -----------------------------------

        /**
         * Open the connection.
         *
         * @throws IOException if an IO error occurs
         */
        protected void open()
                throws IOException
            {
            if (m_state != null)
                {
                throw new IllegalStateException("state = " + m_state);
                }

            m_state = ConnectionState.OPEN;

            EndPoint peer = f_peer;
            getLogger().log(makeRecord(Level.FINER, "{0} opening connection with {1} using {2}", getLocalEndPoint(), peer,
                    m_channel.socket()));
            emitEvent(new SimpleEvent(Event.Type.CONNECT, peer));
            }

        /**
         * Return if the connection is valid
         *
         * @return true iff the connection is valid
         */
        protected boolean isValid()
            {
            ConnectionState state = m_state;
            if (state == null)
                {
                // we can get here for LightMessageBus if there is a concurrent connect and the remote side
                // wins; since LWMB doesn't synchronized in send we have to double check before declaring
                // a failure before open; simply making m_state volatile would not be sufficient because when
                // the connection comes from the remote side it is inserted into the map before it is opened
                // but is inserted while under sync. It still must be volatile though for the double-check
                // to be safe
                synchronized (this)
                    {
                    state = m_state;
                    if (state == null)
                        {
                        // ok, this really is an invalid connection
                        return false;
                        }
                    // else; now we have a stable view of the state
                    }
                }

            return state != ConnectionState.FINAL;
            }

        /**
         * Ensure that the connection is usable.
         *
         * @return this
         */
        public Connection ensureValid()
            {
            if (isValid())
                {
                return this;
                }

            throw new IllegalArgumentException("connection to " + f_peer + " is not open, in state " + m_state);
            }

        /**
         * Perform the actual connect to the peer
         *
         * @throws IOException on an I/O error
         */
        private void connect()
            {
            ConnectionState state = m_state;
            if (state != null && state.ordinal() >= ConnectionState.DEFUNCT.ordinal())
                {
                throw new IllegalStateException("state = " + m_state);
                }
            else if (m_channel != null && m_channel.isOpen())
                {
                // reconnect of open channel
                throw new IllegalStateException();
                }

            SocketChannel channel;
            try
                {
                m_channel = channel = getSocketDriver().getDependencies()
                        .getSocketProvider().openSocketChannel();
                }
            catch (IOException e)
                {
                throw new RuntimeException(e);
                }

            // configure and connect
            try
                {
                Sockets.configureBlocking(channel, false);
                configureSocket(channel.socket());
                channel.connect(f_peer.getAddress());
                }
            catch (IOException e)
                {
                onException(e);
                }

            try
                {
                // emit connect event; allowing operations to be scheduled against the endpoint
                if (state == null)
                    {
                    open();
                    }
                // else this is a re-connect and open has already been issued

                // register initial I/O handler
                SelectionService.Handler handler = new HandshakeHandler(channel, this);
                getSelectionService().register(channel, handler);
                m_handler = handler;
                }
            catch (IOException e)
                {
                scheduleDisconnect(e);
                }
            }

        /**
         * Schedule a disconnect.
         *
         * @param eReason  the reason for the disconnect
         */
        public void scheduleDisconnect(Throwable eReason)
            {
            scheduleShutdown(eReason, false, /*continuation*/ null);
            }

        /**
         * Register a Runnable with the SelectionService
         * to perform the disconnect logic and optionally release it. It schedules
         * the disconnect to happen on the SelectionService thread where the
         * connection is registered.
         *
         * @param eReason       the reason for the disconnect or null
         * @param fRelease      true iff the connection should also be released
         * @param continuation  continuation once the operation has completed
         */
        public void scheduleShutdown(final Throwable eReason,
                 final boolean fRelease, Continuation<Void> continuation)
            {
            invoke(new AbstractContinuationFrame<Void>(continuation)
                {
                @Override
                public Void call()
                    {
                    boolean          fEmitDisconnect = false;
                    HandshakeHandler handlerNext;

                    synchronized (Connection.this)
                        {
                        handlerNext = m_next;
                        switch (m_state)
                            {
                            case OPEN:
                            case ACTIVE:
                                fEmitDisconnect = true; // emit below; outside of sync block
                                m_state         = ConnectionState.DEFUNCT;
                                //fall through

                            case DEFUNCT:
                                if (fRelease)
                                    {
                                    m_next  = null;
                                    m_state = ConnectionState.FINAL;
                                    }
                                break;

                            case FINAL:
                                // this can occur if there were pending
                                // jobs when we processed the release
                                // job
                                return null;

                            default:
                                throw new IllegalStateException("state = " + m_state);
                            }
                        }

                    final Continuation<? super Void> continuation = new WrapperContinuation<Void>(getContinuation())
                        {
                        @Override
                        public void proceed(Void v)
                            {
                            flushEvents();
                            super.proceed(v);
                            }
                        };

                    if (fRelease)
                        {
                        if (fEmitDisconnect)
                            {
                            final HandshakeHandler handlerRelease = handlerNext;
                            doDisconnect(fEmitDisconnect, eReason, new Continuation<Void>()
                                {
                                @Override
                                public void proceed(Void v)
                                    {
                                    doRelease(handlerRelease, continuation);
                                    }
                                });
                            }
                        else
                            {
                            doRelease(handlerNext, continuation);
                            }
                        }
                    else
                        {
                        doDisconnect(fEmitDisconnect, eReason, continuation);
                        }

                    return continueAsync();
                    }
                });
            }

        /**
         * Emit any and all receipts
         */
        protected abstract void drainReceipts();

        /**
         * Perform disconnect processing.
         * <p>
         * This may be called multiple times.
         *
         * @param fFirst        true iff this is the first disconnect
         * @param eReason       the reason (if any) for the disconnect
         * @param continuation  the continuation to run after the disconnect has been performed
         */
        protected void doDisconnect(boolean fFirst, Throwable eReason, Continuation<? super Void> continuation)
            {
            try
                {
                if (fFirst)
                    {
                    onDisconnected(eReason);
                    }

                try
                    {
                    m_channel.close(); // may already be closed
                    }
                catch (IOException e) {}


                // in case of multiple disconnects force drain of receipts on each pass
                // while not required this helps from leaving receipts queue'd until
                // release
                drainReceipts();
                }
            finally
                {
                Continuations.proceed(continuation, null);
                }
            }

        /**
         * Perform release processing.
         *
         * @param handlerNext   the handler for the next connection
         * @param continuation  the continuation to run after the release has been performed
         */
        protected void doRelease(HandshakeHandler handlerNext, Continuation<? super Void> continuation)
            {
            try
                {
                EndPoint peer = getPeer();
                onReleased();

                // emit queue'd receipts
                drainReceipts();

                 // we take out a write lock to handle the cases where a new connection is concurrently
                 // made for the one we are actively releasing
                 Lock lockWrite = AbstractSocketBus.this.f_lockState.writeLock();
                 lockWrite.lock();
                 try
                    {
                    if (handlerNext != null && !handlerNext.getChannel().socket().isInputShutdown() &&
                        AbstractSocketBus.this.m_nState == BusState.OPEN)
                        {
                        // replace with pending connection
                        synchronized (handlerNext.m_connection)
                            {
                            // In order to ensure that the connection is visible once we emit the
                            // CONNECT event, the connection must be in the connection map before
                            // emitting the even.  Of course until the CONNECT event is emitted the
                            // connection isn't usable.  To make these two operations atomic we must
                            // sync on the new connection, put it in the map, and then emit the event.

                            // We also don't want to emit the RELEASE event for the old connection
                            // while it is still in the map, because once RELEASEd new connects are
                            // allowable.  The net result is that we must do a replace, RELEASE,
                            // open all while sync'd on the new connection
                            AbstractSocketBus.this.f_mapConnections.replace(peer, handlerNext.m_connection);
                            getLogger().log(makeRecord(Level.FINER, "{0} releasing connection with {1}",
                                    getLocalEndPoint(), peer));
                            addEvent(new SimpleEvent(Event.Type.RELEASE, peer));

                            try
                                {
                                handlerNext.m_connection.open(); // emits CONNECT event
                                getSelectionService().register(handlerNext.getChannel(), handlerNext);
                                handlerNext.m_connection.m_handler = handlerNext;
                                }
                            catch (IOException e)
                                {
                                handlerNext.m_connection.scheduleDisconnect(e);
                                }
                            }
                        handlerNext = null;
                        }
                    else
                        {
                        if (handlerNext != null)
                            {
                            handlerNext.close(null);
                            }

                        AbstractSocketBus.this.f_mapConnections.remove(peer);
                        getLogger().log(makeRecord(Level.FINER, "{0} releasing connection with {1}",
                                getLocalEndPoint(), peer));
                        addEvent(new SimpleEvent(Event.Type.RELEASE, peer));
                        }
                    }
                finally
                    {
                    lockWrite.unlock();
                    }

                if (handlerNext != null)
                    {
                    // we didn't proceed with the pending connection
                    try
                        {
                        handlerNext.getChannel().close();
                        }
                    catch (Exception e) {}
                    }
                }
            finally
                {
                Continuations.proceed(continuation, null);
                }
            }

        /**
         * Called as a connection is being disconnected.
         *
         * @param eReason  the cause of the disconnect
         */
        public void onDisconnected(Throwable eReason)
            {
            getLogger().log(makeExceptionRecord(
                    eReason instanceof SSLException ? Level.WARNING : Level.FINER, eReason,
                    "{0} disconnected connection with {1}",
                    getLocalEndPoint(), this));

            addEvent(new SimpleEvent(Event.Type.DISCONNECT, getPeer(), eReason));
            }

        /**
         * Called as a connection is being released.
         */
        public void onReleased()
            {
            removeFlushable(this);
            dispose();
            }

        /**
         * Called as part of migrating a connection.
         *
         * The caller must hold synchronization on the Connection while calling this method and
         * this method must be run on the SS thread associated with the connection.
         */
        public void onMigration()
            {
            ++m_cMigrations;
            }

        @Override
        public void dispose()
            {
            }

        /**
         * Flush the connection.
         */
        protected abstract void flush();

        /**
         * Flush the connection.
         *
         * @param fSocketWrite  true if the caller is willing to offer its cpu to perform a socket write
         */
        protected abstract void flush(boolean fSocketWrite);

        /**
         * Issue a heartbeat if necessary
         *
         * @return true if a heartbeat was issued, false if it was determined one wasn't needed
         */
        protected abstract boolean heartbeat();

        /**
         * Check the connection for any ack timeouts.
         *
         * @param ldtNow the current safe time
         */
        protected void checkHealth(long ldtNow)
            {
            }

        /**
         * Perform an optimistic flush, i.e. flush only if the connection is not already being flushed.
         *
         * Caller's need not be synchronized on the connection when calling this method, though the method
         * will synchronize if it determines that it will flush.
         */
        public void optimisticFlush()
            {
            optimisticFlush(/*fSocketWrite*/true);
            }

        /**
         * Perform an optimistic flush, i.e. flush only if the connection is not already being flushed.
         *
         * Caller's need not be synchronized on the connection when calling this method, though the method
         * will synchronize if it determines that it will flush.
         *
         * @param fSocketWrite  true if the caller is willing to offer its cpu to perform a socket write
         */
        public void optimisticFlush(boolean fSocketWrite)
            {
            AtomicBoolean lockFlush = f_lockFlush;

            if (lockFlush.compareAndSet(false, true))
                {
                synchronized (this) // wait for any concurrent send to complete
                    {
                    try
                        {
                        ensureValid().flush(fSocketWrite);
                        }
                    catch (IllegalArgumentException e)
                        {
                        // connection may have been released
                        }
                    finally
                        {
                        // we must unlock while still sync'd to ensure that no thread can add to the send queue
                        // while we hold the flush lock
                        lockFlush.set(false);
                        }
                    }
                }
            // else; another thread is actively flushing this connection. Because both conn.send and conn.flush
            // are performed while sync'd on the conn we know the other thread's flush will include all data on
            // the connection and thus this thread can skip over that connection.  The intent of this optimization
            // is to avoid blocking on flush when there will be nothing left to flush anyway.  This is especially
            // important as SocketBus scalability comes primarily from having many connections, with the idea
            // that threads are less likely to contend on any given connection.  Flushing however involves all
            // used threads and would become a high contention point, thus we need to avoid needlessly blocking
            // here.
            }

        /**
         * Return true if some thread is actively waiting to flush this connection.
         *
         * @return true if some thread is actively waiting to flush this connection
         */
        public final boolean isFlushInProgress()
            {
            return f_lockFlush.get();
            }

        /**
         * Force the SelectionService to process this channel.
         *
         * @throws IOException  if an I/O error occurs
         *
         * @return true iff the wakeup was scheduled
         *
         * @throws IOException if the connection has been closed
         */
        protected boolean wakeup()
                throws IOException
            {
            //COH-19338: m_state is null for new connection before open is called
            if (m_state == null)
                {
                return false;
                }

            switch (m_state)
                {
                case OPEN:
                    return false;

                case ACTIVE:
                    synchronized (Connection.this)
                        {
                        getSelectionService().register(m_channel, m_handler);
                        }
                    return true;

                case DEFUNCT:
                case FINAL:
                default:
                    throw new ClosedChannelException();
                }
            }

        /**
         * Return the send buffer size for the underlying socket.
         *
         * @return the send buffer size, or -1 if not connected
         *
         * @throws SocketException if an I/O error occurs
         */
        protected int getSendBufferSize()
                throws SocketException
            {
            SocketChannel chan = m_channel;
            if (chan == null)
                {
                return -1;
                }
            return chan.socket().getSendBufferSize();
            }

        /**
         * Return the packet size for this connection.
         *
         * @return the packet size for this connection, or -1 if not connected
         */
        protected int getPacketSize()
            {
            int cbPacket = m_cbPacket;
            if (cbPacket <= 0)
                {
                SocketChannel chan = m_channel;
                if (chan != null)
                    {
                    Socket socket = chan.socket();
                    if (socket.isBound())
                        {
                        m_cbPacket = cbPacket = Sockets.getMTU(socket);
                        }
                    }
                }

            return cbPacket;
            }

        /**
         * Return the receive buffer size for the underlying socket.
         *
         * @return the receive buffer size, or -1 if not connected
         *
         * @throws SocketException if an I/O error occurs
         */
        protected int getReceiveBufferSize()
                throws SocketException
            {
            SocketChannel chan = m_channel;
            if (chan == null)
                {
                return -1;
                }
            return chan.socket().getReceiveBufferSize();
            }

        /**
         * Return the peer associated with this connection.
         *
         * @return the peer
         */
        public EndPoint getPeer()
            {
            return f_peer;
            }

        /**
         * Return the protocol version for this connection.
         *
         * @return  the protocol version or -1 if not net negotiated
         */
        public int getProtocolVersion()
            {
            return m_nProtocol;
            }

        /**
         * Schedule an invocation against this channel on the SelectionService.
         *
         * @param runnable  the runnable to invoke
         */
        protected synchronized void invoke(Runnable runnable)
                {
                Queue<Runnable> queueDeferred = m_queueDeferred;
                if (queueDeferred == null)
                    {
                    try
                        {
                        getSelectionService().invoke(m_channel, runnable, /*cMillisDelay*/ 0);
                        }
                    catch (IOException e)
                        {
                        throw new RuntimeException(e);
                        }
                    }
                else
                    {
                    queueDeferred.add(runnable);
                    }
                }

        // ----- Channel interface ------------------------------------------

        /**
         * Register a new SelectionService.Handler for this connection.
         *
         * @param handler  the handler or null to unregister
         *
         * @throws IOException if an IO error occurs
         */
        public void registerHandler(SelectionService.Handler handler)
                throws IOException
            {
            getSelectionService().register(m_channel, handler);
            }

        /**
         * {@inheritDoc}
         */
        public void close()
            {
            scheduleDisconnect(null);
            }

        /**
         * Return true iff the connection has not been released.
         *
         * @return true iff the connection has not been released
         */
        public boolean isOpen()
            {
            return m_state.ordinal() < ConnectionState.FINAL.ordinal();
            }

        /**
         * {@inheritDoc}
         */
        public long write(ByteBuffer[] srcs, int offset, int length)
                throws IOException
            {
            switch (m_state)
                {
                case OPEN:
                    return 0; // not ready yet

                case ACTIVE:
                    {
                    SocketChannel chan    = m_channel;
                    long          cbWrite = 0;
                    long          cb;

                    long cbPre = 0;
                    for (int i = offset, e = offset + length; i < e; ++i)
                        {
                        cbPre += srcs[i].remaining();
                        }

                    if (m_fDropOutput) // for testing purposes only
                        {
                        for (int i = offset, e = offset + length; i < e; ++i)
                            {
                            srcs[i].position(srcs[i].limit());
                            }

                        m_cbWrite += cbPre;
                        return cbPre;
                        }

                    try
                        {
                        int i = offset;
                        int c = length;
                        do
                            {
                            cbWrite += cb = chan.write(srcs, i, c);

                            // According the JRockit team the underlying
                            // OS will generally only support a maximum number
                            // of gather buffers (they said 16). So it is
                            // possible that an incomplete write was not do to
                            // lack of buffer space but do to this OS issue.

                            // so if we wrote something, and have lots of buffers
                            // advance through the written ones and try again
                            while (cb != 0 && c > 0 && !srcs[i].hasRemaining())
                                {
                                ++i;
                                --c;
                                }
                            }
                        while (cb != 0 && c > 0);
                        }
                    catch (IOException ex)
                        {
                        // out of paranoia (chan.write doc isn't specific) test to see if any buffer positions were
                        // updated, we need cbWrite to be correct
                        long cbPost = 0;
                        for (int i = offset, e = offset + length; i < e; ++i)
                            {
                            cbPost += srcs[i].remaining();
                            }

                        cbWrite = cbPre - cbPost;
                        if (cbWrite == 0)
                            {
                            throw ex;
                            }

                        // apparently we managed to do some writes before the socket was closed.  We either need to
                        // rewind the buffers we wrote or pretend that we didn't see that the socket was closed.  The
                        // latter is far easier so we take that approach.  A subsequent write will of course fail without
                        // sending anything and then we'll surface the exception
                        }

                    m_cbWrite += cbWrite;

                    return cbWrite;
                    }

                default:
                    throw new ClosedChannelException();
                }
            }

        /**
         * {@inheritDoc}
         */
        public long write(ByteBuffer[] srcs)
                throws IOException
            {
            switch (m_state)
                {
                case OPEN:
                    return 0; // not ready yet

                case ACTIVE:
                    return write(srcs, 0, srcs.length);

                default:
                    throw new ClosedChannelException();
                }
            }

        /**
         * {@inheritDoc}
         */
        public int write(ByteBuffer src) throws IOException
            {
            switch (m_state)
                {
                case OPEN:
                    return 0; // not ready yet

                case ACTIVE:
                    int cb = m_channel.write(src);
                    m_cbWrite += cb;
                    return cb;

                default:
                    throw new ClosedChannelException();
                }
            }

        /**
         * {@inheritDoc}
         */
        public long read(ByteBuffer[] dsts, int offset, int length)
                throws IOException
            {
            switch (m_state)
                {
                case OPEN:
                    return 0; // not ready yet

                case ACTIVE:
                    // TODO: consider insulating against scatter/gather issue
                    // see write above

                    ByteBuffer bufFirstCorrupt = null;
                    int        cbFirst         = 0;

                    if (f_nCorruptionRatio != 0)
                        {
                        bufFirstCorrupt = getFirstAvailableForCorruption(dsts, offset);
                        cbFirst         = bufFirstCorrupt.remaining();
                        }

                    long cb = m_channel.read(dsts, offset, length);

                    if (cb >= 0)
                        {
                        if (f_nCorruptionRatio != 0)
                            {
                            checkForceCorruption(bufFirstCorrupt, (int) Math.min(cb, cbFirst));
                            }

                        if (f_nDropRatio == 0 || !checkDrop(m_channel))
                            {
                            m_cbRead += cb;
                            return cb;
                            }
                        }
                    // else; fall through

                default:
                    return -1;
                }
            }

        /**
         * {@inheritDoc}
         */
        public long read(ByteBuffer[] dsts) throws IOException
            {
            return read(dsts, 0, dsts.length);
            }

        /**
         * {@inheritDoc}
         */
        public int read(ByteBuffer dst) throws IOException
            {
            switch (m_state)
                {
                case OPEN:
                    return 0; // not ready yet

                case ACTIVE:
                    int cb = m_channel.read(dst);
                    if (cb >= 0)
                        {
                        if (f_fCrc && f_nCorruptionRatio != 0)
                            {
                            checkForceCorruption(dst, cb);
                            }

                        if (f_nDropRatio == 0 || !checkDrop(m_channel))
                            {
                            m_cbRead += cb;
                            return cb;
                            }
                        }
                    // else; fall through

                default:
                    return -1;
                }
            }

        /**
         * Migrate this bus connection to a new socket connection.
         *
         * @param eReason  optional exception describing why current connection needs to be replaced
         */
        public synchronized void migrate(Throwable eReason)
            {
                SocketBusDriver.Dependencies depsDriver = f_driver.getDependencies();
                int cSocketReconnectLimit = depsDriver.getSocketReconnectLimit();

                // COH-24389 - a reconnect limit of < 0 indicates that migrations are disabled
                if (getProtocolVersion() == 0 || cSocketReconnectLimit < 0)
                    {
                    scheduleDisconnect(eReason);
                    }
                else // protocol not yet negotiated or >= 1, in either case we can attempt a migration
                    {
                    if (eReason instanceof ConnectException && ++m_cReconnectAttempts > cSocketReconnectLimit)
                        {
                        // we've exhausted our reconnect attempts, disconnect
                        scheduleDisconnect(eReason);
                        return;
                        }

                    // delay reconnect on initial connect and to also help avoid an endless conflict
                    // if both sides were to keep trying to simultaneously reconnect and in doing so invalidate the other
                    // side's reconnect attempt.
                    long cMillisDelay = eReason instanceof ConnectException ||
                                        getLocalEndPoint().getCanonicalName().compareTo(getPeer().getCanonicalName()) > 0
                                        ? depsDriver.getSocketReconnectDelayMillis()
                                        : 0;

                    SocketChannel chan = m_channel;
                    String sChan = chan.toString(); // to preserve port info for subsequent logging

                    closeChannel(chan);

                    scheduleUnsafeTask(chan, new Runnable()
                        {
                        @Override
                        public void run()
                            {
                        synchronized (Connection.this)
                                {
                                if (m_state.ordinal() < ConnectionState.DEFUNCT.ordinal() && chan == m_channel)
                                    {
                                    // COH-24703 - not adding the exception to the LogRecord because logging the stack trace is not useful in this case
                                    getLogger().log(makeRecord(Level.FINER,
                                                                        "{0} migrating connection with {1} off of {2} on {3}: {4}",
                                                                        getLocalEndPoint(), getPeer(), sChan, Connection.this, eReason));

                                    m_eMigrationCause = eReason;
                                    onMigration();

                                    // we're sync'd on the connection so nothing new can be scheduled
                                    try
                                        {
                                        getSelectionService().register(chan, null);
                                        m_handler = null;
                                        }
                                catch (IOException e) {}

                                    // replaces m_channel, so any subsequent exceptions on the old channel won't make it into this if block
                                    // schedule task to ensure register and connect are processed sequentially
                                    scheduleUnsafeTask(chan, () -> connect(), cMillisDelay);
                                    }
                                }
                            }
                        }, cMillisDelay);
                    }
                }

        /**
         * Called when the channel has been selected.
         * <p>
         * If this method throws an exception it will be handled by {@link
         * #onException}
         *
         * @param nOps  the selected ops
         *
         * @return the new interest set
         *
         * @throws IOException on an I/O error
         */
        protected abstract int onReadySafe(int nOps)
            throws IOException;

        /**
         * Called in the event that {@link #onReadySafe} resulted in an
         * exception.
         * <p>
         * The default implementation simply disconnects the connection.
         *
         * @param t  the exception
         *
         * @return  the new interest set, the default implementation returns 0
         */
        protected int onException(Throwable t)
            {
            ConnectionState state = m_state;
            if (t instanceof IOException && !(t instanceof SSLException) && // don't migrate if the failure was due to security
                (state == null || state.ordinal() < ConnectionState.DEFUNCT.ordinal()))
                {
                migrate(t);
                }
            else
                {
                scheduleDisconnect(t);
                }

            return 0;
            }

        // ----- Handler interface --------------------------------------

        /**
         * {@inheritDoc}
         */
        public final int onReady(int nOps)
            {
            try
                {
                return onReadySafe(nOps);
                }
            catch (Throwable t)
                {
                return onException(t);
                }
            }

        /**
         * Set the protocol version.
         *
         * @param nProt  the version to set
         */
        protected void setProtocolVersion(int nProt)
            {
            int nProtocol = m_nProtocol;
            if (nProtocol == -1 || nProtocol == nProt)
                {
                m_nProtocol = nProt;
                }
            else
                {
                throw new IllegalStateException();
                }
            }


        // ----- Object interface ---------------------------------------

        /**
         * {@inheritDoc}
         */
        public String toString()
            {
            SocketChannel channel     = m_channel;
            Socket        socket      = channel == null ? null : channel.socket();
            int           cMigrations = m_cMigrations;
            return "peer=" + getPeer() + ", state=" + m_state
                    + ", socket=" + socket
                    + (cMigrations == 0 ? "" : ", migrations=" + cMigrations)
                    + ", bytes(in=" + m_cbRead + ", out=" + m_cbWrite + ")"
                    + ", flushlock " + f_lockFlush.get();
            }

        @Override
        public int hashCode()
            {
            return f_peer.hashCode(); // only because profiler says Object.hashCode is expensive here
            }

        // ----- data members -------------------------------------------

        /**
         * The peer's EndPoint.
         */
        private final UrlEndPoint f_peer;

        /**
         * Atomic indicating if the connection is being flushed.
         */
        private final AtomicBoolean f_lockFlush = new AtomicBoolean();

        /**
         * The connect state of this connection.
         *
         * volatile for LightBus usage see isValid
         */
        protected volatile ConnectionState m_state;

        /**
         * The identity of this side of the logical connection.  The identity is used during reconnects
         * to ensure we aren't connecting to a different connection which is simply reusing the ip:port
         *
         * This is a non-zero value, which is unique across usages of this ip:port.  Note it is not unique
         * otherwise, and multiple peer connections may share the same identity.
         */
        protected final long f_lIdentity = f_atomicIdGenerator.incrementAndGet();

        /**
         * Our peer's identity.  Note this must not be changed once set to a non-zero value.
         */
        protected long m_lIdentityPeer;

        /**
         * The channel connecting this bus to the peer.
         */
        private SocketChannel m_channel;

        /**
         * For testing purposes only, force the connection to drop all output traffic.
         */
        private boolean m_fDropOutput;

        /**
         * The packet size for this connection
         */
        private int m_cbPacket = -1;

        /**
         * A handshaking connection waiting for this connection to be released
         */
        private HandshakeHandler m_next;

        /**
         * If non-null then any invocations must inserted into the queue rather then
         * scheduled with the SelectionService
         */
        private Queue<Runnable> m_queueDeferred;

        /**
         * The total number of bytes read from the socket.
         */
        protected long m_cbRead;

        /**
         * The total number of bytes written to the socket.
         */
        protected long m_cbWrite;

        /**
         * The negotiated protocol version, or -1 if not yet known.
         */
        protected int m_nProtocol = -1;

        /**
         * The number of connection migrations that have occured.
         */
        protected int m_cMigrations;

        /**
         * The cause of the last initiated migration.
         */
        protected Throwable m_eMigrationCause;

        /**
         * The number of sequential reconnect attempts which have been made on this connection.
         */
        protected int m_cReconnectAttempts;

        /**
         * CRC32 for read thread.
         */
        protected CRC32 f_crcRx;

        /**
         * CRC32 for write threads.
         */
        protected CRC32 f_crcTx;

        /**
         * Current HandShakeHandler for this connection.
         */
        protected volatile SelectionService.Handler m_handler;

        }

    // ---- HandshakeHandler ------------------------------------------------

    /**
     * HandshakeHandler handles the initial transmissions on a SocketChannel
     * as two buses handshake.
     */
    protected class HandshakeHandler
            extends SafeSelectionHandler<SocketChannel>
        {
        /**
         * Construct a HandshakeHandler for the give SocketChannel.
         *
         * @param channel     the socket channel
         * @param connection  the optional connection
         */
        public HandshakeHandler(SocketChannel channel, Connection connection)
            {
            super(channel);
            this.m_connection = connection;

            int cbNegotiate = 4 + // (int) protocol id
                           2 + // (short) min ver
                           2 + // (short) max ver
                           2;  // (short) name char length

            m_headerIn = ByteBuffer.allocate(cbNegotiate);

            ByteBuffer headerOut = m_headerOut = ByteBuffer.allocate(cbNegotiate);
            headerOut.putInt(getProtocolIdentifier())
                     .putShort(getMinimumProtocolVersion())
                     .putShort(getMaximumProtocolVersion())
                     .putShort((short) getLocalEndPoint().getCanonicalName().length());
            headerOut.flip();
            }

        /**
         * {@inheritDoc}
         */
        public int onReadySafe(int nOps)
                throws IOException
            {
            SocketChannel  channel    = getChannel();
            Connection     connection = m_connection;
            ByteBuffer     headerOut  = m_headerOut;
            ByteBuffer     headerIn   = m_headerIn;
            HandshakePhase phase      = m_phase;
            int            nInterest  = 0;

            if (channel.isConnectionPending())
                {
                if (!channel.finishConnect())
                    {
                    getLogger().log(makeRecord(Level.FINEST, "{0} finishConnect pending for {1} on {2}",
                            AbstractSocketBus.this.getLocalEndPoint(),
                            connection.getPeer(),
                            channel.socket()));
                    return OP_CONNECT;
                    }

                connection.m_cReconnectAttempts = 0;

                getLogger().log(makeRecord(Level.FINEST, "{0} socket connected for {1} on {2}",
                        AbstractSocketBus.this.getLocalEndPoint(),
                        connection.getPeer(),
                        channel.socket()));
                }

            if (f_nDropRatio > 0 && checkDrop(channel))
                {
                throw new IOException("test drop; " + phase);
                }

            channel.write(headerOut);

            if ((nOps & OP_READ) != 0 && channel.read(headerIn) < 0)
                {
                throw new IOException("InputShutdown during handshake " + phase + " in " + headerIn + " out " + headerOut);
                }

            if (headerIn.hasRemaining())
                {
                nInterest = OP_READ;
                }

            if (headerOut.hasRemaining())
                {
                nInterest |= OP_WRITE;
                }

            if (nInterest == 0)
                {
                // end of read & write means we're ready for the next phase
                getLogger().log(makeRecord(Level.FINEST, "{0} processing {1} handshake for {2} on {3}",
                        AbstractSocketBus.this.getLocalEndPoint(),
                        phase,
                        connection == null ? null : connection.getPeer(),
                        channel.socket()));

                switch (phase)
                    {
                    case NEGOTIATE:
                        nInterest = onNegotiate();
                        break;

                    case INTRODUCE:
                        nInterest = onIntroduce();
                        break;

                    case ACCEPT:
                        nInterest = onAccept();
                        break;

                    case ABANDON:
                    default:
                        nInterest = onAbandon();
                        break;
                    }

                if (m_phase != phase)
                    {
                    getLogger().log(makeRecord(Level.FINEST, "{0} waiting for {1} handshake for {2} on {3} with interest {4}, {5}B to read, {6}B to write",
                            AbstractSocketBus.this.getLocalEndPoint(),
                            m_phase,
                            m_connection == null ? null : m_connection.getPeer(),
                            getChannel().socket(), nInterest, m_headerIn.remaining(), m_headerOut.remaining()));
                    }
                }

            return nInterest;
            }

        /**
         * {@inheritDoc}
         */
        public int onException(Throwable eReason)
            {
            Connection connection = m_connection;

            if (connection == null)
                {
                close(eReason);
                }
            else
                {
                ConnectionState state = connection.m_state;
                if (connection.m_channel == getChannel() &&
                    state != null && state.ordinal() < ConnectionState.DEFUNCT.ordinal())
                    {
                    connection.onException(eReason);
                    }
                }

            return 0;
            }

        /**
         * Process the peer's protocol identification.
         *
         * @return  the new interest set
         */
        public int onNegotiate()
            {
            Connection connection = m_connection;
            ByteBuffer headerIn   = m_headerIn;
            headerIn.flip();

            // verify protocol
            int nId    = headerIn.getInt();
            int nIdReq = getProtocolIdentifier();
            if (nId != nIdReq)
                {
                if (nId >>> 8 == (nIdReq & 0x00FFFFFF))
                    {
                    // Note: very rarely (about one in a million attempts)
                    // on OS X 10.6 we'll hit a OS X bug which results in the
                    // first byte off the connection being missing. This has
                    // been verified in small provably correct tests as well.
                    nId = nIdReq;

                    // reset header to parse the remainder
                    headerIn.position(3);
                    }
                else // unknown protocol
                    {
                    getLogger().log(makeRecord(Level.WARNING, "{0} rejecting connection from {1}" +
                            " using incompatible protocol id {2}, required {3}",
                                    getLocalEndPoint(),
                                    getChannel().socket().getInetAddress(),
                                    nId, nIdReq));
                    close(new IOException("incompatible protocol"));
                    return 0;
                    }
                }

            // verify version
            short nMin = headerIn.getShort();
            short nMax = headerIn.getShort();
            if (nMin > getMaximumProtocolVersion() ||
                nMax < getMinimumProtocolVersion())
                {
                // no overlapping version
                getLogger().log(makeRecord(Level.WARNING, "{0} rejecting connection from {1}" +
                        " using unsupported protocol {2} version " +
                        "({3} ... {4}), supported ({5} ... {6})",
                                getLocalEndPoint(),
                                getChannel().socket().getInetAddress(),
                                nId, nMin, nMax, getMinimumProtocolVersion(),
                                getMaximumProtocolVersion()));
                close(new IOException("protocol version mismatch"));
                return 0;
                }

            // we support overlappying versions; select the highest shared version
            int nProt = m_nProtocol = Math.min(getMaximumProtocolVersion(), nMax);

            getLogger().log(makeRecord(Level.FINEST, "{0} handshaking with {1}" +
                    " using protocol {2} version {3}",
                            getLocalEndPoint(),
                            getChannel().socket().getInetAddress(),
                            nId, nProt));

            m_phase = HandshakePhase.INTRODUCE;

            // prepare outbound introduction
            String     sName         = getLocalEndPoint().getCanonicalName();
            boolean    fSendConnect  = nProt > 0 && connection != null; // connect type only sent starting with v1
            boolean    fSendIdentity = nProt > 1 && fSendConnect;       // id only sent starting with v2
            ByteBuffer bufOut        = m_headerOut = ByteBuffer.allocate(sName.length() * 2 +
                    (fSendConnect  ? 1 : 0) +
                    (fSendIdentity ? nProt > 3 ? 16 : 8 : 0));
            for (int i = 0, c = sName.length(); i < c; ++i)
                {
                bufOut.putChar(sName.charAt(i));
                }

            if (fSendConnect)
                {
                // note: accepting side doesn't have sufficient information to send the connect type until
                // it has received the initiators full introduction.
                byte nConnect = connection.m_state == ConnectionState.OPEN && connection.m_lIdentityPeer == 0
                        ? CONNECT_NEW : CONNECT_MIGRATE;
                bufOut.put(nConnect);
                if (fSendIdentity)
                    {
                    if (nProt > 3)
                        {
                        bufOut.putLong(connection.f_lIdentity)
                              .putLong(connection.m_lIdentityPeer);
                        }
                    else
                        {
                        bufOut.putLong(nConnect == CONNECT_NEW ? connection.f_lIdentity : connection.m_lIdentityPeer);
                        }
                    }
                }
            bufOut.flip();

            // prepare for inbound introduction
            m_headerIn = ByteBuffer.allocate(headerIn.getShort() * 2 +
                    (nProt > 0 ? 1  : 0) + // see connect type note above
                    (nProt > 3 ? 16 :
                     nProt > 1 ? 8  : 0));

            return OP_READ | OP_WRITE;
            }

        /**
         * Evaluate the introduction.
         *
         * @return the new interest set
         *
         * @throws IOException on an I/O error
         */
        public int onIntroduce()
            throws IOException
            {
            // We've verified that we can communicate, but we now need to
            // ensure that we don't allow multiple concurrent connections
            // between two peers at once. Now that we have the protocol
            // header we know we've ensured that both sides will know who
            // is at the other end of the socket.  Each side will ensure that
            // they aren't already opening up another socket in the reverse
            // direction, and that they don't already have an existing socket.
            // In the case that both sides simultaneous open sockets to one
            // another the peer with the lower canonical name will win. So here
            // we will do one of the following:
            // a. Identify that we've initiated this connection and move to
            //    the accept phase.
            // b. Identify that we did not initiate this connection, but that
            //    we have no outgoing connection to the same peer, and move
            //    to the accept phase
            // c. Identify that we did not initiate this connection, and that
            //    we do have an outgoing connection to a lower peer.  We will
            //    close our existing socket, and replace it with this one, on
            //    the same Connection object.
            // d. Identify that we did not initiate this connection, and that
            //    we do have an outgoing connection to the same peer. In this
            //    case we will "abandon" this connection. It is important
            //    that we don't close the connection until we know that peer
            //    has performed option "c" from above.
            // e. Identify that we have an active connection to this peer.
            //    While this may seem like an illegal state it can happen for
            //    one legitimate reasons.  The existing connection may
            //    have been disconnected on the remote end, and we just haven't
            //    received the "close" packet yet, and the new "open" packet
            //    which isn't required to appear in order wrt the close arrived
            //    first.

            // TODO: now that we have migration support we don't technically
            // need to worry about seniority during simultaneous connect, we
            // can let any new connection simply close any existing connection
            // this will simply trigger a migration.  Note it is possible that
            // two peers could do this indefinitely, i.e. closing each others
            // connections, we may need to add in some migration backoff
            ByteBuffer headerIn = m_headerIn;

            headerIn.flip();

            int nProt  = m_nProtocol;
            int cbName = headerIn.limit() -
                    ((nProt > 0 ? 1  : 0) +  // connect type
                     (nProt > 3 ? 16 :
                      nProt > 1 ? 8  : 0)); // ID

            char[] achName  = new char[cbName / 2];
            for (int i = 0, c = achName.length; i < c; ++i)
                {
                achName[i] = headerIn.getChar();
                }

            final UrlEndPoint peer = f_driver.resolveSocketEndPoint(new String(achName));

            int        nInterest  = OP_READ | OP_WRITE;
            Connection connection = m_connection;
            boolean    fInbound   = connection == null;
            byte       nConnect   = nProt > 0 ? headerIn.get() : CONNECT_NEW;
            long       lIdThatRx;
            long       lIdThisRx;

            if (nProt > 3)
                {
                lIdThatRx = headerIn.getLong();
                lIdThisRx = headerIn.getLong();
                }
            else if (nProt > 1)
                {
                switch (nConnect)
                    {
                    case CONNECT_NEW:
                        lIdThatRx = headerIn.getLong();
                        lIdThisRx = 0;
                        break;

                    case CONNECT_MIGRATE:
                        lIdThisRx = headerIn.getLong();
                        lIdThatRx = 0;
                        break;

                    default:
                        throw new IOException("protocol error");
                    }
                }
            else
                {
                lIdThatRx = lIdThisRx = 0;
                }

            if (fInbound)
                {
                // in-bound connection

                // check if we already have an existing connection for this
                // peer, this is unlikely, so we will attempt to register
                // our new connection at the same time

                final Connection connNew;

                switch (nConnect)
                    {
                    case CONNECT_NEW:
                        {
                        Connection connOld = AbstractSocketBus.this.f_mapConnections.get(peer);
                        if (connOld == null || lIdThatRx == 0 || connOld.m_lIdentityPeer != lIdThatRx ||
                            connOld.m_state.ordinal() >= ConnectionState.DEFUNCT.ordinal())
                            {
                            connNew = makeConnection(peer);
                            connNew.m_nProtocol     = nProt;
                            connNew.m_channel       = getChannel();
                            connNew.m_lIdentityPeer = lIdThatRx;
                            break;
                            }
                        // else; we lost the connection when we were half connected, i.e. we had learned our peer's ID,
                        // but it had not learned ours. Since it didn't know ours it could only do a CONNECT_NEW, but
                        // since we'd managed to learn its ID, we do have sufficient info to migrate.

                        nConnect  = CONNECT_MIGRATE;
                        lIdThisRx = connOld.f_lIdentity;
                        // fall through
                        }

                    case CONNECT_MIGRATE:
                        {
                        // as part of migrating a connection we must ensure that we're migrating to the
                        // same logical connection.  This is accomplished by only allowing migration if
                        // the connection IDs (ours and our peers) remain the same.  We'll each validate
                        // each other's IDs.  Note, it is ok to have only one of the two sets match so long
                        // as the other set doesn't match because of an unknown (0).  We handle them potentially
                        // not knowing our ID above.
                        Connection connOld = AbstractSocketBus.this.f_mapConnections.get(peer);
                        if (connOld != null &&
                            (connOld.f_lIdentity == lIdThisRx || nProt == 1) &&
                            (lIdThatRx == 0 || connOld.m_lIdentityPeer == 0 || connOld.m_lIdentityPeer == lIdThatRx) && // peer id may still be unknown, and we learn it now
                            connOld.m_state.ordinal() < ConnectionState.DEFUNCT.ordinal())
                            {
                            SocketChannel chanOld = connOld.m_channel;
                            SocketChannel chanNew = getChannel();

                            scheduleUnsafeTask(chanOld, new Runnable()
                                {
                                @Override
                                public void run()
                                    {
                                    synchronized (connOld)
                                        {
                                        if (connOld.m_channel == chanOld && connOld.m_state.ordinal() <= ConnectionState.ACTIVE.ordinal())
                                            {
                                            if (connOld.m_state == ConnectionState.ACTIVE)
                                                {
                                                // to get this far we know that the TCP connection went down without the user choosing
                                                // for it to, and without the other end dying, thus we should log a higher level log
                                                // message, there is something wrong with the environment, though we are auto-correcting
                                                // it.

                                                getLogger().log(
                                                        makeExceptionRecord(Level.WARNING, connOld.m_eMigrationCause,
                                                                "{0} accepting connection migration with {1}, replacing {2} with {3}: {4}",
                                                                getLocalEndPoint(), peer, connOld.m_channel, getChannel(), connOld));
                                                connOld.m_eMigrationCause = null;
                                                }
                                            // else; during connection establishment we may have simply had a connect timeout, this is not
                                            // worth logging a warning over

                                            // move onto the accept phase
                                            m_phase = HandshakePhase.ACCEPT;
                                            m_headerIn.clear().limit(1);

                                            m_headerOut.clear();
                                            m_headerOut.put(CONNECT_MIGRATE);
                                            connOld.m_nProtocol = nProt; // may not have been learned previously
                                            if (nProt > 3)
                                                {
                                                // it's possible that we hadn't yet learned our peer's ID, though it
                                                // had clearly learned ours
                                                if (connOld.m_lIdentityPeer == 0)
                                                    {
                                                    connOld.m_lIdentityPeer = lIdThatRx;
                                                    }
                                                // else; we've already asserted that the ids are equal above

                                                m_headerOut.putLong(connOld.f_lIdentity);
                                                m_headerOut.putLong(connOld.m_lIdentityPeer);
                                                }
                                            else if (nProt > 1)
                                                {
                                                m_headerOut.putLong(connOld.m_lIdentityPeer);
                                                }

                                            m_headerOut.put((byte) 0).flip(); // accept indicator

                                            m_connection = connOld;
                                            closeChannel(chanOld);

                                            connOld.onMigration();

                                            connOld.m_channel = chanNew;

                                            // re-register this channel on the original handler to resume processing
                                            try
                                                {
                                                getSelectionService().register(getChannel(), HandshakeHandler.this);
                                                connOld.m_handler = HandshakeHandler.this;
                                                }
                                            catch (IOException e)
                                                {
                                                closeChannel(chanNew);
                                                }
                                            }
                                        else
                                            {
                                            closeChannel(chanNew);
                                            }
                                        }
                                    }
                                }, /*cMillis*/ 0);
                            return 0;
                            }

                        // we can't reconnect what we do not have (or to what we've disconnected from); abandon the connection
                        getLogger().log(
                                makeRecord(Level.FINE, "{0} rejecting connection migration from {1} on {2}, no existing connection {3}/{4}",
                                        getLocalEndPoint(), peer, getChannel().socket().getLocalSocketAddress(),
                                        lIdThisRx, (connOld == null ? 0 : connOld.f_lIdentity)));
                        m_headerIn.clear(); // some read space > accept size
                        m_phase = HandshakePhase.ABANDON;

                        ByteBuffer bufOut = m_headerOut = ByteBuffer.allocate(1 + (nProt > 3 ? 16 : nProt > 1 ? 8 : 0));
                        bufOut.put(CONNECT_NEW); // indicate that we don't have awareness of this connection

                        // we don't have a connection, and thus don't have an identity; just write zeros
                        if (nProt > 3)
                            {
                            bufOut.putLong(0).putLong(0);
                            }
                        else if (nProt > 1)
                            {
                            bufOut.putLong(0);
                            }

                        bufOut.flip();

                        return OP_READ | OP_WRITE;
                        }

                    default:
                        throw new IOException("protocol error");
                    }

                try
                    {
                    synchronized (connNew)
                        {
                        final Connection connOld;

                        Lock lock = AbstractSocketBus.this.f_lockState.readLock();
                        lock.lock();
                        try
                            {
                            BusState nStateCurr = AbstractSocketBus.this.m_nState;
                            if (nStateCurr != BusState.OPEN)
                                {
                                // Bus is closed. Close the handler and return
                                getLogger().log(
                                        makeRecord(Level.FINE, "{0} rejecting connection from {1}, bus is closing",
                                                getLocalEndPoint(),
                                                getChannel().socket().getInetAddress()));
                                close(null);
                                return 0;
                                }

                            connOld = AbstractSocketBus.this.f_mapConnections.putIfAbsent(peer, connNew);

                            if (connOld == null)
                                {
                                // common case, simply open the new connection
                                m_connection = connection = connNew;
                                connection.open();
                                }
                            }
                        finally
                            {
                            lock.unlock(); // only need for putIfAbsent, and connection.open()
                            }

                        if (connOld != null)
                            {
                            synchronized (connOld)
                                {
                                switch (connOld.m_state)
                                    {
                                    case OPEN:
                                    {
                                    // We have an out-bound pending connection which
                                    // has yet to finish handshaking; apparently our
                                    // peer has the same.  We need to choose one, and
                                    // ensure that the peer makes the same choice.

                                    // lesser of the two acceptor endpoints wins
                                    final EndPoint self = getLocalEndPoint();
                                    if (self.getCanonicalName()
                                            .compareTo(peer.getCanonicalName()) < 0)
                                        {
                                        // this bus wins; don't accept the
                                        // peer's connection, our initiated
                                        // connection will eventually get
                                        // accepted by them, and cause them
                                        // to close this losing channel.

                                        getLogger().log(makeRecord(Level.FINER,
                                                "{0} wins simultaneous connect with {1}, abandoning {2}",
                                                self, peer, getChannel().socket()));

                                        m_phase = HandshakePhase.ABANDON;
                                        m_headerIn.clear().limit(2); // some read space > accept size
                                        m_headerOut.clear().flip(); // no accept byte
                                        return OP_READ;
                                        }
                                    else
                                        {
                                        // this bus looses; we will continue to
                                        // use connOld, but with the peer's
                                        // initiated channel

                                        getLogger().log(makeRecord(Level.FINER,
                                                "{0} loses simultaneous connect with {1}, closing {2}",
                                                self, peer, connOld.m_channel.socket()));

                                        // couple this new channel, and
                                        // HandshakeHandler with the old Connection
                                        // This requires executing all Runnables on the
                                        // SelectionSvc thread associated with the old channel
                                        // before the new channel is linked with the
                                        // old Connection. (for maintaining execution order).

                                        // Register a Runnable on the old channel.
                                        // Execution of this Runnable ensures that there is no
                                        // pending Runnables on the old SS thread for this connection.
                                        getLogger().log(makeRecord(Level.FINER,
                                                "{0} deferring handshake attempt with {1} on {2}",
                                                self, peer, getChannel().socket()));

                                        final SocketChannel channelNew = getChannel();
                                        final SelectionService.Handler handlerNew = this;

                                        connOld.invoke(
                                                new Runnable()
                                                {
                                                @Override
                                                public void run()
                                                    {
                                                    boolean fContinue;
                                                    synchronized (connOld)
                                                        {
                                                        try
                                                            {
                                                            connOld.m_channel.close();
                                                            }
                                                        catch (IOException ioe)
                                                            {
                                                            }

                                                        if (connOld.m_state.ordinal() < ConnectionState.DEFUNCT.ordinal())
                                                            {
                                                            connOld.m_channel       = channelNew;
                                                            connOld.m_lIdentityPeer = lIdThatRx;
                                                            fContinue = true;
                                                            }
                                                        else
                                                            {
                                                            // application released the connection during a concurrent
                                                            // connect, just drop the new channel
                                                            fContinue = false;
                                                            try
                                                                {
                                                                channelNew.close();
                                                                }
                                                            catch (IOException e)
                                                                {
                                                                }
                                                            }

                                                        // re-open invocation
                                                        Queue<Runnable> queueDeferred = connOld.m_queueDeferred;
                                                        connOld.m_queueDeferred = null;

                                                        // reschedule any deferred invocations
                                                        for (Runnable runnable : queueDeferred)
                                                            {
                                                            connOld.invoke(runnable);
                                                            }
                                                        }

                                                    if (fContinue)
                                                        {
                                                        getLogger().log(makeRecord(Level.FINER,
                                                                "{0} continue handshake attempt with {1} on {2}",
                                                                self, peer, getChannel().socket()));

                                                        try
                                                            {
                                                            // Enable read/write interest for the new channel
                                                            getSelectionService().register(channelNew, handlerNew);
                                                            connOld.m_handler = handlerNew;
                                                            }
                                                        catch (IOException ioe)
                                                            {
                                                            onException(ioe);
                                                            }
                                                        }
                                                    }
                                                }
                                        );

                                        m_connection = connection = connOld;

                                        // disable read/write interest for the new channel. It
                                        // will be re-enabled when all the pending Runnables on the old
                                        // channel has been executed.
                                        nInterest = 0;

                                        // defer new Runnables until we switch channels
                                        if (connOld.m_queueDeferred == null)
                                            {
                                            connOld.m_queueDeferred = new LinkedList<Runnable>();
                                            }
                                        // else; deferral is already in progress (not sure this can even happen)
                                        }
                                    }
                                    break;

                                    case ACTIVE:
                                        // We can end up in this situation if the local
                                        // disconnect hasn't yet happened.
                                        // Fall though and enqueue the new connection
                                        // so that it can/ be processed when the old
                                        // connection eventually goes away.

                                        // fall through
                                    case DEFUNCT:
                                        // our connection is DISCONNECTED but
                                        // the app has yet to release it.

                                        // record this into connOld, closing any
                                        // prior one
                                        if (connOld.m_next == null)
                                            {
                                            getLogger().log(makeRecord(Level.FINE,
                                                    "{0} deferring reconnect attempt from {1} on {2}, pending release",
                                                    getLocalEndPoint(), peer, getChannel().socket()));
                                            }
                                        else
                                            {
                                            getLogger().log(makeRecord(Level.FINE,
                                                    "{0} replacing deferred reconnect attempt from {1} on {2}, pending release",
                                                    getLocalEndPoint(), peer, getChannel().socket()));
                                            connOld.m_next.close(null);
                                            }
                                        m_connection = connection = connNew;
                                        connOld.m_next = this;
                                        nInterest = 0; // deffer accept until release
                                        break;

                                    case FINAL:
                                        // to see this the old connection has or
                                        // is in the process of being unregistered
                                        // try again, i.e. just pop out and let the
                                        // next selection operation call back in
                                        // for another try
                                        // NOTE: headers have been left unchanged
                                        return OP_WRITE;

                                    default:
                                        throw new IllegalStateException("state = " + connOld.m_state);
                                    }
                                }
                                }
                            }
                        }
                    finally
                        {
                    if (m_connection != connNew)
                        {
                        connNew.dispose();
                        }
                    }

                m_headerOut.clear();
                if (nProt > 0)
                    {
                    // Note, that if we're here nConnect == CONNECT_NEW, as inbound migrations are completely handled above.

                    // the acceptor doesn't have sufficient information to send the connect type until it
                    // has received the initator's introduction, so we send it at the start of the accept
                    // phase.  Note that on the wire this doesn't look asymetrical since the two phases are
                    // back to back, i.e. the initiator is still in its introduction phase awaiting this byte.
                    m_headerOut.put(nConnect); // if we got this far we're just echoing back the same connect type as the other side

                    if (nProt > 1)
                        {
                        m_headerOut.putLong(connection.f_lIdentity);

                        if (nProt > 3)
                            {
                            m_headerOut.putLong(connection.m_lIdentityPeer);
                            }
                        }
                    }
                }
            else // outbound connection
                {
                if (!connection.getPeer().equals(peer))
                    {
                    // out-bound connection, but the peer replied with
                    // a different name then we used.  While this is ok from an
                    // inet perspective, it is not ok from a bus perspective since
                    // in order to disallow multiple connections between peers
                    // each peer can only be known via one name.

                    getLogger().log(makeRecord(Level.FINER, "{0} Out-bound connection to" +
                            " {1}, found {2}, single connection pair cannot be ensured",
                            getLocalEndPoint(),
                            connection.getPeer(), peer));

                    // Should this be threaded as an error?
                    // the reason we don't treat it as an error is that the other side could be listening
                    // on the wildcard address, and we're connecting to it via a specific IP.  So here
                    // we choose to be practical and allow the possibility of multiple connections between
                    // peers rather then to not allow connections at all.

                    // TODO improve the protocol so that can be both practical and accurate
                    /*
                    close(new IOException("peer mismatch, expected " +
                            connection.getPeer() + " found " + peer));
                    return 0;*/
                    }

                switch (nConnect) // from peer's perspective
                    {
                    case CONNECT_NEW:
                        long lIdPeerCurr = connection.m_lIdentityPeer;
                        if (lIdPeerCurr == 0)
                            {
                            // common case; initial connection, we learn the peer's identity
                            connection.m_lIdentityPeer = lIdThatRx;
                            }
                        else
                            {
                            // since we'd previously known our peer's id we  must have initiated a migration
                            // and the peer responded with CONNECT_NEW indicating that they didn't have a
                            // matching connection.  lId should be 0 if its not that is some form of protocol error
                            connection.scheduleDisconnect(new IOException(
                                    "connection migration rejected by peer; no existing connection"));
                            return OP_READ;
                            }
                        // else; we're migrating to the same peer
                        break;

                    case CONNECT_MIGRATE:
                        // the accepting peer indicated it is a migration, apparently we must have also sent a migration
                        // or the peer had more info then us (prior half connect) and had a connection with our connection
                        // ID, thus allowing a migration
                        if (nProt > 1)
                            {
                            if (connection.f_lIdentity != lIdThisRx)
                                {
                                // but we've been recycled, i.e. not the same logical connection
                                connection.scheduleDisconnect(new IOException(
                                        "connection migration failed; mismatch on local identity " +
                                        connection.f_lIdentity + "/" + lIdThisRx));
                                return OP_READ;
                                }
                            else if (connection.m_lIdentityPeer == 0)
                                {
                                // this can only happen because of a prior half connect, where we hadn't learned our peer's
                                // connection ID, but it had learned ours.  Our peer then decided that this could be
                                // a migration, and thus we learn their ID now
                                connection.m_lIdentityPeer = lIdThatRx;
                                // fall through
                                }
                            else if (nProt > 3 && connection.m_lIdentityPeer != lIdThatRx)
                                {
                                // should not be possible; this is basically an assertion
                                connection.scheduleDisconnect(new IOException(
                                        "connection migration failed (protocol error); mismatch on remote identity " +
                                        connection.m_lIdentityPeer + "/" + lIdThatRx));
                                return OP_READ;
                                }
                            // else; identity match, accept the migration
                            }

                        getLogger().log(
                                makeExceptionRecord(Level.WARNING, connection.m_eMigrationCause,
                                        "{0} accepted connection migration with {1} on {2}: {3}",
                                        getLocalEndPoint(), peer, getChannel(), connection));

                        connection.m_eMigrationCause = null;
                        break;
                    }

                m_headerOut.clear();
                }

            connection.setProtocolVersion(nProt);

            // move onto the accept phase
            m_phase = HandshakePhase.ACCEPT;
            m_headerIn.clear().limit(1);
            m_headerOut.put((byte) 0) // accept byte
                    .flip();

            return nInterest;
            }

        /**
         * Evaluate the "accept" byte.
         *
         * @return the new interest set
         *
         * @throws IOException on an I/O error
         */
        public int onAccept()
            throws IOException
            {
            // getting here means that we've received the accept byte
            // the value is actually meaningless, but getting the byte is
            // as it will only be sent if the other side has accepted our
            // connection, otherwise they would just close the socket

            Connection connection = m_connection;
            synchronized (connection)
                {
                switch (connection.m_state)
                    {
                    case OPEN:
                        connection.m_state = ConnectionState.ACTIVE;
                        // fall through

                    case ACTIVE: // because of migration
                        // switch out the handlers
                        getSelectionService().register(getChannel(), connection);
                        connection.m_handler = connection;
                        break;

                    default:
                        throw new IllegalStateException("state = " + connection.m_state);
                    }
                }

            return 0;
            }

        /**
         * Handle extra data supplied to an abandoned connection.
         *
         * @return  the new interest set.
         */
        public int onAbandon()
            {
            // to get here we've read more then just the peer's accept byte
            // this means that it has started to use the connection, which
            // should not be possible, actively reject the connection
            close(new IOException("protocol error"));

            return OP_READ;
            }

        /**
         * Close the HandshakeHandler's channel.
         *
         * @param eReason  the reason for the disconnect, or null
         */
        public void close(Throwable eReason)
            {
            // we are closing a channel which never reached the point where
            // we could exchange data, this channel may or may not be
            // associated with a local Connection.

            SocketChannel  channel    = getChannel();
            Connection     connection = m_connection;
            HandshakePhase phase      = m_phase;

            if (connection == null)
                {
                if (phase != HandshakePhase.ABANDON)
                    {
                    getLogger().log(makeExceptionRecord(
                            eReason instanceof SSLException ? Level.WARNING : Level.FINEST, eReason,
                            "{0} close due to exception during handshake phase {1} on {2}",
                            getLocalEndPoint(), phase, channel.socket()));
                    }
                }
            else
                {
                connection.scheduleDisconnect(eReason);
                }

            closeChannel(channel);
            }

        // ----- data members -------------------------------------------

        /**
         * Non-null once we can associate the handshake with a local
         * Connection.
         * <p>
         * For out-bound connections this happens immediately, while for
         * in-bound connections the data isn't available until we've finished
         * the introduction phase.
         *
         */
        protected Connection m_connection;

        /**
         * The handshake state.
         */
        protected HandshakePhase m_phase = HandshakePhase.NEGOTIATE;

        /**
         * The out-bound protocol header.
         * <p>
         * The buffer will be resized as needed for each phase.
         */
        protected ByteBuffer m_headerOut;

        /**
         * The in-bound protocol header.
         * <p>
         * Initially only large enough to complete the identification phase,
         * it will be resized for the introduction phase once we know that
         * we can communicate with the peer.
         */
        protected ByteBuffer m_headerIn;

        /**
         * The negotiated protocol version.
         */
        protected int m_nProtocol;
        }


    // ---- AcceptHandler ---------------------------------------------------

    /**
     * AcceptHandler accepts new client connections.
     */
    protected class AcceptHandler
            extends SafeSelectionHandler<ServerSocketChannel>
        {
        /**
         * Construct an AcceptHandler for the bus.
         *
         * @param channel the ServerSocketChannel
         */
        protected AcceptHandler(ServerSocketChannel channel)
            {
            super(channel);
            }

        /**
         * {@inheritDoc}
         */
        public int onReadySafe(int nOps)
            {
            SocketChannel chan = null;
            try
                {
                chan = getChannel().accept();
                if (chan != null)
                    {
                    getLogger().log(makeRecord(Level.FINEST,
                            "{0} starting phase NEGOTIATE on {1}",
                            getLocalEndPoint(), chan.socket()));

                    Sockets.configureBlocking(chan, false);
                    configureSocket(chan.socket());

                    getSelectionService().register(chan, new HandshakeHandler(chan, null));
                    }
                }
            catch (IOException e)
                {
                if (chan == null)
                    {
                    // error in accept
                    throw new RuntimeException(e);
                    }
                else // error with new channel; just close it
                    {
                    try
                        {
                        chan.close();
                        }
                    catch (IOException e1) {}
                    }
                }
            return OP_ACCEPT;
            }

        /**
         * {@inheritDoc}
         */
        public int onException(Throwable t)
            {
            // in the event of an unexpected exception such as OOME, we
            // likely don't want to close our server socket
            ServerSocketChannel channel = getChannel();
            if (channel.isOpen())
                {
                getLogger().log(makeExceptionRecord(Level.INFO, t,
                        "{0} unexpected exception during Bus accept, ignoring", getLocalEndPoint()));
                return OP_ACCEPT;
                }
            else // not open
                {
                synchronized (AbstractSocketBus.this)
                    {
                    if (m_nState == BusState.OPEN)
                        {
                        // our ServerSocket was unexpectedly closed; can this even happen?
                        getLogger().log(makeExceptionRecord(Level.SEVERE, t,
                                "{0} ServerSocket failure; no new connection will be accepted", getLocalEndPoint()));
                        }
                    return 0;
                    }
                }
            }
        }

    /**
     * BusState represents the various states a Bus may be in.
     */
    protected enum BusState
        {
        /**
         * State indicate that the bus has yet to be opened.
         */
        INITIAL,

        /**
         * State indicate that the bus has been opened.
         */
        OPEN,

        /**
         * State indicate that the bus is closing.
         */
        CLOSING,

        /**
         * State indicate that the bus has been closed.
         */
        CLOSED
        }


    // ----- data members ---------------------------------------------------

    /**
     * The SocketDriver which produced this bus.
     */
    protected final SocketBusDriver f_driver;

    /**
     * For the purpose of testing failed connections.
     */
    protected final int f_nDropRatio;

    /**
     * For the purpose of testing corrupted data stream.
     */
    protected final int f_nCorruptionRatio;

    /**
     * True if CRC validation is enabled for this bus.
     */
    protected final boolean f_fCrc;

    /**
     * The ServerSocketChannel on which this bus accepts new connections.
     */
    private final ServerSocketChannel f_channelServer;

    /**
     * The state of the bus.
     * <p>
     * Changes to the state must be done which holding the write lock on
     * f_lockState.
     */
    private volatile BusState m_nState = BusState.INITIAL;

    /**
     * Lock protecting changes to the bus state.
     * <p>
     * The write lock must be held when changing the bus state. The read lock
     * must be held when adding or releasing connections. Otherwise locking is
     * done on a per-connection basis, by synchronizing on the connection.
     */
    private final ReadWriteLock f_lockState = new ReentrantReadWriteLock();

    /**
     * The local EndPoint for the bus.
     */
    protected UrlEndPoint m_pointLocal;

    /**
     * The registered event collector.
     */
    private Collector<Event> m_collectorEvent;

    /**
     * Map of current connections.
     * <p>
     * Changes to this map must be made while holding the read lock on
     * f_lockState.
     */
    private final ConcurrentMap<EndPoint, Connection> f_mapConnections = new ConcurrentHashMap<>();

    /**
     * Set of connections to flush.
     */
    private final Set<Connection> f_setFlush = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * Empty connection array.
     */
    private static final Connection[] EMPTY_CONNECTION_ARRAY = new Connection[0];

    /**
     * The connection ID generator.
     */
    protected static final AtomicLong f_atomicIdGenerator = new AtomicLong(SafeClock.INSTANCE.getSafeTimeMillis());

    /**
     * used to indicate that a new connection is desired.
     */
    private static final byte CONNECT_NEW = 0;

    /**
     * Used to indicate that the peer wishes to migrate a connection to a new channel
     */
    private static final byte CONNECT_MIGRATE = 1;
    }
