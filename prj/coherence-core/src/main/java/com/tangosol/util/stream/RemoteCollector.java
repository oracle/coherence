/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.util.stream;

import com.tangosol.internal.util.stream.collectors.SimpleRemoteCollector;

import com.tangosol.util.function.Remote;

import java.io.Serializable;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;

import java.util.stream.Collector;

/**
 * An extension of {@link Collector} that adds serialization support.
 *
 * @param <T> the type of input elements to the reduction operation
 * @param <A> the mutable accumulation type of the reduction operation (often
 *            hidden as an implementation detail)
 * @param <R> the result type of the reduction operation
 *
 * @see RemoteStream#collect(RemoteCollector)
 * @see RemoteCollectors
 *
 * @author as  2014.10.01
 * @since 12.2.1
 */
public interface RemoteCollector<T, A, R>
        extends Collector<T, A, R>, Serializable
    {
    /**
     * Returns a new {@code Collector} described by the given {@code supplier},
     * {@code accumulator}, and {@code combiner} functions.  The resulting
     * {@code Collector} has the {@code Collector.Characteristics.IDENTITY_FINISH}
     * characteristic.
     *
     * @param supplier         The supplier function for the new collector
     * @param accumulator      The accumulator function for the new collector
     * @param combiner         The combiner function for the new collector
     * @param characteristics  The collector characteristics for the new
     *                         collector
     *
     * @param <T>  The type of input elements for the new collector
     * @param <R>  The type of intermediate accumulation result, and final result,
     *             for the new collector
     *
     * @return the new {@code Collector}
     *
     * @throws NullPointerException if any argument is null
     */
    public static<T, R> RemoteCollector<T, R, R> of(Supplier<R> supplier,
                                              BiConsumer<R, T> accumulator,
                                              BinaryOperator<R> combiner,
                                              Characteristics... characteristics) {
        Objects.requireNonNull(supplier);
        Objects.requireNonNull(accumulator);
        Objects.requireNonNull(combiner);
        Objects.requireNonNull(characteristics);

        Set<Characteristics> cs = (characteristics.length == 0)
                                  ? EnumSet.of(Characteristics.IDENTITY_FINISH)
                                  : EnumSet.of(Characteristics.IDENTITY_FINISH, characteristics);

        return new SimpleRemoteCollector<>(supplier, accumulator, combiner, Collections.unmodifiableSet(cs));
    }

    /**
     * Returns a new {@code Collector} described by the given {@code supplier},
     * {@code accumulator}, and {@code combiner} functions.  The resulting
     * {@code Collector} has the {@code Collector.Characteristics.IDENTITY_FINISH}
     * characteristic.
     *
     * @param supplier         The supplier function for the new collector
     * @param accumulator      The accumulator function for the new collector
     * @param combiner         The combiner function for the new collector
     * @param characteristics  The collector characteristics for the new
     *                         collector
     *
     * @param <T>  The type of input elements for the new collector
     * @param <R>  The type of intermediate accumulation result, and final result,
     *             for the new collector
     *
     * @return the new {@code Collector}
     *
     * @throws NullPointerException if any argument is null
     */
    public static <T, R> RemoteCollector<T, R, R> of(Remote.Supplier<R> supplier,
                                               Remote.BiConsumer<R, T> accumulator,
                                               Remote.BinaryOperator<R> combiner,
                                               Characteristics... characteristics)
        {
        return of((Supplier<R>) supplier, (BiConsumer<R, T>) accumulator, (BinaryOperator<R>) combiner, characteristics);
        }

    /**
     * Returns a new {@code Collector} described by the given {@code supplier},
     * {@code accumulator}, {@code combiner}, and {@code finisher} functions.
     *
     * @param supplier        The supplier function for the new collector
     * @param accumulator     The accumulator function for the new collector
     * @param combiner        The combiner function for the new collector
     * @param finisher        The finisher function for the new collector
     * @param characteristics The collector characteristics for the new
     *                        collector
     * @param <T>             The type of input elements for the new collector
     * @param <A>             The intermediate accumulation type of the new
     *                        collector
     * @param <R>             The final result type of the new collector
     *
     * @return the new {@code Collector}
     *
     * @throws NullPointerException if any argument is null
     */
    public static<T, A, R> RemoteCollector<T, A, R> of(Supplier<A> supplier,
                                                       BiConsumer<A, T> accumulator,
                                                       BinaryOperator<A> combiner,
                                                       Function<A, R> finisher,
                                                       Characteristics... characteristics) {
        Objects.requireNonNull(supplier);
        Objects.requireNonNull(accumulator);
        Objects.requireNonNull(combiner);
        Objects.requireNonNull(finisher);
        Objects.requireNonNull(characteristics);

        Set<Characteristics> cs = Collections.emptySet();
        if (characteristics.length > 0) {
            cs = EnumSet.noneOf(Characteristics.class);
            Collections.addAll(cs, characteristics);
            cs = Collections.unmodifiableSet(cs);
        }

        return new SimpleRemoteCollector<>(supplier, accumulator, combiner, finisher, cs);
    }

    /**
     * Returns a new {@code Collector} described by the given {@code supplier},
     * {@code accumulator}, {@code combiner}, and {@code finisher} functions.
     *
     * @param supplier        The supplier function for the new collector
     * @param accumulator     The accumulator function for the new collector
     * @param combiner        The combiner function for the new collector
     * @param finisher        The finisher function for the new collector
     * @param characteristics The collector characteristics for the new
     *                        collector
     * @param <T>             The type of input elements for the new collector
     * @param <A>             The intermediate accumulation type of the new
     *                        collector
     * @param <R>             The final result type of the new collector
     *
     * @return the new {@code Collector}
     *
     * @throws NullPointerException if any argument is null
     */
    public static <T, A, R> RemoteCollector<T, A, R> of(Remote.Supplier<A> supplier,
                                                        Remote.BiConsumer<A, T> accumulator,
                                                        Remote.BinaryOperator<A> combiner,
                                                        Remote.Function<A, R> finisher,
                                                        Characteristics... characteristics)
        {
        return of((Supplier<A>) supplier, (BiConsumer<A, T>) accumulator, (BinaryOperator<A>) combiner, (Function<A, R>) finisher, characteristics);
        }
    }
