/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.aggregator;


import com.tangosol.net.AbstractPriorityTask;

import com.tangosol.util.ClassHelper;
import com.tangosol.util.InvocableMap;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import java.util.Set;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import javax.json.bind.annotation.JsonbProperty;


/**
* PriorityAggregator is used to explicitly control the scheduling priority and
* timeouts for execution of EntryAggregator-based methods.
* <p>
* For example, let's assume that there is an <i>Orders</i> cache that belongs to
* a partitioned cache service configured with a <i>request-timeout</i> and
* <i>task-timeout</i> of 5 seconds.
* Also assume that we are willing to wait longer for a particular
* aggregation request that scans the entire cache. Then we could override the
* default timeout values by using the PriorityAggregator as follows:
* <pre>
*   DoubleAverage      aggrStandard = new DoubleAverage("getPrice");
*   PriorityAggregator aggrPriority = new PriorityAggregator(aggrStandard);
*   aggrPriority.setExecutionTimeoutMillis(PriorityTask.TIMEOUT_NONE);
*   aggrPriority.setRequestTimeoutMillis(PriorityTask.TIMEOUT_NONE);
*   cacheOrders.aggregate((Filter) null, aggrPriority);
* </pre>
* <p>
* This is an advanced feature which should be used judiciously.
*
 * @param <K>  the type of the Map entry keys
 * @param <V>  the type of the Map entry values
 * @param <P>  the type of the partial result
 * @param <R>  the type of the final result
 *
* @author gg 2007.03.20
* @since Coherence 3.3
*/
public class PriorityAggregator<K, V, P, R>
        extends    AbstractPriorityTask
        implements InvocableMap.StreamingAggregator<K, V, P, R>,
                   ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public PriorityAggregator()
        {
        }

    /**
    * Construct a PriorityAggregator.
    *
    * @param aggregator  the aggregator wrapped by this PriorityAggregator
    */
    public PriorityAggregator(InvocableMap.StreamingAggregator<K, V, P, R> aggregator)
        {
        m_aggregator = aggregator;
        }

    // ----- InvocableMap.StreamingAggregator interface ---------------------


    public InvocableMap.StreamingAggregator<K, V, P, R> supply()
        {
        PriorityAggregator<K, V, P, R> aggregator = new PriorityAggregator<>(getAggregator().supply());

        aggregator.setSchedulingPriority(getSchedulingPriority());
        aggregator.setExecutionTimeoutMillis(getExecutionTimeoutMillis());
        aggregator.setRequestTimeoutMillis(getRequestTimeoutMillis());

        return aggregator;
        }

    @Override
    public boolean accumulate(InvocableMap.Entry<? extends K, ? extends V> entry)
        {
        return m_aggregator.accumulate(entry);
        }

    @Override
    public boolean combine(P partialResult)
        {
        return m_aggregator.combine(partialResult);
        }

    @Override
    public P getPartialResult()
        {
        return m_aggregator.getPartialResult();
        }

    @Override
    public R finalizeResult()
        {
        return m_aggregator.finalizeResult();
        }

    @Override
    public int characteristics()
        {
        return m_aggregator.characteristics();
        }


    // ----- InvocableMap.EntryAggregator interface --------------------------

    /**
    * {@inheritDoc}
    */
    public R aggregate(Set<? extends InvocableMap.Entry<? extends K, ? extends V>> setEntries)
        {
        return m_aggregator.aggregate(setEntries);
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Obtain the underlying aggregator.
    *
    * @return the aggregator wrapped by this PriorityAggregator
    */
    public InvocableMap.StreamingAggregator<K, V, P, R> getAggregator()
        {
        return m_aggregator;
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Return a human-readable description for this PriorityAggregator.
    *
    * @return a String description of the PriorityAggregator
    */
    public String toString()
        {
        return ClassHelper.getSimpleName(getClass()) + '(' + m_aggregator + ')';
        }


    // ----- ExternalizableLite interface -----------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(DataInput in)
            throws IOException
        {
        super.readExternal(in);

        m_aggregator = readObject(in);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        super.writeExternal(out);

        writeObject(out, m_aggregator);
        }


    // ----- PortableObject interface ---------------------------------------

    /**
    * {@inheritDoc}
    * <p>
    * The PriorityAggregator implementation reserves property index 10.
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        super.readExternal(in);

        m_aggregator = in.readObject(10);
        }

    /**
    * {@inheritDoc}
    * <p>
    * The PriorityAggregator implementation reserves property index 10.
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        super.writeExternal(out);

        out.writeObject(10, m_aggregator);
        }


    // ----- data members ---------------------------------------------------

    /**
    * The wrapped aggregator.
    */
    @JsonbProperty("aggregator")
    private InvocableMap.StreamingAggregator<K, V, P, R> m_aggregator;
    }
