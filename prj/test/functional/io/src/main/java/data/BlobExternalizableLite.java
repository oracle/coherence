/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package data;


import com.tangosol.io.ExternalizableLite;
import com.tangosol.util.ExternalizableHelper;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class BlobExternalizableLite
    extends Blob
    implements ExternalizableLite
    {
    // ----- constructors ----------------------------------------------------

    public BlobExternalizableLite()
        {
        }

    public BlobExternalizableLite(int nSize)
        {
        super(nSize);
        }


    // ----- ExternalizableLite methods -------------------------------------

    @Override
    public void readExternal(DataInput in)
            throws IOException
        {
        setPayload(ExternalizableHelper.readByteArray(in));
        }

    @Override
    public void writeExternal(DataOutput out)
            throws IOException
        {
        ExternalizableHelper.writeByteArray(out, getPayload());
        }
    }
