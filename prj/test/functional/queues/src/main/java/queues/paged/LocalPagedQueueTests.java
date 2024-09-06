/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package queues.paged;

import com.oracle.coherence.common.base.Randoms;
import com.tangosol.coherence.config.scheme.PagedQueueScheme;
import com.tangosol.internal.net.queue.PagedQueue;
import com.tangosol.internal.net.queue.paged.PagedQueueCacheNames;
import com.tangosol.net.Coherence;
import com.tangosol.net.NamedMap;
import com.tangosol.net.NamedQueue;
import com.tangosol.net.QueueService;
import com.tangosol.net.Session;
import com.tangosol.util.Binary;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
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

        Map<PagedQueueCacheNames, NamedMap<?, ?>> mapCaches = new HashMap<>();

        for (PagedQueueCacheNames name : PagedQueueCacheNames.values())
            {
            NamedMap<?, ?> cache = getQueueCache(sName, name, session);
            mapCaches.put(name, cache);
            assertThat(cache.isActive(), is(true));
            }

        queue.release();

        for (PagedQueueCacheNames name : PagedQueueCacheNames.values())
            {
            NamedMap<?, ?> cache = mapCaches.get(name);
            assertThat("Cache " + name + " should have been released", cache.isReleased(), is(true));
            }
        }

    @Test
    public void shouldDestroyAllCaches()
        {
        String             sName      = getNewName();
        NamedQueue<String> queue      = getNamedCollection(sName);
        Session            session    = getSession();

        Map<PagedQueueCacheNames, NamedMap<?, ?>> mapCaches = new HashMap<>();

        for (PagedQueueCacheNames name : PagedQueueCacheNames.values())
            {
            NamedMap<?, ?> cache = getQueueCache(sName, name, session);
            mapCaches.put(name, cache);
            assertThat(cache.isActive(), is(true));
            }

        queue.destroy();

        for (PagedQueueCacheNames name : PagedQueueCacheNames.values())
            {
            NamedMap<?, ?> cache = mapCaches.get(name);
            assertThat("Cache " + name + " should have been destroyed", cache.isDestroyed(), is(true));
            }
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldAcceptMessageSameAsMaxPageSize(String sSerializer)
        {
        QueueType queue  = getNewCollection(sSerializer);
        String    sValue = Randoms.getRandomString(PagedQueue.DEFAULT_PAGE_CAPACITY_BYTES, PagedQueue.DEFAULT_PAGE_CAPACITY_BYTES, true);

        assertThat(queue.add(sValue), is(true));

        NamedMap<?, ?> cache = getCollectionCache(queue.getName());
        assertThat(cache.size(), is(1));

        Object oKey   = cache.keySet().iterator().next();
        Object oValue = cache.get(oKey);
        assertThat(oValue, is(sValue));

        assertThat(queue.poll(), is(sValue));
        }

    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldAcceptMessageBiggerThanMaxPageSize(String sSerializer)
        {
        QueueType queue  = getNewCollection(sSerializer);
        int       nSize  = PagedQueue.DEFAULT_PAGE_CAPACITY_BYTES + 1000;
        String    sValue = Randoms.getRandomString(nSize, nSize, true);

        assertThat(queue.add(sValue), is(true));

        NamedMap<?, ?> cache = getCollectionCache(queue.getName());
        assertThat(cache.size(), is(1));

        Object oKey   = cache.keySet().iterator().next();
        Object oValue = cache.get(oKey);
        assertThat(oValue, is(sValue));

        assertThat(queue.poll(), is(sValue));
        }

    protected NamedMap<?, ?> getQueueCache(String sQueueName, PagedQueueCacheNames name, Session session)
        {
        String sCacheName = name.getCacheName(sQueueName);
        return session.getCache(sCacheName);
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
        return (QueueType) PagedQueueScheme.INSTANCE.realize(sName, session);
        }

    @Override
    @SuppressWarnings("unchecked")
    public QueueType getCollection(Session session, String sName)
        {
        return (QueueType) PagedQueueScheme.INSTANCE.realize(sName, session);
        }

    @Override
    public NamedMap getCollectionCache(String sName)
        {
        return super.getCollectionCache(PagedQueueCacheNames.Elements.getCacheName(sName));
        }

    @Override
    public NamedMap<Binary, Binary> getCollectionBinaryCache(String sName)
        {
        return super.getCollectionBinaryCache(PagedQueueCacheNames.Elements.getCacheName(sName));
        }

    // ----- data members ---------------------------------------------------

    private static Session m_session;
    }
