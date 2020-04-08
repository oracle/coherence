/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package data.evolvable.v2;

import com.tangosol.io.Evolvable;

import com.tangosol.io.pof.EvolvableHolder;
import com.tangosol.io.pof.EvolvableObject;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.io.SimpleEvolvable;

import com.tangosol.io.pof.schema.annotation.PortableType;

import java.io.IOException;

@PortableType(id = 3, version = 1)
public class Animal
        implements PortableObject, EvolvableObject
    {
    private Evolvable evolvable;
    private EvolvableHolder m_evolvableHolder;

    protected String species;

    public Animal()
        {
        }

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
    public void readExternal(PofReader in)
            throws IOException
        {
        if (in.getUserTypeId() == 3)
            {
            species = in.readString(0);
            }
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
        if (evolvable == null)
            {
            evolvable = new SimpleEvolvable(1);
            }
        if (nTypeId == 3)
            {
            return evolvable;
            }
        return m_evolvableHolder.get(nTypeId);
        }

    @Override
    public EvolvableHolder getEvolvableHolder()
        {
        if (m_evolvableHolder == null)
            {
            m_evolvableHolder = new EvolvableHolder();
            }
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
