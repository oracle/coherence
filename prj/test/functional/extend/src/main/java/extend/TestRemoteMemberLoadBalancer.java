/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package extend;


import com.oracle.coherence.common.net.InetSocketAddress32;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.Member;
import com.tangosol.net.proxy.DefaultProxyServiceLoadBalancer;
import com.tangosol.net.proxy.RemoteMember;

import com.tangosol.coherence.component.util.SafeService;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ProxyService;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A custom TestRemoteMemberLoadBalancer that extends the
 * DefaultProxyServiceLoadBalancer.
 * <p>
 * This implementation extends DefaultProxyServiceLoadBalancer, overrides
 * the getMemberList() method to return a RemoteMember.
 *
 * @author wl 2013.03.14
*/
public class TestRemoteMemberLoadBalancer
        extends DefaultProxyServiceLoadBalancer
    {
    // ----- contructors ----------------------------------------------------

    /**
     * Default constructor.
     */
    public TestRemoteMemberLoadBalancer()
        {
        this(null);
        }

    /**
     * Create a new TestRemoteMemberLoadBalancer that will order
     * ProxyServiceLoad objects using the specified Comparator. If null, the
     * natural ordering of the ProxyServiceLoad objects will be used.
     *
     * @param comparator  the Comparator used to order ProxyServiceLoad
     *                    objects
     */
    public TestRemoteMemberLoadBalancer(Comparator comparator)
        {
        super(comparator);
        }


    // ----- ProxyServiceLoadBalancer interface -----------------------------

    /**
     * {@inheritDoc}
     */
    public List<Member> getMemberList(Member client)
        {
        SafeService         service    = (SafeService) CacheFactory
                .getCluster().getService("ExtendTcpProxyService2");
        ProxyService        proxy      = (ProxyService) service.getService();
        TcpAcceptor         acceptor   = (TcpAcceptor) proxy.getAcceptor();
        InetSocketAddress32 addr       = (InetSocketAddress32) acceptor
                .getProcessor().getServerSocket().getLocalSocketAddress();
        List                memberList = new ArrayList<Member>(1);
        memberList.add(new RemoteMember(addr.getAddress(), addr.getPort()));
        return memberList;
        }
    }