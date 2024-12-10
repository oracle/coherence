/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package config;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Random;

import com.tangosol.io.FileHelper;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.tangosol.coherence.config.CacheConfig;
import com.tangosol.coherence.config.builder.storemanager.AbstractNioManagerBuilder;
import com.tangosol.coherence.config.builder.storemanager.NioFileManagerBuilder;
import com.tangosol.coherence.config.scheme.ExternalScheme;
import com.tangosol.config.expression.NullParameterResolver;
import com.tangosol.config.expression.ParameterResolver;
import com.tangosol.io.AsyncBinaryStore;
import com.tangosol.io.BinaryStore;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.cache.SerializationMap;
import com.tangosol.util.Base;
import com.oracle.coherence.testing.AbstractFunctionalTest;
import com.oracle.coherence.testing.TestHelper;

/**
 * A collection of functional tests that test the configuration of all
 * the Binary storage manager configuration elements.  Note that
 * {@link SchemeSelectionTests} includes tests for custom BinaryStorageManagers.
 *
 * @author pfm 2012.05.07
 */
public class BinaryStorageManagerConfigTests
        extends AbstractFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor.
    */
    public BinaryStorageManagerConfigTests()
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

        // get the temp directory. java.io.tmpdir doesn't work on linux so get
        // the directory from a temp file
        String tmpDir  = null;
        try
            {
            File f = File.createTempFile("pre", "suffix");
            f.deleteOnExit();
            tmpDir = f.getParent();
            if (!tmpDir.endsWith(File.separator))
                {
                tmpDir += File.separator;
                }
            }
        catch (IOException e)
            {
            throw Base.ensureRuntimeException(e);
            }

        Random rand = new Random();

        m_sBdbDir = tmpDir + BDB_TMP_DIR + rand.nextInt();
        System.setProperty("coherence.bdb.tmpdir", m_sBdbDir);

        try
            {
            m_sFileTestBdbDir = FileHelper.createTempDir();
            m_sFileTestBdbDir.deleteOnExit();
            }
        catch (IOException e) {}
        System.setProperty("test.bdb.tmpdir", m_sFileTestBdbDir.getAbsolutePath());


        m_sNioDir = tmpDir + NIO_TMP_DIR + rand.nextInt();
        System.setProperty("coherence.nio.tmpdir", m_sNioDir);

        AbstractFunctionalTest._startup();
        }

    // ----- test methods ---------------------------------------------------

    @Test
    public void testConfigureBDBStoreManagerWithInitParams()
        throws IOException
        {
        NamedCache cache = getNamedCache("external-configured-bdb-cache");
        for (int i = 0; i < 100; i++)
            {
            cache.put(i, "value_" + i);
            }
        assertFalse(new File(m_sFileTestBdbDir.getAbsolutePath() + File.separator + "je.stat.csv").exists());
        cache.release();
        }

    /**
     * Test the default async configuration.
     */
    @Test
    public void testAsyncDefault() throws Exception
        {
        NamedCache cache = getNamedCache("external-async-default");
        cache.release();
        }

    /**
     * Test the async configuration with specified values.
     */
    @Test
    public void testAsyncSpecified() throws Exception
        {
        NamedCache cache = getNamedCache("external-async-specified");

        Map map = TestHelper.getBackingMap(cache);
        assertTrue(map instanceof SerializationMap);
        AsyncBinaryStore store = (AsyncBinaryStore) ((SerializationMap) map).getBinaryStore();
        Assert.assertEquals(10 * MB, store.getQueuedLimit());
        cache.release();
        }

    /**
     * Test the default BDB configuration.
     */
    @Test
    public void testBdbDefault() throws Exception
        {
        NamedCache cache = getNamedCache("external-bdb-default");
        cache.release();
        }

    /**
     * Test the BDB configuration with specified values. There is no way to
     * get the database name used other than looking for the name
     * in the toString output.
     */
    @Test
    public void testBdbSpecified() throws Exception
        {
        testPersistence("external-bdb-specified", m_sBdbDir);
        }

    /**
     * Test the default NIO file configuration.
     */
    @Test
    public void testNioFileDefault() throws Exception
        {
        NamedCache cache = getNamedCache("external-nio-file-default");
        cache.release();
        }

    /**
     * Test the NIO configuration with specified values.  There is no way
     * to check the internal NIO settings so verify that the scheme is correct.
     */
    @Test
    public void testNioFileSpecified() throws Exception
        {
        ParameterResolver resolver = new NullParameterResolver();

        NioFileManagerBuilder builder = testNio("external-nio-file-specified", 10 * MB, 100 * MB);

        String sDir = m_sNioDir;
        Assert.assertEquals(sDir, builder.getDirectory(resolver));
        TestHelper.markFileDeleteOnExit(sDir);
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Create the BinaryStore which uses persistence and test if the directory
     * is used.
     *
     * @param sCacheName  the cache name
     * @param sDir        the store directory name
     */
    protected <T extends BinaryStore> void testPersistence(String sCacheName, String sDir)
        throws Exception
        {
        File dir = new File(sDir);
        dir.mkdir();
        assertTrue(dir.isDirectory());

        NamedCache cache = getNamedCache(sCacheName);

        File[] files = dir.listFiles();
        assertTrue(files.length > 0);

        Map map = TestHelper.getBackingMap(cache);
        assertTrue(map instanceof SerializationMap);
        T store = (T) ((SerializationMap) map).getBinaryStore();
        assertTrue(store.toString().contains(sCacheName));

        cache.release();
        TestHelper.markFileDeleteOnExit(dir);
        }

    /**
     * Test the NioManager common configuration.
     *
     * @param sCacheName  the cache name
     * @param cInit       the initial size
     * @param cMax        the maximum size
     *
     * @return the AbstractNioManagerBuilder
     *
     * @throws Exception
     */
    protected <T extends AbstractNioManagerBuilder> T testNio(String sCacheName,
            int cInit, int cMax) throws Exception
        {
        T builder = null;

        ConfigurableCacheFactory factory = CacheFactory.getCacheFactoryBuilder()
            .getConfigurableCacheFactory(FILE_CFG_CACHE, null);

        if (factory instanceof ExtensibleConfigurableCacheFactory)
            {
            CacheConfig    config = ((ExtensibleConfigurableCacheFactory) factory).getCacheConfig();
            ExternalScheme scheme = (ExternalScheme) config.findSchemeByCacheName(sCacheName);

            builder = (T) scheme.getBinaryStoreManagerBuilder();

            ParameterResolver resolver = config.getDefaultParameterResolver();
            
            Assert.assertEquals(cInit, builder.getInitialSize(resolver));
            Assert.assertEquals(cMax, builder.getMaximumSize(resolver));
            }

        return  builder;
        }

    // ----- data members ---------------------------------------------------

    /**
     * Megabytes.
     */
    private static final int MB = 1024 * 1024;

    /**
    * The file name of the default cache configuration file used by this test.
    */
    public static final String FILE_CFG_CACHE = "binary-storage-manager-cache-config.xml";

    /**
     * BDB temporary directory base.
     */
    private static final String BDB_TMP_DIR = "coherence.bdb.tmpdir";

    /**
     * NIO temporary directory base.
     */
    private static final String NIO_TMP_DIR = "coherence.nio.tmpdir";

    /**
     * BDB temporary directory.
     */
    private static String m_sBdbDir;

    /**
     * Isolated BDB temporary directory.
     */
    private static File m_sFileTestBdbDir;

    /**
     * NIO temporary directory.
     */
    private static String m_sNioDir;
    }
