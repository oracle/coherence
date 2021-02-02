/*
 * Copyright (c) 2000-2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.tutorials.graphql.model;


import java.io.Serializable;

/**
 * An order line that is entirely enclosed within an Order object.
 *
 * @author Tim Middleton 2021-01-25
 */
public class OrderLine
        implements Serializable {

    /**
     * Line number.
     */
    private int lineNumber;

    /**
     * Product description.
     */
    private String productDescription;

    /**
     * Cost per item.
     */
    private double costPerItem;

    /**
     * Item count.
     */
    private int itemCount;

    /**
     * No-args constructor.
     */
    public OrderLine() {
    }

    /**
     * Constructs a {@link OrderLine}.
     *
     * @param lineNumber         line number
     * @param productDescription product description
     * @param costPerItem        cost per item
     * @param itemCount          item count
     */
    public OrderLine(int lineNumber, String productDescription, double costPerItem, int itemCount) {
        this.lineNumber = lineNumber;
        this.productDescription = productDescription;
        this.costPerItem = costPerItem;
        this.itemCount = itemCount;
    }

    /**
     * Returns the line number.
     *
     * @return the line number
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Sets the line number.
     *
     * @param lineNumber the line number
     */
    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    /**
     * Returns the product description.
     *
     * @return the product description
     */
    public String getProductDescription() {
        return productDescription;
    }

    /**
     * Sets the product description.
     *
     * @param productDescription the product description
     */
    public void setProductDescription(String productDescription) {
        this.productDescription = productDescription;
    }

    /**
     * Return the cost per item.
     *
     * @return the cost per item
     */
    public double getCostPerItem() {
        return costPerItem;
    }

    /**
     * Sets the cost per item.
     *
     * @param costPerItem the cost per item
     */
    public void setCostPerItem(double costPerItem) {
        this.costPerItem = costPerItem;
    }

    /**
     * Returns the item count.
     *
     * @return the item count
     */
    public int getItemCount() {
        return itemCount;
    }

    /**
     * Sets the item count.
     *
     * @param itemCount the item count
     */
    public void setItemCount(int itemCount) {
        this.itemCount = itemCount;
    }

    /**
     * Returns the order line total.
     *
     * @return he order line total
     */
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
