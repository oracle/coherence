/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package ai_tests;

import com.tangosol.net.Coherence;
import com.tangosol.net.Session;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.util.concurrent.TimeUnit;

public class LocalVectorDemoTests
        extends VectorDemoTests
    {
    @BeforeAll
    static void setupCoherence() throws Exception
        {
        System.setProperty("coherence.cluster", CLUSTER_NAME);
        System.setProperty("coherence.wka", "127.0.0.1");
        System.setProperty("coherence.localhost", "127.0.0.1");
        System.setProperty("coherence.ttl", "0");
        System.setProperty("coherence.profile", "thin");
        System.setProperty("coherence.client", "direct");

        Coherence coherence = Coherence.clusterMember().start().get(5, TimeUnit.MINUTES);
        m_session = coherence.getSession();
        }

    @AfterAll
    static void cleanupCoherence()
        {
        Coherence.closeAll();
        }

    @Override
    Session getSession()
        {
        return m_session;
        }

    protected static Session m_session;
    }
