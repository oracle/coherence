/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.component.net.extend;

import com.tangosol.net.messaging.Channel;
import com.tangosol.net.messaging.Request;

/**
 * A class that can use a {@link RequestSender} to
 * send a {@link Request}.
 *
 * @author Jonathan Knight  2024.11.26
 */
public interface RequestSendingProxy
    {
    /**
     * Return the {@link Channel} the proxy is using.
     *
     * @return the {@link Channel} the proxy is using
     */
    Channel getChannel();

    /**
     * Obtain the {@link MessageFactory} to use to create
     * messages and requests.
     *
     * @return the {@link MessageFactory} to use to create
     *         messages and requests
     */
    MessageFactory getMessageFactory();

    /**
     * Set the {@link RequestSender} for this receiver.
     *
     * @param sender  the {@link RequestSender} for this receiver
     */
    void setRequestSender(RequestSender sender);

    /**
     * Return the {@link RequestSender} for this receiver.
     *
     * @return the {@link RequestSender} for this receiver
     */
    RequestSender getRequestSender();

    // ----- inner interface: RequestSender ---------------------------------

    /**
     * A class that can send a {@link Request}.
     */
    interface RequestSender
        {
        /**
         * Synchronously send a {@link Request} to the peer endpoint through this Channel
         * over the underlying Connection and return the result of processing the
         * Request.
         *
         * @param request  the {@link Request} to send
         * @param <R>      the expected response type
         *
         * @return the result sent by the peer
         *
         * @throws RuntimeException if an error or exception occurs while
         *         processing the Request.
         * @throws RuntimeException if the Request is cancelled, a timeout occurs,
         *         or the waiting thread is interrupted
         */
        <R> R request(Request request);
        }
    }
