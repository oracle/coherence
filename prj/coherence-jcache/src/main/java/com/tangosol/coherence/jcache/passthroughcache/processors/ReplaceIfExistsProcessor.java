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
 * replace an existing value with another value.
 *
 * @author bo  2013.10.31
 * @since Coherence 12.1.3
 */
public class ReplaceIfExistsProcessor<K, V>
        extends AbstractProcessor
        implements ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a {@link ReplaceIfExistsProcessor}.
     */
    public ReplaceIfExistsProcessor()
        {
        // required for serialization
        }

    /**
     * Constructs a {@link ReplaceIfExistsProcessor}.
     *
     * @param newValue
     */
    public ReplaceIfExistsProcessor(V newValue)
        {
        if (newValue == null)
            {
            throw new NullPointerException("newValue can't be null)");
            }

        m_newValue = newValue;
        }

    // ----- EntryProcessor interface ---------------------------------------

    @Override
    public Object process(InvocableMap.Entry entry)
        {
        if (entry.isPresent())
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
        m_newValue = (V) ExternalizableHelper.readObject(in);
        }

    @Override
    public void writeExternal(DataOutput out)
            throws IOException
        {
        ExternalizableHelper.writeObject(out, m_newValue);
        }

    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader reader)
            throws IOException
        {
        m_newValue = (V) reader.readObject(0);
        }

    @Override
    public void writeExternal(PofWriter writer)
            throws IOException
        {
        writer.writeObject(0, m_newValue);
        }

    // ------ data members --------------------------------------------------

    /**
     * The new value.
     */
    private V m_newValue;
    }
