/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package persistence;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.tangosol.io.FileHelper;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.NamedCache;
import com.tangosol.net.cache.CacheStore;
import com.tangosol.net.management.MBeanHelper;

import com.tangosol.util.Base;
import com.tangosol.util.comparator.SafeComparator;
import com.tangosol.util.extractor.ReflectionExtractor;
import com.tangosol.util.ValueExtractor;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import java.nio.file.Files;

import java.util.Properties;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;


/**
 * Functional tests for recovering snapshots when write-behind is configured.
 *
 * @author tam 2021.04.01
 */
public class BerkeleyDBWriteBehindPersistenceTests
        extends AbstractFunctionalTest
    {
    // ----- constructors ----------------------------------------------------

    /**
     * Default Constructor.
     */
    public BerkeleyDBWriteBehindPersistenceTests()
        {
        super(CACHE_CONFIG);
        }

    // ----- test lifecycle -------------------------------------------------

    /**
     * Initialize the test class.
     */
    @BeforeClass
    public static void _startup()
        {
        System.setProperty("coherence.management", "all");
        System.setProperty("coherence.management.remote", "true");
        System.setProperty("coherence.management.refresh.expiry", "1s");
        System.setProperty("coherence.management.http", "inherit");
        System.setProperty("coherence.management.metrics.port", "0");
        System.setProperty("coherence.override", "common-tangosol-coherence-override.xml");

        AbstractFunctionalTest._startup();
        }

    // ----- tests ----------------------------------------------------------

    /**
     * Tests persistence and active recovery with write-behind.
     */
    @Test
    public void testPersistenceWithWriteBehindAndActivePersistence()
            throws IOException
        {
        testPersistenceWithWriteBehind("testWriteBehindActive-BDB", "active", /*fMetrics*/false);
        }

    /**
     * Tests persistence and snapshot recovery with write-behind.
     */
    @Test
    public void testPersistenceWithWriteBehindAndOnDemandPersistence()
            throws IOException
        {
        testPersistenceWithWriteBehind("testWriteBehindOnDemand-BDB", "on-demand", /*fMetrics*/false);
        }

    /**
     * Tests persistence and snapshot recovery with write-behind.
     */
    @Test
    public void testPersistenceMetrics()
            throws IOException
        {
        testPersistenceWithWriteBehind("testWriteBehindOnDemand-BDB", "active", /*fMetrics*/true);
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Execute the tests for on-demand or active persistence.
     *
     * @param sServer          server name
     * @param sPersistenceMode persistence mode
     *
     * @throws IOException if any errors
     */
    private void testPersistenceWithWriteBehind(String sServer, String sPersistenceMode, boolean fMetrics)
            throws IOException
        {
        final int DELAY_SECONDS = 30;
        final int MAX = 10;

        File fileActive   = FileHelper.createTempDir();
        File fileSnapshot = FileHelper.createTempDir();
        File fileTrash    = FileHelper.createTempDir();
        fileWriteBehind   = FileHelper.createTempDir();

        Properties props = new Properties();
        props.setProperty("test.persistence.active.dir", fileActive.getAbsolutePath());
        props.setProperty("test.persistence.snapshot.dir", fileSnapshot.getAbsolutePath());
        props.setProperty("test.persistence.trash.dir", fileTrash.getAbsolutePath());
        props.setProperty("test.write-behind.dir", fileWriteBehind.getAbsolutePath());
        props.setProperty("test.write.delay", DELAY_SECONDS + "s");
        props.setProperty("test.persistence.mode", sPersistenceMode);
        props.setProperty("coherence.distribution.2server", "false");
        props.setProperty("coherence.management", "all");
        props.setProperty("coherence.override", "common-tangosol-coherence-override.xml");

        Cluster cluster = CacheFactory.getCluster();

        CoherenceClusterMember clusterMember1 = startCacheServer(sServer + "-1", PROJECT_NAME, CACHE_CONFIG, props);
        CoherenceClusterMember clusterMember2 = startCacheServer(sServer + "-2", PROJECT_NAME, CACHE_CONFIG, props);

        Eventually.assertThat(invoking(cluster).getMemberSet().size(), is(3));

        NamedCache<Integer, String> cache   = getNamedCache("simple-persistent");
        DistributedCacheService     service = (DistributedCacheService) cache.getCacheService();
        String                      sService = service.getInfo().getServiceName();

        Eventually.assertThat(invoking(clusterMember1).isServiceRunning(sService), is(true));
        Eventually.assertThat(invoking(clusterMember2).isServiceRunning(sService), is(true));

        waitForBalanced(service);

        PersistenceTestHelper helper = new PersistenceTestHelper();

        try
            {
            long nDelayMillis = DELAY_SECONDS * 1000L;

            cache.clear();

            if (fMetrics)
                {
                ValueExtractor ext1 = new ReflectionExtractor("toString");
                ValueExtractor ext2 = new ReflectionExtractor("hashCode");

                cache.addIndex(ext1, false, null);
                cache.addIndex(ext2, true, SafeComparator.INSTANCE);
                }

            // assert that nothing is on disk
            assertFileStatus(false, MAX);

            // add the data
            for (int i = 0; i < MAX; i++)
                {
                cache.put(i, "value-" + i);
                }

            assertEquals(MAX, cache.size());

            // assert that nothing is written from write-behind
            assertFileStatus(false, MAX);

            if ("active".equals(sPersistenceMode))
                {
                stopCacheServer(sServer + "-1");
                stopCacheServer(sServer + "-2");

                // start the cache servers again
                clusterMember1 = startCacheServer(sServer + "-1", PROJECT_NAME, getCacheConfigPath(), props);
                clusterMember2 = startCacheServer(sServer + "-2", PROJECT_NAME, getCacheConfigPath(), props);

                Eventually.assertThat(invoking(clusterMember1).isServiceRunning(sService), is(true));
                Eventually.assertThat(invoking(clusterMember2).isServiceRunning(sService), is(true));

                waitForBalanced(service);

                // at this point the data should still not be written
                assertFileStatus(false, MAX);

                // wait for write behind time and entries should have been written
                Base.sleep(nDelayMillis);

                // assert that everything has eventually been written from write-behind
                assertFileStatus(true, MAX);

                if (fMetrics)
                    {
                    long  lMax      = (long) getMbeanAttribute(sService, clusterMember1.getLocalMemberId(), "PersistenceLatencyMax");
                    float flAverage = (float) getMbeanAttribute(sService, clusterMember1.getLocalMemberId(), "PersistenceLatencyAverage");
                    assertTrue("The max latency is too big! " + lMax, lMax >= 0 && lMax < 60000);
                    assertTrue("The average latency is too big! " + flAverage, flAverage >= 0 && flAverage < 60000);
                    }
                }
            else
                {
                helper.createSnapshot(sService, "snapshot-1");
                helper.recoverSnapshot(sService, "snapshot-1");

                // currently on-demand persistence will flush the write-behind queue
                //on suspend service for recovery, so the files will be there
                assertFileStatus(true, MAX);
                }

            stopCacheServer(sServer + "-1");
            stopCacheServer(sServer + "-2");
            }
        catch (Exception e)
            {
            e.printStackTrace();
            fail(e.getMessage());
            }
        finally
            {
            stopAllApplications();
            CacheFactory.shutdown();
            FileHelper.deleteDirSilent(fileActive);
            FileHelper.deleteDirSilent(fileSnapshot);
            FileHelper.deleteDirSilent(fileTrash);
            FileHelper.deleteDirSilent(fileWriteBehind);
            }
        }

    /**
     * Assert the files either exist or not.
     *
     * @param fExists ture if they should exist
     * @param nMax number of entries to check
     */
    private void assertFileStatus(boolean fExists, int nMax)
        {
        for (int i = 0; i < nMax; i++)
            {
            Eventually.assertThat(invoking(this).keyFileExists(i), is(fExists));
            }
        }

    /**
     * Returns true if the file for the given key exists.
     *
     * @param nKey key to check
     * @return true if the file for the given key exists
     */
    public boolean keyFileExists(int nKey)
        {
        return BerkeleyDBWriteBehindPersistenceTests.FileCacheStore.getFile(fileWriteBehind, nKey).exists();
        }

    /**
     * Helper function to an attribute from ServiceMBean.
     *
     * @param  sService   the service name that is tried to get this attribute from
     * @param  nodeId     the node Id of the member that is running the service
     * @param  sAttribute the attribute name
     *
     * @return  the attribute value in Object.
     */
    public Object getMbeanAttribute(String sService, int nodeId, String sAttribute)
        {
        try
            {
            MBeanServer server    = MBeanHelper.findMBeanServer();
            ObjectName oBeanName = new ObjectName("Coherence:type=Service,name=" + sService + ",nodeId=" + nodeId);

            return server.getAttribute(oBeanName, sAttribute);
            }
        catch (Exception e)
            {
            Assert.fail(printStackTrace(e));
            }
        return -1;
        }
        
    /**
     * A trivial implementation of a {@link CacheStore} which stores values in files
     * with the name of the key under a specific directory.
     *
     * @author Tim Middleton 2020.02.18
     */
    public static class FileCacheStore
            implements CacheStore<Integer, String>
        {

        // ----- constructors ---------------------------------------------------

        /**
         * Constructs a {@link FileCacheStore} with the given directory.
         *
         * @param sDirectoryName directory to use
         */
        public FileCacheStore(String sDirectoryName)
            {
            if (sDirectoryName == null || sDirectoryName.equals(""))
                {
                throw new IllegalArgumentException("A directory must be specified");
                }

            f_fileDirectory = new File(sDirectoryName);
            if (!f_fileDirectory.isDirectory() || !f_fileDirectory.canWrite())
                {
                throw new IllegalArgumentException("Unable to open directory " + f_fileDirectory);
                }
            }

        // ----- CacheStore interface -------------------------------------------

        @Override
        public void store(Integer nKey, String sValue)
            {
            try
                {
                BufferedWriter writer = new BufferedWriter(new FileWriter(getFile(f_fileDirectory, nKey), false));
                writer.write(sValue);
                writer.close();
                }
            catch (IOException e)
                {
                throw new RuntimeException("Unable to store key " + nKey, e);
                }
            }

        @Override
        public void erase(Integer nKey)
            {
            // we ignore result of delete as the key may not exist
            getFile(f_fileDirectory, nKey).delete();
            }

        @Override
        public String load(Integer nKey)
            {
            File file = getFile(f_fileDirectory, nKey);
            try
                {
                return Files.readAllLines(file.toPath()).get(0);
                }
            catch (IOException e)
                {
                return null;  // does not exist in cache store
                }
            }

        /**
         * Returns the {@link File} that would contain the key.
         *
         * @param fileDirectory base directory
         * @param nKey          key
         *
         * @return a new {@link File}
         */
        protected static File getFile(File fileDirectory, Integer nKey)
            {
            return new File(fileDirectory, nKey + ".txt");
            }

        // ----- data members ---------------------------------------------------

        /**
         * Base directory off which to store data.
         */
        private final File f_fileDirectory;
        }

    // ----- constants -----------------------------------------------------------

    /**
     * Cache config.
     */
    private static final String CACHE_CONFIG = "write-behind-persistence-bdb-cache-config.xml";

    /**
     * Project name.
     */
    private static final String PROJECT_NAME = "persistence";
    
    // ----- data members --------------------------------------------------------

    /**
     * Write-behind directory.
     */
    private File fileWriteBehind;
    }
