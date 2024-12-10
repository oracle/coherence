/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.notifications;

import com.oracle.bedrock.junit.BootstrapCoherence;

import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.coherence.guides.notifications.model.Customer;
import com.oracle.coherence.guides.notifications.model.Notification;
import com.oracle.coherence.guides.notifications.model.NotificationId;

import com.tangosol.net.BackingMapContext;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.CacheService;
import com.tangosol.net.NamedMap;

import com.tangosol.util.Filter;
import com.tangosol.util.Filters;
import com.tangosol.util.MapIndex;
import com.tangosol.util.ValueExtractor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

import static org.hamcrest.MatcherAssert.assertThat;

@BootstrapCoherence(properties = "coherence.serializer=pof")
public class CustomerRepositoryIT {
    @BootstrapCoherence.Cache(name = CustomerRepository.CUSTOMERS_MAP_NAME)
    public static NamedMap<String, Customer> customers;

    @BootstrapCoherence.Cache(name = CustomerRepository.NOTIFICATIONS_MAP_NAME)
    public static NamedMap<NotificationId, Notification> notifications;

    static CustomerRepository repository;

    @BeforeAll
    static void createRepository() {
        repository = new CustomerRepository();
    }

    /**
     * This test is to verify that the required indexes have been created
     * by the {@link CustomerRepository#createIndices()} method.
     */
    @Test
    @SuppressWarnings("rawtypes")
    public void shouldHaveIndexes() {
        repository.createIndices();
        CacheService                  service           = notifications.getService();
        BackingMapManagerContext      context           = service.getBackingMapManager().getContext();
        BackingMapContext             backingMapContext = context.getBackingMapContext(CustomerRepository.NOTIFICATIONS_MAP_NAME);
        Map<ValueExtractor, MapIndex> indexMap          = backingMapContext.getIndexMap();

        ValueExtractor<NotificationId, String> extractorCustomerId = ValueExtractor.of(NotificationId::getCustomerId).fromKey();
        MapIndex                               indexCustomerId     = indexMap.get(extractorCustomerId);
        assertThat(indexCustomerId, is(notNullValue()));
    }

    /**
     * Should be able to add a {@link Customer} to the customers cache
     * using the {@link CustomerRepository}.
     */
    @Test
    public void shouldAddCustomer() {
        CustomerRepository repository = new CustomerRepository();
        Customer           customer   = new Customer("JV19", "Primož", "Roglič");
        repository.save(customer);

        Customer result = customers.get(customer.getId());
        assertThat(result, is(notNullValue()));
        assertThat(result.getId(), is(customer.getId()));
        assertThat(result.getFirstName(), is(customer.getFirstName()));
        assertThat(result.getLastName(), is(customer.getLastName()));
    }

    /**
     * Should be able to add multiple {@link Customer Customers} to the customers cache
     * using the {@link CustomerRepository}.
     */
    @Test
    public void shouldAddMultipleCustomers() {
        CustomerRepository repository = new CustomerRepository();
        Customer           customer1  = new Customer("UAE20", "Tadej", "Pogačar");
        Customer           customer2  = new Customer("JV21", "Wout", "van Aert");

        repository.saveAll(List.of(customer1, customer2));

        Customer result1 = customers.get(customer1.getId());
        assertThat(result1, is(notNullValue()));
        assertThat(result1.getId(), is(customer1.getId()));
        assertThat(result1.getFirstName(), is(customer1.getFirstName()));
        assertThat(result1.getLastName(), is(customer1.getLastName()));

        Customer result2 = customers.get(customer2.getId());
        assertThat(result2, is(notNullValue()));
        assertThat(result2.getId(), is(customer2.getId()));
        assertThat(result2.getFirstName(), is(customer2.getFirstName()));
        assertThat(result2.getLastName(), is(customer2.getLastName()));
    }

