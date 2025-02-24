/*
 * Copyright (c) 2000, 2025, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package netty.grpc.client.topics;

import com.oracle.bedrock.junit.CoherenceBuilder;
import com.oracle.bedrock.runtime.coherence.CoherenceCluster;
import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.runtime.options.StabilityPredicate;
import com.oracle.coherence.common.base.Logger;
import com.tangosol.net.Coherence;
import org.junit.Ignore;
import topics.AbstractTopicsStorageRecoveryTests;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@Ignore
public class GrpcTopicStorageRecoveryTests
        extends AbstractTopicsStorageRecoveryTests
    {
    @Override
    protected String getClientConfig()
        {
        return "simple-persistence-bdb-grpc-client-config.xml";
        }

    @Override
    protected CoherenceBuilder getCoherenceBuilder()
        {
        return CoherenceBuilder.client();
        }

    @Override
    protected void assertCoherenceReady(Coherence coherence)
        {
        assertThat(coherence.isActive(), is(true));
        assertThat(coherence.isStarted(), is(true));
        }

    @Override
    protected void restartCluster()
        {
        Logger.info(">>>> Starting cluster rolling restart.");
        String sMethodName = m_testName.getMethodName();

        s_storageCluster.unordered().relaunch(StabilityPredicate.of(
                CoherenceCluster.Predicates.isCoherenceRunning()),
                DisplayName.of(sMethodName + "-restarted"));
        Logger.info(">>>> Completed cluster rolling restart.");
        }
    }
