/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.guides.aggregations.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Class to represent an Australian address.
 *
 * @author Tim Middleton 2021-02-25
 */
public class Address
        implements Serializable {

    private String addressLine1;
    private String addressLine2;
    private String suburb;
    private String city;
    private String state;
    private int postCode;

    public Address() {
    }

    public Address(String addressLine1, String addressLine2, String suburb, String city, String state, int postCode) {
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.suburb = suburb;
        this.city = city;
        this.state = state;
        this.postCode = postCode;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public int getPostCode() {
        return postCode;
    }

    public void setPostCode(int postCode) {
        this.postCode = postCode;
    }

    public String getSuburb() {
        return suburb;
    }

    public void setSuburb(String suburb) {
        this.suburb = suburb;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Address address = (Address) o;
        return postCode == address.postCode && Objects.equals(addressLine1, address.addressLine1) &&
               Objects.equals(addressLine2, address.addressLine2) && Objects.equals(suburb, address.suburb) &&
               Objects.equals(city, address.city) && Objects.equals(state, address.state);
    }

    @Override
    public int hashCode() {
        return Objects.hash(addressLine1, addressLine2, suburb, city, state, postCode);
    }

    @Override
    public String toString() {
        return "Address{" +
               "addressLine1='" + addressLine1 + '\'' +
               ", addressLine2='" + addressLine2 + '\'' +
               ", suburb='" + suburb + '\'' +
               ", city='" + city + '\'' +
               ", state='" + state + '\'' +
               ", postCode=" + postCode +
               '}';
    }
}
