/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package jmx;

import com.oracle.bedrock.io.FileHelper;

import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterBuilder;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.oracle.bedrock.runtime.concurrent.RemoteRunnable;

import com.oracle.bedrock.runtime.console.FileWriterApplicationConsole;

import com.oracle.bedrock.runtime.java.options.Headless;
import com.oracle.bedrock.runtime.java.options.IPv4Preferred;
import com.oracle.bedrock.runtime.options.DisplayName;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.tangosol.coherence.component.net.management.Gateway;
import com.tangosol.coherence.component.net.management.NotificationHandler;
import com.tangosol.coherence.component.net.management.listenerHolder.RemoteHolder;
import com.tangosol.coherence.component.net.management.model.LocalModel;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;

import com.tangosol.net.management.MBeanServerProxy;
import com.tangosol.net.management.Registry;

import com.tangosol.util.Base;

import com.oracle.coherence.testing.AbstractTestInfrastructure;

import org.junit.Rule;
import org.junit.rules.TestName;

import javax.management.AttributeChangeNotification;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;

import java.io.File;
import java.io.Serializable;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.concurrent.CountDownLatch;

import java.util.function.Predicate;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author jk  2018.09.13
 */
public abstract class BaseMBeanServerProxyNotificationTests
    {
    protected static CoherenceCluster startCluster(String sClusterName, int cMember) throws Exception
        {
        File folderOut = new File(AbstractTestInfrastructure.ensureOutputDir(JmxTests.PROJECT), sClusterName);

        FileHelper.recursiveDelete(folderOut);

        folderOut.mkdirs();

        CoherenceClusterBuilder builder = new CoherenceClusterBuilder();

        builder.include(cMember,
                        CoherenceClusterMember.class,
                        ClusterName.of(sClusterName),
                        DisplayName.of("Server"),
                        LocalHost.only(),
                        Headless.enabled(),
                        IPv4Preferred.autoDetect(),
                        FileWriterApplicationConsole.builder(folderOut.getCanonicalPath(), null)
                        );

        return builder.build();
        }

    protected static Registry ensureRegistry(String sClusterName, int cMember)
        {
        System.setProperty(LocalStorage.PROPERTY, "false");
        System.setProperty(ClusterName.PROPERTY, sClusterName);

        Cluster cluster = CacheFactory.ensureCluster();

        assertThat(cluster.getMemberSet().size(), is(cMember + 1));

        return cluster.getManagement();
        }

    protected void setValue(Registry registry, String sMBeanName, int nValue) throws Exception
        {
        MBeanServerProxy proxy = registry.getMBeanServerProxy();
        proxy.setAttribute(sMBeanName, ATTRIBUTE_CACHE_SIZE, nValue);
        }

    protected static void registerMBean(CoherenceClusterMember member)
        {
        try
            {
            member.submit(new MBeanServerProxyNotificationTests.RegisterMBean()).get();
            }
        catch (Throwable t)
            {
            throw Base.ensureRuntimeException(t);
            }
        }

    protected void assertMBeansRegistered(CoherenceCluster cluster)
        {
        for (CoherenceClusterMember member : cluster)
            {
            Eventually.assertDeferred(() -> countMBeans(member), is(3));
            }
        }

    protected void assertResponsibilityMBeanRegistered(CoherenceCluster cluster)
        {
        for (CoherenceClusterMember member : cluster)
            {
            Eventually.assertDeferred(() -> canSeeResponsibilityMBean(member), is(true));
            }

        }

    // must be public - used in Eventually.assertThat
    public int countMBeans(CoherenceClusterMember member)
        {
        try
            {
            return member.submit(new MBeanServerProxyNotificationTests.CountRegisteredMBeans()).get();
            }
        catch (Throwable t)
            {
            throw Base.ensureRuntimeException(t);
            }
        }

    // must be public - used in Eventually.assertThat
    public int countMBeanListeners(CoherenceClusterMember member, String sMBean)
        {
        try
            {
            int cListeners = member.submit(new CountMBeanNotificationListeners(sMBean)).get();

            CacheFactory.log("countMBeanListeners  Mbean=" + sMBean + " numberOfListeners=" + cListeners);
            return cListeners;
            }
        catch (Throwable t)
            {
            throw Base.ensureRuntimeException(t);
            }
        }

    // must be public - used in Eventually.assertThat
    public boolean canSeeResponsibilityMBean(CoherenceClusterMember member)
        {
        try
            {
            return 1 == member.submit(new MBeanServerProxyNotificationTests.CountRegisteredMBeans(RESPONSIBILITY_MBEAN_NAME)).get();
            }
        catch (Throwable t)
            {
            throw Base.ensureRuntimeException(t);
            }
        }

    protected static void registerResponsibilityMBean(CoherenceClusterMember member)
        {
        try
            {
            member.submit(new MBeanServerProxyNotificationTests.RegisterResponsibilityMBean()).get();
            }
        catch (Throwable t)
            {
            throw Base.ensureRuntimeException(t);
            }
        }

    // ----- inner class RegisterMBean --------------------------------------

    /**
     * A Bedrock {@link RemoteRunnable} to register the
     * {@link TestEmitter} MBean on a cluster member.
     */
    public static class RegisterMBean
            implements RemoteRunnable
        {
        @Override
        public void run()
            {
            Registry    registry = CacheFactory.ensureCluster().getManagement();
            TestEmitter emitter  = new TestEmitter();
            String      sName    = registry.ensureGlobalName(MBEAN_NAME);

            emitter.setMBeanName(sName);
            registry.register(sName, emitter);
            }
        }

    // ----- inner class RegisterResponsibilityMBean ------------------------

    /**
     * A Bedrock {@link RemoteRunnable} to register the
     * {@link TestEmitter} responsibility MBean on a cluster member.
     */
    public static class RegisterResponsibilityMBean
            implements RemoteRunnable
        {
        @Override
        public void run()
            {
            Registry    registry = CacheFactory.ensureCluster().getManagement();
            TestEmitter emitter  = new TestEmitter();
            String      sName    = registry.ensureGlobalName(RESPONSIBILITY_MBEAN_NAME);

            registry.register(sName, emitter);
            }
        }

    // ----- inner class UnregisterResponsibilityMBean ----------------------

    /**
     * A Bedrock {@link RemoteRunnable} to unregister the
     * {@link TestEmitter} MBean from a cluster member.
     */
    public static class UnregisterResponsibilityMBean
            implements RemoteRunnable
        {
        @Override
        public void run()
            {
            Registry registry = CacheFactory.ensureCluster().getManagement();
            String   sName    = registry.ensureGlobalName(RESPONSIBILITY_MBEAN_NAME);

            if (registry.isRegistered(sName))
                {
                registry.unregister(sName);
                }
            }
        }

    // ----- inner class CountRegisteredMBeans ------------------------------

    /**
     * A Bedrock {@link RemoteCallable} to count the number
     * of MBeans matching a given MBean name pattern.
     */
    public static class CountRegisteredMBeans
            implements RemoteCallable<Integer>
        {
        public CountRegisteredMBeans()
            {
            this(String.format(UNIQUE_MBEAN_PATTERN, "*"));
            }

        public CountRegisteredMBeans(String sPattern)
            {
            m_sPattern = sPattern;
            }

        @Override
        public Integer call()
            {
            try
                {
                Registry         registry = CacheFactory.ensureCluster().getManagement();
                MBeanServerProxy proxy    = registry.getMBeanServerProxy();
                Set<String>      setNames = proxy.queryNames(m_sPattern, null);
                return setNames.size();
                }
            catch (Exception e)
                {
                e.printStackTrace();
                return 0;
                }
            }

        private final String m_sPattern;
        }

    // ----- inner class Listener -------------------------------------------

    /**
     * A {@link NotificationListener} and {@link CountDownLatch}
     * that can wait for a specified number of notifications.
     */
    public static class Listener
            extends CountDownLatch
            implements NotificationListener
        {
        /**
         * Create a {@link Listener}.
         *
         * @param cNotification  the number of notifications to wait for
         */
        public Listener(int cNotification)
            {
            this(cNotification, null);
            }

        /**
         * Create a {@link Listener}.
         *
         * @param cNotification  the number of notifications to wait for
         * @param oHandback      the optional notification hand-back to expect
         */
        public Listener(int cNotification, Object oHandback)
            {
            super(cNotification);
            m_oHandback = oHandback;
            }

        /**
         * Obtain the new value returned from the last
         * {@link AttributeChangeNotification} received.
         *
         * @return  the new value returned from the last
         *          {@link AttributeChangeNotification} received
         */
        public Integer getNewValue()
            {
            return m_listValues.isEmpty() ? null : m_listValues.getLast();
            }

        /**
         * Obtain the values from all of the notifications.
         *
         * @return  the values from all of the notifications
         */
        public List<Integer> getValues()
            {
            return m_listValues;
            }

        @Override
        public void handleNotification(Notification notification, Object oHandback)
            {
            if (oHandback == m_oHandback)
                {
                AttributeChangeNotification changeNotification = (AttributeChangeNotification) notification;
                Object oOldValue = changeNotification.getOldValue();
                Object oNewValue = changeNotification.getNewValue();

                m_listValues.add((Integer) oNewValue);

                // silence log messages from Notifications registered from completed tests
                if (getCount() > 0L)
                    {
                    CacheFactory.log("Received notification: " + changeNotification
                                     + " old=" + oOldValue + " new=" + oNewValue + " CountDownLatch:" + getCount() + " Identifier=" + oHandback);
                    }
                countDown();
                }
            }

        private Object m_oHandback;

        private LinkedList<Integer> m_listValues = new LinkedList<>();
        }

    // ----- inner class: CountListeners ------------------------------------

    /**
     * A {@link RemoteCallable} that counts the number of
     * listeners registered on a local MBean.
     */
    public static class CountMBeanNotificationListeners
            implements RemoteCallable<Integer>
        {
        // ----- constructors -----------------------------------------------

        public CountMBeanNotificationListeners(String sMBean)
            {
            m_sMBean = sMBean;
            }

        // ----- RemoteCallable methods -------------------------------------

        @Override
        public Integer call() throws Exception
            {
            Registry         registry   = CacheFactory.ensureCluster().getManagement();
            MBeanServerProxy proxy      = registry.getMBeanServerProxy();
            Map              mapModels  = ((Gateway) proxy).getLocalModels();
            LocalModel       modelLocal = (LocalModel) mapModels.get(m_sMBean);
            int              cListener  = 0;

            System.err.println("***** CountMBeanNotificationListeners: mBean=" + m_sMBean + " localId="
                                       + CacheFactory.ensureCluster().getLocalMember().getId());

            if (modelLocal != null)
                {
                NotificationHandler handlerLocal  = modelLocal.get_LocalNotificationHandler();
                NotificationHandler handlerRemote = modelLocal.get_RemoteNotificationHandler();

                if (handlerLocal != null)
                    {
                    cListener += handlerLocal.getSubscriptions().size();
                    }
                else
                    {
                    System.err.println("***** CountMBeanNotificationListeners: LocalHandler is null - mBean=" + m_sMBean + " localId="
                                               + CacheFactory.ensureCluster().getLocalMember().getId());
                    }

                if (handlerRemote != null)
                    {
                    Set setSubs = handlerRemote.getSubscriptions();
                    int cSubs = setSubs.size();

                    for (Object o : setSubs)
                        {
                        RemoteHolder holder = (RemoteHolder)o;
                        System.err.println("***** CountMBeanNotificationListeners: RemoteHolder member=" + holder.getMemberId() + " holder=" + holder.getHolderId() + " - mBean=" + m_sMBean + " localId="
                                                   + CacheFactory.ensureCluster().getLocalMember().getId());
                        }

                    cListener += cSubs;
                    System.err.println("***** CountMBeanNotificationListeners: RemoteHandler has " + cSubs + " listeners - mBean=" + m_sMBean + " localId="
                                               + CacheFactory.ensureCluster().getLocalMember().getId());
                    }
                else
                    {
                    System.err.println("***** CountMBeanNotificationListeners: RemoteHandler is null - mBean=" + m_sMBean + " localId="
                                               + CacheFactory.ensureCluster().getLocalMember().getId());
                    }
                }
            else
                {
                System.err.println("***** CountMBeanNotificationListeners: model is null for " + m_sMBean + " localId="
                                           + CacheFactory.ensureCluster().getLocalMember().getId());
                }

            return cListener;
            }

        // ----- data members -----------------------------------------------

        private String m_sMBean;
        }

    // ----- inner class PredicateFilter ------------------------------------

    public static class PredicateNotificationFilter
            implements NotificationFilter
        {
        public PredicateNotificationFilter(Predicate<AttributeChangeNotification> predicate)
            {
            m_predicate = predicate;
            }

        @Override
        public boolean isNotificationEnabled(Notification notification)
            {
            if (notification instanceof AttributeChangeNotification)
                {
                boolean fMatch = m_predicate.test((AttributeChangeNotification) notification);

                CacheFactory.log("PredicateNotificationFilter - match=" + fMatch + " : " + this);

                return fMatch;
                }
            else
                {
                CacheFactory.log("PredicateNotificationFilter - Not a AttributeChangeNotification : " + this);
                return false;
                }
            }

        @Override
        public String toString()
            {
            return "PredicateNotificationFilter{" +
                    "predicate=" + m_predicate +
                    '}';
            }

        private Predicate<AttributeChangeNotification> m_predicate;
        }

    public static class ModThreePredicate
            implements Predicate<AttributeChangeNotification>, Serializable
        {
        @Override
        public boolean test(AttributeChangeNotification attributeChangeNotification)
            {
            Object  oValue = attributeChangeNotification.getNewValue();
            boolean fMatch = oValue instanceof Number && ((Number) oValue).intValue() % 3 == 0;

            CacheFactory.log("ModThreePredicate value=" + oValue + " match=" +fMatch);

            return fMatch;
            }

        @Override
        public String toString()
            {
            return "ModThreePredicate";
            }
        }

    public static class ModFourPredicate
            implements Predicate<AttributeChangeNotification>, Serializable
        {
        @Override
        public boolean test(AttributeChangeNotification attributeChangeNotification)
            {
            Object  oValue = attributeChangeNotification.getNewValue();
            boolean fMatch = oValue instanceof Number && ((Number) oValue).intValue() % 4 == 0;

            CacheFactory.log("ModFourPredicate value=" + oValue + " match=" +fMatch);

            return fMatch;
            }

        @Override
        public String toString()
            {
            return "ModFourPredicate";
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * The name of the bean to register
     */
    protected static String MBEAN_NAME = "Type=TestEmitter";

    /**
     * The name of the bean to register
     */
    protected static String RESPONSIBILITY_MBEAN_NAME = "Type=TestEmitter,responsibility=Coordinator";

    /**
     * The globally unique mbean name pattern
     */
    protected static String UNIQUE_MBEAN_PATTERN = "Type=TestEmitter,nodeId=%s";

    /**
     * The name of the {@link TestEmitter} MBeans cache size attribute.
     */
    protected static final String ATTRIBUTE_CACHE_SIZE = "CacheSize";

    // ----- data members ---------------------------------------------------

    @Rule
    public TestName m_testName = new TestName();
    }
