/*
 * Copyright (c) 2012, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package data.evolvable.v3;

import com.tangosol.io.pof.schema.annotation.Portable;
import com.tangosol.io.pof.schema.annotation.PortableType;

@PortableType(id = 1, version = 2)
public class Pet
        extends Animal
    {
    @Portable(since = 1)
    protected String name;

    @Portable(since = 2)
    protected int age;

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
    public boolean matches(Object o)
        {
        if (o instanceof Pet)
            {
            Pet pet = (Pet) o;
            return super.matches(pet) && age == pet.age && name.equals(pet.name);
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
        return "Pet.v3{" +
                "name='" + name + '\'' +
                ", age=" + age +
                ", species=" + species +
                '}';
        }
    }
