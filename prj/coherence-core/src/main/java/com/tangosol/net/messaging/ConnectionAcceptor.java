/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.messaging;


import java.util.Collection;


/**
* A ConnectionAcceptor represents a {@link ConnectionManager} running on a
* server, and as a server, it is responsible for accepting a connection
* request initiated by a {@link ConnectionInitiator}.
* <p>
* Before a connection can be accepted, the ConnectionAcceptor must be started
* using the {@link #start()} method. Calling this method allocates any
* necessary resources and transitions the ConnectionAcceptor to the running
* state. The ConnectionAcceptor will then accept new connections, which are
* represented by {@link Connection} object. The ConnectionAcceptor maintains
* references to accepted Connection objects until they are closed or the
* ConnectionAcceptor is {@link #shutdown gracefully} or
* {@link #stop forcibly} terminated. Terminating a ConnectionAcceptor also
* closes all accepted Connection objects.
* <p>
* All ConnectionAcceptor implementations must be fully thread-safe.
*
* @author jh  2006.03.23
*
* @see Connection
* @see ConnectionInitiator
* @see ConnectionListener
* @see ConnectionManager
*
* @since Coherence 3.2
*/
public interface ConnectionAcceptor
        extends ConnectionManager
    {
    /**
    * Return the collection of open Connection objects accepted by this
    * ConnectionAcceptor.
    * <p>
    * The client should assume that the returned collection is an immutable
    * snapshot of the actual set of Connection objects maintained by this
    * ConnectionAcceptor.
    *
    * @return the collection of open Connection objects
    */
    public Collection getConnections();

    /**
    * Return the ConnectionFilter that will be used by this ConnectionAcceptor
    * to filter connection attempts.
    *
    * @return the connection filter used to filter connection attempts
    */
    public ConnectionFilter getConnectionFilter();

    /**
    * Configure the ConnectionFilter that will be used by this
    * ConnectionAcceptor to filter connection attempts.
    *
    * @param filter  the connection filter to use
    */
    public void setConnectionFilter(ConnectionFilter filter);
    }
