/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService;

import com.tangosol.util.ConcurrentKeyLock;
import com.tangosol.util.ConcurrentMap;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link PartitionedCache.ResourceCoordinator}.
 *
 * @author Aleks Seovic  2026.04.10
 * @since 26.04
 */
public class PartitionedCacheResourceCoordinatorTest
    {
    @Test
    public void shouldInstantiateConcurrentKeyLock()
        {
        PartitionedCache.ResourceCoordinator coordinator = new PartitionedCache.ResourceCoordinator();
        ConcurrentMap                        map         = coordinator.instantiateControlMap();

        assertTrue(map instanceof ConcurrentKeyLock);
        }

    @Test
    public void shouldSupportResourceControlLockingViaInstantiatedMap()
        {
        PartitionedCache.ResourceCoordinator coordinator = new PartitionedCache.ResourceCoordinator();
        ConcurrentMap                        map         = coordinator.instantiateControlMap();

        assertTrue(map.lock("key", -1L));
        assertTrue(map.unlock("key"));

        assertTrue(map.lock(ConcurrentMap.LOCK_ALL, -1L));
        assertTrue(map.lock("key", 0L));
        assertTrue(map.unlock("key"));
        assertTrue(map.unlock(ConcurrentMap.LOCK_ALL));
        }
    }
