/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package security;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;

import com.tangosol.coherence.component.net.extend.RemoteService;

import com.tangosol.coherence.component.util.SafeService;

import com.tangosol.net.NamedCache;

import com.tangosol.net.messaging.Channel;

import com.tangosol.net.security.SimpleHandler;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import java.util.Properties;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;

import static org.hamcrest.CoreMatchers.is;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Simple test of authorizing a Coherence*Extend client through NameService.
 *
 * @author lh  2015.01.23
 */
public class NameServiceSecurityTests
        extends AbstractFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public NameServiceSecurityTests()
        {
        super("client-cache-config-ns.xml");
        }

    // ----- ExtendSecurityTests methods ------------------------------------

    /**
     * Start the cache server for this test class.
     */
    @BeforeClass
    public static void _startup()
        {
        System.setProperty("coherence.override", "security-coherence-override.xml");
        System.setProperty("java.security.auth.login.config", "login.config");
        AbstractFunctionalTest._startup();

        Properties props = new Properties();
        props.setProperty("test.server.classname", "security.SubjectCacheServer");

        CoherenceClusterMember clusterMember = startCacheServer("NameServiceSecurityTests", "security",
                "server-cache-config-ns.xml", props);
        Eventually.assertThat(invoking(clusterMember).isServiceRunning("TcpProxyService"), is(true));
        }

    /**
     * Stop the cache server for this test class.
     */
    @AfterClass
    public static void stopServer()
        {
        stopCacheServer("NameServiceSecurityTests");
        }

    /**
     * Test if the identity gets passed by Extend to the proxy.
     */
    @Test
    public void testIdentityPassing()
        {
        final Subject subject0 = loginJAAS("manager", "private");
        final Subject subject1 = loginJAAS("worker",  "private");

        assertTrue(subject0 != null);
        assertTrue(subject1 != null);
        try
            {
            Subject.doAs(subject0, new PrivilegedExceptionAction()
                {
                public Object run()
                        throws Exception
                    {
                    SafeService serviceSafe = (SafeService) getFactory().ensureService("TcpProxyService");
                    RemoteService serviceRemote = (RemoteService) serviceSafe.getService();

                    Channel channel = serviceRemote.getChannel();
                    assertEquals(subject0, channel.getSubject());

                    channel = channel.getConnection().getChannel(0);
                    assertEquals(subject0, channel.getSubject());

                    return null;
                    }
                });
            }
        catch (PrivilegedActionException e)
            {
            // failed if exception
            e.printStackTrace();
            fail("should not be an exception");
            }

        try
            {
            Subject.doAs(subject1, new PrivilegedExceptionAction()
                {
                public Object run()
                        throws Exception
                    {
                    SafeService   serviceSafe   = (SafeService) getFactory().ensureService("TcpProxyService");
                    RemoteService serviceRemote = (RemoteService) serviceSafe.getService();

                    Channel channel = serviceRemote.getChannel();
                    assertEquals(subject1, channel.getSubject());

                    channel = channel.getConnection().getChannel(0);
                    assertEquals(subject1, channel.getSubject());

                    return null;
                    }
                });
            }
        catch (PrivilegedActionException e)
            {
            // failed if exception
            e.printStackTrace();
            fail("should not be an exception");
            }

        try
            {
            Subject.doAs(subject0, new PrivilegedAction()
                {
                public Object run()
                    {
                    NamedCache cache = getNamedCache("dist-test");
                    cache.put("key", "value");

                    // TODO: remove when COH-2516 is fixed
                    cache.release();
                    return null;
                    }
                });
            }
        catch (Exception e)
            {
            // failed if security exception
            e.printStackTrace();
            fail("should not be a security exception");
            }

        Exception e = null;
        try
            {
            Subject.doAs(subject1, new PrivilegedAction()
                {
                public Object run()
                    {
                    NamedCache cache = getNamedCache("dist-test");
                    cache.put("key", "value");
                    // TODO: remove when COH-2516 is fixed
                    cache.release();
                    return null;
                    }
                });
            }
        catch (Exception ee)
            {
            // should get security exception
            e = ee;
            }
        if (e == null)
            {
            fail("expected security exception");
            }

        e = null;

       try
            {
            // Also try JAAS
            Subject.doAs(subject1, new PrivilegedAction()
                {
                public Object run()
                    {
                    NamedCache cache = getNamedCache("dist-test");
                    cache.put("key", "value");
                    return null;
                    }
                });
            }
        catch (Exception ee)
            {
            // should get security exception
            e = ee;
            }
        if (e == null)
            {
            fail("expected security exception");
            }
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Login using JAAS.
     *
     * @param sUser      the user name
     * @param sPassword  the password
     *
     * @return the authentcated subject
     */
    public static Subject loginJAAS(String sUser, String sPassword)
        {
        LoginContext context = null;
        try
            {
            context = new LoginContext("Coherence",
                   new SimpleHandler(sUser, sPassword.toCharArray()));
            }
        catch (LoginException le)
            {
            fail("login context failed: " + le);
            }

        try
            {
            context.login();
            }
        catch (Exception ee)
            {
            fail("login failed: " + ee);
            }
        return context.getSubject();
        }
    }
