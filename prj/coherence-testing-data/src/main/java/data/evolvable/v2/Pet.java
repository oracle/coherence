/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package data.evolvable.v2;

import com.tangosol.io.Evolvable;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.SimpleEvolvable;

import com.tangosol.io.pof.schema.annotation.PortableType;

import java.io.IOException;

@PortableType(id = 1, version = 2)
public class Pet
        extends Animal
    {
    private Evolvable evolvable = new SimpleEvolvable(2);

    protected String name;

    protected int age;

    public Pet()
        {
        }

    public Pet(String species, String name, int age)
        {
        super(species);
        this.name = name;
        this.age = age;
        }

    public String getName()
        {
        return name;
        }

    public void setName(String name)
        {
        this.name = name;
        }

    public int getAge()
        {
        return age;
        }

    public void setAge(int age)
        {
        this.age = age;
        }

    @Override
    public void readExternal(PofReader in)
            throws IOException
        {
        if (in.getUserTypeId() == 1)
            {
            name = in.readString(0);
            if (in.getVersionId() > 1)
                {
                age = in.readInt(1);
                }
            }
        else
            {
            super.readExternal(in);
            }
        }

    @Override
    public void writeExternal(PofWriter out)
            throws IOException
        {
        if (out.getUserTypeId() == 1)
            {
            out.writeString(0, name);
            out.writeInt(1, age);
            }
        else
            {
            super.writeExternal(out);
            }
        }

    @Override
    public Evolvable getEvolvable(int nTypeId)
        {
        if (nTypeId == 1)
            {
            return evolvable;
            }
        return super.getEvolvable(nTypeId);
        }

    @Override
    public boolean matches(Object o)
        {
        if (o instanceof Pet)
            {
            Pet pet = (Pet) o;
            return super.matches(pet) && age == pet.age && name.equals(
                    pet.name);
            }
        return false;
        }

    @Override
    public int hashCode()
        {
        int result = super.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + age;
        return result;
        }

    @Override
    public String toString()
        {
        return "Pet.v2{" +
               "name='" + name + '\'' +
               ", age=" + age +
               ", species=" + species +
               '}';
        }
    }
