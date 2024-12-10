/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.util.stream;

import com.tangosol.internal.util.DoubleBag;
import com.tangosol.internal.util.DoubleSummaryStatistics;

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
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.Spliterators;

import java.util.function.BiConsumer;
import java.util.function.DoubleToIntFunction;
import java.util.function.Function;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoublePredicate;
import java.util.function.DoubleToLongFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.ObjDoubleConsumer;
import java.util.function.Supplier;

import java.util.stream.BaseStream;
import java.util.stream.DoubleStream;

import javax.json.bind.annotation.JsonbProperty;

/**
 * Abstract base class for an intermediate pipeline stage or pipeline source
 * stage implementing whose elements are of type {@code int}.
 *
 * @param <E_IN> type of elements in the upstream source
 *
 * @author as  2014.10.08
 * @since 12.2.1
 */
@SuppressWarnings("Convert2MethodRef")
public abstract class DoublePipeline<K, V, E_IN, S_IN extends BaseStream<E_IN, S_IN>>
        extends AbstractPipeline<K, V, E_IN, Double, S_IN, DoubleStream>
        implements RemoteDoubleStream
    {
    // ---- constructors ----------------------------------------------------

    /**
     * Deserialization constructor.
     */
    public DoublePipeline()
        {
        }

    /**
     * Construct intermediate stage of the pipeline.
     *
     * @param previousStage   previous stage in the pipeline
     * @param intermediateOp  Intermediate operation performed by this stage
     */
    protected DoublePipeline(AbstractPipeline<K, V, ?, E_IN, ?, S_IN> previousStage, Function<S_IN, DoubleStream> intermediateOp)
        {
        super(previousStage, intermediateOp);
        }

    // ---- intermediate operations -----------------------------------------

    public RemoteDoubleStream sequential()
        {
        setParallel(false);
        return this;
        }

    public RemoteDoubleStream parallel()
        {
        setParallel(true);
        return this;
        }

    public RemoteDoubleStream unordered()
        {
        return new StatelessOp<>(this, s -> s.unordered());
        }

    public RemoteDoubleStream filter(DoublePredicate predicate)
        {
        return new StatelessOp<>(this, s -> s.filter(predicate));
        }

    public RemoteDoubleStream map(DoubleUnaryOperator mapper)
        {
        return new StatelessOp<>(this, s -> s.map(mapper));
        }

    public <U> RemoteStream<U> mapToObj(DoubleFunction<? extends U> mapper)
        {
        return new ReferencePipeline.StatelessOp<>(this, s -> s.mapToObj(mapper));
        }

    public RemoteLongStream mapToLong(DoubleToLongFunction mapper)
        {
        return new LongPipeline.StatelessOp<>(this, s -> s.mapToLong(mapper));
        }

    public RemoteIntStream mapToInt(DoubleToIntFunction mapper)
        {
        return new IntPipeline.StatelessOp<>(this, s -> s.mapToInt(mapper));
        }

    public RemoteDoubleStream flatMap(DoubleFunction<? extends DoubleStream> mapper)
        {
        return new StatelessOp<>(this, (s) -> s.flatMap(mapper));
        }

    public RemoteDoubleStream peek(DoubleConsumer action)
        {
        return new StatelessOp<>(this, (s) -> s.peek(action));
        }

    public DoubleStream limit(long maxSize)
        {
        StatefulOp<K, V, Double, DoubleStream> op =
                new StatefulOp<>(this, (s) -> s.limit(maxSize));
        return Arrays.stream(collectToBag(op).toArray()).limit(maxSize);
        }

    public DoubleStream skip(long n)
        {
        return Arrays.stream(collectToBag(this).toArray()).skip(n);
        }

    public DoubleStream distinct()
        {
        StatefulOp<K, V, Double, DoubleStream> op =
                new StatefulOp<>(this, (s) -> s.distinct());
        return Arrays.stream(collectToBag(op).toArray());
        }

    public DoubleStream sorted()
        {
        StatefulOp<K, V, Double, DoubleStream> op =
                new StatefulOp<>(this, (s) -> s.sorted());
        return Arrays.stream(collectToBag(op).toArray()).sorted();
        }

    @SuppressWarnings("unchecked")
    public RemoteStream<Double> boxed()
        {
        return new ReferencePipeline.StatelessOp<>(this, (s) -> s.boxed());
        }

    // ---- terminal operations ---------------------------------------------

    public void forEach(DoubleConsumer action)
        {
        collectToBag(this).forEach(action);
        }

    public void forEachOrdered(DoubleConsumer action)
        {
        forEach(action);
        }

    public double[] toArray()
        {
        return collectToBag(this).toArray();
        }

    public double reduce(double identity, DoubleBinaryOperator op)
        {
        return collect(() -> new double[] {identity},
                       (a, t) -> a[0] = op.applyAsDouble(a[0], t),
                       (a, b) -> a[0] = op.applyAsDouble(a[0], b[0]))[0];
        }

    public OptionalDouble reduce(DoubleBinaryOperator op)
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
               ? OptionalDouble.of(result.getValue()) : OptionalDouble.empty();
        }

    public <R> R collect(Supplier<R> supplier, ObjDoubleConsumer<R> accumulator, BiConsumer<R, R> combiner)
        {
        return collect(this, supplier, accumulator, combiner);
        }

    public double sum()
        {
        return reduce(0, Double::sum);
        }

    public OptionalDouble min()
        {
        return reduce(Math::min);
        }

    public OptionalDouble max()
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
                             (ll, i) ->
                                 {
                                 ll[0]++;
                                 ll[1] += i;
                                 },
                             (ll, rr) ->
                                 {
                                 ll[0] += rr[0];
                                 ll[1] += rr[1];
                                 });
        return avg[0] > 0
               ? OptionalDouble.of((double) avg[1] / avg[0])
               : OptionalDouble.empty();
        }

    public DoubleSummaryStatistics summaryStatistics()
        {
        return collect(DoubleSummaryStatistics::new,
                       DoubleSummaryStatistics::accept,
                       DoubleSummaryStatistics::combine);
        }

    public boolean anyMatch(DoublePredicate predicate)
        {
        return invoke(new MatcherAggregator<>(this, (s) -> s.anyMatch(predicate), (p) -> p));
        }

    public boolean allMatch(DoublePredicate predicate)
        {
        return invoke(new MatcherAggregator<>(this, (s) -> s.allMatch(predicate), (p) -> !p));
        }

    public boolean noneMatch(DoublePredicate predicate)
        {
        return invoke(new MatcherAggregator<>(this, (s) -> s.noneMatch(predicate), (p) -> !p));
        }

    public OptionalDouble findFirst()
        {
        return invoke(new FinderAggregator<>(this, DoubleStream::findFirst));
        }

    public OptionalDouble findAny()
        {
        return invoke(new FinderAggregator<>(this, DoubleStream::findAny));
        }

    // ---- AbstractPipeline overrides --------------------------------------

    @Override
    public final PrimitiveIterator.OfDouble iterator() {
        return Spliterators.iterator(spliterator());
    }

    @Override
    public final Spliterator.OfDouble spliterator() {
        return Spliterators.spliterator(toArray(), 0);
    }

    // ---- helpers ---------------------------------------------------------

    protected DoubleBag collectToBag(RemotePipeline<DoubleStream> pipeline)
        {
        return collect(pipeline, DoubleBag::new, DoubleBag::add, DoubleBag::addAll);
        }

    protected <R> R collect(RemotePipeline<DoubleStream> pipeline, Remote.Supplier<R> supplier, Remote.ObjDoubleConsumer<R> accumulator, Remote.BiConsumer<R, R> combiner)
        {
        return invoke(new DoubleCollectorAggregator<>(pipeline, supplier, accumulator, combiner));
        }

    protected <R> R collect(RemotePipeline<DoubleStream> pipeline, Supplier<R> supplier, ObjDoubleConsumer<R> accumulator, BiConsumer<R, R> combiner)
        {
        return invoke(new DoubleCollectorAggregator<>(pipeline, supplier, accumulator, combiner));
        }

    // ---- pipeline stage implementations ----------------------------------

    /**
     * A stage in a pipeline representing stateless operation.
     *
     * @param <E_IN>  the type of input elements
     * @param <S_IN>  the type of input stream
     */
    public static class StatelessOp<K, V, E_IN, S_IN extends BaseStream<E_IN, S_IN>>
            extends DoublePipeline<K, V, E_IN, S_IN>
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
        StatelessOp(AbstractPipeline<K, V, ?, E_IN, ?, S_IN> previousStage, Remote.Function<S_IN, DoubleStream> intermediateOp)
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
            extends DoublePipeline<K, V, E_IN, S_IN>
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
        StatefulOp(AbstractPipeline<K, V, ?, E_IN, ?, S_IN> previousStage, Remote.Function<S_IN, DoubleStream> intermediateOp)
            {
            super(previousStage, intermediateOp);
            }
        }

    // ---- inner class: MatcherAggregator ----------------------------------

    /**
     * Aggregator used by matching terminal operations ({@link RemoteDoubleStream#anyMatch},
     * {@link RemoteDoubleStream#allMatch} and {@link RemoteDoubleStream#noneMatch}).
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
        MatcherAggregator(RemotePipeline<? extends DoubleStream> pipeline,
                          Remote.Function<DoubleStream, Boolean> fnMatcher,
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
            DoubleStream stream = m_pipeline.evaluate(streamer.stream());
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
        private RemotePipeline<? extends DoubleStream> m_pipeline;

        /**
         * Matching operation to invoke on the stream
         */
        @JsonbProperty("fnMatcher")
        private Remote.Function<DoubleStream, Boolean> m_fnMatcher;

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
            implements InvocableMap.StreamingAggregator<K, V, OptionalDouble, OptionalDouble>,
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
        FinderAggregator(RemotePipeline<? extends DoubleStream> pipeline,
                         Remote.Function<DoubleStream, OptionalDouble> fnFinder)
            {
            m_pipeline = pipeline;
            m_fnFinder = fnFinder;
            }

        // ---- InvocableMap.StreamingAggregator interface ------------------

        @Override
        public InvocableMap.StreamingAggregator<K, V, OptionalDouble, OptionalDouble> supply()
            {
            return new FinderAggregator<>(m_pipeline, m_fnFinder);
            }

        @Override
        public boolean accumulate(Streamer<? extends InvocableMap.Entry<? extends K, ? extends V>> streamer)
            {
            DoubleStream stream = m_pipeline.evaluate(streamer.stream());
            m_result = m_fnFinder.apply(stream);
            return !m_result.isPresent();
            }

        @Override
        public boolean accumulate(InvocableMap.Entry<? extends K, ? extends V> entry)
            {
            throw new UnsupportedOperationException();
            }

        @Override
        public boolean combine(OptionalDouble partialResult)
            {
            if (!m_fDone)
                {
                m_result = partialResult;
                m_fDone  = m_result.isPresent();
                }
            return !m_fDone;
            }

        @Override
        public OptionalDouble getPartialResult()
            {
            return m_result;
            }

        @Override
        public OptionalDouble finalizeResult()
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
        private RemotePipeline<? extends DoubleStream> m_pipeline;

        /**
         * Find operation to invoke on the stream
         */
        @JsonbProperty("fnFinder")
        private Remote.Function<DoubleStream, OptionalDouble> m_fnFinder;

        /**
         * The aggregation result.
         */
        private transient OptionalDouble m_result = OptionalDouble.empty();

        /**
         * If true, indicates that no more accumulation is necessary.
         */
        private transient boolean m_fDone;
        }

    // ---- inner class: Optional -------------------------------------------

    /**
     * Serializable replacement for OptionalDouble.
     */
    public static class Optional
            implements Remote.DoubleConsumer, ExternalizableLite, PortableObject
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
        public Optional(DoubleBinaryOperator op)
            {
            this.m_op = op;
            }

        // ---- accessors ---------------------------------------------------

        public double getValue()
            {
            return m_value;
            }

        public boolean isPresent()
            {
            return m_fPresent;
            }

        // ---- DoubleConsumer interface ---------------------------------------

        @Override
        public void accept(double t)
            {
            if (m_fPresent)
                {
                m_value = m_op.applyAsDouble(m_value, t);
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
            m_value    = in.readDouble();
            m_fPresent = in.readBoolean();
            }

        @Override
        public void writeExternal(DataOutput out)
                throws IOException
            {
            ExternalizableHelper.writeObject(out, m_op);
            out.writeDouble(m_value);
            out.writeBoolean(m_fPresent);
            }

        // ---- PortableObject interface ------------------------------------

        public void readExternal(PofReader in) throws IOException
            {
            m_op       = in.readObject(0);
            m_value    = in.readDouble(1);
            m_fPresent = in.readBoolean(2);
            }

        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeObject(0, m_op);
            out.writeDouble(1, m_value);
            out.writeBoolean(2, m_fPresent);
            }

        // ---- data members ------------------------------------------------

        @JsonbProperty("op")
        private DoubleBinaryOperator m_op;

        @JsonbProperty("value")
        private double m_value = 0;

        @JsonbProperty("isPresent")
        private boolean m_fPresent = false;
        }
    }