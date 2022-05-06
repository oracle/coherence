/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package topics.data;

import com.tangosol.io.ExternalizableLite;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.Random;

public class AddressExternalizableLite
        extends Address
        implements ExternalizableLite
    {
    // ----- constructors ---------------------------------------------------

    public AddressExternalizableLite()
        {
        super();
        }

    public AddressExternalizableLite(String street, String city, String state, int zipcode)
        {
        super(street, city, state, zipcode);
        }

    public AddressExternalizableLite(Address address)
        {
        super(address);
        }

    // ----- helpers --------------------------------------------------------

    static public AddressExternalizableLite getRandomAddress()
        {
        return new AddressExternalizableLite(arrAddress[random.nextInt(arrAddress.length)]);
        }

    // ----- ExternalizableLite methods -------------------------------------

    @Override
    public void readExternal(DataInput in)
        throws IOException
        {
        setStreet(in.readUTF());
        setCity(in.readUTF());
        setState(in.readUTF());
        setZipcode(in.readInt());
        }

    @Override
    public void writeExternal(DataOutput out)
        throws IOException
        {
        out.writeUTF(getStreet());
        out.writeUTF(getCity());
        out.writeUTF(getState());
        out.writeInt(getZipcode());
        }

    // ----- data members ---------------------------------------------------

    static private Random random = new Random(5);
    }
