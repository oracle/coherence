/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.jcache.passthroughcache.processors;

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

/**
 * An {@link com.tangosol.util.InvocableMap.EntryProcessor} to
 * replace a specific existing value with another value.
 *
 * @author bo  2013.10.31
 * @since Coherence 12.1.3
 */
public class ReplaceWithProcessor<K, V>
        extends AbstractProcessor
        implements ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link ReplaceWithProcessor}.
     */
    public ReplaceWithProcessor()
        {
        // required for serialization
        }

    /**
     * Constructs a {@link ReplaceWithProcessor}.
     *
     * @param expectedValue
     * @param newValue
     */
    public ReplaceWithProcessor(V expectedValue, V newValue)
        {
        if (expectedValue == null)
            {
            throw new NullPointerException("expectedValue can't be null");
            }

        if (newValue == null)
            {
            throw new NullPointerException("newValue can't be null)");
            }

        m_expectedValue = expectedValue;
        m_newValue      = newValue;
        }

    // ----- EntryProcessor interface ---------------------------------------

    @Override
    public Object process(InvocableMap.Entry entry)
        {
        if (entry.isPresent() && m_expectedValue.equals(entry.getValue()))
            {
            entry.setValue(m_newValue);

            return true;
            }
        else
            {
            return false;
            }
        }

    // ----- ExternalizableLite interface -----------------------------------

    @Override
    public void readExternal(DataInput in)
            throws IOException
        {
        m_expectedValue = (V) ExternalizableHelper.readObject(in);
        m_newValue      = (V) ExternalizableHelper.readObject(in);
        }

    @Override
    public void writeExternal(DataOutput out)
            throws IOException
        {
        ExternalizableHelper.writeObject(out, m_expectedValue);
        ExternalizableHelper.writeObject(out, m_newValue);
        }

    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader reader)
            throws IOException
        {
        m_expectedValue = (V) reader.readObject(0);
        m_newValue      = (V) reader.readObject(1);
        }

    @Override
    public void writeExternal(PofWriter writer)
            throws IOException
        {
        writer.writeObject(0, m_expectedValue);
        writer.writeObject(1, m_newValue);
        }

    // ------ data members --------------------------------------------------

    /**
     * The expected existing value.
     */
    private V m_expectedValue;

    /**
     * The new value.
     */
    private V m_newValue;
    }
