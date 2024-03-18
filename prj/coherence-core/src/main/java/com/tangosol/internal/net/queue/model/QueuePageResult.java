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
import com.tangosol.io.pof.PofContext;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.util.Binary;
import com.tangosol.util.Converter;
import com.tangosol.util.ConverterCollections;
import com.tangosol.util.ExternalizableHelper;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbTransient;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QueuePageResult<E>
        extends AbstractEvolvable
        implements ExternalizableLite, EvolvablePortableObject
    {
    public QueuePageResult()
        {
        m_listBinary = Collections.emptyList();
        m_list       = Collections.emptyList();
        }

    public QueuePageResult(long nKey, List<Binary> list, Converter<Binary, E> converterFromBinary,
                           Converter<E, Binary> converterToBinary)
        {
        m_nKey                = nKey;
        m_fBinary             = true;
        m_converterFromBinary = converterFromBinary;
        m_converterToBinary   = converterToBinary;
        m_listBinary          = list;
        m_list                = ConverterCollections.getList(list, m_converterFromBinary, m_converterToBinary);
        }

    public long getKey()
        {
        return m_nKey;
        }

    public List<E> getList()
        {
        return m_list;
        }

    public List<Binary> getBinaryList()
        {
        return m_listBinary;
        }

    @Override
    public int getImplVersion()
        {
        return IMPL_VERSION;
        }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void readExternal(PofReader in) throws IOException
        {
        m_fPOF    = true;
        m_nKey    = in.readLong(0);
        m_fBinary = in.readBoolean(1);
        ArrayList  list       = in.readCollection(2, new ArrayList<>());
        PofContext pofContext = in.getPofContext();

        m_converterFromBinary = value -> ExternalizableHelper.fromBinary(value, pofContext);
        m_converterToBinary   = value -> ExternalizableHelper.toBinary(value, pofContext);
        if (m_fBinary)
            {
            m_list       = ConverterCollections.getList((List<Binary>) list, m_converterFromBinary, m_converterToBinary);
            m_listBinary = list;
            }
        else
            {
            m_listBinary = ConverterCollections.getList(list, m_converterToBinary, m_converterFromBinary);
            m_list       = list;
            }
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeLong(0, m_nKey);
        if (m_fPOF)
            {
            out.writeBoolean(1, true);
            out.writeCollection(2, m_listBinary, Binary.class);
            }
        else
            {
            out.writeBoolean(1, false);
            out.writeCollection(2, m_list);
            }
        }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void readExternal(DataInput in) throws IOException
        {
        m_fPOF    = false;
        m_nKey    = in.readLong();
        m_fBinary = in.readBoolean();
        List list = new ArrayList<>();
        ExternalizableHelper.readCollection(in, list, null);
        if (m_fBinary)
            {
            m_converterFromBinary = ExternalizableHelper::fromBinary;
            m_converterToBinary   = ExternalizableHelper::toBinary;
            m_list                = ConverterCollections.getList((List<Binary>) list, m_converterFromBinary, m_converterToBinary);
            m_listBinary          = list;
            }
        else
            {
            m_converterFromBinary = ExternalizableHelper::fromBinary;
            m_converterToBinary   = ExternalizableHelper::toBinary;
            m_listBinary          = ConverterCollections.getList(list, m_converterToBinary, m_converterFromBinary);;
            m_list = list;
            }
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        out.writeLong(m_nKey);
        if (!m_fPOF)
            {
            out.writeBoolean(true);
            ExternalizableHelper.writeCollection(out, m_listBinary);
            }
        else
            {
            out.writeBoolean(false);
            ExternalizableHelper.writeCollection(out, m_list);
            }
        }

    // ----- constants ---------------------------------------------------000

    /**
     * The {@link EvolvablePortableObject} implementation version.
     */
    public static final int IMPL_VERSION = 1;

    // ----- data members ---------------------------------------------------

    @JsonbProperty("key")
    private long m_nKey;

    @JsonbProperty("isBinary")
    private boolean m_fBinary;

    private transient boolean m_fPOF;

    @JsonbProperty("list")
    private List<E> m_list;

    @JsonbTransient
    private List<Binary> m_listBinary;

    @JsonbTransient
    private transient Converter<Binary, E> m_converterFromBinary;

    @JsonbTransient
    private transient Converter<E, Binary> m_converterToBinary;
    }
