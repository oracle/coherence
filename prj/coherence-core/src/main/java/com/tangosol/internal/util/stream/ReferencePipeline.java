/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.stream;

import com.tangosol.internal.util.stream.collectors.BiReducingCollector;
import com.tangosol.internal.util.stream.collectors.MappingCollector;
import com.tangosol.internal.util.stream.collectors.ReducingCollector;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.net.NamedCache;
import com.tangosol.net.PartitionedService;

import com.tangosol.net.partition.AbstractPartitionedIterator;
import com.tangosol.net.partition.PartitionSet;

import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.SimpleHolder;
import com.tangosol.util.Streamer;

import com.tangosol.util.comparator.SafeComparator;

import com.tangosol.util.filter.PartitionedFilter;

import com.tangosol.util.function.Remote;

import com.tangosol.util.stream.RemoteCollector;
import com.tangosol.util.stream.RemoteCollectors;
import com.tangosol.util.stream.RemotePipeline;
import com.tangosol.util.stream.RemoteDoubleStream;
import com.tangosol.util.stream.RemoteIntStream;
import com.tangosol.util.stream.RemoteLongStream;
import com.tangosol.util.stream.RemoteStream;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

import java.util.stream.BaseStream;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import javax.json.bind.annotation.JsonbProperty;

/**
 * Abstract base class for an intermediate pipeline stage or pipeline source
 * stage implementing whose elements are of type {@code U}.
 *
 * @param <K>      key type
 * @param <V>      value type
 * @param <P_IN>   type of elements in the upstream source
 * @param <P_OUT>  type of elements produced by this stage
 * @param <S_IN>   the type of input stream
 *
 * @author as  2014.08.26
 * @since 12.2.1
 */
