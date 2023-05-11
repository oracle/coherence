/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package aggregator;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.net.BackingMapContext;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.GuardSupport;
import com.tangosol.util.*;
import com.tangosol.util.InvocableMap.Entry;
import com.tangosol.util.InvocableMap.StreamingAggregator;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class IndexDumpAggregator
        implements StreamingAggregator, PortableObject
    {
    private int expectedSize;

    public IndexDumpAggregator(int expectedSize)
        {
        this.expectedSize = expectedSize;
        }

    @Override
    public boolean accumulate(Entry entry)
        {
        if (entry instanceof BinaryEntry)
            {
            BinaryEntry binEntry = (BinaryEntry) entry;
            BackingMapContext ctx = binEntry.getBackingMapContext();
            Map<?, ?> resourceMap = ctx.getBackingMap();
            Map<ValueExtractor, MapIndex> indexMap = ctx.getIndexMap();

            for (java.util.Map.Entry<ValueExtractor, MapIndex> entryIndex : indexMap.entrySet())
                {
                logMessage("INFO: Extractor: %s", entryIndex.getKey());
                MapIndex index = entryIndex.getValue();

                Map contents = index.getIndexContents();

                int size = contents.size();
                int totalSize = 0;

                logMessage("INFO: Number of index entries: %d\n", size);
                for (Object contentsMapEntry : contents.entrySet())
                    {
                    java.util.Map.Entry<?, ?> internalEntry = (java.util.Map.Entry<?, ?>) contentsMapEntry;
                    int setSize = ((java.util.Set<?>) internalEntry.getValue()).size();

                    logMessage("%s:%d", internalEntry.getKey(), setSize);
                    checkResourceMap(resourceMap, (java.util.Set<?>) internalEntry.getValue(), ctx
                            .getManagerContext().getValueFromInternalConverter());
                    totalSize += setSize;
                    }

                checkIndex(resourceMap, contents, ctx.getManagerContext().getKeyFromInternalConverter());

                logMessage("INFO: Total Size %d", totalSize);
                if (totalSize != expectedSize)
                    {
                    logMessage("WARNING: expected %d != actual %d", expectedSize, totalSize);
                    }
                }
            }

        // after the first entry, we are done!!
        return false;
        }

    /**
     * Asserts that all resource map entries exists as reverse index
     *
     * @param resourceMap
     * @param contents
     * @param converter
     */
    private void checkIndex(Map resourceMap, Map contents, Converter converter)
        {
        for (Object key : resourceMap.keySet())
            {
            boolean contains = false;
            int cnt = 0;
            for (Object contentsMapEntry : contents.entrySet())
                {
                if ((cnt++ & 0xFFF) == 0)
                    {
                    GuardSupport.heartbeat();
                    }

                java.util.Set<?> keySet = (java.util.Set<?>) ((java.util.Map.Entry<?, ?>) contentsMapEntry).getValue();
                if (keySet.contains(key))
                    {
                    contains = true;
                    break;
                    }
                }
            if (!contains)
                {
                logMessage("WARNING: ReverseIndex does not contain %s [%s]", converter.convert(key), key);
                }
            }

        }

    /**
     * Assert that all entries in a set exists in the resource map
     *
     * @param resourceMap
     * @param entries
     * @param converter
     */
    private void checkResourceMap(Map<?, ?> resourceMap, Set<?> entries, Converter converter)
        {
        int cnt = 0;
        for (Iterator<?> iterator = entries.iterator(); iterator.hasNext(); cnt++)
            {
            if ((cnt & 0xFFF) == 0)
                {
                GuardSupport.heartbeat();
                }

            Object object = iterator.next();
            if (!resourceMap.containsKey(object))
                {
                logMessage("WARNING: ResourceMap does not contain %s [%s]", converter.convert(object), object);
                }
            }
        }

    public static void logMessage(String format, Object... values)
        {
        CacheFactory.log(String.format(format, values), 4);
        }



    @Override
    public StreamingAggregator supply() {
    return this;
    }

    @Override
    public boolean combine(Object o) {
    return false;
    }

    @Override
    public Object getPartialResult() {
    return null;
    }

    @Override
    public Object finalizeResult() {
    return null;
    }

    @Override
    public void readExternal(PofReader pofReader) throws IOException
        {
        expectedSize = pofReader.readInt(0);
        }

    @Override
    public void writeExternal(PofWriter pofWriter) throws IOException
        {
        pofWriter.writeInt(0, expectedSize);
        }
    }
