/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.messaging;


import com.tangosol.util.UUID;

import java.net.URI;

import java.util.Collection;

import javax.security.auth.Subject;


/**
* A Connection represents a bi-directional communication channel for
* exchanging {@link Message} objects between two endpoints.
* <p>
* A Connection is created by a {@link ConnectionInitiator} running on a
* client. Before a Connection can be established, the server must initialize
* and start a
* {@link ConnectionAcceptor}. Creating the Connection results in a handshake
* between the client and the server (including Protocol negotiation); if the
* handshake is successful, the Connection will be transitioned to the open
* state.
* <p>
* A Connection itself is not used to send and receive Message objects.
* Rather, one or more {@link Channel} objects may be created from the
* Connection and used to exchange Message objects of a known {@link Protocol}
* with a peer. A Channel may be  {@link #openChannel opened} to a named
* {@link Channel.Receiver} registered by the peer. Alternatively, an
* "anonymous" Channel (a back-Channel) may be {@link #createChannel created}
* by one peer and returned to the other, where it must be
* {@link #acceptChannel accepted} before it can be used.
* <p>
* Once finished with the Connection, an application should release all
* resources held by the Connection by calling the {@link #close} method.
* Closing a Connection also closes all Channel objects created by the
* Connection and renders the Connection unusable. Attempting to use a closed
* Connection or any Channel created by a closed Connection may result in an
* exception.
* <p>
* All Connection implementations must be fully thread-safe.
*
* @author jh  2006.03.22
*
* @see Channel
* @see Channel.Receiver
* @see ConnectionAcceptor
* @see ConnectionInitiator
* @see Message
* @see Protocol
*
* @since Coherence 3.2
*/
public interface Connection
    {
    /**
    * Return the ConnectionManager that created or accepted this Connection.
    *
    * @return the ConnectionManager
    */
    public ConnectionManager getConnectionManager();

    /**
    * Return the unique identifier of this Connection.
    *
    * @return the unique identifier of this Connection or null if the
    *         Connection has not been accepted
    */
    public UUID getId();

    /**
    * Return the unique identifier of the peer to which this Connection
    * object is connected.
    *
    * @return the unique identifier of the peer or null if the Connection
    *         has not been accepted
    */
    public UUID getPeerId();

    /**
    * Return true if this Connection is open.
    * <p>
    * A Connection can only be used to exchange data when it is open.
    *
    * @return true if this Connection is open
    */
    public boolean isOpen();

    /**
    * Close the Connection.
    * <p>
    * Closing a Connection also reclaims all resources held by the
    * Connection, so there is no need to close Channel objects of a closed
    * Connection.
    * <p>
    * If the Connection is already closed, calling this method has no effect.
    */
    public void close();

    /**
    * Create a Channel using a specific Protocol through this Connection to a
    * named Receiver on the other end of the Connection, optionally providing
    * a Subject to indicate the identity that will be utilizing the Channel,
    * and optionally providing a Receiver that will process unsolicited
    * Message objects on this end of the Channel.
    * <p>
    * Conceptually, this is how a Channel is established to an existing named
    * "service" (e.g. a Receiver) on the peer; note that either peer can
    * register named services and either peer can use this method to find a
    * named service on its peer.
    *
    * @param protocol  the Protocol that will be used to communicate through
    *                  the Channel; the Protocol is used to verify that the
    *                  Receiver on the peer with the specified name is
    *                  capable of communicating using that Protocol
    * @param sName     the name that the Receiver was registered with, on the
    *                  other end of this Connection; a Receiver with the
    *                  specified name must have been registered with the
    *                  peer's ConnectionManager prior to calling this method
    *                  (see {@link ConnectionManager#registerReceiver}).
    * @param loader    the ClassLoader used to by the newly opened Channel to
    *                  load classes and resources; if null, the context
    *                  ClassLoader of the ConnectionManager that created this
    *                  Connection will be used
    * @param receiver  an optional Receiver to associate with this Channel
    *                  that will process any unsolicited Message objects sent
    *                  back through the Channel by the peer
    * @param subject   an optional Subject to associate with this Channel; if
    *                  specified, any operation performed upon receipt of a
    *                  Message sent using the returned Channel will be done
    *                  on behalf of the specified Subject
    *
    * @return a new Channel object
    *
    * @throws IllegalArgumentException if the specified Protocol has not been
    *         registered with the underlying ConnectionManager
    * @throws IllegalArgumentException if a Receiver with the given name has
    *         not been registered with the peer's ConnectionManager
    * @throws IllegalArgumentException if the specified Receiver does not use
    *         the same Protocol as the one registered on the peer
    * @throws IllegalArgumentException if the specified Receiver does not use
    *         the specified Protocol
    * @throws IllegalStateException if the Connection is not open
    */
    public Channel openChannel(Protocol protocol, String sName,
            ClassLoader loader, Channel.Receiver receiver, Subject subject);

    /**
    * Create a back-Channel to expose another service to the peer.
    * <p>
    * This method is particularly useful for building a Response Message to
    * send back a new Channel that can be used by the peer. In practice, this
    * means that when a call to a stub is made, it can easily return a new
    * stub that has its own Channel; for example, a stub representing one
    * service can return a stub representing a different service.
    * <p>
    * The new Channel cannot be used until the returned URI is
    * {@link #acceptChannel accepted} by the peer.
    *
    * @param protocol  the Protocol that will be used to communicate through
    *                  the new Channel
    * @param loader    the ClassLoader used to by the newly created Channel
    *                  to load classes and resources; if null, the context
    *                  ClassLoader of the ConnectionManager that created this
    *                  Connection will be used
    * @param receiver  an optional Receiver to associate with the new Channel
    *                  that will process any unsolicited Message objects sent
    *                  back through the Channel by the peer
    *
    * @return a URI that represents the new Channel object
    *
    * @throws IllegalArgumentException if the specified Protocol has not been
    *         registered with the underlying ConnectionManager
    * @throws IllegalArgumentException if the specified Receiver does not use
    *         the specified Protocol
    * @throws IllegalStateException if the Connection is not open
    *
    * @see #acceptChannel
    */
    public URI createChannel(Protocol protocol, ClassLoader loader,
            Channel.Receiver receiver);

    /**
    * Accept a newly created back-Channel that was spawned by the peer.
    * <p>
    * Before a spawned Channel can be used to send and receive Message
    * objects, its URI must be accepted by the peer.
    *
    * @param uri       the URI of a Channel that was spawned by the peer
    * @param loader    the ClassLoader used to by the newly accepted Channel
    *                  to load classes and resources; if null, the context
    *                  ClassLoader of the ConnectionManager that created this
    *                  Connection will be used
    * @param receiver  an optional Receiver to associate with the new Channel
    *                  that will process any unsolicited Message objects sent
    *                  back through the Channel by the peer
    * @param subject   an optional Subject to associate with the new Channel;
    *                  if specified, any operation performed upon receipt of
    *                  a Message sent using the accepted Channel will be done
    *                  on behalf of the specified Subject
    *
    * @return the newly accepted Channel
    *
    * @throws IllegalArgumentException if the specified Receiver does not use
    *         the same Protocol as the spawned Channel (as described by its
    *         URI)
    * @throws IllegalStateException if the Connection is not open
    *
    * @see #createChannel
    */
    public Channel acceptChannel(URI uri, ClassLoader loader,
            Channel.Receiver receiver, Subject subject);

    /**
    * Return the open Channel object with the given identifier.
    * <p>
    * If a Channel object with the specified identifier does not exist or has
    * been closed, null is returned.
    *
    * @param nId  the unique Channel identifier
    *
    * @return the open Channel object with the specified identifer or null if
    *         no such open Channel exists
    */
    public Channel getChannel(int nId);

    /**
    * Return the collection of open Channel objects through this Connection.
    * <p>
    * The client should assume that the returned collection is an immutable
    * snapshot of the actual collection of open Channel objects maintained by
    * this Connection.
    *
    * @return the collection of open Channel objects
    */
    public Collection getChannels();
    }
