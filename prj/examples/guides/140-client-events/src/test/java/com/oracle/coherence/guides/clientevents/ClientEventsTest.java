/*
 * Copyright (c) 2000, 2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.guides.clientevents;

import com.oracle.bedrock.testsupport.deferred.Eventually;

import com.oracle.coherence.common.base.Logger;

import com.tangosol.net.Coherence;
import com.tangosol.net.NamedMap;

import com.tangosol.util.Filter;
import com.tangosol.util.Filters;
import com.tangosol.util.MapEvent;
import com.tangosol.util.MapListener;
import com.tangosol.util.MultiplexingMapListener;
import com.tangosol.util.Processors;

import com.tangosol.util.filter.MapEventFilter;
import com.tangosol.util.listener.SimpleMapListener;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.oracle.bedrock.deferred.DeferredHelper.invoking;
import static org.hamcrest.CoreMatchers.is;

/**
 * Tests to demonstrate various uses of client events.
 *
 * @author Tim Middleton 2021.04.30
 */
public class ClientEventsTest {

    private static NamedMap<Integer, Customer> customers;

    private final AtomicInteger insertCount = new AtomicInteger();

    private final AtomicInteger deleteCount = new AtomicInteger();

    public int getInsertCount() {
        return insertCount.get();
    }

    public int getDeleteCount() {
        return deleteCount.get();
    }

    // #tag::bootstrap[]
    @BeforeAll
    static void boostrapCoherence() {
        Coherence coherence = Coherence.clusterMember();
        coherence.start().join();

        customers = coherence.getSession().getMap("customers");
    }
    // #end::bootstrap[]

    // #tag::cleanup[]
    @AfterAll
    static void shutdownCoherence() {
        Coherence coherence = Coherence.getInstance();
        coherence.close();
    }
    // #end::cleanup[]

    // #tag::testMapListeners[]
    @Test
    public void testMapListeners() {
        Customer customer1;
        Customer customer2;
        Customer customer3;
        Customer customer4;

        // #tag::testStandardMapListener[]
        Logger.info("*** testStandardMapListener");
        customers.clear();
        CustomerMapListener mapListener = new CustomerMapListener(); // <1>
        customers.addMapListener(mapListener); // <2>

        customer1 = new Customer(1, "Tim", "123 James Street Perth", Customer.BRONZE, 1000);
        customer2 = new Customer(2, "James Brown", "1 Main Street New York NY", Customer.GOLD, 10000);

        customers.put(customer1.getId(), customer1); // <3>
        customers.put(customer2.getId(), customer2);

        customers.invoke(1, Processors.update(Customer::setCreditLimit, 2000L));  // <4>
        customers.remove(1);  // <5>

        // ensure that we see all events <6>
        Eventually.assertThat(invoking(mapListener).getInsertCount(), is(2));
        Eventually.assertThat(invoking(mapListener).getUpdateCount(), is(1));
        Eventually.assertThat(invoking(mapListener).getRemoveCount(), is(1));

        customers.removeMapListener(mapListener);
        // #end::testStandardMapListener[]

        // #tag::testMultiplexingMapListener[]
        Logger.info("*** testMultiplexingMapListener");
        customers.clear();
        MapListener<Integer, Customer> multiplexingMapListener = new MultiplexingCustomerMapListener(); // <1>
        // Multiplexing MapListener listening on all entries
        customers.addMapListener(multiplexingMapListener);  // <2>

        customer1 = new Customer(1, "James Brown", "1 Main Street New York NY", Customer.GOLD, 10000);

        customers.put(customer1.getId(), customer1); // <3>
        customers.invoke(1, Processors.update(Customer::setAddress, "Updated address"));
        customers.remove(1);

        // ensure that we see all events <4>
        Eventually.assertThat(invoking((MultiplexingCustomerMapListener) multiplexingMapListener).getCount(), is(3));

        customers.removeMapListener(multiplexingMapListener);
        // #end::testMultiplexingMapListener[]

        // #tag::testSimpleMapListener[]
        Logger.info("*** testSimpleMapListener");
        customers.clear();
        MapListener<Integer, Customer> simpleMapListener = new SimpleMapListener<Integer, Customer>()  // <1>
               .addInsertHandler((e) -> Logger.info("New Customer added with id=" + e.getNewValue().getId())) // <2>
               .addDeleteHandler((e) -> Logger.info("Deleted customer id =" + e.getOldValue().getId())) // <3>
               .addInsertHandler((e) -> insertCount.incrementAndGet()) // <4>
               .addDeleteHandler((e) -> deleteCount.incrementAndGet()); // <5>

        customers.addMapListener(simpleMapListener, 1, false);  // <6>

        customer1 = new Customer(1, "Tim", "123 James Street Perth", Customer.BRONZE, 1000);
        customer2 = new Customer(2, "James Brown", "1 Main Street New York NY", Customer.GOLD, 10000);

        customers.put(customer1.getId(), customer1);
        customers.put(customer2.getId(), customer2);

        customers.clear();

        // should only be 1 insert and 1 delete as we are listening on the key  // <7>
        Eventually.assertThat(invoking(this).getInsertCount(), is(1));
        Eventually.assertThat(invoking(this).getDeleteCount(), is(1));

        // #end::testSimpleMapListener[]

        // #tag::testListenOnQueries[]
        Logger.info("*** testListenOnQueries");
        customers.clear();
        mapListener = new CustomerMapListener(); // <1>

        // MapListener listening only to new customers from NY
        Filter<Customer>                  filter      = Filters.like(Customer::getAddress, "%NY%"); // <2>
        MapEventFilter<Integer, Customer> eventFilter = new MapEventFilter<>(filter);

        customer1 = new Customer(1, "Tim", "123 James Street, Perth, Australia", Customer.BRONZE, 1000);
        customer2 = new Customer(2, "James Brown", "1 Main Street, New York, NY", Customer.GOLD, 10000);
        customer3 = new Customer(3, "Tony Stark", "Malibu Point 10880, 90265 Malibu, CA", Customer.SILVER, 333333);
        customer4 = new Customer(4, "James Stewart", "123 5th Ave, New York, NY", Customer.SILVER, 200);

        // Listen only for events where address is in New York
        customers.addMapListener(mapListener, eventFilter, true); // <3>

        customers.put(customer1.getId(), customer1);
        customers.put(customer2.getId(), customer2);
        customers.put(customer3.getId(), customer3);
        customers.put(customer4.getId(), customer4);

        // ensure that we see all events // <4>
        Eventually.assertThat(invoking(mapListener).getInsertCount(), is(2));
        
        // ensure we only receive lite events
        Eventually.assertThat(invoking(mapListener).getLiteEvents(), is(2));

        customers.removeMapListener(mapListener, eventFilter);
        // #end::testListenOnQueries[]

        // #tag::testEventTypes[]
        Logger.info("*** testEventTypes");
        customers.clear();
        mapListener = new CustomerMapListener(); // <1>

        filter = Filters.equal(Customer::getCustomerType, Customer.GOLD); // <2>

        // listen only for events where customers has been inserted as GOLD or updated to GOLD status or were changed from GOLD
        int mask = MapEventFilter.E_INSERTED | MapEventFilter.E_UPDATED_ENTERED| MapEventFilter.E_UPDATED_LEFT;  // <3>
        eventFilter = new MapEventFilter<>(mask, filter);

        customers.addMapListener(mapListener, eventFilter, false); // <4>

        customer1 = new Customer(1, "Tim", "123 James Street Perth", Customer.BRONZE, 1000);
        customer2 = new Customer(2, "James Brown", "1 Main Street New York NY", Customer.GOLD, 10000);
        customer3 = new Customer(3, "Tony Stark", "Malibu Point 10880, 90265 Malibu, CA", Customer.SILVER, 333333);

        customers.put(customer1.getId(), customer1);
        customers.put(customer2.getId(), customer2);
        customers.put(customer3.getId(), customer3);

        // update customer 1 from BRONZE to GOLD
        customers.invoke(1, Processors.update(Customer::setCustomerType, Customer.GOLD));
        customers.invoke(2, Processors.update(Customer::setCustomerType, Customer.SILVER));

        // ensure that we see all events // <5>
        Eventually.assertThat(invoking(mapListener).getInsertCount(), is(1));
        Eventually.assertThat(invoking(mapListener).getUpdateCount(), is(2));

        customers.removeMapListener(mapListener, eventFilter);
        // #end::testEventTypes[]

    }
    // #end::testMapListeners[]

