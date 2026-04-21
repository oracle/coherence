/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package persistence;

import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache.Storage;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.net.BackingMapContext;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.PartitionedService;

import com.tangosol.util.Base;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.ImmutableArrayList;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.processor.AbstractProcessor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.Map;
import java.util.Set;

/**
 * IndexTriggerCheckProcessor for persistence tests.
 *
 * @author tam 2025.08.07
 */
@SuppressWarnings("rawtypes")
public class IndexTriggerCheckProcessor
        extends AbstractProcessor
        implements ExternalizableLite
    {

    /**
     * {@inheritDoc}
     */
    public Object process(InvocableMap.Entry entry)
        {
        BinaryEntry binEntry = (BinaryEntry) entry;
        BackingMapContext bmc = binEntry.getBackingMapContext();
        BackingMapManagerContext bmmc = bmc.getManagerContext();
        PartitionedService service = (PartitionedService) bmmc.getCacheService();

        try
            {
            PartitionedCache partitionedCache = (PartitionedCache) service;

            Storage storage       = partitionedCache.getStorage(bmc.getCacheName());
            Map     mapIndices    = storage.getIndexExtractorMap();
            Set     setExtractors = mapIndices.keySet();

            // triggers are recovered by being sent to the service senior in a
            // ListenerRequest; on slow platforms this can take some time.
            // poll for 10s if necessary
            long ldtStart    = CacheFactory.getSafeTimeMillis();
            long ldtNow      = 0L;
            Set  setTriggers = null;
            do
                {
                if (ldtNow > 0L)
                    {
                    Base.sleep(500L);
                    }

                setTriggers = storage.getTriggerSet();
                ldtNow      = CacheFactory.getSafeTimeMillis();
                }
            while (setTriggers == null && ldtNow < ldtStart + 10000L);

            return new Object[]{new ImmutableArrayList(setExtractors), new ImmutableArrayList(setTriggers)};
            }
        catch (Exception e)
            {
            throw Base.ensureRuntimeException(e);
            }
        }

    /**
     * {@inheritDoc}
     */
    public void readExternal(DataInput in) throws IOException
        {
        }

    /**
     * {@inheritDoc}
     */
    public void writeExternal(DataOutput out) throws IOException
        {
        }
    }
