/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.guides.streams;

import java.io.Serializable;

import java.time.LocalDate;
import java.time.Period;

import java.util.Objects;


/**
 * Class to represent contact details.
 *
 * @author Tim Middleton 2022.02.16
 */
// tag::class[]
public class Contact
        implements Serializable {

    private int id;
    private String    firstName;
    private String    lastName;
    private LocalDate doB;
    private int       age;
    private Address   homeAddress;
    private Address   workAddress;
    // end::class[]

    public Contact(int id, String firstName, String lastName, LocalDate doB, Address homeAddress, Address workAddress) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.doB = doB;
        this.homeAddress = homeAddress;
        this.workAddress = workAddress;
        calculateAge();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public LocalDate getDoB() {
        return doB;
    }

    public void setDoB(LocalDate doB) {
        this.doB = doB;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public Address getHomeAddress() {
        return homeAddress;
    }

    public void setHomeAddress(Address homeAddress) {
        this.homeAddress = homeAddress;
    }

    public Address getWorkAddress() {
        return workAddress;
    }

    public void setWorkAddress(Address workAddress) {
        this.workAddress = workAddress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Contact contact = (Contact) o;

        if (id != contact.id) return false;
        if (age != contact.age) return false;
        if (!Objects.equals(firstName, contact.firstName)) return false;
        if (!Objects.equals(lastName, contact.lastName)) return false;
        if (!Objects.equals(doB, contact.doB)) return false;
        if (!Objects.equals(homeAddress, contact.homeAddress)) return false;
        return Objects.equals(workAddress, contact.workAddress);
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (firstName != null ? firstName.hashCode() : 0);
        result = 31 * result + (lastName != null ? lastName.hashCode() : 0);
        result = 31 * result + (doB != null ? doB.hashCode() : 0);
        result = 31 * result + age;
        result = 31 * result + (homeAddress != null ? homeAddress.hashCode() : 0);
        result = 31 * result + (workAddress != null ? workAddress.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Contact{" +
               "id=" + id +
               ", firstName='" + firstName + '\'' +
               ", lastName='" + lastName + '\'' +
               ", doB=" + doB +
               ", age=" + age +
               ", homeAddress=" + homeAddress +
               ", workAddress=" + workAddress +
               '}';
    }

    /**
     * Calculate and set the age based upon date of birth.
     */
    public void calculateAge()
        {
        age = Period.between(doB, LocalDate.now()).getYears();
        }
}
