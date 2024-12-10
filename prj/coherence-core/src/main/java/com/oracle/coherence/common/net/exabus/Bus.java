/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.net.exabus;


import com.oracle.coherence.common.base.Collector;

import java.io.Closeable;


/**
 * A Bus represents a communication mechanism that allows the exchange of
 * information between multiple peers, called EndPoints.
 * <p>
 * Communication with another peer requires a connection. The boundaries of a
 * connection are identified by a pair of CONNECT and RELEASE events. Unless
 * otherwise specified, all operations occurring within a connection between
 * the two peers are ordered based upon the order in which they were invoked
 * upon the source bus.
 * <p>
 * Bus operations taking receipts are asynchronous. Completion of asynchronous
 * operations is identified via a corresponding {@link Event.Type#RECEIPT
 * RECEIPT} event {@link #setEventCollector(Collector)}
 * arriving locally} prior to any {@link Event.Type#DISCONNECT DISCONNECT}
 * event for the same peer. Any RECEIPT event arriving after the DISCONNECT
 * event indicates an operation which is not known to have completed.
 * Completion of an operation indicates that the operation reached the peer
 * and is ready for processing. As operations are ordered this also indicates
 * that all prior operations reached the peer as well. Note RECEIPT events are
 * not provided for <tt>null</tt> receipts but can be inferred from a RECEIPT
 * event for a subsequent operation against the same peer.
 * <p>
 * Invoking an asynchronous operation may not result in the operation being
 * immediately dispatched to the peer. The dispatch is only ensured once
 * a call to {@link #flush flush()} is issued. The general usage pattern would
 * be to perform a series of asynchronous operations, followed by a call to
 * flush.
 * <p>
 * Unless otherwise noted all bus operations are thread-safe and all
 * asynchronous bus operations are re-entrant, i.e. may be issued from
 * within a collector callback.
 *
 * @author mf/gg/cp 2010.10.04
 */
public interface Bus
    extends Closeable
    {
    /**
     * Return the EndPoint this bus is bound to.  Parties wishing to transport
     * information to this bus, can connect via this EndPoint.
     * <p>
     * Note: The returned EndPoint may be a different object then the one
     * supplied to the {@link Depot} at creation time.  The EndPoint may in
     * fact not even be {@link Object#equals equal} to the original if the
     * supplied EndPoint for instance had represented an ephemeral endpoint.
     * Once the bus is open this method will always return the same value.
     *
     * @return  the local EndPoint
     */
    public EndPoint getLocalEndPoint();

    /**
     * Open the bus, allowing it to begin exchanging data.
     * <p>
     * Upon completion of the asynchronous operation a {@link Event.Type#OPEN
     * OPEN} event will emitted to its event collector.
     */
    public void open();

    /**
     * Close the bus. This prevents any new connections and {@link #release releases}
     * all existing connections, thus preventing any further data exchanges.
     * <p>
     * Upon completion of the asynchronous operation a {@link Event.Type#CLOSE
     * CLOSE} event will emitted to its event collector.
     * <p>
     * Once closed a bus cannot be re-opened.
     */
    public void close();

    /**
     * Connect this bus to an EndPoint.
     * <p>
     * Prior to the completion of this operation a {@link Event.Type#CONNECT
     * CONNECT} event will emitted to the event collector unless the Bus is
     * already connected to the specified peer.
     * <p>
     * A successful completion of this operation does not imply that the other
     * party is actually reachable, it only sets up a starting point in the
     * conversation.
     *
     * @param peer  the EndPoint
     *
     * @throws IllegalArgumentException if the EndPoint type is not supported
     */
    public void connect(EndPoint peer);

    /**
     * Disconnect an EndPoint from this bus.
     * <p>
     * Upon completion of the asynchronous operation a {@link
     * Event.Type#DISCONNECT DISCONNECT} event will emitted to its event
     * collector unless the Bus had already disconnected from the specified
     * peer.
     * <p>
     * Note: to allow future connections with the same peer {@link #release}
     * must also be called.
     *
     * @param peer  the EndPoint
     *
     * @throws IllegalArgumentException if the peer is unknown to the Bus
     */
    public void disconnect(EndPoint peer);

    /**
     * Release an EndPoint from this bus.
     * <p>
     * The release operation drops all former state associated with a
     * connection to a peer, allowing a new connection to be established. If
     * called on for a connected peer, a disconnect will be performed before
     * the release.
     * <p>
     * Upon completion of the asynchronous operation a {@link
     * Event.Type#RELEASE RELEASE} event will emitted to its event
     * collector.
     *
     * @param peer the EndPoint
     *
     * @throws IllegalArgumentException if the peer is unknown to the Bus
     */
    public void release(EndPoint peer);

    /**
     * Ensure that any buffered asynchronous operations are dispatched.
     * <p>
     * Upon completion of the asynchronous operation all previously buffered
     * asynchronous operations will have been dispatched.
     * </p>
     */
    public void flush();

    /**
     * Ensure that any buffered asynchronous operations are dispatched.
     * <p>
     * Upon completion of the asynchronous operation all previously buffered
     * asynchronous operations will have been dispatched.
     * </p>
     *
     * @param fSocketWrite  true if the caller is willing to offer its cpu to
     *                      perform a socket write
     */
    public default void flush(boolean fSocketWrite)
        {
        flush();
        }

    /**
     * Register a collector which will receive events for this Bus.
     * <p>
     * Collector operations are expected to complete in a timely manner, as
     * they may be called on dedicated Bus threads and blocking could impact
     * the overall Bus performance. A typical implementation may simply add
     * each event to a queue for later processing on application threads.
     * <p>
     * The collector may be called concurrently if the bus is multi-threaded,
     * however the collector will not be called concurrently for multiple
     * events {@link Event#getEndPoint() associated} with the same EndPoint.
     *
     * @param collector  the event collector
     *
     * @throws IllegalStateException if the Bus is open
     */
    public void setEventCollector(Collector<Event> collector);

    /**
     * Obtain the registered event collector.
     *
     * @return the event collector.
     */
    public Collector<Event> getEventCollector();

    /**
     * Return a human readable description of the connection with the peer.
     *
     * @param peer  the peer
     *
     * @return a string describing the connection
     */
    public default String toString(EndPoint peer)
        {
        return peer == null ? "null" : peer.toString();
        }
    }
