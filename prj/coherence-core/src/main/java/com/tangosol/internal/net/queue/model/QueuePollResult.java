/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.internal.net.queue.model;

import com.tangosol.io.AbstractEvolvable;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.SerializationSupport;
import com.tangosol.io.Serializer;
import com.tangosol.io.SerializerAware;
import com.tangosol.io.pof.EvolvablePortableObject;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.util.Binary;
import com.tangosol.util.ExternalizableHelper;
import jakarta.json.bind.annotation.JsonbCreator;
import jakarta.json.bind.annotation.JsonbNillable;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbTransient;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.ObjectStreamException;

public class QueuePollResult
        extends AbstractEvolvable
        implements ExternalizableLite, EvolvablePortableObject, SerializerAware
    {
    public QueuePollResult()
        {
        }

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
     * Return the serialized binary value of the polled element.
     *
     * @return the serialized binary value of the polled element
     */
    public Binary getBinaryElement()
        {
        return m_binElement;
        }

    /**
     * Return the deserialized object form of the polled element.
     *
     * @return the deserialized object form of the polled element
     */
    @SuppressWarnings("unchecked")
    public <E> E getElement()
        {
        return (E) m_oElement;
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
        m_id = in.readLong();
        m_binElement = ExternalizableHelper.readObject(in);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        out.writeLong(m_id);
        ExternalizableHelper.writeObject(out, m_binElement);
        }

    // ----- SerializerAware methods ----------------------------------------

    @Override
    public Serializer getContextSerializer()
        {
        return m_serializer;
        }

    @Override
    public void setContextSerializer(Serializer serializer)
        {
        m_serializer = serializer;
        if (m_binElement != null)
            {
            m_oElement = ExternalizableHelper.fromBinary(m_binElement, m_serializer);
            }
        }

    // ----- constants ------------------------------------------------------

    public static final int IMPL_VERSION = 1;

    // ----- data members ---------------------------------------------------

    /**
     * The serialized binary polled element.
     */
    @JsonbTransient
    private Binary m_binElement;

    /**
     * The id of the polled element.
     */
    @JsonbProperty("id")
    private long m_id;

    /**
     * The serializer used to serialize or deserialize this instance.
     */
    private transient Serializer m_serializer;

    /**
     * A flag to indicate the {@link #m_oElement} field was deserialized from the binary value.
     */
    private transient boolean m_fHasObjectValue;

    /**
     * The Object version of the element;
     */
    @JsonbProperty("element")
    @JsonbNillable
    private transient Object m_oElement;
    }
