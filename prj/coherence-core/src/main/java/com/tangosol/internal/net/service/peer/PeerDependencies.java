/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.peer;

import com.tangosol.internal.net.service.ServiceDependencies;

import com.tangosol.io.WrapperStreamFactory;

import com.tangosol.net.messaging.Codec;

import java.util.List;

/**
* The PeerDependencies interface provides a Peer object with its external dependencies.
*
* @author pfm  2011.06.27
* @since Coherence 12.1.2
*/
@SuppressWarnings("deprecation")
public interface PeerDependencies
        extends ServiceDependencies
    {
    /**
     * Return the list of filters used by the service.
     *
     * @return the filters list
     */
    public List<WrapperStreamFactory> getFilterList();

    /**
     * Return the message codec used by the Service.
     *
     * @return the message codec
     */
    public Codec getMessageCodec();

    /**
     * Return the ping interval which is the number of milliseconds between successive
     * connection "pings" or 0 if heartbeats are disabled.
     *
     * @return the ping interval
     */
    public long getPingIntervalMillis();

    /**
     * Return the default request timeout for a PingRequest. A timeout of 0 is interpreted
     * as an infinite timeout. This property defaults to the value of the request timeout
     * property.
     *
     * @return the ping timeout
     */
    public long getPingTimeoutMillis();

    /**
     * Return the maximum size allowed for an incoming message. A value of 0
     * is interpreted as unlimited size.
     *
     * @return the maximum size of a message
     */
    public int getMaxIncomingMessageSize();

    /**
     * Return the maximum size allowed for an outgoing message. A value of 0
     * is interpreted as unlimited size.
     *
     * @return the maximum size of a message
     */
    public int getMaxOutgoingMessageSize();
    }
