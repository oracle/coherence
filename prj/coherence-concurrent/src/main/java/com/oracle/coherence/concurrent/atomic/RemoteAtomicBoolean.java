/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * http://oss.oracle.com/licenses/upl.
 */
package com.oracle.coherence.concurrent.atomic;

import com.tangosol.net.NamedMap;

import com.tangosol.util.function.Remote;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The remote implementation of {@link com.oracle.coherence.concurrent.atomic.AtomicBoolean AtomicBoolean},
 * backed by a Coherence {@code NamedMap} entry.
 * <p>
 * Every method in this class is guaranteed to execute effectively-once, and provides
 * cluster-wide atomicity guarantees for the backing atomic value. However,
 * keep in mind that this comes at a significant cost -- each method invocation
 * results in a network call to a remote owner of the backing atomic value,
 * which means that each operation has significantly higher latency than a
 * corresponding {@link LocalAtomicBoolean local} implementation.
 * <p>
 * To somewhat reduce that performance penalty, consider using non-blocking
 * {@link AsyncAtomicBoolean} implementation instead.
 *
 * @author Aleks Seovic  2020.12.07
 * @since 21.06
 */
public class RemoteAtomicBoolean
        implements com.oracle.coherence.concurrent.atomic.AtomicBoolean
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct {@code RemoteAtomicBoolean} instance.
     *
     * @param mapAtomic  the map that holds this atomic value
     * @param sName      the name of this atomic value
     */
    protected RemoteAtomicBoolean(NamedMap<String, AtomicBoolean> mapAtomic, String sName)
        {
        f_mapAtomic = mapAtomic;
        f_sName     = sName;
        }

    // ----- AtomicBoolean interface ----------------------------------------

    @Override
    public AsyncRemoteAtomicBoolean async()
        {
        return new AsyncRemoteAtomicBoolean(f_mapAtomic.async(), f_sName);
        }

    @Override
    public boolean get()
        {
        return invoke(AtomicBoolean::get, false);
        }

    @Override
    public void set(boolean fNewValue)
        {
        invoke(value ->
               {
               value.set(fNewValue);
               return null;
               });
        }

    @Override
    public boolean getAndSet(boolean fNewValue)
        {
        return invoke(value -> value.getAndSet(fNewValue));
        }

    @Override
    public boolean compareAndSet(boolean fExpectedValue, boolean fNewValue)
        {
        return invoke(value -> value.compareAndSet(fExpectedValue, fNewValue));
        }

    @Override
    public boolean compareAndExchange(boolean fExpectedValue, boolean fNewValue)
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
        return Boolean.toString(get());
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
    protected <R> R invoke(Remote.Function<AtomicBoolean, R> function)
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
    protected <R> R invoke(Remote.Function<AtomicBoolean, R> function, boolean fMutate)
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
    private final NamedMap<String, AtomicBoolean> f_mapAtomic;

    /**
     * The name of this atomic value.
     */
    private final String f_sName;
    }
