/*
 * Copyright (c) 2016, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.executor;

import com.oracle.coherence.concurrent.executor.function.Predicates;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.ExternalizableHelper;

import com.tangosol.util.function.Remote.Predicate;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A {@link Task.Collector} that conditionally collects values based on the available result.
 *
 * @param <T>  the type of input elements to the reduction operation
 * @param <A>  the mutable accumulation type of the reduction operation (often hidden as an implementation detail)
 * @param <R>  the result type of the reduction operation
 *
 * @author bo
 * @since 21.12
 */
public class ConditionalCollector<T, A, R>
        implements Task.Collector<T, List<T>, R>, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * For serialization.
     */
    @SuppressWarnings("unused")
    public ConditionalCollector()
        {
        }

    /**
     * Constructs a {@link ConditionalCollector}.
     *
     * @param predicate      the {@link Predicate} to determine if collection should occur
     * @param collector      the {@link Task.Collector} to use when the {@link Predicate} is satisfied
     * @param defaultResult  the default result to use when the {@link Predicate} is not satisfied
     */
    @SuppressWarnings("unchecked")
    public ConditionalCollector(Predicate<? super Iterator<T>> predicate, Task.Collector<? super T, A, R>  collector,
                                R defaultResult)
        {
        m_predicate     = predicate;
        m_collector     = (Task.Collector<T, A, R>) collector;
        m_defaultResult = defaultResult;
        }

    // ----- Task.Collector interface ---------------------------------------

    @Override
    public BiConsumer<List<T>, T> accumulator()
        {
        return List::add;
        }

    @Override
    public Predicate<List<T>> finishable()
        {
        return Predicates.never();
        }

    @Override
    public Function<List<T>, R> finisher()
        {
        return results ->
            {
            if (m_predicate.test(results.iterator()))
                {
                // use the provided collector to perform the actual collection
                A                collectorContainer   = m_collector.supplier().get();
                BiConsumer<A, T> collectorAccumulator = m_collector.accumulator();
                Predicate<A>     collectorFinishable  = m_collector.finishable();
                Function<A, R>   collectorFinisher    = m_collector.finisher();

                for (Iterator<T> iterator = results.iterator();
                         iterator.hasNext() && !collectorFinishable.test(collectorContainer); )
                    {
                    T result = iterator.next();

                    collectorAccumulator.accept(collectorContainer, result);
                    }

                return collectorFinisher.apply(collectorContainer);
                }
            else
                {
                return m_defaultResult;
                }
            };
        }

    @Override
    public Supplier<List<T>> supplier()
        {
        return ArrayList::new;
        }

    // ----- ExternalizableLite interface -------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_predicate     = ExternalizableHelper.readObject(in);
        m_collector     = ExternalizableHelper.readObject(in);
        m_defaultResult = ExternalizableHelper.readObject(in);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        ExternalizableHelper.writeObject(out, m_predicate);
        ExternalizableHelper.writeObject(out, m_collector);
        ExternalizableHelper.writeObject(out, m_defaultResult);
        }

    // ----- PortableObject interface ---------------------------------------

    public void readExternal(PofReader in) throws IOException
        {
        m_predicate     = in.readObject(0);
        m_collector     = in.readObject(1);
        m_defaultResult = in.readObject(2);
        }

    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeObject(0, m_predicate);
        out.writeObject(1, m_collector);
        out.writeObject(2, m_defaultResult);
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link Predicate} to determine when results can be collected.
     */
    protected Predicate<? super Iterator<T>> m_predicate;

    /**
     * The {@link Task.Collector} to use when the {@link Predicate} is satisfied.
     */
    protected Task.Collector<T, A, R> m_collector;

    /**
     * The default result to return when the {@link Predicate} is not satisfied.
     */
    protected R m_defaultResult;
    }
