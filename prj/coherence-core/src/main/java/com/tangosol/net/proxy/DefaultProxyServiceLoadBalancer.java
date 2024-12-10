/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.proxy;

import com.tangosol.net.Member;
import com.tangosol.net.ProxyService;
import com.tangosol.net.SimpleServiceLoadBalancer;

import java.util.Comparator;
import java.util.List;

/**
* Default ProxyServiceLoadBalancer implementation.
* <p>
* This implementation will redirect a new client connection to another
* ProxyService Member if a less utilized Member is identified. A Member is
* considered less utilized than another iff it's associated ProxyServiceLoad
* object is less than the ProxyServiceLoad of the other Member according to
* the Comparator specified during construction. By default, the natural
* ordering of the ProxyServiceLoad objects is used.
*
* @author jh  2010.12.10
*/
public class DefaultProxyServiceLoadBalancer
        extends SimpleServiceLoadBalancer<ProxyService, ProxyServiceLoad>
        implements ProxyServiceLoadBalancer
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public DefaultProxyServiceLoadBalancer()
        {
        this(null);
        }

    /**
    * Create a new DefaultProxyServiceLoadBalancer that will order
    * ProxyServiceLoad objects using the specified Comparator. If null, the
    * natural ordering of the ProxyServiceLoad objects will be used.
    *
    * @param comparator  the Comparator used to order ProxyServiceLoad
    *                    objects
    */
    public DefaultProxyServiceLoadBalancer(Comparator comparator)
        {
        super(comparator);
        }

    // ----- SimpleServiceLoadBalancer overrides ----------------------------

    @Override
    public void update(Member member, ProxyServiceLoad load)
        {
        super.update(member, load);

        if (isLocalMember(member))
            {
            m_loadLocal = load;
            }
        }

    @Override
    public List<Member> getMemberList(Member client)
        {
        ProxyServiceLoad loadLocal = m_loadLocal;
        if (loadLocal == null)
            {
            return null;
            }

        List<Member> listReturn = super.getMemberList(client);
        if (listReturn == null || listReturn.isEmpty())
            {
            return listReturn;
            }

        // if the local load is the same as the first element, move the local member to the beginning
        Member     member0    = listReturn.get(0);
        Comparator comparator = m_mapMember.comparator();

        if (!isLocalMember(member0) &&
            (comparator == null
                ? (loadLocal.equals(m_mapLoad.get(member0)))
                : ((comparator.compare(m_mapLoad.get(member0), loadLocal) == 0))))
            {
            Member memberThis = getLocalMember();
            listReturn.remove(memberThis);
            listReturn.add(0, memberThis);
            }

        return listReturn;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The ServiceLoad associated with the "local" Member.
     */
    protected ProxyServiceLoad m_loadLocal;
    }
