/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package util;

import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache$Storage;
import com.tangosol.coherence.component.util.safeService.SafeCacheService;
import com.tangosol.net.CacheService;
import com.tangosol.net.NamedMap;

import java.util.Map;

/**
 * Helper class for tests relying on listener registration.
 *
 * @author Jonathan Knight  2020.06.22
 */
public class EventsHelper
    {
    public static int getTotalListenerCount(NamedMap<?, ?> map)
        {
        PartitionedCache$Storage storage = getStorage(map);
        return getKeyListenerCount(storage) + getListenerCount(storage);
        }

    public static int getListenerCount(NamedMap<?, ?> map)
        {
        PartitionedCache$Storage storage = getStorage(map);
        return getListenerCount(storage);
        }

    public static int getKeyListenerCount(NamedMap<?, ?> map)
        {
        PartitionedCache$Storage storage = getStorage(map);
        return getKeyListenerCount(storage);
        }

    private static int getListenerCount(PartitionedCache$Storage storage)
        {
        if (storage == null)
            {
            return 0;
            }
        Map<?, ?> map = storage.getListenerMap();
        return map != null ? map.size() : 0;
        }

    private static int getKeyListenerCount(PartitionedCache$Storage storage)
        {
        if (storage == null)
            {
            return 0;
            }
        Map<?, ?> map = storage.getKeyListenerMap();
        return map != null ? map.size() : 0;
        }

    private static PartitionedCache$Storage getStorage(NamedMap<?, ?> map)
        {
        CacheService service = map.getService();
        if (service instanceof SafeCacheService)
            {
            service = ((SafeCacheService) service).getRunningCacheService();
            }

        return service instanceof PartitionedCache
                ? ((PartitionedCache) service).getStorage(map.getName())
                : null;
        }
    }
