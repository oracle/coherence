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

public class CustomerExternalizableLiteAndPof
        extends CustomerExternalizableLite
        implements PortableObject
    {
    public CustomerExternalizableLiteAndPof()
        {
        }

    public CustomerExternalizableLiteAndPof(String sName, int nId, Address address)
        {
        super(sName, nId, address);
        }

    // ----- PortableObject methods -----------------------------------------

    @Override
    public void readExternal(PofReader in)
        throws IOException
        {
        setName(in.readString(CustomerPof.NAME));
        setId(in.readInt(CustomerPof.ID));
        setAddress(in.readObject(CustomerPof.ADDRESS));
        }

    @Override
    public void writeExternal(PofWriter out)
        throws IOException
        {
        out.writeString(CustomerPof.NAME, getName());
        out.writeInt(CustomerPof.ID, getId());
        out.writeObject(CustomerPof.ADDRESS, getAddress());
        }
    }
