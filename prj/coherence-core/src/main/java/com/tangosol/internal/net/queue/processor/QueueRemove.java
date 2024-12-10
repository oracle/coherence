/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.queue.processor;

import com.oracle.coherence.common.base.Logger;
import com.tangosol.internal.net.queue.model.QueueKey;
import com.tangosol.io.pof.EvolvablePortableObject;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.LiteMap;

import javax.json.bind.annotation.JsonbProperty;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * An entry processor to remove entries from a queue.
 *
 * @param <E>  the type of element in the queue
 */
public class QueueRemove<E>
        extends AbstractQueueProcessor<QueueKey, E, Boolean>
    {
    /**
     * Default constructor required for serialization.
     */
    public QueueRemove()
        {
        }

    /**
     * Create a {@link QueueRemove} processor.
     *
     * @param m_nTarget  the target of the remove operation
     */
    private QueueRemove(int m_nTarget)
        {
        this.m_nTarget = m_nTarget;
        }

    @Override
    public Map<QueueKey, Boolean> processAll(Set<? extends InvocableMap.Entry<QueueKey, E>> setEntries)
        {
        Map<QueueKey, Boolean> mapResult = new LiteMap<>();
        switch (m_nTarget) {
        case TARGET_FIRST:
        case TARGET_LAST:
            TreeMap<QueueKey, InvocableMap.Entry<QueueKey, E>> sorted = new TreeMap<>();
            for (InvocableMap.Entry<QueueKey, E> entry : setEntries)
                {
                if (entry.isPresent())
                    {
                    sorted.put(entry.getKey(), entry);
                    }
                }
            if (!sorted.isEmpty())
                {
                QueueKey key = m_nTarget == TARGET_FIRST ? sorted.firstKey() : sorted.lastKey();
                sorted.get(key).remove(false);
                mapResult.put(key, true);
                }
            break;
        case TARGET_ALL:
            QueueKey key = null;
            for (InvocableMap.Entry<QueueKey, E> entry : setEntries)
                {
                if (entry.isPresent())
                    {
                    key = entry.getKey();
                    entry.remove(false);
                    }
                }
            if (key != null)
                {
                mapResult.put(key, true);
                }
            break;
        default:
            Logger.warn("Entry processor " + getClass().getSimpleName()
                    + " invoked with invalid target " + m_nTarget);
            break;
        }
        return mapResult;
        }

    @Override
    public Boolean process(InvocableMap.Entry<QueueKey, E> entry)
        {
        if (entry.isPresent())
            {
            entry.remove(false);
            return true;
            }
        return false;
        }

    @Override
    public int getImplVersion()
        {
        return IMPL_VERSION;
        }

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_nTarget = in.readInt(0);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeInt(0, m_nTarget);
        }

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_nTarget = in.readInt();
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        out.writeInt(m_nTarget);
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Return a {@link QueueRemove} to remove the first matching
     * element from the queue.
     *
     * @param <E>  the type of element in the queue
     *
     * @return a {@link QueueRemove} to remove the first matching
     *         element from the queue
     */
    @SuppressWarnings("unchecked")
    public static <E> QueueRemove<E> removeFirst()
        {
        return (QueueRemove<E>) INSTANCE_FIRST;
        }


    /**
     * Return a {@link QueueRemove} to remove the first matching
     * element from the queue.
     *
     * @param <E>  the type of element in the queue
     *
     * @return a {@link QueueRemove} to remove the first matching
     *         element from the queue
     */
    @SuppressWarnings("unchecked")
    public static <E> QueueRemove<E> removeLast()
        {
        return (QueueRemove<E>) INSTANCE_LAST;
        }


    /**
     * Return a {@link QueueRemove} to remove the last matching
     * element from the queue.
     *
     * @param <E>  the type of element in the queue
     *
     * @return a {@link QueueRemove} to remove all the matching
     *         elements from the queue
     */
    @SuppressWarnings("unchecked")
    public static <E> QueueRemove<E> removeAll()
        {
        return (QueueRemove<E>) INSTANCE_ALL;
        }

    // ----- constants ------------------------------------------------------

    /**
     * The {@link EvolvablePortableObject} implementation version.
     */
    public static final int IMPL_VERSION = 1;

    /**
     * The target value to indicate the first element should be removed.
     */
    public static final int TARGET_FIRST = 1;

    /**
     * The target value to indicate the last element should be removed.
     */
    public static final int TARGET_LAST = 2;

    /**
     * The target value to indicate the all elements should be removed.
     */
    public static final int TARGET_ALL = 3;

    private static final QueueRemove<?> INSTANCE_FIRST = new QueueRemove<>(TARGET_FIRST);

    private static final QueueRemove<?> INSTANCE_LAST = new QueueRemove<>(TARGET_LAST);

    private static final QueueRemove<?> INSTANCE_ALL = new QueueRemove<>(TARGET_ALL);

    // ----- data members ---------------------------------------------------

    @JsonbProperty("target")
    private int m_nTarget;
    }
