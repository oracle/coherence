/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package persistence;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import com.tangosol.io.FileHelper;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

import java.io.File;
import java.io.IOException;

import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.within;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.number.OrderingComparison.lessThan;

/**
 * Test BerkeleyDB environment maintenance.
 */
public class BerkeleyDBCleanupTests
        extends AbstractFunctionalTest
    {

    // ----- test lifecycle -------------------------------------------------

    /**
     * Initialize the test class.
     */
    @BeforeClass
    public static void _startup()
        {
        try
            {
            s_fileActive   = FileHelper.createTempDir();
            s_fileSnapshot = FileHelper.createTempDir();
            s_fileTrash    = FileHelper.createTempDir();
            }
        catch (IOException e)
            {
            throw ensureRuntimeException(e);
            }
        }

    /**
     * Shutdown the test class.
     */
    @AfterClass
    public static void shutdown()
        {
        try
            {
            FileHelper.deleteDir(s_fileActive);
            FileHelper.deleteDir(s_fileSnapshot);
            FileHelper.deleteDir(s_fileTrash);
            }
        catch (IOException e)
            {
            // ignore
            }
        }

    @Before
    public void beforeTests()
        {
        CacheFactory.shutdown();
        }

    // ----- tests ----------------------------------------------------------

    @Test
    public void test() throws InterruptedException
        {
        try
            {
            System.setProperty("test.persistence.active.dir",    s_fileActive.getAbsolutePath());
            System.setProperty("test.persistence.snapshot.dir",  s_fileSnapshot.getAbsolutePath());
            System.setProperty("test.persistence.trash.dir",     s_fileTrash.getAbsolutePath());
            System.setProperty("coherence.cacheconfig", CFG_FILE);
            System.setProperty("coherence.override", "common-tangosol-coherence-override.xml");
            System.setProperty("je.log.fileMax", "1000000");

            NamedCache cache = getNamedCache("simple-persistent-1");
            char[] val       = new char[1_000_000];
            for (int i = 0; i < 100; i++)
                {
                cache.put(i, val);
                }

            long size = FileHelper.sizeDir(s_fileActive);
            cache.clear();
            Eventually.assertDeferred(() -> FileHelper.sizeDir(s_fileActive), is(lessThan(size / 5)), within(2, TimeUnit.MINUTES));
            }
        finally
            {
            CacheFactory.shutdown();
            }
        }

    // ----- accessors ------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    public String getCacheConfigPath()
        {
        return CFG_FILE;
        }

    /**
     * {@inheritDoc}
     */
    public static String getProjectName()
        {
        return "persistence";
        }

    // ----- constants ------------------------------------------------------

    protected static final String CFG_FILE = "cleanup-bdb-cache-config.xml";

    // ----- data members ---------------------------------------------------

    private static File s_fileActive;
    private static File s_fileSnapshot;
    private static File s_fileTrash;
    }
