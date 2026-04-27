/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package processor;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import com.tangosol.coherence.component.util.SafeService;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService;

import com.tangosol.net.NamedCache;

import com.tangosol.util.InvocableMap;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * VT-enabled distributed EntryProcessor suite for the pooled partitioned service.
 *
 * @author Aleks Seovic  2026.04.24
 * @since 26.04
 */
public class VirtualDaemonPoolDistPoolEntryProcessorTests
        extends AbstractDistEntryProcessorTests
    {
    public VirtualDaemonPoolDistPoolEntryProcessorTests()
        {
        super("dist-pool-test1");
        }

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

    @Test
    public void shouldUseVirtualDaemonPoolForDistPoolProcessorService()
        {
        NamedCache<?, ?>     cache   = getNamedCache();
        PartitionedService   service = (PartitionedService) ((SafeService) cache.getCacheService()).getRunningService();

        assertThat(service.getDaemonPool(), instanceOf(PartitionedService.VirtualDaemonPool.class));
        assertThat(((PartitionedService.VirtualDaemonPool) service.getDaemonPool()).getTaskLimit(), is(0));
        }
    }
