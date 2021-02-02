/*
 * Copyright (c) 2000-2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.tutorials.graphql.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * A class representing a Customer.
 *
 * @author Tim Middleton 2021-01-25
 */
public class Customer
        implements Serializable {

    /**
     * Customer id.
     */
    private int customerId;

    /**
     * Name.
     */
    private String name;

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

    /**
     * Returns the customer's balance.
     *
     * @return the customer's balance
     */
    public double getBalance() {
        return balance;
    }

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
