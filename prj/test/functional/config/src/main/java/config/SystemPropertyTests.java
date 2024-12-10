/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package config;

import com.oracle.coherence.persistence.PersistenceEnvironment;
import com.tangosol.coherence.component.util.SafeCluster;
import com.tangosol.coherence.config.builder.ParameterizedBuilderRegistry;
import com.tangosol.coherence.config.builder.PersistenceEnvironmentParamBuilder;
import com.tangosol.coherence.config.scheme.AbstractServiceScheme;
import com.tangosol.coherence.config.scheme.ServiceScheme;
import com.tangosol.internal.net.cluster.ClusterDependencies;
import com.tangosol.internal.net.service.ServiceDependencies;
import com.tangosol.internal.net.service.grid.PartitionedServiceDependencies;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;

import com.tangosol.persistence.PersistenceEnvironmentInfo;
import com.oracle.coherence.testing.AbstractFunctionalTest;
import com.oracle.coherence.testing.SystemPropertyIsolation;
import com.oracle.coherence.testing.SystemPropertyResource;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * COH-12944 enable "coherence. system properties with backwards compatibility support for "coherence.
 * Does not extend {@link }AbstractFunctionalTest} since this test goal is to test both 12.2.1 Coherence system property
 * format of <tt>coherence.</tt> and backwards compatibility mode <tt>tangosol.coherence</tt>.  Since testing
 * is using prior to 12.2.1 Coherence system property conventions, do not extend {@link AbstractFunctionalTest}
 * that configures Coherence system property using <tt>tangosol.coherence.</tt> and potentially interfere with
 * this testing.
 *
 * @version 12.2.1
 * @author  jf 2015.04.28
 */

public class SystemPropertyTests
    {
    @BeforeClass
    public static void init()
        {
        CacheFactory.shutdown();
        }

    @AfterClass
    public static void cleanup()
        {
        CacheFactory.shutdown();
        }

    @Test
    public void testPersistenceSystemProperties() throws IOException
        {
        testPersistenceSystemProperties("coherence.");
        }

    @Test
    public void testPersistenceSystemPropertiesBackwardCompatibility() throws IOException
        {
        testPersistenceSystemProperties("coherence.");
        }

    /**
     * Verify can set partition count using either system property coherence.distributed.partitioncount or coherence.distributed.partitions.
     */
    @Test
    public void testPropertyDistributedPartitionCount()
        {
        Object[][] PARTITIONS_DATA =
            {
                {"coherence.distributed.partitioncount", Integer.valueOf(631)},
                {"coherence.distributed.partitions",     Integer.valueOf(638)},
            };

        for (Object[] part_data : PARTITIONS_DATA)
            {
            try (SystemPropertyResource p1 = new SystemPropertyResource((String) part_data[0], part_data[1].toString()))
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
                        assertThat(((PartitionedServiceDependencies) depsService).getPreferredPartitionCount(), is(part_data[1]));
                        }
                    }
                }
            CacheFactory.shutdown();
            }
        }

    /**
     * Single test case that allows for 12.2.1 Coherence system property beginning with <tt>coherence.</tt> or
     * prior to 12.2.1 <tt>tangosol.coherence.</tt>.
     * <p>
     * Test enables verification that operational override is working with either <tt>coherence.</tt> or <tt>tangosol.coherence.</tt>
     * and that localstorage and persistence system property is working using either Coherence system property prefix.
     *
     * @param sPrefix Coherence system property prefix    either <tt>coherence.</tt> or <tt>tangosol.coherence.</tt>
     */
    private void testPersistenceSystemProperties(String sPrefix) throws IOException
        {
        try (SystemPropertyResource p1 = new SystemPropertyResource(sPrefix + "override", "persistence-tangosol-coherence-override.xml");
             SystemPropertyResource p2 = new SystemPropertyResource(sPrefix + "distributed.localstorage", "false");
             SystemPropertyResource p3 = new SystemPropertyResource(sPrefix + "distributed.persistence.base.dir", "target" + File.separator + "mypersistencebasedir");
        )
            {
            ExtensibleConfigurableCacheFactory eccf =
                    (ExtensibleConfigurableCacheFactory) CacheFactory.getCacheFactoryBuilder()
                            .getConfigurableCacheFactory("persistence-bdb-cache-config.xml", null);

            ClusterDependencies deps =
                    (ClusterDependencies) ((SafeCluster) CacheFactory.getCluster()).getDependencies();

            for (ParameterizedBuilderRegistry.Registration r : deps.getBuilderRegistry())
                {
                if (r.getInstanceClass().isAssignableFrom(PersistenceEnvironment.class))
                    {
                    PersistenceEnvironmentInfo info =
                            ((PersistenceEnvironmentParamBuilder) r.getBuilder()).getPersistenceEnvironmentInfo("$CLUSTER$", "$SERVICE$");
                    String sDirSnapshot = info.getPersistenceSnapshotDirectory().getAbsolutePath();
                    if (r.getName().startsWith("default-"))
                        {
                        assertTrue(sDirSnapshot.contains("mypersistencebasedir"));
                        }
                    else
                        {
                        assertTrue(sDirSnapshot.contains("store-"));
                        }
                    }
                }

            // do not use SafeCluster.getClusterName().  It unfortunately ensures cluster is running.
            // this should access all persistence info without starting cluster.
            final String CLUSTER_NAME = deps.getMemberIdentity().getClusterName();

            Iterator<ServiceScheme> iter = eccf.getCacheConfig().getServiceSchemeRegistry().iterator();

            for (ServiceScheme scheme : eccf.getCacheConfig().getServiceSchemeRegistry())
                {
                ServiceDependencies depsService =
                        (ServiceDependencies) ((AbstractServiceScheme) scheme).getServiceDependencies();

                if (depsService instanceof PartitionedServiceDependencies)
                    {
                    PartitionedServiceDependencies psDependencies = (PartitionedServiceDependencies) depsService;
                    assertFalse(psDependencies.isOwnershipCapable());
                    }
                }
            }
        }

    /**
     * A {@link org.junit.ClassRule} to isolate system properties set between test class
     * execution (not individual test method executions).
     */
    @ClassRule
    public static SystemPropertyIsolation s_systemPropertyIsolation = new SystemPropertyIsolation();
    }
