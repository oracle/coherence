/*
 * Copyright (c) 2012, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package data.evolvable.v3;

import com.tangosol.io.pof.schema.annotation.Portable;
import com.tangosol.io.pof.schema.annotation.PortableSet;
import com.tangosol.io.pof.schema.annotation.PortableType;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@SuppressWarnings("unchecked")
@PortableType(id = 4, version = 1)
public class Zoo
    {
    @Portable(since = 1)
    private Address address;

    @PortableSet(since = 1)
    private Set<Animal> animals = new HashSet<>();

    public Zoo()
        {
        }

    public Zoo(Address address)
        {
        this.address = address;
        }

    public Address getAddress()
        {
        return address;
        }

    public Set<Animal> getAnimals()
        {
        return animals;
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

        Zoo zoo = (Zoo) o;

        return Objects.equals(address, zoo.address) &&
               Objects.equals(animals, zoo.animals);
        }

    @Override
    public int hashCode()
        {
        int result = address != null ? address.hashCode() : 0;
        result = 31 * result + (animals != null ? animals.hashCode() : 0);
        return result;
        }

    @Override
    public String toString()
        {
        return "Zoo{" +
                "address=" + address +
                ", animals=" + animals +
                '}';
        }
    }
