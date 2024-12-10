/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.concurrent.executor.function;

import com.oracle.coherence.concurrent.executor.ExecutionPlan;
import com.oracle.coherence.concurrent.executor.Task;

import com.tangosol.util.function.Remote.BiPredicate;
import com.tangosol.util.function.Remote.Predicate;

import java.util.Map;

/**
 * Helper methods for {@link BiPredicate}s.
 *
 * @author bo
 * @since 21.12
 */
public final class BiPredicates
    {
    // ----- constructors ---------------------------------------------------

    /**
     * No instances should be created.
     */
    private BiPredicates()
        {
        }

    // ----- public API -----------------------------------------------------

    /**
     * Obtains a {@link BiPredicate} that never succeeds.
     *
     * @param <T>  the type of the first value
     * @param <U>  the type of the second value
     *
     * @return a {@link BiPredicate}
     */
    public static <T, U> BiPredicate<T, U> never()
        {
        return NeverBiPredicate.get();
        }

    /**
     * Obtains a {@link Predicate} that ensures all results satisfy a specified {@link Predicate}.
     *
     * @param predicate  the result {@link Predicate}
     * @param <T>        the type of the result
     *
     * @return a {@link BiPredicate}
     */
    public static <T> BiPredicate<ExecutionPlan, Map<String, T>> all(Predicate<? super T> predicate)
        {
        return new AllResultsBiPredicate<>(predicate);
        }

    /**
     * Obtains a {@link Predicate} that ensures any result satisfies a specified {@link Predicate}.
     *
     * @param predicate  the result {@link Predicate}
     * @param <T>        the result type
     *
     * @return a {@link BiPredicate}
     */
    public static <T> BiPredicate<ExecutionPlan, Map<String, T>> any(Predicate<? super T> predicate)
        {
        return new AnyResultBiPredicate<>(predicate);
        }

    /**
     * A {@link BiPredicate} that is satisfied only when all provided results satisfy another {@link Predicate}.
     *
     * @param <T>  the type of the {@link Task}
     */
    public static class AllResultsBiPredicate<T>
            implements BiPredicate<ExecutionPlan, Map<String, T>>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs an {@link AllResultsBiPredicate}.
         *
         * @param predicate  the result {@link Predicate}
         */
        public AllResultsBiPredicate(Predicate<? super T> predicate)
            {
            f_predicate = predicate;
            }

        // ----- BiPredicate interface --------------------------------------

        @Override
        public boolean test(ExecutionPlan plan, Map<String, T> mapResult)
            {
            // we can only test the predicate on results when the ExecutionPlan has been satisfied
            if (plan != null && plan.isSatisfied())
                {
                // assume all results satisfy the predicate
                boolean fSatisfied = true;

                for (T result : mapResult.values())
                    {
                    fSatisfied = fSatisfied && f_predicate.test(result);

                    if (!fSatisfied)
                        {
                        break;
                        }
                    }

                return fSatisfied;
                }
            else
                {
                return false;
                }
            }

        // ----- data members -----------------------------------------------

        /**
         * The {@link Predicate} for a result.
         */
        private final Predicate<? super T> f_predicate;
        }

    /**
     * A {@link BiPredicate} that is satisfied when one or more of the provided results satisfies another {@link
     * Predicate}.
     *
     * @param <T>  the type of the {@link Task}
     */
    public static class AnyResultBiPredicate<T>
            implements BiPredicate<ExecutionPlan, Map<String, T>>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs an {@link AnyResultBiPredicate}.
         *
         * @param predicate  the result {@link Predicate}
         */
        public AnyResultBiPredicate(Predicate<? super T> predicate)
            {
            f_predicate = predicate;
            }

        // ----- BiPredicate interface --------------------------------------

        @Override
        public boolean test(ExecutionPlan plan, Map<String, T> mapResult)
            {
            // assume none of the results satisfy the predicate
            boolean fSatisfied = false;

            for (T result : mapResult.values())
                {
                fSatisfied = fSatisfied || f_predicate.test(result);

                if (fSatisfied)
                    {
                    break;
                    }
                }

            return fSatisfied;
            }

        // ----- data members -----------------------------------------------

        /**
         * The {@link Predicate} for a result.
         */
        private final Predicate<? super T> f_predicate;
        }

    /**
     * A {@link NeverBiPredicate} returns false for all values provided to the {@link BiPredicate#test(Object, Object)}
     * method.
     */
    public static class NeverBiPredicate
            implements BiPredicate
        {
        // ----- BiPredicate interface --------------------------------------

        @Override
        public boolean test(Object first, Object second)
            {
            return false;
            }

        // ----- helper methods --------------------------------------------

        /**
         * Obtains an instance of the {@link NeverBiPredicate}.
         *
         * @return an {@link NeverBiPredicate}
         */
        public static NeverBiPredicate get()
            {
            return INSTANCE;
            }

        // ----- constants --------------------------------------------------

        /**
         * A constant for the {@link NeverBiPredicate}.
         */
        protected static final NeverBiPredicate INSTANCE = new NeverBiPredicate();
        }
    }
