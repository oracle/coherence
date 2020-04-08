/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package data.json;

import java.util.Objects;

public class Person
    {
    public String name;
    public int age;
    public boolean minor;

    @SuppressWarnings("unused")
    public Person()
        {
        }

    public Person(String name, int age, boolean minor)
        {
        this.name = name;
        this.age = age;
        this.minor = minor;
        }

    public int getAge()
        {
        return age;
        }

    @Override
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }
        if (o == null || getClass() != o.getClass())
            {
            return false;
            }
        Person person = (Person) o;
        return age == person.age &&
                minor == person.minor &&
                Objects.equals(name, person.name);
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(name, age, minor);
        }

    @Override
    public String toString()
        {
        return "Person{" +
                "name='" + name + '\'' +
                ", age=" + age +
                ", minor=" + minor +
                '}';
        }
    }
