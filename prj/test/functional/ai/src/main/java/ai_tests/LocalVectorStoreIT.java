/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package ai_tests;

import com.tangosol.net.Coherence;

import org.junit.jupiter.api.BeforeAll;

import java.util.concurrent.TimeUnit;

public class LocalVectorStoreIT
        extends BaseVectorStoreIT
    {
    @BeforeAll
    static void startCoherence() throws Exception
        {
        System.setProperty("coherence.ttl",     "0");
        System.setProperty("coherence.wka",     "127.0.0.1");
        System.setProperty("coherence.cluster", "LocalVectorStoreIT");

        Coherence.clusterMember().start().get(5, TimeUnit.MINUTES);
        }
    }
