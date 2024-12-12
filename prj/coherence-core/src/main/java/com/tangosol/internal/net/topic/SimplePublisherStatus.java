/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.topic;

import com.tangosol.io.AbstractEvolvable;
import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.EvolvablePortableObject;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import com.tangosol.net.topic.Position;
import com.tangosol.net.topic.Publisher;

import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.Objects;

/**
 * The status for a successfully published element.
 *
 * @author Jonathan Knight  2024.11.26
 */
public class SimplePublisherStatus
        extends AbstractEvolvable
        implements Publisher.Status, ExternalizableLite, EvolvablePortableObject
    {
    // ----- constructors -----------------------------------------------

    /**
     * Default constructor for serialization.
     */
    public SimplePublisherStatus()
        {
        }

    /**
     * Create a {@link Publisher.Status}.
     *
     * @param nChannel the channel the element was published to
     * @param position the {@link Position} of the offered element
     */
    public SimplePublisherStatus(int nChannel, Position position)
        {
        m_nChannel = nChannel;
        m_position = position;
        }

    // ----- accessor methods -------------------------------------------

    /**
     * Returns the channel that the element was published to.
     *
     * @return the channel that the element was published to
     */
    public int getChannel()
        {
        return m_nChannel;
        }

    /**
     * Returns the {@link Position} in the channel that the element was published to.
     *
     * @return the {@link Position} in the channel that the element was published to
     */
    public Position getPosition()
        {
        return m_position;
        }

    // ----- Object methods ---------------------------------------------

    @Override
    public String toString()
        {
        return "Publisher.Status(" +
                "channel=" + m_nChannel +
                ", position=" + m_position +
                ')';
        }

    @Override
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }
        if (o == null || getClass() != o.getClass())
            {
            return false;
            }
        SimplePublisherStatus that = (SimplePublisherStatus) o;
        return m_nChannel == that.m_nChannel && Objects.equals(m_position, that.m_position);
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(m_nChannel, m_position);
        }

    // ----- serialization ----------------------------------------------

    @Override
    public int getImplVersion()
        {
        return POF_IMPL_VERSION;
        }

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_nChannel = in.readInt(0);
        m_position = in.readObject(1);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeInt(0, m_nChannel);
        out.writeObject(1, m_position);
        }

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_nChannel = in.readInt();
        m_position = ExternalizableHelper.readObject(in);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        out.writeInt(m_nChannel);
        ExternalizableHelper.writeObject(out, m_position);
        }

    // ----- data members -----------------------------------------------

    private static final int POF_IMPL_VERSION = 0;

    /**
     * The channel number.
     */
    private int m_nChannel;

    /**
     * The position that the element was published to.
     */
    private Position m_position;
    }
