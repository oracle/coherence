/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package topics.data;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import java.io.IOException;

public class AddressExternalizableLiteAndPof
        extends AddressExternalizableLite
        implements PortableObject
    {
    // ----- constructors ---------------------------------------------------

    public AddressExternalizableLiteAndPof()
        {
        super();
        }

    public AddressExternalizableLiteAndPof(String street, String city, String state, int zipcode)
        {
        super(street, city, state, zipcode);
        }

    public AddressExternalizableLiteAndPof(Address address)
        {
        super(address);
        }

    // ----- PortableObject methods -----------------------------------------


    @Override
    public void readExternal(PofReader in)
        throws IOException
        {
        setStreet(in.readString(AddressPof.STREET));
        setCity(in.readString(AddressPof.CITY));
        setState(in.readString(AddressPof.STATE));
        setZipcode(in.readObject(AddressPof.ZIP));
        }

    @Override
    public void writeExternal(PofWriter out)
        throws IOException
        {
        out.writeString(AddressPof.STREET, getStreet());
        out.writeString(AddressPof.CITY, getCity());
        out.writeString(AddressPof.STATE, getState());
        out.writeInt(AddressPof.ZIP, getZipcode());
        }

    // ----- helpers --------------------------------------------------------

    static public AddressExternalizableLite getRandomAddress()
        {
        return new AddressExternalizableLiteAndPof(arrAddress[random.nextInt(arrAddress.length)]);
        }

    }
