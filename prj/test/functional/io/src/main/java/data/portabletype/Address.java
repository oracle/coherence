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
@PortableType(id = 1001)
public class Address
    {

    public Address()
        {
        }

    public Address(String sAddress1, String sAddress2, String sCity, String sState, String sPostCode, String sCountry)
        {
        m_sAddress1 = sAddress1;
        m_sAddress2 = sAddress2;
        m_sCity = sCity;
        m_sState = sState;
        m_sPostCode = sPostCode;
        m_sCountry = sCountry;
        }

    public String getAddress1()
        {
        return m_sAddress1;
        }

    public void setAddress1(String sAddress1)
        {
        m_sAddress1 = sAddress1;
        }

    public String getM_sAddress2()
        {
        return m_sAddress2;
        }

    public void setAddress2(String sAddress2)
        {
        m_sAddress2 = sAddress2;
        }

    public String getCity()
        {
        return m_sCity;
        }

    public void setCity(String sCity)
        {
        m_sCity = sCity;
        }

    public String getState()
        {
        return m_sState;
        }

    public void setState(String sState)
        {
        m_sState = sState;
        }

    public String getPostCode()
        {
        return m_sPostCode;
        }

    public void setPostCode(String sPostCode)
        {
        m_sPostCode = sPostCode;
        }

    public String getM_sCountry() {
        return m_sCountry;
    }

    public void sCountry(String sCountry)
        {
        m_sCountry = m_sCountry;
        }

    @Override
    public String toString()
        {
        return "Address{" +
               "m_sAddress1='" + m_sAddress1 + '\'' +
               ", m_sAddress2='" + m_sAddress2 + '\'' +
               ", m_sCity='" + m_sCity + '\'' +
               ", m_sState='" + m_sState + '\'' +
               ", m_sPostCode='" + m_sPostCode + '\'' +
               ", m_sCountry='" + m_sCountry + '\'' +
               '}';
        }

    @Override
    public boolean equals(Object o)
        {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Address address = (Address) o;
        return Objects.equals(m_sAddress1, address.m_sAddress1) &&
               Objects.equals(m_sAddress2, address.m_sAddress2) &&
               Objects.equals(m_sCity, address.m_sCity) &&
               Objects.equals(m_sState, address.m_sState) &&
               Objects.equals(m_sPostCode, address.m_sPostCode) &&
               Objects.equals(m_sCountry, address.m_sCountry);
        }

    @Override
    public int hashCode() {
        return Objects.hash(m_sAddress1, m_sAddress2, m_sCity, m_sState, m_sPostCode, m_sCountry);
    }

    private String m_sAddress1;
    private String m_sAddress2;
    private String m_sCity;
    private String m_sState;
    private String m_sPostCode;
    private String m_sCountry;
}
