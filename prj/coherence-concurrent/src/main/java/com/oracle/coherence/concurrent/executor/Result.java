/*
 * Copyright (c) 2016, 2022, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.executor;

import com.tangosol.io.ExternalizableLite;

import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;

import com.tangosol.util.ExternalizableHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * An immutable container representing an intermediate or final result produced by
 * executing a {@link Task}, typically by an individual {@link Executor}, including no
 * result, a specific value or a {@link Throwable}.
 * <p>
 * If a value or {@link Throwable} is present, isPresent() will return <code>true</code>
 * and get() will return the value or throw the {@link Throwable}.
 *
 * @param <T>  the type of result produced by the {@link Task}
 *
 * @author bo
 * @since 21.12
 */
public class Result<T>
        implements ExternalizableLite, PortableObject
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs an {@link Result} that has no value.
     */
    public Result()
        {
        m_fPresent  = false;
        m_value     = null;
        m_throwable = null;
        }

    /**
     * Constructs an {@link Result} with a specific value.
     *
     * @param value  the value
     */
    private Result(T value)
        {
        m_fPresent  = true;
        m_value     = value;
        m_throwable = null;
        }

    /**
     * Constructs an {@link Result} with a throwable.
     *
     * @param throwable  the throwable
     */
    private Result(Throwable throwable)
        {
        m_fPresent  = true;
        m_value     = null;
        m_throwable = throwable;
        }

    // ----- public methods -------------------------------------------------

    /**
     * Constructs an {@link Result} representing no value.
     *
     * @param <T>  the type throwable the {@link Result}
     *
     * @return a {@link Result} representing no value.
     */
    public static <T> Result<T> none()
        {
        return new Result<>();
        }

    /**
     * Constructs an {@link Result} with a specific value.
     *
     * @param value  the value
     * @param <T>    the type throwable the {@link Result}
     *
     * @return an {@link Result} with a specific value
     */
    public static <T> Result<T> of(T value)
        {
        return new Result<>(value);
        }

    /**
     * Constructs an {@link Result} with a specific {@link Throwable}.
     *
     * @param throwable  the {@link Throwable}
     *
     * @return an {@link Result} with a specific {@link Throwable}
     */
    public static Result throwable(Throwable throwable)
        {
        return new Result<>(throwable);
        }

    /**
     * Determines if a value or {@link Throwable} is present for the {@link Result}
     * (ie: non-{@link #none()}).
     *
     * @return <code>true</code> if the {@link Result} is not {@link #none()},
     *         <code>false</code> otherwise
     */
    public boolean isPresent()
        {
        return m_fPresent;
        }

    /**
     * Determines if the value is available (including <code>null</code>).
     *
     * @return <code>true</code> if the {@link Result} is not {@link #none()} and
     *       a value is available, <code>false</code> otherwise
     */
    public boolean isValue()
        {
        return isPresent() && m_throwable == null;
        }

    /**
     * Determines if the {@link Result} is a {@link Throwable}.
     *
     * @return <code>true</code> if the {@link Result} is not {@link #none()}
     *         and is a {@link Throwable} <code>false</code> otherwise
     */
    public boolean isThrowable()
        {
        return isPresent() && m_throwable != null;
        }

    /**
     * Obtains the value throwable the {@link Result}.
     *
     * @return the value throwable the {@link Result}
     *
     * @throws NoSuchElementException when the {@link Result} value is
     *                                not {@link #isPresent()}
     * @throws Throwable              when the {@link Result} was any
     *                               other {@link Throwable}
     */
    public T get() throws Throwable
        {
        if (m_fPresent)
            {
            if (m_throwable == null)
                {
                return m_value;
                }
            else
                {
                throw m_throwable;
                }
            }
        else
            {
            throw new NoSuchElementException("Result doesn't contain a value");
            }
        }

    /**
     * Obtains the value of the {@link Result} if present, otherwise returns the
     * specified value.
     *
     * @param value  the value to return if the {@link Result#isPresent()} fails
     *
     * @return the {@link Result#get()} when {@link Result#isPresent()}, otherwise
     *         the specified value
     */
    public T orElse(T value)
        {
        return isValue() ? m_value : value;
        }

    // ----- Object methods -------------------------------------------------

    @Override
    public boolean equals(Object o)
        {
        if (this == o)
            {
            return true;
            }

        if (o == null || getClass() != o.getClass())
            {
            return false;
            }

        Result<?> result = (Result<?>) o;

        if (m_fPresent != result.m_fPresent)
            {
            return false;
            }

        if (!Objects.equals(m_value, result.m_value))
            {
            return false;
            }

        return Objects.equals(m_throwable, result.m_throwable);
        }

    @Override
    public int hashCode()
        {
        int result = (m_fPresent ? 1 : 0);

        result = 31 * result + (m_value != null ? m_value.hashCode() : 0);
        result = 31 * result + (m_throwable != null ? m_throwable.hashCode() : 0);

        return result;
        }

    @Override
    public String toString()
        {
        return "Result{"
               + (m_fPresent
                  ? (m_throwable == null ? "value=" + m_value : "throwable=" + m_throwable) : "not-present") + "}";
        }

    // ----- ExternalizableLite interface -------------------------------

    @Override
    public void readExternal(DataInput in) throws IOException
        {
        m_fPresent  = in.readBoolean();
        m_value     = ExternalizableHelper.readObject(in);
        m_throwable = ExternalizableHelper.readObject(in);
        }

    @Override
    public void writeExternal(DataOutput out) throws IOException
        {
        out.writeBoolean(m_fPresent);
        ExternalizableHelper.writeObject(out, m_value);
        ExternalizableHelper.writeObject(out, m_throwable);
        }

    // ----- PortableObject interface ---------------------------------------

    @Override
    public void readExternal(PofReader in) throws IOException
        {
        m_fPresent  = in.readBoolean(0);
        m_value     = in.readObject(1);
        m_throwable = in.readObject(2);
        }

    @Override
    public void writeExternal(PofWriter out) throws IOException
        {
        out.writeBoolean(0, m_fPresent);
        out.writeObject(1,  m_value);
        out.writeObject(2,  m_throwable);
        }

    // ----- data members ---------------------------------------------------

    /**
     * Is the result present?
     */
    protected boolean m_fPresent;

    /**
     * The value (when provided).
     */
    protected T m_value;

    /**
     * The throwable (when error occurred).
     */
    protected Throwable m_throwable;
    }
