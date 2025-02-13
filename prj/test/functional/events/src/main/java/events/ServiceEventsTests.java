/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package events;

import com.tangosol.coherence.component.util.SafeService;
import com.tangosol.net.Cluster;
import com.tangosol.net.Coherence;
import com.tangosol.net.NamedMap;
import com.tangosol.net.Service;
import com.tangosol.net.Session;
import com.tangosol.util.ServiceEvent;
import com.tangosol.util.ServiceListener;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ServiceEventsTests
    {
    @BeforeClass
    public static void setup() throws Exception
        {
        System.setProperty("coherence.cluster", "ServiceEventsTests");
        System.setProperty("coherence.wka", "127.0.0.1");
        System.setProperty("coherence.localhost", "127.0.0.1");
        System.setProperty("coherence.ttl", "0");

        Coherence coherence = Coherence.clusterMember().start().get(5, TimeUnit.MINUTES);
        m_cluster = coherence.getCluster();
        m_session = coherence.getSession();
        }

    @Test
    public void shouldReceiveSuspendAndResume() throws Exception
        {
        NamedMap<?, ?> map      = m_session.getMap("test");
        Service        service  = map.getService();
        String         sName    = service.getInfo().getServiceName();
        Listener       listener = new Listener();

        service.addServiceListener(listener);

        m_cluster.suspendService(sName);
        assertThat(listener.m_latchSuspended.await(20, TimeUnit.SECONDS), is(true));

        m_cluster.resumeService(sName);
        assertThat(listener.m_latchResumed.await(20, TimeUnit.SECONDS), is(true));
        }

    // ----- inner class: Listener ------------------------------------------

    protected static class Listener
            implements ServiceListener
        {
        @Override
        public void serviceStarting(ServiceEvent evt)
            {
            m_latchStarting.countDown();
            }

        @Override
        public void serviceStarted(ServiceEvent evt)
            {
            m_latchStarted.countDown();
            }

        @Override
        public void serviceStopping(ServiceEvent evt)
            {
            m_latchStopping.countDown();
            }

        @Override
        public void serviceStopped(ServiceEvent evt)
            {
            m_latchStopped.countDown();
            }

        @Override
        public void serviceSuspended(ServiceEvent evt)
            {
            m_latchSuspended.countDown();
            }

        @Override
        public void serviceResumed(ServiceEvent evt)
            {
            m_latchResumed.countDown();
            }

        // ----- data members -----------------------------------------------

        private final CountDownLatch m_latchStarting = new CountDownLatch(1);

        private final CountDownLatch m_latchStarted = new CountDownLatch(1);

        private final CountDownLatch m_latchStopping = new CountDownLatch(1);

        private final CountDownLatch m_latchStopped = new CountDownLatch(1);

        private final CountDownLatch m_latchSuspended = new CountDownLatch(1);

        private final CountDownLatch m_latchResumed = new CountDownLatch(1);
        }

    // ----- data members ---------------------------------------------------

    private static Cluster m_cluster;

    private static Session m_session;
    }
