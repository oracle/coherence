/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache.passthroughcache.processors;

import com.tangosol.coherence.jcache.common.CoherenceEntryProcessorResult;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.processor.AbstractProcessor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import javax.cache.processor.EntryProcessor;
import javax.cache.processor.MutableEntry;

/**
 * An {@link com.tangosol.util.InvocableMap.EntryProcessor} to
 * invoke a JCache {@link javax.cache.processor.EntryProcessor} and
 * return a {@link CoherenceEntryProcessorResult}.
 *
 * @param <K> key type
 * @param <V> value type
 * @param <T> invoke return type
 *
 * @author bo  2013.10.31
 * @since Coherence 12.1.3
 */
public class InvokeProcessor<K, V, T>
        extends AbstractProcessor
        implements ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs an {@link InvokeProcessor}.
     */
    public InvokeProcessor()
        {
        // required for serialization
        }

    /**
     * Constructs an {@link InvokeProcessor}.
     *
     * @param processor  the {@link EntryProcessor}
     * @param arguments  optional arguments for the {@link EntryProcessor}
     */
    public InvokeProcessor(EntryProcessor<K, V, T> processor, Object... arguments)
        {
        if (processor == null)
            {
            throw new NullPointerException("processor can't be null");
            }

        m_processor    = processor;
        m_arrArguments = new Object[arguments.length];

        System.arraycopy(arguments, 0, m_arrArguments, 0, arguments.length);
        }

    // ----- EntryProcessor interface ---------------------------------------

    @Override
    public Object process(InvocableMap.Entry entry)
        {
        InternalEntry<K, V>              internalEntry = new InternalEntry<K, V>(entry);
        CoherenceEntryProcessorResult<T> result;

        try
            {
            T t = m_processor.process(internalEntry, m_arrArguments);

            result = t == null ? null : new CoherenceEntryProcessorResult<T>(t);
            }
        catch (Exception e)
            {
            result = new CoherenceEntryProcessorResult<T>(e);
            }

        return result;
        }

    // ----- ExternalizableLite interface -----------------------------------

    @Override
    public void readExternal(DataInput in)
            throws IOException
        {
        m_processor = (EntryProcessor<K, V, T>) ExternalizableHelper.readObject(in);

        int cArguments = ExternalizableHelper.readInt(in);
        azzert(cArguments < 256, "Unexpected number of arguments.");

        m_arrArguments = new Object[cArguments];

        for (int i = 0; i < cArguments; i++)
            {
            m_arrArguments[i] = ExternalizableHelper.readObject(in);
            }
        }

    @Override
    public void writeExternal(DataOutput out)
            throws IOException
        {
        ExternalizableHelper.writeObject(out, m_processor);
        ExternalizableHelper.writeObject(out, m_arrArguments.length);

        for (Object oArgument : m_arrArguments)
            {
            ExternalizableHelper.writeObject(out, oArgument);
            }
        }

    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader reader)
            throws IOException
        {
        m_processor = (EntryProcessor<K, V, T>) reader.readObject(0);

        int cArguments = reader.readInt(1);

        m_arrArguments = new Object[cArguments];

        reader.readObjectArray(2, m_arrArguments);
        }

    @Override
    public void writeExternal(PofWriter writer)
            throws IOException
        {
        writer.writeObject(0, m_processor);
        writer.writeInt(1, m_arrArguments.length);
        writer.writeObjectArray(2, m_arrArguments);
        }

    // ------ InternalEntry class -------------------------------------------

    /**
     * An internal implementation of a {@link MutableEntry}.
     *
     * @param <K>  the type of the entry key
     * @param <V>  the type of the entry value
     */
    static class InternalEntry<K, V>
            implements javax.cache.processor.MutableEntry<K, V>
        {
        /**
         * Constructs an {@link InternalEntry}.
         *
         * @param entry  the {@link InvocableMap} entry
         */
        public InternalEntry(InvocableMap.Entry entry)
            {
            m_entry = entry;
            }

        @Override
        public boolean exists()
            {
            return m_entry.isPresent();
            }

        @Override
        public void remove()
            {
            m_entry.remove(false);
            }

        @Override
        public void setValue(V value)
            {
            m_entry.setValue(value);
            }

        @Override
        public K getKey()
            {
            return (K) m_entry.getKey();
            }

        @Override
        public V getValue()
            {
            return (V) m_entry.getValue();
            }

        @Override
        public <T> T unwrap(Class<T> clz)
            {
            if (clz != null && clz.isInstance(m_entry))
                {
                return (T) m_entry;
                }
            else
                {
                throw new IllegalArgumentException("Unsupported unwrap(" + clz + ")");
                }
            }

        // ------ data members ----------------------------------------------

        /**
         * The underlying {@link InvocableMap} entry.
         */
        private InvocableMap.Entry m_entry;
        }

    // ------ data members --------------------------------------------------

    /**
     * The {@link EntryProcessor}.
     */
    private EntryProcessor<K, V, T> m_processor;

    /**
     * The optional arguments.
     */
    private Object[] m_arrArguments;
    }
