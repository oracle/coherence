/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package ai_tests.search;

import com.oracle.coherence.ai.DocumentChunk;
import com.tangosol.net.Coherence;
import com.tangosol.net.NamedMap;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.util.concurrent.TimeUnit;

public class LocalSimilaritySearchIT
        extends BaseSimilaritySearchIT
    {
    @BeforeAll
    static void setup() throws Exception
        {
        String sAddress = "127.0.0.1";
        System.setProperty("coherence.wka", sAddress);
        System.setProperty("coherence.localhost", sAddress);
        System.setProperty("test.unicast.address", sAddress);
        System.setProperty("test.unicast.port", "0");
        System.setProperty("coherence.ttl", "0");

        System.setProperty("coherence.distributed.partitioncount", "13");

        Coherence coherence = Coherence.clusterMember().start().get(5, TimeUnit.MINUTES);
        m_session = coherence.getSession();

        NamedMap<Integer, DocumentChunk> vectors = m_session.getMap("vectors");
        m_valueZero = populateVectors(vectors);
        }

    @AfterAll
    static void cleanup()
        {
        Coherence.closeAll();
        }
    }
