/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.executor;

import com.oracle.coherence.concurrent.executor.function.Predicates;

import com.tangosol.util.function.Remote.Predicate;

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
        implements Task.Collector<T, List<T>, R>
    {
    // ----- constructors ---------------------------------------------------

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
        f_predicate     = predicate;
        f_collector     = (Task.Collector<T, A, R>) collector;
        f_defaultResult = defaultResult;
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
            if (f_predicate.test(results.iterator()))
                {
                // use the provided collector to perform the actual collection
                A                collectorContainer   = f_collector.supplier().get();
                BiConsumer<A, T> collectorAccumulator = f_collector.accumulator();
                Predicate<A>     collectorFinishable  = f_collector.finishable();
                Function<A, R>   collectorFinisher    = f_collector.finisher();

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
                return f_defaultResult;
                }
            };
        }

    @Override
    public Supplier<List<T>> supplier()
        {
        return ArrayList::new;
        }

    // ----- data members ---------------------------------------------------

    /**
     * The {@link Predicate} to determine when results can be collected.
     */
    protected final Predicate<? super Iterator<T>> f_predicate;

    /**
     * The {@link Task.Collector} to use when the {@link Predicate} is satisfied.
     */
    protected final Task.Collector<T, A, R> f_collector;

    /**
     * The default result to return when the {@link Predicate} is not satisfied.
     */
    protected final R f_defaultResult;
    }
