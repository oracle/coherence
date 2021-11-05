/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic;

import com.tangosol.net.AsyncNamedMap;

import com.tangosol.util.function.Remote;

import java.util.concurrent.CompletableFuture;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The remote implementation of {@link AsyncAtomicBoolean}, backed by a
 * Coherence {@code NamedMap} entry.
 * <p>
 * Every method in this class is guaranteed to execute effectively-once, and provides
 * cluster-wide atomicity guarantees for the backing atomic value. However,
 * keep in mind that this comes at a significant cost -- each method invocation
 * results in a network call to a remote owner of the backing atomic value,
 * which means that each operation has significantly higher latency than a
 * corresponding {@link AsyncLocalAtomicBoolean local} implementation.
 *
 * @author Aleks Seovic  2020.12.07
 * @since 21.12
 */
public class AsyncRemoteAtomicBoolean
        implements AsyncAtomicBoolean
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Constructs a new {@code AsyncRemoteAtomicBoolean}.
     *
     * @param mapAtomic  the map that holds this atomic value
     * @param sName      the name of this atomic value
     */
    protected AsyncRemoteAtomicBoolean(AsyncNamedMap<String, AtomicBoolean> mapAtomic, String sName)
        {
        f_mapAtomic = mapAtomic;
        f_sName     = sName;
        }

    // ----- AsyncAtomicBoolean interface -----------------------------------

    @Override
    public CompletableFuture<Boolean> get()
        {
        return invoke(AtomicBoolean::get, false);
        }

    @Override
    public CompletableFuture<Void> set(boolean fNewValue)
        {
        return invoke(value ->
               {
               value.set(fNewValue);
               return null;
               });
        }

    @Override
    public CompletableFuture<Boolean> getAndSet(boolean fNewValue)
        {
        return invoke(value -> value.getAndSet(fNewValue));
        }

    @Override
    public CompletableFuture<Boolean> compareAndSet(boolean fExpectedValue, boolean fNewValue)
        {
        return invoke(value -> value.compareAndSet(fExpectedValue, fNewValue));
        }

    @Override
    public CompletableFuture<Boolean> compareAndExchange(boolean fExpectedValue, boolean fNewValue)
        {
        return invoke(value -> value.compareAndExchange(fExpectedValue, fNewValue));
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
        return Boolean.toString(get().join());
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
    protected <R> CompletableFuture<R> invoke(Remote.Function<AtomicBoolean, R> function)
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
    protected <R> CompletableFuture<R> invoke(Remote.Function<AtomicBoolean, R> function, boolean fMutate)
        {
        return f_mapAtomic.invoke(f_sName, entry ->
                {
                AtomicBoolean value  = entry.getValue();
                R             result = function.apply(value);

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
    private final AsyncNamedMap<String, AtomicBoolean> f_mapAtomic;

    /**
     * The name of this atomic value.
     */
    private final String f_sName;
    }
