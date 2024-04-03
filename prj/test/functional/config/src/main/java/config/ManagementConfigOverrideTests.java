/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package config;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.management.MBeanServerProxy;
import com.tangosol.net.management.Registry;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test management-config.xml override.
 */
public class ManagementConfigOverrideTests
    {
    @BeforeClass
    public static void setup()
        {
        System.setProperty("coherence.management", "all");
        System.setProperty("coherence.management.report.autostart", "true");
        System.setProperty("coherence.management.exclude", ".*type=Platform,Domain=java.lang,subType=ClassLoading,.* .*type=Platform,Domain=java.lang,subType=Compilation,.* .*type=Platform,Domain=java.lang,subType=MemoryManager,.* .*type=Platform,Domain=java.lang,subType=Threading,.* .*type=DiagnosticCommand,Domain=com.sun.management,subType=HotSpotDiagnostic,.* .*type=Cache,.*,name=$meta$.* .*type=StorageManager,.*,cache=$meta$.*");
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("coherence.wka", "127.0.0.1");
        }

    @Test
    public void shouldOverrideManagementConfig()
        {
        Cluster cluster   = CacheFactory.ensureCluster();
        Registry registry = cluster.getManagement();
        assertNotNull("JMX is disabled", registry);
        MBeanServerProxy proxy = registry.getMBeanServerProxy();
        Long requestTimeout    = (Long) proxy.getAttribute("Coherence:type=Service,name=Management,nodeId=1", "RequestTimeoutMillis");
        assertNotNull("request timeout is null", requestTimeout);
        assertEquals(123456, requestTimeout.longValue());
        }
    }