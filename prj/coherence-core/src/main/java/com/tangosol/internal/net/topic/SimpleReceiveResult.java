/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.topic;

import com.oracle.coherence.common.base.Classes;

import com.tangosol.io.AbstractEvolvable;
import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.EvolvablePortableObject;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import com.tangosol.net.topic.Position;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.LinkedList;
import java.util.Queue;

/**
 * The result of a subscriber receive request.
 *
 * @author Jonathan Knight  2024.11.26
 */
public class SimpleReceiveResult
        extends AbstractEvolvable
        implements ReceiveResult, ExternalizableLite, EvolvablePortableObject
    {
    /**
     * Default constructor for serialization.
     */
    public SimpleReceiveResult()
        {
        }

    /**
     * Create a {@link SimpleReceiveResult}.
     *
     * @param elements    the elements polled from the topic
     * @param cRemaining  the number of remaining elements
     * @param status      the status of the poll operation
     */
    public SimpleReceiveResult(Queue<Binary> elements, int cRemaining, Status status)
        {
        this(elements, cRemaining, status, null);
        }

    /**
     * Create a {@link SimpleReceiveResult}.
     *
     * @param elements    the elements polled from the topic
     * @param cRemaining  the number of remaining elements
     * @param status      the status of the poll operation
     * @param head        the channel head position
     */
    public SimpleReceiveResult(Queue<Binary> elements, int cRemaining, Status status, Position head)
        {
        m_elements   = elements;
        m_cRemaining = Math.max(cRemaining, 0);
        m_status     = status;
        m_head       = head;
        }

    @Override
    public Queue<Binary> getElements()
        {
        return m_elements;
        }

    @Override
    public int getRemainingElementCount()
        {
        return m_cRemaining;
        }

    @Override
    public Status getStatus()
        {
        return m_status;
        }

    @Override
    public int getImplVersion()
        {
        return POF_IMPL_VERSION;
        }

    public Position getHead()
        {
        return m_head;
        }

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_elements   = in.readCollection(0, new LinkedList<>());
        m_cRemaining = in.readInt(1);
        m_status     = in.readObject(2);
        m_head       = in.readObject(3);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeCollection(0, m_elements, Binary.class);
        out.writeInt(1, m_cRemaining);
        out.writeObject(2, m_status);
        out.writeObject(3, m_head);
        }

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_elements = new LinkedList<>();
        ExternalizableHelper.readCollection(in, m_elements, Classes.getContextClassLoader());
        m_cRemaining = in.readInt();
        m_status     = ExternalizableHelper.readObject(in);
        m_head       = ExternalizableHelper.readObject(in);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeCollection(out, m_elements);
        out.writeInt(m_cRemaining);
        ExternalizableHelper.writeObject(out, m_status);
        ExternalizableHelper.writeObject(out, m_head);
        }

    @Override
    public String toString()
        {
        return "SimpleReceiveResult{" +
                " elements=" + m_elements.size() +
                ", remaining=" + m_cRemaining +
                ", status=" + m_status +
                ", head=" + m_head +
                '}';
        }

    // ----- data members ---------------------------------------------------

    /**
     * The POF implementation version of this class.
     */
    public static final int POF_IMPL_VERSION = 0;

    /**
     * The elements polled from the topic.
     */
    private Queue<Binary> m_elements;

    /**
     * The number of remaining elements.
     */
    private int m_cRemaining;

    /**
     * The status of the receive operation.
     */
    private Status m_status;

    /**
     * The channel head position.
     */
    private Position m_head;
    }
