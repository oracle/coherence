/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.queue.processor;

import com.tangosol.internal.net.queue.model.QueueKey;
import com.tangosol.internal.net.queue.model.QueuePollResult;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.EvolvablePortableObject;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.util.InvocableMap;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * An entry processor to poll values from a queue.
 * <p>
 * A poll processor is invoked against a specific {@link QueueKey} for a queue.
 * The {@link QueueKey#getId()} should be {@link Long#MAX_VALUE} to poll from
 * the head and {@link Long#MIN_VALUE} to poll from the tail.
 *
 * @param <E>  the type of element in the queue
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class QueuePoll<E>
        extends AbstractQueueProcessor<QueueKey, E, QueuePollResult>
        implements EvolvablePortableObject, ExternalizableLite
    {
    @Override
    public QueuePollResult process(InvocableMap.Entry<QueueKey, E> entry)
        {
        if (entry.getKey().getId() == QueueKey.ID_TAIL)
            {
            return pollFromTail(entry.asBinaryEntry());
            }
        return pollFromHead(entry.asBinaryEntry());
        }

    // ----- EvolvablePortableObject methods --------------------------------

    @Override
    public int getImplVersion()
        {
        return IMPL_VERSION;
        }

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        }

    // ----- ExternalizableLite methods -------------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Return a singleton instance of {@link QueuePollResult}.
     *
     * @param <T> they type of element in the queue
     */
    public  static <T> QueuePoll<T> instance()
        {
        return INSTANCE;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link EvolvablePortableObject} implementation version.
     */
    public static final int IMPL_VERSION = 1;

    /**
     * A singleton instance of {@link QueuePollResult}.
     */
    public static final QueuePoll INSTANCE = new QueuePoll();
    }
