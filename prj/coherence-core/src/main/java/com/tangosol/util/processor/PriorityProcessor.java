/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.processor;


import com.tangosol.net.AbstractPriorityTask;

import com.tangosol.util.ClassHelper;
import com.tangosol.util.InvocableMap;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PortableObject;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import java.util.Map;
import java.util.Set;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import javax.json.bind.annotation.JsonbProperty;


/**
* PriorityProcessor is used to explicitly control the scheduling priority and
* timeouts for execution of EntryProcessor-based methods.
* <p>
* For example, let's assume that there is a cache that belongs to a partitioned
* cache service configured with a <i>task-timeout</i> of 5 seconds.
* Also assume that there is a particular PreloadRequest processor that could
* take much longer to complete due to a large amount of database related
* processing. Then we could override the default task timeout value by using the
* PriorityProcessor as follows:
* <pre>
*   PreloadRequest     procStandard = PreloadRequest.INSTANCE;
*   PriorityProcessor  procPriority = new PriorityProcessor(procStandard);
*   procPriority.setExecutionTimeoutMillis(PriorityTask.TIMEOUT_NONE);
*   cache.processAll(setKeys, procPriority);
* </pre>
* <p>
* This is an advanced feature which should be used judiciously.
*
* @author gg 2007.03.20
* @since Coherence 3.3
*/
public class PriorityProcessor<K, V, T>
        extends    AbstractPriorityTask
        implements InvocableMap.EntryProcessor<K, V, T>, ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public PriorityProcessor()
        {
        }

    /**
    * Construct a PriorityProcessor.
    *
    * @param processor  the processor wrapped by this PriorityProcessor
    */
    public PriorityProcessor(InvocableMap.EntryProcessor<K, V, T> processor)
        {
        m_processor = processor;
        }


    // ----- InvocableMap.EntryProcessor interface --------------------------

    /**
    * {@inheritDoc}
    */
    public T process(InvocableMap.Entry<K, V> entry)
        {
        return m_processor.process(entry);
        }

    /**
    * {@inheritDoc}
    */
    public Map<K, T> processAll(Set<? extends InvocableMap.Entry<K, V>> setEntries)
        {
        return m_processor.processAll(setEntries);
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Obtain the underlying processor.
    *
    * @return the processor wrapped by this PriorityProcessor
    */
    public InvocableMap.EntryProcessor<K, V, T> getProcessor()
        {
        return m_processor;
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Return a human-readable description for this PriorityProcessor.
    *
    * @return a String description of the PriorityProcessor
    */
    public String toString()
        {
        return ClassHelper.getSimpleName(getClass()) + '(' + m_processor + ')';
        }


    // ----- ExternalizableLite interface -----------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(DataInput in)
            throws IOException
        {
        super.readExternal(in);

        m_processor = (InvocableMap.EntryProcessor) readObject(in);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        super.writeExternal(out);

        writeObject(out, m_processor);
        }


    // ----- PortableObject interface ---------------------------------------

    /**
    * {@inheritDoc}
    * <p>
    * The PriorityProcessor implementation reserves property index 10.
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        super.readExternal(in);

        m_processor = (InvocableMap.EntryProcessor) in.readObject(10);
        }

    /**
    * {@inheritDoc}
    * <p>
    * The PriorityProcessor implementation reserves property index 10.
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        super.writeExternal(out);

        out.writeObject(10, m_processor);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The wrapped processor.
    */
    @JsonbProperty("processor")
    private InvocableMap.EntryProcessor<K, V, T> m_processor;
    }