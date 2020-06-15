/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.stream;

import com.tangosol.internal.util.IntBag;
import com.tangosol.internal.util.IntSummaryStatistics;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.ExternalizableHelper;
import com.tangosol.util.InvocableMap;

import com.tangosol.util.Streamer;
import com.tangosol.util.function.Remote;

import com.tangosol.util.stream.RemotePipeline;
import com.tangosol.util.stream.RemoteDoubleStream;
import com.tangosol.util.stream.RemoteIntStream;
import com.tangosol.util.stream.RemoteLongStream;
import com.tangosol.util.stream.RemoteStream;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.Arrays;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.Spliterators;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.ObjIntConsumer;
import java.util.function.Supplier;

import java.util.stream.BaseStream;
import java.util.stream.IntStream;

import javax.json.bind.annotation.JsonbProperty;

/**
 * Abstract base class for an intermediate pipeline stage or pipeline source
 * stage implementing whose elements are of type {@code int}.
 *
 * @param <E_IN> type of elements in the upstream source
 *
 * @author as  2014.09.19
 * @since 12.2.1
 */
@SuppressWarnings("Convert2MethodRef")
public abstract class IntPipeline<K, V, E_IN, S_IN extends BaseStream<E_IN, S_IN>>
        extends AbstractPipeline<K, V, E_IN, Integer, S_IN, IntStream>
        implements RemoteIntStream
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Deserialization constructor.
     */
    public IntPipeline()
        {
        }

    /**
     * Construct intermediate stage of the pipeline.
     *
     * @param previousStage   previous stage in the pipeline
     * @param intermediateOp  Intermediate operation performed by this stage
     */
    protected IntPipeline(AbstractPipeline<K, V, ?, E_IN, ?, S_IN> previousStage, Function<S_IN, IntStream> intermediateOp)
        {
        super(previousStage, intermediateOp);
        }

    // ---- intermediate operations -----------------------------------------

    public RemoteIntStream sequential()
        {
        setParallel(false);
        return this;
        }

    public RemoteIntStream parallel()
        {
        setParallel(true);
        return this;
        }

    public RemoteIntStream unordered()
        {
        return new StatelessOp<>(this, s -> s.unordered());
        }

    public RemoteIntStream filter(IntPredicate predicate)
        {
        return new StatelessOp<>(this, s -> s.filter(predicate));
        }

    public RemoteIntStream map(IntUnaryOperator mapper)
        {
        return new StatelessOp<>(this, s -> s.map(mapper));
        }

    public <U> RemoteStream<U> mapToObj(IntFunction<? extends U> mapper)
        {
        return new ReferencePipeline.StatelessOp<>(this, s -> s.mapToObj(mapper));
        }

    public RemoteLongStream mapToLong(IntToLongFunction mapper)
        {
        return new LongPipeline.StatelessOp<>(this, s -> s.mapToLong(mapper));
        }

    public RemoteDoubleStream mapToDouble(IntToDoubleFunction mapper)
        {
        return new DoublePipeline.StatelessOp<>(this, s -> s.mapToDouble(mapper));
        }

    public RemoteIntStream flatMap(IntFunction<? extends IntStream> mapper)
        {
        return new StatelessOp<>(this, (s) -> s.flatMap(mapper));
        }

    public RemoteIntStream peek(IntConsumer action)
        {
        return new StatelessOp<>(this, (s) -> s.peek(action));
        }

    public IntStream limit(long maxSize)
        {
        StatefulOp<K, V, Integer, IntStream> op =
                new StatefulOp<>(this, (s) -> s.limit(maxSize));
        return Arrays.stream(collectToBag(op).toArray()).limit(maxSize);
        }

    public IntStream skip(long n)
        {
        return Arrays.stream(collectToBag(this).toArray()).skip(n);
        }

    public IntStream distinct()
        {
        StatefulOp<K, V, Integer, IntStream> op =
                new StatefulOp<>(this, (s) -> s.distinct());
        return Arrays.stream(collectToBag(op).toArray());
        }

    public IntStream sorted()
        {
        StatefulOp<K, V, Integer, IntStream> op =
                new StatefulOp<>(this, (s) -> s.sorted());
        return Arrays.stream(collectToBag(op).toArray()).sorted();
        }

    public RemoteLongStream asLongStream()
        {
        return new LongPipeline.StatelessOp<>(this, s -> s.asLongStream());
        }

    public RemoteDoubleStream asDoubleStream()
        {
        return new DoublePipeline.StatelessOp<>(this, s -> s.asDoubleStream());
        }

    public RemoteStream<Integer> boxed()
        {
        return new ReferencePipeline.StatelessOp<>(this, (s) -> s.boxed());
        }

    // ---- terminal operations ---------------------------------------------

    public void forEach(IntConsumer action)
        {
        collectToBag(this).forEach(action);
        }

    public void forEachOrdered(IntConsumer action)
        {
        forEach(action);
        }

    public int[] toArray()
        {
        return collectToBag(this).toArray();
        }

    public int reduce(int identity, IntBinaryOperator op)
        {
        return collect(() -> new int[] {identity},
                       (a, t) -> a[0] = op.applyAsInt(a[0], t),
                       (a, b) -> a[0] = op.applyAsInt(a[0], b[0]))[0];
        }

    public OptionalInt reduce(IntBinaryOperator op)
        {
        Optional result = collect(() -> new Optional(op),
                                  Optional::accept,
                                  (a, b) ->
                                      {
                                      if (b.isPresent())
                                          {
                                          a.accept(b.getValue());
                                          }
                                      });
        return result.isPresent()
               ? OptionalInt.of(result.getValue()) : OptionalInt.empty();
        }

    public <R> R collect(Supplier<R> supplier, ObjIntConsumer<R> accumulator, BiConsumer<R, R> combiner)
        {
        return collect(this, supplier, accumulator, combiner);
        }

    public int sum()
        {
        return reduce(0, Integer::sum);
        }

    public OptionalInt min()
        {
        return reduce(Math::min);
        }

    public OptionalInt max()
        {
        return reduce(Math::max);
        }

    public long count()
        {
        return mapToLong(e -> 1L).sum();
        }

    public OptionalDouble average()
        {
        long[] avg = collect(() -> new long[2],
                             (a, t) ->
                                 {
                                 a[0]++;
                                 a[1] += t;
                                 },
                             (a, b) ->
                                 {
                                 a[0] += b[0];
                                 a[1] += b[1];
                                 });
        return avg[0] > 0
               ? OptionalDouble.of((double) avg[1] / avg[0])
               : OptionalDouble.empty();
        }

    public IntSummaryStatistics summaryStatistics()
        {
        return collect(IntSummaryStatistics::new,
                       IntSummaryStatistics::accept,
                       IntSummaryStatistics::combine);
        }

    public boolean anyMatch(IntPredicate predicate)
        {
        return invoke(new MatcherAggregator<>(this, (s) -> s.anyMatch(predicate), (p) -> p));
        }

    public boolean allMatch(IntPredicate predicate)
        {
        return invoke(new MatcherAggregator<>(this, (s) -> s.allMatch(predicate), (p) -> !p));
        }

    public boolean noneMatch(IntPredicate predicate)
        {
        return invoke(new MatcherAggregator<>(this, (s) -> s.noneMatch(predicate), (p) -> !p));
        }

    public OptionalInt findFirst()
        {
        return invoke(new FinderAggregator<>(this, IntStream::findFirst));
        }

    public OptionalInt findAny()
        {
        return invoke(new FinderAggregator<>(this, IntStream::findAny));
        }

    // ---- AbstractPipeline overrides --------------------------------------

    @Override
    public final PrimitiveIterator.OfInt iterator() {
        return Spliterators.iterator(spliterator());
    }

    @Override
    public final Spliterator.OfInt spliterator() {
        return Spliterators.spliterator(toArray(), 0);
    }

    // ---- helpers ---------------------------------------------------------

    protected IntBag collectToBag(RemotePipeline<IntStream> pipeline)
        {
        return collect(pipeline, IntBag::new, IntBag::add, IntBag::addAll);
        }

    protected <R> R collect(RemotePipeline<IntStream> pipeline, Remote.Supplier<R> supplier, Remote.ObjIntConsumer<R> accumulator, Remote.BiConsumer<R, R> combiner)
        {
        return invoke(new IntCollectorAggregator<>(pipeline, supplier, accumulator, combiner));
        }

    protected <R> R collect(RemotePipeline<IntStream> pipeline, Supplier<R> supplier, ObjIntConsumer<R> accumulator, BiConsumer<R, R> combiner)
        {
        return invoke(new IntCollectorAggregator<>(pipeline, supplier, accumulator, combiner));
        }

    // ---- pipeline stage implementations ----------------------------------

    /**
     * A stage in a pipeline representing stateless operation.
     *
     * @param <E_IN>  the type of input elements
     * @param <S_IN>  the type of input stream
     */
    public static class StatelessOp<K, V, E_IN, S_IN extends BaseStream<E_IN, S_IN>>
            extends IntPipeline<K, V, E_IN, S_IN>
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
        StatelessOp(AbstractPipeline<K, V, ?, E_IN, ?, S_IN> previousStage, Remote.Function<S_IN, IntStream> intermediateOp)
            {
            super(previousStage, intermediateOp);
            }
        }

    /**
     * A stage in a pipeline representing stateful operation.
     *
     * @param <E_IN>  the type of input elements
     * @param <S_IN>  the type of input stream
     */
    public static class StatefulOp<K, V, E_IN, S_IN extends BaseStream<E_IN, S_IN>>
            extends IntPipeline<K, V, E_IN, S_IN>
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
        StatefulOp(AbstractPipeline<K, V, ?, E_IN, ?, S_IN> previousStage,
                   Remote.Function<S_IN, IntStream> intermediateOp)
            {
            super(previousStage, intermediateOp);
            }
        }

    // ---- inner class: MatcherAggregator ----------------------------------

    /**
     * Aggregator used by matching terminal operations ({@link RemoteIntStream#anyMatch},
     * {@link RemoteIntStream#allMatch} and {@link RemoteIntStream#noneMatch}).
     *
     * @param <K> the type of the Map entry keys
     * @param <V> the type of the Map entry values
     */
    public static class MatcherAggregator<K, V>
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
         * @param pipeline          pipeline of intermediate operations to evaluate
         * @param fnMatcher         matching operation to invoke on the stream
         * @param predShortCircuit  predicate that determines if the aggregation
         *                          should be short-circuited
         */
        MatcherAggregator(RemotePipeline<? extends IntStream> pipeline,
                          Remote.Function<IntStream, Boolean> fnMatcher,
                          Remote.Predicate<Boolean> predShortCircuit)
            {
            m_pipeline = pipeline;
            m_fnMatcher = fnMatcher;
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
            IntStream stream = m_pipeline.evaluate(streamer.stream());
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
        private RemotePipeline<? extends IntStream> m_pipeline;

        /**
         * Matching operation to invoke on the stream
         */
        @JsonbProperty("fnMatcher")
        private Remote.Function<IntStream, Boolean> m_fnMatcher;

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
     */
    public static class FinderAggregator<K, V>
            implements InvocableMap.StreamingAggregator<K, V, OptionalInt, OptionalInt>,
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
        FinderAggregator(RemotePipeline<? extends IntStream> pipeline,
                         Remote.Function<IntStream, OptionalInt> fnFinder)
            {
            m_pipeline = pipeline;
            m_fnFinder = fnFinder;
            }

        // ---- InvocableMap.StreamingAggregator interface ------------------

        @Override
        public InvocableMap.StreamingAggregator<K, V, OptionalInt, OptionalInt> supply()
            {
            return new FinderAggregator<>(m_pipeline, m_fnFinder);
            }

        @Override
        public boolean accumulate(Streamer<? extends InvocableMap.Entry<? extends K, ? extends V>> streamer)
            {
            IntStream stream = m_pipeline.evaluate(streamer.stream());
            m_result = m_fnFinder.apply(stream);
            return !m_result.isPresent();
            }

        @Override
        public boolean accumulate(InvocableMap.Entry<? extends K, ? extends V> entry)
            {
            throw new UnsupportedOperationException();
            }

        @Override
        public boolean combine(OptionalInt partialResult)
            {
            if (!m_fDone)
                {
                m_result = partialResult;
                m_fDone  = m_result.isPresent();
                }
            return !m_fDone;
            }

        @Override
        public OptionalInt getPartialResult()
            {
            return m_result;
            }

        @Override
        public OptionalInt finalizeResult()
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
        private RemotePipeline<? extends IntStream> m_pipeline;

        /**
         * Find operation to invoke on the stream
         */
        @JsonbProperty("fnFinder")
        private Remote.Function<IntStream, OptionalInt> m_fnFinder;

        /**
         * The aggregation result.
         */
        private transient OptionalInt m_result = OptionalInt.empty();

        /**
         * If true, indicates that no more accumulation is necessary.
         */
        private transient boolean m_fDone;
        }


    // ---- inner class: Optional -------------------------------------------

    /**
     * Serializable replacement for OptionalInt.
     */
    public static class Optional
            implements Remote.IntConsumer, ExternalizableLite, PortableObject
        {
        // ---- constructors ------------------------------------------------

        /**
         * Deserialization constructor.
         */
        public Optional()
            {
            }

        /**
         * Construct Optional instance.
         *
         * @param op  binary operator to use
         */
        public Optional(IntBinaryOperator op)
            {
            this.m_op = op;
            }

        // ---- accessors ---------------------------------------------------

        public int getValue()
            {
            return m_value;
            }

        public boolean isPresent()
            {
            return m_fPresent;
            }

        // ---- IntConsumer interface ---------------------------------------

        @Override
        public void accept(int t)
            {
            if (m_fPresent)
                {
                m_value = m_op.applyAsInt(m_value, t);
                }
            else
                {
                m_value = t;
                m_fPresent = true;
                }
            }

        // ----- ExternalizableLite interface -------------------------------

        @Override
        public void readExternal(DataInput in)
                throws IOException
            {
            m_op       = ExternalizableHelper.readObject(in);
            m_value    = in.readInt();
            m_fPresent = in.readBoolean();
            }

        @Override
        public void writeExternal(DataOutput out)
                throws IOException
            {
            ExternalizableHelper.writeObject(out, m_op);
            out.writeInt(m_value);
            out.writeBoolean(m_fPresent);
            }

        // ---- PortableObject interface ------------------------------------

        public void readExternal(PofReader reader) throws IOException
            {
            m_op       = reader.readObject(0);
            m_value    = reader.readInt(1);
            m_fPresent = reader.readBoolean(2);
            }

        public void writeExternal(PofWriter writer) throws IOException
            {
            writer.writeObject(0, m_op);
            writer.writeInt(1, m_value);
            writer.writeBoolean(2, m_fPresent);
            }

        // ---- data members ------------------------------------------------

        @JsonbProperty("op")
        private IntBinaryOperator m_op;

        @JsonbProperty("value")
        private int m_value = 0;

        @JsonbProperty("isPresent")
        private boolean m_fPresent = false;
        }
    }