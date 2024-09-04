/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.processor;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.BinaryEntry;
import com.tangosol.util.InvocableMap;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Touches an entry (if present) in order to trigger interceptor re-evaluation
 * and possibly increment expiry time.
 *
 * @author as  2015.01.16
 * @since 12.2.1
 */
public class TouchProcessor<K, V, R>
        implements InvocableMap.EntryProcessor<K, V, R>,
                   ExternalizableLite, PortableObject
    {
    // ---- EntryProcessor interface ----------------------------------------

    @Override
    public R process(InvocableMap.Entry<K, V> entry)
        {
        if (entry.isPresent())
            {
            BinaryEntry<K, V> binaryEntry = entry.asBinaryEntry();
            binaryEntry.updateBinaryValue(binaryEntry.getBinaryValue());
            }
        return null;
        }

    // ---- ExternalizableLite interface ------------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        }

    // ---- PortableObject interface ----------------------------------------

    @Override
    public void readExternal(PofReader pofReader) throws IOException
        {
        }

    @Override
    public void writeExternal(PofWriter pofWriter) throws IOException
        {
        }
    }
