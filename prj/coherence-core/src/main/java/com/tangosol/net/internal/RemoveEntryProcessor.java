/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.net.internal;

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

import javax.json.bind.annotation.JsonbProperty;

/**
 * SyntheticRemoveEntryProcessor is used to remove an entry from cache using synthetic/nonsynthetic manner.
 *
 *
 */
public class RemoveEntryProcessor<K,V>
        extends AbstractProcessor<K, V, Boolean>
        implements ExternalizableLite, PortableObject
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public RemoveEntryProcessor()
        {
        }

    /**
     * Create a SyntheticRemoveEntryProcessor processor
     * @param fSynthetic - Remove the entry as a synthetic operation if set as true
     */
    public RemoveEntryProcessor(boolean fSynthetic)
        {
        this.m_fSynthetic = fSynthetic;
        }

    // ----- EntryProcessor interface ---------------------------------------

    /**
     * Process a Map.Entry object.
     *
     * @param entry  the Entry to process
     *
     * @return whether the remove was synthetic or not
     */
    public Boolean process(InvocableMap.Entry entry)
        {
        entry.remove(m_fSynthetic);

        return m_fSynthetic;
        }

    // ----- ExternalizableLite ---------------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(DataInput in)
            throws IOException
        {
        m_fSynthetic = ExternalizableHelper.readInt(in) == 0
                       ? true
                       : false;
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        ExternalizableHelper.writeInt(out, m_fSynthetic
                                           ? 0
                                           : 1);
        }

    // ----- PortableObject interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        m_fSynthetic = in.readBoolean(0);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeBoolean(0, m_fSynthetic);
        }
    // ----- accessors ------------------------------------------------------

    /**
     * Returns whether the removal must be synthetic or not.
     *
     * @return whether the removal must be synthetic or not
     */
    public boolean isSynthetic()
        {
        return m_fSynthetic;
        }

    // ----- data members ---------------------------------------------------

    /**
     * Specifies whether the removal must be synthetic or not
     */
    @JsonbProperty("synthetic")
    private boolean m_fSynthetic = false;
    }

