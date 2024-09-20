/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package queues;

import com.tangosol.net.Coherence;
import com.tangosol.net.NamedQueue;
import com.tangosol.net.Session;

import org.junit.jupiter.api.BeforeAll;

import java.util.concurrent.TimeUnit;

@SuppressWarnings({"rawtypes"})
public class LocalQueueTests<QueueType extends NamedQueue>
        extends AbstractQueueTests<QueueType>
    {
    @BeforeAll
    static void setup() throws Exception
        {
        System.setProperty("coherence.ttl",         "0");
        System.setProperty("coherence.wka",         "127.0.0.1");
        System.setProperty("coherence.localhost",   "127.0.0.1");
        System.setProperty("coherence.cacheconfig", "queue-cache-config.xml");

        Coherence coherence = Coherence.clusterMember().start().get(5, TimeUnit.MINUTES);
        m_session = coherence.getSession();
        }

    @Override
    public Session getSession()
        {
        return m_session;
        }

    // ----- data members ---------------------------------------------------

    private static Session m_session;
    }
