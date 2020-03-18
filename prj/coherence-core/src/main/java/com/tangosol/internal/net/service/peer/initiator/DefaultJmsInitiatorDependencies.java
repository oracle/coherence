/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.peer.initiator;

import com.tangosol.config.annotation.Injectable;

import com.tangosol.internal.net.service.peer.CommonJmsDependencies;

/**
 * The DefaultJmsInitiatorDependencies class provides a default implementation of
 * JmsInitiatorDependencies.
 *
 * @author pfm  2011.06.27
 * @since Coherence 12.1.2
 */
public class DefaultJmsInitiatorDependencies
        extends DefaultInitiatorDependencies
        implements JmsInitiatorDependencies
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a DefaultJmsInitiatorDependencies object.
     */
    public DefaultJmsInitiatorDependencies()
        {
        this(null);
        }

    /**
     * Construct a DefaultJmsInitiatorDependencies object, copying the values from the
     * specified JmsInitiatorDependencies object.
     *
     * @param deps  the dependencies to copy, or null
     */
    public DefaultJmsInitiatorDependencies(JmsInitiatorDependencies deps)
        {
        super(deps);

        m_jmsDependencies = new CommonJmsDependencies(deps);
        }

    // ----- DefaultJmsInitiatorDependencies methods ------------------------

    /**
     * Return the common JMS dependencies.
     *
     * @return the common JMS dependencies
     */
    @Injectable(".")
    public CommonJmsDependencies getCommonDependencies()
        {
        return m_jmsDependencies;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMessageDeliveryMode()
        {
        return m_jmsDependencies.getMessageDeliveryMode();
        }

    /**
     * Set the message delivery mode.
     *
     * @param nDeliveryMode  the message delivery mode
     */
    public void setMessageDeliveryMode(int nDeliveryMode)
        {
        m_jmsDependencies.setMessageDeliveryMode(nDeliveryMode);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getMessageExpiration()
        {
        return m_jmsDependencies.getMessageExpiration();
        }

    /**
     * Set the message expiration.
     *
     * @param cMillis  the message expiration
     */
    public void setMessageExpiration(long cMillis)
        {
        m_jmsDependencies.setMessageExpiration(cMillis);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMessagePriority()
        {
        return m_jmsDependencies.getMessagePriority();
        }

    /**
     * Set the message priority.
     *
     * @param nPriority  the message priority
     */
    public void setMessagePriority(int nPriority)
        {
        m_jmsDependencies.setMessagePriority(nPriority);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getQueueConnectionFactoryName()
        {
        return m_jmsDependencies.getQueueConnectionFactoryName();
        }

    /**
     * Set the queue connection factory name.
     *
     * @param sName  the queue connection factory name
     */
    public void setQueueConnectionFactoryName(String sName)
        {
        m_jmsDependencies.setQueueConnectionFactoryName(sName);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getQueueName()
        {
        return m_jmsDependencies.getQueueName();
        }

    /**
     * Set the queue name.
     *
     * @param sName  the queue name
     */
    public void setQueueName(String sName)
        {
        m_jmsDependencies.setQueueName(sName);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public DefaultJmsInitiatorDependencies validate()
        {
        super.validate();
        m_jmsDependencies.validate();

        return this;
        }

    // ----- Object methods -------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
        {
        return super.toString() + m_jmsDependencies.toString();
        }

    // ----- data fields and constants --------------------------------------

    /**
     * The common JMS dependencies used by both the JMS initiator and acceptor.
     */
    private CommonJmsDependencies m_jmsDependencies;
    }
