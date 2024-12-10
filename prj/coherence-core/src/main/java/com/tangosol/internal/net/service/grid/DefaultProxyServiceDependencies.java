/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.grid;

import com.tangosol.coherence.config.builder.ActionPolicyBuilder;
import com.tangosol.coherence.config.builder.ProxyServiceLoadBalancerBuilder;
import com.tangosol.coherence.config.builder.ServiceLoadBalancerBuilder;
import com.tangosol.config.annotation.Injectable;

import com.tangosol.internal.net.service.extend.proxy.CacheServiceProxyDependencies;
import com.tangosol.internal.net.service.extend.proxy.InvocationServiceProxyDependencies;
import com.tangosol.internal.net.service.peer.acceptor.AcceptorDependencies;

import com.tangosol.internal.net.service.peer.acceptor.DefaultTcpAcceptorDependencies;

import com.tangosol.util.Base;

/**
 * The DefaultProxyServiceDependencies class provides a default implementation of
 * ProxyServiceDependencies.
 *
 * @author pfm 2011.07.25
 * @since Coherence 12.1.2
 */
public class DefaultProxyServiceDependencies
        extends DefaultGridDependencies
        implements ProxyServiceDependencies
    {
    /**
     * Construct a DefaultProxyServiceDependencies object.
     */
    public DefaultProxyServiceDependencies()
        {
        this(null);
        }

    /**
     * Construct a DefaultProxyServiceDependencies object, copying the values from the
     * specified ProxyServiceDependencies object.
     *
     * @param deps  the dependencies to copy, or null
     */
    public DefaultProxyServiceDependencies(ProxyServiceDependencies deps)
        {
        super(deps);

        if (deps == null)
            {
            // by default, use a dynamic thread pool
            setWorkerThreadCountMin(1);
            }
        else
            {
            m_acceptorDependencies               = deps.getAcceptorDependencies();
            m_bldrLoadBalancer                   = deps.getLoadBalancerBuilder();
            m_cacheServiceProxyDependencies      = deps.getCacheServiceProxyDependencies();
            m_invocationServiceProxyDependencies = deps.getInvocationServiceProxyDependencies();
            }
        }

    // ----- GridDependencies interface -------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    @Injectable("proxy-quorum-policy-scheme")
    public void setActionPolicyBuilder(ActionPolicyBuilder builder)
        {
        super.setActionPolicyBuilder(builder);
        }

    // ----- ProxyServiceDependencies interface -----------------------------

    /**
 * {@inheritDoc}
 */
    @Override
    public AcceptorDependencies getAcceptorDependencies()
        {
        return m_acceptorDependencies;
        }

    /**
     * Set the AcceptorDependencies.
     *
     * @param deps  the AcceptorDependencies
     */
    @Injectable("acceptor-config")
    public void setAcceptorDependencies(AcceptorDependencies deps)
        {
        m_acceptorDependencies = deps;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public CacheServiceProxyDependencies getCacheServiceProxyDependencies()
        {
        return m_cacheServiceProxyDependencies;
        }

    /**
     * Set the CacheServiceProxyDependencies.
     *
     * @param deps  the CacheServiceProxyDependencies
     */
    @Injectable("proxy-config/cache-service-proxy")
    public void setCacheServiceProxyDependencies(CacheServiceProxyDependencies deps)
        {
        m_cacheServiceProxyDependencies = deps;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public InvocationServiceProxyDependencies getInvocationServiceProxyDependencies()
        {
        return m_invocationServiceProxyDependencies;
        }

    /**
     * Set the InvocationServiceProxyDependencies.
     *
     * @param deps  the InvocationServiceProxyDependencies
     */
    @Injectable("proxy-config/invocation-service-proxy")
    public void setInvocationServiceProxyDependencies(InvocationServiceProxyDependencies deps)
        {
        m_invocationServiceProxyDependencies = deps;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public ServiceLoadBalancerBuilder getLoadBalancerBuilder()
        {
        return m_bldrLoadBalancer;
        }

    /**
     * Set the ProxyServiceLoadBalancerBuilder.
     *
     * @param bldrLoadBalancer  the ProxyServiceLoadBalancerBuilder
     */
    @Injectable("load-balancer")
    public void setLoadBalancerBuilder(ServiceLoadBalancerBuilder bldrLoadBalancer)
        {
        m_bldrLoadBalancer = bldrLoadBalancer;
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public DefaultProxyServiceDependencies validate()
        {
        super.validate();

        Base.checkNotNull(getAcceptorDependencies(), "Acceptor");

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
                + "{AcceptorDependencies=" + getAcceptorDependencies()
                + ", ActionPolicyBuilder=" + getActionPolicyBuilder()
                + ", CacheServiceProxyDependencies=" + getCacheServiceProxyDependencies()
                + ", InvocationServiceProxyDependencies=" + getInvocationServiceProxyDependencies()
                + ", LoadBalancerBuilder=" + getLoadBalancerBuilder() + "}";
        }

    // ----- data fields and constants --------------------------------------

    /**
     * The AcceptorDependencies.
     */
    private AcceptorDependencies m_acceptorDependencies = new DefaultTcpAcceptorDependencies();

    /**
     * The CacheServiceProxyDependencies.
     */
    private CacheServiceProxyDependencies m_cacheServiceProxyDependencies;

    /**
     * The InvocationServiceProxyDependencies.
     */
    private InvocationServiceProxyDependencies m_invocationServiceProxyDependencies;

    /**
     * Initialize with a builder that returns {@link com.tangosol.net.proxy.DefaultProxyServiceLoadBalancer}.
     */
    private ServiceLoadBalancerBuilder m_bldrLoadBalancer = new ProxyServiceLoadBalancerBuilder(null, null);
    }
