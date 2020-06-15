/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.memcached.server;

import com.tangosol.coherence.memcached.Request;

import java.io.IOException;

import java.nio.channels.SocketChannel;

import java.util.List;

/**
 * Memcached Connection.
 *
 * @author bb 2013.05.01
 *
 * @since Coherence 12.1.3
 */
public interface Connection
    {
    /**
     * Get the underlying socket channel.
     *
     * @return socket channel
     */
    public SocketChannel getChannel();

    /**
     * Parse the data from the underlying channel and return a list of memcached requests.
     *
     * @return the List of memcached requests or null if the request is not complete
     *
     * @throws IOException
     */
    public List<Request> read()
            throws IOException;

    /**
     * Write the responses on the underlying channel.
     *
     * @return SelectionService's operation-set bit for write operations if there
     *         are pending data to be written or 0 to de-register for write operations.
     *
     * @throws IOException
     */
    public int write()
            throws IOException;

    /**
     * Set the FlowControl.
     *
     * @param flowControl  the Connection FlowControl
     */
    public void setFlowControl(ConnectionFlowControl flowControl);

    /**
     * Return the Connection Id.
     *
     * @return the connection id
     */
    public int getId();

    // ----- inner interface: ConnectionFlowControl -------------------------

    /**
     * ConnectionFlowControl interface to control reading/writing requests/responses
     * from the connection. This is used to do flow control of traffic.
     */
    public interface ConnectionFlowControl
        {
        /**
         * Enable SelectionService thread to start doing write operations. Only needed
         * if the responses couldn't be sent back by the service thread.
         */
        public void resumeWrites();

        /**
         * Pause reading new requests from the connection.
         */
        public void pauseReads();

        /**
         * Resume reading requests from the connection.
         */
        public void resumeReads();

        /**
         * Check if reads have been paused.
         *
         * @return true iff reads are paused.
         */
        public boolean isReadPaused();
        }
    }