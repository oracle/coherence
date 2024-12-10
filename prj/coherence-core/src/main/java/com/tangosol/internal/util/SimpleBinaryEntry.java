/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.Serializer;
import com.tangosol.io.SerializerAware;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.Binary;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.InvocableMapHelper;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.ValueUpdater;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import javax.json.bind.annotation.JsonbProperty;

/**
 * An immutable Map Entry that lazily converts the key and value from their Binary
 * representations.
 *
 * @author as  2014.12.29
 */
public class SimpleBinaryEntry<K, V>
        implements InvocableMap.Entry<K, V>, SerializerAware, ExternalizableLite, PortableObject
    {
    // ---- constructors ----------------------------------------------------

    public SimpleBinaryEntry()
        {
        }

    public SimpleBinaryEntry(BinaryEntry<K, V> entry)
        {
        this(entry.getBinaryKey(), entry.getBinaryValue());
        }

    public SimpleBinaryEntry(Binary binKey, Binary binValue)
        {
        m_binKey   = binKey;
        m_binValue = binValue;
        }

    // ---- Map.Entry implementation ----------------------------------------

    public K getKey()
        {
        K key = m_key;
        if (key == null)
            {
            key = m_key = (K) ExternalizableHelper.fromBinary(m_binKey, getContextSerializer());
            }
        return key;
        }

    public V getValue()
        {
        V value = m_value;
        if (value == null)
            {
            value = m_value = (V) ExternalizableHelper.fromBinary(m_binValue, getContextSerializer());
            }
        return value;
        }

    public V setValue(V value)
        {
        throw new UnsupportedOperationException();
        }

    // ---- InvocableMap.Entry implementation -------------------------------

    public void setValue(V value, boolean fSynthetic)
        {
        setValue(value);
        }

    public <T> void update(ValueUpdater<V, T> updater, T value)
        {
        InvocableMapHelper.updateEntry(updater, this, value);
        }

    public boolean isPresent()
        {
        return true;
        }

    public boolean isSynthetic()
        {
        return false;
        }

    public void remove(boolean fSynthetic)
        {
        throw new UnsupportedOperationException();
        }

    public <T, E> E extract(ValueExtractor<T, E> extractor)
        {
        return InvocableMapHelper.extractFromEntry(extractor, this);
        }

    // ---- SerializerAware implementation ----------------------------------

    public Serializer getContextSerializer()
        {
        return m_serializer;
        }

    public void setContextSerializer(Serializer serializer)
        {
        m_serializer = serializer;
        }

    // ---- ExternalizableLite implementation -------------------------------

    public void readExternal(DataInput in) throws IOException
        {
        m_binKey   = ExternalizableHelper.readObject(in);
        m_binValue = ExternalizableHelper.readObject(in);
        }

    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeObject(out, m_binKey);
        ExternalizableHelper.writeObject(out, m_binValue);
        }

    // ---- PortableObject implementation -----------------------------------

    public void readExternal(PofReader in) throws IOException
        {
        m_binKey   = in.readBinary(0);
        m_binValue = in.readBinary(1);
        }

    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeBinary(0, m_binKey);
        out.writeBinary(1, m_binValue);
        }


    // ----- Object methods ------------------------------------------------

    public String toString()
        {
        return "SimpleBinaryEntry(key=\"" + getKey() + "\", value=\"" + getValue() + "\")";
        }

    // ---- data members ----------------------------------------------

    @JsonbProperty("binKey")
    protected Binary m_binKey;

    @JsonbProperty("binValue")
    protected Binary m_binValue;

    protected transient Serializer m_serializer;
    protected transient K m_key;
    protected transient V m_value;
    }
