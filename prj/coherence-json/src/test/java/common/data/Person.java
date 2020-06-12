/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package common.data;

import java.util.Objects;
import javax.json.bind.annotation.JsonbProperty;


public class Person
    {
    // ----- constructors ---------------------------------------------------

    @SuppressWarnings("unused")
    public Person()
        {
        }

    public Person(String sName, int nAge, boolean fMinor)
        {
        this.m_sName = sName;
        this.n_nAge = nAge;
        this.m_fMinor = fMinor;
        }

    // ----- public methods -------------------------------------------------

    public int getAge()
        {
        return n_nAge;
        }

    // ----- Object methods -------------------------------------------------

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
        return n_nAge == person.n_nAge &&
               m_fMinor == person.m_fMinor &&
               Objects.equals(m_sName, person.m_sName);
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(m_sName, n_nAge, m_fMinor);
        }

    @Override
    public String toString()
        {
        return "Person{" +
               "name='" + m_sName + '\'' +
               ", age=" + n_nAge +
               ", minor=" + m_fMinor +
               '}';
        }

    // ----- data members ---------------------------------------------------

    @JsonbProperty("name")
    protected String m_sName;

    @JsonbProperty("age")
    protected int n_nAge;

    @JsonbProperty("minor")
    protected boolean m_fMinor;
    }
