/*
 * Copyright (c) 2000-2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.tutorials.graphql.model;


import com.oracle.coherence.inject.Injectable;
import com.tangosol.net.NamedMap;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;

import java.util.Objects;
import javax.inject.Inject;
import org.eclipse.microprofile.graphql.NumberFormat;


/**
 * A class representing an Order.
 *
 * @author Tim Middleton 2021-01-25
 */
// tag::injectable[]
public class Order
        implements Serializable, Injectable {
    // end::injectable[]

    /**
     * An enum to represent the order status.
     */
    public enum OrderStatus {
        NEW,
        IN_PROGRESS,
        COMPLETE,
        CANCELLED
    }

    // tag::namedMap[]

    /**
     * The {@link NamedMap} for customers.
     */
    @Inject
    private transient NamedMap<Integer, Customer> customers;
    // end::namedMap[]

    /**
     * Order id.
     */
    private int orderId;

    /**
     * Customer id.
     */
    private int customerId;

    /**
     * Order date.
     */
    private LocalDate orderDate;

    /**
     * Order status.
     */
    private OrderStatus status;

    /**
     * Order lines.
     */
    private Collection<OrderLine> orderLines;

    /**
     * No-args constructor.
     */
    public Order() {
        this.orderLines = new HashSet<>();
    }

    /**
     * Constructs an {@link Order}
     *
     * @param orderId    order id
     * @param customerId customer id
     */
    public Order(int orderId, int customerId) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.orderDate = LocalDate.now();
        this.status = OrderStatus.NEW;
        this.orderLines = new HashSet<>();
    }

    // tag::getCustomer[]
    /**
     * Returns the {@link Customer} for this {@link Order}.
     *
     * @return the {@link Customer} for this {@link Order}
     */
    public Customer getCustomer() {
        return customers.get(customerId);
    }
    // end::getCustomer[]

    /**
     * Returns the order id.
     *
     * @return the order id
     */
    public int getOrderId() {
        return orderId;
    }

    /**
     * Sets the order id.
     *
     * @param orderId the order id
     */
    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    /**
     * Returns the customer id.
     *
     * @return the customer id
     */
    public int getCustomerId() {
        return customerId;
    }

    /**
     * Sets the customer id.
     *
     * @param customerId the customer id
     */
    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    /**
     * Returns the order date.
     *
     * @return the order date
     */
    public LocalDate getOrderDate() {
        return orderDate;
    }

    /**
     * Sets the order date.
     *
     * @param orderDate the order date
     */
    public void setOrderDate(LocalDate orderDate) {
        this.orderDate = orderDate;
    }

    /**
     * Returns the {@link OrderLine}s for this order.
     *
     * @return the {@link OrderLine}s for this order
     */
    public Collection<OrderLine> getOrderLines() {
        return orderLines;
    }

    /**
     * Adds a {@link OrderLine} for this order.
     *
     * @param orderLine {@link OrderLine}
     */
    public void addOrderLine(OrderLine orderLine) {
        orderLines.add(orderLine);
    }

    // tag::numberFormat[]
    /**
     * Returns the order total.
     *
     * @return the order total
     */
    @NumberFormat("$###,###,##0.00")
    public double getOrderTotal() {
        return orderLines.stream().mapToDouble(OrderLine::getOrderLineTotal).sum();
    }
    // end::numberFormat[]

    @Override
    public String toString() {
        return "Order{" +
               "orderId=" + orderId +
               ", customerId=" + customerId +
               ", orderDate=" + orderDate +
               ", status=" + status +
               ", orderLines=" + orderLines +
               '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Order order = (Order) o;
        return orderId == order.orderId &&
               customerId == order.customerId &&
               Objects.equals(customers, order.customers) &&
               Objects.equals(orderDate, order.orderDate) &&
               status == order.status &&
               Objects.equals(orderLines, order.orderLines);
    }

    @Override
    public int hashCode() {
        return Objects.hash(customers, orderId, customerId, orderDate, status, orderLines);
    }
}
