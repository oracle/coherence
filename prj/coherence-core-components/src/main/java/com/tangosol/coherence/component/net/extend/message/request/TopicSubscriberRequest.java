/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.component.net.extend.message.request;

import com.tangosol.coherence.Component;
import com.tangosol.internal.net.topic.NamedTopicSubscriber;
import com.tangosol.internal.net.topic.SubscriberConnector;
import com.tangosol.net.topic.Subscriber;
import com.tangosol.util.Binary;

/**
 * A base class for topic subscriber requests.
 *
 * @author Jonathan Knight  2024.11.26
 */
public class TopicSubscriberRequest
        extends NamedTopicRequest
    {
    /**
     * The target subscriber for this request.
     */
    protected SubscriberConnector.ConnectedSubscriber<Binary> m_subscriber;

    // ----- constructors ---------------------------------------------------

    public TopicSubscriberRequest(String sName, Component compParent, boolean fInit)
        {
        super(sName, compParent, fInit);
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Obtain the target {@link SubscriberConnector.ConnectedSubscriber} for this request.
     *
     * @return the target {@link SubscriberConnector.ConnectedSubscriber} for this request.
     */
    public SubscriberConnector.ConnectedSubscriber<Binary> getSubscriber()
        {
        return m_subscriber;
        }

    /**
     * Set the target {@link SubscriberConnector.ConnectedSubscriber} for this request.
     *
     * @param connector  the target {@link SubscriberConnector.ConnectedSubscriber} for this request
     */
    public void setSubscriber(SubscriberConnector.ConnectedSubscriber<Binary> connector)
        {
        m_subscriber = connector;
        }
    }
