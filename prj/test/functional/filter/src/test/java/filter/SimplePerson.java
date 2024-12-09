package filter;

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

 * @author Maurice Gamanho  2024.12.05
 */
public class SimplePerson implements Serializable, PortableObject
    {
    private String ssn;
    private String name;
    private int age;
    private LocalDate dateOfBirth;
    private long height;

    private double weight;
    private BigDecimal salary;
    private Address address;

    public SimplePerson()
        {
        }

    public SimplePerson(String ssn)
        {
        this.ssn = ssn;
        }

    public String getSsn()
        {
        return ssn;
        }

    public String getName()
        {
        return name;
        }

    public void setName(String name)
        {
        this.name = name;
        }
    public SimplePerson name(String name)
        {
        setName(name);
        return this;
        }

    public int getAge()
        {
        return age;
        }

    public void setAge(int age)
        {
        this.age = age;
        }
    public SimplePerson age(int age)
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
    public SimplePerson dateOfBirth(LocalDate dateOfBirth)
        {
        setDateOfBirth(dateOfBirth);
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
    public SimplePerson height(long height)
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
    public SimplePerson weight(double weight)
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
    public SimplePerson salary(BigDecimal salary)
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
    public SimplePerson address(Address address)
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
        SimplePerson person = (SimplePerson) o;
        return age == person.age &&
               height == person.height &&
               Double.compare(person.weight, weight) == 0 &&
               ssn.equals(person.ssn) &&
               name.equals(person.name) &&
               Objects.equals(dateOfBirth, person.dateOfBirth) &&
               Objects.equals(salary, person.salary) &&
               Objects.equals(address, person.address);
        }

    public int hashCode()
        {
        return Objects.hash(ssn, name, age, dateOfBirth, weight, salary, address);
        }

    public String toString()
        {
        return "Person{" +
               "ssn='" + ssn + '\'' +
               ", name='" + name + '\'' +
               ", age=" + age +
               ", dateOfBirth=" + dateOfBirth +
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
        height      = in.readLong(4);
        weight      = in.readDouble(5);
        salary      = in.readBigDecimal(6);
        address     = in.readObject(7);
        }

    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeString(0, ssn);
        out.writeString(1, name);
        out.writeInt(   2, age);
        out.writeDate(  3, dateOfBirth);
        out.writeLong(  4, height);
        out.writeDouble(5, weight);
        out.writeBigDecimal(6, salary);
        out.writeObject(7, address);
        }
    }

