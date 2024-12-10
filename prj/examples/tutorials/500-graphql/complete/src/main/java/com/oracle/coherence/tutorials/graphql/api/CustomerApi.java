/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.tutorials.graphql.api;


import com.oracle.coherence.tutorials.graphql.model.Customer;
import com.oracle.coherence.tutorials.graphql.model.Order;
import com.oracle.coherence.tutorials.graphql.model.OrderLine;
import com.tangosol.net.NamedMap;
import com.tangosol.util.Filter;
import com.tangosol.util.Filters;
import com.tangosol.util.QueryHelper;

import java.util.Collection;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Name;
import org.eclipse.microprofile.graphql.Query;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.Timed;


/**
 * A class that exposes a GraphQL endpoint.
 *
 * @author Tim Middleton 2021-01-25
 */
// tag::annotations[]
@ApplicationScoped
@GraphQLApi
public class CustomerApi {
    // end::annotations[]

    // tag::namedMaps[]
    /**
     * The {@link NamedMap} for customers.
     */
    @Inject
    private NamedMap<Integer, Customer> customers;

    /**
     * The {@link NamedMap} for orders.
     */
    @Inject
    private NamedMap<Integer, Order> orders;
    // end::namedMaps[]

    // tag::getCustomers[]
    /**
     * Returns all of the {@link Customer}s.
     *
     * @return all of the {@link Customer}s.
     */
    @Query
    @Description("Displays customers")
    @Counted // <1>
    public Collection<Customer> getCustomers() {
        return customers.values();
    }
    // end::getCustomers[]

    // tag::displayOrders[]
    /**
     * Returns {@link Order}s that match the where clause or all {@link Order}s
     * if the where clause is null.
     *
     * @param whereClause where clause to restrict selection of {@link Order}s
     *
     * @return {@link Order}s that match the where clause or all {@link Order}s
     * if the where clause is null
     */
    @Query("displayOrders")
    @Timed
    public Collection<Order> getOrders(@Name("whereClause") String whereClause) {
        try {
            Filter filter = whereClause == null
                            ? Filters.always()
                            : QueryHelper.createFilter(whereClause);
            return orders.values(filter);
        }
        catch (Exception e) {
            throw new IllegalArgumentException("Invalid where clause: [" + whereClause + "]");
        }
    }
    // end::displayOrders[]

    // tag::createCustomer[]
    /**
     * Creates and saves a {@link Customer}.
     *
     * @param customer and saves a {@link Customer}
     *
     * @return the new {@link Customer}
     */
    @Mutation
    @Timed // <1>
    public Customer createCustomer(@Name("customer") Customer customer) {
        if (customers.containsKey(customer.getCustomerId())) {
            throw new IllegalArgumentException("Customer " + customer.getCustomerId() + " already exists");
        }

        customers.put(customer.getCustomerId(), customer);
        return customers.get(customer.getCustomerId());
    }
    // end::createCustomer[]

    // tag::createOrder[]
    /**
     * Creates and saves an {@link Order} for a given customer id.
     *
     * @param customerId customer id to create the {@link Order} for
     * @param orderId    order id
     *
     * @return the new {@link Order}
     *
     * @throws CustomerNotFoundException if the {@link Customer} was not found
     */
    @Mutation
    @Timed // <1>
    public Order createOrder(@Name("customerId") int customerId,
                             @Name("orderId") int orderId)
            throws CustomerNotFoundException {
        if (!customers.containsKey(customerId)) {
            throw new CustomerNotFoundException("Customer id " + customerId + " was not found");
        }

        if (orders.containsKey(orderId)) {
            throw new IllegalArgumentException("Order " + orderId + " already exists");
        }

        Order order = new Order(orderId, customerId);
        orders.put(orderId, order);
        return orders.get(orderId);
    }
    // end::createOrder[]

    // tag::addOrderLineToOrder[]
    /**
     * Adds an {@link OrderLine} to an existing {@link Order}.
     *
     * @param orderId   order id to add to
     * @param orderLine {@link OrderLine} to add
     *
     * @return the updates {@link Order}
     *
     * @throws OrderNotFoundException the {@link Order} was not found
     */
    @Mutation
    @Timed  // <1>
    public Order addOrderLineToOrder(@Name("orderId") int orderId,
                                     @Name("orderLine") OrderLine orderLine)
            throws OrderNotFoundException {
        if (!orders.containsKey(orderId)) {
            throw new OrderNotFoundException("Order number " + orderId + " was not found");
        }

        if (orderLine.getProductDescription() == null || orderLine.getProductDescription().equals("") ||
            orderLine.getItemCount() <= 0 || orderLine.getCostPerItem() <= 0) {
            throw new IllegalArgumentException("Supplied Order Line is invalid: " + orderLine);
        }

        return orders.compute(orderId, (k, v)->{
            v.addOrderLine(orderLine);
            return v;
        });

    }
    // end::addOrderLineToOrder[]
}
