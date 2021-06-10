/*
 * Copyright (c) 2000-2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.guides.aggregations.model;

import java.io.Serializable;

import java.time.LocalDate;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

/**
 * A class representing an Order.
 *
 * @author Tim Middleton 2021-02-25
 */
public class Order
        implements Serializable {

    /**
     * An enum to represent the order status.
     */
    public enum OrderStatus {
        NEW,
        IN_PROGRESS,
        COMPLETE,
        CANCELLED
    }

    private int orderId;
    private int customerId;
    private LocalDate orderDate;
    private OrderStatus status;
    private Collection<OrderLine> orderLines;

    public Order() {
        this.orderLines = new HashSet<>();
    }

    public Order(int orderId, int customerId) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.orderDate = LocalDate.now();
        this.status = OrderStatus.NEW;
        this.orderLines = new HashSet<>();
    }
    
    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDate orderDate) {
        this.orderDate = orderDate;
    }

    public Collection<OrderLine> getOrderLines() {
        return orderLines;
    }

    public void addOrderLine(OrderLine orderLine) {
        orderLines.add(orderLine);
    }

    public double getOrderTotal() {
        return orderLines.stream().mapToDouble(OrderLine::getOrderLineTotal).sum();
    }

    public int getOrderLineCount() {
        return orderLines.size();
    }

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
               Objects.equals(orderDate, order.orderDate) &&
               status == order.status &&
               Objects.equals(orderLines, order.orderLines);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId, customerId, orderDate, status, orderLines);
    }
}
