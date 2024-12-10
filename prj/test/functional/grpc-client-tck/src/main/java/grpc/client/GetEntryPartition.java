/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package grpc.client;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.processor.AbstractProcessor;

import java.io.IOException;

public class GetEntryPartition<K, V>
        extends AbstractProcessor<K, V, Integer>
        implements ExternalizableLite, PortableObject
    {
    public GetEntryPartition()
        {
        }

    @Override
    public Integer process(InvocableMap.Entry<K, V> entry)
        {
        BinaryEntry<K, V> binaryEntry = entry.asBinaryEntry();
        return binaryEntry.getContext().getKeyPartition(binaryEntry.getBinaryKey());
        }

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        }
    }
