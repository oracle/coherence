/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.component.net.extend.message.request;

import com.tangosol.coherence.Component;
import com.tangosol.coherence.component.net.extend.proxy.TopicPublisherProxy;

/**
 * A base class for topic publisher requests.
 *
 * @author Jonathan Knight  2024.11.26
 */
public class TopicPublisherRequest
        extends NamedTopicRequest
    {
    /**
     * The target publisher for this request.
     */
    protected TopicPublisherProxy m_publisher;

    // ----- constructors ---------------------------------------------------

    public TopicPublisherRequest(String sName, Component compParent, boolean fInit)
        {
        super(sName, compParent, fInit);
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Obtain the target {@link TopicPublisherProxy} for this request.
     *
     * @return the target {@link TopicPublisherProxy} for this request.
     */
    public TopicPublisherProxy getPublisherConnector()
        {
        return m_publisher;
        }

    /**
     * Set the target {@link TopicPublisherProxy} for this request.
     *
     * @param connector  the target {@link TopicPublisherProxy} for this request
     */
    public void setPublisherConnector(TopicPublisherProxy connector)
        {
        m_publisher = connector;
        }
    }
