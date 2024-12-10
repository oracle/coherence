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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The result holding a page of a queue.
 */
public class QueuePageResult
        extends AbstractEvolvable
        implements ExternalizableLite, EvolvablePortableObject
    {
    /**
     * Create an empty page.
     */
    public QueuePageResult()
        {
        m_listBinary = Collections.emptyList();
        }

    /**
     * Create a page of results.
     *
     * @param nKey  the key of the page
     * @param list  the results
     */
    public QueuePageResult(long nKey, List<Binary> list)
        {
        m_nKey                = nKey;
        m_listBinary          = list;
        }

    /**
     * Obtain the page key.
     *
     * @return  the page key
     */
    public long getKey()
        {
        return m_nKey;
        }

    /**
     * Obtain the queue content from the page.
     *
     * @return the queue content from the page
     */
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
    public void readExternal(PofReader in) throws IOException
        {
        m_nKey    = in.readLong(0);
        m_listBinary = in.readCollection(1, new ArrayList<>());
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeLong(0, m_nKey);
        out.writeCollection(1, m_listBinary);
        }

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_nKey    = in.readLong();
        m_listBinary = new ArrayList<>();
        ExternalizableHelper.readCollection(in, m_listBinary, null);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        out.writeLong(m_nKey);
        ExternalizableHelper.writeCollection(out, m_listBinary);
        }

    // ----- constants ---------------------------------------------------000

    /**
     * The {@link EvolvablePortableObject} implementation version.
     */
    public static final int IMPL_VERSION = 1;

    // ----- data members ---------------------------------------------------

    private long m_nKey;

    private List<Binary> m_listBinary;
    }
