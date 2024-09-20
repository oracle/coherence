/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package persistence;

import com.tangosol.io.FileHelper;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.NamedCache;
import com.tangosol.net.cache.TypeAssertion;
import com.tangosol.util.Base;
import com.oracle.coherence.testing.AbstractFunctionalTest;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static persistence.AbstractConfigurationPersistenceTests.getProjectName;

/**
 * Functional persistence tests using a thread pool of 1 thread.
 *
 * @author lh  2019.07.25
 */
public class PersistenceThreadTests
        extends AbstractFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public PersistenceThreadTests()
        {
        super("persistence-thread-cache-config.xml");
        }

    // ----- test lifecycle -------------------------------------------------

    /**
     * Initialize the test class.
     */
    @BeforeClass
    public static void _startup()
        {
        // this test requires local storage to be enabled
        try
            {
            Properties props  = System.getProperties();

            m_fileActive   = FileHelper.createTempDir();
            m_fileSnapshot = FileHelper.createTempDir();
            m_fileTrash    = FileHelper.createTempDir();
            props.setProperty("coherence.distributed.localstorage", "true");
            props.setProperty("coherence.distributed.persistence.active.dir", m_fileActive.getAbsolutePath());
            props.setProperty("coherence.distributed.persistence.snapshot.dir", m_fileSnapshot.getAbsolutePath());
            props.setProperty("coherence.distributed.persistence.trash.dir", m_fileTrash.getAbsolutePath());
            props.setProperty("coherence.distributed.persistence.mode", "active");
            }
        catch (IOException e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    /**
     * Initialize each test
     * <p>
     * This method starts the Coherence cluster, if it isn't already running.
     */
    @Before
    public void _initTest()
        {
        setupProps();

        startCluster();
        }

    /**
     * Clean up after each test.
     * <p/>
     * This method stops the Coherence cluster.
     */
    @After
    public void _cleanTest()
        {
        stopAllApplications();
        CacheFactory.shutdown();
        FileHelper.deleteDirSilent(m_fileActive);
        FileHelper.deleteDirSilent(m_fileSnapshot);
        FileHelper.deleteDirSilent(m_fileTrash);

        // don't use the out() method, as it will restart the cluster
        System.out.println(createMessageHeader() + " <<<<<<< Stopped cluster");
        }

    // ----- tests ----------------------------------------------------------

    /**
     * Test persistence with a thread pool of 1 thread.
     */
    @Test
    public void testPersistenceWith1Thread()
        {
        String                       sServer = "PersistenceWith1Thread";
        NamedCache<Integer, Integer> cache   = null;
        try
            {
            AbstractFunctionalTest.setupProps();
            startCacheServer(sServer, getProjectName(), getCacheConfigPath(), null, false);
            AbstractFunctionalTest.startCluster();

            waitForBalanced((CacheService) getFactory().ensureService("DistributedCachePersistenceDefault"));

            cache = getFactory().ensureTypedCache("default-persistent", null, TypeAssertion.withRawTypes());

            Map<Integer, Integer> mapBuffer = new HashMap<>();
            int                   BATCH     = 1000;
            for (int i = 0; i < 2000; i++)
                {
                mapBuffer.put(i, i);

                if (i % BATCH == 0)
                    {
                    cache.putAll(mapBuffer);
                    mapBuffer.clear();
                    }
                }

            if (!mapBuffer.isEmpty())
                {
                cache.putAll(mapBuffer);
                }

            assertEquals(2000, cache.size());
            }
        finally
            {
            cache.destroy();
            stopCacheServer(sServer);
            }
        }

    // ----- data members ------------------------------------------------------

    private static File m_fileActive;
    private static File m_fileSnapshot;
    private static File m_fileTrash;
    }
