/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package extend;


import com.oracle.coherence.common.internal.net.MultiplexedSocketProvider;
import com.oracle.coherence.common.net.InetSocketAddress32;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.coherence.common.net.SocketProvider;
import com.tangosol.coherence.component.net.extend.connection.TcpConnection;
import com.tangosol.coherence.component.net.extend.remoteService.RemoteCacheService;

import com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.initiator.TcpInitiator;

import com.tangosol.coherence.component.util.safeService.SafeCacheService;

import com.tangosol.net.CacheService;
import com.tangosol.net.NamedCache;

import com.tangosol.net.SocketProviderFactory;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import java.net.Socket;

import java.util.Properties;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;


/**
* A collection of functional tests for the Coherence*Extend load balancing
* policies.
*
* @author jh  2010.12.13
*/
public class LoadBalancerTests
        extends AbstractLoadBalancerTests
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public LoadBalancerTests()
        {
        super("client-cache-config-lb.xml");
        m_configDefault          = "server-cache-config-lb-default.xml";
        m_configProxy            = "server-cache-config-lb-proxy.xml";
        m_configClient           = "server-cache-config-lb-client.xml";
        m_configGreedy           = "server-cache-config-lb-greedy.xml";
        m_configCustom           = "server-cache-config-lb-custom.xml";
        m_configRemoteMember     = "server-cache-config-remotemember.xml";
        m_configCustomComparator = "server-cache-config-lb-custom-comparator.xml";
        }


    // ----- test lifecycle -------------------------------------------------

    /**
    * Initialize the test class.
    */
    @BeforeClass
    public static void startup()
        {
        // generate a unique alternative TCP/IP port
        int nPort1 = Integer.getInteger("test.extend.port", 9999);
        int nPort2 = nPort1 + 1;

        Properties props = System.getProperties();
        props.setProperty("test.extend.port.1", String.valueOf(nPort1));
        props.setProperty("test.extend.port.2", String.valueOf(nPort2));
        }


    // ------- AbstractLoadBalancerTests methods ----------------------------

    /**
    * {@inheritDoc}
    */
    void setPortBefore1(Properties props)
        {
        props.setProperty("test.extend.port",
                System.getProperty("test.extend.port.1"));
        }

    /**
    * {@inheritDoc}
    */
    void setPortBefore2(Properties props)
        {
        props.setProperty("test.extend.port",
                System.getProperty("test.extend.port.2"));
        }

    /**
    * {@inheritDoc}
    */
    void setPortAfter(String sServerName) {}


    // ----- test methods ---------------------------------------------------

    /**
    * Test pinging the ProxyServer from a non-Extend client.
    */
    @Test
    public void testPing()
            throws IOException
        {
        String sAddr  = System.getProperty("test.extend.address.remote", "127.0.0.1");
        int    nPort  = Integer.getInteger("test.extend.port", 9999);
        Socket socket = null;
        try
            {
            // start the proxy server
            CoherenceClusterMember memberProxy = startCacheServer("LoadBalancerTestsPing", "extend", m_configClient);
            Eventually.assertThat(invoking(memberProxy).isServiceRunning("ExtendTcpProxyService"), is(true));

            // test MultiplexedSocketProvider.getAddressString()
            socket = new Socket(sAddr, nPort);
            try
                {
                SocketProvider provider = SocketProviderFactory.DEFAULT_SOCKET_PROVIDER;
                assertTrue(provider instanceof MultiplexedSocketProvider);
                assertEquals(provider.getAddressString(socket), "127.0.0.1:" + nPort);
                }
            catch (Exception e)
                {
                fail(e.getMessage());
                }

            // send a ping request
            socket.getOutputStream().write(PING_REQUEST);

            // read the ping response
            int    cb = PING_RESPONSE.length;
            byte[] ab = new byte[cb];
            for (int of = 0; of < cb; )
                {
                int c = socket.getInputStream().read(ab, of, cb - of);
                if (c < 0)
                    {
                    break;
                    }
                else
                    {
                    of += c;
                    }
                }

            // validate the response
            assertTrue(socket.getInputStream().available() == 0);
            for (int i = 0; i < cb; ++i)
                {
                assertEquals("At index=" + i, PING_RESPONSE[i], ab[i]);
                }

            // send a query ping request
            socket.getOutputStream().write(PING_QUERY_REQUEST);

            // read the query ping response
            cb = PING_QUERY_RESPONSE.length;
            ab = new byte[cb];
            for (int of = 0; of < cb; )
                {
                int c = socket.getInputStream().read(ab, of, cb - of);
                if (c < 0)
                    {
                    break;
                    }
                else
                    {
                    of += c;
                    }
                }

            // validate the query response
            assertTrue(socket.getInputStream().available() == 0);
            for (int i = 0; i < 24; ++i)
                {
                assertEquals("At index=" + i, PING_QUERY_RESPONSE[i], ab[i]);
                }

            int    nPortLen = Integer.toString(nPort).length();
            byte[] abPort   = new byte[nPortLen];
            int    nOffset  = 24;
            System.arraycopy(ab, nOffset, abPort, 0, nPortLen);
            assertEquals(nPort, (int) Integer.valueOf(new String(abPort)));
            assertEquals(PING_QUERY_RESPONSE[PING_QUERY_RESPONSE.length - 1], ab[nOffset + nPortLen]);
            }
        finally
            {
            if (socket != null)
                {
                try
                    {
                    socket.close();
                    }
                catch (IOException e) { /*ignore*/ }
                }
            stopCacheServer("LoadBalancerTestsPing");
            }
        }

    /**
    * Test the behavior of the proxy load balancing policy.
    */
    @Test
    public void testRemoteMember()
        {
        String sServer = getClass().getSimpleName() + "RemoteMember";
        try
            {
            Properties props = new Properties();

            // start the proxy server
            setPortBefore1(props);
            CoherenceClusterMember memberProxy = startCacheServer(sServer, "extend", m_configRemoteMember, props);
            Eventually.assertThat(invoking(memberProxy).isServiceRunning("ExtendTcpProxyService"), is(true));
            Eventually.assertThat(invoking(memberProxy).isServiceRunning("ExtendTcpProxyService2"), is(true));

            setPortAfter(sServer);

            // test that the two remote services connect to a different
            // proxy service
            SafeCacheService service = (SafeCacheService) getFactory().ensureService("ExtendTcpCacheService1");
            try
                {
                RemoteCacheService  remote     = (RemoteCacheService) service.getService();
                TcpInitiator        initiator  = (TcpInitiator) remote.getInitiator();
                TcpConnection       connection = (TcpConnection) initiator.getConnection();
                InetSocketAddress32 addr       = (InetSocketAddress32) connection.getSocket().getRemoteSocketAddress();

                int portOriginal  = Integer.parseInt(props.getProperty("test.extend.port"));
                int portConnected = addr.getPort();

                assertFalse("connected to the original port",
                        equals(portOriginal, portConnected));
                }
            finally
                {
                service.shutdown();
                }
            }
        finally
            {
            stopCacheServer(sServer);
            }
        }

    /**
    * COH-10196; Test the behavior of default proxy load balancing policy.
    * Implementation changed to call out to a default comparator that should
    * demonstration the default behavior.
    */
    @Test
    public void testProxyCustomComparator()
        {
        String sServer1 = getClass().getSimpleName() + "Comparator-1";
        String sServer2 = getClass().getSimpleName() + "Comparator-2";
        try
            {
            Properties props = new Properties();

            // start the first proxy server
            setPortBefore1(props);
            CoherenceClusterMember memberProxy1 = startCacheServer(sServer1, "extend", m_configCustomComparator, props);
            Eventually.assertThat(invoking(memberProxy1).isServiceRunning("ExtendTcpProxyService"), is(true));

            // start the second proxy server
            setPortBefore2(props);
            CoherenceClusterMember memberProxy2 = startCacheServer(sServer2, "extend", m_configCustomComparator, props);
            Eventually.assertThat(invoking(memberProxy2).isServiceRunning("ExtendTcpProxyService"), is(true));

            setPortAfter(sServer1);

            // test that the two remote services connect to a different
            // proxy service
            CacheService service1 = (CacheService) getFactory()
                    .ensureService("ExtendTcpCacheService1");
            CacheService service2 = (CacheService) getFactory()
                    .ensureService("ExtendTcpCacheService2");

            try
                {
                NamedCache cache1 = service1.ensureCache("local-test", null);
                NamedCache cache2 = service2.ensureCache("local-test", null);

                cache1.clear();
                cache2.clear();
                cache1.put("key", "value");

                assertTrue("connected to same proxy service",
                        equals(cache2.get("key"), null));
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


    // ----- data members -------------------------------------------------

    /**
    * Server configuration files.
    */
    protected String m_configRemoteMember;

    /**
     * Custom comparator used to balance load.
     */
    protected String m_configCustomComparator;

    /**
    * Constants.
    */
    private static final byte[] PING_REQUEST        = new byte[] {7, 0, 3, 0, 0, 66, 0, 64};
    private static final byte[] PING_RESPONSE       = new byte[] {9, 0, 4, 3, 0, 66, 0, 3, 100, 64};
    private static final byte[] PING_QUERY_REQUEST  = new byte[] {7, 0, 3, 0, 0, 66, 1, 64};
    private static final byte[] PING_QUERY_RESPONSE = new byte[] {29, 0, 4, 3, 0, 66, 1, 2, 106, 4,
            85, 1, 78, 15, 49, 50, 55, 46, 48, 46, 48, 46, 49, 58, 49, 49, 49, 55, 53, 64};
    }