    @Test
    public void shouldAddNotification() {
        // # tag::shouldAddNotification[]
        CustomerRepository repository = new CustomerRepository();

        Customer customer = new Customer("QA22", "Julian", "Alaphilippe");
        repository.save(customer);

        Notification notification = new Notification("Ride TdF", LocalDateTime.now().plusDays(1));
        repository.addNotifications(customer, "FRA", notification);
        // # end::shouldAddNotification[]

        Filter<?>                filter     = Filters.equal(ValueExtractor.of(NotificationId::getCustomerId).fromKey(),
                customer.getId());
        Collection<Notification> collection = notifications.values(filter);
        assertThat(collection.size(), is(1));
        Notification result = collection.iterator().next();
        assertThat(result, is(notNullValue()));
        assertThat(result.getBody(), is(notification.getBody()));
        assertThat(result.getTTL(), is(notification.getTTL()));
    }

    @Test
    public void shouldGetNotifications() {
        CustomerRepository repository = new CustomerRepository();
        Customer           customer1  = new Customer("IG1", "Richard", "Carapaz");
        Customer           customer2  = new Customer("IG2", "Filippo", "Ganna");
        Customer           customer3  = new Customer("IG3", "Geraint", "Thomas");
        Customer           customer4  = new Customer("IG4", "Adam", "Yates");
        LocalDateTime      ttl        = LocalDateTime.now().plusDays(1);

        repository.saveAll(List.of(customer1, customer2, customer3, customer4));

        repository.addNotifications(customer1, "Columbia", new Notification("Giro", ttl));
        repository.addNotifications(customer2, "Italy", new Notification("TT", ttl));
        repository.addNotifications(customer3, "Wales", new Notification("TdF", ttl));

        Notification notification1 = new Notification("one", ttl);
        Notification notification2 = new Notification("two", ttl);
        Notification notification3 = new Notification("three", ttl);
        Notification notification4 = new Notification("four", ttl);
        Notification notification5 = new Notification("five", ttl);

        repository.addNotifications(customer4, "Europe", List.of(notification1, notification2));
        repository.addNotifications(customer4, "UK", List.of(notification3, notification4));
        repository.addNotifications(customer4, "England", notification5);

        List<Notification> list = repository.getNotifications(customer4);
        assertThat(list, is(notNullValue()));
        assertThat(list.size(), is(5));
        assertThat(list.get(0), is(notNullValue()));
        assertThat(list.get(0).getBody(), is(notification1.getBody()));
        assertThat(list.get(1), is(notNullValue()));
        assertThat(list.get(1).getBody(), is(notification2.getBody()));
        assertThat(list.get(2), is(notNullValue()));
        assertThat(list.get(2).getBody(), is(notification3.getBody()));
        assertThat(list.get(3), is(notNullValue()));
        assertThat(list.get(3).getBody(), is(notification4.getBody()));
        assertThat(list.get(4), is(notNullValue()));
        assertThat(list.get(4).getBody(), is(notification5.getBody()));

        list = repository.getNotifications(customer4, "UK");
        assertThat(list, is(notNullValue()));
        assertThat(list.size(), is(2));
        assertThat(list.get(0), is(notNullValue()));
        assertThat(list.get(0).getBody(), is(notification3.getBody()));
        assertThat(list.get(1), is(notNullValue()));
        assertThat(list.get(1).getBody(), is(notification4.getBody()));
    }

    @Test
    public void shouldExpireNotifications() {
        CustomerRepository repository = new CustomerRepository();
        Customer           customer   = new Customer("AF01", "Mathieu", "van der Poel");
        repository.save(customer);

        LocalDateTime ttl10         = LocalDateTime.now().plusSeconds(10);
        LocalDateTime ttl20         = LocalDateTime.now().plusSeconds(20);
        LocalDateTime ttl30         = LocalDateTime.now().plusSeconds(30);
        LocalDateTime ttl40         = LocalDateTime.now().plusSeconds(40);
        Notification  notification1 = new Notification("one", ttl10);
        Notification  notification2 = new Notification("two", ttl30);

        repository.addNotifications(customer, "NL", List.of(notification1, notification2));

        List<Notification> list = repository.getNotifications(customer);
        assertThat(list.size(), is(2));

        Eventually.assertDeferred(()->ttl20.isBefore(LocalDateTime.now()), is(true));
        list = repository.getNotifications(customer);
        assertThat(list.size(), is(1));

        Eventually.assertDeferred(()->ttl40.isBefore(LocalDateTime.now()), is(true));
        list = repository.getNotifications(customer);
        assertThat(list.size(), is(0));
    }
}
