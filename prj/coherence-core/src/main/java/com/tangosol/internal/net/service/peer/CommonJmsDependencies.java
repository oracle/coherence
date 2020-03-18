/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.peer;

import com.tangosol.config.annotation.Injectable;

import com.tangosol.util.Base;

import javax.jms.DeliveryMode;
import javax.jms.Message;

/**
 * The CommonJmsDependencies class provides a default implementation of JmsDependencies.
 *
 * @author pfm 2011.06.27
 * @since Coherence 12.1.2
 */
public class CommonJmsDependencies
        implements JmsDependencies
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a CommonJmsDependencies object.
     */
    public CommonJmsDependencies()
        {
        this(null);
        }

    /**
     * Construct a CommonJmsDependencies object, copying the values from the specified
     * JmsDependencies object.
     *
     * @param deps  the dependencies to copy, or null
     */
    public CommonJmsDependencies(JmsDependencies deps)
        {
        if (deps != null)
            {
            m_nMessageDeliveryMode        = deps.getMessageDeliveryMode();
            m_cMessageExpiration          = deps.getMessageExpiration();
            m_nMessagePriority            = deps.getMessagePriority();
            m_sQueueConnectionFactoryName = deps.getQueueConnectionFactoryName();
            m_sQueueName                  = deps.getQueueName();
            }
        }

    // ----- CommonJmsDependencies methods ----------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMessageDeliveryMode()
        {
        return m_nMessageDeliveryMode;
        }

    /**
     * Set the message delivery mode.
     *
     * @param nDeliveryMode  the message delivery mode
     */
    @Injectable("message-delivery-mode")
    public void setMessageDeliveryMode(int nDeliveryMode)
        {
        m_nMessageDeliveryMode = nDeliveryMode;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getMessageExpiration()
        {
        return m_cMessageExpiration;
        }

    /**
     * Set the message expiration.
     *
     * @param cMillis  the message expiration
     */
    @Injectable("message-expiration")
    public void setMessageExpiration(long cMillis)
        {
        m_cMessageExpiration = cMillis;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMessagePriority()
        {
        return m_nMessagePriority;
        }

    /**
     * Set the message priority.
     *
     * @param nPriority  the message priority
     */
    @Injectable("message-priority")
    public void setMessagePriority(int nPriority)
        {
        m_nMessagePriority = nPriority;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getQueueConnectionFactoryName()
        {
        return m_sQueueConnectionFactoryName;
        }

    /**
     * Set the queue connection factory name.
     *
     * @param sName  the queue connection factory name
     */
    @Injectable("queue-connection-factory-name")
    public void setQueueConnectionFactoryName(String sName)
        {
        m_sQueueConnectionFactoryName = sName;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getQueueName()
        {
        return m_sQueueName;
        }

    /**
     * Set the queue name.
     *
     * @param sName  the queue name
     */
    @Injectable("queue-name")
    public void setQueueName(String sName)
        {
        m_sQueueName = sName;
        }

    /**
     * Validate the dependencies.
     *
     * @return this object
     */
    public CommonJmsDependencies validate()
        {
        Base.checkNotEmpty(getQueueConnectionFactoryName(), "QueueConnectionFactoryName");
        Base.checkNotEmpty(getQueueName(), "QueueName");

        return this;
        }

    // ----- Object methods -------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
        {
        return "CommonJmsDependencies" + "{MessageDeliveryMode=" + getMessageDeliveryMode() + ", MessageExpiration="
               + getMessageExpiration() + ", MessagePriority=" + getMessagePriority() + ", QueueConnectionFactoryName="
               + getQueueConnectionFactoryName() + ", QueueName=" + getQueueName() + "}";
        }

    // ----- data fields and constants --------------------------------------

    /**
     * The message delivery mode.
     */
    private int m_nMessageDeliveryMode = DeliveryMode.NON_PERSISTENT;

    /**
     * The message expiration.
     */
    private long m_cMessageExpiration = Message.DEFAULT_TIME_TO_LIVE;

    /**
     * The message priority.
     */
    private int m_nMessagePriority = Message.DEFAULT_PRIORITY;

    /**
     * The queue connection factory name.
     */
    private String m_sQueueConnectionFactoryName = "";

    /**
     * The queue name.
     */
    private String m_sQueueName = "";
    }
