/*
 * Copyright (c) 2012, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package data.evolvable.v3;

import com.tangosol.io.pof.schema.annotation.Portable;
import com.tangosol.io.pof.schema.annotation.PortableType;

import java.util.Objects;

@PortableType(id = 10, version = 1)
public class Address
    {
    @Portable(since = 1)
    private String street;
    @Portable(since = 1)
    private String city;
    @Portable(since = 1)
    private String state;

    public Address(String street, String city, String state)
        {
        this.street = street;
        this.city = city;
        this.state = state;
        }

    public String getStreet()
        {
        return street;
        }

    public String getCity()
        {
        return city;
        }

    public String getState()
        {
        return state;
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

        if (!Objects.equals(city, address.city))
            {
            return false;
            }
        if (!Objects.equals(state, address.state))
            {
            return false;
            }
        if (!Objects.equals(street, address.street))
            {
            return false;
            }

        return true;
        }

    @Override
    public int hashCode()
        {
        int result = street != null ? street.hashCode() : 0;
        result = 31 * result + (city != null ? city.hashCode() : 0);
        result = 31 * result + (state != null ? state.hashCode() : 0);
        return result;
        }

    @Override
    public String toString()
        {
        return "Address{" +
                "street='" + street + '\'' +
                ", city='" + city + '\'' +
                ", state='" + state + '\'' +
                '}';
        }
    }
