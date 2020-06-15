/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package topics;

import com.oracle.coherence.persistence.PersistenceManager;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;

import com.tangosol.coherence.component.util.SafeService;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache;
import com.tangosol.coherence.dslquery.internal.PersistenceToolsHelper;

import com.tangosol.io.FileHelper;
import com.tangosol.io.ReadBuffer;

import com.tangosol.net.Action;
import com.tangosol.net.ActionPolicy;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.Cluster;
import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.PartitionedService;
import com.tangosol.net.PartitionedService.PartitionRecoveryAction;
import com.tangosol.net.Service;
import com.tangosol.net.ValueTypeAssertion;
import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Publisher;
import com.tangosol.net.topic.Subscriber;
import com.tangosol.net.topic.Subscriber.CompleteOnEmpty;

import common.AbstractFunctionalTest;
import common.AbstractRollingRestartTest;

import common.SlowTests;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import javax.management.MBeanException;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import java.lang.reflect.InvocationTargetException;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static com.oracle.bedrock.deferred.DeferredHelper.within;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Functional tests for simple topic persistence and recovery.
 *
 * @author jh  2012.07.12
 * @author jf  2016.02.25
 */
public abstract class AbstractSimplePersistenceTopicTests
        extends AbstractFunctionalTest
    {

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

        // use a coherence override file that has persistence defs and
        // extends coherence test overrides required by extending AbstractFunctionalTest
        System.setProperty("coherence.override", "common-tangosol-coherence-override.xml");

        AbstractFunctionalTest._startup();
        }

    // ----- tests ----------------------------------------------------------

    /**
     * Tests persistence and recovery of a single cache server.
     */
    @Test
    public void testSingleServer()
            throws IOException, ExecutionException, InterruptedException
        {
        testBasicPersistence("testSingleServer" + getPersistenceManagerName(),
                "simple-persistent-topic-1", "simple-transient-topic-1");
        }

    /**
     * Tests the create and recover snapshot functionality.
     */
    @Test
    @Category(SlowTests.class)
    public void testPassiveSnapshot()
            throws IOException, MBeanException, ExecutionException, InterruptedException {
        testBasicSnapshot("testPassiveSnapshot"+ getPersistenceManagerName(), "simple-persistent-topic-1", false);
        }

    /**
     * Tests the create and recover snapshot functionality.
     */
    @Test
    @Category(SlowTests.class)
    public void testActiveSnapshot()
            throws IOException, MBeanException, ExecutionException, InterruptedException {
        testBasicSnapshot("testActiveSnapshot" + getPersistenceManagerName(), "simple-persistent-topic-1", true);
        }

    /**
     * Test archivers while in active mode for up to 4 servers.
     */
    @Test
    @Category(SlowTests.class)
    public void testActiveArchiver()
            throws IOException, NoSuchMethodException, IllegalAccessException,
                   InvocationTargetException, MBeanException, ExecutionException, InterruptedException
        {
        testBasicArchiver("testActiveArchiver" + getPersistenceManagerName(), "simple-archiver", true, 2);
    }

     /**
      * Test archivers while in passive (on-demand) mode for up to 3 servers.
      */
    @Test
    @Category(SlowTests.class)
    public void testPassiveArchiver()
            throws IOException, NoSuchMethodException, IllegalAccessException,
                   InvocationTargetException, MBeanException, ExecutionException, InterruptedException
        {
        testBasicArchiver("testPassiveArchiver" + getPersistenceManagerName(), "simple-archiver", false, 2);
    }


    // ----- helpers --------------------------------------------------------

    private void testBasicPersistence(String sServer, String sPersistentTopic, String sTransientTopic)
            throws IOException, ExecutionException, InterruptedException {
        File fileActive   = FileHelper.createTempDir();
        File fileSnapshot = FileHelper.createTempDir();
        File fileTrash    = FileHelper.createTempDir();

        Properties props  = new Properties();
        props.setProperty("test.persistence.active.dir", fileActive.getAbsolutePath());
        props.setProperty("test.persistence.snapshot.dir", fileSnapshot.getAbsolutePath());
        props.setProperty("test.persistence.trash.dir", fileTrash.getAbsolutePath());
        props.setProperty("coherence.distribution.2server", "false");

        Cluster cluster = CacheFactory.getCluster();

        CoherenceClusterMember clusterMember = startCacheServer(sServer + "-1", getProjectName(), getCacheConfigPath(), props);
        Eventually.assertThat(invoking(cluster).getMemberSet().size(), is(2));

        NamedTopic<String> topicTransient  = getFactory().ensureTopic(sTransientTopic, ValueTypeAssertion.withType(String.class));
        NamedTopic<String> topicPersistent = getFactory().ensureTopic(sPersistentTopic, ValueTypeAssertion.withType(String.class));
        PartitionedService service         = (PartitionedService) topicPersistent.getService();

        try (Subscriber<String> subscriberPersistent = topicPersistent.createSubscriber(Subscriber.CompleteOnEmpty.enabled());
             Subscriber<String> subscriberTransient = topicTransient.createSubscriber(Subscriber.CompleteOnEmpty.enabled()))
            {
            final String VALUE = "value";

            Eventually.assertThat(invoking(clusterMember).isServiceRunning(service.getInfo().getServiceName()), is(true));
            AbstractRollingRestartTest.waitForNoOrphans((CacheService)service);


            try (Publisher publisher = topicPersistent.createPublisher())
                {
                publisher.send(VALUE).get();
                }

            try (Publisher publisher = topicTransient.createPublisher())
                {
                publisher.send(VALUE).get();
                }

            stopCacheServer(sServer + "-1", true);
            Eventually.assertThat(invoking(cluster).getMemberSet().size(), is(1));

            // restart the server and assert that all (and only) persisted
            // data was recovered
            clusterMember = startCacheServer(sServer + "-2", getProjectName(), getCacheConfigPath(), props);
            Eventually.assertThat(invoking(cluster).getMemberSet().size(), is(2));
            Eventually.assertThat(invoking(clusterMember).isServiceRunning(service.getInfo().getServiceName()), is(true));
            AbstractRollingRestartTest.waitForNoOrphans((CacheService) topicPersistent.getService());

            assertEquals("assert that value published to persistent topic survived cluster restart.",
                         VALUE, subscriberPersistent.receive().get().getValue());

            try
                {
                subscriberTransient.receive().get();
                fail("assert that there are no values on transient topic after restart of cluster");
                }
            catch (Exception e)
                {
                // expected, the transient subscriber was effectively destroyed
                }
            }
        finally
            {
            Thread.sleep(1000L);
            getFactory().destroyTopic(topicTransient);
            getFactory().destroyTopic(topicPersistent);

            stopCacheServer(sServer + "-2");
            stopAllApplications();
            CacheFactory.shutdown();
            FileHelper.deleteDirSilent(fileActive);
            FileHelper.deleteDirSilent(fileSnapshot);
            FileHelper.deleteDirSilent(fileTrash);
            }
        }

    /**
     * Test for basic snapshot create, recover and remove functionality.
     *
     * @param sServer           the prefix name of the servers to create
     * @param sPersistentTopic  the name of the topic
     * @param fActive           true iff the servers should be in active persistence mode
     */
    private void testBasicSnapshot(String sServer, String sPersistentTopic, boolean fActive)
            throws IOException, MBeanException, ExecutionException, InterruptedException {
        File fileSnapshot = FileHelper.createTempDir();
        File fileActive   = fActive ? FileHelper.createTempDir() : null;
        File fileTrash    = FileHelper.createTempDir();

        Properties props = new Properties();
        props.setProperty("test.persistence.mode", fActive ? "active" : "on-demand");
        props.setProperty("test.persistence.active.dir", fActive ? fileActive.getAbsolutePath() : "");
        props.setProperty("test.persistence.trash.dir", fileTrash.getAbsolutePath());
        props.setProperty("test.persistence.snapshot.dir", fileSnapshot.getAbsolutePath());
        props.setProperty("test.persistence.members", "2");
        props.setProperty("coherence.management", "all");
        props.setProperty("coherence.management.remote", "true");
        props.setProperty("coherence.distribution.2server", "false");

        final NamedTopic        topic     = getFactory().ensureTopic(sPersistentTopic, ValueTypeAssertion.withoutTypeChecking());
        DistributedCacheService service  = (DistributedCacheService) topic.getService();
        Cluster                 cluster  = service.getCluster();
        String                  sService = service.getInfo().getServiceName();

        startCacheServer(sServer + "-1", getProjectName(), getCacheConfigPath(), props);
        startCacheServer(sServer + "-2", getProjectName(), getCacheConfigPath(), props);

        Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(2));
        waitForBalanced(service);

        PersistenceTestHelper helper = new PersistenceTestHelper();

        try (Subscriber subscriberPin = topic.createSubscriber(Subscriber.Name.of("queue")))
            {

            try (Publisher publisher = topic.createPublisher())
                {
                // add a bunch of data
                publisher.send("bar");
                publisher.send("baz");

                for (int i = 0;
                     i < 50;
                     i++)
                    {
                    publisher.send(new Integer(i).toString());
                    }

                // needed before taking a snapshot.
                publisher.flush().join();

                // create snapshot with global consistency
                cluster.suspendService(sService);
                helper.createSnapshot(sService, "snapshot-A");
                cluster.resumeService(sService);

                PersistenceTestHelper.logTopicMBeanStats(topic);

                for (int i = 50;
                     i < 100;
                     i++)
                    {
                    publisher.send(new Integer(i).toString());
                    }

                // needed before creating a snapshot.
                publisher.flush().join();
                }

            // create snapshot with global consistency
            cluster.suspendService(sService);
            helper.createSnapshot(sService, "snapshot-B");
            cluster.resumeService(sService);
            PersistenceTestHelper.logTopicMBeanStats(topic);

            // intentionally lose all data
            stopCacheServer(sServer + "-1", true);
            stopCacheServer(sServer + "-2", true);
            Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(0));

            startCacheServer(sServer + "-3", getProjectName(), getCacheConfigPath(), props);
            startCacheServer(sServer + "-4", getProjectName(), getCacheConfigPath(), props);
            startCacheServer(sServer + "-5", getProjectName(), getCacheConfigPath(), props);

            Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(3));
            waitForBalanced(service);

            if (fActive)
                {
                // if in active persistence mode, we should not have lost anything,
                // and automatic recovery should have restored the logical state
                // of "snapshot-B"

                // validate that the data were recovered
               validateSubscriberReceiveData(topic, "queue", 100);
                }
            else
                {
                // nothing should have survived
                try (Subscriber subscriber = topic.createSubscriber(Subscriber.CompleteOnEmpty.enabled(), Subscriber.Name.of("queue")))
                    {
                    assertNull(subscriber.receive().get());
                    }
                }

            // if we do not call suspend service, then the service will be
            // automatically suspended and then resumed
            helper.recoverSnapshot(sService, "snapshot-A");
            waitForBalanced(service);

            // validate that the data were recovered
            validateSubscriberReceiveData(topic, "queue", 50);

            helper.recoverSnapshot(sService, "snapshot-B");
            waitForBalanced(service);

            validateSubscriberReceiveData(topic, "queue", 100);

            // test partial failure
            stopCacheServer(sServer + "-3", true);
            Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(2));
            waitForBalanced(service);

            AbstractRollingRestartTest.waitForNoOrphans((CacheService) service);
            helper.recoverSnapshot(sService, "snapshot-B");

            waitForBalanced(service);

            validateSubscriberReceiveData(topic, "queue", 100);

            // validate that "snapshot-B" is gone when we issue a removeSnapshot
            Eventually.assertThat(invoking(this).listSnapshots(helper, sService).size(), is(2));

            helper.removeSnapshot(sService, "snapshot-B");

            Eventually.assertThat(invoking(this).listSnapshots(helper, sService).size(), is(1));
            assertThat(listSnapshots(helper, sService).get(0), is("snapshot-A"));

            // validate that "snapshot-A" is gone when we issue a removeSnapshot
            helper.removeSnapshot(sService, "snapshot-A");

            Eventually.assertThat(invoking(this).listSnapshots(helper, sService).size(), is(0));
            }
        finally
            {
            stopAllApplications();
            CacheFactory.shutdown();

            if (fActive)
                {
                FileHelper.deleteDirSilent(fileActive);
                }
            FileHelper.deleteDirSilent(fileSnapshot);
            FileHelper.deleteDirSilent(fileTrash);
            }
        }

    private void validateSubscriberReceiveData(NamedTopic topic, String sGroup, int nValues) throws ExecutionException, InterruptedException
        {
        Eventually.assertDeferred("Topic " + topic.getName() + " must have existing subscriber group " + sGroup + " since expecting " + nValues + " messages",
                () -> topic.getSubscriberGroups().contains(sGroup), is(true));

        PersistenceTestHelper.logTopicMBeanStats(topic);

        try (Subscriber subscriber = topic.createSubscriber(Subscriber.CompleteOnEmpty.enabled(), Subscriber.Name.of(sGroup)))
            {
            // until receive first message from recovered topic, allow for null that occurs when CompleteOnEmpty is enabled for a subscriber.
            int MAX_RETRY = 10;

            Subscriber.Element<String> element = (Subscriber.Element<String>) subscriber.receive().get();
            for (int i = 0; element == null && i < MAX_RETRY; i++)
                {
                element = (Subscriber.Element<String>) subscriber.receive().get();
                Thread.sleep(500L);
                }
            assertNotNull("validate first message in subscriber group " + sGroup + " is non-null", element);
            assertEquals("validate first message in subscriber group is bar",
                "bar", element.getValue());
            assertEquals("baz", ((Subscriber.Element<String>) subscriber.receive().get()).getValue());

            String previousValue = "<empty>";

            for (int i = 0; i < nValues; i++)
                {
                Subscriber.Element<String> e = (Subscriber.Element<String>) subscriber.receive().get();
                if (e == null)
                    {
                    System.out.println("WARNING: iteration " + i + " did not get a value from topic subscriber. " +
                            "Previous subscriber receive=" + previousValue);
                    previousValue = "<empty>";
                    }
                else
                    {
                    assertEquals(i, Integer.parseInt(e.getValue()));
                    previousValue = e.getValue();
                    }
                }
            }
        }

    /**
     * Test basic archiver functionality.
     *
     * @param sServer           the prefix name of the servers to create
     * @param sPersistentTopic  the name of the topic
     * @param fActive           true iff the servers should be in active persistence mode
     */
    private void testBasicArchiver(String sServer, String sPersistentTopic, boolean fActive, int nMaxServers)
            throws IOException, NoSuchMethodException, IllegalAccessException,
                   InvocationTargetException, MBeanException, ExecutionException, InterruptedException
        {
        File fileSnapshot = FileHelper.createTempDir();
        File fileActive   = fActive ? FileHelper.createTempDir() : null;
        File fileTrash    = FileHelper.createTempDir();
        File fileArchive  = FileHelper.createTempDir();

        Properties props = new Properties();
        props.setProperty("test.persistence.mode", fActive ? "active" : "on-demand");
        props.setProperty("test.persistence.active.dir", fActive ? fileActive.getAbsolutePath() : "");
        props.setProperty("test.persistence.trash.dir", fileTrash.getAbsolutePath());
        props.setProperty("test.persistence.snapshot.dir", fileSnapshot.getAbsolutePath());
        props.setProperty("test.persistence.archive.dir", fileArchive.getAbsolutePath());
        props.setProperty("test.persistence.members", "2");
        props.setProperty("test.start.archiver", "true");
        props.setProperty("coherence.management", "all");
        props.setProperty("coherence.management.refresh.expiry", "1s");
        props.setProperty("coherence.management.remote", "true");
        props.setProperty("coherence.distribution.2server", "false");

        final NamedTopic        topic    = getFactory().ensureTopic(sPersistentTopic, ValueTypeAssertion.withType(Integer.class));
        DistributedCacheService service  = (DistributedCacheService) topic.getService();
        Cluster                 cluster  = service.getCluster();
        String                  sService = service.getInfo().getServiceName();

        startCacheServer(sServer + "-1", getProjectName(), getCacheConfigPath(), props);
        Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(1));

        final String sEmptySnapshot = "empty-cluster";
        final String sSnapshot53 = "snapshot-53";
        try
            {
            PersistenceToolsHelper helper = new PersistenceToolsHelper();
            helper.setPrintWriter(new PrintWriter(System.out));

            for (int i = 2; i <= nMaxServers; i++)
                {
                System.out.println("Iteration: " + i);
                // each iteration start another cache server to test out
                // slightly different scenarios with the archival
                // iteration 1:  server-1 & server-2
                // iteration 2:  server-1, server-2 & server-3
                // iteration 3:  server-1, server-2, server-3 & server-4

                startCacheServer(sServer + "-" + i, getProjectName(), getCacheConfigPath(), props);
                Eventually.assertThat(invoking(service).getOwnershipEnabledMembers().size(), is(i));
                waitForBalanced(service);

                topic.createSubscriber(Subscriber.Name.of("queue")).close(); // create a place holder

                // create an empty-cluster snapshot
                // create snapshot with global consistency
                cluster.suspendService(sService);
                helper.invokeOperationWithWait(PersistenceToolsHelper.CREATE_SNAPSHOT, sEmptySnapshot, sService);
                cluster.resumeService(sService);

                // create a second snapshot with 53 entries
                PersistenceTestHelper.populateData(topic, 53);

                cluster.suspendService(sService);
                helper.invokeOperationWithWait(PersistenceToolsHelper.CREATE_SNAPSHOT, sSnapshot53, sService);
                cluster.resumeService(sService);

                // archive the snapshots
                helper.invokeOperationWithWait(PersistenceToolsHelper.ARCHIVE_SNAPSHOT, sEmptySnapshot, sService);
                Eventually.assertThat(invoking(this).getArchivedSnapshotCount(helper, sService), is(1));

                String[] asArchivedSnapshots = helper.listArchivedSnapshots(sService);
                assertTrue(asArchivedSnapshots != null & asArchivedSnapshots.length == 1);
                assertEquals(asArchivedSnapshots[0], sEmptySnapshot);

                helper.invokeOperationWithWait(PersistenceToolsHelper.ARCHIVE_SNAPSHOT, sSnapshot53, sService);
                Eventually.assertThat(invoking(this).getArchivedSnapshotCount(helper, sService), is(2));

                asArchivedSnapshots = helper.listArchivedSnapshots(sService);
                assertTrue(asArchivedSnapshots != null && asArchivedSnapshots.length == 2 &&
                        (sSnapshot53.equals(asArchivedSnapshots[0]) ||
                         sSnapshot53.equals(asArchivedSnapshots[1])));

                // remove the local snapshots
                helper.invokeOperationWithWait(PersistenceToolsHelper.REMOVE_SNAPSHOT, sEmptySnapshot, sService);
                helper.invokeOperationWithWait(PersistenceToolsHelper.REMOVE_SNAPSHOT, sSnapshot53, sService);

                Eventually.assertThat(invoking(this).listSnapshots(helper, sService).size(), is(0));

                // retrieve and recover the empty cluster snapshot
                helper.invokeOperationWithWait(PersistenceToolsHelper.RETRIEVE_ARCHIVED_SNAPSHOT, sEmptySnapshot, sService);

                Eventually.assertThat(listSnapshots(helper, sService), is(containsInAnyOrder(sEmptySnapshot)));

                // if we do not call suspend service, then the service will be
                // automatically suspended and then resumed
                helper.invokeOperationWithWait(PersistenceToolsHelper.RECOVER_SNAPSHOT, sEmptySnapshot, sService);
                try (Subscriber<Integer> subscriber = topic.createSubscriber(CompleteOnEmpty.enabled(), Subscriber.Name.of("queue")))
                    {
                    assertNull(subscriber.receive().get());
                    }

                // retrieve and recover the 53 object snapshot
                helper.invokeOperationWithWait(PersistenceToolsHelper.RETRIEVE_ARCHIVED_SNAPSHOT, sSnapshot53, sService);
                Eventually.assertThat(listSnapshots(helper, sService),
                                      is(containsInAnyOrder(sEmptySnapshot, sSnapshot53)));

                cluster.suspendService(sService);
                helper.invokeOperationWithWait(PersistenceToolsHelper.RECOVER_SNAPSHOT, sSnapshot53, sService);
                cluster.resumeService(sService);
                PersistenceTestHelper.validateData(topic, "queue", 53);

                // purge the archived snapshots
                helper.invokeOperationWithWait(PersistenceToolsHelper.REMOVE_ARCHIVED_SNAPSHOT, sEmptySnapshot, sService);
                helper.invokeOperationWithWait(PersistenceToolsHelper.REMOVE_ARCHIVED_SNAPSHOT, sSnapshot53, sService);

                // Cleanup
                helper.invokeOperationWithWait(PersistenceToolsHelper.REMOVE_SNAPSHOT, sEmptySnapshot, sService);
                helper.invokeOperationWithWait(PersistenceToolsHelper.REMOVE_SNAPSHOT, sSnapshot53, sService);
                Eventually.assertThat(invoking(this).listSnapshots(helper, sService).size(), is(0));
                }
            }
        finally
            {
            getFactory().destroyTopic(topic);
            stopAllApplications();
            CacheFactory.shutdown();

            if (fActive)
                {
                FileHelper.deleteDirSilent(fileActive);
                }
            FileHelper.deleteDirSilent(fileSnapshot);
            FileHelper.deleteDirSilent(fileTrash);
            FileHelper.deleteDirSilent(fileArchive);
            }
        }

    /**
     * A helper method to call the static {@link PersistenceTestHelper#listSnapshots(String)}
     * method. This allows us to use this method in Eventually.assertThat(Deferred, Matcher)
     * tests.
     */
    public List<String> listSnapshots(PersistenceTestHelper helper, String sService)
        {
        String[] asSnapshots = helper.listSnapshots(sService);
        return Arrays.asList(asSnapshots);
        }

    /**
     * A helper method to call the static {@link PersistenceToolsHelper#listSnapshots(String)}
     * method. This allows us to use this method in Eventually.assertThat(Deferred, Matcher)
     * tests.
     */
    public List<String> listSnapshots(PersistenceToolsHelper helper, String sService)
        {
        String[] asSnapshots = helper.listSnapshots(sService);
        return Arrays.asList(asSnapshots);
        }

    /**
     * Helper method to call {@link topics.PersistenceTestHelper#getStatusHA(String)}
     * method. This allows us to use the method in Eventually.assertThat(Deferred, Matcher)
     * tests.
     *
     * @param sService  service name to get statusHA for
     *
     * @return the statusHA value for the service
     */
    public String getStatusHA(String sService)
        {
        return PersistenceTestHelper.getStatusHA(sService);
        }

    /**
     * Return the count of archived snapshots
     *
     * @param helper        helper to query
     * @param sServiceName  service name to query
     *
     * @return the number of snapshots
     */
    public int getArchivedSnapshotCount(PersistenceToolsHelper helper, String sServiceName)
        {
        String[] asSnapshots = helper.listArchivedSnapshots(sServiceName);
        return asSnapshots == null ||  asSnapshots.length == 0 ? 0: asSnapshots.length;
        }

   /**
    * Wait for an extended period compared with {@link AbstractFunctionalTest#waitForBalanced(CacheService)}
    * for the specified (partitioned) cache service to become "balanced".
    *
    * @param service   the partitioned cache to wait for balancing
    */
    public static void waitForBalanced(CacheService service)
        {
        SafeService      serviceSafe = (SafeService) service;
        PartitionedCache serviceReal = (PartitionedCache) serviceSafe.getService();

        Eventually.assertThat(invoking(serviceReal).calculateUnbalanced(), is(0), within(120, TimeUnit.SECONDS));
        }

    // ----- inner class: QuorumPolicy --------------------------------------

    /**
     * Simple quorum policy that disallows partition recovery action until
     * a configurable number of storage-enabled members have joined the
     * service.
     */
    public static class QuorumPolicy
            implements ActionPolicy
        {
        /**
         * {@inheritDoc}
         */
        public void init(Service service)
            {
            }

        /**
         * {@inheritDoc}
         */
        public boolean isAllowed(Service service, Action action)
            {
            if (action instanceof PartitionRecoveryAction)
                {
                int nMembers    = ((PartitionedService) service).getOwnershipEnabledMembers().size();
                int nCheckValue = Integer.getInteger("test.persistence.members", 1);
                CacheFactory.log("Checking Quorum Policy: Service = " + service.getInfo().getServiceName() + ", Action="
                        + action + ", CheckValue=" + nCheckValue + ", Members=" + nMembers, CacheFactory.LOG_INFO);
                return (nMembers >= nCheckValue);
                }
            return true;
            }
        }

    // ----- factory methods ------------------------------------------------

    /**
     * Create a PersistenceManager to validate results of tests.
     *
     * @param file  the persistence root
     *
     * @return a new PersistenceManager for the given root directory
     */
    protected abstract PersistenceManager<ReadBuffer> createPersistenceManager(File file)
            throws IOException;

    // ----- accessors ------------------------------------------------------

    /**
     * Return a name for the PersistenceManager being used by the tests.
     *
     * @return a name used in log files, etc.
     */
    public abstract String getPersistenceManagerName();

    /**
     * {@inheritDoc}
     */
    public abstract String getCacheConfigPath();

    /**
     * Return the project name.
     */
    public static String getProjectName()
        {
        return "topics";
        }
    }
