/*
 * Copyright (c) 2000-2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.tutorials.graphql.api;


import com.oracle.coherence.tutorials.graphql.model.Customer;
import com.oracle.coherence.tutorials.graphql.model.Order;
import com.oracle.coherence.tutorials.graphql.model.OrderLine;
import com.tangosol.net.NamedMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.inject.Inject;


/**
 * Bootstrap the Coherence application.
 *
 * @author Tim Middleton 2021-01-25
 */
@ApplicationScoped
public class Bootstrap {

    /**
     * List of Products.
     */
    private static final String[] PRODUCT_DESCRIPTIONS = new String[] {
            "Hisense 55S8 Series 8 55 inch 4K UHD Smart TV [2020]",
            "Samsung TU8000 55 inch Crystal UHD 4K Smart TV [2020]",
            "Sony X7000G 49 inch 4k Ultra HD HDR Smart TV",
            "LG UN7300 UHD 50 inch Smart 4K TV with AI ThinQ",
            "TCL S615 40 inch Full HD Android TV",
            "Blaupunkt BP650USG9200 65 inch 4K Ultra HD Android TV",
            "Samsung Q80T 85 inch QLED Ultra HD 4K Smart TV [2020]"
    };

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

    /**
     * Random number generator for products.
     */
    private Random random = new Random();

    // tag::init[]
    /**
     * Initialize the Coherence {@link NamedMap}s with data.
     *
     * @param init init
     */
    private void init(@Observes @Initialized(ApplicationScoped.class) Object init) {
        // end::init[]

        // only initialize if empty
        if (customers.size() == 0) {
            Map<Integer, Customer> mapCustomers = new HashMap<>();

            mapCustomers.put(1, new Customer(1, "Billy Joel", "billy@billyjoel.com", "Address 1", 0.0d));
            mapCustomers.put(2, new Customer(2, "James Brown", "soul@jamesbrown.net", "Address 2", 100.0d));
            mapCustomers.put(3, new Customer(3, "John Williams", "john@statware.com", "Address 3", 0.0d));
            mapCustomers.put(4, new Customer(4, "Tom Jones", "tom@jones.com", "Address 4", 0.0d));

            customers.putAll(mapCustomers);

            // add some orders
            Map<Integer, Order> mapOrders = new HashMap<>();
            mapOrders.put(100, createRandomOrder(100, 1, 1));
            mapOrders.put(101, createRandomOrder(101, 1, 2));
            mapOrders.put(102, createRandomOrder(102, 2, 1));
            mapOrders.put(104, createRandomOrder(104, 3, 4));
            mapOrders.put(105, createRandomOrder(105, 3, 2));

            orders.putAll(mapOrders);

            System.out.println("===CUSTOMERS===");
            customers.values().forEach(System.out::println);

            System.out.println("===ORDERS===");
            orders.values().forEach(System.out::println);
        }
    }

    /**
     * Create a random {@link Order}.
     *
     * @param orderId    order id
     * @param customerId customer id
     * @param orderLines number of order lines
     *
     * @return new new {@link Order}
     */
    private Order createRandomOrder(int orderId, int customerId, int orderLines) {
        Order order = new Order(orderId, customerId);
        for (int i = 1; i <= orderLines; i++) {
            OrderLine orderLine = new OrderLine(i, getRandomProduct(), random.nextDouble() * 2000 + 500, random.nextInt(2) + 1);
            order.addOrderLine(orderLine);
        }

        return order;
    }

    /**
     * Returns a random product.
     *
     * @return a random product
     */
    private String getRandomProduct() {
        return PRODUCT_DESCRIPTIONS[random.nextInt(PRODUCT_DESCRIPTIONS.length)];
    }
}
