/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.aggregator;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.ClassHelper;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.LiteMap;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.extractor.ChainedExtractor;
import com.tangosol.util.extractor.MultiExtractor;
import com.tangosol.util.extractor.ReflectionExtractor;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.function.BinaryOperator;

import javax.json.bind.annotation.JsonbProperty;

/**
 * The GroupAggregator provides an ability to split a subset of entries in an
 * InvocableMap into a collection of non-intersecting subsets and then
 * aggregate them separately and independently. The splitting (grouping) is
 * performed using the results of the underlying ValueExtractor in such a way
 * that two entries will belong to the same group if and only if the result of
 * the corresponding {@link ValueExtractor#extract extract} call produces the
 * same value or tuple (list of values). After the entries are split into the
 * groups, the underlying aggregator is applied separately to each group. The
 * result of the aggregation by the GroupAggregator is a Map that has distinct
 * values (or tuples) as keys and results of the individual aggregation as
 * values. Additionally, those results could be further reduced using an
 * optional Filter object.
 * <p>
 * Informally speaking, this aggregator is analogous to the SQL "group by" and
 * "having" clauses. Note that the "having" Filter is applied independently on
 * each server against the partial aggregation results; this generally implies
 * that data affinity is required to ensure that all required data used to
 * generate a given result exists within a single cache partition.
 * In other words, the "group by" predicate should not span multiple
 * partitions if the "having" clause is used.
 * <p>
 * The GroupAggregator is somewhat similar to the {@link DistinctValues}
 * aggregator, which returns back a list of distinct values (tuples) without
 * performing any additional aggregation work.
 * <p>
 * <b>Unlike many other concrete EntryAggregator implementations that are
 * constructed directly, instances of GroupAggregator should only be created
 * using one of the factory methods:</b>
 * {@link #createInstance(ValueExtractor, InvocableMap.EntryAggregator)
 * createInstance(extractor, aggregator)},
 * {@link #createInstance(ValueExtractor, InvocableMap.EntryAggregator, Filter)
 * createInstance(extractor, aggregator, filter)},
 * {@link #createInstance(String, InvocableMap.EntryAggregator)
 * createInstance(sMethod, aggregator)}
 * {@link #createInstance(String, InvocableMap.EntryAggregator, Filter)
 * createInstance(sMethod, aggregator, filter)}
 *
 * @param <K>  the type of the Map entry keys
 * @param <V>  the type of the Map entry values
 * @param <T>  the type of the value to extract from
 * @param <E>  the type of the extracted value
 * @param <R>  the type of the group aggregator result
 *
 * @author gg  2006.02.15
 * @author as  2014.11.09
 *
 * @since Coherence 3.2
 */
