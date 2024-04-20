/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package data.evolvable.v2;

import com.tangosol.io.Evolvable;
import com.tangosol.io.SimpleEvolvable;

import com.tangosol.io.pof.EvolvableHolder;
import com.tangosol.io.pof.EvolvableObject;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.schema.annotation.PortableType;

import java.io.IOException;

@PortableType(id = 3, version = 1)
public class Animal
        implements EvolvableObject
    {
    private final transient Evolvable evolvable = new SimpleEvolvable(1);
    private final transient EvolvableHolder m_evolvableHolder = new EvolvableHolder();

    protected String species;

    public Animal(String species)
        {
        this.species = species;
        }

    public Animal(PofReader reader) throws IOException
        {
        PofReader in = reader.createNestedPofReader(3);

        PofReader v1 = in.version(1);
        species = v1.readString(0);

        readEvolvable(in);
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
    public void writeExternal(PofWriter out)
            throws IOException
        {
        if (out.getUserTypeId() == 3)
            {
            out.writeString(0, species);
            }
        }

    @Override
    public Evolvable getEvolvable(int nTypeId)
        {
        if (nTypeId == 3)
            {
            return evolvable;
            }
        return m_evolvableHolder.get(nTypeId);
        }

    @Override
    public EvolvableHolder getEvolvableHolder()
        {
        return m_evolvableHolder;
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
        return "Animal.v1{" +
               "species='" + species + '\'' +
               '}';
        }
    }
