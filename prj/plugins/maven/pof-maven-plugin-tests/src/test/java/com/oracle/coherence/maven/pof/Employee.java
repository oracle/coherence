/*
 * Copyright (c) 2013, 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.maven.pof;

import com.tangosol.io.pof.schema.annotation.PortableType;

/**
 * @author Aleksandar Seovic  2013.07.22
 */
@PortableType(id = 3)
public class Employee extends Person
    {
    private String department;

    public Employee(String firstName, String lastName, int age, String department)
        {
        super(firstName, lastName, age);

        this.department = department;
        }

    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }
        if (!(o instanceof Employee))
            {
            return false;
            }

        Employee employee = (Employee) o;
        return super.equals(o) && department.equals(employee.department);
        }

    public int hashCode()
        {
        int result = super.hashCode();
        result = 31 * result + department.hashCode();
        return result;
        }

    public String toString()
        {
        return "Employee{" +
               "firstName='" + firstName + '\'' +
               ", lastName='" + lastName + '\'' +
               ", age=" + age +
               ", address=" + address +
               ", department='" + department + '\'' +
               '}';
        }
    }
