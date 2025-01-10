
/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

// ---- class: com.tangosol.coherence.component.net.extend.message.request.TopicServiceRequest

package com.tangosol.coherence.component.net.extend.message.request;

import com.tangosol.coherence.component.net.extend.proxy.serviceProxy.TopicServiceProxy;
import com.tangosol.io.pof.PofReader;
import com.tangosol.net.TopicService;
import com.tangosol.net.messaging.Response;
import com.tangosol.util.ListMap;

import java.util.Map;

/**
 * A base class for topic service requests.
 *
 * @author Jonathan Knight  2024.11.26
 */
public abstract class TopicServiceRequest
        extends com.tangosol.coherence.component.net.extend.message.Request
        implements TopicRequest
    {
    // ---- Fields declarations ----

    /**
     * Property TopicName
     */
    private String __m_TopicName;

    /**
     * Property TopicService
     */
    private transient TopicService __m_TopicService;

    /**
     * Property TopicService proxy
     */
    private transient TopicServiceProxy __m_TopicServiceProxy;

    /**
     * Property TransferThreshold
     */
    private transient long __m_TransferThreshold;

    private static ListMap<String, Class<?>> __mapChildren;

    static
        {
        __initStatic();
        }

    private static void __initStatic()
        {
        // register child classes
        __mapChildren = new com.tangosol.util.ListMap();
        __mapChildren.put("Status", Status.get_CLASS());
        }

    public TopicServiceRequest(String sName, com.tangosol.coherence.Component compParent, boolean fInit)
        {
        super(sName, compParent, false);
        }

    @Override
    protected void __initPrivate()
        {
        super.__initPrivate();
        }

    /**
     * This is an auto-generated method that returns the map of design time
     * [static] children.
     * <p>
     * Note: the class generator will ignore any custom implementation for this
     * behavior.
     */
    @Override
    protected Map<String, Class<?>> get_ChildClasses()
        {
        return __mapChildren;
        }

    /**
     * Return a human-readable description of this component.
     *
     * @return a String representation of this component
     */
    @Override
    protected String getDescription()
        {
        return super.getDescription() + ", TopicName=" + getTopicName();
        }

    /**
     * Getter for property TopicName.<p>
     */
    public String getTopicName()
        {
        return __m_TopicName;
        }

    /**
     * Setter for property TopicName.<p>
     */
    public void setTopicName(String sName)
        {
        __m_TopicName = sName;
        }

    /**
     * Getter for property TopicService.<p>
     */
    public TopicService getTopicService()
        {
        return __m_TopicService;
        }

    /**
     * Setter for property TopicService.<p>
     */
    public void setTopicService(TopicService service)
        {
        __m_TopicService = service;
        }

    /**
     * Getter for property TopicService proxy.<p>
     */
    public TopicServiceProxy getTopicServiceProxy()
        {
        return __m_TopicServiceProxy;
        }

    /**
     * Setter for property TopicService proxy.<p>
     */
    public void setTopicServiceProxy(TopicServiceProxy proxy)
        {
        __m_TopicServiceProxy = proxy;
        }

    /**
     * Getter for property TransferThreshold.<p>
     */
    public long getTransferThreshold()
        {
        return __m_TransferThreshold;
        }

    /**
     * Setter for property TransferThreshold.<p>
     */
    public void setTransferThreshold(long lThreshold)
        {
        __m_TransferThreshold = lThreshold;
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

    @Override
    public void readExternal(PofReader in)
            throws java.io.IOException
        {
        super.readExternal(in);
        __m_TopicName = in.readString(1);
        }

    @Override
    public void writeExternal(com.tangosol.io.pof.PofWriter out)
            throws java.io.IOException
        {
        super.writeExternal(out);
        out.writeString(1, __m_TopicName);
        }
    }
