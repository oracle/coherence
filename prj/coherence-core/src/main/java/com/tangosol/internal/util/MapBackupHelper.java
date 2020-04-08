/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util;

import com.tangosol.net.DistributedCacheService;
import com.tangosol.net.NamedCache;

import com.tangosol.net.partition.PartitionSet;

import com.tangosol.util.EntrySetMap;
import com.tangosol.util.ExternalizableHelper;

import com.tangosol.util.filter.AlwaysFilter;
import com.tangosol.util.filter.PartitionedFilter;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A utility class that contains methods to save a {@link Map}
 * to a stream and to restore a {@link Map} from a {@link DataInput}
 * stream.
 *
 * @author jk 2014.05.13
 *
 * @deprecated use facilities provided by Persistence instead
 */
public abstract class MapBackupHelper
    {
    /**
     * Write the contents of the provided Map to a DataOutputStream. If the Map
     * is an instance of NamedCache serviced by DistributedCacheService, it will
     * obtain the cache contents one partition at a time to minimize memory
     * utilization.
     *
     * @param out  destination for the backup
     * @param map  map to be backed-up
     *
     * @throws IOException if the write operations fail
     */
    public static void writeMap(DataOutput out, Map map)
            throws IOException
        {
        if (map instanceof NamedCache && ((NamedCache) map).getCacheService() instanceof DistributedCacheService)
            {
            NamedCache              cache       = (NamedCache) map;
            DistributedCacheService service     = (DistributedCacheService) cache.getCacheService();
            int                     cPartitions = service.getPartitionCount();
            PartitionSet            parts       = new PartitionSet(cPartitions);

            out.writeInt(cPartitions);

            for (int iPartition = 0; iPartition < cPartitions; iPartition++)
                {
                parts.add(iPartition);

                Set setEntries = cache.entrySet(new PartitionedFilter(AlwaysFilter.INSTANCE, parts));

                ExternalizableHelper.writeMap(out, new EntrySetMap(setEntries));

                parts.remove(iPartition);
                }
            }
        else
            {
            // capture the partition count as 1 for non distributed cache
            out.writeInt(1);
            ExternalizableHelper.writeMap(out, map);
            }
        }

    /**
     * Read map content from a DataInputStream and updates the provided Map
     * with the contents. If the cBlock parameter is > 0, the map will be
     * updated with putAll invocations using a map with a size not exceeding
     * cBlock.
     *
     * @param in      DataInput stream to read from
     * @param map     destination map to add entries into
     * @param cBlock  the maximum number of entries to read at once
     * @param loader  the ClassLoader to use
     *
     * @return the number of read and inserted entries
     *
     * @throws java.io.IOException if the readMap operation fails
     */
    public static int readMap(DataInput in, Map map, int cBlock, ClassLoader loader)
            throws IOException
        {
        // read the partition count
        int cMaps    = in.readInt();
        int cEntries = 0;

        for (int i = 0; i < cMaps; i++)
            {
            Map mapTmp = new HashMap();

            if (cBlock > 0)
                {
                ExternalizableHelper.readMap(in, mapTmp, cBlock, loader);
                }
            else
                {
                ExternalizableHelper.readMap(in, mapTmp, loader);
                }

            cEntries += mapTmp.size();
            map.putAll(mapTmp);
            }

        return cEntries;
        }
    }
