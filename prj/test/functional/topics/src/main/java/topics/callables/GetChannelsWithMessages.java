/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package topics.callables;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.tangosol.internal.net.topic.impl.paged.PagedTopicCaches;
import com.tangosol.internal.net.topic.impl.paged.model.Page;
import com.tangosol.net.Coherence;
import com.tangosol.net.PagedTopicService;
import com.tangosol.net.Session;
import com.tangosol.net.topic.NamedTopic;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Returns an array of channel numbers that contain messages.
 */
public class GetChannelsWithMessages
        implements RemoteCallable<Set<Integer>>
    {
    public GetChannelsWithMessages(String sTopicName)
        {
        m_sTopicName = sTopicName;
        }

    @Override
    public Set<Integer> call() throws Exception
        {
        Session          session = Coherence.getInstance().getSession();
        NamedTopic<?>    topic   = session.getTopic(m_sTopicName);
        PagedTopicCaches caches  = new PagedTopicCaches(m_sTopicName, (PagedTopicService) topic.getService());

        return caches.Pages.keySet()
                .stream()
                .map(Page.Key::getChannelId)
                .collect(Collectors.toSet());
        }
    
    private final String m_sTopicName;
    }