    // #tag::maplistener1[]
    /**
     * Simple {@link MapListener} implementation for Customers.
     */
    public static class CustomerMapListener
            implements MapListener<Integer, Customer> { // <1>

        private final AtomicInteger insertCount = new AtomicInteger();  // <2>

        private final AtomicInteger updateCount = new AtomicInteger();

        private final AtomicInteger removeCount = new AtomicInteger();

        private final AtomicInteger liteEvents = new AtomicInteger();

        @Override
        public void entryInserted(MapEvent<Integer, Customer> mapEvent) { // <3>
            Logger.info("New customer: new key/value=" + mapEvent.getKey() + "/" + mapEvent.getNewValue());
            insertCount.incrementAndGet();
            if (mapEvent.getNewValue() == null) {
                liteEvents.incrementAndGet();
            }
        }

        @Override
        public void entryUpdated(MapEvent<Integer, Customer> mapEvent) { // <4>
            Logger.info("Updated customer key=" + mapEvent.getKey() + ", old=" + mapEvent.getOldValue() + ", new=" +
                        mapEvent.getNewValue());
            updateCount.incrementAndGet();
            if (mapEvent.getOldValue() == null) {
                liteEvents.incrementAndGet();
            }
        }

        @Override
        public void entryDeleted(MapEvent<Integer, Customer> mapEvent) { // <5>
            Logger.info("Deleted customer: old key/value=" + mapEvent.getKey() + "/" + mapEvent.getOldValue());
            removeCount.incrementAndGet();
            if (mapEvent.getOldValue() == null) {
                liteEvents.incrementAndGet();
            }
        }

        public int getInsertCount() {
            return insertCount.get();
        }

        public int getUpdateCount() {
            return updateCount.get();
        }

        public int getRemoveCount() {
            return removeCount.get();
        }

        public int getLiteEvents() {
            return liteEvents.get();
        }
    }
    // #end::maplistener1[]

    // #tag::maplistener2[]
    /**
     * Simple {@link MultiplexingMapListener} implementation for Customers.
     */
    public static class MultiplexingCustomerMapListener
            extends MultiplexingMapListener<Integer, Customer> {  // <1>

        private final AtomicInteger counter = new AtomicInteger(); // <2>

        @Override
        protected void onMapEvent(MapEvent<Integer, Customer> mapEvent) { // <3>
            Logger.info("isInsert=" + mapEvent.isInsert() +
                        ", isDelete=" + mapEvent.isDelete() +
                        ", isUpdate=" + mapEvent.isUpdate());
            Logger.info("key=" + mapEvent.getKey() + ", old=" + mapEvent.getOldValue() + ", new=" + mapEvent.getNewValue());
            Logger.info(mapEvent.toString());

            counter.incrementAndGet();
        }

        public int getCount() {
            return counter.get();
        }
    }
    // #end::maplistener2[]
}
