/*
 * Copyright (c) 2013, 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.maven.pof;

import com.tangosol.io.pof.schema.annotation.Portable;
import com.tangosol.io.pof.schema.annotation.PortableType;

@PortableType(id = 1000)
public class Person
    {
    @Portable
    protected String firstName;

    @Portable
    protected String lastName;

    @Portable
    protected int age;

    @Portable
    protected Address address;

    public Person(String firstName, String lastName, int age)
        {
        this.firstName = firstName;
        this.lastName = lastName;
        this.age = age;
        }

    public String getFirstName()
        {
        return firstName;
        }

    public void setFirstName(String firstName)
        {
        this.firstName = firstName;
        }

    public String getLastName()
        {
        return lastName;
        }

    public void setLastName(String lastName)
        {
        this.lastName = lastName;
        }

    public int getAge()
        {
        return age;
        }

    public void setAge(int age)
        {
        this.age = age;
        }

    public Address getAddress()
        {
        return address;
        }

    public void setAddress(Address address)
        {
        this.address = address;
        }

    @Override
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }
        if (!(o instanceof Person))
            {
            return false;
            }

        Person person = (Person) o;

        return age == person.age
               && address.equals(person.address)
               && firstName.equals(person.firstName)
               && lastName.equals(person.lastName);
        }

    @Override
    public int hashCode()
        {
        int result = firstName.hashCode();
        result = 31 * result + lastName.hashCode();
        result = 31 * result + age;
        return result;
        }

    @Override
    public String toString()
        {
        return "Person{" +
               "firstName='" + firstName + '\'' +
               ", lastName='" + lastName + '\'' +
               ", age=" + age +
               ", address=" + address +
               '}';
        }

    @PortableType(id = 2)
    public static class Address
        {
        @Portable
        private String street;

        @Portable
        private String city;

        @Portable
        private String country;

        public Address(String street, String city, String country)
            {
            this.street = street;
            this.city = city;
            this.country = country;
            }

        public String getStreet()
            {
            return street;
            }

        public void setStreet(String street)
            {
            this.street = street;
            }

        public String getCity()
            {
            return city;
            }

        public void setCity(String city)
            {
            this.city = city;
            }

        public String getCountry()
            {
            return country;
            }

        public void setCountry(String country)
            {
            this.country = country;
            }

        @Override
        public boolean equals(Object o)
            {
            if (this == o)
                {
                return true;
                }
            if (o == null || getClass() != o.getClass())
                {
                return false;
                }

            Address address = (Address) o;

            return city.equals(address.city)
                   && country.equals(address.country)
                   && street.equals(address.street);

            }

        @Override
        public int hashCode()
            {
            int result = street.hashCode();
            result = 31 * result + city.hashCode();
            result = 31 * result + country.hashCode();
            return result;
            }

        @Override
        public String toString()
            {
            return "Address{" +
                   "street='" + street + '\'' +
                   ", city='" + city + '\'' +
                   ", country='" + country + '\'' +
                   '}';
            }
        }
    }
