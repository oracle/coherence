/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.notifications;

import com.oracle.coherence.guides.notifications.extractors.NotificationExtractor;

import com.oracle.coherence.guides.notifications.model.Customer;
import com.oracle.coherence.guides.notifications.model.Notification;

import com.oracle.coherence.guides.notifications.model.NotificationId;
import com.oracle.coherence.guides.notifications.processors.AddNotifications;

import com.oracle.coherence.repository.AbstractRepository;

import com.tangosol.net.CacheService;
import com.tangosol.net.Coherence;
import com.tangosol.net.NamedCache;
import com.tangosol.net.NamedMap;
import com.tangosol.net.Session;

import com.tangosol.util.ValueExtractor;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * An implementation of a Coherence {@link AbstractRepository}
 * for manipulating {@link Customer} entities.
 *
 * @author Jonathan Knight 2022.06.20
 * @since 22.06
 */
// # tag::one[]
public class CustomerRepository
        extends AbstractRepository<String, Customer> {
    // # end::one[]

    /**
     * The name of the cache containing customers.
     */
    public static final String CUSTOMERS_MAP_NAME = "customers";

    /**
     * The name of the cache containing notifications.
     */
    public static final String NOTIFICATIONS_MAP_NAME = "notifications";

    // # tag::two[]

    /**
     * The customer's cache.
     */
    private final NamedMap<String, Customer> customers;
    // # end::two[]

    /**
     * Create a {@link CustomerRepository}.
     * <p>
     * This repository will use the default Coherence {@link Session} to obtain
     * the customer and notification caches.
     */
    public CustomerRepository() {
        this(Coherence.getInstance().getSession());
    }

    /**
     * Create a {@link CustomerRepository}.
     *
     * @param session  the {@link Session} to use to obtain
     *                 the customer and notification caches
     */
    public CustomerRepository(Session session) {
        this(session.getMap(CUSTOMERS_MAP_NAME));
    }

    /**
     * Create a {@link CustomerRepository}.
     *
     * @param customers      the customer cache
     */
    // # tag::construct[]
    public CustomerRepository(NamedMap<String, Customer> customers) {
        this.customers = customers;
    }
    // # end::construct[]

    // # tag::three[]
    @Override
    protected String getId(Customer entity) {
        return entity.getId();
    }

    @Override
    protected Class<? extends Customer> getEntityType() {
        return Customer.class;
    }

    @Override
    protected NamedMap<String, Customer> getMap() {
        return customers;
    }
    // # end::three[]

    // # tag::index[]
    @Override
    @SuppressWarnings( {"unchecked", "resource"})
    protected void createIndices() {
        super.createIndices();
        CacheService                             service       = customers.getService();
        NamedCache<NotificationId, Notification> notifications = service.ensureCache(NOTIFICATIONS_MAP_NAME,
                service.getContextClassLoader());
        notifications.addIndex(ValueExtractor.of(NotificationId::getCustomerId).fromKey());
        notifications.addIndex(ValueExtractor.of(NotificationId::getRegion).fromKey());
    }
    // # end::index[]

    /**
     * Add a {@link Notification} for a {@link Customer}.
     *
     * @param customer      the {@link Customer} to add the notification to
     * @param region        the region to add the notification for
     * @param notification  the notification to add
     *
     * @throws NullPointerException if any of the parameters are null
     */
    public void addNotifications(Customer customer, String region, Notification notification) {
        addNotifications(customer, region, List.of(Objects.requireNonNull(notification)));
    }

    /**
     * Add {@link Notification notifications} for a {@link Customer}.
     *
     * @param customer       the {@link Customer} to add the notification to
     * @param region         the region to add the notification for
     * @param notifications  the notifications to add
     *
     * @throws NullPointerException if any of the parameters are null
     */
    public void addNotifications(Customer customer, String region, List<Notification> notifications) {
        addNotifications(customer.getId(), region, notifications);
    }

    /**
     * Add {@link Notification notifications} for a {@link Customer}.
     *
     * @param customerId     the identifier of the {@link Customer} to add the notification to
     * @param region         the region to add the notification for
     * @param notifications  the notifications to add
     *
     * @throws NullPointerException if any of the parameters are null
     */
    public void addNotifications(String customerId, String region, List<Notification> notifications) {
        addNotifications(customerId,
                Collections.singletonMap(Objects.requireNonNull(region), Objects.requireNonNull(notifications)));
    }

    public void addNotifications(Customer customer, Map<String, List<Notification>> notifications) {
        addNotifications(customer.getId(), notifications);
    }

    /**
     * Add {@link Notification notifications} for a {@link Customer}.
     *
     * @param customerId     the identifier of the {@link Customer} to
     *                       add the notifications to
     * @param notifications  a map of notifications to add, keyed
     *                       by region
     *
     * @throws NullPointerException if any of the parameters are null
     */
    // # tag::add[]
    public void addNotifications(String customerId, Map<String, List<Notification>> notifications) {
        ensureInitialized();
        customers.invoke(Objects.requireNonNull(customerId),
                new AddNotifications(Objects.requireNonNull(notifications)));
    }
    // # end::add[]

    /**
     * Returns the notifications for a customer.
     *
     * @param customer  the customer to obtain the notifications for
     *
     * @return the notifications for the customer
     *
     * @throws NullPointerException if the customer is {@code null}
     */
    public List<Notification> getNotifications(Customer customer) {
        return getNotifications(customer.getId());
    }


    /**
     * Returns the notifications for a customer, and optionally a region.
     *
     * @param customer  the customer to obtain the notifications for
     * @param region    an optional region to get notifications for
     *
     * @return the notifications for the customer, optionally restricted to a region
     *
     * @throws NullPointerException if the customer is {@code null}
     */
    public List<Notification> getNotifications(Customer customer, String region) {
        return getNotifications(customer.getId(), region);
    }

    // # tag::get[]

    /**
     * Returns the notifications for a customer.
     *
     * @param customerId  the identifier of the customer to obtain the notifications for
     *
     * @return the notifications for the customer
     */
    public List<Notification> getNotifications(String customerId) {
        return getNotifications(customerId, null);
    }

    /**
     * Returns the notifications for a customer, and optionally a region.
     *
     * @param customerId  the identifier of the customer to obtain the notifications for
     * @param region      an optional region to get notifications for
     *
     * @return the notifications for the customer, optionally restricted to a region
     */
    public List<Notification> getNotifications(String customerId, String region) {
        Map<String, List<Notification>> map = getAll(List.of(customerId), new NotificationExtractor(region));
        return map.getOrDefault(customerId, Collections.emptyList());
    }
    // # end::get[]
}
