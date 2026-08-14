/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.internal;

import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ProxyService;

import org.junit.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import java.util.Arrays;
import java.util.Collections;

import javax.management.remote.JMXServiceURL;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

/**
 * Unit tests for NameService passive value validation.
 *
 * @author Aleks Seovic  2026.05.17
 *
 * @since 26.07
 */
public class NameServiceValuePolicyTest
    {
    @Test
    public void shouldAllowPassiveValues()
            throws Exception
        {
        InetAddress address = InetAddress.getByName("127.0.0.1");

        NameServiceValuePolicy.validateBindResource("Cluster/name", "cluster", false);
        NameServiceValuePolicy.validateBindResource("Cluster/port", Integer.valueOf(7574), false);
        NameServiceValuePolicy.validateBindResource("Cluster/address",
                new InetSocketAddress(address, 7574), false);
        NameServiceValuePolicy.validateLookupResult("Proxy", new Object[] {"127.0.0.1", Integer.valueOf(20000)});
        NameServiceValuePolicy.validateLookupResult("directory", Arrays.asList("one", "two"));
        NameServiceValuePolicy.validateLookupResult("map", Collections.singletonMap("one", "two"));
        }

    @Test
    public void shouldAllowExactProductLocalResolvableBind()
        {
        NameServiceValuePolicy.validateBindResource("Proxy", new ProxyService(), false);
        }

    @Test
    public void shouldRejectRemoteResolvableBind()
        {
        assertThrowsIllegalArgument(() ->
                NameServiceValuePolicy.validateBindResource("Proxy", new TestResolvable(), true));
        }

    @Test
    public void shouldRejectUnknownLocalResolvableBind()
        {
        assertThrowsIllegalArgument(() ->
                NameServiceValuePolicy.validateBindResource("Proxy", new TestResolvable(), false));
        }

    @Test
    public void shouldRejectNonPassiveLookupValue()
        {
        assertThrowsIllegalArgument(() ->
                NameServiceValuePolicy.validateLookupResult("Proxy", new Object()));
        }

    @Test
    public void shouldValidateJmxServiceUrls()
            throws Exception
        {
        JMXServiceURL url = new JMXServiceURL(
                "service:jmx:rmi:///jndi/rmi://127.0.0.1:9000/jmxrmi");
        JMXServiceURL urlDirect = new JMXServiceURL(
                "service:jmx:rmi://127.0.0.1:9000");
        JMXServiceURL urlIiop = new JMXServiceURL(
                "service:jmx:iiop://127.0.0.1:9000/jndi/weblogic.management.mbeanservers.runtime");
        JMXServiceURL urlIiops = new JMXServiceURL(
                "service:jmx:iiops://127.0.0.1:9000/jndi/weblogic.management.mbeanservers.runtime");
        JMXServiceURL urlIiopLegacy = new JMXServiceURL(
                "service:jmx:iiop://127.0.0.1:9000/jndi/iiop://127.0.0.1:9000/weblogic.management.mbeanservers.runtime");
        JMXServiceURL urlIiopsLegacy = new JMXServiceURL(
                "service:jmx:iiops://127.0.0.1:9000/jndi/iiops://127.0.0.1:9000/weblogic.management.mbeanservers.runtime");
        JMXServiceURL urlStub = new JMXServiceURL(
                "service:jmx:rmi://127.0.0.1:9000/stub/abcd");
        JMXServiceURL urlLdap = new JMXServiceURL(
                "service:jmx:rmi:///jndi/ldap://127.0.0.1:1389/jmxrmi");
        JMXServiceURL urlLdaps = new JMXServiceURL(
                "service:jmx:rmi:///jndi/ldaps://127.0.0.1:1636/jmxrmi");
        JMXServiceURL urlIiopProvider = new JMXServiceURL(
                "service:jmx:iiop:///jndi/iiop://127.0.0.1:9000/jmxrmi");
        JMXServiceURL urlIiopsProvider = new JMXServiceURL(
                "service:jmx:rmi:///jndi/iiops://127.0.0.1:9000/jmxrmi");
        JMXServiceURL urlMalformedIiop = new JMXServiceURL(
                "service:jmx:iiop://127.0.0.1:9000/jndi/");
        JMXServiceURL urlT3 = new JMXServiceURL(
                "service:jmx:t3://127.0.0.1:9000/jndi/weblogic.management.mbeanservers.runtime");
        JMXServiceURL urlT3s = new JMXServiceURL(
                "service:jmx:t3s://127.0.0.1:9000/jndi/weblogic.management.mbeanservers.runtime");
        JMXServiceURL urlDns = new JMXServiceURL(
                "service:jmx:rmi:///jndi/dns://127.0.0.1/jmxrmi");
        JMXServiceURL urlMalformedRmi = new JMXServiceURL(
                "service:jmx:rmi:///jndi/rmi:///jmxrmi");

        assertSame(url, NameServiceValuePolicy.validateJmxServiceURL(url));
        assertSame(urlDirect, NameServiceValuePolicy.validateJmxServiceURL(urlDirect));
        assertSame(urlIiop, NameServiceValuePolicy.validateJmxServiceURL(urlIiop));
        assertSame(urlIiops, NameServiceValuePolicy.validateJmxServiceURL(urlIiops));
        assertSame(urlIiopLegacy, NameServiceValuePolicy.validateJmxServiceURL(urlIiopLegacy));
        assertSame(urlIiopsLegacy, NameServiceValuePolicy.validateJmxServiceURL(urlIiopsLegacy));
        assertSame(urlT3, NameServiceValuePolicy.validateJmxServiceURL(urlT3));
        assertSame(urlT3s, NameServiceValuePolicy.validateJmxServiceURL(urlT3s));
        assertSame(urlIiop, NameServiceValuePolicy.validateManagementPublishJmxServiceURL(urlIiop));
        assertSame(urlIiops, NameServiceValuePolicy.validateManagementPublishJmxServiceURL(urlIiops));
        assertSame(urlT3, NameServiceValuePolicy.validateManagementPublishJmxServiceURL(urlT3));
        assertSame(urlT3s, NameServiceValuePolicy.validateManagementPublishJmxServiceURL(urlT3s));

        assertThrowsIllegalArgument(() ->
                NameServiceValuePolicy.validateJmxServiceURL(urlStub));
        assertThrowsIllegalArgument(() ->
                NameServiceValuePolicy.validateJmxServiceURL(urlLdap));
        assertThrowsIllegalArgument(() ->
                NameServiceValuePolicy.validateJmxServiceURL(urlLdaps));
        assertThrowsIllegalArgument(() ->
                NameServiceValuePolicy.validateJmxServiceURL(urlIiopProvider));
        assertThrowsIllegalArgument(() ->
                NameServiceValuePolicy.validateJmxServiceURL(urlIiopsProvider));
        assertThrowsIllegalArgument(() ->
                NameServiceValuePolicy.validateJmxServiceURL(urlMalformedIiop));
        assertThrowsIllegalArgument(() ->
                NameServiceValuePolicy.validateJmxServiceURL(urlDns));
        assertThrowsIllegalArgument(() ->
                NameServiceValuePolicy.validateJmxServiceURL(urlMalformedRmi));
        }

    private static void assertThrowsIllegalArgument(Runnable runnable)
        {
        try
            {
            runnable.run();
            fail("expected IllegalArgumentException");
            }
        catch (IllegalArgumentException e)
            {
            // expected
            }
        }

    public static class TestResolvable
            implements com.tangosol.net.NameService.Resolvable
        {
        @Override
        public Object resolve(com.tangosol.net.NameService.RequestContext ctx)
            {
            return "resolved";
            }
        }
    }
