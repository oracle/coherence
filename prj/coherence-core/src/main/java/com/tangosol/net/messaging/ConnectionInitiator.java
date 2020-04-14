/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.messaging;


/**
* A ConnectionInitiator represents a {@link ConnectionManager} running on a
* client, and as a client, it is responsible for initiating the connection
* process.
* <p>
* Before a connection can be established, the ConnectionInitiator must be
* started using the {@link #start()} method. Calling this method allocates
* any necessary resources and transitions the ConnectionInitiator to the
* running state. Additionally, the server endpoint must have initialized and
* started a {@link ConnectionAcceptor}. The ConnectionInitiator can then be
* used to establish a connection to the server's ConnectionAcceptor, which is
* represented by a single {@link Connection} object, obtained by calling
* {@link #ensureConnection()}. The ConnectionInitiator maintains a reference
* to the Connection object until it is closed or the ConnectionInitiator is
* {@link #shutdown gracefully} or {@link #stop forcibly} terminated.
* <p>
* All ConnectionInitiator implementations must be fully thread-safe.
*
* @author jh  2006.03.22
*
* @see Connection
* @see ConnectionAcceptor
* @see ConnectionListener
* @see ConnectionManager
*
* @since Coherence 3.2
*/
public interface ConnectionInitiator
        extends ConnectionManager
    {
    /**
    * Create a new or return the existing Connection object.
    * <p>
    * A Connection object has a one-way state transition from open to closed;
    * this method will always return an open Connection object. If the
    * previously existing Connection object has transitioned to a closed
    * state, this method will return a new Connectin object in the open
    * state.
    *
    * @return a Connection object representing a client's connection to a
    *         server
    *
    * @throws IllegalStateException if the ConnectionInitiator is not running
    */
    public Connection ensureConnection();
    }
