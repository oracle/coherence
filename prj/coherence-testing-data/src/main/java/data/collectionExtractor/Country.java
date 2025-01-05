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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * A class representing a Country.
 *
 * @author Gunnar Hillert  2024.09.10
 */
public class Country implements Serializable, PortableObject, ExternalizableLite
    {
    private String name;

    private int area;
    private int population;

    private Collection<City> cities = new ArrayList<>();

    public Country()
        {
        }

    public Country(String name)
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

    public int getArea()
        {
        return area;
        }

    public void setArea(int area)
        {
        this.area = area;
        }

    public int getPopulation()
        {
        return population;
        }

    public void setPopulation(int population)
        {
        this.population = population;
        }

    public Collection<City> getCities()
        {
        return cities;
        }

    public void setCities(Collection<City> cities)
        {
        this.cities = cities;
        }

    @Override
    public boolean equals(Object o)
        {
        if (this == o) return true;
        if (!(o instanceof Country country)) return false;
        return area == country.area && population == country.population && Objects.equals(name, country.name) && Objects.equals(cities, country.cities);
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(name, area, population, cities);
        }

    public String toString()
        {
        return "Country{" +
               "name='" + name + '\'' +
               ", area='" + area + '\'' +
               ", population=" + population +
               ", cities=" + cities.size() +
               '}';
        }

    public void readExternal(PofReader in) throws IOException
        {
        name       = in.readString(0);
        area       = in.readInt(1);
        population = in.readInt(2);
        cities     = in.readCollection(3, new ArrayList());
        }

    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeString(0, name);
        out.writeInt(1, area);
        out.writeInt(2, population);
        out.writeObject(3, cities);
        }

    public Country addCity(City city)
        {
        getCities().add(city);
        return this;
        }
    }
