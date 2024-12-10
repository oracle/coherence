/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package data.pof;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import java.io.IOException;

/**
 * @author jk  2014.02.28
 */
public class PhoneNumber
        implements Comparable<PhoneNumber>, PortableObject
    {
    public PhoneNumber()
        {
        }

    public PhoneNumber(int countryCode, String phoneNumber)
        {
        m_countryCode = countryCode;
        m_phoneNumber = phoneNumber;
        }

    public int getCountryCode()
        {
        return m_countryCode;
        }

    public String getPhoneNumber()
        {
        return m_phoneNumber;
        }

    @Override
    public int compareTo(PhoneNumber o)
        {
        int result = this.m_countryCode - o.m_countryCode;
        if (result == 0)
            {
            result = this.m_phoneNumber.compareTo(o.m_phoneNumber);
            }
        return result;
        }

    @Override
    public String toString()
        {
        return "+" + m_countryCode + ' ' + m_phoneNumber;
        }

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_countryCode = in.readInt(0);
        m_phoneNumber = in.readString(1);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeInt(0, m_countryCode);
        out.writeString(1, m_phoneNumber);
        }

    protected int    m_countryCode;

    protected String m_phoneNumber;
    }
