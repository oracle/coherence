/*
 * Copyright (c) 2000-2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.guides.aggregations.model;

import java.io.Serializable;

/**
 * An order line that is entirely enclosed within an Order object.
 *
 * @author Tim Middleton 2021-02-25
 */
public class OrderLine
        implements Serializable {

    private int    lineNumber;
    private String productDescription;
    private double costPerItem;
    private int    itemCount;

    public OrderLine() {
    }

    public OrderLine(int lineNumber, String productDescription, double costPerItem, int itemCount) {
        this.lineNumber = lineNumber;
        this.productDescription = productDescription;
        this.costPerItem = costPerItem;
        this.itemCount = itemCount;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getProductDescription() {
        return productDescription;
    }

    public void setProductDescription(String productDescription) {
        this.productDescription = productDescription;
    }

    public double getCostPerItem() {
        return costPerItem;
    }

    public void setCostPerItem(double costPerItem) {
        this.costPerItem = costPerItem;
    }

    public int getItemCount() {
        return itemCount;
    }

    public void setItemCount(int itemCount) {
        this.itemCount = itemCount;
    }

    public double getOrderLineTotal() {
        return itemCount * costPerItem;
    }

    @Override
    public String toString() {
        return "OrderLine{" +
               "lineNumber=" + lineNumber +
               ", productDescription='" + productDescription + '\'' +
               ", costPerItem=" + costPerItem +
               ", itemCount=" + itemCount +
               '}';
    }
}
