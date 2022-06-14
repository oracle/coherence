/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package cache;

import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache;
import com.tangosol.coherence.component.util.safeService.SafeCacheService;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Service;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CacheTestHelper
    {
    /**
     * Return the backup map for the given cache.
     * NOTE: this code assumes either DCCF or ECCF is the factory.  Also, it can only
     * be called for distributed caches.
     *
     * @param cache  the cache which owns the backup map
     *
     * @return the backup map
     */
    public static Map getBackupMap(NamedCache cache)
        {
        assertTrue(cache != null);
        Service service = ((SafeCacheService) cache.getCacheService()).getService();
        assertTrue(service instanceof PartitionedCache);

        return ((PartitionedCache) service).getStorage(cache.getCacheName()).getBackupMap();
        }

    /**
     * Validate that the backup map of the cache is a specific type.
     *
     * @param cache  the cache which owns the backup map
     * @param clz    the expected class of the backup map
     */
    public static void validateBackupMapType(NamedCache cache, Class clz)
        {
        assertEquals(clz, getBackupMap(cache).getClass());
        }
    }
