/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.stream;

import com.tangosol.io.ExternalizableLite;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import com.tangosol.io.pof.PortableObject;
import com.tangosol.net.NamedCache;
import com.tangosol.net.PartitionedService;
import com.tangosol.net.cache.ContinuousQueryCache;
import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMap;

import com.tangosol.util.stream.BaseRemoteStream;
import com.tangosol.util.stream.RemoteCollector;
import com.tangosol.util.stream.RemoteCollectors;
import com.tangosol.util.stream.RemotePipeline;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.Collection;

import java.util.Comparator;
import java.util.Set;
import java.util.function.Function;

import java.util.stream.BaseStream;
import java.util.stream.Stream;

import javax.json.bind.annotation.JsonbProperty;

/**
 * Abstract base class for stream pipeline implementations.
 *
 * @author as  2014.08.26
 */
public abstract class AbstractPipeline<K, V, E_IN, E_OUT, S_IN extends BaseStream<E_IN, S_IN>, S_OUT extends BaseStream<E_OUT, S_OUT>>
        implements BaseRemoteStream<E_OUT, S_OUT>, RemotePipeline<S_OUT>,
                   ExternalizableLite, PortableObject
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Deserialization constructor.
     */
    protected AbstractPipeline()
        {
        }

    /**
     * Constructor for the head of a stream pipeline.
     *
     * @param map             the stream source
     * @param fParallel       true if the pipeline is parallel
     */
    protected AbstractPipeline(InvocableMap<K, V> map, boolean fParallel,
                               Collection<? extends K> colKeys, Filter filter,
                               Function<S_IN, S_OUT> intermediateOp)
        {
        m_invoker        = new AggregatorInvoker<>(map, colKeys, filter);
        m_previousStage  = null;
        m_fParallel      = fParallel;
        m_intermediateOp = intermediateOp;
        }

    /**
     * Constructor for appending an intermediate operation stage onto an
     * existing pipeline.
     *
     * @param previousStage   the upstream pipeline stage
     * @param intermediateOp  intermediate operation for this stage
     */
    protected AbstractPipeline(AbstractPipeline<K, V, ?, E_IN, ?, S_IN> previousStage,
                               Function<S_IN, S_OUT> intermediateOp)
        {
        if (previousStage.m_fLinkedOrConsumed)
            {
            throw new IllegalStateException(MSG_STREAM_LINKED);
            }
        previousStage.m_fLinkedOrConsumed = true;

        m_previousStage = previousStage;
        m_intermediateOp = intermediateOp;
        }

    // ---- Pipeline interface ----------------------------------------------

    public <K, V> S_OUT evaluate(Stream<? extends InvocableMap.Entry<? extends K, ? extends V>> stream)
        {
        AbstractPipeline<?, ?, ?, E_IN, ?, S_IN> previousStage = m_previousStage;
        return previousStage == null
               ? ((Function<Stream<? extends InvocableMap.Entry<? extends K, ? extends V>>, S_OUT>) m_intermediateOp).apply(stream)
               : m_intermediateOp.apply(previousStage.evaluate(stream));
        }

    // ---- BaseStream interface --------------------------------------------

    public void close()
        {
        m_fLinkedOrConsumed = true;
        m_invoker = null;
        if (head().m_sourceCloseAction != null)
            {
            Runnable closeAction = head().m_sourceCloseAction;
            head().m_sourceCloseAction = null;
            closeAction.run();
            }
        }

    @SuppressWarnings("unchecked")
    public S_OUT onClose(Runnable closeHandler)
        {
        Runnable existingHandler = head().m_sourceCloseAction;
        head().m_sourceCloseAction =
                (existingHandler == null)
                ? closeHandler
                : composeWithExceptions(existingHandler, closeHandler);
        return (S_OUT) this;
        }

    public RemotePipeline<S_OUT> pipeline()
        {
        return this;
        }

    public final boolean isParallel()
        {
        return head().m_fParallel;
        }

    // ---- accessors -------------------------------------------------------

    /**
     * Set a flag specifying whether this stream is parallel or sequential.
     *
     * @param fParallel  <c>true</c> if parallel, <c>false</c> if sequential
     */
    protected void setParallel(boolean fParallel)
        {
        head().m_fParallel = fParallel;
        }

    /**
     * Return a comparator that should be used to sort stream elements.
     *
     * @return  a comparator to use
     */
    public Comparator<? super E_OUT> getComparator()
        {
        return m_comparator;
        }

    /**
     * Set a comparator that should be used to sort stream elements.
     *
     * @param comparator  a comparator to use
     */
    protected void setComparator(Comparator<? super E_OUT> comparator)
        {
        m_comparator = comparator;
        }

    /**
     * Return true if this stream has sort order defined.
     *
     * @return  true if this stream has sort order defined
     */
    protected boolean isSorted()
        {
        boolean          fSorted = false;
        AbstractPipeline curr    = this;
        while (curr.m_previousStage != null && !fSorted)
            {
            fSorted = curr.m_comparator != null;
            curr    = curr.m_previousStage;
            }
        return fSorted;
        }

    /**
     * Return the InvocableMap this stream was created from.
     *
     * @return  the InvocableMap this stream was created from
     */
    protected InvocableMap<K, V> getMap()
        {
        return head().m_invoker.getMap();
        }

    /**
     * Determine whether or not the iteration of the underlying map for this
     * stream could be partitioned.
     *
     * @return  true if the iteration can be partitioned, false otherwise
     */
    protected boolean isPartitionable()
        {
        if (getInvoker().getKeys() != null)
            {
            // theoretically we could partition key set-based stream as well
            // using InKeySetFilter, but there is very little benefit in doing
            // so since the key set-based aggregation scales better
            return false;
            }

        InvocableMap<K, V> map = getMap();
        return map instanceof NamedCache &&
               !(map instanceof ContinuousQueryCache && ((ContinuousQueryCache) map).isCacheValues()) &&
               ((NamedCache) map).getCacheService() instanceof PartitionedService;
        }

    /**
     * Invoke the aggregator.
     *
     * @param aggregator  aggregator to invoke
     * @param <R>         the type of aggregation result
     *
     * @return  the aggregation result
     */
    protected <R> R invoke(InvocableMap.EntryAggregator<? super K, ? super V, R> aggregator)
        {
        return head().m_invoker.invoke(aggregator);
        }

    public AggregatorInvoker<K, V> getInvoker()
        {
        return head().m_invoker;
        }

    /**
     * Return the head of the pipeline.
     *
     * @return  the head of the pipeline
     */
    protected AbstractPipeline<K, V, ?, ?, ?, ?> head()
        {
        AbstractPipeline head = this;
        while (head.m_previousStage != null)
            {
            head = head.m_previousStage;
            }
        return head;
        }

    // ---- helpers ---------------------------------------------------------

    /**
     * Return appropriate collection collector based on whether this stream is
     * sorted or not.
     *
     * @return  a collector that collects stream elements into a Collection
     */
    protected RemoteCollector<E_OUT, ?, ? extends Collection<E_OUT>> toCollection()
        {
        return isSorted()
               ? RemoteCollectors.toSortedBag(getComparator())
               : RemoteCollectors.toList();
        }

    /**
     * Return appropriate set collector based on whether this stream is
     * sorted or not.
     *
     * @return  a collector that collects stream elements into a Set
     */
    protected RemoteCollector<E_OUT, ?, ? extends Set<E_OUT>> toSet()
        {
        return isSorted()
               ? RemoteCollectors.toSortedSet(getComparator())
               : RemoteCollectors.toSet();
        }

    /**
     * Given two Runnables, return a Runnable that executes both in sequence,
     * even if the first throws an exception, and if both throw exceptions, add
     * any exceptions thrown by the second as suppressed exceptions of the
     * first.
     *
     * @param a  first runnable to execute
     * @param b  second runnable to execute
     */
    private Runnable composeWithExceptions(Runnable a, Runnable b)
        {
        return new Runnable()
            {
            @Override
            public void run()
                {
                try
                    {
                    a.run();
                    }
                catch (Throwable e1)
                    {
                    try
                        {
                        b.run();
                        }
                    catch (Throwable e2)
                        {
                        try
                            {
                            e1.addSuppressed(e2);
                            }
                        catch (Throwable ignore)
                            {
                            }
                        }
                    throw e1;
                    }
                b.run();
                }
            };
        }

    // ---- ExternalizableLite interface ------------------------------------

    public void readExternal(DataInput in) throws IOException
        {
        m_fParallel      = in.readBoolean();
        m_comparator     = ExternalizableHelper.readObject(in);
        m_previousStage  = ExternalizableHelper.readObject(in);
        m_intermediateOp = ExternalizableHelper.readObject(in);
        }

    public void writeExternal(DataOutput out) throws IOException
        {
        out.writeBoolean(m_fParallel);
        ExternalizableHelper.writeObject(out, m_comparator);
        ExternalizableHelper.writeObject(out, m_previousStage);
        ExternalizableHelper.writeObject(out, m_intermediateOp);
        }

    // ---- PortableObject interface ----------------------------------------

    public void readExternal(PofReader reader) throws IOException
        {
        m_fParallel      = reader.readBoolean(0);
        m_comparator     = reader.readObject(1);
        m_previousStage  = reader.readObject(2);
        m_intermediateOp = reader.readObject(3);
        }

    public void writeExternal(PofWriter writer) throws IOException
        {
        writer.writeBoolean(0, m_fParallel);
        writer.writeObject(1, m_comparator);
        writer.writeObject(2, m_previousStage);
        writer.writeObject(3, m_intermediateOp);
        }

    // ---- inner class: AggregatorInvoker ----------------------------------

    /**
     * A helper class that invokes aggregator on either key set or a filter,
     * depending on which one is specified.
     *
     * @param <K>  the key type
     * @param <V>  the value type
     */
    protected static class AggregatorInvoker<K, V>
        {
        /**
         * Construct AggregatorInvoker instance.
         *
         * @param colKeys  the key set to aggregate on
         * @param filter   the filter to aggregate on
         */
        public AggregatorInvoker(InvocableMap<K, V> map, Collection<? extends K> colKeys, Filter filter)
            {
            m_map     = map;
            m_colKeys = colKeys;
            m_filter  = filter;
            }

        /**
         * Invoke specified aggregator and return the result.
         *
         * @param aggregator  the aggregator to invoke
         * @param <R>         the type of the result
         *
         * @return the aggregation result
         */
        public <R> R invoke(InvocableMap.EntryAggregator<? super K, ? super V, R> aggregator)
            {
            return m_colKeys == null
                   ? getMap().aggregate(m_filter, aggregator)
                   : getMap().aggregate(m_colKeys, aggregator);
            }

        /**
         * Return the InvocableMap this invoker is for.
         *
         * @return  the InvocableMap this invoker is for
         */
        public InvocableMap<K, V> getMap()
            {
            InvocableMap<K, V> map = m_map;
            if (map == null)
                {
                throw new IllegalStateException("Cannot invoke terminal operation on a pipeline builder");
                }

            return map;
            }

        public Collection<? extends K> getKeys()
            {
            return m_colKeys;
            }

        public Filter getFilter()
            {
            return m_filter;
            }

        // ---- data members ------------------------------------------------

        /**
         * The InvocableMap to aggregate.
         */
        private InvocableMap<K, V> m_map;

        /**
         * The key set to aggregate on.
         */
        private Collection<? extends K> m_colKeys;

        /**
         * The filter to aggregate on.
         */
        private Filter m_filter;
        }

    // ---- data members ----------------------------------------------------

    private static final String MSG_STREAM_LINKED = "stream has already been operated upon or closed";

    /**
     * The aggregator invoker.
     */
    private transient AggregatorInvoker<K, V> m_invoker;

    /**
     * True if this pipeline has been linked or consumed
     */
    private transient boolean m_fLinkedOrConsumed;

    /**
     * An action to run when the source is closed
     */
    private transient Runnable m_sourceCloseAction;

    /**
     * True if pipeline is parallel, otherwise the pipeline is sequential; only
     * valid for the source stage.
     */
    @JsonbProperty("isParallel")
    private boolean m_fParallel;

    /**
     * A comparator to use if the pipeline is sorted.
     */
    @JsonbProperty("comparator")
    private Comparator<? super E_OUT> m_comparator;

    /**
     * The "upstream" pipeline, or null if this is the source stage.
     */
    @JsonbProperty("previousStage")
    private AbstractPipeline<K, V, ?, E_IN, ?, S_IN> m_previousStage;

    /**
     * Intermediate operation performed by this pipeline stage.
     */
    @JsonbProperty("intermediateOp")
    private Function<S_IN, S_OUT> m_intermediateOp;
    }