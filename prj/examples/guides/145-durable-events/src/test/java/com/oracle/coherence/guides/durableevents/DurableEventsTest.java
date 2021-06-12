/*
 * Copyright (c) 2000, 2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.guides.durableevents;

import com.oracle.bedrock.OptionsByType;
import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.coherence.CoherenceCacheServer;
import com.oracle.bedrock.runtime.coherence.options.LocalStorage;
import com.oracle.bedrock.runtime.coherence.options.Logging;
import com.oracle.bedrock.runtime.coherence.options.Multicast;
import com.oracle.bedrock.runtime.coherence.options.RoleName;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.io.FileHelper;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.CacheService;
import com.tangosol.net.Coherence;
import com.tangosol.net.NamedMap;

import com.tangosol.util.MapListener;
import com.tangosol.util.Processors;
import com.tangosol.util.listener.SimpleMapListener;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Durable Events examples.
 *
 * @author Tim Middleton 2021.06.02
 */
public class DurableEventsTest {

    /**
     * Directory to use for persistence and events.
     */
    private static File persistenceDir;

    /**
     * Cache servers.
     */
    private static CoherenceCacheServer member1;
    private static CoherenceCacheServer member2;

    /**
     * Properties for server startup.
     */
    private static Properties props;

    /**
     * OS file separator.
     */
    private static final String FILE_SEP = System.getProperty("file.separator");

    // tag::startup[]
    /**
     * Startup 2 cache servers using Oracle Bedrock.
     *
     * @throws IOException if any errors creating temporary directory
     */
    @BeforeAll
    public static void startup() throws IOException {
        persistenceDir = FileHelper.createTempDir();
        String path = persistenceDir.getAbsolutePath();
        LocalPlatform platform = LocalPlatform.get();

        props = new Properties();
        props.put("coherence.distributed.partitions", "23");  // <1>
        props.put("coherence.distributed.persistence.mode", "active"); // <2>
        props.put("coherence.distributed.persistence.base.dir", path); // <3>
        props.put("coherence.distributed.persistence.events.dir", path + FILE_SEP + "events"); // <4>

        OptionsByType optionsByType = OptionsByType.empty();
        optionsByType.addAll(LocalStorage.enabled(), Multicast.ttl(0), Logging.at(2));

        // add the properties to the Bedrock startup
        props.forEach((k,v) -> optionsByType.add(SystemProperty.of((String) k, (String) v)));

        OptionsByType optionsByTypeMember1 = OptionsByType.of(optionsByType).add(RoleName.of("member1"));
        OptionsByType optionsByTypeMember2 = OptionsByType.of(optionsByType).add(RoleName.of("member2"));

        member1 = platform.launch(CoherenceCacheServer.class, optionsByTypeMember1.asArray());
        member2 = platform.launch(CoherenceCacheServer.class, optionsByTypeMember2.asArray());

        Eventually.assertThat(invoking(member1).getClusterSize(), CoreMatchers.is(2));
    }
    // end::startup[]

    // tag::testDurableEvents[]
    /**
     * Runs a test to simulate a client registering a versioned {@link MapListener},
     * being disconnected, reconnecting, and then receiving all the events that were
     * missed while the client was disconnected.
     */
    @Test
    public void testDurableEvents()  {
        try {
            final AtomicInteger eventCount = new AtomicInteger();
            final String CACHE_NAME = "customers";

            System.getProperties().putAll(props); // <1>
            System.setProperty("coherence.distributed.localstorage", "false");
            System.setProperty("coherence.log.level", "3");

            Coherence coherence = Coherence.clusterMember();
            coherence.start().join();

            NamedMap<Long, Customer> customers = coherence.getSession().getMap(CACHE_NAME);

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
            member2.invoke(() -> { // <8>
                NamedMap<Long, Customer> customerMap = CacheFactory.getCache(CACHE_NAME);
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
        finally {
            Coherence coherence = Coherence.getInstance();
            coherence.close();
        }
    }
    // end::testDurableEvents[]

    @AfterAll
    public static void shutdown() {
        if (member1 != null) {
            member1.close();
        }
        if (member2 != null) {
            member2.close();
        }
        FileHelper.deleteDirSilent(persistenceDir);
    }

     /**
     * Stop the inner service.
     *
     * @param cache  the cache hosted by the service to stop
     */
    protected void causeServiceDisruption(NamedMap cache)
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
