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
import jakarta.json.bind.annotation.JsonbProperty;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class QueuePollResult
        extends AbstractEvolvable
        implements ExternalizableLite, EvolvablePortableObject
    {
    public QueuePollResult()
        {
        }

    public QueuePollResult(long id, Binary binElement)
        {
        m_id         = id;
        m_binElement = binElement;
        }

    public long getId()
        {
        return m_id;
        }

    public Binary getElement()
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
        m_id = in.readLong();
        m_binElement = ExternalizableHelper.readObject(in);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        out.writeLong(m_id);
        ExternalizableHelper.writeObject(out, m_binElement);
        }

    // ----- data members ---------------------------------------------------

    public static final int IMPL_VERSION = 1;

    @JsonbProperty("element")
    private Binary m_binElement;

    @JsonbProperty("id")
    private long m_id;
    }
