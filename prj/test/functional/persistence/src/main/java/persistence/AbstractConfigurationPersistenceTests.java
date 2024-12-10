/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package persistence;

import com.oracle.coherence.persistence.PersistenceEnvironment;

import com.oracle.coherence.persistence.PersistenceManager;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;

import com.tangosol.coherence.component.util.SafeService;

import com.tangosol.internal.net.service.grid.PersistenceDependencies;

import com.tangosol.io.FileHelper;

import com.tangosol.io.ReadBuffer;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.Service;

import com.tangosol.persistence.DirectorySnapshotArchiver;
import com.tangosol.persistence.SafePersistenceWrappers.SafePersistenceEnvironment;

import com.tangosol.persistence.SnapshotArchiver;
import com.tangosol.persistence.bdb.BerkeleyDBEnvironment;

import com.tangosol.util.ClassHelper;

import com.tangosol.util.NullImplementation;
import com.oracle.coherence.testing.AbstractFunctionalTest;

import java.io.File;
import java.io.IOException;

import java.util.Properties;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;

import static org.hamcrest.CoreMatchers.is;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Functional persistence configuration tests.
 *
 * @author jh  2014.02.14
 */
public abstract class AbstractConfigurationPersistenceTests
        extends AbstractFunctionalTest
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Create a new AbstractConfigurationPersistenceTests that will use the
     * cache configuration file with the given path to instantiate NamedCache
     * instances.
     *
     * @param sPath  the configuration resource name or file path
     */
    public AbstractConfigurationPersistenceTests(String sPath)
        {
        super(sPath);
        }

    // ----- test lifecycle -------------------------------------------------

    /**
     * Initialize the test class.
     */
    @BeforeClass
    public static void _startup()
        {
        // this test requires local storage to be enabled
        System.setProperty("coherence.distributed.localstorage", "true");
        System.setProperty("coherence.override", "common-tangosol-coherence-override.xml");

        // the following property is used by the custom persistence environment
        try
            {
            s_fileActive   = FileHelper.createTempDir();
            s_fileSnapshot = FileHelper.createTempDir();
            s_fileTrash    = FileHelper.createTempDir();

            System.setProperty("test.persistence.active.dir",   s_fileActive.getAbsolutePath());
            System.setProperty("test.persistence.snapshot.dir", s_fileSnapshot.getAbsolutePath());
            System.setProperty("test.persistence.trash.dir",    s_fileTrash.getAbsolutePath());
            }
        catch (IOException e)
            {
            throw ensureRuntimeException(e);
            }

        AbstractFunctionalTest._startup();
        }

    /**
     * Shutdown the test class.
     */
    @AfterClass
    public static void _shutdown()
        {
        AbstractFunctionalTest._shutdown();
        try
            {
            FileHelper.deleteDir(s_fileActive);
            FileHelper.deleteDir(s_fileSnapshot);
            FileHelper.deleteDir(s_fileTrash);
            }
        catch (IOException e)
            {
            log(e);
            }
        }

    // ----- tests ----------------------------------------------------------

    /**
     * Test a distributed cache scheme without a persistence config.
     */
    @Test
    public void testDefaultConfig()
            throws Exception
        {
        ConfigurableCacheFactory factory = CacheFactory.getCacheFactoryBuilder()
                .getConfigurableCacheFactory("client-cache-config.xml", null);
        setFactory(factory);

        CacheService service     = getNamedCache("default-persistent").getCacheService();
        SafeService  serviceSafe = (SafeService) service;
        Service      serviceReal = serviceSafe.getService();

        // assert that the default environment is used
        assertConfig(serviceReal, BerkeleyDBEnvironment.class, false,
                PersistenceDependencies.FAILURE_STOP_SERVICE, null);
        }

    /**
     * Test a distributed cache scheme with an empty persistence config.
     */
    @Test
    public void testEmptyConfig()
            throws Exception
        {
        CacheService service     = getNamedCache("empty-persistent").getCacheService();
        SafeService  serviceSafe = (SafeService) service;
        Service      serviceReal = serviceSafe.getService();

        // assert that the default environment is used
        assertConfig(serviceReal, BerkeleyDBEnvironment.class, false,
                PersistenceDependencies.FAILURE_STOP_SERVICE, null);
        }

    /**
     * Test a distributed cache scheme with persistence config that specifies
     * a failure policy.
     */
    @Test
    public void testFailureModeConfig()
            throws Exception
        {
        CacheService service     = getNamedCache("failure-mode-persistent").getCacheService();
        SafeService  serviceSafe = (SafeService) service;
        Service      serviceReal = serviceSafe.getService();

        // assert that the default environment is used
        assertConfig(serviceReal, BerkeleyDBEnvironment.class, false,
                PersistenceDependencies.FAILURE_STOP_PERSISTENCE, null);
        }

    /**
     * Test a distributed cache scheme with a custom persistence config.
     */
    @Test
    public void testCustomConfig()
            throws Exception
        {
        CacheService service     = getNamedCache("custom-persistent").getCacheService();
        SafeService  serviceSafe = (SafeService) service;
        Service      serviceReal = serviceSafe.getService();

        // assert that the default environment is used
        assertConfig(serviceReal,
                CustomEnvironment.class,
                true, PersistenceDependencies.FAILURE_STOP_SERVICE,
                ConfigurableSnapshotArchiverFactoryTest.CustomArchiver.class);
        }

    @Test
    public void testDirectoryConfig()
            throws Exception
        {
        CacheService service     = getNamedCache("directory-persistent").getCacheService();
        SafeService  serviceSafe = (SafeService) service;
        Service      serviceReal = serviceSafe.getService();

         // assert that the default environment is used
         assertConfig(serviceReal,
                 CustomEnvironment.class,
                 true, PersistenceDependencies.FAILURE_STOP_SERVICE,
                 DirectorySnapshotArchiver.class);
        }

    /**
     * Test a distributed cache scheme with a simple persistence config.
     */
    @Test
    public void testSimpleConfig()
            throws Exception
        {
        CacheService service     = getNamedCache("simple-persistent").getCacheService();
        SafeService  serviceSafe = (SafeService) service;
        Service      serviceReal = serviceSafe.getService();

        // assert that the default environment is used
        assertConfig(serviceReal, getPersistenceEnvironmentImpl(), true,
                PersistenceDependencies.FAILURE_STOP_SERVICE, null);
        }

    /**
     * Test that another storage enabled node running with a conflicting
     * persistence archiver config is not able to join the cache service.
     */
    @Test
    public void testServerArchiverConfigConflict()
        {
        Properties props = new Properties();
        props.setProperty("test.persistence.archiver", "simple-custom-archiver");
        testConfigConflict("ServerArchiverConfigConflict", "simple-persistent", props, true, false);
        }

    /**
     * Test that another storage disabled node running with a conflicting
     * persistence archiver config is able to join the cache service.
     */
    @Test
    public void testClientArchiverConfigConflict()
        {
        Properties props = new Properties();
        props.setProperty("test.persistence.archiver", "simple-custom-archiver");
        testConfigConflict("ClientArchiverConfigConflict", "simple-persistent", props, false, true);
        }

    /**
     * Test that another storage enabled node running with a conflicting
     * persistence environment config is not able to join the cache service.
     */
    @Test
    public void testServerEnvironmentConfigConflict()
        {
        Properties props = new Properties();
        props.setProperty("test.persistence.environment", "simple-custom-environment");
        testConfigConflict("ServerEnvironmentConfigConflict", "simple-persistent", props, true, false);
        }

    /**
     * Test that another storage disabled node running with a conflicting
     * persistence environment config is able to join the cache service.
     */
    @Test
    public void testClientEnvironmentConfigConflict()
        {
        Properties props = new Properties();
        props.setProperty("test.persistence.environment", "simple-custom-environment");
        testConfigConflict("ClientEnvironmentConfigConflict", "simple-persistent", props, false, true);
        }

    /**
     * Test that another storage enabled node running with a conflicting
     * persistence failure config is not able to join the cache service.
     */
    @Test
    public void testServerFailureConfigConflict()
        {
        Properties props = new Properties();
        props.setProperty("test.persistence.failure.mode", "stop-persistence");
        testConfigConflict("ServerFailureConfigConflict", "simple-persistent", props, true, false);
        }

    /**
     * Test that another storage disabled node running with a conflicting
     * persistence failure config is able to join the cache service.
     */
    @Test
    public void testClientFailureConfigConflict()
        {
        Properties props = new Properties();
        props.setProperty("test.persistence.failure.mode", "stop-persistence");
        testConfigConflict("ClientFailureConfigConflict", "simple-persistent", props, false, true);
        }

    /**
     * Test that another storage enabled node running with a conflicting
     * persistence mode config is not able to join the cache service.
     */
    @Test
    public void testServerModeConfigConflict()
        {
        Properties props = new Properties();
        props.setProperty("test.persistence.mode", "on-demand");
        testConfigConflict("ServerModeConfigConflict", "simple-persistent", props, true, false);
        }

    /**
     * Test that another storage disabled node running with a conflicting
     * persistence mode config is able to join the cache service.
     */
    @Test
    public void testClientModeConfigConflict()
        {
        Properties props = new Properties();
        props.setProperty("test.persistence.mode", "on-demand");
        testConfigConflict("ClientModeConfigConflict", "simple-persistent", props, false, true);
        }

    private void testConfigConflict(String sServer, String sCache, Properties props, boolean fStorage, boolean fJoinExpected)
        {
        CacheService service = getNamedCache(sCache).getCacheService();
        assertEquals(1, service.getInfo().getServiceMembers().size());

        if (props == null)
            {
            props = new Properties();
            }
        props.setProperty("test.server.distributed.localstorage", String.valueOf(fStorage));
        props.setProperty("coherence.override", "common-tangosol-coherence-override.xml");
        try
            {
            // start a new member that we either expect to fail (storage-enabled)
            // or start successfully (storage-disabled) due to a persistence
            // configuration mismatch
            CoherenceClusterMember m1 =
                    startCacheServer(sServer, getProjectName(), getCacheConfigPath(), props, false);
            if (fStorage)
                {
                m1.waitFor();
                }

            Eventually.assertThat(invoking(service.getInfo()).getServiceMembers().size(),
                    is(fJoinExpected ? 2 : 1));
            }
        finally
            {
            if (!fStorage)
                {
                stopCacheServer(sServer);
                }
            }
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Assert that the given service uses the specified environment
     * implementation, persistence mode, active failure mode, and optional
     * archiver implementation.
     *
     * @param service       the service
     * @param clzEnv        the PersistenceEnvironment class name
     * @param fActive       true if active persistence if used; false otherwise
     * @param nFailureMode  the failure mode
     * @param clzArch       the optional SnapshotArchiver class name
     */
    protected void assertConfig(Service service, Class clzEnv, boolean fActive, int nFailureMode, Class clzArch)
            throws Exception
        {
        // get the persistence control
        Object oControl = ClassHelper.invoke(service, "getPersistenceControl",
                ClassHelper.VOID);

        // assert the environment configuration
        PersistenceEnvironment env = ((SafePersistenceEnvironment)
                ClassHelper.invoke(oControl, "getPersistenceEnvironment", ClassHelper.VOID)).getEnvironment();

        assertEquals(clzEnv, env.getClass());
        assertTrue(fActive == (env.openActive() != null));

        // assert the failure mode configuration
        assertEquals(Integer.valueOf(nFailureMode),
                ClassHelper.invoke(oControl, "getActiveFailureMode", ClassHelper.VOID));

        // assert the optional archiver configuration
        if (clzArch != null)
            {
            Class clzCheck = ((SnapshotArchiver)
                    ClassHelper.invoke(oControl, "getSnapshotArchiver", ClassHelper.VOID)).getClass();
            assertEquals(clzArch, clzCheck);
            }
        }

    // ----- abstract methods -----------------------------------------------

    /**
     * Return the class of the persistence environment being tested.
     *
     * @return the persistence environment implementation class
     */
    protected abstract Class getPersistenceEnvironmentImpl();

    // ----- accessors ------------------------------------------------------

    /**
     * Return the project name.
     */
    public static String getProjectName()
        {
        return "persistence";
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
        public PersistenceManager<ReadBuffer> openBackup()
            {
            return "active-backup".equals(f_sMode)
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

        // ----- data members -----------------------------------------------

        public final String f_sCluster;
        public final String f_sService;
        public final String f_sMode;
        public final File f_fileActive;
        public final File f_fileSnapshot;
        public final File f_fileTrash;
        }


    // ----- data members ---------------------------------------------------

    /**
     * Temporary persistence directories.
     */
    protected static File s_fileActive;
    protected static File s_fileSnapshot;
    protected static File s_fileTrash;
    }
