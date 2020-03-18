/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package data.evolvable.v1;

import com.tangosol.io.Evolvable;

import com.tangosol.io.pof.EvolvableHolder;
import com.tangosol.io.pof.EvolvableObject;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.io.SimpleEvolvable;

import com.tangosol.io.pof.schema.annotation.PortableType;

import java.io.IOException;

@PortableType(id = 1, version = 1)
public class Pet
        implements PortableObject, EvolvableObject
    {
    private Evolvable evolvable = new SimpleEvolvable(1);
    private EvolvableHolder m_evolvableHolder = new EvolvableHolder();

    protected String name;

    public Pet()
        {
        }

    public Pet(String name)
        {
        this.name = name;
        }

    public String getName()
        {
        return name;
        }

    public void setName(String name)
        {
        this.name = name;
        }

    @Override
    public void readExternal(PofReader in)
            throws IOException
        {
        if (in.getUserTypeId() == 1)
            {
            name = in.readString(0);
            }
        }

    @Override
    public void writeExternal(PofWriter out)
            throws IOException
        {
        if (out.getUserTypeId() == 1)
            {
            out.writeString(0, name);
            }
        }

    @Override
    public Evolvable getEvolvable(int nTypeId)
        {
        if (nTypeId == 1)
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
        if (this == o)
            {
            return true;
            }
        if (!(o instanceof Pet))
            {
            return false;
            }

        Pet pet = (Pet) o;
        return name.equals(pet.name);
        }

    @Override
    public int hashCode()
        {
        return name.hashCode();
        }

    @Override
    public String toString()
        {
        return "Pet.v1{" +
               "name='" + name + '\'' +
               '}';
        }
    }
