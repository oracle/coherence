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
import com.tangosol.net.QueueService;
import com.tangosol.net.Session;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import queues.AbstractQueueTests;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

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

    @Test
    public void shouldCreateAllCaches()
        {
        String             sName      = getNewName();
        NamedQueue<String> queue      = getNamedCollection(sName);
        QueueService       service    = queue.getService();
        List<String>       listCaches = new ArrayList<>();

        Enumeration  en = service.getCacheNames();
        while (en.hasMoreElements())
            {
            listCaches.add((String) en.nextElement());
            }

        for (PagedQueueCacheNames name : PagedQueueCacheNames.values())
            {
            String sCacheName = name.getCacheName(sName);
            assertThat(listCaches.contains(sCacheName), is(true));
            }
        }

    @Test
    public void shouldReleaseAllCaches()
        {
        String             sName      = getNewName();
        NamedQueue<String> queue      = getNamedCollection(sName);
        Session            session    = getSession();

        Map<PagedQueueCacheNames, NamedCache<?, ?>> mapCaches = new HashMap<>();

        for (PagedQueueCacheNames name : PagedQueueCacheNames.values())
            {
            String           sCacheName = name.getCacheName(sName);
            NamedCache<?, ?> cache      = session.getCache(sCacheName);
            mapCaches.put(name, cache);
            assertThat(cache.isActive(), is(true));
            }

        queue.release();

        for (PagedQueueCacheNames name : PagedQueueCacheNames.values())
            {
            NamedCache<?, ?> cache = mapCaches.get(name);
            assertThat("Cache " + name + " should have been released", cache.isReleased(), is(true));
            }
        }

    @Test
    public void shouldDestroyAllCaches()
        {
        String             sName      = getNewName();
        NamedQueue<String> queue      = getNamedCollection(sName);
        Session            session    = getSession();

        Map<PagedQueueCacheNames, NamedCache<?, ?>> mapCaches = new HashMap<>();

        for (PagedQueueCacheNames name : PagedQueueCacheNames.values())
            {
            String           sCacheName = name.getCacheName(sName);
            NamedCache<?, ?> cache      = session.getCache(sCacheName);
            mapCaches.put(name, cache);
            assertThat(cache.isActive(), is(true));
            }

        queue.destroy();

        for (PagedQueueCacheNames name : PagedQueueCacheNames.values())
            {
            NamedCache<?, ?> cache = mapCaches.get(name);
            assertThat("Cache " + name + " should have been destroyed", cache.isDestroyed(), is(true));
            }
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
