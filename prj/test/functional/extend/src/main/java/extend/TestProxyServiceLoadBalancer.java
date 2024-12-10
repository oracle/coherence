/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package extend;


import com.tangosol.net.Member;

import com.tangosol.net.proxy.DefaultProxyServiceLoadBalancer;

import com.tangosol.net.security.SecurityHelper;

import java.util.Comparator;
import java.util.List;

import javax.security.auth.Subject;


/**
* A custom ProxyServiceLoadBalancer that extends the
* DefaultProxyServiceLoadBalancer.
* <p>
* This implementation extends DefaultProxyServiceLoadBalancer, overrides
* the getMemberList() method to save the most recent client passed to it
* and its associated Subject.
*
* @author lh  2011.05.03
*/
public class TestProxyServiceLoadBalancer
        extends DefaultProxyServiceLoadBalancer
    {
    // ----- contructors ----------------------------------------------------

    /**
    * Default constructor.
    */
    public TestProxyServiceLoadBalancer()
        {
        this(null);
        }

    /**
    * Create a new TestProxyServiceLoadBalancer that will order
    * ProxyServiceLoad objects using the specified Comparator. If null, the
    * natural ordering of the ProxyServiceLoad objects will be used.
    *
    * @param comparator  the Comparator used to order ProxyServiceLoad
    *                    objects
    */
    public TestProxyServiceLoadBalancer(Comparator comparator)
        {
        super(comparator);
        }


    // ----- ProxyServiceLoadBalancer interface -----------------------------

    /**
    * {@inheritDoc}
    */
    public List<Member> getMemberList(Member client)
        {
        m_client = client;
        m_subject = SecurityHelper.getCurrentSubject();
        return super.getMemberList(client);
        }


    // ----- TestProxyServiceLoadBalancer interface -------------------------

    /**
    * Get the client member.
    *
    * @return the client member information
    */
    public static Member getClient()
        {
        return m_client;
        }

    /**
    * Get the client subject.
    *
    * @return the client subject information
    */
    public static Subject getSubject()
        {
        return m_subject;
        }


    // ----- data members ---------------------------------------------------

    /**
    * The client member information.
    */
    protected static Member m_client;

    /**
    * The client subject information.
    */
    protected static Subject m_subject;
    }
