/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.peer;

import com.tangosol.config.annotation.Injectable;

import com.tangosol.internal.net.service.DefaultServiceDependencies;

import com.tangosol.io.WrapperStreamFactory;

import com.tangosol.net.messaging.Codec;

import com.tangosol.util.ImmutableArrayList;
import com.tangosol.util.SafeLinkedList;

import java.util.List;

/**
 * The DefaultPeerDependencies class provides a default implementation of PeerDependencies.
 *
 * @author pfm 2011.05.12
 * @since Coherence 12.1.2
 */
public class DefaultPeerDependencies
        extends DefaultServiceDependencies
        implements PeerDependencies
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a DefaultPeerDependencies object.
     */
    public DefaultPeerDependencies()
        {
        this(null);
        }

    /**
     * Construct a DefaultPeerDependencies object. Copy the values from the specified
     * PeerDependencies object.
     *
     * @param deps  the dependencies to copy, or null
     */
    public DefaultPeerDependencies(PeerDependencies deps)
        {
        super(deps);

        if (deps != null)
            {
            m_codec                    = deps.getMessageCodec();
            m_listFilter               = deps.getFilterList();
            m_cPingIntervalMillis      = deps.getPingIntervalMillis();
            m_cPingTimeoutMillis       = deps.getPingTimeoutMillis();
            m_cbMaxIncomingMessageSize = deps.getMaxIncomingMessageSize();
            m_cbMaxOutgoingMessageSize = deps.getMaxOutgoingMessageSize();
            }
        else
            {
            // COH-13713 Peer's default request timeout is 30 secs
            super.setRequestTimeoutMillis(30000);
            }
        }

    // ----- DefaultPeerDependencies methods --------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    @Injectable("outgoing-message-handler/request-timeout")
    public void setRequestTimeoutMillis(long cMillis)
        {
        // NOTE: we override the super-class method here to redefine
        // the request timeout as it's part of the outgoing-message-handler
        super.setRequestTimeoutMillis(cMillis);
        }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public List<WrapperStreamFactory> getFilterList()
        {
        List listFilter = m_listFilter;

        if (listFilter == null)
            {
            m_listFilter = listFilter = new SafeLinkedList();
            }

        return listFilter;
        }

    /**
     * Set the filter list.
     *
     * @param listFilter  the filter list
     */
    @Injectable("use-filters")
    public void setFilterList(List<WrapperStreamFactory> listFilter)
        {
        m_listFilter = listFilter == null ? listFilter : new ImmutableArrayList(listFilter);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public Codec getMessageCodec()
        {
        return m_codec;
        }

    /**
     * Set the message codec.
     *
     * @param codec  the message codec
     */
    @Injectable("message-codec")
    public void setMessageCodec(Codec codec)
        {
        m_codec = codec;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getPingIntervalMillis()
        {
        return m_cPingIntervalMillis;
        }

    /**
     * Set the ping interval.
     *
     * @param cMillis  the ping interval
     */
    @Injectable("outgoing-message-handler/heartbeat-interval")
    public void setPingIntervalMillis(long cMillis)
        {
        m_cPingIntervalMillis = cMillis;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getPingTimeoutMillis()
        {
        return m_cPingTimeoutMillis < 0 ? getRequestTimeoutMillis() : m_cPingTimeoutMillis;
        }

    /**
     * Set the ping timeout.
     *
     * @param cMillis  the ping timeout
     */
    @Injectable("outgoing-message-handler/heartbeat-timeout")
    public void setPingTimeoutMillis(long cMillis)
        {
        m_cPingTimeoutMillis = cMillis;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMaxIncomingMessageSize()
        {
        return m_cbMaxIncomingMessageSize;
        }

    /**
     * Set the maximum incoming message size.
     *
     * @param cbMax  the maximum size
     */
    @Injectable("incoming-message-handler/max-message-size")
    public void setMaxIncomingMessageSize(int cbMax)
        {
        m_cbMaxIncomingMessageSize = cbMax;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMaxOutgoingMessageSize()
        {
        return m_cbMaxOutgoingMessageSize;
        }

    /**
     * Set the maximum outgoing message size.
     *
     * @param cbMax  the maximum size
     */
    @Injectable("outgoing-message-handler/max-message-size")
    public void setMaxOutgoingMessageSize(int cbMax)
        {
        m_cbMaxOutgoingMessageSize = cbMax;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public DefaultPeerDependencies validate()
        {
        super.validate();

        validatePingTimeout();

        return this;
        }

    // ----- Object methods -------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
        {
        return super.toString() + "{Codec=" + getMessageCodec() + ", FilterList=" + getFilterList()
               + ", PingIntervalMillis=" + getPingIntervalMillis() + ", PingTimeoutMillis=" + getPingTimeoutMillis()
               + ", MaxIncomingMessageSize=" + getMaxIncomingMessageSize() + ", MaxOutgoingMessageSize="
               + getMaxOutgoingMessageSize() + "}";
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Validate the ping timeout.
     */
    private void validatePingTimeout()
        {
        // make sure the heartbeat timeout <= interval
        long cTimeoutMillis  = getPingTimeoutMillis();
        long cIntervalMillis = getPingIntervalMillis();

        if (cIntervalMillis > 0L)
            {
            if (cTimeoutMillis == 0L)
                {
                setPingTimeoutMillis(cIntervalMillis);
                }
            else
                {
                setPingTimeoutMillis(Math.min(cIntervalMillis, cTimeoutMillis));
                }
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * The message codec.
     */
    private Codec m_codec = null;

    /**
     * The list of network filters.
     */
    private List<WrapperStreamFactory> m_listFilter = null;

    /**
     * The ping interval.
     */
    private long m_cPingIntervalMillis;

    /**
     * The ping timeout.
     *
     * (-1 means default to the request timeout)
     */
    private long m_cPingTimeoutMillis = -1;

    /**
     * The maximum incoming message size.
     */
    private int m_cbMaxIncomingMessageSize;

    /**
     * The maximum outgoing message size.
     */
    private int m_cbMaxOutgoingMessageSize;
    }
