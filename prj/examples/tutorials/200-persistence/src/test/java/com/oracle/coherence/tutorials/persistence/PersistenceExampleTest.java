/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.tutorials.persistence;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.coherence.common.base.Blocking;
import com.oracle.coherence.common.base.Timeout;
import com.oracle.coherence.tutorials.persistence.NotificationWatcher.PersistenceNotificationListener;

import com.tangosol.io.FileHelper;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.Coherence;
import com.tangosol.net.CoherenceConfiguration;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Session;
import com.tangosol.net.SessionConfiguration;
import com.tangosol.net.management.MBeanHelper;
import com.tangosol.net.management.MBeanServerProxy;
import com.tangosol.net.management.Registry;
import com.tangosol.persistence.CachePersistenceHelper;
import com.tangosol.util.Base;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static com.oracle.coherence.tutorials.persistence.NotificationWatcher.waitForRegistration;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for Persistence examples.
 * @author tam 2022.04.27
 */
public class PersistenceExampleTest {

    protected static PersistenceNotificationListener listener = null;
    protected static ObjectName mBean = null;
    private static Session session;
    private static File    tmpDir;

    private static final String CACHE_CONFIG = "persistence-cache-config.xml";
    private static final String CACHE = "test";
    private static final String SERVICE_NAME = "PartitionedCache";

    @BeforeAll
    public static void _startup() throws IOException {
        System.setProperty("coherence.log.level", "3");
        System.setProperty("coherence.wka", "127.0.0.1");
        System.setProperty("coherence.ttl", "0");
        System.setProperty("coherence.management", "all");

        // ensure persistence snapshots go to a directory we control
        tmpDir = FileHelper.createTempDir();
        System.setProperty("coherence.distributed.persistence.base.dir", tmpDir.getAbsolutePath());

        SessionConfiguration sessionConfig = SessionConfiguration.builder()
                                                                 .withConfigUri(CACHE_CONFIG)
                                                                 .build();
        CoherenceConfiguration cfg = CoherenceConfiguration.builder()
                                                           .withSession(sessionConfig)
                                                           .build();
        Coherence coherence = Coherence.clusterMember(cfg);
        coherence.start().join();

        session = coherence.getSession();
    }

    @AfterAll
    public static void _shutdown() {
        try {
            Coherence coherence = Coherence.getInstance();
            coherence.close();
        }
        finally {
            FileHelper.deleteDirSilent(tmpDir);
        }
    }

    @Test
    public void runTest() throws Exception {
        final int   COUNT    = 1000;
        Cluster     cluster  = CacheFactory.ensureCluster();
        MBeanServer server   = MBeanHelper.findMBeanServer();
        Registry    registry = cluster.getManagement();

        NamedCache<Integer, String> cache = session.getCache(CACHE);
        cache.clear();
        assertEquals(cache.size(), 0);

        String mBeanName = "Coherence:" + CachePersistenceHelper.getMBeanName(SERVICE_NAME);
        waitForRegistration(registry, mBeanName);

        mBean = new ObjectName(mBeanName);
        listener = new PersistenceNotificationListener(SERVICE_NAME);

        server.addNotificationListener(mBean, listener, null, null);

        Map<Integer, String> buffer = new HashMap<>();
        for (int i = 0; i < COUNT; i++) {
            buffer.put(i, "Value-" + i);
        }

        cache.putAll(buffer);
        assertEquals(cache.size(), COUNT);

        Blocking.sleep(10_000L);

        invokeOperationWithWait(registry, "createSnapshot", SERVICE_NAME, "snapshot1");

        Eventually.assertThat(invoking(listener).getEventCount(), Matchers.is(2));

        server.removeNotificationListener(mBean, listener);
    }

    private void invokeOperationWithWait(Registry registry, String sOperation, String sServiceName, String sSnapshot) {
        MBeanServerProxy proxy     = registry.getMBeanServerProxy();
        boolean          isIdle;
        String           sBeanName = CachePersistenceHelper.getMBeanName(sServiceName);

        try (Timeout t = Timeout.after(240, TimeUnit.SECONDS)) {
            proxy.invoke(sBeanName, sOperation, new String[] {sSnapshot}, new String[] {"java.lang.String"});

            while (true) {
                Blocking.sleep(500L);
                isIdle = (boolean) proxy.getAttribute(sBeanName, "Idle");

                if (isIdle) {
                    // idle means the operation has completed as we are guaranteed an up-to-date
                    // attribute value just after an operation was called
                    return;
                }
            }
        }
        catch (Exception e) {
            throw Base.ensureRuntimeException(e, "Unable to complete operation " + sOperation + " for service "
                                                 + sServiceName);
        }
    }
}
