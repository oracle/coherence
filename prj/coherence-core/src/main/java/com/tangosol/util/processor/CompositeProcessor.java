/*
 * Copyright (c) 2000, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.processor;


import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import javax.json.bind.annotation.JsonbProperty;


/**
* The CompositeProcessor represents a collection of entry processors that are
* invoked sequentially against the same Entry.
*
* @author gg/jh 2005.10.31
*/
public class CompositeProcessor<K, V>
        extends    AbstractProcessor<K, V, Object>
        implements ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public CompositeProcessor()
        {
        }

    /**
    * Construct a CompositeProcessor for the specified array of individual
    * entry processors.
    * <p>
    * The result of the CompositeProcessor execution is an array of results
    * returned by the individual EntryProcessor invocations.
    *
    * @param aProcessor  the entry processor array
    */
    public CompositeProcessor(InvocableMap.EntryProcessor<K, V, ?>[] aProcessor)
        {
        azzert(aProcessor != null, "Processor array is null");
        m_aProcessor = aProcessor;
        }


    // ----- EntryProcessor interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public Object process(InvocableMap.Entry<K, V> entry)
        {
        InvocableMap.EntryProcessor[] aProcessor  = m_aProcessor;
        int                           cProcessors = aProcessor.length;
        Object[]                      aoResult    = new Object[cProcessors];
        for (int i = 0; i < cProcessors; i++)
            {
            aoResult[i] = aProcessor[i].process(entry);
            }

        return aoResult;
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Compare the ConditionalProcessor with another object to determine
    * equality.
    *
    * @return true iff this ConditionalProcessor and the passed object are
    *         equivalent ConditionalProcessors
    */
    public boolean equals(Object o)
        {
        if (o instanceof CompositeProcessor)
            {
            CompositeProcessor that = (CompositeProcessor) o;
            return equalsDeep(this.m_aProcessor, that.m_aProcessor);
            }

        return false;
        }

    /**
    * Determine a hash value for the ConditionalProcessor object according to
    * the general {@link Object#hashCode()} contract.
    *
    * @return an integer hash value for this ConditionalProcessor object
    */
    public int hashCode()
        {
        InvocableMap.EntryProcessor[] aProcessor  = m_aProcessor;
        int                           cProcessors = aProcessor.length;
        int                           iHash       = 0;

        for (int i = 0; i < cProcessors; i++)
            {
            iHash += aProcessor[i].hashCode();
            }
        return iHash;
        }

    /**
    * Return a human-readable description for this ConditionalProcessor.
    *
    * @return a String description of the ConditionalProcessor
    */
    public String toString()
        {
        return "CompositeProcessor(" +
            toDelimitedString(m_aProcessor, ", ") + ')';
        }


    // ----- ExternalizableLite interface -----------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(DataInput in)
            throws IOException
        {
        int cProcessors = ExternalizableHelper.readInt(in);

        azzert(cProcessors < 16384, "Unexpected number of composite processors.");

        InvocableMap.EntryProcessor[] aProcessor =
            new InvocableMap.EntryProcessor[cProcessors];

        for (int i = 0; i < cProcessors; i++)
            {
            aProcessor[i] = (InvocableMap.EntryProcessor)
                ExternalizableHelper.readObject(in);
            }
        m_aProcessor = aProcessor;
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        InvocableMap.EntryProcessor[] aProcessor  = m_aProcessor;
        int                           cProcessors = aProcessor.length;

        ExternalizableHelper.writeInt(out, cProcessors);
        for (int i = 0; i < cProcessors; i++)
            {
            ExternalizableHelper.writeObject(out, aProcessor[i]);
            }
        }


    // ----- PortableObject interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        m_aProcessor = (InvocableMap.EntryProcessor[])
                in.readObjectArray(0, EMPTY_PROCESSOR_ARRAY);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeObjectArray(0, m_aProcessor);
        }


    // ----- constants ------------------------------------------------------

    /**
    * An empty array of EntryProcessor objects.
    */
    private static final InvocableMap.EntryProcessor[]
            EMPTY_PROCESSOR_ARRAY = new InvocableMap.EntryProcessor[0];


    // ----- data members ---------------------------------------------------

    /**
    * The underlying entry processor array.
    */
    @JsonbProperty("processors")
    protected InvocableMap.EntryProcessor<K, V, ?>[] m_aProcessor;
    }