/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package extend;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.Cluster;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.Invocable;
import com.tangosol.net.InvocationService;

import com.tangosol.net.management.MBeanHelper;
import com.tangosol.net.management.Registry;

import common.AbstractFunctionalTest;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.management.Attribute;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

/**
* A collection of JMX MBean functional tests for Coherence*Extend.
*
* @author lh  2011.01.20
*/
public class JmxMBeanTests
        extends AbstractFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public JmxMBeanTests()
        {
        super(AbstractExtendTests.FILE_CLIENT_CFG_CACHE);
        }


    // ----- test lifecycle -------------------------------------------------

    /**
    * Initialize the test class.
    */
    @BeforeClass
    public static void startup()
        {
        Properties propsMain = new Properties();
        propsMain.put("coherence.management", "all");
        propsMain.put("coherence.management.remote", "true");
        System.getProperties().putAll(propsMain);

        CoherenceClusterMember memberProxy = startCacheServer("JmxMBeanTests", "extend",
                                                AbstractExtendTests.FILE_SERVER_CFG_CACHE);
        Eventually.assertThat(invoking(memberProxy).isServiceRunning("ExtendTcpProxyService"), is(true));
        }

    /**
    * Shutdown the test class.
    */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("JmxMBeanTests");
        }


    // ----- InvocationService tests ----------------------------------------

    /**
     * Test the ConnectionManager MBean MessagingDebug attribute
     */
    @Test
    public void testConnectionManagerMessagingDebug()
        {
        testMessagingDebug("ConnectionManager");
        }

    /**
     * Test the Connection MBean MessagingDebug attribute
     */
    @Test
    public void testConnectionMessagingDebug()
        {
        testMessagingDebug("Connection");
        }

    /**
     * Run the {@link MessagingDebugInvocable} to set the attribute on the specified MBean.
     *
     * @param sMBeanType  the type of MBean to examine (e.g. ConnectionManager and Connection)
     */
    public void testMessagingDebug(String sMBeanType)
        {
        InvocationService service = (InvocationService)
            getFactory().ensureService(INVOCATION_SERVICE_NAME);

        try
            {
            // initially should be false
            MessagingDebugInvocable task = new MessagingDebugInvocable(sMBeanType, false);
            Map                     map  = service.query(task, null);

            assertTrue(map != null);
            assertTrue(map.size() == 1);

            Object oMember = map.keySet().iterator().next();
            assertTrue(equals(oMember, service.getCluster().getLocalMember()));

            Object oResult = map.values().iterator().next();
            assertTrue(oResult instanceof Boolean);
            assertTrue(oResult.equals(false));

            // make sure the attribute value can be changed
            task = new MessagingDebugInvocable(sMBeanType, true);
            map  = service.query(task, null);

            assertTrue(map != null);
            assertTrue(map.size() == 1);

            oMember = map.keySet().iterator().next();
            assertTrue(equals(oMember, service.getCluster().getLocalMember()));

            oResult = map.values().iterator().next();
            assertTrue(oResult instanceof Boolean);
            assertTrue(oResult.equals(true));
            }
        finally
            {
            service.shutdown();
            }
        }

    /**
    * Query MBean attribute on the server using
    * {@link InvocationService#query(Invocable, Set)}.
    */
    @Test
    public void queryMBean()
        {
        InvocationService service = (InvocationService)
                getFactory().ensureService(INVOCATION_SERVICE_NAME);

        try
            {
            MBeanInvocable task = new MBeanInvocable();
            Map            map  = service.query(task, null);

            assertTrue(map != null);
            assertTrue(map.size() == 1);

            Object oMember = map.keySet().iterator().next();
            assertTrue(equals(oMember, service.getCluster().getLocalMember()));

            Object oKey    = map.keySet().iterator().next();
            Object oResult = map.values().iterator().next();
            assertTrue(oResult instanceof String);
            assertTrue(oResult.equals(oKey.toString()));
            }
        finally
            {
            service.shutdown();
            }
        }
    /**
    * Query MBean attribute on the server using
    * {@link InvocationService#query(Invocable, Set)}.
    */
    @Test
    public void queryHostIPMBean()
        {
        InvocationService service = (InvocationService)
                getFactory().ensureService(INVOCATION_SERVICE_NAME);

        try
            {
            MBeanHostIPInvocable task = new MBeanHostIPInvocable();
            Map            map  = service.query(task, null);

            assertTrue(map != null);
            assertTrue(map.size() == 1);

            Object oMember = map.keySet().iterator().next();
            assertTrue(equals(oMember, service.getCluster().getLocalMember()));

            Object oResult = map.values().iterator().next();
            assertTrue(oResult instanceof String);
            String sResult = (String) oResult;
            int dotIdx = sResult.lastIndexOf('.');
            assertTrue(dotIdx != -1);
            String subPort = null;
            try
                {
                subPort = sResult.substring(dotIdx+1);
                }
            catch (IndexOutOfBoundsException e)
                {
                fail("No subport when expected");
                }
            assertTrue(subPort.length() != 0);
            }
        finally
            {
            service.shutdown();
            }
        }

    // ----- inner class: MessagingDebugInvocable -----------------------------

    /**
     * Invocable implementation that tests and sets the MessagingDebug attribute on
     * an MBean.
     */
    public static class MessagingDebugInvocable
        implements Invocable, PortableObject
        {
        // ----- constructors ---------------------------------------------

        /**
         * Default constructor.
         */
        public MessagingDebugInvocable()
            {
            }

        /**
         * Default constructor.
         */
        public MessagingDebugInvocable(String sMBeanType, boolean fEnableDebug)
            {
            m_fEnableDebug = fEnableDebug;
            m_sMBeanType   = sMBeanType;
            }

        // ----- Invocable interface --------------------------------------

        /**
         * {@inheritDoc}
         */
        public void init(InvocationService service)
            {
            assertTrue(service.getInfo().getServiceType()
                .equals(InvocationService.TYPE_REMOTE));
            m_service = service;
            }

        /**
         * {@inheritDoc}
         */
        public void run()
            {
            if (m_service != null)
                {
                Cluster cluster   = CacheFactory.getCluster();
                Registry registry = cluster.getManagement();
                assertTrue("JMX is disabled", registry != null);

                MBeanServer server = MBeanHelper.findMBeanServer();
                try
                    {
                    Set set = server.queryMBeans(new ObjectName("Coherence:*"), null);
                    for (Iterator iter = set.iterator(); iter.hasNext();)
                        {
                        ObjectInstance instance   = (ObjectInstance) iter.next();
                        ObjectName     objectName = instance.getObjectName();
                        if (objectName.toString().indexOf("type=" + m_sMBeanType + ',') > 0)
                            {
                            server.setAttribute(objectName, new Attribute("MessagingDebug", Boolean.valueOf(m_fEnableDebug)));

                            setValue((Boolean) server.getAttribute(objectName, "MessagingDebug"));
                            break;
                            }
                        }
                    }
                catch (Exception e)
                    {
                    throw ensureRuntimeException(e);
                    }
                }
            }

        /**
         * {@inheritDoc}
         */
        public Object getResult()
            {
            return Boolean.valueOf(m_fValue);
            }

        // ----- PortableObject interface ---------------------------------

        /**
         * {@inheritDoc}
         */
        public void readExternal(PofReader in)
            throws IOException
            {
            m_fEnableDebug = in.readBoolean(0);
            m_sMBeanType   = in.readString(1);
            }

        /**
         * {@inheritDoc}
         */
        public void writeExternal(PofWriter out)
            throws IOException
            {
            out.writeBoolean(0, m_fEnableDebug);
            out.writeString(1, m_sMBeanType);
            }

        // ----- accessors ------------------------------------------------

        /**
         * Set the boolean value.
         *
         * @param fValue  the value of the attribute
         */
        public void setValue(boolean fValue)
            {
            m_fValue = fValue;
            }

        // ----- data members ---------------------------------------------

        /**
         * The value of the attribute.
         */
        private transient boolean m_fValue;

        /**
         * Value to set the debug flag to.
         */
        private boolean m_fEnableDebug;

        /**
         * The MBean type
         */
        private String m_sMBeanType;

        /**
         * The InvocationService that is executing this Invocable.
         */
        private transient InvocationService m_service;
        }

    // ----- inner class: MBeanInvocable --------------------------------------

    /**
    * Invocable implementation that queries the Member attribute of the
    * ConnectionMBean and returns the string value.
    */
    public static class MBeanInvocable
            implements Invocable, PortableObject
        {
        // ----- constructors ---------------------------------------------

        /**
        * Default constructor.
        */
        public MBeanInvocable()
            {
            }

        // ----- Invocable interface --------------------------------------

        /**
        * {@inheritDoc}
        */
        public void init(InvocationService service)
            {
            assertTrue(service.getInfo().getServiceType()
                    .equals(InvocationService.TYPE_REMOTE));
            m_service = service;
            }

        /**
        * {@inheritDoc}
        */
        public void run()
            {
            if (m_service != null)
                {
                Cluster cluster   = CacheFactory.getCluster();
                Registry registry = cluster.getManagement();
                assertTrue("JMX is disabled", registry != null);

                MBeanServer server = MBeanHelper.findMBeanServer();
                try
                    {
                    Set set = server.queryMBeans(new ObjectName("Coherence:*"), null);
                    for (Iterator iter = set.iterator(); iter.hasNext();)
                        {
                        ObjectInstance instance   = (ObjectInstance) iter.next();
                        ObjectName     objectName = instance.getObjectName();
                        if (objectName.toString().indexOf("type=Connection,") > 0)
                            {
                            setValue(server.getAttribute(objectName, "Member").toString());
                            break;
                            }
                        }
                    }
                catch (Exception e)
                    {
                    throw ensureRuntimeException(e);
                    }
                }
            }

        /**
        * {@inheritDoc}
        */
        public Object getResult()
            {
            return m_sValue;
            }

        // ----- PortableObject interface ---------------------------------

        /**
        * {@inheritDoc}
        */
        public void readExternal(PofReader in)
                throws IOException
            {
            m_sValue = in.readString(0);
            }

        /**
        * {@inheritDoc}
        */
        public void writeExternal(PofWriter out)
                throws IOException
            {
            out.writeString(0, m_sValue);
            }

        // ----- accessors ------------------------------------------------

        /**
        * Set the string value.
        *
        * @param sValue  the value of the attribute
        */
        public void setValue(String sValue)
            {
            m_sValue = sValue;
            }

        // ----- data members ---------------------------------------------

        /**
        * The string value of the attribute.
        */
        private String m_sValue;

        /**
        * The InvocationService that is executing this Invocable.
        */
        private transient InvocationService m_service;
        }

    // ----- inner class: MBeanHostIPInvocable --------------------------------------

    /**
    * Invocable implementation that queries the Member attribute of the
    * ConnectionMBean and returns the string value.
    */
    public static class MBeanHostIPInvocable
            implements Invocable, PortableObject
        {
        // ----- constructors ---------------------------------------------

        /**
        * Default constructor.
        */
        public MBeanHostIPInvocable()
            {
            }

        // ----- Invocable interface --------------------------------------

        /**
        * {@inheritDoc}
        */
        public void init(InvocationService service)
            {
            assertTrue(service.getInfo().getServiceType()
                    .equals(InvocationService.TYPE_REMOTE));
            m_service = service;
            }

        /**
        * {@inheritDoc}
        */
        public void run()
            {
            if (m_service != null)
                {
                Cluster cluster   = CacheFactory.getCluster();
                Registry registry = cluster.getManagement();
                assertTrue("JMX is disabled", registry != null);

                MBeanServer server = MBeanHelper.findMBeanServer();
                try
                    {
                    Set set = server.queryMBeans(new ObjectName("Coherence:*"), null);
                    for (Iterator iter = set.iterator(); iter.hasNext();)
                        {
                        ObjectInstance instance   = (ObjectInstance) iter.next();
                        ObjectName     objectName = instance.getObjectName();
                        if (objectName.toString().indexOf("type=ConnectionManager,name=ExtendTcpProxyServiceJMX,") > 0)
                            {
                            setValue(server.getAttribute(objectName, "HostIP").toString());
                            break;
                            }
                        }
                    }
                catch (Exception e)
                    {
                    throw ensureRuntimeException(e);
                    }
                }
            }

        /**
        * {@inheritDoc}
        */
        public Object getResult()
            {
            return m_sValue;
            }

        // ----- PortableObject interface ---------------------------------

        /**
        * {@inheritDoc}
        */
        public void readExternal(PofReader in)
                throws IOException
            {
            m_sValue = in.readString(0);
            }

        /**
        * {@inheritDoc}
        */
        public void writeExternal(PofWriter out)
                throws IOException
            {
            out.writeString(0, m_sValue);
            }

        // ----- accessors ------------------------------------------------

        /**
        * Set the string value.
        *
        * @param sValue  the value of the attribute
        */
        public void setValue(String sValue)
            {
            m_sValue = sValue;
            }

        // ----- data members ---------------------------------------------

        /**
        * The string value of the attribute.
        */
        private String m_sValue;

        /**
        * The InvocationService that is executing this Invocable.
        */
        private transient InvocationService m_service;
        }

    // ----- constants ------------------------------------------------------

    /**
    * The name of the InvocationService used by all test methods.
    */
    public static String INVOCATION_SERVICE_NAME = "ExtendTcpInvocationService";
    }