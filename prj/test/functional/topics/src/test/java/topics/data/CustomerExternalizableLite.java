/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package topics.data;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import static com.tangosol.util.ExternalizableHelper.writeObject;

public class CustomerExternalizableLite
        extends Customer
        implements ExternalizableLite
    {
    // ----- constructors ---------------------------------------------------

    public CustomerExternalizableLite()
        {
        super();
        }

    public CustomerExternalizableLite(String sName, int nId, Address address)
        {
        super(sName, nId, address);
        }

    // ----- ExternalizableLite methods -------------------------------------

    @Override
    public void readExternal(DataInput in)
        throws IOException
        {
        setName(in.readUTF());
        setId(in.readInt());
        setAddress(ExternalizableHelper.readObject(in));
        }

    @Override
    public void writeExternal(DataOutput out)
        throws IOException
        {
        out.writeUTF(getName());
        out.writeInt(getId());
        writeObject(out, getAddress());
        }
    }
