/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.grid;

import com.tangosol.coherence.config.builder.ActionPolicyBuilder;
import com.tangosol.coherence.config.builder.ServiceLoadBalancerBuilder;
import com.tangosol.internal.net.service.extend.proxy.CacheServiceProxyDependencies;
import com.tangosol.internal.net.service.extend.proxy.InvocationServiceProxyDependencies;

import com.tangosol.internal.net.service.peer.acceptor.AcceptorDependencies;

/**
 * The ProxyServiceDependencies interface provides a ProxyService with its external
 * dependencies.
 *
 * @author pfm  2011.07.25
 * @since Coherence 12.1.2
 */
public interface ProxyServiceDependencies
        extends GridDependencies
    {
    /**
     * Return the AcceptorDependencies.
     *
     * @return AcceptorDependencies
     */
    public AcceptorDependencies getAcceptorDependencies();

    /**
     * Return the ActionPolicyBuilder which is a pluggable policy that defines certain aspects
     * of a service's behavior at runtime.
     *
     * @return the ActionPolicyBuilder for the service
     */
    public ActionPolicyBuilder getActionPolicyBuilder();

    /**
     * Return the CacheServiceProxyDependencies.
     *
     * @return the CacheServiceProxyDependencies
     */
    public CacheServiceProxyDependencies getCacheServiceProxyDependencies();

    /**
     * Return the InvocationServiceProxyDependencies.
     *
     * @return the InvocationServiceProxyDependencies
     */
    public InvocationServiceProxyDependencies getInvocationServiceProxyDependencies();

    /**
     * Return the ProxyServiceLoadBalancerBuilder.
     *
     * @return the ProxyServiceLoadBalancerBuilder
     */
    public ServiceLoadBalancerBuilder getLoadBalancerBuilder();
    }
