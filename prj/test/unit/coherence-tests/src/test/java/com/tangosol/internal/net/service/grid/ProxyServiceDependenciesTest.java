/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.service.grid;

import com.oracle.coherence.common.net.InetSocketAddress32;

import com.tangosol.coherence.config.builder.ActionPolicyBuilder;
import com.tangosol.coherence.config.builder.ProxyQuorumPolicyBuilder;
import com.tangosol.coherence.config.builder.ServiceLoadBalancerBuilder;
import com.tangosol.config.expression.NullParameterResolver;

import com.tangosol.internal.net.service.extend.proxy.CacheServiceProxyDependencies;
import com.tangosol.internal.net.service.extend.proxy.DefaultCacheServiceProxyDependencies;
import com.tangosol.internal.net.service.extend.proxy.DefaultInvocationServiceProxyDependencies;
import com.tangosol.internal.net.service.extend.proxy.InvocationServiceProxyDependencies;
import com.tangosol.internal.net.service.peer.acceptor.AcceptorDependencies;
import com.tangosol.internal.net.service.peer.acceptor.DefaultTcpAcceptorDependencies;
import com.tangosol.internal.net.service.peer.acceptor.TcpAcceptorDependencies;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.OperationalContext;

import com.tangosol.run.xml.XmlElement;
import com.tangosol.run.xml.XmlHelper;

import com.tangosol.util.AssertionException;
import com.tangosol.util.Base;

import java.net.SocketAddress;

import org.junit.Test;

import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Unit tests for ProxyServiceDependencies.
 *
 * @author pfm  2011.09.26
 */
