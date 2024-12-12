/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package topics;

import com.oracle.bedrock.junit.CoherenceBuilder;
import com.oracle.bedrock.options.Timeout;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.tangosol.net.Cluster;
import com.tangosol.net.Coherence;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;

/**
 * This test verifies that topic publishers and subscribers recover from
 * a total loss of storage. In this case storage has active persistence
 * enabled and is removed via a clean shutdown (as would happen in k8s
 * using the operator).
 */
public class TopicsStorageRecoveryTests
    extends AbstractTopicsStorageRecoveryTests
    {
    @Override
    protected String getClientConfig()
        {
        return "simple-persistence-bdb-cache-config.xml";
        }

    @Override
    protected CoherenceBuilder getCoherenceBuilder()
        {
        return CoherenceBuilder.clusterMember();
        }

    @Override
    protected void assertCoherenceReady(Coherence coherence)
        {
        Cluster cluster = coherence.getCluster();
        Eventually.assertDeferred(cluster::isRunning, is(true));
        String sMethodName = m_testName.getMethodName();
        System.err.println("Waiting for local Coherence instance to join cluster " + sMethodName);
        Eventually.assertDeferred(() -> cluster.getMemberSet().size(), is(3), Timeout.after(5, TimeUnit.MINUTES));
        }
    }
