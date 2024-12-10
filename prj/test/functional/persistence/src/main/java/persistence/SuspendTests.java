/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package persistence;

import com.oracle.coherence.persistence.PersistenceManager;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.tangosol.io.FileHelper;
import com.tangosol.io.ReadBuffer;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;

import com.tangosol.net.PartitionedService;

import com.tangosol.net.cache.TypeAssertion;

import com.tangosol.net.management.MBeanServerProxy;
import com.tangosol.net.management.Registry;

import com.tangosol.persistence.CachePersistenceHelper;
import com.tangosol.persistence.bdb.BerkeleyDBEnvironment;

import com.tangosol.util.Base;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import org.hamcrest.Matchers;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import java.util.Arrays;
import java.util.Properties;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.core.Is.is;

import static org.junit.Assert.fail;

/**
 * Tests for service suspending, specifically related to persistence as it is
 * the main consumer / dependee of this service level functionality.
 *
 * @author hr  2015.06.22
 */
public class SuspendTests
        extends AbstractFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct the test suite.
     */
    public SuspendTests()
        {
        super(CACHE_CFG);
        }

    // ----- lifecycle methods ----------------------------------------------

    @BeforeClass
    public static void _startup()
        {
        // ensure the test is not an MBean server
        System.setProperty("coherence.management", "none");

        AbstractFunctionalTest._startup();
        }

    // ----- tests ----------------------------------------------------------

    /**
     * Test that ownership senior death during snapshot creation, which automatically
     * suspends the service, is handled correctly such that the service is
     * automatically resumed by the next senior.
     */
    @Test
    public void testSeniorDeathDuringSnapshot()
        {
        String sTestName = "testSeniorDeathDuringSnapshot";

        Properties props = new Properties();
        props.setProperty("test.persistent-environment", "slow-snapshot-environment");
        props.setProperty("test.persistence.active.dir", ACTIVE_DIR);
        props.setProperty("test.persistence.snapshot.dir", SNAPSHOT_DIR);
        props.setProperty("test.persistence.trash.dir", TRASH_DIR);
        props.setProperty("coherence.distributed.threads.min", "2");
        props.setProperty("coherence.management", "none");
        props.setProperty("coherence.override", "common-tangosol-coherence-override.xml");
        System.getProperties().putAll(props);
        try
            {
            startCacheServer(sTestName + "-1", "persistence", CACHE_CFG, props);
            startCacheServer(sTestName + "-2", "persistence", CACHE_CFG, props);
            props.put("coherence.management", "all");
            startCacheServer(sTestName + "-3", "persistence", CACHE_CFG, props);

            ConfigurableCacheFactory factory = CacheFactory.getCacheFactoryBuilder()
                    .getConfigurableCacheFactory("client-cache-config.xml", null);
            setFactory(factory);

            NamedCache<Integer, String> cache = getFactory().ensureTypedCache("simple-persistent", null, TypeAssertion.withRawTypes());

            CacheService service = cache.getCacheService();
            Eventually.assertThat(invoking((PartitionedService) service)
                .getOwnershipEnabledMembers().size(), Matchers.is(3));
            waitForBalanced(service);

            for (int i = 100; i > 0; --i)
                {
                cache.put(i, "val: " + i);
                }

            Registry         registry = CacheFactory.getCluster().getManagement();
            MBeanServerProxy mbsProxy = registry.getMBeanServerProxy();
            String           sMBean   = registry.ensureGlobalName(makeMBeanName(cache.getCacheService()));

            Eventually.assertThat(invoking(mbsProxy).isMBeanRegistered(sMBean), is(true));

            mbsProxy.invoke(sMBean, "createSnapshot", new Object[] {"test"}, null);

            long ldtStart = Base.getSafeTimeMillis();

            Future<Long> futurePut = Executors.newFixedThreadPool(1).submit(() ->
                {
                long ldtStartPut = Base.getSafeTimeMillis();
                cache.put(0, "val: 0");
                long lElapsed = Base.getSafeTimeMillis() - ldtStartPut;

                CacheFactory.log("Put complete in " + lElapsed, CacheFactory.LOG_INFO);

                return lElapsed;
                });

            // after 1s kill the ownership senior
            Base.sleep(500L);
            stopCacheServer(sTestName + "-1");
            Base.sleep(500L);

            // Note: there is a potential for the MBean to become unregistered in-between
            //       the isRegistered and getAttribute calls; the likelihood of
            //       this is reduced by the MBeanServer not being the locally hosted
            Eventually.assertThat(invoking(mbsProxy).isMBeanRegistered(sMBean), is(true));
            Eventually.assertThat(invoking(mbsProxy).getAttribute(sMBean, "OperationStatus"), is("Idle"));

            long lElapsed = Base.getSafeTimeMillis() - ldtStart;
            if (lElapsed < 4000L) // extra logging to track down intermittent failure
                {
                CacheFactory.log("OperationStatus: " + mbsProxy.getAttribute(sMBean, "OperationStatus"), CacheFactory.LOG_INFO);
                Base.sleep(1000L);
                CacheFactory.log("OperationStatus: " + mbsProxy.getAttribute(sMBean, "OperationStatus"), CacheFactory.LOG_INFO);
                }

            // time taken for snapshot should be about 5s
            assertThat(Base.getSafeTimeMillis() - ldtStart, greaterThan(4000L));
            // time taken for put to complete should be slightly over 5s
            assertThat(futurePut.get(30L, TimeUnit.SECONDS), greaterThan(4000L));
            }
        catch (InterruptedException | ExecutionException | TimeoutException e)
            {
            fail("Put request submitted during snapshot creation never completed.");
            }
        finally
            {
            stopAllApplications();
            Arrays.asList(ACTIVE_DIR, SNAPSHOT_DIR, TRASH_DIR)
                .forEach(sPath -> FileHelper.deleteDirSilent(new File(sPath)));
            }
        }

    // ----- helpers --------------------------------------------------------

    /**
     * Create an MBean name that will be used by the ownership senior to register
     * the PersistenceManagerMBean.
     *
     * @param service  the service that will register the MBean
     *
     * @return an MBean name that will be used by the ownership senior to register
     *         the PersistenceManagerMBean
     */
    protected static String makeMBeanName(CacheService service)
        {
        return CachePersistenceHelper.
            getMBeanName(service.getInfo().getServiceName());
        }

    // ----- inner class: SlowBDBEnvironment --------------------------------

    /**
     * An extension to {@link BerkeleyDBEnvironment} that causes the createSnapshot
     * operation to take at least 5s.
     */
    public static class SlowBDBEnvironment
            extends BerkeleyDBEnvironment
        {
        public SlowBDBEnvironment(File fileActive, File fileSnapshot, File fileTrash)
                throws IOException
            {
            super(fileActive, fileSnapshot, fileTrash);
            }

        @Override
        public synchronized PersistenceManager<ReadBuffer> createSnapshot(String sSnapshot, PersistenceManager<ReadBuffer> manager)
            {
            long cSleep = Long.getLong("test.delay", 5000L);
            CacheFactory.log("SlowBDB sleeping for " + cSleep, CacheFactory.LOG_INFO);

            Base.sleep(cSleep);

            PersistenceManager<ReadBuffer> mgr = super.createSnapshot(sSnapshot, manager);

            CacheFactory.log("Returning from createSnapshot", CacheFactory.LOG_INFO);

            return mgr;
            }
        }

    // ----- constants ------------------------------------------------------

    /**
     * Cache configuration file to use for this test suite.
     */
    protected static final String CACHE_CFG    = "simple-persistence-bdb-cache-config.xml";

    /**
     * The relative location of the active directory used for persistence.
     */
    protected static final String ACTIVE_DIR   = "target/slow-snap-recovery-active";

    /**
     * The relative location of the snapshot directory used for persistence.
     */
    protected static final String SNAPSHOT_DIR = "target/slow-snap-recovery-snapshot";

    /**
     * The relative location of the trash directory used for persistence.
     */
    protected static final String TRASH_DIR    = "target/slow-snap-recovery-trash";
    }
