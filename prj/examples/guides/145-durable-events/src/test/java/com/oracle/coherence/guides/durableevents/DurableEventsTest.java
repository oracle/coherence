/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.guides.durableevents;

import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterBuilder;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.Multicast;
import com.oracle.bedrock.runtime.coherence.options.RoleName;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.bedrock.testsupport.junit.TestLogsExtension;
import com.oracle.coherence.common.base.Logger;

import com.tangosol.io.FileHelper;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.Coherence;
import com.tangosol.net.NamedMap;

import com.tangosol.util.MapListener;
import com.tangosol.util.Processors;
import com.tangosol.util.listener.SimpleMapListener;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Durable Events examples.
 *
 * @author Tim Middleton 2021.06.02
 */
public class DurableEventsTest {
    /**
     * The Coherence cluster name to be used by the storage members and test
     */
    public static final String CLUSTER_NAME = "Events";

    /**
     * Directory to use for persistence and events.
     */
    private static File persistenceDir;

    /**
     * The Coherence storage member cluster.
     */
    private static CoherenceCluster cluster;

    /**
     * OS file separator.
     */
    private static final String FILE_SEP = System.getProperty("file.separator");

    @RegisterExtension
    static final TestLogsExtension testLogs = new TestLogsExtension(DurableEventsTest.class);

    // tag::startup[]
    /**
     * Start a Coherence cluster with two cache servers using Oracle Bedrock.
     *
     * @throws IOException if any errors creating temporary directory
     */
    @BeforeAll
    public static void startup() throws IOException {
        persistenceDir = FileHelper.createTempDir();
        File eventsDir = new File(persistenceDir, "events");

        CoherenceClusterBuilder builder = new CoherenceClusterBuilder()
            .with(SystemProperty.of("coherence.distributed.partitions", 23),  // <1>
                  SystemProperty.of("coherence.distributed.persistence.mode", "active"), // <2>
                  SystemProperty.of("coherence.distributed.persistence.base.dir", persistenceDir.getAbsolutePath()), // <3>
                  SystemProperty.of("coherence.distributed.persistence.events.dir", eventsDir.getAbsolutePath()), // <4>
                  ClusterName.of(CLUSTER_NAME),
                  RoleName.of("storage"),
                  DisplayName.of("storage"),
                  Multicast.ttl(0))
            .include(2, CoherenceClusterMember.class, testLogs,
                     LocalStorage.enabled());

        cluster = builder.build();

        Eventually.assertDeferred(() -> cluster.getClusterSize(), is(2));
        for (CoherenceClusterMember member : cluster)
            {
            Eventually.assertDeferred(member::isReady, is(true));
            }
    }
    // end::startup[]

    // tag::testDurableEvents[]
    /**
     * Runs a test to simulate a client registering a versioned {@link MapListener},
     * being disconnected, reconnecting, and then receiving all the events that were
     * missed while the client was disconnected.
     */
    @Test
    public void testDurableEvents() throws Exception {
        AtomicInteger eventCount = new AtomicInteger();
        String cacheName = "customers";

        System.setProperty("coherence.cluster", CLUSTER_NAME);
        System.setProperty("coherence.role", "client");
        System.setProperty("coherence.distributed.localstorage", "false");

        try (Coherence coherence = Coherence.clusterMember()) {
            coherence.start().get(5, TimeUnit.MINUTES);

            NamedMap<Long, Customer> customers = coherence.getSession().getMap(cacheName);

            MapListener<Long, Customer> mapListener = new SimpleMapListener<Long, Customer>() // <2>
                                           .addEventHandler(System.out::println) // <3>
                                           .addEventHandler((e) -> eventCount.incrementAndGet()) // <4>
                                           .versioned(); // <5>
            customers.addMapListener(mapListener); // <6>

            Logger.info("Added Map Listener, generating 3 events");
            // generate 3 events, insert, update and delete
            Customer customer = new Customer(100L, "Customer 100", "Address", Customer.GOLD, 5000);
            customers.put(customer.getId(), customer);
            customers.invoke(100L, Processors.update(Customer::setAddress, "New Address"));
            customers.remove(100L);

            // wait until we receive first three events
            Eventually.assertDeferred(eventCount::get, is(3));

            // cause a service distribution for PartitionedCache service to simulate disc
            Logger.info("Disconnecting client");
            causeServiceDisruption(customers); // <7>

            Logger.info("Remotely insert, update and delete a new customer");
            // do a remote invocation to insert, update and delete a customer. This is done
            // remotely via Oracle Bedrock as not to reconnect the client
            cluster.getAny().invoke(() -> { // <8>
                NamedMap<Long, Customer> customerMap = CacheFactory.getCache(cacheName);
                Customer newCustomer = new Customer(100L, "Customer 101", "Customer address", Customer.SILVER, 100);
                customerMap.put(newCustomer.getId(), newCustomer);
                customerMap.invoke(100L, Processors.update(Customer::setAddress, "New Address"));
                customerMap.remove(100L);
                return null;
            });

            // Events should still only be 3 as client has not yet reconnected
            Eventually.assertDeferred(eventCount::get, is(3));

            Logger.info("Issuing size to reconnect client");
            // issue an operation that will cause a service restart and listener to be re-registered
            customers.size(); // <9>

            // we should now see the 3 events we missed because we were disconnected
            Eventually.assertDeferred(eventCount::get, is(6)); // <10>
        }
    }
    // end::testDurableEvents[]

    @AfterAll
    public static void shutdown() {
        if (cluster != null) {
            cluster.close();
        }
        FileHelper.deleteDirSilent(persistenceDir);
    }

     /**
     * Stop the inner service.
     *
     * @param cache  the cache hosted by the service to stop
     */
    protected void causeServiceDisruption(NamedMap<?, ?> cache)
        {
        CacheService serviceSafe = cache.getService();
        try
            {
            Method       methRunningService = serviceSafe.getClass().getMethod("getRunningService");
            CacheService serviceInternal    = (CacheService) methRunningService.invoke(serviceSafe);

            serviceInternal.stop();
            }
        catch (NoSuchMethodException e)
            {
            fail("Unexpected service: " + serviceSafe);
            }
        catch (IllegalAccessException | InvocationTargetException e)
            {
            fail("Failed to call getRunningService on: " + serviceSafe);
            }
        }

}
