/*
 * Copyright (c) 2000-2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.guides.cachestores;

import java.io.Serializable;
import java.util.Objects;

/**
 * A class to represent a customer.
 *
 * @author Tim Middleton 2020.02.19
 */
public class Customer implements Serializable {

    private int id;
    private String name;
    private String address;
    private int creditLimit;

    public Customer() {
    }

    public Customer(int id, String name, String address, int creditLimit) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.creditLimit = creditLimit;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public int getCreditLimit() {
        return creditLimit;
    }

    public void setCreditLimit(int creditLimit) {
        this.creditLimit = creditLimit;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Customer customer = (Customer) o;
        return id == customer.id && creditLimit == customer.creditLimit && Objects.equals(name, customer.name) &&
               Objects.equals(address, customer.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, address, creditLimit);
    }

    @Override
    public String toString() {
        return "Customer{" +
               "id=" + id +
               ", name='" + name + '\'' +
               ", address='" + address + '\'' +
               ", creditLimit=" + creditLimit +
               '}';
    }
}
