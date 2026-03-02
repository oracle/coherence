/*
 * Copyright (c) 2000, 2026, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.filter;

import com.tangosol.internal.util.invoke.Lambdas;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.ChainedCollection;
import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMapHelper;
import com.tangosol.util.MapIndex;
import com.tangosol.util.QueryMap;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.extractor.ChainedExtractor;
import com.tangosol.util.extractor.ReflectionExtractor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.json.bind.annotation.JsonbProperty;

/**
* Base Filter implementation for doing extractor-based processing.
*
* @param <T> the type of the input argument to the filter
* @param <E> the type of the attribute extracted from the input argument
*
* @author cp/gg 2002.11.01
*/
public abstract class ExtractorFilter<T, E>
        extends    AbstractQueryRecorderFilter<T>
        implements EntryFilter<Object, T>, ExternalizableLite, PortableObject, IndexAwareFilter<Object, T>
    {
    // ----- constructors ---------------------------------------------------

    /**
    * Default constructor (necessary for the ExternalizableLite interface).
    */
    public ExtractorFilter()
        {
        }

    /**
    * Construct a ExtractorFilter for a given ValueExtractor.
    *
    * @param extractor the ValueExtractor to use by this filter
    */
    public ExtractorFilter(ValueExtractor<? super T, ? extends E> extractor)
        {
        azzert(extractor != null);

        m_extractor = Lambdas.ensureRemotable(extractor);
        }

    /**
    * Construct an ExtractorFilter for a given method name.
    *
    * @param sMethod  a method name to make a {@link ReflectionExtractor}
    *                 for; this parameter can also be a dot-delimited
    *                 sequence of method names which would result in an
    *                 ExtractorFilter based on the {@link ChainedExtractor}
    *                 that is based on an array of corresponding
    *                 ReflectionExtractor objects
    */
    public ExtractorFilter(String sMethod)
        {
        m_extractor = sMethod.indexOf('.') < 0
                ? new ReflectionExtractor<>(sMethod)
                : new ChainedExtractor<>(sMethod);
        }


    // ----- Filter interface -----------------------------------------------

    /**
    * {@inheritDoc}
    */
    public boolean evaluate(T target)
        {
        return evaluateExtracted(extract(target));
        }


    // ----- EntryFilter interface ------------------------------------------


    /**
    * {@inheritDoc}
    */
    public boolean evaluateEntry(Map.Entry<?, ? extends T> entry)
        {
        ValueExtractor<? super T, ? extends E> extractor = getValueExtractor();

        return evaluateExtracted(entry instanceof QueryMap.Entry
                           ? ((QueryMap.Entry<?, ? extends T>) entry).extract(extractor)
                           : InvocableMapHelper.extractFromEntry(extractor, entry));
        }


    // ----- inheritance support and accessors ------------------------------

    /**
    * Evaluate the specified extracted value.
    *
    * @param extracted  an extracted value to evaluate
    *
    * @return true iff the test passes
    */
    protected abstract boolean evaluateExtracted(E extracted);

    /**
    * Get the result of ValueExtractor invocation.
    *
    * @param o  the object on which to invoke the ValueExtractor;
    *           must not be null
    *
    * @return the result of the method invocation
    *
    * @throws com.tangosol.util.WrapperException if this ValueExtractor
    *          encounters an exception in the course of extracting the value
    */
    protected E extract(T o)
        {
        return getValueExtractor().extract(o);
        }

    /**
    * Obtain the ValueExtractor used by this filter.
    *
    * @return the ValueExtractor used by this filter
    */
    public ValueExtractor<? super T, ? extends E> getValueExtractor()
        {
        return m_extractor;
        }


    // ----- IndexAwareFilter interface -------------------------------------

    /**
    * {@inheritDoc}
    */
    public int calculateEffectiveness(Map mapIndexes, Set setKeys)
        {
        MapIndex index = (MapIndex) mapIndexes.get(getValueExtractor());
        return isInapplicableIndex(index)
               ? -1
               : setKeys.size();
        }

    /**
    * {@inheritDoc}
    */
    @SuppressWarnings("unchecked")
    public Filter applyIndex(Map mapIndexes, Set setKeys)
        {
        MapIndex<?, ?, E> index = (MapIndex) mapIndexes.get(getValueExtractor());
        if (isInapplicableIndex(index))
            {
            // there is no relevant index, or partitioned index is incomplete;
            // fall back to entry-by-entry evaluation
            return this;
            }

        Map<E, ? extends Set<?>> mapValues = index.getIndexContents();
        List<Set<?>>             listMatch = new ArrayList<>(mapValues.size());

        for (Map.Entry<E, ? extends Set<?>> entry : mapValues.entrySet())
            {
            if (evaluateExtracted(entry.getKey()))
                {
                listMatch.add(ensureSafeSet(entry.getValue()));
                }
            }
        setKeys.retainAll(new ChainedCollection<>(listMatch.toArray(Set[]::new)));
        return null;
        }


    // ----- helpers --------------------------------------------------------

    /**
    * Helper method to calculate effectiveness (or rather ineffectiveness) of
    * a simple iteration against a key set that has to be performed due to an
    * absence of corresponding index.
    *
    * @param cKeys  the number of keys to iterate through
    *
    * @return an effectiveness estimate
    */
    public static int calculateIteratorEffectiveness(int cKeys)
        {
        // convert int to long to prevent integer overflow
        long lCost = ((long) EVAL_COST) * cKeys;
        return lCost <= Integer.MAX_VALUE ? (int) lCost : Integer.MAX_VALUE;
        }


    // ----- ExternalizableLite interface -----------------------------------

    @Override
    public void readExternal(DataInput in)
            throws IOException
        {
        m_extractor = readObject(in);
        }

    @Override
    public void writeExternal(DataOutput out)
            throws IOException
        {
        writeObject(out, m_extractor);
        }


    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader in)
            throws IOException
        {
        m_extractor = in.readObject(0);
        }

    @Override
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeObject(0, m_extractor);
        }

    // ------ helper method -------------------------------------------------

    /**
     * Return a non-null Set.
     *
     * @param set the set to ensure
     *
     * @return the safe set
     */
    protected static Set ensureSafeSet(Set set)
        {
        return set == null ? Collections.emptySet() : set;
        }

    /**
     * Return {@code true} if the index cannot be used for optimization.
     *
     * @param index  the index to test
     *
     * @return {@code true} if the index cannot be used for optimization
     */
    protected static boolean isInapplicableIndex(MapIndex index)
        {
        return index == null || isIncompletePartitionedIndex(index);
        }

    /**
     * Return {@code true} if the specified index is a partitioned composite
     * index in an incomplete state (e.g. at least one partition is missing the
     * corresponding per-partition index).
     *
     * @param index  the index to test
     *
     * @return {@code true} if the index is an incomplete partitioned index
     */
    protected static boolean isIncompletePartitionedIndex(MapIndex index)
        {
        return index != null
                && "com.tangosol.internal.util.PartitionedIndexMap$PartitionedIndex".equals(index.getClass().getName())
                && index.isPartial();
        }

    /**
     * Return {@code true} if the supplied candidate and index-cardinality data
     * indicates that it is cheaper to evaluate keys using a forward index.
     *
     * @param setKeys       the current candidate key set
     * @param colIndexSets  the index sets contributing to the matching range
     *
     * @return {@code true} if forward-index evaluation should be used
     */
    protected static boolean shouldEvaluateUsingForwardIndex(Set setKeys, Collection<? extends Set> colIndexSets)
        {
        return shouldEvaluateUsingForwardIndex(setKeys, colIndexSets, FORWARD_INDEX_EVAL_CARDINALITY_FACTOR);
        }

    /**
     * Return {@code true} if the supplied candidate and index-cardinality data
     * indicates that it is cheaper to evaluate keys using a forward index.
     *
     * @param setKeys       the current candidate key set
     * @param colIndexSets  the index sets contributing to the matching range
     * @param nFactor       the cardinality factor used to determine threshold
     *
     * @return {@code true} if forward-index evaluation should be used
     */
    protected static boolean shouldEvaluateUsingForwardIndex(Set setKeys, Collection<? extends Set> colIndexSets,
                                                             int nFactor)
        {
        if (setKeys == null || setKeys.isEmpty())
            {
            return false;
            }

        long cThreshold = (long) setKeys.size() * nFactor;
        long cEntries   = 0;

        for (Set set : colIndexSets)
            {
            cEntries += ensureSafeSet(set).size();
            if (cEntries > cThreshold)
                {
                return true;
                }
            }

        return false;
        }

    /**
     * Return {@code true} if the supplied index appears to have forward-index
     * support for the supplied keys.
     *
     * @param index    the index to test
     * @param setKeys  the current candidate key set
     *
     * @return {@code true} if forward-index access is available
     */
    protected static boolean isForwardIndexSupported(MapIndex index, Set setKeys)
        {
        if (index == null || setKeys == null || setKeys.isEmpty())
            {
            return false;
            }

        Object oKey = setKeys.iterator().next();
        return index.get(oKey) != MapIndex.NO_VALUE;
        }

    // ----- constants ------------------------------------------------------

    /**
    * The evaluation cost as a factor to the single index access operation.
    *
    * @see IndexAwareFilter#calculateEffectiveness(Map, Set)
    */
    public static int EVAL_COST = 1000;

    /**
     * Cardinality factor used to determine when to switch from inverse-index
     * retain/remove to forward-index key evaluation.
     */
    public static int FORWARD_INDEX_EVAL_CARDINALITY_FACTOR = 1;


    // ----- data members ---------------------------------------------------

    /**
    * The ValueExtractor used by this filter.
    */
    @JsonbProperty("extractor")
    protected ValueExtractor<? super T, ? extends E> m_extractor;
    }
