/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.grid;

import com.tangosol.coherence.config.builder.InvalidConfigServiceLoadBalancerBuilder;
import com.tangosol.coherence.config.builder.ParameterizedBuilder;
import com.tangosol.coherence.config.builder.ProxyServiceLoadBalancerBuilder;
import com.tangosol.coherence.config.builder.ServiceLoadBalancerBuilder;
import com.tangosol.internal.net.LegacyXmlConfigHelper;
import com.tangosol.internal.net.cluster.LegacyXmlConfigurableQuorumPolicy;

import com.tangosol.internal.net.service.extend.proxy.CacheServiceProxyDependencies;
import com.tangosol.internal.net.service.extend.proxy.DefaultCacheServiceProxyDependencies;
import com.tangosol.internal.net.service.extend.proxy.DefaultInvocationServiceProxyDependencies;
import com.tangosol.internal.net.service.extend.proxy.InvocationServiceProxyDependencies;
import com.tangosol.internal.net.service.extend.proxy.LegacyXmlCacheServiceProxyHelper;
import com.tangosol.internal.net.service.extend.proxy.LegacyXmlInvocationServiceProxyHelper;

import com.tangosol.internal.net.service.peer.acceptor.LegacyXmlAcceptorHelper;

import com.tangosol.net.OperationalContext;

import com.tangosol.net.proxy.DefaultProxyServiceLoadBalancer;
import com.tangosol.net.proxy.ProxyServiceLoadBalancer;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

/**
 * LegacyXmlProxyServiceHelper parses XML to populate a DefaultProxyServiceDependencies
 * object.
 *
 * @author pfm 2011.05.08
 * @since Coherence 12.1.2
 */
@Deprecated
public class LegacyXmlProxyServiceHelper
    {
    /**
     * Populate the DefaultProxyServiceDependencies object from the given XML configuration.
     *
     * @param xml     the XML parent element that contains the ProxyService elements
     * @param deps    the DefaultProxyServiceDependencies to be populated
     * @param ctx     the OperationalContext
     * @param loader  the class loader for the current context
     *
     * @return the DefaultProxyServiceDependencies object that was passed in
     */
    public static DefaultProxyServiceDependencies fromXml(XmlElement xml,
            DefaultProxyServiceDependencies deps, OperationalContext ctx, ClassLoader loader)
        {
        LegacyXmlGridHelper.fromXml(xml, deps, ctx, loader);

        // configure all Proxies
        CacheServiceProxyDependencies cacheServiceProxyDeps =
            LegacyXmlCacheServiceProxyHelper.fromXml(
                     xml.getSafeElement("proxy-config/cache-service-proxy"),
                     new DefaultCacheServiceProxyDependencies());
        deps.setCacheServiceProxyDependencies(cacheServiceProxyDeps);

        InvocationServiceProxyDependencies invocationServiceProxyDeps =
            LegacyXmlInvocationServiceProxyHelper.fromXml(
                     xml.getSafeElement("proxy-config/invocation-service-proxy"),
                     new DefaultInvocationServiceProxyDependencies());
        deps.setInvocationServiceProxyDependencies(invocationServiceProxyDeps);

        // create and set the Acceptor dependencies
        deps.setAcceptorDependencies(LegacyXmlAcceptorHelper.createAcceptorDeps(xml, ctx, loader));

        // configure and create the load balancer
        ServiceLoadBalancerBuilder builder = null;
        XmlElement xmlBalancer = xml.getSafeElement("load-balancer");
        XmlElement xmlInstance = xmlBalancer.getElement("instance");
        if (xmlInstance == null)
            {
            String sName = XmlHelper.isEmpty(xmlBalancer) ? "proxy" : xmlBalancer.getString().trim();
            switch (sName)
                {
                case "proxy":
                    builder = new ProxyServiceLoadBalancerBuilder(null, xmlBalancer);
                    break;
                case "client":
                    builder = null;
                    break;
                default:
                    builder = new InvalidConfigServiceLoadBalancerBuilder(sName, xmlBalancer);
                }
            }
        else
            {
            ParameterizedBuilder<?> bldr = LegacyXmlConfigHelper.createBuilder(
                    xmlInstance, ProxyServiceLoadBalancer.class);
            builder = new ProxyServiceLoadBalancerBuilder(bldr, xmlBalancer);
            }

        if (builder != null)
            {
            deps.setLoadBalancerBuilder(builder);
            }

        // configure the action-policy
        XmlElement xmlPolicy = xml.getSafeElement("proxy-quorum-policy-scheme");
        deps.setActionPolicyBuilder(new LegacyXmlConfigurableQuorumPolicy().createPolicyBuilder(
                                 xmlPolicy, ctx, loader));

        return deps;
        }
    }