@SuppressWarnings("unchecked")
public class GroupAggregator<K, V, T, E, R>
        extends    ExternalizableHelper
        implements InvocableMap.StreamingAggregator<K, V, Map<E, Object>, Map<E, R>>,
                   ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor (necessary for the ExternalizableLite interface).
     */
    public GroupAggregator()
        {
        }

    /**
     * Construct a GroupAggregator based on a specified ValueExtractor and
     * underlying EntryAggregator.
     *
     * @param extractor   a ValueExtractor object that is used to split
     *                    InvocableMap entries into non-intersecting subsets;
     *                    may not be null
     * @param aggregator  an EntryAggregator object; may not be null
     * @param filter      an optional Filter object used to filter out
     *                    results of individual group aggregation results
     */
    protected GroupAggregator(ValueExtractor<? super T, ? extends E> extractor,
                              InvocableMap.EntryAggregator<? super K, ? super V, R> aggregator,
                              Filter filter)
        {
        azzert(extractor != null && aggregator != null);

        m_extractor  = extractor;
        m_aggregator = aggregator;
        m_filter     = filter;
        }

    // ----- StreamingAggregator interface ----------------------------------

    @Override
    public InvocableMap.StreamingAggregator<K, V, Map<E, Object>, Map<E, R>> supply()
        {
        return new GroupAggregator<>(m_extractor, m_aggregator, m_filter);
        }

    @Override
    public boolean accumulate(InvocableMap.Entry<? extends K, ? extends V> entry)
        {
        ensureInitialized();

        if (entry.isPresent())
            {
            E groupKey = entry.extract(m_extractor);

            // add the entry to the corresponding group
            if (isDelegateStreaming())
                {
                InvocableMap.StreamingAggregator<? super K, ? super V, Object, R> aggregator =
                        (InvocableMap.StreamingAggregator<? super K, ? super V, Object, R>)
                                m_mapResults.computeIfAbsent(groupKey, k -> streaming(m_aggregator).supply());
                aggregator.accumulate(entry);
                }
            else
                {
                Set<InvocableMap.Entry<? extends K, ? extends V>> setEntries =
                        (Set<InvocableMap.Entry<? extends K, ? extends V>>)
                                m_mapResults.computeIfAbsent(groupKey, k -> new HashSet<>());
                setEntries.add(entry);
                }
            }

        return true;
        }

    @Override
    public boolean combine(Map<E, Object> partialResult)
        {
        ensureInitialized();

        for (Map.Entry<E, Object> part : partialResult.entrySet())
            {
            E groupKey = part.getKey();

            if (isDelegateStreaming())
                {
                InvocableMap.StreamingAggregator<? super K, ? super V, Object, R> aggregator =
                        (InvocableMap.StreamingAggregator<? super K, ? super V, Object, R>)
                                m_mapResults.computeIfAbsent(groupKey, k -> streaming(m_aggregator).supply());
                aggregator.combine(part.getValue());
                }
            else if (isDelegateParallel())
                {
                List listResults = (List) m_mapResults.computeIfAbsent(groupKey, k -> new ArrayList<>());
                listResults.add(part.getValue());
                }
            else
                {
                Set<InvocableMap.Entry<? extends K, ? extends V>> setEntries =
                        (Set<InvocableMap.Entry<? extends K, ? extends V>>)
                                m_mapResults.computeIfAbsent(groupKey, k -> new HashSet<>());
                setEntries.addAll((Collection) part.getValue());
                }
            }

        return true;
        }

    @Override
    public Map<E, Object> getPartialResult()
        {
        ensureInitialized();

        boolean fStreaming = isDelegateStreaming();

        if (!fStreaming && !isDelegateParallel())
            {
            return m_mapResults;
            }

        Map<E, Object> mapResults = new LiteMap<>();
        for (Map.Entry<E, Object> entry : m_mapResults.entrySet())
            {
            Object oResult;
            if (fStreaming)
                {
                oResult = ((InvocableMap.StreamingAggregator) entry.getValue()).getPartialResult();
                }
            else // must be parallel
                {
                oResult = parallel(m_aggregator).getParallelAggregator()
                        .aggregate((Set<InvocableMap.Entry<? extends K, ? extends V>>) entry.getValue());
                }

            mapResults.put(entry.getKey(), oResult);
            }
        return mapResults;
        }

    @Override
    public Map<E, R> finalizeResult()
        {
        ensureInitialized();

        boolean   fStreaming     = isDelegateStreaming();
        boolean   fParallelAware = isDelegateParallel();
        Filter    filter         = m_filter;
        Map<E, R> mapResults     = new LiteMap<>();

        for (Map.Entry<E, Object> entry : m_mapResults.entrySet())
            {
            R result =
                    fStreaming     ? ((InvocableMap.StreamingAggregator<? super K, ? super V, Object, R>) entry.getValue()).finalizeResult() :
                    fParallelAware ? parallel(m_aggregator).aggregateResults((Collection<Object>) entry.getValue())
                                   : m_aggregator.aggregate((Set<InvocableMap.Entry<? extends K, ? extends V>>) entry.getValue());

            if (filter == null || filter.evaluate(result))
                {
                mapResults.put(entry.getKey(), result);
                }
            }

        return mapResults;
        }

    @Override
    public int characteristics()
        {
        ensureInitialized();

        return isDelegateStreaming()
                ? streaming(m_aggregator).characteristics()
                : PARALLEL | RETAINS_ENTRIES; //InvocableMap.StreamingAggregator.super.characteristics();
        }

    // ----- accessors ------------------------------------------------------

    /**
     * Obtain the underlying ValueExtractor.
     *
     * @return the underlying ValueExtractor
     */
    public ValueExtractor<?, ? extends E> getExtractor()
        {
        return m_extractor;
        }

    /**
     * Obtain the underlying EntryAggregator.
     *
     * @return the underlying EntryAggregator
     */
    public InvocableMap.EntryAggregator<? super K, ? super V, R> getAggregator()
        {
        return m_aggregator;
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Ensure that this aggregator is initialized.
     */
    protected void ensureInitialized()
        {
        if (!m_fInit)
            {
            m_mapResults = new LiteMap<>();

            m_fStreaming = m_aggregator instanceof InvocableMap.StreamingAggregator;
            if (!m_fStreaming)
                {
                m_fParallel = m_aggregator instanceof InvocableMap.ParallelAwareAggregator;
                }

            m_fInit = true;
            }
        }

    /**
     * Convert the specified aggregator to StreamingAggregator.
     *
     * @param aggregator  the aggregator to convert
     *
     * @return an instance of a StreamingAggregator
     */
    protected InvocableMap.StreamingAggregator<? super K, ? super V, Object, R> streaming(InvocableMap.EntryAggregator<? super K, ? super V, R> aggregator)
        {
        return (InvocableMap.StreamingAggregator<? super K, ? super V, Object, R>) aggregator;
        }

    /**
     * Convert the specified aggregator to ParallelAwareAggregator.
     *
     * @param aggregator  the aggregator to convert
     *
     * @return an instance of a ParallelAwareAggregator
     */
    protected InvocableMap.ParallelAwareAggregator<? super K, ? super V, Object, R> parallel(InvocableMap.EntryAggregator<? super K, ? super V, R> aggregator)
        {
        return (InvocableMap.ParallelAwareAggregator<? super K, ? super V, Object, R>) aggregator;
        }

    /**
     * Return <code>true</code> if the underlying aggregator is a StreamingAggregator.
     *
     * @return <code>true</code> if the underlying aggregator is a StreamingAggregator
     */
    protected boolean isDelegateStreaming()
        {
        return m_fStreaming;
        }

    /**
     * Return <code>true</code> if the underlying aggregator is a ParallelAwareAggregator.
     *
     * @return <code>true</code> if the underlying aggregator is a ParallelAwareAggregator
     */
    protected boolean isDelegateParallel()
        {
        return m_fParallel;
        }

    protected static <T> BinaryOperator<T> throwingMerger()
        {
        return (u, v) -> { throw new IllegalStateException("Duplicate group key"); };
        }

    // ----- ExternalizableLite interface -----------------------------------

    @Override
    public void readExternal(DataInput in)
            throws IOException
        {
        m_extractor  = readObject(in);
        m_aggregator = readObject(in);
        m_filter     = readObject(in);
        }

    @Override
    public void writeExternal(DataOutput out)
            throws IOException
        {
        writeObject(out, m_extractor);
        writeObject(out, m_aggregator);
        writeObject(out, m_filter);
        }

    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader in)
            throws IOException
        {
        m_extractor  = in.readObject(0);
        m_aggregator = in.readObject(1);
        m_filter     = in.readObject(2);
        }

    @Override
    public void writeExternal(PofWriter out)
            throws IOException
        {
        out.writeObject(0, m_extractor);
        out.writeObject(1, m_aggregator);
        out.writeObject(2, m_filter);
        }

    // ----- Object methods -------------------------------------------------

    /**
     * Compare the GroupAggregator with another object to determine
     * equality.
     *
     * @return true iff this GroupAggregator and the passed object are
     *         equivalent
     */
    public boolean equals(Object o)
        {
        if (o instanceof GroupAggregator)
            {
            GroupAggregator that = (GroupAggregator) o;
            return equals(this.m_extractor,  that.m_extractor)
                && equals(this.m_aggregator, that.m_aggregator);
            }

        return false;
        }

    /**
     * Determine a hash value for the GroupAggregator object according to the
     * general {@link Object#hashCode()} contract.
     *
     * @return an integer hash value for this GroupAggregator object
     */
    public int hashCode()
        {
        return m_extractor.hashCode() + m_aggregator.hashCode();
        }

    /**
     * Return a human-readable description for this GroupAggregator.
     *
     * @return a String description of the GroupAggregator
     */
    public String toString()
        {
        return ClassHelper.getSimpleName(getClass()) +
          '(' + m_extractor + ", " + m_aggregator +
          (m_filter == null ? "" : ", " + m_filter) +
          ')';
        }

    // ----- factory methods ------------------------------------------------

    /**
     * Create an instance of GroupAggregator based on a specified method
     * name(s) and an {@link com.tangosol.util.InvocableMap.EntryAggregator
     * EntryAggregator}.
     * <br>
     * If the specified underlying aggregator is an instance of
     * {@link com.tangosol.util.InvocableMap.ParallelAwareAggregator
     * ParallelAwareAggregator}, then a parallel-aware instance of the
     * GroupAggregator will be created. Otherwise, the resulting
     * GroupAggregator will not be parallel-aware and could be ill-suited for
     * aggregations run against large partitioned caches.
     *
     * @param sMethod     a method name or a comma-delimited sequence of names
     *                     that results in a {@link ReflectionExtractor}
     *                     or a {@link MultiExtractor} that will be used to
     *                     split InvocableMap entries into distinct groups
     * @param aggregator  an underlying EntryAggregator
     */
    public static <K, V, R> GroupAggregator<K, V, Object, Object, R> createInstance(
            String sMethod,
            InvocableMap.EntryAggregator<K, V, R> aggregator)
        {
        return createInstance(sMethod, aggregator, null);
        }

    /**
     * Create an instance of GroupAggregator based on a specified method
     * name(s), an {@link com.tangosol.util.InvocableMap.EntryAggregator
     * EntryAggregator} and a result evaluation filter.
     * <br>
     * If the specified underlying aggregator is an instance of
     * {@link com.tangosol.util.InvocableMap.ParallelAwareAggregator
     * ParallelAwareAggregator}, then a parallel-aware instance of the
     * GroupAggregator will be created. Otherwise, the resulting GroupAggregator
     * will not be parallel-aware and could be ill-suited for aggregations run
     * against large partitioned caches.
     *
     * @param sMethod    a method name or a comma-delimited sequence of names
     *                   that results in a {@link ReflectionExtractor} or a
     *                   {@link MultiExtractor} that will be used to split
     *                   InvocableMap entries into distinct groups
     * @param aggregator an underlying EntryAggregator
     * @param filter     an optional Filter object that will be used to evaluate
     *                   results of each individual group aggregation
     */
    public static <K, V, R> GroupAggregator<K, V, Object, Object, R> createInstance(
            String sMethod,
            InvocableMap.EntryAggregator<K, V, R> aggregator,
            Filter filter)
        {
        ValueExtractor extractor =
               sMethod.indexOf(',') >= 0 ? new MultiExtractor(sMethod) :
               sMethod.indexOf('.') >= 0 ? new ChainedExtractor(sMethod) :
                          (ValueExtractor) new ReflectionExtractor(sMethod);
        return createInstance(extractor, aggregator, filter);
        }

    /**
     * Create an instance of GroupAggregator based on a specified extractor and
     * an {@link com.tangosol.util.InvocableMap.EntryAggregator
     * EntryAggregator}.
     * <br>
     * If the specified aggregator is an instance of
     * {@link com.tangosol.util.InvocableMap.ParallelAwareAggregator
     * ParallelAwareAggregator}, then a parallel-aware instance of the
     * GroupAggregator will be created. Otherwise, the resulting GroupAggregator
     * will not be parallel-aware and could be ill-suited for aggregations run
     * against large partitioned caches.
     *
     * @param extractor  a ValueExtractor that will be used to split a set of
     *                   InvocableMap entries into distinct groups
     * @param aggregator an underlying EntryAggregator
     */
    public static <K, V, T, E, R> GroupAggregator<K, V, T, E, R> createInstance(
            ValueExtractor<? super T, ? extends E> extractor,
            InvocableMap.EntryAggregator<K, V, R> aggregator)
        {
        return createInstance(extractor, aggregator, null);
        }

    /**
     * Create an instance of GroupAggregator based on a specified extractor
     * and an {@link com.tangosol.util.InvocableMap.EntryAggregator
     * EntryAggregator} and a result evaluation filter.
     * <br>
     * If the specified aggregator is an instance of
     * {@link com.tangosol.util.InvocableMap.ParallelAwareAggregator
     * ParallelAwareAggregator}, then a parallel-aware instance of the
     * GroupAggregator will be created. Otherwise, the resulting
     * GroupAggregator will not be parallel-aware and could be ill-suited for
     * aggregations run against large partitioned caches.
     *
     * @param extractor   a ValueExtractor that will be used to split a set of
     *                    InvocableMap entries into distinct groups
     * @param aggregator  an underlying EntryAggregator
     * @param filter      an optional Filter object used to filter out results
     *                    of individual group aggregation results
     */
    public static <K, V, T, E, R> GroupAggregator<K, V, T, E, R> createInstance(
                                          ValueExtractor<? super T, ? extends E> extractor,
                                          InvocableMap.EntryAggregator<? super K, ? super V, R> aggregator,
                                          Filter filter)
        {
        return new GroupAggregator<>(extractor, aggregator, filter);
        }

    // ----- inner classes --------------------------------------------------

    /**
    * Parallel implementation of the GroupAggregator.
    *
    * @deprecated  As of Coherence 12.2.1.  Use GroupAggregator instead.
    */
    @Deprecated
    public static class Parallel<K, V, T, E, R>
            extends    GroupAggregator<K, V, T, E, R>
        {
        // ----- constructors -------------------------------------------

        /**
         * Default constructor (necessary for the ExternalizableLite
         * interface).
         */
        public Parallel()
            {
            }

        /**
         * Construct a Parallel aggregator based on a specified ValueExtractor
         * and underlying ParallelAwareAggregator.
         *
         * @param extractor   a ValueExtractor object; may not be null
         * @param aggregator  an EntryAggregator object; may not be null
         * @param filter      an optional Filter object used to filter out
         *                    results of individual group aggregation results
         */
        protected Parallel(ValueExtractor<? super T, ? extends E> extractor,
                           InvocableMap.EntryAggregator<? super K, ? super V, R> aggregator,
                           Filter<?> filter)
            {
            super(extractor, aggregator, filter);
            }
        }

    // ----- data members ---------------------------------------------------

    /**
     * The underlying ValueExtractor.
     */
    @JsonbProperty("extractor")
    protected ValueExtractor<? super T, ? extends E> m_extractor;

    /**
     * The underlying EntryAggregator.
     */
    @JsonbProperty("aggregator")
    protected InvocableMap.EntryAggregator<? super K, ? super V, R> m_aggregator;

    /**
     * The Filter object representing the "having" clause of this "group by"
     * aggregator.
     */
    @JsonbProperty("filter")
    protected Filter m_filter;

    /**
     * Flag specifying whether this aggregator has been initialized.
     */
    protected transient boolean m_fInit;

    /**
     * Flag specifying whether streaming optimizations can be used.
     */
    protected transient boolean m_fStreaming;

    /**
     * Flag specifying whether parallel optimizations can be used.
     */
    protected transient boolean m_fParallel;

    /**
     * A map of partial results to aggregate.
     */
    protected transient Map<E, Object> m_mapResults;
    }
