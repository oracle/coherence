/*
 * Copyright (c) 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package grpc.client;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.InvocableMap;

import java.io.IOException;
import java.io.Serializable;

import java.util.Set;

/**
 * Aggregator that will fail to execute.
 *
 * @since 14.1.1.2206.5
 */
@SuppressWarnings("rawtypes")
public class UnusableAggregator
        implements InvocableMap.EntryAggregator, Serializable, PortableObject
    {
    // ----- EntryAggregator interface ---------------------------------------

    public Object aggregate(Set setEntries)
        {
        return null;
        }

    // ----- PortableObject interface ---------------------------------------

    public void readExternal(PofReader in) throws IOException
        {
        }

    public void writeExternal(PofWriter out) throws IOException
        {
        }
    }
