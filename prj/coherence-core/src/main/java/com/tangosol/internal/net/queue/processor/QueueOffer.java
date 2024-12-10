/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.queue.processor;

import com.tangosol.internal.net.queue.model.QueueKey;
import com.tangosol.internal.net.queue.model.QueueOfferResult;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.EvolvablePortableObject;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap;

import javax.json.bind.annotation.JsonbProperty;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * An entry processor to offer values to a queue.
 * <p>
 * An offer processor is invoked against a random {@link QueueKey} for a queue.
 * The {@link QueueKey#getId()} should be positive to offer to the tail and
 * negative to offer to the head, it should not be zero.
 *
 * @param <E>  the type of element in the queue
 */
public class QueueOffer<E>
        extends AbstractQueueProcessor<QueueKey, E, QueueOfferResult>
        implements EvolvablePortableObject, ExternalizableLite
    {
    /**
     * Default constructor required for serialization.
     */
    public QueueOffer()
        {
        }

    /**
     * Create a {@link QueueOffer}.
     *
     * @param oValue  the value to offer to the queue
     */
    public QueueOffer(E oValue)
        {
        m_oValue = oValue;
        m_binary = null;
        }

    /**
     * Create a {@link QueueOffer}.
     *
     * @param binary   the {@link Binary} value to offer to the queue
     */
    public QueueOffer(Binary binary)
        {
        m_oValue = null;
        m_binary = binary;
        }

    @Override
    public QueueOfferResult process(InvocableMap.Entry<QueueKey, E> entry)
        {
        if (entry.getKey().getId() < 0)
            {
            return offerToHead(entry.asBinaryEntry(), m_binary, m_oValue);
            }
        return offerToTail(entry.asBinaryEntry(), m_binary, m_oValue);
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
        m_binary = in.readBinary(0);
        m_oValue = in.readObject(1);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeBinary(0, m_binary);
        out.writeObject(1, m_oValue);
        }

    // ----- ExternalizableLite methods -------------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_binary = ExternalizableHelper.readObject(in);
        m_oValue = ExternalizableHelper.readObject(in);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeObject(out, m_binary);
        ExternalizableHelper.writeObject(out, m_oValue);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link EvolvablePortableObject} implementation version.
     */
    public static final int IMPL_VERSION = 1;

    /**
     * The binary value to offer.
     */
    @JsonbProperty("binary")
    private Binary m_binary;

    /**
     * The Object value to offer.
     */
    @JsonbProperty("value")
    private E m_oValue;
    }