public class ProxyServiceDependenciesTest
    {
    // ----- tests ----------------------------------------------------------

    /**
     * Test the setters and getters plus the clone.
     */
    @Test
    public void testAccessAndClone()
        {
        DefaultProxyServiceDependencies deps1 = new DefaultProxyServiceDependencies();
        populate(deps1).validate();

        DefaultProxyServiceDependencies deps2 = new DefaultProxyServiceDependencies(deps1);
        assertCloneEquals(deps1, deps2);
        deps2.validate();
        }

    /**
     * Assert that null AcceptorDependcies throws an exception.
     */
    @Test
    public void nullAcceptorDependencies()
        {
        try
            {
            //TODO: refactor this when new validation is in place
            // populate(new DefaultProxyServiceDependencies()).setAcceptorDependencies(null).validate();
            // Assert.fail();
            }
        catch (IllegalArgumentException e)
            {
            return;
            }
        catch (AssertionException e)
            {
            return;
            }
        }

    /**
     * Validate the Proxy configuration using XML.
     */
    @Test
    public void validateProxyXml()
        {
        validateProxyXml(true);
        validateProxyXml(false);
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Assert that the two ProxyServiceDependencies are equal.
     *
     * @param deps1  the first ProxyServiceDependencies object
     * @param deps2  the second ProxyServiceDependencies object
     */
    public static void assertCloneEquals(ProxyServiceDependencies deps1,
            ProxyServiceDependencies deps2)
        {
        GridDependenciesTest.assertCloneEquals(deps1, deps2);

        assertEquals(deps1.getAcceptorDependencies(), deps2.getAcceptorDependencies());
        assertEquals(deps1.getActionPolicyBuilder(),  deps2.getActionPolicyBuilder());
        assertEquals(deps1.getLoadBalancerBuilder(),  deps2.getLoadBalancerBuilder());
        assertEquals(deps1.getCacheServiceProxyDependencies(),
                deps2.getCacheServiceProxyDependencies());
        assertEquals(deps1.getInvocationServiceProxyDependencies(),
                deps2.getInvocationServiceProxyDependencies());
        }

    /**
     * Populate the dependencies and test the getters.
     *
     * @param deps  the DefaultProxyServiceDependencies to populate
     *
     * @return the DefaultProxyServiceDependencies that was passed in
     */
    public static DefaultProxyServiceDependencies populate(
            DefaultProxyServiceDependencies deps)
        {
        GridDependenciesTest.populate(deps);

        AcceptorDependencies acceptorDeps = new DefaultTcpAcceptorDependencies();
        deps.setAcceptorDependencies(acceptorDeps);
        assertEquals(acceptorDeps, deps.getAcceptorDependencies());

        ActionPolicyBuilder builder = Mockito.mock(ProxyQuorumPolicyBuilder.class);
        deps.setActionPolicyBuilder(builder);
        assertEquals(builder, deps.getActionPolicyBuilder());

        ServiceLoadBalancerBuilder balancer = Mockito.mock(ServiceLoadBalancerBuilder.class);
        deps.setLoadBalancerBuilder(balancer);
        assertEquals(balancer, deps.getLoadBalancerBuilder());

        CacheServiceProxyDependencies cacheDeps = new DefaultCacheServiceProxyDependencies();
        deps.setCacheServiceProxyDependencies(cacheDeps);
        assertEquals(cacheDeps, deps.getCacheServiceProxyDependencies());

        InvocationServiceProxyDependencies invocationDeps =
            new DefaultInvocationServiceProxyDependencies();
        deps.setInvocationServiceProxyDependencies(invocationDeps);
        assertEquals(invocationDeps, deps.getInvocationServiceProxyDependencies());

        return deps;
        }


    /**
     * Validate the Proxy configuration using XML.
     *
     * @param flag  sets all booleans in XML to true or false
     */
    protected void validateProxyXml(boolean flag)
        {
        String sXml = "<?xml version=\"1.0\"?>"+
        "<proxy-scheme>" +
          "<scheme-name>proxy-default</scheme-name>" +
          "<service-name>Proxy</service-name>" +
          "<thread-count>25</thread-count>" +
          "<task-hung-threshold>5s</task-hung-threshold>" +
          "<task-timeout>6s</task-timeout>" +
          "<request-timeout>7s</request-timeout>" +
          "<guardian-timeout>8s</guardian-timeout>" +

          "<acceptor-config>" +
            "<tcp-acceptor>" +
              "<local-address>" +
                "<address>localhost</address>" +
                "<port>9099</port>" +
              "</local-address>" +
              "<reuse-address>false</reuse-address>" +
              "<keep-alive-enabled>" + flag + "</keep-alive-enabled>" +
              "<tcp-delay-enabled>" + flag + "</tcp-delay-enabled>" +
              "<reuse-address>false</reuse-address>" +
              "<send-buffer-size>128kb </send-buffer-size>" +
              "<suspect-protocol-enabled>" + flag + "</suspect-protocol-enabled>" +
              "<suspect-buffer-size>   129kb </suspect-buffer-size>" +
              "<suspect-buffer-length> 5000  </suspect-buffer-length>" +
              "<nominal-buffer-size>   130kb </nominal-buffer-size>" +
              "<nominal-buffer-length> 6000  </nominal-buffer-length>" +
              "<limit-buffer-size>     131kb </limit-buffer-size>" +
              "<limit-buffer-length>   7000  </limit-buffer-length>" +
              "<incoming-buffer-pool>" +
                "<buffer-size>10k</buffer-size>" +
              "</incoming-buffer-pool>" +
              "<outgoing-buffer-pool>" +
                "<buffer-size>10k</buffer-size>" +
              "</outgoing-buffer-pool>" +
            "</tcp-acceptor>" +
            "<connection-limit>16</connection-limit>" +
          "</acceptor-config>" +

          "<proxy-config>" +
              "<cache-service-proxy>" +
                  "<enabled>" + flag + "</enabled>" +
                  "<lock-enabled>" + flag + "</lock-enabled>" +
                  "<read-only>" + flag + "</read-only>" +
                  "<transfer-threshold>9000</transfer-threshold>" +
              "</cache-service-proxy>" +
          "</proxy-config>" +
        "</proxy-scheme>" ;

        Cluster cluster = CacheFactory.getCluster();

        XmlElement xml = XmlHelper.loadXml(sXml);
        ProxyServiceDependencies deps = LegacyXmlProxyServiceHelper.fromXml(xml,
                new DefaultProxyServiceDependencies(), (OperationalContext) cluster,
                Base.getContextClassLoader());

        assertEquals(deps.getWorkerThreadCount(), 25);
        assertEquals(deps.getTaskHungThresholdMillis(), 5000);
        assertEquals(deps.getTaskTimeoutMillis(), 6000);
        assertEquals(deps.getRequestTimeoutMillis(), 7000);

        // test TcpAcceptor
        TcpAcceptorDependencies depsAcceptor = (TcpAcceptorDependencies) deps.getAcceptorDependencies();
        assertEquals(depsAcceptor.getConnectionLimit(), 16);

        // note: the send buffer size is in SocketOptions and there is no way to get it.
        System.out.println(" *** SocketOptions ** " + depsAcceptor.getSocketOptions().toString());
        SocketAddress addr = depsAcceptor.getLocalAddressProviderBuilder().realize(
                new NullParameterResolver(), Base.getContextClassLoader(), null).getNextAddress();
        assertEquals(((InetSocketAddress32) addr).getPort(), 9099);

        assertEquals(depsAcceptor.isSuspectProtocolEnabled(), flag);

        assertEquals(depsAcceptor.getDefaultSuspectBytes(), 129 * 1024);
        assertEquals(depsAcceptor.getDefaultNominalBytes(), 130 * 1024);
        assertEquals(depsAcceptor.getDefaultLimitBytes(),   131 * 1024);

        assertEquals(depsAcceptor.getDefaultSuspectMessages(), 5000);
        assertEquals(depsAcceptor.getDefaultNominalMessages(), 6000);
        assertEquals(depsAcceptor.getDefaultLimitMessages(),   7000);

        assertEquals(depsAcceptor.getIncomingBufferPoolConfig().getBufferSize(), 10 * 1024);
        assertEquals(depsAcceptor.getOutgoingBufferPoolConfig().getBufferSize(), 10 * 1024);

        // test CacheServiceProxy component config
        CacheServiceProxyDependencies depsProxy = deps.getCacheServiceProxyDependencies();
        assertEquals(depsProxy.isEnabled(), flag);
        assertEquals(depsProxy.isLockEnabled(), flag);
        assertEquals(depsProxy.isReadOnly(), flag);
        assertEquals(depsProxy.getTransferThreshold(), 9000);
        }
    }
