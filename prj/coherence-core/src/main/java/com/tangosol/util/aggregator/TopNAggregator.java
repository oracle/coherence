/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.aggregator;

import com.tangosol.internal.util.invoke.Lambdas;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.SortedBag;
import com.tangosol.util.ValueExtractor;

import com.tangosol.util.comparator.SafeComparator;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbTransient;

/**
 * TopNAggregator is a ParallelAwareAggregator that aggregates the top <i>N</i>
 * extracted values into an array.  The extracted values must not be null, but
 * do not need to be unique.
 *
 * @param <K>  the type of the Map entry keys
 * @param <V>  the type of the Map entry values
 * @param <T>  the type of the value to extract from
 * @param <E>  the type of the extracted value
 *
 * @author rhl 2013.04.24
 * @since  12.1.3
 */
public class TopNAggregator<K, V, T, E>
        implements InvocableMap.StreamingAggregator<K, V, TopNAggregator.PartialResult<E>, E[]>,
                   ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Default constructor.
     */
    public TopNAggregator()
        {}

    /**
     * Construct a TopNAggregator that will aggregate the top extracted values,
     * as determined by the specified comparator.
     *
     * @param extractor   the extractor
     * @param comparator  the comparator for extracted values
     * @param cResults    the maximum number of results to return
     */
    public TopNAggregator(ValueExtractor<? super T, ? extends E> extractor, Comparator<? super E> comparator, int cResults)
        {
        m_extractor  = Lambdas.ensureRemotable(extractor);
        m_cResults   = cResults;
        m_comparator = comparator == null ? SafeComparator.INSTANCE : comparator;
        }

    // ----- StreamingAggregator methods -------------------------------------

    @Override
    public InvocableMap.StreamingAggregator<K, V, PartialResult<E>, E[]> supply()
        {
        return new TopNAggregator<>(m_extractor, m_comparator, m_cResults);
        }

    @Override
    public boolean accumulate(InvocableMap.Entry<? extends K, ? extends V> entry)
        {
        ensureInitialized();

        m_result.add(entry.extract(m_extractor));
        return true;
        }

    @Override
    public boolean combine(PartialResult<E> partialResult)
        {
        ensureInitialized();

        m_result.merge(partialResult);
        return true;
        }

    @Override
    public PartialResult<E> getPartialResult()
        {
        ensureInitialized();
        return m_result;
        }

    @Override
    public E[] finalizeResult()
        {
        ensureInitialized();
        E[] aResult = (E[]) m_result.toArray();
        Collections.reverse(Arrays.asList(aResult));
        m_fInit = false;
        return aResult;
        }

    @Override
    public int characteristics()
        {
        return PARALLEL | PRESENT_ONLY;
        }

    // ----- internal methods -----------------------------------------------

    /**
     * Ensure that this aggregator is initialized.
     */
    protected void ensureInitialized()
        {
        if (!m_fInit)
            {
            m_result = new PartialResult<>(m_comparator, m_cResults);
            m_fInit  = true;
            }
        }

    // ----- ExternalizableHelper methods -----------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_fParallel  = in.readBoolean();
        m_extractor  = ExternalizableHelper.readObject(in);
        m_comparator = ExternalizableHelper.readObject(in);
        m_cResults   = in.readInt();
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        out.writeBoolean(m_fParallel);
        ExternalizableHelper.writeObject(out, m_extractor);
        ExternalizableHelper.writeObject(out, m_comparator);
        out.writeInt(m_cResults);
        }

    // ----- PortableObject methods -----------------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_fParallel  = in.readBoolean(0);
        m_extractor  = in.readObject(1);
        m_comparator = in.readObject(2);
        m_cResults   = in.readInt(3);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeBoolean(0, m_fParallel);
        out.writeObject (1, m_extractor);
        out.writeObject (2, m_comparator);
        out.writeInt    (3, m_cResults);
        }

    // ----- inner class: PartialResult -------------------------------------

    /**
     * The sorted partial result.
     */
    public static class PartialResult<E>
            extends SortedBag<E>
            implements ExternalizableLite, PortableObject
        {
        /**
         * Default constructor.
         */
        public PartialResult()
            {
            }

        /**
         * Construct a PartialResult using the specified comparator.
         *
         * @param comparator  the comparator
         * @param cMaxSize    the maximum size of this partial result
         */
        public PartialResult(Comparator<? super E> comparator, int cMaxSize)
            {
            super(comparator);

            m_comparator_copy = comparator;
            m_cMaxSize        = cMaxSize;
            }

        /**
         * Merge single PartialResult into this PartialResult.
         *
         * @param result  the partial result to merge
         *
         * @return  this PartialResult
         */
        public PartialResult<E> merge(PartialResult<E> result)
            {
            // if we have already accumulated N "merged" results, use the fact
            // that the partial results are sorted; we don't need to consider
            // any elements that are lesser-than the first element in the
            // merged results bag so far

            Iterator<E> iterValues = size() >= m_cMaxSize
                                ? result.tailBag(first()).iterator()
                                : result.iterator();
            addAll(iterValues);

            return this;
            }

        /**
         * Add all specified values to this PartialResult.
         *
         * @param iterValues  the values to add
         */
        public void addAll(Iterator<E> iterValues)
            {
            int cCurSize  = size();
            E   elemFirst = null;

            while (iterValues.hasNext())
                {
                E value = iterValues.next();
                if (value == null)
                    {
                    continue;
                    }

                if (cCurSize < m_cMaxSize)
                    {
                    super.add(value);
                    }
                else
                    {
                    if (elemFirst == null)
                        {
                        elemFirst = first();
                        }
                    if (m_comparator.compare(value, elemFirst) > 0)
                        {
                        super.add(value);
                        removeFirst();
                        elemFirst = null;
                        }
                    }

                ++cCurSize;
                }
            }

        @Override
        public boolean add(E value)
            {
            if (size() < m_cMaxSize)
                {
                return super.add(value);
                }
            else
                {
                if (m_comparator.compare(value, first()) > 0)
                    {
                    removeFirst();
                    super.add(value);
                    return true;
                    }
                else
                    {
                    return false;
                    }
                }
            }

        // ----- ExternalizableLite methods ---------------------------------

        @Override
        public void readExternal(DataInput in) throws IOException
            {
            m_comparator = ExternalizableHelper.readObject(in);
            m_cMaxSize   = ExternalizableHelper.readInt(in);
            m_map        = instantiateInternalMap(m_comparator);

            int cElems = in.readInt();
            for (int i = 0; i < cElems; i++)
                {
                add(ExternalizableHelper.readObject(in));
                }
            m_comparator_copy = m_comparator; // TODO (rlubke) remove after proper JSON serialization integration
            }

        @Override
        public void writeExternal(DataOutput out) throws IOException
            {
            ExternalizableHelper.writeObject(out, m_comparator);
            ExternalizableHelper.writeInt(out, m_cMaxSize);

            out.writeInt(size());
            for (Iterator iter = iterator(); iter.hasNext(); )
                {
                ExternalizableHelper.writeObject(out, iter.next());
                }
            }

        // ----- PortableObject methods -------------------------------------

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            m_comparator = in.readObject(0);
            m_cMaxSize   = in.readInt(1);
            m_map        = instantiateInternalMap(m_comparator);

            in.readCollection(2, this);
            m_comparator_copy = m_comparator; // TODO (rlubke) remove after proper JSON serialization integration
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeObject(0, m_comparator);
            out.writeInt(1, m_cMaxSize);
            out.writeCollection(2, this);
            }

        // ---- data members ------------------------------------------------

        /**
         * The maximum size of this partial result.
         */
        @JsonbTransient
        protected int m_cMaxSize;

        /**
         * The comparator used to compare logical elements.  Developers should <em>NOT</em> rely on this field, rely
         * on {@link #m_comparator} instead as this field will eventually be removed.
         */
        @JsonbTransient
        protected Comparator<? super E> m_comparator_copy; // TODO (rlubke) remove after proper JSON serialization integration
        }

    // ----- data members ---------------------------------------------------

    /**
     * True iff this aggregator is to be used in parallel.
     */
    @JsonbProperty("parallel")
    protected boolean m_fParallel;

    /**
     * The ValueExtractor used by this aggregator.
     */
    @JsonbProperty("extractor")
    protected ValueExtractor<? super T, ? extends E> m_extractor;

    /**
     * The Comparator used to order the extracted values.
     */
    @JsonbProperty("comparator")
    protected Comparator<? super E> m_comparator;

    /**
     * The maximum number of results to include in the aggregation result.
     */
    @JsonbProperty("results")
    protected int m_cResults;

    /**
     * The flag specifying whether this aggregator has been initialized.
     */
    private transient boolean m_fInit;

    /**
     * The result accumulator.
     */
    private transient PartialResult<E> m_result;
    }
