/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.component.net.extend.message.request;

import com.tangosol.coherence.Component;

import com.tangosol.coherence.component.net.extend.message.Request;

import com.tangosol.net.messaging.Response;

import com.tangosol.net.topic.NamedTopic;

import com.tangosol.util.ListMap;

import java.util.Map;

/**
 * A base class for topic requests.
 *
 * @author Jonathan Knight  2024.11.26
 */
public class NamedTopicRequest
        extends Request
        implements TopicRequest
    {
    /**
     * The target of this NamedTopicRequest. This property must be set by the
     * Receiver before the run() method is called.
     */
    private transient NamedTopic<?> __m_NamedTopic;

    /**
     * The approximate maximum number of bytes transferred by
     * a partial response.
     */
    private transient long __m_TransferThreshold;

    private static ListMap<String, Class<?>> __mapChildren;

    // Static initializer
    static
        {
        __initStatic();
        }

    // Default static initializer
    private static void __initStatic()
        {
        // register child classes
        __mapChildren = new ListMap<>();
        __mapChildren.put("Status", Status.class);
        }

    public NamedTopicRequest(String sName, Component compParent, boolean fInit)
        {
        super(sName, compParent, fInit);
        }

    @Override
    protected Map<String, Class<?>> get_ChildClasses()
        {
        return __mapChildren;
        }

    // overridden to make these methods public so they can be accessed from the gRPC code.
    @Override
    public void setResponse(Response response)
        {
        super.setResponse(response);
        }

    // overridden to make these methods public so they can be accessed from the gRPC code.
    @Override
    public Response getResponse()
        {
        return super.getResponse();
        }

    public void setNamedTopic(NamedTopic<?> topic)
        {
        __m_NamedTopic = topic;
        }

    public NamedTopic<?> getNamedTopic()
        {
        return __m_NamedTopic;
        }

    public long getTransferThreshold()
        {
        return __m_TransferThreshold;
        }

    public void setTransferThreshold(long threshold)
        {
        __m_TransferThreshold = threshold;
        }
    }
