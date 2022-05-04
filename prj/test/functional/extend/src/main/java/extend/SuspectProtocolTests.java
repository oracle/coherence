/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package extend;


import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.tangosol.coherence.component.util.SafeService;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.ProxyService;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.peer.acceptor.TcpAcceptor;

import com.tangosol.io.pof.PortableObject;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Invocable;
import com.tangosol.net.InvocationService;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Service;

import com.tangosol.net.messaging.ConnectionAcceptor;

import com.tangosol.util.Filter;
import com.tangosol.util.extractor.IdentityExtractor;
import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.AnyFilter;
import com.tangosol.util.filter.LessFilter;


import com.oracle.coherence.testing.AbstractFunctionalTest;
import com.oracle.coherence.testing.TestMapListener;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


import java.io.IOException;

import java.util.HashMap;
import java.util.Map;

/**
 * Test messages output from suspect protocol.
 * This test depends on a property on the proxy's TcpConnection
 * that disables outgoing messages, so the proxy
 * gets overloaded and suspects the client.
 * This causes the suspect protocol to disconnect the client.
 *
 * @author par 9/6/13
 * @since @BUILDVERSION@
 */
public class SuspectProtocolTests
    extends AbstractFunctionalTest
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public SuspectProtocolTests()
        {
        super("client-cache-config-suspect-protocol.xml");
        m_fDebugging = false;
        }

    // ----- test lifecycle -------------------------------------------------

    /**
     * Initialize the test class.
     */
    @BeforeClass
    public static void startup()
        {
        CoherenceClusterMember memberProxy = startCacheServer("SuspectProtocolTests", "extend",
                                                     "server-cache-config-suspect-protocol.xml");
        Eventually.assertThat(invoking(memberProxy).isServiceRunning("ExtendTcpProxyService"), is(true));
        }

    /**
     * Shutdown the test class.
     */
    @AfterClass
    public static void shutdown()
        {
        try
            {
            stopCacheServer("SuspectProtocolTests");
            }
        catch (Exception e)
           {}
        }

    // ----- Suspect protocol tests ------------------------------------------------

    /**
    * Force suspect protocol to disconnect the client.
    */
    @Test
    public void testCOH9843()
        {
        debug("SuspectProtocolTests.testCoh9843(); starting");
        InvocationService service;
        NamedCache        cache;

        try
            {
            cache = getNamedCache("trv-test");
            addListeners(cache, sKey);

            // disable sends on proxy
            service = (InvocationService) getFactory().ensureService("ExtendTcpInvocationService");
            service.query(new DisableSendInvocable("ExtendTcpProxyService"), null);

            debug("SuspectProtocolTests.testCoh9843(); doing 300000 putall");
            TestBulkInvocable task = new TestBulkInvocable("trv-test", 300000);
            Map               map  = service.query(task, null);
            debug("SuspectProtocolTests.testCoh9843(); after doing 300000 putall");
            }
        catch (Exception e3)
            {
            debug("SuspectProtocolTests.testCOH9843(); caught exception: " + e3);
            fail("SuspectProtocolTests, unexpected exception received: "+e3);
            }

        debug("SuspectProtocolTests.testCoh9843(); leaving");
        }

    /**
     * Add listeners to the cache.  The logging message outputs
     * 50 listeners, so add more than that to verify it stops
     * at 50.
     *
     * @param oKey  a key for the key listener  
     */
    private void addListeners(NamedCache cache, Object oKey)
        {
        cache.addMapListener(new TestMapListener(), AlwaysFilter.INSTANCE, false);
        cache.addMapListener(new TestMapListener(), oKey, false);
        cache.addMapListener(new TestMapListener(), new AnyFilter(new Filter[] {AlwaysFilter.INSTANCE}), false);
        cache.addMapListener(new TestMapListener());
        cache.addMapListener(new TestMapListener(), new LessFilter(IdentityExtractor.INSTANCE, 1000), true);
        for (int i = 6; i < 76; i++)
            {
            cache.addMapListener(new TestMapListener(), new LessFilter(IdentityExtractor.INSTANCE, i), true);
            }
        }

    /**
     * accessor to output debugging statements
     */
    private void debug(String msg)
        {
        if (m_fDebugging)
            {
            System.out.println(msg);
            }
        }

    // ----- inner class: TestBulkInvocable -------------------------------------

    /**
     * Invocable implementation sends a batch of messages.
     */
    public static class TestBulkInvocable
            implements Invocable, PortableObject
        {
        // ----- constructors ---------------------------------------------

        /**
         * Default constructor.
         */
        public TestBulkInvocable()
            {
            }

        /**
         * Constructor for proxy service name and number of messages to send.
         */
        public TestBulkInvocable(String cacheName, int count)
            {
            m_cacheName = cacheName;
            m_nCount    = count;
            }

        // ----- Invocable interface --------------------------------------

        /**
         * {@inheritDoc}
         */
        public void init(InvocationService service)
            {
            assertTrue(service.getInfo().getServiceType().equals(InvocationService.TYPE_REMOTE));
            m_service = service;
            }

        /**
         * {@inheritDoc}
         */
        public void run()
            {
            if (m_service != null)
                {
                NamedCache cache = CacheFactory.getCache(m_cacheName);
                HashMap    map   = new HashMap();
                for (int i = 0; i < m_nCount; i++)
                    {
                    map.put(i, i);
                    }
                cache.putAll(map);
                }
            }

        /**
        * {@inheritDoc}
        */
        public Object getResult()
            {
            return null;
            }

        // ----- PortableObject interface ---------------------------------

        /**
        * {@inheritDoc}
        */
        public void readExternal(PofReader in)
                throws IOException
            {
            m_cacheName = in.readString(0);
            m_nCount    = in.readInt(1);
            }

        /**
        * {@inheritDoc}
        */
        public void writeExternal(PofWriter out)
                throws IOException
            {
            out.writeString(0, m_cacheName);
            out.writeInt(1, m_nCount);
            }

        // ----- data members ---------------------------------------------

        /**
         * The proxy service on which to disable outgoing messages.
         */
        private String m_cacheName;

        /**
         * Number of messages to send.
         */
        private int m_nCount;

        /**
         * The InvocationService that is executing this Invocable.
         */
        private transient InvocationService m_service;
        }

    // ----- inner class: DisableSendInvocable -------------------------------------

    public static class DisableSendInvocable
            implements Invocable, PortableObject
        {

        // ----- constructors ---------------------------------------------------

        /**
         * Default constructor.
         */
        public DisableSendInvocable()
            {
            }

        /**
         * Constructor for naming service to disable.
         */
        public DisableSendInvocable(String serviceName)
            {
            m_sServiceName = serviceName;
            }

        // ----- Invocable interface --------------------------------------------

        /**
         * {@inheritDoc}
         */
        public void init(InvocationService service)
            {
            }

        /**
         * {@inheritDoc}
         */
        public void run()
            {
            Service service = ((SafeService) CacheFactory.getService(m_sServiceName)).getService();
            ((TcpAcceptor.TcpConnection) ((ConnectionAcceptor) ((ProxyService) service).getAcceptor()).getConnections().iterator().next()).setOutgoingDisabled(true);
            }

        /**
         * {@inheritDoc}
         */
        public Object getResult()
            {
            return "Test";
            }

        // ----- PortableObject interface ---------------------------------------

        /**
         * {@inheritDoc}
         */
        public void readExternal(PofReader in)
                throws IOException
            {
            m_sServiceName = in.readString(0);
            }

        /**
         * {@inheritDoc}
         */
        public void writeExternal(PofWriter out)
                throws IOException
            {
            out.writeString(0, m_sServiceName);
            }

        // ----- data members ------------------------------------------------

        private String m_sServiceName;
        }

    // ----- data members ------------------------------------------------

    /**
     * Flag whether to output debug messages.
     */
    private boolean m_fDebugging;

    /**
     * Test key.
     */
    static public String sKey = "blah";
    }
