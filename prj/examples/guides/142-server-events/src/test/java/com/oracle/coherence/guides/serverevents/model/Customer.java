/*
 * Copyright (c) 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.guides.serverevents.model;

import java.io.Serializable;

import java.util.Objects;

/**
 * A class to represent a customer.
 *
 * @author Tim Middleton 2021.04.30
 */
public class Customer
        implements Serializable {

    public static final String GOLD = "GOLD";
    public static final String SILVER = "SILVER";
    public static final String BRONZE = "BRONZE";

    // #tag::vars[]
    private int id;
    private String name;
    private String address;
    private String customerType;
    private long creditLimit;
    // #end::vars[]

    public Customer(int id, String name, String address, String customerType, long creditLimit) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.customerType = customerType;
        this.creditLimit = creditLimit;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCustomerType() {
        return customerType;
    }

    public void setCustomerType(String customerType) {
        this.customerType = customerType;
    }

    public long getCreditLimit() {
        return creditLimit;
    }

    public void setCreditLimit(long creditLimit) {
        this.creditLimit = creditLimit;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Customer customer = (Customer) o;
        return id == customer.id && Objects.equals(name, customer.name) &&
               Objects.equals(address, customer.address) && Objects.equals(customerType, customer.customerType) &&
               Objects.equals(creditLimit, customer.creditLimit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, address, customerType, creditLimit);
    }

    @Override
    public String toString() {
        return "Customer{" +
               "id=" + id +
               ", name='" + name + '\'' +
               ", address='" + address + '\'' +
               ", customerType='" + customerType + '\'' +
               ", balance=" + creditLimit +
               '}';
    }
}
