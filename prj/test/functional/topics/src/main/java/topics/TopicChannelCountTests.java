/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package topics;

import com.oracle.bedrock.Option;

import com.oracle.bedrock.OptionsByType;
import com.oracle.bedrock.junit.SessionBuilders;

import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.tangosol.net.Session;

public class TopicChannelCountTests
        extends AbstractTopicChannelCountTests
    {
    @Override
    protected String getTopicName()
        {
        return m_testWatcher.getMethodName();
        }

    @Override
    protected Session createSession(Option... options)
        {
        OptionsByType optionsByType = OptionsByType.of(options);
        optionsByType.add(CacheConfig.of(CACHE_CONFIG_FILE));
        return m_cluster.buildSession(SessionBuilders.storageDisabledMember(optionsByType.asArray()));
        }
    }