@SuppressWarnings("Convert2MethodRef")
public abstract class ReferencePipeline<K, V, P_IN, P_OUT, S_IN extends BaseStream<P_IN,S_IN>>
        extends AbstractPipeline<K, V, P_IN, P_OUT, S_IN, Stream<P_OUT>>
        implements RemoteStream<P_OUT>
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Deserialization constructor.
     */
    protected ReferencePipeline()
        {
        }

    /**
     * Constructor for the head of a stream pipeline.
     *
     * @param map             the stream source
     * @param fParallel       true if the pipeline is parallel
     */
    protected ReferencePipeline(InvocableMap<K, V> map, boolean fParallel,
                             Collection<? extends K> colKeys, Filter filter,
                             Function<S_IN, Stream<P_OUT>> intermediateOp)
        {
        super(map, fParallel, colKeys, filter, intermediateOp);
        }

    /**
     * Construct intermediate stage of the pipeline.
     *
     * @param previousStage   previous stage in the pipeline
     * @param intermediateOp  Intermediate operation performed by this stage
     */
    protected ReferencePipeline(AbstractPipeline<K, V, ?, P_IN, ?, S_IN> previousStage,
                                Function<S_IN, Stream<P_OUT>> intermediateOp)
        {
        super(previousStage, intermediateOp);
        }

    // ---- intermediate operations -----------------------------------------

    public RemoteStream<P_OUT> sequential()
        {
        setParallel(false);
        return this;
        }

    public RemoteStream<P_OUT> parallel()
        {
        setParallel(true);
        return this;
        }

    public RemoteStream<P_OUT> unordered()
        {
        return new StatelessOp<>(this, (s) -> s.unordered());
        }

    public RemoteStream<P_OUT> filter(Predicate<? super P_OUT> filter)
        {
        return new StatelessOp<>(this, (s) -> s.filter(filter));
        }

    public <R> RemoteStream<R> map(Function<? super P_OUT, ? extends R> mapper)
        {
        return new StatelessOp<>(this, (s) -> s.map(mapper));
        }

    public RemoteIntStream mapToInt(ToIntFunction<? super P_OUT> mapper)
        {
        return new IntPipeline.StatelessOp<>(this, (s) -> s.mapToInt(mapper));
        }

    public RemoteLongStream mapToLong(ToLongFunction<? super P_OUT> mapper)
        {
        return new LongPipeline.StatelessOp<>(this, s -> s.mapToLong(mapper));
        }

    public RemoteDoubleStream mapToDouble(ToDoubleFunction<? super P_OUT> mapper)
        {
        return new DoublePipeline.StatelessOp<>(this, s -> s.mapToDouble(mapper));
        }

    public <R> RemoteStream<R> flatMap(Function<? super P_OUT, ? extends Stream<? extends R>> mapper)
        {
        return new StatelessOp<>(this, (s) -> s.flatMap(mapper));
        }

    public RemoteIntStream flatMapToInt(Function<? super P_OUT, ? extends IntStream> mapper)
        {
        return new IntPipeline.StatelessOp<>(this, (s) -> s.flatMapToInt(mapper));
        }

    public RemoteLongStream flatMapToLong(Function<? super P_OUT, ? extends LongStream> mapper)
        {
        return new LongPipeline.StatelessOp<>(this, s -> s.flatMapToLong(mapper));
        }

    public RemoteDoubleStream flatMapToDouble(Function<? super P_OUT, ? extends DoubleStream> mapper)
        {
        return new DoublePipeline.StatelessOp<>(this, s -> s.flatMapToDouble(mapper));
        }

    public RemoteStream<P_OUT> peek(Consumer<? super P_OUT> action)
        {
        return new StatelessOp<>(this, (s) -> s.peek(action));
        }

    public Stream<P_OUT> limit(long maxSize)
        {
        return collect(new StatefulOp<>(this, (s) -> s.limit(maxSize)),
                       toCollection()).stream().limit(maxSize);
        }

    public Stream<P_OUT> skip(long n)
        {
        return collect(this, toCollection()).stream().skip(n);
        }

    public Stream<P_OUT> distinct()
        {
        return collect(this, toSet()).stream();
        }

    public RemoteStream<P_OUT> sorted()
        {
        SafeComparator<? super P_OUT> comp = SafeComparator.INSTANCE;
        StatefulOp<K, V, P_OUT, P_OUT, Stream<P_OUT>> op =
                new StatefulOp<>(this, (s) -> s.sorted(comp));
        op.setComparator(comp);
        return op;
        }

    public RemoteStream<P_OUT> sorted(Comparator<? super P_OUT> comparator)
        {
        SafeComparator<? super P_OUT> comp = new SafeComparator<>(comparator);
        StatefulOp<K, V, P_OUT, P_OUT, Stream<P_OUT>> op =
                new StatefulOp<>(this, (s) -> s.sorted(comp));
        op.setComparator(comp);
        return op;
        }

    // ---- terminal operations ---------------------------------------------

    public Iterator<P_OUT> iterator()
        {
        if (isSorted() || !isPartitionable())
            {
            return collect(toCollection()).iterator();
            }

        return createPartitionedIterator((filter) -> getMap().aggregate(filter, new CollectorAggregator<>(this, toCollection())), false);
        }

    public Spliterator<P_OUT> spliterator()
        {
        if (isSorted() || !isPartitionable())
            {
            return collect(toCollection()).spliterator();
            }

        return new PartitionedSpliterator<>(createPartitionedIterator((filter) -> getMap().aggregate(filter, new CollectorAggregator<>(this, toCollection())), false));
        }

    public void forEach(Consumer<? super P_OUT> action)
        {
        iterator().forEachRemaining(action);
        }

    public void forEachOrdered(Consumer<? super P_OUT> action)
        {
        forEach(action);
        }

    public Object[] toArray()
        {
        return collect(toCollection()).toArray();
        }

    @SuppressWarnings("SuspiciousToArrayCall")
    public <A> A[] toArray(IntFunction<A[]> generator)
        {
        Collection<P_OUT> col = collect(toCollection());
        return col.toArray(generator.apply(col.size()));
        }

    public P_OUT reduce(P_OUT identity, BinaryOperator<P_OUT> accumulator)
        {
        return collect(reducing(identity, accumulator));
        }

    public Optional<P_OUT> reduce(BinaryOperator<P_OUT> accumulator)
        {
        return collect(reducing(accumulator));
        }

    public <U> U reduce(U identity, BiFunction<U, ? super P_OUT, U> accumulator, BinaryOperator<U> combiner)
        {
        return collect(reducing(identity, accumulator, combiner));
        }

    public <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super P_OUT> accumulator, BiConsumer<R, R> combiner)
        {
        Remote.BinaryOperator<R> operator = (left, right) ->
            {
            combiner.accept(left, right);
            return left;
            };

        return collect(RemoteCollector.of(supplier, accumulator, operator));
        }

    public <R, A> R collect(RemoteCollector<? super P_OUT, A, R> collector)
        {
        return collect(this, collector);
        }

    public Optional<P_OUT> min(Comparator<? super P_OUT> comparator)
        {
        return collect(minBy(comparator));
        }

    public Optional<P_OUT> max(Comparator<? super P_OUT> comparator)
        {
        return collect(maxBy(comparator));
        }

    public long count()
        {
        return collect(RemoteCollectors.counting());
        }

    public boolean anyMatch(Predicate<? super P_OUT> predicate)
        {
        return invoke(new MatcherAggregator<>(this, (s) -> s.anyMatch(predicate), (p) -> p));
        }

    public boolean allMatch(Predicate<? super P_OUT> predicate)
        {
        return invoke(new MatcherAggregator<>(this, (s) -> s.allMatch(predicate), (p) -> !p));
        }

    public boolean noneMatch(Predicate<? super P_OUT> predicate)
        {
        return invoke(new MatcherAggregator<>(this, (s) -> s.noneMatch(predicate), (p) -> !p));
        }

    public Optional<P_OUT> findFirst()
        {
        return invoke(new FinderAggregator<>(this, Stream::findFirst));
        }

    public Optional<P_OUT> findAny()
        {
        return invoke(new FinderAggregator<>(this, Stream::findAny));
        }

    // ---- helper methods --------------------------------------------------

    protected <R, A> R collect(RemotePipeline<? extends Stream<P_OUT>> pipeline, RemoteCollector<? super P_OUT, A, R> collector)
        {
        return invoke(new CollectorAggregator<>(pipeline, collector));
        }

    protected static <T, U, A, R> RemoteCollector<T, A, R> mapping(Function<? super T, ? extends U> mapper, RemoteCollector<? super U, A, R> downstream)
        {
        return new MappingCollector<>(mapper, downstream);
        }

    protected static <T> RemoteCollector<T, ?, Optional<T>> minBy(Comparator<? super T> comparator)
        {
        return reducing(Remote.BinaryOperator.minBy(comparator));
        }

    protected static <T> RemoteCollector<T, ?, Optional<T>> maxBy(Comparator<? super T> comparator)
        {
        return reducing(Remote.BinaryOperator.maxBy(comparator));
        }

    protected static <T> RemoteCollector<T, ?, Optional<T>> reducing(BinaryOperator<T> op)
        {
        return RemoteCollectors.collectingAndThen(reducing(null, op), Optional::ofNullable);
        }

    protected static <T> RemoteCollector<T, ?, T> reducing(T identity, BinaryOperator<T> op)
        {
        return new ReducingCollector<>(identity, op);
        }

    protected static <T, U> RemoteCollector<T, SimpleHolder<U>, U> reducing(U identity,
                                    BiFunction<? super U, ? super T, ? extends U> mapper,
                                    BinaryOperator<U> op)
        {
        return new BiReducingCollector<>(identity, mapper, op);
        }

    protected PartitionedIterator<P_OUT> createPartitionedIterator(Function<PartitionedFilter, Iterable<P_OUT>> supplier, boolean fByMember)
        {
        NamedCache         cache   = (NamedCache) getMap();
        PartitionedService service = (PartitionedService) cache.getCacheService();
        int                cParts  = service.getPartitionCount();
        PartitionSet       parts   = new PartitionSet(cParts).fill();

        return new PartitionedIterator<>(getInvoker().getFilter(), cache, parts, supplier, fByMember);
        }

    // ---- pipeline stage implementations ----------------------------------

    /**
     * A stage in a pipeline representing beginning of the pipeline.
     *
     * @param <P_IN>  the type of input elements
     * @param <P_OUT> the type of output elements
     * @param <S_IN>  the type of input stream
     */
    public static class Head<K, V, P_IN, P_OUT, S_IN extends BaseStream<P_IN, S_IN>>
            extends ReferencePipeline<K, V, P_IN, P_OUT, S_IN>
        {
        /**
         * Deserialization constructor.
         */
        public Head()
            {
            }

        /**
         * Construct Head instance.
         *
         * @param map             the parent map this stream was created from
         * @param parallel        a flag specifying whether this stream should be
         *                        evaluated in parallel
         * @param filter          a filter that should be used to narrow down
         *                        parent map contents before stream evaluation
         * @param intermediateOp  intermediate operation for this stage
         */
        protected Head(InvocableMap<K, V> map, boolean parallel,
                    Collection<? extends K> colKeys, Filter filter,
                    Remote.Function<S_IN, Stream<P_OUT>> intermediateOp)
            {
            super(map, parallel, colKeys, filter, intermediateOp);
            }
        }

    /**
     * A stage in a pipeline representing stateless operation.
     *
     * @param <P_IN>  the type of input elements
     * @param <P_OUT> the type of output elements
     * @param <S_IN>  the type of input stream
     */
    public static class StatelessOp<K, V, P_IN, P_OUT, S_IN extends BaseStream<P_IN, S_IN>>
            extends ReferencePipeline<K, V, P_IN, P_OUT, S_IN>
        {
        /**
         * Deserialization constructor.
         */
        public StatelessOp()
            {
            }

        /**
         * Construct StatelessOp instance.
         *
         * @param previousStage   the upstream pipeline stage
         * @param intermediateOp  intermediate operation for this stage
         */
        StatelessOp(AbstractPipeline<K, V, ?, P_IN, ?, S_IN> previousStage, Remote.Function<S_IN, Stream<P_OUT>> intermediateOp)
            {
            super(previousStage, intermediateOp);
            }
        }

    /**
     * A stage in a pipeline representing stateful operation.
     *
     * @param <P_IN>  the type of input elements
     * @param <P_OUT> the type of output elements
     * @param <S_IN>  the type of input stream
     */
    public static class StatefulOp<K, V, P_IN, P_OUT, S_IN extends BaseStream<P_IN, S_IN>>
            extends ReferencePipeline<K, V, P_IN, P_OUT, S_IN>
        {
        /**
         * Deserialization constructor.
         */
        public StatefulOp()
            {
            }

        /**
         * Construct StatefulOp instance.
         *
         * @param previousStage   the upstream pipeline stage
         * @param intermediateOp  intermediate operation for this stage
         */
        StatefulOp(AbstractPipeline<K, V, ?, P_IN, ?, S_IN> previousStage, Remote.Function<S_IN, Stream<P_OUT>> intermediateOp)
            {
            super(previousStage, intermediateOp);
            }
        }

    // ---- inner class: PartitionedIterator --------------------------------

    protected static class PartitionedIterator<T>
            extends AbstractPartitionedIterator<T>
        {
        public PartitionedIterator(Filter filter, NamedCache cache, PartitionSet setPids,
                                   Function<PartitionedFilter, Iterable<T>> supplier, boolean fByMember)
            {
            super(filter, cache, setPids, fByMember, false);

            Objects.requireNonNull(supplier);
            m_supplier = supplier;
            }

        public PartitionedIterator(PartitionedIterator<T> that, PartitionSet setPids)
            {
            super(that.m_filter, that.m_cache, setPids, that.m_fByMember, that.m_fRandom);

            m_supplier = that.m_supplier;
            }

        protected Iterable<T> nextIterable(PartitionedFilter filter)
            {
            return m_supplier.apply(filter);
            }

        protected PartitionSet getPartitionSet()
            {
            return m_setPids;
            }

        private Function<PartitionedFilter, Iterable<T>> m_supplier;
        }

    // ---- inner class: PartitionedSpliterator -----------------------------

    protected static class PartitionedSpliterator<T>
            implements Spliterator<T>
        {
        public PartitionedSpliterator(PartitionedIterator<T> iterator)
            {
            this(iterator, 0);
            }

        public PartitionedSpliterator(PartitionedIterator<T> iterator, int nCharacteristics)
            {
            m_iterator         = iterator;
            m_nCharacteristics = nCharacteristics;
            }

        @Override
        public void forEachRemaining(Consumer<? super T> action)
            {
            Objects.requireNonNull(action);
            m_iterator.forEachRemaining(action);
            }

        @Override
        public boolean tryAdvance(Consumer<? super T> action)
            {
            Objects.requireNonNull(action);

            Iterator<T> it = m_iterator;
            if (it.hasNext())
                {
                action.accept(it.next());
                return true;
                }

            return false;
            }

        public Spliterator<T> trySplit()
            {
            PartitionSet parts = m_iterator.getPartitionSet().split();
            if (parts == null)
                {
                return null;
                }

            PartitionedIterator<T> iterator = new PartitionedIterator<>(m_iterator, parts);
            return new PartitionedSpliterator<>(iterator, m_nCharacteristics);
            }

        public long estimateSize()
            {
            return Long.MAX_VALUE;
            }

        public int characteristics()
            {
            return Spliterator.CONCURRENT | m_nCharacteristics;
            }

        private PartitionedIterator<T> m_iterator;
        private int m_nCharacteristics;
        }

    // ---- inner class: MatcherAggregator ----------------------------------

    /**
     * Aggregator used by matching terminal operations ({@link RemoteStream#anyMatch},
     * {@link RemoteStream#allMatch} and {@link RemoteStream#noneMatch}).
     *
     * @param <K> the type of the Map entry keys
     * @param <V> the type of the Map entry values
     * @param <T> the type of stream elements to match
     */
    public static class MatcherAggregator<K, V, T>
            implements InvocableMap.StreamingAggregator<K, V, Boolean, Boolean>,
                       ExternalizableLite, PortableObject
        {
        // ---- constructors ------------------------------------------------

        /**
         * Deserialization constructor.
         */
        public MatcherAggregator()
            {
            }

        /**
         * Construct MatcherAggregator instance.
         *
         * @param pipeline      pipeline of intermediate operations to evaluate
         * @param fnMatcher     matching operation to invoke on the stream
         * @param predShortCircuit  predicate that determines if the aggregation
         *                      should be short-circuited
         */
        MatcherAggregator(RemotePipeline<? extends Stream<T>> pipeline,
                          Remote.Function<Stream<T>, Boolean> fnMatcher,
                          Remote.Predicate<Boolean> predShortCircuit)
            {
            m_pipeline         = pipeline;
            m_fnMatcher        = fnMatcher;
            m_predShortCircuit = predShortCircuit;
            }

        // ---- InvocableMap.StreamingAggregator interface ------------------

        @Override
        public InvocableMap.StreamingAggregator<K, V, Boolean, Boolean> supply()
            {
            return new MatcherAggregator<>(m_pipeline, m_fnMatcher, m_predShortCircuit);
            }

        @Override
        public boolean accumulate(Streamer<? extends InvocableMap.Entry<? extends K, ? extends V>> streamer)
            {
            Stream<T> stream = m_pipeline.evaluate(streamer.stream());
            m_fResult = m_fnMatcher.apply(stream);
            return !m_predShortCircuit.test(m_fResult);
            }

        @Override
        public boolean accumulate(InvocableMap.Entry<? extends K, ? extends V> entry)
            {
            throw new UnsupportedOperationException();
            }

        @Override
        public boolean combine(Boolean partialResult)
            {
            if (!m_fDone)
                {
                m_fResult = partialResult;
                m_fDone   = m_predShortCircuit.test(m_fResult);
                }
            return !m_fDone;
            }

        @Override
        public Boolean getPartialResult()
            {
            return m_fResult;
            }

        @Override
        public Boolean finalizeResult()
            {
            return m_fResult;
            }

        @Override
        public int characteristics()
            {
            return SERIAL;
            }

        // ---- ExternalizableLite interface --------------------------------

        @Override
        public void readExternal(DataInput in) throws IOException
            {
            m_pipeline         = ExternalizableHelper.readObject(in);
            m_fnMatcher        = ExternalizableHelper.readObject(in);
            m_predShortCircuit = ExternalizableHelper.readObject(in);
            }

        @Override
        public void writeExternal(DataOutput out) throws IOException
            {
            ExternalizableHelper.writeObject(out, m_pipeline);
            ExternalizableHelper.writeObject(out, m_fnMatcher);
            ExternalizableHelper.writeObject(out, m_predShortCircuit);
            }

        // ---- PortableObject interface ------------------------------------

        public void readExternal(PofReader reader) throws IOException
            {
            m_pipeline         = reader.readObject(0);
            m_fnMatcher        = reader.readObject(1);
            m_predShortCircuit = reader.readObject(2);
            }

        public void writeExternal(PofWriter writer) throws IOException
            {
            writer.writeObject(0, m_pipeline);
            writer.writeObject(1, m_fnMatcher);
            writer.writeObject(2, m_predShortCircuit);
            }

        // ---- data members ------------------------------------------------

        /**
         * Pipeline of intermediate operations to evaluate.
         */
        @JsonbProperty("pipeline")
        private RemotePipeline<? extends Stream<T>> m_pipeline;

        /**
         * Matching operation to invoke on the stream
         */
        @JsonbProperty("fnMatcher")
        private Remote.Function<Stream<T>, Boolean> m_fnMatcher;

        /**
         * Predicate that determines if the aggregation should be short-circuited.
         */
        @JsonbProperty("predShortCircuit")
        private Remote.Predicate<Boolean> m_predShortCircuit;

        /**
         * The aggregation result.
         */
        private transient Boolean m_fResult;

        /**
         * If true, indicates that no more accumulation is necessary.
         */
        private transient boolean m_fDone;
        }

    // ---- inner class: FinderAggregator -----------------------------------

    /**
     * Aggregator used by finder terminal operations ({@link RemoteStream#findFirst()}
     * and {@link RemoteStream#findAny()}).
     *
     * @param <K> the type of the Map entry keys
     * @param <V> the type of the Map entry values
     * @param <T> the type of stream elements to find
     */
    public static class FinderAggregator<K, V, T>
            implements InvocableMap.StreamingAggregator<K, V, Optional<T>, Optional<T>>,
                       ExternalizableLite, PortableObject
        {
        // ---- constructors ------------------------------------------------

        /**
         * Deserialization constructor.
         */
        public FinderAggregator()
            {
            }

        /**
         * Construct FinderAggregator instance.
         *
         * @param pipeline  pipeline of intermediate operations to evaluate
         * @param fnFinder  find operation to invoke on the stream
         */
        FinderAggregator(RemotePipeline<? extends Stream<T>> pipeline,
                         Remote.Function<Stream<T>, Optional<T>> fnFinder)
            {
            m_pipeline = pipeline;
            m_fnFinder = fnFinder;
            }

        // ---- InvocableMap.StreamingAggregator interface ------------------

        @Override
        public InvocableMap.StreamingAggregator<K, V, Optional<T>, Optional<T>> supply()
            {
            return new FinderAggregator<>(m_pipeline, m_fnFinder);
            }

        @Override
        public boolean accumulate(Streamer<? extends InvocableMap.Entry<? extends K, ? extends V>> streamer)
            {
            Stream<T> stream = m_pipeline.evaluate(streamer.stream());
            m_result = m_fnFinder.apply(stream);
            return !m_result.isPresent();
            }

        @Override
        public boolean accumulate(InvocableMap.Entry<? extends K, ? extends V> entry)
            {
            throw new UnsupportedOperationException();
            }

        @Override
        public boolean combine(Optional<T> partialResult)
            {
            if (!m_fDone)
                {
                m_result = partialResult;
                m_fDone  = m_result.isPresent();
                }
            return !m_fDone;
            }

        @Override
        public Optional<T> getPartialResult()
            {
            return m_result;
            }

        @Override
        public Optional<T> finalizeResult()
            {
            return m_result;
            }

        @Override
        public int characteristics()
            {
            return SERIAL;
            }

        // ---- ExternalizableLite interface --------------------------------

        @Override
        public void readExternal(DataInput in) throws IOException
            {
            m_pipeline = ExternalizableHelper.readObject(in);
            m_fnFinder = ExternalizableHelper.readObject(in);
            }

        @Override
        public void writeExternal(DataOutput out) throws IOException
            {
            ExternalizableHelper.writeObject(out, m_pipeline);
            ExternalizableHelper.writeObject(out, m_fnFinder);
            }

        // ---- PortableObject interface ------------------------------------

        public void readExternal(PofReader reader) throws IOException
            {
            m_pipeline = reader.readObject(0);
            m_fnFinder = reader.readObject(1);
            }

        public void writeExternal(PofWriter writer) throws IOException
            {
            writer.writeObject(0, m_pipeline);
            writer.writeObject(1, m_fnFinder);
            }

        // ---- data members ------------------------------------------------

        /**
         * Pipeline of intermediate operations to evaluate.
         */
        @JsonbProperty("pipeline")
        private RemotePipeline<? extends Stream<T>> m_pipeline;

        /**
         * Find operation to invoke on the stream
         */
        @JsonbProperty("fnFinder")
        private Remote.Function<Stream<T>, Optional<T>> m_fnFinder;

        /**
         * The aggregation result.
         */
        private transient Optional<T> m_result = Optional.empty();

        /**
         * If true, indicates that no more accumulation is necessary.
         */
        private transient boolean m_fDone;
        }
    }