/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.guides.notifications.model;

import com.tangosol.io.pof.schema.annotation.PortableType;

/**
 * A simple representation of a customer.
 *
 * @author Jonathan Knight 2022.06.20
 * @since 22.06
 */
// # tag::src[]
@PortableType(id = 1001, version = 1)
public class Customer {

    /**
     * The customer's identifier.
     */
    private String id;

    /**
     * The customer's first name.
     */
    private String firstName;

    /**
     * The customer's last name.
     */
    private String lastName;

    /**
     * Create a customer.
     *
     * @param id         the customer's identifier
     * @param firstName  the customer's first name
     * @param lastName   the customer's last name
     */
    public Customer(String id, String firstName, String lastName) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    /**
     * Returns the customer's identifier.
     *
     * @return the customer's identifier
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the customer's first name.
     *
     * @return the customer's first name
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Set the customer's first name.
     *
     * @param firstName  the customer's first name
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * Returns the customer's last name.
     *
     * @return the customer's last name
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Set the customer's last name.
     *
     * @param lastName  the customer's last name
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
}
// # end::src[]
