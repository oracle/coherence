/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.ai.results;

import com.oracle.coherence.ai.QueryResult;

import com.tangosol.io.AbstractEvolvable;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.ReadBuffer;

import com.tangosol.io.pof.EvolvablePortableObject;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.Objects;
import java.util.Optional;

/**
 * A base class for {@link QueryResult} implementations.
 *
 * @param <K>  the type of the keys used to identify vectors
 * @param <M>  the type of the vector's metadata
 * @param <R>  the type of the vector
 */
public abstract class BaseQueryResult<K, M, R>
        extends AbstractEvolvable
        implements QueryResult<R, K, M>, ExternalizableLite, EvolvablePortableObject
    {
    /**
     * Default constructor for serialization.
     */
    protected BaseQueryResult()
        {
        }

    /**
     * Create a {@link BaseQueryResult}.
     *
     * @param result   the query result
     * @param key      the key of the vector the result applies to
     * @param vector   the optional result vector data
     * @param metadata the optional result metadata
     */
    protected BaseQueryResult(float result, K key, ReadBuffer vector, M metadata)
        {
        m_key      = key;
        m_result   = result;
        m_vector   = vector;
        m_metadata = metadata;
        }

    @Override
    public float getResult()
        {
        return m_result;
        }

    @Override
    public Optional<K> getKey()
        {
        return Optional.ofNullable(m_key);
        }

    @Override
    public Optional<ReadBuffer> getBinaryVector()
        {
        if (m_vector == null || m_vector.length() == 0)
            {
            return Optional.empty();
            }
        return Optional.of(m_vector);
        }

    @Override
    public Optional<M> getMetadata()
        {
        return Optional.ofNullable(m_metadata);
        }

    @Override
    public boolean equals(Object o)
        {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass())
            {
            return false;
            }
        BaseQueryResult<?, ?, ?> that = (BaseQueryResult<?, ?, ?>) o;
        return Float.compare(m_result, that.m_result) == 0 && Objects.equals(m_key, that.m_key);
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(m_key, m_result);
        }

    @Override
    public String toString()
        {
        return "BaseQueryResult{" +
                " result=" + m_result +
                ", key=" + m_key +
                '}';
        }

    @Override
    public int getImplVersion()
        {
        return 0;
        }

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_key      = in.readObject(0);
        m_result   = in.readFloat(1);
        m_vector   = in.readBinary(2);
        m_metadata = in.readObject(3);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeObject(0, m_key);
        out.writeFloat(1, m_result);
        out.writeBinary(2, m_vector.toBinary());
        out.writeObject(3, m_metadata);
        }

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_key      = ExternalizableHelper.readObject(in);
        m_result   = in.readFloat();
        m_vector   = ExternalizableHelper.readObject(in);
        m_metadata = ExternalizableHelper.readObject(in);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeObject(out, m_key);
        out.writeFloat(m_result);
        ExternalizableHelper.writeObject(out, m_vector);
        ExternalizableHelper.writeObject(out, m_metadata);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The result of the query.
     */
    protected float m_result;

    /**
     * The optional key of the vector the result applies to.
     */
    protected K m_key;

    /**
     * The optional vector the result applies to.
     */
    protected ReadBuffer m_vector;

    /**
     * The optional metadata the result applies to.
     */
    protected M m_metadata;
    }
