/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package concurrent.queues;

import com.oracle.coherence.concurrent.Queues;
import com.tangosol.coherence.component.net.extend.remoteService.RemoteCacheService;
import com.tangosol.coherence.component.util.safeService.SafeCacheService;
import com.tangosol.internal.net.queue.CacheQueueService;
import com.tangosol.internal.net.queue.NamedCacheBlockingDeque;
import com.tangosol.net.CacheService;
import com.tangosol.net.Coherence;
import com.tangosol.net.NamedBlockingDeque;
import com.tangosol.net.NamedCache;
import com.tangosol.net.QueueService;
import com.tangosol.net.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import queues.ExtendClientDequeTests;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@SuppressWarnings({"rawtypes", "unchecked"})
public class ExtendClientBlockingDequeTests<QueueType extends NamedBlockingDeque>
        extends ExtendClientDequeTests<QueueType>
        implements NamedBlockingDequeTests<QueueType>
    {
    @ParameterizedTest(name = "{index} serializer={0}")
    @MethodSource("serializers")
    public void shouldHaveCorrectSerializer(String sSerializer)
        {
        // overridden because the test is not really able to switch serializers for
        // the concurrent session. It does not matter for this as everything to do
        // with serialization is already tested by other tests.
        }

    @Override
    public Session getSession()
        {
        return Coherence.getInstance().getSession(Queues.SESSION_NAME);
        }

    @Override
    public QueueType getNamedCollection(Session session, String sName)
        {
        return (QueueType) session.getDeque(sName, Queues.BUILDER);
        }

    @Override
    public NamedCache getCollectionCache(String sName)
        {
        return super.getCollectionCache(NamedCacheBlockingDeque.Builder.getCacheName(Queues.QUEUE_CACHE_PREFIX, sName));
        }

    @Test
    public void shouldBeRemoteBlockingQueue()
        {
        NamedBlockingDeque queue   = getNewCollection();
        QueueService       service = queue.getService();

        assertThat(service, is(instanceOf(CacheQueueService.class)));
        CacheService cacheService = ((CacheQueueService) service).getCacheService();
        if (cacheService instanceof SafeCacheService)
            {
            cacheService = ((SafeCacheService) cacheService).getRunningCacheService();
            }
        assertThat(cacheService, is(instanceOf(RemoteCacheService.class)));
        }
    }
