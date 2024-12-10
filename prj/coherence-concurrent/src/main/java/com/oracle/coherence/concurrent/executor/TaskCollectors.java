/*
 * Copyright (c) 2016, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.executor;

import com.oracle.coherence.concurrent.executor.function.Predicates;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.function.Remote.Predicate;

import java.io.IOException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import java.util.concurrent.atomic.AtomicReference;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Static helper methods to create {@link Task.Collector}s.
 *
 * @author bo
 * @since 21.06
 */
public final class TaskCollectors
    {
    // ----- constructors ---------------------------------------------------

    /**
     * New instances not allowed.
     */
    private TaskCollectors()
        {
        }

    // ----- public API -----------------------------------------------------

    /**
     * A {@link Task.Collector} that returns the {@link List} of results that are values.
     *
     * @param <T>  the type of the result
     *
     * @return a {@link Task.Collector}
     */
    public static <T> Task.Collector<T, ?, List<T>> listOf()
        {
        return new ListOfCollector<>();
        }

    /**
     * A {@link Task.Collector} that returns the {@link Set} of results that are values.
     *
     * @param <T>  the type of the result
     *
     * @return a {@link Task.Collector}
     */
    public static <T> Task.Collector<T, ?, Set<T>> setOf()
        {
        return new SetOfCollector<>();
        }

    /**
     * A {@link Task.Collector} that returns the last added result, when all results
     * have been provided.
     *
     * @param <T>  the type of the result
     *
     * @return a {@link Task.Collector}
     */
    public static <T> Task.Collector<T, ?, T> lastOf()
        {
        return new LastOfCollector<>();
        }

    /**
     * A {@link Task.Collector} that counts the number of non-null results that
     * have been made.
     *
     * @param <T>  the type of result
     *
     * @return a {@link Task.Collector}
     */
    public static <T> Task.Collector<? super T, ?, Integer> count()
        {
        return new CountCollector<>();
        }

    /**
     * A {@link Task.Collector} that will collect the first provided result.
     *
     * @param <T>  the type of result
     *
     * @return a {@link Task.Collector}
     */
    public static <T> Task.Collector<T, ?, T> firstOf()
        {
        return new FirstOfCollector<>();
        }

    // ----- inner class: CountCollector ------------------------------------

    /**
     * A {@link Task.Collector} that counts the number of results are present.
     *
     * @param <T>  the results type.
     */
    public static class CountCollector<T>
            extends AbstractCollector<T, Integer>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs a {@link CountCollector} (required for serialization).
         */
        public CountCollector()
            {
            }

        // ----- AbstractCollector methods ----------------------------------

        @Override
        public Function<List<T>, Integer> finisher()
            {
            return List::size;
            }

        // ----- PortableObject interface -----------------------------------

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            }
        }

    /**
     * A {@link Task.Collector} to collect any (the first provided) available result.
     *
     * @param <T>  the type of result
     */
    public static class FirstOfCollector<T>
            implements Task.Collector<T, AtomicReference<T>, T>, PortableObject
        {
        // ----- Task.Collector interface -----------------------------------

        @Override
        public BiConsumer<AtomicReference<T>, T> accumulator()
            {
            return AtomicReference::set;
            }

        @Override
        public Function<AtomicReference<T>, T> finisher()
            {
            return reference ->
                {
                try
                    {
                    return reference.get();
                    }
                catch (Throwable e)
                    {
                    throw new RuntimeException(e);
                    }
                };
            }

        @Override
        public Predicate<AtomicReference<T>> finishable()
            {
            return reference -> reference.get() != null;
            }

        @Override
        public Supplier<AtomicReference<T>> supplier()
            {
            return () -> new AtomicReference<>(null);
            }

        // ----- PortableObject interface -----------------------------------

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            }
        }

    // ----- inner class: LastOfCollector -----------------------------------

    /**
     * A {@link Task.Collector} that collects and returns the last contributed result.
     *
     * @param <T>  the type of result
     */
    public static class LastOfCollector<T>
            extends AbstractCollector<T, T>
        {
        // ----- AbstractCollector methods ----------------------------------

        @Override
        public Function<List<T>, T> finisher()
            {
            return results ->
                {
                T last = null;

                for (T result : results)
                    {
                    last = result;
                    }

                return last;
                };
            }

        // ----- PortableObject interface -----------------------------------

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            }
        }

    // ----- inner class: ListOfCollector -----------------------------------

    /**
     * A {@link Task.Collector} that collects and returns all contributed results
     * that are values as a {@link List}.
     *
     * @param <T>  the type of the result
     */
    public static class ListOfCollector<T>
            implements Task.Collector<T, List<T>, List<T>>, PortableObject
        {
        // ----- Task.Collector interface -----------------------------------

        @Override
        public BiConsumer<List<T>, T> accumulator()
            {
            return (list, result) ->
                {
                try
                    {
                    if (result != null)
                        {
                        list.add(result);
                        }
                    }
                catch (Throwable throwable)
                    {
                    // we skip adding the value if it's an exception
                    }
                };
            }

        @Override
        public Function<List<T>, List<T>> finisher()
            {
            return list -> list;
            }

        @Override
        public Predicate<List<T>> finishable()
            {
            return Predicates.never();
            }

        @Override
        public Supplier<List<T>> supplier()
            {
            return ArrayList::new;
            }

        // ----- PortableObject interface -----------------------------------

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            }
        }

    // ----- inner class: SetOfCollector ------------------------------------

    /**
     * A {@link Task.Collector} that collects and returns all contributed results that
     * are values as a {@link Set}.
     *
     * @param <T>  the type of the result
     */
    public static class SetOfCollector<T>
            implements Task.Collector<T, Set<T>, Set<T>>, PortableObject
        {
        // ----- Task.Collector interface -----------------------------------

        @Override
        public BiConsumer<Set<T>, T> accumulator()
            {
            return (set, result) ->
                {
                try
                    {
                    if (result != null)
                        {
                        set.add(result);
                        }
                    }
                catch (Throwable throwable)
                    {
                    // we skip adding the value if it's an exception
                    }
                };
            }

        @Override
        public Function<Set<T>, Set<T>> finisher()
            {
            return set -> set;
            }

        @Override
        public Predicate<Set<T>> finishable()
            {
            return Predicates.never();
            }

        @Override
        public Supplier<Set<T>> supplier()
            {
            return HashSet::new;
            }

        // ----- PortableObject interface -----------------------------------

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            }
        }
    }
