/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package config;

import com.oracle.coherence.testing.SystemPropertyIsolation;
import com.oracle.coherence.testing.SystemPropertyResource;

import com.tangosol.coherence.config.scheme.AbstractServiceScheme;
import com.tangosol.coherence.config.scheme.ServiceScheme;

import com.tangosol.internal.net.service.ServiceDependencies;
import com.tangosol.internal.net.service.grid.DefaultPartitionedServiceDependencies;
import com.tangosol.internal.net.service.grid.PartitionedServiceDependencies;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * Test for system property overrides of {@code coherence.service.partitions} and <code>coherence.service. {@literal <}scoped-servicename{@literal >}.partitions</code>.
 *
 * @author  jf 2024.05.24
 */
public class ServiceSystemPropertyTests
    {
    @BeforeClass
    public static void init()
        {
        System.setProperty(DefaultPartitionedServiceDependencies.PROP_DEFAULT_SERVICE_PARTITIONS, DEFAULT_SERVICE_PARTITIONS.toString());
        }

    @After
    public void cleanup()
        {
        CacheFactory.shutdown();
        }

    /**
     * Verify that system property <code>coherence.service.partitions</code> overrides default in coherence cache config file of 257.
     */
    @Test
    public void testDefaultServicePartitions()
        {
        ExtensibleConfigurableCacheFactory eccf =
                (ExtensibleConfigurableCacheFactory) CacheFactory.getCacheFactoryBuilder()
                        .getConfigurableCacheFactory("com/oracle/coherence/defaults/coherence-cache-config.xml", null);

        for (ServiceScheme scheme : eccf.getCacheConfig().getServiceSchemeRegistry())
            {
            ServiceDependencies depsService =
                    (ServiceDependencies) ((AbstractServiceScheme) scheme).getServiceDependencies();

            if (depsService instanceof PartitionedServiceDependencies)
                {
                assertThat(((PartitionedServiceDependencies) depsService).getPreferredPartitionCount(), is(DEFAULT_SERVICE_PARTITIONS));
                }
            }
        }


    /**
     * Verify can set partition count for default coherence cache config using
     * either system property {@code coherence.service.PartitionedCache.partitions} or {@code coherence.service.PartitionedTopic.partitions}.
     */
    @Test
    public void testPropertyDistributedPartitionCount()
        {
        Object[][] PARTITIONS_DATA =
            {
                {String.format(DefaultPartitionedServiceDependencies.PROP_SERVICE_PARTITIONS, "PartitionedCache"), Integer.valueOf(631)},
                {String.format(DefaultPartitionedServiceDependencies.PROP_SERVICE_PARTITIONS, "PartitionedTopic"), Integer.valueOf(641)}
            };

        try (SystemPropertyResource p1 = new SystemPropertyResource((String) PARTITIONS_DATA[0][0], PARTITIONS_DATA[0][1].toString());
             SystemPropertyResource p2 = new SystemPropertyResource((String) PARTITIONS_DATA[1][0], PARTITIONS_DATA[1][1].toString()))
            {
            ExtensibleConfigurableCacheFactory eccf =
                    (ExtensibleConfigurableCacheFactory) CacheFactory.getCacheFactoryBuilder()
                            .getConfigurableCacheFactory("com/oracle/coherence/defaults/coherence-cache-config.xml", null);

            for (ServiceScheme scheme : eccf.getCacheConfig().getServiceSchemeRegistry())
                {
                ServiceDependencies depsService =
                        (ServiceDependencies) ((AbstractServiceScheme) scheme).getServiceDependencies();

                if (depsService instanceof PartitionedServiceDependencies)
                    {
                    int i = scheme.getServiceName().equals("PartitionedCache") ? 0 : 1;

                    assertThat(((PartitionedServiceDependencies) depsService).getPreferredPartitionCount(), is(PARTITIONS_DATA[i][1]));
                    }
                }
            }
        }

    /**
     * Default for system property {@code coherence.service.partitions} value for this test.
     */
    public static final Integer DEFAULT_SERVICE_PARTITIONS = 541;

    /**
     * A {@link ClassRule} to isolate system properties set between test class
     * execution (not individual test method executions).
     */
    @ClassRule
    public static SystemPropertyIsolation s_systemPropertyIsolation = new SystemPropertyIsolation();
    }
