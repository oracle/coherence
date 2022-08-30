/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.peer.acceptor;

import com.oracle.coherence.common.net.InetAddresses;
import com.tangosol.config.annotation.Injectable;

import com.tangosol.internal.net.service.peer.DefaultPeerDependencies;

import com.tangosol.util.Base;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * The AbstractAcceptorDependencies class provides an abstract implementation of
 * AcceptorDependencies.
 *
 * @author pfm 2011.06.27
 * @since Coherence 12.1.2
 */
public abstract class AbstractAcceptorDependencies
        extends DefaultPeerDependencies
        implements AcceptorDependencies
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a AbstractAcceptorDependencies object.
     */
    public AbstractAcceptorDependencies()
        {
        this(null);
        }

    /**
     * Construct a AbstractAcceptorDependencies object, copying the values from the specified
     * AcceptorDependencies object.
     *
     * @param deps  the dependencies to copy, or null
     */
    public AbstractAcceptorDependencies(AcceptorDependencies deps)
        {
        super(deps);

        if (deps != null)
            {
            m_cConnectionLimit = deps.getConnectionLimit();
            }
        }

    // ----- AbstractAcceptorDependencies methods ---------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public int getConnectionLimit()
        {
        return m_cConnectionLimit;
        }

    /**
     * Set the connection limit.
     *
     * @param cLimit  The connection limit
     */
    @Injectable("connection-limit")
    public void setConnectionLimit(int cLimit)
        {
        m_cConnectionLimit = cLimit;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public AbstractAcceptorDependencies validate()
        {
        super.validate();

        Base.azzert(getConnectionLimit() >= 0, "Connection limit cannot be less than 0");

        return this;
        }

    // ----- Object methods -------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
        {
        return super.toString()
                + "{ConnectionLimit=" + getConnectionLimit()
                + "}";
        }

    // ----- helper methods -------------------------------------------------


    /**
     * Normalize and validate that the passed in address is a sensible local address.
     *
     * @param sAddress  the address to normalize; may be <tt>null</tt>
     * @param nPort     the local port
     *
     * @return the validated, normalized address
     */
    protected String normalizeAddress(String sAddress, int nPort)
        {
        try
            {
            if (sAddress == null || sAddress.isEmpty())
                {
                sAddress = InetAddresses.ADDR_ANY.getHostAddress();
                }
            else
                {
                InetAddress addr = InetAddresses.getLocalAddress(sAddress);
                if (InetAddresses.isLocalAddress(addr))
                    {
                    sAddress = addr.getHostAddress();
                    }
                else if (InetAddresses.isNatLocalAddress(addr, nPort))
                    {
                    sAddress = InetAddresses.ADDR_ANY.getHostAddress();
                    }
                else
                    {
                    throw new IllegalArgumentException(sAddress + " does not represent a local address");
                    }
                }
            }
        catch (UnknownHostException e)
            {
            throw new IllegalArgumentException(e);
            }

        return sAddress;
        }

    // ----- data fields and constants --------------------------------------

    /**
     * The connection limit.
     */
    private int m_cConnectionLimit;
    }
