/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.queue;

import com.tangosol.internal.net.NamedMapValuesCollection;
import com.tangosol.internal.net.queue.extractor.QueueKeyExtractor;
import com.tangosol.internal.net.queue.model.QueueKey;
import com.tangosol.internal.net.queue.model.QueueOfferResult;
import com.tangosol.internal.net.queue.model.QueuePollResult;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.CacheService;
import com.tangosol.net.NamedMap;
import com.tangosol.net.NamedQueue;
import com.tangosol.net.QueueService;
import com.tangosol.net.queue.MutableQueueStatistics;
import com.tangosol.net.queue.QueueStatistics;
import com.tangosol.util.Binary;
import com.tangosol.util.CollectionListener;
import com.tangosol.util.Converter;
import com.tangosol.util.Filter;
import com.tangosol.util.transformer.MapListenerCollectionListener;

import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * A base class for {@link NamedQueue} implementations that wraps a {@link NamedMap}.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public abstract class BaseBinaryNamedMapQueue
        extends NamedMapValuesCollection<Binary, Binary>
        implements NamedMapQueue<Binary, Binary>
    {
    /**
     * Create a {@link BaseBinaryNamedMapQueue} that wrap s a {@link NamedMap}.
     *
     * @param sName  the name of the cache to wrap
     * @param map    the {@link NamedMap} to wrap
     *
     * @throws NullPointerException      if either of the {@code name} or {@code cache} parameters is {@code null}
     * @throws IllegalArgumentException  if the name is blank
     */
    public BaseBinaryNamedMapQueue(String sName, NamedMap<Binary, Binary> map)
        {
        super(Objects.requireNonNull(sName), Objects.requireNonNull(map));

        CacheService             service = map.getService();
        BackingMapManagerContext context = service.getBackingMapManager().getContext();

        m_service                    = ensureQueueService(service);
        m_converterKeyToInternal     = context.getKeyToInternalConverter();
        m_converterValueToInternal   = context.getValueToInternalConverter();
        m_converterValueFromInternal = context.getValueFromInternalConverter();
        m_keyHead                    = QueueKey.head(m_sName);
        m_nHash                      = m_keyHead.getHash();
        m_binKeyHead                 = m_converterKeyToInternal.convert(m_keyHead);
        m_keyTail                    = QueueKey.tail(m_sName);
        m_binKeyTail                 = m_converterKeyToInternal.convert(m_keyTail);
        m_statistics             = new SimpleQueueStatistics();

        m_cache.addIndex(QueueKeyExtractor.instance(), true, null);
        }

    // ----- NamedQueue methods ---------------------------------------------

    @Override
    public Binary createKey(long id)
        {
        return m_converterKeyToInternal.convert(new QueueKey(m_nHash, id));
        }

    @Override
    public QueueService getService()
        {
        return m_service;
        }

    @Override
    public QueueStatistics getQueueStatistics()
        {
        return m_statistics;
        }

    @Override
    public int getQueueNameHash()
        {
        return m_nHash;
        }

    @Override
    public void addListener(CollectionListener<? super Binary> listener)
        {
        MapListenerCollectionListener<Binary, Binary> wrapper = new MapListenerCollectionListener(listener);
        m_cache.addMapListener(wrapper);
        }

    @Override
    public void removeListener(CollectionListener<? super Binary> listener)
        {
        MapListenerCollectionListener<Binary, Binary> wrapper = new MapListenerCollectionListener(listener);
        m_cache.removeMapListener(wrapper);
        }

    @Override
    public void addListener(CollectionListener<? super Binary> listener, Filter<Binary> filter, boolean fLite)
        {
        MapListenerCollectionListener<Binary, Binary> wrapper = new MapListenerCollectionListener(listener);
        m_cache.addMapListener(wrapper, filter, fLite);
        }

    @Override
    public void removeListener(CollectionListener<? super Binary> listener, Filter<Binary> filter)
        {
        MapListenerCollectionListener<Binary, Binary> wrapper = new MapListenerCollectionListener(listener);
        m_cache.removeMapListener(wrapper, filter);
        }

    // ----- AbstractQueue methods ------------------------------------------

    @Override
    public boolean add(Binary e)
        {
        return offer(e);
        }

    @Override
    public boolean addAll(Collection<? extends Binary> c)
        {
        assertNotSameCollection(c, "This collection cannot be added to itself or the same underlying cache");
        for (Binary e : c)
            {
            if (!offer(e))
                {
                return false;
                }
            }
        return true;
        }

    @Override
    public Binary element()
        {
        QueuePollResult result = peekAtHeadInternal();
        Binary          binary = result.getBinaryElement();
        if (binary != null)
            {
            m_statistics.registerHit();
            return binary;
            }
        m_statistics.registerMiss();
        throw new NoSuchElementException();
        }

    @Override
    public long append(Binary e)
        {
        assertNotNull(e);
        QueueOfferResult result = offerToTailInternal(e);
        boolean fSuccess = result.getResult() == QueueOfferResult.RESULT_SUCCESS;
        if (fSuccess)
            {
            m_statistics.registerAccepted();
            }
        else
            {
            m_statistics.registerRejected();
            }
        return fSuccess ? result.getId() : Long.MIN_VALUE;
        }

    @Override
    public boolean offer(Binary e)
        {
        long id = append(e);
        return id >= 0L;
        }

    @Override
    public Binary peek()
        {
        QueuePollResult result = peekAtHeadInternal();
        Binary          binary = result.getBinaryElement();
        if (binary == null)
            {
            m_statistics.registerMiss();
            }
        else
            {
            m_statistics.registerHit();
            }
        return binary;
        }

    @Override
    public Binary poll()
        {
        QueuePollResult result = pollFromHeadInternal();
        Binary          binary = result.getBinaryElement();
        if (binary == null)
            {
            m_statistics.registerMiss();
            }
        else
            {
            m_statistics.registerHit();
            }
        return binary;
        }

    @Override
    public Binary remove()
        {
        QueuePollResult result = pollFromHeadInternal();
        Binary          binary = result.getBinaryElement();
        if (binary != null)
            {
            m_statistics.registerHit();
            return binary;
            }
        m_statistics.registerMiss();
        throw new NoSuchElementException();
        }

    @Override
    public abstract Iterator<Binary> iterator();

    @Override
    public Object[] toArray()
        {
        return toArrayInternal(null);
        }

    @Override
    public <T> T[] toArray(T[] ao)
        {
        return (T[]) toArrayInternal(ao);
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public String toString()
        {
        StringBuilder s = new StringBuilder()
                .append(getClass().getSimpleName())
                .append("{name=\"")
                .append(m_sName)
                .append("\"")
                .append(", metrics={");
        getQueueStatistics().logTo(s);
        s.append("}");
        return s.toString();
        }

    // ----- helper methods -------------------------------------------------

    protected static QueueService ensureQueueService(CacheService cacheService)
        {
        if (cacheService instanceof QueueService)
            {
            return (QueueService) cacheService;
            }
        return new CacheQueueService(cacheService);
        }

    protected abstract QueueOfferResult offerToTailInternal(Binary e);

    protected abstract QueuePollResult pollFromHeadInternal();

    protected abstract QueuePollResult peekAtHeadInternal();

    protected Object[] toArrayInternal(Object[] ao)
        {
        int c = size();
        if (ao == null)
            {
            ao = new Object[c];
            }
        else if (ao.length < c)
            {
            ao = (Object[])java.lang.reflect.Array.newInstance(
                ao.getClass().getComponentType(), c);
            }
        else if (ao.length > c)
            {
            ao[c] = null;
            }

        Iterator iter = iterator();
        for (int i = 0; i < c; i++)
            {
            try
                {
                ao[i] = iter.next();
                }
            catch (ArrayStoreException e) // NoSuchElement; IndexOutOfBounds
                {
                throw e;
                }
            catch (RuntimeException e) // NoSuchElement; IndexOutOfBounds
                {
                throw new ConcurrentModificationException("element " + i + ": " + e.getMessage());
                }
            }

        return ao;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link QueueService}.
     */
    protected final QueueService m_service;

    /**
     * The {@link MutableQueueStatistics} to use.
     */
    protected final MutableQueueStatistics m_statistics;

    protected final Converter<Object, Binary> m_converterKeyToInternal;

    protected final Converter<Object, Binary> m_converterValueToInternal;

    protected final Converter<Binary, Object> m_converterValueFromInternal;

    protected final int m_nHash;

    protected final QueueKey m_keyHead;

    /**
     * The key to use to invoke operations against the queue head
     */
    protected final Binary m_binKeyHead;

    protected final QueueKey m_keyTail;

    /**
     * The key to use to invoke operations against the queue tail
     */
    protected final Binary m_binKeyTail;
    }
