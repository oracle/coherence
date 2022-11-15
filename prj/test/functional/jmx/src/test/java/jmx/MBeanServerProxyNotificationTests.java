/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package jmx;

import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.tangosol.net.CacheFactory;

import com.tangosol.net.management.MBeanServerProxy;
import com.tangosol.net.management.Registry;

import com.tangosol.util.Base;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.management.NotificationFilter;

import java.util.UUID;

import java.util.concurrent.TimeUnit;

import java.util.stream.Collectors;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyIterable.emptyIterable;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;

/**
 * @author jk  2018.09.13
 */
public class MBeanServerProxyNotificationTests
        extends BaseMBeanServerProxyNotificationTests
    {
    @BeforeClass
    public static void setupClass() throws Exception
        {
        String sCluster  = "MBeanServerProxyNotificationTests";

        s_cluster = startCluster(sCluster, CLUSTER_SIZE);

        s_cluster.forEach(MBeanServerProxyNotificationTests::registerMBean);

        s_memberResponsibility = (CoherenceClusterMember) Base.randomize(s_cluster.collect(Collectors.toList())).get(0);

        registerResponsibilityMBean(s_memberResponsibility);

        s_registry = ensureRegistry(sCluster, CLUSTER_SIZE);
        }

    @AfterClass
    public static void cleanup()
        {
        if (s_cluster != null)
            {
            s_cluster.close();
            }
        }

    @Before
    public void setup() throws Exception
        {
        assertMBeansRegistered(s_cluster);
        assertResponsibilityMBeanRegistered(s_cluster);

        String sMessage = ">>>>>>> Starting test " + m_testName.getMethodName();

        for (CoherenceClusterMember member : s_cluster)
            {
            member.submit(() -> CacheFactory.log(sMessage, CacheFactory.LOG_INFO)).get();
            }
        }

    @After
    public void after() throws Exception
        {
        String sMessage = ">>>>>>> Completed test " + m_testName.getMethodName();

        for (CoherenceClusterMember member : s_cluster)
            {
            member.submit(() -> CacheFactory.log(sMessage, CacheFactory.LOG_INFO)).get();
            }
        }

    // ----- test methods ---------------------------------------------------

    @Test
    public void shouldRegisterNotificationListener() throws Exception
        {
        CoherenceClusterMember member     = s_cluster.findAny().get();
        int                    nMemberId  = member.getLocalMemberId();
        MBeanServerProxy       proxy      = s_registry.getMBeanServerProxy();
        String                 sMBeanName = String.format(UNIQUE_MBEAN_PATTERN, nMemberId);
        String                 sHandback  = UUID.randomUUID().toString();
        Listener               listener   = new Listener(1, sHandback);
        int                    cBefore    = countMBeanListeners(member, sMBeanName);

        proxy.addNotificationListener(sMBeanName, listener, null, sHandback);

        // Ensure that the listener is added by checking the listener count on the server
        Eventually.assertThat(invoking(this).countMBeanListeners(member, sMBeanName), is(cBefore + 1));

        proxy.setAttribute(sMBeanName, ATTRIBUTE_CACHE_SIZE, 1234);

        assertThat(listener.await(1, TimeUnit.MINUTES), is(true));

        assertThat(listener.getNewValue(), is(1234));
        }

    @Test
    public void shouldRegisterMultipleTimesNotificationListener() throws Exception
        {
        CoherenceClusterMember member     = s_cluster.findAny().get();
        int                    nMemberId  = member.getLocalMemberId();
        MBeanServerProxy       proxy      = s_registry.getMBeanServerProxy();
        String                 sMBeanName = String.format(UNIQUE_MBEAN_PATTERN, nMemberId);
        String                 sHandback  = UUID.randomUUID().toString();
        Listener               listener   = new Listener(5, sHandback);
        int                    cBefore    = countMBeanListeners(member, sMBeanName);

        proxy.addNotificationListener(sMBeanName, listener, null, sHandback);

        // Ensure that the listener is added by checking the listener count on the server
        Eventually.assertThat(invoking(this).countMBeanListeners(member, sMBeanName), is(cBefore + 1));
        assertThat(listener.getCount(), is(5L));

        proxy.setAttribute(sMBeanName, ATTRIBUTE_CACHE_SIZE, 111);
        proxy.setAttribute(sMBeanName, ATTRIBUTE_CACHE_SIZE, 222);
        proxy.setAttribute(sMBeanName, ATTRIBUTE_CACHE_SIZE, 333);

        Eventually.assertThat("verify that listener has been called 3 times", invoking(listener).getCount(), is(2L));

        // the listener should eventually receive the notifications
        Eventually.assertThat(invoking(listener).getNewValue(), is(333));

        proxy.removeNotificationListener(sMBeanName, listener, null, sHandback);

        // Ensure that the listener is gone by checking the listener count on the server goes back to the before count
        Eventually.assertThat(invoking(this).countMBeanListeners(member, sMBeanName), is(cBefore));

        // should not receive this notification
        proxy.setAttribute(sMBeanName, ATTRIBUTE_CACHE_SIZE, 444);
        assertThat("verify listener was unregistered and not called for modification setting CacheSize to 444", listener.getCount(), is(2L));

        proxy.addNotificationListener(sMBeanName, listener, null, sHandback);

        // Ensure that the listener is added by checking the listener count on the server
        Eventually.assertThat(invoking(this).countMBeanListeners(member, sMBeanName), is(cBefore + 1));

        assertThat("verify listener still not called for modification setting CacheSize to 444", listener.getCount(), is(2L));
        proxy.setAttribute(sMBeanName, ATTRIBUTE_CACHE_SIZE, 555);
        proxy.setAttribute(sMBeanName, ATTRIBUTE_CACHE_SIZE, 666);

        assertThat("wait until no outstanding expected notifications to listener", listener.await(1, TimeUnit.MINUTES), is(true));
        Eventually.assertThat("wait until see last expected modification", invoking(listener).getNewValue(), is(666));

        assertThat("CacheSize of 444 should not have been sent to listener", listener.getValues().contains(444), is(false));
        }

    @Test
    public void shouldUnRegisterNotificationListener() throws Exception
        {
        CoherenceClusterMember member     = s_cluster.findAny().get();
        int                    nMemberId  = member.getLocalMemberId();
        MBeanServerProxy       proxy      = s_registry.getMBeanServerProxy();
        String                 sMBeanName = String.format(UNIQUE_MBEAN_PATTERN, nMemberId);
        String                 sHandback  = UUID.randomUUID().toString();
        Listener               listener   = new Listener(1, sHandback);
        int                    cBefore    = countMBeanListeners(member, sMBeanName);

        proxy.addNotificationListener(sMBeanName, listener, null, sHandback);

        // Ensure that the listener is added by checking the listener count on the server
        Eventually.assertThat(invoking(this).countMBeanListeners(member, sMBeanName), is(cBefore + 1));

        proxy.removeNotificationListener(sMBeanName, listener, null, sHandback);

        // Ensure that the listener has been removed by checking the listener count on the server
        Eventually.assertThat(invoking(this).countMBeanListeners(member, sMBeanName), is(cBefore));

        proxy.setAttribute(sMBeanName, ATTRIBUTE_CACHE_SIZE, 1234);

        assertThat(listener.await(5, TimeUnit.SECONDS), is(false));

        assertThat(listener.getValues(), is(emptyIterable()));
        }

    @Test
    public void shouldRegisterSameNotificationListenerMultipleTimesWithFilters() throws Exception
        {
        CoherenceClusterMember member     = s_cluster.findAny().get();
        int                    nMemberId  = member.getLocalMemberId();
        MBeanServerProxy       proxy      = s_registry.getMBeanServerProxy();
        String                 sMBeanName = String.format(UNIQUE_MBEAN_PATTERN, nMemberId);
        String                 sHandback  = UUID.randomUUID().toString();
        Listener               listener   = new Listener(5, sHandback);
        int                    cBefore    = countMBeanListeners(member, sMBeanName);
        NotificationFilter     filterMod3 = new PredicateNotificationFilter(new ModThreePredicate());
        NotificationFilter     filterMod4 = new PredicateNotificationFilter(new ModFourPredicate());

        proxy.addNotificationListener(sMBeanName, listener, filterMod3, sHandback);
        proxy.addNotificationListener(sMBeanName, listener, filterMod4, sHandback);

        // Ensure that the listeners have been removed by checking the listener count on the server
        Eventually.assertThat(invoking(this).countMBeanListeners(member, sMBeanName), is(cBefore + 2));

        for (int i=1; i<=9; i++)
            {
            proxy.setAttribute(sMBeanName, ATTRIBUTE_CACHE_SIZE, i);
            }

        assertThat(listener.await(1, TimeUnit.MINUTES), is(true));

        assertThat(listener.getValues(), contains(3, 4, 6, 8, 9));
        }

    @Test
    public void shouldRemoveAllRegistrationsOfSameListener() throws Exception
        {
        CoherenceClusterMember member     = s_cluster.findAny().get();
        int                    nMemberId  = member.getLocalMemberId();
        MBeanServerProxy       proxy      = s_registry.getMBeanServerProxy();
        String                 sMBeanName = String.format(UNIQUE_MBEAN_PATTERN, nMemberId);
        String                 sHandback  = UUID.randomUUID().toString();
        Listener               listener   = new Listener(1, sHandback);
        int                    cBefore    = countMBeanListeners(member, sMBeanName);
        NotificationFilter     filterMod3 = new PredicateNotificationFilter(new ModThreePredicate());
        NotificationFilter     filterMod4 = new PredicateNotificationFilter(new ModFourPredicate());

        // register listener twice with different filters
        proxy.addNotificationListener(sMBeanName, listener, filterMod3, sHandback);
        proxy.addNotificationListener(sMBeanName, listener, filterMod4, sHandback);

        // Ensure that the listener is added by checking the listener count on the server
        Eventually.assertThat(invoking(this).countMBeanListeners(member, sMBeanName), is(cBefore + 2));

        // remove listener - should remove all registrations
        proxy.removeNotificationListener(sMBeanName, listener);

        // Ensure that the listener have been removed by checking the listener count on the server
        Eventually.assertThat(invoking(this).countMBeanListeners(member, sMBeanName), is(cBefore));

        for (int i=1; i<=9; i++)
            {
            proxy.setAttribute(sMBeanName, ATTRIBUTE_CACHE_SIZE, i);
            }

        // should not receive any notifications
        assertThat(listener.await(5, TimeUnit.SECONDS), is(false));
        assertThat(listener.getValues(), is(emptyIterable()));
        }

    @Test
    public void shouldRegisterNotificationListenerOnResponsibilityMBean() throws Exception
        {
        MBeanServerProxy proxy      = s_registry.getMBeanServerProxy();
        String           sHandback  = UUID.randomUUID().toString();
        Listener         listener   = new Listener(1, sHandback);
        int              cBefore    = countMBeanListeners(s_memberResponsibility, RESPONSIBILITY_MBEAN_NAME);

        proxy.addNotificationListener(RESPONSIBILITY_MBEAN_NAME, listener, null, sHandback);

        // Ensure that the listener is added by checking the listener count on the server
        Eventually.assertThat(invoking(this)
                .countMBeanListeners(s_memberResponsibility, RESPONSIBILITY_MBEAN_NAME), is(cBefore + 1));

        proxy.setAttribute(RESPONSIBILITY_MBEAN_NAME, ATTRIBUTE_CACHE_SIZE, 1234);

        assertThat(listener.await(1, TimeUnit.MINUTES), is(true));

        assertThat(listener.getNewValue(), is(1234));
        }


    @Test
    public void shouldRegisterNotificationListenersForMBeansOnAllMembers() throws Exception
        {
        String           sMBeanNode1 = String.format(UNIQUE_MBEAN_PATTERN, "1");
        String           sMBeanNode2 = String.format(UNIQUE_MBEAN_PATTERN, "2");
        String           sMBeanNode3 = String.format(UNIQUE_MBEAN_PATTERN, "3");
        int              nValue1     = 123123;
        int              nValue2     = 456456;
        int              nValue3     = 789789;
        MBeanServerProxy proxy       = s_registry.getMBeanServerProxy();
        Listener         listener1   = new Listener(1, "One");
        Listener         listener2   = new Listener(1, "Two");
        Listener         listener3   = new Listener(1, "Three");

        proxy.addNotificationListener(sMBeanNode1, listener1, null, "One");
        proxy.addNotificationListener(sMBeanNode2, listener2, null, "Two");
        proxy.addNotificationListener(sMBeanNode3, listener3, null, "Three");

        setValue(s_registry, sMBeanNode1, nValue1);
        setValue(s_registry, sMBeanNode2, nValue2);
        setValue(s_registry, sMBeanNode3, nValue3);

        assertThat(listener1.await(1, TimeUnit.MINUTES), is(true));
        assertThat(listener2.await(1, TimeUnit.MINUTES), is(true));
        assertThat(listener3.await(1, TimeUnit.MINUTES), is(true));

        assertThat(listener1.getNewValue(), is(nValue1));
        assertThat(listener2.getNewValue(), is(nValue2));
        assertThat(listener3.getNewValue(), is(nValue3));
        }

    // ----- constants ------------------------------------------------------

    /**
     * The size of the cluster to run.
     */
    private static final int CLUSTER_SIZE = 3;

    // ----- data members ---------------------------------------------------

    private static CoherenceCluster s_cluster;

    private static CoherenceClusterMember s_memberResponsibility;

    private static Registry s_registry;
    }
