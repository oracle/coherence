/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package topics.callables;

import com.oracle.bedrock.runtime.concurrent.RemoteCallable;
import com.oracle.coherence.common.base.Logger;
import com.tangosol.net.Coherence;
import com.tangosol.net.TopicService;
import com.tangosol.net.topic.NamedTopic;

public class GetTopicChannelCount
        implements RemoteCallable<Integer>
    {
    public GetTopicChannelCount(String sName)
        {
        f_sName = sName;
        }

    @Override
    public Integer call() throws Exception
        {
        NamedTopic<?> topic = Coherence.getInstance().getSession().getTopic(f_sName);
        TopicService  topicService = topic.getTopicService();
        return topicService.getChannelCount(f_sName);
        }

    private final String f_sName;
    }
