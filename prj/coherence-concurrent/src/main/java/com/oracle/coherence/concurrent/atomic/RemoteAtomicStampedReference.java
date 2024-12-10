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
import java.util.concurrent.atomic.AtomicStampedReference;

/**
 * The remote implementation of {@link com.oracle.coherence.concurrent.atomic.AtomicStampedReference
 * AtomicStampedReference}, backed by a Coherence {@code NamedMap} entry.
 * <p>
 * Every method in this class is guaranteed to execute effectively-once, and provides
 * cluster-wide atomicity guarantees for the backing atomic value. However,
 * keep in mind that this comes at a significant cost -- each method invocation
 * results in a network call to a remote owner of the backing atomic value,
 * which means that each operation has significantly higher latency than a
 * corresponding {@link LocalAtomicStampedReference local} implementation.
 * <p>
 * To somewhat reduce that performance penalty, consider using non-blocking
 * {@link AsyncAtomicStampedReference} implementation instead.
 *
 * @param <V>  the type of object referred to by this reference
 *
 * @author Aleks Seovic  2020.12.09
 * @since 21.12
 */
public class RemoteAtomicStampedReference<V>
        implements com.oracle.coherence.concurrent.atomic.AtomicStampedReference<V>
    {
    // ----- constructors ---------------------------------------------------

    /**
     * Construct {@code RemoteAtomicStampedReference} instance.
     *
     * @param mapAtomic  the map that holds this atomic value
     * @param sName      the name of this atomic value
     */
    protected RemoteAtomicStampedReference(NamedMap<String, AtomicStampedReference<V>> mapAtomic, String sName)
        {
        f_mapAtomic = mapAtomic;
        f_sName     = sName;
        }

    // ----- AtomicStampedReference interface -------------------------------

    @Override
    public AsyncRemoteAtomicStampedReference<V> async()
        {
        return new AsyncRemoteAtomicStampedReference<>(f_mapAtomic.async(), f_sName);
        }

    @Override
    public V getReference()
        {
        return invoke(AtomicStampedReference::getReference, false);
        }

    @Override
    public int getStamp()
        {
        return invoke(AtomicStampedReference::getStamp, false);
        }

    @Override
    @SuppressWarnings("unchecked")
    public V get(int[] iaStampHolder)
        {
        Object[] aResult = invoke(value ->
                {
                int[] aiStamp = new int[1];
                V     v       = value.get(aiStamp);

                return new Object[] {v, aiStamp[0]};
                }, false);

        iaStampHolder[0] = (int) aResult[1];
        return (V) aResult[0];
        }

    @Override
    @SuppressWarnings("DuplicatedCode")
    public boolean compareAndSet(V expectedReference, V newReference, int nExpectedStamp, int nNewStamp)
        {
        return invoke(value ->
                {
                int[] aiStamp = new int[1];
                V     v       = value.get(aiStamp);

                if (Objects.equals(v, expectedReference) && nExpectedStamp == aiStamp[0])
                    {
                    value.set(newReference, nNewStamp);
                    return true;
                    }

                return false;
                });
        }

    @Override
    public void set(V newReference, int nNewStamp)
        {
        invoke(value ->
                {
                value.set(newReference, nNewStamp);
                return null;
                });
        }

    @Override
    public boolean attemptStamp(V expectedReference, int nNewStamp)
        {
        return invoke(value ->
                {
                V v = value.getReference();

                if (Objects.equals(v, expectedReference))
                    {
                    value.set(v, nNewStamp);
                    return true;
                    }

                return false;
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
        int[] aiStamp = new int[1];
        V     value   = get(aiStamp);

        return value + " (" + aiStamp[0] + ')';
        }

    // ----- helper methods -------------------------------------------------

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
    protected <R> R invoke(Remote.Function<AtomicStampedReference<V>, R> function)
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
    protected <R> R invoke(Remote.Function<AtomicStampedReference<V>, R> function, boolean fMutate)
        {
        return f_mapAtomic.invoke(f_sName, entry ->
                {
                AtomicStampedReference<V> value = entry.getValue();
                R result                        = function.apply(value);
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
    private final NamedMap<String, AtomicStampedReference<V>> f_mapAtomic;

    /**
     * The name of this atomic value.
     */
    private final String f_sName;
    }
