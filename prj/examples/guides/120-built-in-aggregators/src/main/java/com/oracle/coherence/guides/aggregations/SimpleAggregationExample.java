/*
 * Copyright (c) 2021, 2024 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.guides.aggregations;

import com.oracle.coherence.common.base.Logger;

import com.oracle.coherence.guides.aggregations.model.Address;
import com.oracle.coherence.guides.aggregations.model.Customer;
import com.oracle.coherence.guides.aggregations.model.Order;

import com.oracle.coherence.guides.aggregations.model.OrderLine;

import com.tangosol.net.Coherence;
import com.tangosol.net.CoherenceConfiguration;
import com.tangosol.net.NamedMap;
import com.tangosol.net.Session;

import com.tangosol.util.Aggregators;
import com.tangosol.util.Filters;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.aggregator.GroupAggregator;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Simple aggregation examples.
 *
 * @author Tim Middleton  2021.02.25
 */
public class SimpleAggregationExample {

    private static final int OFFICE = 0;

    private static final int POSTAL = 1;

    /**
     * Random instance with predictable seed.
     */
    protected static final Random RANDOM = new Random(12312383838L);

    private static final String[] STATES = new String[] {"WA", "SA", "VIC", "NSW", "ACT", "QLD", "NT", "TAS"};

    private static final String[] STREETS = new String[] {
            "James Street", "Williams Street", "Hay Street", "Shenton Ave",
            "Railway Parade", "Roe Street", "North Beach Road", "Grand Blvd",
            "Collier Pass", "Mitchel Freeway", "South Street", "Lakeside Drive"
    };

    private static final String[] SUBURBS = new String[] {
            "Iluka", "Clarkson", "Quinns Rock", "North Perth", "Ocean Reef",
            "Belmont", "Padbury", "Carine", "North Beach", "Fremantle",
            "Mount Hawthorn", "East Perth", "Perth", "Kensington", "Waterford"
    };

    private static final String[] POSTCODES = new String[] {
            "6028", "6030", "6030", "6006", "6028",
            "6104", "6025", "6020", "6020", "6160",
            "6016", "6004", "6000", "6151", "6152"
    };

    private static final String[] PRODUCTS = new String[] {
            "Commodore 64", "Commodore 128", "Apple IIe",
            "Sinclair ZX81", "IBM PCjr", "Commodore Amiga (1000)",
            "Atari ST 1024", "Commodore VIC-20", "Altair 8800",
            "Tandy TRS-80", "BBC Micro", "ZX Spectrum 48K", "IBM PC"
    };

    private static final String STORAGE_ENABLED = "coherence.distributed.localstorage";

    /**
     * Maximum customers to add.
     */
    private static final int MAX_CUSTOMERS = 10000;

    /**
     * Customers {@link NamedMap}.
     */
    private NamedMap<Integer, Customer> customers;

    /**
    * Orders {@link NamedMap}.
    */
    private NamedMap<Integer, Order> orders;

    /**
     * Entry point to run from IDE.
     *
     * @param args arguments
     */
    public static void main(String[] args) {
        // if storage-enabled is not set then set it to true
        if (getStorageEnabled() == null) {
            setStorageEnabled(true);
        }

        SimpleAggregationExample example = new SimpleAggregationExample();
        example.populate();
        example.runExample();
    }

    /**
     * Constructor.
     */
    public SimpleAggregationExample() {
        CoherenceConfiguration cfg = CoherenceConfiguration.builder().build();
        Coherence coherence = Coherence.clusterMember(cfg);
        coherence.start().join();
        Session session = coherence.getSession();
        this.customers = session.getMap("customers");
        this.orders = session.getMap("orders");
    }

    /**
     * Populate the map with data.
     */
    public void populate() {
        NamedMap<Integer, Customer> customers = getCustomers();
        NamedMap<Integer, Order> orders = getOrders();
        customers.clear();
        orders.clear();

        // add the indexes when the maps are empty
        getOrders().addIndex(Order::getOrderTotal, false, null);
        getOrders().addIndex(Order::getOrderLineCount, false, null);
        getCustomers().addIndex(ValueExtractor.of(Customer::getOfficeAddress).andThen(Address::getState), false, null);

        addCustomers();
        createOrdersForAllCustomers();
    }

