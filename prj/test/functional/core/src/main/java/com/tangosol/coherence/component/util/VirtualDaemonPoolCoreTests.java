/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.component.util;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService;

import com.tangosol.net.NamedCache;

import com.tangosol.util.InvocableMap;
import com.tangosol.util.processor.AbstractProcessor;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Functional validation for the conservative Phase 1 VirtualDaemonPool path in
 * partitioned cache services.
 *
 * @author Aleks Seovic  2026.04.24
 * @since 26.04
 */
public class VirtualDaemonPoolCoreTests
        extends AbstractFunctionalTest
    {
    @BeforeClass
    public static void _startup()
        {
        System.setProperty("coherence.test.daemonpool", "virtual");
        System.setProperty("coherence.distributed.localstorage", "true");

        AbstractFunctionalTest._startup();
        }

    @AfterClass
    public static void _shutdown()
        {
        try
            {
            AbstractFunctionalTest._shutdown();
            }
        finally
            {
            System.clearProperty("coherence.test.daemonpool");
            System.clearProperty("coherence.distributed.localstorage");
            }
        }

    @After
    public void cleanupCache()
        {
        getNamedCache(CACHE_NAME).clear();
        }

    @Test
    public void shouldUseVirtualDaemonPoolForDistributedCacheService()
        {
        NamedCache<Integer, Integer> cache   = getNamedCache(CACHE_NAME);
        PartitionedService           service = (PartitionedService) ((SafeService) cache.getCacheService()).getRunningService();

        assertThat(service.getDaemonPool(), instanceOf(PartitionedService.VirtualDaemonPool.class));
        assertThat(((PartitionedService.VirtualDaemonPool) service.getDaemonPool()).getTaskLimit(), is(0));
        }

    @Test
    public void shouldExecuteBasicDistributedCacheOperationsUnderVirtualDaemonPool()
        {
        NamedCache<Integer, Integer> cache = getNamedCache(CACHE_NAME);

        cache.put(1, 1);
        assertThat(cache.get(1), is(1));

        Integer nUpdated = cache.invoke(1, new IncrementProcessor());
        assertThat(nUpdated, is(2));
        assertThat(cache.get(1), is(2));

        cache.putAll(Map.of(2, 20, 3, 30));

        Map<Integer, Integer> map = cache.getAll(Set.of(1, 2, 3));
        assertThat(map.get(1), is(2));
        assertThat(map.get(2), is(20));
        assertThat(map.get(3), is(30));
        }

    public static class IncrementProcessor
            extends AbstractProcessor<Integer, Integer, Integer>
            implements Serializable
        {
        @Override
        public Integer process(InvocableMap.Entry<Integer, Integer> entry)
            {
            Integer nValue = entry.getValue();
            int     nNext  = (nValue == null ? 0 : nValue) + 1;

            entry.setValue(nNext);
            return nNext;
            }
        }

    protected static final String CACHE_NAME = "dist-vt-phase1-core";
    }
