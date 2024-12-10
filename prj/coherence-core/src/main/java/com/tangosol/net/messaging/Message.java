/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.messaging;


/**
* Message is the root interface for all message objects sent by peer
* endpoints through a {@link Channel}.
* <p>
* Message objects are created by a {@link Protocol.MessageFactory}. A Message
* object has a type identifier that uniquely identifies the Message object
* class and is scoped to the MessageFactory that created the Message. In
* other words, Message objects with the same type identifier that were
* created by two different MessageFactory instances may be of different
* classes, but Message objects with the same type identifier that were
* created by the same MessageFactory are guaranteed to be of the same type.
*
* @author jh  2006.04.04
*
* @see Channel
* @see Protocol.MessageFactory
*
* @since Coherence 3.2
*/
public interface Message
        extends Runnable
    {
    /**
    * Return the identifier for this Message object's class.
    * <p>
    * The type identifier is scoped to the MessageFactory that created this
    * Message.
    *
    * @return an identifier that uniquely identifies this Message object's
    *         class
    */
    public int getTypeId();

    /**
    * Return the Channel through which the Message will be sent, was sent, or
    * was received.
    *
    * @return the Channel used to send or receve this this Message
    */
    public Channel getChannel();

    /**
    * Set the Channel through which the Message will be sent, was sent, or
    * was received.
    *
    * @param channel  the Channel used to send or receive this Message
    *
    * @throws IllegalStateException if the Channel has already been set
    */
    public void setChannel(Channel channel);

    /**
    * Determine if this Message should be executed in the same order as it
    * was received relative to other Messages sent through the same Channel.
    * <p>
    * Consider two Messages: M1 and M2. Say M1 is received before M2 but
    * executed on a different execute thread (for example, when the
    * ConnectionManager is configured with an execute thread pool of size
    * greater than 1). In this case, there is no way to guarantee that M1
    * will finish executing before M2. However, if M1 returns true from this
    * method, the ConnectionManager will execute M1 on its service thread,
    * thus guaranteeing that M1 will execute before M2.
    * <p>
    * In-order execution should be considered as a very advanced feature and
    * implementations that return true from this method must exercise extreme
    * caution during execution, since any delay or unhandled exceptions will
    * cause a delay or complete shutdown of the underlying ConnectionManager.
    *
    * @return true if the Message should be executed in the same order as it
    *         was received relative to other Messages
    */
    public boolean isExecuteInOrder();
    }
