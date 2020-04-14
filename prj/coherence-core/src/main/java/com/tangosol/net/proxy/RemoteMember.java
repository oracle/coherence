/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.proxy;

import com.tangosol.net.Member;

import com.tangosol.util.UID;

import java.net.InetAddress;

/**
 * A RemoteMember is a special {@link Member} implementation that carries
 * the remote IP/port information of a member that isn't in the cluster.
 * The ProxyService will then skip resolving the address if a RemoteMember
 * is returned from the {@link com.tangosol.net.proxy.ProxyServiceLoadBalancer}.
 *
 * @author wl  2013.03.01
 *
 * @since Coherence 12.1.3
 */
public class RemoteMember
         implements Member
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct a RemoteMember.
     *
     * @param address  the IP listen address of the remote member
     *                 ProxyService Acceptor
     * @param nPort    the TCP listen port of the remote member
     *                 ProxyService Acceptor
     */
    public RemoteMember(InetAddress address, int nPort)
        {
        m_Address = address;
        m_nPort   = nPort;
        }

    // ----- Member interface -----------------------------------------------

    /**
     * {@inheritDoc}
     */
    public InetAddress getAddress()
        {
        return m_Address;
        }

    /**
     * {@inheritDoc}
     */
     public int getPort()
        {
        return m_nPort;
        }

    /**
     * {@inheritDoc}
     */
    public long getTimestamp()
        {
        throw new UnsupportedOperationException();
        }

    /**
     * {@inheritDoc}
     */
    public UID getUid()
        {
        throw new UnsupportedOperationException();
        }

    /**
     * {@inheritDoc}
     */
    public int getId()
        {
        throw new UnsupportedOperationException();
        }

    // ----- MemberIdentity interface ---------------------------------------

    /**
     * {@inheritDoc}
     */
    public String getClusterName()
        {
        throw new UnsupportedOperationException();
        }

    /**
     * {@inheritDoc}
     */
     public int getMachineId()
        {
        throw new UnsupportedOperationException();
        }

    /**
     * {@inheritDoc}
     */
    public String getMachineName()
        {
        throw new UnsupportedOperationException();
        }

    /**
     * {@inheritDoc}
     */
    public String getMemberName()
        {
        throw new UnsupportedOperationException();
        }

    /**
     * {@inheritDoc}
     */
    public int getPriority()
        {
        throw new UnsupportedOperationException();
        }

    /**
     * {@inheritDoc}
     */
    public String getProcessName()
        {
        throw new UnsupportedOperationException();
        }

    /**
     * {@inheritDoc}
     */
    public String getRackName()
        {
        throw new UnsupportedOperationException();
        }

    /**
     *
     * {@inheritDoc}
     */
    public String getRoleName()
        {
        throw new UnsupportedOperationException();
        }

    /**
     * {@inheritDoc}
     */
    public String getSiteName()
        {
        throw new UnsupportedOperationException();
        }

    // ----- data members ---------------------------------------------------

    /**
     * The IP listen address of the remote member ProxyService Acceptor
     */
    private InetAddress m_Address;

    /**
    * The TCP listen port of the remote member ProxyService Acceptor
    */
    private int m_nPort;
    }
