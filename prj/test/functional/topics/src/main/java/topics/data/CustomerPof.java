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

/**
 * Customer that supports POF serialization.
 */
public class CustomerPof
        extends Customer
        implements PortableObject
    {
    // ----- constructors ---------------------------------------------------

    public CustomerPof()
        {
        super();
        }

    public CustomerPof(String sName, int nId, Address address)
        {
        super(sName, nId, address);
        }

    // ----- PortableObject methods -----------------------------------------

    @Override
    public void readExternal(PofReader in)
        throws IOException
        {
        setName(in.readString(NAME));
        setId(in.readInt(ID));
        setAddress(in.readObject(ADDRESS));
        }

    @Override
    public void writeExternal(PofWriter out)
        throws IOException
        {
        out.writeString(NAME, getName());
        out.writeInt(ID, getId());
        out.writeObject(ADDRESS, getAddress());
        }

    // ----- constants ------------------------------------------------------

    public static final int NAME    = 0;
    public static final int ID      = 1;
    public static final int ADDRESS = 2;
    }
