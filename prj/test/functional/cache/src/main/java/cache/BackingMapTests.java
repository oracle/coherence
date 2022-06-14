/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package cache;

import com.oracle.coherence.caffeine.CaffeineCache;
import com.tangosol.io.nio.BinaryMap;

import com.tangosol.net.NamedCache;
import com.tangosol.net.cache.LocalCache;
import com.tangosol.net.partition.ObservableSplittingBackingCache;
import com.tangosol.net.partition.PartitionSplittingBackingMap;

import com.tangosol.util.SafeHashMap;

import com.oracle.coherence.testing.AbstractFunctionalTest;
import com.oracle.coherence.testing.TestHelper;

import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * A collection of functional tests for PartitionedCache backing and backup maps.
 *
 * @author pfm  2012.04.19
 */
public class BackingMapTests
        extends AbstractFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public BackingMapTests()
        {
        super(FILE_CFG_CACHE);
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

        // get temp directory for file-mapped storage test
        m_sNioDir = TestHelper.getTempDir("NIO_TMP_DIR");
        TestHelper.markFileDeleteOnExit(m_sNioDir);

        System.setProperty("coherence.nio.tmpdir", m_sNioDir);

        AbstractFunctionalTest._startup();
        }

    // ----- test methods ---------------------------------------------------

    /**
     * Test that the partitioning is used correctly if it is not explicitly specified.
     */
    @Test
    public void testBackupMapPartitionDefault() throws Exception
        {
        NamedCache cache = getNamedCache("dist-backing-map-default");
        TestHelper.validateBackingMapType(cache, LocalCache.class);
        CacheTestHelper.validateBackupMapType(cache, PartitionSplittingBackingMap.class);
        cache.release();
        }

    /**
     * Test that the maps for a distributed cache is partitioned when the partitioned
     * flag is true.
     */
    @Test
    public void testBackupMapPartitioned() throws Exception
        {
        NamedCache cache = getNamedCache("dist-backing-map-partitioned");
        TestHelper.validateBackingMapType(cache, ObservableSplittingBackingCache.class);
        CacheTestHelper.validateBackupMapType(cache, PartitionSplittingBackingMap.class);
        cache.release();
        }

     /**
      * Test that the maps for a distributed cache is partitioned even when the partitioned
      * flag is false.
      */
     @Test
     public void testBackupMapNonPartitioned() throws Exception
         {
         NamedCache cache = getNamedCache("dist-backing-map-non-partitioned");
         TestHelper.validateBackingMapType(cache, LocalCache.class);
         CacheTestHelper.validateBackupMapType(cache, PartitionSplittingBackingMap.class);
         cache.release();
         }

    @Test
    public void testBackupMapPartitionCaffeine() throws Exception
        {
        NamedCache cache = getNamedCache("dist-backing-map-caffeine");
        TestHelper.validateBackingMapType(cache, CaffeineCache.class);
        CacheTestHelper.validateBackupMapType(cache, PartitionSplittingBackingMap.class);
        cache.release();
        }

    @Test
    public void testBackupMapPartitionedCaffeine() throws Exception
        {
        NamedCache cache = getNamedCache("dist-backing-map-partitioned-caffeine");
        TestHelper.validateBackingMapType(cache, ObservableSplittingBackingCache.class);
        CacheTestHelper.validateBackupMapType(cache, PartitionSplittingBackingMap.class);
        cache.release();
        }

     /**
      * Test that the backup map for a distributed cache is partitioned even when the partitioned
      * flag is false.
      */
     @Test
     public void testBackupMapNonPartitionedCaffeine() throws Exception
         {
         NamedCache cache = getNamedCache("dist-backing-map-non-partitioned-caffeine");
         TestHelper.validateBackingMapType(cache, CaffeineCache.class);
         CacheTestHelper.validateBackupMapType(cache, PartitionSplittingBackingMap.class);
         cache.release();
         }

    /**
     * Coh7248  Test that file-mapped backup map is not partitioned by default.
     */
    @Test
    public void testFileMappedDefault() throws Exception
        {
        NamedCache cache = getNamedCache("dist-backup-storage-nio-file");
        TestHelper.validateBackingMapType(cache, LocalCache.class);
        CacheTestHelper.validateBackupMapType(cache, BinaryMap.class);
        cache.release();
        }

    /**
     * Coh-7808  Test that  backup-storage configuration is respected with partitioned backing map.
     */
    @Test
    public void testCOH7808_backupSchemePartitioned() throws Exception
        {
        NamedCache cache = getNamedCache("dist-backup-storage-partitioned");
        Map mapBackup = CacheTestHelper.getBackupMap(cache);
        assertEquals( PartitionSplittingBackingMap.class, mapBackup.getClass());

        PartitionSplittingBackingMap mapSplitting = (PartitionSplittingBackingMap) mapBackup;
        mapSplitting.createPartition(1);
        Map mapPartition = mapSplitting.getPartitionMap(1);
        assertEquals(CustomMap.class, mapPartition.getClass());
        cache.release();
        }

    // ----- inner class CustomMap ------------------------------------------

    public static class CustomMap extends SafeHashMap
        {
        }

    // ----- constants ------------------------------------------------------

    /**
    * The file name of the default cache configuration file used by this test.
    */
    public static String FILE_CFG_CACHE = "backing-map-cache-config.xml";

    /**
     * NIO temporary directory base.
     */
    private static final String NIO_TMP_DIR = "coherence.nio.tmpdir";

    /**
     * NIO temporary directory.
     */
    private static String m_sNioDir;
    }
