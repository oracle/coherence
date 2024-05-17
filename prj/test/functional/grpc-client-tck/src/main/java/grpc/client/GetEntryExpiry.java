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
import com.tangosol.net.cache.LocalCache;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.processor.AbstractProcessor;

import java.io.IOException;

public class GetEntryExpiry<K, V>
        extends AbstractProcessor<K, V, Long>
        implements ExternalizableLite, PortableObject
    {
    public GetEntryExpiry()
        {
        }

    @Override
    @SuppressWarnings("deprecation")
    public Long process(InvocableMap.Entry<K, V> entry)
        {
        BinaryEntry<K, V> binaryEntry = entry.asBinaryEntry();
        LocalCache        cache       = (LocalCache) binaryEntry.getBackingMapContext().getBackingMap();
        LocalCache.Entry  entryLocal  = (LocalCache.Entry) cache.getEntry(binaryEntry.getBinaryKey());
        return entryLocal.getExpiryMillis();
        }

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        }

    @SuppressWarnings("unchecked")
    public static <K, V> GetEntryExpiry<K, V> instance()
        {
        return INSTANCE;
        }

    @SuppressWarnings("rawtypes")
    public static final GetEntryExpiry INSTANCE = new GetEntryExpiry();
    }