    // tag::runExample[]
    /**
     * Run the example.
     */
    public void runExample() {
        NamedMap<Integer, Customer> customers = getCustomers();
        NamedMap<Integer, Order> orders = getOrders();

        // count the customers using the Aggregators helper
        int customerCount = customers.aggregate(Aggregators.count());
        Logger.info("Customer Count = " + customerCount);

        // count the orders
        int orderCount = orders.aggregate(Aggregators.count());
        Logger.info("Order Count = " + orderCount);

        // get the total value of all orders - requires index on Order::getOrderTotal to be efficient
        Double totalOrders = orders.aggregate(Aggregators.sum(Order::getOrderTotal));
        Logger.info("Total Order Value " + formatMoney(totalOrders));

        // get the average order value across all orders - requires index to be efficient
        Double averageOrderValue = orders.aggregate(Aggregators.average(Order::getOrderTotal));
        Logger.info("Average Order Value " + formatMoney(averageOrderValue));

        // get the minimum order value where then is only 1 order line - requires index on Order::getOrderLineCount to be efficient
        Double minOrderValue1Line = orders.aggregate(Filters.equal(Order::getOrderLineCount, 1),
                Aggregators.min(Order::getOrderTotal));
        Logger.info("Min Order Value for orders with 1 line " + formatMoney(minOrderValue1Line));

        // get the outstanding balances by state - requires index on the full ValueExtractor to be efficient
        ValueExtractor<Customer, String> officeState = ValueExtractor.of(Customer::getOfficeAddress).andThen(Address::getState);
        Map<String, BigDecimal> mapOutstandingByState = customers.aggregate(
                GroupAggregator.createInstance(officeState, Aggregators.sum(Customer::getOutstandingBalance)));
        mapOutstandingByState.forEach((k, v) -> Logger.info("State: " + k + ", outstanding total is " + formatMoney(v)));

        // get the top 5 order totals by value
        Logger.info("Top 5 orders by value");
        Object[] topOrderValues = orders.aggregate(Aggregators.topN(Order::getOrderTotal, 5));
        for (Object value : topOrderValues) {
            Logger.info(formatMoney((Double) value));
        }
    }
    // end::runExample[]

    /**
     * Add the specified number of customers.
     */
    protected void addCustomers() {
        final int              BATCH = 1000;
        Map<Integer, Customer> map   = new HashMap<>();
        Logger.info("Creating " + MAX_CUSTOMERS + " customers");

        for (int i = 1; i <= MAX_CUSTOMERS; i++) {
            map.put(i, new Customer(i, "Customer " + i, getRandomAddress(OFFICE), getRandomAddress(POSTAL),
                    BigDecimal.valueOf(RANDOM.nextInt(10) * 100)));
            if (i % BATCH == 0) {
                customers.putAll(map);
                map.clear();
            }
        }

        if (!map.isEmpty()) {
            customers.putAll(map);
        }
    }

    /**
     * Create a random number of orders for each customer.
     */
    protected void createOrdersForAllCustomers() {
        Logger.info("Creating orders for customers");
        int                 orderId = 1000;
        Map<Integer, Order> map     = new HashMap<>();

        for (int i = 1; i <= MAX_CUSTOMERS; i++) {
            int orderCount = RANDOM.nextInt(5) + 1;

            for (int o = 0; o < orderCount; o++) {
                Order order = createRandomOrder(orderId++, i, RANDOM.nextInt(3) + 1);
                map.put(order.getOrderId(), order);
            }
            orders.putAll(map);
            map.clear();
        }

        Logger.info("Orders created");
    }

    /**
     * Return a random address in Australia.
     *
     * @param type either POSTAL or OFFICE
     *
     * @return a new {@link Address}
     */
    protected Address getRandomAddress(int type) {
        String addressLine1;
        String addressLine2 = null;
        if (type == POSTAL) {
            addressLine1 = ("PO Box " + (RANDOM.nextInt(200) + 1));
        }
        else {
            addressLine1 = (RANDOM.nextInt(400) + 1) + " " + getRandomValue(STREETS);
            if (RANDOM.nextInt(100) >= 90) {
                // 10% of the time add an address line 1
                addressLine2 = addressLine1;
                addressLine1 = "Level " + RANDOM.nextInt(10) + 1;
            }
        }

        // make sure the suburb always matches post code
        int suburb = RANDOM.nextInt(POSTCODES.length);
        return new Address(addressLine1, addressLine2, SUBURBS[suburb], "Perth",
                getRandomValue(STATES), Integer.parseInt(POSTCODES[suburb]));
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
            OrderLine orderLine = new OrderLine(i, getRandomValue(PRODUCTS), RANDOM.nextDouble() * 500 + 500,
                    RANDOM.nextInt(3) + 1);
            order.addOrderLine(orderLine);
        }

        return order;
    }

    /**
     * Return a random value from an array.
     *
     * @param array array to return from
     *
     * @return a random value
     */
    protected String getRandomValue(String[] array) {
        int len = array.length;
        return array[RANDOM.nextInt(len)];
    }

    /**
     * Format a money value.
     *
     * @param value value to format
     *
     * @return a formatted money value
     */
    protected String formatMoney(Number value) {
        return String.format("$%,.2f", value.floatValue());
    }

    /**
     * Returns the customers {@link NamedMap}.
     *
     * @return the customers {@link NamedMap}
     */
    private NamedMap<Integer, Customer> getCustomers() {
        return customers;
    }

    /**
     * Returns the orders {@link NamedMap}.
     *
     * @return the orders {@link NamedMap}
     */
    private NamedMap<Integer, Order> getOrders() {
        return orders;
    }

    /**
     * Return true if the member is storage-enabled.
     *
     * @return rue if the member is storage-enabled
     */
    protected static String getStorageEnabled() {
        return System.getProperty(STORAGE_ENABLED);
    }

    /**
     * Set if the member is storage-enabled.
     *
     * @param storageEnabled if the member is storage-enabled
     */
    protected static void setStorageEnabled(boolean storageEnabled) {
        System.setProperty(STORAGE_ENABLED, Boolean.toString(storageEnabled));
    }
}
