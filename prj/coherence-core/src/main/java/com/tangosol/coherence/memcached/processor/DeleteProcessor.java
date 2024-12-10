/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.coherence.memcached.processor;

import com.tangosol.coherence.memcached.Response.ResponseCode;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.InvocableMap.Entry;

import com.tangosol.util.processor.AbstractProcessor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * DeleteProcessor deletes the binary entry.
 *
 * @author bb 2013.05.01
 *
 * @since Coherence 12.1.3
 */
public class DeleteProcessor
        extends AbstractProcessor
        implements ExternalizableLite, PortableObject
    {

    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor (necessary for the ExternalizableLite interface).
     */
    public DeleteProcessor()
        {
        }

    // ----- EntryProcessor methods -----------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Object process(Entry entry)
        {
        if (entry.isPresent())
            {
            entry.remove(/*fSynthetic*/ false);
            return ResponseCode.OK;
            }
        return ResponseCode.KEYNF;
        }

    // ----- ExternalizableLite methods -------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(DataInput in)
            throws IOException
        {
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(DataOutput out)
            throws IOException
        {
        }

    // ----- PortableObject methods -----------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void readExternal(PofReader in)
            throws IOException
        {
        }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeExternal(PofWriter out)
            throws IOException
        {
        }
    }