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

import com.tangosol.net.CacheFactory;

import com.tangosol.util.BinaryEntry;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.MapIndex;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.extractor.ReflectionExtractor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class IndexCheckAggregator
        implements InvocableMap.StreamingAggregator, PortableObject
    {
    private String getterString;
    private List getterList = new ArrayList();

    public IndexCheckAggregator(String sMethod)
        {
        getterString = sMethod;
        if (sMethod.indexOf(',') < 0)
            getterList.add(sMethod);
        else
            getterList = Arrays.asList(getterString.split(","));

        CacheFactory.log("++ DEBUG: getterList = " + getterList.toString());
        }

    @Override
    public InvocableMap.StreamingAggregator supply()
        {
        return this;
        }

    public boolean accumulate(InvocableMap.Entry entry)
        {
        if (entry instanceof BinaryEntry)
            {
            BinaryEntry binEntry = (BinaryEntry) entry;

            String missingIndice = "";
            Map<ValueExtractor, MapIndex> indexMap = binEntry.getBackingMapContext().getIndexMap();

            if (indexMap == null || indexMap.size() == 0)
                {
                throw new IllegalStateException(String.format("WARNING: No indices defined == %d\n",
                                                              indexMap == null ? -1 : indexMap.size()));
                }

            // further check the IndexMap to make sure no index is missing
            Iterator itr = getterList.iterator();
            while (itr.hasNext())
                {
                String getter = (String) itr.next();
                ReflectionExtractor extractor = new ReflectionExtractor(getter);
                if (indexMap.containsKey(extractor))
                    {
                    CacheFactory.log("++ DEBUG: Got index for: " + getter);
                    }
                else
                    {
                    missingIndice = missingIndice + " " + getter;
                    }

                }

            if (!missingIndice.isEmpty())
                throw new IllegalStateException("Missing index for: " + missingIndice);
            }

        // after the first entry, we are done!!
        return false;
        }

    @Override
    public boolean combine(Object partialResult)
        {
        return false;
        }

    @Override
    public Object getPartialResult()
        {
        return null;
        }

    @Override
    public Object finalizeResult()
        {
        return null;
        }

    public int characteristics()
        {
        return SERIAL;
        }

    @Override
    public void readExternal(PofReader pofReader) throws IOException
        {
        getterString = pofReader.readString(0);
        }

    @Override
    public void writeExternal(PofWriter pofWriter) throws IOException
        {
        pofWriter.writeString(0, getterString);
        }
    }

