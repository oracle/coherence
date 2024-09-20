/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package data.evolvable.v1;

import com.tangosol.io.Evolvable;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.SimpleEvolvable;

import com.tangosol.io.pof.schema.annotation.PortableType;

import java.io.IOException;

@PortableType(id = 2, version = 1)
public class Dog
        extends Pet
    {
    private final transient Evolvable evolvable = new SimpleEvolvable(1);

    protected String breed;

    public Dog(String name, String breed)
        {
        super(name);
        this.breed = breed;
        }

    public Dog(PofReader reader) throws IOException
        {
        super(reader);

        PofReader in = reader.createNestedPofReader(2);

        PofReader v1 = in.version(1);
        breed = v1.readString(0);

        readEvolvable(in);
        }

    public String getBreed()
        {
        return breed;
        }

    public void setBreed(String breed)
        {
        this.breed = breed;
        }

    @Override
    public void writeExternal(PofWriter out)
            throws IOException
        {
        if (out.getUserTypeId() == 2)
            {
            out.writeString(0, breed);
            }
        else
            {
            super.writeExternal(out);
            }
        }

    @Override
    public Evolvable getEvolvable(int nTypeId)
        {
        if (nTypeId == 2)
            {
            return evolvable;
            }
        return super.getEvolvable(nTypeId);
        }

    @Override
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }
        if (!(o instanceof Dog))
            {
            return false;
            }

        Dog dog = (Dog) o;
        return super.equals(dog) && breed.equals(dog.breed);
        }

    @Override
    public int hashCode()
        {
        int result = super.hashCode();
        result = 31 * result + breed.hashCode();
        return result;
        }

    @Override
    public String toString()
        {
        return "Dog.v1{" +
               "name='" + name + '\'' +
               ", breed='" + breed + '\'' +
               '}';
        }
    }
