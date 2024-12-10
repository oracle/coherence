/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.messaging;


import com.tangosol.io.Serializer;

import java.util.Map;

import javax.security.auth.Subject;


/**
* A Channel is a communication construct that allows one or more threads to
* send and receive {@link Message} objects via a {@link Connection}.
* <p>
* Channel objects are created from a Connection. Once created, a Channel can
* be used to:
* <ul>
*   <li>asynchronously {@link #send send} a Message</li>
*   <li>asynchronously {@link #send(Request) send} a Request</li>
*   <li>synchronously {@link #request(Request) send} a Request</li>
*   <li>asynchronously {@link #getReceiver receive} a Message</li>
* </ul>
* Once a Channel has been {@link #close closed}, any attempt to send a
* Message using the Channel may result in an exception.
* <p>
* All Channel implementations must be fully thread-safe.
*
* @author jh  2006.03.23
*
* @see Connection
* @see Message
* @see Request
* @see Response
*
* @since Coherence 3.2
*/
public interface Channel
    {
    /**
    * Return the Connection that created this Channel.
    *
    * @return the Connection that created this Channel
    */
    public Connection getConnection();

    /**
    * Return the unique identifier for this Channel.
    * <p>
    * The returned identifier is only unique among Channel objects created
    * from the same underlying Connection. In other words, Channel objects
    * created by different Connection objects may have the same unique
    * identifier, but Channel objects created by the same Connection cannot.
    *
    * @return a unique integer identifier for this Channel
    */
    public int getId();

    /**
    * Return true if this Channel is open.
    *
    * @return true if this Channel is open
    */
    public boolean isOpen();

    /**
    * Close the Channel and reclaim all resources held by the Channel.
    * <p>
    * When this method is invoked, it will not return until Message
    * processing has been shut down in an orderly fashion. This means that
    * the {@link Receiver} object associated with this Channel (if any) have
    * finished processing and that all pending requests are completed or
    * canceled. If the Receiver is processing a Message at the time when
    * close is invoked, all the facilities of the Channel must remain
    * available until it finishes.
    * <p>
    * If the Channel is not open, calling this method has no effect.
    */
    public void close();

    /**
    * Return the MessageFactory used create Message objects that may be sent
    * through this Channel over the underlying Connection.
    *
    * @return the MessageFactory for this Channel
    */
    public Protocol.MessageFactory getMessageFactory();

    /**
    * Return the {@link Serializer} used to serialize and deserialize payload
    * objects carried by Message objects sent through this Channel.
    *
    * @return the Serializer for this Channel
    */
    public Serializer getSerializer();

    /**
    * Return the optional Receiver that processes unsolicited Message objects
    * sent through this Channel over the underlying Connection.
    *
    * @return the Receiver for this Channel or null if a Receiver has not
    *         been associated with this Channel
    */
    public Receiver getReceiver();

    /**
    * Return the optional Subject associated with this Channel.
    * <p>
    * If a Subject is associated with this Channel, any operation performed
    * upon receipt of a Message sent through this Channel will be done on
    * behalf of the Subject.
    *
    * @return the Subject associated with this Channel
    */
    public Subject getSubject();

    /**
    * Return the object bound with the specified name to this Channel, or
    * null if no object is bound with that name.
    *
    * @param sName  the name with which the object was bound
    *
    * @return the object bound with the given name or null if no such binding
    *         exists
    */
    public Object getAttribute(String sName);

    /**
    * Return the map of Channel attributes.
    * <p>
    * The keys of the map are the names with which the corresponding values
    * have been bound to the Channel.
    * <p>
    * The client should assume that the returned map is an immutable snapshot
    * of the actual map of attribute objects maintained by this Connection.
    *
    * @return a map of attributes bound to this Channel
    */
    public Map getAttributes();

    /**
    * Bind an object to the specified name in this Channel.
    * <p>
    * If an object is already bound to the specified name, it is replaced
    * with the given object.
    * <p>
    * Channel attributes are local to the binding peer's Channel. In other
    * words, attributes bound to this Channel object will not be bound to the
    * peer's Channel object.
    *
    * @param sName   the name with which to bind the object
    * @param oValue  the object to bind
    *
    * @return the object that the newly bound object replaced (if any)
    */
    public Object setAttribute(String sName, Object oValue);

    /**
    * Unbind the object that was bound with the specified name to this
    * Channel.
    *
    * @param sName  the name with which the object was bound
    *
    * @return the object that was unbound
    */
    public Object removeAttribute(String sName);

    /**
    * Asynchronously send a Message to the peer endpoint through this Channel
    * over the underlying Connection.
    *
    * @param message  the Message to send
    */
    public void send(Message message);

    /**
    * Asynchronously send a Request to the peer endpoint through this Channel
    * over the underlying Connection.
    *
    * @param request  the Request to send
    *
    * @return a Status object representing the asynchronous Request
    */
    public Request.Status send(Request request);

    /**
    * Synchronously send a Request to the peer endpoint through this Channel
    * over the underlying Connection and return the result of processing the
    * Request.
    *
    * @param request  the Request to send
    *
    * @return the result sent by the peer
    *
    * @throws RuntimeException if an error or exception occurs while
    *         processing the Request.
    * @throws RuntimeException if the Request is cancelled, a timeout occurs,
    *         or the waiting thread is interrupted
    */
    public Object request(Request request);

    /**
    * Synchronously send a Request to the peer endpoint through this Channel
    * over the underlying Connection and return the result of processing the
    * Request.
    *
    * @param request  the Request to send
    * @param cMillis  the number of milliseconds to wait for the result;
    *                 pass zero to block the calling thread indefinitely
    *
    * @return the result sent by the peer
    *
    * @throws RuntimeException if an error or exception occurs while
    *         processing the Request.
    * @throws RuntimeException if the Request is cancelled, a timeout occurs,
    *         or the waiting thread is interrupted
    */
    public Object request(Request request, long cMillis);

    /**
    * Return the outstanding Request with the given identifier or null if no
    * such Request exists.
    * <p>
    * This method can be used during Response execution to correlate the
    * Response with the Request for which the Response was sent.
    *
    * @param lId  the unique identifer of the outstanding Request
    *
    * @return the outstanding Request with the given identifer or null if no
    *         such Request exists
    */
    public Request getRequest(long lId);


    // ----- Receiver inner interface ---------------------------------------

    /**
    * A Receiver processes unsolicited Message objects sent via any number of
    * Channel objects.
    * <p>
    * A Receiver acts as a server-side proxy, in that it can be registered
    * with a ConnectionAcceptor, it can be looked up, and Channels from
    * multiple clients can be established to it. In this sense, the Receiver
    * represents server-side state shared across any number of client
    * Channels, and thus provides an efficient mechanism for demultiplexing
    * multi-client communication into a shared service proxy, and locating
    * state that is shared across all of those client Channels. Conversely,
    * the Channel object represents client-specific state, allowing per-
    * client information to be efficiently managed on the server side.
    * <p>
    * While the Receiver is particularly useful as a server-side proxy, it is
    * also useful on the client, allowing a client to publish named services
    * to a server, and in the case of both named services and any other
    * Channels created by a client, it allows a client to efficiently manage
    * stateful communication and process unsolicited Message objects.
    *
    * @author cp jh 2005.04.17
    *
    * @since Coherence 3.2
    */
    public interface Receiver
        {
        /**
        * Return the name of this Receiver.
        * <p>
        * If the Receiver is registered with a ConnectionManager, the
        * registration and any subsequent accesses are by the Receiver's
        * name, meaning that the name must be unique within the domain of the
        * ConnectionManager.
        *
        * @return the Receiver name
        */
        public String getName();

        /**
        * The Protocol understood by the Receiver.
        * <p>
        * Only Channel objects with the specified Protocol can be registered
        * with this Receiver.
        *
        * @return the Protocol used by this Receiver
        */
        public Protocol getProtocol();

        /**
        * Notify this Receiver that it has been associated with a Channel.
        * This method is invoked by the Channel when a Receiver is associated
        * with the Channel.
        * <p>
        * Once registered, the Receiver will receive all unsolicited Message
        * objects sent through the Channel until the Channel is unregistered
        * or closed. Without a Receiver, the unsolicited Message objects are
        * executed with only a Channel as context; with a Receiver, the
        * Receiver is given the Message to process, and may execute the
        * Message in turn.
        *
        * @param channel  a Channel that has been associated with this
        *                 Receiver
        */
        public void registerChannel(Channel channel);

        /**
        * Called when an unsolicited (non-Response) Message is received by a
        * Channel that had been previously registered with this Receiver.
        *
        * @param message  an unsolicited Message received by a registered
        *                 Channel
        */
        public void onMessage(Message message);

        /**
        * Unregister the given Channel with this Receiver. This method is
        * invoked by the Channel when a Receiver is disassociated with the
        * Channel.
        * <p>
        * Once unregistered, the Receiver will no longer receive unsolicited
        * Message objects sent through the Channel.
        *
        * @param channel  a Channel that was disassociated with this Receiver
        */
        public void unregisterChannel(Channel channel);

        /**
        * Notify this Receiver that the Channel it was associated with has
        * been closed.
        * <p>
        * This method may be invoked after the Receiver has been unregistered.
        * <p>
        * The default implementation, provided for backwards compatibility,
        * is a no-op.
        *
        * @param channel  a Channel that has been associated with this
        *                 Receiver
        *
        *  @since 12.2.1.2.0
        */
        public default void onChannelClosed(Channel channel)
            {
            // no-op
            }
        }
    }
