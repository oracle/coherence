/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.ai.search;

import com.oracle.coherence.ai.QueryResult;
import com.tangosol.io.AbstractEvolvable;
import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.EvolvablePortableObject;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.util.ExternalizableHelper;
import jakarta.json.bind.annotation.JsonbProperty;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Objects;

/**
 * A base class for {@link QueryResult} implementations.
 *
 * @param <K>    the type of the keys used to identify vectors
 * @param <V>  the type of the vector
 */
public abstract class BaseQueryResult<K, V>
        extends AbstractEvolvable
        implements QueryResult<K, V>, ExternalizableLite, EvolvablePortableObject
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
     * @param result  the query result
     * @param key     the optional key of the vector the result applies to
     * @param value   the optional result value
     */
    protected BaseQueryResult(double result, K key, V value)
        {
        m_key      = key;
        m_distance = result;
        m_value    = value;
        }

    @Override
    public double getDistance()
        {
        return m_distance;
        }

    @Override
    public K getKey()
        {
        return m_key;
        }

    @Override
    public V getValue()
        {
        return m_value;
        }

    @Override
    public boolean equals(Object o)
        {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass())
            {
            return false;
            }
        BaseQueryResult<?, ?> that = (BaseQueryResult<?, ?>) o;
        return Double.compare(m_distance, that.m_distance) == 0 && Objects.equals(m_key, that.m_key);
        }

    @Override
    public int hashCode()
        {
        return Objects.hash(m_key, m_distance);
        }

    @Override
    public String toString()
        {
        return "BaseQueryResult{" +
                " result=" + m_distance +
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
        m_distance = in.readDouble(1);
        m_value    = in.readObject(2);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeObject(0, m_key);
        out.writeDouble(1, m_distance);
        out.writeObject(2, m_value);
        }

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_key      = ExternalizableHelper.readObject(in);
        m_distance = in.readDouble();
        m_value    = ExternalizableHelper.readObject(in);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeObject(out, m_key);
        out.writeDouble(m_distance);
        ExternalizableHelper.writeObject(out, m_value);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The result of the query.
     */
    @JsonbProperty("distance")
    protected double m_distance;

    /**
     * The optional key of the vector the result applies to.
     */
    @JsonbProperty("key")
    protected K m_key;

    /**
     * The optional vector the result applies to.
     */
    @JsonbProperty("value")
    protected V m_value;
    }
