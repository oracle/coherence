/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.peer.initiator;

import com.tangosol.config.annotation.Injectable;

import com.tangosol.internal.net.service.peer.DefaultPeerDependencies;

import com.tangosol.util.Base;

/**
 * The DefaultInitiatorDependencies class provides a default implementation of
 * InitiatorDependencies.
 *
 * @author pfm 2011.06.27
 * @since Coherence 12.1.2
 */
public class DefaultInitiatorDependencies
        extends DefaultPeerDependencies
        implements InitiatorDependencies
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a DefaultInitiatorDependencies object.
     */
    public DefaultInitiatorDependencies()
        {
        this(null);
        }

    /**
     * Construct a DefaultInitiatorDependencies object, copying the values from the
     * specified InitiatorDependencies object.
     *
     * @param deps  the dependencies to copy, or null
     */
    public DefaultInitiatorDependencies(InitiatorDependencies deps)
        {
        super(deps);

        if (deps != null)
            {
            m_cConnectTimeoutMillis     = deps.getConnectTimeoutMillis();
            m_cRequestSendTimeoutMillis = deps.getRequestSendTimeoutMillis();
            }
        }

    // ----- DefaultInitiatorDependencies methods --------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public long getConnectTimeoutMillis()
        {
        long cTimeout = m_cConnectTimeoutMillis;

        if (cTimeout == 0)
            {
            m_cConnectTimeoutMillis = cTimeout = getRequestTimeoutMillis();
            }

        return cTimeout;
        }

    /**
     * Set the connect timeout.
     *
     * @param cMillis  the connect timeout
     */
    @Injectable("connect-timeout")
    public void setConnectTimeoutMillis(long cMillis)
        {
        m_cConnectTimeoutMillis = cMillis;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public DefaultInitiatorDependencies validate()
        {
        super.validate();

        Base.azzert(getConnectTimeoutMillis() >= 0, "Connect timeout cannot be less than 0");
        Base.azzert(getRequestSendTimeoutMillis() >= 0, "Request send timeout cannot be less than 0");

        return this;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getRequestSendTimeoutMillis()
        {
        long cTimeout = m_cRequestSendTimeoutMillis;

        if (cTimeout == 0)
            {
            m_cRequestSendTimeoutMillis = cTimeout = getRequestTimeoutMillis();
            }

        return cTimeout;
        }

    /**
     * Set the request send timeout.
     *
     * @param cMillis  the request send timeout
     */
    @Injectable("outgoing-message-handler/request-timeout")
    public void setRequestSendTimeoutMillis(long cMillis)
        {
        m_cRequestSendTimeoutMillis = cMillis;
        }

    // ----- Object methods -------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
        {
        return super.toString() + "{ConnectTimeoutMillis=" + getConnectTimeoutMillis() + ", RequestSendTimeoutMillis="
               + getRequestSendTimeoutMillis() + "}";
        }

    // ----- data fields and constants --------------------------------------

    /**
     * The connect timeout.
     */
    private long m_cConnectTimeoutMillis;

    /**
     * The request send timeout.
     */
    private long m_cRequestSendTimeoutMillis;
    }
