/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.queue;

import com.tangosol.internal.net.queue.model.QueueKey;
import com.tangosol.net.BackingMapManagerContext;
import com.tangosol.net.NamedCache;
import com.tangosol.net.QueueService;
import com.tangosol.net.queue.QueueStatistics;
import com.tangosol.util.Binary;
import com.tangosol.util.CollectionEvent;
import com.tangosol.util.CollectionListener;
import com.tangosol.util.Converter;
import com.tangosol.util.ConverterCollections;
import com.tangosol.util.Filter;

import java.util.Collection;
import java.util.Objects;

public class ConverterNamedMapQueue<FK, FV, TK, TV>
        extends ConverterCollections.ConverterCollection<FV, TV>
        implements NamedMapQueue<TK, TV>
    {
    public ConverterNamedMapQueue(NamedMapQueue<FK, FV> queue, Converter<FK, TK> convKeyUp, Converter<FV, TV> convUp,
                                  Converter<TK, FK> convKeyDown, Converter<TV, FV> convDown)
        {
        super(queue, convUp, convDown);
        f_queue       = queue;
        f_convKeyUp   = convKeyUp;
        f_convKeyDown = convKeyDown;
        }

    @Override
    public TK createKey(long id)
        {
        return f_convKeyUp.convert(f_queue.createKey(id));
        }

    @Override
    public boolean addAll(Collection<? extends TV> col)
        {
        if (col.contains(null))
            {
            throw new NullPointerException("null elements are not supported");
            }
        return super.addAll(col);
        }

    @Override
    public NamedCache<TK, TV> getNamedMap()
        {
        NamedCache<FK, FV> cache = (NamedCache<FK, FV>) f_queue.getNamedMap();
        return ConverterCollections.getNamedCache(cache, f_convKeyUp, f_convKeyDown, m_convUp, m_convDown);
        }

    @Override
    public boolean isReady()
        {
        return f_queue.isReady();
        }

    @Override
    public boolean isDestroyed()
        {
        return f_queue.isDestroyed();
        }

    @Override
    public boolean isReleased()
        {
        return f_queue.isReleased();
        }

    @Override
    public void close()
        {
        f_queue.close();
        }

    @Override
    public QueueService getService()
        {
        return f_queue.getService();
        }

    @Override
    public boolean isActive()
        {
        return f_queue.isActive();
        }

    @Override
    public QueueStatistics getQueueStatistics()
        {
        return f_queue.getQueueStatistics();
        }

    @Override
    public int getQueueNameHash()
        {
        return f_queue.getQueueNameHash();
        }

    @Override
    public long append(TV t)
        {
        return f_queue.append(getConverterDown().convert(t));
        }

    @Override
    public String getName()
        {
        return f_queue.getName();
        }

    @Override
    public void destroy()
        {
        f_queue.destroy();
        }

    @Override
    public void release()
        {
        f_queue.release();
        }

    @Override
    public void addListener(CollectionListener<? super TV> listener)
        {
        f_queue.addListener(new ConverterListener(listener));
        }

    @Override
    public void removeListener(CollectionListener<? super TV> listener)
        {
        f_queue.removeListener(new ConverterListener(listener));
        }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void addListener(CollectionListener<? super TV> listener, Filter<TV> filter, boolean fLite)
        {
        f_queue.addListener(new ConverterListener(listener), (Filter) filter, fLite);
        }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void removeListener(CollectionListener<? super TV> listener, Filter<TV> filter)
        {
        f_queue.removeListener(new ConverterListener(listener), (Filter) filter);
        }

    @Override
    public boolean offer(TV t)
        {
        return f_queue.offer(getConverterDown().convert(t));
        }

    @Override
    public TV remove()
        {
        return getConverterUp().convert(f_queue.remove());
        }

    @Override
    public TV poll()
        {
        return getConverterUp().convert(f_queue.poll());
        }

    @Override
    public TV element()
        {
        return getConverterUp().convert(f_queue.element());
        }

    @Override
    public TV peek()
        {
        return getConverterUp().convert(f_queue.peek());
        }



    // ----- factory methods ------------------------------------------------

    @SuppressWarnings("unchecked")
    public static <K, E> NamedMapQueue<K, E> createQueue(NamedMapQueue<Binary, Binary> queue)
        {
        BackingMapManagerContext context     = queue.getService().getBackingMapManager().getContext();
        Converter<Binary, K>     convKeyUp   = context.getKeyFromInternalConverter();
        Converter<K, Binary>     convKeyDown = context.getKeyToInternalConverter();
        Converter<Binary, E>     convValUp   = context.getKeyFromInternalConverter();
        Converter<E, Binary>     convValDown = context.getKeyToInternalConverter();
        return new ConverterNamedMapQueue<>(queue, convKeyUp, convValUp, convKeyDown, convValDown);
        }

    // ----- inner class: ConverterListener ---------------------------------

    private class ConverterListener
            implements CollectionListener<FV>
        {
        @SuppressWarnings("unchecked")
        public ConverterListener(CollectionListener<? super TV> listener)
            {
            f_listener = (CollectionListener<TV>) listener;
            }

        @Override
        public void entryInserted(CollectionEvent<FV> evt)
            {
            f_listener.entryInserted(new ConverterEvent(evt));
            }

        @Override
        public void entryUpdated(CollectionEvent<FV> evt)
            {
            f_listener.entryUpdated(new ConverterEvent(evt));
            }

        @Override
        public void entryDeleted(CollectionEvent<FV> evt)
            {
            f_listener.entryDeleted(new ConverterEvent(evt));
            }

        @Override
        public int characteristics()
            {
            return f_listener.characteristics();
            }

        @Override
        public boolean isAsynchronous()
            {
            return f_listener.isAsynchronous();
            }

        @Override
        public boolean isSynchronous()
            {
            return f_listener.isSynchronous();
            }

        @Override
        public boolean isVersionAware()
            {
            return f_listener.isVersionAware();
            }

        @Override
        @SuppressWarnings("unchecked")
        public boolean equals(Object o)
            {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ConverterListener that = (ConverterListener) o;
            return Objects.equals(f_listener, that.f_listener);
            }

        @Override
        public int hashCode()
            {
            return Objects.hashCode(f_listener);
            }

        // ----- data members -----------------------------------------------

        private final CollectionListener<TV> f_listener;
        }

    // ----- inner class ConverterEvent -------------------------------------

    protected class ConverterEvent
            implements CollectionEvent<TV>
        {
        public ConverterEvent(CollectionEvent<FV> event)
            {
            f_event = event;
            }

        @Override
        public int getId()
            {
            return f_event.getId();
            }

        @Override
        public TV getOldValue()
            {
            return getConverterUp().convert(f_event.getOldValue());
            }

        @Override
        public TV getNewValue()
            {
            return getConverterUp().convert(f_event.getNewValue());
            }

        @Override
        public int getPartition()
            {
            return f_event.getPartition();
            }

        @Override
        public long getVersion()
            {
            return f_event.getVersion();
            }

        @Override
        public boolean isInsert()
            {
            return f_event.isInsert();
            }

        @Override
        public boolean isUpdate()
            {
            return f_event.isUpdate();
            }

        @Override
        public boolean isDelete()
            {
            return f_event.isDelete();
            }

        // ----- data members -----------------------------------------------

        private final CollectionEvent<FV> f_event;
        }

    // ----- data members ---------------------------------------------------

    protected final NamedMapQueue<FK, FV> f_queue;

    protected final Converter<FK, TK> f_convKeyUp;

    protected final Converter<TK, FK> f_convKeyDown;
    }
