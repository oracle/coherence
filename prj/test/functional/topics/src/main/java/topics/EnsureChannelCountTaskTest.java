/*
 * Copyright (c) 2000, 2023, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package topics;

import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.PartitionedCache;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache.PagedTopic;
import com.tangosol.coherence.component.util.daemon.queueProcessor.service.grid.partitionedService.partitionedCache.PagedTopic.EnsureChannelCountTask;

import com.tangosol.internal.net.topic.impl.paged.PagedTopicCaches;
import com.tangosol.run.xml.SimpleElement;
import com.tangosol.run.xml.XmlElement;
import org.junit.Test;

import java.util.concurrent.locks.ReentrantLock;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class EnsureChannelCountTaskTest
    {
    @Test
    public void shouldSetChannelCountIntoConfigMap()
        {
        String                 sTopicName = "test-topic";
        String                 sCacheName = PagedTopicCaches.Names.CONTENT.cacheNameForTopicName(sTopicName);
        ReentrantLock          lock       = new ReentrantLock();
        XmlElement             xmlCache   = new SimpleElement();
        EnsureChannelCountTask task       = new EnsureChannelCountTask();

        task.setChannelCount(29);
        task.setRequiredChannelCount(29);
        task.setTopicName(sTopicName);

        EnsureChannelCountTask stub = spy(task);

        PartitionedCache.ServiceConfig.Map configMap = mock(PartitionedCache.ServiceConfig.Map.class);
        PagedTopic                         service   = mock(PagedTopic.class);

        when(stub.getService()).thenReturn(service);

        when(service.getChannelCountFromConfigMap(sTopicName)).thenReturn(17);
        when(service.getTopicStoreLock()).thenReturn(lock);
        when(service.isSuspendedFully()).thenReturn(true);
        when(service.getServiceConfigMap()).thenReturn(configMap);

        when(configMap.get(sCacheName)).thenReturn(xmlCache);

        stub.run();

        assertThat(xmlCache.getSafeAttribute("channels").getInt(), is(29));
        }

    @Test
    public void shouldUpdateChannelCountIntoConfigMap()
        {
        String                 sTopicName = "test-topic";
        String                 sCacheName = PagedTopicCaches.Names.CONTENT.cacheNameForTopicName(sTopicName);
        ReentrantLock          lock       = new ReentrantLock();
        XmlElement             xmlCache   = new SimpleElement();
        EnsureChannelCountTask task       = new EnsureChannelCountTask();

        xmlCache.addElement("channels").setInt(17);

        task.setChannelCount(29);
        task.setRequiredChannelCount(29);
        task.setTopicName(sTopicName);

        EnsureChannelCountTask stub = spy(task);

        PartitionedCache.ServiceConfig.Map configMap = mock(PartitionedCache.ServiceConfig.Map.class);
        PagedTopic                         service   = mock(PagedTopic.class);

        when(stub.getService()).thenReturn(service);

        when(service.getChannelCountFromConfigMap(sTopicName)).thenReturn(17);
        when(service.getTopicStoreLock()).thenReturn(lock);
        when(service.isSuspendedFully()).thenReturn(true);
        when(service.getServiceConfigMap()).thenReturn(configMap);

        when(configMap.get(sCacheName)).thenReturn(xmlCache);

        stub.run();

        assertThat(xmlCache.getSafeAttribute("channels").getInt(), is(29));
        }
    }
