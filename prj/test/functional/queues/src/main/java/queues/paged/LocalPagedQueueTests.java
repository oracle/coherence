/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package queues.paged;

import com.tangosol.internal.net.queue.paged.PagedNamedQueue;
import com.tangosol.internal.net.queue.paged.PagedQueueCacheNames;
import com.tangosol.net.Coherence;
import com.tangosol.net.NamedCache;
import com.tangosol.net.NamedQueue;
import com.tangosol.net.Session;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import queues.AbstractQueueTests;

import java.util.concurrent.TimeUnit;

@SuppressWarnings({"rawtypes", "unchecked"})
public class LocalPagedQueueTests<QueueType extends NamedQueue>
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
    @Disabled("Paged queue is not size limited")
    public void shouldOfferAndPollSizeLimitedQueue(String sSerializer)
        {
        }

    @Override
    public Session getSession()
        {
        return m_session;
        }

    @Override
    @SuppressWarnings("unchecked")
    public QueueType getNamedCollection(Session session, String sName)
        {
        return (QueueType) new PagedNamedQueue<>(sName, session.getCache(sName));
        }

    @Override
    @SuppressWarnings("unchecked")
    public QueueType getCollection(Session session, String sName)
        {
        return (QueueType) new PagedNamedQueue<>(sName, session.getCache(sName));
        }

    @Override
    public NamedCache getCollectionCache(String sName)
        {
        return super.getCollectionCache(PagedQueueCacheNames.Elements.getCacheName(sName));
        }

    // ----- data members ---------------------------------------------------

    private static Session m_session;
    }
