/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic;

import com.tangosol.net.NamedMap;

import com.tangosol.util.function.Remote;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;

/**
 * The remote implementation of {@link com.oracle.coherence.concurrent.atomic.AtomicReference AtomicReference},
 * backed by a Coherence {@code NamedMap} entry.
 * <p>
 * Every method in this class is guaranteed to execute effectively-once, and provides
 * cluster-wide atomicity guarantees for the backing atomic value. However,
 * keep in mind that this comes at a significant cost -- each method invocation
 * results in a network call to a remote owner of the backing atomic value,
 * which means that each operation has significantly higher latency than a
 * corresponding {@link LocalAtomicReference local} implementation.
 * <p>
 * To somewhat reduce that performance penalty, consider using non-blocking
 * {@link AsyncAtomicReference} implementation instead.
 *
 * @param <V>  the type of object referred to by this reference
 *
 * @author Aleks Seovic  2020.12.08
 * @since 21.12
 */
public class RemoteAtomicReference<V>
        implements com.oracle.coherence.concurrent.atomic.AtomicReference<V>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct {@code RemoteAtomicReference} instance.
     *
     * @param mapAtomic  the map that holds this atomic value
     * @param sName      the name of this atomic value
     */
    protected RemoteAtomicReference(NamedMap<String, AtomicReference<V>> mapAtomic, String sName)
        {
        f_mapAtomic = mapAtomic;
        f_sName     = sName;
        }

    // ----- AtomicReference interface --------------------------------------

    @Override
    public AsyncRemoteAtomicReference<V> async()
        {
        return new AsyncRemoteAtomicReference<>(f_mapAtomic.async(), f_sName);
        }

    @Override
    public V get()
        {
        return invoke(AtomicReference::get, false);
        }

    @Override
    public void set(V newValue)
        {
        invoke(value ->
               {
               value.set(newValue);
               return null;
               });
        }

    @Override
    public V getAndSet(V newValue)
        {
        return invoke(value -> value.getAndSet(newValue));
        }

    @Override
    @SuppressWarnings("DuplicatedCode")
    public boolean compareAndSet(V expectedValue, V newValue)
        {
        return invoke(value ->
                {
                V v = value.get();

                if (Objects.equals(v, expectedValue))
                    {
                    value.set(newValue);
                    return true;
                    }
                return false;
                });
        }

    @Override
    public V getAndUpdate(Remote.UnaryOperator<V> updateFunction)
        {
        return getAndUpdate((UnaryOperator<V>) updateFunction);
        }

    @Override
    public V getAndUpdate(UnaryOperator<V> updateFunction)
        {
        return invoke(value -> value.getAndUpdate(updateFunction));
        }

    @Override
    public V updateAndGet(Remote.UnaryOperator<V> updateFunction)
        {
        return updateAndGet((UnaryOperator<V>) updateFunction);
        }

    @Override
    public V updateAndGet(UnaryOperator<V> updateFunction)
        {
        return invoke(value -> value.updateAndGet(updateFunction));
        }

    @Override
    public V getAndAccumulate(V x, Remote.BinaryOperator<V> accumulatorFunction)
        {
        return getAndAccumulate(x, (BinaryOperator<V>) accumulatorFunction);
        }

    @Override
    public V getAndAccumulate(V x, BinaryOperator<V> accumulatorFunction)
        {
        return invoke(value -> value.getAndAccumulate(x, accumulatorFunction));
        }

    @Override
    public V accumulateAndGet(V x, Remote.BinaryOperator<V> accumulatorFunction)
        {
        return accumulateAndGet(x, (BinaryOperator<V>) accumulatorFunction);
        }

    @Override
    public V accumulateAndGet(V x, BinaryOperator<V> accumulatorFunction)
        {
        return invoke(value -> value.accumulateAndGet(x, accumulatorFunction));
        }

    @Override
    @SuppressWarnings("DuplicatedCode")
    public V compareAndExchange(V expectedValue, V newValue)
        {
        return invoke(value ->
                {
                V v = value.get();
                if (v == null)
                    {
                    if (expectedValue == null)
                        {
                        value.set(newValue);
                        }
                    }
                else
                    {
                    if (v.equals(expectedValue))
                        {
                        value.set(newValue);
                        }
                    }

                return v;
                });
        }

    // ----- Object methods -------------------------------------------------

    /**
     * Returns the String representation of the current value.
     *
     * @return the String representation of the current value
     */
    @Override
    public String toString()
        {
        return String.valueOf(get());
        }

    // ----- helpers methods ------------------------------------------------

    /**
     * Apply specified function against the remote object and return the result.
     *
     * <p>Any changes the function makes to the remote object will be preserved.
     *
     * @param function  the function to apply
     * @param <R>       the type of the result
     *
     * @return the result of the function applied to a remote object
     */
    protected <R> R invoke(Remote.Function<AtomicReference<V>, R> function)
        {
        return invoke(function, true);
        }

    /**
     * Apply specified function against the remote object and return the result.
     *
     * <p>If the {@code fMutate} argument is {@code true}, any changes to the
     * remote object will be preserved.
     *
     * @param function  the function to apply
     * @param fMutate   flag specifying whether the function mutates the object
     * @param <R>       the type of the result
     *
     * @return the result of the function applied to a remote object
     */
    protected <R> R invoke(Remote.Function<AtomicReference<V>, R> function, boolean fMutate)
        {
        return f_mapAtomic.invoke(f_sName, entry ->
                {
                AtomicReference<V> value  = entry.getValue();
                R                  result = function.apply(value);

                if (fMutate)
                    {
                    entry.setValue(value);
                    }
                return result;
                });
        }

    // ----- data members ---------------------------------------------------

    /**
     * The map that holds this atomic value.
     */
    private final NamedMap<String, AtomicReference<V>> f_mapAtomic;

    /**
     * The name of this atomic value.
     */
    private final String f_sName;
    }
