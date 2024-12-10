/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * General purpose container that can be used as an accumulator for any
 * reference type.
 *
 * @param <V>  the type of contained value
 */
public class SimpleHolder<V>
        extends com.oracle.coherence.common.base.SimpleHolder<V>
        implements com.tangosol.io.ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Deserialization constructor.
     */
    public SimpleHolder()
        {
        }

    /**
     * Construct SimpleHolder instance.
     *
     * @param value  the contained value
     */
    public SimpleHolder(V value)
        {
        super(value);
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Return true if the contained value is present, false otherwise.
     *
     * @return  true if the contained value is present, false otherwise
     */
    public boolean isPresent()
        {
        return get() != null;
        }

    // ----- ExternalizableLite interface -----------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        set((V) ExternalizableHelper.readObject(in));
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeObject(out, get());
        }

    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        set(in.readObject(0));
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeObject(0, get());
        }

    // ----- Object methods -------------------------------------------------

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

        SimpleHolder that = (SimpleHolder) o;

        return m_value == null
                 ? that.m_value == null
                 : m_value.equals(that.m_value);
        }

    public int hashCode()
        {
        return m_value != null ? m_value.hashCode() : 0;
        }

    public String toString()
        {
        return "SimpleHolder{" +
               "value=" + m_value +
               '}';
        }
    }
