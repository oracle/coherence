/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.management;

import com.tangosol.internal.net.management.GatewayDependencies;

import com.tangosol.net.management.MBeanHelper;
import com.tangosol.net.management.MBeanServerFinder;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;

import static org.junit.Assert.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

/**
 * @author jk 2014.09.29
 */
public class MBeanHelperTest
    {
    @Test
    public void shouldUseFirstExistingMBeanServer()
            throws Exception
        {
        MBeanServerFactory.createMBeanServer("Server0");

        GatewayDependencies deps = mock(GatewayDependencies.class);

        when(deps.getDefaultDomain()).thenReturn(null);

        ArrayList<MBeanServer> listServers = MBeanServerFactory.findMBeanServer(null);
        MBeanServer            mBeanServer = MBeanHelper.findMBeanServer("", deps);

        assertThat(mBeanServer, is(sameInstance(listServers.get(0))));
        }

    @Test
    public void shouldUseNamedPlatformMBeanServer()
            throws Exception
        {
        GatewayDependencies deps = mock(GatewayDependencies.class);

        when(deps.getDefaultDomain()).thenReturn("Server1");

        MBeanServer expectedMBeanServer = MBeanServerFactory.createMBeanServer("Server1");
        MBeanServer mBeanServer         = MBeanHelper.findMBeanServer("", deps);

        assertThat(mBeanServer, is(sameInstance(expectedMBeanServer)));
        }

    @Test
    public void shouldUseProvidedMBeanServer()
            throws Exception
        {
        GatewayDependencies deps                = mock(GatewayDependencies.class);
        MBeanServerFinder   finder              = mock(MBeanServerFinder.class);
        MBeanServer         expectedMBeanServer = MBeanServerFactory.createMBeanServer("Server2");

        when(deps.getDefaultDomain()).thenReturn("Server2");
        when(deps.getMBeanServerFinder()).thenReturn(finder);
        when(finder.findMBeanServer("Server2")).thenReturn(expectedMBeanServer);

        MBeanServer mBeanServer = MBeanHelper.findMBeanServer("", deps);

        assertThat(mBeanServer, is(sameInstance(expectedMBeanServer)));
        }

    @Test
    public void shouldCreateMBeanServer()
            throws Exception
        {
        GatewayDependencies deps = mock(GatewayDependencies.class);

        when(deps.getDefaultDomain()).thenReturn("Server3");

        MBeanServer            mBeanServer  = MBeanHelper.findMBeanServer("", deps);

        ArrayList<MBeanServer> listServers  = MBeanServerFactory.findMBeanServer(null);
        MBeanServer            serverResult = null;

        for (MBeanServer server : listServers)
            {
            if (server.getDefaultDomain().equals("Server3"))
                {
                serverResult = server;
                break;
                }
            }

        assertThat(serverResult, is(notNullValue()));
        assertThat(mBeanServer, is(sameInstance(serverResult)));
        }

    @Test
    public void testSafeUnquote()
        {
        String name   = "Test APP";
        String quoted = "\"Test APP\"";

        String newStr = MBeanHelper.safeUnquote(quoted);
        assertThat(newStr, is(name));

        newStr = MBeanHelper.safeUnquote(name);
        assertThat(newStr, is(name));
        }

    @Test
    public void shouldAllowWildcardPropertyValue()
            throws MalformedObjectNameException
        {
        ObjectName objectName = new ObjectName(MBeanHelper.quoteCanonical("Coherence:type=*HttpSessionManager,appId=" + "testapp"));
        assertThat(objectName.isPropertyValuePattern("type"), is(true));
        }

    @Test
    public void shouldAllowAnyCharacterPropertyValue()
            throws MalformedObjectNameException
        {
        ObjectName objectName = new ObjectName(MBeanHelper.quoteCanonical("Coherence:type=?HttpSessionManager,appId=" + "testapp"));
        assertThat(objectName.isPropertyValuePattern("type"), is(true));
        }

    @Test
    public void shouldEscapeWildcardAnyCharInKeyName()
            throws MalformedObjectNameException
        {
        String sNameCanonical = MBeanHelper.quoteCanonical("Coherence:type*?=*HttpSessionManager,appId=" + "testapp");
        assertThat(sNameCanonical, is("Coherence:\"type\\*\\?\"=*HttpSessionManager,appId=testapp"));
        }

    @Test
    public void shouldQuoteScopedServiceName()
            throws MalformedObjectNameException
        {
        String sNameCanonical = MBeanHelper.quoteCanonical("Coherence:service=SYS:Config");
        assertThat(sNameCanonical, is("Coherence:service=\"SYS:Config\""));
        }

    @Test
    public void shouldNotQuoteAlreadyQuotedScopedServiceName()
            throws MalformedObjectNameException
        {
        String sNameCanonical = MBeanHelper.quoteCanonical("Coherence:service=\"SYS:Config\"");
        assertThat(sNameCanonical, is("Coherence:service=\"SYS:Config\""));
        }
    }
