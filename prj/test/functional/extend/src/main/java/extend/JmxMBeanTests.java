/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package extend;

import com.oracle.bedrock.options.Timeout;
import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;

import com.oracle.coherence.common.base.Exceptions;
import com.oracle.coherence.common.base.Logger;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.Cluster;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.Invocable;
import com.tangosol.net.InvocationService;

import com.tangosol.net.management.MBeanHelper;
import com.tangosol.net.management.Registry;

import com.tangosol.util.Base;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.management.Attribute;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static com.oracle.bedrock.testsupport.deferred.Eventually.assertThat;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.fail;

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
        assertThat(invoking(memberProxy).isServiceRunning("ExtendTcpProxyService"), is(true));
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
     * @param sMBeanType the type of MBean to examine (e.g. ConnectionManager and Connection)
     */
    public void testMessagingDebug(String sMBeanType)
        {
        InvocationService service = (InvocationService)
                getFactory().ensureService(INVOCATION_SERVICE_NAME);
        Eventually.assertDeferred(service::isRunning, is(true));

        try
            {
            // initially should be false
            MessagingDebugInvocable task = new MessagingDebugInvocable(sMBeanType, false);
            Map<?, ?>               map  = service.query(task, null);

            assertThat(map, is(notNullValue()));
            assertThat(map.size(), is(1));

            Object oMember = map.keySet().iterator().next();
            assertThat(service.getCluster().getLocalMember(), is(oMember));

            Object oResult = map.values().iterator().next();
            assertThat(oResult, is(instanceOf(Boolean.class)));
            assertThat(oResult, is(Boolean.FALSE));

            // make sure the attribute value can be changed
            task = new MessagingDebugInvocable(sMBeanType, true);
            map  = service.query(task, null);

            assertThat(map, is(notNullValue()));
            assertThat(map.size(), is(1));

            oMember = map.keySet().iterator().next();
            assertThat(service.getCluster().getLocalMember(), is(oMember));

            oResult = map.values().iterator().next();
            assertThat(oResult, is(instanceOf(Boolean.class)));
            assertThat(oResult, is(Boolean.TRUE));
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
        Eventually.assertDeferred(service::isRunning, is(true));

        try
            {
            MBeanInvocable task = new MBeanInvocable();
            Map<?, ?>      map  = service.query(task, null);

            assertThat(map, is(notNullValue()));
            assertThat(map.size(), is(1));

            Object oMember = map.keySet().iterator().next();
            Object oResult = map.values().iterator().next();
            assertThat(service.getCluster().getLocalMember(), is(oMember));

            assertThat(oResult, is(instanceOf(String.class)));
            assertThat(oResult, is(oMember.toString()));
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
        Eventually.assertDeferred(service::isRunning, is(true));

        try
            {
            MBeanHostIPInvocable task = new MBeanHostIPInvocable();
            Map<?, ?>            map  = service.query(task, null);

            assertThat(map, is(notNullValue()));
            assertThat(map.size(), is(1));

            Object oMember = map.keySet().iterator().next();
            assertThat(service.getCluster().getLocalMember(), is(oMember));

            Object oResult = map.values().iterator().next();
            assertThat(oResult, is(instanceOf(String.class)));

            String sResult = (String) oResult;
            int    dotIdx  = sResult.lastIndexOf('.');
            assertThat(dotIdx, is(not(-1)));

            String subPort = null;
            try
                {
                subPort = sResult.substring(dotIdx + 1);
                }
            catch (IndexOutOfBoundsException e)
                {
                fail("No sub-port when expected");
                }
            assertThat(subPort, is(notNullValue()));
            assertThat(subPort.length(), is(not(0)));
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
        @SuppressWarnings("unused")
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

        @Override
        public void init(InvocationService service)
            {
            assertThat(service.getInfo().getServiceType(), is(InvocationService.TYPE_REMOTE));
            m_service = service;
            }

        @Override
        public void run()
            {
            if (m_service != null)
                {
                Cluster  cluster  = CacheFactory.getCluster();
                Registry registry = cluster.getManagement();
                assertThat("JMX is disabled", registry, is(notNullValue()));

                MBeanServer server = MBeanHelper.findMBeanServer();
                try
                    {
                    Set<ObjectInstance> set = server.queryMBeans(new ObjectName("Coherence:*"), null);
                    for (ObjectInstance instance : set)
                        {
                        ObjectName objectName = instance.getObjectName();
                        if (objectName.toString().indexOf("type=" + m_sMBeanType + ',') > 0)
                            {
                            server.setAttribute(objectName, new Attribute("MessagingDebug", m_fEnableDebug));

                            setValue((Boolean) server.getAttribute(objectName, "MessagingDebug"));
                            break;
                            }
                        }
                    }
                catch (Exception e)
                    {
                    Base.log("JmxMBeanTests.MessagingDebugInvocable.run() got an exception: " + e);
                    throw ensureRuntimeException(e);
                    }
                }
            }

        @Override
        public Object getResult()
            {
            return m_fValue;
            }

        // ----- PortableObject interface ---------------------------------

        @Override
        public void readExternal(PofReader in)
                throws IOException
            {
            m_fEnableDebug = in.readBoolean(0);
            m_sMBeanType   = in.readString(1);
            }

        @Override
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
         * @param fValue the value of the attribute
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

        @Override
        public void init(InvocationService service)
            {
            assertThat(service.getInfo().getServiceType(), is(InvocationService.TYPE_REMOTE));
            m_service = service;
            }

        @Override
        public void run()
            {
            if (m_service != null)
                {
                Cluster  cluster  = CacheFactory.getCluster();
                Registry registry = cluster.getManagement();
                assertThat("JMX is disabled", registry, is(notNullValue()));

                MBeanServer server = MBeanHelper.findMBeanServer();

                Eventually.assertDeferred("Didn't find Connection MBean within timeout",
                        () -> findConnectionMBean(server), is(true), Timeout.after(1, TimeUnit.MINUTES));
                }
            else
                {
                Logger.warn("MBeanInvocable.run(), m_service is not initialized.");
                }
            }

        protected boolean findConnectionMBean(MBeanServer server)
            {
            try
                {
                Set<ObjectInstance> set = server.queryMBeans(new ObjectName("Coherence:*"), null);
                for (ObjectInstance instance : set)
                    {
                    ObjectName objectName = instance.getObjectName();
                    if (objectName.toString().indexOf("type=Connection,") > 0)
                        {
                        setValue(server.getAttribute(objectName, "Member").toString());
                        return true;
                        }
                    }
                return false;
                }
            catch (Exception e)
                {
                throw Exceptions.ensureRuntimeException(e);
                }
            }

        @Override
        public Object getResult()
            {
            return m_sValue;
            }

        // ----- PortableObject interface ---------------------------------

        @Override
        public void readExternal(PofReader in)
                throws IOException
            {
            m_sValue = in.readString(0);
            }

        @Override
        public void writeExternal(PofWriter out)
                throws IOException
            {
            out.writeString(0, m_sValue);
            }

        // ----- accessors ------------------------------------------------

        /**
         * Set the string value.
         *
         * @param sValue the value of the attribute
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

        @Override
        public void init(InvocationService service)
            {
            assertThat(service.getInfo().getServiceType(), is(InvocationService.TYPE_REMOTE));
            m_service = service;
            }

        @Override
        public void run()
            {
            if (m_service != null)
                {
                Cluster  cluster  = CacheFactory.getCluster();
                Registry registry = cluster.getManagement();
                assertThat("JMX is disabled", registry, is(notNullValue()));

                MBeanServer server = MBeanHelper.findMBeanServer();
                try
                    {
                    Set<ObjectInstance> set = server.queryMBeans(new ObjectName("Coherence:*"), null);
                    for (ObjectInstance instance : set)
                        {
                        ObjectName objectName = instance.getObjectName();
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

        @Override
        public Object getResult()
            {
            return m_sValue;
            }

        // ----- PortableObject interface ---------------------------------

        @Override
        public void readExternal(PofReader in)
                throws IOException
            {
            m_sValue = in.readString(0);
            }

        @Override
        public void writeExternal(PofWriter out)
                throws IOException
            {
            out.writeString(0, m_sValue);
            }

        // ----- accessors ------------------------------------------------

        /**
         * Set the string value.
         *
         * @param sValue the value of the attribute
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