/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package persistence;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.coherence.persistence.PersistenceManager;

import com.tangosol.io.FileHelper;
import com.tangosol.io.ReadBuffer;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.NamedCache;

import com.tangosol.persistence.bdb.BerkeleyDBManager;

import org.junit.Test;

import javax.management.MBeanException;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;

/**
 * Functional tests for simple cache persistence and recovery using the
 * BerkeleyDBPersistenceManager.
 *
 * @author jh  2012.10.18
 */
public class BerkeleyDBSimplePersistenceTests
        extends AbstractSimplePersistenceTests
    {

    // ----- AbstractSimplePersistenceTests methods -------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    protected PersistenceManager<ReadBuffer> createPersistenceManager(File file)
            throws IOException
        {
        return new BerkeleyDBManager(file, null, null);
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPersistenceManagerName()
        {
        return "BDB";
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCacheConfigPath()
        {
        return "simple-persistence-bdb-cache-config.xml";
        }

    /**
     * Test multiple restarts with ensure cache for active persistence.
     */
    @Test
    public void testRestartsWithEnsureCacheForActiveMode()
            throws IOException, MBeanException
        {
        testMultipleRestartsWithClientEnsureCache("active");
        }

    /**
     * Test multiple restarts with ensure cache for async-active persistence.
     */
    @Test
    public void testRestartsWithEnsureCacheForAsyncActiveMode()
            throws IOException, MBeanException
        {
        testMultipleRestartsWithClientEnsureCache("active-async");
        }

    /**
     * Test multiple restarts with ensure cache before storage nodes start.
     */
    public void testMultipleRestartsWithClientEnsureCache(String sMode)
            throws IOException, MBeanException
        {
        File fileSnapshot = FileHelper.createTempDir();
        File fileActive   = FileHelper.createTempDir();
        File fileTrash    = FileHelper.createTempDir();

        Properties props = new Properties();
        props.setProperty("test.persistence.mode", sMode);
        props.setProperty("test.persistence.active.dir", fileActive.getAbsolutePath());
        props.setProperty("test.persistence.trash.dir", fileTrash.getAbsolutePath());
        props.setProperty("test.persistence.snapshot.dir", fileSnapshot.getAbsolutePath());
        props.setProperty("test.threads", "5");
        props.setProperty("test.persistence.members", "3");

        final String            sServer          = "testMultipleRestartsWithClientEnsureCache";
        final String            sPersistentCache = "simple-persistent";
        NamedCache              cache            = getNamedCache(sPersistentCache);
        DistributedCacheService service          = (DistributedCacheService) cache.getCacheService();
        Cluster                 cluster          = CacheFactory.ensureCluster();

        String sServer1;
        String sServer2;
        String sServer3;

        int i             = 0;
        int nRestartCount = 3;
        try
            {
            while (++i <= nRestartCount)
                {
                System.out.println("**** Iteration: " + i + " of " + nRestartCount);
                sServer1 = sServer + "-" + (i*3 - 1);
                sServer2 = sServer + "-" + (i*3 - 2);
                sServer3 = sServer + "-" + (i*3);
                startCacheServer(sServer1, getProjectName(), getCacheConfigPath(), props);
                startCacheServer(sServer2, getProjectName(), getCacheConfigPath(), props);
                startCacheServer(sServer3, getProjectName(), getCacheConfigPath(), props);

                Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(3));
                waitForBalanced(service);

                // populate with some data if first time only
                if (i == 1)
                    {
                    PersistenceTestHelper.populateData(cache, 5000);
                    }
                else
                    {
                    service = (DistributedCacheService) cache.getCacheService();
                    }

                // always assert the size to ensure we have not lost data
                assertEquals(cache.size(), 5000);

                String  sService = service.getInfo().getServiceName();

                cluster.suspendService(sService);

                try
                    {
                    // abruptly shutdown
                    stopCacheServer(sServer1);
                    stopCacheServer(sServer2);
                    stopCacheServer(sServer3);

                    Eventually.assertThat(cluster.getMemberSet().size(), is(1));
                    }
                finally
                    {
                    cluster.resumeService(sService);
                    }

                service.shutdown();

                try
                    {
                    cache = getNamedCache(sPersistentCache);
                    cache.size();
                    }
                catch (Throwable t)
                    {
                    CacheFactory.log("got Exception: " + t);
                    }
                }
            }
        finally
            {
            CacheFactory.shutdown();

            FileHelper.deleteDirSilent(fileActive);
            FileHelper.deleteDirSilent(fileSnapshot);
            FileHelper.deleteDirSilent(fileTrash);
            }
        }
    }
