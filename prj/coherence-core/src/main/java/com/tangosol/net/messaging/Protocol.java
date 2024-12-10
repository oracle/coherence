/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.messaging;


/**
* A Protocol is a binding between a unique name, version information, and a
* set of {@link Message} types. It is used to describe the types of
* {@link Message} objects (the "dialect", so to speak) that can be exchanged
* between two endpoints through a {@link Channel} via a {@link Connection}.
* <p>
* Before a Connection can be created or accepted, one or more Protocol
* instances must be registered with the client and server-side
* {@link ConnectionManager}. During Connection establishment, the client's
* {@link ConnectionInitiator} sends information about each registered
* Protocol. A compatable set of Protocol objects (or superset) must be
* registered with server's ConnectionManager in order for the Connection
* to be accepted.
* <p>
* All Channel implementations must be fully thread-safe.
*
* @author jh  2006.04.11
*
* @see Channel
* @see Connection
* @see ConnectionAcceptor
* @see ConnectionInitiator
* @see ConnectionManager
* @see Protocol
*
* @since Coherence 3.2
*/
public interface Protocol
    {
    /**
    * Return the unique name of this Protocol.
    * <p>
    * This name serves as a unique identifier for the Protocol; therefore,
    * only a single instance of a Protocol with a given name may be
    * registered with a ConnectionManager.
    *
    * @return the Protocol name
    */
    public String getName();

    /**
    * Determine the newest protocol version supported by this Protocol.
    *
    * @return the version number of this Protocol
    */
    public int getCurrentVersion();

    /**
    * Determine the oldest protocol version supported by this Protocol.
    *
    * @return the oldest protocol version that this Protocol object supports
    */
    public int getSupportedVersion();

    /**
    * Return a MessageFactory that can be used to create Message objects for
    * the specified version of this Protocol.
    *
    * @param nVersion  the desired Protocol version
    *
    * @return a MessageFactory that can create Message objects for the
    *         specified version of this Protocol
    *
    * @throws IllegalArgumentException if the specified protocol version is
    *         not supported by this Protocol
    */
    public MessageFactory getMessageFactory(int nVersion);


    // ----- MessageFactory inner interface ---------------------------------

    /**
    * A MessageFactory is a factory for {@link Message} objects.
    *
    * @author jh  2006.04.04
    *
    * @see Message
    *
    * @since Coherence 3.2
    */
    public interface MessageFactory
        {
        /**
        * Return the Protocol for which this MessageFactory creates Message
        * objects.
        *
        * @return the Protocol associated with this MessageFactory
        */
        public Protocol getProtocol();

        /**
        * Return the Protocol version supported by this MessageFactory.
        *
        * @return the Protocol version associated with this MessageFactory
        */
        public int getVersion();

        /**
        * Create a new Message object of the specified type.
        *
        * @param nType  the type identifier of the Message class to
        *               instantiate
        *
        * @return the new Message object
        *
        * @throws IllegalArgumentException if the specified type is unknown
        *         to this MessageFactory
        */
        public Message createMessage(int nType);
        }
    }
