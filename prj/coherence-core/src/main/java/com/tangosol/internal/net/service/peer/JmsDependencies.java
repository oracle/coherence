/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.peer;

/**
* The JmsDependencies interface provides common JMS external dependencies.
*
* @author pfm  2011.06.27
* @since Coherence 12.1.2
*/
public interface JmsDependencies
    {
    /**
     * Return the message delivery mode.
     *
     * @return the message delivery mode
     */
    public int getMessageDeliveryMode();

    /**
     * Return the message expiration.
     *
     * @return the message expiration
     */
    public long getMessageExpiration();

    /**
     * Return the message priority.
     *
     * @return the message priority
     */
    public int getMessagePriority();

    /**
     * Return the queue connection factory name.
     *
     * @return the queue connection factory name
     */
    public String getQueueConnectionFactoryName();

    /**
     * Return the queue name.
     *
     * @return the queue name
     */
    public String getQueueName();
    }
