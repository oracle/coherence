/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.tangosol.internal.net.management;

import java.util.EnumSet;
import java.util.Set;

import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;

import java.util.stream.Collector;

/**
 * MBeanAttribute represents an attribute of an MBean that can be aggregated.
 * This interface allows an attribute to specialize the {@link Collector} that
 * should be used to reduce many values to a single value. It also allows
 * specializing the name of the attribute and whether this attribute should
 * be avoided.
 *
 * @author hr 2016.07.21
 * @since 12.2.1.4.0
 */
public interface MBeanAttribute<T, A, R>
    {
    /**
     * A {@link Collector} to reduce many MBean attribute values.
     *
     * @return a Collector to reduce many MBean attribute values
     */
    public Collector<T, A, R> collector();

    /**
     * Return a description to be used instead of the attribute name or null.
     *
     * @return a description to be used instead of the attribute name or null
     */
    public String description();

    /**
     * Return whether this attribute should be visible.
     *
     * @return whether this attribute should be visible
     */
    public default boolean isVisible()
        {
        return true;
        }

    // ----- inner class: NullCollector -------------------------------------

    /**
     * A {@link Collector} implementation that results in a null result.
     *
     * @param <T> the type of input elements to the reduction operation
     * @param <A> the mutable accumulation type of the reduction operation (often
     *            hidden as an implementation detail)
     * @param <R> the result type of the reduction operation
     */
    public static class NullCollector<T, A, R>
        implements Collector<T, A, R>
        {
        // ----- Collector interface ----------------------------------------

        @Override
        public Supplier<A> supplier()
            {
            return () -> null;
            }

        @Override
        public BiConsumer<A, T> accumulator()
            {
            return (result, element) -> {};
            }

        @Override
        public BinaryOperator<A> combiner()
            {
            return (result1, result2) -> result1;
            }

        @Override
        public Function<A, R> finisher()
            {
            return (result) -> (R) result;
            }

        @Override
        public Set<Characteristics> characteristics()
            {
            return EnumSet.allOf(Characteristics.class);
            }

        // ----- constants --------------------------------------------------

        /**
         * An instance of a NullCollector.
         */
        public static Collector INSTANCE = new NullCollector<>();

        /**
         * Return a type-safe instance of a NullCollector.
         *
         * @return a type-safe instance of a NullCollector
         */
        public static <T, A, R> Collector<T, A, R> INSTANCE()
            {
            return INSTANCE;
            }
        }
    }
