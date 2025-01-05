/*
 * Copyright (c) 2024, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package data.collectionExtractor;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import java.io.IOException;
import java.io.Serializable;

import java.util.Objects;

/**
 * A class representing a City.
 *
 * @author Gunnar Hillert  2024.09.10
 */
public class City implements Serializable, PortableObject, ExternalizableLite
    {
    private String name;
    private int population;

    public City()
        {
        }

    public City(String name, int population)
        {
        this.name = name;
        this.population = population;
        }

    public String getName()
        {
        return name;
        }
    public void setName(String name)
        {
        this.name = name;
        }

    public int getPopulation()
        {
        return population;
        }

    public void setPopulation(int population)
        {
        this.population = population;
        }

    @Override
    public boolean equals(Object o)
        {
        if (this == o) return true;
        if (!(o instanceof City city)) return false;
        return population == city.population && Objects.equals(name, city.name);
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(name, population);
        }

    public String toString()
        {
        return "Country{" +
               "name='" + name + '\'' +
               ", population=" + population +
               '}';
        }

    public void readExternal(PofReader in) throws IOException
        {
        name       = in.readString(0);
        population = in.readInt(1);
        }

    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeString(0, name);
        out.writeInt(   1, population);
        }
    }
