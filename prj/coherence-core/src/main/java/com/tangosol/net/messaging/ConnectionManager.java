/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.messaging;


import com.tangosol.net.OperationalContext;

import com.tangosol.util.Service;

import java.util.Map;


/**
* The ConnectionManager is the base SPI (Service Provider Interface) for both
* {@link ConnectionAcceptor} and {@link ConnectionInitiator} implementations.
* <p>
* Implementations of this interface use a provider-specific mechanism to
* establish a bi-directional communication channel between two endpoints,
* represented by a {@link Connection}. Some implementations restrict data
* transfer between endpoints within a single JVM, whereas others enable two
* processes to exchange data. Advanced implementations allow communication
* between processes on different machines, for example using TCP sockets or
* JMS.
* <p>
* Before a Connection can be established between a ConnectionInitiator
* (client) and ConnectionAcceptor (server), one or more Protocol instances
* must be registered with the ConnectionManager on each. During Connection
* establishment, the ConnectionInitiator sends information about each
* registered Protocol. A compatable set of Protocol instances (or superset)
* must be registered with the acceptor's ConnectionManager in order for the
* Connection to be established.
* <p>
* Establishing a Connection is assumed to be a heavyweight operation that may
* allocate significant resources within and outside the JVM. For example, a
* TCP-based implementation of this interface may implement a Connection using
* a persistent Socket connection with a remote server. However, once
* established, successive uses of the same Connection should be relatively
* lightweight. In other words, a Connection object, once opened, should
* appear to be persistent from the perspective of the user until closed.
* Additionally, underlying transports used by implementations must be both
* reliable and ordered.
* <p>
* Once a Connection is established, either client or server may open a
* {@link Channel} to a {@link Channel.Receiver} registered by its peer and
* use it to send and receive {@link Message} objects to/from the peer.
*
* @author jh  2006.03.30
*
* @see Channel
* @see Channel.Receiver
* @see Connection
* @see ConnectionAcceptor
* @see ConnectionInitiator
* @see Protocol
*
* @since Coherence 3.2
*/
public interface ConnectionManager
        extends Service
    {
    /**
    * Return the {@link OperationalContext} used by this ConnectionManager.
    *
    * @return the OperationalContext used by this ConnectionManager
    */
    public OperationalContext getOperationalContext();

    /**
    * Configure the {@link OperationalContext} used by this ConnectionManager.
    *
    * @param ctx  the OperationalContext used by this ConnectionManager
    */
    public void setOperationalContext(OperationalContext ctx);

    /**
    * Return a Protocol that was registered with this ConnectionManager.
    *
    * @param sName  the name of the registered Protocol
    *
    * @return the registered Protocol or null if a Protocol with the given
    *         name is not registered with this ConnectionManager
    */
    public Protocol getProtocol(String sName);

    /**
    * Return a map of Protocol names to Protocol objects.
    * <p>
    * The client should assume that the returned map is an immutable snapshot
    * of the actual map of Protocol objects maintained by this
    * ConnectionManager.
    *
    * @return an map of all registered Protocol objects, keyed by the
    *         Protocol name
    */
    public Map getProtocols();

    /**
    * Register a Protocol with this ConnectionManager.
    * <p>
    * This method may only be called before the ConnectionManager is started.
    *
    * @param protocol  the new Protocol to register; if the Protocol has
    *                  already been registered, this method has no effect
    *
    * @throws IllegalStateException if the ConnectionManager is running
    */
    public void registerProtocol(Protocol protocol);

    /**
    * Return a Receiver that was registered with this ConnectionManager.
    * <p>
    * The client should assume that the returned map is an immutable snapshot
    * of the actual map of Receiver objects maintained by this
    * ConnectionManager.
    *
    * @param sName  the name of the registered Receiver
    *
    * @return the registered Protocol or null if a Protocol with the given
    *         name is not registered with this ConnectionManager
    */
    public Channel.Receiver getReceiver(String sName);

    /**
    * Return a map of Receiver names to Receiver objects.
    * <p>
    * The client should assume that the returned map is an immutable snapshot
    * of the actual map of Receiver objects maintained by this
    * ConnectionManager.
    *
    * @return an map of all registered Receiver objects, keyed by the
    *         Receiver name
    */
    public Map getReceivers();

    /**
    * Register a Receiver that will received unsolicited Message objects sent
    * through Channel objects associated with the Receiver name and Protocol.
    * <p>
    * This method may only be called before the ConnectionManager is started.
    *
    * @param receiver  the new Receiver to register; if the Receiver has
    *                  already been registered, this method has no effect
    *
    * @throws IllegalStateException if the ConnectionManager is running
    */
    public void registerReceiver(Channel.Receiver receiver);

    /**
    * Return the Codec that will be used to encode and decode Messages sent
    * through Connections managed by this ConnectionManager.
    *
    * @return the configured Codec
    */
    public Codec getCodec();

    /**
    * Configure the Codec that will be used to encode and decode Messages
    * sent through Connections managed by this ConnectionManager.
    *
    * @param codec  the optional Codec associated with Connection objects
    *
    * @throws IllegalStateException if the ConnectionManager is running
    */
    public void setCodec(Codec codec);

    /**
    * Register a ConnectionListener that will receive events pertaining to
    * the Connection objects managed by this ConnectionManager.
    *
    * @param listener  the new ConnectionListener to register; if the
    *                  listener has already been registered, this method has
    *                  no effect
    */
    public void addConnectionListener(ConnectionListener listener);

    /**
    * Unregister a ConnectionListener from this ConnectionManager.
    * <p>
    * After a ConnectionListener is removed, it will no longer receive events
    * pertaining to the Connection objects managed by this ConnectionManager.
    *
    * @param listener  the ConnectionListener to deregister; if the listener
    *                  has not previously been registered, this method has no
    *                  effect
    */
    public void removeConnectionListener(ConnectionListener listener);
    }
