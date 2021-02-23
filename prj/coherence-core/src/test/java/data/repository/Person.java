/*
 * Copyright (c) 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package data.repository;

import com.oracle.coherence.repository.Indexed;

import com.tangosol.util.comparator.InverseComparator;

import data.pof.Address;
import java.io.Serializable;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * @author Aleks Seovic  2021.02.12
 */
public class Person implements Serializable
    {
    private String ssn;

    private String name;

    private int age;

    private long dateOfBirth;

    private Gender gender;

    private double weight;

    private BigDecimal salary;

    private Address address;

    public Person()
        {
        }

    public Person(String ssn)
        {
        this.ssn = ssn;
        }

    public String getSsn()
        {
        return ssn;
        }

    @Indexed
    public String getName()
        {
        return name;
        }

    public void setName(String name)
        {
        this.name = name;
        }
    public Person name(String name)
        {
        setName(name);
        return this;
        }

    @Indexed(ordered = true, comparator = InverseComparator.class)
    public int getAge()
        {
        return age;
        }

    public void setAge(int age)
        {
        this.age = age;
        }
    public Person age(int age)
        {
        setAge(age);
        return this;
        }

    public long getDateOfBirth()
        {
        return dateOfBirth;
        }

    public void setDateOfBirth(long dateOfBirth)
        {
        this.dateOfBirth = dateOfBirth;
        }
    public Person dateOfBirth(long dateOfBirth)
        {
        setDateOfBirth(dateOfBirth);
        return this;
        }

    public Gender getGender()
        {
        return gender;
        }

    public void setGender(Gender gender)
        {
        this.gender = gender;
        }
    public Person gender(Gender gender)
        {
        setGender(gender);
        return this;
        }

    public double getWeight()
        {
        return weight;
        }

    public void setWeight(double weight)
        {
        this.weight = weight;
        }
    public Person weight(double weight)
        {
        setWeight(weight);
        return this;
        }

    public BigDecimal getSalary()
        {
        return salary;
        }

    public void setSalary(BigDecimal salary)
        {
        this.salary = salary;
        }
    public Person salary(BigDecimal salary)
        {
        setSalary(salary);
        return this;
        }

    public Address getAddress()
        {
        return address;
        }

    public void setAddress(Address address)
        {
        this.address = address;
        }
    public Person address(Address address)
        {
        setAddress(address);
        return this;
        }

    public boolean isAdult()
        {
        return age >= 18;
        }

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
               dateOfBirth == person.dateOfBirth &&
               Double.compare(person.weight, weight) == 0 &&
               ssn.equals(person.ssn) &&
               name.equals(person.name) &&
               gender == person.gender &&
               Objects.equals(salary, person.salary) &&
               Objects.equals(address, person.address);
        }

    public int hashCode()
        {
        return Objects.hash(ssn, name, age, dateOfBirth, gender, weight, salary, address);
        }

    public String toString()
        {
        return "Person{" +
               "ssn='" + ssn + '\'' +
               ", name='" + name + '\'' +
               ", age=" + age +
               ", dateOfBirth=" + dateOfBirth +
               ", gender=" + gender +
               ", weight=" + weight +
               ", salary=" + salary +
               ", address=" + address +
               ", adult=" + isAdult() +
               '}';
        }
    }

