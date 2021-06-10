/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.guides.aggregations.model;

import java.io.Serializable;

import java.math.BigDecimal;

import java.util.Objects;

/**
 * Class to represent a customer.
 *
 * @author Tim Middleton 2021-02-25
 */
public class Customer
        implements Serializable {

    private int id;
    private String customerName;
    private Address officeAddress;
    private Address postalAddress;
    private BigDecimal outstandingBalance;

    public Customer(int id, String customerName, Address officeAddress, Address postalAddress, BigDecimal outstandingBalance) {
        this.id = id;
        this.customerName = customerName;
        this.officeAddress = officeAddress;
        this.postalAddress = postalAddress;
        this.outstandingBalance = outstandingBalance;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public Address getOfficeAddress() {
        return officeAddress;
    }

    public void setOfficeAddress(Address officeAddress) {
        this.officeAddress = officeAddress;
    }

    public Address getPostalAddress() {
        return postalAddress;
    }

    public void setPostalAddress(Address postalAddress) {
        this.postalAddress = postalAddress;
    }

    public BigDecimal getOutstandingBalance() {
        return outstandingBalance;
    }

    public void setOutstandingBalance(BigDecimal outstandingBalance) {
        this.outstandingBalance = outstandingBalance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Customer customer = (Customer) o;
        return id == customer.id && Objects.equals(customerName, customer.customerName) &&
               Objects.equals(officeAddress, customer.officeAddress) &&
               Objects.equals(postalAddress, customer.postalAddress) &&
               Objects.equals(outstandingBalance, customer.outstandingBalance);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, customerName, officeAddress, postalAddress, outstandingBalance);
    }

    @Override
    public String toString() {
        return "Customer{" +
               "id=" + id +
               ", customerName='" + customerName + '\'' +
               ", officeAddress=" + officeAddress +
               ", postalAddress=" + postalAddress +
               ", outstandingBalance=" + outstandingBalance +
               '}';
    }
}
