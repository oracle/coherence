/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.net.exabus;


import com.oracle.coherence.common.io.BufferSequence;


/**
 * A MessageBus is a Bus that provides a message-passing communication model.
 *
 * @author mf/gg/cp  2010.10.04
 */
public interface MessageBus
        extends Bus
    {
    /**
     * Send a message to an EndPoint.
     * <p>
     * Upon {@link Event.Type#RECEIPT completion} of the asynchronous
     * operation it is guaranteed that the peer will eventually have a
     * {@link Event.Type#MESSAGE MESSAGE} event with the specified
     * BufferSequence contents emitted to its event collector.
     *
     * @param peer     the target EndPoint
     * @param bufseq   the contents of the message to send
     * @param receipt  the optional receipt
     *
     * @throws IllegalArgumentException if the peer is unknown to the Bus
     * @throws UnsupportedOperationException if the message size exceeds the
     *         size supported by the bus
     */
    public void send(EndPoint peer, BufferSequence bufseq, Object receipt);

    /**
     * Send a message to an EndPoint.
     * <p>
     * Upon {@link Event.Type#RECEIPT completion} of the asynchronous
     * operation it is guaranteed that the peer will eventually have a
     * {@link Event.Type#MESSAGE MESSAGE} event with the specified
     * BufferSequence contents emitted to its event collector.
     *
     * @param peer          the target EndPoint
     * @param bufseq        the contents of the message to send
     * @param receipt       the optional receipt
     * @param fSocketWrite  true if the caller is willing to offer its cpu to
     *                      perform a socket write
     *
     * @throws IllegalArgumentException if the peer is unknown to the Bus
     * @throws UnsupportedOperationException if the message size exceeds the
     *         size supported by the bus
     */
    public default void send(EndPoint peer, BufferSequence bufseq, Object receipt, boolean fSocketWrite)
        {
        send(peer, bufseq, receipt);
        }
    }
