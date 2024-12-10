/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.common.net;


import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;


/**
 * The SelectionService interface describes a service for selecting on channels.
 *
 * @author mf  2010.10.29
 */
public interface SelectionService
    {
    /**
     * Register a channel with the service.
     * <p>
     * If called for the same channel multiple times, the prior handler will
     * be unregistered and the new handler will be registered in its place.
     * <p>
     * The handler can be deregistered either by closing the channel, or
     * via an explicit reregistration with a <tt>null</tt> handler.
     * <p>
     * Following a (re)registration the handler will have an initial interest
     * set based on the channels full {@link SelectableChannel#validOps valid}
     * operation set.
     * <p>
     * Note that registration should be treated as asynchronous except when it is performed from
     * within a {@link Handler#onReady} callback on the same channel, in which case it will take
     * effect once the callback returns, without setting {@link Handler#OP_EAGER}.
     *
     * @param chan     the channel to monitor
     * @param handler  the handler to call when the channel is ready for
     *                 servicing
     *
     * @throws IOException if an I/O error occurs
     */
    public void register(SelectableChannel chan, Handler handler)
        throws IOException;

    /**
     * Invoke the runnable by the SelectionService. It guarantees that the
     * runnable associated with the SelectableChannel and any Handler associated
     * with the same channel will not run concurrently
     * <p>
     * If called for the same channel multiple times, the Runnables will be
     * executed sequentially in the order invoke was called.
     * <p>
     * Note there is no implied ordering between calls to {@link #register} and {@link #invoke}.
     *
     * @param chan      the channel the runnable is associated with
     * @param runnable  the Runnable to call by the SelectionService
     * @param cMillis   the delay before invocation, or 0 for none
     *
     * @throws IOException if an I/O error occurs
     */
    public void invoke(SelectableChannel chan, Runnable runnable, long cMillis)
        throws IOException;

    /**
     * Associate one (the child) with another channel (the parent).  Once associated the child channel
     * will be handled serially with respect to the parent and all other associated children.  Generally
     * this means that it will be handled on the same thread in multi-threaded SelectionService implementations.
     * <p>
     * Note that changing a channels association may cause various ordering issues if the channel was previously
     * in use by the service, or if the channel was the parent of another association.  It is generally discouraged
     * to re-associate channels.  If re-association is performed it is up to the caller to handle any potential ordering
     * issues with outstanding registrations, invocations, and children.  Any pre-existing registration will be
     * asynchronously deregistered, and any pending invocations will be asynchronously executed.
     * </p>
     *
     * @param chanParent  the parent channel, or null to remove any prior association
     * @param chanChild   the child channel to associate with parent
     *
     * @throws IOException if an I/O error occurs
     */
    public void associate(SelectableChannel chanParent, SelectableChannel chanChild)
        throws IOException;

    /**
     * Shutdown the SelectionService.
     */
    public void shutdown();

    /**
     * Handler provides a pluggable callback which is invoked when the
     * registered channel needs servicing.
     */
    public interface Handler
        {
        /**
         * Called when the channel has been selected.
         * <p>
         * The handler implementation is expected to handle all exceptions.
         * Any unchecked exception thrown from this method will result in
         * the handler being deregistered from the SelectionService, and no
         * further selection operations being performed upon the channel.
         *
         * @param nOps  the ready ops
         *
         * @return the new interest ops
         */
        public int onReady(int nOps);

        /**
         * Operation-set bit for socket-accept operations.
         *
         * @see SelectionKey#OP_ACCEPT
         */
        public static final int OP_ACCEPT = SelectionKey.OP_ACCEPT;

        /**
         * Operation-set bit for socket-connect operations.
         *
         * @see SelectionKey#OP_CONNECT
         */
        public static final int OP_CONNECT = SelectionKey.OP_CONNECT;

        /**
         * Operation-set bit for read operations.
         *
         * @see SelectionKey#OP_READ
         */
        public static final int OP_READ = SelectionKey.OP_READ;

        /**
         * Operation-set bit for write operations.
         *
         * @see SelectionKey#OP_WRITE
         */
        public static final int OP_WRITE = SelectionKey.OP_WRITE;

        /**
         * Operation-set bit indicating that it is likely that at least one of the other bits in the set are likely
         * to be satisfied soon.  This serves as a hint to the service that it may be worth re-running the handler
         * prior to waiting on the selector.  If a Handler is called eagerly this bit will be set in nOps value
         * passed to {@link #onReady} to indicate that the reason for the call.
         */
        public static final int OP_EAGER = Math.max(OP_ACCEPT, Math.max(OP_CONNECT, Math.max(OP_READ, OP_WRITE))) << 1;
        }
    }
