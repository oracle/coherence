/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.io.pof.generator.data;

import com.tangosol.io.Evolvable;
import com.tangosol.io.SimpleEvolvable;
import com.tangosol.io.pof.EvolvableHolder;
import com.tangosol.io.pof.schema.annotation.Portable;
import com.tangosol.io.pof.schema.annotation.PortableType;

/**
 * Class for test case for Bug 35038656.
 *
 * @author tam / lsho 2023.02.03
 */
@PortableType(version = 1)
public class Simple
    {
    @Portable
    protected String name;

    @Portable(since = 1)
    protected int age;

    public Simple(String name, int age)
        {
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
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }
        if (!(o instanceof Simple))
            {
            return false;
            }

        Simple simple = (Simple) o;

        return age == simple.age
               && name.equals(simple.name);
        }

    @Override
    public int hashCode()
        {
        int result = name.hashCode();
        result = 31 * result + age;
        return result;
        }

    @Override
    public String toString()
        {
        return "Simple{" +
               "name='" + name + '\'' +
               ", age=" + age +
               '}';
        }
    }
