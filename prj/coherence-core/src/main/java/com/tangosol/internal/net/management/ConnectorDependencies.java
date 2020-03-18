/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.management;

/**
 * The ConnectionDependencies interface provides a Connector object with its external dependencies.
 *
 * @author der  2011.07.10
 * @since Coherence 12.1.2
 */
public interface ConnectorDependencies
    {
    /**
     * Return the type of refresh policy the connector will use; one of the
     * REFRESH_* enumerated values.
     *
     * @return the specified policy
     */
    public int getRefreshPolicy();

    /**
     * Sets the type of refresh policy the connector will use; one of the
     * REFRESH_* enumerated values.
     *
     * @param sPolicy  the specified policy in String format
     *
     * @return this object
     */
    public DefaultConnectorDependencies setRefreshPolicy(String sPolicy);

    /**
     * Returns the duration which the managing server waits for a response from
     * a remote node when refreshing MBean information.
     *
     * @return refresh request timeout milliseconds.
     */
    public long getRefreshRequestTimeoutMillis();

    /**
     * Return the number of milliseconds that the managing server can use a model
     * snapshot before a refresh is required.
     *
     * @return refresh timeout milliseconds.
     */
    public long getRefreshTimeoutMillis();

    /**
     * Set the number of milliseconds that the managing server can use a model
     * snapshot before a refresh is required.
     *
     * @param cMillis  number of milliseconds that the managing server can use a model
     * snapshot before a refresh is required.
     *
     * @return this object
     */
    public ConnectorDependencies setRefreshTimeoutMillis(long cMillis);

    /**
     * Refresh policy where the information will be refreshed when the object is
     * requested and the expiry time has passed.
     */
    public static final int REFRESH_EXPIRED = 0;

    /**
     * Refresh policy where the information will be refreshed when any cache miss
     * has occurred and the object has been used since the prior asynchronous fetch.
     */
    public static final int REFRESH_AHEAD = 1;

    /**
     * Refresh policy where the information will be refreshed after the expiry time
     * has occurred and the object is requested. The request that initiates the
     * fetch will receive the information from the prior fetch.
     *
     */
    public static final int REFRESH_BEHIND  = 2;

    /**
     * Refresh policy where the information will be refreshed only when the MBeans
     * are queried. This policy requires the WrapperMBeanServer to be utilized.
     * Note: this policy does not use a time based expiry. Information ONLY be
     * refreshed when an MBean name is returned as part of a query.
     */
    public static final int REFRESH_ONQUERY = 3;
    }
