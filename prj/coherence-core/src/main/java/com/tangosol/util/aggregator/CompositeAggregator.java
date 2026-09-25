/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.tangosol.util.aggregator;


import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.ClassHelper;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.ImmutableArrayList;
import com.tangosol.util.InvocableMap;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import jakarta.json.bind.annotation.JsonbProperty;


/**
* CompositeAggregator provides an ability to execute a collection of
* aggregators against the same subset of the entries in an InvocableMap,
* resulting in a list of corresponding aggregation results. The size of the
* returned list will always be equal to the length of the aggregators' array.
*
* @author gg 2006.02.08
* @since Coherence 3.2
*/
public class CompositeAggregator<K, V>
        extends    ExternalizableHelper
        implements InvocableMap.StreamingAggregator<K, V, Object, List>,
                   ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public CompositeAggregator()
        {
        }

    /**
    * Construct a CompositeAggregator based on a specified EntryAggregator
    * array.
    *
    * @param aAggregator  an array of EntryAggregator objects; may not be
    *                     null
    */
    public CompositeAggregator(InvocableMap.EntryAggregator[] aAggregator)
        {
        Objects.requireNonNull(aAggregator);
        m_aAggregator = aAggregator;
        }

    // ----- EntryAggregator interface --------------------------------------

    /**
    * Process a set of InvocableMap Entry objects using each of the
    * underlying agregators in order to produce an array of aggregated
    * results.
    *
    * @param setEntries  a Set of read-only InvocableMap Entry objects to
    *                    aggregate
    *
    * @return a List of aggregated results from processing the entries by the
    *         corresponding underlying aggregators
    */
    public List aggregate(Set setEntries)
        {
        InvocableMap.EntryAggregator[] aAggregator = m_aAggregator;

        int      cAggregators = aAggregator.length;
        Object[] aoResult     = new Object[cAggregators];
        for (int i = 0; i < cAggregators; i++)
            {
            aoResult[i] = aAggregator[i].aggregate(setEntries);
            }
        return new ImmutableArrayList(aoResult);
        }


    // ----- StreamingAggregator interface ----------------------------------

    @Override
    public InvocableMap.StreamingAggregator<K, V, Object, List> supply()
        {
        ensureInitialized();

        if (m_fStreaming)
            {
            InvocableMap.StreamingAggregator[] aCopy =
                    Arrays.stream(m_aAggregator)
                            .map(aggr -> ((InvocableMap.StreamingAggregator) aggr).supply())
                            .toArray(InvocableMap.StreamingAggregator[]::new);

            return createInstance(aCopy);
            }
        else
            {
            return createInstance(m_aAggregator);
            }
        }

    @Override
    public boolean accumulate(InvocableMap.Entry entry)
        {
        ensureInitialized();

        if (m_fStreaming)
            {
            InvocableMap.EntryAggregator[] aAggregator = m_aAggregator;

            for (int i = 0; i < aAggregator.length; i++)
                {
                ((InvocableMap.StreamingAggregator) aAggregator[i]).accumulate(entry);
                }
            }
        else
            {
            m_setEntries.add(entry);
            }

        return true;
        }

    @Override
    public RetractionResult retract(InvocableMap.Entry entry)
        {
        ensureInitialized();
        if (!m_fContinuous)
            {
            return RetractionResult.REBUILD_REQUIRED;
            }

        RetractionResult result = RetractionResult.UPDATED;
        for (InvocableMap.EntryAggregator aggregator : m_aAggregator)
            {
            RetractionResult child =
                    ((InvocableMap.StreamingAggregator) aggregator).retract(entry);
            if (child == RetractionResult.REBUILD_REQUIRED)
                {
                return child;
                }
            if (child == RetractionResult.STALE_BOUND)
                {
                result = child;
                }
            }
        return result;
        }

    @Override
    public RetractionResult update(InvocableMap.Entry entry)
        {
        ensureInitialized();
        if (!m_fContinuous)
            {
            return RetractionResult.REBUILD_REQUIRED;
            }

        for (InvocableMap.EntryAggregator aggregator : m_aAggregator)
            {
            RetractionResult result =
                    ((InvocableMap.StreamingAggregator) aggregator).update(entry);
            if (result == RetractionResult.REBUILD_REQUIRED)
                {
                return result;
                }
            }
        return getMaintenanceStatus();
        }

    @Override
    public RetractionResult getMaintenanceStatus()
        {
        ensureInitialized();
        if (!m_fContinuous)
            {
            return RetractionResult.REBUILD_REQUIRED;
            }

        for (InvocableMap.EntryAggregator aggregator : m_aAggregator)
            {
            if (((InvocableMap.StreamingAggregator) aggregator).getMaintenanceStatus()
                    != RetractionResult.UPDATED)
                {
                // A composite does not have a single bound that can be used
                // to prune the partition scan.
                return RetractionResult.REBUILD_REQUIRED;
                }
            }
        return RetractionResult.UPDATED;
        }

    @Override
    public boolean combine(Object oResultPart)
        {
        ensureInitialized();

        InvocableMap.EntryAggregator[] aAggregator = m_aAggregator;
        int cAggregators = aAggregator.length;

        if (m_fStreaming)
            {
            List<Object> aPartialResults = (List<Object>) oResultPart;

            for (int i = 0, len = aPartialResults.size(); i < len; i++)
                {
                ((InvocableMap.StreamingAggregator) aAggregator[i]).combine(aPartialResults.get(i));
                }
            }
        else if (m_fParallel)
            {
            if (!(oResultPart instanceof List))  // do we need this?
                {
                throw new IllegalStateException(
                    "Expected result type: java.util.List; actual type: " +
                    oResultPart.getClass().getName());
                }

            List listResultPart = (List) oResultPart;
            if (listResultPart.size() != cAggregators) // do we need this?
                {
                throw new IllegalStateException(
                    "Expected result list size: " + cAggregators +
                    "; actual size: " + listResultPart.size());
                }

            for (int i = 0; i < cAggregators; i++)
                {
                m_aParallelResults[i].add(listResultPart.get(i));
                }
            }
        else
            {
            m_setEntries.addAll((Set) oResultPart);
            }

        return true;
        }

    @Override
    public Object getPartialResult()
        {
        ensureInitialized();

        InvocableMap.EntryAggregator[] aAggregator = m_aAggregator;
        int cAggregators = aAggregator.length;

        if (m_fStreaming)
            {
            List<Object> aPartialResults = new ArrayList<>(cAggregators);
            for (int i = 0; i < cAggregators; i++)
                {
                aPartialResults.add(((InvocableMap.StreamingAggregator) aAggregator[i]).getPartialResult());
                }

            return aPartialResults;
            }
        else if (m_fParallel)
            {
            return aggregate(m_setEntries);
            }
        else
            {
            return m_setEntries;
            }
        }

    @Override
    public List finalizeResult()
        {
        ensureInitialized();

        InvocableMap.EntryAggregator[] aAggregator = m_aAggregator;
        int cAggregators = aAggregator.length;

        if (m_fStreaming)
            {
            Object[] aoResult = new Object[cAggregators];
            for (int i = 0; i < cAggregators; i++)
                {
                aoResult[i] = ((InvocableMap.StreamingAggregator) aAggregator[i]).finalizeResult();
                }

            return new ImmutableArrayList(aoResult);
            }
        else if (m_fParallel)
            {
            Object[] aoResult = new Object[cAggregators];
            for (int i = 0; i < cAggregators; i++)
                {
                aoResult[i] = ((InvocableMap.ParallelAwareAggregator) aAggregator[i]).aggregateResults(m_aParallelResults[i]);
                }

            return new ImmutableArrayList(aoResult);
            }
        else
            {
            return aggregate(m_setEntries);
            }
        }

    @Override
    public int characteristics()
        {
        ensureInitialized();

        return m_fStreaming
               ? PARALLEL
                       | (m_fContinuous ? CONTINUOUS : 0)
                       | (m_fStateCheckpointable ? STATE_CHECKPOINTABLE : 0)
               : PARALLEL | RETAINS_ENTRIES;
        }

    @Override
    public Object snapshotState()
        {
        ensureInitialized();
        if (!m_fStateCheckpointable)
            {
            throw new UnsupportedOperationException("Composite aggregator state is not checkpointable");
            }

        InvocableMap.EntryAggregator[] aAggregator = m_aAggregator;
        Object[]                       aoState      = new Object[aAggregator.length];
        for (int i = 0; i < aAggregator.length; i++)
            {
            aoState[i] = ((InvocableMap.StreamingAggregator) aAggregator[i]).snapshotState();
            }
        return aoState;
        }

    @Override
    public void restoreState(Object oState)
        {
        ensureInitialized();
        if (!m_fStateCheckpointable)
            {
            throw new UnsupportedOperationException("Composite aggregator state is not checkpointable");
            }
        if (!(oState instanceof Object[]) || ((Object[]) oState).length != m_aAggregator.length)
            {
            throw new IllegalArgumentException("Invalid composite aggregation state");
            }

        Object[] aoState = (Object[]) oState;
        for (int i = 0; i < m_aAggregator.length; i++)
            {
            ((InvocableMap.StreamingAggregator) m_aAggregator[i]).restoreState(aoState[i]);
            }
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Ensure that this aggregator is initialized.
     */
    protected void ensureInitialized()
        {
        if (!m_fInit)
            {
            InvocableMap.EntryAggregator[] aAggregator = m_aAggregator;
            int cAggregators = aAggregator.length;

            boolean fStreaming = true;
            boolean fContinuous = true;
            boolean fStateCheckpointable = true;
            for (int i = 0; i < cAggregators; i++)
                {
                if (!(aAggregator[i] instanceof InvocableMap.StreamingAggregator))
                    {
                    fStreaming = false;
                    fContinuous = false;
                    fStateCheckpointable = false;
                    break;
                    }
                InvocableMap.StreamingAggregator aggregator =
                        (InvocableMap.StreamingAggregator) aAggregator[i];
                fContinuous          &= aggregator.isContinuous();
                fStateCheckpointable &= aggregator.isStateCheckpointable();
                }

            m_fStreaming           = fStreaming;
            m_fContinuous          = fContinuous;
            m_fStateCheckpointable = fStateCheckpointable;
            if (!fStreaming)
                {
                m_setEntries = new HashSet();

                boolean fParallel = true;
                for (int i = 0; i < cAggregators; i++)
                    {
                    if (!(aAggregator[i] instanceof InvocableMap.ParallelAwareAggregator))
                        {
                        fParallel = false;
                        break;
                        }
                    }

                m_fParallel = fParallel;
                if (fParallel)
                    {
                    List[] aParallelResults = new List[cAggregators];
                    for (int i = 0; i < aParallelResults.length; i++)
                        {
                        aParallelResults[i] = new ArrayList<>();
                        }
                    m_aParallelResults = aParallelResults;
                    }
                }

            m_fInit = true;
            }
        }

    // ----- ExternalizableLite interface -----------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(DataInput in)
            throws IOException
        {
        int cAggregators = readInt(in);

        azzert(cAggregators < 16384, "Unexpected number of chained aggregators");

        InvocableMap.EntryAggregator[] aAggregator =
            new InvocableMap.EntryAggregator[cAggregators];

        for (int i = 0; i < cAggregators; i++)
            {
            aAggregator[i] = readObject(in);
            }
        m_aAggregator = aAggregator;
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(DataOutput out)
            throws IOException
        {
        InvocableMap.EntryAggregator[] aAggregator = m_aAggregator;

        int cAggregators = aAggregator.length;

        writeInt(out, cAggregators);
        for (int i = 0; i < cAggregators; i++)
            {
            writeObject(out, aAggregator[i]);
            }
        }


    // ----- PortableObject interface ---------------------------------------

    /**
    * {@inheritDoc}
    */
    public void readExternal(PofReader in)
            throws IOException
        {
        m_aAggregator = in.readArray(0, InvocableMap.EntryAggregator[]::new);
        }

    /**
    * {@inheritDoc}
    */
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeObjectArray(0, m_aAggregator);
        }


    // ----- accessors ------------------------------------------------------

    /**
    * Obtain the EntryAggregator array.
    *
    * @return the EntryAggregator array
    */
    public InvocableMap.EntryAggregator[] getAggregators()
        {
        return m_aAggregator;
        }


    // ----- Object methods -------------------------------------------------

    /**
    * Compare the CompositeAggregator with another object to determine
    * equality.
    *
    * @return true iff this CompositeAggregator and the passed object are
    *         equivalent
    */
    public boolean equals(Object o)
        {
        if (o instanceof CompositeAggregator)
            {
            CompositeAggregator that = (CompositeAggregator) o;
            return equalsDeep(this.m_aAggregator, that.m_aAggregator);
            }

        return false;
        }

    /**
    * Determine a hash value for the MultiExtractor object according to the
    * general {@link Object#hashCode()} contract.
    *
    * @return an integer hash value for this ValueExtractor object
    */
    public int hashCode()
        {
        InvocableMap.EntryAggregator[] aAggregator = m_aAggregator;

        int iHash = 0;
        for (int i = 0, c = aAggregator.length; i < c; i++)
            {
            iHash += aAggregator[i].hashCode();
            }
        return iHash;
        }

    /**
    * Return a human-readable description for this ValueExtractor.
    *
    * @return a String description of the ValueExtractor
    */
    public String toString()
        {
        StringBuilder sb = new StringBuilder();
        sb.append(ClassHelper.getSimpleName(getClass()))
          .append('(');

        InvocableMap.EntryAggregator[] aAggregator = m_aAggregator;
        for (int i = 0, c = aAggregator.length; i < c; i++)
            {
            if (i > 0)
                {
                sb.append(", ");
                }
            sb.append(aAggregator[i]);
            }
        sb.append(')');

        return sb.toString();
        }


    // ----- factory methods ------------------------------------------------

    /**
    * Create an instance of CompositeAggregator based on a specified
    * {@link com.tangosol.util.InvocableMap.EntryAggregator EntryAggregator}
    * array.  If all the aggregators in the specified array are instances of
    * {@link com.tangosol.util.InvocableMap.ParallelAwareAggregator
    * ParallelAwareAggregator}, then a parallel-aware instance of the
    * CompositeAggregator will be created.
    * <br>
    * If at least one of the specified aggregator is not parallel-aware, then
    * the resulting CompositeAggregator will not be parallel-aware and could
    * be ill-suited for aggregations run against large partitioned caches.
    *
    * @param aAggregator  an array of EntryAggregator objects; must contain
    *                      not less than two aggregators
    */
    public static CompositeAggregator createInstance(
            InvocableMap.EntryAggregator[] aAggregator)
        {
        return new CompositeAggregator(aAggregator);
        }

    // ----- inner classes --------------------------------------------------

    /**
    * Parallel implementation of the CompositeAggregator.
    *
    * @deprecated  As of Coherence 12.2.1.  Use CompositeAggregator instead.
    */
    @Deprecated
    public static class Parallel
            extends CompositeAggregator
        {
        /**
         * Default constructor (necessary for the ExternalizableLite
         * interface).
         */
        public Parallel()
            {
            }

        /**
         * Construct a CompositeParallelAggregator based on a specified
         * {@link com.tangosol.util.InvocableMap.EntryAggregator
         * EntryAggregator} array.
         *
         * @param aAggregator an array of ParallelAwareAggregator objects;
         *                    may not be null
         */
        protected Parallel(InvocableMap.EntryAggregator[] aAggregator)
            {
            super(aAggregator);
            }
        }

    // ----- data members ---------------------------------------------------

    /**
    * The underlying EntryAggregator array.
    */
    @JsonbProperty("aggregators")
    protected InvocableMap.EntryAggregator[] m_aAggregator;

    /**
     * Flag specifying whether this aggregator has been initialized.
     */
    protected transient boolean m_fInit;

    /**
     * Flag specifying whether streaming optimizations can be used.
     */
    protected transient boolean m_fStreaming;

    /**
     * Flag specifying whether all delegates support continuous maintenance.
     */
    protected transient boolean m_fContinuous;

    /**
     * Flag specifying whether all delegates support state checkpoints.
     */
    protected transient boolean m_fStateCheckpointable;

    /**
     * Flag specifying whether parallel optimizations can be used.
     */
    protected transient boolean m_fParallel;

    /**
     * A set of accumulated entries to aggregate.
     */
    protected transient Set m_setEntries;

    /**
     * An array of partial results for each aggregator.
     */
    protected transient List[] m_aParallelResults;
    }
