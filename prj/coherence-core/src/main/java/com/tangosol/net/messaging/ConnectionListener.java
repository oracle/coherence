/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net.messaging;


import java.util.EventListener;


/**
* A ConnectionListener receives events pertaining to {@link Connection}
* objects managed by a {@link ConnectionManager}.
* <p>
* Before a ConnectionListener can start to receive events, it must be
* registered with a ConnectionManager. Once registered, the
* ConnectionListener will receive events pertaining to all Connection objects
* managed by the ConnectionManager until it is unregistered or the
* ConnectionManager is terminated.
* <p>
* Implementations are not required to be thread-safe.
*
* @author jh  2006.03.23
*
* @see Connection
* @see ConnectionManager
* @see ConnectionEvent
*
* @since Coherence 3.2
*/
public interface ConnectionListener
        extends EventListener
    {
    /**
    * Invoked after a Connection has been successfully established.
    *
    * @param evt  the {@link ConnectionEvent#CONNECTION_OPENED} event
    */
    public void connectionOpened(ConnectionEvent evt);

    /**
    * Invoked after a Connection is closed.
    *
    * @param evt  the {@link ConnectionEvent#CONNECTION_CLOSED} event
    * <p>
    * After this event is raised, any attempt to use the Connection (or any
    * Channel created by the Connection) may result in an exception.
    */
    public void connectionClosed(ConnectionEvent evt);

    /**
    * Invoked when the ConnectionManager detects that the underlying
    * communication channel has been closed by the peer, severed, or become
    * unusable.
    * <p>
    * After this event is raised, any attempt to use the Connection (or any
    * Channel created by the Connection) may result in an exception.
    *
    * @param evt  the {@link ConnectionEvent#CONNECTION_ERROR} event
    */
    public void connectionError(ConnectionEvent evt);
    }
