/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package processor;

import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;

import com.oracle.coherence.testing.AbstractFunctionalTest;

import com.tangosol.coherence.component.util.SafeService;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.PartitionedService;

import com.tangosol.net.NamedCache;

import com.tangosol.util.InvocableMap;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Properties;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * VT-enabled distributed EntryProcessor suite for the pooled two-server service.
 *
 * @author Aleks Seovic  2026.04.24
 * @since 26.04
 */
public class VirtualDaemonPoolDistPoolMultiEntryProcessorTests
        extends AbstractDistEntryProcessorTests
    {
    public VirtualDaemonPoolDistPoolMultiEntryProcessorTests()
        {
        super("dist-pool-test2");
        }

    @BeforeClass
    public static void _startup()
        {
        System.setProperty("coherence.test.daemonpool", "virtual");

        Properties props = new Properties();

        props.setProperty("coherence.test.daemonpool", "virtual");

        AbstractFunctionalTest._startup();

        CoherenceClusterMember member1 =
                startCacheServer("VirtualDistPoolMultiProcessorTests-1", "processor", null, props);
        CoherenceClusterMember member2 =
                startCacheServer("VirtualDistPoolMultiProcessorTests-2", "processor", null, props);

        m_listClusterMembers.add(member1);
        m_listClusterMembers.add(member2);
        }

    @AfterClass
    public static void _shutdown()
        {
        try
            {
            stopCacheServer("VirtualDistPoolMultiProcessorTests-1");
            stopCacheServer("VirtualDistPoolMultiProcessorTests-2");
            }
        finally
            {
            try
                {
                AbstractFunctionalTest._shutdown();
                }
            finally
                {
                System.clearProperty("coherence.test.daemonpool");
                }
            }
        }

    @Test
    public void shouldUseVirtualDaemonPoolForDistPoolMultiProcessorService()
        {
        NamedCache<?, ?>   cache   = getNamedCache();
        PartitionedService service = (PartitionedService) ((SafeService) cache.getCacheService()).getRunningService();

        assertThat(service.getDaemonPool(), instanceOf(PartitionedService.VirtualDaemonPool.class));
        assertThat(((PartitionedService.VirtualDaemonPool) service.getDaemonPool()).getTaskLimit(), is(0));
        }
    }
