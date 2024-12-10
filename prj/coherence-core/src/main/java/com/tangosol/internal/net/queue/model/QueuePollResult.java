/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.queue.model;

import com.tangosol.io.AbstractEvolvable;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.EvolvablePortableObject;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;

import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbTransient;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * The result of invoking a {@link com.tangosol.internal.net.queue.processor.QueuePoll}.
 */
public class QueuePollResult
        extends AbstractEvolvable
        implements ExternalizableLite, EvolvablePortableObject
    {
    /**
     * Default constructor for serialization.
     */
    public QueuePollResult()
        {
        }

    /**
     * Create a {@link QueuePollResult}.
     *
     * @param id  the id of the polled element
     */
    public QueuePollResult(long id)
        {
        this(id, null);
        }

    /**
     * Create a {@link QueuePollResult}.
     *
     * @param idMSB       the most significant bits of the id of the polled element
     * @param idLSB       the least significant bits of the id of the polled element
     * @param binElement  the serialized {@link Binary} value polled from the queue
     *                    or {@code null} if the queue was empty
     */
    public QueuePollResult(int idMSB, int idLSB, Binary binElement)
        {
        this((((long) idMSB) << 32) | (idLSB & 0xffffffffL), binElement);
        }

    /**
     * Create a {@link QueuePollResult}.
     *
     * @param id          the id of the polled element
     * @param binElement  the serialized {@link Binary} value polled from the queue
     *                    or {@code null} if the queue was empty
     */
    public QueuePollResult(long id, Binary binElement)
        {
        m_id         = id;
        m_binElement = binElement;
        }

    /**
     * Return the id of the polled element.
     *
     * @return the id of the polled element
     */
    public long getId()
        {
        return m_id;
        }

    /**
     * Return the serialized binary value of the polled element,
     * or {@code null} if the queue was empty.
     *
     * @return the serialized binary value of the polled element,
     *         or {@code null} if the queue was empty
     */
    public Binary getBinaryElement()
        {
        return m_binElement;
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
        m_id         = in.readLong(0);
        m_binElement = in.readBinary(1);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeLong(0, m_id);
        out.writeBinary(1, m_binElement);
        }

    // ----- ExternalizableLite methods -------------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_id         = in.readLong();
        m_binElement = ExternalizableHelper.readObject(in);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        out.writeLong(m_id);
        ExternalizableHelper.writeObject(out, m_binElement);
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Return an empty {@link QueuePollResult}.
     *
     * @return an empty {@link QueuePollResult}
     */
    public static QueuePollResult empty()
        {
        return new QueuePollResult(RESULT_EMPTY);
        }

    /**
     * Return a poll next page {@link QueuePollResult}.
     *
     * @return a poll next page {@link QueuePollResult}
     */
    public static QueuePollResult nextPage()
        {
        return new QueuePollResult(RESULT_POLL_NEXT_PAGE);
        }

    // ----- constants ------------------------------------------------------

    public static final long RESULT_EMPTY = Long.MIN_VALUE;

    public static final long RESULT_POLL_NEXT_PAGE = Long.MIN_VALUE + 1;

    /**
     * The {@link EvolvablePortableObject} version of this class.
     */
    public static final int IMPL_VERSION = 1;

    // ----- data members ---------------------------------------------------

    /**
     * The serialized binary polled element.
     */
    private Binary m_binElement;

    /**
     * The id of the polled element.
     */
    private long m_id;
    }
