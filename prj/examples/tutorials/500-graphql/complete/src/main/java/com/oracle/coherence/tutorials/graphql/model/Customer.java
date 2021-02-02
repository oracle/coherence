/*
 * Copyright (c) 2000-2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.tutorials.graphql.model;


import com.oracle.coherence.inject.Injectable;
import com.tangosol.net.NamedMap;
import com.tangosol.util.Filters;

import java.io.Serializable;
import java.util.Collection;
import java.util.Objects;

import javax.inject.Inject;

import org.eclipse.microprofile.graphql.NonNull;
import org.eclipse.microprofile.graphql.NumberFormat;


/**
 * A class representing a Customer.
 *
 * @author Tim Middleton 2021-01-25
 */
// tag::injectable[]
public class Customer
        implements Serializable, Injectable {
    // end::injectable[]

    // tag::namedMap[]
    /**
     * The {@link NamedMap} for orders.
     */
    @Inject
    private transient NamedMap<Integer, Order> orders;
    // end::namedMap[]

    /**
     * Customer id.
     */
    private int customerId;

    // tag::nonNull[]
    /**
     * Name.
     */
    @NonNull
    private String name;
    // end::nonNull[]

    /**
     * Email address.
     */
    private String email;

    /**
     * Address.
     */
    private String address;

    /**
     * Balance.
     */
    private double balance;

    /**
     * No-args constructor.
     */
    public Customer() {
    }

    /**
     * Constructs a {@link Customer}.
     *
     * @param nCustomerId customer id
     * @param sName       name
     * @param sEmail      email address
     * @param sAddress    address
     * @param nBalance    balance
     */
    public Customer(int nCustomerId, String sName, String sEmail, String sAddress, double nBalance) {
        this.customerId = nCustomerId;
        this.name = sName;
        this.email = sEmail;
        this.address = sAddress;
        this.balance = nBalance;
    }

    // tag::getOrders[]
    /**
     * Returns the {@link Order}s for a {@link Customer}.
     *
     * @return the {@link Order}s for a {@link Customer}
     */
    public Collection<Order> getOrders() {
        return orders.values(Filters.equal(Order::getCustomerId, customerId));
    }
    // end::getOrders[]

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
     * @param nCustomerId the customer id
     */
    public void setCustomerId(int nCustomerId) {
        this.customerId = nCustomerId;
    }

    /**
     * Returns the customer name.
     *
     * @return the customer name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the customer name.
     *
     * @param sName the customer name
     */
    public void setName(String sName) {
        this.name = sName;
    }

    /**
     * Returns the email address.
     *
     * @return the email address
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the email address.
     *
     * @param sEmail the email address
     */
    public void setEmail(String sEmail) {
        this.email = sEmail;
    }

    /**
     * Returns the address.
     *
     * @return the address
     */
    public String getAddress() {
        return address;
    }

    /**
     * Sets the address.
     *
     * @param sAddress the address
     */
    public void setAddress(String sAddress) {
        this.address = sAddress;
    }

    // tag::numberFormat[]

    /**
     * Returns the customer's balance.
     *
     * @return the customer's balance
     */
    @NumberFormat("$###,##0.00")
    public double getBalance() {
        return balance;
    }
    // end::numberFormat[]

    /**
     * Sets the customer's balance.
     *
     * @param nBalance the customer's balance
     */
    public void setBalance(double nBalance) {
        this.balance = nBalance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Customer customer = (Customer) o;
        return customerId == customer.customerId &&
               Double.compare(customer.balance, balance) == 0 &&
               Objects.equals(name, customer.name) &&
               Objects.equals(email, customer.email) &&
               Objects.equals(address, customer.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(customerId, name, email, address, balance);
    }

    @Override
    public String toString() {
        return "Customer{" +
               "customerId=" + customerId +
               ", name='" + name + '\'' +
               ", email='" + email + '\'' +
               ", address='" + address + '\'' +
               ", balance=" + balance +
               '}';
    }
}
