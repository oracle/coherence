/*
 * Copyright (c) 2012, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package data.evolvable.v3;

import com.tangosol.io.pof.schema.annotation.Portable;
import com.tangosol.io.pof.schema.annotation.PortableType;

@PortableType(id = 3, version = 1)
public class Animal
    {
    @Portable(since = 1)
    protected String species;

    public Animal(String species)
        {
        this.species = species;
        }

    public String getSpecies()
        {
        return species;
        }

    public void setSpecies(String species)
        {
        this.species = species;
        }

    @Override
    public boolean equals(Object o)
        {
        return this == o || getClass().equals(o.getClass()) && matches(o);
        }

    public boolean matches(Object o)
        {
        if (o instanceof Animal)
            {
            Animal animal = (Animal) o;
            return species.equals(animal.species);
            }
        return false;
        }

    @Override
    public int hashCode()
        {
        return species.hashCode();
        }

    @Override
    public String toString()
        {
        return "Animal.v3{" +
                "species='" + species + '\'' +
                '}';
        }
    }
