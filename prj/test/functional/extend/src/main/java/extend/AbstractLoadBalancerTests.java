/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package extend;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;

import com.tangosol.io.pof.PofPrincipal;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.AbstractInvocable;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.InvocationService;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Service;

import com.tangosol.net.messaging.ConnectionException;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import java.io.IOException;

import java.security.Principal;
import java.security.PrivilegedAction;

import java.util.Map;
import java.util.Properties;

import javax.security.auth.Subject;

import org.junit.After;
import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;

import static org.hamcrest.CoreMatchers.is;

import static org.junit.Assert.*;

/**
* A collection of functional tests for the Coherence*Extend load balancing
* policies.
*
* @author jh  2010.12.13
*/
public abstract class AbstractLoadBalancerTests
        extends AbstractFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public AbstractLoadBalancerTests(String sPath)
        {
        super(sPath);
        }


    // ----- setup methods --------------------------------------------------

    @After
    public void cleanup()
        {
        stopAllApplications();
        }

    // ----- abstract methods -----------------------------------------------

    /**
    * Set port property before the first cache server starts.
    *
    * @param props  Properties for first cache server
    */
    abstract void setPortBefore1(Properties props);

    /**
    * Set port property before the second cache server starts.
    *
    * @param props  Properties for second cache server
    */
    abstract void setPortBefore2(Properties props);

    /**
    * Set system property after the cache servers start.
    *
    * @param sServerName  cache server name
    */
    abstract void setPortAfter(String sServerName);


    // ----- test methods ---------------------------------------------------

    /**
     * Test the behavior of the client load balancing policy.
     */
    @Test
    public void testClient()
        {
        verifyLB("Client", m_configClient, false, null);
        }

    /**
     * Test that connections can be redirected by a ProxyService which
     * is at its connection limit.
     */
    @Test
    public void testConnectionLimit()
        {
        Properties props = new Properties();
        props.setProperty("test.extend.connection.limit", "1");
        verifyLB("ConnLimit", m_configDefault, true, props);
        }

    /**
     * Test case for COH-5018 using a custom load balancer.
     */
    @Test
    public void testCustom()
        {
        verifyLB("Custom", m_configCustom, true, null);
        }

    /**
    * Test the behavior of the default load balancing policy.
    */
    @Test
    public void testDefault()
        {
        verifyLB("Default", m_configDefault, true, null);
        }

    /**
     * Test case for COH-5207 using a "greedy" load balancer.
     */
    @Test
    public void testGreedy()
        {
        verifyLB("Greedy", m_configGreedy, false, null);
        }

    /**
    * Test the behavior of the proxy load balancing policy.
    */
    @Test
    public void testProxy()
        {
        verifyLB("Proxy", m_configProxy, true, null);
        }

    /**
     * Test that connections are rejected once all servers are at their
     * connection limit.
     */
    @Test
    public void testConnectionLimitExceeded()
        {
        String sServer1 = getClass().getSimpleName() + "ConnLimit-1";
        String sServer2 = getClass().getSimpleName() + "ConnLimit-2";

        try
            {
            Properties props = new Properties();
            props.setProperty("test.extend.connection.limit", "1");

            // start the first proxy server
            setPortBefore1(props);
            CoherenceClusterMember clusterMember1 = startCacheServer(sServer1, "extend",
                                                    m_configDefault, props);

            // start the second proxy server
            setPortBefore2(props);
            CoherenceClusterMember clusterMember2 = startCacheServer(sServer2, "extend",
                                                    m_configDefault, props);

            Eventually.assertThat(invoking(clusterMember1).isServiceRunning(EXTEND_INVOCATION_SERVICE), is(true));
            Eventually.assertThat(invoking(clusterMember2).isServiceRunning(EXTEND_INVOCATION_SERVICE), is(true));

            setPortAfter(sServer1);

            // create two connections bringing both proxies to their connection limit
            CacheService service1 = (CacheService) getFactory().ensureService("ExtendTcpCacheService1");

            // give the ProxyService MemberConfigMap a chance to stabilize
            // across the cluster. With certain test configurations
            // (e.g. NameService based) the client may initially connect
            // to a random node.
            // TODO: find a way to check via an Eventually
            sleep(2000L);

            CacheService service2 = (CacheService) getFactory().ensureService("ExtendTcpCacheService2");

            try
                {
                // attempt a 3rd connection which should fail
                getFactory().ensureService("ExtendTcpCacheService3");
                fail("ConnectException expected");
                }
            catch (ConnectionException e)
                {
                // expected
                }
            finally
                {
                service1.shutdown();
                service2.shutdown();
                }
            }
        finally
            {
            stopCacheServer(sServer1);
            stopCacheServer(sServer2);
            }
        }

    /**
    * Test that a custom load balancer is used when configured for one.
    */
    @Test
    public void testCustomIsUsed()
        {
        String  sServer = getClass().getSimpleName() + "Custom";

        try
            {
            Properties props = new Properties();

            setPortBefore1(props);
            CoherenceClusterMember clusterMember = startCacheServer(sServer, "extend", m_configCustom, props);

            Eventually.assertThat(invoking(clusterMember).isServiceRunning(EXTEND_INVOCATION_SERVICE), is(true));

            setPortAfter(sServer);

            ensureServiceMembers("ExtendTcpProxyService", 1);

            CacheService cacheService = (CacheService)
                    getFactory().ensureService("ExtendTcpCacheService1");

            // use invocation service to get the client member from the
            // the custom load balancer
            InvocationService invocationService = (InvocationService)
                    getFactory().ensureService("ExtendTcpCustomInvocationService");

            try
                {
                Map map = invocationService.query(new GetClientInvocable(), null);
                assertTrue(map != null);
                assertTrue(map.size() == 1);
                Object oMember = map.keySet().iterator().next();
                Object oResult = map.values().iterator().next();
                assertTrue(equals(oMember, oResult));
                }
            finally
                {
                cacheService.shutdown();
                invocationService.shutdown();
                }
            }
        finally
            {
            stopCacheServer(sServer);
            }
        }

    /**
    * Test case for COH-9100 using a custom load balancer.
    */
    @Test
    public void testSubject()
        {
        String sServer1 = getClass().getSimpleName() + "Subject-1";

        try
            {
            Properties props = new Properties();

            // start just one proxy server for this test - no redirects needed
            setPortBefore1(props);
            CoherenceClusterMember clusterMember = startCacheServer(sServer1, "extend", m_configCustom, props);

            Eventually.assertThat(invoking(clusterMember).isServiceRunning(EXTEND_INVOCATION_SERVICE), is(true));

            setPortAfter(sServer1);

            ensureServiceMembers("ExtendTcpProxyService", 1);

            final Subject subject = new Subject();
            subject.getPrincipals().add(new PofPrincipal("CN=Manager, OU=MyUnit"));

            try
                {
                Subject.doAs(subject, new PrivilegedAction()
                    {
                    public Object run()
                        {
                        // running as Subject; use invocation service to get
                        // the Subject from the custom load balancer
                        InvocationService service = (InvocationService)
                                getFactory().ensureService("ExtendTcpCustomInvocationService");
                        try
                            {
                            Map map = service.query(new GetSubjectInvocable(), null);
                            assertNotNull(map);
                            assertEquals(1, map.size());
                            Object oResult = map.values().iterator().next();
                            assertNotNull("Subject should not be null", oResult);

                            Principal p1 = subject.getPrincipals().iterator().next();
                            Principal p2 = ((javax.security.auth.Subject)
                                    oResult).getPrincipals().iterator().next();
                            assertEquals(p1.getName(), p2.getName());
                            }
                        finally
                            {
                            service.shutdown();
                            }

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
            }
        finally
            {
            stopCacheServer(sServer1);
            }
        }


    // ----- helper methods -------------------------------------------------

    /**
    * Ensure that that the given number of cluster Members are running the
    * specified Service.
    *
    * @param sName    the name of the target Service
    * @param cMember  the desired number of Service Members
    */
    protected void ensureServiceMembers(String sName, int cMember)
        {
        InvocationService service = (InvocationService) getFactory()
                .ensureService("ExtendTcpInvocationService");
        try
            {
            Eventually.assertThat(invoking(this).getMemberCount(service, sName), is(cMember));
            }
        catch (Exception e)
            {
            throw new IllegalStateException("failed to ensure " + cMember
                + " instances of Service \"" + sName + "\"");
            }
        finally
            {
            service.stop();
            }
        }

    public int getMemberCount(InvocationService service, String sName)
        {
        Map map = service.query(new GetServiceMemberCount(sName), null);
        if (!map.isEmpty())
            {
            Integer IMember = (Integer) map.values().iterator().next();
            return IMember == null ? 0 : IMember.intValue();
            }
        return 0;
        }

    /**
     * Verify whether or not client connections are load balanced for a
     * given load balancing configuration.
     *
     * @param sServerName     server name to use
     * @param sConfigFile     server cache configuration
     * @param fShouldBalance  whether the connections should or should not
     *                        be load balanced
     * @param props           cache server system properties
     */
    protected void verifyLB(String sServerName, String sConfigFile,
                            boolean fShouldBalance, Properties props)
        {
        String sServer1 = getClass().getSimpleName() + sServerName + "-1";
        String sServer2 = getClass().getSimpleName() + sServerName + "-2";

        try
            {
            if (props == null)
                {
                props = new Properties();
                }

            // start the first proxy server
            setPortBefore1(props);
            CoherenceClusterMember clusterMember1 = startCacheServer(sServer1, "extend",
                                                    sConfigFile, props);

            Eventually.assertThat(invoking(clusterMember1).isServiceRunning(EXTEND_INVOCATION_SERVICE), is(true));

            // start the second proxy server
            setPortBefore2(props);
            CoherenceClusterMember clusterMember2 = startCacheServer(sServer2, "extend",
                                                    sConfigFile, props);

            Eventually.assertThat(invoking(clusterMember2).isServiceRunning(EXTEND_INVOCATION_SERVICE), is(true));

            setPortAfter(sServer1);

            ensureServiceMembers("ExtendTcpProxyService", 2);

            // test that the two remote services connect to a different
            // proxy service
            CacheService service1 = (CacheService) getFactory()
                    .ensureService("ExtendTcpCacheService1");

            // give the ProxyService MemberConfigMap a chance to stabilize
            // across the cluster. With certain test configurations
            // (e.g. NameService based) the client may initially connect
            // to a random node.
            // TODO: find a way to check via an Eventually
            sleep(2000L);

            CacheService service2 = (CacheService) getFactory()
                    .ensureService("ExtendTcpCacheService2");

            try
                {
                NamedCache cache1 = service1.ensureCache("local-test", null);
                NamedCache cache2 = service2.ensureCache("local-test", null);

                cache1.clear();
                cache2.clear();
                cache1.put("key", "value");

                if (fShouldBalance)
                    {
                    assertTrue("connected to the same proxy service",
                            equals(cache2.get("key"), null));
                    }
                else
                    {
                    assertTrue("connected to a different proxy service",
                            equals(cache2.get("key"), "value"));
                    }
                }
            finally
                {
                service1.shutdown();
                service2.shutdown();
                }
            }
        finally
            {
            stopCacheServer(sServer1);
            stopCacheServer(sServer2);
            }
        }


    // ----- inner class: GetClientInvocable --------------------------------

    /**
    * Invocable implementation that returns a client of the load balancer.
    */
    public static class GetClientInvocable
            extends AbstractInvocable
            implements PortableObject
        {
        // ----- constructors ---------------------------------------------

        /**
        * Default constructor.
        */
        public GetClientInvocable()
            {
            }


        // ----- Invocable interface --------------------------------------

        /**
        * {@inheritDoc}
        */
        public void run()
            {
            setResult(TestProxyServiceLoadBalancer.getClient());
            }


        // ----- PortableObject interface ---------------------------------

        /**
        * {@inheritDoc}
        */
        public void readExternal(PofReader in)
                throws IOException
            {
            setResult(in.readObject(0));
            }

        /**
        * {@inheritDoc}
        */
        public void writeExternal(PofWriter out)
                throws IOException
            {
            out.writeObject(0, getResult());
            }
        }


    // ----- inner class: EnsureServicesInvocable ---------------------------

    /**
    * Invocable implementation that returns the number of Members running
    * a specified Service.
    */
    public static class GetServiceMemberCount
            extends AbstractInvocable
            implements PortableObject
        {
        // ----- constructors ---------------------------------------------

        /**
        * Default constructor.
        */
        public GetServiceMemberCount()
            {
            }

        /**
        * Create a new GetServiceMemberCount that will return the number of
        * Members running the Service with the given name.
        *
        * @param sName  the name of the target Service
        */
        public GetServiceMemberCount(String sName)
            {
            m_sName = sName;
            }


        // ----- Invocable interface --------------------------------------

        /**
        * {@inheritDoc}
        */
        public void run()
            {
            Service service = CacheFactory.ensureCluster().getService(m_sName);

            int cMember = 0;
            if (service != null)
                {
                cMember = service.getInfo().getServiceMembers().size();
                }
            setResult(Integer.valueOf(cMember));
            }


        // ----- PortableObject interface ---------------------------------

        /**
        * {@inheritDoc}
        */
        public void readExternal(PofReader in)
                throws IOException
            {
            m_sName = in.readString(0);
            setResult(in.readObject(1));
            }

        /**
        * {@inheritDoc}
        */
        public void writeExternal(PofWriter out)
                throws IOException
            {
            out.writeString(0, m_sName);
            out.writeObject(1, getResult());
            }


        // ----- data members ---------------------------------------------

        /**
        * The name of the Service.
        */
        private String m_sName;
        }


    // ----- inner class: GetSubjectInvocable --------------------------------

    /**
    * Invocable implementation that returns the subject of the load balancer.
    */
    public static class GetSubjectInvocable
            extends AbstractInvocable
            implements PortableObject
        {
        // ----- constructors ---------------------------------------------

        /**
        * Default constructor.
        */
        public GetSubjectInvocable()
            {
            }

        // ----- Invocable interface --------------------------------------

        /**
        * {@inheritDoc}
        */
        public void run()
            {
            setResult(TestProxyServiceLoadBalancer.getSubject());
            }

        // ----- PortableObject interface ---------------------------------

        /**
        * {@inheritDoc}
        */
        public void readExternal(PofReader in)
                throws IOException
            {
            setResult(in.readObject(0));
            }

        /**
        * {@inheritDoc}
        */
        public void writeExternal(PofWriter out)
                throws IOException
            {
            out.writeObject(0, getResult());
            }
        }


    // ----- data members -------------------------------------------------

    public static final String EXTEND_INVOCATION_SERVICE = "ExtendTcpInvocationProxyService";

    /**
    * Server configuration files.
    */
    protected String m_configDefault;
    protected String m_configProxy;
    protected String m_configClient;
    protected String m_configGreedy;
    protected String m_configCustom;
    }
