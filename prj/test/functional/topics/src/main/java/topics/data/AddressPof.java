/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package topics.data;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import java.io.IOException;
import java.util.Random;

/**
 * Customer that supports POF serialization.
 */
public class AddressPof
        extends Address
        implements PortableObject
    {
    // ----- constructors ---------------------------------------------------

    public AddressPof()
        {
        super();
        }

    public AddressPof(String street, String city, String state, int zipcode)
        {
        super(street, city, state, zipcode);
        }

    public AddressPof(Address address)
        {
        super(address);
        }

    // ----- helpers --------------------------------------------------------

    static public AddressPof getRandomAddress()
        {
        return new AddressPof(arrAddress[random.nextInt(arrAddress.length)]);
        }

    // ----- PortableObject methods -----------------------------------------


    @Override
    public void readExternal(PofReader in)
        throws IOException
        {
        setStreet(in.readString(STREET));
        setCity(in.readString(CITY));
        setState(in.readString(STATE));
        setZipcode(in.readObject(ZIP));
        }

    @Override
    public void writeExternal(PofWriter out)
        throws IOException
        {
        out.writeString(STREET, getStreet());
        out.writeString(CITY, getCity());
        out.writeString(STATE, getState());
        out.writeInt(ZIP, getZipcode());
        }

    // ----- constants ------------------------------------------------------

    public static final int STREET = 0;
    public static final int CITY   = 1;
    public static final int STATE  = 2;
    public static final int ZIP    = 3;

    // ----- data members ---------------------------------------------------

    static private Random random = new Random(5);
    }
