/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.concurrent.executor.atomic;

import java.util.Objects;

import java.util.concurrent.atomic.AtomicReference;

/**
 * An {@link Enum} value that may be updated atomically.
 * <p>
 * See the java.util.concurrent.atomic package specification for description of the properties of atomic variables.
 *
 * @param <T> the type of the {@link Enum}
 *
 * @author bo
 * @since 21.12
 */
public final class AtomicEnum<T extends Enum<T>>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Privately constructs an {@link AtomicEnum} based on the specified value.
     *
     * @param value  the {@link Enum}
     */
    private AtomicEnum(T value)
        {
        f_reference = new AtomicReference<>(value);
        }

    // ----- AtomicEnum methods ---------------------------------------------

    /**
     * Atomically sets the value to the new value if the current value == the expected value.
     *
     * @param expect    the expected value
     * @param newValue  the new value
     *
     * @return  <code>true</code> if successful
     *          <code>false</code> indicates that the current value was not equal to the expected value.
     */
    public boolean compareAndSet(T expect, T newValue)
        {
        return f_reference.compareAndSet(expect, newValue);
        }

    /**
     * Gets the current value.
     *
     * @return the current value
     */
    public T get()
        {
        return f_reference.get();
        }

    /**
     * Atomically sets the {@link AtomicEnum} to the given new value and returns the previous value.
     *
     * @param newValue  the new value
     *
     * @return the previous value
     */
    public T getAndSet(T newValue)
        {
        return f_reference.getAndSet(newValue);
        }

    /**
     * Sets the {@link AtomicEnum} to the new value.
     *
     * @param value  the new value
     */
    public void set(T value)
        {
        f_reference.set(value);
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }

        if (!(o instanceof AtomicEnum))
            {
            return false;
            }

        AtomicEnum<?> that = (AtomicEnum<?>) o;

        return Objects.equals(f_reference, that.f_reference);
        }

    @Override
    public int hashCode()
        {
        return f_reference.hashCode();
        }

    @Override
    public String toString()
        {
        return "AtomicEnum{" + f_reference.get().toString() + '}';
        }

    // ----- helper methods -------------------------------------------------

    /**
     * Constructs an {@link AtomicEnum} given a {@link Enum} value.
     *
     * @param value  the {@link Enum} value (may be <code>null</code>)
     * @param <T>    the type of the {@link Enum}
     *
     * @return the {@link AtomicEnum}
     */
    public static <T extends Enum<T>> AtomicEnum<T> of(T value)
        {
        return new AtomicEnum<>(value);
        }

    // ----- data members ---------------------------------------------------

    /**
     * A reference to the {@link Enum}.
     */
    private final AtomicReference<T> f_reference;
    }
