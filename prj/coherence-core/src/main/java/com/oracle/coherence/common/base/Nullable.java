/*
 * Copyright (c) 2000, 2024, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.common.base;

/**
 * An interface that any class can implement to mark itself as "nullable",
 * which allows it to be used in a more optimal way with collections that
 * support "nullable" keys and/or values.
 * <p>
 * While technically not a marker interface, this interface can typically be
 * used as such as it provides a reasonable default implementation of the
 * {@code get} method that simply returns the instance itself.
 * <p>
 * The rest of the methods in this interface are static factory methods that
 * allow creation of a {@code Nullable} values from various primitive, wrapper
 * and reference types, as well as a static {@link Nullable#get(Nullable)}
 * method that allows you to "unwrap" any {@code Nullable} and return either
 * the value itself or a {@code null}, if the specified {@code Nullable} is
 * empty.
 *
 * @since 24.03
 * @author Aleks Seovic  2024.01.08
 */
@SuppressWarnings("unchecked")
public interface Nullable<T>
    {
    /**
     * Create a {@code Nullable} representation of the specified reference value.
     *
     * @param value  the value to create a {@code Nullable} for
     *
     * @return a {@code Nullable} representation of the specified value
     *
     * @param <T>  the type of wrapped reference value
     */
    static <T> Nullable<T> of(T value)
        {
        if (value instanceof Nullable<?>)
            {
            return (Nullable<T>) value;
            }
        else if (value == null)
            {
            return empty();
            }
        else if (value instanceof Integer)
            {
            return (Nullable<T>) of((int) value);
            }
        else if (value instanceof Long)
            {
            return (Nullable<T>) of((long) value);
            }
        else if (value instanceof Short)
            {
            return (Nullable<T>) of((short) value);
            }
        else if (value instanceof Byte)
            {
            return (Nullable<T>) of((byte) value);
            }
        else if (value instanceof Double)
            {
            return (Nullable<T>) of((double) value);
            }
        else if (value instanceof Float)
            {
            return (Nullable<T>) of((float) value);
            }
        else if (value instanceof Boolean)
            {
            return (Nullable<T>) of((boolean) value);
            }

        return new NullableWrapper<>(value);
        }

    /**
     * Create a {@code Nullable} representation of the specified {@code int} value.
     *
     * @param value  the value to create a {@code Nullable} for
     *
     * @return a {@code Nullable} representation of the specified value
     */
    static Nullable<Integer> of(int value)
        {
        return new NullableInt(value);
        }

    /**
     * Create a {@code Nullable} representation of the specified {@code Integer} value.
     *
     * @param value  the value to create a {@code Nullable} for
     *
     * @return a {@code Nullable} representation of the specified value
     */
    static Nullable<Integer> of(Integer value)
        {
        return value == null ? empty() : of((int) value);
        }

    /**
     * Create a {@code Nullable} representation of the specified {@code long} value.
     *
     * @param value  the value to create a {@code Nullable} for
     *
     * @return a {@code Nullable} representation of the specified value
     */
    static Nullable<Long> of(long value)
        {
        return new NullableLong(value);
        }

    /**
     * Create a {@code Nullable} representation of the specified {@code Long} value.
     *
     * @param value  the value to create a {@code Nullable} for
     *
     * @return a {@code Nullable} representation of the specified value
     */
    static Nullable<Long> of(Long value)
        {
        return value == null ? empty() : of((long) value);
        }

    /**
     * Create a {@code Nullable} representation of the specified {@code short} value.
     *
     * @param value  the value to create a {@code Nullable} for
     *
     * @return a {@code Nullable} representation of the specified value
     */
    static Nullable<Short> of(short value)
        {
        return new NullableShort(value);
        }

    /**
     * Create a {@code Nullable} representation of the specified {@code Short} value.
     *
     * @param value  the value to create a {@code Nullable} for
     *
     * @return a {@code Nullable} representation of the specified value
     */
    static Nullable<Short> of(Short value)
        {
        return value == null ? empty() : of((short) value);
        }

    /**
     * Create a {@code Nullable} representation of the specified {@code byte} value.
     *
     * @param value  the value to create a {@code Nullable} for
     *
     * @return a {@code Nullable} representation of the specified value
     */
    static Nullable<Byte> of(byte value)
        {
        return new NullableByte(value);
        }

    /**
     * Create a {@code Nullable} representation of the specified {@code Byte} value.
     *
     * @param value  the value to create a {@code Nullable} for
     *
     * @return a {@code Nullable} representation of the specified value
     */
    static Nullable<Byte> of(Byte value)
        {
        return value == null ? empty() : of((byte) value);
        }

    /**
     * Create a {@code Nullable} representation of the specified {@code double} value.
     *
     * @param value  the value to create a {@code Nullable} for
     *
     * @return a {@code Nullable} representation of the specified value
     */
    static Nullable<Double> of(double value)
        {
        return new NullableDouble(value);
        }

    /**
     * Create a {@code Nullable} representation of the specified {@code Double} value.
     *
     * @param value  the value to create a {@code Nullable} for
     *
     * @return a {@code Nullable} representation of the specified value
     */
    static Nullable<Double> of(Double value)
        {
        return value == null ? empty() : of((double) value);
        }

    /**
     * Create a {@code Nullable} representation of the specified {@code float} value.
     *
     * @param value  the value to create a {@code Nullable} for
     *
     * @return a {@code Nullable} representation of the specified value
     */
    static Nullable<Float> of(float value)
        {
        return new NullableFloat(value);
        }

    /**
     * Create a {@code Nullable} representation of the specified {@code Float} value.
     *
     * @param value  the value to create a {@code Nullable} for
     *
     * @return a {@code Nullable} representation of the specified value
     */
    static Nullable<Float> of(Float value)
        {
        return value == null ? empty() : of((float) value);
        }

    /**
     * Create a {@code Nullable} representation of the specified {@code boolean} value.
     *
     * @param value  the value to create a {@code Nullable} for
     *
     * @return a {@code Nullable} representation of the specified value
     */
    static Nullable<Boolean> of(boolean value)
        {
        return value ? NullableBoolean.TRUE : NullableBoolean.FALSE;
        }

    /**
     * Create a {@code Nullable} representation of the specified {@code Boolean} value.
     *
     * @param value  the value to create a {@code Nullable} for
     *
     * @return a {@code Nullable} representation of the specified value
     */
    static Nullable<Boolean> of(Boolean value)
        {
        return value == null ? empty() : of((boolean) value);
        }

    /**
     * Create an empty {@code Nullable} value.
     *
     * @return an empty {@code Nullable}
     */
    static <T> Nullable<T> empty()
        {
        return ((Nullable<T>) NullableEmpty.INSTANCE);
        }

    /**
     * Return the value of the specified {@code Nullable}.
     *
     * @param value  the {@code Nullable} value to get the value from
     *
     * @return the value of the specified {@code Nullable}, or {@code null} if
     *         the {@code Nullable} is empty
     *
     * @param <T> the type of {@code Nullable} value
     */
    static <T> T get(Nullable<? extends T> value)
        {
        return value == null ? null : value.get();
        }

    /**
     * Return the raw value of this {@code Nullable}.
     *
     * @return the raw value of this {@code Nullable}
     */
    default T get()
        {
        return (T) this;
        }
    }
