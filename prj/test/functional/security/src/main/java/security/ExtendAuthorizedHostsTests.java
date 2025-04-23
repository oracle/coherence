/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package security;


import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

import java.util.Properties;

import org.junit.BeforeClass;
import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static com.oracle.bedrock.testsupport.deferred.Eventually.assertDeferred;
import static com.oracle.bedrock.testsupport.deferred.Eventually.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;


/**
 * Functional test of system property coherence.extend.authorized.hosts using default coherence-cache-config.xml
 * and verify that comma separated list can be put directly in tcp-acceptor.authorized-hosts.host-address.
 *
 * @author jf  2025.04.16
 */
public class ExtendAuthorizedHostsTests
        extends AbstractFunctionalTest
    {
    public ExtendAuthorizedHostsTests()
        {
        // use default coherence cache config
        super("coherence-cache-config.xml");
        }

    /**
     * Initialize the test class.
     */
    @BeforeClass
    public static void _startup()
        {
        System.setProperty("coherence.client", "remote");
        System.setProperty("coherence.profile", "thin");
        System.setProperty("coherence.wka", "127.0.0.1");
        System.setProperty("coherence.tcmp.enabled", "false"); // disable joining cluster as a member.
        }

    /**
     * Validate setting system property coherence.extend.authorized.hosts works with default coherence cache config.
     */
    @Test
    public void testAccessAllowed()
        {
        Properties props = new Properties();
        props.setProperty("coherence.wka", "127.0.0.1");
        props.setProperty("coherence.tcmp.enabled", "true");
        props.setProperty("coherence.client", "direct");
        props.setProperty("coherence.extend.authorized.hosts", "nonexistent.dns.address,baddomain.badhost,127.0.0.1");

        CoherenceClusterMember clusterMember = startCacheServer("ExtendAuthorizedHostsAllowed", "security", null, props, false);
        assertThat(invoking(clusterMember).getClusterSize(), is(1));
        assertDeferred(()->clusterMember.isServiceRunning("Proxy"), is(true));

        try
            {
            NamedCache cache = CacheFactory.getCache("dist-extend");
            assertNotNull(cache);
            assertThat(cache.size(), is(0));
            }
        finally
            {
            stopAllApplications();
            CacheFactory.shutdown();
            System.setProperty("coherence.tcmp.enabled", "true");
            }
        }

    /**
     * Validate setting system property coherence.extend.authorized.hosts works with default coherence cache config.
     */
    @Test
    public void testDeniedBySystemProperty()
        {
        Properties props = new Properties();

        props.setProperty("coherence.extend.authorized.hosts", "nonexistent.dns.address,baddomain.badhost,127.0.0.2");
        props.setProperty("coherence.wka", "127.0.0.1");
        props.setProperty("coherence.tcmp.enabled", "true");
        props.setProperty("coherence.client", "direct");

        CoherenceClusterMember clusterMember = startCacheServer("ExtendAuthorizedHostsDeniedBySystemProperty", "security", null, props, false);
        assertThat(invoking(clusterMember).getClusterSize(), is(1));
        assertDeferred(()->clusterMember.isServiceRunning("Proxy"), is(true));
        System.setProperty("coherence.tcmp.enabled", "false"); // disable joining cluster as a member.

        try
            {
            NamedCache cache = getNamedCache("dist-extend");
            fail("should not have been allowed to connect to proxy due to system property coherence.extend.authorized.hosts");
            }
        catch (Throwable e)
            {
            System.out.println("testDeniedBySystemProperty: Handled exception " + e.getClass().getName() + ": " + e.getMessage());
            // expected following in server log: Received a connection attempt from remote address 127.0.0.1 that was not an authorized host and following client side exception message.
            assertThat("testDeniedBySystemProperty: validate exception message matches \"could not establish connection...\" message=" + e.getMessage(),
                       e.getMessage().contains("could not establish a connection to one of the following addresses: [127.0.0.1"), is(true));
            }
        finally
            {
            stopAllApplications();
            CacheFactory.shutdown();
            }
        }

    /**
     * Validate comma separated dns/ip addresses work for proxy-scheme.acceptor-config.tcp-acceptor.authorized-hosts.host-address value.
     */
    @Test
    public void testDeniedByTcpAcceptorAuthorizedHosts()
        {
        Properties props = new Properties();

        props.setProperty("coherence.wka", "127.0.0.1");
        props.setProperty("coherence.tcmp.enabled", "true");
        props.setProperty("coherence.client", "direct");
        //props.setProperty("coherence.authorized.hosts", "nonexistent.dns.name,127.0.0.2");

        CoherenceClusterMember clusterMember = startCacheServer("DeniedByTcpAcceptorAuthorizedHosts", "security",
                                                                "server-cache-config-extend-authorized-hosts.xml", props, false);
        assertThat(invoking(clusterMember).getClusterSize(), is(1));
        assertDeferred(()->clusterMember.isServiceRunning("Proxy"), is(true));
        System.setProperty("coherence.tcmp.enabled", "false"); // disable joining cluster as a member.

        try
            {
            NamedCache cache = getNamedCache("dist-extend");
            fail("should not have been allowed to connect to proxy due to system property coherence.extend.authorized.hosts");
            }
        catch (Throwable e)
            {
            // expected following in server log: Received a connection attempt from remote address 127.0.0.1 that was not an authorized host and following client side exception message.
            assertThat("testDeniedBySystemProperty: validate exception message matches \"could not establish connection...\" message=" + e.getMessage(),
                       e.getMessage().contains("could not establish a connection to one of the following addresses: [127.0.0.1"), is(true));
            }
        finally
            {
            stopAllApplications();
            CacheFactory.shutdown();
            }
        }
    }
