/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package persistence;

import com.tangosol.io.FileHelper;

import com.tangosol.util.Base;

import java.io.File;
import java.io.IOException;

import java.util.Properties;

import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * Performance tests for simple cache persistence using BerkeleyDB.
 *
 * @author jh  2012.04.09
 */
public class BerkeleyDBPerformanceTests
        extends AbstractPerformancePersistenceTests
    {

    // ----- constructors ---------------------------------------------------

    public BerkeleyDBPerformanceTests()
        {
        super(FILE_CFG_CACHE);
        }

    // ----- test lifecycle -------------------------------------------------

    /**
     * Initialize the test class.
     */
    @BeforeClass
    public static void startup()
        {
        File fileActive1;
        File fileActive2;
        File fileSnapshot1;
        File fileSnapshot2;
        File fileTrash1;
        File fileTrash2;
        try
            {
            fileActive1   = FileHelper.createTempDir();
            fileActive2   = FileHelper.createTempDir();
            fileSnapshot1 = FileHelper.createTempDir();
            fileSnapshot2 = FileHelper.createTempDir();
            fileTrash1    = FileHelper.createTempDir();
            fileTrash2    = FileHelper.createTempDir();
            }
        catch (IOException e)
            {
            throw Base.ensureRuntimeException(e);
            }

        s_fileActive1   = fileActive1;
        s_fileActive2   = fileActive2;
        s_fileSnapshot1 = fileSnapshot1;
        s_fileSnapshot2 = fileSnapshot2;
        s_fileTrash1    = fileTrash1;
        s_fileTrash2    = fileTrash2;

        Properties props = new Properties();
        props.setProperty("test.persistence.active.dir",   fileActive1.getAbsolutePath());
        props.setProperty("test.persistence.snapshot.dir", fileSnapshot1.getAbsolutePath());
        props.setProperty("test.persistence.trash.dir",    fileTrash1.getAbsolutePath());

        startCacheServer("BerkeleyDBPerformanceTests-1", getProjectName(),
                FILE_CFG_CACHE, props);

        props = new Properties();
        props.setProperty("test.persistence.active.dir",   fileActive2.getAbsolutePath());
        props.setProperty("test.persistence.snapshot.dir", fileSnapshot2.getAbsolutePath());
        props.setProperty("test.persistence.trash.dir",    fileTrash2.getAbsolutePath());

        startCacheServer("BerkeleyDBPerformanceTests-2", getProjectName(),
                FILE_CFG_CACHE, props);
        }

    /**
     * Shutdown the test class.
     */
    @AfterClass
    public static void shutdown()
        {
        stopCacheServer("BerkeleyDBPerformanceTests-1");
        stopCacheServer("BerkeleyDBPerformanceTests-2");

        try
            {
            FileHelper.deleteDir(s_fileActive1);
            FileHelper.deleteDir(s_fileActive2);
            FileHelper.deleteDir(s_fileSnapshot1);
            FileHelper.deleteDir(s_fileSnapshot2);
            FileHelper.deleteDir(s_fileTrash1);
            FileHelper.deleteDir(s_fileTrash2);
            }
        catch (IOException e)
            {
            // ignore
            }
        }

    // ----- constants ------------------------------------------------------

    public static final String FILE_CFG_CACHE = "performance-bdb-cache-config.xml";

    // ----- data members ---------------------------------------------------

    private static File s_fileActive1;
    private static File s_fileActive2;
    private static File s_fileSnapshot1;
    private static File s_fileSnapshot2;
    private static File s_fileTrash1;
    private static File s_fileTrash2;
    }
