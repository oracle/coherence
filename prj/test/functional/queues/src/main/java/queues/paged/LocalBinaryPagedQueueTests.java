/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package queues.paged;

import com.tangosol.coherence.config.scheme.PagedQueueScheme;
import com.tangosol.internal.net.queue.ConverterNamedMapQueue;
import com.tangosol.internal.net.queue.NamedMapQueue;
import com.tangosol.internal.net.queue.PagedQueue;
import com.tangosol.internal.net.queue.paged.BinaryPagedNamedQueue;
import com.tangosol.internal.net.queue.paged.PagedQueueCacheNames;
import com.tangosol.net.NamedMap;
import com.tangosol.net.NamedQueue;
import com.tangosol.net.Session;
import com.tangosol.net.options.WithClassLoader;
import com.tangosol.util.Binary;
import com.tangosol.util.NullImplementation;

@SuppressWarnings({"rawtypes", "unchecked"})
public class LocalBinaryPagedQueueTests<QueueType extends NamedQueue>
        extends LocalPagedQueueTests<QueueType>
    {
    @Override
    protected NamedMap<?, ?> getQueueCache(String sQueueName, PagedQueueCacheNames name, Session session)
        {
        String sCacheName = name.getCacheName(sQueueName);
        if (name.isPassThru())
            {
            return session.getCache(sCacheName, WithClassLoader.nullImplementation());
            }
        return session.getCache(sCacheName);
        }

    @Override
    @SuppressWarnings("unchecked")
    public QueueType getNamedCollection(Session session, String sName)
        {
        NamedMapQueue<Binary, Binary> queue = (NamedMapQueue<Binary, Binary>) PagedQueueScheme.INSTANCE.realize(sName, session, NullImplementation.getClassLoader());
        return (QueueType) ConverterNamedMapQueue.createQueue(queue);
        }

    @Override
    @SuppressWarnings("unchecked")
    public QueueType getCollection(Session session, String sName)
        {
        return (QueueType) ConverterNamedMapQueue.createQueue(new BinaryPagedNamedQueue(sName, session));
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
    }
