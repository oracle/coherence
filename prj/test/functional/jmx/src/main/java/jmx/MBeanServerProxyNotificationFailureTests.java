/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package jmx;

import com.oracle.bedrock.runtime.Application;
import com.oracle.bedrock.runtime.coherence.CoherenceCluster;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.tangosol.coherence.component.net.management.Connector;
import com.tangosol.coherence.component.net.management.gateway.Remote;

import com.tangosol.net.CacheFactory;

import com.tangosol.net.management.MBeanServerProxy;
import com.tangosol.net.management.Registry;

import org.junit.After;
import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

/**
 * @author jk  2018.09.13
 */
public class MBeanServerProxyNotificationFailureTests
        extends BaseMBeanServerProxyNotificationTests
    {
    @After
    public void cleanupTest()
        {
        CacheFactory.shutdown();
        }

    @Test
    public void shouldStillReceiveNotificationsWhenManagementSeniorFailsOver() throws Exception
        {
        String sClusterName = m_testName.getMethodName();
        int    nClusterSize = 3;

        try (CoherenceCluster cluster = startCluster(sClusterName, 3))
            {
            // register an MBean on each member
            cluster.forEach(BaseMBeanServerProxyNotificationTests::registerMBean);
            assertMBeansRegistered(cluster);

            Registry         registry     = ensureRegistry(sClusterName, nClusterSize);
            int              cMemberCount = getClusterSize();
            MBeanServerProxy proxy        = registry.getMBeanServerProxy();
            int              nSenior      = findMBeanServerMember(proxy);
            int              nMember      = findNonMBeanServerMember(proxy, nClusterSize);
            String           sMBeanName   = String.format(UNIQUE_MBEAN_PATTERN, nMember);
            String           sHandback    = UUID.randomUUID().toString();
            Listener         listener     = new Listener(2, sHandback);
            int              nBefore      = countListeners(cluster, sMBeanName);

            // add a listener to the MBean on a non-management senior member
            proxy.addNotificationListener(sMBeanName, listener, null, sHandback);

            // wait to ensure that the listener is registered
            Eventually.assertThat(invoking(this).countListeners(cluster, sMBeanName), is(greaterThan(nBefore)));

            // update the MBean's cache size attribute
            proxy.setAttribute(sMBeanName, ATTRIBUTE_CACHE_SIZE, 1234);

            // we should eventually get a notification
            Eventually.assertThat(invoking(listener).getNewValue(), is(1234));

            // close the management senior
            cluster.stream()
                    .filter(m -> m.getLocalMemberId() == nSenior)
                    .findFirst()
                    .ifPresent(Application::close);

            // wait for the member to depart
            Eventually.assertThat(invoking(this).getClusterSize(), is(cMemberCount - 1));
            // eventually a new member will become management senior and we will be able to see the Mbean
            Eventually.assertThat(invoking(this).getValue(proxy, sMBeanName), is(1234));

            // update the MBean cache size attribute
            proxy.setAttribute(sMBeanName, ATTRIBUTE_CACHE_SIZE, 9999);

            // we should get a notification if everythign failed over
            assertThat(listener.await(1, TimeUnit.MINUTES), is(true));

            assertThat(listener.getNewValue(), is(9999));
            }
        }

    // must be public - used in Eventually.assertThat
    public int getClusterSize()
        {
        return CacheFactory.ensureCluster().getMemberSet().size();
        }

    // must be public - used in Eventually.assertThat
    public Integer getValue(MBeanServerProxy proxy, String sMbean)
        {
        try
            {
            return (Integer) proxy.getAttribute(sMbean, ATTRIBUTE_CACHE_SIZE);
            }
        catch (Throwable t)
            {
            // can be cause if the management senior has left the cluster
            // and a new senior has not yet taken over
            return null;
            }
        }

    /**
     * Find a cluster member that is running the MBeanServer.
     *
     * @param proxy  the {@link MBeanServerProxy}.
     *
     * @return  the member id of a cluster member that is
     *          running the MBeanServer
     */
    public int findMBeanServerMember(MBeanServerProxy proxy)
        {
        Connector connector = ((Remote) proxy).getConnector();

        return connector.getDynamicSenior().getId();
        }

    /**
     * Find a cluster member that is not running the MBeanServer.
     *
     * @param proxy  the {@link MBeanServerProxy}.
     *
     * @return  the member id of a cluster member that is
     *          not running the MBeanServer
     */
    public int findNonMBeanServerMember(MBeanServerProxy proxy, int cMember)
        {
        int nSenior = findMBeanServerMember(proxy);

        for (int i=1; i<=cMember; i++)
            {
            if (nSenior != i)
                {
                return i;
                }
            }

        throw new IllegalStateException("Cannot find non-senior member");
        }

    // must be public - used in Eventually.assertThat
    public int countListeners(CoherenceCluster cluster, String sMBeanName)
        {
        int count = 0;

        for (CoherenceClusterMember member : cluster)
            {
            count += countMBeanListeners(member, sMBeanName);
            }

        return count;
        }
    }
