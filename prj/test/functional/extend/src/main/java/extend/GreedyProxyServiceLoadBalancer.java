/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package extend;


import com.tangosol.net.Member;
import com.tangosol.net.ProxyService;

import com.tangosol.net.AbstractServiceLoadBalancer;

import com.tangosol.net.proxy.ProxyServiceLoad;
import com.tangosol.net.proxy.ProxyServiceLoadBalancer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


/**
* Greedy ProxyServiceLoadBalancer implementation.
*
* @author jh  2011.06.06
*/
public class GreedyProxyServiceLoadBalancer
        extends AbstractServiceLoadBalancer<ProxyService, ProxyServiceLoad>
        implements ProxyServiceLoadBalancer
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public GreedyProxyServiceLoadBalancer()
        {
        }


    // ----- ProxyServiceLoadBalancer interface -----------------------------

    /**
    * {@inheritDoc}
    */
    public void update(Member member, ProxyServiceLoad load)
        {
        }

    /**
    * {@inheritDoc}
    */
    public List<Member> getMemberList(Member client)
        {
        ProxyService service = getService();
        Set<Member>  set     = service.getInfo().getServiceMembers();
        List<Member> list    = new ArrayList(set.size());

        for (Iterator<Member> iter = set.iterator(); iter.hasNext();)
            {
            Member member = iter.next();
            if (isLocalMember(member))
                {
                continue;
                }
            list.add(member);
            }

        out(">>> List=" + list);

        return list;
        }
    }
