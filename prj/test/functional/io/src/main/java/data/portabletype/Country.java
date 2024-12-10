/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package data.portabletype;

import java.util.Objects;
import com.tangosol.io.pof.schema.annotation.PortableType;

/**
 * Test class for ConfigurablePofContextPortableTypeTest.
 *
 * @author tam  2020.08.21
 */
@PortableType(id = 1002)
public class Country
    {

    public Country()
        {
        }

    public Country(String sCode, String sDescription)
        {
        m_sCode = sCode;
        m_sDescription = sDescription;
        }

    public String getCode()
        {
        return m_sCode;
        }

    public void setCode(String code)
        {
        m_sCode = code;
        }

    public String getDescription()
        {
        return m_sDescription;
        }

    public void setDescription(String sDescription)
        {
        m_sDescription = sDescription;
        }

    @Override
    public boolean equals(Object o)
        {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Country country = (Country) o;
        return Objects.equals(m_sCode, country.m_sCode) &&
               Objects.equals(m_sDescription, country.m_sDescription);
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(m_sCode, m_sDescription);
        }

    @Override
    public String toString()
        {
        return "Country{" +
               "code='" + m_sCode + '\'' +
               ", description='" + m_sDescription + '\'' +
               '}';
        }

    private String m_sCode;
    private String m_sDescription;
}
