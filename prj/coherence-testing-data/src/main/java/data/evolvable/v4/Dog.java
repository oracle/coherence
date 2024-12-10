/*
 * Copyright (c) 2012, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package data.evolvable.v4;

import com.tangosol.io.pof.schema.annotation.Portable;
import com.tangosol.io.pof.schema.annotation.PortableType;

import data.evolvable.Color;
import data.evolvable.v3.Pet;

@PortableType(id = 2, version = 2)
public class Dog extends Pet
    {
    @Portable(since = 1)
    private String breed;

    @Portable(since = 2)
    private Color color;

    public Dog(String name, int age, String breed, Color color)
        {
        super("Canis lupus familiaris", name, age);
        this.breed = breed;
        this.color = color;
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
    public boolean matches(Object o)
        {
        if (o instanceof data.evolvable.v4.Dog)
            {
            data.evolvable.v4.Dog dog = (data.evolvable.v4.Dog) o;
            return super.matches(dog) && breed.equals(dog.breed) && color.equals(dog.color);
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
        return "Dog.v4{" +
                "name='" + name + '\'' +
                ", age=" + age +
                ", species='" + species + '\'' +
                ", breed='" + breed + '\'' +
                ", color='" + color + '\'' +
                '}';
        }
    }
