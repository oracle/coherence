/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.cdi.server.data;


import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import java.io.IOException;
import java.io.Serializable;
import java.util.Objects;


/**
 * A simple class representing an address that can be used in tests requiring
 * test data.
 *
 * @author Jonathan Knight  2019.10.25
 */
public class PhoneNumber
        implements PortableObject, Serializable
    {

    private int countryCode;

    private String number;

    /**
     * Default constructor for serialization.
     */
    public PhoneNumber()
        {
        }

    /**
     * Create a {@link PhoneNumber}.
     *
     * @param countryCode the country code
     * @param number      the phone number
     */
    public PhoneNumber(int countryCode, String number)
        {
        this.countryCode = countryCode;
        this.number = number;
        }

    public int getCountryCode()
        {
        return countryCode;
        }

    public void setCountryCode(int countryCode)
        {
        this.countryCode = countryCode;
        }

    public String getNumber()
        {
        return number;
        }

    public void setNumber(String number)
        {
        this.number = number;
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
        PhoneNumber that = (PhoneNumber) o;
        return Objects.equals(countryCode, that.countryCode)
               && Objects.equals(number, that.number);
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(countryCode, number);
        }

    @Override
    public String toString()
        {
        return "{countryCode: " + countryCode
               + ", number: \"" + number + "\"}";
        }

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        countryCode = in.readInt(0);
        number = in.readString(1);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeInt(0, countryCode);
        out.writeString(1, number);
        }
    }
