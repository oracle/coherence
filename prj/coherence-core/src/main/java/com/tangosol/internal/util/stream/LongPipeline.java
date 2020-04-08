/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.stream;

import com.tangosol.internal.util.LongBag;
import com.tangosol.internal.util.LongSummaryStatistics;

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
import java.util.OptionalLong;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.Spliterators;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;
import java.util.function.LongPredicate;
import java.util.function.LongToDoubleFunction;
import java.util.function.LongToIntFunction;
import java.util.function.LongUnaryOperator;
import java.util.function.ObjLongConsumer;
import java.util.function.Supplier;

import java.util.stream.BaseStream;
import java.util.stream.LongStream;

import javax.json.bind.annotation.JsonbProperty;

/**
 * Abstract base class for an intermediate pipeline stage or pipeline source
 * stage implementing whose elements are of type {@code long}.
 *
 * @param <E_IN> type of elements in the upstream source
 *
 * @author as  2014.10.08
 * @since 12.2.1
 */
@SuppressWarnings("Convert2MethodRef")
public abstract class LongPipeline<K, V, E_IN, S_IN extends BaseStream<E_IN, S_IN>>
        extends AbstractPipeline<K, V, E_IN, Long, S_IN, LongStream>
        implements RemoteLongStream
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Deserialization constructor.
     */
    public LongPipeline()
        {
        }

    /**
     * Construct intermediate stage of the pipeline.
     *
     * @param previousStage   previous stage in the pipeline
     * @param intermediateOp  Intermediate operation performed by this stage
     */
    protected LongPipeline(AbstractPipeline<K, V, ?, E_IN, ?, S_IN> previousStage,
                 Function<S_IN, LongStream> intermediateOp)
        {
        super(previousStage, intermediateOp);
        }

    // ---- intermediate operations -----------------------------------------

    public RemoteLongStream sequential()
        {
        setParallel(false);
        return this;
        }

    public RemoteLongStream parallel()
        {
        setParallel(true);
        return this;
        }

    public RemoteLongStream unordered()
        {
        return new StatelessOp<>(this, s -> s.unordered());
        }

    public RemoteLongStream filter(LongPredicate predicate)
        {
        return new StatelessOp<>(this, s -> s.filter(predicate));
        }

    public RemoteLongStream map(LongUnaryOperator mapper)
        {
        return new StatelessOp<>(this, s -> s.map(mapper));
        }

    public <U> RemoteStream<U> mapToObj(LongFunction<? extends U> mapper)
        {
        return new ReferencePipeline.StatelessOp<>(this, s -> s.mapToObj(mapper));
        }

    public RemoteIntStream mapToInt(LongToIntFunction mapper)
        {
        return new IntPipeline.StatelessOp<>(this, s -> s.mapToInt(mapper));
        }

    public RemoteDoubleStream mapToDouble(LongToDoubleFunction mapper)
        {
        return new DoublePipeline.StatelessOp<>(this, s -> s.mapToDouble(mapper));
        }

    public RemoteLongStream flatMap(LongFunction<? extends LongStream> mapper)
        {
        return new StatelessOp<>(this, (s) -> s.flatMap(mapper));
        }

    public RemoteLongStream peek(LongConsumer action)
        {
        return new StatelessOp<>(this, (s) -> s.peek(action));
        }

    public LongStream limit(long maxSize)
        {
        StatefulOp<K, V, Long, LongStream> op =
                new StatefulOp<>(this, (s) -> s.limit(maxSize));
        return Arrays.stream(collectToBag(op).toArray()).limit(maxSize);
        }

    public LongStream skip(long n)
        {
        return Arrays.stream(collectToBag(this).toArray()).skip(n);
        }

    public LongStream distinct()
        {
        StatefulOp<K, V, Long, LongStream> op =
                new StatefulOp<>(this, (s) -> s.distinct());
        return Arrays.stream(collectToBag(op).toArray());
        }

    public LongStream sorted()
        {
        StatefulOp<K, V, Long, LongStream> op =
                new StatefulOp<>(this, (s) -> s.sorted());
        return Arrays.stream(collectToBag(op).toArray()).sorted();
        }

    public RemoteDoubleStream asDoubleStream()
        {
        return new DoublePipeline.StatelessOp<>(this, s -> s.asDoubleStream());
        }

    @SuppressWarnings("unchecked")
    public RemoteStream<Long> boxed()
        {
        return new ReferencePipeline.StatelessOp<>(this, (s) -> s.boxed());
        }

    // ---- terminal operations ---------------------------------------------

    public void forEach(LongConsumer action)
        {
        collectToBag(this).forEach(action);
        }

    public void forEachOrdered(LongConsumer action)
        {
        forEach(action);
        }

    public long[] toArray()
        {
        return collectToBag(this).toArray();
        }

    public long reduce(long identity, LongBinaryOperator op)
        {
        return collect(() -> new long[] {identity},
                       (a, t) -> a[0] = op.applyAsLong(a[0], t),
                       (a, b) -> a[0] = op.applyAsLong(a[0], b[0]))[0];
        }

    public OptionalLong reduce(LongBinaryOperator op)
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
               ? OptionalLong.of(result.getValue()) : OptionalLong.empty();
        }

    public <R> R collect(Supplier<R> supplier, ObjLongConsumer<R> accumulator, BiConsumer<R, R> combiner)
        {
        return collect(this, supplier, accumulator, combiner);
        }

    public long sum()
        {
        return reduce(0, Long::sum);
        }

    public OptionalLong min()
        {
        return reduce(Math::min);
        }

    public OptionalLong max()
        {
        return reduce(Math::max);
        }

    public long count()
        {
        return map(e -> 1L).sum();
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

    public LongSummaryStatistics summaryStatistics()
        {
        return collect(LongSummaryStatistics::new,
                       LongSummaryStatistics::accept,
                       LongSummaryStatistics::combine);
        }

    public boolean anyMatch(LongPredicate predicate)
        {
        return invoke(new MatcherAggregator<>(this, (s) -> s.anyMatch(predicate), (p) -> p));
        }

    public boolean allMatch(LongPredicate predicate)
        {
        return invoke(new MatcherAggregator<>(this, (s) -> s.allMatch(predicate), (p) -> !p));
        }

    public boolean noneMatch(LongPredicate predicate)
        {
        return invoke(new MatcherAggregator<>(this, (s) -> s.noneMatch(predicate), (p) -> !p));
        }

    public OptionalLong findFirst()
        {
        return invoke(new FinderAggregator<>(this, LongStream::findFirst));
        }

    public OptionalLong findAny()
        {
        return invoke(new FinderAggregator<>(this, LongStream::findAny));
        }

    // ---- AbstractPipeline overrides --------------------------------------

    @Override
    public final PrimitiveIterator.OfLong iterator() {
        return Spliterators.iterator(spliterator());
    }

    @Override
    public final Spliterator.OfLong spliterator() {
        return Spliterators.spliterator(toArray(), 0);
    }

    // ---- helpers ---------------------------------------------------------

    protected LongBag collectToBag(RemotePipeline<LongStream> pipeline)
        {
        return collect(pipeline, LongBag::new, LongBag::add, LongBag::addAll);
        }

    protected <R> R collect(RemotePipeline<LongStream> pipeline, Remote.Supplier<R> supplier, Remote.ObjLongConsumer<R> accumulator, Remote.BiConsumer<R, R> combiner)
        {
        return invoke(new LongCollectorAggregator<>(pipeline, supplier, accumulator, combiner));
        }

    protected <R> R collect(RemotePipeline<LongStream> pipeline, Supplier<R> supplier, ObjLongConsumer<R> accumulator, BiConsumer<R, R> combiner)
        {
        return invoke(new LongCollectorAggregator<>(pipeline, supplier, accumulator, combiner));
        }


    // ---- pipeline stage implementations ----------------------------------

    /**
     * A stage in a pipeline representing stateless operation.
     *
     * @param <E_IN>  the type of input elements
     * @param <S_IN>  the type of input stream
     */
    public static class StatelessOp<K, V, E_IN, S_IN extends BaseStream<E_IN, S_IN>>
            extends LongPipeline<K, V, E_IN, S_IN>
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
        StatelessOp(AbstractPipeline<K, V, ?, E_IN, ?, S_IN> previousStage, Remote.Function<S_IN, LongStream> intermediateOp)
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
            extends LongPipeline<K, V, E_IN, S_IN>
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
        StatefulOp(AbstractPipeline<K, V, ?, E_IN, ?, S_IN> previousStage, Remote.Function<S_IN, LongStream> intermediateOp)
            {
            super(previousStage, intermediateOp);
            }
        }

    // ---- inner class: MatcherAggregator ----------------------------------

    /**
     * Aggregator used by matching terminal operations ({@link RemoteLongStream#anyMatch},
     * {@link RemoteLongStream#allMatch} and {@link RemoteLongStream#noneMatch}).
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
         * @param pipeline      pipeline of intermediate operations to evaluate
         * @param fnMatcher     matching operation to invoke on the stream
         * @param predShortCircuit  predicate that determines if the aggregation
         *                      should be short-circuited
         */
        MatcherAggregator(RemotePipeline<? extends LongStream> pipeline,
                          Remote.Function<LongStream, Boolean> fnMatcher,
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
            LongStream stream = m_pipeline.evaluate(streamer.stream());
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
        private RemotePipeline<? extends LongStream> m_pipeline;

        /**
         * Matching operation to invoke on the stream
         */
        @JsonbProperty("fnMatcher")
        private Remote.Function<LongStream, Boolean> m_fnMatcher;

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
            implements InvocableMap.StreamingAggregator<K, V, OptionalLong, OptionalLong>,
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
        FinderAggregator(RemotePipeline<? extends LongStream> pipeline,
                         Remote.Function<LongStream, OptionalLong> fnFinder)
            {
            m_pipeline = pipeline;
            m_fnFinder = fnFinder;
            }

        // ---- InvocableMap.StreamingAggregator interface ------------------

        @Override
        public InvocableMap.StreamingAggregator<K, V, OptionalLong, OptionalLong> supply()
            {
            return new FinderAggregator<>(m_pipeline, m_fnFinder);
            }

        @Override
        public boolean accumulate(Streamer<? extends InvocableMap.Entry<? extends K, ? extends V>> streamer)
            {
            LongStream stream = m_pipeline.evaluate(streamer.stream());
            m_result = m_fnFinder.apply(stream);
            return !m_result.isPresent();
            }

        @Override
        public boolean accumulate(InvocableMap.Entry<? extends K, ? extends V> entry)
            {
            throw new UnsupportedOperationException();
            }

        @Override
        public boolean combine(OptionalLong partialResult)
            {
            if (!m_fDone)
                {
                m_result = partialResult;
                m_fDone  = m_result.isPresent();
                }
            return !m_fDone;
            }

        @Override
        public OptionalLong getPartialResult()
            {
            return m_result;
            }

        @Override
        public OptionalLong finalizeResult()
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
        private RemotePipeline<? extends LongStream> m_pipeline;

        /**
         * Find operation to invoke on the stream
         */
        @JsonbProperty("fnFinder")
        private Remote.Function<LongStream, OptionalLong> m_fnFinder;

        /**
         * The aggregation result.
         */
        private transient OptionalLong m_result = OptionalLong.empty();

        /**
         * If true, indicates that no more accumulation is necessary.
         */
        private transient boolean m_fDone;
        }


    // ---- inner class: Optional -------------------------------------------

    /**
     * Serializable replacement for OptionalLong.
     */
    public static class Optional
            implements Remote.LongConsumer, ExternalizableLite, PortableObject
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
        public Optional(LongBinaryOperator op)
            {
            this.m_op = op;
            }

        // ---- accessors ---------------------------------------------------

        public long getValue()
            {
            return m_value;
            }

        public boolean isPresent()
            {
            return m_fPresent;
            }

        // ---- LongConsumer interface ---------------------------------------

        @Override
        public void accept(long t)
            {
            if (m_fPresent)
                {
                m_value = m_op.applyAsLong(m_value, t);
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
            m_value    = in.readLong();
            m_fPresent = in.readBoolean();
            }

        @Override
        public void writeExternal(DataOutput out)
                throws IOException
            {
            ExternalizableHelper.writeObject(out, m_op);
            out.writeLong(m_value);
            out.writeBoolean(m_fPresent);
            }

        // ---- PortableObject interface ------------------------------------

        public void readExternal(PofReader reader) throws IOException
            {
            m_op       = reader.readObject(0);
            m_value    = reader.readLong(1);
            m_fPresent = reader.readBoolean(2);
            }

        public void writeExternal(PofWriter writer) throws IOException
            {
            writer.writeObject(0, m_op);
            writer.writeLong(1, m_value);
            writer.writeBoolean(2, m_fPresent);
            }

        // ---- data members ------------------------------------------------

        @JsonbProperty("op")
        private LongBinaryOperator m_op;

        @JsonbProperty("value")
        private long m_value = 0;

        @JsonbProperty("isPresent")
        private boolean m_fPresent = false;
        }
    }