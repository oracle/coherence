/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.component.net.extend.message.request;

import com.tangosol.net.messaging.Channel;
import com.tangosol.net.messaging.Message;
import com.tangosol.net.messaging.Response;

/**
 * A common interface for topic service requests.
 *
 * @author Jonathan Knight  2024.11.26
 */
public interface TopicRequest
        extends Message
    {
    /**
     * Set the {@link Channel} for the request.
     *
     * @param channel the {@link Channel} for the request
     */
    void setChannel(Channel channel);

    /**
     * Set the {@link Response}
     *
     * @param response the {@link Response} to use
     */
    void setResponse(Response response);

    /**
     * Return the {@link Response}
     *
     * @return the {@link Response} to used
     */
    Response getResponse();
    }
