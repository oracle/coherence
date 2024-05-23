/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.concurrent.executor.function;

import com.oracle.coherence.concurrent.executor.Result;
import com.oracle.coherence.concurrent.executor.Task;
import com.oracle.coherence.concurrent.executor.TaskExecutorService;

import com.oracle.coherence.concurrent.executor.options.Role;

import com.tangosol.util.function.Remote.Predicate;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import java.io.IOException;

/**
 * Helper methods for {@link Predicate}s.
 *
 * @author bo
 * @since 21.12
 */
public final class Predicates
    {
    // ----- constructors ---------------------------------------------------

    /**
     * New instances should not be created.
     */
    private Predicates()
        {
        }

    /**
     * Obtains a {@link Predicate} that always succeeds.
     *
     * @param <T>  the type of value
     *
     * @return a {@link Predicate}
     */
    public static <T> Predicate<T> always()
        {
        return AlwaysPredicate.get();
        }

    /**
     * Obtains a {@link Predicate} that always succeeds.
     *
     * @param <T>  the type of value
     *
     * @return a {@link Predicate}
     */
    public static <T> Predicate<T> anything()
        {
        return AlwaysPredicate.get();
        }

    /**
     * Obtains a {@link Predicate} that never succeeds.
     *
     * @param <T>  the type of value
     *
     * @return a {@link Predicate}
     */
    public static <T> Predicate<T> never()
        {
        return NeverPredicate.get();
        }

    /**
     * Obtains a {@link Predicate} that succeeds when provided with a <code>null</code> value.
     *
     * @param <T>  the type of value
     *
     * @return a {@link Predicate}
     */
    public static <T> Predicate<T> nullValue()
        {
        return NullValuePredicate.get();
        }

    /**
     * Obtains a {@link Predicate} that succeeds when provided with a non-<code>null</code> value.
     *
     * @param <T>  the type of value
     *
     * @return a {@link Predicate}
     */
    public static <T> Predicate<T> notNullValue()
        {
        return not(NullValuePredicate.get());
        }

    /**
     * Obtains a {@link Predicate} that uses {@link Object#equals(Object)} to compare against a specified value.
     *
     * @param value  the value to compare
     * @param <T>    the type of value
     *
     * @return a {@link Predicate}
     */
    public static <T> Predicate<T> equalTo(T value)
        {
        return new EqualToPredicate<T>(value);
        }

    /**
     * Obtains a {@link Predicate} that uses {@link Object#equals(Object)} to compare against a specified value.
     *
     * @param value  the value to compare
     * @param <T>    the type of value
     *
     * @return a {@link Predicate}
     *
     * @see #equalTo(Object)
     */
    public static <T> Predicate<T> is(T value)
        {
        return new EqualToPredicate<T>(value);
        }

    /**
     * Returns the specified {@link Predicate}.
     * <p>
     * This is syntactic sugar to allow more expressive predicate construction.
     *
     * @param predicate  the {@link Predicate} to return
     * @param <T>        the type of value for the {@link Predicate}
     *
     * @return the specified {@link Predicate}
     */
    public static <T> Predicate<T> is(Predicate<T> predicate)
        {
        return predicate;
        }

    /**
     * Obtains a {@link Predicate} that negates the result of another {@link Predicate}.
     *
     * @param predicate  the {@link Predicate} to negate
     * @param <T>        the type of the value
     *
     * @return a {@link Predicate}
     */
    public static <T> Predicate<T> not(Predicate<T> predicate)
        {
        return new NegatePredicate<>(predicate);
        }

    /**
     * Obtains a {@link Predicate} that succeeds when {@link Result#isPresent()} ()}.
     *
     * @param <T>  the type of {@link Task} result
     *
     * @return a {@link Predicate}
     */
    public static <T> Predicate<Result<T>> available()
        {
        return IsValuePredicate.get();
        }

    /**
     * Obtains a {@link Predicate} for {@link TaskExecutorService.ExecutorInfo} to match the {@link Role} name.
     *
     * @param pattern  the {@link Role} name pattern to match
     *
     * @return a {@link Predicate} for {@link TaskExecutorService.ExecutorInfo}
     */
    public static Predicate<TaskExecutorService.ExecutorInfo> role(String pattern)
        {
        return new RolePredicate(pattern);
        }

    /**
     * Obtains a {@link Predicate} for {@link TaskExecutorService.ExecutorInfo} to determine if a specific {@link
     * TaskExecutorService.Registration.Option} is defined.
     *
     * @param option  the {@link TaskExecutorService.Registration.Option}
     *
     * @return a {@link Predicate} for {@link TaskExecutorService.ExecutorInfo}
     */
    public static Predicate<TaskExecutorService.ExecutorInfo> has(TaskExecutorService.Registration.Option option)
        {
        return new OptionPredicate(option);
        }

    /**
     * Obtains a {@link Predicate} to ensure that the {@link Throwable} is handled.
     *
     * @param <T>  the type of the task
     *
     * @return a {@link Predicate} for {@link Throwable}
     */
    public static <T> Predicate<Result<T>> onException()
        {
        return new ThrowablePredicate();
        }

    /**
     * Obtains a {@link Predicate} to ensure that the {@link Result} satisfies a specified {@link Predicate}.
     *
     * @param <T>        the type of the task
     * @param throwable  a result with a throwable
     *
     * @return a {@link Predicate} for a given {@link Throwable}
     */
    public static <T> Predicate<Result<T>> onException(Throwable throwable)
        {
        return new ThrowablePredicate(throwable);
        }

    // ----- inner class AlwaysPredicate ------------------------------------

    /**
     * An {@link AlwaysPredicate} returns true for any value provided to the {@link Predicate#test(Object)} method.
     */
    @SuppressWarnings("rawtypes")
    public static class AlwaysPredicate
            implements PortablePredicate
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs a {@link AlwaysPredicate} (required for serialization).
         */
        public AlwaysPredicate()
            {
            }

        /**
         * Obtains an instance of the {@link AlwaysPredicate}.
         *
         * @param <T>  the type of the input to the predicate
         *
         * @return an {@link AlwaysPredicate}
         */
        @SuppressWarnings("unchecked")
        public static <T> Predicate<T> get()
            {
            return INSTANCE;
            }

        // ----- Predicate interface ----------------------------------------

        @Override
        public boolean test(Object o)
            {
            return true;
            }

        // ----- constants --------------------------------------------------

        /**
         * A constant for the {@link AlwaysPredicate}.
         */
        protected static final AlwaysPredicate INSTANCE = new AlwaysPredicate();
        }

    // ----- inner class: EqualToPredicate ----------------------------------

    /**
     * A {@link Predicate} to compare a value using {@link Object#equals(Object)}.
     *
     * @param <T>  the type of value
     */
    public static class EqualToPredicate<T>
            implements PortablePredicate<T>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs a {@link EqualToPredicate} (required for serialization).
         */
        @SuppressWarnings("unused")
        public EqualToPredicate()
            {
            }

        // ----- PortablePredicate interface --------------------------------

        @Override
        public boolean test(T t)
            {
            return m_value == null && t == null || m_value != null && m_value.equals(t);
            }

        // ----- PortablePredicate interface --------------------------------

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            m_value = in.readObject(0);
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeObject(0, m_value);
            }

        // ----- helper methods ---------------------------------------------

        /**
         * Constructs an {@link EqualToPredicate}.
         *
         * @param value  the value being compared
         */
        public EqualToPredicate(T value)
            {
            m_value = value;
            }

        // ----- data members -----------------------------------------------

        /**
         * The value to compare.
         */
        protected T m_value;
        }

    // ----- inner class: IsValuePredicate ----------------------------------

    /**
     * An {@link IsValuePredicate} returns true when a provided {@link Result#isValue()}.
     *
     * @param <T>  the type of the {@link Result} being tested
     */
    public static class IsValuePredicate<T>
            implements PortablePredicate<Result<T>>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs a {@link IsValuePredicate} (required for serialization).
         */
        public IsValuePredicate()
            {
            }

        // ----- PortablePredicate interface --------------------------------

        @Override
        public boolean test(Result<T> result)
            {
            return result != null && result.isValue();
            }

        // ----- helper methods ---------------------------------------------

        /**
         * Obtains an instance of the {@link IsValuePredicate}.
         *
         * @param <T>  the type of the {@link Task}
         *
         * @return an {@link IsValuePredicate}
         */
        public static <T> IsValuePredicate<T> get()
            {
            return INSTANCE;
            }


        // ----- data members -----------------------------------------------

        /**
         * A constant for the {@link IsValuePredicate}.
         */
        protected static final IsValuePredicate INSTANCE = new IsValuePredicate();
        }

    // ----- inner class: NegatePredicate -----------------------------------

    /**
     * A {@link Predicate} that negates the result of another {@link Predicate}.
     *
     * @param <T>  the type of value
     */
    public static class NegatePredicate<T>
            implements PortablePredicate<T>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs a {@link NegatePredicate} (required for serialization).
         */
        @SuppressWarnings("unused")
        public NegatePredicate()
            {
            }

        /**
         * Constructs a {@link NegatePredicate}.
         *
         * @param predicate  the {@link Predicate} to negate
         */
        public NegatePredicate(Predicate<T> predicate)
            {
            m_predicate = predicate;
            }

        // ----- PortablePredicate interface --------------------------------

        @Override
        public boolean test(T t)
            {
            return !m_predicate.test(t);
            }

        // ----- PortablePredicate methods ----------------------------------

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            m_predicate = in.readObject(0);
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeObject(0, m_predicate);
            }

        // ----- data members -----------------------------------------------

        /**
         * The {@link Predicate} to negate.
         */
        private Predicate<T> m_predicate;
        }

    /**
     * An {@link NeverPredicate} returns false for any value provided to the {@link Predicate#test(Object)} method.
     */
    public static class NeverPredicate
            implements PortablePredicate
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs a {@link NeverPredicate} (required for serialization).
         */
        public NeverPredicate()
            {
            }

        // ----- helpers ----------------------------------------------------

        /**
         * Obtains an instance of the {@link NeverPredicate}.
         *
         * @return an {@link NeverPredicate}
         */
        public static NeverPredicate get()
            {
            return INSTANCE;
            }

        // ----- PortablePredicate interface --------------------------------

        @Override
        public boolean test(Object o)
            {
            return false;
            }

        // ----- constants --------------------------------------------------

        /**
         * A constant for the {@link NeverPredicate}.
         */
        protected static final NeverPredicate INSTANCE = new NeverPredicate();
        }

    // ----- inner class: NullValuePredicate --------------------------------

    /**
     * An {@link NullValuePredicate} returns <code>true</code> for any value provided to the {@link
     * Predicate#test(Object)} method that is <code>null</code>.
     */
    public static class NullValuePredicate
            implements PortablePredicate
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs a {@link NullValuePredicate} (required for serialization).
         */
        public NullValuePredicate()
            {
            }

        // ----- PortablePredicate interface --------------------------------

        @Override
        public boolean test(Object o)
            {
            return o == null;
            }

        // ----- helper methods ---------------------------------------------

        /**
         * Obtains an instance of the {@link NullValuePredicate}.
         *
         * @return a {@link NullValuePredicate}
         */
        public static NullValuePredicate get()
            {
            return INSTANCE;
            }

        // ----- constants --------------------------------------------------

        /**
         * A constant for the {@link NullValuePredicate}.
         */
        protected static final NullValuePredicate INSTANCE = new NullValuePredicate();
        }

    // ----- inner class: OptionPredicate -----------------------------------

    /**
     * A {@link Predicate} to determine if an {@link TaskExecutorService.ExecutorInfo} has a specific {@link
     * TaskExecutorService.Registration.Option}.
     */
    public static class OptionPredicate
            implements PortablePredicate<TaskExecutorService.ExecutorInfo>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs a {@link OptionPredicate} (required for serialization).
         */
        @SuppressWarnings("unused")
        public OptionPredicate()
            {
            }

        /**
         * Constructs a {@link OptionPredicate}.
         *
         * @param option  the {@link TaskExecutorService.Registration.Option}
         */
        public OptionPredicate(TaskExecutorService.Registration.Option option)
            {
            m_option = option;
            }

        // ----- PortablePredicate interface --------------------------------

        @Override
        public boolean test(TaskExecutorService.ExecutorInfo executorInfo)
            {
            TaskExecutorService.Registration.Option option = executorInfo.getOption(m_option.getClass(), null);

            return m_option == null && option == null || m_option != null && m_option.equals(option);
            }

        // ----- PortablePredicate methods ----------------------------------

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            m_option = in.readObject(0);
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeObject(0, m_option);
            }

        // ----- data members -----------------------------------------------

        /**
         * The {@link TaskExecutorService.Registration.Option}.
         */
        protected TaskExecutorService.Registration.Option m_option;
        }

    // ----- inner class: RolePredicate -------------------------------------

    /**
     * A {@link Predicate} for matching the name of a {@link Role}.
     */
    public static class RolePredicate
            implements PortablePredicate<TaskExecutorService.ExecutorInfo>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs a {@link RolePredicate} (required for serialization).
         */
        @SuppressWarnings("unused")
        public RolePredicate()
            {
            }

        /**
         * Constructs a {@link RolePredicate}.
         *
         * @param sPattern  the pattern to match
         */
        public RolePredicate(String sPattern)
            {
            m_sPattern = sPattern;
            }

        // ----- PortablePredicate interface --------------------------------

        @Override
        public boolean test(TaskExecutorService.ExecutorInfo executorInfo)
            {
            Role role = executorInfo.getOption(Role.class, null);

            return role != null && role.getName().matches(m_sPattern);
            }

        // ----- PortablePredicate methods ----------------------------------

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            m_sPattern = in.readString(0);
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeString(0, m_sPattern);
            }

        // ----- data members -----------------------------------------------

        /**
         * The pattern for matching a {@link Role} name.
         */
        protected String m_sPattern;
        }

    // ----- inner class: ThrowablePredicate --------------------------------

    /**
     * An {@link ThrowablePredicate} returns true when a provided {@link Result#isThrowable()}.
     *
     * @param <T>  the type of the {@link Result} being tested
     */
    public static class ThrowablePredicate<T>
            implements PortablePredicate<Result<T>>
        {
        // ----- constructors -----------------------------------------------

        /**
         * Constructs a {@link ThrowablePredicate}, without caring for the type of {@link Throwable}.
         */
        public ThrowablePredicate()
            {
            }

        /**
         * Constructs a {@link ThrowablePredicate}.
         *
         * @param throwable  the {@link Throwable} to compare with
         */
        public ThrowablePredicate(Throwable throwable)
            {
            m_throwable = throwable;
            }

        // ----- PortablePredicate interface --------------------------------

        @Override
        public boolean test(Result<T> result)
            {
            if (m_throwable == null || result == null)
                {
                return result != null && result.isThrowable();
                }

            Throwable given = null;

            try
                {
                result.get();
                }
            catch (Throwable throwable)
                {
                given = throwable;
                }

            return given != null && given.toString().equals(m_throwable.toString());
            }

        // ----- PortablePredicate methods-- --------------------------------

        @Override
        public void readExternal(PofReader in) throws IOException
            {
            m_throwable = in.readObject(0);
            }

        @Override
        public void writeExternal(PofWriter out) throws IOException
            {
            out.writeObject(0, m_throwable);
            }

        // ----- data members -----------------------------------------------

        /**
         * The {@link Throwable}.
         */
        protected Throwable m_throwable;
        }
    }
