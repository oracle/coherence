/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package topics.remote;

import com.oracle.bedrock.Option;
import com.oracle.bedrock.OptionsByType;
import com.oracle.bedrock.junit.SessionBuilders;
import com.oracle.bedrock.runtime.coherence.options.CacheConfig;
import com.tangosol.net.Session;
import topics.AbstractTopicChannelCountTests;

public class RemoteTopicChannelCountTests
        extends AbstractTopicChannelCountTests
    {
    @Override
    protected String getTopicName()
        {
        return "pof-" + m_testWatcher.getMethodName();
        }

    @Override
    protected Session createSession(Option... options)
        {
        OptionsByType optionsByType = OptionsByType.of(options);
        optionsByType.add(CacheConfig.of("topics-channel-client-config.xml"));

        return m_cluster.buildSession(SessionBuilders
                .extendClient("topics-channel-client-config.xml", options));
        }
    }
