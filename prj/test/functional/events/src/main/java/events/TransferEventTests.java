/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package events;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.ServiceStatus;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.InvocationService;
import com.tangosol.net.Member;
import com.tangosol.net.NamedCache;

import com.tangosol.net.events.InterceptorRegistry;
import com.tangosol.net.events.partition.TransferEvent;

import com.tangosol.util.ExternalizableHelper;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import events.common.AbstractTestInterceptor.Expectations;
import events.common.AbstractTestInterceptor.ExpectationsInformerInvocable;
import events.common.TestTransferInterceptor;
import events.common.TestTransferInterceptor.TransferExpectation;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import java.util.concurrent.TimeUnit;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static com.oracle.bedrock.deferred.DeferredHelper.within;
import static events.EventTestHelper.ensureRemoteService;
import static events.EventTestHelper.remoteReset;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isIn;

import static org.junit.Assert.*;

/**
 * Basic tests for events.
 */
public class TransferEventTests
        extends AbstractFunctionalTest
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default Constructor.
     */
    public TransferEventTests()
        {
        super(CFG_FILE);
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
        System.setProperty("coherence.management", "all");
        System.setProperty("coherence.distribution.2server", "false");

        AbstractFunctionalTest._startup();
        }

    /**
     * Initialize the test class.
     */
    @BeforeClass
    public static void startup()
        {
        //TODO: When we have configuration this should change to a
        //multi-server storage disabled test
        //        startCacheServer("BasicEventsTests-1", "events");
        }

    @Before
    public void testBefore()
        {
        }

    @After
    public void testAfter()
        {
        }

    /**
     * Shutdown the test class.
     */
    @AfterClass
    public static void shutdown()
        {
        //        stopCacheServer("BasicEventsTests-1", "events");
        }

    @After
    public void cleanup()
        {
        AbstractFunctionalTest._shutdown();
        }


    // ----- test methods ---------------------------------------------------

    /**
     * Test Empty StorageTransferEvents.
     */
    @Test
    public void testEmptyStorageTransferEvents()
        {
        System.out.println("----- testEmptyStorageTransferEvents -----------------");

        boolean           fShutdown   = true;
        String            sIdentifier = TestTransferInterceptor.IDENTIFIER;
        InvocationService serviceInv  = (InvocationService) getFactory().
                ensureService(EventTestHelper.INVOCATION_SERVICE_NAME);
        Matcher           matcher     = isIn(Arrays.asList(128, 129));
        try
            {
            // inform this member of the partition assignment and departure about to occur
            new ExpectationsInformerInvocable(sIdentifier)
                    .addExpectation(TransferEvent.Type.ASSIGNED, new Expectations().
                            expect(new TransferExpectation(TransferEvent.Type.ASSIGNED, is(257))))
                    .addExpectation(TransferEvent.Type.DEPARTING, new Expectations().
                            expect(new TransferExpectation(TransferEvent.Type.DEPARTING, matcher)))
                    .addExpectation(TransferEvent.Type.DEPARTED, new Expectations().
                            expect(new TransferExpectation(TransferEvent.Type.DEPARTED, matcher)))
                    .run();

            NamedCache cache = getNamedCache("transfer-foo");
            cache.clear();

            Cluster                 cluster    = serviceInv.getCluster();
            Member                  memberThis = cluster.getLocalMember();
            DistributedCacheService service    = (DistributedCacheService) cache.getCacheService();
            // start the service on this member to ensure we receive the ASSIGNED events
            ensureRemoteService(SERVICE_NAME, getFactory(), Collections.singleton(memberThis));

            startCacheServer("EmptyStorageTransfer-1", "events", CFG_FILE, PROPS_SEONE);
            // start the service on the remote host
            ensureRemoteService(SERVICE_NAME, getFactory(), null);

            Eventually.assertThat(invoking(cluster).getMemberSet().size(), CoreMatchers.is(2));

            Set setMembers = cluster.getMemberSet();
            // make sure remote server received all the primary transfer
            for (Iterator iterMembers = setMembers.iterator(); iterMembers.hasNext(); )
                {
                Member member = (Member) iterMembers.next();
                if (member != memberThis)
                    {
                    Eventually.assertThat(invoking(new EventTestHelper()).getOwnedPartitionCount(service, member), matcher);
                    break;
                    }
                }

            // assert that this member received the expectations above
            Eventually.assertThat(invoking(new EventTestHelper()).remoteFail(sIdentifier, getFactory(), Collections.singleton(memberThis)), isIn(Arrays.asList(513, 514)));
            // reset invocation count
            remoteReset(sIdentifier, getFactory(), Collections.singleton(memberThis));

            // as we gracefully shutdown the server we expect arrived opposed to restored
            new ExpectationsInformerInvocable(sIdentifier)
                    .addExpectation(TransferEvent.Type.ARRIVED, new Expectations().
                            expect(new TransferExpectation(TransferEvent.Type.ARRIVED, matcher)))
                    .run();

            stopCacheServer("EmptyStorageTransfer-1", true);

            Eventually.assertThat(invoking(new EventTestHelper()).getOwnedPartitionCount(service, memberThis), is(257));

            fShutdown = false;

            Eventually.assertThat(invoking(new EventTestHelper()).remoteFail(sIdentifier, getFactory(), Collections.singleton(memberThis)), isIn(Arrays.asList(128, 129)));
            // reset invocation count
            remoteReset(sIdentifier, getFactory(), Collections.singleton(memberThis));
            }
        finally
            {
            if (fShutdown)
                {
                stopCacheServer("EmptyStorageTransfer-1", true);
                }
            }
        }

    /**
     * Test StorageTransferEvents with graceful shutdown.
     */
    @Test
    public void testTransferEventsNewPrimary() throws InterruptedException
        {
        System.out.println("----- testTransferEventsNewPrimary -----------------");
        testTransferEvents(true);
        }

    /**
     * Test StorageTransferEvents with forced shutdown.
     */
    @Test
    public void testTransferEventsPromotion() throws InterruptedException
        {
        System.out.println("----- testTransferEventsPromotion -----------------");
        testTransferEvents(false);
        }

    private void testTransferEvents(boolean fGracefulShutdown) throws InterruptedException
        {
        String                   sIdentifier = TestTransferInterceptor.IDENTIFIER;
        ConfigurableCacheFactory ccf         = getFactory();
        Cluster                  cluster     = CacheFactory.ensureCluster();
        Member                   memberLocal = cluster.getLocalMember();
        TransferEvent.Type       eventType   = TransferEvent.Type.ARRIVED;
        NamedCache               cache       = null;

        try
            {
            // set the expectation of 257 assigned events
            new ExpectationsInformerInvocable(sIdentifier)
                    .addExpectation(TransferEvent.Type.ASSIGNED, new Expectations().
                            expect(new TransferExpectation(TransferEvent.Type.ASSIGNED, is(257))))
                    .run();

            cache = getNamedCache("transfer-foo");

            DistributedCacheService service = (DistributedCacheService) cache.getCacheService();

            cache.clear();

            Eventually.assertThat(invoking(new EventTestHelper()).getOwnedPartitionCount(service, memberLocal), is(257));

            // assert the assigned events were delivered
            Eventually.assertThat(invoking(new EventTestHelper()).remoteFail(sIdentifier, getFactory(), null), is(257));
            // reset invocation count
            remoteReset(sIdentifier, getFactory(), Collections.singleton(memberLocal));

            Matcher matcher = isIn(Arrays.asList(128, 129));
            // expectations for this member when the remote member joins the service
            Expectations exptsOlderMemberDeparting = new Expectations()
                    .expect(new TransferExpectation(TransferEvent.Type.DEPARTING, matcher));

            Expectations exptsOlderMemberDeparted = new Expectations()
                    .expect(new TransferExpectation(TransferEvent.Type.DEPARTED, matcher));

            // expectations for this member when the remote member leaves the service
            Expectations exptsNewerMember = new Expectations()
                    .expect(new TransferExpectation(eventType, matcher));

            // set the expectations for the entries we insert so that we can
            // assert departing and arriving / restored events have the correct
            // entries
            for (int x = 0; x < 100; ++x)
                {
                cache.put(x, x);

                int iPart = ExternalizableHelper.toBinary(x, service.getSerializer())
                        .calculateNaturalPartition(service.getPartitionCount());

                if (iPart < 129)
                    {
                    exptsOlderMemberDeparting.expect(cache.getCacheName(), TransferEvent.Type.DEPARTING, x, x);
                    exptsNewerMember.expect(cache.getCacheName(), eventType, x, x);
                    exptsOlderMemberDeparted.expect(cache.getCacheName(), TransferEvent.Type.DEPARTED, x, x);
                    }
                }

            // inform this member of the partition departure about to occur
            new ExpectationsInformerInvocable(sIdentifier)
                    .addExpectation(TransferEvent.Type.DEPARTING, exptsOlderMemberDeparting)
                    .addExpectation(TransferEvent.Type.DEPARTED, exptsOlderMemberDeparted)
                    .run();

            CoherenceClusterMember clusterMember = startCacheServer("StorageTransfer-1", "events", CFG_FILE, PROPS_SEONE);

            Eventually.assertThat(invoking(cluster).getMemberSet().size(), CoreMatchers.is(2));

            // start the service on the remote host
            ensureRemoteService(SERVICE_NAME, ccf, null);

            Set setMembers = cluster.getMemberSet();
            // make sure remote server received all the primary transfer
            for (Iterator iterMembers = setMembers.iterator(); iterMembers.hasNext(); )
                {
                Member member = (Member) iterMembers.next();
                if (member != memberLocal)
                    {
                    Eventually.assertThat(invoking(new EventTestHelper()).getOwnedPartitionCount(service, member), matcher);
                    break;
                    }
                }

            // assert that 128 or 129 partitions departed
            Eventually.assertThat(invoking(new EventTestHelper()).remoteFail(sIdentifier, getFactory(), Collections.singleton(memberLocal)), isIn(Arrays.asList(256, 258)), within(2, TimeUnit.MINUTES));

            // reset invocation count
            remoteReset(sIdentifier, getFactory(), Collections.singleton(memberLocal));

            // set the expectation to receive either ARRIVED or RESTORED events
            new ExpectationsInformerInvocable(sIdentifier)
                    .addExpectation(eventType, exptsNewerMember)
                    .run();

            // make sure remote server are node safe before kill
            String sName = cache.getCacheService().getInfo().getServiceName();
            Eventually.assertThat(invoking(this).getServiceStatus(clusterMember,sName),
                    CoreMatchers.is(ServiceStatus.NODE_SAFE.name()), within(5, TimeUnit.MINUTES));

            stopCacheServer("StorageTransfer-1", fGracefulShutdown);

            Eventually.assertThat(invoking(new EventTestHelper()).getOwnedPartitionCount(service, memberLocal), is(257));

            // assert the above expectation occurred
            Eventually.assertThat(invoking(new EventTestHelper()).remoteFail(sIdentifier, getFactory(), Collections.singleton(memberLocal)), matcher);

            // reset invocation count
            remoteReset(sIdentifier, getFactory(), Collections.singleton(memberLocal));

            assertEquals(100, cache.size());
            assertTrue(cache.getCacheService().isRunning());
            }
        finally
            {
            if (cache != null)
                {
                cache.clear();
                CacheFactory.shutdown();
                }
            }
        }

    protected int getPartitionCount(DistributedCacheService service, Member member)
        {
        return service.getOwnedPartitions(member).cardinality();
        }

    public String getServiceStatus(CoherenceClusterMember member, String sService)
        {
        return member.getServiceStatus(sService).name();
        }

    /**
     * Get the interceptor registry for the default cache factory in use.
     *
     * @return interceptor registry
     */
    protected InterceptorRegistry getInterceptorRegistry()
        {
        return getFactory().getInterceptorRegistry();
        }

    // ----- constants ------------------------------------------------------

    /**
     * The Cache config file to use for these tests.
     */
    public static final String CFG_FILE = "basic-server-cache-config.xml";

    /**
     * The name of the service used by this test class.
     */
    protected static final String SERVICE_NAME = "TransferDistributedService";
    }
