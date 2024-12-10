/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package data.evolvable.v2;

import com.tangosol.io.Evolvable;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.SimpleEvolvable;

import com.tangosol.io.pof.schema.annotation.PortableType;

import data.evolvable.Color;

import java.io.IOException;

@PortableType(id = 2, version = 2)
public class Dog
        extends Pet
    {
    private final transient Evolvable evolvable = new SimpleEvolvable(2);

    private String breed;
    private Color color;

    public Dog(String name, int age, String breed, Color color)
        {
        super("Canis lupus familiaris", name, age);
        this.breed = breed;
        this.color = color;
        }

    public Dog(PofReader reader) throws IOException
        {
        super(reader);

        PofReader in = reader.createNestedPofReader(2);

        PofReader v1 = in.version(1);
        breed = v1.readString(0);

        PofReader v2 = in.version(2);
        color = v2.readObject(1);

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

    public Color getColor()
        {
        return color;
        }

    public void setColor(Color color)
        {
        this.color = color;
        }

    @Override
    public void writeExternal(PofWriter out)
            throws IOException
        {
        if (out.getUserTypeId() == 2)
            {
            out.writeString(0, breed);
            out.writeObject(1, color);
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
    public boolean matches(Object o)
        {
        if (o instanceof Dog)
            {
            Dog dog = (Dog) o;
            return super.matches(dog) && breed.equals(dog.breed)
                   && color.equals(dog.color);
            }
        return false;
        }

    @Override
    public int hashCode()
        {
        int result = super.hashCode();
        result = 31 * result + breed.hashCode();
        result = 31 * result + color.hashCode();
        return result;
        }

    @Override
    public String toString()
        {
        return "Dog.v2{" +
               "name='" + name + '\'' +
               ", age=" + age +
               ", species='" + species + '\'' +
               ", breed='" + breed + '\'' +
               ", color='" + color + '\'' +
               '}';
        }
    }
