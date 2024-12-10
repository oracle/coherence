/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.net;

/**
* A ServiceLoad encapsulates information about the current utilization
* of a Service. It can be used to implement load balancing algorithms
* that control the distribution of clients across individual instances of a
* clustered Service.
*
* @author jh  2010.12.07
*
* @since Coherence 3.7
*/
@SuppressWarnings("rawtypes")
public interface ServiceLoad
        extends Comparable
    {
    /**
    * Return the connection count.
    *
    * @return the number of connected clients
    */
    public int getConnectionCount();

    /**
    * Return the number of connections that are pending.
    *
    * @return the number of pending connections
    */
    public int getConnectionPendingCount();

    /**
    * Return the maximum number of simultaneous connections allowed. Valid
    * values are positive integers and zero. A value of zero implies no limit.
    *
    * @return the maximum number of connections
    */
    public int getConnectionLimit();

    /**
    * Return number of daemon threads that are used to process messages.
    *
    * @return the number of daemon threads
    */
    public int getDaemonCount();

    /**
    * Return the number of daemon threads that are currently processing
    * messages.
    *
    * @return the number of active daemon threads
    */
    public int getDaemonActiveCount();

    /**
    * Return the number of messages that are queued for processing.
    *
    * @return the number of outstanding incoming messages
    */
    public int getMessageBacklogIncoming();

    /**
    * Return the number of messages that are queued for delivery.
    *
    * @return the number of outstanding outgoing messages
    */
    public int getMessageBacklogOutgoing();
    }
