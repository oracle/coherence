/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package config;

import com.oracle.coherence.persistence.PersistenceEnvironment;
import com.oracle.coherence.persistence.PersistenceManager;

import com.oracle.datagrid.persistence.PersistenceTools;

import com.tangosol.coherence.component.util.SafeCluster;

import com.tangosol.coherence.config.builder.ParameterizedBuilderRegistry;
import com.tangosol.coherence.config.builder.PersistenceEnvironmentParamBuilder;
import com.tangosol.coherence.config.scheme.AbstractServiceScheme;
import com.tangosol.coherence.config.scheme.ServiceScheme;

import com.tangosol.internal.net.cluster.ClusterDependencies;
import com.tangosol.internal.net.service.ServiceDependencies;
import com.tangosol.internal.net.service.grid.DefaultPersistenceDependencies;
import com.tangosol.internal.net.service.grid.PartitionedServiceDependencies;
import com.tangosol.internal.net.service.grid.PersistenceDependencies;

import com.tangosol.io.ReadBuffer;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;

import com.tangosol.persistence.Snapshot;
import com.tangosol.persistence.SnapshotArchiver;
import com.tangosol.persistence.SnapshotArchiverFactory;
import com.tangosol.util.NullImplementation;
import common.SystemPropertyIsolation;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;


import java.io.File;
import java.util.Iterator;
import java.util.Properties;

/**
 * COH-12947 Persistence Environments Should be able to be retrieved via API.
 *
 * @version 12.2.1
 * @author  jf 2015.03.16
 */
public class PersistenceInfoConfigTests
    {
    /**
     * Demonstrate access to PersistenceEnvironmentInfo without starting services or creating PersistenceEnvironment.
     */
    @Test
    public void testPersistenceCluster()
        {
        Properties props = new Properties();

        props.setProperty("tangosol.coherence.override", "persistence-tangosol-coherence-override.xml");
        props.setProperty("tangosol.coherence.distributed.localstorage", "false");
        props.setProperty("coherence.distributed.synchronize", "false");
        props.setProperty("tangosol.coherence.distributed.aggressive", "21");
        CacheFactory.shutdown();

        try
            {
            System.getProperties().putAll(props);

            ExtensibleConfigurableCacheFactory eccf =
                (ExtensibleConfigurableCacheFactory) CacheFactory.getCacheFactoryBuilder()
                    .getConfigurableCacheFactory("persistence-bdb-cache-config.xml", null);

            ClusterDependencies deps =
                    (ClusterDependencies) ((SafeCluster)CacheFactory.getCluster()).getDependencies();

            for (ParameterizedBuilderRegistry.Registration r : deps.getBuilderRegistry())
                {
                if (r.getInstanceClass().isAssignableFrom(PersistenceEnvironment.class))
                    {
                    System.out.println("Name: " + r.getName() + " PersistenceEnvironmentBuilder info="
                            + ((PersistenceEnvironmentParamBuilder)r.getBuilder()).getPersistenceEnvironmentInfo("$CLUSTER$", "$SERVICE$"));
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
                    assertEquals(21, psDependencies.getDistributionAggressiveness());
                    assertFalse(psDependencies.isDistributionSynchronized());

                    DefaultPersistenceDependencies depsPersistence =
                        (DefaultPersistenceDependencies) ((PartitionedServiceDependencies) depsService)
                            .getPersistenceDependencies();


                    PersistenceEnvironmentParamBuilder bldr = (PersistenceEnvironmentParamBuilder)
                            depsPersistence.getPersistenceEnvironmentBuilder();
                    System.out.println("Service " + scheme.getServiceName() +
                                       " persistence failure mode: " + getFailureModeDescription(depsPersistence.getFailureMode())
                                       + " persistence env info="
                                       + bldr.getPersistenceEnvironmentInfo(CLUSTER_NAME, scheme.getServiceName())
                                       );
                    SnapshotArchiverFactory factory = depsPersistence.getArchiverFactory();
                    if (factory != null)
                        {
                        factory.createSnapshotArchiver(CLUSTER_NAME, scheme.getServiceName());
                        }
                    }
                }
            }
        catch (IllegalArgumentException e)
            {
            }
        finally
            {
            System.clearProperty("tangosol.coherence.override");
            CacheFactory.shutdown();
            }
        }

    private String getFailureModeDescription(int mode)
        {
        switch (mode)
            {
            case PersistenceDependencies.FAILURE_STOP_PERSISTENCE :
                return "failure-stop-persistence";

            case PersistenceDependencies.FAILURE_STOP_SERVICE :
                return "failure-stop-service";

            default :
                return "unknown-failure-mode: " + mode;
            }
        }

    // ----- inner class: CustomEnvironment ---------------------------------

    public static class CustomEnvironment
            implements PersistenceEnvironment<ReadBuffer>
        {
        public CustomEnvironment(String sCluster, String sService, String sMode, File fileActive, File fileSnapshot, File fileTrash)
            {
            f_sCluster     = sCluster;
            f_sService     = sService;
            f_sMode        = sMode;
            f_fileActive   = fileActive;
            f_fileSnapshot = fileSnapshot;
            f_fileTrash    = fileTrash;
            }

        @Override
        public PersistenceManager<ReadBuffer> openActive()
            {
            return "active".equals(f_sMode)
                    ? NullImplementation.getPersistenceManager(ReadBuffer.class)
                    : null;
            }

        @Override
        public PersistenceManager<ReadBuffer> openSnapshot(String sSnapshot)
            {
            return NullImplementation.getPersistenceManager(ReadBuffer.class);
            }

        @Override
        public PersistenceManager<ReadBuffer> createSnapshot(String sSnapshot, PersistenceManager<ReadBuffer> manager)
            {
            if (sSnapshot == null)
                {
                throw new IllegalArgumentException();
                }
            return NullImplementation.getPersistenceManager(ReadBuffer.class);
            }

        @Override
        public boolean removeSnapshot(String sSnapshot)
            {
            return false;
            }

        @Override
        public String[] listSnapshots()
            {
            return new String[0];
            }

        @Override
        public void release()
            {
            }

        protected final String f_sCluster;
        protected final String f_sService;
        protected final String f_sMode;
        protected final File f_fileActive;
        protected final File f_fileSnapshot;
        protected final File f_fileTrash;
        }

    // ----- inner class: CustomArchiver ---------------------------------

    public static class CustomArchiver
            implements SnapshotArchiver
        {
        public CustomArchiver(String sCluster, String sService)
            {
            f_sCluster = sCluster;
            f_sService = sService;
            }

        public CustomArchiver(String sCluster, String sService, String sURI)
            {
            f_sCluster = sCluster;
            f_sService = sService;
            m_sURI     = sURI;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public String[] list()
            {
            return new String[0];
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public Snapshot get(String sSnapshot)
            {
            return null;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean remove(String sSnapshot)
            {
            return false;
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void archive(Snapshot snapshot, PersistenceEnvironment<ReadBuffer> env)
            {
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public void retrieve(Snapshot snapshot, PersistenceEnvironment<ReadBuffer> env)
            {
            }

        /**
         * {@inheritDoc}
         */
        @Override
        public PersistenceTools getPersistenceTools(String sSnapshot)
            {
            return null;
            }

        protected final String f_sCluster;
        protected final String f_sService;
        protected String       m_sURI;
        }

    /**
     * A {@link org.junit.ClassRule} to isolate system properties set between test class
     * execution (not individual test method executions).
     */
    @ClassRule
    public static SystemPropertyIsolation s_systemPropertyIsolation = new SystemPropertyIsolation();
    }
