/*
 * Copyright (c) 2001, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package data.repository;

import com.oracle.coherence.repository.Indexed;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import data.pof.Address;

import java.io.IOException;
import java.io.Serializable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * A class representing a Person entity.

 * @author Aleks Seovic  2021.02.12
 */
public class Person implements Serializable, PortableObject
    {
    private String ssn;
    private String name;
    private int age;
    private LocalDate dateOfBirth;
    private Gender gender;
    private long height;

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

    @Indexed(ordered = true)
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

    public LocalDate getDateOfBirth()
        {
        return dateOfBirth;
        }

    public void setDateOfBirth(LocalDate dateOfBirth)
        {
        this.dateOfBirth = dateOfBirth;
        }
    public Person dateOfBirth(LocalDate dateOfBirth)
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

    public long getHeight()
        {
        return height;
        }

    public void setHeight(long height)
        {
        this.height = height;
        }
    public Person height(long height)
        {
        setHeight(height);
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
               height == person.height &&
               Double.compare(person.weight, weight) == 0 &&
               ssn.equals(person.ssn) &&
               name.equals(person.name) &&
               gender == person.gender &&
               Objects.equals(dateOfBirth, person.dateOfBirth) &&
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
               ", height=" + height +
               ", weight=" + weight +
               ", salary=" + salary +
               ", address=" + address +
               ", adult=" + isAdult() +
               '}';
        }

    public void readExternal(PofReader in) throws IOException
        {
        ssn         = in.readString(0);
        name        = in.readString(1);
        age         = in.readInt(2);
        dateOfBirth = in.readLocalDate(3);
        gender      = in.readObject(4);
        height      = in.readLong(5);
        weight      = in.readDouble(6);
        salary      = in.readBigDecimal(7);
        address     = in.readObject(8);
        }

    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeString(0, ssn);
        out.writeString(1, name);
        out.writeInt(   2, age);
        out.writeDate(  3, dateOfBirth);
        out.writeObject(4, gender);
        out.writeLong(  5, height);
        out.writeDouble(6, weight);
        out.writeBigDecimal(7, salary);
        out.writeObject(8, address);
        }
    }

