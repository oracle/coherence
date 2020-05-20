/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package events;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.coherence.common.internal.util.HeapDump;
import com.tangosol.coherence.dslquery.internal.PersistenceToolsHelper;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.Member;
import com.tangosol.net.NamedCache;

import com.tangosol.net.events.partition.TransferEvent;

import com.tangosol.io.FileHelper;

import com.tangosol.net.management.MBeanServerProxy;
import common.AbstractFunctionalTest;

import events.common.AbstractTestInterceptor.Expectations;
import events.common.AbstractTestInterceptor.ExpectationsInformerInvocable;
import events.common.TestTransferInterceptor;
import events.common.TestTransferInterceptor.RecoverTransferExpectation;
import events.common.TestTransferInterceptor.TransferExpectation;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.management.MBeanException;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.Collections;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

/**
 * Basic tests for events.
 */
public class RecoverEventTests
        extends AbstractFunctionalTest
    {
    @BeforeClass
    public static void _startup()
        {
        // we will control the startup manually
        System.setProperty("tangosol.coherence.distributed.localstorage", "true");
        AbstractFunctionalTest.setupProps();
        }

    @AfterClass
    public static void _shutdown()
        {
        // we will control the shutdown manually
        }

    @After
    public void cleanup()
        {
        AbstractFunctionalTest._shutdown();
        }


    // ----- test methods ---------------------------------------------------

    /**
     * Test persistence RECOVER events
     */
    @Test
    public void testRecoverEvents()
            throws IOException
        {
        AbstractFunctionalTest._shutdown();

        File fileActive   = FileHelper.createTempDir();
        File fileSnapshot = FileHelper.createTempDir();
        File fileTrash    = FileHelper.createTempDir();

        System.setProperty("test.persistence.active.dir", fileActive.getAbsolutePath());
        System.setProperty("test.persistence.snapshot.dir", fileSnapshot.getAbsolutePath());
        System.setProperty("test.persistence.trash.dir", fileTrash.getAbsolutePath());
        System.setProperty("tangosol.coherence.cacheconfig", "event-persistence-cache-config.xml");
        System.setProperty("tangosol.coherence.override", "event-override.xml");

        String sIdentifier = TestTransferInterceptor.IDENTIFIER;

        new ExpectationsInformerInvocable(sIdentifier)
                .addExpectation(TransferEvent.Type.ASSIGNED, new Expectations().
                        expect(new TransferExpectation(TransferEvent.Type.ASSIGNED, is(257))))
                .run();

        String                   sPersistentCache = "simple-persistent";
        String                   sTransientCache  = "simple-transient";
        ConfigurableCacheFactory ccf              = getFactory();
        try
            {
            Cluster cluster = AbstractFunctionalTest.startCluster();

            NamedCache cache  = CacheFactory.getCache(sPersistentCache);
            NamedCache cache1 = CacheFactory.getCache(sTransientCache);

            DistributedCacheService service = (DistributedCacheService) cache.getCacheService();
            int                cParts       = service.getPartitionCount();
            Member             memberLocal  = service.getCluster().getLocalMember();

            Eventually.assertThat(invoking(new EventTestHelper()).remoteFail(sIdentifier, ccf, Collections.singleton(memberLocal)), is(cParts));

            // assert that the persistence root was created
            File dir = new File(fileActive, FileHelper.toFilename(cluster.getClusterName()));

            dir = new File(dir, FileHelper.toFilename(service.getInfo().getServiceName()));
            assertTrue(dir.exists());

            // expect all partitions with cache entries are recovered when server restart
            Expectations exptsMember = new Expectations()
                    .expect(new TransferExpectation(TransferEvent.Type.RECOVERED, is(cParts)));

            for (int x = 0; x <10; x++)
                {
                cache.put(x, x);
                cache1.put(x, x);
                //expect all the entries are recovered
                exptsMember.expect(cache.getCacheName(), TransferEvent.Type.RECOVERED, x, x);
                }

            // inform this member of the partition recover about to occur
            new ExpectationsInformerInvocable(sIdentifier)
                    .addExpectation(TransferEvent.Type.RECOVERED, exptsMember)
                    .run();

            AbstractFunctionalTest._shutdown();

            AbstractFunctionalTest.startCluster();

            cache  = CacheFactory.getCache(sPersistentCache);
            cache1 = CacheFactory.getCache(sTransientCache);

            assertEquals(10, cache.size());
            assertEquals(0, cache1.size());

            Eventually.assertThat(invoking(new EventTestHelper()).remoteFail(sIdentifier, ccf, Collections.singleton(memberLocal)), is(cParts));
            }
        finally
            {
            try
                {
                AbstractFunctionalTest._shutdown();
                }
            finally
                {
                try
                    {
                    FileHelper.deleteDir(fileActive);
                    FileHelper.deleteDir(fileSnapshot);
                    FileHelper.deleteDir(fileTrash);
                    }
                catch (IOException e)
                    {
                    // ignore
                    }
                }
            }
        }

    /**
     * Test persistence RECOVER events
     */
    //@Ignore
    @Test
    public void testRecoverFromSnapshotEvent()
            throws IOException, MBeanException
        {
        AbstractFunctionalTest._shutdown();

        File fileActive   = FileHelper.createTempDir();
        File fileSnapshot = FileHelper.createTempDir();
        File fileTrash    = FileHelper.createTempDir();

        System.setProperty("test.persistence.active.dir", fileActive.getAbsolutePath());
        System.setProperty("test.persistence.snapshot.dir", fileSnapshot.getAbsolutePath());
        System.setProperty("test.persistence.trash.dir", fileTrash.getAbsolutePath());
        System.setProperty("tangosol.coherence.cacheconfig", "event-persistence-cache-config.xml");
        System.setProperty("tangosol.coherence.override", "event-override.xml");

        String sIdentifier      = TestTransferInterceptor.IDENTIFIER;
        String sPersistentCache = "simple-persistent";
        String sTransientCache  = "simple-transient";
        String sSnapshotName    = "testRecoverEvent";

        ConfigurableCacheFactory ccf = getFactory();
        try
            {
            Cluster    cluster = AbstractFunctionalTest.startCluster();
            NamedCache cache   = CacheFactory.getCache(sPersistentCache);
            NamedCache cache1  = CacheFactory.getCache(sTransientCache);

            DistributedCacheService service = (DistributedCacheService) cache.getCacheService();
            int                cParts       = service.getPartitionCount();
            Member             memberLocal  = service.getCluster().getLocalMember();

            // assert that the persistence root was created
            File dir = new File(fileActive, FileHelper.toFilename(cluster.getClusterName()));
                 dir = new File(dir, FileHelper.toFilename(service.getInfo().getServiceName()));

            assertTrue(dir.exists());

            // expect all partitions with cache entries are recovered when server restart
            Expectations exptsMember = new Expectations().expect(new RecoverTransferExpectation(is(cParts), sSnapshotName));

            for (int x = 0; x <10; x++)
                {
                cache.put(x, x);
                cache1.put(x, x);
                //expect all the entries are recovered
                exptsMember.expect(cache.getCacheName(), TransferEvent.Type.RECOVERED, x, x);
                }

            PersistenceToolsHelper persistence = new PersistenceToolsHelper(new PrintWriter(System.out));

            ensureMBean(service, persistence);
            persistence.invokeOperationWithWait(PersistenceToolsHelper.CREATE_SNAPSHOT,
                    sSnapshotName, service.getInfo().getServiceName());
            cache.truncate();
            cache1.truncate();

            assertEquals(0, cache.size());
            assertEquals(0, cache1.size());

            service.shutdown();

            cache  = CacheFactory.getCache(sPersistentCache);
            cache1 = CacheFactory.getCache(sTransientCache);

            assertEquals(0, cache.size());
            assertEquals(0, cache1.size());

            EventTestHelper.remoteReset(sIdentifier, ccf, Collections.singleton(memberLocal));

            // inform this member of the partition recover about to occur
            new ExpectationsInformerInvocable(sIdentifier)
                    .addExpectation(TransferEvent.Type.RECOVERED, exptsMember)
                    .run();

            ensureMBean(service, persistence);
            persistence.invokeOperationWithWait(PersistenceToolsHelper.RECOVER_SNAPSHOT,
                    sSnapshotName, service.getInfo().getServiceName());

            // TODO: this use of invoking and remoteFail is questionable as a
            //       a failure (AssertionError) is not raised until after the
            //       wait interval used by invoking, i.e. 60s
            Eventually.assertThat(invoking(new EventTestHelper()).remoteFail(sIdentifier, ccf, Collections.singleton(memberLocal)), is(cParts));
            }
        finally
            {
            try
                {
                AbstractFunctionalTest._shutdown();
                }
            finally
                {
                try
                    {
                    FileHelper.deleteDir(fileActive);
                    FileHelper.deleteDir(fileSnapshot);
                    FileHelper.deleteDir(fileTrash);
                    }
                catch (IOException e)
                    {
                    // ignore
                    }
                }
            }
        }

    /**
     * Return control when either:
     * <ol>
     *     <li>the persistence mbean has been registered or</li>
     *     <li>the timeout of 60s has been reached</li>
     * </ol>
     * Note: if timeout has been reached all persistence mbeans will be output
     *       to the logger
     *
     * @param service      the service
     * @param persistence  the {@link PersistenceToolsHelper}
     */
    protected void ensureMBean(DistributedCacheService service, PersistenceToolsHelper persistence)
        {
        MBeanServerProxy proxy             = CacheFactory.getCluster().getManagement().getMBeanServerProxy();
        String           sPersistenceMBean = persistence.getPersistenceMBean(service.getInfo().getServiceName());

        try
            {
            Eventually.assertThat(invoking(proxy).isMBeanRegistered(sPersistenceMBean), is(true));
            }
        catch (AssertionError | RuntimeException t)
            {
            CacheFactory.log("Could not find MBean (" + sPersistenceMBean + "); following has been registered:\n" +
                    proxy.queryNames("type=Persistence,*", null), CacheFactory.LOG_INFO);

            String sOut = HeapDump.dumpHeap();
            CacheFactory.log("Dumping heap for analysis here :\n" + sOut, CacheFactory.LOG_INFO);

            throw t;
            }
        }


    // ----- constants ------------------------------------------------------

    /**
     * The Cache config file to use for these tests.
     */
    public static final String CFG_FILE = "event-persistence-cache-config.xml";

    /**
     * The name of the service used by this test class.
     */
    protected static final String SERVICE_NAME = "DistributedCachePersistence";
    }
